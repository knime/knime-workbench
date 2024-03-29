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
 *   24 Feb 2017 (albrecht): created
 */
package org.knime.workbench.editor2.actions;

import java.util.Optional;
import java.util.function.Function;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.wizard.AbstractWizardNodeView;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.SubNodeContainerUI;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.core.wizard.CompositeViewPageManager;
import org.knime.core.wizard.SubnodeViewableModel;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;

/**
 * Action to open a combined interactive web view of a subnode,
 * comprised of all applicable contained view nodes, arranged in the defined layout.
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz, Germany
 * @since 3.4
 */
public final class OpenSubnodeWebViewAction extends Action {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(OpenSubnodeWebViewAction.class);

    private final SubNodeContainerUI m_nodeContainer;

    /**
     * New action to open an interactive subnode view.
     *
     * @param subnodeContainer The SNC for the view
     */
    public OpenSubnodeWebViewAction(final SubNodeContainerUI subnodeContainer) {
        m_nodeContainer = subnodeContainer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        boolean executed = m_nodeContainer.getNodeContainerState().isExecuted();
        return mapSNC(snc -> {
            CompositeViewPageManager spm = CompositeViewPageManager.of(snc.getParent());
            return executed && spm.hasWizardPage(snc.getID());
        }, snc -> {
            return executed && snc.hasWizardPage();
        }, m_nodeContainer).get();
    }

    private static <T> Optional<T> mapSNC(final Function<SubNodeContainer, T> sncFct,
        final Function<SubNodeContainerUI, T> sncUIFct, final NodeContainerUI nc) {
        if (nc instanceof SubNodeContainerUI) {
            SubNodeContainerUI snc = (SubNodeContainerUI)nc;
            if (Wrapper.wraps(snc, SubNodeContainer.class)) {
                return Optional.ofNullable(sncFct.apply(Wrapper.unwrap(snc, SubNodeContainer.class)));
            } else {
                return Optional.ofNullable(sncUIFct.apply(snc));
            }
        } else {
            return Optional.empty();
        }
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/openInteractiveView.png");
    }

    @Override
    public String getToolTipText() {
        return "Opens interactive node view: " + getSubnodeViewName(m_nodeContainer);
    }

    @Override
    public String getText() {
        return "Interactive View: " + getSubnodeViewName(m_nodeContainer);
    }

    @Override
    public void run() {
        LOGGER.debug("Open Interactive Web Node View " + getSubnodeViewName(m_nodeContainer));
        mapSNC(snc -> {
            openView(snc);
            return null;
        }, snc -> {
            try {
                @SuppressWarnings("rawtypes")
                AbstractWizardNodeView view = null;
                NodeContext.pushContext(snc);
                try {
                    view = OpenInteractiveWebViewAction
                        .getConfiguredWizardNodeView(null, snc.getInteractiveWebViews().get(0).getModel());
                } finally {
                    NodeContext.removeLastContext();
                }
                Node.invokeOpenView(view, snc.getName(), OpenViewAction.getAppBoundsAsAWTRec());
            } catch (Throwable t) {
                handleException(t, snc.getNameWithID());
            }
            return null;
        }, m_nodeContainer);

    }

    private static void handleException(final Throwable t, final String nodeName) {
        final MessageBox mb = new MessageBox(SWTUtilities.getActiveShell(), SWT.ICON_ERROR | SWT.OK);
        mb.setText("Interactive View cannot be opened");
        mb.setMessage("The interactive view cannot be opened for the following reason:\n" + t.getMessage());
        mb.open();
        LOGGER.error("The interactive view for node '" + nodeName + "' has thrown a '"
            + t.getClass().getSimpleName() + "'. That is most likely an implementation error.", t);
    }

    @Override
    public String getId() {
        return "knime.open.subnode.web.view.action";
    }

    private static String getSubnodeViewName(final SubNodeContainerUI snc) {
        //TODO: decide if this is the correct name for the view
        return snc.getName();
    }

    private static String getSubnodeViewName(final SubNodeContainer snc) {
        //TODO: decide if this is the correct name for the view
        return snc.getName();
    }

    static boolean hasContainerView(final NodeContainerUI cont) {
        return mapSNC(snc -> {
            CompositeViewPageManager spm = CompositeViewPageManager.of(snc.getParent());
            return spm.hasWizardPage(cont.getID());
        }, snc -> {
            return snc.hasWizardPage();
        }, cont).orElse(false);
    }

    /**
     * Opens the actual view.
     *
     * @param snc the component to open the view for
     */
    public static void openView(final SubNodeContainer snc) {
        try {
            @SuppressWarnings("rawtypes")
            AbstractWizardNodeView view = null;
            NodeContext.pushContext(snc);
            try {
                SubnodeViewableModel model = new SubnodeViewableModel(snc, getSubnodeViewName(snc), true, null);
                view = OpenInteractiveWebViewAction.getConfiguredWizardNodeView(snc, model);
                model.registerView(view);
            } finally {
                NodeContext.removeLastContext();
            }
            view.setWorkflowManagerAndNodeID(snc.getParent(), snc.getID());
            Node.invokeOpenView(view, snc.getName(), OpenViewAction.getAppBoundsAsAWTRec());
        } catch (Throwable t) {
            handleException(t, snc.getNameWithID());
        }
    }

}
