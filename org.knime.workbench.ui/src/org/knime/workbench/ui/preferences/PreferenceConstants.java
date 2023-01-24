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
 *
 */
package org.knime.workbench.ui.preferences;

import org.eclipse.swt.graphics.RGB;

/**
 * Constant definitions for plug-in preferences. Values are stored under these
 * keys in the <code>PreferenceStore</code> of the UI plugin.
 *
 * @author Florian Georg, University of Konstanz
 */
public interface PreferenceConstants {
    /** Preference constant: whether the tips&amp;tricks window should be hidden.
     */
    public static final String P_HIDE_TIPS_AND_TRICKS = "knime.hidetipsandtricks";

    /** Preference constant: whether user needs to confirm reset actions. */
    public static final String P_CONFIRM_RESET = "knime.confirm.reset";

    /** Preference constant: whether user needs to confirm delete actions. */
    public static final String P_CONFIRM_DELETE = "knime.confirm.delete";

    /** Preference constant: whether user needs to confirm node/connection replacement by drop-on actions. */
    public static final String P_CONFIRM_REPLACE = "knime.confirm.replace";

    /** Preference constant to confirm reconnecting a node. */
    public static final String P_CONFIRM_RECONNECT = "knime.confirm.reconnect";

    /** Preference constant to confirm executing nodes not saved on close. */
    public static final String P_CONFIRM_EXEC_NODES_NOT_SAVED = "knime.confirm.exec_nodes_not_saved";

    /** Preference constant to (not) warn when loading nightly build workflow.
     * @since 3.5 */
    public static final String P_CONFIRM_LOAD_NIGHTLY_BUILD_WORKFLOW = "knime.confirm.nightlybuildflow";

    /** Preference constant to execute upstream nodes for nodes w/ data aware dialog (ALWAYS, NEVER, PROMPT). */
    public static final String P_EXEC_NODES_DATA_AWARE_DIALOGS = "knime.exec_nodes_for_data_aware_dialogs";

    /** Preference constant to (not) warn when storing passwords in node configurations and
     * {@link org.knime.core.node.KNIMEConstants#PROPERTY_WEAK_PASSWORDS_IN_SETTINGS_FORBIDDEN} is set. */
    public static final String P_CONFIRM_PASSWORDS_IN_SETTINGS = "knime.confirm.passwords_in_settings";

    /** Preference constant to (not) warn when creating a template link to "Current State" on Hub. */
    public static final String P_CONFIRM_LINK_CURRENT_STATE = "knime.confirm.link_current_state";

    /** Preference constant for the size of the favorite nodes frequency
     * history size.
     */
    public static final String P_FAV_FREQUENCY_HISTORY_SIZE =
        "knime.favorites.frequency";

    /** Preference constant for the size of the favorite nodes last used
     * history size.
     */
    public static final String P_FAV_LAST_USED_SIZE =
        "knime.favorites.lastused";

    /** Preference constant to allow setting empty node label. */
    public static final String P_SET_NODE_LABEL = "knime.set.node_label";

    /** Preference constant for default node label prefix. */
    public static final String P_DEFAULT_NODE_LABEL =
        "knime.default.node_label";

    /** Preference constant for node name and node label to change font size. */
    public static final String P_NODE_LABEL_FONT_SIZE =
        "knime.node.font_size";

    /** Preference constant for whether meta node links should be updated on
     * workflow load. */
    public static final String P_META_NODE_LINK_UPDATE_ON_LOAD =
        "knime.metanode.updateOnLoad";

    /** Preference constant for mount points for the Explorer. */
    @Deprecated
    public static final String P_EXPLORER_MOUNT_POINT =
        "knime.explorer.mountpoint";

    /** Preference constant for mount points for the Explorer (xml format). */
    public static final String P_EXPLORER_MOUNT_POINT_XML =
        "knime.explorer.mountpoint.xml";

    /** Pref constant to link the original meta node to a newly
     * defined template. */
    public static final String P_EXPLORER_LINK_ON_NEW_TEMPLATE =
        "knime.explorer.link_on_new_template";

    /** Pref constant whether to show grid in workflow editor (boolean). */
    public static final String P_GRID_SHOW = "knime.showgrid";

    /** Pref constant whether to snap to grid. */
    public static final String P_GRID_SNAP_TO = "knime.snaptogrid";

