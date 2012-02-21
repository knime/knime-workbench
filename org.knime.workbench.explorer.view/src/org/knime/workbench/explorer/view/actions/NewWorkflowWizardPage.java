/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * Created: May 19, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer.view.actions;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.dialogs.SpaceResourceSelectionDialog;
import org.knime.workbench.explorer.dialogs.SpaceResourceSelectionDialog.SelectionValidator;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.knime.workbench.explorer.view.ContentObject;

/**
 *
 * @author ohl, University of Konstanz
 */
public class NewWorkflowWizardPage extends WizardPage {

    private static final String INITIAL_PROJECT_NAME = "KNIME_project";

    private static final String DEFAULT_WORKFLOW_GROUP_NAME = "workflow_group";

    private static final String WORKFLOW_GROUP = "Workflow Group";

    private static final String WORKFLOW = "Workflow";

    private static final ImageDescriptor ICON = ExplorerActivator
            .imageDescriptorFromPlugin(ExplorerActivator.PLUGIN_ID,
                    "icons/new_knime55.png");

    private AbstractExplorerFileStore m_parent;

    private Text m_projectNameUI;

    private String m_projectName;

    private Text m_destinationUI;

    private final boolean m_isWorkflow;

    private final String m_elementName;

    private final String[] m_mountIDs;

    /**
     * Create and init the page.
     *
     * @param mountIDs the IDs of the mount points to show
     * @param selection the initial selection
     * @param isWorkflow true if used to create a workflow, false if used to
     *            create a workflow group
     *
     */
    public NewWorkflowWizardPage(final String[] mountIDs,
            final AbstractExplorerFileStore selection,
            final boolean isWorkflow) {
        super("NewWorkflowWizardPage");

        m_isWorkflow = isWorkflow;
        if (m_isWorkflow) {
            m_elementName = WORKFLOW;
        } else {
            m_elementName = WORKFLOW_GROUP;
        }
        setTitle("New KNIME " + m_elementName + " Wizard");
        setDescription("Create a new KNIME " + m_elementName.toLowerCase()
                + ".");
        setImageDescriptor(ICON);
        if (selection != null) {
            m_parent = selection;
        } else {
            // set the parent to the root of the first selected content provider
            m_parent = ExplorerMountTable.getMountPoint(mountIDs[0])
                    .getProvider().getFileStore("/");
        }
        m_mountIDs = mountIDs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createControl(final Composite parent) {
        Composite overall = new Composite(parent, SWT.NULL);
        overall.setLayout(new GridLayout(1, false));
        createNameComposite(overall);
        createDestinationSelectionComposite(overall);
        setControl(overall);
    }

    private void createNameComposite(final Composite parent) {
        Group nameGroup = new Group(parent, SWT.NONE);
        nameGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        nameGroup.setLayout(new GridLayout(2, false));
        // first row: workflow name
        final Label label = new Label(nameGroup, SWT.NONE);
        label.setText("Name of the " + m_elementName.toLowerCase()
                + " to create:");
        m_projectNameUI = new Text(nameGroup, SWT.BORDER);
        m_projectNameUI.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(final ModifyEvent e) {
                dialogChanged();
            }
        });
        m_projectNameUI.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        // initialize the new project name field
        String initialProjectName = INITIAL_PROJECT_NAME;
        if (!m_isWorkflow) {
            initialProjectName = DEFAULT_WORKFLOW_GROUP_NAME;
        }
        // make sure the initial default name doesn't exist yet
        String projectName = initialProjectName;
        int add = 2;
        IFileStore newFile = m_parent.getChild(projectName);
        while (newFile.fetchInfo().exists()) {
            projectName = initialProjectName + add++;
            newFile = m_parent.getChild(projectName);
        }
        m_projectNameUI.setText(projectName);
        m_projectNameUI.setSelection(0, projectName.length());
        m_projectName = projectName;
    }

    private void createDestinationSelectionComposite(final Composite parent) {
        Group destGroup = new Group(parent, SWT.SHADOW_ETCHED_IN);
        destGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        destGroup.setLayout(new GridLayout(3, false));
        // second row: workflow destination
        Label destinationLabel = new Label(destGroup, SWT.NONE);
        destinationLabel.setText("Destination of new "
                + m_elementName.toLowerCase() + " :");
        m_destinationUI = new Text(destGroup, SWT.BORDER | SWT.READ_ONLY);
        m_destinationUI.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        m_destinationUI.setText(m_parent.getMountIDWithFullPath());
        Button browseBtn = new Button(destGroup, SWT.PUSH);
        browseBtn.setText("Browse...");
        browseBtn.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }

            @Override
            public void widgetSelected(final SelectionEvent e) {
                handleBrowseButton();
            }
        });
    }

    private void handleBrowseButton() {
        SpaceResourceSelectionDialog dlg =
                new SpaceResourceSelectionDialog(getShell(),
                        m_mountIDs,
                        ContentObject.forFile(m_parent));
        dlg.setTitle("Destination Selection");
        dlg.setHeader("Select a new destination.");
        dlg.setDescription("Please select the destination directory in the "
                + m_parent.getMountID() + " TeamSpace");
        dlg.setValidator(new SelectionValidator() {
            @Override
            public String isValid(final AbstractExplorerFileStore selection) {
                String msg = "Please select a directory or workflow group.";
                if (AbstractExplorerFileStore.isWorkflowGroup(selection)) {
                    return null;
                } else {
                    return msg;
                }
            }
        });
        if (dlg.open() != IDialogConstants.OK_ID) {
            return;
        }
        m_parent = dlg.getSelection();
        m_destinationUI.setText(m_parent.getMountIDWithFullPath());
        dialogChanged();
    }

    private void updateStatus(final String message) {
        setErrorMessage(message);
        setPageComplete(message == null);
    }

    /**
     * Ensures that the text field is set properly.
     */
    private void dialogChanged() {
        // check if a name was entered
        String projectName = m_projectNameUI.getText().trim();
        if (projectName.length() == 0) {
            updateStatus("Please enter a new for the new "
                    + m_elementName.toLowerCase());
            return;
        }
        if (!ExplorerFileSystem.isValidFilename(projectName)) {
            updateStatus(projectName + " is not a valid "
                    + m_elementName.toLowerCase() + " name");
            return;
        }
        // check whether this container already exists
        IFileStore newFile = m_parent.getChild(projectName);
        if (newFile.fetchInfo().exists()) {
            updateStatus("A " + m_elementName.toLowerCase()
                    + " with the provided name already exists.");
            return;
        }
        m_projectName = projectName;
        updateStatus(null);
    }

    /**
     *
     * @return a file store containing the new path
     */
    public AbstractExplorerFileStore getNewFile() {
        return m_parent.getChild(m_projectName);
    }

}
