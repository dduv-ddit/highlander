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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.Resources.Palette;
import be.uclouvain.ngs.highlander.UI.dialog.ProfileTree.Action;
import be.uclouvain.ngs.highlander.UI.misc.WaitingPanel;
import be.uclouvain.ngs.highlander.administration.users.User.UserData;
import be.uclouvain.ngs.highlander.datatype.HPOTerm;
import be.uclouvain.ngs.highlander.datatype.Reference;

public class AskListOfHPOTermDialog extends JDialog {

	private String listName = null;
	final private Reference reference;
	private boolean singleValue = false;
	
	private DefaultTableModel tableSearchResultsTermsModel;
	private DefaultTableModel tableSearchResultsDiseasesModel;
	private DefaultTableModel tableSearchResultsDiseasesModelAssTerms;
	private DefaultTableModel tableSearchResultsGenesModel;
	private DefaultTableModel tableSearchResultsGenesModelAssTerms;
	private DefaultTableModel tableDiseasesModel;
	private DefaultTableModel tableGenesModel;
	private JTable tableSearchResultsTerms;
	private JTable tableSearchResultsDiseases;
	private JTable tableSearchResultsDiseasesAssTerms;
	private JTable tableSearchResultsGenes;
	private JTable tableSearchResultsGenesAssTerms;
	private JTable tableDiseases;
	private JTable tableGenes;
	private JTextPane paneDescrition;
	private DefaultMutableTreeNode root;
	private DefaultTreeModel treeModel;
	private JTree tree;
	private DefaultTableModel tSelectionModel;
	private JTable tableSelection;

	private boolean frameVisible = true;
	private volatile String query = "";
	private boolean modifyingTableSelection = false;
	private boolean modifyingTableTerms = false;
	private boolean modifyingTableDiseases = false;
	private boolean modifyingTableGenes = false;
	private boolean modifyingTree = false;
	private HPOTerm displayedTerm = null;
	
	private Set<HPOTerm> selection = new TreeSet<>();
	
	static private WaitingPanel waitingPanel;
	
	public AskListOfHPOTermDialog(Reference reference){
		this(false, reference, new TreeSet<HPOTerm>());
	}
	
	public AskListOfHPOTermDialog(Reference reference, Set<HPOTerm> phenotypes){
		this(false, reference, phenotypes);
	}
	
