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

package dev.barebones.commander.ui.main;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import dev.barebones.commander.commons.logging.Logger;
import dev.barebones.commander.commons.logging.LoggerFactory;

import dev.barebones.commander.bookmark.Bookmark;
import dev.barebones.commander.bookmark.BookmarkListener;
import dev.barebones.commander.bookmark.BookmarkManager;
import dev.barebones.commander.bookmark.file.BookmarkProtocolProvider;
import dev.barebones.commander.commons.conf.ConfigurationEvent;
import dev.barebones.commander.commons.conf.ConfigurationListener;
import dev.barebones.commander.commons.file.AbstractFile;
import dev.barebones.commander.commons.file.FileFactory;
import dev.barebones.commander.commons.file.FileURL;
import dev.barebones.commander.commons.file.filter.PathFilter;
import dev.barebones.commander.commons.file.filter.RegexpPathFilter;
import dev.barebones.commander.commons.file.protocol.FileProtocols;
import dev.barebones.commander.commons.file.protocol.local.LocalFile;
import dev.barebones.commander.commons.file.protocol.search.SearchFile;
import dev.barebones.commander.commons.runtime.OsFamily;
import dev.barebones.commander.commons.util.ui.helper.MnemonicHelper;
import dev.barebones.commander.conf.MuConfigurations;
import dev.barebones.commander.conf.MuPreference;
import dev.barebones.commander.conf.MuPreferences;
import dev.barebones.commander.protocol.ui.ProtocolPanelProvider;
import dev.barebones.commander.protocol.ui.ServerPanel;
import dev.barebones.commander.text.Translator;
import dev.barebones.commander.ui.action.MuAction;
import dev.barebones.commander.ui.action.impl.OpenLocationAction;
import dev.barebones.commander.ui.button.PopupButton;
import dev.barebones.commander.ui.dialog.server.ServerConnectDialog;
import dev.barebones.commander.ui.event.LocationEvent;
import dev.barebones.commander.ui.event.LocationListener;
import dev.barebones.commander.ui.icon.CustomFileIconProvider;
import dev.barebones.commander.ui.icon.FileIcons;
import dev.barebones.commander.ui.icon.IconManager;

/**
 * <code>DrivePopupButton</code> is a button which, when clicked, pops up a menu with a list of volumes items that be
 * used to change the current folder.
 *
 * @author Maxence Bernard
 */
