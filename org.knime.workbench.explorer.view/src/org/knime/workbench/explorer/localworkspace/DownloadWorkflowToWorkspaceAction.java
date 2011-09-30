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
package org.knime.workbench.explorer.localworkspace;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.wizards.datatransfer.ILeveledImportStructureProvider;
import org.eclipse.ui.internal.wizards.datatransfer.ZipLeveledStructureProvider;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;
import org.knime.workbench.explorer.filesystem.RemoteFlowDownloadStream;
import org.knime.workbench.explorer.view.actions.imports.IWorkflowImportElement;
import org.knime.workbench.explorer.view.actions.imports.WorkflowImportElementFromArchive;
import org.knime.workbench.explorer.view.actions.imports.WorkflowImportOperation;
import org.knime.workbench.ui.navigator.ZipLeveledStructProvider;

/**
 *
 * @author ohl, KNIME.com, Zurich, Switzerland
 */
public class DownloadWorkflowToWorkspaceAction extends Action {

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(DownloadWorkflowToWorkspaceAction.class);

    private static final ImageDescriptor IMG_DOWNLOAD = AbstractUIPlugin
            .imageDescriptorFromPlugin(ExplorerActivator.PLUGIN_ID,
                    "icons/download_wf.png");

    /** action's id */
    public static final String ID =
            "com.knime.explorer.teamspace.downloadaction";

    private final RemoteExplorerFileStore m_source;

    private final LocalExplorerFileStore m_parent;

    private final Shell m_parentShell;

