/*
 * ------------------------------------------------------------------------
 *
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
 *   Feb 12, 2016 (hornm): created
 */
package org.knime.workbench.workflowcoach.ui;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeFactory.NodeType;
import org.knime.core.node.NodeInfo;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.ui.node.workflow.NativeNodeContainerUI;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.util.NodeTemplateId;
import org.knime.core.ui.workflowcoach.NodeRecommendationManager;
import org.knime.core.ui.workflowcoach.NodeRecommendationManager.IUpdateListener;
import org.knime.core.ui.workflowcoach.NodeRecommendationManager.NodeRecommendation;
import org.knime.core.ui.workflowcoach.data.NodeTripleProvider;
import org.knime.core.ui.workflowcoach.data.UpdatableNodeTripleProvider;
import org.knime.core.util.KNIMEJob;
import org.knime.core.util.Pair;
import org.knime.workbench.core.KNIMECorePlugin;
import org.knime.workbench.core.nodeprovider.NodeProvider;
import org.knime.workbench.core.preferences.HeadlessPreferencesConstants;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.repository.RepositoryManager;
import org.knime.workbench.repository.model.NodeTemplate;
import org.knime.workbench.workflowcoach.data.CommunityTripleProvider;
import org.knime.workbench.workflowcoach.prefs.UpdateJob;
import org.knime.workbench.workflowcoach.prefs.UpdateJob.UpdateListener;
import org.knime.workbench.workflowcoach.prefs.WorkflowCoachPreferenceInitializer;
import org.osgi.framework.FrameworkUtil;

/**
 * Workflow coach view that displays a table of recommended nodes, e.g. for the currently selected node.
 *
 * @author Martin Horn, University of Konstanz
 */
public class WorkflowCoachView extends ViewPart implements ISelectionListener, IUpdateListener {
    private static final ScopedPreferenceStore PREFS = new ScopedPreferenceStore(InstanceScope.INSTANCE,
        FrameworkUtil.getBundle(CommunityTripleProvider.class).getSymbolicName());

    private static final String NO_WORKFLOW_OPENED_MESSAGE = "No workflow opened.";

    private static final String NO_RECOMMENDATIONS_AVAILABLE_MESSAGE = "No node recommendations available."
        + (Platform.getOS().equals(Platform.OS_MACOSX) ? " " : "\n") + "Click here to configure ...";

    private static final String NO_DATA_REPORTING_MESSAGE =
        "Node recommendations only available with usage data reporting."
            + (Platform.getOS().equals(Platform.OS_MACOSX) ? " " : "\n") + "Click here to configure ...";

    private static final String LOADING_MESSAGE = "Loading recommendations...";

