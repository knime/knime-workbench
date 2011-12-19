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
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
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

    /**
     *
     * Creates a new action with the given text and the active shell.
     *
     * @param text the string used as the text for the action, or null if there
     *            is no text
     * @param source the source file store containing the workflow
     * @param targetDir the target directory to download the workflow to
     */
    public AbstractDownloadAction(final String text,
            final RemoteExplorerFileStore source, final File targetDir) {
        super(text);
        m_targetDir = targetDir;
        m_source = source;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void run() {
        Shell shell = Display.getDefault().getActiveShell();

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
        final IRunnableWithProgress rwp = new IRunnableWithProgress() {
            @Override
            public void run(final IProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException {
                dwnLoader.run(monitor);
            }
        };
        try {
            new ProgressMonitorDialog(shell).run(true, true, rwp);
        } catch (InvocationTargetException e) {
            LOGGER.info("Download failed.", e);
            return;
        } catch (InterruptedException e) {
            LOGGER.info("Download cancelled by user.");
            return;
        }

        // now wait for the download to finish
        boolean success = false;
        LOGGER.info("Waiting for download to finish... ("+ srcIdentifier + ")");
        success = dwnLoader.waitUntilDone();

        // error handling if download failed
        File tmpZip = dwnLoader.getTempFile();
        if (tmpZip == null || !success) {
            String msg = "Unable to download workflow: ";
            if (success) {
                msg += dwnLoader.getErrorMessage();
                LOGGER.error(msg);
                MessageDialog.openError(shell, "Download Error",
                        msg);
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
        return AbstractExplorerFileStore.isWorkflow(getSourceFile());
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
