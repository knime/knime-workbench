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
 *   Jun 5, 2019 (loki): created
 */
package org.knime.workbench.explorer.view.actions;

import java.util.Optional;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.ExplorerView;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * With the KAP 4.0 release this action has been kept in so as to not alarm existing users; it will simply show
 *  a reminder to users where the new functionality sits.
 *
 * @author loki der quaeler
 * @since 8.4
 */
public class DeprecatedEditMetadataAction extends ExplorerAction {
    /** ID of the global rename action in the explorer menu. */
    public static final String METAINFO_ACTION_ID = "org.knime.workbench.explorer.action.openMetaInfo";

    private static final ImageDescriptor ICON =
        AbstractUIPlugin.imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID, "icons/meta_info_edit.png");


    /**
     * @param viewer the associated tree viewer
     */
    public DeprecatedEditMetadataAction(final ExplorerView viewer) {
        super(viewer, "Edit Meta Information...");
        setImageDescriptor(ICON);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return METAINFO_ACTION_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        final Shell s = PlatformUI.getWorkbench().getDisplay().getActiveShell();

        MessageDialog.openInformation(s, "Editing has moved...",
            "You are now able to edit metadata in the 'Description' view. If the Description View is not "
                    + "currently open, you can open it from the View menu.");
    }

    @Override
    public boolean isEnabled() {
        return getSingleSelectedElement().isPresent();
    }

    private Optional<AbstractExplorerFileStore> getSingleSelectedElement() {
        return super.getSingleSelectedElement(
            fs -> AbstractExplorerFileStore.isWorkflow(fs) || AbstractExplorerFileStore.isWorkflowGroup(fs));
    }
}
