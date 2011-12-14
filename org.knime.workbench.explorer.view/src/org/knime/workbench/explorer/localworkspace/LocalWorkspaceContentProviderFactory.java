/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2011
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
package org.knime.workbench.explorer.localworkspace;

import org.eclipse.swt.graphics.Image;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.AbstractContentProviderFactory;
import org.knime.workbench.explorer.view.IconFactory;


/**
 * Creates a content provider for the KNIME Workflows in the local workspace of
 * the current workbench.
 *
 * @author ohl, University of Konstanz
 */
public class LocalWorkspaceContentProviderFactory extends
        AbstractContentProviderFactory {

    /**
     * The id of this predefined and always existing content provider.
     */
    public static final String ID = "org.knime.workbench.explorer.workspace";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getID() {
        return ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractContentProvider createContentProvider(final String id) {
        return new LocalWorkspaceContentProvider(this, id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean multipleInstances() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultMountID() {
        return "LOCAL";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Local Workspace";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Image getImage() {
        return IconFactory.instance.localWorkspace();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractContentProvider createContentProvider(final String mountID,
            final String content) {
        return new LocalWorkspaceContentProvider(this, mountID);
    }

}
