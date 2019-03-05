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
 *   Jan 30, 2019 (loki): created
 */
package org.knime.workbench.outportview;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * This class has been copied over over from the 9129 branch - once 9129 gets merged down, we should move the
 *  flat buttons to a more common location.
 *
 * @author loki der quaeler
 */
public class FlatButton extends Canvas {
    /**
     * One of the myriad of SWT shortcomings finds me writing this interface.
     */
    public interface ClickListener {
        /**
         * The implementor will be invoked by this method after a click has been received and the receiving instance has
         * set its selection state.
         *
         * @param source the instance which received the click
         */
        void clickOccurred(final FlatButton source);

        /**
         * The implementor may choose to implement this in order to prevent the button instance from processing the
         * click as a selection state change; if the flat button is not stateful, this method will not be called.
         *
         * @param source the instance which received the click
         * @return true if the click should be vetoed (ie. no selection state change will occur.) If the click is
         *         vetoed, the <code>clickOccurred(AbstractFlatButton)</code> method will not be invoked as part of this
         *         event cycle.
         */
        default boolean clickShouldBeVetoed(final FlatButton source) {
            return false;
        }
    }

    private static Color MOUSE_DOWN_OVERLAY_COLOR = null;
    private static final int MOUSE_DOWN_OVERLAY_ALPHA = 255;
    private static Color MOUSE_OVER_OVERLAY_COLOR = null;
    private static Color MOUSE_OVER_BORDER_COLOR = null;
    private static final int MOUSE_OVER_OVERLAY_ALPHA = 255;
    private static Color SELECTED_BACKGROUND_COLOR = null;
    private static Color SELECTED_BORDER_COLOR = null;

    // Frameworks candidate... oh SWT, why are you so insufficient.. seriously - an obvious use case ignored
    //      This is in ColorDropDown on the 9129 branch.
    static Color createColorFromHexString(final Display display, final String hexString) {
        java.awt.Color c = java.awt.Color.decode(hexString);

        return new Color(display, c.getRed(), c.getGreen(), c.getBlue());
    }

    private static synchronized void initializeColorsIfNecessary() {
        if (MOUSE_OVER_OVERLAY_COLOR == null) {
            final Display d = Display.getDefault();

            MOUSE_OVER_OVERLAY_COLOR = createColorFromHexString(d, "#D8E6F2");
            MOUSE_OVER_BORDER_COLOR = createColorFromHexString(d, "#C0DCF3");
            MOUSE_DOWN_OVERLAY_COLOR = MOUSE_OVER_BORDER_COLOR;
            SELECTED_BACKGROUND_COLOR = MOUSE_DOWN_OVERLAY_COLOR;
            SELECTED_BORDER_COLOR = createColorFromHexString(d, "#90C8F6");
        }
    }


    private final boolean m_buttonIsStateful;
    private final AtomicBoolean m_state;

    private final Image m_defaultImage;
    private final Image m_selectedImage;
    private final Point m_imageDrawLocation;

    private final HashSet<ClickListener> m_listeners;

    private final UIPresenter m_uiPresenter;

    private PaintListener m_postRenderer;

    private final AtomicBoolean m_avoidSelectionAndMouseRendering;

    /**
     * This version of the constructor should be used for buttons which only do dynamic rendering, having no backing image.
     *
     * @param parent the parent widget which owns this instance; if the layout of this widget is <code>GridLayout</code>
     *            then size appropriate <code>GridData</code> will be set.
     * @param style <code>SWT.PUSH</code> or <code>SWT.TOGGLE</code> are expected.
     * @param postRenderer the renderer responsible for painting content into this button
     * @param size the size of the button
     * @throws IllegalArgumentException if SWT.RADIO is the passed <code>style</code>.
     */
    public FlatButton(final Composite parent, final int style, final PaintListener postRenderer, final Point size) {
        this(parent, style, null, null, 0, true, size, false);

        setPostRenderer(postRenderer);
    }

