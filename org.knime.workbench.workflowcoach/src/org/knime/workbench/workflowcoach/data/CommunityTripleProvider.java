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
 *   Mar 17, 2016 (hornm): created
 */
package org.knime.workbench.workflowcoach.data;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.ui.workflowcoach.data.NodeTripleProvider;
import org.knime.core.ui.workflowcoach.data.NodeTripleProviderFactory;
import org.knime.workbench.workflowcoach.prefs.WorkflowCoachPreferenceInitializer;
import org.knime.workbench.workflowcoach.prefs.WorkflowCoachPreferencePage;
import org.osgi.framework.FrameworkUtil;

/**
 * Reads the node triples from a json file that was originally generated from the KNIME usage statistics.
 *
 * @author Martin Horn, University of Konstanz
 */
public class CommunityTripleProvider extends AbstractFileDownloadTripleProvider {
    private static final ScopedPreferenceStore PREFS = new ScopedPreferenceStore(InstanceScope.INSTANCE,
        FrameworkUtil.getBundle(CommunityTripleProvider.class).getSymbolicName());

    /**
     * Factory for {@link CommunityTripleProvider}s.
     *
     * @author Thorsten Meinl, KNIME AG, Zurich, Switzerland
     */
    public static final class Factory implements NodeTripleProviderFactory {
        /**
         * {@inheritDoc}
         */
        @Override
        public List<NodeTripleProvider> createProviders() {
            return Collections.singletonList(new CommunityTripleProvider());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getPreferencePageID() {
            return WorkflowCoachPreferencePage.ID;
        }

    }

    /**
     * Creates a new provider that fetched recommendation from the KNIME web page.
     */
    public CommunityTripleProvider() {
        super("https://update.knime.com/community_recommendations_5.2.json", "community_recommendations_5.2.json");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update() throws Exception {
        super.update();
        Files.deleteIfExists(Paths.get(KNIMEConstants.getKNIMEHomeDir(), "community_recommendations.json"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "Community";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Frequency of how often the KNIME community used this node.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return PREFS.getBoolean(WorkflowCoachPreferenceInitializer.P_COMMUNITY_NODE_TRIPLE_PROVIDER);
    }
}
