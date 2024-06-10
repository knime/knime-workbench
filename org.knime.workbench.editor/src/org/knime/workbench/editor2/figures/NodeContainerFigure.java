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
 */
package org.knime.workbench.editor2.figures;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.Platform;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.DelegatingLayout;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FlowLayout;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.MouseMotionListener;
import org.eclipse.draw2d.OrderedLayout;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.RelativeLocator;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.interactive.ReExecutable;
import org.knime.core.node.workflow.NativeNodeContainer;
import org.knime.core.node.workflow.NativeNodeContainer.LoopStatus;
import org.knime.core.node.workflow.NodeContainerState;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.SingleNodeContainerUI;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.EditorModeParticipant;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.WorkflowEditorMode;
import org.knime.workbench.editor2.actions.ports.PortActionCreator;
import org.knime.workbench.editor2.editparts.FontStore;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.figures.ProgressFigure.ProgressMode;

/**
 * Figure displaying a <code>NodeContainer</code> in a workflow. This serves as
 * a container for <code>NodeInPortFigure</code>s and
 * <code>NodeOutPortFigure</code>s
 *
 * This figure is composed of the following sub-figures:
 * <ul>
 * <li><code>ContentFigure</code> - hosts the child visuals (port figures) and
 * the center icon</li>
 * <li><code>StatusFigure</code> - contains description text and some color
 * codes</li>
 * <li><code>ProgressFigure</code> - displaying the execution progress</li>
 * </ul>
 *
 * @author Florian Georg, University of Konstanz
 * @author Christoph Sieb, University of Konstanz
 */
public class NodeContainerFigure extends RectangleFigure implements EditorModeParticipant {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(NodeContainerFigure.class);

    // default plugin ID to get icons/images from
    private static final String EDITOR_PLUGIN_ID = KNIMEEditorPlugin.PLUGIN_ID;

    /** absolute width of this figure. */
    public static final int WIDTH = SymbolFigure.SYMBOL_FIG_WIDTH + (2 * AbstractPortFigure.getPortSizeNode());

    /** absolute height of this figure. */
    public static final int HEIGHT = 48;

    /** Red traffic light. */
    public static final Image RED = ImageRepository.getUnscaledImage(EDITOR_PLUGIN_ID, "icons/ampel_red.png");
    /** Ghostly form of the red traffic light. */
    public static final Image RED_GHOSTLY = makeImageGhostly(RED);

    /** Yellow traffic light. */
    public static final Image YELLOW = ImageRepository.getUnscaledImage(EDITOR_PLUGIN_ID, "icons/ampel_yellow.png");
    /** Ghostly form of the yello traffic light. */
    public static final Image YELLOW_GHOSTLY = makeImageGhostly(YELLOW);

    /** Green traffic light. */
    public static final Image GREEN = ImageRepository.getUnscaledImage(EDITOR_PLUGIN_ID, "icons/ampel_green.png");
    /** Ghostly form of the green traffic light. */
    public static final Image GREEN_GHOSTLY = makeImageGhostly(GREEN);

    /** Inactive traffic light. */
    public static final Image INACTIVE = ImageRepository.getUnscaledImage(EDITOR_PLUGIN_ID, "icons/ampel_inactive.png");
    /** Ghostly form of the inactive traffic light. */
    public static final Image INACTIVE_GHOSTLY = makeImageGhostly(INACTIVE);

    /** Info sign. */
    public static final Image INFO_SIGN = ImageRepository.getUnscaledImage(EDITOR_PLUGIN_ID, "icons/roundInfo.jpg");
    /** Ghostly form of the info sign. */
    public static final Image INFO_SIGN_GHOSTLY = makeImageGhostly(INFO_SIGN);

    /** Warning sign. */
    public static final Image WARNING_SIGN = ImageRepository.getUnscaledImage(EDITOR_PLUGIN_ID, "icons/warning.gif");
    /** Ghostly form of the warning sign. */
    public static final Image WARNING_SIGN_GHOSTLY = makeImageGhostly(WARNING_SIGN);

    /** Error sign. */
    public static final Image ERROR_SIGN = ImageRepository.getUnscaledImage(EDITOR_PLUGIN_ID, "icons/error.png");
    /** Ghostly form of the error sign. */
    public static final Image ERROR_SIGN_GHOSTLY = makeImageGhostly(ERROR_SIGN);

    /** Delete sign. */
    public static final Image DELETE_SIGN = ImageRepository.getUnscaledImage(EDITOR_PLUGIN_ID, "icons/delete.png");

    /** Loop End Node extra icon: In Progress. */
    public static final Image LOOP_IN_PROGRESS_SIGN =
        ImageRepository.getUnscaledImage(EDITOR_PLUGIN_ID, "icons/loop_in_progress.png");
    /** Loop End Node extra icon: In Progress - for use in AE mode */
    public static final Image LOOP_IN_PROGRESS_SIGN_GHOSTLY = makeImageGhostly(LOOP_IN_PROGRESS_SIGN);

    /** Loop End Node extra icon: Done. */
    public static final Image LOOP_DONE_SIGN =
        ImageRepository.getUnscaledImage(EDITOR_PLUGIN_ID, "icons/loop_done.png");
    /** Loop End Node extra icon: Done - for use in AE mode */
    public static final Image LOOP_DONE_SIGN_GHOSTLY = makeImageGhostly(LOOP_DONE_SIGN);

    /** Loop End Node extra icon: No Status. */
    public static final Image LOOP_NO_STATUS =
        ImageRepository.getUnscaledImage(EDITOR_PLUGIN_ID, "icons/loop_nostatus.png");
    /** Loop End Node extra icon: No Status - for use in AE mode */
    public static final Image LOOP_NO_STATUS_GHOSTLY = makeImageGhostly(LOOP_NO_STATUS);

    /** Replace-node sign. */
    public static final Image REPLACE_SIGN = ImageRepository.getUnscaledImage(EDITOR_PLUGIN_ID, "icons/replace-node.png");

    /** State: Node not configured. */
    public static final int STATE_NOT_CONFIGURED = 0;

    /** dummy font for status figure. Needs a "small" font... */
    private static final Font NODE_FONT = FontStore.INSTANCE.getDefaultFont(3);

    private static final Color HEADING_CONTAINER_FOREGROUND = ColorConstants.black;


    /** Re-execution sign */
    private static final Image REEXECUTION_SIGN = ImageRepository.getUnscaledImage(EDITOR_PLUGIN_ID, "icons/re-execution-active.png");

    /** content pane, contains the port visuals and the icon. */
    private final SymbolFigure m_symbolFigure;

    /** contains the the "traffic light". * */
    private final StatusFigure m_statusFigure;

    /** contains the "progress bar". * */
    private ProgressFigure m_progressFigure;