    /**
     * This version of the constructor should be used for buttons which display only one image. Should you be
     * constructing a toggle button, this class will render a selection indication under the icon; if you don't want
     * this behavior, use the two image constructor.
     *
     * @param parent the parent widget which owns this instance; if the layout of this widget is <code>GridLayout</code>
     *            then size appropriate <code>GridData</code> will be set.
     * @param style <code>SWT.PUSH</code> or <code>SWT.TOGGLE</code> are expected.
     * @param buttonImage the image which this button will display.
     * @param size the size of the button; if this is null, then the image size will be taken from
     *            <code>buttonImage</code>
     * @throws IllegalArgumentException if SWT.RADIO is the passed <code>style</code> or <code>buttonImage</code> in
     *             null.
     */
    public FlatButton(final Composite parent, final int style, final Image buttonImage, final Point size) {
        this(parent, style, buttonImage, null, 0, size);
    }

    /**
     * This version of the constructor should be used for buttons which display only one image;. Should you be
     * constructing a toggle button, this class will render a selection indication under the icon; if you don't want
     * this behavior, use the two image constructor.
     *
     * @param parent the parent widget which owns this instance; if the layout of this widget is <code>GridLayout</code>
     *            then size appropriate <code>GridData</code> will be set.
     * @param style <code>SWT.PUSH</code> or <code>SWT.TOGGLE</code> are expected.
     * @param buttonImage the image which this button will display.
     * @param indent the number of pixels to put on the left margin of the button
     * @param size the size of the button; if this is null, then the image size will be taken from
     *            <code>buttonImage</code>
     * @throws IllegalArgumentException if SWT.RADIO is the passed <code>style</code> or <code>buttonImage</code> in
     *             null.
     */
    public FlatButton(final Composite parent, final int style, final Image buttonImage, final int indent,
        final Point size) {
        this(parent, style, buttonImage, null, indent, size);
    }

    /**
     * This version of the constructor should be used for buttons which display only one image. Should you be
     * constructing a toggle button, this class will render a selection indication under the icon; if you don't want
     * this behavior, use the two image constructor.
     *
     * @param parent the parent widget which owns this instance; if the layout of this widget is <code>GridLayout</code>
     *            then size appropriate <code>GridData</code> will be set.
     * @param style <code>SWT.PUSH</code> or <code>SWT.TOGGLE</code> are expected.
     * @param buttonImage the image which this button will display.
     * @param size the size of the button; if this is null, then the image size will be taken from
     *            <code>buttonImage</code>
     * @param verticalCenter if true, the image will be centered vertically within the button bounds; this is
     *            meaningless if <code>size</code> is null.
     * @throws IllegalArgumentException if SWT.RADIO is the passed <code>style</code> or <code>buttonImage</code> in
     *             null.
     */
    public FlatButton(final Composite parent, final int style, final Image buttonImage, final Point size,
        final boolean verticalCenter) {
        this(parent, style, buttonImage, null, 0, false, size, verticalCenter);
    }

    /**
     * This version of the constructor should be used for buttons which are stateful (toggle) and want to have a
     * specific image rendered in the selected state.
     *
     * @param parent the parent widget which owns this instance; if the layout of this widget is <code>GridLayout</code>
     *            then size appropriate <code>GridData</code> will be set.
     * @param style <code>SWT.PUSH</code> or <code>SWT.TOGGLE</code> are expected.
     * @param buttonImage the image which this button will display.
     * @param altImage the image which this button will display in its selected state; this is expected to be of the
     *            same dimensions as <code>buttonImage</code>, if it is not null.
     * @param size the size of the button; if this is null, then the image size will be taken from
     *            <code>buttonImage</code>
     * @param verticalCenter if true, the image will be centered vertically within the button bounds; this is
     *            meaningless if <code>size</code> is null.
     * @throws IllegalArgumentException if SWT.RADIO is the passed <code>style</code> or <code>buttonImage</code> in
     *             null.
     */
    public FlatButton(final Composite parent, final int style, final Image buttonImage, final Image altImage,
        final Point size, final boolean verticalCenter) {
        this(parent, style, buttonImage, altImage, 0, false, size, verticalCenter);
    }

