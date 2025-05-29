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
 * Created on 23.10.2013 by Christian Albrecht, KNIME AG, Zurich, Switzerland
 */
package org.knime.workbench.explorer.view.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.function.FailableFunction;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;
import org.knime.core.node.NodeLogger;
import org.knime.core.workbench.WorkbenchActivator;
import org.knime.core.workbench.WorkbenchConstants;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountException;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountPointSettings;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountPointType;
import org.knime.core.workbench.preferences.MountPointsPreferencesUtil;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.view.AbstractContentProviderFactory;

/**
 * Classic UI editor for the mount table.
 *
 * @author Christian Albrecht, KNIME AG, Zurich, Switzerland
 * @since 6.0
 */
@SuppressWarnings("java:S6212") // `var` can hinder readability
public final class MountPointTableEditor extends FieldEditor {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(MountPointTableEditor.class);

    private static final int MOUNT_ID_PROP = 0;

    private static final int CONTENT_PROP = 1;

    private static final int TYPE_PROP = 2;

    private static final class MountPointTableLabelProvider implements ITableLabelProvider {

        @Override
        public Image getColumnImage(final Object element, final int columnIndex) {
            switch (columnIndex) {
                case CONTENT_PROP, TYPE_PROP:
                    final EnablementMountPointSettingsWrapper settings = (EnablementMountPointSettingsWrapper)element;
                    final AbstractContentProviderFactory factory =
                        ExplorerMountTable.getContentProviderFactory(settings.factoryID());
                    return factory == null ? null : factory.getImage();

                default:
                    break;
            }

            return null;
        }

        @Override
        public String getColumnText(final Object element, final int columnIndex) {
            EnablementMountPointSettingsWrapper settings = (EnablementMountPointSettingsWrapper)element;
            final WorkbenchMountPointType type =
                WorkbenchActivator.getInstance().getMountPointType(settings.factoryID()).orElse(null);

            return switch (columnIndex) {
                case MOUNT_ID_PROP -> settings.mountID();
                case CONTENT_PROP -> fromMountPointType(type, settings, settings::getContentDisplayString);
                case TYPE_PROP -> fromMountPointType(type, settings, WorkbenchMountPointType::getDisplayName);
                default -> null;
            };
        }

        private static String fromMountPointType(final WorkbenchMountPointType type,
            final EnablementMountPointSettingsWrapper settings,
            final FailableFunction<WorkbenchMountPointType, String, WorkbenchMountException> callback) {
            try {
                return type == null ? ("Unknown type '" + settings.factoryID() + "'") : callback.apply(type);
            } catch (WorkbenchMountException e) {
                LOGGER.error(e);
                return "'" + settings.factoryID() + "' (unloadable)";
            }
        }

        @Override
        public boolean isLabelProperty(final Object element, final String property) {
            return false;
        }

        @Override
        public void addListener(final ILabelProviderListener listener) {
            // unused
        }

        @Override
        public void removeListener(final ILabelProviderListener listener) {
            // unused
        }

        @Override
        public void dispose() {
          // unused
        }
    }

    private static final class MountPointTableCheckStateProvider implements ICheckStateProvider {

        @Override
        public boolean isGrayed(final Object element) {
            return false;
        }

        @Override
        public boolean isChecked(final Object element) {
            return ((EnablementMountPointSettingsWrapper)element).isActive();
        }
    }

    private static final class MountPointTableCheckStateListener implements ICheckStateListener {

        @Override
        public void checkStateChanged(final CheckStateChangedEvent event) {
            EnablementMountPointSettingsWrapper settings = (EnablementMountPointSettingsWrapper)event.getElement();
            settings.setActive(event.getChecked());
        }

    }

    private Table m_table;

    private TableViewer m_tableViewer;

    private List<EnablementMountPointSettingsWrapper> m_mountSettings;

    private Composite m_buttonBox;

    private Button m_addButton;

    private Button m_editButton;

    private Button m_removeButton;

    private Button m_upButton;

    private Button m_downButton;

    private SelectionListener m_selectionListener;

