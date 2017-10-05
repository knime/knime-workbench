/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ------------------------------------------------------------------------
 */
package org.knime.workbench.explorer.view.actions;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.filesystem.local.LocalFile;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileInfo;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.ExplorerView;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.metainfo.model.MetaInfoFile;

public class GlobalEditMetaInfoAction extends ExplorerAction {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            GlobalEditMetaInfoAction.class);

    /** ID of the global rename action in the explorer menu. */
    public static final String METAINFO_ACTION_ID =
        "org.knime.workbench.explorer.action.openMetaInfo";

    private static final ImageDescriptor ICON = KNIMEUIPlugin
    .imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
            "icons/meta_info_edit.png");;

    /**
     * @param viewer the associated tree viewer
     */
    public GlobalEditMetaInfoAction(final ExplorerView viewer) {
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
        try {
            AbstractExplorerFileStore srcFileStore = getSingleSelectedWorkflowOrGroup()
                    .orElseThrow(() -> new IllegalStateException("No single workflow or group selected"));
            AbstractExplorerFileStore metaInfo =
                    srcFileStore.getChild(WorkflowPersistor.METAINFO_FILE);
            AbstractExplorerFileInfo fetchInfo = metaInfo.fetchInfo();
            if (!fetchInfo.exists() || (fetchInfo.getLength() == 0)) {
                // create a new meta info file if it does not exist
                MetaInfoFile.createMetaInfoFile(
                        srcFileStore.toLocalFile(EFS.NONE, null),
                        AbstractExplorerFileStore.isWorkflow(srcFileStore));
            }
            IFileStore localFS = new LocalFile(
                    metaInfo.toLocalFile(EFS.NONE, null));
            IDE.openEditorOnFileStore(PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow().getActivePage(), localFS);
        } catch (Exception e) {
            LOGGER.error("Could not open meta info editor.", e);
        }
    }

    @Override
    public boolean isEnabled() {
        return getSingleSelectedWorkflowOrGroup().isPresent();
    }

}
