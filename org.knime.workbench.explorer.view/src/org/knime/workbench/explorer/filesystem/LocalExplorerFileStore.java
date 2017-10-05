/*
 * ------------------------------------------------------------------------
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
 * Created: Apr 12, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer.filesystem;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.knime.workbench.explorer.ExplorerActivator;

/**
 *
 * @author ohl, University of Konstanz
 */
public abstract class LocalExplorerFileStore extends AbstractExplorerFileStore {

    /**
     * @param mountID the id of the mount point
     * @param fullPath the full path
     */
    protected LocalExplorerFileStore(final String mountID,
            final String fullPath) {
        super(mountID, fullPath);
    }

    /**
     * Convenience method that calls #toLocalFile(int, IProgressMonitor) with
     * options = EFS.CACHE and monitor = null.
     *
     * @return the local file or null if not supported
     * @throws CoreException if this method fails
     */
    @Override
    public File toLocalFile() throws CoreException {
        // to be called with NONE (bug fix 2887) - the local (work-) space
        // uses org.eclipse.core.internal.filesystem.local.LocalFile, which
        // ignores the options field ... and the SharedSpaceFileStore
        // overwrites to the toLocalFile(int, IProgMon) to return the underlying
        // file (ignores options argument, too)
        return toLocalFile(EFS.NONE, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract LocalExplorerFileStore getChild(String name);

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract LocalExplorerFileStore getParent();

    /**
     * {@inheritDoc}
     */
    @Override
    public File resolveToLocalFile() throws CoreException {
        return toLocalFile();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File resolveToLocalFile(final IProgressMonitor pm) throws CoreException {
        return toLocalFile(EFS.NONE, pm);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copy(final IFileStore destination, final int options, final IProgressMonitor monitor)
            throws CoreException {
                if (this.equals(destination)) {
                    throw new CoreException(new Status(IStatus.ERROR,
                            ExplorerActivator.PLUGIN_ID,
                            "Filestore cannot be copied to itself"));
                }
                File srcFile = toLocalFile(options, monitor);
                File dstFile = destination.toLocalFile(options, monitor);

                if (dstFile == null) {
                    throw new UnsupportedOperationException("The local workspace "
                            + "filestore only allows copying to local destinations but"
                            + " \"" + destination.getName() + "\" is not local.");
                }
                if (dstFile.exists() && ((options & EFS.OVERWRITE) == 0)) {
                    throw new CoreException(new Status(IStatus.ERROR,
                            ExplorerActivator.PLUGIN_ID,
                            "A file of the same name already exists at the copy "
                            + "destination"));
                }

                super.cleanupDestination(destination, options, monitor);
                try {
                    if (srcFile.isDirectory()) {
                        FileUtils.copyDirectory(srcFile, dstFile);
                    } else if (srcFile.isFile()) {
                        FileUtils.copyFile(srcFile, dstFile);
                    }
                } catch (IOException e) {
                    String message =
                            "Could not copy \"" + srcFile.getAbsolutePath()
                                    + "\" to \"" + dstFile.getAbsolutePath() + "\".";
                    throw new CoreException(new Status(IStatus.ERROR,
                            ExplorerActivator.PLUGIN_ID, message, e));
                }
                IFileStore destParent = destination.getParent();
                if (destParent instanceof AbstractExplorerFileStore) {
                    ((AbstractExplorerFileStore)destParent).refresh();
                }
            }
}
