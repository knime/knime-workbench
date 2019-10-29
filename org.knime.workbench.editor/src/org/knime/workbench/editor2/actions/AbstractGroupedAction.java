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
 * History
 *   Oct 28, 2019 (loki): created
 */
package org.knime.workbench.editor2.actions;

import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.knime.workbench.editor2.WorkflowEditor;

/**
 * This action is to be subclassed by N toggle actions that wish to function as a radio button group.
 *
 * @author loki der quaeler
 */
abstract class AbstractGroupedAction extends WorkbenchPartAction {

    private final String m_id;
    private final String[] m_otherIds;

    private final IToolBarManager m_toolbarManager;

    /**
     * @param editor the {@code WorkflowEditor} upon which this action will act
     * @param tbm the toolbar manager in which this action's item will sit
     * @param id the id of this action
     * @param otherId the ids of the other actions in this group
     */
    protected AbstractGroupedAction(final WorkflowEditor editor, final IToolBarManager tbm, final String id,
            final String... otherId) {
        super(editor, AS_CHECK_BOX);

        m_toolbarManager = tbm;

        m_id = id;
        m_otherIds = otherId;
    }

    /**
     * This is invoked, on the SWT thread, if the action is being run, after all of the buttons in the group have had
     * their state set correctly.
     */
    protected abstract void performAction();

    /**
     * Sets the workbench part / editor to which this action is associated.
     *
     * Due to ordering of invocations in Eclipse 4.7 related to toolbar setup and setting of active editor, we
     *  need provide a non-constructor way to set this.
     *
     * @param editor
     */
    public void setWorkflowEditor(final WorkflowEditor editor) {
        setWorkbenchPart(editor);
    }

    /**
     * @return the {@link WorkflowEditor} set via the constructor, or via {@code setWorkflowEditor(WorkflowEditor)}
     */
    protected WorkflowEditor getWorkflowEditor() {
        return (WorkflowEditor)getWorkbenchPart();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return m_id;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public final void run() {
        if (!isChecked()) {
            // disallow unselection - only allowed via the selection of another group member
            setChecked(true);
        } else {
            for (final String id : m_otherIds) {
                final ActionContributionItem actionItem = (ActionContributionItem)m_toolbarManager.find(id);

                if (actionItem != null) {
                    actionItem.getAction().setChecked(false);
                }
            }

            performAction();
        }
    }
}
