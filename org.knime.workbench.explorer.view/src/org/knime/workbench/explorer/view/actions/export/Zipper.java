/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (c) KNIME.com, Zurich, Switzerland
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
 * Created: Jun 2, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer.view.actions.export;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

/**
 *
 * @author ohl, University of Konstanz
 */
public class Zipper {

    private static final int BUFFSIZE = 1024 * 2048;

    private static final int COMPR_LEVEL = 9;

    /**
     * Compresses multiple files into one archive. Pass only files in the list!
     * Allows for removing leading path segments of each file's path.
     *
     * @param files files (no dirs) to add to the archive
     * @param outputFile the compressed output archive
     * @param stripOff number of segments in the path of each file that are
     *            stripped off before storing (if zero or negative nothing is
     *            stripped off). The device is always removed.
     * @param mon to report progress and check for cancellation (can be null)
     * @throws IOException if an an I/O error occurred, the user canceled, one
     *             of the specified didn't exist or anything else went wrong. It
     *             tries to delete the partially created output file before
     *             then.
     */
    public static void zipFiles(final Collection<File> files,
            final File outputFile, final int stripOff,
            final IProgressMonitor mon) throws IOException {

        IProgressMonitor monitor = mon;
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }

        byte[] buf = new byte[BUFFSIZE];
        ZipOutputStream zout =
                new ZipOutputStream(new BufferedOutputStream(
                        new FileOutputStream(outputFile), BUFFSIZE));
        IOException ioException = null;

        try {
            if (files.size() == 0) {
                ioException = null;
                // cleanup done in the finally block
                return;
            }

            zout.setLevel(COMPR_LEVEL);
            if (mon != null) {
                // set the overall progress to the overall megabyte
                int wrk = 0;
                for (File f : files) {
                    int megaBytes = (int)(f.length() >>> 20);
                    wrk += megaBytes + 1;
                }
                monitor.beginTask("Compressing " + files.size() + " files...",
                        wrk);
            }
            for (File f : files) {
                if (f == null) {
                    ioException =
                            new IOException("Illegal file in archive list: "
                                    + "<null>!");
                    // cleanup done in the finally block
                    return;
                }
                if (f.isDirectory()) {
                    ioException =
                            new IOException("Illegal file in archive list: "
                                    + "directory (" + f.getAbsolutePath()
                                    + ").");
                    // cleanup done in the finally block
                    return;
                }
                if (monitor.isCanceled()) {
                    ioException = new IOException("Canceled.");
                    // cleanup done in the finally block
                    return;
                }

                IPath path = new Path(f.getAbsolutePath()).setDevice(null);
                if (stripOff > 0 && path.segmentCount() > stripOff) {
                    path = path.removeFirstSegments(stripOff);
                }
                String entryName = path.makeRelative().toString();
                InputStream in = null;
                try {
                    in =
                            new BufferedInputStream(new FileInputStream(f),
                                    BUFFSIZE);
                    zout.putNextEntry(new ZipEntry(entryName));
                    int read;
                    while ((read = in.read(buf)) >= 0) {
                        if (monitor.isCanceled()) {
                            ioException =
                                    new IOException("Canceled.");
                            // cleanup done in the finally block
                            return;
                        }
                        zout.write(buf, 0, read);
                    }
                } finally {
                    zout.closeEntry();
                    in.close();
                }
                int megaBytes = (int)(f.length() >>> 20);
                monitor.worked(megaBytes + 1);
            }
        } finally {
            monitor.done();
            zout.close();
            if (ioException != null) {
                outputFile.delete();
                throw ioException;
            }
        }

    }
}
