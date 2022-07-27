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
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.dialog.ProfileTree.Action;
import be.uclouvain.ngs.highlander.UI.table.VariantsTable;
import be.uclouvain.ngs.highlander.UI.toolbar.FilteringPanel;
import be.uclouvain.ngs.highlander.administration.users.User.UserData;
import be.uclouvain.ngs.highlander.database.Category;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.filter.CustomFilter;
import be.uclouvain.ngs.highlander.datatype.filter.Filter;
import be.uclouvain.ngs.highlander.datatype.filter.CustomFilter.ComparisonOperator;

import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.AutoCompleteSupport;

public class CreateCustomFilter extends JDialog {

	private CustomFilter criterion = null;
	private Analysis analysis;
	private FilteringPanel filteringPanel;
	private final VariantsTable variantsTable;

	private final ButtonGroup nullButtonGroup = new ButtonGroup();
	private final ButtonGroup comparisonButtonGroup = new ButtonGroup();
	private JCheckBox chkBox_null = new JCheckBox();
	private JTextArea txtArea_values;
	private JTextArea txtArea_profile;
	private JRadioButton rdbtnExcludeFromCriterion;
	private JRadioButton rdbtnIncludeInCriterion;
	private JComboBox<String> boxCategories;
	private JComboBox<Field> comboBox_field;
	private EventList<Field> fields;
	private JRadioButton radioButton_doesnotcontains;
	private JRadioButton radioButton_contains;
	private JRadioButton radioButton_smallerequal;
	private JRadioButton radioButton_smaller;
	private JRadioButton radioButton_greaterequal;
	private JRadioButton radioButton_greater;
	private JRadioButton radioButton_different;
	private JRadioButton radioButton_equal;
	private JButton btnValueListFrom;
	private JButton btnFreeValues;
	private JButton btnPossibleValuesDatabase;
	private JButton btnPossibleValuesTable = new JButton("Possible values from your table",Resources.getScaledIcon(Resources.i3dPlus, 16));;

	AutoCompleteSupport<Field> support;

	public CreateCustomFilter(Analysis analysis, FilteringPanel filteringPanel, CustomFilter filter) {
		this.analysis = analysis;
		this.filteringPanel = filteringPanel;
		this.variantsTable = filteringPanel.getVariantsTable();
		this.criterion = filter;		
		initUI();		
		fillFields();
		pack();		
	}

	public CreateCustomFilter(Analysis analysis, FilteringPanel filteringPanel) {
		this(analysis, filteringPanel, null);
	}

	private void initUI(){
		setTitle("Create filtering criterion");
		setIconImage(Resources.getScaledIcon(Resources.iFilter, 64).getImage());
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
		comboBox_field.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					if (comboBox_field.getSelectedIndex() >= 0){
						setAvailableComparisonOperators();
						setAvailableValueLists();
					}
				}
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

		boxCategories = new JComboBox<>(Category.getAvailableCategories(true,true));
		boxCategories.setToolTipText("Restrict 'Database field' for easier searching");
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
		panel_0.add(boxCategories, gbc_boxCategories);


		JLabel lblValues = new JLabel("Comparison operator");
		GridBagConstraints gbc_lblValues = new GridBagConstraints();
		gbc_lblValues.anchor = GridBagConstraints.EAST;
		gbc_lblValues.insets = new Insets(3, 5, 5, 10);
		gbc_lblValues.gridx = 0;
		gbc_lblValues.gridy = 2;
		mainPanel.add(lblValues, gbc_lblValues);

		JPanel panel_1 = new JPanel();
		FlowLayout flowLayout_1 = (FlowLayout) panel_1.getLayout();
		flowLayout_1.setAlignment(FlowLayout.LEADING);
		flowLayout_1.setHgap(15);
		GridBagConstraints gbc_panel_1 = new GridBagConstraints();
		gbc_panel_1.insets = new Insets(0, 0, 5, 0);
		gbc_panel_1.weightx = 1.0;
		gbc_panel_1.fill = GridBagConstraints.BOTH;
		gbc_panel_1.gridx = 1;
		gbc_panel_1.gridy = 2;
		mainPanel.add(panel_1, gbc_panel_1);

		radioButton_equal = new JRadioButton(ComparisonOperator.EQUAL.getUnicode());
		radioButton_equal.setToolTipText("Exaclty equal to");
		radioButton_equal.setSelected(true);
		comparisonButtonGroup.add(radioButton_equal);
		panel_1.add(radioButton_equal);

