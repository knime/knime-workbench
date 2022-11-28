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
 *   Nov 28, 2022 (leonard.woerteler): created
 */
package org.knime.workbench.explorer.urlresolve;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.core.runtime.URIUtil;
import org.knime.core.node.util.ClassUtils;
import org.knime.core.node.workflow.contextv2.HubSpaceLocationInfo;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.ui.node.workflow.RemoteWorkflowContext;
import org.knime.core.util.URIPathEncoder;

/**
 * KNIME URL Resolver for a remote executor in the Remote Workflow Editor.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 */
final class RemoteExecutorUrlResolver extends KnimeUrlResolver {

    private final RemoteWorkflowContext m_remoteContext;

    RemoteExecutorUrlResolver(final RemoteWorkflowContext remoteContext) {
        m_remoteContext = remoteContext;
    }

    @Override
    URL resolveMountpointAbsolute(final URL url) throws IOException {
        final var mountId = url.getAuthority();
        if (!m_remoteContext.getMountId().equals(mountId)) {
            throw new IOException("Unknown Mount ID on Remote Executor in URL '" + url + "'.");
        }

        final var hubLocationInfo = m_remoteContext.getWorkflowContextV2()
                .map(WorkflowContextV2::getLocationInfo)
                .flatMap(loc -> ClassUtils.castOptional(HubSpaceLocationInfo.class, loc));
        if (hubLocationInfo.isEmpty()) {
            // the rest is done by the ExplorerFileStore instance from the ExplorerMountTable
            return url;
        }

        // preserve space version of the absolute URL
        final var decodedPath = URIPathEncoder.decodePath(url);
        final var spaceVersion = getSpaceVersion(url);

        // since the version in absolute URLs is fixed, we get the correct item via the repository
        final var hubInfo = hubLocationInfo.get();
        final var spacePath = hubInfo.getSpacePath();
        final var spaceRepoUri = URIUtil.append(hubInfo.getRepositoryAddress(), spacePath);
        final var workflowAddress = hubInfo.getWorkflowAddress();
        final var workflowPath = hubInfo.getWorkflowPath();
        final var resolvedUri = HubExecutorUrlResolver.createSpaceRelativeRepoUri(workflowAddress, workflowPath,
            decodedPath, spacePath, spaceRepoUri, spaceVersion);
        return URIPathEncoder.UTF_8.encodePathSegments(resolvedUri.toURL());
    }

    @Override
    URI resolveMountpointRelative(final String decodedPath) throws IOException {
        return resolveSpaceRelative(decodedPath);
    }

    @Override
    URI resolveSpaceRelative(final String decodedPath) throws IOException {
        final var hubLocationInfo = m_remoteContext.getWorkflowContextV2()
                .map(WorkflowContextV2::getLocationInfo)
                .flatMap(loc -> ClassUtils.castOptional(HubSpaceLocationInfo.class, loc));
        if (hubLocationInfo.isEmpty()) {
            // server executor, Space == Mountpoint Root and no versions
            final var mpUri = m_remoteContext.getMountpointURI();
            final URI uri;
            try {
                uri = new URI(mpUri.getScheme(), mpUri.getHost(), decodedPath, null);
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
            return uri.normalize();
        }

        // Hub executor, resolve via the repository
        final var hubInfo = hubLocationInfo.get();
        final var spaceVersion =  hubInfo.getSpaceVersion().orElse(null);
        final var spacePath = hubInfo.getSpacePath();
        final var spaceRepoUri = URIUtil.append(hubInfo.getRepositoryAddress(), spacePath);
        final var workflowAddress = hubInfo.getWorkflowAddress();
        final var workflowPath = hubInfo.getWorkflowPath();
        return HubExecutorUrlResolver.createSpaceRelativeRepoUri(workflowAddress, workflowPath,
            decodedPath, spacePath, spaceRepoUri, spaceVersion);
    }

    @Override
    URI resolveWorkflowRelative(final String decodedPath) throws IOException {
        if (!leavesScope(decodedPath)) {
            throw new IOException("Workflow relative URL points to a resource within a workflow. Not accessible.");
        }

        final var mpURI = m_remoteContext.getMountpointURI();
        final URI mpURIWithoutQueryAndFragment;
        try {
            mpURIWithoutQueryAndFragment =
                new URI(mpURI.getScheme(), null, mpURI.getHost(), mpURI.getPort(), mpURI.getPath(), null, null);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        return URIUtil.append(mpURIWithoutQueryAndFragment, decodedPath).normalize();
    }

    @Override
    URI resolveNodeRelative(final String decodedPath) throws IOException {
        throw new IOException("Node relative URLs cannot be resolved from within purely remote workflows.");
    }
}
