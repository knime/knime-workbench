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
 *   29.05.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.editparts.policy;

import static java.util.Arrays.asList;
import static org.knime.core.ui.wrapper.Wrapper.unwrapWFM;
import static org.knime.core.util.URIUtil.createEncodedURI;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IBundleGroup;
import org.eclipse.core.runtime.IBundleGroupProvider;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractEditPart;
import org.eclipse.gef.editpolicies.ContainerEditPolicy;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.KNIMEComponentInformation;
import org.knime.core.node.NodeCreationContext;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.core.util.FileUtil;
import org.knime.workbench.core.imports.EntityImport;
import org.knime.workbench.core.imports.ExtensionImport;
import org.knime.workbench.core.imports.ImportForbiddenException;
import org.knime.workbench.core.imports.NodeImport;
import org.knime.workbench.core.imports.RepoObjectImport;
import org.knime.workbench.core.imports.RepoObjectImport.RepoObjectType;
import org.knime.workbench.core.imports.URIImporterFinder;
import org.knime.workbench.core.imports.URIImporterUtil;
import org.knime.workbench.core.imports.UpdateSiteInfo;
import org.knime.workbench.editor2.CreateDropRequest;
import org.knime.workbench.editor2.CreateDropRequest.RequestType;
import org.knime.workbench.editor2.InstallMissingNodesJob;
import org.knime.workbench.editor2.ReaderNodeSettings;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.actions.CreateSpaceAction;
import org.knime.workbench.editor2.commands.CreateMetaNodeCommand;
import org.knime.workbench.editor2.commands.CreateMetaNodeTemplateCommand;
import org.knime.workbench.editor2.commands.CreateNodeCommand;
import org.knime.workbench.editor2.commands.CreateReaderNodeCommand;
import org.knime.workbench.editor2.commands.InsertMetaNodeCommand;
import org.knime.workbench.editor2.commands.InsertMetaNodeTemplateCommand;
import org.knime.workbench.editor2.commands.InsertNodeCommand;
import org.knime.workbench.editor2.commands.InsertReaderNodeCommand;
import org.knime.workbench.editor2.commands.ReplaceMetaNodeCommand;
import org.knime.workbench.editor2.commands.ReplaceMetaNodeTemplateCommand;
import org.knime.workbench.editor2.commands.ReplaceNodeCommand;
import org.knime.workbench.editor2.commands.ReplaceReaderNodeCommand;
import org.knime.workbench.editor2.editparts.ConnectionContainerEditPart;
import org.knime.workbench.editor2.editparts.NodeContainerEditPart;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileStore;
import org.knime.workbench.explorer.view.ExplorerJob;
import org.knime.workbench.explorer.view.actions.OpenKNIMEArchiveFileAction;
import org.knime.workbench.repository.RepositoryManager;
import org.knime.workbench.repository.model.NodeTemplate;
import org.knime.workbench.ui.p2.actions.AbstractP2Action;

/**
 * Container policy, handles the creation of new nodes that are inserted into the workflow. The request wraps an object
 * dictating what sort of factory should create the new container, as well as the source for that creation; the request
 * also specifies what sort of action (insert, replace, create) should be done.
 *
 * @author Florian Georg, University of Konstanz
 * @author Tim-Oliver Buchholz, KNIME AG, Zurich, Switzerland
 */
public class NewWorkflowContainerEditPolicy extends ContainerEditPolicy {

    /**
     * SUFFIX for our feature.groups
     */
    private static final String FEATURE_GROUP_SUFFIX = ".feature.group";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(NewWorkflowContainerEditPolicy.class);

    /**
     * 2018.12.31 - i can find no usage of this String in a search across BitBucket
     */
    public static final String REQ_LINK_METANODE_TEMPLATE = "link metanode template";

