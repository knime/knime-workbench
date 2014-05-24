/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * Created: May 2, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer.view;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.core.util.ImageRepository.SharedImages;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 *
 * @author ohl, University of Konstanz
 */
public final class IconFactory {
    public final static IconFactory instance = new IconFactory();


    /** Icon representing the executing state. */
    private static final Image EXECUTING = KNIMEUIPlugin.getDefault().getImage(
            KNIMEUIPlugin.PLUGIN_ID, "icons/project_executing.png");

    /** Icon representing the executed state. */
    private static final Image EXECUTED = KNIMEUIPlugin.getDefault().getImage(
            KNIMEUIPlugin.PLUGIN_ID, "icons/project_executed.png");

    /** Icon representing the configured state. */
    private static final Image CONFIGURED = KNIMEUIPlugin.getDefault()
            .getImage(KNIMEUIPlugin.PLUGIN_ID, "icons/project_configured.png");

    private static final Image ERROR_WORKFLOW = KNIMEUIPlugin.getDefault()
            .getImage(KNIMEUIPlugin.PLUGIN_ID, "icons/project_error.png");

    /** Icon representing a closed workflow. */
    private static final Image CLOSED_WORKFLOW = KNIMEUIPlugin.getDefault()
            .getImage(KNIMEUIPlugin.PLUGIN_ID, "icons/project_closed2.png");

    /** Icon representing a workflow. Neutral.*/
    private static final Image WORKFLOW = KNIMEUIPlugin.getDefault()
            .getImage(KNIMEUIPlugin.PLUGIN_ID, "icons/project_basic.png");

    /** Icon representing a node in the resource navigator. */
    private static final Image NODE = KNIMEUIPlugin.getDefault().getImage(
            KNIMEUIPlugin.PLUGIN_ID, "icons/node.png");

    /** Icon representing a workflow group in the resource navigator. */
    private static final Image WORKFLOW_GROUP = AbstractUIPlugin
            .imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
                    "icons/wf_set.png").createImage();

    private static final Image DEFAULT = AbstractUIPlugin
            .imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
                    "icons/knime_default.png").createImage();

    private static final Image UNKNOWN = AbstractUIPlugin
            .imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
                    "icons/knime_unknown.png").createImage();

    private static final Image UNKNOWN_RED = AbstractUIPlugin
            .imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
                    "icons/knime_unknown_red.png").createImage();

    private static final Image SYNC = KNIMEUIPlugin.getDefault().getImage(
            KNIMEUIPlugin.PLUGIN_ID, "icons/sync.png");

    private static final Image INFO = KNIMEUIPlugin.getDefault().getImage(
            KNIMEUIPlugin.PLUGIN_ID, "icons/info.png");

    private static final Image LOCAL_WS_IMG =
        KNIMEUIPlugin.getDefault().getImage(
                KNIMEUIPlugin.PLUGIN_ID, "icons/workflow_projects.png");

    private static final Image FILE_IMG =
        KNIMEUIPlugin.getDefault().getImage(ExplorerActivator.PLUGIN_ID,
            "icons/file.png");

    private static final Image ANY_FILE_IMG =
        KNIMEUIPlugin.getDefault().getImage(ExplorerActivator.PLUGIN_ID,
            "icons/any_file.png");

    private static final Image DIR_IMG =
        KNIMEUIPlugin.getDefault().getImage(ExplorerActivator.PLUGIN_ID,
            "icons/folder.png");

    private IconFactory() {
    }

    public Image error() {
        return ImageRepository.getImage(SharedImages.Error);
    }

    public Image unknown() {
        return UNKNOWN;
    }

    public Image unknownRed() {
        return UNKNOWN_RED;
    }

    public Image workflowClosed() {
        return WORKFLOW;
    }

    public Image workflowConfigured() {
        return CONFIGURED;
    }

    public Image workflowExecuting() {
        return EXECUTING;
    }

    public Image workflowExecuted() {
        return EXECUTED;
    }

    public Image sync() {
        return SYNC;
    }

    public Image workflowError() {
        return ERROR_WORKFLOW;
    }

    public Image workflowNeutral() {
        return WORKFLOW;
    }

    public Image node() {
        return NODE;
    }

    public Image workflowgroup() {
        return WORKFLOW_GROUP;
    }

    public Image workflowtemplate() {
        return ImageRepository.getImage(SharedImages.MetanodeRepository);
    }

    public Image info() {
        return INFO;
    }

    public Image localWorkspace() {
        return LOCAL_WS_IMG;
    }

    public Image file() {
        return FILE_IMG;
    }

    /**
     * @since 4.0
     */
    public Image any_file() {
        return ANY_FILE_IMG;
    }

    public Image directory() {
        return DIR_IMG;
    }

}
