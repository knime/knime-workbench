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
final class Zipper {

    private static final int BUFFSIZE = 1024 * 2048;

    private static final int COMPR_LEVEL = 9;

    /**
     * Compresses multiple files into one archive. Pass only files in the list! Allows for removing leading path
     * segments of each file's path.
     *
     * @param files files (no dirs) to add to the archive
     * @param outputFile the compressed output archive
     * @param stripOff number of segments in the path of each file that are stripped off before storing (if zero or
     *            negative nothing is stripped off). The device is always removed.
     * @param mon to report progress and check for cancellation (can be null)
     * @throws IOException if an an I/O error occurred, the user canceled, one of the specified didn't exist or anything
     *             else went wrong. It tries to delete the partially created output file before then.
     */
    public static void zipFiles(final Collection<File> files, final File outputFile, final int stripOff,
        final IProgressMonitor mon) throws IOException {

        IProgressMonitor monitor = mon;
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }

        byte[] buf = new byte[BUFFSIZE];
        IOException ioException = null;

        try (FileOutputStream outStream = new FileOutputStream(outputFile);
                ZipOutputStream zout = new ZipOutputStream(new BufferedOutputStream(outStream, BUFFSIZE))) {
            if (files.size() == 0) {
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
                monitor.beginTask("Compressing " + files.size() + " files...", wrk);
            }
            for (File f : files) {
                if (f == null) {
                    ioException = new IOException("Illegal file in archive list: <null>!");
                    // cleanup done in the finally block
                    return;
                }
                if (f.isDirectory()) {
                    ioException =
                        new IOException("Illegal file in archive list: directory (" + f.getAbsolutePath() + ").");
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
                if (f.length() == 0) {
                    // this is mainly for the .knimeLock file of open workflows; the file is locked and windows forbids
                    // mmap-ing locked files but FileInputStream seems to mmap files which leads to exceptions while
                    // reading the (non-existing) contents of the file
                    zout.putNextEntry(new ZipEntry(entryName));
                    zout.closeEntry();
                } else {
                    try (InputStream in = new BufferedInputStream(new FileInputStream(f), BUFFSIZE)) {
                        zout.putNextEntry(new ZipEntry(entryName));
                        int read;
                        while ((read = in.read(buf)) >= 0) {
                            if (monitor.isCanceled()) {
                                ioException = new IOException("Canceled.");
                                // cleanup done in the finally block
                                return;
                            }
                            zout.write(buf, 0, read);
                        }
                    } catch (IOException ioe) {
                        ioException = new IOException(String.format("Unable to add file \"%s\" to archive \"%s\": %s",
                            f.getAbsolutePath(), outputFile.getAbsoluteFile(), ioe.getMessage()), ioe);

                    } finally {
                        zout.closeEntry();
                    }
                }
                int megaBytes = (int)(f.length() >>> 20);
                monitor.worked(megaBytes + 1);
            }
        } catch (IOException ioe) {
            ioException = ioe; // catch it to have variable assigned for finally block
        } finally {
            monitor.done();
            if (ioException != null) {
                outputFile.delete();
                throw ioException;
            }
        }

    }
}
