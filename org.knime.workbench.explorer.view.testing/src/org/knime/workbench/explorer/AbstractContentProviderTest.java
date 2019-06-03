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
 *   Jun 3, 2019 (hornm): created
 */
package org.knime.workbench.explorer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.knime.workbench.explorer.LocalWorkspaceFileStoreTest.createTemplate;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.junit.Test;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.localworkspace.LocalWorkspaceContentProvider;
import org.knime.workbench.explorer.localworkspace.LocalWorkspaceContentProviderFactory;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.mockito.Mockito;

/**
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class AbstractContentProviderTest {

    /**
     * Tests
     * {@link AbstractContentProvider#canHostWorkflowTemplate(org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore)}.
     *
     * @throws CoreException
     * @throws IOException
     */
    @Test
    public void testCanHostWorkflowTemplate() throws CoreException, IOException {
        LocalWorkspaceContentProvider localWorkspace = (LocalWorkspaceContentProvider)ExplorerMountTable.mount("LOCAL",
            LocalWorkspaceContentProviderFactory.ID, null);
        LocalExplorerFileStore localExplorerRoot = (LocalExplorerFileStore)localWorkspace.getFileStore("/");

        LocalExplorerFileStore component = createTemplate(localExplorerRoot, "component", true);
        LocalExplorerFileStore metanode = createTemplate(localExplorerRoot, "metanode", false);
        assertTrue(localWorkspace.canHostWorkflowTemplate(component));
        assertTrue(localWorkspace.canHostWorkflowTemplate(metanode));

        AbstractContentProvider abstractContentProvider =
            mock(AbstractContentProvider.class, Mockito.CALLS_REAL_METHODS);
        when(abstractContentProvider.canHostComponentTemplates()).thenReturn(false);
        when(abstractContentProvider.canHostMetaNodeTemplates()).thenReturn(true);
        assertFalse(abstractContentProvider.canHostWorkflowTemplate(component));
        assertTrue(abstractContentProvider.canHostWorkflowTemplate(metanode));

        when(abstractContentProvider.canHostComponentTemplates()).thenReturn(true);
        when(abstractContentProvider.canHostMetaNodeTemplates()).thenReturn(false);
        assertTrue(abstractContentProvider.canHostWorkflowTemplate(component));
        assertFalse(abstractContentProvider.canHostWorkflowTemplate(metanode));

        when(abstractContentProvider.canHostComponentTemplates()).thenReturn(false);
        when(abstractContentProvider.canHostMetaNodeTemplates()).thenReturn(false);
        assertFalse(abstractContentProvider.canHostWorkflowTemplate(component));
        assertFalse(abstractContentProvider.canHostWorkflowTemplate(metanode));
    }
}
