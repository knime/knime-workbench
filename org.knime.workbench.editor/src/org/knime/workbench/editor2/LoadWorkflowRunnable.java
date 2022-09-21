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
 *   11.01.2007 (sieb): created
 */
package org.knime.workbench.editor2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.UIManager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.data.container.storage.TableStoreFormatInformation;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEComponentInformation;
import org.knime.core.node.NodeAndBundleInformationPersistor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.UnsupportedWorkflowVersionException;
import org.knime.core.node.workflow.WorkflowCreationHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResult;
import org.knime.core.node.workflow.WorkflowPersistor.LoadResultEntry.LoadResultEntryType;
import org.knime.core.node.workflow.WorkflowPersistor.WorkflowLoadResult;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.core.util.LockFailedException;
import org.knime.workbench.editor2.actions.CheckUpdateMetaNodeLinkAllAction;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.p2.actions.AbstractP2Action;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * A runnable which is used by the {@link WorkflowEditor} to load a workflow with a progress bar. NOTE: As the
 * {@link UIManager} holds a reference to this runnable an own class file is necessary such that all references to the
 * created workflow manager can be deleted, otherwise the manager cannot be deleted later and the memory cannot be
 * freed.
 *
 * @author Christoph Sieb, University of Konstanz
 * @author Fabian Dill, University of Konstanz
 */
class LoadWorkflowRunnable extends PersistWorkflowRunnable {

    /**
     * Message returned by {@link #getLoadingCanceledMessage()} in case the loading has been canceled due to a (future)
     * version conflict. (See also AP-7982)
     */
    static final String INCOMPATIBLE_VERSION_MSG = "Canceled workflow load due to incompatible version";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(LoadWorkflowRunnable.class);

    private WorkflowEditor m_editor;

    private File m_workflowFile;

    private WorkflowContextV2 m_workflowContext;

    private Throwable m_throwable = null;

    /** Message, which is non-null if the user canceled to the load. */
    private String m_loadingCanceledMessage;

    /**
     * Creates a new runnable that load a workflow.
     *
     * @param editor the {@link WorkflowEditor} for which the workflow should be loaded
     * @param workflowFile the workflow file from which the workflow should be loaded (or created = empty workflow file)
     * @param workflowContext context of the workflow to be loaded (not null)
     */
    public LoadWorkflowRunnable(final WorkflowEditor editor, final File workflowFile,
            final WorkflowContextV2 workflowContext) {
        m_editor = editor;
        m_workflowFile = workflowFile;
        m_workflowContext = CheckUtils.checkArgumentNotNull(workflowContext);
    }

    /**
     *
     * @return the throwable which was thrown during the loading of the workflow or null, if no throwable was thrown
     */
    Throwable getThrowable() {
        return m_throwable;
    }

