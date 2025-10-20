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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.Role;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainerTemplate;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.component.CheckForComponentUpdatesUtil;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.core.util.proxy.DisabledSchemesChecker;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.commands.UpdateMetaNodeLinkCommand;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;

/**
 * Action to check for updates of metanode templates.
 *
 * Iterates recursively over all selected metanodes/components.
 * Checks if any are linked (metanode templates) and updateable (workflow manager can update the link).
 * If any are found a prompt is shown whether to update those.
 * If the user agrees, an {@link UpdateMetaNodeLinkCommand} is executed.
 *
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
            final var model = p.getNodeContainer();
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
            if (nc instanceof NodeContainerTemplate tnc) {
                final var isLink = tnc.getTemplateInformation().getRole() == Role.Link;
                if (isLink && updateableOnly && !tnc.getParent().canUpdateMetaNodeLink(tnc.getID())) {
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

    @SuppressWarnings("restriction")
    @Override
    public void runInSWT() {
        List<NodeID> candidateList = getMetaNodesToCheck(false);
        final var shell = SWTUtilities.getActiveShell();
        IWorkbench wb = PlatformUI.getWorkbench();
        IProgressService ps = wb.getProgressService();
        LOGGER.debug("Checking for updates for " + candidateList.size() + " node link(s)...");
        final var runner = new CheckUpdateRunnableWithProgress(getManager(), candidateList);
        Status status;
        try {
            if (isAnyModalShellOpen(shell.getDisplay())) {
                MessageDialog.openWarning(shell, "Update Check Unavailable", //
                    "Cannot check for component and metanode updates while another dialog is open.");
            } else {
                ps.busyCursorWhile(runner);
            }
            status = runner.getStatus();
        } catch (InvocationTargetException | IllegalStateException e) {
            var message = e instanceof InvocationTargetException ite ? ite.getTargetException().getMessage()
                : e.getMessage();
            if (DisabledSchemesChecker.isCausedByDisabledSchemes(e)) {
                message = DisabledSchemesChecker.FAQ_MESSAGE;
            }
            LOGGER.warn("Failed to check for updates: " + message, e);
            status = new Status(IStatus.WARNING, KNIMEEditorPlugin.PLUGIN_ID, message);
        } catch (InterruptedException e) { // NOSONAR
            return;
        }
        List<NodeID> updateList = runner.getUpdateList();
        if (status.getSeverity() == IStatus.ERROR
                || status.getSeverity() == IStatus.WARNING) {
            ErrorDialog.openError(SWTUtilities.getActiveShell(), null,
                "Errors while checking for updates on node links", status);
            if (updateList.isEmpty()) {
                /* As there are only nodes which have no updates or an error, there is nothing else to do. */
                return;
            }
        }

        // find nodes that will be reset as part of the update
        var nodesToResetCount = 0;
        var hasOnlySelectedSubnodes = true;
        for (NodeID id : updateList) {
            NodeContainerTemplate templateNode = (NodeContainerTemplate)getManager().findNodeContainer(id);
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
            final var messageBuilder = new StringBuilder();
            messageBuilder.append("Update available for ");
            if (isSingle && candidateList.size() == 1) {
                messageBuilder.append(nodeSLow);
                messageBuilder.append(" \"");
                messageBuilder.append(getManager().findNodeContainer(candidateList.get(0)).getNameWithID());
                messageBuilder.append("\".");
            } else if (isSingle) {
                messageBuilder.append("one " + nodeSLow + ".");
            } else {
                messageBuilder.append(updateList.size());
                messageBuilder.append(" ").append(nodeSLow).append("s.");
            }
            try {
                messageBuilder.append("\n\n");
                final var manager = getManager();
                messageBuilder
                    .append(nodeUpdateList(updateList, nodeId -> manager.findNodeContainer(nodeId).getName(), 50, 120));
            } catch (Exception e) {  // NOSONAR we don't want to break component updates because of a failed message
                LOGGER.debug("Error while creating update message", e);
            }
            messageBuilder.append("\n\n");
            if (nodesToResetCount > 0) {
                messageBuilder.append("Reset " + nodeSLow + "s and update now?");
            } else {
                messageBuilder.append("Update now?");
            }
            final var message = messageBuilder.toString();
            if (MessageDialog.openQuestion(shell, title, message)) {
                LOGGER.debug("Running update for " + updateList.size() + " node(s): " + updateList);
                execute(new UpdateMetaNodeLinkCommand(getManager(), updateList.toArray(NodeID[]::new)));
            }
        }
    }

    static boolean isAnyModalShellOpen(final Display display) {
        for (var shell : display.getShells()) {
            if (!shell.isDisposed() && shell.isVisible()
                && (shell.getStyle() & (SWT.APPLICATION_MODAL | SWT.SYSTEM_MODAL | SWT.PRIMARY_MODAL)) != 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolve node ids to names and group by name.
     * @param nodeIds of components with updates
     * @param nodeName function to resolve node id to name
     * @param maxNodes maximum number of lines to generate
     * @param maxChars maximum width of one line
     * @return formatted message like
     * <pre>
     * Connect to Instrumentation
     * Community Hub Revenue (2 instances with updates)
     * </pre>
     */
    static String nodeUpdateList(final List<NodeID> nodeIds, final Function<NodeID, String> nodeName,
        final int maxNodes, final int maxChars) {
        // resolve node ids to names
        final var nodeNames = nodeIds.stream().map(nodeName::apply).toList();
        // group by node name, count occurrences
        final var nodeCounts = nodeNames.stream().collect(Collectors.groupingBy(n -> n, Collectors.counting()));
        // append to message in format "NodeName (n instances with updates)"
        final var formattedNodeList = nodeCounts.entrySet().stream() //
            // sort by name
            .sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey())) //
            .map(e -> e.getValue() == 1 ? e.getKey()
                : "%s (%s instances with updates)".formatted(e.getKey(), e.getValue())) //
            .map(s -> "\u2022 " + s) //
            .map(s -> StringUtils.abbreviateMiddle(s, "...", maxChars)) //
            .limit(maxNodes) //
            .collect(Collectors.joining("\n"));
        if (nodeCounts.size() > maxNodes) {
            return formattedNodeList + "\nand " + (nodeCounts.size() - maxNodes) + " more";
        }
        return formattedNodeList;
    }

    private static final class CheckUpdateRunnableWithProgress implements IRunnableWithProgress {

        private final WorkflowManager m_hostWFM;
        private final List<NodeID> m_candidateList;
        private List<NodeID> m_updateList;
        private Status m_status;

        /**
         * @param hostWFM
         * @param candidateList */
        public CheckUpdateRunnableWithProgress(final WorkflowManager hostWFM, final List<NodeID> candidateList) {
            m_hostWFM = hostWFM;
            m_candidateList = candidateList;
            m_updateList = Collections.emptyList();
        }

        @Override
        public void run(final IProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException, IllegalStateException {
            var res = CheckForComponentUpdatesUtil.checkForComponentUpdatesAndSetUpdateStatus(m_hostWFM,
                KNIMEEditorPlugin.PLUGIN_ID, m_candidateList, monitor);
            m_updateList = res.updateList();
            m_status = res.status();
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
