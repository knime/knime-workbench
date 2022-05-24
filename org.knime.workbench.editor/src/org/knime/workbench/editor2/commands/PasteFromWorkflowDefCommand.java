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
 * ---------------------------------------------------------------------
 *
 * History
 *   Jul 8, 2009 (wiswedel): created
 */
package org.knime.workbench.editor2.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.gef.EditPartViewer;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.ConnectionID;
import org.knime.core.node.workflow.ConnectionUIInformation;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowAnnotation;
import org.knime.core.node.workflow.WorkflowAnnotationID;
import org.knime.core.node.workflow.WorkflowCopyContent;
import org.knime.core.ui.node.workflow.ConnectionContainerUI;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.wrapper.WorkflowDefWrapper;
import org.knime.shared.workflow.def.WorkflowDef;
import org.knime.shared.workflow.storage.clipboard.DefClipboardContent;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.WorkflowEditorMode;
import org.knime.workbench.editor2.actions.ToggleEditorModeAction;
import org.knime.workbench.editor2.commands.PasteFromWorkflowPersistorCommand.ShiftCalculator;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;

/**
 * Pastes workflow content specified by a {@link WorkflowDef} into the workflow.
 * Only for the local workflow editor.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public final class PasteFromWorkflowDefCommand
    extends AbstractKNIMECommand {

    private final DefClipboardContent m_contentToPaste;
    private final WorkflowEditor m_editor;
    private ShiftCalculator m_shiftCalculator;

    private WorkflowCopyContent m_pastedContent;

    /**
     * @param editor The workflow to paste into
     * @param contentToPaste non-null workflow description, e.g., parsed from a string in the system clipboard.
     * @param shiftCalculator The shift calculation routine, used to include
     * some offset during paste or to provide target coordinates.
     *
     */
    public PasteFromWorkflowDefCommand(final WorkflowEditor editor,
            final DefClipboardContent contentToPaste,
            final ShiftCalculator shiftCalculator) {
        super(editor.getWorkflowManagerUI());
        m_editor = editor;
        CheckUtils.checkArgumentNotNull(contentToPaste);
        m_contentToPaste = contentToPaste;
        m_shiftCalculator = shiftCalculator;
    }

    /** @return true if the paste action would insert nothing into the workflow */
    private static boolean isEmpty(final WorkflowDef content) {
        // only nodes and annotations are considered during paste
        return content.getAnnotations().isEmpty() && content.getNodes().isEmpty();
    }

    /** {@inheritDoc} */
    @Override
    public boolean canExecute() {
        if (!super.canExecute()) {
            return false;
        }
        final boolean hasEditor = m_editor != null;
        // don't allow empty pastes (they don't belong into the history as undoing them doesn't make sense)
        final boolean hasEffect = !isEmpty(m_contentToPaste.getPayload());
        return hasEditor && hasEffect;
    }

    /**
     * Copied from the local execution branch of {@link PasteFromWorkflowPersistorCommand}.
     *
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        final var syncWfmUI = m_editor.getWorkflowManagerUI();

        //paste the content, calculate the shift and then move the pasted objects accordingly
        final WorkflowCopyContent pastedContent = syncWfmUI.paste(WorkflowDefWrapper.wrap(m_contentToPaste));

        final NodeID[] pastedNodes = pastedContent.getNodeIDs();
        final WorkflowAnnotation[] pastedAnnos = syncWfmUI.getWorkflowAnnotations(pastedContent.getAnnotationIDs());
        final boolean pasteIsForNodes = pastedNodes != null && pastedNodes.length > 0;
        final boolean needToggle = pasteIsForNodes ? (WorkflowEditorMode.NODE_EDIT != m_editor.getEditorMode())
            : (WorkflowEditorMode.ANNOTATION_EDIT != m_editor.getEditorMode());

        if (needToggle) {
            final var toggleAction = new ToggleEditorModeAction(m_editor);
            toggleAction.runInSWT();
        }

        final Set<NodeID> newIDs = new HashSet<>(); // fast lookup below
        final List<int[]> insertedElementBounds = calculateBounds(syncWfmUI, pastedNodes, pastedAnnos);
        final int[] moveDist = m_shiftCalculator.calculateShift(insertedElementBounds, syncWfmUI);
        // for redo-operations we need the exact same shift.
        m_shiftCalculator = new PasteFromWorkflowPersistorCommand.FixedShiftCalculator(moveDist);
        for (final NodeID id : pastedNodes) {
            newIDs.add(id);
            final NodeContainerUI nc = syncWfmUI.getNodeContainer(id);
            final NodeUIInformation oldUI = nc.getUIInformation();
            final NodeUIInformation newUI = NodeUIInformation.builder(oldUI).translate(moveDist).build();
            nc.setUIInformation(newUI);
        }
        for (final ConnectionContainerUI conn : syncWfmUI.getConnectionContainers()) {
            if (newIDs.contains(conn.getDest()) && newIDs.contains(conn.getSource())) {
                // get bend points and move them
                final ConnectionUIInformation oldUI = conn.getUIInfo();
                if (oldUI != null) {
                    final ConnectionUIInformation newUI =
                        ConnectionUIInformation.builder(oldUI).translate(moveDist).build();
                    conn.setUIInfo(newUI);
                }
            }
        }
        for (final WorkflowAnnotation a : pastedAnnos) {
            a.shiftPosition(moveDist[0], moveDist[1]);
        }
        setFutureSelection(pastedContent.getNodeIDs(),
            Arrays.asList(syncWfmUI.getWorkflowAnnotations(pastedContent.getAnnotationIDs())));
        m_pastedContent = pastedContent;

    }

    /**
     * @param syncWfmUI
     * @param pastedNodes
     * @param pastedAnnos
     * @return
     */
    private List<int[]> calculateBounds(final WorkflowManagerUI syncWfmUI, final NodeID[] pastedNodes,
        final WorkflowAnnotation[] pastedAnnos) {
        final List<int[]> insertedElementBounds = new ArrayList<>();
        for (final NodeID i : pastedNodes) {
            final NodeContainerUI nc = syncWfmUI.getNodeContainer(i);
            final NodeUIInformation ui = nc.getUIInformation();
            final int[] bounds = ui.getBounds();
            insertedElementBounds.add(bounds);
        }
        for (final WorkflowAnnotation a : pastedAnnos) {
            final var bounds = new int[]{a.getX(), a.getY(), a.getWidth(), a.getHeight()};
            insertedElementBounds.add(bounds);
        }
        return insertedElementBounds;
    }

    private void setFutureSelection(final NodeID[] nodeIds, final Collection<WorkflowAnnotation> was) {
        EditPartViewer partViewer = m_editor.getViewer();
        partViewer.deselectAll();
        // select the new ones....
        if (partViewer.getRootEditPart().getContents() instanceof WorkflowRootEditPart) {
            WorkflowRootEditPart rootEditPart = (WorkflowRootEditPart)partViewer.getRootEditPart().getContents();
            rootEditPart.setFutureSelection(nodeIds);
            rootEditPart.setFutureAnnotationSelection(was);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean canUndo() {
        if (m_pastedContent == null) {
            //can happen if workflow copy is not available on the server anymore
            //will almost never happen
            return false;
        }
        var manager = m_editor.getWorkflowManagerUI();
        NodeID[] pastedNodes = m_pastedContent.getNodeIDs();
        WorkflowAnnotationID[] pastedAnnos = m_pastedContent.getAnnotationIDs();
        if ((pastedNodes == null || pastedNodes.length == 0)
                && (pastedAnnos == null || pastedAnnos.length == 0)) {
            return false;
        }
        // TODO is it possible to paste a node with delete lock? is it sensible to be unable to undo the paste?
        for (NodeID id : pastedNodes) {
            if (!manager.canRemoveNode(id)) {
                return false;
            }
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void undo() {
        var manager = m_editor.getWorkflowManagerUI();
        NodeID[] nodeIDs = m_pastedContent.getNodeIDs();
        var connIDs = new ConnectionID[0];
        WorkflowAnnotationID[] annoIDs = m_pastedContent.getAnnotationIDs();
        manager.remove(nodeIDs, connIDs, annoIDs);
    }
}
