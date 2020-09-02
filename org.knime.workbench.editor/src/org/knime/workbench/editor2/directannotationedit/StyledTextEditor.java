/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   2010 10 25 (ohl): created
 */
package org.knime.workbench.editor2.directannotationedit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.core.commands.Command;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.Annotation;
import org.knime.core.node.workflow.AnnotationData;
import org.knime.core.node.workflow.NodeAnnotation;
import org.knime.core.util.ColorUtilities;
import org.knime.workbench.KNIMEEditorPlugin;
import org.knime.workbench.core.util.ImageRepository;
import org.knime.workbench.editor2.WorkflowCanvasClickListener;
import org.knime.workbench.editor2.AnnotationUtilities;
import org.knime.workbench.editor2.WorkflowEditor;
import org.knime.workbench.editor2.editparts.FontStore;

/**
 * @author ohl, KNIME AG, Zurich, Switzerland
 */
public class StyledTextEditor extends CellEditor implements WorkflowCanvasClickListener.AnnotationModeExitListener {
    static final boolean PLATFORM_IS_MAC = Platform.OS_MACOSX.equals(Platform.getOS());
    static final boolean PLATFORM_IS_LINUX = Platform.OS_LINUX.equals(Platform.getOS());
    static final boolean PLATFORM_IS_WINDOWS = Platform.OS_WIN32.equals(Platform.getOS());

    private static final AtomicBoolean DISABLED_MOD1_I_HANDLER = new AtomicBoolean(false);

    private static final NodeLogger LOGGER = NodeLogger.getLogger(StyledTextEditor.class);

    private static final int FONT_COLOR_SELECTION = 0;
    private static final int BORDER_COLOR_SELECTION = 1;
    private static final int BACKGROUND_COLOR_SELECTION = 2;

    /**
     * This value was originally 90ms, and 90ms still works fine when running this launched out of Eclipse, but there
     * was a report (AP-10744) that nightly builds have reverted to previous behaviour. Investigating a nightly build, i
     * saw times in the 200-250ms range; i have no idea what's to account for this greater-than-90ms timings now present
     * in the 3.7.0 nightlies (but not in the 3.6.2 release.)
     *
     * @see #workflowContextMenuShouldBeVetoed()
     */
    // Context menu subsystem
    private static final long INHUMANLY_QUICK_INTER_EVENT_TIME = 300;

    // Perfectly fine that this is shared across all instances as the amount of time it would take to switch workflow
    // editors falls very far outside our "inhumanly quick" boundary.
    // Context menu subsystem
    private static final AtomicLong LAST_STYLE_CONTEXT_MENU_CLOSE = new AtomicLong(-1);

    // Context menu subsystem
    private static final RGB[] DEFAULT_COLORS = new RGB[]{
        ColorUtilities.fromHex("CDE280"), ColorUtilities.fromHex("D8D37B"),
        ColorUtilities.fromHex("93DDD2"), ColorUtilities.fromHex("D0D2B5"),
        ColorUtilities.fromHex("ADDF9E"), ColorUtilities.fromHex("E8AFA7"),
        ColorUtilities.fromHex("C4CBE0"), ColorUtilities.fromHex("E3B67D")};

    // Context menu subsystem
    private static RGB[] LAST_COLORS = null;

    private static StyledTextEditor CURRENT_INSTANCE_IN_VIEW;

    private static final int TAB_SIZE;

    static {
        // set tab size for win and linux and mac differently (it even depends on the zoom level, yuk!)
        if (PLATFORM_IS_MAC) {
            TAB_SIZE = 8;
        } else if (PLATFORM_IS_LINUX) {
            TAB_SIZE = 8;
        } else {
            TAB_SIZE = 16;
        }
    }

    // Context menu subsystem
    static void markStyleDecorationCloseTime() {
        LAST_STYLE_CONTEXT_MENU_CLOSE.set(System.currentTimeMillis());
    }

    /**
     * This is a hack work-around for https://knime-com.atlassian.net/browse/AP-10383 in which we conclude that if we
     * are on a Mac and the request for the display of the context menu is inhumanly fast given the close time of this
     * editor's context menu, that the worklfow editor's context menu should not be shown.
     *
     * @return true if the workflow context menu should not be shown, false if it should be shown
     */
    // Context menu subsystem
    public static boolean workflowContextMenuShouldBeVetoed() {
        if ((PLATFORM_IS_MAC)
              && ((System.currentTimeMillis() - LAST_STYLE_CONTEXT_MENU_CLOSE.get()) < INHUMANLY_QUICK_INTER_EVENT_TIME)) {
            return true;
        }

        return false;
    }

    private static void disableHandlerWithItalicKeybindingConflict () {
        if (! DISABLED_MOD1_I_HANDLER.getAndSet(true)) {
            ICommandService cService = PlatformUI.getWorkbench().getService(ICommandService.class);
            Command command = cService.getCommand("org.eclipse.ui.file.properties");

            command.undefine();

            LOGGER.debug("We have disabled the command for org.eclipse.ui.file.properties");
        }
    }

    /**
     * An avenue to allow the other listerners on the editor to alert us of the case where we should end current
     * focus, for example:
     *  . the drag processor detects another annotation selection
     *  . we're editing a node annotation and the user is attempting to exit by click-away
     *  . the current editor part is deactivating (due to another editor part being opened / switched to)
     */
    public static void relinquishFocusOfCurrentEditorIfInView() {
        if (CURRENT_INSTANCE_IN_VIEW != null) {
            CURRENT_INSTANCE_IN_VIEW.setFocusLossAllowed(true);
            CURRENT_INSTANCE_IN_VIEW.lostFocus();
        }
    }

    /**
     * This allows for notifications of things impacting our location which we can't detect ourselves (such as viewport
     * location changing.)
     */
    public static void toolbarLocationShouldUpdate() {
        if (CURRENT_INSTANCE_IN_VIEW != null) {
            CURRENT_INSTANCE_IN_VIEW.placeAndDisplayToolbar();
        }
    }


    private StyledText m_styledText;

    /**
     * instance used to get layout info in a non-word-wrapping editor (the foreground text editor must be auto-wrapped
     * otherwise the alignment is ignored!)
     */
    private StyledText m_shadowStyledText;

    /**
     * List of menu items that are disabled/enabled with text selection, e.g. copy or font selection.
     */
    private List<MenuItem> m_enableOnSelectedTextMenuItems;

    /**
     * Whether the text shall be selected when the editor is activated. It's true if the annotation contains the default
     * text ("Double-Click to edit" or "Node x").
     */
    private boolean m_selectAllUponFocusGain;

    private Composite m_panel;

    private Color m_backgroundColor = null;

    // now, especially, that we have the style toolbar in a different SWT shell, we need to be very strict about
    //      not giving up focus (which ends the edit.)
    private final AtomicBoolean m_allowFocusLoss = new AtomicBoolean(false);

