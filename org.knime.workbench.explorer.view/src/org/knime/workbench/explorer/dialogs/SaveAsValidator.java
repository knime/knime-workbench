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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 * Created on 29.04.2014 by ohl
 */
package org.knime.workbench.explorer.dialogs;

import java.io.File;
import java.net.URI;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.knime.core.util.VMFileLocker;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileInfo;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;

/**
 *
 * @author Peter Ohl, KNIME AG, Zurich, Switzerland
 * @since 6.4
 */
public final class SaveAsValidator extends Validator {

    private final SpaceResourceSelectionDialog m_dialog;
    private final AbstractExplorerFileStore m_currentStore;

    /**
     * Accepts writable worflows and workflow groups. If a flow is selected the name field is updated.
     * @param dialog the dialog this is the validator for
     * @param currentLocation current location of the flow. SaveTo this location is disallowed. (Could be null - but
     * save to location with open editors is disallowed too!)
     */
   public SaveAsValidator(final SpaceResourceSelectionDialog dialog, final AbstractExplorerFileStore currentLocation) {
        m_dialog = dialog;
        m_currentStore = currentLocation;
    }

    @Override
    public String validateSelectionValue(final AbstractExplorerFileStore selection, final String name) {
        // called when clicked
        if (selection == null) {
            return "Please select a destination workflow or workflow group.";
        }
        AbstractExplorerFileInfo info = selection.fetchInfo();
        if (!info.isWorkflow() && !info.isWorkflowGroup()) {
            return "Please select a destination workflow or workflow group.";
        }
        if (selection.fetchInfo().isWorkflow()) {
            // set the name
            m_dialog.setNameFieldValue(selection.getName()); // triggers name validation
        }
        String flowname = m_dialog.getNameFieldValue();
        if (flowname == null) {
            // name field not enabled
            flowname = name;
        }
        return validateDestination(selection, flowname);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String validateNewNameValue(final String name, final AbstractExplorerFileStore currentSelection) {
        return validateDestination(currentSelection, name);

    }

    /**
     * Validates the destination created from the current selection and name.
     * @param selection current selection
     * @param name current name
     * @return the error message or null if aceptable
     */
    protected String validateDestination(final AbstractExplorerFileStore selection, final String name) {
        if (selection == null) {
            return "Please select a destination workflow or workflow group.";
        }
        AbstractExplorerFileInfo info = selection.fetchInfo();
        if (!info.isWorkflow() && !info.isWorkflowGroup()) {
            return "Please select a destination workflow or workflow group.";
        }
        if (name == null || name.isEmpty()) {
            return "Please enter a name for the destination workflow";
        }
        String err = ExplorerFileSystem.validateFilename(name);
        if (err != null) {
            return name + " is not a valid file name:" + err;
        }
        AbstractExplorerFileStore dest = selection;
        if (selection.fetchInfo().isWorkflow()) {
            dest = selection.getParent().getChild(name);
        } else {
            // add the name to the selection
            dest = selection.getChild(name);
        }

        if (m_currentStore != null && m_currentStore.toURI().equals(dest.toURI())) {
            return "Please select a new (different!) location as destination.";
        }
        AbstractExplorerFileInfo destInfo = dest.fetchInfo();
        if (!destInfo.exists()) {
            // see if we can create it
            AbstractExplorerFileStore parent = dest.getParent();
            if (parent != null && !parent.fetchInfo().isModifiable()) {
                return "Can't write to destination workflow group (missing write permissions).";
            } else {
                return null;
            }
        } else {
            if (!destInfo.isModifiable()) {
                return "Can't override selected destination workflow (missing overwrite permissions)";
            }
        }
        if (dest instanceof RemoteExplorerFileStore) {
            return null;
        }
        File locDest = null;
        try {
            locDest = dest.toLocalFile();
        } catch (CoreException e) {
            // stays null then
        }
        if (locDest == null) {
            return "Problem retrieving the selected file - please change the selection.";
        }
        if (isAnyEditorToWorkflowOpen(locDest.toURI())) {
            return "The selected destination is currently open in another editor.";
        }
        if (VMFileLocker.lockForVM(locDest)) {
            VMFileLocker.unlockForVM(locDest);
        } else {
            return "The selected destination is locked/edited by another user";
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getResultPath(final AbstractExplorerFileStore selection, final String name) {
        if (selection == null || name == null || name.isEmpty()) {
            return "";
        }
        AbstractExplorerFileInfo info = selection.fetchInfo();
        if (info.isWorkflow()) {
            AbstractExplorerFileStore parent = selection.getParent();
            if (parent == null) {
                return "";
            } else {
                return parent.getChild(name).getMountIDWithFullPath();
            }
        } else if (info.isWorkflowGroup()) {
            return selection.getChild(name).getMountIDWithFullPath();
        } else {
            return "";
        }
    }

    /**
     * Checks if an editor (meta info or workflow or any editor) to the workflow with the given URI is open.
     *
     * @param workflowURI The URI to the workflow directory. Must be file protocol
     * @return true if an editor is open, false otherwise
     */
    public static boolean isAnyEditorToWorkflowOpen(final URI workflowURI) {
        boolean isOpen = false;
        // Go through all open editors
        for (IEditorReference editorRef
                : PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences()) {
            IEditorPart editorPart = editorRef.getEditor(true);
            IEditorInput editorInput = editorPart.getEditorInput();
            if (editorInput instanceof FileStoreEditorInput) {
                FileStoreEditorInput fileStoreInput = (FileStoreEditorInput)editorInput;
                // Get URI of the editor
                URI uri = fileStoreInput.getURI();
                File wfFile = null;
                if ("file".equals(uri.getScheme())) {
                    wfFile = new File(uri);
                } else if (ExplorerFileSystem.SCHEME.equals(uri.getScheme())) {
                    AbstractExplorerFileStore store = ExplorerFileSystem.INSTANCE.getStore(uri);
                    if (store != null) {
                        try {
                            wfFile = store.toLocalFile();
                        } catch (CoreException e) {
                            // wfFile stays null then.
                        }
                    }
                }
                if (wfFile == null) {
                    continue;
                }
                // Check if parent of the editor is the workflow directory (workflow uri must be file:)
                if (workflowURI.equals(wfFile.getParentFile().toURI())) {
                    isOpen = true;
                    break;
                }
            }
        }
        return isOpen;
    }
}
