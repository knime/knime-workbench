/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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

package org.knime.workbench.explorer.view.actions;

import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystemUtils;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.view.ExplorerView;
import org.knime.workbench.explorer.view.dnd.DragAndDropUtils;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * Action for stepwise workflow execution using QuickForm nodes.
 *
 * @author Thomas Gabriel, KNIME.com, Zurich, Switzerland
 * @since 3.1
 */
public class GlobalQuickformWorkflowAction extends ExplorerAction {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(GlobalQuickformWorkflowAction.class);

    private static final ImageDescriptor IMG = KNIMEUIPlugin
            .imageDescriptorFromPlugin(ExplorerActivator.PLUGIN_ID,
                    "icons/quickform.png");

    /** ID of the global rename action in the explorer menu. */
    public static final String QUICKFORM_ACTION_ID =
            "org.knime.workbench.explorer.action.quickform-execute-workflow";

    private static final int SIZING_WIZARD_WIDTH  = 800;
    private static final int SIZING_WIZARD_HEIGHT = 600;

    /**
     * @param viewer the associated tree viewer
     */
    public GlobalQuickformWorkflowAction(final ExplorerView viewer) {
        super(viewer, "QuickForm Execution (legacy)...");
        setImageDescriptor(IMG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return QUICKFORM_ACTION_ID;
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
            LOGGER.error("Can only execute workflows from LOCAL space.");
            return;
        }

        try {
            if (ExplorerFileSystemUtils.lockWorkflow(
                    (LocalExplorerFileStore) wfStore)) {
                final WorkflowManager wfm = getWorkflow();

                QuickformExecuteWizard newWiz = new QuickformExecuteWizard(wfm);
                // newWiz.init(PlatformUI.getWorkbench(), getSelection());
                final QuickformExecuteWizardDialog dialog
                        = new QuickformExecuteWizardDialog(
                        getParentShell(), newWiz);
                dialog.create();
                dialog.getShell().setSize(Math.max(SIZING_WIZARD_WIDTH,
                        dialog.getShell().getSize().x),
                        SIZING_WIZARD_HEIGHT);
                dialog.open(); // wizard executes the flow
            } else {
                LOGGER.info("The workflow cannot be executed as "
                        + "is still in use by another user/instance.");
            }
        } finally {
            ExplorerFileSystemUtils.unlockWorkflow(
                    (LocalExplorerFileStore) wfStore);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEnabled() {
        final WorkflowManager wfm = getWorkflow();
        return (wfm != null);
    }

}
