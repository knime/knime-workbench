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
package org.knime.workbench.explorer.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileInfo;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystemUtils;
import org.knime.workbench.explorer.localworkspace.LocalWorkspaceFileStore;
import org.knime.workbench.explorer.view.dialogs.MergeRenameDialog;
import org.knime.workbench.explorer.view.dialogs.OverwriteAndMergeInfo;
import org.knime.workbench.explorer.view.dialogs.OverwriteRenameDialog;

public final class DestinationChecker <S extends AbstractExplorerFileStore,
    T extends AbstractExplorerFileStore> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            DestinationChecker.class);

    private static final OverwriteAndMergeInfo OVERWRITE_ALL_INFO =
        new OverwriteAndMergeInfo(null, false, true, false, null, false);

    private final Shell m_shell;
    private boolean m_overwriteAll;
    private boolean m_abort;

    private final Map<S, T> m_mappings = new LinkedHashMap<S, T>();

    private final Map<T, OverwriteAndMergeInfo> m_overwriteAndMergeInfos = new HashMap<T, OverwriteAndMergeInfo>();

    private final boolean m_fastDuplicate;
    private final String m_cmd;
    private final boolean m_isMultiple;
    private boolean m_isOverwriteDefault = false;
    private boolean m_isOverwriteEnabled = true;

    /**
     * Creates a new destination checker that allows to determine a
     * target file store.
     *
     * @param shell the parent shell
     * @param command the command that is executed (for messages in dialogs
     *      and logging)
     * @param isMultiple set to true if multiple operations are performed
     *      (adds additional options skip and abort to dialogs)
     * @param fastDuplicate set to true to allow a fast duplication of files
     *      in the same directory without an additional confirmation
     */
    public DestinationChecker(final Shell shell, final String command,
            final boolean isMultiple, final boolean fastDuplicate) {
        m_shell = shell;
        m_cmd = command;
        m_fastDuplicate = fastDuplicate;
        m_isMultiple = isMultiple;
    }

    /**
     * Checks if the destination can be overridden, confirms overwrites with
     * the user, checks permission, and sets merge options (for groups).
     * @param source the source file store
     * @param destination the target file store
     *
     * @return the resulting file store to write the source to
     */
    // parents and childs are always of the same type
    @SuppressWarnings("unchecked")
    public T getAndCheckDestinationFlow(final S source,
            final T destination) {
        T result = null;
        // check target
        boolean confirmOverwrite = false;
        if (destination == null) {
            return null;
        }

        AbstractExplorerFileInfo destInfo = destination.fetchInfo();
        if (destInfo.exists() && destInfo.isWorkflowGroup()) {
            String childName = source.getName();
            if ("/".equals(childName)) {
                /* If the mount point itself was selected we cannot
                 * use the root "/" as name but choose the mount id. */
                childName = source.getMountID();
            }
            // if selected destination is a dir: append workflow name

            T destStore = (T)destination.getChild(childName);
            result = destStore;
            AbstractExplorerFileInfo resultInfo = destStore.fetchInfo();
            if (resultInfo.exists() || m_mappings.containsValue(result)) {
                /* The filestore already exist or another filestore with the
                 * same name is already selected for the operation.*/
                confirmOverwrite = true;
            } else {
                // it doesn't exist - check permissions to create it
                AbstractExplorerFileInfo parentInfo =
                        result.getParent().fetchInfo();
                if (parentInfo.exists() && !parentInfo.isWriteable()) {
                    MessageDialog.openError(m_shell, m_cmd
                            + " Error", "You don't have permissions to "
                            + "store a workflow in the workflow group ("
                            + result.getParent()
                                .getMountIDWithFullPath() + ").");
                    result = null;
                }
            }
        } else if (!destInfo.exists()
                && destination.getParent().fetchInfo().isWorkflowGroup()
                && destination.getParent().fetchInfo().exists()) {
            confirmOverwrite = false;
            AbstractExplorerFileInfo parentInfo =
                    destination.getParent().fetchInfo();
            if (!parentInfo.isModifiable()) {
                MessageDialog.openError(m_shell, m_cmd + " Error",
                        "You don't have permissions to store a flow in"
                                + " the workflow group ("
                                + destination.getParent()
                                        .getMountIDWithFullPath() + ").");
                result = null;
            }
            result = destination;
        } else {
            confirmOverwrite = destInfo.exists()
                    || m_mappings.values().contains(destination);
            result = destination;
        }

        if (result != null && confirmOverwrite) {
            if (m_overwriteAll && !source.fetchInfo().isWorkflowGroup()) {
                m_overwriteAndMergeInfos.put(result, OVERWRITE_ALL_INFO);
            } else {
                AbstractExplorerFileInfo resultInfo = result.fetchInfo();
                AbstractExplorerFileInfo srcInfo = source.fetchInfo();
                Set<AbstractExplorerFileStore> forbiddenStores =
                    new HashSet<AbstractExplorerFileStore>(m_mappings.values());

                if (m_fastDuplicate && source.getParent().equals(destination)) {
                    /* Skip the rename dialog if a file is copied into its own
                     * parent. Overwriting makes no sense in this case and
                     * chances are good that the user just wanted to duplicate
                     * the file. */
                    result = (T)destination.getChild(OverwriteRenameDialog
                        .getAlternativeName(destination.getChild(source.getName()), forbiddenStores));
                } else {
                    if (srcInfo.isWorkflowGroup() && resultInfo.isWorkflowGroup()) {
                        result = openMergeDialog(source, result);
                    } else {
                        boolean isModifiable = resultInfo.isModifiable()
                            && ExplorerFileSystemUtils
                                .isLockable((List<AbstractExplorerFileStore>)Arrays.asList(result), false) == null
                            && !ExplorerFileSystemUtils.hasOpenReports(Arrays.asList(result));
                        /* Make sure that a workflow group is not overwritten by
                         * a workflow, a template or a file or vice versa */
                        final boolean overwriteOk = isSameType(srcInfo, resultInfo) && m_isOverwriteEnabled;
                        result = openOverwriteDialog(source, result, isModifiable && overwriteOk, forbiddenStores);
                    }
                }
            }
        }
        m_mappings.put(source, result);
        return result;
    }

    private static boolean isSameType(final AbstractExplorerFileInfo srcInfo,
        final AbstractExplorerFileInfo resultInfo) {
        return srcInfo.isWorkflowGroup() && resultInfo.isWorkflowGroup()
            || srcInfo.isWorkflow() && resultInfo.isWorkflow()
            || srcInfo.isFile() && resultInfo.isFile()
            || srcInfo.isMetaNode() && resultInfo.isMetaNode()
            || srcInfo.isMetaNodeTemplate() && resultInfo.isMetaNodeTemplate()
            || srcInfo.isComponentTemplate() && resultInfo.isComponentTemplate()
            || srcInfo.isSnapshot() && resultInfo.isSnapshot();
    }

    private T openMergeDialog(final S source, final T dest) {
        final boolean showKeepHistoryCheckbox = dest != null && source != null && dest.getContentProvider().isUseRest()
            && dest.getContentProvider().equals(source.getContentProvider());
        MergeRenameDialog dlg =
            new MergeRenameDialog(m_shell, dest, dest.fetchInfo().isModifiable(), showKeepHistoryCheckbox);
        if (dlg.open() != IDialogConstants.OK_ID) {
            return null;
        }
        OverwriteAndMergeInfo info = dlg.getInfo();

        if (info.merge()) {
            collectChildMappings(source, dest, info);
            return null;
        } else {
            // parents and childs are always of the same type
            @SuppressWarnings("unchecked")
            T newDest = (T)dest.getParent().getChild(info.getNewName());
            return newDest;
        }
    }

    private void collectChildMappings(final S source, final T dest,
            final OverwriteAndMergeInfo info) {
        S[] childs = null;
        try {
            childs = (S[])source.childStores(EFS.NONE, null);
        } catch (CoreException e) {
           LOGGER.error("Could not collect children for \""
                   + source.getMountIDWithFullPath() + "\".");
        }
        if (childs == null) {
            return;
        }
        for (S child : childs) {
            T destChild = (T)dest.getChild(child.getName());
            AbstractExplorerFileInfo destInfo = destChild.fetchInfo();
            boolean exists = destInfo.exists();
            if (child.fetchInfo().isWorkflowGroup() && exists
                    /* Workflow groups can only overwrite other workflow groups. */
                    && destInfo.isWorkflowGroup()) {
                collectChildMappings(child, destChild, info);
            } else { // workflows, metanode templates and files
                if (child instanceof LocalWorkspaceFileStore && ".project".equals(child.getName())) {
                        // skip .project files
                        m_mappings.put(child, null);
                } else {
                    if (exists) {
                        if (info.overwrite()) { // overwrite existing
                            m_mappings.put(child, destChild);
                            m_overwriteAndMergeInfos.put(destChild, info);
                        } else { // skip existing
                            m_mappings.put(child, null);
                        }
                    } else { // new store
                        m_mappings.put(child, destChild);
                    }
                }
            }
        }
    }



    /** Open overwrite dialog.
     * @return new destination (same as <code>dest</code> if to overwrite)
     * or <code>null</code> if canceled
     * @since 3.0
     */
    public T openOverwriteDialog(
            final T dest,
            final boolean canOverwrite,
            final Set<AbstractExplorerFileStore> forbiddenStores) {
        return openOverwriteDialog(null, dest, canOverwrite, forbiddenStores);
    }

    /**
     *
     * @param source The source.
     * @param dest The destination.
     * @param canOverwrite <code>true</code> if overwrite is possible, <code>false</code> otherwise.
     * @param forbiddenStores A set of forbidden file stores.
     * @return New destination (same as <code>dest</code> if to overwrite) or <code>null</code> if canceled
     */
    private T openOverwriteDialog(final S source, final T dest, final boolean canOverwrite,
        final Set<AbstractExplorerFileStore> forbiddenStores) {
        final boolean showSnapshotPanel =
            source == null || !dest.getContentProvider().equals(source.getContentProvider());
        final boolean showKeepHistoryCheckbox = dest != null && source != null && dest.getContentProvider().isUseRest()
            && dest.getContentProvider().equals(source.getContentProvider());

        final OverwriteRenameDialog dlg = new OverwriteRenameDialog(m_shell, dest, canOverwrite, m_isMultiple,
            forbiddenStores, showSnapshotPanel, showKeepHistoryCheckbox);
        if (canOverwrite && m_isOverwriteDefault) {
            dlg.setOverwriteAsDefault(true);
        }
        int returnCode = dlg.open();

        switch (returnCode) {
            case IDialogConstants.CANCEL_ID:
                return null;
            case IDialogConstants.ABORT_ID:
                m_abort = true;
                return null;
            case IDialogConstants.YES_TO_ALL_ID:
                m_overwriteAll = true;
                // continue to default case and return the overwrite name
            default:
                OverwriteAndMergeInfo info = dlg.getInfo();
                if (info.overwrite()) {
                    m_overwriteAndMergeInfos.put(dest, info);
                    return dest;
                } else {
                    // parents and childs are always of the same type
                    @SuppressWarnings("unchecked")
                    T newDest = (T)dest.getParent().getChild(info.getNewName());
                    return newDest;
                }
        }
    }

    /** Can be called right after construction to programmatically make
     * the overwrite action the default. This option is ignored
     * when {@link #setIsOverwriteDefault(boolean)} is set to false.
     * @param isOverwriteDefault the isOverwriteDefault to set
     * @since 3.0*/
    public void setIsOverwriteDefault(final boolean isOverwriteDefault) {
        m_isOverwriteDefault = isOverwriteDefault;
    }

    /** Can be called right after construction to programmatically disable
     * the overwrite option.
     * @param isOverwriteEnabled the isOverwriteEnabled to set
     * @since 3.0*/
    public void setIsOverwriteEnabled(final boolean isOverwriteEnabled) {
        m_isOverwriteEnabled = isOverwriteEnabled;
    }

    /**
     * @return true if the operation should be aborted, false otherwise
     */
    public boolean isAbort() {
        return m_abort;
    }

    /**
     * Returns the target file stores that have been explicitly marked to
     * be overwritten.
     *
     * @return the target file stores that have been marked for overwriting
     */
    public List<T> getOverwriteFS() {
        return new ArrayList<T>(m_overwriteAndMergeInfos.keySet());
    }

    /**
     * @return a mapping of source file stores to the chosen target file
     *      stores
     */
    public Map<S, T>
            getMappings() {
        return Collections.unmodifiableMap(m_mappings);
    }

    /**
     * Returns a map between item that should be overwritten and their overwrite-and-merge information.
     *
     * @return a map between destination file stores and overwrite-and-merge infos
     * @since 6.0
     */
    public Map<T, OverwriteAndMergeInfo> getOverwriteAndMergeInfos() {
        return Collections.unmodifiableMap(m_overwriteAndMergeInfos);
    }
}
