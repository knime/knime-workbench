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
 *   12.02.2012 (Dominik Morent): created
 */
package org.knime.workbench.editor2.actions;

import static org.knime.core.ui.wrapper.Wrapper.unwrap;
import static org.knime.core.ui.wrapper.Wrapper.wraps;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.MetaNodeTemplateInformation;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.Role;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.UI;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.core.util.KnimeUrlType;
import org.knime.core.util.exception.ResourceAccessException;
import org.knime.core.util.urlresolve.KnimeUrlResolver;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.knime.workbench.explorer.view.ContentDelegator;
import org.knime.workbench.explorer.view.ExplorerView;

/**
 * Action to reveal the template of a linked sub node.
 *
 * @author Dominik Morent, KNIME AG, Zurich, Switzerland
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public class RevealSubNodeTemplateAction extends AbstractNodeAction {
    /** Action ID. */
    public static final String ID = "knime.action.sub_node_reveal_template";

    /** Create new action based on given editor.
     * @param editor The associated editor. */
    public RevealSubNodeTemplateAction(final WorkflowEditor editor) {
        super(editor);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getText() {
        return "Select in Explorer";
    }

    @Override
    public String getToolTipText() {
        return "Selects the shared component in the KNIME explorer";
    }


    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/meta/metanode_link_reveal.png");
    }

    @Override
    protected boolean internalCalculateEnabled() {
        final var nodes = getSelectedParts(NodeContainerEditPart.class);
        if (nodes == null) {
            return false;
        }

        final var resolver = getEditor().getRootEditor().getWorkflowManager() //
                .map(WorkflowManager::getContextV2) //
                .map(KnimeUrlResolver::getResolver) //
                .orElse(null);

        for (final var p : nodes) {
            final var model = p.getModel();
            if (wraps(model, SubNodeContainer.class)) {
                final var templateInfo = unwrap((UI)model, SubNodeContainer.class).getTemplateInformation();
                if (templateInfo.getRole() != Role.Link) {
                    continue;
                }

                if (getAbsoluteKnimeUri(resolver, templateInfo) //
                        .map(URI::getHost) //
                        .filter(ExplorerMountTable.getMountedContent()::containsKey) //
                        .isPresent()) {
                    // the URL references a known mountpoint
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodes) { // NOSONAR
        final var resolver = getEditor().getRootEditor().getWorkflowManager() //
                .map(WorkflowManager::getContextV2) //
                .map(KnimeUrlResolver::getResolver) //
                .orElse(null);

        final var templates = new ArrayList<AbstractExplorerFileStore>();
        for (final var p : nodes) { // NOSONAR
            final var model = p.getModel();
            if (!wraps(model, SubNodeContainer.class)) {
                continue;
            }

            NodeContext.pushContext(Wrapper.unwrapNC(p.getNodeContainer()));
            try {
                Optional.of(unwrap((UI)model, SubNodeContainer.class)) //
                    .map(SubNodeContainer::getTemplateInformation) //
                    .filter(templateInfo -> templateInfo.getRole() == Role.Link)
                    .flatMap(templateInfo -> getAbsoluteKnimeUri(resolver, templateInfo)) //
                    .map(ExplorerFileSystem.INSTANCE::getStore) //
                    .ifPresent(templates::add);
            } finally {
                NodeContext.removeLastContext();
            }
        }

        final var selection = Optional.ofNullable(ContentDelegator.getTreeObjectList(templates)) //
            .filter(objs -> !objs.isEmpty()) //
            .map(StructuredSelection::new) //
            .orElse(null);

        if (selection != null) {
            Optional.ofNullable(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()) //
                .map(IWorkbenchPage::getViewReferences) //
                .flatMap(refs -> Arrays.stream(refs).filter(ref -> ExplorerView.ID.equals(ref.getId())).findAny()) //
                .ifPresent(viewRef -> ((ExplorerView)viewRef.getView(true)).getViewer().setSelection(selection, true));
        }
    }

    private static Optional<URI> getAbsoluteKnimeUri(final KnimeUrlResolver resolver,
            final MetaNodeTemplateInformation templateInfo) {
        var sourceURI = templateInfo.getSourceURI();
        if (resolver != null) {
            try {
                sourceURI = resolver.resolveToAbsolute(sourceURI).orElse(sourceURI);
            } catch (final ResourceAccessException e) {
                NodeLogger.getLogger(RevealSubNodeTemplateAction.class) //
                    .debug(() -> "Cannot resolve source URI '" + templateInfo.getSourceURI()
                        + "' to absolute: " + e.getMessage(), e);
            }
        }
        return KnimeUrlType.getType(sourceURI).orElse(null) == KnimeUrlType.MOUNTPOINT_ABSOLUTE
                ? Optional.of(sourceURI) : Optional.empty();
    }

}
