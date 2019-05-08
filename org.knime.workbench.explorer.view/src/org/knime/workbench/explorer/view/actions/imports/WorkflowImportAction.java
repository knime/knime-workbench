/*
 * ------------------------------------------------------------------------
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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

import org.knime.core.node.workflow.NodeTimer;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ContentDelegator;
import org.knime.workbench.explorer.view.ExplorerView;
import org.knime.workbench.explorer.view.actions.ExplorerAction;

/**
 * Action to export workflow(s).
 *
 * @author ohl, University of Konstanz
 */
public class WorkflowImportAction extends ExplorerAction {
    /** id of the action. */
    public static final String ID = "org.knime.explorer.view.actions.import";

    private AbstractExplorerFileStore m_destination;

    private String m_selectedFile;

    /**
     * @param view underlying view
     */
    public WorkflowImportAction(final ExplorerView view) {
        this(view, null, null);
    }

    /**
     * @param view underlying view
     * @param destination destination in workflow tree
     * @since 7.3
     */
    public WorkflowImportAction(final ExplorerView view, final AbstractExplorerFileStore destination) {
        this(view, destination, null);
    }

    /**
     * @param view underlying view
     * @param destination destination in workflow tree
     * @param selectedFile selected file to import from
     * @since 7.3
     */
    public WorkflowImportAction(final ExplorerView view, final AbstractExplorerFileStore destination,
        final String selectedFile) {
        super(view, WorkflowImportHelper.MENU_TEXT);
        setImageDescriptor(WorkflowImportHelper.ICON);
        setToolTipText(WorkflowImportHelper.TOOLTIP);
        m_destination = destination;
        m_selectedFile = selectedFile;
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
        if (m_destination == null) {
            Map<AbstractContentProvider, List<AbstractExplorerFileStore>> selectedFiles = getSelectedFiles();
            if (selectedFiles != null && selectedFiles.size() > 0) {
                m_destination = selectedFiles.values().iterator().next().iterator().next();
            }
        }

        AbstractExplorerFileStore destination =
            WorkflowImportHelper.openImportWizard(getParentShell(), m_destination, m_selectedFile);
        if (destination != null) {
            NodeTimer.GLOBAL_TIMER.incWorkflowImport();
            Object object = ContentDelegator.getTreeObjectFor(destination);
            getViewer().refresh(object);
            getViewer().reveal(object);
        }
    }
}
