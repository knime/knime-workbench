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
 * Created on 05.11.2013 by thor
 */
package org.knime.workbench.explorer.view.dialogs;

/**
 * Simple class that contains information about overwrite and merge operations, such as whether existing items should be
 * overwritten or whether snapshots should be created or not.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 * @since 6.0
 */
public class OverwriteAndMergeInfo {
    private final String m_newName;

    private final boolean m_merge;

    private final boolean m_overwrite;

    private final boolean m_createSnapshot;

    private final String m_snapshotComment;

    /**
     * Creates a new merge-and-overwrite info object
     *
     * @param newName the new name in case an existing destination should <b>not</b> be overwritten
     * @param merge <code>true</code> if the source group should be merged into the destination group
     * @param overwrite <code>true</code> if existing destination items should be overwritten
     * @param createSnapshot <code>true</code> if a snapshot for each overwritten item should be created
     * @param snapshotComment the optional snapshot comment
     */
    public OverwriteAndMergeInfo(final String newName, final boolean merge, final boolean overwrite, final boolean createSnapshot,
        final String snapshotComment) {
        m_newName = newName;
        m_merge = merge;
        m_overwrite = overwrite;
        m_createSnapshot = createSnapshot;
        m_snapshotComment = snapshotComment;
    }

    /**
     * Returns the new name of the item (in case the target should not be overwritten).
     *
     * @return the new name or <code>null</code> if not applicable
     */
    public String getNewName() {
        return m_newName;
    }

    /**
     * Returns whether the source group should be merged into the destination group.
     *
     * @return <code>true</code> if the groups should be merged, <code>false</code> otherwise
     */
    public boolean merge() {
        return m_merge;
    }

    /**
     * Returns whether the destination item should be overwritten or not.
     *
     * @return <code>true</code> if the destination should be overwritten, <code>false</code> otherwise
     */
    public boolean overwrite() {
        return m_overwrite;
    }

    /**
     * Returns whether a snapshot should be created or not.
     *
     * @return <code>true</code> if a snapshot should be created, <code>false</code> otherwise
     */
    public boolean createSnapshot() {
        return m_createSnapshot;
    }

    /**
     * Returns the comment for the snapshot. Maybe an empty string or <code>null</code> if it is not applicable.
     *
     * @return a comment or <code>null</code>
     */
    public String getComment() {
        return m_snapshotComment;
    }
}