    /**
     * This version of the constructor should be used for buttons which are stateful (toggle) and want to have a
     * specific image rendered in the selected state.
     *
     * @param parent the parent widget which owns this instance; if the layout of this widget is <code>GridLayout</code>
     *            then size appropriate <code>GridData</code> will be set.
     * @param style <code>SWT.PUSH</code> or <code>SWT.TOGGLE</code> are expected.
     * @param buttonImage the image which this button will display.
     * @param altImage the image which this button will display in its selected state; this is expected to be of the
     *            same dimensions as <code>buttonImage</code>, if it is not null.
     * @param size the size of the button; if this is null, then the image size will be taken from
     *            <code>buttonImage</code>
     * @throws IllegalArgumentException if SWT.RADIO is the passed <code>style</code> or <code>buttonImage</code> in
     *             null.
     */
    public FlatButton(final Composite parent, final int style, final Image buttonImage, final Image altImage,
        final Point size) {
        this(parent, style, buttonImage, altImage, 0, size);
    }

    /**
     * This version of the constructor should be used for buttons which are stateful (toggle) and want to have a
     * specific image rendered in the selected state.
     *
     * @param parent the parent widget which owns this instance; if the layout of this widget is <code>GridLayout</code>
     *            then size appropriate <code>GridData</code> will be set.
     * @param style <code>SWT.PUSH</code> or <code>SWT.TOGGLE</code> are expected.
     * @param buttonImage the image which this button will display.
     * @param altImage the image which this button will display in its selected state; this is expected to be of the
     *            same dimensions as <code>buttonImage</code>, if it is not null.
     * @param indent the number of pixels to put on the left margin of the button
     * @param size the size of the button; if this is null, then the image size will be taken from
     *            <code>buttonImage</code>
     * @throws IllegalArgumentException if SWT.RADIO is the passed <code>style</code> or <code>buttonImage</code> in
     *             null.
     */
    public FlatButton(final Composite parent, final int style, final Image buttonImage, final Image altImage,
        final int indent, final Point size) {
        this(parent, style, buttonImage, altImage, indent, false, size, false);
    }

    /**
     *
     * @param parent the parent widget which owns this instance; if the layout of this widget is <code>GridLayout</code>
     *            then size appropriate <code>GridData</code> will be set.
     * @param style <code>SWT.PUSH</code> or <code>SWT.TOGGLE</code> are expected.
     * @param buttonImage the image which this button will display.
     * @param altImage the image which this button will display in its selected state; this is expected to be of the
     *            same dimensions as <code>buttonImage</code>, if it is not null.
     * @param indent the number of pixels to put on the left margin of the button
     * @param nullImageAcceptable false if we should check for a null image as an error case
     * @param size the size of the button; if this is null, then the image size will be taken from
     *            <code>buttonImage</code>
     * @param verticalCenter whether the image should be centered vertically
     */
    protected FlatButton(final Composite parent, final int style, final Image buttonImage, final Image altImage,
        final int indent, final boolean nullImageAcceptable, final Point size, final boolean verticalCenter) {
        super(parent, SWT.NO_REDRAW_RESIZE);

        if ((style & SWT.RADIO) != 0) {
            throw new IllegalArgumentException("Radio groups should be handled via FlatButtonRadioGroup.");
        }

        if ((buttonImage == null) && !nullImageAcceptable) {
            throw new IllegalArgumentException("buttonImage cannot be null.");
        }

        m_buttonIsStateful = ((style & SWT.TOGGLE) != 0);
        m_state = new AtomicBoolean(false);

        initializeColorsIfNecessary();

        m_defaultImage = buttonImage;
        m_selectedImage = (altImage != null) ? altImage : buttonImage;

        if (size != null) {
            setSize(size.x, size.y);
            if (parent.getLayout() instanceof GridLayout) {
                final GridData gd = new GridData(size.x, size.y);

                setLayoutData(gd);
            }
        }

        if (buttonImage != null) {
            final ImageData id = buttonImage.getImageData();

            if (size == null) {
                setSize(id.width, id.height);
                if (parent.getLayout() instanceof GridLayout) {
                    final GridData gd = new GridData(id.width, id.height);

                    gd.horizontalIndent = indent;

                    setLayoutData(gd);
                }

                m_imageDrawLocation = new Point(0, 0);
            } else {
                if (size.x > id.width) {
                    m_imageDrawLocation = calculateImageDrawLocation(size, id, verticalCenter);
                } else {
                    m_imageDrawLocation = new Point(0, 0);
                }
            }
        } else {
            m_imageDrawLocation = null;
        }

        m_avoidSelectionAndMouseRendering = new AtomicBoolean(false);

        m_listeners = new HashSet<>();

        m_uiPresenter = new UIPresenter();
        addMouseListener(m_uiPresenter);
        addMouseTrackListener(m_uiPresenter);
        addPaintListener(m_uiPresenter);
    }

