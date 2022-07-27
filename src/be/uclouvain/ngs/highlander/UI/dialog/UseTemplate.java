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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.text.JTextComponent;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.dialog.ProfileTree.Action;
import be.uclouvain.ngs.highlander.administration.users.User;
import be.uclouvain.ngs.highlander.administration.users.User.UserData;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.FiltersTemplate;
import be.uclouvain.ngs.highlander.datatype.filter.ComboFilter;
import be.uclouvain.ngs.highlander.datatype.filter.Filter;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.AutoCompleteSupport;

public class UseTemplate extends JDialog {

	private FiltersTemplate template;

	private DefaultTableModel tmodel;
	private JTable table;
	private JComboBox<String> sampleBox;
	private String[] availableSamples;
	private List<JCheckBox> filterCheckBoxes = new ArrayList<>();

	private boolean saved = false;

	public UseTemplate(FiltersTemplate template) {
		this.template = template;
		initUI();
	}

	private void initUI(){
		setModal(true);
		setTitle("Generate filters using a template");
		setIconImage(Resources.getScaledIcon(Resources.iTemplate, 64).getImage());
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setSize(new Dimension((int)(screenSize.width/1.3),(int)(screenSize.height/1.3)));

		List<String> listap = new ArrayList<String>(); 
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT `value` FROM "+template.getAnalysis().getFromPossibleValues()+" WHERE `field` = 'sample' ORDER BY `value`")) {
			while (res.next()){
				listap.add(res.getString(1));
			}
			availableSamples = listap.toArray(new String[0]);
		}catch(Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(this, Tools.getMessage("Can't retreive sample list", ex), "Fetching sample list",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}

		JPanel panel = new JPanel(new BorderLayout(5,5));
		panel.add(getPanelSamplePlaceholders(), BorderLayout.NORTH);
		panel.add(getPanelFilters(), BorderLayout.CENTER);
		JScrollPane scroll = new JScrollPane(panel);
		getContentPane().add(scroll, BorderLayout.CENTER);

		JPanel buttons = new JPanel(new FlowLayout());

		JButton btnOk = new JButton(Resources.getScaledIcon(Resources.iButtonApply, 24));
		btnOk.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				saveTemplate();
				if (saved) dispose();
			}
		});
		buttons.add(btnOk);

		JButton btnCancel = new JButton(Resources.getScaledIcon(Resources.iCross, 24));
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				dispose();
			}
		});
		buttons.add(btnCancel);

		getContentPane().add(buttons, BorderLayout.SOUTH);
	}

	private JPanel getPanelFilters() {
		JLabel titleLabel = new JLabel(template.toString());
		titleLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		int row = 0;
		JPanel panel = new JPanel(new GridBagLayout());
		panel.add(titleLabel, new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
		for (Entry<String, String> e : template.getFiltersSaveStrings().entrySet()) {
			final JCheckBox filterCheckBox = new JCheckBox(e.getKey() , true);
			filterCheckBox.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
			panel.add(filterCheckBox, new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			final JPanel filter = new JPanel(new BorderLayout());
			try {
				filter.add(ProfileTree.getDescriptionFilter((ComboFilter)new ComboFilter().loadCriterion(null, e.getValue())), BorderLayout.CENTER);
			}catch(Exception ex) {
				ex.printStackTrace();
				JLabel label = new JLabel(" "+(new ComboFilter()).parseSaveString(e.getValue())+" ");
				label.setOpaque(true);
				filter.add(label, BorderLayout.CENTER);
			}
			panel.add(filter, new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			filterCheckBox.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					for (Component c : filter.getComponents())
						c.setEnabled(filterCheckBox.isSelected());
				}
			});
			filterCheckBoxes.add(filterCheckBox);
		}
		panel.add(new JPanel(), new GridBagConstraints(0, row++, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(5, 5, 5, 5), 0, 0));
		return panel;
	}

	private JPanel getPanelSamplePlaceholders() {
		tmodel = new DefaultTableModel(	) {
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				if (columnIndex == 0) return false;
				return true;
			}
		};
		tmodel.addColumn("Placeholder");
		tmodel.addColumn("Sample");
		for (String ph : template.getPlaceHolders()) {
			tmodel.addRow(new Object[] {ph, ""});
		}

		table = new JTable(tmodel){
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
		table.setRowSelectionAllowed(false);
		table.setColumnSelectionAllowed(false);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getTableHeader().setReorderingAllowed(false);
		table.setRowHeight(25);

		TableColumn sampleColumn = table.getColumnModel().getColumn(1);
		sampleBox = new JComboBox<String>();
		sampleColumn.setCellEditor(new DefaultCellEditor(sampleBox));

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				AutoCompleteSupport<String> support = AutoCompleteSupport.install(sampleBox, GlazedLists.eventListOf(availableSamples));
				support.setCorrectsCase(true);
				support.setFilterMode(TextMatcherEditor.CONTAINS);
				support.setTextMatchingStrategy(TextMatcherEditor.NORMALIZED_STRATEGY);
				support.setStrict(false);
				sampleBox.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						if (arg0.getActionCommand().equals("comboBoxEdited")){
							if (sampleBox.getSelectedIndex() < 0) sampleBox.setSelectedItem(null);
						}
					}
				});
			}
		});

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(table, BorderLayout.CENTER);
		panel.add(table.getTableHeader(), BorderLayout.NORTH);
		Border outterBorder = BorderFactory.createEmptyBorder(10,10,10,10);
		TitledBorder innerBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Sample placeholders");
		Border compoundBorder = BorderFactory.createCompoundBorder(outterBorder, innerBorder);
		panel.setBorder(compoundBorder);
		return panel;		
	}

	public void saveTemplate() {
		String folder = ProfileTree.showProfileDialog(this, null, Action.SAVE, UserData.FILTER, template.getAnalysis().toString(),  "Create "+UserData.FILTER.getName()+" folder to your profile", template.toString());
		AskUsersDialog ask = new AskUsersDialog(false);
		Tools.centerWindow(ask, false);
		ask.setVisible(true);
		if (!ask.getSelection().isEmpty()){
			saveTemplate(folder, ask.getSelection());
		}
	}

	public void saveTemplate(String folder, Set<User> users) {
		Map<String,String> placeholders = new TreeMap<>();
		for (int i=0 ; i < tmodel.getRowCount() ; i++) {
			if (tmodel.getValueAt(i, 1) == null || tmodel.getValueAt(i, 1).toString().length() == 0) {
				JOptionPane.showMessageDialog(UseTemplate.this, "You must select a sample for each placeholder", "Create filter using template",
						JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				return;
			}
			if (Filter.containsForbiddenCharacters(tmodel.getValueAt(i, 1).toString())){
				JOptionPane.showMessageDialog(this, "Samples cannot contains the following characters: "+Filter.getForbiddenCharacters(), "Create filter using template", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				return;				
			}
			String placeholder = "@" + tmodel.getValueAt(i, 0).toString() + "@";
			String sample  = tmodel.getValueAt(i, 1).toString().toUpperCase(); //save the uppercase because it caused problems with stats/user annotation fields (MySQL doesn't care, but my Stats class makes a difference between lower/upper case).
			placeholders.put(placeholder,sample);
		}
		try {			
			if (folder == null) return;
			if (Highlander.getLoggedUser().doesPersonalDataExists(UserData.FOLDER, template.getAnalysis().toString(), folder)){
				int yesno = JOptionPane.showConfirmDialog(new JFrame(), 
						"You already have a "+UserData.FOLDER.getName()+" named '"+folder.replace("~", " -> ")+"', do you want to overwrite it ?", 
						"Overwriting "+UserData.FOLDER.getName()+" in your profile", JOptionPane.YES_NO_OPTION , JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iDbSave,64));
				if (yesno == JOptionPane.NO_OPTION)	return;
			}else {
				Highlander.getLoggedUser().saveFolder(folder, UserData.FILTER, template.getAnalysis().toString());
			}
			//Create shared folder for all other receivers -- folders shared are automatically added (users cannot refuse them) but a message is displayed 
			for (User receiver : users) {
				if (!receiver.equals(Highlander.getLoggedUser())) {
					Highlander.getDB().update(Schema.HIGHLANDER, "INSERT INTO users_data (`username`, `type`, `analysis`, `key`, `value`) VALUES (" +
							"'"+Highlander.getDB().format(Schema.HIGHLANDER, receiver.getUsername())+"'," +
							"'"+Highlander.getDB().format(Schema.HIGHLANDER, UserData.FOLDER.toString())+"'," +
							"'"+Highlander.getDB().format(Schema.HIGHLANDER, template.getAnalysis().toString())+"'," +
							"'"+Highlander.getDB().format(Schema.HIGHLANDER, "SHARE|"+Highlander.getLoggedUser()+"|"+folder)+"'," +
							"'"+Highlander.getDB().format(Schema.HIGHLANDER, UserData.FILTER.toString())+"')");
				}
			}
			for (JCheckBox box : filterCheckBoxes) {
				if (box.isSelected()) {
					String filterSaveString = template.getFiltersSaveStrings().get(box.getText());
					for (String placeholder : placeholders.keySet()) {
						filterSaveString = filterSaveString.replace(placeholder, placeholders.get(placeholder));
					}
					for (User receiver : users) {
						Highlander.getDB().update(Schema.HIGHLANDER, "INSERT INTO users_data (`username`, `type`, `analysis`, `key`, `value`) VALUES (" +
								"'"+Highlander.getDB().format(Schema.HIGHLANDER, receiver.getUsername())+"'," +
								"'"+Highlander.getDB().format(Schema.HIGHLANDER, UserData.FILTER.toString())+"'," +
								"'"+Highlander.getDB().format(Schema.HIGHLANDER, template.getAnalysis().toString())+"'," +
								"'"+Highlander.getDB().format(Schema.HIGHLANDER, folder+"~"+box.getText())+"'," + 
								"'"+Highlander.getDB().format(Schema.HIGHLANDER, filterSaveString)+"')");
					}
				}
			}
		} catch (Exception ex) {
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Error", ex), "Create filter using template", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
		saved = true;
	}
}
