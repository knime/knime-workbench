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
 * History
 *   6.6.2011: Peter Ohl, KNIME AG, Zurich, Switzerland
 */
package org.knime.workbench.explorer.view.actions.imports;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.internal.wizards.datatransfer.ArchiveFileManipulations;
import org.eclipse.ui.internal.wizards.datatransfer.ILeveledImportStructureProvider;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;

/**
 * Imports workflows from an archive (Zip, tar.gz) file or directory into the workspace.
 */
public class WorkflowImportOperation extends WorkspaceModifyOperation {
    private static final int BUFFSIZE = 1024 * 2048;

    private final Collection<IWorkflowImportElement> m_workflows;

    /** Collection containing all unchecked workflows and workflows group. */
    private final Collection<IWorkflowImportElement> m_uncheckedWorkflows = new HashSet<>();

    private final AbstractExplorerFileStore m_targetPath;

    private final Shell m_shell;

    private final Set<String> m_importedFiles = new HashSet<>();

    /**
     * Imports the elements specified in the passed collection.
     *
     * @param workflows the import elements (file or archive entries) to import
     * @param targetPath the destination path within the workspace
     * @param shell the shell
     * @param unchecked the unchecked workflows, i.e. the ones that shouldn't be imported
     * @since 8.6
     */
    public WorkflowImportOperation(final Collection<IWorkflowImportElement> workflows,
        final AbstractExplorerFileStore targetPath, final Shell shell,
        final Collection<IWorkflowImportElement> unchecked) {
        m_workflows = workflows;
        m_targetPath = targetPath;
        m_shell = shell;
        m_uncheckedWorkflows.addAll(unchecked);
    }

    /**
     * Imports the root element and its entire subtree.
     *
     * @param rootElement
     * @param targetPath
     * @since 7.1
     *
     */
    public WorkflowImportOperation(final IWorkflowImportElement rootElement,
        final AbstractExplorerFileStore targetPath) {
        m_workflows = Collections.singleton(rootElement);
        m_targetPath = targetPath;
        m_shell = null;
    }

    /**
     *
     * {@inheritDoc}
     */
    @SuppressWarnings("restriction")
    @Override
    protected void execute(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        ILeveledImportStructureProvider provider = null;
        try {
            monitor.beginTask("", m_workflows.size());
            for (IWorkflowImportElement wf : m_workflows) {
                if (monitor.isCanceled()) {
                    throw new OperationCanceledException();
                }
                provider = handleCopyProject(wf, monitor);
            }

        } catch (Exception e) {
            throw new InvocationTargetException(e);
        } finally {
            if (provider != null) {
                ArchiveFileManipulations.closeStructureProvider(provider, m_shell);
            }
            monitor.done();
        }
    }

    @SuppressWarnings("restriction")
    private ILeveledImportStructureProvider handleCopyProject(final IWorkflowImportElement importElement,
        final IProgressMonitor monitor) throws IOException, CoreException {

        // determine the destination from the renamed element
        IPath renamedElementPath = importElement.getRenamedPath();
        AbstractExplorerFileStore destination;
        if (renamedElementPath.segmentCount() > 0) {
            destination = m_targetPath.getChild(renamedElementPath.toString());
        } else {
            throw new IllegalStateException("Cannot import workflow to empty destination path");
        }

        ILeveledImportStructureProvider provider = null;

        if (importElement instanceof WorkflowImportElementFromFile) {
            WorkflowImportElementFromFile importFile = (WorkflowImportElementFromFile)importElement;

            // copy workflow from file location
            importWorkflowFromFile(importFile, destination, new SubProgressMonitor(monitor, 1));

        } else if (importElement instanceof WorkflowImportElementFromArchive) {
            WorkflowImportElementFromArchive archive = (WorkflowImportElementFromArchive)importElement;
            provider = archive.getProvider();

            // create workflow from archive
            importWorkflowFromArchive(archive, destination, new SubProgressMonitor(monitor, 1));

        } else {
            monitor.worked(1);
        }
        return provider;
    }

