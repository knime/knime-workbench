/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   Mar 16, 2016 (albrecht): created
 */
package org.knime.workbench.explorer.view.actions;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.internal.wizards.datatransfer.ILeveledImportStructureProvider;
import org.eclipse.ui.internal.wizards.datatransfer.ZipLeveledStructureProvider;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.AbstractContentProvider.AfterRunCallback;
import org.knime.workbench.explorer.view.actions.imports.IWorkflowImportElement;
import org.knime.workbench.explorer.view.actions.imports.WorkflowImportElementFromArchive;
import org.knime.workbench.explorer.view.actions.imports.WorkflowImportOperation;
import org.knime.workbench.ui.navigator.ZipLeveledStructProvider;

/**
 * An action to temporary extract a workflow archive file into a directory.
 *
 * @author Christian Albrecht, KNIME.com GmbH, Konstanz, Germany
 * @since 7.3
 */
public class TempExtractArchive {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(TempExtractArchive.class);

    private final IProgressMonitor m_monitor;

    private File m_source;

    private final LocalExplorerFileStore m_targetDir;

    private final boolean m_deleteSource;

    private final AfterRunCallback m_afterRunCallback;

    /**
     * Creates an action with the source file and parent directory.
     *
     * @param source the source file containing the KNIME archive
     * @param target the target directory to download the workflow to
     * @param deleteSource if true the source is deleted after a successful download
     * @param afterRunCallback see
     *            {@link AbstractContentProvider#performDownloadAsync(RemoteExplorerFileStore, LocalExplorerFileStore, boolean, AfterRunCallback)}
     *            - may be null.
     */
    public TempExtractArchive(final File source, final LocalExplorerFileStore target, final boolean deleteSource,
        final AfterRunCallback afterRunCallback) {
        this(source, target, deleteSource, afterRunCallback, null);
    }

    /**
     * Creates an action with the source file and parent directory.
     *
     * @param source the source file containing the KNIME archive
     * @param target the target directory to download the workflow to
     * @param deleteSource if true the source is deleted after a successful download
     * @param afterRunCallback see
     *            {@link AbstractContentProvider#performDownloadAsync(RemoteExplorerFileStore, LocalExplorerFileStore, boolean, AfterRunCallback)}
     *            - may be null.
     * @param monitor the progress monitor to use
     */
    public TempExtractArchive(final File source, final LocalExplorerFileStore target, final boolean deleteSource,
        final AfterRunCallback afterRunCallback, final IProgressMonitor monitor) {
        m_source = source;
        m_targetDir = target;
        m_deleteSource = deleteSource;
        m_afterRunCallback = afterRunCallback;
        m_monitor = monitor;
    }

    /**
     * @return the source archive file
     */
    protected File getSourceArchiveFile() {
        return m_source;
    }

    /**
     * Setter for the source archive file
     *
     * @param source the archive file to temporary extract
     */
    protected void setSourceArchiveFile(final File source) {
        m_source = source;
    }

    /**
     * @return the deleteSource
     */
    protected boolean getDeleteSource() {
        return m_deleteSource;
    }

    /**
     * @return the monitor
     */
    protected IProgressMonitor getMonitor() {
        return m_monitor;
    }

    /**
     * @return the afterRunCallback
     */
    protected AfterRunCallback getAfterRunCallback() {
        return m_afterRunCallback;
    }

    /**
     * @return the directory to save the download to
     */
    protected LocalExplorerFileStore getTargetDir() {
        return m_targetDir;
    }

    /**
     * @return a string identifying the download target
     */
    protected String getTargetIdentifier() {
        return m_targetDir.getMountIDWithFullPath();
    }

    /**
     * Perform preparations on the target. Nothing is done in the base implementation.
     */
    protected void prepareTarget() {
        // do nothing
    }

    /**
    *
    */
    protected void refreshTarget() {
        final LocalExplorerFileStore targetDir = getTargetDir();
        LocalExplorerFileStore parent = targetDir.getParent();
        if (parent != null) {
            parent.refresh();
        }
    }

