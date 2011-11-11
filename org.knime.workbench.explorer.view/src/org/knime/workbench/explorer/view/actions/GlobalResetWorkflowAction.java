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
  *   Oct 20, 2011 (morent): created
  */

package org.knime.workbench.explorer.view.actions;

import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.view.ExplorerView;
import org.knime.workbench.explorer.view.dnd.DragAndDropUtils;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class GlobalResetWorkflowAction extends ExplorerAction {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            GlobalResetWorkflowAction.class);
    /**
     * ID of the global delete action in the explorer menu.
     */
    public static final String RESETACTION_ID =
            "org.knime.workbench.explorer.action.reset";

    private static final ImageDescriptor IMG_RESET = KNIMEUIPlugin
    .imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
            "/icons/actions/reset.gif");
    /**
     * @param viewer the associated tree viewer
     */
    public GlobalResetWorkflowAction(final ExplorerView viewer) {
        super(viewer, "Reset");
        setToolTipText("Resets the workflow");
        setImageDescriptor(IMG_RESET);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return RESETACTION_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        WorkflowManager wfm = getWorkflow();
        return wfm != null && wfm.getParent().canResetNode(wfm.getID());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        List<AbstractExplorerFileStore> fileStores =
            DragAndDropUtils.getExplorerFileStores(getSelection());
        AbstractExplorerFileStore wfStore = fileStores.get(0);
        if (!(wfStore instanceof LocalExplorerFileStore)) {
            LOGGER.error("Can only execute local workflows locally.");
            return;
        }
        WorkflowManager wfm = getWorkflow();
        wfm.resetAndConfigureAll();
    }






}
