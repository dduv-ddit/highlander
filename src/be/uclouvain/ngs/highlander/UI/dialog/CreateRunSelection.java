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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.misc.SearchField;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.Field.SampleType;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.RunNGS;

import javax.swing.JComboBox;

import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

import javax.swing.JLabel;

public class CreateRunSelection extends JDialog {

	private Map<String,RunNGS> availableValues = new TreeMap<String, RunNGS>();
	private Set<RunNGS> sourceValues;
	private Set<RunNGS> selection;

	private DefaultTableModel tSourceModel;
	private JTable tableSource;
	private TableRowSorter<DefaultTableModel> sorter;
	private DefaultTableModel tSelectionModel;
	private JTable tableSelection;
	private SearchField	searchField = new SearchField(10);
	private JComboBox<String> box_sequencing_target;
	private JComboBox<String> box_platform;
	private JComboBox<String> box_outsourcing;
	private JComboBox<String> box_pathology;
	private JComboBox<String> box_sample_type;
	private JComboBox<String> box_kit;
	private JComboBox<String> box_read_length;
	private JComboBox<String> box_pair_end;

	public CreateRunSelection() {
		this(new TreeSet<RunNGS>());
	}

	public CreateRunSelection(Set<RunNGS> selection) {
		this.selection = new TreeSet<RunNGS>(selection);
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
				"SELECT sequencing_target, platform, outsourcing, pathology, sample_type, kit, read_length, pair_end, run_id, run_date, run_name, run_label "
				+ "FROM projects JOIN pathologies USING (pathology_id)")) {
			while (res.next()){
				String label = res.getString("run_label");
				if (availableValues.containsKey(label)){
					availableValues.get(label).addPathology(res.getString("pathology"));
					availableValues.get(label).addSampleType(SampleType.valueOf(res.getString("sample_type")));
					if(res.getString("kit") != null && res.getString("kit").length() > 0) availableValues.get(label).addKit(res.getString("kit"));
				}else{
					availableValues.put(label, new RunNGS(res));
				}
			}
		}catch (Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(this,  Tools.getMessage("Cannot retreive runs", ex), "Create a selection of NGS runs",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
		sourceValues = new TreeSet<RunNGS>(availableValues.values());		
		initUI();		
		if (!selection.isEmpty()) {
			updateSourceTable();
			updateSelectionTable();
		}
		pack();
	}

	private void initUI(){
		setModal(true);
		setTitle("Create a selection of NGS runs");
		setIconImage(Resources.getScaledIcon(Resources.iRunReport, 64).getImage());

		JPanel panel = new JPanel();	
		getContentPane().add(panel, BorderLayout.SOUTH);

		JButton btnOk = new JButton(Resources.getScaledIcon(Resources.iButtonApply, 24));
		btnOk.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {		
				dispose();
			}
		});
		panel.add(btnOk);

		JButton btnCancel = new JButton(Resources.getScaledIcon(Resources.iCross, 24));
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				cancelClose();
			}
		});
		panel.add(btnCancel);

		JPanel panel_1 = new JPanel();
		getContentPane().add(panel_1, BorderLayout.CENTER);

		GridBagLayout gbl_panel_1 = new GridBagLayout();
		gbl_panel_1.rowWeights = new double[]{0.0};
		gbl_panel_1.columnWeights = new double[]{0.0, 1.0, 0.0, 1.0};
		panel_1.setLayout(gbl_panel_1);

		GridBagLayout gbl_panel_filter = new GridBagLayout();
		gbl_panel_filter.columnWidths = new int[]{100, 150};
		JPanel panel_filter = new JPanel(gbl_panel_filter);
		panel_filter.setBorder(BorderFactory.createTitledBorder("Filtering options"));		
		GridBagConstraints gbc_panel_filter = new GridBagConstraints();
		gbc_panel_filter.weighty = 1.0;
		gbc_panel_filter.insets = new Insets(5, 5, 5, 5);
		gbc_panel_filter.fill = GridBagConstraints.BOTH;
		gbc_panel_filter.gridx = 0;
		gbc_panel_filter.gridy = 0;
		panel_1.add(panel_filter, gbc_panel_filter);

		JLabel lblPlatform = new JLabel("Platform");
		GridBagConstraints gbc_lblPlatform = new GridBagConstraints();
		gbc_lblPlatform.anchor = GridBagConstraints.WEST;
		gbc_lblPlatform.insets = new Insets(0, 10, 5, 5);
		gbc_lblPlatform.gridx = 0;
		gbc_lblPlatform.gridy = 1;
		panel_filter.add(lblPlatform, gbc_lblPlatform);

		JLabel lblOutsourcing = new JLabel("Outsourcing");
		GridBagConstraints gbc_lblOutsourcing = new GridBagConstraints();
		gbc_lblOutsourcing.anchor = GridBagConstraints.WEST;
		gbc_lblOutsourcing.insets = new Insets(0, 10, 5, 5);
		gbc_lblOutsourcing.gridx = 0;
		gbc_lblOutsourcing.gridy = 2;
		panel_filter.add(lblOutsourcing, gbc_lblOutsourcing);

		box_outsourcing = new JComboBox<>(fillComboBox("outsourcing"));
		box_outsourcing.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					applyBoxFilters();
				}
			}
		});
		GridBagConstraints gbc_box_outsourcing = new GridBagConstraints();
		gbc_box_outsourcing.insets = new Insets(0, 0, 5, 10);
		gbc_box_outsourcing.fill = GridBagConstraints.HORIZONTAL;
		gbc_box_outsourcing.gridx = 1;
		gbc_box_outsourcing.gridy = 2;
		panel_filter.add(box_outsourcing, gbc_box_outsourcing);

		JLabel lblGehuGroup = new JLabel("Pathology");
		GridBagConstraints gbc_lblGehuGroup = new GridBagConstraints();
		gbc_lblGehuGroup.anchor = GridBagConstraints.WEST;
		gbc_lblGehuGroup.insets = new Insets(0, 10, 5, 5);
		gbc_lblGehuGroup.gridx = 0;
		gbc_lblGehuGroup.gridy = 3;
		panel_filter.add(lblGehuGroup, gbc_lblGehuGroup);

		box_pathology = new JComboBox<>(fillComboBox("pathology"));
		box_pathology.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					applyBoxFilters();
				}
			}
		});
		GridBagConstraints gbc_box_group = new GridBagConstraints();
		gbc_box_group.insets = new Insets(0, 0, 5, 10);
		gbc_box_group.fill = GridBagConstraints.HORIZONTAL;
		gbc_box_group.gridx = 1;
		gbc_box_group.gridy = 3;
		panel_filter.add(box_pathology, gbc_box_group);

		JLabel lblSampleType = new JLabel("Sample type");
		GridBagConstraints gbc_lblSampleType = new GridBagConstraints();
		gbc_lblSampleType.anchor = GridBagConstraints.WEST;
		gbc_lblSampleType.insets = new Insets(0, 10, 5, 5);
		gbc_lblSampleType.gridx = 0;
		gbc_lblSampleType.gridy = 4;
		panel_filter.add(lblSampleType, gbc_lblSampleType);

		box_sample_type = new JComboBox<>(fillComboBox("sample_type"));
		box_sample_type.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					applyBoxFilters();
				}
			}
		});
		GridBagConstraints gbc_box_sample_type = new GridBagConstraints();
		gbc_box_sample_type.insets = new Insets(0, 0, 5, 10);
		gbc_box_sample_type.fill = GridBagConstraints.HORIZONTAL;
		gbc_box_sample_type.gridx = 1;
		gbc_box_sample_type.gridy = 4;
		panel_filter.add(box_sample_type, gbc_box_sample_type);

		JLabel lblCaptureKit = new JLabel("Capture kit");
		GridBagConstraints gbc_lblCaptureKit = new GridBagConstraints();
		gbc_lblCaptureKit.anchor = GridBagConstraints.WEST;
		gbc_lblCaptureKit.insets = new Insets(0, 10, 5, 5);
		gbc_lblCaptureKit.gridx = 0;
		gbc_lblCaptureKit.gridy = 5;
		panel_filter.add(lblCaptureKit, gbc_lblCaptureKit);

		box_kit = new JComboBox<>(fillComboBox("kit"));
		box_kit.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					applyBoxFilters();
				}
			}
		});
		GridBagConstraints gbc_box_kit = new GridBagConstraints();
		gbc_box_kit.insets = new Insets(0, 0, 5, 10);
		gbc_box_kit.fill = GridBagConstraints.HORIZONTAL;
		gbc_box_kit.gridx = 1;
		gbc_box_kit.gridy = 5;
		panel_filter.add(box_kit, gbc_box_kit);

		JLabel lblReadLength = new JLabel("Read length");
		GridBagConstraints gbc_lblReadLength = new GridBagConstraints();
		gbc_lblReadLength.anchor = GridBagConstraints.WEST;
		gbc_lblReadLength.insets = new Insets(0, 10, 5, 5);
		gbc_lblReadLength.gridx = 0;
		gbc_lblReadLength.gridy = 6;
		panel_filter.add(lblReadLength, gbc_lblReadLength);

		box_read_length = new JComboBox<>(fillComboBox("read_length"));
		box_read_length.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					applyBoxFilters();
				}
			}
		});
		GridBagConstraints gbc_box_read_length = new GridBagConstraints();
		gbc_box_read_length.insets = new Insets(0, 0, 5, 10);
		gbc_box_read_length.fill = GridBagConstraints.HORIZONTAL;
		gbc_box_read_length.gridx = 1;
		gbc_box_read_length.gridy = 6;
		panel_filter.add(box_read_length, gbc_box_read_length);

		JLabel lblPairEnd = new JLabel("Pair end");
		GridBagConstraints gbc_lblPairEnd = new GridBagConstraints();
		gbc_lblPairEnd.anchor = GridBagConstraints.WEST;
		gbc_lblPairEnd.insets = new Insets(0, 10, 5, 5);
		gbc_lblPairEnd.gridx = 0;
		gbc_lblPairEnd.gridy = 7;
		panel_filter.add(lblPairEnd, gbc_lblPairEnd);

		JLabel lblSequencingTarget = new JLabel("Sequencing target");
		GridBagConstraints gbc_lblSequencingTarget = new GridBagConstraints();
		gbc_lblSequencingTarget.anchor = GridBagConstraints.WEST;
		gbc_lblSequencingTarget.insets = new Insets(10, 10, 5, 5);
		gbc_lblSequencingTarget.gridx = 0;
		gbc_lblSequencingTarget.gridy = 0;
		panel_filter.add(lblSequencingTarget, gbc_lblSequencingTarget);

		box_sequencing_target = new JComboBox<>(fillComboBox("sequencing_target"));
		box_sequencing_target.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					applyBoxFilters();
				}
			}
		});
		GridBagConstraints gbc_box_sequencing_target = new GridBagConstraints();
		gbc_box_sequencing_target.insets = new Insets(10, 0, 5, 10);
		gbc_box_sequencing_target.fill = GridBagConstraints.HORIZONTAL;
		gbc_box_sequencing_target.gridx = 1;
		gbc_box_sequencing_target.gridy = 0;
		panel_filter.add(box_sequencing_target, gbc_box_sequencing_target);

		box_platform = new JComboBox<>(fillComboBox("platform"));
		box_platform.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					applyBoxFilters();
				}
			}
		});
		GridBagConstraints gbc_box_platform = new GridBagConstraints();
		gbc_box_platform.insets = new Insets(0, 0, 5, 10);
		gbc_box_platform.fill = GridBagConstraints.HORIZONTAL;
		gbc_box_platform.gridx = 1;
		gbc_box_platform.gridy = 1;
		panel_filter.add(box_platform, gbc_box_platform);
		
		box_pair_end = new JComboBox<>(fillComboBox("pair_end"));
		box_pair_end.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					applyBoxFilters();
				}
			}
		});
		GridBagConstraints gbc_box_pair_end = new GridBagConstraints();
		gbc_box_pair_end.insets = new Insets(0, 0, 5, 10);
		gbc_box_pair_end.fill = GridBagConstraints.HORIZONTAL;
		gbc_box_pair_end.gridx = 1;
		gbc_box_pair_end.gridy = 7;
		panel_filter.add(box_pair_end, gbc_box_pair_end);

		JButton btnClearFilters = new JButton("Clear filters",Resources.getScaledIcon(Resources.iCross, 18));
		btnClearFilters.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				box_pathology.setSelectedItem("any");
				box_kit.setSelectedItem("any");
				box_outsourcing.setSelectedItem("any");
				box_pair_end.setSelectedItem("any");
				box_platform.setSelectedItem("any");
				box_sequencing_target.setSelectedItem("any");
				box_read_length.setSelectedItem("any");
				box_sample_type.setSelectedItem("any");
			}
		});
		GridBagConstraints gbc_btnClearFilters = new GridBagConstraints();
		gbc_btnClearFilters.anchor = GridBagConstraints.WEST;
		gbc_btnClearFilters.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnClearFilters.gridwidth = 2;
		gbc_btnClearFilters.insets = new Insets(10, 5, 10, 5);
		gbc_btnClearFilters.gridx = 0;
		gbc_btnClearFilters.gridy = 10;
		panel_filter.add(btnClearFilters, gbc_btnClearFilters);

		JPanel panel_3 = new JPanel();
		GridBagConstraints gbc_panel_3 = new GridBagConstraints();
		gbc_panel_3.gridwidth = 2;
		gbc_panel_3.weighty = 1.0;
		gbc_panel_3.fill = GridBagConstraints.BOTH;
		gbc_panel_3.gridx = 0;
		gbc_panel_3.gridy = 11;
		panel_filter.add(panel_3, gbc_panel_3);


		JPanel panel_0 = new JPanel(new BorderLayout(0,0));
		panel_0.setBorder(BorderFactory.createTitledBorder("Available runs"));		
		GridBagConstraints gbc_scrollPaneSource = new GridBagConstraints();
		gbc_scrollPaneSource.weighty = 1.0;
		gbc_scrollPaneSource.weightx = 1.0;
		gbc_scrollPaneSource.insets = new Insets(5, 5, 5, 5);
		gbc_scrollPaneSource.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneSource.gridx = 1;
		gbc_scrollPaneSource.gridy = 0;
		panel_1.add(panel_0, gbc_scrollPaneSource);

		JScrollPane scrollPaneSource = new JScrollPane();
		panel_0.add(scrollPaneSource, BorderLayout.CENTER);

		tableSource = new JTable(){
			public String getToolTipText(MouseEvent e) {
				String tip = null;
				java.awt.Point p = e.getPoint();
				int rowIndex = rowAtPoint(p);
				Object val = getValueAt(rowIndex, 0);
				try{
					tip = (val != null) ? val.toString() : null;
				}catch(Exception ex){
					Tools.exception(ex);
				}
				return tip;
			}

			public boolean isCellEditable(int row, int column){
				return false;
			}
		};
		tableSource.setTableHeader(null);
		tableSource.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount() == 2){
					addValues();
				}
			}
		});
		scrollPaneSource.setViewportView(tableSource);

		JPanel panel_north = new JPanel();
		panel_0.add(panel_north, BorderLayout.NORTH);
		panel_north.setLayout(new BorderLayout(0, 0));

		panel_north.add(searchField, BorderLayout.SOUTH);

		updateSourceTable();

		JPanel panel_middle = new JPanel();
		GridBagConstraints gbc_panel_middle = new GridBagConstraints();
		gbc_panel_middle.gridx = 2;
		gbc_panel_middle.gridy = 0;
		panel_1.add(panel_middle, gbc_panel_middle);
		GridBagLayout gbl_panel_2 = new GridBagLayout();
		gbl_panel_2.columnWidths = new int[]{0, 0};
		gbl_panel_2.rowHeights = new int[]{0, 0, 0};
		gbl_panel_2.columnWeights = new double[]{0.0, Double.MIN_VALUE};
		gbl_panel_2.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		panel_middle.setLayout(gbl_panel_2);

		JButton button = new JButton(Resources.getScaledIcon(Resources.iArrowDoubleRight, 24));
		button.setToolTipText("Add selected run(s)");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				addValues();
			}
		});
		GridBagConstraints gbc_button = new GridBagConstraints();
		gbc_button.insets = new Insets(0, 0, 5, 0);
		gbc_button.gridx = 0;
		gbc_button.gridy = 0;
		panel_middle.add(button, gbc_button);

		JButton button_1 = new JButton(Resources.getScaledIcon(Resources.iArrowDoubleLeft, 24));
		button_1.setToolTipText("Remove selected run(s)");
		button_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				removeValues();
			}
		});
		GridBagConstraints gbc_button_1 = new GridBagConstraints();
		gbc_button_1.gridx = 0;
		gbc_button_1.gridy = 1;
		panel_middle.add(button_1, gbc_button_1);

		JPanel panel_2 = new JPanel(new BorderLayout(0,0));
		panel_2.setBorder(BorderFactory.createTitledBorder("Your selection of runs"));
		GridBagConstraints gbc_panel_2 = new GridBagConstraints();
		gbc_panel_2.weighty = 1.0;
		gbc_panel_2.weightx = 1.0;
		gbc_panel_2.insets = new Insets(5, 5, 5, 5);
		gbc_panel_2.fill = GridBagConstraints.BOTH;
		gbc_panel_2.gridx = 3;
		gbc_panel_2.gridy = 0;
		panel_1.add(panel_2, gbc_panel_2);		

		JScrollPane scrollPaneSelection = new JScrollPane();
		panel_2.add(scrollPaneSelection, BorderLayout.CENTER);

		tSelectionModel = new DefaultTableModel(0,1);
		tableSelection = new JTable(tSelectionModel){
			public boolean isCellEditable(int row, int column){
				return false;
			}
		};
		tableSelection.setTableHeader(null);
		tableSelection.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount() == 2){
					removeValues();
				}
			}
		});
		scrollPaneSelection.setViewportView(tableSelection);

	}

	private String[] fillComboBox(String field){
		Set<Object> set = new TreeSet<Object>();
		for (RunNGS run : availableValues.values()){
			if (field.equalsIgnoreCase("platform")){
				set.add(run.getPlatform());
			}else if (field.equalsIgnoreCase("sequencing_target")){
				if (run.getSequencingTarget() == null) set.add("no sequencing target");
				else set.add(run.getSequencingTarget());
			}else if (field.equalsIgnoreCase("outsourcing")){
				if (run.getOutsourcing() == null) set.add("no outsourcing");
				else set.add(run.getOutsourcing());
			}else if (field.equalsIgnoreCase("pathology")){
				for (String pathology : run.getPathologies()){
					set.add(pathology);
				}
			}else if (field.equalsIgnoreCase("sample_type")){
				for(SampleType type : run.getSampleTypes()){
					set.add(type);
				}
			}else if (field.equalsIgnoreCase("kit")){
				for (String kit : run.getKits()){
					set.add(kit);
				}
			}else if (field.equalsIgnoreCase("read_length")){
				if (run.getRead_length() == null) set.add("unknown");
				else set.add(run.getRead_length());
			}else if (field.equalsIgnoreCase("pair_end")){
				set.add(run.isPair_end());
			}
		}
		String[] array = new String[set.size()+1];
		array[0] = "any";
		int i = 1;
		for (Object item : set){
			array[i++] = item.toString();
		}
		return array;
	}

	private void updateSourceTable(){
		Set<RunNGS> sortedList = new TreeSet<RunNGS>(sourceValues);
		for (RunNGS run : selection){
			sortedList.remove(run);			
		}
		Object[][] data = new Object[sortedList.size()][1];
		int row = 0;
		for (RunNGS r : sortedList){
			data[row++][0] = r;
		}
		tSourceModel = new DefaultTableModel(data, new String[] {"Available runs"});
		sorter = new TableRowSorter<DefaultTableModel>(tSourceModel);
		tableSource.setModel(tSourceModel);		
		tableSource.setRowSorter(sorter);
		searchField.setSorter(sorter);
		searchField.applyFilter();		
	}

	private void updateSelectionTable(){
		Set<RunNGS> sortedList = new TreeSet<RunNGS>(selection);
		Object[][] data = new Object[sortedList.size()][1];
		int row = 0;
		for (RunNGS f : sortedList){
			data[row++][0] = f;
		}
		tSelectionModel = new DefaultTableModel(data, new String[] {"Selected runs"});
		tableSelection.setModel(tSelectionModel);		
	}

	private void applyBoxFilters(){
		sourceValues = new TreeSet<RunNGS>(availableValues.values());
		for (Iterator<RunNGS> it = sourceValues.iterator() ; it.hasNext() ; ){
			RunNGS run = it.next();
			boolean remove = false;
			if (!box_outsourcing.getSelectedItem().toString().equals("any")){
				String sel = box_outsourcing.getSelectedItem().toString();
				if (sel.equals("no outsourcing")){
					if (run.getOutsourcing() != null) remove = true;
				}else{
					if (run.getOutsourcing() == null) remove = true;
					else if (!run.getOutsourcing().equals(sel)) remove = true;
				}
			}
			if (!box_read_length.getSelectedItem().toString().equals("any")){
				String sel = box_read_length.getSelectedItem().toString();
				if (sel.equals("unknown")){
					if (run.getRead_length() != null) remove = true;
				}else{
					if (run.getRead_length() == null) remove = true;
					else if (!run.getRead_length().equals(sel)) remove = true;
				}
			}
			if (!box_pair_end.getSelectedItem().toString().equals("any")){
				String sel = box_pair_end.getSelectedItem().toString();
				if (run.isPair_end() != Boolean.parseBoolean(sel)) remove = true;
			}
			if (!box_sequencing_target.getSelectedItem().toString().equals("any")){
				String sel = box_sequencing_target.getSelectedItem().toString();
				if (!run.getSequencingTarget().equals(sel)) remove = true;
			}
			if (!box_platform.getSelectedItem().toString().equals("any")){
				String sel = box_platform.getSelectedItem().toString();
				if (!run.getPlatform().equals(sel)) remove = true;
			}
			if (!box_pathology.getSelectedItem().toString().equals("any")){
				String sel = box_pathology.getSelectedItem().toString();
				if (!run.getPathologies().contains(sel)) remove = true;
			}
			if (!box_kit.getSelectedItem().toString().equals("any")){
				String sel = box_kit.getSelectedItem().toString();
				if (!run.getKits().contains(sel)) remove = true;
			}
			if (!box_sample_type.getSelectedItem().toString().equals("any")){
				String sel = box_sample_type.getSelectedItem().toString();
				if (!run.getSampleTypes().contains(SampleType.valueOf(sel))) remove = true;
			}
			if (remove) it.remove();
		}
		updateSourceTable();
	}

	private void addValues(){		
		for (int row : tableSource.getSelectedRows()){
			selection.add(availableValues.get(tableSource.getValueAt(row, 0).toString()));
		}
		updateSourceTable();
		updateSelectionTable();
	}

	private void removeValues(){
		for (int row : tableSelection.getSelectedRows()){
			RunNGS select = availableValues.get(tableSelection.getValueAt(row, 0).toString());
			selection.remove(select);
		}
		updateSourceTable();
		updateSelectionTable();
	}

	public Set<RunNGS> getSelection(){
		return selection;
	}

	//Overridden so we can exit when window is closed
	protected void processWindowEvent(WindowEvent e) {
		super.processWindowEvent(e);
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			cancelClose();
		}
	}

	private void cancelClose(){
		selection.clear();
		dispose();
	}

}
