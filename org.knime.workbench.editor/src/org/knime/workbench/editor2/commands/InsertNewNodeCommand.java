/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 *   29.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.commands;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * GEF command for adding a <code>Node</code> to the
 * <code>WorkflowManager</code>.
 *
 * @author Florian Georg, University of Konstanz
 */
public class InsertNewNodeCommand extends AbstractKNIMECommand {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(InsertNewNodeCommand.class);

    private final NodeFactory<? extends NodeModel> m_factory;

    private final boolean m_snapToGrid;

    private NodeContainer m_container;

    private ConnectionContainer m_edge;

    private DeleteCommand m_dc;

    /**
     * Creates a new command.
     *
     * @param manager The workflow manager that should host the new node
     * @param factory The factory of the Node that should be added
     * @param location Initial visual location in the
     */
    public InsertNewNodeCommand(final WorkflowManager manager,
            final NodeFactory<? extends NodeModel> factory, final ConnectionContainerEditPart edge, final boolean snapToGrid) {
        super(manager);
        m_factory = factory;
        m_edge = edge.getModel();
        m_snapToGrid = snapToGrid;
    }

    /** We can execute, if all components were 'non-null' in the constructor.
     * {@inheritDoc} */
    @Override
    public boolean canExecute() {
        // TODO: check porttypes
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void execute() {
        // the following code has mainly been copied from
        // IDEWorkbenchWindowAdvisor#preWindowShellClose
        IPreferenceStore store =
            KNIMEUIPlugin.getDefault().getPreferenceStore();
        if (!store.contains(PreferenceConstants.P_CONFIRM_RESET)
                || store.getBoolean(PreferenceConstants.P_CONFIRM_RESET)) {
            MessageDialogWithToggle dialog =
                MessageDialogWithToggle.openOkCancelConfirm(
                    Display.getDefault().getActiveShell(),
                    "Confirm reset...",
                    "Do you really want to reset all downstream node(s) ?",
                    "Do not ask again", false, null, null);
            if (dialog.getReturnCode() != IDialogConstants.OK_ID) {
                return;
            }
            if (dialog.getToggleState()) {
                store.setValue(PreferenceConstants.P_CONFIRM_RESET, false);
                KNIMEUIPlugin.getDefault().savePluginPreferences();
            }
        }

        WorkflowManager hostWFM = getHostWFM();

        LOGGER.debug("Insert new node between " + m_edge.getSource().toString() + " and " + m_edge.getDest().toString() + ".");
        // Add node to workflow and get the container
        try {
            NodeID id = hostWFM.createAndAddNode(m_factory);
            m_container = hostWFM.getNodeContainer(id);
        } catch (Throwable t) {
            // if fails notify the user
            LOGGER.debug("Node cannot be inserted.", t);
            MessageBox mb = new MessageBox(Display.getDefault().
                    getActiveShell(), SWT.ICON_WARNING | SWT.OK);
            mb.setText("Node cannot be inserted.");
            mb.setMessage("The selected node could not be inserted "
                    + "due to the following reason:\n" + t.getMessage());
            mb.open();
            return;
        }

        // create extra info and set it

        int[] boundsSource = hostWFM.getNodeContainer(m_edge.getSource()).getUIInformation().getBounds();
        int[] boundsDest = hostWFM.getNodeContainer(m_edge.getDest()).getUIInformation().getBounds();
        NodeUIInformation info = new NodeUIInformation(
                (boundsSource[0] + boundsDest[0]) / 2, (boundsSource[1] + boundsDest[1]) / 2, -1, -1, true);
        info.setSnapToGrid(false);
        info.setIsDropLocation(false);
        m_container.setUIInformation(info);

        int p = 0;
        while (!hostWFM.canAddConnection(m_edge.getSource(), m_edge.getSourcePort(), m_container.getID(), p)) {
            p++;
            if (p > m_container.getNrInPorts()) {
                break;
            }
        }
        if (p < m_container.getNrInPorts()) {
            hostWFM.addConnection(m_edge.getSource(), m_edge.getSourcePort(), m_container.getID(), p);
        }

        p = 0;
        while (!hostWFM.canAddConnection(m_container.getID(), p, m_edge.getDest(), m_edge.getDestPort())) {
            p++;
            if (p > m_container.getNrInPorts()) {
                break;
            }
        }
        if (p < m_container.getNrInPorts()) {
            hostWFM.addConnection(m_container.getID(), p, m_edge.getDest(), m_edge.getDestPort());
        }


    }

    /** {@inheritDoc} */
    @Override
    public boolean canUndo() {
        return m_edge != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        LOGGER.debug("Undo: Removing node #" + m_container.getID());
        if (canUndo()) {
            getHostWFM().removeNode(m_container.getID());
            getHostWFM().addConnection(m_edge.getSource(), m_edge.getSourcePort(), m_edge.getDest(), m_edge.getDestPort());
        } else {
            MessageDialog.openInformation(Display.getDefault().getActiveShell(),
                    "Operation no allowed", "The node "
                    + m_container.getNameWithID()
                    + " can currently not be removed");
        }
    }

}
