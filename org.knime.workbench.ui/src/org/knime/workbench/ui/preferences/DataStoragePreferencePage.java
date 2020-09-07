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
 * History
 *   Jan 07, 2018 (wiswedel): created
 */
package org.knime.workbench.ui.preferences;

import java.util.Objects;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.knime.core.data.container.IDataContainerFactoryRegistry;
import org.knime.core.data.container.storage.TableStoreFormatRegistry;
import org.knime.core.node.BufferedDataContainer;
import org.osgi.framework.FrameworkUtil;

/**
 * (Parent) Preference page to select table backend settings such as persistence format (old style KNIME + column store
 * vs pure column store).
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public class DataStoragePreferencePage extends FieldEditorPreferencePage
    implements IWorkbenchPreferencePage, IPreferenceChangeListener {

    /** The name of "org.knime.core". */
    private static final String CORE_BUNDLE_SYMBOLIC_NAME =
        FrameworkUtil.getBundle(TableStoreFormatRegistry.class).getSymbolicName();

    static final ScopedPreferenceStore CORE_STORE =
        new ScopedPreferenceStore(InstanceScope.INSTANCE, CORE_BUNDLE_SYMBOLIC_NAME);

    private RadioGroupFieldEditor m_rowContainerFactoryEditor;

    private RadioGroupFieldEditor m_tableStoreFormatEditor;

    /**
     * Creates a new preference page.
     */
    public DataStoragePreferencePage() {
        super(GRID);
        setDescription("Select the table store backend and table store format (in case of default backend) used to "
            + "store data. This applies to temporary data and final table results that are persisted as part of an "
            + "executed workflow.\n"
            + "Additional options become available once the corresponding extensions are installed.");
    }

    @Override
    protected void createFieldEditors() {
        String[][] rowContainerFactoryLabelsAndText =
            IDataContainerFactoryRegistry.getInstance().getRowContainerFactories().stream()
                .map(f -> new String[]{f.getName(), f.getClass().getName()}).toArray(String[][]::new);
        m_rowContainerFactoryEditor =
            new RadioGroupFieldEditor(IDataContainerFactoryRegistry.PREF_KEY_ROWCONTAINER_FACTORY, "KNIME Table Backend",
                1, rowContainerFactoryLabelsAndText, getFieldEditorParent());
        addField(m_rowContainerFactoryEditor);

        // maps the table store format human readable name to the fully qualified class name
        String[][] tableStoreFormatLabelsAndText = TableStoreFormatRegistry.getInstance().getTableStoreFormats()
            .stream().map(f -> new String[]{f.getName(), f.getClass().getName()}).toArray(String[][]::new);
        m_tableStoreFormatEditor = new RadioGroupFieldEditor(TableStoreFormatRegistry.PREF_KEY_STORAGE_FORMAT,
            "KNIME Table Storage Format", 1, tableStoreFormatLabelsAndText, getFieldEditorParent());
        addField(m_tableStoreFormatEditor);

        // NB: in case default value changes at some point, we have to change this behaviour here again.
        IDataContainerFactoryRegistry.getInstance().getInstanceRowContainerFactory();
        checkEnablementOfTableStoreFormatEditor(
            IDataContainerFactoryRegistry.getInstance().getInstanceRowContainerFactory().getClass().getName());

        DefaultScope.INSTANCE.getNode(CORE_BUNDLE_SYMBOLIC_NAME).addPreferenceChangeListener(this);
    }

    @Override
    public void init(final IWorkbench workbench) {
        setPreferenceStore(CORE_STORE);

    }

    @Override
    public void dispose() {
        DefaultScope.INSTANCE.getNode(CORE_BUNDLE_SYMBOLIC_NAME).removePreferenceChangeListener(this);
    }

    /**
     * The field editor preference page implementation of this <code>IPreferencePage</code> (and
     * <code>IPropertyChangeListener</code>) method intercepts <code>IS_VALID</code> events but passes other events on
     * to its superclass.
     */
    @Override
    public void propertyChange(final PropertyChangeEvent event) {
        super.propertyChange(event);
        if (event.getSource() == m_rowContainerFactoryEditor) {
            m_tableStoreFormatEditor.setEnabled(event.getNewValue().equals(BufferedDataContainer.class.getName()),
                getFieldEditorParent());
        }
    }

    @Override
    public void preferenceChange(final PreferenceChangeEvent event) {
        // The default preferences may change while this page is open (e.g. by an action on other preference page, e.g.
        // Novartis). If this editor is showing only default preference it will not updated its contents with the new
        // default preference and then store the old default preference as user settings once the preference dialog is
        // closed with OK. This listener updates the components when the default preferences change.
        if (TableStoreFormatRegistry.PREF_KEY_STORAGE_FORMAT.equals(event.getKey())) {
            m_tableStoreFormatEditor.load();
        }

        if (IDataContainerFactoryRegistry.PREF_KEY_ROWCONTAINER_FACTORY.equals(event.getKey())) {
            m_rowContainerFactoryEditor.load();
            checkEnablementOfTableStoreFormatEditor(event.getNewValue());
        }
    }

    private void checkEnablementOfTableStoreFormatEditor(final Object selectedTableStoreFormat) {
        m_tableStoreFormatEditor.setEnabled(
            Objects.equals(selectedTableStoreFormat, BufferedDataContainer.class.getName()),
            getFieldEditorParent());
    }
}
