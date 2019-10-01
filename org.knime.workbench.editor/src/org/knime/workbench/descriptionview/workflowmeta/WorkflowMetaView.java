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
 *   Apr 28, 2019 (loki): created
 */
package org.knime.workbench.descriptionview.workflowmeta;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.knime.core.internal.ReferencedFile;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistor;
import org.knime.core.ui.node.workflow.WorkflowManagerUI;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.core.ui.wrapper.Wrapper;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.descriptionview.workflowmeta.atoms.LinkMetaInfoAtom;
import org.knime.workbench.descriptionview.workflowmeta.atoms.MetaInfoAtom;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.directannotationedit.FlatButton;
import org.knime.workbench.editor2.editparts.WorkflowRootEditPart;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileInfo;
import org.knime.workbench.explorer.filesystem.AbstractExplorerFileStore;
import org.knime.workbench.explorer.filesystem.RemoteExplorerFileInfo;
import org.knime.workbench.explorer.localworkspace.LocalWorkspaceFileInfo;
import org.knime.workbench.explorer.view.ContentObject;
import org.knime.workbench.repository.util.NodeUtil;

/**
 * This is the view reponsible for displaying, and potentially allowing the editing of, the meta-information associated
 * with a workflow; for example:
 *      . description
 *      . tags
 *      . links
 *      . license
 *      . author
 *
 * The genesis for this view is https://knime-com.atlassian.net/browse/AP-11628
 *
 * As part of https://knime-com.atlassian.net/browse/AP-12082 is was decided that the license field would only be shown
 * in cases where the metadata was coming from a KNIME Hub server; i've gated this condition with a static boolean below
 * (search 'AP-12082') so that future generations can turn the license stuff back on when we support it more widely.
 *
 * @author loki der quaeler
 */
public class WorkflowMetaView extends ScrolledComposite implements MetadataModelFacilitator.ModelObserver {
    /** Display font which the author read-only should use. **/
    public static final Font ITALIC_CONTENT_FONT;
    /** Display font which the read-only versions of metadata should use. **/
    public static final Font VALUE_DISPLAY_FONT;
    /** Font which should be used with the n-ary close character. **/
    public static final Font BOLD_CONTENT_FONT;
    /** The read-only text color. **/  // in 4.0.0 was: 128, 128, 128
    public static final Color TEXT_COLOR = new Color(PlatformUI.getWorkbench().getDisplay(), 62, 58, 57);
    /** The fill color for the header bar and other widgets (like tag chiclets.) **/
    public static final Color GENERAL_FILL_COLOR = new Color(PlatformUI.getWorkbench().getDisplay(), 240, 240, 242);

    private static final String NO_TITLE_TEXT = "No title has been set yet.";
    private static final String NO_DESCRIPTION_TEXT = "No description has been set yet.";
    private static final String NO_LINKS_TEXT = "No links have been added yet.";
    private static final String NO_TAGS_TEXT = "No tags have been added yet.";

    private static final String SERVER_WORKFLOW_TEXT =
        "The metadata will be displayed here momentarily if it is able to be fetched. You may also download it "
            + "and view it locally (by double-clicking on it in the KNIME Explorer.)";

    private static final String SERVER_WORKFLOW_FETCH_FAILED_TEXT =
            "The metadata failed to be fetched from the server. If you would like to view its metadata, you may "
                + "download it and then view it locally by double-clicking on the workflow in the KNIME Explorer.";

    private static final String NO_METADATA_TEXT =
        "Metadata cannot be shown nor edited for the type of item you have selected.";

    private static final Font HEADER_FONT;
    private static final Color HEADER_BORDER_COLOR = new Color(PlatformUI.getWorkbench().getDisplay(), 229, 229, 229);
    private static final Color HEADER_TEXT_COLOR = new Color(PlatformUI.getWorkbench().getDisplay(), 87, 87, 87);
    private static final Color SECTION_LABEL_TEXT_COLOR = TEXT_COLOR;