	public AskListOfHPOTermDialog(boolean singleValue, Reference reference, Set<HPOTerm> phenotypes){
		this.singleValue = singleValue;
		this.reference = reference;
		selection.addAll(phenotypes);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int width = screenSize.width - (screenSize.width/3*2);
		int height = screenSize.height - (screenSize.height/6);
		setSize(new Dimension(width,height));
		initUI();
		this.addComponentListener(new ComponentListener() {
			@Override
			public void componentShown(ComponentEvent arg0) {
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
		new Thread(new SearchTermsEngine(), "AskHPOTerm.SearchTermsEngine").start();
		new Thread(new SearchDiseasesEngine(), "AskHPOTerm.SearchDiseasesEngine").start();
		new Thread(new SearchGenesEngine(), "AskHPOTerm.SearchGenesEngine").start();
	}
	
	private void initUI(){
		setTitle("Human Phenotype Ontology");
		setIconImage(Resources.getScaledIcon(Resources.iHPO, 64).getImage());
		setModal(true);

		JPanel southPanel = new JPanel();
		getContentPane().add(southPanel, BorderLayout.SOUTH);
		
		JButton btnCancel = new JButton(Resources.getScaledIcon(Resources.iCross, 32));
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				selection.clear();
				frameVisible = false;
				dispose();
			}
		});
		southPanel.add(btnCancel);
		
		JButton btnValidate = new JButton(Resources.getScaledIcon(Resources.iButtonApply, 32));
		btnValidate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				frameVisible = false;
				dispose();
			}
		});
		southPanel.add(btnValidate);
		
		GridBagLayout gbl_center = new GridBagLayout();
		gbl_center.rowWeights = new double[]{0.0};
		gbl_center.columnWeights = new double[]{1.0, 0.0, 1.0};
		JPanel northPanel = new JPanel(gbl_center);
		getContentPane().add(northPanel, BorderLayout.NORTH);

		northPanel.add(getPanelFullSearch(), new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));		
		northPanel.add(getPanelAddRemove(), new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		
		JPanel selectionPanel = new JPanel(new BorderLayout());
		selectionPanel.add(getPanelSelectionControls(), BorderLayout.NORTH);
		selectionPanel.add(getPanelSelection(), BorderLayout.CENTER);
		northPanel.add(selectionPanel, new GridBagConstraints(2, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
		
		getContentPane().add(getPanelFullDescription(), BorderLayout.CENTER);

		waitingPanel = new WaitingPanel();
		setGlassPane(waitingPanel);
	}
	
	protected void processWindowEvent(WindowEvent e) {
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			selection.clear();;
			frameVisible = false;
			dispose();
		}
	}

	private JPanel getPanelFullDescription() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(new TitledBorder(null, "Selected HPO term details", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panel.add(getPanelTree(), BorderLayout.WEST);
		JPanel hpoDescriptionMainPanel = new JPanel(new GridBagLayout());
		panel.add(hpoDescriptionMainPanel, BorderLayout.CENTER);
		hpoDescriptionMainPanel.add(getPanelDescription(), new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
		hpoDescriptionMainPanel.add(getPanelAssociations(), new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
		return panel;
	}
	
	private JPanel getPanelFullSearch() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(getPanelSearch(), BorderLayout.NORTH);
		panel.add(getPanelSearchResults(), BorderLayout.CENTER);
		return panel;
	}
	
	private JPanel getPanelAddRemove() {
		JPanel panel = new JPanel(new GridBagLayout());
		
		JButton buttonAdd = new JButton(Resources.getScaledIcon(Resources.iArrowDoubleRight, 24));
		buttonAdd.setToolTipText("Add selected HPO term(s) to your selection");
		buttonAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					public void run() {
						addValues();
					}
				}).start();
			}
		});
		GridBagConstraints gbc_button = new GridBagConstraints();
		gbc_button.insets = new Insets(0, 0, 5, 0);
		gbc_button.gridx = 0;
		gbc_button.gridy = 0;
		panel.add(buttonAdd, gbc_button);

		JButton buttonRemove = new JButton(Resources.getScaledIcon(Resources.iArrowDoubleLeft, 24));
		buttonRemove.setToolTipText("Remove selected HPO term(s) from your selection");
		buttonRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				removeValues();
			}
		});
		GridBagConstraints gbc_button_1 = new GridBagConstraints();
		gbc_button_1.gridx = 0;
		gbc_button_1.gridy = 1;
		panel.add(buttonRemove, gbc_button_1);

		return panel;
	}
	
	private void updateSelectionTable(){
		Object[][] data = new Object[selection.size()][1];
		int row = 0;
		for (HPOTerm o : selection){
			data[row++][0] = o;
		}
		tSelectionModel = new DefaultTableModel(data, new String[] {"Selected phenotypes"});
		tableSelection.setModel(tSelectionModel);		
	}

	private void addValues(){		
		if (singleValue && !selection.isEmpty()){
			JOptionPane.showMessageDialog(this, "You can only choose one HPO term.", "Too many values", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}else{
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(true);
					waitingPanel.start();
				}
			});
			modifyingTableSelection = true;
			for (int row : tableSearchResultsTerms.getSelectedRows()) {
				int id = Integer.valueOf(tableSearchResultsTerms.getModel().getValueAt(row, 0).toString());
				try {
					selection.add(new HPOTerm(id, reference));
				}catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			for (int row : tableSearchResultsDiseasesAssTerms.getSelectedRows()) {
				int id = Integer.valueOf(tableSearchResultsDiseasesAssTerms.getModel().getValueAt(row, 0).toString());
				try {
					selection.add(new HPOTerm(id, reference));
				}catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			for (int row : tableSearchResultsGenesAssTerms.getSelectedRows()) {
				int id = Integer.valueOf(tableSearchResultsGenesAssTerms.getModel().getValueAt(row, 0).toString());
				try {
					selection.add(new HPOTerm(id, reference));
				}catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			updateSelectionTable();
			modifyingTableSelection = false;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(true);
					waitingPanel.stop();
				}
			});
		}
	}

	private void removeValues(){
		modifyingTableSelection = true;
		for (int row : tableSelection.getSelectedRows()){
			selection.remove(tableSelection.getValueAt(row, 0));
		}
		updateSelectionTable();
		modifyingTableSelection = false;
	}

	private JPanel getPanelSelectionControls() {
		JPanel panel = new JPanel();
		JButton btnSaveList = new JButton(Resources.getScaledIcon(Resources.iDbSave, 40));
		btnSaveList.setToolTipText("Save current list of HPO terms in your profile");
		btnSaveList.setPreferredSize(new Dimension(54,54));
		btnSaveList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				saveList();
			}
		});
		panel.add(btnSaveList);
		JButton btnLoadList = new JButton(Resources.getScaledIcon(Resources.iDbLoad, 40));
		btnLoadList.setToolTipText("Load a list of HPO terms from your profile");
		btnLoadList.setPreferredSize(new Dimension(54,54));
		btnLoadList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String listname = ProfileTree.showProfileDialog(AskListOfHPOTermDialog.this, Action.LOAD, UserData.PHENOTYPES, reference.getName());
				if (listname != null){
					loadList(listname);
				}
			}
		});
		panel.add(btnLoadList);
		return panel;
	}
	
	private JPanel getPanelSelection() {
		JPanel panel = new JPanel(new BorderLayout());
		JScrollPane scroll = new JScrollPane();
		scroll.setBorder(new TitledBorder(null, "Selected HPO Terms", TitledBorder.LEADING, TitledBorder.TOP, null, null));
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
		tableSelection.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent event) {
				if (event.getValueIsAdjusting() || modifyingTableSelection)
					return;
				if (tableSelection.getSelectedRow() >= 0) {
					showHPOTerm(((HPOTerm)(tableSelection.getModel().getValueAt(tableSelection.getSelectedRow(), 0))).getId());					
					tableSearchResultsTerms.clearSelection();
					tableSearchResultsDiseasesAssTerms.clearSelection();
					tableSearchResultsGenesAssTerms.clearSelection();
				}
			}
		}); 
		scroll.setViewportView(tableSelection);
		panel.add(scroll, BorderLayout.CENTER);
		return panel;
	}
	
	private JPanel getPanelSearch() {
		JPanel panel = new JPanel(new GridBagLayout());
		JTextField fieldQuery = new JTextField();
		fieldQuery.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent arg0) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						//search(fieldQuery.getText());
						query = fieldQuery.getText();
					}
				});
			}
			public void keyTyped(KeyEvent arg0) {			}
			public void keyPressed(KeyEvent arg0) {			}
		});
		panel.add(new JLabel("Search for", Resources.getScaledIcon(Resources.iHPO, 24), SwingConstants.LEADING), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		panel.add(fieldQuery, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		return panel;		
	}
	
	public class SearchTermsEngine implements Runnable {

		private volatile String query = "";        

		public void run() {
			while (frameVisible) {
				if (this.query != AskListOfHPOTermDialog.this.query) {
					this.query = AskListOfHPOTermDialog.this.query;
					searchTerms(this.query);
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	public class SearchDiseasesEngine implements Runnable {
		
		private volatile String query = "";        
		
		public void run() {
			while (frameVisible) {
				if (this.query != AskListOfHPOTermDialog.this.query) {
					this.query = AskListOfHPOTermDialog.this.query;
					searchDiseases(this.query);
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}
		}
	}
	
	public class SearchGenesEngine implements Runnable {
		
		private volatile String query = "";        
		
		public void run() {
			while (frameVisible) {
				if (this.query != AskListOfHPOTermDialog.this.query) {
					this.query = AskListOfHPOTermDialog.this.query;
					searchGenes(this.query);
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}
		}
	}
		
	private void searchTerms(String query) {
		modifyingTableTerms = true;
		Map<String, Integer> results = HPOTerm.searchNamesAndSynonyms(query, reference);
		Object[][] data = new Object[results.size()][2];
		int row = 0;
		for (String o : results.keySet()){
			data[row][0] = results.get(o);
			data[row][1] = o;
			row++;
		}
		tableSearchResultsTermsModel = new DefaultTableModel(data, new String[] {"Id","Terms"});
		tableSearchResultsTerms.setModel(tableSearchResultsTermsModel);	
		tableSearchResultsTerms.removeColumn(tableSearchResultsTerms.getColumnModel().getColumn(0));
		modifyingTableTerms = false;
	}
	
	private void searchDiseases(String query) {
		Map<String, Map<String,Integer>> results = HPOTerm.searchDiseases(query, reference);
		modifyingTableDiseases = true;
		Object[][] data = new Object[results.size()][2];
		int row = 0;
		for (String o : results.keySet()){
			StringBuilder sb = new StringBuilder();
			for(Iterator<Entry<String,Integer>> it = results.get(o).entrySet().iterator() ; it.hasNext() ;) {
				Entry<String,Integer> e = it.next();
				sb.append(e.getKey()+"|"+e.getValue());
				if (it.hasNext()) sb.append(";");
			}
			data[row][0] = sb.toString();
			data[row][1] = o;
			row++;
		}
		tableSearchResultsDiseasesModel = new DefaultTableModel(data, new String[] {"Ids","Diseases"});
		tableSearchResultsDiseases.setModel(tableSearchResultsDiseasesModel);	
		tableSearchResultsDiseases.removeColumn(tableSearchResultsDiseases.getColumnModel().getColumn(0));
		tableSearchResultsDiseasesAssTerms.setModel(new DefaultTableModel(0,0));
		modifyingTableDiseases = false;
	}
		
	private void searchGenes(String query) {
		modifyingTableGenes = true;
		Map<String, Map<String,Integer>> results = HPOTerm.searchGenes(query, reference);
		Object[][] data = new Object[results.size()][2];
		int row = 0;
		for (String o : results.keySet()){
			StringBuilder sb = new StringBuilder();
			for(Iterator<Entry<String,Integer>> it = results.get(o).entrySet().iterator() ; it.hasNext() ;) {
				Entry<String,Integer> e = it.next();
				sb.append(e.getKey()+"|"+e.getValue());
				if (it.hasNext()) sb.append(";");
			}
			data[row][0] = sb.toString();
			data[row][1] = o;
			row++;
		}
		tableSearchResultsGenesModel = new DefaultTableModel(data, new String[] {"Ids","Diseases"});
		tableSearchResultsGenes.setModel(tableSearchResultsGenesModel);	
		tableSearchResultsGenes.removeColumn(tableSearchResultsGenes.getColumnModel().getColumn(0));
		tableSearchResultsGenesAssTerms.setModel(new DefaultTableModel(0,0));
		modifyingTableGenes = false;
	}
	
	private JPanel getPanelSearchResults() {
		JPanel panel = new JPanel(new BorderLayout());
		JTabbedPane tabs = new JTabbedPane();
		panel.add(tabs, BorderLayout.CENTER);
		tabs.addTab("HPO terms matching", getPanelSearchResultsTerms());
		tabs.addTab("Diseases matching", getPanelSearchResultsDiseases());
		tabs.addTab("Genes matching", getPanelSearchResultsGenes());
		return panel;		
	}
	
	private JPanel getPanelSearchResultsTerms() {
		JPanel panel = new JPanel(new BorderLayout());
		JScrollPane scroll = new JScrollPane();
		tableSearchResultsTerms = new JTable(){
			public boolean isCellEditable(int row, int column){
				return false;
			}
		};
		tableSearchResultsTerms.setTableHeader(null);
		tableSearchResultsTerms.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		tableSearchResultsTerms.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent event) {
				if (event.getValueIsAdjusting() || modifyingTableTerms)
					return;
				if (tableSearchResultsTerms.getSelectedRow() >= 0) {
					int id = Integer.valueOf(tableSearchResultsTerms.getModel().getValueAt(tableSearchResultsTerms.getSelectedRow(), 0).toString());
					showHPOTerm(id);					
					tableSelection.clearSelection();
					tableSearchResultsDiseasesAssTerms.clearSelection();
					tableSearchResultsGenesAssTerms.clearSelection();
				}
			}
		}); 
		scroll.setViewportView(tableSearchResultsTerms);
		panel.add(scroll, BorderLayout.CENTER);
		return panel;
	}
	
	private JPanel getPanelSearchResultsDiseases() {
		JPanel panel = new JPanel(new BorderLayout());
		JScrollPane scroll = new JScrollPane();
		tableSearchResultsDiseases = new JTable(){
			public boolean isCellEditable(int row, int column){
				return false;
			}
		};
		tableSearchResultsDiseases.setTableHeader(null);
		tableSearchResultsDiseases.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);		
		tableSearchResultsDiseases.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent event) {
				if (event.getValueIsAdjusting() || modifyingTableDiseases)
					return;
				if (tableSearchResultsDiseases.getSelectedRow() >= 0) {
					String[] resultsTerms = tableSearchResultsDiseases.getModel().getValueAt(tableSearchResultsDiseases.getSelectedRow(), 0).toString().split(";");
					Object[][] dataTerms = new Object[resultsTerms.length][2];
					int row = 0;
					for (String o : resultsTerms){
						dataTerms[row][0] = o.split("\\|")[1];
						dataTerms[row][1] = o.split("\\|")[0];
						row++;
					}
					tableSearchResultsDiseasesModelAssTerms = new DefaultTableModel(dataTerms, new String[] {"Id","Terms"});
					tableSearchResultsDiseasesAssTerms.setModel(tableSearchResultsDiseasesModelAssTerms);	
					tableSearchResultsDiseasesAssTerms.removeColumn(tableSearchResultsDiseasesAssTerms.getColumnModel().getColumn(0));
				}
			}
		}); 
		scroll.setViewportView(tableSearchResultsDiseases);
		panel.add(scroll, BorderLayout.CENTER);
		JScrollPane scrollAssTerms = new JScrollPane();
		scrollAssTerms.setBorder(new TitledBorder(null, "Associated HPO Terms", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		tableSearchResultsDiseasesAssTerms = new JTable(){
			public boolean isCellEditable(int row, int column){
				return false;
			}
		};
		tableSearchResultsDiseasesAssTerms.setTableHeader(null);
		tableSearchResultsDiseasesAssTerms.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		tableSearchResultsDiseasesAssTerms.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent event) {
				if (event.getValueIsAdjusting() || modifyingTableDiseases)
					return;
				if (tableSearchResultsDiseasesAssTerms.getSelectedRow() >= 0) {
					int id = Integer.valueOf(tableSearchResultsDiseasesAssTerms.getModel().getValueAt(tableSearchResultsDiseasesAssTerms.getSelectedRow(), 0).toString());
					showHPOTerm(id);					
					tableSelection.clearSelection();
					tableSearchResultsTerms.clearSelection();
					tableSearchResultsGenesAssTerms.clearSelection();
				}
			}
		}); 
		scrollAssTerms.setViewportView(tableSearchResultsDiseasesAssTerms);
		panel.add(scrollAssTerms, BorderLayout.EAST);
		return panel;
	}
	
	private JPanel getPanelSearchResultsGenes() {
		JPanel panel = new JPanel(new BorderLayout());
		JScrollPane scroll = new JScrollPane();
		tableSearchResultsGenes = new JTable(){
			public boolean isCellEditable(int row, int column){
				return false;
			}
		};
		tableSearchResultsGenes.setTableHeader(null);
		tableSearchResultsGenes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tableSearchResultsGenes.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent event) {
				if (event.getValueIsAdjusting() || modifyingTableGenes)
					return;
				if (tableSearchResultsGenes.getSelectedRow() >= 0) {
					String[] resultsTerms = tableSearchResultsGenes.getModel().getValueAt(tableSearchResultsGenes.getSelectedRow(), 0).toString().split(";");
					Object[][] dataTerms = new Object[resultsTerms.length][2];
					int row = 0;
					for (String o : resultsTerms){
						dataTerms[row][0] = o.split("\\|")[1];
						dataTerms[row][1] = o.split("\\|")[0];
						row++;
					}
					tableSearchResultsGenesModelAssTerms = new DefaultTableModel(dataTerms, new String[] {"Id","Terms"});
					tableSearchResultsGenesAssTerms.setModel(tableSearchResultsGenesModelAssTerms);	
					tableSearchResultsGenesAssTerms.removeColumn(tableSearchResultsGenesAssTerms.getColumnModel().getColumn(0));					
				}
			}
		}); 
		scroll.setViewportView(tableSearchResultsGenes);
		panel.add(scroll, BorderLayout.CENTER);
		JScrollPane scrollAssTerms = new JScrollPane();
		scrollAssTerms.setBorder(new TitledBorder(null, "Associated HPO Terms", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		tableSearchResultsGenesAssTerms = new JTable(){
			public boolean isCellEditable(int row, int column){
				return false;
			}
		};
		tableSearchResultsGenesAssTerms.setTableHeader(null);
		tableSearchResultsGenesAssTerms.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		tableSearchResultsGenesAssTerms.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent event) {
				if (event.getValueIsAdjusting() || modifyingTableGenes)
					return;
				if (tableSearchResultsGenesAssTerms.getSelectedRow() >= 0) {
					int id = Integer.valueOf(tableSearchResultsGenesAssTerms.getModel().getValueAt(tableSearchResultsGenesAssTerms.getSelectedRow(), 0).toString());
					showHPOTerm(id);					
					tableSelection.clearSelection();
					tableSearchResultsTerms.clearSelection();
					tableSearchResultsDiseasesAssTerms.clearSelection();
				}
			}
		}); 
		scrollAssTerms.setViewportView(tableSearchResultsGenesAssTerms);
		panel.add(scrollAssTerms, BorderLayout.EAST);
		return panel;
	}
	
	private JPanel getPanelTree() {
		JPanel panel = new JPanel(new BorderLayout());
		root = new DefaultMutableTreeNode(null, true);
		treeModel = new DefaultTreeModel(root);
		tree = new JTree(treeModel);
		tree.setRootVisible(false);
		tree.setCellRenderer(new HPOTreeCellRenderer());
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent e) {
      	if (!modifyingTree) {
        	DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        	HPOTerm hpo = (HPOTerm)selectedNode.getUserObject();
        	if (!hpo.equals(displayedTerm)) {
        		modifyingTree = true;
        		showHPOTerm(hpo.getId());
  					tableSelection.clearSelection();
  					tableSearchResultsTerms.clearSelection();
  					tableSearchResultsDiseasesAssTerms.clearSelection();
  					tableSearchResultsGenesAssTerms.clearSelection();
        		modifyingTree = false;
        	}      		
      	}
      }
    }) ;
		panel.add(new JScrollPane(tree), BorderLayout.CENTER);
		return panel;
	}
	
	public class HPOTreeCellRenderer extends DefaultTreeCellRenderer {
		public Component getTreeCellRendererComponent(JTree tree,	Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
			if (node.getUserObject() != null) {
				HPOTerm hpo = (HPOTerm)node.getUserObject();
				int level = node.getLevel();
				if (displayedTerm.getOntologyId().equalsIgnoreCase("HP:0000001")) {
					level++;
				}
				switch(level) {
				case 1:
					setText("<html>"+hpo.getName()+"</html>");
					setIcon(getColoredTreeIcon(24, Resources.getColor(Palette.Indigo, 300, false), level));
					break;
				case 2:
					setText("<html><b>"+hpo.getName()+"</b></html>");
					setIcon(getColoredTreeIcon(24, Resources.getColor(Palette.Red, 300, false), level));
					break;
				case 3:
					setText("<html>"+hpo.getName()+"</html>");
					setIcon(getColoredTreeIcon(24, Resources.getColor(Palette.Teal, 300, false), level));
					break;
				default:
					setText("<html>"+hpo.getName()+"</html>");
					setIcon(getColoredTreeIcon(24, Resources.getColor(Palette.DeepOrange, 300, false), node.getLevel()));
					break;
				}
			}
			return this;
		}
	}
	
  public static ImageIcon getColoredTreeIcon(int size, Color color, int level){
  	BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);  	
  	Graphics g = image.getGraphics();
  	g.setColor(new Color(0, 0, 0, 0));
  	g.fillRect(0, 0, size, size);
  	g.setColor(color);
		switch(level) {
		case 1:
			g.fillRect(1, 1, size, size/3);
			g.fillRect(2*size/3, 1, size/3, size);
			break;
		case 2:
			g.fillRect(1, size/3, size, size/3);
			break;
		case 3:
			g.fillRect(1, size/3, size, size/3);
			g.fillRect(1, 1, size/3, size);
			break;
		default:
			g.fillRect(1, 1, size, size);
			break;
		}
		return new ImageIcon(image);
  }

  private JPanel getPanelDescription() {
		JPanel panel = new JPanel(new BorderLayout());
		paneDescrition = new JTextPane();
		paneDescrition.setContentType("text/html");
		paneDescrition.setEditable(false);
		panel.add(paneDescrition, BorderLayout.CENTER);
		return panel;
	}
	
  private JPanel getPanelAssociations() {		
		JPanel panel = new JPanel(new BorderLayout());
		JTabbedPane tabs = new JTabbedPane();
		panel.add(tabs, BorderLayout.CENTER);
		JScrollPane scrollDiseases = new JScrollPane();
		tableDiseases = new JTable(){
			public boolean isCellEditable(int row, int column){
				return false;
			}
		};
		scrollDiseases.setViewportView(tableDiseases);
		tabs.addTab("Diseases Associations", scrollDiseases);
		JScrollPane scrollGenes = new JScrollPane();
		tableGenes = new JTable(){
			public boolean isCellEditable(int row, int column){
				return false;
			}
		};
		scrollGenes.setViewportView(tableGenes);
		tabs.addTab("Genes Associations", scrollGenes);
		return panel;
	}
	
	public Set<HPOTerm> getSelection(){
		return selection;
	}
	
	public void saveList(){
		String name = ProfileTree.showProfileDialog(this, Action.SAVE, UserData.PHENOTYPES, reference.getName(), "Save list of HPO terms to your profile", listName);
		saveList(name);
	}

	public void saveList(String name){
		try {
			if (name == null) return;
			if (Highlander.getLoggedUser().doesPersonalDataExists(UserData.PHENOTYPES, reference.getName(), name)){
				int yesno = JOptionPane.showConfirmDialog(new JFrame(), 
						"You already have a "+UserData.PHENOTYPES.getName()+" named '"+name.replace("~", " -> ")+"', do you want to overwrite it ?", 
						"Overwriting element in your profile", JOptionPane.YES_NO_OPTION , JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iDbSave,64));
				if (yesno == JOptionPane.NO_OPTION)	return;
			}

			listName = name;
			Highlander.getLoggedUser().savePhenotypes(listName, reference, new ArrayList<>(selection));
		} catch (Exception ex) {
			Tools.exception(ex);
			JOptionPane.showMessageDialog(AskListOfHPOTermDialog.this, Tools.getMessage("Error", ex), "Save current list of HPO terms in your profile", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
	}

	public void loadList(String key){
		listName = key;
		try {
			List<HPOTerm> list = Highlander.getLoggedUser().loadPhenotypes(reference, listName);			
			if (list.isEmpty()) {
				list = Highlander.getLoggedUser().loadPhenotypes(listName);
			}
			selection.addAll(list);
		} catch (Exception ex) {
			Tools.exception(ex);
			JOptionPane.showMessageDialog(AskListOfHPOTermDialog.this, Tools.getMessage("Error", ex), 
					"Load list of HPO terms from your profile", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
		updateSelectionTable();
	}

	public void showHPOTerm(int dbId) {
		try {
			HPOTerm hpo = new HPOTerm(dbId, reference);
			displayedTerm = hpo;
			//Description
			paneDescrition.setText(hpo.getHTMLDescription());
			//Associated Diseases
			Object[][] dataDisease = new Object[hpo.getAssociatedDiseases().size()][2];
			int row = 0;
			for (String o : hpo.getAssociatedDiseases()){
				dataDisease[row][0] = o;
				StringBuilder sb = new StringBuilder();
				for (Iterator<String> it = hpo.getAssociatedGenes(o.toString()).iterator() ; it.hasNext() ; ) {
					sb.append(it.next());
					if (it.hasNext()) sb.append(", ");
				}
				dataDisease[row][1] = sb.toString();
				row++;
			}
			tableDiseasesModel = new DefaultTableModel(dataDisease, new String[] {"Disease","Associated Genes"});
			tableDiseases.setModel(tableDiseasesModel);
			//Associated Genes
			Object[][] dataGene = new Object[hpo.getAssociatedGenes().size()][2];
			row = 0;
			for (String o : hpo.getAssociatedGenes()){
				dataGene[row][0] = o;
				StringBuilder sb = new StringBuilder();
				for (Iterator<String> it = hpo.getAssociatedDiseases(o.toString()).iterator() ; it.hasNext() ; ) {
					sb.append(it.next());
					if (it.hasNext()) sb.append(", ");
				}
				dataGene[row][1] = sb.toString();
				row++;
			}
			tableGenesModel = new DefaultTableModel(dataGene, new String[] {"Gene Symbol","Associated Diseases"});
			tableGenes.setModel(tableGenesModel);
			//Parents and children terms
			tree.setRootVisible(true);
			root.removeAllChildren();
			treeModel.reload();
			DefaultMutableTreeNode parent = root;
			for (Iterator<HPOTerm> it = hpo.getParents().iterator() ; it.hasNext() ; ) {
				HPOTerm term = it.next();
				DefaultMutableTreeNode node = new DefaultMutableTreeNode(term);
				parent.add(node);
				if (!it.hasNext()) parent = node;				
			}
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(hpo);
			parent.add(node);
			parent = node;
			for (Iterator<HPOTerm> it = hpo.getChildren().iterator() ; it.hasNext() ; ) {
				HPOTerm term = it.next();
				node = new DefaultMutableTreeNode(term);
				parent.add(node);
			}
			for (int i = 0; i < tree.getRowCount(); i++) {
		    tree.expandRow(i);
			}
			tree.setRootVisible(false);
		}catch(Exception ex) {
			ex.printStackTrace();
			String hpo = tableSearchResultsTerms.getValueAt(tableSearchResultsTerms.getSelectedRow(), 1).toString();
			paneDescrition.setText("<html><b>"+hpo+"</b><br>Error: cannot retreive HPO term description</html>");
		}
	}
}