    /**
     * @param listener an implementor of ClickListener which wishes to hear about clicks on this instance
     */
    public void addClickListener(final ClickListener listener) {
        synchronized(m_listeners) {
            m_listeners.add(listener);
        }
    }

    /**
     * @param listener an implementor of ClickListener which wishes to no longer hear about clicks on this instance
     */
    public void removeClickListener(final ClickListener listener) {
        synchronized(m_listeners) {
            m_listeners.remove(listener);
        }
    }

    /**
     * This allows a renderer to be called as a final render step after the button has been painted but before it has
     * had any mouse state styling indicators applied to it.
     *
     * @param renderer a renderer which will replace an existing post-renderer; this can be null.
     */
    public void setPostRenderer(final PaintListener renderer) {
        m_postRenderer = renderer;
    }

    /**
     * Set whether the button should render a selected state as well as mouse over hints. This is false by default.
     *
     * @param flag whether or not to render selected state and mouse over hints.
     */
    public void setAvoidSelectionAndMouseRendering(final boolean flag) {
        m_avoidSelectionAndMouseRendering.set(flag);
    }

    /**
     * @return whether this button is selected
     */
    public boolean isSelected() {
        return m_state.get();
    }

    /**
     * This method will set the selected state on the button, triggering a repaint if it has changed. This method will
     * do nothing if the instance was not constructed with a style of <code>SWT.TOGGLE</code>
     *
     * This method is thread safe and need not be called on the SWT thread.
     *
     * @param selected the state to set.
     */
    public void setSelected(final boolean selected) {
        synchronized (m_state) {
            if (m_buttonIsStateful && (selected != m_state.get())) {
                m_state.set(selected);

                getDisplay().asyncExec(() -> {
                    redraw();
                });
            }
        }
    }

    /**
     * Subclasses can override this but should be sure to call super prior to their method exit.
     *
     * @param pe the <code>PaintEvent</code> instance given to the paint listener responsible for this paint cycle.
     */
    protected void handlePaintingPostProcess(final PaintEvent pe) {
        if (m_postRenderer != null) {
            m_postRenderer.paintControl(pe);
        }
    }

    /**
     * If the size of the button, and the default image are specified to the constructor and the size's width is greater
     * than the button's width, this method is called to calculate the image's draw location.
     *
     * Subclasses can override this change the default behaviour which is to center the image horizontally, and
     * vertically depending on so-named the parameter's value.
     *
     * @param buttonSize the intended size of the button
     * @param imageData the <code>ImageData</code> for the default image
     * @param verticalCenter whether the image should be centered vertically
     * @return the location (relative to the button) at which the button image should be drawn
     */
    protected Point calculateImageDrawLocation(final Point buttonSize, final ImageData imageData,
        final boolean verticalCenter) {
        final int y = verticalCenter ? Math.abs((buttonSize.y - imageData.height) / 2) : 0;

        return new Point(((buttonSize.x - imageData.width) / 2), y);
    }

    /**
     * Notify listeners of this button that a click has occurred. Subclasses should only call this if their click
     * workflow is outside of the standard button click workflow.
     *
     * @param exemptSelf if true, and this instance is registered as a listener of itself, it will not be self-notified
     */
    protected void messageListeners(final boolean exemptSelf) {
        synchronized(m_listeners) {
            m_listeners.forEach((listener) -> {
                if (!exemptSelf || (listener != this)) {
                    listener.clickOccurred(this);
                }
            });
        }
    }

