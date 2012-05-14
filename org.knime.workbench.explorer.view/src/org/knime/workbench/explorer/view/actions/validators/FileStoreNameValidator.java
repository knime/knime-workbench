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
    private boolean m_initialDisplay;

    /**
     * Creates a new FileStoreNameValidator.
     */
    public FileStoreNameValidator() {
        this(null);
    }

    /**
     * Creates a new FileStoreNameValidator with an initial name that only
     * validates to true, if the name has been changed.
     *
     * @param initialName the initial name to validate against
     */
    public FileStoreNameValidator(final String initialName) {
        m_initialName = initialName;
        m_initialDisplay = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String isValid(final String name) {
        if (m_initialName != null) {
            if (m_initialName.equals(name)) {
                if (m_initialDisplay) {
                    m_initialDisplay = false;
                    return "";
                } else {
                    return "Name is unchanged.";
                }
            }
        }
        if (!ExplorerFileSystem.isValidFilename(name.trim())) {
            return "One of the following illegal characters is used: "
                    + ExplorerFileSystem.getIllegalFilenameChars();
        } else {
            return null;
        }
    }

}