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
 */
package org.knime.workbench.explorer.view;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.filesystem.local.LocalFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeMessageEvent;
import org.knime.core.node.workflow.NodeMessageListener;
import org.knime.core.node.workflow.NodePropertyChangedEvent;
import org.knime.core.node.workflow.NodePropertyChangedListener;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.node.workflow.WorkflowEvent;
import org.knime.core.node.workflow.WorkflowListener;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.core.WorkflowManagerTransfer;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.MountPoint;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.MessageFileStore;
import org.knime.workbench.explorer.view.actions.CollapseAction;
import org.knime.workbench.explorer.view.actions.CollapseAllAction;
import org.knime.workbench.explorer.view.actions.ConfigureExplorerViewAction;
import org.knime.workbench.explorer.view.actions.CopyLocationAction;
import org.knime.workbench.explorer.view.actions.CopyURLAction;
import org.knime.workbench.explorer.view.actions.ExpandAction;
import org.knime.workbench.explorer.view.actions.ExplorerAction;
import org.knime.workbench.explorer.view.actions.GlobalCancelWorkflowExecutionAction;
import org.knime.workbench.explorer.view.actions.GlobalConfigureWorkflowAction;
import org.knime.workbench.explorer.view.actions.GlobalCopyAction;
import org.knime.workbench.explorer.view.actions.GlobalCredentialVariablesDialogAction;
import org.knime.workbench.explorer.view.actions.GlobalDeleteAction;
import org.knime.workbench.explorer.view.actions.GlobalEditMetaInfoAction;
import org.knime.workbench.explorer.view.actions.GlobalExecuteWorkflowAction;
import org.knime.workbench.explorer.view.actions.GlobalMoveAction;
import org.knime.workbench.explorer.view.actions.GlobalOpenWorkflowVariablesDialogAction;
import org.knime.workbench.explorer.view.actions.GlobalRefreshAction;
import org.knime.workbench.explorer.view.actions.GlobalRenameAction;
import org.knime.workbench.explorer.view.actions.GlobalResetWorkflowAction;
import org.knime.workbench.explorer.view.actions.NewWorkflowAction;
import org.knime.workbench.explorer.view.actions.NewWorkflowGroupAction;
import org.knime.workbench.explorer.view.actions.NoMenuAction;
import org.knime.workbench.explorer.view.actions.SynchronizeExplorerViewAction;
import org.knime.workbench.explorer.view.actions.export.WorkflowExportAction;
import org.knime.workbench.explorer.view.actions.imports.WorkflowImportAction;
import org.knime.workbench.explorer.view.dnd.DragAndDropUtils;
import org.knime.workbench.explorer.view.dnd.ExplorerDragListener;
import org.knime.workbench.explorer.view.dnd.ExplorerDropListener;
import org.knime.workbench.repository.view.FilterViewContributionItem;
import org.knime.workbench.repository.view.LabeledFilterViewContributionItem;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.SyncExecQueueDispatcher;
import org.knime.workbench.ui.navigator.ProjectWorkflowMap;

/**
 *
 * @author Peter Ohl, KNIME.com, Zurich, Switzerland
 */
