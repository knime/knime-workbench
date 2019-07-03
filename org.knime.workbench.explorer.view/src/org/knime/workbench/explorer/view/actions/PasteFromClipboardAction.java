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

import static org.knime.core.util.URIUtil.createEncodedURI;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;
import org.knime.core.util.pathresolve.ResolverUtil;
import org.knime.workbench.core.imports.EntityImport;
import org.knime.workbench.core.imports.RepoObjectImport;
import org.knime.workbench.core.imports.RepoObjectImport.RepoObjectType;
import org.knime.workbench.core.imports.URIImporter;
import org.knime.workbench.core.imports.URIImporterFinder;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileInfo;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystemUtils;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ContentDelegator;
import org.knime.workbench.explorer.view.ExplorerFileStoreTransfer;
import org.knime.workbench.explorer.view.ExplorerView;

public class PasteFromClipboardAction extends AbstractCopyMoveAction {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            PasteFromClipboardAction.class);
//    private AbstractExplorerFileStore RemoteExplorerFileStore;
    /**
     * @param viewer the viewer
     */
    public PasteFromClipboardAction(final ExplorerView viewer) {
        super(viewer, "Paste", false);
        // Disable by default to make sure an event is fired when enabled the
        // first time. Otherwise an inconsistent state is possible when the
        // (system) clipboard contains already a valid object at the KNIME start
        setEnabled(false);
    }

    /** The id of this action. */
    public static final String ID
            = "org.knime.workbench.explorer.view.actions.PasteFromClipboard";
    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        //        String[] avail = getView().getClipboard().getAvailableTypeNames();
        //        LOGGER.debug("Available type names:");
        //        for (String t : avail) {
        //            LOGGER.debug(t);
        //        }
        //        LOGGER.debug(getView().getClipboard().getAvailableTypes());

        if (isValidExplorerFileStoreToPaste()) {
            return true;
        }

        return isValidURIToPaste();

    }

    private boolean isValidExplorerFileStoreToPaste() {
        ExplorerFileStoreTransfer transfer = ExplorerFileStoreTransfer.getInstance();
        Object c = getView().getClipboard().getContents(transfer);
        URI[] uri = (URI[])c;
        if (c != null && c instanceof URI[] && uri.length > 0) {
            return isSelectionValid(uri);
        }
        return false;
    }

    /**
     * @return true if the selected target is valid for pasting content, false
     *      otherwise
     */
    private boolean isSelectionValid(final URI[] sourceURI) {
        // then check if the selected target is valid
        // only enabled if exactly on file is selected
        List<AbstractExplorerFileStore> files = getAllSelectedFiles();
        if (isRO() || files.size() == 0) {
            return false;
        }
        if (files.size() > 1) {
            // invalid if the files do not have a common parent
            AbstractExplorerFileStore parent = null;
            for (AbstractExplorerFileStore file : files) {
                if (parent != null && !parent.equals(file.getParent())) {
                    return false;
                }
                parent = file.getParent();
            }
        }

        AbstractContentProvider cp = files.get(0).getContentProvider();
        if (!cp.isWritable()) {
            return false;
        }

        for (AbstractExplorerFileStore file : files) {
            final AbstractExplorerFileInfo fileInfo = file.fetchInfo();
            // for workflow groups check if it is writable
            if (fileInfo.isWorkflowGroup()) {
                if (!fileInfo.isModifiable()) {
                    return false;
                }
            } else {
                // for other types check if the parent is a writable workflow group
                final AbstractExplorerFileStore parent = file.getParent();
                if (parent == null) {
                    // no parent = root
                    return false;
                }
                final AbstractExplorerFileInfo parentInfo = parent.fetchInfo();

                if (parentInfo.isWorkflowGroup() && !parentInfo.isModifiable()) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isValidURIToPaste() {
        Object c = getView().getClipboard().getContents(TextTransfer.getInstance());
        if (c != null && c instanceof String && ((String)c).startsWith("http")) {
            URI encodedURI = createEncodedURI((String)c);

            //is there a URIImporter for the given URI?
            Optional<URIImporter> uriImport = URIImporterFinder.getInstance().findURIImporterFor(encodedURI);
            if (uriImport.isPresent()) {
                Optional<Class<? extends EntityImport>> entityImportClass =
                    uriImport.get().getEntityImportClass(encodedURI);
                //is the entity the URI represents a repo object (e.g. workflow)?
                if (entityImportClass.isPresent() && RepoObjectImport.class.isAssignableFrom(entityImportClass.get())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        if (pasteFromExplorerFileStore()) {
            return;
        }

        pasteFromURI();
    }

    private boolean pasteFromExplorerFileStore() {
        ExplorerFileStoreTransfer transfer = ExplorerFileStoreTransfer.getInstance();
        Clipboard clipboard = getView().getClipboard();
        Object c = clipboard.getContents(transfer);
        if (c == null || !(c instanceof URI[]) || ((URI[])c).length == 0) {
            return false;
        }

        setTarget(determineTarget());

        URI[] fileURI = (URI[]) c;
        List<AbstractExplorerFileStore> srcFileStores
        = new ArrayList<AbstractExplorerFileStore>(fileURI.length);
        for (URI uri : fileURI) {
            srcFileStores.add(ExplorerFileSystem.INSTANCE.getStore(uri));
        }

        // check if all affected flows can be copied/moved
        String message = ExplorerFileSystemUtils.isLockable(srcFileStores,
                !transfer.isCut());
        if (message != null) {
            LOGGER.warn("Can't paste from clipboard: " + message);
            MessageBox mb = new MessageBox(
                    Display.getCurrent().getActiveShell(),
                    SWT.ICON_ERROR | SWT.OK);
            mb.setText("Can't Paste All Selected Items.");
            mb.setMessage(message);
            mb.open();
            return true;
        }
        setPerformMove(transfer.isCut());
        setSuccess(copyOrMove(srcFileStores));
        if (!isSuccessful()) {
            LOGGER.error(isPerformMove() ? "Moving" : "Copying" + " to \""
                    + getTarget().getFullName() + "\" failed.");
        } else {
            LOGGER.debug("Successfully "
                    + (isPerformMove() ? "moved " : "copied ")
                    + srcFileStores.size() + " item(s) to \""
                    + getTarget().getFullName() + "\".");
        }
        if (isPerformMove()) {
            clipboard.clearContents();
            updateSelection();
        }
        return true;
    }

    private void pasteFromURI() {
        URI knimeURI = createEncodedURI((String)getView().getClipboard().getContents(TextTransfer.getInstance()));
        try {
            AtomicReference<File> file = new AtomicReference<File>();
            PlatformUI.getWorkbench().getProgressService().busyCursorWhile((monitor) -> {
                monitor.beginTask("Downloading file...", 100);
                try {
                    Optional<EntityImport> entityImport =
                        URIImporterFinder.getInstance().createEntityImportFor(knimeURI);
                    if (!entityImport.isPresent()) {
                        LOGGER.warn("Object at URI '" + knimeURI + "' not found");
                        return;
                    }
                    RepoObjectImport objImport = ((RepoObjectImport)entityImport.get());
                    URI dataURI = objImport.getDataURI();
                    File tmpFile = ResolverUtil.resolveURItoLocalOrTempFile(dataURI, monitor);
                    File tmpDir = FileUtil.createTempDir("download");

                    //change file extension according to the data object to paste
                    RepoObjectType objType = objImport.getType();
                    String fileExt = "";
                    if (objType == RepoObjectType.Workflow) {
                        fileExt = "." + KNIMEConstants.KNIME_WORKFLOW_FILE_EXTENSION;
                    } else if (objType == RepoObjectType.WorkflowGroup) {
                        fileExt = "." + KNIMEConstants.KNIME_ARCHIVE_FILE_EXTENSION;
                    }

                    file.set(new File(tmpDir, objImport.getName() + fileExt));
                    if (!tmpFile.renameTo(file.get())) {
                        LOGGER.warn("Pasting failed. The temporary file '" + file.get() + "' couldn't be renamed to '"
                            + file.get() + "'");
                        return;
                    }
                } catch (IOException e) {
                    LOGGER.warn("Object from URI '" + knimeURI + "' couldn't be pasted", e);
                    return;
                }
            });
            if (file.get() == null) {
                return;
            }

            AbstractExplorerFileStore target = determineTarget();
            target.getContentProvider().performDrop(getView(), new String[]{file.get().getAbsolutePath()}, target,
                DND.DROP_COPY);
            getViewer().refresh(ContentDelegator.getTreeObjectFor(target));
        } catch (InvocationTargetException | InterruptedException e) {
            LOGGER.warn("Object from URI '" + knimeURI + "' couldn't be pasted", e);
        }
    }

    private AbstractExplorerFileStore determineTarget() {
        List<AbstractExplorerFileStore> selection = getAllSelectedFiles();
        if (selection.size() == 1) {
            return selection.get(0);
        } else { // for multiple selection set the common parent as target
            return selection.get(0).getParent();
        }
    }

    /**
     * Updates this action in response to a selection change.
     */
    public void updateSelection() {
        setEnabled(isEnabled());
    }
}