    /** contains the image indicating the loop status (if available). */
    private Image m_loopStatusFigure = null;
    // For rendering in AE mode
    private Image m_loopStatusGhostlyFigure = null;

    /** The background color to apply. */
    private final Color m_backgroundColor;

    /** contains the the warning/error sign. */
    private final InfoWarnErrorPanel m_infoWarnErrorPanel;

    /** The node name, e.g File Reader. */
    private final Figure m_headingContainer;

    private String m_label;

    /**
     * Tooltip for displaying the custom description. This tooltip is displayed
     * with the custom name.
     */
    private final NewToolTipFigure m_symbolTooltip;

    private Image m_jobExec;

    private Image m_metaNodeLinkIcon;

    private Image m_reExecutionIcon;

    private Image m_metaNodeLockIcon;

    private Image m_nodeLockIcon;

    private boolean m_showFlowVarPorts;

    private WorkflowEditorMode m_currentEditorMode = WorkflowEditor.INITIAL_EDITOR_MODE;

    /**
     * Creates a new node figure.
     *
     * @param progressFigure the progress figure for this node
     */
    public NodeContainerFigure(final ProgressFigure progressFigure) {

        m_backgroundColor = ColorConstants.white;
        m_showFlowVarPorts = false;

        setOpaque(false);
        setFill(false);
        setOutline(false);

        // add sub-figures
        setLayoutManager(new DelegatingLayout());

        super.setFont(FontStore.INSTANCE.getDefaultFont(FontStore.getFontSizeFromKNIMEPrefPage()));


        // Heading (Label)
        m_headingContainer = new Figure();

        // icon
        m_symbolFigure = new SymbolFigure();
        m_symbolTooltip = new NewToolTipFigure("");

        // Status: traffic light
        m_statusFigure = new StatusFigure();
        // progress bar
        if (progressFigure != null) {
            m_progressFigure = progressFigure;
        } else {
            m_progressFigure = new ProgressFigure();
        }
        m_progressFigure.setCurrentDisplay(Display.getCurrent());
        m_progressFigure.setOpaque(true);
        // Additional status (warning/error sign)
        m_infoWarnErrorPanel = new InfoWarnErrorPanel();

        // the locators depend on the order!
        add(m_headingContainer);
        add(m_symbolFigure);
        add(m_statusFigure);
        add(m_infoWarnErrorPanel);

        // layout the components
        setConstraint(m_headingContainer, new NodeContainerLocator(this));
        setConstraint(m_symbolFigure, new NodeContainerLocator(this));
        setConstraint(m_infoWarnErrorPanel, new NodeContainerLocator(this));
        setConstraint(m_statusFigure, new NodeContainerLocator(this));
    }

    /**
     * @return true if implicit flow variable ports are currently shown
     */
    boolean getShowFlowVarPorts() {
        return m_showFlowVarPorts;
    }

    /**
     * @param showPorts true if implicit flow variable ports should be shown
     */
    public void setShowFlowVarPorts(final boolean showPorts) {
        m_showFlowVarPorts = showPorts;
    }

