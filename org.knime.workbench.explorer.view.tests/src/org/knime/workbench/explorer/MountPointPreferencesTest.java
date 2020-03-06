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
 *   Apr 11, 2018 (oole): created
 */
package org.knime.workbench.explorer;

import static org.junit.Assert.assertThat;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.Test;
import org.knime.core.node.workflow.BatchExecutor;
import org.knime.core.util.CoreConstants;
import org.knime.workbench.explorer.view.preferences.MountSettings;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 *
 * @author Ole Ostergaard, KNIME GmbH, Konstanz, Germany
 */
public class MountPointPreferencesTest {
    /**
     * Loads mount point preferences before the tests.
     *
     * @throws Exception if an error occurs
     */
    @BeforeClass
    public static void loadPreferences() throws Exception {
        Bundle myself = FrameworkUtil.getBundle(MountPointPreferencesTest.class);
        URL url = FileLocator.find(myself, new Path("/files/testing.epf"), null);
        URL fileUrl = FileLocator.toFileURL(url);
        BatchExecutor.setPreferences(new File(fileUrl.getFile()));
    }

    /**
     * Testcase for AP-8989
     *
     * Loads all MountSettings, adds a new MountSetting and checks if they are all saved and loaded correctly after that.
     *
     * @throws Exception if an errors occurs
     */
    @Test
    public void testMountPointLoading() throws Exception {
        ExplorerMountTable.unmount(CoreConstants.KNIME_HUB_MOUNT_ID);
        List<MountSettings> initialSettings = MountSettings.loadSortedMountSettingsFromPreferenceNode();
        int numberOfSettings = initialSettings.size();

        List<String> initialMountIDs = initialSettings.stream().map(ms -> ms.getMountID()).collect(Collectors.toList());

        assertThat(initialMountIDs,
            Matchers.containsInAnyOrder("test-mountpoint1", "test-mountpoint2", CoreConstants.KNIME_HUB_MOUNT_ID));



        String mountID = "new-mountpoint";
        String content = "https://testing.knime.org/tomee/ejb;oole;false;;false";
        String displayName = "new-mountpoint (oole@https://testing.knime.org/tomee/ejb)";
        String factoryID = "com.knime.explorer.server";
        int mountPointNumber = 0;
        boolean active = true;

        MountSettings newMountSettings = new MountSettings(mountID, displayName, factoryID, content, "", active, mountPointNumber);


        initialSettings.add(newMountSettings);

        MountSettings.saveMountSettings(initialSettings);

        List<MountSettings> modifiedSettings = MountSettings.loadSortedMountSettingsFromPreferenceNode();


        List<String> newMountIDs= modifiedSettings.stream().map(ms -> ms.getMountID()).collect(Collectors.toList());

        assertThat(modifiedSettings.size(), Matchers.is(numberOfSettings + 1));
        assertThat(newMountIDs, Matchers.containsInAnyOrder("test-mountpoint1", "test-mountpoint2",
            CoreConstants.KNIME_HUB_MOUNT_ID, newMountSettings.getMountID()));

    }

    /**
     * Testcase for AP-8989
     *
     * Loads all MountSettings, overwrites one of the default MountSettings and checks that the default settings is hidden/overwritten.
     *
     * @throws Exception if an errors occurs
     */
    @Test
    public void testDefaultOverwrite() throws Exception {
        ExplorerMountTable.unmount(CoreConstants.KNIME_HUB_MOUNT_ID);
        List<MountSettings> initialSettings = MountSettings.loadSortedMountSettingsFromPreferenceNode();
        int numberOfSettings = initialSettings.size();


        Optional<MountSettings> optMS = initialSettings.stream().filter(ms -> ms.getMountID().equals("test-mountpoint1")).findFirst();

        MountSettings oldMountSettings = optMS.orElse(null);
        assertThat(oldMountSettings, Matchers.notNullValue());

        String mountID = "test-mountpoint1";
        String content = "https://testing.knime.org/tomee/ejb;oole;false;;false;Credentials;/";
        String displayName = "test-mountpoint1 (oole@https://testing.knime.org/tomee/ejb)";
        String factoryID = "com.knime.explorer.server";
        int mountPointNumber = 0;
        boolean active = true;

        MountSettings newMountSettings = new MountSettings(mountID, displayName, factoryID, content, "", active, mountPointNumber);

        initialSettings.add(newMountSettings);

        MountSettings.saveMountSettings(initialSettings);

        List<MountSettings> modifiedSettings = MountSettings.loadSortedMountSettingsFromPreferenceNode();

        assertThat(modifiedSettings.size(), Matchers.equalTo(numberOfSettings + 1));

        Optional<MountSettings> optoverwritteMS =
            modifiedSettings.stream().filter(ms -> ms.getMountID().equals("test-mountpoint1")).findFirst();

        MountSettings overwrittenMountSettings = optoverwritteMS.orElse(null);

        assertThat(overwrittenMountSettings, Matchers.notNullValue());
        assertThat(oldMountSettings, Matchers.not(Matchers.equalTo(overwrittenMountSettings)));
        assertThat(overwrittenMountSettings, Matchers.equalTo(newMountSettings));
    }
}
