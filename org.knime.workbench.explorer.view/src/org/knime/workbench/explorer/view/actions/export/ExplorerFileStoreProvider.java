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
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.explorer.filesystem.ExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;

/**
 *
 * @author ohl, University of Konstanz
 */
public class ExplorerFileStoreProvider extends LabelProvider implements
        IStructuredContentProvider, ITreeContentProvider {

    private static final Object[] NONE = new Object[0];

    private static final Image FILE_IMG = ExplorerActivator
            .imageDescriptorFromPlugin(ExplorerActivator.PLUGIN_ID,
                    "icons/file.png").createImage();

    private static final Image DIR_IMG = ExplorerActivator
            .imageDescriptorFromPlugin(ExplorerActivator.PLUGIN_ID,
                    "icons/folder.png").createImage();

    private ExplorerFileStore m_root;

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
        if (parentElement instanceof ExplorerFileStore) {
            ExplorerFileStore[] childs = m_provider.getChildren(
                    parentElement);
            // can only export workflows, groups and dirs
            ArrayList<ExplorerFileStore> result =
                    new ArrayList<ExplorerFileStore>();
            for (ExplorerFileStore c : childs) {
                if (ExplorerFileStore.isDirOrWorkflowGroup(c)
                        || ExplorerFileStore.isWorkflow(c)) {
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
        if (element instanceof ExplorerFileStore) {
            ExplorerFileStore child = (ExplorerFileStore)element;
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
        if (newInput instanceof ExplorerFileStore) {
            ExplorerFileStore input = (ExplorerFileStore)newInput;
            m_root = input;
            m_provider = input.getContentProvider();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Image getImage(final Object element) {
        if (element instanceof ExplorerFileStore) {
            ExplorerFileStore e = (ExplorerFileStore)element;
            Image i = AbstractContentProvider.getWorkspaceImage(e);
            if (i != null) {
                return i;
            }
            IFileInfo info = e.fetchInfo();
            if (info.isDirectory()) {
                return DIR_IMG;
            } else {
                return FILE_IMG;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getText(final Object element) {
        if (element instanceof ExplorerFileStore) {
            return ((ExplorerFileStore)element).getName();
        }
        return super.getText(element);
    }
}
