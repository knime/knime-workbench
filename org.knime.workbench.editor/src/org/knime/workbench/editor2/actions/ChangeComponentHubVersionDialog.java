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
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeContext;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.TemplateUpdateUtil.LinkType;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.core.util.hub.HubItemVersion;
import org.knime.core.util.hub.NamedItemVersion;
import org.knime.core.util.pathresolve.ResolverUtil;
import org.knime.core.util.pathresolve.URIToFileResolve.KNIMEURIDescription;
import org.knime.workbench.ui.KNIMEUIPlugin;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * Dialog for selecting a Hub item version for a component.
 *
 * Shows the metadata, e.g., author, date, and description for each version.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public final class ChangeComponentHubVersionDialog extends Dialog {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ChangeComponentHubVersionDialog.class);

    // ============ state ============ //
    private final WorkflowManager m_manager;

    /** The component to change version of. */
    private final SubNodeContainer m_component;

    private LinkType m_selectedLinkType;

    private Integer m_selectedItemVersion;


    /** Fetched using a {@link FetchVersionListJob} */
    private List<NamedItemVersion> m_versions;

    // ============ view ============ //
    /** Shows the available item versions */
    private TableViewer m_tableViewer;

    private Button m_useSpecificVersionCheckBox;

    private Button m_useLatestStateCheckBox;

    private Button m_useLatestVersionCheckBox;

    /** The strings that appear in the header column of the version table. */
    private static final List<String> COLUMN_NAMES = List.of("Version", "Title", "Description", "Author", "Created On");

    /** Initial column widths in pixels. */
    private static final List<Integer> COLUMN_WIDTHS = List.of(54, 213, 200, 166, 152);

    // ============ async ============ //
    private FetchVersionListJob m_fetchVersionListJob;

    /**
     * Creates a dialog.
     *
     * @param parent the parent shell
     * @param manager The manager (used for resolution of 'knime://' URLs)
     * @param metaNodes components and metanodes in the currently edited workflow
     */
    ChangeComponentHubVersionDialog(final Shell parent, final SubNodeContainer subNodeContainer,
        final WorkflowManager manager) {
        super(parent);
        m_manager = manager;
        m_component = subNodeContainer;
        m_selectedLinkType = LinkType.LATEST_STATE;
        m_selectedItemVersion = null;
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
        selectorLabel.setText("Select a KNIME Hub Item Version");
        final Composite parent1 = content;

        final var buttonGroup = new Composite(content, SWT.NONE);
        buttonGroup.setLayout(new RowLayout());

        final var link = HubItemVersion.of(
            m_component.getTemplateInformation().getSourceURI());

        m_useSpecificVersionCheckBox = new Button(buttonGroup, SWT.RADIO);
        m_useSpecificVersionCheckBox.setText("Specific version    ");
        m_useSpecificVersionCheckBox.setSelection(link.linkType() == LinkType.FIXED_VERSION);
        m_useSpecificVersionCheckBox.addListener(SWT.Selection, l -> sync());

        m_useLatestVersionCheckBox = new Button(buttonGroup, SWT.RADIO);
        m_useLatestVersionCheckBox.setText("Latest version     ");
        m_useLatestVersionCheckBox.setSelection(link.linkType() == LinkType.LATEST_VERSION);
        m_useLatestVersionCheckBox.addListener(SWT.Selection, l -> sync());

        m_useLatestStateCheckBox = new Button(buttonGroup, SWT.RADIO);
        m_useLatestStateCheckBox.setText("Working Area");
        m_useLatestStateCheckBox.setSelection(link.linkType() == LinkType.LATEST_STATE);
        m_useLatestStateCheckBox.addListener(SWT.Selection, l -> sync());
        // holds the table and defines its resizing behavior
        var tableComp = new Composite(parent1, SWT.NONE);
        var gridData = new GridData(GridData.FILL_BOTH);
        gridData.grabExcessHorizontalSpace = true;
        tableComp.setLayout(new FillLayout());
        tableComp.setLayoutData(gridData);

        m_tableViewer = createTableViewer(tableComp);
        m_tableViewer.addSelectionChangedListener(l -> sync());
        m_tableViewer.addDoubleClickListener(this::versionTableDoubleClicked);

        sync();
        scheduleFetchVersionListJob(content.getDisplay());
        return content;
    }

    private void sync() {
        if (m_versions == null) {
            m_useLatestVersionCheckBox.setEnabled(false);
            m_useLatestStateCheckBox.setEnabled(false);
            m_useSpecificVersionCheckBox.setEnabled(false);
        } else if (m_versions.isEmpty()) {
            m_useLatestStateCheckBox.setEnabled(true);
            m_useLatestVersionCheckBox.setEnabled(false);
            m_useSpecificVersionCheckBox.setEnabled(false);
        } else {
            m_useLatestStateCheckBox.setEnabled(true);
            m_useLatestVersionCheckBox.setEnabled(true);
            m_useSpecificVersionCheckBox.setEnabled(true);
        }

        final boolean okEnabled;
        final var table = m_tableViewer.getTable();
        final LinkType selectedLinkType;
        Integer selectedItemVersion = null;
        if (m_useLatestStateCheckBox.getSelection()) {
            selectedLinkType = LinkType.LATEST_STATE;
            okEnabled = m_useLatestStateCheckBox.isEnabled();
            table.setEnabled(false);
        } else if (m_useLatestVersionCheckBox.getSelection()) {
            selectedLinkType = LinkType.LATEST_VERSION;
            okEnabled = m_useLatestVersionCheckBox.isEnabled();
            table.setEnabled(false);
        } else {
            selectedLinkType = LinkType.FIXED_VERSION;
            if (table.getSelectionIndex() != -1 && m_versions != null) {
                int index = table.getSelectionIndex();
                // the table is not sortable, the i-th row always corresponds to the i-th item version
                selectedItemVersion = m_versions.get(index).version();
            }
            table.setEnabled(m_useSpecificVersionCheckBox.isEnabled());
            okEnabled = m_useSpecificVersionCheckBox.isEnabled() && selectedItemVersion != null;
        }
        m_selectedLinkType = selectedLinkType;
        m_selectedItemVersion = selectedItemVersion;

        final var okButton = getButton(IDialogConstants.OK_ID);
        if (okButton != null) {
            okButton.setEnabled(okEnabled);
        }
    }

    /**
     * Upon double clicking an item version in the versions list, select the version and close the dialog.
     *
     * @param event
     */
    private void versionTableDoubleClicked(final DoubleClickEvent event) {
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
        viewer.setLabelProvider(new HubItemVersionLabelProvider());

        viewer.getTable().pack();
        viewer.refresh();

        return viewer;
    }

    /**
     * @return selected link item version
     */
    HubItemVersion getSelectedVersion() {
        return new HubItemVersion(m_selectedLinkType,
            m_selectedLinkType == LinkType.FIXED_VERSION ? m_selectedItemVersion : null);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected void configureShell(final Shell shell) {
        super.configureShell(shell);
        shell.setSize(615, 400);
        shell.setText("Select KNIME Hub Item Version");
        var img = KNIMEUIPlugin.getDefault().getImageRegistry().get("knime");
        shell.setImage(img);
    }

    private static final class HubItemVersionLabelProvider extends LabelProvider implements ITableLabelProvider {

        /**
         * Gets the content of the n-th row in the versions table.
         * <ol>
         * <li>version number</li>
         * <li>short description</li>
         * <li>author name</li>
         * <li>date of creation</li>
         * </ol>
         */
        private static final List<Function<NamedItemVersion, String>> COLUMN_VALUE_EXTRACTORS =
            List.of(e -> String.valueOf(e.version()), NamedItemVersion::title, NamedItemVersion::description, NamedItemVersion::author,
                NamedItemVersion::createdOn);

        @Override
        public String getColumnText(final Object row, final int colIdx) {
            return COLUMN_VALUE_EXTRACTORS.get(colIdx).apply((NamedItemVersion)row);
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
            super("Fetching available Hub item versions.");
            m_display = display;
            m_tableViewer.getTable().setEnabled(false);
            setPriority(Job.INTERACTIVE);
        }

        @Override
        protected IStatus run(final IProgressMonitor monitor) {
            IStatus status;

            try {
                var versions = fetch(monitor);
                status = Status.OK_STATUS;
                m_display.asyncExec(() -> {
                    if (!m_display.isDisposed() && !monitor.isCanceled()) {
                        setItemVersionList(versions);
                    }
                });
            } catch (Exception e) {
                status = Status.warning(
                    String.format("Unable to fetch available Hub item versions for Component %s with name \"%s\"%n%s",
                        m_component.getID(), m_component.getName(), e.getLocalizedMessage()));
                e.printStackTrace();
            }

            return status;
        }

        private List<NamedItemVersion> fetch(final IProgressMonitor monitor) throws CanceledExecutionException {
            if (monitor.isCanceled()) {
                throw new CanceledExecutionException();
            }
            NodeContext.pushContext(m_manager);
            try {
                return ResolverUtil.getHubItemVersions(m_component.getTemplateInformation().getSourceURI());
            } finally {
                NodeContext.removeLastContext();
            }
        }

        /** Display the fetched versions in the dialog's table viewer component. */
        private void setItemVersionList(final List<NamedItemVersion> list) {
            // incoming list is immutable, copy before sorting
            m_versions = new ArrayList<>(list);
            m_versions.sort(Comparator.comparing(NamedItemVersion::version).reversed());

            m_tableViewer.setInput(m_versions);
            final var uri = m_component.getTemplateInformation().getSourceURI();
            final var initialLinkPair = HubItemVersion.of(uri);

            if (initialLinkPair.linkType() == LinkType.FIXED_VERSION) {
                // pre-select current version in the list

                rowIdxForItemVersion(initialLinkPair.versionNumber())//
                    .map(m_tableViewer::getElementAt)//
                    .ifPresent(o -> m_tableViewer.setSelection(new StructuredSelection(o), true));
            }

            sync();
        }

        /** Find the row that represents an item version (in order to preselect the current version in the table) */
        private Optional<Integer> rowIdxForItemVersion(final Integer version) {
            final List<Integer> versions =
                m_versions.stream().map(NamedItemVersion::version).collect(Collectors.toList());
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
