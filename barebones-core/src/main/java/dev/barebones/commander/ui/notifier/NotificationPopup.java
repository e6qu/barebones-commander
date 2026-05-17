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
package dev.barebones.commander.ui.notifier;

import dev.barebones.commander.commons.logging.Logger;
import dev.barebones.commander.commons.logging.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;

/**
 * A singleton class that shows notification popup in the provided frame (for example main frame).
 */
final class NotificationPopup {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationPopup.class);

    /**
     * Y-Position of notification popup as percentage of main frame height.
     */
    private static final float BOTTOM_POSITION = 0.9f;

    /**
     * Default opacity of popup.
     */
    private static final float OPACITY = 0.8f;

    private Timer closingTimer;

    private final CustomPopupMenu popup;
    private final JPanel panel;
    private final JLabel labelText;

    private CustomPopupMenuListener popupListener;

    /**
     * Used to re-center notification when we know panel width.
     */
    private class CustomPopupMenuListener implements PopupMenuListener {

        JFrame mainFrame;

        public void setMainFame(JFrame mainFrame) {
            this.mainFrame = mainFrame;
        }

        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            if (mainFrame != null) {
                SwingUtilities.invokeLater(() -> {
                    // now panel has a proper width and we can correctly re-center
                    Point position = getPosition(mainFrame, panel.getWidth());
                    popup.setLocation(mainFrame.getLocation().x + position.x,
                            mainFrame.getLocation().y + position.y);
                    setOpacity(popup, OPACITY);
                });
            }
        }

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        }

        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {
        }
    };

    private class CustomPopupMenu extends JPopupMenu {
        @Override
        public Insets getInsets() {
            return new Insets(0, 0, 0, 0);
        }

        @Override
        public void setVisible(boolean visible) {
            // ignore hiding, useful to force popup to stay if dialog is shown
            if (visible) {
                super.setVisible(visible);
            }
        }

        /**
         * Hide popup (setVisible ignores hiding)
         */
        private void hidePopup() {
            super.setVisible(false);
        }
    }

    private NotificationPopup() {
        popupListener = new CustomPopupMenuListener();

        popup = new CustomPopupMenu();
        popup.setFocusable(false);
        popup.setLayout(new BorderLayout());
        popup.addPopupMenuListener(popupListener);
        popup.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                popup.hidePopup();
            }
        });
        panel = new JPanel() {
            @Override
            public Insets getInsets() {
                return new Insets(0, 50, 0, 50);
            }
        };
        panel.setBorder(BorderFactory.createLineBorder(Color.black, 1));

        labelText = new JLabel();
        panel.add(labelText);
        popup.add(panel, BorderLayout.CENTER);
    }

    private static NotificationPopup instance;

    public static NotificationPopup getInstance() {
        synchronized (NotificationPopup.class) {
            if (instance != null) {
                return instance;
            }
        }
        if (SwingUtilities.isEventDispatchThread()) {
            return createInstance();
        }
        final NotificationPopup[] result = new NotificationPopup[1];
        try {
            SwingUtilities.invokeAndWait(() -> result[0] = createInstance());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while creating notification popup", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Failed to create notification popup", e.getCause());
        }
        return result[0];
    }

    private static synchronized NotificationPopup createInstance() {
        if (instance == null) {
            instance = new NotificationPopup();
        }
        return instance;
    }

    /**
     * Displays overlay notification popup in the given main frame, the notification
     * disappears after provider timeout.
     *
     * @param mainFrame the main frame, must not be null
     * @param notificationText the text to be displayed, if null or blank then does nothing
     * @param bgColor background color
     * @param fgColor foreground color
     * @param timeout the timeout, must be >=0;
     */
    public void displayNotification(JFrame mainFrame, String notificationText,
                                    Color bgColor, Color fgColor, long timeout) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() ->
                    displayNotification(mainFrame, notificationText, bgColor, fgColor, timeout));
            return;
        }
        if (notificationText == null || notificationText.isBlank()) {
            return; // noop
        }
        popupListener.setMainFame(mainFrame);
        popup.hidePopup();    // if there's still notification visible
        panel.setBackground(bgColor);
        labelText.setForeground(fgColor);
        labelText.setText(notificationText);
        Point position = getPosition(mainFrame, panel.getWidth());
        popup.show(mainFrame, position.x, position.y);
        setOpacity(popup, OPACITY);
        scheduleClosing(timeout);
    }

    private void setOpacity(Component comp, float opacity) {
        try {
            SwingUtilities.getWindowAncestor(comp).setOpacity(opacity);
        } catch (Exception e) {
            LOGGER.debug("Error setting opacity for notification popup", e);
        }
    }

    private void scheduleClosing(long timeout) {
        if (closingTimer != null) {
            closingTimer.stop();
        }
        int delay = (int) Math.min(timeout, Integer.MAX_VALUE);
        closingTimer = new Timer(delay, event -> {
            closingTimer = null;
            popup.hidePopup();
        });
        closingTimer.setRepeats(false);
        closingTimer.start();
    }

    private Point getPosition(JFrame mainFrame, int width) {
        return new Point(mainFrame.getWidth() / 2 - width / 2,
                (int) (mainFrame.getHeight() * BOTTOM_POSITION));
    }
}
