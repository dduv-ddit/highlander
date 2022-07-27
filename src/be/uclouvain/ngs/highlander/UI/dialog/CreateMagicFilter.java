/*****************************************************************************************
*
* Highlander - Copyright (C) <2012-2020> <Université catholique de Louvain (UCLouvain)>
* 	
* List of the contributors to the development of Highlander: see LICENSE file.
* Description and complete License: see LICENSE file.
* 	
* This program (Highlander) is free software: 
* you can redistribute it and/or modify it under the terms of the 
* GNU General Public License as published by the Free Software Foundation, 
* either version 3 of the License, or (at your option) any later version.
* 
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with this program (see COPYING file).  If not, 
* see <http://www.gnu.org/licenses/>.
* 
*****************************************************************************************/

/**
*
* @author Raphael Helaers
*
*/

package be.uclouvain.ngs.highlander.UI.dialog;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.javadev.AnimatingCardLayout;
import org.javadev.effects.SlideAnimation;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.Resources.Palette;
import be.uclouvain.ngs.highlander.UI.misc.HighlanderObserver;
import be.uclouvain.ngs.highlander.UI.toolbar.FilteringPanel;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Interval;
import be.uclouvain.ngs.highlander.datatype.filter.CombinedHeterozygousVariants;
import be.uclouvain.ngs.highlander.datatype.filter.CommonGeneVariants;
import be.uclouvain.ngs.highlander.datatype.filter.Filter;
import be.uclouvain.ngs.highlander.datatype.filter.Intervals;
import be.uclouvain.ngs.highlander.datatype.filter.MultipleNucleotidesPolymorphisms;
import be.uclouvain.ngs.highlander.datatype.filter.PathologyFrequency;
import be.uclouvain.ngs.highlander.datatype.filter.SameCodon;
import be.uclouvain.ngs.highlander.datatype.filter.SampleSpecificVariants;
import be.uclouvain.ngs.highlander.datatype.filter.VariantsCommonToSamples;
import be.uclouvain.ngs.highlander.datatype.filter.CommonGeneVariants.NumVariantThreshold;
import be.uclouvain.ngs.highlander.datatype.filter.CustomFilter.ComparisonOperator;
import be.uclouvain.ngs.highlander.datatype.filter.Filter.FilterType;
import be.uclouvain.ngs.highlander.datatype.filter.Filter.LogicalOperator;
import be.uclouvain.ngs.highlander.datatype.filter.VariantsCommonToSamples.VCSCriterion;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.AutoCompleteSupport;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.JRadioButton;

import java.awt.Component;
import java.awt.FlowLayout;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

public class CreateMagicFilter extends JDialog {

	private HighlanderObserver obs = new HighlanderObserver();

	private Filter filter = null;
	private final FilteringPanel filteringPanel;

	private final String FILTER_SELECTION = "Filter selection";
	private final String VARIANTS_COMMON_TO_SAMPLES = FilterType.VARIANTS_COMMON_TO_SAMPLES.getName();
	private final String SAMPLE_SPECIFIC_VARIANTS = FilterType.SAMPLE_SPECIFIC_VARIANTS.getName();
	private final String COMMON_GENE_VARIANTS = FilterType.COMMON_GENE_VARIANTS.getName();
	private final String PATHOLOGY_FREQUENCY = FilterType.PATHOLOGY_FREQUENCY.getName();
	private final String INTERVALS = FilterType.INTERVALS.getName();
	private final String SAME_CODON = FilterType.SAME_CODON.getName();
	private final String MULTIPLE_NUCLEOTIDES_POLYMORPHISMS = FilterType.MULTIPLE_NUCLEOTIDES_VARIANTS.getName();
	private final String COMBINED_HETEROZYGOUS_VARIANTS = FilterType.COMBINED_HETEROZYGOUS_VARIANTS.getName();

	private String currentPanel = FILTER_SELECTION;

	private String[] availableSamples;
	private String[] availablePathologies;

	private AnimatingCardLayout centerCardLayout;
	private JPanel centerPanel;	
	private JButton btnBack;
	private JButton btnNext;	
	private JPanel filterSelectionPanel;	
	private final ButtonGroup selectionButtonGroup = new ButtonGroup();

	private JRadioButton selectionVariantIntersection;
	private JRadioButton selectionVariantComplement;
	private JRadioButton selectionGeneCommon;
	private JRadioButton selectionCombinedHeterozygous;
	private JRadioButton selectionPathologyFrequency;
	private JRadioButton selectionIntervals;
	private JRadioButton selectionSameCodon;
	private JRadioButton selectionMNV;
	private JPanel panel_varint_main;
	private IndependentFilteringPanel varIntPreFilter = new IndependentFilteringPanel(null, obs);
	private String[] varIntFields = new String[]{"zygosity","allelic depth ref","allelic depth alt","proportion ref","proportion alt"};
	private int varIntCols = varIntFields.length;
	private int varIntRow = 0;
	private JPanel varIntSubPanel;
	private Map<String, JCheckBox> varIntChkBoxes = new LinkedHashMap<String, JCheckBox>();
	private Map<JComboBox<String>, List<FieldCriterion>> varIntValues = new LinkedHashMap<JComboBox<String>, List<FieldCriterion>>();
	private JPanel panel_varcomp_main;
	private IndependentFilteringPanel varCompPreFilter = new IndependentFilteringPanel(null, obs);
	private int varCompNumCrit = 0;
	private Map<Integer, LogicalOperator> varCompLogicalOperator = new HashMap<Integer, LogicalOperator>();
	private JPanel panel_genecom_main;
	private IndependentFilteringPanel geneComPreFilter = new IndependentFilteringPanel(null, obs);
	private int geneComNumSample = 0;
	private DefaultListModel<String> geneComSampleListModel = new DefaultListModel<>();
	private JSpinner geneComMinCommon;
	private JComboBox<String> geneComNumVariantsThreshold = new JComboBox<String>(NumVariantThreshold.getTextValues());
	private JSpinner geneComMinMaxVariants;
	private JPanel panel_freqpat_main;
	private IndependentFilteringPanel freqpatPreFilter = new IndependentFilteringPanel(null, obs);
	private JComboBox<String> freqpatPathology = new JComboBox<String>();
	private JComboBox<Field> freqpatField = new JComboBox<Field>();
	private JComboBox<ComparisonOperator> freqpatComparisonOperator = new JComboBox<ComparisonOperator>();
	private JTextField freqpatValue = new JTextField();
	private DefaultListModel<String> freqpatSampleListModel = new DefaultListModel<>();
	private JPanel panel_combhet_main;
	private IndependentFilteringPanel combHetPreFilter = new IndependentFilteringPanel(null, obs);
	private String[] combHetLabels = new String[]{"Child","Father","Mother"};
	private List<JComboBox<String>> combHetBoxes = new ArrayList<>();
	private IndependentFilteringPanel intervalPreFilter = new IndependentFilteringPanel(null, obs);
	private JTextArea text_intervals_intervals;
	private JRadioButton intervals_inside;
	private JRadioButton intervals_outside;
	private DefaultListModel<String> intervals_samples = new DefaultListModel<>();
	private IndependentFilteringPanel sameCodonPreFilter = new IndependentFilteringPanel(null, obs);
	private DefaultListModel<String> same_codon_samples = new DefaultListModel<>();
	private IndependentFilteringPanel mnpPreFilter = new IndependentFilteringPanel(null, obs);
	private DefaultListModel<String> mnp_samples = new DefaultListModel<>();

