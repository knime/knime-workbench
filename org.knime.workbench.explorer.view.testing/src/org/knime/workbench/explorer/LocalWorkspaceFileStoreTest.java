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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.TemplateType;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.WorkflowCreationHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.filesystem.meta.MetaInfo;
import org.knime.workbench.explorer.filesystem.meta.TemplateInfo;
import org.knime.workbench.explorer.localworkspace.LocalWorkspaceContentProvider;
import org.knime.workbench.explorer.localworkspace.LocalWorkspaceContentProviderFactory;
import org.knime.workbench.ui.preferences.PreferenceConstants;

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
     * @throws IOException
     */
    @Before
    public void setup() throws IOException {
        LocalWorkspaceContentProvider localWorkspace = (LocalWorkspaceContentProvider)ExplorerMountTable.mount("LOCAL",
            LocalWorkspaceContentProviderFactory.ID, null);
        m_localExplorerRoot = (LocalExplorerFileStore)localWorkspace.getFileStore("/");
    }

    @SuppressWarnings("javadoc")
    @Test
    public void testFetchTemplateMetaInfo() throws CoreException {
        LocalExplorerFileStore wt = createTemplate(m_localExplorerRoot, "wt1", false);
        MetaInfo metaInfo = wt.fetchMetaInfo().get();
        assertTrue("wrong meta info type", metaInfo instanceof TemplateInfo);
        assertThat("wrong template type", ((TemplateInfo)metaInfo).getType(), is(TemplateType.MetaNode));

        wt = createTemplate(m_localExplorerRoot, "wt2", true);
        metaInfo = wt.fetchMetaInfo().get();
        assertTrue("wrong meta info type", metaInfo instanceof TemplateInfo);
        assertThat("wrong template type", ((TemplateInfo)metaInfo).getType(), is(TemplateType.SubNode));
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
    @SuppressWarnings("unchecked")
    static <T extends LocalExplorerFileStore> T createTemplate(final T parent, final String name, final boolean wrap) {
        if (METANODE_ROOT== null) {
            METANODE_ROOT = WorkflowManager.ROOT.createAndAddProject("KNIME Tmp MetaNode Repository",
                createWorkflowCreationHelper(parent, null));
        }
        //deactivate prompt for linking type
        IPreferenceStore prefStore = ExplorerActivator.getDefault().getPreferenceStore();
        prefStore.setValue(PreferenceConstants.P_EXPLORER_LINK_ON_NEW_TEMPLATE, MessageDialogWithToggle.NEVER);

        WorkflowManager metaNode = METANODE_ROOT.createAndAddProject(name, createWorkflowCreationHelper(parent, name));
        if (wrap) {
            metaNode.getParent().convertMetaNodeToSubNode(metaNode.getID());
            boolean success = parent.getContentProvider()
                .saveSubNodeTemplate((SubNodeContainer)metaNode.getParent().getNodeContainer(metaNode.getID()), parent);
            assert success;
        } else {
            boolean success = parent.getContentProvider().saveMetaNodeTemplate(metaNode, parent);
            assert success;
        }

        return (T)parent.getChild(name);
    }

    private static WorkflowCreationHelper createWorkflowCreationHelper(final AbstractExplorerFileStore parent,
        final String name) {
        try {
            WorkflowCreationHelper workflowCreation = new WorkflowCreationHelper();
            File currentLocation;
            if (name != null) {
                currentLocation = new File(parent.toLocalFile(), name);
            } else {
                currentLocation = parent.toLocalFile();
            }
            WorkflowContext.Factory workflowContextFactory = new WorkflowContext.Factory(currentLocation);
            workflowContextFactory.setMountpointRoot(parent.getContentProvider().getFileStore("/").toLocalFile());
            workflowCreation.setWorkflowContext(workflowContextFactory.createContext());
            return workflowCreation;
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }

}
