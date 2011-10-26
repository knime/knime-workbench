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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Widget;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * Allows to manipulate, persist and restore a list of mount points.
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class MountPointListEditor extends ListEditor {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            MountPointListEditor.class);
    private final Map<String, MountSettings> m_mountSettings;

    /**
     * @param parent the parent composite
     */
    public MountPointListEditor(final Composite parent) {
        super(PreferenceConstants.P_EXPLORER_MOUNT_POINT,
                "List of configured mount points:", parent);
        m_mountSettings = new TreeMap<String, MountSettings>();

        // select the newly created element
        getAddButton().addSelectionListener(new SelectionAdapter() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void widgetSelected(final SelectionEvent event) {
                Widget widget = event.widget;
                if (widget == getAddButton()) {
                    int index = getList().getSelectionIndex();
                    if (index >= 0) {
                        getList().select(index + 1);
                    } else {
                        getList().select(0);
                    }
                    selectionChanged();
                }
            }
        });

        /* Add our own selection listener before the parent's. Otherwise
         * the parent just removes the item and there is no chance to retrieve
         * the removed item.*/
        Button removeButton = getRemoveButton();
        Listener[] listeners = removeButton.getListeners(SWT.Selection);
        // remove registered selection listeners
        for (Listener listener : listeners) {
            removeButton.removeListener(SWT.Selection, listener);
        }
        // insert this listener as first selection listener
        removeButton.addSelectionListener(new SelectionAdapter() {
            /**
             * {@inheritDoc}
             */
            @Override
            public void widgetSelected(final SelectionEvent event) {
                org.eclipse.swt.widgets.List list = getList();
                int index = list.getSelectionIndex();
                if (index >= 0) {
                    MountSettings mountSettings = m_mountSettings.remove(
                            list.getItem(index));
                    String mountID = mountSettings.getMountID();
                    if (ExplorerMountTable.clearPreparedMount(mountID)) {
                        LOGGER.debug("Prepared mount point \"" + mountID
                                    + "\" removed. ");
                    }
                }
            }
        });
        // add the previously registered listeners
        for (Listener listener : listeners) {
            removeButton.addListener(SWT.Selection, listener);
        }

    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected String[] parseString(final String stringList) {
        List<MountSettings> settings = MountSettings.parseSettings(stringList);
        String[] labels = new String[settings.size()];
        for (int i = 0; i < labels.length; i++) {
            MountSettings ms = settings.get(i);
            m_mountSettings.put(ms.getDisplayName(), ms);
            labels[i] = ms.getDisplayName();
        }
        return labels;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getNewInputObject() {
        NewMountPointDialog dlg =
            new NewMountPointDialog(getShell(),
                    ExplorerMountTable.getAddableContentProviders(),
                    ExplorerMountTable.getAllMountIDs());
        if (dlg.open() != Dialog.OK) {
            return null;
        }
        AbstractContentProvider newCP = null;
        try {
            String mountID = dlg.getMountID();
            newCP =
                    ExplorerMountTable.prepareMount(mountID,
                            dlg.getFactory().getID());
            /* The associated preference page has to take care of committing or
             * canceling the prepared mounts. */
            LOGGER.debug("Mount point \"" + mountID + "\" prepared.");
        } catch (IOException e) {
            MessageBox mb =
                    new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
            String msg = e.getMessage();
            if (msg == null || msg.isEmpty()) {
                msg = "<no details>";
            }
            mb.setMessage("I/O error while creating new content: " + msg);
            mb.open();
        }
        if (newCP != null) {
            MountSettings mountSettings = new MountSettings(newCP);
            String label = mountSettings.getDisplayName();
            m_mountSettings.put(label, mountSettings);
            return label;
        }
        return null;
    }



    /**
     * {@inheritDoc}
     */
    @Override
    protected String createList(final String[] items) {
        String res = "";
        for (int i = 0; i < items.length; i++) {
            if (i > 0) {
                res += MountSettings.SETTINGS_SEPARATOR;
            }
            String label = items[i];
            MountSettings settings = m_mountSettings.get(label);
            res += settings.getSettingsString();
        }
        return res;
    }
}
