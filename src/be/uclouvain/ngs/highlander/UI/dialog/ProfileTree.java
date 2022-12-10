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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.dialog.FilteringTree.FilterTreeCellRenderer;
import be.uclouvain.ngs.highlander.UI.dialog.FilteringTree.FilterTreeModel;
import be.uclouvain.ngs.highlander.UI.misc.WaitingPanel;
import be.uclouvain.ngs.highlander.administration.users.User;
import be.uclouvain.ngs.highlander.administration.users.User.UserData;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
import be.uclouvain.ngs.highlander.datatype.FiltersTemplate;
import be.uclouvain.ngs.highlander.datatype.HeatMapCriterion;
import be.uclouvain.ngs.highlander.datatype.HighlightCriterion;
import be.uclouvain.ngs.highlander.datatype.HighlightingRule;
import be.uclouvain.ngs.highlander.datatype.Reference;
import be.uclouvain.ngs.highlander.datatype.SortingCriterion;
import be.uclouvain.ngs.highlander.datatype.VariantsList;
import be.uclouvain.ngs.highlander.datatype.filter.ComboFilter;
import be.uclouvain.ngs.highlander.datatype.filter.Filter;

import javax.swing.border.BevelBorder;

public class ProfileTree extends JDialog implements TreeWillExpandListener {
	
	public enum Action {MANAGE, LOAD, SAVE, FOLDER}
	
	private final Highlander mainFrame;
	private final Action action;
	private final UserData userDataFilter;

	private Map<ProfileNode, UserData> linkedData = new HashMap<ProfileNode, UserData>();

	private ProfileTreeModel model;
	private JTree tree;
	private JScrollPane scrollDescription;
	private JLabel history;

	private WaitingPanel waitingPanel;
	
	private JButton btnFolderNew;
	private JButton btnRename;
	private JButton btnEdit;
	private JButton btnDelete;
	private JButton btnCopy;
	private JButton btnShare;
	private JButton createUserValueList;
	private JButton createUserIntervalsList;
	private JButton createUserPhenotypesList;
	private JButton createUserTemplateList;
	private JButton okButton;
	private JButton cancelButton;
	
	private JTextField userInput;
	
	private JTextField searchField = new JTextField();
	private Map<UserData, Map<String, List<String>>> searchResults;
	private JLabel searchResultsNumber = new JLabel("");
	private JButton searchPrecedent = new JButton(Resources.getScaledIcon(Resources.iArrowDoubleLeft, 24));
	private JButton searchNext = new JButton(Resources.getScaledIcon(Resources.iArrowDoubleRight, 24));
	private int searchResultsTotal = 0;
	private int searchResultsCurrent = 0;
	
	private static String selection = null;

	public ProfileTree(Window parent, Highlander mainFrame, Action action, UserData userDataFilter, Analysis analysisFilter, String title, String currentSelection){
		this(parent, mainFrame, action, userDataFilter, analysisFilter, null, null, title, currentSelection);
	}
	
	public ProfileTree(Window parent, Highlander mainFrame, Action action, UserData userDataFilter, Reference filterReference, String title, String currentSelection){
		this(parent, mainFrame, action, userDataFilter, null, filterReference, null, title, currentSelection);
	}
	
	public ProfileTree(Window parent, Highlander mainFrame, Action action, UserData userDataFilter, Field filterField, String title, String currentSelection){
		this(parent, mainFrame, action, userDataFilter, null, null, filterField, title, currentSelection);
	}
	
