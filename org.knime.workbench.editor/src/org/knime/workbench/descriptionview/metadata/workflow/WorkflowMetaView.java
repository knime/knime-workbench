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
 *   Apr 28, 2019 (loki): created
 */
package org.knime.workbench.descriptionview.metadata.workflow;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.workflow.NodeContainerMetadata.ContentType;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowMetadata;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.node.workflow.metadata.MetaInfoFile;
import org.knime.core.node.workflow.metadata.MetadataVersion;
import org.knime.core.node.workflow.metadata.MetadataXML;
import org.knime.core.node.workflow.metadata.WorkflowSetMetaParser;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.workbench.descriptionview.metadata.AbstractMetaView;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileInfo;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileInfo;
import org.knime.workbench.explorer.localworkspace.LocalWorkspaceFileInfo;
import org.knime.workbench.explorer.view.ContentObject;
import org.knime.workbench.ui.workflow.metadata.MetadataItemType;

/**
 * This is the view reponsible for displaying, and potentially allowing the editing of, the meta-information associated
 * with a workflow; for example:
 *      . description
 *      . tags
 *      . links
 *      . license
 *      . author
 *
 * The genesis for this view is https://knime-com.atlassian.net/browse/AP-11628
 *
 * As part of https://knime-com.atlassian.net/browse/AP-12082 is was decided that the license field would only be shown
 * in cases where the metadata was coming from a KNIME Hub server; i've gated this condition with a static boolean below
 * (search 'AP-12082') so that future generations can turn the license stuff back on when we support it more widely.
 *
 * @author loki der quaeler
 */
public class WorkflowMetaView extends AbstractMetaView {

