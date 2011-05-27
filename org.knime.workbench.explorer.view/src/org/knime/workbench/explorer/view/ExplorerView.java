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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.filesystem.local.LocalFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
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
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeMessageEvent;
import org.knime.core.node.workflow.NodeMessageListener;
import org.knime.core.node.workflow.NodePropertyChangedEvent;
import org.knime.core.node.workflow.NodePropertyChangedListener;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.node.workflow.WorkflowEvent;
import org.knime.core.node.workflow.WorkflowListener;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.core.WorkflowManagerTransfer;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.MountPoint;
import org.knime.workbench.explorer.filesystem.ExplorerFileStore;
import org.knime.workbench.explorer.view.actions.GlobalDeleteAction;
import org.knime.workbench.explorer.view.actions.GlobalOpenMetaInfoDialogAction;
import org.knime.workbench.explorer.view.actions.GlobalRenameAction;
import org.knime.workbench.explorer.view.actions.NewWorkflowAction;
import org.knime.workbench.explorer.view.dialogs.SelectMountPointDialog;
import org.knime.workbench.explorer.view.dnd.DragAndDropUtils;
import org.knime.workbench.explorer.view.dnd.ExplorerDragListener;
import org.knime.workbench.explorer.view.dnd.ExplorerDropListener;
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

    private TreeViewer m_viewer;

    private final ContentDelegator m_contentDelegator = new ContentDelegator();

    private ExplorerDragListener m_dragListener;

    private ExplorerDropListener m_dropListener;

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
        createButtons(overall);
        createTreeViewer(overall, m_contentDelegator);
        assert m_viewer != null; // should be set by createTreeViewer
        // needed by the toolbar and the menus
        m_dragListener = new ExplorerDragListener(m_viewer);
        m_dropListener = new ExplorerDropListener(m_viewer);
        initDragAndDrop();
        makeGlobalActions();
        createLocalToolBar();
        hookContextMenu();
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

    }

    private void createButtons(final Composite parent) {
        Composite panel = new Composite(parent, SWT.NONE);
        panel.setLayout(new GridLayout(3, false));
        panel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button newItem = new Button(panel, SWT.PUSH);
        newItem.setText("Manage Content...");
        newItem.setToolTipText("Mount/Unmount resources.");
        newItem.addSelectionListener(new SelectionListener() {
            /** {@inheritDoc} */
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }

            /** {@inheritDoc} */
            @Override
            public void widgetSelected(final SelectionEvent e) {
                addNewItemToView();
            }
        });
        Button refresh = new Button(panel, SWT.PUSH);
        refresh.setText("Refresh");
        refresh.addSelectionListener(new SelectionListener() {
            /** {@inheritDoc} */
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }

            /** {@inheritDoc} */
            @Override
            public void widgetSelected(final SelectionEvent e) {
                m_viewer.refresh();
            }
        });
        Button open = new Button(panel, SWT.PUSH);
        open.setText("Open");
        open.addSelectionListener(new SelectionListener() {
            /** {@inheritDoc} */
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }

            /** {@inheritDoc} */
            @Override
            public void widgetSelected(final SelectionEvent e) {
                openSelected();
            }
        });
    }

    private boolean openSelected() {
        IStructuredSelection selection =
                (IStructuredSelection)m_viewer.getSelection();
        // for now we open only local files
        LinkedList<LocalFile> selFiles = new LinkedList<LocalFile>();
        if (selection.isEmpty()) {
            return false;
        }
        @SuppressWarnings("unchecked")
        Iterator<Object> iter = selection.iterator();
        while (iter.hasNext()) {
            Object sel = iter.next();
            if (sel instanceof ContentObject) {
                try {
                    ContentObject co = (ContentObject)sel;
                    File f = co.getObject().toLocalFile(EFS.NONE, null);
                    if (f != null) {
                        selFiles.add(new LocalFile(f));
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

    private void addNewItemToView() {
        SelectMountPointDialog selectDlg =
                new SelectMountPointDialog(m_viewer.getControl().getShell(),
                        m_contentDelegator.getMountedIds());
        selectDlg.setBlockOnOpen(true);
        if (selectDlg.open() != InputDialog.OK) {
            return;
        }
        m_contentDelegator.removeAllMountPoints();
        List<MountPoint> result = selectDlg.getResult();
        for (MountPoint mp : result) {
            m_contentDelegator.addMountPoint(mp);
        }
        m_viewer.refresh();

    }

    private void createTreeViewer(final Composite parent,
            final ContentDelegator provider) {
        m_viewer =
                new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL
                        | SWT.BORDER);
        m_viewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
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
        ProjectWorkflowMap.addWorkflowListener(this);
        ProjectWorkflowMap.addStateListener(this);
        ProjectWorkflowMap.addNodeMessageListener(this);
        ProjectWorkflowMap.addNodePropertyChangedListener(this);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void workflowChanged(final WorkflowEvent event) {
        refreshAsync();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stateChanged(final NodeStateEvent state) {
        refreshAsync();
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
        refreshAsync();
    }

    private void refreshAsync() {
        // TODO only call on possibly affected branches in the tree
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (m_viewer != null && !m_viewer.getControl().isDisposed()) {
                    m_viewer.refresh();
                }
            }
        });
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

        addGlobalActions(manager);

        IStructuredSelection sel =
                (IStructuredSelection)m_viewer.getSelection();
        Map<AbstractContentProvider, List<ExplorerFileStore>> selFiles =
                DragAndDropUtils.getProviderMap(sel);

        // all visible spaces may contribute to the menu
        Set<String> ids = m_contentDelegator.getMountedIds();
        for (String id : ids) {
            MountPoint mp = ExplorerMountTable.getMountPoint(id);
            if (mp == null) {
                // gone...
                continue;
            }

            mp.getProvider().addContextMenuActions(m_viewer, manager, selFiles);
        }

        manager.add(new Separator());
        // Other plug-ins can contribute there actions here
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

    }

    private void addGlobalActions(final IMenuManager manager) {
        manager.add(new NewWorkflowAction(m_viewer));
        manager.add(new GlobalDeleteAction(m_viewer));
        manager.add(new GlobalRenameAction(m_viewer));
        manager.add(new GlobalOpenMetaInfoDialogAction(m_viewer));
    }

    private void createLocalToolBar(final DrillDownAdapter dda) {
        IToolBarManager toolBarMgr =
                getViewSite().getActionBars().getToolBarManager();

        // m_toolbarFilterCombo = new ViewFilterItem(m_viewer);
        // toolBarMgr.add(m_toolbarFilterCombo);
        // toolBarMgr.add(new Separator());
        // Action collAll = new CollapseAllAction(m_viewer);
        // collAll.setToolTipText("Collapses the entire tree");
        // collAll.setImageDescriptor(IMG_COLLALL);
        // toolBarMgr.add(collAll);
        // Action coll = new CollapseAction(m_viewer);
        // coll.setToolTipText("Collapses the selected element");
        // coll.setImageDescriptor(IMG_COLL);
        // toolBarMgr.add(coll);
        // Action exp = new ExpandAction(m_viewer);
        // exp.setToolTipText("Expands fully the selected element");
        // exp.setImageDescriptor(IMG_EXP);
        // toolBarMgr.add(exp);
        toolBarMgr.add(new Separator());
        // add drill down actions to local tool bar
        dda.addNavigationActions(toolBarMgr);
        toolBarMgr.add(new Separator());
        // Action info = new ShowInfoAction(m_viewer);
        // info.setToolTipText("Shows server version and info");
        // info.setImageDescriptor(IMG_INFO);
        // info.setEnabled(true);
        // toolBarMgr.add(info);
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    @Override
    public void setFocus() {
        m_viewer.getControl().setFocus();
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
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        m_contentDelegator.removePropertyChangeListener(this);
        m_contentDelegator.dispose();

    }

}
