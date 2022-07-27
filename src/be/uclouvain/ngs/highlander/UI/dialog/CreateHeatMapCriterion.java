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

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.UI.table.HeatMap.ColorRange;
import be.uclouvain.ngs.highlander.UI.table.HeatMap.ConversionMethod;
import be.uclouvain.ngs.highlander.UI.toolbar.HighlightingPanel;
import be.uclouvain.ngs.highlander.database.Category;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.HeatMapCriterion;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.util.List;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import javax.swing.SwingConstants;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.AutoCompleteSupport;

public class CreateHeatMapCriterion extends JDialog {
	
	private HeatMapCriterion criterion = null;
	private Analysis analysis;
	private HighlightingPanel highlighting;
	
	private final ButtonGroup colorRangeButtonGroup = new ButtonGroup();
	private final ButtonGroup conversionMethodButtonGroup = new ButtonGroup();
	private final ButtonGroup affectButtonGroup = new ButtonGroup();
	private JComboBox<String> boxCategories;
	private JComboBox<Field> comboBox_field;
	private JRadioButton radio_color_RGB_RG;
	private JRadioButton radio_color_RGB_GR;
	private JRadioButton radio_color_HSV_RB;
	private JRadioButton radio_color_HSV_BR;
	private JRadioButton radio_method_sorting;	
	private JRadioButton radio_method_range_data;	
	private JRadioButton radio_method_range_given;
	private JRadioButton radio_affect_column;
	private JRadioButton radio_affect_table;
	private JTextField text_method_range_minimum;
	private JTextField text_method_range_maximum;
	private EventList<Field> fields;
	
	AutoCompleteSupport<Field> support;
	
	public CreateHeatMapCriterion(Analysis analysis, HighlightingPanel highlightingPanel, HeatMapCriterion highlighting) {
		this.analysis = analysis;
		this.highlighting = highlightingPanel;
		this.criterion = highlighting;		
		initUI();		
		fillFields();
		pack();		
	}

	public CreateHeatMapCriterion(Analysis analysis, HighlightingPanel highlightingPanel) {
		this(analysis, highlightingPanel, null);
	}
	
	private void initUI(){
		setTitle("Create heat map");
		setIconImage(Resources.getScaledIcon(Resources.iHeatMap, 64).getImage());
		setModal(true);
		
		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.SOUTH);
		
