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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;
import javax.swing.border.EtchedBorder;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.dialog.AskSamplesDialog;
import be.uclouvain.ngs.highlander.UI.dialog.CreateColumnSelection;
import be.uclouvain.ngs.highlander.UI.dialog.ProfileTree;
import be.uclouvain.ngs.highlander.UI.dialog.ProfileTree.Action;
import be.uclouvain.ngs.highlander.UI.misc.ToolbarScrollablePanel;
import be.uclouvain.ngs.highlander.UI.table.VariantsTable;
import be.uclouvain.ngs.highlander.administration.users.User.UserData;
import be.uclouvain.ngs.highlander.database.Field;

public class NavigationPanel extends JPanel {

	private final Highlander mainFrame;
	private final VariantsTable table;
	
	private JPanel colSelectionPanel;
	private JToggleButton button_cell_selection;
	private JToggleButton button_row_selection;
	private JToggleButton button_sample_mask;
	private JToggleButton button_interest_yes;
	private JToggleButton button_interest_maybe;
	private JToggleButton button_interest_no;
	private JToggleButton button_evaluation_unclassified;
	private JToggleButton button_evaluation_1;
	private JToggleButton button_evaluation_2;
	private JToggleButton button_evaluation_3;
	private JToggleButton button_evaluation_4;
	private JToggleButton button_evaluation_5;
	private ButtonGroup selectionGroup = new ButtonGroup();
	JRadioButton selection_all;
	private JRadioButton selection_custom;
	private JButton customButton;
	
	//private List<Field> variantListMask = null;
	private String loadedSelectionPath = null;
	private List<Field> loadedSelection = null;

