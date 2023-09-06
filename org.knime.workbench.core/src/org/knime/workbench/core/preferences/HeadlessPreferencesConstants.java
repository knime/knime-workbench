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

import org.knime.core.node.workflow.NodeTimer;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public final class HeadlessPreferencesConstants {
    private HeadlessPreferencesConstants() { }

    /** Preference constant: log level for log file appender. */
    public static final String P_LOGLEVEL_LOG_FILE = "logging.loglevel.logfile";

    /** Preference constant: log level for stdout appender. */
    public static final String P_LOGLEVEL_STDOUT = "logging.loglevel.stdout";

    /** Preference constant: log level for console appender. */
    public static final String P_LOGLEVEL_CONSOLE = "logging.loglevel.console";

    /** Preference constant: log file location. */
    public static final String P_LOG_FILE_LOCATION = "logging.logfile.location";

    /** Preference constant: if the node id should be logged. */
    public static final String P_LOG_GLOBAL_IN_WF_DIR = "logging.logfile.logGlobalInWfDir";

    /** Preference constant: maximum threads to use. */
    public static final String P_MAXIMUM_THREADS = "knime.maxThreads";

    /** Preference constant: directory for temporary files. */
    public static final String P_TEMP_DIR = "knime.tempDir";

    /** Preference constant: send anonymous usage statistics to KNIME, yes or no. */
    public static final String P_SEND_ANONYMOUS_STATISTICS = NodeTimer.PREF_KEY_SEND_ANONYMOUS_STATISTICS;
    /** Preference constant: if KNIME already asked the user to transmit usage statistics, yes or no. */
    public static final String P_ASKED_ABOUT_STATISTICS = NodeTimer.PREF_KEY_ASKED_ABOUT_STATISTICS;

    /* --- Master Key constants --- */

    /** Preference constant if the master key dialog was opened. */
    public static final String P_MASTER_KEY_DEFINED
        = "knime.master_key.defined";
    /** Preference constant if a master key should be used. */
    public static final String P_MASTER_KEY_ENABLED
        = "knime.master.key.enabled";
    /** Preference constant to store the master key flag during a session. */
    public static final String P_MASTER_KEY_SAVED = "knime.master_key.saved";
    /** Preference constant to store the master key during a session. */
    public static final String P_MASTER_KEY = "knime.master_key";

    /* --- Database settings constants --- */

    /** Preference constant to store loaded database driver files. */
    public static final String P_DATABASE_DRIVERS = "database_drivers";

    /** Preference constant to store loaded database driver files. */
    public static final String P_DATABASE_TIMEOUT = "database_timeout";

    /* --- Other --- */

    /** Preference constant for whether meta node links should be updated on workflow load. */
    public static final String P_META_NODE_LINK_UPDATE_ON_LOAD = "knime.metanode.updateOnLoad";
}
