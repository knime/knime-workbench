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
  *   Sep 1, 2011 (morent): created
  */

package org.knime.workbench.explorer.localworkspace;

import org.eclipse.core.filesystem.IFileStore;
import org.knime.core.node.workflow.SingleNodeContainerPersistorVersion200;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileInfo;

/**
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class LocalWorkspaceFileInfo extends AbstractExplorerFileInfo {
    private final IFileStore m_file;

    /**
     * @param file The file store this file info belongs to
     */
    LocalWorkspaceFileInfo(final IFileStore file) {
        super(file.getName());
        m_file = file;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exists() {
        return m_file.fetchInfo().exists();
    }

    /**
     * @return true if the file represents a workflow, false otherwise
     */
    @Override
    public boolean isWorkflow() {
        return exists() && isWorkflow(m_file);
    }

    /**
     * @return true if the file represents a workflow group, false otherwise
     */
    @Override
    public boolean isWorkflowGroup() {
        return exists() && isWorkflowGroup(m_file);
    }

    /**
     * @return the isWorkflowTemplate
     */
    @Override
    public boolean isWorkflowTemplate() {
        return exists() && isWorkflowTemplate(m_file);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNode() {
        return exists() && isNode(m_file);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMetaNode() {
        return exists() & isMetaNode(m_file);
    }

    private static boolean isWorkflow(final IFileStore file) {
        if (file == null || !file.fetchInfo().exists()) {
            return false;
        }
        IFileStore wfFile = file.getChild(WorkflowPersistor.WORKFLOW_FILE);
        IFileStore parentFile = file.getParent();
        if (parentFile == null) {
            return false;
        }
        IFileStore parentWfFile = parentFile.getChild(
                WorkflowPersistor.WORKFLOW_FILE);
        return wfFile.fetchInfo().exists()
                && !parentWfFile.fetchInfo().exists();
    }

    private static boolean isWorkflowGroup(final IFileStore file) {
        if (file == null || !file.fetchInfo().exists()) {
            return false;
        }
        return file.fetchInfo().isDirectory() && !isWorkflow(file)
                && !isMetaNode(file) && !isNode(file)
                && !isWorkflowTemplate(file);
    }

    private static boolean isWorkflowTemplate(final IFileStore file) {
        if (file == null || !file.fetchInfo().exists()) {
            return false;
        }
        IFileStore templateFile = file.getChild(
                WorkflowPersistor.TEMPLATE_FILE);
        return templateFile.fetchInfo().exists();
    }

    private static boolean isMetaNode(final IFileStore file) {
        if (file == null || !file.fetchInfo().exists()) {
            return false;
        }
        IFileStore wfFile = file.getChild(WorkflowPersistor.WORKFLOW_FILE);
        IFileStore parentFile = file.getParent();
        if (parentFile == null) {
            return false;
        }
        IFileStore parentWfFile = parentFile.getChild(
                WorkflowPersistor.WORKFLOW_FILE);
        return wfFile.fetchInfo().exists() && parentWfFile.fetchInfo().exists();
    }

    private static boolean isNode(final IFileStore file) {
        if (file == null || !file.fetchInfo().exists() || isMetaNode(file)) {
            return false;
        }
        IFileStore containerFile = file.getChild(
                SingleNodeContainerPersistorVersion200.NODE_FILE);
        return containerFile.fetchInfo().exists()
                && isWorkflow(file.getParent());
    }
}
