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

import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger.LEVEL;
import org.knime.workbench.core.KNIMECorePlugin;
import org.knime.workbench.core.preferences.HeadlessPreferencesConstants;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * This class represents a preference page that is contributed to the Preferences dialog. By subclassing
 * <samp>FieldEditorPreferencePage</samp>, we can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the preference store that belongs to the main
 * plug-in class. That way, preferences can be accessed directly via the preference store.
 *
 * @author Florian Georg, University of Konstanz
 */
public class MainPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private RadioGroupFieldEditor m_consoleLogEditor;

    private IntegerFieldEditor m_autoSaveIntervalEditor;

    private BooleanFieldEditor m_autoSaveWithDataEditor;

    /**
     * Constructor.
     */
    public MainPreferencePage() {
        super(GRID);
    }

    /**
     * Creates the field editors. Field editors are abstractions of the common GUI blocks needed to manipulate various
     * types of preferences. Each field editor knows how to save and restore itself.
     */
    @Override
    public void createFieldEditors() {
        // Specify the minimum log level for the console
        m_consoleLogEditor = new RadioGroupFieldEditor(HeadlessPreferencesConstants.P_LOGLEVEL_CONSOLE,
            "Console View Log Level", 4, new String[][]{//
                {"&DEBUG", LEVEL.DEBUG.name()}, //
                {"&INFO", LEVEL.INFO.name()}, //
                {"&WARN", LEVEL.WARN.name()}, //
                {"&ERROR", LEVEL.ERROR.name()}},
            getFieldEditorParent());
        addField(m_consoleLogEditor);

        addField(new HorizontalLineField(getFieldEditorParent()));
        addField(
            new BooleanFieldEditor(PreferenceConstants.P_CONFIRM_RESET, "Confirm Node Reset", getFieldEditorParent()));
        addField(new BooleanFieldEditor(PreferenceConstants.P_CONFIRM_DELETE, "Confirm Node/Connection Deletion",
            getFieldEditorParent()));
        addField(new BooleanFieldEditor(PreferenceConstants.P_CONFIRM_REPLACE,
            "Confirm Node/Connection Replacement/Interruption", getFieldEditorParent()));
        addField(new BooleanFieldEditor(PreferenceConstants.P_CONFIRM_RECONNECT,
            "Confirm reconnection of already connected nodes", getFieldEditorParent()));
        addField(new BooleanFieldEditor(PreferenceConstants.P_CONFIRM_EXEC_NODES_NOT_SAVED,
            "Confirm if executing nodes are not saved", getFieldEditorParent()));
        addField(new BooleanFieldEditor(PreferenceConstants.P_CONFIRM_LOAD_NIGHTLY_BUILD_WORKFLOW,
            "Confirm when loading workflows created by a nightly build", getFieldEditorParent()));

        // added with AP-15442 -- don't bother user with this unless this property is set
        if (Node.DISALLOW_WEAK_PASSWORDS_IN_NODE_CONFIGURATION) {
            addField(new BooleanFieldEditor(PreferenceConstants.P_CONFIRM_PASSWORDS_IN_SETTINGS,
                "Confirm when storing (weakly encrypted) passwords in node configurations", getFieldEditorParent()));
        }

        final var dataAwareExecutePromptEditor =
            new ComboFieldEditor(PreferenceConstants.P_EXEC_NODES_DATA_AWARE_DIALOGS,
                "Execute upstream nodes when needed", new String[][]{//
                    {"Always", MessageDialogWithToggle.ALWAYS}, //
                    {"Never", MessageDialogWithToggle.NEVER}, //
                    {"Prompt", MessageDialogWithToggle.PROMPT}},
                getFieldEditorParent());
        addField(dataAwareExecutePromptEditor);

        addField(new HorizontalLineField(getFieldEditorParent()));
        final var enableAutoSaveBooleanField = new BooleanFieldEditor(PreferenceConstants.P_AUTO_SAVE_ENABLE,
            "Auto Save open workflows", getFieldEditorParent()) {
            @Override
            protected void valueChanged(final boolean old, final boolean neu) {
                m_autoSaveIntervalEditor.setEnabled(neu, getFieldEditorParent());
                m_autoSaveWithDataEditor.setEnabled(neu, getFieldEditorParent());
            }
        };
        m_autoSaveIntervalEditor = new IntegerFieldEditor(PreferenceConstants.P_AUTO_SAVE_INTERVAL,
            "Auto-Save Interval (in secs)", getFieldEditorParent());
        m_autoSaveWithDataEditor =
            new BooleanFieldEditor(PreferenceConstants.P_AUTO_SAVE_DATA, "Save with data", getFieldEditorParent());
        addField(enableAutoSaveBooleanField);
        addField(m_autoSaveIntervalEditor);
        addField(m_autoSaveWithDataEditor);

        addField(new HorizontalLineField(getFieldEditorParent()));
        addField(new BooleanFieldEditor(PreferenceConstants.P_WRAP_TABLE_HEADER, "Wrap Column Header in Table Views",
            getFieldEditorParent()));
        addField(new IntegerFieldEditor(PreferenceConstants.P_ANNOTATION_BORDER_SIZE,
            "Workflow Annotation border size (in px)", getFieldEditorParent()));

        addField(new HorizontalLineField(getFieldEditorParent()));
        addField(new BooleanFieldEditor(PreferenceConstants.P_OMIT_MISSING_BROWSER_WARNING,
            "Suppress warnings about missing browser integration", getFieldEditorParent()));

        addField(new HorizontalLineField(getFieldEditorParent()));
        addField(new LabelField(getFieldEditorParent(), "Settings for the 'Favorite Nodes' view"));
        final var freqHistorySizeEditor = new IntegerFieldEditor(PreferenceConstants.P_FAV_FREQUENCY_HISTORY_SIZE,
            "Maximal size for most frequently used nodes", getFieldEditorParent(), 3);
        freqHistorySizeEditor.setValidRange(1, 50);
        freqHistorySizeEditor.setTextLimit(3);
        freqHistorySizeEditor.load();
        final var usedHistorySizeEditor = new IntegerFieldEditor(PreferenceConstants.P_FAV_LAST_USED_SIZE,
            "Maximal size for last used nodes", getFieldEditorParent(), 3);
        usedHistorySizeEditor.setValidRange(1, 50);
        usedHistorySizeEditor.setTextLimit(3);
        usedHistorySizeEditor.load();
        addField(usedHistorySizeEditor);
        addField(freqHistorySizeEditor);
    }

    @Override
    public void init(final IWorkbench workbench) {
        // we use the pref store of the UI plugin
        setPreferenceStore(KNIMEUIPlugin.getDefault().getPreferenceStore());
    }

    @Override
    protected void initialize() {
        super.initialize();
        m_consoleLogEditor.setPreferenceStore(KNIMECorePlugin.getDefault().getPreferenceStore());
        m_consoleLogEditor.load();
    }

    @Override
    protected void performDefaults() {
        super.performDefaults();
        m_consoleLogEditor.setPreferenceStore(KNIMECorePlugin.getDefault().getPreferenceStore());
        m_consoleLogEditor.loadDefault();
    }
}