    /**
     * Creates a new MountPointTableEditor.
     *
     * @param parent The parent component
     */
    public MountPointTableEditor(final Composite parent) {
        init(WorkbenchConstants.P_EXPLORER_MOUNT_POINT_XML, "List of configured mount points:");
        createControl(parent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void adjustForNumColumns(final int numColumns) {
        Control control = getLabelControl();
        ((GridData)control.getLayoutData()).horizontalSpan = numColumns;
        ((GridData)m_tableViewer.getControl().getLayoutData()).horizontalSpan = numColumns - 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doFillIntoGrid(final Composite parent, final int numColumns) {
        Control control = getLabelControl(parent);
        GridData gd = new GridData();
        gd.horizontalSpan = numColumns;
        control.setLayoutData(gd);

        m_tableViewer = getTableControl(parent);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.verticalAlignment = GridData.FILL;
        gd.horizontalSpan = numColumns - 1;
        gd.grabExcessHorizontalSpace = true;
        m_tableViewer.getTable().setLayoutData(gd);

        m_buttonBox = getButtonBoxControl(parent);
        gd = new GridData();
        gd.verticalAlignment = GridData.BEGINNING;
        m_buttonBox.setLayoutData(gd);
    }

    /**
     * @param parent
     * @return
     */
    private TableViewer getTableControl(final Composite parent) {

        m_table =
            new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION | SWT.CHECK);
        m_table.setHeaderVisible(true);
        m_table.setLinesVisible(true);

        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.heightHint = getTableHeightHint(m_table, 10);
        gd.horizontalSpan = 1;
        gd.widthHint = new PixelConverter(parent).convertWidthInCharsToPixels(30);

        m_table.addSelectionListener(getSelectionListener());

        TableLayout tableLayout = new TableLayout();
        m_table.setLayout(tableLayout);
        m_table.setLayoutData(gd);

        m_tableViewer = new CheckboxTableViewer(m_table);
        m_tableViewer.setUseHashlookup(true);
        m_tableViewer.setContentProvider(ArrayContentProvider.getInstance());
        m_tableViewer.setLabelProvider(new MountPointTableLabelProvider());
        ((CheckboxTableViewer)m_tableViewer).setCheckStateProvider(new MountPointTableCheckStateProvider());
        ((CheckboxTableViewer)m_tableViewer).addCheckStateListener(new MountPointTableCheckStateListener());

        TableColumn mountCol = new TableColumn(m_table, SWT.NONE);
        mountCol.setText("MountID");
        mountCol.setWidth(150);
        mountCol.setResizable(true);

        TableColumn contentCol = new TableColumn(m_table, SWT.NONE);
        contentCol.setText("Mounted Content");
        contentCol.setWidth(200);
        contentCol.setResizable(true);

        TableColumn typeCol = new TableColumn(m_table, SWT.NONE);
        typeCol.setText("Mounted Type");
        typeCol.setWidth(170);
        typeCol.setResizable(true);

        return m_tableViewer;
    }

    private static int getTableHeightHint(final Table table, final int rows) {
        if (table.getFont().equals(JFaceResources.getDefaultFont())) {
            table.setFont(JFaceResources.getDialogFont());
        }
        int result = table.getItemHeight() * rows + table.getHeaderHeight();
        if (table.getLinesVisible()) {
            result += table.getGridLineWidth() * (rows - 1);
        }
        return result;
    }

    private Composite getButtonBoxControl(final Composite parent) {
        if (m_buttonBox == null) {
            m_buttonBox = new Composite(parent, SWT.NULL);
            GridLayout layout = new GridLayout();
            layout.marginWidth = 0;
            m_buttonBox.setLayout(layout);
            createButtons(m_buttonBox);
            m_buttonBox.addDisposeListener(event -> {
                m_addButton = null;
                m_editButton = null;
                m_removeButton = null;
                m_upButton = null;
                m_downButton = null;
                m_buttonBox = null;
            });

        } else {
            checkParent(m_buttonBox, parent);
        }

        selectionChanged();
        return m_buttonBox;
    }

    /**
     * Creates the Add, Remove, Up, and Down button in the given button box.
     *
     * @param box the box for the buttons
     */
    private void createButtons(final Composite box) {
        m_addButton = createPushButton(box, "Ne&w...");
        m_editButton = createPushButton(box, "&Edit...");
        m_removeButton = createPushButton(box, "&Remove");
        m_upButton = createPushButton(box, "&Up");
        m_downButton = createPushButton(box, "Dow&n");
    }

    /**
     * @param box
     * @param string
     * @return
     */
    private Button createPushButton(final Composite parent, final String label) {
        Button button = new Button(parent, SWT.PUSH);
        button.setText(label);
        button.setFont(parent.getFont());
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        int widthHint = convertHorizontalDLUsToPixels(button, IDialogConstants.BUTTON_WIDTH);
        data.widthHint = Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
        button.setLayoutData(data);
        button.addSelectionListener(getSelectionListener());
        return button;
    }

    /**
     * Returns this field editor's selection listener. The listener is created if nessessary.
     *
     * @return the selection listener
     */
    private SelectionListener getSelectionListener() {
        if (m_selectionListener == null) {
            createSelectionListener();
        }
        return m_selectionListener;
    }

    /**
     * Creates a selection listener.
     */
    public void createSelectionListener() {
        m_selectionListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent event) {
                Widget widget = event.widget;
                if (widget == m_addButton) {
                    addPressed();
                } else if (widget == m_editButton) {
                    editPressed();
                } else if (widget == m_removeButton) {
                    removePressed();
                } else if (widget == m_upButton) {
                    upPressed();
                } else if (widget == m_downButton) {
                    downPressed();
                } else if (widget == m_table) {
                    selectionChanged();
                }
            }
        };
    }

