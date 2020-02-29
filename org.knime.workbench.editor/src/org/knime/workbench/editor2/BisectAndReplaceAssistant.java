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
 *   Mar 30, 2020 (loki): created
 */
package org.knime.workbench.editor2;

import java.util.Set;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.ConnectionContainer.ConnectionType;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * The genesis of this is the rearchitecture documented in https://knime-com.atlassian.net/browse/AP-11772
 *
 * @author loki der quaeler
 */
public class BisectAndReplaceAssistant {
    /**
     * The result of the methods inquiring as to whether a node or connection can be affected.
     *
     * @see BisectAndReplaceAssistant#canAffectNode(WorkflowManager, NodeContainer, boolean)
     * @see BisectAndReplaceAssistant#canBisectConnection(WorkflowManager, ConnectionContainer)
     */
    public enum Result {
        /** Node or connection can be affected */
        OK,
        /** Node cannot be removed from the workflow manager */
        CANNOT_REMOVE_NODE,
        /**
         * The incoming connection cannot be removed from the workflow manager, used with:
         *  {@link BisectAndReplaceAssistant#canAffectNode(WorkflowManager, NodeContainer, boolean)}
         */
        CANNOT_REMOVE_INCOMING_CONNECTION,
        /**
         * An outgoing connection cannot be removed from the workflow manager, used with:
         *  {@link BisectAndReplaceAssistant#canAffectNode(WorkflowManager, NodeContainer, boolean)}
         */
        CANNOT_REMOVE_OUTGOING_CONNECTION,
        /**
         * The connection cannot be removed from the workflow manager, used with:
         *  {@link BisectAndReplaceAssistant#canBisectConnection(WorkflowManager, ConnectionContainer)}
         */
        CANNOT_REMOVE_CONNECTION,
        /** The user was solicited for input and decided to prevent the replace action */
        USER_PREVENTED_REPLACE,
        /** The execute state of a node being affected prevents the action */
        EXECUTE_STATE_PREVENTED_REPLACE;
    }


    private static final String CONNECTION_DROP_WARNING =
        "You are altering the existing connection between two nodes; are you sure you want to do this?";
    private static final String NODE_DROP_WARNING =
        "You are replacing an existing node; are you sure you want to do this?";
    private static final String UNABLE_TO_DROP_DIALOG_TITLE = "Unable to complete";
    private static final String UNABLE_TO_DROP_DIALOG_MESSAGE =
        "The drop action cannot be completed, likely because a node being affected is in a paused, or waiting to be "
                        + "executed, state.";


    /**
     * A convenience method which places a node into a location; i would have imagined this would exist elsewhere in the
     * code base but searching on "setUIInformation" reveals a number of usages, but not something this minimal.
     *
     * @param node the <code>NodeContainerEditPart</code> representing the node to move
     * @param destinationBounds location information in the format returned by
     *            <code>NodeUIInformation.getBounds()</code>
     */
    public static void moveNodeToLocation(final NodeContainerEditPart node, final int[] destinationBounds) {
        final NodeContainerUI container = node.getNodeContainer();
        final NodeUIInformation.Builder builder = NodeUIInformation.builder(container.getUIInformation());

        builder.setNodeLocation(destinationBounds[0], destinationBounds[1], destinationBounds[2], destinationBounds[3]);
        container.setUIInformation(builder.build());
    }

    /**
     * A convenience method to display a dialog needed across classes.
     *
     * @param result the enum value returned by one of the methods of this class
     * @see #canAffectNode(WorkflowManager, NodeContainer, boolean)
     * @see #canBisectConnection(WorkflowManager, ConnectionContainer)
     * @see NodeSupplantDragListener#mouseUp(org.eclipse.swt.events.MouseEvent)
     */
    public static void displayUnableToDropDialogIfAppropriate(final Result result) {
        // The two checked states would have potentially already shown interceding dialog questions to the user
        if (!Result.USER_PREVENTED_REPLACE.equals(result) && !Result.EXECUTE_STATE_PREVENTED_REPLACE.equals(result)) {
            MessageDialog.openInformation(PlatformUI.getWorkbench().getDisplay().getActiveShell(),
                                          UNABLE_TO_DROP_DIALOG_TITLE,
                                          UNABLE_TO_DROP_DIALOG_MESSAGE);
        }
    }

    /**
     * Used to decide whether a node can be affected (either replaced, or augmented - like a port addition or removal.)
     * For an event diagram, see the JIRA issue cited in the class javadocs for this class.
     *
     * @param wm the workflow manager which owns the node in question
     * @param nodeToAffected the node container which will be affected (e.g if this action is part of a node replace,
     *            then this should be the node which will be replaced - not the replacing node)
     * @param nodeWillBeReplaced true if the parameter node is being replaced, false for cases where it is only being
     *            materially modified
     * @return one of the {@code Result} enum values representing whether the node in question can be affected
     */
    public static Result canAffectNode(final WorkflowManager wm, final NodeContainer nodeToAffected,
                                       final boolean nodeWillBeReplaced) {
        final NodeID nid = nodeToAffected.getID();

        if (!wm.canRemoveNode(nid)) {
            return Result.CANNOT_REMOVE_NODE;
        }

        for (final ConnectionContainer connection : wm.getIncomingConnectionsFor(nid)) {
            if (!wm.canRemoveConnection(connection)) {
                return Result.CANNOT_REMOVE_INCOMING_CONNECTION;
            }
        }
        final Set<ConnectionContainer> outgoingConnections = wm.getOutgoingConnectionsFor(nid);
        final ConnectionContainer[] outgoing = new ConnectionContainer[outgoingConnections.size()];
        int index = 0;
        for (final ConnectionContainer connection : outgoingConnections) {
            outgoing[index++] = connection;
            if (!wm.canRemoveConnection(connection)) {
                return Result.CANNOT_REMOVE_OUTGOING_CONNECTION;
            }
        }

        if (nodeWillBeReplaced && !replacingNodeOrConnectionBisectionIsAllowed(false)) {
            return Result.USER_PREVENTED_REPLACE;
        }

        if (!executedStateAllowsAffect(wm, nodeToAffected, outgoing)) {
            return Result.EXECUTE_STATE_PREVENTED_REPLACE;
        }

        return Result.OK;
    }

