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
package org.knime.workbench.editor2.commands;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.ConfigurableNodeFactory;
import org.knime.core.node.NodeCreationContext;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.context.ModifiableNodeCreationConfiguration;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.core.ui.wrapper.NodeContainerWrapper;
import org.knime.workbench.ui.wrapper.WrappedNodeDialog;

/**
 * This command is invoked due to the user dragging a file of some known registered type onto the workflow canvas; it
 * was previously named DropNodeCommand.
 *
 * @author ohl, University of Konstanz
 */
public class CreateReaderNodeCommand extends AbstractKNIMECommand {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(CreateReaderNodeCommand.class);


    /** the location on the canvas at which the newly created node should be placed **/
    protected final Point m_location;

    /** whether the node should be snapped to the grid closest to that location **/
    protected final boolean m_snapToGrid;

    /**
     * the node container of the created node, this will not be populated until <code>execute()</code> successfully
     * returns
     */
    protected NodeContainer m_container;

    private final ConfigurableNodeFactory<NodeModel> m_factory;

    private final NodeCreationContext m_dropContext;

    /**
     * Creates a new command.
     *
     * @param manager the workflow manager that should host the new node
     * @param factory the factory of the Node that should be added
     * @param context the file to be set as source for the new node
     * @param location initial visual location on the canvas
     * @param snapToGrid if location should be rounded to closest grid location
     */
    public CreateReaderNodeCommand(final WorkflowManager manager, final ConfigurableNodeFactory<NodeModel> factory,
        final NodeCreationContext context, final Point location, final boolean snapToGrid) {
        super(manager);
        m_factory = factory;
        m_location = location;
        m_snapToGrid = snapToGrid;
        m_dropContext = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canExecute() {
        return (m_factory != null) && (m_location != null) && (m_dropContext != null) && super.canExecute();
    }

    /** {@inheritDoc} */
    @Override
    public void execute() {
        // Add node to workflow and get the container
        final WorkflowManager hostWFM = getHostWFM();
        try {
            final ModifiableNodeCreationConfiguration config = m_factory.createNodeCreationConfig();
            config.setURLConfiguration(m_dropContext.getUrl());
            final NodeID id = hostWFM.addNodeAndApplyContext(m_factory, config, -1);
            m_container = hostWFM.getNodeContainer(id);
            // create extra info and set it
            NodeUIInformation info = NodeUIInformation.builder()
                    .setNodeLocation(m_location.x, m_location.y, -1, -1)
                    .setHasAbsoluteCoordinates(false)
                    .setSnapToGrid(m_snapToGrid)
                    .setIsDropLocation(true).build();
            m_container.setUIInformation(info);

            // Open the dialog -- sometimes...
            if ((m_container instanceof SingleNodeContainer)
                    && m_container.getNodeContainerState().isIdle() && m_container.hasDialog()
                    // and has only a variable in port
                    && (m_container.getNrInPorts() == 1)) {
                // if not executable and has a dialog and is fully connected

                // This is embedded in a special JFace wrapper dialog
                //
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final WrappedNodeDialog dlg = new WrappedNodeDialog(SWTUtilities.getActiveShell(),
                                NodeContainerWrapper.wrap(m_container));
                            dlg.open();
                        } catch (Exception e) {
                            // they need to open it manually then
                        }
                    }
                });
            }
        } catch (Throwable t) {
            // if fails notify the user
            LOGGER.debug("Node cannot be created.", t);
            MessageBox mb = new MessageBox(SWTUtilities.getActiveShell(), SWT.ICON_WARNING | SWT.OK);
            mb.setText("Node cannot be created.");
            mb.setMessage("The selected node could not be created due to the following reason:\n" + t.getMessage());
            mb.open();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canUndo() {
        return (m_container != null) && getHostWFM().canRemoveNode(m_container.getID());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        LOGGER.debug("Undo: Removing node #" + m_container.getID());
        if (canUndo()) {
            getHostWFM().removeNode(m_container.getID());

            // TODO: save the nodes settings for a re-do. In case the dialog
            // was opened and settings adjusted.
        } else {
            MessageDialog.openInformation(SWTUtilities.getActiveShell(), "Operation no allowed",
                "The node " + m_container.getNameWithID() + " can currently not be removed");
        }
    }
}
