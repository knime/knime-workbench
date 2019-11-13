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
 *   Nov 1, 2019 (loki): created
 */
package org.knime.workbench.descriptionview.metadata.component;

import java.net.URL;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.workbench.descriptionview.FallbackBrowser;
import org.knime.workbench.descriptionview.metadata.AbstractMetaView;
import org.knime.workbench.descriptionview.metadata.AbstractMetadataModelFacilitator;
import org.knime.workbench.descriptionview.metadata.LicenseType;
import org.knime.workbench.descriptionview.metadata.atoms.ComboBoxMetaInfoAtom;
import org.knime.workbench.descriptionview.metadata.atoms.DateMetaInfoAtom;
import org.knime.workbench.descriptionview.metadata.atoms.TextAreaMetaInfoAtom;
import org.knime.workbench.descriptionview.metadata.atoms.TextFieldMetaInfoAtom;
import org.knime.workbench.repository.util.DynamicNodeDescriptionCreator;
import org.knime.workbench.repository.util.NodeFactoryHTMLCreator;
import org.knime.workbench.ui.workflow.metadata.MetadataItemType;
import org.w3c.dom.Element;

/**
 * This is the concrete subclass of {@code AbstractMetadataModelFacilitator} which knows how to consume metadata
 * from a {@link SubNodeContainer}.
 *
 * @author loki der quaeler
 */
class ComponentMetadataModelFacilitator extends AbstractMetadataModelFacilitator {
    private static final NodeLogger LOGGER = NodeLogger.getLogger(ComponentMetadataModelFacilitator.class);

    private static final int[] DROP_ZONE_DASH = { 5, 4 };

    private static final String NO_SELECTION_COLOR_COMBO_TEXT = "Select color";


    private final SubNodeContainer m_subNodeContainer;

    /*
     * I've had an architectural change of heart concerning how the UI is formed in this metadata view; originally,
     *  when the metadata were XML elements in a file, it made more sense to me to craft an architecture where
     *  each of those metadata attribute where associated to an 'atom' and it was those atoms which were charged
     *  with populating UI. With this metadata view, we simply have an object (the SubNodeContainer instance) which
     *  has some getters. It feels more artificial to build new types of atoms out of these returns in order to
     *  persist the original architecture. So the atoms will continue to be used for our superclass metadata
     *  functionality (author, title, etc.) but not the component specific items (node color and node icon.)
     *
     * The main problem is that this sort of multi-attribute, but singleton, 'atom' both does not exist in our
     *  atoms, and is also not requiring of all the existing functionality in the MetaInfoAtom parent class.
     */

    // TODO waiting on API to fetch metadata (AP-12986) - see comments below on MetadataMockProvider inner class
    // TODO waiting on API to fetch metadata (AP-12986) - see comments below on MetadataMockProvider inner class
    // TODO waiting on API to fetch metadata (AP-12986) - see comments below on MetadataMockProvider inner class
    // TODO waiting on API to fetch metadata (AP-12986) - see comments below on MetadataMockProvider inner class
    // TODO waiting on API to fetch metadata (AP-12986) - see comments below on MetadataMockProvider inner class
    // TODO waiting on API to fetch metadata (AP-12986) - see comments below on MetadataMockProvider inner class
    // TODO waiting on API to fetch metadata (AP-12986) - see comments below on MetadataMockProvider inner class

    // for development only
    private final MetadataMockProvider m_mockProvider;

    // TODO node color will become an enum which contains either an Image or a pointer to a PNG resource
    private RGB m_nodeColor;
    private RGB m_savedNodeColor;
    private ImageData m_nodeIcon;
    private ImageData m_savedNodeIcon;

    private ImageSwatch m_imageSwatch;
    private Canvas m_iconDropZone;
    private ColorSwatch m_colorSwatch;
    private ComboViewer m_colorComboViewer;

    private NodeDisplayPreview m_nodeDisplayPreview;


    private String[] m_savedInPortNames;
    private String[] m_savedInPortDescriptions;
    private String[] m_savedOutPortNames;
    private String[] m_savedOutPortDescriptions;

    private Text[] m_inportNameTextFields;
    private Text[] m_inportDescriptionTextFields;
    private Text[] m_outportNameTextFields;
    private Text[] m_outportDescriptionTextFields;

    private Browser m_browser;
    private FallbackBrowser m_text;
    private boolean m_isFallback;


