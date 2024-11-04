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
 * -------------------------------------------------------------------
 *
 */
package org.knime.workbench.editor2.actions;

import org.eclipse.jface.resource.ImageDescriptor;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.ui.node.workflow.NativeNodeContainerUI;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.core.webui.node.dialog.NodeDialogManager;
import org.knime.core.webui.node.dialog.SubNodeContainerDialogFactory;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeDialog;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Action to open the dialog of a node.
 *
 * @author Florian Georg, University of Konstanz
 */
public class OpenDialogAction extends AbstractNodeAction {

    /**
     * The dialog type that is available for a specific node
     */
    public enum DialogType {
            /**
             * A modern JS-based dialog is available
             */
            MODERN,
            /**
            * A "classic" swing-based dialog is available
            */
            SWING,
            /**
            * No dialog is available at all
            */
            NONE
    }

    /** unique ID for this action. * */
    public static final String ID = "knime.action.openDialog";

    /**
     *
     * @param editor The workflow editor
     */
    public OpenDialogAction(final WorkflowEditor editor) {
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
     */
    @Override
    public String getText() {
        return "Configure...\t" + getHotkey("knime.commands.openDialog");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/openDialog.gif");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Open configuration dialog for this node";
    }

    /**
     * @return <code>true</code> if at we have a single node which has a dialog
     * @see org.eclipse.gef.ui.actions.WorkbenchPartAction#calculateEnabled()
     */
    @Override
    protected boolean internalCalculateEnabled() {
        NodeContainerEditPart[] selected = getSelectedParts(NodeContainerEditPart.class);
        if (selected.length != 1) {
            return false;
        }

        NodeContainerEditPart part = selected[0];
        var nc = part.getNodeContainer();
        return getDialogType(nc) != DialogType.NONE;
    }

    /**
     * Get the {@link DialogType} that is provided by a given {@link NodeContainerUI}.
     *
     * @param nc The wrapped node container
     * @return The {@link DialogType} that is provided by this node container
     */
    public static DialogType getDialogType(final NodeContainerUI ncUI) {
        var nc = Wrapper.unwrapNCOptional(ncUI).orElse(null);
        if (nc != null) {
            if (nc instanceof SubNodeContainer && SubNodeContainerDialogFactory.isSubNodeContainerNodeDialogEnabled()) {
                if (NodeDialogManager.hasNodeDialog(nc)) {
                    return DialogType.MODERN;
                } else {
                    return DialogType.NONE;
                }
            } else if (nc instanceof NativeNodeContainer && NodeDialogManager.hasNodeDialog(nc)) {
                return DialogType.MODERN;
            }
        } else if (ncUI instanceof NativeNodeContainerUI nncUI) {
            var dialog = nncUI.getNodeDialog().orElse(null);
            if (dialog instanceof DefaultNodeDialog) {
                return DialogType.MODERN;
            } else if (dialog != null) {
                return DialogType.NONE;
            }
        }
        return ncUI.hasDialog() ? DialogType.SWING : DialogType.NONE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        if (nodeParts.length > 0) {
            final NodeContainerEditPart nodeContainerEditPart = nodeParts[0];
            nodeContainerEditPart.openNodeDialog();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean canHandleWorkflowManagerUI() {
        return true;
    }
}
