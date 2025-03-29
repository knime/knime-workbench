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
 */
package org.knime.workbench.editor2;

import static org.knime.core.ui.wrapper.Wrapper.wrap;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EventObject;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.SnapToGeometry;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.PrintAction;
import org.eclipse.gef.ui.actions.RedoAction;
import org.eclipse.gef.ui.actions.SaveAction;
import org.eclipse.gef.ui.actions.StackAction;
import org.eclipse.gef.ui.actions.UndoAction;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.gef.ui.actions.ZoomComboContributionItem;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.gef.ui.properties.UndoablePropertySheetEntry;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.internal.EditorHistory;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.internal.help.WorkbenchHelpSystem;
import org.eclipse.ui.progress.IProgressService;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.ClassUtils;
import org.knime.core.node.util.StringFormat;
import org.knime.core.node.workflow.AbstractNodeExecutionJobManager;
import org.knime.core.node.workflow.EditorUIInformation;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainerState;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.NodeExecutionJobManager;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodePropertyChangedEvent;
import org.knime.core.node.workflow.NodePropertyChangedListener;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.node.workflow.NodeTimer;
import org.knime.core.node.workflow.NodeTimer.GlobalNodeStats.WorkflowType;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.NodeUIInformationEvent;
import org.knime.core.node.workflow.NodeUIInformationListener;
import org.knime.core.node.workflow.WorkflowCipherPrompt;
import org.knime.core.node.workflow.WorkflowEvent;
import org.knime.core.node.workflow.WorkflowListener;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.node.workflow.WorkflowSaveHelper;
import org.knime.core.node.workflow.contextv2.AnalyticsPlatformExecutorInfo;
import org.knime.core.node.workflow.contextv2.LocalLocationInfo;
import org.knime.core.node.workflow.contextv2.LocationInfo;
import org.knime.core.node.workflow.contextv2.RestLocationInfo;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2;
import org.knime.core.node.workflow.contextv2.WorkflowContextV2.LocationType;
import org.knime.core.ui.UI;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.RemoteWorkflowContext;
import org.knime.core.ui.node.workflow.SubNodeContainerUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.node.workflow.async.AsyncWorkflowManagerUI;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.core.ui.wrapper.WorkflowManagerWrapper;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.core.util.Pointer;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.nodeprovider.NodeProvider;
import org.knime.workbench.core.nodeprovider.NodeProvider.EventListener;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.descriptionview.DescriptionView;
import org.knime.workbench.editor2.WorkflowEditorEventListener.ActiveWorkflowEditorEvent;
import org.knime.workbench.editor2.WorkflowEditorEventListener.WorkflowEditorEvent;
import org.knime.workbench.editor2.actions.AbstractNodeAction;
import org.knime.workbench.editor2.actions.AddAnnotationAction;
import org.knime.workbench.editor2.actions.BringAnnotationForwardAction;
import org.knime.workbench.editor2.actions.BringAnnotationToFrontAction;
import org.knime.workbench.editor2.actions.CancelAction;
import org.knime.workbench.editor2.actions.CancelAllAction;
import org.knime.workbench.editor2.actions.ChangeComponentHubVersionAction;
import org.knime.workbench.editor2.actions.ChangeMetaNodeLinkAction;
import org.knime.workbench.editor2.actions.ChangeSubNodeLinkAction;
import org.knime.workbench.editor2.actions.CheckUpdateMetaNodeLinkAction;
import org.knime.workbench.editor2.actions.CollapseMetaNodeAction;
import org.knime.workbench.editor2.actions.ConvertMetaNodeToSubNodeAction;
import org.knime.workbench.editor2.actions.ConvertSubNodeToMetaNodeAction;
import org.knime.workbench.editor2.actions.CopyAction;
import org.knime.workbench.editor2.actions.CutAction;
import org.knime.workbench.editor2.actions.DefaultOpenViewAction;
import org.knime.workbench.editor2.actions.DisconnectMetaNodeLinkAction;
import org.knime.workbench.editor2.actions.DisconnectSubNodeLinkAction;
import org.knime.workbench.editor2.actions.EncapsulateSubNodeAction;
import org.knime.workbench.editor2.actions.ExecuteAction;
import org.knime.workbench.editor2.actions.ExecuteAllAction;
import org.knime.workbench.editor2.actions.ExecuteAndOpenViewAction;
import org.knime.workbench.editor2.actions.ExpandMetaNodeAction;
import org.knime.workbench.editor2.actions.ExpandSubNodeAction;
import org.knime.workbench.editor2.actions.HideNodeNamesAction;
import org.knime.workbench.editor2.actions.LinkNodesAction;
import org.knime.workbench.editor2.actions.LockMetaNodeAction;
import org.knime.workbench.editor2.actions.LockSubNodeAction;
import org.knime.workbench.editor2.actions.MetaNodeReconfigureAction;
import org.knime.workbench.editor2.actions.NodeConnectionContainerDeleteAction;
import org.knime.workbench.editor2.actions.OpenDialogAction;
import org.knime.workbench.editor2.actions.OpenFlowVariableConfigAction;
import org.knime.workbench.editor2.actions.PasteAction;
import org.knime.workbench.editor2.actions.PasteActionContextMenu;
import org.knime.workbench.editor2.actions.PauseLoopExecutionAction;
import org.knime.workbench.editor2.actions.ResetAction;
import org.knime.workbench.editor2.actions.ResumeLoopAction;
import org.knime.workbench.editor2.actions.RevealMetaNodeTemplateAction;
import org.knime.workbench.editor2.actions.RevealSubNodeTemplateAction;
import org.knime.workbench.editor2.actions.SaveAsMetaNodeTemplateAction;
import org.knime.workbench.editor2.actions.SaveAsSubNodeTemplateAction;
import org.knime.workbench.editor2.actions.SelectLoopAction;
import org.knime.workbench.editor2.actions.SendAnnotationBackwardAction;
import org.knime.workbench.editor2.actions.SendAnnotationToBackAction;
import org.knime.workbench.editor2.actions.SetNodeDescriptionAction;
import org.knime.workbench.editor2.actions.ShowNodeIdsAction;
import org.knime.workbench.editor2.actions.StepLoopAction;
import org.knime.workbench.editor2.actions.SubNodeReconfigureAction;
import org.knime.workbench.editor2.actions.ToggleEditorModeAction;
import org.knime.workbench.editor2.actions.ToggleFlowVarPortsAction;
import org.knime.workbench.editor2.actions.UnlinkNodesAction;
import org.knime.workbench.editor2.actions.ZoomInAlternateIncrementAction;
import org.knime.workbench.editor2.actions.ZoomOutAlternateIncrementAction;
import org.knime.workbench.editor2.actions.ZoomResetAction;
import org.knime.workbench.editor2.actions.search.FindResults;
import org.knime.workbench.editor2.commands.CreateNewConnectedMetaNodeCommand;
import org.knime.workbench.editor2.commands.CreateNewConnectedNodeCommand;
import org.knime.workbench.editor2.commands.CreateNodeCommand;
import org.knime.workbench.editor2.editparts.AnnotationEditPart;
import org.knime.workbench.editor2.editparts.GUIWorkflowCipherPrompt;
import org.knime.workbench.editor2.editparts.NodeAnnotationEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;
import org.knime.workbench.editor2.figures.ProgressPolylineConnection;
import org.knime.workbench.editor2.figures.WorkflowFigure;
import org.knime.workbench.editor2.menu.MRUFileMenuItem;
import org.knime.workbench.editor2.svgexport.WorkflowSVGExport;
import org.knime.workbench.editor2.viewport.MessageAppearance;
import org.knime.workbench.editor2.viewport.ViewportPinningGraphicalViewer;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.RemoteWorkflowInput;
import org.knime.workbench.explorer.dialogs.SaveAsValidator;
import org.knime.workbench.explorer.dialogs.SpaceResourceSelectionDialog;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ContentObject;
import org.knime.workbench.explorer.view.dialogs.OverwriteAndMergeInfo;
import org.knime.workbench.explorer.view.dialogs.SnapshotPanel;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.SyncExecQueueDispatcher;
import org.knime.workbench.ui.navigator.ProjectWorkflowMap;
import org.knime.workbench.ui.navigator.WorkflowEditorAdapter;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * This is the implementation of the Eclipse Editor used for editing a
 * <code>WorkflowManager</code> object. This also handles the basic GEF stuff
 * (command stack) and hooks into the workbench to provide actions etc. ...
 *
 * @author Florian Georg, University of Konstanz
 * @author Christoph Sieb, University of Konstanz
 */
