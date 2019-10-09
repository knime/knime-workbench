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
 * Created: Apr 14, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer.localworkspace;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Objects;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.knime.core.util.PathUtils;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.osgi.framework.FrameworkUtil;

/**
 * Wraps the Eclipse LocalFile. Provides a file interface to the workspace.
 * Returns all files (doesn't stop at nodes and doesn't hide workflow files or
 * meta files, etc.).
 *
 * @author ohl, University of Konstanz
 */
public class LocalWorkspaceFileStore extends LocalExplorerFileStore {
    private final IFileStore m_file;

    /**
     * @param mountID the id of the mount
     * @param fullPath the full path of the file store
     */
    public LocalWorkspaceFileStore(final String mountID, final String fullPath) {
        super(mountID, fullPath);
        IPath rootPath = ResourcesPlugin.getWorkspace().getRoot().getLocation();
        IPath filePath = rootPath.append(new Path(getFullName()));
        m_file = EFS.getLocalFileSystem().getStore(filePath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof LocalWorkspaceFileStore)) {
            // Fix for Bug AP-5451 (switch from report -> workflow)
            // If it is the same file but a different class it is still equal.
            // this "equals" isn't symmetric but that's OK as equals is called
            // on the right object
            if (obj instanceof FileStore) {
                try {
                    File thisfile = this.toLocalFile();
                    File thatfile = ((FileStore)obj).toLocalFile(0, null);
                    return Objects.equals(thisfile, thatfile);
                } catch (CoreException e) {
                }
            }
            return false;
        }
        LocalExplorerFileStore other = (LocalExplorerFileStore)obj;
        try {
            return this.toLocalFile().equals(other.toLocalFile());
        } catch (CoreException ex) {
            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void copy(final IFileStore destination, final int options,
            final IProgressMonitor monitor) throws CoreException {
        super.copy(destination, options, monitor);
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
        String[] children = m_file.childNames(options, monitor);
        // remove .metadata, .project and workflowset.meta from the list of shown children
        ArrayList<String> filteredChildren = new ArrayList<String>(children.length);
        for (String c : children) {
            if (!AbstractContentProvider.isHiddenFile(c)) {
                filteredChildren.add(c);
            }
        }
        return filteredChildren.toArray(new String[filteredChildren.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalWorkspaceFileInfo fetchInfo() {
        return new LocalWorkspaceFileInfo(m_file);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalExplorerFileStore getChild(final String name) {
        return new LocalWorkspaceFileStore(getMountID(), m_file.getChild(name),
                new Path(getFullName()).append(name).toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalExplorerFileStore getParent() {
        if ("/".equals(getFullName())) { // root file store
            return null;
        }
        IFileStore parent = m_file.getParent();
        if (parent == null) {
            return null;
        }
        return new LocalWorkspaceFileStore(getMountID(), parent, new Path(
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
    public void refresh() {
        refresh(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refresh(final IProgressMonitor monitor) {
        refreshResource(this);
    }

    private static void refreshResource(final LocalExplorerFileStore fileStore) {
        fileStore.getContentProvider().refresh(fileStore);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final int options, final IProgressMonitor monitor)
            throws CoreException {
        java.nio.file.Path srcFile = toLocalFile(options, monitor).toPath();
        try {
            if (Files.isDirectory(srcFile)) {
                PathUtils.deleteDirectoryIfExists(srcFile);
            } else if (Files.isRegularFile(srcFile)) {
                Files.delete(srcFile);
            }
        } catch (IOException e) {
            String message = "Could not delete \"" + srcFile.toAbsolutePath() + "\".";
            throw new CoreException(new Status(IStatus.ERROR, ExplorerActivator.PLUGIN_ID, message, e));
        }
        refreshResource(getParent());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractExplorerFileStore mkdir(final int options,
            final IProgressMonitor monitor) throws CoreException {
        m_file.mkdir(options, monitor);
        refreshResource(getParent());
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

        if (srcFile.equals(dstFile)) {
            throw new CoreException(new Status(IStatus.ERROR, FrameworkUtil.getBundle(getClass()).getSymbolicName(),
                "Unable to move file. \"" + srcFile.getAbsolutePath() + "\" and \"" + dstFile.getAbsolutePath()
                    + "\" are the same file."));
        }
        super.cleanupDestination(destination, options, monitor);

        try {
            if (srcFile.renameTo(dstFile)) {
                // if rename works: refresh
                final LocalExplorerFileStore srcParent = getParent();
                IFileStore destParent = destination.getParent();
                if (!srcParent.equals(destParent)
                        && destParent instanceof AbstractExplorerFileStore) {
                    ((AbstractExplorerFileStore)destParent).refresh();
                }
                refreshResource(srcParent);
            } else {
                copy(destination, options, monitor);
                delete(options, monitor);
                // copy/delete refreshes
            }
        } catch (SecurityException e) {
            String message =
                    "Could not rename file \"" + srcFile.getAbsolutePath()
                            + "\" to \"" + dstFile.getAbsolutePath()
                            + "\" due to missing access rights.";
            throw new CoreException(new Status(IStatus.ERROR,
                    ExplorerActivator.PLUGIN_ID, message, e));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IFileStore getNativeFilestore() {
        return m_file;
    }

    /**
     * Creates a new core exception.
     *
     * @param message Message of the {@link IStatus}.
     * @param cause The cause of the exception.
     */
    private CoreException newCoreException(final String message, final Throwable cause) throws CoreException {
        return new CoreException(
            new Status(IStatus.ERROR, FrameworkUtil.getBundle(getClass()).getSymbolicName(), message, cause));
    }
}
