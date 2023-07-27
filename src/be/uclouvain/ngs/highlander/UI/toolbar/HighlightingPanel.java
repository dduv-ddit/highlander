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

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.dialog.CreateHeatMapCriterion;
import be.uclouvain.ngs.highlander.UI.dialog.CreateHighlightCriterion;
import be.uclouvain.ngs.highlander.UI.dialog.ProfileTree;
import be.uclouvain.ngs.highlander.UI.dialog.ProfileTree.Action;
import be.uclouvain.ngs.highlander.UI.misc.ToolbarScrollablePanel;
import be.uclouvain.ngs.highlander.UI.table.VariantsTable;
import be.uclouvain.ngs.highlander.administration.users.User.UserData;
import be.uclouvain.ngs.highlander.datatype.HeatMapCriterion;
import be.uclouvain.ngs.highlander.datatype.HighlightCriterion;
import be.uclouvain.ngs.highlander.datatype.HighlightingRule;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JButton;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

public class HighlightingPanel extends JPanel {
	
	private JPanel highlightCriteria;
	private final VariantsTable table;
	
	private String currentHighlightName = null;
	
	public HighlightingPanel(final Highlander mainframe) {
		this.table = mainframe.getVariantTable();
		
		setLayout(new BorderLayout(0, 0));
		
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		
		JButton btnSave = new JButton(Resources.getScaledIcon(Resources.iDbSave, 40));
		btnSave.setToolTipText("Save current highlighting rules set in your profile");
		btnSave.setPreferredSize(new Dimension(54,54));
		btnSave.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					String highlightingName = ProfileTree.showProfileDialog(mainframe, Action.SAVE, UserData.HIGHLIGHTING, Highlander.getCurrentAnalysis().toString(), "Save "+UserData.HIGHLIGHTING.getName()+" to your profile", currentHighlightName);
					if (highlightingName == null) return;
					if (Highlander.getLoggedUser().doesPersonalDataExists(UserData.HIGHLIGHTING, Highlander.getCurrentAnalysis().toString(), highlightingName)){
						int yesno = JOptionPane.showConfirmDialog(new JFrame(), 
								"You already have a highlighting rules set named '"+highlightingName.replace("~", " -> ")+"', do you want to overwrite it ?", 
								"Overwriting highlighting rules set in your profile", JOptionPane.YES_NO_OPTION , JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iDbSave,64));
						if (yesno == JOptionPane.NO_OPTION)	return;
					}
					List<HighlightingRule> list = getHighlightingRules();
					Highlander.getLoggedUser().saveHighlighting(highlightingName, Highlander.getCurrentAnalysis(), list);
					currentHighlightName = highlightingName;
				} catch (Exception ex) {
					Tools.exception(ex);
					JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Error", ex), "Save current highlighting rules set in your profile", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}
			}
		});
		panel.add(btnSave);
		
		JButton btnLoad = new JButton(Resources.getScaledIcon(Resources.iDbLoad, 40));
		btnLoad.setToolTipText("Load a highlighting rules set from your profile");
		btnLoad.setPreferredSize(new Dimension(54,54));
		btnLoad.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String highlightingName = ProfileTree.showProfileDialog(mainframe, Action.LOAD, UserData.HIGHLIGHTING, Highlander.getCurrentAnalysis().toString(), "Load highlighting rules set from your profile");
				if (highlightingName != null){
					if (table == null) {
						JOptionPane.showMessageDialog(new JFrame(), "Table is empty, you must first generate a filter.", "No table", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}else{
						try {
							setHighlightingRules(Highlander.getLoggedUser().loadHighlighting(HighlightingPanel.this, Highlander.getCurrentAnalysis(), highlightingName), highlightingName);
						} catch (Exception ex) {
							Tools.exception(ex);
							JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Error", ex), "Load highlighting rules set from your profile", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
						}						
					}
				}
			}
		});
		panel.add(btnLoad);
		
		JButton button_add_highlighting = new JButton(Resources.getScaledIcon(Resources.iHighlightingAdd, 40));
		button_add_highlighting.setToolTipText("Add highlighting rule");
		button_add_highlighting.setPreferredSize(new Dimension(54,54));
		button_add_highlighting.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (table == null) {
					JOptionPane.showMessageDialog(new JFrame(), "Table is empty, you must first generate a filter.", "No table", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}else{
					CreateHighlightCriterion cfc = new CreateHighlightCriterion(Highlander.getCurrentAnalysis(), HighlightingPanel.this);
					Tools.centerWindow(cfc, false);
					cfc.setVisible(true);
					if(cfc.getCriterion() != null){			
						highlightCriteria.add(cfc.getCriterion());
						Highlander.getCellRenderer().addHighlighting(cfc.getCriterion());
					}
					refresh();
				}
			}
		});
		panel.add(button_add_highlighting);
		
		JButton button_add_heatmap = new JButton(Resources.getScaledIcon(Resources.iHeatMapAdd, 40));
		button_add_heatmap.setToolTipText("Add heat map");
		button_add_heatmap.setPreferredSize(new Dimension(54,54));
		button_add_heatmap.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (table == null) {
					JOptionPane.showMessageDialog(new JFrame(), "Table is empty, you must first generate a filter.", "No table", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}else{
					CreateHeatMapCriterion cfc = new CreateHeatMapCriterion(Highlander.getCurrentAnalysis(), HighlightingPanel.this);
					Tools.centerWindow(cfc, false);
					cfc.setVisible(true);
					if(cfc.getCriterion() != null){			
						highlightCriteria.add(cfc.getCriterion());
						Highlander.getCellRenderer().addHeatMap(cfc.getCriterion());
					}
					refresh();
				}
			}
		});
		panel.add(button_add_heatmap);
		
		highlightCriteria = new JPanel();
		FlowLayout flowLayout = (FlowLayout) highlightCriteria.getLayout();
		flowLayout.setVgap(10);
		flowLayout.setHgap(10);
		flowLayout.setAlignment(FlowLayout.LEADING);
		panel.add(highlightCriteria);
		
		final JButton btnRemoveAll = new JButton(Resources.getScaledIcon(Resources.iCross, 40));
		btnRemoveAll.setToolTipText("Remove all filtering criteria from the list");
		btnRemoveAll.setPreferredSize(new Dimension(54,54));
		btnRemoveAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
	  			@Override
					public void run(){
	  				highlightCriteria.removeAll();
	  				currentHighlightName = null;	  				
	  				Highlander.getCellRenderer().clearHighlighting();
	  				Highlander.getCellRenderer().clearHeatMaps();
	  				refresh();
	  			}
	  		}, "HighlightingPanel.btnRemoveAll").start();
			}
		});
		panel.add(btnRemoveAll);
		
		ToolbarScrollablePanel scrollablePanel = new ToolbarScrollablePanel(panel, Highlander.getHighlanderObserver(), 40);
	  add(scrollablePanel, BorderLayout.CENTER);
	}

	public JPanel getCriteriaPanel(){
		return highlightCriteria;
	}
	
	public VariantsTable getVariantsTable(){
		return table;
	}
	
	public List<HighlightingRule> getHighlightingRules(){
		List<HighlightingRule> list = new ArrayList<HighlightingRule>();
		for (Object o : highlightCriteria.getComponents()){
			list.add((HighlightingRule)o);
		}
		return list;
	}

	public void setHighlightingRules(List<HighlightingRule> list, String highlightingName) throws Exception {
		highlightCriteria.removeAll();
		Highlander.getCellRenderer().clearHighlighting();
		Highlander.getCellRenderer().clearHeatMaps();
		for (HighlightingRule item : list){
			highlightCriteria.add(item);
			switch(item.getRuleType()){
			case HEATMAP:
				Highlander.getCellRenderer().addHeatMap((HeatMapCriterion)item);
				break;
			case HIGHLIGHTING:
				Highlander.getCellRenderer().addHighlighting((HighlightCriterion)item);
				break;
			default:
				System.err.println("Unknown Highlighting rule type : " + item.getRuleType());
				break;
			}
		}
		currentHighlightName = highlightingName;
		refresh();
	}

	public void refresh(){
		validate();
		repaint();
		Highlander.getHighlanderObserver().setControlName("RESIZE_TOOLBAR");
	}

}