public class WorkflowEditor extends GraphicalEditor implements
        WorkflowListener,
        NodeStateChangeListener,
        NodePropertyChangedListener, ISaveablePart2, NodeUIInformationListener,
        EventListener, IPropertyChangeListener {

    /** Id as defined in plugin.xml. */
    public static final String ID = "org.knime.workbench.editor.WorkflowEditor";

    /** The editor mode which a newly opened worklow is in **/
    public static final WorkflowEditorMode INITIAL_EDITOR_MODE = WorkflowEditorMode.NODE_EDIT;

    /** The root clipboard name **/
    public static final String CLIPBOARD_ROOT_NAME = "clipboard";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowEditor.class);

    /**
     * The static clipboard for copy/cut/paste.
     */
    private static ClipboardObject CLIPBOARD;

    private static final Color BG_COLOR_WRITE_LOCK =
        new Color(null, 235, 235, 235);

    private static final Color BG_COLOR_DEFAULT =
        Display.getDefault().getSystemColor(SWT.COLOR_WHITE);


    /** root model object (=editor input) that is handled by the editor. * */
    private WorkflowManagerUI m_manager;

    /** the editor's action registry. */
    private ActionRegistry m_actionRegistry;

    /** the <code>EditDomain</code>. */
    private final DefaultEditDomain m_editDomain;

    /** the dirty state. */
    private boolean m_isDirty;

    /** the metadata editor dirty state. */
    private boolean m_isMetadataEditorDirty;

    /** List with the action ids that are associated to this editor. * */
    private List<String> m_editorActions;

    /*
     * As part of AP-12516, we moved from an implementation of editor message displaying in which there could
     *  only ever be one per type displayed across the application, to where N could be displayed (for example,
     *  perhaps a consumer besides the WorkflowEditor would like to show a pinned message, as might be done in
     *  AP-12503) per type. So message displaying code in this class would simply set a new message of type X
     *  assuming it would replace the last message it set of type X. We now track what we set in this class
     *  and remove it appropriately.
     */
    private Long m_lastDisplayedInfoMessageId = null;
    private Long m_lastDisplayedWarningMessageId = null;
    private Long m_lastDisplayedErrorMessageId = null;

    private EditorMouseWheelListener m_mouseWheelListener;

    private ZoomComboContributionItem m_zoomComboBox;
    private ZoomInAlternateIncrementAction m_alternateZoomInAction;
    private ZoomOutAlternateIncrementAction m_alternateZoomOutAction;

    private WorkflowCanvasClickListener m_canvasClickListener;
    private NodeSupplantDragListener m_nodeSupplantDragListener;

    private ConnectionHighlighter m_connectionHighlighter;

    private FindResults m_lastSearchResults;

    private WorkflowEditorMode m_editorMode;

    /** path to the workflow directory (that contains the workflow.knime file). */
    private URI m_fileResource;

    /** If subworkflow editor, store the parent for saving. */
    private WorkflowEditor m_parentEditor;

    private NewOverviewOutlinePage m_overviewOutlinePage;

    private PropertySheetPage m_undoablePropertySheetPage;

    private final WorkflowSelectionTool m_selectionTool;

    /** whether the afterOpen method has been run already (disallow queuing
     * another runnable). */
    private boolean m_hasAfterOpenRun = false;
    /** A list of runnable to be run after the editor is initialized.
     * See also {@link #addAfterOpenRunnable(Runnable)} for details. */
    private List<Runnable> m_afterOpenRunnables;

    /** Indicates if this editor has been closed. */
    private boolean m_closed;
    /** Indicates that this instance aborted its init(...) because the app is exiting */
    private boolean m_initAbortedDueToAppExit;

    private String m_manuallySetToolTip;

    /** Auto save is possible when this editor is for a workflow (not a metanode) and no other auto-save copy
     * is detected when the workflow is opened (see assignment of variable for additional requirements). */
    private boolean m_isAutoSaveAllowed;

    /** The job for auto-saving. Reused for each save, init when first needed. */
    private AutoSaveJob m_autoSaveJob;

    private final Semaphore m_workflowCanBeDeleted = new Semaphore(1);

    /**
     * Refresher for the workflow editor. Only non-null if underlying workflow manager is of type
     * {@link AsyncWorkflowManagerUI}.
     */
    private WorkflowEditorRefresher m_refresher = null;

    /**
     * Whether the dialog has passed the {@link #promptToSaveOnClose()} method.
     * This helps us to distinguish whether we have to offer an exit dialog when
     * executing nodes are not saveable. This flag is only true if the user
     * closes the editor.
     */
    private boolean m_isClosing;

    /**
     * Whether node connections should be drawn curved or straight.
     */
    private Boolean m_hasCurvedConnections = null;

    /**
     * Width of the line connecting two nodes.
     */
    private int m_connectionLineWidth;

    /**
     * No arg constructor, creates the edit domain for this editor.
     */
    public WorkflowEditor() {
        super();

        LOGGER.debug("Creating WorkflowEditor...");

        m_closed = false;
        m_initAbortedDueToAppExit = false;

        // create an edit domain for this editor (handles the command stack)
        m_editDomain = new DefaultEditDomain(this);
        m_selectionTool = new WorkflowSelectionTool();
        m_editDomain.setActiveTool(m_selectionTool);
        m_editDomain.setDefaultTool(m_selectionTool);

        setEditDomain(m_editDomain);

        // initialize actions (can't be in init(), as setInput is called before)
        createActions();

        m_editorMode = INITIAL_EDITOR_MODE;
    }

    /**
     * @return the graphical viewer of this editor.
     */
    public GraphicalViewer getViewer() {
        return getGraphicalViewer();
    }

    /**
     * Returns the clipboard content shared between all {@link WorkflowEditor} instances.
     *
     * @return the clipboard for this editor
     */
    public ClipboardObject getClipboardContent() {
        return CLIPBOARD;
    }

    /**
     * Sets the clipboard content; note that this sets the class variable object and so applies to all instances of
     * this class within the JVM.
     *
     * @param content the content to set into the clipboard
     */
    public void setClipboardContent(final ClipboardObject content) {
        CLIPBOARD = content;
    }

    /**
     * @return the current edit mode of this editor.
     */
    public WorkflowEditorMode getEditorMode() {
        return m_editorMode;
    }

    /**
     * @param wme the new editor mode; this setting does not trigger any sort of listener notification, so it should
     *            therefore be done in a code lifecycle prior to any dependent code querying the WorkflowEditor.
     */
    public void setEditorMode(final WorkflowEditorMode wme) {
        m_editorMode = wme;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final IEditorSite site, final IEditorInput input)
            throws PartInitException {
        if (site.getWorkbenchWindow().isClosing()) {
            m_initAbortedDueToAppExit = true;
            return;
        }

        LOGGER.debug("Initializing editor UI...");

        // TODO FIXME: Colors need to be assigned to different debugging levels
        NodeLogger.getLogger(WorkflowEditor.class).debug(
                "Opening workflow Editor on " + input.getName());

        // store site and input
        setSite(site);
        setInput(input);

        // add this as a CommandStackListener
        getCommandStack().addCommandStackListener(this);

        m_connectionHighlighter = new ConnectionHighlighter(this);

        final ISelectionService iss = getSite().getWorkbenchWindow().getSelectionService();
        iss.addSelectionListener(this);
        iss.addSelectionListener(m_connectionHighlighter);

        final IPreferenceStore prefStore = KNIMEUIPlugin.getDefault().getPreferenceStore();
        prefStore.addPropertyChangeListener(this);

        // update usage statistics for all workflow types e.g. from local/remote repository or KNIME Hub by
        // double click or drag&drop or if the workflow was already open during startup but.
        //This also includes workflows opened in remote job view.
        if (input instanceof IURIEditorInput) {
            //this excludes all metanodes and subnodes
            //this workflow was opened from a Server repository or KNIME Hub but not via remote job view
            final WorkflowType type;
            if (input instanceof RemoteWorkflowInput) {
                type = WorkflowType.REMOTE;
            } else {
                type = WorkflowType.LOCAL;
            }
            NodeTimer.GLOBAL_TIMER.incWorkflowOpening(getWorkflowManager().orElse(null), type);
        }

        queueAfterOpen();
    }

    /** Add an action that is run in the SWT main thread after the editor
     * is initialized. It's used to prompt for additional actions (update links)
     * or to display notifications.
     * @param action The action to queue.
     * @throws IllegalStateException If editor is already initialized and the
     * after-open method has already been called. */
    public void addAfterOpenRunnable(final Runnable action) {
        if (m_hasAfterOpenRun) {
            throw new IllegalStateException("Can't queue afterOpen-runner - method has already been run");
        }
        if (m_afterOpenRunnables == null) {
            m_afterOpenRunnables = new ArrayList<>();
        }
        m_afterOpenRunnables.add(action);
    }

    /** Queues all {@link Runnable} in the after-open-runnable list in the
     * SWT main thread. */
    private void queueAfterOpen() {
        m_hasAfterOpenRun = true;
        if (m_afterOpenRunnables != null) {
            final Display d = Display.getDefault();
            for (final Runnable r : m_afterOpenRunnables) {
                d.asyncExec(r);
            }
        }
        m_afterOpenRunnables = null;
    }


    /*
     * Returns all sub editors (of sub nodes / metanodes) of this editor.
     */
    private List<IEditorPart> getSubEditors() {
        List<IEditorPart> result = new ArrayList<IEditorPart>();
        for (NodeContainerUI nc : m_manager.getNodeContainers()) {
            result.addAll(getSubEditors(nc.getID()));
        }
        return result;
    }

    /**
     * Collects the open editor(s) of the specified (sub-)workflow manager and all sub editor(s) of it. The provided id
     * must be a child of the workflow displayed in this editor.
     *
     * @param id of a child of this editor. Must be a sub/metanode whose editor (and all sub-editors recursively) will
     *            be returned.
     * @return a list of open editors
     */
    public List<IEditorPart> getSubEditors(final NodeID id) {
        List<IEditorPart> editors = new ArrayList<IEditorPart>();
        if (m_manager == null) {
            // no workflow, no sub editors
            return editors;
        }
        NodeContainerUI child = null;
        WorkflowManagerUI child_mgr = null;
        try {
            child = m_manager.getNodeContainer(id);
        } catch (IllegalArgumentException iae) {
            // if node doesn't exist - or just got deleted, then there are no sub editors
            return editors;
        }
        if (child instanceof SubNodeContainerUI) {
            child_mgr = ((SubNodeContainerUI)child).getWorkflowManager();
        } else if (child instanceof WorkflowManagerUI) {
            child_mgr = (WorkflowManagerUI)child;
        } else {
            return editors;
        }
        WorkflowManagerInput in = new WorkflowManagerInput(child_mgr, this);
        if (PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null) {
            for (IWorkbenchPage p : PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPages()) {
                IEditorPart child_editor = p.findEditor(in);
                if (child_editor != null) {
                    editors.add(child_editor);
                    if (child_editor instanceof WorkflowEditor) {
                        // recursively add sub editors (to get sub/metanodes in sub/metanodes)
                        for (NodeContainerUI nc : child_mgr.getNodeContainers()) {
                            editors.addAll(((WorkflowEditor)child_editor).getSubEditors(nc.getID()));
                        }
                    }
                }
            }
        }
        return editors;
    }

    /**
     * Deregisters all listeners when the editor is disposed.
     *
     * @see org.eclipse.ui.IWorkbenchPart#dispose()
     */
    @SuppressWarnings("restriction")
    @Override
    public void dispose() {
        if (m_initAbortedDueToAppExit) {
            return;
        }

        NodeLogger.getLogger(WorkflowEditor.class).debug("Disposing editor...");
        if (m_mouseWheelListener != null) {
            m_mouseWheelListener.dispose();
        }
        if (m_nodeSupplantDragListener != null) {
            m_nodeSupplantDragListener.dispose();
        }
        if (m_canvasClickListener != null) {
            m_canvasClickListener.dispose();
        }
        if (m_lastSearchResults != null) {
            m_lastSearchResults.dispose();
        }
        if (m_fileResource != null && m_manager != null) {
            // disposed is also called when workflow load fails or is canceled
            ProjectWorkflowMap.unregisterClientFrom(m_fileResource, this);
            ProjectWorkflowMap.remove(m_fileResource); // removes the workflow from memory
            if (isTempRemoteWorkflowEditor() || isTempLocalWorkflowEditor()) {
                final var remoteURI = Wrapper.unwrapWFM(m_manager).getContextV2().getTempSourceLocation().orElseThrow();
                // after the workflow is deleted we can delete the temp location
                final var flowLoc = getFileStore(m_fileResource);
                if (flowLoc.isPresent()) {
                    new Thread(() -> {
                        try {
                            m_workflowCanBeDeleted.acquire();
                            final var d = flowLoc.get().toLocalFile();
                            if (d != null && d.exists()) {
                                FileUtils.deleteDirectory(d.getParentFile());
                            }
                        } catch (CoreException | IOException | InterruptedException e) {
                            LOGGER.warn("Error during deletion of temporary workflow location: " + e.getMessage(), e);
                        }
                    } , "Delete temporary copy of " + remoteURI).start();
                }
            }
        }
        final ReferencedFile autoSaveDirectory;
        if (m_manager != null && m_parentEditor == null && m_fileResource != null) {
            autoSaveDirectory = Wrapper.unwrapWFMOptional(m_manager)
                    .map(WorkflowManager::getAutoSaveDirectory)
                    .orElse(null);
        } else {
            autoSaveDirectory = null;
        }
        // remember that this editor has been closed
        m_closed = true;
        for (IEditorPart child : getSubEditors()) {
            IWorkbenchPage page = child.getEditorSite().getPage();
            if (page != null) { // null for child-child editor
                page.closeEditor(child, false);
            }
        }
        NodeProvider.INSTANCE.removeListener(this);
        final ISelectionService iss = getSite().getWorkbenchWindow().getSelectionService();
        iss.removeSelectionListener(this);
        iss.removeSelectionListener(m_connectionHighlighter);
        if (m_manager != null) {
            // Store the editor settings with the metanode
            if ((m_parentEditor != null) && getWorkflowManagerUI().isDirty()) {
                saveEditorSettingsToWorkflowManager(); // doesn't persist settings to disk
            }
            // With AP-15770 & bug fix 2051: memory leak: WorkflowEditor still kept in EditorHistory on dispose.
            IWorkbench workbench = PlatformUI.getWorkbench();
            if (workbench instanceof Workbench) {
                EditorHistory hist = ((Workbench)workbench).getEditorHistory();
                WorkflowManagerInput wfmInput = new WorkflowManagerInput(m_manager, m_parentEditor);
                hist.remove(wfmInput);
            }
        }

        if (m_autoSaveJob != null) {
            m_autoSaveJob.cancel();
            m_autoSaveJob = null;
        }
        setWorkflowManager(null); // unregisters wfm listeners
        if (autoSaveDirectory != null) {
            KNIMEConstants.GLOBAL_THREAD_POOL.enqueue(new Runnable() {
                @Override
                public void run() {
                    LOGGER.debugWithFormat("Deleting auto-saved copy (\"%s\")...", autoSaveDirectory);
                    try {
                        FileUtils.deleteDirectory(autoSaveDirectory.getFile());
                    } catch (IOException e) {
                        LOGGER.error(String.format("Failed to delete auto-saved copy of workflow (folder \"%s\"): %s",
                            autoSaveDirectory, e.getMessage()), e);
                    }
                }
            });
        }
        getCommandStack().removeCommandStackListener(this);
        IPreferenceStore prefStore = KNIMEUIPlugin.getDefault().getPreferenceStore();

        prefStore.removePropertyChangeListener(this);

        if (m_refresher != null) {
            m_refresher.dispose();
        }

        notifyCloseEventListeners();

        // cleanup viewer references to nodes & co
        final GraphicalViewer viewer = getGraphicalViewer();
        viewer.getVisualPartMap().clear();
        viewer.getEditPartRegistry().clear();
        viewer.getRootEditPart().deactivate();
        viewer.getRootEditPart().getContents().deactivate();

        m_overviewOutlinePage = null;

        super.dispose();
    }

    /**
     * Creates the editor actions.
     *
     * @see org.eclipse.gef.ui.parts.GraphicalEditor#createActions()
     */
    @Override
    protected void createActions() {
        LOGGER.debug("creating editor actions...");

        // super already does something for us...
        super.createActions();

        // Stack actions
        final StackAction undo = new UndoAction(this);
        final StackAction redo = new RedoAction(this);

        // Editor Actions
        final WorkbenchPartAction delete = new NodeConnectionContainerDeleteAction(this);
        final WorkbenchPartAction save = new SaveAction(this);
        final WorkbenchPartAction print = new PrintAction(this);
        final WorkbenchPartAction hideNodeName = new HideNodeNamesAction(this);
        final WorkbenchPartAction showNodeIdAction = new ShowNodeIdsAction(this);

        // node actions
        //
        final AbstractNodeAction openDialog = new OpenDialogAction(this);

        final AbstractNodeAction execute = new ExecuteAction(this);
        final AbstractNodeAction executeAll = new ExecuteAllAction(this);
        final AbstractNodeAction cancelAll = new CancelAllAction(this);
        final AbstractNodeAction cancel = new CancelAction(this);
        final AbstractNodeAction pause = new PauseLoopExecutionAction(this);
        final AbstractNodeAction step = new StepLoopAction(this);
        final AbstractNodeAction resume = new ResumeLoopAction(this);
        final AbstractNodeAction executeAndView = new ExecuteAndOpenViewAction(this);
        final AbstractNodeAction reset = new ResetAction(this);
        final AbstractNodeAction selectScope = new SelectLoopAction(this);
        final AbstractNodeAction setNameAndDescription = new SetNodeDescriptionAction(this);
        final AbstractNodeAction toggleFlowVarPorts = new ToggleFlowVarPortsAction(this);
        final AbstractNodeAction defaultOpenView = new DefaultOpenViewAction(this);

        final AbstractNodeAction linkNodes = new LinkNodesAction(this);
        final AbstractNodeAction unlinkNodes = new UnlinkNodesAction(this);

        final AbstractNodeAction metaNodeReConfigure = new MetaNodeReconfigureAction(this);
        final AbstractNodeAction metaNodeChangeLink = new ChangeMetaNodeLinkAction(this);
        final AbstractNodeAction defineMetaNodeTemplate = new SaveAsMetaNodeTemplateAction(this);
        final AbstractNodeAction checkUpdateMetaNodeLink = new CheckUpdateMetaNodeLinkAction(this);
        final AbstractNodeAction revealMetaNodeTemplate = new RevealMetaNodeTemplateAction(this);
        final AbstractNodeAction disconnectMetaNodeLink = new DisconnectMetaNodeLinkAction(this);
        final AbstractNodeAction lockMetaLink = new LockMetaNodeAction(this);

        final AbstractNodeAction subNodeReConfigure = new SubNodeReconfigureAction(this);
        final AbstractNodeAction subNodeChangeLink = new ChangeSubNodeLinkAction(this);
        final AbstractNodeAction defineSubNodeTemplate = new SaveAsSubNodeTemplateAction(this);
        final AbstractNodeAction checkUpdateSubNodeLink = new CheckUpdateMetaNodeLinkAction(this);
        final AbstractNodeAction changeComponentSpaceVersion = new ChangeComponentHubVersionAction(this);
        final AbstractNodeAction revealSubNodeTemplate = new RevealSubNodeTemplateAction(this);
        final AbstractNodeAction disconnectSubNodeLink = new DisconnectSubNodeLinkAction(this);
        final AbstractNodeAction lockSubLink = new LockSubNodeAction(this);

        // annotation actions
        final AddAnnotationAction annotation = new AddAnnotationAction(this);
        final BringAnnotationToFrontAction bringAnnotationToFront = new BringAnnotationToFrontAction(this);
        final BringAnnotationForwardAction bringAnnotationForward = new BringAnnotationForwardAction(this);
        final SendAnnotationBackwardAction sendAnnotationBackward = new SendAnnotationBackwardAction(this);
        final SendAnnotationToBackAction sendAnnotationToBack = new SendAnnotationToBackAction(this);

        // copy / cut / paste action
        final CopyAction copy = new CopyAction(this);
        final CutAction cut = new CutAction(this);
        final PasteAction paste = new PasteAction(this);
        final PasteActionContextMenu pasteContext = new PasteActionContextMenu(this);
        final CollapseMetaNodeAction collapse = new CollapseMetaNodeAction(this);
        final EncapsulateSubNodeAction encapsulate = new EncapsulateSubNodeAction(this);
        final ExpandMetaNodeAction expand = new ExpandMetaNodeAction(this);
        final ExpandSubNodeAction expandSub = new ExpandSubNodeAction(this);
        final ConvertMetaNodeToSubNodeAction wrap = new ConvertMetaNodeToSubNodeAction(this);
        final ConvertSubNodeToMetaNodeAction unWrap = new ConvertSubNodeToMetaNodeAction(this);

        // ui-ext action
        final var openFlowVariableConfig = new OpenFlowVariableConfigAction(this);


        // register the actions
        m_actionRegistry.registerAction(undo);
        m_actionRegistry.registerAction(redo);
        m_actionRegistry.registerAction(delete);
        m_actionRegistry.registerAction(save);
        m_actionRegistry.registerAction(print);

        m_actionRegistry.registerAction(openDialog);
        m_actionRegistry.registerAction(execute);
        m_actionRegistry.registerAction(executeAll);
        m_actionRegistry.registerAction(cancelAll);
        m_actionRegistry.registerAction(cancel);
        m_actionRegistry.registerAction(pause);
        m_actionRegistry.registerAction(step);
        m_actionRegistry.registerAction(resume);
        m_actionRegistry.registerAction(executeAndView);
        m_actionRegistry.registerAction(reset);
        m_actionRegistry.registerAction(selectScope);
        m_actionRegistry.registerAction(toggleFlowVarPorts);
        m_actionRegistry.registerAction(setNameAndDescription);
        m_actionRegistry.registerAction(defaultOpenView);

        m_actionRegistry.registerAction(linkNodes);
        m_actionRegistry.registerAction(unlinkNodes);

        m_actionRegistry.registerAction(copy);
        m_actionRegistry.registerAction(cut);
        m_actionRegistry.registerAction(paste);
        m_actionRegistry.registerAction(pasteContext);
        m_actionRegistry.registerAction(hideNodeName);
        m_actionRegistry.registerAction(showNodeIdAction);
        m_actionRegistry.registerAction(collapse);
        m_actionRegistry.registerAction(encapsulate);
        m_actionRegistry.registerAction(expand);
        m_actionRegistry.registerAction(expandSub);
        m_actionRegistry.registerAction(wrap);
        m_actionRegistry.registerAction(unWrap);

        m_actionRegistry.registerAction(metaNodeReConfigure);
        m_actionRegistry.registerAction(metaNodeChangeLink);
        m_actionRegistry.registerAction(defineMetaNodeTemplate);
        m_actionRegistry.registerAction(checkUpdateMetaNodeLink);
        m_actionRegistry.registerAction(revealMetaNodeTemplate);
        m_actionRegistry.registerAction(disconnectMetaNodeLink);
        m_actionRegistry.registerAction(lockMetaLink);

        m_actionRegistry.registerAction(subNodeReConfigure);
        m_actionRegistry.registerAction(subNodeChangeLink);
        m_actionRegistry.registerAction(defineSubNodeTemplate);
        m_actionRegistry.registerAction(checkUpdateSubNodeLink);
        m_actionRegistry.registerAction(changeComponentSpaceVersion);
        m_actionRegistry.registerAction(revealSubNodeTemplate);
        m_actionRegistry.registerAction(disconnectSubNodeLink);
        m_actionRegistry.registerAction(lockSubLink);

        m_actionRegistry.registerAction(annotation);
        m_actionRegistry.registerAction(bringAnnotationToFront);
        m_actionRegistry.registerAction(bringAnnotationForward);
        m_actionRegistry.registerAction(sendAnnotationBackward);
        m_actionRegistry.registerAction(sendAnnotationToBack);

        // ui-ext action
        m_actionRegistry.registerAction(openFlowVariableConfig);

        // remember ids for later updates via 'updateActions'
        m_editorActions = new ArrayList<>();
        m_editorActions.add(undo.getId());
        m_editorActions.add(redo.getId());
        m_editorActions.add(delete.getId());
        m_editorActions.add(save.getId());

        m_editorActions.add(openDialog.getId());
        m_editorActions.add(execute.getId());
        m_editorActions.add(executeAll.getId());
        m_editorActions.add(cancelAll.getId());
        m_editorActions.add(executeAndView.getId());
        m_editorActions.add(reset.getId());
        m_editorActions.add(setNameAndDescription.getId());
        m_editorActions.add(toggleFlowVarPorts.getId());
        m_editorActions.add(defaultOpenView.getId());
        m_editorActions.add(hideNodeName.getId());
        m_editorActions.add(showNodeIdAction.getId());
        m_editorActions.add(collapse.getId());
        m_editorActions.add(expand.getId());
        m_editorActions.add(unWrap.getId());

        m_editorActions.add(linkNodes.getId());
        m_editorActions.add(unlinkNodes.getId());

        m_editorActions.add(copy.getId());
        m_editorActions.add(cut.getId());
        m_editorActions.add(paste.getId());
        m_editorActions.add(metaNodeReConfigure.getId());
        m_editorActions.add(subNodeReConfigure.getId());
        m_editorActions.add(metaNodeChangeLink.getId());
        m_editorActions.add(defineMetaNodeTemplate.getId());
        m_editorActions.add(checkUpdateMetaNodeLink.getId());

        m_editorActions.add(annotation.getId());
        m_editorActions.add(bringAnnotationToFront.getId());
        m_editorActions.add(bringAnnotationForward.getId());
        m_editorActions.add(sendAnnotationBackward.getId());
        m_editorActions.add(sendAnnotationToBack.getId());

        // ui-ext action
        m_editorActions.add(openFlowVariableConfig.getId());
    }

    /**
     * This hooks keys like F2 for editing inside the editor.
     *
     * @return The common (shared) key handler.
     */
    protected KeyHandler getCommonKeyHandler() {
        return new KeyHandler() {
            @Override
            public boolean keyPressed(final org.eclipse.swt.events.KeyEvent e) {
                if (e.keyCode == SWT.F2) {
                    onF2Pressed();
                }
                return super.keyPressed(e);
            }
        };
    }

    /** Opens editor for (node) annotation (given that a single node or
     * annotation is selected). */
    private void onF2Pressed() {
        ISelectionProvider provider = getEditorSite().getSelectionProvider();
        if (provider == null) {
            return;
        }
        ISelection sel = provider.getSelection();
        if (!(sel instanceof IStructuredSelection)) {
            return;
        }

        Set<AnnotationEditPart> selectedAnnoParts = new HashSet<>();
        @SuppressWarnings("rawtypes")
        Iterator selIter = ((IStructuredSelection)sel).iterator();
        while (selIter.hasNext()) {
            Object next = selIter.next();
            if (next instanceof AnnotationEditPart) {
                selectedAnnoParts.add((AnnotationEditPart)next);
            } else if (next instanceof NodeContainerEditPart) {
                NodeAnnotationEditPart nodeAnnoPart = ((NodeContainerEditPart)next).getNodeAnnotationEditPart();
                if (nodeAnnoPart != null) {
                    selectedAnnoParts.add(nodeAnnoPart);
                }
            } else {
                return; // unknown type selected
            }
        }
        if (selectedAnnoParts.size() == 1) {
            AnnotationEditPart next = selectedAnnoParts.iterator().next();
            next.performEdit(null);
        }
    }

    /**
     * Returns the action registry for this editor. It is "lazy" created on
     * first invocation.
     *
     * @see org.eclipse.gef.ui.parts.GraphicalEditor#getActionRegistry()
     * @return The action registry
     */
    @Override
    public ActionRegistry getActionRegistry() {
        if (m_actionRegistry == null) {
            m_actionRegistry = new ActionRegistry();
        }
        return m_actionRegistry;
    }

    /**
     * Creates the graphical viewer that is hosted in this editor and hooks
     * keyhandler and edit domain.
     *
     * @see org.eclipse.gef.ui.parts.GraphicalEditor
     *      #createGraphicalViewer(org.eclipse.swt.widgets.Composite)
     * @param parent The parent
     */
    @SuppressWarnings("restriction") // WorkbenchHelpSystem.getInstance().setHelp(...) is discouraged API
    @Override
    protected void createGraphicalViewer(final Composite parent) {
        final IEditorSite editorSite = getEditorSite();
        final ViewportPinningGraphicalViewer viewer =
            new WorkflowGraphicalViewerCreator(editorSite, getActionRegistry()).createViewer(parent);

        // Add a listener to the static node provider
        NodeProvider.INSTANCE.addListener(this);

        // Configure the key handler
        final GraphicalViewerKeyHandler keyHandler = new GraphicalViewerKeyHandler(viewer);

        final KeyHandler parentKeyHandler = keyHandler.setParent(getCommonKeyHandler());
        viewer.setKeyHandler(parentKeyHandler);

        setGraphicalViewer(viewer);

        // activate the viewer as selection provider for Eclipse
        getSite().setSelectionProvider(viewer);

        // load properties like grid- or node-connections settings (e.g. width, curved)
        // needs to be called before getGraphicalViewer().setContents(m_manager), since
        // the node connections are repainted on that setContents-call and the properties need
        // to be set by then
        loadProperties();

        // We already have the model - set it into the viewer; this action instantiates our WorkflowFigure
        viewer.setContents(m_manager);
        final WorkflowFigure workflowFigure = getWorkflowFigure();
        final FigureCanvas canvas = getFigureCanvas();
        workflowFigure.setViewport(canvas.getViewport());
        // TODO don't we want the viewport and not the figure canvas for 'control listening'?
        canvas.addControlListener(workflowFigure);


        // add Help context
        WorkbenchHelpSystem.getInstance().setHelp(getGraphicalViewer().getControl(),
            "org.knime.workbench.help.flow_editor_context");

        updateEditorBackgroundColor();
        updateJobManagerDisplay();
        updateWorkflowMessages();
        RootEditPart rep = getGraphicalViewer().getRootEditPart();
        ((WorkflowRootEditPart)rep.getChildren().get(0)).createToolTipHelper(getSite().getShell());

        final ZoomManager zm = getZoomManager();
        m_mouseWheelListener = new EditorMouseWheelListener(zm, getFigureCanvas());

        final ZoomInAction zoomIn = new ZoomInAction(zm);
        m_alternateZoomInAction = new ZoomInAlternateIncrementAction(zm);
        final ZoomOutAction zoomOut = new ZoomOutAction(zm);
        m_alternateZoomOutAction = new ZoomOutAlternateIncrementAction(zm);
        final ZoomResetAction zoomReset = new ZoomResetAction(zm);
        final ActionRegistry registry = getActionRegistry();
        registry.registerAction(zoomReset);
        registry.registerAction(m_alternateZoomOutAction);
        registry.registerAction(zoomOut);
        registry.registerAction(m_alternateZoomInAction);
        registry.registerAction(zoomIn);

        final IHandlerService handlerService = getSite().getService(IHandlerService.class);
        handlerService.activateHandler(ZoomResetAction.KEY_COMMAND_ID, zoomReset);
        handlerService.activateHandler(ZoomInAlternateIncrementAction.KEY_COMMAND_ID, m_alternateZoomInAction);
        handlerService.activateHandler(ZoomOutAlternateIncrementAction.KEY_COMMAND_ID, m_alternateZoomOutAction);

        m_nodeSupplantDragListener = new NodeSupplantDragListener(this);
        if (m_manager != null) { // should "never" be null here...
            m_manager.addListener(m_nodeSupplantDragListener);
            m_manager.addListener(workflowFigure);
        }

        m_canvasClickListener = new WorkflowCanvasClickListener(this);

        updateConnectionHighlighting();
        updateZoomLevelSettings();
    }

    /**
     * This does nothing by now, as all is handled by {@link #createGraphicalViewer(Composite)}
     *
     * {@inheritDoc}
     */
    @Override
    protected void initializeGraphicalViewer() { }

    /**
     * Simply redeclared as other classes in this class' package invoke this protected method; needing to do this is,
     * arguably, a fault of the resolution logic of 'protected' in the Java language.
     *
     * {@inheritDoc}
     */
    @Override
    protected GraphicalViewer getGraphicalViewer() {
        return super.getGraphicalViewer();
    }

    /**
     * This is public exposure of the canvas click listener which allows for interested consumers to register themselves
     * for notification of mode exit. This will return null until the graphical viewer has been created.
     *
     * @return the {@link WorkflowCanvasClickListener} instance created and held by this instance
     * @see #createGraphicalViewer(Composite)
     */
    public WorkflowCanvasClickListener getCanvasClickListener() {
        return m_canvasClickListener;
    }

    /**
     * Returns the URI to the workflow. If this is a metanode editor, it traverses up until it finds the top. Should
     * never return null - returns null if it has no file resource AND no parent....
     * @return the URI to the workflow. If this is a metanode editor, it traverses up until it finds the top.
     */
    public URI getWorkflowURI() {
        if (m_fileResource != null) {
            return m_fileResource;
        }
        if (m_parentEditor != null) {
            return m_parentEditor.getWorkflowURI();
        }
        return null;
    }

    /**
     * Returns the root editor.
     *
     * @return the editor one ends up at when following the chain of parent editors to the end
     */
    public WorkflowEditor getRootEditor() {
        var editor = this;
        while (editor.m_parentEditor != null) {
            editor = editor.m_parentEditor;
        }
        return editor;
    }

    /**
     * Sets the editor input, that is, the file that contains the serialized
     * workflow manager.
     *
     * {@inheritDoc}
     */
    @Override
    public void setInput(final IEditorInput input) {
        LOGGER.debug("Setting input into editor...");
        super.setInput(input);

        //order of instance check important here
        //see WorkflowManagerURIInput
        if (input instanceof WorkflowManagerInput) { // metanode, subnode, remote workflows
            setWorkflowManagerInput((WorkflowManagerInput) input);
        } else if (input instanceof IURIEditorInput) {
            final var uriInput = (IURIEditorInput) input;
            File wfFile;
            AbstractExplorerFileStore wfFileFileStore = null;
            boolean isComponentTemplate = false;

            WorkflowContextV2 workflowContext = null;

            final var uri = uriInput.getURI();
            if (uriInput instanceof RemoteWorkflowInput) {
                workflowContext = ((RemoteWorkflowInput) uriInput).getWorkflowContext();
            }
            if ("file".equals(uri.getScheme())) {
                // most workflows are imported using the `knime://` URI in their mountpoint, this is for `.knwf`
                // (and potentially other obscure use-cases, please add those here)
                wfFile = new File(uri);
                if (workflowContext == null) {
                    try {
                        LocalExplorerFileStore fs = ExplorerFileSystem.INSTANCE.fromLocalFile(wfFile);
                        if ((fs == null) || (fs.getContentProvider() == null)) {
                            LOGGER.info("Could not determine mount point root for " + wfFile.getParent()
                                + ", looks like it is a linked resource");
                        } else {
                            wfFileFileStore = fs;
                            final var optLocationInfo = wfFileFileStore.locationInfo();
                            if (optLocationInfo.isEmpty()) {
                                LOGGER.error("Could not determine location info for URI " + uri);
                                openErrorDialogAndCloseEditor("Could not determine location info for URI " + uri);
                                return;
                            }

                            final var locationInfo = optLocationInfo.get();
                            final var localPath = wfFile.getParentFile().toPath().toAbsolutePath();
                            final var contentProvider = fs.getContentProvider();
                            final var mpRootStore = contentProvider.getRootStore();
                            final var mountpointRoot = mpRootStore.toLocalFile().toPath();
                            workflowContext = WorkflowContextV2.builder()
                                    .withAnalyticsPlatformExecutor(builder -> {
                                        final var builder2 = builder
                                                .withCurrentUserAsUserId()
                                                .withLocalWorkflowPath(localPath);
                                        if (!contentProvider.getFactory().isTempSpace()) {
                                            builder2.withMountpoint(contentProvider.getMountID(), mountpointRoot);
                                        }
                                        return builder2;
                                    })
                                    .withLocation(locationInfo)
                                    .build();
                        }
                    } catch (CoreException ex) {
                        LOGGER.warn("Could not determine mount point root for " +
                                wfFile.getParent() + ": " + ex.getMessage(), ex);
                    }
                }
            } else if (ExplorerFileSystem.SCHEME.equals(uri.getScheme())) {
                AbstractExplorerFileStore filestore = ExplorerFileSystem.INSTANCE.getStore(uri);
                if (filestore == null) {
                    LOGGER.error("Could not find filestore for URI " + uri);
                    openErrorDialogAndCloseEditor("Could not find filestore for URI " + uri);
                    return;
                }
                wfFileFileStore = filestore;
                final var contentProvider = filestore.getContentProvider();
                final java.nio.file.Path mountpointRoot;
                try {
                    wfFile = filestore.toLocalFile();
                    final var mpRootStore = contentProvider.getRootStore();
                    mountpointRoot = mpRootStore.toLocalFile().toPath();
                } catch (CoreException ex) {
                    LOGGER.error(ex.getMessage(), ex);
                    openErrorDialogAndCloseEditor(ex.getMessage());
                    return;
                }
                if (wfFile == null) {
                    LOGGER.error("URI " + uri + " is not a local workflow");
                    openErrorDialogAndCloseEditor("URI " + uri + " is not a local workflow");
                    return;
                }
                if (workflowContext == null) {
                    final var optLocationInfo = wfFileFileStore.locationInfo();
                    if (optLocationInfo.isEmpty()) {
                        LOGGER.error("Could not determine location info for URI " + uri);
                        openErrorDialogAndCloseEditor("Could not determine location info for URI " + uri);
                        return;
                    }
                    final var localPath = wfFile.getParentFile().toPath().toAbsolutePath();
                    workflowContext = WorkflowContextV2.builder()
                            .withAnalyticsPlatformExecutor(builder -> {
                                final var builder2 = builder
                                        .withCurrentUserAsUserId()
                                        .withLocalWorkflowPath(localPath);
                                if (!contentProvider.getFactory().isTempSpace()) {
                                    builder2.withMountpoint(contentProvider.getMountID(), mountpointRoot);
                                }
                                return builder2;
                            })
                            .withLocation(optLocationInfo.get())
                            .build();
                }
                AbstractExplorerFileStore parent = filestore.getParent();
                isComponentTemplate = AbstractExplorerFileStore.isWorkflowTemplate(parent);
            } else {
                LOGGER.error("Unsupported scheme for workflow URI: " + uri);
                openErrorDialogAndCloseEditor("Unsupported scheme for workflow URI: " + uri);
                return;
            }

            URI oldFileResource = m_fileResource;
            WorkflowManagerUI oldManager = m_manager;

            final var wfDir = wfFile.getParentFile();
            m_fileResource = wfDir.toURI();

            LOGGER.debug("Resource File's project: " + m_fileResource);
            boolean isEnableAutoSave = true;
            try {
                if (oldManager != null) { // doSaveAs called
                    assert oldFileResource != null;
                    WorkflowManagerUI managerForOldResource =
                            (WorkflowManagerUI)ProjectWorkflowMap.getWorkflowUI(oldFileResource);
                    if (m_manager != managerForOldResource) {
                        throw new IllegalStateException(String.format("Cannot set new input for workflow editor "
                                + "as there was already a workflow manager set (old resource: \"%s\", "
                                + "new resource: \"%s\", old manager: \"%s\", manager to old resource: \"%s\"",
                                oldFileResource, m_fileResource, oldManager, managerForOldResource));
                    }
                    ProjectWorkflowMap.replace(m_fileResource, oldManager, oldFileResource);
                    isEnableAutoSave = m_isAutoSaveAllowed;
                } else {
                    m_manager = (WorkflowManagerUI)ProjectWorkflowMap.getWorkflowUI(m_fileResource);
                }

                if (m_manager != null) {
                    // in case the workflow manager was edited somewhere else
                    if (m_manager.isDirty()) {
                        markDirty();
                    }
                } else {
                    File autoSaveDirectory = WorkflowSaveHelper.getAutoSaveDirectory(new ReferencedFile(wfDir));
                    if (autoSaveDirectory.exists()) {
                        if (!autoSaveDirectory.isDirectory() || !autoSaveDirectory.canRead()) {
                            LOGGER.warnWithFormat("Found existing auto-save location to workflow \"%s\" (\"%s\") but %s"
                                + " - disabling auto-save", wfDir.getName(), autoSaveDirectory.getAbsolutePath(),
                                (!autoSaveDirectory.isDirectory() ? "it is not a directory" : "cannot read it"));
                            isEnableAutoSave = false;
                        } else {
                            final var parentDir = autoSaveDirectory.getParentFile();
                            String date = DateFormatUtils.format(autoSaveDirectory.lastModified(), "yyyy-MM-dd HH-mm");
                            String newName = wfDir.getName() + " (Auto-Save Copy - " + date + ")";
                            var unique = 1;
                            File restoredAutoSaveDirectory;
                            while ((restoredAutoSaveDirectory = new File(parentDir, newName)).exists()) {
                                newName = wfDir.getName() + " (Auto-Save Copy - " + date + " #" + (unique++) + ")";
                            }

                            // this is the file store object to autoSaveDirectory - if we can resolve it
                            // we use it below in user messages and to do the rename in order to trigger a refresh
                            // in the explorer tree - if we can't resolve it (dunno why) we use java.io.File operation
                            AbstractExplorerFileStore autoSaveDirFileStore = null;
                            AbstractExplorerFileStore restoredAutoSaveDirFileStore = null;
                            if (wfFileFileStore != null) {
                                try {
                                    // first parent is workflow dir, parent of that is the workflow group
                                    AbstractExplorerFileStore parFS = wfFileFileStore.getParent().getParent();
                                    AbstractExplorerFileStore temp = parFS.getChild(autoSaveDirectory.getName());
                                    if (autoSaveDirectory.equals(temp.toLocalFile())) {
                                        autoSaveDirFileStore = temp;
                                    }
                                    restoredAutoSaveDirFileStore = parFS.getChild(newName);
                                } catch (CoreException e) {
                                    LOGGER.warn("Unable to resolve parent file store for \"" + wfFileFileStore + "\"", e);
                                }
                            }

                            int action = openQuestionDialogWhenLoadingWorkflowWithAutoSaveCopy(
                                wfDir.getName(), restoredAutoSaveDirectory.getName());

                            final boolean openCopy;
                            switch (action) {
                                case 0: // Open Copy
                                    openCopy = true;
                                    break;
                                case 1: // Open Original
                                    openCopy = false;
                                    break;
                                default: // Cancel
                                    final var error = "Canceling due to auto-save copy conflict";
                                    openErrorDialogAndCloseEditor(error);
                                    throw new OperationCanceledException(error);
                            }

                            boolean couldRename = false;
                            if (autoSaveDirFileStore != null) { // preferred way to rename, updates explorer tree
                                try {
                                    autoSaveDirFileStore.move(restoredAutoSaveDirFileStore, EFS.NONE, null);
                                    couldRename = true;
                                } catch (CoreException e) {
                                    String message = "Could not rename auto-save copy\n"
                                            + "from\n  " + autoSaveDirFileStore.getMountIDWithFullPath()
                                            + "\nto\n  " + newName;
                                    LOGGER.error(message, e);
                                }
                            } else {
                                LOGGER.warnWithFormat("Could not resolve explorer file store to \"%s\" - "
                                        + "renaming on file system directly", autoSaveDirectory.getAbsolutePath());
                                // just rename on file system and ignore explorer tree
                                couldRename = autoSaveDirectory.renameTo(restoredAutoSaveDirectory);
                            }
                            if (!couldRename) {
                                isEnableAutoSave = false;
                                String message = "Could not rename auto-save copy\n"
                                        + "from\n  " + autoSaveDirectory.getAbsolutePath() + "\nto\n  "
                                        + restoredAutoSaveDirectory.getAbsolutePath() + "";
                                if (openCopy) {
                                    openErrorDialogAndCloseEditor(message);
                                    throw new OperationCanceledException(message);
                                } else {
                                    MessageDialog.openWarning(SWTUtilities.getActiveShell(),
                                        "Auto-Save Rename Problem", message + "\nAuto-Save will be disabled.");
                                }
                            } else if (Platform.OS_WIN32.equals(Platform.getOS())) {
                                // the Autosave file might be hidden in DOS based systems
                                try {
                                    boolean isHidden =
                                        (boolean)Files.getAttribute(restoredAutoSaveDirectory.toPath(), "dos:hidden");
                                    if (isHidden) {
                                        Files.setAttribute(restoredAutoSaveDirectory.toPath(), "dos:hidden", false);
                                    }
                                } catch (IOException e) {
                                    LOGGER.warn(e);
                                }
                            }
                            if (openCopy) {
                                m_fileResource = restoredAutoSaveDirectory.toURI();
                                wfFile = new File(restoredAutoSaveDirectory, wfFile.getName());
                            }
                        }
                    }

                    final var progressService = PlatformUI.getWorkbench().getProgressService();
                    // this one sets the workflow manager in the editor
                    LoadWorkflowRunnable loadWorkflowRunnable = null;
                    if (!isComponentTemplate) {
                        loadWorkflowRunnable = new LoadWorkflowRunnable(this, wfFile, workflowContext);
                        progressService.busyCursorWhile(loadWorkflowRunnable);
                    } else {
                        final var loadTemplateRunnable = new LoadMetaNodeTemplateRunnable(this, uri,
                            CheckUtils.checkNotNull(workflowContext), false);
                        progressService.busyCursorWhile(loadTemplateRunnable);
                        if (m_manager != null && Wrapper.wraps(m_manager, WorkflowManager.class)
                            && m_manager.isEncrypted()) {
                            WorkflowCipherPrompt prompt = new GUIWorkflowCipherPrompt(true);
                            if (!Wrapper.unwrapWFM(m_manager).unlock(prompt)) {
                                m_manager = null;
                                final var accessDeniedMessage = "Component could not be opened: Access denied";
                                openErrorDialogAndCloseEditor(accessDeniedMessage);
                                throw new OperationCanceledException(accessDeniedMessage);
                            }
                        }
                    }

                    // check if the editor should be disposed
                    // non-null if set by workflow runnable above
                    if (m_manager == null && loadWorkflowRunnable != null) {
                        if (loadWorkflowRunnable.hasLoadingBeenCanceled()) {
                            String cancelError = loadWorkflowRunnable.getLoadingCanceledMessage();
                            openErrorDialogAndCloseEditor(cancelError);
                            throw new OperationCanceledException(cancelError);
                        } else if (loadWorkflowRunnable.getThrowable() != null) {
                            throw new RuntimeException(loadWorkflowRunnable.getThrowable());
                        }
                    }
                    ProjectWorkflowMap.putWorkflowUI(m_fileResource, m_manager);
                    setupChangesTracker();
                }
                if (oldManager == null) { // not null if via doSaveAs
                    // in any case register as client (also if the workflow was already loaded by another client
                    ProjectWorkflowMap.registerClientTo(m_fileResource, this);
                }
            } catch (InterruptedException ie) {
                LOGGER.fatal("Workflow loading thread interrupted", ie);
            } catch (InvocationTargetException e) {
                LOGGER.fatal("Workflow could not be loaded.", e);
            }

            m_isAutoSaveAllowed = m_parentEditor == null && isEnableAutoSave;
            setupAutoSaveSchedule();

            m_manuallySetToolTip = null;
            updatePartName();
            if (getGraphicalViewer() != null) {
                loadProperties();
                updateWorkflowMessages();
            }


            // Per AP-11628, we want the metadata to be displayed in the Description View when a workflow is opened
            final Runnable workflowSelectionForMetadataDisplay = () -> {
                final GraphicalViewer viewer = getGraphicalViewer();

                viewer.deselectAll();
                viewer.select(viewer.getRootEditPart().getContents());

                setFocus();
            };
            if (m_hasAfterOpenRun) {
                KNIMEConstants.GLOBAL_THREAD_POOL.enqueue(() -> {
                    final Display d = PlatformUI.getWorkbench().getDisplay();
                    final GraphicalViewer viewer = getGraphicalViewer();

                    // In this case, we potentially already have the workflow root as the selection, so we force
                    //      a twiddle; this won't work for the case of a workflow undergoing save-as with
                    //      no content (in which case, if downloaded from a server, the user will not see a refreshed
                    //      edit button display automatically.)
                    d.asyncExec(() -> {
                        final EditPart editorPart = viewer.getRootEditPart().getChildren().get(0);
                        final List<?> workflowAssets = editorPart.getChildren();

                        viewer.deselectAll();
                        if (workflowAssets.size() > 0) {
                            viewer.select((EditPart)workflowAssets.get(0));
                        }
                        d.asyncExec(workflowSelectionForMetadataDisplay);
                    });
                });
            } else {
                addAfterOpenRunnable(workflowSelectionForMetadataDisplay);
            }

            MRUFileMenuItem.editorHasOpenedWithInput(uriInput);

            // update Actions, as now there's everything available
            updateActions();
        } else {
            throw new IllegalArgumentException("Unsupported editor input: " + input.getClass());
        }
    }

    private void setupAutoSaveSchedule() {
        IPreferenceStore prefStore = KNIMEUIPlugin.getDefault().getPreferenceStore();
        Boolean wasSavingWithData = null;
        if (m_autoSaveJob != null) {
            LOGGER.debugWithFormat("Clearing auto-save job for %s", m_manager.getName());
            m_autoSaveJob.cancel();
            wasSavingWithData = m_autoSaveJob.isSavingWithData();
            m_autoSaveJob = null;
        }
        if (m_isAutoSaveAllowed && prefStore.getBoolean(PreferenceConstants.P_AUTO_SAVE_ENABLE)) {
            final var interval = prefStore.getInt(PreferenceConstants.P_AUTO_SAVE_INTERVAL);
            final var saveWithData = prefStore.getBoolean(PreferenceConstants.P_AUTO_SAVE_DATA);
            if (wasSavingWithData != null && wasSavingWithData.booleanValue() != saveWithData) {
                m_manager.setAutoSaveDirectoryDirtyRecursivly();
            }
            m_autoSaveJob = new AutoSaveJob(saveWithData, interval);
            m_autoSaveJob.scheduleNextAutoSave();
            LOGGER.debugWithFormat("Scheduled auto-save job for %s (every %d secs %s data)", m_manager.getName(),
                interval, (saveWithData ? "with" : "without"));
        }

    }

    /* Only called on project or component project wfms */
    private void setupChangesTracker() {
        if(getWorkflowManager().isPresent()) {
            WorkflowManager wfm = getWorkflowManager().get();
            if (wfm.isComponentProjectWFM()) {
                wfm.getProjectComponent().get().initChangesTracker();
            }
        }
    }

    /** Opens dialog to ask user for action when auto-save copy is found. Return values:
     * <ul>
     * <li>0: Open Copy</li>
     * <li>1: Open Original</li>
     * <li>any other value: Cancel</li>
     * </ul>
     * @param workflowName to print in message
     * @param restoredPath the path where the workflow will be restored to.
     * @return as above...
     */
    private static int openQuestionDialogWhenLoadingWorkflowWithAutoSaveCopy(final String workflowName,
            final String restoredPath) {
        final var buttons = new String[]{ "Open Auto-Save Co&py", "Open &Original", "&Cancel" };
        final var messageBuilder = new StringBuilder("\"");
        messageBuilder.append(workflowName).append("\" was not properly closed.\n\n");
        messageBuilder.append("An auto-saved copy will be restored to \"").append(restoredPath).append("\".\n");
        final var shell = SWTUtilities.getActiveShell();
        final var dialog = new MessageDialog(shell, "Detected auto-save copy", null, messageBuilder.toString(),
            MessageDialog.QUESTION, buttons, 0);
        return dialog.open();
    }

    private void updateJobManagerDisplay() {
        final NodeExecutionJobManager jobManager = m_manager.findJobManager();
        URL url;
        if (jobManager instanceof AbstractNodeExecutionJobManager) {
            url = ((AbstractNodeExecutionJobManager)jobManager).getIconForWorkflow();
        } else {
            url = null;
        }
        Image image;
        if (url != null) {
            image = ImageRepository.getUnscaledImage(url);
        } else {
            image = null;
        }
        getWorkflowFigure().setJobManagerFigure(image);
    }

    private void updatePartName() {
        // Editor name (title)
        setPartName(getTitleToolTip());
        setTitleToolTip(getTitleToolTip());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setTitleToolTip(final String toolTip) {
        m_manuallySetToolTip = toolTip;
        super.setTitleToolTip(toolTip);
    }

    @Override
    public String getTitleToolTip() {
        // only for projects -> they have file resources in title
        // when renamed, they cannot reflect the changes, thus, the manually
        // set title is returned...
        // metanodes can do not have file resources in title
        if (m_manuallySetToolTip != null && m_parentEditor == null) {
            return m_manuallySetToolTip;
        }

        // if it's not an ordinary local workflow manager it is a workflow job
        // (since there aren't any other WorkflowManagerUI implementations, yet)
        // -> add a prefix to the title
        final var optWFM = Wrapper.unwrapWFMOptional(m_manager);
        final var nameBuilder = new StringBuilder(optWFM.isPresent() ? "" : "Job ");
        if (m_parentEditor == null) {
            final String path = optWFM
                    .map(WorkflowManager::getContextV2)
                    .flatMap(WorkflowContextV2::getTempSourceLocation)
                    .orElse(m_fileResource)
                    .getPath();

            final String nodeID = m_manager.getID().toString();
            //remove trailing ":0" from id for component projects
            nameBuilder.append(isComponentProjectWFM() ? nodeID.replace(":0", "") : nodeID);
            nameBuilder.append(": ").append(new Path(path).lastSegment());
        } else {
            // we are a metanode editor, return id and node name (custom name)
            nameBuilder.append(m_manager.getDisplayLabel());
        }
        return nameBuilder.toString();
    }

    private void setWorkflowManagerInput(final WorkflowManagerInput input) {
        final URI oldFileResource = m_fileResource;
        m_parentEditor = input.getParentEditor();
        m_fileResource = input.getWorkflowLocation();

        final WorkflowManagerUI wfm = input.getWorkflowManager();
        setWorkflowManagerUI(wfm);
        setPartName(input.getName());
        if (getGraphicalViewer() != null) {
            loadProperties();
        }

        // update Actions, as now there's everything available
        updateActions();
        m_manuallySetToolTip = null;
        updatePartName();

        // also called from doSaveAs for projects -- old m_fileResource != null
        if (oldFileResource != null) {
            ProjectWorkflowMap.replace(m_fileResource, m_manager, oldFileResource);
        }

        if (!Wrapper.wraps(wfm, WorkflowManager.class)) {
            //set different icon for job view
            setTitleImage(ImageRepository.getIconImage(SharedImages.ServerJob));
        }

        //initialize workflow refresh if workflow manager is of type AsyncWorkflowManagerUI
        if (wfm instanceof AsyncWorkflowManagerUI) {
            m_refresher = new WorkflowEditorRefresher(this, () -> {
                Display.getDefault().syncExec(() -> {
                    updateWorkflowMessages();
                    updateEditorBackgroundColor();
                    updateActions();
                });
            });
            m_refresher.setup();
        } else {
            // Per AP-13066, we want the metadata to be displayed in the Description View when a workflow is opened
            KNIMEConstants.GLOBAL_THREAD_POOL.enqueue(() -> {
                PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
                    final GraphicalViewer viewer = getGraphicalViewer();

                    viewer.deselectAll();
                    viewer.select(viewer.getRootEditPart().getContents());

                    setFocus();
                });
            });
        }
    }

    /** The file store to the argument URI (can be file: or knime:).
     * @param uri The uri associated with the editor
     * @return The corresponding file store or null if it can't be resolved.
     */
    private static Optional<AbstractExplorerFileStore> getFileStore(final URI uri) {
        if ("file".equals(uri.getScheme())) {
            return Optional.ofNullable(ExplorerFileSystem.INSTANCE.fromLocalFile(new File(uri)));
        } else if (ExplorerFileSystem.SCHEME.equals(uri.getScheme())) {
            return Optional.of(ExplorerFileSystem.INSTANCE.getStore(uri));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Updates the actions of this workflow editor. Can be used from subclassing
     * objects to update the actions.
     */
    public void updateActions() {
        // TODO: update here the actions in the action bar based on current selection
        // -> maybe solves the execute all enabled/disabled problem
        updateActions(m_editorActions);
    }

    /**
     * Returns the overview for the outline view.
     *
     * @return the overview
     */
    protected NewOverviewOutlinePage getOverviewOutlinePage() {
        if ((m_overviewOutlinePage == null) && (getGraphicalViewer() != null)) {
            final var rootEditPart = getGraphicalViewer().getRootEditPart();
            if (rootEditPart instanceof ScalableFreeformRootEditPart) {
                m_overviewOutlinePage = new NewOverviewOutlinePage((ScalableFreeformRootEditPart)rootEditPart);
            }
        }

        return m_overviewOutlinePage;
    }

    /**
     * Returns the undoable <code>PropertySheetPage</code> for this editor.
     *
     * @return the undoable <code>PropertySheetPage</code>
     */
    protected PropertySheetPage getPropertySheetPage() {
        if (m_undoablePropertySheetPage == null) {
            m_undoablePropertySheetPage = new PropertySheetPage();
            m_undoablePropertySheetPage.setRootEntry(new UndoablePropertySheetEntry(getCommandStack()));
        }

        return m_undoablePropertySheetPage;
    }

    /**
     * @return The WFM that is edited by this editor ("root model object") or an empty optional if there is only a
     *         UIWorkflowManager (see {@link #getWorkflowManagerUI()}).
     */
    public Optional<WorkflowManager> getWorkflowManager() {
        return Wrapper.unwrapOptional(m_manager, WorkflowManager.class);
    }

    /**
     * @return The UI-WFM that is edited by this editor
     */
    public WorkflowManagerUI getWorkflowManagerUI() {
        return m_manager;
    }

    /**
     * Adaptable implementation for Editor, returns the objects used in this
     * editor, if asked for.
     *
     * @see org.eclipse.gef.ui.parts.GraphicalEditor
     *      #getAdapter(java.lang.Class)
     * @param adapter The adapters class
     * @return The adapter object
     */
    @Override
    public Object getAdapter(@SuppressWarnings("rawtypes") final Class adapter) {
        // we need to handle common GEF elements we created
        if (adapter == GraphicalViewer.class) {
            return this.getGraphicalViewer();
        } else if (adapter == EditPartViewer.class) {
            return this.getGraphicalViewer();
        } else if (adapter == CommandStack.class) {
            return this.getCommandStack();
        } else if (adapter == EditDomain.class) {
            return this.getEditDomain();
        } else if (adapter == ActionRegistry.class) {
            return this.getActionRegistry();
        } else if (adapter == IPropertySheetPage.class) {
            return this.getPropertySheetPage();
        } else if (adapter == IContentOutlinePage.class) {
            return this.getOverviewOutlinePage();
        } else if (adapter == ZoomManager.class) {
            return getGraphicalViewer().getProperty(
                    ZoomManager.class.toString());
        }

        if (adapter == WorkflowEditorAdapter.class) {
            // hackaround to deliver the wfm to the navigator
            return new WorkflowEditorAdapter(getWorkflowManagerUI(), m_parentEditor);
        }

        // the super implementation handles the rest
        return super.getAdapter(adapter);
    }

    /**
     * Sets the snap functionality and zoomlevel.
     */
    private void loadProperties() {
        // Snap to Geometry property
        GraphicalViewer graphicalViewer = getGraphicalViewer();
        graphicalViewer.setProperty(SnapToGeometry.PROPERTY_SNAP_ENABLED,
                Boolean.TRUE);

        if (getWorkflowManagerUI() != null) {
            applyEditorSettingsFromWorkflowManager();
        }
    }

    /** Sets background color according to edit mode (see
     * {@link WorkflowManager#isWriteProtected()}. */
    public void updateEditorBackgroundColor() {
        final Color color;
        if (m_manager instanceof AsyncWorkflowManagerUI) {
            assert m_refresher != null;
            if (m_manager.isWriteProtected() || !m_refresher.isConnected() || m_refresher.isWorkflowEditDisabled()) {
                color = BG_COLOR_WRITE_LOCK;
            } else {
                color = BG_COLOR_DEFAULT;
            }
        } else {
            if (m_manager.isWriteProtected()) {
                color = BG_COLOR_WRITE_LOCK;
            } else {
                color = BG_COLOR_DEFAULT;
            }
        }
        runInDisplayThread(() -> {
            GraphicalViewer gv = getGraphicalViewer();
            Control control = gv.getControl();
            control.setBackground(color);
        });
    }

    private static void runInDisplayThread(final Runnable task) {
        Display display = Display.getDefault();
        if (display.getThread() == Thread.currentThread()) {
            task.run();
        } else {
            display.asyncExec(task);
        }
    }

    private void updateZoomLevelSettings() {
        final ZoomManager zm = getZoomManager();
        final IPreferenceStore store = KNIMEUIPlugin.getDefault().getPreferenceStore();
        final int alternateDelta = store.getInt(PreferenceConstants.P_EDITOR_ZOOM_MODIFIED_DELTA);

        m_mouseWheelListener.setZoomDelta(alternateDelta);
        m_alternateZoomInAction.setZoomDelta(alternateDelta);
        m_alternateZoomOutAction.setZoomDelta(alternateDelta);

        final String zoomLevelsPreference = store.getString(PreferenceConstants.P_EDITOR_ZOOM_LEVELS);
        final String[] levels = zoomLevelsPreference.split(",");
        final double[] zoomLevels = new double[levels.length];
        for (int i = 0; i < zoomLevels.length; i++) {
            zoomLevels[i] = (Double.parseDouble(levels[i].trim())) / 100.0;
        }
        zm.setZoomLevels(zoomLevels);

        if (m_zoomComboBox != null) {
            // we need do this couplet due to the code in setZoomManager
            m_zoomComboBox.setZoomManager(null);
            m_zoomComboBox.setZoomManager(zm);
        }
    }

    private void updateConnectionHighlighting() {
        final IPreferenceStore store = KNIMEUIPlugin.getDefault().getPreferenceStore();
        final boolean showHighlights = store.getBoolean(PreferenceConstants.P_EDITOR_SELECTED_NODE_HIGHLIGHT_CONNECTIONS);
        final int widthDelta = store.getInt(PreferenceConstants.P_EDITOR_SELECTED_NODE_CONNECTIONS_WIDTH_DELTA);
        final RGB highlightColor
            = PreferenceConverter.getColor(store, PreferenceConstants.P_EDITOR_SELECTED_NODE_CONNECTIONS_HIGHLIGHT_COLOR);
        final RGB flowHighlightColor
            = PreferenceConverter.getColor(store, PreferenceConstants.P_EDITOR_SELECTED_NODE_FLOW_CONNECTION_HIGHLIGHT_COLOR);

        ProgressPolylineConnection.setHighlightColor(highlightColor);
        ProgressPolylineConnection.setFlowVariableHighlightColor(flowHighlightColor);
        ProgressPolylineConnection.PREFERENCE_DISPLAY_HIGHLIGHTING = showHighlights;
        ProgressPolylineConnection.PREFERENCE_HIGHLIGHTED_WIDTH_DELTA = widthDelta;

        m_connectionHighlighter.selectionChanged(WorkflowEditor.this, getSite().getSelectionProvider().getSelection());
    }

    /**
     * Brings up the Save-Dialog and sets the m_isClosing flag.
     * {@inheritDoc}
     */
    @Override
    public int promptToSaveOnClose() {

        if (m_isMetadataEditorDirty) {
            final var views = Optional.of(PlatformUI.getWorkbench()) //
                    .map(IWorkbench::getActiveWorkbenchWindow) //
                    .map(IWorkbenchWindow::getActivePage) //
                    .map(IWorkbenchPage::getViewReferences) //
                    .map(Arrays::asList) //
                    .orElse(List.of());
            for (final var view : views) {
                if (view.getPart(false) instanceof DescriptionView descriptionView
                        && Wrapper.unwrapWFMOptional(m_manager) //
                                .filter(wfm -> descriptionView.isDirtyEditing(wfm)).isPresent()) { // NOSONAR
                    final var sh = SWTUtilities.getActiveShell();
                    MessageDialog.openInformation(sh, "Pending Metadata Changes",
                        "There are unsaved changes in the metadata view. "
                        + "Save or discard them before closing the workflow.");
                    m_isClosing = false;
                    return ISaveablePart2.CANCEL;
                }
            }
        }

        /*
         * Ideally we would just set the m_isClosing flag and return
         * ISaveablePart2.DEFAULT which will bring up a separate dialog. This
         * does not work as we have to set the m_isClosing only if the user
         * presses YES (no means to figure out what button was pressed when
         * eclipse opens the dialog).
         */
        if (m_parentEditor != null) {
            // ignore closing metanode editors.
            return ISaveablePart2.NO;
        }
        String message = NLS.bind(WorkbenchMessages.EditorManager_saveChangesQuestion, getTitle());
        // Show a dialog.
        Shell sh = SWTUtilities.getActiveShell();
        String[] buttons = new String[]{
            IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL };
        MessageDialog d = new MessageDialog(sh, WorkbenchMessages.Save_Resource, null, message,
            MessageDialog.QUESTION, buttons, 0);
        switch (d.open()) { // returns index in buttons[] array
        case 0: // YES
            m_isClosing = true;
            return ISaveablePart2.YES;
        case 1: // NO
            m_isClosing = false;
            return ISaveablePart2.NO;
        default: // CANCEL button or window 'x'
            m_isClosing = false;
            return ISaveablePart2.CANCEL;
        }
    }

    /**
     * Save workflow to resource.
     *
     * @param fileResource .. the resource, usually m_fileResource or m_autoSaveFileResource or soon-to-be
     *            m_fileResource (for save-as)
     * @param monitor ...
     * @param saveWithData ... save data also
     * @param newContext a new workflow context for the saved workflow; if this is non-<code>null</code>, a "save as" is
     *            performed
     */
    private void saveTo(final URI fileResource, final IProgressMonitor monitor, final boolean saveWithData,
        final WorkflowContextV2 newContext) {
        LOGGER.debug("Saving workflow " + getWorkflowManager().get().getNameWithID());

        // Exception messages from the inner thread
        final StringBuilder exceptionMessage = new StringBuilder();
        final Display d = PlatformUI.getWorkbench().getDisplay();

        if (fileResource == null && m_parentEditor != null) {
            m_parentEditor.doSave(monitor);
            m_isDirty = false;
            d.asyncExec(() -> {
                firePropertyChange(IEditorPart.PROP_DIRTY);
            });
            return;
        }

        // attach editor properties with the workflow manager - for all sub editors too
        saveEditorSettingsToWorkflowManager();
        /* TODO: EditorSettings should be saved on all child editors too. But this triggers them dirty. And they stay
         * dirty even after save (and setting them clean!). The dirty flag is a mess. Needs to be cleaned up!
         * And after that, we may not inherit the zoom level (in metanodes) from the parent anymore
         * (see #applyEditorSettingsFromWorkflowManager).
         * for (IEditorPart subEditor : getSubEditors()) {
         *   ((WorkflowEditor)subEditor).saveEditorSettingsToWorkflowManager();
         * }
         */

        // The genesis of this block is https://knime-com.atlassian.net/browse/AP-13689
        if (WorkflowEditorMode.ANNOTATION_EDIT.equals(getEditorMode())) {
            final ToggleEditorModeAction action = new ToggleEditorModeAction(this);

            d.syncExec(() -> {
                action.runInSWT();
            });

            // now wait on the OOB deselection to finish before allowing the
            //      canvas 'picture' capture to SVG
            final ISelectionProvider isp = getSite().getSelectionProvider();
            ISelection is = null;
            int waitCount = 0;
            while ((waitCount < 30) && ((is == null) || !is.isEmpty())) {
                try {
                    Thread.sleep(32);
                } catch (final Exception e) { }

                is = isp.getSelection();
                waitCount++;
            }
        } else {
            // The genesis of this block is the desire not to have an SVG saved with potentially highlighted
            //      connections which now may exist due to https://knime-com.atlassian.net/browse/AP-13833
            //  At first i was going to implement this as "if highlighting is enabled and there is only one connector
            //      possessor selected, then deselect everything," but then i though - we probably, ideally, do not
            //      want to generate the worfklow SVG with anything selected, really. So we have clear the
            //      selection regardless prior to a save now.
            // Update Martin: to preserve old behavior I decided that the de-selection is only done when highlighting
            //      is enabled
            if (ProgressPolylineConnection.PREFERENCE_DISPLAY_HIGHLIGHTING) {
                final ISelectionProvider isp = getSite().getSelectionProvider();
                if (isp != null && !isp.getSelection().isEmpty()) {
                    isp.setSelection(new StructuredSelection());
                }
            }
        }

        // to be sure to mark dirty and inform the user about running nodes
        // we ask for the state BEFORE saving
        // this flag is evaluated at the end of this method
        boolean wasInProgress = false;
        try {
            final File workflowDir = new File(fileResource);
            AbstractSaveRunnable saveRunnable;
            if (isComponentProjectWFM()) {
                //it's a workflow of an opened component
                //-> use different save routine

                final var oldContext = getWorkflowManager()
                        .map(WorkflowManager::getContextV2)
                        .orElseThrow(() -> new IllegalStateException("Project workflow manager needs to have a "
                            + "context: " + getWorkflowManager().get()));
                if (newContext != null) {
                    saveRunnable = new SaveProjectComponentRunnable(this, exceptionMessage, monitor, newContext);
                } else {
                    saveRunnable = new SaveProjectComponentRunnable(this, exceptionMessage, monitor, oldContext);
                }
            } else {
                if (newContext != null) {
                    saveRunnable = new SaveAsRunnable(this, exceptionMessage, monitor, newContext);
                } else {
                    WorkflowSaveHelper saveHelper = new WorkflowSaveHelper(saveWithData, false);
                    saveRunnable = new InplaceSaveRunnable(this, exceptionMessage, saveHelper, monitor, workflowDir);
                }
            }

            IWorkbench wb = PlatformUI.getWorkbench();
            IProgressService ps = wb.getProgressService();

            NodeContainerState state = m_manager.getNodeContainerState();
            wasInProgress = state.isExecutionInProgress() && !state.isExecutingRemotely();

            ps.run(true, false, saveRunnable);
            // this code is usually (always?) run in the UI thread but in case it's not we schedule in UI thread
            // (SVG export always in UI thread)
            final File svgFile = new File(workflowDir, WorkflowPersistor.SVG_WORKFLOW_FILE);
            svgFile.delete();
            if (m_manager.isProject()) {
                d.syncExec(() -> {
                    saveSVGImage(svgFile);
                });
            }
            // mark command stack (no undo beyond this point)
            getCommandStack().markSaveLocation();

        } catch (Exception e) {
            boolean isWfm = !isComponentProjectWFM();
            LOGGER.error("Could not save " + (isWfm ? "workflow" : "component") + ": " + exceptionMessage, e);

            // inform the user
            if (exceptionMessage.length() > 0) {
                showInfoMessage((isWfm ? "Workflow" : "Component") + " could not be saved ...",
                    exceptionMessage.toString());
            }

            throw new OperationCanceledException(
                (isWfm ? "Workflow" : "Component") + " was not saved: " + exceptionMessage.toString());
        }

        d.asyncExec(() -> {
            if (!d.isDisposed() && (m_manager != null)) {
                // mark all sub editors as saved
                for (final IEditorPart subEditor : getSubEditors()) {
                    final WorkflowEditor editor = (WorkflowEditor)subEditor;
                    editor.setIsDirty(false);
                    editor.firePropertyChange(IEditorPart.PROP_DIRTY);
                }
            }
        });

        monitor.done();

        // bugfix 799 (partly)
        // check if the workflow manager is in execution
        // this happens if the user pressed "Yes" on save confirmation dialog
        // or simply saves (Ctrl+S)
        if (wasInProgress && !isComponentProjectWFM()) {
            markDirty();
            final Pointer<Boolean> abortPointer = new Pointer<Boolean>();
            abortPointer.set(Boolean.FALSE);
            d.syncExec(() -> {
                boolean abort = false;
                final Shell sh = SWTUtilities.getActiveShell();
                final String title = "Workflow in execution";
                final String message = "Executing nodes are not saved!";
                if (m_isClosing) {
                    abort = !MessageDialog.openQuestion(sh, title, message + " Exit anyway?");
                    m_isClosing = !abort; // user canceled close
                } else {
                    final IPreferenceStore prefStore = KNIMEUIPlugin.getDefault().getPreferenceStore();
                    final String toogleMessage = "Don't warn me again";
                    if (prefStore.getBoolean(PreferenceConstants.P_CONFIRM_EXEC_NODES_NOT_SAVED)) {
                        MessageDialogWithToggle
                                .openInformation(
                                        sh,
                                        title,
                                        message,
                                        toogleMessage,
                                        false,
                                        prefStore,
                                        PreferenceConstants.P_CONFIRM_EXEC_NODES_NOT_SAVED);
                    }
                }
                abortPointer.set(Boolean.valueOf(abort));
            });
            if (abortPointer.get().booleanValue()) {
                throw new OperationCanceledException("Closing workflow canceled on user request.");
            }
        }
    }

    private void saveSVGImage(final File svgFile) {
        // If SVGExporter available try to export
        WorkflowSVGExport svgExporter = KNIMEEditorPlugin.getDefault().getSvgExport();
        if (svgExporter != null) {
            try {
                svgExporter.exportToSVG(this, svgFile);
            } catch (Exception e) {
                LOGGER.error("Could not save workflow SVG", e);
            }
        }
    }


    /** {@inheritDoc} */
    @Override
    public void doSave(final IProgressMonitor monitor) {
        if (isTempLocalWorkflowEditor()) {
            MessageDialog.openError(getSite().getShell(), "Workflow not writable",
                "You cannot save a workflow opened from an archive. Use \"Save As...\" in order to save it to "
                + "a different location.");
            monitor.setCanceled(true);
            return;
        }

        if (isTempRemoteWorkflowEditor()) {
            if (!saveBackToServer()) {
                monitor.setCanceled(true);
                return;
            }
            updateWorkflowMessages();
        } else {
            saveTo(m_fileResource, monitor, !isComponentProjectWFM(), null);
        }
        notifySaveEventListeners();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doSaveAs() {
        final var optWFM = getWorkflowManager();
        if(!optWFM.isPresent()) {
            LOGGER.warn("'Save As...'-action not possible for workflow " + getWorkflowManagerUI().getName());
            return;
        }
        final var wfm = optWFM.get();
        if (m_parentEditor != null) { // parent does it if this is a metanode editor
            m_parentEditor.doSaveAs();
            return;
        }
        URI fileResource = m_fileResource;
        Shell activeShell = SWTUtilities.getActiveShell();
        if (fileResource == null) {
            MessageDialog.openError(activeShell, "Workflow file resource",
                "Could not determine the save location to the workflow.");
            return;
        }
        File workflowDir = new File(fileResource);
        // Only continue if no  other editor to this workflow is open
        if (isOtherEditorToWorkflowOpen(workflowDir.toURI())) {
            MessageDialog.openError(activeShell, "\"Save As...\" not available",
                    "\"Save As...\" is not possible while another editor to this workflow is open.");
            return;
        }
        OverwriteAndMergeInfo newLocationInfo = getNewLocation(wfm, fileResource, isTempRemoteWorkflowEditor());
        if (newLocationInfo == null) {
            // user canceled
            return;
        }
        AbstractExplorerFileStore newWorkflowDir;
        try {
            newWorkflowDir = ExplorerFileSystem.INSTANCE.getStore(new URI(newLocationInfo.getNewName()));
        } catch (URISyntaxException e2) {
            LOGGER.error("Unable to create a URI from the selected destination: " + e2.getMessage()
                    + " Canceling the SaveAs.");
            MessageDialog.openError(getSite().getShell(), "Internal Error",
                "Unable to create a URI from the selected destination. \n" + e2.getMessage()
                + "\nCanceling the SaveAs.");
            return;
        }

        if (newLocationInfo.createSnapshot()) {
            try {
                ((RemoteExplorerFileStore)newWorkflowDir).createSnapshot(newLocationInfo.getComment());
            } catch (CoreException e) {
                String msg =
                    "Unable to create the desired snapshot before overwriting the workflow:\n" + e.getMessage()
                        + "\n\nCanceling the upload!";
                LOGGER.error(
                    "Unable to create the desired snapshot before overwriting the workflow: " + e.getMessage()
                        + " Upload canceled!", e);
                MessageDialog.openError(getSite().getShell(), "Server Error", msg);
                return;
            }
        }


        if (newWorkflowDir instanceof RemoteExplorerFileStore) {
            // selected a remote location: save + upload
            if (isDirty()) {
                saveTo(m_fileResource, new NullProgressMonitor(), true, null);
            }
            final var localFS = getFileStore(fileResource);
            if (localFS.filter(fs -> fs instanceof LocalExplorerFileStore).isEmpty()) {
                LOGGER.error("Unable to resolve current workflow location. Flow not uploaded!");
                return;
            }
            try {
                if (!m_workflowCanBeDeleted.tryAcquire()) {
                    throw new CoreException(new Status(IStatus.WARNING, ID,
                        "Cannot acquire lock since another save operation takes place."));
                }

                newWorkflowDir.getContentProvider().performUploadAsync((LocalExplorerFileStore)localFS.orElseThrow(),
                    (RemoteExplorerFileStore)newWorkflowDir, /*deleteSource=*/false,
                    newWorkflowDir.getContentProvider().isForceResetOnUpload(), t -> m_workflowCanBeDeleted.release());
            } catch (CoreException e) {
                String msg =
                    "\"Save As...\" failed to upload the workflow to the selected remote location\n(" + e.getMessage()
                        + ")";
                LOGGER.error(msg, e);
                MessageDialog.openError(activeShell, "\"Save As...\" failed.", msg);
            }
            // update the location and the link to the workflow as part of SRV-1326
            if (wfm.getContextV2().isTemporyWorkflowCopyMode()) {
                m_manuallySetToolTip = null;
                updateWorkflowMessages();
                updatePartName();
            }
        } else {

            // this is messy. Some methods want the URI with the folder, others the file store denoting workflow.knime
            AbstractExplorerFileStore newWorkflowFile = newWorkflowDir.getChild(WorkflowPersistor.WORKFLOW_FILE);
            final File localNewWorkflowDir;
            try {
                localNewWorkflowDir = newWorkflowDir.toLocalFile();
            } catch (CoreException e1) {
                LOGGER.error("Unable to resolve selection to local file path: " + e1.getMessage(), e1);
                return;
            }

            // retrieve mount point information for the new location
            File mountPointRoot = null;
            AbstractExplorerFileStore rootStore = null;
            try {
                rootStore = newWorkflowDir.getContentProvider().getRootStore();
                mountPointRoot = rootStore.toLocalFile();
            } catch (CoreException ex) {
                LOGGER.warn("Could not determine mount point root for " + newWorkflowDir + ": " + ex.getMessage(), ex);
            }

            final WorkflowContextV2 oldContext =
                    CheckUtils.checkNotNull(wfm.getContextV2(), "Workflow context not available.");
            final var oldExecutorInfo = oldContext.getExecutorInfo();
            CheckUtils.checkArgument(oldExecutorInfo instanceof AnalyticsPlatformExecutorInfo,
                "Coding error. Executor info is not of type Analytics Platform but of type: "
                    + oldExecutorInfo.getClass());
            final var mountPointId = rootStore.getMountID();
            final var mountPointRootPath = Optional.ofNullable(mountPointRoot).map(File::toPath);

            var locationInfoOptional = newWorkflowDir.locationInfo();
            if (locationInfoOptional.isEmpty()) {
                LOGGER.warn("Unable to determine location info for new workflow dir at " + workflowDir.getParent());
                return;
            }
            final LocationInfo locInfo = locationInfoOptional.get();
            final WorkflowContextV2 newContext =
                WorkflowContextV2.builder().withAnalyticsPlatformExecutor(stdBuilder -> {
                    var builder = stdBuilder//
                        .withUserId(oldExecutorInfo.getUserId())//
                        .withLocalWorkflowPath(localNewWorkflowDir.toPath())//
                        .withBatchMode(oldExecutorInfo.isHeadless())//
                        .withTempFolder(oldExecutorInfo.getTempFolder());
                    mountPointRootPath.ifPresent(rootPath -> builder.withMountpoint(mountPointId, rootPath));
                    return builder;
                }).withLocation(locInfo).build();

            saveTo(localNewWorkflowDir.toURI(), new NullProgressMonitor(), true, newContext);
            setInput(new FileStoreEditorInput(newWorkflowFile));
            if (newWorkflowDir.getParent() != null) {
                newWorkflowDir.getParent().refresh();
            }
        }

        notifySaveEventListeners();
    }

    private class AutoSaveJob extends Job {

        private final boolean m_isSavingWithData;
        private final int m_intervalInSecs;

        AutoSaveJob(final boolean isSavingWithData, final int intervalInSecs) {
            super("Auto-Save " + getWorkflowManager().orElseThrow().getName());
            m_isSavingWithData = isSavingWithData;
            m_intervalInSecs = intervalInSecs;
        }

        boolean isSavingWithData() {
            return m_isSavingWithData;
        }

        void scheduleNextAutoSave() {
            schedule(TimeUnit.SECONDS.toMillis(m_intervalInSecs));
        }

        @Override
        protected IStatus run(final IProgressMonitor jobMonitor) {
            assert m_parentEditor == null : "No auto save on metanode";
            long start = System.currentTimeMillis();
            IStatus status = doIt(jobMonitor);
            IStatus resultStatus = status;
            if (status == null) {
                resultStatus = Status.OK_STATUS;
            } else if (status.isOK()) {
                String delay = StringFormat.formatElapsedTime(System.currentTimeMillis() - start);
                LOGGER.debugWithFormat("Auto-saved workflow %s (took %s)", m_manager.getName(), delay);
            } else {
                LOGGER.warnWithFormat("Auto-saving workflow %s caused issues: ", m_manager.getName(), status);
            }
            scheduleNextAutoSave();
            return resultStatus;
        }

        /** Performs the save, returns null if no save needed (=not dirty). */
        private IStatus doIt(final IProgressMonitor jobMonitor) {
            NodeContext.pushContext(Wrapper.unwrapWFM(m_manager));
            try {
                final var wfm = getWorkflowManager().orElseThrow();
                ReferencedFile autoSaveDir = wfm.getAutoSaveDirectory();
                if (autoSaveDir == null) { // net yet auto-saved
                    if (!isDirty()) {      // main editor not dirty
                        return null;
                    }
                    final ReferencedFile ncDirRef = wfm.getNodeContainerDirectory();
                    autoSaveDir = new ReferencedFile(WorkflowSaveHelper.getAutoSaveDirectory(ncDirRef));
                    autoSaveDir.setDirty(true);
                    wfm.setAutoSaveDirectory(autoSaveDir);
                }
                if (!autoSaveDir.isDirty()) {
                    return null;
                }
                final var autoSaveURI = autoSaveDir.getFile().toURI();

                saveEditorSettingsToWorkflowManager();
                final var workflowDir = new File(autoSaveURI);

                // Exception messages from the inner thread
                final var messageBuilder = new StringBuilder();
                final var saveHelper = new WorkflowSaveHelper(m_isSavingWithData, true);
                final var saveRunnable = new AutosaveRunnable(WorkflowEditor.this, messageBuilder, saveHelper,
                    new NullProgressMonitor(), workflowDir);
                saveRunnable.run(jobMonitor);

                // Hides the auto save directory on DOS based file systems
                if (Platform.OS_WIN32.equals(Platform.getOS())) {
                    try {
                        Files.setAttribute(workflowDir.toPath(), "dos:hidden", true);
                    } catch (IOException e) {
                        LOGGER.warn(e);
                    }
                }
                jobMonitor.done();
                return Status.OK_STATUS;
            } catch (Exception e) {
                final String error = "Failed auto-saving " + (m_manager == null ? "<null>" : m_manager.getNameWithID());
                LOGGER.error(error, e);
                return new Status(IStatus.ERROR, KNIMEEditorPlugin.PLUGIN_ID, error, e);
            } finally {
                NodeContext.removeLastContext();
            }
        }
    }

    /**
     * For SaveAs...
     * @param wfm workflow manager
     * @param currentLocation
     * @param allowRemoteLocation local and remote mount points are added to the selection dialog
     * @return new (different!) URI or null if user canceled. Caller should create a snapshot if told so.
     */
    private OverwriteAndMergeInfo getNewLocation(final WorkflowManager wfm, final URI currentLocation,
            final boolean allowRemoteLocation) {

        final var currentStore = getFileStore(currentLocation);
        AbstractExplorerFileStore currentParent = null;
        if (currentStore.isPresent()) {
            currentParent = currentStore.get().getParent();
        }

        final var workflowPath = wfm.getContextV2().getTempSourceLocation().orElse(currentLocation).getPath();
        var currentName = new Path(workflowPath).lastSegment();
        List<String> selIDs = new LinkedList<>();
        for (String id : ExplorerMountTable.getAllVisibleMountIDs()) {
            AbstractContentProvider provider = ExplorerMountTable.getMountPoint(id).getProvider();
            if (!provider.isRemote() || (allowRemoteLocation && provider.isWritable())) {
                selIDs.add(id);
            }
        }
        ContentObject preSel = ContentObject.forFile(currentParent);
        if (isTempRemoteWorkflowEditor()) {
            AbstractExplorerFileStore remoteStore = null;
            try{
                remoteStore = ExplorerFileSystem.INSTANCE.getStore(wfm.getContextV2().getMountpointURI().orElseThrow());
            } catch (IllegalArgumentException e) { /* don't preselect on unknown original location */ }

            if (remoteStore != null) {
                preSel = ContentObject.forFile(remoteStore);
            } else {
                preSel = null;
            }
        }
        OverwriteAndMergeInfo result = null;
        while (result == null) { // keep the selection dialog open until we get a useful result
            final SpaceResourceSelectionDialog dialog =
                new SpaceResourceSelectionDialog(getSite().getShell(), selIDs.toArray(new String[selIDs.size()]),
                    preSel);
            final var validator = new SaveAsValidator(dialog, currentStore.orElse(null));
            String defName = currentName + " - Copy";
            if (!isTempRemoteWorkflowEditor() && !isTempLocalWorkflowEditor()) {
                if (currentParent != null) {
                    try {
                        Set<String> childs =
                            new HashSet<String>(Arrays.asList(currentParent.childNames(EFS.NONE, null)));
                        defName = guessNewWorkflowNameOnSaveAs(childs, currentName);
                    } catch (CoreException e1) {
                        // keep the simple default
                    }
                }
            } else {
                defName = currentName;
                if (defName.endsWith("." + KNIMEConstants.KNIME_WORKFLOW_FILE_EXTENSION)) {
                    defName = defName.substring(0, defName.length() - KNIMEConstants.KNIME_WORKFLOW_FILE_EXTENSION.length() - 1);
                }
            }
            dialog.setTitle("Save to new Location");
            dialog.setDescription("Select the new destination workflow group for the workflow.");
            dialog.setValidator(validator);
            // Setup the name field of the dialog
            dialog.setNameFieldEnabled(true);
            dialog.setNameFieldDefaultValue(defName);
            final var proceed = new AtomicBoolean(false);
            Display.getDefault().syncExec(() -> proceed.set(dialog.open() == Window.OK));
            if (!proceed.get()) {
                return null;
            }
            AbstractExplorerFileStore newLocation = dialog.getSelection();
            if (newLocation.fetchInfo().isWorkflowGroup()) {
                newLocation = newLocation.getChild(dialog.getNameFieldValue());
            } else {
                // in case they have selected a flow but changed the name in the name field afterwards
                newLocation = newLocation.getParent().getChild(dialog.getNameFieldValue());
            }
            assert !newLocation.fetchInfo().exists() || newLocation.fetchInfo().isWorkflow();
            if (newLocation.fetchInfo().exists()) {
                // confirm overwrite (with snapshot?)
                final AtomicBoolean snapshotSupported = new AtomicBoolean(false);
                final AtomicReference<SnapshotPanel> snapshotPanel = new AtomicReference<SnapshotPanel>(null);
                if (newLocation.getContentProvider().supportsSnapshots()
                    && (newLocation instanceof RemoteExplorerFileStore)) {
                    snapshotSupported.set(true);
                }
                final AbstractExplorerFileStore target = newLocation;
                MessageDialog dlg =
                    new MessageDialog(getSite().getShell(), "Confirm SaveAs Overwrite", null,
                        "The selected destination\n\n\t" + newLocation.getMountIDWithFullPath()
                            + "\n\nalready exists. Do you want to overwrite?\n",
                        MessageDialog.QUESTION, new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL,
                            IDialogConstants.CANCEL_LABEL}, 1) {
                        /**
                         * {@inheritDoc}
                         */
                        @Override
                        protected Control createCustomArea(final Composite parent) {
                            if (snapshotSupported.get()) {
                                snapshotPanel.set(new SnapshotPanel(parent, SWT.NONE,
                                    target.getContentProvider().isForceSnapshotCreation()));
                                snapshotPanel.get().setEnabled(true);
                                return snapshotPanel.get();
                            } else {
                                return null;
                            }
                        }
                    };
                int dlgResult = dlg.open();
                if (dlgResult == 2 /* CANCEL */) {
                    return null;
                }
                if (dlgResult == 0) { /* YES (= please overwrite) */
                    if (snapshotPanel.get() != null) {
                        SnapshotPanel snapPanel = snapshotPanel.get();
                        result = new OverwriteAndMergeInfo(newLocation.toURI().toASCIIString(), false, true,
                            snapPanel.createSnapshot(), snapPanel.getComment(), false);
                    } else {
                        result = new OverwriteAndMergeInfo(newLocation.toURI().toASCIIString(), false, true, false, "",
                            false);
                    }

                } else {
                    /* NO, don't overwrite: continue while loop asking for a different location */
                    preSel = ContentObject.forFile(newLocation);
                    currentName = newLocation.getName();
                }
            } else {
                result = new OverwriteAndMergeInfo(newLocation.toURI().toASCIIString(), false, false, false, "", false);
            }
        } /* end of while (result != null) keep the target selection dialog open */
        return result;
    }

    /**
     * Checks if a non workflow editor (e.g. meta info editor) to the workflow with the given URI is open.
     *
     * @param workflowURI The URI to the workflow directory. Must be file protocol
     * @return true if an editor is open, false otherwise
     */
    private static boolean isOtherEditorToWorkflowOpen(final URI workflowURI) {
        boolean isOpen = false;
        File wfDir = new File(workflowURI);

        // Go through all open editors
        for (IEditorReference editorRef : PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
            .getEditorReferences()) {
            IEditorPart editorPart = editorRef.getEditor(false); // avoid restoring editor (for large workflows)
            if (editorPart == null) {
                // Editor is not active/loaded. But tooltip usually points to report template file or meta file
                final String titleToolTip = editorRef.getTitleToolTip();
                if (titleToolTip == null || wfDir.equals(new File(titleToolTip).getParentFile())) {
                    // the tooltip indicates that it is an editor for the same workflow (report/metadata/etc.)
                    editorPart = editorRef.getEditor(true); // activate/load the editor
                }
            }
            // Check if editor is something other than a workflow editor
            if ((editorPart != null) && !(editorPart instanceof WorkflowEditor)) {
                IEditorInput editorInput = editorPart.getEditorInput();
                if (editorInput instanceof FileStoreEditorInput) {
                    FileStoreEditorInput fileStoreInput = (FileStoreEditorInput)editorInput;
                    // Get URI of the editor
                    URI uri = fileStoreInput.getURI();
                    // Check if parent of the editor is the workflow directory
                    uri = new File(uri).getParentFile().toURI();
                    if (workflowURI.equals(uri)) {
                        isOpen = true;
                        break;
                    }
                }
            }
        }
        return isOpen;
    }

    /** Derives new name, e.g. KNIME_project_2 -> KNIME_project_3.
     * Separator between name and number can be empty, space, dash, underscore
     * @param invalidFileNames Names of files/folders that already exist
     * @param workflowDirName The name of the old workflow
     * @return The new suggested name (shown in rename prompt). */
    private static String guessNewWorkflowNameOnSaveAs(
            final Set<String> invalidFileNames, final String workflowDirName) {
        Pattern pattern = Pattern.compile("(.*\\D)?(\\d+)");
        Matcher matcher = pattern.matcher(workflowDirName);
        String baseName = workflowDirName;
        int index;
        if (matcher.matches()) {
            try {
                index = Integer.parseInt(matcher.group(2));
            } catch (Exception e) {
                index = 0;
            }
            baseName = matcher.group(1);
            if (baseName == null) {
                baseName = "";
            }
        } else {
            index = 0;
            baseName = workflowDirName + " ";
        }
        String workflowDirNewName = workflowDirName;
        while (invalidFileNames.contains(workflowDirNewName)) {
            workflowDirNewName = baseName + (++index);
        }
        return workflowDirNewName;
    }

    /**
     * Stores the current editor settings with the workflow manager. In its NodeUIInfo object. Overrides any previously
     * stored editor settings.
     */
    private void saveEditorSettingsToWorkflowManager() {
        // overwriting any existing editor settings in the ui info
        getWorkflowManagerUI().setEditorUIInformation(getCurrentEditorSettings());
    }

    /**
     * @return the current values of the settings (grid and zoomlevel) of this editor (but not the ones stored with the
     *         workflow manager!)
     */
    public EditorUIInformation getCurrentEditorSettings() {
        return EditorUIInformation.builder()
            .setZoomLevel(getZoomfactor())
            .setSnapToGrid(getEditorSnapToGrid())
            .setShowGrid(getEditorIsGridVisible())
            .setGridX(getEditorGridX())
            .setGridY(getEditorGridY())
            .setHasCurvedConnections(getHasCurvedConnections())
            .setConnectionLineWidth(getConnectionLineWidth())
            .build();
    }

    /**
     * Applies the settings to the editor. Can't be null. Settings are not stored in the workflow manager.
     * @see #getCurrentEditorSettings()
     * @see #getEditorSettingsDefault()
     * @param settings to apply
     */
    public void applyEditorSettings(final EditorUIInformation settings) {
        getViewer().setProperty(SnapToGrid.PROPERTY_GRID_ENABLED, Boolean.valueOf(settings.getSnapToGrid()));
        getViewer().setProperty(SnapToGrid.PROPERTY_GRID_VISIBLE, Boolean.valueOf(settings.getShowGrid()));
        getViewer().setProperty(SnapToGrid.PROPERTY_GRID_SPACING,
                new Dimension(settings.getGridX(), settings.getGridY()));
        setZoomfactor(settings.getZoomLevel());
        m_hasCurvedConnections = settings.getHasCurvedConnections();
        m_connectionLineWidth = settings.getConnectionLineWidth();
    }

    private void applyEditorSettingsFromWorkflowManager() {
        final WorkflowManagerUI wfm = getWorkflowManagerUI();
        EditorUIInformation settings = wfm.getEditorUIInformation();
        if (settings == null || settings.getGridX() == -1) {
            // if this is a metanode - derive settings from parent
            if (m_fileResource == null && m_parentEditor != null) {
                settings = m_parentEditor.getCurrentEditorSettings();
            } else {
                // this is an old workflow: don't show or enable grid
                settings = getEditorSettingsDefaultBuilder().setShowGrid(false).setSnapToGrid(false).build();
            }
        }
        applyEditorSettings(settings);
    }

    /**
     * @return an object with the default value (mostly from the preference page) for the editor settings
     */
    public EditorUIInformation.Builder getEditorSettingsDefaultBuilder() {
        return EditorUIInformation.builder()
                .setSnapToGrid(getPrefSnapToGrid())
                .setShowGrid(getPrefIsGridVisible())
                .setGridX(getPrefGridXSize())
                .setGridY(getPrefGridYSize())
                .setZoomLevel(1.0)
                .setHasCurvedConnections(getPrefHasCurvedConnections())
                .setConnectionLineWidth(getPrefConnectionLineWidth());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirty() {
        // if we are a subworkflow editor we are never dirty
        if (m_parentEditor != null) {
            return m_parentEditor.isDirty();
        }
        return m_isDirty || m_isMetadataEditorDirty;
    }

    /**
     * Updates the metadata editor dirty status.
     *
     * @param dirty flag
     */
    public void setMetadataEditorDirty(final boolean dirty) {
        if (m_parentEditor != null) {
            // delegate to parent
            m_parentEditor.setMetadataEditorDirty(dirty);
            return;
        }

        if (m_isMetadataEditorDirty == dirty) {
            // nothing to do
            return;
        }

        boolean oldDirty = isDirty();
        m_isMetadataEditorDirty = dirty;
        if (isDirty() ^ oldDirty) {
            // state has changed
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSaveAsAllowed() {
        return true; // disallow save-as on subworkflow editors
    }

    /**
     * @return true if this is an editor of a remote workflow that was downloaded in a temp location
     * @since 2.10
     */
    public boolean isTempRemoteWorkflowEditor() {
        if (m_fileResource == null && m_parentEditor != null) { // metanode editor
            return m_parentEditor.isTempRemoteWorkflowEditor();
        }
        return Wrapper.unwrapWFMOptional(m_manager)
                .map(WorkflowManager::getContextV2)
                .map(WorkflowContextV2::getLocationInfo)
                .filter(RestLocationInfo.class::isInstance)
                .isPresent();
    }

    /**
     * @return true if this is an editor for a workflow opened from a local archive file (e.g a ".knwf")
     */
    public boolean isTempLocalWorkflowEditor() {
        if (m_fileResource == null && m_parentEditor != null) { // metanode editor
            return m_parentEditor.isTempLocalWorkflowEditor();
        }
        return Wrapper.unwrapWFMOptional(m_manager)
                .map(WorkflowManager::getContextV2)
                .map(WorkflowContextV2::getLocationInfo)
                .flatMap(loc -> ClassUtils.castOptional(LocalLocationInfo.class, loc))
                .flatMap(LocalLocationInfo::getSourceArchive)
                .isPresent();
    }

    /**
     * Places the message at the top of the editor - above all other contents.
     */
    private void updateWorkflowMessages() {
        if (getViewer() == null) {
            //do nothing if it hasn't been loaded entirely, yet
            return;
        }

        final ViewportPinningGraphicalViewer viewer = (ViewportPinningGraphicalViewer)getGraphicalViewer();
        final StringBuilder sb = new StringBuilder();
        if (isTempRemoteWorkflowEditor() || isTempLocalWorkflowEditor()) {
            WorkflowEditor editor = this;
            while (editor.m_fileResource == null && editor.m_parentEditor != null) {
                editor = editor.m_parentEditor;
            }
            final var origRemoteLocation = Wrapper.unwrapWFM(editor.m_manager)
                    .getContextV2()
                    .getTempSourceLocation()
                    .orElseThrow();
            final var uriString = URIUtil.toDecodedString(origRemoteLocation);
            sb.append("  This is a temporary copy of \"" + uriString + "\".");
            if (!(uriString.startsWith("knime://EXAMPLE") || uriString.startsWith("file:/"))) {
                //"Save"-action only allowed for server-workflows, but not temporary workflows from the example-server nor an external file
                if (isDirty()) {
                    sb.append("\n  Use \"Save\" to upload it back to its original location "
                        + " or \"Save As...\" to store it in a different location.");
                } else {
                    sb.append(
                        "\n  Use \"Save As...\" to store it in your local workspace or a hub you are currently connected to.");
                }
            } else {
                sb.append("\n  Use \"Save As...\" to save a permanent copy of the "
                    + (isComponentProjectWFM() ? "component" : "workflow")
                    + " to your local workspace, or a mounted KNIME Hub.");
            }
            viewer.removeMessage(m_lastDisplayedWarningMessageId);
            m_lastDisplayedWarningMessageId = viewer.displayMessage(sb.toString(), MessageAppearance.WARNING,
                new String[] { "Save as..." }, new Runnable[] { this::doSaveAs });
        } else if (getWorkflowManagerUI() instanceof AsyncWorkflowManagerUI) {
            // if the underlying workflow manager is a AsyncWorkflowManagerUI instance
            final var serverType = isHubJob() ? "KNIME Hub" : "KNIME Server";

            assert m_refresher != null;
            if ((m_fileResource != null) && (m_parentEditor == null)) {
                //root workflow
                sb.append("This is a job running on ").append(serverType + " (" + m_fileResource.getAuthority() + ").");
            } else {
                //metanode editor
                sb.append("This is a metanode of a job running on " + serverType + ".");
            }
            if (!m_refresher.isAutoRefreshEnabled()) {
                sb.append("\nIt just represents a static snapshot of the job and won't get updated automatically. Use "
                    + "context menu to refresh or the preferences to activate the auto-refresh and edit operations.");
            } else {
                if (m_refresher.isWorkflowEditDisabled()) {
                    sb.append("\nWorkflow locked for edits. Enable edit operations in the preferences.");
                }
            }
            viewer.removeMessage(m_lastDisplayedInfoMessageId);
            m_lastDisplayedInfoMessageId =
                viewer.displayMessage(sb.toString(), MessageAppearance.INFO);

            if (!m_refresher.isConnected() && !m_refresher.isWorkflowEditDisabled()) {
                Optional<String> disconnectedMessage = m_refresher.getDisconnectedMessage();
                sb.setLength(0);
                sb.append("Remote Workflow Editor disconnected: ");
                sb.append(disconnectedMessage.isPresent() ? disconnectedMessage.get() : "an unknown reason");
                sb.append("\nWorkflow will not refresh and no changes can be made.");
                viewer.removeMessage(m_lastDisplayedErrorMessageId);
                m_lastDisplayedErrorMessageId =
                    viewer.displayMessage(sb.toString(), MessageAppearance.ERROR);
            } else {
                viewer.removeMessage(m_lastDisplayedErrorMessageId);
                m_lastDisplayedErrorMessageId = null;
            }

            if (getWorkflowManagerUI().isInWizardExecution()) {
                viewer.removeMessage(m_lastDisplayedWarningMessageId);
                final var dataAppType = isHubJob() ? "Data App" : "WebPortal";
                m_lastDisplayedWarningMessageId = viewer.displayMessage(String.format(
                    "Job started by %s. Edit operations are not allowed. "
                        + "Nodes following the currently active component ($s page) are not executed.",
                    dataAppType, dataAppType), MessageAppearance.WARNING);
            } else if (getWorkflowManagerUI().isWriteProtected() && isHubJob()) {
                viewer.removeMessage(m_lastDisplayedWarningMessageId);
                m_lastDisplayedWarningMessageId =
                    viewer.displayMessage("No permissions to edit the job.", MessageAppearance.WARNING);
            }
        } else {
            viewer.removeMessage(m_lastDisplayedInfoMessageId);
            viewer.removeMessage(m_lastDisplayedWarningMessageId);
            viewer.removeMessage(m_lastDisplayedErrorMessageId);

            m_lastDisplayedInfoMessageId = null;
            m_lastDisplayedWarningMessageId = null;
            m_lastDisplayedErrorMessageId = null;
        }

        for (final IEditorPart ep : getSubEditors()) {
            if (ep instanceof WorkflowEditor) {
                ((WorkflowEditor)ep).updateWorkflowMessages();
            }
        }

    }

    /**
     * Shows a simple information message.
     *
     * @param message the info message to display
     */
    private void showInfoMessage(final String header, final String message) {
        MessageBox mb = new MessageBox(this.getSite().getShell(), SWT.ICON_INFORMATION | SWT.OK);
        mb.setText(header);
        mb.setMessage(message);
        mb.open();
    }

    /**
     * Notifies property listeners on the editor about changes (e.g. dirty state
     * has changed). Updates the available actions afterwards
     *
     * @see org.eclipse.ui.part.WorkbenchPart#firePropertyChange(int)
     * @param property The property that has changed
     */
    @Override
    protected void firePropertyChange(final int property) {

        super.firePropertyChange(property);
        if (!m_isClosing) { // see AP-17103
            // updates the editor actions
            updateActions(m_editorActions);
        }
    }

    /**
     * Called when the editors selection has changed. Updates the list of
     * available actions for the new selection in the editor.
     *
     * @see org.eclipse.ui.ISelectionListener#selectionChanged
     *      (org.eclipse.ui.IWorkbenchPart,
     *      org.eclipse.jface.viewers.ISelection)
     * @param part The selected parts
     * @param selection The selection that has changed
     */
    @Override
    public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
        if (this == part) {
            // update available actions
            // but only if selection change happens within this workflow editor
            updateActions();
        }
    }

    /**
     * Called when the command stack has changed, that is, a GEF command was
     * executed (Add,Remove,....). This keeps track of the dirty state of the
     * editor. {@inheritDoc}
     */
    @Override
    public void commandStackChanged(final EventObject event) {

        // update the actions (should enable undo/redo accordingly)
        updateActions(m_editorActions);

        // track the dirty state of the edit domain
        boolean b = m_editDomain.getCommandStack().isDirty();
        if (b || getWorkflowManagerUI().isDirty()) {
            markDirty();
        } else {
            unmarkDirty();
        }
    }

    /*
     * --------- methods for adding a auto-placed and auto-connected node -----
     */

    /**
     * {@inheritDoc}
     * Listener interface method of the {@link NodeProvider}.
     * Called when other instances want to add a metanode to the workflow in
     * the editor. <br>
     * The implementation only adds it if the editor is active. If one other
     * node is selected in the editor, to which the new node will then be
     * connected to.
     *
     * @param sourceManager wfm to copy the metanode from
     * @param id the id of the metanode in the source manager
     * @return if the metanode was actually added
     */
    @Override
    public boolean addMetaNode(final WorkflowManager sourceManager,
            final NodeID id) {
        if (id == null || sourceManager == null) {
            return false;
        }
        if (!isEditorActive()) {
            return false;
        }

        NodeContainerEditPart preNode = getTheOneSelectedNode();
        NodeID preID = null;
        Point nodeLoc = null;
        if (preNode == null) {
            nodeLoc = getViewportCenterLocation();
        } else {
            nodeLoc = getLocationRightOf(preNode);
            preID = preNode.getNodeContainer().getID();
        }
        if (getEditorSnapToGrid()) {
            nodeLoc = getClosestGridLocation(nodeLoc);
        }
        Command newNodeCmd =
            new CreateNewConnectedMetaNodeCommand(getViewer(), m_manager, WorkflowManagerWrapper.wrap(sourceManager),
                id, nodeLoc, preID);
        getCommandStack().execute(newNodeCmd);
        // after adding a node the editor should get the focus
        setFocus();
        return true;
    }

    /**
     * Listener interface method of the {@link NodeProvider}. Called when other instances want to add a node to the
     * workflow in the editor. <br>
     * The implementation only adds it if the editor is active; if one node is selected in the editor, the new node will
     * then be connected to it.
     * {@inheritDoc}
     */
    @Override
    public boolean addNode(final NodeFactory<? extends NodeModel> nodeFactory) {
        if (!isEditorActive()) {
            return false;
        }

        final NodeContainerEditPart preNode = getTheOneSelectedNode();
        final Point nodeLoc;
        final Command newNodeCmd;
        if (preNode == null) {
            nodeLoc = getViewportCenterLocation();
            // this command accepts/requires relative coordinates
            newNodeCmd = new CreateNodeCommand(m_manager, nodeFactory, nodeLoc, getEditorSnapToGrid());
        } else {
            nodeLoc = getLocationRightOf(preNode);
            newNodeCmd = new CreateNewConnectedNodeCommand(getViewer(), m_manager, nodeFactory, nodeLoc,
                preNode.getNodeContainer().getID());
        }

        getCommandStack().execute(newNodeCmd);

        // after adding a node the editor should get the focus
        // this is issued asynchronously, in order to avoid bug #3029
        Display.getDefault().asyncExec(() -> {
            if (WorkflowEditorMode.ANNOTATION_EDIT.equals(m_editorMode)) {
                final ToggleEditorModeAction action = new ToggleEditorModeAction(this, false);
                action.runInSWT();
            }

            setFocus();
        });

        return true;
    }

    /**
     * Empty out the current selection and then select the specified nodes.
     *
     * @param containers 1 or more {@code NodeContainer} instances which should have {@link NodeContainerEditPart}
     *            corresponding instances in this editor
     */
    public void setNodeSelection(final NodeContainer ... containers) {
        final ArrayList<NodeContainerEditPart> selection = new ArrayList<>();
        final GraphicalViewer viewer = getGraphicalViewer();

        final HashSet<NodeContainer> candidates = new HashSet<>(Arrays.asList(containers));
        final EditPart rootChild = viewer.getRootEditPart().getChildren().get(0);
        for (final Object child : rootChild.getChildren()) {
            if (child instanceof NodeContainerEditPart) {
                final NodeContainerEditPart ncep = (NodeContainerEditPart)child;
                final NodeContainerUI ncUI = ncep.getNodeContainer();

                if (candidates.contains(Wrapper.unwrapNC(ncUI))) {
                    selection.add(ncep);
                }
            }
        }

        PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
            final ISelectionProvider isp = getSite().getSelectionProvider();

            isp.setSelection(new StructuredSelection(selection));
            if (selection.size() == 1) {
                final NodeContainerEditPart ncep = selection.get(0);
                final NodeUIInformation uiInfo = ncep.getNodeContainer().getUIInformation();
                final int[] bounds = uiInfo.getBounds();
                final Rectangle r = new Rectangle(bounds[0], bounds[1], bounds[2], bounds[3]);

                ((ViewportPinningGraphicalViewer)getGraphicalViewer()).ensureBoundsAreInView(r);
            }
        });
    }

    /**
     * Store the last find-node search results.
     *
     * @param results can be null
     */
    public void setSearchResults(final FindResults results) {
        if (m_lastSearchResults != null) {
            m_lastSearchResults.dispose();
        }
        m_lastSearchResults = results;
        if (m_lastSearchResults != null) {
            m_lastSearchResults.setWorkflowEditor(this);
        }
    }

    /**
     * @return true if there are search results
     */
    public boolean hasSearchResults() {
        return (m_lastSearchResults != null);
    }

    /**
     * Grabs the 'next' search result and sets our selection to that node.
     */
    public void selectNextSearchResult() {
        if (m_lastSearchResults != null) {
            setNodeSelection(m_lastSearchResults.getNextResult());
        }
    }

    private boolean isEditorActive() {
        // find out if we are the active editor (any easier way than that???)
        IWorkbenchWindow window =
                PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) {
            return false;
        }
        IWorkbenchPage page = window.getActivePage();
        if (page == null) {
            return false;
        }
        IEditorPart editor = page.getActiveEditor();
        return editor == this;
    }

    /**
     * @return a node if and only if exactly one node in the editor is selected
     */
    private NodeContainerEditPart getTheOneSelectedNode() {
        IStructuredSelection edSel =
                (IStructuredSelection)getSite().getSelectionProvider()
                        .getSelection();
        if (edSel.size() == 1) {
            Object o = edSel.getFirstElement();
            if (o instanceof NodeContainerEditPart) {
                // the one and only node that is selected
                return (NodeContainerEditPart)o;
            }
        }
        return null;
    }

    FigureCanvas getFigureCanvas() {
        final GraphicalViewer v = getViewer();

        if (v != null) {
            return (FigureCanvas)v.getControl();
        }

        return null;
    }

    private WorkflowFigure getWorkflowFigure() {
        return ((WorkflowRootEditPart)getViewer().getRootEditPart().getContents()).getFigure();
    }

    /**
     * @return a location in the middle of the visible part of the editor. These
     *         are absolute coordinates.
     */
    private Point getViewportCenterLocation() {
        final FigureCanvas ctrl = getFigureCanvas();
        final Viewport viewPort = ctrl.getViewport();
        final Dimension viewSize = viewPort.getSize();
        final int relX = viewSize.width / 2;
        final int relY = viewSize.height / 2;
        final Point nodeLoc = new Point(relX, relY);
        // make sure we have a free spot
        final int stepX = getEditorSnapToGrid() ? getEditorGridXOffset(10) : 10;
        final int stepY = getEditorSnapToGrid() ? getEditorGridYOffset(10) : 10;

        while (isNodeAtRel(nodeLoc)) {
            // move it a bit
            nodeLoc.x += stepX;
            nodeLoc.y += stepY;
        }

        return toAbsolute(nodeLoc);
    }

    private Point getLocationRightOf(final NodeContainerEditPart refNode) {
        NodeUIInformation ui =
                refNode.getNodeContainer()
                .getUIInformation();
        int xOffset = 100;
        int yOffset = 120;
        // adjust offset to grid location
        if (getEditorSnapToGrid()) {
            // with grid enabled we use the grid size as offset (but at least a bit mire than the node width)
            xOffset = getEditorGridXOffset((int)(refNode.getFigure().getBounds().width * 1.1));
            yOffset = getEditorGridYOffset((int)(refNode.getFigure().getBounds().height * 1.1));
        }

        // first try: right of reference node
        Point loc = new Point(ui.getBounds()[0] + xOffset, ui.getBounds()[1]);
        // make sure we have a free spot
        while (isNodeAtAbs(loc)) {
            // move it down a bit
            loc.y += yOffset;
        }
        return loc;
    }

    private boolean isNodeAtAbs(final Point absoluteLoc) {
        return isNodeAtRel(toRelative(absoluteLoc));

    }

    private Point toRelative(final Point absLoc) {
        ScalableFreeformRootEditPart rootEditPart
            = (ScalableFreeformRootEditPart) getViewer().getRootEditPart();
        Viewport viewport = (Viewport) rootEditPart.getFigure();
        Rectangle area = viewport.getClientArea();
        Point loc = absLoc.getCopy();
        double z = getZoomfactor();
        loc.x = (int)Math.round((loc.x - area.x) * z);
        loc.y = (int)Math.round((loc.y - area.y) * z);
        return loc;
    }

    private Point toAbsolute(final Point relLoc) {
        ScalableFreeformRootEditPart rootEditPart
        = (ScalableFreeformRootEditPart) getViewer().getRootEditPart();
        Viewport viewport = (Viewport) rootEditPart.getFigure();
        Rectangle area = viewport.getClientArea();

        Point loc = relLoc.getCopy();
        loc.x += area.x;
        loc.y += area.y;
        return loc;
    }

    private ZoomManager getZoomManager() {
        return (ZoomManager)(getViewer().getProperty(ZoomManager.class.toString()));
    }

    private double getZoomfactor() {
        final GraphicalViewer viewer = getViewer();
        if (viewer == null) {
            return 1.0;
        }
        final ZoomManager zoomManager = this.getZoomManager();
        if (zoomManager == null) {
            return 1.0;
        }
        return zoomManager.getZoom();
    }

    private void setZoomfactor(final double z) {
        final ZoomManager zoomManager = this.getZoomManager();
        zoomManager.setZoom(z);
    }

    private boolean isNodeAtRel(final Point relativeLoc) {
        EditPart ep = getViewer().findObjectAt(relativeLoc);
        if (ep == null) {
            return false;
        }
        while (!(ep instanceof RootEditPart)) {
            if (ep instanceof NodeContainerEditPart) {
                return true;
            }
            EditPart parent = ep.getParent();
            // avoid endless loops
            if (parent == null || parent.equals(ep)) {
                return false;
            }
            ep = parent;
        }
        return false;
    }
    /*
     * ---------- end of auto-placing and auto-connecting --------------
     */

    /**
     * Returns the grid horizontal spacing or the x value from the preference page, if the editor's property is not
     * set.
     *
     * @return the editors grid, or the value from the pref page (if not set in the editor)
     */
    public int getEditorGridX() {
        GraphicalViewer viewer = getViewer();
        if (viewer == null) {
            return getPrefGridXSize();
        }
        Dimension grid = (Dimension)viewer.getProperty(SnapToGrid.PROPERTY_GRID_SPACING);
        if (grid != null) {
            return grid.width;
        }
        return getPrefGridXSize();
    }

    /**
     * Returns the grid vertical spacing or the y value from the preference page, if the editor's property is not
     * set.
     *
     * @return the editors grid, or the value from the pref page (if not set in the editor)
     */
    public int getEditorGridY() {
        GraphicalViewer viewer = getViewer();
        if (viewer == null) {
            return getPrefGridYSize();
        }
        Dimension grid = (Dimension)viewer.getProperty(SnapToGrid.PROPERTY_GRID_SPACING);
        if (grid != null) {
            return grid.height;
        }
        return getPrefGridYSize();
    }

    /**
     * Returns true, if the grid is visible in this editor (or the preference page value if the editor's property is not
     * set).
     *
     * @return true, if the grid is visible in this editor
     */
    public boolean getEditorIsGridVisible() {
        GraphicalViewer viewer = getViewer();
        if (viewer == null) {
            return getPrefIsGridVisible();
        }
        Boolean visi = (Boolean)viewer.getProperty(SnapToGrid.PROPERTY_GRID_VISIBLE);
        if (visi != null) {
            return visi.booleanValue();
        }
        return getPrefIsGridVisible();
    }

    /**
     * Returns true, if the grid is enabled in this editor (or the preference page value if the editor's property is not
     * set).
     *
     * @return true, if snap to grid is enabled in this editor
     */
    public boolean getEditorSnapToGrid() {
        GraphicalViewer viewer = getViewer();
        if (viewer == null) {
            return getPrefSnapToGrid();
        }
        Boolean snap = (Boolean)viewer.getProperty(SnapToGrid.PROPERTY_GRID_ENABLED);
        if (snap == null) {
            return getPrefSnapToGrid();
        }
        return snap.booleanValue();
    }

    /**
     * Returns the curved connections property or takes it from the preference page if not set.
     *
     * @return whether connections are drawn curved
     */
    private boolean getHasCurvedConnections() {
        if (m_hasCurvedConnections != null) {
            return m_hasCurvedConnections;
        } else {
            return getPrefHasCurvedConnections();
        }
    }

    /**
     * Returns the connection line width property or takes the value from the preference page if not set.
     *
     * @return the line width
     */
    private int getConnectionLineWidth() {
        if (m_connectionLineWidth > 0) {
            return m_connectionLineWidth;
        } else {
            return getPrefConnectionLineWidth();
        }
    }

    /**
     * @return the value from the preference page for 'show grid' (each editor has its own property value which could be
     *         different)
     */
    public static boolean getPrefIsGridVisible() {
        IPreferenceStore prefStore = KNIMEUIPlugin.getDefault().getPreferenceStore();
        return prefStore.getBoolean(PreferenceConstants.P_GRID_SHOW);
    }

    /**
     * @return the value from the preference page for 'snap to grid' (each editor has its own property value which could
     *         be different)
     */
    public static boolean getPrefSnapToGrid() {
        IPreferenceStore prefStore = KNIMEUIPlugin.getDefault().getPreferenceStore();
        return prefStore.getBoolean(PreferenceConstants.P_GRID_SNAP_TO);
    }

    /**
     * @return the preference page value for the horizontal grid size (or the default value if zero or negative)
     */
    public static int getPrefGridXSize() {
        IPreferenceStore prefStore = KNIMEUIPlugin.getDefault().getPreferenceStore();
        int gridSize = prefStore.getInt(PreferenceConstants.P_GRID_SIZE_X);
        if (gridSize <= 0) {
            gridSize = prefStore.getDefaultInt(PreferenceConstants.P_GRID_SIZE_X);
        }
        return gridSize;
    }

    /**
     * @return the preference page value for the vertical grid size (or the default value if zero or negative)
     */
    public static int getPrefGridYSize() {
        IPreferenceStore prefStore = KNIMEUIPlugin.getDefault().getPreferenceStore();
        int gridSize = prefStore.getInt(PreferenceConstants.P_GRID_SIZE_Y);
        if (gridSize <= 0) {
            gridSize = prefStore.getDefaultInt(PreferenceConstants.P_GRID_SIZE_Y);
        }
        return gridSize;
    }

    /**
     * @return the preference page value for whether to show node connections as curved lines
     */
    public static boolean getPrefHasCurvedConnections() {
        IPreferenceStore prefStore = KNIMEUIPlugin.getDefault().getPreferenceStore();
        return prefStore.getBoolean(PreferenceConstants.P_CURVED_CONNECTIONS);
    }

    /**
     * @return the preference page value for line width of a connection between two nodes
     */
    public static int getPrefConnectionLineWidth() {
        IPreferenceStore prefStore = KNIMEUIPlugin.getDefault().getPreferenceStore();
        int lineWidth = prefStore.getInt(PreferenceConstants.P_CONNECTIONS_LINE_WIDTH);
        if (lineWidth <= 0) {
            lineWidth = prefStore.getDefaultInt(PreferenceConstants.P_CONNECTIONS_LINE_WIDTH);
        }
        return lineWidth;
    }

    /**
     * Adjusts the passed offset to the grid size. The result is a multiple of the grid X size equal to or larger than
     * the passed value (disregards the snap to grid flag).
     *
     * @param xOffset to translate into a 'grid compatible' offset
     * @return a multiple of the grid X size equal to or larger than the passed value
     */
    public int getEditorGridXOffset(final int xOffset) {
        int gridSizeX = getEditorGridX();
        if (gridSizeX > 1 && (xOffset % gridSizeX != 0)) {
            return ((xOffset / gridSizeX) + 1) * gridSizeX;
        }
        return xOffset;
    }

    /**
     * The result is a multiple of the grid Y size equal to or larger than the passed value (disregards the snap to grid
     * flag).
     *
     * @param yOffset to translate into a 'grid compatible' offset
     * @return a multiple of the grid Y size equal to or larger than the passed value
     */
    public int getEditorGridYOffset(final int yOffset) {
        int gridSizeY = getEditorGridY();
        if (gridSizeY > 1 && (yOffset % gridSizeY != 0)) {
            return ((yOffset / gridSizeY) + 1) * gridSizeY;
        }
        return yOffset;
    }

    /**
     * Returns the closest location that is located on the grid.
     *
     * @param loc reference point for the closest grid location, must be translated relative to the container
     * @return closest grid point
     */
    public Point getClosestGridLocation(final Point loc) {
        Point location = loc.getCopy();
        IFigure gridContainer = ((WorkflowRootEditPart)getViewer().getRootEditPart().getContents()).getFigure();
        gridContainer.translateToRelative(location);
        Point result = location.getCopy();
        int locX = loc.x;
        int gridX = getEditorGridX();
        if (gridX > 1) {
            // distance to the left grid line (or right, if locX is negative)
            int leftGrid = (locX / gridX) * gridX;
            if (Math.abs(locX - leftGrid) <= (gridX / 2)) {
                result.x = leftGrid;
            } else {
                // location is closer to the next grid (right of the location, or left if x is negative)
                result.x = leftGrid + (((int)Math.signum(locX)) * gridX);
            }
        }
        int locY = loc.y;
        int gridY = getEditorGridY();
        if (gridY > 1) {
            // distance to the next upper grid line (or lower line, if y is negative)
            int upperGrid = (locY / gridY) * gridY;
            if (Math.abs(locY - upperGrid) <= (gridY / 2)) {
                // location is closer to the upper grid line (or lower line, if y is negative)
                result.y = upperGrid;
            } else {
                // location is closer to the next lower grid
                result.y = upperGrid + (((int)Math.signum(locY)) * gridY);
            }
        }
        return result;
    }

    /**
     * Returns a point on the grid that has equal or larger coordinates than the passed location.
     *
     * @param loc the reference point for the next grid location
     * @return next grid location (right of and lower than argument location or equal)
     */
    public final Point getNextGridLocation(final Point loc) {
        Point location = loc.getCopy();
        Point result = location.getCopy();
        int stepX = (loc.x >= 0) ? 1 : 0;
        int gridX = getEditorGridX();
        if (gridX > 1 && result.x % gridX != 0) {
            result.x = ((loc.x / gridX) + stepX) * gridX;
        }

        int stepY = (loc.y >= 0) ? 1 : 0;
        int gridY = getEditorGridY();
        if (gridY > 1 && result.y % gridY != 0) {
            result.y = ((loc.y / gridY) + stepY) * gridY;
        }
        return result;
    }

    /**
     * Returns a point on the grid that has equal or smaller coordinates than the passed location.
     *
     * @param loc the reference point for the next grid location. Coordinates must be relative to the viewer contents
     *            (cursor/drop locations must be translated).
     * @return previous grid location (left of and upper than argument location or equal)
     */
    public final Point getPrevGridLocation(final Point loc) {
        Point location = loc.getCopy();
        // container coordinates could be negative
        Point result = location.getCopy();
        int stepX = (location.x >= 0) ? 0 : -1;
        int gridX = getEditorGridX();
        if (gridX > 1 && result.x % gridX != 0) {
            result.x = ((location.x / gridX) + stepX) * gridX;
        }

        int stepY = (location.y >= 0) ? 0 : -1;
        int gridY = getEditorGridY();
        if (gridY > 1 && result.y % gridY != 0) {
            result.y = ((location.y / gridY) + stepY) * gridY;
        }
        return result;
    }


    /**
     * This is a static convenience method which involves fetching the active page's active editor, and then returning
     * the instance of this class attached to it.
     *
     * @return the glass pane {@code ViewportPinningGraphicalViewer} or null if we were unable to get an active
     *         page or an active editor for it.
     */
    public static ViewportPinningGraphicalViewer getActiveViewer() {
        final IWorkbenchWindow iw = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

        if (iw != null) {
            final IWorkbenchPage page = iw.getActivePage();

            if (page != null) {
                final WorkflowEditor we = (WorkflowEditor)page.getActiveEditor();

                if (we != null) {
                    return (ViewportPinningGraphicalViewer)we.getGraphicalViewer();
                }
            }
        }

        return null;
    }

    /**
     * Returns the vertical grid distance for the active workflow editor (or -1 if no workflow editor is active at the
     * moment).
     *
     * @return the y grid size for the active workflow editor (or -1)
     * @see #getEditorGridY()
     */
    public static int getActiveEditorGridY() {
        IWorkbenchWindow activeWorkbenchWindow = Workbench.getInstance().getActiveWorkbenchWindow();
        if (activeWorkbenchWindow == null) {
            return -1;
        }
        IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
        if (activePage == null) {
            return -1;
        }
        IEditorPart activeEditor = activePage.getActiveEditor();
        if (activeEditor instanceof WorkflowEditor) {
            return ((WorkflowEditor)activeEditor).getEditorGridY();
        }
        return -1;
    }

    /**
     * Returns the horizontal grid distance for the active workflow editor (or -1 if no workflow editor is active at the
     * moment).
     *
     * @return the x grid size for the active workflow editor (or -1)
     * @see #getEditorGridX()
     */
    public static int getActiveEditorGridX() {
        IWorkbenchWindow activeWorkbenchWindow = Workbench.getInstance().getActiveWorkbenchWindow();
        if (activeWorkbenchWindow == null) {
            return -1;
        }
        IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
        if (activePage == null) {
            return -1;
        }
        IEditorPart activeEditor = activePage.getActiveEditor();
        if (activeEditor instanceof WorkflowEditor) {
            return ((WorkflowEditor)activeEditor).getEditorGridX();
        }
        return -1;
    }

    /**
     * Returns true, if the active editor is a workflow editor and snap to grid is enabled. False otherwise.
     *
     * @return true, if the active editor is a workflow editor and snap to grid is enabled. False otherwise
     * @see #getEditorSnapToGrid()
     */
    public static boolean getActiveEditorSnapToGrid() {
        @SuppressWarnings("restriction")
        final Optional<Boolean> activeEditor = Optional.ofNullable(Workbench.getInstance())//
            .map(b -> b.getActiveWorkbenchWindow())//
            .map(w -> w.getActivePage())//
            .map(p -> p.getActiveEditor())
            .map(e -> e instanceof WorkflowEditor && ((WorkflowEditor)e).getEditorSnapToGrid());
        return activeEditor.orElse(false);
    }

    /**
     * Returns the closest location on the grid of the active editor, or the argument if no workflow editor is active.
     * @param loc the ref point
     * @return the closest location on the grid of the active editor, or the argument if no workflow editor is active
     * @see #getClosestGridLocation(Point)
     */
    public static Point getActiveEditorClosestGridLocation(final Point loc) {
        IWorkbenchWindow activeWorkbenchWindow = Workbench.getInstance().getActiveWorkbenchWindow();
        if (activeWorkbenchWindow == null) {
            return loc;
        }
        IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
        if (activePage == null) {
            return loc;
        }
        IEditorPart activeEditor = activePage.getActiveEditor();
        if (activeEditor instanceof WorkflowEditor) {
            return ((WorkflowEditor)activeEditor).getClosestGridLocation(loc);
        }
        return loc;
    }

    /**
     * Returns grid location on the next lower right grid location in the active editor, or the argument, if no workflow
     * editor is active.
     *
     * @param loc the ref point
     * @return the next grid point if the active editor is a worflow editor
     * @see #getNextGridLocation(Point)
     */
    public static Point getActiveEditorNextGridLocation(final Point loc) {
        IWorkbenchWindow activeWorkbenchWindow = Workbench.getInstance().getActiveWorkbenchWindow();
        if (activeWorkbenchWindow == null) {
            return loc;
        }
        IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
        if (activePage == null) {
            return loc;
        }
        IEditorPart activeEditor = activePage.getActiveEditor();
        if (activeEditor instanceof WorkflowEditor) {
            return ((WorkflowEditor)activeEditor).getClosestGridLocation(loc);
        }
        return loc;
    }

    /**
     * The result is a multiple of the grid Y size of the active editor equal to or larger than the passed value
     * (disregards the snap to grid flag).
     *
     * @param yOffset to translate into a 'grid compatible' offset
     * @return a multiple of the grid Y size equal to or larger than the passed value - or the argument, if no workflow
     * editor is active
     * @see #getEditorGridYOffset(int)
     */
    public static int getActiveEditorGridYOffset(final int yOffset) {
        IWorkbenchWindow activeWorkbenchWindow = Workbench.getInstance().getActiveWorkbenchWindow();
        if (activeWorkbenchWindow == null) {
            return yOffset;
        }
        IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
        if (activePage == null) {
            return yOffset;
        }
        IEditorPart activeEditor = activePage.getActiveEditor();
        if (activeEditor instanceof WorkflowEditor) {
            return ((WorkflowEditor)activeEditor).getEditorGridYOffset(yOffset);
        }
        return yOffset;
    }

    /**
     * The result is a multiple of the grid X size of the active editor equal to or larger than the passed value
     * (disregards the snap to grid flag).
     *
     * @param xOffset to translate into a 'grid compatible' offset
     * @return a multiple of the grid X size equal to or larger than the passed value - or the argument, if no workflow
     * editor is active
     * @see #getEditorGridXOffset(int)
     */
    public static int getActiveEditorGridXOffset(final int xOffset) {
        IWorkbenchWindow activeWorkbenchWindow = Workbench.getInstance().getActiveWorkbenchWindow();
        if (activeWorkbenchWindow == null) {
            return xOffset;
        }
        IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
        if (activePage == null) {
            return xOffset;
        }
        IEditorPart activeEditor = activePage.getActiveEditor();
        if (activeEditor instanceof WorkflowEditor) {
            return ((WorkflowEditor)activeEditor).getEditorGridXOffset(xOffset);
        }
        return xOffset;
    }


    /*----------------------------------------------------------------------------------------------------------*/

    /**
     * Listener callback, listens to workflow events and triggers UI updates.
     *
     * {@inheritDoc}
     */
    @Override
    public void workflowChanged(final WorkflowEvent event) {
        LOGGER.debug("Workflow event triggered: " + event.toString());
        SyncExecQueueDispatcher.asyncExec(() -> {
            if (WorkflowEditor.this.isClosed()) {
                return;
            }

            switch (event.getType()) {
                case NODE_REMOVED:
                    Object oldValue = event.getOldValue();
                    // close sub-editors if a child metanode is deleted
                    WorkflowManagerUI wm = null;
                    //NOTE: workflow event can contain either UI objects or non-UI objects!!
                    UI uiVal = wrap(oldValue);
                    if (uiVal instanceof WorkflowManagerUI) {
                        wm = (WorkflowManagerUI)uiVal;
                    } else if (uiVal instanceof SubNodeContainerUI) {
                        wm = ((SubNodeContainerUI)uiVal).getWorkflowManager();
                    }
                    if (wm != null) {
                        // since the equals method of the WorkflowManagerInput
                        // only looks for the WorkflowManager, we can pass
                        // null as the editor argument
                        WorkflowManagerInput in = new WorkflowManagerInput(wm, (WorkflowEditor)null);
                        IEditorPart editor = getEditorSite().getPage().findEditor(in);
                        if (editor != null) {
                            editor.getEditorSite().getPage().closeEditor(editor, false);
                        }
                    }
                    break;
                case CONNECTION_ADDED:
                    SyncExecQueueDispatcher.asyncExec(() -> {
                        // We need an additional cycle delay for the situation in which this connection add
                        //  was achieved by dragging on the port of a not-currently-selected-node; in that case
                        //  we need the seletion process of that node to complete before telling the connection
                        //  highlighter to highlight connections based on the current workflow selection.
                        if (WorkflowEditor.this.isClosed()) {
                            return;
                        }

                        m_connectionHighlighter.selectionChanged(WorkflowEditor.this,
                                                                 getSite().getSelectionProvider().getSelection());
                    });
                case CONNECTION_REMOVED:
                    getViewer().getContents().refresh();
                    break;
                case WORKFLOW_DIRTY:
                    if (isComponentProjectWFM()) {
                        //filter out the dirty events due to node state changes
                        //because component projects are saved without node states
                        if (getWorkflowManager().get().getProjectComponent().get().getTrackedChanges()
                            .map(tc -> tc.hasNodeStateChanges() && !tc.hasOtherChanges()).orElse(false)) {
                            break;
                        }
                    }
                    markDirty();
                    break;
                case NODE_SETTINGS_CHANGED:
                    markDirty();
                    break;
                default:
                    // all other event types are handled somewhere else, e.g. in edit policies etc
            }
            updateActions();
        });
    }

    /** {@inheritDoc} */
    @Override
    public void nodePropertyChanged(final NodePropertyChangedEvent e) {
        switch (e.getProperty()) {
        case JobManager:
            updateJobManagerDisplay();
            break;
        case Name:
            updatePartName();
            break;
        case TemplateConnection:
            updateEditorBackgroundColor();
            break;
        case MetaNodePorts:
            // for metanode editors in case the port bar needs to dis/appear
            runInDisplayThread(() -> getViewer().getContents().refresh());
            break;
        case LockStatus:
            // nothing to do
            break;
        default:
            throw new AssertionError("Unhandeled switch case: " + e.getProperty());
        }
    }

    /**
     * Unsets the dirty flag if underlying wfm is not dirty.
     * @since 4.6 (made public)
     **/
    public void unmarkDirty() {
        if (m_isDirty && !m_manager.isDirty()) {
            m_isDirty = false;
            SyncExecQueueDispatcher.asyncExec(new Runnable() {
                @Override
                public void run() {
                    firePropertyChange(IEditorPart.PROP_DIRTY);
                }
            });
        }
    }

    /**
     * Marks this editor as dirty and notifies the registered listeners.
     */
    public void markDirty() {
        //only can be marked dirty if not write protected
        if (!m_manager.isWriteProtected()) {
            m_manager.setDirty(); // call anyway to allow auto-save copy to be dirty (the WFM has 2 dirty flags, really)
            //some WM-implementations won't allow to be set dirty -> i.e. check whether dirty
            if (!m_isDirty && m_manager.isDirty()) {
                m_isDirty = true;

                SyncExecQueueDispatcher.asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        if (!WorkflowEditor.this.isClosed()) {
                            firePropertyChange(IEditorPart.PROP_DIRTY);
                        }
                    }
                });
                if (m_parentEditor != null) {
                    m_parentEditor.markDirty();
                }
                //mark dirty not always called from UI-thread
                Display.getDefault().asyncExec(() -> {
                    updateWorkflowMessages();
                });
            }
        }
    }

    void setWorkflowManagerUI(final WorkflowManagerUI manager) {
        if (manager == m_manager) {
            return;
        }
        if (m_manager != null) {
            m_manager.removeListener(this);
            if (m_nodeSupplantDragListener != null) {
                m_manager.removeListener(m_nodeSupplantDragListener);
            }
            if (getViewer() != null) {
                m_manager.removeListener(getWorkflowFigure());
            }
            m_manager.removeNodePropertyChangedListener(this);
            m_manager.removeNodeStateChangeListener(this);
            m_manager.removeUIInformationListener(this);
        }
        m_manager = manager;
        if (m_manager != null) {
            m_manager.addListener(this);
            if (m_nodeSupplantDragListener != null) {
                m_manager.addListener(m_nodeSupplantDragListener);
            }
            if (getViewer() != null) {
                m_manager.addListener(getWorkflowFigure());
            }
            m_manager.addNodePropertyChangedListener(this);
            m_manager.addNodeStateChangeListener(this);
            m_manager.addUIInformationListener(this);
        }
    }

    /**
     * Sets the underlying workflow manager for this editor.
     *
     * @param manager the workflow manager to set
     */
    void setWorkflowManager(final WorkflowManager manager) {
        setWorkflowManagerUI(WorkflowManagerWrapper.wrap(manager));
    }

    /**
     * @return returns true if this editor has been closed on the workbench
     */
    public boolean isClosed() {
        return m_closed;
    }

    /**
     * Transposes a point according to the given zoom manager.
     *
     * @param zoomManager the zoom manager providing the zoom levels
     * @param pointToAdapt the point to adapt
     * @param adaptViewPortLocation The adapt view port location
     */
    public static void transposeZoom(final ZoomManager zoomManager,
            final Point pointToAdapt, final boolean adaptViewPortLocation) {

        double zoomLevel = zoomManager.getZoom();

        // adapt the location accordint to the zoom level
        pointToAdapt.x = (int)(pointToAdapt.x * zoomLevel);
        pointToAdapt.y = (int)(pointToAdapt.y * zoomLevel);

        if (adaptViewPortLocation) {
            Point viewPortLocation =
                    zoomManager.getViewport().getViewLocation();
            pointToAdapt.x -= viewPortLocation.x;
            pointToAdapt.y -= viewPortLocation.y;
        }
    }

    /**
     * Adapts a point according to the given zoom manager.
     *
     * @param zoomManager the zoom manager providing the zoom levels
     * @param pointToAdapt the point to adapt
     * @param adaptViewPortLocation The adapt view port location
     */
    public static void adaptZoom(final ZoomManager zoomManager,
            final Point pointToAdapt, final boolean adaptViewPortLocation) {

        if (adaptViewPortLocation) {
            Point viewPortLocation =
                    zoomManager.getViewport().getViewLocation();
            pointToAdapt.x += viewPortLocation.x;
            pointToAdapt.y += viewPortLocation.y;
        }
        double zoomLevel = zoomManager.getZoom();

        // adapt the location according to the zoom level
        pointToAdapt.x = (int)(pointToAdapt.x * (1.0 / zoomLevel));
        pointToAdapt.y = (int)(pointToAdapt.y * (1.0 / zoomLevel));
    }

    /**
     * Adapts a precission point according to the given zoom manager.
     *
     * @param zoomManager the zoom manager providing the zoom levels
     * @param pointToAdapt the point to adapt
     * @param adaptViewPortLocation The adapt view port location
     */
    public static void adaptZoom(final ZoomManager zoomManager,
            final PrecisionPoint pointToAdapt,
            final boolean adaptViewPortLocation) {

        if (adaptViewPortLocation) {
            Point viewPortLocation =
                    zoomManager.getViewport().getViewLocation();
            pointToAdapt.x += viewPortLocation.x;
            pointToAdapt.y += viewPortLocation.y;
        }
        double zoomLevel = zoomManager.getZoom();

        // adapt the location accordint to the zoom level
        pointToAdapt.preciseX = (pointToAdapt.x * (1.0 / zoomLevel));
        pointToAdapt.preciseY = (pointToAdapt.y * (1.0 / zoomLevel));
    }

    /**
     * Set if the workflow loading process. Should only be invoked during
     * workflow loading.
     *
     * @param dirty whether the editor should be marked as dirty or not
     * @see LoadWorkflowRunnable
     */
    void setIsDirty(final boolean dirty) {
        m_isDirty = dirty;
    }

    void setZoomComboBox(final ZoomComboContributionItem combo) {
        m_zoomComboBox = combo;
    }

    /**
     * @return The selection tool
     */
    public WorkflowSelectionTool getSelectionTool() {
        return m_selectionTool;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public void stateChanged(final NodeStateEvent state) {
        if (!isComponentProjectWFM()) {
            //in case of component, execution states changes are not saved
            markDirty();
        }
    }

    /**
     * @return <code>true</code> if the opened workflow is a workflow of a component that is edited directly (i.e. a
     *         components project's workflow)
     */
    private boolean isComponentProjectWFM() {
        return getWorkflowManager().map(wfm -> wfm.isComponentProjectWFM()).orElse(false);
    }

    /**
     * UI information listener only relevant if this editor is for a metanode
     * (update part name). {@inheritDoc}
     */
    @Override
    public void nodeUIInformationChanged(final NodeUIInformationEvent evt) {
        updatePartName();
    }

    /** {@inheritDoc} */
    @Override
    public void propertyChange(final PropertyChangeEvent event) {
        switch (event.getProperty()) {
            case PreferenceConstants.P_AUTO_SAVE_DATA:
            case PreferenceConstants.P_AUTO_SAVE_ENABLE:
            case PreferenceConstants.P_AUTO_SAVE_INTERVAL:
                setupAutoSaveSchedule();
                break;
            case PreferenceConstants.P_REMOTE_WORKFLOW_EDITOR_AUTO_REFRESH:
            case PreferenceConstants.P_REMOTE_WORKFLOW_EDITOR_AUTO_REFRESH_INTERVAL_MS:
            case PreferenceConstants.P_REMOTE_WORKFLOW_EDITOR_EDITS_DISABLED:
                updateWorkflowMessages();
                updateEditorBackgroundColor();
                break;
            case PreferenceConstants.P_EDITOR_ZOOM_LEVELS:
            case PreferenceConstants.P_EDITOR_ZOOM_MODIFIED_DELTA:
                updateZoomLevelSettings();
                break;
            case PreferenceConstants.P_EDITOR_SELECTED_NODE_HIGHLIGHT_CONNECTIONS:
            case PreferenceConstants.P_EDITOR_SELECTED_NODE_CONNECTIONS_HIGHLIGHT_COLOR:
            case PreferenceConstants.P_EDITOR_SELECTED_NODE_FLOW_CONNECTION_HIGHLIGHT_COLOR:
            case PreferenceConstants.P_EDITOR_SELECTED_NODE_CONNECTIONS_WIDTH_DELTA:
                updateConnectionHighlighting();
                break;
            default:
        }
    }

    private void openErrorDialogAndCloseEditor(final String message) {
        if (message != LoadWorkflowRunnable.INCOMPATIBLE_VERSION_MSG) { // string identiy is OK here
            final var errorTitle = "Workflow could not be opened";
            if (Display.getDefault().getThread() == Thread.currentThread()) {
                MessageDialog.openError(SWTUtilities.getActiveShell(), errorTitle, message);
            } else {
                // 20140525 - dunno why we ever use Swing here - keep unmodified to be sure
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(null, message, errorTitle, JOptionPane.ERROR_MESSAGE));
            }
        }
        Display.getDefault().asyncExec(() -> getEditorSite().getPage().closeEditor(WorkflowEditor.this, false));
    }

    /**
     * @return false if the save to the server failed for some reason, otherwise true
     */
    private boolean saveBackToServer() {
        if (m_parentEditor != null) { // parent does it if this is a metanode editor
            return m_parentEditor.saveBackToServer();
        }

        final var remoteMountpointURI = Wrapper.unwrapWFM(m_manager).getContextV2().getMountpointURI().orElseThrow();
        final var remoteStore = ExplorerFileSystem.INSTANCE.getStore(remoteMountpointURI);

        final var fetchInfo = remoteStore.fetchInfo();
        if (fetchInfo.exists()) {
            if (!fetchInfo.isModifiable()) {
                MessageDialog.openError(getSite().getShell(), "Workflow not writable",
                    "You don't have permissions to overwrite the workflow. Use \"Save As...\" in order to save it to "
                    + "a different location.");
                return false;
            }

            boolean snapshotSupported = remoteStore.getContentProvider().supportsSnapshots();
            final AtomicReference<SnapshotPanel> snapshotPanel = new AtomicReference<SnapshotPanel>(null);
            MessageDialog dlg = new MessageDialog(getSite().getShell(), "Overwrite on server?", null,
                "The workflow\n\n\t" + remoteStore.getMountIDWithFullPath()
                    + "\n\nalready exists on the server. Do you want to overwrite it?\n",
                MessageDialog.QUESTION, new String[] { IDialogConstants.NO_LABEL, IDialogConstants.YES_LABEL }, 1) {
                /**
                 * {@inheritDoc}
                 */
                @Override
                protected Control createCustomArea(final Composite parent) {
                    if (snapshotSupported) {
                        snapshotPanel.set(new SnapshotPanel(parent, SWT.NONE,
                            remoteStore.getContentProvider().isForceSnapshotCreation()));
                        snapshotPanel.get().setEnabled(true);
                        return snapshotPanel.get();
                    } else {
                        return null;
                    }
                }
            };
            int dlgResult = dlg.open();
            if (dlgResult != 1) {
                return false;
            }

            if ((snapshotPanel.get() != null) && (snapshotPanel.get().createSnapshot())) {
                try {
                    ((RemoteExplorerFileStore)remoteStore).createSnapshot(snapshotPanel.get().getComment());
                } catch (CoreException e) {
                    final var msg = "Unable to create snapshot before overwriting the workflow:\n" + e.getMessage()
                            + "\n\nUpload was canceled.";
                    LOGGER.error("Unable to create snapshot before overwriting the workflow: " + e.getMessage()
                            + " Upload was canceled.", e);
                    MessageDialog.openError(getSite().getShell(), "Server Error", msg);
                    return false;
                }
            }
        } else {
            final AbstractExplorerFileStore parent = remoteStore.getParent();
            if (parent == null) {
                MessageDialog.openError(getSite().getShell(), "Workflow's parent folder not accessible",
                    "You seem to be logged out of the mointpoint this workflow is stored on. "
                        + "Please reconnect or use \"Save As...\" to save the workflow somewhere else.");
                return false;
            } else if (!parent.fetchInfo().isModifiable()) {
                MessageDialog.openError(getSite().getShell(), "Workflow not writable",
                    "You don't have permissions to write into the workflow's parent folder. Use \"Save As...\" in order to"
                        + " save it to a different location.");
                return false;
            }
        }

        // selected a remote location: save + upload
        if (isDirty()) {
            saveTo(m_fileResource, new NullProgressMonitor(), true, null);
        }
        final var optLocalFS = getFileStore(m_fileResource)
                .flatMap(res -> ClassUtils.castOptional(LocalExplorerFileStore.class, res));
        if (optLocalFS.isEmpty()) {
            LOGGER.error("Unable to resolve current workflow location. Workflow not uploaded!");
            return false;
        }
        final var localFS = optLocalFS.get();
        try {
            if (!m_workflowCanBeDeleted.tryAcquire()) {
                throw new CoreException(new Status(IStatus.WARNING, ID,
                    "Cannot acquire lock since another save operation takes place."));
            }

            /**
             * If the thread we're on at this point weren't the SWT / main thread, then we'd just make an object
             *  to lock on, notifyAll in the callback, and then wait on it after invoking the perform
             *  upload async method.
             *
             * Instead, if this is part of an editor close operation, we need return false (to, ultimately,
             *  veto the close, then put up our "save dialog" blocking user action until we get the results
             *  of the upload attempt. If the upload is successful, then we hand-close the workbench editor.
             */

            if (m_isClosing) {
                final Display d = PlatformUI.getWorkbench().getDisplay();
                final IEditorPart ep
                        = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
                final MessageDialog pleaseWait
                                        = new MessageDialog(d.getActiveShell(), "Please wait", null,
                                                            "Your workflow is being uploaded to the server...",
                                                            MessageDialog.INFORMATION, new String[] {}, 0);
                final AbstractContentProvider.AfterRunCallback callback = throwable -> {
                    m_workflowCanBeDeleted.release();

                    pleaseWait.close();

                    if (throwable == null) {
                        updateWorkflowMessages();
                        notifySaveEventListeners();

                        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeEditor(ep, false);
                    } else {
                        waitUntilEditorIsCleanAndThenMarkDirty();
                        m_isClosing = false;
                    }
                };

                d.asyncExec(() -> {
                    try {
                        remoteStore.getContentProvider().performUploadAsync(localFS,
                            (RemoteExplorerFileStore)remoteStore, /*deleteSource=*/false,
                            remoteStore.getContentProvider().isForceResetOnUpload(), callback);

                        pleaseWait.open();
                    } catch (final Exception e) {
                        String msg = "Failed to upload the workflow to its remote location\n(" + e.getMessage() + ")";
                        LOGGER.error(msg, e);
                        callback.afterCompletion(e);

                        MessageDialog.openError(SWTUtilities.getActiveShell(), "Upload has failed", msg);
                    }
                });

                return false;
            } else {
                remoteStore.getContentProvider().performUploadAsync(localFS,
                    (RemoteExplorerFileStore)remoteStore, /*deleteSource=*/false,
                    remoteStore.getContentProvider().isForceResetOnUpload(),
                    throwable -> {
                        m_workflowCanBeDeleted.release();
                        if (throwable != null) {
                            waitUntilEditorIsCleanAndThenMarkDirty();
                        }
                    });
            }
        } catch (CoreException e) {
            String msg = "Failed to upload the workflow to its remote location\n(" + e.getMessage() + ")";
            LOGGER.error(msg, e);
            MessageDialog.openError(SWTUtilities.getActiveShell(), "Upload has failed", msg);
            return false;
        }
        return true;
    }

    /**
     * Eclipse will eventually mark us clean, even though the save hasn't yet finished -
     * correctly or incorrectly. We need make sure to mark ourselves dirty again since
     * the remote save failed.
     *
     * As Moritz has seen happen, we can apparently get here *too* quickly and Eclipse
     * ends up marking us clean after we do the below dirty set. This is the reason for
     * the following hacky thread sleep (the thread is of a scheduled action operating
     * asynchronously from vital threads.)
     */
    private void waitUntilEditorIsCleanAndThenMarkDirty() {
        var waitCount = 0;
        // we should no long be dirty if Eclipse has finished its save cycle
        while (isDirty()) {
            if (waitCount > 30) {
                LOGGER.warn("Eclipse never (im)properly set the dirty state.");
                m_isClosing = false;
                return;
            }
            try {
                Thread.sleep(100);
            } catch (final Exception e) { }  // NOSONAR

            waitCount++;
        }
        markDirty();
    }

    private void notifyCloseEventListeners() {
        final Collection<WorkflowEditorEventListener> workflowEditorEventListeners =
            WorkflowEditorEventListeners.getListeners();
        if (!workflowEditorEventListeners.isEmpty()) {
            SyncExecQueueDispatcher.asyncExec(() -> {
                final WorkflowEditorEvent event = WorkflowEditorEventListeners.createWorkflowEditorEvent(this);
                for (final WorkflowEditorEventListener listener : workflowEditorEventListeners) {
                    try {
                        listener.editorClosed(event);
                    } catch (final Throwable throwable) {
                        LOGGER.error("Workflow editor listener error.", throwable);
                    }
                }
            });
        }
    }

    private void notifySaveEventListeners() {
        final Collection<WorkflowEditorEventListener> workflowEditorEventListeners =
            WorkflowEditorEventListeners.getListeners();
        if (!workflowEditorEventListeners.isEmpty()) {
            SyncExecQueueDispatcher.asyncExec(() -> {
                final ActiveWorkflowEditorEvent event =
                    WorkflowEditorEventListeners.createActiveWorkflowEditorEvent(this);
                for (final WorkflowEditorEventListener listener : workflowEditorEventListeners) {
                    try {
                        listener.workflowSaved(event);
                    } catch (final Throwable throwable) {
                        LOGGER.error("Workflow editor listener error.", throwable);
                    }
                }
            });
        }
    }

    private final boolean isHubJob() {
        final var executionContext = getWorkflowManagerUI().getContext();
        return executionContext instanceof RemoteWorkflowContext && ((RemoteWorkflowContext)executionContext)
            .getWorkflowContextV2().map(context -> context.getLocationType() == LocationType.HUB_SPACE).orElse(false);
    }
}
