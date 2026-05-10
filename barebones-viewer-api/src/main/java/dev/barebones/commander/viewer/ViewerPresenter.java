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

import java.io.IOException;
import java.util.function.Function;

import javax.swing.JFrame;

import org.jetbrains.annotations.NotNull;

/**
 * Interface for viewer presenter.
 */
public interface ViewerPresenter {

    /**
     * Extends title of the presenter.
     *
     * @param title
     *            title
     */
    void extendTitle(@NotNull String title);

    /**
     * Returns presenter's frame.
     *
     * @return frame
     */
    @NotNull
    JFrame getWindowFrame();

    /**
     * Performs goto (for image plugin).
     *
     * @param advance
     *            advance
     * @param viewerService
     *            viewer service to use for filtering
     * @throws java.io.IOException
     *             exception if loading fails
     */
    void goToFile(@NotNull Function<Integer, Integer> advance, @NotNull FileViewerService viewerService) throws IOException;
}
