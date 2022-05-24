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
 * -------------------------------------------------------------------
 *
 * History
 *   20.02.2006 (sieb): created
 */
package org.knime.workbench.editor2.actions;

import java.util.Optional;
import java.util.UUID;

import org.eclipse.gef.commands.Command;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.shared.workflow.storage.clipboard.DefClipboardContent;
import org.knime.shared.workflow.storage.clipboard.InvalidDefClipboardContentVersionException;
import org.knime.workbench.editor2.ClipboardObject;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.commands.PasteFromWorkflowDefCommand;
import org.knime.workbench.editor2.commands.PasteFromWorkflowPersistorCommand;
import org.knime.workbench.editor2.commands.PasteFromWorkflowPersistorCommand.ShiftCalculator;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Implements the clipboard paste action to paste nodes and connections from the
 * clipboard into the editor.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class PasteAction extends AbstractClipboardAction {

    private static final int OFFSET = 120;

    private static final NodeLogger LOGGER = NodeLogger.getLogger(PasteAction.class);

    /** The payload identifier of the clipboard content that was previously pasted. */
    private Optional<UUID> m_lastPastedPayloadIdentifier = Optional.empty();
    /** The number of times the payload with identifier {@link #m_lastPastedPayloadIdentifier} has been pasted. */
    private int m_pasteCount = 0;

    /**
     * Constructs a new clipboard paste action.
     *
     * @param editor the workflow editor this action is intended for
     */
    public PasteAction(final WorkflowEditor editor) {
        super(editor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ActionFactory.PASTE.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        ISharedImages sharedImages =
                PlatformUI.getWorkbench().getSharedImages();
        return sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "Paste";
    }

    /**
     * @return whether we're executing the paste in a remote workflow editor
     */
    private boolean inRemoteWorkflowEditor() {
        return getEditor().getWorkflowManager().isEmpty();
    }

    /**
     * @return the operating system clipboard content as unicode string
     */
    private static Optional<String> getSystemClipboardContentAsString() {
        return CopyAction.readFromSystemClipboard();
    }

    /**
     * Parse the system clipboard string contents into a workflow def.
     */
    private static Optional<DefClipboardContent> getSystemClipboardAsDef() {
        var optContent = getSystemClipboardContentAsString();
        if (optContent.isPresent()) {
            try {
                return DefClipboardContent.valueOf(optContent.get());
            } catch (InvalidDefClipboardContentVersionException idccve) {
                LOGGER.warn(idccve.getMessage(), idccve);
            }
        }
        return Optional.empty();
    }

    /**
     * In the workflow editor, any string content in the system clipboard will enable the paste action. In the remote
     * workflow editor, any content of the workbench clipboard will enable the paste action.
     *
     * {@inheritDoc}
     */
    @Override
    protected boolean internalCalculateEnabled() {
        if (getManagerUI().isWriteProtected()) {
            return false;
        }
        if (inRemoteWorkflowEditor()) {
            return getEditor().getClipboardContent() != null;
        }
        return getSystemClipboardContentAsString().isPresent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {

        var shiftCalculator = newShiftCalculator();

        Command pasteCommand = null;
        ClipboardObject clipObject = getEditor().getClipboardContent();
        if (clipObject != null) {
            //TODO change this to support the persistor paste
            pasteCommand = new PasteFromWorkflowPersistorCommand(getEditor(), clipObject, shiftCalculator);
        } else {

            var parsedClipboardContent = getSystemClipboardAsDef();
            if (parsedClipboardContent.isEmpty()) {
                LOGGER.info("The system clipboard does not contain KNIME workflow content.");
            } else {
                pasteCommand = createDefPasteCommand(parsedClipboardContent.get());
            }
        }

        // the system clipboard content can change between internalCalculateEnabled and runOnNodes
        // that's why we might not have an executable paste here
        if (pasteCommand != null) {
            getCommandStack().execute(pasteCommand); // enables undo
            // update the actions
            getEditor().updateActions();

            // Give focus to the editor again. Otherwise the actions (selection)
            // is not updated correctly.
            getWorkbenchPart().getSite().getPage().activate(getWorkbenchPart());
        }

    }

    /**
     * @param parsedClipboardContent
     * @return
     */
    private PasteFromWorkflowDefCommand createDefPasteCommand(final DefClipboardContent contentToPaste) {
        final UUID newPayload = contentToPaste.getPayloadIdentifier();
        var isNewContent = m_lastPastedPayloadIdentifier.map(last -> !last.equals(newPayload)).orElse(true);
        if(isNewContent) {
            m_lastPastedPayloadIdentifier = Optional.of(newPayload);
            m_pasteCount = 1;
        } else {
            m_pasteCount++;
        }
        var shiftCalculator = newShiftCalculator(m_pasteCount);
        return new PasteFromWorkflowDefCommand(getEditor(), contentToPaste, shiftCalculator);
    }

    /**
     * A shift operator that calculates a fixed offset. The sub class
     * {@link PasteActionContextMenu} overrides this method to return a
     * different shift calculator that respects the current mouse
     * pointer location.
     * @return A new shift calculator.
     */
    protected ShiftCalculator newShiftCalculator(final int pasteCount) {
        return new ShiftCalculator() {
            /** {@inheritDoc} */
            @Override
            public int[] calculateShift(final Iterable<int[]> boundsList,
                    final WorkflowManagerUI manager,
                    final ClipboardObject clipObject) {
                final int counter =
                    clipObject.incrementAndGetRetrievalCounter();
                return calculateShift(counter);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public int[] calculateShift(final int offsetX, final int offsetY, final ClipboardObject clipObject) {
                return calculateShift(null, null, clipObject);
            }

            @Override
            public int[] calculateShift(final Iterable<int[]> bounds, final WorkflowManagerUI manager) {
                return calculateShift(pasteCount);
            }

            private int[] calculateShift(final int counter) {
            int offsetX = OFFSET;
            int offsetY = OFFSET;
            if (getEditor().getEditorSnapToGrid()) {
                // with grid
                offsetX = getEditor().getEditorGridXOffset(OFFSET);
                offsetY = getEditor().getEditorGridYOffset(OFFSET);
            }
            int newX = (offsetX * counter);
            int newY = (offsetY * counter);
            return new int[] {newX, newY};
        }
        };
    }

    /**
     * A shift operator that calculates a fixed offset. The sub class
     * {@link PasteActionContextMenu} overrides this method to return a
     * different shift calculator that respects the current mouse
     * pointer location.
     * @return A new shift calculator.
     */
    protected ShiftCalculator newShiftCalculator() {
        return new ShiftCalculator() {
            /** {@inheritDoc} */
            @Override
            public int[] calculateShift(final Iterable<int[]> boundsList,
                    final WorkflowManagerUI manager,
                    final ClipboardObject clipObject) {
                final int counter =
                    clipObject.incrementAndGetRetrievalCounter();
                return calculateShift(counter);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public int[] calculateShift(final int offsetX, final int offsetY, final ClipboardObject clipObject) {
                return calculateShift(null, null, clipObject);
            }

            @Override
            public int[] calculateShift(final Iterable<int[]> bounds, final WorkflowManagerUI manager) {
                // TODO increment
                return calculateShift(1);
            }

            private int[] calculateShift(final int counter) {
            int offsetX = OFFSET;
            int offsetY = OFFSET;
            if (getEditor().getEditorSnapToGrid()) {
                // with grid
                offsetX = getEditor().getEditorGridXOffset(OFFSET);
                offsetY = getEditor().getEditorGridYOffset(OFFSET);
            }
            int newX = (offsetX * counter);
            int newY = (offsetY * counter);
            return new int[] {newX, newY};
        }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean canHandleWorkflowManagerUI() {
        return true;
    }
}
