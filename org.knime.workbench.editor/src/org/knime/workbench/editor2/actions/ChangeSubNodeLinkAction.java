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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import org.knime.core.node.workflow.MetaNodeTemplateInformation.Role;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.core.util.KnimeUrlType;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.urlresolve.KnimeUrlResolver;
import org.knime.core.util.urlresolve.URLResolverUtil;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.commands.ChangeSubNodeLinkCommand;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Allows changing the type of the template link of a sub node.
 *
 * @author Peter Ohl, KNIME AG, Zurich, Switzerland
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
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
        return "Change the link type to the shared component";
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
        return extractLinkedComponent(getSelectedParts(NodeContainerEditPart.class)) //
                .flatMap(ChangeSubNodeLinkAction::getURLsIfValid) //
                .isPresent();
    }


    static Optional<SubNodeContainer> extractLinkedComponent(final NodeContainerEditPart[] nodes) {
        if (nodes.length != 1) {
            return Optional.empty();
        }
        final var nodeContainer = Wrapper.unwrapNC(nodes[0].getNodeContainer());
        if (!(nodeContainer instanceof SubNodeContainer)) {
            return Optional.empty();
        }
        final var subNode = (SubNodeContainer)nodeContainer;
        final var templateInfo = subNode.getTemplateInformation();
        if (subNode.getParent().isWriteProtected() || templateInfo.getRole() != Role.Link) {
            // the subnode's parent must not forbid the change
            return Optional.empty();
        }

        return Optional.of(subNode);
    }

    static Optional<Map<KnimeUrlType, URL>> getURLsIfValid(final SubNodeContainer subNode) {

        final var templateInfo = subNode.getTemplateInformation();
        final var templateUri = templateInfo.getSourceURI();
        final var optLinkType = KnimeUrlType.getType(templateUri);
        if (optLinkType.isEmpty()) {
            return Optional.empty();
        }

        final var linkType = optLinkType.get();
        final var context = Optional.of(subNode.getWorkflowManager()) //
                .map(wfm -> wfm.getProjectComponent().map(SubNodeContainer::getWorkflowManager) //
                    .orElse(wfm.getProjectWFM())) //
                .map(WorkflowManager::getContextV2) //
                .orElseThrow(() -> new IllegalStateException("Could not find workflow context for " + subNode));

        try {
            final var urls = KnimeUrlResolver.getResolver(context).changeLinkType(URLResolverUtil.toURL(templateUri));
            if (urls.size() > (urls.containsKey(linkType) ? 1 : 0)) {
                // there are other options available
                return Optional.of(urls);
            }
        } catch (ResourceAccessException e) {
            LOGGER.debug(() -> "Cannot compute alternative KNIME URL types for '"
                    + templateUri + "': " + e.getMessage(), e);
        }

        return Optional.empty();
    }

    static final String getLinkTypeName(final KnimeUrlType type) {
        return switch (type) {
            case MOUNTPOINT_ABSOLUTE -> "mountpoint-absolute";
            case HUB_SPACE_RELATIVE -> "space-relative";
            case MOUNTPOINT_RELATIVE -> "mountpoint-relative";
            case NODE_RELATIVE -> "node-relative";
            case WORKFLOW_RELATIVE -> "workflow-relative";
        };
    }

    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        if (nodeParts.length < 1) {
            return;
        }

        final var subNode = extractLinkedComponent(nodeParts) //
                .orElseThrow(() -> new IllegalStateException("Current selection is not a linked component."));
        final var changeOptions = getURLsIfValid(subNode) //
                .orElseThrow(() -> new IllegalStateException("Can't change link type of the current selection."));

        final var linkUrl = subNode.getTemplateInformation().getSourceURI();
        final var linkType = KnimeUrlType.getType(linkUrl).orElseThrow();
        final var message = "This is a linked (read-only) component. Only the link type can be changed.\n"
            + "Please select the new type of the link to the shared component.\n" + "(current type: "
            + getLinkTypeName(linkType) + ", current link: " + linkUrl + ")\n"
            + "The origin of the component will not be changed - just the way it is referenced.";

        final var dialog = new LinkPrompt(getEditor().getSite().getShell(), message, changeOptions, linkType);
        final var newUri = showDialogAndGetUri(dialog, linkUrl, linkType, changeOptions);
        if (newUri.isPresent()) {
            final var cmd = new ChangeSubNodeLinkCommand(subNode.getParent(), subNode,
                linkUrl, null, newUri.get(), null);
            getCommandStack().execute(cmd);
        }
    }

    /**
     * Displays a dialog to the user presenting the different representations of the URL that are available.
     *
     * @param shell context in which the dialog is opened
     * @param linkUrl initial link URL
     * @param linkType initial link type
     * @param message text displayed in the body of the dialog
     * @param options possible options for representing the initial link URL
     * @return new URI if the user chose one, {@link Optional#empty()} otherwise
     */
    public static Optional<URI> showDialogAndGetUri(final Shell shell, final URI linkUrl, final KnimeUrlType linkType,
            final String message, final Map<KnimeUrlType, URL> options) {
        final var dialog = new LinkPrompt(shell, message, options, linkType);
        return showDialogAndGetUri(dialog, linkUrl, linkType, options);
    }

    static Optional<URI> showDialogAndGetUri(final LinkPrompt dialog, final URI linkUrl,
            final KnimeUrlType linkType, final Map<KnimeUrlType, URL> options) {

        dialog.open();
        if (dialog.getReturnCode() == Window.CANCEL) {
            return Optional.empty();
        }

        var newLinkType = dialog.getLinkType();
        if (linkType == newLinkType) {
            LOGGER.info("Link type not changed as selected type equals existing type " + linkUrl);
            return Optional.empty();
        }

        final var newUrl = options.get(newLinkType);
        try {
            return Optional.of(newUrl.toURI());
        } catch (final URISyntaxException e) {
            LOGGER.debug(() -> "Cannot convert KNIME URL'" + newUrl + "' to URI: " + e.getMessage(), e);
            return Optional.empty();
        }
    }

    static class LinkPrompt extends MessageDialog {

        private static final List<KnimeUrlType> ORDER = List.of(
            KnimeUrlType.MOUNTPOINT_ABSOLUTE,
            KnimeUrlType.HUB_SPACE_RELATIVE,
            KnimeUrlType.MOUNTPOINT_RELATIVE,
            KnimeUrlType.WORKFLOW_RELATIVE,
            KnimeUrlType.NODE_RELATIVE);

        record LinkTypeOption(String desc, String tooltip) {}

        private final Map<KnimeUrlType, URL> m_options;

        private KnimeUrlType m_linkType;

        private KnimeUrlType m_preSelect;

        LinkPrompt(final Shell parentShell, final String messageText, final Map<KnimeUrlType, URL> options,
            final KnimeUrlType preSelect) {
            super(parentShell, "Change Link Type to Shared Component", null, messageText,
                MessageDialog.QUESTION_WITH_CANCEL, new String[]{ IDialogConstants.OK_LABEL,
                    IDialogConstants.CANCEL_LABEL }, 0);
            setShellStyle(getShellStyle() | SWT.SHEET); //NOSONAR
            m_options = options;
            if (preSelect != null) {
                m_preSelect = preSelect;
                m_linkType = preSelect;
            } else {
                m_preSelect = KnimeUrlType.MOUNTPOINT_ABSOLUTE;
                m_linkType = KnimeUrlType.MOUNTPOINT_ABSOLUTE;
                for (final var type : ORDER) {
                    if (options.containsKey(type)) {
                        m_preSelect = type;
                        m_linkType = type;
                        break;
                    }
                }
            }
        }

        LinkTypeOption getOptionTexts(final KnimeUrlType urlType) {
            return switch (urlType) {
                case MOUNTPOINT_ABSOLUTE -> new LinkTypeOption("Create absolute link",
                    "If you move the workflow to a new location it will always link back to its shared component");
                case HUB_SPACE_RELATIVE -> new LinkTypeOption("Create space-relative link",
                    "If you move the workflow to another space, the shared component must be available in the same"
                            + " location relative to the space's root");
                case MOUNTPOINT_RELATIVE -> new LinkTypeOption("Create mountpoint-relative link",
                    "If you move the workflow to a new workspace, the shared component must be available on this new"
                            + " workspace as well");
                case WORKFLOW_RELATIVE -> new LinkTypeOption("Create workflow-relative link",
                    "Workflow and Component should always be moved together");
                case NODE_RELATIVE -> new LinkTypeOption("Create node-relative link",
                    "Addresses a resource relative to the current node's directory");
            };
        }

        /**
         * After the dialog closes get the selected link type.
         *
         * @return null, if no link should be created, otherwise the selected link type.
         */
        public KnimeUrlType getLinkType() {
            return m_linkType;
        }

        @Override
        protected Control createCustomArea(final Composite parent) {
            final var group = new Composite(parent, SWT.NONE);
            group.setLayout(new GridLayout(1, false));
            group.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
            final var top = new Label(group, SWT.NONE);
            top.setText("Select the new type of the link:");

            for (final var type : ORDER) {
                final var url = m_options.get(type);
                if (url != null) {
                    final var option = getOptionTexts(type);
                    final var radioButton = new Button(group, SWT.RADIO);
                    radioButton.setToolTipText(option.tooltip());
                    radioButton.setText(option.desc() + ":  " + url);
                    radioButton.setSelection(m_preSelect == type);
                    radioButton.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(final SelectionEvent e) {
                            super.widgetSelected(e);
                            m_linkType = type;
                        }
                    });
                }
            }

            return group;
        }
    }
}
