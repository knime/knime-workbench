/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2011
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * History
 *   May 27, 2011 (morent): created
 */

package org.knime.workbench.explorer.view.actions;

import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystemUtils;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.view.dnd.DragAndDropUtils;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class GlobalExecuteWorkflowAction extends ExplorerAction {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(GlobalExecuteWorkflowAction.class);

    private static final ImageDescriptor IMG = KNIMEUIPlugin
            .imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
                    "/icons/actions/execute.gif");

    /** ID of the global rename action in the explorer menu. */
    public static final String EXECUTEWF_ACTION_ID =
            "org.knime.workbench.explorer.action.execute-workflow";

    /**
     * @param viewer the associated tree viewer
     */
    public GlobalExecuteWorkflowAction(final TreeViewer viewer) {
        super(viewer, "Execute...");
        setImageDescriptor(IMG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return EXECUTEWF_ACTION_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        WorkflowManager wfm = getWorkflow();
        List<AbstractExplorerFileStore> fileStores =
                DragAndDropUtils.getExplorerFileStores(getSelection());
        AbstractExplorerFileStore wfStore = fileStores.get(0);
        if (!(wfStore instanceof LocalExplorerFileStore)) {
            LOGGER.error("Can only execute local workflows locally.");
            return;
        }

        try {
            if (ExplorerFileSystemUtils
                    .lockWorkflow((LocalExplorerFileStore)wfStore)) {
                WorkflowManager.ROOT.executeUpToHere(wfm.getID());
            } else {
                LOGGER.info("The workflow cannot be executed as "
                        + "is still in use by another user/instance.\n"
                        + "Canceling execution.");
                showCantExecuteLockMessage();
            }
        } finally {
            ExplorerFileSystemUtils
                    .unlockWorkflow((LocalExplorerFileStore)wfStore);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return getWorkflow() != null;
    }

    private void showCantExecuteLockMessage() {
        MessageBox mb =
                new MessageBox(getParentShell(), SWT.ICON_ERROR | SWT.OK);
        mb.setText("Can't Lock for Execution");
        mb.setMessage("The workflow cannot be executed as "
                + "is still in use by another user/instance.\n"
                + "Canceling execution.");
        mb.open();
    }
}
