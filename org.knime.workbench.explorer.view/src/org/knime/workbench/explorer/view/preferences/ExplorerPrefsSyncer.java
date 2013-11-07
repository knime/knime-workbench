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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * Keeps the {@link ExplorerMountTable} in sync with the mount point
 * preferences.
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class ExplorerPrefsSyncer implements IPropertyChangeListener {
    /**
     * {@inheritDoc}
     */

    @Override
    public void propertyChange(final PropertyChangeEvent event) {
        if (PreferenceConstants.P_EXPLORER_MOUNT_POINT.equals(
                event.getProperty())) {
            String oldValue = (String)event.getOldValue();
            String newValue = (String)event.getNewValue();
            if ((oldValue == newValue) || ((oldValue != null) && oldValue.equals(newValue))) {
                return;
            }

            Set<MountSettings> oldSettings;
            if (oldValue != null) {
                List<MountSettings> oldMS = MountSettings.parseSettings(
                        oldValue);
                oldSettings = new LinkedHashSet<MountSettings>(oldMS);
            } else {
                oldSettings = Collections.emptySet();
            }

            Set<MountSettings> newSettings;
            List<MountSettings> newMS;
            if (newValue != null) {
                newMS = MountSettings.parseSettings(newValue);
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
    }
}
