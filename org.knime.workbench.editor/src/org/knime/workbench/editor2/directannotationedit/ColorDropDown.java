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
 *   Feb 5, 2019 (loki): created
 */
package org.knime.workbench.editor2.directannotationedit;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
import org.knime.core.util.ColorUtilities;
import org.knime.workbench.core.LayoutExemptingLayout;
import org.knime.workbench.editor2.ViewportPinningGraphicalViewer;

/**
 * This is a color drop down picker created originally to be used with the annotation styling toolbar, but presumably is
 * generic enough that it should be moved to a more central location at some point.
 *
 *
 * @author loki der quaeler
 */
public class ColorDropDown extends Canvas implements TransientEditAssetGroup.AssetProvider {

    private static final int DROP_SHADOW_OUTSET = 2;

    private static final Point GRID_WELL_SIZE = new Point(18, 18);
    private static final Rectangle GRID_WELL_PAINT_BOUNDS = new Rectangle(0, 0, GRID_WELL_SIZE.x, GRID_WELL_SIZE.y);
    private static final Rectangle GRID_WELL_BORDER_PAINT_BOUNDS =
        new Rectangle(0, 0, (GRID_WELL_SIZE.x - 1), (GRID_WELL_SIZE.y - 1));
    private static final Rectangle GRID_WELL_SELECTION_BORDER_PAINT_BOUNDS =
        new Rectangle(1, 1, (GRID_WELL_SIZE.x - 3), (GRID_WELL_SIZE.y - 3));

    private static final String[][] COLOR_GRID_HEX_VALUES =
        {{"#EF2929", "#FCAF3E", "#E9B96E", "#FCE94F", "#8AE234", "#B2CFD6", "#729FCF", "#AD7FA8", "#EFF0EE"},
            {"#CC0000", "#F57900", "#C17D11", "#EDD400", "#73D216", "#70A7B4", "#3465A4", "#75507B", "#D3D7CF"},
            {"#A40000", "#CE5C00", "#8F5902", "#FFD800", "#4E9A06", "#1B6A7D", "#204A87", "#5C3566", "#BABDB6"},
            {"#000000", "#2E3436", "#555753", "#888A85", "#BABDB6", "#D3D7CF", "#EEEEEC", "#F3F3F3", "#FFFFFF"}};
    private static final boolean[][] GRID_WELL_NEEDING_BORDER =
        {{false, false, false, false, false, false, false, false, true},
            {false, false, false, false, false, false, false, false, false},
            {false, false, false, false, false, false, false, false, false},
            {false, false, false, false, false, false, true, true, true}};
    private static final boolean[][] GRID_WELL_SELECTION_IS_DARK =
        {{true, true, true, true, true, true, true, true, true},
            {false, true, true, true, true, true, true, true, true},
            {false, true, false, true, true, false, false, false, true},
            {false, false, false, false, true, true, true, true, true}};

    private static Color BACKGROUND_COLOR = null;
    private static Color BORDER_COLOR = null;
    private static Color DROP_SHADOW_COLOR = null;
    private static Color GRID_SELECTION_DARK_COLOR = null;
    private static Color GRID_SELECTION_LIGHT_COLOR = null;
    private static Color GRID_WELL_BORDER_COLOR = null;

    private static Color[][] GRID_WELL_COLORS = null;
    private static HashMap<Color, Point> COLOR_GRID_LOCATION_MAP = null;

    private static synchronized void initializeColorsIfNecessary() {
        if (DROP_SHADOW_COLOR == null) {
            final Display d = Display.getDefault();

            BACKGROUND_COLOR = createColorFromHexString(d, "#F0F0F0");
            BORDER_COLOR = createColorFromHexString(d, "#4D4D4D");
            DROP_SHADOW_COLOR = new Color(d, 204, 204, 204);
            GRID_SELECTION_DARK_COLOR = ColorConstants.black;
            GRID_SELECTION_LIGHT_COLOR = ColorConstants.white;
            GRID_WELL_BORDER_COLOR = createColorFromHexString(d, "#BFBFBF");

            final int gridRowCount = COLOR_GRID_HEX_VALUES.length;
            final int gridColumnCount = COLOR_GRID_HEX_VALUES[0].length;
            COLOR_GRID_LOCATION_MAP = new HashMap<>();
            GRID_WELL_COLORS = new Color[gridRowCount][gridColumnCount];
            for (int x = 0; x < gridColumnCount; x++) {
                for (int y = 0; y < gridRowCount; y++) {
                    GRID_WELL_COLORS[y][x] = createColorFromHexString(d, COLOR_GRID_HEX_VALUES[y][x]);
                    COLOR_GRID_LOCATION_MAP.put(GRID_WELL_COLORS[y][x], new Point(x, y));
                }
            }
        }
    }

