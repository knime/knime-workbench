/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by 
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
 */

package org.knime.workbench.explorer.view.actions;

import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystemUtils;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.view.ExplorerView;
import org.knime.workbench.explorer.view.dnd.DragAndDropUtils;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * Action for stepwise workflow execution using QuickForm nodes.
 *
 * @author Thomas Gabriel, KNIME.com, Zurich, Switzerland
 * @since 3.1
 */
public class GlobalQuickformWorkflowAction extends ExplorerAction {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(GlobalQuickformWorkflowAction.class);

    private static final ImageDescriptor IMG = KNIMEUIPlugin
            .imageDescriptorFromPlugin(ExplorerActivator.PLUGIN_ID,
                    "icons/quickform.png");

    /** ID of the global rename action in the explorer menu. */
    public static final String QUICKFORM_ACTION_ID =
            "org.knime.workbench.explorer.action.quickform-execute-workflow";

    private static final int SIZING_WIZARD_WIDTH  = 800;
    private static final int SIZING_WIZARD_HEIGHT = 600;

    /**
     * @param viewer the associated tree viewer
     */
    public GlobalQuickformWorkflowAction(final ExplorerView viewer) {
        super(viewer, "QuickForm Execution...");
        setImageDescriptor(IMG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return QUICKFORM_ACTION_ID;
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
            LOGGER.error("Can only execute workflows from LOCAL space.");
            return;
        }

        try {
            if (ExplorerFileSystemUtils.lockWorkflow(
                    (LocalExplorerFileStore) wfStore)) {
                final WorkflowManager wfm = getWorkflow();

                QuickformExecuteWizard newWiz = new QuickformExecuteWizard(wfm);
                // newWiz.init(PlatformUI.getWorkbench(), getSelection());
                final QuickformExecuteWizardDialog dialog
                        = new QuickformExecuteWizardDialog(
                        getParentShell(), newWiz);
                dialog.create();
                dialog.getShell().setSize(Math.max(SIZING_WIZARD_WIDTH,
                        dialog.getShell().getSize().x),
                        SIZING_WIZARD_HEIGHT);
                dialog.open(); // wizard executes the flow
            } else {
                LOGGER.info("The workflow cannot be executed as "
                        + "is still in use by another user/instance.");
            }
        } finally {
            ExplorerFileSystemUtils.unlockWorkflow(
                    (LocalExplorerFileStore) wfStore);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEnabled() {
        final WorkflowManager wfm = getWorkflow();
        return (wfm != null);
    }

}
