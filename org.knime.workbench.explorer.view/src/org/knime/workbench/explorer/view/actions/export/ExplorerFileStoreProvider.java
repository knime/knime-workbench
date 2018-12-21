/*
 * ------------------------------------------------------------------------
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
 * Created: May 31, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer.view.actions.export;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;

/**
 *
 * @author ohl, University of Konstanz
 */
public class ExplorerFileStoreProvider extends LabelProvider implements
        IStructuredContentProvider, ITreeContentProvider {

    private static final Object[] NONE = new Object[0];

    private AbstractExplorerFileStore m_root;

    private AbstractContentProvider m_provider;

    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] getElements(final Object inputElement) {
        return getChildren(inputElement);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object[] getChildren(final Object parentElement) {
        if (parentElement instanceof AbstractExplorerFileStore) {
            AbstractExplorerFileStore[] childs = m_provider.getChildren(
                    parentElement);
            // can only export workflows, groups and dirs
            ArrayList<AbstractExplorerFileStore> result =
                    new ArrayList<AbstractExplorerFileStore>();
            for (AbstractExplorerFileStore c : childs) {
                if (AbstractExplorerFileStore.isWorkflowGroup(c) || WorkflowExportPage.isExportElement(c)) {
                    result.add(c);
                }
            }
            return result.toArray();
        } else if (parentElement instanceof Collection) {
            return ((Collection<?>)parentElement).toArray();
        }
        return NONE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getParent(final Object element) {
        if (element instanceof AbstractExplorerFileStore) {
            AbstractExplorerFileStore child
                    = (AbstractExplorerFileStore)element;
            if (child.equals(m_root)) {
                // don't traverse up beyond the root - the tree will be confused
                return null;
            }
            return child.getParent();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasChildren(final Object element) {
        return getChildren(element).length > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void inputChanged(final Viewer viewer, final Object oldInput,
            final Object newInput) {
        m_root = null;
        if (newInput instanceof AbstractExplorerFileStore) {
            AbstractExplorerFileStore input
                    = (AbstractExplorerFileStore)newInput;
            m_root = input;
            m_provider = input.getContentProvider();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Image getImage(final Object element) {
        if (element instanceof AbstractExplorerFileStore) {
            AbstractExplorerFileStore e = (AbstractExplorerFileStore)element;
            Image i = AbstractContentProvider.getWorkspaceImage(e);
            if (i != null) {
                return i;
            }
            IFileInfo info = e.fetchInfo();
            if (info.isDirectory()) {
                return ImageRepository.getIconImage(SharedImages.Folder);
            } else {
                return ImageRepository.getIconImage(SharedImages.File2);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText(final Object element) {
        if (element instanceof AbstractExplorerFileStore) {
            return ((AbstractExplorerFileStore)element).getName();
        }
        return super.getText(element);
    }
}
