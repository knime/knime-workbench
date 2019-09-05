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
import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.editor2.WorkflowEditorEventListener.ActiveWorkflowEditorEvent;
import org.knime.workbench.editor2.WorkflowEditorEventListener.MessageDefinition;
import org.knime.workbench.editor2.WorkflowEditorEventListener.MessageDefinition.MessageButton;
import org.knime.workbench.editor2.WorkflowEditorEventListener.WorkflowEditorEvent;
import org.knime.workbench.editor2.viewport.ViewportPinningGraphicalViewer;

/**
 * Static utility methods for working with workflow editor event listeners.
 *
 * @author Noemi Balassa
 */
final class WorkflowEditorEventListeners {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowEditorEventListeners.class);

    private static final Collection<WorkflowEditorEventListener> LISTENERS;

    static {
        Collection<WorkflowEditorEventListener> listeners = stream(Platform.getExtensionRegistry()
            .getExtensionPoint("org.knime.workbench.editor.WorkflowEditorEventListener").getExtensions())
                .flatMap(extension -> stream(extension.getConfigurationElements())).map(configuration -> {
                    try {
                        final Object extension = configuration.createExecutableExtension("class");
                        if (extension instanceof WorkflowEditorEventListener) {
                            return (WorkflowEditorEventListener)extension;
                        }
                    } catch (final Throwable throwable) {
                        LOGGER.error("An error occurred during the loading of a workflow editor event listener: "
                            + configuration.getDeclaringExtension().getNamespaceIdentifier(), throwable);
                    }
                    return null;
                }).filter(Objects::nonNull).collect(Collectors.toList());
        LISTENERS = listeners.isEmpty() ? emptyList() : unmodifiableCollection(listeners);
    }

    /**
     * Creates an event for an active workflow editor.
     *
     * @param workflowEditor the workflow editor.
     * @return an {@link ActiveWorkflowEditorEvent} object.
     * @throws NullPointerException if {@code workflowEditor} is {@code null}.
     */
    static ActiveWorkflowEditorEvent createActiveWorkflowEditorEvent(final WorkflowEditor workflowEditor) {
        requireNonNull(workflowEditor, "workflowEditor");
        return new ActiveWorkflowEditorEvent() {
            @Override
            public WorkflowEditor getWorkflowEditor() {
                return workflowEditor;
            }

            @Override
            public long addMessage(final MessageDefinition messageDefinition) {
                ViewportPinningGraphicalViewer viewer =
                    ((ViewportPinningGraphicalViewer)workflowEditor.getGraphicalViewer());
                final List<MessageButton> messageButtons = messageDefinition.getButtons();
                return messageButtons.isEmpty() // Forced line break.
                    ? viewer.displayMessage(messageDefinition.getMessage(), messageDefinition.getAppearance(),
                        messageDefinition.isCloseButtonVisible()) // Forced line break.
                    : viewer.displayMessage(messageDefinition.getMessage(), messageDefinition.getAppearance(),
                        messageButtons.stream().map(MessageButton::getTitle).toArray(size -> new String[size]),
                        messageButtons.stream().map(MessageButton::getAction).toArray(size -> new Runnable[size]),
                        false, messageDefinition.isCloseButtonVisible());
            }
        };
    }

    /**
     * Creates an event for a workflow editor.
     *
     * @param workflowEditor the workflow editor.
     * @return a {@link WorkflowEditorEvent} object.
     * @throws NullPointerException if {@code workflowEditor} is {@code null}.
     */
    static WorkflowEditorEvent createWorkflowEditorEvent(final WorkflowEditor workflowEditor) {
        requireNonNull(workflowEditor, "workflowEditor");
        return new WorkflowEditorEvent() {
            @Override
            public WorkflowEditor getWorkflowEditor() {
                return workflowEditor;
            }
        };
    }

    /**
     * Gets the immutable collection of the workflow editor event listeners that have been registered as extensions.
     *
     * @return a {@link Collection} of {@link WorkflowEditorEventListener} objects.
     */
    static Collection<WorkflowEditorEventListener> getListeners() {
        return LISTENERS;
    }

    private WorkflowEditorEventListeners() {
        throw new UnsupportedOperationException();
    }

}
