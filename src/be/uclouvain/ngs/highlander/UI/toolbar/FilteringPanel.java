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
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
//import javax.swing.JToggleButton;
import javax.swing.JScrollPane;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.dialog.CreateCustomFilter;
import be.uclouvain.ngs.highlander.UI.dialog.CreateMagicFilter;
import be.uclouvain.ngs.highlander.UI.dialog.FilteringTree;
import be.uclouvain.ngs.highlander.UI.dialog.ProfileTree;
import be.uclouvain.ngs.highlander.UI.dialog.FilteringTree.Result;
import be.uclouvain.ngs.highlander.UI.dialog.ProfileTree.Action;
import be.uclouvain.ngs.highlander.UI.misc.HighlanderObserver;
import be.uclouvain.ngs.highlander.UI.misc.ToolbarScrollablePanel;
import be.uclouvain.ngs.highlander.UI.table.VariantsTable;
import be.uclouvain.ngs.highlander.administration.users.User.UserData;
import be.uclouvain.ngs.highlander.datatype.filter.ComboFilter;
import be.uclouvain.ngs.highlander.datatype.filter.CustomFilter;
import be.uclouvain.ngs.highlander.datatype.filter.Filter.FilterType;
import be.uclouvain.ngs.highlander.datatype.filter.Filter.LogicalOperator;

public class FilteringPanel extends JPanel {

	public enum AddChoice {NewCustom, NewMagic, LoadCombo}

	private JPanel filtersPanel;
	private final Highlander mainFrame;
	protected final HighlanderObserver obs;
	private final VariantsTable variantsTable;
	private JButton btnAddCustom;
	private JButton btnAddCustomAnd;
	private JButton btnAddCustomOr;
	private JButton btnAddmagic;
	private JButton btnAddmagicAnd;
	private JButton btnAddmagicOr;
	private JButton btnLoad;
	private JButton btnLoadAnd;
	private JButton btnLoadOr;
	//private JToggleButton btnAutoApply = new JToggleButton(Resources.getScaledIcon(Resources.iButtonAutoApplyGrey, 40));

	private ComboFilter filter = null;
	private String currentFilterName = null;

	public FilteringPanel(Highlander mainFrame, HighlanderObserver obs) {
		this.mainFrame = mainFrame;
		if (mainFrame != null) this.variantsTable = mainFrame.getVariantTable();
		else this.variantsTable = null;
		this.obs = obs;
		initUI();
	}

	protected void initUI(){
		setLayout(new BorderLayout(0, 0));
		JPanel panel = new JPanel(new BorderLayout(0, 0));

		JPanel panel_w = new JPanel();
		panel.add(panel_w, BorderLayout.WEST);
		initWestPanel(panel_w);

		JPanel panel_e = new JPanel();
		panel.add(panel_e, BorderLayout.EAST);
		initEastPanel(panel_e);

		initCenterPanel(panel);

		//ScrollablePanel for the whole bar ... more user-friendly for the center panel only (and should be OK even for low resolutions)
		//ToolbarScrollablePanel scrollablePanel = new ToolbarScrollablePanel(panel, Highlander.getHighlanderObserver(), 40);
		//add(scrollablePanel, BorderLayout.CENTER);
		add(panel, BorderLayout.CENTER);
	}

