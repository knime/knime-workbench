package org.knime.workbench.explorer.filesystem;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.graphics.Image;
import org.knime.workbench.explorer.view.IconFactory;

/**
 * Placeholder store in the tree for string messages.
 *
 * @author ohl, University of Konstanz
 */
public final class MessageFileStore extends AbstractExplorerFileStore {
    private final String m_msg;
    private final Image m_image;

    /**
     * Creates a new message file store with the specified mount id and
     * message.
     * @param mountID the id of the mount point
     * @param message the message to display
     *
     * @see AbstractExplorerFileStore#AbstractExplorerFileStore(String, String)
     */
    public MessageFileStore(final String mountID, final String message) {
        this(mountID, message, IconFactory.instance.info());
    }

    /**
     * Creates a new message file store with the specified mount id, message
     * and icon.
     * @param mountID the id of the mount point
     * @param message the message to display
     * @param image the image for the icon to display
     */
    public MessageFileStore(final String mountID, final String message,
            final Image image) {
        super(mountID, "");
        if (message == null) {
            throw new NullPointerException(
                    "Message to display must no be null");
        }
        m_msg = message;

        if (image == null) {
            throw new NullPointerException("Image to display must not be null");
        }
        m_image = image;
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
    public AbstractExplorerFileStore getMessageFileStore(final String msg) {
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
    public AbstractExplorerFileStore[] childStores(final int options,
            final IProgressMonitor monitor) throws CoreException {
        return new AbstractExplorerFileStore[0];
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
    public AbstractExplorerFileStore getChild(final String name) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractExplorerFileStore getParent() {
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
    public AbstractExplorerFileStore mkdir(final int options,
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
    public File toLocalFile(final int options, final IProgressMonitor monitor)
            throws CoreException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refresh() {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_msg;
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public File toLocalFile() throws CoreException {
        return null;
    }
}