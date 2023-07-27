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

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import java.awt.BorderLayout;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JScrollPane;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Toolkit;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.misc.SearchField;
import be.uclouvain.ngs.highlander.UI.misc.WaitingPanel;
import be.uclouvain.ngs.highlander.administration.users.User;

public class AskUsersDialog extends JDialog {
	private Set<User> values;
	private Set<User> selection = new TreeSet<User>();
	private boolean singleValue = false;

	private DefaultTableModel tSourceModel;
	private JTable tableSource;
	private TableRowSorter<DefaultTableModel> sorter;
	private DefaultTableModel tSelectionModel;
	private JTable tableSelection;
	private SearchField	searchField = new SearchField(10);

	static private WaitingPanel waitingPanel;

	public AskUsersDialog(boolean singleValue, Set<User> startingSelection) {
		try{
			values = new TreeSet<User>(User.fetchList());
		}catch(Exception ex){
			Tools.exception(ex);
		}
		this.singleValue = singleValue;
		selection.addAll(startingSelection);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int width = screenSize.width - (screenSize.width/3*2);
		int height = screenSize.height - (screenSize.height/3);
		setSize(new Dimension(width,height));
		initUI();
		this.addComponentListener(new ComponentListener() {
			@Override
			public void componentShown(ComponentEvent arg0) {
				updateSourceTable();				
				if (!selection.isEmpty()){
					updateSelectionTable();
				}
			}
			@Override
			public void componentResized(ComponentEvent arg0) {
			}
			@Override
			public void componentMoved(ComponentEvent arg0) {
			}

			@Override
			public void componentHidden(ComponentEvent arg0) {
			}
		});
	}

	public AskUsersDialog(boolean singleValue) {
		this(singleValue, new TreeSet<User>());
	}

	private void initUI(){
		setModal(true);
		setTitle("Create a selection of users");
		setIconImage(Resources.getScaledIcon(Resources.iUser, 64).getImage());

		JPanel panel = new JPanel();	
		getContentPane().add(panel, BorderLayout.SOUTH);

		JButton btnOk = new JButton(Resources.getScaledIcon(Resources.iButtonApply, 24));
		btnOk.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {				
				dispose();
			}
		});
		panel.add(btnOk);

		JButton btnCancel = new JButton(Resources.getScaledIcon(Resources.iCross, 24));
		btnCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
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
		panel_0.setBorder(BorderFactory.createTitledBorder("Available users"));		
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
			@Override
			public boolean isCellEditable(int row, int column){
				return false;
			}
		};
		tableSource.setTableHeader(null);
		if (singleValue) tableSource.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tableSource.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount() == 2){
					addValues();
				}
			}
		});
		scrollPaneSource.setViewportView(tableSource);

		panel_0.add(searchField, BorderLayout.NORTH);

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
		button.setToolTipText("Add selected user(s) to your selection");
		button.addActionListener(new ActionListener() {
			@Override
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
		button_1.setToolTipText("Remove selected user(s) from your selection");
		button_1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				removeValues();
			}
		});
		GridBagConstraints gbc_button_1 = new GridBagConstraints();
		gbc_button_1.gridx = 0;
		gbc_button_1.gridy = 1;
		panel_middle.add(button_1, gbc_button_1);

		JScrollPane scrollPaneSelection = new JScrollPane();
		scrollPaneSelection.setBorder(BorderFactory.createTitledBorder("Your selection of users"));
		GridBagConstraints gbc_scrollPaneSelection = new GridBagConstraints();
		gbc_scrollPaneSelection.weighty = 1.0;
		gbc_scrollPaneSelection.weightx = 1.0;
		gbc_scrollPaneSelection.insets = new Insets(5, 5, 5, 5);
		gbc_scrollPaneSelection.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneSelection.gridx = 2;
		gbc_scrollPaneSelection.gridy = 0;
		panel_1.add(scrollPaneSelection, gbc_scrollPaneSelection);

		tSelectionModel = new DefaultTableModel(0,1);
		tableSelection = new JTable(tSelectionModel){
			@Override
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

		waitingPanel = new WaitingPanel();
		setGlassPane(waitingPanel);
	}	

	private void updateSourceTable(){
		List<User> sortedList = new ArrayList<User>(values);
		sortedList.removeAll(selection);
		Object[][] data = new Object[sortedList.size()][1];
		int row = 0;
		for (User o : sortedList){
			data[row++][0] = o;
		}
		tSourceModel = new DefaultTableModel(data, new String[] {"Available users"});
		sorter = new TableRowSorter<DefaultTableModel>(tSourceModel);
		tableSource.setModel(tSourceModel);		
		tableSource.setRowSorter(sorter);
		searchField.setSorter(sorter);
		searchField.applyFilter();		
	}

	private void updateSelectionTable(){
		List<User> sortedList = new ArrayList<User>(selection);
		Object[][] data = new Object[sortedList.size()][1];
		int row = 0;
		for (User o : sortedList){
			data[row++][0] = o;
		}
		tSelectionModel = new DefaultTableModel(data, new String[] {"Selected users"});
		tableSelection.setModel(tSelectionModel);		
	}

	private void addValues(){		
		if (singleValue && !selection.isEmpty()){
			JOptionPane.showMessageDialog(this, "You can only choose one user.", "Too many values", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}else{
			for (int row : tableSource.getSelectedRows()){
				selection.add((User)tableSource.getValueAt(row, 0));
			}
			updateSourceTable();
			updateSelectionTable();
		}
	}

	private void removeValues(){
		for (int row : tableSelection.getSelectedRows()){
			values.add((User)tableSelection.getValueAt(row, 0));
			selection.remove(tableSelection.getValueAt(row, 0));
		}
		updateSourceTable();
		updateSelectionTable();
	}

	public Set<User> getSelection(){
		return selection;
	}

	//Overridden so we can exit when window is closed
	@Override
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
