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
 *   13.04.2011 (Bernd Wiswedel): created
 */
package org.knime.workbench.editor2.commands;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainerTemplate;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.workbench.editor2.UpdateMetaNodeTemplateRunnable;

/**
 * GEF command for update metanode links.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich
 */
public class UpdateMetaNodeLinkCommand extends AbstractKNIMECommand {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(UpdateMetaNodeLinkCommand.class);

    // fields correspond to the fields in UpdateMetaNodeTemplateRunnable
    private final NodeID[] m_ids;
    private List<NodeID> m_newIDs;
    private List<WorkflowPersistor> m_undoPersistors;

    /**
     * Creates a new command.
     *
     * @param manager The workflow manager containing the links to be updated.
     * @param ids The ids of the link nodes.
     */
    public UpdateMetaNodeLinkCommand(final WorkflowManager manager,
            final NodeID[] ids) {
        super(manager);
        m_ids = ids.clone();
    }

    /** We can execute, if all components were 'non-null' in the constructor.
     * {@inheritDoc} */
    @Override
    public boolean canExecute() {
        if (!super.canExecute()) {
            return false;
        }
        if (m_ids == null) {
            return false;
        }
        boolean containsUpdateableMN = false;
        WorkflowManager hostWFM = getHostWFM();
        for (NodeID id : m_ids) {
            NodeContainer nc = hostWFM.findNodeContainer(id);
            if (nc instanceof NodeContainerTemplate) {
                NodeContainerTemplate tnc = (NodeContainerTemplate)nc;
                final WorkflowManager parent = tnc.getParent();
                if (parent.hasUpdateableMetaNodeLink(id)) {
                    containsUpdateableMN = true;
                }
                if (!parent.canUpdateMetaNodeLink(id)) {
                    return false;
                }
            }
        }
        return containsUpdateableMN;
    }

    /** {@inheritDoc} */
    @Override
    public void execute() {
        UpdateMetaNodeTemplateRunnable updateRunner = null;
        try {
            IWorkbench wb = PlatformUI.getWorkbench();
            IProgressService ps = wb.getProgressService();
            WorkflowManager hostWFM = getHostWFM();
            updateRunner = new UpdateMetaNodeTemplateRunnable(hostWFM, m_ids);
            ps.busyCursorWhile(updateRunner);
            m_newIDs = updateRunner.getNewIDs();
            m_undoPersistors = updateRunner.getUndoPersistors();
            assert m_newIDs.size() == m_undoPersistors.size();
        } catch (InvocationTargetException | InterruptedException ex) {
            // if fails notify the user
            LOGGER.debug("Node cannot be created.", ex);
            var message = ex instanceof InvocationTargetException
                    ? ((InvocationTargetException)ex).getTargetException().getMessage() : ex.getMessage();
            MessageDialog.openWarning(SWTUtilities.getActiveShell(), "Node cannot be created.",
                "The selected node could not be created " + "due to the following reason:\n" + message);
        } finally {
            if (updateRunner != null) {
                updateRunner.discard();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean canUndo() {
        if (m_ids == null || m_ids.length == 0) {
            return false;
        }
        if (m_newIDs == null || m_undoPersistors == null) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        LOGGER.debug("Undo: Reverting metanode links ("
                + m_newIDs.size() + " metanode(s))");
        WorkflowManager hostWFM = getHostWFM();
        for (int i = 0; i < m_newIDs.size(); i++) {
            NodeID id = m_newIDs.get(i);
            WorkflowPersistor p = m_undoPersistors.get(i);
            NodeContainerTemplate nodeToBeDeleted = (NodeContainerTemplate)hostWFM.findNodeContainer(id);
            WorkflowManager parent = nodeToBeDeleted.getParent();
            parent.removeNode(nodeToBeDeleted.getID());
            parent.paste(p);
        }

    }

}
