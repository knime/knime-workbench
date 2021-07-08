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
 *   30.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.editparts;

import static org.knime.core.ui.wrapper.Wrapper.wraps;
import static org.knime.workbench.editor2.figures.NodeContainerFigure.scaleImageTo;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LayoutManager;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartListener;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.requests.CreateConnectionRequest;
import org.eclipse.gef.requests.SelectionRequest;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.IPropertySource;
import org.knime.core.node.ConfigurableNodeFactory.ConfigurableNodeDialog;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ModifiableNodeCreationConfiguration;
import org.knime.core.node.workflow.AbstractNodeExecutionJobManager;
import org.knime.core.node.workflow.MetaNodeTemplateInformation;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NodeAnnotation;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainerTemplate;
import org.knime.core.node.workflow.NodeExecutionJobManager;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.NodeMessageEvent;
import org.knime.core.node.workflow.NodeMessageListener;
import org.knime.core.node.workflow.NodeProgressEvent;
import org.knime.core.node.workflow.NodeProgressListener;
import org.knime.core.node.workflow.NodePropertyChangedEvent;
import org.knime.core.node.workflow.NodePropertyChangedListener;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.NodeUIInformationEvent;
import org.knime.core.node.workflow.NodeUIInformationListener;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowCipherPrompt;
import org.knime.core.ui.node.workflow.NativeNodeContainerUI;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.NodePortUI;
import org.knime.core.ui.node.workflow.SubNodeContainerUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.node.workflow.async.AsyncWorkflowManagerUI;
import org.knime.core.ui.node.workflow.lazy.LazyWorkflowManagerUI;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.EditorModeParticipant;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.WorkflowEditorMode;
import org.knime.workbench.editor2.WorkflowManagerInput;
import org.knime.workbench.editor2.WorkflowSelectionDragEditPartsTracker;
import org.knime.workbench.editor2.commands.CreateConnectionCommand;
import org.knime.workbench.editor2.commands.ReplaceNodePortCommand;
import org.knime.workbench.editor2.commands.ShiftConnectionCommand;
import org.knime.workbench.editor2.editparts.policy.PortGraphicalRoleEditPolicy;
import org.knime.workbench.editor2.editparts.snap.SnapIconToGrid;
import org.knime.workbench.editor2.figures.NodeContainerFigure;
import org.knime.workbench.editor2.figures.ProgressFigure;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.async.AsyncUtil;
import org.knime.workbench.ui.preferences.PreferenceConstants;
import org.knime.workbench.ui.wrapper.WrappedNodeDialog;

/**
 * Edit part for node containers. This also listens to interesting events, like changed extra infos or execution states.
 *
 * <br>
 * Model: {@link NodeContainer} <br>
 * View: {@link NodeContainerFigure} <br>
 * Controller: {@link NodeContainerEditPart}
 *
 * @author Florian Georg, University of Konstanz
 * @author Christoph Sieb, University of Konstanz
 */
