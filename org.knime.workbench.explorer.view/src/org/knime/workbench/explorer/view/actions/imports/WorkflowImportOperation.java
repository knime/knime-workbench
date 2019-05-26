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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.util.FileUtil;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;
import org.knime.workbench.ui.workflow.metadata.MetaInfoFile;

/**
 * Imports workflows from an archive (Zip, tar.gz) file or directory into the workspace.
 */
public class WorkflowImportOperation extends WorkspaceModifyOperation {
    private static final int BUFFSIZE = 1024 * 2048;

    private final Collection<IWorkflowImportElement> m_workflows;

    private final AbstractExplorerFileStore m_targetPath;

    private final boolean m_recursive;

    private final Shell m_shell;

    // stores those directories which not yet contain a metainfo file and
    // hence are not displayed - meta info file has to be created after the
    // import -> occurs when importing archive files containing directories
    private final List<AbstractExplorerFileStore> m_missingMetaInfoLocations =
        new ArrayList<AbstractExplorerFileStore>();

    /**
     * Imports the elements specified in the passed collection.
     * @param workflows the import elements (file or archive entries) to import
     * @param targetPath the destination path within the workspace
     * @param shell the shell
     */
    public WorkflowImportOperation(final Collection<IWorkflowImportElement> workflows,
        final AbstractExplorerFileStore targetPath, final Shell shell) {
        m_workflows = workflows;
        m_targetPath = targetPath;
        m_shell = shell;
        m_recursive = false;
    }

    /**
     * Imports the root element and its entire subtree.
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
        m_recursive = true;
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
            if (!m_missingMetaInfoLocations.isEmpty()) {
                createMetaInfo();
            }
            // clean up afterwards
            m_missingMetaInfoLocations.clear();

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
        final IProgressMonitor monitor) throws Exception {

        // determine the destination from the renamed element
        IPath renamedElementPath = importElement.getRenamedPath();
        AbstractExplorerFileStore destination = m_targetPath;
        if (renamedElementPath.segmentCount() > 0) {
            destination = m_targetPath.getChild(renamedElementPath.toString());
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
     * @throws IOException if things go bad.
     */
    protected void importWorkflowFromFile(final WorkflowImportElementFromFile fileElement,
        final AbstractExplorerFileStore destination, final IProgressMonitor monitor) throws IOException {

        // in the wizard page we make sure the destination doesn't exist
        assert !destination.fetchInfo().exists();

        File dest = null;
        try {
            dest = destination.toLocalFile();
        } catch (CoreException e) {
            throw new IOException(e.getMessage(), e);
        }

        if (fileElement.isWorkflow() || fileElement.isTemplate()) {
            File dir = fileElement.getFile();
            FileUtil.copyDir(dir, dest);
        } else if (fileElement.isFile()) {
            FileUtil.copy(fileElement.getFile(), dest);
        } else {
            // copy the meta info file from a group - if it exists
            try {
                destination.mkdir(EFS.SHALLOW, monitor);
            } catch (CoreException e) {
                throw new IOException(e.getMessage(), e);
            }
            if (m_recursive) {
                File dir = fileElement.getFile();
                FileUtil.copyDir(dir, dest);
            } else {
                File metaFile = new File(fileElement.getFile(), WorkflowPersistor.METAINFO_FILE);
                if (metaFile.exists() && (metaFile.length() > 0)) {
                    FileUtil.copy(metaFile, new File(dest, WorkflowPersistor.METAINFO_FILE));
                }
            }
            m_missingMetaInfoLocations.add(destination);
        }
        // we operated on the file system directly. Refresh file stores.
        destination.refresh();
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

        if (archiveElement.isWorkflow() || archiveElement.isTemplate() || archiveElement.isFile()) {
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
                    (RemoteExplorerFileStore)destination, true, false, null);
            }
        } else if (archiveElement.isWorkflowGroup()) {
            if (m_recursive) {
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
                        (RemoteExplorerFileStore)destination, true, false, null);
                }
            } else {
                try {
                    destination.mkdir(EFS.SHALLOW, monitor);
                } catch (CoreException e) {
                    throw new IOException(e.getMessage(), e);
                }
                // copy the metainfo file in a group - if it exists
                if (!importMetaInfoFile(archiveElement, destination, monitor)) {
                    m_missingMetaInfoLocations.add(destination);
                }
            }
        }
    }

    /*
     * Copies the meta info file from the passed group into the destination directory. Returns true if the meta info
     * file exists, false if it doesn't.
     */
    private boolean importMetaInfoFile(final WorkflowImportElementFromArchive group,
        final AbstractExplorerFileStore destinationDir, final IProgressMonitor monitor) throws IOException {
        assert group.isWorkflowGroup();
        assert destinationDir.fetchInfo().isDirectory();
        for (IWorkflowImportElement ch : group.getChildren()) {
            if ((ch instanceof WorkflowImportElementFromArchive) && ch.isFile()
                    && ch.getName().equals(WorkflowPersistor.METAINFO_FILE)) {
                WorkflowImportElementFromArchive child = (WorkflowImportElementFromArchive)ch;
                importArchiveEntry(child.getProvider(), child.getEntry(),
                    destinationDir.getChild(WorkflowPersistor.METAINFO_FILE), monitor);
                return true;
            }
        }
        return false;
    }

    /**
     * Import the entire subtree.
     */
    private void importArchiveEntry(final ILeveledImportStructureProvider importProvider, final Object entry,
        final AbstractExplorerFileStore destination, final IProgressMonitor monitor)
        throws IOException {

        //assert !destination.fetchInfo().exists();

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
                AbstractExplorerFileStore childDest = destination.getChild(new Path(path).lastSegment());
                importArchiveEntry(importProvider, child, childDest, monitor);
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

    }

    private void createMetaInfo() throws Exception {
        for (AbstractExplorerFileStore f : m_missingMetaInfoLocations) {
            assert f.fetchInfo().exists();
            File parent = f.toLocalFile();
            if (parent != null) {
                File metaInfoFile = new File(parent, WorkflowPersistor.METAINFO_FILE);
                if (!metaInfoFile.exists() || (metaInfoFile.length() == 0)) {
                    // don't overwrite (use io.File for the test, AEFS hides the meta info file!)
                    MetaInfoFile.createOrGetMetaInfoFileForDirectory(parent, false);
                }
            }
        }
    }

}
