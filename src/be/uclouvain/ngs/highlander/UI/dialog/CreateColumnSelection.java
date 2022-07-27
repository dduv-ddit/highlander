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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.dialog.ProfileTree.Action;
import be.uclouvain.ngs.highlander.UI.misc.SearchField;
import be.uclouvain.ngs.highlander.administration.users.User;
import be.uclouvain.ngs.highlander.administration.users.User.Settings;
import be.uclouvain.ngs.highlander.administration.users.User.UserData;
import be.uclouvain.ngs.highlander.database.Category;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.datatype.Analysis;

import javax.swing.JComboBox;

import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

public class CreateColumnSelection extends JDialog {

	private final UserData userData;
	private List<Field> availableValues;
	private List<Field> sourceValues;
	private List<Field> selection = new ArrayList<Field>();
	private String listName;
	private boolean overwrite = false; 

	private DefaultTableModel tSourceModel;
	private JTable tableSource;
	private TableRowSorter<DefaultTableModel> sorter;
	private DefaultTableModel tSelectionModel;
	private JTable tableSelection;
	private JComboBox<String> boxCategories;
	private SearchField	searchField = new SearchField(10);
	private String name;

	public CreateColumnSelection(Analysis analysis, UserData userData, List<Field> availableColumns) {
		this(analysis, userData, availableColumns, null);
	}

	public CreateColumnSelection(Analysis analysis, UserData userData, List<Field> availableColumns, String listName) {
		this.userData = userData;
		this.listName = listName;	
		this.availableValues = availableColumns;
		this.sourceValues = new ArrayList<Field>(availableValues);
		initUI();		
		pack();
		try{
			if (listName != null){
				overwrite = true;
				switch(userData){
				case COLUMN_SELECTION:
					selection = Highlander.getLoggedUser().loadColumnSelection(analysis, listName);
					break;
				case COLUMN_MASK:
					selection = Highlander.getLoggedUser().loadColumnMask(analysis, listName);
					break;
				default:
					break;
				}
				updateSourceTable();
				updateSelectionTable();
			}else if(userData == UserData.SETTINGS) {
				overwrite = true;
				for (String field : User.loadDefaultSettings(analysis.toString(), Settings.DEFAULT_COLUMNS)) {
					selection.add(Field.getField(field));
				}
				updateSourceTable();
				updateSelectionTable();
			}
		}catch(Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Unrecognized field", ex), "Columns selection",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			dispose();
		}
	}

