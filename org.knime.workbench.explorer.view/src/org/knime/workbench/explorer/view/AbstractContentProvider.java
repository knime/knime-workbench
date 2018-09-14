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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.MetaNodeTemplateInformation;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeContainerParent;
import org.knime.core.node.workflow.NodeContainerState;
import org.knime.core.node.workflow.NodeMessage;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowContext;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.VMFileLocker;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystemUtils;
import org.knime.workbench.explorer.filesystem.LocalExplorerFileStore;
import org.knime.workbench.explorer.filesystem.MessageFileStore;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;
import org.knime.workbench.explorer.view.actions.ExplorerAction;
import org.knime.workbench.explorer.view.actions.validators.FileStoreNameValidator;
import org.knime.workbench.explorer.view.dialogs.OverwriteAndMergeInfo;
import org.knime.workbench.repository.util.ContextAwareNodeFactoryMapper;
import org.knime.workbench.ui.navigator.ProjectWorkflowMap;
import org.osgi.service.prefs.Preferences;

/**
 * Content and label provider for one source in the user space view. One
 * instance represents one mount point. It might be used by multiple views to
 * show the content of that one mount point.
 *
 * @author ohl, University of Konstanz
 */
public abstract class AbstractContentProvider extends LabelProvider implements
        ITreeContentProvider, Comparable<AbstractContentProvider>, IColorProvider {

    /**
     * Enumeration for the different link types for metanode templates.
     *
     * @since 5.0
     */
    public enum LinkType {
        /** Don't create a link. */
        None,
        /** Link with absolute URI, i.e. with mountpoint name. */
        Absolute,
        /** Link with mountpoint-relative URI, i.e. <tt>knime://knime.mountpoint/...</tt>. */
        MountpointRelative,
        /** Link with workflow-relative URI, i.e. <tt>knime://knime.workflow/...</tt>. */
        WorkflowRelative
    }

    /**
     * Empty result array.
     */
    protected static final AbstractExplorerFileStore[] NO_CHILD =
            new AbstractExplorerFileStore[0];

    /**
     * Files not displayed if contained in a workflow group.
     * @since 4.0
     */
    protected static final Collection<String> HIDDEN_FILENAMES = new ArrayList<String>();
    static {
        HIDDEN_FILENAMES.add("workflowset.meta");
    }

    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(AbstractContentProvider.class);

    private final AbstractContentProviderFactory m_creator;

    private final String m_id;

    /**
     * @param myCreator the factory creating this instance.
     * @param id mount id of this content provider
     *
     */
    public AbstractContentProvider(
            final AbstractContentProviderFactory myCreator, final String id) {
        if (myCreator == null) {
            throw new NullPointerException(
                    "The factory creating this object must be set");
        }
        if (id == null || id.isEmpty()) {
            throw new NullPointerException(
                    "The mount id can't be null nor empty");
        }
        m_creator = myCreator;
        m_id = id;
    }

    /**
     * @param fileName the file name to test
     * @return true if a file is hidden, false otherwise
     * @since 6.0
     */
    public static final boolean isHiddenFile(final String fileName) {
        if (fileName != null) {
            return fileName.startsWith(".") || HIDDEN_FILENAMES.contains(fileName);
        }
        return false;
    }

    /**
     * @return the factory that created this object.
     */
    public AbstractContentProviderFactory getFactory() {
        return m_creator;
    }

    /**
     * Returns the ID this content is mounted with.
     *
     * @return the mount id of this content provider.
     */
    public String getMountID() {
        return m_id;
    }

    /**
     * The refresh goes up and tells the view to refresh our content.
     */
    public final void refresh() {
        refresh(getFileStore("/"));
    }

    public final void refresh(final AbstractExplorerFileStore changedChild) {
        fireLabelProviderChanged(new LabelProviderChangedEvent(this,
                changedChild));
    }

    /**
     * Save state and parameters.
     *
     * @return a string representation of this factory
     *
     * @see AbstractContentProviderFactory
     */
    public abstract String saveState();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void dispose();

    /**
     * @return displayed name of this instance. {@inheritDoc}
     */
    @Override
    public abstract String toString();

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final AbstractContentProvider provider) {
        return m_id.compareTo(provider.getMountID());
    }

    /**
     * @return icon of this instance. Or null, if you don't have any.
     */
    public abstract Image getImage();

    /**
     * @param fullPath the path to the item.
     * @return the file store for the specified path.
     */
    public abstract AbstractExplorerFileStore getFileStore(final String fullPath);

    /**
     * @param uri the uri of the item
     * @return the file store for the specified uri
     */
    public AbstractExplorerFileStore getFileStore(final URI uri) {
        String mountID = ExplorerFileSystem.getIDfromURI(uri);
        if (m_id.equals(mountID)) {
            return getFileStore(uri.getPath());
        } else {
            return ExplorerFileSystem.INSTANCE.getStore(uri);
        }
    }

    /**
     * Implementation of {@link ExplorerFileSystem#fromLocalFile(File)}. If the
     * file does not exist in this space or this is not a file based mount, null
     * is returned.
     *
     * @param file The file in question.
     * @return the file store or null.
     */
    public abstract LocalExplorerFileStore fromLocalFile(final File file);

    /**
     * Helper class to find the path segment for a given local (absolute) file.
     * It will traverse the file's parents until it finds the root file (which
     * is the root of the caller). If that matches, it will assemble the
     * relative path ("/" separated).
     *
     * @param file The file to query, never null.
     * @param root The root file of the argument content provider, never null.
     * @return The path segments in a string or null if the argument file does
     *         not have the root argument as parent.
     */
    public static String getRelativePath(final File file, final File root) {
        LinkedList<String> segments = new LinkedList<String>();
        File parent = file;
        while (parent != null && !parent.equals(root)) {
            segments.addFirst(parent.getName());
            parent = parent.getParentFile();
        }
        if (parent == null || !parent.equals(root)) {
            return null;
        }
        if (segments.size() == 0) {
            return ("/");
        }
        StringBuilder path = new StringBuilder();
        for (String s : segments) {
            if (!s.isEmpty()) {
                path.append("/").append(s);
            }
        }
        return path.toString();
    }

    /* ---------------- view context menu methods ------------------- */
    /**
     * Add items to the context menu.
     *
     * @param view the explorer viewer
     * @param manager the context menu manager
     * @param visibleMountIDs the ids of the mount points currently viewed
     * @param selection the current selection sorted by content provider (with
     *            all selected item for all providers!)
     */
    public abstract void addContextMenuActions(
            final ExplorerView view,
            final IMenuManager manager,
            final Set<String> visibleMountIDs,
            final Map<AbstractContentProvider, List<AbstractExplorerFileStore>> selection);

    /* ---------------- drag and drop methods ----------------------- */

    /**
     * @param target the target the data is dropped on
     * @param operation the operation to be performed
     * @param transferType the transfer type
     * @return true if the drop is valid, false otherwise
     */
    public abstract boolean validateDrop(
            final AbstractExplorerFileStore target, final int operation,
            TransferData transferType);

    /**
     * Performs any work associated with the drop. Drop data might be null. In
     * this case the implementing classes should try to retrieve the data from
     * the {@link LocalSelectionTransfer}. Implementors must finish the drop!
     * I.e. if the operation is a move, the source should be deleted!
     *
     * @param view the view that displays the content
     * @param data the drop data, might be null
     * @param operation the operation to be performed as received from
     *            {@link ViewerDropAdapter#getCurrentOperation()}
     * @param target the drop target
     * @return true if the drop was successful, false otherwise
     * @see ViewerDropAdapter#getCurrentOperation()
     */
    public abstract boolean performDrop(final ExplorerView view,
            Object data, final AbstractExplorerFileStore target, int operation);

    /**
     * @param fileStores the dragged file stores of the content provider
     * @return true if dragging is allowed for the selection, false otherwise
     */
    public abstract boolean dragStart(List<AbstractExplorerFileStore> fileStores);


    /**
     * Saves the given metanode as template into the given file store. The metanode is marked as linked metanode
     * in its parent workflow manager. The user is queried if s/he wants to link back to the tamples and what kind
     * of link it should be.
     *
     * @param metaNode the metanode
     * @param target the target for the template
     * @return <code>true</code> if the operation was successful, <code>false</code> otherwise
     */
    @SuppressWarnings("unchecked")
    public boolean saveMetaNodeTemplate(final WorkflowManager metaNode,
            final AbstractExplorerFileStore target) {

        if (!AbstractExplorerFileStore.isWorkflowGroup(target)) {
            return false;
        }

        final String originalName = metaNode.getName();

        String mountIDWithFullPath = target.getMountIDWithFullPath();
        Shell shell = Display.getDefault().getActiveShell();
        String uniqueName = originalName;
        if (new FileStoreNameValidator().isValid(uniqueName) != null) {
            InputDialog dialog = new InputDialog(shell, "Metanode rename",
                    "The name \"" + uniqueName + "\" is not a valid "
                    + "template name.\n\nChoose a new name under which the "
                    + "template will be saved.", uniqueName,
                    new FileStoreNameValidator());
            dialog.setBlockOnOpen(true);
            if (dialog.open() == Window.CANCEL) {
                return false;
            }
            uniqueName = dialog.getValue();
        }
        AbstractExplorerFileStore templateLoc = target.getChild(uniqueName);
        boolean doesTargetExist = templateLoc.fetchInfo().exists();
        // don't allow to overwrite existing workflow groups with a template
        final boolean overwriteOK = doesTargetExist
            && !AbstractExplorerFileStore.isWorkflowGroup(templateLoc);
        boolean isOverwrite = false;

        OverwriteAndMergeInfo info = null;
        if (doesTargetExist) {
            DestinationChecker<AbstractExplorerFileStore,
                AbstractExplorerFileStore> dc = new DestinationChecker
                    <AbstractExplorerFileStore, AbstractExplorerFileStore>(
                            shell, "create template", false, false);
            dc.setIsOverwriteEnabled(overwriteOK);
            dc.setIsOverwriteDefault(overwriteOK);

            AbstractExplorerFileStore old = templateLoc;
            templateLoc = dc.openOverwriteDialog(
                    templateLoc, overwriteOK, Collections.EMPTY_SET);
            if (templateLoc == null) { // canceled
                return false;
            }
            isOverwrite = old.equals(templateLoc);
            info = dc.getOverwriteAndMergeInfos().get(templateLoc);
        }

        String newName = templateLoc.getName();

        WorkflowContext wfc = metaNode.getProjectWFM().getContext();
        AbstractContentProvider workflowMountPoint = null;
        LocalExplorerFileStore fs = ExplorerFileSystem.INSTANCE.fromLocalFile(wfc.getMountpointRoot());
        if (fs != null) {
            workflowMountPoint = fs.getContentProvider();
        }
        Collection<LinkType> allowedLinkTypes = new ArrayList<LinkType>();
        allowedLinkTypes.add(LinkType.None);
        allowedLinkTypes.add(LinkType.Absolute);
        if (target.getContentProvider().equals(workflowMountPoint)) {
            allowedLinkTypes.add(LinkType.WorkflowRelative);
            allowedLinkTypes.add(LinkType.MountpointRelative);
        }

        LinkType linkType = promptLinkMetaNodeTemplate(originalName, newName, allowedLinkTypes);
        if (linkType == null) {
            // user canceled
            return false;
        }

        File directory = metaTemplateDropGetTempDir(templateLoc);

        try {
            if (directory == null) {
                LOGGER.error("Unable to convert \"" + templateLoc
                        + "\" to local path " + "(mount provider \""
                        + toString() + "\"");
                return false;
            }
            if (directory.exists()) {
                if (!directory.isDirectory()) {
                    LOGGER.error("Implementation error: Provided storage path"
                            + " doesn't denote a directory!");
                    return false;
                }
                if (!directory.canWrite()) {
                    MessageDialog.openWarning(shell, "No write permission",
                            "You don't have sufficient privileges to write "
                                    + "to the target directory \""
                                    + mountIDWithFullPath + "\"");
                    return false;
                }
            }

            if (!metaTemplateDropPrepareForSave(templateLoc, directory,
                    isOverwrite)) {
                LOGGER.debug("Preparation for MetaTemplate save failed.");
                return false;
            }
            try {
                MetaNodeTemplateInformation template =
                        metaNode.saveAsTemplate(directory,
                                new ExecutionMonitor());
                if (!linkType.equals(LinkType.None)) {
                    // TODO this needs to be done via the command stack,
                    // the rename can currently not be undone.
                    if (!originalName.equals(newName)) {
                        metaNode.setName(newName);
                    }

                    URI uri = createMetanodeLinkUri(metaNode, templateLoc, linkType);
                    MetaNodeTemplateInformation link = template.createLink(uri);
                    metaNode.getParent().setTemplateInformation(
                            metaNode.getID(), link);
                }
                if ((info != null) && (templateLoc instanceof RemoteExplorerFileStore) && info.createSnapshot()) {
                    ((RemoteExplorerFileStore)templateLoc).createSnapshot(info.getComment());
                }
            } catch (Exception e) {
                String error = "Unable to save template: " + e.getMessage();
                LOGGER.warn(error, e);
                MessageDialog.openError(shell, "Error while writing template",
                        error);
            }

            metaTemplateDropFinish(templateLoc, directory);

        } finally {
            metaTemplateDropCleanup(directory);
        }
        target.refresh();
        return true;
    }


    /**
     * Creates a URI from the metaNode to the template location. Honors the link type. If the template location is
     * in a different mount point, only absolute paths are allowed (LinkType.Absolute).
     * @param metaNode the metanode for whome the link is created
     * @param templateLocation the template to link the metanode to
     * @param linkType the type of link
     * @return the URI pointing to the template - honoring the passed type
     * @throws CoreException
     * @throws URISyntaxException
     * @throws UnsupportedEncodingException
     * @since 5.0
     */
    public static URI createMetanodeLinkUri(final WorkflowManager metaNode,
        final AbstractExplorerFileStore templateLocation, final LinkType linkType) throws CoreException,
        URISyntaxException, UnsupportedEncodingException {
        return createMetanodeLinkUri((NodeContainerParent)metaNode, templateLocation, linkType);
    }

    /**
     * Creates a URI from the metaNode to the template location. Honors the link type. If the template location is
     * in a different mount point, only absolute paths are allowed (LinkType.Absolute).
     * @param node the node for whome the link is created
     * @param templateLocation the template to link the metanode to
     * @param linkType the type of link
     * @return the URI pointing to the template - honoring the passed type
     * @throws CoreException
     * @throws URISyntaxException
     * @throws UnsupportedEncodingException
     * @since 6.4
     */
    public static URI createMetanodeLinkUri(final NodeContainerParent node,
        final AbstractExplorerFileStore templateLocation, final LinkType linkType) throws CoreException,
        URISyntaxException, UnsupportedEncodingException {
        URI originalUri = templateLocation.toURI();
        if (linkType.equals(LinkType.Absolute)) {
            return originalUri;
        } else {
            File templateMountpointRoot = templateLocation.getContentProvider().getFileStore("/").toLocalFile();
            if (templateMountpointRoot == null) {
                LOGGER.warn("Cannot determine mountpoint for template, using absolute link instead of relative link");
                return originalUri;
            }

            WorkflowContext wfc = node.getProjectWFM().getContext();
            File workflowMountpointRoot = wfc.getMountpointRoot();
            if (workflowMountpointRoot == null) {
                LOGGER.warn("Cannot determine mountpoint for workflow, using absolute link instead of relative link");
                return originalUri;
            }

            if (!templateMountpointRoot.equals(workflowMountpointRoot)) {
                LOGGER.warn("Template and workflow are not in same mountpoint, using absolute link instead");
                return originalUri;
            }

            if (linkType.equals(LinkType.MountpointRelative)) {
                return new URI(originalUri.getScheme(), originalUri.getUserInfo(), "knime.mountpoint", -1,
                    originalUri.getPath(), originalUri.getQuery(), originalUri.getFragment());
            } else if (linkType.equals(LinkType.WorkflowRelative)) {
                String[] templatePathParts = templateLocation.toLocalFile().getAbsolutePath().split("[/\\\\]");
                String[] workflowPathParts = wfc.getCurrentLocation().getAbsolutePath().split("[/\\\\]");

                int indexWhereDifferent = 0;
                while ((indexWhereDifferent < templatePathParts.length)
                    && (indexWhereDifferent < workflowPathParts.length)
                    && templatePathParts[indexWhereDifferent].equals(workflowPathParts[indexWhereDifferent])) {
                    indexWhereDifferent++;
                }

                StringBuilder relPath = new StringBuilder();
                for (int i = indexWhereDifferent; i < workflowPathParts.length; i++) {
                    relPath.append("/..");
                }
                for (int i = indexWhereDifferent; i < templatePathParts.length; i++) {
                    relPath.append('/').append(templatePathParts[i]);
                }
                return new URI(ExplorerFileSystem.SCHEME, "knime.workflow", relPath.toString(), null);
            } else {
                throw new IllegalArgumentException("Unknown metanode link type: " + linkType);
            }
        }
    }

    /**
     * Remote targets create and return a tmp dir, local targets can just return
     * their local file.
     *
     * @param target
     * @return never null
     */
    protected File metaTemplateDropGetTempDir(
            final AbstractExplorerFileStore target) {
        File result;
        try {
            result = target.toLocalFile();
        } catch (CoreException e) {
            throw new IllegalStateException("IMPLEMENTATION ERROR: Remote file"
                    + "content provider must overwrite this method");
        }
        if (result == null) {
            throw new IllegalStateException("IMPLEMENTATION ERROR: Remote file"
                    + "content provider must overwrite this method");
        }
        return result;
    }

    /**
     * After a successful or unsuccessful finish of the template save/drop this
     * is called to get the (possible) tmp file cleaned.
     *
     * @param tmpDir the temp location provided by
     *            {@link #metaTemplateDropGetTempDir(AbstractExplorerFileStore)}
     */
    protected void metaTemplateDropCleanup(final File tmpDir) {
        // default implementation for local file stores doesn't create tmp dirs
    }

    /**
     * Called right before the meta template is saved to the tmpDir. Locals may
     * delete existing/overwritten templates. Target could be locked for
     * writing.
     *
     * @param target the final target
     * @param tmpDir the dir provided by
     *            {@link #metaTemplateDropGetTempDir(AbstractExplorerFileStore)}
     * @param overwrite if true the target/tmpDir should be cleaned for the
     *            following template save
     * @return true if drop can proceed. If false is return the drop method
     *         silently return.
     */
    protected boolean metaTemplateDropPrepareForSave(
            final AbstractExplorerFileStore target, final File tmpDir,
            final boolean overwrite) {
        /*
         * default implementation assumes a local target file store and tries to
         * lock it for writing (and deletes it to provide a clean target)
         */
        if (target.fetchInfo().exists() && overwrite) {
            while (!VMFileLocker.lockForVM(tmpDir)) {
                MessageDialog dialog =
                        new MessageDialog(
                                Display.getDefault().getActiveShell(),
                                "Unable to lock directory", null,
                                "The target folder \""
                                        + target.getMountIDWithFullPath()
                                        + "\" can currently not be locked. ",
                                MessageDialog.QUESTION, new String[]{
                                        "&Try again", "&Cancel"}, 0);
                if (dialog.open() == 0) {
                    continue; // next try
                } else {
                    return false; // abort
                }
            }
            assert VMFileLocker.isLockedForVM(tmpDir);
            ExplorerFileSystemUtils.deleteLockedWorkflows(Collections.singletonList(target), Collections.EMPTY_MAP);
            // the deletion method unlocks
        }
        return true;
    }

    /**
     * Called after the meta template is stored in the temp dir. Implementations
     * can now synch the tempDir with the actual target file store.
     *
     * @param target
     * @param tmpDir
     */
    protected void metaTemplateDropFinish(
            final AbstractExplorerFileStore target, final File tmpDir) {
        // default local implementation doesn't need to do nothing
    }

    private LinkType promptLinkMetaNodeTemplate(final String oldName, final String newName,
        final Collection<LinkType> allowedLinkTypes) {

        Shell activeShell = Display.getDefault().getActiveShell();
        String msg = "Update metanode to link to the template?";
        if (!oldName.equals(newName)) {
            msg = msg + "\n(The node will be renamed to \"" + newName + "\".)";
        }

        LinkPrompt dlg = new LinkPrompt(activeShell, msg, allowedLinkTypes);
        if (dlg.open() == Window.CANCEL) {
            return null;
        }
        return dlg.getLinkType();
    }

    private static final class LinkPrompt extends MessageDialog {
        private Button m_absoluteLink;

        private Button m_mountpointRelativeLink;

        private Button m_workflowRelativeLink;

        private Button m_noLink;

        private final Collection<LinkType> m_allowedLinkTypes;

        private LinkType m_linkType = LinkType.Absolute;


        /**
         * Create a new dialog that prompts the user for the link type.
         *
         * @param parentShell the dialog's parent shell
         * @param message the message that is shown in the dialog
         * @param allowedLinkTypes a collection of allowed linked types
         */
        LinkPrompt(final Shell parentShell, final String message, final Collection<LinkType> allowedLinkTypes) {
            super(parentShell, "Link Metanode Template", null, message, MessageDialog.QUESTION_WITH_CANCEL,
                new String[]{IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL}, 0);
            setShellStyle(getShellStyle() | SWT.SHEET);
            m_allowedLinkTypes = allowedLinkTypes;
        }

        /**
         * After the dialog closes get the selected link type.
         *
         * @return null, if no link should be created, otherwise the selected link type.
         */
        public LinkType getLinkType() {
            return m_linkType;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Control createCustomArea(final Composite parent) {
            Composite group = new Composite(parent, SWT.NONE);
            group.setLayout(new GridLayout());

            GridData data = new GridData();
            data.horizontalIndent = 5;

            Label l1 = new Label(group, SWT.NONE);
            l1.setText("Select the type of link to be created:");

            m_absoluteLink = new Button(group, SWT.RADIO);
            m_absoluteLink.setText("Create absolute link");
            m_absoluteLink.setLayoutData(data);
            m_absoluteLink.setToolTipText("If you move the workflow to a new location it will "
                + "always link back to this template");
            m_absoluteLink.setSelection(true);
            m_absoluteLink.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    m_linkType = LinkType.Absolute;
                }
            });
            m_absoluteLink.setEnabled(m_allowedLinkTypes.contains(LinkType.Absolute));

            m_mountpointRelativeLink = new Button(group, SWT.RADIO);
            m_mountpointRelativeLink.setLayoutData(data);
            m_mountpointRelativeLink.setText("Create mountpoint-relative link");
            m_mountpointRelativeLink.setToolTipText("If you move the workflow to a new workspace - the metanode "
                + "template must be available on this new workspace as well");
            m_mountpointRelativeLink.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    m_linkType = LinkType.MountpointRelative;
                }
            });
            m_mountpointRelativeLink.setEnabled(m_allowedLinkTypes.contains(LinkType.MountpointRelative));

            m_workflowRelativeLink = new Button(group, SWT.RADIO);
            m_workflowRelativeLink.setLayoutData(data);
            m_workflowRelativeLink.setText("Create workflow-relative link");
            m_workflowRelativeLink.setToolTipText("Workflow and metanode should always be moved together");
            m_workflowRelativeLink.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    m_linkType = LinkType.WorkflowRelative;
                }
            });
            m_workflowRelativeLink.setEnabled(m_allowedLinkTypes.contains(LinkType.WorkflowRelative));

            m_noLink = new Button(group, SWT.RADIO);
            m_noLink.setLayoutData(data);
            m_noLink.setText("Don't link metanode with saved template");
            m_noLink.setToolTipText("You will not be able to update the metanode from the template.");
            m_noLink.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(final SelectionEvent e) {
                    m_linkType = LinkType.None;
                }
            });
            m_noLink.setEnabled(m_allowedLinkTypes.contains(LinkType.None));
            return group;
        }
    }

    /**
     * Saves the given sub node as template into the given file store. The sub node is marked as linked sub node
     * in its parent workflow manager. The user is queried if s/he wants to link back to the tamples and what kind
     * of link it should be.
     *
     * @param subNode the sub node
     * @param target the target for the template
     * @return <code>true</code> if the operation was successful, <code>false</code> otherwise
     * @since 6.4
     */
    @SuppressWarnings("unchecked")
    public boolean saveSubNodeTemplate(final SubNodeContainer subNode,
            final AbstractExplorerFileStore target) {

        if (!AbstractExplorerFileStore.isWorkflowGroup(target)) {
            return false;
        }

        final String originalName = subNode.getName();

        String mountIDWithFullPath = target.getMountIDWithFullPath();
        Shell shell = Display.getDefault().getActiveShell();
        String uniqueName = originalName;
        if (new FileStoreNameValidator().isValid(uniqueName) != null) {
            InputDialog dialog = new InputDialog(shell, "Wrapped metanode rename",
                    "The name \"" + uniqueName + "\" is not a valid "
                    + "template name.\n\nChoose a new name under which the "
                    + "template will be saved.", uniqueName,
                    new FileStoreNameValidator());
            dialog.setBlockOnOpen(true);
            if (dialog.open() == Window.CANCEL) {
                return false;
            }
            uniqueName = dialog.getValue();
        }
        AbstractExplorerFileStore templateLoc = target.getChild(uniqueName);
        boolean doesTargetExist = templateLoc.fetchInfo().exists();
        // don't allow to overwrite existing workflow groups with a template
        final boolean overwriteOK = doesTargetExist
            && !AbstractExplorerFileStore.isWorkflowGroup(templateLoc);
        boolean isOverwrite = false;

        OverwriteAndMergeInfo info = null;
        if (doesTargetExist) {
            DestinationChecker<AbstractExplorerFileStore,
                AbstractExplorerFileStore> dc = new DestinationChecker
                    <AbstractExplorerFileStore, AbstractExplorerFileStore>(
                            shell, "create template", false, false);
            dc.setIsOverwriteEnabled(overwriteOK);
            dc.setIsOverwriteDefault(overwriteOK);

            AbstractExplorerFileStore old = templateLoc;
            templateLoc = dc.openOverwriteDialog(
                    templateLoc, overwriteOK, Collections.EMPTY_SET);
            if (templateLoc == null) { // canceled
                return false;
            }
            isOverwrite = old.equals(templateLoc);
            info = dc.getOverwriteAndMergeInfos().get(templateLoc);
        }

        String newName = templateLoc.getName();

        WorkflowContext wfc = subNode.getProjectWFM().getContext();
        AbstractContentProvider workflowMountPoint = null;
        LocalExplorerFileStore fs = ExplorerFileSystem.INSTANCE.fromLocalFile(wfc.getMountpointRoot());
        if (fs != null) {
            workflowMountPoint = fs.getContentProvider();
        }
        Collection<LinkType> allowedLinkTypes = new ArrayList<LinkType>();
        allowedLinkTypes.add(LinkType.None);
        allowedLinkTypes.add(LinkType.Absolute);
        if (target.getContentProvider().equals(workflowMountPoint)) {
            allowedLinkTypes.add(LinkType.WorkflowRelative);
            allowedLinkTypes.add(LinkType.MountpointRelative);
        }

        LinkType linkType = promptLinkMetaNodeTemplate(originalName, newName, allowedLinkTypes);
        if (linkType == null) {
            // user canceled
            return false;
        }

        File directory = metaTemplateDropGetTempDir(templateLoc);

        try {
            if (directory == null) {
                LOGGER.error("Unable to convert \"" + templateLoc
                        + "\" to local path " + "(mount provider \""
                        + toString() + "\"");
                return false;
            }
            if (directory.exists()) {
                if (!directory.isDirectory()) {
                    LOGGER.error("Implementation error: Provided storage path"
                            + " doesn't denote a directory!");
                    return false;
                }
                if (!directory.canWrite()) {
                    MessageDialog.openWarning(shell, "No write permission",
                            "You don't have sufficient privileges to write "
                                    + "to the target directory \""
                                    + mountIDWithFullPath + "\"");
                    return false;
                }
            }

            if (!metaTemplateDropPrepareForSave(templateLoc, directory,
                    isOverwrite)) {
                LOGGER.debug("Preparation for MetaTemplate save failed.");
                return false;
            }
            try {
                MetaNodeTemplateInformation template = subNode.saveAsTemplate(directory, new ExecutionMonitor());
                if (!linkType.equals(LinkType.None)) {
                    // TODO this needs to be done via the command stack,
                    // the rename can currently not be undone.
                    if (!originalName.equals(newName)) {
                        subNode.setName(newName);
                    }

                    URI uri = createMetanodeLinkUri(subNode, templateLoc, linkType);
                    MetaNodeTemplateInformation link = template.createLink(uri);
                    subNode.getParent().setTemplateInformation(
                            subNode.getID(), link);
                }
                if ((info != null) && (templateLoc instanceof RemoteExplorerFileStore) && info.createSnapshot()) {
                    ((RemoteExplorerFileStore)templateLoc).createSnapshot(info.getComment());
                }
            } catch (Exception e) {
                String error = "Unable to save template: " + e.getMessage();
                LOGGER.warn(error, e);
                MessageDialog.openError(shell, "Error while writing template",
                        error);
            }

            metaTemplateDropFinish(templateLoc, directory);

        } finally {
            metaTemplateDropCleanup(directory);
        }
        target.refresh();
        return true;
    }

    /**
     * @return whether this content provider is able to host metanode templates,
     *         this is true for KNIME Server or KNIME TeamSpace but false for the LOCAL
     *         space (or the the read-only EXAMPLES KNIME Server)
     */
    public abstract boolean canHostMetaNodeTemplates();

    /**
     * @return whether this content provider is able to host data files. This is true for KNIME Server or
     *         KNIME TeamSpace, but false for the LOCAL space.
     * @since 4.0
     */
    public abstract boolean canHostDataFiles();

    /**
     * Returns whether this content provider supports snapshots. Currently only a server supports snapshots and only
     * since version 3.8.
     *
     * @return <code>true</code> when snapshots are supported, <code>false</code> otherwise
     * @since 6.0
     */
    public abstract boolean supportsSnapshots();


    /* -------------- content provider methods ---------------------------- */

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract AbstractExplorerFileStore[] getChildren(Object parentElement);

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract AbstractExplorerFileStore[] getElements(Object inputElement);

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract AbstractExplorerFileStore getParent(Object element);

    /* ---------- helper methods for content provider ------------------- */
    /**
     * Helper method for content providers. Returns children of a workflow.
     *
     * @param workflow the workflow to return the children for
     * @return children of a workflow
     */
    public static AbstractExplorerFileStore[] getWorkflowChildren(
            final AbstractExplorerFileStore workflow) {
        assert AbstractExplorerFileStore.isWorkflow(workflow);

        try {
            IFileStore[] childs = workflow.childStores(EFS.NONE, null);
            if (childs == null || childs.length == 0) {
                return NO_CHILD;
            }
            /*
             * currently we are not showing nodes
             */
            return NO_CHILD;
            // ArrayList<ExplorerFileStore> result =
            // new ArrayList<ExplorerFileStore>();
            // for (IFileStore c : childs) {
            // not adding nodes for now.
            // if (ExplorerFileStore.isMetaNode((ExplorerFileStore)c)) {
            // || ExplorerFileStore.isNode(childFile)) {
            // result.add((ExplorerFileStore)c);
            // }
            // }
            // return result.toArray(new ExplorerFileStore[result.size()]);
        } catch (CoreException ce) {
            LOGGER.debug(ce);
            return NO_CHILD;
        }

    }

    /**
     *
     * @param template to return children for
     * @return children of the template.
     */
    public static AbstractExplorerFileStore[] getWorkflowTemplateChildren(
            final AbstractExplorerFileStore template) {
        // metanodes have not children (as long as we don't show their nodes)
        return NO_CHILD;
    }

    /**
     * Helper method for content providers. Returns children of a workflowgroup.
     *
     * @param workflowGroup the workflow group to return the children for
     * @return the content of the workflow group
     */
    public static AbstractExplorerFileStore[] getWorkflowgroupChildren(
            final AbstractExplorerFileStore workflowGroup) {

        assert AbstractExplorerFileStore.isWorkflowGroup(workflowGroup);

        try {
            AbstractExplorerFileStore[] childs =
                    workflowGroup.childStores(EFS.NONE, null);
            if (childs == null || childs.length == 0) {
                return NO_CHILD;
            }
            ArrayList<AbstractExplorerFileStore> result =
                    new ArrayList<AbstractExplorerFileStore>();
            for (AbstractExplorerFileStore c : childs) {
                if (AbstractExplorerFileStore.isWorkflowGroup(c)
                        || AbstractExplorerFileStore.isWorkflow(c)
                        || AbstractExplorerFileStore.isWorkflowTemplate(c)) {
                    result.add(c);
                }
                if (AbstractExplorerFileStore.isDataFile(c)) {
                    if (!isHiddenFile(c.getName())) {
                        result.add(c);
                    }
                }
            }
            return result.toArray(new AbstractExplorerFileStore[result.size()]);
        } catch (CoreException ce) {
            LOGGER.debug(ce);
            return NO_CHILD;
        }
    }

    public static AbstractExplorerFileStore[] getMetaNodeChildren(
            final AbstractExplorerFileStore metaNode) {
        assert AbstractExplorerFileStore.isMetaNode(metaNode);

        try {
            IFileStore[] childs = metaNode.childStores(EFS.NONE, null);
            if (childs == null || childs.length == 0) {
                return NO_CHILD;
            }
            /*
             * currently we are not showing nodes
             */
            return NO_CHILD;
            // ArrayList<ExplorerFileStore> result =
            // new ArrayList<ExplorerFileStore>();
            // for (IFileStore c : childs) {
            // not adding nodes for now.
            // if (ExplorerFileStore.isMetaNode((ExplorerFileStore)c)) {
            // || ExplorerFileStore.isNode(childFile)) {
            // result.add((ExplorerFileStore)c);
            // }
            // }
            // return result.toArray(new ExplorerFileStore[result.size()]);
        } catch (CoreException ce) {
            LOGGER.debug(ce);
            return NO_CHILD;
        }

    }

    /* ------------ helper methods for label provider (icons) ------------- */
    /**
     * Returns an icon/image for the passed file, if it is something like a
     * workflow, group, node or metanode. If it is not a store representing one
     * of these, null is returned.
     *
     * @param efs the explorer file store
     * @return the icon/image for the passed file store
     */
    public static Image getWorkspaceImage(final AbstractExplorerFileStore efs) {

        if (AbstractExplorerFileStore.isNode(efs)) {
            return ImageRepository.getIconImage(SharedImages.Node);
        }
        if (AbstractExplorerFileStore.isMetaNode(efs)) {
            return ImageRepository.getIconImage(SharedImages.Node);
        }
        if (AbstractExplorerFileStore.isWorkflowGroup(efs)) {
            return ImageRepository.getIconImage(SharedImages.WorkflowGroup);
        }
        if (AbstractExplorerFileStore.isWorkflowTemplate(efs)) {
            return ImageRepository.getIconImage(SharedImages.MetaNodeTemplate);
        }
        if (AbstractExplorerFileStore.isDataFile(efs)) {
            Image img = ContextAwareNodeFactoryMapper.getImage(efs.getName());
            if (img != null) {
                return img;
            }
            return ImageRepository.getIconImage(SharedImages.File);
        }
        if (!AbstractExplorerFileStore.isWorkflow(efs)) {
            return null;
        }

        // if it is a local workflow return the correct icon for open flows
        File f;
        try {
            f = efs.toLocalFile(EFS.NONE, null);
        } catch (CoreException ce) {
            return ImageRepository.getIconImage(SharedImages.WorkflowClosed);
        }

        if (f == null) {
            return ImageRepository.getIconImage(SharedImages.WorkflowClosed);
        }
        URI wfURI = f.toURI();
        NodeContainer nc = ProjectWorkflowMap.getWorkflow(wfURI);
        if (nc == null) {
            return ImageRepository.getIconImage(SharedImages.WorkflowClosed);
        }
        if (nc instanceof WorkflowManager) {
            if (nc.getID().hasSamePrefix(WorkflowManager.ROOT.getID())) {
                // only show workflow directly off the root
                if (nc.getNodeMessage().getMessageType()
                        .equals(NodeMessage.Type.ERROR)) {
                    return ImageRepository.getIconImage(SharedImages.WorkflowError);
                }
                NodeContainerState ncState = nc.getNodeContainerState();
                if (ncState.isExecuted()) {
                    return ImageRepository.getIconImage(SharedImages.WorkflowExecuted);
                } else if (ncState.isExecutionInProgress()) {
                    return ImageRepository.getIconImage(SharedImages.WorkflowExecuting);
                } else if (ncState.isConfigured()) {
                    return ImageRepository.getIconImage(SharedImages.WorkflowConfigured);
                } else {
                    return ImageRepository.getIconImage(SharedImages.WorkflowConfigured);
                }
            } else {
                return ImageRepository.getIconImage(SharedImages.Node);
            }
        } else {
            return ImageRepository.getIconImage(SharedImages.WorkflowUnknown);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Image getImage(final Object element) {
        if (element instanceof MessageFileStore) {
            return ((MessageFileStore)element).getImage();
        }
        return null;
    }

    /**
     * @return true if this content provider is accessing a remote file system
     */
    public abstract boolean isRemote();

    /**
     * Connects to the content provider. This can for example trigger a login.
     *
     * @since 7.3
     */
    public void connect() {
        // do nothing by default
    }

    /**
     * Disconnects the content provider. This can for example trigger a logout.
     *
     * @since 7.3
     */
    public void disconnect() {
        // do nothing by default
    }


    /**
     * Checks whether it is possible to add items to the content provider or
     * change its content. This might not be the case, for example, if
     * authentification is required but the user is not authenticated yet or on
     * a read-only server like the public server is accessed.
     *
     * @return true if the provider's content cannot be modified, false
     *      otherwise
     */
    public abstract boolean isWritable();

    /**
     * Copies or moves one or multiple file stores into the target directory.
     *
     * @param view the view that displays the content
     * @param fileStores the file stores to copy or move
     * @param targetDir the target directory. Make sure to call the content
     *            provider that can handle the target dir type.
     * @param performMove true, if the file stores should be moved, false
     *            otherwise
     * @return true if the operation was successful, false otherwise
     */
    public abstract boolean copyOrMove(final ExplorerView view,
            List<AbstractExplorerFileStore> fileStores,
            AbstractExplorerFileStore targetDir, boolean performMove);

    /**
     * Downloads a file store from a remote provider to a local provider.
     *
     * @param source the file store to be downloaded
     * @param target the file store to download to
     * @param deleteSource if true the source is deleted after a successful download
     * @param afterRunCallback Callback that is called after the operation is completed (or null).
     * @since 7.0
     */
    public abstract void performDownloadAsync(RemoteExplorerFileStore source, LocalExplorerFileStore target,
        boolean deleteSource, AfterRunCallback afterRunCallback);

    /**
     * Uploads a file store from a local provider to a remote provider.
     *
     * @param source the file store to be uploaded
     * @param target the file store to upload to
     * @param deleteSource if true the source is deleted after a successful upload
     * @param excludeDataInWorkflows If true, any workflow in the selected folder will be 'exported' without data
     *        (similar to the export menu option).
     * @param callback TODO
     * @throws CoreException if this method fails. Reasons include: A
     *         corresponding file could not be created in the local file system.
     * @since 7.0
     */
    public abstract void performUploadAsync(final LocalExplorerFileStore source, final RemoteExplorerFileStore target,
        boolean deleteSource, boolean excludeDataInWorkflows, AfterRunCallback callback) throws CoreException;

    /**
     * Allows the content provider to open a 'special' confirmation dialog. Server is currently the only one confirming
     * deletion of jobs and sched execs. Default implementation accepts deletion with default user confirmation dialog.
     *
     * @param parentShell the parent shell for dialogs
     * @param allFiles list of all files to be deleted.
     * @param toDelWFs workflows contained in toDel
     * @return an object if the provided opened a dialog, null if no confirm dialog was shown (standard confirm dialog
     *         will open then). If not-null is returned the standard confirmation dialog will not show.
     * @since 6.0
     */
    public DeletionConfirmationResult confirmDeletion(final Shell parentShell,
        final Collection<AbstractExplorerFileStore> allFiles, final Collection<AbstractExplorerFileStore> toDelWFs) {
        return null;
    }

    /**
     * Allows the content provider to open a dialog warning the user of overwrite in a copy/move operation.
     * Currently the server is the only one if jobs or sched execs exist. Default implementation accepts the overwrite
     * without user confirmation.
     * @param parentShell the parent shell for dialogs
     * @param flowsToOverWrite list of flows of this content provider being overwritten.
     * @return true if overwrite is confirmed, false, if user canceled, null if no confirm dialog was shown.
     * @since 5.0
     */
    public AtomicBoolean confirmOverwrite(final Shell parentShell,
        final Collection<AbstractExplorerFileStore> flowsToOverWrite) {
        return null;
    }

    /**
     * Allows the content provider to open a dialog warning the user of the move (and deletion of flows in the source
     * location). Currently the server is the only one if jobs or sched execs exist. Default implementation accepts
     * the move without user confirmation.
     * @param parentShell the parent shell for dialogs
     * @param flowsToMove list of flows of this content provider being moved.
     * @return true if move is confirmed, false, if user canceled, null if no confirm dialog was shown.
     * @since 5.0
     */
    public AtomicBoolean confirmMove(final Shell parentShell,
        final Collection<AbstractExplorerFileStore> flowsToMove) {
        return null;
    }

    /**
     * @param fileStores the file stores to be copied / moved
     * @param performMove true if moving, false for copying
     * @return an error message describing the problem or null, if the no open
     *      editor blocks the operation
     * @since 3.0
     */
    protected String checkOpenEditors(
            final List<AbstractExplorerFileStore> fileStores,
            final boolean performMove) {
        // even saved editors are note allowed when moving
        String msg = ExplorerFileSystemUtils.isLockable(fileStores,
                !performMove);
        if (msg != null) {
            MessageBox mb =
                    new MessageBox(Display.getCurrent().getActiveShell(),
                            SWT.ICON_ERROR | SWT.OK);
            mb.setText("Dragging canceled");
            mb.setMessage(msg);
            mb.open();
        }
        return msg;
    }

    /**
     * {@inheritDoc}
     * @since 7.2
     */
    @Override
    public Color getForeground(final Object element) {
        return Display.getDefault().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
    }

    /**
     * {@inheritDoc}
     * @since 7.2
     */
    @Override
    public Color getBackground(final Object element) {
        return Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
    }

    /**
     * Callback for clients to be notified when async operation is completed. Used for future selections in the
     * explorer. Error messages are collected and displayed by the Eclipse framework - no need to collect and display
     * them in here.
     *
     * @since 7.0
     */
    public interface AfterRunCallback {

        /**
         * Called after completion of the async execution of up/download. This method is call in the UI thread.
         *
         * @param throwable A throwable or <code>null</code> (non-null if and only if errorMessage is non-null)
         */
        void afterCompletion(final Throwable throwable);

        /**
         * @param callback callback class or null
         * @param e ...
         */
        public static void callCallbackInDisplayThread(final AfterRunCallback callback, final Exception e) {
            if (callback != null) {
                Display current = Display.getDefault();
                if (current != null && !current.isDisposed()) {
                    current.syncExec(new Runnable() {
                        @Override
                        public void run() {
                            callback.afterCompletion(e);
                        }
                    });
                }
            }
        }

    }

    /**
     * Saves the given state of the ContentProvider to the {@link IEclipsePreferences} node.
     * @param node The {@link IEclipsePreferences} node to save to
     * @since 8.2
     */
    public void saveStateToPreferenceNode(final IEclipsePreferences node) {
        // AP-8989 Switching to IEclipsePreferences
        // By default don't save anything to the preference node.
    }

    /**
     * Load the state from the given {@link Preferences} node.
     *
     * @param node The {@link Preferences} node to load from
     * @return The state loaded from the {@link Preferences} node
     * @since 8.2
     */
    public String loadStateFromPreferenceNode(final Preferences node) {
        // AP-8989 Switching to IEclipsePreferences
        // By default return an empty state.
        return "";
    }

    /**
     * Can optionally be overridden by subclasses in order to react on double-click events on an
     * {@link AbstractExplorerFileStore}-object in the {@link ExplorerView}.
     *
     * @param fileStore the {@link AbstractExplorerFileStore} that is selected
     * @param view the {@link ExplorerView}-instance (e.g. in order to create an {@link ExplorerAction})
     *
     * @since 8.3
     */
    protected void onDoubleClick(final AbstractExplorerFileStore fileStore, final ExplorerView view) {
        //do nothing by default
    }
}
