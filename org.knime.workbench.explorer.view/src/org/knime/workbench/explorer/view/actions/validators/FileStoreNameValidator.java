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
    private final String m_initialName;

    /**
     * Create a new file store validator that checks that the name was changed.
     * @param name the initial name of the file
     */
    public FileStoreNameValidator(final String name) {
        m_initialName = name;
    }

    /**
     * Create a new file store validator.
     */
    public FileStoreNameValidator() {
        m_initialName = null;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public String isValid(final String name) {
        if (name == null || name.isEmpty()) {
            return "Please choose a name";
        } else if (name.equals(m_initialName)) {
            return "Please choose a new name";
        } else if (!ExplorerFileSystem.isValidFilename(name.trim())) {
            return "One of the following illegal characters is used: "
                    + ExplorerFileSystem.getIllegalFilenameChars();
        } else {
            return null;
        }
    }

}