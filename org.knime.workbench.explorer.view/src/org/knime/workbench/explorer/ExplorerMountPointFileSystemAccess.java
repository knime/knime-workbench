/*
 * ------------------------------------------------------------------------
 *
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
 * History
 *   Oct 9, 2019 (gabriel): created
 */
package org.knime.workbench.explorer;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountPoint;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountTable;
import org.knime.filehandling.core.connections.base.attributes.BaseFileAttributes;
import org.knime.filehandling.core.util.MountPointFileSystemAccess;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileInfo;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.filesystem.TmpLocalExplorerFile;

/**
 * Implementation Of {@link MountPointFileSystemAccess} backed by {@link ExplorerMountTable}.
 *
 * @author Gabriel Einsdorf
 * @since 8.6
 */
public class ExplorerMountPointFileSystemAccess implements MountPointFileSystemAccess {

    private static final Set<String> SERVER_PROVIDER_FACTORY_IDS = Set.of("com.knime.explorer.server");

    private static final Set<String> HUB_PROVIDER_FACTORY_IDS = Set.of(
        "com.knime.explorer.server.knime_hub", "com.knime.explorer.server.workflow_hub");

    private static String getProviderFactoryId(final String mountId) {
        return ExplorerMountTable.getContentProvider(mountId) //
                .map(a -> a.getMountPoint().getType().getTypeIdentifier()) //
                .orElse(null);
    }

    @Override
    public boolean isHubMountPoint(final String mountId) {
        return HUB_PROVIDER_FACTORY_IDS.contains(getProviderFactoryId(mountId));
    }

    @Override
    public boolean isServerMountPoint(final String mountId) {
        return SERVER_PROVIDER_FACTORY_IDS.contains(getProviderFactoryId(mountId));
    }

    @Override
    public List<String> getMountedIDs() {
        return WorkbenchMountTable.getAllMountedIDs().stream() //
                .map(WorkbenchMountTable::getMountPoint) //
                .flatMap(Optional::stream) //
                .filter(mp -> ExplorerMountTable.toAbstractContentProvider(mp).isPresent()) //
                .map(WorkbenchMountPoint::getMountID) //
                .toList();
    }

    @Override
    public URL resolveKNIMEURL(final URL url) throws IOException {
        return ExplorerURLStreamHandler.resolveKNIMEURL(url);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if mountpoint does not exist
     */
    @Override
    public List<URI> listFiles(final URI uri) throws IOException {
        try {
            final AbstractExplorerFileStore store = getStore(uri);
            final List<URI> children = new ArrayList<>();
            for (final String childName : store.childNames(0, null)) {
                children.add(store.getChild(childName).toURI());
            }
            return children;
        } catch (final CoreException e) {
            throw new IOException(e);
        }

    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if mountpoint does not exist
     */
    @Override
    public BaseFileAttributes getFileAttributes(final URI uri) throws IOException {

        final AbstractExplorerFileStore store = getStore(uri);
        AbstractExplorerFileInfo info;
        try {
            info = store.fetchInfo(0, null);
        } catch (final CoreException e) {
            throw new IOException(e);
        }

        if (!info.exists()) {
            throw new IOException("File at URI does not exist '" + uri.toString() + "'");
        }

        final FileTime lastMod = FileTime.fromMillis(info.getLastModified());

        //FIXME AP-13837 Implement POSIX Attributes for Mountpoint File System
        return new BaseFileAttributes(!info.isDirectory() || AbstractExplorerFileStore.isWorkflow(store), null,
           lastMod, lastMod, lastMod, info.getLength(), false, false, null);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if mountpoint does not exist
     */
    @Override
    public boolean copyFile(final URI source, final URI target) throws IOException {

        final AbstractExplorerFileStore sourceStore = getStore(source);
        final AbstractExplorerFileStore targetStore = getStore(target);
        try {
            sourceStore.copy(targetStore, 0, null);
        } catch (final CoreException e) {
            throw new IOException(e);
        }

        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if mountpoint does not exist
     */
    @Override
    public boolean moveFile(final URI source, final URI target) throws IOException {

        final AbstractExplorerFileStore sourceStore = getStore(source);
        final AbstractExplorerFileStore targetStore = getStore(target);
        try {
            sourceStore.move(targetStore, 0, null);
        } catch (final CoreException e) {
            throw new IOException(e);
        }
        sourceStore.getContentProvider().refresh();
        targetStore.getContentProvider().refresh();

        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if mountpoint does not exist
     */
    @Override
    public boolean deleteFile(final URI uri) throws IOException {
        try {
            getStore(uri).delete(0, null);
        } catch (final CoreException e) {
            throw new IOException(e);
        }

        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if mountpoint does not exist
     */
    @Override
    public void createDirectory(final URI uri) throws IOException {
        try {
            final AbstractExplorerFileStore store = getStore(uri);
            store.mkdir(0, null);
            store.getParent().refresh();
        } catch (final CoreException e) {
            throw new IOException(e);
        }

    }

    /**
     * {@inheritDoc}
     *
     * @since 8.6
     */
    @Override
    public void deployWorkflow(final File source, final URI target, final boolean overwrite, final boolean attemptOpen)
        throws IOException {
        final LocalExplorerFileStore sourceStore = new TmpLocalExplorerFile(source, true);
        final AbstractExplorerFileStore targetStore = getStore(target);
        if (targetStore.fetchInfo().exists() && !overwrite) {
            throw new IOException(
                String.format("Destination path \"%s\" exists and must not be overwritten due to user settings.",
                    targetStore.toString()));
        }
        try {
            targetStore.importAsWorkflow(sourceStore, overwrite, attemptOpen, null);
        } catch (CoreException e) {
            throw new IOException(e);
        }
        targetStore.getParent().refresh();
    }

    /**
     * {@inheritDoc}
     *
     * @since 8.7
     */
    @Override
    public File toLocalWorkflowDir(final URI uri) throws IOException {
        try {
            AbstractExplorerFileStore store = getStore(uri);
            if (!AbstractExplorerFileStore.isWorkflow(store)) {
                throw new IOException("Not a workflow");
            }
            return store.resolveToLocalFile();
        } catch (CoreException e) {
            throw new IOException(e);
        }
    }

    /**
     * @since 8.13
     */
    @Override
    public boolean isAuthenticated(final URI uri) {
        return getStore(uri).getContentProvider().isAuthenticated();
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if mountpoint does not exist
     */
    @Override
    public boolean isReadable(final URI uri) throws IOException {
        //FIXME hacky way to see if we are connected
        try {
            return getStore(uri).getContentProvider().getRootStore().childNames(0, null).length != 0;
        } catch (final CoreException e) {
            throw new IOException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if mountpoint does not exist
     */
    @Override
    public boolean isWorkflow(final URI uri) {
        return AbstractExplorerFileStore.isWorkflow(getStore(uri));
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if mountpoint does not exist
     */
    @Override
    public URI getDefaultDirectory(final URI uri) {
        return getStore(uri).getContentProvider().getRootStore().toURI();
    }

    private static AbstractExplorerFileStore getStore(final URI uri) {
        final AbstractExplorerFileStore store = ExplorerMountTable.getFileSystem().getStore(uri);
        if (store == null) {
            throw new IllegalArgumentException(String.format("Mountpoint %s does not exist.", uri.getHost()));
        } else {
            return store;
        }
    }
}
