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
package org.knime.workbench.explorer.view.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.view.AbstractContentProviderFactory;
import org.knime.workbench.ui.preferences.PreferenceConstants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.prefs.BackingStoreException;

public class ExplorerPreferenceInitializer extends
        AbstractPreferenceInitializer {

    private static final String MOUNTPOINT_PREFERENCE_LOCATION = ExplorerActivator.PLUGIN_ID + "/defaultMountpoint";

    private static final String DEFAULT_MOUNTPOINTS_LIST = "defaultMountpoints";

    private static final String ENFORCE_EXCLUSION = "enforceExclusion";

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeDefaultPreferences() {

        IPreferenceStore prefStore =
                ExplorerActivator.getDefault().getPreferenceStore();

        // Set the default behavior of "Do you want to link this metanode".
        prefStore.setDefault(
                PreferenceConstants.P_EXPLORER_LINK_ON_NEW_TEMPLATE,
                MessageDialogWithToggle.PROMPT);

        // Set the default behavior of "Should a warning dialog appear when you connect to a server via EJB".
        prefStore.setDefault(PreferenceConstants.P_SHOW_EJB_WARNING_DIALOG,
            PreferenceConstants.P_DEFAULT_SHOW_EJB_WARNING_DIALOG);

        // Set the default behavior of "Should a warning dialog appear when you connect to an older server".
        prefStore.setDefault(PreferenceConstants.P_SHOW_OLDER_SERVER_WARNING_DIALOG,
            PreferenceConstants.P_DEFAULT_SHOW_OLDER_SERVER_WARNING_DIALOG);
    }

    /**
     * Loads the default mount points into default {@link PreferenceConstants#P_EXPLORER_MOUNT_POINT_XML}. This should
     * be called if there are no other settings and you have to fall back to the old xml format.
     *
     * @since 8.5
     */
    public static void loadDefaultMountPoints() {
        final IPreferenceStore prefStore = ExplorerActivator.getDefault().getPreferenceStore();
        // Set the default mount points
        final List<AbstractContentProviderFactory> factories = ExplorerMountTable.getAddableContentProviders();
        final List<MountSettings> settingsList = new ArrayList<MountSettings>();
        final List<String> include = getIncludedDefaultMountPoints();

        for (AbstractContentProviderFactory fac : factories) {
            if (fac.getDefaultMountID() != null && include.contains(fac.getDefaultMountID())) {
                fac.tryCreateContentProvider(fac.getDefaultMountID()).ifPresent(cntProvider -> {
                    try {
                        settingsList.add(new MountSettings(cntProvider));
                    } finally {
                        cntProvider.dispose();
                    }
                });
            }
        }

        if (!settingsList.isEmpty()) {
            prefStore.setDefault(PreferenceConstants.P_EXPLORER_MOUNT_POINT_XML,
                MountSettings.getSettingsString(settingsList));
        }
    }

    /**
     * @return true, if mount settings have been stored in XML yet
     * @since 6.2
     */
    public static boolean existsMountPreferencesXML() {
        IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(FrameworkUtil.getBundle(
            ExplorerActivator.class).getSymbolicName());
        String mpSettings = preferences.get(PreferenceConstants.P_EXPLORER_MOUNT_POINT_XML, null);
        return (mpSettings != null && !mpSettings.isEmpty());
    }

    /**
     * Returns whether there are MountSettings stored in the {@link ExplorerActivator#PLUGIN_ID} preference node.
     *
     * @return Whether there are MountSettingsStored in the {@link ExplorerActivator#PLUGIN_ID} preference node
     * @since 8.2
     */
    public static boolean existMountPointPreferenceNodes() {
        IEclipsePreferences mountPointNode = InstanceScope.INSTANCE.getNode(ExplorerActivator.PLUGIN_ID);
        try {
            return mountPointNode.childrenNames().length > 0;
        } catch (BackingStoreException e) {
            // No settings to be found.
            return false;
        }
    }

    /**
     * Returns a list with all default mount points that shall be added.
     *
     * @return List with all default mount points that shall be added.
     * @since 8.5
     */
    public static List<String> getIncludedDefaultMountPoints() {
        final String mpSetting =
            DefaultScope.INSTANCE.getNode(MOUNTPOINT_PREFERENCE_LOCATION).get(DEFAULT_MOUNTPOINTS_LIST, null);

        final boolean enforceExclusion =
            DefaultScope.INSTANCE.getNode(MOUNTPOINT_PREFERENCE_LOCATION).getBoolean(ENFORCE_EXCLUSION, false);

        if (mpSetting == null) {
            return enforceExclusion ? Collections.emptyList() : ExplorerMountTable.getAddableContentProviders().stream()
                .map(e -> e.getDefaultMountID()).filter(e -> !StringUtils.isEmpty(e)).collect(Collectors.toList());
        }

        final String[] mps = mpSetting.split("\\,");

        return Arrays.stream(mps).map(e -> e.trim()).filter(e -> !StringUtils.isEmpty(e)).collect(Collectors.toList());
    }

    /**
     * Returns a list with all default mount points that shall be excluded.
     *
     * @return List with all default mount points that shall be excluded.
     * @since 8.5
     */
    public static List<String> getExcludedDefaultMountPoints() {
        if (DefaultScope.INSTANCE.getNode(MOUNTPOINT_PREFERENCE_LOCATION).getBoolean(ENFORCE_EXCLUSION, false)) {
            final List<String> includedMountPoints = getIncludedDefaultMountPoints();

            return ExplorerMountTable.getAddableContentProviders().stream().map(e -> e.getDefaultMountID())
                .filter(e -> !StringUtils.isEmpty(e) && !includedMountPoints.contains(e)).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
