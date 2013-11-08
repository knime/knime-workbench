/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2013
 * KNIME.com, Zurich, Switzerland
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
     * @param errorLabel an error label, which can be filled by the panel
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

    }
}
