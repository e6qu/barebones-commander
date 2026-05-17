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

package dev.barebones.commander.ui.theme;

import java.awt.Color;
import java.awt.Font;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;


/**
 * Contains cached colors and fonts for current theme.
 *
 * @author Mariusz Jakubowski
 */
public class ThemeCache implements ThemeListener {
    
    // - Color definitions -----------------------------------------------------------
    // -------------------------------------------------------------------------------
    private static final Color[][][] foregroundColors = new Color[2][2][7];
    private static final Color[][]   backgroundColors = new Color[2][4];
    public static Color       unmatchedForeground;
    public static Color       unmatchedBackground;
    public static Color       activeOutlineColor;
    public static Color       inactiveOutlineColor;
    public static final int NORMAL               = 0;
    public static final int SELECTED             = 1;
    public static final int ALTERNATE            = 2;
    public static final int SECONDARY            = 3;
    public static final int INACTIVE             = 0;
    public static final int ACTIVE               = 1;
    public static final int HIDDEN_FILE          = 0;
    public static final int FOLDER               = 1;
    public static final int ARCHIVE              = 2;
    public static final int SYMLINK              = 3;
    public static final int MARKED               = 4;
    public static final int PLAIN_FILE           = 5;
    public static final int READ_ONLY            = 6;

    // - Font definitions ------------------------------------------------------------
    // -------------------------------------------------------------------------------
    public static Font tableFont;
    
    /** Theme cache instance */
    public static final ThemeCache instance = new ThemeCache();
  
