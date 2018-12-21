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
package org.knime.workbench.explorer.view;

import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.MountPoint;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;

/**
 * Used as object in the treeview of the KNIME Explorer. Wraps the objects
 * provided by the different content providers. Stores the original object and a
 * reference to the creator/provider. <br>
 * (The original approach with a hash map doesn't work because the original
 * objects may not implement equals. Actually the eclipse resources don't.)
 *
 * @author ohl, University of Konstanz
 */
public final class ContentObject implements IFileStoreProvider{

    private final AbstractExplorerFileStore m_obj;

    private final AbstractContentProvider m_creator;

    /**
     * @param creator
     * @param o
     */
    ContentObject(final AbstractContentProvider creator,
            final AbstractExplorerFileStore o) {
        if (creator == null) {
            throw new NullPointerException(
                    "Creator (provider) can't be null for Object " + o);
        }
        m_creator = creator;
        m_obj = o;
    }

    /**
     * @return the wrapped object
     */
    public AbstractExplorerFileStore getObject() {
        return m_obj;
    }

    /**
     * @return the creator of the wrapped object.
     */
    public AbstractContentProvider getProvider() {
        return m_creator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_obj.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ContentObject) {
            ContentObject co = (ContentObject)obj;
            if (m_creator != co.m_creator) {
                return false;
            }
            if (m_obj == co.m_obj) {
                return true;
            }
            if (m_obj != null) {
                return m_obj.equals(co.getObject());
            }
            return false;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_obj.hashCode();
    }

    /**
     * @param file to wrap into a into a content object
     * @return an object used in the explorer view or null, if the file refers
     *         to a non existing mount point. NOTE: The explorer view does not
     *         store root file objects (with path "/"). But this method returns
     *         a content object if a file with path "/" is passed.
     * @see ContentDelegator#getTreeObjectFor(AbstractExplorerFileStore)
     *
     */
    public static ContentObject forFile(final AbstractExplorerFileStore file) {
        if (file == null) {
            return null;
        }
        String id = file.getMountID();
        MountPoint mp = ExplorerMountTable.getMountPoint(id);
        if (mp == null) {
            return null;
        }
        return new ContentObject(mp.getProvider(), file);
    }

    /**
     * {@inheritDoc}
     * @since 7.2
     */
    @Override
    public AbstractExplorerFileStore getFileStore() {
        return m_obj;
    }
}