    private void selectionChanged() {

        int index = m_table.getSelectionIndex();
        int size = m_table.getItemCount();

        if (index >= 0) {
            EnablementMountPointSettingsWrapper settings = getSettingsAt(index);
            AbstractContentProviderFactory contentProviderFactory =
                ExplorerMountTable.getContentProviderFactories().get(settings.factoryID());
            m_editButton.setEnabled(contentProviderFactory.isMountpointEditable());
        } else {
            m_editButton.setEnabled(false);
        }
        // disable remove button for only one entry
        // deleting all mount points, causes problems with updated pre 2.9 workspaces
        m_removeButton.setEnabled(index >= 0 && size > 1);
        m_upButton.setEnabled(size > 1 && index > 0);
        m_downButton.setEnabled(size > 1 && index >= 0 && index < size - 1);
    }

    private void addPressed() {
        setPresentsDefaultValue(false);
        EnablementMountPointSettingsWrapper input = getNewInputObject();

        if (input != null) {
            int newIndex = Math.max(0, m_table.getSelectionIndex());
            addItemToTable(input, newIndex);
            m_table.setSelection(newIndex);
            selectionChanged();
        }
    }

    private void editPressed() {
        setPresentsDefaultValue(false);
        int index = m_table.getSelectionIndex();
        if (index >= 0) {
            EnablementMountPointSettingsWrapper settings = getSettingsAt(index);
            EnablementMountPointSettingsWrapper edited = editSelectedObject(settings);
            if (edited != null) {
                removeItemFromTable(settings);
                addItemToTable(edited, index);
                m_table.setSelection(index);
                selectionChanged();
            }
        }
    }

    private void removePressed() {
        setPresentsDefaultValue(false);
        int index = m_table.getSelectionIndex();
        if (index >= 0) {
            EnablementMountPointSettingsWrapper settings = getSettingsAt(index);
            removeItemFromTable(settings);
            selectionChanged();
        }
    }

    private EnablementMountPointSettingsWrapper getSettingsAt(final int index) {
        return (EnablementMountPointSettingsWrapper)m_table.getItem(index).getData();
    }

    private void upPressed() {
        swap(true);
    }

    private void downPressed() {
        swap(false);
    }

    private void swap(final boolean up) {
        setPresentsDefaultValue(false);
        int index = m_table.getSelectionIndex();
        int target = up ? (index - 1) : (index + 1);

        if (index >= 0) {
            TableItem[] selection = m_table.getSelection();
            Assert.isTrue(selection.length == 1);

            EnablementMountPointSettingsWrapper settings = (EnablementMountPointSettingsWrapper)selection[0].getData();
            removeItemFromTable(settings);
            addItemToTable(settings, target);
            m_table.setSelection(target);
        }
        selectionChanged();
    }

