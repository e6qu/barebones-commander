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
package dev.barebones.commander.viewer;

import dev.barebones.commander.commons.file.AbstractFile;
import java.io.IOException;
import javax.swing.JComponent;
import javax.swing.JMenuBar;

import org.jetbrains.annotations.NotNull;

/**
 * Interface for file viewer.
 */
public interface FileViewer {

    /**
     * Opens a given AbstractFile for display.
     *
     * @param file
     *            the file to be presented
     * @throws IOException
     *             in case of an I/O problem
     */
    void open(@NotNull AbstractFile file) throws IOException;

    /**
     * Closes currently opened file.
     */
    void close();

    /**
     * Returns UI for viewer.
     *
     * @return UI component instance
     */
    @NotNull
    JComponent getUI();

    /**
     * Sets presenter API.
     *
     * @param presenter
     *            presenter API
     */
    void setPresenter(@NotNull ViewerPresenter presenter);

    /**
     * Extends provided menu with new menu items specific for this viewer.
     *
     * @param menuBar
     *            menu bar
     */
    void extendMenu(@NotNull JMenuBar menuBar);

    /**
     * Enables to customize focus handling
     */
    default void requestFocus() {}
}
