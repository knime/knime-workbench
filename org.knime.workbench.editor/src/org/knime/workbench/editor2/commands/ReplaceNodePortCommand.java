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
 *   Oct 14, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.workbench.editor2.commands;

import org.eclipse.gef.RootEditPart;
import org.knime.core.node.context.ModifiableNodeCreationConfiguration;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.UndoableUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.node.workflow.async.OperationNotAllowedException;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.ui.async.AsyncUtil;

/**
 * Command that adds, removes, or exchanges ports of a native node.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public class ReplaceNodePortCommand extends AbstractKNIMECommand {

    private ModifiableNodeCreationConfiguration m_modifiedConfig;

    private final NodeID m_affectedNodeID;

    private final RootEditPart m_root;

    private UndoableUI m_undoObject = null;

    /**
     * Constructor.
     *
     * @param nodeToReplace the node to be replaced
     * @param modifiedConfig the modified creation configuration
     */
    public ReplaceNodePortCommand(final NodeContainerEditPart nodeToReplace,
        final ModifiableNodeCreationConfiguration modifiedConfig) {
        super(nodeToReplace.getWorkflowManager());
        NodeContainerUI nc = nodeToReplace.getNodeContainer();
        m_affectedNodeID = nc.getID();
        m_modifiedConfig = modifiedConfig;
        m_root = nodeToReplace.getRoot();
    }

    @Override
    public boolean canExecute() {
        return getHostWFMUI().canReplaceNode(m_affectedNodeID);
    }

    @Override
    public void execute() {
        m_undoObject = replaceNode(getHostWFMUI(), m_affectedNodeID, m_modifiedConfig);
        // the connections are not always properly re-drawn after "unmark". (Eclipse bug.) Repaint here.
        m_root.refresh();
    }

    /**
     * Calls {@link WorkflowManagerUI#replaceNode(NodeID, ModifiableNodeCreationConfiguration)}, either synchronously or
     * asynchronously, depending on the {@link WorkflowManagerUI}-implementation.
     *
     * @param hostWFM the workflow that contains the node to replace
     * @param id the id of the node to be replaced
     * @param config the new node creation config
     * @return the undo object object to undo the operation
     */
    public static UndoableUI replaceNode(final WorkflowManagerUI hostWFM, final NodeID id,
        final ModifiableNodeCreationConfiguration config) {
        try {
            return AsyncUtil.wfmAsyncSwitchRethrow(wfm -> {
                return wfm.replaceNode(id, config);
            }, wfm -> {
                return wfm.replaceNodeAsync(id, config);
            }, hostWFM, "Replacing node ports");
        } catch (OperationNotAllowedException e) {
            openDialog("Node couldn't be replaced", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean canUndo() {
        return m_undoObject != null && m_undoObject.canUndo();
    }

    @Override
    public void undo() {
        m_undoObject.undo();
        // the connections are not always properly re-drawn after "unmark". (Eclipse bug.) Repaint here.
        m_root.refresh();
    }

}