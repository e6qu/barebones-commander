/*
 * This file is part of muCommander, http://www.mucommander.com
 *
 * muCommander is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * muCommander is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.barebones.commander.core;

import dev.barebones.commander.commons.logging.Logger;
import dev.barebones.commander.commons.logging.LoggerFactory;

import dev.barebones.commander.commons.file.AbstractFile;
import dev.barebones.commander.commons.file.FileURL;
import dev.barebones.commander.ui.event.LocationManager;

/**
 * @author Arik Hadas
 */
public abstract class ChangeFolderThread extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeFolderThread.class);

    protected LocationManager locationManager;
    protected FileURL folderURL;

    /** True if this thread has been interrupted by the user using #tryKill */
    protected boolean killed;
    /** True if an attempt to kill this thread using Thread#interrupt() has already been made */
    private boolean killedByInterrupt;
    /** True if it is unsafe to kill this thread */
    protected boolean doNotKill;

    /** Lock object used to ensure consistency and thread safeness when killing the thread */
    protected final Object KILL_LOCK = new Object();

    protected ChangeFolderThread(LocationManager locationManager, FileURL folderURL) {
        this.locationManager = locationManager;
        this.folderURL = folderURL;
    }

    @Override
    public void start() {
        // Notify listeners that location is changing
        locationManager.fireLocationChanging(folderURL);

        super.start();
    }

    // For debugging purposes
    public String toString() {
        return super.toString()+" folderURL="+folderURL;
    }

    /**
     * Attempts to stop this thread and returns <code>true</code> if an attempt was made.
     * An attempt to stop this thread will be made using one of the methods detailed hereunder, only if
     * it is still safe to do so: if the thread is too far into the process of changing the current folder,
     * this method will have no effect and return <code>false</code>.
     *
     * <p>The first time this method is called, {@link #interrupt()} is called, giving the thread a chance to stop
     * gracefully should it be waiting for a thread or blocked in an interruptible operation such as an
     * InterruptibleChannel. This may have no immediate effect if the thread is blocked in a non-interruptible
     * operation. This thread will however be marked as 'killed' which will sooner or later cause {@link #run()}
     * to stop the thread by simply returning.</p> 
     *
     * <p>Any subsequent calls to this method will have no effect and return <code>false</code>.</p>
     *
     * @return true if an attempt was made to stop this thread.
     */
    public boolean tryKill() {
        synchronized(KILL_LOCK) {
            if(doNotKill) {
                LOGGER.debug("Can't kill thread now, it's too late, returning");
                return false;
            }

            // This field needs to be set before actually killing the thread, #run() relies on it
            killed = true;

            // Call Thread#interrupt() to give the thread a chance to stop gracefully if it is waiting in
            // Thread#sleep() or Thread#wait() or Thread#join() or in an
            // interruptible operation such as java.nio.channel.InterruptibleChannel. If this is the case,
            // InterruptedException or ClosedByInterruptException will be thrown and thus need to be catched by
            // #run().
            if(!killedByInterrupt) {
                LOGGER.debug("Killing thread using #interrupt()");

                // This field needs to be set before actually interrupting the thread, #run() relies on it
                killedByInterrupt = true;
                interrupt();
                return true;
            }

            LOGGER.debug("Thread already interrupted, there's nothing more we can safely do, returning");
            return false;
        }
    }

    public abstract void selectThisFileAfter(AbstractFile fileToSelect);
    protected abstract void cleanup(boolean folderChangedSuccessfully);
}
