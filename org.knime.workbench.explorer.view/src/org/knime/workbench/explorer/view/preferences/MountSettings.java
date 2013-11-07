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
  *   May 10, 2011 (morent): created
  */

package org.knime.workbench.explorer.view.preferences;

import java.util.ArrayList;
import java.util.List;

import org.knime.workbench.explorer.view.AbstractContentProvider;

/**
 * Stores all necessary information that is needed for creating mount points.
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class MountSettings {
    /** Used for separating multiple mount settings in the preferences.*/
    public static final String SETTINGS_SEPARATOR = "\n";
    private static final String VISIBILITY_SEPARATOR = "\t";
    /** Used for separating the different setting elements. */
    public static final String ELEMENTS_SEPARATOR = ":";
    private String m_displayName;
    private String m_mountID;
    private String m_factoryID;

    private String m_content;
    private String m_state;

    private boolean m_active;

    /**
     * Creates a new mount settings object based on the passed settings string.
     * @param settings a settings string
     */
    public MountSettings(final String settings) {
        parse(settings);
    }

    /**
     * Creates a new mount settings object for the content provider.
     * @param cp the content provider to create mount settings for
     */
    public MountSettings(final AbstractContentProvider cp) {
        m_mountID = cp.getMountID();
        m_displayName = m_mountID + " (" + cp.toString() + ")";
        m_factoryID = cp.getFactory().getID();
        m_content = cp.saveState();
        m_active = true;
    }

    /**
     * @param settings the settings string to be parsed
     */
    private void parse(final String settings) {
        String[] visibleSplit = settings.split(VISIBILITY_SEPARATOR, 2);
        if (2 != visibleSplit.length) {
            throw new IllegalArgumentException(
                    "Invalid settings string provided.");
        }
        m_displayName = visibleSplit[0];
        String[] settingsSplit = visibleSplit[1].split(ELEMENTS_SEPARATOR, 4);
        if (3 != settingsSplit.length && 4 != settingsSplit.length) {
            throw new IllegalArgumentException(
                    "Invalid settings string provided.");
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
                m_active = false;
                m_content = settingsSplit[2] + ELEMENTS_SEPARATOR + settingsSplit[3];
            }
        } else {
            m_active = false;
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
     */
    public boolean isActive() {
        return m_active;
    }

    /**
     * @param active the active to set
     */
    public void setActive(final boolean active) {
        if (m_active != active) {
            m_state = null;
        }
        m_active = active;
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
                    + m_content;
        }
        return m_state;
    }

    /**
     * Parses a settings string containing one or multiple settings separated
     * by {@link MountSettings#SETTINGS_SEPARATOR}.
     *      * @param settings the preference string to parse
     * @return the parsed list of mount settings
     */
    public static List<MountSettings> parseSettings(final String settings) {
        List<MountSettings> ms = new ArrayList<MountSettings>();
        if (settings.isEmpty()) {
            return ms;
        }
        String[] split = settings.split(SETTINGS_SEPARATOR);
        for (String setting : split) {
            ms.add(new MountSettings(setting));
        }
        return ms;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof MountSettings)) {
            return false;
        }
        return getSettingsString().equals(
                ((MountSettings)obj).getSettingsString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getSettingsString().hashCode();
    }
}