    private final AtomicBoolean m_annotationIsNodeAnnotation = new AtomicBoolean(false);

    // These six are for the context menu
    private MenuItem m_borderMenuItem;
    private MenuItem m_postBorderMenuItemSeparator;
    private MenuItem m_rightAlignMenuItem;
    private MenuItem m_centerAlignMenuItem;
    private MenuItem m_leftAlignMenuItem;
    private MenuItem[] m_alignmentMenuItems;

    private FloatingStyleToolbar m_toolbarWindow;
    private AnnotationEditFloatingToolbar m_toolbar;

    private ColorDropDown m_colorDropDown;

    private Point m_mouseDownLocation;

    private int m_currentColorSelectionTarget;

    /*
     * If styles are being applied to a caret placement (no existing text selection,) then we stick them here and apply
     * them on next text entry.
     */
    private StyleRange m_pendingStyle;
    private List<StyleRange> m_currentSelectionStyles;
    private SelectionEvent m_activeSelection;
    private volatile boolean m_caretEventWillBeDueToSelectionReplacingKeyPress;

    private int m_lastCaretMovementOffset;


    /**
     * Creates a workflow annotation editor (with the font set to workflow annotations default font - see
     * #setDefaultFont(Font)).
     */
    public StyledTextEditor() {
        super();

        disableHandlerWithItalicKeybindingConflict();
    }

    /**
     * This is the only constructor used, probably; (it is invoked via Constructor.newInstance in
     * DirectEditManager.createCellEditorOn - the GEF superclass of our AnnotationEditManager.)
     *
     * @param parent
     */
    public StyledTextEditor(final Composite parent) {
        super(parent);

        disableHandlerWithItalicKeybindingConflict();
    }

    /**
     * @param parent
     * @param style
     */
    public StyledTextEditor(final Composite parent, final int style) {
        super(parent, style);

        disableHandlerWithItalicKeybindingConflict();
    }

