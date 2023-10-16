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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;

import javax.net.ssl.HttpsURLConnection;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.ClassUtils;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.contextv2.JobExecutorInfo;
import org.knime.core.node.workflow.contextv2.RestLocationInfo;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.ui.node.workflow.RemoteWorkflowContext;
import org.knime.core.ui.node.workflow.WorkflowContextUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.core.util.KNIMEServerHostnameVerifier;
import org.knime.core.util.KnimeUrlType;
import org.knime.core.util.auth.Authenticator;
import org.knime.core.util.auth.CouldNotAuthorizeException;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.proxy.GlobalProxyConfigProvider;
import org.knime.core.util.proxy.ProxyProtocol;
import org.knime.core.util.proxy.URLConnectionFactory;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.knime.workbench.explorer.urlresolve.KnimeUrlResolver;
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

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ExplorerURLStreamHandler.class);

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
        return openConnection(url, null);
    }

    @Override
    public URLConnection openConnection(final URL url, final Proxy p) throws IOException {
        final var resolvedUrl = resolveKNIMEURL(url);
        if (p == null) {
            return openConnectionForResolved(resolvedUrl);
        } else if (urlPointsToRemote(url)) { // must be unresolved URL to extract mount ID
            final var globalProxy = GlobalProxyConfigProvider.getCurrent().map(cfg -> {
                int intPort;
                try {
                    intPort = Integer.parseInt(cfg.port());
                } catch (NumberFormatException nfe) {
                    intPort = cfg.protocol().getDefaultPort();
                }
                return new Proxy(cfg.protocol() == ProxyProtocol.SOCKS ? Proxy.Type.SOCKS : Proxy.Type.HTTP,
                    new InetSocketAddress(cfg.host(), intPort));
            });
            // log ignored proxy if different to global proxy config
            if (!globalProxy.map(p::equals).orElse(false)) {
                final var identifier = globalProxy.map(Proxy::toString).orElse("no proxy");
                LOGGER.warn(String.format("For the connection to \"%s\" the proxy \"%s\" has been supplied, "
                    + "ignoring and using \"%s\" instead", url, p, identifier));
            }
        }
        // global proxy settings will be applied if and when an actual remote connection is opened
        return openConnectionForResolved(resolvedUrl);
    }

    /**
     * Checks whether the provided URL points to a remote host.
     * This is the case for KNIME URLs to remote mountpoints, and all non-KNIME URLs.
     *
     * @param url
     * @return whether the url points to a remote location
     * @throws IOException
     */
    private static boolean urlPointsToRemote(final URL url) throws IOException {
        if (ExplorerFileSystem.SCHEME.equals(url.getProtocol())) {
            try {
                final var mountId = ExplorerFileSystem.getIDfromURI(url.toURI());
                return !ExplorerMountTable.getAllVisibleLocalMountIDs().contains(mountId);
            } catch (URISyntaxException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
        return true;
    }

    /**
     * Opens the connection to an *already resolved* URL, distinguishes between KNIME URLs (opens the connection via the
     * mount table) and other URLs, like HTTP(S).
     *
     * @param resolvedUrl
     * @return opened connection to the given URL
     * @throws IOException
     */
    private URLConnection openConnectionForResolved(final URL resolvedUrl) throws IOException {
        if (ExplorerFileSystem.SCHEME.equals(resolvedUrl.getProtocol())) {
            return openExternalMountConnection(resolvedUrl);
        } else if ("http".equals(resolvedUrl.getProtocol()) || "https".equals(resolvedUrl.getProtocol())) {
            // neither the node context nor the workflow context can be null here, otherwise resolveKNIMEURL would have
            // already failed
            final var workflowContext =
                NodeContext.getContext().getContextObjectForClass(WorkflowManagerUI.class).orElseThrow().getContext();
            final var conn = URLConnectionFactory.getConnection(resolvedUrl);
            authorizeClient(workflowContext, conn);

            getRemoteRepositoryAddress(workflowContext).ifPresent(u -> m_requestModifier.modifyRequest(u, conn));

            if (conn instanceof HttpsURLConnection) {
                ((HttpsURLConnection)conn).setHostnameVerifier(KNIMEServerHostnameVerifier.getInstance());
            }
            return conn;
        } else {
            return URLConnectionFactory.getConnection(resolvedUrl);
        }
    }

    /**
     * Resolves a knime-URL to the final address. The final address can be a local file-URL in case the workflow runs
     * locally, a KNIME server address, in case the workflow runs inside an executor, or the unaltered address in case
     * it points to a server mount point.
     *
     * @param url a KNIME URL
     * @return the resolved URL
     * @throws ResourceAccessException if an error occurs while resolving the URL
     */
    public static URL resolveKNIMEURL(final URL url) throws ResourceAccessException {
        final var urlType = KnimeUrlType.getType(url).orElseThrow(
            () -> new ResourceAccessException("Unexpected protocol: " + url.getProtocol() + ". Only "
                        + KnimeUrlType.SCHEME + " is supported by this handler."));

        final var nodeContext = Optional.ofNullable(NodeContext.getContext());
        final var wfmUI = nodeContext.flatMap(ctx -> ctx.getContextObjectForClass(WorkflowManagerUI.class));
        final var workflowContext = wfmUI.map(WorkflowManagerUI::getContext).orElse(null);
        if (urlType.isRelative()) {
            if (nodeContext.isEmpty()) {
                throw new ResourceAccessException("No context for relative URL available");
            } else if (workflowContext == null) {
                throw new ResourceAccessException("Workflow " + wfmUI + " does not have a context");
            }
        }

        return KnimeUrlResolver.getResolver(workflowContext).resolve(url);
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

    private static URLConnection openExternalMountConnection(final URL url) throws IOException {
        try {
            final var efs = ExplorerMountTable.getFileSystem().getStore(url.toURI());
            return new ExplorerURLConnection(url, efs);
        } catch (URISyntaxException e) {
            throw new IOException(e.getMessage(), e);
        }
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
