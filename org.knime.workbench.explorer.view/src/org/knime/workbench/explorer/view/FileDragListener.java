/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2011
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * History
 *   Apr 28, 2011 (morent): created
 */

package org.knime.workbench.explorer.view;

import java.util.List;
import java.util.Map;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.filesystem.ExplorerFileStore;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class FileDragListener implements DragSourceListener {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(FileDragListener.class);

    private final TreeViewer m_viewer;

    /**
     * @param viewer the viewer to which this drag support has been added.
     */
    public FileDragListener(final TreeViewer viewer) {
        m_viewer = viewer;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void dragStart(final DragSourceEvent event) {
        LOGGER.debug("dragStart with event: " + event);
        IStructuredSelection selection =
                (IStructuredSelection)m_viewer.getSelection();
        Map<AbstractContentProvider, List<ExplorerFileStore>> providers =
                DragAndDropUtils.getProviderMap(selection);
        if (providers == null) {
            // do not allow to drag whole mount points
            LOGGER.warn("Dragging cancelled. Mount points cannot be "
                    + "dragged.");
            event.doit = false;
        } else {
            // delegate the evaluation to the content providers
            for (Map.Entry<AbstractContentProvider, List<ExplorerFileStore>>
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
            LOGGER.debug("dragSetData to selection:" + selection);
        }
        // if (FileTransfer.getInstance().isSupportedType(event.dataType)) {
        // List<String> drags = new ArrayList<String>();
        // IStructuredSelection selection =
        // (IStructuredSelection)m_viewer.getSelection();
        // Iterator<Object> s = selection.iterator();
        // while (s.hasNext()) {
        // Object nextObject = s.next();
        // if (nextObject instanceof ContentObject) {
        // ContentObject content = (ContentObject)nextObject;
        // Object object = content.getObject();
        // drags.add()
        // }
        // }
        // event.data = drags.toArray();
        // }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dragFinished(final DragSourceEvent event) {
        LOGGER.debug("dragFinished of event: " + event);
        LocalSelectionTransfer.getTransfer().setSelection(null);
    }

}
