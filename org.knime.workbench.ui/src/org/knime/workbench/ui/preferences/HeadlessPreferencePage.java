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
 */
package org.knime.workbench.ui.preferences;

import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger.LEVEL;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.workbench.core.KNIMECorePlugin;
import org.knime.workbench.core.preferences.HeadlessPreferencesConstants;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 *
 * @author Fabian Dill, University of Konstanz
 */
public class HeadlessPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private boolean m_apply = false;

    private String m_tempPath;

    public HeadlessPreferencePage() {
        super(GRID);

        // get the preference store for the UI plugin
        final var store = KNIMECorePlugin.getDefault().getPreferenceStore();
        m_tempPath = store.getString(HeadlessPreferencesConstants.P_TEMP_DIR);
    }

    @Override
    protected void createFieldEditors() {
        // Specify the minimum log level for log file
        addField(new RadioGroupFieldEditor(HeadlessPreferencesConstants.P_LOGLEVEL_LOG_FILE, "Log File Log Level", 5,
            new String[][]{//
                {"&DEBUG", LEVEL.DEBUG.name()}, //
                {"&INFO", LEVEL.INFO.name()}, //
                {"&WARN", LEVEL.WARN.name()}, //
                {"&ERROR", LEVEL.ERROR.name()}, //
                {"&OFF", LEVEL.OFF.name()}},
            getFieldEditorParent()));

        addField(new BooleanFieldEditor(HeadlessPreferencesConstants.P_LOG_FILE_LOCATION, "Enable per workflow logs",
            getFieldEditorParent()));
        final var logDirGLobal = new BooleanFieldEditor(HeadlessPreferencesConstants.P_LOG_GLOBAL_IN_WF_DIR,
            "Log global messages also to workflow log", getFieldEditorParent());
        addField(logDirGLobal);

        // number threads
        final var maxThreadEditor = new IntegerFieldEditor(HeadlessPreferencesConstants.P_MAXIMUM_THREADS,
            "Maximum working threads for all nodes", getFieldEditorParent(), 3);
        maxThreadEditor.setValidRange(1, Math.max(100, Runtime.getRuntime().availableProcessors() * 4));
        maxThreadEditor.setTextLimit(3);
        addField(maxThreadEditor);

        // temp dir
        final var tempDirEditor = new TempDirFieldEditor(HeadlessPreferencesConstants.P_TEMP_DIR,
            "Directory for temporary files\n(you should restart KNIME after changing this value)",
            getFieldEditorParent());
        tempDirEditor.setEmptyStringAllowed(false);

        addField(tempDirEditor);

        addField(new HorizontalLineField(getFieldEditorParent()));
        addField(new LabelField(getFieldEditorParent(), "Improve KNIME", SWT.BOLD));
        addField(new LabelField(getFieldEditorParent(), "Help us improve KNIME by sending anonymous usage data."));
        addField(new LabelField(getFieldEditorParent(),
            "Click <a href=\"https://www.knime.com/faq#usage_data\">here</a> to find out what is being transmitted."));
        final var sendAnonymousStatisticsEditor =
            new BooleanFieldEditor(HeadlessPreferencesConstants.P_SEND_ANONYMOUS_STATISTICS, "Yes, help improve KNIME.",
                getFieldEditorParent());
        addField(sendAnonymousStatisticsEditor);

        addField(new HorizontalLineField(getFieldEditorParent()));

        addField(new LabelField(getFieldEditorParent(), "Component updates", SWT.BOLD));
        final var updateMetaNodeLinkOnLoadEditor =
            new ComboFieldEditor(HeadlessPreferencesConstants.P_META_NODE_LINK_UPDATE_ON_LOAD,
                "Update linked components when workflow loads", new String[][]{//
                    {"Always", MessageDialogWithToggle.ALWAYS}, //
                    {"Never", MessageDialogWithToggle.NEVER}, //
                    {"Prompt", MessageDialogWithToggle.PROMPT}},
                getFieldEditorParent());
        addField(updateMetaNodeLinkOnLoadEditor);
    }
    //TK_TODO: Enable disable the global messages in wf dir option depending on the wf option
    //
    //    /**
    //     * {@inheritDoc}
    //     */
    //    @Override
    //    public void propertyChange(final PropertyChangeEvent event) {
    //        if (HeadlessPreferencesConstants.P_LOG_FILE_LOCATION.equals(event.getProperty())) {
    //            final Boolean enabled = (Boolean)event.getNewValue();
    //            m_logDirGLobal.setEnabled(enabled, getFieldEditorParent());
    //        }
    //        super.propertyChange(event);
    //    }

    @Override
    protected void performApply() {
        m_apply = true;
        super.performApply();
    }

    /**
     * Overridden to display a message box in case the temp directory was changed.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean performOk() {
        final boolean result = super.performOk();

        checkChanges();

        return result;
    }

    /**
     * Overridden to react when the users applies but then presses cancel.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean performCancel() {
        final boolean result = super.performCancel();

        checkChanges();

        return result;
    }

    private void checkChanges() {
        final boolean apply = m_apply;
        m_apply = false;

        if (apply) {
            return;
        }

        // get the preference store for the UI plugin
        final var store = KNIMECorePlugin.getDefault().getPreferenceStore();
        final var currentTmpDir = store.getString(HeadlessPreferencesConstants.P_TEMP_DIR);
        boolean tempDirChanged = !m_tempPath.equals(currentTmpDir);
        if (tempDirChanged) {
            // reset the directory
            m_tempPath = currentTmpDir;
            final var message =
                "Changes of the temporary directory become first available after restarting the workbench.\n"
                    + "Do you want to restart the workbench now?";

            Display.getDefault().asyncExec(() -> promptRestartWithMessage(message));

        }
    }

    private static void promptRestartWithMessage(final String message) {
        final var mb = new MessageBox(SWTUtilities.getActiveShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
        mb.setText("Restart workbench...");
        mb.setMessage(message);
        if (mb.open() != SWT.YES) {
            return;
        }

        PlatformUI.getWorkbench().restart();
    }

    @Override
    public void init(final IWorkbench workbench) {
        // we use the pref store of the UI plugin
        setPreferenceStore(KNIMECorePlugin.getDefault().getPreferenceStore());

        // copy "update metanode" setting from ui plugin
        final var oldKey = PreferenceConstants.P_META_NODE_LINK_UPDATE_ON_LOAD;
        final var newKey = HeadlessPreferencesConstants.P_META_NODE_LINK_UPDATE_ON_LOAD;
        if (!getPreferenceStore().contains(newKey)) {
            final var uiPrefStore = KNIMEUIPlugin.getDefault().getPreferenceStore();
            if (uiPrefStore.contains(oldKey)) {
                getPreferenceStore().setValue(newKey, uiPrefStore.getString(oldKey));
            }
        }
    }

}
