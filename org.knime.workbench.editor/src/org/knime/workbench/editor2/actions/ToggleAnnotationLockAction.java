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
 *   Sep 23, 2019 (loki): created
 */
package org.knime.workbench.editor2.actions;

import org.eclipse.jface.action.Action;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.actions.delegates.AbstractEditorAction;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * An action which supports the toggling of a workflow editor's annotation-lock status.
 *
 * N.B We don't necessarily want to inherit from anything other than {@link Action} but we _do_ want to get the workflow
 * editor which an accompanying delegate {@link AbstractEditorAction} can provide; that editor action, though, must
 * return an instance of {@code AbstractNodeAction}
 *
 * @author loki der quaeler
 */
public class ToggleAnnotationLockAction extends AbstractNodeAction {
    /** the id of this action (not to be confused with ids used in the plugin.xml) */
    public static final String ID = "knime.action.toggle_annotation_lock";


    /**
     * @param editor the workflow editor on which this action operates.
     */
    public ToggleAnnotationLockAction(final WorkflowEditor editor) {
        super(editor);
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
     *
     * We override this as we don't need the overhead of collecting the selected nodes which our parent's implementation
     * performs.
     */
    @Override
    public void runInSWT() {
        final WorkflowEditor we = getEditor();

        we.setAnnotationsLocked(!we.getAnnotationsLocked());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        throw new UnsupportedOperationException("This method should never be called.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean internalCalculateEnabled() {
        return true;
    }
}
