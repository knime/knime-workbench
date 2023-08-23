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
 *   09.06.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.editparts;

import java.util.ArrayList;

import org.eclipse.draw2d.AbsoluteBendpoint;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.ConnectionProgressEvent;
import org.knime.core.node.workflow.ConnectionProgressListener;
import org.knime.core.node.workflow.ConnectionUIInformation;
import org.knime.core.node.workflow.ConnectionUIInformationEvent;
import org.knime.core.node.workflow.ConnectionUIInformationListener;
import org.knime.core.node.workflow.EditorUIInformation;
import org.knime.core.ui.node.workflow.ConnectionContainerUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.workbench.editor2.EditorModeParticipant;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.WorkflowEditorMode;
import org.knime.workbench.editor2.commands.ChangeBendPointLocationCommand;
import org.knime.workbench.editor2.editparts.policy.ConnectionBendpointEditPolicy;
import org.knime.workbench.editor2.editparts.snap.SnapOffBendPointConnectionRouter;
import org.knime.workbench.editor2.figures.AbstractPortFigure;
import org.knime.workbench.editor2.figures.CurvedPolylineConnection;
import org.knime.workbench.editor2.figures.ProgressPolylineConnection;

/**
 * EditPart controlling a <code>ConnectionContainer</code> object in the
 * workflow. Model: {@link ConnectionContainerUI} View: {@link PolylineConnection}
 * created in {@link #createFigure()} Controller:
 * {@link ConnectionContainerEditPart}
 *
 *
 * @author Florian Georg, University of Konstanz
 */
public class ConnectionContainerEditPart extends AbstractConnectionEditPart
    implements ConnectionUIInformationListener, ConnectionProgressListener, EditorModeParticipant {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ConnectionContainerEditPart.class);


    private WorkflowEditorMode m_currentEditorMode = WorkflowEditor.INITIAL_EDITOR_MODE;
    private Color m_figureOriginalForegroundColor;

    /** {@inheritDoc} */
    @Override
    public ConnectionContainerUI getModel() {
        return (ConnectionContainerUI)super.getModel();
    }

    /**
     * Returns the parent WFM. This method may also return null if the target
     * edit part has no parent assigned.
     *
     * @return The hosting WFM
     */
    public WorkflowManagerUI getWorkflowManager() {
        EditPart targetEditPart = getTarget();
        if (targetEditPart instanceof NodeInPortEditPart) {
            return ((NodeInPortEditPart)targetEditPart).getManager();
        }
        if (targetEditPart instanceof WorkflowOutPortEditPart) {
            return ((WorkflowOutPortEditPart)targetEditPart).getManager();
        }
        return null;
    }

