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
 * Created: Apr 14, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer.localworkspace;

import java.io.File;
import java.io.InputStream;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.filesystem.local.LocalFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.knime.workbench.explorer.filesystem.ExplorerFileStore;

/**
 * Wraps the Eclipse LocalFile. Provides a file interface to the workspace.
 * Returns all files (doesn't stop at nodes and doesn't hide workflow files or
 * meta files, etc.).
 *
 * @author ohl, University of Konstanz
 */
public class LocalWorkspaceFileStore extends ExplorerFileStore {

    private final IFileStore m_file;

    /**
     * @param mountID
     * @param fullPath
     */
    public LocalWorkspaceFileStore(final String mountID, final String fullPath) {
        super(mountID, fullPath);
        IPath rootPath = ResourcesPlugin.getWorkspace().getRoot().getLocation();
        IPath filePath = rootPath.append(new Path(fullPath));
        m_file = EFS.getLocalFileSystem().getStore(filePath);
    }

    /**
     * Call this only with a local file!
     *
     * @param mountID
     * @param file the underlying {@link LocalFile}
     */
    private LocalWorkspaceFileStore(final String mountID, final IFileStore file) {
        super(mountID, file.getName());
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
    public IFileInfo fetchInfo(final int options, final IProgressMonitor monitor)
            throws CoreException {
        return m_file.fetchInfo(options, monitor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalWorkspaceFileStore getChild(final String name) {
        return new LocalWorkspaceFileStore(getMountID(), m_file.getChild(name));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalWorkspaceFileStore getParent() {
        return new LocalWorkspaceFileStore(getMountID(), m_file.getParent());
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

}
