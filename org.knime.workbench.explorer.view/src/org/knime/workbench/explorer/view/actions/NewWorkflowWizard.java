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
 * ---------------------------------------------------------------------
 *
 * Created: May 19, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer.view.actions;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.localworkspace.LocalWorkspaceContentProviderFactory;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.dnd.DragAndDropUtils;

/**
 *
 * @author ohl, University of Konstanz
 */
public class NewWorkflowWizard extends Wizard implements INewWizard {

    private NewWorkflowWizardPage m_page;

    private AbstractExplorerFileStore m_initialSelection;
    private String[] m_mountIDs;

    /**
     * Creates the wizard.
     * @since 3.0
     */
    public NewWorkflowWizard() {
        setNeedsProgressMonitor(true);
    }

    /**
     * @return true if a workflow is created, false otherwise
     * @since 3.0
     */
    protected boolean isWorkflowCreated() {
        return true;
    }

    /**
     * @param workbench see {@link #init(IWorkbench, IStructuredSelection)}
     * @param selection see {@link #init(IWorkbench, IStructuredSelection)}
     * @param mountPointFilter predicate to filter the allowed mountpoints
     */
    public void init(final IWorkbench workbench, final IStructuredSelection selection,
        final Predicate<AbstractContentProvider> mountPointFilter) {
        m_mountIDs = getValidMountpoints() //
            .filter(e -> mountPointFilter.test(e.getValue())) //
            .map(Map.Entry::getKey) //
            .toArray(String[]::new);
        init(workbench, selection);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final IWorkbench workbench,
            final IStructuredSelection selection) {

        List<String> validMountPointList = getValidMountpoints().map(Map.Entry::getKey).collect(Collectors.toList());
        if (m_mountIDs == null) {
            m_mountIDs = validMountPointList.toArray(String[]::new);
        }

        if ((selection != null) && !selection.isEmpty()) {
            String defaultLocalID = new LocalWorkspaceContentProviderFactory().getDefaultMountID();

            Map<AbstractContentProvider, List<AbstractExplorerFileStore>> providerMap =
                DragAndDropUtils.getProviderMap(selection);
            if (providerMap != null) {
                AbstractExplorerFileStore firstSelectedItem = providerMap.values().iterator().next().get(0);
                // use a different default selection if:
                //   - the selected mount point isn't writable (e.g. missing teamspace license)
                //   - a remote workflow is requested to be created (not supported)
                if (!validMountPointList.contains(firstSelectedItem.getMountID())
                        || (isWorkflowCreated() && firstSelectedItem.getContentProvider().isRemote())) {
                    // can't create workflow on the selected item (it is remote)
                    if (ExplorerMountTable.getMountPoint(defaultLocalID) != null) {
                        m_initialSelection =
                                ExplorerMountTable.getMountPoint(defaultLocalID).getProvider().getRootStore();
                    } else {
                        // find some local content provider to use as a fallback
                        Optional<AbstractContentProvider>
                                defaultLocalContentProvider = ExplorerMountTable.getMountedContent().values().stream()
                                .filter(cp -> !cp.isRemote()).findFirst();
                        m_initialSelection = defaultLocalContentProvider.map(AbstractContentProvider::getRootStore).orElse(null);
                    }
                } else if (firstSelectedItem.fetchInfo().isWorkflowGroup()) {
                    m_initialSelection = firstSelectedItem;
                } else {
                    m_initialSelection = firstSelectedItem.getParent();
                }
            } else {
                m_initialSelection = ExplorerMountTable.getMountPoint(defaultLocalID).getProvider().getRootStore();
            }
        }
    }

