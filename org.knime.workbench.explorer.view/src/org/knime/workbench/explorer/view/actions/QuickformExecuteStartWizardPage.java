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
 */
package org.knime.workbench.explorer.view.actions;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.ui.masterkey.CredentialVariablesDialog;
import org.knime.workbench.ui.wfvars.WorkflowVariablesDialog;

/**
 *
 * @author Thomas Gabriel, KNIME.com AG, Zurich
 * @author Dominik Morent, KNIME.com AG, Zurich
 * @since 3.1
 */
public class QuickformExecuteStartWizardPage extends WizardPage {

    /** The name of this page. */
    static final String PAGE_NAME = "QuickformExecuteStartWizardPage";

    private static final ImageDescriptor ICON = ExplorerActivator
            .imageDescriptorFromPlugin(ExplorerActivator.PLUGIN_ID,
                    "icons/new_knime55.png");

    private WorkflowVariablesDialog m_wfmVars;
    private CredentialVariablesDialog m_credDialog;

    /**
     * Create a new quickform start wizard page.
     * @param wizard parent wizard
     */
    QuickformExecuteStartWizardPage(final QuickformExecuteWizard wizard) {
        super(PAGE_NAME);
        setTitle("QuickForm Execution Wizard");
        setDescription("Stepwise Execution of a Workflow using QuickForm "
                + "nodes.\nShows global variables and credentials defined on "
                + "the workflow.");
        setImageDescriptor(ICON);
        setWizard(wizard);
    }

    /** {@inheritDoc} */
    @Override
    public boolean canFlipToNextPage() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public IWizardPage getNextPage() {
        if (m_wfmVars != null) {
            m_wfmVars.okPressed();
        }
        if (m_credDialog != null) {
            m_credDialog.okPressed();
        }
        return super.getNextPage();
    }

    /** {@inheritDoc} */
    @Override
    public void createControl(final Composite parent) {
        Composite overall = new Composite(parent, SWT.NULL);
        overall.setLayout(new GridLayout(1, false));
        final WorkflowManager wfm = getWizard().getWorkflowManager();
        if (wfm != null) {
            if (!wfm.getWorkflowVariables().isEmpty()) {
                m_wfmVars = new WorkflowVariablesDialog(getShell(), wfm);
                Group group1 = new Group(overall, SWT.SHADOW_ETCHED_IN);
                group1.setText("Workflow Variables");
                group1.setLayout(new GridLayout(1, true));
                group1.setLayoutData(new GridData(SWT.FILL));
                m_wfmVars.createDialogArea(group1, true);
            }
            if (!wfm.getCredentialsStore().listNames().isEmpty()) {
                m_credDialog = new CredentialVariablesDialog(getShell(),
                        wfm.getCredentialsStore());
                Group group2 = new Group(overall, SWT.SHADOW_ETCHED_IN);
                group2.setText("Workflow Credentials");
                group2.setLayout(new GridLayout(1, true));
                group2.setLayoutData(new GridData(SWT.FILL));
                m_credDialog.createDialogArea(group2, true);
            }
            if ((m_wfmVars == null) && (m_credDialog == null)) {
                CLabel l = new CLabel(overall, SWT.CENTER);
                l.setText("No workflow variables or credentials are defined "
                        + "for the current workflow.");
                l.setImage(ImageRepository.getIconImage(SharedImages.Info));
            }
        }
        setControl(overall);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QuickformExecuteWizard getWizard() {
        return (QuickformExecuteWizard)super.getWizard();
    }

}
