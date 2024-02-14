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
 *   17.07.2013 (Peter Ohl): created.
 */
package org.knime.workbench.editor2.actions;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.Role;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.core.util.KnimeUrlType;
import org.knime.core.util.Pair;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.urlresolve.KnimeUrlResolver;
import org.knime.core.util.urlresolve.URLResolverUtil;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.commands.ChangeMetaNodeLinkCommand;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Allows changing the type of the template link of a metanode.
 * @author Peter Ohl, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("restriction")
public class ChangeMetaNodeLinkAction extends AbstractNodeAction {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ChangeMetaNodeLinkAction.class);

    /** id of this action. */
    public static final String ID = "knime.action.meta_node_relink";

    /**
     * @param editor the current workflow editor
     */
    public ChangeMetaNodeLinkAction(final WorkflowEditor editor) {
        super(editor);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getText() {
        return "Change Link Type...";
    }

    @Override
    public String getToolTipText() {
        return "Change the link type to the shared metanode";
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/meta/metanode_setname.png");
    }

    /**
     * @return true, if underlying model instance of <code>WorkflowManager</code>, otherwise false
     */
    @Override
    protected boolean internalCalculateEnabled() {
        return getMetanodeAndURLsIfValid(getSelectedParts(NodeContainerEditPart.class)).isPresent();
    }

    private static Optional<Pair<WorkflowManager, Map<KnimeUrlType, URL>>>
            getMetanodeAndURLsIfValid(final NodeContainerEditPart[] nodes) { //NOSONAR too many returns
        if (nodes.length != 1) {
            return Optional.empty();
        }
        NodeContainerUI nc = nodes[0].getNodeContainer();
        if (!(nc instanceof WorkflowManagerUI && Wrapper.wraps(nc, WorkflowManager.class))) {
            //action not yet supported for the general UI workflow manager
            return Optional.empty();
        }

        final var metaNode = (WorkflowManagerUI)nc;
        final var metaNodeWFM = Wrapper.unwrapWFM(metaNode);
        var templateInfo = metaNodeWFM.getTemplateInformation();
        if (metaNode.getParent().isWriteProtected() || templateInfo.getRole() != Role.Link) {
            // the subnode's parent must not forbid the change
            return Optional.empty();
        }

        final var templateUri = templateInfo.getSourceURI();
        final var optLinkType = KnimeUrlType.getType(templateUri);
        if (optLinkType.isEmpty()) {
            return Optional.empty();
        }

        final var linkType = optLinkType.get();
        final var context = Optional.of(metaNodeWFM) //
            .map(wfm -> wfm.getProjectComponent().map(SubNodeContainer::getWorkflowManager) //
                .orElse(wfm.getProjectWFM())) //
            .map(WorkflowManager::getContextV2) //
            .orElseThrow(() -> new IllegalStateException("Could not find workflow context for " + metaNode));

        try {
            final var urls = KnimeUrlResolver.getResolver(context).changeLinkType(URLResolverUtil.toURL(templateUri));
            if (urls.size() > (urls.containsKey(linkType) ? 1 : 0)) {
                // there are other options available
                return Optional.of(Pair.create(metaNodeWFM, urls));
            }
        } catch (ResourceAccessException e) {
            LOGGER.debug(
                () -> "Cannot compute alternative KNIME URL types for '" + templateUri + "': " + e.getMessage(), e);
        }

        return Optional.empty();
    }

    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        if (nodeParts.length < 1) {
            return;
        }

        final var validated = getMetanodeAndURLsIfValid(nodeParts) //
                .orElseThrow(() -> new IllegalStateException("Can't change link type of the current selection."));

        final var metaNode = validated.getFirst();

        final var linkUrl = metaNode.getTemplateInformation().getSourceURI();
        final var linkType = KnimeUrlType.getType(linkUrl).orElseThrow();
        final var linkTypeName = ChangeSubNodeLinkAction.getLinkTypeName(linkType);
        final var message = "This is a linked (read-only) Metanode. Only the link type can be changed.\n"
            + "Please select the new type of the link to the metanode.\n"
            + "(current type: " + linkTypeName + ", current link: " + linkUrl + ")\n"
            + "The origin of the template will not be changed - just the way it is referenced.";

        final var options = validated.getSecond();
        final var dialog = new LinkPrompt(getEditor().getSite().getShell(), message, options, linkType);
        dialog.open();
        if (dialog.getReturnCode() == Window.CANCEL) {
            return;
        }

        var newLinkType = dialog.getLinkType();
        if (linkType == newLinkType) {
            LOGGER.info("Link type not changes as selected type equals existing type " + linkUrl);
            return;
        }

        final var newUrl = options.get(newLinkType);
        try {
            var cmd = new ChangeMetaNodeLinkCommand(metaNode.getParent(), metaNode, linkUrl, newUrl.toURI());
            getCommandStack().execute(cmd);
        } catch (final URISyntaxException e) {
            LOGGER.debug(() -> "Cannot convert KNIME URL'" + newUrl + "' to URI: " + e.getMessage(), e);
        }
    }

    static class LinkPrompt extends ChangeSubNodeLinkAction.LinkPrompt {

        /**
         * @param parentShell
         * @param messageText
         * @param options
         * @param preSelect
         */
        public LinkPrompt(final Shell parentShell, final String messageText, final Map<KnimeUrlType, URL> options,
            final KnimeUrlType preSelect) {
            super(parentShell, messageText, options, preSelect);
        }

        @Override
        LinkTypeOption getOptionTexts(final KnimeUrlType urlType) {
            return switch (urlType) {
                case MOUNTPOINT_ABSOLUTE -> new LinkTypeOption("Create absolute link",
                    "If you move the workflow to a new location it will always link back to this template");
                case HUB_SPACE_RELATIVE -> new LinkTypeOption("Create space-relative link",
                    "If you move the workflow to another space, the metanode must be available in the same"
                            + " location relative to the space's root");
                case MOUNTPOINT_RELATIVE -> new LinkTypeOption("Create mountpoint-relative link",
                    "If you move the workflow to a new workspace - the metanode template must be available on this new"
                            + " workspace as well");
                case WORKFLOW_RELATIVE -> new LinkTypeOption("Create workflow-relative link",
                    "Workflow and metanode should always be moved together");
                case NODE_RELATIVE -> new LinkTypeOption("Create node-relative link",
                    "Addresses a resource relative to the current node's directory");
            };
        }
    }

}
