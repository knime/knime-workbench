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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

import org.eclipse.core.runtime.URIUtil;
import org.knime.core.node.workflow.contextv2.ServerJobExecutorInfo;
import org.knime.core.node.workflow.contextv2.ServerLocationInfo;
import org.knime.core.util.URIPathEncoder;

/**
 * KNIME URL Resolver for a Server executor.
 *
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 */
final class ServerExecutorUrlResolver extends KnimeUrlResolver {

    private final ServerJobExecutorInfo m_executorInfo;

    private final ServerLocationInfo m_locationInfo;

    ServerExecutorUrlResolver(final ServerJobExecutorInfo executorInfo, final ServerLocationInfo locationInfo) {
        m_executorInfo = executorInfo;
        m_locationInfo = locationInfo;
    }

    @Override
    URL resolveMountpointAbsolute(final URL url) throws IOException {
        final var mountId = url.getAuthority();
        if (!m_locationInfo.getDefaultMountId().equals(mountId)) {
            throw new IOException("Unknown Mount ID on Server Executor in URL '" + url + "'.");
        }
        final var uri = resolveMountpointRelative(URIPathEncoder.decodePath(url));
        return URIPathEncoder.UTF_8.encodePathSegments(uri.toURL());
    }

    @Override
    URI resolveMountpointRelative(final String decodedPath) throws IOException {
        // legacy Servers don't have spaces, resolve directly against repo root
        final var repositoryAddress = m_locationInfo.getRepositoryAddress().normalize();
        return URIUtil.append(repositoryAddress, decodedPath + ":data").normalize();
    }

    @Override
    URI resolveSpaceRelative(final String decodedPath) throws IOException {
        return resolveMountpointRelative(decodedPath);
    }

    @Override
    URI resolveWorkflowRelative(final String decodedPath) throws IOException {
        if (leavesScope(decodedPath)) {
            // we're on a server executor, resolve against the repository
            final var repositoryAddress = m_locationInfo.getRepositoryAddress().normalize();
            final var uri = URIUtil.append(repositoryAddress,
                m_locationInfo.getWorkflowPath() + "/" + decodedPath + ":data");
            return uri.normalize();
        }

        // file inside the workflow
        final var currentLocation = m_executorInfo.getLocalWorkflowPath();
        final var resolvedFile = new File(currentLocation.toFile(), decodedPath);
        if (!resolvedFile.getCanonicalPath().startsWith(currentLocation.toFile().getCanonicalPath())) {
                throw new IOException("Path component of workflow relative URLs leaving the workflow must start with "
                    + "'/..', found '" + decodedPath + "'.");
        }
        return resolvedFile.toURI();
    }

    @Override
    URI resolveNodeRelative(final String decodedPath) throws IOException {
        return defaultResolveNodeRelative(decodedPath, m_executorInfo.getLocalWorkflowPath());
    }
}
