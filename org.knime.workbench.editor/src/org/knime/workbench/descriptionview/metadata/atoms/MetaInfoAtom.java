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
 *   Apr 30, 2019 (loki): created
 */
package org.knime.workbench.descriptionview.metadata.atoms;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.transform.sax.TransformerHandler;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Drawable;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.workflow.metadata.MetadataXML;
import org.knime.workbench.descriptionview.metadata.AbstractMetaView;
import org.knime.workbench.descriptionview.metadata.PlatformSpecificUIisms;
import org.knime.workbench.ui.workflow.metadata.MetadataItemType;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Instances of this class represent a wrapped N-tuple of items representing a single 'atom' of workflow metainfo. This
 * comprises the model data, the widget to be used to display the read only variant of this data, the widget to used to
 * display the write (edit) variant of the data, and the form attribute value for the XML metadata element.
 *
 * @author loki der quaeler
 */
public abstract class MetaInfoAtom {
    /** The read-only text color. **/
    protected static final Color LINK_COLOR = new Color(PlatformUI.getWorkbench().getDisplay(), 255, 144, 0);
    /** Used in rendering a close 'icon' **/
    protected static final String N_ARY_TIMES = "\u2A09";
    /** Used to denote a click-able surface. **/
    protected static final Cursor HAND_CURSOR = new Cursor(PlatformUI.getWorkbench().getDisplay(), SWT.CURSOR_HAND);
    /** Cursor to return to. **/
    protected static final Cursor DEFAULT_CURSOR = new Cursor(PlatformUI.getWorkbench().getDisplay(), SWT.CURSOR_ARROW);
    /** Font Metrics calculations on some platforms, with some fonts, calculate incorrectly; this is our fudge factor. **/
    protected static final double FONT_METRICS_CORRECTION;

    static {
        final Optional<Object> o =
            PlatformSpecificUIisms.getDetail(PlatformSpecificUIisms.FONT_METRICS_CORRECTION_DETAIL);

        FONT_METRICS_CORRECTION = o.isPresent() ? ((Double)o.get()).doubleValue() : 1.0;
    }


    /**
     * Consumers who want to know when a metadata info atom has mutated should implement this.
     */
    public interface MutationListener {
        /**
         * Implementors will be notified via this when an atom has been deleted.
         *
         * @param deletedAtom the atom which has been deleted
         */
        void metaInfoAtomDeleted(final MetaInfoAtom deletedAtom);

        /**
         * Implementors will be notified via this when an atom in edit, which was dirty, changed to clean; this will
         *  not be invoked if the atom became clean because {@link MetaInfoAtom#restoreState()} was invoked.
         *
         * @param cleanAtom the atom which became clean
         */
        void metaInfoAtomBecameClean(final MetaInfoAtom cleanAtom);

        /**
         * Implementors will be notified via this when an atom in edit, which was clean, became dirty.
         *
         * @param dirtyAtom the atom which has became dirty
         */
        void metaInfoAtomBecameDirty(final MetaInfoAtom dirtyAtom);
    }

    /**
     * Subclasses can use this in their custom SWT widgets.
     *
     * @param widget used in creating a temporary GC instance
     * @param text the text whose bounds we're calculating
     * @param horizontalInset the horizontal inset value
     * @param verticalInset the vertical inset value
     * @param renderNAry whether we should calculate the n-ary bounds as well
     * @return an instance embodying the total size and potentially the bounds of the n-ary 'close' icon
     */
    private static RenderSizeData calculateRenderSize(final Drawable widget, final String text,
        final int horizontalInset, final int verticalInset, final boolean renderNAry) {
        final GC gc = new GC(widget);

        try {
            gc.setFont(AbstractMetaView.VALUE_DISPLAY_FONT);
            final Point tagTextSize = gc.textExtent(text);

            final Point nArySize;
            if (renderNAry) {
                gc.setFont(AbstractMetaView.BOLD_CONTENT_FONT);
                nArySize = gc.textExtent(N_ARY_TIMES);
                if (PlatformSpecificUIisms.OS_IS_MAC) {
                    nArySize.y = nArySize.x;    // it's an equi-sided X, but some platform fonts give it a bottom inset
                }
            } else {
                nArySize = new Point(0, 0);
            }

            final int totalWidth
                    = (int)(tagTextSize.x * FONT_METRICS_CORRECTION)
                            + (2 * horizontalInset)
                            + (renderNAry ? ((int)(nArySize.x * FONT_METRICS_CORRECTION) + horizontalInset) : 0);
            final int totalHeight = Math.max(tagTextSize.y, nArySize.y) + (2 * verticalInset);

            final Rectangle nAryBounds;
            if (renderNAry) {
                final int nAryX = totalWidth - horizontalInset - nArySize.x;
                final int nAryY = (totalHeight - nArySize.y) / 2;

                nAryBounds = new Rectangle(nAryX, nAryY, nArySize.x, nArySize.y);
            } else {
                nAryBounds = null;
            }

            return new RenderSizeData(new Point(totalWidth, totalHeight), nAryBounds);
        } finally {
            gc.dispose();
        }
    }


