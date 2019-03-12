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
 *   Feb 12, 2019 (loki): created
 */
package org.knime.workbench.editor2.directannotationedit;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.knime.workbench.core.LayoutExemptingLayout;
import org.knime.workbench.editor2.ViewportPinningGraphicalViewer;

/**
 * As part of AP-9129, this <code>FlatButton</code> specialization provides a numeric pulldown.
 *
 * @author loki der quaeler
 */
public class NumericPulldownFlatButton extends FlatButton
    implements FlatButton.ClickListener, TransientEditAssetGroup.AssetProvider {

    private static final Point PULLDOWN_WIDGET_SIZE = new Point(32, 22);
    private static final Point CHEVRON_SIZE = new Point(8, 4);
    private static final int CHEVRON_INSET = 4;
    private static final int VALUE_DRAW_WIDTH = PULLDOWN_WIDGET_SIZE.x - 2 - CHEVRON_INSET - CHEVRON_SIZE.x;

    private static Font VALUE_FONT = null;
    private static Color VALUE_COLOR = null;
    private static Color PULLDOWN_BACKGROUND_COLOR = null;
    private static Color PULLDOWN_FOREGROUND_COLOR = null;

    private static synchronized void initializeSWTAssetsIfNecessary() {
        if (VALUE_FONT == null) {
            final Display d = Display.getDefault();
            final int fontSize = StyledTextEditor.PLATFORM_IS_WINDOWS ? 12 : 14;

            VALUE_FONT = new Font(d, new FontData("Helvetica", fontSize, SWT.NORMAL));
            VALUE_COLOR = ColorConstants.black;

            PULLDOWN_BACKGROUND_COLOR = ColorConstants.white;
            PULLDOWN_FOREGROUND_COLOR = ColorDropDown.createColorFromHexString(d, "#797979");
        }
    }

    private static Path createChevronPath(final Display display, final Rectangle pulldownBounds) {
        final Path path = new Path(display);
        int x = pulldownBounds.x + pulldownBounds.width - CHEVRON_INSET;
        int y = pulldownBounds.y + ((pulldownBounds.height - CHEVRON_SIZE.y) / 2);

        path.moveTo(x, y);

        x -= CHEVRON_SIZE.x / 2;
        y += CHEVRON_SIZE.y;
        path.lineTo(x, y);

        x -= CHEVRON_SIZE.x / 2;
        y -= CHEVRON_SIZE.y;
        path.lineTo(x, y);

        return path;
    }


    private final int[] m_valueList;
    private final AtomicInteger m_selectedValue;
    private String m_cachedSelectedValue;

    private final Rectangle m_pulldownBounds;
    private final Rectangle m_pulldownBorder;

    private final Path m_chevronPath;

    private final AtomicBoolean m_isMultiValued;

    private final DropDownValueList m_dropDownList;
    private final Text m_textBox;

    private final StyledTextEditor m_styledTextEditor;

    /**
     * @param parent the parent widget which owns this instance; if the layout of this widget is <code>GridLayout</code>
     *            then size appropriate <code>GridData</code> will be set.
     * @param values the value choices this button will provide; it is expected to be naturally ordered
     * @param editor the owning editor, if one exists
     */
    public NumericPulldownFlatButton(final Composite parent, final int[] values, final StyledTextEditor editor) {
        super(parent, SWT.PUSH, (PaintListener)null, PULLDOWN_WIDGET_SIZE);

        initializeSWTAssetsIfNecessary();

        m_valueList = values;
        m_selectedValue = new AtomicInteger(values[0]);
        m_cachedSelectedValue = Integer.toString(values[0]);
        m_isMultiValued = new AtomicBoolean(false);

        final Point size = getSize();
        final int pulldownLocationX = 0;
        final int pulldownLocationY = (size.y - PULLDOWN_WIDGET_SIZE.y) / 2;
        m_pulldownBounds =
            new Rectangle(pulldownLocationX, pulldownLocationY, PULLDOWN_WIDGET_SIZE.x, PULLDOWN_WIDGET_SIZE.y);
        m_pulldownBorder = new Rectangle(m_pulldownBounds.x, m_pulldownBounds.y, (m_pulldownBounds.width - 1),
            (m_pulldownBounds.height - 1));

        m_chevronPath = createChevronPath(getDisplay(), m_pulldownBounds);

        m_dropDownList = new DropDownValueList();

        m_textBox = new Text(ViewportPinningGraphicalViewer.getActiveViewportComposite(), SWT.BORDER);
        m_textBox.addKeyListener(KeyListener.keyPressedAdapter((event) -> {
            if (event.keyCode == SWT.CR) {
                final String text = m_textBox.getText();

                try {
                    int value = Integer.parseInt(text);

                    event.doit = false;

                    userSelectedValue(value);
                } catch (NumberFormatException e) { }  // NOPMD
            }
        }));
        m_textBox.setSize(PULLDOWN_WIDGET_SIZE.x, PULLDOWN_WIDGET_SIZE.y);
        m_textBox.setBackground(PULLDOWN_BACKGROUND_COLOR);
        m_textBox.setFont(VALUE_FONT);
        m_textBox.setVisible(false);
        m_textBox.moveBelow(null);
        LayoutExemptingLayout.exemptControlFromLayout(m_textBox);

        m_styledTextEditor = editor;

        addClickListener(this);
    }

    /**
     * This can be invoked from any thread and triggers a redraw; calling this will clear a previously set multivalued state.
     *
     * @param value the value to be displayed.
     */
    public void setSelectedValue(final int value) {
        m_selectedValue.set(value);
        m_cachedSelectedValue = Integer.toString(value);
        m_textBox.setText(m_cachedSelectedValue);
        m_isMultiValued.set(false);

        m_dropDownList.valueWasSet(value);

        getDisplay().asyncExec(() -> {
            if (!isDisposed()) {
                redraw();
            }
        });
    }

    /**
     * @return the last set selected value, or the last value chosen from the pulldown by the user, whichever occurred
     *         more recently.
     */
    public int getSelectedValue() {
        return m_selectedValue.get();
    }

    /**
     * @param flag this sets whether this button represents a multivalue selection; if the value of <code>flag</code> is
     *            different than the current multivalue state, a redraw is triggered.
     */
    public void setMultiValued(final boolean flag) {
        if (flag != m_isMultiValued.getAndSet(flag)) {
            if (flag) {
                m_textBox.setText("");
            }

            getDisplay().asyncExec(() -> {
                redraw();
            });
        }
    }

    /**
     * @return the value set by <code>setMultiValued(boolean)</code> or false if <code>setSelectedValue(int)</code> has
     *         been called since then
     */
    public boolean isMultiValued() {
        return m_isMultiValued.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Point calculateImageDrawLocation(final Point buttonSize, final ImageData imageData,
        final boolean verticalCenter) {
        final int y = verticalCenter ? Math.abs((buttonSize.y - imageData.height) / 2) : 0;

        return new Point(0, y);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void handlePaintingPostProcess(final PaintEvent pe) {
        final GC gc = pe.gc;
        final String text = m_isMultiValued.get() ? "-" : m_cachedSelectedValue;

        gc.setAdvanced(true);
        gc.setAntialias(SWT.ON);

        // pulldown box background
        gc.setBackground(PULLDOWN_BACKGROUND_COLOR);
        gc.fillRectangle(m_pulldownBounds);

        // value
        gc.setForeground(VALUE_COLOR);
        gc.setFont(VALUE_FONT);
        gc.setTextAntialias(SWT.ON);

        final Point stringSize = gc.stringExtent(text);
        final int drawX = m_pulldownBounds.x + ((VALUE_DRAW_WIDTH - stringSize.x) / 2);
        final int drawY = m_pulldownBounds.y + ((m_pulldownBounds.height - stringSize.y) / 2);
        gc.drawText(text, (StyledTextEditor.PLATFORM_IS_WINDOWS ? (drawX + 1) : drawX), drawY);

        // pulldown box border
        gc.setForeground(PULLDOWN_FOREGROUND_COLOR);
        gc.drawRectangle(m_pulldownBorder);

        // chevron
        gc.drawPath(m_chevronPath);

        super.handlePaintingPostProcess(pe);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clickOccurred(final FlatButton source) {
        final Rectangle bounds = source.getParent().getBounds();
        final Point sourceLocation = source.getLocation();
        final int xLocation = sourceLocation.x + bounds.x + m_pulldownBounds.x;
        final int textBoxYLocation = bounds.y + sourceLocation.y + m_pulldownBounds.y;

        m_dropDownList.setLocation(xLocation, (bounds.y + bounds.height));
        m_textBox.setLocation(xLocation, textBoxYLocation);

        m_dropDownList.setVisible(true);
        m_dropDownList.moveAbove(null);
        m_textBox.setVisible(true);
        m_textBox.moveAbove(null);

        m_styledTextEditor.setFocusLossAllowed(false);

        m_textBox.setFocus();
        m_textBox.selectAll();
    }

    @Override
    public void shouldHideEditAssets() {
        if (m_dropDownList.isVisible()) {
            m_dropDownList.setVisible(false);
            m_dropDownList.moveBelow(null);
            m_textBox.setVisible(false);
            m_textBox.moveBelow(null);

            m_styledTextEditor.setFocusLossAllowed(true);
        }
    }

    @Override
    public Widget getEditAsset() {
        return m_textBox;
    }

    int getRequiredDropDownHeight() {
        return m_dropDownList.getBounds().height;
    }

    private void userSelectedValue(final int value) {
        shouldHideEditAssets();

        setSelectedValue(value);

        messageListeners(true);
    }


    private class DropDownValueList extends Canvas {
        private final LabelFlatButton[] m_buttons;
        private int m_selectedIndex;

        DropDownValueList() {
            super(ViewportPinningGraphicalViewer.getActiveViewportComposite(), SWT.NONE);

            LayoutExemptingLayout.exemptControlFromLayout(this);

            final GridLayout layout = new GridLayout(1, true);
            layout.marginHeight = 1;
            layout.marginWidth = 1;
            layout.horizontalSpacing = 0;
            layout.verticalSpacing = 0;
            setLayout(layout);

            m_selectedIndex = -1;

            final String prototype = Integer.toString(m_valueList[m_valueList.length - 1] * 100);

            m_buttons = new LabelFlatButton[m_valueList.length];
            for (int i = 0; i < m_valueList.length; i++) {
                final int value = m_valueList[i];
                final LabelFlatButton b = new LabelFlatButton(this, Integer.toString(value), prototype, true);

                b.setBackground(PULLDOWN_BACKGROUND_COLOR);
                b.addClickListener((source) -> {
                    final int selectedValue = Integer.parseInt(((LabelFlatButton)source).getText());

                    userSelectedValue(selectedValue);
                });

                m_buttons[i] = b;
            }

            addPaintListener((pe) -> {
               paintBackground(pe);
            });

            pack();
        }

        private void valueWasSet(final int value) {
            final int index = Arrays.binarySearch(m_valueList, value);

            if (m_selectedIndex >= 0) {
                m_buttons[m_selectedIndex].setSelected(false);
            }

            if (index >= 0) {
                m_buttons[index].setSelected(true);
            }

            m_selectedIndex = index;
        }

        private void paintBackground(final PaintEvent pe) {
            final Point size = getSize();
            final GC gc = pe.gc;

            gc.setBackground(PULLDOWN_BACKGROUND_COLOR);
            gc.fillRectangle(0, 0, size.x, size.y);
            gc.setForeground(PULLDOWN_FOREGROUND_COLOR);
            gc.drawRectangle(0, 0, (size.x - 1), (size.y - 1));
        }
    }
}
