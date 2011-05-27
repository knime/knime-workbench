/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (c) KNIME.com, Zurich, Switzerland
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
 *
 * Created: May 27, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer.view.actions;

import java.io.File;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.knime.workbench.explorer.filesystem.ExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.ui.metainfo.model.MetaInfoFile;

/**
 *
 * @author ohl, University of Konstanz
 */
public class NewWorkflowGroupWizard extends NewWorkflowWizard {

    /**
     * Creates the wizard.
     *
     * @param spaceProvider the content provider to create a group in
     */
    public NewWorkflowGroupWizard(final AbstractContentProvider spaceProvider) {
        super(spaceProvider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addPages() {
        NewWorkflowWizardPage page = new NewWorkflowWizardPage(getConentProvider(),
                getInitialSelection(), /* isWorkflow= */false);
        addPage(page);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doFinish(final ExplorerFileStore newItem,
            final IProgressMonitor monitor) throws CoreException {

        if (newItem.fetchInfo().exists()) {
            throwCoreException("Resource \"" + newItem.getFullName()
                    + "\" already exists.", null);
        }

        if (!newItem.getParent().fetchInfo().exists()) {
            throwCoreException("Parent directory doesn't exist. "
                    + "Create a workflow group before you place "
                    + "a workflow group in.", null);
        }

        // create workflow group dir
        newItem.mkdir(EFS.NONE, monitor);

        // create a new empty meta info file
        File locFile = newItem.toLocalFile(EFS.NONE, monitor);
        if (locFile == null) {
            // strange - can't create meta info file then
            return;
        }
        MetaInfoFile.createMetaInfoFile(locFile, false);
    }

}
