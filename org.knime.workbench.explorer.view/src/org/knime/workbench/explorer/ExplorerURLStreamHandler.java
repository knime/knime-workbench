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
import org.knime.core.node.util.ClassUtils;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.contextv2.AnalyticsPlatformExecutorInfo;
import org.knime.core.node.workflow.contextv2.JobExecutorInfo;
import org.knime.core.node.workflow.contextv2.RestLocationInfo;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.ui.node.workflow.RemoteWorkflowContext;
import org.knime.core.ui.node.workflow.WorkflowContextUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.core.util.KNIMEServerHostnameVerifier;
import org.knime.core.util.KnimeUrlType;
import org.knime.core.util.Pair;
import org.knime.core.util.auth.Authenticator;
import org.knime.core.util.auth.CouldNotAuthorizeException;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
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
    public static final String WORKFLOW_RELATIVE = KnimeUrlType.WORKFLOW_RELATIVE.getAuthority();

    /**
     * The magic hostname for mountpoint-relative URLs.
     *
     * @since 5.0
     */
    public static final String MOUNTPOINT_RELATIVE = KnimeUrlType.MOUNTPOINT_RELATIVE.getAuthority();

    /**
     * The magic hostname for node-relative URLs.
     *
     * @since 6.4
     */
    public static final String NODE_RELATIVE = KnimeUrlType.NODE_RELATIVE.getAuthority();

    private static final URIPathEncoder UTF8_ENCODER = new URIPathEncoder(StandardCharsets.UTF_8);

    private final ServerRequestModifier m_requestModifier;

    /**
     * Creates a new URL stream handler.
     */
    public ExplorerURLStreamHandler() {
        final var myself = FrameworkUtil.getBundle(getClass());
        if (myself != null) {
            final var ctx = myself.getBundleContext();
            final ServiceReference<ServerRequestModifier> ser = ctx.getServiceReference(ServerRequestModifier.class);
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

    @Override
    public URLConnection openConnection(final URL url) throws IOException {
        final var resolvedUrl = resolveKNIMEURL(url);
        if (ExplorerFileSystem.SCHEME.equals(resolvedUrl.getProtocol())) {
            return openExternalMountConnection(resolvedUrl);
        } else if ("http".equals(resolvedUrl.getProtocol()) || "https".equals(resolvedUrl.getProtocol())) {
            // neither the node context nor the workflow context can be null here, otherwise resolveKNIMEURL would have
            // already failed
            final var workflowContext =
                NodeContext.getContext().getContextObjectForClass(WorkflowManagerUI.class).orElseThrow().getContext();
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
        if (!KnimeUrlType.SCHEME.equalsIgnoreCase(url.getProtocol())) {
            throw new IOException("Unexpected protocol: " + url.getProtocol() + ". Only " + KnimeUrlType.SCHEME
                + " is supported by this handler.");
        }

        final var nodeContext = Optional.ofNullable(NodeContext.getContext());
        final var wfmUI = nodeContext.flatMap(ctx -> ctx.getContextObjectForClass(WorkflowManagerUI.class));
        final var workflowContext = wfmUI.map(WorkflowManagerUI::getContext).orElse(null);

        final var host = url.getHost();
        final var isWorkflowRel = WORKFLOW_RELATIVE.equalsIgnoreCase(host);
        final var isMountpointRel = MOUNTPOINT_RELATIVE.equalsIgnoreCase(host);
        final var isNodeRel = NODE_RELATIVE.equalsIgnoreCase(host);
        if (isWorkflowRel || isMountpointRel || isNodeRel) {
            if (nodeContext.isEmpty()) {
                throw new IOException("No context for relative URL available");
            } else if (workflowContext == null) {
                throw new IOException("Workflow " + wfmUI + " does not have a context");
            }
        }

        final URL result;
        if (isWorkflowRel) {
            result = resolveWorkflowRelativeUrl(url, workflowContext);
        } else if (isMountpointRel || (workflowContext != null
                && host.equalsIgnoreCase(getRemoteMountId(workflowContext).orElse(null)))) {
            result = resolveMountpointRelativeUrl(url, workflowContext);
        } else if (isNodeRel) {
            result = resolveNodeRelativeUrl(url, nodeContext.orElseThrow(), workflowContext);
        } else {
            result = url;
        }
        return UTF8_ENCODER.encodePathSegments(result);
    }

    private static Optional<String> getRemoteMountId(final WorkflowContextUI workflowContext) {
        if (workflowContext instanceof RemoteWorkflowContext) {
            return Optional.of(((RemoteWorkflowContext)workflowContext).getMountId());
        } else {
            return Wrapper.unwrapOptional(workflowContext, WorkflowContextV2.class)
                    .filter(ctx -> ctx.getExecutorInfo() instanceof JobExecutorInfo)
                    .flatMap(ctx -> ClassUtils.castOptional(RestLocationInfo.class, ctx.getLocationInfo()))
                    .map(RestLocationInfo::getDefaultMountId);
        }
    }

    private static Optional<URI> getRemoteRepositoryAddress(final WorkflowContextUI workflowContext) {
        if (workflowContext instanceof RemoteWorkflowContext) {
            return Optional.of(((RemoteWorkflowContext) workflowContext).getRepositoryAddress());
        } else {
            return Wrapper.unwrapOptional(workflowContext, WorkflowContextV2.class)
                    .filter(ctx -> ctx.getExecutorInfo() instanceof JobExecutorInfo)
                    .flatMap(ctx -> ClassUtils.castOptional(RestLocationInfo.class, ctx.getLocationInfo()))
                    .map(RestLocationInfo::getRepositoryAddress);
        }
    }

    private static Optional<Authenticator> getServerAuthenticator(final WorkflowContextUI workflowContext) {
        if (workflowContext instanceof RemoteWorkflowContext) {
            return Optional.of(((RemoteWorkflowContext) workflowContext).getServerAuthenticator());
        } else {
            return Wrapper.unwrapOptional(workflowContext, WorkflowContextV2.class)
                    .filter(ctx -> ctx.getExecutorInfo() instanceof JobExecutorInfo)
                    .flatMap(ctx -> ClassUtils.castOptional(RestLocationInfo.class, ctx.getLocationInfo()))
                    .map(RestLocationInfo::getAuthenticator);
        }
    }

    private static void authorizeClient(final WorkflowContextUI workflowContext, final URLConnection conn)
        throws IOException {
        final Optional<Authenticator> authenticator = getServerAuthenticator(workflowContext);
        if (authenticator.isPresent()) {
            try {
                authenticator.get().authorizeClient(conn);
            } catch (CouldNotAuthorizeException e) {
                throw new IOException("Error while authenticating the client: " + e.getMessage(), e);
            }
        }
    }

    private static URL resolveWorkflowRelativeUrl(final URL origUrl, final WorkflowContextUI workflowContext)
        throws IOException {
        if (Wrapper.wraps(workflowContext, WorkflowContextV2.class)) {
            return resolveWorkflowRelativeUrl(origUrl, Wrapper.unwrap(workflowContext, WorkflowContextV2.class));
        }

        assert workflowContext instanceof RemoteWorkflowContext;
        RemoteWorkflowContext rwc = (RemoteWorkflowContext) workflowContext;
        final var decodedPath = decodePath(origUrl);
        if (!leavesWorkflow(decodedPath)) {
            throw new IllegalArgumentException(
                "Workflow relative URL points to a resource within a workflow. Not accessible.");
        }
        final var mpURI = rwc.getMountpointURI();
        final URI mpURIWithoutQueryAndFragment;
        try {
            mpURIWithoutQueryAndFragment =
                new URI(mpURI.getScheme(), null, mpURI.getHost(), mpURI.getPort(), mpURI.getPath(), null, null);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        final var uri = URIUtil.append(mpURIWithoutQueryAndFragment, decodedPath);
        return uri.normalize().toURL();
   }

    private static URL resolveWorkflowRelativeUrl(final URL origUrl, final WorkflowContextV2 workflowContext2)
            throws IOException {
        final var decodedPath = decodePath(origUrl);

        final boolean leavesWorkflow = leavesWorkflow(decodedPath);

        final var executor = workflowContext2.getExecutorInfo();
        final var restLocation = ClassUtils.castOptional(RestLocationInfo.class, workflowContext2.getLocationInfo());
        if (leavesWorkflow && executor instanceof JobExecutorInfo && restLocation.isPresent()) {
            // we're on a server of hub executor, resolve against the repository
            final var restLocationInfo = restLocation.get();
            final var uri = URIUtil.append(restLocationInfo.getRepositoryAddress(),
                restLocationInfo.getWorkflowPath() + "/" + decodedPath + ":data");
            return uri.normalize().toURL();
        }

        final var mountpointURI = workflowContext2.getMountpointURI();
        if (leavesWorkflow && workflowContext2.isTemporyWorkflowCopyMode() && mountpointURI.isPresent()) {
            // remote REST location, access via mountpoint-absolute URL
            final var uri = URIUtil.append(mountpointURI.get(), decodedPath);
            return uri.normalize().toURL();
        }

        // in local application, an executor controlled by a pre-4.4 server, an old job without a token,
        // or a file inside the workflow
        final var currentLocation = workflowContext2.getExecutorInfo().getLocalWorkflowPath();
        final var resolvedFile = new File(currentLocation.toFile(), decodedPath);

        // if resolved path is outside the workflow, check whether it is still inside the mountpoint
        final var mountpoint = ClassUtils.castOptional(AnalyticsPlatformExecutorInfo.class, executor)
                .flatMap(AnalyticsPlatformExecutorInfo::getMountpoint);

        if (!resolvedFile.getCanonicalPath().startsWith(currentLocation.toFile().getCanonicalPath())
                && mountpoint.isPresent()) {
            final var mountpointRoot = mountpoint.get().getSecond();
            final var normalizedRoot = mountpointRoot.normalize().toUri();
            final var normalizedPath = resolvedFile.toPath().normalize().toUri();

            if (!normalizedPath.toString().startsWith(normalizedRoot.toString())) {
                throw new IOException("Leaving the mount point is not allowed for workflow relative URLs: "
                    + resolvedFile.getAbsolutePath() + " is not in " + mountpointRoot.toAbsolutePath());
            }
        }
        return resolvedFile.toURI().toURL();
    }

    private static URL resolveMountpointRelativeUrl(final URL origUrl, final WorkflowContextUI workflowContext)
        throws IOException {
        if (Wrapper.wraps(workflowContext, WorkflowContextV2.class)) {
            return resolveMountpointRelativeUrl(origUrl, Wrapper.unwrap(workflowContext, WorkflowContextV2.class));
        }
        assert workflowContext instanceof RemoteWorkflowContext;
        RemoteWorkflowContext rwc = (RemoteWorkflowContext)workflowContext;
        String decodedPath = decodePath(origUrl);
        final var mpUri = rwc.getMountpointURI();
        final URI uri;
        try {
            uri = new URI(mpUri.getScheme(), mpUri.getHost(), decodedPath, null);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        return uri.normalize().toURL();
    }

    private static URL resolveMountpointRelativeUrl(final URL origUrl, final WorkflowContextV2 workflowContext)
            throws IOException {
        final var decodedPath = decodePath(origUrl);

        final var executorInfo = workflowContext.getExecutorInfo();
        final var restLocationInfo = ClassUtils.castOptional(RestLocationInfo.class, workflowContext.getLocationInfo());
        if (executorInfo instanceof JobExecutorInfo && restLocationInfo.isPresent()) {
            // we're in a server or hub executor, access the repository
            final var uri = URIUtil.append(restLocationInfo.get().getRepositoryAddress(), decodedPath + ":data");
            return uri.normalize().toURL();
        }

        final var mountpointURI = workflowContext.getMountpointURI();
        if (workflowContext.isTemporyWorkflowCopyMode() && mountpointURI.isPresent()) {
            try {
                // remote REST location, access via mountpoint-absolute URL
                final var mpUri = mountpointURI.get();
                final var uri = new URI(mpUri.getScheme(), mpUri.getHost(), decodedPath, null);
                return uri.normalize().toURL();
            } catch (URISyntaxException ex) {
                throw new IOException(ex);
            }
        }

        // in local application, an executor controlled by a pre-4.4 server, or an old job without a token
        final var mountpointRoot = ClassUtils.castOptional(AnalyticsPlatformExecutorInfo.class, executorInfo)
                .flatMap(AnalyticsPlatformExecutorInfo::getMountpoint)
                .map(Pair::getSecond)
                .orElseThrow(() -> new IllegalStateException("Mountpoint-relative URL without a mountpoint."));
        final var resolvedFile = new File(mountpointRoot.toFile(), decodedPath);
        final var normalizedPath = resolvedFile.toPath().normalize().toUri();
        final var normalizedRoot = mountpointRoot.normalize().toUri();

        if (!normalizedPath.toString().startsWith(normalizedRoot.toString())) {
            throw new IOException("Leaving the mount point is not allowed for mount point relative URLs: "
                + resolvedFile.getAbsolutePath() + " is not in " + mountpointRoot.toFile().getAbsolutePath());
        }
        return resolvedFile.toURI().toURL();
    }

    private static URL resolveNodeRelativeUrl(final URL origUrl, final NodeContext nodeContext,
        final WorkflowContextUI workflowContext) throws IOException {
        if (Wrapper.wraps(workflowContext, WorkflowContextV2.class)) {
            return resolveNodeRelativeUrl(origUrl, nodeContext,
                Wrapper.unwrap(workflowContext, WorkflowContextV2.class));
        } else {
            throw new IllegalArgumentException(
                "Node relative URLs cannot be resolved from within purely remote workflows.");
        }
    }

    private static URL resolveNodeRelativeUrl(final URL origUrl, final NodeContext nodeContext,
        final WorkflowContextV2 workflowContext) throws IOException {
        ReferencedFile nodeDirectoryRef = nodeContext.getNodeContainer().getNodeContainerDirectory();
        if (nodeDirectoryRef == null) {
            throw new IOException("Workflow must be saved before node-relative URLs can be used");
        }

        final var resolvedPath = new File(nodeDirectoryRef.getFile().getAbsolutePath(), decodePath(origUrl));

        // check if resolved path leaves the workflow
        final var currentLocation = workflowContext.getExecutorInfo().getLocalWorkflowPath().toFile();
        final var resolved = resolvedPath.getCanonicalPath();
        final var workflow = currentLocation.getCanonicalPath();
        if (!resolved.startsWith(workflow)) {
            throw new IOException("Leaving the workflow is not allowed for node-relative URLs: "
                + resolved + " is not in " + workflow);
        }
        return resolvedPath.toURI().toURL();
    }

    private static URLConnection openExternalMountConnection(final URL url) throws IOException {
        try {
            final var efs = ExplorerMountTable.getFileSystem().getStore(url.toURI());
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

        @Override
        public void connect() throws IOException {
            // nothing to do
        }

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
