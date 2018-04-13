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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.AbstractContentProviderFactory;
import org.knime.workbench.ui.preferences.PreferenceConstants;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class MountSettings {

    /** The preference key used to store the MountSettings as XML in the IEclipsePreference nodes */
    private static final String MOUNTPOINT_PREFERENCE_KEY = "mountpoint";

    /** Used for separating multiple mount settings in the preferences. */
    private static final String SETTINGS_SEPARATOR = "\n";

    private static final String VISIBILITY_SEPARATOR = "\t";

    /** Used for separating the different setting elements. */
    private static final String ELEMENTS_SEPARATOR = ":";

    private String m_displayName;

    private String m_mountID;

    private String m_defaultMountID;

    private String m_factoryID;

    private String m_content;

    private String m_state;

    private boolean m_active;

    private int m_mountPointNumber;

    /**
     * Creates a new mount settings object based on the passed settings string.
     *
     * @param settings a settings string
     */
    @Deprecated
    public MountSettings(final String settings) {
        parse(settings);
    }

    /**
     * Creates a new mount settings object based on the passed NodeSettings object.
     *
     * @param settings a NodeSettings object
     * @throws InvalidSettingsException if settings can't be retrieved
     * @since 6.0
     */
    public MountSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_mountID = settings.getString("mountID");
        m_displayName = settings.getString("displayName");
        m_factoryID = settings.getString("factoryID");
        m_content = settings.getString("content");
        m_defaultMountID = settings.getString("defaultMountID");
        m_active = settings.getBoolean("active");
        if (settings.containsKey("mountPointNumber")) {
            m_mountPointNumber = settings.getInt("mountPointNumber");
        }

    }

    /**
     * Creates a new mount settings object for the content provider.
     *
     * @param cp the content provider to create mount settings for
     */
    public MountSettings(final AbstractContentProvider cp) {
        m_mountID = cp.getMountID();
        m_displayName = m_mountID + " (" + cp.toString() + ")";
        m_factoryID = cp.getFactory().getID();
        m_content = cp.saveState();
        m_defaultMountID = cp.getFactory().getDefaultMountID();
        m_active = true;
        // New Mount Points Are always at the top of the table.
        m_mountPointNumber = 0;
    }

    /**
     * Creates a new mount settings object based on the passed NodeSettings object and the provided mount point number.
     *
     * @param settings A NodeSettings object
     * @param mountPointNumber The corresponding mount point number for the given NodeSettings
     * @throws InvalidSettingsException if the settings can't be retrieved
     * @since 8.2
     */
    public MountSettings(final NodeSettingsRO settings, final int mountPointNumber) throws InvalidSettingsException {
        this(settings);
        m_mountPointNumber = mountPointNumber;
    }

    /**
     * @param settings the settings string to be parsed
     */
    @Deprecated
    private void parse(final String settings) {
        String[] visibleSplit = settings.split(VISIBILITY_SEPARATOR, 2);
        if (2 != visibleSplit.length) {
            throw new IllegalArgumentException("Invalid settings string provided.");
        }
        m_displayName = visibleSplit[0];
        String[] settingsSplit = visibleSplit[1].split(ELEMENTS_SEPARATOR, 4);
        if (3 != settingsSplit.length && 4 != settingsSplit.length) {
            throw new IllegalArgumentException("Invalid settings string provided.");
        }
        m_mountID = settingsSplit[0];
        m_factoryID = settingsSplit[1];
        // settings with active state
        if (settingsSplit.length == 4) {
            String possibleBoolean = settingsSplit[2];
            // in case previous content contained ":", test for boolean value
            if ("true".equalsIgnoreCase(possibleBoolean) || "false".equalsIgnoreCase(possibleBoolean)) {
                m_active = Boolean.parseBoolean(possibleBoolean);
                m_content = settingsSplit[3];
            } else {
                m_active = true;
                m_content = settingsSplit[2] + ELEMENTS_SEPARATOR + settingsSplit[3];
            }
        } else {
            m_active = true;
            m_content = settingsSplit[2];
        }
    }

    /**
     * @return the name to be displayed for this mount settings
     */
    public String getDisplayName() {
        return m_displayName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getDisplayName();
    }

    /**
     * @return the mountID
     */
    public String getMountID() {
        return m_mountID;
    }

    /**
     * @return the defaultMountID
     * @since 6.0
     */
    public String getDefaultMountID() {
        return m_defaultMountID;
    }

    /**
     * @param defaultMountID the defaultMountID to set
     * @since 6.0
     */
    public void setDefaultMountID(final String defaultMountID) {
        if ((m_defaultMountID == null && defaultMountID != null)
                || (m_defaultMountID != null && defaultMountID == null)
                || (m_defaultMountID != null && defaultMountID != null && !defaultMountID.equals(m_defaultMountID))) {
            m_state = null;
        }
        m_defaultMountID = defaultMountID;
    }

    /**
     * @return the factoryID
     */
    public String getFactoryID() {
        return m_factoryID;
    }

    /**
     * @return the state of the content provider stored as string
     */
    public String getContent() {
        return m_content;
    }

    /**
     * @return the active
     * @since 6.0
     */
    public boolean isActive() {
        return m_active;
    }

    /**
     * @param active the active to set
     * @since 6.0
     */
    public void setActive(final boolean active) {
        if (m_active != active) {
            m_state = null;
        }
        m_active = active;
    }

    /**
     * Returns the mount point's number according to the mount points' ordering.
     *
     * @return The mount point number
     * @since 8.2
     */
    public int getMountPointNumber() {
        return m_mountPointNumber;
    }

    /**
     * @param nodeSettings the NodeSettings to save to
     */
    private void saveToNodeSettings(final NodeSettingsWO nodeSettings) {
        nodeSettings.addString("mountID", m_mountID);
        nodeSettings.addString("displayName", m_displayName);
        nodeSettings.addString("factoryID", m_factoryID);
        nodeSettings.addString("content", m_content);
        nodeSettings.addString("defaultMountID", m_defaultMountID);
        nodeSettings.addBoolean("active", m_active);
    }

    /**
     * @return the state of this mount settings as preference string
     */
    public String getSettingsString() {
        if (m_state == null) {
            m_state = getDisplayName() + VISIBILITY_SEPARATOR
                    + m_mountID + ELEMENTS_SEPARATOR
                    + m_factoryID + ELEMENTS_SEPARATOR
                    + Boolean.toString(m_active) + ELEMENTS_SEPARATOR
                    + (m_defaultMountID == null ? "" : m_defaultMountID) + ELEMENTS_SEPARATOR
                    + m_content;
        }
        return m_state;
    }

    /**
     * Parses a settings string containing one or multiple settings in XML form or separated by
     * {@link MountSettings#SETTINGS_SEPARATOR}.
     *
     * @param settings the preference string to parse
     * @param excludeUnknownContentProviders true if resulting list should only contain displayable settings
     * @return the parsed list of mount settings
     * @since 6.2
     */
    public static List<MountSettings> parseSettings(final String settings,
            final boolean excludeUnknownContentProviders)  {
        List<MountSettings> ms = new ArrayList<MountSettings>();
        if (settings == null || settings.isEmpty()) {
            return ms;
        }
        if (settings.startsWith("<?xml")) {
            try {
                NodeSettingsRO nodeSettings = NodeSettings.loadFromXML(new ByteArrayInputStream(settings.getBytes()));
                int numSettings = nodeSettings.getInt("numSettings");
                for (int i = 0; i < numSettings; i++) {
                    NodeSettingsRO singleSettings = nodeSettings.getNodeSettings("mountSettings_" + i);
                    MountSettings singleMountSettings = new MountSettings(singleSettings);
                    if (!excludeUnknownContentProviders || isMountSettingsAddable(singleMountSettings)) {
                        ms.add(singleMountSettings);
                    }
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Error parsing mount settings. ", e);
            }
        } else {
            String[] split = settings.split(SETTINGS_SEPARATOR);
            for (String setting : split) {
                MountSettings singleMountSettings = new MountSettings(setting);
                if (!excludeUnknownContentProviders || isMountSettingsAddable(singleMountSettings)) {
                    ms.add(singleMountSettings);
                }
            }
        }
        return ms;
    }

    /**
     * Checks if a given MountSettings object can be displayed.
     * @param mountSettings the settings to check
     * @return True, if the ContenProviderFactory of the given mountSettings is available.
     * @since 6.2
     */
    public static boolean isMountSettingsAddable(final MountSettings mountSettings) {
        AbstractContentProviderFactory contentProviderFactory =
                ExplorerMountTable.getContentProviderFactory(mountSettings.getFactoryID());
        return contentProviderFactory != null;
    }

    /**
     * @param mountSettings a list of MountSettings
     * @return an XML string representing the given list of MountSettings
     * @since 6.0
     */
    public static String getSettingsString(final List<MountSettings> mountSettings) {
        NodeSettings nodeSettings = new NodeSettings("mountSettings");
        for (int i = 0; i < mountSettings.size(); i++) {
            NodeSettingsWO singleSettings = nodeSettings.addNodeSettings("mountSettings_" + i);
            mountSettings.get(i).saveToNodeSettings(singleSettings);
        }
        nodeSettings.addInt("numSettings", mountSettings.size());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            nodeSettings.saveToXML(out);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error while saving mount settings to XML.", e);
        }
        return out.toString();
    }

    /**
     * Parses MountSettings to an XML which can be saved to a Preference node.
     *
     * @param mountSetting The MountSettings to be parsed as an XML
     * @param mountPointNumber The mount number of these MountSettings
     * @return The MountSettings as a String in XML format
     * @since 8.2
     */
    public static String getSingleSettingsString(final MountSettings mountSetting, final int mountPointNumber) {
        // Settings are parsed individually for IEclipsePreferences node.
        NodeSettings nodeSettings = new NodeSettings("mountSettings");
        NodeSettingsWO singleSettings = nodeSettings.addNodeSettings("mountSettings_" + mountPointNumber);
        mountSetting.saveToNodeSettings(singleSettings);
        nodeSettings.addInt("mountPointNumber", mountPointNumber);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            nodeSettings.saveToXML(out);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error while saving mount settings to XML.", e);
        }
        return out.toString();
    }

    /**
     * Parses a single mount settings from the give mount settings in XML format.
     *
     * @param settings The mount settings in XMLformat
     * @param excludeUnknownContentProviders true if resulting list should only contain displayable settings
     * @return The MountSettings parsed from the given XML
     * @since 8.2
     */
    public static MountSettings parseSingleSetting(final String settings,
            final boolean excludeUnknownContentProviders)  {
     // Settings are parsed individually for IEclipsePreferences node.
        MountSettings mountSettings = null;
        if (settings == null || settings.isEmpty()) {
            return mountSettings;
        }
        try {
            NodeSettingsRO nodeSettings = NodeSettings.loadFromXML(new ByteArrayInputStream(settings.getBytes()));
            int mountPointNumber = nodeSettings.getInt("mountPointNumber");
            NodeSettingsRO singleSettings = nodeSettings.getNodeSettings("mountSettings_" + mountPointNumber);
            MountSettings singleMountSettings = new MountSettings(singleSettings, mountPointNumber);
            if (!excludeUnknownContentProviders || isMountSettingsAddable(singleMountSettings)) {
                mountSettings = singleMountSettings;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing mount settings. ", e);
        }
        return mountSettings;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof MountSettings)) {
            return false;
        }
        return getSettingsString().equals(((MountSettings)obj).getSettingsString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getSettingsString().hashCode();
    }

    /**
     * Loads the MountSettings from the {@link ExplorerActivator#PLUGIN_ID} preference node.
     *
     * @return The MountSettings read from the {@link ExplorerActivator#PLUGIN_ID} preference node
     * @since 8.2
     */
    public static List<MountSettings> loadSortedMountSettingsFromPreferenceNode() {
        // AP-8989 Switching to IEclipsePreferences
        Map<Integer, MountSettings> defaultMountSettingsMap = new TreeMap<Integer, MountSettings>();
        Map<Integer, MountSettings> instancetMountSettingsMap = new TreeMap<Integer, MountSettings>();
        List<MountSettings> mountSettings = new ArrayList<MountSettings>();
        IEclipsePreferences instanceMountPointsNode = getInstanceMountPointParentNode();
        IEclipsePreferences defaultMountPointsNode  = getDefaultMountPointParentNode();
        try {
            String[] defaultChildNodes = defaultMountPointsNode.childrenNames();
            for (final String mountPointNodeName : defaultChildNodes) {
                final Preferences childMountPointNode = defaultMountPointsNode.node(mountPointNodeName);
                String s = childMountPointNode.get(MountSettings.MOUNTPOINT_PREFERENCE_KEY, "");
                MountSettings ms = MountSettings.parseSingleSetting(s, true);
                if (ms != null) {
                    defaultMountSettingsMap.put(ms.getMountPointNumber(), ms);
                }
            }
            String[] instanceChildNodes = instanceMountPointsNode.childrenNames();
            for (final String mountPointNodeName : instanceChildNodes) {
                final Preferences childMountPointNode = instanceMountPointsNode.node(mountPointNodeName);
                String s = childMountPointNode.get(MountSettings.MOUNTPOINT_PREFERENCE_KEY, "");
                MountSettings ms = MountSettings.parseSingleSetting(s, true);
                if (ms != null) {
                    instancetMountSettingsMap.put(ms.getMountPointNumber(), ms);
                }
            }



            mountSettings = new ArrayList<>(defaultMountSettingsMap.values());
            mountSettings.addAll(instancetMountSettingsMap.values());
        } catch (BackingStoreException e) {
            // ignore, return an empty list
        }

        return mountSettings;
    }

    /**
     * Loads the MountSettings from the DefaultInstance {@link ExplorerActivator#PLUGIN_ID} preference node.
     *
     * @return The MountSettings read from the {@link ExplorerActivator#PLUGIN_ID} preference node
     * @since 8.2
     */
    public static List<MountSettings> loadSortedMountSettingsFromDefaultPreferenceNode() {
        // AP-8989 Switching to IEclipsePreferences
        Map<Integer, MountSettings> defaultMountSettingsMap = new TreeMap<Integer, MountSettings>();
        List<MountSettings> mountSettings = new ArrayList<MountSettings>();
        IEclipsePreferences defaultMountPointsNode  = getDefaultMountPointParentNode();
        try {
            String[] defaultChildNodes = defaultMountPointsNode.childrenNames();
            for (final String mountPointNodeName : defaultChildNodes) {
                final Preferences childMountPointNode = defaultMountPointsNode.node(mountPointNodeName);
                String s = childMountPointNode.get(MountSettings.MOUNTPOINT_PREFERENCE_KEY, "");
                MountSettings ms = MountSettings.parseSingleSetting(s, true);
                if (ms != null) {
                    defaultMountSettingsMap.put(ms.getMountPointNumber(), ms);
                }
            }

            mountSettings = new ArrayList<>(defaultMountSettingsMap.values());
        } catch (BackingStoreException e) {
            // ignore, return an empty list
        }

        return mountSettings;
    }

    /**
     * Loads the MountSettings from either the {@link ExplorerActivator#PLUGIN_ID} preference node, or from the
     * PreferenceStore, this ensures backwards compatibility.
     *
     * @return The MountSettings read from the {@link ExplorerActivator#PLUGIN_ID} preference node
     * @throws BackingStoreException if there is a failure in the backing store
     * @since 8.2
     */
    public static List<MountSettings> loadSortedMountSettingsFromPreferences() throws BackingStoreException {
        // AP-8989 Switching to IEclipsePreferences
        List<MountSettings> mountSettings = new ArrayList<MountSettings>();
        IEclipsePreferences mountPointsNode = getInstanceMountPointParentNode();
        String[] childNodes = null;
        childNodes = mountPointsNode.childrenNames();
        if (childNodes == null || childNodes.length == 0) {
            // Backwards compatibility.
            IPreferenceStore prefStore = ExplorerActivator.getDefault().getPreferenceStore();
            String prefString = prefStore.getString(PreferenceConstants.P_EXPLORER_MOUNT_POINT_XML);;
            if (prefString == null || prefString.isEmpty()) {
                prefString = prefStore.getDefaultString(PreferenceConstants.P_EXPLORER_MOUNT_POINT_XML);
            }
            mountSettings = MountSettings.parseSettings(prefString, false);
        } else {
            mountSettings = loadSortedMountSettingsFromPreferenceNode();
        }
        return mountSettings;
    }

    /**
     * Saves the given mountSettings to the {@link ExplorerActivator#PLUGIN_ID) preference node.
     * The preferences are saved in the mount point ordering of the given List.
     *
     * @param mountSettings  The MountSettings to be saved to the preference node
     * @since 8.2
     */
    public static void saveMountSettings(final List<MountSettings> mountSettings) {
        // AP-8989 Switching to IEclipsePreferences
        List<MountSettings> defaultMountSettings = MountSettings.loadSortedMountSettingsFromDefaultPreferenceNode();
        mountSettings.removeAll(defaultMountSettings);

        IEclipsePreferences mountPointsNode = getInstanceMountPointParentNode();
        for (int i = 0; i < mountSettings.size(); i++) {
                MountSettings ms = mountSettings.get(i);
                IEclipsePreferences mountPointChildNode = (IEclipsePreferences)mountPointsNode.node(ms.getMountID());
                //InstanceScope.INSTANCE.getNode(ExplorerActivator.PLUGIN_ID + "/" + ms.getMountID());
                mountPointChildNode.put(MOUNTPOINT_PREFERENCE_KEY, MountSettings.getSingleSettingsString(ms, i));
        }
    }

    /**
     * Removes the given MountSettings from the {@link ExplorerActivator#PLUGIN_ID} preference node.
     *
     * @param mountSettings  The mounSettings to be removed from the preference node
     * @throws BackingStoreException if there is a failure in the backing store
     * @since 8.2
     */
    public static void removeMountSettings(final List<String> mountSettings) throws BackingStoreException {
        // AP-8989 Switching to IEclipsePreferences
        for (String ms : mountSettings) {
            IEclipsePreferences mountPointNode = InstanceScope.INSTANCE.getNode(ExplorerActivator.PLUGIN_ID);
            mountPointNode.node(ms).removeNode();
        }
    }

    private static IEclipsePreferences getInstanceMountPointParentNode() {
        IEclipsePreferences mountPointsNode = InstanceScope.INSTANCE.getNode(ExplorerActivator.PLUGIN_ID);
        return mountPointsNode;
    }

    private static IEclipsePreferences getDefaultMountPointParentNode() {
        IEclipsePreferences mountPointsNode = DefaultScope.INSTANCE.getNode(ExplorerActivator.PLUGIN_ID);
        return mountPointsNode;
    }

}
