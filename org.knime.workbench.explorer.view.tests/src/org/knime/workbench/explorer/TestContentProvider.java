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
 *   08.05.2020 (thor): created
 */
package org.knime.workbench.explorer;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.graphics.Image;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.AbstractContentProviderFactory;
import org.knime.workbench.explorer.view.ExplorerView;
import org.osgi.service.prefs.Preferences;

/**
 * Content provider for tests.
 *
 * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
 */
class TestContentProvider extends AbstractContentProvider {
    private String m_address;

    private String m_user;

    TestContentProvider(final AbstractContentProviderFactory myCreator, final String id, final String content) {
        super(myCreator, id);
        if (!content.isEmpty()) {
            final var parts = content.split(";");
            m_address = parts[0];
            m_user = parts[1];
        }
    }

    @Override
    public boolean hasChildren(final Object element) {
        return false;
    }

    @Override
    public void saveStateToPreferenceNode(final IEclipsePreferences node) {
        super.saveStateToPreferenceNode(node);
        node.put("address", m_address);
        node.put("user", m_user);
    }

    @Override
    public String loadStateFromPreferenceNode(final Preferences node) {
        m_address = node.get("address", "");
        m_user = node.get("user", "");
        return m_address + ";" + m_user;
    }

    @Override
    public String saveState() {
        return null;
    }

    @Override
    public void dispose() {
    }

    @Override
    public String toString() {
        return m_user + "@" + m_address;
    }

    @Override
    public Image getImage() {
        return null;
    }

    @Override
    public AbstractExplorerFileStore getFileStore(final String fullPath) {
        return null;
    }

    @Override
    public LocalExplorerFileStore fromLocalFile(final File file) {
        return null;
    }

    @Override
    public void addContextMenuActions(final ExplorerView view, final IMenuManager manager,
        final Set<String> visibleMountIDs,
        final Map<AbstractContentProvider, List<AbstractExplorerFileStore>> selection) {
    }

    @Override
    public boolean validateDrop(final AbstractExplorerFileStore target, final int operation,
        final TransferData transferType) {
        return false;
    }

    @Override
    public boolean performDrop(final ExplorerView view, final Object data, final AbstractExplorerFileStore target,
        final int operation) {
        return false;
    }

    @Override
    public boolean dragStart(final List<AbstractExplorerFileStore> fileStores) {
        return false;
    }

    @Override
    public boolean canHostMetaNodeTemplates() {
        return false;
    }

    @Override
    public boolean canHostDataFiles() {
        return false;
    }

    @Override
    public boolean supportsSnapshots() {
        return false;
    }

    @Override
    public AbstractExplorerFileStore[] getChildren(final Object parentElement) {
        return null;
    }

    @Override
    public AbstractExplorerFileStore[] getElements(final Object inputElement) {
        return null;
    }

    @Override
    public AbstractExplorerFileStore getParent(final Object element) {
        return null;
    }

    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public boolean copyOrMove(final ExplorerView view, final List<AbstractExplorerFileStore> fileStores,
        final AbstractExplorerFileStore targetDir, final boolean performMove) {
        return false;
    }

    @Override
    public void performDownloadAsync(final RemoteExplorerFileStore source, final LocalExplorerFileStore target,
        final boolean deleteSource, final AfterRunCallback afterRunCallback) {
    }

    @Override
    public void performUploadAsync(final LocalExplorerFileStore source, final RemoteExplorerFileStore target,
        final boolean deleteSource, final boolean excludeDataInWorkflows, final AfterRunCallback callback)
        throws CoreException {
    }
}