    /**
     * 2018.12.31 - i can find no usage of this String in a search across BitBucket
     */
    public static final String REQ_COPY_METANODE_TEMPLATE = "copy metanode template";

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked") // generics casting...
    protected Command getCreateCommand(final CreateRequest request) {
        final WorkflowRootEditPart workflowPart = (WorkflowRootEditPart)this.getHost();
        final WorkflowManagerUI managerUI = workflowPart.getWorkflowManager();
        final WorkflowManager manager = Wrapper.unwrapWFMOptional(managerUI).orElse(null);

        if (request instanceof CreateDropRequest) {
            final Object obj = request.getNewObject();
            final CreateDropRequest cdr = (CreateDropRequest)request;
            if (obj instanceof NodeFactory) {
                return handleNodeDrop(managerUI, (NodeFactory<? extends NodeModel>)obj, cdr);
            }

            if (manager == null) {
                //drops of the objects below are not yet supported
                //for WorkflowManagerUI-implementations
                return null;
            }
            if (obj instanceof AbstractExplorerFileStore) {
                final AbstractExplorerFileStore fs = (AbstractExplorerFileStore)obj;
                if (AbstractExplorerFileStore.isWorkflowTemplate(fs)) {
                    var uri =
                        fs instanceof RemoteExplorerFileStore ? ((RemoteExplorerFileStore)fs).toIdURI() : fs.toURI();
                    return handleMetaNodeTemplateDrop(manager, cdr, uri, fs instanceof RemoteExplorerFileStore);
                }
            } else if (obj instanceof WorkflowPersistor) {
                return handleMetaNodeDrop(manager, (WorkflowPersistor)obj, cdr);
            } else if (obj instanceof ReaderNodeSettings) {
                return handleFileDrop(manager, (ReaderNodeSettings)obj, cdr);
            } else if (obj instanceof URL) {
                return handleURLDrop(managerUI, manager, (URL)obj, cdr);
            } else {
                LOGGER.error("Illegal drop object: " + obj);
            }
        }
        return null;
    }