    @Override
    public void run(final IProgressMonitor pm) {
        // indicates whether to create an empty workflow
        // this is done if the file is empty
        boolean createEmptyWorkflow = false;

        // name of workflow will be null (uses directory name then)
        String name = null;

        m_throwable = null;

        try {
            // create progress monitor
            final var progressHandler = new ProgressHandler(pm, 101, "Loading workflow...");
            final var progressMonitor = new CheckCancelNodeProgressMonitor(pm);
            progressMonitor.addProgressListener(progressHandler);

            final var workflowDirectory = m_workflowFile.getParentFile();
            final var display = Display.getDefault();
            final var loadHelper =
                    GUIWorkflowLoadHelper.forProject(display, workflowDirectory.getName(), m_workflowContext);
            final WorkflowLoadResult result =
                    WorkflowManager.loadProject(workflowDirectory, new ExecutionMonitor(progressMonitor), loadHelper);
            final var wfm = result.getWorkflowManager();
            m_editor.setWorkflowManager(wfm);
            pm.subTask("Finished.");
            pm.done();
            if (wfm.isDirty()) {
                m_editor.markDirty();
            }

            final IStatus status = createStatus(result, !result.getGUIMustReportDataLoadErrors(), false);
            String message;
            switch (status.getSeverity()) {
                case IStatus.OK:
                    message = "No problems during load.";
                    break;
                case IStatus.WARNING:
                    message = "Warnings during load";
                    logPreseveLineBreaks(
                        "Warnings during load: " + result.getFilteredError("", LoadResultEntryType.Warning), false);
                    break;
                default:
                    message = "Errors during load";
                    logPreseveLineBreaks(
                        "Errors during load: " + result.getFilteredError("", LoadResultEntryType.Warning), true);
            }
            if (!status.isOK()) {
                showLoadErrorDialog(result, status, message, true);
            }
            final List<NodeID> linkedMNs = wfm.getLinkedMetaNodes(true);
            if (!linkedMNs.isEmpty()) {
                final WorkflowEditor editor = m_editor;
                m_editor.addAfterOpenRunnable(() -> postLoadCheckForMetaNodeUpdates(editor, wfm, linkedMNs));
            }
            final Collection<WorkflowEditorEventListener> workflowEditorEventListeners =
                WorkflowEditorEventListeners.getListeners();
            if (!workflowEditorEventListeners.isEmpty()) {
                final WorkflowEditor editor = m_editor;
                editor.addAfterOpenRunnable(() -> {
                    final var event = WorkflowEditorEventListeners.createActiveWorkflowEditorEvent(editor);
                    for (final WorkflowEditorEventListener listener : workflowEditorEventListeners) {
                        try {
                            listener.workflowLoaded(event);
                        } catch (final Throwable throwable) {
                            LOGGER.error("Workflow editor listener error.", throwable);
                        }
                    }
                });
            }
        } catch (FileNotFoundException fnfe) {
            m_throwable = fnfe;
            LOGGER.fatal("File not found", fnfe);
        } catch (IOException ioe) {
            m_throwable = ioe;
            if (m_workflowFile.length() == 0) {
                LOGGER.info("New workflow created.");
                // this is the only place to set this flag to true: we have an
                // empty workflow file, i.e. a new project was created
                // bugfix 1555: if an exception is thrown DO NOT create empty
                // workflow
                createEmptyWorkflow = true;
            } else {
                LOGGER.error("Could not load workflow from: " + m_workflowFile.getName(), ioe);
            }
        } catch (InvalidSettingsException ise) {
            LOGGER.error("Could not load workflow from: " + m_workflowFile.getName(), ise);
            m_throwable = ise;
        } catch (UnsupportedWorkflowVersionException uve) {
            m_loadingCanceledMessage = INCOMPATIBLE_VERSION_MSG;
            LOGGER.info(m_loadingCanceledMessage, uve);
            m_editor.setWorkflowManager(null);
        } catch (CanceledExecutionException cee) {
            m_loadingCanceledMessage = "Canceled loading workflow: " + m_workflowFile.getParentFile().getName();
            LOGGER.info(m_loadingCanceledMessage, cee);
            m_editor.setWorkflowManager(null);
        } catch (LockFailedException lfe) {
            final var error = new StringBuilder("Unable to load workflow \"");
            error.append(m_workflowFile.getParentFile().getName());
            if (m_workflowFile.getParentFile().exists()) {
                error.append("\"\nIt is in use by another user/instance.");
            } else {
                error.append("\"\nLocation does not exist.");
            }
            m_loadingCanceledMessage = error.toString();
            LOGGER.info(m_loadingCanceledMessage, lfe);
            m_editor.setWorkflowManager(null);
        } catch (Throwable e) {
            m_throwable = e;
            LOGGER.error("Workflow could not be loaded. " + e.getMessage(), e);
            m_editor.setWorkflowManager(null);
        } finally {
            // create empty WFM if a new workflow is created
            // (empty workflow file)
            if (createEmptyWorkflow) {
                m_editor.setWorkflowManager(
                    WorkflowManager.ROOT.createAndAddProject(name, new WorkflowCreationHelper(m_workflowContext)));

                // save empty project immediately
                // bugfix 1341 -> see WorkflowEditor line 1294
                // (resource delta visitor movedTo)
                Display.getDefault().syncExec(() -> m_editor.doSave(new NullProgressMonitor()));
                m_editor.setIsDirty(false);

            }
            // IMPORTANT: Remove the reference to the file and the
            // editor!!! Otherwise the memory cannot be freed later
            m_editor = null;
            m_workflowFile = null;
            m_workflowContext = null;
        }
    }

