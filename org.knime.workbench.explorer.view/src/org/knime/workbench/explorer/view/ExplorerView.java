/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
package org.knime.workbench.explorer.view;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeMessageEvent;
import org.knime.core.node.workflow.NodeMessageListener;
import org.knime.core.node.workflow.NodePropertyChangedEvent;
import org.knime.core.node.workflow.NodePropertyChangedListener;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.node.workflow.NodeTimer;
import org.knime.core.node.workflow.WorkflowEvent;
import org.knime.core.node.workflow.WorkflowListener;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.core.WorkflowManagerTransfer;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;
import org.knime.workbench.explorer.view.actions.CollapseAction;
import org.knime.workbench.explorer.view.actions.CollapseAllAction;
import org.knime.workbench.explorer.view.actions.ConfigureExplorerViewAction;
import org.knime.workbench.explorer.view.actions.CopyLocationAction;
import org.knime.workbench.explorer.view.actions.CopyMountpointRelativeURLAction;
import org.knime.workbench.explorer.view.actions.CopyURLAction;
import org.knime.workbench.explorer.view.actions.CutCopyToClipboardAction;
import org.knime.workbench.explorer.view.actions.DownloadAndOpenWorkflowAction;
import org.knime.workbench.explorer.view.actions.ExpandAction;
import org.knime.workbench.explorer.view.actions.ExplorerAction;
import org.knime.workbench.explorer.view.actions.GlobalCancelWorkflowExecutionAction;
import org.knime.workbench.explorer.view.actions.GlobalConfigureWorkflowAction;
import org.knime.workbench.explorer.view.actions.GlobalCredentialVariablesDialogAction;
import org.knime.workbench.explorer.view.actions.GlobalDeleteAction;
import org.knime.workbench.explorer.view.actions.GlobalEditMetaInfoAction;
import org.knime.workbench.explorer.view.actions.GlobalExecuteWorkflowAction;
import org.knime.workbench.explorer.view.actions.GlobalOpenWorkflowVariablesDialogAction;
import org.knime.workbench.explorer.view.actions.GlobalQuickformWorkflowAction;
import org.knime.workbench.explorer.view.actions.GlobalRefreshAction;
import org.knime.workbench.explorer.view.actions.GlobalRenameAction;
import org.knime.workbench.explorer.view.actions.GlobalResetWorkflowAction;
import org.knime.workbench.explorer.view.actions.NewWorkflowAction;
import org.knime.workbench.explorer.view.actions.NewWorkflowGroupAction;
import org.knime.workbench.explorer.view.actions.NoMenuAction;
import org.knime.workbench.explorer.view.actions.PasteFromClipboardAction;
import org.knime.workbench.explorer.view.actions.SynchronizeExplorerViewAction;
import org.knime.workbench.explorer.view.actions.export.WorkflowExportAction;
import org.knime.workbench.explorer.view.actions.imports.WorkflowImportAction;
import org.knime.workbench.explorer.view.dnd.DragAndDropUtils;
import org.knime.workbench.explorer.view.dnd.ExplorerDragListener;
import org.knime.workbench.explorer.view.dnd.ExplorerDropListener;
import org.knime.workbench.repository.view.FilterViewContributionItem;
import org.knime.workbench.repository.view.TextualViewFilter;
import org.knime.workbench.ui.SyncExecQueueDispatcher;
import org.knime.workbench.ui.navigator.ProjectWorkflowMap;

/**
 *
 * @author Peter Ohl, KNIME.com, Zurich, Switzerland
 */