		radioButton_different = new JRadioButton(ComparisonOperator.DIFFERENT.getUnicode());
		radioButton_different.setToolTipText("Different than");
		comparisonButtonGroup.add(radioButton_different);
		panel_1.add(radioButton_different);

		radioButton_greater = new JRadioButton(ComparisonOperator.GREATER.getUnicode());
		radioButton_greater.setToolTipText("Greater than");
		comparisonButtonGroup.add(radioButton_greater);
		panel_1.add(radioButton_greater);

		radioButton_greaterequal = new JRadioButton(ComparisonOperator.GREATEROREQUAL.getUnicode());
		radioButton_greaterequal.setToolTipText("Greater or equal than");
		comparisonButtonGroup.add(radioButton_greaterequal);
		panel_1.add(radioButton_greaterequal);

		radioButton_smaller = new JRadioButton(ComparisonOperator.SMALLER.getUnicode());
		radioButton_smaller.setToolTipText("Smaller than");
		comparisonButtonGroup.add(radioButton_smaller);
		panel_1.add(radioButton_smaller);

		radioButton_smallerequal = new JRadioButton(ComparisonOperator.SMALLEROREQUAL.getUnicode());
		radioButton_smallerequal.setToolTipText("Smaller or equal than");
		comparisonButtonGroup.add(radioButton_smallerequal);
		panel_1.add(radioButton_smallerequal);

		radioButton_contains = new JRadioButton(ComparisonOperator.CONTAINS.getUnicode());
		radioButton_contains.setToolTipText("Contains");
		comparisonButtonGroup.add(radioButton_contains);
		panel_1.add(radioButton_contains);

		radioButton_doesnotcontains = new JRadioButton(ComparisonOperator.DOESNOTCONTAINS.getUnicode());
		radioButton_doesnotcontains.setToolTipText("Does not contains");
		comparisonButtonGroup.add(radioButton_doesnotcontains);
		panel_1.add(radioButton_doesnotcontains);

		JLabel lblIfAValue = new JLabel("Value(s)");
		GridBagConstraints gbc_lblIfAValue = new GridBagConstraints();
		gbc_lblIfAValue.anchor = GridBagConstraints.NORTHEAST;
		gbc_lblIfAValue.insets = new Insets(3, 5, 5, 10);
		gbc_lblIfAValue.gridx = 0;
		gbc_lblIfAValue.gridy = 3;
		mainPanel.add(lblIfAValue, gbc_lblIfAValue);

		JPanel panel_2 = new JPanel();
		GridBagConstraints gbc_panel_2 = new GridBagConstraints();
		gbc_panel_2.insets = new Insets(0, 5, 5, 0);
		gbc_panel_2.fill = GridBagConstraints.BOTH;
		gbc_panel_2.gridx = 1;
		gbc_panel_2.gridy = 3;
		mainPanel.add(panel_2, gbc_panel_2);
		GridBagLayout gbl_panel_2 = new GridBagLayout();
		gbl_panel_2.rowWeights = new double[]{0.0, 1.0, 0.0, 1.0, 0.0};
		gbl_panel_2.columnWeights = new double[]{0.0, 0.0, 0.0, 1.0};
		panel_2.setLayout(gbl_panel_2);