    /**
     * Subclasses can use this as mutable storage of the instance value, as they see fit.
     */
    protected String m_value;

    /**
     * Subclasses can use this as a store for edit time.
     *
     * @see #storeStateForEdit()
     */
    protected final Map<String, String> m_editMap;

    private final CopyOnWriteArrayList<MutationListener> m_listeners;
    private final MetadataItemType m_type;

    private final String m_label;
    private final boolean m_readOnly;

    /**
     * Subclasses must invoke this constructor passing the correct info type.
     *
     * @param type the type of this instance.
     * @param label the label displayed with the value of this atom in some UI widget.
     * @param value the displayed value of this atom.
     * @param readOnly this has never been observed, and we don't currently have a use case in which we allow the user
     *            to mark something as read-only, so consider this future-proofing.
     */
    protected MetaInfoAtom(final MetadataItemType type, final String label, final String value, final boolean readOnly) {
        m_listeners = new CopyOnWriteArrayList<>();

        m_type = type;
        m_label = label;
        m_value = value;
        m_readOnly = readOnly;

        m_editMap = new HashMap<>();
    }

    /**
     * @return the type of this instance
     */
    public final MetadataItemType getType() {
        return m_type;
    }

    /**
     * @return the label displayed with the value of this atom in some UI widget
     */
    public final String getLabel() {
        return m_label;
    }

    /**
     * Subclasses may override this.
     *
     * @return the value to be displayed.
     */
    public String getValue() {
        return m_value;
    }

    /**
     * Subclasses may override this.
     *
     * @return true if this instance has a non-empty value.
     */
    public boolean hasContent() {
        return ((m_value != null) && !m_value.isEmpty());
    }

    /**
     * @return true if this atom is read only; this has never been observed, and we don't currently have a use case in
     *         which we allow the user to mark something as read-only, so consider this future-proofing.
     */
    public final boolean isReadOnly() {
        return m_readOnly;
    }

    /**
     * Consumers can add themselves via this method.
     *
     * @param listener an implementor
     */
    public void addChangeListener(final MutationListener listener) {
        m_listeners.add(listener);
    }

    /**
     * Consumers which have added themselves via {@link #addChangeListener(MutationListener)} can remove themselves via
     * this method.
     *
     * @param listener an implementor
     */
    public void removeChangeListener(final MutationListener listener) {
        m_listeners.remove(listener);
    }

    /**
     * Subclasses should invoke this method when an applicable action has transpired.
     *
     * @param type the type of event to be messaged
     */
    protected final void messageListeners(final ListenerEventType type) {
        switch (type) {
            case DELETE:
                m_listeners.stream().forEach((listener) -> {
                    listener.metaInfoAtomDeleted(this);
                });

                break;
            case DIRTY:
                m_listeners.stream().forEach((listener) -> {
                    listener.metaInfoAtomBecameDirty(this);
                });

                break;
            case CLEAN:
                m_listeners.stream().forEach((listener) -> {
                    listener.metaInfoAtomBecameClean(this);
                });

                break;
        }
    }

    /**
     * Subclasses can use this to write the most common case of element storage from their save implementation;
     *  subclasses should check for whether they have content and not bother calling this if they do not.
     *
     * @param parentElement the parent element
     * @param form the value for the {@link MetadataXML#FORM} attribute
     * @throws SAXException
     * @see #save(TransformerHandler)
     */
    protected void save(final TransformerHandler parentElement, final String form)
        throws SAXException {
        save(parentElement, form, m_value);
    }