    private static final DateTimeFormatter AUTHORED_WHEN_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowMetaView.class);

    private WeakReference<WorkflowManager> m_editWorkflowManager;

    private boolean m_reportedDirty;


    /**
     * @param parent
     */
    public WorkflowMetaView(final Composite parent) {
        super(parent, EnumSet.of(HiddenSection.UPPER, HiddenSection.LOWER));
    }

    /**
     * @param manager workflow manager to look for, not {@code null}
     * @return {@code true} if this editor is currently editing the given workflow manager's metadata,
     * {@code false} otherwise
     */
    public boolean isDirtyEditing(final WorkflowManager manager) {
        return Optional.ofNullable(m_editWorkflowManager).map(WeakReference::get).filter(manager::equals).isPresent();
    }

    /**
     * Should the description view receive a remotely fetched metadata for a server-side object, it will facilitate its
     * display by invoking this method. N.B. {@link #selectionChanged(IStructuredSelection)} has already been called for
     * this item prior to receiving this invocation, and so <code>m_currentWorkflowName</code> has been correctly
     * populated.
     *
     * If the author, description, and creation date are all null, it will be interpreted as a failure to fetch remote
     * metadata.
     *
     * @param author the author, or null
     * @param legacyDescription the legacy-style description, or null
     * @param creationDate the creation date, or null
     * @param shouldShowCCBY40License if true, the CC-BY-4.0 license will be shown in the UI
     */
    public void handleAsynchronousRemoteMetadataPopulation(final String author, final String legacyDescription,
        final ZonedDateTime creationDate, final boolean shouldShowCCBY40License) {
        m_modelFacilitator = new WorkflowMetadataModelFacilitator(author, legacyDescription, creationDate);
        m_modelFacilitator.parsingHasFinishedWithDefaultTitleName(m_currentAssetName,
            () -> creationDate == null ? ZonedDateTime.now() : creationDate, null);
        m_modelFacilitator.setModelObserver(this);

        if (m_metadataCanBeEdited.get()) {
            m_metadataCanBeEdited.set(false);
            configureFloatingHeaderBarButtons();
        }

        m_waitingForAsynchronousMetadata.set(false);
        m_asynchronousMetadataFetchFailed
            .set((author == null) && (legacyDescription == null) && (creationDate == null));
        m_assetRepresentsATemplate.set(false);
        m_assetRepresentsAJob.set(false);
        m_shouldDisplayLicenseSection.set(shouldShowCCBY40License);
        getDisplay().asyncExec(this::updateDisplay);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selectionChanged(final IStructuredSelection selection) { // NOSONAR
        final Object selected = selection.getFirstElement();
        final boolean canEditMetadata;

        m_waitingForAsynchronousMetadata.set(false);
        m_asynchronousMetadataFetchFailed.set(false);
        m_assetRepresentsATemplate.set(false);
        m_assetRepresentsAJob.set(false);
        m_shouldDisplayLicenseSection.set(!SHOW_LICENSE_ONLY_FOR_HUB);
        m_assetIsReadable.set(true);
        m_editWorkflowManager = null;

        final Supplier<ZonedDateTime> creationDateSupplier;
        File legacyMetadataFile = null;
        File metadataFile = null;
        WorkflowManager projectWM = null;
        if (selected instanceof ContentObject contentObject) {
            final AbstractExplorerFileStore fs = contentObject.getFileStore();
            final AbstractExplorerFileInfo fileInfo = fs.fetchInfo();
            if (!(fileInfo instanceof RemoteExplorerFileInfo) && !(fileInfo instanceof LocalWorkspaceFileInfo)) {
                LOGGER.debug("Received unexpected file info type: " + fileInfo.getClass());
                return;
            }

            final boolean isWorkflow = AbstractExplorerFileStore.isWorkflow(fs);
            final boolean isTemplate = AbstractExplorerFileStore.isWorkflowTemplate(fs);
            final boolean isRemote = fs.getContentProvider().isRemote();
            final boolean isJob = fileInfo instanceof RemoteExplorerFileInfo remoteInfo && remoteInfo.isWorkflowJob();
            final boolean isReadable = fs.fetchInfo().isReadable();
            final boolean validFS = (isWorkflow || isTemplate || isJob);
            if (!validFS) {
                return;
            }

            m_currentAssetName = fs.getName();
            if (isTemplate) {
                m_waitingForAsynchronousMetadata.set(false);
                m_assetRepresentsATemplate.set(true);
                m_assetRepresentsAJob.set(isJob);
                m_assetIsReadable.set(isReadable);

                canEditMetadata = false;
            } else if (isRemote) {
                m_waitingForAsynchronousMetadata.set(isRemote && !isTemplate && !isJob);
                m_assetRepresentsATemplate.set(false);
                m_assetRepresentsAJob.set(isJob);
                m_assetIsReadable.set(isReadable);

                canEditMetadata = false;
            } else {
                final AbstractExplorerFileStore metaInfo = fs.getChild(WorkflowPersistor.METAINFO_FILE);
                final AbstractExplorerFileStore metadata = fs.getChild(WorkflowPersistor.WORKFLOW_METADATA_FILE_NAME);
                try {
                    legacyMetadataFile = metaInfo.toLocalFile(EFS.NONE, null);
                    metadataFile = metadata.toLocalFile(EFS.NONE, null);
                } catch (final CoreException ce) {
                    LOGGER.error("Unable to convert EFS to local file.", ce);

                    return;
                }
                canEditMetadata = false;
            }
            creationDateSupplier = createDefaultCreationDateSupplier(() -> {
                try {
                    if (!isRemote) {
                        return fs.toLocalFile().toPath();
                    }
                } catch (CoreException e) {
                    LOGGER.error(e);
                }
                return null;
            });
        } else {
            final WorkflowRootEditPart wrep = (WorkflowRootEditPart)selected;
            final WorkflowManagerUI wmUI = wrep.getWorkflowManager();
            final Optional<WorkflowManager> wm = Wrapper.unwrapWFMOptional(wmUI);
            if (wm.isPresent()) {
                final var workflowManager = wm.get();
                if (workflowManager.isComponentProjectWFM()) {
                    m_assetRepresentsAJob.set(false);
                    canEditMetadata = false;
                } else {
                    projectWM = workflowManager.getProjectWFM();
                    m_currentAssetName = projectWM.getName();

                    final WorkflowEditor editor = (WorkflowEditor)PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                        .getActivePage().getActiveEditor();
                    canEditMetadata = (!editor.isTempRemoteWorkflowEditor() && !editor.isTempLocalWorkflowEditor());
                    if (canEditMetadata) {
                        m_editWorkflowManager = new WeakReference<>(projectWM);
                        m_reportedDirty = false;
                    }
                }
                creationDateSupplier =
                    createDefaultCreationDateSupplier(() -> workflowManager.getContext().getCurrentLocation().toPath());
            } else {
                m_assetRepresentsAJob.set(true);
                canEditMetadata = false;
                creationDateSupplier = ZonedDateTime::now;
            }
        }

        currentAssetNameHasChanged();

        final var facilitator = new WorkflowMetadataModelFacilitator();
        WorkflowMetadata metadata = null;
        if (projectWM != null) {
            metadata = CheckUtils.checkNotNull(projectWM.getMetadata());
            loadMetadata(facilitator, metadata);
        } else if ((metadataFile != null) && metadataFile.exists()) {
            try {
                metadata = CheckUtils.checkNotNull(WorkflowMetadata.fromXML(metadataFile.toPath(),
                    MetadataVersion.V1_0));
                loadMetadata(facilitator, metadata);
            } catch (Exception e) {
                LOGGER.error("Failed to parse the workflow metadata file.", e);
                return;
            }
        } else if ((legacyMetadataFile != null) && legacyMetadataFile.exists()) {
            try (final var inputStream = Files.newInputStream(legacyMetadataFile.toPath())) {
                final var contents = WorkflowSetMetaParser.parse(inputStream);
                metadata = WorkflowMetadata.fromWorkflowSetMeta(contents);
                loadMetadata(facilitator, metadata);
            } catch (Exception e) {
                LOGGER.error("Failed to parse the workflow metadata file.", e);
                return;
            }
        }

        m_modelFacilitator = facilitator;
        m_modelFacilitator.parsingHasFinishedWithDefaultTitleName(m_currentAssetName, creationDateSupplier, metadata);
        m_modelFacilitator.setModelObserver(this);

        if (m_metadataCanBeEdited.get() != canEditMetadata) {
            m_metadataCanBeEdited.set(canEditMetadata);
            configureFloatingHeaderBarButtons();
        }

        if (!isDisposed()) {
            getDisplay().asyncExec(() -> {
                if (!isDisposed()) {
                    updateDisplay();
                }
            });
        }
    }

    private static void loadMetadata(final WorkflowMetadataModelFacilitator facilitator,
        final WorkflowMetadata workflowMetadata) {
        facilitator.processElement(null, MetadataItemType.DESCRIPTION.getType(),
            workflowMetadata.getDescription().orElse(""), false, Map.of("newStyle", "true",
                "html", Boolean.toString(workflowMetadata.getContentType() == ContentType.HTML)));

        workflowMetadata.getAuthor().ifPresent(author -> {
            facilitator.processElement(null, MetadataItemType.AUTHOR.getType(), author, false, null);
        });

        workflowMetadata.getCreated().ifPresent(created -> {
            final var dateString = MetaInfoFile.dateToStorageString(created.getDayOfMonth(),
                created.getMonthValue(), created.getYear());
            facilitator.processElement(null, MetadataItemType.CREATION_DATE.getType(), dateString, false, null);
        });

        workflowMetadata.getTags() //
                .forEach(tag -> facilitator.processElement(null, MetadataItemType.TAG.getType(), tag, false, null));

        workflowMetadata.getLinks().forEach(link -> {
            facilitator.processElement(null, MetadataItemType.LINK.getType(), link.text(), false,
                Map.of(MetadataXML.URL_TYPE_ATTRIBUTE, MetadataXML.URL_LEGACY_KEYWORD_TYPE_NAME,
                    MetadataXML.URL_URL_ATTRIBUTE, link.url()));
        });
    }

    private static Supplier<ZonedDateTime> createDefaultCreationDateSupplier(final Supplier<Path> workflowPath) {
        return () -> {
            final var dateTime = ZonedDateTime.now();
            try {
                final var path = workflowPath.get();
                return path == null ? dateTime : setFallbackCreationDateFromWorkflowFile(dateTime, workflowPath.get());
            } catch (IOException | InvalidSettingsException e) {
                LOGGER.error("The creation date couldn't be extracted from the workflow.", e);
            }
            return dateTime;
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void completeSave() {
        final var oldWFM = Optional.ofNullable(m_editWorkflowManager).map(WeakReference::get).orElse(null);
        if (oldWFM == null) {
            throw new IllegalStateException("Can't save metadata because the containing workflow was closed.");
        }
        oldWFM.setContainerMetadata(((WorkflowMetadataModelFacilitator)m_modelFacilitator).getMetadata());
    }

    @Override
    protected void editModeChanged(final boolean dirty) {
        if (m_reportedDirty == dirty) {
            return;
        }
        m_reportedDirty = dirty;

        // notify the editor that we are done
        final var oldWFM = m_editWorkflowManager == null ? null : m_editWorkflowManager.get();
        if (oldWFM == null) {
            return;
        }

        Optional.of(PlatformUI.getWorkbench()) //
                .map(IWorkbench::getActiveWorkbenchWindow) //
                .map(IWorkbenchWindow::getActivePage) //
                .stream() //
                .flatMap(page -> Arrays.stream(page.getEditorReferences())) //
                .map(ref -> ref.getEditor(false)) //
                .filter(WorkflowEditor.class::isInstance) //
                .map(WorkflowEditor.class::cast) //
                .filter(editor -> editor.getWorkflowManager().filter(wfm -> wfm.equals(oldWFM)).isPresent()) //
                .findAny() //
                .ifPresent(editor -> editor.setMetadataEditorDirty(dirty));
    }

    @Override
    protected boolean isEditorAvailable() {
        final var oldWFM = m_editWorkflowManager == null ? null : m_editWorkflowManager.get();
        if (oldWFM == null) {
            return false;
        }

        return Optional.of(PlatformUI.getWorkbench()) //
                .map(IWorkbench::getActiveWorkbenchWindow) //
                .map(IWorkbenchWindow::getActivePage) //
                .stream() //
                .flatMap(page -> Arrays.stream(page.getEditorReferences())) //
                .map(ref -> ref.getEditor(false)) //
                .filter(WorkflowEditor.class::isInstance) //
                .map(WorkflowEditor.class::cast) //
                .anyMatch(editor -> editor.getWorkflowManager().filter(wfm -> wfm.equals(oldWFM)).isPresent());
    }

    /**
     * Helper method to read the creation date from the workflow file (<code>workflow.knime</code>) and set as the new
     * time of the provided calendar. Usually used as a fallback when, e.g., no <code>workflowset.meta</code> file is
     * given.
     *
     * @param dateTime the calendar to set the new creation date into
     * @param workflowPath the workflow directory
     * @throws IOException
     * @throws InvalidSettingsException
     */
    private static ZonedDateTime setFallbackCreationDateFromWorkflowFile(final ZonedDateTime dateTime,
            final Path workflowPath) throws IOException, InvalidSettingsException {
        final var creationDateFromWorkflowFile = getCreationDateFromWorkflowFile(workflowPath);
        return creationDateFromWorkflowFile == null ? dateTime : creationDateFromWorkflowFile;
    }

    private static ZonedDateTime getCreationDateFromWorkflowFile(final Path workflowPath)
        throws IOException, InvalidSettingsException {
        final var workflowFile = workflowPath.resolve(WorkflowPersistor.WORKFLOW_FILE);
        if (Files.exists(workflowFile)) {
            final var settings = new NodeSettings("ignored");
            try (final var inStream = Files.newInputStream(workflowFile)) {
                settings.load(inStream);
                if (!settings.containsKey("authorInformation")) {
                    return null;
                }
                final var authoredWhen = settings.getConfigBase("authorInformation").getString("authored-when");
                return ZonedDateTime.parse(authoredWhen, AUTHORED_WHEN_FORMAT);
            }
        } else {
            throw new FileNotFoundException(
                "Failed to read creation date from workflow. File '" + workflowFile + "' not found");
        }
    }

}
