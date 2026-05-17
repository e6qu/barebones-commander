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

package dev.barebones.commander.ui.dialog.startup;

import dev.barebones.commander.VersionChecker;
import dev.barebones.commander.conf.MuConfigurations;
import dev.barebones.commander.conf.MuPreference;
import dev.barebones.commander.conf.MuPreferences;
import dev.barebones.commander.core.desktop.DesktopManager;
import dev.barebones.commander.desktop.ActionType;
import dev.barebones.commander.text.Translator;
import dev.barebones.commander.ui.action.ActionProperties;
import dev.barebones.commander.ui.dialog.DialogAction;
import dev.barebones.commander.ui.dialog.InformationDialog;
import dev.barebones.commander.ui.dialog.QuestionDialog;
import dev.barebones.commander.ui.layout.InformationPane;
import dev.barebones.commander.ui.main.MainFrame;
import dev.barebones.commander.commons.logging.Logger;
import dev.barebones.commander.commons.logging.LoggerFactory;

import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * This class takes care of retrieving the information about the latest barebones-commander version from a remote server and
 * displaying the result to the end user.
 *
 * @author Maxence Bernard
 */
public class CheckVersionDialog extends QuestionDialog {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckVersionDialog.class);

    /**
     * Parent MainFrame instance
     */
    private MainFrame mainFrame;

    /**
     * true if the user manually clicked on the 'Check for updates' menu item,
     * false if the update check was automatically triggered on startup
     */
    private boolean userInitiated;

    /**
     * Dialog's width has to be at least 240
     */
    private final static Dimension MINIMUM_DIALOG_DIMENSION = new Dimension(320, 0);

    private record VersionCheckResult(boolean showDialog, String title, String message, URL downloadURL, boolean downloadOption) {
    }

    public enum CheckVersionAction implements DialogAction {

        OK(Translator.get("ok")),
        // TODO not nice (to load the name into enum and in this way)
        GO_TO_WEBSITE(ActionProperties.getActionLabel(ActionType.GoToWebsite)),
        INSTALL_AND_RESTART("COMMENTED-OUT");

        private final String actionName;

        CheckVersionAction(String actionName) {
            this.actionName = actionName;
        }

        @Override
        public String getActionName() {
            return actionName;
        }
    }

    /**
     * Checks for updates and notifies the user of the outcome. The check itself is performed in a separate thread
     * to prevent the app from waiting for the request's result.
     *
     * @param userInitiated true if the user manually clicked on the 'Check for updates' menu item,
     *                      false if the update check was automatically triggered on startup. If the check was automatically triggered,
     *                      the user won't be notified if there is no new version (current version is the latest).
     */
    public CheckVersionDialog(MainFrame mainFrame, boolean userInitiated) {
        super(mainFrame.getJFrame(), "", mainFrame.getJFrame());
        this.mainFrame = mainFrame;
        this.userInitiated = userInitiated;

        // Do all the hard work in a separate thread
        new SwingWorker<VersionCheckResult, Void>() {
            @Override
            protected VersionCheckResult doInBackground() {
                return checkVersion();
            }

            @Override
            protected void done() {
                try {
                    VersionCheckResult result = get();
                    SwingUtilities.invokeLater(() -> showVersionCheckResult(result));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    dispose();
                } catch (ExecutionException e) {
                    LOGGER.warn("Failed to complete version check", e.getCause());
                    if (userInitiated) {
                        VersionCheckResult result = new VersionCheckResult(true,
                                Translator.get("version_dialog.not_available_title"),
                                Translator.get("version_dialog.not_available"),
                                null,
                                false);
                        SwingUtilities.invokeLater(() -> showVersionCheckResult(result));
                    } else {
                        dispose();
                    }
                }
            }
        }.execute();
    }


    /**
     * Checks for updates and notifies the user of the outcome.
     */
    private VersionCheckResult checkVersion() {
        String message;
        String title;
        VersionChecker version;
        URL downloadURL = null;
        boolean downloadOption = false;

        try {
            LOGGER.debug("Checking for new version...");

            version = VersionChecker.getInstance();
            // A newer version is available
            if (version.isNewVersionAvailable()) {
                LOGGER.info("A new version is available!");

                title = Translator.get("version_dialog.new_version_title");

                // Checks if the current platform can open a new browser window
                downloadURL = URI.create(version.getDownloadURL()).toURL();
                downloadOption = DesktopManager.isOperationSupported(DesktopManager.BROWSE, new Object[]{downloadURL});

                // If the platform is not capable of opening a new browser window,
                // display the download URL.
                if (downloadOption) {
                    message = Translator.get("version_dialog.new_version");
                } else {
                    message = Translator.get("version_dialog.new_version_url", downloadURL.toString());
                }
            }
            // We're already running latest version
            else {
                LOGGER.debug("No new version.");

                // If the version check was not iniated by the user (i.e. was automatic),
                // we do not need to inform the user that he already has the latest version
                if (!userInitiated) {
                    return new VersionCheckResult(false, null, null, null, false);
                }

                title = Translator.get("version_dialog.no_new_version_title");
                message = Translator.get("version_dialog.no_new_version");
            }
        }
        // Check failed
        catch (Exception e) {
            // If the version check was not initiated by the user (i.e. was automatic),
            // we do not need to inform the user that the check failed
            if (!userInitiated) {
                LOGGER.debug("Automatic version check failed", e);
                return new VersionCheckResult(false, null, null, null, false);
            }

            LOGGER.warn("User-initiated version check failed", e);
            title = Translator.get("version_dialog.not_available_title");
            message = Translator.get("version_dialog.not_available");
        }

        return new VersionCheckResult(true, title, message, downloadURL, downloadOption);
    }

    private void showVersionCheckResult(VersionCheckResult result) {
        if (!result.showDialog()) {
            dispose();
            return;
        }

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        // Set title
        setTitle(result.title());

        List<DialogAction> actions = new ArrayList<>();
        actions.add(CheckVersionAction.OK);

        // 'Go to website' choice (if available)
        if (result.downloadOption()) {
            actions.add(CheckVersionAction.GO_TO_WEBSITE);
        }

        init(new InformationPane(result.message(), null, Font.PLAIN, InformationPane.INFORMATION_ICON),
                actions,
                0);

        JCheckBox showNextTimeCheckBox = new JCheckBox(Translator.get("prefs_dialog.check_for_updates_on_startup"),
                MuConfigurations.getPreferences().getVariable(MuPreference.CHECK_FOR_UPDATE,
                        MuPreferences.DEFAULT_CHECK_FOR_UPDATE));
        addComponent(showNextTimeCheckBox);

        setMinimumSize(MINIMUM_DIALOG_DIMENSION);

        // Show dialog and get user action
        DialogAction action = getActionValue();

        if (action == CheckVersionAction.GO_TO_WEBSITE) {
            try {
                DesktopManager.executeOperation(DesktopManager.BROWSE, new Object[]{result.downloadURL()});
            } catch (Exception e) {
                InformationDialog.showErrorDialog(this);
            }
        }

        // Remember user preference
        MuConfigurations.getPreferences().setVariable(MuPreference.CHECK_FOR_UPDATE, showNextTimeCheckBox.isSelected());
    }
}
