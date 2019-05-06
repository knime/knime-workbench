/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 */
package org.knime.workbench.explorer.templates.preferences;

import static org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore.isWorkflowGroup;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.jface.preference.ListEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.dialogs.SpaceResourceSelectionDialog;
import org.knime.workbench.explorer.dialogs.Validator;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileInfo;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.templates.NodeRepoSyncSettings;

/**
 * A field editor to select workflow groups in all connected mount points.
 */
public class WorkflowGroupListEditor extends ListEditor {

    /**
     * Creates a new path field editor
     */
    protected WorkflowGroupListEditor() {
    }

    /**
     * Creates a path field editor.
     *
     * @param name the name of the preference this field editor works on
     * @param labelText the label text of the field editor
     * @param parent the parent of the field editor's control
     */
    public WorkflowGroupListEditor(final String name, final String labelText, final Composite parent) {
        init(name, labelText);
        createControl(parent);
        getDownButton().setVisible(false);
        getUpButton().setVisible(false);
    }

    @Override
    protected String createList(final String[] items) {
        return Arrays.stream(items).collect(Collectors.joining(","));
    }

    @Override
	protected String getNewInputObject() {

        String[] mountIDs = getConnectedAndCustomizableMountPoints();
        SpaceResourceSelectionDialog dialog = new SpaceResourceSelectionDialog(getShell(), mountIDs, null);
        dialog.setTitle("Select workflow group");
        dialog.setHeader(
            "Select a workflow group at a mount point that shall\ncontribute metanode templates to the node repository");
        dialog.setValidator(new Validator() {
            @Override
            public String validateSelectionValue(final AbstractExplorerFileStore selection, final String name) {
                final AbstractExplorerFileInfo info = selection.fetchInfo();
                if (info.isWorkflowGroup()) {
                    return null;
                }
                return "Only workflow groups can be selected as target.";
            }
        });
        if (dialog.open() != Window.OK) {
            return null;
        }
        AbstractExplorerFileStore target = dialog.getSelection();
        String entry = target.getMountIDWithFullPath();
        if (Arrays.stream(getList().getItems()).anyMatch(i -> i.equals(entry))) {
            return null;
        }
        return entry;
    }

    @Override
	protected String[] parseString(final String stringList) {
        return stringList.split(",");
    }

    private static String[] getConnectedAndCustomizableMountPoints() {
        return ExplorerMountTable.getMountedContent().values().stream().filter(cp -> {
            if (cp.getMountID().equals("LOCAL")) {
                return true;
            } else if (isWorkflowGroup(cp.getFileStore("/"))) {
                Optional<Boolean> isServerConfigured = NodeRepoSyncSettings.getInstance().hasServerConfiguredPaths(cp);
                if (isServerConfigured.isPresent()) {
                    return !isServerConfigured.get();
                } else {
                    //should actually not happen
                    return false;
                }
            }
            return false;
        }).map(cp -> cp.getMountID()).toArray(s -> new String[s]);
    }
}
