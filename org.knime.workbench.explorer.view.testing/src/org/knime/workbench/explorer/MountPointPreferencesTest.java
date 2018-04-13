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

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.knime.workbench.explorer.view.preferences.MountSettings;

/**
 *
 * @author Ole Ostergaard, KNIME GmbH, Konstanz, Germany
 */
public class MountPointPreferencesTest {

    /**
     * Testcase for AP-8989
     *
     * Loads all MountSettings, adds a new MountSetting and checks if they are all saved and loaded correctly after that.
     *
     * @throws Exception if an errors occurs
     */
    @Test
    public void testMountPointLoading() throws Exception {
        List<MountSettings> initialSettings = MountSettings.loadSortedMountSettingsFromPreferenceNode();
        int numberOfSettings = initialSettings.size();

        List<String> initialMountIDs= initialSettings.stream().map(ms -> ms.getMountID()).collect(Collectors.toList());

        assertThat(initialMountIDs, Matchers.containsInAnyOrder("test-mountpoint1", "test-mountpoint2"));


        String newMountPointString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<config xmlns=\"http://www.knime.org/2008/09/XMLConfig\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.knime.org/2008/09/XMLConfig http://www.knime.org/XMLConfig_2008_09.xsd\" key=\"mountSettings\">\n<config key=\"mountSettings_0\">\n<entry key=\"mountID\" type=\"xstring\" value=\"newMountSettings\"/>\n<entry key=\"displayName\" type=\"xstring\" value=\"newMountSettings (knimeuser@https://testing.knime.org/tomee/ejb)\"/>\n<entry key=\"factoryID\" type=\"xstring\" value=\"com.knime.explorer.server\"/>\n<entry key=\"content\" type=\"xstring\" value=\"https://testing.knime.org/tomee/ejb;ole.ostergaard;false;\"/>\n<entry key=\"defaultMountID\" type=\"xstring\" isnull=\"true\" value=\"\"/>\n<entry key=\"active\" type=\"xboolean\" value=\"true\"/>\n</config>\n<entry key=\"mountPointNumber\" type=\"xint\" value=\"0\"/>\n</config>\n";


        MountSettings newMountSettings = MountSettings.parseSingleSetting(newMountPointString, true);


        initialSettings.add(newMountSettings);

        MountSettings.saveMountSettings(initialSettings);

        List<MountSettings> modifiedSettings = MountSettings.loadSortedMountSettingsFromPreferenceNode();


        List<String> newMountIDs= modifiedSettings.stream().map(ms -> ms.getMountID()).collect(Collectors.toList());

        assertThat(modifiedSettings.size(), Matchers.is(numberOfSettings+1));
        assertThat(newMountIDs, Matchers.containsInAnyOrder("test-mountpoint1", "test-mountpoint2", newMountSettings.getMountID()));

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
        List<MountSettings> initialSettings = MountSettings.loadSortedMountSettingsFromPreferenceNode();
        int numberOfSettings = initialSettings.size();

        String defaultMountPoint1SettingsString = null;
        for (Iterator<MountSettings> iterator = initialSettings.iterator(); iterator.hasNext();) {
            MountSettings defaultSettings = iterator.next();
            if (defaultSettings.getMountID().equals("test-mountpoint1")) {
                defaultMountPoint1SettingsString = defaultSettings.getSettingsString();
            }
        }

        assertThat(defaultMountPoint1SettingsString, Matchers.notNullValue());

        String overWriteMountPoint1SettingsString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<config xmlns=\"http://www.knime.org/2008/09/XMLConfig\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.knime.org/2008/09/XMLConfig http://www.knime.org/XMLConfig_2008_09.xsd\" key=\"mountSettings\">\n<config key=\"mountSettings_0\">\n<entry key=\"mountID\" type=\"xstring\" value=\"test-mountpoint1\"/>\n<entry key=\"displayName\" type=\"xstring\" value=\"test-mountpoint1 (newKNIMEUser@https://testing.knime.org/tomee/ejb)\"/>\n<entry key=\"factoryID\" type=\"xstring\" value=\"com.knime.explorer.server\"/>\n<entry key=\"content\" type=\"xstring\" value=\"https://testing.knime.org/tomee/ejb;ole.ostergaard;false;\"/>\n<entry key=\"defaultMountID\" type=\"xstring\" isnull=\"true\" value=\"\"/>\n<entry key=\"active\" type=\"xboolean\" value=\"true\"/>\n</config>\n<entry key=\"mountPointNumber\" type=\"xint\" value=\"0\"/>\n</config>\n";


        MountSettings newMountSettings = MountSettings.parseSingleSetting(overWriteMountPoint1SettingsString, true);

        initialSettings.add(newMountSettings);

        MountSettings.saveMountSettings(initialSettings);

        List<MountSettings> modifiedSettings = MountSettings.loadSortedMountSettingsFromPreferenceNode();

        assertThat(modifiedSettings.size(), Matchers.equalTo(numberOfSettings));

        String overwrittenMountPoint1Settings = null;
        for (Iterator<MountSettings> iterator = modifiedSettings.iterator(); iterator.hasNext();) {
            MountSettings mount = iterator.next();
            if (mount.getMountID().equals("test-mountpoint1")) {
                overwrittenMountPoint1Settings = mount.getSettingsString();
            }
        }

        assertThat(overwrittenMountPoint1Settings, Matchers.notNullValue());
        assertThat(defaultMountPoint1SettingsString, Matchers.not(Matchers.equalTo(overwrittenMountPoint1Settings)));
    }
}
