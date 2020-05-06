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
 *   Apr 18, 2020 (loki): created
 */
package org.knime.workbench.editor2.actions.delegates;

import static org.knime.workbench.editor2.actions.delegates.FindNodeEditorAction.fixItemInNodeMenu;

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.widgets.Menu;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.actions.AbstractNodeAction;
import org.knime.workbench.editor2.actions.FindNextNodeAction;

/**
 * The genesis for this dialog is https://knime-com.atlassian.net/browse/AP-13990
 *
 * @author loki der quaeler
 */
@SuppressWarnings("restriction")    // WorkbenchWindow is discouraged API
public class FindNextNodeEditorAction extends AbstractEditorAction {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(FindNodeEditorAction.class);
    private static final AtomicBoolean FIND_NEXT_MENU_ITEM_IS_FIXED = new AtomicBoolean(false);
    private static Menu NODE_MENU = null;

    // We'd prefer to do this once, catching it in the application workbench advisor's postStartup() - but
    //      the Node menu is unpopulated at that point (presumably as the perspective has not yet been set.)
    // This is hacky and brittle, but what we need resort to as Eclipse fails to set the accelerator for the menu
    //      item because of the binding conflict on M1+G.
    // This will also be called many many times during the applications lifecycle; the first half-dozen or so
    //      before the user can interact with the app, and the Node menu won't actually have menu items until
    //      the user does their first click on it, so there's a bit of a dance going on here.
    private synchronized static void fixFindNextMenuItemIfNotYetDone() {
        if (NODE_MENU != null) {
            return;
        }
        NODE_MENU = fixItemInNodeMenu("Find Next Node...", "Find Next Node...\tCtrl+G", FIND_NEXT_MENU_ITEM_IS_FIXED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected AbstractNodeAction createAction(final WorkflowEditor editor) {
        fixFindNextMenuItemIfNotYetDone();
        return new FindNextNodeAction(editor);
    }
}