    private EnablementMountPointSettingsWrapper getNewInputObject() {
        EditMountPointDialog dlg = new EditMountPointDialog(getShell(),
            ExplorerMountTable.getAddableContentProviders(getContentProviderIDs()), getAllMountIDs());
        if (dlg.open() != Window.OK) {
            return null;
        }
        return dlg.getMountSettings().map(EnablementMountPointSettingsWrapper::new).orElseThrow();
    }

    private List<String> getContentProviderIDs() {
        Set<String> idSet = new LinkedHashSet<>();
        for (EnablementMountPointSettingsWrapper settings : m_mountSettings) {
            idSet.add(settings.factoryID());
        }
        return new ArrayList<>(idSet);
    }

    private List<String> getAllMountIDs() {
        return m_mountSettings.stream() //
            .map(EnablementMountPointSettingsWrapper::mountID) //
            .collect(Collectors.toCollection(ArrayList::new));
    }

    private Shell getShell() {
        if (m_addButton == null) {
            return null;
        }
        return m_addButton.getShell();
    }

    /**
     * Edits an existing item from the list.
     *
     * @param settings the settings object to load into the edit dialog
     * @return an edited item or null if item unchanged
     */
    private EnablementMountPointSettingsWrapper editSelectedObject(final EnablementMountPointSettingsWrapper settings) {
        List<String> existingMountIDs = getAllMountIDs();
        existingMountIDs.remove(settings.mountID());
        final String factoryID = settings.factoryID();
        AbstractContentProviderFactory factory = ExplorerMountTable.getContentProviderFactory(factoryID);
        EditMountPointDialog dlg =
            new EditMountPointDialog(getShell(), List.of(factory), existingMountIDs, settings.toSettings());
        if (dlg.open() != Window.OK) {
            return null;
        }

        return dlg.getMountSettings().map(EnablementMountPointSettingsWrapper::new).orElse(null);
    }

    @Override
    protected void doLoad() {
        doLoad(MountPointsPreferencesUtil.loadSortedMountSettingsFromPreferences(true));
    }

    @Override
    protected void doLoadDefault() {
        doLoad(MountPointsPreferencesUtil.loadSortedMountSettingsFromDefaultPreferenceNode());
    }

    private void doLoad(final List<WorkbenchMountPointSettings> mountSettings) {
        m_mountSettings = mountSettings.stream().map(EnablementMountPointSettingsWrapper::new)
            .collect(Collectors.toCollection(ArrayList::new));
        m_tableViewer.setInput(m_mountSettings);
        m_tableViewer.refresh();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doStore() {
        // AP-8989 Switching to IEclipsePreferences
        List<WorkbenchMountPointSettings> mountSettings = Arrays.stream(m_table.getItems()) //
                .map(item -> (EnablementMountPointSettingsWrapper)item.getData()) //
                .map(EnablementMountPointSettingsWrapper::toSettings) //
                .toList();
        MountPointsPreferencesUtil.saveMountSettings(mountSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfControls() {
        return 6;
    }

    private void addItemToTable(final EnablementMountPointSettingsWrapper settings, final int index) {
        m_mountSettings.add(index, settings);
        m_tableViewer.insert(settings, index);
        m_tableViewer.refresh();
    }

    @SuppressWarnings("java:S2250") // not a performance hotspot
    private void removeItemFromTable(final EnablementMountPointSettingsWrapper settings) {
        m_mountSettings.remove(settings);
        m_tableViewer.remove(settings);
        m_tableViewer.refresh();
    }

    static final class EnablementMountPointSettingsWrapper {

        private final WorkbenchMountPointSettings m_settings;
        private boolean m_isActive;

        private EnablementMountPointSettingsWrapper(final WorkbenchMountPointSettings settings) {
            m_settings = settings;
            m_isActive = settings.isActive();
        }

        String mountID() {
            return m_settings.mountID();
        }

        String factoryID() {
            return m_settings.factoryID();
        }

        WorkbenchMountPointSettings toSettings() {
            if (m_settings.isActive() == m_isActive) {
                return m_settings;
            }
            return m_settings.withActive(m_isActive);
        }

        String getContentDisplayString(final WorkbenchMountPointType type) throws WorkbenchMountException {
            return type.getContentDisplayString(m_settings);
        }

        boolean isActive() {
            return m_isActive;
        }

        void setActive(final boolean active) {
            m_isActive = active;
        }
    }
}
