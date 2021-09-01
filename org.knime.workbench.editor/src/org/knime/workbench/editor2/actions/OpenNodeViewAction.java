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
 *   Aug 24, 2021 (hornm): created
 */
package org.knime.workbench.editor2.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.AbstractNodeView;
import org.knime.core.node.Node;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.webui.NodeView;
import org.knime.core.node.webui.NodeViewFactory;
import org.knime.core.node.webui.internal.NodeViewFactoryInternal;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;

/**
 * Action to open a view of a node (a view contributed via the UI extensions framework).
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class OpenNodeViewAction extends Action {

    private static final String NODE_VIEW_EXTENSION_ID = "org.knime.workbench.editor.NodeView";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(OpenNodeViewAction.class);

    private NativeNodeContainer m_nnc;

    OpenNodeViewAction(final NodeContainerUI nc) {
        m_nnc = Wrapper.unwrap(nc, NativeNodeContainer.class);
    }

    @Override
    public boolean isEnabled() {
        return m_nnc.getNodeContainerState().isExecuted();
    }

    @Override
    public void run() {
        openNodeView(m_nnc, createNodeView(m_nnc), getText());
    }

    @Override
    public String getText() {
        return m_nnc.getNodeViewName(0);
    }

    @Override
    public String getToolTipText() {
        return "Opens node view: " + getText();
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/openInteractiveView.png");
    }

    @Override
    public String getId() {
        return "knime.open.node.view";
    }

    private static void openNodeView(final NativeNodeContainer nnc, final AbstractNodeView<?> view,
        final String viewName) {
        try {
            Node.invokeOpenView(view, viewName, OpenViewAction.getAppBoundsAsAWTRec());
        } catch (Exception t) { // NOSONAR
            showWarningDialog(nnc, t);
        }

    }

    private static void showWarningDialog(final NativeNodeContainer nnc, final Throwable t) {
        final MessageBox mb = new MessageBox(SWTUtilities.getActiveShell(), SWT.ICON_ERROR | SWT.OK);
        mb.setText("Node View cannot be opened");
        mb.setMessage("The node view cannot be opened for the following reason:\n" + t.getMessage());
        mb.open();
        StringBuilder sb = new StringBuilder("The view for node '");
        sb.append(nnc.getNameWithID());
        sb.append("' has thrown a '");
        sb.append(t.getClass().getSimpleName());
        sb.append("'.");
        LOGGER.error(sb.toString(), t);
    }

    private static AbstractNodeView<?> createNodeView(final NativeNodeContainer nnc) {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(NODE_VIEW_EXTENSION_ID);
        CheckUtils.checkState(point != null, "Invalid extension point: %s", NODE_VIEW_EXTENSION_ID);
        IConfigurationElement el = Arrays.stream(point.getExtensions())//
            .flatMap(ext -> Stream.of(ext.getConfigurationElements())).iterator().next();
        // TODO what if nothing registered
        NodeContext.pushContext(nnc);
        try {
            Class<?> nodeViewClass = Platform.getBundle(el.getDeclaringExtension().getContributor().getName())
                .loadClass(el.getAttribute("class"));
            return (AbstractNodeView<?>)nodeViewClass.getConstructor(NativeNodeContainer.class).newInstance(nnc);
        } catch (ClassNotFoundException | InvalidRegistryObjectException | InstantiationException
                | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                | SecurityException e) {
            throw new IllegalStateException("Node view could not be created", e);
        } finally {
            NodeContext.removeLastContext();
        }
    }

    /**
     * @param nc the node container to check
     * @return <code>true</code> if the node container provides a {@link NodeView}
     */
    public static boolean hasNodeView(final NodeContainerUI nc) {
        if (Wrapper.wraps(nc, NativeNodeContainer.class)) {
            NativeNodeContainer nnc = Wrapper.unwrap(nc, NativeNodeContainer.class);
            if (NodeViewFactory.hasNodeView(nnc) || NodeViewFactoryInternal.hasNodeView(nnc)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates an instance of this action if the node container provides a {@link NodeView}.
     *
     * @param nc the node container to check and create the action for
     * @return a new action instance or an empty optional if the node container doesn't provide a node view
     */
    public static Optional<OpenNodeViewAction> createActionIfApplicable(final NodeContainerUI nc) {
        if (hasNodeView(nc)) {
            return Optional.of(new OpenNodeViewAction(nc));
        }
        return Optional.empty();
    }

}
