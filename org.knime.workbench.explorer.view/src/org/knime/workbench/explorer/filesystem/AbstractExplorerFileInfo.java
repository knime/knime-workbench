/* ------------------------------------------------------------------
  * This source code, its documentation and all appendant files
  * are protected by copyright law. All rights reserved.
  *
  * Copyright, 2008 - 2013
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
  *   Sep 1, 2011 (morent): created
  */

package org.knime.workbench.explorer.filesystem;

import org.eclipse.core.filesystem.provider.FileInfo;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
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
     * Checks whether a file represents a KNIME meta node.
     *
     * @return true if the file is a KNIME meta node, false otherwise
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
     * @since 5.1
     */
    public abstract boolean isSnapshot();
}