    // - Initialisation --------------------------------------------------------------
    // -------------------------------------------------------------------------------
    static {
        // Active background colors.
        backgroundColors[ACTIVE][NORMAL]    = ThemeManager.getCurrentColor(Theme.FILE_TABLE_BACKGROUND_COLOR);
        backgroundColors[ACTIVE][SELECTED]  = ThemeManager.getCurrentColor(Theme.FILE_TABLE_SELECTED_BACKGROUND_COLOR);
        backgroundColors[ACTIVE][ALTERNATE] = ThemeManager.getCurrentColor(Theme.FILE_TABLE_ALTERNATE_BACKGROUND_COLOR);
        backgroundColors[ACTIVE][SECONDARY] = ThemeManager.getCurrentColor(Theme.FILE_TABLE_SELECTED_SECONDARY_BACKGROUND_COLOR);

        // Inactive background colors.
        backgroundColors[INACTIVE][NORMAL]    = ThemeManager.getCurrentColor(Theme.FILE_TABLE_INACTIVE_BACKGROUND_COLOR);
        backgroundColors[INACTIVE][SELECTED]  = ThemeManager.getCurrentColor(Theme.FILE_TABLE_INACTIVE_SELECTED_BACKGROUND_COLOR);
        backgroundColors[INACTIVE][ALTERNATE] = ThemeManager.getCurrentColor(Theme.FILE_TABLE_INACTIVE_ALTERNATE_BACKGROUND_COLOR);
        backgroundColors[INACTIVE][SECONDARY] = ThemeManager.getCurrentColor(Theme.FILE_TABLE_INACTIVE_SELECTED_SECONDARY_BACKGROUND_COLOR);

        // Normal foreground foregroundColors.
        foregroundColors[ACTIVE][NORMAL][HIDDEN_FILE]     = ThemeManager.getCurrentColor(Theme.HIDDEN_FILE_FOREGROUND_COLOR);
        foregroundColors[ACTIVE][NORMAL][READ_ONLY]       = ThemeManager.getCurrentColor(Theme.READ_ONLY_FOREGROUND_COLOR);
        foregroundColors[ACTIVE][NORMAL][FOLDER]          = ThemeManager.getCurrentColor(Theme.FOLDER_FOREGROUND_COLOR);
        foregroundColors[ACTIVE][NORMAL][ARCHIVE]         = ThemeManager.getCurrentColor(Theme.ARCHIVE_FOREGROUND_COLOR);
        foregroundColors[ACTIVE][NORMAL][SYMLINK]         = ThemeManager.getCurrentColor(Theme.SYMLINK_FOREGROUND_COLOR);
        foregroundColors[ACTIVE][NORMAL][MARKED]          = ThemeManager.getCurrentColor(Theme.MARKED_FOREGROUND_COLOR);
        foregroundColors[ACTIVE][NORMAL][PLAIN_FILE]      = ThemeManager.getCurrentColor(Theme.FILE_FOREGROUND_COLOR);

        // Normal unfocused foreground foregroundColors.
        foregroundColors[INACTIVE][NORMAL][HIDDEN_FILE]    = ThemeManager.getCurrentColor(Theme.HIDDEN_FILE_INACTIVE_FOREGROUND_COLOR);
        foregroundColors[INACTIVE][NORMAL][READ_ONLY]      = ThemeManager.getCurrentColor(Theme.READ_ONLY_INACTIVE_FOREGROUND_COLOR);
        foregroundColors[INACTIVE][NORMAL][FOLDER]         = ThemeManager.getCurrentColor(Theme.FOLDER_INACTIVE_FOREGROUND_COLOR);
        foregroundColors[INACTIVE][NORMAL][ARCHIVE]        = ThemeManager.getCurrentColor(Theme.ARCHIVE_INACTIVE_FOREGROUND_COLOR);
        foregroundColors[INACTIVE][NORMAL][SYMLINK]        = ThemeManager.getCurrentColor(Theme.SYMLINK_INACTIVE_FOREGROUND_COLOR);
        foregroundColors[INACTIVE][NORMAL][MARKED]         = ThemeManager.getCurrentColor(Theme.MARKED_INACTIVE_FOREGROUND_COLOR);
        foregroundColors[INACTIVE][NORMAL][PLAIN_FILE]     = ThemeManager.getCurrentColor(Theme.FILE_INACTIVE_FOREGROUND_COLOR);

        // Selected foreground foregroundColors.
        foregroundColors[ACTIVE][SELECTED][HIDDEN_FILE]   = ThemeManager.getCurrentColor(Theme.HIDDEN_FILE_SELECTED_FOREGROUND_COLOR);
        foregroundColors[ACTIVE][SELECTED][READ_ONLY]     = ThemeManager.getCurrentColor(Theme.READ_ONLY_SELECTED_FOREGROUND_COLOR);
        foregroundColors[ACTIVE][SELECTED][FOLDER]        = ThemeManager.getCurrentColor(Theme.FOLDER_SELECTED_FOREGROUND_COLOR);
        foregroundColors[ACTIVE][SELECTED][ARCHIVE]       = ThemeManager.getCurrentColor(Theme.ARCHIVE_SELECTED_FOREGROUND_COLOR);
        foregroundColors[ACTIVE][SELECTED][SYMLINK]       = ThemeManager.getCurrentColor(Theme.SYMLINK_SELECTED_FOREGROUND_COLOR);
        foregroundColors[ACTIVE][SELECTED][MARKED]        = ThemeManager.getCurrentColor(Theme.MARKED_SELECTED_FOREGROUND_COLOR);
        foregroundColors[ACTIVE][SELECTED][PLAIN_FILE]    = ThemeManager.getCurrentColor(Theme.FILE_SELECTED_FOREGROUND_COLOR);

        // Selected unfocused foreground foregroundColors.
        foregroundColors[INACTIVE][SELECTED][HIDDEN_FILE]  = ThemeManager.getCurrentColor(Theme.HIDDEN_FILE_INACTIVE_SELECTED_FOREGROUND_COLOR);
        foregroundColors[INACTIVE][SELECTED][READ_ONLY]    = ThemeManager.getCurrentColor(Theme.READ_ONLY_INACTIVE_SELECTED_FOREGROUND_COLOR);
        foregroundColors[INACTIVE][SELECTED][FOLDER]       = ThemeManager.getCurrentColor(Theme.FOLDER_INACTIVE_SELECTED_FOREGROUND_COLOR);
        foregroundColors[INACTIVE][SELECTED][ARCHIVE]      = ThemeManager.getCurrentColor(Theme.ARCHIVE_INACTIVE_SELECTED_FOREGROUND_COLOR);
        foregroundColors[INACTIVE][SELECTED][SYMLINK]      = ThemeManager.getCurrentColor(Theme.SYMLINK_INACTIVE_SELECTED_FOREGROUND_COLOR);
        foregroundColors[INACTIVE][SELECTED][MARKED]       = ThemeManager.getCurrentColor(Theme.MARKED_INACTIVE_SELECTED_FOREGROUND_COLOR);
        foregroundColors[INACTIVE][SELECTED][PLAIN_FILE]   = ThemeManager.getCurrentColor(Theme.FILE_INACTIVE_SELECTED_FOREGROUND_COLOR);

        unmatchedForeground                                = ThemeManager.getCurrentColor(Theme.FILE_TABLE_UNMATCHED_FOREGROUND_COLOR);
        unmatchedBackground                                = ThemeManager.getCurrentColor(Theme.FILE_TABLE_UNMATCHED_BACKGROUND_COLOR);
        tableFont                                          = ThemeManager.getCurrentFont(Theme.FILE_TABLE_FONT);

        activeOutlineColor                                 = ThemeManager.getCurrentColor(Theme.FILE_TABLE_SELECTED_OUTLINE_COLOR);
        inactiveOutlineColor                               = ThemeManager.getCurrentColor(Theme.FILE_TABLE_INACTIVE_SELECTED_OUTLINE_COLOR);

        ThemeManager.addCurrentThemeListener(instance);
    }

   
    /**
     * Strong-ref listener set. The previous WeakHashMap-as-set
     * silently dropped anonymous-class listeners as soon as the
     * caller's local reference went out of scope, so theme changes
     * stopped firing. Callers are now responsible for matching
     * remove calls.
     */
    private static final Set<ThemeListener> listeners = new CopyOnWriteArraySet<>();


