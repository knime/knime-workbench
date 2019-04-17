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
 *   Apr 11, 2019 (hornm): created
 */
package org.knime.workbench.explorer.templates;

import static org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore.isWorkflowGroup;
import static org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore.isWorkflowTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.explorer.ExplorerMountTable;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.ExplorerJob;
import org.knime.workbench.repository.RepositoryManager;
import org.knime.workbench.repository.model.AbstractRepositoryObject;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.ExplorerMetaNodeTemplate;
import org.knime.workbench.repository.model.Root;
import org.knime.workbench.repository.view.AbstractRepositoryView;

/**
 * Collects the metanode templates from all mounted mountpoints.
 *
 * @author Martin Horn, KNIME GmbH, Konstanz, Germany
 */
public class CollectMetanodeTemplatesAndUpdateNodeRepoJob extends ExplorerJob {

    /**
     */
    public CollectMetanodeTemplatesAndUpdateNodeRepoJob() {
        super("Collect metanode templates");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IStatus run(final IProgressMonitor monitor) {
        //create root categories
        Category templatesCat = new Category("metanode_templates", "Templates", "TODO");
        templatesCat.setIcon(ImageRepository.getIconImage(SharedImages.DefaultMetaNodeIcon));

        Map<String, AbstractContentProvider> mountedContent = ExplorerMountTable.getMountedContent();
        for (Entry<String, AbstractContentProvider> mountPoint : mountedContent.entrySet()) {
            AbstractExplorerFileStore fileStore = mountPoint.getValue().getFileStore("/");
            if (!isWorkflowGroup(fileStore)) {
                continue;
            }
            Category mountPointCat = new Category(mountPoint.getKey(), mountPoint.getKey(), "TODO");
            mountPointCat.setIcon(ImageRepository.getIconImage(SharedImages.WorkflowGroup));
            templatesCat.addChild(mountPointCat);

            //traverse entire tree to find metanode templates
            try {
                mountPointCat.addAllChildren(traverseTree(fileStore));
            } catch (CoreException e) {
                throw new RuntimeException(e);
            }
        }

        //add to node repository
        Root root = RepositoryManager.INSTANCE.getRoot();
        if (root.getChildByID("metanode_templates", false) != null) {
            root.removeChild((AbstractRepositoryObject)root.getChildByID("metanode_templates", false));
        }
        root.addChild(templatesCat);

        Display.getDefault().syncExec(() -> {
            IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            AbstractRepositoryView view =
                (AbstractRepositoryView)activePage.findView("org.knime.workbench.repository.view.RepositoryView");
            view.updateRepositoryView(root);
        });
        return Status.OK_STATUS;
    }

    private List<AbstractRepositoryObject> traverseTree(final AbstractExplorerFileStore fs) throws CoreException {
        assert isWorkflowGroup(fs);
        String[] childNames = fs.childNames(EFS.NONE, null);
        List<AbstractRepositoryObject> newChildren = new ArrayList<>(childNames.length);
        for (String name : childNames) {
            AbstractExplorerFileStore child = fs.getChild(name);
            if (isWorkflowGroup(child)) {
                List<AbstractRepositoryObject> objs = traverseTree(child);
                if (!objs.isEmpty()) {
                    //create new category
                    Category cat = new Category(child.getName(), child.getName(), "TODO");
                    cat.setIcon(ImageRepository.getIconImage(SharedImages.WorkflowGroup));
                    cat.addAllChildren(objs);
                    newChildren.add(cat);
                }
            } else if (isWorkflowTemplate(child)) {
                //TODO filter wrapped metanodes only and/or templates that are flagged
                ExplorerMetaNodeTemplate metanodeTemplate = new ExplorerMetaNodeTemplate(name, name, "", "TODO", child);
                metanodeTemplate.setIcon(ImageRepository.getIconImage(SharedImages.MetanodeRepository));
                newChildren.add(metanodeTemplate);
            }
        }
        return newChildren;
    }
}
