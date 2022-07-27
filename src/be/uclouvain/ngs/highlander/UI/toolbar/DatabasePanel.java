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

package be.uclouvain.ngs.highlander.UI.toolbar;

import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;
import javax.swing.ButtonGroup;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.border.EtchedBorder;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.dialog.CreateColumnSelection;
import be.uclouvain.ngs.highlander.UI.dialog.ProfileTree;
import be.uclouvain.ngs.highlander.UI.dialog.ProfileTree.Action;
import be.uclouvain.ngs.highlander.UI.misc.ToolbarScrollablePanel;
import be.uclouvain.ngs.highlander.administration.users.User;
import be.uclouvain.ngs.highlander.administration.users.User.Settings;
import be.uclouvain.ngs.highlander.administration.users.User.TargetLastSelection;
import be.uclouvain.ngs.highlander.administration.users.User.UserData;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;

public class DatabasePanel extends JPanel {
	
	private final Highlander mainFrame;
	private static boolean switchingAnalysis = false;
	
	private JPanel colSelectionPanel;
	private ButtonGroup dbGroup = new ButtonGroup();
	private ButtonGroup selectionGroup = new ButtonGroup();
	private JRadioButton selection_all;
	private JRadioButton selection_default;
	private JRadioButton selection_custom;
	private JButton customButton;
	private Map<Analysis, JToggleButton> analysisButtons = new HashMap<Analysis, JToggleButton>();
	
	private String loadedSelectionPath = null;
	private List<Field> loadedSelection = null;
	
