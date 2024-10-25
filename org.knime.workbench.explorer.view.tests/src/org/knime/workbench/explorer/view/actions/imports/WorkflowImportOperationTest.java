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
 *   Nov 5, 2024 (lw): created
 */
package org.knime.workbench.explorer.view.actions.imports;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.rules.TemporaryFolder;
import org.knime.core.internal.KNIMEPath;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.TestContentProviderFactory;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;

/**
 * Tests the behavior of {@link AbstractExplorerFileStore} with the {@link WorkflowImportOperation}.
 */
class WorkflowImportOperationTest {

    private static final String TEST_FOLDER_NAME = "AP-23419-non-empty-import-rename";

    @Rule
    private TemporaryFolder m_temporaryFolder = new TemporaryFolder();

    /**
     * Copies contents of a workflow (basically just a directory) recursively from source to target.
     *
     * @param sourcePath workflow to be copied
     * @param targetParentPath directory into which the source should be copied
     * @return actual path of the copied workflow
     * @throws IOException
     */
    private static Path copyWorkflowContents(final Path sourcePath, final Path targetPath) throws IOException {
        try (var stream = Files.walk(sourcePath)) {
            final var iterator = stream.iterator();
            while (iterator.hasNext()) {
                final var path = iterator.next();
                Files.copy(path, targetPath.resolve(sourcePath.relativize(path)), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return targetPath;
    }

    /**
     * Recursively collects all contents of a workflow and stored them as children of a {@link IWorkflowImportElement}
     * that is then returns as their parent.
     *
     * @param sourcePath workflow directory to be resolved
     * @return collection of import elements stored as children
     * @throws IOException
     */
    private static IWorkflowImportElement resolveWorkflowContents(final Path sourcePath) throws IOException {
        final var sourceElement = new WorkflowImportElementFromFile(sourcePath.toFile(), true);
        try (var stream = Files.walk(sourcePath)) {
            final var iterator = stream.iterator();
            while (iterator.hasNext()) {
                final var path = iterator.next();
                if (path == sourcePath) {
                    continue;
                }
                sourceElement.addChild(new WorkflowImportElementFromFile(path.toFile(), true));
            }
        }
        return sourceElement;
    }

    /**
     * Compares a source to a target directory. More specifically checks recursively whether every source file and
     * directory is contained in the target directory.
     *
     * @param sourcePath reference directory
     * @param targetPath comparison directory
     * @return true if all elements of source are present in target, false otherwise
     * @throws IOException
     */
    private static boolean compareWorkflowContents(final Path sourcePath, final Path targetPath) throws IOException {
        try (var stream = Files.walk(sourcePath)) {
            final var iterator = stream.iterator();
            while (iterator.hasNext()) {
                final var path = iterator.next();
                if (path == sourcePath) {
                    continue;
                }
                if (!Files.exists(targetPath.resolve(sourcePath.relativize(path)))) {
                    return false;
                }
            }
        }
        return true;
    }

    @BeforeEach
    void setUp() throws IOException {
        m_temporaryFolder.create();
    }

    @Test
    void testImportWithRename() throws IOException, InvocationTargetException, InterruptedException {
        // finding test directory "files" with workflow to import to (renamed with "-copy")
        final var testFilesURL =
            FileLocator.toFileURL(Platform.getBundle(ExplorerActivator.PLUGIN_ID).getResource("files"));
        final var testWorkflowPath = Paths.get(testFilesURL.getPath()).resolve(TEST_FOLDER_NAME);
        final var testWorkflowCopyPath = testWorkflowPath.resolveSibling(TEST_FOLDER_NAME + "-copy");

        // prepare target destination where the original workflow lives
        final var target = KNIMEPath.getWorkspaceDirPath().toPath().relativize(testWorkflowPath).getParent();
        final var targetElement =
            new TestContentProviderFactory().createContentProvider("LOCAL").fromLocalFile(target.toFile());

        // prepare source destination at temporary folder with suffix "-copy"
        final var source = copyWorkflowContents(testWorkflowPath,
            m_temporaryFolder.getRoot().toPath().resolveSibling(testWorkflowCopyPath.getFileName()));
        final var sourceElement = resolveWorkflowContents(source);

        // perform the import operation and check that source and target are equal
        new WorkflowImportOperation(sourceElement, targetElement).execute(new NullProgressMonitor());
        assertThat("Imported workflow target should have the same contents as its source",
            compareWorkflowContents(source, testWorkflowCopyPath)
                && compareWorkflowContents(testWorkflowCopyPath, source));
        assertThat("Workflow copy from the import should have been cleaned up",
            FileUtils.deleteQuietly(testWorkflowCopyPath.toFile()));
    }

    @AfterEach
    void tearDown() {
        m_temporaryFolder.delete();
    }
}
