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
 *   8 Oct 2020 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.workbench.ui.preferences;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.knime.core.data.TableBackend;
import org.knime.core.data.TableBackendRegistry;
import org.osgi.framework.FrameworkUtil;

/**
 * This is the preference page for KNIME table backends.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public final class TableBackendPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    /** The name of "org.knime.core". */
    private static final String CORE_BUNDLE_SYMBOLIC_NAME =
        FrameworkUtil.getBundle(TableBackendRegistry.class).getSymbolicName();

    static final ScopedPreferenceStore CORE_STORE =
        new ScopedPreferenceStore(InstanceScope.INSTANCE, CORE_BUNDLE_SYMBOLIC_NAME);

    private RadioGroupFieldEditor m_backendSelectionEditor;

    private FormText m_descriptionText;

    /**
     * Creates a new (empty) table backend preference page.
     */
    public TableBackendPreferencePage() {
        setDescription("This subsection contains preferences for KNIME table backends. "
            + "Select the table backend which should be used for new workflows here.");
    }

    @Override
    public void init(final IWorkbench workbench) {
        setPreferenceStore(CORE_STORE);
        if (TableBackendRegistry.getInstance().isBackendPropertySet()) {
            setMessage("The VM property \"" + TableBackendRegistry.PROPERTY_TABLE_BACKEND_IMPLEMENTATION + "\" is set. "
                + "The configuration on this page will be ignored.", WARNING);
        }
    }

    @Override
    protected void createFieldEditors() {
        final Composite parent = getFieldEditorParent();

        // Backend selection
        final String[][] backendOptions = TableBackendRegistry.getInstance().getTableBackends() //
            .stream() //
            .map(t -> new String[]{t.getShortName(), t.getClass().getName()}) //
            .toArray(String[][]::new);
        m_backendSelectionEditor = new RadioGroupFieldEditor(TableBackendRegistry.PREF_KEY_TABLE_BACKEND,
            "Table backend for new workflows", 1, backendOptions, parent) {
            @Override
            protected void fireValueChanged(final String property, final Object oldValue, final Object newValue) {
                super.fireValueChanged(property, oldValue, newValue);
                updateDescription((String)newValue);
            }
        };
        addField(m_backendSelectionEditor);

        // Separator
        final Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));

        // Description
        m_descriptionText = new FormText(parent, SWT.NO_FOCUS) {
            @Override
            public Point computeSize(final int wHint, final int hHint, final boolean changed) {
                // Prevent this FormText from requesting more than 500px width
                if (wHint == SWT.DEFAULT || wHint > 500) {
                    return super.computeSize(500, hHint, changed);
                }
                return super.computeSize(wHint, hHint, changed);
            }
        };
        m_descriptionText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
        updateDescription(CORE_STORE.getString(TableBackendRegistry.PREF_KEY_TABLE_BACKEND));
    }

    @Override
    protected void performDefaults() {
        super.performDefaults();
        updateDescription(m_backendSelectionEditor.getSelectionValue());
    }

    /** Update the description text using the description from the given table backend */
    private void updateDescription(final String tableBackendName) {
        final TableBackend selectedBackend = TableBackendRegistry.getInstance().getTableBackend(tableBackendName);
        final String description = selectedBackend.getDescription();
        m_descriptionText.setText(description, false, false);
        m_descriptionText.getParent().getParent().layout(true, true);
    }
}
