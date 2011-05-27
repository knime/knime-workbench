/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright (c) KNIME.com, Zurich, Switzerland
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
 * Created: Apr 12, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer.filesystem;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.core.runtime.Path;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.MountPoint;

/**
 *
 * @author ohl, University of Konstanz
 */
public class ExplorerFileSystem extends FileSystem {

    /**
     * The scheme this file system is registered with (see extension point
     * "org.eclipse.core.filesystem.filesystems").
     */
    public static final String SCHEME = "knime";

    /**
     * {@inheritDoc}
     */
    @Override
    public ExplorerFileStore getStore(final URI uri) {
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
    public IFileStore fromLocalFile(final File file) {
        // TODO Auto-generated method stub
        return super.fromLocalFile(file);
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
     * @param parent
     * @param child
     * @return true, if the child is contained in the parent or any sub folder
     *         of the parent.
     */
    static boolean isParent(final File parent, final File child) {
        try {
            return isParent(parent.getCanonicalPath(), child.getCanonicalPath());
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * If the child path starts with the parent path (ignoring case).
     *
     * @param parent
     * @param child
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
     * Characters that are unfortunate to use in most file systems and in URIs.
     * Slashes are disallowed in the name, but are separators though. It is not
     * guaranteed that these characters are not used but usage should be
     * avoided.
     */
    public static final String ILLEGAL_FILENAME_CHARS = "*?#:\"<>|%~";

    /**
     * Returns true, if the specified path doesn't contain invalid characters.
     * Slashes and backslashes are valid, as they are considered separators -
     * and the argument could contain a full path. See
     * {@link #isValidFilename(String)}.
     *
     * @param path to test.
     * @return true, if the specified path doesn't contain invalid characters.
     */
    public static boolean isValidPath(final String path) {
        String forbidden = ILLEGAL_FILENAME_CHARS;
        if (path != null) {
            for (int i = 0; i < path.length(); i++) {
                if (forbidden.indexOf(path.charAt(i)) >= 0) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns true, if the specified filename doesn't contain invalid
     * characters. This is the same method as {@link #isValidPath(String)},
     * except slashes are not allowed either.
     *
     * @param simplename to test.
     * @return true, if the specified filename doesn't contain invalid
     *         characters.
     */
    public static boolean isValidFilename(final String simplename) {
        if (!isValidPath(simplename)) {
            return false;
        }
        // separators are disallowed too in simple file names.
        if (simplename.indexOf('/') >= 0 || simplename.indexOf('\\') >= 0) {
            return false;
        }
        return true;
    }

}
