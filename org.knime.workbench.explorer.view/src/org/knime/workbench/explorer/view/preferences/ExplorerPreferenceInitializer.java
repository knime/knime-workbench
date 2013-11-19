/* ------------------------------------------------------------------
  * This source code, its documentation and all appendant files
  * are protected by copyright law. All rights reserved.
  *
  * Copyright, 2008 - 2013
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.AbstractContentProviderFactory;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class ExplorerPreferenceInitializer extends
        AbstractPreferenceInitializer {

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore prefStore =
                ExplorerActivator.getDefault().getPreferenceStore();
        // Set the default mount points
        List<AbstractContentProviderFactory> factories =
                ExplorerMountTable.getAddableContentProviders();
        List<MountSettings> settingsList = new ArrayList<MountSettings>();
        for (AbstractContentProviderFactory fac : factories) {
            if (fac.getDefaultMountID() != null) {
                final AbstractContentProvider cntProvider =
                    fac.createContentProvider(fac.getDefaultMountID());
                if (cntProvider != null) {
                    try {
                        settingsList.add(new MountSettings(cntProvider));
                    } finally {
                        cntProvider.dispose();
                    }
                }
            }
        }
        if (!settingsList.isEmpty()) {
            prefStore.setDefault(PreferenceConstants.P_EXPLORER_MOUNT_POINT,
                    MountSettings.getSettingsString(settingsList));
        }
        // Set the default behavior of "Do you want to link this meta node".
        prefStore.setDefault(
                PreferenceConstants.P_EXPLORER_LINK_ON_NEW_TEMPLATE,
                MessageDialogWithToggle.PROMPT);
    }

}
