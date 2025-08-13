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
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.knime.core.util.CoreConstants;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.hub.NamedItemVersion;
import org.knime.core.util.pathresolve.ResolverUtil;
import org.knime.core.util.pathresolve.SpaceVersion;
import org.knime.core.util.pathresolve.URIToFileResolve;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Redirect for backward compatibility to the OSGi service.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @deprecated use OSGi service directly
 */
@Deprecated(since = "9.3", forRemoval = true)
public class URIToFileResolveImpl implements URIToFileResolve {

    private static final ServiceTracker<URIToFileResolve, URIToFileResolve> SERVICE_TRACKER;

    static {
        final var coreBundle = FrameworkUtil.getBundle(ResolverUtil.class);
        if (coreBundle != null) {
            SERVICE_TRACKER = new ServiceTracker<>(coreBundle.getBundleContext(), URIToFileResolve.class, null);
            SERVICE_TRACKER.open();
        } else {
            SERVICE_TRACKER = null;
        }
    }

    private static URIToFileResolve getService() throws ResourceAccessException {
        if (SERVICE_TRACKER == null) {
            throw new ResourceAccessException("Core bundle is not active");
        }
        URIToFileResolve res = SERVICE_TRACKER.getService();
        if (res == null) {
            throw new ResourceAccessException("Can't resolve URI; no URI resolve service registered");
        }
        return res;
    }

    @Override
    public File resolveToFile(final URI uri) throws ResourceAccessException {
        return getService().resolveToFile(uri);
    }

    @Override
    public File resolveToLocalOrTempFile(final URI uri) throws ResourceAccessException {
        return getService().resolveToLocalOrTempFile(uri);
    }

    @Override
    public File resolveToFile(final URI uri, final IProgressMonitor monitor) throws ResourceAccessException {
        return getService().resolveToFile(uri, monitor);
    }

    @Override
    public File resolveToLocalOrTempFile(final URI uri, final IProgressMonitor monitor) throws ResourceAccessException {
        return getService().resolveToLocalOrTempFile(uri, monitor);
    }

    @Override
    public Optional<File> resolveToLocalOrTempFileConditional(final URI uri, final IProgressMonitor monitor,
        final ZonedDateTime ifModifiedSince) throws ResourceAccessException {
        return getService().resolveToLocalOrTempFileConditional(uri, monitor, ifModifiedSince);
    }

    /**
     * {@inheritDoc}
     *
     * @since 5.0
     */
    @Override
    public boolean isMountpointRelative(final URI uri) {
        return ExplorerFileSystem.SCHEME.equalsIgnoreCase(uri.getScheme())
            && CoreConstants.MOUNTPOINT_RELATIVE.equalsIgnoreCase(uri.getHost());
    }

    /**
     * {@inheritDoc}
     *
     * @since 5.0
     */
    @Override
    public boolean isWorkflowRelative(final URI uri) {
        return ExplorerFileSystem.SCHEME.equalsIgnoreCase(uri.getScheme())
            && CoreConstants.WORKFLOW_RELATIVE.equalsIgnoreCase(uri.getHost());
    }

    /**
     * {@inheritDoc}
     *
     * @since 6.4
     */
    @Override
    public boolean isNodeRelative(final URI uri) {
        return ExplorerFileSystem.SCHEME.equalsIgnoreCase(uri.getScheme())
            && CoreConstants.NODE_RELATIVE.equalsIgnoreCase(uri.getHost());
    }

    @Override
    public boolean isSpaceRelative(final URI uri) {
        return ExplorerFileSystem.SCHEME.equalsIgnoreCase(uri.getScheme())
                && CoreConstants.SPACE_RELATIVE.equalsIgnoreCase(uri.getHost());
    }

    @Override
    public Optional<KNIMEURIDescription> toDescription(final URI uri, final IProgressMonitor monitor) {
        try {
            return getService().toDescription(uri, monitor);
        } catch (ResourceAccessException e) {
            throw ExceptionUtils.asRuntimeException(e);
        }
    }

    @Override
    public Optional<List<SpaceVersion>> getSpaceVersions(final URI uri) throws Exception {
        try {
            return getService().getSpaceVersions(uri);
        } catch (ResourceAccessException e) {
            throw ExceptionUtils.asRuntimeException(e);
        }
    }

    /**
    * @deprecated use {@link #getHubItemVersionList(URI)}
    */
    @Override
    @Deprecated(since = "5.4")
    public List<NamedItemVersion> getHubItemVersions(final URI uri) {
        try {
            return getHubItemVersionList(uri);
        } catch (ResourceAccessException e) {
            throw ExceptionUtils.asRuntimeException(e);
        }
    }

    @Override
    public List<NamedItemVersion> getHubItemVersionList(final URI uri) throws ResourceAccessException {
        try {
            return getService().getHubItemVersionList(uri);
        } catch (ResourceAccessException e) {
            throw ExceptionUtils.asRuntimeException(e);
        }
    }
}
