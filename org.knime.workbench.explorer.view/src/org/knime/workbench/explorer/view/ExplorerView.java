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
import java.util.List;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.filesystem.local.LocalFileSystem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.util.LocalSelectionTransfer;
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
import org.knime.core.node.workflow.JobManagerChangedEvent;
import org.knime.core.node.workflow.JobManagerChangedListener;
import org.knime.core.node.workflow.NodeMessageEvent;
import org.knime.core.node.workflow.NodeMessageListener;
import org.knime.core.node.workflow.NodeStateChangeListener;
import org.knime.core.node.workflow.NodeStateEvent;
import org.knime.core.node.workflow.WorkflowEvent;
import org.knime.core.node.workflow.WorkflowListener;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.explorer.MountPoint;
import org.knime.workbench.explorer.view.dialogs.SelectMountPointDialog;
import org.knime.workbench.ui.navigator.ProjectWorkflowMap;

/**
 *
 * @author Peter Ohl, KNIME.com, Zurich, Switzerland
 */
public class ExplorerView extends ViewPart implements WorkflowListener,
        NodeStateChangeListener, NodeMessageListener, JobManagerChangedListener {

    /** The ID of the view as specified by the extension. */
    public static final String ID = "com.knime.workbench.userspace.view";

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(ExplorerView.class);

    private TreeViewer m_viewer;

    private ContentDelegator m_contentDelegator;

    private DrillDownAdapter m_drillDownAdapter;

    private FileDragListener m_dragListener;

    private FileDropListener m_dropListener;

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

        createButtons(overall);
        m_contentDelegator = new ContentDelegator();
        createTreeViewer(overall, m_contentDelegator);
        assert m_viewer != null; // should be set by createTreeViewer
        // needed by the toolbar and the menus
        m_drillDownAdapter = new DrillDownAdapter(m_viewer);
        m_dragListener = new FileDragListener(m_viewer);
        m_dropListener = new FileDropListener(m_viewer);
        initDragAndDrop();
        makeGlobalActions();
        createLocalToolBar();
        hookContextMenu();
    }

    private void initDragAndDrop() {
        LocalSelectionTransfer selectionTransfer =
                LocalSelectionTransfer.getTransfer();
        FileTransfer fileTransfer = FileTransfer.getInstance();
        final int operation = DND.DROP_MOVE | DND.DROP_COPY;
        m_viewer.addDragSupport(operation, new Transfer[]{selectionTransfer},
                m_dragListener);
        m_viewer.addDropSupport(operation, new Transfer[]{selectionTransfer,
                fileTransfer}, m_dropListener);
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

    private void openSelected() {
        IStructuredSelection selection =
                (IStructuredSelection)m_viewer.getSelection();
        if (!selection.isEmpty()
                && (selection.getFirstElement() instanceof ContentObject)) {
            ContentObject co = (ContentObject)selection.getFirstElement();
            if (co.getObject() instanceof IFileStore) {
                openEditor((IFileStore)co.getObject());
            }
            if (co.getObject() instanceof File) {
                openEditor(LocalFileSystem.getInstance().fromLocalFile(
                        (File)co.getObject()));
            }
        }
    }

    private void openEditor(final IFileStore fs) {
        if (fs.fetchInfo().isDirectory()) {
            IFileStore wf = fs.getChild(WorkflowPersistor.WORKFLOW_FILE);
            if (wf.fetchInfo().exists()) {
                try {
                    IDE.openEditorOnFileStore(PlatformUI.getWorkbench()
                            .getActiveWorkbenchWindow().getActivePage(), wf);
                    return;
                } catch (PartInitException e) {
                    LOGGER.warn("Unable to open editor for " + fs.getName()
                            + ": " + e.getMessage(), e);
                }
            }
            LOGGER.warn("Can't open directories: " + fs.getName());
        } else {
            try {
                PlatformUI
                        .getWorkbench()
                        .getActiveWorkbenchWindow()
                        .getActivePage()
                        .openEditor(new FileStoreEditorInput(fs),
                                "org.eclipse.ui.DefaultTextEditor");
                return;
            } catch (PartInitException e) {
                LOGGER.warn("Unable to open editor for " + fs.getName() + ": "
                        + e.getMessage(), e);
            }
        }
    }

    private void addNewItemToView() {
        SelectMountPointDialog selectDlg =
                new SelectMountPointDialog(m_viewer.getControl().getShell());
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
        ProjectWorkflowMap.addWorkflowListener(this);
        ProjectWorkflowMap.addStateListener(this);
        ProjectWorkflowMap.addNodeMessageListener(this);
        ProjectWorkflowMap.addJobManagerChangedListener(this);

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
    public void jobManagerChanged(final JobManagerChangedEvent e) {
        refreshAsync();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void messageChanged(final NodeMessageEvent messageEvent) {
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
                assert m_drillDownAdapter != null; // set in createPartControl
                ExplorerView.this.fillContextMenu(manager, m_drillDownAdapter);
            }
        });
        Menu menu = menuMgr.createContextMenu(m_viewer.getControl());
        m_viewer.getControl().setMenu(menu);

        // This allows other plug-ins to add to our menu
        getSite().registerContextMenu(menuMgr, m_viewer);
    }

    private void fillContextMenu(final IMenuManager manager,
            final DrillDownAdapter dda) {

        // TODO: Add global actions

        // TODO: loop through contributors

        manager.add(new Separator());

        if (dda != null) {
            dda.addNavigationActions(manager);
        }

        // Other plug-ins can contribute there actions here
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

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
        m_contentDelegator.saveState(memento.getChild("content"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final IViewSite site, final IMemento memento)
            throws PartInitException {
        super.init(site, memento);
        if (memento != null) {
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

        m_contentDelegator.dispose();

    }

}
