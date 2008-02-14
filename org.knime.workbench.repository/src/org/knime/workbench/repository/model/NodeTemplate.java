/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   16.03.2005 (georg): created
 */
package org.knime.workbench.repository.model;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.properties.IPropertySource;
import org.knime.core.node.GenericNodeFactory;
import org.knime.core.node.GenericNodeModel;
import org.knime.workbench.repository.model.props.NodePropertySource;

/**
 * Class that realizes a (contributed) node in the repository tree. This is used
 * as a "template" for actual instances of a node in the workflow editor.
 * 
 * Note: The type constants *must* match those defined in the "nodes"- extension
 * point (Node.exsd).
 * 
 * TODO introduce new fields: provider, url, license-tag (free/commercial) ...
 * ???
 * 
 * @author Florian Georg, University of Konstanz
 */
public class NodeTemplate extends AbstractSimpleObject {
    /** Type for nodes that read data. */
    public static final String TYPE_DATA_READER = "data reader";

    /** Type for nodes that transform data. */
    public static final String TYPE_DATA_TRANSFORMER = "data transformer";

    /** Type for learner nodes. */
    public static final String TYPE_LEARNER = "learner";

    /** Type for nodes that use a model make predictions. */
    public static final String TYPE_PREDICTOR = "predictor";

    /** Type for nodes that provide a view on the data/model. */
    public static final String TYPE_VISUALIZER = "visualizer";

    /** Type for nodes that evaluate some model. */
    public static final String TYPE_EVALUATOR = "evaluator";

    /** Type for nodes that are in fact meta nodes. */
    public static final String TYPE_META = "meta";

    
    /** Type for nodes that can't be assigned to one of the other types. */
    public static final String TYPE_OTHER = "other";

    private static final Set<String> TYPES = new HashSet<String>();

    static {
        TYPES.add(TYPE_DATA_READER);
        TYPES.add(TYPE_DATA_TRANSFORMER);
        TYPES.add(TYPE_LEARNER);
        TYPES.add(TYPE_PREDICTOR);
        TYPES.add(TYPE_VISUALIZER);
        TYPES.add(TYPE_EVALUATOR);
        TYPES.add(TYPE_META);
        TYPES.add(TYPE_OTHER);
    }

    private Class<GenericNodeFactory<? extends GenericNodeModel>> m_factory;

    private Image m_icon;

    private String m_categoryPath;

    private String m_type;
    
    private String m_pluginID;
    

    /**
     * Constructs a new NodeTemplate.
     * 
     * @param id The id, usually parsed from the extension
     */
    public NodeTemplate(final String id) {
        setID(id);
    }

    /**
     * @return Returns the factory.
     */
    @SuppressWarnings("unchecked")
    public Class<GenericNodeFactory<? extends GenericNodeModel>> getFactory() {
        return m_factory;
    }

    /**
     * @param factory The factory to set.
     */
    @SuppressWarnings("unchecked")
    public void setFactory(
            final Class<GenericNodeFactory<? extends GenericNodeModel>> 
            factory) {
        m_factory = factory;
    }

    /**
     * @return Returns the categoryPath.
     */
    public String getCategoryPath() {
        return m_categoryPath;
    }

    /**
     * @param categoryPath The categoryPath to set.
     */
    public void setCategoryPath(final String categoryPath) {
        m_categoryPath = categoryPath;
    }




    /**
     * @return Returns the icon.
     */
    public Image getIcon() {
        return m_icon;
    }

    /**
     * @param icon The icon to set.
     */
    public void setIcon(final Image icon) {
        m_icon = icon;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object getAdapter(final Class adapter) {
        if (adapter == IPropertySource.class) {
            return new NodePropertySource(this);
        }

        return super.getAdapter(adapter);
    }

    /**
     * @return Returns the type.
     */
    public String getType() {
        return m_type;
    }

    /**
     * @param type The type to set.
     */
    public void setType(final String type) {
        assert TYPES.contains(type) : "Illegal node type: " + type;
        m_type = type;
    }

    /**
     * @return Returns the pluginID.
     */
    public String getPluginID() {
        return m_pluginID;
    }

    /**
     * @param pluginID The pluginID to set.
     */
    public void setPluginID(final String pluginID) {
        m_pluginID = pluginID;
    }
}
