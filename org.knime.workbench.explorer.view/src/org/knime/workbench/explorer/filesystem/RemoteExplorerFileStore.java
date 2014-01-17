/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by 
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
 *   Aug 25, 2011 (morent): created
 */

package org.knime.workbench.explorer.filesystem;

import java.io.File;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public abstract class RemoteExplorerFileStore extends AbstractExplorerFileStore {

    /**
     * @param mountID the id of the mount point
     * @param fullPath the full path
     */
    protected RemoteExplorerFileStore(final String mountID,
            final String fullPath) {
        super(mountID, fullPath);
    }

    /**
     * File stores representing files on the same remote host should return
     * equal IDs. To enable smart "remote copy" (i.e. copying files directly on
     * the host avoiding download with a subsequent upload to the same host)
     *
     * @return an ID unique to the host the underlying file is located
     */
    public abstract String getRemoteHostID();

    /**
     * {@inheritDoc}
     * <p>
     * Note: Implementations should do a smart "remote copy" if the destination
     * is on the same remote host than this (see {@link #getRemoteHostID()}).
     */
    @Override
    public abstract void copy(IFileStore destination, int options,
            IProgressMonitor monitor) throws CoreException;

    /**
     * {@inheritDoc}
     */
    @Override
    public File toLocalFile() throws CoreException {
        return null;
    }

    /**
     * If this store represents a workflow return an open stream that sends the
     * zipped flow. If it is a file, the unzipped content is streamed.
     *
     * @return a stream containing the zipped workflow or unzipped file content.
     * @throws CoreException if this doesn't exist
     * @since 4.0
     */
    public abstract RemoteDownloadStream openDownloadStream()
            throws CoreException;

    /**
     * A zipped workflow sent through the stream is stored on the server as
     * workflow represented by this.
     *
     * @return an open stream, that stores the content as a workflow on the
     *         server.
     * @throws CoreException
     * @since 4.0
     */
    public abstract RemoteUploadStream openWorkflowUploadStream()
            throws CoreException;

    /**
     * Stores the content send through the stream as file on the server (doesn't unzip it).
     * @return an open stream that stores the content (unzipped) in a file on the server.
     * @throws CoreException
     * @since 4.0
     */
    public abstract RemoteUploadStream openFileUploadStream()
            throws CoreException;

    /**
     * {@inheritDoc}
     */
    @Override
    public RemoteExplorerFileInfo fetchInfo(final int options,
            final IProgressMonitor monitor) throws CoreException {
        return fetchInfo();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract RemoteExplorerFileInfo fetchInfo();

    /**
     * Creates a new snapshot for this file store. The operation may fail if the user does not have sufficient
     * permissions or if the filesystem does not support snapshots.
     *
     * @param comment an optional comment for the snapshot, may be <code>null</code>
     * @return the name of the created snapshot
     * @throws CoreException if the snapshot could not be created
     * @since 6.0
     */
    public abstract String createSnapshot(String comment) throws CoreException;


    /**
     * Replaces this item (a workflow, template, or file) with one of its snapshots.
     *
     * @param snapshotName the name of the snapshot (usually the name of the file store representing the snapshot)
     * @throws CoreException if an error occurs
     * @since 6.0
     */
    public abstract void replaceWithSnapshot(String snapshotName) throws CoreException;
}