	private void initUI(){
		setModal(true);
		name =  "";
		switch(userData){
		case COLUMN_SELECTION:
			setTitle("Create a selection of columns to fetch from the database");
			setIconImage(Resources.getScaledIcon(Resources.iColumnSelection, 64).getImage());
			name =  "selection";
			break;
		case COLUMN_MASK:
			setTitle("Select columns to mask in the table");
			setIconImage(Resources.getScaledIcon(Resources.iColumnMask, 64).getImage());
			name =  "mask";
			break;
		case SORTING:
			setTitle("Select columns to sort in the table");
			setIconImage(Resources.getScaledIcon(Resources.iSort, 64).getImage());
			name =  "selection";
			break;
		case SETTINGS:
			setTitle("Create a default selection of columns for selected analysis");
			setIconImage(Resources.getScaledIcon(Resources.iColumnSelection, 64).getImage());
			name =  "selection";
			break;
		default:
			break;
		}

		JPanel panel = new JPanel();	
		getContentPane().add(panel, BorderLayout.SOUTH);

		JButton btnOk = new JButton(Resources.getScaledIcon(Resources.iButtonApply, 24));
		btnOk.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (userData == UserData.COLUMN_SELECTION || userData == UserData.COLUMN_MASK){
					if (!overwrite){
						String saveName = ProfileTree.showProfileDialog(CreateColumnSelection.this, Action.SAVE, userData, Highlander.getCurrentAnalysis().toString(), "Save columns "+name+" to your profile", listName);
						if (saveName == null) return;
						listName = saveName;
						dispose();
					}else{
						int res = JOptionPane.showConfirmDialog(CreateColumnSelection.this, "Are you sure you want to save you changes ?", "Edit columns "+name+" in your profile",
								JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iDbSave,64));
						if (res == JOptionPane.YES_OPTION){
							dispose();
						}else if (res == JOptionPane.NO_OPTION){
							cancelClose();
						}
					}
				}else{
					dispose();
				}
			}
		});
		panel.add(btnOk);

		JButton btnCancel = new JButton(Resources.getScaledIcon(Resources.iCross, 24));
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				selection.clear();
				cancelClose();
			}
		});
		panel.add(btnCancel);

		JPanel panel_1 = new JPanel();
		getContentPane().add(panel_1, BorderLayout.CENTER);

		GridBagLayout gbl_panel_1 = new GridBagLayout();
		gbl_panel_1.rowWeights = new double[]{0.0};
		gbl_panel_1.columnWeights = new double[]{1.0, 0.0, 1.0};
		panel_1.setLayout(gbl_panel_1);

		JPanel panel_0 = new JPanel(new BorderLayout(0,0));
		panel_0.setBorder(BorderFactory.createTitledBorder("Available columns"));		
		GridBagConstraints gbc_scrollPaneSource = new GridBagConstraints();
		gbc_scrollPaneSource.weighty = 1.0;
		gbc_scrollPaneSource.weightx = 1.0;
		gbc_scrollPaneSource.insets = new Insets(5, 5, 5, 5);
		gbc_scrollPaneSource.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneSource.gridx = 0;
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
					tip = (val != null) ? Field.getField(val.toString()).getHtmlTooltip() : null;
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
		panel_north.add(boxCategories, BorderLayout.NORTH);
		updateSourceTable();

		JPanel panel_middle = new JPanel();
		GridBagConstraints gbc_panel_middle = new GridBagConstraints();
		gbc_panel_middle.gridx = 1;
		gbc_panel_middle.gridy = 0;
		panel_1.add(panel_middle, gbc_panel_middle);
		GridBagLayout gbl_panel_2 = new GridBagLayout();
		gbl_panel_2.columnWidths = new int[]{0, 0};
		gbl_panel_2.rowHeights = new int[]{0, 0, 0};
		gbl_panel_2.columnWeights = new double[]{0.0, Double.MIN_VALUE};
		gbl_panel_2.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		panel_middle.setLayout(gbl_panel_2);

		JButton button = new JButton(Resources.getScaledIcon(Resources.iArrowDoubleRight, 24));
		button.setToolTipText("Add selected column(s) to your "+name);
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
		button_1.setToolTipText("Remove selected column(s) from your "+name);
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
		panel_2.setBorder(BorderFactory.createTitledBorder("Your selection of columns"));
		GridBagConstraints gbc_panel_2 = new GridBagConstraints();
		gbc_panel_2.weighty = 1.0;
		gbc_panel_2.weightx = 1.0;
		gbc_panel_2.insets = new Insets(5, 5, 5, 5);
		gbc_panel_2.fill = GridBagConstraints.BOTH;
		gbc_panel_2.gridx = 2;
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

		JPanel panel_ordering = new JPanel();
		panel_2.add(panel_ordering, BorderLayout.EAST);
		GridBagLayout gbl_panel_3 = new GridBagLayout();
		gbl_panel_3.columnWidths = new int[]{0, 0};
		gbl_panel_3.rowHeights = new int[]{0, 0, 0, 0, 0};
		gbl_panel_3.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_panel_3.rowWeights = new double[]{1.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		panel_ordering.setLayout(gbl_panel_3);

		JButton button_2 = new JButton(Resources.getScaledIcon(Resources.iArrowDoubleUp, 24));
		button_2.setToolTipText("Put selected column(s) before in order of appearance");
		button_2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setBefore();
			}
		});

		JPanel panel_3 = new JPanel();
		GridBagConstraints gbc_panel_3 = new GridBagConstraints();
		gbc_panel_3.fill = GridBagConstraints.BOTH;
		gbc_panel_3.gridx = 0;
		gbc_panel_3.gridy = 0;
		panel_ordering.add(panel_3, gbc_panel_3);
		GridBagConstraints gbc_button_2 = new GridBagConstraints();
		gbc_button_2.insets = new Insets(0, 0, 5, 0);
		gbc_button_2.gridx = 0;
		gbc_button_2.gridy = 1;
		panel_ordering.add(button_2, gbc_button_2);

		JButton button_3 = new JButton(Resources.getScaledIcon(Resources.iArrowDoubleDown, 24));
		button_3.setToolTipText("Put selected column(s) after in order of appearance");
		button_3.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setAfter();
			}
		});
		GridBagConstraints gbc_button_3 = new GridBagConstraints();
		gbc_button_3.insets = new Insets(0, 0, 10, 0);
		gbc_button_3.gridx = 0;
		gbc_button_3.gridy = 2;
		panel_ordering.add(button_3, gbc_button_3);

		JPanel panel_4 = new JPanel();
		GridBagConstraints gbc_panel_4 = new GridBagConstraints();
		gbc_panel_4.fill = GridBagConstraints.BOTH;
		gbc_panel_4.gridx = 0;
		gbc_panel_4.gridy = 3;
		panel_ordering.add(panel_4, gbc_panel_4);

		if (userData == UserData.COLUMN_MASK) panel_ordering.setVisible(false);
	}

	private void updateSourceTable(){
		List<Field> sortedList = new ArrayList<Field>(sourceValues);
		sortedList.removeAll(selection);
		Object[][] data = new Object[sortedList.size()][1];
		int row = 0;
		for (Field f : sortedList){
			data[row++][0] = f.getName();
		}
		tSourceModel = new DefaultTableModel(data, new String[] {"Available columns"});
		sorter = new TableRowSorter<DefaultTableModel>(tSourceModel);
		tableSource.setModel(tSourceModel);		
		tableSource.setRowSorter(sorter);
		searchField.setSorter(sorter);
		searchField.applyFilter();		
	}

	private void updateSelectionTable(){
		List<Field> sortedList = new ArrayList<Field>(selection);
		Object[][] data = new Object[sortedList.size()][1];
		int row = 0;
		for (Field f : sortedList){
			data[row++][0] = f;
		}
		tSelectionModel = new DefaultTableModel(data, new String[] {"Selected columns"});
		tableSelection.setModel(tSelectionModel);		
	}

	private void filterByCategory(){
		sourceValues = new ArrayList<Field>(availableValues);
		for (Iterator<Field> it = sourceValues.iterator() ; it.hasNext() ; ){
			Field field = it.next();
			if (!boxCategories.getSelectedItem().toString().equals("all available fields") && 
					!field.getCategory().getName().equals(boxCategories.getSelectedItem().toString())){
				it.remove();
			}
		}
		updateSourceTable();
	}

	private void addValues(){		
		try{
			for (int row : tableSource.getSelectedRows()){
				selection.add(Field.getField(tableSource.getValueAt(row, 0).toString()));
			}
			updateSourceTable();
			updateSelectionTable();
		}catch(Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Unrecognized field", ex), "Columns selection",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
	}

	private void removeValues(){
		try{
			for (int row : tableSelection.getSelectedRows()){
				Field select = Field.getField(tableSelection.getValueAt(row, 0).toString());
				selection.remove(select);
			}
			updateSourceTable();
			updateSelectionTable();
		}catch(Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Unrecognized field", ex), "Columns selection",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
	}

	private void setBefore(){
		try{
			List<String> toMove = new ArrayList<String>();
			for (int row : tableSelection.getSelectedRows()){
				toMove.add(tableSelection.getValueAt(row, 0).toString());			
			}
			int pivot = tableSelection.getSelectedRow();
			if (pivot != -1){
				if (pivot > 0) pivot--;
				for (Iterator<Field> it = selection.iterator() ; it.hasNext() ; ){
					Field field = it.next();
					if (toMove.contains(field.getName())) it.remove();
				}
				for (String name : toMove){
					selection.add(pivot++, Field.getField(name));
				}
			}
			updateSelectionTable();
			for (int r=0 ; r < tableSelection.getRowCount() ; r++){
				String val = tableSelection.getValueAt(r, 0).toString();
				if (toMove.contains(val)) tableSelection.addRowSelectionInterval(r, r);
			}
		}catch(Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Unrecognized field", ex), "Columns reordering",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
	}

	private void setAfter(){
		try{
			List<String> toMove = new ArrayList<String>();
			for (int row : tableSelection.getSelectedRows()){
				toMove.add(tableSelection.getValueAt(row, 0).toString());			
			}
			int pivot = tableSelection.getSelectedRow();
			if (pivot != -1){
				for (Iterator<Field> it = selection.iterator() ; it.hasNext() ; ){
					Field field = it.next();
					if (toMove.contains(field.getName())) it.remove();
				}
				if (pivot < selection.size()) pivot++;
				for (String name : toMove){
					selection.add(pivot++, Field.getField(name));
				}
			}
			updateSelectionTable();
			for (int r=0 ; r < tableSelection.getRowCount() ; r++){
				String val = tableSelection.getValueAt(r, 0).toString();
				if (toMove.contains(val)) tableSelection.addRowSelectionInterval(r, r);
			}
		}catch(Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Unrecognized field", ex), "Columns reordering",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
	}

	public List<Field> getSelection(){
		return selection;
	}

	public String getListName(){
		return listName;
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
		listName = null;
		dispose();
	}

}