    /**
     * Adding the page to the wizard.
     */
    @Override
    public void addPages() {
        NewWorkflowWizardPage page = new NewWorkflowWizardPage(m_mountIDs,
                m_initialSelection, true);
        addPage(page);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addPage(final IWizardPage page) {
        if (page instanceof NewWorkflowWizardPage) {
            m_page = (NewWorkflowWizardPage)page;
        }
        super.addPage(page);
    }

    /**
     * Perform finish - queries the page and creates the project / file.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean performFinish() {
        final AbstractExplorerFileStore newitem = m_page.getNewFile();
        // Create new runnable
        IRunnableWithProgress op = new IRunnableWithProgress() {
            @Override
            public void run(final IProgressMonitor monitor)
                    throws InvocationTargetException {
                try {
                    // call the worker method
                    doFinish(newitem, monitor);
                } catch (CoreException e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            }
        };
        try {
            getContainer().run(true, false, op);
        } catch (InterruptedException e) {
            return false;
        } catch (InvocationTargetException e) {
            // get the exception that issued this async exception
            Throwable realException = e.getTargetException();
            MessageDialog.openError(getShell(), "Error",
                    realException.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Worker method, creates the project using the given options.
     *
     * @param newItem filestore for the new workflow directory
     * @param monitor a progress monitor
     * @throws CoreException if an error occurs while creating the workflow
     */
    protected void doFinish(final AbstractExplorerFileStore newItem,
            final IProgressMonitor monitor) throws CoreException {
        createNewWorkflow(newItem, monitor);
    }

    protected static void throwCoreException(final String message,
            final Throwable t) throws CoreException {
        IStatus status =
                new Status(IStatus.ERROR, "org.knime.workbench.ui", IStatus.OK,
                        message, t);
        throw new CoreException(status);
    }

    /**
     * @return the initially selected file or null if multiple files or no file
     *      is selected
     * @since 3.0
     */
    protected AbstractExplorerFileStore getInitialSelection() {
        return m_initialSelection;
    }

    /**
     * @return the involved mount ids
     * @since 3.0
     */
    protected String[] getMountIDs() {
        return m_mountIDs;
    }


    /**
     * Creates a new workflow.
     *
     * @param newItem the destination of the new workflow
     * @param monitor a progress monitor, must not be <code>null</code>
     * @throws CoreException if an error occurs
     *
     * @since 5.0
     */
    public static void createNewWorkflow(final AbstractExplorerFileStore newItem, final IProgressMonitor monitor)
        throws CoreException {
        if (newItem.fetchInfo().exists()) {
            throwCoreException("Resource \"" + newItem.getFullName()
                    + "\" already exists.", null);
        }

        if (!newItem.getParent().fetchInfo().exists()) {
            throwCoreException("Parent directory doesn't exist. "
                    + "Create a workflow group before you place "
                    + "a workflow in.", null);
        }

        // create workflow dir
        newItem.mkdir(EFS.NONE, monitor);

        // create a new empty workflow file
        final AbstractExplorerFileStore workflowFile =
                newItem.getChild(WorkflowPersistor.WORKFLOW_FILE);
        OutputStream outStream =
                workflowFile.openOutputStream(EFS.NONE, monitor);
        try {
            outStream.close();
        } catch (IOException e1) {
            // don't close - don't open editor
            return;
        }
        newItem.refresh();

        // open workflow in a new editor
        monitor.setTaskName("Opening new workflow for editing...");
        File locFile = workflowFile.toLocalFile(EFS.NONE, monitor);
        if (locFile == null) {
            // only local workflows can be opened
            return;
        }
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                IEditorDescriptor editorDescriptor;
                try {
                    editorDescriptor = IDE.getEditorDescriptor(workflowFile.getName());
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                        .openEditor(new FileStoreEditorInput(workflowFile), editorDescriptor.getId());
                } catch (PartInitException ex) {
                    NodeLogger.getLogger(NewWorkflowWizard.class).info("Could not open editor for new workflow "
                        + newItem.getFullName() + ": " + ex.getMessage(), ex);
                }
            }
        });
    }

    private Stream<Map.Entry<String, AbstractContentProvider>> getValidMountpoints() {
        return ExplorerMountTable.getMountedContent().entrySet().stream()
                .filter(entry -> {
                    AbstractContentProvider cp = entry.getValue();
                    return cp.isWritable() && !(isWorkflowCreated() && cp.isRemote());
                });
    }

}
