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
 *   May 23, 2011 (morent): created
 */

package org.knime.workbench.explorer.view.actions;

import java.io.File;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.knime.core.internal.ReferencedFile;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.MountPoint;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ContentDelegator;
import org.knime.workbench.explorer.view.ExplorerView;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.navigator.WorkflowEditorAdapter;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class SynchronizeExplorerViewAction extends ExplorerAction {
    private static final String TOOLTIP
            = "Selects the workflow displayed in the active editor";

    private static final ImageDescriptor IMG
            = KNIMEUIPlugin.imageDescriptorFromPlugin(
                    KNIMEUIPlugin.PLUGIN_ID, "icons/sync.png");

    private final ContentDelegator m_delegator;

    /**
     *
     * @param viewer the viewer this action is attached to
     * @param delegator the content delegator of the view
     */
    public SynchronizeExplorerViewAction(final ExplorerView viewer,
            final ContentDelegator delegator) {
        super(viewer, "Synchronize...");
        m_delegator = delegator;
        setToolTipText(TOOLTIP);
        setImageDescriptor(IMG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return "org.knime.workbench.explorer.view.action.synchronize-view";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        // that's the local file to find in a content provider
        File wfDir;

        try {
            IEditorPart activeEditor =
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                            .getActivePage().getActiveEditor();
            Object adapter =
                    activeEditor.getAdapter(WorkflowEditorAdapter.class);
            if (adapter == null) {
                // not a workflow editor
                return;
            }
            ReferencedFile wfFileRef =
                    ((WorkflowEditorAdapter)adapter).getWorkflowManager()
                            .getWorkingDir();
            if (wfFileRef == null) {
                // not saved yet
                return;
            }
            wfDir = wfFileRef.getFile();
        } catch (Throwable t) {
            // if anything is null or fails: don't sync
            return;
        }

        Set<String> mountedIds = m_delegator.getMountedIds();
        for (String id : mountedIds) {
            MountPoint mountPoint = ExplorerMountTable.getMountPoint(id);
            AbstractExplorerFileStore root
                    = mountPoint.getProvider().getFileStore("/");
            final File localRoot;
            try {
                localRoot = root.toLocalFile();
            } catch (CoreException e) {
                // no corresponding local file
                continue;
            }
            if (localRoot == null) {
                // no corresponding local file
                continue;
            }
            String relPath = AbstractContentProvider.getRelativePath(
                    wfDir, localRoot);
            if (relPath == null) {
                // got the wrong content provider
                continue;
            }

            AbstractExplorerFileStore store =
                    mountPoint.getProvider().getFileStore(relPath);
            getViewer().setSelection(new StructuredSelection(ContentDelegator
                    .getTreeObjectFor(store)), true);
            return;
        }
    }
}
