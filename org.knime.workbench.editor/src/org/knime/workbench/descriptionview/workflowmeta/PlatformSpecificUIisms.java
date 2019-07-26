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
 *   Jul 22, 2019 (loki): created
 */
package org.knime.workbench.descriptionview.workflowmeta;

import java.util.HashMap;
import java.util.Optional;

import org.eclipse.core.runtime.Platform;

/**
 * As part of implementing AP-12001, we've entered a time when there is too much divergence from platform to platform
 * on, at least, font-related issues. We dole out platform specific details through this class.
 *
 * @author loki der quaeler
 */
public final class PlatformSpecificUIisms {
    /** This will return an instance of Integer **/
    public static final String HEADER_FONT_SIZE_DETAIL = "headerFontSize";
    /** This will return an instance of Integer **/
    public static final String CONTENT_FONT_SIZE_DETAIL = "contentFontSize";
    /** This will return an instance of Double **/
    public static final String FONT_METRICS_CORRECTION_DETAIL = "fontMetricsCorrection";
    /** This will return an instance of String **/
    public static final String BLACK_CIRCLE_UNICODE_DETAIL = "blackCircleUnicode";

    /** Whether the platform is running on Mac **/
    public static final boolean OS_IS_MAC = Platform.OS_MACOSX.equals(Platform.getOS());


    private static final HashMap<String, HashMap<String, Object>> OS_DETAILS_MAP = new HashMap<>();

    // bullets: \u2219 \u25CF \u2022  \u22C5  \u2027  \u00B7
    //      linux: too big: \u25CF  too small: \u00B7, \uu2027, \u2219, \u22C5
    static {
        HashMap<String, Object> detailMap = new HashMap<>();

        // macOS
        detailMap.put(HEADER_FONT_SIZE_DETAIL, new Integer(18));
        detailMap.put(CONTENT_FONT_SIZE_DETAIL, new Integer(13));
        detailMap.put(FONT_METRICS_CORRECTION_DETAIL, new Double(1.05));
        detailMap.put(BLACK_CIRCLE_UNICODE_DETAIL, "\u25CF");
        OS_DETAILS_MAP.put(Platform.OS_MACOSX, detailMap);

        // Linux
        detailMap = new HashMap<>();
        detailMap.put(HEADER_FONT_SIZE_DETAIL, new Integer(13));
        detailMap.put(CONTENT_FONT_SIZE_DETAIL, new Integer(10));
        detailMap.put(FONT_METRICS_CORRECTION_DETAIL, new Double(1.075));
        detailMap.put(BLACK_CIRCLE_UNICODE_DETAIL, "\u2022");
        OS_DETAILS_MAP.put(Platform.OS_LINUX, detailMap);

        // Windows
        detailMap = new HashMap<>();
        detailMap.put(HEADER_FONT_SIZE_DETAIL, new Integer(14));
        detailMap.put(CONTENT_FONT_SIZE_DETAIL, new Integer(10));
        detailMap.put(FONT_METRICS_CORRECTION_DETAIL, new Double(1.1));
        detailMap.put(BLACK_CIRCLE_UNICODE_DETAIL, "\u2022");
        OS_DETAILS_MAP.put(Platform.OS_WIN32, detailMap);
    }

    /**
     * @param detailKey one of the "_DETAIL" static Strings defined in this class
     * @return an {@link Optional} detail for the current OS
     */
    public static Optional<Object> getDetail(final String detailKey) {
        return getDetailForOS(detailKey, Platform.getOS());
    }

    /**
     * @param detailKey one of the "_DETAIL" static Strings defined in this class
     * @param osString this should be a value returned by {@link Platform#getOS()}
     * @return an {@link Optional} detail for the current OS
     */
    public static Optional<Object> getDetailForOS(final String detailKey, final String osString) {
        return Optional.ofNullable(OS_DETAILS_MAP.get(osString)).map(d -> d.get(detailKey));
    }


    private PlatformSpecificUIisms() { }
}
