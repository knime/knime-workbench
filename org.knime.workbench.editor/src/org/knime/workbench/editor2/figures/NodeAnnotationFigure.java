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
 * ------------------------------------------------------------------------
 *
 * History
 *   2010 10 14 (ohl): created
 */
package org.knime.workbench.editor2.figures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.draw2d.BorderLayout;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw2d.text.BlockFlow;
import org.eclipse.draw2d.text.BlockFlowLayout;
import org.eclipse.draw2d.text.FlowPage;
import org.eclipse.draw2d.text.PageFlowLayout;
import org.eclipse.draw2d.text.TextFlow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.Annotation;
import org.knime.core.node.workflow.AnnotationData;
import org.knime.core.node.workflow.NodeAnnotation;
import org.knime.core.util.ColorUtilities;
import org.knime.workbench.editor2.AnnotationUtilities;
import org.knime.workbench.editor2.EditorModeParticipant;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.WorkflowEditorMode;
import org.knime.workbench.editor2.editparts.FontStore;

/**
 * @author ohl, KNIME AG, Zurich, Switzerland
 */
public class NodeAnnotationFigure extends Figure implements EditorModeParticipant {
    private static NodeLogger LOGGER = NodeLogger.getLogger(NodeAnnotationFigure.class);


    /**
     * The flow figure which we are wrapping.
     */
    protected final FlowPage m_page;

    /**
     * The annotation which we are rendering.
     */
    protected Annotation m_annotation;

    private final ArrayList<Color> m_disposableForegroundStyledTextColors;
    private int m_lastRevisionDisplayed;
    private boolean m_lastRenderEnableDisplay;

    /**
     * @param annotation the annotation to display
     */
    public NodeAnnotationFigure(final Annotation annotation) {
        setLayoutManager(new BorderLayout());

        final Color bg = AnnotationUtilities.getWorkflowAnnotationDefaultBackgroundColor();
        m_page = new FlowPage();
        m_page.setLayoutManager(new PageFlowLayout(m_page));
        m_page.setBackgroundColor(bg);

        m_disposableForegroundStyledTextColors = new ArrayList<>();
        m_annotation = annotation;

        m_lastRevisionDisplayed = Integer.MIN_VALUE;
        m_lastRenderEnableDisplay = false;

        add(m_page);
        setConstraint(m_page, BorderLayout.CENTER);
        setBackgroundColor(bg);
        computeDisplay();
    }

    /**
     * @return true if the annotation should be rendered as "enabled", false for "disabled"
     */
    protected boolean determineRenderEnabledState() {
        final boolean isNodeAnnotation = (m_annotation instanceof NodeAnnotation);
        final IWorkbenchWindow iww = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IEditorPart iep = null;

        if (iww != null) {
            final IWorkbenchPage iwp = iww.getActivePage();

            if (iwp != null) {
                iep = iwp.getActiveEditor();
            }
        }

        if (iep instanceof WorkflowEditor) {
            final WorkflowEditor we = (WorkflowEditor)iep;
            final WorkflowEditorMode wem =
                isNodeAnnotation ? WorkflowEditorMode.NODE_EDIT : WorkflowEditorMode.ANNOTATION_EDIT;

            return (wem.equals(we.getEditorMode()) || !isNodeAnnotation);
        }

        // Case 1: if we have a null active editor, it's because the app is still coming up, or the active page is being
        //          reset - so render us enabled for the time being because we should be consulted again later
        // Case 2: iep is not null, and not a WorkflowEditor, because the 'active editor' was still the Welcome Page
        //          while we are switching tabs to a workflow; in this case ensure the workflow annotations are
        //          rendered correctly
        return ((iep == null) || !isNodeAnnotation);
    }

