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
 * Created: Apr 14, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer.localworkspace;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.ui.metainfo.model.MetaInfoFile;
import org.knime.workbench.ui.nature.KNIMEProjectNature;
import org.knime.workbench.ui.navigator.KnimeResourceUtil;

/**
 * Wraps the Eclipse LocalFile. Provides a file interface to the workspace.
 * Returns all files (doesn't stop at nodes and doesn't hide workflow files or
 * meta files, etc.).
 *
 * @author ohl, University of Konstanz
 */
public class LocalWorkspaceFileStore extends LocalExplorerFileStore {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(LocalWorkspaceFileStore.class);

    private final IFileStore m_file;

    /**
     * @param mountID the id of the mount
     * @param fullPath the full path of the file store
     */
    public LocalWorkspaceFileStore(final String mountID, final String fullPath) {
        super(mountID, fullPath);
        IPath rootPath = ResourcesPlugin.getWorkspace().getRoot().getLocation();
        IPath filePath = rootPath.append(new Path(fullPath));
        m_file = EFS.getLocalFileSystem().getStore(filePath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof LocalWorkspaceFileStore)) {
            return false;
        }
        LocalWorkspaceFileStore other = (LocalWorkspaceFileStore)obj;
        return getFullName().equalsIgnoreCase(other.getFullName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getFullName().toLowerCase().hashCode();
    }

