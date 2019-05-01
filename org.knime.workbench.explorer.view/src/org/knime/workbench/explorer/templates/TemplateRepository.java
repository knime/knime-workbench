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
package org.knime.workbench.explorer.templates;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.IProgressMonitor;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.FileWorkflowPersistor;
import org.knime.core.node.workflow.UnsupportedWorkflowVersionException;
import org.knime.core.node.workflow.WorkflowCreationHelper;
import org.knime.core.node.workflow.WorkflowLoadHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.util.pathresolve.ResolverUtil;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;

/**
 * TODO
 * TODO remove templates from repo
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 8.4
 */
public final class TemplateRepository {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(TemplateRepository.class);

    private static TemplateRepository INSTANCE;

    private final WorkflowManager m_templatesRoot =
        WorkflowManager.ROOT.createAndAddProject("KNIME Template Repository", new WorkflowCreationHelper());

    private Map<String, CompletableFuture<FileWorkflowPersistor>> m_cachedTemplates = new HashMap<>();

    private TemplateRepository() {
        //singleton
    }

    /**
     * @return the singleton instance
     */
    public static TemplateRepository getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TemplateRepository();
        }
        return INSTANCE;
    }

    /**
     * TODO
     * @param fileStore
     * @param pm
     * @return
     */
    public CompletableFuture<WorkflowManager> getTemplateWfm(final AbstractExplorerFileStore fileStore,
        final IProgressMonitor pm) {
        startLoadTemplateIfDoesntExist(fileStore, pm);
        return m_cachedTemplates.get(fileStore.getFullName()).thenApply(persistor -> {
            try {
                //TODO also cache workflow manager??
                return m_templatesRoot.load(persistor, new ExecutionMonitor(), false).getWorkflowManager();
            } catch (IOException | InvalidSettingsException | CanceledExecutionException
                    | UnsupportedWorkflowVersionException e) {
                // TODO Auto-generated catch block
                throw new RuntimeException();
            }
        });
    }

    /**
     * TODO
     *
     * @param fileStore
     * @param pm
     * @return
     */
    public CompletableFuture<FileWorkflowPersistor> getTemplatePersistor(final AbstractExplorerFileStore fileStore,
        final IProgressMonitor pm) {
        startLoadTemplateIfDoesntExist(fileStore, pm);
        return m_cachedTemplates.get(getFileStoreKey(fileStore));
    }

    public boolean hasTemplate(final AbstractExplorerFileStore fileStore) {
        return m_cachedTemplates.containsKey(getFileStoreKey(fileStore));
    }

    private void startLoadTemplateIfDoesntExist(final AbstractExplorerFileStore fileStore, final IProgressMonitor pm) {
        m_cachedTemplates.computeIfAbsent(getFileStoreKey(fileStore), k -> {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    URI sourceURI = fileStore.toURI();
                    File f = ResolverUtil.resolveURItoLocalOrTempFile(sourceURI, pm);

                    LOGGER.debug("meta node template name: " + f.getName());
                    WorkflowLoadHelper loadHelper = new WorkflowLoadHelper(true) {
                        /** {@inheritDoc} */
                        @Override
                        public String getDotKNIMEFileName() {
                            return WorkflowPersistor.WORKFLOW_FILE;
                        }
                    };
                    // don't lock workflow dir
                    return WorkflowManager.createLoadPersistor(f, loadHelper);
                } catch (Exception e) {
                    LOGGER.error("Failed to load meta workflow repository", e);
                }
                return null;
            });
        });
    }

    public WorkflowManager getTemplateRoot() {
        return m_templatesRoot;
    }

    private static String getFileStoreKey(final AbstractExplorerFileStore fileStore) {
        return fileStore.getMountIDWithFullPath();
    }
}

