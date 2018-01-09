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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.ui.preferences.PreferenceConstants;

public class ExplorerPrefsSyncer implements IPropertyChangeListener, IPreferenceChangeListener {
    private String m_previousValue;

    /**
     * Creates a new preference syncer.
     */
    public ExplorerPrefsSyncer() {
        m_previousValue = getUserOrDefaultValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void propertyChange(final PropertyChangeEvent event) {
        if (PreferenceConstants.P_EXPLORER_MOUNT_POINT_XML.equals(
            event.getProperty())) {
            String newValue = getUserOrDefaultValue();
            updateSettings(m_previousValue, newValue);
            m_previousValue = newValue;
        }
    }

    /**
     * {@inheritDoc}
     * @since 6.3
     */
    @Override
    public void preferenceChange(final PreferenceChangeEvent event) {
        if (PreferenceConstants.P_EXPLORER_MOUNT_POINT_XML.equals(event.getKey())) {
            String newValue = getUserOrDefaultValue();
            updateSettings(m_previousValue, newValue);
            m_previousValue = newValue;
        }
    }

    private void updateSettings(final String oldValue, final String newValue) {
        if (ConvenienceMethods.areEqual(oldValue, newValue)) {
            return;
        }

        Set<MountSettings> oldSettings;
        if (oldValue != null) {
            List<MountSettings> oldMS = MountSettings.parseSettings(
                    oldValue, false);
            oldSettings = new LinkedHashSet<MountSettings>(oldMS);
        } else {
            oldSettings = Collections.emptySet();
        }

        Set<MountSettings> newSettings;
        List<MountSettings> newMS;
        if (newValue != null) {
            newMS = MountSettings.parseSettings(newValue, false);
            newSettings = new LinkedHashSet<MountSettings>(newMS);
            // leave unchanged values untouched
            newSettings.removeAll(oldSettings);
        } else {
            newSettings = Collections.emptySet();
            newMS = Collections.emptyList();
        }
        oldSettings.removeAll(new LinkedHashSet<MountSettings>(newMS));

        // remove deleted mount points
        for (MountSettings ms : oldSettings) {
            boolean successful = ExplorerMountTable.unmount(
                    ms.getMountID());
            if (!successful) {
                // most likely mount point was not present to begin with
                NodeLogger.getLogger(this.getClass()).debug("Mount point \"" + ms.getDisplayName()
                        + "\" could not be unmounted.");
            }
        }

        // add all new mount points
        for (MountSettings ms : newSettings) {
            if (!ms.isActive()) {
                continue;
            }
            try {
                ExplorerMountTable.mount(ms.getMountID(),
                        ms.getFactoryID(), ms.getContent());
            } catch (IOException e) {
                NodeLogger.getLogger(this.getClass()).error("Mount point \"" + ms.getDisplayName()
                        + "\" could not be mounted.", e);
            }
        }

        // sync the ordering of the mount points
        List<String> newMountIds = new ArrayList<String>();
        for (MountSettings ms : newMS) {
            newMountIds.add(ms.getMountID());
        }
        ExplorerMountTable.setMountOrder(newMountIds);
    }

    private static String getUserOrDefaultValue() {
        IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode(ExplorerActivator.PLUGIN_ID);
        String value = preferences.get(PreferenceConstants.P_EXPLORER_MOUNT_POINT_XML, null);
        if (value == null) {
            IEclipsePreferences defaultPreferences = DefaultScope.INSTANCE.getNode(ExplorerActivator.PLUGIN_ID);
            value = defaultPreferences.get(PreferenceConstants.P_EXPLORER_MOUNT_POINT_XML, null);
        }
        return value;
    }
}