    // Frameworks candidate... oh SWT, why are you so insufficient.. seriously - an obvious use case ignored
    static Color createColorFromHexString(final Display display, final String hexString) {
        java.awt.Color c = java.awt.Color.decode(hexString);

        return new Color(display, c.getRed(), c.getGreen(), c.getBlue());
    }


    private final boolean m_drawDropShadow;
    private final Composite m_wellGridPane;
    private final HashMap<Point, FlatButton> m_gridLocationButtonMap;

    private final LabelFlatButton m_customColorButton;

    private FlatButton m_currentlySelectedButton;
    private Color m_currentlySelectedColor;

    private final StyledTextEditor m_styledTextEditor;

    private final AtomicLong m_lastCustomChooserInteraction;

    /**
     * Constructs a color drop down picker
     *
     * @param editor the editor while is hosting the stylized annotation
     * @param dropShadow if the panel should render a drop shadow, this should be set to true
     */
    public ColorDropDown(final StyledTextEditor editor, final boolean dropShadow) {
        super(ViewportPinningGraphicalViewer.getActiveViewportComposite(), SWT.NONE);

        LayoutExemptingLayout.exemptControlFromLayout(this);

        initializeColorsIfNecessary();

        m_drawDropShadow = dropShadow;
        m_styledTextEditor = editor;

        m_lastCustomChooserInteraction = new AtomicLong(-1);

        addPaintListener((pe) -> {
           paintBackground(pe);
        });

        final GridLayout layout = new GridLayout(1, true);
        layout.marginHeight = 0;
        layout.marginTop = 5;
        layout.marginBottom = 7;
        setLayout(layout);

        m_wellGridPane = new Composite(this, SWT.NONE);
        final GridLayout gridPaneLayout = new GridLayout(COLOR_GRID_HEX_VALUES[0].length, true);
        gridPaneLayout.marginHeight = 0;
        gridPaneLayout.marginWidth = 0;
        gridPaneLayout.horizontalSpacing = 2;
        gridPaneLayout.verticalSpacing = 2;
        m_wellGridPane.setLayout(gridPaneLayout);
        m_wellGridPane.setVisible(false);

        m_gridLocationButtonMap = new HashMap<>();
        buildButtonGrid();

        m_customColorButton = new LabelFlatButton(this, "Custom...");
        m_customColorButton.addClickListener((source) -> {
            final Color currentSelection = (m_currentlySelectedColor != null) ? m_currentlySelectedColor : null;
            final Color userSelectedColor;

            m_lastCustomChooserInteraction.set(System.currentTimeMillis());

            final Shell disposableShell;
            final ColorDialog colorDialog;
            // the ColorDialog on Windows locates its window at the location of the parent shell
            if (StyledTextEditor.PLATFORM_IS_WINDOWS) {
                disposableShell = new Shell(getShell(), SWT.NO_TRIM);
                disposableShell.setSize(0, 0);
                disposableShell.setLocation(getParent().toDisplay(getLocation()));

                colorDialog = new ColorDialog(disposableShell);
            } else {
                colorDialog = new ColorDialog(getShell());
                disposableShell = null;
            }

            colorDialog.setText("Choose Color");

            if (currentSelection != null) {
                colorDialog.setRGB(currentSelection.getRGB());
            }

            final RGB selectedRGB = colorDialog.open();

            if (disposableShell != null) {
                disposableShell.dispose();
            }

            m_lastCustomChooserInteraction.set(System.currentTimeMillis());

            userSelectedColor = (selectedRGB != null) ? ColorUtilities.RGBtoColor(selectedRGB) : null;

            m_styledTextEditor.colorWasSelected(userSelectedColor);
        });

        pack();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVisible(final boolean visible) {
        super.setVisible(visible);
        m_wellGridPane.setVisible(visible);

        if (visible) {
            moveAbove(null);
            m_wellGridPane.moveAbove(null);
        } else {
            moveBelow(null);
            m_wellGridPane.moveBelow(null);
        }
    }

    @Override
    public void shouldHideEditAssets() {
        if (isVisible()) {
            m_styledTextEditor.colorWasSelected(null);
        }
    }

    @Override
    public Widget getEditAsset() {
        return this;
    }

    // We have this hack to prevent the relegation of the custom color chooser on Linux to tell the styled text widget
    //      to hide.
    boolean customColorChooserWasDisplayedInTheLastTimeWindow(final long timeWindowInMS) {
        final long delta = (System.currentTimeMillis() - m_lastCustomChooserInteraction.get());

        return (delta < timeWindowInMS);
    }

    void setSelectedColor(final Color c) {
        final Point gridLocation = COLOR_GRID_LOCATION_MAP.get(c);

        resetSelection();

        if (gridLocation != null) {
            final FlatButton fb = m_gridLocationButtonMap.get(gridLocation);

            fb.setSelected(true);
            m_currentlySelectedButton = fb;
        }

        m_currentlySelectedColor = c;
    }

    private void resetSelection() {
        if (m_currentlySelectedButton != null) {
            m_currentlySelectedButton.setSelected(false);
            m_currentlySelectedButton = null;
        }

        m_currentlySelectedColor = null;
    }

    private void buildButtonGrid() {
        final int gridRowCount = GRID_WELL_COLORS.length;
        final int gridColumnCount = GRID_WELL_COLORS[0].length;
        for (int y = 0; y < gridRowCount; y++) {
            for (int x = 0; x < gridColumnCount; x++) {
                final Color fillColor = GRID_WELL_COLORS[y][x];
                final boolean paintBorder = GRID_WELL_NEEDING_BORDER[y][x];
                final boolean paintSelectionDark = GRID_WELL_SELECTION_IS_DARK[y][x];
                final FlatButton button =
                    new FlatButton(m_wellGridPane, SWT.TOGGLE, (PaintListener)null, GRID_WELL_SIZE);
                final PaintListener painter = (paintEvent) -> {
                    final GC gc = paintEvent.gc;

                    gc.setAdvanced(true);
                    gc.setAntialias(SWT.ON);
                    gc.setBackground(fillColor);
                    gc.fillRectangle(GRID_WELL_PAINT_BOUNDS);

                    if (paintBorder) {
                        gc.setForeground(GRID_WELL_BORDER_COLOR);
                        gc.drawRectangle(GRID_WELL_BORDER_PAINT_BOUNDS);
                    }

                    if (button.isSelected()) {
                        final Color c = paintSelectionDark ? GRID_SELECTION_DARK_COLOR : GRID_SELECTION_LIGHT_COLOR;

                        gc.setForeground(c);

                        final int x00 = GRID_WELL_SELECTION_BORDER_PAINT_BOUNDS.x;
                        final int y00 = (GRID_WELL_SELECTION_BORDER_PAINT_BOUNDS.y
                            + GRID_WELL_SELECTION_BORDER_PAINT_BOUNDS.height);
                        final int x01 = (GRID_WELL_SELECTION_BORDER_PAINT_BOUNDS.x + GRID_WELL_SELECTION_BORDER_PAINT_BOUNDS.width);
                        final int y01 = GRID_WELL_SELECTION_BORDER_PAINT_BOUNDS.y;
                        final int x10 = GRID_WELL_SELECTION_BORDER_PAINT_BOUNDS.x;
                        final int y10 = GRID_WELL_SELECTION_BORDER_PAINT_BOUNDS.y;
                        final int x11 = (GRID_WELL_SELECTION_BORDER_PAINT_BOUNDS.x + GRID_WELL_SELECTION_BORDER_PAINT_BOUNDS.width);
                        final int y11 = (GRID_WELL_SELECTION_BORDER_PAINT_BOUNDS.y
                                + GRID_WELL_SELECTION_BORDER_PAINT_BOUNDS.height);

                        gc.drawLine(x00, y00, x01, y01);
                        gc.drawLine(x10, y10, x11, y11);

                        gc.drawRectangle(GRID_WELL_SELECTION_BORDER_PAINT_BOUNDS);
                    }
                };

                button.setPostRenderer(painter);
                button.setAvoidSelectionAndMouseRendering(true);
                button.setBackground(fillColor);
                button.setData(fillColor);
                button.addClickListener((source) -> {
                    resetSelection();

                    m_currentlySelectedButton = source;
                    m_currentlySelectedColor = (Color)source.getData();
                    m_styledTextEditor.colorWasSelected(m_currentlySelectedColor);
                });

                m_gridLocationButtonMap.put(new Point(x, y), button);
            }
        }

        m_wellGridPane.pack();
    }

    private void paintBackground(final PaintEvent pe) {
        final Point size = getSize();
        final GC gc = pe.gc;
        final int width = m_drawDropShadow ? (size.x - DROP_SHADOW_OUTSET) : size.x;
        final int height = m_drawDropShadow ? (size.y - DROP_SHADOW_OUTSET) : size.y;

        if (m_drawDropShadow) {
            gc.setBackground(DROP_SHADOW_COLOR);
            gc.setAlpha(55);
            gc.fillRectangle(DROP_SHADOW_OUTSET, DROP_SHADOW_OUTSET, width, height);
            gc.setAlpha(255);
        }

        gc.setBackground(BACKGROUND_COLOR);
        gc.fillRectangle(0, 0, width, height);
        gc.setForeground(BORDER_COLOR);
        gc.drawRectangle(0, 0, (width - 1), (height - 1));
    }
}
