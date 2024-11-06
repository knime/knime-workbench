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
 */
package org.knime.workbench.editor2.actions;

import static org.knime.core.ui.wrapper.Wrapper.unwrap;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainerState;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.ui.node.workflow.InteractiveWebViewsResultUI;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.SubNodeContainerUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Action to execute all selected nodes and open their first view.
 *
 * @author Christoph Sieb, University of Konstanz
 */
public class ExecuteAndOpenViewAction extends AbstractNodeAction {
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(ExecuteAndOpenViewAction.class);

    /**
     * unique ID for this action.
     */
    public static final String ID = "knime.action.executeandopenview";

    /**
     * @param editor The workflow editor
     */
    public ExecuteAndOpenViewAction(final WorkflowEditor editor) {
        super(editor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "Execute and Open Views\t" + getHotkey("knime.commands.executeAndOpenView");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/executeAndView.GIF");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getDisabledImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/executeAndView_diabled.PNG");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        String tooltip = "Execute the selected node";
        NodeContainerEditPart[] parts = getSelectedParts(NodeContainerEditPart.class);
        if (parts.length == 1) {
            NodeContainerUI nc = parts[0].getNodeContainer();
            if (nc.hasInteractiveView() || nc.getInteractiveWebViews().size() > 0) {
                return tooltip + " and open interactive view.";
            }
        } else {
            tooltip += "s";
        }
        return tooltip + " and open first view.";
    }

    /**
     * @return <code>true</code>, if just one node part is selected which is
     *         executable and additionally has at least one view.
     *
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean internalCalculateEnabled() {
        NodeContainerEditPart[] parts =
            getSelectedParts(NodeContainerEditPart.class);
        // enable if we have at least one executable node in our selection
        WorkflowManagerUI wm = getEditor().getWorkflowManagerUI();
        for (int i = 0; i < parts.length; i++) {
            NodeContainerUI nc = parts[i].getNodeContainer();
            boolean hasView = hasView(nc);
            if (wm.canExecuteNode(nc.getID()) && hasView) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasView(final NodeContainerUI nc) {
        boolean hasView = nc.getNrViews() > 0;
        hasView |= nc.hasInteractiveView() || nc.getInteractiveWebViews().size() > 0;
        hasView |= OpenSubnodeWebViewAction.hasContainerView(nc);
        hasView |= OpenNodeViewAction.hasNodeView(nc);
        return hasView;
    }

    private void executeAndOpen(final NodeContainerUI cont) {
        boolean hasView = hasView(cont);
        @SuppressWarnings("rawtypes")
        final InteractiveWebViewsResultUI interactiveWebViews = cont.getInteractiveWebViews();
        if (hasView) {
            // another listener must be registered at the workflow manager to
            // receive also those events from nodes that have just been queued
            cont.addNodeStateChangeListener(new NodeStateChangeListener() {
                @Override
                public void stateChanged(final NodeStateEvent state) {
                    NodeContainerState ncState = cont.getNodeContainerState();
                    // check if the node has finished (either executed or
                    // removed from the queue)
                    if (state.getSource().equals(cont.getID()) && ncState.isExecuted()) {
                        // if the node was successfully executed
                        // start the view
                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                // run open view action
                                IAction viewAction;
                                if (cont.hasInteractiveView()) {
                                    viewAction = new OpenInteractiveViewAction(unwrap(cont, NodeContainer.class));
                                } else if (cont instanceof SubNodeContainerUI) {
                                    viewAction = new OpenSubnodeWebViewAction((SubNodeContainerUI)cont);
                                } else if (interactiveWebViews.size() > 0) {
                                    viewAction = new OpenInteractiveWebViewAction(cont, interactiveWebViews.get(0));
                                } else if (OpenNodeViewAction.hasNodeView(cont)) {
                                    viewAction =
                                        new OpenNodeViewAction(Wrapper.unwrap(cont, NativeNodeContainer.class));
                                } else {
                                    viewAction = new OpenViewAction(unwrap(cont, NodeContainer.class), 0);
                                }
                                viewAction.run();
                            }
                        });
                    }
                    if (!ncState.isExecutionInProgress()) {
                        // in those cases remove the listener
                        cont.removeNodeStateChangeListener(this);
                    }
                }

            });
        }
        getManagerUI().executeUpToHere(cont.getID());
    }

    /**
     * Execute all selected nodes and open their first view (if available).
     *
     * {@inheritDoc}
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        LOGGER.debug("Creating 'Execute and Open Views' job for "
                + nodeParts.length + " node(s)...");
        for (NodeContainerEditPart p : nodeParts) {
            final NodeContainerUI cont = p.getNodeContainer();
            executeAndOpen(cont);
        }
        try {
            // Give focus to the editor again. Otherwise the actions (selection)
            // is not updated correctly.
            getWorkbenchPart().getSite().getPage().activate(getWorkbenchPart());
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean canHandleWorkflowManagerUI() {
        return true;
    }
}
