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
package org.knime.workbench.explorer.localworkspace;

import java.io.IOException;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.knime.core.node.NodeLogger;
import org.knime.core.workbench.WorkbenchActivator;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountPoint;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountPointDefinition;
import org.knime.core.workbench.mountpoint.api.WorkbenchMountPointSettingsHandler;
import org.knime.core.workbench.mountpoint.contribution.NoopMountPointSettings;
import org.knime.core.workbench.mountpoint.contribution.local.LocalWorkspaceMountPointSettingsHandler;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.AbstractContentProviderFactory;


/**
 * Creates a content provider for the KNIME Workflows in the local workspace of
 * the current workbench.
 *
 * @author ohl, University of Konstanz
 */
public class LocalWorkspaceContentProviderFactory extends AbstractContentProviderFactory<NoopMountPointSettings> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(LocalWorkspaceContentProviderFactory.class);

    /**
     * The id of this predefined and always existing content provider.
     */
    public static final String ID = LocalWorkspaceMountPointSettingsHandler.ID;

    private static final WorkbenchMountPointDefinition<NoopMountPointSettings> MOUNT_POINT_DEFINITION =
        WorkbenchActivator.getInstance().getMountPointDefinitionOrFail(LocalWorkspaceMountPointSettingsHandler.ID);

    @Override
    public WorkbenchMountPointDefinition<NoopMountPointSettings> getDefinition() {
        return MOUNT_POINT_DEFINITION;
    }

    @Override
    public AbstractContentProvider<NoopMountPointSettings> createContentProvider(final String mountID) {
        WorkbenchMountPoint<NoopMountPointSettings> mountPoint;
        try {
            mountPoint =
                MOUNT_POINT_DEFINITION.createMountPoint(mountID, WorkbenchMountPointSettingsHandler.EMPTY_STORAGE);
        } catch (IOException e) {
            LOGGER.error("Unable to create mountpoint", e);
            return null;
        }
        return createContentProvider(mountPoint);
    }

    @Override
    public AbstractContentProvider<NoopMountPointSettings>
        createContentProvider(final WorkbenchMountPoint<NoopMountPointSettings> mountPoint) {
        return new LocalWorkspaceContentProvider(this, mountPoint);
    }

    @Override
    public boolean isMountpointEditable() {
        return false;
    }

    @Override
    public boolean isMountIdStatic() {
        return true;
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
        return ImageRepository.getIconImage(SharedImages.LocalSpaceIcon);
    }

    @Override
    public boolean isAdditionalInformationNeeded() {
        return false;
    }

    @Override
    public AdditionalInformationPanel createAdditionalInformationPanel(final Composite parent,
        final Text mountPointIDInput) {
        // no additional information needed
        return null;
    }

}
