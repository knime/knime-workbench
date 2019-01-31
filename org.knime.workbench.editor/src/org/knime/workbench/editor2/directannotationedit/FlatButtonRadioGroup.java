/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Jan 30, 2019 (loki): created
 */
package org.knime.workbench.editor2.directannotationedit;

import java.util.ArrayList;

/**
 * Consumers who wish to have a radio group of <code>AbstractFlatButton</code> instances should use this class to
 * provide appropriate UI behavior.
 *
 * This class should presumably be moved to a more central location in the code base at some point, so that
 * non-workbench code can use it.
 *
 * @author loki der quaeler
 */
public class FlatButtonRadioGroup {

    private final ArrayList<FlatButton> m_buttons;
    private final ButtonClickListener m_listener;

    /**
     * Default constructor; to be useful, add buttons via <code>addButton(FlatButton)</code>.
     */
    public FlatButtonRadioGroup() {
        m_buttons = new ArrayList<>();
        m_listener = new ButtonClickListener();
    }

    /**
     * Constructor allowing instantiation-time population of the internal group representation.
     *
     * @param buttons the buttons to add to this group
     */
    public FlatButtonRadioGroup(final FlatButton ... buttons) {
        this();

        for (final FlatButton button : buttons) {
            addButtonNoSynchronization(button);
        }
    }

    /**
     * @param button the button to add to this group; no effort is made to check for pre-existence in the group.
     */
    public void addButton(final FlatButton button) {
        if (button != null) {
            synchronized(m_buttons) {
                addButtonNoSynchronization(button);
            }
        }
    }

    /**
     * @param button the button to remove from the group.
     */
    public void removeButton(final FlatButton button) {
        if (button != null) {
            synchronized(m_buttons) {
                if (m_buttons.remove(button)) {
                    button.removeClickListener(m_listener);
                }
            }
        }
    }

    /**
     * We're a click listener; in the future perhaps make <code>FlatButton</code> support selection listeners as well
     * and then we needed provide this.
     */
    void updateButtonsDueToProgrammaticSelection(final FlatButton newlySelectedButton) {
        synchronized(m_buttons) {
            m_buttons.forEach((button) -> {
                if (button != newlySelectedButton) { // no need for introspective comparison
                    button.setSelected(false);
                }
            });
        }
    }

    private void addButtonNoSynchronization(final FlatButton button) {
        m_buttons.add(button);
        button.addClickListener(m_listener);
    }


    private class ButtonClickListener implements FlatButton.ClickListener {
        /**
         * {@inheritDoc}
         */
        @Override
        public void clickOccurred(final FlatButton source) {
            // since we veto a button if it is currently selected, we know this button is now selected and
            //      all others should be deselected
            updateButtonsDueToProgrammaticSelection(source);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean clickShouldBeVetoed(final FlatButton source) {
            return source.isSelected();
        }
    }
}
