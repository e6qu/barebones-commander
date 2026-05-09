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

package dev.barebones.commander.ui.autocomplete;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.text.BadLocationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.barebones.commander.ui.autocomplete.completers.Completer;

/**
 * TextFieldCompleter is a CompleterType which suite to text-field.
 * 
 * @author Arik Hadas, based on the code of Santhosh Kumar: http://www.jroller.com/santhosh/entry/file_path_autocompletion
 */

public class TextFieldCompletion extends CompletionType {
    private static final Logger LOGGER = LoggerFactory.getLogger(TextFieldCompletion.class);

    public TextFieldCompletion(AutocompleterTextComponent comp, Completer completer){
        super(comp, completer);

        autocompletedtextComp.getDocument().addDocumentListener(documentListener);

        autocompletedtextComp.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent keyEvent) {

                switch (keyEvent.getKeyCode()) {
                case KeyEvent.VK_ENTER:
                    if (isItemSelectedAtPopupList()) {
                        hideAutocompletionPopup();
                        acceptListItem((String)list.getSelectedValue());
                        keyEvent.consume();
                    }
                    else
                        autocompletedtextComp.OnEnterPressed(keyEvent);
                    break;
                case KeyEvent.VK_ESCAPE:
                    if (isPopupListShowing()) {
                        if (autocompletedtextComp.isEnabled())
                            hideAutocompletionPopup();
                        keyEvent.consume();
                    }
                    else
                        autocompletedtextComp.OnEscPressed(keyEvent);
                    break;
                case KeyEvent.VK_UP:
                    if(autocompletedtextComp.isEnabled() && popup.isVisible()) {
                        selectPreviousPossibleValue();
                        keyEvent.consume();
                    }
                    break;
                case KeyEvent.VK_SPACE:
                    // The combination of cntrl+space makes open the auto-complete popup without delay.
                    if (keyEvent.isControlDown()) {
                        if (!popup.isVisible()) {
                            autocompletedtextComp.moveCarentToEndOfText();
                            createNewShowingThread(0);
                        }
                    }
                    break;
                case KeyEvent.VK_DOWN:
                    if(autocompletedtextComp.isEnabled()){ 
                        if(popup.isVisible()) {
                            selectNextPossibleValue();
                            keyEvent.consume();
                        }
                        else {                	
                            autocompletedtextComp.moveCarentToEndOfText();
                            createNewShowingThread(0);
                        }
                    }
                    break;
                case KeyEvent.VK_PAGE_DOWN:
                    if(autocompletedtextComp.isEnabled() && isPopupListShowing()) {
                        selectNextPage();
                        keyEvent.consume();
                    }
                    break;
                case KeyEvent.VK_PAGE_UP:
                    if(autocompletedtextComp.isEnabled() && isItemSelectedAtPopupList()) {
                        selectPreviousPage();
                        keyEvent.consume();
                    }
                    break;
                case KeyEvent.VK_HOME:
                    if(autocompletedtextComp.isEnabled() && isPopupListShowing()) {
                        selectFirstValue();
                        keyEvent.consume();
                    }
                    break;
                case KeyEvent.VK_END:
                    if(autocompletedtextComp.isEnabled() && isPopupListShowing()) {
                        selectLastValue();
                        keyEvent.consume();
                    }
                    break;
                case KeyEvent.VK_LEFT:
                    hideAutocompletionPopup();
                    break;
                default:
                }
            }
        });
    }

    @Override
    protected void showAutocompletionPopup() {
        if (!(autocompletedtextComp.isShowing() && autocompletedtextComp.isEnabled()
                && updateListData(list))) {
            return;
        }
        list.setVisibleRowCount(Math.min(list.getModel().getSize(), VISIBLE_ROW_COUNT));

        int x;
        try {
            x = autocompletedtextComp.modelToView().x;
        } catch (BadLocationException e) {
            LOGGER.debug("Caught exception", e);
            return;
        }
        if (!autocompletedtextComp.hasFocus()) {
            return;
        }
        list.ensureIndexIsVisible(0);
        synchronized (popup) {
            popup.show(autocompletedtextComp.getTextComponent(), x,
                autocompletedtextComp.getHeight());
            // Swing quirk: popup occasionally renders as a grey rectangle —
            // repainting fixes it.
            popup.repaint();
        }
    }

    @Override
    protected void hideAutocompletionPopup() {
        synchronized (popup) {
            if (popup.isVisible())
                popup.setVisible(false);
        }
    }
}
