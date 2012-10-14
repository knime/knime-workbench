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
 * -------------------------------------------------------------------
 *
 * History
 *   31.05.2011 (ohl): created
 */
package org.knime.workbench.explorer.view.actions.export;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodePersistor;
import org.knime.core.node.NodePersistorVersion200;
import org.knime.core.node.workflow.SingleNodeContainer;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.util.VMFileLocker;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.ContentDelegator;

/**
 * This wizard exports KNIME workflows and workflow groups if workflows are
 * selected which are in different workflow groups.
 */
public class WorkflowExportWizard extends Wizard implements INewWizard {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(WorkflowExportWizard.class);

    private WorkflowExportPage m_page;

    private ISelection m_selection;

    private final Collection<AbstractExplorerFileStore> m_workflowsToExport =
            new ArrayList<AbstractExplorerFileStore>();

    private boolean m_excludeData;

    private AbstractExplorerFileStore m_commonParent;

    private String m_fileName;

    /**
     * Constructor.
     */
    public WorkflowExportWizard() {
        super();
        setWindowTitle("Export");
        setNeedsProgressMonitor(true);
    }

    /**
     * Adding the page to the wizard.
     */
    @Override
    public void addPages() {
        AbstractExplorerFileStore initFile = null;
        if (m_selection instanceof IStructuredSelection) {
            initFile =
                    ContentDelegator
                            .getFileStore(((IStructuredSelection)m_selection)
                                    .getFirstElement());
        }
        m_page = new WorkflowExportPage(initFile);
        addPage(m_page);
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean canFinish() {
        return m_page.isPageComplete();
    }

    /**
     * This method is called when 'Finish' button is pressed in the wizard. We
     * will create an operation and run it using wizard as execution context.
     *
     * @return If finished successfully
     */
    @Override
    public boolean performFinish() {

        // first save dirty editors
        boolean canceled = !PlatformUI.getWorkbench().saveAllEditors(true);
        if (canceled) {
            return false;
        }

        m_page.saveDialogSettings();

        m_commonParent = m_page.getSelectedStore();
        m_fileName = m_page.getFileName().trim();
        m_excludeData = m_page.excludeData();
        m_workflowsToExport.clear();
        // get all workflows and workflow groups selected
        m_workflowsToExport.addAll(m_page.getWorkflowsOrGroups());
        boolean containsFlow = false;
        for (AbstractExplorerFileStore efs : m_workflowsToExport) {
            if (AbstractExplorerFileStore.isWorkflow(efs)) {
                containsFlow = true;
                break;
            }
        }
        if (!containsFlow) {
            m_page.setErrorMessage("No workflows contained in the selection. "
                    + "Nothing to export.");
            return false;
        }

        // if the specified export file already exist ask the user
        // for confirmation

        final File exportFile = new File(m_fileName);

        if (exportFile.exists()) {
            // if it exists we have to check if we can write to:
            if (!exportFile.canWrite() || exportFile.isDirectory()) {
                // display error
                m_page.setErrorMessage("Cannot write to specified file");
                return false;
            }
            boolean overwrite =
                    MessageDialog.openQuestion(getShell(),
                            "File already exists...",
                            "File already exists.\nDo you want to "
                                    + "overwrite the specified file ?");
            if (!overwrite) {
                return false;
            }
        } else {
            File parentFile = exportFile.getParentFile();
            if ((parentFile != null) && parentFile.exists()) {
                // check if we can write to
                if (!parentFile.canWrite() || !parentFile.isDirectory()) {
                    // display error
                    m_page.setErrorMessage("Cannot write to specified file");
                    return false;
                }
            } else if (parentFile != null && !parentFile.exists()) {
                if (!exportFile.getParentFile().mkdirs()) {
                    boolean wasRoot = false;
                    for (File root : File.listRoots()) {
                        if (exportFile.getParentFile().equals(root)) {
                            wasRoot = true;
                            break;
                        }
                    }
                    if (!wasRoot) {
                        m_page.setErrorMessage("Failed to create: "
                                + exportFile.getAbsolutePath()
                                + ". \n Please check if it is a "
                                + "valid file name.");
                        return false;
                    }
                }
            }
        }

        IRunnableWithProgress op = new IRunnableWithProgress() {
            @Override
            public void run(final IProgressMonitor monitor)
                    throws InvocationTargetException {
                try {
                    doFinish(exportFile, monitor);
                } catch (CoreException e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            }
        };
        try {
            getContainer().run(true, true, op);
        } catch (InterruptedException e) {
            LOGGER.info("Export of workflows canceled by user.");
            m_page.setErrorMessage("Export of workflows was canceled.");
            return false;
        } catch (InvocationTargetException e) {
            Throwable realException = e.getTargetException();
            String message = realException.getMessage();
            message = "Export of workflows did not succeed: "
                + realException.getMessage();
            LOGGER.debug(message);
            MessageDialog.openError(getShell(), "Error", message);
            m_page.setErrorMessage(message);
            return false;
        }
        return true;
    }

    /**
     * Implements the exclude policy. Called only if "exclude data" is checked.
     *
     * @param store the resource to check
     * @return true if the given resource should be excluded, false if it should
     *         be included
     */
    protected static boolean excludeResource(final AbstractExplorerFileStore store) {
        String name = store.getName();
        if (name.equals("internal")) {
            return true;
        }
        if (store.fetchInfo().isDirectory()) {
            // directories to exclude:
            if (name.startsWith(NodePersistorVersion200.PORT_FOLDER_PREFIX)) {
                return true;
            }
            if (name.startsWith(NodePersistorVersion200.INTERNAL_TABLE_FOLDER_PREFIX)) {
                return true;
            }
            if (name.startsWith(NodePersistorVersion200.FILESTORE_FOLDER_PREFIX)) {
                return true;
            }
            if (name.startsWith(NodePersistor.INTERN_FILE_DIR)) {
                return true;
            }
            if (name.startsWith(SingleNodeContainer.DROP_DIR_NAME)) {
                return true;
            }
        } else {
            // files to exclude:
            if (name.startsWith("model_")) {
                return true;
            }
            if (name.equals("data.xml")) {
                return true;
            }
            if (name.startsWith(WorkflowPersistor.SAVED_WITH_DATA_FILE)) {
                return true;
            }
            if (name.startsWith(VMFileLocker.LOCK_FILE)) {
                return true;
            }
        }
        // always exclude zip files
        return name.toLowerCase().endsWith(".zip");
    }

    /**
     * The worker method. It will find the container, create the export file if
     * missing or just replace its contents.
     */
    private void doFinish(final File fileName,
            final IProgressMonitor monitor) throws CoreException {
        // start zipping
        monitor.beginTask("Archiving selected workflows... ", 10);
        // if the data should be excluded from the export
        // iterate over the resources and add only the wanted stuff
        // i.e. the "intern" folder and "*.zip" files are excluded
        final List<File> resourceList = new ArrayList<File>();
        for (AbstractExplorerFileStore wf : m_workflowsToExport) {
            // add all files within the workflow or group
            addResourcesFor(resourceList, wf, m_excludeData);
        }
        monitor.worked(1); // 10% for collecting the files...
        try {
            SubProgressMonitor sub = new SubProgressMonitor(monitor, 9);

            File parentLoc = m_commonParent.toLocalFile();
            if (parentLoc == null) {
                throw new CoreException(new Status(Status.ERROR,
                        ExplorerActivator.PLUGIN_ID,
                        "Only local files can be exported ("
                                + m_commonParent.getFullName()
                                + " has no local file)", null));
            }

            int stripOff = new Path(parentLoc.getAbsolutePath()).segmentCount();
            if (!m_commonParent.getFullName().equals("/")) {
                // keep the common workflow group (if exists) in the archive
                stripOff = stripOff - 1;
            }
            Zipper.zipFiles(resourceList, fileName, stripOff, sub);

        } catch (final IOException t) {
            LOGGER.debug(
                    "Export of KNIME workflow(s) failed: " + t.getMessage(), t);
            throw new CoreException(new Status(Status.ERROR,
                    ExplorerActivator.PLUGIN_ID,
                    t.getMessage(), t));
        }
        monitor.done();

    }

    /**
     * Collects the files (files only) that are contained in the passed workflow
     * or workflow group and are that are not excluded. For groups it does not
     * recurse in sub directories (that is in contained flows), for workflows it
     * does include all files contained in sub dirs (unless excluded).
     *
     * @param resourceList result list of resources to export
     * @param flowOrGroup the resource representing the workflow directory
     * @param excludeData true if KNIME data files should be excluded
     */
    public static void addResourcesFor(final List<File> resourceList,
            final AbstractExplorerFileStore flowOrGroup, final boolean excludeData)
            throws CoreException {
        if (resourceList == null) {
            throw new NullPointerException("Result list can't be null");
        }
        if (AbstractExplorerFileStore.isWorkflow(flowOrGroup)
                || AbstractExplorerFileStore.isWorkflowTemplate(flowOrGroup)) {
            addResourcesRec(resourceList, flowOrGroup, excludeData);
        } else if (AbstractExplorerFileStore.isWorkflowGroup(flowOrGroup)) {
            addFiles(resourceList, flowOrGroup);
        } else {
            throw new IllegalArgumentException(
                    "Only resources of flows or groups can be added");
        }

    }

    /**
     * Adds files of the passed workflow group to the resourcelist.
     *
     * @param resourceList
     * @param group
     * @throws CoreException
     */
    private static void addFiles(final List<File> resourceList,
            final AbstractExplorerFileStore group) throws CoreException {
        AbstractExplorerFileStore[] childStores = group.childStores(EFS.NONE,
                null);
        for (AbstractExplorerFileStore child : childStores) {
            if (!child.fetchInfo().isDirectory()) {
                File loc = child.toLocalFile();
                if (loc == null) {
                    throw new CoreException(new Status(Status.ERROR,
                            ExplorerActivator.PLUGIN_ID,
                            "Only local files can be exported ("
                                    + child.getFullName()
                                    + " has no local file)", null));
                }
                resourceList.add(loc);
            }
        }
    }

    private static void addResourcesRec(final List<File> resourceList,
            final AbstractExplorerFileStore store, final boolean excludeData)
            throws CoreException {

        if (!AbstractExplorerFileStore.isWorkflow(store)
                && !AbstractExplorerFileStore.isMetaNode(store)
                && !AbstractExplorerFileStore.isWorkflowTemplate(store)) {
            // make sure flows and templates named like excluded resource (e.g.
            // "internal" or "drop" are not accidently excluded!
            if ( excludeData && excludeResource(store)) {
                return;
            }
        }

        // if this is a file add it to the list
        if (!store.fetchInfo().isDirectory()) {
            File loc = store.toLocalFile();
            if (loc == null) {
                throw new CoreException(getErrorStatus("Only local files can "
                        + "be exported (" + store.getFullName()
                        + " has no local file)", null));
            }

            resourceList.add(loc);
            return;
        }

        // add all (not-excluded) sub dirs of workflows
        for (AbstractExplorerFileStore child
                : store.childStores(EFS.NONE, null)) {
            addResourcesRec(resourceList, child, excludeData);
        }
    }

    /**
     * We will initialize file contents with a sample text.
     *
     * @return an empty content stream
     */
    InputStream openContentStream() {
        String contents = "";
        return new ByteArrayInputStream(contents.getBytes());
    }

    /**
     * We will accept the selection in the workbench to see if we can initialize
     * from it.
     *
     * {@inheritDoc}
     */
    @Override
    public void init(final IWorkbench workbench,
            final IStructuredSelection selection) {
        this.m_selection = selection;
    }

    private static Status getErrorStatus(final String msg, final Throwable e) {
        return new Status(Status.ERROR, ExplorerActivator.PLUGIN_ID, msg, e);
    }
}
