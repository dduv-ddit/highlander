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

package be.uclouvain.ngs.highlander.administration.UI.users;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.UI.misc.SearchField;
import be.uclouvain.ngs.highlander.administration.UI.ManagerPanel;
import be.uclouvain.ngs.highlander.administration.UI.ProjectManager;
import be.uclouvain.ngs.highlander.administration.users.User;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.AutoCompleteSupport;

/**
* @author Raphael Helaers
*/

public class UserPermissionsPanel extends ManagerPanel {
	
	private JComboBox<User> boxUser;
	private JComboBox<String> boxPathologies;
	AutoCompleteSupport<User> boxSupport;
	private DefaultTableModel tableHasPermissionModel;
	private JTable tableHasPermission;
	private TableRowSorter<DefaultTableModel> hasPermissionSorter;
	private DefaultTableModel tableNoPermissionModel;
	private JTable tableNoPermission;
	private TableRowSorter<DefaultTableModel> noPermissionSorter;
	private SearchField	searchField = new SearchField(10);

	public UserPermissionsPanel(ProjectManager manager){
		super(manager);
		JPanel panel_filters = new JPanel(new BorderLayout());
		add(panel_filters, BorderLayout.NORTH);

		JPanel panel_subfilters = new JPanel(new GridLayout(1,2)); 
		panel_filters.add(panel_subfilters, BorderLayout.NORTH);

		boxUser = new JComboBox<User>(manager.getUsers());
		boxUser.setMaximumRowCount(20);

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				boxSupport = AutoCompleteSupport.install(boxUser, manager.getUserList());
				boxSupport.setCorrectsCase(true);
				boxSupport.setFilterMode(TextMatcherEditor.CONTAINS);
				boxSupport.setTextMatchingStrategy(TextMatcherEditor.NORMALIZED_STRATEGY);
				boxSupport.setStrict(false);
			}
		});
		boxUser.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (arg0.getActionCommand().equals("comboBoxEdited")){
					if (boxUser.getSelectedIndex() < 0) boxUser.setSelectedItem(null);
				}
			}
		});
		boxUser.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					if (boxUser.getSelectedIndex() >= 0) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								updateTables(boxPathologies.getSelectedItem().toString());
							}
						});
					}
				}
			}
		});
		panel_subfilters.add(boxUser);

		boxPathologies = new JComboBox<String>(manager.listPathologies());
		boxPathologies.insertItemAt("All pathologies",0);
		boxPathologies.setSelectedIndex(0);
		boxPathologies.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED){
					if (boxPathologies.getSelectedIndex() >= 0) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								updateTables(boxPathologies.getSelectedItem().toString());
							}
						});
					}
				}
			}
		});
		panel_subfilters.add(boxPathologies);

		searchField.addFieldListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent arg0) {
				applyBothFilters();
			}
			@Override
			public void keyTyped(KeyEvent arg0) {			}
			@Override
			public void keyPressed(KeyEvent arg0) {			}
		});
		panel_filters.add(searchField, BorderLayout.CENTER);

		JPanel panel_1 = new JPanel();
		add(panel_1, BorderLayout.CENTER);

		GridBagLayout gbl_panel_1 = new GridBagLayout();
		gbl_panel_1.rowWeights = new double[]{0.0};
		gbl_panel_1.columnWeights = new double[]{1.0, 0.0, 1.0};
		panel_1.setLayout(gbl_panel_1);

		JPanel panel_0 = new JPanel(new BorderLayout(0,0));
		panel_0.setBorder(BorderFactory.createTitledBorder("Has permission to modify"));		
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

		tableHasPermission = new JTable(){
			@Override
			public boolean isCellEditable(int row, int column){
				return false;
			}
		};
		tableHasPermission.setTableHeader(null);
		tableHasPermission.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount() == 2){
					removePermission();
				}
			}
		});
		scrollPaneSource.setViewportView(tableHasPermission);

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
		button.setToolTipText("Remove permission for selected sample(s)");
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				removePermission();
			}
		});
		GridBagConstraints gbc_button = new GridBagConstraints();
		gbc_button.insets = new Insets(0, 0, 5, 0);
		gbc_button.gridx = 0;
		gbc_button.gridy = 0;
		panel_middle.add(button, gbc_button);

		JButton button_1 = new JButton(Resources.getScaledIcon(Resources.iArrowDoubleLeft, 24));
		button_1.setToolTipText("Grant permission for selected sample(s)");
		button_1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				grantPermission();
			}
		});
		GridBagConstraints gbc_button_1 = new GridBagConstraints();
		gbc_button_1.gridx = 0;
		gbc_button_1.gridy = 1;
		panel_middle.add(button_1, gbc_button_1);

		JScrollPane scrollPaneSelection = new JScrollPane();
		scrollPaneSelection.setBorder(BorderFactory.createTitledBorder("Do not has permission to modify"));
		GridBagConstraints gbc_scrollPaneSelection = new GridBagConstraints();
		gbc_scrollPaneSelection.weighty = 1.0;
		gbc_scrollPaneSelection.weightx = 1.0;
		gbc_scrollPaneSelection.insets = new Insets(5, 5, 5, 5);
		gbc_scrollPaneSelection.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneSelection.gridx = 2;
		gbc_scrollPaneSelection.gridy = 0;
		panel_1.add(scrollPaneSelection, gbc_scrollPaneSelection);

		tableNoPermissionModel = new DefaultTableModel(0,1);
		tableNoPermission = new JTable(tableNoPermissionModel){
			@Override
			public boolean isCellEditable(int row, int column){
				return false;
			}
		};
		tableNoPermission.setTableHeader(null);
		tableNoPermission.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount() == 2){
					grantPermission();
				}
			}
		});
		scrollPaneSelection.setViewportView(tableNoPermission);
	
		new Thread(new Runnable() {				
			@Override
			public void run() {
				for (int i=0 ; i < manager.getUsers().length ; i++){
					if (manager.getUsers()[i].getUsername().equals(ProjectManager.getLoggedUser().getUsername())){
						boxUser.setSelectedIndex(i);
						break;
					}
				}
			}
		}, "UserPermissionsPanel.fill").start();
}
	
	private void applyBothFilters(){
		RowFilter<DefaultTableModel, Object> rf = null;
		//If current expression doesn't parse, don't update.
		try {
			rf = RowFilter.regexFilter("(?i)"+searchField.getText());
		} catch (java.util.regex.PatternSyntaxException e) {
			return;
		}
		noPermissionSorter.setRowFilter(rf);
	}

	private void updateTables(String pathology){
		try{
			User selectedUser = boxUser.getItemAt(boxUser.getSelectedIndex());
			Set<String> samples = new TreeSet<>();
			try (Results res = DB.select(Schema.HIGHLANDER, "SELECT sample from projects JOIN pathologies USING (pathology_id)" + ((pathology.equals("All pathologies")) ?  "" : " WHERE pathology = '"+pathology+"'"))) {
				while (res.next()){
					samples.add(res.getString(1));
				}
			}
			Set<String> permissions = new TreeSet<>();
			try (Results res = DB.select(Schema.HIGHLANDER, "SELECT sample FROM projects_users JOIN projects USING (project_id) JOIN pathologies USING (pathology_id) WHERE username = '"+selectedUser.getUsername()+"'" + ((pathology.equals("All pathologies")) ? "" :  " AND pathology = '"+pathology+"'"))) {
				while (res.next()){
					permissions.add(res.getString(1));
				}
			}
			List<String> noPermission = new ArrayList<String>(samples);
			noPermission.removeAll(permissions);

			Object[][] dataHasPermission = new Object[permissions.size()][1];
			int row = 0;
			for (String o : permissions){
				dataHasPermission[row++][0] = o;
			}
			tableHasPermissionModel = new DefaultTableModel(dataHasPermission, new String[] {"Has permission to modify"});
			hasPermissionSorter = new TableRowSorter<DefaultTableModel>(tableHasPermissionModel);
			tableHasPermission.setModel(tableHasPermissionModel);		
			tableHasPermission.setRowSorter(hasPermissionSorter);
			searchField.setSorter(hasPermissionSorter);
			searchField.applyFilter();

			Object[][] dataNoPermission = new Object[noPermission.size()][1];
			row = 0;
			for (String o : noPermission){
				dataNoPermission[row++][0] = o;
			}
			tableNoPermissionModel = new DefaultTableModel(dataNoPermission, new String[] {"No permission to modify"});
			noPermissionSorter = new TableRowSorter<DefaultTableModel>(tableNoPermissionModel);
			tableNoPermission.setModel(tableNoPermissionModel);		
			tableNoPermission.setRowSorter(noPermissionSorter);
			applyBothFilters();

		}catch(Exception ex){
			ProjectManager.toConsole(ex);
		}
	}

	private void removePermission(){
		User selectedUser = boxUser.getItemAt(boxUser.getSelectedIndex());
		Set<String> samples = new HashSet<>(); 
		Set<Integer> projectIds = new HashSet<>(); 
		for (int row : tableHasPermission.getSelectedRows()){
			samples.add(tableHasPermission.getValueAt(row, 0).toString());
		}
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT project_id FROM projects WHERE sample IN ("+HighlanderDatabase.makeSqlList(samples, String.class)+")")) {
			while (res.next()){
				projectIds.add(res.getInt(1));
			}
			DB.update(Schema.HIGHLANDER, "DELETE FROM projects_users WHERE username = '"+selectedUser.getUsername()+"' AND project_id IN ("+HighlanderDatabase.makeSqlList(projectIds, Integer.class)+")");
		}catch(Exception ex){
			ProjectManager.toConsole(ex);
		}
		updateTables(boxPathologies.getSelectedItem().toString());
	}

	private void grantPermission(){
		User selectedUser = boxUser.getItemAt(boxUser.getSelectedIndex());
		Set<String> samples = new HashSet<>(); 
		Set<Integer> projectIds = new HashSet<>(); 
		for (int row : tableNoPermission.getSelectedRows()){
			samples.add(tableNoPermission.getValueAt(row, 0).toString());
		}
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT project_id FROM projects WHERE sample IN ("+HighlanderDatabase.makeSqlList(samples, String.class)+")")) {
			while (res.next()){
				projectIds.add(res.getInt(1));
			}
			for (int id : projectIds){
				DB.insert(Schema.HIGHLANDER, "INSERT IGNORE INTO projects_users VALUES("+id+",'"+selectedUser.getUsername()+"')");
			}
		}catch(Exception ex){
			ProjectManager.toConsole(ex);
		}		
		updateTables(boxPathologies.getSelectedItem().toString());
	}

}
