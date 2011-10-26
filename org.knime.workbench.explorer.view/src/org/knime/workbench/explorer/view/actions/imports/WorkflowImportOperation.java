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
 * History
 *   6.6.2011: Peter Ohl, KNIME.com, Zurich, Switzerland
 */
package org.knime.workbench.explorer.view.actions.imports;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.internal.wizards.datatransfer.ArchiveFileManipulations;
import org.eclipse.ui.internal.wizards.datatransfer.ILeveledImportStructureProvider;
import org.knime.core.util.FileUtil;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.ui.metainfo.model.MetaInfoFile;

/**
 * Imports workflows from a zip file or directory into the workspace.
 */
public class WorkflowImportOperation extends WorkspaceModifyOperation {

    private static final int BUFFSIZE = 1024 * 2048;

    private final Collection<IWorkflowImportElement> m_workflows;

    private final AbstractExplorerFileStore m_targetPath;

    private final Shell m_shell;

    // stores those directories which not yet contain a metainfo file and
    // hence are not displayed - meta info file has to be created after the
    // import -> occurs when importing zip files containing directories
    private final List<AbstractExplorerFileStore> m_missingMetaInfoLocations =
            new ArrayList<AbstractExplorerFileStore>();

    /**
     *
     * @param workflows the import elements (file or zip entries) to import
     * @param targetPath the destination path within the workspace
     * @param shell the shell
     */
    public WorkflowImportOperation(
            final Collection<IWorkflowImportElement> workflows,
            final AbstractExplorerFileStore targetPath, final Shell shell) {
        m_workflows = workflows;
        m_targetPath = targetPath;
        m_shell = shell;
    }

    /**
     *
     * {@inheritDoc}
     */
    @SuppressWarnings("restriction")
    @Override
    protected void execute(final IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException {
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
                ArchiveFileManipulations.closeStructureProvider(provider,
                        m_shell);
            }
            monitor.done();
        }
    }

    @SuppressWarnings("restriction")
    private ILeveledImportStructureProvider handleCopyProject(
            final IWorkflowImportElement importElement,
            final IProgressMonitor monitor) throws Exception {

        // determine the destination from the renamed element
        IPath renamedElementPath = importElement.getRenamedPath();
        AbstractExplorerFileStore destination = m_targetPath;
        if (renamedElementPath.segmentCount() > 0) {
            destination = m_targetPath.getChild(renamedElementPath.toString());
        }

        ILeveledImportStructureProvider provider = null;

        if (importElement instanceof WorkflowImportElementFromFile) {
            WorkflowImportElementFromFile importFile =
                    (WorkflowImportElementFromFile)importElement;

            // copy workflow from file location
            importWorkflowFromFile(importFile, destination,
                    new SubProgressMonitor(monitor, 1));

        } else if (importElement instanceof WorkflowImportElementFromArchive) {
            WorkflowImportElementFromArchive zip =
                    (WorkflowImportElementFromArchive)importElement;
            provider = zip.getProvider();

            // create workflow from ZIP archive
            importWorkflowFromArchive(zip, destination, new SubProgressMonitor(
                    monitor, 1));

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
    protected void importWorkflowFromFile(
            final WorkflowImportElementFromFile fileElement,
            final AbstractExplorerFileStore destination,
            final IProgressMonitor monitor) throws IOException {

        File destinationDir = null;

        // in the wizard page we make sure the destination doesn't exist
        assert !destination.fetchInfo().exists();

        // first create the destination
        try {
            destination.mkdir(EFS.SHALLOW, monitor);
            destinationDir = destination.toLocalFile();
        } catch (CoreException e) {
            throw new IOException(e.getMessage(), e);
        }

        if (fileElement.isWorkflow()) {
            File dir = fileElement.getFile();
            FileUtil.copyDir(dir, destinationDir);
        } else {

            m_missingMetaInfoLocations.add(destination);
            // import all sub elements
            for (IWorkflowImportElement wieff : fileElement.getChildren()) {
                AbstractExplorerFileStore dest
                        = destination.getChild(wieff.getName());
                importWorkflowFromFile((WorkflowImportElementFromFile)wieff,
                        dest, monitor);
            }
        }

        // we operated on the file system directly. Refresh file stores.
        destination.refresh();

    }

    /**
     * Extracts the import elements (recursively) from the corresponding
     * provider and stores them in the destination.
     *
     * @param zipElement the element to extract (and its children)
     * @param destination target
     * @param monitor monitor
     * @throws IOException if things go bad or user cancels
     */
    protected void importWorkflowFromArchive(
            final WorkflowImportElementFromArchive zipElement,
            final AbstractExplorerFileStore destination, final IProgressMonitor monitor)
            throws IOException {

        if (zipElement.isWorkflow()) {
            importZipEntry(zipElement.getProvider(),
                    (ZipEntry)zipElement.getEntry(), destination, monitor);
        } else {

            // first create the destination
            try {
                destination.mkdir(EFS.SHALLOW, monitor);
            } catch (CoreException e) {
                throw new IOException(e.getMessage(), e);
            }
            m_missingMetaInfoLocations.add(destination);
            // import all sub elements
            for (IWorkflowImportElement wie : zipElement.getChildren()) {
                AbstractExplorerFileStore dest = destination.getChild(wie.getName());
                importWorkflowFromArchive(
                        (WorkflowImportElementFromArchive)wie, dest, monitor);
            }
        }

    }

    /**
     * Import the entire subtree.
     *
     * @param zipProvider
     * @param entry
     * @param destination
     * @param monitor
     * @throws IOException if it blows.
     */
    protected void importZipEntry(
            final ILeveledImportStructureProvider zipProvider,
            final ZipEntry entry, final AbstractExplorerFileStore destination,
            final IProgressMonitor monitor) throws IOException {

        //assert !destination.fetchInfo().exists();

        if (zipProvider.isFolder(entry)) {
            // first create the destination
            try {
                destination.mkdir(EFS.SHALLOW, monitor);
            } catch (CoreException e) {
                throw new IOException(e.getMessage(), e);
            }

            // import all sub elements
            for (Object c : zipProvider.getChildren(entry)) {
                ZipEntry child = (ZipEntry)c;
                AbstractExplorerFileStore childDest =
                        destination.getChild(
                                new Path(child.getName()).lastSegment());
                importZipEntry(zipProvider, child, childDest, monitor);
            }
        } else {
            // suck the file content in
            BufferedOutputStream outStream = null;
            try {
                outStream =
                        new BufferedOutputStream(
                                destination.openOutputStream(EFS.NONE,
                                        monitor));
            } catch (CoreException e) {
                throw new IOException(e.getMessage(), e);
            }
            BufferedInputStream inStream = null;
            try {
                inStream =
                        new BufferedInputStream(zipProvider.getContents(entry));
                byte[] buffer = new byte[BUFFSIZE];
                int read;
                while ((read = inStream.read(buffer)) >= 0) {
                    if (monitor.isCanceled()) {
                        throw new IOException("Canceled.");
                    }
                    outStream.write(buffer, 0, read);
                }
            } finally {
                if (inStream != null) {
                    inStream.close();
                }
                outStream.close();
            }
        }

    }

    private void createMetaInfo() throws Exception {
        for (AbstractExplorerFileStore f : m_missingMetaInfoLocations) {
            assert f.fetchInfo().exists();
            File parent = f.toLocalFile();
            if (parent != null) {
                MetaInfoFile.createMetaInfoFile(parent, false);
            }
        }
    }

}
