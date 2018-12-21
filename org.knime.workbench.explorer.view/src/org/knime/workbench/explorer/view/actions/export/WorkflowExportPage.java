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
 * Created: May 30, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer.view.actions.export;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.knime.core.node.KNIMEConstants;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.MountPoint;
import org.knime.workbench.explorer.dialogs.SpaceResourceSelectionDialog;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.ContentObject;

/**
 * Page to enter the select the workflows to export and enter the destination.
 *
 * @author Christoph Sieb, University of Konstanz
 * @author Fabian Dill, KNIME AG, Zurich, Switzerland
 */
public class WorkflowExportPage extends WizardPage {

    private static final String[] FILTER_EXTENSION =
        {"*." + KNIMEConstants.KNIME_ARCHIVE_FILE_EXTENSION + ";*." + KNIMEConstants.KNIME_WORKFLOW_FILE_EXTENSION};

    private Text m_containerText;

    private Text m_fileText;

    private Button m_excludeData;

    private AbstractExplorerFileStore m_selection;

    private ExplorerFileStoreProvider m_provider;

    private CheckboxTreeViewer m_treeViewer;

    private Button m_selAll;

    private Button m_deselAll;

    private static String lastSelectedTargetLocation;

    private Label m_fileLabel;

    /**
     * Constructor for NewWorkflowPage.
     *
     * @param selection The initial selection
     */
    public WorkflowExportPage(final AbstractExplorerFileStore selection) {
        super("wizardPage");
        setTitle("Export KNIME workflow(s)");
        setDescription("Exports KNIME workflows.");
        setImageDescriptor(ImageRepository
                .getImageDescriptor(SharedImages.ExportBig));
        this.m_selection = selection;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    public boolean isPageComplete() {
        return (getStatus() == null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createControl(final Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        // place components vertically
        container.setLayout(new GridLayout(1, false));

        Group exportGroup = new Group(container, SWT.NONE);
        exportGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        GridLayout layout = new GridLayout();
        exportGroup.setLayout(layout);
        layout.numColumns = 3;
        layout.verticalSpacing = 9;
        Label label = new Label(exportGroup, SWT.NULL);
        label.setText("Select workflow(s) to export:");

        m_containerText =
                new Text(exportGroup, SWT.BORDER | SWT.SINGLE | SWT.READ_ONLY);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        m_containerText.setLayoutData(gd);
        m_containerText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                dialogChanged();
            }
        });

        Button selectProjectButton = new Button(exportGroup, SWT.PUSH);
        selectProjectButton.setText("Select...");
        selectProjectButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                handleFlowSelect();
            }
        });

        m_fileLabel = new Label(exportGroup, SWT.NULL);
        if (AbstractExplorerFileStore.isWorkflow(m_selection)) {
            m_fileLabel.setText("Destination workflow file name (." + KNIMEConstants.KNIME_WORKFLOW_FILE_EXTENSION + "):");
        } else {
            m_fileLabel.setText("Destination archive file name (." + KNIMEConstants.KNIME_ARCHIVE_FILE_EXTENSION + "):");
        }

        m_fileText = new Text(exportGroup, SWT.BORDER | SWT.SINGLE);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        m_fileText.setLayoutData(gd);
        m_fileText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                dialogChanged();
            }
        });

        Button selectExportFilebutton = new Button(exportGroup, SWT.PUSH);
        selectExportFilebutton.setText("Browse...");
        selectExportFilebutton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                handleExportFileBrowse();
            }
        });

        final Group group = new Group(container, SWT.NONE);
        final GridLayout gridLayout1 = new GridLayout();
        group.setLayout(gridLayout1);
        group.setText("Options");
        final GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 2;
        group.setLayoutData(gridData);

        m_excludeData = new Button(group, SWT.CHECK);
        m_excludeData.setSelection(true);
        m_excludeData.setText("Reset Workflow(s) before export");

        createTreeViewer(container);

        initialize();
        containerSelectionChanged();
        dialogChanged();
        setControl(container);
    }

    private void createTreeViewer(final Composite parent) {
        final Group group = new Group(parent, SWT.V_SCROLL);
        final GridLayout gridLayout1 = new GridLayout(1, false);
        group.setLayout(gridLayout1);
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        m_treeViewer = new CheckboxTreeViewer(group);
        m_treeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
        // provider handling file stores
        m_provider = new ExplorerFileStoreProvider();
        m_treeViewer.setContentProvider(m_provider);
        m_treeViewer.setLabelProvider(m_provider);
        m_treeViewer.addCheckStateListener(new ICheckStateListener() {

            @Override
            public void checkStateChanged(final CheckStateChangedEvent event) {
                Object o = event.getElement();
                boolean isChecked = event.getChecked();
                m_treeViewer.setSubtreeChecked(o, isChecked);
                if (o instanceof AbstractExplorerFileStore) {
                    AbstractExplorerFileStore sel =
                            (AbstractExplorerFileStore)o;
                    setParentTreeChecked(sel, isChecked);
                }
                dialogChanged();
            }

        });
        Composite buttons = new Composite(group, SWT.NONE);
        buttons.setLayout(new GridLayout(2, false));
        buttons.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        m_selAll = new Button(buttons, SWT.PUSH);
        m_selAll.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
        m_selAll.setText("Select All");
        m_selAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                selectAll();
            }
        });
        m_deselAll = new Button(buttons, SWT.PUSH);
        m_deselAll.setText("Deselect all");
        m_deselAll.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));
        m_deselAll.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                deselectAll();
            }
        });

        m_treeViewer.getTree().setVisible(false);
        m_selAll.setVisible(false);
        m_deselAll.setVisible(false);
    }

    /**
     * Tests if the current workbench selection is a suitable container to use.
     */
    private void initialize() {

        AbstractExplorerFileStore sel = m_selection;
        // load last selected dir from dialog settings
        IDialogSettings settings = getDialogSettings();
        if (settings != null) {
            String lastSelected = settings.get(KEY_LOC);
            if (lastSelected != null && !lastSelected.isEmpty()) {
                lastSelectedTargetLocation = lastSelected;
            }
        }
        if (sel != null) {
            m_containerText.setText(sel.getMountIDWithFullPath());
        }
        updateFileName(sel);
    }

    private void updateFileName(final AbstractExplorerFileStore sel) {
        if (sel != null) {
            String fileName = sel.getName() + ".";
            if (AbstractExplorerFileStore.isWorkflow(sel)) {
                fileName += KNIMEConstants.KNIME_WORKFLOW_FILE_EXTENSION;
                m_fileLabel
                    .setText("Destination workflow file name (." + KNIMEConstants.KNIME_WORKFLOW_FILE_EXTENSION + "):");
            } else {
                fileName += KNIMEConstants.KNIME_ARCHIVE_FILE_EXTENSION;
                m_fileLabel
                    .setText("Destination workflow file name (." + KNIMEConstants.KNIME_ARCHIVE_FILE_EXTENSION + "):");
            }
            if (sel.getFullName().equals("/")) {
                fileName = "knime-export." + KNIMEConstants.KNIME_ARCHIVE_FILE_EXTENSION;
            }
            File f = null;
            if (lastSelectedTargetLocation != null) {
                // create file in last used directory
                File parentFile = new File(lastSelectedTargetLocation);
                if (parentFile.exists() && parentFile.isDirectory()) {
                    f = new File(parentFile, fileName);
                }
            }
            if (f == null) {
                // no value for last selected target - or folder does not exists
                // anymore -> default case: create file in user.home
                f = new File(System.getProperty("user.home"), fileName);
            }
            m_fileText.setText(f.getAbsolutePath());
        } else {
            m_fileText.setText("");
        }
    }

    private void setLastSelectedExportLocation() {
        File parent = new File(getFileName());
        lastSelectedTargetLocation = parent.getParent();
    }

    /**
     * Uses the KNIME Spaces select dialog to get a new flow/group for export.
     */
    private void handleFlowSelect() {
        ContentObject initSel = ContentObject.forFile(m_selection);
        List<String> allIDs = ExplorerMountTable.getAllVisibleMountIDs();
        ArrayList<String> selIDs = new ArrayList<String>();
        for (String id : allIDs) {
            MountPoint mountPoint = ExplorerMountTable.getMountPoint(id);
            if (!mountPoint.getProvider().isRemote()) {
                selIDs.add(id);
            }
        }
        SpaceResourceSelectionDialog dlg =
                new SpaceResourceSelectionDialog(getShell(), selIDs.toArray(new String[selIDs.size()]), initSel);
        dlg.setTitle("Source Selection");
        dlg.setHeader("Export items selection.");
        dlg.setDescription("Please select the item or directory to export.");
        if (dlg.open() != Window.OK) {
            return;
        }
        AbstractExplorerFileStore sel = dlg.getSelection();
        if (sel == null) {
            return;
        }
        m_selection = sel;
        containerSelectionChanged();
    }

    /**
     * Updates the tree visibility, and container text field, etc.
     */
    private void containerSelectionChanged() {
        if ((m_selection == null) || isExportElement(m_selection)) {
            m_treeViewer.getTree().setVisible(false);
            m_selAll.setVisible(false);
            m_deselAll.setVisible(false);
        } else {
            // clear the selection before setting a new input!!!
            m_treeViewer.setSelection(null);
            m_treeViewer.setInput(m_selection);
            m_treeViewer.getTree().setVisible(true);
            m_selAll.setVisible(true);
            m_deselAll.setVisible(true);
            getContainer().getShell().pack(true);
            getContainer().getShell().update();
            selectAll();
        }
        if (m_selection != null) {
            m_containerText.setText(m_selection.getMountIDWithFullPath());
        } else {
            m_containerText.setText("");
        }
        // also update the target file name
        updateFileName(m_selection);
    }

    /**
     * @return true if the check box for excluding data is checked
     */
    boolean excludeData() {
        return m_excludeData.getSelection();
    }

    /**
     * Uses the standard file selection dialog to choose the export file name.
     */

    private void handleExportFileBrowse() {
        FileDialog fileDialog = new FileDialog(getShell(), SWT.SAVE);
        fileDialog.setFilterExtensions(FILTER_EXTENSION);
        fileDialog.setText("Specify archive file to store "
                + "the exported elements in.");
        if (m_fileText.getText() != null
                && !m_fileText.getText().trim().isEmpty()) {
            String exportString = m_fileText.getText().trim();
            IPath p = new Path(exportString);
            if (p.segmentCount() > 1) {
                fileDialog.setFilterPath(p.removeLastSegments(1).toOSString());
                fileDialog.setFileName(p.lastSegment());
            }
        }
        String filePath = fileDialog.open();
        if (filePath == null) {
            return;
        }
        filePath = filePath.trim();
        if (filePath.isEmpty()) {
            return;
        }
        if (AbstractExplorerFileStore.isWorkflow(m_selection)) {
            if (!filePath.toLowerCase().endsWith("." + KNIMEConstants.KNIME_WORKFLOW_FILE_EXTENSION)) {
                filePath += "." + KNIMEConstants.KNIME_WORKFLOW_FILE_EXTENSION;
            }
        } else {
            if (!filePath.toLowerCase().endsWith("." + KNIMEConstants.KNIME_ARCHIVE_FILE_EXTENSION)) {
                filePath += "." + KNIMEConstants.KNIME_ARCHIVE_FILE_EXTENSION;
            }
        }
        m_fileText.setText(filePath);
        setLastSelectedExportLocation();
    }

    private void selectAll() {
        setCheckedAll(true);
    }

    private void deselectAll() {
        setCheckedAll(false);
    }

    private void setCheckedAll(final boolean state) {
        ArrayList<Object> obj = new ArrayList<Object>();
        obj.add(m_selection);
        while (obj.size() > 0) {
            Object f = obj.remove(obj.size() - 1);
            if (f instanceof AbstractExplorerFileStore) {
                AbstractExplorerFileStore store = (AbstractExplorerFileStore)f;
                m_treeViewer.setChecked(store, state);
            }
            Object[] childs = m_provider.getChildren(f);
            if (childs != null) {
                obj.addAll(Arrays.asList(childs));
            }
        }
    }

    /**
     * Returns true if store is one of the accepted elements for export (file, flow, template). Workflow groups (dirs)
     * are not accepted (will return false).
     * @param store
     * @return
     */
    static boolean isExportElement(final AbstractExplorerFileStore store) {
        return AbstractExplorerFileStore.isWorkflow(store) || AbstractExplorerFileStore.isDataFile(store)
                || AbstractExplorerFileStore.isWorkflowTemplate(store);
    }

    /**
     * Ensures that both text fields are set.
     */
    private void dialogChanged() {
        String msg = getStatus();
        updateStatus(msg);
    }

    private String getStatus() {
        if (m_selection == null) {
            return "Please select an element to export!";
        }
        String fileName = getFileName();
        if (fileName.length() == 0 || fileName.endsWith(File.separator)) {
            return "Please enter a destination file name.";
        }
        if (m_treeViewer.getTree().isVisible()
                && m_treeViewer.getCheckedElements().length == 0) {
            return "Please select the element(s) to export.";
        }
        Collection<AbstractExplorerFileStore> sel = getElementsToExport();
        if (sel.isEmpty()) {
            return "Selection contains no elements.";
        } else {
            return null;
        }
    }

    private void updateStatus(final String message) {
        setErrorMessage(message);
        setPageComplete(message == null);
    }

    /**
     * The selected workflow, or the root of all selected flows.
     *
     * @return selected workflow, or the root of all selected flows
     */
    public AbstractExplorerFileStore getSelectedStore() {
        return m_selection;
    }

    /**
     *
     * @return checked workflows and workflow groups, files or templates
     * @since 7.1
     */
    public Collection<AbstractExplorerFileStore> getElementsToExport() {

        List<AbstractExplorerFileStore> elements =
                new ArrayList<AbstractExplorerFileStore>();
        if (m_selection == null) {
            return elements;
        }
        if (isExportElement(m_selection)) { // workgroups are not exportElements
            elements.add(m_selection);
            return elements;
        }

        if (AbstractExplorerFileStore.isWorkflowGroup(m_selection)) {
            // also add the selected root - unless it is the "/"
            if (!"/".equals(m_selection.getFullName())) {
                elements.add(m_selection);
            }
            Object[] checkedObjs = m_treeViewer.getCheckedElements();
            for (Object o : checkedObjs) {
                AbstractExplorerFileStore f = (AbstractExplorerFileStore)o;
                if (isExportElement(f) || AbstractExplorerFileStore.isWorkflowGroup(f)) {
                    // add groups to also collect meta info files contained in there.
                    elements.add((AbstractExplorerFileStore)o);
                }

            }
            return elements;
        }

        return elements;
    }

    /**
     * @return The file name
     */
    public String getFileName() {
        return m_fileText.getText();
    }

    private static final String KEY_LOC = "destination-location";

    /**
     * Saves the last selected location (the parent of the last export file).
     *
     * @see WorkflowExportWizard#performFinish()
     */
    public void saveDialogSettings() {
        setLastSelectedExportLocation();
        IDialogSettings settings = getDialogSettings();
        if (settings != null) {
            settings.put(KEY_LOC, lastSelectedTargetLocation);
        }
    }

    /**
     *
     * @param viewer the viewer which items should be (un-)checked
     * @param element the element whose parents should be checked
     * @param state true for checked, false for uncheck
     */
    private void setParentTreeChecked(final AbstractExplorerFileStore element,
            final boolean state) {

        if (state) {
            // trivial case -> go up and set active
            AbstractExplorerFileStore child = element;
            AbstractExplorerFileStore parent = element.getParent();
            while (parent != null && parent != child) {
                if (!m_treeViewer.setChecked(parent, state)) {
                    break;
                }
                child = parent;
                parent = parent.getParent();
            }
        } else {
            // go up and set parents inactive that have no checked children.
            AbstractExplorerFileStore child = element;
            AbstractExplorerFileStore parent = element.getParent();
            while (parent != null && child != parent) {
                boolean hasCheckedChild = false;
                for (Object c : m_provider.getChildren(parent)) {
                    if (m_treeViewer.getChecked(c)) {
                        hasCheckedChild = true;
                        break;
                    }
                }
                if (!hasCheckedChild) {
                    if (!m_treeViewer.setChecked(parent, false)) {
                        // check mark couldn't be changed: done
                        break;
                    }
                    child = parent;
                    parent = parent.getParent();
                } else {
                    // parent stays checked - and with it all its grand-parents
                    break;
                }
            }
        }
    }

}
