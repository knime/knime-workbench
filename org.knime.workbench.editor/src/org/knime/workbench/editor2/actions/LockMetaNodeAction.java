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
 * History
 *   15.10.2011 (Bernd Wiswedel): created
 */
package org.knime.workbench.editor2.actions;

import static org.knime.core.ui.wrapper.Wrapper.unwrapWFM;
import static org.knime.core.ui.wrapper.Wrapper.wraps;

import java.security.NoSuchAlgorithmException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.GUIWorkflowCipherPrompt;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/** Action to set locking on metanode.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class LockMetaNodeAction extends AbstractNodeAction {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(LockMetaNodeAction.class);

    /** Action ID. */
    public static final String ID = "knime.action.meta_node_lock";


    /** Create new action based on given editor.
     * @param editor The associated editor.
     */
    public LockMetaNodeAction(final WorkflowEditor editor) {
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
     *
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "Lock...";
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Set password protection";
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/meta/metanode_lock.png");
    }

    /**
     * @return true, if underlying model instance of
     *         <code>WorkflowManager</code>, otherwise false
     */
    @Override
    protected boolean internalCalculateEnabled() {
        if (getManager().isWriteProtected()) {
            return false;
        }
        NodeContainerEditPart[] nodes =
            getSelectedParts(NodeContainerEditPart.class);
        if (nodes.length != 1) {
            return false;
        }
        Object model = nodes[0].getModel();
        if (!wraps(model, WorkflowManager.class)) {
            // action not yet supported by general UI workflow manager
            return false;
        }
        if (model instanceof WorkflowManagerUI) {
            WorkflowManagerUI metaNode = (WorkflowManagerUI)model;
            if (metaNode.isWriteProtected()) {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodes) {
        if (nodes.length != 1) {
            return;
        }
        Object model = nodes[0].getModel();
        if (!(model instanceof WorkflowManagerUI)) {
            return;
        }
        WorkflowManagerUI metaNodeWFM = (WorkflowManagerUI)model;
        final Shell shell = SWTUtilities.getActiveShell();
        if (!unwrapWFM(metaNodeWFM).unlock(new GUIWorkflowCipherPrompt(false))) {
            return;
        }
        LockMetaNodeDialog lockDialog = new LockMetaNodeDialog(shell, unwrapWFM(metaNodeWFM), false);
        if (lockDialog.open() != Window.OK) {
            return;
        }
        String password = lockDialog.getPassword();
        String hint = lockDialog.getPasswordHint();
        try {
            unwrapWFM(metaNodeWFM).setWorkflowPassword(password, hint);
        } catch (NoSuchAlgorithmException e) {
            String msg = "Unable to encrypt metanode: " + e.getMessage();
            LOGGER.error(msg, e);
            MessageDialog.openError(shell, "Metanode encrypt", msg);
        }
    }

}
