/* ------------------------------------------------------------------
  * This source code, its documentation and all appendant files
  * are protected by copyright law. All rights reserved.
  *
  * Copyright by 
  * KNIME.com, Zurich, Switzerland
  *
  * You may not modify, publish, transmit, transfer or sell, reproduce,
  * create derivative works from, distribute, perform, display, or in
  * any way exploit any of the content, in whole or in part, except as
  * otherwise expressly permitted in writing by the copyright owner or
  * as specified in the license file distributed with this product.
  *
  * If you have any questions please contact the copyright holder:
  * website: www.knime.com
  * email: contact@knime.com
  * ---------------------------------------------------------------------
  *
  * History
  *   May 5, 2011 (morent): created
  */

package org.knime.workbench.explorer.view.preferences;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.ui.preferences.PreferenceConstants;
import org.osgi.framework.FrameworkUtil;

/**
 * Preference page for the Explorer containing the configured mount
 * points.
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class ExplorerPreferencePage extends FieldEditorPreferencePage
        implements IWorkbenchPreferencePage, IPreferenceChangeListener {
    /** The id of this preference page. */
    public static final String ID
            = "org.knime.workbench.explorer.view.explorer";
    private MountPointTableEditor m_mountEditor;
    private ComboFieldEditor m_linkTemplateEditor;

    /**
    *
    */
   public ExplorerPreferencePage() {
       super("KNIME Explorer Settings", null, GRID);
       setDescription("Setup mount points for usage in KNIME Explorer view.");
   }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final IWorkbench workbench) {
        setPreferenceStore(ExplorerActivator.getDefault().getPreferenceStore());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createFieldEditors() {
        m_mountEditor = new MountPointTableEditor(getFieldEditorParent());
        addField(m_mountEditor);

        m_linkTemplateEditor = new ComboFieldEditor(
                PreferenceConstants.P_EXPLORER_LINK_ON_NEW_TEMPLATE,
                "Link meta node when defining new template",
                new String[][] {
                        {"Always", MessageDialogWithToggle.ALWAYS},
                        {"Never", MessageDialogWithToggle.NEVER},
                        {"Prompt", MessageDialogWithToggle.PROMPT},
                }, getFieldEditorParent());
        addField(m_linkTemplateEditor);

        DefaultScope.INSTANCE.getNode(FrameworkUtil.getBundle(ExplorerActivator.class).getSymbolicName())
            .addPreferenceChangeListener(this);
    }

    /**
     * {@inheritDoc}
     * @since 6.3
     */
    @Override
    public void preferenceChange(final PreferenceChangeEvent event) {
        // The default preferences may change while this page is open (e.g. by an action on other preference page, e.g.
        // Novartis). If this editor is showing only default preference it will not upated its contents with the new
        // default preference and then store the old default preference as user settings once the preference dialog is
        // closed with OK. This listener updates the components when the default preferences change.

        if (PreferenceConstants.P_EXPLORER_MOUNT_POINT_XML.equals(event.getKey())) {
            m_mountEditor.load();
        } else if (PreferenceConstants.P_EXPLORER_LINK_ON_NEW_TEMPLATE.equals(event.getKey())) {
            m_linkTemplateEditor.load();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        super.dispose();
        DefaultScope.INSTANCE.getNode(FrameworkUtil.getBundle(ExplorerActivator.class).getSymbolicName())
            .removePreferenceChangeListener(this);
    }
}
