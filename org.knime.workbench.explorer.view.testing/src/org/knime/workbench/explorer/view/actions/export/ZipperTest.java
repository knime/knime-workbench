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
 *   Jan 24, 2020 (wiswedel): created
 */
package org.knime.workbench.explorer.view.actions.export;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.knime.workbench.explorer.ExplorerActivator;
import org.zeroturnaround.zip.ZipUtil;

/**
 * Came into exsistence as part of AP-13538: Empty folders in workflow directory are missing from export
 * https://knime-com.atlassian.net/browse/AP-13538
 * @author wiswedel
 */
public class ZipperTest {

    private static final String ROOT_FOLDER_NAME = "AP-13538-empty-folders-export";

    @Rule
    public TemporaryFolder m_tempParentFolder= new TemporaryFolder();
    private File m_filesFolder;

    @Before
    public void setUp() throws URISyntaxException, IOException {
        final String name = "files";
        URL testFolderURL = FileLocator.toFileURL(Platform.getBundle(ExplorerActivator.PLUGIN_ID).getResource(name));
        assertTrue("unable to locate folder containing \"" + name + "\"", testFolderURL != null);
        assertThat("protocol of test folder URL", testFolderURL.getProtocol(), is("file"));
        m_filesFolder = new File(testFolderURL.getFile()); // not toURI() etc due to spaces in path
        Map<Object, List<File>> fileAndFolders= indexFilesAndFoldersIn(new File(m_filesFolder, ROOT_FOLDER_NAME).toPath());
        assertThat("File count in source folder", fileAndFolders.get(Boolean.FALSE).size(), is(4));
        assertThat("Folder count in source folder", fileAndFolders.get(Boolean.TRUE).size(), is(5));
    }

    /**
     * Zip all folders that contain files, don't specify the folder names in the arg list. This closely resembles what
     * used to happen before providing a fix for AP-13538.
     */
    @Test
    public void testZipFilesWithoutEmptyDir() throws Exception {
        File targetZipFile = m_tempParentFolder.newFile("ZipperTest.zip");
        File targetExtracted= m_tempParentFolder.newFolder("ZipperTest");
        Map<Object, List<File>> sourceIndices = indexFilesAndFoldersIn(new File(m_filesFolder, ROOT_FOLDER_NAME).toPath());
        Zipper.zipFiles(sourceIndices.get(Boolean.FALSE), targetZipFile, m_filesFolder.toPath().getNameCount(), new NullProgressMonitor());
        ZipUtil.unpack(targetZipFile, targetExtracted);
        File extractedRoot = new File(targetExtracted, ROOT_FOLDER_NAME);
        assertThat("Root folder \"" + ROOT_FOLDER_NAME + "\" in extracted zip exists and is directory",
            extractedRoot.isDirectory(), is(extractedRoot.isDirectory()));
        Map<Object, List<File>> indexInExtracted = indexFilesAndFoldersIn(extractedRoot.toPath());
        assertThat("Folder count in extracted zip", indexInExtracted.get(Boolean.TRUE).size(), is(3));
        assertThat("File count in extracted zip", indexInExtracted.get(Boolean.FALSE).size(), is(4));
    }

    /** Additionally specifies folders that must be part of the archive (= empty folders).
     * Non-empty folders are not part of the list. */
    @Test
    public void testZipFilesWithEmptyDir() throws URISyntaxException, IOException {
        File targetZipFile = m_tempParentFolder.newFile("ZipperTest.zip");
        File targetExtracted= m_tempParentFolder.newFolder("ZipperTest");
        List<File> allFilesToZip = new ArrayList<File>();
        try (Stream<Path> dirStream = Files.walk(m_filesFolder.toPath())) {
            for (Iterator<Path> it = dirStream.iterator(); it.hasNext();) {
                File f = it.next().toFile();
                if (f.isFile() || f.list().length == 0) {
                    allFilesToZip.add(f);
                }
            }
        }
        Zipper.zipFiles(allFilesToZip, targetZipFile, m_filesFolder.toPath().getNameCount(), new NullProgressMonitor());
        ZipUtil.unpack(targetZipFile, targetExtracted);
        File extractedRoot = new File(targetExtracted, ROOT_FOLDER_NAME);
        assertThat("Root folder \"" + ROOT_FOLDER_NAME + "\" in extracted zip exists and is directory",
            extractedRoot.isDirectory(), is(extractedRoot.isDirectory()));
        Map<Object, List<File>> indexInExtracted = indexFilesAndFoldersIn(extractedRoot.toPath());
        assertThat("Folder count in extracted zip", indexInExtracted.get(Boolean.TRUE).size(), is(5));
        assertThat("File count in extracted zip", indexInExtracted.get(Boolean.FALSE).size(), is(4));
    }

    /** Specifies all folders and files found in 'files'. Not really called by KNIME AP but should work anyway.*/
    @Test
    public void testZipFilesWithAllFilesAndFolders() throws Exception {
        File targetZipFile = m_tempParentFolder.newFile("ZipperTest.zip");
        File targetExtracted= m_tempParentFolder.newFolder("ZipperTest");
        List<File> allFilesToZip;
        try (Stream<Path> dirStream = Files.walk(m_filesFolder.toPath())) {
            allFilesToZip = dirStream.map(Path::toFile).collect(Collectors.toList());
        }
        Zipper.zipFiles(allFilesToZip, targetZipFile, m_filesFolder.toPath().getNameCount(), new NullProgressMonitor());
        ZipUtil.unpack(targetZipFile, targetExtracted);
        File extractedRoot = new File(targetExtracted, ROOT_FOLDER_NAME);
        assertThat("Root folder \"" + ROOT_FOLDER_NAME + "\" in extracted zip exists and is directory",
            extractedRoot.isDirectory(), is(extractedRoot.isDirectory()));
        Map<Object, List<File>> indexInExtracted = indexFilesAndFoldersIn(extractedRoot.toPath());
        assertThat("Folder count in extracted zip", indexInExtracted.get(Boolean.TRUE).size(), is(5));
        assertThat("File count in extracted zip", indexInExtracted.get(Boolean.FALSE).size(), is(4));
    }

    /** A map with:
     * TRUE -> the list of folders within the argument path (excluding the arg path)
     * FALSE -> the list of files in that path
     */
    static Map<Object, List<File>> indexFilesAndFoldersIn(final Path path) throws IOException {
        try (Stream<Path> s = Files.walk(path)) {
            return s.filter(p -> !p.equals(path)).collect(Collectors.groupingBy(p -> Files.isDirectory(p),
                Collectors.mapping(Path::toFile, Collectors.toList())));
        }
    }

}