    static void showLoadErrorDialog(final LoadResult result, final IStatus status, final String message,
        final boolean isWorkflow) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                Shell shell = SWTUtilities.getActiveShell();
                List<NodeAndBundleInformationPersistor> missingNodes = result.getMissingNodes();
                List<TableStoreFormatInformation> missingTableFormats = result.getMissingTableFormats();
                if (missingNodes.isEmpty() && missingTableFormats.isEmpty()) {
                    String title = isWorkflow ? "Workflow Load" : "Component Load";
                    // will not open if status is OK.
                    ErrorDialog.openError(shell, title, message, status);
                } else {
                    String missingExtensions = Stream.concat(missingNodes.stream(), missingTableFormats.stream()) //
                            .map(KNIMEComponentInformation::getComponentName) //
                            .distinct() //
                            .collect(Collectors.joining(", "));

                    String missingPrefix = determineMissingPrefix(missingNodes, missingTableFormats);

                    String[] dialogButtonLabels = {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL};
                    String title = (isWorkflow ? "Workflow" : "Component") + " requires " + missingPrefix;
                    MessageDialog dialog = new MessageDialog(shell, title, null,
                        message + " due to " + missingPrefix + " (" + missingExtensions
                            + "). Do you want to search and install the required extensions?",
                        MessageDialog.WARNING, dialogButtonLabels, 0);
                    if ((dialog.open() == 0) && AbstractP2Action.checkSDKAndReadOnly()) {
                        Job j = new InstallMissingNodesJob(missingNodes, missingTableFormats);
                        j.setUser(true);
                        j.schedule();
                    }
                }
            }
        });
    }

    /**
     * Depending on what's missing it returns "a missing node extension", "a missing table format extension" and also
     * respects singular/plural.
     */
    static String determineMissingPrefix(final List<NodeAndBundleInformationPersistor> missingNodes,
        final List<TableStoreFormatInformation> missingTableFormats) {
        StringBuilder b = new StringBuilder();
        if (missingNodes.size() + missingTableFormats.size() == 1) {
            b.append("a ");
        }
        b.append("missing ");
        if (missingTableFormats.isEmpty()) {
            b.append("node extension");
            if (missingNodes.size() > 1) {
                b.append("s");
            }
        } else if (missingNodes.isEmpty()) {
            b.append("table format extension");
            if (missingTableFormats.size() > 1) {
                b.append("s");
            }
        } else {
            b.append("extensions");
        }
        return b.toString();
    }

    /** @return True if the load process has been interrupted. */
    public boolean hasLoadingBeenCanceled() {
        return m_loadingCanceledMessage != null;
    }

    /**
     * @return the loadingCanceledMessage, non-null if {@link #hasLoadingBeenCanceled()}.
     */
    public String getLoadingCanceledMessage() {
        return m_loadingCanceledMessage;
    }

    static void postLoadCheckForMetaNodeUpdates(final WorkflowEditor editor, final WorkflowManager parent,
        final List<NodeID> links) {

        final Map<Boolean, List<NodeID>> partitionedLinks = links.stream()
            .collect(Collectors.partitioningBy(i -> parent.findNodeContainer(i) instanceof SubNodeContainer));
        final List<NodeID> componentLinks = partitionedLinks.get(Boolean.TRUE);
        final List<NodeID> metanodeLinks = partitionedLinks.get(Boolean.FALSE);

        final StringBuilder m = new StringBuilder("The workflow contains ");
        if (componentLinks.size() > 0) {
            if (componentLinks.size() == 1) {
                m.append("one component link (\"");
                m.append(parent.findNodeContainer(componentLinks.get(0)).getNameWithID());
                m.append("\")");
            } else {
                m.append(componentLinks.size()).append(" component links");
            }
            if (metanodeLinks.size() > 0) {
                m.append(" and ");
            } else {
                m.append(".");
            }
        }
        if (metanodeLinks.size() == 1) {
            m.append("one metanode link (\"");
            m.append(parent.findNodeContainer(metanodeLinks.get(0)).getNameWithID());
            m.append("\").");
        } else if (metanodeLinks.size() > 1) {
            m.append(metanodeLinks.size()).append(" metanode links.");
        }
        m.append("\n\nDo you want to check for updates now?");

        final String message = m.toString();
        final AtomicBoolean result = new AtomicBoolean(false);
        final IPreferenceStore corePrefStore = KNIMEUIPlugin.getDefault().getPreferenceStore();
        final String pKey = PreferenceConstants.P_META_NODE_LINK_UPDATE_ON_LOAD;
        String pref = corePrefStore.getString(pKey);
        boolean showInfoMsg = true;
        if (MessageDialogWithToggle.ALWAYS.equals(pref)) {
            result.set(true);
            showInfoMsg = false;
        } else if (MessageDialogWithToggle.NEVER.equals(pref)) {
            result.set(false);
        } else {
            final Display display = Display.getDefault();
            display.syncExec(new Runnable() {
                @Override
                public void run() {
                    Shell activeShell = SWTUtilities.getActiveShell(display);
                    MessageDialogWithToggle dlg = MessageDialogWithToggle.openYesNoCancelQuestion(activeShell,
                        "Metanode Link Update", message, "Remember my decision", false, corePrefStore, pKey);
                    switch (dlg.getReturnCode()) {
                        case IDialogConstants.YES_ID:
                            result.set(true);
                            break;
                        default:
                            result.set(false);
                    }
                }
            });
        }
        if (result.get()) {
            new CheckUpdateMetaNodeLinkAllAction(editor, showInfoMsg).run();
        }
    }

}
