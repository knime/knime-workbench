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
 * History
 *   07.07.2014 (thor): created
 */
package org.knime.workbench.explorer.view.actions.imports;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * Utility class for workflow import actions.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
final class WorkflowImportHelper {
    private WorkflowImportHelper() {
    }

    static final ImageDescriptor ICON = AbstractUIPlugin.imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
        "icons/knime_import.png");

    static final String MENU_TEXT = "Import KNIME Workflow...";

    static final String TOOLTIP = "Imports KNIME workflows from a directory or an archive into the workspace";

    static AbstractExplorerFileStore openImportWizard(final Shell parentShell,
        final AbstractExplorerFileStore initialFile) {
        return openImportWizard(parentShell, initialFile, null);
    }

    static AbstractExplorerFileStore openImportWizard(final Shell parentShell,
        final AbstractExplorerFileStore initialFile, final String selectedFile) {
        WorkflowImportWizard impWiz = new WorkflowImportWizard();
        impWiz.setInitialDestination(initialFile);
        if (selectedFile != null && !selectedFile.isEmpty()) {
            impWiz.setSelectedZipFile(selectedFile);
        }

        WizardDialog dialog = new WizardDialog(parentShell, impWiz);
        if (Window.CANCEL != dialog.open()) {
            return impWiz.getDestinationContainer();
        } else {
            return null;
        }
    }
}
