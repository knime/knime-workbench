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
import java.net.URI;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.filesystem.EFS;
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
import org.knime.core.node.workflow.WorkflowPersistor;
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
        IPath filePath = rootPath.append(new Path(getFullName()));
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
        File srcFile = toLocalFile(options, monitor);
        if (srcFile.isDirectory()) {
            createProjectFile(destination, monitor);
        }
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
        if (getFullName().equals("/")) {
            // TODO: We MUST rewrite this if we change to IResources!!!
            // remove .metadata and workflowset.meta from the list of shown
            // childs
            ArrayList<String> rootChilds =
                    new ArrayList<String>(children.length);
            for (String c : children) {
                if (c.equals(".metadata")
                        || c.equals(WorkflowPersistor.METAINFO_FILE)) {
                    continue;
                }
                rootChilds.add(c);
            }
            return rootChilds.toArray(new String[rootChilds.size()]);
        }
        return children;
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
     * Creates a .project for the destination if necessary (only in case that
     * destination is the workflow root and no .project file exists yet).
     *
     * @param destination the file store of the project
     * @param monitor a progress monitor, or null if progress reporting and
     *            cancellation are not desired
     *
     * @throws CoreException
     */
    public static void createProjectFile(final IFileStore destination,
            final IProgressMonitor monitor) throws CoreException {
        File dstDir = destination.toLocalFile(EFS.NONE, monitor);
        if (dstDir == null) {
            return; // do nothing
        }
        File root = dstDir.getParentFile();
        IResource res = null;
        if (root != null) {
            res = KnimeResourceUtil.getResourceForURI(root.toURI());
        }
        if (res != null && res instanceof IWorkspaceRoot) {
            /*
             * The target is the workspace root. Therefore we have to create a
             * .project file.
             */
            IProject newProject =
                    ((IWorkspaceRoot)res).getProject(dstDir.getName());
            newProject.delete(false, true, monitor);
            try {
                newProject =
                        MetaInfoFile.createKnimeProject(dstDir.getName(),
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

    private static void refreshResource(final IResource resource,
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

    private static void refreshResource(
            final LocalExplorerFileStore fileStore,
            final IProgressMonitor monitor) throws CoreException {
        if (fileStore == null) {
            return;
        }
        File file = fileStore.toLocalFile(EFS.NONE, null);
        if (file != null) {
            refreshResource(KnimeResourceUtil.getResourceForURI(file.toURI()),
                    monitor);
        }
        fileStore.getContentProvider().refresh(fileStore);
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
        IResource res = KnimeResourceUtil.getResourceForURI(srcFile.toURI());
        if (res != null) {
            res.delete(IResource.FORCE
                    | IResource.ALWAYS_DELETE_PROJECT_CONTENT
                    | IResource.DEPTH_INFINITE, monitor);
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
        createProjectFile(this, monitor);
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
        if (srcFile.equals(dstFile)) {
            LOGGER.debug("Cannot move file store on itself. "
                    + "Ignoring operation...");
            return;
        }
        super.cleanupDestination(destination, options, monitor);
        try {
            URI srcURI = srcFile.toURI();
            if (srcFile.renameTo(dstFile)) {
                // if rename works: refresh
                createProjectFile(destination, monitor);
                final LocalExplorerFileStore srcParent = getParent();
                IResource res = KnimeResourceUtil.getResourceForURI(srcURI);
                if (res != null) {
                    res.delete(IResource.FORCE
                            | IResource.ALWAYS_DELETE_PROJECT_CONTENT
                            | IResource.DEPTH_INFINITE, monitor);
                }
                refreshResource(srcParent, monitor);
                IFileStore destParent = destination.getParent();
                if (!srcParent.equals(destParent)
                        && destParent instanceof AbstractExplorerFileStore) {
                    ((AbstractExplorerFileStore)destParent).refresh();
                }
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

}