    private static final int DEFAULT_FIRST_COLUMN_WIDTH = 200;
    private static final int DEFAULT_OTHER_COLUMNS_WIDTH = 100;

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowCoachView.class);


    /** Whether nodes are being loaded, loaded, being updated, disposed, etc. */
    private AtomicReference<LoadState> m_loadState = new AtomicReference<>(LoadState.LOADING_NODES);

    /**
     * Indicates whether recommendations are available (i.e. properly configured etc.).
     */
    private boolean m_recommendationsAvailable = false;

    /**
     * The table with the recommendation or a message.
     */
    private TableViewer m_viewer;
    private TableColumnLayout m_tableLayout;

    /**
     * Current state of the viewer.
     */
    private ViewerState m_viewerState = null;

    /**
     * A string describing the last selection (e.g. a node or no selection), in order to not unneccessarily retrieve and
     * repaint the node recommendations.
     */
    private String m_lastSelection = "";

    /**
     * Possible states of the table viewer.
     */
    private enum ViewerState {
        /** normal text, one column, no headers */
        MESSAGE,
        /** normal text, n columns, with headers */
        RECOMMENDATIONS,
        /** text as link, one column, no headers, hand mouse cursor, mouse listener */
        LINK;
    }

    /** Load state of the view, added to address AP-6822 (deadlocks when disposing while loading repository). */
    private enum LoadState {
        /** While picking up repository content. */
        LOADING_NODES,
        /** 'normal' operation. */
        INITIALIZED,
        /** during dispose or after dispose. */
        DISPOSED
    }

    private MouseListener m_openPrefPageMouseListener = new MouseAdapter() {
        @Override
        public void mouseUp(final MouseEvent e) {
            new ConfigureAction(m_viewer).run();
        }
    };

    /**
     * Names and tool tips of the column headers of the recommendation table.
     */
    private List<Pair<String, String>> m_namesAndToolTips = Collections.emptyList();

    /**
     * {@inheritDoc}
     */
    @Override
    public void createPartControl(final Composite parent) {
        m_viewer = new TableViewer(parent, SWT.V_SCROLL | SWT.FULL_SELECTION) {
            @Override
            public ISelection getSelection() {
                ISelection sel = super.getSelection();
                if (!sel.isEmpty() && sel instanceof IStructuredSelection) {
                    IStructuredSelection ss = (IStructuredSelection)sel;

                    if (ss.getFirstElement() instanceof NodeRecommendation[]) {
                        // Turn node recommendation selection into a node template selection
                        NodeRecommendation[] nodeRecommendations = (NodeRecommendation[])ss.getFirstElement();
                        var nodeTemplate = getNodeTemplateFromNodeRecommendations(nodeRecommendations);
                        return new StructuredSelection(new Object[]{nodeTemplate});
                    }
                }
                return sel;
            }
        };
        getSite().setSelectionProvider(m_viewer);
        m_viewer.setComparator(new TableColumnSorter(m_viewer));
        var table = m_viewer.getTable();

        m_tableLayout = new TableColumnLayout();
        table.getParent().setLayout(m_tableLayout);

        //drag & drop
        var transfers = new Transfer[]{LocalSelectionTransfer.getTransfer()};
        m_viewer.addDragSupport(DND.DROP_COPY | DND.DROP_MOVE, transfers, new WorkflowCoachDragSource(this));

        //column configuration
        var column = new TableColumn(table, SWT.LEFT, 0);
        column.setText("Recommended Nodes");
        column.setToolTipText("Nodes recommended to use next (e.g. based on the currently selected node).");
        column.setWidth(DEFAULT_FIRST_COLUMN_WIDTH);

        m_tableLayout.setColumnData(column, new ColumnWeightData(100, DEFAULT_FIRST_COLUMN_WIDTH));

        table.setHeaderVisible(true);
        table.setLinesVisible(false);

        m_viewer.setContentProvider(new WorkflowCoachContentProvider());
        m_viewer.setLabelProvider(new WorkflowCoachLabelProvider());

        m_viewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));

        getViewSite().getPage().addSelectionListener(this);
        hookDoubleClickAction();

        //actions
        IToolBarManager toolbarMGR = getViewSite().getActionBars().getToolBarManager();
        toolbarMGR.add(new ConfigureAction(m_viewer));

        updateInput("Waiting for node repository to be loaded ...");
        m_loadState.set(LoadState.LOADING_NODES);
        Job nodesLoader = createWorkflowCoachLoader();
        nodesLoader.setSystem(true);
        nodesLoader.schedule();

        //if the 'send anonymous statistics'-property has been changed, try updating the workflow coach
        KNIMECorePlugin.getDefault().getPreferenceStore().addPropertyChangeListener(e -> {
            if (e.getProperty().equals(HeadlessPreferencesConstants.P_SEND_ANONYMOUS_STATISTICS)
                && e.getNewValue().equals(Boolean.TRUE)) {
                //enable the community recommendations
                PREFS.setValue(WorkflowCoachPreferenceInitializer.P_COMMUNITY_NODE_TRIPLE_PROVIDER, true);
                try {
                    PREFS.save();
                } catch (Exception e1) {
                    throw new RuntimeException(e1);
                }
                updateInput(StructuredSelection.EMPTY);
            }
        });
    }

    /**
     * Initializes the {@link NodeRecommendationManager} building the predicates needed
     *
     */
    private static void initializeNodeRecommendationManager() {
        Predicate<NodeInfo> isSourceNode = nodeInfo -> {
            var nt = getNodeTemplate(nodeInfo);
            try {
                return nt != null && nt.getType() == NodeType.Source;
            } catch (Exception e) {
                LOGGER.warn(String.format("Could not create a factory instance for <%s>", nodeInfo), e);
                return false;
            }
        };
        Predicate<NodeInfo> existsInRepository = nodeInfo -> getNodeTemplate(nodeInfo) != null;
        NodeRecommendationManager.getInstance().initialize(isSourceNode, existsInRepository);
    }

    /**
     * @return A {@link KNIMEJob} loading the workflow coach
     */
    private Job createWorkflowCoachLoader() {
        return new KNIMEJob("Workflow Coach loader", FrameworkUtil.getBundle(getClass())) { // NOSONAR: Legacy code
            @Override
            protected IStatus run(final IProgressMonitor monitor) {
                RepositoryManager.INSTANCE.getRoot(); // wait until the repository is fully loaded
                if (m_loadState.get() == LoadState.DISPOSED) {
                    return Status.CANCEL_STATUS;
                } else if (monitor.isCanceled()) {
                    m_loadState.set(LoadState.INITIALIZED);
                    return Status.CANCEL_STATUS;
                } else {
                    // check for update if necessary
                    updateInput(LOADING_MESSAGE);
                    checkForStatisticUpdates();
                }
                if (m_loadState.get() != LoadState.DISPOSED) {
                    // Prevent state transition if already disposed. In that case, the Part can no longer be used.
                    m_loadState.set(LoadState.INITIALIZED);
                }
                initializeNodeRecommendationManager();
                NodeRecommendationManager.getInstance().addUpdateListener(WorkflowCoachView.this);
                updateFrequencyColumnHeadersAndToolTips();
                updateInput(StructuredSelection.EMPTY);
                return Status.OK_STATUS;
            }
        };
    }

    /**
     * @param nodeInfo The node info object to return a node template for
     * @return A node template or <code>null</code>
     */
    private static NodeTemplate getNodeTemplate(final NodeInfo nodeInfo) {
        return NodeTemplateId.callWithNodeTemplateIdVariants(nodeInfo.getFactory(), nodeInfo.getName(),
            RepositoryManager.INSTANCE::getNodeTemplate);
    }

    static NodeTemplate getNodeTemplateFromNodeRecommendations(final NodeRecommendation[] nodeRecommendations) {
        var nodeRecommendation = getNonNullEntry(nodeRecommendations);
        var nodeTemplate = NodeTemplateId.callWithNodeTemplateIdVariants(nodeRecommendation.getNodeFactoryClassName(),
            nodeRecommendation.getNodeName(), RepositoryManager.INSTANCE::getNodeTemplate);
        if (nodeTemplate == null) {
            LOGGER.debug(String.format("Could not find node template for <%s>", nodeRecommendation));
        }
        return nodeTemplate;
    }

    private static final <T> T getNonNullEntry(final T[] arr) {
        return arr[getNonNullIdx(arr)];
    }

    private static final <T> int getNonNullIdx(final T[] arr) {
        for (var i = 0; i < arr.length; i++) {
            if (arr[i] != null) {
                return i;
            }
        }
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setFocus() {
        m_viewer.getControl().setFocus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        //unregister selection listener, dispose objects etc.
        if (m_loadState.get() != LoadState.LOADING_NODES) {
            NodeRecommendationManager.getInstance().removeUpdateListener(this);
        }
        m_loadState.set(LoadState.DISPOSED);
        this.getSite().setSelectionProvider(null);
        getViewSite().getPage().removeSelectionListener(this);
        m_viewer.getTable().dispose();
        m_viewer = null;
        super.dispose();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
        if (m_recommendationsAvailable
            && PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences().length == 0) {
            //if no workflow is opened and the workflow coach is configured properly, show according message
            updateInput(NO_WORKFLOW_OPENED_MESSAGE);
        }
        var loadState = m_loadState.get();
        if (part instanceof WorkflowCoachView || loadState == LoadState.LOADING_NODES) {
            // If source of the selection is this view itself, or the nodes or statistics are still loading, do nothing
            return;
        }
        if (!(selection instanceof IStructuredSelection) || ((part != null) && !(part instanceof WorkflowEditor))) {
            // If the selection event comes from another view than the workbench, do nothing
            return;
        }

        updateInput(selection);
    }

    private void updateInput(final ISelection selection) {
        if (NodeRecommendationManager.getNumLoadedProviders() == 0) {
            //if there is at least one enabled triple provider then the statistics might need to be download first
            if (NodeRecommendationManager.getNodeTripleProviders().stream()
                .anyMatch(NodeTripleProvider::isEnabled)) {

                if (m_loadState.get() == LoadState.DISPOSED) {
                    return;
                }

                m_loadState.set(LoadState.LOADING_NODES);
                updateInput(LOADING_MESSAGE);

                //try updating the triple provider that are enabled and require an update
                updateTripleProviders(e -> {
                    m_loadState.set(LoadState.INITIALIZED);
                    if (e.isPresent()) {
                        updateInputNoProvider();
                    } else {
                        try {
                            NodeRecommendationManager.getInstance().loadRecommendations();
                            if (NodeRecommendationManager.getNumLoadedProviders() == 0) {
                                //if there are still no triple provider, show link
                                updateInputNoProvider();
                            } else {
                                updateInput("Statistics successfully loaded. Select a node...");
                            }
                        } catch (Exception e1) {
                            updateInputNoProvider();
                        }
                    }
                }, true, false);
            } else {
                //no triple provider enabled -> needs to be configured
                updateInputNoProvider();
                return;
            }
        }
        IStructuredSelection structSel = (IStructuredSelection)selection;

        if (structSel.size() > 1) {
            updateInput("No recommendation for multiple selected nodes.");
            return;
        }

        // retrieve first (and only!) selection:
        Iterator<?> selIt = structSel.iterator();

        boolean nodeSelected = selIt.hasNext();
        NodeContainerUI nc = null;
        if (nodeSelected) {
            Object sel = selIt.next();
            nodeSelected &= (sel instanceof NodeContainerEditPart);
            if (nodeSelected) {
                nc = ((NodeContainerEditPart)sel).getNodeContainer();
                nodeSelected &= nc instanceof NativeNodeContainerUI;
            }
        }

        //check whether it's just the same selection as the previous one (e.g. when a node has been reset etc.)
        //-> in that case no redraw is required
        if (nodeSelected && nc != null) {
            if (m_lastSelection.equals(nc.getNameWithID())) {
                return;
            } else {
                m_lastSelection = nc.getNameWithID();
            }
        } else {
            if (m_lastSelection.equals("no node selected")) {
                return;
            } else {
                m_lastSelection = "no node selected";
            }
        }


        List<NodeRecommendation>[] recommendations;
        if (nodeSelected) {
            //retrieve node recommendations if exactly one node is selected
            recommendations = NodeRecommendationManager.getInstance().getNodeRecommendationFor((NativeNodeContainerUI)nc);
        } else if (nc == null) {
            //retrieve node recommendations if no node is selected (most likely the source nodes etc.)
            recommendations = NodeRecommendationManager.getInstance().getNodeRecommendationFor();
        } else {
            Display.getDefault().syncExec(() -> {
                if (m_loadState.get() == LoadState.DISPOSED) {
                    return;
                }
                m_viewer.setInput("");
                m_viewer.refresh();
            });
            return;
        }

        if (recommendations == null) {
            //something went wrong with loading the node recommendations, show the configure link
            updateInputNoProvider();
            return;
        }

        //TODO: cache node recommendations??
        var recommendationsWithoutDups =
            NodeRecommendationManager.joinRecommendationsWithoutDuplications(recommendations);

        //update viewer
        changeViewerStateTo(ViewerState.RECOMMENDATIONS);
        Display.getDefault().syncExec(() -> {
            if (m_loadState.get() != LoadState.DISPOSED) {
                m_viewer.setInput(recommendationsWithoutDups);
                m_viewer.refresh();
                m_recommendationsAvailable = true;
                //scroll to the very top
                if (!recommendationsWithoutDups.isEmpty()) {
                    m_viewer.getTable().setTopIndex(0);
                }
            }
        });
    }

    /**
     *
     * @return the selection of the underlying list
     */
    public ISelection getSelection() {
        return m_viewer.getSelection();
    }

    /**
     * Updates the names and tooltips of the frequency column headers.
     */
    private void updateFrequencyColumnHeadersAndToolTips() {
        if (m_loadState.get() == LoadState.DISPOSED) {
            return;
        }

        m_namesAndToolTips  =
            NodeRecommendationManager.getNodeTripleProviders().stream().filter(NodeTripleProvider::isEnabled)
                .map(p -> new Pair<>(p.getName(), p.getDescription())).collect(Collectors.toList());
        if (m_namesAndToolTips == null || m_namesAndToolTips.isEmpty()) {
            updateInputNoProvider();
            return;
        }

        //reset table sorter
        IElementComparer sorter = m_viewer.getComparer();
        if (sorter instanceof TableColumnSorter) {
            ((TableColumnSorter)sorter).setColumn(null);
        }

        //enforce to change the viewer state to update the headers
        m_viewerState = null;
        m_lastSelection = "";
        changeViewerStateTo(ViewerState.RECOMMENDATIONS);

        //get current selection from the workbench and update the recommendation list
        IEditorPart activeEditor = getViewSite().getPage().getActiveEditor();
        if (activeEditor == null) {
            //if no workflow is opened
            updateInput(NO_WORKFLOW_OPENED_MESSAGE);
        } else {
            IWorkbenchPartSite site = activeEditor.getSite();
            if (site != null) {
                ISelectionProvider selectionProvider = site.getSelectionProvider();
                if (selectionProvider != null) {
                    ISelection selection = selectionProvider.getSelection();
                    if (selection instanceof IStructuredSelection) {
                        updateInput(selection);
                        return;
                    }
                }
            }
            updateInput(StructuredSelection.EMPTY);
        }
    }

    /**
     * Helper set the input of the {@link TableViewer} whereas setting-process is run in a special display-thread.
     * Otherwise it causes some problems.
     *
     * @param o
     */
    private void updateInput(final String message) {
        changeViewerStateTo(ViewerState.MESSAGE);
        m_recommendationsAvailable = true;
        m_lastSelection = "";
        Display.getDefault().syncExec(() -> {
            if (m_viewer != null) {
                m_viewer.setInput(message);
            }
        });
    }

    /**
     * Updates the table viewer to complain about missing node triple providers or corrupt node statistics.
     */
    private void updateInputNoProvider() {
        changeViewerStateTo(ViewerState.LINK);
        m_recommendationsAvailable = false;
        Display.getDefault().syncExec(() -> {
            if (KNIMECorePlugin.getDefault().getPreferenceStore()
                .getBoolean(HeadlessPreferencesConstants.P_SEND_ANONYMOUS_STATISTICS)) {
                m_viewer.setInput(NO_RECOMMENDATIONS_AVAILABLE_MESSAGE);
            } else {
                m_viewer.setInput(NO_DATA_REPORTING_MESSAGE);
            }
        });
    }

    private void pruneTableColumns(final Table table, final boolean willAppendColumns) {
        while (table.getColumnCount() > 1) {
            table.getColumns()[1].dispose();
        }

        if (!willAppendColumns) {
            m_tableLayout.setColumnData(table.getColumns()[0], new ColumnWeightData(100, DEFAULT_FIRST_COLUMN_WIDTH));
        }
    }

    /**
     * Changes the state of the table viewer or leaves it unchanged (if the provided one is the same as the current one).
     *
     * @param state the state to change to
     */
    private void changeViewerStateTo(final ViewerState state) {
        if (m_viewerState == null || state != m_viewerState) {
            m_viewerState = state;
            Display.getDefault().syncExec(() -> {
                if (m_viewer == null) {
                    return; // already disposed
                }

                var table = m_viewer.getTable();
                table.setRedraw(false);
                switch (state) {
                    case MESSAGE:
                        table.removeMouseListener(m_openPrefPageMouseListener);
                        table.setHeaderVisible(false);
                        m_viewer.setLabelProvider(new WorkflowCoachLabelProvider());
                        pruneTableColumns(table, false);
                        table.setCursor(new Cursor(Display.getCurrent(), SWT.CURSOR_ARROW));
                        break;
                    case RECOMMENDATIONS:
                        final int tipsCount = m_namesAndToolTips.size();
                        final int otherColumnsWeighting = 60 / Math.max(tipsCount, 1);

                        table.removeMouseListener(m_openPrefPageMouseListener);
                        table.setHeaderVisible(true);
                        m_viewer.setLabelProvider(new WorkflowCoachLabelProvider());
                        pruneTableColumns(table, true);

                        for (var i = 0; i < m_namesAndToolTips.size(); i++) {
                            var column = new TableColumn(table, SWT.LEFT, i + 1);
                            column.setText(m_namesAndToolTips.get(i).getFirst());
                            column.setToolTipText(m_namesAndToolTips.get(i).getSecond());
                            column.addSelectionListener((TableColumnSorter) m_viewer.getComparator());
                        }

                        for (var i = 0; i <= m_namesAndToolTips.size(); i++) {
                            final int weight = (i == 0) ? 40 : otherColumnsWeighting;
                            final int width = (i == 0) ? DEFAULT_FIRST_COLUMN_WIDTH
                                                       : DEFAULT_OTHER_COLUMNS_WIDTH;

                            m_tableLayout.setColumnData(table.getColumns()[i],
                                                        new ColumnWeightData(weight, width, true));
                        }
                        table.setCursor(new Cursor(Display.getCurrent(), SWT.CURSOR_ARROW));
                        break;
                    case LINK:
                        table.addMouseListener(m_openPrefPageMouseListener);
                        table.setHeaderVisible(false);
                        pruneTableColumns(table, false);
                        m_viewer.setLabelProvider(new LinkStyleLabelProvider());
                        table.setCursor(new Cursor(Display.getCurrent(), SWT.CURSOR_HAND));
                }
                table.setRedraw(true);
            });
        }
    }

    /**
     * Inserts a node on double click into the workflow editor.
     */
    private void hookDoubleClickAction() {
        m_viewer.addDoubleClickListener(event -> {
            Object o = ((IStructuredSelection)event.getSelection()).getFirstElement();
            if (o instanceof NodeRecommendation[]) {
                NodeRecommendation[] nodeRecommendations = (NodeRecommendation[])o;
                var nodeTemplate = getNodeTemplateFromNodeRecommendations(nodeRecommendations);
                NodeFactory<? extends NodeModel> nodeFact;
                try {
                    nodeFact = nodeTemplate.createFactoryInstance();
                } catch (Exception e) {
                    NodeLogger.getLogger(WorkflowCoachView.class)
                        .error("Unable to instantiate the selected node " + nodeTemplate.getFactory().getName(), e);
                    return;
                }
                boolean added = NodeProvider.INSTANCE.addNode(nodeFact);
                if (added) {
                    Display.getDefault().asyncExec(this::setFocus);
                }
            }
        });
    }

    /**
     * Checks whether the update (i.e. download) of the node recommendation statistics is necessary, either because they
     * haven't been updated so far, or the update schedule tells to do so. If an update is necessary it is immediately
     * performed.
     */
    private static void checkForStatisticUpdates() {
        var updateSchedule = PREFS.getInt(WorkflowCoachPreferenceInitializer.P_AUTO_UPDATE_SCHEDULE);
        if (updateSchedule == WorkflowCoachPreferenceInitializer.NO_AUTO_UPDATE) {
            return;
        }

        Optional<LocalDateTime> oldest = NodeRecommendationManager.getNodeTripleProviders().stream()
            .map(NodeTripleProvider::getLastUpdate).filter(Optional::isPresent).map(Optional::get)
            .min(Comparator.naturalOrder());

        if (oldest.isPresent()) {
            //check whether an automatic update is necessary
            long weeksDiff = ChronoUnit.WEEKS.between(oldest.get(), LocalDateTime.now());
            if ((updateSchedule == WorkflowCoachPreferenceInitializer.WEEKLY_UPDATE && weeksDiff == 0)
                || (updateSchedule == WorkflowCoachPreferenceInitializer.MONTHLY_UPDATE && weeksDiff < 4)) {
                return;
            }
        }

        //trigger update for all updatable and enabled providers
        updateTripleProviders(e -> {
            if (e.isPresent()) {
                NodeLogger.getLogger(WorkflowCoachView.class).warn("Could not update node recommendations statistics.",
                    e.get());
            }
        }, false, true);
    }

    /**
     * Updates all updatable and enabled triple providers.
     *
     * @param requiredOnly if only the enabled triple providers should be updated that require an update in order to
     *            work
     * @param updateListener to get feedback of the updating process
     * @param block if <code>true</code> the method will block till the update is finished, otherwise it will return
     *            immediately after triggering the update job
     */
    private static void updateTripleProviders(final UpdateListener updateListener, final boolean requiredOnly, final boolean block) {
        List<UpdatableNodeTripleProvider> toUpdate =
            NodeRecommendationManager.getNodeTripleProviders().stream().filter(ntp -> {
                if (!(ntp instanceof UpdatableNodeTripleProvider)) {
                    return false;
                } else {
                    UpdatableNodeTripleProvider untp = (UpdatableNodeTripleProvider)ntp;
                    return ntp.isEnabled() && (!requiredOnly || untp.updateRequired());
                }
            }).map(UpdatableNodeTripleProvider.class::cast).collect(Collectors.toList());
        UpdateJob.schedule(updateListener, toUpdate, block);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updated() {
        updateFrequencyColumnHeadersAndToolTips();
        m_loadState.set(LoadState.INITIALIZED);
    }
}