    private static final Image CANCEL_IMAGE =
        ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "/icons/meta-view-cancel.png");

    private static final Image EDIT_IMAGE =
        ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "/icons/meta-view-edit.png");

    private static final Image SAVE_IMAGE =
        ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "/icons/meta-view-save.png");

    private static final NodeLogger LOGGER = NodeLogger.getLogger(WorkflowMetaView.class);

    private static final int MINIMUM_CONTENT_PANE_WIDTH = 300;

    private static final int HEADER_VERTICAL_INSET = 10;
    private static final int HEADER_TEXT_DRAW_Y = HEADER_VERTICAL_INSET + 5;
    private static final int HEADER_MARGIN_RIGHT = 9;
    private static final int LEFT_INDENT_HEADER_SUB_PANES = 9;
    private static final int TOTAL_HEADER_PADDING
            = HEADER_MARGIN_RIGHT + (2 * LEFT_INDENT_HEADER_SUB_PANES) + (new GridLayout()).horizontalSpacing;
    private static final int CONTENT_VERTICAL_INDENT = 30 + (2 * HEADER_VERTICAL_INSET);

    // AP-12082
    private static final boolean SHOW_LICENSE_ONLY_FOR_HUB = true;

    static {
        final Optional<Object> headerFontSize =
            PlatformSpecificUIisms.getDetail(PlatformSpecificUIisms.HEADER_FONT_SIZE_DETAIL);
        final Optional<Object> contentFontSize =
            PlatformSpecificUIisms.getDetail(PlatformSpecificUIisms.CONTENT_FONT_SIZE_DETAIL);
        final int headerSize = headerFontSize.isPresent() ? ((Integer)headerFontSize.get()).intValue() : 16;
        final int contentSize = contentFontSize.isPresent() ? ((Integer)contentFontSize.get()).intValue() : 12;

        @SuppressWarnings("resource")   // stream is closed in loadFontFromInputStream(...)
        final InputStream is = NodeUtil.class.getResourceAsStream("Proboto-Bold.ttf");
        Optional<Font> f = SWTUtilities.loadFontFromInputStream(is, contentSize, SWT.BOLD);

        if (f.isPresent()) {
            BOLD_CONTENT_FONT = f.get();
        } else {
            NodeLogger.getLogger(WorkflowMetaView.class).warn("Could not load bold font.");
            BOLD_CONTENT_FONT = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
        }


        // We do this annoying new declaration of InputStream instances because we otherwise can't apply
        //      the warning suppression to anywhere but at class level
        @SuppressWarnings("resource")   // stream is closed in loadFontFromInputStream(...)
        final InputStream is2 = NodeUtil.class.getResourceAsStream("Proboto-Regular.ttf");
        f = SWTUtilities.loadFontFromInputStream(is2, contentSize, SWT.NORMAL);

        if (f.isPresent()) {
            VALUE_DISPLAY_FONT = f.get();
        } else {
            NodeLogger.getLogger(WorkflowMetaView.class).warn("Could not load regular font.");
            VALUE_DISPLAY_FONT = JFaceResources.getFont(JFaceResources.DIALOG_FONT);
        }


        // We do this annoying new declaration of InputStream instances because we otherwise can't apply
        //      the warning suppression to anywhere but at class level
        @SuppressWarnings("resource")   // stream is closed in loadFontFromInputStream(...)
        final InputStream is3 = NodeUtil.class.getResourceAsStream("Proboto-Italic.ttf");
        f = SWTUtilities.loadFontFromInputStream(is3, contentSize, SWT.ITALIC);

        if (f.isPresent()) {
            ITALIC_CONTENT_FONT = f.get();
        } else {
            NodeLogger.getLogger(WorkflowMetaView.class).warn("Could not load italic font.");
            ITALIC_CONTENT_FONT = JFaceResources.getFontRegistry().getItalic(JFaceResources.DIALOG_FONT);
        }


        final FontData[] baseFD = BOLD_CONTENT_FONT.getFontData();
        final FontData headerFD = new FontData(baseFD[0].getName(), headerSize, baseFD[0].getStyle());
        HEADER_FONT = new Font(PlatformUI.getWorkbench().getDisplay(), headerFD);
    }

    private static Text addLabelTextFieldCouplet(final Composite parent, final String labelText,
        final String placeholderText) {
        final Label l = new Label(parent, SWT.LEFT);
        l.setText(labelText);
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        l.setLayoutData(gd);

        final Text textField = createTextWithPlaceholderText(parent, placeholderText);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        textField.setLayoutData(gd);

        return textField;
    }

    private static Text createTextWithPlaceholderText(final Composite parent, final String text) {
        final Text textField = new Text(parent, SWT.BORDER);
        final GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        textField.setLayoutData(gd);

        textField.addPaintListener((event) -> {
            if (textField.getCharCount() == 0) {
                final GC gc = event.gc;
                final Rectangle size = textField.getClientArea();

                gc.setAdvanced(true);
                gc.setTextAntialias(SWT.ON);
                gc.setFont(ITALIC_CONTENT_FONT);
                gc.setForeground(TEXT_COLOR);
                gc.drawString(text, size.x + 3, size.y + 3, true);
            }
        });

        return textField;
    }


    private final Composite m_contentPane;

    private final Composite m_headerBar;
    private final Label m_headerLabelPlaceholder;
    private String m_currentWorkflowName;
    private final AtomicInteger m_headerDrawX;
    private String m_headerText;
    private FlatButton m_editSaveButton;
    private final Composite m_headerButtonPane;

    private final Composite m_remoteServerNotificationPane;

    private final Composite m_remoteServerFailureNotificationPane;

    private final Composite m_noUsableMetadataNotificationPane;

    private final Composite m_titleSection;
    private final Composite m_titleNoDataPane;
    private final Composite m_titleContentPane;

    private final Composite m_descriptionSection;
    private final Composite m_descriptionNoDataLabelPane;
    private final Composite m_descriptionContentPane;

    private final Composite m_authorSection;
    private final Composite m_authorContentPane;

    private final Composite m_tagsSection;
    private final Composite m_tagsNoDataLabelPane;
    private final Composite m_tagsContentPane;
    private final Composite m_tagsAddContentPane;
    private final Composite m_tagsTagsContentPane;
    private Text m_tagAddTextField;
    private Button m_tagsAddButton;

    private final Composite m_linksSection;
    private final Composite m_linksNoDataLabelPane;
    private final Composite m_linksContentPane;
    private final Composite m_linksAddContentPane;
    private final Composite m_linksLinksContentPane;
    private Text m_linksAddURLTextField;
    private Text m_linksAddTitleTextField;
    private ComboViewer m_linksAddTypeComboViewer;
    private Button m_linksAddButton;

    private final Composite m_licenseSection;
    private final Composite m_licenseContentPane;

    private final Composite m_creationDateSection;
    private final Composite m_creationDateContentPane;

    private File m_metadataFile;
    private MetadataModelFacilitator m_modelFacilitator;

    private final AtomicBoolean m_workflowMetadataNeedBeFetchedFromServer;
    private final AtomicBoolean m_workflowMetadataServerFetchFailed;
    private final AtomicBoolean m_workflowIsATemplate;
    private final AtomicBoolean m_workflowIsAJob;

    private final AtomicBoolean m_shouldDisplayLicenseSection;

    private final AtomicBoolean m_metadataCanBeEdited;
    private final AtomicBoolean m_inEditMode;

    private final AtomicBoolean m_workflowNameHasChanged;

    private final AtomicInteger m_lastRenderedViewportWidth;
    private final AtomicInteger m_lastRenderedViewportOriginX;
    private final AtomicInteger m_lastRenderedViewportOriginY;
    private final FloatingHeaderBarPositioner m_floatingHeaderPositioner;

    /**
     * @param parent
     */
    public WorkflowMetaView(final Composite parent) {
        super(parent, SWT.H_SCROLL | SWT.V_SCROLL);


        m_inEditMode = new AtomicBoolean(false);
        m_metadataCanBeEdited = new AtomicBoolean(false);

        m_workflowMetadataNeedBeFetchedFromServer = new AtomicBoolean(false);
        m_workflowMetadataServerFetchFailed = new AtomicBoolean(false);
        m_workflowIsATemplate = new AtomicBoolean(false);
        m_workflowIsAJob = new AtomicBoolean(false);

        m_shouldDisplayLicenseSection = new AtomicBoolean(!SHOW_LICENSE_ONLY_FOR_HUB);
        m_workflowNameHasChanged = new AtomicBoolean(false);

        m_lastRenderedViewportWidth = new AtomicInteger(Integer.MIN_VALUE);
        m_lastRenderedViewportOriginX = new AtomicInteger(Integer.MIN_VALUE);
        m_lastRenderedViewportOriginY = new AtomicInteger(Integer.MIN_VALUE);

        m_headerDrawX = new AtomicInteger(0);

        setBackgroundMode(SWT.INHERIT_DEFAULT);


        m_contentPane = new Composite(this, SWT.NONE);
        m_contentPane.setBackground(ColorConstants.white);
        setContent(m_contentPane);

        GridLayout gl = new GridLayout(1, false);
        gl.marginHeight = 0;
        gl.marginBottom = 3;
        gl.marginWidth = 3;
        m_contentPane.setLayout(gl);


        m_headerBar = new Composite(m_contentPane, SWT.NONE);
        GridData gd = new GridData();
        gd.exclude = true;
        m_headerBar.setLayoutData(gd);
        gl = new GridLayout(2, false);
        gl.marginHeight = HEADER_VERTICAL_INSET;
        gl.marginWidth = 0;
        gl.marginRight = HEADER_MARGIN_RIGHT;
        m_headerBar.setLayout(gl);
        m_headerBar.addPaintListener((event) -> {
            final GC gc = event.gc;
            final Rectangle size = m_headerBar.getClientArea();

            gc.setAdvanced(true);
            gc.setBackground(GENERAL_FILL_COLOR);
            gc.setForeground(HEADER_BORDER_COLOR);
            // the node description header bar actually renders as only the top left being a rounded rectangle
            //      so we fill and draw something larger, letting the clip sort it out, and the paint the east
            //      and south border lines individually.
            gc.fillRoundRectangle(size.x, size.y, size.width + 15, size.height + 15, 10, 10);
            final int x = size.x + 1;
            final int y = size.y + 1;
            gc.drawRoundRectangle(x, y, size.width + 15, size.height + 15, 10, 10);
            final int x2 = x + size.width - 2;
            final int y2 = y + size.height - 2;
            gc.drawLine(x2, y, x2, y2);
            gc.drawLine(x, y2, x2, y2);

            if (m_headerText != null) {
                gc.setFont(HEADER_FONT);
                gc.setForeground(HEADER_TEXT_COLOR);
                gc.setTextAntialias(SWT.ON);
                gc.drawString(m_headerText, m_headerDrawX.intValue(), HEADER_TEXT_DRAW_Y);
            }
        });

        m_headerLabelPlaceholder = new Label(m_headerBar, SWT.LEFT);
        m_headerLabelPlaceholder.setText("");
        gd = new GridData();
        gd.horizontalIndent = LEFT_INDENT_HEADER_SUB_PANES;
        m_headerLabelPlaceholder.setLayoutData(gd);
        m_headerLabelPlaceholder.setFont(HEADER_FONT);
        m_headerLabelPlaceholder.setVisible(false);

        m_headerButtonPane = new Composite(m_headerBar, SWT.NONE);
        m_headerButtonPane.setBackground(GENERAL_FILL_COLOR);
        gd = new GridData();
        gd.horizontalAlignment = SWT.RIGHT;
        gd.grabExcessHorizontalSpace = true;
        gd.heightHint = 24;
        gd.widthHint = 45;
        gd.horizontalIndent = LEFT_INDENT_HEADER_SUB_PANES;
        m_headerButtonPane.setLayoutData(gd);
        gl = new GridLayout(2, true);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        m_headerButtonPane.setLayout(gl);



        m_remoteServerNotificationPane = new Composite(m_contentPane, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.verticalIndent = CONTENT_VERTICAL_INDENT + 8;
        m_remoteServerNotificationPane.setLayoutData(gd);
        m_remoteServerNotificationPane.setLayout(new GridLayout(1, false));
        Label l = new Label(m_remoteServerNotificationPane, SWT.CENTER | SWT.WRAP);
        l.setText(SERVER_WORKFLOW_TEXT);
        l.setForeground(TEXT_COLOR);
        l.setFont(ITALIC_CONTENT_FONT);
        gd.horizontalAlignment = SWT.LEFT;
        gd.verticalAlignment = SWT.BOTTOM;
        gd.grabExcessHorizontalSpace = true;
        l.setLayoutData(gd);



        m_remoteServerFailureNotificationPane = new Composite(m_contentPane, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.verticalIndent = CONTENT_VERTICAL_INDENT + 8;
        m_remoteServerFailureNotificationPane.setLayoutData(gd);
        m_remoteServerFailureNotificationPane.setLayout(new GridLayout(1, false));
        l = new Label(m_remoteServerFailureNotificationPane, SWT.CENTER | SWT.WRAP);
        l.setText(SERVER_WORKFLOW_FETCH_FAILED_TEXT);
        l.setForeground(TEXT_COLOR);
        l.setFont(ITALIC_CONTENT_FONT);
        gd.horizontalAlignment = SWT.LEFT;
        gd.verticalAlignment = SWT.BOTTOM;
        gd.grabExcessHorizontalSpace = true;
        l.setLayoutData(gd);



        m_noUsableMetadataNotificationPane = new Composite(m_contentPane, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.verticalIndent = CONTENT_VERTICAL_INDENT + 8;
        m_noUsableMetadataNotificationPane.setLayoutData(gd);
        m_noUsableMetadataNotificationPane.setLayout(new GridLayout(1, false));
        l = new Label(m_noUsableMetadataNotificationPane, SWT.CENTER | SWT.WRAP);
        l.setText(NO_METADATA_TEXT);
        l.setForeground(TEXT_COLOR);
        l.setFont(ITALIC_CONTENT_FONT);
        gd.horizontalAlignment = SWT.LEFT;
        gd.verticalAlignment = SWT.BOTTOM;
        gd.grabExcessHorizontalSpace = true;
        l.setLayoutData(gd);



        Composite[] sectionAndContentPane = createHorizontalSection("Title", NO_TITLE_TEXT);
        m_titleSection = sectionAndContentPane[0];
        m_titleNoDataPane = sectionAndContentPane[1];
        m_titleContentPane = sectionAndContentPane[2];
        gd = (GridData)m_titleSection.getLayoutData();
        gd.verticalIndent = CONTENT_VERTICAL_INDENT;
        m_titleSection.setLayoutData(gd);


        sectionAndContentPane = createVerticalSection("Description", NO_DESCRIPTION_TEXT);
        m_descriptionSection = sectionAndContentPane[0];
        m_descriptionNoDataLabelPane = sectionAndContentPane[1];
        m_descriptionContentPane = sectionAndContentPane[2];


        sectionAndContentPane = createVerticalSection("Tags", NO_TAGS_TEXT);
        m_tagsSection = sectionAndContentPane[0];
        m_tagsNoDataLabelPane = sectionAndContentPane[1];
        m_tagsContentPane = sectionAndContentPane[2];
        m_tagsAddContentPane = new Composite(m_tagsContentPane, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.heightHint = 27;
        m_tagsAddContentPane.setLayoutData(gd);
        gl = new GridLayout(2, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        gl.horizontalSpacing = 3;
        m_tagsAddContentPane.setLayout(gl);
        m_tagsTagsContentPane = new Composite(m_tagsContentPane, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        m_tagsTagsContentPane.setLayoutData(gd);
        final RowLayout rl = new RowLayout();
        rl.wrap = true;
        rl.pack = true;
        rl.type = SWT.HORIZONTAL;
        rl.marginWidth = 3;
        rl.marginHeight = 2;
        m_tagsTagsContentPane.setLayout(rl);


        sectionAndContentPane = createVerticalSection("Links", NO_LINKS_TEXT);
        m_linksSection = sectionAndContentPane[0];
        m_linksNoDataLabelPane = sectionAndContentPane[1];
        m_linksContentPane = sectionAndContentPane[2];
        m_linksAddContentPane = new Composite(m_linksContentPane, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        m_linksAddContentPane.setLayoutData(gd);
        gl = new GridLayout(2, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        gl.horizontalSpacing = 3;
        m_linksAddContentPane.setLayout(gl);
        m_linksLinksContentPane = new Composite(m_linksContentPane, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        m_linksLinksContentPane.setLayoutData(gd);
        gl = new GridLayout(1, false);
        gl.marginHeight = 0;
        m_linksLinksContentPane.setLayout(gl);


        sectionAndContentPane = createHorizontalSection("License", null);
        m_licenseSection = sectionAndContentPane[0];
        m_licenseContentPane = sectionAndContentPane[1];
        gd = (GridData)m_licenseContentPane.getLayoutData();
        gd.heightHint = 24;
        m_licenseContentPane.setLayoutData(gd);


        sectionAndContentPane = createHorizontalSection("Creation Date", null);
        m_creationDateSection = sectionAndContentPane[0];
        m_creationDateContentPane = sectionAndContentPane[1];


        sectionAndContentPane = createHorizontalSection("Author", null);
        m_authorSection = sectionAndContentPane[0];
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.TOP;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        m_authorSection.setLayoutData(gd);
        m_authorContentPane = sectionAndContentPane[1];



        configureFloatingHeaderBarButtons();

        SWTUtilities.spaceReclaimingSetVisible(m_remoteServerNotificationPane, false);
        SWTUtilities.spaceReclaimingSetVisible(m_remoteServerFailureNotificationPane, false);
        SWTUtilities.spaceReclaimingSetVisible(m_noUsableMetadataNotificationPane, false);
        SWTUtilities.spaceReclaimingSetVisible(m_titleContentPane, false);
        SWTUtilities.spaceReclaimingSetVisible(m_descriptionContentPane, false);
        SWTUtilities.spaceReclaimingSetVisible(m_tagsContentPane, false);
        SWTUtilities.spaceReclaimingSetVisible(m_linksContentPane, false);
        SWTUtilities.spaceReclaimingSetVisible(m_licenseSection, m_shouldDisplayLicenseSection.get());

        setMinWidth(MINIMUM_CONTENT_PANE_WIDTH);
        setMinHeight(625);
        setExpandHorizontal(true);
        setExpandVertical(true);

        addListener(SWT.Resize, (event) -> {
            updateFloatingHeaderBar();
        });

        ScrollBar sb = getHorizontalBar();
        if (sb != null) {
            sb.addListener(SWT.Selection, (event) -> {
                updateFloatingHeaderBar();
            });
        }
        sb = getVerticalBar();
        if (sb != null) {
            sb.addListener(SWT.Selection, (event) -> {
                updateFloatingHeaderBar();
            });
            sb.addListener(SWT.Show, (event) -> {
                updateFloatingHeaderBar();
            });
            sb.addListener(SWT.Hide, (event) -> {
                updateFloatingHeaderBar();
            });
        }

        pack();

        m_floatingHeaderPositioner = new FloatingHeaderBarPositioner();
    }

    /**
     * @return whether the view is currently in edit mode
     */
    public boolean inEditMode() {
        return m_inEditMode.get();
    }

    /**
     * @return whether the model is dirty - this is meaningless if {@link #inEditMode()} returns false
     */
    public boolean modelIsDirty() {
        return m_modelFacilitator.modelIsDirty();
    }

    /**
     * If the view is currently in edit mode, the mode is ended with either a save or cancel.
     *
     * @param shouldSave if true, then the model state is committed, otherwise restored.
     */
    public void endEditMode(final boolean shouldSave) {
        if (m_inEditMode.getAndSet(false)) {
            if (shouldSave) {
                performSave();
            } else {
                performDiscard();
            }
        }
    }

    /**
     * Should the description view receive a remotely fetched metadata for a server-side object, it will facilitate its
     * display by invoking this method. N.B. {@link #selectionChanged(IStructuredSelection)} has already been called for
     * this item prior to receiving this invocation, and so <code>m_currentWorkflowName</code> has been correctly
     * populated.
     *
     * If the author, description, and creation date are all null, it will be interpretted as a failure to fetch remote
     * metadata.
     *
     * @param author the author, or null
     * @param legacyDescription the legacy-style description, or null
     * @param creationDate the creation date, or null
     * @param shouldShowCCBY40License if true, the CC-BY-4.0 license will be shown in the UI
     */
    public void handleAsynchronousRemoteMetadataPopulation(final String author, final String legacyDescription,
        final Calendar creationDate, final boolean shouldShowCCBY40License) {
        m_modelFacilitator = new MetadataModelFacilitator(author, legacyDescription, creationDate);
        m_modelFacilitator.parsingHasFinishedForWorkflowWithFilename(m_currentWorkflowName);
        m_modelFacilitator.setModelObserver(this);

        if (m_metadataCanBeEdited.get()) {
            m_metadataCanBeEdited.set(false);
            configureFloatingHeaderBarButtons();
        }

        m_metadataFile = null;

        m_workflowMetadataNeedBeFetchedFromServer.set(false);
        m_workflowMetadataServerFetchFailed
            .set((author == null) && (legacyDescription == null) && (creationDate == null));
        m_workflowIsATemplate.set(false);
        m_workflowIsAJob.set(false);
        m_shouldDisplayLicenseSection.set(shouldShowCCBY40License);
        getDisplay().asyncExec(() -> {
            updateDisplay();
        });
    }

    /**
     * @param selection the selection passed along from the ISelectionListener
     */
    public void selectionChanged(final IStructuredSelection selection) {
        final Object o = selection.getFirstElement();
        final boolean knimeExplorerItem = (o instanceof ContentObject);

        final File metadataFile;
        final boolean canEditMetadata;

        m_workflowMetadataNeedBeFetchedFromServer.set(false);
        m_workflowMetadataServerFetchFailed.set(false);
        m_workflowIsATemplate.set(false);
        m_workflowIsAJob.set(false);
        m_shouldDisplayLicenseSection.set(!SHOW_LICENSE_ONLY_FOR_HUB);
        if (knimeExplorerItem) {
            final AbstractExplorerFileStore fs = ((ContentObject) o).getFileStore();
            final AbstractExplorerFileInfo fileInfo = fs.fetchInfo();
            if (!(fileInfo instanceof RemoteExplorerFileInfo) && !(fileInfo instanceof LocalWorkspaceFileInfo)) {
                LOGGER.debug("Received unexpected file info type: " + fileInfo.getClass());
                return;
            }

            final boolean isWorkflow = AbstractExplorerFileStore.isWorkflow(fs);
            final boolean isTemplate = AbstractExplorerFileStore.isWorkflowTemplate(fs);
            final boolean isRemote = fs.getContentProvider().isRemote();
            final boolean isJob = isRemote ? ((RemoteExplorerFileInfo)fileInfo).isWorkflowJob() : false;
            final boolean validFS =
                (isWorkflow || AbstractExplorerFileStore.isWorkflowGroup(fs) || isTemplate || isJob);
            if (!validFS) {
                return;
            }

            m_currentWorkflowName = fs.getName();
            if (isRemote || isTemplate || isJob) {
                m_workflowMetadataNeedBeFetchedFromServer.set(isRemote && !isTemplate && !isJob);
                m_workflowIsATemplate.set(isTemplate);
                m_workflowIsAJob.set(isJob);

                metadataFile = null;
                canEditMetadata = false;
            } else {
                final AbstractExplorerFileStore metaInfo = fs.getChild(WorkflowPersistor.METAINFO_FILE);
                try {
                    metadataFile = metaInfo.toLocalFile(EFS.NONE, null);
                } catch (final CoreException ce) {
                    LOGGER.error("Unable to convert EFS to local file.", ce);

                    return;
                }
                canEditMetadata = true;
            }
        } else {
            final WorkflowRootEditPart wrep = (WorkflowRootEditPart)o;
            final WorkflowManagerUI wmUI = wrep.getWorkflowManager();
            final Optional<WorkflowManager> wm = Wrapper.unwrapWFMOptional(wmUI);
            if (wm.isPresent()) {
                if (wm.get().getDirectNCParent() instanceof SubNodeContainer) {
                    //it's a component project (i.e. component not embedded in a workflow)
                    m_workflowIsAJob.set(false);
                    metadataFile = null;
                    canEditMetadata = false;
                } else {
                    final WorkflowManager projectWM = wm.get().getProjectWFM();
                    final ReferencedFile rf = projectWM.getWorkingDir();

                    metadataFile = new File(rf.getFile(), WorkflowPersistor.METAINFO_FILE);
                    m_currentWorkflowName = projectWM.getName();

                    final WorkflowEditor editor = (WorkflowEditor)PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                        .getActivePage().getActiveEditor();
                    canEditMetadata = !editor.isTempRemoteWorkflowEditor();
                }
            } else {
                m_workflowIsAJob.set(true);
                metadataFile = null;
                canEditMetadata = false;
            }
        }

        if ((m_metadataFile != null) && m_metadataFile.equals(metadataFile)) {
            return;
        }

        m_headerLabelPlaceholder.getDisplay().asyncExec(() -> {
            m_headerText = m_currentWorkflowName;
            m_workflowNameHasChanged.set(true);
            if (!isDisposed()) {
                updateFloatingHeaderBar();
            }
        });


        if ((metadataFile != null) && metadataFile.exists()) {
            final SAXInputHandler handler = new SAXInputHandler();
            try {
                final SAXParserFactory parserFactory = SAXParserFactory.newInstance();
                parserFactory.setNamespaceAware(true);

                final SAXParser parser = parserFactory.newSAXParser();
                parser.parse(metadataFile, handler);
            } catch (Exception e) {
                LOGGER.error("Failed to parse the workflow metadata file.", e);

                return;
            }

            m_modelFacilitator = handler.getModelFacilitator();
        } else {
            m_modelFacilitator = new MetadataModelFacilitator();
        }
        m_modelFacilitator.parsingHasFinishedForWorkflowWithFilename(m_currentWorkflowName);
        m_modelFacilitator.setModelObserver(this);

        if (m_metadataCanBeEdited.get() != canEditMetadata) {
            m_metadataCanBeEdited.set(canEditMetadata);
            configureFloatingHeaderBarButtons();
        }

        m_metadataFile = metadataFile;

        getDisplay().asyncExec(() -> {
            if (!isDisposed()) {
                updateDisplay();
            }
        });
    }

    private void updateDisplay() {
        boolean focusFirstEditElement = false;

        if (m_workflowMetadataNeedBeFetchedFromServer.get() || m_workflowMetadataServerFetchFailed.get()
                    || m_workflowIsATemplate.get() || m_workflowIsAJob.get()) {
            SWTUtilities.spaceReclaimingSetVisible(m_remoteServerNotificationPane,
                m_workflowMetadataNeedBeFetchedFromServer.get());
            SWTUtilities.spaceReclaimingSetVisible(m_remoteServerFailureNotificationPane,
                m_workflowMetadataServerFetchFailed.get());
            SWTUtilities.spaceReclaimingSetVisible(m_noUsableMetadataNotificationPane,
                m_workflowIsATemplate.get() || m_workflowIsAJob.get());

            SWTUtilities.spaceReclaimingSetVisible(m_titleSection, false);
            SWTUtilities.spaceReclaimingSetVisible(m_descriptionSection, false);
            SWTUtilities.spaceReclaimingSetVisible(m_authorSection, false);
            SWTUtilities.spaceReclaimingSetVisible(m_tagsSection, false);
            SWTUtilities.spaceReclaimingSetVisible(m_linksSection, false);
            SWTUtilities.spaceReclaimingSetVisible(m_licenseSection, false);
            SWTUtilities.spaceReclaimingSetVisible(m_creationDateSection, false);
        } else {
            final boolean editMode = m_inEditMode.get();

            focusFirstEditElement = editMode;

            SWTUtilities.spaceReclaimingSetVisible(m_remoteServerNotificationPane, false);
            SWTUtilities.spaceReclaimingSetVisible(m_remoteServerFailureNotificationPane, false);
            SWTUtilities.spaceReclaimingSetVisible(m_noUsableMetadataNotificationPane, false);

            SWTUtilities.spaceReclaimingSetVisible(m_titleSection, true);
            SWTUtilities.spaceReclaimingSetVisible(m_descriptionSection, true);
            SWTUtilities.spaceReclaimingSetVisible(m_authorSection, true);
            SWTUtilities.spaceReclaimingSetVisible(m_tagsSection, true);
            SWTUtilities.spaceReclaimingSetVisible(m_linksSection, true);
            SWTUtilities.spaceReclaimingSetVisible(m_licenseSection, m_shouldDisplayLicenseSection.get());
            SWTUtilities.spaceReclaimingSetVisible(m_creationDateSection, true);

            SWTUtilities.removeAllChildren(m_titleContentPane);
            MetaInfoAtom mia = m_modelFacilitator.getTitle();
            if (editMode || mia.hasContent()) {
                if (editMode) {
                    mia.populateContainerForEdit(m_titleContentPane);
                } else {
                    mia.populateContainerForDisplay(m_titleContentPane);
                }

                SWTUtilities.spaceReclaimingSetVisible(m_titleNoDataPane, false);
                SWTUtilities.spaceReclaimingSetVisible(m_titleContentPane, true);
            } else {
                SWTUtilities.spaceReclaimingSetVisible(m_titleContentPane, false);
                SWTUtilities.spaceReclaimingSetVisible(m_titleNoDataPane, true);
            }

            SWTUtilities.removeAllChildren(m_descriptionContentPane);
            mia = m_modelFacilitator.getDescription();
            if (editMode || mia.hasContent()) {
                if (editMode) {
                    mia.populateContainerForEdit(m_descriptionContentPane);
                } else {
                    mia.populateContainerForDisplay(m_descriptionContentPane);
                }

                SWTUtilities.spaceReclaimingSetVisible(m_descriptionNoDataLabelPane, false);
                SWTUtilities.spaceReclaimingSetVisible(m_descriptionContentPane, true);
            } else {
                SWTUtilities.spaceReclaimingSetVisible(m_descriptionContentPane, false);
                SWTUtilities.spaceReclaimingSetVisible(m_descriptionNoDataLabelPane, true);
            }

            SWTUtilities.removeAllChildren(m_tagsAddContentPane);
            SWTUtilities.removeAllChildren(m_tagsTagsContentPane);
            List<? extends MetaInfoAtom> atoms = m_modelFacilitator.getTags();
            if (editMode || (atoms.size() > 0)) {
                if (editMode) {
                    SWTUtilities.spaceReclaimingSetVisible(m_tagsAddContentPane, true);
                    createTagsAddUI();
                } else {
                    SWTUtilities.spaceReclaimingSetVisible(m_tagsAddContentPane, false);
                }

                atoms.stream().forEach((atom) -> {
                    if (editMode) {
                        atom.populateContainerForEdit(m_tagsTagsContentPane);
                    } else {
                        atom.populateContainerForDisplay(m_tagsTagsContentPane);
                    }
                });

                SWTUtilities.spaceReclaimingSetVisible(m_tagsNoDataLabelPane, false);
                SWTUtilities.spaceReclaimingSetVisible(m_tagsContentPane, true);
            } else {
                SWTUtilities.spaceReclaimingSetVisible(m_tagsContentPane, false);
                SWTUtilities.spaceReclaimingSetVisible(m_tagsNoDataLabelPane, true);
            }

            SWTUtilities.removeAllChildren(m_linksAddContentPane);
            SWTUtilities.removeAllChildren(m_linksLinksContentPane);
            atoms = m_modelFacilitator.getLinks();
            if (editMode || (atoms.size() > 0)) {
                if (editMode) {
                    SWTUtilities.spaceReclaimingSetVisible(m_linksAddContentPane, true);
                    createLinksAddUI();
                } else {
                    SWTUtilities.spaceReclaimingSetVisible(m_linksAddContentPane, false);
                }

                atoms.stream().forEach((atom) -> {
                    if (editMode) {
                        atom.populateContainerForEdit(m_linksLinksContentPane);
                    } else {
                        atom.populateContainerForDisplay(m_linksLinksContentPane);
                    }
                });

                SWTUtilities.spaceReclaimingSetVisible(m_linksNoDataLabelPane, false);
                SWTUtilities.spaceReclaimingSetVisible(m_linksContentPane, true);
            } else {
                SWTUtilities.spaceReclaimingSetVisible(m_linksContentPane, false);
                SWTUtilities.spaceReclaimingSetVisible(m_linksNoDataLabelPane, true);
            }

            SWTUtilities.removeAllChildren(m_licenseContentPane);
            mia = m_modelFacilitator.getLicense();
            // We currently *always* have a license - this if block is 'just in case'
            if (editMode || mia.hasContent()) {
                final GridData gd = (GridData)m_licenseContentPane.getLayoutData();
                if (editMode) {
                    mia.populateContainerForEdit(m_licenseContentPane);
                    gd.verticalIndent = 0;
                } else {
                    mia.populateContainerForDisplay(m_licenseContentPane);
                    gd.verticalIndent = Math.max(0, (gd.heightHint - 22));
                }
                m_licenseContentPane.setLayoutData(gd);
            }

            SWTUtilities.removeAllChildren(m_creationDateContentPane);
            mia = m_modelFacilitator.getCreationDate();
            if (editMode) {
                mia.populateContainerForEdit(m_creationDateContentPane);
            } else {
                mia.populateContainerForDisplay(m_creationDateContentPane);
            }

            SWTUtilities.removeAllChildren(m_authorContentPane);
            mia = m_modelFacilitator.getAuthor();
            if (editMode) {
                mia.populateContainerForEdit(m_authorContentPane);
            } else {
                mia.populateContainerForDisplay(m_authorContentPane);
            }
        }

        layout(true, true);

        updateFloatingHeaderBar();

        updateMinimumSizes();

        if (focusFirstEditElement) {
            m_modelFacilitator.getTitle().focus();
        }
    }

    private void updateMinimumSizes() {
        final Point minSize = m_contentPane.computeSize(m_contentPane.getParent().getSize().x, SWT.DEFAULT);
        final Point linksSize = m_linksLinksContentPane.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        final Point titleSize = m_titleSection.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        setMinWidth(Math.max(Math.max(titleSize.x, linksSize.x), MINIMUM_CONTENT_PANE_WIDTH));
        setMinHeight(minSize.y);
    }

    private void performSave() {
        m_inEditMode.set(false);

        // we must commit prior to updating display, else atoms may not longer have their UI elements
        //      available to query
        m_modelFacilitator.commitEdit();

        performPostEditModeTransitionActions();

        try {
            final Path metadata = Paths.get(m_metadataFile.getAbsolutePath());
            final String metadataXML = m_modelFacilitator.metadataSavedInLegacyFormat();
            final Job job = new WorkspaceJob("Saving workflow metadata...") {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public IStatus runInWorkspace(final IProgressMonitor monitor) throws CoreException {
                    try {
                        Files.write(metadata, metadataXML.getBytes("UTF-8"), StandardOpenOption.CREATE,
                            StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                    } catch (final IOException e) {
                        throw new CoreException(new Status(IStatus.ERROR, KNIMEEditorPlugin.PLUGIN_ID, -1,
                            "Failed to save the metadata to file.", e));
                    }
                    return Status.OK_STATUS;
                }
            };
            job.setRule(ResourcesPlugin.getWorkspace().getRoot());
            job.setUser(true);
            job.schedule();
        } catch (final IOException e) {
            LOGGER.error("Failed to save metadata.", e);
        }
    }

    private void performDiscard() {
        m_inEditMode.set(false);

        // must restore state before updating display to have a display synced to the restored model
        m_modelFacilitator.restoreState();

        performPostEditModeTransitionActions();
    }

    private void performPostEditModeTransitionActions() {
        updateDisplay();

        configureFloatingHeaderBarButtons();

        m_editSaveButton = null;

        m_tagAddTextField = null;
        m_tagsAddButton = null;

        m_linksAddURLTextField = null;
        m_linksAddTitleTextField = null;
        m_linksAddTypeComboViewer = null;
        m_linksAddButton = null;
    }

    private void configureFloatingHeaderBarButtons() {
        SWTUtilities.removeAllChildren(m_headerButtonPane);

        if (m_inEditMode.get()) {
            m_editSaveButton = new FlatButton(m_headerButtonPane, SWT.PUSH, SAVE_IMAGE, new Point(20, 20), true);
            GridData gd = (GridData)m_editSaveButton.getLayoutData();
            gd.verticalIndent += 3;
            m_editSaveButton.setLayoutData(gd);
            m_editSaveButton.addClickListener((source) -> {
                performSave();
            });
            m_editSaveButton.setHighlightAsCircle(true);

            final FlatButton fb = new FlatButton(m_headerButtonPane, SWT.PUSH, CANCEL_IMAGE, new Point(20, 20), true);
            gd = (GridData)fb.getLayoutData();
            gd.verticalIndent += 3;
            fb.setLayoutData(gd);
            fb.addClickListener((source) -> {
                performDiscard();
            });
            fb.setHighlightAsCircle(true);

            updateEditSaveButton();
        } else {
            final Label l = new Label(m_headerButtonPane, SWT.LEFT);
            l.setLayoutData(new GridData(20, 20));

            if (m_metadataCanBeEdited.get()) {
                final FlatButton fb = new FlatButton(m_headerButtonPane, SWT.PUSH, EDIT_IMAGE, new Point(20, 20), true);
                final GridData gd = (GridData)fb.getLayoutData();
                gd.verticalIndent += 3;
                fb.setLayoutData(gd);
                fb.addClickListener((source) -> {
                    m_inEditMode.set(true);

                    m_modelFacilitator.storeStateForEdit();

                    updateDisplay();

                    configureFloatingHeaderBarButtons();
                });
                fb.setHighlightAsCircle(true);
            } else {
                final Label l2 = new Label(m_headerButtonPane, SWT.LEFT);
                l2.setLayoutData(new GridData(20, 20));
            }
        }

        m_headerBar.layout(true, true);
    }

    private void updateEditSaveButton() {
        if (m_inEditMode.get()) {
            m_editSaveButton.setVisible(m_modelFacilitator.modelIsDirty());
            updateFloatingHeaderBar();
        }
    }

    private void updateFloatingHeaderBar() {
        if ((m_currentWorkflowName == null) || (m_currentWorkflowName.trim().length() == 0)) {
            return;
        }

        m_floatingHeaderPositioner.run();

        // the ol' "SWT occasionally hands us stale bounds at the moment of this mouse event inspired action so
        //      let's give it a moment to sort itself out..." - we cue up one that doesn't actually fire until
        //      the user releases a mouse down
        getDisplay().asyncExec(m_floatingHeaderPositioner);
    }

    private Composite[] createHorizontalSection(final String label, final String noDataLabel) {
        final int compositeCount = (noDataLabel != null) ? 3 : 2;
        final Composite[] sectionAndContentPane = new Composite[compositeCount];

        sectionAndContentPane[0] = new Composite(m_contentPane, SWT.NONE);
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        sectionAndContentPane[0].setLayoutData(gd);
        GridLayout gl = new GridLayout(compositeCount, false);
        gl.marginTop = 5;
        gl.marginBottom = 8;
        gl.marginWidth = 0;
        sectionAndContentPane[0].setLayout(gl);

        Label l = new Label(sectionAndContentPane[0], SWT.LEFT);
        l.setText(label);
        l.setFont(BOLD_CONTENT_FONT);
        l.setForeground(SECTION_LABEL_TEXT_COLOR);
        gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        l.setLayoutData(gd);

        int index = 1;
        if (noDataLabel != null) {
            sectionAndContentPane[index] = new Composite(sectionAndContentPane[0], SWT.NONE);
            gd = new GridData();
            gd.horizontalAlignment = SWT.LEFT;
            gd.horizontalIndent = 9;
            sectionAndContentPane[index].setLayoutData(gd);
            gl = new GridLayout(1, false);
            gl.marginHeight = 0;
            gl.marginWidth = 0;
            gl.marginBottom = 2;
            sectionAndContentPane[index].setLayout(gl);

            l = new Label(sectionAndContentPane[index], SWT.LEFT);
            l.setFont(ITALIC_CONTENT_FONT);
            l.setForeground(TEXT_COLOR);
            l.setText(noDataLabel);
            gd = new GridData();
            gd.horizontalAlignment = SWT.LEFT;
            gd.verticalAlignment = SWT.BOTTOM;
            gd.grabExcessHorizontalSpace = true;
            l.setLayoutData(gd);

            gd = (GridData)sectionAndContentPane[index].getLayoutData();
            gd.heightHint = l.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
            // re-set it again in case some platform does a clone on get
            sectionAndContentPane[index].setLayoutData(gd);

            index++;
        }

        sectionAndContentPane[index] = new Composite(sectionAndContentPane[0], SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalIndent = 9;
        sectionAndContentPane[index].setLayoutData(gd);
        gl = new GridLayout(1, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        sectionAndContentPane[index].setLayout(gl);

        return sectionAndContentPane;
    }

    private Composite[] createVerticalSection(final String label, final String noDataLabel) {
        final Composite[] sectionAndContentPane = new Composite[3];

        sectionAndContentPane[0] = new Composite(m_contentPane, SWT.NONE);
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        sectionAndContentPane[0].setLayoutData(gd);
        GridLayout gl = new GridLayout(1, false);
        gl.marginTop = 5;
        gl.marginBottom = 8;
        gl.marginWidth = 0;
        gl.verticalSpacing = 0;
        sectionAndContentPane[0].setLayout(gl);

        Label l = new Label(sectionAndContentPane[0], SWT.LEFT);
        l.setText(label);
        l.setFont(BOLD_CONTENT_FONT);
        l.setForeground(SECTION_LABEL_TEXT_COLOR);
        gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        l.setLayoutData(gd);

        sectionAndContentPane[1] = new Composite(sectionAndContentPane[0], SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        sectionAndContentPane[1].setLayoutData(gd);
        gl = new GridLayout(1, false);
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        sectionAndContentPane[1].setLayout(gl);

        l = new Label(sectionAndContentPane[1], SWT.LEFT);
        l.setText(noDataLabel);
        l.setFont(ITALIC_CONTENT_FONT);
        l.setForeground(TEXT_COLOR);
        gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        l.setLayoutData(gd);

        sectionAndContentPane[2] = new Composite(sectionAndContentPane[0], SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        sectionAndContentPane[2].setLayoutData(gd);
        gl = new GridLayout(1, false);
        gl.marginTop = 5;
        gl.marginBottom = 0;
        sectionAndContentPane[2].setLayout(gl);

        return sectionAndContentPane;
    }

    private void createTagsAddUI() {
        m_tagAddTextField = new Text(m_tagsAddContentPane, SWT.BORDER);
        m_tagAddTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(final KeyEvent ke) {
                m_tagsAddButton.setEnabled(m_tagAddTextField.getText().length() > 0);

                if (ke.character == SWT.CR) {
                    processTagAdd();
                }
            }
        });
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        m_tagAddTextField.setLayoutData(gd);

        m_tagsAddButton = new Button(m_tagsAddContentPane, SWT.PUSH);
        m_tagsAddButton.setText("Add");
        m_tagsAddButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent se) {
                processTagAdd();
            }
        });
        gd = new GridData();
        gd.horizontalAlignment = SWT.RIGHT;
        m_tagsAddButton.setLayoutData(gd);
        m_tagsAddButton.setEnabled(false);

        gd = (GridData)m_tagsAddContentPane.getLayoutData();
        final Point textFieldSize = m_tagAddTextField.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        final Point buttonSize = m_tagsAddButton.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        gd.heightHint = Math.max(textFieldSize.y, buttonSize.y) + 4;
        // re-set it again in case some platform does a clone on get
        m_tagsAddContentPane.setLayoutData(gd);
    }

    private void processTagAdd() {
        final String tagText = m_tagAddTextField.getText().trim();
        if (tagText.length() > 0) {
            final MetaInfoAtom tag = m_modelFacilitator.addTag(tagText);
            tag.populateContainerForEdit(m_tagsTagsContentPane);
        }
        m_tagAddTextField.setText("");
        m_tagsAddButton.setEnabled(false);

        m_tagAddTextField.setFocus();

        layout(true, true);

        updateMinimumSizes();
    }

    private void createLinksAddUI() {
        m_linksAddURLTextField = addLabelTextFieldCouplet(m_linksAddContentPane, "URL:", "e.g. https://www.knime.com");
        m_linksAddURLTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(final KeyEvent ke) {
                m_linksAddButton.setEnabled(m_linksAddURLTextField.getText().length() > 0);

                if ((ke.character == SWT.CR) && m_linksAddButton.isEnabled()) {
                    processLinkAdd();
                }
            }
        });
        m_linksAddTitleTextField = addLabelTextFieldCouplet(m_linksAddContentPane, "Title:", "e.g. Outlier detection");
        m_linksAddTitleTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(final KeyEvent ke) {
                if ((ke.character == SWT.CR) && m_linksAddButton.isEnabled()) {
                    processLinkAdd();
                }
            }
        });
        final Label l = new Label(m_linksAddContentPane, SWT.LEFT);
        l.setText("Type:");
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        l.setLayoutData(gd);
        m_linksAddTypeComboViewer = new ComboViewer(m_linksAddContentPane, SWT.READ_ONLY);
        gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        gd.grabExcessHorizontalSpace = true;
        m_linksAddTypeComboViewer.getCombo().setLayoutData(gd);
        m_linksAddTypeComboViewer.setContentProvider(ArrayContentProvider.getInstance());
        m_linksAddTypeComboViewer.setInput(LinkMetaInfoAtom.LEGACY_LINK_TYPES);
        m_linksAddTypeComboViewer.setLabelProvider(new LabelProvider() {
            /**
             * {@inheritDoc}
             */
            @Override
            @SuppressWarnings("unchecked")  // generic casting...
            public String getText(final Object o) {
                if (o instanceof Pair) {
                    return ((Pair<String, String>)o).getLeft();
                }

                return null;
            }
        });
        m_linksAddTypeComboViewer.getCombo().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(final KeyEvent ke) {
                if ((ke.character == SWT.CR) && m_linksAddButton.isEnabled()) {
                    processLinkAdd();
                }
            }
        });
        m_linksAddTypeComboViewer.getCombo().select(0);

        m_linksAddButton = new Button(m_linksAddContentPane, SWT.PUSH);
        m_linksAddButton.setText("Add");
        m_linksAddButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent se) {
                processLinkAdd();
            }
        });
        gd = new GridData();
        gd.horizontalAlignment = SWT.RIGHT;
        gd.horizontalSpan = 2;
        m_linksAddButton.setLayoutData(gd);
        m_linksAddButton.setEnabled(false);
    }

    @SuppressWarnings("unchecked")  // generics casting...
    private void processLinkAdd() {
        final String url = m_linksAddURLTextField.getText();
        final String title = m_linksAddTitleTextField.getText();
        final StructuredSelection selection = (StructuredSelection)m_linksAddTypeComboViewer.getSelection();
        final Pair<String, String> selectedType = (Pair<String, String>)selection.getFirstElement();
        final String type = selectedType.getLeft();

        try {
            final MetaInfoAtom link = m_modelFacilitator.addLink(url, title, type);
            m_linksAddURLTextField.setText("");
            m_linksAddTitleTextField.setText("");
            m_linksAddButton.setEnabled(false);
            link.populateContainerForEdit(m_linksLinksContentPane);

            m_linksAddURLTextField.setFocus();

            layout(true, true);

            updateMinimumSizes();
        } catch (final MalformedURLException e) {
            MessageDialog.openError(PlatformUI.getWorkbench().getDisplay().getActiveShell(), "Bad URL",
                "The specified URL [" + url + "] appears to be in an invalid format.");
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void modelCardinalityChanged(final boolean increased) {
        if (!increased) {
            layout(true, true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void modelDirtyStateChanged() {
        updateEditSaveButton();
    }


    private class FloatingHeaderBarPositioner implements Runnable {
        private final double m_fontMetricsCorrectionFactor;

        private FloatingHeaderBarPositioner() {
            final Optional<Object> o =
                PlatformSpecificUIisms.getDetail(PlatformSpecificUIisms.FONT_METRICS_CORRECTION_DETAIL);

            m_fontMetricsCorrectionFactor = o.isPresent() ? ((Double)o.get()).doubleValue() : 1.0;
        }

        @Override
        public void run() {
            if (isDisposed()) {
                return;
            }

            final Rectangle viewportBounds = getBounds();
            final ScrollBar verticalSB = getVerticalBar();
            final int sbWidth = (verticalSB.isVisible() ? verticalSB.getSize().x : 0);
            final int viewportWidth = viewportBounds.width - ((2 * getBorderWidth()) + sbWidth);
            final Point origin = getOrigin();

            if ((m_lastRenderedViewportWidth.getAndSet(viewportWidth) == viewportWidth)
                    && (m_lastRenderedViewportOriginX.getAndSet(origin.x) == origin.x)
                    && (m_lastRenderedViewportOriginY.getAndSet(origin.y) == origin.y)
                    && !m_workflowNameHasChanged.getAndSet(false)) {
                return;
            }

            final Point neededButtonPaneSize = m_headerButtonPane.computeSize(SWT.DEFAULT, SWT.DEFAULT);
            final Point labelSize = m_headerLabelPlaceholder.getSize();
            final int barHeight = Math.max(neededButtonPaneSize.y, labelSize.y) + 4 + (2 * HEADER_VERTICAL_INSET);
            m_headerBar.setBounds(origin.x, origin.y, viewportWidth, barHeight);

            final int placeholderWidth = viewportWidth - (TOTAL_HEADER_PADDING + neededButtonPaneSize.x);
            m_headerLabelPlaceholder.setSize(placeholderWidth, labelSize.y);
            final GridData gd = (GridData)m_headerLabelPlaceholder.getLayoutData();
            gd.widthHint = placeholderWidth;
            m_headerLabelPlaceholder.setLayoutData(gd);

            final int centerAlignedAvailableWidth
                = viewportWidth - (2 * (LEFT_INDENT_HEADER_SUB_PANES + HEADER_MARGIN_RIGHT + neededButtonPaneSize.x));
            final GC gc = new GC(m_headerBar.getDisplay());
            try {
                gc.setFont(m_headerLabelPlaceholder.getFont());
                Point fullStringSize = gc.textExtent(m_currentWorkflowName);
                final int stringWidth = (int)(fullStringSize.x * m_fontMetricsCorrectionFactor);

                if (stringWidth > centerAlignedAvailableWidth) {
                    // this could be made more precise by iterative size checks,
                    //      but let's try this first for performance
                    final double percentage = (centerAlignedAvailableWidth * 0.87) / fullStringSize.x;
                    final int charCount = (int)(m_currentWorkflowName.length() * percentage);
                    final String substring = m_currentWorkflowName.substring(0, charCount) + "...";

                    m_headerText = substring;
                    fullStringSize = gc.textExtent(m_headerText);
                } else {
                    m_headerText = m_currentWorkflowName;
                }
                m_headerDrawX.set((viewportWidth - (int)(fullStringSize.x * m_fontMetricsCorrectionFactor)) / 2);
            } finally {
                gc.dispose();
            }

            m_headerBar.layout(true, true);
            m_headerBar.redraw();
        }
    }
}