    /**
     * Copies the referenced file from the fileElement to the destination.
     *
     * @param fileElement source
     * @param destination target
     * @param monitor monitor
     * @throws IOException if things go bad
     * @throws CoreException if deleting the existing files fails
     */
    protected void importWorkflowFromFile(final WorkflowImportElementFromFile fileElement,
        final AbstractExplorerFileStore destination, final IProgressMonitor monitor) throws IOException, CoreException {
        if (!m_importedFiles.contains(fileElement.getRenamedPath().toString())) {
            // in the wizard page we make sure the destination doesn't exist
            assert !destination.fetchInfo().exists();

            if (destination.getContentProvider().isRemote()) {
                try {
                    AbstractExplorerFileStore tmpDestDir =
                        ExplorerMountTable.createExplorerTempDir(fileElement.getRenamedPath().toString());
                    tmpDestDir = tmpDestDir.getChild(fileElement.getRenamedPath().toString());
                    SubMonitor progress = SubMonitor.convert(monitor, 1);
                    tmpDestDir.mkdir(EFS.NONE, progress);

                    importFromFile(fileElement, tmpDestDir, progress);

                    destination.getContentProvider().performUploadAsync((LocalExplorerFileStore)tmpDestDir,
                        (RemoteExplorerFileStore)destination, true,
                        destination.getContentProvider().isForceResetOnUpload(), null);
                } catch (CoreException e) {
                    throw new IOException(e);
                }

            } else {
                importFromFile(fileElement, destination, monitor);
                // we operated on the file system directly. Refresh file stores.
                destination.refresh();
            }
        }
    }

    /**
     * Extracts the import elements (recursively) from the corresponding provider and stores them in the destination.
     *
     * @param archiveElement the element to extract (and its children)
     * @param destination target
     * @param monitor monitor
     * @throws IOException if things go bad or user cancels
     * @throws CoreException if there is no tmpSpace but user tries to import to server
     */
    protected void importWorkflowFromArchive(final WorkflowImportElementFromArchive archiveElement,
        final AbstractExplorerFileStore destination, final IProgressMonitor monitor) throws IOException, CoreException {
        if (!m_importedFiles.contains(archiveElement.getOriginalPath().toString())) {
            // check if destination (1) exists and (2) can be deleted, then delete its children
            deleteIfExists(destination, monitor);

            AbstractExplorerFileStore tmpDestDir;
            if (destination instanceof RemoteExplorerFileStore) {
                tmpDestDir = ExplorerMountTable.createExplorerTempDir(archiveElement.getRenamedPath().toString());
                tmpDestDir = tmpDestDir.getChild(archiveElement.getRenamedPath().toString());
                SubMonitor progress = SubMonitor.convert(monitor, 1);
                tmpDestDir.mkdir(EFS.NONE, progress);
            } else {
                tmpDestDir = destination;
            }
            importArchiveEntry(archiveElement.getProvider(), archiveElement.getEntry(), tmpDestDir, monitor);
            if (destination instanceof RemoteExplorerFileStore) {
                destination.getContentProvider().performUploadAsync((LocalExplorerFileStore)tmpDestDir,
                    (RemoteExplorerFileStore)destination, true, destination.getContentProvider().isForceResetOnUpload(),
                    null);
            }
        }
    }

    /**
     * Import the entire subtree.
     */
    private void importArchiveEntry(final ILeveledImportStructureProvider importProvider, final Object entry,
        final AbstractExplorerFileStore destination, final IProgressMonitor monitor) throws IOException, CoreException {
        if (importProvider.isFolder(entry)) {
            // first create the destination
            try {
                destination.mkdir(EFS.SHALLOW, monitor);
            } catch (CoreException e) {
                throw new IOException(e.getMessage(), e);
            }

            // import all sub elements
            for (Object child : importProvider.getChildren(entry)) {
                String path = importProvider.getFullPath(child);

                /* Check if it is an unchecked item (AP-13299). */
                final boolean isUnchecked = m_uncheckedWorkflows.stream()
                    .anyMatch(e -> ((WorkflowImportElementFromArchive)e).getEntry().equals(child));

                if (!isUnchecked) {
                    AbstractExplorerFileStore childDest = destination.getChild(new Path(path).lastSegment());
                    importArchiveEntry(importProvider, child, childDest, monitor);
                }
            }
        } else {
            try (InputStream inStream = importProvider.getContents(entry);
                    OutputStream outStream = destination.openOutputStream(EFS.NONE, monitor)) {
                byte[] buffer = new byte[BUFFSIZE];
                int read;
                while ((read = inStream.read(buffer)) >= 0) {
                    if (monitor.isCanceled()) {
                        throw new IOException("Canceled.");
                    }
                    outStream.write(buffer, 0, read);
                }
            } catch (CoreException ex) {
                throw new IOException(ex);
            }
        }

        final String path = importProvider.getFullPath(entry);
        m_importedFiles.add(path.endsWith("/") ? path.substring(0, path.length() - 1) : path);
    }