    /**
     * This method creates a completely populated <code>BlockFlow</code> instance consisting of correctly styled
     * <code>TextFlow</code> instances. If the wrapped <code>Annotation</code> instance has not be revised since the
     * last time this method was invoked, this method will not bother recomputing anything.
     */
    public final void computeDisplay() {
        final boolean renderEnabled = determineRenderEnabledState();
        if ((m_annotation.getRevision() == m_lastRevisionDisplayed) && (m_lastRenderEnableDisplay == renderEnabled)) {
            return;
        }

        final boolean isNodeAnnotation = (m_annotation instanceof NodeAnnotation);

        final String text;

        final AnnotationData.StyleRange[] sr;
        if (AnnotationUtilities.isDefaultNodeAnnotation(m_annotation)) {
            text = AnnotationUtilities.getAnnotationText(m_annotation);
            sr = new AnnotationData.StyleRange[0];
        } else {
            text = m_annotation.getText();
            if (m_annotation.getStyleRanges() != null) {
                sr = Arrays.copyOf(m_annotation.getStyleRanges(), m_annotation.getStyleRanges().length);
            } else {
                sr = new AnnotationData.StyleRange[0];
            }
        }

        Arrays.sort(sr, (range1, range2) -> {
            if (range1.getStart() == range2.getStart()) {
                LOGGER.error("Ranges overlap");
                return 0;
            } else {
                return Integer.compare(range1.getStart(),range2.getStart());
            }
        });

        final Color previousBackgroundColor = getBackgroundColor();
        if ((previousBackgroundColor != null)
            && !AnnotationUtilities.getWorkflowAnnotationDefaultBackgroundColor().equals(previousBackgroundColor)) {
            previousBackgroundColor.dispose();
        }
        m_disposableForegroundStyledTextColors.stream().forEach((c) -> {
            c.dispose();
        });
        m_disposableForegroundStyledTextColors.clear();

        final Color bg = ColorUtilities.RGBintToColor(m_annotation.getBgColor());
        setBackgroundColor(bg);
        m_page.setBackgroundColor(bg);
        if (isNodeAnnotation
            && (AnnotationUtilities.getNodeAnnotationDefaultBackgroundColor().equals(bg) || !renderEnabled)) {
            setOpaque(false);
        } else {
            setOpaque(true);
        }
        int i = 0;
        final List<TextFlow> segments = new ArrayList<TextFlow>(sr.length);
        // in old flow annotations didn't store the font if system default was used. New annotations always store font
        // info. For backward compatibility use the system font if no font is specified here.
        final Font defaultFont;
        if (isNodeAnnotation) {
            defaultFont = AnnotationUtilities.getNodeAnnotationDefaultFont();
        } else if (m_annotation.getVersion() < AnnotationData.VERSION_20151012) {
            defaultFont = FontStore.INSTANCE.getSystemDefaultFont();
        } else if (m_annotation.getVersion() < AnnotationData.VERSION_20151123) {
            defaultFont = AnnotationUtilities.getWorkflowAnnotationDefaultFont();
        } else {
            if (m_annotation.getDefaultFontSize() < 0) {
                defaultFont = AnnotationUtilities.getWorkflowAnnotationDefaultFont();
            } else {
                defaultFont = AnnotationUtilities.getWorkflowAnnotationDefaultFont(m_annotation.getDefaultFontSize());
            }
        }
        for (final AnnotationData.StyleRange range : sr) {
            // create text from last range to beginning of this range
            if (i < range.getStart()) {
                final String unstyled = text.substring(i, range.getStart());

                segments.add(getDefaultStyledAnnotation(unstyled, defaultFont, bg, renderEnabled));

                i = range.getStart();
            }

            final String styled = text.substring(i, range.getStart() + range.getLength());
            segments.add(getStyledAnnotation(styled, range, bg, defaultFont, renderEnabled,
                m_disposableForegroundStyledTextColors));
            i = range.getStart() + range.getLength();
        }
        if (i < text.length()) {
            final String unstyled = text.substring(i, text.length());
            segments.add(getDefaultStyledAnnotation(unstyled, defaultFont, bg, renderEnabled));
        }

        final BlockFlow blockFlow = new BlockFlow();
        final BlockFlowLayout blockFlowLayout = new BlockFlowLayout(blockFlow);
        blockFlowLayout.setContinueOnSameLine(true);
        blockFlow.setLayoutManager(blockFlowLayout);

        final int position;
        switch (m_annotation.getAlignment()) {
            case CENTER:
                position = PositionConstants.CENTER;
                break;
            case RIGHT:
                position = PositionConstants.RIGHT;
                break;
            default:
                position = PositionConstants.LEFT;
        }
        blockFlow.setHorizontalAligment(position);
        blockFlow.setOrientation(SWT.LEFT_TO_RIGHT);
        blockFlow.setBackgroundColor(bg);
        for (final TextFlow textFlow : segments) {
            blockFlow.add(textFlow);
        }

        m_page.removeAll();
        m_page.add(blockFlow);
        m_page.setVisible(true);

        m_lastRevisionDisplayed = m_annotation.getRevision();
        m_lastRenderEnableDisplay = renderEnabled;

        performPostDisplayComputation();

        revalidate();
    }

    /**
     * Subclasses may override this to perform any additional display computation tasks prior to revalidate()
     *  being called on this instance.
     *
     * @see #computeDisplay()
     */
    protected void performPostDisplayComputation() { }

    /**
     * Subclasses should override this to return a <code>TextFlow</code> instance appropriately styled to their figure
     * type.
     *
     * @param text the text content of the flow
     * @param f the font to use
     * @param bg the background color to use
     * @param enabled whether the display should appear "enabled"
     * @return an instance of <code>TextFlow</code> embodying the attributes specified
     */
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
    public void setBounds(final Rectangle rect) {
        super.setBounds(rect);
        m_page.invalidate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void workflowEditorModeWasSet(final WorkflowEditorMode newMode) {
        computeDisplay();
    }

    private static TextFlow getStyledAnnotation(final String text, final AnnotationData.StyleRange style,
        final Color bg, final Font defaultFont, final boolean enabled, final List<Color> disposableForegroundColors) {
        final Font styledFont = FontStore.INSTANCE.getAnnotationFont(style, defaultFont);
        final WiderTextFlow styledText = new WiderTextFlow(text);
        Color fg;
        if (enabled) {
            fg = ColorUtilities.RGBintToColor(style.getFgColor());
            disposableForegroundColors.add(fg);
        } else {
            fg = ColorConstants.lightGray;
        }
        styledText.setFont(styledFont);
        styledText.setForegroundColor(fg);
        styledText.setBackgroundColor(bg);
        return styledText;
    }
}
