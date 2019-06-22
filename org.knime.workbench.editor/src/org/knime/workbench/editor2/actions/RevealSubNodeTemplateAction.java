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
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.workflow.MetaNodeTemplateInformation;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.LinkType;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.Role;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.ui.UI;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ContentDelegator;
import org.knime.workbench.explorer.view.ExplorerView;

/**
 * Action to reveal the template of a linked sub node.
 * @author Dominik Morent, KNIME AG, Zurich, Switzerland
 */
public class RevealSubNodeTemplateAction extends AbstractNodeAction {
    /** Action ID. */
    public static final String ID = "knime.action.sub_node_reveal_template";

    /** Create new action based on given editor.
     * @param editor The associated editor. */
    public RevealSubNodeTemplateAction(final WorkflowEditor editor) {
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
        return "Select in Explorer";
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Selects the shared component in the KNIME explorer";
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/meta/metanode_link_reveal.png");
    }

    /**
     * @return true, if underlying model instance of
     *         <code>WorkflowManager</code>, otherwise false
     */
    @Override
    protected boolean internalCalculateEnabled() {
        NodeContainerEditPart[] nodes =
            getSelectedParts(NodeContainerEditPart.class);
        if (nodes == null) {
            return false;
        }
        for (NodeContainerEditPart p : nodes) {
            Object model = p.getModel();
            if (wraps(model, SubNodeContainer.class)) {
                SubNodeContainer snc = unwrap((UI)model, SubNodeContainer.class);
                MetaNodeTemplateInformation templateInfo = snc.getTemplateInformation();

                final URI uri = templateInfo.getSourceURI();
                final AbstractContentProvider provider = ExplorerMountTable.getMountedContent().get(uri.getHost());

                if (provider == null) {
                    return false;
                }

                final AbstractExplorerFileStore fileStore = provider.getFileStore(uri);
                final AbstractExplorerFileStore rootStore = provider.getRootStore();
                final String rootPath =
                    rootStore.getFullName().endsWith("/") ? rootStore.getFullName() : rootStore.getFullName() + "/";

                /* To check if this action is enabled firstly check if the component exists and
                 * if it's actually part of the mount point. This can be easily tested by getting the root of the mount
                 * point and check if the Component is a descendant of the root, which is reflected by the full name. */
                if (templateInfo.getRole().equals(Role.Link) && templateInfo.getLinkType().equals(LinkType.Knime)
                    && fileStore.getFullName().startsWith(rootPath) && fileStore.fetchInfo().exists()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodes) {
        List<NodeID> candidateList = new ArrayList<NodeID>();
        List<AbstractExplorerFileStore> templates
                = new ArrayList<AbstractExplorerFileStore>();
        for (NodeContainerEditPart p : nodes) {
            Object model = p.getModel();
            if (wraps(model, SubNodeContainer.class)) {
                NodeContext.pushContext(Wrapper.unwrapNC(p.getNodeContainer()));
                try {
                    SubNodeContainer snc = unwrap((UI)model, SubNodeContainer.class);
                    MetaNodeTemplateInformation i = snc.getTemplateInformation();
                    if (Role.Link.equals(i.getRole())) {
                        candidateList.add(snc.getID());
                        AbstractExplorerFileStore template = ExplorerFileSystem.INSTANCE.getStore(i.getSourceURI());
                        if (template != null) {
                            templates.add(template);
                        }
                    }
                } finally {
                    NodeContext.removeLastContext();
                }
            }
        }
        List<Object> treeObjects
                = ContentDelegator.getTreeObjectList(templates);
        if (treeObjects != null && treeObjects.size() > 0) {
            IViewReference[] views = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage().getViewReferences();

            for (IViewReference view : views) {
                if (ExplorerView.ID.equals(view.getId())) {
                    ExplorerView explorerView
                            = (ExplorerView)view.getView(true);
                   explorerView.getViewer().setSelection(
                           new StructuredSelection(treeObjects), true);
                }
            }
        }

    }

}
