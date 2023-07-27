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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.JTextComponent;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.dialog.ProfileTree.Action;
import be.uclouvain.ngs.highlander.UI.misc.HighlanderObserver;
import be.uclouvain.ngs.highlander.administration.users.User.UserData;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.FiltersTemplate;
import be.uclouvain.ngs.highlander.datatype.filter.ComboFilter;
import be.uclouvain.ngs.highlander.datatype.filter.Filter;

public class CreateTemplate extends JDialog {

	private HighlanderObserver obs = new HighlanderObserver();
	
	private Analysis analysis;
	private Map<String, IndependentFilteringPanel> filters = new LinkedHashMap<>();
	
	private DefaultTableModel tmodel;
	private JTable table;
	private JScrollPane filterScroll;
	private JPanel filterPanel;
	
	private boolean save = false;
	
	public CreateTemplate(Analysis analysis) {
		this(analysis, null);
	}
	
	public CreateTemplate(Analysis analysis, FiltersTemplate template) {
		this.analysis = analysis;
		initUI();
		this.addComponentListener(new ComponentListener() {
			@Override
			public void componentShown(ComponentEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						while (CreateTemplate.this.isVisible()){
							refreshSamplePlaceholders();
							try{
								Thread.sleep(1_000);
							}catch (InterruptedException ex){
								Tools.exception(ex);
							}
						}
					}
				}, "CreateTemplate.refreshSamplePlaceholders").start();
			}
			@Override
			public void componentResized(ComponentEvent arg0) {
				obs.setControlName("RESIZE_TOOLBAR");
			}
			@Override
			public void componentMoved(ComponentEvent arg0) {
			}

