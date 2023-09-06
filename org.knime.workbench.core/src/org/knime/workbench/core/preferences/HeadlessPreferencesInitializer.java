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
 * -------------------------------------------------------------------
 */
package org.knime.workbench.core.preferences;

import static org.knime.workbench.core.WorkflowMigrationSettings.P_WORKFLOW_MIGRATION_NOTIFICATION_ENABLED;
import static org.knime.workbench.core.preferences.HeadlessPreferencesConstants.P_DATABASE_TIMEOUT;
import static org.knime.workbench.core.preferences.HeadlessPreferencesConstants.P_LOGLEVEL_CONSOLE;
import static org.knime.workbench.core.preferences.HeadlessPreferencesConstants.P_LOGLEVEL_LOG_FILE;
import static org.knime.workbench.core.preferences.HeadlessPreferencesConstants.P_LOGLEVEL_STDOUT;
import static org.knime.workbench.core.preferences.HeadlessPreferencesConstants.P_LOG_FILE_LOCATION;
import static org.knime.workbench.core.preferences.HeadlessPreferencesConstants.P_LOG_GLOBAL_IN_WF_DIR;
import static org.knime.workbench.core.preferences.HeadlessPreferencesConstants.P_MAXIMUM_THREADS;
import static org.knime.workbench.core.preferences.HeadlessPreferencesConstants.P_META_NODE_LINK_UPDATE_ON_LOAD;
import static org.knime.workbench.core.preferences.HeadlessPreferencesConstants.P_SEND_ANONYMOUS_STATISTICS;
import static org.knime.workbench.core.preferences.HeadlessPreferencesConstants.P_TEMP_DIR;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeLogger.LEVEL;
import org.knime.core.node.NodeLoggerConfig;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.workbench.core.KNIMECorePlugin;

/**
 * General KNIME preferences, not necessarily only for headless (e.g., P_META_NODE_LINK_UPDATE_ON_LOAD)
 *
 * @author Fabian Dill, University of Konstanz
 */
public class HeadlessPreferencesInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = KNIMECorePlugin.getDefault().getPreferenceStore();
        store.setDefault(P_MAXIMUM_THREADS, KNIMEConstants.DEF_MAX_THREAD_COUNT);

        store.setDefault(P_TEMP_DIR, System.getProperty("java.io.tmpdir"));

        final var logfileLevelRange = NodeLoggerConfig.getAppenderLevelRange(NodeLogger.LOGFILE_APPENDER);
        store.setDefault(P_LOGLEVEL_LOG_FILE, logfileLevelRange != null//
                ? logfileLevelRange.getFirst().name()//
                : LEVEL.WARN.name());

        final var stdoutLevelRange = NodeLoggerConfig.getAppenderLevelRange(NodeLogger.STDOUT_APPENDER);
        store.setDefault(P_LOGLEVEL_STDOUT, stdoutLevelRange != null//
                ? stdoutLevelRange.getFirst().name()//
                : LEVEL.INFO.name());

        final var consoleLevelRange = NodeLoggerConfig.getAppenderLevelRange(NodeLogger.KNIME_CONSOLE_APPENDER);
        store.setDefault(P_LOGLEVEL_CONSOLE, consoleLevelRange != null//
                ? consoleLevelRange.getFirst().name()//
                : LEVEL.WARN.name());

        store.setDefault(P_LOG_FILE_LOCATION, false);

        store.setDefault(P_LOG_GLOBAL_IN_WF_DIR, false);

        store.setDefault(P_SEND_ANONYMOUS_STATISTICS, false);

        //Since the deprecation of the legacy database framework the following two settings are migrated
        //to the new database preference page defined in the DBPreferencePage class which still stores the values
        //in the KNIMECorePlugin preferences store!!!
        int syspropTimeout = DatabaseConnectionSettings.getSystemPropertyDatabaseTimeout();
        store.setDefault(P_DATABASE_TIMEOUT, syspropTimeout >= 0 ? syspropTimeout : 15);
        store.setDefault(P_WORKFLOW_MIGRATION_NOTIFICATION_ENABLED, true);

        store.setDefault(P_META_NODE_LINK_UPDATE_ON_LOAD, "prompt");
    }

}
