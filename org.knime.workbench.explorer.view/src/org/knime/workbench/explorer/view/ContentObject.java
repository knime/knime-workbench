/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2008 - 2011
 * KNIME.com, Zurich, Switzerland
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
 */
package org.knime.workbench.explorer.view;


/**
 * Used as object in the treeview of the user space. Wraps the objects provided
 * by the different content providers. Stores the original object and a
 * reference to the creator/provider. <br />
 * (The original approach with a hash map doesn't work because the original
 * objects may not implement equals. Actually the eclipse resources don't.)
 *
 * @author ohl, University of Konstanz
 */
public final class ContentObject {

    private final Object m_obj;

    private final AbstractContentProvider m_creator;

    /**
     * @param creator
     * @param o
     */
    ContentObject(final AbstractContentProvider creator, final Object o) {
        if (creator == null) {
            throw new NullPointerException(
                    "Creator (provider) can't be null for Object " + o);
        }
        m_creator = creator;
        m_obj = o;
    }

    /**
     * @return the wrapped object
     */
    public Object getObject() {
        return m_obj;
    }

    /**
     * @return the creator of the wrapped object.
     */
    AbstractContentProvider getProvider() {
        return m_creator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_obj.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ContentObject) {
            ContentObject co = (ContentObject)obj;
            if (m_creator != co.m_creator) {
                return false;
            }
            if (m_obj == co.m_obj) {
                return true;
            }
            if (m_obj != null) {
                return m_obj.equals(co);
            }
            return false;
        }
        return false;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_obj.hashCode();
    }
}
