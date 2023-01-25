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
 *   17 Aug 2022 (leon.wenzler): created
 */
package org.knime.workbench.editor2.actions;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.knime.core.node.workflow.MetaNodeTemplateInformation.Role;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.TemplateUpdateUtil.LinkType;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.core.util.Pair;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.commands.ChangeComponentSpaceVersionCommand;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;

/**
 * Select a space version for one linked component in a workflow. Does nothing if the selected version equals the
 * current version.
 *
 * @see #internalCalculateEnabled()
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
public class ChangeComponentSpaceVersionAction extends AbstractNodeAction {

    /** The action ID is in snake case while in the plugin.xml, the definition ID is in camel case. */
    public static final String ID = "knime.action.change_component_space_version";

    /** @param editor The host editor. */
    public ChangeComponentSpaceVersionAction(final WorkflowEditor editor) {
        super(editor);
    }

    /** {@inheritDoc} */
    @Override
    public String getId() {
        return ID;
    }

    /** {@inheritDoc} */
    @Override
    public String getText() {
        return "Change KNIME Hub Space Version...\t" + getHotkey("knime.commands.changeComponentSpaceVersion");
    }

    /** {@inheritDoc} */
    @Override
    public String getToolTipText() {
        return "";
    }

    // TODO Peter Schramm contacted to create icon
    /** {@inheritDoc} */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageRepository.getIconDescriptor(KNIMEEditorPlugin.PLUGIN_ID, "icons/meta/metanode_link_update.png");
    }

    /** {@inheritDoc} */
    @Override
    public void runOnNodes(final NodeContainerEditPart[] nodeParts) {
        throw new IllegalStateException("Not to be called");
    }

    /**
     * Enabled if exactly one Component is selected that has a source URI that points to a KNIME hub.
     * <ul>
     * <li>Metanodes cannot be stored on the hub, thus have no versioning.</li>
     * <li>A selection of Components may come from different spaces, in which case no sensible version list can be
     * displayed.</li>
     * </ul>
     * {@inheritDoc}
     */
    @Override
    protected boolean internalCalculateEnabled() {
        final var optComponent = getSingleSelectedComponent();
        if (optComponent.isEmpty()) {
            return false;
        }
        final var component = optComponent.get();

        // Component must be linked and parent must allow modification
        final var isLinked = Role.Link == component.getTemplateInformation().getRole();
        final var isChangeable = component.getParent().isWriteProtected();
        return isLinked && !isChangeable;
    }

    static boolean isHubUri(final URI uri) {
        if (uri == null) {
            return false;
        }
        // TODO getting the explorer file store can take in the order of ~100ms, so pretty expensive
        // inspecting the mount ID won't be enough, since for custom mounted hub instances the user can select an
        // arbitrary mount ID
        final var explorerFileStore = ExplorerFileSystem.INSTANCE.getStore(uri);
        if (explorerFileStore == null) {
            return false;
        }
        final var fileStoreClassName = explorerFileStore.getClass().getName();
        // NOSONAR I don't want instanceof because it would force me to introduce a dependency to commercial code
        return fileStoreClassName.equals("com.knime.explorer.server.hub.HubExplorerFileStore"); // NOSONAR
    }

    /**
     * @return the currently selected Component. Empty if more or less than one component is selected or if other node
     *         types are selected.
     */
    private Optional<SubNodeContainer> getSingleSelectedComponent() {
        final NodeContainerEditPart[] parts = getSelectedParts(NodeContainerEditPart.class);
        return parts.length == 1 ? Wrapper.unwrapOptional(parts[0].getNodeContainer(), SubNodeContainer.class)
            : Optional.empty();
    }

    /**
     * Open a dialog to select a version. If it differs from the current version, pass the selected version to a change
     * command and execute it.
     *
     * {@inheritDoc}
     */
    @Override
    public void runInSWT() {
        final var shell = SWTUtilities.getActiveShell();
        final var manager = getEditor().getWorkflowManager().orElse(null);

        // abort in remote workflow editor
        if (manager == null) {
            MessageDialog.openInformation(shell, "Select Hub Space Version",
                "Changing links is currently only possible for local workflows");
            return;
        }

        final var optComponent = getSingleSelectedComponent();
        // just in the unlikely case that the selection has changed after the last calculate enabled and now
        if (optComponent.isEmpty()) {
            return;
        }
        final var component = optComponent.get();
        // currently, the only sources that support versioning are hub instances
        if (!isHubUri(component.getTemplateInformation().getSourceURI())) {
            String message = "Changing the space version is only supported on KNIME Hub instances.\n"
                + "The source of this node is located either on a local mountpoint or on a KNIME Server.";
            MessageDialog.openWarning(shell, "Change Space Version", message);
            return;
        }

        final Pair<LinkType, Integer> previous = spaceVersion(component.getTemplateInformation().getSourceURI());
        final var prevVersion = previous.getSecond();

        // prompt for target version
        final var dialog =
            new ChangeComponentSpaceVersionDialog(shell, component, manager);
        if (dialog.open() != 0) {
            // dialog has been cancelled - no changes
            return;
        }

        // only do something if versions differ
        final var target = dialog.getSelectedVersion();
        final var targetType = target.getFirst();
        final var targetVersion = target.getSecond();
        if (targetType != previous.getFirst()
                || (targetType == LinkType.FIXED_VERSION && !Objects.equals(prevVersion, targetVersion))) {
            execute(new ChangeComponentSpaceVersionCommand(getManager(), optComponent.get(),
                targetType.getParameterString(targetVersion)));
        }
    }

    /**
     * For instance "knime://My-KNIME-Hub/*Rllck6Bn2-EaOR6d?spaceVersion=3" -> "3"
     *
     * @param knimeUri KNIME URI
     * @return the link space version
     */
    static Pair<LinkType, Integer> spaceVersion(final URI knimeUri) {
        final var queryParams = JAXRSUtils.getStructuredParams(knimeUri.getRawQuery(), "&", false, true);
        if (queryParams.containsKey("spaceVersion")) {
            // getFirst: technically, the URI could have the form someprefix?spaceVersion=1,2
            // however, this URI comes from the core so we expect it to either contain no spaceVersion or exactly one
            final var spaceVersion = queryParams.getFirst("spaceVersion");
            switch (spaceVersion) {
                case "-1":
                    return Pair.create(LinkType.LATEST_STATE, null);
                case "latest":
                    return Pair.create(LinkType.LATEST_VERSION, null);
                default:
                    return Pair.create(LinkType.FIXED_VERSION, Integer.parseInt(spaceVersion));
            }
        } else {
            return Pair.create(LinkType.LATEST_STATE, -1);
        }
    }
}