    private Command handleURLDrop(final WorkflowManagerUI managerUI, final WorkflowManager manager, final URL url,
        final CreateDropRequest cdr) {
        URI uri;
        if ((uri = createEncodedURI(url).orElse(null)) != null) {
            AtomicReference<EntityImport> entityImport = new AtomicReference<>();
            try {
                PlatformUI.getWorkbench().getProgressService().busyCursorWhile((monitor) -> {
                    monitor.setTaskName("Importing object from URI ...");
                    try {
                        entityImport.set(URIImporterFinder.getInstance().createEntityImportFor(uri).orElse(null));
                    } catch (ImportForbiddenException e) {
                        throw new InvocationTargetException(e);
                    }
                });
            } catch (InvocationTargetException e) {
                if (e.getTargetException() instanceof ImportForbiddenException) {
                    showPopup("Not allowed", e.getTargetException().getMessage(), SWT.ICON_WARNING);
                    LOGGER.debug(e.getMessage());
                    return null;
                }
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (entityImport.get() != null) {
                if (entityImport.get() instanceof RepoObjectImport) {
                    return handleObjectDropFromURI(manager, cdr, (RepoObjectImport)entityImport.get());
                } else if (entityImport.get() instanceof NodeImport) {
                    return handleNodeDropFromURI(managerUI, (NodeImport)entityImport.get(), cdr);
                } else if (entityImport.get() instanceof ExtensionImport) {
                    return handleExtensionDropFromURI((ExtensionImport)entityImport.get());
                }
            }
            showPopup("Unknown URL dropped!", "URL can't be dropped to KNIME workbench!\n\nUnknown URL:\n" + uri,
                SWT.ICON_WARNING);
            LOGGER.debug("Unknown URL dropped: " + uri);
        }
        return null;
    }

    private Command handleObjectDropFromURI(final WorkflowManager manager, final CreateDropRequest cdr,
        final RepoObjectImport uriImport) {
        if (uriImport.getType() == RepoObjectType.WorkflowTemplate) {
            return handleMetaNodeTemplateDrop(manager, cdr, uriImport.getKnimeURI(), true);
        } else if (uriImport.getType() == RepoObjectType.Workflow || uriImport.getType() == RepoObjectType.WorkflowGroup
            || uriImport.getType() == RepoObjectType.Data) {
            downloadAndOpenRepoObject(uriImport);
            return null;
        } else {
            // ignore drop for any other types
            return null;
        }
    }

    /**
     * Downloads and opens/imports workflows/workflow groups.
     *
     * @param uriImport
     *
     * TODO move to a more appropriate place
     */
    public static void downloadAndOpenRepoObject(final RepoObjectImport uriImport) {
        IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        final var job = new ExplorerJob("Downloading " + uriImport.getName()) {
            @Override
            protected IStatus run(final IProgressMonitor monitor) {
                try {
                    final var tmpDir = FileUtil.createTempDir("knime_download");
                    final var file = URIImporterUtil.fetchFile(uriImport, tmpDir);
                    // will always be non-null - imports are only allowed if D&D from a "known" hub (#handleURLDrop)
                    final String mountId = uriImport.getKnimeURI().getAuthority();
                    final var locationInfo = uriImport.locationInfo().orElse(null);
                    final var a = new OpenKNIMEArchiveFileAction(activePage, file, mountId, locationInfo);
                    Display.getDefault().asyncExec(a::run);
                } catch (IOException e) {
                    LOGGER.error("Problem downloading and importing repository object '" + uriImport.getName()
                        + "' from '" + uriImport.getDataURI() + "'", e);
                }
                return Status.OK_STATUS;
            }
        };
        job.schedule();

    }

    private Command handleMetaNodeTemplateDrop(final WorkflowManager manager, final CreateDropRequest request,
        final URI templateURI, final boolean isRemoteLocation) {
        final RequestType requestType = request.getRequestType();
        final Point location = request.getLocation();
        final boolean snapToGrid = WorkflowEditor.getActiveEditorSnapToGrid();

        if (RequestType.CREATE.equals(requestType)) {
            // create metanode from template
            return new CreateMetaNodeTemplateCommand(manager, templateURI, location, snapToGrid, isRemoteLocation);
        } else {
            final AbstractEditPart dropTarget = request.getEditPart();

            if (RequestType.INSERT.equals(requestType)) {
                // insert metanode from template into connection
                final InsertMetaNodeTemplateCommand insertCommand = new InsertMetaNodeTemplateCommand(manager,
                    templateURI, location, snapToGrid, (ConnectionContainerEditPart)dropTarget, isRemoteLocation);

                return potentiallyAugmentCommandForSpacing(insertCommand, request);
            } else if (RequestType.REPLACE.equals(requestType)) {
                // replace node with metanode from template
                return new ReplaceMetaNodeTemplateCommand(manager, templateURI, location, snapToGrid,
                    (NodeContainerEditPart)dropTarget, isRemoteLocation);
            }
            return null;
        }
    }

    private static Command handleExtensionDropFromURI(final ExtensionImport extImport) {
        handleExtensionDrop(extImport.getName(), extImport.getSymbolicName(), extImport.getUpdateSiteInfo());
        return null;
    }

    private static void handleExtensionDrop(final String featureName, final String featureSymbolicName,
        final UpdateSiteInfo siteInfo) {
        if (isFeatureInstalled(featureSymbolicName)) {
            showPopup("Extension cannot be installed!", "Extension " + featureName + " is already installed",
                SWT.ICON_INFORMATION);
            return;
        } else {
            final String[] dialogButtonLabels = {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL};
            MessageDialog dialog = new MessageDialog(SWTUtilities.getActiveShell(), "Install Extension?", null,
                "Do you want to search and install the extension '" + featureName + "'?", MessageDialog.QUESTION,
                dialogButtonLabels, 0);
            if ((dialog.open() == 0) && AbstractP2Action.checkSDKAndReadOnly()) {
                //try installing the missing extension
                startInstallationJob(featureName, featureSymbolicName, siteInfo);
            }
        }
    }

    private Command handleFileDrop(final WorkflowManager manager, final ReaderNodeSettings settings,
        final CreateDropRequest request) {
        final RequestType requestType = request.getRequestType();
        final boolean snapToGrid = WorkflowEditor.getActiveEditorSnapToGrid();
        final Point location = request.getLocation();
        final NodeCreationContext context = new NodeCreationContext(settings.getUrl());

        if (RequestType.CREATE.equals(requestType)) {
            return new CreateReaderNodeCommand(manager, settings.getFactory(), context, location, snapToGrid);
        } else {
            final AbstractEditPart dropTarget = request.getEditPart();

            if (RequestType.INSERT.equals(requestType)) {
                final InsertReaderNodeCommand insertCommand = new InsertReaderNodeCommand(manager,
                    settings.getFactory(), context, location, snapToGrid, (ConnectionContainerEditPart)dropTarget);

                return potentiallyAugmentCommandForSpacing(insertCommand, request);
            } else if (RequestType.REPLACE.equals(requestType)) {
                return new ReplaceReaderNodeCommand(manager, settings.getFactory(), context, location, snapToGrid,
                    (NodeContainerEditPart)dropTarget);
            }

            return null;
        }
    }

    private Command handleMetaNodeDrop(final WorkflowManager manager, final WorkflowPersistor content,
        final CreateDropRequest request) {
        final RequestType requestType = request.getRequestType();
        final Point location = request.getLocation();

        if (RequestType.CREATE.equals(requestType)) {
            // create metanode from node repository
            return new CreateMetaNodeCommand(manager, content, location, WorkflowEditor.getActiveEditorSnapToGrid());
        } else {
            final AbstractEditPart dropTarget = request.getEditPart();

            if (RequestType.INSERT.equals(requestType)) {
                // insert metanode from node repository into connection
                final InsertMetaNodeCommand insertCommand = new InsertMetaNodeCommand(manager, content, location,
                    WorkflowEditor.getActiveEditorSnapToGrid(), (ConnectionContainerEditPart)dropTarget);

                return potentiallyAugmentCommandForSpacing(insertCommand, request);
            } else if (RequestType.REPLACE.equals(requestType)) {
                // replace node with metanode from repository
                return new ReplaceMetaNodeCommand(manager, content, location,
                    WorkflowEditor.getActiveEditorSnapToGrid(), (NodeContainerEditPart)dropTarget);
            }
            return null;
        }
    }

    private Command handleNodeDropFromURI(final WorkflowManagerUI manager, final NodeImport nodeImport,
        final CreateDropRequest request) {
        String nodeFactory = nodeImport.getCanonicalNodeFactory();
        String nodeName = nodeImport.getNodeName();
        boolean isDynamicNode = nodeImport.isDynamicNode();
        final NodeTemplate nodeTemplate = getNodeTemplate(nodeFactory, nodeName, isDynamicNode);
        if (nodeTemplate != null) {
            try {
                return handleNodeDrop(manager, nodeTemplate.createFactoryInstance(), request);
            } catch (Exception e) {
                //shouldn't happen
                LOGGER.error("Cannot add node (node factory: " + nodeFactory + ", node name: " + nodeName + ")", e);
                return null;
            }
        } else {
            String featureName = nodeImport.getFeatureName();
            String featureSymbolicName = nodeImport.getFeatureSymbolicName();
            //try installing the missing extension
            String[] dialogButtonLabels = {IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL};
            Shell shell = SWTUtilities.getActiveShell();
            // TODO: derive feature name from feature symbolic name (TODO)
            MessageDialog dialog = new MessageDialog(shell, "The KNIME Extension for the node is not installed!", null,
                "The extension '" + featureName + "' is not installed. Do you want to search and install it?"
                    + "\n\nNote: Please drag and drop the node again once the installation process is finished.",
                MessageDialog.QUESTION, dialogButtonLabels, 0);
            if ((dialog.open() == 0) && AbstractP2Action.checkSDKAndReadOnly()) {
                startInstallationJob(featureName, featureSymbolicName, nodeImport.getUpdateSiteInfo());
                // TODO: add the node once the extension has been installed
                return null;
            } else {
                return null;
            }
        }
    }

    private Command handleNodeDrop(final WorkflowManagerUI manager, final NodeFactory<? extends NodeModel> factory,
        final CreateDropRequest request) {
        final RequestType requestType = request.getRequestType();
        final Point location = request.getLocation();
        final Point absoluteLocation = location.getCopy();
        final WorkflowRootEditPart rootEditPart = (WorkflowRootEditPart)super.getHost();
        rootEditPart.getFigure().translateToRelative(absoluteLocation);

        final boolean snapToGrid = WorkflowEditor.getActiveEditorSnapToGrid();
        if (RequestType.CREATE.equals(requestType)) {
            // create a new node
            return new CreateNodeCommand(manager, factory, absoluteLocation, snapToGrid);
        } else {
            if (!Wrapper.wraps(manager, WorkflowManager.class)) {
                // node insertion and replacement not yet supported for non-standard workflow managers
                // TODO: in order to enable the support, lines 158 and 167 of DragPositionProcessor need to be adopted,
                // too
                return null;
            }

            final AbstractEditPart dropTarget = request.getEditPart();
            if (RequestType.INSERT.equals(requestType)) {
                // insert new node into connection
                final InsertNodeCommand insertCommand = new InsertNodeCommand(unwrapWFM(manager), factory, location,
                    snapToGrid, (ConnectionContainerEditPart)dropTarget);

                return potentiallyAugmentCommandForSpacing(insertCommand, request);
            } else if (RequestType.REPLACE.equals(requestType)) {
                // replace node with a node
                return new ReplaceNodeCommand(unwrapWFM(manager), factory, location, snapToGrid,
                    (NodeContainerEditPart)dropTarget);
            }
            return null;
        }
    }

    private static NodeTemplate getNodeTemplate(final String factoryName, final String nodeName,
        final boolean isDynamicNode) {
        String templateId = factoryName;

        // If we are dealing with a dynamic node the node template
        // can only be found with the id {node_factory}#{node-name}.
        if (isDynamicNode) {
            templateId = factoryName + "#" + nodeName;
        }

        NodeTemplate template = RepositoryManager.INSTANCE.getNodeTemplate(templateId);

        // Corner case: sometimes nodes are registered at the nodesets-extension point (for dynamic nodes)
        // but don't implement the DynamicNodeFactory. In that case the node can only be found
        // via {node-factory}#{node-name}. But we don't know beforehand
        // - that's why we try again with the node name appended if template is still null.
        // (see https://knime-com.atlassian.net/browse/AP-12220)
        if (template == null && !isDynamicNode) {
            templateId = templateId + "#" + nodeName;
            template = RepositoryManager.INSTANCE.getNodeTemplate(templateId);
        }

        return template;
    }

    @Override
    public EditPart getTargetEditPart(final Request request) {
        final Object type = request.getType();

        if (REQ_CREATE.equals(type) || REQ_ADD.equals(type) || REQ_MOVE.equals(type)
            || REQ_COPY_METANODE_TEMPLATE.equals(type) || REQ_LINK_METANODE_TEMPLATE.equals(type)) {
            return getHost();
        }
        return super.getTargetEditPart(request);
    }

    private static void showPopup(final String text, final String msg, final int swtIcon) {
        MessageBox mb = new MessageBox(SWTUtilities.getActiveShell(), swtIcon);
        mb.setText(text);
        mb.setMessage(msg);
        mb.open();
    }

    /**
     * Checks whether the provided feature is already installed or not.
     *
     * @param featureSymbolicName the feature's symbolic name
     * @return {@code true} if the feature is already installed, {@code false} otherwise
     */
    static boolean isFeatureInstalled(final String featureSymbolicName) {
        // currently all our extensions have "feature.group" suffix. if we change this in the hub, we have to change
        // it here as well.
        final String featureName = featureSymbolicName.endsWith(FEATURE_GROUP_SUFFIX)
            ? featureSymbolicName.substring(0, featureSymbolicName.length() - FEATURE_GROUP_SUFFIX.length())
            : featureSymbolicName;
        for (IBundleGroupProvider provider : Platform.getBundleGroupProviders()) {
            for (IBundleGroup feature : provider.getBundleGroups()) {
                if (feature.getIdentifier().equals(featureName)) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("static-method")
    private Command potentiallyAugmentCommandForSpacing(final Command command, final CreateDropRequest request) {
        if (request.createSpace()) {
            final WorkflowEditor we =
                (WorkflowEditor)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
            final CreateSpaceAction csa = new CreateSpaceAction(we, request.getDirection(), request.getDistance());

            return command.chain(csa.createCompoundCommand(csa.selectedParts()));
        }

        return command;
    }

    private static void startInstallationJob(final String featureName, final String featureSymbolicName,
        final UpdateSiteInfo siteInfo) {
        Job j = new InstallMissingNodesJob(asList(new KNIMEComponentInformation() {

            @Override
            public Optional<String> getFeatureSymbolicName() {
                // Our internal update-sites require ".feature.group" suffix.
                return Optional.of(featureSymbolicName.endsWith(FEATURE_GROUP_SUFFIX) ? featureSymbolicName
                    : featureSymbolicName + FEATURE_GROUP_SUFFIX);
            }

            @Override
            public String getComponentName() {
                return featureName;
            }

            @Override
            public Optional<String> getBundleSymbolicName() {
                return Optional.empty();
            }
        }), siteInfo);
        j.setUser(true);
        j.schedule();
    }
}