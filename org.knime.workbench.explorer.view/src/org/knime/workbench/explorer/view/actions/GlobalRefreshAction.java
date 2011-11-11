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
 *   14.08.2009 (ohl): created
 */
package org.knime.workbench.explorer.view.actions;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.ExplorerView;

/**
 *
 * @author morent, KNIME.com, Zurich, Switzerland
 */
public class GlobalRefreshAction extends ExplorerAction {

    /**
     * The icon for the refresh action.
     */
    public static final ImageDescriptor IMG_REFRESH =
        AbstractUIPlugin.imageDescriptorFromPlugin(ExplorerActivator.PLUGIN_ID,
        "icons/refresh_nav.gif");

    private static final String TOOLTIP = "Refresh the view";

    private AbstractExplorerFileStore[] m_fileStore;

    /**
     * Refreshes all elements in the viewer.
     * @param viewer the viewer to refresh
     */
    public GlobalRefreshAction(final ExplorerView viewer) {
        this(viewer, (AbstractExplorerFileStore[])null);
    }

    /**
     * Refreshes the passed elements in the viewer.
     * @param viewer the viewer containing the file stores
     * @param fileStore the file stores to refresh
     */
    public GlobalRefreshAction(final ExplorerView viewer,
            final AbstractExplorerFileStore ... fileStore) {
        super(viewer, "Refresh");
        setImageDescriptor(IMG_REFRESH);
        setToolTipText(TOOLTIP);
        m_fileStore = fileStore;
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
        if (m_fileStore == null) {
            getViewer().refresh();
        } else {
            for (AbstractExplorerFileStore file : m_fileStore) {
//                getViewer().refresh(ContentDelegator.getTreeObjectFor(file));
                file.refresh();
            }
        }
    }
}
