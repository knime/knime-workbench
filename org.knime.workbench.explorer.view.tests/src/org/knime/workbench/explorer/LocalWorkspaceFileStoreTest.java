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
 *   May 29, 2019 (hornm): created
 */
package org.knime.workbench.explorer;

import static org.junit.Assert.assertTrue;
import static org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore.isComponentTemplate;
import static org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore.isMetaNodeTemplate;

import java.security.NoSuchAlgorithmException;
import java.util.function.Consumer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowCreationHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.workbench.WorkbenchConstants;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountException;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountTable;
import org.knime.core.workbench.mountpoint.contribution.local.LocalWorkspaceMountPointState;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.localworkspace.LocalWorkspaceContentProvider;

/**
 * Tests some method implementations of {@link LocalExplorerFileStore}.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class LocalWorkspaceFileStoreTest {

    private LocalExplorerFileStore m_localExplorerRoot;

    private static WorkflowManager METANODE_ROOT;

    /**
     * Setup local mountpoint etc.
     *
     * @throws WorkbenchMountException
     */
    @Before
    public void setup() throws WorkbenchMountException {
        final var localMountPoint =
                WorkbenchMountTable.mount(LocalWorkspaceMountPointState.TYPE.getDefaultSettings().orElseThrow());
        LocalWorkspaceContentProvider localWorkspace =
            (LocalWorkspaceContentProvider)ExplorerMountTable.toAbstractContentProvider(localMountPoint).orElseThrow();
        m_localExplorerRoot = (LocalExplorerFileStore)localWorkspace.getRootStore();
    }

    @SuppressWarnings("javadoc")
    @Test
    public void testFetchTemplateMetaInfo() throws CoreException {
        LocalExplorerFileStore wt = createTemplate(m_localExplorerRoot, "wt1", false);
        assertTrue("wrong template type", isMetaNodeTemplate(wt));

        wt = createTemplate(m_localExplorerRoot, "wt2", true);
        assertTrue("wrong template type", isComponentTemplate(wt));
    }

    @SuppressWarnings("javadoc")
    @Test
    public void testEncryptedSharedComponent() {
        LocalExplorerFileStore wt = createTemplate(m_localExplorerRoot, "encrypted_wt", true, wfm -> {
            try {
                wfm.setWorkflowPassword("1234", "dunno");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        });
        assertTrue("wrong template type", isComponentTemplate(wt));
    }

    /**
     * Creates a 'real' template (with no input and output ports) where the respective files
     * (<tt>workflow.knime, template.knime</tt> etc.) have actual content.
     *
     * @param parent the location in which the template should be created
     * @param name the tempate's name
     * @param wrap whether to create a 'wrapped' metanode-template to a metanode-template
     * @param wfmManipulation function for optional workflow manager manipulations after its creation (can be
     *            <code>null</code>)
     * @return the file store that represents the new template
     */
    @SuppressWarnings("unchecked")
    private static <T extends LocalExplorerFileStore> T createTemplate(final T parent, final String name,
        final boolean wrap, final Consumer<WorkflowManager> wfmManipulation) {
        if (METANODE_ROOT == null) {
            METANODE_ROOT = WorkflowManager.ROOT.createAndAddProject("KNIME Tmp MetaNode Repository",
                createWorkflowCreationHelper(parent, null));
        }
        //deactivate prompt for linking type
        IPreferenceStore prefStore = ExplorerActivator.getDefault().getPreferenceStore();
        prefStore.setValue(WorkbenchConstants.P_EXPLORER_LINK_ON_NEW_TEMPLATE, MessageDialogWithToggle.NEVER);

        NodeID nodeId = METANODE_ROOT.createAndAddProject(name, createWorkflowCreationHelper(parent, name)).getID();
        if (wrap) {
            METANODE_ROOT.convertMetaNodeToSubNode(nodeId);
            SubNodeContainer snc = (SubNodeContainer)METANODE_ROOT.getNodeContainer(nodeId);
            if (wfmManipulation != null) {
                wfmManipulation.accept(snc.getWorkflowManager());
            }
            boolean success = parent.getContentProvider()
                .saveSubNodeTemplate(snc, parent);
            assert success;
        } else {
            WorkflowManager wfm = (WorkflowManager)METANODE_ROOT.getNodeContainer(nodeId);
            if (wfmManipulation != null) {
                wfmManipulation.accept(wfm);
            }
            boolean success = parent.getContentProvider()
                .saveMetaNodeTemplate(wfm, parent);
            assert success;
        }

        return (T)parent.getChild(name);
    }

    /**
     * Creates a 'real' template (with no input and output ports) where the respective files
     * (<tt>workflow.knime, template.knime</tt> etc.) have actual content.
     *
     * @param parent the location in which the template should be created
     * @param name the tempate's name
     * @param wrap whether to create a 'wrapped' metanode-template to a metanode-template
     * @return the file store that represents the new template
     */
    static <T extends LocalExplorerFileStore> T createTemplate(final T parent, final String name, final boolean wrap) {
        return createTemplate(parent, name, wrap, null);
    }

    private static WorkflowCreationHelper createWorkflowCreationHelper(final LocalExplorerFileStore parent,
        final String name) {
        try {
            final var contentProvider = parent.getContentProvider();
            final var wfStore = name == null ? parent : parent.getChild(name);
            final var rootPath = contentProvider.getRootStore().toLocalFile().toPath();
            final var localPath = wfStore.toLocalFile().toPath();
            final var workflowContext = WorkflowContextV2.builder()
                .withAnalyticsPlatformExecutor(exec -> exec
                    .withCurrentUserAsUserId()
                    .withLocalWorkflowPath(localPath)
                    .withMountpoint(wfStore.getMountID(), rootPath))
                .withLocalLocation()
                .build();
            return new WorkflowCreationHelper(workflowContext);
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }

}