			@Override
			public void componentHidden(ComponentEvent arg0) {
			}
		});
		if (template != null) {
			loadTemplate(template);
		}
	}
	
	private void initUI(){
		setModal(true);
		setTitle("Create a filters template");
		setIconImage(Resources.getScaledIcon(Resources.iTemplate, 64).getImage());
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setSize(new Dimension((int)(screenSize.width/1.3),(int)(screenSize.height/1.3)));

		JPanel panel = new JPanel(new BorderLayout(5,5));
		filterScroll = new JScrollPane(getPanelFilters());
		panel.add(filterScroll, BorderLayout.CENTER);
		panel.add(getPanelSamplePlaceholders(), BorderLayout.SOUTH);
		getContentPane().add(panel, BorderLayout.CENTER);
		
		JPanel buttons = new JPanel(new FlowLayout());

		JButton btnOk = new JButton(Resources.getScaledIcon(Resources.iButtonApply, 24));
		btnOk.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				save = true;
				dispose();
			}
		});
		buttons.add(btnOk);
		
		JButton btnCancel = new JButton(Resources.getScaledIcon(Resources.iCross, 24));
		btnCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				save = false;
				dispose();
			}
		});
		buttons.add(btnCancel);

		getContentPane().add(buttons, BorderLayout.SOUTH);
	}
	
	private JPanel getPanelFilters() {
		final JPanel panel = new JPanel(new BorderLayout(5,5));
		
		filterPanel = new JPanel(new GridBagLayout());
		panel.add(filterPanel, BorderLayout.NORTH);
		JButton addFilter = new JButton("Add filter", Resources.getScaledIcon(Resources.iFaintPlus, 24));
		addFilter.setToolTipText("<html><b>Create a filter that will be included in the template.</b><br>"
				+ "Samples ids are irrelevant, select them as placeholder, and define a name in the table below.<br>"
				+ "For example, you can select SAMPLE-1 and SAMPLE-2, then in the table below name them 'Normal sample' and 'Tumor sample'.<br>"
				+ "In your final template, SAMPLE-1 and SAMPLE-2 won't appear, and the user will have to select samples for 'Normal sample' and 'Tumor sample'.<br>"
				+ "Another example would be to select a trio of samples and define them as 'Father', 'Mother' and 'Child'.<br>"
				+ "If your filter is larger than the window and blue arrows don't appear to scroll, resize a little bit the window !</html>");
		addFilter.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				GridBagConstraints gbc_panel = new GridBagConstraints();
				gbc_panel.insets = new Insets(5, 5, 5, 5);
				gbc_panel.weightx = 1.0;
				gbc_panel.weighty = 1.0;
				gbc_panel.fill = GridBagConstraints.BOTH;
				gbc_panel.gridy = filters.size()+1;
				gbc_panel.gridx = 0;
				filterPanel.add(addFilter(), gbc_panel);		
				filterPanel.validate();
				panel.validate();
				filterScroll.validate();
			}
		});
		filterPanel.add(addFilter, new GridBagConstraints(0, 0, 1, 1, 1.0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));	
		
		JPanel centerPanel = new JPanel(new BorderLayout(5,5));
		panel.add(centerPanel, BorderLayout.CENTER);
		
		return panel;
	}
	
	private JScrollPane getPanelSamplePlaceholders() {
		tmodel = new DefaultTableModel(	) {
			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				if (columnIndex == 0) return false;
				return true;
			}
		};
		tmodel.addColumn("Sample");
		tmodel.addColumn("Placeholder");
		
		table = new JTable(tmodel){
			@Override
			public boolean editCellAt(int row, int column, java.util.EventObject e){
        boolean result = super.editCellAt(row, column, e);
        final Component editor = getEditorComponent();
        if (editor == null || !(editor instanceof JTextComponent)) {
            return result;
        }
        if (e instanceof KeyEvent) {
            ((JTextComponent) editor).selectAll();
        }
        return result;
			} 
		//JTable.processKeyBinding has a bug (it doesn't check if the meta key is pressed before triggering the cell editor), causing problem on MacOSX
			@Override
			protected boolean processKeyBinding(KeyStroke ks, KeyEvent e,
					int condition, boolean pressed) {
				int code = e.getKeyCode();
				if (code == KeyEvent.VK_SHIFT || code == KeyEvent.VK_CONTROL ||
						code == KeyEvent.VK_ALT || code == KeyEvent.VK_META) {
					return false;
				}else{
					return super.processKeyBinding(ks, e, condition, pressed);
				}
			}
		};
		table.setCellSelectionEnabled(true);
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		table.getTableHeader().setReorderingAllowed(false);
		
		JScrollPane scrollPane = new JScrollPane(table);
		Border outterBorder = BorderFactory.createEmptyBorder(10,10,10,10);
		TitledBorder innerBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Sample placeholders");
		Border compoundBorder = BorderFactory.createCompoundBorder(outterBorder, innerBorder);
		scrollPane.setBorder(compoundBorder);
		return scrollPane;		
	}
	
	private JPanel addFilter() {
		return addFilter(null, null);
	}
	
	private JPanel addFilter(String loadName, String saveString) {
		final IndependentFilteringPanel filter = new IndependentFilteringPanel(null, obs);
		String name = "Filter " + (filters.size()+1);
		
		if (loadName != null && saveString != null) {
			try {
				filter.setFilter((ComboFilter)new ComboFilter().loadCriterion(filter, saveString), loadName);
				name = loadName;
			} catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		filters.put(name, filter);		
		final JPanel panel = new JPanel(new BorderLayout());
		Border outterBorder = BorderFactory.createEmptyBorder(10,10,10,10);
		final TitledBorder innerBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), name);
		Border compoundBorder = BorderFactory.createCompoundBorder(outterBorder, innerBorder);
		panel.setToolTipText("<html><b>Create a filter that will be included in the template.</b><br>"
				+ "Samples ids are irrelevant, select them as placeholder, and define a name in the table below.<br>"
				+ "For example, you can select SAMPLE-1 and SAMPLE-2, then in the table below name them 'Normal sample' and 'Tumor sample'.<br>"
				+ "In your final template, SAMPLE-1 and SAMPLE-2 won't appear, and the user will have to select samples for 'Normal sample' and 'Tumor sample'.<br>"
				+ "Another example would be to select a trio of samples and define them as 'Father', 'Mother' and 'Child'.<br>"
				+ "If your filter is larger than the window and blue arrows don't appear to scroll, resize a little bit the window !</html>");
		panel.setBorder(compoundBorder);
		panel.add(filter, BorderLayout.CENTER);
			
		
		JPanel buttons = new JPanel(new FlowLayout());
		JButton btnRename = new JButton(Resources.getScaledIcon(Resources.iEditPen, 40));
		btnRename.setToolTipText("Rename filter");
		btnRename.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {		
				final String oldName = innerBorder.getTitle();
				Object newName = null;
				do {
					newName = JOptionPane.showInputDialog(CreateTemplate.this, "Set a new name for '" + oldName + "':", "Rename filter", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iTemplate, 64), null, null);
					if (newName != null && filters.containsKey(newName.toString())) {
						JOptionPane.showMessageDialog(CreateTemplate.this, "You cannot have two filters with the same name", "Rename filter",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}while(newName != null && filters.containsKey(newName.toString()));
				if (newName != null) {
					filters.put(newName.toString(), filters.remove(oldName));
					innerBorder.setTitle(newName.toString());
					panel.validate();
					panel.repaint();
				}
			}
		});
		buttons.add(btnRename);
		panel.add(buttons, BorderLayout.WEST);

		return panel;
	}

	private void refreshSamplePlaceholders() {
		Set<String> samples = new TreeSet<>();
		for (IndependentFilteringPanel ifp : filters.values()) {
			if (ifp.getFilter() != null) {
				samples.addAll(ifp.getFilter().getUserDefinedSamples(false));
			}
		}
		for (int i=tmodel.getRowCount()-1 ; i >=0  ; i--) {
			String sample = tmodel.getValueAt(i, 0).toString();
			if (!samples.contains(sample)) {
				tmodel.removeRow(i);
			}else {
				samples.remove(sample);
			}
		}
		for (String sample : samples) {
			tmodel.addRow(new Object[] {sample, ""});
		}
	}
	
	public boolean needToSave() {
		return save;
	}
	
	public void saveTemplate() {
		String templateName = ProfileTree.showProfileDialog(this, Action.SAVE, UserData.FILTERS_TEMPLATE, Highlander.getCurrentAnalysis().toString(), "Save "+UserData.FILTERS_TEMPLATE.getName()+" to your profile");
		saveTemplate(templateName);
	}
	
	public void loadTemplate(FiltersTemplate template) {
		for (Entry<String, String> e : template.getFiltersSaveStrings().entrySet()) {
			GridBagConstraints gbc_panel = new GridBagConstraints();
			gbc_panel.insets = new Insets(5, 5, 5, 5);
			gbc_panel.weightx = 1.0;
			gbc_panel.weighty = 1.0;
			gbc_panel.fill = GridBagConstraints.BOTH;
			gbc_panel.gridy = filters.size()+1;
			gbc_panel.gridx = 0;
			filterPanel.add(addFilter(e.getKey(), e.getValue()), gbc_panel);		
			filterPanel.validate();
			filterScroll.validate();
		}
		refreshSamplePlaceholders();
		for (int i=0 ; i < tmodel.getRowCount() ; i++) {
			tmodel.setValueAt(tmodel.getValueAt(i, 0).toString().replace("@", ""), i, 1);
		}
	}
	
	public void saveTemplate(String templateName) {
		Map<String,String> placeholders = new TreeMap<>();
		for (int i=0 ; i < tmodel.getRowCount() ; i++) {
			if (tmodel.getValueAt(i, 1) == null || tmodel.getValueAt(i, 1).toString().length() == 0) {
				JOptionPane.showMessageDialog(CreateTemplate.this, "You must define a placeholder for each sample", "Save template",
						JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				return;
			}
			if (Filter.containsForbiddenCharacters(tmodel.getValueAt(i, 1).toString())){
				JOptionPane.showMessageDialog(this, "Placeholders cannot contains the following characters: "+Filter.getForbiddenCharacters(), "Save template", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				return;				
			}
			String sample = tmodel.getValueAt(i, 0).toString();
			String placeholder = "@" + tmodel.getValueAt(i, 1).toString() + "@";
			placeholders.put(sample, placeholder);
		}
		for (String filterName : filters.keySet()) {
			if (Filter.containsForbiddenCharacters(filterName.toString())){
				JOptionPane.showMessageDialog(this, "Filter names cannot contains the following characters: "+Filter.getForbiddenCharacters(), "Save template", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				return;				
			}
		}
		Map<String,String> filtersSaveStrings = new TreeMap<>();
		for (String filterName : filters.keySet()) {
			String filterSaveString = filters.get(filterName).getFilter().getSaveString();
			for (int i=0 ; i < tmodel.getRowCount() ; i++) {
				String sample = tmodel.getValueAt(i, 0).toString();
				String placeholder = "@" + tmodel.getValueAt(i, 1).toString() + "@";
				filterSaveString = filterSaveString.replace(sample, placeholder);
			}
			filtersSaveStrings.put(filterName, filterSaveString);
		}
		try {			
			if (templateName == null) return;
			if (Highlander.getLoggedUser().doesPersonalDataExists(UserData.FILTERS_TEMPLATE, analysis.toString(), templateName)){
				int yesno = JOptionPane.showConfirmDialog(new JFrame(), 
						"You already have a "+UserData.FILTERS_TEMPLATE.getName()+" named '"+templateName.replace("~", " -> ")+"', do you want to overwrite it ?", 
						"Overwriting "+UserData.FILTERS_TEMPLATE.getName()+" in your profile", JOptionPane.YES_NO_OPTION , JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iDbSave,64));
				if (yesno == JOptionPane.NO_OPTION)	return;
			}
			FiltersTemplate template = new FiltersTemplate(analysis, filtersSaveStrings, templateName);
			Highlander.getLoggedUser().saveFiltersTemplate(template, templateName);
		} catch (Exception ex) {
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Error", ex), "Save template in your profile", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
	}
}
