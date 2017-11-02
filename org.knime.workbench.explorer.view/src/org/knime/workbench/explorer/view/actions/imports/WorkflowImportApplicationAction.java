/*
 * ------------------------------------------------------------------------
 *
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
 * History
 *   02.07.2014 (ohl): created
 */
package org.knime.workbench.explorer.view.actions.imports;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.ContentDelegator;

/**
 * Registered with the application File menue.
 *
 * @author Peter Ohl, KNIME AG, Zurich, Switzerland
 * @since 6.4
 */
public class WorkflowImportApplicationAction extends Action {
    /** id of the action. */
    public static final String ID = "org.knime.workbench.explorer.view.actions.imports.WorkflowImportApplicationAction";

    private AbstractExplorerFileStore m_destination; // set after wizard finish

    /**
     * The constructor.
     */
    public WorkflowImportApplicationAction() {
        super(WorkflowImportHelper.MENU_TEXT, WorkflowImportHelper.ICON);
        setToolTipText(WorkflowImportHelper.TOOLTIP);
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
     * run is called multiple times on a static instance in the WorkflowImportAction. This method/class must not
     * use any member variable or store a state in any other way. See {@link WorkflowImportAction}.
     */
    @Override
    public void run() {
        IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (workbenchWindow == null) {
            return;
        }
        ISelection workbenchSelection = workbenchWindow.getSelectionService().getSelection();
        AbstractExplorerFileStore initFile = null;
        if (workbenchSelection instanceof IStructuredSelection) {
            initFile = ContentDelegator.getFileStore(((IStructuredSelection)workbenchSelection).getFirstElement());
        }

        m_destination = WorkflowImportHelper.openImportWizard(workbenchWindow.getShell(), initFile);
    }

    /**
     * Returns the import destination - only valid after the wizard finishes (after run()).
     * @return the import destination - only valid after the wizard finishes (after run())
     */
    public AbstractExplorerFileStore getDestination() {
        return m_destination;
    }
}
