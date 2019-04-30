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
 */
package org.knime.workbench.explorer.templates.preferences;

import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.ListEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.ui.preferences.HorizontalLineField;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * Preference page regarding metanode templates in the explorer.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class MetaNodeTemplatesPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private ComboFieldEditor m_linkTemplateEditor;

    private ListEditor m_consideredWorkflowGroups;

    private BooleanFieldEditor m_addToNodeRepo;


    /**
     * Constructor.
     */
    public MetaNodeTemplatesPreferencePage() {
        super("Explorer Metanode Template Settings", null, GRID);
        setDescription("Preferences regarding metanode templates in the KNIME Explorer.");
    }

    /**
     * Creates the field editors. Field editors are abstractions of the common GUI blocks needed to manipulate various
     * types of preferences. Each field editor knows how to save and restore itself.
     */
    @Override
    public void createFieldEditors() {
        final Composite parent = getFieldEditorParent();

        m_linkTemplateEditor = new ComboFieldEditor(
            PreferenceConstants.P_EXPLORER_LINK_ON_NEW_TEMPLATE,
            "Link metanode when defining new template",
            new String[][] {
                    {"Never", MessageDialogWithToggle.NEVER},
                    {"Prompt", MessageDialogWithToggle.PROMPT},
            }, getFieldEditorParent());
        addField(m_linkTemplateEditor);

        addField(new HorizontalLineField(parent));

        m_addToNodeRepo = new BooleanFieldEditor(PreferenceConstants.P_EXPLORER_ADD_TEMPLATES_TO_NODE_REPO,
            "Add metanode templates to node repository", parent) {
            @Override
            protected void valueChanged(final boolean oldValue, final boolean newValue) {
                super.valueChanged(oldValue, newValue);
                m_consideredWorkflowGroups.setEnabled(newValue, parent);
            }
        };
        addField(m_addToNodeRepo);
        m_consideredWorkflowGroups = new WorkflowGroupListEditor(
            PreferenceConstants.P_EXPLORER_TEMPLATE_WORKFLOW_GROUPS_TO_NODE_REPO,
            "Select the mount points and workflow groups that shall"
            + "\ncontribute metanode templates to the node repository",
            parent);
        addField(m_consideredWorkflowGroups);

        new Label(parent, SWT.NONE).setText("Note for local workspace: only wrapped metanodes are considered");
    }

    /** {@inheritDoc} */
    @Override
    public void init(final IWorkbench workbench) {
        setPreferenceStore(ExplorerActivator.getDefault().getPreferenceStore());
    }

    /** {@inheritDoc} */
    @Override
    protected void initialize() {
        super.initialize();
        m_consideredWorkflowGroups.setEnabled(m_addToNodeRepo.getBooleanValue(), getFieldEditorParent());
    }
}