    /**
     * Used to decide whether a connection can be bisected (technically, whether it can be removed / replaced.)
     *
     * @param wm the workflow manager which owns the connection in question
     * @param connectionToBisect the connection which will be bisected
     * @return one of the {@code Result} enum values representing whether the connection in question can be affected
     */
    public static Result canBisectConnection(final WorkflowManager wm,
                                             final ConnectionContainer connectionToBisect) {
        if (!wm.canRemoveConnection(connectionToBisect)) {
            return Result.CANNOT_REMOVE_CONNECTION;
        }

        if (!replacingNodeOrConnectionBisectionIsAllowed(true)) {
            return Result.USER_PREVENTED_REPLACE;
        }

        if (!executedStateAllowsAffect(wm, null, connectionToBisect)) {
            return Result.EXECUTE_STATE_PREVENTED_REPLACE;
        }

        return Result.OK;
    }

    /**
     * @param forConnection true if the action is bisecting a connection, false if it is replacing a node
     * @return true if the user's preferences do not require an intervention or if they do but the user ok'd, false
     *         otherwise
     */
    @SuppressWarnings("deprecation")
    private static boolean replacingNodeOrConnectionBisectionIsAllowed(final boolean forConnection) {
        final IPreferenceStore store = KNIMEUIPlugin.getDefault().getPreferenceStore();
        if (!store.contains(PreferenceConstants.P_CONFIRM_REPLACE)
            || store.getBoolean(PreferenceConstants.P_CONFIRM_REPLACE)) {
            final String msg = forConnection ? CONNECTION_DROP_WARNING : NODE_DROP_WARNING;
            final MessageDialogWithToggle dialog = MessageDialogWithToggle.openOkCancelConfirm(
                SWTUtilities.getActiveShell(), "Confirm ...", msg, "Do not ask again", false, null, null);

            if (dialog.getReturnCode() != IDialogConstants.OK_ID) {
                return false;
            }
            if (dialog.getToggleState()) {
                store.setValue(PreferenceConstants.P_CONFIRM_REPLACE, false);
                KNIMEUIPlugin.getDefault().savePluginPreferences();
            }
        }

        return true;
    }

    /**
     * This checks for an executed state of downstream nodes and, if the user's preference is such, dialog's the user to
     * determine whether they really want to perform the action which will move those nodes out of that state.
     *
     * @param wm the <code>WorkflowManager</code> instance containining the connection(s) in question
     * @param node if non-null, <code>wm</code> will have its <code>canRemoveNode(NodeID)</node> consulted and if false
     *            is returned, this will trigger a potential dialog
     * @param connections one or more connections whose destinations will be checked for an executed state
     * @return true if the replacement can occur
     */
    @SuppressWarnings("deprecation")
    private static boolean executedStateAllowsAffect(final WorkflowManager wm, final NodeContainer node,
                                                     final ConnectionContainer... connections) {
        boolean aWarnableStateExists
                = (node != null)
                    ? (((connections != null) && (connections.length > 0))
                            && (node.getNodeContainerState().isExecuted() || !wm.canRemoveNode(node.getID())))
                    : false;

        if (!aWarnableStateExists && (connections != null)) {
            for (final ConnectionContainer connectionContainer : connections) {
                WorkflowManager wmToFindDestNode = wm;
                if (doesConnectionLeaveWorkflow(connectionContainer)) {
                    wmToFindDestNode = wm.getParent();
                }
                if (wmToFindDestNode.findNodeContainer(connectionContainer.getDest()).getNodeContainerState()
                    .isExecuted()) {
                    aWarnableStateExists = true;

                    break;
                }
            }
        }

        if (aWarnableStateExists) {
            final IPreferenceStore store = KNIMEUIPlugin.getDefault().getPreferenceStore();
            if (!store.contains(PreferenceConstants.P_CONFIRM_RESET)
                    || store.getBoolean(PreferenceConstants.P_CONFIRM_RESET)) {
                final MessageDialogWithToggle dialog =
                    MessageDialogWithToggle.openOkCancelConfirm(SWTUtilities.getActiveShell(), "Confirm reset...",
                        "This operation requires to reset all downstream node(s). Do you want to proceed?",
                        "Do not ask again", false, null, null);
                if (dialog.getReturnCode() != IDialogConstants.OK_ID) {
                    return false;
                }
                if (dialog.getToggleState()) {
                    store.setValue(PreferenceConstants.P_CONFIRM_RESET, false);
                    KNIMEUIPlugin.getDefault().savePluginPreferences();
                }
            }
        }

        return true;
    }

    private static final boolean doesConnectionLeaveWorkflow(final ConnectionContainer connection) {
        return connection.getType() == ConnectionType.WFMOUT;
    }
}
