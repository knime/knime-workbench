/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 * Created: Apr 12, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer.filesystem;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.core.runtime.Path;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.MountPoint;
import org.knime.workbench.explorer.view.AbstractContentProvider;

/**
 *
 * @author ohl, University of Konstanz
 */
public class ExplorerFileSystem extends FileSystem {
    /**
     * Singleton instance of the file system.
     * @since 3.0
     */
    public static final ExplorerFileSystem INSTANCE = new ExplorerFileSystem();

    /**
     * The scheme this file system is registered with (see extension point
     * "org.eclipse.core.filesystem.filesystems").
     */
    public static final String SCHEME = "knime";

    /**
     * Characters that are unfortunate to use in most file systems and in URIs.
     * Slashes are disallowed in the name, but are separators though. It is not
     * guaranteed that these characters are not used but usage should be
     * avoided.
     */
    private static final String ILLEGAL_FILENAME_CHARS;
    private static final Pattern ILLEGAL_FILENAME_CHARS_PATTERN;

    static {
        ILLEGAL_FILENAME_CHARS = "*?#:\"<>%~|/\\";
        // double escape backslashes for regular expression
        ILLEGAL_FILENAME_CHARS_PATTERN =
            Pattern.compile("[" + ILLEGAL_FILENAME_CHARS.replace(
                    "\\", "\\\\") + "]+");
    }

    /**
     * Please consider using the singleton instance {@link #INSTANCE} instead.
     */
    public ExplorerFileSystem() {
        // needed by the Eclipse framework (EFS)
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractExplorerFileStore getStore(final URI uri) {
        String mountID = getIDfromURI(uri);
        MountPoint mountPoint = ExplorerMountTable.getMountPoint(mountID);
        if (mountPoint == null) {
            return null;
        }
        return mountPoint.getProvider().getFileStore(uri.getPath());
    }

    /**
     * The ID of the mount point (referred by in the URIs). As specified
     * (upper/lower cases) by the user.
     *
     * @return
     */
    private static String getIDfromURI(final URI uri) {
        if (SCHEME.equalsIgnoreCase(uri.getScheme())) {
            return uri.getHost();
        }
        throw new IllegalArgumentException("Invalid scheme in URI ('"
                + uri.getScheme() + "'). Only '" + SCHEME
                + "' is allowed here.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canDelete() {
        return canWrite();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canWrite() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LocalExplorerFileStore fromLocalFile(final File file) {
        for (AbstractContentProvider acp
                : ExplorerMountTable.getMountedContent().values()) {
            LocalExplorerFileStore fromLocalFile = acp.fromLocalFile(file);
            if (fromLocalFile != null) {
                return fromLocalFile;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCaseSensitive() {
        return false;
    }

    /**
     * If the child path starts with the parent path (ignoring case).
     *
     * @param parent the file to check if it's the parent
     * @param child the file to check if it's a child
     * @return true, if the child is contained in the parent or any sub folder
     *         of the parent.
     */
    static boolean isParent(final File parent, final File child) {
        try {
            return isParent(parent.getCanonicalPath(),
                    child.getCanonicalPath());
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * If the child path starts with the parent path (ignoring case).
     *
     * @param parent the file to check if it's the parent
     * @param child the file to check if it's a child
     * @return true, if the child is contained in the parent or any sub folder
     *         of the parent.
     */
    static boolean isParent(final String parent, final String child) {
        if (parent != null && child != null) {
            Path childPath = new Path(child);
            Path parentPath = new Path(parent);
            if (childPath.segmentCount() <= parentPath.segmentCount()) {
                return false;
            }
            Path childsParent =
                    (Path)childPath.removeLastSegments(childPath.segmentCount()
                            - parentPath.segmentCount());
            for (int s = childsParent.segmentCount() - 1; s >= 0; s--) {
                if (!childsParent.segment(s).equalsIgnoreCase(
                        parentPath.segment(s))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Returns true, if the specified filename doesn't contain invalid
     * characters.
     *
     * @param name to test.
     * @return null, if the specified filename doesn't contain invalid
     *         characters, error message otherwise
     * @since 3.8
     */
    public static String validateFilename(final String name) {
        if (name == null || name.isEmpty()) {
            return "No name provided.";
        }
        if (name.startsWith(".")) {
            return "Name cannot start with dot.";
        }
        if (name.endsWith(".")) {
            return "Name cannot end with dot.";
        }
        Matcher matcher = ILLEGAL_FILENAME_CHARS_PATTERN.matcher(name);
        if (matcher.find()) {
            return "Name contains invalid characters ("
                    + ExplorerFileSystem.getIllegalFilenameChars() + ").";
        }
        return null;
    }

    /**
     * @return the characters that are invalid for file names
     */
    public static String getIllegalFilenameChars() {
        return ILLEGAL_FILENAME_CHARS;
    }

}
