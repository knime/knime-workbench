/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 * Created: May 19, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer.view.actions;

import java.util.List;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileInfo;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ContentDelegator;
import org.knime.workbench.explorer.view.ExplorerView;

/**
 * Action to create a new workflow.
 *
 * @author ohl, University of Konstanz
 */
public class NewWorkflowAction extends ExplorerAction {

    /** ID of the global new workflow action in the explorer menu. */
    public static final String ID =
            "org.knime.explorer.view.actions.newflow";

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(NewWorkflowAction.class);

    private static final int SIZING_WIZARD_WIDTH = 470;

    private static final int SIZING_WIZARD_HEIGHT = 550;

    private static final ImageDescriptor ICON = ExplorerActivator
            .imageDescriptorFromPlugin(ExplorerActivator.PLUGIN_ID,
                    "icons/new_knime16.png");;

    /**
     * @param viewer underlying viewer
     */
    public NewWorkflowAction(final ExplorerView viewer) {
        super(viewer, "New KNIME Workflow...");
        setImageDescriptor(ICON);
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
    public boolean isEnabled() {
        // only enabled if exactly on file is selected
        List<AbstractExplorerFileStore> files = getAllSelectedFiles();
        if (isRO() || files.size() != 1) {
            return false;
        }
        AbstractExplorerFileStore file = files.get(0);
        AbstractExplorerFileInfo fileInfo = file.fetchInfo();
        // for workflow groups check if it is writable
        if (fileInfo.isWorkflowGroup() && fileInfo.isModifiable()) {
            return true;
        }
        // for other items check if the parent is writable
        return file.getParent() != null
                && file.getParent().fetchInfo().isModifiable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        Map<AbstractContentProvider, List<AbstractExplorerFileStore>> sel =
                getSelectedFiles();
        if (sel.size() != 1) {
            LOGGER.info("Select only files from one mount point "
                    + "to create new workflow");
            return;
        }
        AbstractContentProvider provider = sel.keySet().iterator().next();
        List<AbstractExplorerFileStore> list = sel.get(provider);
        if (list == null || list.size() != 1) {
            // no selection in our sub tree
            LOGGER.debug("No selection: no action!");
            return;
        }
        if (!(list.get(0).getMountID().equals(provider.getMountID()))) {
            LOGGER.error("Internal Error: wrong file type selected.");
            return;
        }
        NewWorkflowWizard newWiz = new NewWorkflowWizard();
        newWiz.init(PlatformUI.getWorkbench(), getSelection());

        WizardDialog dialog = new WizardDialog(getParentShell(), newWiz);
        dialog.create();
        dialog.getShell().setSize(
                Math.max(SIZING_WIZARD_WIDTH, dialog.getShell().getSize().x),
                SIZING_WIZARD_HEIGHT);
        int ok = dialog.open();
        if (ok == Window.OK) {
            // update the tree
            IWizardPage currentPage = dialog.getCurrentPage();
            if (currentPage instanceof NewWorkflowWizardPage) {
                NewWorkflowWizardPage nwwp = (NewWorkflowWizardPage)currentPage;
                AbstractExplorerFileStore file = nwwp.getNewFile();
                Object p = ContentDelegator.getTreeObjectFor(file.getParent());
                Object o = ContentDelegator.getTreeObjectFor(file);
                getViewer().refresh(p);
                getViewer().setSelection(new StructuredSelection(o), true);

            }
        }
    }
}
