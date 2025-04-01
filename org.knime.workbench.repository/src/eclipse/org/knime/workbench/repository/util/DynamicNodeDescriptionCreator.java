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
 * History
 *   16.08.2007 (Fabian Dill): created
 */
package org.knime.workbench.repository.util;

import java.io.FileNotFoundException;
import java.util.Set;

import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.ui.node.workflow.NodeContainerUI;
import org.knime.core.ui.node.workflow.SingleNodeContainerUI;
import org.knime.core.ui.node.workflow.SubNodeContainerUI;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.node.workflow.lazy.LazyWorkflowManagerUI;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.IRepositoryObject;
import org.knime.workbench.repository.model.MetaNodeTemplate;
import org.knime.workbench.repository.model.NodeTemplate;

/**
 * This class has two functions static and dynamic HTML creation.
 *
 * 1) Static HTML: When the helpview plugin is loaded all installed Nodes are
 * retrieved from the RepositoryManager and their XML descriptions are converted
 * into HTML files which are placed in this plugins html directory, which makes
 * them available in the Eclipse help contents.
 *
 * 2) Dynamic HTML: Based on the selection of nodes in the workflow or
 * nodes/categories in the repository manager the description is dynamically
 * created by either using the node's XML description (node selected) or a
 * listing of all short descriptions (multiple node or category selection).
 *
 * @author Fabian Dill, University of Konstanz
 */
@SuppressWarnings("restriction")
public final class DynamicNodeDescriptionCreator {

    private static final DynamicNodeDescriptionCreator INSTANCE = new DynamicNodeDescriptionCreator();

    private DynamicNodeDescriptionCreator() {
    }

    /**
     * Returns the single instance of this class.
     *
     * @return singleton instance of this class
     */
    public static DynamicNodeDescriptionCreator instance() {
        return INSTANCE;
    }

    /**
     *
     * @return the HTML header with stylesheet import and opened body tag.
     */
    public static String getHeader() {
        StringBuilder content = new StringBuilder();
        content.append("<html><head>");
        // include stylesheet
        content.append("<style>");
        content.append(NodeFactoryHTMLCreator.instance.getCss());
        content.append("</style>");
        content.append("</head><body>");
        return content.toString();
    }

    /**
     * Adds the single line description for all nodes contained in the category
     * (and all sub categories) to the StringBuilder. It will separate the lines
     * by a HTML new line tag.
     *
     * @param cat the category to add the descriptions for.
     * @param bld the buffer to add the one line strings to.
     * @param idsDisplayed a set of IDs of categories and templates already
     *            displayed. Items appearing twice will be skipped.
     */
    public void addDescription(final Category cat, final StringBuilder bld,
            final Set<String> idsDisplayed) {
        bld.append("<dl>");
        bld.append("<dt><h2>In <b>");
        bld.append(StringEscapeUtils.escapeHtml4(cat.getName()));
        bld.append("</b>:</h2></dt> \n");
        if (!cat.hasChildren()) {
            bld.append("<dd> - contains no nodes - </dd>");
        } else {
            bld.append("<dd><dl>");
            for (IRepositoryObject child : cat.getChildren()) {
                if (child instanceof Category childCat) {
                    if (!idsDisplayed.contains(childCat.getID())) {
                        idsDisplayed.add(childCat.getID());
                        addDescription(childCat, bld, idsDisplayed);
                    }
                } else if (child instanceof NodeTemplate templ) {
                    if (!idsDisplayed.contains(templ.getID())) {
                        idsDisplayed.add(templ.getID());
                        addDescription(templ, /* useSingleLine */true, bld);
                    }
                } else if (child instanceof MetaNodeTemplate templ) {
                    if (!idsDisplayed.contains(templ.getID())) {
                        idsDisplayed.add(templ.getID());
                        addDescription(((MetaNodeTemplate)child).getManager(), /* useSingleLine */true, bld);
                    }
                } else {
                    bld.append(" - contains unknown object (internal err!) -");
                }
            }
            bld.append("</dl></dd>");
        }
        bld.append("</dl>");
    }

    /**
     * Adds the description for the node represented by this node template to
     * the StringBuilder. If useSingleLine is set it will use the simple one
     * line description and add a new line html tag at the end, otherwise it
     * will just add the entire full description of the node to the passed
     * buffer.
     *
     * @param template of the node to add the descriptions for.
     * @param useSingleLine if set the single line description is added,
     *            otherwise the entire full description is added
     * @param bld the buffer to add the one line strings to.
     */
    public static void addDescription(final NodeTemplate template, final boolean useSingleLine,
            final StringBuilder bld) {
        NodeFactory<? extends NodeModel> nf = null;
        try {
            nf = template.createFactoryInstance();
            if (useSingleLine) {
                bld.append("<dt><b>");
                bld.append(StringEscapeUtils.escapeHtml4(nf.getNodeName()));
                bld.append(":</b></dt><dd>");
                bld.append(goodOneLineDescr( //
                    NodeFactoryHTMLCreator.instance.readShortDescriptionFromXML(nf.getXMLDescription())));
                bld.append("</dd>");
            } else {
                bld.append(NodeFactoryHTMLCreator.instance.readFullDescription(nf.getXMLDescription()));
            }
        } catch (Exception e) {
            if (useSingleLine) {
                bld.append("<dt>");
                bld.append(StringEscapeUtils.escapeHtml4(template.getName()));
                bld.append(":</dt>");
                bld.append("<dd>no description available ");
                bld.append("(couldn't inst. NodeFactory!)</dd>");
            } else {
                bld.append("<html><body><b>");
                bld.append(StringEscapeUtils.escapeHtml4(template.getName()));
                bld.append("<br><br></b>");
                bld.append("Full description not available.<br>");
                bld.append("(Internal error: couldn't instantiate ");
                bld.append("NodeFactory!)</body></html>");
            }
        }
    }