    /**
     * Retrieves the Workflows from the archive and extracts them into the destination
     *
     * @throws Exception
     */
    protected void unpackWorkflowIntoLocalDir() throws Exception {

        if (m_source == null) {
            LOGGER.error("Archive File not set while trying to unpack. Aborting action.");
            return;
        }

        LocalExplorerFileStore destWorkflowDir = m_targetDir.getParent();

        ZipFile zFile = new ZipFile(m_source);
        ZipLeveledStructProvider importStructureProvider = new ZipLeveledStructProvider(zFile);
        importStructureProvider.setStrip(1);

        ZipEntry rootEntry = (ZipEntry)importStructureProvider.getRoot();
        List<ZipEntry> rootChild = importStructureProvider.getChildren(rootEntry);
        if (rootChild.size() == 1) {
            // the zipped workflow normally contains only one dir
            rootEntry = rootChild.get(0);
        }
        WorkflowImportElementFromArchive root = collectWorkflowsFromZipFile(m_source);
        IWorkflowImportElement element = null;
        if (root.getChildren().size() == 1) {
            element = root.getChildren().iterator().next();
        } else {
            element = root;
        }
        // rename the import element
        element.setName(getTargetDir().getName());
        LOGGER.debug("Unpacking workflow \"" + element.getName() + "\" into destination: "
            + destWorkflowDir.getMountIDWithFullPath());
        final WorkflowImportOperation importOp = new WorkflowImportOperation(element, destWorkflowDir);

        try {
            importOp.run(m_monitor);
        } finally {
            importStructureProvider.closeArchive();
        }
    }

    private WorkflowImportElementFromArchive collectWorkflowsFromZipFile(final File zipFile)
        throws ZipException, IOException {
        ILeveledImportStructureProvider provider = null;
        ZipFile sourceFile = new ZipFile(zipFile);
        provider = new ZipLeveledStructureProvider(sourceFile);
        // TODO: store only the workflows (dirs are created automatically)
        Object child = provider.getRoot();
        WorkflowImportElementFromArchive root = new WorkflowImportElementFromArchive(provider, child, 0);
        collectWorkflowsFromProvider(root);
        return root;
    }

    /**
     *
     * @param parent the archive element to collect the workflows from
     * @param monitor progress monitor
     */
    private void collectWorkflowsFromProvider(final WorkflowImportElementFromArchive parent) {
        ILeveledImportStructureProvider provider = parent.getProvider();
        Object entry = parent.getEntry();
        if (parent.isWorkflow() || parent.isTemplate()) {
            // abort recursion
            return;
        }
        List children = provider.getChildren(entry);
        if (children == null) {
            return;
        }
        Iterator childrenEnum = children.iterator();
        while (childrenEnum.hasNext()) {
            Object child = childrenEnum.next();
            if (provider.isFolder(child)) {
                WorkflowImportElementFromArchive childElement =
                    new WorkflowImportElementFromArchive(provider, child, parent.getLevel() + 1);
                collectWorkflowsFromProvider(childElement);
                // either it's a workflow
                if (childElement.isWorkflow()
                    // or it is a workflow group
                    || childElement.isWorkflowGroup()
                    // or it is a workflow template
                    || childElement.isTemplate()) {
                    /* because only workflows, templates and workflow groups are
                     * added */
                    parent.addChild(childElement);
                }
            }
        }
    }

    /**
     * @param monitor the monitor to report progress
     * @throws CoreException if the download does not complete without warnings
     */
    public final void runSync(final IProgressMonitor monitor) throws CoreException {
        try {
            runSyncInternal(monitor);
            AfterRunCallback.callCallbackInDisplayThread(getAfterRunCallback(), null);
        } catch (final Exception e) {
            AfterRunCallback.callCallbackInDisplayThread(getAfterRunCallback(), e);
            if (e instanceof CoreException) {
                throw (CoreException)e;
            } else if (e instanceof RuntimeException) {
                throw (RuntimeException)e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * @param monitor the monitor to report progress
     * @throws CoreException if the download does not complete without warnings
     */
    protected void runSyncInternal(final IProgressMonitor monitor) throws CoreException {

        prepareTarget();

        try {
            unpackWorkflowIntoLocalDir();
        } catch (Exception e) {
            LOGGER.error("Unable to extract the archive file. ", e);
        }

        refreshTarget();

        if (m_deleteSource) {
            if (!m_source.delete()) {
                LOGGER.error("Unable to delete source file.");
            }
        }

    }

}