    /**
     * @param viewer
     */
    public DownloadWorkflowToWorkspaceAction(final Shell parentShell,
            final RemoteExplorerFileStore source,
            final LocalExplorerFileStore parent) {
        super("Download Workflow...");
        setImageDescriptor(IMG_DOWNLOAD);
        if (parentShell == null) {
            m_parentShell = Display.getCurrent().getActiveShell();
        } else {
            m_parentShell = parentShell;
        }
        m_source = source;
        m_parent = parent;
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
    public boolean isEnabled() {
        return m_parent != null && m_source != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        try {
            if (!AbstractExplorerFileStore.isWorkflow(m_source)) {
                LOGGER.error("Only workflows can be downloaded.");
                return;
            }
            LOGGER.debug("Downloading " + m_source.getMountIDWithFullPath()
                    + " into local destination "
                    + m_parent.getMountIDWithFullPath());
            LocalExplorerFileStore target =
                    (LocalExplorerFileStore)m_parent.getChild(m_source
                            .getName());

            final DownloadRunnable dwnLoader = new DownloadRunnable(m_source);
            final IRunnableWithProgress rwp = new IRunnableWithProgress() {
                @Override
                public void run(final IProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException {
                    dwnLoader.run(monitor);
                }
            };
            new ProgressMonitorDialog(m_parentShell).run(true, true, rwp);

            // now wait for the download to finish
            boolean success = false;
            LOGGER.info("Waiting for download to finish... ("
                    + m_source.getMountIDWithFullPath() + ")");
            success = dwnLoader.waitUntilDone();

            File tmpZip = dwnLoader.getTempFile();
            if (tmpZip == null || !success) {
                String msg = "Unable to download workflow: ";
                if (success) {
                    msg += dwnLoader.getErrorMessage();
                    LOGGER.error(msg);
                    MessageDialog.openError(m_parentShell, "Download Error",
                            msg);
                } else {
                    msg += " Download interrupted.";
                    LOGGER.warn(msg);
                }
                if (target.fetchInfo().exists()) {
                    LOGGER.info("Existing destination not modified ("
                            + target.getMountIDWithFullPath() + ") ");
                }
                return;

            }

            // blow away everything in our way
            if (target.fetchInfo().exists()) {
                LOGGER.info("Download destination exists! Removing it! ("
                        + target.getMountIDWithFullPath() + ")");
                target.delete(EFS.NONE, null);
//                 target.mkdir(EFS.NONE, null); // mkdir still shallow
                target.toLocalFile().mkdirs();
            }

            unpackWorkflowIntoLocalDir(m_parent, tmpZip);
            tmpZip.delete();
            target.refresh();
        } catch (Exception e) {
            String emsg = e.getMessage();
            if (emsg == null || emsg.isEmpty()) {
                emsg = "<no details>";
            }
            String msg = "Unable to store downloaded workflow in ";
            LOGGER.error(msg, e);
            MessageDialog.openError(m_parentShell, "Download Error", msg);
        }
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
        LOGGER.debug("Unpacking workflow. Destination:"
                + destWorkflowDir.getMountIDWithFullPath());
        WorkflowImportElementFromArchive root =
                collectWorkflowsFromZipFile(zippedWorkflow);
        List<IWorkflowImportElement> flows =
                new LinkedList<IWorkflowImportElement>();
        if (root.getChildren().size() == 1) {
            flows.add(root.getChildren().iterator().next());
        } else {
            flows.add(root);
        }
        final WorkflowImportOperation importOp =
                new WorkflowImportOperation(flows, destWorkflowDir,
                        m_parentShell);

        try {
            PlatformUI.getWorkbench().getProgressService()
                    .busyCursorWhile(new IRunnableWithProgress() {

                        @Override
                        public void run(final IProgressMonitor monitor)
                                throws InvocationTargetException,
                                InterruptedException {
                            importOp.run(monitor);
                        }
                    });
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
        if (parent.isWorkflow()) {
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
                        || childElement.isWorkflowGroup()) {
                    // because only workflows and workflow groups are added
                    parent.addChild(childElement);
                }
            }
        }
    }

    public static class DownloadRunnable implements Runnable {

        private final RemoteExplorerFileStore m_source;

        private final AtomicBoolean m_cancel = new AtomicBoolean(false);

        // this is also used as object to wait for the runnable to finish
        private final AtomicBoolean m_done = new AtomicBoolean(false);

        private File m_tmpFile;

        private String m_errorMsg;

        /**
         *
         */
        public DownloadRunnable(final RemoteExplorerFileStore source) {
            if (source == null) {
                throw new NullPointerException("Download source can't be null");
            }
            m_source = source;
        }

        /**
         * {@inheritDoc}
         */
        public void run() {
            run(null);
        }

        public void run(final IProgressMonitor monitor) {

            try {
                if (monitor != null) {
                    StringBuilder progMsg = null;
                    progMsg = new StringBuilder("Compressing workflow ");
                    progMsg.append(m_source.getFullName());
                    progMsg.append(" on the server. ");
                    monitor.setTaskName(progMsg.toString());
                    // we progress over kilobytes in case people download
                    // flows larger than 4GB. Have fun.
                    monitor.beginTask(progMsg.toString(),
                            IProgressMonitor.UNKNOWN);
                }
                RemoteFlowDownloadStream in =
                        m_source.openWorkflowDownloadStream();
                // wait for the server to finish zipping
                while (!in.readyForDownload()) {
                    if (monitor != null && monitor.isCanceled()) {
                        m_cancel.set(true);
                    }
                    if (m_cancel.get()) {
                        m_tmpFile = null;
                        m_errorMsg = "Canceled.";
                        // cancel server
                        in.close();
                        return;
                    }
                    Thread.sleep(1000);
                }
                if (monitor != null) {
                    int kbyte = IProgressMonitor.UNKNOWN;
                    long l = in.length();
                    if (l >= 0) {
                        kbyte = (int)(l >> 10);
                    }
                    StringBuilder progMsg = null;
                    progMsg = new StringBuilder("Downloading workflow ");
                    progMsg.append(m_source.getFullName());
                    progMsg.append(" (" + (kbyte >> 10) + "MB).");
                    monitor.setTaskName(progMsg.toString());
                    // we progress over kilobytes in case people download
                    // flows larger than 4GB. Have fun.
                    monitor.beginTask(progMsg.toString(), kbyte);
                }
                m_tmpFile = File.createTempFile("KNIMEServerDownload", ".tmp");
                LOGGER.debug("Received server download stream for '" + m_source
                        + "', storing it '" + m_tmpFile.getAbsolutePath() + "'");
                BufferedInputStream inStream = null;
                FileOutputStream outStream = null;
                try {
                    inStream = new BufferedInputStream(in, 1024 * 1024);
                    outStream = new FileOutputStream(m_tmpFile);
                    int b;
                    byte[] buffer = new byte[1024 * 1024];
                    while ((b = inStream.read(buffer)) >= 0) {
                        outStream.write(buffer, 0, b);
                        if (monitor != null) {
                            monitor.worked(b >> 10);
                            if (monitor.isCanceled()) {
                                m_cancel.set(true);
                            }
                        }
                        if (m_cancel.get()) {
                            m_tmpFile.delete();
                            m_tmpFile = null;
                            m_errorMsg = "Canceled.";
                            return;
                        }
                    }
                } finally {
                    try {
                        if (inStream != null) {
                            inStream.close();
                        }
                    } catch (Exception ee) {
                        // not closing then...
                    }
                    try {
                        if (outStream != null) {
                            outStream.close();
                        }
                    } catch (Exception ee) {
                        // not closing then...
                    }
                }
                m_errorMsg = null;
            } catch (Throwable e) {
                m_tmpFile = null;
                m_errorMsg = e.getMessage();
            } finally {
                done();
            }
        }

        /**
         * Doesn't cancel server side activities and doesn't interrupt until
         * server response is received.
         */
        public void cancel() {
            m_cancel.set(true);
        }

        private void done() {
            synchronized (m_done) {
                m_done.set(true);
                m_done.notifyAll();
            }
        }

        public boolean waitUntilDone() {
            synchronized (m_done) {
                if (m_done.get()) {
                    return !m_cancel.get();
                }
                try {
                    m_done.wait();
                } catch (InterruptedException e) {
                    return !m_cancel.get();
                }
                return !m_cancel.get();
            }
        }

        public boolean finished() {
            synchronized (m_done) {
                return m_done.get();
            }
        }

        /**
         *
         * @return null until the jobs finishes. If successful a file with the
         *         archive. If unsuccessfully zipped, also null, and the error
         *         message is set.
         */
        public File getTempFile() {
            synchronized (m_done) {
                if (m_done.get()) {
                    return m_tmpFile;
                }
                return null;
            }
        }

        /**
         * Should contain something if {@link #getTempFile()} returns null.
         *
         * @return
         */
        public String getErrorMessage() {
            return m_errorMsg;
        }
    }

}
