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
 *   12.05.2010 (Bernd Wiswedel): created
 */
package org.knime.workbench.editor2.actions;

import static org.knime.core.ui.wrapper.Wrapper.unwrap;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.UI;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.commands.DisconnectSubNodeLinkCommand;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.dialogs.MessageJobFilter;
import org.knime.workbench.explorer.dialogs.SpaceResourceSelectionDialog;
import org.knime.workbench.explorer.dialogs.Validator;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileInfo;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ContentObject;

/**
 * Action to save a sub node as template (requires KNIME TeamSpace feature).
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class SaveAsSubNodeTemplateAction extends AbstractNodeAction {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(SaveAsSubNodeTemplateAction.class);

    /** Action ID. */
    public static final String ID = "knime.action.sub_node_save_as_template";

    /** Create new action based on given editor.
     * @param editor The associated editor. */
    public SaveAsSubNodeTemplateAction(final WorkflowEditor editor) {
        super(editor);
    }

    /** {@inheritDoc} */
    @Override
    public String getId() {
        return ID;
    }

    /** {@inheritDoc} */
    @Override
    public String getText() {
        return "Share...";
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Saves the component as a reusable shared component";
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/meta/metanode_link_create.png");
    }

    /**
     * @return true, if underlying model instance of
     *         <code>SubNodeContainer</code> and there is no link associated
     *         with it, otherwise false
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
        if (Wrapper.wraps(model, SubNodeContainer.class)) {
            SubNodeContainer snc = unwrap((UI)model, SubNodeContainer.class);
            switch (snc.getTemplateInformation().getRole()) {
            case None:
                break;
            default:
                return false;
            }
            for (AbstractContentProvider p
                    : ExplorerMountTable.getMountedContent().values()) {
                if (p.canHostComponentTemplates()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodes) {
        if (nodes.length < 1) {
            return;
        }

        SubNodeContainer snc = unwrap(nodes[0].getNodeContainer(), SubNodeContainer.class);
        WorkflowManager wm = snc.getWorkflowManager();

        List<String> validMountPointList = new ArrayList<String>();
        for (Map.Entry<String, AbstractContentProvider> entry
                : ExplorerMountTable.getMountedContent().entrySet()) {
            AbstractContentProvider contentProvider = entry.getValue();
            if (contentProvider.isWritable() && contentProvider.canHostComponentTemplates()) {
                validMountPointList.add(entry.getKey());
            }
        }
        if (validMountPointList.isEmpty()) {
            throw new IllegalStateException("No valid mount points found - "
                    + "this is inconsistent with calculateEnabled()");
        }
        String[] validMountPoints = validMountPointList.toArray(new String[0]);
        final Shell shell = SWTUtilities.getActiveShell();
        ContentObject defSel = getDefaultSaveLocation(snc);
        DestinationSelectionDialog dialog = new DestinationSelectionDialog(shell, validMountPoints, defSel);
        if (dialog.open() != Window.OK) {
            return;
        }
        AbstractExplorerFileStore target = dialog.getSelection();
        AbstractContentProvider contentProvider = target.getContentProvider();
        AtomicReference<PortObject[]> exampleInputData = new AtomicReference<PortObject[]>();
        if (dialog.m_isIncludeInputData) {
            //fetch input data
            IProgressService ps = PlatformUI.getWorkbench().getProgressService();
            try {
                ps.run(true, false, mon -> {
                    mon.setTaskName("Executing upstream nodes ...");
                    // since 5.5, the "fetchInputDataFromParent" method is not executing the workflow anymore
                    snc.getParent().executePredecessorsAndWait(snc.getID());
                    exampleInputData.set(snc.fetchInputDataFromParent());
                });
            } catch (InvocationTargetException e) {
                String error =
                    "An error occurred while executing upstream nodes: " + e.getTargetException().getMessage();
                LOGGER.warn(error, e.getTargetException());
                MessageDialog.openError(shell, "Problem saving component with example input data", error);
            } catch (InterruptedException e) {
                String error = "Execution of upstream nodes interrupted: " + e.getMessage();
                LOGGER.warn(error, e);
                MessageDialog.openError(shell, "Problem saving component with example input data", error);
            }
            if (exampleInputData.get() == null) {
                MessageDialog.openError(shell, "Problem saving component with example input data",
                    "No data available at the component's input ports most likely because the exeuction of upstream nodes failed.");
                return;
            }
        }
        contentProvider.saveSubNodeTemplate(snc, target, exampleInputData.get());
    }

    private static ContentObject getDefaultSaveLocation(final SubNodeContainer snc) {
        final NodeID id = snc.getID();
        URI uri = DisconnectSubNodeLinkCommand.RECENTLY_USED_URIS.get(id);
        if (uri == null || !ExplorerFileSystem.SCHEME.equals(uri.getScheme())) {
            return null;
        }
        final AbstractExplorerFileStore oldTemplateFileStore =
            ExplorerFileSystem.INSTANCE.getStore(uri);
        final AbstractExplorerFileStore parent = oldTemplateFileStore == null
            ? null : oldTemplateFileStore.getParent();
        if (parent != null) {
            return ContentObject.forFile(parent);
        }
        return null;
    }

    /** Dialog to select the mountpoint + destination folder. Also contains a checkbox whether to include input data. */
    private static final class DestinationSelectionDialog extends SpaceResourceSelectionDialog {

        private Button m_includeInputDataButton;

        private boolean m_isIncludeInputData;

        /**
         * @param parentShell
         * @param mountIDs
         * @param initialSelection
         */
        public DestinationSelectionDialog(final Shell parentShell, final String[] mountIDs,
            final ContentObject initialSelection) {
            super(parentShell, mountIDs, initialSelection);
            setTitle("Save As Shared Component");
            setHeader("Select destination workflow group for shared component");
            setValidator(new Validator() {
                @Override
                public String validateSelectionValue(final AbstractExplorerFileStore selection, final String name) {
                    final AbstractExplorerFileInfo info = selection.fetchInfo();
                    if (info.isWorkflowGroup()) {
                        return null;
                    }
                    return "Only workflow groups can be selected as target.";
                }
            });
            setFilter(new MessageJobFilter());
        }

        @Override
        protected void createCustomFooterField(final Composite parent) {
            m_includeInputDataButton = new Button(parent, SWT.CHECK);
            m_isIncludeInputData = false;
            m_includeInputDataButton.setSelection(m_isIncludeInputData);
            m_includeInputDataButton.setText("Include input data with component");
            m_includeInputDataButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    Button b = (Button)e.widget;
                    m_isIncludeInputData = b.getSelection();
                }
            });
            Label hint = new Label(parent, SWT.NONE);
            hint.setText("Including input data in a component facilitates their direct editing later on.\n"
                + "Please note that upstream nodes need to be executed (or will be executed on save)\nif input data is to be included. "
                + "It is advised to keep the input data as small as possible.");
        }
    }
}
