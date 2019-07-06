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
 *   2010 10 11 (ohl): created
 */
package org.knime.workbench.editor2.editparts;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.SelectionManager;
import org.eclipse.gef.requests.LocationRequest;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.workflow.Annotation;
import org.knime.core.node.workflow.NodeUIInformationEvent;
import org.knime.core.node.workflow.NodeUIInformationListener;
import org.knime.core.node.workflow.WorkflowAnnotation;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.workbench.editor2.AnnotationEditExitEnabler;
import org.knime.workbench.editor2.EditorModeParticipant;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.WorkflowEditorMode;
import org.knime.workbench.editor2.WorkflowSelectionDragEditPartsTracker;
import org.knime.workbench.editor2.WorkflowSelectionTool;
import org.knime.workbench.editor2.directannotationedit.AnnotationEditManager;
import org.knime.workbench.editor2.directannotationedit.AnnotationEditPolicy;
import org.knime.workbench.editor2.directannotationedit.StyledTextEditorLocator;
import org.knime.workbench.editor2.figures.NodeAnnotationFigure;
import org.knime.workbench.editor2.figures.WorkflowAnnotationFigure;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * TODO This architecture is an acid trip. This class creates figures of B, and is subclassed by X; X creates figures of
 * A which is a superclass of B. A's computeDisplay() method knows about salient facets of B... trippy - not good trippy.
 *
 * @author ohl, KNIME AG, Zurich, Switzerland
 */
