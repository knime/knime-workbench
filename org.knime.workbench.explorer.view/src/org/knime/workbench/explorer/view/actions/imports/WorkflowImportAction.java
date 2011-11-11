/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * Created: May 19, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer.view.actions.imports;

import java.util.List;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ContentDelegator;
import org.knime.workbench.explorer.view.actions.ExplorerAction;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * Action to export workflow(s).
 *
 * @author ohl, University of Konstanz
 */
public class WorkflowImportAction extends ExplorerAction {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(WorkflowImportAction.class);

    private static final int SIZING_WIZARD_WIDTH = 470;

    private static final int SIZING_WIZARD_HEIGHT = 550;

    private static final ImageDescriptor ICON = KNIMEUIPlugin
            .imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
                    "icons/knime_import.png");

    /** id of the action */
    public static final String ID = "org.knime.explorer.view.actions.import";

    /**
     * @param viewer underlying viewer
     */
    public WorkflowImportAction(final TreeViewer viewer) {
        super(viewer, "Import KNIME Workflow...");
        setImageDescriptor(ICON);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return !isRO();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        WorkflowImportWizard impWiz = new WorkflowImportWizard();
        Map<AbstractContentProvider, List<AbstractExplorerFileStore>> selectedFiles =
                getSelectedFiles();
        AbstractExplorerFileStore sel = null;
        if (selectedFiles != null && selectedFiles.size() > 0) {
            sel = selectedFiles.values().iterator().next().iterator().next();
        }
        impWiz.setInitialDestination(sel);

        WizardDialog dialog = new WizardDialog(getParentShell(), impWiz);
        // dialog.create();
        // dialog.getShell().setSize(
        // Math.max(SIZING_WIZARD_WIDTH, dialog.getShell().getSize().x),
        // SIZING_WIZARD_HEIGHT);
        dialog.open();

        AbstractExplorerFileStore destination =
                impWiz.getDestinationContainer();
        Object object = ContentDelegator.getTreeObjectFor(destination);
        getViewer().refresh(object);
        getViewer().reveal(object);
    }

}