    private void clickDidOccur() {
        boolean shouldMessageListeners = true;

        if (m_buttonIsStateful) {
            boolean veto = false;

            synchronized(m_listeners) {
                for (final ClickListener listener : m_listeners) {
                    if (listener.clickShouldBeVetoed(this)) {
                        veto = true;

                        break;
                    }
                }
            }

            if (!veto) {
                setSelected(!m_state.get());
            }
        }

        if (shouldMessageListeners) {
            messageListeners(false);
        }
    }


    private final static byte DRAW_DEFAULT = 1;
    private final static byte DRAW_MOUSE_OVER = 2;
    private final static byte DRAW_MOUSE_DOWN = 4;

    private class UIPresenter implements MouseListener, MouseTrackListener, PaintListener {
        private boolean m_inMouseDown = false;

        private byte m_drawState = DRAW_DEFAULT;

        /**
         * {@inheritDoc}
         */
        @Override
        public void paintControl(final PaintEvent pe) {
            final GC gc = pe.gc;
            final Point size = FlatButton.this.getSize();
            final Image i;

            gc.setAdvanced(true);

            if (m_state.get() && !m_avoidSelectionAndMouseRendering.get()) {
                gc.setBackground(SELECTED_BACKGROUND_COLOR);
                gc.setForeground(SELECTED_BORDER_COLOR);

                gc.fillRectangle(0, 0, size.x, size.y);
                gc.drawRectangle(0, 0, (size.x - 1), (size.y - 1));

                i = m_selectedImage;
            } else {
                i = m_defaultImage;
            }

            if (!m_avoidSelectionAndMouseRendering.get()) {
                Color fill = null;
                int alpha = 0;

                if ((m_drawState & DRAW_MOUSE_DOWN) == DRAW_MOUSE_DOWN) {
                    alpha = MOUSE_DOWN_OVERLAY_ALPHA;
                    fill = MOUSE_DOWN_OVERLAY_COLOR;
                } else if ((m_drawState & DRAW_MOUSE_OVER) == DRAW_MOUSE_OVER) {
                    alpha = MOUSE_OVER_OVERLAY_ALPHA;
                    fill = MOUSE_OVER_OVERLAY_COLOR;
                }

                if (fill != null) {
                    gc.setForeground(SELECTED_BORDER_COLOR);
                    gc.drawRectangle(0, 0, (size.x - 1), (size.y - 1));

                    gc.setBackground(fill);
                    gc.setAlpha(alpha);
                    gc.fillRectangle(0, 0, size.x, size.y);
                    gc.setAlpha(255);
                }
            }

            if (i != null) {
                gc.drawImage(i, m_imageDrawLocation.x, m_imageDrawLocation.y);
            }

            handlePaintingPostProcess(pe);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseEnter(final MouseEvent me) {
            alterDrawState(DRAW_MOUSE_OVER, true);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseExit(final MouseEvent me) {
            alterDrawState(DRAW_MOUSE_OVER, false);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseHover(final MouseEvent me) { }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseDoubleClick(final MouseEvent me) { }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseDown(final MouseEvent me) {
            m_inMouseDown = (me.button == 1);

            if (m_inMouseDown) {
                alterDrawState(DRAW_MOUSE_DOWN, true);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mouseUp(final MouseEvent me) {
            if (m_inMouseDown && (me.button == 1) && mouseEventWasInBounds(me)) {
                clickDidOccur();
            }

            m_inMouseDown = false;

            alterDrawState(DRAW_MOUSE_DOWN, false);
        }

        private boolean mouseEventWasInBounds(final MouseEvent me) {
            final Point size = FlatButton.this.getSize();

            return ((me.x >= 0) && (me.x <= size.x) && (me.y >= 0) && (me.y <= size.y));
        }

        private void alterDrawState(final byte mask, final boolean enable) {
            final int newState = enable ? (m_drawState | mask) : (m_drawState & ~mask);

            if (newState != m_drawState) {
                m_drawState = (byte)newState;

                FlatButton.this.redraw();
            }
        }
    }
}