	public NavigationPanel(final Highlander mainFrame) {
		this.mainFrame = mainFrame;
		this.table = mainFrame.getVariantTable();
		setLayout(new BorderLayout(0, 0));

		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));		
		
		ButtonGroup selgroup = new ButtonGroup();
		
		button_cell_selection = new JToggleButton(Resources.getScaledIcon(Resources.iSelectionCell, 40));
		button_cell_selection.setSelected(false);
		button_cell_selection.setToolTipText("Select cells in the table");
		button_cell_selection.setPreferredSize(new Dimension(54,54));
		button_cell_selection.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					mainFrame.getVariantTable().setRowSelectionAllowed(false);
			}
		});	
		selgroup.add(button_cell_selection);		
		panel.add(button_cell_selection);
		
		button_row_selection = new JToggleButton(Resources.getScaledIcon(Resources.iSelectionRow, 40));
		button_row_selection.setSelected(true);
		button_row_selection.setToolTipText("Select full rows in the table");
		button_row_selection.setPreferredSize(new Dimension(54,54));
		button_row_selection.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED)
					mainFrame.getVariantTable().setRowSelectionAllowed(true);
			}
		});			
		selgroup.add(button_row_selection);		
		panel.add(button_row_selection);
		
		JPanel sepPanel1 = new JPanel();
		sepPanel1.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		sepPanel1.setPreferredSize(new Dimension(2, 50));
		panel.add(sepPanel1);

		button_sample_mask = new JToggleButton(Resources.getScaledIcon(Resources.iPatients, 40));
		button_sample_mask.setSelected(false);
		button_sample_mask.setToolTipText("Show/Hide variants based on a selection of samples");
		button_sample_mask.setPreferredSize(new Dimension(54,54));
		button_sample_mask.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				sampleMask();
			}
		});	
		panel.add(button_sample_mask);
		
		JPanel sepPanel4 = new JPanel();
		sepPanel4.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		sepPanel4.setPreferredSize(new Dimension(2, 50));
		panel.add(sepPanel4);
		
		button_interest_yes = new JToggleButton(Resources.getScaledIcon(Resources.iButtonApply, 40));
		button_interest_yes.setSelected(true);
		button_interest_yes.setToolTipText("Show/Hide variants marked as 'of interest' (if column " + Field.variant_of_interest.getName() + " is present)");
		button_interest_yes.setPreferredSize(new Dimension(54,54));
		button_interest_yes.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				updateInterestFiltering();
			}
		});	
		panel.add(button_interest_yes);
		
		button_interest_maybe = new JToggleButton(Resources.getScaledIcon(Resources.iQuestion, 40));
		button_interest_maybe.setSelected(true);
		button_interest_maybe.setToolTipText("Show/Hide variants not marked as 'of interest'/'no interesting' (if column " + Field.variant_of_interest.getName() + " is present)");
		button_interest_maybe.setPreferredSize(new Dimension(54,54));
		button_interest_maybe.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				updateInterestFiltering(); 
			}
		});	
		panel.add(button_interest_maybe);
		
		button_interest_no = new JToggleButton(Resources.getScaledIcon(Resources.iCross, 40));
		button_interest_no.setSelected(true);
		button_interest_no.setToolTipText("Show/Hide variants marked as 'not interesting' (if column " + Field.variant_of_interest.getName() + " is present)");
		button_interest_no.setPreferredSize(new Dimension(54,54));
		button_interest_no.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				updateInterestFiltering(); 
			}
		});	
		panel.add(button_interest_no);
		
		JPanel sepPanel3 = new JPanel();
		sepPanel3.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		sepPanel3.setPreferredSize(new Dimension(2, 50));
		panel.add(sepPanel3);

		button_evaluation_unclassified = new JToggleButton(Resources.getScaledIcon(Resources.iQuestion, 40));
		button_evaluation_unclassified.setSelected(true);
		button_evaluation_unclassified.setToolTipText("Show/Hide variants not evaluated (if column " + Field.evaluation.getName() + " is present)");
		button_evaluation_unclassified.setPreferredSize(new Dimension(54,54));
		button_evaluation_unclassified.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				updateEvaluationFiltering();
			}
		});	
		panel.add(button_evaluation_unclassified);
			
		button_evaluation_1 = new JToggleButton(Resources.getScaledIcon(Resources.iRoman1, 40));
		button_evaluation_1.setSelected(true);
		button_evaluation_1.setToolTipText("Show/Hide variants evaluated as Type I - Polymorphism (if column " + Field.evaluation.getName() + " is present)");
		button_evaluation_1.setPreferredSize(new Dimension(54,54));
		button_evaluation_1.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				updateEvaluationFiltering();
			}
		});	
		panel.add(button_evaluation_1);
		
		button_evaluation_2 = new JToggleButton(Resources.getScaledIcon(Resources.iRoman2, 40));
		button_evaluation_2.setSelected(true);
		button_evaluation_2.setToolTipText("Show/Hide variants evaluated as Type II - Variant Likely Benign (if column " + Field.evaluation.getName() + " is present)");
		button_evaluation_2.setPreferredSize(new Dimension(54,54));
		button_evaluation_2.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				updateEvaluationFiltering();
			}
		});	
		panel.add(button_evaluation_2);
		
		button_evaluation_3 = new JToggleButton(Resources.getScaledIcon(Resources.iRoman3, 40));
		button_evaluation_3.setSelected(true);
		button_evaluation_3.setToolTipText("Show/Hide variants evaluated as Type III - Variant of Unknown Significance (if column " + Field.evaluation.getName() + " is present)");
		button_evaluation_3.setPreferredSize(new Dimension(54,54));
		button_evaluation_3.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				updateEvaluationFiltering();
			}
		});	
		panel.add(button_evaluation_3);
		
		button_evaluation_4 = new JToggleButton(Resources.getScaledIcon(Resources.iRoman4, 40));
		button_evaluation_4.setSelected(true);
		button_evaluation_4.setToolTipText("Show/Hide variants evaluated as Type IV - Variant Likely Pathogenic (if column " + Field.evaluation.getName() + " is present)");
		button_evaluation_4.setPreferredSize(new Dimension(54,54));
		button_evaluation_4.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				updateEvaluationFiltering();
			}
		});	
		panel.add(button_evaluation_4);
		
		button_evaluation_5 = new JToggleButton(Resources.getScaledIcon(Resources.iRoman5, 40));
		button_evaluation_5.setSelected(true);
		button_evaluation_5.setToolTipText("Show/Hide variants evaluated as Type V - Pathogenic Mutation (if column " + Field.evaluation.getName() + " is present)");
		button_evaluation_5.setPreferredSize(new Dimension(54,54));
		button_evaluation_5.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				updateEvaluationFiltering();
			}
		});	
		panel.add(button_evaluation_5);

		JPanel sepPanel2 = new JPanel();
		sepPanel2.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		sepPanel2.setPreferredSize(new Dimension(2, 50));
		panel.add(sepPanel2);
		
		JButton button_add = new JButton(Resources.getScaledIcon(Resources.iColumnMaskNew, 40));
		button_add.setToolTipText("Create a new columns mask for this analysis");
		button_add.setPreferredSize(new Dimension(54,54));
		button_add.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				addNewColumnMask();
			}
		});		
		panel.add(button_add);

		JPanel colSelectionPanelContainer = new JPanel(new GridBagLayout());
		panel.add(colSelectionPanelContainer);
		
		JLabel selectionLabel = new JLabel("Selection of columns to mask");
		colSelectionPanelContainer.add(selectionLabel, new GridBagConstraints(0,1,1,1,1.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,5,0,10), 0, 0));
		
		colSelectionPanel = new JPanel();
		FlowLayout flowLayout_2 = (FlowLayout) colSelectionPanel.getLayout();
		flowLayout_2.setAlignment(FlowLayout.LEFT);

		colSelectionPanelContainer.add(colSelectionPanel, new GridBagConstraints(0,2,1,1,1.0,0.0,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,5,0,10), 0, 0));
		
		selectionGroup = new ButtonGroup();
		
		selection_all = new JRadioButton("Show all columns");
		selection_all.setToolTipText("Request all columns available in the database");
		selection_all.setSelected(true);
		selection_all.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					new Thread(new Runnable() {
						public void run() {
							mainFrame.refreshTableView();
						}
					}, "NavigationPanel.refreshTableView").start();
				}
			}
		});
		colSelectionPanel.add(selection_all);
		selectionGroup.add(selection_all);

		selection_custom = new JRadioButton("Columns mask");
		selection_custom.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					new Thread(new Runnable() {
						public void run() {
							if (loadedSelection == null){
								loadMaskFromProfile();
							}else{
								mainFrame.refreshTableView();
							}
						}
					}, "NavigationPanel.loadMaskFromProfile").start();
				}
			}
		});
		colSelectionPanel.add(selection_custom);
		selectionGroup.add(selection_custom);
		
		customButton = new JButton("Load columns mask");
		customButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						loadMaskFromProfile();
					}
				}, "NavigationPanel.loadMaskFromProfile").start();
			}
		});
		colSelectionPanel.add(customButton);

		ToolbarScrollablePanel scrollablePanel = new ToolbarScrollablePanel(panel, Highlander.getHighlanderObserver(), 40);
	  add(scrollablePanel, BorderLayout.CENTER);
	}

	public void sampleMask(){
		new Thread(new Runnable(){
			public void run(){
				try{
					setIcon();
					if (button_sample_mask.isSelected()){
						AskSamplesDialog apd = new AskSamplesDialog(false, Highlander.getCurrentAnalysis());
						Tools.centerWindow(apd, false);
						apd.setVisible(true);
						if (apd.getSelection() != null){
							table.setSampleFilter(apd.getSelection());
						}
					}else{
						table.setSampleFilter(new TreeSet<String>());
					}
				}catch(Exception ex){
					Tools.exception(ex);
				}
			}
		}, "NavigationPanel.sampleMask").start(); 
	}
	
	public void updateInterestFiltering(){
		new Thread(new Runnable(){
			public void run(){
				try{
					setIcon();
					table.setInterestFilter(button_interest_yes.isSelected(), button_interest_maybe.isSelected(), button_interest_no.isSelected());
				}catch(Exception ex){
					Tools.exception(ex);
				}
			}
		}, "NavigationPanel.updateInterestFiltering").start(); 
	}
	
	public void updateEvaluationFiltering(){
		new Thread(new Runnable(){
			public void run(){
				try{
					setIcon();
					table.setEvaluationFilter(button_evaluation_unclassified.isSelected(), button_evaluation_1.isSelected(), button_evaluation_2.isSelected(), button_evaluation_3.isSelected(), button_evaluation_4.isSelected(), button_evaluation_5.isSelected());
				}catch(Exception ex){
					Tools.exception(ex);
				}
			}
		}, "NavigationPanel.updateEvaluationFiltering").start(); 
	}
	
	public void setIcon(){
		if (button_sample_mask.isSelected() ||
				!button_interest_maybe.isSelected() ||
				!button_interest_no.isSelected() ||
				!button_interest_yes.isSelected() ||
				!button_evaluation_unclassified.isSelected() ||
				!button_evaluation_1.isSelected() ||
				!button_evaluation_2.isSelected() ||
				!button_evaluation_3.isSelected() ||
				!button_evaluation_4.isSelected() ||
				!button_evaluation_5.isSelected() ){
			mainFrame.tabbedPane.setIconAt(2, Resources.getScaledIcon(Resources.iNavigationGlow, 32));
		}else{
			mainFrame.tabbedPane.setIconAt(2, Resources.getScaledIcon(Resources.iNavigation, 32));
		}
	}
	
	public void resetSelections(){
		customButton.setText("Load columns mask");
		customButton.setToolTipText(null);
		loadedSelection = null;
		loadedSelectionPath = null;
		selection_all.setSelected(true);
	}
	
	public void loadMaskFromProfile(){
		String selectionPath = ProfileTree.showProfileDialog(mainFrame, Action.LOAD, UserData.COLUMN_MASK, Highlander.getCurrentAnalysis().toString(), "Load columns mask from profile", loadedSelectionPath);		
		loadMaskFromProfile(selectionPath);
	}
	
	public void loadMaskFromProfile(String selectionPath){
		if (selectionPath != null){
			try{
				loadedSelectionPath = selectionPath;
				loadedSelection = Highlander.getLoggedUser().loadColumnMask(Highlander.getCurrentAnalysis(), selectionPath);
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
					mainFrame.refreshTableView();
				}else{
					selection_custom.setSelected(true);
				}
				Highlander.getHighlanderObserver().setControlName("RESIZE_TOOLBAR");
			}catch(Exception ex){
				Tools.exception(ex);
				JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Can't retreive columns mask from your profile", ex), "Retreive user columns mask",
						JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}
		}
	}

	public String getColumnMaskName(){
		return (selection_all.isSelected()) ? "All" : loadedSelectionPath;
	}

	public List<Field> getColumnMask(){
		if (selection_all.isSelected() || loadedSelection == null){
			return new ArrayList<Field>();
		}else{
			return loadedSelection;
		}
	}

	private void addNewColumnMask(){
		CreateColumnSelection ccs = new CreateColumnSelection(Highlander.getCurrentAnalysis(), UserData.COLUMN_MASK, mainFrame.getColumnSelection());
		Tools.centerWindow(ccs, false);
		ccs.setVisible(true);
		if(!ccs.getSelection().isEmpty()){		
			try{
				String maskName = ccs.getListName();
				if (maskName == null) return;
				if (Highlander.getLoggedUser().doesPersonalDataExists(UserData.COLUMN_MASK, null, maskName)){
					int yesno = JOptionPane.showConfirmDialog(new JFrame(), 
							"You already have a "+UserData.COLUMN_MASK.getName()+" named '"+maskName.replace("~", " -> ")+"', do you want to overwrite it ?", 
							"Overwriting element in your profile", JOptionPane.YES_NO_OPTION , JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iDbSave,64));
					if (yesno == JOptionPane.NO_OPTION)	return;
				}
				List<Field> mask = ccs.getSelection();
				Highlander.getLoggedUser().saveColumnMask(maskName, Highlander.getCurrentAnalysis(), mask);
				loadMaskFromProfile(maskName);
			}catch(Exception ex){
				Tools.exception(ex);
				JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Can't save column mask to your profile", ex), "Saving column mask to your profile",
						JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}
		}
	}
	
	public void showVariantListMask(List<Field> mask){
		loadedSelectionPath = null;
		loadedSelection = mask;
		customButton.setText("Variant list mask");
		StringBuilder sb = new StringBuilder();
		sb.append("<html><b>[Variant list mask]</b>");
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
