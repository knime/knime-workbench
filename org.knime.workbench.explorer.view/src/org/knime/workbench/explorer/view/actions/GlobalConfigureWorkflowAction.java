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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.explorer.filesystem.ExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystemUtils;
import org.knime.workbench.explorer.view.dnd.DragAndDropUtils;
import org.knime.workbench.ui.wrapper.WrappedNodeDialog;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class GlobalConfigureWorkflowAction extends ExplorerAction {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            GlobalConfigureWorkflowAction.class);

    /** ID of the global rename action in the explorer menu. */
    public static final String CONFIGUREWF_ACTION_ID =
        "org.knime.workbench.explorer.action.configure-workflow";

    /**
     * @param viewer the associated tree viewer
     */
    public GlobalConfigureWorkflowAction(final TreeViewer viewer) {
        super(viewer, "Configure...");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
       return CONFIGUREWF_ACTION_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        List<ExplorerFileStore> fileStores =
            DragAndDropUtils.getExplorerFileStores(getSelection());
        ExplorerFileStore wfStore = fileStores.get(0);

        try {
            if (ExplorerFileSystemUtils.lockWorkflow(wfStore)) {
                showDialog(getWorkflow());
            } else {
                LOGGER.info("The workflow cannot be configured as "
                + "is still in use by another user/instance.\n"
                + "Canceling configuration.");
                showCantConfigureLockMessage();
            }
        } finally {
            ExplorerFileSystemUtils.unlockWorkflow(wfStore);
        }

    }

    private void showDialog(final WorkflowManager wfm) {
        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    WrappedNodeDialog dialog = new WrappedNodeDialog(
                            Display.getDefault().getActiveShell(),
                            wfm);
                    dialog.setBlockOnOpen(true);
                    dialog.open();
                } catch (final NotConfigurableException nce) {
                    Display.getDefault().syncExec(new Runnable() {
                        @Override
                        public void run() {
                            MessageDialog.openError(
                                    Display.getDefault().getActiveShell(),
                                    "Workflow Not Configurable",
                                    "This workflow can not be "
                                    + "configured: "
                                    + nce.getMessage());
                        }
                    });
                }

            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return getWorkflow() != null && getWorkflow().hasDialog();
    }

    private void showCantConfigureLockMessage() {
        MessageBox mb =
                new MessageBox(getParentShell(), SWT.ICON_ERROR | SWT.OK);
        mb.setText("Can't Lock for Configuration");
        mb.setMessage("The workflow cannot be configured as "
                + "is still in use by another user/instance.\n"
                + "Canceling configuration.");
        mb.open();
    }
}