		chkBox_null = new JCheckBox("No value");
		chkBox_null.setToolTipText("For example, you could ask for variants WITH ou WITHOUT a dbsnp_id.");
		chkBox_null.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent arg0) {
				fireEventNullSelected();
			}
		});
		GridBagConstraints gbc_chkBox_null = new GridBagConstraints();
		gbc_chkBox_null.gridwidth = 3;
		gbc_chkBox_null.anchor = GridBagConstraints.WEST;
		gbc_chkBox_null.insets = new Insets(0, 0, 10, 0);
		gbc_chkBox_null.gridx = 0;
		gbc_chkBox_null.gridy = 0;
		panel_2.add(chkBox_null, gbc_chkBox_null);

		btnPossibleValuesDatabase = new JButton("From database",Resources.getScaledIcon(Resources.i3dPlus, 16));
		btnPossibleValuesDatabase.setEnabled(false);
		btnPossibleValuesDatabase.setToolTipText("Choose among possible values found in the whole database");
		btnPossibleValuesDatabase.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (comboBox_field.getSelectedItem() == null){ 
					JOptionPane.showMessageDialog(CreateCustomFilter.this, "You must first select a field !", "Ask for possible values", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}else{
					List<String> existingValues = new ArrayList<String>();
					if(txtArea_values.getText().length() > 0){
						existingValues = Arrays.asList(txtArea_values.getText().split(";"));
					}
					AskListOfPossibleValuesDialog ask = new AskListOfPossibleValuesDialog(Field.getField(comboBox_field.getSelectedItem().toString()), null, hasSingleValue(), existingValues);
					Tools.centerWindow(ask, false);
					ask.setVisible(true);
					if (!ask.getSelection().isEmpty()){
						txtArea_values.setText("");
						for (String value : ask.getSelection()){
							txtArea_values.append(value+";");
						}
					}
				}
			}
		});
		GridBagConstraints gbc_btnPossibleValues = new GridBagConstraints();
		gbc_btnPossibleValues.insets = new Insets(0, 0, 5, 5);
		gbc_btnPossibleValues.gridx = 0;
		gbc_btnPossibleValues.gridy = 1;
		panel_2.add(btnPossibleValuesDatabase, gbc_btnPossibleValues);

		if (variantsTable != null){
			btnPossibleValuesTable = new JButton("From your table",Resources.getScaledIcon(Resources.i3dPlus, 16));
			btnPossibleValuesTable.setToolTipText("Choose among possible values found in the current table (i.e. applying your filters)");
			btnPossibleValuesTable.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					if (comboBox_field.getSelectedItem() == null){ 
						JOptionPane.showMessageDialog(CreateCustomFilter.this, "You must first select a field !", "Ask for possible values", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}else if (!variantsTable.hasColumn(Field.getField(comboBox_field.getSelectedItem().toString()))){
						JOptionPane.showMessageDialog(CreateCustomFilter.this, "The field '"+comboBox_field.getSelectedItem().toString()+"' is not present in your table.", "Ask for possible values", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}else{
						List<String> existingValues = new ArrayList<String>();
						if(txtArea_values.getText().length() > 0){
							existingValues = Arrays.asList(txtArea_values.getText().split(";"));
						}
						AskListOfPossibleValuesDialog ask = new AskListOfPossibleValuesDialog(Field.getField(comboBox_field.getSelectedItem().toString()), variantsTable, hasSingleValue(), existingValues);
						Tools.centerWindow(ask, false);
						ask.setVisible(true);
						if (!ask.getSelection().isEmpty()){
							txtArea_values.setText("");
							for (String value : ask.getSelection()){
								txtArea_values.append(value+";");
							}
						}
					}
				}
			});
			GridBagConstraints gbc_btnPossibleValuesFrom = new GridBagConstraints();
			gbc_btnPossibleValuesFrom.insets = new Insets(0, 0, 5, 5);
			gbc_btnPossibleValuesFrom.gridx = 1;
			gbc_btnPossibleValuesFrom.gridy = 1;
			panel_2.add(btnPossibleValuesTable, gbc_btnPossibleValuesFrom);
			if (variantsTable != null && variantsTable.isEmpty()) btnPossibleValuesTable.setEnabled(false);
		}

		btnFreeValues = new JButton("Encode",Resources.getScaledIcon(Resources.i3dPlus, 16));
		btnFreeValues.setToolTipText("Open a form to encode or import your values");
		btnFreeValues.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				List<String> existingValues = new ArrayList<String>();
				if(txtArea_values.getText().length() > 0){
					existingValues = Arrays.asList(txtArea_values.getText().split(";"));
				}
				Field field = Field.getField(comboBox_field.getSelectedItem().toString());
				AskListOfFreeValuesDialog ask = new AskListOfFreeValuesDialog(field, existingValues);
				Tools.centerWindow(ask, false);
				ask.setVisible(true);
				if (!ask.getSelection().isEmpty()){
					txtArea_values.setText("");
					for (String value : ask.getSelection()){
						txtArea_values.append(value+";");
					}
				}
			}
		});
		GridBagConstraints gbc_btnFreeValues = new GridBagConstraints();
		gbc_btnFreeValues.anchor = GridBagConstraints.WEST;
		gbc_btnFreeValues.insets = new Insets(0, 0, 5, 5);
		gbc_btnFreeValues.gridx = 2;
		gbc_btnFreeValues.gridy = 1;
		panel_2.add(btnFreeValues, gbc_btnFreeValues);

		panel_2.add(new JPanel(), new GridBagConstraints(3, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

		JScrollPane scrollPane_1 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_1 = new GridBagConstraints();
		gbc_scrollPane_1.gridwidth = 4;
		gbc_scrollPane_1.insets = new Insets(0, 0, 10, 5);
		gbc_scrollPane_1.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_1.gridx = 0;
		gbc_scrollPane_1.gridy = 2;
		panel_2.add(scrollPane_1, gbc_scrollPane_1);

		txtArea_values = new JTextArea();
		txtArea_values.setRows(4);
		txtArea_values.setLineWrap(true);
		txtArea_values.setWrapStyleWord(true);
		txtArea_values.setToolTipText("Case insensitive, values must be separated by ';'. If you want to search for a ';' in the database you must write preceded by a backslash '\\;'");
		scrollPane_1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane_1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane_1.setViewportView(txtArea_values);

		btnValueListFrom = new JButton("Value list from profile",Resources.getScaledIcon(Resources.i3dPlus, 16));
		btnValueListFrom.setEnabled(false);
		btnValueListFrom.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (comboBox_field.getSelectedItem() != null){
					String listname = ProfileTree.showProfileDialog(CreateCustomFilter.this, Action.LOAD, UserData.VALUES, comboBox_field.getSelectedItem().toString());
					if (listname != null){
						if(txtArea_profile.getText().length() > 0 && !txtArea_profile.getText().endsWith(";")){
							txtArea_profile.append(";");
						}
						txtArea_profile.append(listname+";");
					}
				}
			}
		});
		GridBagConstraints gbc_btnValueListFrom = new GridBagConstraints();
		gbc_btnValueListFrom.gridwidth = 4;
		gbc_btnValueListFrom.insets = new Insets(0, 0, 5, 0);
		gbc_btnValueListFrom.anchor = GridBagConstraints.WEST;
		gbc_btnValueListFrom.gridx = 0;
		gbc_btnValueListFrom.gridy = 3;
		panel_2.add(btnValueListFrom, gbc_btnValueListFrom);

		JScrollPane scrollPane_2 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_2 = new GridBagConstraints();
		gbc_scrollPane_2.gridwidth = 4;
		gbc_scrollPane_2.insets = new Insets(0, 0, 10, 5);
		gbc_scrollPane_2.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_2.gridy = 4;
		gbc_scrollPane_2.gridx = 0;
		panel_2.add(scrollPane_2, gbc_scrollPane_2);

		txtArea_profile = new JTextArea();
		txtArea_profile.setRows(2);
		txtArea_values.setLineWrap(true);
		txtArea_values.setWrapStyleWord(true);
		txtArea_profile.setToolTipText("Case insensitive, values must be separated by ';'. If you want to search for a ';' in the database you must write preceded by a backslash '\\;'");
		scrollPane_2.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane_2.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane_2.setViewportView(txtArea_profile);

		JLabel lblIfVariantHas = new JLabel("Variant without value");
		GridBagConstraints gbc_lblIfVariantHas = new GridBagConstraints();
		gbc_lblIfVariantHas.anchor = GridBagConstraints.EAST;
		gbc_lblIfVariantHas.insets = new Insets(3, 5, 5, 10);
		gbc_lblIfVariantHas.gridx = 0;
		gbc_lblIfVariantHas.gridy = 4;
		mainPanel.add(lblIfVariantHas, gbc_lblIfVariantHas);

		JPanel panel_3 = new JPanel();
		FlowLayout flowLayout = (FlowLayout) panel_3.getLayout();
		flowLayout.setAlignment(FlowLayout.LEADING);
		GridBagConstraints gbc_panel_3 = new GridBagConstraints();
		gbc_panel_3.insets = new Insets(0, 0, 5, 0);
		gbc_panel_3.fill = GridBagConstraints.BOTH;
		gbc_panel_3.gridx = 1;
		gbc_panel_3.gridy = 4;
		mainPanel.add(panel_3, gbc_panel_3);

		rdbtnIncludeInCriterion = new JRadioButton("include in results");
		rdbtnIncludeInCriterion.setToolTipText("If a variant has no value for the selected field, " +
				"it will be INCLUDED in the results, like it has 'passed' the criterion. " +
				"So if pph2_div_score field is selected and a variant has no Polyphen score (e.g. a variant creating a STOP), " +
				"it will be included in the results, whatever the comparison operator and value(s).");
		rdbtnIncludeInCriterion.setSelected(true);
		nullButtonGroup.add(rdbtnIncludeInCriterion);
		panel_3.add(rdbtnIncludeInCriterion);

		rdbtnExcludeFromCriterion = new JRadioButton("exclude from results");
		rdbtnExcludeFromCriterion.setToolTipText("If a variant has no value for the selected field, " +
				"it will be EXCLUDED from the results, like it has NOT 'passed' the criterion. " +
				"So if pph2_div_score field is selected and a variant has no Polyphen score (e.g. a variant creating a STOP), " +
				"it won't be included in the results, whatever the comparison operator and value(s).");
		nullButtonGroup.add(rdbtnExcludeFromCriterion);
		panel_3.add(rdbtnExcludeFromCriterion);

		scrollPanel.add(mainPanel, new GridBagConstraints(0,0,1,1,1.0,0,GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
		scrollPanel.add(new JPanel(), new GridBagConstraints(0,1,1,1,1.0,1.0,GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));		
		scroll.setViewportView(scrollPanel);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		getContentPane().add(scroll);

		radioButton_equal.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				if(arg0.getStateChange() == ItemEvent.SELECTED)
					setAvailableValueLists();
			}
		});
		radioButton_different.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				if(arg0.getStateChange() == ItemEvent.SELECTED)
					setAvailableValueLists();
			}
		});
		radioButton_greater.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				if(arg0.getStateChange() == ItemEvent.SELECTED)
					setAvailableValueLists();
			}
		});
		radioButton_greaterequal.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				if(arg0.getStateChange() == ItemEvent.SELECTED)
					setAvailableValueLists();
			}
		});
		radioButton_smaller.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				if(arg0.getStateChange() == ItemEvent.SELECTED)
					setAvailableValueLists();
			}
		});
		radioButton_smallerequal.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				if(arg0.getStateChange() == ItemEvent.SELECTED)
					setAvailableValueLists();
			}
		});
		radioButton_contains.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				if(arg0.getStateChange() == ItemEvent.SELECTED)
					setAvailableValueLists();
			}
		});
		radioButton_doesnotcontains.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				if(arg0.getStateChange() == ItemEvent.SELECTED)
					setAvailableValueLists();
			}
		});

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

	private void setAvailableComparisonOperators(){
		try{
			List<ComparisonOperator> availableOps = new ArrayList<CustomFilter.ComparisonOperator>();
			if (comboBox_field.getSelectedItem() != null){
				Field f = Field.getField(comboBox_field.getSelectedItem().toString());
				availableOps = f.getPossibleComparisonOperators();
				btnPossibleValuesDatabase.setEnabled(f.hasPossibleValues(Highlander.getDB(), analysis));
			}else{
				availableOps.add(ComparisonOperator.EQUAL);
				availableOps.add(ComparisonOperator.DIFFERENT);
				availableOps.add(ComparisonOperator.GREATER);
				availableOps.add(ComparisonOperator.GREATEROREQUAL);
				availableOps.add(ComparisonOperator.SMALLER);
				availableOps.add(ComparisonOperator.SMALLEROREQUAL);
				availableOps.add(ComparisonOperator.CONTAINS);
				availableOps.add(ComparisonOperator.DOESNOTCONTAINS);
				btnPossibleValuesDatabase.setEnabled(false);
			}
			if(chkBox_null.isSelected()){
				availableOps.remove(ComparisonOperator.GREATER);
				availableOps.remove(ComparisonOperator.GREATEROREQUAL);
				availableOps.remove(ComparisonOperator.SMALLER);
				availableOps.remove(ComparisonOperator.SMALLEROREQUAL);
				availableOps.remove(ComparisonOperator.CONTAINS);
				availableOps.remove(ComparisonOperator.DOESNOTCONTAINS);
				btnPossibleValuesDatabase.setEnabled(false);
			}
			radioButton_equal.setEnabled((availableOps.contains(ComparisonOperator.EQUAL)));
			radioButton_different.setEnabled((availableOps.contains(ComparisonOperator.DIFFERENT)));
			radioButton_greater.setEnabled((availableOps.contains(ComparisonOperator.GREATER)));
			radioButton_greaterequal.setEnabled((availableOps.contains(ComparisonOperator.GREATEROREQUAL)));
			radioButton_smaller.setEnabled((availableOps.contains(ComparisonOperator.SMALLER)));
			radioButton_smallerequal.setEnabled((availableOps.contains(ComparisonOperator.SMALLEROREQUAL)));
			radioButton_contains.setEnabled((availableOps.contains(ComparisonOperator.CONTAINS)));
			radioButton_doesnotcontains.setEnabled((availableOps.contains(ComparisonOperator.DOESNOTCONTAINS)));
			if (!availableOps.contains(getSelectedComparisonOperator())){
				switch(availableOps.get(0)){
				case EQUAL:
					radioButton_equal.setSelected(true);
					break;
				case DIFFERENT:
					radioButton_different.setSelected(true);
					break;
				case GREATER:
					radioButton_greater.setSelected(true);
					break;
				case GREATEROREQUAL:
					radioButton_greaterequal.setSelected(true);
					break;
				case SMALLER:
					radioButton_smaller.setSelected(true);
					break;
				case SMALLEROREQUAL:
					radioButton_smallerequal.setSelected(true);
					break;
				case CONTAINS:
					radioButton_contains.setSelected(true);
					break;
				case DOESNOTCONTAINS:
					radioButton_doesnotcontains.setSelected(true);
					break;
				default:
					throw new Exception("Comparison operator " + availableOps.get(0) + " not supported here");
				}
			}
		}catch(Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(CreateCustomFilter.this, Tools.getMessage("Error", ex), "Field selection", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
	}

	private void setAvailableValueLists(){
		if(chkBox_null.isSelected()){
			btnFreeValues.setEnabled(false);
			txtArea_values.setText("");
			txtArea_values.setEnabled(false);
			btnPossibleValuesDatabase.setEnabled(false);
			btnPossibleValuesTable.setEnabled(false);
			btnValueListFrom.setEnabled(false);
			txtArea_profile.setText("");			
			txtArea_profile.setEnabled(false);
			rdbtnIncludeInCriterion.setSelected(true);
			rdbtnIncludeInCriterion.setEnabled(false);
			rdbtnExcludeFromCriterion.setEnabled(false);
		}else{
			btnFreeValues.setEnabled(!hasSingleValue());
			txtArea_values.setEnabled(true);
			if (variantsTable != null && !variantsTable.isEmpty()) btnPossibleValuesTable.setEnabled(true);
			if (comboBox_field.getSelectedItem() != null){
				Field f = Field.getField(comboBox_field.getSelectedItem().toString());
				btnPossibleValuesDatabase.setEnabled(f.hasPossibleValues(Highlander.getDB(), analysis));
			}
			btnValueListFrom.setEnabled(!hasSingleValue() && (comboBox_field.getSelectedItem() != null));
			txtArea_profile.setEnabled(!hasSingleValue() && (comboBox_field.getSelectedItem() != null));
			if (hasSingleValue() ||  (comboBox_field.getSelectedItem() == null)) txtArea_profile.setText("");
			rdbtnIncludeInCriterion.setEnabled(true);
			rdbtnExcludeFromCriterion.setEnabled(true);
		}
	}

	private void fireEventNullSelected(){
		setAvailableComparisonOperators();
		setAvailableValueLists();
	}

	private void fillFields(){
		if (criterion != null) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					comboBox_field.setSelectedItem(criterion.getField().getName());
					switch(criterion.getComparisonOperator()){
					case CONTAINS:
						radioButton_contains.setSelected(true);
						break;
					case DIFFERENT:
						radioButton_different.setSelected(true);
						break;
					case DOESNOTCONTAINS:
						radioButton_doesnotcontains.setSelected(true);
						break;
					case EQUAL:
						radioButton_equal.setSelected(true);
						break;
					case GREATER:
						radioButton_greater.setSelected(true);
						break;
					case GREATEROREQUAL:
						radioButton_greaterequal.setSelected(true);
						break;
					case SMALLER:
						radioButton_smaller.setSelected(true);
						break;
					case SMALLEROREQUAL:
						radioButton_smallerequal.setSelected(true);
						break;
					default:
						System.err.println("Comparison operator " + criterion.getComparisonOperator() + " not supported here");
					}
					if (criterion.getNullValue()){
						chkBox_null.setSelected(true);
					}else{
						String vals = "";
						for (String val : criterion.getValues()) vals += val + ";";
						if (vals.endsWith(";")) vals = vals.substring(0, vals.length()-1);
						txtArea_values.setText(vals);
						vals = "";
						for (String val : criterion.getProfileValues()) vals += val + ";";
						if (vals.endsWith(";")) vals = vals.substring(0, vals.length()-1);
						txtArea_profile.setText(vals);
						rdbtnIncludeInCriterion.setSelected(criterion.getIncludeNulls());
						rdbtnExcludeFromCriterion.setSelected(!criterion.getIncludeNulls());
					}
					setAvailableComparisonOperators();
					setAvailableValueLists();
				}
			});
		}
	}

	private boolean hasSingleValue(){
		return !(radioButton_equal.isSelected() || radioButton_different.isSelected());
	}

	private ComparisonOperator getSelectedComparisonOperator(){
		ComparisonOperator compop = null;
		if (radioButton_equal.isSelected()){
			compop = ComparisonOperator.EQUAL;
		}else if (radioButton_different.isSelected()){
			compop = ComparisonOperator.DIFFERENT;				
		}else if (radioButton_greater.isSelected()){
			compop = ComparisonOperator.GREATER;				
		}else if (radioButton_greaterequal.isSelected()){
			compop = ComparisonOperator.GREATEROREQUAL;				
		}else if (radioButton_smaller.isSelected()){
			compop = ComparisonOperator.SMALLER;				
		}else if (radioButton_smallerequal.isSelected()){
			compop = ComparisonOperator.SMALLEROREQUAL;				
		}else if (radioButton_contains.isSelected()){
			compop = ComparisonOperator.CONTAINS;				
		}else if (radioButton_doesnotcontains.isSelected()){
			compop = ComparisonOperator.DOESNOTCONTAINS;				
		}
		return compop;
	}

	private boolean generateCriterion(){		
		try {
			if (comboBox_field.getSelectedItem() == null){
				JOptionPane.showMessageDialog(this, "You must select a valid field", "Error in filtering criterion", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				return false;
			}
			if (Filter.containsForbiddenCharacters(txtArea_values.getText()) || Filter.containsForbiddenCharacters(txtArea_profile.getText())){
				JOptionPane.showMessageDialog(this, "Your criteria list cannot contains the following characters: "+Filter.getForbiddenCharacters(), "Error in filtering criterion", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				return false;				
			}
			Field field = Field.getField(comboBox_field.getSelectedItem().toString());
			ComparisonOperator compop = getSelectedComparisonOperator();			
			if (chkBox_null.isSelected()){
				criterion = new CustomFilter(filteringPanel, field, compop, true, new ArrayList<String>(), new ArrayList<String>(), true);
			}else{
				List<String> values = new ArrayList<String>();				
				for (String val : txtArea_values.getText().replace("\\\\;", "|*?").split(";")){
					if (val.trim().length() > 0) {
						if (field.getFieldClass() == Double.class){
							if (val.contains("%")){
								val = ""+(Double.parseDouble(val.replace('%', ' ').trim())/100.0);
							}
							values.add(val.replace("|*?", ";").toUpperCase().replace(",",".").trim());
						}else{
							values.add(val.replace("|*?", ";").toUpperCase().trim());					
						}
					}
				}
				if (hasSingleValue() && values.size() > 1){
					JOptionPane.showMessageDialog(this, "The selected comparison operator ("+compop+") only allows a single value.", "Error in filtering criterion", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					return false;
				}
				List<String> profileValues = new ArrayList<String>();
				for (String val : txtArea_profile.getText().replace("\\\\;", "|*?").split(";")){
					if (val.trim().length() > 0) profileValues.add(val.replace("|*?", ";").trim());
				}
				if (hasSingleValue() && profileValues.size() > 0){
					JOptionPane.showMessageDialog(this, "The selected comparison operator ("+compop+") don't allow profile value list.", "Error in filtering criterion", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					return false;
				}
				if ((values.size() + profileValues.size()) == 0){
					JOptionPane.showMessageDialog(this, "You must set at least one value.", "Error in filtering criterion", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					return false;
				}
				boolean includeNulls = rdbtnIncludeInCriterion.isSelected();
				criterion = new CustomFilter(filteringPanel, field, compop, false, values, profileValues, includeNulls);
			}
			return true;
		} catch (Exception ex) {
			Tools.exception(ex);
			JOptionPane.showMessageDialog(this, Tools.getMessage("Error when generating criterion", ex), "Create filtering criterion", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			criterion = null;
			return false;
		}
	}

	public CustomFilter getCriterion(){
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
