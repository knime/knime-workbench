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
 */
package org.knime.workbench.editor2.editparts;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.Request;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.workflow.Annotation;
import org.knime.core.node.workflow.NodeAnnotation;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.NodeUIInformationEvent;
import org.knime.workbench.editor2.AnnotationUtilities;
import org.knime.workbench.editor2.WorkflowSelectionDragEditPartsTracker;
import org.knime.workbench.editor2.figures.NodeAnnotationFigure;
import org.knime.workbench.editor2.figures.NodeContainerFigure;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * @author Peter Ohl, KNIME AG, Zurich, Switzerland
 */
public class NodeAnnotationEditPart extends AnnotationEditPart {

    /**
     * font actually used to compute the bounds of the annotation figure.
     */
    private Font m_lastDefaultFont = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public void nodeUIInformationChanged(final NodeUIInformationEvent evt) {
        Display.getDefault().asyncExec(() -> {
            final WorkflowRootEditPart parent = (WorkflowRootEditPart)getParent();
            final NodeAnnotationFigure annoFig = (NodeAnnotationFigure)getFigure();
            annoFig.computeDisplay();

            final NodeAnnotation anno = (NodeAnnotation)getModel();
            // node annotation ignores its x/y ui info and hooks itself to its node
            final NodeID nodeID = anno.getNodeID();
            if (nodeID == null) {
                // may happen if the node is disposed before this runnable is executed
                return;
            }
            int x = anno.getX();
            int y = anno.getY();
            int w = anno.getWidth();
            int h = anno.getHeight();
            boolean update = false; // update only if anno has no ui info
            boolean isDirty = false; // check if the workflow is already dirty
            final NodeUIInformation nodeUI = parent.getWorkflowManager().getNodeContainer(nodeID).getUIInformation();
            if ((w <= 0) || (h <= 0)) {
                /* this code can be removed (but not for a bug fix release) as this
                 * method is called at least twice during activation and the 2nd
                 * invocation has valid bounds. To reproduce: create flow w/ v2.4,
                 * load in v2.5+ and set a break point
                 *
                 * TODO: remove if block + don't save bounds in node annotation
                 * (always computed automatically)
                 */
                update = true;
                // if the annotation has no width, make it as wide as the node
                if ((nodeUI != null) && (nodeUI.getBounds()[2] > 0)) {
                    // pre2.5 flows used the node width as label width
                    w = nodeUI.getBounds()[2];
                }
                // make it at least wide enough to hold "Node 9999xxxxxxxxx"
                w = Math.max(w, getNodeAnnotationMinWidth());
                h = getNodeAnnotationMinHeight();
            } else {
                // recalculate the dimension according to the default font (which
                // could change through the pref page or on different OS)
                final Font currentDefaultFont = AnnotationUtilities.getNodeAnnotationDefaultFont();
                if (!currentDefaultFont.equals(m_lastDefaultFont)) {
                    final Dimension textBounds = annoFig.getPreferredSize();
                    h = Math.max(textBounds.height, getNodeAnnotationMinHeight());
                    w = Math.max(textBounds.width, getNodeAnnotationMinWidth());
                    m_lastDefaultFont = currentDefaultFont;
                }
            }
            if (nodeUI != null) {
                final NodeContainerEditPart nodePart =
                    (NodeContainerEditPart)getViewer().getEditPartRegistry().get(parent.getWorkflowManager().getNodeContainer(nodeID));
                final Point offset;
                final int nodeHeight;
                final int symbFigWidth;
                if (nodePart != null) {
                    final NodeContainerFigure ncf = (NodeContainerFigure)nodePart.getFigure();
                    offset = ncf.getOffsetToRefPoint(nodeUI);
                    nodeHeight = ncf.getPreferredSize().height;
                    symbFigWidth = ncf.getSymbolFigure().getPreferredSize().width;
                    isDirty = nodePart.getNodeContainer().isDirty();
                } else {
                    offset = new Point(65, 35);
                    nodeHeight = NodeContainerFigure.HEIGHT;
                    symbFigWidth = 32;
                }
                final int[] nodeBounds = nodeUI.getBounds();
                final int mid = nodeBounds[0] + (symbFigWidth / 2);
                x = mid - (w / 2);
                y = nodeBounds[1] + nodeHeight + 1 - offset.y;
                update = true;
            }
            if (update) {
                if (isDirty) {
                    /* If the workflow is already dirty we update the dimensions with notify to ensure that the new
                     * dimensions are synched correctly (AP-11150). In case it is not dirty, which could happen when we
                     * update the annotation and the dimension is set prior to altering the text, then the change of the
                     * text triggers the sync, thus the possible race condition caused by async exec is no problem. */
                    anno.setDimension(x, y, w, h);
                } else {
                    anno.setDimensionNoNotify(x, y, w, h);
                }
            }
            parent.setLayoutConstraint(NodeAnnotationEditPart.this, annoFig, new Rectangle(x, y, w, h));
            refreshVisuals();
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IFigure createFigure() {
        final Annotation anno = getModel();
        return new NodeAnnotationFigure(anno);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DragTracker getDragTracker(final Request request) {
        return new WorkflowSelectionDragEditPartsTracker(this);
    }

    /**
     * @return the minimum width of a node annotation.
     */
    public static int getNodeAnnotationMinWidth() {
        // make it at least wide enough to hold "Node 9999xxxxxxxxx"
        String prefix =
                KNIMEUIPlugin
                        .getDefault()
                        .getPreferenceStore()
                        .getString(PreferenceConstants.P_DEFAULT_NODE_LABEL);
        if (prefix == null || prefix.isEmpty()) {
            prefix = "Node";
        }
        final int minTextW = AnnotationUtilities.nodeAnnotationDefaultLineWidth(prefix + " 9999xxxxxxxxx");
        // but not less than the node default width
        return Math.max(minTextW, NodeContainerFigure.WIDTH);
    }

    /**
     * @return the minimum height for a node annotation
     */
    public static int getNodeAnnotationMinHeight() {
        return AnnotationUtilities.nodeAnnotationDefaultOneLineHeight();
    }
}