    /**
     * Subclasses can use this to write the most common case of element storage from their save implementation;
     *  subclasses should check for whether they have content and not bother calling this if they do not.
     *
     * @param parentElement the parent element
     * @param form the value for the {@link MetadataXML#FORM} attribute
     * @param atomValue the value which gets written as the element's text content
     * @throws SAXException
     * @see #save(TransformerHandler)
     */
    protected void save(final TransformerHandler parentElement, final String form, final String atomValue)
        throws SAXException {
        final AttributesImpl attributes = new AttributesImpl();
        attributes.addAttribute(null, null, MetadataXML.FORM, "CDATA", form);
        attributes.addAttribute(null, null, MetadataXML.READ_ONLY, "CDATA", Boolean.toString(isReadOnly()));
        attributes.addAttribute(null, null, MetadataXML.NAME, "CDATA", getLabel());

        // TODO including a type breaks server parsing of the metadata - 4.0.0
//        attributes.addAttribute(null, null, MetadataXML.TYPE, "CDATA", getType().getType());

        addAdditionalSaveTimeAttributes(attributes);
        parentElement.startElement(null, null, MetadataXML.ATOM_WRITE_ELEMENT, attributes);
        final char[] value = atomValue.toCharArray();
        parentElement.characters(value, 0, value.length);
        parentElement.endElement(null, null, MetadataXML.ATOM_WRITE_ELEMENT);
    }

    /**
     * Subclasses can override this if their serialization involves extra attributes.
     *
     * @param attributes the attributes object which will get serialized.
     */
    protected void addAdditionalSaveTimeAttributes(final AttributesImpl attributes) { }

    /**
     * Instances will have this invoked when the view is switching from read/display to editing state.
     */
    public abstract void storeStateForEdit();
    /**
     * Instances will have this invoked when the edit mode is exiting as a cancel.
     */
    public abstract void restoreState();
    /**
     * Instances will have this invoked when the edit mode is exiting as a keep / save.
     */
    public abstract void commitEdit();
    /**
     * @return true if the atom is currently dirty; this value will be meaningless if not in edit mode
     */
    public abstract boolean isDirty();

    /**
     * Instances will have this invoked when they should build the UI for display (read-only).
     *
     * @param parent
     */
    public abstract void populateContainerForDisplay(final Composite parent);
    /**
     * Instances will have this invoked when they should build the UI for editing.
     *
     * @param parent
     */
    public abstract void populateContainerForEdit(final Composite parent);

    /**
     * Instances will only receive this notification while in edit mode; subclasses should override this to request
     * focus for their main edit widget.
     */
    public abstract void focus();


    /**
     * Subclasses must implement this to serialize themselves and their value(s) to the element.
     *
     * @param parentElement
     * @throws SAXException
     */
    public abstract void save(final TransformerHandler parentElement)
            throws SAXException;


    /**
     * A wrapper class for render calculations.
     *
     * @see MetaInfoAtom#calculateRenderSize(Drawable, String, int, int, boolean)
     */
    private static class RenderSizeData {
        private Rectangle m_nAryBounds;
        private Point m_entireSize;

        /**
         * @param size
         * @param nAryBounds
         */
        protected RenderSizeData(final Point size, final Rectangle nAryBounds) {
            m_entireSize = size;
            m_nAryBounds = nAryBounds;
        }

        /**
         * @return the entire size, potentially incorporating an n-ary
         */
        protected Point getEntireSize() {
            return m_entireSize;
        }

        /**
         * @return the n-ary bounds or null
         */
        protected Rectangle getNAryBounds() {
            return m_nAryBounds;
        }
    }


    /**
     * This is a superclass for Label-like widgets which can display an n-ary operator close 'icon'.
     */
    protected abstract class CloseableLabel extends Canvas {

        /** Whether this label with render a close 'icon'. **/
        protected final boolean m_renderEdit;
        /** The n-ary bounds, if this instance was created 'for edit' **/
        protected Rectangle m_nAryBounds;

        /** Subclasses can use this in their override of {@link Control#computeSize(int, int, boolean)} **/
        protected Point m_calculatedSize;

        /** The horizontal inset which will be used. **/
        protected final int m_horizontalInset;
        /** The vertical inset which will be used. **/
        protected final int m_verticalInset;

