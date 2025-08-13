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
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

import org.knime.core.util.CoreConstants;
import org.knime.core.util.exception.ResourceAccessException;
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
 * @deprecated moved to org.knime.core.internal.knimeurl.ExplorerURLStreamHandler
 */
@Deprecated(since = "9.3", forRemoval = true)
public class ExplorerURLStreamHandler extends AbstractURLStreamHandlerService {
    /**
     * The magic hostname for workflow-relative URLs.
     *
     * @since 5.0
     */
    public static final String WORKFLOW_RELATIVE = CoreConstants.WORKFLOW_RELATIVE;

    /**
     * The magic hostname for mountpoint-relative URLs.
     *
     * @since 5.0
     */
    public static final String MOUNTPOINT_RELATIVE = CoreConstants.MOUNTPOINT_RELATIVE;

    /**
     * The magic hostname for node-relative URLs.
     *
     * @since 6.4
     */
    public static final String NODE_RELATIVE = CoreConstants.NODE_RELATIVE;

    /**
     * The magic hostname for space-relative URLs.
     *
     * @since 8.9
     */
    public static final String SPACE_RELATIVE = CoreConstants.SPACE_RELATIVE;

    @Override
    public URLConnection openConnection(final URL url) throws IOException {
        return new org.knime.core.internal.knimeurl.ExplorerURLStreamHandler().openConnection(url);
    }

    @Override
    public URLConnection openConnection(final URL url, final Proxy p) throws IOException {
        return new org.knime.core.internal.knimeurl.ExplorerURLStreamHandler().openConnection(url, p);
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
        return org.knime.core.internal.knimeurl.ExplorerURLStreamHandler.resolveKNIMEURL(url);
    }
}