	protected void initWestPanel(JPanel panel_w){
		JButton btnTreeView = new JButton(Resources.getScaledIcon(Resources.iFilterTree, 40));
		btnTreeView.setToolTipText("View/Build filter in a tree interface");
		btnTreeView.setPreferredSize(new Dimension(54,54));
		btnTreeView.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				treeView();
			}
		});
		panel_w.add(btnTreeView);

		JButton btnSave = new JButton(Resources.getScaledIcon(Resources.iDbSave, 40));
		btnSave.setToolTipText("Save current filters in your profile");
		btnSave.setPreferredSize(new Dimension(54,54));
		btnSave.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				saveCurrentFilter(mainFrame);
			}
		});
		panel_w.add(btnSave);

		btnLoad = new JButton(Resources.getScaledIcon(Resources.iDbLoad, 40));
		btnLoad.setToolTipText("Load a filter from your profile");
		btnLoad.setPreferredSize(new Dimension(54,54));
		btnLoad.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				addFilter(AddChoice.LoadCombo, null, mainFrame);
			}
		});
		panel_w.add(btnLoad);

		btnLoadAnd = new JButton(Resources.getScaledIcon(Resources.iFilterLoadAnd, 40));
		btnLoadAnd.setToolTipText("Add a filter from your profile to the existing ones, using the logical operator AND (i.e. results will be the INTERSECTION of filters)");
		btnLoadAnd.setPreferredSize(new Dimension(54,54));
		btnLoadAnd.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				addFilter(AddChoice.LoadCombo, LogicalOperator.AND, mainFrame);
			}
		});
		btnLoadAnd.setVisible(false);
		panel_w.add(btnLoadAnd);

		btnLoadOr = new JButton(Resources.getScaledIcon(Resources.iFilterLoadOr, 40));
		btnLoadOr.setToolTipText("Add a filter from your profile to the existing ones, using the logical operator OR (i.e. results will be the UNION of filters)");
		btnLoadOr.setPreferredSize(new Dimension(54,54));
		btnLoadOr.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				addFilter(AddChoice.LoadCombo, LogicalOperator.OR, mainFrame);
			}
		});
		btnLoadOr.setVisible(false);
		panel_w.add(btnLoadOr);

		btnAddCustom = new JButton(Resources.getScaledIcon(Resources.iFilterAddCustom, 40));
		btnAddCustom.setToolTipText("Add a new custom filter");
		btnAddCustom.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						addFilter(AddChoice.NewCustom, null, mainFrame);
					}
				}, "FilteringPanel.addFilter").start();				
			}
		});
		btnAddCustom.setPreferredSize(new Dimension(54,54));
		panel_w.add(btnAddCustom);

		btnAddCustomAnd = new JButton(Resources.getScaledIcon(Resources.iFilterAddCustomAnd, 40));
		btnAddCustomAnd.setToolTipText("Add a custom filter to the existing ones, using the logical operator AND (i.e. results will be the INTERSECTION of filters)");
		btnAddCustomAnd.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						addFilter(AddChoice.NewCustom, LogicalOperator.AND, mainFrame);
					}
				}, "FilteringPanel.addFilter").start();
			}
		});
		btnAddCustomAnd.setPreferredSize(new Dimension(54,54));
		btnAddCustomAnd.setVisible(false);
		panel_w.add(btnAddCustomAnd);

		btnAddCustomOr = new JButton(Resources.getScaledIcon(Resources.iFilterAddCustomOr, 40));
		btnAddCustomOr.setToolTipText("Add a custom filter to the existing ones, using the logical operator OR (i.e. results will be the UNION of filters)");
		btnAddCustomOr.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						addFilter(AddChoice.NewCustom, LogicalOperator.OR, mainFrame);
					}
				}, "FilteringPanel.addFilter").start();
			}
		});
		btnAddCustomOr.setPreferredSize(new Dimension(54,54));
		btnAddCustomOr.setVisible(false);
		panel_w.add(btnAddCustomOr);

		btnAddmagic = new JButton(Resources.getScaledIcon(Resources.iFilterAddMagic, 40));
		btnAddmagic.setToolTipText("Add a new magic filter");
		btnAddmagic.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						addFilter(AddChoice.NewMagic, null, mainFrame);
					}
				}, "FilteringPanel.addFilter").start();
			}
		});
		btnAddmagic.setPreferredSize(new Dimension(54,54));
		panel_w.add(btnAddmagic);

		btnAddmagicAnd = new JButton(Resources.getScaledIcon(Resources.iFilterAddMagicAnd, 40));
		btnAddmagicAnd.setToolTipText("Add a magic filter to the existing ones, using the logical operator AND (i.e. results will be the INTERSECTION of filters)");
		btnAddmagicAnd.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						addFilter(AddChoice.NewMagic, LogicalOperator.AND, mainFrame);
					}
				}, "FilteringPanel.addFilter").start();
			}
		});
		btnAddmagicAnd.setPreferredSize(new Dimension(54,54));
		btnAddmagicAnd.setVisible(false);
		panel_w.add(btnAddmagicAnd);

		btnAddmagicOr = new JButton(Resources.getScaledIcon(Resources.iFilterAddMagicOr, 40));
		btnAddmagicOr.setToolTipText("Add a magic filter to the existing ones, using the logical operator OR (i.e. results will be the UNION of filters)");
		btnAddmagicOr.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						addFilter(AddChoice.NewMagic, LogicalOperator.OR, mainFrame);
					}
				}, "FilteringPanel.addFilter").start();
			}
		});
		btnAddmagicOr.setPreferredSize(new Dimension(54,54));
		btnAddmagicOr.setVisible(false);
		panel_w.add(btnAddmagicOr);
	}

	protected void initEastPanel(JPanel panel_e){
		final JButton btnRemoveAll = new JButton(Resources.getScaledIcon(Resources.iCross, 40));
		btnRemoveAll.setToolTipText("Remove all filtering criteria from the list");
		btnRemoveAll.setPreferredSize(new Dimension(54,54));
		btnRemoveAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						clearFilter();
					}
				}, "FilteringPanel.clearFilter").start();
			}
		});
		panel_e.add(btnRemoveAll);

		final JButton btnSamples = new JButton(Resources.getScaledIcon(Resources.iPatients, 40));
		btnSamples.setToolTipText("Get number and list of samples included using current filtering criteria");
		btnSamples.setPreferredSize(new Dimension(54,54));
		btnSamples.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						listSamples();
					}
				}, "FilteringPanel.listSamples").start();
			}
		});
		panel_e.add(btnSamples);

		final JButton btnCount = new JButton(Resources.getScaledIcon(Resources.iCount, 40));
		btnCount.setToolTipText("Count variants from the database using current filters");
		btnCount.setPreferredSize(new Dimension(54,54));
		btnCount.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						countVariants();
					}
				}, "FilteringPanel.countVariants").start();
			}
		});
		panel_e.add(btnCount);

		final JButton btnApply = new JButton(Resources.getScaledIcon(Resources.iButtonApply, 40));
		btnApply.setToolTipText("Fetch variants from the database using current filters");
		btnApply.setPreferredSize(new Dimension(54,54));
		btnApply.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						mainFrame.refreshTable();
					}
				}, "FilteringPanel.refreshTable").start();
			}
		});
		panel_e.add(btnApply);
		/* Not so useful after all ...
		btnAutoApply = new JToggleButton(Resources.getScaledIcon(Resources.iButtonAutoApplyGrey, 40));
		btnAutoApply.setSelectedIcon(Resources.getScaledIcon(Resources.iButtonAutoApply, 40));
		btnAutoApply.setRolloverIcon(Resources.getScaledIcon(Resources.iButtonAutoApply, 40));
		btnAutoApply.setRolloverSelectedIcon(Resources.getScaledIcon(Resources.iButtonAutoApplyGrey, 40));
		btnAutoApply.setRolloverEnabled(true);
		btnAutoApply.setToolTipText("Apply automatically each filtering criteria whenever they are created (can be slow if you plan to build a complex filter)");
		btnAutoApply.setPreferredSize(new Dimension(54,54));
		btnAutoApply.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {				
				btnApply.setVisible(!btnAutoApply.isSelected());
				if (btnAutoApply.isSelected()){
					new Thread(new Runnable(){
						public void run(){
							mainFrame.refreshTable();
						}
					}, "FilteringPanel.refreshTable").start();
				}
			}
		});
		panel_e.add(btnAutoApply);
		 */
	}

	protected void initCenterPanel(JPanel mainPanel){
		filtersPanel = new JPanel();
		FlowLayout flowLayout = (FlowLayout) filtersPanel.getLayout();
		flowLayout.setAlignment(FlowLayout.LEADING);
		//mainPanel.add(filteringCriteria, BorderLayout.CENTER);
		ToolbarScrollablePanel scrollablePanel = new ToolbarScrollablePanel(filtersPanel, Highlander.getHighlanderObserver(), 40);
		mainPanel.add(scrollablePanel, BorderLayout.CENTER);
	}

	private void treeView(){
		FilteringTree treeView = new FilteringTree(this);
		Tools.centerWindow(treeView, true);
		treeView.setVisible(true);
		if (treeView.getResult() == Result.SUBMIT){
			new Thread(new Runnable(){
				@Override
				public void run(){
					mainFrame.refreshTable();
				}
			}, "FilteringPanel.refreshTable").start();
		}else if (treeView.getResult() == Result.COUNT){
			new Thread(new Runnable(){
				@Override
				public void run(){
					countVariants();
				}
			}, "FilteringPanel.countVariants").start();
		}
	}

	public void saveCurrentFilter(Window parent){
		try {
			String filterName = ProfileTree.showProfileDialog(parent, Action.SAVE, UserData.FILTER, Highlander.getCurrentAnalysis().toString(), "Save "+UserData.FILTER.getName()+" to your profile", currentFilterName);
			if (filterName == null) return;
			if (Highlander.getLoggedUser().doesPersonalDataExists(UserData.FILTER, Highlander.getCurrentAnalysis().toString(), filterName)){
				int yesno = JOptionPane.showConfirmDialog(new JFrame(), 
						"You already have a filter named '"+filterName.replace("~", " -> ")+"', do you want to overwrite it ?", 
						"Overwriting filter in your profile", JOptionPane.YES_NO_OPTION , JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iDbSave,64));
				if (yesno == JOptionPane.NO_OPTION)	return;
			}
			Highlander.getLoggedUser().saveFilter(filterName, Highlander.getCurrentAnalysis(), filter, false);			
			currentFilterName = filterName;
		} catch (Exception ex) {
			Tools.exception(ex);
			JOptionPane.showMessageDialog(FilteringPanel.this, Tools.getMessage("Error", ex), "Save current criteria list in your profile", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
	}

	public void addFilter(AddChoice addChoice, LogicalOperator operator, Window parent){
		if (operator == null){
			switch(addChoice){
			case NewCustom:
				CreateCustomFilter cfc = new CreateCustomFilter(Highlander.getCurrentAnalysis(), FilteringPanel.this);
				Tools.centerWindow(cfc, false);
				cfc.setVisible(true);
				if(cfc.getCriterion() != null){
					filter = new ComboFilter(FilteringPanel.this, cfc.getCriterion());
					filtersPanel.add(filter, 0);
					refresh();
				}
				break;
			case NewMagic:
				CreateMagicFilter cmf = new CreateMagicFilter(FilteringPanel.this);
				Tools.centerWindow(cmf, false);
				cmf.setVisible(true);
				if (cmf.getCriterion() != null){
					filter = new ComboFilter(FilteringPanel.this, cmf.getCriterion());
					filtersPanel.add(filter, 0);
					refresh();
				}
				break;
			case LoadCombo:
				String name = ProfileTree.showProfileDialog(parent, Action.LOAD, UserData.FILTER, Highlander.getCurrentAnalysis().toString());
				if (name != null){
					try {
						setFilter(Highlander.getLoggedUser().loadFilter(FilteringPanel.this, Highlander.getCurrentAnalysis(), name), name);				
					} catch (Exception ex) {
						Tools.exception(ex);
						JOptionPane.showMessageDialog(FilteringPanel.this, Tools.getMessage("Error", ex), "Load criteria list from your profile", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
				break;
			}
		}else{
			switch(addChoice){
			case LoadCombo:
				filter.addProfileFilter(operator);
				break;
			case NewCustom:
				filter.addNewCustomFilter(operator);
				break;
			case NewMagic:
				filter.addNewMagicFilter(operator);
				break;
			}
		}
	}

	public ComboFilter getFilter(){
		return filter;
	}

	public String getFilterName(){
		return currentFilterName;
	}

	public JPanel getFiltersPanel(){
		return filtersPanel;
	}

	public VariantsTable getVariantsTable(){
		return variantsTable;
	}

	public void setFilter(ComboFilter combo, String filtername) {
		filtersPanel.removeAll();
		combo.setFilteringPanel(this);
		filtersPanel.add(combo);
		filter = combo;
		currentFilterName = filtername;
		if (filtername == null || filtername.length() == 0) currentFilterName = null;
		refresh();
	}

	public void refresh(){
		if (filter == null){
			btnAddCustom.setVisible(true);
			btnAddCustomOr.setVisible(false);
			btnAddCustomAnd.setVisible(false);			
			btnAddmagic.setVisible(true);
			btnAddmagicOr.setVisible(false);
			btnAddmagicAnd.setVisible(false);
			btnLoad.setVisible(true);
			btnLoadOr.setVisible(false);
			btnLoadAnd.setVisible(false);			
		}else if (filter.isSimple()){
			if (filter.getFilter().getFilterType() != FilterType.CUSTOM || filter.getFilter().isSimple()){
				btnAddCustom.setVisible(false);
				btnAddCustomOr.setVisible(true);
				btnAddCustomAnd.setVisible(true);
				btnAddmagic.setVisible(false);
				btnAddmagicOr.setVisible(true);
				btnAddmagicAnd.setVisible(true);
				btnLoad.setVisible(false);
				btnLoadOr.setVisible(true);
				btnLoadAnd.setVisible(true);
			}else{
				btnAddCustom.setVisible(false);
				btnAddCustomOr.setVisible(((CustomFilter)filter.getFilter()).getLogicalOperator() == LogicalOperator.OR);
				btnAddCustomAnd.setVisible(((CustomFilter)filter.getFilter()).getLogicalOperator() == LogicalOperator.AND);
				btnAddmagic.setVisible(false);
				btnAddmagicOr.setVisible(true);
				btnAddmagicAnd.setVisible(true);
				btnLoad.setVisible(false);
				btnLoadOr.setVisible(true);
				btnLoadAnd.setVisible(true);
			}
		}else{
			btnAddCustom.setVisible(false);
			btnAddCustomOr.setVisible(filter.getLogicalOperator() == LogicalOperator.OR);
			btnAddCustomAnd.setVisible(filter.getLogicalOperator() == LogicalOperator.AND);
			btnAddmagic.setVisible(false);
			btnAddmagicOr.setVisible(filter.getLogicalOperator() == LogicalOperator.OR);
			btnAddmagicAnd.setVisible(filter.getLogicalOperator() == LogicalOperator.AND);
			btnLoad.setVisible(false);
			btnLoadOr.setVisible(filter.getLogicalOperator() == LogicalOperator.OR);
			btnLoadAnd.setVisible(filter.getLogicalOperator() == LogicalOperator.AND);
		}
		validate();
		repaint();
		Highlander.getHighlanderObserver().setControlName("RESIZE_TOOLBAR");
		//if (btnAutoApply.isSelected()) mainFrame.refreshTable();
	}

	public void clearFilter(){
		filtersPanel.removeAll();
		filter = null;
		currentFilterName = null;
		refresh();
	}

	public void listSamples(){
		Set<String> samples = new TreeSet<String>();
		JFrame frame = new JFrame();
		Tools.centerWindow(frame, false);
		Highlander.waitingPanel.start(true);
		if (filter != null && !filter.checkProfileValues()){
			JOptionPane.showMessageDialog(frame, "Cannot retreive '"+filter.getFirstInexistantProfileList()+"' value list from your profile." +
					"\nVerify that this list has not been deleted, renamed or is empty.", "Executing query",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iUserListDelete,64));
		}else if (filter != null){
			samples = filter.getSamples();
		}
		Highlander.waitingPanel.stop();
		JList<String> list = new JList<>(samples.toArray(new String[0]));
		JScrollPane scrollPane = new JScrollPane(list);
		JOptionPane.showMessageDialog(frame, scrollPane, "Query includes " + samples.size() + " samples",
				JOptionPane.INFORMATION_MESSAGE, Resources.getScaledIcon(Resources.iPatients,64));
	}

	public void countVariants(){
		try {
			Highlander.waitingPanel.start(true);
			CustomFilter singleRealCustomFilter = null;
			if (filter.isSimple() && filter.getFilter().getFilterType() == FilterType.CUSTOM){
				singleRealCustomFilter = (CustomFilter)filter.getFilter();
			}
			JFrame frame = new JFrame();
			Tools.centerWindow(frame, false);
			if (singleRealCustomFilter != null && !singleRealCustomFilter.checkProfileValues()){
				JOptionPane.showMessageDialog(frame, "Cannot retreive '"+singleRealCustomFilter.getFirstInexistantProfileList()+"' value list from your profile." +
						"\nVerify that this list has not been deleted, renamed or is empty.", "Executing query",
						JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iUserListDelete,64));
			}else if (singleRealCustomFilter != null){
				String count = Tools.doubleToString(singleRealCustomFilter.retreiveCount(filter.getAllSamples()), 0, false);
				JOptionPane.showMessageDialog(frame, "Your filters will retreive " + count + " variants.", "Count variants",
						JOptionPane.INFORMATION_MESSAGE, Resources.getScaledIcon(Resources.iCount,64));
			}else{
				String count = Tools.doubleToString(filter.getResultIds(filter.getAllSamples()).size(), 0, false);
				JOptionPane.showMessageDialog(frame, "Your filters will retreive " + count + " variants.", "Count variants",
						JOptionPane.INFORMATION_MESSAGE, Resources.getScaledIcon(Resources.iCount,64));

			}
		} catch (com.mysql.cj.jdbc.exceptions.MySQLStatementCancelledException ex){
			Highlander.waitingPanel.setProgressString("Cancelling query", true);
		}catch (Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(this, Tools.getMessage("Problem when executing query", ex), "Executing query",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}finally{
			Highlander.waitingPanel.forceStop();
		}			
	}

	public boolean checkForChangesToBeSaved(){
		if (currentFilterName != null){
			try{
				if(!(Highlander.getLoggedUser().compareFilter(currentFilterName, Highlander.getCurrentAnalysis(), filter))){
					int yesno = JOptionPane.showConfirmDialog(new JFrame(), 
							"Do you want to save the modifications of your filter '"+currentFilterName+"' ?", 
							"Saving filter", JOptionPane.YES_NO_CANCEL_OPTION , JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iFilter,64));
					if (yesno == JOptionPane.YES_OPTION)	{
						saveCurrentFilter(mainFrame);
						return true;
					}else if(yesno == JOptionPane.NO_OPTION)	{
						return true;
					}else{
						return false;
					}
				}
			}catch(Exception ex){
				Tools.exception(ex);
				return false;
			}
		}else if (filter != null){
			int yesno = JOptionPane.showConfirmDialog(new JFrame(), 
					"Do you want to save your filter ?", 
					"Saving filter", JOptionPane.YES_NO_CANCEL_OPTION , JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iFilter,64));
			if (yesno == JOptionPane.YES_OPTION)	{
				saveCurrentFilter(mainFrame);
				return true;
			}else if(yesno == JOptionPane.NO_OPTION)	{
				return true;
			}else{
				return false;
			}
		}
		return true;
	}
}