        /** Ths may or may not be equal to the enclosing class' <code>m_value</code>. **/
        protected final String m_displayText;

        private boolean m_mouseDownOnNAry;
        private boolean m_inNAryBounds;

        /**
         * @param parent the SWT parent
         * @param forEdit whether this label will show an n-ary 'icon'
         * @param horizontalInset the horizontal inset
         * @param verticalInset the vertical inset
         * @param displayText if this is non-null, this will be used to calculate the rendering bounds; if this is null
         *            then the enclosing class' <code>m_value</code> will be used
         */
        protected CloseableLabel(final Composite parent, final boolean forEdit, final int horizontalInset,
            final int verticalInset, final String displayText) {
            super(parent, SWT.NONE);

            m_displayText = (displayText != null) ? displayText : m_value;

            m_renderEdit = forEdit;

            setFont(AbstractMetaView.VALUE_DISPLAY_FONT);
            setForeground(AbstractMetaView.TEXT_COLOR);

            m_horizontalInset = horizontalInset;
            m_verticalInset = verticalInset;

            calculateSize();

            if (forEdit) {
                addMouseListener(new MouseListener() {
                    @Override
                    public void mouseDoubleClick(final MouseEvent me) { }

                    @Override
                    public void mouseDown(final MouseEvent me) {
                        if (m_inNAryBounds) {
                            m_mouseDownOnNAry = true;
                            redraw();
                        }
                    }

                    @Override
                    public void mouseUp(final MouseEvent me) {
                        m_mouseDownOnNAry = false;
                        redraw();

                        if (m_inNAryBounds) {
                            dispose();

                            messageListeners(ListenerEventType.DELETE);
                        }
                    }
                });
                addMouseMoveListener((event) -> {
                    m_inNAryBounds = m_nAryBounds.contains(event.x, event.y);
                    if (m_inNAryBounds) {
                        setCursor(HAND_CURSOR);
                    } else {
                        mouseMovedOutsideNAryBounds(event);
                    }
                });
            }
        }

        /**
         * This constructor ends up using <code>m_value</code> as the display text.
         *
         * @param parent the SWT parent
         * @param forEdit whether this label will show an n-ary 'icon'
         * @param horizontalInset the horizontal inset
         * @param verticalInset the vertical inset
         */
        protected CloseableLabel(final Composite parent, final boolean forEdit, final int horizontalInset,
            final int verticalInset) {
            this(parent, forEdit, horizontalInset, verticalInset, null);
        }

        /**
         * Subclasses may override this to avoid the cursor being changed to default should they want dibs on
         *  setting the cursor (for example in the HyperLinkLabel.)
         *
         * @param me the mouse event which birthed this call
         */
        protected void mouseMovedOutsideNAryBounds(final MouseEvent me) {
            setCursor(DEFAULT_CURSOR);
        }

        /**
         * Subclasses should invoke this in their paint cycle to paint the n-ary 'icon'.
         *
         * @param gc the {@link GC} from the paint event
         */
        protected void paintNAry(final GC gc) {
            if (m_renderEdit) {
                if (m_mouseDownOnNAry) {
                    gc.setBackground(AbstractMetaView.TEXT_COLOR);
                    // y-1 is on purpose
                    gc.fillOval((m_nAryBounds.x - 2), (m_nAryBounds.y - 1), (m_nAryBounds.width + 4),
                        (m_nAryBounds.height + 4));
                    gc.setForeground(AbstractMetaView.GENERAL_FILL_COLOR);
                } else {
                    gc.setForeground(AbstractMetaView.TEXT_COLOR);
                }

                gc.setFont(AbstractMetaView.BOLD_CONTENT_FONT);
                gc.drawString(N_ARY_TIMES, m_nAryBounds.x, m_nAryBounds.y, true);
            }
        }

        // We intercept these to avoid our layout attempting to resize us.
        @Override
        public void setSize(final Point size) { }
        @Override
        public void setSize(final int width, final int height) { }
        @Override
        public void setBounds(final Rectangle rect) {
            setLocation(rect.x, rect.y);
        }
        @Override
        public void setBounds(final int x, final int y, final int width, final int height) {
            setLocation(x, y);
        }

