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
 *   Mar 28, 2019 (loki): created
 */
package org.knime.workbench.editor2;

import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.TextUtilities;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.RGB;
import org.jsoup.Jsoup;
import org.knime.core.node.workflow.Annotation;
import org.knime.core.node.workflow.AnnotationData;
import org.knime.core.node.workflow.NodeAnnotation;
import org.knime.core.node.workflow.WorkflowAnnotation;
import org.knime.core.node.workflow.WorkflowAnnotationID;
import org.knime.core.util.ColorUtilities;
import org.knime.workbench.editor2.editparts.AnnotationEditPart;
import org.knime.workbench.editor2.editparts.FontStore;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * A class of utilities to operate on annotation-related objects.
 *
 * TODO this package is becoming a catch all; consider moving some classes (like this) into a <code>.util</code>
 * sub-package.
 *
 * @author loki der quaeler
 */
public class AnnotationUtilities {
    private static final Color DEFAULT_FG = ColorConstants.black;

    /** White (since annotations have borders the default color is white) */
    private static final Color DEFAULT_BG_WORKFLOW = new Color(null, 255, 255, 255);

    /** default Color of the border for workflow annotations. Medium yellow-orange. */
    private static final Color DEFAULT_BORDER_WORKFLOW = new Color(null, 255, 216, 0);

    /** White. */
    private static final Color DEFAULT_BG_NODE = new Color(null, 255, 255, 255);

    /**
     * If no foreground color is set, this one should be used.
     *
     * @return the default font color of annotation figures
     */
    public static Color getAnnotationDefaultForegroundColor() {
        // TODO: read it from a pref page...
        return DEFAULT_FG;
    }

    /**
     * @return the height of one line in default font (doesn't honor label size setting in preference page)
     */
    public static int workflowAnnotationDefaultOneLineHeight() {
        return TextUtilities.INSTANCE.getStringExtents("Agq|_ÊZ", getWorkflowAnnotationDefaultFont()).height;
    }

    /**
     * @return the height of one line in default font for node annotations - uses the preference page setting to
     *         determine the font size
     */
    public static int nodeAnnotationDefaultOneLineHeight() {
        final Font font = getNodeAnnotationDefaultFont();
        return TextUtilities.INSTANCE.getStringExtents("Agq|_ÊZ", font).height;
    }

    /**
     * @param text to test
     * @return the width of the specified text with the workflow annotation default font
     */
    public static int workflowAnnotationDefaultLineWidth(final String text) {
        final Font font = getWorkflowAnnotationDefaultFont();
        return TextUtilities.INSTANCE.getStringExtents(text, font).width;
    }

    /**
     * @param text to test
     * @return the width of the specified text with the workflow annotation default font
     */
    public static int nodeAnnotationDefaultLineWidth(final String text) {
        final Font font = getNodeAnnotationDefaultFont();
        return TextUtilities.INSTANCE.getStringExtents(text, font).width;
    }

    /**
     * If no background color is set, this one should be used for workflow annotations.
     *
     * @return the default background color of workflow annotation figures
     */
    public static Color getWorkflowAnnotationDefaultBackgroundColor() {
        // TODO: read it from a pref page...
        return DEFAULT_BG_WORKFLOW;
    }

    /**
     * If no background color is set, this one should be used for node annotations.
     *
     * @return the default background color of node annotation figures
     */
    public static Color getNodeAnnotationDefaultBackgroundColor() {
        // TODO: read it from a pref page...
        return DEFAULT_BG_NODE;
    }

    /**
     * @return the default color of the workflow annotation border.
     */
    public static Color getAnnotationDefaultBorderColor() {
        return DEFAULT_BORDER_WORKFLOW;
    }

    /**
     * @return the value set in the preference page for the default border width.
     */
    public static int getAnnotationDefaultBorderSizePreferenceValue() {
        final IPreferenceStore store = KNIMEUIPlugin.getDefault().getPreferenceStore();
        return store.getInt(PreferenceConstants.P_ANNOTATION_BORDER_SIZE);
    }

    /**
     * If no font is set, this one should be used for workflow annotations.
     *
     * @return the default font for workflow annotation
     */
    public static Font getWorkflowAnnotationDefaultFont() {
        // its the default font.
        return FontStore.INSTANCE.getDefaultFont(FontStore.getFontSizeFromKNIMEPrefPage());
    }

    /**
     * If no font is set, this one should be used for workflow annotations.
     *
     * @param size size
     * @return the default font for workflow annotation
     */
    public static Font getWorkflowAnnotationDefaultFont(final int size) {
        // its the default font.
        return FontStore.INSTANCE.getDefaultFont(size);
    }

    /**
     * If no font is set, this one should be used for node annotations. page for node labels.
     *
     * @return the default font for node annotation
     */
    public static Font getNodeAnnotationDefaultFont() {
        return FontStore.INSTANCE.getDefaultFont(FontStore.getFontSizeFromKNIMEPrefPage());
    }

