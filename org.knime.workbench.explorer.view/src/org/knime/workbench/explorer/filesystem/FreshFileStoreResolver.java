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
 */
package org.knime.workbench.explorer.filesystem;

import java.net.URI;

/**
 * Resolve a KNIME URI to an {@link AbstractExplorerFileStore}. This is in principle not a good idea since the latter
 * will be deprecated. For the time being, it is still required to establish compatibility with some legacy code until
 * it is rewritten (see usages).
 * <p>
 * It is important to note that with AP-23529, the {@link ExplorerFileSystem} and any related
 * {@link AbstractExplorerFileStore}s are no longer always refreshed automatically. If not, the file system has to be
 * refreshed before resolving. This is an expensive operation involving network I/O and should be avoided where
 * possible.
 *
 * @noreference This class is not intended to be referenced by clients.
 * @since 8.14
 * @deprecated this resolver does not refresh remotes anymore, because the repository fetchers keep content up to date
 *             again
 */
@Deprecated(forRemoval = true)
public final class FreshFileStoreResolver {

    private FreshFileStoreResolver() {

    }

    public static AbstractExplorerFileStore resolve(final URI knimeUri) {
        return ExplorerFileSystem.INSTANCE.getStore(knimeUri);
    }

    /**
     * Refresh a single item.
     *
     * @param knimeURI The KNIME URI of the item.
     * @return
     */
    public static AbstractExplorerFileStore resolveAndRefreshWithProgress(final URI knimeURI) {
        var fileStore = resolve(knimeURI);
        if (!(fileStore instanceof RemoteExplorerFileStore remoteFileStore)) {
            return fileStore; // not remote, nothing to refresh
        }
        // no manual refresh necessary, repository fetchers keep everything up to date again
        return remoteFileStore;
    }

    /**
     * Refresh remote non-local content providers corresponding to the given mount IDs. Progress is displayed as one
     * Eclipse workbench job per provider, all belonging to a job family. Call blocks until all jobs are finished or
     * cancelled.
     */
    public static void refreshContentProvidersWithProgress(final String... mountIds) {
        // no manual refresh necessary, repository fetchers keep everything up to date again
    }

}
