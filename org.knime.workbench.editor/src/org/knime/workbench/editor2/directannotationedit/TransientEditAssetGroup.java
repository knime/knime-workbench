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
 *   Feb 15, 2019 (loki): created
 */
package org.knime.workbench.editor2.directannotationedit;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Widget;

/**
 * This class acts somewhat like a radio button group, in the sense that only one member of the group (at most) is
 * allowed to be displaying its transient edit assets.
 *
 * More concretely, given color grid drop downs and numeric editable pulldowns, only one should be allowed to display at
 * any one time; this class and its architecture ensures that.
 *
 * @author loki der quaeler
 */
class TransientEditAssetGroup {
    /**
     * Classes which wish to participate in this class' tracking should implement this interface.
     */
    public interface AssetProvider {
        /**
         * The implementor will be notified via this method as to whether they should ensure their assets are hidden.
         */
        void shouldHideEditAssets();

        /**
         * The implementor should return their edit asset (or one of them, assuming that their visibility is homogenous).
         *
         * @return the implementor's edit asset.
         */
        Widget getEditAsset();
    }


    private final ArrayList<AssetProvider> m_assetProviders;

    TransientEditAssetGroup() {
        m_assetProviders = new ArrayList<>();
    }

    void addAssetProvider(final AssetProvider ap) {
        synchronized(m_assetProviders) {
            m_assetProviders.add(ap);

            ap.getEditAsset().addListener(SWT.Show, (e) -> {
                providerDidDisplayAssets(ap);
            });
        }
    }

    private void providerDidDisplayAssets(final AssetProvider ap) {
        synchronized(m_assetProviders) {
            for (final AssetProvider assetProvider : m_assetProviders) {
                if (assetProvider != ap) {
                    assetProvider.shouldHideEditAssets();
                }
            }
        }
    }
}
