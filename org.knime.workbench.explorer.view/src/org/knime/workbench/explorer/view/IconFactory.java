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
 * Created: May 2, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer.view;

import org.eclipse.swt.graphics.Image;
import org.knime.workbench.ui.KNIMEUIPlugin;

/**
 *
 * @author ohl, University of Konstanz
 */
public class IconFactory {
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
    private static final Image WORKFLOW_GROUP = KNIMEUIPlugin
            .imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
                    "icons/wf_set.png").createImage();

    /** Icon representing a workflow template in the resource navigator. */
    private static final Image FLOW_TEMPLATE = KNIMEUIPlugin
            .imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
                    "icons/meta/metanode_template.png").createImage();


    private static final Image DEFAULT = KNIMEUIPlugin
            .imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
                    "icons/knime_default.png").createImage();

    private static final Image UNKNOWN = KNIMEUIPlugin
            .imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
                    "icons/knime_unknown.png").createImage();

    private static final Image UNKNOWN_RED = KNIMEUIPlugin
            .imageDescriptorFromPlugin(KNIMEUIPlugin.PLUGIN_ID,
                    "icons/knime_unknown_red.png").createImage();

    private static final Image ERROR = KNIMEUIPlugin.getDefault().getImage(
            KNIMEUIPlugin.PLUGIN_ID, "icons/error.png");

    public Image error() {
        return ERROR;
    }

    public Image unknown() {
        return UNKNOWN;
    }

    public Image unknownRed() {
        return UNKNOWN_RED;
    }

    public Image workflowClosed() {
        return CLOSED_WORKFLOW;
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
}