    ComponentMetadataModelFacilitator(final SubNodeContainer snc) {
        super();

        m_subNodeContainer = snc;

        m_mockProvider = new MetadataMockProvider(m_subNodeContainer);

        m_descriptionAtom = new TextAreaMetaInfoAtom("legacy-description", m_mockProvider.getDescription(), false);
        m_descriptionAtom.addChangeListener(this);

        // TODO color and icon - AP-12986
        m_nodeColor = null;
        m_nodeIcon = null;


        // These are not presently used in component views, so we populate them with dummy values as their
        //      existence and (not-)dirty state is tracked by our parent class.
        m_titleAtom =
            new TextFieldMetaInfoAtom(MetadataItemType.TITLE, "legacy-title", "unused in components", false);
        m_authorAtom =
            new TextFieldMetaInfoAtom(MetadataItemType.AUTHOR, "legacy-author", "unused in components", false);
        m_creationDateAtom = new DateMetaInfoAtom("legacy-creation-date", Calendar.getInstance(), false);
        m_licenseAtom = new ComboBoxMetaInfoAtom("legacy-license", "unused in components", false);
        // Left here, but commented out, for future versions of component metadata
//        for (final String tag : m_mockProvider.getTags()) {
//            addTag(tag);
//        }
//        for (final String[] link : m_mockProvider.getLinkObjects()) {
//            try {
//                addLink(link[2], link[0], link[1]);
//            } catch (final MalformedURLException e) {
//                m_logger.error("Could not parse incoming URL [" + link[2] + "]", e);
//            }
//        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void storeStateForEdit() {
        super.storeStateForEdit();

        m_savedNodeColor = m_nodeColor;
        m_savedNodeIcon = m_nodeIcon;

        m_savedInPortNames = m_mockProvider.getInPortNames();
        m_savedInPortDescriptions = m_mockProvider.getInPortDescriptions();
        m_savedOutPortNames = m_mockProvider.getOutPortNames();
        m_savedOutPortDescriptions = m_mockProvider.getOutPortDescriptions();


        m_imageSwatch.setImage(m_nodeIcon);
        m_colorSwatch.setColor(m_nodeColor);
        if (m_nodeColor == null) {
            resetColorComboBox();
        } else {
            // TODO select color in combo list
        }


        populateTextArray(m_inportNameTextFields, m_savedInPortNames);
        populateTextArray(m_inportDescriptionTextFields, m_savedInPortDescriptions);
        populateTextArray(m_outportNameTextFields, m_savedOutPortNames);
        populateTextArray(m_outportDescriptionTextFields, m_savedOutPortDescriptions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreState() {
        super.restoreState();

        m_nodeColor = m_savedNodeColor;
        m_nodeIcon = m_savedNodeIcon;

        m_savedNodeColor = null;
        m_savedNodeIcon = null;

        m_savedInPortNames = null;
        m_savedInPortDescriptions = null;
        m_savedOutPortNames = null;
        m_savedOutPortDescriptions = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commitEdit() {
        super.commitEdit();

        m_mockProvider.setDescription(m_descriptionAtom.getValue());
        m_mockProvider.setComponentColor(m_nodeColor);
        m_mockProvider.setComponentIcon(m_nodeIcon);

        m_nodeDisplayPreview.setImage(m_nodeIcon);
        m_nodeDisplayPreview.setColor(m_nodeColor);

        m_savedNodeColor = null;
        m_savedNodeIcon = null;

        m_mockProvider.setInPortNames(getStringArrayFromTextArray(m_inportNameTextFields));
        m_mockProvider.setInPortDescriptions(getStringArrayFromTextArray(m_inportDescriptionTextFields));
        m_mockProvider.setOutPortNames(getStringArrayFromTextArray(m_outportNameTextFields));
        m_mockProvider.setOutPortDescriptions(getStringArrayFromTextArray(m_outportDescriptionTextFields));

        m_savedInPortNames = null;
        m_savedInPortDescriptions = null;
        m_savedOutPortNames = null;
        m_savedOutPortDescriptions = null;

        populatePortDisplay();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean containedMetadataIsDirty() {
        if (!Objects.equals(m_nodeColor, m_savedNodeColor) || !Objects.equals(m_nodeIcon, m_savedNodeIcon)) {
            return true;
        }

        if (!Objects.equals(m_savedInPortNames, getStringArrayFromTextArray(m_inportNameTextFields))) {
            return true;
        }
        if (!Objects.equals(m_savedOutPortNames, getStringArrayFromTextArray(m_outportNameTextFields))) {
            return true;
        }
        if (!Objects.equals(m_savedInPortDescriptions, getStringArrayFromTextArray(m_inportDescriptionTextFields))) {
            return true;
        }

        return !Objects.equals(m_savedOutPortDescriptions, getStringArrayFromTextArray(m_outportDescriptionTextFields));
    }

    void createUIAtomsForEdit(final Composite componentIconParent, final Composite portsParent) {
        Composite c = new Composite(componentIconParent, SWT.NONE);
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        c.setLayoutData(gd);
        GridLayout gl = new GridLayout(2, false);
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        gl.horizontalSpacing = 9;
        c.setLayout(gl);

        m_imageSwatch = new ImageSwatch(c, (e) -> {
            m_imageSwatch.setImage(null);
            setComponentIcon(null);
            triggerReLayout();
        });

        m_iconDropZone = new Canvas(c, SWT.NONE);
        // SWT.BORDER_DASH does nothing... SWT!
        m_iconDropZone.addPaintListener((e) -> {
            final GC gc = e.gc;

            gc.setAntialias(SWT.ON);
            gc.setLineDash(DROP_ZONE_DASH);
            gc.setForeground(AbstractMetaView.SECTION_LABEL_TEXT_COLOR);
            final Point size = m_iconDropZone.getSize();
            gc.drawRectangle(0, 0, (size.x - 1), (size.y - 1));
        });
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.heightHint = 48;
        m_iconDropZone.setLayoutData(gd);
        gl = new GridLayout(1, false);
        gl.marginLeft = 6;
        gl.verticalSpacing = 6;
        m_iconDropZone.setLayout(gl);
        // -----
        Label l = new Label(m_iconDropZone, SWT.NONE);
        l.setText("Drag and Drop a square image file");
        FontData[] baseFD = AbstractMetaView.BOLD_CONTENT_FONT.getFontData();
        FontData smallerFD = new FontData(baseFD[0].getName(), (baseFD[0].getHeight() - 1), baseFD[0].getStyle());
        Font smallerFont = new Font(PlatformUI.getWorkbench().getDisplay(), smallerFD);
        l.setFont(smallerFont);
        l.setForeground(AbstractMetaView.SECTION_LABEL_TEXT_COLOR);
        gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        l.setLayoutData(gd);
        // -----
        l = new Label(m_iconDropZone, SWT.NONE);
        l.setText("Can be a SVG, or a PNG 32x32 or larger");
        baseFD = AbstractMetaView.VALUE_DISPLAY_FONT.getFontData();
        smallerFD = new FontData(baseFD[0].getName(), (baseFD[0].getHeight() - 3), baseFD[0].getStyle());
        smallerFont = new Font(PlatformUI.getWorkbench().getDisplay(), smallerFD);
        l.setFont(smallerFont);
        l.setForeground(AbstractMetaView.TEXT_COLOR);
        gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        l.setLayoutData(gd);

        c = new Composite(componentIconParent, SWT.NONE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        c.setLayoutData(gd);
        gl = new GridLayout(2, false);
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        gl.horizontalSpacing = 9;
        c.setLayout(gl);

        m_colorSwatch = new ColorSwatch(c, (e) -> {
            m_colorSwatch.setColor(null);
            setComponentColor(null);
            resetColorComboBox();
            triggerReLayout();
        });

        m_colorComboViewer = new ComboViewer(c, SWT.READ_ONLY);
        m_colorComboViewer.setContentProvider(ArrayContentProvider.getInstance());
        m_colorComboViewer.setLabelProvider(new LabelProvider() {
            @Override
            public Image getImage(final Object element) {
                // TODO maybe generate a color swatch?
                return null;
            }

            @Override
            public String getText(final Object o) {
                // TODO if we use some sort of object containing swatch color and text
                return (String)o;
            }
        });
        resetColorComboBox();

        gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        gd.grabExcessHorizontalSpace = true;
        gd.verticalAlignment = SWT.CENTER;
        m_colorComboViewer.getCombo().setLayoutData(gd);
        m_colorComboViewer.addPostSelectionChangedListener((event) -> {
            if (!m_colorComboViewer.getStructuredSelection().getFirstElement().equals(NO_SELECTION_COLOR_COMBO_TEXT)) {
                // TODO update swatch color component once we have the object sorted out - for the time being, randomize
                m_colorComboViewer.remove(NO_SELECTION_COLOR_COMBO_TEXT);
                final int r = (int)(Math.random() * 255.0);
                final int g = (int)(Math.random() * 255.0);
                final int b = (int)(Math.random() * 255.0);
                final RGB newColor = new RGB(r, g, b);
                m_colorSwatch.setColor(newColor);
                setComponentColor(newColor);
                triggerReLayout();
            }
        });


        final DropTarget dropTarget = new DropTarget(m_iconDropZone, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_DEFAULT);
        dropTarget.setTransfer(FileTransfer.getInstance());
        dropTarget.addDropListener(new DropTargetListener() {
            @Override
            public void dragEnter(final DropTargetEvent event) {
                event.detail = DND.DROP_COPY;
            }

            @Override
            public void dragLeave(final DropTargetEvent event) { }

            @Override
            public void dragOperationChanged(final DropTargetEvent event) { }

            @Override
            public void dragOver(final DropTargetEvent event) { }

            @Override
            public void drop(final DropTargetEvent event) {
                final String[] files = (String[])event.data;

                if (files.length == 1) {
                    final String lcFilename = files[0].toLowerCase();

                    if (lcFilename.endsWith("png")) {
                        pngFileWasDropped(files[0]);
                    } else if (lcFilename.endsWith("svg")) {
                        svgFileWasDropped(files[0]);
                    }
                }
            }

            @Override
            public void dropAccept(final DropTargetEvent event) { }
        });



        final int inportCount = m_mockProvider.getInPortNames().length;
        m_inportNameTextFields = new Text[inportCount];
        m_inportDescriptionTextFields = new Text[inportCount];
        for (int i = 0; i < inportCount; i++) {
            final Text[] pair = createPortPair(portsParent, (i + 1), true);
            m_inportNameTextFields[i] = pair[0];
            m_inportDescriptionTextFields[i] = pair[1];
        }

        final int outportCount = m_mockProvider.getOutPortNames().length;
        m_outportNameTextFields = new Text[outportCount];
        m_outportDescriptionTextFields = new Text[outportCount];
        for (int i = 0; i < outportCount; i++) {
            final Text[] pair = createPortPair(portsParent, (i + 1), false);
            m_outportNameTextFields[i] = pair[0];
            m_outportDescriptionTextFields[i] = pair[1];
        }
    }

    private static Text[] createPortPair(final Composite parent, final int portNumber, final boolean inport) {
        final Text[] textPair = new Text[2];
        final String labelText = (inport ? "In Port #" : "Out Port #") + portNumber;
        Label l = new Label(parent, SWT.NONE);
        l.setText(labelText);
        l.setFont(AbstractMetaView.BOLD_CONTENT_FONT);
        l.setForeground(AbstractMetaView.SECTION_LABEL_TEXT_COLOR);
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        gd.horizontalIndent = 6;
        l.setLayoutData(gd);

        final Composite c = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(2, false);
        gl.horizontalSpacing = 15;
        gl.marginHeight = 0;
        gl.marginBottom = 6;
        gl.marginWidth = 0;
        gl.marginLeft = 12;
        c.setLayout(gl);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.TOP;
        gd.grabExcessHorizontalSpace = true;
        c.setLayoutData(gd);

        final Composite nameComposite = new Composite(c, SWT.NONE);
        gl = new GridLayout(2, false);
        gl.horizontalSpacing = 6;
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        nameComposite.setLayout(gl);
        gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        gd.verticalAlignment = SWT.TOP;
        nameComposite.setLayoutData(gd);
        l = new Label(nameComposite, SWT.NONE);
        l.setText("Name:");
        l.setForeground(AbstractMetaView.SECTION_LABEL_TEXT_COLOR);
        gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        gd.verticalAlignment = SWT.TOP;
        l.setLayoutData(gd);
        textPair[0] = new Text(nameComposite, SWT.BORDER);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.TOP;
        gd.widthHint = 72;
        textPair[0].setLayoutData(gd);

        final Composite descriptionComposite = new Composite(c, SWT.NONE);
        gl = new GridLayout(2, false);
        gl.horizontalSpacing = 6;
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        descriptionComposite.setLayout(gl);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.TOP;
        gd.grabExcessHorizontalSpace = true;
        descriptionComposite.setLayoutData(gd);
        l = new Label(descriptionComposite, SWT.NONE);
        l.setText("Description:");
        l.setForeground(AbstractMetaView.SECTION_LABEL_TEXT_COLOR);
        gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        gd.verticalAlignment = SWT.TOP;
        l.setLayoutData(gd);
        textPair[1] = new Text(descriptionComposite, (SWT.MULTI | SWT.V_SCROLL | SWT.WRAP | SWT.BORDER));
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.heightHint = 48;
        textPair[1].setLayoutData(gd);

        return textPair;
    }

    void createUIAtomsForDisplay(final Composite componentIconParent, final Composite portsParent) {
        m_nodeDisplayPreview = new NodeDisplayPreview(componentIconParent);

        Composite compositeToLayout;
        try {
            m_text = null;
            m_browser = new Browser(portsParent, SWT.NONE);
            m_browser.setText("");
            m_isFallback = false;
            compositeToLayout = m_browser;
        } catch (final SWTError e) {
            LOGGER.warn("No html browser for node description available.", e);
            m_browser = null;
            m_text = new FallbackBrowser(portsParent, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
            m_isFallback = true;
            compositeToLayout = m_text.getStyledText();
        }
        final GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.verticalAlignment = SWT.FILL;
        gd.heightHint = 296;
        compositeToLayout.setLayoutData(gd);

        populatePortDisplay();
    }

    void setComponentColor(final RGB rgb) {
        m_nodeColor = rgb;

        if (Objects.equals(m_nodeColor, m_savedNodeColor)) {
            metaInfoAtomBecameClean(null);
        } else {
            metaInfoAtomBecameDirty(null);
        }
    }

    void setComponentIcon(final ImageData id) {
        m_nodeIcon = id;

        if (Objects.equals(m_nodeIcon, m_savedNodeIcon)) {
            metaInfoAtomBecameClean(null);
        } else {
            metaInfoAtomBecameDirty(null);
        }
    }

    private void pngFileWasDropped(final String filename) {
        final String urlString = "file:" + filename;

        try {
            final URL url = new URL(urlString);
            final ImageDescriptor imageDescriptor = ImageDescriptor.createFromURL(url);
            final ImageData imageData = imageDescriptor.getImageData(100);

            // We want to hand over the original size image data for storage
            setComponentIcon(imageData);

            m_imageSwatch.setImage(imageData);

            triggerReLayout();
        } catch (final Exception e) {
            LOGGER.error("Unable to create and load from url [" + urlString + "].", e);
        }
    }

    private void svgFileWasDropped(final String filename) {
        try {
            final ImageData id = SVGRasterizer.rasterizeImageFromSVGFile(filename);

            // We want to hand over the 'original' size image data for storage
            setComponentIcon(id);

            m_imageSwatch.setImage(id);

            triggerReLayout();
        } catch (final Exception e) {
            LOGGER.error("Caught exception attempting to read in SVG file.", e);
        }
    }

    private void triggerReLayout() {
        m_imageSwatch.getParent().getParent().getParent().getParent().layout(true, true);
    }

    private void resetColorComboBox() {
        m_colorComboViewer.setInput(Arrays.asList(NO_SELECTION_COLOR_COMBO_TEXT, "Blue", "Green", "Red")/**TODO**/);
        m_colorComboViewer.setSelection(new StructuredSelection(m_colorComboViewer.getElementAt(0)), true);
    }

    void populatePortDisplay() {
        final StringBuilder content = new StringBuilder(DynamicNodeDescriptionCreator.instance().getHeader());
        final Element portDOM = m_subNodeContainer.getXMLDescriptionForPorts();

        try {
            content.append(NodeFactoryHTMLCreator.instance.readFullDescription(portDOM));
            content.append("</body></html>");

            if (m_browser != null) {
                m_browser.getDisplay().asyncExec(() -> {
                    if (!m_browser.isDisposed()) {
                        m_browser.setText(content.toString());
                        m_browser.setVisible(true);
                    }
                });
            } else if (m_isFallback) {
                m_text.getDisplay().asyncExec(() -> {
                    m_text.setText(content.toString());
                    m_text.setVisible(true);
                });
            }
        } catch (final Exception e) {
            LOGGER.error("Exception attempting to generate components port description display.", e);
        }
    }

    private static String[] getStringArrayFromTextArray(final Text[] textArray) {
        final List<String> collected =
            Arrays.stream(textArray).map(text -> text.getText()).collect(Collectors.toList());

        return collected.toArray(new String[collected.size()]);
    }

    private static void populateTextArray(final Text[] textArray, final String[] content) {
        for (int i = 0; i < textArray.length; i++) {
            textArray[i].setText(content[i]);
        }
    }


    /*
     * Just for development... prior to AP-12986, this will return made up data; after AP-12986, during development
     *  this will be a proxy to SubNodeContainer and prior to final PR this class will go away.
     */
    @SuppressWarnings("static-method")
    private static class MetadataMockProvider {
        private final SubNodeContainer m_subNodeContainer;

        private MetadataMockProvider(final SubNodeContainer snc) {
            m_subNodeContainer = snc;
        }

        @SuppressWarnings("unused")
        private String[] getTags() {
            return new String[] {"cool component", "db", "complex", "deep learning"};
        }
        @SuppressWarnings("unused")
        private void setTags(final String[] tags) {
            // TODO
        }

        // unclear what, if any object, SNC will hand over for a "link object".. this will be mocked up to
        //      return a triplet {link text, link type, url}
        @SuppressWarnings("unused")
        private String[][] getLinkObjects() {
            return new String[][] {
              { "KNIME Homepage", "Website", "http://www.knime.org" },
              { "Moe Flanders", "Video", "https://www.youtube.com/watch?v=AWbElkaeqVA" }
            };
        }
        @SuppressWarnings("unused")
        private void setLinkObjects(final String[][] linkObjects) {
            // TODO
        }

        @SuppressWarnings("unused")
        private String getAuthor() {
            return "acamus";
        }
        @SuppressWarnings("unused")
        private void setAuthor(final String author) {
            // TODO
        }

        // this assumes the format will be the long standing metadata precedent for time-dates..
        //      see MetaInfoFile#calendarFromDateString(String)
        @SuppressWarnings("unused")
        private String getDate() {
            return "13/5/2018/10:28:12 +02:00";
        }
        @SuppressWarnings("unused")
        private void setDate(final String date) {
            // TODO
        }

        @SuppressWarnings("unused")
        private String getTitle() {
            return "Tomorrow's Component, Today";
        }
        @SuppressWarnings("unused")
        private void setTitle(final String title) {
            // TODO
        }

        private String getDescription() {
            return "This component does some stuff, and then emits further operations on that stuff.\n\n"
                        + "That being said, at the moment, this is just mocked up metadata.";
        }
        private void setDescription(final String description) {
            // TODO
        }

        @SuppressWarnings("unused")
        private String getLicense() {
            return LicenseType.DEFAULT_LICENSE_NAME;
        }
        @SuppressWarnings("unused")
        private void setLicense(final String license) {
            // TODO
        }

        @SuppressWarnings("unused")
        private RGB getComponentColor() {
            // TODO
            return null;
        }
        @SuppressWarnings("unused")
        private void setComponentColor(final RGB color) {
            // TODO
        }

        @SuppressWarnings("unused")
        private ImageData getComponentIcon() {
            // TODO
            return null;
        }
        @SuppressWarnings("unused")
        private void setComponentIcon(final ImageData icon) {
            // TODO
        }

        private String[] getInPortNames() {
            return m_subNodeContainer.getVirtualInPortNames();
        }
        private void setInPortNames(final String[] names) {
            m_subNodeContainer.setVirtualInPortNames(names);
        }
        private String[] getInPortDescriptions() {
            return m_subNodeContainer.getVirtualInPortDescriptions();
        }
        private void setInPortDescriptions(final String[] descriptions) {
            m_subNodeContainer.setVirtualInPortDescriptions(descriptions);
        }
        private String[] getOutPortNames() {
            return m_subNodeContainer.getVirtualOutPortNames();
        }
        private void setOutPortNames(final String[] names) {
            m_subNodeContainer.setVirtualOutPortNames(names);
        }
        private String[] getOutPortDescriptions() {
            return m_subNodeContainer.getVirtualOutPortDescriptions();
        }
        private void setOutPortDescriptions(final String[] descriptions) {
            m_subNodeContainer.setVirtualOutPortDescriptions(descriptions);
        }
    }
}
