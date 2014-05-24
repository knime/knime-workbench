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
     * @param id the mount id
     * @param contentProvider the content provider
     * @param factory the content provider factory
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
