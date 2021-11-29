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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
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
        final String[][] backendOptions = TableBackendRegistry.getInstance().getTableBackends() //
            .stream() //
            .map(t -> new String[]{t.getShortName(), t.getClass().getName()}) //
            .toArray(String[][]::new);
        m_backendSelectionEditor = new RadioGroupFieldEditor(TableBackendRegistry.PREF_KEY_TABLE_BACKEND,
            "Table backend for new workflows", 1, backendOptions, parent);
        addField(m_backendSelectionEditor);

        // TODO show the description
        // The description is HTML and I did not find a widget for that
        // Tried "Browser" (looks strange) and "FormText" (does not accept the markup)
    }
}