	public CreateMagicFilter(FilteringPanel filteringPanel, Filter criterion) {
		this.filteringPanel = filteringPanel;
		this.filter = criterion;
		List<String> listap = new ArrayList<String>(); 
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
				"SELECT `value` FROM "+Highlander.getCurrentAnalysis().getFromPossibleValues()+"WHERE `field` = 'sample' ORDER BY `value`")) {
			while (res.next()){
				listap.add(res.getString(1));
			}
			availableSamples = listap.toArray(new String[0]);
		}catch(Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(this, Tools.getMessage("Can't retreive sample list", ex), "Fetching sample list",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
		listap.clear();
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
				"SELECT "+Field.pathology.getQuerySelectName(Highlander.getCurrentAnalysis(), false) + " "
				+ "FROM "+Highlander.getCurrentAnalysis().getFromPathologies()
				)) {
			while (res.next()){
				listap.add(res.getString(1));
			}
			availablePathologies = listap.toArray(new String[0]);
		}catch(Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(this, Tools.getMessage("Can't retreive sample list", ex), "Fetching sample list",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
		initUI();
		fillFields();
		pack();
		this.addComponentListener(new ComponentListener() {
			@Override
			public void componentShown(ComponentEvent arg0) {
			}
			@Override
			public void componentResized(ComponentEvent arg0) {
				obs.setControlName("RESIZE_TOOLBAR");
			}
			@Override
			public void componentMoved(ComponentEvent arg0) {
			}

			@Override
			public void componentHidden(ComponentEvent arg0) {
			}
		});
	}

	public CreateMagicFilter(FilteringPanel filteringPanel){
		this(filteringPanel, null);
	}

	private void initUI(){
		setModal(true);
		setTitle("Create a magic filter");
		setIconImage(Resources.getScaledIcon(Resources.iFilterMagic, 64).getImage());

		JPanel panel = new JPanel();	
		getContentPane().add(panel, BorderLayout.SOUTH);

		btnBack = new JButton(Resources.getScaledIcon(Resources.iCross, 24));
		btnBack.setToolTipText("Back");
		btnBack.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {	
				if (currentPanel.equals(FILTER_SELECTION)){
					cancelClose();
				}else if (currentPanel.equals(VARIANTS_COMMON_TO_SAMPLES) 
						|| currentPanel.equals(SAMPLE_SPECIFIC_VARIANTS)
						|| currentPanel.equals(COMMON_GENE_VARIANTS)
						|| currentPanel.equals(PATHOLOGY_FREQUENCY)
						|| currentPanel.equals(INTERVALS)
						|| currentPanel.equals(SAME_CODON)
						|| currentPanel.equals(MULTIPLE_NUCLEOTIDES_POLYMORPHISMS)
						|| currentPanel.equals(COMBINED_HETEROZYGOUS_VARIANTS)){
					showPanel(FILTER_SELECTION);
				}
			}
		});
		panel.add(btnBack);

		btnNext = new JButton(Resources.getScaledIcon(Resources.iArrowDoubleRight, 24));
		btnNext.setToolTipText("Next");
		btnNext.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {				
				if (currentPanel.equals(FILTER_SELECTION)){
					if (selectionVariantIntersection.isSelected()){
						showPanel(VARIANTS_COMMON_TO_SAMPLES);
					}else if (selectionVariantComplement.isSelected()){
						showPanel(SAMPLE_SPECIFIC_VARIANTS);
					}else if (selectionGeneCommon.isSelected()){
						showPanel(COMMON_GENE_VARIANTS);
					}else if (selectionPathologyFrequency.isSelected()){
						showPanel(PATHOLOGY_FREQUENCY);
					}else if (selectionIntervals.isSelected()){
						showPanel(INTERVALS);
					}else if (selectionSameCodon.isSelected()){
						showPanel(SAME_CODON);
					}else if (selectionMNV.isSelected()){
						showPanel(MULTIPLE_NUCLEOTIDES_POLYMORPHISMS);
					}else if (selectionCombinedHeterozygous.isSelected()){
						showPanel(COMBINED_HETEROZYGOUS_VARIANTS);
					}
				}else if (currentPanel.equals(VARIANTS_COMMON_TO_SAMPLES) 
						|| currentPanel.equals(SAMPLE_SPECIFIC_VARIANTS)
						|| currentPanel.equals(COMMON_GENE_VARIANTS)
						|| currentPanel.equals(PATHOLOGY_FREQUENCY)
						|| currentPanel.equals(INTERVALS)
						|| currentPanel.equals(SAME_CODON)
						|| currentPanel.equals(MULTIPLE_NUCLEOTIDES_POLYMORPHISMS)
						|| currentPanel.equals(COMBINED_HETEROZYGOUS_VARIANTS)){
					try {
						generateCriterion();
						dispose();		  				
					} catch (Exception ex) {
						Tools.exception(ex);
						JOptionPane.showMessageDialog(CreateMagicFilter.this, Tools.getMessage("Cannot create filter", ex), "Creating magic filter",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			}
		});
		panel.add(btnNext);

		centerPanel = new JPanel();
		centerCardLayout = new AnimatingCardLayout(new SlideAnimation());
		centerCardLayout.setAnimationDuration(500);
		centerPanel.setLayout(centerCardLayout);

		filterSelectionPanel = new JPanel();
		JScrollPane scroll = new JScrollPane(filterSelectionPanel);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		centerPanel.add(scroll, FILTER_SELECTION);
		GridBagLayout gbl_filterSelection = new GridBagLayout();
		filterSelectionPanel.setLayout(gbl_filterSelection);

		setAvailableFilters();

		centerPanel.add(getPanelVariantIntersection(), VARIANTS_COMMON_TO_SAMPLES);
		centerPanel.add(getPanelVariantComplement(), SAMPLE_SPECIFIC_VARIANTS);
		centerPanel.add(getPanelGeneCommon(), COMMON_GENE_VARIANTS);
		centerPanel.add(getPanelCombinedHeterozygous(), COMBINED_HETEROZYGOUS_VARIANTS);
		centerPanel.add(getPanelPathologyFrequency(), PATHOLOGY_FREQUENCY);
		centerPanel.add(getPanelIntervals(), INTERVALS);
		centerPanel.add(getPanelSameCodon(), SAME_CODON);
		centerPanel.add(getPanelMNV(), MULTIPLE_NUCLEOTIDES_POLYMORPHISMS);

		centerCardLayout.show(centerPanel, FILTER_SELECTION);

		getContentPane().add(centerPanel, BorderLayout.CENTER);

	}

	private void showPanel(String panel){
		if (panel.equals(VARIANTS_COMMON_TO_SAMPLES)){
			centerCardLayout.show(centerPanel, VARIANTS_COMMON_TO_SAMPLES);						
			currentPanel = VARIANTS_COMMON_TO_SAMPLES;
			btnBack.setIcon(Resources.getScaledIcon(Resources.iArrowDoubleLeft, 24));
			btnNext.setIcon(Resources.getScaledIcon(Resources.iButtonApply, 24));
		}else if (panel.equals(SAMPLE_SPECIFIC_VARIANTS)){
			centerCardLayout.show(centerPanel, SAMPLE_SPECIFIC_VARIANTS);						
			currentPanel = SAMPLE_SPECIFIC_VARIANTS;
			btnBack.setIcon(Resources.getScaledIcon(Resources.iArrowDoubleLeft, 24));
			btnNext.setIcon(Resources.getScaledIcon(Resources.iButtonApply, 24));	
		}else if (panel.equals(COMMON_GENE_VARIANTS)){
			centerCardLayout.show(centerPanel, COMMON_GENE_VARIANTS);						
			currentPanel = COMMON_GENE_VARIANTS;
			btnBack.setIcon(Resources.getScaledIcon(Resources.iArrowDoubleLeft, 24));
			btnNext.setIcon(Resources.getScaledIcon(Resources.iButtonApply, 24));	
		}else if (panel.equals(PATHOLOGY_FREQUENCY)){
			centerCardLayout.show(centerPanel, PATHOLOGY_FREQUENCY);						
			currentPanel = PATHOLOGY_FREQUENCY;
			btnBack.setIcon(Resources.getScaledIcon(Resources.iArrowDoubleLeft, 24));
			btnNext.setIcon(Resources.getScaledIcon(Resources.iButtonApply, 24));	
		}else if (panel.equals(INTERVALS)){
			centerCardLayout.show(centerPanel, INTERVALS);						
			currentPanel = INTERVALS;
			btnBack.setIcon(Resources.getScaledIcon(Resources.iArrowDoubleLeft, 24));
			btnNext.setIcon(Resources.getScaledIcon(Resources.iButtonApply, 24));	
		}else if (panel.equals(SAME_CODON)){
			centerCardLayout.show(centerPanel, SAME_CODON);						
			currentPanel = SAME_CODON;
			btnBack.setIcon(Resources.getScaledIcon(Resources.iArrowDoubleLeft, 24));
			btnNext.setIcon(Resources.getScaledIcon(Resources.iButtonApply, 24));	
		}else if (panel.equals(MULTIPLE_NUCLEOTIDES_POLYMORPHISMS)){
			centerCardLayout.show(centerPanel, MULTIPLE_NUCLEOTIDES_POLYMORPHISMS);						
			currentPanel = MULTIPLE_NUCLEOTIDES_POLYMORPHISMS;
			btnBack.setIcon(Resources.getScaledIcon(Resources.iArrowDoubleLeft, 24));
			btnNext.setIcon(Resources.getScaledIcon(Resources.iButtonApply, 24));	
		}else if (panel.equals(COMBINED_HETEROZYGOUS_VARIANTS)){
			centerCardLayout.show(centerPanel, COMBINED_HETEROZYGOUS_VARIANTS);						
			currentPanel = COMBINED_HETEROZYGOUS_VARIANTS;
			btnBack.setIcon(Resources.getScaledIcon(Resources.iArrowDoubleLeft, 24));
			btnNext.setIcon(Resources.getScaledIcon(Resources.iButtonApply, 24));	
		}else if (panel.equals(FILTER_SELECTION)){
			centerCardLayout.show(centerPanel, FILTER_SELECTION);
			currentPanel = FILTER_SELECTION;
			btnBack.setIcon(Resources.getScaledIcon(Resources.iCross, 24));
			btnNext.setIcon(Resources.getScaledIcon(Resources.iArrowDoubleRight, 24));
		}
	}

	private void setAvailableFilters(){
		int y = 0;
		filterSelectionPanel.add(getSelectionPanelVariantIntersection(), 
				new GridBagConstraints(0, y++, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		filterSelectionPanel.add(getSelectionPanelVariantComplement(), 
				new GridBagConstraints(0, y++, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		filterSelectionPanel.add(getSelectionPanelGeneCommon(), 
				new GridBagConstraints(0, y++, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		filterSelectionPanel.add(getSelectionPanelCombinedHeterozygous(), 
				new GridBagConstraints(0, y++, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		filterSelectionPanel.add(getSelectionPanelPathologyFrequency(), 
				new GridBagConstraints(0, y++, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		filterSelectionPanel.add(getSelectionPanelIntervals(), 
				new GridBagConstraints(0, y++, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		filterSelectionPanel.add(getSelectionPanelSameCodon(), 
				new GridBagConstraints(0, y++, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		filterSelectionPanel.add(getSelectionPanelMNV(), 
				new GridBagConstraints(0, y++, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		filterSelectionPanel.add(new JPanel(), 
				new GridBagConstraints(0, y++, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));

	}

	private JPanel getSelectionPanelVariantIntersection(){
		JPanel panel_selection = new JPanel();
		panel_selection.setBorder(BorderFactory.createEtchedBorder());
		panel_selection.setLayout(new BorderLayout(0, 0));

		JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		panel_selection.add(north, BorderLayout.NORTH);
		selectionVariantIntersection = new JRadioButton(VARIANTS_COMMON_TO_SAMPLES);
		selectionVariantIntersection.setSelected(true);
		selectionButtonGroup.add(selectionVariantIntersection);
		north.add(selectionVariantIntersection);

		JPanel panel_description = new JPanel(new FlowLayout(FlowLayout.LEFT,20,5));
		panel_selection.add(panel_description, BorderLayout.SOUTH);

		JTextArea txtpnKeepsOnlyThe = new JTextArea();
		txtpnKeepsOnlyThe.setFont(selectionVariantIntersection.getFont());
		txtpnKeepsOnlyThe.setText(FilterType.VARIANTS_COMMON_TO_SAMPLES.getDescription());
		txtpnKeepsOnlyThe.setAlignmentY(Component.TOP_ALIGNMENT);
		txtpnKeepsOnlyThe.setAlignmentX(0.0f);
		txtpnKeepsOnlyThe.setBorder(null);
		txtpnKeepsOnlyThe.setBackground(new Color(UIManager.getColor("control").getRGB()));
		txtpnKeepsOnlyThe.setEditable(false);
		panel_description.add(txtpnKeepsOnlyThe);

		JPanel panel_separtion = new JPanel();
		panel_selection.add(panel_separtion, BorderLayout.CENTER);
		GridBagLayout gbl_panel_separtion_1 = new GridBagLayout();
		panel_separtion.setLayout(gbl_panel_separtion_1);

		JPanel panel_line = new JPanel();
		panel_line.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		panel_line.setPreferredSize(new Dimension(50, 2));
		GridBagConstraints gbc_panel_line = new GridBagConstraints();
		gbc_panel_line.insets = new Insets(0, 20, 0, 20);
		gbc_panel_line.weightx = 1.0;
		gbc_panel_line.fill = GridBagConstraints.HORIZONTAL;
		gbc_panel_line.gridx = 0;
		gbc_panel_line.gridy = 0;
		panel_separtion.add(panel_line, gbc_panel_line);

		return panel_selection;
	}

	private JPanel getSelectionPanelVariantComplement(){
		JPanel panel_selection = new JPanel();
		panel_selection.setBorder(BorderFactory.createEtchedBorder());
		panel_selection.setLayout(new BorderLayout(0, 0));

		JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		panel_selection.add(north, BorderLayout.NORTH);
		selectionVariantComplement = new JRadioButton(SAMPLE_SPECIFIC_VARIANTS);
		selectionVariantComplement.setSelected(true);
		selectionButtonGroup.add(selectionVariantComplement);
		north.add(selectionVariantComplement);

		JPanel panel_description = new JPanel(new FlowLayout(FlowLayout.LEFT,20,5));
		panel_selection.add(panel_description, BorderLayout.SOUTH);

		JTextArea txtpnDescription = new JTextArea();
		txtpnDescription.setFont(selectionVariantComplement.getFont());
		txtpnDescription.setText(FilterType.SAMPLE_SPECIFIC_VARIANTS.getDescription());
		txtpnDescription.setAlignmentY(Component.TOP_ALIGNMENT);
		txtpnDescription.setAlignmentX(0.0f);
		txtpnDescription.setBorder(null);
		txtpnDescription.setBackground(new Color(UIManager.getColor("control").getRGB()));
		txtpnDescription.setEditable(false);
		panel_description.add(txtpnDescription);

		JPanel panel_separtion = new JPanel();
		panel_selection.add(panel_separtion, BorderLayout.CENTER);
		GridBagLayout gbl_panel_separtion_1 = new GridBagLayout();
		panel_separtion.setLayout(gbl_panel_separtion_1);

		JPanel panel_line = new JPanel();
		panel_line.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		panel_line.setPreferredSize(new Dimension(50, 2));
		GridBagConstraints gbc_panel_line = new GridBagConstraints();
		gbc_panel_line.insets = new Insets(0, 20, 0, 20);
		gbc_panel_line.weightx = 1.0;
		gbc_panel_line.fill = GridBagConstraints.HORIZONTAL;
		gbc_panel_line.gridx = 0;
		gbc_panel_line.gridy = 0;
		panel_separtion.add(panel_line, gbc_panel_line);

		return panel_selection;
	}

	private JPanel getSelectionPanelGeneCommon(){
		JPanel panel_selection = new JPanel();
		panel_selection.setBorder(BorderFactory.createEtchedBorder());
		panel_selection.setLayout(new BorderLayout(0, 0));

		JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		panel_selection.add(north, BorderLayout.NORTH);
		selectionGeneCommon = new JRadioButton(COMMON_GENE_VARIANTS);
		selectionGeneCommon.setSelected(true);
		selectionButtonGroup.add(selectionGeneCommon);
		north.add(selectionGeneCommon);

		JPanel panel_description = new JPanel(new FlowLayout(FlowLayout.LEFT,20,5));
		panel_selection.add(panel_description, BorderLayout.SOUTH);

		JTextArea txtpnKeepsOnlyThe = new JTextArea();
		txtpnKeepsOnlyThe.setFont(selectionGeneCommon.getFont());
		txtpnKeepsOnlyThe.setText(FilterType.COMMON_GENE_VARIANTS.getDescription());
		txtpnKeepsOnlyThe.setAlignmentY(Component.TOP_ALIGNMENT);
		txtpnKeepsOnlyThe.setAlignmentX(0.0f);
		txtpnKeepsOnlyThe.setBorder(null);
		txtpnKeepsOnlyThe.setBackground(new Color(UIManager.getColor("control").getRGB()));
		txtpnKeepsOnlyThe.setEditable(false);
		panel_description.add(txtpnKeepsOnlyThe);

		JPanel panel_separtion = new JPanel();
		panel_selection.add(panel_separtion, BorderLayout.CENTER);
		GridBagLayout gbl_panel_separtion_1 = new GridBagLayout();
		panel_separtion.setLayout(gbl_panel_separtion_1);

		JPanel panel_line = new JPanel();
		panel_line.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		panel_line.setPreferredSize(new Dimension(50, 2));
		GridBagConstraints gbc_panel_line = new GridBagConstraints();
		gbc_panel_line.insets = new Insets(0, 20, 0, 20);
		gbc_panel_line.weightx = 1.0;
		gbc_panel_line.fill = GridBagConstraints.HORIZONTAL;
		gbc_panel_line.gridx = 0;
		gbc_panel_line.gridy = 0;
		panel_separtion.add(panel_line, gbc_panel_line);

		return panel_selection;
	}

	private JPanel getSelectionPanelPathologyFrequency(){
		JPanel panel_selection = new JPanel();
		panel_selection.setBorder(BorderFactory.createEtchedBorder());
		panel_selection.setLayout(new BorderLayout(0, 0));

		JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		panel_selection.add(north, BorderLayout.NORTH);
		selectionPathologyFrequency = new JRadioButton(PATHOLOGY_FREQUENCY);
		selectionPathologyFrequency.setSelected(true);
		selectionButtonGroup.add(selectionPathologyFrequency);
		north.add(selectionPathologyFrequency);

		JPanel panel_description = new JPanel(new FlowLayout(FlowLayout.LEFT,20,5));
		panel_selection.add(panel_description, BorderLayout.SOUTH);

		JTextArea txtpnKeepsOnlyThe = new JTextArea();
		txtpnKeepsOnlyThe.setFont(selectionPathologyFrequency.getFont());
		txtpnKeepsOnlyThe.setText(FilterType.PATHOLOGY_FREQUENCY.getDescription());
		txtpnKeepsOnlyThe.setAlignmentY(Component.TOP_ALIGNMENT);
		txtpnKeepsOnlyThe.setAlignmentX(0.0f);
		txtpnKeepsOnlyThe.setBorder(null);
		txtpnKeepsOnlyThe.setBackground(new Color(UIManager.getColor("control").getRGB()));
		txtpnKeepsOnlyThe.setEditable(false);
		panel_description.add(txtpnKeepsOnlyThe);

		JPanel panel_separtion = new JPanel();
		panel_selection.add(panel_separtion, BorderLayout.CENTER);
		GridBagLayout gbl_panel_separtion_1 = new GridBagLayout();
		panel_separtion.setLayout(gbl_panel_separtion_1);

		JPanel panel_line = new JPanel();
		panel_line.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		panel_line.setPreferredSize(new Dimension(50, 2));
		GridBagConstraints gbc_panel_line = new GridBagConstraints();
		gbc_panel_line.insets = new Insets(0, 20, 0, 20);
		gbc_panel_line.weightx = 1.0;
		gbc_panel_line.fill = GridBagConstraints.HORIZONTAL;
		gbc_panel_line.gridx = 0;
		gbc_panel_line.gridy = 0;
		panel_separtion.add(panel_line, gbc_panel_line);

		return panel_selection;
	}
	
	private JPanel getSelectionPanelIntervals(){
		JPanel panel_selection = new JPanel();
		panel_selection.setBorder(BorderFactory.createEtchedBorder());
		panel_selection.setLayout(new BorderLayout(0, 0));

		JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		panel_selection.add(north, BorderLayout.NORTH);
		selectionIntervals = new JRadioButton(INTERVALS);
		selectionIntervals.setSelected(true);
		selectionButtonGroup.add(selectionIntervals);
		north.add(selectionIntervals);

		JPanel panel_description = new JPanel(new FlowLayout(FlowLayout.LEFT,20,5));
		panel_selection.add(panel_description, BorderLayout.SOUTH);

		JTextArea txtpnKeepsOnlyThe = new JTextArea();
		txtpnKeepsOnlyThe.setFont(selectionIntervals.getFont());
		txtpnKeepsOnlyThe.setText(FilterType.INTERVALS.getDescription());
		txtpnKeepsOnlyThe.setAlignmentY(Component.TOP_ALIGNMENT);
		txtpnKeepsOnlyThe.setAlignmentX(0.0f);
		txtpnKeepsOnlyThe.setBorder(null);
		txtpnKeepsOnlyThe.setBackground(new Color(UIManager.getColor("control").getRGB()));
		txtpnKeepsOnlyThe.setEditable(false);
		panel_description.add(txtpnKeepsOnlyThe);

		JPanel panel_separtion = new JPanel();
		panel_selection.add(panel_separtion, BorderLayout.CENTER);
		GridBagLayout gbl_panel_separtion_1 = new GridBagLayout();
		panel_separtion.setLayout(gbl_panel_separtion_1);

		JPanel panel_line = new JPanel();
		panel_line.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		panel_line.setPreferredSize(new Dimension(50, 2));
		GridBagConstraints gbc_panel_line = new GridBagConstraints();
		gbc_panel_line.insets = new Insets(0, 20, 0, 20);
		gbc_panel_line.weightx = 1.0;
		gbc_panel_line.fill = GridBagConstraints.HORIZONTAL;
		gbc_panel_line.gridx = 0;
		gbc_panel_line.gridy = 0;
		panel_separtion.add(panel_line, gbc_panel_line);

		return panel_selection;
	}

	private JPanel getSelectionPanelSameCodon(){
		JPanel panel_selection = new JPanel();
		panel_selection.setBorder(BorderFactory.createEtchedBorder());
		panel_selection.setLayout(new BorderLayout(0, 0));

		JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		panel_selection.add(north, BorderLayout.NORTH);
		selectionSameCodon = new JRadioButton(SAME_CODON);
		selectionSameCodon.setSelected(true);
		selectionButtonGroup.add(selectionSameCodon);
		north.add(selectionSameCodon);

		JPanel panel_description = new JPanel(new FlowLayout(FlowLayout.LEFT,20,5));
		panel_selection.add(panel_description, BorderLayout.SOUTH);

		JTextArea txtpnKeepsOnlyThe = new JTextArea();
		txtpnKeepsOnlyThe.setFont(selectionSameCodon.getFont());
		txtpnKeepsOnlyThe.setText(FilterType.SAME_CODON.getDescription());
		txtpnKeepsOnlyThe.setAlignmentY(Component.TOP_ALIGNMENT);
		txtpnKeepsOnlyThe.setAlignmentX(0.0f);
		txtpnKeepsOnlyThe.setBorder(null);
		txtpnKeepsOnlyThe.setBackground(new Color(UIManager.getColor("control").getRGB()));
		txtpnKeepsOnlyThe.setEditable(false);
		panel_description.add(txtpnKeepsOnlyThe);

		JPanel panel_separtion = new JPanel();
		panel_selection.add(panel_separtion, BorderLayout.CENTER);
		GridBagLayout gbl_panel_separtion_1 = new GridBagLayout();
		panel_separtion.setLayout(gbl_panel_separtion_1);

		JPanel panel_line = new JPanel();
		panel_line.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		panel_line.setPreferredSize(new Dimension(50, 2));
		GridBagConstraints gbc_panel_line = new GridBagConstraints();
		gbc_panel_line.insets = new Insets(0, 20, 0, 20);
		gbc_panel_line.weightx = 1.0;
		gbc_panel_line.fill = GridBagConstraints.HORIZONTAL;
		gbc_panel_line.gridx = 0;
		gbc_panel_line.gridy = 0;
		panel_separtion.add(panel_line, gbc_panel_line);

		return panel_selection;
	}

	private JPanel getSelectionPanelMNV(){
		JPanel panel_selection = new JPanel();
		panel_selection.setBorder(BorderFactory.createEtchedBorder());
		panel_selection.setLayout(new BorderLayout(0, 0));

		JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		panel_selection.add(north, BorderLayout.NORTH);
		selectionMNV = new JRadioButton(MULTIPLE_NUCLEOTIDES_POLYMORPHISMS);
		selectionMNV.setSelected(true);
		selectionButtonGroup.add(selectionMNV);
		north.add(selectionMNV);

		JPanel panel_description = new JPanel(new FlowLayout(FlowLayout.LEFT,20,5));
		panel_selection.add(panel_description, BorderLayout.SOUTH);

		JTextArea txtpnKeepsOnlyThe = new JTextArea();
		txtpnKeepsOnlyThe.setFont(selectionMNV.getFont());
		txtpnKeepsOnlyThe.setText(FilterType.MULTIPLE_NUCLEOTIDES_VARIANTS.getDescription());
		txtpnKeepsOnlyThe.setAlignmentY(Component.TOP_ALIGNMENT);
		txtpnKeepsOnlyThe.setAlignmentX(0.0f);
		txtpnKeepsOnlyThe.setBorder(null);
		txtpnKeepsOnlyThe.setBackground(new Color(UIManager.getColor("control").getRGB()));
		txtpnKeepsOnlyThe.setEditable(false);
		panel_description.add(txtpnKeepsOnlyThe);

		JPanel panel_separtion = new JPanel();
		panel_selection.add(panel_separtion, BorderLayout.CENTER);
		GridBagLayout gbl_panel_separtion_1 = new GridBagLayout();
		panel_separtion.setLayout(gbl_panel_separtion_1);

		JPanel panel_line = new JPanel();
		panel_line.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		panel_line.setPreferredSize(new Dimension(50, 2));
		GridBagConstraints gbc_panel_line = new GridBagConstraints();
		gbc_panel_line.insets = new Insets(0, 20, 0, 20);
		gbc_panel_line.weightx = 1.0;
		gbc_panel_line.fill = GridBagConstraints.HORIZONTAL;
		gbc_panel_line.gridx = 0;
		gbc_panel_line.gridy = 0;
		panel_separtion.add(panel_line, gbc_panel_line);

		return panel_selection;
	}

	private JPanel getSelectionPanelCombinedHeterozygous(){
		JPanel panel_selection = new JPanel();
		panel_selection.setBorder(BorderFactory.createEtchedBorder());
		panel_selection.setLayout(new BorderLayout(0, 0));

		JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
		panel_selection.add(north, BorderLayout.NORTH);
		selectionCombinedHeterozygous = new JRadioButton(COMBINED_HETEROZYGOUS_VARIANTS);
		selectionCombinedHeterozygous.setSelected(true);
		selectionButtonGroup.add(selectionCombinedHeterozygous);
		north.add(selectionCombinedHeterozygous);

		JPanel panel_description = new JPanel(new FlowLayout(FlowLayout.LEFT,20,5));
		panel_selection.add(panel_description, BorderLayout.SOUTH);

		JTextArea txtpnKeepsOnlyThe = new JTextArea();
		txtpnKeepsOnlyThe.setFont(selectionCombinedHeterozygous.getFont());
		txtpnKeepsOnlyThe.setText(FilterType.COMBINED_HETEROZYGOUS_VARIANTS.getDescription());
		txtpnKeepsOnlyThe.setAlignmentY(Component.TOP_ALIGNMENT);
		txtpnKeepsOnlyThe.setAlignmentX(0.0f);
		txtpnKeepsOnlyThe.setBorder(null);
		txtpnKeepsOnlyThe.setBackground(new Color(UIManager.getColor("control").getRGB()));
		txtpnKeepsOnlyThe.setEditable(false);
		panel_description.add(txtpnKeepsOnlyThe);

		JPanel panel_separtion = new JPanel();
		panel_selection.add(panel_separtion, BorderLayout.CENTER);
		GridBagLayout gbl_panel_separtion_1 = new GridBagLayout();
		panel_separtion.setLayout(gbl_panel_separtion_1);

		JPanel panel_line = new JPanel();
		panel_line.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		panel_line.setPreferredSize(new Dimension(50, 2));
		GridBagConstraints gbc_panel_line = new GridBagConstraints();
		gbc_panel_line.insets = new Insets(0, 20, 0, 20);
		gbc_panel_line.weightx = 1.0;
		gbc_panel_line.fill = GridBagConstraints.HORIZONTAL;
		gbc_panel_line.gridx = 0;
		gbc_panel_line.gridy = 0;
		panel_separtion.add(panel_line, gbc_panel_line);

		return panel_selection;
	}


	private class FieldCriterion extends JPanel {
		private String field;
		private JComboBox<String> zygozity;
		private JComboBox<String> compop;
		private JTextField val;

		public FieldCriterion(String field){
			this.field = field;
			setLayout(new FlowLayout(FlowLayout.LEADING));
			if (field.equalsIgnoreCase("zygosity")){
				zygozity = new JComboBox<>(new String[]{"Heterozygous","Homozygous","Any"});
				add(zygozity);
			}else{
				compop = new JComboBox<>(new String[]{
						ComparisonOperator.EQUAL.getUnicode(),
						ComparisonOperator.DIFFERENT.getUnicode(),
						ComparisonOperator.GREATER.getUnicode(),
						ComparisonOperator.GREATEROREQUAL.getUnicode(),
						ComparisonOperator.SMALLER.getUnicode(),
						ComparisonOperator.SMALLEROREQUAL.getUnicode(),
				});
				add(compop);
				val = new JTextField(2);
				add(val);
			}
		}

		public void setEnable(boolean enable){
			if (field.equalsIgnoreCase("zygosity")){
				zygozity.setEnabled(enable);
			}else{
				compop.setEnabled(enable);
				val.setEnabled(enable);
			}
		}

		public String getField(){
			return field;
		}

		public ComparisonOperator getOperator(){
			if (compop.getSelectedItem() != null){
				String op = compop.getSelectedItem().toString();
				for (ComparisonOperator cop : ComparisonOperator.values()){
					if (op.equals(cop.getUnicode())) return cop;
				}
			}
			return null;
		}

		public void setComparisonOperator(ComparisonOperator op){
			compop.setSelectedItem(op.getUnicode());
		}

		public String getValue(){
			if (field.equalsIgnoreCase("zygosity")){
				return zygozity.getSelectedItem().toString();
			}else{
				return val.getText();
			}
		}

		public void setValue(String value){
			if (field.equalsIgnoreCase("zygosity")){
				zygozity.setSelectedItem(value);
			}else{
				val.setText(value);
			}
		}
	}

	private JPanel getPanelPreFiltering(IndependentFilteringPanel filter){
		JPanel panel_prefilter = new JPanel(new BorderLayout());
		Border outterBorder = BorderFactory.createEmptyBorder(10,10,10,10);
		Border innerBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "PreFiltering");
		Border compoundBorder = BorderFactory.createCompoundBorder(outterBorder, innerBorder);
		panel_prefilter.setToolTipText("<html><b>Only variants obtained from this pre-filter will be taken into account in the magic filter.</b><br>"
				+ "If possible, you should include all the samples from the magic filter for better performances.<br>"
				+ "If your pre-filter is larger than the window and blue arrows don't appear to scroll, resize a little bit the window !</html>");
		panel_prefilter.setBorder(compoundBorder);
		panel_prefilter.add(filter, BorderLayout.CENTER);
		return panel_prefilter;
	}

	private JPanel getPanelVariantIntersection(){
		JPanel panel = new JPanel(new BorderLayout(5,5));

		panel.add(getPanelPreFiltering(varIntPreFilter), BorderLayout.NORTH);

		panel_varint_main = new JPanel(new GridBagLayout());
		Border outterBorder = BorderFactory.createEmptyBorder(10,10,10,10);
		Border innerBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), VARIANTS_COMMON_TO_SAMPLES);
		Border compoundBorder = BorderFactory.createCompoundBorder(outterBorder, innerBorder);
		panel_varint_main.setBorder(compoundBorder);
		panel.add(panel_varint_main, BorderLayout.CENTER);

		panel_varint_main.setLayout(new BorderLayout());	

		JPanel topPanel = new JPanel(new GridBagLayout());
		JPanel panelHead = new JPanel();
		panelHead.setBackground(Resources.rowHeadBackground);
		topPanel.add(panelHead, new GridBagConstraints(2, 2, 1, 1, 1.0, 1.0
				,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		BoxLayout layout = new BoxLayout(panelHead, BoxLayout.X_AXIS); 
		panelHead.setLayout(layout);
		JLabel lblColHead = new JLabel(" Samples");
		lblColHead.setForeground(Resources.rowHeadForeground);
		//lblColHead.setVerticalAlignment(JLabel.BOTTOM);
		lblColHead.setPreferredSize(new Dimension(150, 25));
		lblColHead.setMaximumSize(new Dimension(150, 25));
		panelHead.add(lblColHead);
		for (int k=0 ; k < varIntCols ; k++){
			JCheckBox fieldChkBox = new JCheckBox(varIntFields[k]);
			varIntChkBoxes.put(varIntFields[k], fieldChkBox);
			fieldChkBox.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					for (List<FieldCriterion> list : varIntValues.values()){
						for (FieldCriterion crit : list){
							if (crit.getField().equals(((JCheckBox)e.getSource()).getText())){
								crit.setEnable(((JCheckBox)e.getSource()).isSelected());
							}
						}
					}
				}
			});
			fieldChkBox.setForeground(Resources.rowHeadForeground);
			fieldChkBox.setPreferredSize(new Dimension(120, 25));
			fieldChkBox.setMaximumSize(new Dimension(120, 25));
			fieldChkBox.setHorizontalAlignment(SwingConstants.LEADING);
			panelHead.add(Box.createHorizontalGlue());	
			panelHead.add(fieldChkBox);			
		}

		varIntSubPanel = new JPanel(new GridBagLayout());
		varIntSubPanel.add(new JPanel(), new GridBagConstraints(0, Short.MAX_VALUE, 1, 1, 1.0, 1.0
				,GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
		
		if (filter == null){
			addSampleInVariantIntersection(null);		
			addSampleInVariantIntersection(null);
		}

		panel_varint_main.add(topPanel, BorderLayout.NORTH);
		JScrollPane scrollPane = new JScrollPane(varIntSubPanel);
		panel_varint_main.add(scrollPane, BorderLayout.CENTER);



		JPanel panel_add = new JPanel(new GridBagLayout());
		panel.add(panel_add, BorderLayout.SOUTH);
		JButton button_add = new JButton(Resources.getScaledIcon(Resources.iFaintPlus, 24));
		button_add.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				addSampleInVariantIntersection(null);
			}
		});
		GridBagConstraints gbc_button_frz_add = new GridBagConstraints();
		gbc_button_frz_add.fill = GridBagConstraints.HORIZONTAL;
		gbc_button_frz_add.weighty = 1.0;
		gbc_button_frz_add.weightx = 1.0;
		gbc_button_frz_add.insets = new Insets(5, 5, 5, 5);
		gbc_button_frz_add.gridx = 0;
		gbc_button_frz_add.gridy = 0;
		gbc_button_frz_add.anchor = GridBagConstraints.NORTHWEST;
		panel_add.add(button_add, gbc_button_frz_add);

		return panel;
	}

	private JComboBox<String> addSampleInVariantIntersection(final String selectedSample){
		final JComboBox<String> sampleBox = new JComboBox<>(availableSamples);
		varIntRow++;
		JPanel panelSample = new JPanel();
		BoxLayout boxLayout = new BoxLayout(panelSample, BoxLayout.X_AXIS);
		panelSample.setLayout(boxLayout);
		if (varIntRow%2 == 0) panelSample.setBackground(Resources.getTableEvenRowBackgroundColor(Palette.Orange));
		else panelSample.setBackground(Resources.getTableOddRowBackgroundColor(Palette.Orange));
		varIntSubPanel.add(panelSample, new GridBagConstraints(0, (varIntRow), 1, 1, 1.0, 0.0
				,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				AutoCompleteSupport<String> support = AutoCompleteSupport.install(sampleBox, GlazedLists.eventListOf(availableSamples));
				support.setCorrectsCase(true);
				support.setFilterMode(TextMatcherEditor.CONTAINS);
				support.setTextMatchingStrategy(TextMatcherEditor.NORMALIZED_STRATEGY);
				support.setStrict(false);
				sampleBox.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						if (arg0.getActionCommand().equals("comboBoxEdited")){
							if (sampleBox.getSelectedIndex() < 0) sampleBox.setSelectedItem(null);
						}
					}
				});
				if (selectedSample != null) sampleBox.setSelectedItem(selectedSample);
			}
		});
		JPanel sampleFrame = new JPanel(new FlowLayout(FlowLayout.LEADING));
		if (varIntRow%2 == 0) sampleFrame.setBackground(Resources.getTableEvenRowBackgroundColor(Palette.Orange));
		else sampleFrame.setBackground(Resources.getTableOddRowBackgroundColor(Palette.Orange));
		sampleFrame.setPreferredSize(new Dimension(150, 25));
		sampleFrame.setMaximumSize(new Dimension(150, 35));
		sampleBox.setPreferredSize(new Dimension(140, 25));
		sampleBox.setMaximumSize(new Dimension(140, 35));
		sampleFrame.add(sampleBox);
		panelSample.add(sampleFrame);
		varIntValues.put(sampleBox, new ArrayList<CreateMagicFilter.FieldCriterion>());

		for (int j=0 ; j < varIntCols ; j++){
			FieldCriterion crit = new FieldCriterion(varIntFields[j]);
			crit.setEnable(varIntChkBoxes.get(varIntFields[j]).isSelected());
			crit.setBackground((varIntRow%2 == 0)?Resources.getTableEvenRowBackgroundColor(Palette.Orange):Resources.getTableOddRowBackgroundColor(Palette.Orange));
			crit.setPreferredSize(new Dimension(120, 35));
			crit.setMaximumSize(new Dimension(120, 35));
			panelSample.add(Box.createHorizontalGlue());				
			panelSample.add(crit);				
			varIntValues.get(sampleBox).add(crit);
		}		
		return sampleBox;
	}

	private JPanel getPanelVariantComplement(){
		final JPanel panel = new JPanel(new BorderLayout(5,5));

		panel.add(getPanelPreFiltering(varCompPreFilter), BorderLayout.NORTH);

		panel_varcomp_main = new JPanel(new GridBagLayout());
		Border outterBorder = BorderFactory.createEmptyBorder(10,10,10,10);
		Border innerBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), SAMPLE_SPECIFIC_VARIANTS);
		Border compoundBorder = BorderFactory.createCompoundBorder(outterBorder, innerBorder);
		panel_varcomp_main.setBorder(null);
		JPanel fillPanel = new JPanel(new FlowLayout());
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.insets = new Insets(0, 0, 0, 0);
		gbc_panel.weighty = 1.0;
		gbc_panel.fill = GridBagConstraints.HORIZONTAL;
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 100;
		panel_varcomp_main.add(fillPanel, gbc_panel);
		JScrollPane scroll = new JScrollPane(panel_varcomp_main);
		scroll.setBorder(compoundBorder);
		panel.add(scroll, BorderLayout.CENTER);

		if (filter == null){
			addCritInVariantComplement(null, null, null);
		}

		JPanel panel_add = new JPanel(new GridBagLayout());
		panel.add(panel_add, BorderLayout.SOUTH);
		JButton button_add = new JButton(Resources.getScaledIcon(Resources.iFaintPlus, 24));
		button_add.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				addCritInVariantComplement(null, null, null);
				panel.validate();
			}
		});
		GridBagConstraints gbc_button_add = new GridBagConstraints();
		gbc_button_add.fill = GridBagConstraints.HORIZONTAL;
		gbc_button_add.weighty = 1.0;
		gbc_button_add.weightx = 1.0;
		gbc_button_add.insets = new Insets(5, 5, 5, 5);
		gbc_button_add.gridx = 0;
		gbc_button_add.gridy = 0;
		gbc_button_add.anchor = GridBagConstraints.NORTHWEST;
		panel_add.add(button_add, gbc_button_add);

		return panel;
	}

	private void addCritInVariantComplement(List<String> toKeep, List<String> toExclude, LogicalOperator toKeepOperator){
		JPanel panel = new JPanel(new GridBagLayout());
		if (varCompNumCrit == 0){
			panel.setBorder(BorderFactory.createTitledBorder("Criterion " + (varCompNumCrit+1)));
		}else{
			panel.setBorder(BorderFactory.createTitledBorder("OR Criterion " + (varCompNumCrit+1)));
		}
		final int critIndex = varCompNumCrit;
		final JPanel panel_tokeep = new JPanel(new BorderLayout());
		final DefaultListModel<String> listToKeepModel = new DefaultListModel<>();
		final JList<String> listToKeep = new JList<String>(listToKeepModel);
		listToKeep.setVisibleRowCount(6);
		JScrollPane scrollToKeep = new JScrollPane(listToKeep);
		panel_tokeep.add(scrollToKeep, BorderLayout.CENTER);
		JPanel panelTopToKeep = new JPanel(new BorderLayout());		
		panel_tokeep.add(panelTopToKeep, BorderLayout.NORTH);
		final JButton addSampleToKeep = new JButton("Select sample(s) with variants to KEEP", Resources.getScaledIcon(Resources.i3dPlus, 24));
		addSampleToKeep.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Set<String> existingSamples = new TreeSet<>();
				Enumeration<?> e = listToKeepModel.elements();
				while (e.hasMoreElements()){
					existingSamples.add((String) e.nextElement());
				}
				AskSamplesDialog apd = new AskSamplesDialog(false, Highlander.getCurrentAnalysis(), existingSamples);
				Tools.centerWindow(apd, false);
				apd.setVisible(true);
				if (apd.getSelection() != null){
					listToKeepModel.clear();
					for (String sample : apd.getSelection()){
						listToKeepModel.addElement(sample);
					}
				}
			}
		});
		panelTopToKeep.add(addSampleToKeep, BorderLayout.CENTER);
		final JToggleButton joinSamplesButton = new JToggleButton(Resources.getScaledIcon(Resources.iFilterOr, 24));
		joinSamplesButton.setToolTipText("Union or Intersection of sample variants");
		joinSamplesButton.setSelectedIcon(Resources.getScaledIcon(Resources.iFilterAnd, 24));
		varCompLogicalOperator.put(critIndex, LogicalOperator.OR);
		joinSamplesButton.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (joinSamplesButton.isSelected()){
					varCompLogicalOperator.put(critIndex, LogicalOperator.AND);
				}else{
					varCompLogicalOperator.put(critIndex, LogicalOperator.OR);
				}
			}
		});
		panelTopToKeep.add(joinSamplesButton, BorderLayout.EAST);
		if (toKeepOperator != null){
			varCompLogicalOperator.put(critIndex, toKeepOperator);
			if (toKeepOperator == LogicalOperator.AND){
				joinSamplesButton.setSelected(true);
			}			
		}
		panel.add(panel_tokeep, new GridBagConstraints(0, 0, 1, 1, 1.0, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
		if (toKeep != null){
			for (int i=0 ; i < toKeep.size() ; i++){
				listToKeepModel.addElement(toKeep.get(i));
			}
		}

		final JPanel panel_toexclude = new JPanel(new BorderLayout());
		final DefaultListModel<String> listToExcludeModel = new DefaultListModel<>();
		final JList<String> listToExclude = new JList<String>(listToExcludeModel);
		listToExclude.setVisibleRowCount(6);
		JScrollPane scrollToExclude = new JScrollPane(listToExclude);
		panel_toexclude.add(scrollToExclude, BorderLayout.CENTER);
		JPanel panelTopToExclude = new JPanel(new BorderLayout());		
		panel_toexclude.add(panelTopToExclude, BorderLayout.NORTH);
		final JButton addSampleToExclude = new JButton("Select sample(s) with variants to EXCLUDE", Resources.getScaledIcon(Resources.i3dPlus, 24));
		addSampleToExclude.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Set<String> existingSamples = new TreeSet<>();
				Enumeration<?> e = listToExcludeModel.elements();
				while (e.hasMoreElements()){
					existingSamples.add((String) e.nextElement());
				}
				AskSamplesDialog apd = new AskSamplesDialog(false, Highlander.getCurrentAnalysis(), existingSamples);
				Tools.centerWindow(apd, false);
				apd.setVisible(true);
				if (apd.getSelection() != null){
					listToExcludeModel.clear();
					for (String sample : apd.getSelection()){
						listToExcludeModel.addElement(sample);
					}
				}
			}
		});
		panelTopToExclude.add(addSampleToExclude, BorderLayout.CENTER);
		panel.add(panel_toexclude, new GridBagConstraints(1, 0, 1, 1, 1.0, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
		if (toExclude != null){
			for (int i=0 ; i < toExclude.size() ; i++){
				listToExcludeModel.addElement(toExclude.get(i));
			}
		}

		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.insets = new Insets(5, 5, 5, 5);
		gbc_panel.weightx = 1.0;
		gbc_panel.fill = GridBagConstraints.HORIZONTAL;
		gbc_panel.gridx = 0;
		gbc_panel.gridy = varCompNumCrit++;
		panel_varcomp_main.add(panel, gbc_panel);		
		panel_varcomp_main.validate();
	}

	private JPanel getPanelGeneCommon(){
		JPanel panel = new JPanel(new BorderLayout(5,5));

		panel.add(getPanelPreFiltering(geneComPreFilter), BorderLayout.NORTH);

		panel_genecom_main = new JPanel(new GridBagLayout());
		Border outterBorderM = BorderFactory.createEmptyBorder(10,10,10,10);
		Border innerBorderM = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), COMMON_GENE_VARIANTS);
		Border compoundBorderM = BorderFactory.createCompoundBorder(outterBorderM, innerBorderM);
		panel_genecom_main.setBorder(null);
		/*
		JPanel fillPanel = new JPanel();
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.insets = new Insets(0, 0, 0, 0);
		gbc_panel.weighty = 1.0;
		gbc_panel.fill = GridBagConstraints.HORIZONTAL;
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 100;
		panel_genecom_main.add(fillPanel, gbc_panel);
		 */
		JScrollPane scroll = new JScrollPane(panel_genecom_main);
		scroll.setBorder(compoundBorderM);
		panel.add(scroll, BorderLayout.CENTER);

		JPanel geneComMinCommonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
		JLabel geneComMinCommonLabel = new JLabel("Keep common-gene variants present in at least  ");
		geneComMinCommonPanel.add(geneComMinCommonLabel);
		geneComMinCommon = new JSpinner(new SpinnerNumberModel(geneComNumSample, Math.min(1, geneComNumSample), geneComNumSample, 1));
		((JSpinner.DefaultEditor) geneComMinCommon.getEditor()).getTextField().setColumns(4);
		geneComMinCommonPanel.add(geneComMinCommon);
		JLabel geneComMinCommonLabel2 = new JLabel("  given samples");
		geneComMinCommonPanel.add(geneComMinCommonLabel2);
		panel_genecom_main.add(geneComMinCommonPanel, 
				new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

		JPanel geneComMinVariantsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
		JLabel geneComMinVariantsLabel = new JLabel("Keep common-gene variants only if the gene has ");
		geneComMinVariantsPanel.add(geneComMinVariantsLabel);
		geneComNumVariantsThreshold.setSelectedItem(NumVariantThreshold.AT_LEAST.getText());
		geneComMinVariantsPanel.add(geneComNumVariantsThreshold);
		geneComMinMaxVariants = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
		((JSpinner.DefaultEditor) geneComMinMaxVariants.getEditor()).getTextField().setColumns(4);
		geneComMinVariantsPanel.add(geneComMinMaxVariants);
		JLabel geneComMinVariantsLabel2 = new JLabel("  variants in each sample");
		geneComMinVariantsPanel.add(geneComMinVariantsLabel2);
		panel_genecom_main.add(geneComMinVariantsPanel, 
				new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

		final JList<String> listSamples = new JList<String>(geneComSampleListModel);
		JScrollPane scrollSamples = new JScrollPane(listSamples);
		panel_genecom_main.add(scrollSamples, new GridBagConstraints(0, 3, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));

		JButton button_select = new JButton("Sample selection dialog", Resources.getScaledIcon(Resources.i3dPlus, 24));
		button_select.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Set<String> existingSamples = new TreeSet<>();
				Enumeration<?> e = geneComSampleListModel.elements();
				while (e.hasMoreElements()){
					existingSamples.add((String) e.nextElement());
				}
				AskSamplesDialog apd = new AskSamplesDialog(false, Highlander.getCurrentAnalysis(), existingSamples);
				Tools.centerWindow(apd, false);
				apd.setVisible(true);
				if (apd.getSelection() != null){
					geneComSampleListModel.clear();
					for (String sample : apd.getSelection()){
						geneComSampleListModel.addElement(sample);
					}
					geneComNumSample = apd.getSelection().size();
					((SpinnerNumberModel)geneComMinCommon.getModel()).setMinimum(Math.min(1, geneComNumSample));
					((SpinnerNumberModel)geneComMinCommon.getModel()).setMaximum(geneComNumSample);
					((SpinnerNumberModel)geneComMinCommon.getModel()).setValue(geneComNumSample);		
				}
			}
		});
		panel_genecom_main.add(button_select, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));

		return panel;
	}

	private JPanel getPanelPathologyFrequency(){
		JPanel panel = new JPanel(new BorderLayout(5,5));
		
		panel.add(getPanelPreFiltering(freqpatPreFilter), BorderLayout.NORTH);
		
		panel_freqpat_main = new JPanel(new GridBagLayout());
		Border outterBorderM = BorderFactory.createEmptyBorder(10,10,10,10);
		Border innerBorderM = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), PATHOLOGY_FREQUENCY);
		Border compoundBorderM = BorderFactory.createCompoundBorder(outterBorderM, innerBorderM);
		panel_freqpat_main.setBorder(null);
		
		JScrollPane scroll = new JScrollPane(panel_freqpat_main);
		scroll.setBorder(compoundBorderM);
		panel.add(scroll, BorderLayout.CENTER);
		
		JPanel freqpatPathologyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
		JLabel freqpatMinCommonLabel = new JLabel("Pathology");
		freqpatPathologyPanel.add(freqpatMinCommonLabel);
		freqpatPathology = new JComboBox<String>(availableSamples);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {				
				freqpatPathology = new JComboBox<String>(availablePathologies);
				AutoCompleteSupport<String> support = AutoCompleteSupport.install(freqpatPathology, GlazedLists.eventListOf(availablePathologies));
				support.setCorrectsCase(true);
				support.setFilterMode(TextMatcherEditor.CONTAINS);
				support.setTextMatchingStrategy(TextMatcherEditor.NORMALIZED_STRATEGY);
				support.setStrict(false);
				freqpatPathology.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						if (e.getActionCommand().equals("comboBoxEdited")){
							JComboBox<?> sampleBox = (JComboBox<?>)e.getSource();
							if (sampleBox.getSelectedIndex() < 0) sampleBox.setSelectedItem(null);
						}
					}
				});
				freqpatPathologyPanel.add(freqpatPathology);
			}
		});
		panel_freqpat_main.add(freqpatPathologyPanel, 
				new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		
		JPanel freqpatFieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
		JLabel freqpatMinVariantsLabel = new JLabel("Criterion for selected pathology: ");
		freqpatFieldPanel.add(freqpatMinVariantsLabel);
		freqpatField = new JComboBox<Field>(new Field[] {
				new Field("local_af"),
				new Field("local_an"),
				new Field("local_het"),
				new Field("local_hom"),
				new Field("germline_af"),
				new Field("germline_an"),
				new Field("germline_het"),
				new Field("germline_hom"),
				new Field("somatic_af"),
				new Field("somatic_an"),
				new Field("somatic_het"),
				new Field("somatic_hom"),
				});
		freqpatFieldPanel.add(freqpatField);
		freqpatComparisonOperator = new JComboBox<ComparisonOperator>(new ComparisonOperator[] {
				ComparisonOperator.SMALLER,
				ComparisonOperator.SMALLEROREQUAL,
				ComparisonOperator.EQUAL,
				ComparisonOperator.GREATEROREQUAL,
				ComparisonOperator.GREATER,
		});
		freqpatFieldPanel.add(freqpatComparisonOperator);
		freqpatValue.setColumns(10);
		freqpatFieldPanel.add(freqpatValue);
		panel_freqpat_main.add(freqpatFieldPanel, 
				new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		
		final JList<String> listSamples = new JList<String>(freqpatSampleListModel);
		JScrollPane scrollSamples = new JScrollPane(listSamples);
		panel_freqpat_main.add(scrollSamples, new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
		
		JButton button_select = new JButton("Sample selection dialog", Resources.getScaledIcon(Resources.i3dPlus, 24));
		button_select.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Set<String> existingSamples = new TreeSet<>();
				Enumeration<?> e = freqpatSampleListModel.elements();
				while (e.hasMoreElements()){
					existingSamples.add((String) e.nextElement());
				}
				AskSamplesDialog apd = new AskSamplesDialog(false, Highlander.getCurrentAnalysis(), existingSamples);
				Tools.centerWindow(apd, false);
				apd.setVisible(true);
				if (apd.getSelection() != null){
					freqpatSampleListModel.clear();
					for (String sample : apd.getSelection()){
						freqpatSampleListModel.addElement(sample);
					}
				}
			}
		});
		panel_freqpat_main.add(button_select, new GridBagConstraints(0, 3, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		
		return panel;
	}
	
	private JPanel getPanelIntervals(){
		JPanel panel = new JPanel(new BorderLayout());

		panel.add(getPanelPreFiltering(intervalPreFilter), BorderLayout.NORTH);

		JPanel filterPanel = new JPanel(new BorderLayout());
		panel.add(filterPanel, BorderLayout.CENTER);

		JPanel main = new JPanel(new GridLayout(1, 2, 5, 5));
		filterPanel.add(main, BorderLayout.CENTER);

		JPanel inoutPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
		ButtonGroup inoutgroup = new ButtonGroup();
		intervals_inside = new JRadioButton("Keep variant INSIDE the intervals");
		inoutgroup.add(intervals_inside);
		inoutPanel.add(intervals_inside);
		intervals_outside = new JRadioButton("Keep variant OUTSIDE the intervals");
		inoutgroup.add(intervals_outside);
		inoutPanel.add(intervals_outside);
		intervals_inside.setSelected(true);
		filterPanel.add(inoutPanel, BorderLayout.NORTH);

		JPanel panelIntervals = new JPanel(new BorderLayout(5,5));

		text_intervals_intervals = new JTextArea();
		text_intervals_intervals.setToolTipText("Enter intervals in the format chr:start-stop or chr:pos, values must be separated by ';' or be on different lines. e.g. 15:1562448;X:2450000-2460000");
		text_intervals_intervals.setLineWrap(true);
		text_intervals_intervals.setWrapStyleWord(true);

		Border outterBorder = BorderFactory.createEmptyBorder(10,10,10,10);
		Border innerBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), INTERVALS);
		Border compoundBorder = BorderFactory.createCompoundBorder(outterBorder, innerBorder);

		JScrollPane scrollPane = new JScrollPane(text_intervals_intervals);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setViewportView(text_intervals_intervals);
		scrollPane.setBorder(compoundBorder);
		panelIntervals.add(scrollPane, BorderLayout.CENTER);

		JButton button_select = new JButton("Import intervals from profile or file", Resources.getScaledIcon(Resources.i3dPlus, 24));
		button_select.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				AskListOfIntervalsDialog apd = new AskListOfIntervalsDialog(Highlander.getCurrentAnalysis().getReference());
				Tools.centerWindow(apd, false);
				apd.setVisible(true);
				for (Interval interval : apd.getSelection()){
					text_intervals_intervals.append(((text_intervals_intervals.getText().length() > 0)?";":"")+interval);					
				}
			}
		});
		panelIntervals.add(button_select, BorderLayout.SOUTH);

		JPanel panelSamples = new JPanel(new BorderLayout(5,5));

		Border outterBorder2 = BorderFactory.createEmptyBorder(10,10,10,10);
		Border innerBorder2 = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Samples");
		Border compoundBorder2 = BorderFactory.createCompoundBorder(outterBorder2, innerBorder2);
		final JList<String> listSamples = new JList<String>(intervals_samples);
		JScrollPane scroll = new JScrollPane(listSamples);
		scroll.setBorder(compoundBorder2);
		panelSamples.add(scroll, BorderLayout.CENTER);

		JButton button_select_2 = new JButton("Sample selection dialog", Resources.getScaledIcon(Resources.i3dPlus, 24));
		button_select_2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Set<String> existingSamples = new TreeSet<>();
				Enumeration<?> e = intervals_samples.elements();
				while (e.hasMoreElements()){
					existingSamples.add((String) e.nextElement());
				}
				AskSamplesDialog apd = new AskSamplesDialog(false, Highlander.getCurrentAnalysis(), existingSamples);
				Tools.centerWindow(apd, false);
				apd.setVisible(true);
				if (apd.getSelection() != null){
					intervals_samples.clear();
					for (String sample : apd.getSelection()){
						intervals_samples.addElement(sample);
					}
				}
			}
		});
		GridBagConstraints gbc_button_select_2 = new GridBagConstraints();
		gbc_button_select_2.fill = GridBagConstraints.HORIZONTAL;
		gbc_button_select_2.weighty = 0.0;
		gbc_button_select_2.weightx = 1.0;
		gbc_button_select_2.insets = new Insets(5, 5, 5, 5);
		gbc_button_select_2.gridx = 0;
		gbc_button_select_2.gridy = 1;
		gbc_button_select_2.anchor = GridBagConstraints.NORTHWEST;
		panelSamples.add(button_select_2, BorderLayout.SOUTH);


		main.add(panelIntervals);
		main.add(panelSamples);

		return panel;
	}

	private JPanel getPanelSameCodon(){
		JPanel panel = new JPanel(new BorderLayout(5,5));

		panel.add(getPanelPreFiltering(sameCodonPreFilter), BorderLayout.NORTH);

		JPanel main = new JPanel(new BorderLayout());
		Border outterBorder = BorderFactory.createEmptyBorder(10,10,10,10);
		Border innerBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), SAME_CODON);
		Border compoundBorder = BorderFactory.createCompoundBorder(outterBorder, innerBorder);
		main.setBorder(compoundBorder);

		final JList<String> listSamples = new JList<String>(same_codon_samples);
		JScrollPane scrollSamples = new JScrollPane(listSamples);
		main.add(scrollSamples, BorderLayout.CENTER);
		panel.add(main, BorderLayout.CENTER);

		JButton button_select = new JButton("Sample selection dialog", Resources.getScaledIcon(Resources.i3dPlus, 24));
		button_select.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Set<String> existingSamples = new TreeSet<>();
				Enumeration<?> e = same_codon_samples.elements();
				while (e.hasMoreElements()){
					existingSamples.add((String) e.nextElement());
				}
				AskSamplesDialog apd = new AskSamplesDialog(false, Highlander.getCurrentAnalysis(), existingSamples);
				Tools.centerWindow(apd, false);
				apd.setVisible(true);
				if (apd.getSelection() != null){
					same_codon_samples.clear();
					for (String sample : apd.getSelection()){
						same_codon_samples.addElement(sample);
					}
				}
			}
		});
		main.add(button_select, BorderLayout.NORTH);

		return panel;
	}


	private JPanel getPanelMNV(){
		JPanel panel = new JPanel(new BorderLayout(5,5));

		panel.add(getPanelPreFiltering(mnpPreFilter), BorderLayout.NORTH);

		JPanel main = new JPanel(new BorderLayout());
		Border outterBorder = BorderFactory.createEmptyBorder(10,10,10,10);
		Border innerBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), MULTIPLE_NUCLEOTIDES_POLYMORPHISMS);
		Border compoundBorder = BorderFactory.createCompoundBorder(outterBorder, innerBorder);
		main.setBorder(compoundBorder);
		panel.add(main, BorderLayout.CENTER);

		final JList<String> listSamples = new JList<String>(mnp_samples);
		JScrollPane scrollSamples = new JScrollPane(listSamples);
		main.add(scrollSamples, BorderLayout.CENTER);

		JButton button_select = new JButton("Sample selection dialog", Resources.getScaledIcon(Resources.i3dPlus, 24));
		button_select.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Set<String> existingSamples = new TreeSet<>();
				Enumeration<?> e = mnp_samples.elements();
				while (e.hasMoreElements()){
					existingSamples.add((String) e.nextElement());
				}
				AskSamplesDialog apd = new AskSamplesDialog(false, Highlander.getCurrentAnalysis(), existingSamples);
				Tools.centerWindow(apd, false);
				apd.setVisible(true);
				if (apd.getSelection() != null){
					mnp_samples.clear();
					for (String sample : apd.getSelection()){
						mnp_samples.addElement(sample);
					}
				}
			}
		});
		main.add(button_select, BorderLayout.NORTH);

		return panel;
	}

	private JPanel getPanelCombinedHeterozygous(){
		JPanel panel = new JPanel(new BorderLayout(5,5));

		JPanel northPanel = new JPanel(new BorderLayout(5,5));
		panel.add(northPanel, BorderLayout.NORTH);

		northPanel.add(getPanelPreFiltering(combHetPreFilter), BorderLayout.NORTH);

		panel_combhet_main = new JPanel(new GridBagLayout());		
		Border outterBorderM = BorderFactory.createEmptyBorder(10,10,10,10);
		Border innerBorderM = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), COMBINED_HETEROZYGOUS_VARIANTS);
		Border compoundBorderM = BorderFactory.createCompoundBorder(outterBorderM, innerBorderM);
		panel_combhet_main.setBorder(compoundBorderM);
		northPanel.add(panel_combhet_main, BorderLayout.CENTER);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {				
				for (int i=0 ; i < combHetLabels.length ; i++){
					JPanel panel_ch = new JPanel(new FlowLayout(FlowLayout.LEFT));
					GridBagConstraints gbc_panel_ch = new GridBagConstraints();
					gbc_panel_ch.insets = new Insets(5, 5, 5, 5);
					gbc_panel_ch.weightx = 1.0;
					gbc_panel_ch.fill = GridBagConstraints.HORIZONTAL;
					gbc_panel_ch.gridx = 0;
					gbc_panel_ch.gridy = i;
					panel_combhet_main.add(panel_ch, gbc_panel_ch);
					panel_ch.add(new JLabel(combHetLabels[i]));
					combHetBoxes.add(new JComboBox<String>(availableSamples));
					AutoCompleteSupport<String> support = AutoCompleteSupport.install(combHetBoxes.get(i), GlazedLists.eventListOf(availableSamples));
					support.setCorrectsCase(true);
					support.setFilterMode(TextMatcherEditor.CONTAINS);
					support.setTextMatchingStrategy(TextMatcherEditor.NORMALIZED_STRATEGY);
					support.setStrict(false);
					combHetBoxes.get(i).addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							if (e.getActionCommand().equals("comboBoxEdited")){
								JComboBox<?> sampleBox = (JComboBox<?>)e.getSource();
								if (sampleBox.getSelectedIndex() < 0) sampleBox.setSelectedItem(null);
							}
						}
					});
					panel_ch.add(combHetBoxes.get(i));
				}
			}
		});
		return panel;
	}

	private void fillFields(){
		if (filter != null) {
			switch(filter.getFilterType()){
			case VARIANTS_COMMON_TO_SAMPLES:
				selectionVariantIntersection.setSelected(true);
				showPanel(VARIANTS_COMMON_TO_SAMPLES);					
				try{
					if (((VariantsCommonToSamples)filter).getPreFiltering() != null) varIntPreFilter.setFilter(((VariantsCommonToSamples)filter).getPreFiltering(), "");
				}catch(Exception ex){
					Tools.exception(ex);
					JOptionPane.showMessageDialog(CreateMagicFilter.this, Tools.getMessage("Can't restore prefiltering", ex), "Restoring fields",
							JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}
				List<VCSCriterion> criteria = ((VariantsCommonToSamples)filter).getCriteria();
				for (int i = 0 ; i < criteria.size() ; i++){
					final VCSCriterion crit = criteria.get(i);
					final JComboBox<String> sampleBox = addSampleInVariantIntersection(crit.sample);
					if (i == 0){
						varIntChkBoxes.get(varIntFields[0]).setSelected(crit.useZigosity);
						varIntChkBoxes.get(varIntFields[1]).setSelected(crit.useAllelicDepthRef);
						varIntChkBoxes.get(varIntFields[2]).setSelected(crit.useAllelicDepthAlt);
						varIntChkBoxes.get(varIntFields[3]).setSelected(crit.useAllelicDepthProportionRef);
						varIntChkBoxes.get(varIntFields[4]).setSelected(crit.useAllelicDepthProportionAlt);
					}
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							for (FieldCriterion fc : varIntValues.get(sampleBox)){
								if (fc.getField().equalsIgnoreCase(varIntFields[0]) && crit.useZigosity){
									fc.setValue(crit.zygosity.toString());
								}else if (fc.getField().equalsIgnoreCase(varIntFields[1]) && crit.useAllelicDepthRef){
									fc.setComparisonOperator(crit.opAllelicDepthRef);
									fc.setValue(""+crit.allelic_depth_ref);
								}else if (fc.getField().equalsIgnoreCase(varIntFields[2]) && crit.useAllelicDepthAlt){
									fc.setComparisonOperator(crit.opAllelicDepthAlt);
									fc.setValue(""+crit.allelic_depth_alt);
								}else if (fc.getField().equalsIgnoreCase(varIntFields[3]) && crit.useAllelicDepthProportionRef){
									fc.setComparisonOperator(crit.opAllelicDepthProportionRef);
									fc.setValue(""+crit.allelic_depth_proportion_ref);
								}else if (fc.getField().equalsIgnoreCase(varIntFields[4]) && crit.useAllelicDepthProportionAlt){
									fc.setComparisonOperator(crit.opAllelicDepthProportionAlt);
									fc.setValue(""+crit.allelic_depth_proportion_alt);
								}
							}
						}
					});
				}
				break;
			case SAMPLE_SPECIFIC_VARIANTS:
				selectionVariantComplement.setSelected(true);
				showPanel(SAMPLE_SPECIFIC_VARIANTS);
				try{
					if (((SampleSpecificVariants)filter).getPreFiltering() != null) varCompPreFilter.setFilter(((SampleSpecificVariants)filter).getPreFiltering(), "");
				}catch(Exception ex){
					Tools.exception(ex);
					JOptionPane.showMessageDialog(CreateMagicFilter.this, Tools.getMessage("Can't restore prefiltering", ex), "Restoring fields",
							JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}
				Map<List<String>, List<String>> samplesSp = ((SampleSpecificVariants)filter).getFilterSamples();
				Map<List<String>, LogicalOperator> log = ((SampleSpecificVariants)filter).getToKeepOperator();
				for (Entry<List<String>, List<String>> e : samplesSp.entrySet()){
					addCritInVariantComplement(e.getKey(), e.getValue(), log.get(e.getKey()));
				}
				break;
			case COMMON_GENE_VARIANTS:
				selectionGeneCommon.setSelected(true);
				showPanel(COMMON_GENE_VARIANTS);
				try{
					if (((CommonGeneVariants)filter).getPreFiltering() != null) geneComPreFilter.setFilter(((CommonGeneVariants)filter).getPreFiltering(), "");
				}catch(Exception ex){
					Tools.exception(ex);
					JOptionPane.showMessageDialog(CreateMagicFilter.this, Tools.getMessage("Can't restore prefiltering", ex), "Restoring fields",
							JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}
				Set<String> samples = ((CommonGeneVariants)filter).getIncludedSamples();
				geneComSampleListModel.clear();
				for (final String sample : samples){
					geneComSampleListModel.addElement(sample);					
				}
				geneComNumSample = samples.size();
				((SpinnerNumberModel)geneComMinCommon.getModel()).setMinimum(Math.min(1, geneComNumSample));
				((SpinnerNumberModel)geneComMinCommon.getModel()).setMaximum(geneComNumSample);
				geneComMinCommon.setValue(((CommonGeneVariants)filter).getMinCommon());
				geneComNumVariantsThreshold.setSelectedItem((((CommonGeneVariants)filter).getNumVariantsThreshold().getText()));
				geneComMinMaxVariants.setValue(((CommonGeneVariants)filter).getMinMaxVariants());
				break;
			case PATHOLOGY_FREQUENCY:
				selectionPathologyFrequency.setSelected(true);
				showPanel(PATHOLOGY_FREQUENCY);
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						try{
							if (((PathologyFrequency)filter).getPreFiltering() != null) freqpatPreFilter.setFilter(((PathologyFrequency)filter).getPreFiltering(), "");
						}catch(Exception ex){
							Tools.exception(ex);
							JOptionPane.showMessageDialog(CreateMagicFilter.this, Tools.getMessage("Can't restore prefiltering", ex), "Restoring fields",
									JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
						}
						Set<String> freqpat_samples = ((PathologyFrequency)filter).getIncludedSamples();
						freqpatSampleListModel.clear();
						for (final String sample : freqpat_samples){
							freqpatSampleListModel.addElement(sample);					
						}
						freqpatPathology.setSelectedItem(((PathologyFrequency)filter).getPathology());
						freqpatField.setSelectedItem(((PathologyFrequency)filter).getField());
						freqpatComparisonOperator.setSelectedItem(((PathologyFrequency)filter).getComparisonOperator());
						freqpatValue.setText(((PathologyFrequency)filter).getValue());
					}
				});
				break;
			case INTERVALS:
				selectionIntervals.setSelected(true);
				showPanel(INTERVALS);
				try{
					if (((Intervals)filter).getPreFiltering() != null) intervalPreFilter.setFilter(((Intervals)filter).getPreFiltering(), "");
				}catch(Exception ex){
					Tools.exception(ex);
					JOptionPane.showMessageDialog(CreateMagicFilter.this, Tools.getMessage("Can't restore prefiltering", ex), "Restoring fields",
							JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}
				if (((Intervals)filter).isInside()){
					intervals_inside.setSelected(true);
				}else{
					intervals_outside.setSelected(true);
				}
				Set<Interval> intIntervals = ((Intervals)filter).getIntervals();
				for (final Interval interval : intIntervals){
					text_intervals_intervals.append(((text_intervals_intervals.getText().length() > 0)?";":"")+interval);					
				}
				Set<String> intSamples = ((Intervals)filter).getIncludedSamples();
				intervals_samples.clear();
				for (final String sample : intSamples){
					intervals_samples.addElement(sample);	
				}
				break;
			case SAME_CODON:
				selectionSameCodon.setSelected(true);
				showPanel(SAME_CODON);
				try{
					if (((SameCodon)filter).getPreFiltering() != null) sameCodonPreFilter.setFilter(((SameCodon)filter).getPreFiltering(), "");
				}catch(Exception ex){
					Tools.exception(ex);
					JOptionPane.showMessageDialog(CreateMagicFilter.this, Tools.getMessage("Can't restore prefiltering", ex), "Restoring fields",
							JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}
				Set<String> sameCodonSamples = ((SameCodon)filter).getIncludedSamples();
				same_codon_samples.clear();
				for (final String sample : sameCodonSamples){
					same_codon_samples.addElement(sample);					
				}
				break;
			case MULTIPLE_NUCLEOTIDES_VARIANTS:
				selectionMNV.setSelected(true);
				showPanel(MULTIPLE_NUCLEOTIDES_POLYMORPHISMS);
				try{
					if (((MultipleNucleotidesPolymorphisms)filter).getPreFiltering() != null) mnpPreFilter.setFilter(((MultipleNucleotidesPolymorphisms)filter).getPreFiltering(), "");
				}catch(Exception ex){
					Tools.exception(ex);
					JOptionPane.showMessageDialog(CreateMagicFilter.this, Tools.getMessage("Can't restore prefiltering", ex), "Restoring fields",
							JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}
				Set<String> mnpSamples = ((MultipleNucleotidesPolymorphisms)filter).getIncludedSamples();
				mnp_samples.clear();
				for (final String sample : mnpSamples){
					mnp_samples.addElement(sample);					
				}
				break;
			case COMBINED_HETEROZYGOUS_VARIANTS:
				selectionCombinedHeterozygous.setSelected(true);
				showPanel(COMBINED_HETEROZYGOUS_VARIANTS);				
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						try{
							if (((CombinedHeterozygousVariants)filter).getPreFiltering() != null) combHetPreFilter.setFilter(((CombinedHeterozygousVariants)filter).getPreFiltering(), "");
						}catch(Exception ex){
							Tools.exception(ex);
							JOptionPane.showMessageDialog(CreateMagicFilter.this, Tools.getMessage("Can't restore prefiltering", ex), "Restoring fields",
									JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
						}
						combHetBoxes.get(0).setSelectedItem(((CombinedHeterozygousVariants)filter).getChild());
						combHetBoxes.get(1).setSelectedItem(((CombinedHeterozygousVariants)filter).getFather());
						combHetBoxes.get(2).setSelectedItem(((CombinedHeterozygousVariants)filter).getMother());
					}
				});
				break;
			case CUSTOM:
				break;
			case COMBO:
				break;
			case LIST_OF_VARIANTS:
				break;
			}
		}
	}

	private void generateCriterion() throws Exception {
		if (selectionVariantIntersection.isSelected()){
			VariantsCommonToSamples vcs = new VariantsCommonToSamples();
			List<VCSCriterion> criteria = new ArrayList<VCSCriterion>();
			for (JComboBox<String> box : varIntValues.keySet()){
				if (box.getSelectedItem() != null){
					VCSCriterion crit = vcs.new VCSCriterion();
					crit.sample = box.getSelectedItem().toString();
					for (FieldCriterion fc : varIntValues.get(box)){
						if (fc.getField().equalsIgnoreCase(varIntFields[0])){
							if (varIntChkBoxes.get(varIntFields[0]).isSelected()){
								crit.useZigosity = true;
								crit.zygosity = fc.getValue();
							}
						}else if (fc.getField().equalsIgnoreCase(varIntFields[1])){
							if (varIntChkBoxes.get(varIntFields[1]).isSelected()){
								crit.useAllelicDepthRef = true;
								crit.opAllelicDepthRef = fc.getOperator();
								crit.allelic_depth_ref = Integer.parseInt(fc.getValue());
							}
						}else if (fc.getField().equalsIgnoreCase(varIntFields[2])){
							if (varIntChkBoxes.get(varIntFields[2]).isSelected()){
								crit.useAllelicDepthAlt = true;
								crit.opAllelicDepthAlt = fc.getOperator();
								crit.allelic_depth_alt = Integer.parseInt(fc.getValue());
							}
						}else if (fc.getField().equalsIgnoreCase(varIntFields[3])){
							if (varIntChkBoxes.get(varIntFields[3]).isSelected()){
								crit.useAllelicDepthProportionRef = true;
								crit.opAllelicDepthProportionRef = fc.getOperator();
								crit.allelic_depth_proportion_ref = Double.parseDouble(fc.getValue());
							}
						}else if (fc.getField().equalsIgnoreCase(varIntFields[4])){
							if (varIntChkBoxes.get(varIntFields[4]).isSelected()){
								crit.useAllelicDepthProportionAlt = true;
								crit.opAllelicDepthProportionAlt = fc.getOperator();
								crit.allelic_depth_proportion_alt = Double.parseDouble(fc.getValue());
							}
						}
					}
					criteria.add(crit);
				}
			}
			if (criteria.size() < 2) throw new Exception("You need to select at least 2 samples");
			filter = new VariantsCommonToSamples(filteringPanel, criteria, varIntPreFilter.getFilter());
		}else if (selectionVariantComplement.isSelected()){
			Map<List<String>, List<String>> samples = new LinkedHashMap<List<String>, List<String>>();
			Map<List<String>, LogicalOperator> op = new LinkedHashMap<List<String>, LogicalOperator>();
			int index = 0;
			for (Component p : panel_varcomp_main.getComponents()){
				if (p instanceof JPanel){
					if (((JPanel)p).getLayout() instanceof GridBagLayout){
						GridBagLayout gbl = (GridBagLayout)((JPanel)p).getLayout();
						List<String> keep = new ArrayList<String>();
						List<String> exclude = new ArrayList<String>();
						for (Component p2 : ((JPanel)p).getComponents()){
							if (gbl.getConstraints(p2).gridx == 0){
								for (Component p3 : ((JPanel)p2).getComponents()){
									if (p3 instanceof JScrollPane){
										JList<?> list = (JList<?>)((JScrollPane)p3).getViewport().getView();
										Enumeration<?> e = ((DefaultListModel<?>)list.getModel()).elements();
										while (e.hasMoreElements()){
											keep.add((String) e.nextElement());
										}
									}
								}
							}else{
								for (Component p3 : ((JPanel)p2).getComponents()){
									if (p3 instanceof JScrollPane){
										JList<?> list = (JList<?>)((JScrollPane)p3).getViewport().getView();
										Enumeration<?> e = ((DefaultListModel<?>)list.getModel()).elements();
										while (e.hasMoreElements()){
											exclude.add((String) e.nextElement());
										}
									}
								}
							}
						}
						if (!keep.isEmpty() && !exclude.isEmpty()){
							samples.put(keep, exclude);
							op.put(keep, varCompLogicalOperator.get(index));
						}
						index++;
					}
				}		
			}
			if (samples.size() == 0) throw new Exception("No samples selected !");			
			filter = new SampleSpecificVariants(filteringPanel, samples, op, varCompPreFilter.getFilter());
		}else if (selectionGeneCommon.isSelected()){
			Set<String> samples = getSamples();
			if (samples.isEmpty()) throw new Exception("You need to select at least 1 sample");
			filter = new CommonGeneVariants(filteringPanel, samples, (Integer)geneComMinCommon.getValue(), (Integer)geneComMinMaxVariants.getValue(), (NumVariantThreshold.getNumVariantThresholdFromText(geneComNumVariantsThreshold.getSelectedItem().toString())), geneComPreFilter.getFilter());
		}else if (selectionPathologyFrequency.isSelected()){
			Set<String> samples = getSamples();
			if (samples.isEmpty()) throw new Exception("You need to select at least 1 sample");
			String value = freqpatValue.getText();
			if (value.trim().length() > 0) {
				if (value.contains("%")){
					value = ""+(Double.parseDouble(value.replace('%', ' ').trim())/100.0);
				}
				value = value.toUpperCase().replace(",",".").trim();
			}else {
				value = "0";
			}
			if (freqpatPathology.getSelectedItem() == null && freqpatPathology.getSelectedItem().toString().length() == 0) throw new Exception("You need to select a pathology");
			String pathology = freqpatPathology.getSelectedItem().toString();
			filter = new PathologyFrequency(filteringPanel, samples, pathology, (Field)freqpatField.getSelectedItem(), (ComparisonOperator)freqpatComparisonOperator.getSelectedItem(), value, freqpatPreFilter.getFilter());
		}else if (selectionIntervals.isSelected()){
			Set<Interval> intervals = new LinkedHashSet<Interval>();
			String[] input = text_intervals_intervals.getText().replace('\n', ';').split(";");
			for (String string : input){
				try{
					intervals.add(new Interval(Highlander.getCurrentAnalysis().getReference(), string));
				}catch(NumberFormatException ex){
					throw new Exception("You must give a numeric positive value for start and end positions");
				}
			}
			if (intervals.isEmpty()) throw new Exception("You need to select at least 1 interval");
			Set<String> samples = getSamples();
			if (samples.isEmpty()) throw new Exception("You need to select at least 1 sample");
			boolean inside = intervals_inside.isSelected();
			filter = new Intervals(filteringPanel, intervals, samples, inside, intervalPreFilter.getFilter());
		}else if (selectionSameCodon.isSelected()){
			Set<String> samples = getSamples();
			if (samples.isEmpty()) throw new Exception("You need to select at least 1 sample");
			filter = new SameCodon(filteringPanel, samples, sameCodonPreFilter.getFilter());
		}else if (selectionMNV.isSelected()){
			Set<String> samples = getSamples();
			if (samples.isEmpty()) throw new Exception("You need to select at least 1 sample");
			filter = new MultipleNucleotidesPolymorphisms(filteringPanel, samples, mnpPreFilter.getFilter());
		}else if (selectionCombinedHeterozygous.isSelected()){
			for (JComboBox<?> box : combHetBoxes){
				if (box.getSelectedItem() == null){
					throw new Exception("You MUST select a sample id for each family member");
				}
			}
			filter = new CombinedHeterozygousVariants(filteringPanel, combHetBoxes.get(0).getSelectedItem().toString(), 
					combHetBoxes.get(1).getSelectedItem().toString(), combHetBoxes.get(2).getSelectedItem().toString(), 
					combHetPreFilter.getFilter());
		}
	}

	private Set<String> getSamples(){
		Set<String> samples = new LinkedHashSet<String>();
		Enumeration<?> e = null;
		if (selectionGeneCommon.isSelected()){
			e = geneComSampleListModel.elements();
		}else if (selectionPathologyFrequency.isSelected()){
			e = freqpatSampleListModel.elements();
		}else if (selectionIntervals.isSelected()){
			e = intervals_samples.elements();
		}else if (selectionSameCodon.isSelected()){
			e = same_codon_samples.elements();
		}else if (selectionMNV.isSelected()){
			e = mnp_samples.elements();
		}
		while (e.hasMoreElements()){
			samples.add((String) e.nextElement());
		}
		return samples;
	}

	public Filter getCriterion(){
		return filter;
	}

	//Overridden so we can exit when window is closed
	protected void processWindowEvent(WindowEvent e) {
		super.processWindowEvent(e);
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			cancelClose();
		}
	}

	private void cancelClose(){
		filter = null;
		dispose();  }

}
