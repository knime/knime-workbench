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
 *   Sep 15, 2018 (loki): created
 */
package org.knime.workbench.editor2;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.core.LayoutExemptingLayout;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.editor2.directannotationedit.StyledTextEditor;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;
import org.knime.workbench.editor2.figures.WorkflowFigure;

/**
 * Our subclass of <code>ScrollingGraphicalViewer</code> which facilitates the pinning of info, warning and error
 * messages to the top of the viewport, regardless of where the user scrolls to on the canvas, or how the user resizes
 * the canvas' editor pane.
 *
 * @author loki der quaeler
 */
public class ViewportPinningGraphicalViewer extends ScrollingGraphicalViewer {
    /**
     * Consumers of the this viewer who want to add pinned messages can either use the constants defined in this inner
     * class for 'standard' message types, or create a new instance providing their own color, and potentially
     * iconographic, scheme.
     *
     * @see ViewportPinningGraphicalViewer#displayMessage(String, MessageAppearance)
     * @see ViewportPinningGraphicalViewer#displayMessage(String, MessageAppearance, String[], Runnable[])
     */
    public static final class MessageAppearance {
        /**
         * The standard message appearance for an "info" message.
         */
        public static final MessageAppearance INFO =
            new MessageAppearance(2, INFO_MESSAGE_BACKGROUND, SharedImages.Info, true);
        /**
         * The standard message appearance for an "warning" message.
         */
        public static final MessageAppearance WARNING =
            new MessageAppearance(1, WARN_ERROR_MESSAGE_BACKGROUND, SharedImages.Warning, true);
        /**
         * The standard message appearance for an "error" message.
         */
        public static final MessageAppearance ERROR =
            new MessageAppearance(0, WARN_ERROR_MESSAGE_BACKGROUND, SharedImages.Error, true);


        private int m_index;
        private final Color m_fillColor;
        private final SharedImages m_icon;
        private boolean m_internalConstant;

        /**
         * The available constructor for consumers who want to display a message with a non-predefined appearance.
         *
         * @param c the background color for the message. <b>NOTE:</b> this instance will be disposed when the
         *              message ceases to be displayed.
         * @param icon the icon to display, or null - it will not be disposed
         */
        public MessageAppearance(final Color c, final SharedImages icon) {
            assert(c != null);
            m_index = Integer.MIN_VALUE;
            m_fillColor = c;
            m_icon = icon;
            m_internalConstant = false;
        }

        private MessageAppearance(final int index, final Color c, final SharedImages icon, final boolean internal) {
            this(c, icon);
            m_index = index;
            m_internalConstant = internal;
        }

        /**
         * @return the fill color associated with this message type
         */
        public Color getFillColor() {
            return m_fillColor;
        }

        /**
         * @return the icon associated with this message type
         */
        public SharedImages getIcon() {
            return m_icon;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            final HashCodeBuilder hcb = new HashCodeBuilder();
            return hcb.append(m_fillColor).append(m_icon).append(m_index).append(m_internalConstant).toHashCode();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }

            final MessageAppearance other = (MessageAppearance)obj;
            final EqualsBuilder eb = new EqualsBuilder();
            return eb.append(m_fillColor, other.m_fillColor)
                     .append(m_icon, other.m_icon)
                     .append(m_index, other.m_index)
                     .append(m_internalConstant, other.m_internalConstant)
                     .isEquals();
        }

        /**
         * @return the internal ordering index for the message type; note that this index is a holdover from the original
         *              design in which message ordering was based on type (e.g info; warning; etc..) See the developer
         *              notes at the end of this class.
         */
        private int getIndex() {
            return m_index;
        }

