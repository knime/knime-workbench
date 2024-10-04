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
 * -------------------------------------------------------------------
 *
 * History
 *   13.04.2011 (Bernd Wiswedel): created
 */
package org.knime.workbench.editor2.commands;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.MetaNodeTemplateInformation;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeUIInformation;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.UnsupportedWorkflowVersionException;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor.MetaNodeLinkUpdateResult;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.workbench.editor2.LoadMetaNodeTemplateRunnable;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;

/**
 * GEF command for adding a MetaNode from a file location to the workflow.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich
 */
public class CreateMetaNodeTemplateCommand extends AbstractKNIMECommand {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(CreateMetaNodeTemplateCommand.class);

    private final URI m_templateURI;

    /**
     * Location of the new metanode template.
     * @since 2.12
     */
    protected final Point m_location;

    /**
     * Snap metanode template to grid.
     * @since 2.12
     */
    protected final boolean m_snapToGrid;

    /**
     * Container of the metanode template.
     * @since 2.12
     */
    protected NodeContainer m_container;

    private final boolean m_isRemoteLocation;


    /**
     * Creates a new command.
     *
     * @param manager The workflow manager that should host the new node
     * @param templateFolder the directory underlying the template
     * @param location Initial visual location in the
     * @param snapToGrid if node should be placed on closest grid location
     * @throws IllegalArgumentException if the passed file store doesn't represent a workflow template
     */
    public CreateMetaNodeTemplateCommand(final WorkflowManager manager, final AbstractExplorerFileStore templateFolder,
        final Point location, final boolean snapToGrid) {
        super(manager);
        if (!AbstractExplorerFileStore.isWorkflowTemplate(templateFolder)) {
            throw new IllegalArgumentException(
                "Provided workflow '" + templateFolder.getMountIDWithFullPath() + "' is not a template");
        }
        m_templateURI = templateFolder.toURI();
        m_location = location;
        m_snapToGrid = snapToGrid;
        m_isRemoteLocation = templateFolder instanceof RemoteExplorerFileStore;
    }

    /**
     * Creates a new command.
     *
     * @param manager The workflow manager that should host the new node
     * @param templateURI the URI to the directory or file of the underlying the template
     * @param location Initial visual location in the
     * @param snapToGrid if node should be placed on closest grid location
     * @param isRemoteLocation if the workflow template needs to be downloaded first (determines whether to show a busy
     *            cursor on command execution)
     */
    public CreateMetaNodeTemplateCommand(final WorkflowManager manager, final URI templateURI, final Point location,
        final boolean snapToGrid, final boolean isRemoteLocation) {
        super(manager);
        m_templateURI = templateURI;
        m_location = location;
        m_snapToGrid = snapToGrid;
        m_isRemoteLocation = isRemoteLocation;
    }

    /** We can execute, if all components were 'non-null' in the constructor.
     * {@inheritDoc} */
    @Override
    public boolean canExecute() {
        if (!super.canExecute()) {
            return false;
        }
        return m_location != null && m_templateURI != null;
    }

    /** {@inheritDoc} */
    @Override
    public void execute() {
        createMetaNodeTemplate(getHostWFM(), m_templateURI, m_location.x, m_location.y, m_isRemoteLocation,
            m_snapToGrid);
    }

    /**
     * Creates a meta-node template (i.e. adds a component or metanode from a url to the workflow).
     *
     * @param wfm
     * @param templateURI
     * @param x
     * @param y
     * @param isRemoteLocation
     * @param snapToGrid
     * @return the added node or {@code null} if it failed
     */
    public static NodeContainer createMetaNodeTemplate(final WorkflowManager wfm, final URI templateURI, final int x, // NOSONAR
        final int y, final boolean isRemoteLocation, final boolean snapToGrid) {
        // Add node to workflow and get the container
        LoadMetaNodeTemplateRunnable loadRunnable = null;
        NodeContainer container = null;
        try {
            IWorkbench wb = PlatformUI.getWorkbench();
            IProgressService ps = wb.getProgressService();
            // this one sets the workflow manager in the editor
            loadRunnable = new LoadMetaNodeTemplateRunnable(wfm, templateURI);
            if (isRemoteLocation) {
                ps.busyCursorWhile(loadRunnable);
            } else {
                ps.run(false, true, loadRunnable);
            }
            MetaNodeLinkUpdateResult result = loadRunnable.getLoadResult();
            container = (NodeContainer)result.getLoadedInstance();
            if (container == null) {
                throw new RuntimeException("No template returned by load routine, see log for details");
            }
            // create extra info and set it
            NodeUIInformation info = NodeUIInformation.builder().setNodeLocation(x, y, -1, -1)
                .setHasAbsoluteCoordinates(false).setSnapToGrid(snapToGrid).setIsDropLocation(true).build();
            container.setUIInformation(info);

            if (container instanceof SubNodeContainer snc) {
                SubNodeContainer projectComponent = wfm.getProjectComponent().orElse(null);
                if (projectComponent == null) {
                    return container;
                }

                // unlink component if it's added to itself
                MetaNodeTemplateInformation projectTemplateInformation = projectComponent.getTemplateInformation();
                MetaNodeTemplateInformation templateInformation = snc.getTemplateInformation();
                if (Objects.equals(templateInformation.getSourceURI(), projectTemplateInformation.getSourceURI())) {
                    MessageDialog.openWarning(SWTUtilities.getActiveShell(), "Disconnect Link",
                        "Components can only be added to themselves without linking. Will be disconnected.");
                    container.getParent().setTemplateInformation(container.getID(), MetaNodeTemplateInformation.NONE);
                }
            }
        } catch (Throwable t) {
            final var cause = ExceptionUtils.getRootCause(t);
            if ((cause instanceof CanceledExecutionException) || (cause instanceof InterruptedException)) {
                LOGGER.info("Metanode loading was canceled by the user", cause);
            } else {
                openErrorOnFailedNodeCreation(cause);
            }
        }

        return container;
    }

    private static void openErrorOnFailedNodeCreation(final Throwable cause) {
        var error = "The selected node could not be created";
        if (cause instanceof FileNotFoundException) {
            error += " because a file could not be found.";
        } else if (cause instanceof IOException) {
            error += " because of an I/O error.";
        } else if (cause instanceof InvalidSettingsException) {
            error += " because the metanode contains invalid settings.";
        } else if (cause instanceof UnsupportedWorkflowVersionException) {
            error += " because the metanode version is incompatible.";
        } else {
            error += ".";
            LOGGER.error(String.format("Metanode loading failed with %s: %s",
                cause.getClass().getSimpleName(), cause.getMessage()), cause);
        }
        var causeMessage = StringUtils.defaultIfBlank(cause.getMessage(), "");
        if (!"".equals(causeMessage) && !StringUtils.endsWith(causeMessage, ".")) {
            causeMessage += ".";
        }
        MessageDialog.openError(SWTUtilities.getActiveShell(), "Node could not be created.",
            String.format("%s %s", error, causeMessage));
    }

    /** {@inheritDoc} */
    @Override
    public boolean canUndo() {
        return m_container != null
            && getHostWFM().canRemoveNode(m_container.getID());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        LOGGER.debug("Undo: Removing node #" + m_container.getID());
        if (canUndo()) {
            getHostWFM().removeNode(m_container.getID());
        } else {
            MessageDialog.openInformation(SWTUtilities.getActiveShell(),
                    "Operation no allowed", "The node "
                    + m_container.getNameWithID()
                    + " can currently not be removed");
        }
    }

}