    /**
     * Call this only with a local file!
     *
     * @param mountID
     * @param file the underlying {@link IFileStore}
     * @param fullPath the path relative to the root!
     */
    private LocalWorkspaceFileStore(final String mountID,
            final IFileStore file, final String fullPath) {
        super(mountID, fullPath);
        m_file = file;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] childNames(final int options, final IProgressMonitor monitor)
            throws CoreException {
        return m_file.childNames(options, monitor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalWorkspaceFileInfo fetchInfo(final int options,
            final IProgressMonitor monitor) throws CoreException {
        LocalWorkspaceFileInfo info = new LocalWorkspaceFileInfo(m_file);
        IFileInfo fileInfo = m_file.fetchInfo();
        if (fileInfo.exists()) {
            info.setExists(true);
            info.setDirectory(fileInfo.isDirectory());
            info.setLastModified(fileInfo.getLastModified());
            info.setLength(fileInfo.getLength());
            info.setAttribute(EFS.ATTRIBUTE_READ_ONLY,
                    fileInfo.getAttribute(EFS.ATTRIBUTE_READ_ONLY));
            info.setAttribute(EFS.ATTRIBUTE_HIDDEN,
                    fileInfo.getAttribute(EFS.ATTRIBUTE_HIDDEN));
        } else {
            info.setExists(false);
        }
        return info;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalWorkspaceFileStore getChild(final String name) {
        return new LocalWorkspaceFileStore(getMountID(), m_file.getChild(name),
                new Path(getFullName()).append(name).toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalWorkspaceFileStore getParent() {
        IFileStore p = m_file.getParent();
        if (p == null) {
            return null;
        }
        return new LocalWorkspaceFileStore(getMountID(), p, new Path(
                getFullName()).removeLastSegments(1).toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openInputStream(final int options,
            final IProgressMonitor monitor) throws CoreException {
        return m_file.openInputStream(options, monitor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File toLocalFile(final int options, final IProgressMonitor monitor)
            throws CoreException {
        return m_file.toLocalFile(options, monitor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copy(final IFileStore destination, final int options,
            final IProgressMonitor monitor) throws CoreException {
        File srcFile = toLocalFile(options, monitor);
        File dstFile = destination.toLocalFile(options, monitor);
        if (!dstFile.isDirectory()) {
            throw new UnsupportedOperationException("The local workspace "
                    + "filestore only allows copying to directories but the "
                    + "destination \"" + dstFile.getAbsolutePath()
                    + "\" is not" + " a directory.");
        }
        File targetDir = new File(dstFile, srcFile.getName());
        if (targetDir.exists()) {
            throw new CoreException(new Status(IStatus.ERROR,
                    ExplorerActivator.PLUGIN_ID, "A resource with the name "
                            + srcFile.getName() + " already exists in "
                            + dstFile.getName()));
        }
        try {
            if (srcFile.isDirectory()) {
                FileUtils.copyDirectory(srcFile, targetDir);
            } else if (srcFile.isFile()) {
                FileUtils.copyFileToDirectory(srcFile, dstFile);
            }
        } catch (IOException e) {
            String message =
                    "Could not copy \"" + srcFile.getAbsolutePath()
                            + "\" to \"" + dstFile.getAbsolutePath() + "\".";
            throw new CoreException(new Status(IStatus.ERROR,
                    ExplorerActivator.PLUGIN_ID, message, e));
        }
        createProjectFile(destination, monitor);
        refreshResource(destination, monitor);
    }

    private void createProjectFile(final IFileStore dest,
            final IProgressMonitor monitor) throws CoreException {
        createProjectFile(getName(), dest, monitor);
    }

    /**
     * Creates a .project for the destination if necessary (only in case that
     * destination is the workflow root and no .project file exists yet).
     *
     * @param projectName the name of the project
     * @param destination the parent file store of the project
     * @param monitor a progress monitor, or null if progress reporting and
     *            cancellation are not desired
     *
     * @throws CoreException
     */
    public static void createProjectFile(final String projectName,
            final IFileStore destination, final IProgressMonitor monitor)
            throws CoreException {
        File dstDir = destination.toLocalFile(EFS.NONE, monitor);
        if (dstDir == null) {
            return; // do nothing
        }
        IResource res = KnimeResourceUtil.getResourceForURI(dstDir.toURI());
        if (res != null && res instanceof IWorkspaceRoot) {
            /*
             * The target is the workspace root. Therefore we have to create a
             * .project file.
             */
            IProject newProject = ((IWorkspaceRoot)res).getProject(projectName);
            newProject.delete(false, true, monitor);
            try {
                newProject =
                        MetaInfoFile.createKnimeProject(newProject.getName(),
                                KNIMEProjectNature.ID);
            } catch (Exception e) {
                String message =
                        "Could not create KNIME project in "
                                + "workspace root.";
                throw new CoreException(new Status(IStatus.ERROR,
                        ExplorerActivator.PLUGIN_ID, message, e));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refresh() {
        try {
            refreshResource(this, null);
        } catch (CoreException e) {
            // too bad
        }
    }

    /**
     * Refreshes the parent resource if it exists.
     */
    public void refreshParentResource() {
        try {
            refreshResource(getParent(), null);
        } catch (CoreException e) {
            // do nothing if it cannot be refreshed
        }
    }

    private void refreshResource(final IResource resource,
            final IProgressMonitor monitor) {
        if (resource != null) {
            try {
                resource.refreshLocal(IResource.DEPTH_INFINITE, monitor);
                LOGGER.debug("Refreshed resource " + resource);
            } catch (Exception e) {
                // do not refresh
                LOGGER.debug("Could not refresh resource " + resource, e);
            }
        }
    }

    private void refreshResource(final IFileStore fileStore,
            final IProgressMonitor monitor) throws CoreException {
        if (fileStore == null) {
            return;
        }
        File file = fileStore.toLocalFile(EFS.NONE, null);
        if (file != null) {
            refreshResource(KnimeResourceUtil.getResourceForURI(file.toURI()),
                    monitor);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final int options, final IProgressMonitor monitor)
            throws CoreException {
        File srcFile = toLocalFile(options, monitor);
        try {
            if (srcFile.isDirectory()) {
                FileUtils.deleteDirectory(srcFile);
            } else if (srcFile.isFile()) {
                srcFile.delete();
            }
        } catch (IOException e) {
            String message =
                    "Could not delete \"" + srcFile.getAbsolutePath() + "\".";
            throw new CoreException(new Status(IStatus.ERROR,
                    ExplorerActivator.PLUGIN_ID, message, e));
        }
        refreshResource(getParent(), monitor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractExplorerFileStore mkdir(final int options,
            final IProgressMonitor monitor) throws CoreException {
        m_file.mkdir(options, monitor);
        createProjectFile(getParent(), monitor);
        refreshResource(getParent(), monitor);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputStream openOutputStream(final int options,
            final IProgressMonitor monitor) throws CoreException {
        return m_file.openOutputStream(options, monitor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void move(final IFileStore destination, final int options,
            final IProgressMonitor monitor) throws CoreException {
        File srcFile = toLocalFile(options, monitor);
        File dstFile = destination.toLocalFile(options, monitor);

        try {
            srcFile.renameTo(dstFile);
        } catch (SecurityException e) {
            String message =
                    "Could not rename file \"" + srcFile.getAbsolutePath()
                            + "\" to \"" + dstFile.getAbsolutePath()
                            + "\" due to missing access rights.";
            throw new CoreException(new Status(IStatus.ERROR,
                    ExplorerActivator.PLUGIN_ID, message, e));
        }
        createProjectFile(destination.getName(), destination.getParent(),
                monitor);
        refreshResource(destination.getParent(), monitor);
        refreshParentResource();
    }

}
