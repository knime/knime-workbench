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
 *   14.08.2009 (ohl): created
 */
package org.knime.workbench.explorer.view.actions;

import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.ExplorerView;
import org.knime.workbench.explorer.view.dnd.DragAndDropUtils;

/**
 *
 * @author morent, KNIME.com, Zurich, Switzerland
 */
public class GlobalRefreshAction extends ExplorerAction {
    private static final String TOOLTIP = "Refresh the view";


    /**
     * @param viewer the viewer containing the file stores
     */
    public GlobalRefreshAction(final ExplorerView viewer) {
        super(viewer, "Refresh");
        setImageDescriptor(ImageRepository.getImageDescriptor(SharedImages.Refresh));
        setToolTipText(TOOLTIP);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return "org.knime.workbench.explorer.view.action.refresh";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        IStructuredSelection selection = getSelection();
        List<AbstractExplorerFileStore> stores =
                DragAndDropUtils.getExplorerFileStores(selection);
        if (stores == null) {
            getViewer().refresh();
        } else {
            for (AbstractExplorerFileStore file : stores) {
//                getViewer().refresh(ContentDelegator.getTreeObjectFor(file));
                file.refresh();
            }
        }
    }
}
