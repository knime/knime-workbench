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
 * ------------------------------------------------------------------------
 */
package org.knime.workbench.explorer.view;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.knime.workbench.explorer.ExplorerMountTable;

/**
 *
 * @author ohl, University of Konstanz
 */
public abstract class AbstractContentProviderFactory {

    /**
     * @return a unique ID (e.g. "com.knime.explorer.filesystem", etc.)
     */
    public abstract String getID();

    /**
     * @return a readable name useful for the user (e.g. "Local Workspace",
     *         "Local File System", etc.)
     */
    @Override
    public abstract String toString();

    /**
     * @return an icon displayed in selection lists
     */
    public abstract Image getImage();

    /**
     *
     * @return true, if the factory can produce multiple content provider
     *         instances, false, if not more than one content provider must be
     *         created.
     */
    public abstract boolean multipleInstances();

    /**
     * Always return false. Except for the one temp space provider implementation.
     * @return false. Almost always.
     * @since 6.4
     */
    public boolean isTempSpace() {
        return false;
    }


    /**
     * @return a unique mount ID if this mount point should appear by default in
     *         the mount table and the explorer view. Or null, if it shouldn't
     *         be mounted by default. If an ID is returned the instantiation of
     *         the corresponding content provider must not open any dialog (or
     *         cause any other interaction).
     */
    public String getDefaultMountID() {
        return null;
    }

    /**
     * Returns whether the mountpoint can be edited.
     *
     * @return Whether the mountpoint can be edited
     * @since 8.4
     */
    public boolean isMountpointEditable() {
        return true;
    }

    /**
     * Not intended to be called. Rather go through the
     * {@link ExplorerMountTable}.
     *
     * <p>The caller needs to make sure the returned value is disposed when
     * no longer needed!
     *
     * @param mountID the mount ID the new content provider is mounted with
     *
     * @return a new, fully parameterized instance for a specific content
     *         provider or <code>null</code> if no provider can be created
     */
    public abstract AbstractContentProvider
        createContentProvider(final String mountID);

    /** Restore content provider. Caller needs to dispose returned value when
     * no longer needed!
     *
     * @param mountID the id of the mount to restore
     * @param content the string content representing the state of the content
     *            provider
     * @return a new instance with its state restored from the passed structure
     */
   public abstract AbstractContentProvider createContentProvider(
           final String mountID, final String content);

    /**
     * Indicates if additional information is needed by the factory for
     * creating a content provider. In general this information is gathered
     * by the factory in the {@link #createContentProvider(String)} by opening
     * a dialog.
     *
     * @return true, if additional information is needed by the factory for
     *      creating a content provider, false otherwise
     *
     * @since 3.0
     */
    public abstract boolean isAdditionalInformationNeeded();

    /**
     * If additional information is needed for creating a content provider,
     * the factory has to provide a {@link Composite} to allow input to be
     * included in other panels.
     *
     * @param parent the parent {@link Composite}
     * information input.
     * @param mountIDInput the input component which can be used to provide a default mount ID
     * @return the additional information panel
     * @since 6.0
     */
    public abstract AdditionalInformationPanel createAdditionalInformationPanel(
            Composite parent, Text mountIDInput);

    /**
     * @since 6.0
     */
    public abstract static class AdditionalInformationPanel {

        private Text m_mountIDInput;
        private Composite m_parent;
        private List<ValidationRequiredListener> m_listeners;

        /**
         * @param parent the parent composite
         * @param mountIDInput the text input used to fill the default mount ID into
         */
        protected AdditionalInformationPanel(final Composite parent, final Text mountIDInput) {
            m_parent = parent;
            m_mountIDInput = mountIDInput;
            m_listeners = new ArrayList<ValidationRequiredListener>();
        }

        /**
         * Creates and fills the panel.
         * @param content The settings content string or null if not present
         */
        public abstract void createPanel(String content);

        /**
         * Validate additional information.
         * @return validation error message or null if validation succeeds
         */
        public abstract String validate();

        /**
         * @return an {@link AbstractContentProvider} if it can be created from the panel's information.
         */
        public abstract AbstractContentProvider createContentProvider();

        /**
         * @return the parent
         */
        protected Composite getParent() {
            return m_parent;
        }

        /**
         * @return the mount ID
         */
        protected String getMountIDValue() {
            return m_mountIDInput.getText();
        }

        /**
         * Notifies listeners of a new default mount ID value.
         * @param value the mount ID
         */
        protected void setMountIDValue(final String value) {
            for (ValidationRequiredListener listener : m_listeners) {
                listener.defaultMountIDChanged(value);
            }
        }

        /**
         * @param listener
         */
        public void addValidationRequiredListener(final ValidationRequiredListener listener) {
            m_listeners.add(listener);
        }

        /**
         * @param listener
         */
        public void removeValidationRequiredListener(final ValidationRequiredListener listener) {
            m_listeners.remove(listener);
        }

        /**
         *
         */
        public void removeAllValidationRequiredListeners() {
            m_listeners.clear();
        }

        /**
         *
         */
        protected void notifyValidationListeners() {
            for (ValidationRequiredListener listener : m_listeners) {
                listener.validationRequired();
            }
        }

        /**
         * @return the current mount ID
         */
        protected String getCurrentMountID() {
            if (m_listeners.size() > 0) {
                return m_listeners.get(0).getCurrentMountID();
            }
            return null;
        }

        /**
         * @param label
         */
        protected void setMountIDLabel(final String label) {
            for (ValidationRequiredListener listener: m_listeners) {
                listener.setMountIDLabel(label);
            }
        }


        /**
         * Indicates that a background process is running that needs to be finished before the ok-button is enabled. The
         * provided message is displayed as an information. If no background thread is running, {@code null} has to be
         * returned.
         *
         * @return The loading message that is shown at the top, if a background process is running that needs to be
         *         finished, or {@code null} if no such process exists.
         * @since 8.3
         */
        public String getLoadMessage() {
            return null;
        }

        /**
         * Cancels background processes that may be still running when window is being disposed through cancel.
         *
         * @since 8.3
         */
        public void cancelBackgroundWork() {
            // do nothing
        }
    }

    /**
     * Interface for a listener which is called when validation is required
     * on additional panels.
     * @since 6.0
     */
    public static interface ValidationRequiredListener {

        /**
         * Called when validation is required.
         */
        public void validationRequired();

        /**
         * Called when the default mount ID changes.
         * @param defaultMountID the new default mount ID
         */
        public void defaultMountIDChanged(String defaultMountID);

        /**
         * @return the currently entered mount ID
         */
        public String getCurrentMountID();

        /**
         * Called when an additional panel wants to change the mount ID label.
         * @param label the new label
         */
        public void setMountIDLabel(String label);

    }
}
