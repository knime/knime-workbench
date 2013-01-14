/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2013
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystemUtils;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.view.ExplorerView;
import org.knime.workbench.explorer.view.dnd.DragAndDropUtils;
import org.knime.workbench.ui.wfvars.WorkflowVariablesDialog;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class GlobalOpenWorkflowVariablesDialogAction extends ExplorerAction {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(GlobalOpenWorkflowVariablesDialogAction.class);

    /** ID of the global rename action in the explorer menu. */
    public static final String WFVAR_ACTION_ID =
            "org.knime.workbench.explorer.action.workflow-vars-dialog";

    /**
     * @param viewer the associated tree viewer
     */
    public GlobalOpenWorkflowVariablesDialogAction(final ExplorerView viewer) {
        super(viewer, "Workflow Variables...");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return WFVAR_ACTION_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        List<AbstractExplorerFileStore> fileStores =
                DragAndDropUtils.getExplorerFileStores(getSelection());
        AbstractExplorerFileStore wfStore = fileStores.get(0);
        if (!(wfStore instanceof LocalExplorerFileStore)) {
            LOGGER.error("Can only show variables of local workflows.");
            return;
        }
        WorkflowManager workflow = getWorkflow();

        if (ExplorerFileSystemUtils
                .lockWorkflow((LocalExplorerFileStore)wfStore)) {
            try {
                showDialog(workflow);
            } finally {
                ExplorerFileSystemUtils
                        .unlockWorkflow((LocalExplorerFileStore)wfStore);
            }
        } else {
            LOGGER.info("The workflow variables cannot be edited as the "
                    + "workflow is in use by another user/instance.\n");
            showCantEditVarsLockMessage();
        }

    }

    private void showDialog(final WorkflowManager wfm) {
        // open the dialog
        final Display d = Display.getDefault();
        // run in UI thread
        d.asyncExec(new Runnable() {
            @Override
            public void run() {
                // and put it into the workflow variables dialog
                WorkflowVariablesDialog dialog =
                        new WorkflowVariablesDialog(d.getActiveShell(), wfm);
                dialog.open();
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return getWorkflow() != null;
    }

    private void showCantEditVarsLockMessage() {
        MessageBox mb =
                new MessageBox(getParentShell(), SWT.ICON_ERROR | SWT.OK);
        mb.setText("Can't Lock for Editing Workflow Variables");
        mb.setMessage("The workflow variables cannot be edited as the "
                + "workflow is in use by another user/instance.\n");
        mb.open();
    }
}