    /**
     * Adds the description for the node represented by this node edit part to
     * the StringBuilder. If useSingleLine is set it will use the simple one
     * line description and add a new line html tag at the end, otherwise it
     * will just add the entire full description of the node to the passed
     * buffer.
     *
     * @param nc the node to add the descriptions for.
     * @param useSingleLine if set the single line description is added,
     *            otherwise the entire full description is added
     * @param bld the buffer to add the one line strings to.
     */
    public void addDescription(final NodeContainerUI nc, final boolean useSingleLine, final StringBuilder bld) {

        if (!(nc instanceof SingleNodeContainerUI singleNC)) {
            addSubWorkflowDescription(nc, useSingleLine, bld);
        } else {
            if (useSingleLine) {
                bld.append("<dt><b>");
                bld.append(StringEscapeUtils.escapeHtml4(nc.getName()));
                bld.append(":</b></dt>");
                bld.append("<dd>");
                // TODO functionality disabled
                bld.append(goodOneLineDescr( //
                    NodeFactoryHTMLCreator.instance.readShortDescriptionFromXML(singleNC.getXMLDescription())));
                bld.append("</dd>");
            } else {
                try {
                    bld.append(NodeFactoryHTMLCreator.instance.readFullDescription(singleNC.getXMLDescription()));
                } catch (FileNotFoundException | TransformerFactoryConfigurationError | TransformerException ex) {
                    NodeLogger.getLogger(DynamicNodeDescriptionCreator.class)
                        .error("Could not create HTML node description: " + ex.getMessage(), ex);
                    bld.append("<b>No description available, reason: " + ex.getMessage() + "</b>");
                }
            }
        }
    }

    /**
     *
     * @param template meta node template
     * @param useSingleLine true if several nodes are selected
     * @param builder gathers the HTML content
     */
    public void addDescription(final MetaNodeTemplate template,
            final boolean useSingleLine, final StringBuilder builder) {
        WorkflowManagerUI manager = template.getManager();
        if (!useSingleLine) {
            builder.append(getHeader());
            builder.append("<h1>");
            builder.append(StringEscapeUtils.escapeHtml4(manager.getName()));
            builder.append("</h1>");
            builder.append("<h2>Description:</h2>");
            builder.append("<p>" + StringEscapeUtils.escapeHtml4(template.getDescription()) + "</p>");
            builder.append("<h2>Contained nodes: </h2>");
            for (NodeContainerUI child : manager.getNodeContainers()) {
                addDescription(child, true, builder);
            }
            builder.append("</body></html>");
        } else {
            builder.append("<dt><b>" + StringEscapeUtils.escapeHtml4(manager.getName()) + "</b></dt>");
            builder.append("<dd>" + StringEscapeUtils.escapeHtml4(template.getDescription()) + "</dd>");
        }
    }

    private void addSubWorkflowDescription(final NodeContainerUI nc,
            final boolean useSingleLine, final StringBuilder bld) {
        WorkflowManagerUI wfm;
        if (nc instanceof SubNodeContainerUI subNode) {
            wfm = subNode.getWorkflowManager();
        } else {
            wfm = (WorkflowManagerUI)nc;
        }
        if(wfm instanceof LazyWorkflowManagerUI lazyWFM && !lazyWFM.isLoaded()) {
            String missingDescMessage = "Description will be available once the node has been opened.";
            if (!useSingleLine) {
                bld.append(getHeader());
                bld.append("<h1>");
                bld.append(StringEscapeUtils.escapeHtml4(nc.getName()));
                bld.append("</h1>");
                bld.append("<p>" + missingDescMessage + "</p>");
                bld.append("</body></html>");
            } else {
                bld.append("<dd>");
                bld.append("<dl>");
                bld.append(missingDescMessage);
                bld.append("</dl>");
                bld.append("</dd>");
            }
            return;
        }

        if (!useSingleLine) {
            bld.append(getHeader());
            bld.append("<h1>");
            bld.append(StringEscapeUtils.escapeHtml4(nc.getName()));
            bld.append("</h1>");
            final String customDescription = nc.getCustomDescription();
            if (StringUtils.isNotBlank(customDescription)) {
                bld.append("<h2>Description:</h2>");
                bld.append("<p>" + StringEscapeUtils.escapeHtml4(customDescription) + "</p>");
            }
            bld.append("<h2>Contained nodes: </h2>");
            for (NodeContainerUI child : wfm.getNodeContainers()) {
                addDescription(child, true, bld);
            }
            bld.append("</body></html>");
        } else {
            bld.append("<dt><b>");
            bld.append(StringEscapeUtils.escapeHtml4(nc.getName()) + " contained nodes:");
            bld.append("</b></dt>");
            bld.append("<dd>");
            bld.append("<dl>");
            for (NodeContainerUI child : wfm.getNodeContainers()) {
                addDescription(child, true, bld);
            }
            bld.append("</dl>");
            bld.append("</dd>");
        }
    }

    /**
     * @param oneLineFromFactory the string returned by the factory (could be
     *            null or contain special html characters).
     * @return a not null string containing some (more or less) meaningfull text
     *         with no special characters in html.
     */
    private static String goodOneLineDescr(final String oneLineFromFactory) {
        final var escaped = StringEscapeUtils.escapeHtml4(oneLineFromFactory);
        return StringUtils.defaultIfBlank(escaped, " - No node description available - ");
    }
}