public class ExplorerView extends ViewPart implements WorkflowListener,
        NodeStateChangeListener, NodeMessageListener,
        NodePropertyChangedListener, IPropertyChangeListener,
        ISelectionChangedListener {

    /**
     * Helper class which sets and resets the global actions, if the search is selected or unselected.
     * @author Marcel Hanser
     */
    private final class FilterViewContributionItemExtension extends FilterViewContributionItem {
        /**
         * @param viewer the treeviewer
         * @param filter used view filter
         * @param liveUpdate live update settings
         */
        private FilterViewContributionItemExtension(final TreeViewer viewer,
            final TextualViewFilter filter, final boolean liveUpdate) {
            super(viewer, filter, liveUpdate);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Control createControl(final Composite parent) {
            Control createControl = super.createControl(parent);
            getCombo().addFocusListener(new FocusListener() {
                @Override
                public void focusLost(final FocusEvent e) {
                    hookGlobalActions();
                }

                @Override
                public void focusGained(final FocusEvent e) {
                    resetGlobalActions();
                }
            });
            return createControl;
        }
    }

    /** The ID of the view as specified by the extension. */
    public static final String ID = "org.knime.workbench.explorer.view";

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ExplorerView.class);

    private TreeViewer m_viewer;

    private final ContentDelegator m_contentDelegator = new ContentDelegator();

    private ExplorerDragListener m_dragListener;

    private ExplorerDropListener m_dropListener;

    private Clipboard m_clipboard;

    private CutCopyToClipboardAction m_copyAction;
    private CutCopyToClipboardAction m_cutAction;
    private PasteFromClipboardAction m_pasteAction;

    // selected after next refresh
    private final AtomicReference<Collection<AbstractExplorerFileStore>> m_nextSelection =
            new AtomicReference<Collection<AbstractExplorerFileStore>>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void createPartControl(final Composite parent) {
        Composite overall = new Composite(parent, SWT.NONE);
        overall.setLayout(new GridLayout(1, false));
        GridData data = new GridData(GridData.FILL_BOTH);
        data.verticalIndent = 0;
        data.horizontalIndent = 0;
        overall.setLayoutData(data);
        m_contentDelegator.addPropertyChangeListener(this);
        createTreeViewer(overall, m_contentDelegator);
        assert m_viewer != null; // should be set by createTreeViewer
        // needed by the toolbar and the menus
        m_clipboard = new Clipboard(Display.getCurrent()); // used by copy actions
        m_dragListener = new ExplorerDragListener(m_viewer);
        m_dropListener = new ExplorerDropListener(this);
        initDragAndDrop();
        createLocalToolBar();
        hookContextMenu();
        hookKeyListener();

        m_copyAction = new CutCopyToClipboardAction(this, "Copy", false);
        m_cutAction = new CutCopyToClipboardAction(this, "Move", true);
        m_pasteAction = new PasteFromClipboardAction(this);
        m_copyAction.setPasteAction(m_pasteAction);
        m_cutAction.setPasteAction(m_pasteAction);
        hookGlobalActions();
        // schedule future selection (must populate tree content first)
        Display.getCurrent().asyncExec(new Runnable() {
            @Override
            public void run() {
                new SynchronizeExplorerViewAction(ExplorerView.this,
                        m_contentDelegator).run();
            }
        });
    }

    private void hookKeyListener() {
        m_viewer.getControl().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(final KeyEvent event) {
                handleKeyReleased(event);
            }
        });
    }

    private void handleKeyReleased(final KeyEvent event) {
        final ExplorerAction action;
        if (event.keyCode == SWT.F2 && event.stateMask == 0) {
            action = new GlobalRenameAction(this);
        } else if (event.keyCode == SWT.F5 && event.stateMask == 0) {
            action = new GlobalRefreshAction(this);
        } else if (event.keyCode == SWT.DEL && event.stateMask == 0) {
            action = new GlobalDeleteAction(this);
        } else {
            action = null;
        }
        if (action != null && action.isEnabled()) {
            action.run();
            event.doit = false;
        }
    }

    private void initDragAndDrop() {
        LocalSelectionTransfer selectionTransfer =
                LocalSelectionTransfer.getTransfer();
        FileTransfer fileTransfer = FileTransfer.getInstance();
        WorkflowManagerTransfer wfmTransfer
            = WorkflowManagerTransfer.getTransfer();
        final int operation = DND.DROP_MOVE | DND.DROP_COPY;
        m_viewer.addDragSupport(operation, new Transfer[]{selectionTransfer},
                m_dragListener);
        m_viewer.addDropSupport(operation, new Transfer[]{selectionTransfer,
                fileTransfer, wfmTransfer}, m_dropListener);
    }

    private void hookGlobalActions() {
        IActionBars bars = getViewSite().getActionBars();
        bars.setGlobalActionHandler(ActionFactory.COPY.getId(), m_copyAction);
        bars.setGlobalActionHandler(ActionFactory.CUT.getId(), m_cutAction);
        bars.setGlobalActionHandler(ActionFactory.PASTE.getId(), m_pasteAction);
    }

    /**
     * Reset the default actions. Used by the inner class which activates them if
     * the search field gained focus.
     */
    private void resetGlobalActions() {
        IActionBars bars = getViewSite().getActionBars();
        bars.setGlobalActionHandler(ActionFactory.COPY.getId(),
            ActionFactory.COPY.create(this.getSite().getWorkbenchWindow()));
        bars.setGlobalActionHandler(ActionFactory.CUT.getId(),
            ActionFactory.CUT.create(this.getSite().getWorkbenchWindow()));
        bars.setGlobalActionHandler(ActionFactory.PASTE.getId(),
            ActionFactory.PASTE.create(this.getSite().getWorkbenchWindow()));
    }

    private void updateGlobalActions(final IStructuredSelection selection) {
        m_copyAction.updateSelection(selection);
        m_cutAction.updateSelection(selection);
        m_pasteAction.updateSelection();
    }

    private void createLocalToolBar() {
        IToolBarManager toolBarMgr =
            getViewSite().getActionBars().getToolBarManager();
        Action exp = new ExpandAction(this);
        exp.setToolTipText("Expands fully the selected element");
        toolBarMgr.add(exp);
        Action coll = new CollapseAction(this);
        coll.setToolTipText("Collapses the selected element.");
        toolBarMgr.add(coll);
        Action collAll = new CollapseAllAction(this);
        collAll.setToolTipText("Collapses the entire tree");
        toolBarMgr.add(collAll);
        Action refresh = new GlobalRefreshAction(this);
        toolBarMgr.add(new Separator());
        toolBarMgr.add(refresh);
        Action synchronize = new SynchronizeExplorerViewAction(this,
                m_contentDelegator);
        toolBarMgr.add(synchronize);
        toolBarMgr.add(new Separator());
        FilterViewContributionItemExtension filterViewContributionItem =
                new FilterViewContributionItemExtension(m_viewer, new ExplorerFilter(), false);

        toolBarMgr.add(filterViewContributionItem);
        toolBarMgr.add(new Separator());
        Action configure = new ConfigureExplorerViewAction(this,
            m_contentDelegator);
        toolBarMgr.add(configure);
    }

    private boolean openSelected() {
        IStructuredSelection selection =
                (IStructuredSelection)m_viewer.getSelection();
        if (selection.isEmpty()) {
            return false;
        }
        List<ContentObject> localWorkflowsToOpen = new ArrayList<ContentObject>();
        List<ContentObject> remoteWorkflowsToOpen = new ArrayList<ContentObject>();

        Iterator<?> iter = selection.iterator();
        while (iter.hasNext()) {
            Object sel = iter.next();
            if (sel instanceof ContentObject) {
                try {
                    ContentObject co = (ContentObject)sel;
                    AbstractExplorerFileStore fs = co.getObject();
                    if (fs.fetchInfo().isWorkflow()) {
                        if (fs instanceof RemoteExplorerFileStore) {
                            remoteWorkflowsToOpen.add(co);
                        } else {
                            File f = co.getObject().toLocalFile(EFS.NONE, null);
                            if (f != null) {
                                localWorkflowsToOpen.add(co);
                            }
                        }
                    }
                } catch (CoreException ce) {
                    // then don't add it
                }
            }
        }

        int numOfFlows = remoteWorkflowsToOpen.size() + localWorkflowsToOpen.size();
        if (numOfFlows > 5) {
            MessageBox mb =
                    new MessageBox(getViewSite().getShell(), SWT.ICON_QUESTION
                            | SWT.CANCEL | SWT.OK);
            mb.setText("Confirm creation of multiple editors");
            mb.setMessage("Are you sure you want to open " + numOfFlows + " editor windows?");
            if (mb.open() != SWT.OK) {
                return false;
            }
        }
        boolean opened = false;
        for (ContentObject co : localWorkflowsToOpen) {
            opened |= openEditor(co);
        }
        List<RemoteExplorerFileStore> remotes = new LinkedList<RemoteExplorerFileStore>();
        for (ContentObject co : remoteWorkflowsToOpen) {
            remotes.add((RemoteExplorerFileStore)co.getObject());
        }
        DownloadAndOpenWorkflowAction a;
        a = new DownloadAndOpenWorkflowAction(getSite().getPage(), remotes);
        a.run();
        m_viewer.update(localWorkflowsToOpen.toArray(), null);
        return opened;
    }

    private boolean openEditor(final ContentObject co) {
        AbstractExplorerFileStore filestore = co.getObject();

        if (filestore.fetchInfo().isDirectory()) {
            AbstractExplorerFileStore workflowFile = filestore.getChild(WorkflowPersistor.WORKFLOW_FILE);
            if (workflowFile.fetchInfo().exists()) {
                filestore = workflowFile;
            } else {
                // directories that are not workflows cannot be opened
                return false;
            }
        }

        IEditorDescriptor editorDescriptor;
        try {
            editorDescriptor = IDE.getEditorDescriptor(filestore.getName());
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                .openEditor(new FileStoreEditorInput(filestore), editorDescriptor.getId());
            NodeTimer.GLOBAL_TIMER.incWorkflowOpening();
            return true;
        } catch (PartInitException ex) {
            LOGGER.warn("Cannot open editor for " + filestore + ": " + ex.getMessage(), ex);
            return false;
        }
    }

    private void onDoubleClickInTreeViewerInSWT() {
        IStructuredSelection selection = (IStructuredSelection)m_viewer.getSelection();
        if (selection.toList().size() == 1) {
            Object firstElement = selection.getFirstElement();
            m_viewer.setExpandedState(firstElement, !m_viewer.getExpandedState(firstElement));
        }
    }

    private void createTreeViewer(final Composite parent,
            final ContentDelegator provider) {
        m_viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        m_viewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
        m_viewer.setComparator(new ExplorerViewComparator());
        m_viewer.setContentProvider(provider);
        m_viewer.setLabelProvider(provider);
        m_viewer.setInput(provider); // the provider is also the root!
        m_viewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                Display.getCurrent().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        if (!openSelected()) {
                            onDoubleClickInTreeViewerInSWT();
                        }
                    }
                });
            }
        });
        m_viewer.addSelectionChangedListener(this);
        // this allows other plugins to see our selection.
        getSite().setSelectionProvider(m_viewer);

        ProjectWorkflowMap.addStateListener(this);
        ProjectWorkflowMap.addWorkflowListener(this);
        //ProjectWorkflowMap.addNodeMessageListener(this);
        //ProjectWorkflowMap.addNodePropertyChangedListener(this);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void workflowChanged(final WorkflowEvent event) {
        switch (event.getType()) {
        case NODE_ADDED:
            NodeID id = event.getID();
            refreshAsync(id);
            break;
        case NODE_REMOVED:
            // can't just use the ID here as the workflow is no longer in
            // the static workflow map, try to get path from workflow and
            // refresh here
            Object oldValue = event.getOldValue();
            if (oldValue instanceof WorkflowManager) {
                WorkflowManager wm = (WorkflowManager)oldValue;
                ReferencedFile workingDir = wm.getWorkingDir();
                if (workingDir != null) {
                    File file = workingDir.getFile();
                    final AbstractExplorerFileStore fs =
                        ExplorerMountTable.getFileSystem().fromLocalFile(file);
                    if (fs != null) {
                        SyncExecQueueDispatcher.asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                if (m_viewer == null
                                        || m_viewer.getControl().isDisposed()) {
                                    return;
                                }
                                m_viewer.refresh(ContentObject.forFile(fs));
                            }
                        });
                    }
                }
            }
            break;
        default:
            // ignore
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stateChanged(final NodeStateEvent state) {
        refreshAsync(state.getSource());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void nodePropertyChanged(final NodePropertyChangedEvent e) {
        refreshAsync();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void messageChanged(final NodeMessageEvent messageEvent) {
        refreshAsync();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void propertyChange(final PropertyChangeEvent event) {
        if (event != null
                && ContentDelegator.CONTENT_CHANGED.equals(event.getProperty())) {
            if (event.getNewValue() instanceof AbstractExplorerFileStore) {
                refreshAsync(ContentDelegator.getTreeObjectFor((AbstractExplorerFileStore)event.getNewValue()));
            } else {
                refreshAsync();
            }
        } else {
            refreshAsync();
        }
    }

    private void refreshAsync() {
        refreshAsync((Object)null);
    }

    private void refreshAsync(final Object refreshRoot) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (m_viewer != null && !m_viewer.getControl().isDisposed()) {
                    if (refreshRoot != null) {
                        m_viewer.refresh(refreshRoot);
                    } else {
                        m_viewer.refresh();
                    }
                    Collection<AbstractExplorerFileStore> fs =
                            m_nextSelection.getAndSet(null);
                    if (fs != null) {
                        List<Object> sel = ContentDelegator.getTreeObjectList(fs);
                        m_viewer.setSelection(new StructuredSelection(sel),
                                true);
                    }

                    for (ViewerFilter vf : m_viewer.getFilters()) {
                        if ((vf instanceof TextualViewFilter)
                                && ((TextualViewFilter) vf).hasNonEmptyQuery()) {
                            m_viewer.expandAll();
                            break;
                        }
                    }
                }
            }
        });
    }

    /**
     * Sets the file that should be selected after the next refresh.
     *
     * @param sel the file to select
     */
    public void setNextSelection(final AbstractExplorerFileStore sel) {
        m_nextSelection.set(Collections.singletonList(sel));
    }

    /**
     * Sets the files that should be selected after the next refresh.
     *
     * @param sel the files to select
     * @since 3.0
     */
    public void setNextSelection(final Collection<AbstractExplorerFileStore> sel) {
        m_nextSelection.set(sel);
    }

    /** The set/map of node IDs that need to be refreshed. Only if a new id is
     * not in the map it will be queued for refresh. */
    private final ConcurrentHashMap<NodeID, NodeID> m_refreshSet =
        new ConcurrentHashMap<NodeID, NodeID>();

    private void refreshAsync(final NodeID node) {
        if (m_refreshSet.put(node, node) == null) { // freshly added to set
            SyncExecQueueDispatcher.asyncExec(new Runnable() {
                @Override
                public void run() {
                    if (m_viewer == null
                            || m_viewer.getControl().isDisposed()) {
                        return;
                    }
                    m_refreshSet.remove(node);
                    try {
                        URI wf = ProjectWorkflowMap.findProjectFor(node);
                        if (wf == null) {
                            return;
                        }
                        File file = new File(wf);
                        AbstractExplorerFileStore fs = ExplorerMountTable.
                            getFileSystem().fromLocalFile(file);
                        if (fs != null) {
                            m_viewer.refresh(ContentObject.forFile(fs));
                        }
                    } catch (IllegalArgumentException iae) {
                        // node couldn't be found -> so we don't make a refresh
                    }
                }
            });
        }
    }
    private void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(final IMenuManager manager) {
                ExplorerView.this.fillContextMenu(manager);
            }
        });
        Menu menu = menuMgr.createContextMenu(m_viewer.getControl());
        m_viewer.getControl().setMenu(menu);

        // This allows other plug-ins to add to our menu
        getSite().registerContextMenu(menuMgr, m_viewer);
    }

    private void fillContextMenu(final IMenuManager manager) {
        Set<String> ids = m_contentDelegator.getMountedIds();
        if (ids.size() == 0) {
            manager.add(new NoMenuAction(this));
            return;
        }
        final TreeSelection selection = (TreeSelection) m_viewer.getSelection();
        addGlobalActions(manager);

        Map<AbstractContentProvider, List<AbstractExplorerFileStore>> selFiles =
                DragAndDropUtils.getProviderMap(selection);

        manager.add(new Separator());
        /* All visible spaces with at least one selected file may contribute to
         * the menu. */
        for (AbstractContentProvider provider : selFiles.keySet()) {
            provider.addContextMenuActions(this, manager, ids, selFiles);
        }

        manager.add(new Separator());
        // Other plug-ins can contribute there actions here
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

    }

    private void addGlobalActions(final IMenuManager manager) {
        manager.add(new NewWorkflowAction(this));
        manager.add(new NewWorkflowGroupAction(this));
        manager.add(new Separator());
        manager.add(new WorkflowImportAction(this));
        manager.add(new WorkflowExportAction(this));
        manager.add(new Separator());
        manager.add(new GlobalDeleteAction(this));
        manager.add(new GlobalRenameAction(this));
        manager.add(new Separator());
        manager.add(new GlobalConfigureWorkflowAction(this));
        manager.add(new GlobalExecuteWorkflowAction(this));
        manager.add(new GlobalCancelWorkflowExecutionAction(this));
        manager.add(new GlobalResetWorkflowAction(this));
        manager.add(new Separator());
        manager.add(new GlobalQuickformWorkflowAction(this));
        manager.add(new Separator());
        manager.add(new GlobalCredentialVariablesDialogAction(this));
        manager.add(new GlobalOpenWorkflowVariablesDialogAction(this));
        manager.add(new Separator());
        manager.add(new GlobalEditMetaInfoAction(this));
        manager.add(new Separator());
        manager.add(new GlobalRefreshAction(this));
        manager.add(new Separator());

        IMenuManager copyUrlSubmenu = new MenuManager("Copy Location", "copy-url");
        manager.add(copyUrlSubmenu);

        copyUrlSubmenu.add(new CopyURLAction(this, m_clipboard));
        copyUrlSubmenu.add(new CopyMountpointRelativeURLAction(this, m_clipboard));
        copyUrlSubmenu.add(new CopyLocationAction(this, m_clipboard));
    }

    /**
     * Returns the clipboard for the current display.
     *
     * @return a clipboard
     */
    public Clipboard getClipboard() {
        return m_clipboard;
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    @Override
    public void setFocus() {
        m_viewer.getControl().setFocus();
    }

    /**
     * Returns the tree viewer component.
     *
     * @return a tree viewer
     */
    public TreeViewer getViewer() {
        return m_viewer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveState(final IMemento memento) {
        m_contentDelegator.saveState(memento.createChild("content"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final IViewSite site, final IMemento memento)
            throws PartInitException {
        super.init(site, memento);
        IMemento content = null;
        if (memento != null) {
            // restore visible mount points
            content = memento.getChild("content");
        }
        m_contentDelegator.restoreState(content);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        m_contentDelegator.removePropertyChangeListener(this);
        m_contentDelegator.dispose();
        ProjectWorkflowMap.removeStateListener(this);
        ProjectWorkflowMap.removeWorkflowListener(this);
//        ProjectWorkflowMap.removeNodePropertyChangedListener(this);
//        ProjectWorkflowMap.removeNodeMessageListener(this);
        if (m_clipboard != null) {
            // some times we get a NPE if the view is not fully initialized
            m_clipboard.dispose();
        }
    }

    /**
     * @return the IDs of the mount points that are shown in this view
     */
    public Set<String> getMountedIds() {
        return m_contentDelegator.getMountedIds();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selectionChanged(final SelectionChangedEvent event) {
        updateGlobalActions((IStructuredSelection)event.getSelection());
    }

}