public class AnnotationEditPart extends AbstractWorkflowEditPart
    implements EditorModeParticipant, IPropertyChangeListener, NodeUIInformationListener {
    private AnnotationEditManager m_directEditManager;

    private WorkflowEditorMode m_currentEditorMode = WorkflowEditor.INITIAL_EDITOR_MODE;

    /** {@inheritDoc} */
    @Override
    public Annotation getModel() {
        return (Annotation)super.getModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IFigure createFigure() {
        final Annotation anno = getModel();
        final WorkflowAnnotationFigure annotationFigure = new WorkflowAnnotationFigure(anno);
        if (anno instanceof WorkflowAnnotation) {
            annotationFigure.setBounds(new Rectangle(anno.getX(), anno.getY(), anno.getWidth(), anno.getHeight()));
        }
        return annotationFigure;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void activate() {
        super.activate();

        final IPreferenceStore store = KNIMEUIPlugin.getDefault().getPreferenceStore();
        store.addPropertyChangeListener(this);

        final Annotation anno = getModel();
        anno.addUIInformationListener(this);

        // update the ui info now
        nodeUIInformationChanged(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deactivate() {
        final IPreferenceStore store = KNIMEUIPlugin.getDefault().getPreferenceStore();
        store.removePropertyChangeListener(this);

        final Annotation anno = getModel();
        anno.removeUIInformationListener(this);

        super.deactivate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createEditPolicies() {
        // Installs the edit policy to directly edit the annotation in its
        // editpart (through the StyledTextEditor) after clicking it twice.
        installEditPolicy(EditPolicy.DIRECT_EDIT_ROLE, new AnnotationEditPolicy());
        installEditPolicy(EditPolicy.GRAPHICAL_NODE_ROLE, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void nodeUIInformationChanged(final NodeUIInformationEvent evt) {
        ((NodeAnnotationFigure)getFigure()).computeDisplay();

        final WorkflowRootEditPart parent = (WorkflowRootEditPart)getParent();
        final Annotation anno = getModel();
        final Rectangle constraint = new Rectangle(anno.getX(), anno.getY(), anno.getWidth(), anno.getHeight());
        parent.setLayoutConstraint(this, getFigure(), constraint);
        refreshVisuals();
        parent.refresh();
    }


    /** {@inheritDoc} */
    @Override
    public void propertyChange(final PropertyChangeEvent p) {
        if (p.getProperty().equals(PreferenceConstants.P_DEFAULT_NODE_LABEL)
                || p.getProperty().equals(PreferenceConstants.P_ANNOTATION_BORDER_SIZE)) {
            ((NodeAnnotationFigure)getFigure()).computeDisplay();
        }
    }

    @Override
    public void performRequest(final Request request) {
        if ((request.getType() == RequestConstants.REQ_OPEN)
            || (WorkflowEditorMode.ANNOTATION_EDIT.equals(m_currentEditorMode) && (getSelected() != SELECTED_NONE))) {
            // REQ_OPEN is caused by a double click on this edit part
            performEdit();
            // we ignore REQ_DIRECT_EDIT as we want to allow editing only after a double-click, or click on selected
        } else {
            super.performRequest(request);
        }
    }

    /**
     * Opens the editor to directly edit the annotation in place.
     */
    public void performEdit() {
        // Only allow the edit if we're in AE mode, or we're editing the node's name annotation
        if (WorkflowEditorMode.ANNOTATION_EDIT.equals(m_currentEditorMode)
            || (this instanceof NodeAnnotationEditPart)) {
            Display.getDefault().asyncExec(() -> {
                final EditPart parent = getParent();

                if (parent instanceof WorkflowRootEditPart) {
                    final WorkflowRootEditPart wkfRootEdit = (WorkflowRootEditPart)parent;
                    if (wkfRootEdit.getWorkflowManager().isWriteProtected()
                        || !Wrapper.wraps(wkfRootEdit.getWorkflowManager(), WorkflowManager.class)) {
                        return;
                    }
                }

                if (m_directEditManager == null) {
                    m_directEditManager =
                        new AnnotationEditManager(this, new StyledTextEditorLocator((NodeAnnotationFigure)getFigure()));
                }

                m_directEditManager.show();
            });
        }
    }

    private boolean figureIsForWorkflowAnnotation() {
        return getFigure() instanceof WorkflowAnnotationFigure;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showTargetFeedback(final Request request) {
        if (WorkflowEditorMode.NODE_EDIT.equals(m_currentEditorMode) && request.getType().equals(REQ_SELECTION)) {
            if (figureIsForWorkflowAnnotation()) {
                ((WorkflowAnnotationFigure)getFigure()).showEditIcon(true);
            }
        }

        super.showTargetFeedback(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void eraseTargetFeedback(final Request request) {
        if (WorkflowEditorMode.NODE_EDIT.equals(m_currentEditorMode) && request.getType().equals(REQ_SELECTION)) {
            if (figureIsForWorkflowAnnotation()) {
                ((WorkflowAnnotationFigure)getFigure()).showEditIcon(false);
            }
        }

        super.eraseTargetFeedback(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DragTracker getDragTracker(final Request request) {
        if (!WorkflowEditorMode.ANNOTATION_EDIT.equals(m_currentEditorMode)) {
            final Object o = request.getExtendedData().get(WorkflowSelectionTool.DRAG_START_LOCATION);

            if ((o instanceof Point) && ((WorkflowAnnotationFigure)getFigure()).getEditIconBounds().contains((Point)o)
                && figureIsForWorkflowAnnotation()) {
                return null;
            }
        }

        if (request instanceof LocationRequest) {
            Point location = ((LocationRequest)request).getLocation();

            if (AnnotationEditExitEnabler.annotationDragTrackerShouldVeto(location)) {
                return null;
            }
        }

        if (AnnotationEditManager.partShouldVetoSelection(this)) {
            return null;
        }

        return new WorkflowSelectionDragEditPartsTracker(this);
    }

    /**
     * {@inheritDoc}
     *
     * We don't want to be selected if:
     * . we're in Annotation Edit mode, but our figure is not an instance of WorkflowAnnotationFigure (because that sort
     * of annotation figure is semantically actually part of a node.)
     */
    @Override
    public EditPart getTargetEditPart(final Request request) {
        if (m_currentEditorMode.equals(WorkflowEditorMode.ANNOTATION_EDIT) && !figureIsForWorkflowAnnotation()) {
            return null;
        }

        return super.getTargetEditPart(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void workflowEditorModeWasSet(final WorkflowEditorMode newMode) {
        m_currentEditorMode = newMode;

        ((NodeAnnotationFigure)getFigure()).workflowEditorModeWasSet(newMode);

        if (WorkflowEditorMode.ANNOTATION_EDIT.equals(m_currentEditorMode) && figureIsForWorkflowAnnotation()
            && ((WorkflowAnnotationFigure)getFigure()).getAndClearTriggeredToggleState()) {
            final SelectionManager sm = getViewer().getSelectionManager();

            sm.appendSelection(this);
        }
    }
}
