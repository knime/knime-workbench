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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.ComponentMetadata;
import org.knime.core.node.workflow.NodeContainerMetadata.ContentType;
import org.knime.core.node.workflow.NodeContainerMetadata.Link;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.ui.util.SWTUtilities;
import org.knime.workbench.descriptionview.BrowserProvider;
import org.knime.workbench.descriptionview.metadata.AbstractMetaView;
import org.knime.workbench.descriptionview.metadata.AbstractMetadataModelFacilitator;
import org.knime.workbench.descriptionview.metadata.atoms.ComboBoxMetaInfoAtom;
import org.knime.workbench.descriptionview.metadata.atoms.DateMetaInfoAtom;
import org.knime.workbench.descriptionview.metadata.atoms.TextAreaMetaInfoAtom;
import org.knime.workbench.descriptionview.metadata.atoms.TextFieldMetaInfoAtom;
import org.knime.workbench.editor2.AnnotationUtilities;
import org.knime.workbench.editor2.figures.DisplayableNodeType;
import org.knime.workbench.editor2.figures.NodeContainerFigure;
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

    private static final String NO_SELECTION_NODE_TYPE = "Select node type";

    private static final List<Object> NODE_TYPES_TO_DISPLAY;

    static {
        NODE_TYPES_TO_DISPLAY = new ArrayList<>();
        NODE_TYPES_TO_DISPLAY.add(NO_SELECTION_NODE_TYPE);
        NODE_TYPES_TO_DISPLAY.addAll(Arrays.asList(ComponentMetadata.ComponentNodeType.values()));
    }



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

    private ComponentMetadata.ComponentNodeType m_nodeType;
    private ComponentMetadata.ComponentNodeType m_savedNodeType;
    private ImageData m_nodeIcon;
    private ImageData m_savedNodeIcon;

    private ImageSwatch m_imageSwatch;
    private Canvas m_iconDropZone;
    private ImageSwatch m_nodeTypeImageSwatch;
    private ComboViewer m_nodeTypeComboViewer;

    private NodeDisplayPreview m_nodeDisplayPreview;


    private String[] m_savedInPortNames;
    private String[] m_savedInPortDescriptions;
    private String[] m_savedOutPortNames;
    private String[] m_savedOutPortDescriptions;

    private Text[] m_inportNameTextFields;
    private Text[] m_inportDescriptionTextFields;
    private Text[] m_outportNameTextFields;
    private Text[] m_outportDescriptionTextFields;

    private ArrayList<PortAttributeDirtyTracker> m_portDirtyTrackers;

    private BrowserProvider m_browserProvider;

    private Composite m_portsParent;


    ComponentMetadataModelFacilitator(final SubNodeContainer snc) {
        super();

        m_subNodeContainer = snc;
        final var metadata = snc.getMetadata();

        metadata.getDescription().ifPresent(description -> {
            final var desc = metadata.getContentType() == ContentType.PLAIN ? description
                : AnnotationUtilities.stripHtmlFromTextPreservingLineBreaks(description);
            m_descriptionAtom = new TextAreaMetaInfoAtom("legacy-description", desc, false);
            m_descriptionAtom.addChangeListener(this);
        });

        m_nodeType = metadata.getNodeType().orElse(null);

        metadata.getIcon().ifPresent(icon -> m_nodeIcon = new ImageData(new ByteArrayInputStream(icon)));

        // These are not presently used in component views, so we populate them with dummy values as their
        //      existence and (not-)dirty state is tracked by our parent class.
        m_authorAtom = new TextFieldMetaInfoAtom(MetadataItemType.AUTHOR, "legacy-author",
            metadata.getAuthor().orElse(System.getProperty("user.name")), false);
        m_creationDateAtom = new DateMetaInfoAtom("legacy-creation-date",
            metadata.getCreated().orElse(ZonedDateTime.now()));
        m_licenseAtom = new ComboBoxMetaInfoAtom("legacy-license", "unused in components", false);
        for (final String tag : metadata.getTags()) {
            addTag(tag);
        }
        for (final Link link : metadata.getLinks()) {
            try {
                addLink(link.url(), link.text());
            } catch (final MalformedURLException e) {
                m_logger.error("Could not parse incoming URL [" + link.url() + "]", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void storeStateForEdit() {
        super.storeStateForEdit();

        m_savedNodeType = m_nodeType;
        m_savedNodeIcon = m_nodeIcon;

        ComponentMetadata meta = m_subNodeContainer.getMetadata();
        final var richText = mayContainHTML();
        m_savedInPortNames = meta.getInPortNames().orElse(new String[m_subNodeContainer.getNrInPorts() - 1]);
        correctSavedPortValues(m_savedInPortNames, richText);
        m_savedInPortDescriptions =
                meta.getInPortDescriptions().orElse(new String[m_subNodeContainer.getNrInPorts() - 1]);
        correctSavedPortValues(m_savedInPortDescriptions, richText);
        m_savedOutPortNames = meta.getOutPortNames().orElse(new String[m_subNodeContainer.getNrOutPorts() - 1]);
        correctSavedPortValues(m_savedOutPortNames, richText);
        m_savedOutPortDescriptions =
                meta.getOutPortDescriptions().orElse(new String[m_subNodeContainer.getNrOutPorts() - 1]);
        correctSavedPortValues(m_savedOutPortDescriptions, richText);

        m_imageSwatch.setImage(m_nodeIcon);
        m_nodeTypeImageSwatch.setImage(getImageForComponentType(m_nodeType));
        if (m_nodeType == null) {
            resetNodeTypeCombobox();
        } else {
            m_nodeTypeComboViewer.setSelection(new StructuredSelection(m_nodeType), true);
        }

        reinitPortTextFields();

        populateTextArray(m_inportNameTextFields, m_savedInPortNames);
        populateTextArray(m_inportDescriptionTextFields, m_savedInPortDescriptions);
        populateTextArray(m_outportNameTextFields, m_savedOutPortNames);
        populateTextArray(m_outportDescriptionTextFields, m_savedOutPortDescriptions);
    }

    // If we don't do this, the Objects.equals comparisons in containedMetadataIsDirty() will fail for empty
    //      content
    private static void correctSavedPortValues (final String[] array, final boolean strip) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == null) {
                array[i] = "";
            } else if (strip) {
                array[i] = AnnotationUtilities.stripHtmlFromTextPreservingLineBreaks(array[i]);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreState() {
        super.restoreState();

        m_nodeType = m_savedNodeType;
        m_nodeIcon = m_savedNodeIcon;

        m_savedNodeType = null;
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

        m_nodeDisplayPreview.setImage(m_nodeIcon);
        m_nodeDisplayPreview.setNodeTypeBackground(getImageForComponentType(m_nodeType));

        m_savedNodeType = null;
        m_savedNodeIcon = null;

        m_savedInPortNames = null;
        m_savedInPortDescriptions = null;
        m_savedOutPortNames = null;
        m_savedOutPortDescriptions = null;

        storeMetadataInComponent();
        populatePortDisplay();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean containedMetadataIsDirty() {
        if (!Objects.equals(m_nodeType, m_savedNodeType) || !Objects.equals(m_nodeIcon, m_savedNodeIcon)) {
            return true;
        }

        if (!Arrays.equals(m_savedInPortNames, getStringArrayFromTextArray(m_inportNameTextFields))) {
            return true;
        }
        if (!Arrays.equals(m_savedOutPortNames, getStringArrayFromTextArray(m_outportNameTextFields))) {
            return true;
        }
        if (!Arrays.equals(m_savedInPortDescriptions, getStringArrayFromTextArray(m_inportDescriptionTextFields))) {
            return true;
        }

        return !Arrays.equals(m_savedOutPortDescriptions, getStringArrayFromTextArray(m_outportDescriptionTextFields));
    }

    void storeMetadataInComponent() {
        final byte[] icon;
        if (m_nodeIcon != null) {
            if (Math.max(m_nodeIcon.width, m_nodeIcon.height) > 64) {
                //limit size of the image stored
                m_nodeIcon = NodeContainerFigure.scaleImageTo(64, m_nodeIcon);
            }
            final var imageLoader = new ImageLoader();
            imageLoader.data = new ImageData[]{ m_nodeIcon };
            final var stream = new ByteArrayOutputStream();
            imageLoader.save(stream, SWT.IMAGE_PNG);
            icon = stream.toByteArray();
        } else {
            icon = null;
        }

        final var componentMetadataBuilder = ComponentMetadata.fluentBuilder()
                .withComponentType((m_nodeType != null) ? m_nodeType : null)
                .withIcon(icon);

        String[] names = getStringArrayFromTextArray(m_inportNameTextFields);
        String[] descriptions = getStringArrayFromTextArray(m_inportDescriptionTextFields);
        for (var i = 0; i < names.length; i++) {
            componentMetadataBuilder.withInPort(names[i], descriptions[i]);
        }

        names = getStringArrayFromTextArray(m_outportNameTextFields);
        descriptions = getStringArrayFromTextArray(m_outportDescriptionTextFields);
        for (var i = 0; i < names.length; i++) {
            componentMetadataBuilder.withOutPort(names[i], descriptions[i]);
        }

        final var metadataBuilder = componentMetadataBuilder //
                .withPlainContent() //
                .withLastModifiedNow() //
                .withDescription(m_descriptionAtom.getValue()) //
                .withAuthor(m_authorAtom.getValue()) //
                .withCreated(m_creationDateAtom.getDateTime());

        m_tagAtoms.forEach(tag -> metadataBuilder.addTag(tag.getValue()));
        m_linkAtoms.forEach(link -> metadataBuilder.addLink(link.getURL(), link.getValue()));

        m_subNodeContainer.setMetadata(metadataBuilder.build());
    }

    void createUIAtomsForEdit(final Composite componentIconParent, final Composite portsParent) {
        m_portDirtyTrackers = new ArrayList<>();

        m_portsParent = portsParent;
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
            m_imageSwatch.setImage((Image)null);
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
        l.setText("PNG image of size 16x16 or larger");
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

        m_nodeTypeImageSwatch = new ImageSwatch(c, (e) -> {
            m_nodeTypeImageSwatch.setImage((Image)null);
            setNodeType(null);
            resetNodeTypeCombobox();
            triggerReLayout();
        });

        m_nodeTypeComboViewer = new ComboViewer(c, SWT.READ_ONLY);
        m_nodeTypeComboViewer.setContentProvider(ArrayContentProvider.getInstance());
        m_nodeTypeComboViewer.setLabelProvider(new LabelProvider() {
            @Override
            public Image getImage(final Object o) {
                if (o instanceof ComponentMetadata.ComponentNodeType) {
                    return getImageForComponentType((ComponentMetadata.ComponentNodeType)o);
                }

                return null;
            }

            @Override
            public String getText(final Object o) {
                if (o instanceof ComponentMetadata.ComponentNodeType) {
                    return ((ComponentMetadata.ComponentNodeType)o).getDisplayText();
                }

                return (String)o;
            }
        });
        resetNodeTypeCombobox();

        gd = new GridData();
        gd.horizontalAlignment = SWT.LEFT;
        gd.grabExcessHorizontalSpace = true;
        gd.verticalAlignment = SWT.CENTER;
        m_nodeTypeComboViewer.getCombo().setLayoutData(gd);
        m_nodeTypeComboViewer.addPostSelectionChangedListener((event) -> {
            final Object o = m_nodeTypeComboViewer.getStructuredSelection().getFirstElement();
            if (!o.equals(NO_SELECTION_NODE_TYPE)) {
                m_nodeTypeComboViewer.remove(NO_SELECTION_NODE_TYPE);

                final ComponentMetadata.ComponentNodeType ct = (ComponentMetadata.ComponentNodeType)o;
                m_nodeTypeImageSwatch.setImage(getImageForComponentType(ct));
                setNodeType(ct);
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
                    }
                }
            }

            @Override
            public void dropAccept(final DropTargetEvent event) { }
        });



        reinitPortTextFields();
    }

    private void reinitPortTextFields() {
        final int inportCount = m_subNodeContainer.getNrInPorts() - 1; //minus flow variable port
        final int outportCount = m_subNodeContainer.getNrOutPorts() - 1; //minus flow variable port
        if (((m_inportNameTextFields != null) && (m_inportNameTextFields.length != inportCount))
            || ((m_outportNameTextFields != null) && (m_outportNameTextFields.length != outportCount))) {
            SWTUtilities.removeAllChildren(m_portsParent);
            m_portDirtyTrackers.clear();
            m_inportNameTextFields = null;
            m_inportDescriptionTextFields = null;
            m_outportNameTextFields = null;
            m_outportDescriptionTextFields = null;
        }

        boolean trackersNeedReset = false;
        if (m_inportNameTextFields == null) {
            m_inportNameTextFields = new Text[inportCount];
            m_inportDescriptionTextFields = new Text[inportCount];
            for (int i = 0; i < inportCount; i++) {
                final Text[] pair = createPortPair(m_portsParent, (i + 1), true);
                m_inportNameTextFields[i] = pair[0];
                m_inportDescriptionTextFields[i] = pair[1];
            }
        } else {
            trackersNeedReset = true;
        }

        if (m_outportNameTextFields == null) {
            m_outportNameTextFields = new Text[outportCount];
            m_outportDescriptionTextFields = new Text[outportCount];
            for (int i = 0; i < outportCount; i++) {
                final Text[] pair = createPortPair(m_portsParent, (i + 1), false);
                m_outportNameTextFields[i] = pair[0];
                m_outportDescriptionTextFields[i] = pair[1];
            }
        } else {
            trackersNeedReset = true;
        }

        if (trackersNeedReset) {
            m_portDirtyTrackers.stream().forEach(tracker -> tracker.resetTracker());
        }
    }

    private Text[] createPortPair(final Composite parent, final int portNumber, final boolean inport) {
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

        PortAttributeDirtyTracker tracker = new PortAttributeDirtyTracker(textPair[0], inport, true, (portNumber - 1));
        textPair[0].addKeyListener(tracker);
        m_portDirtyTrackers.add(tracker);

        tracker = new PortAttributeDirtyTracker(textPair[1], inport, false, (portNumber - 1));
        textPair[1].addKeyListener(tracker);
        m_portDirtyTrackers.add(tracker);

        return textPair;
    }

    void createUIAtomsForDisplay(final Composite componentIconParent, final Composite portsParent) {
        m_nodeDisplayPreview = new NodeDisplayPreview(componentIconParent);
        m_nodeDisplayPreview.setImage(m_nodeIcon);
        m_nodeDisplayPreview.setNodeTypeBackground(getImageForComponentType(m_nodeType));

        m_browserProvider = new BrowserProvider(portsParent, true);

        populatePortDisplay();
    }

    private void setNodeType(final ComponentMetadata.ComponentNodeType componentType) {
        m_nodeType = componentType;

        if (Objects.equals(m_nodeType, m_savedNodeType)) {
            metaInfoAtomBecameClean(null);
        } else {
            metaInfoAtomBecameDirty(null);
        }
    }

    private void setComponentIcon(final ImageData id) {
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

    private void triggerReLayout() {
        m_imageSwatch.getParent().getParent().getParent().getParent().layout(true, true);
    }

    private void resetNodeTypeCombobox() {
        m_nodeTypeComboViewer.setInput(NODE_TYPES_TO_DISPLAY);
        m_nodeTypeComboViewer.setSelection(new StructuredSelection(m_nodeTypeComboViewer.getElementAt(0)), true);
    }

    void populatePortDisplay() {
        final StringBuilder content = new StringBuilder();
        final Element portDOM = m_subNodeContainer.getXMLDescriptionForPorts();

        try {
            content.append(NodeFactoryHTMLCreator.instance.readFullDescription(portDOM));

            m_browserProvider.updateBrowserContent(content.toString());
        } catch (final Exception e) {
            LOGGER.error("Exception attempting to generate components port description display.", e);
        }
    }

    private static Image getImageForComponentType(final ComponentMetadata.ComponentNodeType ct) {
        if (ct == null) {
            return null;
        }

        NodeFactory.NodeType nt = ct.getType();
        final DisplayableNodeType dnt = DisplayableNodeType.getTypeForNodeType(nt, true);
        if (dnt == null) {
            LOGGER.warn("Could find no node type image for type: " + nt);
            return null;
        }
        return dnt.getImage();
    }

    private static String[] getStringArrayFromTextArray(final Text[] textArray) {
        final List<String> collected =
            Arrays.stream(textArray).map(text -> text.getText()).collect(Collectors.toList());

        return collected.toArray(new String[collected.size()]);
    }

    private static void populateTextArray(final Text[] textArray, final String[] content) {
        for (int i = 0; i < content.length; i++) {
            if (content[i] != null) {
                textArray[i].setText(content[i]);
            }
        }
    }


    private class PortAttributeDirtyTracker extends KeyAdapter {
        private final Text m_textWidget;
        private final boolean m_isInport;
        private final boolean m_isPortName;
        private final int m_saveArrayIndex;
        private final AtomicBoolean m_isDirty;

        // We won't have saved values at the construction time of these instances, so cache at keydown and then
        //      reset potentially in subsequent edits when a different component has not been selected since.
        private String m_originalValue;

        private PortAttributeDirtyTracker(final Text widget, final boolean inport, final boolean isName, final int index) {
            m_textWidget = widget;
            m_isInport = inport;
            m_isPortName = isName;
            m_saveArrayIndex = index;

            m_isDirty = new AtomicBoolean(false);
        }

        private void resetTracker() {
            m_originalValue = null;
            m_isDirty.set(false);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void keyReleased(final KeyEvent ke) {
            boolean dirty = false;

            if (m_originalValue == null) {
                if (m_isPortName) {
                    m_originalValue = m_isInport ? m_savedInPortNames[m_saveArrayIndex]
                                                 : m_savedOutPortNames[m_saveArrayIndex];
                } else {
                    m_originalValue = m_isInport ? m_savedInPortDescriptions[m_saveArrayIndex]
                                                 : m_savedOutPortDescriptions[m_saveArrayIndex];
                }
            }

            if (m_originalValue.length() != m_textWidget.getCharCount()) {
                dirty = true;
            } else if (!m_originalValue.equals(m_textWidget.getText())) {
                dirty = true;
            }

            if (dirty != m_isDirty.getAndSet(dirty)) {
                if (dirty) {
                    metaInfoAtomBecameDirty(null);
                } else {
                    metaInfoAtomBecameClean(null);
                }
            }
        }
    }
}
