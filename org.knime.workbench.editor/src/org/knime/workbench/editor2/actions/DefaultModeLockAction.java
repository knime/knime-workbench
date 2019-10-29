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
 *   Oct 27, 2019 (loki): created
 */
package org.knime.workbench.editor2.actions;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.WorkflowEditorMode;

/**
 * This action is the default mode lock action (that nothing is locked.)
 *
 * @author loki der quaeler
 */
public class DefaultModeLockAction extends AbstractLockModeAction {
    /** The id under which instances of this are registered with the toolbar manager. **/
    public static final String ID = "org.knime.workbench.editor2.actions.defaultModeLockAction";


    /**
     * @param editor
     * @param tbm
     */
    public DefaultModeLockAction(final WorkflowEditor editor, final IToolBarManager tbm) {
        super(editor, tbm, ID, NodeLockAction.ID, AnnotationLockAction.ID);

        setChecked(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/nodes-annotations.png");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void performAction() {
        getWorkflowEditor().setAnnotationsLocked(false);

        final ToggleEditorModeAction toggleAction = getToggleEditorModeAction();
        toggleAction.toggleToModeIfNecessary(WorkflowEditorMode.NODE_EDIT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void refresh() {
        super.refresh();

        final WorkflowEditor we = getWorkflowEditor();
        setChecked(!we.getAnnotationsLocked() && we.getEditorMode().equals(WorkflowEditorMode.NODE_EDIT));
    }
}
