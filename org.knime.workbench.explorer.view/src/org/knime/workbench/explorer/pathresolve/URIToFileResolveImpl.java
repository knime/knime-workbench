/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
 *
 */
package org.knime.workbench.explorer.pathresolve;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.util.pathresolve.URIToFileResolve;
import org.knime.workbench.explorer.ExplorerURLStreamHandler;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public class URIToFileResolveImpl implements URIToFileResolve {
    /** {@inheritDoc} */
    @Override
    public File resolveToFile(final URI uri) throws IOException {
        return resolveToFile(uri, new NullProgressMonitor());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File resolveToLocalOrTempFile(final URI uri) throws IOException {
        return resolveToLocalOrTempFile(uri, new NullProgressMonitor());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File resolveToFile(final URI uri, final IProgressMonitor monitor) throws IOException {
        if (uri == null) {
            throw new IOException("Can't resolve null URI to file");
        }
        String scheme = uri.getScheme();
        if (scheme == null) {
            throw new IOException("Can't resolve URI \"" + uri + "\": it does not have a scheme");
        }
        if (scheme.equalsIgnoreCase("file")) {
            try {
                return new File(uri);
            } catch (IllegalArgumentException e) {
                throw new IOException("Can't resolve file URI \"" + uri + "\" to file", e);
            }
        }
        if (scheme.equalsIgnoreCase(ExplorerFileSystem.SCHEME)) {

            if (ExplorerURLStreamHandler.WORKFLOW_RELATIVE.equalsIgnoreCase(uri.getHost())) {
                return resolveWorkflowRelativeUri(uri);
            } else if (ExplorerURLStreamHandler.MOUNTPOINT_RELATIVE.equalsIgnoreCase(uri.getHost())) {
                return resolveMountpointRelativeUri(uri);
            } else {
                return resolveStandardUri(uri, monitor);
            }
        }
        throw new IOException("Unable to resolve URI \"" + uri + "\" to local file, unknown scheme");
    }

    private File resolveStandardUri(final URI uri, final IProgressMonitor monitor) throws IOException {
        try {
            AbstractExplorerFileStore s = ExplorerFileSystem.INSTANCE.getStore(uri);
            if (s == null) {
                throw new IOException("Can't resolve file to URI \"" + uri
                    + "\"; the corresponding mount point is probably "
                    + "not defined or the resource has been (re)moved");
            }
            return s.toLocalFile(EFS.NONE, monitor);
        } catch (Exception e) {
            throw new IOException("Can't resolve knime URI \"" + uri + "\" to file", e);
        }
    }

    /**
     * Takes a mountpoint-relative knime URI (i.e. <tt>knime://knime.mountpoint/...</tt>) and resolves into its
     * corresponding file object.
     *
     * @param uri a mountpoint-relative URI
     * @return a file object
     * @throws IOException if the resolution fails because of a missing node context or if the URI leaves the
     *             mountpoint root
     * @throws IllegalArgumentException if the URI is not a mountpoint-relative URI
     * @since 5.0
     */
    public static File resolveMountpointRelativeUri(final URI uri) throws IOException {
        if (!ExplorerURLStreamHandler.MOUNTPOINT_RELATIVE.equalsIgnoreCase(uri.getHost())) {
            throw new IllegalArgumentException("Wrong magic hostname for mountpoint-relative URLs: " + uri.getHost());
        }

        NodeContext context = NodeContext.getContext();
        if (context == null) {
            throw new IOException("No context for mountpoint-relative URL available");
        }

        WorkflowContext workflowContext = context.getWorkflowManager().getContext();
        if (workflowContext == null) {
            throw new IOException("Workflow " + context.getWorkflowManager() + " does not have a context");
        }

        File mountpointRoot = workflowContext.getMountpointRoot();
        File resolvedPath = new File(mountpointRoot, URLDecoder.decode(uri.getPath(), "UTF-8"));

        URI normalizedPath = resolvedPath.toURI().normalize();
        URI normalizedRoot = mountpointRoot.toURI().normalize();

        if (!normalizedPath.toString().startsWith(normalizedRoot.toString())) {
            throw new IOException("Leaving the mount point is not allowed for mount point relative URLs: "
                + resolvedPath.getAbsolutePath() + " is not in " + mountpointRoot.getAbsolutePath());
        }
        return resolvedPath;
    }

    /**
     * Takes a workflow-relative knime URI (i.e. <tt>knime://knime.workflow/...</tt>) and resolves into its
     * corresponding file object.
     *
     * @param uri a workflow-relative URI
     * @return a file object
     * @throws IOException if the resolution fails because of a missing node context or if the URI leaves the
     *             mountpoint root
     * @throws IllegalArgumentException if the URI is not a workflow-relative URI
     * @since 5.0
     */
    public static File resolveWorkflowRelativeUri(final URI uri) throws IOException {
        if (!ExplorerURLStreamHandler.WORKFLOW_RELATIVE.equalsIgnoreCase(uri.getHost())) {
            throw new IllegalArgumentException("Wrong magic hostname for workflow-relative URLs: " + uri.getHost());
        }

        NodeContext context = NodeContext.getContext();
        if (context == null) {
            throw new IOException("No context for workflow-relative URL available");
        }

        WorkflowContext workflowContext = context.getWorkflowManager().getContext();
        if (workflowContext == null) {
            throw new IOException("Workflow " + context.getWorkflowManager() + " does not have a context");
        }

        File currentLocation = workflowContext.getCurrentLocation();
        File resolvedPath = new File(currentLocation, URLDecoder.decode(uri.getPath(), "UTF-8"));
        if ((workflowContext.getOriginalLocation() != null)
            && !currentLocation.equals(workflowContext.getOriginalLocation())
            && !resolvedPath.getCanonicalPath().startsWith(currentLocation.getCanonicalPath())) {
            // we are outside the current workflow directory => use the original location in the server repository
            resolvedPath = new File(workflowContext.getOriginalLocation(), URLDecoder.decode(uri.getPath(), "UTF-8"));
        }

        // if resolved path is outside the workflow, check whether it is still inside the mountpoint
        if (!resolvedPath.getCanonicalPath().startsWith(currentLocation.getCanonicalPath())
            && (workflowContext.getMountpointRoot() != null)) {
            URI normalizedPath = resolvedPath.toURI().normalize();
            URI normalizedRoot = workflowContext.getMountpointRoot().toURI().normalize();

            if (!normalizedPath.toString().startsWith(normalizedRoot.toString())) {
                throw new IOException("Leaving the mount point is not allowed for workflow relative URLs: "
                    + resolvedPath.getAbsolutePath() + " is not in "
                    + workflowContext.getMountpointRoot().getAbsolutePath());
            }
        }
        return resolvedPath;
    }


    /**
     * Takes a node-relative knime URI (i.e. <tt>knime://knime.node/...</tt>) and resolves into its
     * corresponding file object.
     *
     * @param uri a node-relative URI
     * @return a file object
     * @throws IOException if the resolution fails because of a missing node context or if the URI leaves the workflow
     * @throws IllegalArgumentException if the URI is not a node-relative URI
     * @since 6.4
     */
    public static File resolveNodeRelativeUri(final URI uri) throws IOException {
        if (!ExplorerURLStreamHandler.NODE_RELATIVE.equalsIgnoreCase(uri.getHost())) {
            throw new IllegalArgumentException("Wrong magic hostname for node-relative URLs: " + uri.getHost());
        }

        NodeContext context = NodeContext.getContext();
        if (context == null) {
            throw new IOException("No context for node-relative URL available");
        }

        WorkflowContext workflowContext = context.getWorkflowManager().getContext();
        if (workflowContext == null) {
            throw new IOException("Workflow " + context.getWorkflowManager() + " does not have a context");
        }

        ReferencedFile nodeDirectoryRef = context.getNodeContainer().getNodeContainerDirectory();
        if (nodeDirectoryRef == null) {
            throw new IOException("Workflow must be saved before node-relative URLs can be used");
        }
        File resolvedPath =
            new File(nodeDirectoryRef.getFile().getAbsolutePath(), URLDecoder.decode(uri.getPath(), "UTF-8"));

        File currentLocation = workflowContext.getCurrentLocation();

        // check if resolved path leaves the workflow
        if (!resolvedPath.getCanonicalPath().startsWith(currentLocation.getCanonicalPath())) {
            throw new IOException("Leaving the workflow is not allowed for node-relative URLs: "
                + resolvedPath.getCanonicalPath() + " is not in " + currentLocation.getCanonicalPath());
        }
        return resolvedPath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File resolveToLocalOrTempFile(final URI uri, final IProgressMonitor monitor) throws IOException {
        File localFile = resolveToFile(uri, monitor);
        if (localFile != null) {
            return localFile;
        }

        // we have a remote file
        RemoteExplorerFileStore source = (RemoteExplorerFileStore)ExplorerFileSystem.INSTANCE.getStore(uri);
        if (source == null) {
            throw new IOException("Can't resolve file to URI \"" + uri
                + "\"; the corresponding mount point is probably " + "not defined or the resource has been (re)moved");
        }
        try {
            return source.resolveToLocalFile(monitor);
        } catch (CoreException e) {
            throw new IOException(e);
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
}
