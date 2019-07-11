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
 * Created on 28.11.2012 by ohl
 */
package org.knime.workbench.explorer.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.knime.workbench.explorer.ExplorerActivator;

/**
 * Represents a local (java.io) file wrapped in a local explorer file store. Note, this is not meant to be used in mount
 * points (as it never has a valid mount id). It should only be used in methods that need the provided interfaces to
 * handle local files.
 *
 * @author Peter Ohl, KNIME.com AG, Switzerland
 * @since 4.0
 */
public class TmpLocalExplorerFile extends LocalExplorerFileStore {

    private final File m_wrappedFile;
    private final boolean m_mimicFlowOrTemplate;
    private AbstractExplorerFileInfo m_info;

    /**
     * @param wrappedFile the wrapped file
     *
     */
    public TmpLocalExplorerFile(final File wrappedFile) {
        this(wrappedFile, false);
    }

    /**
     * @param wrappedFile the wrapped file
     * @param mimicFlowOrTemplate If set to <code>true</code>, the file info will return <code>true</code> when
     * its {@link AbstractExplorerFileInfo#isMetaNode()} or {@link AbstractExplorerFileInfo#isWorkflow()} get called.
     * @since 8.1
     */
    public TmpLocalExplorerFile(final File wrappedFile, final boolean mimicFlowOrTemplate) {
        super("", wrappedFile.getAbsolutePath());
        m_wrappedFile = wrappedFile;
        m_mimicFlowOrTemplate = mimicFlowOrTemplate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File toLocalFile() throws CoreException {
        return m_wrappedFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File toLocalFile(final int options, final IProgressMonitor monitor) throws CoreException {
        return toLocalFile();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return m_wrappedFile.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalExplorerFileStore getChild(final String name) {
        return new TmpLocalExplorerFile(new File(m_wrappedFile, name));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalExplorerFileStore getParent() {
        return new TmpLocalExplorerFile(m_wrappedFile.getParentFile());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] childNames(final int options, final IProgressMonitor monitor) throws CoreException {
        return m_wrappedFile.list();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractExplorerFileInfo fetchInfo() {
        if (m_info == null) {
            m_info = new AbstractExplorerFileInfo() {

                {
                    setDirectory(m_mimicFlowOrTemplate);
                }

                @Override
                public boolean isWorkflowTemplate() {
                    return m_mimicFlowOrTemplate;
                }

                @Override
                public boolean isWorkflowGroup() {
                    return false;
                }

                @Override
                public boolean isWorkflow() {
                    return m_mimicFlowOrTemplate;
                }

                @Override
                public boolean isReadable() {
                    return true;
                }

                @Override
                public boolean isNode() {
                    return false;
                }

                @Override
                public boolean isModifiable() {
                    return false;
                }

                @Override
                public boolean isMetaNode() {
                    return false;
                }

                @Override
                public boolean isFile() {
                    return m_wrappedFile.isFile();
                }

                @Override
                public boolean exists() {
                    return m_wrappedFile.exists();
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public boolean isReservedSystemItem() {
                    return false;
                }

                @Override
                public boolean isSnapshot() {
                    return false;
                }
            };
        }
        return m_info;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream openInputStream(final int options, final IProgressMonitor monitor) throws CoreException {
        try {
            return new FileInputStream(m_wrappedFile);
        } catch (FileNotFoundException e) {
            throw new CoreException(new Status(IStatus.ERROR, ExplorerActivator.PLUGIN_ID, e.getMessage(), e));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractExplorerFileStore mkdir(final int options, final IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Not available for temp upload files.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputStream openOutputStream(final int options, final IProgressMonitor monitor) throws CoreException {
        try {
            return new FileOutputStream(m_wrappedFile);
        } catch (FileNotFoundException e) {
            throw new CoreException(new Status(IStatus.ERROR, ExplorerActivator.PLUGIN_ID, e.getMessage(), e));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void move(final IFileStore destination, final int options, final IProgressMonitor monitor)
            throws CoreException {
        throw new UnsupportedOperationException("Not available for temp upload files.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final int options, final IProgressMonitor monitor) throws CoreException {
        throw new UnsupportedOperationException("Not available for temp upload files.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        return m_wrappedFile.equals(obj);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_wrappedFile.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refresh() {
        throw new UnsupportedOperationException("Not available for temp upload files.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_wrappedFile.getAbsolutePath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IFileStore getNativeFilestore() {
        return this;
    }
}