    /**
     * Copies the content of the provided file element to the destination.
     *
     * @param fileElement The file element to be imported.
     * @param destination The destination that is used to copy files to.
     * @param monitor The progress monitor.
     * @throws IOException If an error occurs.
     */
    private void importFromFile(final WorkflowImportElementFromFile fileElement,
        final AbstractExplorerFileStore destination, final IProgressMonitor monitor) throws IOException {
        File dest = null;
        try {
            dest = destination.toLocalFile();
        } catch (CoreException e) {
            throw new IOException(e.getMessage(), e);
        }

        if (fileElement.isFile()) {
            FileUtil.copy(fileElement.getFile(), dest);
            addImportedFiles(fileElement, false);
        } else if (fileElement.isWorkflow()) {
            final File dir = fileElement.getFile();
            FileUtil.copyDir(dir, dest);
            addImportedFiles(fileElement, true);
        } else {
            if (!dest.exists()) {
                if (!dest.mkdir()) {
                    throw new IOException("Cannot create target directory \"" + dest.getAbsolutePath() + "\"");
                }
                addImportedFiles(fileElement, false);
            }

            for (final IWorkflowImportElement child : fileElement.getChildren()) {
                if (!m_uncheckedWorkflows.contains(child)) {
                    importFromFile((WorkflowImportElementFromFile)child, destination.getChild(child.getName()),
                        monitor);
                }
            }
        }
    }

    /**
     * Adds the provided file element and all its descendants to the set containing the imported files.
     *
     * @param fileElement The file element to add.
     * @param recursive if the descendants shall be added.
     */
    private void addImportedFiles(final IWorkflowImportElement fileElement, final boolean recursive) {
        final String path = fileElement.getRenamedPath().toString();
        m_importedFiles.add(path);

        if (recursive) {
            for (IWorkflowImportElement child : fileElement.getChildren()) {
                if (!m_uncheckedWorkflows.contains(child)) {
                    addImportedFiles(child, recursive);
                }
            }
        }
    }

    /**
     * Deletes the contents (i.e. children) of a given target destination if it the target already exists.
     * Only call this method, if you know that the destination should be overwritten by the import.
     *
     * @param destination the target file store (top-level element)
     * @param monitor the progress monitor
     * @throws IOException if deletion went wrong
     */
    private static void deleteIfExists(final AbstractExplorerFileStore destination, final IProgressMonitor monitor)
        throws CoreException {
        final var progress = SubMonitor.convert(monitor, 2);
        final var info = destination.fetchInfo(EFS.NONE, progress);
        if (!info.exists()) {
            progress.worked(1);
            return;
        }
        progress.worked(1);

        if (monitor.isCanceled()) {
            throw new OperationCanceledException();
        }
        if (destination.canDelete()) {
            destination.delete(EFS.NONE, progress);
            NodeLogger.getLogger(WorkflowImportAction.class).debug( //
                "Deleted existing destination \"%s\" with an overwriting import".formatted(destination));
        } else if (info.isDirectory()) {
            NodeLogger.getLogger(WorkflowImportAction.class).warn( //
                "Cannot delete \"%s\" for import, will only overwrite certain files within".formatted(destination));
        }
        progress.worked(1);
    }
}
