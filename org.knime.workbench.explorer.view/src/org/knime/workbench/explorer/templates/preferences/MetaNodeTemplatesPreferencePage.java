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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.templates.NodeRepoSyncSettings;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.ui.preferences.HorizontalLineField;
import org.knime.workbench.ui.preferences.PreferenceConstants;

/**
 * Preference page regarding metanode templates in the explorer.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class MetaNodeTemplatesPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private java.util.List<Control> m_composites = new ArrayList<>();

    private WorkflowGroupListEditor m_consideredWorkflowGroups;

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

        FieldEditor linkTemplateEditor = new ComboFieldEditor(
            PreferenceConstants.P_EXPLORER_LINK_ON_NEW_TEMPLATE,
            "Link metanode when defining new template",
            new String[][] {
                    {"Never", MessageDialogWithToggle.NEVER},
                    {"Prompt", MessageDialogWithToggle.PROMPT},
            }, getFieldEditorParent());
        addField(linkTemplateEditor);

        addField(new HorizontalLineField(parent));

        m_addToNodeRepo = new BooleanFieldEditor(PreferenceConstants.P_EXPLORER_ADD_TEMPLATES_TO_NODE_REPO,
            "Add metanode templates to node repository", parent) {
            @Override
            protected void valueChanged(final boolean oldValue, final boolean newValue) {
                super.valueChanged(oldValue, newValue);
                setWorkflowGroupConfigEnabled(newValue);
            }
        };
        addField(m_addToNodeRepo);
        m_consideredWorkflowGroups = new WorkflowGroupListEditor(
            PreferenceConstants.P_EXPLORER_TEMPLATE_WORKFLOW_GROUPS_TO_NODE_REPO,
            "Select the mount points and workflow groups that shall"
                + "\ncontribute metanode templates to the node repository.",
            parent);
        addField(m_consideredWorkflowGroups);

        Label notes = new Label(parent, SWT.NONE);
        notes.setText("Notes:\nSome server mount points cannot be selected here"
            + "\nbecause they provide their custom configuration (see below)."
            + "\nOnly wrapped metanodes are considered in the local workspace.\n");
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        notes.setLayoutData(gd);
        m_composites.add(notes);

        Group serverGroup = new Group(parent, SWT.NONE);
        serverGroup.setText("Server-configured workflow groups:");
        serverGroup.setLayout(new GridLayout(1, false));
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        serverGroup.setLayoutData(gd);
        m_composites.add(serverGroup);

        Label serverConfig = new Label(serverGroup, SWT.NONE);
        final StringBuilder sb = new StringBuilder();
        addPaths(sb, cp -> NodeRepoSyncSettings.getInstance().getServerConfiguredPaths(cp));
        serverConfig.setText(sb.toString());
        m_composites.add(serverConfig);

        Group defaultGroup = new Group(parent, SWT.NONE);
        defaultGroup.setText("Default workflow groups:");
        defaultGroup.setLayout(new GridLayout(1, false));
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        defaultGroup.setLayoutData(gd);
        m_composites.add(defaultGroup);

        Label defaultConfig = new Label(defaultGroup, SWT.NONE);
        sb.setLength(0);
        addPaths(sb, cp -> NodeRepoSyncSettings.getInstance().getDefaultPaths(cp));
        defaultConfig.setText(sb.toString());
        m_composites.add(defaultConfig);

    }

    private static void addPaths(final StringBuilder sb,
        final Function<AbstractContentProvider, Optional<List<String>>> pathProvider) {
        AtomicBoolean pathAppended = new AtomicBoolean(false);
        ExplorerMountTable.getMountedContent().values().forEach(cp -> {
            pathProvider.apply(cp).map(paths -> {
                for (String p : paths) {
                    sb.append(cp.getMountID());
                    sb.append(":");
                    sb.append(p);
                    sb.append("\n");
                }
                pathAppended.set(true);
                return paths;
            });
        });
        if (!pathAppended.get()) {
            sb.append("<None>");
        } else if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
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
        boolean enabled = m_addToNodeRepo.getBooleanValue();
        setWorkflowGroupConfigEnabled(enabled);
    }

    private void setWorkflowGroupConfigEnabled(final boolean enabled) {
        m_consideredWorkflowGroups.setEnabled(enabled, getFieldEditorParent());
        m_composites.forEach(c -> c.setEnabled(enabled));
    }
}
