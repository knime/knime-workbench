/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * History
 *   6.06.2011 Peter Ohl, KNIME.com, Zurich, Switzerland
 */
package org.knime.workbench.explorer.view.actions.imports;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ExplorerView;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * Wizard for importing workflows and workflow groups from a directory or an
 * archive file into the explorer.
 */
public class WorkflowImportWizard extends Wizard {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            WorkflowImportWizard.class);

    private WorkflowImportSelectionPage m_importPage;

    private AbstractExplorerFileStore m_initialDestination;

    private AbstractExplorerFileStore m_target;

    private String m_selectedFile;

    /**
     *
     */
    public WorkflowImportWizard() {
        super();
        setWindowTitle("Import");
        setNeedsProgressMonitor(true);
    }

    /**
     * Sets the initial destination of the import, only workflow groups or root
     * are allowed. If a workflow or node is passed the next higher workflow
     * group or - eventually - root is chosen.
     *
     * @param destination the inital destination of the import
     */
    public void setInitialDestination(
            final AbstractExplorerFileStore destination) {
        if (destination == null) {
            m_initialDestination = null;
            // if no initial selection is made, chose LOCAL, or the first local mountpoint
            Map<String, AbstractContentProvider> mountedContent = ExplorerMountTable.getMountedContent();
            AbstractContentProvider firstlocal = null;
            for (AbstractContentProvider prov : mountedContent.values()) {
                if (!prov.isRemote() && prov.isWritable()) {
                    if (firstlocal == null) {
                        firstlocal = prov;
                    }
                    if (prov.getMountID().equals("LOCAL")) {
                        m_initialDestination = prov.getFileStore("/");
                        return;
                    }
                }
            }
            // didn't find the LOCAL (they renamed the workspace mount point), use the first non-remote we got.
            if (firstlocal != null) {
                m_initialDestination = firstlocal.getFileStore("/");
            }
            return;
        } else {
            AbstractContentProvider prov = destination.getContentProvider();
            if (prov.isWritable()) {
                m_initialDestination = destination;
                while (!AbstractExplorerFileStore.isWorkflowGroup(m_initialDestination)) {
                    AbstractExplorerFileStore f = m_initialDestination;
                    m_initialDestination = m_initialDestination.getParent();
                    if (m_initialDestination == null || m_initialDestination == f) {
                        // that is strange - at least the root should be valid...
                        m_initialDestination = null;
                        return;
                    }
                }
            }
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void addPages() {
        super.addPages();
        m_importPage = new WorkflowImportSelectionPage();
        m_importPage.restoreDialogSettings();
        m_importPage.setInitialTarget(m_initialDestination);
        if (StringUtils.isNotEmpty(m_selectedFile)) {
            m_importPage.setSelectedZipFile(m_selectedFile);
        }
        addPage(m_importPage);
        // the next page is returned by the import page
        setForcePreviousAndNextButtons(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean performFinish() {
        // save WidgetValues
        m_importPage.saveDialogSettings();
        m_target = m_importPage.getDestination();
        return createWorkflows();
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public IWizardPage getNextPage(final IWizardPage page) {
        return m_importPage.getNextPage();
    }



    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean canFinish() {
        return m_importPage.canFinish();
    }

    /**
     * Create the selected workflows.
     *
     * @return boolean <code>true</code> if all project creations were
     *         successful.
     */
    public boolean createWorkflows() {
       final AbstractExplorerFileStore target = m_importPage.getDestination();
       final Collection<IWorkflowImportElement> workflows = m_importPage
           .getWorkflowsToImport();
       // validate target path
       WorkspaceModifyOperation op = new WorkflowImportOperation(
               workflows, target, getShell());
        // run the new project creation operation
        try {
            getContainer().run(true, false, op);
        } catch (InterruptedException e) {
            return false;
        } catch (InvocationTargetException e) {
            // one of the steps resulted in a core exception
            Throwable t = e.getTargetException();
            String message = "Error during import!";
            IStatus status;
            if (t instanceof CoreException) {
                status = ((CoreException) t).getStatus();
            } else {
                status = new Status(IStatus.ERROR,
                        ExplorerActivator.PLUGIN_ID, 1, message, t);
            }
            LOGGER.error(message, t);
            ErrorDialog.openError(getShell(), message, null, status);
            return false;
        } catch (Exception e) {
            String message = "Error during import!";
            IStatus status = new Status(IStatus.ERROR,
                    KNIMEUIPlugin.PLUGIN_ID, 1, message, e);
            ErrorDialog.openError(getShell(), message, null, status);
            LOGGER.error(message, e);
        }
        // select imported items in explorer view
        selectImportedElementsInExplorerView(m_importPage.getSelectedTopLevelElements());

        return true;
    }


    private void selectImportedElementsInExplorerView(final ArrayList<IWorkflowImportElement> topLevelElements) {
        IWorkbenchWindow wbWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (wbWindow == null) {
            return;
        }
        IWorkbenchPage activePage = wbWindow.getActivePage();
        if (activePage == null) {
            return;
        }
        IViewPart viewPart = activePage.findView("org.knime.workbench.explorer.view");
        if (viewPart == null) {
            return;
        }
        if (!(viewPart instanceof ExplorerView)) {
            return;
        }
        ExplorerView explorer = (ExplorerView)viewPart;
        ArrayList<AbstractExplorerFileStore> importedElements = new ArrayList<AbstractExplorerFileStore>();
        AbstractExplorerFileStore importRoot = m_importPage.getDestination();
        for (IWorkflowImportElement e : topLevelElements) {
            IPath topPath = e.getRenamedPath();
            if (topPath.segmentCount() > 0) {
                importedElements.add(importRoot.getChild(topPath.toString()));
            } else {
                importedElements.add(importRoot);
            }
        }
        explorer.setNextSelection(importedElements);
        explorer.getViewer().reveal(importRoot);
        explorer.getViewer().expandToLevel(importRoot, 1);
        importRoot.refresh();
    }


    /**
     * @return the destination where flows were imported to
     */
    AbstractExplorerFileStore getDestinationContainer() {
        return m_target;
    }

    /**
     * @param selectedFile the file selected in the import wizard
     * @since 7.3
     */
    public void setSelectedZipFile(final String selectedFile) {
        m_selectedFile = selectedFile;
    }

}
