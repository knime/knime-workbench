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

import static org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore.isWorkflowGroup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.Optional;
import java.util.function.Supplier;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.descriptionview.metadata.AbstractMetaView;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileInfo;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileInfo;
import org.knime.workbench.explorer.localworkspace.LocalWorkspaceFileInfo;
import org.knime.workbench.explorer.view.ContentObject;

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
    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowMetaView.class);


    private File m_metadataFile;

    /**
     * @param parent
     */
    public WorkflowMetaView(final Composite parent) {
        super(parent, EnumSet.of(HiddenSection.UPPER, HiddenSection.LOWER));
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
        final Calendar creationDate, final boolean shouldShowCCBY40License) {
        m_modelFacilitator = new MetadataModelFacilitator(author, legacyDescription, creationDate);
        m_modelFacilitator.parsingHasFinishedWithDefaultTitleName(m_currentAssetName,
            () -> creationDate == null ? Calendar.getInstance() : creationDate);
        m_modelFacilitator.setModelObserver(this);

        if (m_metadataCanBeEdited.get()) {
            m_metadataCanBeEdited.set(false);
            configureFloatingHeaderBarButtons();
        }

        m_metadataFile = null;

        m_waitingForAsynchronousMetadata.set(false);
        m_asynchronousMetadataFetchFailed
            .set((author == null) && (legacyDescription == null) && (creationDate == null));
        m_assetRepresentsATemplate.set(false);
        m_assetRepresentsAJob.set(false);
        m_shouldDisplayLicenseSection.set(shouldShowCCBY40License);
        getDisplay().asyncExec(() -> {
            updateDisplay();
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void selectionChanged(final IStructuredSelection selection) {
        final Object o = selection.getFirstElement();
        final boolean knimeExplorerItem = (o instanceof ContentObject);

        final File metadataFile;
        final boolean canEditMetadata;

        m_waitingForAsynchronousMetadata.set(false);
        m_asynchronousMetadataFetchFailed.set(false);
        m_assetRepresentsATemplate.set(false);
        m_assetRepresentsAJob.set(false);
        m_shouldDisplayLicenseSection.set(!SHOW_LICENSE_ONLY_FOR_HUB);
        m_assetIsReadable.set(true);
        final Supplier<Calendar> defaultCreationDateSupplier;
        if (knimeExplorerItem) {
            final AbstractExplorerFileStore fs = ((ContentObject) o).getFileStore();
            final AbstractExplorerFileInfo fileInfo = fs.fetchInfo();
            if (!(fileInfo instanceof RemoteExplorerFileInfo) && !(fileInfo instanceof LocalWorkspaceFileInfo)) {
                LOGGER.debug("Received unexpected file info type: " + fileInfo.getClass());
                return;
            }

            final boolean isWorkflow = AbstractExplorerFileStore.isWorkflow(fs);
            final boolean isTemplate = AbstractExplorerFileStore.isWorkflowTemplate(fs);
            final boolean isRemote = fs.getContentProvider().isRemote();
            final boolean isJob = isRemote ? ((RemoteExplorerFileInfo)fileInfo).isWorkflowJob() : false;
            final boolean isReadable = fs.fetchInfo().isReadable();
            final boolean validFS =
                (isWorkflow || AbstractExplorerFileStore.isWorkflowGroup(fs) || isTemplate || isJob);
            if (!validFS) {
                return;
            }

            m_currentAssetName = fs.getName();
            if (isRemote || isTemplate || isJob) {
                m_waitingForAsynchronousMetadata.set(isRemote && !isTemplate && !isJob);
                m_assetRepresentsATemplate.set(isTemplate);
                m_assetRepresentsAJob.set(isJob);
                m_assetIsReadable.set(isReadable);

                metadataFile = null;
                canEditMetadata = false;
            } else {
                final AbstractExplorerFileStore metaInfo = fs.getChild(WorkflowPersistor.METAINFO_FILE);
                try {
                    metadataFile = metaInfo.toLocalFile(EFS.NONE, null);
                } catch (final CoreException ce) {
                    LOGGER.error("Unable to convert EFS to local file.", ce);

                    return;
                }
                canEditMetadata = true;
            }
            defaultCreationDateSupplier = createDefaultCreationDateSupplier(() -> {
                try {
                    if (!isRemote && !isWorkflowGroup(fs)) {
                        return fs.toLocalFile().toPath();
                    }
                } catch (CoreException e) {
                    LOGGER.error(e);
                }
                return null;
            });
        } else {
            final WorkflowRootEditPart wrep = (WorkflowRootEditPart)o;
            final WorkflowManagerUI wmUI = wrep.getWorkflowManager();
            final Optional<WorkflowManager> wm = Wrapper.unwrapWFMOptional(wmUI);
            if (wm.isPresent()) {
                if (wm.get().isComponentProjectWFM()) {
                    m_assetRepresentsAJob.set(false);
                    metadataFile = null;
                    canEditMetadata = false;
                } else {
                    final WorkflowManager projectWM = wm.get().getProjectWFM();
                    final ReferencedFile rf = projectWM.getWorkingDir();

                    metadataFile = new File(rf.getFile(), WorkflowPersistor.METAINFO_FILE);
                    m_currentAssetName = projectWM.getName();

                    final WorkflowEditor editor = (WorkflowEditor)PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                        .getActivePage().getActiveEditor();
                    canEditMetadata = (!editor.isTempRemoteWorkflowEditor() && !editor.isTempLocalWorkflowEditor());
                }
                defaultCreationDateSupplier =
                    createDefaultCreationDateSupplier(() -> wm.get().getContext().getCurrentLocation().toPath());
            } else {
                m_assetRepresentsAJob.set(true);
                metadataFile = null;
                canEditMetadata = false;
                defaultCreationDateSupplier = Calendar::getInstance;
            }
        }

        if ((m_metadataFile != null) && m_metadataFile.equals(metadataFile)) {
            return;
        }

        currentAssetNameHasChanged();

        if ((metadataFile != null) && metadataFile.exists()) {
            final SAXInputHandler handler = new SAXInputHandler();
            try {
                final SAXParserFactory parserFactory = SAXParserFactory.newInstance();
                parserFactory.setNamespaceAware(true);

                final SAXParser parser = parserFactory.newSAXParser();
                parser.parse(metadataFile, handler);
            } catch (Exception e) {
                LOGGER.error("Failed to parse the workflow metadata file.", e);

                return;
            }

            m_modelFacilitator = handler.getModelFacilitator();
        } else {
            m_modelFacilitator = new MetadataModelFacilitator();
        }
        m_modelFacilitator.parsingHasFinishedWithDefaultTitleName(m_currentAssetName, defaultCreationDateSupplier);
        m_modelFacilitator.setModelObserver(this);

        if (m_metadataCanBeEdited.get() != canEditMetadata) {
            m_metadataCanBeEdited.set(canEditMetadata);
            configureFloatingHeaderBarButtons();
        }

        m_metadataFile = metadataFile;

        getDisplay().asyncExec(() -> {
            if (!isDisposed()) {
                updateDisplay();
            }
        });
    }

    private static Supplier<Calendar> createDefaultCreationDateSupplier(final Supplier<Path> workflowPath) {
        return () -> {
            Calendar calendar = Calendar.getInstance();
            try {
                Path path = workflowPath.get();
                if (path != null) {
                    setFallbackCreationDateFromWorkflowFile(calendar, workflowPath.get());
                }
            } catch (IOException | InvalidSettingsException | ParseException e) {
                LOGGER.error("The creation date couldn't be extracted from the workflow.", e);
            }
            return calendar;
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void completeSave() {
        try {
            final Path metadata = Paths.get(m_metadataFile.getAbsolutePath());
            final String metadataXML = ((MetadataModelFacilitator)m_modelFacilitator).metadataSavedInLegacyFormat();
            final Job job = new WorkspaceJob("Saving workflow metadata...") {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public IStatus runInWorkspace(final IProgressMonitor monitor) throws CoreException {
                    try {
                        Files.write(metadata, metadataXML.getBytes("UTF-8"), StandardOpenOption.CREATE,
                            StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                    } catch (final IOException e) {
                        throw new CoreException(new Status(IStatus.ERROR, KNIMEEditorPlugin.PLUGIN_ID, -1,
                            "Failed to save the metadata to file.", e));
                    }
                    return Status.OK_STATUS;
                }
            };
            job.setRule(ResourcesPlugin.getWorkspace().getRoot());
            job.setUser(true);
            job.schedule();
        } catch (final IOException e) {
            LOGGER.error("Failed to save metadata.", e);
        }
    }

    /**
     * Helper method to read the creation date from the workflow file (<code>workflow.knime</code>) and set as the new
     * time of the provided calendar. Usually used as a fallback when, e.g., no <code>workflowset.meta</code> file is
     * given.
     *
     * @param calendar the calendar to set the new creation date into
     * @param workflowPath the workflow directory
     * @throws IOException
     * @throws InvalidSettingsException
     * @throws ParseException
     */
    public static void setFallbackCreationDateFromWorkflowFile(final Calendar calendar, final Path workflowPath)
        throws IOException, InvalidSettingsException, ParseException {
        Date creationDateFromWorkflowFile = getCreationDateFromWorkflowFile(workflowPath);
        if (creationDateFromWorkflowFile != null) {
            calendar.setTime(creationDateFromWorkflowFile);
        }
    }

    private static Date getCreationDateFromWorkflowFile(final Path workflowPath)
        throws IOException, InvalidSettingsException, ParseException {
        Path workflowFile = workflowPath.resolve(WorkflowPersistor.WORKFLOW_FILE);
        if (Files.exists(workflowFile)) {
            NodeSettings ns = new NodeSettings("ignored");
            try (final InputStream is = Files.newInputStream(workflowFile)) {
                ns.load(is);
                final String s = ns.getConfigBase("authorInformation").getString("authored-when");
                final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
                return df.parse(s);
            }
        } else {
            throw new FileNotFoundException(
                "Failed to read creation date from workflow. File '" + workflowFile + "' not found");
        }
    }

}