    /**
     * Returns the text contained in the annotation or the default text if the argument annotation is a default node
     * annotation ("Node 1", "Node 2", ...).
     *
     * @param t the annotation, not null.
     * @return the above text.
     */
    public static String getAnnotationText(final Annotation t) {
        if (!isDefaultNodeAnnotation(t)) {
            return getPlainAnnotationText(t);
        }
        if (((NodeAnnotation)t).getNodeID() == null) {
            return "";
        }
        final int id = ((NodeAnnotation)t).getNodeID().getIndex();
        final String prefix =
            KNIMEUIPlugin.getDefault().getPreferenceStore().getString(PreferenceConstants.P_DEFAULT_NODE_LABEL);
        final String text;
        if ((prefix == null) || prefix.isEmpty()) {
            text = "";
        } else {
            text = prefix + " " + id;
        }
        return text;
    }

    /**
     * @param t an Annotation
     * @return true if t is an instance of NodeAnnotation and <code>isDefault()</code> returns true.
     */
    public static boolean isDefaultNodeAnnotation(final Annotation t) {
        return (t instanceof NodeAnnotation) && (((NodeAnnotation)t).getData()).isDefault();
    }

    /**
     * @param t annotation data to be converted to style ranges
     * @param defaultFont the default font for text
     * @return an array of StyleRange instances
     */
    public static StyleRange[] toSWTStyleRanges(final AnnotationData t, final Font defaultFont) {
        final AnnotationData.StyleRange[] knimeStyleRanges = t.getStyleRanges();
        final ArrayList<StyleRange> swtStyleRange = new ArrayList<StyleRange>(knimeStyleRanges.length);
        for (final AnnotationData.StyleRange knimeSR : knimeStyleRanges) {
            final StyleRange swtStyle = new StyleRange();
            swtStyle.font = FontStore.INSTANCE.getAnnotationFont(knimeSR, defaultFont);
            if (knimeSR.getFgColor() >= 0) {
                int rgb = knimeSR.getFgColor();
                RGB rgbObj = ColorUtilities.RGBintToRGBObj(rgb);
                swtStyle.foreground = new Color(null, rgbObj);
            }
            swtStyle.start = knimeSR.getStart();
            swtStyle.length = knimeSR.getLength();
            swtStyleRange.add(swtStyle);
        }
        return swtStyleRange.toArray(new StyleRange[swtStyleRange.size()]);
    }

    /**
     * @param s the component with the styled text to convert.
     * @return an instance of AnnotationData embodying the styled text.
     */
    public static AnnotationData toAnnotationData(final StyledText s) {
        final AnnotationData result = new AnnotationData();
        result.setText(s.getText());
        result.setBgColor(ColorUtilities.colorToRGBint(s.getBackground()));
        result.setBorderColor(ColorUtilities.colorToRGBint(s.getMarginColor()));
        // annotations have the same margin top/left/right/bottom, however getLeftMargin may return a different value.
        result.setBorderSize(s.getRightMargin());
        result.setDefaultFontSize(s.getFont().getFontData()[0].getHeight());
        result.setAlignment(AnnotationData.getTextAlignmentForSWTAlignment(s.getAlignment()));

        final StyleRange[] swtStyleRange = s.getStyleRanges();
        final ArrayList<AnnotationData.StyleRange> wfStyleRanges = new ArrayList<>(swtStyleRange.length);
        for (final StyleRange sr : swtStyleRange) {
            if (sr.isUnstyled()) {
                continue;
            }
            final AnnotationData.StyleRange waSr = new AnnotationData.StyleRange();
            final Color fg = sr.foreground;
            if (fg != null) {
                int rgb = ColorUtilities.colorToRGBint(fg);
                waSr.setFgColor(rgb);
            }
            FontStore.saveAnnotationFontToStyleRange(waSr, sr.font);
            waSr.setStart(sr.start);
            waSr.setLength(sr.length);
            wfStyleRanges.add(waSr);
        }
        result.setStyleRanges(wfStyleRanges.toArray(new AnnotationData.StyleRange[wfStyleRanges.size()]));
        return result;
    }

    /**
     * Extract the WorkflowAnnotation models from the argument list. It will ignore NodeAnnotations (which have the same
     * edit part).
     *
     * @param annoParts the selected annotation parts
     * @return The IDs of the workflow annotation models (possibly fewer than selected edit parts!!!)
     */
    public static WorkflowAnnotationID[] extractWorkflowAnnotationIDs(final AnnotationEditPart[] annoParts) {
        return Arrays.stream(annoParts) //
            .map(AnnotationEditPart::getModel) //
            .filter(WorkflowAnnotation.class::isInstance) //
            .map(WorkflowAnnotation.class::cast) //
            .map(WorkflowAnnotation::getID) //
            .toArray(WorkflowAnnotationID[]::new);
    }

    /**
     * Strips HTML tags from annotation text.
     *
     * @param annotation The annotation
     * @return The plain text
     */
    public static String getPlainAnnotationText(final Annotation annotation) {
        if (annotation.getVersion() < AnnotationData.VERSION_20230412) {
            return annotation.getText();
        } else {
            return Jsoup.parse(annotation.getText()).text();
        }
    }
}
