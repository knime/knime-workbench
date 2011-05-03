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
 * Created: Mar 17, 2011
 * Author: ohl
 */
package org.knime.workbench.explorer;

import java.util.concurrent.atomic.AtomicInteger;

import org.knime.workbench.explorer.view.AbstractContentProvider;
import org.knime.workbench.explorer.view.AbstractContentProviderFactory;


/**
 * Represents a content tree in the KNIME Explorer.
 *
 * @author ohl, University of Konstanz
 */
public class MountPoint {

    // the name it is mounted with
    private final String m_id;

    // the factory that created the provider of this map point
    private final AbstractContentProviderFactory m_parent;

    private final AbstractContentProvider m_contentProvider;

    private final AtomicInteger m_refCount = new AtomicInteger();

    /**
     * Creates a new mount point. Sets the ref count to one.
     *
     * @param id
     */
    MountPoint(final String id, final AbstractContentProvider contentProvider,
            final AbstractContentProviderFactory factory) {
        m_id = id;
        m_parent = factory;
        m_contentProvider = contentProvider;
        m_refCount.set(1);
    }

    public AbstractContentProviderFactory getProviderFactory() {
        return m_parent;
    }

    public AbstractContentProvider getProvider() {
        return m_contentProvider;
    }

    public String getMountID() {
        return m_id;
    }

    /**
     * Incr the ref count by one and return the new value.
     *
     * @return the incremented value
     */
    public int incrRefCount() {
        return m_refCount.incrementAndGet();
    }

    /**
     * Decrement the ref count by one and return the new value.
     *
     * @return the decremented value.
     */
    public int decrRefCount() {
        return m_refCount.decrementAndGet();
    }

    public void dispose() {
        m_contentProvider.dispose();
    }
}
