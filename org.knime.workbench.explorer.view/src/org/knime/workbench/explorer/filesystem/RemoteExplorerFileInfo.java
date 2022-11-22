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
package org.knime.workbench.explorer.filesystem;

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

    /**
     * @return true, if the workflow of this job got overwritten after the job was created
     * @since 8.7
     */
    public abstract boolean isOutDated();

    /**
     * Checks whether the job is vanished, e.g. due to a crashed executor.
     *
     * @return <code>true</code> if the job is vanished, <code>false</code> otherwise
     * @since 5.0
     */
    public abstract boolean isVanished();

    /**
     * Checks whether a file represents a workflow containing a report.
     * @return true if the file is a workflow and contains a report, false
     *      otherwise
     * @since 3.1
     */
    public abstract boolean isReportWorkflow();


    /**
     * Checks whether a file represents a scheduled ẃorkflow job.
     *
     * @return true, if the file is a scheduled job, false otherwise.
     *
     * @since 8.3
     */
    public abstract boolean isScheduledFlow();

    /**
     * Checks whether a file has node messages.
     *
     * @return true, if the file has node messages, false otherwise.
     *
     * @since 8.3
     */
    public abstract boolean hasNodeMessages();

    /**
     * Returns the owner of the item.
     *
     * @return The owner of the item.
     *
     * @since 8.3
     */
    public abstract String getOwner();

    /**
     * Checks whether the file is of unknown type.
     *
     * @return <code>true</code> if the type is unkown, <code>false</code> otherwise.
     *
     * @since 8.5
     */
    public boolean isUnknownType() {
        return false;
    }

    /**
     * Checks whether the item is a user root in the Hub, i.e. <code>/Users/user:&#60id&#62</code>
     *
     * @return <code>true</code> if it's a user root, <code>false</code> otherwise
     */
    public boolean isUserRoot() {
        return false;
    }

    /**
     * Checks whether the item is a team root in the Hub, i.e. <code>/Users/team:&#60id&#62</code>
     *
     * @return <code>true</code> if it's a team root, <code>false</code> otherwise
     */
    public boolean isTeamRoot() {
        return false;
    }
}