    /**
     * @param p the location of the mouse click which started the edit using this editor
     */
    public void setMouseDownLocation(final Point p) {
        m_mouseDownLocation = p;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Control createControl(final Composite parent) {
        m_panel = new Composite(parent, SWT.NONE);

        final StackLayout layout = new StackLayout();
        m_panel.setLayout(layout);

        layout.topControl = createStyledText(m_panel);
        createShadowText(m_panel);
        applyBackgroundColor();

        m_toolbarWindow = new FloatingStyleToolbar(this);
        m_toolbar = m_toolbarWindow.getToolbar();

        m_colorDropDown =
            new ColorDropDown(m_toolbarWindow.getShell(), this, false, m_toolbarWindow.vendShellListener());

        m_toolbarWindow.registerListenersWithColorDropDown(m_colorDropDown);
        m_toolbar.getEditAssetGroup().addAssetProvider(m_colorDropDown);
        m_panel.addListener(SWT.Show, (e) -> {
            placeAndDisplayToolbar();

            // we add this listener as part of showing, as there are crazy shenanigans in jface's CellEditor
            //      which actually produce a Hide event before a Show event.
            m_panel.addListener(SWT.Hide, (event) -> {
                if (!m_colorDropDown.customColorChooserWasDisplayedInTheLastTimeWindow(400)) {
                    m_colorDropDown.setVisible(false);
                    hideToolbar();
                }
            });
        });
        m_styledText.addPaintListener((e) -> {
            final int width = getCurrentBorderWidth();
            if (width > 0) {
                final Color c = getCurrentBorderColor();
                if (c != null) {
                    final GC gc = e.gc;
                    gc.setAntialias(SWT.ON);
                    gc.setForeground(c);

                    final Point size = m_panel.getSize();
                    gc.setLineWidth(width * 2);
                    gc.drawRectangle(0, 0, size.x, size.y);
                }
            }
        });

        final WorkflowEditor we =
                (WorkflowEditor)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        we.getCanvasClickListener().addExitListener(this);

        CURRENT_INSTANCE_IN_VIEW = this;

        return m_panel;
    }

    void pruneMenuAndToolbarForNodeAnnotation() {
        if (!m_postBorderMenuItemSeparator.isDisposed()) {
            m_postBorderMenuItemSeparator.dispose();
            m_borderMenuItem.dispose();
        }

        m_toolbarWindow.pruneToolbarForNodeAnnotation();
    }

    void placeAndDisplayToolbar() {
        if (m_panel.isDisposed() || m_toolbarWindow.isDisposed()) {
            return;
        }

        m_toolbarWindow.setLocationFromAnnotationDisplayLocation(m_panel.toDisplay(0, 0));
        m_toolbarWindow.open();
    }

    private void hideToolbar() {
        if (!m_toolbarWindow.isDisposed()) {
            m_toolbarWindow.close();
        }
    }

    private void createShadowText(final Composite parent) {
        m_shadowStyledText = new StyledText(parent, SWT.MULTI | SWT.FULL_SELECTION);
        m_shadowStyledText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        syncShadowWithEditor();
    }

    private void syncShadowWithEditor() {
        m_shadowStyledText.setFont(m_styledText.getFont());
        m_shadowStyledText.setVisible(false);
        m_shadowStyledText.setBounds(m_styledText.getBounds());
        m_shadowStyledText.setText(m_styledText.getText());
        m_shadowStyledText.setStyleRanges(m_styledText.getStyleRanges());
        m_shadowStyledText.setAlignment(m_styledText.getAlignment());
        m_shadowStyledText.setBackground(m_styledText.getBackground());
        final int m = m_styledText.getRightMargin();
        m_shadowStyledText.setMargins(m, m, m, m);
        m_shadowStyledText.setMarginColor(m_styledText.getMarginColor());
    }

    private Control createStyledText(final Composite parent) {
        m_styledText = new StyledText(parent, SWT.MULTI | SWT.WRAP | SWT.FULL_SELECTION);
        // by default we are a workflow annotation editor
        // can be changed by changing the default font (setDefaultFont(Font))
        m_styledText.setFont(AnnotationUtilities.getWorkflowAnnotationDefaultFont());
        m_styledText.setAlignment(SWT.LEFT);
        m_styledText.setText("");
        m_styledText.setTabs(TAB_SIZE);
        m_styledText.addVerifyKeyListener((event) -> {
            if ((event.stateMask & SWT.MOD1) != 0) {
                if (event.character == SWT.CR) {
                    event.doit = false;
                } else if ((PLATFORM_IS_LINUX || PLATFORM_IS_WINDOWS) && (event.keyCode == 105)) {
                    // Luckily the moronic SWT design lets us veto the key event which would insert a tab (aliased to ctrl-i)
                    //      but that event, emitted as a ListenerEvent, is still picked up by our <code>SWT.KeyDown</code>
                    //      listener.
                    event.doit = false;
                }
            }
        });
        // forward some events to the cell editor
        m_styledText.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(final KeyEvent ke) {
                if ((ke.stateMask & SWT.MOD1) != 0) {
                    if (ke.keyCode == 'b') {
                        setSWTStyle(SWT.BOLD);
                        m_toolbar.updateToolbarToReflectState();
                    } else if ((ke.keyCode == 'i')
                                || ((PLATFORM_IS_LINUX || PLATFORM_IS_WINDOWS) && (ke.keyCode == 105))) {
                        setSWTStyle(SWT.ITALIC);
                        m_toolbar.updateToolbarToReflectState();
                    }
                }
            }
            @Override
            public void keyReleased(final KeyEvent ke) {
                keyReleaseOccured(ke);
            }
        });
        m_styledText.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(final FocusEvent fe) {
                if (m_toolbar != null) {
                    m_toolbar.updateToolbarToReflectState();
                    m_toolbar.ensureEditAssetsAreNotVisible();
                }
                if (m_colorDropDown.isVisible()) {
                    m_colorDropDown.setVisible(false);
                }
            }
            @Override
            public void focusLost(final FocusEvent fe) {
                final boolean cddActive = m_colorDropDown.customColorChooserWasDisplayedInTheLastTimeWindow(400);

                if (cddActive) {
                    if (m_styledText.isDisposed()) {
                        // the ship has already sailed - we've lost focus out of our control
                        hideToolbar();
                    }
                } else if (m_allowFocusLoss.get()) {
                    lostFocus();
                }
            }
        });
        m_styledText.addCaretListener((event) -> {
            final int newOffset = m_styledText.getCaretOffset();
            final int delta = newOffset - m_lastCaretMovementOffset;

            m_lastCaretMovementOffset = newOffset;

            // If the user places the style elsewhere, the current pending style should go away
            if (delta != 1) {
                m_pendingStyle = null;
            }
            // if we're replacing text, the nuking of the current selection styles will take place in the
            //      textInserted(int,int) method which gets notification after the caret listener notification
            if (!m_caretEventWillBeDueToSelectionReplacingKeyPress) {
                m_currentSelectionStyles = null;
            }

            m_activeSelection = null;

            if (m_toolbar != null) {
                m_toolbar.updateToolbarToReflectState();
            }
        });
        m_styledText.addVerifyListener((event) -> {
            // we get this notification prior to the caret listener implementation
            if ((m_activeSelection != null)
                    && (m_activeSelection.x == event.start)
                    && (m_activeSelection.y == event.end)) {
                m_caretEventWillBeDueToSelectionReplacingKeyPress = true;
            } else {
                m_caretEventWillBeDueToSelectionReplacingKeyPress = false;
            }
        });
        m_styledText.addModifyListener((event) -> {
            // super marks it dirty (otherwise no commit at the end)
            fireEditorValueChanged(true, true);
        });
        m_styledText.addExtendedModifyListener((event) -> {
            if (event.length > 0) {
                textInserted(event.start, event.length);
                if (m_toolbar != null) {
                    m_toolbar.updateToolbarToReflectState();
                }
            }
        });
        m_styledText.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(final SelectionEvent se) {
                selectionChanged(se);
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent se) {
                selectionChanged(se);
            }
        });
        m_styledText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        // Context menu subsystem
        addMenu(m_styledText);

        // enable the style buttons as appropriate
        // Context menu subsystem
        selectionChanged(null);

        return m_styledText;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        super.dispose();

        m_shadowStyledText.dispose();
        m_toolbarWindow.dispose();
        m_colorDropDown.dispose();

        final WorkflowEditor we =
                (WorkflowEditor)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        we.getCanvasClickListener().removeExitListener(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void annotationModeWillExit(final WorkflowCanvasClickListener enabler) {
        if (m_toolbar.isDisposed()) {
            return;
        }

        setFocusLossAllowed(true);

        m_toolbar.ensureEditAssetsAreNotVisible();

        if (m_colorDropDown.isVisible()) {
            m_colorDropDown.setVisible(false);
        }

        lostFocus();

        // we can't remove ourselves from the listeners list in the enabler in this thread because this thread has
        //      the synchronization lock on the collection structure (and so the the removal from the collection
        //      could happen midst iteration involved in messaging the listeners - resulting in a
        //      <code>ConcurrentModificationException</code> being thrown.
        (new Thread(() -> {
            enabler.removeExitListener(this);
        })).start();
    }

    /**
     * Changes the font of unformatted text ranges.
     *
     * @param newDefaultFont The font to use, not null
     */
    public void setDefaultFont(final Font newDefaultFont) {
        if (newDefaultFont != null && !newDefaultFont.equals(m_styledText.getFont())) {
            m_styledText.setFont(newDefaultFont);
        }
    }

    /**
     * @return the current alignment (SWT.LEFT, .CENTER, .RIGHT) of the text
     */
    int getCurrentAlignment() {
        if (m_styledText.isDisposed()) {
            return SWT.LEFT;
        }

        return m_styledText.getAlignment();
    }

    /**
     * @return the current color of the background
     */
    Color getCurrentBackgroundColor() {
        return m_backgroundColor;
    }

    /**
     * @return the current color of the border
     */
    Color getCurrentBorderColor() {
        if (m_styledText.isDisposed()) {
            return null;
        }

        return m_styledText.getMarginColor();
    }

    /**
     * @return the current width of the border
     */
    int getCurrentBorderWidth() {
        if (m_styledText.isDisposed()) {
            return -1;
        }

        return m_styledText.getRightMargin();
    }

    /**
     * @return the current font color of the selected text (if more than one range is in the selection, and they have
     *         differing colors, null will be returned) or of the style at the current cursor position if there is no
     *         selection.
     */
    Color getCurrentFontColor() {
        if (m_pendingStyle != null) {
            return m_pendingStyle.foreground;
        }

        final List<StyleRange> selection = getStylesInSelection();

        if (selection.size() == 0) {
            final StyleRange styleRange = getStyleRangeAtCaret();

            return (styleRange != null)
                        ? (styleRange.foreground != null) ? styleRange.foreground
                                                          : AnnotationUtilities.getAnnotationDefaultForegroundColor()
                        : m_styledText.isDisposed() ? AnnotationUtilities.getAnnotationDefaultForegroundColor()
                                                    : m_styledText.getForeground();
        } else {
            Color color = null;

            for (final StyleRange styleRange : selection) {
                if (color == null) {
                    color = styleRange.foreground;

                    if (color == null) {
                        color = AnnotationUtilities.getAnnotationDefaultForegroundColor();
                    }
                } else {
                    final Color c = styleRange.foreground;

                    if ((!color.equals(c))
                        && ((c != null) || (!color.equals(AnnotationUtilities.getAnnotationDefaultForegroundColor())))) {
                        return null;
                    }
                }
            }

            return color;
        }
    }

    /**
     * @return the current font size of the selected text (if more than one range is in the selection, and they have
     *         differing font sizes, -1 will be returned) or of the style at the current cursor position if there is no
     *         selection.
     */
    int getCurrentFontSize() {
        if (m_pendingStyle != null) {
            return m_pendingStyle.font.getFontData()[0].getHeight();
        }

        final List<StyleRange> selection = getStylesInSelection();
        if (selection.size() == 0) {
            final StyleRange styleRange = getStyleRangeAtCaret();
            final Font f = (styleRange != null) ? styleRange.font : m_styledText.getFont();

            return f.getFontData()[0].getHeight();
        } else {
            int size = Integer.MIN_VALUE;

            for (final StyleRange styleRange : selection) {
                if (size == Integer.MIN_VALUE) {
                    size = styleRange.font.getFontData()[0].getHeight();
                } else {
                    final int fontSize = styleRange.font.getFontData()[0].getHeight();

                    if (fontSize != size) {
                        return -1;
                    }
                }
            }

            return size;
        }
    }

    /**
     * @return the current font style of the selected text (if more than one range is in the selection, and they have
     *         differing font styles, this will be a union of all styles) or of the style at the current cursor
     *         position if there is no selection.
     */
    int getCurrentFontStyle() {
        if (m_pendingStyle != null) {
            return m_pendingStyle.font.getFontData()[0].getStyle();
        }

        final List<StyleRange> selection = getStylesInSelection();
        if (selection.size() == 0) {
            final StyleRange styleRange = getStyleRangeAtCaret();
            final Font f = (styleRange != null) ? styleRange.font : m_styledText.getFont();

            return f.getFontData()[0].getStyle();
        } else {
            int style = 0;

            for (final StyleRange styleRange : selection) {
                style |= styleRange.font.getFontData()[0].getStyle();
            }

            return style;
        }
    }

    private void setFocusLossAllowed(final boolean flag) {
        if (m_allowFocusLoss != null) {
            m_allowFocusLoss.set(flag);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void fireEditorValueChanged(final boolean oldValidState, final boolean newValidState) {
        syncShadowWithEditor();
        super.fireEditorValueChanged(oldValidState, newValidState);
    }

    private void updateCachedStyleSelectionIfNecessary() {
        if (m_styledText.getSelectionCount() > 1) {
            m_currentSelectionStyles = getStylesInSelection();
        }
    }

    // TODO in the future consider combining this and getNeighboringStyle(int, int)
    private StyleRange getStyleRangeAtCaret() {
        if (m_styledText.isDisposed()) {
            return null;
        }

        int offset = m_styledText.getCaretOffset();
        final int textLength = m_styledText.getCharCount();

        if (offset > textLength) {
            return null;
        } else if (offset > 0) {
            offset--;
        } else if (offset == textLength) {
            return null;
        }

        return m_styledText.getStyleRangeAtOffset(offset);
    }

    /**
     * Sets the style range for the new text. Copies it from the left neighbor (or from the right neighbor, if there is
     * no left neighbor).
     *
     * @param start
     * @param length
     */
    private void textInserted(final int start, final int length) {
        StyleRange styleToAffect = null;
        if (m_pendingStyle != null) {
            styleToAffect = createNewStyleRange(start, 0); // 0 length as length is addressed below

            if (styleToAffect.font != null) {
                final FontStore fs = FontStore.INSTANCE;
                if (fs.fontContainsStyle(m_pendingStyle.font, SWT.BOLD)) {
                    if (!fs.fontContainsStyle(styleToAffect.font, SWT.BOLD)) {
                        styleToAffect.font = fs.addStyleToFont(styleToAffect.font, SWT.BOLD);
                    }
                }
                if (fs.fontContainsStyle(m_pendingStyle.font, SWT.ITALIC)) {
                    if (!fs.fontContainsStyle(styleToAffect.font, SWT.ITALIC)) {
                        styleToAffect.font = fs.addStyleToFont(styleToAffect.font, SWT.ITALIC);
                    }
                }
                final FontData fd = styleToAffect.font.getFontData()[0];
                if (fd.getHeight() != m_pendingStyle.font.getFontData()[0].getHeight()) {
                    styleToAffect.font = fs.getFont(fd.getName(), m_pendingStyle.font.getFontData()[0].getHeight(),
                        fd.getStyle());
                }
            } else {
                styleToAffect.font = m_pendingStyle.font;
            }

            if (m_pendingStyle.foreground != null) {
                styleToAffect.foreground = m_pendingStyle.foreground;
            }

            m_pendingStyle = null;
        } else if (m_currentSelectionStyles != null) {
            // The case in which a selection has been replaced but we want to preserve its style with the new text
            if (m_currentSelectionStyles.size() > 0) {
                styleToAffect = m_currentSelectionStyles.get(0);
            }

            m_currentSelectionStyles = null;

            if (styleToAffect == null) {
                return;
            }

            styleToAffect.start = start;
            styleToAffect.length = 0;       // length is addressed below
        } else if (m_styledText.getCharCount() <= length) {
            // no left nor right neighbor
            return;
        } else {
            final StyleRange[] currentStyles = m_styledText.getStyleRanges(start, length);
            if ((currentStyles != null) && (currentStyles.length > 0) && (currentStyles[0] != null)) {
                // inserted text already has a style (shouldn't really happen)
                return;
            }

            styleToAffect = getNeighboringStyle(start, length);
        }

        if (styleToAffect == null) {
            // should not be possible
            return;
        }

        if (start == 0) {
            styleToAffect.start = 0;
        }
        styleToAffect.length += length;

        m_styledText.setStyleRange(styleToAffect);
    }

    /**
     * Gets style from the left neighbor (or from the right neighbor, if there is no left neighbor).
     *
     * TODO this should handle L10N RTL-isms
     *
     * @param start
     * @param length
     */
    private StyleRange getNeighboringStyle(final int start, final int length) {
        final StyleRange[] neighborStyles;
        if (start == 0) {
            if (length >= m_styledText.getCharCount()) {
                return null;
            }

            neighborStyles = m_styledText.getStyleRanges(length, 1);
        } else {
            neighborStyles = m_styledText.getStyleRanges(start - 1, 1);
        }

        if ((neighborStyles == null) || (neighborStyles.length != 1) || (neighborStyles[0] == null)) {
            // no style to extend over inserted text
            return null;
        }

        return neighborStyles[0];
    }

    private void selectionChanged(final SelectionEvent se) {
        if (m_toolbar != null) {
            m_toolbar.updateToolbarToReflectState();
        }

        m_pendingStyle = null;

        fireEnablementChanged(COPY);
        fireEnablementChanged(CUT);

        // Context menu subsystem
        boolean enableStyleButtons = true;
        final int[] selections = m_styledText.getSelectionRanges();
        if ((selections == null) || (selections.length != 2)) {
            enableStyleButtons = false;
        } else {
            final int length = selections[1];
            enableStyleButtons = (length > 0);
        }
        enableStyleButtons(enableStyleButtons);

        if ((se != null) && ((se.y - se.x) > 0)) {  // why they don't instead populate se.width? SWT!
            m_currentSelectionStyles = getStylesInSelection();
            m_activeSelection = se;
        } else {
            m_activeSelection = null;
        }
    }

    private void applyBackgroundColor() {
        if (m_backgroundColor != null && m_panel != null) {
            LinkedList<Composite> comps = new LinkedList<Composite>();
            comps.add(m_panel);
            while (!comps.isEmpty()) {
                // set the composite's bg
                Composite c = comps.pollFirst();
                c.setBackgroundMode(SWT.INHERIT_NONE);
                c.setBackground(m_backgroundColor);
                // and the bg all of its children
                Control[] children = c.getChildren();
                for (Control child : children) {
                    if (child instanceof Composite) {
                        comps.add((Composite)child);
                    } else {
                        child.setBackground(m_backgroundColor);
                    }
                }
            }
        }
    }

    private void setBackgroundColor(final Color color) {
        m_backgroundColor = color;
        applyBackgroundColor();
    }

    /**
     *
     */
    protected void lostFocus() {
        CURRENT_INSTANCE_IN_VIEW = null;

        super.focusLost();
    }

    boolean currentAnnotationIsNodeAnnotation() {
        return m_annotationIsNodeAnnotation.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void keyReleaseOccured(final KeyEvent keyEvent) {
        if (keyEvent.character == SWT.CR) { // Return key
            // don't let super close the editor on CR
            if ((keyEvent.stateMask & SWT.MOD1) != 0) {
                // closing the editor with Ctrl/Command-CR.
                keyEvent.doit = false;
                fireApplyEditorValue();
                deactivate();
                return;
            }
        } else {
            super.keyReleaseOccured(keyEvent);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link AnnotationData} with the new text and style ranges - and with the same ID as the original
     *         annotation (the one the editor was initialized with) - but in a new object.
     */
    @Override
    protected Object doGetValue() {
        assert m_styledText != null : "Control not created!";
        return AnnotationUtilities.toAnnotationData(m_styledText);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doSetFocus() {
        assert m_styledText != null : "Control not created!";
        if (m_selectAllUponFocusGain) {
            performSelectAll();
        }
        m_styledText.setFocus();
        if (m_mouseDownLocation != null) {
            if (m_styledText.getCharCount() > 0) {
                final Point parentLocation = m_panel.getLocation();
                // this is more or less a correct translation - certainly better that the results of SWT's toControl(Point)
                final Point translatedLocation =
                    new Point((m_mouseDownLocation.x - parentLocation.x), (m_mouseDownLocation.y - parentLocation.y));
                try {
                    final int charOffset = m_styledText.getOffsetAtLocation(translatedLocation);
                    m_styledText.setCaretOffset(charOffset);
                } catch (final IllegalArgumentException e) {
                    // thrown when the point is not within the text bounds
                    m_styledText.setCaretOffset(m_styledText.getCharCount());
                }
            }
        } else {
            m_styledText.setCaretOffset(m_styledText.getCharCount());
        }
        m_lastCaretMovementOffset = m_styledText.getCaretOffset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doSetValue(final Object value) {
        assert value instanceof Annotation : "Wrong value object!";
        final Annotation wa = (Annotation)value;
        final int alignment;
        switch (wa.getAlignment()) {
            case CENTER:
                alignment = SWT.CENTER;
                break;
            case RIGHT:
                alignment = SWT.RIGHT;
                break;
            default:
                alignment = SWT.LEFT;
        }

        m_selectAllUponFocusGain = false;
        m_annotationIsNodeAnnotation.set(wa instanceof NodeAnnotation);
        final String text;
        if (m_annotationIsNodeAnnotation.get()) {
            if (AnnotationUtilities.isDefaultNodeAnnotation(wa)) {
                text = AnnotationUtilities.getAnnotationText(wa);
                m_selectAllUponFocusGain = true;
            } else {
                text = wa.getText();
            }
        } else {
            text = wa.getText();

            final int annotationBorderSize = wa.getBorderSize();
            // set margins as borders
            m_styledText.setMarginColor(ColorUtilities.RGBintToColor(wa.getBorderColor()));
            if (annotationBorderSize > 0) {
                m_styledText.setMargins(annotationBorderSize, annotationBorderSize, annotationBorderSize,
                    annotationBorderSize);
            }

            // for workflow annotations set the default font to the size stored in the annotation
            final Font defFont;
            final int defFontSize = wa.getDefaultFontSize();
            if (defFontSize < 0) {
                defFont = AnnotationUtilities.getWorkflowAnnotationDefaultFont(); // uses the size from the pref page
            } else {
                defFont = AnnotationUtilities.getWorkflowAnnotationDefaultFont(defFontSize);
            }
            setDefaultFont(defFont);
        }
        checkSelectionOfAlignmentMenuItems(alignment);
        m_styledText.setAlignment(alignment);
        m_styledText.setText(text);
        m_styledText.setStyleRanges(AnnotationUtilities.toSWTStyleRanges(wa.getData(), m_styledText.getFont()));

        setBackgroundColor(ColorUtilities.RGBintToColor(wa.getBgColor()));
        syncShadowWithEditor();

        if (m_toolbar != null) {
            m_toolbar.updateToolbarToReflectState();
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCopyEnabled() {
        return (!m_styledText.isDisposed()) && (m_styledText.getSelectionCount() > 0);
    }

    /** {@inheritDoc} */
    @Override
    public void performCopy() {
        m_styledText.copy();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPasteEnabled() {
        return !m_styledText.isDisposed();
    }

    /** {@inheritDoc} */
    @Override
    public void performPaste() {
        m_styledText.paste();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCutEnabled() {
        return isCopyEnabled();
    }

    /** {@inheritDoc} */
    @Override
    public void performCut() {
        m_styledText.cut();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSelectAllEnabled() {
        return !m_styledText.isDisposed();
    }

    /** {@inheritDoc} */
    @Override
    public void performSelectAll() {
        m_styledText.selectAll();
        selectionChanged(null);
    }

    void setSWTStyle(final int swtStyle) {
        final FontStore fs = FontStore.INSTANCE;

        if (m_styledText.getSelectionCount() == 0) {
            createPendingStyleIfNeeded();

            if (fs.fontContainsStyle(m_pendingStyle.font, swtStyle)) {
                m_pendingStyle.font = fs.removeStyleFromFont(m_pendingStyle.font, swtStyle);
            } else {
                m_pendingStyle.font = fs.addStyleToFont(m_pendingStyle.font, swtStyle);
            }
        } else {
            final List<StyleRange> styles = getStylesInSelection();
            boolean shouldSetAttribute = true;
            for (final StyleRange s : styles) {
                if ((s.font != null) && fs.fontContainsStyle(s.font, swtStyle)) {
                    shouldSetAttribute = false;
                    break;
                }
            }
            for (final StyleRange s : styles) {
                if (shouldSetAttribute) {
                    s.font = fs.addStyleToFont(s.font, swtStyle);
                } else {
                    s.font = fs.removeStyleFromFont(s.font, swtStyle);
                }
                m_styledText.setStyleRange(s);
            }

            updateCachedStyleSelectionIfNecessary();

            fireEditorValueChanged(true, true);
        }
    }

    private StyleRange createNewStyleRange(final int start, final int length) {
        final StyleRange newStyle = new StyleRange();
        newStyle.font = m_styledText.getFont();
        newStyle.start = start;
        newStyle.length = length;
        return newStyle;
    }

    private void createPendingStyleIfNeeded() {
        if (m_pendingStyle == null) {
            m_pendingStyle = getNeighboringStyle(m_styledText.getCaretOffset(), 1);

            if (m_pendingStyle == null) {
                m_pendingStyle = new StyleRange();
                m_pendingStyle.font = m_styledText.getFont();
                m_pendingStyle.foreground = AnnotationUtilities.getAnnotationDefaultForegroundColor();
            }
        }
    }

    /**
     * Returns a list of ordered styles in the selected range. For regions in the selection that do not have a style
     * yet, it inserts a new (empty) style. The styles are ordered and not overlapping. If there is no selection in the
     * control, an empty list is returned, never null. Contained styles should be applied individually (after possible
     * modification) with setStyleRange().
     *
     * @return styles for the entire selected range, ordered and not overlapping. Empty list, if no selection exists,
     *         never null.
     */
    private List<StyleRange> getStylesInSelection() {
        if (m_styledText.isDisposed()) {
            return Collections.emptyList();
        }

        final Point selection = m_styledText.getSelectionRange();
        if (selection.y == 0) {
            return Collections.emptyList();
        }

        final int start = selection.x;
        final int length = selection.y;
        final StyleRange[] styles = m_styledText.getStyleRanges(start, length);
        if ((styles == null) || (styles.length == 0)) {
            // no existing styles in selection
            final StyleRange newStyle = new StyleRange();
            newStyle.font = m_styledText.getFont();
            newStyle.start = start;
            newStyle.length = length;
            return Collections.singletonList(newStyle);
        } else {
            final LinkedList<StyleRange> result = new LinkedList<StyleRange>();
            int lastEnd = start; // not yet covered index
            for (final StyleRange s : styles) {
                if (s.start < lastEnd) {
                    LOGGER.error("StyleRanges not ordered! Style might be messed up");
                }
                if (lastEnd < s.start) {
                    // create style for range not covered by next exiting style
                    final StyleRange newRange = new StyleRange();
                    newRange.font = m_styledText.getFont();
                    newRange.start = lastEnd;
                    newRange.length = s.start - lastEnd;
                    lastEnd = s.start;
                    result.add(newRange);
                }
                result.add(s);
                lastEnd = s.start + s.length;
            }
            if (lastEnd < (start + length)) {
                // create new style for the part at the end, not covered
                final StyleRange newRange = new StyleRange();
                newRange.font = m_styledText.getFont();
                newRange.start = lastEnd;
                newRange.length = start + length - lastEnd;
                result.add(newRange);
            }
            return result;
        }
    }

    void userWantsToAffectBackgroundColor(final Point clickSourceLocation) {
        displayColorDropDown(m_backgroundColor, BACKGROUND_COLOR_SELECTION, clickSourceLocation);
    }

    void userWantsToAffectBorderColor(final Point clickSourceLocation) {
        displayColorDropDown(m_styledText.getMarginColor(), BORDER_COLOR_SELECTION, clickSourceLocation);
    }

    /**
     * Change alignment.
     *
     * @param alignment SWT.LEFT|CENTER|RIGHT.
     */
    void alignment(final int alignment) {
        checkSelectionOfAlignmentMenuItems(alignment);
        m_styledText.setAlignment(alignment);

        fireEditorValueChanged(true, true);
    }

    void borderWidthWasSelected(final int width) {
        m_styledText.setMargins(width, width, width, width);

        fireEditorValueChanged(true, true);
    }

    void colorWasSelected(final Color color) {
        m_colorDropDown.setVisible(false);

        if (color != null) {
            switch (m_currentColorSelectionTarget) {
                case FONT_COLOR_SELECTION:
                    if (m_styledText.getSelectionCount() == 0) {
                        createPendingStyleIfNeeded();
                        m_pendingStyle.foreground = color;
                    } else {
                        for (final StyleRange style : getStylesInSelection()) {
                            style.foreground = color;
                            m_styledText.setStyleRange(style);
                        }
                        updateCachedStyleSelectionIfNecessary();
                    }
                    break;
                case BORDER_COLOR_SELECTION:
                    m_styledText.setMarginColor(color);
                    m_panel.redraw();
                    break;
                case BACKGROUND_COLOR_SELECTION:
                    setBackgroundColor(color);
                    break;
            }

            m_toolbar.updateToolbarToReflectState();
        }

        fireEditorValueChanged(true, true);

        m_styledText.getDisplay().asyncExec(() -> {
            m_styledText.forceFocus();
        });
    }

    void fontSizeWasSelected(final int size) {
        if (m_styledText.getSelectionCount() == 0) {
            createPendingStyleIfNeeded();

            final FontData fd = m_pendingStyle.font.getFontData()[0];
            final boolean bold = (fd.getStyle() & SWT.BOLD) != 0;
            final boolean italic = (fd.getStyle() & SWT.ITALIC) != 0;
            m_pendingStyle.font = FontStore.INSTANCE.getDefaultFont(size, bold, italic);
        } else {
            for (final StyleRange style : getStylesInSelection()) {
                final FontData styleFD = style.font.getFontData()[0];
                final boolean bold = (styleFD.getStyle() & SWT.BOLD) != 0;
                final boolean italic = (styleFD.getStyle() & SWT.ITALIC) != 0;

                style.font = FontStore.INSTANCE.getDefaultFont(size, bold, italic);
                m_styledText.setStyleRange(style);
            }

            updateCachedStyleSelectionIfNecessary();

            fireEditorValueChanged(true, true);
        }
    }

    void userWantsToAffectFontColor(final Point clickSourceLocation) {
        Color color = null;
        boolean multipleColorsExist = false;

        if (m_pendingStyle != null) {
            color = m_pendingStyle.foreground;
        } else {
            final List<StyleRange> styles = getStylesInSelection();
            for (final StyleRange style : styles) {
                final Color c;

                if (style.foreground != null) {
                    c = style.foreground;
                } else {
                    c = AnnotationUtilities.getAnnotationDefaultForegroundColor();
                }

                if (color == null) {
                    color = c;
                } else if (!c.equals(color)) {
                    multipleColorsExist = true;

                    break;
                }
            }

            if (color == null) {
                final StyleRange sr = getNeighboringStyle(m_styledText.getCaretOffset(), 0);

                if (sr != null) {
                    color = sr.foreground;
                }
            }

            if (color == null) {
                color = AnnotationUtilities.getAnnotationDefaultForegroundColor();
            }
        }

        displayColorDropDown((multipleColorsExist ? null : color), FONT_COLOR_SELECTION, clickSourceLocation);
    }

    /**
     * Used by <code>StyledTextEditorLocator.relocate(CellEditor)</code>
     *
     * @return the bounds needed to display the current text
     */
    Rectangle getTextBounds() {
        // use the shadow instance to get the size of the not auto-wrapped text
        final int charCount = m_shadowStyledText.getCharCount();
        if (charCount < 1) {
            Rectangle b = m_shadowStyledText.getBounds();
            return new Rectangle(b.x, b.y, 0, 0);
        } else {
            Rectangle r = m_shadowStyledText.getTextBounds(0, charCount - 1);
            if (m_shadowStyledText.getText(charCount - 1, charCount - 1).charAt(0) == '\n') {
                r.height += m_shadowStyledText.getLineHeight();
            }
            return r;
        }
    }

    private void displayColorDropDown(final Color color, final int colorSelectionTarget, final Point parentLocation) {
        final Rectangle bounds = m_toolbar.getBounds();

        m_colorDropDown.setSelectedColor(color);
        m_colorDropDown.setLocation((bounds.x + parentLocation.x), (bounds.y + bounds.height));

        // We are using the same ColorDropDown instance for font, background and border color.
        // Clicking two of these buttons succesively means that the colour drop down was
        // already visible. `setVisible(true)` then does not produce a new SWT.Show event.
        // This event is needed to 1) trigger FloatingStyleToolbar#recomputeRegion and
        // 2) have TransientEditAssetGroup behave as intended.
        if (m_colorDropDown.isVisible()) {
            m_colorDropDown.notifyListeners(SWT.Show, new Event());
        } else {
            // Will produce event if not visible before.
            m_colorDropDown.setVisible(true);
        }

        m_colorDropDown.setFocus();

        m_currentColorSelectionTarget = colorSelectionTarget;
    }


    ///  CONTEXT MENU SUBSYSTEM vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    ///  CONTEXT MENU SUBSYSTEM vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    ///  CONTEXT MENU SUBSYSTEM vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    ///  CONTEXT MENU SUBSYSTEM vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
    ///  CONTEXT MENU SUBSYSTEM vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv

    private void enableStyleButtons(final boolean enableThem) {
        if (m_enableOnSelectedTextMenuItems != null) {
            for (final MenuItem action : m_enableOnSelectedTextMenuItems) {
                action.setEnabled(enableThem);
            }
        }
    }

    @SuppressWarnings("unused") // menu item instance creation without assignation
    private void addMenu(final Composite parent) {
        final Menu menu = new Menu(parent);
        menu.addListener(SWT.Hide, new Listener() {
            @Override
            public void handleEvent(final Event event) {
                markStyleDecorationCloseTime();
            }
        });
        Image image;
        MenuItem action;

        // background color
        image = ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/annotations/bgcolor_10.png");
        action = addMenuItem(menu, "bg", SWT.PUSH, "Background", image);

        // alignment
        image = ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/annotations/alignment_10.png");

        MenuItem alignmentMenuItem = addMenuItem(menu, "alignment", SWT.CASCADE, "Alignment", image);

        final Menu alignMenu = new Menu(alignmentMenuItem);
        alignmentMenuItem.setMenu(alignMenu);

        m_leftAlignMenuItem = addMenuItem(alignMenu, "alignment_left", SWT.RADIO, "Left", null);
        m_leftAlignMenuItem.setSelection(true);

        m_centerAlignMenuItem = addMenuItem(alignMenu, "alignment_center", SWT.RADIO, "Center", null);

        m_rightAlignMenuItem = addMenuItem(alignMenu, "alignment_right", SWT.RADIO, "Right", null);

        m_alignmentMenuItems = new MenuItem[] {m_leftAlignMenuItem, m_centerAlignMenuItem, m_rightAlignMenuItem};

        new MenuItem(menu, SWT.SEPARATOR);
        // contains buttons being en/disabled with selection
        m_enableOnSelectedTextMenuItems = new ArrayList<MenuItem>();

        // font/style button
        image = ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/annotations/font_10.png");
        action = addMenuItem(menu, "style", SWT.PUSH, "Font Style...", image);
        m_enableOnSelectedTextMenuItems.add(action);

        new MenuItem(menu, SWT.SEPARATOR);

        // border style
        image = ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/annotations/border_10.png");
        m_borderMenuItem = addMenuItem(menu, "border", SWT.PUSH, "Border...", image);
        m_postBorderMenuItemSeparator = new MenuItem(menu, SWT.SEPARATOR);

        // ok button
        image = ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/annotations/ok_10.png");
        addMenuItem(menu, "ok", SWT.PUSH, "OK (commit)", image);

        // cancel button
        image = ImageRepository.getImage(KNIMEEditorPlugin.PLUGIN_ID, "icons/annotations/cancel_10.png");
        addMenuItem(menu, "cancel", SWT.PUSH, "Cancel (discard)", image);

        parent.setMenu(menu);
    }

    private MenuItem addMenuItem(final Menu menuMgr, final String id, final int style, final String text,
        final Image img) {
        final MenuItem menuItem = new MenuItem(menuMgr, style);
        final SelectionListener listener = new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                buttonClick(id);
            }
        };
        menuItem.addSelectionListener(listener);
        menuItem.setText(text);
        menuItem.setImage(img);
        return menuItem;
    }

    private void buttonClick(final String src) {
        if (src.equals("style")) {
            font();
            fireEditorValueChanged(true, true);
        } else if (src.equals("color")) {
            fontColor();
            fireEditorValueChanged(true, true);
        } else if (src.equals("bold")) {
            bold();
        } else if (src.equals("italic")) {
            italic();
        } else if (src.equals("bg")) {
            bgColor();
            fireEditorValueChanged(true, true);
        } else if (src.equals("alignment_left")) {
            alignment(SWT.LEFT);
            m_toolbar.updateToolbarToReflectState();
        } else if (src.equals("alignment_center")) {
            alignment(SWT.CENTER);
            m_toolbar.updateToolbarToReflectState();
        } else if (src.equals("alignment_right")) {
            alignment(SWT.RIGHT);
            m_toolbar.updateToolbarToReflectState();
        } else if (src.equals("border")) {
            borderStyle();
            fireEditorValueChanged(true, true);
        } else if (src.equals("ok")) {
            ok();
        } else if (src.equals("cancel")) {
            cancel();
        } else {
            LOGGER.coding("IMPLEMENTATION ERROR: Wrong button ID");
        }

        // set the focus back to the editor after the buttons finish
        if (!src.equals("ok") && !src.equals("cancel")) {
            m_styledText.setFocus();
        }
    }

    /**
     * Update selection state of alignment buttons in menu.
     *
     * @param swtAlignment SWT.LEFT, CENTER, or RIGHT
     */
    private void checkSelectionOfAlignmentMenuItems(final int swtAlignment) {
        MenuItem activeMenuItem;
        switch (swtAlignment) {
            case SWT.LEFT:
                activeMenuItem = m_leftAlignMenuItem;
                break;
            case SWT.CENTER:
                activeMenuItem = m_centerAlignMenuItem;
                break;
            case SWT.RIGHT:
                activeMenuItem = m_rightAlignMenuItem;
                break;
            default:
                LOGGER.coding("Invalid alignment (ignored): " + swtAlignment);
                return;
        }
        for (final MenuItem m : m_alignmentMenuItems) {
            m.setSelection(m == activeMenuItem);
        }
    }

    private void font() {
        final List<StyleRange> selectionStyles = getStylesInSelection();
        Font f = m_styledText.getFont();
        Color c = null;
        // set the first font style in the selection
        for (final StyleRange style : selectionStyles) {
            if (style.font != null) {
                f = style.font;
                c = style.foreground;
                break;
            }
        }
        final FontData fd = f.getFontData()[0];
        final FontStyleDialog dlg = new FontStyleDialog(m_styledText.getShell(), c, fd.getHeight(),
            (fd.getStyle() & SWT.BOLD) != 0, (fd.getStyle() & SWT.ITALIC) != 0);
        if (dlg.open() != Window.OK) {
            // user canceled.
            return;
        }

        final RGB newRGB = dlg.getColor();
        final Integer newSize = dlg.getSize();
        final Boolean newBold = dlg.getBold();
        final Boolean newItalic = dlg.getItalic();
        final Color newColor = (newRGB == null) ? null : ColorUtilities.RGBtoColor(newRGB);
        if (m_styledText.getSelectionCount() == 0) {
            if (newColor != null) {
                m_currentColorSelectionTarget = FONT_COLOR_SELECTION;
                colorWasSelected(newColor);
            }
            if (newSize != null) {
                fontSizeWasSelected(newSize.intValue());
            }
            if (newBold != null) {
                setSWTStyle(SWT.BOLD);
            }
            if (newItalic != null) {
                setSWTStyle(SWT.ITALIC);
            }
        } else {
            for (final StyleRange style : selectionStyles) {
                if ((newSize != null) || (newBold != null) || (newItalic != null)) {
                    final FontData stylefd = style.font.getFontData()[0];
                    boolean b = (stylefd.getStyle() & SWT.BOLD) != 0;
                    if (newBold != null) {
                        b = newBold.booleanValue();
                    }
                    boolean i = (stylefd.getStyle() & SWT.ITALIC) != 0;
                    if (newItalic != null) {
                        i = newItalic.booleanValue();
                    }
                    int s = stylefd.getHeight();
                    if (newSize != null) {
                        s = newSize.intValue();
                    }
                    style.font = FontStore.INSTANCE.getDefaultFont(s, b, i);
                }
                if (newColor != null) {
                    style.foreground = newColor;
                }
                m_styledText.setStyleRange(style);
            }

            updateCachedStyleSelectionIfNecessary();
        }

        m_toolbar.updateToolbarToReflectState();
    }

    private void fontColor() {
        Color col = AnnotationUtilities.getAnnotationDefaultForegroundColor();
        final List<StyleRange> selectionStyles = getStylesInSelection();
        // set the color of the first selection style
        for (final StyleRange style : selectionStyles) {
            if (style.foreground != null) {
                col = style.foreground;
                break;
            }
        }
        final ColorDialog colorDialog = new ColorDialog(m_styledText.getShell());
        colorDialog.setText("Change Font Color in Selection");
        colorDialog.setRGB(col.getRGB());
        final RGB newRGB = colorDialog.open();
        if (newRGB == null) {
            // user canceled
            return;
        }

        final Color newColor = ColorUtilities.RGBtoColor(newRGB);
        if (m_styledText.getSelectionCount() == 0) {
            m_currentColorSelectionTarget = FONT_COLOR_SELECTION;
            colorWasSelected(newColor);
        } else {
            for (final StyleRange style : selectionStyles) {
                style.foreground = newColor;
                m_styledText.setStyleRange(style);
            }

            updateCachedStyleSelectionIfNecessary();
        }

        m_toolbar.updateToolbarToReflectState();
    }

    private void bold() {
        setSWTStyle(SWT.BOLD);
        m_toolbar.updateToolbarToReflectState();
    }

    private void italic() {
        setSWTStyle(SWT.ITALIC);
        m_toolbar.updateToolbarToReflectState();
    }

    private void bgColor() {
        final ColorDialog colorDialog = new ColorDialog(m_styledText.getShell());
        RGB[] toSet = LAST_COLORS == null ? DEFAULT_COLORS : LAST_COLORS;
        colorDialog.setText("Change the Background Color");
        colorDialog.setRGBs(toSet);
        if (m_backgroundColor != null) {
            colorDialog.setRGB(m_backgroundColor.getRGB());
        }
        RGB newBGCol = colorDialog.open();
        markStyleDecorationCloseTime();
        if (newBGCol == null) {
            // user canceled
            return;
        }
        LAST_COLORS = colorDialog.getRGBs();
        m_backgroundColor = new Color(null, newBGCol);
        applyBackgroundColor();
        m_toolbar.updateToolbarToReflectState();
    }


    private void borderStyle() {
        BorderStyleDialog dlg = new BorderStyleDialog(m_styledText.getShell(), m_styledText.getMarginColor(),
            m_styledText.getRightMargin());
        if (dlg.open() == Window.OK) {
            m_styledText.setMarginColor(ColorUtilities.RGBtoColor(dlg.getColor()));
            m_styledText.redraw();
            int s = dlg.getSize();
            m_styledText.setMargins(s, s, s, s);
            m_toolbar.updateToolbarToReflectState();
        }
    }

    private void ok() {
        fireApplyEditorValue();
        deactivate();
        return;
    }

    private void cancel() {
        fireCancelEditor();
        deactivate();
        return;
    }
    ///  CONTEXT MENU SUBSYSTEM ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    ///  CONTEXT MENU SUBSYSTEM ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    ///  CONTEXT MENU SUBSYSTEM ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    ///  CONTEXT MENU SUBSYSTEM ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    ///  CONTEXT MENU SUBSYSTEM ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
}