	public ProfileTree(Window parent, Highlander mainFrame, Action action, UserData userDataFilter, Analysis analysisFilter, Reference filterReference, Field filterField, String title, String currentSelection){
		super(parent);
		this.mainFrame = mainFrame;
		this.action = action;
		this.userDataFilter = userDataFilter;
		setTitle(title);
		model = new ProfileTreeModel(Highlander.getLoggedUser().getUsername(), userDataFilter, analysisFilter, filterReference, filterField);
		tree = new JTree(model);
		tree.setCellRenderer(new ProfileTreeCellRenderer());
		tree.addTreeWillExpandListener(this);
		switch(action){
		case LOAD:
			setModalityType(ModalityType.APPLICATION_MODAL);
			setIconImage(Resources.getScaledIcon(Resources.iLoad, 64).getImage());
			tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
			break;
		case SAVE:
			setModalityType(ModalityType.APPLICATION_MODAL);
			setIconImage(Resources.getScaledIcon(Resources.iSave, 64).getImage());
			tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
			break;
		case FOLDER:
			setModalityType(ModalityType.APPLICATION_MODAL);
			setIconImage(Resources.getScaledIcon(Resources.iFolder, 64).getImage());
			tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
			break;
		case MANAGE:
			setModalityType(ModalityType.MODELESS);
			setIconImage(Resources.getScaledIcon(Resources.iUserTree, 64).getImage());
			tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
			tree.setDragEnabled(true);
	    tree.setDropMode(DropMode.ON_OR_INSERT);
	    tree.setTransferHandler(new TreeTransferHandler(tree));
			break;
		}
		tree.addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent e) {
      	refreshButtons() ;
      	scrollDescription.setViewportView(showSelectedElement());
      	history.setText(showHistory());
      	if (ProfileTree.this.action == Action.SAVE){
      		ProfileNode selectedNode = (ProfileNode) tree.getLastSelectedPathComponent();
      		if (selectedNode.getUserData() != UserData.FOLDER){
      			userInput.setText(selectedNode.getKey());
      		}
      	}
      }
    }) ;
		initUI();
		refreshButtons();
		if (userDataFilter != null){
			String defaultAnalysis = (Highlander.getCurrentAnalysis() != null) ? Highlander.getCurrentAnalysis().toString() : "";
			ProfileNode target = (ProfileNode)model.getRoot();
			for (ProfileNode n : linkedData.keySet()){
				if (userDataFilter == null || linkedData.get(n) == userDataFilter){
					if (userDataFilter == UserData.VALUES && ((filterField != null) ? n.getKey().equals(filterField.getName()) : true)){
						target = n;
						break;
					}else if ((userDataFilter == UserData.INTERVALS || userDataFilter == UserData.PHENOTYPES) && ((filterReference != null) ? n.getKey().equals(filterReference.getName()) : true)){
						target = n;
						break;						
					}else if (n.getKey().equals((analysisFilter != null) ? analysisFilter.toString() : defaultAnalysis)){
						target = n;
						break;
					}
				}
			}
			tree.expandPath(new TreePath(model.getPathToRoot(target)));
			tree.setSelectionPath(new TreePath(model.getPathToRoot(target)));
			if(currentSelection == null) currentSelection = selection;
			if (currentSelection != null){
				String[] targetPath = currentSelection.split("~");
				for (int i=0 ; i < targetPath.length ; i++){
					for (int j=0 ; j < target.getChildCount() ; j++){
						if (((ProfileNode)target.getChildAt(j)).getKey().equals(targetPath[i])){
							target = (ProfileNode)target.getChildAt(j);
							break;
						}
					}
				}
				tree.expandPath(new TreePath(model.getPathToRoot(target)));
				tree.setSelectionPath(new TreePath(model.getPathToRoot(target)));
				if (ProfileTree.this.action == Action.SAVE){
					userInput.setText(targetPath[targetPath.length-1]);
				}
				/*
				 * Pose problem with save template if precendent selection was e.g. a filter inside a folder : will set selection as folder~filtername and the folder will be selected, creating a result like folder~folder~filtername
				 * Don't find out why the setting below was useful
				 * 
				if (userInput.getText().length() == 0) {
					userInput.setText(currentSelection);
				}
				*/
			}
		}
	}
	
	private void initUI(){
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setSize(new Dimension((int)(screenSize.width/2),(int)(screenSize.height/2.5)));
		
		JSplitPane splitpane = new JSplitPane();
		splitpane.setResizeWeight(0.3);
		getContentPane().add(splitpane, BorderLayout.CENTER);
		
		JPanel treePanel = new JPanel(new BorderLayout());
		splitpane.setLeftComponent(treePanel);
		
		JScrollPane scrollTree = new JScrollPane(tree);
		treePanel.add(scrollTree, BorderLayout.CENTER);
		
		JPanel searchPanel = new JPanel(new BorderLayout());
		searchPanel.add(new JLabel(Resources.getScaledIcon(Resources.iSearch, 30)), BorderLayout.WEST);
		searchField.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				searchProfile(searchField.getText());	
			}
		});
		searchPanel.add(searchField, BorderLayout.CENTER);
		JPanel searchResultsPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
		searchResultsPanel.add(searchResultsNumber);
		searchPrecedent.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				searchProfileGoto(false);
			}
		});
		searchResultsPanel.add(searchPrecedent);
		searchNext.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				searchProfileGoto(true);
			}
		});
		searchResultsPanel.add(searchNext);
		searchPanel.add(searchResultsPanel, BorderLayout.EAST);
		treePanel.add(searchPanel, BorderLayout.NORTH);
		
		scrollDescription = new JScrollPane();
		history = new JLabel();
		history.setBackground(Color.WHITE);
		history.setFont(history.getFont().deriveFont(Font.BOLD, history.getFont().getSize()+2));
		JPanel rightPanel = new JPanel(new BorderLayout());
		rightPanel.add(scrollDescription, BorderLayout.CENTER);
		rightPanel.add(history, BorderLayout.SOUTH);
		splitpane.setRightComponent(rightPanel);
		
		JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		getContentPane().add(northPanel, BorderLayout.NORTH);

		btnRename = new JButton(Resources.getScaledIcon(Resources.iEditPen, 40));
		btnRename.setToolTipText("Rename selected element");
		btnRename.setPreferredSize(new Dimension(54,54));
		btnRename.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					public void run(){
						if (canRenameSelection())
							renameElement((ProfileNode) tree.getLastSelectedPathComponent());
					}
				}, "ProfileTree.renameElement").start();
			}
		});
		northPanel.add(btnRename);
		
		btnEdit = new JButton(Resources.getScaledIcon(Resources.iEditWrench, 40));
		btnEdit.setToolTipText("Edit selected element");
		btnEdit.setPreferredSize(new Dimension(54,54));
		btnEdit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					public void run(){
						if (canEditSelection())
							editElement((ProfileNode) tree.getLastSelectedPathComponent());
					}
				}, "ProfileTree.editElement").start();
			}
		});
		northPanel.add(btnEdit);
		if (action != Action.MANAGE) btnEdit.setVisible(false);
		
		btnDelete = new JButton(Resources.getScaledIcon(Resources.iCross, 40));
		btnDelete.setToolTipText("Delete selected elements");
		btnDelete.setPreferredSize(new Dimension(54,54));
		btnDelete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
	  			public void run(){
	  				if (canDeleteSelection())
	  					deleteElement(tree.getSelectionPaths());
	  			}
	  		}, "ProfileTree.deleteElement").start();
			}
		});
		northPanel.add(btnDelete);
		if (action != Action.MANAGE) btnDelete.setVisible(false);
		
		btnCopy = new JButton(Resources.getScaledIcon(Resources.iCopy, 40));
		btnCopy.setToolTipText("Copy selected elements");
		btnCopy.setPreferredSize(new Dimension(54,54));
		btnCopy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					public void run(){
						if (canCopySelection())
							copyElement(tree.getSelectionPaths());
					}
				}, "ProfileTree.copyElement").start();
			}
		});
		northPanel.add(btnCopy);
		if (action != Action.MANAGE) btnCopy.setVisible(false);
		
		btnShare = new JButton(Resources.getScaledIcon(Resources.iUsers, 40));
		btnShare.setToolTipText("Share selected elements");
		btnShare.setPreferredSize(new Dimension(54,54));
		btnShare.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					public void run(){
						if (canShareSelection())
							if (tree.getSelectionPaths().length > 0){
								try{
									AskUsersDialog ask = new AskUsersDialog(false);
									Tools.centerWindow(ask, false);
									ask.setVisible(true);
									if (!ask.getSelection().isEmpty()){
										shareElement(tree.getSelectionPaths(), ask.getSelection());
									}
								}catch(Exception ex){
									Tools.exception(ex);
									JOptionPane.showMessageDialog(ProfileTree.this, Tools.getMessage("Cannot share selected element", ex), "Sharing element from profile", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));				
								}
							}
					}
				}, "ProfileTree.Share").start();
			}
		});
		northPanel.add(btnShare);
		if (action != Action.MANAGE) btnShare.setVisible(false);
		
		btnFolderNew = new JButton(Resources.getScaledIcon(Resources.iFolderNew, 40));
		btnFolderNew.setToolTipText("Create new folder under selected folder");
		btnFolderNew.setPreferredSize(new Dimension(54,54));
		btnFolderNew.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (canCreateElement((ProfileNode) tree.getLastSelectedPathComponent(), UserData.FOLDER))
					newFolder((ProfileNode) tree.getLastSelectedPathComponent());
			}
		});
		northPanel.add(btnFolderNew);
		
	  createUserValueList = new JButton(Resources.getScaledIcon(Resources.iUserListNew, 40));
	  createUserValueList.setPreferredSize(new Dimension(54,54));
	  createUserValueList.setToolTipText("Create a new list of values (like a sample or gene list)");
	  createUserValueList.addActionListener(new ActionListener() {
	  	public void actionPerformed(ActionEvent e) {
	  		new Thread(new Runnable(){
	  			public void run(){
	  				if (canCreateElement((ProfileNode) tree.getLastSelectedPathComponent(), UserData.VALUES))
	  					createValueList((ProfileNode) tree.getLastSelectedPathComponent());
	  			}
	  		}, "ProfileTree.createValueList").start();
	  	}
	  });
		northPanel.add(createUserValueList);
		if (action != Action.MANAGE) createUserValueList.setVisible(false);
			
	  createUserIntervalsList = new JButton(Resources.getScaledIcon(Resources.iUserIntervalsNew, 40));
	  createUserIntervalsList.setPreferredSize(new Dimension(54,54));
	  createUserIntervalsList.setToolTipText("Create a new list of genomic intervals");
	  createUserIntervalsList.addActionListener(new ActionListener() {
	  	public void actionPerformed(ActionEvent e) {
	  		new Thread(new Runnable(){
	  			public void run(){
	  				if (canCreateElement((ProfileNode) tree.getLastSelectedPathComponent(), UserData.INTERVALS))
	  					createIntervalsList((ProfileNode) tree.getLastSelectedPathComponent());
	  			}
	  		}, "ProfileTree.createIntervalsList").start();
	  	}
	  });
		northPanel.add(createUserIntervalsList);	  
		if (action != Action.MANAGE) createUserIntervalsList.setVisible(false);
		
		createUserPhenotypesList = new JButton(Resources.getScaledIcon(Resources.iUserHPONew, 40));
		createUserPhenotypesList.setPreferredSize(new Dimension(54,54));
		createUserPhenotypesList.setToolTipText("Create a new list of phenotypes (HPO terms)");
		createUserPhenotypesList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						if (canCreateElement((ProfileNode) tree.getLastSelectedPathComponent(), UserData.PHENOTYPES))
							createPhenotypesList((ProfileNode) tree.getLastSelectedPathComponent());
					}
				}, "ProfileTree.createPhenotypesList").start();
			}
		});
		northPanel.add(createUserPhenotypesList);	  
		if (action != Action.MANAGE) createUserPhenotypesList.setVisible(false);
		
		createUserTemplateList = new JButton(Resources.getScaledIcon(Resources.iUserTemplateNew, 40));
		createUserTemplateList.setPreferredSize(new Dimension(54,54));
		createUserTemplateList.setToolTipText("Create a filters template in your profile");
		createUserTemplateList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						if (canCreateElement((ProfileNode) tree.getLastSelectedPathComponent(), UserData.FILTERS_TEMPLATE))
							createFiltersTemplate((ProfileNode) tree.getLastSelectedPathComponent());
					}
				}, "ProfileTree.createFiltersTemplate").start();
			}
		});
		northPanel.add(createUserTemplateList);	  
		if (action != Action.MANAGE) createUserTemplateList.setVisible(false);
		
		JPanel southPanel = new JPanel(new GridBagLayout());
		getContentPane().add(southPanel, BorderLayout.SOUTH);
		if (action == Action.MANAGE) southPanel.setVisible(false);

		JLabel userInputLabel = new JLabel("Element name: ");
		southPanel.add(userInputLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 15, 2, 5), 0, 0));	  
		if (action != Action.SAVE) userInputLabel.setVisible(false);
		
		userInput = new JTextField(30);
		southPanel.add(userInput, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 5, 2, 5), 0, 0));
		userInput.getDocument().addDocumentListener(new DocumentListener() {
		  public void changedUpdate(DocumentEvent e) {
		  	refreshButtons();
		  }
		  public void removeUpdate(DocumentEvent e) {
		  	refreshButtons();
		  }
		  public void insertUpdate(DocumentEvent e) {
		  	refreshButtons();
		  }
		});
		if (action != Action.SAVE) userInput.setVisible(false);
		
		okButton = new JButton(Resources.getScaledIcon(Resources.iButtonApply, 30));
	  createUserIntervalsList.setToolTipText("Choose");
		okButton.setPreferredSize(new Dimension(42,42));
		okButton.addActionListener(new ActionListener() {
	  	public void actionPerformed(ActionEvent e) {
	  		new Thread(new Runnable(){
	  			public void run(){
	  				if (ProfileTree.this.action == Action.SAVE){
	  					if (Filter.containsForbiddenCharacters(userInput.getText())) {
	  						JOptionPane.showMessageDialog(new JFrame(), "You cannot use the following characters: "+Filter.getForbiddenCharacters(), "Saving in profile", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
	  						return;
	  					}
	  					ProfileNode selectedNode = (ProfileNode) tree.getLastSelectedPathComponent();
	  					if (selectedNode.getUserData() == UserData.FOLDER){
	  						selection = selectedNode.getFullPath();
	  					}else{
		  					selection = ((ProfileNode)selectedNode.getParent()).getFullPath();	  						
	  					}
	  					if (selection.length() > 0) selection += "~";
	  					selection += userInput.getText();
	  				}else if (ProfileTree.this.action == Action.LOAD){
	  					ProfileNode selectedNode = (ProfileNode) tree.getLastSelectedPathComponent();
	  					selection = selectedNode.getFullPath();
	  				}else if (ProfileTree.this.action == Action.FOLDER){
	  					selection = "";
	  					Object[] path = tree.getSelectionPath().getPath();
	  					for (int i=1 ; i < path.length ; i++){
	  						selection += ((ProfileNode)path[i]).getKey();
	  						if (i < path.length-1){
	  							selection += "~";
	  						}
	  					}
	  				}
	  				dispose();
	  			}
	  		}, "ProfileTree.OK").start();
	  	}
		});
		southPanel.add(okButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 5, 2, 5), 0, 0));	  
		
		cancelButton = new JButton(Resources.getScaledIcon(Resources.iCross, 30));
		cancelButton.setToolTipText("Cancel");
	  cancelButton.setPreferredSize(new Dimension(42,42));
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						selection = null;
						dispose();
					}
				}, "ProfileTree.Cancel").start();
			}
		});
		southPanel.add(cancelButton, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 5, 2, 15), 0, 0));	  
	
		waitingPanel = new WaitingPanel();
		setGlassPane(waitingPanel);
	}

	public String getSelection(){
		return selection;
	}
		
	private void newFolder(ProfileNode node){
		if (canCreateElement(node, UserData.FOLDER)){
			Object res = JOptionPane.showInputDialog(ProfileTree.this,  "Folder name", "New folder",
					JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iFolderNew,64), null, null);
			if (res != null){
				String fullPath = node.getFullPath();
				if (fullPath.length() > 0) fullPath += "~";
				fullPath += res.toString();
				ProfileNode folder = new ProfileNode(UserData.FOLDER, node.getAnalysis(), node.getReference(), node.getField(), fullPath, res.toString(), null, UserData.FOLDER.getIcon());
				model.insertNodeInto(folder, node, node.getChildCount());
				tree.expandPath(new TreePath(model.getPathToRoot(folder)));
				tree.setSelectionPath(new TreePath(model.getPathToRoot(folder)));
				UserData userData = linkedData.get(folder.getPath()[1]);
				try{
					Highlander.getLoggedUser().saveFolder(fullPath, userData, node.getCategory());
				}catch(Exception ex){
					Tools.exception(ex);
					JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Error", ex), "New folder", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}
			}
		}
	}

	private void renameElement(ProfileNode node){
		Object res = JOptionPane.showInputDialog(ProfileTree.this,  "New name", "Renaming",
				JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iEditPen,64), null, node.getKey());
		if (res != null){
			try{
				switch (node.getUserData()) {
				case FOLDER:
					UserData userData = linkedData.get(node.getPath()[1]);
					for (Enumeration<?> descendants = node.preorderEnumeration() ; descendants.hasMoreElements() ; ){
						ProfileNode n = (ProfileNode)descendants.nextElement();
						TreeNode[] pathToN = n.getPath();
						StringBuilder oldsb = new StringBuilder();
						StringBuilder newsb = new StringBuilder();
						for (int i=1 ; i < pathToN.length ; i++){
							ProfileNode pn = (ProfileNode)pathToN[i];
							if (!linkedData.containsKey(pn)){
								oldsb.append(pn.getKey()+"~");
								if (pn == node){
									newsb.append(res.toString()+"~");										
								}else{
									newsb.append(pn.getKey()+"~");
								}
							}
						}
						if (oldsb.length() > 0) oldsb.deleteCharAt(oldsb.length()-1);
						if (newsb.length() > 0) newsb.deleteCharAt(newsb.length()-1);
						if (n.getUserData() == UserData.FOLDER){
							Highlander.getLoggedUser().renameFolder(oldsb.toString(), newsb.toString(), UserData.FOLDER, node.getCategory(), node.getCategory(), userData.toString());
						}else{
							if (node.getUserData().isLinked()){					
								Highlander.getLoggedUser().renameData(n.getUserData(), n.getCategory(), n.getCategory(), n.getFullPath(), newsb.toString());	
							}else{
								Highlander.getLoggedUser().renameData(n.getUserData(), n.getFullPath(), newsb.toString());						
							}							
						}
					}
					break;
				default:
					String newPath = res.toString();
					int pos = node.getFullPath().lastIndexOf("~");
					if (pos > -1){
						newPath = node.getFullPath().substring(0, pos) + "~" + res.toString();
					}
					if (node.getUserData().isLinked()){					
						Highlander.getLoggedUser().renameData(node.getUserData(), node.getCategory(), node.getCategory(), node.getFullPath(), newPath);	
					}else{
						Highlander.getLoggedUser().renameData(node.getUserData(), node.getFullPath(), newPath);						
					}
					break;
				}
				node.setKey(res.toString());
				node.reComputeFullPath();
				model.nodeChanged(node);
				tree.expandPath(new TreePath(model.getPathToRoot(node)));
				tree.setSelectionPath(new TreePath(model.getPathToRoot(node)));
			}catch(Exception ex){
				Tools.exception(ex);
				JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Error", ex), "Renaming", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}
		}
	}
	
	private void editElement(ProfileNode node){
		switch(node.getUserData()){
		case COLUMN_MASK:
			CreateColumnSelection ccs = new CreateColumnSelection(node.getAnalysis(), UserData.COLUMN_MASK, Field.getAvailableFields(node.getAnalysis(), true), node.getFullPath());
			Tools.centerWindow(ccs, false);
			ccs.setVisible(true);
			if(ccs.getListName() != null){		
				try{
					String maskName = ccs.getListName();
					List<Field> mask = ccs.getSelection();
					Highlander.getLoggedUser().saveColumnMask(maskName, node.getAnalysis(), mask);
					if (maskName.equals(mainFrame.getSelectedMaskName()) && Highlander.getCurrentAnalysis().equals(node.getAnalysis())){
						new Thread(new Runnable() {
							public void run() {
								mainFrame.refreshTableView();
							}
						}, "ProfileTree.refreshTableView").start();
					}
				} catch (Exception ex) {
					Tools.exception(ex);
					JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Cannot save your mask "+node.getKey(), ex), "Edit column mask", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));				
				}
			}
			break;
		case COLUMN_SELECTION:
			CreateColumnSelection cs = new CreateColumnSelection(node.getAnalysis(), UserData.COLUMN_SELECTION, Field.getAvailableFields(node.getAnalysis(), true), node.getFullPath());
			Tools.centerWindow(cs, false);
			cs.setVisible(true);
			if (cs.getListName() != null){
				String selectionName = cs.getListName();
				List<Field> selection = cs.getSelection();
				try {
					Highlander.getLoggedUser().saveColumnSelection(selectionName, node.getAnalysis(), selection);
					if (selectionName.equals(mainFrame.getSelectedColumnSelectionName()) && Highlander.getCurrentAnalysis().equals(node.getAnalysis())){
						new Thread(new Runnable() {
							public void run() {
								mainFrame.refreshTable();
							}
						}, "ProfileTree.refreshTable").start();
					}
				} catch (Exception ex) {
					Tools.exception(ex);
					JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Cannot save your selection "+node.getKey(), ex), "Edit column selection", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));				
				}
			}
			break;
		case INTERVALS:
			AskListOfIntervalsDialog askInterval = new AskListOfIntervalsDialog(node.getReference());
			Tools.centerWindow(askInterval, false);
			askInterval.loadList(node.getFullPath());
			askInterval.setVisible(true);
			if (!askInterval.getSelection().isEmpty()) askInterval.saveList();
			break;
		case PHENOTYPES:
			AskListOfHPOTermDialog askPhenotype = new AskListOfHPOTermDialog(node.getReference());
			Tools.centerWindow(askPhenotype, false);
			askPhenotype.loadList(node.getFullPath());
			askPhenotype.setVisible(true);
			if (!askPhenotype.getSelection().isEmpty()) askPhenotype.saveList();
			break;
		case VALUES:
			AskListOfFreeValuesDialog askValues = new AskListOfFreeValuesDialog(node.getField());
			Tools.centerWindow(askValues, false);
			askValues.loadList(node.getField(), node.getFullPath());
			askValues.setVisible(true);
			if (!askValues.getSelection().isEmpty()) askValues.saveList();
			break;
		case FILTERS_TEMPLATE:
			try {
				CreateTemplate ct = new CreateTemplate(node.getAnalysis(), Highlander.getLoggedUser().loadFiltersTemplate(node.getAnalysis(), node.getFullPath()));
				Tools.centerWindow(ct, false);
				ct.setVisible(true);
				if (ct.needToSave()) {
					ct.saveTemplate(node.getKey());
				}
			} catch (Exception ex) {
				Tools.exception(ex);
				JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Cannot modify your template "+node.getKey(), ex), "Edit template", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));				
			}
			break;
		default:
			break;			
		}
	}

	private void deleteElement(TreePath[] paths){
		int res = JOptionPane.showConfirmDialog(new JFrame(), 
				"Are you sure you want to permanently delete ALL selected elements (and all contained sub-element if any) ?", 
				"Deleting element from profile", JOptionPane.YES_NO_CANCEL_OPTION , JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iUserDelete,64));
		if (res == JOptionPane.YES_OPTION){
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(true);
					waitingPanel.start();
				}
			});
			Set<TreePath> recursive = new HashSet<TreePath>();
			for (TreePath path : paths){
				recursive.add(path);
				ProfileNode node = (ProfileNode)path.getLastPathComponent();
				if (node.getUserData() == UserData.FOLDER){
					for (Enumeration<?> descendants = node.postorderEnumeration() ; descendants.hasMoreElements() ; ){
						ProfileNode n = (ProfileNode)descendants.nextElement();
						recursive.add(new TreePath(n.getPath()));
					}
				}				
			}
			for (TreePath path : recursive){
				ProfileNode node = (ProfileNode)path.getLastPathComponent();
				waitingPanel.setProgressString("Deleting " + node, true);
				try{
					switch(node.getUserData()){
					case VALUES:
					case INTERVALS:
					case PHENOTYPES:
					case COLUMN_MASK:
					case COLUMN_SELECTION:
					case FILTER:
					case HIGHLIGHTING:
					case SORTING:
					case VARIANT_LIST:
					case FILTERS_TEMPLATE:
						Highlander.getLoggedUser().deleteData(node.getUserData(), node.getCategory(), node.getFullPath());
						break;
					case FOLDER:
						Highlander.getLoggedUser().deleteFolder(node.getFullPath(), linkedData.get(path.getPathComponent(1)), node.getCategory());
						break;
					case SETTINGS:
					case HISTORY:
						break;
					}
					model.removeNodeFromParent(node);
				}catch(Exception ex){
					Tools.exception(ex);
					JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Cannot delete profile element "+node, ex), "Deleting profile element", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));				
				}
			}
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(false);
					waitingPanel.stop();
				}
			});
		}
	}

	private void shareElement(TreePath[] paths, Set<User> users) throws Exception {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				waitingPanel.setVisible(true);
				waitingPanel.start();
			}
		});
		String userString = "";
		for (final User user : users){
			userString += user.toString() + "\n";
			for (final TreePath path : paths){
				waitingPanel.setProgressString("Sharing " + (ProfileNode)path.getLastPathComponent() + " with " + user.toString(), true);
				ProfileNode node = (ProfileNode)path.getLastPathComponent();
				if (node.getUserData() == UserData.FOLDER){
					duplicate(node, UserData.FOLDER, node, node.getKey(), false, user);
					user.renameFolder(node.getKey(), "SHARE|"+Highlander.getLoggedUser().getUsername()+"|"+node.getKey(), UserData.FOLDER, node.getCategory(), node.getCategory(), linkedData.get(node.getPath()[1]).toString());
				}else{
					if (node.getUserData().isLinked()){
						Highlander.getLoggedUser().shareData(node.getUserData(), node.getCategory(), node.getFullPath(), node.getKey(), user);
					}else{
						Highlander.getLoggedUser().shareData(node.getUserData(), node.getFullPath(), node.getKey(), user);
					}
				}
			}
		}
		if (!users.isEmpty()){
			JOptionPane.showMessageDialog(ProfileTree.this, paths.length+" elements have been sent,\n"+  userString + "will be notified.", "Sharing element from profile", JOptionPane.PLAIN_MESSAGE, Resources.getScaledIcon(Resources.iUsers,64));
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				waitingPanel.setVisible(false);
				waitingPanel.stop();
			}
		});
	}

	private void moveElement(ProfileNode[] nodes, ProfileNode destination, int indexInDestination){		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				waitingPanel.setVisible(true);
				waitingPanel.start();
			}
		});
		
		for(int i = 0; i < nodes.length; i++) {
			model.removeNodeFromParent(nodes[i]);
			model.insertNodeInto(nodes[i], destination, indexInDestination++);
			String oldCategory = nodes[i].getCategory();
			try{
				waitingPanel.setProgressString("Moving " + nodes[i], true);
				switch (nodes[i].getUserData()) {
				case FOLDER:
					UserData userData = linkedData.get(nodes[i].getPath()[1]);
					for (Enumeration<?> descendants = nodes[i].preorderEnumeration() ; descendants.hasMoreElements() ; ){
						ProfileNode n = (ProfileNode)descendants.nextElement();
						TreeNode[] pathToN = n.getPath();
						StringBuilder newsb = new StringBuilder();
						String newCategory = oldCategory;
						for (int j=1 ; j < pathToN.length ; j++){
							ProfileNode pn = (ProfileNode)pathToN[j]; 
							if (j == 2 && linkedData.containsKey(pn)){
								newCategory = pn.getKey();
							}
							if (!linkedData.containsKey(pn)){
								newsb.append(pn.getKey()+"~");
							}
						}
						if (newsb.length() > 0) newsb.deleteCharAt(newsb.length()-1);
						if (n.getUserData() == UserData.FOLDER) {
							Highlander.getLoggedUser().renameFolder(nodes[i].getFullPath(), newsb.toString(), UserData.FOLDER, oldCategory, newCategory, userData.toString());
						}else {
							if (n.getUserData().isLinked()){					
								Highlander.getLoggedUser().renameData(n.getUserData(), oldCategory, newCategory, n.getFullPath(), newsb.toString());	
							}else{
								Highlander.getLoggedUser().renameData(n.getUserData(), n.getFullPath(), newsb.toString());						
							}
						}
					}
					break;
				default:
					TreeNode[] pathToN = nodes[i].getPath();
					StringBuilder newsb = new StringBuilder();
					String newCategory = oldCategory;
					for (int j=1 ; j < pathToN.length ; j++){
						ProfileNode pn = (ProfileNode)pathToN[j]; 
						if (j == 2 && linkedData.containsKey(pn)){
							newCategory = pn.getKey();
						}
						if (!linkedData.containsKey(pn)){
							newsb.append(pn.getKey()+"~");
						}
					}
					if (newsb.length() > 0) newsb.deleteCharAt(newsb.length()-1);
					if (nodes[i].getUserData().isLinked()){					
						Highlander.getLoggedUser().renameData(nodes[i].getUserData(), oldCategory, newCategory, nodes[i].getFullPath(), newsb.toString());	
					}else{
						Highlander.getLoggedUser().renameData(nodes[i].getUserData(), nodes[i].getFullPath(), newsb.toString());						
					}
					break;
				}
				nodes[i].reComputeFullPath();
				model.nodeChanged(nodes[i]);
			}catch(Exception ex){
				Tools.exception(ex);
				JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Error", ex), "Renaming", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}
		}
		tree.expandPath(new TreePath(model.getPathToRoot(destination)));
		tree.setSelectionPath(new TreePath(model.getPathToRoot(destination)));
		tree.scrollPathToVisible(new TreePath(model.getPathToRoot(destination)));

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				waitingPanel.setVisible(false);
				waitingPanel.stop();
			}
		});
	}

	private void copyElement(TreePath[] paths){		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				waitingPanel.setVisible(true);
				waitingPanel.start();
			}
		});
		UserData userData = ((ProfileNode)paths[0].getLastPathComponent()).getUserData();
		String fullpath = showProfileDialog(this, mainFrame, Action.FOLDER, 
				(userData == UserData.FOLDER)?linkedData.get(((ProfileNode)paths[0].getLastPathComponent()).getPath()[1]):userData, 
						null, "Select a destination folder to copy selected "+userData.getName(), null);
		if (fullpath != null){
			String[] targetPath = fullpath.split("~");
			ProfileNode target = (ProfileNode)model.getRoot();
			for (int i=0 ; i < targetPath.length ; i++){
				boolean nodeFound = false;
				for (int j=0 ; j < target.getChildCount() ; j++){
					if (((ProfileNode)target.getChildAt(j)).getKey().equals(targetPath[i])){
						target = (ProfileNode)target.getChildAt(j);
						nodeFound = true;
						break;
					}
				}
				if (!nodeFound){
					//Node not found ... meaning that a new folder was created in the selection dialog
					ProfileNode newFolder = new ProfileNode(UserData.FOLDER, target.getAnalysis(), target.getReference(), target.getField(), 
							(target.getFullPath().length() > 0) ? target.getFullPath()+"~"+targetPath[i] : targetPath[i], targetPath[i], null, UserData.FOLDER.getIcon());
					model.insertNodeInto(newFolder, target, target.getChildCount());
					target = newFolder;
					tree.expandPath(new TreePath(model.getPathToRoot(target))); //if tree is not expanded and a new node is added in collapsed subtree, it will be present two times (the second being loaded from the database at the expansion of this node)
				}
			}
			for (TreePath path : paths){
				ProfileNode node = (ProfileNode)path.getLastPathComponent();
				waitingPanel.setProgressString("Copying " + node, true);
				String newFullPath = target.getFullPath();
				if (newFullPath.length() > 0) newFullPath += "~";
				newFullPath += node.getKey();
				try{
					int yesno = JOptionPane.YES_OPTION;
					boolean alreadyExists = (userData != UserData.FOLDER) 
							? Highlander.getLoggedUser().doesPersonalDataExists(userData, target.getCategory(), newFullPath)
							: false; 
					if (alreadyExists){
						yesno = JOptionPane.showConfirmDialog(new JFrame(), 
								"You already have an element named '"+node.getKey().replace("~", " -> ")+"', do you want to overwrite it ?", 
								"Overwriting element in your profile", JOptionPane.YES_NO_OPTION , JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iDbSave,64));
					}
					if (yesno == JOptionPane.YES_OPTION){
						duplicate(node, userData, target, newFullPath, alreadyExists);
					}else {
						//rename
						Object res = null;
						do {
							res = JOptionPane.showInputDialog(ProfileTree.this,  "Element already exists, give it a new name", "Renaming",
									JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iEditPen,64), null, node.getKey());
							if (res != null){
								alreadyExists = Highlander.getLoggedUser().doesPersonalDataExists(userData, target.getCategory(), newFullPath.replace(node.getKey(), res.toString()));
							}
						}while(alreadyExists && res != null);
						if (res != null) {
							duplicate(node, userData, target, newFullPath.replace(node.getKey(), res.toString()), false);	
						}
					}
				}catch(Exception ex){
					Tools.exception(ex);
					JOptionPane.showMessageDialog(this, Tools.getMessage("Error when copying " + userData.getName() + " '"+node.getKey()+"'", ex), 
							"Copy element", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}
			}			
			tree.expandPath(new TreePath(model.getPathToRoot(target)));
			tree.setSelectionPath(new TreePath(model.getPathToRoot(target)));
			tree.scrollPathToVisible(new TreePath(model.getPathToRoot(target)));			
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				waitingPanel.setVisible(false);
				waitingPanel.stop();
			}
		});
	}
	
	private void duplicate(ProfileNode node, UserData userData, ProfileNode target, String newFullPath, boolean alreadyExists) throws Exception {
		duplicate(node, userData, target, newFullPath, alreadyExists, Highlander.getLoggedUser());
	}
	
	private void duplicate(ProfileNode node, UserData userData, ProfileNode target, String newFullPath, boolean alreadyExists, User targetUser) throws Exception {
		tree.expandPath(new TreePath(model.getPathToRoot(target))); //if tree is not expanded and a new node is added in collapsed subtree, it will be present two times (the second being loaded from the database at the expansion of this node)
		switch(userData){
		case FOLDER:
			targetUser.saveFolder(newFullPath, linkedData.get(node.getPath()[1]), target.getCategory());
			break;
		case VALUES:
			targetUser.saveValues(newFullPath, target.getField(), Highlander.getLoggedUser().loadValuesWithComments(node.getField(), node.getFullPath()));
			break;
		case INTERVALS:
			targetUser.saveIntervals(newFullPath, target.getReference(), Highlander.getLoggedUser().loadIntervals(node.getReference(), node.getFullPath()));
			break;
		case PHENOTYPES:
			targetUser.savePhenotypes(newFullPath, target.getReference(), Highlander.getLoggedUser().loadPhenotypes(node.getReference(), node.getFullPath()));
			break;
		case COLUMN_MASK:
			List<Field> mask = Highlander.getLoggedUser().loadColumnMask(node.getAnalysis(), node.getFullPath());
			if (!target.getAnalysis().equals(node.getAnalysis())){
				List<Field> otherAnalSelec = new ArrayList<Field>();
				for (Field f : mask){
					if (f.hasAnalysis(target.getAnalysis())){
						otherAnalSelec.add(f);
					}
				}
				mask = otherAnalSelec;
			}							
			targetUser.saveColumnMask(newFullPath, target.getAnalysis(), mask);
			break;
		case COLUMN_SELECTION:
			List<Field> selection = Highlander.getLoggedUser().loadColumnSelection(node.getAnalysis(), node.getFullPath());
			if (!target.getAnalysis().equals(node.getAnalysis())){
				List<Field> otherAnalSelec = new ArrayList<Field>();
				for (Field f : selection){
					if (f.hasAnalysis(target.getAnalysis())){
						otherAnalSelec.add(f);
					}
				}
				selection = otherAnalSelec;
			}							
			targetUser.saveColumnSelection(newFullPath, target.getAnalysis(), selection);
			break;
		case SORTING:
			List<SortingCriterion> sorting = Highlander.getLoggedUser().loadSorting(null, node.getAnalysis(), node.getFullPath());
			if (!target.getAnalysis().equals(node.getAnalysis())){
				List<SortingCriterion> otherAnalSelec = new ArrayList<SortingCriterion>();
				for (SortingCriterion sc : sorting){
					if (sc.getField().hasAnalysis(target.getAnalysis())){
						otherAnalSelec.add(new SortingCriterion(null, sc.getField(), sc.getSortOrder()));
					}
				}
				sorting = otherAnalSelec;
			}
			targetUser.saveSorting(newFullPath, target.getAnalysis(), sorting);
			break;
		case FILTER:
			ComboFilter filter = Highlander.getLoggedUser().loadFilter(null, node.getAnalysis(), node.getFullPath());
			if (!target.getAnalysis().equals(node.getAnalysis())){
				if (!targetUser.saveFilter(newFullPath, target.getAnalysis(), filter, true)){
					Tools.print("Can't save this filter for analysis '"+target.getAnalysis()+"', some fields are not compatible");
				}
			}else{
				targetUser.saveFilter(newFullPath, target.getAnalysis(), filter, false);
			}
			break;
		case HIGHLIGHTING:
			List<HighlightingRule> highlighting = Highlander.getLoggedUser().loadHighlighting(null, node.getAnalysis(), node.getFullPath());
			if (!node.getAnalysis().equals(node.getAnalysis())){
				List<HighlightingRule> otherAnalSelec = new ArrayList<HighlightingRule>();
				for (HighlightingRule sc : highlighting){
					if (sc.getField().hasAnalysis(node.getAnalysis())){									
						switch (sc.getRuleType()) {
						case HIGHLIGHTING:
							HighlightCriterion hl = (HighlightCriterion)sc;
							otherAnalSelec.add(new HighlightCriterion(null, hl.getField(), 
									hl.getComparisonOperator(), hl.getNullValue(), hl.getValues(), hl.getProfileValues(), hl.getBackground(), hl.getForeground(), hl.isBold(), hl.isItalic(), hl.expandRow()));
							break;
						case HEATMAP:
							HeatMapCriterion hm = (HeatMapCriterion)sc;
							otherAnalSelec.add(new HeatMapCriterion(null, hm.getField(), 
									hm.getColorRange(), hm.getConversionMethod(), hm.getMinimum(), hm.getMaximum(), hm.expandTable()));
							break;
						default:
							System.err.println("Unknown Highlighting rule type : " + sc.getRuleType());
							break;
						}
					}
				}
				highlighting = otherAnalSelec;
			}
			targetUser.saveHighlighting(newFullPath, target.getAnalysis(), highlighting);
			break;
		case VARIANT_LIST:
			VariantsList list = Highlander.getLoggedUser().loadVariantList(node.getAnalysis(), node.getFullPath(), null, null, null, null, null, null);
			targetUser.saveVariantList(list, newFullPath);
			break;
		case FILTERS_TEMPLATE:
			FiltersTemplate template = Highlander.getLoggedUser().loadFiltersTemplate(node.getAnalysis(), node.getFullPath());
			if (!target.getAnalysis().equals(node.getAnalysis())){
				if (!template.changeAnalysis(target.getAnalysis())){
					Tools.print("Can't save this filter for analysis '"+target.getAnalysis()+"', some fields are not compatible");
				}else {
					targetUser.saveFiltersTemplate(template, newFullPath);
				}
			}else {
				targetUser.saveFiltersTemplate(template, newFullPath);
			}
			break;
		case SETTINGS:
		case HISTORY:
			break;
		}
		ProfileNode newNode = null;
		if (targetUser.getUsername().equals(Highlander.getLoggedUser().getUsername())){
			if (!alreadyExists){
				newNode = new ProfileNode(userData, target.getAnalysis(), target.getReference(), target.getField(), newFullPath, node.getKey(), node.getValues(), node.getIcon());
				model.insertNodeInto(newNode, target, target.getChildCount());
			}else{
				for (int i=0 ; i < target.getChildCount() ; i++){
					newNode = (ProfileNode)target.getChildAt(i);
					if (newNode.getFullPath().equals(newFullPath)){
						break;
					}
				}
			}
		}else{
			newNode = target;
		}
		if (userData == UserData.FOLDER){
			for (int i=0 ; i < node.getChildCount() ; i++){
				ProfileNode child = (ProfileNode)node.getChildAt(i);
				String newFullPathChild = newFullPath;
				if (newFullPathChild.length() > 0) newFullPathChild += "~";
				newFullPathChild += child.getKey();
				duplicate(child, child.getUserData(), newNode, newFullPathChild, false, targetUser);
			}
		}
	}
	

	private void createValueList(ProfileNode node){
		if (node.getUserData() != UserData.FOLDER) node = (ProfileNode)node.getParent();
		Field field = node.getField();
		AskListOfFreeValuesDialog ask = new AskListOfFreeValuesDialog(field);
		Tools.centerWindow(ask, false);
		ask.setVisible(true);
		if (!ask.getSelection().isEmpty()) {
			Object res = JOptionPane.showInputDialog(ProfileTree.this,  "List name", "New value list",
					JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iUserListNew,64), null, null);
			if (res != null){
				String fullPath = node.getFullPath();
				if (fullPath.length() > 0) fullPath += "~";
				fullPath += res.toString();
				Field newField = ask.getSelectedField();
				//Check if either no field was selected when creating list, either the user change the field during list creation
				if (newField != field) {
					//Get back to root of VALUES
					while (node.getLevel() != 1) {
						node = (ProfileNode)node.getParent();
					}
					//Check if a child node of this field already exist
					for (int j=0 ; j < node.getChildCount() ; j++){
						if (((ProfileNode)node.getChildAt(j)).getKey().equals(newField.getName())){
							node = (ProfileNode)node.getChildAt(j);
							break;
						}
					}
					//We are still at level 1, meaning no node of this field exists, create it
					if (node.getLevel() == 1) {
						ProfileNode newFieldNode = new ProfileNode(UserData.FOLDER, newField, "", newField.toString(), null, Resources.getScaledIcon(Resources.iField, 24));
						model.insertNodeInto(newFieldNode, node, node.getChildCount());
						linkedData.put(newFieldNode, UserData.VALUES);		
						node = newFieldNode;
					}
					tree.expandPath(new TreePath(model.getPathToRoot(node)));//if tree is not expanded and a new node is added in collapsed subtree, it will be present two times (the second being loaded from the database at the expansion of this node)
					field = newField;
				}
				ask.saveList(fullPath);
				ProfileNode newNode = new ProfileNode(UserData.VALUES, field, fullPath, res.toString(), null, UserData.VALUES.getIcon());
				model.insertNodeInto(newNode, node, node.getChildCount());
				tree.expandPath(new TreePath(model.getPathToRoot(newNode)));
				tree.setSelectionPath(new TreePath(model.getPathToRoot(newNode)));
			}
		}
	}

	private void createIntervalsList(ProfileNode node){
		if (node.getUserData() != UserData.FOLDER) node = (ProfileNode)node.getParent();
		Reference reference = node.getReference();
		if (reference == null) {
			reference = (Reference)JOptionPane.showInputDialog(new JFrame(), "Select a reference genome", "Create list of genomic intervals", 
					JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iReference, 64), 
					Reference.getAvailableReferences().toArray(new Reference[0]), 
					Highlander.getCurrentAnalysis().getReference());
		}
		if (reference != null) {
			AskListOfIntervalsDialog ask = new AskListOfIntervalsDialog(reference);
			Tools.centerWindow(ask, false);
			ask.setVisible(true);
			if (!ask.getSelection().isEmpty()) {
				Object res = JOptionPane.showInputDialog(ProfileTree.this,  "List name", "New intervals list",
						JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iUserListNew,64), null, null);
				if (res != null){
					String fullPath = node.getFullPath();
					if (fullPath.length() > 0) fullPath += "~";
					fullPath += res.toString();
					//If we are at level 1, user has selected the reference with a dialog
					if (node.getLevel() == 1) {
						//Check if a child node of this reference already exist
						for (int j=0 ; j < node.getChildCount() ; j++){
							if (((ProfileNode)node.getChildAt(j)).getKey().equals(reference.getName())){
								node = (ProfileNode)node.getChildAt(j);
								break;
							}
						}
						//We are still at level 1, meaning no node of this reference exists, create it
						if (node.getLevel() == 1) {
							ProfileNode newFieldNode = new ProfileNode(UserData.FOLDER, reference, "", reference.toString(), null, Resources.getScaledIcon(Resources.iReference, 24));
							model.insertNodeInto(newFieldNode, node, node.getChildCount());
							linkedData.put(newFieldNode, UserData.INTERVALS);
							node = newFieldNode;
						}
						tree.expandPath(new TreePath(model.getPathToRoot(node)));//if tree is not expanded and a new node is added in collapsed subtree, it will be present two times (the second being loaded from the database at the expansion of this node)
					}
					ask.saveList(fullPath);
					ProfileNode newNode = new ProfileNode(UserData.INTERVALS, reference, fullPath, res.toString(), null, UserData.INTERVALS.getIcon());
					model.insertNodeInto(newNode, node, node.getChildCount());
					tree.expandPath(new TreePath(model.getPathToRoot(newNode)));
					tree.setSelectionPath(new TreePath(model.getPathToRoot(newNode)));
				}
			}
		}
	}
	
	private void createPhenotypesList(ProfileNode node){
		if (node.getUserData() != UserData.FOLDER) node = (ProfileNode)node.getParent();
		Reference reference = node.getReference();
		if (reference == null) {
			reference = (Reference)JOptionPane.showInputDialog(new JFrame(), "Select a reference genome", "Create list of phenotypes", 
					JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iReference, 64), 
					Reference.getAvailableReferences().toArray(new Reference[0]), 
					Highlander.getCurrentAnalysis().getReference());
		}
		if (reference != null) {
			AskListOfHPOTermDialog ask = new AskListOfHPOTermDialog(reference);
			Tools.centerWindow(ask, false);
			ask.setVisible(true);
			if (!ask.getSelection().isEmpty()) {
				Object res = JOptionPane.showInputDialog(ProfileTree.this,  "List name", "New phenotypes list",
						JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iUserListNew,64), null, null);
				if (res != null){
					String fullPath = node.getFullPath();
					if (fullPath.length() > 0) fullPath += "~";
					fullPath += res.toString();
					//If we are at level 1, user has selected the reference with a dialog
					if (node.getLevel() == 1) {
						//Check if a child node of this reference already exist
						for (int j=0 ; j < node.getChildCount() ; j++){
							if (((ProfileNode)node.getChildAt(j)).getKey().equals(reference.getName())){
								node = (ProfileNode)node.getChildAt(j);
								break;
							}
						}
						//We are still at level 1, meaning no node of this reference exists, create it
						if (node.getLevel() == 1) {
							ProfileNode newFieldNode = new ProfileNode(UserData.FOLDER, reference, "", reference.toString(), null, Resources.getScaledIcon(Resources.iReference, 24));
							model.insertNodeInto(newFieldNode, node, node.getChildCount());
							linkedData.put(newFieldNode, UserData.PHENOTYPES);
							node = newFieldNode;
						}
						tree.expandPath(new TreePath(model.getPathToRoot(node)));//if tree is not expanded and a new node is added in collapsed subtree, it will be present two times (the second being loaded from the database at the expansion of this node)
					}
					ask.saveList(fullPath);
					ProfileNode newNode = new ProfileNode(UserData.PHENOTYPES, reference, fullPath, res.toString(), null, UserData.PHENOTYPES.getIcon());
					model.insertNodeInto(newNode, node, node.getChildCount());
					tree.expandPath(new TreePath(model.getPathToRoot(newNode)));
					tree.setSelectionPath(new TreePath(model.getPathToRoot(newNode)));
				}
			}
		}
	}
	
	private void createFiltersTemplate(ProfileNode node){
		if (node.getUserData() != UserData.FOLDER) node = (ProfileNode)node.getParent();
		CreateTemplate ask = new CreateTemplate(node.getAnalysis());
		Tools.centerWindow(ask, false);
		ask.setVisible(true);
		if (ask.needToSave()) {
			Object res = JOptionPane.showInputDialog(ProfileTree.this,  "Template name", "New filters template",
					JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iUserTemplateNew,64), null, null);
			if (res != null){
				String fullPath = node.getFullPath();
				if (fullPath.length() > 0) fullPath += "~";
				fullPath += res.toString();
				tree.expandPath(new TreePath(model.getPathToRoot(node)));
				ask.saveTemplate(fullPath);
				ProfileNode newNode = new ProfileNode(UserData.FILTERS_TEMPLATE, node.getAnalysis(), fullPath, res.toString(), null, UserData.FILTERS_TEMPLATE.getIcon());
				model.insertNodeInto(newNode, node, node.getChildCount());
				tree.expandPath(new TreePath(model.getPathToRoot(newNode)));
				tree.setSelectionPath(new TreePath(model.getPathToRoot(newNode)));
			}
		}
	}
	
	public void refreshButtons(){	
		if (canRenameSelection()){
			btnRename.setEnabled(true);
		}else{
			btnRename.setEnabled(false);			
		}
		if (canEditSelection()){
			btnEdit.setEnabled(true);
		}else{
			btnEdit.setEnabled(false);
		}
		if (canDeleteSelection()){
			btnDelete.setEnabled(true);
		}else{
			btnDelete.setEnabled(false);
		}
		if (canCopySelection()){
			btnCopy.setEnabled(true);
		}else{
			btnCopy.setEnabled(false);
		}
		if (canShareSelection()){
			btnShare.setEnabled(true);
		}else{
			btnShare.setEnabled(false);
		}
		if (canCreateElement((ProfileNode)tree.getLastSelectedPathComponent(), UserData.FOLDER)){
			btnFolderNew.setEnabled(true);
		}else{
			btnFolderNew.setEnabled(false);
		}
		if (canCreateElement((ProfileNode)tree.getLastSelectedPathComponent(), UserData.VALUES)){
			createUserValueList.setEnabled(true);
		}else{
			createUserValueList.setEnabled(false);
		}
		if (canCreateElement((ProfileNode)tree.getLastSelectedPathComponent(), UserData.INTERVALS)){
			createUserIntervalsList.setEnabled(true);
		}else{
			createUserIntervalsList.setEnabled(false);
		}
		if (canCreateElement((ProfileNode)tree.getLastSelectedPathComponent(), UserData.PHENOTYPES)){
			createUserPhenotypesList.setEnabled(true);
		}else{
			createUserPhenotypesList.setEnabled(false);
		}
		if (canCreateElement((ProfileNode)tree.getLastSelectedPathComponent(), UserData.FILTERS_TEMPLATE)){
			createUserTemplateList.setEnabled(true);
		}else{
			createUserTemplateList.setEnabled(false);
		}
		if (action != Action.MANAGE){
			if ((action == Action.SAVE && canSave()) || (action == Action.LOAD && canLoad()) || (action == Action.FOLDER && canLoadFolder())){
				okButton.setEnabled(true);
			}else{
				okButton.setEnabled(false);
			}
		}
	}
	
	private boolean canRenameSelection(){
		if (tree.getSelectionCount() != 1) return false;
		ProfileNode node = (ProfileNode) tree.getLastSelectedPathComponent() ;
		if (node.getUserData() == UserData.SETTINGS) return false;
		if (node.getUserData() == UserData.HISTORY) return false;
		if (node.isRoot()) return false;
		if (linkedData.containsKey(node)) return false;
		return true;
	}
	
	private boolean canEditSelection(){
		if (tree.getSelectionCount() != 1) return false;
		ProfileNode node = (ProfileNode) tree.getLastSelectedPathComponent() ;
		if (node.getUserData() == UserData.FOLDER) return false;
		if (node.getUserData() == UserData.FILTER) return false;
		if (node.getUserData() == UserData.SORTING) return false;
		if (node.getUserData() == UserData.HIGHLIGHTING) return false;
		if (node.getUserData() == UserData.VARIANT_LIST) return false;
		if (node.getUserData() == UserData.FILTERS_TEMPLATE) return true;
		if (node.getUserData() == UserData.SETTINGS) return false;
		if (node.getUserData() == UserData.HISTORY) return false;
		if (node.isRoot()) return false;
		if (linkedData.containsKey(node)) return false;
				
		return true;
	}
	
	private boolean canDeleteSelection(){
		if (tree.getSelectionCount() == 0) return false;
		for(TreePath path : tree.getSelectionPaths()) {
			ProfileNode node = (ProfileNode)path.getLastPathComponent();
			if (node.isRoot()) return false;
			if (linkedData.containsKey(node)) return false;
		}
		return true;
	}
	
	private boolean canCopySelection(){
		if (tree.getSelectionCount() == 0) return false;
		UserData selectedUserData = null;
		for(TreePath path : tree.getSelectionPaths()) {
			ProfileNode node = (ProfileNode)path.getLastPathComponent();
			if (node.getUserData() == UserData.VARIANT_LIST){
				return false;
			}
			if (node.getUserData() == UserData.SETTINGS	|| node.getUserData() == UserData.HISTORY){
				return false;
			}
			if (node.getLevel() <= 2) {
				return false;
			}
			// Do not allow copying different user data
			if (selectedUserData == null){
				selectedUserData = node.getUserData();
			}else if (node.getUserData() != selectedUserData){
				return false;
			}
		}
		return true;
	}
	
	private boolean canShareSelection(){
		if (tree.getSelectionCount() == 0) return false;
		for(TreePath path : tree.getSelectionPaths()) {
			ProfileNode node = (ProfileNode)path.getLastPathComponent();
			if (node.getUserData() == UserData.SETTINGS) return false;
			if (node.getUserData() == UserData.HISTORY) return false;
			if (node.getLevel() <= 2) {
				return false;
			}
		}
		return true;
	}
	
	private boolean canCreateElement(ProfileNode node, UserData elementType){
		if (tree.getSelectionCount() != 1) return false;
		switch(elementType){
		case FOLDER:
			if (node.getUserData() != UserData.FOLDER) return false;
			if (node.isRoot()) return false;
			if (((ProfileNode)node.getParent()).isRoot() && linkedData.get(node).isLinked()) return false;
			break;
		default:
			if (node.getUserData() != UserData.FOLDER) return false;
			if (getUserDataLinked(node) != elementType) return false;
			if (node.isRoot()) return false;
			if (((ProfileNode)node.getParent()).isRoot() && linkedData.get(node).isAnalysisLinked()) return false; //VALUES and INTERVALS can be created at 1st level 
			break;
		}
		return true;
	}
	
	private boolean canSave(){
		if (tree.getSelectionCount() != 1) return false;
		ProfileNode selectedNode = (ProfileNode) tree.getLastSelectedPathComponent();
		if (selectedNode.isRoot()) return false;
		if (selectedNode.getUserData() != UserData.FOLDER){
			selectedNode = ((ProfileNode)selectedNode.getParent());
		}
		if (!canCreateElement(selectedNode, userDataFilter)) return false;
		if (userInput.getText().length() == 0) return false;
		return true;
	}
	
	private boolean canLoad(){
		if (tree.getSelectionCount() != 1) return false;
		ProfileNode selectedNode = (ProfileNode) tree.getLastSelectedPathComponent();
		if (selectedNode.isRoot()) return false;
		if (selectedNode.getUserData() == UserData.FOLDER) return false;
		return true;		
	}
	
	private boolean canLoadFolder(){
		if (tree.getSelectionCount() != 1) return false;		
		ProfileNode selectedNode = (ProfileNode) tree.getLastSelectedPathComponent();
		if (selectedNode.isRoot()) return false;
		if (selectedNode.getUserData() != UserData.FOLDER) return false;
		if (((ProfileNode)selectedNode.getParent()).isRoot() && linkedData.get(selectedNode).isLinked()) return false;
		return true;				
	}

	private void searchProfile(String search) {
		try {
			searchResults = Highlander.getLoggedUser().searchElements(search);
			searchResultsTotal = 0;
			for (UserData userData : searchResults.keySet()){
				for (String category : searchResults.get(userData).keySet()) {
					searchResultsTotal += searchResults.get(userData).get(category).size();
				}
			}
			searchResultsCurrent = 0;
			searchProfileGoto(true);
		}catch(Exception ex){
			Tools.exception(ex);
		}
	}
	
	private void searchProfileGoto(boolean next) {
		if (next) {
			searchResultsCurrent = (searchResultsCurrent == searchResultsTotal) ? 1 : searchResultsCurrent+1;
		}else {
			searchResultsCurrent = (searchResultsCurrent == 1) ? searchResultsTotal : searchResultsCurrent-1;
		}
		int k=1;
		for (UserData userData : searchResults.keySet()){
			for (String category : searchResults.get(userData).keySet()) {
				for (String fullpath : searchResults.get(userData).get(category)) {
					if (k == searchResultsCurrent) {
						//System.out.println(userData + " # " + analysis + " # " + fullpath);
						searchResultsNumber.setText("Result "+searchResultsCurrent+"/" + searchResultsTotal);
						if (userData != null){
							ProfileNode target = (ProfileNode)model.getRoot();
							if (userData == UserData.FOLDER) {
								userData = UserData.valueOf(fullpath.split("\\|")[0]);
								fullpath = fullpath.split("\\|")[1];
							}
							for (ProfileNode n : linkedData.keySet()){
								if (userData == null || linkedData.get(n) == userData){
									if (n.getKey().equals((category != null) ? category : Highlander.getCurrentAnalysis().toString())){
										target = n;
										break;
									}
								}
							}
							tree.expandPath(new TreePath(model.getPathToRoot(target)));
							tree.setSelectionPath(new TreePath(model.getPathToRoot(target)));
							if (fullpath != null){
								String[] targetPath = fullpath.split("~");
								for (int i=0 ; i < targetPath.length ; i++){
									for (int j=0 ; j < target.getChildCount() ; j++){
										if (((ProfileNode)target.getChildAt(j)).getKey().equals(targetPath[i])){
											target = (ProfileNode)target.getChildAt(j);
											break;
										}
									}
								}
								tree.expandPath(new TreePath(model.getPathToRoot(target)));
								tree.setSelectionPath(new TreePath(model.getPathToRoot(target)));
								tree.scrollPathToVisible(new TreePath(model.getPathToRoot(target)));
							}
						}
						return;
					}
					k++;
				}
			}
		}
	}
	
	private JComponent showSelectedElement(){
		ProfileNode node = (ProfileNode) tree.getLastSelectedPathComponent();
		if (node != null){
			String path = node.getUserData().getName() + " -> ";
			if (node.getCategory() != null) path += node.getCategory() +  " -> ";
			path += node.getFullPath().replace("~", " -> ");
			if (node.getLevel() == 2) {
				return getDescriptionCategory(node);
			}else {
				return getDescriptionElement(node.getUserData(), node.getCategory(), node.getFullPath(), path);
			}
		}else{
			return new JPanel();
		}
	}
	
	private String showHistory() {
		String history = "";
		ProfileNode node = (ProfileNode) tree.getLastSelectedPathComponent();
		if (node != null){
			if (node.getLevel() != 2) {
				try {
					history = Highlander.getLoggedUser().getHistory(node.getUserData(),  node.getCategory(), node.getFullPath());
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		}
		return history;
	}
	
	public static JComponent getDescriptionElement(UserData userData, String category, String listFullPath, String title){
		try{
			switch(userData){
			case COLUMN_MASK:					
				return getDescriptionColumnsList(Highlander.getLoggedUser().loadColumnMask(new Analysis(category), listFullPath), title);
			case COLUMN_SELECTION:
				return getDescriptionColumnsList(Highlander.getLoggedUser().loadColumnSelection(new Analysis(category), listFullPath), title);
			case FILTER:
				return getDescriptionFilter(Highlander.getLoggedUser().loadFilter(null, new Analysis(category), listFullPath));
			case HIGHLIGHTING:
				return getDescriptionHighlighting(Highlander.getLoggedUser().loadHighlighting(null, new Analysis(category), listFullPath), title);
			case INTERVALS:
				return getDescriptionValueList(Highlander.getLoggedUser().loadIntervals(ProfileTree.getReference(category), listFullPath), title);
			case PHENOTYPES:
				return getDescriptionValueList(Highlander.getLoggedUser().loadPhenotypes(ProfileTree.getReference(category), listFullPath), title);
			case SORTING:
				return getDescriptionSorting(Highlander.getLoggedUser().loadSorting(null, new Analysis(category), listFullPath), title);
			case VALUES:
				return getDescriptionValueList(Highlander.getLoggedUser().loadValuesWithComments(Field.getField(category), listFullPath), new String[] {title, "Comments"});
			case VARIANT_LIST:
				return getDescriptionVariantList(Highlander.getLoggedUser().loadVariantList(new Analysis(category), listFullPath, null, null, null, null, null, null), title);
			case FILTERS_TEMPLATE:
				return getDescriptionFiltersTemplate(Highlander.getLoggedUser().loadFiltersTemplate(new Analysis(category), listFullPath), title);
			case FOLDER:
			case SETTINGS:
			case HISTORY:
			default:
				return new JPanel();
			}
		}catch(Exception ex){
			Tools.exception(ex);
			return Tools.getMessage("Cannot create panel", ex);
		}
	}
	
	public JPanel getDescriptionCategory(ProfileNode node){
		JLabel titleLabel = new JLabel(node.getKey());
		titleLabel.setIcon(node.getIcon());
		titleLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		int row = 0;
		JPanel panel = new JPanel(new GridBagLayout());
		panel.add(titleLabel, new GridBagConstraints(0, row++, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		switch(linkedData.get(node).getLink()) {
		case ANALYSIS:
			AnalysisFull analysis = Highlander.getAnalysis(node.getAnalysis().toString());
			panel.add(new JLabel("Reference "), new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			panel.add(new JLabel(analysis.getReference().toString()), new GridBagConstraints(1, row++, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			panel.add(new JLabel("Sequencing target "), new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			panel.add(new JLabel(analysis.getSequencingTarget().toString()), new GridBagConstraints(1, row++, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			panel.add(new JLabel("Variant caller "), new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			panel.add(new JLabel(analysis.getVariantCaller().toString()), new GridBagConstraints(1, row++, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			break;
		case REFERENCE:
			Reference reference = node.getReference();
			panel.add(new JLabel(reference.getDescription()), new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			break;
		case FIELD:
			Field field = node.getField();
			panel.add(new JLabel(field.getHtmlTooltip()), new GridBagConstraints(0, row++, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			break;
		case NONE:
		default:
			break;
		}
		panel.add(new JPanel(), new GridBagConstraints(0, row++, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(5, 5, 5, 5), 0, 0));
		return panel;
	}
	
	public static JPanel getDescriptionVariantList(VariantsList list, String title){
		JLabel titleLabel = new JLabel(title);
		titleLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		int row = 0;
		JPanel panel = new JPanel(new GridBagLayout());
		panel.add(titleLabel, new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		JLabel filterLabel = new JLabel("Filter");
		filterLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		panel.add(filterLabel, new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		panel.add(getDescriptionFilter(list.getFilter()), new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));		
		panel.add(getDescriptionSorting(list.getSorting(), "Sorting criteria"), new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));		
		panel.add(getDescriptionHighlighting(list.getHighlighting(), "Highlighting rules"), new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));		
		JLabel colselLabel = new JLabel("Columns selection");
		colselLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		panel.add(colselLabel, new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		panel.add(getDescriptionColumnsList(list.getColumnSelection(), "Columns selection"), new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));		
		JLabel colmaskLabel = new JLabel("Column mask");
		colmaskLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		panel.add(colmaskLabel, new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		panel.add(getDescriptionColumnsList(list.getColumnMask(), "Column mask"), new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));		
		JLabel idsLabel = new JLabel("Variant identifiers ("+Tools.intToString(list.getVariants().size())+" variants)");
		idsLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		panel.add(idsLabel, new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		panel.add(getDescriptionValueList(list.getVariants(), "Variant identifiers"), new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		panel.add(new JPanel(), new GridBagConstraints(0, row++, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(5, 5, 5, 5), 0, 0));
		return panel;
	}
	
	public static JPanel getDescriptionFiltersTemplate(FiltersTemplate template, String title){
		JLabel titleLabel = new JLabel(title);
		titleLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		int row = 0;
		JPanel panel = new JPanel(new GridBagLayout());
		panel.add(titleLabel, new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		for (Entry<String, String> e : template.getFiltersSaveStrings().entrySet()) {
			JLabel filterLabel = new JLabel(e.getKey());
			filterLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
			panel.add(filterLabel, new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			try {
				panel.add(getDescriptionFilter((ComboFilter)new ComboFilter().loadCriterion(null, e.getValue())), new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			}catch(Exception ex) {
				ex.printStackTrace();
				JLabel label = new JLabel(" "+(new ComboFilter()).parseSaveString(e.getValue())+" ");
				label.setOpaque(true);
				panel.add(label, new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			}
		}
		panel.add(new JPanel(), new GridBagConstraints(0, row++, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(5, 5, 5, 5), 0, 0));
		return panel;
	}
	
	public static JPanel getDescriptionHighlighting(List<HighlightingRule> list, String title){
		JLabel titleLabel = new JLabel(title);
		titleLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		int row = 0;
		JPanel panel = new JPanel(new GridBagLayout());
		panel.add(titleLabel, new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		for (HighlightingRule h : list){						
			JLabel label = new JLabel();
			switch (h.getRuleType()) {
			case HIGHLIGHTING:
				label = (new HighlightCriterion()).parseSaveString(h.getSaveString());
				label.setText(" "+label.getText()+" ");
				label.setOpaque(true);
				break;
			case HEATMAP:
				label = (new HeatMapCriterion()).parseSaveString(h.getSaveString());
				label.setText(" "+label.getText()+" ");
				label.setOpaque(true);
				break;
			default:
				System.err.println("Unknown Highlighting rule type : " + h.getRuleType());
				break;
			}
			panel.add(label, new GridBagConstraints(0, row++, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		}
		panel.add(new JPanel(), new GridBagConstraints(0, row++, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(5, 5, 5, 5), 0, 0));
		return panel;
	}
	
	public static JPanel getDescriptionSorting(List<SortingCriterion> list, String title){
		JLabel titleLabel = new JLabel(title);
		titleLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		int row = 0;
		JPanel panel = new JPanel(new GridBagLayout());
		panel.add(titleLabel, new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		for (SortingCriterion s : list){
			JLabel label = new JLabel();
			label.setText(s.getFieldName());
			label.setIcon(((s.getSortOrder() == SortOrder.DESCENDING)?Resources.getScaledIcon(Resources.iSortDesc, 16):Resources.getScaledIcon(Resources.iSortAsc, 16)));
			label.setToolTipText(s.getField().getDescriptionAndSource());
			panel.add(label, new GridBagConstraints(0, row++, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		}
		panel.add(new JPanel(), new GridBagConstraints(0, row++, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(5, 5, 5, 5), 0, 0));
		return panel;
	}
	
	public static JTable getDescriptionValueList(List<?> list, String header){
		Object[][] data = new Object[list.size()][1];
		int row = 0;
		for (Object o : list){
			data[row++][0] = o;
		}
		return new JTable(new DefaultTableModel(data, new String[] {header}));
	}
	
	public static JTable getDescriptionValueList(Map<?,?> map, String[] headers){
		Object[][] data = new Object[map.size()][2];
		int row = 0;
		for (Entry<?, ?> o : map.entrySet()){
			data[row][0] = o.getKey();
			data[row][1] = o.getValue();
			row++;
		}
		return new JTable(new DefaultTableModel(data, headers));
	}
	
	public static JTable getDescriptionColumnsList(List<Field> columns, String header){
		Object[][] data = new Object[columns.size()][1];
		int row = 0;
		for (Field f : columns){
			data[row++][0] = f;
		}
		return new JTable(new DefaultTableModel(data, new String[] {header})){
			public String getToolTipText(MouseEvent e) {
				String tip = null;
				java.awt.Point p = e.getPoint();
				int rowIndex = rowAtPoint(p);
				Object val = getValueAt(rowIndex, 0);
				try{
					tip = (val != null) ? ((Field)val).getHtmlTooltip() : null;
				}catch(Exception ex){
					Tools.exception(ex);
				}
				return tip;
			}

			public boolean isCellEditable(int row, int column){
				return false;
			}
		};
	}
	
	public static JTree getDescriptionFilter(ComboFilter filter){
		JTree tree = new JTree(new FilterTreeModel(filter));
		tree.setCellRenderer(new FilterTreeCellRenderer());
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		for (int r=0 ; r < tree.getRowCount() ; r++){
			tree.expandRow(r);
		}
		return tree;
	}
	
	private UserData getUserDataLinked(ProfileNode node){
		for (TreeNode n : node.getPath()){
			if (linkedData.containsKey(n)){
				return linkedData.get(n);
			}
		}
		return UserData.FOLDER;
	}

	public class ProfileTreeModel extends DefaultTreeModel {
		
		public ProfileTreeModel(String username, UserData filterUserData, Analysis filterAnalysis, Reference filterReference, Field filterField){
			super(new ProfileNode(UserData.FOLDER, "", username, null, Resources.getScaledIcon(Resources.iUser, 24)), true);
			for (UserData stuff : UserData.values()){
				if (filterUserData == null || filterUserData == stuff){
					if (stuff != UserData.FOLDER && stuff != UserData.SETTINGS && stuff != UserData.HISTORY){
						ProfileNode userDataNode = new ProfileNode(UserData.FOLDER, "", stuff.getName().substring(0, 1).toUpperCase() + stuff.getName().substring(1), null, stuff.getIcon());
						((ProfileNode)root).add(userDataNode);
						linkedData.put(userDataNode, stuff);
						switch(stuff.getLink()) {
						case ANALYSIS:
							for (AnalysisFull analysis : Highlander.getAvailableAnalyses()){
								if (filterAnalysis == null || filterAnalysis.equals(analysis)){
									ProfileNode analysisNode = new ProfileNode(UserData.FOLDER, analysis, "", analysis.toString(), null, analysis.getSmallIcon());
									userDataNode.add(analysisNode);
									linkedData.put(analysisNode, stuff);
								}
							}
							break;
						case FIELD:
							if (filterField == null) {
								for (Field field : Highlander.getLoggedUser().getExistingListOfValuesFields()) {
									ProfileNode fieldNode = new ProfileNode(UserData.FOLDER, field, "", field.toString(), null, Resources.getScaledIcon(Resources.iField, 24));
									userDataNode.add(fieldNode);
									linkedData.put(fieldNode, stuff);
								}
							}else {
								ProfileNode fieldNode = new ProfileNode(UserData.FOLDER, filterField, "", filterField.toString(), null, Resources.getScaledIcon(Resources.iField, 24));
								userDataNode.add(fieldNode);
								linkedData.put(fieldNode, stuff);								
							}
							break;
						case REFERENCE:
							if (filterReference == null) {
								for (Reference reference : Highlander.getLoggedUser().getExistingListOfIntervalsReferences()) {
									ProfileNode referenceNode = new ProfileNode(UserData.FOLDER, reference, "", reference.toString(), null, Resources.getScaledIcon(Resources.iReference, 24));
									userDataNode.add(referenceNode);
									linkedData.put(referenceNode, stuff);
								}
							}else {
								for (Reference reference : Reference.getAvailableReferences()) {
									if (filterReference.usesSameReferenceSequenceAs(reference)){
										ProfileNode referenceNode = new ProfileNode(UserData.FOLDER, reference, "", reference.toString(), null, Resources.getScaledIcon(Resources.iReference, 24));
										userDataNode.add(referenceNode);
										linkedData.put(referenceNode, stuff);
									}
								}
							}
							break;
						case NONE:
						default:
							break;
						}
					}
				}
			}
		}
				
		public void buildSubtree(UserData stuff, ProfileNode rootNode){			
			if (rootNode.getLevel() > 1) {
				try{
					String category = (stuff.isLinked()) ? rootNode.getKey() : null;
					Analysis analysis = (stuff.isAnalysisLinked()) ? new Analysis(rootNode.getKey()) : null;
					Reference reference = (stuff.isReferenceLinked()) ? ProfileTree.getReference(rootNode.getKey()) : null;
					Field field = (stuff.isFieldLinked()) ? Field.getField(rootNode.getKey()) : null;
					List<String> folders = Highlander.getLoggedUser().loadFolders(stuff, category);
					for (String folder : folders){
						String[] dirs = folder.split("~");
						ProfileNode currentNode = rootNode;
						for (int i=0 ; i < dirs.length ; i++){
							ProfileNode dirNode = null;
							for (int j=0 ; j < currentNode.getChildCount() ; j++){
								if (((ProfileNode)currentNode.getChildAt(j)).getKey().equals(dirs[i])){
									dirNode = (ProfileNode)currentNode.getChildAt(j);
									break;
								}
							}
							if (dirNode == null){
								String path = "";
								for (int j=0 ; j <= i ; j++) {
									path += dirs[j];
									if (j < i) path += "~";
								}
								switch(stuff.getLink()) {
								case ANALYSIS:
									dirNode = new ProfileNode(UserData.FOLDER, analysis, path, dirs[i], null, UserData.FOLDER.getIcon());
									break;
								case REFERENCE:
									dirNode = new ProfileNode(UserData.FOLDER, reference, path, dirs[i], null, UserData.FOLDER.getIcon());
									break;
								case FIELD:
									dirNode = new ProfileNode(UserData.FOLDER, field, path, dirs[i], null, UserData.FOLDER.getIcon());
									break;
								case NONE:
								default:
									dirNode = new ProfileNode(UserData.FOLDER, path, dirs[i], null, UserData.FOLDER.getIcon());
									break;							
								}
								currentNode.add(dirNode);
							}
							currentNode = dirNode;
						}
					}
					Map<String,List<String>> data;
					if (category != null){
						data = Highlander.getLoggedUser().getPersonalData(stuff, category);
					}else{
						data = Highlander.getLoggedUser().getPersonalData(stuff);
					}
					for (String key : data.keySet()){
						if (!key.startsWith("SHARE|")){
							String[] dirs = key.split("~");
							ProfileNode currentNode = rootNode;
							for (int i=0 ; i < dirs.length-1 ; i++){
								ProfileNode dirNode = null;
								for (int j=0 ; j < currentNode.getChildCount() ; j++){
									if (((ProfileNode)currentNode.getChildAt(j)).getKey().equals(dirs[i])){
										dirNode = (ProfileNode)currentNode.getChildAt(j);
										break;
									}
								}
								if (dirNode == null){
									String path = "";
									for (int j=0 ; j <= i ; j++) {
										path += dirs[j];
										if (j < i) path += "~";
									}
									switch(stuff.getLink()) {
									case ANALYSIS:
										dirNode = new ProfileNode(UserData.FOLDER, analysis, path, dirs[i], null, UserData.FOLDER.getIcon());
										break;
									case REFERENCE:
										dirNode = new ProfileNode(UserData.FOLDER, reference, path, dirs[i], null, UserData.FOLDER.getIcon());
										break;
									case FIELD:
										dirNode = new ProfileNode(UserData.FOLDER, field, path, dirs[i], null, UserData.FOLDER.getIcon());
										break;
									case NONE:
									default:
										dirNode = new ProfileNode(UserData.FOLDER, path, dirs[i], null, UserData.FOLDER.getIcon());
										break;							
									}
									currentNode.add(dirNode);
								}
								currentNode = dirNode;
							}
							switch(stuff.getLink()) {
							case ANALYSIS:
								currentNode.add(new ProfileNode(stuff, analysis, key, dirs[dirs.length-1], data.get(key), stuff.getIcon()));
								break;
							case REFERENCE:
								currentNode.add(new ProfileNode(stuff, reference, key, dirs[dirs.length-1], data.get(key), stuff.getIcon()));
								break;
							case FIELD:
								currentNode.add(new ProfileNode(stuff, field, key, dirs[dirs.length-1], data.get(key), stuff.getIcon()));
								break;
							case NONE:
							default:
								currentNode.add(new ProfileNode(stuff, key, dirs[dirs.length-1], data.get(key), stuff.getIcon()));
								break;							
							}
						}
					}
				}catch(Exception ex){
					Tools.exception(ex);
					JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Error", ex), "Fetch profile items", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}
			}
		}
	}
	
	public class ProfileNode extends DefaultMutableTreeNode {
		private UserData userData;
		private Analysis analysis;
		private Reference reference;
		private Field field;
		private ImageIcon icon;
		private String key;
		private String fullPath;
		private List<String> values;
		
		public ProfileNode(UserData userData, Analysis analysis, Reference reference, Field field, String fullPath, String key, List<String> values, ImageIcon icon){
			super(key, true);
			this.userData = userData;
			this.analysis = analysis;
			this.reference = reference;
			this.field = field;
			this.fullPath = fullPath;
			this.key = key;
			this.values = values;
			this.icon = icon;
			if (userData != UserData.FOLDER) setAllowsChildren(false);
			
		}
		
		public ProfileNode(UserData userData, String fullPath, String key, List<String> values, ImageIcon icon){
			this(userData, null, null, null, fullPath, key, values, icon);
		}
		
		public ProfileNode(UserData userData, Analysis analysis, String fullPath, String key, List<String> values, ImageIcon icon){
			this(userData, analysis, null, null, fullPath, key, values, icon);
		}
		
		public ProfileNode(UserData userData, Reference reference, String fullPath, String key, List<String> values, ImageIcon icon){
			this(userData, null, reference, null, fullPath, key, values, icon);
		}
		
		public ProfileNode(UserData userData, Field field, String fullPath, String key, List<String> values, ImageIcon icon){
			this(userData, null, null, field, fullPath, key, values, icon);
		}
		
		public UserData getUserData(){
			return userData;
		}
		
		public String getCategory() {
			switch(userData) {
			case COLUMN_MASK:
			case COLUMN_SELECTION:
			case FILTER:
			case FILTERS_TEMPLATE:
			case VARIANT_LIST:
			case HIGHLIGHTING:
			case SORTING:
				return getAnalysis().toString();
			case VALUES:
				return getField().getName();
			case INTERVALS:
			case PHENOTYPES:
				return getReference().getName();
			case FOLDER:
				if (analysis != null) return analysis.toString();
				else if (field != null) return field.getName();
				else if (reference != null) return reference.getName();
				else return null;
			case SETTINGS:
			case HISTORY:
			default:
				return null;
			}
		}
		
		public Analysis getAnalysis(){
			return analysis;
		}
		
		public Reference getReference(){
			return reference;
		}
		
		public Field getField(){
			return field;
		}
		
		public ImageIcon getIcon(){
			return icon;
		}
		
		public String getFullPath(){
			return fullPath;
		}
		
		public String getKey(){
			return key;
		}
		
		public void setKey(String newKey){
			key = newKey;
			setUserObject(newKey);			
		}
		
		public void reComputeFullPath(){
			StringBuilder sb = new StringBuilder();
			TreeNode[] path = getPath();
			for (int i=1 ; i < path.length ; i++){
				ProfileNode node = (ProfileNode) path[i];
				if (i == 2 && linkedData.containsKey(node)){
					if (analysis != null && !node.getKey().equals(analysis.toString())){
						analysis = new Analysis(node.getKey());
					}else if (reference != null && !node.getKey().equals(reference.toString())){
						reference = ProfileTree.getReference(node.getKey());
					}else if (field != null && !node.getKey().equals(field.toString())){
						field = Field.getField(node.getKey());
					}
				}
				if (!linkedData.containsKey(node)){
					sb.append(node.getKey());
					if (i < path.length-1) sb.append("~");
				}
			}
			fullPath = sb.toString();
			for (int i=0 ; i < getChildCount() ; i++){
				((ProfileNode)getChildAt(i)).reComputeFullPath();
			}
		}
		
		public List<String> getValues(){
			return values;
		}		
	}
	
	class ProfileTreeCellRenderer extends DefaultTreeCellRenderer {
		public Component getTreeCellRendererComponent(JTree tree,	Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			ProfileNode node = (ProfileNode)value; 
			setIcon((node).getIcon());
			if ((linkedData.containsKey(node) && linkedData.get(node).isReferenceLinked()) || node.getUserData().isReferenceLinked()) {
				if (node.getReference() != null && !node.getReference().hasSchema(Schema.ENSEMBL)) {
					//All references must have an ENSEMBL schema, this node is probably a deleted reference
					setForeground(Color.RED);
				}
			}
			if ((linkedData.containsKey(node) && linkedData.get(node).isFieldLinked()) || node.getUserData().isFieldLinked()) {
				if (node.getField() != null && node.getField().getTableSuffix() == null) {
					//All fields comes from a table, this node is probably a deleted/renamed field
					setForeground(Color.RED);
				}
			}
			return this;
		}
	}

	@Override
	public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
		ProfileNode rootNode = (ProfileNode)event.getPath().getLastPathComponent();
		if (linkedData.containsKey(rootNode)){
			model.buildSubtree(linkedData.get(rootNode), rootNode);
			refreshButtons();
		}
	}

	@Override
	public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
		//Do nothing
	}

	class TreeTransferHandler extends TransferHandler {
		private DataFlavor nodesFlavor;
		private DataFlavor[] flavors = new DataFlavor[1];
		private JTree tree;

		public TreeTransferHandler(JTree tree) {
			this.tree = tree;	
			try {
				String mimeType = DataFlavor.javaJVMLocalObjectMimeType + ";class=\"" + ProfileNode[].class.getName() + "\"";
				nodesFlavor = new DataFlavor(mimeType);
				flavors[0] = nodesFlavor;
			} catch(ClassNotFoundException e) {
				System.out.println("ClassNotFound: " + e.getMessage());
			}
		}

		public boolean canImport(TransferHandler.TransferSupport support) {
			if(!support.isDrop()) {
				return false;
			}
			support.setShowDropLocation(true);
			if(!support.isDataFlavorSupported(nodesFlavor)) {
				return false;
			}
			// Do not allow a drop on the drag source selections.
			JTree.DropLocation dl = (JTree.DropLocation)support.getDropLocation();
			JTree tree = (JTree)support.getComponent();
			int dropRow = tree.getRowForPath(dl.getPath());
			int[] selRows = tree.getSelectionRows();
			for(int i = 0; i < selRows.length; i++) {
				if(selRows[i] == dropRow) {
					return false;
				}
			}        
			TreePath dest = dl.getPath();
			ProfileNode target = (ProfileNode)dest.getLastPathComponent();
			// Do not allow a drop on a target that is root or direct child that is linked to a category
			if (target.isRoot() || (((ProfileNode)target.getParent()).isRoot() && linkedData.get(target).isLinked())){
				return false;
			}
			for(int i = 0; i < selRows.length; i++) {
				TreePath path = tree.getPathForRow(selRows[i]);
				ProfileNode firstNode = (ProfileNode)path.getLastPathComponent();
				// Do not allow a drop of a "root" node
				if (linkedData.containsKey(firstNode)){
					return false;
				}
				// Do not allow a non-leaf node to be copied to a level
				// which is less than its source level.
				if(firstNode.getChildCount() > 0 &&
						target.getLevel() > firstNode.getLevel()) {
					return false;
				}
				// Do not allow a drop in a subtree of another UserData
				if (getUserDataLinked(firstNode) != getUserDataLinked(target)){
					return false;
				}
			}
			return true;
		}

		protected Transferable createTransferable(JComponent c) {
			TreePath[] paths = tree.getSelectionPaths();
			if(paths != null) {
				List<ProfileNode> nodes = new ArrayList<ProfileNode>();
				for(int i = 0; i < paths.length; i++) {
					boolean isAncestorAlsoSelected = false;
					for (Object o : paths[i].getPath()){
						ProfileNode ancestor = (ProfileNode)o;						
						for (int j = 0 ; j < paths.length; j++) {
							if (j != i){
								ProfileNode node = (ProfileNode)paths[j].getLastPathComponent();
								if (node == ancestor){
									isAncestorAlsoSelected = true;
									break;
								}
							}
						}
					}
					if (!isAncestorAlsoSelected){
						ProfileNode node = (ProfileNode)paths[i].getLastPathComponent();
						nodes.add(node);
					}
				}
				return new NodesTransferable(nodes.toArray(new ProfileNode[nodes.size()]));
			}
			return null;
		}

		public int getSourceActions(JComponent c) {
			return MOVE;
		}

		public boolean importData(TransferHandler.TransferSupport support) {
			if(!canImport(support)) {
				return false;
			}
			// Extract transfer data.
			ProfileNode[] nodes = null;
			try {
				Transferable t = support.getTransferable();
				nodes = (ProfileNode[])t.getTransferData(nodesFlavor);
			} catch(UnsupportedFlavorException ufe) {
				System.out.println("UnsupportedFlavor: " + ufe.getMessage());
			} catch(java.io.IOException ioe) {
				System.out.println("I/O error: " + ioe.getMessage());
			}
			// Get drop location info.
			JTree.DropLocation dl = (JTree.DropLocation)support.getDropLocation();
			int childIndex = dl.getChildIndex();
			TreePath dest = dl.getPath();
			final ProfileNode parent = (ProfileNode)dest.getLastPathComponent();
			//DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
			// Configure for drop mode.
			int index = childIndex;    // DropMode.INSERT
			if(childIndex == -1) {     // DropMode.ON
				index = parent.getChildCount();
			}
			// Modify model
			final ProfileNode[] nodesToMove = nodes;
			final int indexInDestination = index;
			new Thread(new Runnable() {
				@Override
				public void run() {
					moveElement(nodesToMove, parent, indexInDestination);
				}
			}, "ProfileTree.moveElement").start();
			return true;
		}

		public String toString() {
			return getClass().getName();
		}

		public class NodesTransferable implements Transferable {
			ProfileNode[] nodes;

			public NodesTransferable(ProfileNode[] nodes) {
				this.nodes = nodes;
			}

			public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
				if(!isDataFlavorSupported(flavor))
					throw new UnsupportedFlavorException(flavor);
				return nodes;
			}

			public DataFlavor[] getTransferDataFlavors() {
				return flavors;
			}

			public boolean isDataFlavorSupported(DataFlavor flavor) {
				return nodesFlavor.equals(flavor);
			}
		}
	}
	
	public static String showProfileDialog(Window parent, Highlander mainFrame, Action action, UserData userDataFilter, String filter, String title, String currentSeletion){
		ProfileTree profileTree = null;
		if (filter != null) {
			switch(userDataFilter.getLink()) {
			case ANALYSIS:
				profileTree = new ProfileTree(parent, mainFrame, action, userDataFilter, new Analysis(filter), title, currentSeletion);
				break;
			case REFERENCE:
				profileTree = new ProfileTree(parent, mainFrame, action, userDataFilter, ProfileTree.getReference(filter), title, currentSeletion);
				break;
			case FIELD:
				profileTree = new ProfileTree(parent, mainFrame, action, userDataFilter, Field.getField(filter), title, currentSeletion);
				break;
			case NONE:
			default:
				profileTree = new ProfileTree(parent, mainFrame, action, userDataFilter, null, null, null, title, currentSeletion);
				break;
			}
		}else {
			profileTree = new ProfileTree(parent, mainFrame, action, userDataFilter, null, null, null, title, currentSeletion);
		}
		Tools.centerWindow(profileTree, action == Action.MANAGE);
		profileTree.setVisible(true);
		return profileTree.getSelection();
	}

	public static String showProfileDialog(Window parent, Action action, UserData userDataFilter, String filter, String title, String currentSeletion){
		return showProfileDialog(parent, null, action, userDataFilter, filter, title, currentSeletion);
	}

	public static String showProfileDialog(Window parent, Action action, UserData userDataFilter, String filter, String title){
		return showProfileDialog(parent, null, action, userDataFilter, filter, title, null);
	}
	
	public static String showProfileDialog(Window parent, Action action, UserData userDataFilter, String filter){
		switch(action){
		case LOAD:
			return showProfileDialog(parent, null, action, userDataFilter, filter, "Load "+userDataFilter.getName()+" from profile", null);
		case SAVE:
			return showProfileDialog(parent, null, action, userDataFilter, filter, "Save "+userDataFilter.getName()+" to profile", null);
		case MANAGE:
		default :
			return showProfileDialog(parent, null, action, userDataFilter, filter, "Profile management", null);
		}		
	}
	
	public static String showProfileDialog(Window parent, Highlander mainFrame){
		return showProfileDialog(parent, mainFrame, Action.MANAGE, null, null, "Profile management", null);
	}
	
	/**
	 * Method {@link Reference#getReference(String)} can throw exception if no reference matches the given String.
	 * If no reference matching referenceString is found, a dummy Reference is created with same name.
	 * It can be useful if a reference has been deleted from the database (users can still access their lists, and move them to a potential other reference).
	 * 
	 * @param referenceString
	 * @return
	 */
	public static Reference getReference(String referenceString) {
		try {
			return Reference.getReference(referenceString);
		}catch(Exception ex) {
		}
		return new Reference(referenceString, new ArrayList<>(), "Reference " + referenceString + " seems to have been deleted from Highlander. \nYou should delete items associated with this reference, or move them to another reference.", new HashMap<>());
	}

}
