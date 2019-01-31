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
 *   Jan 28, 2019 (loki): created
 */
package org.knime.workbench.editor2.directannotationedit;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;

/**
 * The genesis for this component is AP-9129 which seeks to replace the right click context menu available when
 *  editing an annotation with a toolbar which floats vertically above or below the annotation.
 *
 * @author loki der quaeler
 */
public class AnnotationEditFloatingToolbar extends Composite implements FlatButton.ClickListener {
    private static final int[] AVAILABLE_FONT_SIZES = {6, 8, 9, 11, 13, 16, 18, 20, 24, 28, 36};
    private static final int[] AVAILABLE_BORDER_THICKNESSES = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16};

    private static final int ROUNDED_RADIUS = 2;

    private static final Point DEFAULT_BUTTON_SIZE = new Point(23, 22);
    private static final Rectangle COLOR_WELL_RECTANGLE = new Rectangle(2, 16, 17, 4);

    private static Color BACKGROUND_COLOR = null;
    private static Color BORDER_COLOR = null;

    private static synchronized void initializeSWTAssetsIfNecessary() {
        if (BACKGROUND_COLOR == null) {
            final Display d = Display.getDefault();

            BACKGROUND_COLOR = ColorDropDown.createColorFromHexString(d, "#F0F0F0");
            BORDER_COLOR = ColorDropDown.createColorFromHexString(d, "#D7D7D7");
        }
    }

    private static void fillButtonColorWell(final GC gc, final Color color) {
        if (color == null) {
            return;
        } else {
            gc.setBackground(color);
            gc.fillRectangle(COLOR_WELL_RECTANGLE);
        }


        gc.setForeground(ColorConstants.black);
        gc.drawRectangle(COLOR_WELL_RECTANGLE);
    }


    private final NumericPulldownFlatButton m_fontSizeButton;

    private final FlatButton m_boldToggleButton;
    private final FlatButton m_italicToggleButton;
    private final FlatButton m_fontColorButton;

    @SuppressWarnings("unused")  // we hang on to it by name for cleaner object graphs
    private final FlatButtonRadioGroup m_radioGroup;
    private final FlatButton m_alignLeftRadioButton;
    private final FlatButton m_alignCenterRadioButton;
    private final FlatButton m_alignRightRadioButton;

    private final NumericPulldownFlatButton m_borderWidthButton;
    private final FlatButton m_borderColorButton;

    private final FlatButton m_backgroundColorButton;

    private final TransientEditAssetGroup m_editAssetGroup;

    private final StyledTextEditor m_styledTextEditor;

    private final int m_requiredHeightForNumericPulldownDisplay;

    /**
     * @param parent the SWT container which holds the toolbar
     * @param editor the editor while is hosting the stylized annotation
     */
    public AnnotationEditFloatingToolbar(final Composite parent, final StyledTextEditor editor) {
        super(parent, SWT.NONE);

        m_styledTextEditor = editor;

        addPaintListener((pe) -> {
           paintBackground(pe);
        });

        getShell().setBackgroundMode(SWT.INHERIT_FORCE);

        initializeSWTAssetsIfNecessary();

        final GridLayout layout = new GridLayout(15, false);
        layout.marginHeight = 0;
        layout.marginTop = 4;
        layout.marginBottom = 4;
        layout.marginWidth = 5;
        layout.horizontalSpacing = 4;
        layout.verticalSpacing = 0;
        setLayout(layout);

        Label imageLabel = new Label(this, SWT.NONE);
        imageLabel
            .setImage(ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "/icons/annotations/font-size-icon.png"));
        imageLabel.setBackground(BACKGROUND_COLOR);
        m_fontSizeButton = new NumericPulldownFlatButton(this, AVAILABLE_FONT_SIZES, editor);
        m_fontSizeButton.addClickListener(this);
        m_fontSizeButton.setBackground(BACKGROUND_COLOR);
        m_fontSizeButton.setToolTipText("Font Size");

        m_boldToggleButton = new FlatButton(this, SWT.TOGGLE,
            ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "/icons/annotations/bold-icon.png"), null,
            DEFAULT_BUTTON_SIZE, true);
        m_boldToggleButton.addClickListener(this);
        m_boldToggleButton.setBackground(BACKGROUND_COLOR);
        m_boldToggleButton.setToolTipText("Bold Text");

        m_italicToggleButton = new FlatButton(this, SWT.TOGGLE,
            ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "/icons/annotations/italic-icon.png"), null,
            DEFAULT_BUTTON_SIZE, true);
        m_italicToggleButton.addClickListener(this);
        m_italicToggleButton.setBackground(BACKGROUND_COLOR);
        m_italicToggleButton.setToolTipText("Italic Text");

        m_fontColorButton = new FlatButton(this, SWT.PUSH,
            ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "/icons/annotations/font-color-icon.png"),
            DEFAULT_BUTTON_SIZE);
        m_fontColorButton.addClickListener(this);
        m_fontColorButton.setPostRenderer((pe) -> {
            final GC gc = pe.gc;
            final Color c = m_styledTextEditor.getCurrentFontColor();

            fillButtonColorWell(gc, c);
        });
        m_fontColorButton.setBackground(BACKGROUND_COLOR);
        m_fontColorButton.setToolTipText("Font Color");

        final Label firstSeparator = new Label(this, SWT.SEPARATOR | SWT.VERTICAL);
        GridData gd = new GridData();
        gd.heightHint = DEFAULT_BUTTON_SIZE.y;
        gd.verticalAlignment = SWT.CENTER;
        firstSeparator.setLayoutData(gd);

        m_alignLeftRadioButton = new FlatButton(this, SWT.TOGGLE,
            ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "/icons/annotations/left-align-icon.png"), null,
            DEFAULT_BUTTON_SIZE, true);
        m_alignLeftRadioButton.addClickListener(this);
        m_alignLeftRadioButton.setBackground(BACKGROUND_COLOR);
        m_alignLeftRadioButton.setToolTipText("Align Left");
        m_alignCenterRadioButton = new FlatButton(this, SWT.TOGGLE,
            ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "/icons/annotations/center-align-icon.png"), null,
            DEFAULT_BUTTON_SIZE, true);
        m_alignCenterRadioButton.addClickListener(this);
        m_alignCenterRadioButton.setBackground(BACKGROUND_COLOR);
        m_alignCenterRadioButton.setToolTipText("Align Center");
        m_alignRightRadioButton = new FlatButton(this, SWT.TOGGLE,
            ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "/icons/annotations/right-align-icon.png"), null,
            DEFAULT_BUTTON_SIZE, true);
        m_alignRightRadioButton.addClickListener(this);
        m_alignRightRadioButton.setBackground(BACKGROUND_COLOR);
        m_alignRightRadioButton.setToolTipText("Align Right");
        m_radioGroup =
            new FlatButtonRadioGroup(m_alignLeftRadioButton, m_alignCenterRadioButton, m_alignRightRadioButton);
        m_alignLeftRadioButton.setSelected(true);

        final Label secondSeparator = new Label(this, SWT.SEPARATOR | SWT.VERTICAL);
        gd = new GridData();
        gd.heightHint = DEFAULT_BUTTON_SIZE.y;
        gd.verticalAlignment = SWT.CENTER;
        secondSeparator.setLayoutData(gd);

        imageLabel = new Label(this, SWT.NONE);
        imageLabel.setImage(
            ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "/icons/annotations/border-width-icon.png"));
        imageLabel.setBackground(BACKGROUND_COLOR);
        m_borderWidthButton = new NumericPulldownFlatButton(this, AVAILABLE_BORDER_THICKNESSES, editor);
        m_borderWidthButton.addClickListener(this);
        m_borderWidthButton.setBackground(BACKGROUND_COLOR);
        m_borderWidthButton.setToolTipText("Border Width");

        m_borderColorButton = new FlatButton(this, SWT.PUSH,
            ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "/icons/annotations/border-color-icon.png"),
            DEFAULT_BUTTON_SIZE);
        m_borderColorButton.addClickListener(this);
        m_borderColorButton.setPostRenderer((pe) -> {
            final GC gc = pe.gc;
            final Color c = m_styledTextEditor.getCurrentBorderColor();

            if (c == null) {
                return;
            }

            fillButtonColorWell(gc, c);
        });
        m_borderColorButton.setBackground(BACKGROUND_COLOR);
        m_borderColorButton.setToolTipText("Border Color");

        m_backgroundColorButton = new FlatButton(this, SWT.PUSH,
            ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "/icons/annotations/background-color-icon.png"),
            DEFAULT_BUTTON_SIZE);
        m_backgroundColorButton.addClickListener(this);
        m_backgroundColorButton.setPostRenderer((pe) -> {
            final GC gc = pe.gc;
            final Color c = m_styledTextEditor.getCurrentBackgroundColor();

            if (c == null) {
                return;
            }

            fillButtonColorWell(gc, c);
        });
        m_backgroundColorButton.setBackground(BACKGROUND_COLOR);
        m_backgroundColorButton.setToolTipText("Background Color");

        m_editAssetGroup = new TransientEditAssetGroup();
        m_editAssetGroup.addAssetProvider(m_fontSizeButton);
        m_editAssetGroup.addAssetProvider(m_borderWidthButton);

        pack();

        m_requiredHeightForNumericPulldownDisplay =
            Math.max(m_fontSizeButton.getRequiredDropDownHeight(), m_borderWidthButton.getRequiredDropDownHeight());

        updateToolbarToReflectState();
    }

    /**
     * This should be called when the toolbar need update its visual state to match what is currently selected, or at
     * place of insert, in the associated text editor.
     */
    public void updateToolbarToReflectState() {
        final int fontStyle = m_styledTextEditor.getCurrentFontStyle();

        m_boldToggleButton.setSelected((fontStyle & SWT.BOLD) == SWT.BOLD);
        m_italicToggleButton.setSelected((fontStyle & SWT.ITALIC) == SWT.ITALIC);

        final int fontSize = m_styledTextEditor.getCurrentFontSize();
        if (fontSize != -1) {
            m_fontSizeButton.setSelectedValue(fontSize);
        } else {
            m_fontSizeButton.setMultiValued(true);
        }

        updateAlignmentButtons(m_styledTextEditor.getCurrentAlignment());

        m_borderWidthButton.setSelectedValue(m_styledTextEditor.getCurrentBorderWidth());

        getDisplay().asyncExec(() -> {
            redraw();

            if (StyledTextEditor.PLATFORM_IS_WINDOWS) {
                // For some reason, on Windows, the toolbar is not triggering a redraw of the custom painters on flat
                //      buttons (so, for example, a custom color well will not be visually updated until some other
                //      dirty-ing event like the mouse moving over it.)
                m_fontColorButton.redraw();
                m_borderColorButton.redraw();
                m_backgroundColorButton.redraw();
            }
        });
    }

    private void updateAlignmentButtons(final int alignment) {
        FlatButton buttonToSelect;

        switch (alignment) {
            case SWT.LEFT:
                buttonToSelect = m_alignLeftRadioButton;
                break;
            case SWT.CENTER:
                buttonToSelect = m_alignCenterRadioButton;
                break;
            default:
                buttonToSelect = m_alignRightRadioButton;
                break;
        }

        if (! buttonToSelect.isSelected()) {
            buttonToSelect.setSelected(true);
            m_radioGroup.updateButtonsDueToProgrammaticSelection(buttonToSelect);
        }
    }

    /**
     * This makes sure that all numeric pulldown buttons in the toolbar have hidden their edit assets.
     */
    public void ensureEditAssetsAreNotVisible() {
        m_fontSizeButton.shouldHideEditAssets();
        m_borderWidthButton.shouldHideEditAssets();
    }

    /**
     * This returns the height below the toolbar which will be need to fully display all pulldowns.
     *
     * @return an <code>int</code> describing the minimum addition height below the toolbar needed for all UI elements
     *         to be interacted with by the user.
     */
    public int getRequiredMinimumHeightForDropdownAssets() {
        return m_requiredHeightForNumericPulldownDisplay;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clickOccurred(final FlatButton source) {
        if (source == m_fontSizeButton) {
            if (!m_fontSizeButton.isMultiValued()) {
                m_styledTextEditor.fontSizeWasSelected(m_fontSizeButton.getSelectedValue());
            }
        }
        else if (source == m_boldToggleButton) {
            m_styledTextEditor.setSWTStyle(SWT.BOLD);
        } else if (source == m_italicToggleButton) {
            m_styledTextEditor.setSWTStyle(SWT.ITALIC);
        } else if (source == m_fontColorButton) {
            m_styledTextEditor.userWantsToAffectFontColor(source.getLocation());
        } else if (source == m_alignLeftRadioButton) {
            m_styledTextEditor.alignment(SWT.LEFT);
        } else if (source == m_alignCenterRadioButton) {
            m_styledTextEditor.alignment(SWT.CENTER);
        } else if (source == m_alignRightRadioButton) {
            m_styledTextEditor.alignment(SWT.RIGHT);
        } else if (source == m_borderWidthButton) {
            m_styledTextEditor.borderWidthWasSelected(m_borderWidthButton.getSelectedValue());
        } else if (source == m_borderColorButton) {
            m_styledTextEditor.userWantsToAffectBorderColor(source.getLocation());
        } else if (source == m_backgroundColorButton) {
            m_styledTextEditor.userWantsToAffectBackgroundColor(source.getLocation());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVisible(final boolean visible) {
        if (!visible) {
            ensureEditAssetsAreNotVisible();
        }

        super.setVisible(visible);
    }

    TransientEditAssetGroup getEditAssetGroup() {
        return m_editAssetGroup;
    }

    private void paintBackground(final PaintEvent pe) {
        final Point size = getSize();
        final GC gc = pe.gc;

        gc.setBackground(BACKGROUND_COLOR);
        gc.fillRoundRectangle(0, 0, (size.x - 1), (size.y - 1), ROUNDED_RADIUS, ROUNDED_RADIUS);
        gc.setForeground(BORDER_COLOR);
        gc.drawRoundRectangle(1, 1, (size.x - 2), (size.y - 2), ROUNDED_RADIUS, ROUNDED_RADIUS);
    }
}
