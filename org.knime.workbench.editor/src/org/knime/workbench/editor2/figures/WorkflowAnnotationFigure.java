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
 *   17.09.2015 (ohl): created
 */
package org.knime.workbench.editor2.figures;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw2d.text.TextFlow;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.workflow.Annotation;
import org.knime.core.node.workflow.AnnotationData;
import org.knime.core.util.ColorUtilities;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.editor2.AnnotationUtilities;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.WorkflowEditorMode;
import org.knime.workbench.editor2.actions.ToggleEditorModeAction;

/**
 * @author ohl
 */
public class WorkflowAnnotationFigure extends NodeAnnotationFigure {
    private static final Image SWITCH_MODE_ICON = ImageRepository.getImage(SharedImages.AnnotationEditModeHover);

    private static final Rectangle MODE_ICON_BOUNDS;

    static {
        final ImageData d = SWITCH_MODE_ICON.getImageData();

        MODE_ICON_BOUNDS = new Rectangle(2, 2, d.width, d.height);
    }


    private final Label m_modeIcon;

    private final AtomicBoolean m_figureTriggeredToggle;

    private ToggleEditorModeAction m_toggleAction;

    private final ArrayList<Color> m_currentContentDisposableColors;

    /**
     * @param anno the annotation which serves as the model backing this figure
     */
    public WorkflowAnnotationFigure(final Annotation anno) {
        super(anno);

        m_currentContentDisposableColors = new ArrayList<>();

        m_modeIcon = new Label(SWITCH_MODE_ICON);
        m_modeIcon.setBounds(MODE_ICON_BOUNDS);
        m_modeIcon.setVisible(false); // visible only when mouse enters
        add(m_modeIcon);

        m_modeIcon.addMouseListener(new MouseListener() {
            @Override
            public void mousePressed(final MouseEvent me) {
                me.consume();
            }

            @Override
            public void mouseReleased(final MouseEvent me) {
                performToggleAction();
                showEditIcon(false);
            }

            @Override
            public void mouseDoubleClicked(final MouseEvent me) { }
         });

        m_figureTriggeredToggle = new AtomicBoolean(false);
    }

    private void performToggleAction() {
        if (m_toggleAction == null) {
            final WorkflowEditor we =
                (WorkflowEditor)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

            m_toggleAction = new ToggleEditorModeAction(we);
        }

        m_figureTriggeredToggle.set(true);
        m_toggleAction.runInSWT();
    }

    /**
     * @return whether the mode change icon held by this instance was the cause of the toggle action; once queried, this
     *         will return false until the next time this instances mode icon triggers a toggle action
     */
    public boolean getAndClearTriggeredToggleState() {
        return m_figureTriggeredToggle.getAndSet(false);
    }

    /**
     * @param flag if true, set the edit mode icon visible, else hidden if false
     */
    public void showEditIcon(final boolean flag) {
        m_modeIcon.setVisible(flag);
    }

    /**
     * @return The bounds of the icon representing edit mode change.
     */
    public Rectangle getEditIconBounds() {
        return m_modeIcon.getBounds();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void performPostDisplayComputation() {
        final ArrayList<Color> disposableColors = new ArrayList<>();
        // this has already been computed once in the super call .... TODO
        final boolean renderEnabled = determineRenderEnabledState();
        final AnnotationData data = m_annotation.getData();

        Color bg;
        if (renderEnabled) {
            bg = ColorUtilities.RGBintToColor(data.getBgColor());
            disposableColors.add(bg);
        } else {
            bg = ColorConstants.lightGray;
        }
        setBackgroundColor(bg);
        m_page.setBackgroundColor(bg);

        Color fg = AnnotationUtilities.getAnnotationDefaultForegroundColor();
        if (!renderEnabled) {
            fg = ColorUtilities.convertToGrayscale(fg, 32);
            disposableColors.add(fg);
        }
        setForegroundColor(fg);
        m_page.setForegroundColor(fg);

        // set border with specified annotation color
        if (data.getBorderSize() > 0) {
            Color borderColor = ColorUtilities.RGBintToColor(data.getBorderColor());
            if (!renderEnabled) {
                final Color grayscale = ColorUtilities.convertToGrayscale(borderColor, 32);
                // dispose of it ~ now
                m_currentContentDisposableColors.add(borderColor);
                borderColor = grayscale;
            }
            disposableColors.add(borderColor);
            m_page.setBorder(new LineBorder(borderColor, data.getBorderSize()));
        } else {
            m_page.setBorder(null);
        }

        Display.getCurrent().asyncExec(() -> {
            m_currentContentDisposableColors.stream().forEach((c) -> {
                c.dispose();
            });

            m_currentContentDisposableColors.clear();
            m_currentContentDisposableColors.addAll(disposableColors);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected TextFlow getDefaultStyledAnnotation(final String text, final Font f, final Color bg,
        final boolean enabled) {
        final WiderTextFlow unstyledText = new WiderTextFlow();
        final Color fg;
        if (enabled) {
            fg = AnnotationUtilities.getAnnotationDefaultForegroundColor();
        } else {
            fg = ColorConstants.lightGray;
        }
        unstyledText.setForegroundColor(fg);
        unstyledText.setBackgroundColor(bg);
        unstyledText.setFont(f);
        unstyledText.setText(text);
        return unstyledText;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void workflowEditorModeWasSet(final WorkflowEditorMode newMode) {
        if (WorkflowEditorMode.ANNOTATION_EDIT.equals(newMode)) {
            showEditIcon(false);
        }

        super.workflowEditorModeWasSet(newMode);
    }
}
