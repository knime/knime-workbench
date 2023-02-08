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
 * History
 *   12.05.2010 (Bernd Wiswedel): created
 */
package org.knime.workbench.editor2.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.Role;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.UpdateStatus;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainerTemplate;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.TemplateUpdateUtil;
import org.knime.core.node.workflow.WorkflowLoadHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.commands.UpdateMetaNodeLinkCommand;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Action to check for updates of metanode templates.
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
public class CheckUpdateMetaNodeLinkAction extends AbstractNodeAction {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(CheckUpdateMetaNodeLinkAction.class);

    private final boolean m_showInfoMsgIfNoUpdateAvail;

    /** Action ID. */
    public static final String ID = "knime.action.meta_node_check_update_link";

    /** Create new action based on given editor.
     * @param editor The associated editor. */
    public CheckUpdateMetaNodeLinkAction(final WorkflowEditor editor) {
        this(editor, true);
    }

    /** Create new action based on given editor.
     * @param editor The associated editor.
     * @param showInfoMsgIfNoUpdateAvail If to show an info box if no
     * updates are available, true if this is a manually triggered command,
     * false if is run as automatic procedure after load (no user interaction)
     */
    public CheckUpdateMetaNodeLinkAction(final WorkflowEditor editor, final boolean showInfoMsgIfNoUpdateAvail) {
        super(editor);
        m_showInfoMsgIfNoUpdateAvail = showInfoMsgIfNoUpdateAvail;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public String getId() {
        return ID;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getText() {
        return "Update Link\t" + getHotkey("knime.commands.updateMetaNodeLink");
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public String getToolTipText() {
        return "Checks whether a newer version of the underlying metanodes and components"
            + " are available and updates the selected links";
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/meta/metanode_link_update.png");
    }

    /**
     * @return true, if underlying model instance of
     *         <code>WorkflowManager</code>, otherwise false
     */
    @Override
    protected boolean internalCalculateEnabled() {
        if (getManager().isWriteProtected()) {
            return false;
        }
        return !getMetaNodesToCheck(true).isEmpty();
    }

    /**
     * Default implementation of getMetaNodeToCheck
     * @return NodeIDs
     */
    protected List<NodeID> getMetaNodesToCheck() {
        return getMetaNodesToCheck(true);
    }

    /**
     * Based on the selection in the workflow, retrieves the NodeIDs to check for updates. Does two things:
     *  1. Scanning templates for updateable nodes (and in consequence enabling "Update Link" context menu entry)
     *  2. Recursively retrieving all candidates for checking for available updates (upon clicking "Update Link")
     *
     * @param updateableOnly Determines whether a pre-check on collected templates should be made, see
     *            {@link WorkflowManager#canUpdateMetaNodeLink(NodeID)}
     * @return List of candidate NodeIDs
     */
    protected List<NodeID> getMetaNodesToCheck(final boolean updateableOnly) {
        List<NodeID> list = new ArrayList<>();
        for (NodeContainerEditPart p : getSelectedParts(NodeContainerEditPart.class)) {
            NodeContainerUI model = p.getNodeContainer();
            if (Wrapper.wraps(model, NodeContainerTemplate.class)) {
                NodeContainerTemplate tnc = Wrapper.unwrap(model, NodeContainerTemplate.class);
                var isLink = tnc.getTemplateInformation().getRole() == Role.Link;

                if (isLink && updateableOnly && !getManager().canUpdateMetaNodeLink(tnc.getID())) {
                    return Collections.emptyList();
                } else if (isLink) {
                    list.add(tnc.getID());
                }
                list.addAll(getNCTemplatesToCheck(tnc, updateableOnly));
            }
        }
        return list;
    }

    /**
     * Does the recursive calling of {@link CheckUpdateMetaNodeLinkAction#getMetaNodesToCheck(boolean)}
     * for a selected NodeContainerTemplate.
     *
     * @param template NodeContainerTemplate
     * @param updateableOnly should only directly updateable templates be collected?
     * @return List of candidate NodeIDs
     */
    private List<NodeID> getNCTemplatesToCheck(final NodeContainerTemplate template, final boolean updateableOnly) {
        List<NodeID> list = new ArrayList<>();
        for (NodeContainer nc : template.getNodeContainers()) {
            if (nc instanceof NodeContainerTemplate) {
                NodeContainerTemplate tnc = (NodeContainerTemplate)nc;
                var isLink = tnc.getTemplateInformation().getRole() == Role.Link;

                if (isLink && updateableOnly && !getManager().canUpdateMetaNodeLink(tnc.getID())) {
                    return Collections.emptyList();
                } else if (isLink) {
                    list.add(tnc.getID());
                }
                list.addAll(getNCTemplatesToCheck(tnc, updateableOnly));
            }
        }
        return list;
    }

    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodes) {
        throw new IllegalStateException("Not to be called");
    }

    @Override
    public void runInSWT() {
        List<NodeID> candidateList = getMetaNodesToCheck(false);
        final Shell shell = SWTUtilities.getActiveShell();
        IWorkbench wb = PlatformUI.getWorkbench();
        IProgressService ps = wb.getProgressService();
        LOGGER.debug("Checking for updates for " + candidateList.size() + " node link(s)...");
        CheckUpdateRunnableWithProgress runner =
            new CheckUpdateRunnableWithProgress(getManager(), candidateList);
        Status status;
        try {
            ps.busyCursorWhile(runner);
            status = runner.getStatus();
        } catch (InvocationTargetException | IllegalStateException e) {
            var message = e instanceof InvocationTargetException
                ? ((InvocationTargetException)e).getTargetException().getMessage() : e.getMessage();
            LOGGER.warn("Failed to check for updates: " + message, e);
            status = new Status(IStatus.WARNING, KNIMEEditorPlugin.PLUGIN_ID, message);
        } catch (InterruptedException e) {
            return;
        }
        List<NodeID> updateList = runner.getUpdateList();
        if (status.getSeverity() == IStatus.ERROR
                || status.getSeverity() == IStatus.WARNING) {
            ErrorDialog.openError(SWTUtilities.getActiveShell(), null,
                "Errors while checking for updates on node links", status);
            if (updateList.isEmpty()) {
                /* As there are only nodes which have no updates or an error,
                 * there is nothing else to do. */
                return;
            }
        }

        // find nodes that will be reset as part of the update
        var nodesToResetCount = 0;
        var hasOnlySelectedSubnodes = true;
        for (NodeID id : updateList) {
            NodeContainerTemplate templateNode =
                (NodeContainerTemplate)getManager().findNodeContainer(id);
            // TODO problematic with through-connections
            if (templateNode.containsExecutedNode()) {
                nodesToResetCount += 1;
            }
            if (!(templateNode instanceof SubNodeContainer)) {
                hasOnlySelectedSubnodes = false;
            }
        }

        String nodeSLow = hasOnlySelectedSubnodes ? "component" : "node";
        String nodeSUp = hasOnlySelectedSubnodes ? "Component" : "Node";

        if (updateList.isEmpty()) {
            if (m_showInfoMsgIfNoUpdateAvail) {
                MessageDialog.openInformation(shell, "Update", "No updates available");
            } else {
                LOGGER.infoWithFormat("No updates available (%d %s link(s))", candidateList.size(), nodeSLow);
            }
        } else {
            boolean isSingle = updateList.size() == 1;
            String title = "Update " + nodeSUp + (isSingle ? "" : "s");
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("Update available for ");
            if (isSingle && candidateList.size() == 1) {
                messageBuilder.append(nodeSLow);
                messageBuilder.append(" \"");
                messageBuilder.append(getManager().findNodeContainer(
                        candidateList.get(0)).getNameWithID());
                messageBuilder.append("\".");
            } else if (isSingle) {
                messageBuilder.append("one " + nodeSLow + ".");
            } else {
                messageBuilder.append(updateList.size());
                messageBuilder.append(" ").append(nodeSLow).append("s.");
            }
            messageBuilder.append("\n\n");
            if (nodesToResetCount > 0) {
                messageBuilder.append("Reset " + nodeSLow + "s and update now?");
            } else {
                messageBuilder.append("Update now?");
            }
            String message = messageBuilder.toString();
            if (MessageDialog.openQuestion(shell, title, message)) {
                LOGGER.debug("Running update for " + updateList.size() + " node(s): " + updateList);
                execute(new UpdateMetaNodeLinkCommand(getManager(),
                        updateList.toArray(new NodeID[updateList.size()])));
            }
        }
    }

    private static final class CheckUpdateRunnableWithProgress
        implements IRunnableWithProgress {

        private final WorkflowManager m_hostWFM;
        private final List<NodeID> m_candidateList;
        private final List<NodeID> m_updateList;
        private Status m_status;

        /**
         * @param hostWFM
         * @param candidateList */
        public CheckUpdateRunnableWithProgress(final WorkflowManager hostWFM,
                final List<NodeID> candidateList) {
            m_hostWFM = hostWFM;
            m_candidateList = candidateList;
            m_updateList = new ArrayList<>();
        }

        /** {@inheritDoc} */
        @Override
        public void run(final IProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException, IllegalStateException {
            monitor.beginTask("Checking Link Updates", m_candidateList.size());
            var lH = new WorkflowLoadHelper(true, m_hostWFM.getContextV2());

            var stats = new Status[m_candidateList.size()];
            int overallStatus = IStatus.OK;

            // retrieving the node templates per node id
            Map<NodeID, NodeContainerTemplate> nodeIdToTemplate = new LinkedHashMap<>();
            for (NodeID id : m_candidateList) {
                nodeIdToTemplate.put(id, (NodeContainerTemplate)m_hostWFM.findNodeContainer(id));
            }

            // retrieving the update status per node template
            final var loadResult = new LoadResult("ignored");
            Map<NodeID, UpdateStatus> nodeIdToUpdateStatus;
            try {
                nodeIdToUpdateStatus = TemplateUpdateUtil.fillNodeUpdateStates(nodeIdToTemplate.values(), lH,
                    loadResult, new LinkedHashMap<>());
            } catch (IOException e) {
                final var ex = e.getCause() != null ? e.getCause() : e;
                LOGGER.warn(ex);
                m_status = new MultiStatus(KNIMEEditorPlugin.PLUGIN_ID, IStatus.ERROR, new IStatus[]{Status.error("")},
                    "Some Node Link Updates failed", ex);
                verifyMultiStatus();
                monitor.done();
                return;
            }

            var i = 0;
            for (Map.Entry<NodeID, UpdateStatus> entry : nodeIdToUpdateStatus.entrySet()) {
                var id = entry.getKey();
                var updateStatus = entry.getValue();
                var tnc = nodeIdToTemplate.get(id);

                monitor.subTask(tnc.getNameWithID());
                var stat = createTemplateStatus(updateStatus, tnc);
                // if at least one WARNING level status was detected, entire status will be on WARNING level
                if (stat.getSeverity() == IStatus.WARNING) {
                    overallStatus = IStatus.WARNING;
                }

                if (monitor.isCanceled()) {
                    throw new InterruptedException("Update check canceled");
                }
                stats[i] = stat;
                i++;
                monitor.worked(1);

            }
            m_status = new MultiStatus(
                KNIMEEditorPlugin.PLUGIN_ID, overallStatus, stats, "Some Node Link Updates failed", null);
            verifyMultiStatus();
            monitor.done();
        }

        /**
         * Builds a Status object from the the retrieve UpdateStatus. An UpdateStatus.Error will be masked if a
         * parent node has an update available
         * @param updateStatus status per node template
         * @param tnc node container template
         * @return status object
         */
        private Status createTemplateStatus(final UpdateStatus updateStatus, final NodeContainerTemplate tnc) {
            final String idName = KNIMEEditorPlugin.PLUGIN_ID;
            var id = tnc.getID();
            final String tncName = tnc.getNameWithID();

            switch (updateStatus) {
                case HasUpdate:
                    m_updateList.add(id);
                    return new Status(IStatus.OK, idName, "Update available for " + tncName);
                case UpToDate:
                    return new Status(IStatus.OK, idName, "No update available for " + tncName);
                case Error:
                    // if an update for a parent was found, ignore the child's error
                    if (!updateableParentExists(id)) {
                        return new Status(IStatus.WARNING, idName, "Unable to check for update on node \"" + tncName + "\": Can't read metanode/template directory " + tnc.getTemplateInformation().getSourceURI(), null);
                    } else {
                        return new Status(IStatus.OK, idName,
                            "Update error exists, but could be resolved by parent update for " + tncName);
                    }
                default:
                    return new Status(IStatus.WARNING , idName, "Could not resolve update status for " + tncName, null);
            }
        }

        /**
         * Checks whether for a given nodeID a parent already has an update found.
         * Note: this absolutely relies on the node templates being scanned in the correct order, from outer to inner,
         * which will be due to how {@link CheckUpdateMetaNodeLinkAction#getMetaNodesToCheck()} works.
         * @param id NodeID
         * @return does a parent have an update available?
         */
        private boolean updateableParentExists(final NodeID id) {
            return m_updateList.stream().anyMatch(id::hasPrefix);
        }

        /**
         * Verifies the multi status that was constructed in the run method.
         * As a side effect, correctly sets the internal update state per NodeTemplate iff all went well.
         * @throws InterruptedException
         */
        private void verifyMultiStatus() throws IllegalStateException {
            // retrieves all top-level NodeIDs, works because of the recursive scan of the candidates,
            // i.e. the first node to set m_topLevelPrefix will always be on the top level
            var topLevelCandidates = m_candidateList.stream().filter(new Predicate<NodeID>() {
                private NodeID m_topLevelPrefix = null;

                @Override
                public boolean test(final NodeID t) {
                    if (m_topLevelPrefix == null) {
                        m_topLevelPrefix = t.getPrefix();
                        return true;
                    }
                    return t.getPrefix().equals(m_topLevelPrefix);
                }
            }).collect(Collectors.toList());

            var updateError = false;
            // for each of the top level templates nodes, invoke a recursive update check
            for (NodeID tlc : topLevelCandidates) {
                try {
                    m_hostWFM.checkUpdateMetaNodeLink(tlc, new WorkflowLoadHelper(true, m_hostWFM.getContextV2()));
                } catch (IOException e) {
                    updateError = true;
                }
            }

            if ((m_status.getSeverity() == IStatus.WARNING || m_status.getSeverity() == IStatus.ERROR) != updateError) {
                throw new IllegalStateException("Inconsistent update states, something went wrong");
            }
        }

        /** @return the updateList */
        public List<NodeID> getUpdateList() {
            return m_updateList;
        }

        /** @return the status */
        public Status getStatus() {
            return m_status;
        }
    }

}