    /** Pref constant for grid size (number of pixels). */
    public static final String P_GRID_SIZE_X = "knime.gridsize.x";
    /** Pref constant for grid size (number of pixels). */
    public static final String P_GRID_SIZE_Y = "knime.gridsize.y";
    /** Default grid distance. */
    public static final int P_GRID_DEFAULT_SIZE_X = 20;
    /** Default grid distance. */
    public static final int P_GRID_DEFAULT_SIZE_Y = 20;

    /** Pref constant whether nodes connections should be drawn curved. */
    public static final String P_CURVED_CONNECTIONS = "knime.curvedconnections";
    /** Pref constant for the line width of node connections. */
    public static final String P_CONNECTIONS_LINE_WIDTH = "knime.connectionslinewidth";
    /** Default value for the curved connection property.  */
    public static final boolean P_DEFAULT_CURVED_CONNECTIONS = true;
    /** Default value for the connection line width. */
    public static final int P_DEFAULT_CONNECTION_LINE_WIDTH = 2;

    /** Pref constant whether to enable auto save for workflows. */
    public static final String P_AUTO_SAVE_ENABLE = "knime.autosave.enable";
    /** Pref constant auto save interval (in secs). */
    public static final String P_AUTO_SAVE_INTERVAL = "knime.autosave.interval";
    /** Pref constant whether to save data with auto-save. */
    public static final String P_AUTO_SAVE_DATA = "knime.autosave.data";
    /** Default auto save enablement. */
    public static final boolean P_AUTO_SAVE_DEFAULT_ENABLE = false;
    /** Default auto save interval in seconds ({@value #P_AUTO_SAVE_DEFAULT_INTERVAL_SECS}). */
    public static final int P_AUTO_SAVE_DEFAULT_INTERVAL_SECS = 180;
    /** Default auto save data property: true = save with data (default is {@value #P_AUTO_SAVE_DEFAULT_WITH_DATA}). */
    public static final boolean P_AUTO_SAVE_DEFAULT_WITH_DATA = false;

    /** Pref constant to wrap column headers in table views (interactive table view & outport view). */
    public static final String P_WRAP_TABLE_HEADER = "knime.table.header.wrap";
    /** Default to {@link #P_WRAP_TABLE_HEADER} ({@value #P_WRAP_TABLE_HEADER_DEFAULT}). */
    public static final boolean P_WRAP_TABLE_HEADER_DEFAULT = false;

    /** Pref key for annotation border size in pixel. */
    public static final String P_ANNOTATION_BORDER_SIZE = "knime.workflow.annotation.bordersize";
    /** Default to {@link #P_ANNOTATION_BORDER_SIZE} ({@value #P_ANNOTATION_BORDER_SIZE_DEFAULT}). */
    public static final int P_ANNOTATION_BORDER_SIZE_DEFAULT = 10;

    /** Preference constant for omitting the missing browser warning during startup. */
    public static final String P_OMIT_MISSING_BROWSER_WARNING = "knime.ui.omit-missing-browser-warning";

    /** Preference constant for enabling/disabling the auto-refresh for the remote workflow editor */
    public static final String P_REMOTE_WORKFLOW_EDITOR_AUTO_REFRESH = "knime.remoteworkfloweditor.autorefresh.enable";
    /** Preference constant for the auto-workflow-refresh interval in milliseconds */
    public static final String P_REMOTE_WORKFLOW_EDITOR_AUTO_REFRESH_INTERVAL_MS =
        "knime.remoteworkfloweditor.autorefresh.interval";
    /** Preference constant for the enabling/disabling remote workflow edits. */
    public static final String P_REMOTE_WORKFLOW_EDITOR_EDITS_DISABLED = "knime.remoteworkfloweditor.edits.disabled";
    /** Preference constant for the size of chunks retrieved when lazily loading data for a node's table view */
    public static final String P_REMOTE_WORKFLOW_EDITOR_TABLE_VIEW_CHUNK_SIZE =
        "knime.remoteworkfloweditor.edits.tableviewchunksize";
    /** Default whether auto-refresh is enabled */
    public static final boolean P_DEFAULT_REMOTE_WORKFLOW_EDITOR_AUTO_REFRESH = true;
    /** Default milliseconds the auto-refresh interval for workflows */
    public static final long P_DEFAULT_REMOTE_WORKFLOW_EDITOR_AUTO_REFRESH_INTERVAL_MS = 1000;
    /** Default whether remote workflow edits are enabled */
    public static final boolean P_DEFAULT_REMOTE_WORKFLOW_EDITOR_EDITS_DISABLED = false;
    /** Default remote table view chunk size */
    public static final int P_DEFAULT_REMOTE_WORKFLOW_EDITOR_TABLE_VIEW_CHUNK_SIZE = 100;
    /**
     * Preference constant for client timeout in milliseconds
     *
     * @since 4.3
     */
    public static final String P_REMOTE_WORKFLOW_EDITOR_CLIENT_TIMEOUT = "knime.remoteworkfloweditor.client.timeout";
    /**
     * Default value for client timeout in milliseconds
     *
     * @since 4.3
     */
    public static final int P_DEFAULT_REMOTE_WORKFLOW_EDITOR_CLIENT_TIMEOUT = 60000;


