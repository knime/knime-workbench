/* ------------------------------------------------------------------
  * This source code, its documentation and all appendant files
  * are protected by copyright law. All rights reserved.
  *
  * Copyright, 2008 - 2013
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
  *   Jan 19, 2012 (morent): created
  */

package org.knime.workbench.explorer.view;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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
import org.knime.workbench.explorer.view.dialogs.MergeRenameDialog;
import org.knime.workbench.explorer.view.dialogs.OverwriteRenameDialog;

/**
 * Creates a new destination checker that allows to determine a target file
 * store. It offers multiple options if the destination already exists, e.g.
 * overwriting, renaming or skipping it.
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 * @param <S> the type of the source file store
 * @param <T> the type of the target file store
 *
 */
public final class DestinationChecker <S extends AbstractExplorerFileStore,
        T extends AbstractExplorerFileStore> {

        private static final NodeLogger LOGGER = NodeLogger.getLogger(
                DestinationChecker.class);

        private final Shell m_shell;
        private boolean m_overwriteAll;
        private boolean m_abort;

        private final List<T> m_overwriteFS;

        private final Map<S, T> m_mappings;

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
            m_overwriteFS = new LinkedList<T>();
            m_mappings = new LinkedHashMap<S, T>();
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
                if (resultInfo.exists()
                        || m_mappings.values().contains(result)) {
                    /* The filestore already exist or another filestore with the
                     * same name is already selected for the operation.*/
                    confirmOverwrite = true;
                } else {
                    // it doesn't exist - check permissions to create it
                    AbstractExplorerFileInfo parentInfo =
                            result.getParent().fetchInfo();
                    if (parentInfo.exists() && !parentInfo.isModifiable()) {
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

            if (result != null && confirmOverwrite && !m_overwriteAll) {
                AbstractExplorerFileInfo resultInfo = result.fetchInfo();
                AbstractExplorerFileInfo srcInfo = source.fetchInfo();
                Set<AbstractExplorerFileStore> forbiddenStores
                    = new HashSet<AbstractExplorerFileStore>(
                            m_mappings.values());

                if (m_fastDuplicate
                        && source.getParent().equals(destination)) {
                    /* Skip the rename dialog if a file is copied into its own
                     * parent. Overwriting makes no sense in this case and
                     * chances are good that the user just wanted to duplicate
                     * the file. */
                    result = (T)destination.getChild(
                            OverwriteRenameDialog.getAlternativeName(
                                    destination.getChild(source.getName()),
                                    forbiddenStores));
                } else {
                    if (srcInfo.isWorkflowGroup()
                            && resultInfo.isWorkflowGroup()) {
                        result = openMergeDialog(source, result);
                    } else {
                        boolean isModifiable = resultInfo.isModifiable()
                                && ExplorerFileSystemUtils.isLockable(
                                        (List<AbstractExplorerFileStore>)
                                        Arrays.asList(result), false) == null;
                        /* Make sure that a workflow group is not overwritten by
                         * a workflow, a template or a file or vice versa */
                        boolean overwriteOk = !srcInfo.isWorkflowGroup()
                                && !resultInfo.isWorkflowGroup()
                                && m_isOverwriteEnabled;
                        result = openOverwriteDialog(result,
                                isModifiable && overwriteOk, forbiddenStores);
                    }
                }
            }
            m_mappings.put(source, result);
            return result;
        }

        private T openMergeDialog(final S source, final T dest) {
            MergeRenameDialog dlg =
                    new MergeRenameDialog(m_shell, dest,
                            dest.fetchInfo().isModifiable());
            if (dlg.open() != IDialogConstants.OK_ID) {
                return null;
            }
            if (dlg.merge()) {
                collectChildMappings(source, dest, dlg.overwrite());
                return null;
            }
            String newName = dlg.rename();
            if (newName == null) {
                // canceled.
                return null;
            }
            // parents and childs are always of the same type
            @SuppressWarnings("unchecked")
            T newDest = (T)dest.getParent().getChild(newName);
            return newDest;
        }

        private void collectChildMappings(final S source, final T dest,
                final boolean overwrite) {
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
                        /* Workflow groups can only overwrite other workflow
                            groups. */
                        && destInfo.isWorkflowGroup()) {
                    collectChildMappings(child, destChild, overwrite);
                } else { // workflows, meta node templates and files
                    if (exists) {
                        if (overwrite) { // overwrite existing
                            m_overwriteFS.add(destChild);
                            m_mappings.put(child, destChild);
                        } else { // skip existing
                            m_mappings.put(child, null);
                        }
                    } else { // new store
                        m_mappings.put(child, destChild);
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
            OverwriteRenameDialog dlg =
                    new OverwriteRenameDialog(m_shell, dest, canOverwrite,
                            m_isMultiple, forbiddenStores);
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
                if (dlg.overwrite()) {
                    m_overwriteFS.add(dest);
                    return dest;
                }
                String newName = dlg.rename();
                if (newName == null) {
                    // canceled.
                    return null;
                }
                // parents and childs are always of the same type
                @SuppressWarnings("unchecked")
                T newDest = (T)dest.getParent().getChild(newName);
                return newDest;
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
            return Collections.unmodifiableList(m_overwriteFS);
        }

        /**
         * @return a mapping of source file stores to the chosen target file
         *      stores
         */
        public Map<S, T>
                getMappings() {
            return Collections.unmodifiableMap(m_mappings);
        }

}
