package org.knime.workbench.explorer.view.actions.validators;

import org.eclipse.jface.dialogs.IInputValidator;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.ExplorerFileSystem;

/**
 * Checks for valid {@link AbstractExplorerFileStore} file names.
 *
 * @author Dominik Morent, KNIME.com, Zurich, Switzerland
 *
 */
public class FileStoreNameValidator implements IInputValidator {
    /**
     * {@inheritDoc}
     */
    @Override
    public String isValid(final String name) {
        if (!ExplorerFileSystem.isValidFilename(name.trim())) {
            return "One of the following illegal characters is used: "
                    + ExplorerFileSystem.getIllegalFilenameChars();
        } else {
            return null;
        }
    }

}