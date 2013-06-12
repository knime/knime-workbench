/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
import java.net.URLDecoder;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
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
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 */
public class ExplorerURLStreamHandler extends AbstractURLStreamHandlerService {
    /**
     * Return the magic hostname for workflow-relative URLs.
     *
     * @since 5.0
     */
    public static final String WORKFLOW_RELATIVE = "knime.workflow";

    /**
     * Return the magic hostname for mountpoint-relative URLs.
     *
     * @since 5.0
     */
    public static final String MOUNTPOINT_RELATIVE = "knime.mountpoint";

    /**
     * {@inheritDoc}
     */
    @Override
    public URLConnection openConnection(final URL url) throws IOException {
        if (!ExplorerFileSystem.SCHEME.equalsIgnoreCase(url.getProtocol())) {
            throw new IOException("Unexpected protocol: " + url.getProtocol() + ". Only " + ExplorerFileSystem.SCHEME
                + " is supported by this handler.");
        }

        if (WORKFLOW_RELATIVE.equalsIgnoreCase(url.getHost())) {
            return openWorkflowRelativeConnection(url);
        } else if (MOUNTPOINT_RELATIVE.equalsIgnoreCase(url.getHost())) {
            return openMountpointRelativeConnection(url);
        } else {
            return openExternalMountConnection(url);
        }
    }

    private URLConnection openMountpointRelativeConnection(final URL url) throws IOException {
        assert MOUNTPOINT_RELATIVE.equalsIgnoreCase(url.getHost()) : "Wrong magic hostname for mountpoint-relative URLs: "
            + url.getHost();

        NodeContext context = NodeContext.getContext();
        if (context == null) {
            throw new IOException("No context for mountpoint-relative URL available");
        }

        WorkflowContext workflowContext = context.getWorkflowManager().getContext();

        File mountpointRoot = workflowContext.getMountpointRoot();
        File resolvedPath = new File(mountpointRoot, URLDecoder.decode(url.getPath(), "UTF-8"));

        URI normalizedPath = resolvedPath.toURI().normalize();
        URI normalizedRoot = mountpointRoot.toURI().normalize();

        if (!normalizedPath.toString().startsWith(normalizedRoot.toString())) {
            throw new IOException("Leaving the mount point is not allowed for mount point relative URLs: "
                + resolvedPath.getAbsolutePath() + " is not in " + mountpointRoot.getAbsolutePath());
        }
        // FIXME add permission check if run on the server
        return resolvedPath.toURI().toURL().openConnection();
    }

    private URLConnection openWorkflowRelativeConnection(final URL url) throws IOException {
        assert WORKFLOW_RELATIVE.equalsIgnoreCase(url.getHost()) : "Wrong magic hostname for workflow-relative URLs: "
            + url.getHost();

        NodeContext context = NodeContext.getContext();
        if (context == null) {
            throw new IOException("No context for workflow-relative URL available");
        }

        WorkflowContext workflowContext = context.getWorkflowManager().getContext();
        if (workflowContext == null) {
            throw new IOException("No workflow context available");
        }

        File currentLocation = workflowContext.getCurrentLocation();
        File resolvedPath = new File(currentLocation, URLDecoder.decode(url.getPath(), "UTF-8"));
        if ((workflowContext.getOriginalLocation() != null)
            && !currentLocation.equals(workflowContext.getOriginalLocation())
            && !resolvedPath.getCanonicalPath().startsWith(currentLocation.getCanonicalPath())) {
            // we are outside the current workflow directory => use the original location in the server repository
            resolvedPath = new File(workflowContext.getOriginalLocation(), url.getPath());
        }

        if (workflowContext.getMountpointRoot() != null) {
            URI normalizedPath = resolvedPath.toURI().normalize();
            URI normalizedRoot = workflowContext.getMountpointRoot().toURI().normalize();

            if (!normalizedPath.toString().startsWith(normalizedRoot.toString())) {
                throw new IOException("Leaving the mount point is not allowed for mount point relative URLs: "
                    + resolvedPath.getAbsolutePath() + " is not in "
                    + workflowContext.getMountpointRoot().getAbsolutePath());
            }
        }

        // FIXME add permission check if run on the server
        return resolvedPath.toURI().toURL().openConnection();
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
     * Allows the communication with a "knime" URL.
     *
     * @author ohl, University of Konstanz
     *
     */
    static class ExplorerURLConnection extends URLConnection {
        private final AbstractExplorerFileStore m_file;

        /**
         * @param url the specified url
         * @param file the specified file
         */
        public ExplorerURLConnection(final URL url, final AbstractExplorerFileStore file) {
            super(url);
            m_file = file;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void connect() throws IOException {
            // ...
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public InputStream getInputStream() throws IOException {
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
            long length = m_file.fetchInfo().getLength();
            if (length > Integer.MAX_VALUE) {
                length = Integer.MAX_VALUE;
            }
            return EFS.NONE == length ? -1 : (int)length;
        }
    }
}
