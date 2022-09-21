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
 * -------------------------------------------------------------------
 *
 * History
 *   13.04.2011 (wiswedel): created
 */
package org.knime.workbench.editor2;


import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.List;

import javax.swing.UIManager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodePropertyChangedEvent.NodeProperty;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.node.workflow.WorkflowPersistor.MetaNodeLinkUpdateResult;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.util.FileUtil;
import org.knime.core.util.pathresolve.ResolverUtil;

/**
 * A runnable which is used by the {@link WorkflowEditor} to load a workflow
 * with a progress bar. NOTE: As the {@link UIManager} holds a reference to this
 * runnable an own class file is necessary such that all references to the
 * created workflow manager can be deleted, otherwise the manager cannot be
 * deleted later and the memory cannot be freed.
 *
 * @author Christoph Sieb, University of Konstanz
 * @author Fabian Dill, University of Konstanz
 */
public class LoadMetaNodeTemplateRunnable extends PersistWorkflowRunnable {

    private WorkflowManager m_parentWFM;

    private final URI m_templateURI;

    private final WorkflowEditor m_editor;

    private MetaNodeLinkUpdateResult m_result;

    private final WorkflowContextV2 m_context;

    /**
     * @param wfm the target workflow (where to insert)
     * @param templateURI URI to the workflow directory or file from which the template should be loaded
     */
    public LoadMetaNodeTemplateRunnable(final WorkflowManager wfm, final URI templateURI) {
        m_parentWFM = wfm;
        m_templateURI = templateURI;
        m_editor = null;
        m_context = null;
    }

    /**
     * Used if a template/component is to be loaded into the workflow editor as a project (i.e. not embedded in another
     * workflow).
     *
     * @param editor the editor to open the component with
     * @param templateURI URI to the workflow directory or file from which the template should be loaded
     * @param context The context (for component template editors)
     */
    public LoadMetaNodeTemplateRunnable(final WorkflowEditor editor, final URI templateURI,
        final WorkflowContextV2 context) {
        m_parentWFM = WorkflowManager.ROOT;

        //strip "workflow.knime" from the URI which is append if
        //local component project is opened in workflow editor
        if (templateURI.toString().endsWith(WorkflowPersistor.WORKFLOW_FILE)) {
            String s = templateURI.toString();
            try {
                m_templateURI = new URI(s.substring(0, s.length() - WorkflowPersistor.WORKFLOW_FILE.length() - 1));
            } catch (URISyntaxException e) {
                //should never happen
                throw new RuntimeException(e);
            }
        } else {
            m_templateURI = templateURI;
        }
        m_editor = editor;
        m_context = context;
    }

    @Override
    public void run(final IProgressMonitor pm) {
        try {
            // create progress monitor
            final var progressHandler = new ProgressHandler(pm, 101, "Loading instance...");
            final var progressMonitor = new CheckCancelNodeProgressMonitor(pm);
            progressMonitor.addProgressListener(progressHandler);

            var parentFile = ResolverUtil.resolveURItoLocalOrTempFile(m_templateURI, pm);
            if (parentFile.isFile()) {
                //unzip
                final var tempDir = FileUtil.createTempDir("template-workflow");
                FileUtil.unzip(parentFile, tempDir);
                Files.delete(parentFile.toPath());
                final var extractedFiles = tempDir.listFiles();
                if (extractedFiles.length == 0) {
                    throw new IOException("Unzipping of file '" + parentFile + "' failed");
                }
                parentFile = extractedFiles[0];
            }
            if (pm.isCanceled()) {
                throw new InterruptedException();
            }

            final var d = Display.getDefault();
            final var loadHelper =
                GUIWorkflowLoadHelper.forTemplate(d, parentFile.getName(), m_context, m_editor != null);
            final var loadPersistor = loadHelper.createTemplateLoadPersistor(parentFile, m_templateURI);
            final var loadResult = new MetaNodeLinkUpdateResult("Shared instance from \"" + m_templateURI + "\"");
            m_parentWFM.load(loadPersistor, loadResult, new ExecutionMonitor(progressMonitor), false);

            m_result = loadResult;
            if (pm.isCanceled()) {
                throw new InterruptedException();
            }
            pm.subTask("Finished.");
            pm.done();

            // components are always stored without data
            // -> don't report data load errors neither node state changes if component is loaded as project
            final IStatus status = createStatus(m_result,
                !m_result.getGUIMustReportDataLoadErrors() || isComponentProject(), isComponentProject());
            final String message;
            switch (status.getSeverity()) {
                case IStatus.OK:
                    message = "No problems during load.";
                    break;
                case IStatus.WARNING:
                    message = "Warnings during load";
                    break;
                default:
                    message = "Errors during load";
            }
            if (isComponentProject() && m_result.getLoadedInstance() instanceof SubNodeContainer) {
                SubNodeContainer snc = (SubNodeContainer)m_result.getLoadedInstance();
                final var wfm = snc.getWorkflowManager();
                m_editor.setWorkflowManager(wfm);
                if (!status.isOK()) {
                    LoadWorkflowRunnable.showLoadErrorDialog(m_result, status, message, false);
                }
                final List<NodeID> linkedMNs = wfm.getLinkedMetaNodes(true);
                if (!linkedMNs.isEmpty()) {
                    final WorkflowEditor editor = m_editor;
                    m_editor.addAfterOpenRunnable(
                        () -> LoadWorkflowRunnable.postLoadCheckForMetaNodeUpdates(editor, wfm, linkedMNs));
                }
                snc.addNodePropertyChangedListener(l -> {
                    if (l.getProperty() == NodeProperty.ComponentMetadata) {
                        m_editor.markDirty();
                    }
                });
            } else {
                if (!status.isOK()) {
                    LoadWorkflowRunnable.showLoadErrorDialog(m_result, status, message, false);
                }
            }
        } catch (Exception ex) {
            if(isComponentProject()) {
                m_editor.setWorkflowManager(null);
            }
            throw new RuntimeException(ex);
        } finally {
            // IMPORTANT: Remove the reference to the file and the
            // editor!!! Otherwise the memory cannot be freed later
            m_parentWFM = null;
        }
    }

    private boolean isComponentProject() {
        return m_editor != null;
    }

    /** @return the result */
    public MetaNodeLinkUpdateResult getLoadResult() {
        return m_result;
    }
}
