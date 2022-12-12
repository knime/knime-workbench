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
 *   17 Aug 2022 (leon.wenzler): created
 */
package org.knime.workbench.editor2.actions;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.TemplateUpdateUtil;
import org.knime.core.node.workflow.TemplateUpdateUtil.LinkType;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.core.util.Pair;
import org.knime.core.util.pathresolve.ResolverUtil;
import org.knime.core.util.pathresolve.SpaceVersion;
import org.knime.core.util.pathresolve.URIToFileResolve.KNIMEURIDescription;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * Dialog for selecting a space version for a component.
 *
 * Shows the metadata, e.g., author, date, and description for each version.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public final class ChangeComponentSpaceVersionDialog extends Dialog {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ChangeComponentSpaceVersionDialog.class);

    // ============ state ============ //
    private final WorkflowManager m_manager;

    /** The component to change version of. */
    private final SubNodeContainer m_component;

    private TemplateUpdateUtil.LinkType m_linkType;

    /**
     * The currently selected space version number. Initialized as the current space version of the Component to change.
     * Can be null to indicate latest version/state.
     */
    private Integer m_selectedSpaceVersion;


    /** Fetched using a {@link FetchVersionListJob} */
    private List<SpaceVersion> m_spaceVersions;

    // ============ view ============ //
    /** Shows the available hub space versions */
    private TableViewer m_tableViewer;

    private Button m_useLatestStateCheckBox;

    private Button m_useLatestVersionCheckBox;

    /** The strings that appear in the header column of the version table. */
    private static final List<String> COLUMN_NAMES = List.of("Version", "Name", "Author", "Created On");

    /** Initial column widths in pixels. */
    private static final List<Integer> COLUMN_WIDTHS = List.of(54, 213, 166, 152);

    // ============ async ============ //
    private FetchVersionListJob m_fetchVersionListJob;

    /**
     * Creates a dialog.
     *
     * @param parent the parent shell
     * @param currentSpaceVersion the hub space version to pre-select in the version list. null represents the latest
     *            version.
     * @param metaNodes components and metanodes in the currently edited workflow
     * @param manager The manager (used for resolution of 'knime://' URLs)
     */
    ChangeComponentSpaceVersionDialog(final Shell parent, final SubNodeContainer subNodeContainer,
        final Integer currentSpaceVersion, final WorkflowManager manager) {
        super(parent);
        m_manager = manager;
        m_component = subNodeContainer;
        m_selectedSpaceVersion = currentSpaceVersion;
    }

    @Override
    protected Control createDialogArea(final Composite parent) {
        var content = new Composite(parent, SWT.NONE);
        var fillBoth = new GridData(GridData.FILL_BOTH);
        content.setLayoutData(fillBoth);
        var gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        gridLayout.verticalSpacing = 8;
        gridLayout.marginWidth = 15;
        content.setLayout(gridLayout);

        // version list

        var selectorLabel = new Label(content, SWT.LEFT);
        selectorLabel.setText("Select a KNIME Hub Space Version");
        final Composite parent1 = content;
        // holds the table and defines its resizing behavior
        var tableComp = new Composite(parent1, SWT.NONE);
        var gridData = new GridData(GridData.FILL_BOTH);
        gridData.grabExcessHorizontalSpace = true;
        tableComp.setLayout(new FillLayout());
        tableComp.setLayoutData(gridData);

        m_tableViewer = createTableViewer(tableComp);
        m_tableViewer.addSelectionChangedListener(l -> {
            final var okButton = getButton(IDialogConstants.OK_ID);
            if (m_linkType == LinkType.FIXED_VERSION && okButton != null) {
                okButton.setEnabled(!l.getSelection().isEmpty());
            }
        });
        m_tableViewer.addDoubleClickListener(this::versionTableDoubleClicked);

        final var link = ChangeComponentSpaceVersionAction.spaceVersion(
            m_component.getTemplateInformation().getSourceURI());

        final var buttonGroup = new Composite(content, SWT.NONE);
        buttonGroup.setLayout(new RowLayout());

        final var specificVersion = new Button(buttonGroup, SWT.RADIO);
        specificVersion.setText("Use specific version");
        specificVersion.setSelection(link.getFirst() == LinkType.FIXED_VERSION);
        specificVersion.addListener(SWT.Selection, l -> syncEnableTableViewer());

        m_useLatestVersionCheckBox = new Button(buttonGroup, SWT.RADIO);
        m_useLatestVersionCheckBox.setText("Use latest version");
        m_useLatestVersionCheckBox.setSelection(link.getFirst() == LinkType.LATEST_VERSION);
        m_useLatestVersionCheckBox.addListener(SWT.Selection, l -> syncEnableTableViewer());

        m_useLatestStateCheckBox = new Button(buttonGroup, SWT.RADIO);
        m_useLatestStateCheckBox.setText("Use current state (unversioned)");
        m_useLatestStateCheckBox.setSelection(link.getFirst() == LinkType.LATEST_STATE);
        m_useLatestStateCheckBox.addListener(SWT.Selection, l -> syncEnableTableViewer());

        syncEnableTableViewer();
        scheduleFetchVersionListJob(content.getDisplay());
        return content;
    }

    private void syncEnableTableViewer() {
        if (m_useLatestStateCheckBox.getSelection()) {
            m_linkType = LinkType.LATEST_STATE;
        } else if (m_useLatestVersionCheckBox.getSelection()) {
            m_linkType = LinkType.LATEST_VERSION;
        } else {
            m_linkType = LinkType.FIXED_VERSION;
        }
        m_tableViewer.getTable().setEnabled(m_linkType == LinkType.FIXED_VERSION);
        final var okButton = getButton(IDialogConstants.OK_ID);
        if (okButton != null) {
            okButton.setEnabled(m_linkType != LinkType.FIXED_VERSION || !m_tableViewer.getSelection().isEmpty());
        }
    }

    /**
     * Upon double clicking a space version in the versions list, select the version and close the dialog.
     *
     * @param event
     */
    private void versionTableDoubleClicked(final DoubleClickEvent event) {
        var table = m_tableViewer.getTable();
        if (table.getSelectionIndex() != -1) {
            int index = table.getSelectionIndex();
            // the table is not sortable, the i-th row always corresponds to the i-th space version
            m_selectedSpaceVersion = m_spaceVersions.get(index).getVersion();
        }
        super.okPressed();
    }

    /**
     * @param parent
     * @return four columns: version number, title, author, and creation date
     */
    private static TableViewer createTableViewer(final Composite parent) {
        final var viewer =
            new TableViewer(parent, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);

        // create columns
        for (var i = 0; i < COLUMN_NAMES.size(); i++) {
            var column = new TableViewerColumn(viewer, SWT.NONE);
            column.getColumn().setText(COLUMN_NAMES.get(i));
            column.getColumn().setWidth(COLUMN_WIDTHS.get(i));
        }

        viewer.getTable().setHeaderVisible(true);
        viewer.getTable().setLinesVisible(true);

        viewer.setContentProvider(new ArrayContentProvider());
        viewer.setLabelProvider(new SpaceVersionLabelProvider());

        viewer.getTable().pack();
        viewer.refresh();

        return viewer;
    }

    /**
     * @return selected link space version
     */
    Pair<LinkType, Integer> getSelectedVersion() {
        return Pair.create(m_linkType, m_linkType == LinkType.FIXED_VERSION ? m_selectedSpaceVersion : null);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected void configureShell(final Shell shell) {
        super.configureShell(shell);
        shell.setSize(615, 400);
        shell.setText("Select KNIME Hub Space Version");
        var img = KNIMEUIPlugin.getDefault().getImageRegistry().get("knime");
        shell.setImage(img);
    }

    private static final class SpaceVersionLabelProvider extends LabelProvider implements ITableLabelProvider {

        /**
         * Gets the content of the n-th row in the versions table.
         * <ol>
         * <li>version number</li>
         * <li>short description</li>
         * <li>author name</li>
         * <li>date of creation</li>
         * </ol>
         */
        private static final List<Function<SpaceVersion, String>> COLUMN_VALUE_EXTRACTORS =
            List.of(e -> String.valueOf(e.getVersion()), SpaceVersion::getName, SpaceVersion::getAuthor,
                SpaceVersion::getCreatedOn);

        @Override
        public String getColumnText(final Object row, final int colIdx) {
            return COLUMN_VALUE_EXTRACTORS.get(colIdx).apply((SpaceVersion)row);
        }

        @Override
        public Image getColumnImage(final Object element, final int columnIndex) {
            // no image to show
            return null;
        }
    }

    @Override
    protected void okPressed() {
        versionTableDoubleClicked(null);
        super.okPressed();
    }

    /**
     * Schedules a {@link Job} to retrieve {@link KNIMEURIDescription} for the selected component and populates the info
     * text field.
     *
     * @param display to schedule UI update in the GUI thread
     */
    private void scheduleFetchVersionListJob(final Display display) {
        if (m_fetchVersionListJob != null) {
            m_fetchVersionListJob.cancel();
        }
        m_fetchVersionListJob = new FetchVersionListJob(display);
        m_fetchVersionListJob.schedule();
    }

    /**
     * Calls {@link ResolverUtil#toDescription(URI, IProgressMonitor)} and updates
     * {@link BulkChangeMetaNodeLinksDialog#m_versionDescriptionTextField}.
     */
    private final class FetchVersionListJob extends Job {

        private final Display m_display;

        FetchVersionListJob(final Display display) {
            super("Fetching available Hub Space versions.");
            m_display = display;
            m_tableViewer.getTable().setEnabled(false);
            setPriority(Job.INTERACTIVE);
        }

        @Override
        protected IStatus run(final IProgressMonitor monitor) {
            Optional<List<SpaceVersion>> optList = fetch(monitor);
            List<SpaceVersion> list;

            IStatus status;
            if (optList.isPresent()) {
                list = optList.get();
                status = Status.OK_STATUS;
            } else {
                list = List.of();
                status = Status.warning(
                    String.format("Unable to fetch available Hub Space versions for Component %s with name \"%s\"",
                        m_component.getID(), m_component.getName()));
            }

            m_display.asyncExec(() -> {
                if (!m_display.isDisposed() && !monitor.isCanceled()) {
                    setSpaceVersionList(list);
                }
            });

            return status;
        }

        private Optional<List<SpaceVersion>> fetch(final IProgressMonitor monitor) {
            if (monitor.isCanceled()) {
                return Optional.empty();
            }
            NodeContext.pushContext(m_manager);
            try {
                return ResolverUtil.getSpaceVersions(m_component.getTemplateInformation().getSourceURI(), monitor);
            } catch (Exception e) {
                NodeLogger.getLogger(getClass()).error(e);
                return Optional.empty();
            } finally {
                NodeContext.removeLastContext();
            }
        }

        /** Display the fetched versions in the dialog's table viewer component. */
        private void setSpaceVersionList(final List<SpaceVersion> list) {
            // incoming list is immutable, copy before sorting
            m_spaceVersions = new ArrayList<>(list);
            m_spaceVersions.sort(Comparator.comparing(SpaceVersion::getVersion).reversed());

            syncEnableTableViewer();

            m_tableViewer.setInput(m_spaceVersions);

            // pre-select current version in the list
            rowIdxForSpaceVersion(m_selectedSpaceVersion)//
                .map(m_tableViewer::getElementAt)//
                .ifPresent(o -> m_tableViewer.setSelection(new StructuredSelection(o), true));

        }

        /** Find the row that represents a space version (in order to preselect the current version in the table) */
        private Optional<Integer> rowIdxForSpaceVersion(final Integer version) {
            final List<Integer> versions =
                m_spaceVersions.stream().map(SpaceVersion::getVersion).collect(Collectors.toList());
            int idx = versions.indexOf(version);
            return Optional.ofNullable(idx == -1 ? null : idx);
        }
    }

    /**
     * Shows a warning about template links to <i>Current State</i> being unstable.
     * The warning can be suppressed, utilizing the {@link PreferenceConstants#P_CONFIRM_LINK_CURRENT_STATE} key.
     *
     * @param link template link URL
     */
    @SuppressWarnings({ "deprecation", "restriction" })
    public static void warnLinkToCurrentState(final URL link) {
        final var uiPlugin = KNIMEUIPlugin.getDefault();
        final var prefStore = uiPlugin.getPreferenceStore();
        final var confirmLinkCurrentState = PreferenceConstants.P_CONFIRM_LINK_CURRENT_STATE;
        if (!prefStore.contains(confirmLinkCurrentState) || prefStore.getBoolean(confirmLinkCurrentState)) {
            final var dialog = MessageDialogWithToggle.openInformation(SWTUtilities.getActiveShell(),
                "Template Link references Current State on Hub...",
                "The link URL '" + link + "' points to the current state of the Template on the Hub.\n\n"
                        + "If the referenced template is not maintained by you, it is recommended to reference a "
                        + "*versioned* state (either a specific version or 'Latest Version') instead. "
                        + "The 'Current State' of the template may not always be fully working or tested.",
                        "Do not show again", false, prefStore, confirmLinkCurrentState);
            if (dialog.getToggleState()) {
                prefStore.setValue(confirmLinkCurrentState, false);
                uiPlugin.savePluginPreferences();
            }
        }
    }
}
