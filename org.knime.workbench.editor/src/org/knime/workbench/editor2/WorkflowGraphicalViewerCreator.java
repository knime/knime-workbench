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
 * -------------------------------------------------------------------
 *
 * History
 *   25.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorSite;
import org.knime.core.node.util.CheckUtils;
import org.knime.workbench.editor2.viewport.ViewportPinningGraphicalViewer;

/**
 * Helper class that creates the <code>GraphicalViewer</code> that is hosted inside the editor. This creates the root
 * edit part and the <code>NodeTemplateDropTargetListener</code> that is responsible for dropping
 * <code>NodeTemplates</code> into the viewer. (which get converted into <code>NodeContainer</code> objects.)
 *
 * TODO loki sez: we should consider deprecating this class and moving the createViewer(Composite) code
 *          elsewhere; the usage of this class within the codebase appears to be restricted to one place in
 *          the WorkflowEditor in which it is instantiated and dumped.
 *
 * @author Florian Georg, University of Konstanz
 */
public class WorkflowGraphicalViewerCreator {
    /** the viewer. * */
    private GraphicalViewer m_viewer;

    /** the editor's action registry. */
    private final ActionRegistry m_actionRegistry;

    private final IEditorSite m_editorSite;

    /**
     *
     * @param editorSite Current editor site
     * @param actionRegistry The action registry to use
     */
    public WorkflowGraphicalViewerCreator(final IEditorSite editorSite, final ActionRegistry actionRegistry) {
        m_editorSite = CheckUtils.checkArgumentNotNull(editorSite);
        m_actionRegistry = actionRegistry;
    }

    /**
     * Creates a new <code>Viewer</code>, configures, registers and initializes it.
     *
     * @param parent the parent composite
     */
    public void createGraphicalViewer(final Composite parent) {
        m_viewer = createViewer(parent);
    }

    /**
     * Creates the viewer control, and connect it to a root edit part Additionally the viewer gets the edit part factory
     * and a drop-listener.
     *
     * @param parent Parent composite
     * @return The viewer
     */
    protected ViewportPinningGraphicalViewer createViewer(final Composite parent) {
        final ViewportPinningGraphicalViewer viewer = new ViewportPinningGraphicalViewer();

        viewer.createControl(parent);

        // configure the m_viewer
        viewer.getControl().setBackground(ColorConstants.white);
        final ScalableFreeformRootEditPart part = new ConnectionSelectingScalableFreeformRootEditPart();
        viewer.setRootEditPart(part);
        viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer));

        // Add drop listeners
        viewer.addDropTargetListener(new NodeDropTargetListener(viewer));
        viewer.addDropTargetListener(new MetaNodeDropTargetListener(viewer));
        viewer.addDropTargetListener(new WorkflowEditorFileDropTargetListener(viewer));
        viewer.addDropTargetListener(new WorkflowEditorSelectionDropListener(viewer));
        viewer.addDropTargetListener(new WorkflowEditorURLDropTargetListener(viewer));

        // DO NOT Add drag listener
        /* Don't add any drag listener here. Processing of drag events seems
         * to influence GEF events (resize, node move, create connection).
         * See bug 2844 for details (linux only) */
        final MetaNodeTemplateDropTargetListener metaNodeTemplateDropListener =
            new MetaNodeTemplateDropTargetListener(viewer);
        viewer.addDropTargetListener(metaNodeTemplateDropListener);
        // configure context menu
        viewer.setContextMenu(new WorkflowContextMenuProvider(m_actionRegistry, viewer));

        m_editorSite.registerContextMenu(viewer.getContextMenu(), viewer, false);
        // set the factory that is able to create the edit parts to be
        // used in the viewer
        viewer.setEditPartFactory(new WorkflowEditPartFactory());

        return viewer;
    }

    /**
     * @return Returns the m_viewer.
     */
    public GraphicalViewer getViewer() {
        return m_viewer;
    }
}