    /** Preference constant for the comma delimited list of zoom levels */
    public static final String P_EDITOR_ZOOM_LEVELS = "knime.zoom.levels";
    /** The default value for editor zoom levels */
    public static final String P_DEFAULT_EDITOR_ZOOM_LEVELS =
        "10, 25, 33, 50, 67, 75, 90, 100, 110, 125, 150, 175, 200, 250";
    /** Preference constant for the value of how much zoom changes when the modifier keys are held down */
    public static final String P_EDITOR_ZOOM_MODIFIED_DELTA = "knime.zoom.alternate_delta";
    /** The default value for the delta in modifier zoom change */
    public static final int P_DEFAULT_EDITOR_ZOOM_MODIFIED_DELTA = 5;

    /**
     * Preference constant for the comma delimited list of custom colors (chosen in the native color picker
     *  by Windows users from the annotation toolbar.)
     */
    public static final String P_CUSTOM_COLORS = "knime.colors.custom";

    /** Preference constant as to whether a selected node highlights its immediate connections */
    public static final String P_EDITOR_SELECTED_NODE_HIGHLIGHT_CONNECTIONS
                                                = "knime.node.highlight_connections_on_selection";
    /** Default value for whether a selected node highlights its immediate connections  */
    public static final boolean P_DEFAULT_EDITOR_SELECTED_NODE_HIGHLIGHT_CONNECTIONS = true;
    /** Preference constant for the selected node input & output connection lines highlight color */
    public static final String P_EDITOR_SELECTED_NODE_CONNECTIONS_HIGHLIGHT_COLOR
                                                = "knime.colors.selected_node_connection_highlight";
    /** The default value for the selected node input & output connection lines highlight color */
    public static final RGB P_DEFAULT_EDITOR_SELECTED_NODE_CONNECTIONS_HIGHLIGHT_COLOR = new RGB(75, 75, 75);
    /** Preference constant for the selected node flow variable connection lines highlight color */
    public static final String P_EDITOR_SELECTED_NODE_FLOW_CONNECTION_HIGHLIGHT_COLOR
                                            = "knime.colors.selected_node_flow_connection_highlight";
    /** The default value for the selected node flow variable connection lines highlight color */
    public static final RGB P_DEFAULT_EDITOR_SELECTED_NODE_FLOW_CONNECTION_HIGHLIGHT_COLOR = new RGB(212, 60, 52);
    /** Preference constant for the selected node input & output connection lines highlight width change stored as int */
    public static final String P_EDITOR_SELECTED_NODE_CONNECTIONS_WIDTH_DELTA
                                                = "knime.colors.selected_node_connection_width_delta";
    /** The default value for the selected node input & output connection lines highlight width change */
    public static final int P_DEFAULT_EDITOR_SELECTED_NODE_CONNECTIONS_WIDTH_CHANGE = 1;


    /** Preference constant for showing a warning dialog when connecting to a server via EJB */
    public static final String P_SHOW_EJB_WARNING_DIALOG = "knime.explorer.show_EJB_warning";
    /** The default value for whether a warning dialog should appear when connecting to a server via EJB or not */
    public static final boolean P_DEFAULT_SHOW_EJB_WARNING_DIALOG = true;

    /** Preference constant for showing a warning dialog when connecting to an older server */
    public static final String P_SHOW_OLDER_SERVER_WARNING_DIALOG = "knime.explorer.show_older_server_warning";
    /** The default value for whether a warning dialog should appear when connecting to an older server or not */
    public static final boolean P_DEFAULT_SHOW_OLDER_SERVER_WARNING_DIALOG = true;
}
