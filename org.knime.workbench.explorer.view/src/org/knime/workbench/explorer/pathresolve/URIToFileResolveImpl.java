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
 *
 */
package org.knime.workbench.explorer.pathresolve;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.Response;
import org.knime.core.util.FileUtil;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.pathresolve.SpaceVersion;
import org.knime.core.util.pathresolve.URIToFileResolve;
import org.knime.workbench.explorer.ExplorerURLStreamHandler;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;
import org.knime.workbench.explorer.urlresolve.URLResolverUtil;

/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class URIToFileResolveImpl implements URIToFileResolve {
    @Override
    public File resolveToFile(final URI uri) throws ResourceAccessException {
        return resolveToFile(uri, new NullProgressMonitor());
    }

    @Override
    public File resolveToLocalOrTempFile(final URI uri) throws ResourceAccessException {
        return resolveToLocalOrTempFile(uri, new NullProgressMonitor());
    }

    @Override
    public File resolveToFile(final URI uri, final IProgressMonitor monitor) throws ResourceAccessException {
        if (uri == null) {
            throw new IllegalArgumentException("Can't resolve null URI to file");
        }
        String scheme = uri.getScheme();
        if ("file".equalsIgnoreCase(scheme)) {
            try {
                return new File(uri);
            } catch (IllegalArgumentException e) {
                throw new ResourceAccessException("Can't resolve file URI \"" + uri + "\" to file", e);
            }
        } else if (ExplorerFileSystem.SCHEME.equalsIgnoreCase(scheme)) {
            var url = ExplorerURLStreamHandler.resolveKNIMEURL(URLResolverUtil.toURL(uri));

            if ("file".equals(url.getProtocol())) {
                return FileUtil.getFileFromURL(url);
            } else if (ExplorerFileSystem.SCHEME.equals(url.getProtocol())) {
                return resolveStandardUri(uri, monitor);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private static File resolveStandardUri(final URI uri, final IProgressMonitor monitor)
            throws ResourceAccessException {
        try {
            final var fileStore = ExplorerFileSystem.INSTANCE.getStore(uri);
            if (fileStore == null) {
                throw new ResourceAccessException(
                    "Can't resolve file to URI \"" + uri + "\"; the corresponding mount point is probably "
                        + "not defined or the resource has been (re)moved");
            }
            return fileStore.toLocalFile(EFS.NONE, monitor);
        } catch (final Exception e) {
            throw new ResourceAccessException("Can't resolve knime URI \"" + uri + "\" to file", e);
        }
    }

    @Override
    public File resolveToLocalOrTempFile(final URI uri, final IProgressMonitor monitor) throws ResourceAccessException {
        return resolveToLocalOrTempFileInternal(uri, monitor, null, null).getEntity().orElse(null);
    }

    private static Response<File> resolveToLocalOrTempFileInternal(final URI uri,
            final IProgressMonitor monitor, final String entityTag, final ZonedDateTime ifModifiedSince)
                    throws ResourceAccessException {
        if (uri == null) {
            throw new IllegalArgumentException("Can't resolve null URI to file");
        }
        String scheme = uri.getScheme();
        if ("file".equalsIgnoreCase(scheme)) {
            try {
                return Response.from(new File(uri), null, null);
            } catch (IllegalArgumentException e) {
                throw new ResourceAccessException("Can't resolve file URI \"" + uri + "\" to file", e);
            }
        } else if (ExplorerFileSystem.SCHEME.equalsIgnoreCase(scheme)) {
            return resolveKnimeUriToLocalOrTempFile(uri, monitor, entityTag, ifModifiedSince);
        } else if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
            return fetchRemoteFile(URLResolverUtil.toURL(uri), entityTag, ifModifiedSince);
        } else {
            throw new ResourceAccessException("Unable to resolve URI \"" + uri + "\" to local file, unknown scheme");
        }
    }

    private static Response<File> resolveKnimeUriToLocalOrTempFile(final URI uri, final IProgressMonitor monitor,
            final String entityTag, final ZonedDateTime ifModifiedSince) throws ResourceAccessException {
        var url = ExplorerURLStreamHandler.resolveKNIMEURL(URLResolverUtil.toURL(uri));
        if ("file".equals(url.getProtocol())) {
            return Response.from(FileUtil.getFileFromURL(url), null, null);
        } else if (ExplorerFileSystem.SCHEME.equals(url.getProtocol())) {
            AbstractExplorerFileStore fs = ExplorerFileSystem.INSTANCE.getStore(uri);
            if (fs instanceof LocalExplorerFileStore) {
                return Response.from(resolveStandardUri(uri, monitor), null, null);
            } else if (fs instanceof RemoteExplorerFileStore) {
                return fetchRemoteFileStore((RemoteExplorerFileStore)fs, monitor, entityTag, ifModifiedSince);
            } else {
                throw new ResourceAccessException("Unsupported file store type: " + fs.getClass());
            }
        } else {
            // use the original URL because otherwise the handler may not be invoked correctly
            return fetchRemoteFile(URLResolverUtil.toURL(uri), entityTag, ifModifiedSince);
        }
    }

    @Override
    public Optional<File> resolveToLocalOrTempFileConditional(final URI uri, final IProgressMonitor monitor,
            final ZonedDateTime ifModifiedSince) throws ResourceAccessException {
        return resolveToLocalOrTempFileConditional(uri, monitor, null, ifModifiedSince).getEntity();
    }

    @Override
    public final Response<File> resolveToLocalOrTempFileConditional(final URI uri, final IProgressMonitor monitor,
            final String entityTag, final ZonedDateTime ifModifiedSince)
                    throws ResourceAccessException {
        return resolveToLocalOrTempFileInternal(uri, monitor, entityTag, ifModifiedSince);
    }

    private static Response<File> fetchRemoteFileStore(final RemoteExplorerFileStore source,
            final IProgressMonitor monitor, final String entityTag, final ZonedDateTime ifModifiedSince)
                    throws ResourceAccessException {
        try {
            return source.resolveToLocalFileConditional(monitor, entityTag, ifModifiedSince);
        } catch (CoreException e) {
            throw new ResourceAccessException(e);
        }
    }

    private static Response<File> fetchRemoteFile(final URL url, final String entityTag,
            final ZonedDateTime ifModifiedSince) throws ResourceAccessException {
        final var res = addAuthHeaderAndOpenStream(url, entityTag, ifModifiedSince);
        final var contents = res.getEntity();
        final var resultETag = res.getETag().orElse(null);
        final var lastModified = res.getLastModified().orElse(null);
        if (contents.isEmpty()) {
            return Response.from(null, resultETag, lastModified);
        }
        try (final var inStream = contents.get()) {
            final var tempFile = FileUtil.createTempFile("download", ".bin");
            try (final var tempFileStream = new FileOutputStream(tempFile)) {
                IOUtils.copy(inStream, tempFileStream);
            }
            return Response.from(tempFile, resultETag, lastModified);
        } catch (final IOException e) {
            throw new ResourceAccessException(e);
        }
    }

    @SuppressWarnings("resource")
    private static Response<InputStream> addAuthHeaderAndOpenStream(final URL url, final String entityTag,
            final ZonedDateTime ifModifiedSince) throws ResourceAccessException {
        try {
            HttpURLConnection uc = (HttpURLConnection)url.openConnection();
            String userInfo = url.getUserInfo();
            if (userInfo != null) {
                String urlDecodedUserInfo = URLDecoder.decode(userInfo, StandardCharsets.UTF_8.name());
                String basicAuth = "Basic "
                    + new String(Base64.getEncoder().encode(urlDecodedUserInfo.getBytes(StandardCharsets.UTF_8)),
                        StandardCharsets.UTF_8);
                uc.setRequestProperty("Authorization", basicAuth);
            }
            if (entityTag != null) {
                uc.setRequestProperty("If-None-Match", entityTag);
            } else if (ifModifiedSince != null) {
                uc.setIfModifiedSince(Objects.requireNonNull(ifModifiedSince).toInstant().toEpochMilli());
            }

            uc.connect();
            final var timestamp = uc.getLastModified();
            final var lastModified = timestamp <= 0 ? null : Instant.ofEpochMilli(timestamp);
            final var eTag = uc.getHeaderField("ETag");
            final var code = uc.getResponseCode();

            if ((entityTag != null || ifModifiedSince != null) && code == HttpURLConnection.HTTP_NOT_MODIFIED) {
                NodeLogger.getLogger(URIToFileResolveImpl.class)
                    .debug("Download of resource at '" + url + "' skipped. Resource not modified.");
                return Response.from(null, eTag, lastModified);
            }
            return Response.from(uc.getInputStream(), eTag, lastModified);
        } catch (IOException e) {
            throw new ResourceAccessException("Failed to open stream: " + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @since 5.0
     */
    @Override
    public boolean isMountpointRelative(final URI uri) {
        return ExplorerFileSystem.SCHEME.equalsIgnoreCase(uri.getScheme())
            && ExplorerURLStreamHandler.MOUNTPOINT_RELATIVE.equalsIgnoreCase(uri.getHost());
    }

    /**
     * {@inheritDoc}
     *
     * @since 5.0
     */
    @Override
    public boolean isWorkflowRelative(final URI uri) {
        return ExplorerFileSystem.SCHEME.equalsIgnoreCase(uri.getScheme())
            && ExplorerURLStreamHandler.WORKFLOW_RELATIVE.equalsIgnoreCase(uri.getHost());
    }

    /**
     * {@inheritDoc}
     *
     * @since 6.4
     */
    @Override
    public boolean isNodeRelative(final URI uri) {
        return ExplorerFileSystem.SCHEME.equalsIgnoreCase(uri.getScheme())
            && ExplorerURLStreamHandler.NODE_RELATIVE.equalsIgnoreCase(uri.getHost());
    }

    @Override
    public boolean isSpaceRelative(final URI uri) {
        return ExplorerFileSystem.SCHEME.equalsIgnoreCase(uri.getScheme())
                && ExplorerURLStreamHandler.SPACE_RELATIVE.equalsIgnoreCase(uri.getHost());
    }

    @Override
    public Optional<KNIMEURIDescription> toDescription(final URI uri, final IProgressMonitor monitor) {
        if (uri.getScheme().equals("file")) {
            return Optional.of(new KNIMEURIDescription(uri.getHost(), uri.getPath()));
        }
        var s = ExplorerFileSystem.INSTANCE.getStore(uri);
        if (s == null) {
            return Optional.empty();
        }
        var mountId = s.getMountID();
        var path = StringUtils.substringAfterLast(s.getMountIDWithFullPath(), ":");
        return Optional.of(new KNIMEURIDescription(mountId, path));
    }

    @Override
    public Optional<List<SpaceVersion>> getSpaceVersions(final URI uri) throws Exception {
        if (uri.getScheme().equals("file")) {
            return Optional.empty();
        }

        var s = ExplorerFileSystem.INSTANCE.getStore(uri);
        if (s instanceof RemoteExplorerFileStore) {
            var remoteFileStore = (RemoteExplorerFileStore)s;
            return Optional.of(remoteFileStore.getSpaceVersions());
        }
        return Optional.empty();
    }
}
