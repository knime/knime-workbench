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
 * ---------------------------------------------------------------------
 *
 * Created: Mar 17, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.net.ssl.HttpsURLConnection;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.URIUtil;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.contextv2.HubSpaceLocationInfo;
import org.knime.core.node.workflow.contextv2.RestLocationInfo;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.ui.node.workflow.RemoteWorkflowContext;
import org.knime.core.ui.node.workflow.WorkflowContextUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.core.util.KNIMEServerHostnameVerifier;
import org.knime.core.util.auth.Authenticator;
import org.knime.core.util.auth.CouldNotAuthorizeException;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.url.AbstractURLStreamHandlerService;

/**
 * Handler for the <tt>knime</tt> protocol. It can resolved three types of URLs:
 * <ul>
 *      <li>workflow-relative URLs using the magic hostname <tt>knime.workflow</tt> (see {@link #WORKFLOW_RELATIVE})</li>
 *      <li>mountpoint-relative URLs using the magic hostname <tt>knime.mountpoint</tt> (see {@link #MOUNTPOINT_RELATIVE})</li>
 *      <li>mount point in the KNIME Explorer using the mount point name as hostname</li>
 * </ul>
 *
 * @author ohl, University of Konstanz
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
public class ExplorerURLStreamHandler extends AbstractURLStreamHandlerService {
    /**
     * The magic hostname for workflow-relative URLs.
     *
     * @since 5.0
     */
    public static final String WORKFLOW_RELATIVE = "knime.workflow";

    /**
     * The magic hostname for mountpoint-relative URLs.
     *
     * @since 5.0
     */
    public static final String MOUNTPOINT_RELATIVE = "knime.mountpoint";

    /**
     * The magic hostname for node-relative URLs.
     *
     * @since 6.4
     */
    public static final String NODE_RELATIVE = "knime.node";

    /**
     * The magic hostname for space-relative URLs.
     *
     * @since 8.9
     */
    public static final String SPACE_RELATIVE = "knime.space";

    private static final String SPACE_VERSION_FORMAT = "spaceVersion=%s";

    private static final URIPathEncoder UTF8_ENCODER = new URIPathEncoder(StandardCharsets.UTF_8);

    private final ServerRequestModifier m_requestModifier;

    /**
     * Creates a new URL stream handler.
     */
    public ExplorerURLStreamHandler() {
        Bundle myself = FrameworkUtil.getBundle(getClass());
        if (myself != null) {
            BundleContext ctx = myself.getBundleContext();
            ServiceReference<ServerRequestModifier> ser = ctx.getServiceReference(ServerRequestModifier.class);
            if (ser != null) {
                try {
                    m_requestModifier = ctx.getService(ser);
                } finally {
                    ctx.ungetService(ser);
                }
            } else {
                m_requestModifier = (p, c) -> {};
            }
        } else {
            m_requestModifier = (p, c) -> {};
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URLConnection openConnection(final URL url) throws IOException {
        URL resolvedUrl = resolveKNIMEURL(url);
        if (ExplorerFileSystem.SCHEME.equals(resolvedUrl.getProtocol())) {
            return openExternalMountConnection(resolvedUrl);
        } else if ("http".equals(resolvedUrl.getProtocol()) || "https".equals(resolvedUrl.getProtocol())) {
            // neither the node context nor the workflow context can be null here, otherwise resolveKNIMEURL would have
            // already failed
            WorkflowContextUI workflowContext =
                NodeContext.getContext().getContextObjectForClass(WorkflowManagerUI.class).get().getContext();
            URLConnection conn = resolvedUrl.openConnection();
            authorizeClient(workflowContext, conn);

            getRemoteRepositoryAddress(workflowContext).ifPresent(u -> m_requestModifier.modifyRequest(u, conn));

            if (conn instanceof HttpsURLConnection) {
                ((HttpsURLConnection)conn).setHostnameVerifier(KNIMEServerHostnameVerifier.getInstance());
            }
            return conn;
        } else {
            return resolvedUrl.openConnection();
        }
    }

    /**
     * Resolves a knime-URL to the final address. The final address can be a local file-URL in case the workflow runs
     * locally, a KNIME server address, in case the workflow runs inside an executor, or the unaltered address in case
     * it points to a server mount point.
     *
     * @param url a KNIME URL
     * @return the resolved URL
     * @throws IOException if an error occurs while resolving the URL
     */
    public static URL resolveKNIMEURL(final URL url) throws IOException {
        if (!ExplorerFileSystem.SCHEME.equalsIgnoreCase(url.getProtocol())) {
            throw new IOException("Unexpected protocol: " + url.getProtocol() + ". Only " + ExplorerFileSystem.SCHEME
                + " is supported by this handler.");
        }
        NodeContext nodeContext = NodeContext.getContext();
        WorkflowContextUI workflowContext;
        if (nodeContext != null) {
            workflowContext =
                nodeContext.getContextObjectForClass(WorkflowManagerUI.class).map(wfm -> wfm.getContext()).orElse(null);
        } else {
            workflowContext = null;
        }

        if (WORKFLOW_RELATIVE.equalsIgnoreCase(url.getHost()) || MOUNTPOINT_RELATIVE.equalsIgnoreCase(url.getHost())
                || NODE_RELATIVE.equalsIgnoreCase(url.getHost()) || SPACE_RELATIVE.equalsIgnoreCase(url.getHost())) {
            if (nodeContext == null) {
                throw new IOException("No context for relative URL available");
            } else if (workflowContext == null) {
                throw new IOException("Workflow " + nodeContext.getContextObjectForClass(WorkflowManagerUI.class)
                    + " does not have a context");
            }
        }

        if (WORKFLOW_RELATIVE.equalsIgnoreCase(url.getHost())) {
            return UTF8_ENCODER.encodePathSegments(resolveWorkflowRelativeUrl(url, workflowContext));
        } else if (MOUNTPOINT_RELATIVE.equalsIgnoreCase(url.getHost()) || ((workflowContext != null)
            && url.getHost().equalsIgnoreCase(getRemoteMountId(workflowContext).orElse(null)))) {
            return UTF8_ENCODER.encodePathSegments(resolveMountpointRelativeUrl(url, workflowContext));
        } else if (NODE_RELATIVE.equalsIgnoreCase(url.getHost())) {
            return UTF8_ENCODER
                .encodePathSegments(resolveNodeRelativeUrl(url, NodeContext.getContext(), workflowContext));
        } else if (SPACE_RELATIVE.equalsIgnoreCase(url.getHost())) {
            return UTF8_ENCODER.encodePathSegments(resolveSpaceRelativeUrl(url, workflowContext));
        } else {
            return UTF8_ENCODER.encodePathSegments(url);
        }
    }

    private static Optional<String> getRemoteMountId(final WorkflowContextUI workflowContext) {
        if (workflowContext instanceof RemoteWorkflowContext) {
            return Optional.of(((RemoteWorkflowContext)workflowContext).getMountId());
        } else {
            return Wrapper.unwrapOptional(workflowContext, WorkflowContextV2.class)
                    .map(WorkflowContextV2::getLocationInfo)
                    .filter(RestLocationInfo.class::isInstance)
                    .map(loc -> ((RestLocationInfo) loc).getDefaultMountId());
        }
    }

    private static Optional<URI> getRemoteRepositoryAddress(final WorkflowContextUI workflowContext) {
        if (workflowContext instanceof RemoteWorkflowContext) {
            return Optional.of(((RemoteWorkflowContext)workflowContext).getRepositoryAddress());
        } else {
            return Wrapper.unwrapOptional(workflowContext, WorkflowContextV2.class)
                    .map(WorkflowContextV2::getLocationInfo)
                    .filter(RestLocationInfo.class::isInstance)
                    .map(loc -> ((RestLocationInfo) loc).getRepositoryAddress());
        }
    }

    private static void authorizeClient(final WorkflowContextUI workflowContext, final URLConnection conn)
        throws IOException {
        Authenticator authenticator = null;
        if (workflowContext instanceof RemoteWorkflowContext) {
            authenticator = ((RemoteWorkflowContext)workflowContext).getServerAuthenticator();
        } else {
            var unwrappedAuthenticator = Wrapper.unwrapOptional(workflowContext, WorkflowContextV2.class)
                    .map(WorkflowContextV2::getLocationInfo)
                    .filter(RestLocationInfo.class::isInstance)
                    .map(loc -> ((RestLocationInfo) loc).getAuthenticator());

            authenticator = unwrappedAuthenticator.orElse(null);
        }

        if (authenticator != null) {
            try {
                authenticator.authorizeClient(conn);
            } catch (CouldNotAuthorizeException e) {
                throw new IOException("Error while authenticating the client: " + e.getMessage(), e);
            }
        }
    }

    private static URL resolveWorkflowRelativeUrl(final URL origUrl, final WorkflowContextUI workflowContext)
        throws IOException {
        if (Wrapper.wraps(workflowContext, WorkflowContextV2.class)) {
            return resolveWorkflowRelativeUrl(origUrl, Wrapper.unwrap(workflowContext, WorkflowContextV2.class).toLegacyWorkflowContext());
        } else {
            assert workflowContext instanceof RemoteWorkflowContext;
            RemoteWorkflowContext rwc = (RemoteWorkflowContext)workflowContext;
            String decodedPath = decodePath(origUrl);
            if (!leavesWorkflow(decodedPath)) {
                throw new IllegalArgumentException(
                    "Workflow relative URL points to a resource within a workflow. Not accessible.");
            }
            URI mpURI = rwc.getMountpointURI();
            URI mpURIWithoutQueryAndFragment;
            try {
                mpURIWithoutQueryAndFragment =
                    new URI(mpURI.getScheme(), null, mpURI.getHost(), mpURI.getPort(), mpURI.getPath(), null, null);
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
            URI uri = URIUtil.append(mpURIWithoutQueryAndFragment, decodedPath);
            return uri.normalize().toURL();
        }
   }

    private static URL resolveWorkflowRelativeUrl(final URL origUrl, final WorkflowContext workflowContext)
        throws IOException {
        String decodedPath = decodePath(origUrl);

        boolean leavesWorkflow = leavesWorkflow(decodedPath);

        if (leavesWorkflow && workflowContext.getRemoteRepositoryAddress().isPresent()
            && workflowContext.getServerAuthenticator().isPresent()) {
            URI uri = URIUtil.append(workflowContext.getRemoteRepositoryAddress().get(),
                workflowContext.getRelativeRemotePath().get() + "/" + decodedPath + ":data");
            return uri.normalize().toURL();
        } else if (leavesWorkflow && workflowContext.isTemporaryCopy() && workflowContext.getMountpointURI().isPresent()) {
            URI uri = URIUtil.append(workflowContext.getMountpointURI().get(), decodedPath);
            return uri.normalize().toURL();
        } else {
            // in local application, an executor controlled by a pre-4.4 server, an old job without a token, or
            // a file inside the workflow
            File currentLocation = workflowContext.getCurrentLocation();
            File resolvedPath = new File(currentLocation, decodedPath);
            if ((workflowContext.getOriginalLocation() != null)
                && !currentLocation.equals(workflowContext.getOriginalLocation())
                && !resolvedPath.getCanonicalPath().startsWith(currentLocation.getCanonicalPath())) {
                // we are outside the current workflow directory => use the original location in the server repository
                resolvedPath = new File(workflowContext.getOriginalLocation(), decodedPath);
            }

            // if resolved path is outside the workflow, check whether it is still inside the mountpoint
            if (!resolvedPath.getCanonicalPath().startsWith(currentLocation.getCanonicalPath())
                && (workflowContext.getMountpointRoot() != null)) {
                URI normalizedRoot = workflowContext.getMountpointRoot().toPath().normalize().toUri();
                URI normalizedPath = resolvedPath.toPath().normalize().toUri();

                if (!normalizedPath.toString().startsWith(normalizedRoot.toString())) {
                    throw new IOException("Leaving the mount point is not allowed for workflow relative URLs: "
                        + resolvedPath.getAbsolutePath() + " is not in "
                        + workflowContext.getMountpointRoot().getAbsolutePath());
                }
            }
            return resolvedPath.toURI().toURL();
        }
    }

    private static URL resolveMountpointRelativeUrl(final URL origUrl, final WorkflowContextUI workflowContext)
        throws IOException {
        if (Wrapper.wraps(workflowContext, WorkflowContextV2.class)) {
            return resolveMountpointRelativeUrl(origUrl, Wrapper.unwrap(workflowContext, WorkflowContextV2.class).toLegacyWorkflowContext());
        } else {
            assert workflowContext instanceof RemoteWorkflowContext;
            RemoteWorkflowContext rwc = (RemoteWorkflowContext)workflowContext;
            String decodedPath = decodePath(origUrl);
            URI mpUri = rwc.getMountpointURI();
            URI uri;
            try {
                uri = new URI(mpUri.getScheme(), mpUri.getHost(), decodedPath, null);
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
            return uri.normalize().toURL();
        }
    }

    private static URL resolveMountpointRelativeUrl(final URL origUrl, final WorkflowContext workflowContext)
        throws IOException {
        String decodedPath = decodePath(origUrl);

        if (workflowContext.getRemoteRepositoryAddress().isPresent()
            && workflowContext.getServerAuthenticator().isPresent()) {
            URI uri = URIUtil.append(workflowContext.getRemoteRepositoryAddress().get(), decodedPath + ":data");
            return uri.normalize().toURL();
        } else if (workflowContext.isTemporaryCopy() && workflowContext.getMountpointURI().isPresent()) {
            try {
                URI mpUri = workflowContext.getMountpointURI().get();
                URI uri = new URI(mpUri.getScheme(), mpUri.getHost(), decodedPath, null);
                return uri.normalize().toURL();
            } catch (URISyntaxException ex) {
                throw new IOException(ex);
            }
        } else {
            // in local application, an executor controlled by a pre-4.4 server, or an old job without a token
            File mountpointRoot = workflowContext.getMountpointRoot();
            File resolvedPath = new File(mountpointRoot, decodedPath);

            URI normalizedPath = resolvedPath.toPath().normalize().toUri();
            URI normalizedRoot = mountpointRoot.toPath().normalize().toUri();

            if (!normalizedPath.toString().startsWith(normalizedRoot.toString())) {
                throw new IOException("Leaving the mount point is not allowed for mount point relative URLs: "
                    + resolvedPath.getAbsolutePath() + " is not in " + mountpointRoot.getAbsolutePath());
            }
            return resolvedPath.toURI().toURL();
        }
    }

    private static URL resolveNodeRelativeUrl(final URL origUrl, final NodeContext nodeContext,
        final WorkflowContextUI workflowContext) throws IOException {
        if (Wrapper.wraps(workflowContext, WorkflowContextV2.class)) {
            return resolveNodeRelativeUrl(origUrl, nodeContext, Wrapper.unwrap(workflowContext, WorkflowContextV2.class).toLegacyWorkflowContext());
        } else {
            throw new IllegalArgumentException(
                "Node relative URLs cannot be resolved from within purely remote workflows.");
        }
    }

    private static URL resolveNodeRelativeUrl(final URL origUrl, final NodeContext nodeContext,
        final WorkflowContext workflowContext) throws IOException {
        ReferencedFile nodeDirectoryRef = nodeContext.getNodeContainer().getNodeContainerDirectory();
        if (nodeDirectoryRef == null) {
            throw new IOException("Workflow must be saved before node-relative URLs can be used");
        }
        String decodedPath = decodePath(origUrl);
        File resolvedPath = new File(nodeDirectoryRef.getFile().getAbsolutePath(), decodedPath);

        File currentLocation = workflowContext.getCurrentLocation();

        // check if resolved path leaves the workflow
        if (!resolvedPath.getCanonicalPath().startsWith(currentLocation.getCanonicalPath())) {
            throw new IOException("Leaving the workflow is not allowed for node-relative URLs: "
                + resolvedPath.getCanonicalPath() + " is not in " + currentLocation.getCanonicalPath());
        }
        return resolvedPath.toURI().toURL();
    }

    private static URL resolveSpaceRelativeUrl(final URL origUrl, final WorkflowContextUI workflowContext)
            throws IOException {

        final var workflowContext2 = NodeContext.getContext().getWorkflowManager().getContextV2();
        final var locationInfo = workflowContext2.getLocationInfo();

        if (locationInfo instanceof HubSpaceLocationInfo) {
            resolveSpaceRelativeUrl(origUrl, (HubSpaceLocationInfo) locationInfo);
        }

        final var decodedPath = decodePath(origUrl);
        final var mountpointRelUrl = URI.create("knime://knime.mountpoint" + decodedPath).toURL();

        return resolveMountpointRelativeUrl(mountpointRelUrl, workflowContext);
    }

    static URL resolveSpaceRelativeUrl(final URL origUrl, final HubSpaceLocationInfo hubInfo)
        throws IOException {

        final var repoAddress = hubInfo.getRepositoryAddress();
        final var repoSpacePath = repoAddress.getPath() + hubInfo.getSpacePath();
        final var decodedPath = decodePath(origUrl);
        final var fullPath = repoSpacePath + decodedPath + ":data";

        final var query = hubInfo.getSpaceVersion().isPresent() ?
            String.format(SPACE_VERSION_FORMAT, hubInfo.getSpaceVersion().get()) : null; //NOSONAR

        try {
            final var normalizedUri = new URI(repoAddress.getScheme(), null,
                repoAddress.getHost(), repoAddress.getPort(), fullPath, query, null).normalize();

            final var normalizedFullPath = normalizedUri.getPath();

            if (!normalizedFullPath.startsWith(repoSpacePath)) {
                throw new IOException("Leaving the Hub space is not allowed for space relative URLs: "
                    + decodedPath + " is not in " + hubInfo.getSpacePath());
            }

            return normalizedUri.toURL();
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    private URLConnection openExternalMountConnection(final URL url) throws IOException {
        AbstractExplorerFileStore efs;
        try {
            efs = ExplorerMountTable.getFileSystem().getStore(url.toURI());
            return new ExplorerURLConnection(url, efs);
        } catch (URISyntaxException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * Extract, decode and return the path part of a given URL. If the path part is not valid according to the spec,
     * assume it is already decoded and return it unchanged.
     * @param url The URL to extract the decoded path part from.
     * @return The decoded path part of the given URL, i.e. any escaped hex sequences in the shape of "% hex hex"
     *         (potentially repeated) are decoded into their respective Unicode characters.
     * @see URI#getPath()
     */
    private static String decodePath(final URL url) {
        try {
            // Obtain the decoded path part using java.net.URI. Note that using java.net.URLDecoder follows a different
            //    spec and is not correct (see AP-17103).
            // We must not use something like `new URI( input.getPath() )` here because inputs
            //    containing double slashes at the beginning of the path part (e.g. knime://knime.workflow//Files;
            //    these are indeed technically valid) will lead the `URI` constructor to incorrectly interpret this as a
            //    scheme part.
            // Instead, we can rely on converting the entire `URL` to `URI`. `URI#getPath` will decode its path part.
            return url.toURI().getPath();
        } catch (URISyntaxException e) {  // NOSONAR: Exception is handled.
            // In this case, we assume there are disallowed characters in the path string, although
            //   there are other instances that also trigger a parse error. This means this method does not enforce
            //   or ensure that the given and returned paths are actually valid.
            // Assuming there are disallowed characters, we conclude that the string is already decoded and return it as-is.
            // (Checking for encoded-ness is not trivial because of ambiguous instances such as "per%cent" which may be
            //   taken literally or interpret %ce as a byte pair representing an encoded character.)
            return url.getPath();
        }
    }

    private static boolean leavesWorkflow(final String decodedPath) {
        return decodedPath.startsWith("/../");
    }

    /**
     * Allows the communication with a "knime" URL.
     *
     * @author ohl, University of Konstanz
     */
    static class ExplorerURLConnection extends URLConnection {
        private final AbstractExplorerFileStore m_file;

        ExplorerURLConnection(final URL u, final AbstractExplorerFileStore file) {
            super(u);
            m_file = file;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void connect() throws IOException {
            // nothing to do
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public InputStream getInputStream() throws IOException {
            if (m_file == null) {
                throw new IOException("Resource associated with \"" + getURL() + "\" does not exist");
            }
            try {
                return m_file.openInputStream(EFS.NONE, null);
            } catch (CoreException e) {
                throw new IOException(e.getMessage(), e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public OutputStream getOutputStream() throws IOException {
            if (m_file == null) {
                throw new IOException("Resource associated with \"" + getURL() + "\" does not exist");
            }
            try {
                return m_file.openOutputStream(EFS.NONE, null);
            } catch (CoreException e) {
                throw new IOException(e.getMessage(), e);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getContentLength() {
            if (m_file == null) {
                return -1;
            }
            long length = m_file.fetchInfo().getLength();
            if (length > Integer.MAX_VALUE) {
                return -1;
            }
            return EFS.NONE == length ? -1 : (int)length;
        }
    }
}