        private void calculateSize() {
            final RenderSizeData rsd =
                calculateRenderSize(this, m_displayText, m_horizontalInset, m_verticalInset, m_renderEdit);

            m_nAryBounds = rsd.getNAryBounds();
            m_calculatedSize = rsd.getEntireSize();

            // This falls in the category of "can't hurt - does nothing"
            super.setSize(m_calculatedSize);
        }
    }


    /**
     * A label which displays as a hyperlink and has an accompanying action.
     */
    protected class HyperLinkLabel extends CloseableLabel {
        private static final int HORIZONTAL_INSET = 6;
        private static final int VERTICAL_INSET = 3;

        private final String m_url;

        private Point m_cachedComputedSize;

        /**
         * This uses the enclosing class' <code>m_value</code> as the display text.
         *
         * @param parent the SWT parent
         * @param forEdit whether this label will show an n-ary 'icon'
         * @param url the URL associated with this link display
         */
        protected HyperLinkLabel(final Composite parent, final boolean forEdit, final String url) {
            this(parent, forEdit, null, url);
        }

        /**
         * @param parent the SWT parent
         * @param forEdit whether this label will show an n-ary 'icon'
         * @param displayText the text to show
         * @param url the URL associated with this link display
         */
        protected HyperLinkLabel(final Composite parent, final boolean forEdit, final String displayText,
            final String url) {
            super(parent, forEdit, HORIZONTAL_INSET, VERTICAL_INSET, displayText);

            setForeground(LINK_COLOR);

            m_url = url;

            addPaintListener((paintEvent) -> {
                final GC gc = paintEvent.gc;
                final Rectangle r = getClientArea();

                gc.setAdvanced(true);
                gc.setAntialias(SWT.ON);
                gc.setTextAntialias(SWT.ON);
                gc.setFont(AbstractMetaView.VALUE_DISPLAY_FONT);

                gc.drawString(m_displayText, (r.x + HORIZONTAL_INSET), (r.y + VERTICAL_INSET));

                paintNAry(gc);
            });

            addMouseListener(new MouseListener() {
                @Override
                public void mouseDoubleClick(final MouseEvent me) { }

                @Override
                public void mouseDown(final MouseEvent me) {
                    if ((m_url != null) && ((m_nAryBounds == null) || !m_nAryBounds.contains(me.x, me.y))) {
                        Program.launch(m_url);
                    }
                }

                @Override
                public void mouseUp(final MouseEvent me) { }
            });
            addListener(SWT.MouseEnter, (event) -> {
                setCursor(HAND_CURSOR);
            });
            addListener(SWT.MouseExit, (event) -> {
                setCursor(DEFAULT_CURSOR);
            });

            m_cachedComputedSize = null;

            super.computeSize(0, 0);
        }

        /**
         * We override this as we change our cursor from the hand cursor only when the mouse exists this instance's
         * bounds.
         *
         * {@inheritDoc}
         */
        @Override
        protected void mouseMovedOutsideNAryBounds(final MouseEvent me) { }

        @Override
        public Point computeSize(final int wHint, final int hHint, final boolean changed) {
            if (m_cachedComputedSize == null) {
                final RenderSizeData rsd =
                        calculateRenderSize(this, m_displayText, m_horizontalInset, m_verticalInset, m_renderEdit);

                m_cachedComputedSize = rsd.getEntireSize();

                /*
                 * Here is some pretty lame hackery - for some indecipherable reason, we are with the above code
                 *  telling SWT the very correct size that we should be. SWT takes the value and in our parent
                 *  Composite's layout does some incorrect calculation and says - "oh, ok.. so you [of width X] and
                 *  the bullet label [of with Y] horizontally preceding you and the 3 pixels of interspace between
                 *  the two specified to me in my layout definition --- lemme sum that up.. oh - of course,
                 *  my width should be "X + Y + 3 ..... minus ~15 for some reason!"
                 */
                m_cachedComputedSize.x += 15;
            }

            return m_cachedComputedSize;
        }
    }

    /**
     * @see MetaInfoAtom#messageListeners(ListenerEventType)
     */
    protected enum ListenerEventType {
        /** Message about a deletion **/
        DELETE,
        /** Message about becoming dirty in edit **/
        DIRTY,
        /** Message about becoming clean in edit **/
        CLEAN;
    }
}
