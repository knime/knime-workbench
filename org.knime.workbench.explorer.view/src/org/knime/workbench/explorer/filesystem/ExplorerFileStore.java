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
 * Created: Apr 12, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer.filesystem;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.knime.core.node.workflow.SingleNodeContainerPersistorVersion200;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.MountPoint;
import org.knime.workbench.explorer.view.AbstractContentProvider;

/**
 *
 * @author ohl, University of Konstanz
 */
public abstract class ExplorerFileStore extends FileStore {

    private final String m_mountID;

    private final String m_fullPath;

    public ExplorerFileStore(final String mountID, final String fullPath) {
        m_fullPath = fullPath;
        m_mountID = mountID;
        if (m_fullPath == null) {
            throw new NullPointerException("Path in can't be null (mountID = "
                    + m_mountID + ")");
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return new Path(m_fullPath).lastSegment();
    }

    public String getFullName() {
        return m_fullPath;
    }

    /** @return a human readable name including mount ID and path. */
    public String getMountIDWithFullPath() {
        return getMountID() + ":" + getFullName();
    }

    public String getMountID() {
        return m_mountID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract ExplorerFileStore mkdir(int options,
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
    public ExplorerFileSystem getFileSystem() {
        return ExplorerMountTable.getFileSystem();
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
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof ExplorerFileStore) {
            ExplorerFileStore other = (ExplorerFileStore)obj;
            if (!other.m_mountID.equalsIgnoreCase(m_mountID)) {
                return false;
            }
            Path otherPath = new Path(other.m_fullPath);
            Path thisPath = new Path(m_fullPath);
            if (otherPath.segmentCount() != thisPath.segmentCount()) {
                return false;
            }
            for (int s = otherPath.segmentCount() - 1; s >= 0; s--) {
                if (!otherPath.segment(s).equalsIgnoreCase(thisPath.segment(s))) {
                    return false;
                }
            }
            return true;
        }
        return false;
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
    public static boolean isWorkflowGroup(final ExplorerFileStore file) {
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
    public static boolean isMetaNode(final ExplorerFileStore file) {
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
    public static boolean isNode(final ExplorerFileStore file) {
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
    public static boolean isDirOrWorkflowGroup(final ExplorerFileStore file) {
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
    public static boolean isDirOnly(final ExplorerFileStore file) {
         return isDirOrWorkflowGroup(file) && !isWorkflowGroup(file);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExplorerFileStore[] childStores(final int options,
            final IProgressMonitor monitor) throws CoreException {
        IFileStore[] childStores = super.childStores(options, monitor);
        ExplorerFileStore[] efs = new ExplorerFileStore[childStores.length];
        for (int i = 0; i < efs.length; i++) {
            efs[i] = (ExplorerFileStore)childStores[i];
        }
        return efs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract ExplorerFileStore getParent();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract ExplorerFileStore getChild(String name);

    /**
     * Convenience method that calls #toLocalFile(int, IProgressMonitor) with
     * options = EFS.NONE and monitor = null.
     *
     * @return the local file or null if not supported
     * @throws CoreException if this method fails
     */
    public File toLocalFile() throws CoreException {
        return toLocalFile(EFS.NONE, null);
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
    public ExplorerFileStore getMessageFileStore(final String msg) {
        return new MessageFileStore(getMountID(), msg);
    }

    /**
     * Creates a file store that carries a message.
     *
     * @param mountID id of the mount point to which this is added
     * @param msg the message used as name and by toString
     * @return a file store carrying the message
     */
    public static ExplorerFileStore getMessageFileStore(final String mountID,
            final String msg) {
        return new MessageFileStore(mountID, msg);

    }

    /**
     * Placeholder store in the tree for string messages.
     *
     * @author ohl, University of Konstanz
     */
    private static final class MessageFileStore extends ExplorerFileStore {

        private final String m_msg;

        /**
         * @param mountID
         * @param fullPath
         */
        public MessageFileStore(final String mountID, final String message) {
            super(mountID, "");
            if (message == null) {
                throw new NullPointerException(
                        "Message to display can't be null");
            }
            m_msg = message;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getName() {
            return m_msg;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getFullName() {
            return m_msg;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public URI toURI() {
            throw new UnsupportedOperationException(
                    "Not supported in message file store.");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            return obj == this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ExplorerFileStore getMessageFileStore(final String msg) {
            throw new UnsupportedOperationException(
                    "Not supported in message file store.");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public IFileInfo[] childInfos(final int options,
                final IProgressMonitor monitor) throws CoreException {
            return EMPTY_FILE_INFO_ARRAY;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String[] childNames(final int options,
                final IProgressMonitor monitor) throws CoreException {
            return EMPTY_STRING_ARRAY;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ExplorerFileStore[] childStores(final int options,
                final IProgressMonitor monitor) throws CoreException {
            return new ExplorerFileStore[0];
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void copy(final IFileStore destination, final int options,
                final IProgressMonitor monitor) throws CoreException {
            throw new UnsupportedOperationException(
                    "Not supported in message file store.");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void copyDirectory(final IFileInfo sourceInfo,
                final IFileStore destination, final int options,
                final IProgressMonitor monitor) throws CoreException {
            throw new UnsupportedOperationException(
                    "Not supported in message file store.");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void copyFile(final IFileInfo sourceInfo,
                final IFileStore destination, final int options,
                final IProgressMonitor monitor) throws CoreException {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException(
                    "Not supported in message file store.");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void delete(final int options, final IProgressMonitor monitor)
                throws CoreException {
            throw new UnsupportedOperationException(
                    "Not supported in message file store.");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public IFileInfo fetchInfo() {
            // re-use Eclipse local file info
            FileInfo info = new FileInfo();
            info.setExists(false);
            info.setDirectory(false);
            info.setLastModified(0);
            info.setLength(0);
            info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, true);
            info.setAttribute(EFS.ATTRIBUTE_HIDDEN, false);
            info.setName(m_msg);
            return info;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public IFileInfo fetchInfo(final int options,
                final IProgressMonitor monitor) throws CoreException {
            return fetchInfo();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public IFileStore getChild(final IPath path) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public IFileStore getFileStore(final IPath path) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ExplorerFileStore getChild(final String name) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ExplorerFileStore getParent() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isParentOf(final IFileStore other) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ExplorerFileStore mkdir(final int options,
                final IProgressMonitor monitor) throws CoreException {
            throw new UnsupportedOperationException(
                    "Not supported in message file store.");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void move(final IFileStore destination, final int options,
                final IProgressMonitor monitor) throws CoreException {
            throw new UnsupportedOperationException(
                    "Not supported in message file store.");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public InputStream openInputStream(final int options,
                final IProgressMonitor monitor) throws CoreException {
            throw new UnsupportedOperationException(
                    "Not supported in message file store.");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public OutputStream openOutputStream(final int options,
                final IProgressMonitor monitor) throws CoreException {
            throw new UnsupportedOperationException(
                    "Not supported in message file store.");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void putInfo(final IFileInfo info, final int options,
                final IProgressMonitor monitor) throws CoreException {
            throw new UnsupportedOperationException(
                    "Not supported in message file store.");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public File toLocalFile(final int options,
                final IProgressMonitor monitor) throws CoreException {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return m_msg;
        }
    }
}
