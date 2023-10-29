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
 * -------------------------------------------------------------------
 *
 * History
 *   13.04.2011 (wiswedel): created
 */
package org.knime.workbench.editor2;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContainerTemplate;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.node.workflow.WorkflowPersistor.NodeContainerTemplateLinkUpdateResult;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.workbench.KNIMEEditorPlugin;

/**
 * Runnable used to update a single metanode template link.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class UpdateMetaNodeTemplateRunnable extends PersistWorkflowRunnable {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(UpdateMetaNodeTemplateRunnable.class);

    /** The host WFM. */
    private WorkflowManager m_parentWFM;
    /** The IDs of the metanode links to be updated. */
    private NodeID[] m_ids;
    /** The IDs of the metanodes after update (very very likely correspond
     * to m_ids except in case of errors. */
    private List<NodeID> m_newIDs;
    /** The undo persistors of the previously deleted metanodes. */
    private List<WorkflowPersistor> m_undoPersistors;

    /**
     * @param wfm target workflow (where to insert)
     * @param ids The ID of the metanode to update
     */
    public UpdateMetaNodeTemplateRunnable(final WorkflowManager wfm, final NodeID[] ids) {
        m_parentWFM = wfm;
        m_ids = ids.clone();
    }

    @Override
    public void run(final IProgressMonitor pm) throws InterruptedException {
        m_newIDs = new ArrayList<NodeID>();
        m_undoPersistors = new ArrayList<WorkflowPersistor>();
        // create progress monitor
        ProgressHandler progressHandler = new ProgressHandler(pm, 101, "Updating node links...");
        final CheckCancelNodeProgressMonitor progressMonitor = new CheckCancelNodeProgressMonitor(pm);
        progressMonitor.addProgressListener(progressHandler);
        final Display d = Display.getDefault();
        ExecutionMonitor exec = new ExecutionMonitor(progressMonitor);
        IStatus[] stats = new IStatus[m_ids.length];
        for (int i = 0; i < m_ids.length; i++) {
            NodeID id = m_ids[i];
            if (!needsUpdate(id)) {
                stats[i] = new Status(IStatus.OK, KNIMEEditorPlugin.PLUGIN_ID,
                    "Node with ID \"" + id + "\" already has been updated.", null);
                continue;
            }
            NodeContainerTemplate tnc = (NodeContainerTemplate)m_parentWFM.findNodeContainer(id);
            LOGGER.debug("Updating " + tnc.getNameWithID() + " from " + tnc.getTemplateInformation().getSourceURI());
            ExecutionMonitor subExec = exec.createSubProgress(1.0 / m_ids.length);
            String progMsg = "Node Link \"" + tnc.getNameWithID() + "\"";
            exec.setMessage(progMsg);

            final var loadHelper = GUIWorkflowLoadHelper.forTemplate(d, progMsg, null, false);
            NodeContainerTemplateLinkUpdateResult updateMetaNodeLinkResult =
                    tnc.getParent().updateMetaNodeLink(id, subExec, loadHelper);
            WorkflowPersistor p = updateMetaNodeLinkResult.getUndoPersistor();
            if (p != null) { // no error
                m_newIDs.add(updateMetaNodeLinkResult.getNCTemplate().getID());
                m_undoPersistors.add(p);
            }
            // metanodes don't have data
            // data load errors are unexpected but OK
            IStatus status = createStatus(updateMetaNodeLinkResult, true, false);
            subExec.setProgress(1.0);
            switch (status.getSeverity()) {
            case IStatus.OK:
                break;
            case IStatus.WARNING:
                logPreseveLineBreaks("Warnings during load: "
                        + updateMetaNodeLinkResult.getFilteredError("", LoadResultEntryType.Warning), false);
                break;
            default:
                logPreseveLineBreaks("Errors during load: "
                        + updateMetaNodeLinkResult.getFilteredError("", LoadResultEntryType.Warning), true);
            }
            stats[i] = status;
        }
        pm.done();
        final IStatus status = createMultiStatus("Update node links", stats);
        final String message;
        switch (status.getSeverity()) {
        case IStatus.OK:
            message = "No problems during node link update.";
            break;
        case IStatus.WARNING:
            message = "Warnings during node link update";
            break;
        default:
            message = "Errors during node link update";
        }

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                // will not open if status is OK.
                ErrorDialog.openError(SWTUtilities.getActiveShell(), "Update Node Links", message, status);
            }
        });
    }

    /**
     * Does a final check before actually updating the node. Called for the following edge case:
     *
     * Outer component has an inner component inside and both have an update available. However, the outer update
     * would fix the inner update, which means, the inner update can be skipped. Since UpdateMetaNodeTemplateRunnable always
     * receives both components and tries to update them, this failed previously.
     *
     * @param id NodeID
     * @return should the template still be updated?
     */
    private boolean needsUpdate(final NodeID id) {
        boolean needsUpdate;
        try {
            // this line will fail with in IllegalArgumentException if the node already has been updated
            // basically a computationally cheaper option to WorkflowManager#checkUpdateMetaNodeLink
            needsUpdate = m_parentWFM.findNodeContainer(id) instanceof NodeContainerTemplate;
        } catch (IllegalArgumentException e) {
            LOGGER.debug("Node with ID \"" + id + "\" unexpectedly doesn't need an update.", e);
            needsUpdate = false;
        }
        return needsUpdate;
    }

    /** @return the newIDs */
    public List<NodeID> getNewIDs() {
        return m_newIDs;
    }

    /** @return the undoPersistors */
    public List<WorkflowPersistor> getUndoPersistors() {
        return m_undoPersistors;
    }

    /** Set fields to null so that they can get GC'ed. */
    public void discard() {
        m_parentWFM = null;
        m_ids = null;
        m_newIDs = null;
        m_undoPersistors = null;
    }

}

