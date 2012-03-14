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
  *   Oct 28, 2011 (morent): created
  */

package org.knime.workbench.explorer.view.actions;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;
import org.knime.workbench.explorer.filesystem.RemoteFlowDownloadStream;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public abstract class AbstractDownloadAction extends Action {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            AbstractDownloadAction.class);

    private static final ImageDescriptor IMG_DOWNLOAD = AbstractUIPlugin
            .imageDescriptorFromPlugin(ExplorerActivator.PLUGIN_ID,
            "icons/download_wf.png");

    private final File m_targetDir;
    private final RemoteExplorerFileStore m_source;

    private final IProgressMonitor m_monitor;

    /**
     *
     * Creates a new action with the given text without a progress monitor.
     *
     * @param text the string used as the text for the action, or null if there
     *            is no text
     * @param source the source file store containing the workflow
     * @param targetDir the target directory to download the workflow to
     */
    public AbstractDownloadAction(final String text,
            final RemoteExplorerFileStore source, final File targetDir) {
        this(text, source, targetDir, null);
    }

    /**
    *
    * Creates a new action with the given text and the active shell.
    *
    * @param text the string used as the text for the action, or null if there
    *            is no text
    * @param source the source file store containing the workflow
    * @param targetDir the target directory to download the workflow to
    * @param monitor the progress monitor to use
    */
   public AbstractDownloadAction(final String text,
           final RemoteExplorerFileStore source, final File targetDir,
           final IProgressMonitor monitor) {
       super(text);
       m_targetDir = targetDir;
       m_source = source;
       m_monitor = monitor;
   }



    /**
     * {@inheritDoc}
     */
    @Override
    public final void run() {
        if (m_monitor == null) {
            try {
                PlatformUI.getWorkbench().getProgressService()
                        .busyCursorWhile(new IRunnableWithProgress() {
                            /**
                             * {@inheritDoc}
                             */
                            @Override
                            public void run(final IProgressMonitor monitor)
                                    throws InvocationTargetException,
                                    InterruptedException {
                                try {
                                    runSync(monitor);
                                } catch (Exception e) {
                                    // handled outside
                                    throw new RuntimeException(e);
                                }
                            }
                        });
            } catch (Exception e) {
                LOGGER.error("Upload error: " + e.getMessage(), e);
            }
        } else {
            try {
                runSync(m_monitor);
            } catch (Exception e) {
                // handled outside
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * @param monitor the monitor to report progress
     * @throws CoreException if the download does not complete without warnings
     */
    public final void runSync(final IProgressMonitor monitor)
            throws CoreException {
        String srcIdentifier = getSourceFile().getMountIDWithFullPath();
        if (!isSourceSupported()) {
            throw new IllegalArgumentException("Download source \""
                    + srcIdentifier
                    + "\" is not supported.");
        }
        LOGGER.debug("Downloading " + srcIdentifier
                + " into local destination " + getTargetIdentifier());

        final DownloadRunnable dwnLoader = new DownloadRunnable(
                getSourceFile());
        dwnLoader.run(monitor);

        // now wait for the download to finish
        boolean success = false;
        LOGGER.info("Waiting for download to finish...(" + srcIdentifier + ")");
        success = dwnLoader.waitUntilDone();

        // error handling if download failed
        File tmpZip = dwnLoader.getTempFile();
        if (tmpZip == null || !success) {
            String msg = "Unable to download workflow: ";
            if (success) {
                msg += dwnLoader.getErrorMessage();
                LOGGER.error(msg);
            } else {
                msg += " Download interrupted.";
                LOGGER.warn(msg);
            }
            if (getTargetDir().exists()) {
                LOGGER.info("Existing destination not modified ("
                        + getTargetIdentifier() + ") ");
            }
            return;
        }

        prepareTarget();

        try {
            extractDownloadToTarget(tmpZip);
        } catch (Exception e) {
            LOGGER.error("Unable to extract the download. ", e);
        } finally {
            tmpZip.delete();
        }

        refreshTarget();
        Status status = dwnLoader.getStatus();
        if (status != null) {
            throw new CoreException(status);
        }
    }

    /**
     * Perform preparations on the target. Nothing is done
     * in the base implementation.
     */
    protected void prepareTarget() {
        // do nothing
    }

    /**
     * Unpack the downloaded zip file to the target.
     * @param zipFile the zip file
     * @throws Exception if something goes wrong while extracting the file
     */
    protected abstract void extractDownloadToTarget(File zipFile)
            throws Exception;

    /**
     * Perform any necessary update actions on the target. Nothing is done
     * in the base implementation. Override this method to refresh the file
     * system.
     */
    protected void refreshTarget() {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return getTargetDir() != null && getSourceFile() != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return IMG_DOWNLOAD;
    }


    /**
     * @return the directory to save the download to
     */
    protected File getTargetDir() {
        return m_targetDir;
    }

    /**
     * @return the local explorer file store corresponding to the target
     *      directory, or null if none exists
     */
    protected LocalExplorerFileStore getTargetFileStore() {
        return null;
    }

    /**
     * @return a string identifying the download target
     */
    protected String getTargetIdentifier() {
        LocalExplorerFileStore fs = getTargetFileStore();
        if (fs == null) {
            return m_targetDir.getAbsolutePath();
        } else {
            return fs.getMountIDWithFullPath();
        }
    }

    /**
     * @return the file store to download
     */
    protected RemoteExplorerFileStore getSourceFile() {
        return m_source;
    }

    /**
     * @return true if the download source provided by {@link #getSourceFile()}
     *      represents a workflow
     */
    protected boolean isSourceSupported() {
        RemoteExplorerFileStore sourceFile = getSourceFile();
        return AbstractExplorerFileStore.isWorkflow(sourceFile)
                || AbstractExplorerFileStore.isWorkflowGroup(sourceFile);
// copying of workflow jobs is disabled until implemented on server
//        RemoteExplorerFileInfo info = getSourceFile().fetchInfo();
//        return info.isWorkflow()
//            || (info.isWorkflowJob() && !info.isExecuting());
    }

    /**
     * @return a message containing the supported source types
     */
    protected String getUnsupportedSourceMessage() {
        return "Only worklows can be downloaded.";
    }

    /**
     * @return the locally downloaded file
     */
    public File getDownload() {
        return new File(m_targetDir, m_source.getName());
    }

    /**
     * @return the progress monitor
     */
    protected IProgressMonitor getMonitor() {
        return m_monitor;
    }


    //=========================================================================

    /**
     * Downloads a remote file store to a local temp dir.
     *
     * @author Peter Ohl, KNIME.com, Zurich, Switzerland
     *
     */
    protected static class DownloadRunnable implements Runnable {
        private static final NodeLogger LOGGER = NodeLogger.getLogger(
                DownloadRunnable.class);

        private final RemoteExplorerFileStore m_source;

        private final AtomicBoolean m_cancel = new AtomicBoolean(false);

        // this is also used as object to wait for the runnable to finish
        private final AtomicBoolean m_done = new AtomicBoolean(false);

        private File m_tmpFile;

        private String m_errorMsg;

        private MultiStatus m_status = null;

        /**
         * Returns the collected status of the download operation. If some
         * items could not be downloaded, e.g. due to missing permissions,
         * they are collected as a MultiStatus.
         *
         * @return the status of the download or null if no status messages are available
         */
        public Status getStatus() {
            return m_status;
        }

        /**
         * @param source the file store to download
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
        @Override
        public void run() {
        	run(null);
        }

        /**
         * @param monitor the progress monitor
         */
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
                        + "', storing it '"
                        + m_tmpFile.getAbsolutePath() + "'");
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
                List<String> messages = null;
                try {
                    messages = in.getMessages();
                } catch (Exception e) {
                    messages = Collections.emptyList();
                    LOGGER.error("Could not retrieve download messages.", e);
                }

                if (messages.size() > 0) {
                    final List<IStatus> result = new LinkedList<IStatus>();
                    for (String msg : messages) {
                        result.add(new Status(IStatus.WARNING,
                                ExplorerActivator.PLUGIN_ID, msg));
                    }
                    m_status = new MultiStatus(
                            ExplorerActivator.PLUGIN_ID,
                            IStatus.WARNING, result.toArray(new IStatus[0]),
                            "Could not download all contained files due to "
                            + "missing permissions. Skipped items:", null);
                }
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

        /**
         * Waits until the download has finished.
         *
         * @return true if the download has successfully finished, false
         *      if it has been canceled
         */
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

        /**
         * @return true if the download has successfully finished, false
         *      otherwise
         */
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
