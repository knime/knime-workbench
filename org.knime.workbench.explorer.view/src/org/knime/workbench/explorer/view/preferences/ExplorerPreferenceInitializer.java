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

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.localworkspace.LocalWorkspaceContentProviderFactory;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class ExplorerPreferenceInitializer extends
        AbstractPreferenceInitializer {
    /**
     * The name of the local space mounted by default.
     */
    public static final String DEFAULT_LOCAL_NAME = "LOCAL";

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore prefStore =
            ExplorerActivator.getDefault().getPreferenceStore();
        LocalWorkspaceContentProviderFactory fac
                = new LocalWorkspaceContentProviderFactory();
        // Add the local workspace per default.
        MountSettings ms = new MountSettings(fac.getContentProvider(
                DEFAULT_LOCAL_NAME));
        prefStore.setDefault(PreferenceConstants.P_EXPLORER_MOUNT_POINT,
                ms.getSettingsString() + MountSettings.SETTINGS_SEPARATOR);
        prefStore.setDefault(
                PreferenceConstants.P_EXPLORER_LINK_ON_NEW_TEMPLATE,
                MessageDialogWithToggle.PROMPT);
    }

}
