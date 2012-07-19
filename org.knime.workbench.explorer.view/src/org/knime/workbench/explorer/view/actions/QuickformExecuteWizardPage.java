/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 */
package org.knime.workbench.explorer.view.actions;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.quickform.AbstractQuickFormConfiguration;
import org.knime.core.quickform.AbstractQuickFormValueInConfiguration;
import org.knime.core.quickform.QuickFormConfigurationPanel;
import org.knime.core.quickform.in.QuickFormInputNode;
import org.knime.core.util.Pair;
import org.knime.workbench.explorer.ExplorerActivator;
import org.knime.workbench.ui.wrapper.Panel2CompositeWrapper;

/**
 *
 * @author Thomas Gabriel, KNIME.com AG, Zurich
 * @since 3.1
 */
public class QuickformExecuteWizardPage extends WizardPage {

    private static final ImageDescriptor ICON = ExplorerActivator
            .imageDescriptorFromPlugin(ExplorerActivator.PLUGIN_ID,
                    "icons/new_knime55.png");

    private final int m_index;

    private final QuickformExecuteWizard m_wizard;

    private final Map<Pair<NodeID, QuickFormInputNode>,
            QuickFormConfigurationPanel
                <? extends AbstractQuickFormValueInConfiguration>> m_nodes;

    /**
     * Create a new quickform wizard page.
     * @param wizard parent wizard
     * @param index index of current wizard page
     */
    QuickformExecuteWizardPage(final QuickformExecuteWizard wizard,
            final int index) {
        super("QuickformExecuteWizardPage_" + index);
        setTitle("QuickForm Execution Wizard");
        setDescription(
            "Stepwise Execution of a Workflow using QuickForm nodes.");
        setImageDescriptor(ICON);
        m_index = index + 1;
        m_wizard = wizard;
        m_nodes = new LinkedHashMap<Pair<NodeID, QuickFormInputNode>,
                QuickFormConfigurationPanel
                    <? extends AbstractQuickFormValueInConfiguration>>();
    }

    /** {@inheritDoc} */
    @Override
    public boolean canFlipToNextPage() {
        return !m_wizard.findQuickformNodes().isEmpty();
    }

    /** {@inheritDoc} */
    @Override
    public IWizardPage getNextPage() {
        m_wizard.stepExecution();
        return m_wizard.getNextPage(
                new QuickformExecuteWizardPage(m_wizard, m_index));
    }

    @Override
    public IWizard getWizard() {
        return m_wizard;
    }

    /** {@inheritDoc} */
    @Override
    public void createControl(final Composite parent) {
        Map<NodeID, QuickFormInputNode> nodes = m_wizard.findQuickformNodes();
        setControl(setQuickformNodes(parent, nodes));
    }

    /**
     * Set quickform nodes into this dialog; called just before
     * {@link #loadSettingsFrom(NodeSettingsRO,
     * org.knime.core.data.DataTableSpec[])} is called.
     * @param nodes the quickform nodes to show settings for
     */
    private Composite setQuickformNodes(final Composite parent,
            final Map<NodeID, QuickFormInputNode> nodes) {
        // create a new panel holding all Swing/AWT quickform components

        List<Pair<Integer, QuickFormConfigurationPanel<?>>> sortedPanelList =
            new ArrayList<Pair<Integer, QuickFormConfigurationPanel<?>>>();

        // a list of quick form elements that will sorted below according to
        // the weight values
        for (Map.Entry<NodeID, QuickFormInputNode> e : nodes.entrySet()) {
            AbstractQuickFormConfiguration
                <? extends AbstractQuickFormValueInConfiguration> config =
                    e.getValue().getConfiguration();
            if (config == null) { // quickform nodes has no valid configuration
                continue;
            }
            QuickFormConfigurationPanel
                <? extends AbstractQuickFormValueInConfiguration> quickform =
                    config.createController();
            m_nodes.put(new Pair<NodeID, QuickFormInputNode>(
                    e.getKey(), e.getValue()), quickform);
            Pair<Integer, QuickFormConfigurationPanel<?>> weightPanelPair =
                new Pair<Integer, QuickFormConfigurationPanel<?>>(
                        config.getWeight(), quickform);
            sortedPanelList.add(weightPanelPair);
        }
        Collections.sort(sortedPanelList, new Comparator<
                Pair<Integer, QuickFormConfigurationPanel<?>>>() {
            /** {@inheritDoc} */
            @Override
            public int compare(
                    final Pair<Integer, QuickFormConfigurationPanel<?>> o1,
                    final Pair<Integer, QuickFormConfigurationPanel<?>> o2) {
                return o1.getFirst() - o2.getFirst();
            }
        });

        final JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(0, 0, 0, 0);
        c.anchor = GridBagConstraints.NORTHWEST;

        org.eclipse.swt.graphics.Color wizardPageBgColor = parent.getBackground();
        Color bgColor = new Color(wizardPageBgColor.getRed(),
                wizardPageBgColor.getGreen(), wizardPageBgColor.getBlue());
        panel.setBackground(bgColor);


        for (Pair<Integer, QuickFormConfigurationPanel<?>> weightPanelPair
                    : sortedPanelList) {
            QuickFormConfigurationPanel<?> p = weightPanelPair.getSecond();
            p.setBackground(bgColor);
            panel.add(weightPanelPair.getSecond(), c);
            c.gridy++;
        }

        JPanel buffer = new JPanel();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        buffer.setBackground(bgColor);
        panel.add(buffer, c);
        if (m_nodes.isEmpty()) {
            panel.add(new JLabel("No valid Quickform configurations. You probably need "
                    + "to reset the quickform nodes first."));
        }

        JScrollPane sp = new JScrollPane(panel);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setBackground(bgColor);
        return new Panel2CompositeWrapper(parent, sp, 0);
    }
}