    /**
     * This method should be invoked when its parent is being deactivated (in the GEF sense.)
     */
    public void figureIsBeingDisposed() {
        // The other figures containing images contains class-static Image instances, so we are only concerned
        //      with the SymbolFigure instance.
        m_symbolFigure.disposeGhostlyImage();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(final IFigure figure, final Object constraint,
            final int index) {
        // the only one who is allowed to add objects at a certain index
        // are we (with the method addAtIndex).
        // Ports are added by the framework with index - but we need them
        // at the end of the list (the layouter depends on it).
        // We ignore any index provided.
        super.add(figure, constraint, getChildren().size());
    }

    private void addAtIndex(final IFigure figure, final int index) {
        super.add(figure, null, index);
    }

    /**
     * Returns the progress figure of this node figure. The figure is also a
     * progress listener and can be registered with node progress monitors. It
     * renders the progress then.
     *
     * @return the progress figure associated with this node container figure.
     */
    public ProgressFigure getProgressFigure() {
        return m_progressFigure;
    }

    /**
     * @return the figure showing the status (traffic light)
     */
    public StatusFigure getStatusFigure() {
        return m_statusFigure;
    }

    /**
     * @return the dimension of the status bar figure
     */
    public static Dimension getStatusBarDimension() {
        org.eclipse.swt.graphics.Rectangle r = RED.getBounds();

        return new Dimension(r.width, r.height);
    }

    /**
     * @return the figure showing the error and warning symbol (e.g. warning triangle)
     */
    public InfoWarnErrorPanel getInfoWarnErrorPanel() {
        return m_infoWarnErrorPanel;
    }

    /**
     * Sets the icon.
     *
     * @param icon the icon
     */
    public void setIcon(final Image icon) {
        m_symbolFigure.setIcon(icon);
    }

    /**
     * Utility method to scale an image to the given size
     *
     * @param size target size in px
     * @param img the image data to scale
     * @return the scaled image data
     */
    public static ImageData scaleImageTo(final int size, final ImageData img) {
        int max = Math.max(img.width, img.height);
        if (max == size) {
            return img;
        }
        double f = size / (double)max;
        return img.scaledTo((int)(img.width * f), (int)(img.height * f));
    }

    /**
     * Sets the type.
     *
     * @param type the type
     * @param isComponent
     */
    public void setType(final NodeType type, final boolean isComponent) {
        m_symbolFigure.setType(type, isComponent);
    }

    /**
     * @param jobExecIcon the icon associated with job execution
     */
    public void setJobExecutorIcon(final Image jobExecIcon) {
        if (m_jobExec != null) {
            m_jobExec.dispose();
        }

        m_jobExec = jobExecIcon;
        m_symbolFigure.refreshJobManagerIcon();
    }

    /**
     * @param icon the icon associated with metanode links
     */
    public void setMetaNodeLinkIcon(final Image icon) {
        if (!Objects.equals(m_metaNodeLinkIcon, icon)) {
            m_metaNodeLinkIcon = icon;
            m_symbolFigure.refreshMetaNodeLinkIcon();
        }
    }

    /**
     * @param icon the icon associated with metanode locks
     */
    public void setMetaNodeLockIcon(final Image icon) {
        if (!Objects.equals(m_metaNodeLockIcon, icon)) {
            m_metaNodeLockIcon = icon;
            m_symbolFigure.refreshMetaNodeLockIcon();
        }
    }

    /**
     * @param icon the icon associated with re-execution
     */
    private void setReExecutionIcon(final Image icon) {
        if (!Objects.equals(m_reExecutionIcon, icon)) {
            m_reExecutionIcon = icon;
            m_symbolFigure.refreshReExecutionIcon();
        }
    }

    /**
     * @param icon the lock icon
     * @param lockToolTip a tool tip describing the type of node lock, if <code>null</code> or empty, no tool tip will
     *            be set
     */
    public void setNodeLockIcon(final Image icon, final String lockToolTip) {
        m_nodeLockIcon = icon;
        m_symbolFigure.refreshNodeLockIcon(lockToolTip);
    }

    /**
     * Sets the modifiable port icon.
     *
     * @param nodeContainerEditPart the node container edit part
     */
    public void setModifiablePortIcon(final NodeContainerEditPart nodeContainerEditPart) {
        m_symbolFigure.setModifiablePortIcon(nodeContainerEditPart);
    }

    /**
     * Sets the text of the heading label.
     *
     * @param text The text to set.
     */
    @SuppressWarnings("unchecked")
    public void setLabelText(final String text) {
        m_label = text;
        m_headingContainer.removeAll();
        // needed, otherwise labels disappear after font size has changed
        m_headingContainer.setBounds(new Rectangle(0, 0, 0, 0));

        Font boldFont = FontStore.INSTANCE.getDefaultFontBold(FontStore.getFontSizeFromKNIMEPrefPage());
        m_headingContainer.setFont(boldFont);

        int width = 0;
        for (String s : wrapText(text).split("\n")) {
            Label l = new Label(s) {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public Dimension getPreferredSize(final int wHint, final int hHint) {
                    Dimension d = super.getPreferredSize(wHint, hHint).getCopy();
                    // headings labels are too small when the editor is zoomed.
                    d.width = (int)(d.width * 1.1);
                    return d;
                }
            };
            l.setForegroundColor(HEADING_CONTAINER_FOREGROUND);
            l.setFont(boldFont);
            m_headingContainer.add(l);
            Dimension size = l.getPreferredSize();
            width = Math.max(width, size.width);
        }
        int height = 0;
        for (IFigure child : (List<IFigure>)m_headingContainer.getChildren()) {
            Dimension size = child.getPreferredSize();
            int offset = (width - size.width) / 2;

            child.setBounds(new Rectangle(offset, height, size.width, size.height));
            height += size.height;
        }

        m_headingContainer.setBounds(new Rectangle(0, 0, width, height));
        repaint();
    }

    private static String wrapText(final String text) {
        if (text == null || text.length() == 0) {
            return "";
        }
        // wrap the text with line breaks if too long
        // we split just one time (i.e. two lines at most)
        if (text.trim().length() < 20) {
            return text.trim();
        }
        if (text.trim().indexOf(" ") < 0) {
            return text.trim();
        }
        final int middle = text.length() / 2;
        // now go left and right to the next space
        // the closest space is used for a split
        int indexLeft = middle;
        int indexRight = middle + 1;
        for (; indexLeft >= 0 && indexRight < text.length(); indexLeft--, indexRight++) {
            if (text.charAt(indexLeft) == ' ') {
                final StringBuilder sb = new StringBuilder(text);
                return sb.replace(indexLeft, indexLeft + 1, "\n").toString();
            }
            if (text.charAt(indexRight) == ' ') {
                final StringBuilder sb = new StringBuilder(text);
                return sb.replace(indexRight, indexRight + 1, "\n").toString();
            }
        }

        return text;
    }

    // TODO general image utilities class (along with AnnotationEditPart and other locations)
    private static Image makeImageGhostly(final Image image) {
        Image iWithoutDataProvider = null;

        if (Platform.OS_MACOSX.equals(Platform.getOS())) {
            iWithoutDataProvider = new Image(Display.getCurrent(), image.getImageData());
        }

        final Image i = new Image(Display.getCurrent(), iWithoutDataProvider != null ? iWithoutDataProvider : image, SWT.IMAGE_GRAY);

        final ImageData id = i.getImageData();
        id.alpha = 32;
        if (id.getTransparencyType() != SWT.TRANSPARENCY_ALPHA) {
            id.transparentPixel = -1;
            id.maskData = null;
        }

        final Image ghostlyImage = new Image(Display.getCurrent(), id);
        i.dispose();
        if (iWithoutDataProvider != null) {
            iWithoutDataProvider.dispose();
        }

        return ghostlyImage;
    }

    /**
     * Sets the description for this node as the symbol's tooltip.
     *
     * @param description the description to set as tooltip
     */
    public void setCustomDescription(final String description) {
        if (description == null || description.trim().equals("")) {
            m_symbolTooltip.setText("");
            m_symbolFigure.setToolTip(null);
        } else {
            m_symbolTooltip.setText(description);
            m_symbolFigure.setToolTip(m_symbolTooltip);
        }
    }

    private boolean isChild(final Figure figure) {
        for (final Object contentFigure : getChildren()) {
            if (contentFigure == figure) {
                return true;
            }
        }

        return false;
    }

    /**
     * Replaces the status traffic light with the progress bar. This is done
     * once the node is paused, queued or executing.
     *
     * @param the mode @see ProgressMode.
     */
    private void setProgressBar(final ProgressMode mode) {

        // remove both intergangable onse
        if (isChild(m_statusFigure)) {
            remove(m_statusFigure);
        }

        boolean alreadySet = false;
        m_progressFigure.reset();
        // and set the progress bar
        if (!isChild(m_progressFigure)) {

            // reset the progress first
            addAtIndex(m_progressFigure, 2);
            setConstraint(m_progressFigure, new NodeContainerLocator(this));

        } else {
            // if already set, remember this
            alreadySet = true;
        }

        switch (mode) {
        case EXECUTING:
            // temporarily remember the execution state of the progress bar
            final ProgressMode oldMode = m_progressFigure.getProgressMode();
            m_progressFigure.setProgressMode(ProgressMode.EXECUTING);
            m_progressFigure.setStateMessage("Executing");

            // if the progress bar was not set already
            // init it with an unknown progress first
            if (!alreadySet || !ProgressMode.EXECUTING.equals(oldMode)) {
                m_progressFigure.activateUnknownProgress();
            }
            break;
        case QUEUED:
            m_progressFigure.setProgressMode(ProgressMode.QUEUED);
            m_progressFigure.setStateMessage("Queued");
            break;
        case PAUSED:
            m_progressFigure.setProgressMode(ProgressMode.PAUSED);
            m_progressFigure.setStateMessage("Paused");
            break;
        default:
            throw new AssertionError("Unhandled switch case: " + mode);
        }
    }

    private void setStatusAmple() {
        // in every case reset the progress bar
        m_progressFigure.reset();

        // remove both intergangable onse
        if (isChild(m_progressFigure)) {
            remove(m_progressFigure);
            m_progressFigure.stopUnknownProgress();
            m_progressFigure.setProgressMode(ProgressMode.QUEUED);
        }

        // and set the progress bar
        if (!isChild(m_statusFigure)) {
            addAtIndex(m_statusFigure, 2);
            setConstraint(m_statusFigure, new NodeContainerLocator(this));
        }
    }

    private Image imageReflectingEditMode(final Image image, final Image ghostlyImage) {
        return WorkflowEditorMode.NODE_EDIT.equals(m_currentEditorMode) ? image : ghostlyImage;
    }

    /**
     *
     * @param nc new state of underlying node
     */
    public void setStateFromNC(final NodeContainerUI nc) {
        boolean isInactive = false;
        LoopStatus loopStatus = LoopStatus.NONE;
        if (nc instanceof SingleNodeContainerUI) {
            SingleNodeContainerUI snc = (SingleNodeContainerUI)nc;
            isInactive = snc.isInactive();
            if (Wrapper.wraps(snc, NativeNodeContainer.class)) {
                NativeNodeContainer nnc = Wrapper.unwrap(snc, NativeNodeContainer.class);
                loopStatus = nnc.getLoopStatus();
            }
        }
        NodeContainerState state = nc.getNodeContainerState();
        if (!isInactive) {
            if (state.isIdle()) {
                setStatusAmple();
                m_statusFigure.setIcon(RED, RED_GHOSTLY);
            } else if (state.isConfigured()) {
                setStatusAmple();
                m_statusFigure.setIcon(YELLOW, YELLOW_GHOSTLY);
            } else if (state.isExecuted()) {
                setStatusAmple();
                m_statusFigure.setIcon(GREEN, GREEN_GHOSTLY);
            } else if (state.isWaitingToBeExecuted()) {
                if (LoopStatus.PAUSED.equals(loopStatus)) {
                    setProgressBar(ProgressMode.PAUSED);
                } else {
                    setProgressBar(ProgressMode.QUEUED);
                }
            } else if (state.isExecutionInProgress()) {
                setProgressBar(ProgressMode.EXECUTING);
            } else {
                setStatusAmple();
                m_statusFigure.setIcon(INACTIVE, INACTIVE_GHOSTLY);
            }
        } else {
            setStatusAmple();
            m_statusFigure.setIcon(INACTIVE, INACTIVE_GHOSTLY);
        }
        setReExecutableStatus(nc);
        setLoopStatus(loopStatus, state.isExecuted());
        repaint();
    }

    private void setReExecutableStatus(final NodeContainerUI nc) {
        if (Wrapper.wraps(nc, NativeNodeContainer.class)) {
            var nnc = Wrapper.unwrap(nc, NativeNodeContainer.class);
            if(nnc.getNodeModel() instanceof ReExecutable) {
                if (((ReExecutable) nnc.getNodeModel()).canTriggerReExecution()) {
                    setReExecutionIcon(REEXECUTION_SIGN);
                } else {
                    setReExecutionIcon(null);
                }
            }
        }
    }

    /**
     *
     * @param msg the node message
     */
    public void setMessage(final NodeMessage msg) {
        removeMessages();
        if (msg == null || msg.getMessageType() == null) {
            NodeLogger.getLogger(NodeContainerFigure.class).warn("Received NULL message!");
        } else {
            switch (msg.getMessageType()) {
                case RESET:
                    break;
                case WARNING:
                    m_infoWarnErrorPanel.setWarning(msg);
                    break;
                case ERROR:
                    m_infoWarnErrorPanel.setError(msg);
                    break;
                default:
                    throw new AssertionError("Unhandled switch case: " + msg.getMessageType());
            }
        }
        m_statusFigure.repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsPoint(final int x, final int y) {
        if (!getBounds().contains(x, y)) {
            return false;
        }
        for (final Object contentFigure : getChildren()) {
            if (((IFigure)contentFigure).containsPoint(x, y)) {
                return (contentFigure != m_headingContainer);
            }
        }
        return false;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension getMinimumSize(final int whint, final int hhint) {
        return getPreferredSize(whint, hhint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension getPreferredSize(final int wHint, final int hHint) {

        int prefWidth = Math.max(WIDTH, m_headingContainer.getBounds().width);

        int prefHeight = 0;
        int compCount = 3;
        prefHeight += m_headingContainer.getPreferredSize().height;
        prefHeight += m_symbolFigure.getPreferredSize().height;
        // metanode don't have a status figure
        if (isChild(m_statusFigure) || isChild(m_progressFigure)) {
            prefHeight += m_statusFigure.getPreferredSize().height;
            compCount++;
        }
        // warning sign is painted over the status traffic light - don't account for it.
        prefHeight += ((compCount - 1) * NodeContainerLocator.GAP);
        // make sure all ports fit in the figure (ports start below the label)
        int minPortsHeight = m_headingContainer.getPreferredSize().height + getMinimumPortsHeight();
        if (minPortsHeight > prefHeight) {
            prefHeight = minPortsHeight;
        }
        return new Dimension(prefWidth, prefHeight);
    }

    /**
     * @return the minimum height required to display all (input or output) ports
     */
    private int getMinimumPortsHeight() {
        int minH = 0;
        NodeInPortFigure inPort = null;
        NodeOutPortFigure outPort = null;
        for (Object o : getChildren()) {
            if ((inPort == null) && (o instanceof NodeInPortFigure)) {
                inPort = (NodeInPortFigure)o;
            }
            if ((outPort == null) && (o instanceof NodeOutPortFigure)) {
                outPort = (NodeOutPortFigure)o;
            }
        }
        if (inPort != null) {
            int minIn = ((NodePortLocator)inPort.getLocator()).getMinimumHeightForPorts();
            if (minH < minIn) {
                minH = minIn;
            }
        }
        if (outPort != null) {
            int minOut = ((NodePortLocator)outPort.getLocator()).getMinimumHeightForPorts();
            if (minH < minOut) {
                minH = minOut;
            }
        }
        return minH;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Color getBackgroundColor() {
        if (!WorkflowEditorMode.NODE_EDIT.equals(m_currentEditorMode)) {
            return ColorConstants.lightGray;
        }

        return m_backgroundColor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Color getForegroundColor() {
        return ColorConstants.white;
    }

    /**
     * @return The content figure (= content pane for port visuals)
     */
    public IFigure getSymbolFigure() {
        return m_symbolFigure;
    }

    /**
     * Removes all currently set messages.
     */
    public void removeMessages() {
        m_infoWarnErrorPanel.removeAll();
    }

    /**
     * We need to set the color before invoking super. {@inheritDoc}
     */
    @Override
    protected void fillShape(final Graphics graphics) {
        graphics.setBackgroundColor(getBackgroundColor());
        super.fillShape(graphics);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paint(final Graphics graphics) {
        // paints the figure and its children
        super.paint(graphics);

        if (m_loopStatusFigure != null) {
            final Rectangle r = getSymbolFigure().getBounds();
            final Image i = WorkflowEditorMode.NODE_EDIT.equals(m_currentEditorMode) ? m_loopStatusFigure
                : m_loopStatusGhostlyFigure;

            graphics.drawImage(i, new Point(r.x + 24, r.y + 32));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintFigure(final Graphics graphics) {
      graphics.setBackgroundColor(getBackgroundColor());
      super.paintFigure(graphics);
    }


    private abstract class GhostlySupportingFigure extends Figure {
        protected Label m_figureLabel;
        protected Image m_originalIcon;
        protected Image m_ghostlyIcon;

        void disposeGhostlyImage() {
            if (m_ghostlyIcon != null) {
                m_ghostlyIcon.dispose();

                m_ghostlyIcon = null;
            }
        }

        /**
         * Sets the icon to display.
         *
         * @param icon The icon to set
         */
        void setIcon(final Image icon) {
            m_originalIcon = icon;

            disposeGhostlyImage();
            m_ghostlyIcon = makeImageGhostly(icon);

            updateFigure();
        }

        /**
         * Sets the icon and an already-created ghostly form of the icon.
         *
         * @param icon The icon to set
         * @param ghostly The ghostly version of the icon
         */
        void setIcon(final Image icon, final Image ghostly) {
            m_originalIcon = icon;
            m_ghostlyIcon = ghostly;

            updateFigure();
        }

        void updateFigure() {
            final Image icon = (m_originalIcon != null) ? imageReflectingEditMode(m_originalIcon, m_ghostlyIcon) : null;

            m_figureLabel.setIcon(icon);

            revalidate();
        }
    }

    /**
     * Subfigure, hosts the icon and the job manager icon.
     */
    protected class SymbolFigure extends GhostlySupportingFigure {
        private static final int SYMBOL_FIG_HEIGHT = 48;

        private static final int SYMBOL_FIG_WIDTH = 32;

        private final Label m_deleteIcon;
        private final Label m_replaceIcon;

        private final Label m_backgroundIcon;
        private Image m_originalBackgroundIcon;
        private Image m_ghostlyBackgroundIcon;

        private Label m_jobExecutorLabel;

        private Label m_metaNodeLinkedLabel;

        private Label m_metaNodeLockLabel;

        private Label m_reExecutionLabel;

        private Label m_nodeLockLabel;

        private Label m_modifiablePortLabel;

        private NodeType m_nodeType;

        private boolean m_isComponent = false;

        /**
         * Creates a new figure containing the symbol. That is the background
         * icon (depending on the type of the node) and the node's icon. Also
         * the job manager indicator and the mark for deletion.
         */
        public SymbolFigure() {
            // delegating layout, children provide a Locator as constraint
            final DelegatingLayout layout = new DelegatingLayout();
            setLayoutManager(layout);
            setOpaque(false);
            setFill(false);


            // the "frame", that indicates the node type
            m_backgroundIcon = new Label();

            // create a label that shows the nodes' icon
            m_figureLabel = new Label();
            m_figureLabel.setOpaque(false);

            // create the delete icon
            m_deleteIcon = new Label();
            m_deleteIcon.setOpaque(false);
            m_deleteIcon.setIcon(DELETE_SIGN);

            // create the replacement icon
            m_replaceIcon = new Label();
            m_replaceIcon.setOpaque(false);
            m_replaceIcon.setIcon(REPLACE_SIGN);

            // center the icon figure
            add(m_backgroundIcon);
            m_backgroundIcon.setLayoutManager(new DelegatingLayout());
            m_backgroundIcon.add(m_figureLabel);
            m_backgroundIcon.setConstraint(m_figureLabel, new RelativeLocator(
                    m_backgroundIcon, 0.5, 0.5));

            setConstraint(m_backgroundIcon, new RelativeLocator(this, 0.5, 0.5));
        }

        /**
         * Refreshes the job manager icon.
         */
        protected void refreshJobManagerIcon() {
            // do we have to remove it?
            if (m_jobExecutorLabel != null && m_jobExec == null) {
                m_backgroundIcon.remove(m_jobExecutorLabel);
                m_jobExecutorLabel = null;
            } else {
                if (m_jobExecutorLabel == null) {
                    m_jobExecutorLabel = new Label();
                    m_jobExecutorLabel.setOpaque(false);
                    m_backgroundIcon.add(m_jobExecutorLabel);
                    m_backgroundIcon.setConstraint(m_jobExecutorLabel,
                            new RelativeLocator(m_backgroundIcon, 0.73, 0.73));
                }
                m_jobExecutorLabel.setIcon(m_jobExec);
                repaint();
            }
        }

        /**
         * Refreshes the metanode link icon.
         */
        protected void refreshMetaNodeLinkIcon() {
            // do we have to remove it?
            if (m_metaNodeLinkedLabel != null && m_metaNodeLinkIcon == null) {
                m_backgroundIcon.remove(m_metaNodeLinkedLabel);
                m_metaNodeLinkedLabel = null;
            } else {
                if (m_metaNodeLinkedLabel == null) {
                    m_metaNodeLinkedLabel = new Label();
                    m_metaNodeLinkedLabel.setOpaque(false);
                    m_backgroundIcon.add(m_metaNodeLinkedLabel);
                    m_backgroundIcon.setConstraint(m_metaNodeLinkedLabel,
                            new RelativeLocator(m_backgroundIcon, 0.21, .84));
                }
                m_metaNodeLinkedLabel.setIcon(m_metaNodeLinkIcon);
                repaint();
            }
        }

        /**
         * Refreshes the metanode lock icon.
         */
        protected void refreshMetaNodeLockIcon() {
            // do we have to remove it?
            if (m_metaNodeLockLabel != null && m_metaNodeLockIcon == null) {
                m_backgroundIcon.remove(m_metaNodeLockLabel);
                m_metaNodeLockLabel = null;
            } else {
                if (m_metaNodeLockLabel == null) {
                    m_metaNodeLockLabel = new Label();
                    m_metaNodeLockLabel.setOpaque(false);
                    m_backgroundIcon.add(m_metaNodeLockLabel);
                    m_backgroundIcon.setConstraint(m_metaNodeLockLabel,
                            new RelativeLocator(m_backgroundIcon, 0.79, .24));
                }
                m_metaNodeLockLabel.setIcon(m_metaNodeLockIcon);
                repaint();
            }
        }

        /**
         * Refreshes the re-execution icon.
         */
        private void refreshReExecutionIcon() {
            if (m_reExecutionLabel != null && m_reExecutionIcon == null) {
                m_backgroundIcon.remove(m_reExecutionLabel);
                m_reExecutionLabel = null;
            } else {
                if (m_reExecutionLabel == null) {
                    m_reExecutionLabel = new Label();
                    m_reExecutionLabel.setOpaque(false);
                    m_backgroundIcon.add(m_reExecutionLabel);
                    m_backgroundIcon.setConstraint(m_reExecutionLabel,
                            new RelativeLocator(m_backgroundIcon, 0.79, .24));
                }
                m_reExecutionLabel.setIcon(m_reExecutionIcon);
                repaint();
            }
        }

        /**
         * @param lockToolTip if <code>null</code> or empty no tool tip will be set
         */
        protected void refreshNodeLockIcon(final String lockToolTip) {
            // do we have to remove it?
            if (m_nodeLockLabel != null && m_nodeLockIcon == null) {
                m_backgroundIcon.remove(m_nodeLockLabel);
                m_nodeLockLabel = null;
            } else {
                if (m_nodeLockLabel == null) {
                    m_nodeLockLabel = new Label();
                    m_nodeLockLabel.setOpaque(false);
                    m_backgroundIcon.add(m_nodeLockLabel);
                    m_backgroundIcon.setConstraint(m_nodeLockLabel, new RelativeLocator(m_backgroundIcon, 0.79, .24));
                }
                m_nodeLockLabel.setIcon(m_nodeLockIcon);
                m_nodeLockLabel.setToolTip(
                    lockToolTip != null && lockToolTip.length() > 0 ? new NewToolTipFigure(lockToolTip) : null);
                repaint();
            }
        }


        /**
         * Creates the modifiable port icon and its menu.
         *
         * @param nodeContainerEditPart the node container edit part
         */
        public void setModifiablePortIcon(final NodeContainerEditPart nodeContainerEditPart) {
            if (m_modifiablePortLabel == null) {
                // setup the label
                m_modifiablePortLabel = new Label();
                m_modifiablePortLabel.setOpaque(false);
                m_backgroundIcon.add(m_modifiablePortLabel);
                m_backgroundIcon.setConstraint(m_modifiablePortLabel, new RelativeLocator(m_backgroundIcon, 0.28, .9));
                m_modifiablePortLabel.setIcon(ModifiablePortMouseTracker.TRANSPARENT_ICON);
                m_modifiablePortLabel.setToolTip(new NewToolTipFigure("Click to modify ports"));
                final ModifiablePortMouseTracker mouseTracker =
                    new ModifiablePortMouseTracker(nodeContainerEditPart, this);
                m_modifiablePortLabel.addMouseListener(mouseTracker);
                m_modifiablePortLabel.addMouseMotionListener(mouseTracker);
                repaint();
            }
        }

        Label getModifiablePortLabel() {
            return m_modifiablePortLabel;
        }

        /**
         * This determines the background image according to the "type" of the node as stored in the repository model.
         *
         * @return Image that should be uses as background for this node
         */
        private Image getBackgroundImage() {
            if (m_nodeType == null) {
                return DisplayableNodeType.getTypeForNodeType(NodeType.Unknown, m_isComponent).getImage();
            }

            final DisplayableNodeType dnt = DisplayableNodeType.getTypeForNodeType(m_nodeType, m_isComponent);
            if (dnt == null) {
                LOGGER.warn("A DisplayableNodeType has not been mapped for the node type " + m_nodeType);

                return DisplayableNodeType.getTypeForNodeType(NodeType.Unknown, m_isComponent).getImage();
            }
            return dnt.getImage();
        }

        /**
         * Sets a type specific background image.
         *
         * @param type The node type, results in a different background
         * @param isComponent whether to render a figure for a component
         * @see org.knime.workbench.repository.model.NodeTemplate
         */
        void setType(final NodeType type, final boolean isComponent) {
            m_nodeType = type;
            m_isComponent = isComponent;

            if (m_ghostlyBackgroundIcon != null) {
                m_ghostlyBackgroundIcon.dispose();
            }

            m_originalBackgroundIcon = getBackgroundImage();
            m_ghostlyBackgroundIcon = makeImageGhostly(m_originalBackgroundIcon);

            updateFigure();
        }

        @Override
        void disposeGhostlyImage() {
            super.disposeGhostlyImage();
            if (m_ghostlyBackgroundIcon != null) {
                m_ghostlyBackgroundIcon.dispose();

                m_ghostlyBackgroundIcon = null;
            }
        }

        @Override
        void updateFigure() {
            super.updateFigure();
            setBackgroundIcon(imageReflectingEditMode(m_originalBackgroundIcon, m_ghostlyBackgroundIcon));
        }

        void setBackgroundIcon(final Image icon) {
            m_backgroundIcon.setIcon(icon);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Dimension getPreferredSize(final int wHint, final int hHint) {
            return new Dimension(SYMBOL_FIG_WIDTH, SYMBOL_FIG_HEIGHT);
        }
    }


    /**
     * Subfigure containing the information/warning/error signs. The panel can
     * display any combination of the signs and also provides functionality to
     * set tool tips containing one or more messages for each category
     * (info/warning/error).
     */
    private class InfoWarnErrorPanel extends Figure {
        /**
         * The info figure.
         */
        private final InfoWarnErrorFigure m_infoFigure;

        /**
         * The warning figure.
         */
        private final InfoWarnErrorFigure m_warningFigure;

        /**
         * The error figure.
         */
        private final InfoWarnErrorFigure m_errorFigure;

        /**
         * Constructor for a new <code>SignPanel</code>.
         */
        public InfoWarnErrorPanel() {

            // a flow layout is used to arrange all signs in
            // a line

            final FlowLayout layout = new FlowLayout(true);
            layout.setMajorAlignment(OrderedLayout.ALIGN_TOPLEFT);
            layout.setMinorAlignment(OrderedLayout.ALIGN_TOPLEFT);
            layout.setMajorSpacing(3);
            setLayoutManager(layout);

            // create the info/warning/error figures
            m_infoFigure = new InfoWarnErrorFigure();
            m_infoFigure.setIcon(INFO_SIGN, INFO_SIGN_GHOSTLY);

            m_warningFigure = new InfoWarnErrorFigure();
            m_warningFigure.setIcon(WARNING_SIGN, WARNING_SIGN_GHOSTLY);

            m_errorFigure = new InfoWarnErrorFigure();
            m_errorFigure.setIcon(ERROR_SIGN, ERROR_SIGN_GHOSTLY);

            setVisible(true);
            repaint();
        }

        /**
         * Sets a new warning message.
         *
         * @param message the message to set
         */
        @SuppressWarnings("unchecked")
        void setWarning(final NodeMessage message) {

            // as the warning sign should always be before the error sign
            // but after the info sign, it must be checked in the case there
            // is only one sign so far at which position to insert the
            // warning sign
            final var children = getChildren();

            // check if there is already a warning sign
            boolean alreadyInserted = false;
            for (final var child : children) {
                if (child.equals(m_warningFigure)) {
                    alreadyInserted = true;
                }
            }

            if (!alreadyInserted) {
                // in case of 0 children the sign can simply be inserted
                if (children.size() == 0) {
                    add(m_warningFigure);
                } else if (children.size() == 2) {
                    // in case of 2 children, the warning sign must be at idx 1
                    add(m_warningFigure, 1);
                } else {
                    // else there is exact 1 child
                    final var figure = children.get(0);
                    // in case of the error sign, the warning sign has to be
                    // inserted before the error sign
                    if (figure.equals(m_errorFigure)) {
                        add(m_warningFigure, 0);
                    } else {
                        // else append at the end (after the info sign)
                        add(m_warningFigure);
                    }
                }
            }

            NodeMessage msg = message;
            if (StringUtils.isBlank(message.getMessage())) {
                msg = NodeMessage.newWarning("Warning occurred: no details.");
            }
            m_warningFigure.setToolTip(msg, WarnErrorToolTip.WARNING);

            repaint();
        }

        /**
         * Sets a new error message.
         *
         * @param message the message to set
         */
        void setError(final NodeMessage message) {

            // the error sign is always the last sign.
            add(m_errorFigure);

            NodeMessage msg = message;
            if (StringUtils.isBlank(message.getMessage())) {
                msg = NodeMessage.newWarning("Error occurred: no details.");
            }
            m_errorFigure.setToolTip(msg, WarnErrorToolTip.ERROR);

            repaint();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Dimension getPreferredSize(final int wHint, final int hHint) {
            if (getChildren().size() == 0) {
                return new Dimension(0, 0);
            }
            org.eclipse.swt.graphics.Rectangle errBnds = ERROR_SIGN.getBounds();
            org.eclipse.swt.graphics.Rectangle wrnBnds = WARNING_SIGN.getBounds();
            int h = Math.max(0, Math.max(errBnds.height, wrnBnds.height));
            int w = Math.max(0, Math.max(errBnds.width, wrnBnds.width));
            return new Dimension(w, h);
        }

        void updateFigures() {
            m_warningFigure.updateFigure();
            m_infoFigure.updateFigure();
            m_errorFigure.updateFigure();
        }
    }


    /**
     * Subfigure, contains the "traffic light".
     */
    private class StatusFigure extends GhostlySupportingFigure {
        /**
         * Creates a new bottom figure.
         */
        public StatusFigure() {
            // status figure must have exact same dimensions as progress bar
            Dimension d = getStatusBarDimension();
            setBounds(new Rectangle(0, 0, d.width, d.height));
            final ToolbarLayout layout = new ToolbarLayout(false);
            layout.setMinorAlignment(OrderedLayout.ALIGN_CENTER);
            layout.setStretchMinorAxis(true);
            setLayoutManager(layout);
            m_figureLabel = new Label();

            // the font is just set due to a bug in the getPreferredSize
            // method of a label which accesses the font somewhere
            // if not set a NPE is thrown.
            // PO: Set a small font. The status image (as icon of the label) is
            // placed at the bottom of the label, which is too low, if the
            // font is bigger than the slot for the image.
            m_figureLabel.setFont(NODE_FONT);

            add(m_figureLabel);
            setOpaque(false);
            setIcon(RED, RED_GHOSTLY);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        void setIcon(final Image icon) {
            throw new IllegalStateException("This method should not be called.");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Dimension getPreferredSize(final int wHint, final int hHint) {
            return getStatusBarDimension();
        }
    }


    /**
     * Subfigure, contains the warning error signs.
     */
    private class InfoWarnErrorFigure extends GhostlySupportingFigure {
        /**
         * Creates a new bottom figure.
         */
        public InfoWarnErrorFigure() {
            final ToolbarLayout layout = new ToolbarLayout(false);
            layout.setMinorAlignment(OrderedLayout.ALIGN_CENTER);
            layout.setStretchMinorAxis(true);
            setLayoutManager(layout);
            m_figureLabel = new Label();

            add(m_figureLabel);
            setOpaque(false);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        void setIcon(final Image icon) {
            throw new IllegalStateException("This method should not be called.");
        }

        /**
         * Sets tool tip message.
         *
         * @param message The status message for the tool tip
         */
        private void setToolTip(final NodeMessage message, final int type) {
            m_figureLabel.setToolTip(new WarnErrorToolTip(message, type));
            revalidate();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Dimension getPreferredSize(final int wHint, final int hHint) {
            return super.getPreferredSize(NodeContainerFigure.this
                    .getSymbolFigure().getPreferredSize().width, m_figureLabel
                    .getPreferredSize().height);
        }
    }


    private static class ModifiablePortMouseTracker implements MouseListener, MouseMotionListener {
        private static final Image PORT_ICON =
            ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/node/add_port_flat.png");
        private static final Image TRANSPARENT_ICON =
                ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/node/add_port_flat_transparent.png");

        private static void fillMenu(final NodeContainerEditPart nodeContainerEditPart, final Menu popUpMenu) {
            final PortActionCreator portCreator = new PortActionCreator(nodeContainerEditPart);
            if (portCreator.hasActions()) {
                addActions(popUpMenu, portCreator.getAddPortActions(), "Add ports");
                addActions(popUpMenu, portCreator.getRemovePortActions(), "Remove ports");
                addActions(popUpMenu, portCreator.getExchangePortActions(), "Exchange ports");
            } else if (portCreator.getNotSupportedAction().isPresent()) {
                addActions(popUpMenu, Arrays.asList(portCreator.getNotSupportedAction().get()), "");
            }
        }

        private static void addActions(final Menu popupMenu, final List<? extends Action> actions,
            final String menuEntryName) {
            if (!actions.isEmpty()) {
                if (actions.size() == 1) {
                    createMenuItem(popupMenu, actions.get(0));
                } else {
                    final MenuItem mi = new MenuItem(popupMenu, SWT.CASCADE);
                    mi.setText(menuEntryName);
                    final Menu subMenuManager = new Menu(popupMenu);
                    mi.setMenu(subMenuManager);
                    actions.stream().forEach(action -> createMenuItem(subMenuManager, action));
                }
            }
        }

        private static void createMenuItem(final Menu popupMenu, final Action action) {
            final MenuItem mi = new MenuItem(popupMenu, SWT.PUSH);
            mi.setText(action.getText());
            mi.setEnabled(action.isEnabled());
            mi.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent se) {
                    popupMenu.setVisible(false);
                    action.run();
                }
            });
        }

        // force the platform shell to become active again (even though it appears to be visually
        //      already) so that it can continue getting mouse events correctly
        private static void reactivatePlatformShell() {
            final Runnable r = () -> {
                try {
                    Thread.sleep(50);
                } catch (Exception e) { }

                PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
                    final Shell s = SWTUtilities.getKNIMEWorkbenchShell();
                    s.forceActive();
                    s.forceFocus();
                });
            };

            KNIMEConstants.GLOBAL_THREAD_POOL.enqueue(r);
        }


        private final NodeContainerEditPart m_nodeContainerEditPart;
        private final SymbolFigure m_symbolFigure;

        private ModifiablePortMouseTracker(final NodeContainerEditPart ncep, final SymbolFigure figure) {
            m_nodeContainerEditPart = ncep;
            m_symbolFigure = figure;
        }

        private void updateIcon(final boolean transparent) {
            final Label l = m_symbolFigure.getModifiablePortLabel();

            l.setIcon(transparent ? TRANSPARENT_ICON : PORT_ICON);
            l.repaint();
        }

        @Override
        public void mousePressed(final MouseEvent me) {
            // only if the left mouse button is clicked
            if (me.button == 1) {
                final Menu m = new Menu(SWTUtilities.getKNIMEWorkbenchShell());
                fillMenu(m_nodeContainerEditPart, m);
                m.addMenuListener(new MenuListener() {
                    @Override
                    public void menuHidden(final MenuEvent e) {
                        reactivatePlatformShell();

                        updateIcon(true);
                    }

                    @Override
                    public void menuShown(final MenuEvent e) { }
                });
                m.setVisible(true);
            }
        }

        @Override
        public void mouseReleased(final MouseEvent me) { }

        @Override
        public void mouseDoubleClicked(final MouseEvent me) { }

        @Override
        public void mouseDragged(final MouseEvent me) { }

        @Override
        public void mouseEntered(final MouseEvent me) {
            updateIcon(false);
        }

        @Override
        public void mouseExited(final MouseEvent me) {
            updateIcon(true);
        }

        @Override
        public void mouseHover(final MouseEvent me) { }

        @Override
        public void mouseMoved(final MouseEvent me) { }
    }


    /**
     * Marks this node parts figure as slated for delete. Used to hilite it from the rest of the parts.
     *
     * @see NodeContainerFigure#unmarkForDelete()
     */
    public void markForDelete() {
        m_symbolFigure.m_backgroundIcon.add(m_symbolFigure.m_deleteIcon);
        m_symbolFigure.m_backgroundIcon.setConstraint(
                m_symbolFigure.m_deleteIcon, new RelativeLocator(
                        m_symbolFigure.m_backgroundIcon, 0.5, 0.5));
    }

    /**
     * Resets the delete-marked figure.
     *
     * @see NodeContainerFigure#markForDelete()
     */
    public void unmarkForDelete() {
        m_symbolFigure.m_backgroundIcon.remove(m_symbolFigure.m_deleteIcon);
    }

    /**
     * Marks this node parts figure as slated for replacement. Used to hilite it from the rest of the parts.
     *
     * @see NodeContainerFigure#unmarkForReplacement()
     */
    public void markForReplacement() {
        m_symbolFigure.m_backgroundIcon.add(m_symbolFigure.m_replaceIcon);
        m_symbolFigure.m_backgroundIcon.setConstraint(
                m_symbolFigure.m_replaceIcon, new RelativeLocator(
                        m_symbolFigure.m_backgroundIcon, 0.5, 0.5));
    }

    /**
     * Resets the replacement-marked figure.
     *
     * @see NodeContainerFigure#markForReplacement()
     */
    public void unmarkForReplacement() {
        m_symbolFigure.m_backgroundIcon.remove(m_symbolFigure.m_replaceIcon);
    }

    /**
     * Set a new font size which is applied to the node name and label.
     *
     * @param fontSize the new font size to ba applied.
     */
    public void setFontSize(final int fontSize) {
        setLabelText(m_label);

        // apply new font for node label
        Font font2 = super.getFont();
        FontData fontData2 = font2.getFontData()[0];
        Font newFont2 = FontStore.INSTANCE.getFont(fontData2.getName(), fontSize, fontData2.getStyle());
        // apply the standard node label font also to its parent figure to allow
        // editing the node label with the same font (size)
        super.setFont(newFont2);
        FontStore.INSTANCE.releaseFont(font2);
    }

    /**
     * Set the hide flag to hide/show the node name.
     *
     * @param hide true, if the node name is visible, otherwise false
     */
    public void hideNodeName(final boolean hide) {
        m_headingContainer.setVisible(!hide);
    }

    /**
     * Set the image indicating the loop status.
     *
     * @param loopStatus loop status of the loop end node
     * @param isExecuted is true when node is executed
     */
    private void setLoopStatus(final LoopStatus loopStatus, final boolean isExecuted) {
        if (loopStatus.equals(LoopStatus.NONE)) {
            m_loopStatusFigure = null;
            m_loopStatusGhostlyFigure = null;
        } else if (loopStatus.equals(LoopStatus.RUNNING) || loopStatus.equals(LoopStatus.PAUSED)) {
            m_loopStatusFigure = LOOP_IN_PROGRESS_SIGN;
            m_loopStatusGhostlyFigure = LOOP_IN_PROGRESS_SIGN_GHOSTLY;
        } else {
            assert loopStatus.equals(LoopStatus.FINISHED);
            if (isExecuted) {
                m_loopStatusFigure = LOOP_DONE_SIGN;
                m_loopStatusGhostlyFigure = LOOP_DONE_SIGN_GHOSTLY;
            } else {
                m_loopStatusFigure = LOOP_NO_STATUS;
                m_loopStatusGhostlyFigure = LOOP_NO_STATUS_GHOSTLY;
            }
        }
    }

    /**
     * New bounds describe the boundaries of figure with x/y at the top left
     * corner. Since 2.3.0 the UI info stores the boundaries with x/y relative
     * to the icon symbol. Width and height in the UI info is in both cases the
     * width and height of the figure. <br>
     * This method returns x/y offsets - basically the current distance of the
     * top left corner to the top left corner of the symbol (with the current
     * font size etc.).
     * @param uiInfo underlying node's ui information holding the bounds
     * @return the offset of the reference point in the figure to the figure's
     *         symbol (values could be negative, if bounds are very small).
     * @since KNIME 2.3.0
     */
    public Point getOffsetToRefPoint(final NodeUIInformation uiInfo) {
        final int yDiff = m_headingContainer.getPreferredSize().height
                + NodeContainerLocator.GAP * 2;
        final Rectangle t = this.getBounds();
        int thiswidth = uiInfo.getBounds()[2];
        if (thiswidth <= 0) {
            thiswidth = t.width;
            if (thiswidth <= 0) {
                // figure with not set yet
                thiswidth = getPreferredSize().width;
            }
        }
        final int xDiff = (thiswidth - m_symbolFigure.getPreferredSize().width) / 2;

        return new Point(xDiff, yDiff);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")  // generic casting...
    @Override
    public void workflowEditorModeWasSet(final WorkflowEditorMode newMode) {
        m_currentEditorMode = newMode;

        m_symbolFigure.updateFigure();
        m_statusFigure.updateFigure();
        m_infoWarnErrorPanel.updateFigures();

        final Color c =
            WorkflowEditorMode.NODE_EDIT.equals(newMode) ? HEADING_CONTAINER_FOREGROUND : ColorConstants.lightGray;
        for (IFigure child : (List<IFigure>)m_headingContainer.getChildren()) {
            if (child instanceof Label) {
                child.setForegroundColor(c);
            }
        }

        repaint();
    }
}
