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

package org.knime.workbench.explorer.view.dnd;

import java.util.List;
import java.util.Map;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;

/**
 *
 * @author Dominik Morent, KNIME AG, Zurich, Switzerland
 *
 */
public class ExplorerDragListener implements DragSourceListener {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ExplorerDragListener.class);

    private final TreeViewer m_viewer;

    /**
     * @param viewer the viewer to which this drag support has been added.
     */
    public ExplorerDragListener(final TreeViewer viewer) {
        m_viewer = viewer;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void dragStart(final DragSourceEvent event) {
        IStructuredSelection selection =
                (IStructuredSelection)m_viewer.getSelection();


        Map<AbstractContentProvider, List<AbstractExplorerFileStore>>
                providers = DragAndDropUtils.getProviderMap(selection);
        if (providers == null) {
            // do not allow to drag whole mount points
            LOGGER.warn("Mount points cannot be dragged.");
            event.doit = false;
        } else {
            // delegate the evaluation to the content providers
            for (Map.Entry<AbstractContentProvider,
                    List<AbstractExplorerFileStore>>
                    entry : providers.entrySet()) {
                AbstractContentProvider provider = entry.getKey();
                if (!provider.dragStart(entry.getValue())) {
                    // do not start dragging if one content provider rejects
                    event.doit = false;
                    LOGGER.debug("Content provider \"" + provider.getMountID()
                            + "\" canceled dragging.");
                    return;
                }
            }
            event.doit = true;
            LocalSelectionTransfer.getTransfer().setSelection(selection);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dragSetData(final DragSourceEvent event) {
        if (LocalSelectionTransfer.getTransfer()
                .isSupportedType(event.dataType)) {
            ISelection selection =
                    LocalSelectionTransfer.getTransfer().getSelection();
            event.data = selection;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dragFinished(final DragSourceEvent event) {
        /* drop performer are supposed to delete the source in case of a 
         * drag-move operation (for performance reasons). Therefore no action
         * is required here. */
        LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
        transfer.setSelection(null);
    }

}
