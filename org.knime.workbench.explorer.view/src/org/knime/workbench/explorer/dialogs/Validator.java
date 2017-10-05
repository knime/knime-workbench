/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * Created on 28.04.2014 by ohl
 */
package org.knime.workbench.explorer.dialogs;

import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;

/**
 * Used to validate a new selection in the
 * {@link SpaceResourceSelectionDialog}.
 *
 * @author Peter Ohl, KNIME.com AG, Zurich, Switzerland
 * @since 6.4
 */
public abstract class Validator {


    /**
     * Called whenever the selection in the tree changes. If the name field is not enabled, the parameter name is always
     * null. If the name field is enabled, both values must be validated. If the name field in the dialog is enabled,
     * you probably want to overwrite {@link #validateNewNameValue(String, AbstractExplorerFileStore)}.
     *
     * @param selection new selection value
     * @param currentName (or null if name field not enabled)
     * @return null if the parameter values are acceptable or an error message
     */
    public abstract String validateSelectionValue(final AbstractExplorerFileStore selection, final String currentName);

    /**
     * @param name new value entered in the name field (if enabled)
     * @param currentSelection in the tree
     * @return null if values are acceptable or an error message
     */
    public String validateNewNameValue(final String name, final AbstractExplorerFileStore currentSelection) {
        return validateSelectionValue(currentSelection, name);
    }

    /** Displayed in the label at the bottom of the dialog.
     * @param selection currently selected file store.
     * @param name currently entered name - null if name field is not enabled.
     * @return the resulting path */
    public String getResultPath(final AbstractExplorerFileStore selection, final String name) {
        if (selection != null) {
            return "knime://" + selection.getMountID() + selection.getFullName();
        } else {
            return null;
        }
    }
}