public class ExplorerView extends ViewPart implements WorkflowListener,
        NodeStateChangeListener, NodeMessageListener,
        NodePropertyChangedListener, IPropertyChangeListener {

    /** The ID of the view as specified by the extension. */
    public static final String ID = "com.knime.workbench.userspace.view";

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ExplorerView.class);

    private static final ImageDescriptor IMG_COLLALL =
        AbstractUIPlugin.imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
                "icons/fav/collapseall.png");

    private TreeViewer m_viewer;

    private final ContentDelegator m_contentDelegator = new ContentDelegator();

    private FilterViewContributionItem m_toolbarFilterCombo;

    private ExplorerDragListener m_dragListener;

    private ExplorerDropListener m_dropListener;

    private Clipboard m_clipboard;

    // selected after next refresh
    private AtomicReference<AbstractExplorerFileStore> m_nextSelection =
            new AtomicReference<AbstractExplorerFileStore>();

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
        m_dropListener = new ExplorerDropListener(m_viewer);
        initDragAndDrop();
        makeGlobalActions();
        createLocalToolBar();
        hookContextMenu();
        hookKeyListener();
    }

    private void hookKeyListener() {
        m_viewer.getControl().addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(final KeyEvent event) {
                handleKeyPressed(event);
            }

            @Override
            public void keyReleased(final KeyEvent event) {
                handleKeyReleased(event);
            }
        });
    }

    private void handleKeyPressed(final KeyEvent event) {
        // nothing
    }

    private void handleKeyReleased(final KeyEvent event) {
        final ExplorerAction action;
        if (event.keyCode == SWT.F2 && event.stateMask == 0) {
            action = new GlobalRenameAction(this);
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

    private void makeGlobalActions() {

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
        m_toolbarFilterCombo =
            new LabeledFilterViewContributionItem(m_viewer,
                    new ExplorerFilter(), false);
        toolBarMgr.add(m_toolbarFilterCombo);
        Action configure = new ConfigureExplorerViewAction(this,
                m_contentDelegator);
        toolBarMgr.add(configure);
    }

    private boolean openSelected() {
        IStructuredSelection selection =
                (IStructuredSelection)m_viewer.getSelection();
        // for now we open only local files
        LinkedList<LocalFile> selFiles = new LinkedList<LocalFile>();
        if (selection.isEmpty()) {
            return false;
        }
        List<ContentObject> refreshs = new ArrayList<ContentObject>();
        Iterator<?> iter = selection.iterator();
        while (iter.hasNext()) {
            Object sel = iter.next();
            if (sel instanceof ContentObject) {
                try {
                    ContentObject co = (ContentObject)sel;
                    File f = co.getObject().toLocalFile(EFS.NONE, null);
                    if (f != null) {
                        selFiles.add(new LocalFile(f));
                        refreshs.add(co);
                    }
                } catch (CoreException ce) {
                    // then don't add it
                }
            }
        }

        if (selFiles.size() > 5) {
            MessageBox mb =
                    new MessageBox(getViewSite().getShell(), SWT.ICON_QUESTION
                            | SWT.CANCEL | SWT.OK);
            mb.setText("Confirm creation of multiple edtiors");
            mb.setMessage("Are you sure you want to open " + selection.size()
                    + " editor windows?");
            if (mb.open() != SWT.OK) {
                return false;
            }
        }
        boolean opened = false;
        for (LocalFile lf : selFiles) {
            opened |= openEditor(lf);
        }
        m_viewer.update(refreshs.toArray(), null);
        return opened;
    }

    private boolean openEditor(final LocalFile lf) {
        if (lf.fetchInfo().isDirectory()) {
            IFileStore wf = lf.getChild(WorkflowPersistor.WORKFLOW_FILE);
            if (wf.fetchInfo().exists()) {
                return openWorkflow(lf);
            } else {
                // we open no other than workflow directories
                return false;
            }
        } else {
            try {
                PlatformUI
                        .getWorkbench()
                        .getActiveWorkbenchWindow()
                        .getActivePage()
                        .openEditor(new FileStoreEditorInput(lf),
                                "org.eclipse.ui.DefaultTextEditor");
                return true;
            } catch (PartInitException e) {
                LOGGER.warn("Unable to open editor for " + lf.getName() + ": "
                        + e.getMessage(), e);
                return false;
            }
        }
    }

    private boolean openWorkflow(final LocalFile wfDirectory) {
        String wfName = new Path(wfDirectory.getName()).lastSegment();
        try {
            LocalFile wfFile =
                    (LocalFile)wfDirectory
                            .getChild(WorkflowPersistor.WORKFLOW_FILE);
            IDE.openEditorOnFileStore(PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow().getActivePage(), wfFile);
            return true;
        } catch (PartInitException e) {
            LOGGER.warn(
                    "Unable to open editor for " + wfName + ": "
                            + e.getMessage(), e);
            return false;
        }
    }

    private void expandSelected() {
        IStructuredSelection selection =
                (IStructuredSelection)m_viewer.getSelection();
        @SuppressWarnings("unchecked")
        Iterator<Object> iter = selection.iterator();
        while (iter.hasNext()) {
            m_viewer.expandToLevel(iter.next(), 1);
        }
    }

    private void createTreeViewer(final Composite parent,
            final ContentDelegator provider) {
        m_viewer =
                new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL
                        | SWT.BORDER);
        m_viewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
        m_viewer.setComparator(new ExplorerViewComparator());
        m_viewer.setContentProvider(provider);
        m_viewer.setLabelProvider(provider);
        m_viewer.setInput(provider); // the provider is also the root!
        m_viewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(final DoubleClickEvent event) {
                if (!openSelected()) {
                    expandSelected();
                }
            }
        });
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
                    AbstractExplorerFileStore fs =
                            m_nextSelection.getAndSet(null);
                    if (fs != null) {
                        Object treeObj = ContentDelegator.getTreeObjectFor(fs);
                        if (treeObj != null) {
                            m_viewer.setSelection(new StructuredSelection(
                                    treeObj), true);
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
        List<AbstractExplorerFileStore> fs = DragAndDropUtils
            .getExplorerFileStores((TreeSelection) m_viewer.getSelection());
        if ((fs != null) && (fs.size() == 1)
                && (fs.get(0) instanceof MessageFileStore)) {
            return;
        }

        addGlobalActions(manager, fs);

        IStructuredSelection sel =
                (IStructuredSelection)m_viewer.getSelection();
        Map<AbstractContentProvider, List<AbstractExplorerFileStore>> selFiles =
                DragAndDropUtils.getProviderMap(sel);

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

    private void addGlobalActions(final IMenuManager manager,
            final List<AbstractExplorerFileStore> fs) {
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
        manager.add(new GlobalCredentialVariablesDialogAction(this));
        manager.add(new GlobalOpenWorkflowVariablesDialogAction(this));
        manager.add(new Separator());
        manager.add(new GlobalEditMetaInfoAction(this));
        manager.add(new Separator());
        manager.add(new GlobalCopyAction(this));
        manager.add(new GlobalMoveAction(this));
        manager.add(new Separator());
        manager.add(new CopyURLAction(this, m_clipboard));
        manager.add(new CopyLocationAction(this, m_clipboard));
        manager.add(new Separator());
        if (fs != null && !fs.isEmpty()) {
            manager.add(new GlobalRefreshAction(this,
                    fs.toArray(new AbstractExplorerFileStore[0])));
        }
    }

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
        if (memento != null) {
            // restore visible mount points
            IMemento content = memento.getChild("content");
            if (content != null) {
                m_contentDelegator.restoreState(content);
            } else {
                LOGGER.warn("Corrupted User Resource View state storage. "
                        + "Can't restore state of view.");
            }
        } else { // freshly opened view
            // add all mount points that are mounted by default
            List<String> mountIDs = ExplorerMountTable.getAllMountIDs();
            for (String id : mountIDs) {
                MountPoint mountPoint = ExplorerMountTable.getMountPoint(id);
                String defaultID =
                    mountPoint.getProviderFactory().getDefaultMountID();
                if (id.equals(defaultID)) {
                    LOGGER.debug("Adding default mount point '" + id
                            + "' to explorer view");
                    m_contentDelegator.addMountPoint(mountPoint);
                }
            }
        }
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

}
