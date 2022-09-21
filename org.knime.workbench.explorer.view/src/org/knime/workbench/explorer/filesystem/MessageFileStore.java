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
 * ------------------------------------------------------------------------
 */
package org.knime.workbench.explorer.filesystem;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Optional;

import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.graphics.Image;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.contextv2.LocationInfo;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;

/**
 * Placeholder store in the tree for string messages.
 *
 * @author ohl, University of Konstanz
 */
public final class MessageFileStore extends AbstractExplorerFileStore {
    private static final String NOT_SUPPORTED = "Not supported in message file store.";
    private final String m_msg;
    private final Image m_image;
    private AbstractExplorerFileStore m_parent = null;

    /**
     * Creates a new message file store with the specified mount id, message, and parent.
     * @param mountID the id of the mount point
     * @param message the message to display
     * @param parent the parent file store
     *
     * @see AbstractExplorerFileStore#AbstractExplorerFileStore(String, String)
     * @since 8.6
     */
    public MessageFileStore(final String mountID, final String message, final AbstractExplorerFileStore parent) {
        this(mountID, message);
        m_parent = parent;
    }

    /**
     * Creates a new message file store with the specified mount id and message.
     * @param mountID the id of the mount point
     * @param message the message to display
     *
     * @see AbstractExplorerFileStore#AbstractExplorerFileStore(String, String)
     */
    public MessageFileStore(final String mountID, final String message) {
        this(mountID, message, ImageRepository.getIconImage(SharedImages.InfoButton));
    }

    /**
     * Creates a new message file store with the specified mount id, message and icon.
     * @param mountID the id of the mount point
     * @param message the message to display
     * @param image the image for the icon to display
     */
    public MessageFileStore(final String mountID, final String message, final Image image) {
        super(mountID, "");
        m_msg = CheckUtils.checkNotNull(message, "Message to display must no be null");
        m_image = CheckUtils.checkNotNull(image, "Image to display must not be null");
    }

    @Override
    public Optional<LocationInfo> locationInfo() {
        return Optional.empty();
    }

    @Override
    public String getName() {
        return getFullName();
    }

    @Override
    public String getFullName() {
        return m_msg;
    }

    @Override
    public URI toURI() {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

    @Override
    public boolean equals(final Object obj) {
        return obj == this;
    }

    @Override
    public AbstractExplorerFileStore getMessageFileStore(final String msg) {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

    @Override
    public IFileInfo[] childInfos(final int options, final IProgressMonitor monitor) throws CoreException {
        return EMPTY_FILE_INFO_ARRAY;
    }

    @Override
    public String[] childNames(final int options, final IProgressMonitor monitor) throws CoreException {
        return EMPTY_STRING_ARRAY;
    }

    @Override
    public AbstractExplorerFileStore[] childStores(final int options, final IProgressMonitor monitor)
            throws CoreException {
        return new AbstractExplorerFileStore[0];
    }

    @Override
    public void copy(final IFileStore destination, final int options, final IProgressMonitor monitor)
            throws CoreException {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

    @Override
    protected void copyDirectory(final IFileInfo sourceInfo, final IFileStore destination, final int options,
            final IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

    @Override
    protected void copyFile(final IFileInfo sourceInfo, final IFileStore destination, final int options,
            final IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

    @Override
    public void delete(final int options, final IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

    @Override
    public MessageFileInfo fetchInfo() {
        return MessageFileInfo.getInstance();
    }

    @SuppressWarnings("deprecation")
    @Override
    public IFileStore getChild(final IPath path) {
        return null;
    }

    @Override
    public IFileStore getFileStore(final IPath path) {
        return null;
    }

    @Override
    public AbstractExplorerFileStore getChild(final String name) {
        return null;
    }

    @Override
    public AbstractExplorerFileStore getParent() {
        return m_parent;
    }

    @Override
    public boolean isParentOf(final IFileStore other) {
        return false;
    }

    @Override
    public AbstractExplorerFileStore mkdir(final int options, final IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

    @Override
    public void move(final IFileStore destination, final int options, final IProgressMonitor monitor)
            throws CoreException {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

    @Override
    public InputStream openInputStream(final int options, final IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

    @Override
    public OutputStream openOutputStream(final int options, final IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

    @Override
    public void putInfo(final IFileInfo info, final int options, final IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException(NOT_SUPPORTED);
    }

    @Override
    public File toLocalFile(final int options, final IProgressMonitor monitor) throws CoreException {
        return null;
    }

    @Override
    public void refresh() {
        // no-op
    }

    @Override
    public String toString() {
        return m_msg;
    }

    @Override
    public int hashCode() {
        return m_msg.hashCode();
    }

    /**
     * Returns the icon for the message.
     *
     * @return an image
     */
    public Image getImage() {
        return m_image;
    }

    @Override
    public File toLocalFile() throws CoreException {
        return null;
    }

    @Override
    public File resolveToLocalFile() throws CoreException {
        return null;
    }

    @Override
    public IFileStore getNativeFilestore() {
        return this;
    }
}
