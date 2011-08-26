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

package org.knime.workbench.explorer.view.dnd;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystemUtils;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ContentDelegator;
import org.knime.workbench.explorer.view.actions.ExplorerAction;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class ExplorerDragListener implements DragSourceListener {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ExplorerDragListener.class);

    private final TreeViewer m_viewer;

    private LinkedList<AbstractExplorerFileStore> m_lockedFlows;

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

     // find affected workflows
        List<AbstractExplorerFileStore> affectedFlows
                = ExplorerAction.getContainedWorkflows(
                        DragAndDropUtils.getExplorerFileStores(selection));
        if (ExplorerFileSystemUtils.hasOpenWorkflows(affectedFlows)) {
            event.doit = false;
            MessageBox mb =
                new MessageBox(Display.getCurrent().getActiveShell(),
                        SWT.ICON_ERROR | SWT.OK);
            mb.setText("Can't perform operation");
            mb.setMessage("At least one of the workflows affected by the "
                    + "dragging is still open in the editor and has to be "
                    + "closed.");
            mb.open();
            return;
        }

        m_lockedFlows = new LinkedList<AbstractExplorerFileStore>();
        LinkedList<AbstractExplorerFileStore> unlockableFlows 
                = new LinkedList<AbstractExplorerFileStore>();
        ExplorerFileSystemUtils.lockWorkflows(affectedFlows, unlockableFlows, 
                m_lockedFlows);
        // unlock flows locked in here
        ExplorerFileSystemUtils.unlockWorkflows(m_lockedFlows);
        if (!unlockableFlows.isEmpty()) {
            event.doit = false;
            StringBuilder msg = new StringBuilder("Dragging canceled. "
                    + "At least one of the workflows affected by the "
                    + "dragging is in use by another user/instance:\n");
            for (AbstractExplorerFileStore lockedFlow : unlockableFlows) {
                msg.append("\t" + lockedFlow.getMountIDWithFullPath() + "\n");
            }
            LOGGER.warn(msg);
            MessageBox mb =
                new MessageBox(Display.getCurrent().getActiveShell(),
                        SWT.ICON_ERROR | SWT.OK);
            mb.setText("Can't perform operation");
            mb.setMessage("At least one of the workflows affected by the "
                    + "dragging is in use by another user/instance.\n"
                    + "(See log file for details.)");
            mb.open();

            return;
        }

        Map<AbstractContentProvider, List<AbstractExplorerFileStore>> 
                providers = DragAndDropUtils.getProviderMap(selection);
        if (providers == null) {
            // do not allow to drag whole mount points
            LOGGER.warn("Dragging cancelled. Mount points cannot be "
                    + "dragged.");
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
        LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
        try {
        /* TODO delegate drag finished to content provider */
        if (DND.DROP_MOVE == event.detail && event.doit) {
            LOGGER.debug("Removing source file(s) after successful drop.");
            IStructuredSelection selections = (IStructuredSelection)
                    transfer.getSelection();
            List<AbstractExplorerFileStore> fileStores = DragAndDropUtils
                    .getExplorerFileStores(selections);
            for (AbstractExplorerFileStore fs : fileStores) {
                try {
                    if (!fs.fetchInfo().exists()) {
                        continue;
                    }
                    fs.delete(EFS.NONE, null);
                } catch (CoreException e) {
                    String msg = "Could not move file \"" + fs.getFullName()
                            + "\". Source file could not be deleted.";
                    throw new RuntimeException(msg, e);
                }
                m_viewer.refresh(ContentDelegator.getTreeObjectFor(
                        fs.getParent()));
            }
            Iterator iterator = selections.iterator();
            while (iterator.hasNext()) {
                DragAndDropUtils.refreshResource(iterator.next());
//                m_viewer.refresh(fs.getParent());
            }
        }
        } finally {
            transfer.setSelection(null);
        }
    }

}