//    /**
//     * Returns the node with the corresponding id. If the passed wfm is null, it
//     * traverses the hierarchy to find it. If the wfm is passed, it must contain
//     * the node - or be the node itself (that happens in metanodes and outgoing
//     * connectinos).
//     *
//     *
//     * @param wfm if not null, the node is taken from there
//     * @param node id of the node to return
//     * @return the node with the specified id.
//     */
//    private INodeContainer getNode(final IWorkflowManager wfm, final NodeID node) {
//        if (wfm != null) {
//            if (wfm.getID().equals(node)) {
//                return wfm;
//            } else {
//                return wfm.getNodeContainer(node);
//            }
//        }
//
//        // follow the hierarchy
//        NodeID prefix = node.getPrefix();
//        if (prefix.equals(NodeID.ROOTID)) {
//            //TODO -> don't use the WorkflowManager root to manage workflow projects (see https://knime-com.atlassian.net/projects/AP/issues/AP-6511)
//            return WorkflowManager.ROOT.getNodeContainer(node);
//        } else {
//            INodeContainer nc = getNode(null, prefix);
//            if (!(nc instanceof IWorkflowManager)) {
//                return null;
//            }
//            IWorkflowManager parent = (IWorkflowManager)nc;
//            return parent.getNodeContainer(node);
//        }
//    }

    /** {@inheritDoc} */
    @Override
    public void activate() {
        super.activate();
        getModel().addUIInformationListener(this);
        getModel().addProgressListener(this);
    }

    /** {@inheritDoc} */
    @Override
    public void deactivate() {
        getModel().removeUIInformationListener(this);
        getModel().removeProgressListener(this);
        super.deactivate();
    }

    /**
     * Sets whether this connection should render as highlighted or regular default color; this will be ultimately
     *  ignored if the user has disabled connection highlighting via Preferences.
     *
     * @param flag if true, then render the line in the highlight color.
     */
    public void setHighlighted(final boolean flag) {
        ((CurvedPolylineConnection)getFigure()).setHighlighted(flag, getModel().isFlowVariablePortConnection());
    }

    /**
     * Creates a GEF command to shift the connections bendpoints.
     *
     * @param request the underlying request holding information about the shift
     * @return the command to change the bendpoint locations
     */
    public Command getBendpointAdaptionCommand(final Request request) {
        assert (request instanceof ChangeBoundsRequest) : "Unexpected request type: " + request.getClass();

        ZoomManager zoomManager = (ZoomManager)(getRoot().getViewer().getProperty(ZoomManager.class.toString()));

        Point moveDelta = ((ChangeBoundsRequest) request).getMoveDelta();
        return new ChangeBendPointLocationCommand(this, moveDelta, zoomManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createEditPolicies() {
        installEditPolicy(EditPolicy.CONNECTION_ENDPOINTS_ROLE, new ConnectionEndpointEditPolicy());

        // enable bendpoints (must be stored in extra info)
        installEditPolicy(EditPolicy.CONNECTION_BENDPOINTS_ROLE, new ConnectionBendpointEditPolicy());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("rawtypes")  // untyped ArrayList creation
    protected IFigure createFigure() {
        ProgressPolylineConnection conn = new CurvedPolylineConnection(false);
        // Bendpoints
        SnapOffBendPointConnectionRouter router = new SnapOffBendPointConnectionRouter();
        conn.setConnectionRouter(router);
        conn.setRoutingConstraint(new ArrayList());
        conn.setLineWidth(getCurrentEditorSettings().getConnectionLineWidth());

        // make flow variable port connections look red.
        if (getModel().isFlowVariablePortConnection()) {
            conn.setForegroundColor(AbstractPortFigure.getFlowVarPortColor());
        }

        m_figureOriginalForegroundColor = conn.getForegroundColor();

        return conn;
    }

    /**
     * {@inheritDoc}
     *
     * We don't want to be selected if we're not in node-edit-mode.
     */
    @Override
    public EditPart getTargetEditPart(final Request request) {
        if (m_currentEditorMode.equals(WorkflowEditorMode.NODE_EDIT) ) {
            return super.getTargetEditPart(request);
        }

        return null;
    }

//    private boolean isFlowVariablePortConnection() {
//        IConnectionContainer connCon = getModel();
//        INodeContainer node = getNode(getWorkflowManager(), connCon.getSource());
//        if (node != null) {
//            INodeOutPort srcPort;
//            if (isIncomingConnection(connCon)) {
//                // then this is a workflow port
//                srcPort =
//                        ((IWorkflowManager)node).getWorkflowIncomingPort(connCon
//                                .getSourcePort());
//            } else {
//                srcPort = node.getOutPort(connCon.getSourcePort());
//            }
//            if (srcPort != null
//                    && PortTypeUtil.getPortType(srcPort.getPortTypeUID())
//                            .equals(FlowVariablePortObject.TYPE)) {
//                return true;
//            }
//        }
//        return false;
//    }

//    /**
//     * Returns true, if the connection is a connection from a metanode incoming port to a node inside the metanode.
//     *
//     * @param conn
//     * @return
//     */
//    private boolean isIncomingConnection(final ConnectionContainerUI conn) {
//        switch (conn.getType()) {
//            case WFMIN:
//            case WFMTHROUGH:
//                return true;
//            default:
//                return false;
//        }
//    }

    /** {@inheritDoc} */
    @Override
    public void connectionUIInformationChanged(final ConnectionUIInformationEvent evt) {
        Display.getDefault().syncExec(this::refreshVisuals);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void refreshVisuals() {
        super.refreshVisuals();
        LOGGER.debug("refreshing visuals for: " + getModel());
        ConnectionUIInformation ei = null;
        ei = getModel().getUIInfo();
        LOGGER.debug("modelling info: " + ei);

        // make flow variable port connections look red.
        final CurvedPolylineConnection fig = (CurvedPolylineConnection)getFigure();
        if (getModel().isFlowVariablePortConnection()) {
            fig.setForegroundColor(AbstractPortFigure.getFlowVarPortColor());
        }

        //update 'curved' settings and line width
        EditorUIInformation uiInfo = getCurrentEditorSettings();
        fig.setCurved(uiInfo.getHasCurvedConnections());
        fig.setLineWidth(uiInfo.getConnectionLineWidth());

        // recreate list of bendpoints
        ArrayList<AbsoluteBendpoint> constraint =
                new ArrayList<AbsoluteBendpoint>();
        if (ei != null) {
            int[][] p = ei.getAllBendpoints();
            for (int i = 0; i < p.length; i++) {
                AbsoluteBendpoint bp = new AbsoluteBendpoint(p[i][0], p[i][1]);
                constraint.add(bp);
            }
        }

        fig.setRoutingConstraint(constraint);
    }

    private EditorUIInformation getCurrentEditorSettings() {
        //use the workflow editor (instead of the WorkflowManager) to get the settings from, otherwise
        //the settings won't get inherited from the parent workflow (if the displayed workflow is a metanode)
        return ((WorkflowEditor)((DefaultEditDomain)getViewer().getEditDomain()).getEditorPart())
            .getCurrentEditorSettings();
    }

    /** {@inheritDoc} */
    @Override
    public void progressChanged(final ConnectionProgressEvent pe) {
        ProgressPolylineConnection conn =
            (ProgressPolylineConnection)getFigure();
        conn.progressChanged(pe.getConnectionProgress());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void workflowEditorModeWasSet(final WorkflowEditorMode newMode) {
        m_currentEditorMode = newMode;
        final Color fgColor = WorkflowEditorMode.NODE_EDIT.equals(newMode) ? m_figureOriginalForegroundColor
                : ColorConstants.lightGray;
        final CurvedPolylineConnection cpc = (CurvedPolylineConnection)getFigure();

        cpc.setForegroundColor(fgColor);
        cpc.repaint();
    }
}