    private ThemeCache() {
	}

    public static void addThemeListener(ThemeListener listener) {
        listeners.add(listener);
    }

    public static void removeThemeListener(ThemeListener listener) {
        listeners.remove(listener);
    }

    public static Color foregroundColor(int focusIndex, int selectionIndex, int fileKindIndex) {
        return foregroundColors[focusIndex][selectionIndex][fileKindIndex];
    }

    public static Color backgroundColor(int focusIndex, int backgroundIndex) {
        return backgroundColors[focusIndex][backgroundIndex];
    }

    private static void fireColorChanged(ColorChangedEvent event) {
        listeners.forEach(listener -> listener.colorChanged(event));
    }

    private static void fireFontChanged(FontChangedEvent event) {
        listeners.forEach(listener -> listener.fontChanged(event));
    }
    

    // - Theme listening -------------------------------------------------------------
    // -------------------------------------------------------------------------------
    /**
     * Receives theme color changes notifications.
     */
    public void colorChanged(ColorChangedEvent event) {
        switch(event.getColorId()) {
            // Plain file color.
        case Theme.FILE_FOREGROUND_COLOR:
            foregroundColors[ACTIVE][NORMAL][PLAIN_FILE] = event.getColor();
            break;

            // Selected file color.
        case Theme.FILE_SELECTED_FOREGROUND_COLOR:
            foregroundColors[ACTIVE][SELECTED][PLAIN_FILE] = event.getColor();
            break;

            // Hidden files.
        case Theme.HIDDEN_FILE_FOREGROUND_COLOR:
            foregroundColors[ACTIVE][NORMAL][HIDDEN_FILE] = event.getColor();
            break;

            // Selected hidden files.
        case Theme.HIDDEN_FILE_SELECTED_FOREGROUND_COLOR:
            foregroundColors[ACTIVE][SELECTED][HIDDEN_FILE] = event.getColor();
            break;

            // Folders.
        case Theme.FOLDER_FOREGROUND_COLOR:
            foregroundColors[ACTIVE][NORMAL][FOLDER] = event.getColor();
            break;

            // Selected folders.
        case Theme.FOLDER_SELECTED_FOREGROUND_COLOR:
            foregroundColors[ACTIVE][SELECTED][FOLDER] = event.getColor();
            break;

            // Archives.
        case Theme.ARCHIVE_FOREGROUND_COLOR:
            foregroundColors[ACTIVE][NORMAL][ARCHIVE] = event.getColor();
            break;

            // Selected archives.
        case Theme.ARCHIVE_SELECTED_FOREGROUND_COLOR:
            foregroundColors[ACTIVE][SELECTED][ARCHIVE] = event.getColor();
            break;

            // Symlinks.
        case Theme.SYMLINK_FOREGROUND_COLOR:
            foregroundColors[ACTIVE][NORMAL][SYMLINK] = event.getColor();
            break;

            // Selected symlinks.
        case Theme.SYMLINK_SELECTED_FOREGROUND_COLOR:
            foregroundColors[ACTIVE][SELECTED][SYMLINK] = event.getColor();
            break;

            // Read-only
        case Theme.READ_ONLY_FOREGROUND_COLOR:
            foregroundColors[ACTIVE][NORMAL][READ_ONLY] = event.getColor();
            break;

            // Selected read-only
        case Theme.READ_ONLY_SELECTED_FOREGROUND_COLOR:
            foregroundColors[ACTIVE][SELECTED][READ_ONLY] = event.getColor();
            break;

            // Marked files.
        case Theme.MARKED_FOREGROUND_COLOR:
            foregroundColors[ACTIVE][NORMAL][MARKED] = event.getColor();
            break;

            // Selected marked files.
        case Theme.MARKED_SELECTED_FOREGROUND_COLOR:
            foregroundColors[ACTIVE][SELECTED][MARKED] = event.getColor();
            break;

            // Plain file color.
        case Theme.FILE_INACTIVE_FOREGROUND_COLOR:
            foregroundColors[INACTIVE][NORMAL][PLAIN_FILE] = event.getColor();
            break;

            // Selected file color.
        case Theme.FILE_INACTIVE_SELECTED_FOREGROUND_COLOR:
            foregroundColors[INACTIVE][SELECTED][PLAIN_FILE] = event.getColor();
            break;

            // Hidden files.
        case Theme.HIDDEN_FILE_INACTIVE_FOREGROUND_COLOR:
            foregroundColors[INACTIVE][NORMAL][HIDDEN_FILE] = event.getColor();
            break;

            // Selected hidden files.
        case Theme.HIDDEN_FILE_INACTIVE_SELECTED_FOREGROUND_COLOR:
            foregroundColors[INACTIVE][SELECTED][HIDDEN_FILE] = event.getColor();
            break;

            // Folders.
        case Theme.FOLDER_INACTIVE_FOREGROUND_COLOR:
            foregroundColors[INACTIVE][NORMAL][FOLDER] = event.getColor();
            break;

            // Selected folders.
        case Theme.FOLDER_INACTIVE_SELECTED_FOREGROUND_COLOR:
            foregroundColors[INACTIVE][SELECTED][FOLDER] = event.getColor();
            break;

            // Archives.
        case Theme.ARCHIVE_INACTIVE_FOREGROUND_COLOR:
            foregroundColors[INACTIVE][NORMAL][ARCHIVE] = event.getColor();
            break;

            // Selected archives.
        case Theme.ARCHIVE_INACTIVE_SELECTED_FOREGROUND_COLOR:
            foregroundColors[INACTIVE][SELECTED][ARCHIVE] = event.getColor();
            break;

            // Symlinks.
        case Theme.SYMLINK_INACTIVE_FOREGROUND_COLOR:
            foregroundColors[INACTIVE][NORMAL][SYMLINK] = event.getColor();
            break;

            // Selected symlinks.
        case Theme.SYMLINK_INACTIVE_SELECTED_FOREGROUND_COLOR:
            foregroundColors[INACTIVE][SELECTED][SYMLINK] = event.getColor();
            break;

            // Read-only
        case Theme.READ_ONLY_INACTIVE_FOREGROUND_COLOR:
            foregroundColors[INACTIVE][NORMAL][READ_ONLY] = event.getColor();
            break;

            // Selected read-only
        case Theme.READ_ONLY_INACTIVE_SELECTED_FOREGROUND_COLOR:
            foregroundColors[INACTIVE][SELECTED][READ_ONLY] = event.getColor();
            break;

            // Marked files.
        case Theme.MARKED_INACTIVE_FOREGROUND_COLOR:
            foregroundColors[INACTIVE][NORMAL][MARKED] = event.getColor();
            break;

            // Selected marked files.
        case Theme.MARKED_INACTIVE_SELECTED_FOREGROUND_COLOR:
            foregroundColors[INACTIVE][SELECTED][MARKED] = event.getColor();
            break;

            // Unmatched foreground
        case Theme.FILE_TABLE_UNMATCHED_FOREGROUND_COLOR:
            setUnmatchedForeground(event.getColor());
            break;

            // Unmached background
        case Theme.FILE_TABLE_UNMATCHED_BACKGROUND_COLOR:
            setUnmatchedBackground(event.getColor());
            break;

            // Active normal background.
        case Theme.FILE_TABLE_BACKGROUND_COLOR:
            backgroundColors[ACTIVE][NORMAL] = event.getColor();
            break;

            // Active selected background.
        case Theme.FILE_TABLE_SELECTED_BACKGROUND_COLOR:
            backgroundColors[ACTIVE][SELECTED] = event.getColor();
            break;

            // Active alternate background.
        case Theme.FILE_TABLE_ALTERNATE_BACKGROUND_COLOR:
            backgroundColors[ACTIVE][ALTERNATE] = event.getColor();
            break;

            // Inactive normal background.
        case Theme.FILE_TABLE_INACTIVE_BACKGROUND_COLOR:
            backgroundColors[INACTIVE][NORMAL] = event.getColor();
            break;

            // Inactive selected background.
        case Theme.FILE_TABLE_INACTIVE_SELECTED_BACKGROUND_COLOR:
            backgroundColors[INACTIVE][SELECTED] = event.getColor();
            break;

            // Inactive alternate background.
        case Theme.FILE_TABLE_INACTIVE_ALTERNATE_BACKGROUND_COLOR:
            backgroundColors[INACTIVE][ALTERNATE] = event.getColor();
            break;

            // Active selection outline.
        case Theme.FILE_TABLE_SELECTED_OUTLINE_COLOR:
            setActiveOutlineColor(event.getColor());
            break;

            // Inactive selection outline.
        case Theme.FILE_TABLE_INACTIVE_SELECTED_OUTLINE_COLOR:
            setInactiveOutlineColor(event.getColor());
            break;

            // Secondary background color.
        case Theme.FILE_TABLE_SELECTED_SECONDARY_BACKGROUND_COLOR:
            backgroundColors[ACTIVE][SECONDARY] = event.getColor();
            break;

            // Inactive secondary background color.
        case Theme.FILE_TABLE_INACTIVE_SELECTED_SECONDARY_BACKGROUND_COLOR:
            backgroundColors[INACTIVE][SECONDARY] = event.getColor();
            break;

        default:
            return;
        }
        fireColorChanged(event);
    }

    /**
     * Receives theme font changes notifications.
     */
    public void fontChanged(FontChangedEvent event) {
    	switch (event.getFontId()) {
    	case Theme.FILE_TABLE_FONT:
            setTableFont(event.getFont());
    		break;
   		default:
   		    return;
     	}
    	fireFontChanged(event);
    }

    // Static setters so the colorChanged / fontChanged instance
    // methods (forced to be instance because they implement
    // ThemeListener) don't write directly to scalar static fields,
    // which trips SpotBugs ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD.
    // The color arrays stay private so callers cannot mutate cache
    // storage; renderers read through the small static accessors above.
    private static void setUnmatchedForeground(java.awt.Color c) { unmatchedForeground = c; }
    private static void setUnmatchedBackground(java.awt.Color c) { unmatchedBackground = c; }
    private static void setActiveOutlineColor  (java.awt.Color c) { activeOutlineColor   = c; }
    private static void setInactiveOutlineColor(java.awt.Color c) { inactiveOutlineColor = c; }
    private static void setTableFont(java.awt.Font f) { tableFont = f; }
}
