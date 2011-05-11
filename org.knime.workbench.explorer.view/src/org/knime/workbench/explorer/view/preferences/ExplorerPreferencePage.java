/* ------------------------------------------------------------------
  * This source code, its documentation and all appendant files
  * are protected by copyright law. All rights reserved.
  *
  * Copyright, 2008 - 2011
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

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.knime.workbench.core.KNIMECorePlugin;
import org.knime.workbench.explorer.ExplorerMountTable;

/**
 * Preference page for the Spaces Explorer containing the configured mount
 * points.
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class ExplorerPreferencePage extends FieldEditorPreferencePage
        implements IWorkbenchPreferencePage {
    /** The id of this preference page. */
    public static final String ID
            = "org.knime.workbench.explorer.view.explorer";
    private MountPointListEditor m_mountPoints;

    /**
    *
    */
   public ExplorerPreferencePage() {
       super("KNIME Spaces Explorer Settings", null, GRID);
       setDescription("Setup mount points for usage in KNIME Spaces Explorer "
               + "view.");
   }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final IWorkbench workbench) {
        IPreferenceStore corePrefStore =
            KNIMECorePlugin.getDefault().getPreferenceStore();
        setPreferenceStore(corePrefStore);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createFieldEditors() {
        m_mountPoints = new MountPointListEditor(getFieldEditorParent());
        addField(m_mountPoints);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void performApply() {
        ExplorerMountTable.clearPreparedMounts();
        super.performApply();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean performOk() {
        ExplorerMountTable.clearPreparedMounts();
        return super.performOk();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean performCancel() {
        ExplorerMountTable.clearPreparedMounts();
        return super.performCancel();
    }
}