        private boolean isInternalConstant() {
            return m_internalConstant;
        }
    }

    private static final int MESSAGE_BACKGROUND_OPACITY = 171;
    private static final Color WARN_ERROR_MESSAGE_BACKGROUND = new Color(null, 255, 249, 0, MESSAGE_BACKGROUND_OPACITY);
    private static final Color INFO_MESSAGE_BACKGROUND = new Color(null, 200, 200, 255, MESSAGE_BACKGROUND_OPACITY);

    private static NodeLogger LOGGER = NodeLogger.getLogger(ViewportPinningGraphicalViewer.class);

    /**
     * This is a static convenience method which involves fetching the active page's active editor, and then returning
     * the instance of this class attached to it.
     *
     * @return the glass pane <code>ViewportPinningGraphicalViewer</code> or null if we were unable to get an active
     *         page or an active editor for it.
     */
    public static ViewportPinningGraphicalViewer getActiveViewer() {
        final IWorkbenchWindow iw = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

        if (iw != null) {
            final IWorkbenchPage page = iw.getActivePage();

            if (page != null) {
                final WorkflowEditor we = (WorkflowEditor)page.getActiveEditor();

                if (we != null) {
                    return (ViewportPinningGraphicalViewer)we.getGraphicalViewer();
                }
            }
        }

        return null;
    }

    /**
     * This is a static convenience method which involves fetching the active page's active editor, and then getting the
     * instance of this class attached to it, and the SWT <code>Composite</code> parent into which it draws.
     *
     * This is useful for classes that want to draw into the glass pane that is the viewport. <b>NOTE:</b> that any SWT
     * widget created which has a parent as this composite should call
     * {@link LayoutExemptingLayout#exemptControlFromLayout(Control)}.
     *
     * @return the glass pane SWT <code>Composite</code> or null in conditions where null would be returned from
     *         {@link #getActiveViewer()}
     * @see LayoutExemptingLayout#exemptControlFromLayout(Control)
     * @see #getActiveViewer()
     */
    public static Composite getActiveViewportComposite() {
        ViewportPinningGraphicalViewer viewer = getActiveViewer();

        if (viewer != null) {
            return viewer.m_parent;
        }

        return null;
    }


    private final AtomicBoolean m_haveInitializedViewport = new AtomicBoolean(false);
    private final AtomicInteger m_currentMessageViewHeight = new AtomicInteger(0);
    private final AtomicLong m_messageKeyCounter = new AtomicLong(1);

    /* See the developer comment block at the bottom of this class concerning ordering. */
    private final ConcurrentHashMap<Long, Message> m_messageMap = new ConcurrentHashMap<>();
    private final TreeMap<Integer, ArrayList<Message>> m_typeOrderedMessageMap = new TreeMap<>();
    private final ArrayList<Message> m_orderedMessageList = new ArrayList<>();

    private Composite m_parent;

    /**
     * Displays a message pinned to the top of the viewport with the given appearance. If multiple messages are
     * displayed at the same time, their order of display is based on earliest added is the top most displayed.
     *
     * This method calls <code>displayMessage(message, appearance, null, null)</code>
     *
     * @param message the message text to be displayed
     * @param appearance an instance of {@link MessageAppearance}
     * @return a message 'key' that should be handed to the remove message method to remove the message from the
     *         viewport
     * @see #removeMessage(Long)
     */
    public Long displayMessage(final String message, final MessageAppearance appearance) {
        return displayMessage(message, appearance, null, null);
    }

    /**
     * Displays a message pinned to the top of the viewport with the given appearance, and displaying right-aligned flat
     * buttons with the specified titles, performing the specified actions dispatched on the SWT thread.
     *
     * If multiple messages are displayed at the same time, their order of display is based on earliest added is the top
     * most displayed.
     *
     * @param message the message text to be displayed
     * @param appearance an instance of {@link MessageAppearance}
     * @param buttonTitles if non-null and of cardinality 1 or more, flat buttons, right aligned within the message box
     *            will be displayed; the buttons will be displayed left-to-right (i.e the title in the 0th index will be
     *            the leftmost button.)
     * @param buttonActions a parallel array of {@link Runnable} instances which correspond 1-1 to the button titles; an
     *            action can be null, but <code>buttonTitles.length =must= buttonActions.length</code>
     * @return a message 'key' that should be handed to the remove message method to remove the message from the
     *         viewport
     * @see #removeMessage(Long)
     */
    public Long displayMessage(final String message, final MessageAppearance appearance, final String[] buttonTitles,
        final Runnable[] buttonActions) {
        final VeryLightFlatButton[] buttons;

        if ((buttonTitles != null) && (buttonTitles.length > 0)) {
            assert(buttonActions != null);
            assert(buttonActions.length == buttonTitles.length);

            final Display d = PlatformUI.getWorkbench().getDisplay();
            final Color c = appearance.getFillColor();

            buttons = new VeryLightFlatButton[buttonTitles.length];
            for (int i = 0; i < buttonTitles.length; i++) {
                buttons[i] = new VeryLightFlatButton(buttonTitles[i], buttonActions[i], d, c);
            }
        } else {
            buttons = null;
        }

        final Long key = Long.valueOf(m_messageKeyCounter.getAndIncrement());
        final Message m = new Message(m_parent, key, message, appearance, buttons);

        synchronized (m_typeOrderedMessageMap) {
            final Integer orderedKey = Integer.valueOf(appearance.getIndex());
            ArrayList<Message> messages = m_typeOrderedMessageMap.get(orderedKey);

            if (messages == null) {
                messages = new ArrayList<>();
                m_typeOrderedMessageMap.put(orderedKey, messages);
            }
            messages.add(m);

            m_messageMap.put(key, m);

            m_orderedMessageList.add(m);
        }

        performMessageLayout(true);

        return key;
    }

    /**
     * Removes a message from the display.
     *
     * @param messageKey the value returned by one of the display methods in this class
     * @see #displayMessage(String, MessageAppearance)
     * @see #displayMessage(String, MessageAppearance, String[], Runnable[])
     */
    public void removeMessage(final Long messageKey) {
        if (messageKey == null) {
            return;
        }

        final Message message;
        synchronized (m_typeOrderedMessageMap) {
            message = m_messageMap.remove(messageKey);

            if (message != null) {
                final Integer key = Integer.valueOf(message.getAppearance().getIndex());
                final ArrayList<Message> messages = m_typeOrderedMessageMap.get(key);
                if (messages != null) {
                    messages.remove(message);
                }

                m_orderedMessageList.remove(message);
            }
        }

        if (message != null) {
            message.dispose();
            performMessageLayout(true);
        }
    }

    /**
     * Removes all messages associated to a given appearance.
     *
     * @param appearance the appearance
     */
    public void removeMessagesOfAppearance(final MessageAppearance appearance) {
        synchronized (m_typeOrderedMessageMap) {
            final ArrayList<Message> messages = m_typeOrderedMessageMap.remove(Integer.valueOf(appearance.getIndex()));

            if (messages != null) {
                messages.stream().forEach((message) -> {
                    m_messageMap.remove(message.getMessageId());
                    m_orderedMessageList.remove(message);
                    message.dispose();
                });
            }
        }

        performMessageLayout(true);
    }

    /**
     * A less computationally / redraw intensive method to clear messages than calling {@link #removeMessage(Long)} N
     * times.
     */
    public void clearAllMessages() {
        Display.getDefault().asyncExec(() -> {
            final boolean shouldUpdateView;

            synchronized (m_typeOrderedMessageMap) {
                shouldUpdateView = (m_messageMap.size() > 0);

                m_messageMap.values().stream().forEach((message) -> {
                    message.dispose();
                });

                m_messageMap.clear();
                m_typeOrderedMessageMap.clear();
                m_orderedMessageList.clear();
            }

            m_currentMessageViewHeight.set(0);

            if (shouldUpdateView) {
                updateTopWhitespaceBuffer();

                repaint();
            }
        });
    }

    /**
     * @return the <code>WorkflowFigure</code> which is the root figure for this viewer
     */
    public WorkflowFigure getWorkflowFigure() {
        return ((WorkflowRootEditPart)getRootEditPart().getContents()).getFigure();
    }

    /**
     * @see org.eclipse.gef.EditPartViewer#setControl(Control)
     */
    @Override
    public void setControl(final Control control) {
        if (control != null) {
            m_parent = control.getParent();
            m_parent.setLayout(new LayoutExemptingLayout());

            if (control instanceof FigureCanvas) {
                final FigureCanvas fc = (FigureCanvas)control;
                ScrollBar sb = fc.getHorizontalBar();
                if (sb != null) {
                    sb.addListener(SWT.Selection, (event) -> {
                        StyledTextEditor.toolbarLocationShouldUpdate();
                    });
                }
                sb = fc.getVerticalBar();
                if (sb != null) {
                    sb.addListener(SWT.Selection, (event) -> {
                        StyledTextEditor.toolbarLocationShouldUpdate();
                    });
                }
            }
        }

        super.setControl(control);
    }

    private void repaint() {
        getFigureCanvas().redraw();
    }

    private void updateTopWhitespaceBuffer() {
        final int yOffset = m_currentMessageViewHeight.get();
        getWorkflowFigure().placeTentStakeToAllowForTopWhitespaceBuffer(yOffset);

        final FigureCanvas fc = getFigureCanvas();
        if (fc.getViewport().getViewLocation().y == 0) {
            // If the view is already sitting at the 0-height position, then scroll the view back to
            //      tent-stake so that the messages are not covering any of the canvas elements.
            // We asyncExec again to give *something* a pause, for invoking this immediately rarely seems to work :-/
            Display.getDefault().asyncExec(() -> {
                fc.scrollToY(-yOffset);
            });
        }
    }

    private void performMessageLayout(final boolean requireTopWhitespaceReplacement) {
        final Viewport v = getViewport();

        if (v != null) {
            final Rectangle bounds = v.getBounds();
            int yOffset = 0;

            synchronized (m_typeOrderedMessageMap) {
                /* for ordering by type, iterate over m_typeOrderedMessageMap.entrySet() */
                for (final Message message : m_orderedMessageList) {
                    message.setLocation(0, yOffset);
                    yOffset += message.calculateSpatialsAndReturnRequiredHeight(bounds.width);

                    if (!message.isVisible()) {
                        message.moveAbove(null);
                        message.setVisible(true);
                    }
                }
            }

            final boolean calculatedHeightChanged = (m_currentMessageViewHeight.getAndSet(yOffset) != yOffset);
            if (requireTopWhitespaceReplacement || calculatedHeightChanged) {
                updateTopWhitespaceBuffer();
            }
        } else {
            LOGGER.warn("Could not get viewport to layout messages.");
        }
    }

    private Viewport getViewport() {
        final FigureCanvas fc = getFigureCanvas();

        if (fc != null) {
            final Viewport v = fc.getViewport();

            if (v != null) {
                if (!m_haveInitializedViewport.getAndSet(true)) {
                    v.addFigureListener((figure) -> {
                        // this is invoked when the size of the viewport changes
                        performMessageLayout(false);
                    });
                }

                return fc.getViewport();
            }
        } else {
            LOGGER.error("Could not get viewer's figure canvas.");
        }

        return null;
    }


    /*
     * This is a flat button class removed entirely from the SWT class hierarchy; it's basically a mouse state
     *  container which knows how to paint itself.
     */
    private static class VeryLightFlatButton {
        private static final int HORIZONTAL_INSET = 15;
        private static final int VERTICAL_INSET = 3;
        private static final Color BORDER_COLOR = new Color(PlatformUI.getWorkbench().getDisplay(), 163, 163, 163);

        // TODO this belongs in ColorUtilities - but that's in knime-core which will trigger another refactor request
        //          to get all UI code out of knime-core which i'd rather get done in one fell swoop in its own
        //          ticket. So... at that point, consider moving this there.
        // @param darker should be [0.0, 1.0] where 1.0 is no darker, and 0.0 is 'black.'
        private static Color getDarkerColor(final Color originalColor, final Display display, final float darker) {
            final float[] hsb = java.awt.Color.RGBtoHSB(originalColor.getRed(), originalColor.getGreen(),
                originalColor.getBlue(), null);

            hsb[2] *= darker;

            final java.awt.Color awtDarker = java.awt.Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
            return new Color(display, awtDarker.getRed(), awtDarker.getGreen(), awtDarker.getBlue());
        }


        private final String m_buttonTitle;
        private final Runnable m_action;
        private final AtomicBoolean m_mouseInBounds;
        private final Point m_size;
        private Point m_location;
        private final Color m_fillColor;
        private final Color m_mouseOverColor;
        private final Color m_textColor;

        VeryLightFlatButton(final String title, final Runnable action, final Display display,
                final Color parentBackgroundColor) {
            m_buttonTitle = title;
            m_action = action;

            m_fillColor = getDarkerColor(parentBackgroundColor, display, 0.93f);
            m_mouseOverColor = getDarkerColor(parentBackgroundColor, display, 0.75f);
            m_textColor = display.getSystemColor(SWT.COLOR_BLACK);

            m_mouseInBounds = new AtomicBoolean(false);

            final GC gc = new GC(display);
            try {
                final Point textSize = gc.stringExtent(m_buttonTitle);
                final int width = textSize.x + (2 * HORIZONTAL_INSET) + 2;  // +2s for border
                final int height = textSize.y + (2 * VERTICAL_INSET) + 2;  // +2s for border

                m_size = new Point(width, height);
            } finally {
                gc.dispose();
            }
        }

        void dispose() {
            m_fillColor.dispose();
            m_mouseOverColor.dispose();
        }

        Point getSize() {
            return m_size;
        }

        Runnable getAction() {
            return m_action;
        }

        void setLocation(final Point location) {
            m_location = location;
        }

        /* returns true if the mouse was previously considered out of bounds  */
        boolean mouseInBounds() {
            return !m_mouseInBounds.getAndSet(true);
        }

        /* returns true if the mouse was previously considered in bounds  */
        boolean mouseOutOfBounds() {
            return m_mouseInBounds.getAndSet(false);
        }

        void paint(final GC gc) {
            gc.setBackground(m_mouseInBounds.get() ? m_mouseOverColor : m_fillColor);
            gc.fillRectangle(m_location.x, m_location.y, m_size.x, m_size.y);

            gc.setForeground(m_textColor);
            gc.drawText(m_buttonTitle, (m_location.x + HORIZONTAL_INSET), (m_location.y + VERTICAL_INSET));

            gc.setForeground(BORDER_COLOR);
            gc.drawRectangle(m_location.x, m_location.y, (m_size.x - 1), (m_size.y - 1));
        }
    }


    /**
     * The message "component" - note that we are continuing to use Draw2D's {@link Label} class to calculate required
     * component height, as well as render the text, as the 'messages' have always done. I could foresee at somepoint,
     * should messages become more varied, that we would want to move to a different text rendering system which does
     * not truncate (ellipsis-ize) the text.
     */
    private static class Message extends Canvas {
        private static final int HORIZONTAL_INSET = 10;
        private static final int VERTICAL_INSET = 9;
        private static final int INTER_BUTTON_WIDTH = 6;


        private final Long m_messageId;
        private final Label m_label;
        private final MessageAppearance m_messageAppearance;
        private final VeryLightFlatButton[] m_buttons;
        private final Rectangle[] m_buttonBounds;
        private final Dimension m_totalButtonSize;

        private Message(final Composite parent, final Long id, final String message, final MessageAppearance appearance,
            final VeryLightFlatButton[] buttons) {
            super(parent, SWT.NONE);

            m_messageId = id;

            m_messageAppearance = appearance;
            m_buttons = buttons;

            m_label = new Label(message);
            m_label.setOpaque(false);
            m_label.setIcon(ImageRepository.getUnscaledIconImage(m_messageAppearance.getIcon()));
            m_label.setLabelAlignment(PositionConstants.LEFT);
            if (m_messageAppearance.getIcon() != null) {
                m_label.setIconTextGap(6);
            }

            addPaintListener((event) -> {
                final GC gc = event.gc;

                gc.setAdvanced(true);
                gc.setAntialias(SWT.ON);
                gc.setTextAntialias(SWT.ON);

                final SWTGraphics g = new SWTGraphics(gc);
                try {
                    m_label.paint(g);

                    if (m_buttons != null) {
                        for (final VeryLightFlatButton button : m_buttons) {
                            button.paint(gc);
                        }
                    }
                } finally {
                    g.dispose();
                }
            });
            if (m_buttons != null) {
                m_buttonBounds = new Rectangle[m_buttons.length];

                int totalButtonWidth = 0;
                int buttonHeight = Integer.MIN_VALUE;
                for (int i = 0; i < m_buttons.length; i++) {
                    final VeryLightFlatButton button = m_buttons[i];
                    final Point buttonSize = button.getSize();
                    if (totalButtonWidth > 0) {
                        totalButtonWidth += INTER_BUTTON_WIDTH;
                    }
                    totalButtonWidth += buttonSize.x;
                    buttonHeight = Math.max(buttonHeight, buttonSize.y);
                }
                m_totalButtonSize = new Dimension(totalButtonWidth, buttonHeight);

                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseDown(final MouseEvent me) {
                        for (int i = 0; i < m_buttonBounds.length; i++) {
                            if (m_buttonBounds[i].contains(me.x, me.y)) {
                                // this is technically incorrect, but UX correct
                                m_buttons[i].mouseOutOfBounds();
                                redraw(m_buttonBounds[i].x, m_buttonBounds[i].y, m_buttonBounds[i].width,
                                    m_buttonBounds[i].height, false);

                                final Runnable r = m_buttons[i].getAction();
                                if (r != null) {
                                    getDisplay().asyncExec(r);
                                }

                                return;
                            }
                        }
                    }
                });
                addMouseMoveListener((event) -> {
                    for (int i = 0; i < m_buttonBounds.length; i++) {
                        final boolean repaint;
                        if (m_buttonBounds[i].contains(event.x, event.y)) {
                            repaint = m_buttons[i].mouseInBounds();
                        } else {
                            repaint = m_buttons[i].mouseOutOfBounds();
                        }

                        if (repaint) {
                            redraw(m_buttonBounds[i].x, m_buttonBounds[i].y, m_buttonBounds[i].width,
                                m_buttonBounds[i].height, false);
                        }
                    }
                });
            } else {
                m_buttonBounds = null;
                m_totalButtonSize = new Dimension(0, 0);
            }

            calculateSpatialsAndReturnRequiredHeight(2 * HORIZONTAL_INSET);

            setBackground(m_messageAppearance.getFillColor());

            setVisible(false);
            moveBelow(null);

            LayoutExemptingLayout.exemptControlFromLayout(this);
        }

        private int calculateSpatialsAndReturnRequiredHeight(final int width) {
            int height = 0;
            final int labelYLocation;
            final int labelWidth;
            final int labelHeight = m_label.getPreferredSize().height;

            if ((m_buttons != null) && (width > 0)) {
                final int totalButtonHeight = m_totalButtonSize.height;

                height = Math.max(totalButtonHeight, labelHeight);
                int buttonInset = VERTICAL_INSET;
                if (height == labelHeight) {
                    buttonInset += ((labelHeight - totalButtonHeight) / 2);
                    labelYLocation = VERTICAL_INSET;
                } else {
                    labelYLocation = VERTICAL_INSET + ((totalButtonHeight - labelHeight) / 2);
                }

                int currentX = width - (m_totalButtonSize.width + HORIZONTAL_INSET);
                labelWidth = currentX - HORIZONTAL_INSET;
                for (int i = 0; i < m_buttons.length; i++) {
                    final VeryLightFlatButton button = m_buttons[i];
                    final Point buttonSize = button.getSize();

                    button.setLocation(new Point(currentX, buttonInset));
                    m_buttonBounds[i] = new Rectangle(currentX, buttonInset, buttonSize.x, buttonSize.y);

                    currentX += buttonSize.x + INTER_BUTTON_WIDTH;
                }
            } else {
                labelYLocation = VERTICAL_INSET;
                labelWidth = width - (2 * HORIZONTAL_INSET);
                height = labelHeight;
            }

            m_label.setBounds(new Rectangle(HORIZONTAL_INSET, labelYLocation, labelWidth, labelHeight));

            height += (2 * VERTICAL_INSET);
            setSize(width, height);

            return height;
        }

        private Long getMessageId() {
            return m_messageId;
        }

        private MessageAppearance getAppearance() {
            return m_messageAppearance;
        }

        @Override
        public void dispose() {
            if (!m_messageAppearance.isInternalConstant()) {
                m_messageAppearance.getFillColor().dispose();
            }

            if (m_buttons != null) {
                for (final VeryLightFlatButton button : m_buttons) {
                    button.dispose();
                }
            }

            super.dispose();
        }
    }

    /*
     * A note on the code in this class involving the ordering of the displayed messages.
     *
     * Historically, viewport message ordering was done by message type, as such:
     *
     *  |   an INFO
     *  |   a WARNING
     *  V   an ERROR
     *
     * during the re-write done for AP-12516, which introduces both custom types and the ability to have more than one
     * message per type displayed at the same time, this ordering was changed to:
     *
     *  |   a custom type
     *  |   an ERROR
     *  |   a WARNING
     *  V   an INFO
     *
     * If multiple messages of the same appearance type were displayed, they would be shown top-to-bottom in order of
     * when they were requested to be displayed.
     *
     * After a group Slack chat with Martin and Johannes (and Jon by proxy) it was decided that message display
     * ordering should be temporally based - with the earliest addition will be at the top of the viewport.
     *
     * It's a relatively trivial weight and performance issue to maintain the parallel structures containing the newer
     * ordering, so i am preserving them should there be a desire in the future to return to type-based message display
     * ordering.
     */
}
