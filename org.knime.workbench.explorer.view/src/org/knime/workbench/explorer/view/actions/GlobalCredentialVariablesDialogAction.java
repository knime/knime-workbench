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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.Credentials;
import org.knime.core.node.workflow.CredentialsStore;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystemUtils;
import org.knime.workbench.explorer.view.dnd.DragAndDropUtils;
import org.knime.workbench.ui.masterkey.CredentialVariablesDialog;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class GlobalCredentialVariablesDialogAction extends ExplorerAction {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            GlobalCredentialVariablesDialogAction.class);

    /** ID of the global rename action in the explorer menu. */
    public static final String CREDENTIAL_ACTION_ID =
        "org.knime.workbench.explorer.action.credentials-dialog";

    /**
     * @param viewer the associated tree viewer
     */
    public GlobalCredentialVariablesDialogAction(final TreeViewer viewer) {
        super(viewer, "Workflow Credentials...");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
       return CREDENTIAL_ACTION_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        List<AbstractExplorerFileStore> fileStores =
            DragAndDropUtils.getExplorerFileStores(getSelection());
        AbstractExplorerFileStore wfStore = fileStores.get(0);
        WorkflowManager workflow = getWorkflow();

        if (ExplorerFileSystemUtils.lockWorkflow(wfStore)) {
            try {
                showDialog(workflow);
            } finally {
                ExplorerFileSystemUtils.unlockWorkflow(wfStore);
            }
        } else {
            LOGGER.info("The workflow credentials cannot be edited as the "
                    + "workflow is in use by another user/instance.\n");
            showCantEditCredentialsLockMessage();
        }

    }

    private void showDialog(final WorkflowManager wfm) {
        // open the dialog
        final Display d = Display.getDefault();
        // run in UI thread
        d.asyncExec(new Runnable() {
            @Override
            public void run() {
                CredentialsStore store = wfm.getCredentialsStore();
                CredentialVariablesDialog dialog =
                    new CredentialVariablesDialog(d.getActiveShell(), store,
                        wfm.getName());
                if (dialog.open() == Dialog.OK) {
                    for (Credentials cred : store.getCredentials()) {
                        store.remove(cred.getName());
                    }
                    List<Credentials> credentials = dialog.getCredentials();
                    for (Credentials cred : credentials) {
                        store.add(cred);
                    }
                }
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

    private void showCantEditCredentialsLockMessage() {
        MessageBox mb =
                new MessageBox(getParentShell(), SWT.ICON_ERROR | SWT.OK);
        mb.setText("Can't Lock for Editing Credentials");
        mb.setMessage("The workflow credentials cannot be edited as the "
                + "workflow is in use by another user/instance.\n");
        mb.open();
    }
}