public class NodeContainerEditPart extends AbstractWorkflowEditPart implements ConnectableEditPart, EditPartListener,
    EditorModeParticipant, IAdaptable, IPropertyChangeListener, NodeEditPart, NodeMessageListener, NodeProgressListener,
    NodePropertyChangedListener, NodeStateChangeListener, NodeUIInformationListener {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(NodeContainerEditPart.class);

    private static final ExecutorService DATA_AWARE_DIALOG_EXECUTOR = Executors
        .newCachedThreadPool(new ThreadFactory() {
            private final AtomicInteger m_threadIDs = new AtomicInteger();

            @Override
            public Thread newThread(final Runnable r) {
                return new Thread(r, "DataAwareNode-Executor-" + m_threadIDs.incrementAndGet());
            }
        });

    private static final Image META_NODE_LINK_GREEN_ICON =
        ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/meta/metanode_link_green_decorator.png");

    private static final Image META_NODE_LINK_RED_ICON =
        ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/meta/metanode_link_red_decorator.png");

    private static final Image META_NODE_LINK_PROBLEM_ICON =
        ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/meta/metanode_link_problem_decorator.png");

    private static final Image META_NODE_LOCK_ICON =
            ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/meta/metanode_lock_decorator.png");

    private static final Image META_NODE_UNLOCK_ICON =
        ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/meta/metanode_unlock_decorator.png");

    private static final Image NODE_LOCK_ICON =
        ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/meta/metanode_lock_decorator.png");

    /**
     * Given the node container, return the icon displayed in the workflow canvas node.
     * @param nodeContainer
     * @return the icon displayed in the workflow canvas node
     */
    public static Image getIconImageForNodeContainer(final NodeContainerUI nodeContainer) {
        Image icon = null;

        try (InputStream iconStream = nodeContainer.getIconAsStream()) {
            if (iconStream != null) {
                final ImageData imageData = new ImageData(iconStream);
                icon = new Image(Display.getDefault(), scaleImageTo(16, imageData));
                // TODO this is a source of a potential SWT handle leak (this is not new code though) - Nov.2019
            }
        } catch (IOException e) {
            //should never happen
            LOGGER.error("Problem while closing icon input stream", e);
        }

        if (icon == null) {
            icon = ImageRepository.getUnscaledIconImage(nodeContainer.getIcon());
        }
        if (icon == null) { // get default image if null
            icon = ImageRepository.getUnscaledIconImage(NodeFactory.getDefaultIcon());
        }

        return icon;
    }


    /**
     * true, if the figure was initialized from the node extra info object.
     */
    private boolean m_uiListenerActive = true;

    private boolean m_showFlowVarPorts = false;

    private WorkflowEditorMode m_currentEditorMode = WorkflowEditor.INITIAL_EDITOR_MODE;

    /**
     * @return The <code>NodeContainer</code>(= model)
     */
    @Override
    public NodeContainerUI getNodeContainer() {
        return (NodeContainerUI)getModel();
    }

    /**
     * Returns the parent WFM.
     *
     * @return The hosting WFM
     */
    public WorkflowManagerUI getWorkflowManager() {
        return (WorkflowManagerUI)getParent().getModel();
    }

    public boolean getShowImplFlowVarPorts() {
        return m_showFlowVarPorts;
    }

    public void setShowImplFlowVarPorts(final boolean showEm) {
        m_showFlowVarPorts = showEm;
        ((NodeContainerFigure)getFigure()).setShowFlowVarPorts(showEm);
        getFigure().repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void activate() {
        super.activate();

        initFigure();

        // If we already have extra info, init figure now
        NodeContainerUI cont = getNodeContainer();
        NodeUIInformation uiInfo = cont.getUIInformation();
        if (uiInfo != null) {
            // takes over all info except the coordinates
            updateFigureFromUIinfo(uiInfo);
        } else {
            // set a new empty UI info
            NodeUIInformation info = NodeUIInformation.builder().setNodeLocation(0, 0, -1, -1).build();
            // not yet a listener -- no event received
            cont.setUIInformation(info);
        }

        // need to notify node annotation about our presence
        // the annotation is a child that's added first (placed in background)
        // to the viewer - so it doesn't know about the correct location yet
        NodeAnnotation nodeAnnotation = cont.getNodeAnnotation();
        NodeAnnotationEditPart nodeAnnotationEditPart =
            (NodeAnnotationEditPart)getViewer().getEditPartRegistry().get(nodeAnnotation);
        if (nodeAnnotationEditPart != null) {
            nodeAnnotationEditPart.nodeUIInformationChanged(null);
        }

        IPreferenceStore store = KNIMEUIPlugin.getDefault().getPreferenceStore();
        store.addPropertyChangeListener(this);

        // listen to node container (= model object)
        cont.addNodeStateChangeListener(this);
        cont.addNodeMessageListener(this);
        cont.addProgressListener(this);
        cont.addUIInformationListener(this);
        cont.addNodePropertyChangedListener(this);
        addEditPartListener(this);

        updateJobManagerIcon();
        checkMetaNodeTemplateIcon();
        checkMetaNodeLockIcon();
        checkNodeLockIcon();
        checkModifiablePortIcon();
        // set the active (or disabled) state
        ((NodeContainerFigure)getFigure()).setStateFromNC(cont);
        // set the node message
        updateNodeMessage();
        callHideNodeName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deactivate() {
        final NodeContainerUI nc = getNodeContainer();
        final IPreferenceStore store = KNIMEUIPlugin.getDefault().getPreferenceStore();

        store.removePropertyChangeListener(this);
        nc.removeNodeStateChangeListener(this);
        nc.removeNodeMessageListener(this);
        nc.removeNodeProgressListener(this);
        nc.removeUIInformationListener(this);
        nc.removeNodePropertyChangedListener(this);

        removeEditPartListener(this);

        for (final Object o : getChildren()) {
            final EditPart editPart = (EditPart)o;
            editPart.deactivate();
        }

        final EditPolicyIterator editPolicyIterator = getEditPolicyIterator();
        while (editPolicyIterator.hasNext()) {
            editPolicyIterator.next().deactivate();
        }

        ((NodeContainerFigure)figure).figureIsBeingDisposed();

        super.deactivate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IFigure createFigure() {
        // create the visuals for the node container.
        final NodeContainerFigure containerFigure = new NodeContainerFigure(new ProgressFigure());
        // init the user specified node name
        if (getRootEditPart() != null) {
            containerFigure.hideNodeName(getRootEditPart().hideNodeNames());
        }
        return containerFigure;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void performRequest(final Request request) {
        if (request.getType() == RequestConstants.REQ_OPEN) {
            // double click on node
            if ((request instanceof SelectionRequest) && ((SelectionRequest)request).isShiftKeyPressed()) {
                shiftConnection();
                return;
            }
            // simple dbl-click, no modifier key
            openDialog();
        }
    }

    public void shiftConnection() {
        ShiftConnectionCommand cmd = new ShiftConnectionCommand(getWorkflowManager(), this);
        if (cmd.canExecute()) {
            getViewer().getEditDomain().getCommandStack().execute(cmd);
        }
    }

    /** @return The associated node annotation edit part (maybe null). */
    public final NodeAnnotationEditPart getNodeAnnotationEditPart() {
        NodeAnnotation nodeAnnotation = getNodeContainer().getNodeAnnotation();
        NodeAnnotationEditPart nodeAnnotationEditPart =
            (NodeAnnotationEditPart)getViewer().getEditPartRegistry().get(nodeAnnotation);
        return nodeAnnotationEditPart;
    }

    /**
     * Installs the COMPONENT_ROLE for this edit part.
     *
     * {@inheritDoc}
     */
    @Override
    protected void createEditPolicies() {

        // This policy provides create/reconnect commands for connections that
        // are associated with ports of this node
        this.installEditPolicy(EditPolicy.GRAPHICAL_NODE_ROLE, new PortGraphicalRoleEditPolicy());
    }

    /**
     * Returns the model children (= the ports) of the <code>NodeContainer</code> managed by this edit part. Note that
     * in/out ports are handled the same.
     *
     * {@inheritDoc}
     */
    @Override
    protected List<NodePortUI> getModelChildren() {
        ArrayList<NodePortUI> ports = new ArrayList<NodePortUI>();
        NodeContainerUI container = getNodeContainer();

        for (int i = 0; i < container.getNrInPorts(); i++) {
            ports.add(container.getInPort(i));
        }
        for (int i = 0; i < container.getNrOutPorts(); i++) {
            ports.add(container.getOutPort(i));
        }
        return ports;
    }

    private final AtomicBoolean m_updateInProgress = new AtomicBoolean(false);

    /** {@inheritDoc} */
    @Override
    public void stateChanged(final NodeStateEvent state) {
        // if another state is waiting to be processed, simply return
        // and leave the work to the previously started thread. This
        // works because we are retrieving the current state information!
        if (m_updateInProgress.compareAndSet(false, true)) {
            Display display = Display.getDefault();
            if (display.isDisposed()) {
                return;
            }
            display.asyncExec(new Runnable() {
                @Override
                public void run() {
                    // let others know we are in the middle of processing
                    // this update - they will now need to start their own job.
                    NodeContainerFigure fig = (NodeContainerFigure)getFigure();
                    m_updateInProgress.set(false);
                    if (isActive()) {
                        NodeContainerUI nc = getNodeContainer();
                        fig.setStateFromNC(nc);
                        updateNodeMessage();
                        // reset the tooltip text of the outports
                        for (Object part : getChildren()) {
                            if (part instanceof NodeOutPortEditPart || part instanceof WorkflowInPortEditPart
                                || part instanceof MetaNodeOutPortEditPart) {
                                AbstractPortEditPart outPortPart = (AbstractPortEditPart)part;
                                outPortPart.rebuildTooltip();
                            }
                        }
                        // always refresh visuals (does not seem to do anything
                        // by default though: call repaints on updated figures).
                        refreshVisuals();
                    }
                }
            });
        }
    }

    /** {@inheritDoc} */
    @Override
    public void progressChanged(final NodeProgressEvent pe) {
        // forward the new progress to our progress figure
        ((NodeContainerFigure)getFigure()).getProgressFigure().progressChanged(pe.getNodeProgress());
    }

    private final AtomicBoolean m_messageUpdateInProgress = new AtomicBoolean();

    /** {@inheritDoc} */
    @Override
    public void messageChanged(final NodeMessageEvent ignored) {
        if (m_messageUpdateInProgress.compareAndSet(false, true)) {
            Display display = Display.getDefault();
            if (display.isDisposed()) {
                return;
            }
            display.asyncExec(new Runnable() {
                @Override
                public void run() {
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            m_messageUpdateInProgress.set(false);
                            if (isActive()) {
                                // must ignore event content - as this runnable
                                // may be processing another (following) event
                                updateNodeMessage();
                                refreshVisuals();
                            }
                        }
                    });
                }
            });
        }
    }

    /** {@inheritDoc} */
    @Override
    public void nodeUIInformationChanged(final NodeUIInformationEvent evt) {

        if (!m_uiListenerActive) {
            return;
        }

        //
        // As this code updates the UI it must be executed in the UI thread.
        //
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (isActive()) {

                    NodeUIInformation uiInfo = getNodeContainer().getUIInformation();
                    updateFigureFromUIinfo(uiInfo);
                }
            }
        });
    }

    private void updateFigureFromUIinfo(final NodeUIInformation uiInfo) {

        NodeContainerFigure fig = (NodeContainerFigure)getFigure();
        setBoundsFromUIinfo(uiInfo);

        // update tooltip
        fig.setCustomDescription(getNodeContainer().getCustomDescription());
        // check status of node
        updateNodeMessage();

        // reset the tooltip text of the outports
        for (Object part : getChildren()) {
            if (part instanceof NodeOutPortEditPart) {
                NodeOutPortEditPart outPortPart = (NodeOutPortEditPart)part;
                outPortPart.rebuildTooltip();
            }
        }

        // for sub node refresh all tooltips
        if (getNodeContainer() instanceof SubNodeContainerUI) {
            for (Object part : getChildren()) {
                if (part instanceof NodeInPortEditPart) {
                    NodeInPortEditPart inPortPart = (NodeInPortEditPart)part;
                    inPortPart.rebuildTooltip();
                }
                if (part instanceof NodeOutPortEditPart) {
                    NodeOutPortEditPart outPortPart = (NodeOutPortEditPart)part;
                    outPortPart.rebuildTooltip();
                }
            }
        }

        // always refresh visuals
        refreshVisuals();

    }

    private void setBoundsFromUIinfo(final NodeUIInformation uiInfo) {
        NodeContainerFigure fig = (NodeContainerFigure)getFigure();
        int[] bounds = uiInfo.getBounds();
        Point iconOffset = fig.getOffsetToRefPoint(uiInfo);
        Point iconCenterOffset = SnapIconToGrid.getGridRefPointOffset(fig);
        Point diff = new Point(iconCenterOffset.x - iconOffset.x, iconCenterOffset.y - iconOffset.y);
        boolean newBounds = false;
        if (uiInfo.isDropLocation()) {
            bounds[0] -= diff.x;
            bounds[1] -= diff.y;
            newBounds = true; // apply these new values at the end of the method
        }
        if (!uiInfo.hasAbsoluteCoordinates()) {
            // make it absolute coordinates taking scrolling into account
            Point p = new Point(bounds[0], bounds[1]);
            fig.translateToRelative(p);
            bounds[0] = p.x;
            bounds[1] = p.y;
            newBounds = true; // apply these new values at the end of the method
        }
        if (uiInfo.getSnapToGrid()) {
            // snap icon center to grid
            Point iconCenterLoc = new Point(bounds[0] + diff.x, bounds[1] + diff.y);
            iconCenterLoc = WorkflowEditor.getActiveEditorClosestGridLocation(iconCenterLoc);
            // ui coordinates use the icon top left corner as reference
            bounds[0] = iconCenterLoc.x - diff.x;
            bounds[1] = iconCenterLoc.y - diff.y;
            newBounds = true;
        }
        if (!uiInfo.isSymbolRelative()) {
            // ui info from an earlier version - x/y is top top left coordinates
            // store symbol relative coordinates
            int xCorr = 29;
            int yCorr = Platform.OS_LINUX.equals(Platform.getOS()) ? 18 : 15;
            // don't trigger another entry into this method
            bounds[0] += xCorr;
            bounds[1] += yCorr;
            newBounds = true;
        }
        if (newBounds) {
            // don't trigger another event here, when updating ui info
            m_uiListenerActive = false;
            getNodeContainer().setUIInformationForCorrection(
                NodeUIInformation.builder().setNodeLocation(bounds[0], bounds[1], bounds[2], bounds[3]).build());
            m_uiListenerActive = true;
        }
        // since v2.5 we ignore any width and height and keep bounds minimal
        refreshBounds();
    }

    /**
     * Adjusts the height and width of the node's figure. It automatically sets them to the preferred height/width of
     * the figure (which might change if the warning icons change). It doesn't change x/y position of the figure. It
     * does change width and height.
     */
    private void refreshBounds() {
        NodeUIInformation uiInfo = getNodeContainer().getUIInformation();
        int[] bounds = uiInfo.getBounds();
        WorkflowRootEditPart parent = (WorkflowRootEditPart)getParent();
        NodeContainerFigure fig = (NodeContainerFigure)getFigure();
        Dimension pref = fig.getPreferredSize();
        boolean set = false;
        if (pref.width != bounds[2]) {
            bounds[2] = pref.width;
            set = true;
        }
        if (pref.height != bounds[3]) {
            bounds[3] = pref.height;
            set = true;
        }
        if (set) {
            // notify uiInfo listeners (e.g. node annotations)
            m_uiListenerActive = false;
            getNodeContainer().setUIInformationForCorrection(
                NodeUIInformation.builder().setNodeLocation(bounds[0], bounds[1], bounds[2], bounds[3]).build());
            m_uiListenerActive = true;
        }

        // since ver2.3.0 all coordinates are relative to the icon
        Point offset = fig.getOffsetToRefPoint(uiInfo);
        bounds[0] -= offset.x;
        bounds[1] -= offset.y;
        Rectangle rect = new Rectangle(bounds[0], bounds[1], bounds[2], bounds[3]);
        fig.setBounds(rect);
        parent.setLayoutConstraint(this, fig, rect);
    }

    private void initFigure() {
        NodeContainerFigure f = (NodeContainerFigure)getFigure();
        NodeType type = getNodeContainer().getType();
        String description = getNodeContainer().getCustomDescription();

        updateIcon();
        f.setType(type, getNodeContainer() instanceof SubNodeContainerUI);
        updateLabelText();
        f.setCustomDescription(description);
    }

    private void updateIcon() {
        final Image icon = getIconImageForNodeContainer(getNodeContainer());
        if (icon != null) {
            ((NodeContainerFigure)getFigure()).setIcon(icon);
        }
    }

    private void updateDisplayType() {
        final NodeContainerUI ncUI = getNodeContainer();
        final Optional<SubNodeContainer> snc = Wrapper.unwrapOptional(ncUI, SubNodeContainer.class);
        if (snc.isPresent()) {
            final NodeContainerFigure f = (NodeContainerFigure)getFigure();
            f.setType(snc.get().getType(), true);
        }
    }

    /**
     * Checks the message of the this node and if there is a message in the <code>NodeStatus</code> object the message
     * is set. Otherwise the currently displayed message is removed.
     */
    private void updateNodeMessage() {
        NodeContainerUI nc = getNodeContainer();
        NodeContainerFigure containerFigure = (NodeContainerFigure)getFigure();
        NodeMessage nodeMessage = nc.getNodeMessage();
        containerFigure.setMessage(nodeMessage);
        refreshBounds();
    }

    /**
     * Marks this node parts figure as slated for delete. Used to hilite it from the rest of the parts.
     *
     * @see NodeContainerEditPart#unmarkForDelete()
     */
    public void markForDelete() {
        ((NodeContainerFigure)getFigure()).markForDelete();
    }

    /**
     * Resets the delete-marked part.
     *
     * @see NodeContainerEditPart#markForDelete()
     */
    public void unmarkForDelete() {
        ((NodeContainerFigure)getFigure()).unmarkForDelete();
    }

    /**
     * Marks this node parts figure as slated for replacement. Used to hilite it from the rest of the parts.
     *
     * @see NodeContainerEditPart#unmarkForReplacement()
     */
    public void markForReplacement() {
        ((NodeContainerFigure)getFigure()).markForReplacement();
    }

    /**
     * Resets the replacement-marked part.
     *
     * @see NodeContainerEditPart#markForReplacement()
     */
    public void unmarkForReplacement() {
        ((NodeContainerFigure)getFigure()).unmarkForReplacement();
    }

    /**
     * Overridden to return a custom <code>DragTracker</code> for NodeContainerEditParts.
     *
     * {@inheritDoc}
     */
    @Override
    public DragTracker getDragTracker(final Request request) {
        return new WorkflowSelectionDragEditPartsTracker(this);
    }

    /**
     * {@inheritDoc}
     *
     * We don't want to be selected if we're not in node-edit-mode.
     */
    @Override
    public EditPart getTargetEditPart(final Request request) {
        if (m_currentEditorMode.equals(WorkflowEditorMode.NODE_EDIT) ) {
            return super.getTargetEditPart(request);
        }

        return null;
    }

    /**
     * @return all outgoing connections of this node part
     */
    public ConnectionContainerEditPart[] getOutgoingConnections() {

        List<ConnectionContainerEditPart> result = new ArrayList<ConnectionContainerEditPart>();

        for (Object part : getChildren()) {

            if (part instanceof NodeOutPortEditPart) {
                result.addAll(((NodeOutPortEditPart)part).getSourceConnections());
            } else if (part instanceof MetaNodeOutPortEditPart) {
                result.addAll(((MetaNodeOutPortEditPart)part).getSourceConnections());
            }
        }

        return result.toArray(new ConnectionContainerEditPart[result.size()]);
    }

    /**
     * @return all incoming connections of this node part
     */
    public ConnectionContainerEditPart[] getIncomingConnections() {

        List<ConnectionContainerEditPart> result = new ArrayList<ConnectionContainerEditPart>();

        for (Object part : getChildren()) {

            if (part instanceof NodeInPortEditPart) {
                result.addAll(((NodeInPortEditPart)part).getTargetConnections());
            }
        }

        return result.toArray(new ConnectionContainerEditPart[result.size()]);
    }

    /**
     * @return all connections of this node part
     */
    public ConnectionContainerEditPart[] getAllConnections() {

        ConnectionContainerEditPart[] out = getOutgoingConnections();
        ConnectionContainerEditPart[] in = getIncomingConnections();

        ConnectionContainerEditPart[] result = new ConnectionContainerEditPart[out.length + in.length];
        System.arraycopy(in, 0, result, 0, in.length);
        System.arraycopy(out, 0, result, in.length, out.length);

        return result;
    }

    /**
     * Opens the node dialog on double click.
     *
     */
    public void openDialog() {
        NodeContainerUI container = (NodeContainerUI)getModel();
        if (container instanceof WorkflowManagerUI) {
            openSubWorkflowEditor();
        } else {
            openNodeDialog();
        }
    }

    /**
     * Opens the node's dialog (also metanode dialogs).
     *
     * @since 2.6
     */
    public void openNodeDialog() {
        final NodeContainerUI container = (NodeContainerUI)getModel();
        openDialog(container, this);
    }

    /**
     * Helper method to open the dialog of the provided node container (if it has one).
     *
     * @param container the node to open the dialog for
     * @param editPart the node container edit-part, can be <code>null</code> (not undo-able in that case!)
     */
    public static void openDialog(final NodeContainerUI container, final NodeContainerEditPart editPart) {
        // if this node does not have a dialog
        if (!container.hasDialog()) {
            LOGGER.debug("No dialog for " + container.getNameWithID());
            return;
        }

        final Shell shell = SWTUtilities.getActiveShell();
        shell.setEnabled(false);
        try {
            if (container.hasDataAwareDialogPane() && !container.isAllInputDataAvailable()
                && container.canExecuteUpToHere()) {
                IPreferenceStore store = KNIMEUIPlugin.getDefault().getPreferenceStore();
                String prefPrompt = store.getString(PreferenceConstants.P_EXEC_NODES_DATA_AWARE_DIALOGS);
                boolean isExecuteUpstreamNodes;
                if (MessageDialogWithToggle.PROMPT.equals(prefPrompt)) {
                    int returnCode =
                        MessageDialogWithToggle.openYesNoCancelQuestion(
                            shell,
                            "Execute upstream nodes",
                            "The " + container.getName() + " node can be configured using the full input data.\n\n"
                                + "Execute upstream nodes?", "Remember my decision", false, store,
                            PreferenceConstants.P_EXEC_NODES_DATA_AWARE_DIALOGS).getReturnCode();
                    if (returnCode == Window.CANCEL) {
                        return;
                    } else if (returnCode == IDialogConstants.YES_ID) {
                        isExecuteUpstreamNodes = true;
                    } else {
                        isExecuteUpstreamNodes = false;
                    }
                } else if (MessageDialogWithToggle.ALWAYS.equals(prefPrompt)) {
                    isExecuteUpstreamNodes = true;
                } else {
                    isExecuteUpstreamNodes = false;
                }
                if (isExecuteUpstreamNodes) {
                    try {
                        PlatformUI.getWorkbench().getProgressService().run(true, true, new IRunnableWithProgress() {
                            @Override
                            public void run(final IProgressMonitor monitor) throws InvocationTargetException,
                                InterruptedException {
                                Future<Void> submit = DATA_AWARE_DIALOG_EXECUTOR.submit(new Callable<Void>() {
                                    @Override
                                    public Void call() throws Exception {
                                        container.getParent().executePredecessorsAndWait(container.getID());
                                        return null;
                                    }
                                });
                                while (!submit.isDone()) {
                                    if (monitor.isCanceled()) {
                                        submit.cancel(true);
                                        throw new InterruptedException();
                                    }
                                    try {
                                        submit.get(300, TimeUnit.MILLISECONDS);
                                    } catch (ExecutionException e) {
                                        LOGGER.error("Error while waiting for execution to finish", e);
                                    } catch (InterruptedException e) {
                                        submit.cancel(true);
                                        throw e;
                                    } catch (TimeoutException e) {
                                        // do another round
                                    }
                                }
                            }
                        });
                    } catch (InvocationTargetException e) {
                        String error = "Exception while waiting for completion of execution";
                        LOGGER.warn(error, e);
                        ErrorDialog.openError(shell, "Failed opening dialog", error, new Status(IStatus.ERROR,
                            KNIMEEditorPlugin.PLUGIN_ID, error, e));
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }

            //
            // This is embedded in a special JFace wrapper dialog
            //
            try {
                WrappedNodeDialog dlg = new WrappedNodeDialog(shell, container,
                    dp -> preOpenDialogAction(dp, container), dp -> postApplyDialogAction(dp, container, editPart));
                dlg.open();
            } catch (NotConfigurableException ex) {
                MessageBox mb = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
                mb.setText("Dialog cannot be opened");
                mb.setMessage("The dialog cannot be opened for the following" + " reason:\n" + ex.getMessage());
                mb.open();
            } catch (Throwable t) {
                LOGGER.error("The dialog pane for node '" + container.getNameWithID() + "' has thrown a '"
                    + t.getClass().getSimpleName() + "'. That is most likely an implementation error.", t);
            }
        } finally {
            shell.setEnabled(true);
            // set the (keyboard) focus to the current workflow editor
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor().setFocus();
        }
    }

    private static void preOpenDialogAction(final NodeDialogPane dialogPane, final NodeContainerUI nc) {
        if (dialogPane instanceof ConfigurableNodeDialog && wraps(nc, NativeNodeContainer.class)) {
            NativeNodeContainer nnc = Wrapper.unwrap(nc, NativeNodeContainer.class);
            ((ConfigurableNodeDialog)dialogPane)
                .setCurrentNodeCreationConfiguration(nnc.getNode().getCopyOfCreationConfig().orElse(null));
        }
    }

    private static void postApplyDialogAction(final NodeDialogPane dialogPane, final NodeContainerUI nc,
        final NodeContainerEditPart editPart) {
        if (dialogPane instanceof ConfigurableNodeDialog && nc instanceof NativeNodeContainerUI) {
            Optional<ModifiableNodeCreationConfiguration> newNodeCreationConfiguration =
                ((ConfigurableNodeDialog)dialogPane).getNewNodeCreationConfiguration();
            if (newNodeCreationConfiguration.isPresent()) {
                if (editPart != null) {
                    editPart.getViewer().getEditDomain().getCommandStack()
                        .execute(new ReplaceNodePortCommand(editPart, newNodeCreationConfiguration.get()));
                } else {
                    ReplaceNodePortCommand.replaceNode(nc.getParent(), nc.getID(), newNodeCreationConfiguration.get());
                }
            }

            if (editPart != null) {
                //the connections are not always properly re-drawn after "unmark". (Eclipse bug.) Repaint here.
                editPart.getRoot().refresh();
            }
        }
    }

    public void openSubWorkflowEditor() {
        Object obj = getModel();
        WorkflowManagerUI wm;
        if (obj instanceof WorkflowManagerUI) {
            wm = (WorkflowManagerUI)getModel();
        } else if (obj instanceof SubNodeContainerUI) {
            wm = ((SubNodeContainerUI)obj).getWorkflowManager();
        } else {
            return;
        }

        //if workflow manager is encrypted, try unlocking it
        if (wm.isEncrypted()) {
            WorkflowCipherPrompt prompt = new GUIWorkflowCipherPrompt(obj instanceof SubNodeContainerUI);
            if (!Wrapper.unwrapWFM(wm).unlock(prompt)) {
                return;
            }
        }

        if (!checkAndLoadIfLazyWorkflowManager(wm)) {
            return;
        }

        // open new editor for subworkflow
        LOGGER.debug("opening new editor for sub-workflow");
        try {
            final WorkflowEditor parent =
                (WorkflowEditor)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
            WorkflowManagerInput input = new WorkflowManagerInput(wm, parent);
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                .openEditor(input, "org.knime.workbench.editor.WorkflowEditor");
        } catch (PartInitException e) {
            LOGGER.error("Error while opening new editor", e);
        }
        return;
    }

    /**
     * @param wfm
     * @return <code>true</code> if loading was successful or not necessary, <code>false</code> if loading failed
     */
    private static boolean checkAndLoadIfLazyWorkflowManager(final WorkflowManagerUI wm) {
        if (wm instanceof AsyncWorkflowManagerUI && ((AsyncWorkflowManagerUI)wm).getConnectionProblem().isPresent()) {
            final Display display = Display.getDefault();
            display.syncExec(() -> {
                final Shell shell = SWTUtilities.getActiveShell(display);
                MessageDialog.openError(shell, "Action not possible",
                    "Cannot be opened because the job is not available or the server connection is lost");
            });
            return false;
        }
        if (wm instanceof LazyWorkflowManagerUI && !((LazyWorkflowManagerUI)wm).isLoaded()) {
            CompletableFuture<Void> future = ((LazyWorkflowManagerUI)wm).load();
            try {
                AsyncUtil.waitForTerminationOrThrowException(future, "Opening metanode");
                return true;
            } catch (CompletionException e) {
                Throwable cause = e.getCause();
                String message = "A problem occurred while opening metanode: "
                    + (cause != null ? cause.getMessage() : e.getMessage());
                final Display display = Display.getDefault();
                display.syncExec(() -> {
                    final Shell shell = SWTUtilities.getActiveShell(display);
                    MessageDialog.openError(shell, "Problem", message + "\nSee log for more details.");
                    LOGGER.error(message, e);
                });
                return false;
            }
        } else {
            return true;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void nodePropertyChanged(final NodePropertyChangedEvent e) {
        Display.getDefault().asyncExec(new Runnable() {
            /** {@inheritDoc} */
            @Override
            public void run() {
                if (isActive()) {
                    switch (e.getProperty()) {
                        case JobManager:
                            updateJobManagerIcon();
                            break;
                        case Name:
                            updateHeaderField();
                            break;
                        case TemplateConnection:
                            checkMetaNodeTemplateIcon();
                            break;
                        case LockStatus:
                            checkMetaNodeLockIcon();
                            checkNodeLockIcon();
                            break;
                        case MetaNodePorts:
                            refreshChildren(); // account for new/removed ports
                            updatePortIndex(); // set the (possibly changed) index in all ports
                            updateNumberOfPorts();
                            relayoutPorts(); // in case an index has changed
                            refreshBounds(); // different port number could mean different bounds
                            break;
                        case ComponentMetadata:
                            updateIcon();
                            updateDisplayType();
                        default:
                            // unknown, ignore
                    }
                }
            }
        });
    }

    /**
     * Updates the port index in all port editparts from the underlying port model.
     */
    private void updatePortIndex() {
        for (Object ep : getChildren()) {
            if (ep instanceof AbstractPortEditPart) {
                Object model = ((EditPart)ep).getModel();
                if (model instanceof NodePortUI) {
                    ((AbstractPortEditPart)ep).setIndex(((NodePortUI)model).getPortIndex());
                }
            }
        }
    }

    private void updateNumberOfPorts() {
        for (Object ep : getChildren()) {
            if (ep instanceof AbstractPortEditPart) {
                ((AbstractPortEditPart)ep).updateNumberOfPorts();
            }
        }
    }

    private void relayoutPorts() {
        IFigure nodeFig = getFigure();
        LayoutManager layoutManager = nodeFig.getLayoutManager();
        if (layoutManager != null) {
            layoutManager.invalidate();
            layoutManager.layout(figure);
        }
        nodeFig.repaint();
    }

    private void updateJobManagerIcon() {
        NodeContainerUI nc = getNodeContainer();
        NodeExecutionJobManager jobManager = nc.getJobManager();
        URL iconURL;
        if (jobManager != null) {
            iconURL = jobManager.getIcon();
        } else {
            NodeExecutionJobManager parentJobManager = nc.findJobManager();
            if (parentJobManager instanceof AbstractNodeExecutionJobManager) {
                if (Wrapper.wraps(nc, NodeContainer.class)) {
                    iconURL = ((AbstractNodeExecutionJobManager)parentJobManager).getIconForChild(Wrapper.unwrapNC(nc));
                } else {
                    iconURL = null;
                }
            } else {
                iconURL = null;
            }
        }
        Image icon = null;
        if (iconURL != null) {
            icon = ImageDescriptor.createFromURL(iconURL).createImage();
        }
        ((NodeContainerFigure)getFigure()).setJobExecutorIcon(icon);
    }

    private void checkMetaNodeTemplateIcon() {
        NodeContainerUI nc = getNodeContainer();
        MetaNodeTemplateInformation templInfo = null;
        if (Wrapper.wraps(nc, NodeContainerTemplate.class)) {
            NodeContainerTemplate t = Wrapper.unwrap(nc, NodeContainerTemplate.class);
            templInfo = t.getTemplateInformation();
        }
        if (templInfo != null) {
            NodeContainerFigure fig = (NodeContainerFigure)getFigure();
            switch (templInfo.getRole()) {
                case Link:
                    Image i;
                    switch (templInfo.getUpdateStatus()) {
                        case HasUpdate:
                            i = META_NODE_LINK_RED_ICON;
                            break;
                        case UpToDate:
                            i = META_NODE_LINK_GREEN_ICON;
                            break;
                        default:
                            i = META_NODE_LINK_PROBLEM_ICON;
                    }
                    fig.setMetaNodeLinkIcon(i);
                    break;
                default:
                    fig.setMetaNodeLinkIcon(null);
            }
        }
    }

    private void checkMetaNodeLockIcon() {
        NodeContainerUI nc = getNodeContainer();
        if (nc instanceof WorkflowManagerUI || nc instanceof SubNodeContainerUI) {
            WorkflowManagerUI wm =
                nc instanceof WorkflowManagerUI ? (WorkflowManagerUI)nc : ((SubNodeContainerUI)nc).getWorkflowManager();
            Image i;
            if (wm.isEncrypted()) {
                if (wm.isUnlocked()) {
                    i = META_NODE_UNLOCK_ICON;
                } else {
                    i = META_NODE_LOCK_ICON;
                }
            } else {
                i = null;
            }
            NodeContainerFigure fig = (NodeContainerFigure)getFigure();
            fig.setMetaNodeLockIcon(i);
        }
    }

    private void checkNodeLockIcon() {
        NodeContainerUI nc = getNodeContainer();
        Image i;
        StringBuilder toolTip = new StringBuilder();
        //node is considered being locked if it is either lock from being reset, it's not deletable, or the dialog is locked
        if (nc.getNodeLocks().hasResetLock() || nc.getNodeLocks().hasDeleteLock() || nc.getNodeLocks().hasConfigureLock()) {
            toolTip.append("Node Locked (");
            i = NODE_LOCK_ICON;
            if (nc.getNodeLocks().hasResetLock()) {
                toolTip.append("Reset, ");
            }
            if (nc.getNodeLocks().hasDeleteLock()) {
                toolTip.append("Delete, ");
            }
            if (nc.getNodeLocks().hasConfigureLock()) {
                toolTip.append("Configure, ");
            }
            toolTip.setLength(toolTip.length() - 2);
            toolTip.append(")");
        } else {
            i = null;
        }
        NodeContainerFigure fig = (NodeContainerFigure)getFigure();
        fig.setNodeLockIcon(i, toolTip.toString());
    }

    private void checkModifiablePortIcon() {
        if (getNodeContainer() instanceof NativeNodeContainerUI) {
            NativeNodeContainerUI nnc = (NativeNodeContainerUI)getNodeContainer();
            try {
                if (!nnc.getCopyOfCreationConfig().map(ncc -> ncc.getPortConfig().isPresent()).orElse(false)) {
                    return;
                }
            } catch (UnsupportedOperationException e) {
                // still show '...'-figure to inform the user about dynamic ports not being supported
            }
            NodeContainerFigure fig = (NodeContainerFigure)getFigure();
            fig.setModifiablePortIcon(this);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void childAdded(final EditPart child, final int index) {
        // TODO Auto-generated method stub
    }

    /** {@inheritDoc} */
    @Override
    public void partActivated(final EditPart editpart) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void partDeactivated(final EditPart editpart) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void removingChild(final EditPart child, final int index) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void selectedStateChanged(final EditPart editpart) {
        LOGGER.debug(getNodeContainer().toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void propertyChange(final PropertyChangeEvent pce) {
        if (pce.getProperty().equals(PreferenceConstants.P_NODE_LABEL_FONT_SIZE)) {
            NodeContainerFigure fig = (NodeContainerFigure)getFigure();
            Object value = pce.getNewValue();
            Integer fontSize = null;
            if (value == null) {
                return;
            } else if (value instanceof Integer) {
                fontSize = (Integer)value;
            } else if (value instanceof String && !value.toString().isEmpty()) {
                try {
                    fontSize = Integer.parseInt((String)value);
                } catch (NumberFormatException e) {
                    LOGGER.warn("Setting " + PreferenceConstants.P_NODE_LABEL_FONT_SIZE
                        + " could not be updated. Unable to parse the value.");
                }
            }
            if (fontSize != null) {
                fig.setFontSize(fontSize);
                Display.getCurrent().syncExec(new Runnable() {
                    @Override
                    public void run() {
                        updateFigureFromUIinfo(getNodeContainer().getUIInformation());
                        getRootEditPart().getFigure().invalidate();
                        getRootEditPart().refreshVisuals();
                    }
                });
            }
            return;
        }
    }

    /** @return underlying workflow root */
    WorkflowRootEditPart getRootEditPart() {
        EditPartViewer viewer = getViewer();
        if (viewer != null && viewer.getRootEditPart().getContents() instanceof WorkflowRootEditPart) {
            WorkflowRootEditPart part = (WorkflowRootEditPart)viewer.getRootEditPart().getContents();
            return part;
        }
        return null;
    }

    /** Change hide/show node label status. */
    public void callHideNodeName() {
        WorkflowRootEditPart root = getRootEditPart();
        if (root != null) {
            NodeContainerFigure ncFigure = (NodeContainerFigure)getFigure();
            ncFigure.hideNodeName(root.hideNodeNames());
        }
    }

    /** Called when the name (label above the node) changes. */
    public void updateHeaderField() {
        updateLabelText();
        // Bug 3037 trigger refresh of bounds
        getNodeContainer().setUIInformation(getNodeContainer().getUIInformation());
        refreshBounds();
    }

    /**
     * Updates the label text either with or without the node id, depending on the settings in the root.
     */
    private void updateLabelText() {
        WorkflowRootEditPart root = getRootEditPart();
        NodeContainerFigure ncFigure = (NodeContainerFigure)getFigure();
        String nodeName = getNodeContainer().getName();
        if (root != null && root.showNodeId()) {
            nodeName += " (#" + getNodeContainer().getID().getIndex() + ")";
        }
        ncFigure.setLabelText(nodeName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("rawtypes")
    public Object getAdapter(final Class adapter) {
        if (adapter == IPropertySource.class) {
            return new NodeContainerProperties(Wrapper.unwrapNC(getNodeContainer()));
        }
        return super.getAdapter(adapter);
    }

    /**
     * @param sourceNode
     * @param srcPortIdx
     * @return the first free port the specified port could be connected to. Or -1 if there is none.
     */
    public int getFreeInPort(final ConnectableEditPart sourceNode, final int srcPortIdx) {
        WorkflowManagerUI wm = getWorkflowManager();
        if (wm == null || sourceNode == null || srcPortIdx < 0) {
            return -1;
        }
        int startPortIdx = 1; // skip variable ports
        int connPortIdx = -1;
        NodeContainerUI nc = getNodeContainer();
        if (nc instanceof WorkflowManagerUI) {
            startPortIdx = 0;
        }
        for (int i = startPortIdx; i < nc.getNrInPorts(); i++) {
            if (wm.canAddNewConnection(sourceNode.getNodeContainer().getID(), srcPortIdx, nc.getID(), i)) {
                connPortIdx = i;
                break;
            }
        }
        if ((connPortIdx < 0) && (startPortIdx == 1)
            && wm.canAddConnection(sourceNode.getNodeContainer().getID(), srcPortIdx, nc.getID(), 0)) {
            // if the src is a flow var port, connect it to impl flow var port
            connPortIdx = 0;
        }
        return connPortIdx;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionAnchor getTargetConnectionAnchor(final ConnectionEditPart connection) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionAnchor getSourceConnectionAnchor(final ConnectionEditPart connection) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionAnchor getSourceConnectionAnchor(final Request request) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectionAnchor getTargetConnectionAnchor(final Request request) {
        if (!(request instanceof CreateConnectionRequest)) {
            return null;
        }
        CreateConnectionRequest req = (CreateConnectionRequest)request;
        Command cmd = req.getStartCommand();
        if (!(cmd instanceof CreateConnectionCommand)) {
            return null;
        }
        CreateConnectionCommand connCmd = (CreateConnectionCommand)cmd;
        // find a free port that could take the connection
        int portIdx = getFreeInPort(connCmd.getSourceNode(), connCmd.getSourcePortID());
        if (portIdx < 0) {
            return null;
        }
        for (Object part : getChildren()) {
            if (part instanceof AbstractPortEditPart) {
                AbstractPortEditPart port = (AbstractPortEditPart)part;
                if (port.isInPort() && port.getIndex() == portIdx) {
                    return port.getTargetConnectionAnchor(request);
                }
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")  // getChildren().stream() ...
    @Override
    public void workflowEditorModeWasSet(final WorkflowEditorMode newMode) {
        m_currentEditorMode = newMode;

        ((NodeContainerFigure)getFigure()).workflowEditorModeWasSet(newMode);
        getAllConnections();
        Stream.of(getAllConnections()).forEach((c) -> {
           c.workflowEditorModeWasSet(newMode);
        });

        getChildren().stream().forEach((c) -> {
            if (c instanceof AbstractPortEditPart) {
                ((AbstractPortEditPart)c).workflowEditorModeWasSet(newMode);
            }
        });
    }
}
