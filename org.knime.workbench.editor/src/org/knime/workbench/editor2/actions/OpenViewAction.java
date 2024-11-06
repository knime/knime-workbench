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
 * History
 *   25.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.actions;

import javax.swing.SwingUtilities;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.core.ui.wrapper.NativeNodeContainerWrapper;
import org.knime.core.webui.node.view.NodeViewManager;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;

/**
 * Action to open a view of a node.
 *
 * TODO: Embedd view in an eclipse view (preference setting)
 *
 * @author Florian Georg, University of Konstanz
 */
public class OpenViewAction extends Action {
    private final NodeContainer m_nodeContainer;

    private final int m_index;

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(OpenViewAction.class);

    /**
     * New action to opne a node view.
     *
     * @param nodeContainer The node
     * @param viewIndex The index of the node view
     */
    public OpenViewAction(final NodeContainer nodeContainer,
            final int viewIndex) {
        m_nodeContainer = nodeContainer;
        m_index = viewIndex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/openView.gif");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Opens node view " + m_index + ": "
                + m_nodeContainer.getViewName(m_index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "View: " + m_nodeContainer.getViewName(m_index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        LOGGER.debug("Open Node View " + m_nodeContainer.getName() + " (#" + m_index + ")");
        try {
            openNodeView();
        } catch (Throwable t) {
            MessageBox mb = new MessageBox(SWTUtilities.getActiveShell(), SWT.ICON_ERROR | SWT.OK);
            mb.setText("View cannot be opened");
            mb.setMessage("The view cannot be opened for the following reason:\n" + t.getMessage());
            mb.open();
            LOGGER.error("The view for node '" + m_nodeContainer.getNameWithID() + "' has thrown a '"
                + t.getClass().getSimpleName() + "'. That is most likely an " + "implementation error.", t);
        }
    }

    /**
     * Copy of org.knime.ui.java.api.NodeAPI#openNodeView
     * (accepting a copy since this code is going to be phased out)
     */
    private void openNodeView() {
        LOGGER.debug("Open Node View " + m_nodeContainer.getName() + " (#" + m_index + ")");
        final var nc = m_nodeContainer;
        if (nc instanceof SubNodeContainer snc) {
            // composite view
            OpenSubnodeWebViewAction.openView(snc);
        } else if (NodeViewManager.hasNodeView(nc)) {
            // 'ui-extension' view
            final var nnc = ((NativeNodeContainer)nc);
            final var viewName = "Interactive View: " + nnc.getNodeViewName(0);
            var nncWrapper = NativeNodeContainerWrapper.wrap(nnc);
            OpenNodeViewAction.openNodeView(nncWrapper, OpenNodeViewAction.createNodeView(nncWrapper, false, true),
                viewName);
        } else if (nc.getInteractiveWebViews().size() > 0 || nc.hasInteractiveView()) {
            // legacy js-view
            OpenInteractiveWebViewAction.openView((NativeNodeContainer)nc,
                nc.getInteractiveWebViews().get(0).getViewName());
        } else if (nc.getNrNodeViews() > m_index) {
            // swing-based view
            final var title = nc.getViewName(m_index) + " - " + nc.getDisplayLabel();
            final var knimeWindowBounds = getAppBoundsAsAWTRec();
            SwingUtilities.invokeLater(() -> Node.invokeOpenView(nc.getView(m_index), title, knimeWindowBounds));
        } else {
            throw new IllegalStateException(String.format(
                "Node with id '%s' in workflow '%s' does not have a node view", nc.getID(), nc.getParent().getName()));
        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return "knime.open.view.action";
    }

    /** Get the workbench window as a Swing Rectangle -- used in various actions to center a new swing view on
     * top of the application.
     * @return non-null rectangle. */
    static java.awt.Rectangle getAppBoundsAsAWTRec() {
        final Rectangle knimeWindowBounds = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getBounds();
        return new java.awt.Rectangle(knimeWindowBounds.x, knimeWindowBounds.y, knimeWindowBounds.width, knimeWindowBounds.height);
    }
}
