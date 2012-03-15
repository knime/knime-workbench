/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2011
 * KNIME.com, Zurich, Switzerland
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.com
 * email: contact@knime.com
 * ---------------------------------------------------------------------
 *
 * History
 *   14.08.2009 (ohl): created
 */
package org.knime.workbench.explorer.view.actions;


import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.internal.wizards.datatransfer.ILeveledImportStructureProvider;
import org.eclipse.ui.internal.wizards.datatransfer.ZipLeveledStructureProvider;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;
import org.knime.workbench.explorer.view.actions.imports.IWorkflowImportElement;
import org.knime.workbench.explorer.view.actions.imports.WorkflowImportElementFromArchive;
import org.knime.workbench.explorer.view.actions.imports.WorkflowImportOperation;
import org.knime.workbench.ui.navigator.ZipLeveledStructProvider;

/**
 *
 * @author ohl, KNIME.com, Zurich, Switzerland
 */
public class LocalDownloadWorkflowAction extends AbstractDownloadAction {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(LocalDownloadWorkflowAction.class);

    /** The action's id. */
    public static final String ID = "com.knime.explorer.downloadaction";


    private final LocalExplorerFileStore m_targetFileStore;

    /**
     * Creates a action with the source and parent directory.
     *
     * @param source the source file store containing the workflow
     * @param target the target directory to download the workflow to
     */
    public LocalDownloadWorkflowAction(final RemoteExplorerFileStore source,
            final File target) {
        this(source, target, null);
    }

    /**
     * Creates a action with the source and parent directory.
     *
     * @param source the source file store containing the workflow
     * @param target the target directory to download the workflow to
     * @param monitor the progress monitor to use
     */
    public LocalDownloadWorkflowAction(final RemoteExplorerFileStore source,
            final File target, final IProgressMonitor monitor) {
        super("Download", source, target, monitor);
        LocalExplorerFileStore fs = ExplorerFileSystem.instance
                .fromLocalFile(getTargetDir());
        m_targetFileStore = fs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected LocalExplorerFileStore getTargetFileStore() {
        return m_targetFileStore;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void prepareTarget() {
        // blow away everything in our way
        LocalExplorerFileStore target = getTargetFileStore();
        if (target.fetchInfo().exists()) {
            LOGGER.info("Download destination exists! Removing it! ("
                    + target.getMountIDWithFullPath() + ")");
            try {
                target.delete(EFS.NONE, null);
                target.toLocalFile().mkdirs();
            } catch (CoreException e) {
                LOGGER.warn("Could not clean up download directory \""
                        + getTargetIdentifier() + "\"", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void extractDownloadToTarget(final File zipFile)
            throws Exception {
        unpackWorkflowIntoLocalDir(getTargetFileStore().getParent(), zipFile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void refreshTarget() {
        getTargetFileStore().refresh();
    }

    private void unpackWorkflowIntoLocalDir(
            final LocalExplorerFileStore destWorkflowDir,
            final File zippedWorkflow) throws Exception {

        ZipFile zFile = new ZipFile(zippedWorkflow);
        ZipLeveledStructProvider importStructureProvider =
                new ZipLeveledStructProvider(zFile);
        importStructureProvider.setStrip(1);

        ZipEntry rootEntry = (ZipEntry)importStructureProvider.getRoot();
        List<ZipEntry> rootChild =
                importStructureProvider.getChildren(rootEntry);
        if (rootChild.size() == 1) {
            // the zipped workflow normally contains only one dir
            rootEntry = rootChild.get(0);
        }
        WorkflowImportElementFromArchive root =
                collectWorkflowsFromZipFile(zippedWorkflow);
        List<IWorkflowImportElement> flows =
                new LinkedList<IWorkflowImportElement>();
        IWorkflowImportElement element = null;
        if (root.getChildren().size() == 1) {
            element = root.getChildren().iterator().next();
        } else {
            element = root;
        }
        // rename the import element
        element.setName(getTargetFileStore().getName());
        flows.add(element);
        LOGGER.debug("Unpacking workflow \"" + element.getName()
                + "\" into destination: "
            + destWorkflowDir.getMountIDWithFullPath());
        final WorkflowImportOperation importOp =
                new WorkflowImportOperation(flows, destWorkflowDir, null);

        try {
            importOp.run(getMonitor());
        } finally {
            importStructureProvider.closeArchive();
        }
    }

    private WorkflowImportElementFromArchive collectWorkflowsFromZipFile(
            final File zipFile) throws ZipException, IOException {
        ILeveledImportStructureProvider provider = null;
        ZipFile sourceFile = new ZipFile(zipFile);
        provider = new ZipLeveledStructureProvider(sourceFile);
        // TODO: store only the workflows (dirs are created automatically)
        final ILeveledImportStructureProvider finalProvider = provider;
        if (provider != null) {
            Object child = finalProvider.getRoot();
            WorkflowImportElementFromArchive root =
                    new WorkflowImportElementFromArchive(finalProvider, child,
                            0);
            collectWorkflowsFromProvider(root);
            return root;
        }
        throw new IllegalStateException(
                "Didn't get a root structure from workflow zip archive");
    }

    /**
     *
     * @param parent the archive element to collect the workflows from
     * @param monitor progress monitor
     */
    private void collectWorkflowsFromProvider(
            final WorkflowImportElementFromArchive parent) {
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
                        new WorkflowImportElementFromArchive(provider, child,
                                parent.getLevel() + 1);
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
}
