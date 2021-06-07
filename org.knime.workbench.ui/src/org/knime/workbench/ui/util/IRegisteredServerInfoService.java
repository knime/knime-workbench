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
 *   May 31, 2021 (wiswedel): created
 */
package org.knime.workbench.ui.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.knime.core.util.Version;
import org.knime.workbench.ui.p2.actions.InvokeUpdateAction;

/**
 * Used by {@link InvokeUpdateAction} to inform the user that an upgrade may possibly break connectivity with connected
 * KNIME Server (server executors older than AP version after upgrade), see AP-16035.
 *
 * @author wiswedel
 * @noreference Not to be implemented or used by code outside of KNIME core framework.
 */
public interface IRegisteredServerInfoService {

    public List<ServerAndExecutorVersions> getServerAndVersionInfos();

    /**
     * Class containing KNIME Server and KNIME Executor versions.
     */
    public static class ServerAndExecutorVersions {
        private static final Version KS_413 = new Version(4,13,0);

        private final String m_mountId;

        private final Version m_serverVersion;

        private Set<Version> m_executorVersion = new HashSet<>();

        private Set<Version> m_analyticsPlatformVersion = new HashSet<>();

        /**
         * Creates a new object.
         *
         * @param mountId the mount id of the KNIME Server
         * @param serverVersion the version of the KNIME Server
         * @param executorVersions the versions of the executors known by the server
         * @param analyticsPlatformVersions the versions of the Analytics Platform of the executors
         */
        public ServerAndExecutorVersions(final String mountId, final Version serverVersion,
            final Set<Version> executorVersions, final Set<Version> analyticsPlatformVersions) {
            m_mountId = mountId;
            m_serverVersion = serverVersion;
            m_executorVersion.addAll(executorVersions);
            m_analyticsPlatformVersion.addAll(analyticsPlatformVersions);
        }

        /**
         * Returns the mount id of the KNIME Server.
         *
         * @return the mount id
         */
        public String getServerMountId() {
            return m_mountId;
        }

        /**
         * Returns the known executor versions of the KNIME Server.
         *
         * @return the versions
         */
        public Set<Version> getExecutorVersions() {
            return m_executorVersion;
        }

        /**
         * Returns the known Analytics Platform versions of the executors of the KNIME Server.
         *
         * @return the versions
         */
        public Set<Version> getAnalyticsPlatformVersions() {
            return m_analyticsPlatformVersion;
        }

        /**
         * Returns the server version
         *
         * @return the version
         */
        public Version getServerVersion() {
            return m_serverVersion;
        }

        /**
         * Checks if the server is older than version 4.13.0 which introduced this feature.
         *
         * @return <code>true</code> if the version is older than 4.13.0, <code>false</code> otherwise
         */
        public boolean isServerOlderThan413() {
            return !m_serverVersion.isSameOrNewer(KS_413);
        }

        /**
         * Compares the known Analytics Platform versions of the executor with the provided client version.
         *
         * @param clientVersion the Analytics Platform version of the client
         * @return <code>true</code> if the server contains at least one executor that is older than the provided
         *         Analytics Platform version, <code>false</code> otherwise
         */
        public boolean hasAnalyticsPlatformOlderThan(final Version clientVersion) {
            return m_analyticsPlatformVersion.stream()
                .anyMatch(v -> clientVersion.isSameOrNewer(v) && !v.isSameOrNewer(clientVersion));
        }

    }
}
