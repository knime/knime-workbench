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
  *
  * History
  *   Oct 28, 2011 (morent): created
  */

package org.knime.workbench.explorer.view.actions;

import java.io.File;

import org.knime.core.util.FileUtil;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;

/**
 * Downloads workflows and workflow templates to a temporary directory.
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class TempDownloadAction extends AbstractDownloadAction {

    /**
    *
    * Creates a new action with the given text.
    *
    * @param text the string used as the text for the action, or null if there
    *            is no text
    * @param source the source file store containing the workflow
    * @param targetDir the target directory to download the workflow to
    */
    public TempDownloadAction(final RemoteExplorerFileStore source,
            final File targetDir) {
       super("Download", source, targetDir);
    }

    /**
     * @return true if the download source provided by {@link #getSourceFile()}
     *      represents a workflow or a workflow template
     */
    @Override
    protected boolean isSourceSupported() {
        RemoteExplorerFileStore sourceFile = getSourceFile();
        return AbstractExplorerFileStore.isWorkflow(sourceFile)
                || AbstractExplorerFileStore.isWorkflowTemplate(sourceFile);
    }

    /**
     * @return a message containing the supported source types
     */
    @Override
    protected String getUnsupportedSourceMessage() {
        return "Only worklows and workflow templates can be downloaded.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void extractDownloadToTarget(final File zipFile)
            throws Exception {
        FileUtil.unzip(zipFile, getTargetDir());
    }

}
