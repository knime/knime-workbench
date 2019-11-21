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
package org.knime.workbench.editor2.viewport;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.core.LayoutExemptingLayout;
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
    private static NodeLogger LOGGER = NodeLogger.getLogger(ViewportPinningGraphicalViewer.class);


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
        return displayMessage(message, appearance, false);
    }

    /**
     * Displays a message pinned to the top of the viewport with the given appearance. If multiple messages are
     * displayed at the same time, their order of display is based on earliest added is the top most displayed.
     *
     * This method calls <code>displayMessage(message, appearance, null, null)</code>
     *
     * @param message the message text to be displayed
     * @param appearance an instance of {@link MessageAppearance}
     * @param showMessageCloseButton if true, a close button will be displayed on the right side of the message allowing
     *            the user to dismiss the message from the viewport themselves
     * @return a message 'key' that should be handed to the remove message method to remove the message from the
     *         viewport
     * @see #removeMessage(Long)
     */
    public Long displayMessage(final String message, final MessageAppearance appearance,
        final boolean showMessageCloseButton) {
        return displayMessage(message, appearance, null, null, false, showMessageCloseButton);
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
        return displayMessage(message, appearance, buttonTitles, buttonActions, false, false);
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
     * @param buttonPressDismissesMessage if true, the message will be removed from the viewport whenever one of the
     *            buttons defined in {@code buttonTitles} is pressed. The button's corresponding action will be invoked
     *            after the message is removed.
     * @see #removeMessage(Long)
     */
    public Long displayMessage(final String message, final MessageAppearance appearance, final String[] buttonTitles,
        final Runnable[] buttonActions, final boolean buttonPressDismissesMessage) {
        return displayMessage(message, appearance, buttonTitles, buttonActions, buttonPressDismissesMessage, false);
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
     * @param buttonPressDismissesMessage if true, the message will be removed from the viewport whenever one of the
     *            buttons defined in {@code buttonTitles} is pressed. The button's corresponding action will be invoked
     *            after the message is removed.
     * @param showMessageCloseButton if true, a close button will be displayed on the right side of the message allowing
     *            the user to dismiss the message from the viewport themselves
     * @return a message 'key' that should be handed to the remove message method to remove the message from the
     *         viewport
     * @see #removeMessage(Long)
     */
    public Long displayMessage(final String message, final MessageAppearance appearance, final String[] buttonTitles,
        final Runnable[] buttonActions, final boolean buttonPressDismissesMessage,
        final boolean showMessageCloseButton) {
        final VeryLightFlatButton[] buttons;

        final Long key = Long.valueOf(m_messageKeyCounter.getAndIncrement());
        if ((buttonTitles != null) && (buttonTitles.length > 0)) {
            assert(buttonActions != null);
            assert(buttonActions.length == buttonTitles.length);

            final Display d = PlatformUI.getWorkbench().getDisplay();
            final Color c = appearance.getFillColor();

            buttons = new VeryLightFlatButton[buttonTitles.length + (showMessageCloseButton ? 1 : 0)];
            for (int i = 0; i < buttonTitles.length; i++) {
                final Runnable buttonAction = buttonActions[i];
                final Runnable action = buttonPressDismissesMessage
                                            ? () -> {
                                                removeMessage(key);
                                                buttonAction.run();
                                              }
                                            : buttonAction;
                buttons[i] = new VeryLightFlatButton(buttonTitles[i], action, d, c);
            }
            if (showMessageCloseButton) {
                final Runnable action = () -> {
                    removeMessage(key);
                };
                buttons[buttonTitles.length] = new CloseButton(action, d, c);
            }
        } else if (showMessageCloseButton) {
            final Display d = PlatformUI.getWorkbench().getDisplay();
            final Color c = appearance.getFillColor();
            final Runnable action = () -> {
                removeMessage(key);
            };

            buttons = new VeryLightFlatButton[1];
            buttons[0] = new CloseButton(action, d, c);
        } else {
            buttons = null;
        }

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
     * @param size the width and height, as size.x and size.y, of a rectangle to be centered in the viewport
     * @return a {@code Point} representing a location within the display (i.e absolute) which should be the location of
     *         the item in order to have it centered in the viewport
     */
    public Point locationForSizeCenteredInViewport(final Point size) {
        final Viewport v = getViewport();
        final Rectangle viewportBounds = v.getBounds();
        final int relativeX = (viewportBounds.width - size.x) / 2;
        final int relativeY = (viewportBounds.height - size.y) / 2;

        return getControl().getParent().toDisplay(relativeX, relativeY);
    }

    /**
     * @param bounds the rectangular region which is desired to be in the viewport
     */
    public void ensureBoundsAreInView(final Rectangle bounds) {
        final Viewport v = getViewport();
        final Rectangle viewportBounds = v.getClientArea();
        if (!viewportBounds.contains(bounds)) {
            // this math is assuming that the viewport bounds are wider and taller than the parameter bounds

            final int deltaX = midPointRectangle(bounds, true) - midPointRectangle(viewportBounds, true);
            final int deltaY = midPointRectangle(bounds, false) - midPointRectangle(viewportBounds, false);
            final int newX = viewportBounds.x + deltaX;
            final int newY = viewportBounds.y + deltaY;
            ((FigureCanvas)getControl()).scrollTo(newX, newY);
        }
    }

    private static int midPointRectangle(final Rectangle r, final boolean width) {
        if (width) {
            return r.x + (r.width / 2);
        }

        return r.y + (r.height / 2);
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
