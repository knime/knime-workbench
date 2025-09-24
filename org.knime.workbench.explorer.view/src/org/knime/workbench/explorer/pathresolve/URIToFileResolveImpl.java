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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.mail.internet.ContentDisposition;
import javax.mail.internet.ParseException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.hc.core5.http.HttpHeaders;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.util.FileUtil;
import org.knime.core.util.KnimeUrlType;
import org.knime.core.util.ThreadLocalHTTPAuthenticator;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.hub.NamedItemVersion;
import org.knime.core.util.pathresolve.SpaceVersion;
import org.knime.core.util.pathresolve.URIToFileResolve;
import org.knime.core.util.proxy.URLConnectionFactory;
import org.knime.core.util.urlresolve.URLResolverUtil;
import org.knime.workbench.explorer.ExplorerURLStreamHandler;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;

/**
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class URIToFileResolveImpl implements URIToFileResolve {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(URIToFileResolveImpl.class);

    private static final Pattern KNWF_KNAR_PATTERN = Pattern.compile("\\.kn(wf|ar)$", Pattern.CASE_INSENSITIVE);

    private static final String FILE_PROTOCOL = "file";

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
        if (FILE_PROTOCOL.equalsIgnoreCase(scheme)) {
            try {
                return new File(uri);
            } catch (IllegalArgumentException e) {
                throw new ResourceAccessException("Can't resolve file URI \"" + uri + "\" to file", e);
            }
        } else if (ExplorerFileSystem.SCHEME.equalsIgnoreCase(scheme)) {
            var url = ExplorerURLStreamHandler.resolveKNIMEURL(URLResolverUtil.toURL(uri));

            if (FILE_PROTOCOL.equalsIgnoreCase(url.getProtocol())) {
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
            AbstractExplorerFileStore s = ExplorerFileSystem.INSTANCE.getStore(uri);
            if (s == null) {
                throw new ResourceAccessException(
                    "Can't resolve file to URI \"" + uri + "\"; the corresponding mount point is probably "
                        + "not defined or the resource has been (re)moved");
            }
            return s.toLocalFile(EFS.NONE, monitor);
        } catch (Exception e) {
            throw new ResourceAccessException("Can't resolve KNIME URL \"" + uri + "\" to local file or folder", e);
        }
    }

    @Override
    public File resolveToLocalOrTempFile(final URI uri, final IProgressMonitor monitor) throws ResourceAccessException {
        return resolveToLocalOrTempFileInternal(uri, monitor, null);
    }

    private static File resolveToLocalOrTempFileInternal(final URI uri, final IProgressMonitor monitor,
        final ZonedDateTime ifModifiedSince) throws ResourceAccessException {
        if (uri == null) {
            throw new IllegalArgumentException("Can't resolve null URI to file");
        }
        String scheme = uri.getScheme();
        if (FILE_PROTOCOL.equalsIgnoreCase(scheme)) {
            try {
                return new File(uri);
            } catch (IllegalArgumentException e) {
                throw new ResourceAccessException("Can't resolve file URI \"" + uri + "\" to file", e);
            }
        } else if (ExplorerFileSystem.SCHEME.equalsIgnoreCase(scheme)) {
            return resolveKnimeUriToLocalOrTempFile(uri, monitor, ifModifiedSince);
        } else if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
            return fetchRemoteFile(URLResolverUtil.toURL(uri), ifModifiedSince);
        } else {
            throw new ResourceAccessException("Unable to resolve URI \"" + uri + "\" to local file, unknown scheme");
        }
    }

    private static File resolveKnimeUriToLocalOrTempFile(final URI uri, final IProgressMonitor monitor,
        final ZonedDateTime ifModifiedSince) throws ResourceAccessException {
        var url = ExplorerURLStreamHandler.resolveKNIMEURL(URLResolverUtil.toURL(uri));
        if (FILE_PROTOCOL.equalsIgnoreCase(url.getProtocol())) {
            return FileUtil.getFileFromURL(url);
        } else if (ExplorerFileSystem.SCHEME.equals(url.getProtocol())) {
            final AbstractExplorerFileStore fs = ExplorerFileSystem.INSTANCE.getStore(uri);

            if (fs instanceof LocalExplorerFileStore) {
                return resolveStandardUri(uri, monitor);
            } else if (fs instanceof RemoteExplorerFileStore remoteStore) {
                return fetchRemoteFileStore(remoteStore, monitor, ifModifiedSince);
            } else {
                throw new ResourceAccessException("Unsupported file store type: " + fs.getClass());
            }
        } else {
            // use the original URL because otherwise the handler may not be invoked correctly
            return fetchRemoteFile(URLResolverUtil.toURL(uri), ifModifiedSince);
        }
    }

    @Override
    public Optional<File> resolveToLocalOrTempFileConditional(final URI uri, final IProgressMonitor monitor,
        final ZonedDateTime ifModifiedSince) throws ResourceAccessException {
        return Optional.ofNullable(resolveToLocalOrTempFileInternal(uri, monitor, ifModifiedSince));
    }

    private static File fetchRemoteFileStore(final RemoteExplorerFileStore source, final IProgressMonitor monitor,
        final ZonedDateTime ifModifiedSince) throws ResourceAccessException {
        try {
            return source.resolveToLocalFileConditional(monitor, ifModifiedSince).orElse(null);
        } catch (CoreException e) {
            throw new ResourceAccessException(e);
        }
    }

    private static File fetchRemoteFile(final URL url, final ZonedDateTime ifModifiedSince)
        throws ResourceAccessException {
        InputStream inputStream = addAuthHeaderAndOpenStream(url, ifModifiedSince);
        File f = null;
        if (inputStream != null) {
            try {
                f = FileUtil.createTempFile("download", ".bin");
                try (InputStream is = inputStream; OutputStream os = new FileOutputStream(f)) {
                    IOUtils.copy(is, os);
                }
            } catch (IOException e) {
                throw new ResourceAccessException(e);
            }
        }
        return f;
    }

    private static InputStream addAuthHeaderAndOpenStream(final URL url, final ZonedDateTime ifModifiedSince)
        throws ResourceAccessException {
        try (final var c = ThreadLocalHTTPAuthenticator.suppressAuthenticationPopups()) {
            final var uc = URLConnectionFactory.getConnection(url);
            String userInfo = url.getUserInfo();
            if (userInfo != null) {
                String urlDecodedUserInfo = URLDecoder.decode(userInfo, StandardCharsets.UTF_8.name());
                String basicAuth = "Basic "
                    + new String(Base64.getEncoder().encode(urlDecodedUserInfo.getBytes(StandardCharsets.UTF_8)),
                        StandardCharsets.UTF_8);
                uc.setRequestProperty("Authorization", basicAuth);
            }
            if (ifModifiedSince != null) {
                uc.setIfModifiedSince(ifModifiedSince.toInstant().toEpochMilli());
                uc.connect();
                if (uc instanceof HttpURLConnection huc
                    && huc.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                    LOGGER.debug("Download of resource at '" + url + "' skipped. Resource not modified.");
                    return null;
                }
            }
            return uc.getInputStream();
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
        if (FILE_PROTOCOL.equalsIgnoreCase(uri.getScheme())) {
            try {
                return getDescriptionFromFile(uri.toURL());
            } catch (final MalformedURLException e) {
                LOGGER.debug("Failed to resolve name from file '%s'".formatted(uri), e);
                return Optional.empty();
            }
        }

        if (KnimeUrlType.getType(uri).isPresent()) {
            return getDescriptionFromFilestore(uri) // try to get from filestore (mount point) in local AP
                .or(() -> getDescriptionFromURLConnection(uri)); // try to get from URLConnection in executor
        }

        // neither a knime:// nor a file:// URL
        return Optional.empty();
    }

    private static Optional<KNIMEURIDescription> getDescriptionFromFile(final URL url) {
        final var file = FileUtil.getFileFromURL(url);
        return Optional.of(new KNIMEURIDescription(url.getHost(), file.getAbsolutePath(), file.getName()));
    }

    private static Optional<KNIMEURIDescription> getDescriptionFromFilestore(final URI uri) {
        AbstractExplorerFileStore fileStore = ExplorerFileSystem.INSTANCE.getStore(uri);
        if (fileStore != null) {
            final var fileStoreInfo = fileStore.fetchInfo();
            if (fileStoreInfo.exists()) {
                final var mountId = fileStore.getMountID();
                final var path = StringUtils.substringAfterLast(fileStore.getMountIDWithFullPath(), ":");
                return Optional.of(new KNIMEURIDescription(mountId, path, fileStoreInfo.getName()));
            }
        }
        return Optional.empty();
    }

    private static Optional<KNIMEURIDescription> getDescriptionFromURLConnection(final URI uri) {
        URLConnection connection = null;
        try (final var c = ThreadLocalHTTPAuthenticator.suppressAuthenticationPopups()) {
            connection = URLConnectionFactory.getConnection(URLResolverUtil.toURL(uri));

            if (connection instanceof HttpURLConnection httpURLConnection) {
                // HTTP URL, probably on an executor
                return getDescriptionFromResponseHeader(uri, httpURLConnection);
            }

            // KNIME URLs can be resolved to file:// URLs inside the workflow
            final var resolvedUrl = connection.getURL();
            if (FILE_PROTOCOL.equalsIgnoreCase(resolvedUrl.getProtocol())) {
                return getDescriptionFromFile(resolvedUrl);
            }

            // we don't know how to extract a name from other protocols
            return Optional.empty();
        } catch (IOException | IllegalArgumentException | ParseException e) {
            LOGGER.debug("Failed to resolve name from Content-Disposition header", e);
            return Optional.empty();
        } finally {
            if (connection instanceof HttpURLConnection httpURLConnection) {
                httpURLConnection.disconnect();
            }
        }
    }

    private static Optional<KNIMEURIDescription> getDescriptionFromResponseHeader(final URI uri,
        final HttpURLConnection httpURLConnection) throws ParseException {
        // A HEAD request is not enough, the Content-Disposition response header is
        // not provided there; the below will implicitly connect
        final var header = httpURLConnection.getHeaderField(HttpHeaders.CONTENT_DISPOSITION);
        final var itemName = header == null ? null : new ContentDisposition(header).getParameter("filename");
        if (StringUtils.isBlank(itemName)) {
            LOGGER.debug("Expected Content-Disposition header with filename parameter, but got none");
            return Optional.empty();
        }
        return Optional.of(new KNIMEURIDescription(uri.getHost(), uri.toString(),
            KNWF_KNAR_PATTERN.matcher(itemName).replaceFirst("")));
    }

    @Override
    public Optional<List<SpaceVersion>> getSpaceVersions(final URI uri) throws Exception {
        if (FILE_PROTOCOL.equalsIgnoreCase(uri.getScheme())) {
            return Optional.empty();
        }

        var s = ExplorerFileSystem.INSTANCE.getStore(uri);
        if (s instanceof RemoteExplorerFileStore remoteFileStore) {
            return Optional.of(remoteFileStore.getSpaceVersions());
        }
        return Optional.empty();
    }

    /**
    * @deprecated use {@link #getHubItemVersionList(URI)}
    */
    @Override
    @Deprecated(since = "5.4")
    public List<NamedItemVersion> getHubItemVersions(final URI uri) {
        try {
            return getHubItemVersionList(uri);
        } catch (ResourceAccessException ex) {
            throw ExceptionUtils.asRuntimeException(ex);
        }
    }

    @Override
    public List<NamedItemVersion> getHubItemVersionList(final URI uri) throws ResourceAccessException {
        CheckUtils.checkArgument(KnimeUrlType.getType(uri).isPresent(), "Expected a KNIME URI but got: %s", uri);

        var s = ExplorerFileSystem.INSTANCE.getStore(uri);

        CheckUtils.checkState(s instanceof RemoteExplorerFileStore,
            "Cannot retrieve Hub item versions for %s, the content is not available via a mount point.", uri);

        var remoteFileStore = (RemoteExplorerFileStore)s;
        return remoteFileStore.getVersions();

    }
}
