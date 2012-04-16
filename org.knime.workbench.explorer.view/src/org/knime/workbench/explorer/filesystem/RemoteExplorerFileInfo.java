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
  *   Dec 13, 2011 (morent): created
  */

package org.knime.workbench.explorer.filesystem;


/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public abstract class RemoteExplorerFileInfo extends AbstractExplorerFileInfo {
    /**
     * Creates a new file information object with default values.
     */
    protected RemoteExplorerFileInfo() {
        super();
    }

    /**
     * Creates a new file information object. All values except the file name
     * will have default values.
     *
     * @param name The name of this file
     */
    protected RemoteExplorerFileInfo(final String name) {
        super(name);
    }

    /**
     * Checks whether a file represents a workflow job.
     *
     * @return true if the file is a workflow job, false otherwise
     * */
    public abstract boolean isWorkflowJob();

    /**
     * Checks whether a file represents a workflow and it is executable.
     *
     * @return true if the file is a workflow and executable, false otherwise
     */
    public abstract boolean isExecutable();

    /**
     * Checks whether a file represents a workflow or a workflow job
     * and is executed.
     *
     * @return true if the file is a workflow or workflow job and executed,
     *      false otherwise
     */
    public abstract boolean isExecuted();

    /**
     * Checks whether a file represents a workflow or a workflow job
     * and is currently executing.
     *
     * @return true if the file is a workflow or workflow job and executing,
     *      false otherwise
     */
    public abstract boolean isExecuting();

    /**
     * Checks whether a file represents a workflow or a workflow job
     * and is configured.
     *
     * @return true if the file is a workflow or workflow job and configured,
     *      false otherwise
     */
    public abstract boolean isConfigured();

    /**
     * Checks whether a file represents a workflow and is idle.
     *
     * @return true if the file is a workflow and idle, false otherwise
     */
    public abstract boolean isIdle();


}
