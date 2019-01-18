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

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
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

    @Override
    public boolean isMountpointEditable() {
        return false;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractContentProvider createContentProvider(final String mountID,
            final String content) {
        return new LocalWorkspaceContentProvider(this, mountID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAdditionalInformationNeeded() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AdditionalInformationPanel createAdditionalInformationPanel(final Composite parent,
        final Text mountPointIDInput) {
        // no additional information needed
        return null;
    }

}