		JButton btnCreate = new JButton(Resources.getScaledIcon(Resources.iButtonApply, 32));
		btnCreate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (generateCriterion()){
					dispose();
				}
			}
		});
		panel.add(btnCreate);
		
		JButton btnCancel = new JButton(Resources.getScaledIcon(Resources.iCross, 32));
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				cancelClose();
			}
		});
		panel.add(btnCancel);
		
		JScrollPane scroll = new JScrollPane();
		JPanel scrollPanel = new JPanel();
		GridBagLayout gblayout = new GridBagLayout();
		scrollPanel.setLayout(gblayout);
		JPanel mainPanel = new JPanel();
		GridBagLayout gbl_mainPanel = new GridBagLayout();
		gbl_mainPanel.columnWidths = new int[]{0, 0, 0};
		gbl_mainPanel.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0};
		gbl_mainPanel.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gbl_mainPanel.rowWeights = new double[]{0.0, 1.0, 1.0, 1.0, 1.0, 1.0, Double.MIN_VALUE};
		mainPanel.setLayout(gbl_mainPanel);
		
		JLabel lblCategory = new JLabel("Category");
		lblCategory.setHorizontalAlignment(SwingConstants.CENTER);
		GridBagConstraints gbc_lblCategory = new GridBagConstraints();
		gbc_lblCategory.anchor = GridBagConstraints.EAST;
		gbc_lblCategory.insets = new Insets(5, 5, 5, 10);
		gbc_lblCategory.gridx = 0;
		gbc_lblCategory.gridy = 0;
		mainPanel.add(lblCategory, gbc_lblCategory);
		
		boxCategories = new JComboBox<>(Category.getAvailableCategories(true,true));
		boxCategories.setMaximumRowCount(20);
		boxCategories.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							filterByCategory();							
						}
					});
				}
			}
		});
		GridBagConstraints gbc_boxCategories = new GridBagConstraints();
		gbc_boxCategories.insets = new Insets(5, 5, 5, 5);
		gbc_boxCategories.fill = GridBagConstraints.HORIZONTAL;
		gbc_boxCategories.gridx = 1;
		gbc_boxCategories.gridy = 0;
		mainPanel.add(boxCategories, gbc_boxCategories);
		
		JLabel lblField = new JLabel("Database field");
		GridBagConstraints gbc_lblField = new GridBagConstraints();
		gbc_lblField.anchor = GridBagConstraints.EAST;
		gbc_lblField.insets = new Insets(3, 5, 5, 10);
		gbc_lblField.gridx = 0;
		gbc_lblField.gridy = 1;
		mainPanel.add(lblField, gbc_lblField);
		
		JPanel panel_0 = new JPanel();
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.insets = new Insets(0, 0, 5, 0);
		gbc_panel.fill = GridBagConstraints.BOTH;
		gbc_panel.gridx = 1;
		gbc_panel.gridy = 1;
		mainPanel.add(panel_0, gbc_panel);
		GridBagLayout gbl_panel_0 = new GridBagLayout();
		panel_0.setLayout(gbl_panel_0);
		
		Field[] fieldsArr = Field.getAvailableFields(analysis, true).toArray(new Field[0]);
		fields = GlazedLists.eventListOf(fieldsArr);
		comboBox_field = new JComboBox<>(fieldsArr);
		comboBox_field.setMaximumRowCount(20);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				support = AutoCompleteSupport.install(comboBox_field, fields);
				support.setCorrectsCase(true);
				support.setFilterMode(TextMatcherEditor.CONTAINS);
				support.setTextMatchingStrategy(TextMatcherEditor.NORMALIZED_STRATEGY);
				support.setStrict(false);
			}
		});		
		comboBox_field.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (arg0.getActionCommand().equals("comboBoxEdited")){
					if (comboBox_field.getSelectedIndex() < 0) comboBox_field.setSelectedItem(null);
				}
				ComboboxToolTipRenderer renderer = new ComboboxToolTipRenderer();
				renderer.setTooltips(support.getItemList());
				comboBox_field.setRenderer(renderer);
			}
		});
		ComboboxToolTipRenderer renderer = new ComboboxToolTipRenderer();
		renderer.setTooltips(Field.getAvailableFields(analysis, true));
		comboBox_field.setRenderer(renderer);
		GridBagConstraints gbc_comboBox_field = new GridBagConstraints();
		gbc_comboBox_field.weightx = 1.0;
		gbc_comboBox_field.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBox_field.insets = new Insets(5, 5, 5, 5);
		gbc_comboBox_field.anchor = GridBagConstraints.NORTHWEST;
		gbc_comboBox_field.gridx = 0;
		gbc_comboBox_field.gridy = 0;
		panel_0.add(comboBox_field, gbc_comboBox_field);
		
		JLabel lblValues = new JLabel("Color range");
		GridBagConstraints gbc_lblValues = new GridBagConstraints();
		gbc_lblValues.anchor = GridBagConstraints.NORTHEAST;
		gbc_lblValues.insets = new Insets(3, 5, 5, 10);
		gbc_lblValues.gridx = 0;
		gbc_lblValues.gridy = 2;
		mainPanel.add(lblValues, gbc_lblValues);
		
		JPanel panel_1 = new JPanel();
		GridBagConstraints gbc_panel_1 = new GridBagConstraints();
		gbc_panel_1.insets = new Insets(0, 0, 5, 0);
		gbc_panel_1.weightx = 1.0;
		gbc_panel_1.fill = GridBagConstraints.BOTH;
		gbc_panel_1.gridx = 1;
		gbc_panel_1.gridy = 2;
		mainPanel.add(panel_1, gbc_panel_1);
		GridBagLayout gbl_panel_1 = new GridBagLayout();
		gbl_panel_1.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0};
		gbl_panel_1.columnWeights = new double[]{0.0, 0.0, 1.0, 0.0};
		panel_1.setLayout(gbl_panel_1);
		
		radio_color_RGB_RG = new JRadioButton();
		colorRangeButtonGroup.add(radio_color_RGB_RG);
		panel_1.add(radio_color_RGB_RG, new GridBagConstraints(0,0,1,1,0.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 5, 2, 0), 0, 0));
		panel_1.add(new JLabel("min"), new GridBagConstraints(1,0,1,1,0.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
		panel_1.add(ColorRange.RGB_RED_TO_GREEN.getExampleRange(), new GridBagConstraints(2,0,1,1,1.0,1.0,GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
		panel_1.add(new JLabel("max"), new GridBagConstraints(3,0,1,1,0.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 10), 0, 0));
		radio_color_RGB_RG.setSelected(true);
		
		radio_color_RGB_GR = new JRadioButton();
		colorRangeButtonGroup.add(radio_color_RGB_GR);
		panel_1.add(radio_color_RGB_GR, new GridBagConstraints(0,1,1,1,0.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 5, 2, 0), 0, 0));
		panel_1.add(new JLabel("min"), new GridBagConstraints(1,1,1,1,0.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
		panel_1.add(ColorRange.RGB_GREEN_TO_RED.getExampleRange(), new GridBagConstraints(2,1,1,1,1.0,1.0,GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
		panel_1.add(new JLabel("max"), new GridBagConstraints(3,1,1,1,0.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 10), 0, 0));
		
		radio_color_HSV_RB = new JRadioButton();
		colorRangeButtonGroup.add(radio_color_HSV_RB);
		panel_1.add(radio_color_HSV_RB, new GridBagConstraints(0,2,1,1,0.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 5, 2, 0), 0, 0));
		panel_1.add(new JLabel("min"), new GridBagConstraints(1,2,1,1,0.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
		panel_1.add(ColorRange.HSV_RED_TO_BLUE.getExampleRange(), new GridBagConstraints(2,2,1,1,1.0,1.0,GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
		panel_1.add(new JLabel("max"), new GridBagConstraints(3,2,1,1,0.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 10), 0, 0));
		
		radio_color_HSV_BR = new JRadioButton();
		colorRangeButtonGroup.add(radio_color_HSV_BR);
		panel_1.add(radio_color_HSV_BR, new GridBagConstraints(0,3,1,1,0.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 5, 2, 0), 0, 0));
		panel_1.add(new JLabel("min"), new GridBagConstraints(1,3,1,1,0.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
		panel_1.add(ColorRange.HSV_BLUE_TO_RED.getExampleRange(), new GridBagConstraints(2,3,1,1,1.0,1.0,GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
		panel_1.add(new JLabel("max"), new GridBagConstraints(3,3,1,1,0.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 10), 0, 0));
		
		JLabel lblIfAValue = new JLabel("Conversion method");
		GridBagConstraints gbc_lblIfAValue = new GridBagConstraints();
		gbc_lblIfAValue.anchor = GridBagConstraints.NORTHEAST;
		gbc_lblIfAValue.insets = new Insets(3, 5, 5, 10);
		gbc_lblIfAValue.gridx = 0;
		gbc_lblIfAValue.gridy = 3;
		mainPanel.add(lblIfAValue, gbc_lblIfAValue);
		
		JPanel panel_2 = new JPanel();
		GridBagConstraints gbc_panel_2 = new GridBagConstraints();
		gbc_panel_2.insets = new Insets(0, 0, 5, 0);
		gbc_panel_2.fill = GridBagConstraints.BOTH;
		gbc_panel_2.gridx = 1;
		gbc_panel_2.gridy = 3;
		mainPanel.add(panel_2, gbc_panel_2);
		GridBagLayout gbl_panel_2 = new GridBagLayout();
		gbl_panel_2.rowWeights = new double[]{0.0, 0.0, 0.0};
		gbl_panel_2.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 1.0};
		panel_2.setLayout(gbl_panel_2);
		
		radio_method_sorting = new JRadioButton("Position sorting");
		radio_method_sorting.setToolTipText(ConversionMethod.SORTING.getDefinition());
		conversionMethodButtonGroup.add(radio_method_sorting);
		panel_2.add(radio_method_sorting, new GridBagConstraints(0,0,5,1,0.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 5, 2, 10), 0, 0));
		radio_method_sorting.setSelected(true);
		
		radio_method_range_data = new JRadioButton("Estimated range");
		radio_method_range_data.setToolTipText(ConversionMethod.RANGE_DATA.getDefinition());
		conversionMethodButtonGroup.add(radio_method_range_data);
		panel_2.add(radio_method_range_data, new GridBagConstraints(0,1,5,1,0.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 5, 2, 10), 0, 0));

		radio_method_range_given = new JRadioButton("Given range: ");
		radio_method_range_given.setToolTipText(ConversionMethod.RANGE_GIVEN.getDefinition());
		conversionMethodButtonGroup.add(radio_method_range_given);
		text_method_range_minimum = new JTextField(5);
		text_method_range_maximum = new JTextField(5);
		panel_2.add(radio_method_range_given, new GridBagConstraints(0,2,1,1,0.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 2, 0), 0, 0));
		panel_2.add(new JLabel("min."), new GridBagConstraints(1,2,1,1,0.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 2, 0), 0, 0));
		panel_2.add(text_method_range_minimum, new GridBagConstraints(2,2,1,1,0.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 2, 0), 0, 0));
		panel_2.add(new JLabel("max."), new GridBagConstraints(3,2,1,1,0.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 2, 0), 0, 0));
		panel_2.add(text_method_range_maximum, new GridBagConstraints(4,2,1,1,0.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 2, 0), 0, 0));
		panel_2.add(new JPanel(), new GridBagConstraints(5,2,1,1,0.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 2, 10), 0, 0));
		
		JLabel lblColoredCells = new JLabel("Colored cells");
		GridBagConstraints gbc_lblColoredCells = new GridBagConstraints();
		gbc_lblColoredCells.anchor = GridBagConstraints.NORTHEAST;
		gbc_lblColoredCells.insets = new Insets(3, 5, 5, 10);
		gbc_lblColoredCells.gridx = 0;
		gbc_lblColoredCells.gridy = 4;
		mainPanel.add(lblColoredCells, gbc_lblColoredCells);
		
		JPanel panel_3 = new JPanel();
		GridBagConstraints gbc_panel_3 = new GridBagConstraints();
		gbc_panel_3.insets = new Insets(0, 0, 5, 0);
		gbc_panel_3.fill = GridBagConstraints.BOTH;
		gbc_panel_3.gridx = 1;
		gbc_panel_3.gridy = 4;
		mainPanel.add(panel_3, gbc_panel_3);
		GridBagLayout gbl_panel_3 = new GridBagLayout();
		gbl_panel_3.rowWeights = new double[]{0.0, 0.0, 0.0};
		gbl_panel_3.columnWeights = new double[]{0.0, 1.0};
		panel_3.setLayout(gbl_panel_3);
		
		radio_affect_column = new JRadioButton("Field's column only");
		radio_affect_column.setToolTipText("Only the selected column will be colored");
		affectButtonGroup.add(radio_affect_column);
		panel_3.add(radio_affect_column, new GridBagConstraints(0,0,5,1,0.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 5, 2, 10), 0, 0));
		radio_affect_column.setSelected(true);
		
		radio_affect_table = new JRadioButton("Expand to whole table");
		radio_affect_table.setToolTipText("The whole table will be colored depending on the value of the selected column");
		affectButtonGroup.add(radio_affect_table);
		panel_3.add(radio_affect_table, new GridBagConstraints(0,1,5,1,0.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 5, 2, 10), 0, 0));

		scrollPanel.add(mainPanel, new GridBagConstraints(0,0,1,1,1.0,0,GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		scrollPanel.add(new JPanel(), new GridBagConstraints(0,1,1,1,1.0,1.0,GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));		
		scroll.setViewportView(scrollPanel);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		getContentPane().add(scroll);
		
	}

	private void filterByCategory(){
		fields.clear();
		for (Field field : Field.getAvailableFields(analysis, true)){
			if (boxCategories.getSelectedItem().toString().equals("all available fields") || 
					field.getCategory().getName().equals(boxCategories.getSelectedItem().toString())){
				fields.add(field);
			}
		}
		ComboboxToolTipRenderer renderer = new ComboboxToolTipRenderer();
		renderer.setTooltips(fields);
		comboBox_field.setRenderer(renderer);
		comboBox_field.validate();
		comboBox_field.repaint();
	}

	public class ComboboxToolTipRenderer extends DefaultListCellRenderer {
		List<Field> tooltips;

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value,
                        int index, boolean isSelected, boolean cellHasFocus) {

        JComponent comp = (JComponent) super.getListCellRendererComponent(list,
                value, index, isSelected, cellHasFocus);

        if (-1 < index && null != value && null != tooltips) {
                    list.setToolTipText(tooltips.get(index).getHtmlTooltip());
                }
        return comp;
    }

    public void setTooltips(List<Field> tooltips) {
        this.tooltips = tooltips;
    }
	}
	
	private void fillFields(){
		if (criterion != null) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					comboBox_field.setSelectedItem(criterion.getField().getName());
					switch(criterion.getColorRange()){
					case HSV_BLUE_TO_RED:
						radio_color_HSV_BR.setSelected(true);
						break;
					case HSV_RED_TO_BLUE:
						radio_color_HSV_RB.setSelected(true);
						break;
					case RGB_GREEN_TO_RED:
						radio_color_RGB_GR.setSelected(true);
						break;
					case RGB_RED_TO_GREEN:
						radio_color_RGB_RG.setSelected(true);
						break;
					}
					switch(criterion.getConversionMethod()){
					case RANGE_DATA:
						radio_method_range_data.setSelected(true);
						break;
					case RANGE_GIVEN:
						radio_method_range_given.setSelected(true);
						text_method_range_minimum.setText(criterion.getMinimum());
						text_method_range_maximum.setText(criterion.getMaximum());
						break;
					case SORTING:
						radio_method_sorting.setSelected(true);
						break;
					}
					if (criterion.expandTable()) {
						radio_affect_table.setSelected(true);
					}else {
						radio_affect_table.setSelected(false);
					}
				}
			});
		}
	}
	
	private boolean generateCriterion(){		
		if (comboBox_field.getSelectedItem() == null){
			JOptionPane.showMessageDialog(this, "You must select a valid field", "Error in heat map", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			return false;
		}
		Field field = Field.getField(comboBox_field.getSelectedItem().toString());
		ColorRange color = null;
		if (radio_color_RGB_RG.isSelected()){
			color = ColorRange.RGB_RED_TO_GREEN;
		}else if (radio_color_RGB_GR.isSelected()){
			color = ColorRange.RGB_GREEN_TO_RED;				
		}else if (radio_color_HSV_BR.isSelected()){
			color = ColorRange.HSV_BLUE_TO_RED;				
		}else if (radio_color_HSV_RB.isSelected()){
			color = ColorRange.HSV_RED_TO_BLUE;				
		}
		ConversionMethod method = null;
		String minimum = "";
		String maximum = "";
		if (radio_method_sorting.isSelected()){
			method = ConversionMethod.SORTING;
		}else if (radio_method_range_data.isSelected()){
			method = ConversionMethod.RANGE_DATA;
		}else if (radio_method_range_given.isSelected()){
			method = ConversionMethod.RANGE_GIVEN;
			minimum = text_method_range_minimum.getText();
			maximum = text_method_range_maximum.getText();
		}
		boolean expand = radio_affect_table.isSelected();
		criterion = new HeatMapCriterion(highlighting, field, color, method, minimum, maximum, expand);
		return true;
	}
	
	public HeatMapCriterion getCriterion(){
		return criterion;
	}

	//Overridden so we can exit when window is closed
  protected void processWindowEvent(WindowEvent e) {
    super.processWindowEvent(e);
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
    	cancelClose();
    }
  }
  
  private void cancelClose(){
  	criterion = null;
		dispose(); 
	}

}
