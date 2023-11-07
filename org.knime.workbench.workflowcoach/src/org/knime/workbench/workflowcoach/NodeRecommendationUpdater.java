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
 *   Nov 6, 2023 (kai): created
 */
package org.knime.workbench.workflowcoach;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.BiPredicate;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.knime.core.node.NodeLogger;
import org.knime.core.ui.workflowcoach.NodeRecommendationManager;
import org.knime.core.ui.workflowcoach.data.NodeTripleProvider;
import org.knime.core.ui.workflowcoach.data.UpdatableNodeTripleProvider;
import org.knime.workbench.core.KNIMECorePlugin;
import org.knime.workbench.core.preferences.HeadlessPreferencesConstants;
import org.knime.workbench.workflowcoach.data.CommunityTripleProvider;
import org.knime.workbench.workflowcoach.prefs.UpdateJob;
import org.knime.workbench.workflowcoach.prefs.UpdateJob.UpdateListener;
import org.knime.workbench.workflowcoach.prefs.WorkflowCoachPreferenceInitializer;
import org.osgi.framework.FrameworkUtil;

/**
 * Utility class to manager node recommendation updates
 *
 * @author Kai Franze, KNIME GmbH, Germany
 */
public final class NodeRecommendationUpdater {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(NodeRecommendationUpdater.class);

    private static final UpdateListener UPDATE_LISTENER =
        eo -> eo.ifPresent(event -> LOGGER.warn("Could not update node recommendations statistics.", event));

    private static final ScopedPreferenceStore PREFS = new ScopedPreferenceStore(InstanceScope.INSTANCE,
        FrameworkUtil.getBundle(CommunityTripleProvider.class).getSymbolicName());

    private static IPropertyChangeListener propertyChangeListener;

    static {
        if (propertyChangeListener == null) { // Can be overwritten by the `WorkflowCoachView`
            propertyChangeListener = updateRequiredTripleProvidersPreferenceChanges();
            KNIMECorePlugin.getDefault().getPreferenceStore().addPropertyChangeListener(propertyChangeListener);
        }
    }

    private NodeRecommendationUpdater() {
        // Utility class
    }

    /**
     * Updates all required and enable {@link NodeTripleProvider} when the user updated the
     * {@code HeadlessPreferencesConstants.P_SEND_ANONYMOUS_STATISTICS}.
     */
    private static IPropertyChangeListener updateRequiredTripleProvidersPreferenceChanges() {
        return event -> {
            if (event.getProperty().equals(HeadlessPreferencesConstants.P_SEND_ANONYMOUS_STATISTICS)
                && event.getNewValue().equals(Boolean.TRUE)) {
                setCommunityNodeTripleProviderPrefOrLogError(true);
                updateTripleProviders(UPDATE_LISTENER, true, false);
            }
        };
    }

    private static void setCommunityNodeTripleProviderPrefOrLogError(final boolean value) {
        PREFS.setValue(WorkflowCoachPreferenceInitializer.P_COMMUNITY_NODE_TRIPLE_PROVIDER, value);
        try {
            PREFS.save();
        } catch (IOException ex) {
            LOGGER.error("Could not set 'P_COMMUNITY_NODE_TRIPLE_PROVIDER' to 'true'", ex);
        }
    }

    private static boolean thereArePendingUpdates() {
        final var updateSchedule = PREFS.getInt(WorkflowCoachPreferenceInitializer.P_AUTO_UPDATE_SCHEDULE);
        if (updateSchedule == WorkflowCoachPreferenceInitializer.NO_AUTO_UPDATE) {
            return false; // Since no updates shall be performed at all
        }
        return NodeRecommendationManager.getNodeTripleProviders().stream() //
            .map(NodeTripleProvider::getLastUpdate) // Empty optional if no previous update recorded
            .filter(Optional::isPresent) //
            .map(Optional::get) //
            .min(Comparator.naturalOrder()) //
            .map(oldest -> {
                final BiPredicate<Integer, Long> weeklyUpdatePending =
                    (us, diff) -> us == WorkflowCoachPreferenceInitializer.WEEKLY_UPDATE && diff > 0;
                final BiPredicate<Integer, Long> monthlyUpdatePending =
                    (us, diff) -> us == WorkflowCoachPreferenceInitializer.MONTHLY_UPDATE && diff >= 4;
                final var weeksDiff = ChronoUnit.WEEKS.between(oldest, LocalDateTime.now());
                return weeklyUpdatePending.or(monthlyUpdatePending).test(updateSchedule, weeksDiff);
            }) //
            .orElse(true); // Since no previous update was recorded
    }

    /**
     * Sets the {@link IPropertyChangeListener} to be registered for
     * {@link HeadlessPreferencesConstants#P_SEND_ANONYMOUS_STATISTICS} changes.
     *
     * @param listener The listener to register
     */
    public static void setPropertyChangeListener(final IPropertyChangeListener listener) {
        if (propertyChangeListener != null) { // To make sure there is only one at a time
            KNIMECorePlugin.getDefault().getPreferenceStore().removePropertyChangeListener(propertyChangeListener);
        }
        propertyChangeListener = listener;
        KNIMECorePlugin.getDefault().getPreferenceStore().addPropertyChangeListener(propertyChangeListener);
    }

    /**
     * Updates all updatable and enabled triple providers.
     *
     * @param updateListener to get feedback of the updating process
     * @param requiredOnly if only the enabled triple providers should be updated that require an update in order to
     *            work
     * @param block if {@code true} the method will block till the update is finished, otherwise it will return
     *            immediately after triggering the update job
     */
    public static void updateTripleProviders(final UpdateListener updateListener, final boolean requiredOnly,
        final boolean block) {
        final var providersToUpdate = NodeRecommendationManager.getNodeTripleProviders().stream()//
            .filter(ntp -> {
                if (ntp instanceof UpdatableNodeTripleProvider untp) { // Provider is updatable
                    // Provider is enabled (and optionally requires an update)
                    return ntp.isEnabled() && (!requiredOnly || untp.updateRequired());
                }
                return false;
            })//
            .map(UpdatableNodeTripleProvider.class::cast)//
            .toList();
        UpdateJob.schedule(updateListener, providersToUpdate, block);
    }

    /**
     * Checks whether the update (i.e. download) of the node recommendation statistics is necessary, either because they
     * haven't been updated so far, or the update schedule tells to do so. If an update is necessary it is immediately
     * performed.
     *
     * @param block if {@code true} the method will block till the update is finished, otherwise it will return
     *            immediately after triggering the update job
     */
    public static void checkForStatisticUpdates(final boolean block) {
        if (thereArePendingUpdates()) {
            updateTripleProviders(UPDATE_LISTENER, false, block);
        }
    }

}
