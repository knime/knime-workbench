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
 *   17.07.2013 (Peter Ohl): created.
 */
package org.knime.workbench.editor2.actions;

import java.net.URI;
import java.nio.file.Path;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.ClassUtils;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.Role;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.contextv2.AnalyticsPlatformExecutorInfo;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.core.util.Pair;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.commands.BulkChangeMetaNodeLinksCommand;
import org.knime.workbench.editor2.commands.ChangeSubNodeLinkCommand;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider.LinkType;

/**
 * Allows changing the type of the template link of a sub node.
 * @author Peter Ohl, KNIME AG, Zurich, Switzerland
 */
public class ChangeSubNodeLinkAction extends AbstractNodeAction {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ChangeSubNodeLinkAction.class);

    /** id of this action. */
    public static final String ID = "knime.action.sub_node_relink";

    /**
     * @param editor the current workflow editor
     */
    public ChangeSubNodeLinkAction(final WorkflowEditor editor) {
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
        return "Change Link Type...";
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Change the link type to the shared component";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/meta/metanode_setname.png");
    }

    /**
     * @return true, if underlying model instance of <code>WorkflowManager</code>, otherwise false
     */
    @Override
    protected boolean internalCalculateEnabled() {
        NodeContainerEditPart[] nodes = getSelectedParts(NodeContainerEditPart.class);
        if (nodes.length != 1) {
            return false;
        }
        NodeContainer nc = Wrapper.unwrapNC(nodes[0].getNodeContainer());
        if (!(nc instanceof SubNodeContainer)) {
            return false;
        }
        SubNodeContainer subNode = (SubNodeContainer)nc;
        if (subNode.getParent().isWriteProtected()) {
            // the subnode's parent must not forbid the change
            return false;
        }

        var templateInfo = subNode.getTemplateInformation();
        var host = templateInfo.getSourceURI() != null ? templateInfo.getSourceURI().getHost() : null;
        var provider = ExplorerMountTable.getMountedContent().get(host);

        // the subnode must be linked and the contentprovider must be non-null
        return templateInfo.getRole() == Role.Link && provider != null;
    }

    /** {@inheritDoc} */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        if (nodeParts.length < 1) {
            return;
        }

        SubNodeContainer subNode = Wrapper.unwrap(nodeParts[0].getNodeContainer(), SubNodeContainer.class);
        if (subNode.getTemplateInformation().getRole() == Role.Link) {
            WorkflowManager wfm = subNode.getParent();
            var targetURI = subNode.getTemplateInformation().getSourceURI();

            /**
             * Do all further API calls to check if the URI has ONE of the following attributes:
             * - is a mountpoint-relative URI
             * - is a workflow-relative URI
             * - is a absolute URI and the local mountpoint's contentprovider equals the one of the target filestore
             */
            var linkType = BulkChangeMetaNodeLinksCommand.resolveLinkType(targetURI);
            switch (linkType) {
                case MountpointRelative:
                case WorkflowRelative:
                    // continue to change link
                    break;
                case Absolute:
                    if (!checkAbsoluteURIValidity(subNode, targetURI)) {
                        return;
                    }
                    break;
                case None:
                default:
                    // abort changing link without message - existing behavior
                    return;
            }

            String msg = "This is a linked (read-only) component. Only the link type can be changed.\n";
            msg += "Please select the new type of the link to the shared component.\n";
            msg += "(current type: " + linkType + ", current link: " + targetURI + ")\n";
            msg += "The origin of the component will not be changed - just the way it is referenced.";
            LinkPrompt dlg = new LinkPrompt(getEditor().getSite().getShell(), msg, linkType);
            dlg.open();
            if (dlg.getReturnCode() == Window.CANCEL) {
                return;
            }
            var newLinkType = dlg.getLinkType();
            if (linkType == newLinkType) {
                LOGGER.info("Link type not changes as selected type equals existing type " + targetURI);
                return;
            }
            // as the workflow is local and the template in the same mountID, it should resolve to a file
            var newURI = BulkChangeMetaNodeLinksCommand.changeLinkType(subNode, targetURI, newLinkType);
            if (newURI != null) {
                var cmd = new ChangeSubNodeLinkCommand(wfm, subNode, targetURI, newURI);
                getCommandStack().execute(cmd);
            }
        } else {
            throw new IllegalStateException(
                "Can only change the link type of if the component is actually linked - " + subNode + " is not.");
        }
    }

    /**
     * Checks the validity of an absolute URI for changing the link (aka performing the action).
     * @param targetURI the subnode's source URI
     * @return is valid?
     */
    private boolean checkAbsoluteURIValidity(final SubNodeContainer subNode, final URI targetURI) {
        // check if the contentproviders match and if the filestore is resolvable
        final var wfc = subNode.getProjectWFM().getContextV2();
        final var mountpointRoot = ClassUtils.castOptional(AnalyticsPlatformExecutorInfo.class, wfc.getExecutorInfo())
                .flatMap(AnalyticsPlatformExecutorInfo::getMountpoint)
                .map(Pair::getSecond)
                .map(Path::toFile)
                .orElse(null);
        LocalExplorerFileStore fs = ExplorerFileSystem.INSTANCE.fromLocalFile(mountpointRoot);
        var localProvider = fs != null ? fs.getContentProvider() : null;

        AbstractExplorerFileStore targetFs = ExplorerFileSystem.INSTANCE.getStore(targetURI);
        var targetProvider = targetFs != null ? targetFs.getContentProvider() : null;

        if (localProvider == null || targetProvider == null || !localProvider.equals(targetProvider)) {
            String message = "Cannot change component link of node: " + subNode.getNameWithID() + ".\n"
                + "You can only change the link for a component source on a local mountpoint.";
            MessageDialog.openError(getEditor().getSite().getShell(), "Change Component Link", message);
            return false;
        }
        return true;
    }

    public static class LinkPrompt extends MessageDialog {
        private Button m_absoluteLink;

        private Button m_mountpointRelativeLink;

        private Button m_workflowRelativeLink;

        private LinkType m_linkType;

        private LinkType m_preSelect;

        /**
         *
         */
        public LinkPrompt(final Shell parentShell, final String message, final LinkType preSelect) {
            super(parentShell, "Change Link Type to Shared Component", null, message,
                MessageDialog.QUESTION_WITH_CANCEL, new String[]{IDialogConstants.OK_LABEL,
                    IDialogConstants.CANCEL_LABEL}, 0);
            setShellStyle(getShellStyle() | SWT.SHEET);
            if (preSelect != null) {
                m_preSelect = preSelect;
                m_linkType = preSelect;
            } else {
                m_preSelect = LinkType.Absolute;
                m_linkType = LinkType.Absolute;
            }
        }

        /**
         * After the dialog closes get the selected link type.
         *
         * @return null, if no link should be created, otherwise the selected link type.
         */
        public LinkType getLinkType() {
            return m_linkType;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Control createCustomArea(final Composite parent) {
            Composite group = new Composite(parent, SWT.NONE);
            group.setLayout(new GridLayout(2, true));
            group.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));

            Label l1 = new Label(group, SWT.NONE);
            l1.setText("Select the new type of the link:");
            m_absoluteLink = new Button(group, SWT.RADIO);
            m_absoluteLink.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
            m_absoluteLink.setText("Create absolute link");
            m_absoluteLink.setToolTipText("If you move the workflow to a new location it will "
                + "always link back to its shared component");
            m_absoluteLink.setSelection(LinkType.Absolute.equals(m_preSelect));
            m_absoluteLink.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    m_linkType = LinkType.Absolute;
                }
            });

            new Label(group, SWT.NONE);
            m_mountpointRelativeLink = new Button(group, SWT.RADIO);
            m_mountpointRelativeLink.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
            m_mountpointRelativeLink.setText("Create mountpoint-relative link");
            m_mountpointRelativeLink.setToolTipText("If you move the workflow to a new workspace - the shared "
                + "component must be available on this new workspace as well");
            m_mountpointRelativeLink.setSelection(LinkType.MountpointRelative.equals(m_preSelect));
            m_mountpointRelativeLink.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    m_linkType = LinkType.MountpointRelative;
                }
            });

            new Label(group, SWT.NONE);
            m_workflowRelativeLink = new Button(group, SWT.RADIO);
            m_workflowRelativeLink.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
            m_workflowRelativeLink.setText("Create workflow-relative link");
            m_workflowRelativeLink.setToolTipText("Workflow and Component should always be moved together");
            m_workflowRelativeLink.setSelection(LinkType.WorkflowRelative.equals(m_preSelect));
            m_workflowRelativeLink.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    m_linkType = LinkType.WorkflowRelative;
                }
            });
            return group;
        }
    }

}
