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
 *   02.03.2006 (sieb): created
 */
package org.knime.workbench.editor2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.commands.UnexecutableCommand;
import org.eclipse.gef.tools.DragEditPartsTracker;
import org.eclipse.swt.SWT;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.SubNodeContainerUI;
import org.knime.workbench.editor2.commands.AsyncCommand;
import org.knime.workbench.editor2.editparts.AbstractPortEditPart;
import org.knime.workbench.editor2.editparts.AbstractWorkflowPortBarEditPart;
import org.knime.workbench.editor2.editparts.AnnotationEditPart;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Adjusts the default <code>DragEditPartsTracker</code> to create commands that also move bendpoints and also
 *  create temporary selections for the spatial children of annotations in annotation edit mode.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class WorkflowSelectionDragEditPartsTracker extends DragEditPartsTracker {
    /**
     * Constructs a new WorkflowSelectionDragEditPartsTracker with the given source edit part.
     *
     * @param sourceEditPart the source edit part
     */
    public WorkflowSelectionDragEditPartsTracker(final EditPart sourceEditPart) {
        super(sourceEditPart);
    }

    @Override
    protected boolean handleButtonUp(final int button) {
        if (getSourceEditPart() instanceof AnnotationEditPart) {
            final Input i = getCurrentInput();

            if (WorkflowCanvasClickListener.annotationDragTrackerShouldVeto(i.getMouseLocation())) {
                return false;
            }
        }

        return super.handleButtonUp(button);
    }

    /*
     * {@inheritDoc}
     */
    @Override
    protected boolean handleButtonDown(final int button) {
        // don't do any state changes if this is the pan button
        if (button != WorkflowSelectionTool.PAN_BUTTON) {
            return super.handleButtonDown(button);
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean handleDoubleClick(final int button) {
        EditPart part = getSourceEditPart();
        if (part instanceof NodeContainerEditPart && getCurrentInput().isModKeyDown(SWT.MOD1)) {
            NodeContainerEditPart ncPart = ((NodeContainerEditPart)part);
            NodeContainerUI container = (NodeContainerUI)ncPart.getModel();
            if (container instanceof SubNodeContainerUI) {
                ncPart.openSubWorkflowEditor();
                return false;
            }
        }
        return super.handleDoubleClick(button);
    }

    /**
     * Asks each edit part in the
     * {@link org.eclipse.gef.tools.AbstractTool#getOperationSet() operation set}
     * to contribute to a {@link CompoundCommand} after first setting the
     * request type to either {@link org.eclipse.gef.RequestConstants#REQ_MOVE}
     * or {@link org.eclipse.gef.RequestConstants#REQ_ORPHAN}, depending on the
     * result of {@link #isMove()}.
     *
     * Additionally the method creates a command to adapt connections where both
     * node container are include in the drag operation.
     *
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")  // generic casting
    @Override
    protected Command getCommand() {

        CompoundCommand command = new CompoundCommand();
        command.setDebugLabel("Drag Object Tracker");

        Iterator<?> iter = getOperationSet().iterator();

        Request request = getTargetRequest();

        if (isCloneActive()) {
            request.setType(REQ_CLONE);
        } else if (isMove()) {
            request.setType(REQ_MOVE);
        } else {
            request.setType(REQ_ORPHAN);
        }

        List<AsyncCommand> asyncCommands = new ArrayList<AsyncCommand>();
        if (!isCloneActive()) {
            while (iter.hasNext()) {
                EditPart editPart = (EditPart)iter.next();
                Command c = editPart.getCommand(request);
                if(!collectIfAsync(c, asyncCommands)) {
                    command.add(c);
                }
            }
        }

        // now add the commands for the node-embraced connections
        ConnectionContainerEditPart[] connectionsToAdapt =
                getEmbracedConnections(getOperationSet());
        for (ConnectionContainerEditPart connectionPart : connectionsToAdapt) {
            Command c = connectionPart.getBendpointAdaptionCommand(request);
            if (!collectIfAsync(c, asyncCommands)) {
                command.add(c);
            }
        }

        //create one single command from the async commands such that they are executed as one
        if (!asyncCommands.isEmpty()) {
            command.add(AsyncCommand.combineWithRefresh(asyncCommands,
                "Waiting to complete operations on selected nodes and connections ..."));
        }

        if (!isMove() || isCloneActive()) {

            if (!isCloneActive()) {
                request.setType(REQ_ADD);
            }

            if (getTargetEditPart() == null) {
                command.add(UnexecutableCommand.INSTANCE);
            } else {
                command.add(getTargetEditPart().getCommand(getTargetRequest()));
            }
        }

        return command;
    }

    private static boolean collectIfAsync(final Command c, final List<AsyncCommand> asyncCommands) {
        if (c instanceof AsyncCommand && ((AsyncCommand)c).shallExecuteAsync()) {
            asyncCommands.add((AsyncCommand)c);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns the connections whose source and target is contained in the argument list.
     * @param parts list of selected nodes
     * @return the connections whose source and target is contained in the argument list.
     */
    public static ConnectionContainerEditPart[] getEmbracedConnections(
            final List<? extends EditPart> parts) {

        // result list
        List<ConnectionContainerEditPart> result =
                new ArrayList<ConnectionContainerEditPart>();

        for (EditPart part : parts) {
            if (part instanceof NodeContainerEditPart
                    || part instanceof AbstractWorkflowPortBarEditPart) {
                EditPart containerPart = part;

                ConnectionContainerEditPart[] outPortConnectionParts =
                        getOutportConnections(containerPart);

                // if one of the connections in-port-node is included in the
                // selected list, the connections bendpoints must be adapted
                for (ConnectionContainerEditPart connectionPart
                            : outPortConnectionParts) {

                    // get the in-port-node part of the connection and check
                    AbstractPortEditPart inPortPart = null;
                    if (connectionPart.getTarget() != null
                            && ((AbstractPortEditPart)connectionPart
                                    .getTarget()).isInPort()) {
                        inPortPart =
                                (AbstractPortEditPart)connectionPart
                                        .getTarget();
                    } else if (connectionPart.getSource() != null) {
                        inPortPart =
                                (AbstractPortEditPart)connectionPart
                                        .getSource();
                    }

                    if (inPortPart != null
                            && isPartInList(inPortPart.getParent(), parts)) {
                        result.add(connectionPart);
                    }
                }

            }
        }
        return result.toArray(new ConnectionContainerEditPart[result.size()]);
    }

    @SuppressWarnings("unchecked")
    private static ConnectionContainerEditPart[] getOutportConnections(final EditPart containerPart) {
        // result list
        final List<ConnectionContainerEditPart> result = new ArrayList<>();
        var children = containerPart.getChildren();

        for (EditPart part : children) {
            if (part instanceof AbstractPortEditPart outPortPart) {
                // append all connection edit parts
                result.addAll((Collection<? extends ConnectionContainerEditPart>) outPortPart.getSourceConnections());
            }
        }

        return result.toArray(new ConnectionContainerEditPart[result.size()]);
    }

    private static boolean isPartInList(final EditPart partToCheck,
            final List<? extends EditPart> parts) {

        for (EditPart part : parts) {

            if (part == partToCheck) {

                return true;
            }
        }

        return false;
    }
}
