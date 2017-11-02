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
 * ------------------------------------------------------------------------
 */
package org.knime.workbench.explorer.filesystem;

import org.eclipse.core.filesystem.provider.FileInfo;

public abstract class AbstractExplorerFileInfo extends FileInfo {

    /**
     * Creates a new file information object. All values except the file name
     * will have default values.
     *
     * @param name The name of this file
     */
    protected AbstractExplorerFileInfo(final String name) {
        super(name);
    }

    /**
     * Creates a new file information object with default values.
     */
    protected AbstractExplorerFileInfo() {
        super();
    }

    /**
     * Checks whether a file represents a workflow group.
     *
     * @return true if the file is a workflow group, false otherwise
     */
    public abstract boolean isWorkflow();

    /**
     * Checks whether a file represents a workflow.
     *
     * @return true if the file is a workflow, false otherwise
     */
    public abstract boolean isWorkflowGroup();

    /**
     * Checks whether a file represents a workflow template.
     *
     * @return true if the file is a workflow template, false otherwise
     */
    public abstract boolean isWorkflowTemplate();

    /**
     * Checks whether a file represents a KNIME node.
     *
     * @return true if the file is a KNIME node, false otherwise
     */
    public abstract boolean isNode();

    /**
     * Checks whether a file represents a KNIME metanode.
     *
     * @return true if the file is a KNIME metanode, false otherwise
     */
    public abstract boolean isMetaNode();

    /**
     * @return <code>true</code>, if the file is a file (not a workflow, group, template, etc., but a plain file)
     * @since 4.0
     */
    public abstract boolean isFile();

    /**
     * @return <code>true</code>, if the item (file/flow/dir) is a system created item.
     * @since 5.0
     */
    public abstract boolean isReservedSystemItem();

    /**
     * @return true if the file store can be modified, false otherwise
     */
    public abstract boolean isModifiable();

    /**
     * @return true if the file store can be read, false otherwise
     */
    public abstract boolean isReadable();


    /**
     * Returns whether the files store is a snapshot or not.
     *
     * @return <code>true</code> if it is a snapshot, <code>false</code> otherwise
     * @since 6.0
     */
    public abstract boolean isSnapshot();
}
