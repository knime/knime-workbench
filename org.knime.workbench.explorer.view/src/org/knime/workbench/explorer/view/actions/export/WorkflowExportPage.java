/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (c) KNIME.com, Zurich, Switzerland
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
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.AbstractTreeViewer;
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
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.filesystem.ExplorerFileStore;
import org.knime.workbench.explorer.view.ContentObject;
import org.knime.workbench.explorer.view.dialogs.SpaceResourceSelectionDialog;
import org.knime.workbench.explorer.view.dialogs.SpaceResourceSelectionDialog.SelectionValidator;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 * Page to enter the select the workflows to export and enter the destination.
 *
 * @author Christoph Sieb, University of Konstanz
 * @author Fabian Dill, KNIME.com GmbH, Zurich, Switzerland
 */
public class WorkflowExportPage extends WizardPage {

    private static final String[] FILTER_EXTENSION = {"*.zip"};

    private Text m_containerText;

    private Text m_fileText;

    private Button m_excludeData;

    private ExplorerFileStore m_selection;

    private ExplorerFileStoreProvider m_provider;

    private CheckboxTreeViewer m_treeViewer;

    private static String lastSelectedTargetLocation;

    private static final ImageDescriptor ICON = ExplorerActivator
            .imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
                    "icons/knime_export55.png");

    /**
     * Constructor for NewWorkflowPage.
     *
     * @param selection The initial selection
     */
    public WorkflowExportPage(final ExplorerFileStore selection) {
        super("wizardPage");
        setTitle("Export KNIME workflow(s)");
        setDescription("Exports KNIME workflows.");
        setImageDescriptor(ICON);
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
        Composite container = new Composite(parent, SWT.NULL);
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

        label = new Label(exportGroup, SWT.NULL);
        label.setText("Destination archive file name (zip):");

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
        m_excludeData.setText("Exclude data from export.");

        createTreeViewer(container);

        initialize();
        containerSelectionChanged();
        dialogChanged();
        setControl(container);
    }

    private void createTreeViewer(final Composite parent) {
        m_treeViewer = new CheckboxTreeViewer(parent);
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
                // first expand in order to be able to check children as well
                m_treeViewer.expandToLevel(o, AbstractTreeViewer.ALL_LEVELS);
                m_treeViewer.setSubtreeChecked(o, isChecked);
                if (o instanceof ExplorerFileStore) {
                    ExplorerFileStore sel = (ExplorerFileStore)o;
                    setParentTreeChecked(sel, isChecked);
                }
                dialogChanged();
            }

        });
        m_treeViewer.getTree().setVisible(false);
    }

    /**
     * Tests if the current workbench selection is a suitable container to use.
     */
    private void initialize() {

        ExplorerFileStore sel = m_selection;
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

    private void updateFileName(final ExplorerFileStore sel) {
        if (sel != null) {
            String fileName = sel.getName() + ".zip";
            if (sel.getFullName().equals("/")) {
                fileName = "knime-export.zip";
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
        SpaceResourceSelectionDialog dlg =
                new SpaceResourceSelectionDialog(getShell(), ExplorerMountTable
                        .getAllMountIDs().toArray(new String[0]), initSel,
                        "Select workflow or group to export");
        dlg.setValidator(new SelectionValidator() {
            @Override
            public String isValid(final ExplorerFileStore selection) {
                if (!(ExplorerFileStore.isDirOrWorkflowGroup(selection)
                        || ExplorerFileStore.isWorkflow(selection))) {
                    return "Please select a workflow or workflow group";
                }
                return null;
            }
        });
        // dlg.expand(2);
        if (dlg.open() != Window.OK) {
            return;
        }
        ExplorerFileStore sel = dlg.getSelection();
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
//        // reset checked elements
//        m_treeViewer.expandAll();
//        m_treeViewer.setAllChecked(false);

        if (ExplorerFileStore.isWorkflow(m_selection)) {
            m_treeViewer.getTree().setVisible(false);
        } else {
            // clear the selection before setting a new input!!!
            m_treeViewer.setSelection(null);
            m_treeViewer.setInput(m_selection);
            m_treeViewer.getTree().setVisible(true);
            selectAllWorkflows();
        }

        m_containerText.setText(m_selection.getMountIDWithFullPath());
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
                + "exported workflows in.");
        if (m_fileText.getText() != null
                && !m_fileText.getText().trim().isEmpty()) {
            String exportString = m_fileText.getText().trim();
            IPath p = new Path(exportString);
            if (p.segmentCount() > 1) {
                fileDialog.setFilterPath(p.removeLastSegments(1).toOSString());
                fileDialog.setFileName(p.lastSegment());
            }
        }
        String filePath = fileDialog.open().trim();
        if (filePath == null || filePath.isEmpty()) {
            return;
        }
        if (!filePath.toLowerCase().endsWith(".zip")) {
            filePath += ".zip";
        }
        m_fileText.setText(filePath);
        setLastSelectedExportLocation();
    }

    private void selectAllWorkflows() {
        ArrayList<Object> obj = new ArrayList<Object>();
        obj.add(m_selection);
        while (obj.size() > 0) {
            Object f = obj.remove(obj.size() - 1);
            if (f instanceof ExplorerFileStore) {
                ExplorerFileStore store = (ExplorerFileStore)f;
                if (ExplorerFileStore.isWorkflow(store)) {
                    m_treeViewer.setChecked(store, true);
                    setParentTreeChecked(store, true);
                    // workflows don't have no children
                    continue;
                }
            }
            Object[] childs = m_provider.getChildren(f);
            if (childs != null) {
                obj.addAll(Arrays.asList(childs));
            }
        }

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
            return "Please select the workflows to export.";
        }
        Collection<ExplorerFileStore> sel = getWorkflowsOrGroups();
        String msg = "Selection contains no workflows.";
        for (ExplorerFileStore store : sel) {
            if (ExplorerFileStore.isWorkflow(store)) {
                msg = null;
                break;
            }
        }
        return msg;
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
    public ExplorerFileStore getSelectedStore() {
        return m_selection;
    }

    /**
     *
     * @return checked workflows and workflow groups
     */
    public Collection<ExplorerFileStore> getWorkflowsOrGroups() {

        List<ExplorerFileStore> workflows = new ArrayList<ExplorerFileStore>();

        if (ExplorerFileStore.isWorkflow(m_selection)) {
            workflows.add(m_selection);
            return workflows;
        }

        if (ExplorerFileStore.isDirOrWorkflowGroup(m_selection)) {
            // also add the selected root - unless it is the "/"
            if (!"/".equals(m_selection.getFullName())) {
                workflows.add(m_selection);
            }
            Object[] checkedObjs = m_treeViewer.getCheckedElements();
            for (Object o : checkedObjs) {
                ExplorerFileStore file = (ExplorerFileStore)o;
                if (ExplorerFileStore.isDirOrWorkflowGroup(file)) {
                    workflows.add(file);
                } else if (ExplorerFileStore.isWorkflow(file)) {
                    workflows.add(file);
                }
            }
            return workflows;
        }

        return workflows;
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
    private void setParentTreeChecked(final ExplorerFileStore element,
            final boolean state) {

        if (state) {
            // trivial case -> go up and set active
            ExplorerFileStore child = element;
            ExplorerFileStore parent = element.getParent();
            while (parent != null && parent != child) {
                if (!m_treeViewer.setChecked(parent, state)) {
                    break;
                }
                child = parent;
                parent = parent.getParent();
            }
        } else {
            // go up and set parents inactive that have no checked children.
            ExplorerFileStore child = element;
            ExplorerFileStore parent = element.getParent();
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
