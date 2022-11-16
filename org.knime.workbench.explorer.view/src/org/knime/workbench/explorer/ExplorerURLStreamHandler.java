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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.URIUtil;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.util.ClassUtils;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.contextv2.AnalyticsPlatformExecutorInfo;
import org.knime.core.node.workflow.contextv2.HubJobExecutorInfo;
import org.knime.core.node.workflow.contextv2.HubSpaceLocationInfo;
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
import org.knime.core.util.URIPathEncoder;
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

    /**
     * The magic hostname for space-relative URLs.
     *
     * @since 8.9
     */
    public static final String SPACE_RELATIVE = KnimeUrlType.HUB_SPACE_RELATIVE.getAuthority();

    private static final URIPathEncoder UTF8_ENCODER = URIPathEncoder.UTF_8;

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
        final var urlType = KnimeUrlType.getType(url).orElseThrow(
            () -> new IOException("Unexpected protocol: " + url.getProtocol() + ". Only "
                        + KnimeUrlType.SCHEME + " is supported by this handler."));

        final var nodeContext = Optional.ofNullable(NodeContext.getContext());
        final var wfmUI = nodeContext.flatMap(ctx -> ctx.getContextObjectForClass(WorkflowManagerUI.class));
        final var workflowContext = wfmUI.map(WorkflowManagerUI::getContext).orElse(null);
        if (urlType.isRelative()) {
            if (nodeContext.isEmpty()) {
                throw new IOException("No context for relative URL available");
            } else if (workflowContext == null) {
                throw new IOException("Workflow " + wfmUI + " does not have a context");
            }
        }

        final URL result;
        switch (urlType) {
            case HUB_SPACE_RELATIVE:
                result = resolveSpaceRelativeUrl(url, workflowContext);
                break;
            case MOUNTPOINT_RELATIVE:
                result = resolveMountpointRelativeUrl(url, workflowContext); //NOSONAR context `null` checked above
                break;
            case NODE_RELATIVE:
                result = resolveNodeRelativeUrl(url, nodeContext.orElseThrow(), workflowContext);
                break;
            case WORKFLOW_RELATIVE:
                result = resolveWorkflowRelativeUrl(url, workflowContext);
                break;
            case MOUNTPOINT_ABSOLUTE:
                if (matchesDefaultMountIdOnExecutor(workflowContext, url.getHost())) {
                    // resolve mountpoint-absolute URLs on job executors as relative if mount IDs match
                    result = resolveMountpointRelativeUrl(url, workflowContext); //NOSONAR context `null` checked above
                } else {
                    result = url;
                }
                break;
            default:
                throw new IllegalStateException("Unhandled KNIME URL type: " + urlType);
        }

        return UTF8_ENCODER.encodePathSegments(result);
    }

    /**
     * Checks whether mount ID of a mountpoint-absolute URL is resolved by the Job Executor of a Server or Hub which
     * has a matching default mount ID. In this case the URL is resolved locally in the repository.
     *
     * @param workflowContext workflow context
     * @param mountId mount ID of the mountpoint-absolute URL
     * @return {@code true} if the mount ID matches the default mount ID of the executor, {@code false} otherwise
     */
    private static boolean matchesDefaultMountIdOnExecutor(final WorkflowContextUI workflowContext,
            final String mountId) {
        final Optional<String> defaultMountId;
        if (workflowContext instanceof RemoteWorkflowContext) {
            defaultMountId = Optional.of(((RemoteWorkflowContext)workflowContext).getMountId());
        } else {
            defaultMountId = Wrapper.unwrapOptional(workflowContext, WorkflowContextV2.class)
                    .filter(ctx -> ctx.getExecutorInfo() instanceof JobExecutorInfo)
                    .flatMap(ctx -> ClassUtils.castOptional(RestLocationInfo.class, ctx.getLocationInfo()))
                    .map(RestLocationInfo::getDefaultMountId);
        }
        return defaultMountId.map(defMountId -> defMountId.equalsIgnoreCase(mountId)).orElse(false);
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
        final var decodedPath = URIPathEncoder.decodePath(origUrl);
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
        final var decodedPath = URIPathEncoder.decodePath(origUrl);

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
        final var rwc = (RemoteWorkflowContext) workflowContext;
        final var decodedPath = URIPathEncoder.decodePath(origUrl);
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
        final var decodedPath = URIPathEncoder.decodePath(origUrl);

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

        final var resolvedPath = new File(nodeDirectoryRef.getFile().getAbsolutePath(),
            URIPathEncoder.decodePath(origUrl));

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

    private static URL resolveSpaceRelativeUrl(final URL origUrl, final WorkflowContextUI workflowContextUI)
            throws IOException {
        final var context = Wrapper.unwrapOptional(workflowContextUI, WorkflowContextV2.class)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Space relative URLs cannot be resolved from within purely remote workflows."));

        if (!StringUtils.isBlank(origUrl.getQuery())) {
            // Since we stay in the same space, the version shouldn't change either. We reject all query parameters here
            // because we would understand none of them, this may change in the future.
            throw new IllegalArgumentException("Space relative URLs don't support query parameters: " + origUrl);
        }

        if (context.getLocationInfo() instanceof HubSpaceLocationInfo) {
            // resolve against the actual Hub space
            final var hubInfo = (HubSpaceLocationInfo) context.getLocationInfo();
            return resolveSpaceRelativeUrlAgainstHub(origUrl, context, hubInfo);
        }

        try {
            // before a workflow is uploaded to Hub, the local mountpoint can be used as staging area and paths are
            // resolved against it
            final var decodedPath = URIPathEncoder.decodePath(origUrl);
            final var mountpointRelUrl = new URL(KnimeUrlType.SCHEME, MOUNTPOINT_RELATIVE, decodedPath);
            return resolveMountpointRelativeUrl(mountpointRelUrl, workflowContextUI);
        } catch (MalformedURLException e) {
            throw new IOException(e);
        }
    }

    private static URL resolveSpaceRelativeUrlAgainstHub(final URL origUrl, final WorkflowContextV2 workflowContext,
            final HubSpaceLocationInfo hubInfo) throws IOException {
        final var decodedPath = URIPathEncoder.decodePath(origUrl);

        if (workflowContext.getExecutorInfo() instanceof HubJobExecutorInfo) {
            // we're on a Hub executor, resolve workflow locally via the repository
            final var repoAddress = hubInfo.getRepositoryAddress();
            final var spaceRepoUri = URIUtil.append(repoAddress, hubInfo.getSpacePath());
            final var builder = new URIBuilder(URIUtil.append(spaceRepoUri, decodedPath + ":data"));
            hubInfo.getSpaceVersion().ifPresent(v -> builder.addParameter("spaceVersion", v));
            try {
                final var normalizedUrl = builder.build().normalize().toURL();
                final var fullPath = URIPathEncoder.decodePath(normalizedUrl);
                if (fullPath.startsWith(hubInfo.getWorkflowPath())) {
                    // we could allow this at some point and resolve the URL in the local file system
                    throw new IOException("Accessing the workflow contents is not allowed for space relative URLs: "
                            + origUrl + " points into current workflow " + hubInfo.getWorkflowPath());
                }

                final var repoSpacePath = URIPathEncoder.decodePath(spaceRepoUri.toURL());
                if (!fullPath.startsWith(repoSpacePath)) {
                    throw new IOException("Leaving the Hub space is not allowed for space relative URLs: "
                            + decodedPath + " is not in " + hubInfo.getSpacePath());
                }
                return normalizedUrl;
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
        }

        final var mountpointURI = workflowContext.getMountpointURI().orElseThrow(() ->
                new IOException("Cannot resolve space relative URLs outside of Hub or mountpoint: '" + origUrl + "'"));

        // we are mounted in the Analytics Platform, make the ExplorerMountTable sort it out
        final var spaceUri = URIUtil.append(mountpointURI, hubInfo.getSpacePath()).normalize();
        final var resolvedUri = URIUtil.append(spaceUri, decodedPath).normalize();
        if (!resolvedUri.toString().startsWith(spaceUri.toString())) {
            throw new IOException("Leaving the Hub space is not allowed for space relative URLs: "
                    + resolvedUri + " is not in " + spaceUri);
        }
        return resolvedUri.toURL();
    }

    private static URLConnection openExternalMountConnection(final URL url) throws IOException {
        try {
            final var efs = ExplorerMountTable.getFileSystem().getStore(url.toURI());
            return new ExplorerURLConnection(url, efs);
        } catch (URISyntaxException e) {
            throw new IOException(e.getMessage(), e);
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
