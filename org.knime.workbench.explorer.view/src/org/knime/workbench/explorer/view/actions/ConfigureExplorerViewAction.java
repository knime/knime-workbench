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
 *   May 23, 2011 (morent): created
 */

package org.knime.workbench.explorer.view.actions;

import java.util.List;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.explorer.MountPoint;
import org.knime.workbench.explorer.view.ContentDelegator;
import org.knime.workbench.explorer.view.ExplorerView;
import org.knime.workbench.explorer.view.dialogs.SelectMountPointDialog;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class ConfigureExplorerViewAction extends ExplorerAction {
    private static final String TOOLTIP
            = "Configure the visible content of the Explorer View";

    private final ContentDelegator m_delegator;
    /**
     *
     * @param viewer the viewer this action is attached to
     * @param delegator the content delegator of the view
     */
    public ConfigureExplorerViewAction(final ExplorerView viewer,
            final ContentDelegator delegator) {
        super(viewer, "Configure Content...");
        m_delegator = delegator;
        setToolTipText(TOOLTIP);
        setImageDescriptor(ImageRepository
                .getImageDescriptor(SharedImages.ConfigureNode));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return "org.knime.workbench.explorer.view.action.configure-view";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
            TreeViewer viewer = getViewer();
            SelectMountPointDialog selectDlg =
                    new SelectMountPointDialog(viewer.getControl().getShell(),
                            m_delegator.getMountedIds());
            selectDlg.setBlockOnOpen(true);
            if (selectDlg.open() != Window.OK) {
                return;
            }
            m_delegator.removeAllMountPoints();
            List<MountPoint> result = selectDlg.getResult();
            for (MountPoint mp : result) {
                m_delegator.addMountPoint(mp);
            }
            viewer.refresh();
    }
}
