/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.workbench.editor2;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.knime.workbench.editor2.viewport.MessageAppearance;

/**
 * Interface for workflow editor event listeners.
 *
 * @author Noemi Balassa
 */
public interface WorkflowEditorEventListener {
    /**
     * The definition of the pinned message's appearance and content.
     *
     * @author Noemi Balassa
     */
    public static class MessageDefinition {
        /**
         * The definition of a button in a pinned message.
         *
         * @author Noemi Balassa
         * @see MessageDefinition
         */
        public static class MessageButton {
            private final Runnable m_action;

            private final String m_title;

            /**
             * Constructs a {@link MessageButton} object.
             *
             * @param title the title of the button.
             * @param action the action to be performed by the button.
             */
            public MessageButton(final String title, final Runnable action) {
                m_title = requireNonNull(title, "title");
                m_action = requireNonNull(action, "action");
            }

            /**
             * Gets the action to be performed by the button.
             *
             * @return the action to be performed by the button.
             */
            public Runnable getAction() {
                return m_action;
            }

            /**
             * Gets the title of the button.
             *
             * @return the title of the button.
             */
            public String getTitle() {
                return m_title;
            }
        }

        private final MessageAppearance m_appearance;

        private final List<MessageButton> m_buttons;

        private final boolean m_closeButtonVisible;

        private final String m_message;

        /**
         * Constructs a {@link MessageDefinition} object.
         *
         * @param message the message text.
         * @param closeButtonVisible whether to show the button for closing the message.
         * @param appearance the definition of the message's appearance.
         * @param buttons the definitions of the buttons to be added to the message.
         */
        public MessageDefinition(final String message, final boolean closeButtonVisible, final MessageAppearance appearance,
            final MessageButton... buttons) {
            m_appearance = requireNonNull(appearance, "appearance");
            m_closeButtonVisible = closeButtonVisible;
            m_buttons =
                buttons == null ? emptyList() : stream(buttons).filter(Objects::nonNull).collect(Collectors.toList());
            m_message = requireNonNull(message, "message");
        }

        /**
         * Gets the definition of the message's appearance.
         *
         * @return the definition of the message's appearance.
         */
        public MessageAppearance getAppearance() {
            return m_appearance;
        }

        /**
         * Gets the definitions of the buttons to be added to the message.
         *
         * @return the definitions of the buttons to be added to the message.
         */
        public List<MessageButton> getButtons() {
            return m_buttons;
        }

        /**
         * Gets the message text.
         *
         * @return the message text.
         */
        public String getMessage() {
            return m_message;
        }

        /**
         * Gets whether to show the button for closing the message.
         *
         * @return {@code true} iff the message closing button is to be shown.
         */
        public boolean isCloseButtonVisible() {
            return m_closeButtonVisible;
        }
    }

    /**
     * Interface for accessing the workflow editor.
     *
     * @author Noemi Balassa
     */
    public interface WorkflowEditorEvent {
        /**
         * Gets the workflow editor on which the event occurred.
         *
         * @return a {@link WorkflowEditor} object.
         */
        WorkflowEditor getWorkflowEditor();
    }

    /**
     * Interface for accessing the active workflow editor and performing restricted operations on it.
     *
     * @author Noemi Balassa
     */
    public interface ActiveWorkflowEditorEvent extends WorkflowEditorEvent {
        /**
         * Adds a message to be pinned to the top of the viewport with optional right-aligned buttons.
         *
         * @param messageDefinition the definition of the message's appearance and content.
         * @return the unique ordinal of the message in the editor.
         */
        long addMessage(MessageDefinition messageDefinition);
    }

    /**
     * Performs an action when invoked after the workflow editor has been closed.
     * <p>
     * Since the workflow editor has already been closed, it is not recommended to try using the reference to it for
     * anything other than identification.
     * </p>
     *
     * @param event the event to process.
     */
    void editorClosed(WorkflowEditorEvent event);

    /**
     * Performs an action when invoked after the workflow has been loaded by the editor.
     *
     * @param event the event to process.
     */
    void workflowLoaded(ActiveWorkflowEditorEvent event);

    /**
     * Performs an action when invoked after the edited workflow has been saved.
     *
     * @param event the event to process.
     */
    void workflowSaved(ActiveWorkflowEditorEvent event);

}
