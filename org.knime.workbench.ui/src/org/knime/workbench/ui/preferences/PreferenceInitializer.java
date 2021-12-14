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

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;
import org.knime.core.data.ExtensibleUtilityFactory;
import org.knime.core.data.TableBackendRegistry;
import org.knime.core.data.container.storage.TableStoreFormatRegistry;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * Class used to initialize default preference values.
 *
 * @author Florian Georg, University of Konstanz
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {
    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeDefaultPreferences() {
        // get the preference store for the UI plugin
        final IPreferenceStore store = KNIMEUIPlugin.getDefault().getPreferenceStore();

        store.setDefault(PreferenceConstants.P_HIDE_TIPS_AND_TRICKS, false);

        store.setDefault(PreferenceConstants.P_CONFIRM_RESET, true);

        store.setDefault(PreferenceConstants.P_CONFIRM_DELETE, true);

        store.setDefault(PreferenceConstants.P_CONFIRM_REPLACE, true);

        store.setDefault(PreferenceConstants.P_CONFIRM_RECONNECT, true);

        store.setDefault(PreferenceConstants.P_CONFIRM_EXEC_NODES_NOT_SAVED, true);

        store.setDefault(PreferenceConstants.P_CONFIRM_LOAD_NIGHTLY_BUILD_WORKFLOW, true);

        store.setDefault(PreferenceConstants.P_CONFIRM_PASSWORDS_IN_SETTINGS, true);

        store.setDefault(PreferenceConstants.P_EXEC_NODES_DATA_AWARE_DIALOGS, MessageDialogWithToggle.PROMPT);

        store.setDefault(PreferenceConstants.P_FAV_FREQUENCY_HISTORY_SIZE, 10);

        store.setDefault(PreferenceConstants.P_FAV_LAST_USED_SIZE, 10);

        store.setDefault(PreferenceConstants.P_DEFAULT_NODE_LABEL, "Node");

        final Display defaultDisplay = Display.getDefault();
        store.setDefault(PreferenceConstants.P_NODE_LABEL_FONT_SIZE, 8);
        // run this async as there were strange exception on windows, even if in swt thread, see bug 6401
        defaultDisplay.asyncExec(new Runnable() {
            @Override
            public void run() {
                Font systemFont = defaultDisplay.getSystemFont();
                FontData[] systemFontData = systemFont.getFontData();
                if (systemFontData.length >= 1) {
                    store.setDefault(PreferenceConstants.P_NODE_LABEL_FONT_SIZE, systemFontData[0].getHeight());
                }
            }
        });

        store.setDefault(PreferenceConstants.P_META_NODE_LINK_UPDATE_ON_LOAD,
                MessageDialogWithToggle.PROMPT);

        store.setDefault(PreferenceConstants.P_GRID_SHOW, Boolean.FALSE);
        store.setDefault(PreferenceConstants.P_GRID_SNAP_TO, Boolean.TRUE);
        store.setDefault(PreferenceConstants.P_GRID_SIZE_X, PreferenceConstants.P_GRID_DEFAULT_SIZE_X);
        store.setDefault(PreferenceConstants.P_GRID_SIZE_Y, PreferenceConstants.P_GRID_DEFAULT_SIZE_Y);

        store.setDefault(PreferenceConstants.P_CURVED_CONNECTIONS, PreferenceConstants.P_DEFAULT_CURVED_CONNECTIONS);
        store.setDefault(PreferenceConstants.P_CONNECTIONS_LINE_WIDTH, PreferenceConstants.P_DEFAULT_CONNECTION_LINE_WIDTH);

        store.setDefault(PreferenceConstants.P_AUTO_SAVE_ENABLE, PreferenceConstants.P_AUTO_SAVE_DEFAULT_ENABLE);
        store.setDefault(PreferenceConstants.P_AUTO_SAVE_INTERVAL,
            PreferenceConstants.P_AUTO_SAVE_DEFAULT_INTERVAL_SECS);
        store.setDefault(PreferenceConstants.P_AUTO_SAVE_DATA, PreferenceConstants.P_AUTO_SAVE_DEFAULT_WITH_DATA);

        store.setDefault(PreferenceConstants.P_WRAP_TABLE_HEADER, PreferenceConstants.P_WRAP_TABLE_HEADER_DEFAULT);
        store.setDefault(PreferenceConstants.P_ANNOTATION_BORDER_SIZE,
            PreferenceConstants.P_ANNOTATION_BORDER_SIZE_DEFAULT);

        store.setDefault(PreferenceConstants.P_REMOTE_WORKFLOW_EDITOR_AUTO_REFRESH,
            PreferenceConstants.P_DEFAULT_REMOTE_WORKFLOW_EDITOR_AUTO_REFRESH);
        store.setDefault(PreferenceConstants.P_REMOTE_WORKFLOW_EDITOR_AUTO_REFRESH_INTERVAL_MS,
            PreferenceConstants.P_DEFAULT_REMOTE_WORKFLOW_EDITOR_AUTO_REFRESH_INTERVAL_MS);
        store.setDefault(PreferenceConstants.P_REMOTE_WORKFLOW_EDITOR_EDITS_DISABLED,
            PreferenceConstants.P_DEFAULT_REMOTE_WORKFLOW_EDITOR_EDITS_DISABLED);
        store.setDefault(PreferenceConstants.P_REMOTE_WORKFLOW_EDITOR_TABLE_VIEW_CHUNK_SIZE,
            PreferenceConstants.P_DEFAULT_REMOTE_WORKFLOW_EDITOR_TABLE_VIEW_CHUNK_SIZE);
        store.setDefault(PreferenceConstants.P_REMOTE_WORKFLOW_EDITOR_CLIENT_TIMEOUT,
            PreferenceConstants.P_DEFAULT_REMOTE_WORKFLOW_EDITOR_CLIENT_TIMEOUT);

        store.setDefault(PreferenceConstants.P_EDITOR_ZOOM_LEVELS, PreferenceConstants.P_DEFAULT_EDITOR_ZOOM_LEVELS);
        store.setDefault(PreferenceConstants.P_EDITOR_ZOOM_MODIFIED_DELTA,
            PreferenceConstants.P_DEFAULT_EDITOR_ZOOM_MODIFIED_DELTA);

        store.setDefault(PreferenceConstants.P_EDITOR_SELECTED_NODE_HIGHLIGHT_CONNECTIONS,
            PreferenceConstants.P_DEFAULT_EDITOR_SELECTED_NODE_HIGHLIGHT_CONNECTIONS);
        PreferenceConverter.setDefault(store, PreferenceConstants.P_EDITOR_SELECTED_NODE_CONNECTIONS_HIGHLIGHT_COLOR,
            PreferenceConstants.P_DEFAULT_EDITOR_SELECTED_NODE_CONNECTIONS_HIGHLIGHT_COLOR);
        PreferenceConverter.setDefault(store, PreferenceConstants.P_EDITOR_SELECTED_NODE_FLOW_CONNECTION_HIGHLIGHT_COLOR,
            PreferenceConstants.P_DEFAULT_EDITOR_SELECTED_NODE_FLOW_CONNECTION_HIGHLIGHT_COLOR);
        store.setDefault(PreferenceConstants.P_EDITOR_SELECTED_NODE_CONNECTIONS_WIDTH_DELTA,
            PreferenceConstants.P_DEFAULT_EDITOR_SELECTED_NODE_CONNECTIONS_WIDTH_CHANGE);

        // TODO retrieve the utility factories from the data type extension point once we have it
        // this loads all registered renderers and initializes the default value
        for (final ExtensibleUtilityFactory fac : ExtensibleUtilityFactory.getAllFactories()) {
            fac.getDefaultRenderer(); // this sets the default preference for the renderer for this data type
        }
        TableStoreFormatRegistry.getInstance().getDefaultTableStoreFormat();
        TableBackendRegistry.initDefaultPreferences();
    }
}
