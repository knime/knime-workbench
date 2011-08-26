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
 *   Aug 24, 2011 (morent): created
 */

package org.knime.workbench.explorer.filesystem;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.knime.core.node.workflow.SingleNodeContainerPersistorVersion200;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.MountPoint;
import org.knime.workbench.explorer.view.AbstractContentProvider;

/**
 * Abstract base class for all explorer file stores.
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public abstract class AbstractExplorerFileStore extends FileStore {
    private final String m_mountID;
    private final String m_fullPath;

    /**
     * Creates a new local explorer file store with the specified mount id and
     * full path.
     * @param mountID the id of the mount point
     * @param fullPath the full path
     */
    public AbstractExplorerFileStore(final String mountID,
            final String fullPath) {
        m_fullPath = fullPath;
        m_mountID = mountID;
        if (fullPath == null) {
            throw new NullPointerException("Path can't be null (mountID = "
                    + getMountID() + ")");
        }
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public abstract String[] childNames(final int options,
            final IProgressMonitor monitor) throws CoreException;

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract IFileInfo fetchInfo(final int options,
            final IProgressMonitor monitor) throws CoreException;

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract AbstractExplorerFileStore getChild(final String name);

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract AbstractExplorerFileStore getParent();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract InputStream openInputStream(final int options,
            final IProgressMonitor monitor) throws CoreException;

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract AbstractExplorerFileStore mkdir(int options,
            IProgressMonitor monitor) throws CoreException;

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract OutputStream openOutputStream(int options,
            IProgressMonitor monitor) throws CoreException;

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void copy(IFileStore destination, int options,
            IProgressMonitor monitor) throws CoreException;

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void delete(int options, IProgressMonitor monitor)
            throws CoreException;

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract boolean equals(Object obj);

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract int hashCode();

    /**
     * Called when changes are made to the underlying file system.
     * Implementations can update their caches or internal members.
     */
    public abstract void refresh();


    /**
     * Convenience method that calls #toLocalFile(int, IProgressMonitor) with
     * options = EFS.CACHE and monitor = null.
     *
     * @return the local file or null if not supported
     * @throws CoreException if this method fails
     */
    public abstract File toLocalFile() throws CoreException;

    /**
     * {@inheritDoc}
     */
    @Override
    public ExplorerFileSystem getFileSystem() {
        return ExplorerMountTable.getFileSystem();
    }

    /**
     * @return the mountID
     */
    public String getMountID() {
        return m_mountID;
    }

    /**
     * @return the fullName
     */
    public String getFullName() {
        return m_fullPath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return new Path(m_fullPath).lastSegment();
    }

    /** @return a human readable name including mount ID and path. */
    public String getMountIDWithFullPath() {
        return getMountID() + ":" + getFullName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI toURI() {
        try {
            return new URI(ExplorerFileSystem.SCHEME, m_mountID, m_fullPath,
                    null);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Checks whether a file represents a workflow.
     *
     * @param file the file to check if it represents a workflow
     * @return true if the file is a workflow, false otherwise
     */
    public static boolean isWorkflow(final IFileStore file) {
        if (file == null || !file.fetchInfo().exists()) {
            return false;
        }
        IFileStore wf = file.getChild(WorkflowPersistor.WORKFLOW_FILE);
        return wf.fetchInfo().exists() && !isWorkflow(file.getParent());
    }

    /**
     * Checks whether a file represents a workflow template.
     *
     * @param file the file to check if it represents a workflow
     * @return true if the file is a workflow template, false otherwise
     */
    public static boolean isWorkflowTemplate(final IFileStore file) {
        if (file == null || !file.fetchInfo().exists()) {
            return false;
        }
        IFileStore tf = file.getChild(WorkflowPersistor.TEMPLATE_FILE);
        return tf.fetchInfo().exists();

    }

    /**
     * Checks whether a file represents a workflow group.
     *
     * @param file the file to check if it represents a workflow group
     * @return true if the file is a workflow group, false otherwise
     */
    public static boolean isWorkflowGroup(final AbstractExplorerFileStore file) {
        if (file == null || !file.fetchInfo().exists()) {
            return false;
        }

        if (isWorkflow(file) || isWorkflow(file.getParent())
                || isWorkflowTemplate(file)) {
            return false;
        }
        return file.getChild(WorkflowPersistor.METAINFO_FILE).fetchInfo()
                .exists();
    }

    /**
     * Checks whether a file represents a meta node.
     *
     * @param file the file to check if it represents a meta node
     * @return true if the file is a meta node, false otherwise
     */
    public static boolean isMetaNode(final AbstractExplorerFileStore file) {
        if (file == null || !file.fetchInfo().exists()) {
            return false;
        }
        IFileStore parent = file.getParent();
        if (parent == null) {
            return false;
        }
        IFileStore wf = file.getChild(WorkflowPersistor.WORKFLOW_FILE);
        IFileStore parentWf = parent.getChild(WorkflowPersistor.WORKFLOW_FILE);
        return wf.fetchInfo().exists() && parentWf.fetchInfo().exists();
    }

    /**
     * Checks whether a file represents a node.
     *
     * @param file the file to check if it represents a node
     * @return true if the file is a node, false otherwise
     */
    public static boolean isNode(final AbstractExplorerFileStore file) {
        if (file == null || !file.fetchInfo().exists() || isMetaNode(file)) {
            return false;
        }
        return file.getChild(SingleNodeContainerPersistorVersion200.NODE_FILE)
                .fetchInfo().exists() && isWorkflow(file.getParent());
    }

    /**
     * @param file the file to test.
     * @return true if the argument is a directory but no node nor meta node.
     */
    public static boolean isDirOrWorkflowGroup(
            final AbstractExplorerFileStore file) {
        if (file == null || !file.fetchInfo().isDirectory()) {
            return false;
        }
        if (isWorkflow(file) || isMetaNode(file) || isNode(file)
                || isWorkflowTemplate(file)) {
            return false;
        }
        return true;
    }

    /**
     * @param file the file to test.
     * @return true if the argument is only a plain directory.
     */
    public static boolean isDirOnly(final AbstractExplorerFileStore file) {
         return isDirOrWorkflowGroup(file) && !isWorkflowGroup(file);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractExplorerFileStore[] childStores(final int options,
            final IProgressMonitor monitor) throws CoreException {
        IFileStore[] childStores = super.childStores(options, monitor);
        AbstractExplorerFileStore[] efs
                = new AbstractExplorerFileStore[childStores.length];
        for (int i = 0; i < efs.length; i++) {
            efs[i] = (AbstractExplorerFileStore)childStores[i];
        }
        return efs;
    }

    /**
     * @return the content provider responsible for the file store, or null
     *      if the content provider is no longer mounted
     */
    public AbstractContentProvider getContentProvider() {
        MountPoint mountPoint = ExplorerMountTable.getMountPoint(getMountID());
        if (mountPoint == null) {
            return null;
        }
        return mountPoint.getProvider();
    }

    /* ----------- placeholder store in the tree for string messages ----- */

    /**
     * Creates a file store that carries a message.
     *
     * @param msg the message used as name and by toString
     * @return a file store carrying the message
     */
    public AbstractExplorerFileStore getMessageFileStore(final String msg) {
        return new MessageFileStore(getMountID(), msg);
    }
}
