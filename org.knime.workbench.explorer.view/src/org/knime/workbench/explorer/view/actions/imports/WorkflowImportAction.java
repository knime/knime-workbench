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
 * Created: May 19, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer.view.actions.imports;

import java.util.List;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.filesystem.ExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ContentDelegator;
import org.knime.workbench.explorer.view.actions.ExplorerAction;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * Action to export workflow(s).
 *
 * @author ohl, University of Konstanz
 */
public class WorkflowImportAction extends ExplorerAction {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(WorkflowImportAction.class);

    private static final int SIZING_WIZARD_WIDTH = 470;

    private static final int SIZING_WIZARD_HEIGHT = 550;

    private static final ImageDescriptor ICON = KNIMEUIPlugin
            .imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
                    "icons/knime_import.png");;

    /**
     * @param viewer underlying viewer
     */
    public WorkflowImportAction(final TreeViewer viewer) {
        super(viewer, "Import KNIME Workflow...");
        setImageDescriptor(ICON);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return "org.knime.explorer.view.actions.import";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        WorkflowImportWizard impWiz = new WorkflowImportWizard();
        Map<AbstractContentProvider, List<ExplorerFileStore>> selectedFiles =
                getSelectedFiles();
        ExplorerFileStore sel = null;
        if (selectedFiles != null && selectedFiles.size() > 0) {
            sel = selectedFiles.values().iterator().next().iterator().next();
        }
        impWiz.setInitialDestination(sel);

        WizardDialog dialog = new WizardDialog(getParentShell(), impWiz);
//        dialog.create();
//        dialog.getShell().setSize(
//                Math.max(SIZING_WIZARD_WIDTH, dialog.getShell().getSize().x),
//                SIZING_WIZARD_HEIGHT);
        dialog.open();

        ExplorerFileStore destination = impWiz.getDestinationContainer();
        Object object = ContentDelegator.getTreeObjectFor(destination);
        getViewer().refresh(object);
        getViewer().reveal(object);
    }

}
