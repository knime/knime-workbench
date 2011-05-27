/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (c) KNIME.com, Zurich, Switzerland
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
 * Created: May 27, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer.view.actions;

import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.filesystem.ExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ContentDelegator;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 *
 * @author ohl, University of Konstanz
 */
public class NewWorkflowGroupAction extends ExplorerAction {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(NewWorkflowGroupAction.class);

    private static final int SIZING_WIZARD_WIDTH = 470;

    private static final int SIZING_WIZARD_HEIGHT = 550;

    private static final ImageDescriptor ICON = KNIMEUIPlugin
            .imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
                    "icons/wf_group_new.png");;

    /**
     * @param viewer underlying viewer
     * @param teamSpaceProvider the sub tree provider
     */
    public NewWorkflowGroupAction(final TreeViewer viewer) {
        super(viewer, "New Workflow Group...");
        setImageDescriptor(ICON);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return "org.knime.explorer.view.actions.newgroup";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        // if selections belong to one content provider
        return getSelectedFiles().size() == 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        Map<AbstractContentProvider, List<ExplorerFileStore>> sel =
                getSelectedFiles();
        if (sel.size() != 1) {
            LOGGER.info("Select only files from one mount point "
                    + "to create a new workflow group");
            return;
        }
        AbstractContentProvider provider = sel.keySet().iterator().next();
        List<ExplorerFileStore> list = sel.get(provider);
        if (list == null || list.size() != 1) {
            // no selection in our sub tree
            LOGGER.debug("No selection: no action!");
            return;
        }
        if (!(list.get(0).getMountID().equals(provider.getMountID()))) {
            LOGGER.error("Internal Error: wrong file type selected.");
            return;
        }
        NewWorkflowWizard newWiz = new NewWorkflowGroupWizard(provider);

        newWiz.init(PlatformUI.getWorkbench(), getSelection());

        WizardDialog dialog = new WizardDialog(getParentShell(), newWiz);
        dialog.create();
        dialog.getShell().setSize(
                Math.max(SIZING_WIZARD_WIDTH, dialog.getShell().getSize().x),
                SIZING_WIZARD_HEIGHT);
        int ok = dialog.open();
        if (ok == Dialog.OK) {
            // update the tree
            IWizardPage currentPage = dialog.getCurrentPage();
            if (currentPage instanceof NewWorkflowWizardPage) {
                NewWorkflowWizardPage nwwp = (NewWorkflowWizardPage)currentPage;
                ExplorerFileStore file = nwwp.getNewFile();
                Object p = ContentDelegator.getTreeObjectFor(file.getParent());
                Object o = ContentDelegator.getTreeObjectFor(file);
                getViewer().update(p, null);
                getViewer().setSelection(new StructuredSelection(o), true);
            }
        }
    }

}
