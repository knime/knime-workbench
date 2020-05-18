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

package org.knime.workbench.explorer.view.actions;

import java.io.File;
import java.net.URI;
import java.util.Optional;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.MountPoint;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ContentDelegator;
import org.knime.workbench.explorer.view.ExplorerView;
import org.knime.workbench.ui.navigator.WorkflowEditorAdapter;

/**
 *
 * @author Dominik Morent, KNIME AG, Zurich, Switzerland
 *
 */
public class SynchronizeExplorerViewAction extends ExplorerAction {
    private static final String TOOLTIP
            = "Selects the workflow displayed in the active editor";

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
        setImageDescriptor(ImageRepository.getIconDescriptor(SharedImages.Synch));
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
        Optional<AbstractExplorerFileStore> fs = getFileStoreForCurrentWorkbenchSelection(m_delegator.getMountedIds());
        fs.ifPresent(f -> getViewer().setSelection(new StructuredSelection(ContentDelegator.getTreeObjectFor(f)), true));
    }

    /** Return the file store for the workflow currently selected/edited in the main workbench window or an empty
     * optional if that's not possible.
     * @param mountedIds List of mounted IDs.
     * @return That optional.
     * @noreference This method is not intended to be referenced by clients.
     */
    public static Optional<AbstractExplorerFileStore>
        getFileStoreForCurrentWorkbenchSelection(final Set<String> mountedIds) {
        IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (activeWorkbenchWindow == null) {
            return Optional.empty(); // no window open at all
        }
        IEditorPart activeEditor =
            activeWorkbenchWindow.getActivePage().getActiveEditor();
        if (activeEditor == null) {
            return Optional.empty(); // no editor open at all
        }
        IEditorPart rootEditor = findRootEditor(activeEditor);
        if (rootEditor == null) {
            // no workflow editor
            return Optional.empty();
        }


        AbstractExplorerFileStore fs = resolveViaFilestore(rootEditor);
        if (fs == null) {
            fs = resolveViaPath(activeEditor, mountedIds);
        }
        if (fs == null) {
            fs = resolveViaURI(activeEditor);
        }
        return Optional.ofNullable(fs);
    }

    private static AbstractExplorerFileStore resolveViaPath(final IEditorPart rootEditor, final Set<String> mountedIds) {
        WorkflowEditorAdapter adapter = rootEditor.getAdapter(WorkflowEditorAdapter.class);
        if (adapter == null) {
            return null;
        }
        WorkflowManager wm = adapter.getWorkflowManager();
        if (wm == null) {
            return null;
        }
        ReferencedFile wfFileRef = wm.getWorkingDir();
        if (wfFileRef == null) {
            return null;
        }
        File wfDir = wfFileRef.getFile();

        for (String id : mountedIds) {
            MountPoint mountPoint = ExplorerMountTable.getMountPoint(id);
            AbstractExplorerFileStore root = mountPoint.getProvider().getRootStore();
            try {
                File localRoot = root.toLocalFile();
                if (localRoot != null) {
                    String relPath = AbstractContentProvider.getRelativePath(wfDir, localRoot);
                    if (relPath != null) {
                        return mountPoint.getProvider().getFileStore(relPath);
                    }
                }
            } catch (CoreException e) {
                // no corresponding local file
            }
        }
        return null;
    }

    private static AbstractExplorerFileStore resolveViaFilestore(final IEditorPart rootEditor) {
        IEditorInput editorInput = rootEditor.getEditorInput();
        if (editorInput instanceof FileStoreEditorInput) {
            URI uri = ((FileStoreEditorInput) editorInput).getURI();
            if (uri.getPath().endsWith("/" + WorkflowPersistor.WORKFLOW_FILE)) {
                String newUri = uri.toString();
                newUri = newUri.substring(0, newUri.length() - WorkflowPersistor.WORKFLOW_FILE.length() - 1);
                uri = URI.create(newUri);
            }
            return ExplorerFileSystem.INSTANCE.getStore(uri);
        }
        return null;
    }

    private static AbstractExplorerFileStore resolveViaURI(final IEditorPart rootEditor) {
        IEditorInput editorInput = rootEditor.getEditorInput();
        if (editorInput instanceof IURIEditorInput) {
            URI uri = ((IURIEditorInput)editorInput).getURI();
            return ExplorerFileSystem.INSTANCE.getStore(uri);
        }
        return null;
    }

    private static IEditorPart findRootEditor(final IEditorPart editor) {
        WorkflowEditorAdapter wfAdapter = editor.getAdapter(WorkflowEditorAdapter.class);
        if (wfAdapter != null) {
            if (wfAdapter.getParentEditor() == null) {
                return editor;
            } else {
                return findRootEditor(wfAdapter.getParentEditor());
            }
        } else {
            return null;
        }
    }
}