public class DrivePopupButton extends PopupButton implements BookmarkListener, ConfigurationListener, LocationListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(DrivePopupButton.class);

    /** FolderPanel instance that contains this button */
    private FolderPanel folderPanel;

    /** Current volumes */
    private static AbstractFile volumes[];

    /** Caches drive icons */
    private static Map<AbstractFile, Icon> iconCache = new Hashtable<AbstractFile, Icon>();

    /**
     * Filters out volumes from the list based on the exclude regexp defined in the configuration, null if the regexp is
     * not defined.
     */
    private static PathFilter volumeFilter;

    /** ProtocolPanelProviders that we should make shortcuts for */
    private static Map<String, ProtocolPanelProvider> schemaToPanelProvider = new HashMap<>();

    static {
        try {
            String excludeRegexp = MuConfigurations.getPreferences().getVariable(MuPreference.VOLUME_EXCLUDE_REGEXP);
            if (excludeRegexp != null) {
                volumeFilter = new RegexpPathFilter(excludeRegexp, true);
                volumeFilter.setInverted(true);
            }
        } catch (PatternSyntaxException e) {
            LOGGER.info("Invalid regexp for conf variable " + MuPreferences.VOLUME_EXCLUDE_REGEXP, e);
        }

        // Initialize the volumes list
        volumes = getDisplayableVolumes();
    }

    /**
     * Creates a new <code>DrivePopupButton</code> which is to be added to the given FolderPanel.
     *
     * @param folderPanel
     *            the FolderPanel instance this button will be added to
     */
    public DrivePopupButton(FolderPanel folderPanel) {
        this.folderPanel = folderPanel;

        // Listen to location events to update the button when the current folder changes
        folderPanel.getLocationManager().addLocationListener(this);

        // Listen to bookmark changes to update the button if a bookmark corresponding to the current folder
        // has been added/edited/removed
        BookmarkManager.addBookmarkListener(this);

        // Listen to configuration changes to update the button if the system file icons policy has changed
        MuConfigurations.addPreferencesListener(this);

        if (OsFamily.MAC_OS.isCurrent()) {
            setMargin(new Insets(1, 1, 1, 1));
            putClientProperty("JComponent.sizeVariant", "small");
            putClientProperty("JButton.buttonType", "textured");
        }
    }

    /**
     * Updates the button's label and icon to reflect the current folder and match one of the current volumes: <
     * <ul>
     * <li>If the specified folder corresponds to a bookmark, the bookmark's name will be displayed
     * <li>If the specified folder corresponds to a local file, the enclosing volume's name will be displayed
     * <li>If the specified folder corresponds to a remote file, the protocol's name will be displayed
     * </ul>
     * The button's icon will be the current folder's one.
     */
    private void updateButton() {
        AbstractFile currentFolder = folderPanel.getCurrentFolder();
        String currentPath = currentFolder.getAbsolutePath();
        FileURL currentURL = currentFolder.getURL();

        // First try to find a bookmark matching the specified folder
        for (Bookmark bookmark : BookmarkManager.getBookmarks()) {
            if (currentPath.equals(bookmark.getLocation())) {
                // Note: if several bookmarks match current folder, the first one will be used
                setText(bookmark.getName());
                setIcon(IconManager.getIcon(IconManager.FILE_ICON_SET, CustomFileIconProvider.BOOKMARK_ICON_NAME));
                return;
            }
        }

        // If no bookmark matched current folder
        String protocol = currentURL.getScheme();
        switch (protocol) {
        // Local file, use volume's name
        case LocalFile.SCHEMA:
            String newLabel = null;
            currentPath = currentFolder.getCanonicalPath(false).toLowerCase();

            int bestLength = -1;
            int bestIndex = 0;
            String temp;
            int len;
            for (int i = 0; i < volumes.length; i++) {
                temp = volumes[i].getCanonicalPath(false).toLowerCase();

                len = temp.length();
                if (currentPath.startsWith(temp) && len > bestLength) {
                    bestIndex = i;
                    bestLength = len;
                }
            }
            newLabel = volumes[bestIndex].getName();
            setText(newLabel);
            setIcon(FileIcons.getFileIcon(currentFolder));
            break;

        case BookmarkProtocolProvider.BOOKMARK:
            String currentFolderName = currentFolder.getName();
            setText(currentFolderName.isEmpty() ? Translator.get("bookmarks_menu") : currentFolderName);
            setIcon(IconManager.getIcon(IconManager.FILE_ICON_SET, CustomFileIconProvider.BOOKMARKS_ICON_NAME));
            break;

        case SearchFile.SCHEMA:
            setText(Translator.get("find"));
            setIcon(IconManager.getIcon(IconManager.FILE_ICON_SET, CustomFileIconProvider.FIND_RESULT_ICON_NAME));
            break;

        case "gdrive":
            setText(Translator.get("gdrive"));
            setIcon(IconManager.getIcon(IconManager.FILE_ICON_SET, CustomFileIconProvider.GOOGLE_DRIVE_ICON_NAME));
            break;

        case "dropbox":
            setText(Translator.get("dropbox"));
            setIcon(IconManager.getIcon(IconManager.FILE_ICON_SET, CustomFileIconProvider.DROPBOX_ICON_NAME));
            break;

        default:
            // Remote file, use the protocol's name
            setText(protocol.toUpperCase());
            setIcon(FileIcons.getFileIcon(currentFolder));
        }

    }

    /**
     * Returns the list of volumes to be displayed in the popup menu.
     *
     * <p>
     * The raw list of volumes is fetched using {@link LocalFile#getVolumes()} and then filtered using the regexp
     * defined in the {@link MuPreferences#VOLUME_EXCLUDE_REGEXP} configuration variable (if defined).
     * </p>
     *
     * @return the list of volumes to be displayed in the popup menu
     */
    public static AbstractFile[] getDisplayableVolumes() {
        var volumes = Arrays.stream(LocalFile.getVolumes());

        if (volumeFilter != null)
            volumes = volumes.filter(volumeFilter::match);

        return volumes.sorted(Comparator.comparing(AbstractFile::getName)).toArray(AbstractFile[]::new);
    }

    ////////////////////////////////
    // PopupButton implementation //
    ////////////////////////////////

    @Override
    public JPopupMenu getPopupMenu() {
        JPopupMenu popupMenu = new JPopupMenu();

        // Update the list of volumes in case new ones were mounted
        volumes = getDisplayableVolumes();

        // Add volumes
        int nbVolumes = volumes.length;
        final MainFrame mainFrame = folderPanel.getMainFrame();

        MnemonicHelper mnemonicHelper = new MnemonicHelper(); // Provides mnemonics and ensures uniqueness
        JMenuItem item;
        MuAction action;
        String volumeName;

        ArrayList<JMenuItem> itemsV = new ArrayList<JMenuItem>();

        for (int i = 0; i < nbVolumes; i++) {
            action = new OpenLocationAction(mainFrame, Collections.emptyMap(), volumes[i]) {
                /**
                 * Changes the current folder on the {@link FolderPanel} that contains this
                 * button, instead of the currently active {@link FolderPanel}
                 */
                @Override
                protected FolderPanel getFolderPanel() {
                    return folderPanel;
                }
            };
            volumeName = volumes[i].getName();

            // If several volumes have the same filename, use the volume's path for the action's label instead of the
            // volume's path, to disambiguate
            for (int j = 0; j < nbVolumes; j++) {
                if (j != i && volumes[j].getName().equalsIgnoreCase(volumeName)) {
                    action.setLabel(volumes[i].getAbsolutePath());
                    break;
                }
            }

            item = popupMenu.add(action);
            setMnemonic(item, mnemonicHelper);

            // Set icon from cache
            Icon icon = iconCache.get(volumes[i]);
            if (icon != null) {
                item.setIcon(icon);
            }

            itemsV.add(item); // JMenu offers no way to retrieve a particular JMenuItem, so we have to keep them
        }

        new RefreshDriveNamesAndIcons(popupMenu, itemsV).start();

        popupMenu.add(new JSeparator());

        // Add boookmarks
        var bookmarks = BookmarkManager.getBookmarks();
        if (!bookmarks.isEmpty()) {
            for (Bookmark bookmark : bookmarks) {
                action = new OpenLocationAction(mainFrame, Collections.emptyMap(), bookmark) {
                    /**
                     * Changes the current folder on the {@link FolderPanel} that contains this
                     * button, instead of the currently active {@link FolderPanel}
                     */
                    @Override
                    protected FolderPanel getFolderPanel() {
                        return folderPanel;
                    }
                };
                item = popupMenu.add(action);
                setMnemonic(item, mnemonicHelper);
            }
        } else {
            // No bookmark : add a disabled menu item saying there is no bookmark
            popupMenu.add(Translator.get("bookmarks_menu.no_bookmark")).setEnabled(false);
        }

        popupMenu.add(new JSeparator());

        // Add 'Network shares' shortcut
        if (FileFactory.isRegisteredProtocol(FileProtocols.SMB)) {
            var bookmark = new Bookmark(Translator.get("drive_popup.network_shares"), "smb:///");
            action = new OpenLocationAction(mainFrame, Collections.emptyMap(), bookmark) {
                /**
                 * Changes the current folder on the {@link FolderPanel} that contains this
                 * button, instead of the currently active {@link FolderPanel}
                 */
                @Override
                protected FolderPanel getFolderPanel() {
                    return folderPanel;
                }
            };
            action.setIcon(IconManager.getIcon(IconManager.FILE_ICON_SET, CustomFileIconProvider.NETWORK_ICON_NAME));
            setMnemonic(popupMenu.add(action), mnemonicHelper);
        }
        popupMenu.add(new JSeparator());

        // Add 'connect to server' shortcuts
        schemaToPanelProvider.values()
                .stream()
                .sorted(Comparator.comparing(ProtocolPanelProvider::priority))
                .map(this::toServerConnectAction)
                .map(popupMenu::add)
                .forEach(menuItem -> setMnemonic(menuItem, mnemonicHelper));

        return popupMenu;
    }

    /**
     * Registers an instance of {@link ProtocolPanelProvider} to the drive popup buttons, this will add shortcut to the
     * relevant 'connect to server' dialog.
     * 
     * @param protocolPanelProvider
     *            the {@link ProtocolPanelProvider} to register.
     */
    public static void register(ProtocolPanelProvider protocolPanelProvider) {
        schemaToPanelProvider.put(protocolPanelProvider.getSchema(), protocolPanelProvider);
    }

    /**
     * Unregisters an instance of {@link ProtocolPanelProvider}.
     * 
     * @see {@link #register(ProtocolPanelProvider)}}
     * @param protocolPanelProvider
     *            an instance of {@link ProtocolPanelProvider} to unregister.
     */
    public static void unregister(ProtocolPanelProvider protocolPanelProvider) {
        schemaToPanelProvider.remove(protocolPanelProvider.getSchema());
    }

    /**
     * Converts an instance of {@link ProtocolPanelProvider} to an instance of {@link ServerConnectAction}.
     * 
     * @param service
     *            an instance of {@link ProtocolPanelProvider} to convert.
     * @return an instance of of {@link ProtocolPanelProvider} that corresponds to the given instance of
     *         {@link ServerConnectAction}.
     */
    private ServerConnectAction toServerConnectAction(ProtocolPanelProvider service) {
        String label = service.getSchema().toUpperCase() + "...";
        return new ServerConnectAction(label, service.getPanelClass());
    }

    private class RefreshDriveNamesAndIcons extends Thread {

        private JPopupMenu popupMenu;
        private ArrayList<JMenuItem> items;

        public RefreshDriveNamesAndIcons(JPopupMenu popupMenu, ArrayList<JMenuItem> items) {
            super("RefreshDriveNamesAndIcons");
            this.popupMenu = popupMenu;
            this.items = items;
        }

        @Override
        public void run() {
            for (int i = 0; i < items.size(); i++) {
                final JMenuItem item = items.get(i);

                // Set system icon for volumes, only if system icons are available on the current platform
                final Icon icon = FileIcons.hasProperSystemIcons() ? FileIcons.getSystemFileIcon(volumes[i]) : null;
                if (icon != null) {
                    iconCache.put(volumes[i], icon);
                }

                SwingUtilities.invokeLater(() -> {
                    if (icon != null) {
                        item.setIcon(icon);
                    }
                });
            }

            // Re-calculate the popup menu's dimensions
            SwingUtilities.invokeLater(() -> {
                popupMenu.invalidate();
                popupMenu.pack();
            });
        }

    }

    /**
     * Convenience method that sets a mnemonic to the given JMenuItem, using the specified MnemonicHelper.
     *
     * @param menuItem
     *            the menu item for which to set a mnemonic
     * @param mnemonicHelper
     *            the MnemonicHelper instance to be used to determine the mnemonic's character.
     */
    private void setMnemonic(JMenuItem menuItem, MnemonicHelper mnemonicHelper) {
        menuItem.setMnemonic(mnemonicHelper.getMnemonic(menuItem.getText()));
    }

    //////////////////////////////
    // BookmarkListener methods //
    //////////////////////////////

    public void bookmarksChanged() {
        // Refresh label in case a bookmark with the current location was changed
        updateButton();
    }

    ///////////////////////////////////
    // ConfigurationListener methods //
    ///////////////////////////////////

    /**
     * Listens to certain configuration variables.
     */
    public void configurationChanged(ConfigurationEvent event) {
        String var = event.getVariable();

        // Update the button's icon if the system file icons policy has changed
        if (var.equals(MuPreferences.USE_SYSTEM_FILE_ICONS))
            updateButton();
    }

    ////////////////////////
    // Overridden methods //
    ////////////////////////

    @Override
    public Dimension getPreferredSize() {
        // Limit button's maximum width to something reasonable and leave enough space for location field,
        // as bookmarks name can be as long as users want them to be.
        // Note: would be better to use JButton.setMaximumSize() but it doesn't seem to work
        Dimension d = super.getPreferredSize();
        if (d.width > 160)
            d.width = 160;
        return d;
    }

    ///////////////////
    // Inner classes //
    ///////////////////

    /**
     * This action pops up {@link dev.barebones.commander.ui.dialog.server.ServerConnectDialog} for a specified protocol.
     */
    private class ServerConnectAction extends AbstractAction {
        private Class<? extends ServerPanel> serverPanelClass;

        private ServerConnectAction(String label, Class<? extends ServerPanel> serverPanelClass) {
            super(label);
            this.serverPanelClass = serverPanelClass;
        }

        public void actionPerformed(ActionEvent actionEvent) {
            new ServerConnectDialog(folderPanel, serverPanelClass).showDialog();
        }
    }

    /**********************************
     * LocationListener Implementation
     **********************************/

    public void locationChanged(LocationEvent e) {
        // Update the button's label to reflect the new current folder
        updateButton();
    }
}
