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
 * ------------------------------------------------------------------------
 */
package org.knime.workbench.explorer.view.actions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.tuple.Triple;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.node.workflow.contextv2.LocalLocationInfo;
import org.knime.core.node.workflow.contextv2.LocationInfo;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.RemoteWorkflowInput;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.view.ExplorerJob;
import org.knime.workbench.explorer.view.ExplorerView;
import org.knime.workbench.explorer.view.actions.imports.WorkflowImportAction;
import org.osgi.framework.FrameworkUtil;

/**
 * Action that downloads remote items to a temp location and opens them in an editor.
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz, Germany
 * @since 7.3
 */
public class OpenKNIMEArchiveFileAction extends Action {

    private static final String PLUGIN_ID = FrameworkUtil.getBundle(OpenKNIMEArchiveFileAction.class).getSymbolicName();

    private static final NodeLogger LOGGER = NodeLogger.getLogger(OpenKNIMEArchiveFileAction.class);

    /*--------- inner job class -------------------------------------------------------------------------*/
    private static class ExtractInTempAndOpenJob extends ExplorerJob {

        private final IWorkbenchPage m_page;

        private final File m_source;

        private final String m_mountId;

        private final LocationInfo m_locationInfo;

        ExtractInTempAndOpenJob(final IWorkbenchPage page, final File file, final String mountId,
                final LocationInfo locationInfo) {
            super("Extract and open items from KNIME archive file");
            m_page = page;
            m_source = file;
            m_mountId = mountId;
            m_locationInfo = locationInfo;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected IStatus run(final IProgressMonitor monitor) {
            String fileId;
            try {
                fileId = m_source.getCanonicalPath();
            } catch (IOException | SecurityException e) {
                LOGGER.warn("Constructing canonical path of file: " + m_source + " failed: " + e.getMessage(), e);
                fileId = m_source.getPath();
            }
            fileId = fileId.replace("\\", "/").replaceFirst(":", "/");
            SubMonitor progress = SubMonitor.convert(monitor, 1);
            progress.beginTask("Extracting " + fileId, 1);
            LocalExplorerFileStore tmpDestDir;
            try {
                tmpDestDir = ExplorerMountTable.createExplorerTempDir(fileId);
                tmpDestDir = tmpDestDir.getChild(fileId);
                tmpDestDir.mkdir(EFS.NONE, progress);
            } catch (CoreException e1) {
                return new Status(e1.getStatus().getSeverity(), PLUGIN_ID, e1.getMessage(), e1);
            }
            final TempExtractArchive extractAction =
                new TempExtractArchive(m_source, tmpDestDir, /*deleteSource=*/false, null, progress);
            try {
                extractAction.runSync(monitor);
            } catch (CoreException e) {
                LOGGER.info("The (possibly partially) downloaded file is not deleted and might be still available: "
                    + tmpDestDir);
                return e.getStatus();
            }

            String[] content;
            try {
                content = tmpDestDir.childNames(EFS.NONE, monitor);
            } catch (CoreException e) {
                return new Status(e.getStatus().getSeverity(), PLUGIN_ID, e.getMessage(), e);
            }
            if (content == null || content.length == 0) {
                try {
                    tmpDestDir.delete(EFS.NONE, monitor);
                } catch (CoreException e) {
                    LOGGER.error("Unable to delete the empty temporary download directory: " + e.getMessage(), e);
                    // ignore the deletion error
                }
                return new Status(IStatus.ERROR, PLUGIN_ID, 1, "Error during download: No content available.", null);
            }
            if (content.length == 1) {
                // it is weird if the length is not 1 (because we downloaded one item)
                tmpDestDir = tmpDestDir.getChild(content[0]);
            }

            if (tmpDestDir.fetchInfo().isDirectory()) {
                LocalExplorerFileStore wf = tmpDestDir.getChild(WorkflowPersistor.WORKFLOW_FILE);
                if (wf.fetchInfo().exists()) {
                    tmpDestDir = wf;
                } else {
                    // directories that are not workflows cannot be opened
                    LOGGER.info("The downloaded content is not deleted and is still available: " + tmpDestDir);
                    return new Status(IStatus.ERROR, PLUGIN_ID, 1,
                        "Cannot open downloaded content: Directory doesn't contain a workflow", null);
                }
            }

            final LocalExplorerFileStore editorInput = tmpDestDir;
            final var rwi = new RemoteWorkflowInput(editorInput, m_mountId, m_locationInfo, m_source.toURI());
            final AtomicReference<IStatus> returnStatus = new AtomicReference<IStatus>(Status.OK_STATUS);
            Display.getDefault().syncExec(() -> {
                try {
                    m_page.openEditor(rwi, IDE.getEditorDescriptor(editorInput.getName()).getId());
                } catch (PartInitException ex) {
                    LOGGER.info("Cannot open editor for the downloaded content. "
                        + "It is not deleted and is still available: " + extractAction.getTargetDir());
                    returnStatus.set(new Status(IStatus.ERROR, PLUGIN_ID, 1,
                        "Cannot open the editor for downloaded " + editorInput.getName(), null));
                }
            });

            return returnStatus.get();
        }
    }

    /* --- end of inner job class -----------------------------------------------------------------------------------*/

    private final List<Triple<File, String, LocationInfo>> m_sources;

    private final IWorkbenchPage m_page;

    /**
     * Downloads a remote item to a temp location and opens it in an editor.
     *
     * @param page the current workbench page
     * @param sources things to open
     */
    public OpenKNIMEArchiveFileAction(final IWorkbenchPage page, final List<File> sources) {
        setDescription("Download and open");
        setToolTipText(getDescription());
        setImageDescriptor(ImageRepository.getIconDescriptor(SharedImages.ServerDownload));
        m_page = page;
        m_sources = new ArrayList<>(sources.size());
        for (final var source : sources) {
            m_sources.add(Triple.of(source, null, LocalLocationInfo.getInstance(source.toPath())));
        }
    }

    /**
     * Downloads a remote item to a temp location and opens it in an editor.
     *
     * @param page the current workbench page
     * @param source things to open
     */
    public OpenKNIMEArchiveFileAction(final IWorkbenchPage page, final File source, final String mountId,
            final LocationInfo optLocationInfo) {
        setDescription("Download and open");
        setToolTipText(getDescription());
        setImageDescriptor(ImageRepository.getIconDescriptor(SharedImages.ServerDownload));
        m_page = page;
        m_sources = List.of(Triple.of(source, mountId, optLocationInfo!= null ? optLocationInfo
            : LocalLocationInfo.getInstance(source.toPath())));
    }

    @Override
    public void run() {
        for (var p : m_sources) {
            final var localDir = p.getLeft();
            final var mountId = p.getMiddle();
            final var location = p.getRight();
            String fileID;
            try {
                fileID = localDir.getCanonicalPath();
            } catch (IOException | SecurityException e) {
                fileID = localDir.getPath();
            }
            if (localDir.getName().endsWith("." + KNIMEConstants.KNIME_WORKFLOW_FILE_EXTENSION)) {
                LOGGER.info("Opening file " + fileID + ", extracting it first into the temp mount point");
                ExtractInTempAndOpenJob job = new ExtractInTempAndOpenJob(m_page, localDir, mountId, location);
                job.schedule();
            } else {
                IViewPart part = m_page.findView("org.knime.workbench.explorer.view");
                if (part instanceof ExplorerView) {
                    LOGGER.info("Importing file " + fileID);
                    ExplorerView view = (ExplorerView)part;
                    WorkflowImportAction action = new WorkflowImportAction(view, null, fileID);
                    action.run();
                }
            }
        }
    }
}
