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
 *   Jul 18, 2019 (hornm): created
 */
package org.knime.workbench.explorer.view.dnd;

import static org.knime.core.util.URIUtil.createEncodedURI;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.dnd.URLTransfer;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;
import org.knime.core.util.pathresolve.ResolverUtil;
import org.knime.workbench.core.imports.EntityImport;
import org.knime.workbench.core.imports.RepoObjectImport;
import org.knime.workbench.core.imports.RepoObjectImport.RepoObjectType;
import org.knime.workbench.core.imports.URIImporter;
import org.knime.workbench.core.imports.URIImporterFinder;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ContentDelegator;
import org.knime.workbench.explorer.view.ExplorerView;
import org.knime.workbench.explorer.view.actions.PasteFromClipboardAction;

/**
 * Provides (and 'encapsulates') the logic required to validate and perform drops from an URI into the Explorer. The
 * same logic is used for both, (copy&)paste ({@link PasteFromClipboardAction}) and (drag'n')drop
 * ({@link ExplorerDropListener}).
 *
 * Another reason for this code separation (apart from making it re-usable for paste and drop) is to not clutter the
 * validate/perform drop code in {@link ExplorerDropListener} even more.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 * @since 8.5
 */
public class ExplorerURIDropUtil {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(ExplorerURIDropUtil.class);

    private ExplorerURIDropUtil() {
        //utility class
    }

    /**
     * Validates the drop/paste of an uri into a given target (e.g. a workflow group). If a URI is given it uses the
     * {@link URIImporterFinder} to check its validity. If a target is given it (additionally) uses the target's
     * {@link AbstractContentProvider#validateDrop(AbstractExplorerFileStore, int, TransferData)} method to checks the
     * validity.
     *
     * @param uri can be <code>null</code> (e.g. while a URL is dragged it is often not available here, yet -
     *            OS-dependent)
     * @param target where the URI is supposed to be dropped, can be <code>null</code> (will return <code>false</code>
     *            then)
     * @param transferData the transfer data, if <code>null</code>, a default transfer data type is used (i.e. the first
     *            entry of {@link URLTransfer#getSupportedTypes()})
     * @return <code>true</code> if it's a valid drop (i.e. URI and target are valid)
     */
    public static boolean validateDrop(final String uri, final AbstractExplorerFileStore target,
        final TransferData transferData) {
        if (target == null) {
            return false;
        }
        if (uri != null && uri.startsWith("http")) {
            URI encodedURI = createEncodedURI(uri).orElse(null);
            if (encodedURI == null) {
                return false;
            }

            //is there a URIImporter for the given URI?
            Optional<URIImporter> uriImport = URIImporterFinder.getInstance().findURIImporterFor(encodedURI);
            if (uriImport.isPresent()) {
                Optional<Class<? extends EntityImport>> entityImportClass =
                    uriImport.get().getEntityImportClass(encodedURI);
                //is the entity the URI represents a repo object (e.g. workflow)?
                if (!entityImportClass.isPresent()
                    || !RepoObjectImport.class.isAssignableFrom(entityImportClass.get())) {
                    return false;
                }
            }
        }
        TransferData td;
        if (transferData == null) {
            //hacky!
            td = URLTransfer.getInstance().getSupportedTypes()[0];
        } else {
            td = transferData;
        }
        //let content provider (i.e. mount point) validate the drop, too
        return target.getContentProvider().validateDrop(target, DND.DROP_COPY, td);
    }

    /**
     * Performs the drop/paste, main steps:
     * <ul>
     * <li>download the object to a temporary location</li>
     * <li>open import wizard in case of a workflow or workflow group and import</li>
     * <li>refresh explorer view</li>
     * <li>delete temporary file</li>
     * </ul>
     * Will open the import wizard in case of a workflow and workflow group. Once the operation is finished successfully
     * the explorer view will be refreshed.
     *
     * @param uri the URI to drop/paste that represents an object, such as a workflow, workflow group or data file
     * @param view the view the drop/paste is performed into
     * @param target the target (e.g. a workflow group) to drop the object
     * @return <code>true</code> if the drop was successful
     */
    public static boolean performDrop(final String uri, final ExplorerView view,
        final AbstractExplorerFileStore target) {
        URI knimeURI = createEncodedURI(uri.split("\n")[0]).orElse(null);
        if (knimeURI == null) {
            return false;
        }
        try {
            AtomicReference<File> file = new AtomicReference<File>();
            AtomicReference<File> tmpDir = new AtomicReference<File>();
            PlatformUI.getWorkbench().getProgressService().busyCursorWhile((monitor) -> {
                monitor.beginTask("Downloading file...", 100);
                try {
                    Optional<EntityImport> entityImport =
                        URIImporterFinder.getInstance().createEntityImportFor(knimeURI);
                    if (!entityImport.isPresent()) {
                        LOGGER.warn("Object at URI '" + knimeURI + "' not found");
                        return;
                    }
                    RepoObjectImport objImport = ((RepoObjectImport)entityImport.get());
                    URI dataURI = objImport.getDataURI();
                    File tmpFile = ResolverUtil.resolveURItoLocalOrTempFile(dataURI, monitor);
                    tmpDir.set(FileUtil.createTempDir("download"));

                    //change file extension according to the data object to paste
                    RepoObjectType objType = objImport.getType();
                    String fileExt = "";
                    if (objType == RepoObjectType.Workflow) {
                        fileExt = "." + KNIMEConstants.KNIME_WORKFLOW_FILE_EXTENSION;
                    } else if (objType == RepoObjectType.WorkflowGroup) {
                        fileExt = "." + KNIMEConstants.KNIME_ARCHIVE_FILE_EXTENSION;
                    }

                    file.set(new File(tmpDir.get(), objImport.getName() + fileExt));
                    if (!tmpFile.renameTo(file.get())) {
                        LOGGER.warn("Pasting failed. The temporary file '" + file.get() + "' couldn't be renamed to '"
                            + file.get() + "'");
                        return;
                    }
                } catch (IOException e) {
                    LOGGER.warn("Object from URI '" + knimeURI + "' couldn't be pasted", e);
                    return;
                }
            });
            if (file.get() == null) {
                return false;
            }

            boolean result = target.getContentProvider().performDrop(view, new String[]{file.get().getAbsolutePath()},
                target, DND.DROP_COPY);
            view.getViewer().refresh(ContentDelegator.getTreeObjectFor(target));

            //remove temporary directory
            if (!file.get().delete() || !tmpDir.get().delete()) {
                LOGGER.warn("The temporary file '" + file.get() + "' couldn't be deleted");
            }
            return result;
        } catch (InvocationTargetException | InterruptedException e) {
            LOGGER.warn("Object from URI '" + knimeURI + "' couldn't be pasted", e);
            return false;
        }
    }

}