	public DatabasePanel(final Highlander mainFrame) {
		this.mainFrame = mainFrame;
		setLayout(new BorderLayout(0,0));

		JPanel center = new JPanel(new FlowLayout(FlowLayout.LEFT));
		ToolbarScrollablePanel scrollablePanel = new ToolbarScrollablePanel(center, Highlander.getHighlanderObserver(), 40);
		add(scrollablePanel, BorderLayout.CENTER);

		for (AnalysisFull analysis : Highlander.getAvailableAnalyses()){
			JToggleButton button = new JToggleButton(Resources.getScaledIcon(analysis.getIcon(), 40));
			button.setToolTipText(analysis.getHtmlTooltip());
			button.setPreferredSize(new Dimension(54,54));
			button.setSelected(true);
			final AnalysisFull thisAnalysis = analysis;
			button.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent arg0) {
					if (arg0.getStateChange() == ItemEvent.SELECTED && !switchingAnalysis){
						new Thread(new Runnable(){
							public void run(){
								mainFrame.changeAnalysis(thisAnalysis);
							}
						}, "DatabasePanel.changeAnalysis").start();
					}
				}
			});
			dbGroup.add(button);
			center.add(button);
			analysisButtons.put(analysis, button);
		}
				
		JPanel panel = new JPanel();
		panel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		panel.setPreferredSize(new Dimension(2, 50));
		center.add(panel);

		JButton button_add = new JButton(Resources.getScaledIcon(Resources.iColumnSelectionNew, 40));
		button_add.setToolTipText("Create a new selection of columns for this analysis");
		button_add.setPreferredSize(new Dimension(54,54));
		button_add.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				addNewColumnSelection();
			}
		});		
		center.add(button_add);

		JPanel colSelectionPanelContainer = new JPanel(new GridBagLayout());
		
		JLabel selectionLabel = new JLabel("Selection of columns to fetch from the database");
		colSelectionPanelContainer.add(selectionLabel, new GridBagConstraints(0,1,1,1,1.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,5,0,10), 0, 0));
		
		colSelectionPanel = new JPanel();
		FlowLayout flowLayout_2 = (FlowLayout) colSelectionPanel.getLayout();
		flowLayout_2.setAlignment(FlowLayout.LEFT);

		colSelectionPanelContainer.add(colSelectionPanel, new GridBagConstraints(0,2,1,1,1.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,5,0,10), 0, 0));
		
		center.add(colSelectionPanelContainer);
		
		selectionGroup = new ButtonGroup();
		
		selection_default = new JRadioButton("Default columns");
		selection_default.setToolTipText("Request the default columns set for this analysis");
		selection_default.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					new Thread(new Runnable() {
						public void run() {
							mainFrame.refreshTable();
							if (!switchingAnalysis) {
								try {
									Highlander.getLoggedUser().saveSettings(Highlander.getCurrentAnalysis().toString(), Settings.LAST_SELECTION, TargetLastSelection.COLUMN_SELECTION.toString(), getColumnSelectionName());
								}catch (Exception ex){
									Tools.exception(ex);
								}
							}
						}
					}, "DatabasePanel.refreshTable").start();
				}
			}
		});
		colSelectionPanel.add(selection_default);
		selectionGroup.add(selection_default);

		selection_all = new JRadioButton("All columns");
		selection_all.setToolTipText("Request all columns available in the database");
		selection_all.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					new Thread(new Runnable() {
						public void run() {
							mainFrame.refreshTable();
							if (!switchingAnalysis) {
								try {
									Highlander.getLoggedUser().saveSettings(Highlander.getCurrentAnalysis().toString(), Settings.LAST_SELECTION, TargetLastSelection.COLUMN_SELECTION.toString(), getColumnSelectionName());
								}catch (Exception ex){
									Tools.exception(ex);
								}
							}
						}
					}, "DatabasePanel.refreshTable").start();
				}
			}
		});
		colSelectionPanel.add(selection_all);
		selectionGroup.add(selection_all);
		
		selection_custom = new JRadioButton("Columns selection");
		selection_custom.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					new Thread(new Runnable() {
						public void run() {
							if (loadedSelection == null){
								if(!loadSelectionFromProfile()){
									selection_default.setSelected(true);
								}else {
									if (!switchingAnalysis) {
										try {
											Highlander.getLoggedUser().saveSettings(Highlander.getCurrentAnalysis().toString(), Settings.LAST_SELECTION, TargetLastSelection.COLUMN_SELECTION.toString(), getColumnSelectionName());
										}catch (Exception ex){
											Tools.exception(ex);
										}
									}
								}
							}else{
								mainFrame.refreshTable();
								if (!switchingAnalysis) {
									try {
										Highlander.getLoggedUser().saveSettings(Highlander.getCurrentAnalysis().toString(), Settings.LAST_SELECTION, TargetLastSelection.COLUMN_SELECTION.toString(), getColumnSelectionName());
									}catch (Exception ex){
										Tools.exception(ex);
									}
								}
							}
						}
					}, "DatabasePanel.refreshTable").start();
				}
			}
		});
		colSelectionPanel.add(selection_custom);
		selectionGroup.add(selection_custom);
		
		customButton = new JButton("Load columns selection");
		customButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						loadSelectionFromProfile();
						if (!switchingAnalysis) {
							try {
								Highlander.getLoggedUser().saveSettings(Highlander.getCurrentAnalysis().toString(), Settings.LAST_SELECTION, TargetLastSelection.COLUMN_SELECTION.toString(), getColumnSelectionName());
							}catch (Exception ex){
								Tools.exception(ex);
							}
						}						
					}
				}, "DatabasePanel.loadSelectionFromProfile").start();
			}
		});
		colSelectionPanel.add(customButton);
		
		String lastSelection = null;
		try {
			lastSelection = Highlander.getLoggedUser().loadSetting(Highlander.getCurrentAnalysis().toString(), Settings.LAST_SELECTION, TargetLastSelection.COLUMN_SELECTION.toString());
		}catch (Exception ex){
			Tools.exception(ex);
		}
		if (lastSelection != null) {
			switch(lastSelection) {
			case "Default":
				selection_default.setSelected(true);
				break;
			case "All":
				selection_all.setSelected(true);
				break;
			default:
				loadSelectionFromProfile(lastSelection, Highlander.getCurrentAnalysis());
				break;
			}
		}else {
			selection_default.setSelected(true);
		}
	}

	public String getColumnSelectionName(){
		if (selection_default.isSelected()) return "Default";
		if (selection_all.isSelected()) return "All";
		return loadedSelectionPath;
	}
	
	public void switchAnalysis(Analysis analysis){
		switchingAnalysis = true;
		String lastSelection = null;
		try {
			lastSelection = Highlander.getLoggedUser().loadSetting(analysis.toString(), Settings.LAST_SELECTION, TargetLastSelection.COLUMN_SELECTION.toString());
		}catch (Exception ex){
			Tools.exception(ex);
		}
		if (lastSelection != null) {
			switch(lastSelection) {
			case "Default":
				selection_default.setSelected(true);
				break;
			case "All":
				selection_all.setSelected(true);
				break;
			default:
				loadSelectionFromProfile(lastSelection, analysis);
				break;
			}
		}else {
			if (selection_custom.isSelected()){
				List<Field> fieldToRemove = new ArrayList<Field>();
				//System.out.println(loadedSelection);
				for (int i=0 ; i < loadedSelection.size() ; i++){
					Field f = loadedSelection.get(i);
					if (f.hasAnalysis(analysis))
						loadedSelection.set(i, f);
					else{
						//System.out.println("remove field "+f);
						fieldToRemove.add(f);
					}
				}
				for (Field f : fieldToRemove){
					loadedSelection.remove(f);
				}
				//System.out.println(loadedSelection);
			}else{
				customButton.setText("Load columns selection");
				customButton.setToolTipText(null);
				loadedSelection = null;
				loadedSelectionPath = null;
			}
		}
		switchingAnalysis = false;
	}
	
	public void switchBack(Analysis analysis){
		switchingAnalysis = true;
		analysisButtons.get(analysis).setSelected(true);
		switchingAnalysis = false;
	}
	
	private boolean loadSelectionFromProfile(){
		String selectionPath = ProfileTree.showProfileDialog(mainFrame, Action.LOAD, UserData.COLUMN_SELECTION, Highlander.getCurrentAnalysis().toString(), "Load columns selection from profile", loadedSelectionPath);		
		return loadSelectionFromProfile(selectionPath, Highlander.getCurrentAnalysis());
	}
	
	private boolean loadSelectionFromProfile(String selectionPath, Analysis analysis){
		if (selectionPath != null){
			try{
				loadedSelectionPath = selectionPath;
				loadedSelection = Highlander.getLoggedUser().loadColumnSelection(analysis, selectionPath);
				String selectionName = (loadedSelectionPath.contains("~"))?loadedSelectionPath.substring(loadedSelectionPath.lastIndexOf("~")+1):loadedSelectionPath;
				customButton.setText(selectionName);
				StringBuilder sb = new StringBuilder();
				sb.append("<html><b>["+selectionName+"]</b>");
				for (Field f : loadedSelection){
					sb.append("<br>" + f);
				}
				sb.append("</html>");
				customButton.setToolTipText(sb.toString());
				if (selection_custom.isSelected()){
					mainFrame.refreshTable();
				}else{
					selection_custom.setSelected(true);
				}
				Highlander.getHighlanderObserver().setControlName("RESIZE_TOOLBAR");
				return true;
			}catch(Exception ex){
				Tools.exception(ex);
				JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Can't retreive columns selection from your profile", ex), "Retreive user column selection",
						JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}
		}
		return false;
	}

	public List<Field> getColumnSelection(){
		if (selection_all.isSelected()) {
			return Field.getAvailableFields(Highlander.getCurrentAnalysis(), false);
		}else if (selection_default.isSelected() || loadedSelection == null){
			List<Field> selection = new ArrayList<>();
			try {
				for (String field : User.loadDefaultSettings(Highlander.getCurrentAnalysis().toString(), Settings.DEFAULT_COLUMNS)) {
					selection.add(Field.getField(field));
				}
			}catch(Exception ex){
				Tools.exception(ex);
				JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Can't retreive default columns selection", ex), "Retreive default column selection",
						JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}
			return selection;
		}else{
			return loadedSelection;
		}
	}
	
	private void addNewColumnSelection(){
		CreateColumnSelection ccs = new CreateColumnSelection(Highlander.getCurrentAnalysis(), UserData.COLUMN_SELECTION, Field.getAvailableFields(Highlander.getCurrentAnalysis(), true));
		Tools.centerWindow(ccs, false);
		ccs.setVisible(true);
		if(!ccs.getSelection().isEmpty()){		
			try{
				String selectionName = ccs.getListName();
				if (selectionName == null) return;
				if (Highlander.getLoggedUser().doesPersonalDataExists(UserData.COLUMN_SELECTION, null, selectionName)){
					int yesno = JOptionPane.showConfirmDialog(new JFrame(), 
							"You already have a "+UserData.COLUMN_SELECTION.getName()+" named '"+selectionName.replace("~", " -> ")+"', do you want to overwrite it ?", 
							"Overwriting element in your profile", JOptionPane.YES_NO_OPTION , JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iDbSave,64));
					if (yesno == JOptionPane.NO_OPTION)	return;
				}
				List<Field> selection = ccs.getSelection();
				Highlander.getLoggedUser().saveColumnSelection(selectionName, Highlander.getCurrentAnalysis(), selection);
				loadSelectionFromProfile(selectionName, Highlander.getCurrentAnalysis());
			}catch(Exception ex){
				Tools.exception(ex);
				JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Can't save column selection list to your profile", ex), "Saving column selection list to your profile",
						JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}
		}
	}
	
	public void showVariantListSelection(List<Field> selection){
		loadedSelectionPath = null;
		loadedSelection = selection;
		customButton.setText("Variant list columns");
		StringBuilder sb = new StringBuilder();
		sb.append("<html><b>[Variant list columns]</b>");
		for (Field f : loadedSelection){
			sb.append("<br>" + f);
		}
		sb.append("</html>");
		customButton.setToolTipText(sb.toString());
		if (selection_custom.isSelected()){
			mainFrame.refreshTableView();
		}else{
			selection_custom.setSelected(true);
		}
		Highlander.getHighlanderObserver().setControlName("RESIZE_TOOLBAR");
	}

}
