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

package be.uclouvain.ngs.highlander.administration.UI.database;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.administration.UI.ManagerPanel;
import be.uclouvain.ngs.highlander.administration.UI.ProjectManager;
import be.uclouvain.ngs.highlander.database.DBUtils;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Reference;

/**
* @author Raphael Helaers
*/

public class ReferencesPanel extends ManagerPanel {
	
	private String[] availableSchema;
	private String[] availableSchemaWithUnavailable;

	private DefaultListModel<Reference> listReferencesModel = new DefaultListModel<>();
	private JList<Reference> listReferences = new JList<>(listReferencesModel);
	private Map<Schema, String> schemas = new EnumMap<>(Schema.class);
	private List<JCheckBox> chromosomes = new ArrayList<>();
	private JTextArea txtAreaDescription = new JTextArea();
	private JScrollPane scrollSchemas = new JScrollPane();
	private JScrollPane scrollChromosomes = new JScrollPane();
	
	public ReferencesPanel(ProjectManager manager){
		super(manager);

		try {
			availableSchema = manager.listSchema(false);
			availableSchemaWithUnavailable = manager.listSchema(true);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.2);
		add(splitPane, BorderLayout.CENTER);

		JPanel panel_left = new JPanel(new BorderLayout(5,5));
		panel_left.setBorder(BorderFactory.createTitledBorder("Reference genomes"));
		splitPane.setLeftComponent(panel_left);
		
		JScrollPane scrollPane_left = new JScrollPane();
		panel_left.add(scrollPane_left, BorderLayout.CENTER);

		for (Reference ref : Reference.getAvailableReferences()) {
			listReferencesModel.addElement(ref);
		}
		listReferences.setFixedCellHeight(20); //To avoid a 'random bug' to sometimes (super) oversize one cell
		listReferences.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listReferences.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					fill(listReferences.getSelectedValue());
				}
			}
		});
		scrollPane_left.setViewportView(listReferences);
		
		JPanel panel_right = new JPanel(new BorderLayout());
		panel_right.setBorder(new EmptyBorder(10, 5, 0, 5));
		splitPane.setRightComponent(panel_right);
		
		JPanel panel_right_north = new JPanel(new BorderLayout());
		panel_right.add(panel_right_north, BorderLayout.NORTH);
		
		JPanel panel_description = new JPanel(new BorderLayout());
		panel_description.setBorder(new EmptyBorder(0, 0, 5, 5));
		JLabel lbl_description = new JLabel("Description");
		txtAreaDescription.setRows(3);
		panel_description.add(lbl_description, BorderLayout.NORTH);
		txtAreaDescription.setInputVerifier(new InputVerifier() {
			@Override
			public boolean verify(JComponent input) {
				Reference reference = listReferences.getSelectedValue();
				try {
					reference.updateDescription(txtAreaDescription.getText());
				}catch(Exception ex) {
					ProjectManager.toConsole(ex);
					txtAreaDescription.setForeground(Color.RED);
					return false;
				}
				txtAreaDescription.setForeground(Color.BLACK);
				return true;
			}
		});
		panel_description.add(txtAreaDescription, BorderLayout.CENTER);
		panel_right_north.add(panel_description, BorderLayout.NORTH);
		
		JPanel panel_center = new JPanel(new GridLayout(1, 2));
		panel_right.add(panel_center, BorderLayout.CENTER);
		
		JPanel panel_schemas = new JPanel(new BorderLayout());
		panel_schemas.setBorder(BorderFactory.createTitledBorder("Annotation schemas"));
		scrollSchemas.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		panel_schemas.add(scrollSchemas, BorderLayout.CENTER);
		panel_center.add(panel_schemas);

		JPanel panel_chromosomes = new JPanel(new BorderLayout());
		panel_chromosomes.setBorder(BorderFactory.createTitledBorder("Chromosomes to use in Highlander"));
		scrollChromosomes.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		panel_chromosomes.add(scrollChromosomes, BorderLayout.CENTER);
		panel_center.add(panel_chromosomes);
		
		JPanel panel_south = new JPanel();
		add(panel_south, BorderLayout.SOUTH);
		
		JButton createNewButton = new JButton("Create new reference", Resources.getScaledIcon(Resources.i3dPlus, 16));
		createNewButton.setToolTipText("You need at least a name, a description and the Ensembl schema");
		createNewButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						createReference();
					}
				}, "ReferencesPanel.createReference").start();

			}
		});
		panel_south.add(createNewButton);

		JButton renameButton = new JButton("Rename selected reference", Resources.getScaledIcon(Resources.iUpdater, 16));
		renameButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						renameReference(listReferences.getSelectedValue());
					}
				}, "ReferencesPanel.renameReference").start();

			}
		});
		panel_south.add(renameButton);

		JButton deleteButton = new JButton("Delete reference", Resources.getScaledIcon(Resources.iCross, 16));
		deleteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						deleteReference(listReferences.getSelectedValue());
					}
				}, "ReferencesPanel.deleteReference").start();

			}
		});
		panel_south.add(deleteButton);
		
		listReferences.setSelectedIndex(0);
	}

	public void fill() {
		listReferences.getSelectedValue();
	}
	
	private void fill(Reference reference) {
		scrollSchemas.setViewportView(null);
		scrollChromosomes.setViewportView(null);
		txtAreaDescription.setText("");
		if (reference != null) {
			chromosomes.clear();
			try {
				for (String chr : DBUtils.getAllChromosomes(reference)) {
					chromosomes.add(new JCheckBox(chr));
				}
			}catch(Exception ex) {
				ProjectManager.toConsole(ex);
			}
			JPanel panel_schemas_in = new JPanel(new GridBagLayout());
			schemas.clear();
			for (Schema schema : Schema.values()) {
				if (schema != Schema.HIGHLANDER && schema != Schema.HIGHLANDER) {
					if (reference.hasSchema(schema)) {
						try {
							schemas.put(schema, reference.getSchemaName(schema));
						}catch(Exception ex) {
							ProjectManager.toConsole(ex);
						}
					}else {
						schemas.put(schema, null);
						schemas.get(schema);
					}
				}
			}
			txtAreaDescription.setText(reference.getDescription());
			int row = 0;
			for (final Schema schema : schemas.keySet()) {
				JLabel lbl_schema = new JLabel(schema.toString());
				JComboBox<String> box_schema = new JComboBox<String>((schema == Schema.ENSEMBL) ? availableSchema : availableSchemaWithUnavailable);
				if (schemas.get(schema) == null) {
					box_schema.setSelectedItem("SCHEMA UNAVAILABLE");				
				}else {
					box_schema.setSelectedItem(schemas.get(schema));				
				}
				box_schema.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(ItemEvent e) {
						if (e.getStateChange() == ItemEvent.SELECTED){
							if (box_schema.getSelectedIndex() >= 0) {
								SwingUtilities.invokeLater(new Runnable() {
									@Override
									public void run() {
										String newDatabase = ((JComboBox<?>)e.getSource()).getSelectedItem().toString();
										if (!newDatabase.equals("SCHEMA UNAVAILABLE")) {
											try {
												reference.updateSchema(schema, newDatabase);
											}catch(Exception ex) {
												ProjectManager.toConsole(ex);
											}
										}else {
											try {
												reference.deleteSchema(schema);
											}catch(Exception ex) {
												ProjectManager.toConsole(ex);
											}
										}
									}
								});
							}
						}
					}
				});
				panel_schemas_in.add(lbl_schema, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
				panel_schemas_in.add(box_schema, new GridBagConstraints(1, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
				panel_schemas_in.add(new JPanel(), new GridBagConstraints(2, row, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0), 0, 0));
				row++;
			}
			panel_schemas_in.add(new JPanel(), new GridBagConstraints(0, row, 3, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
			scrollSchemas.setViewportView(panel_schemas_in);

			JPanel panel_chr_in = new JPanel(new GridBagLayout());
			row = 0;
			for (JCheckBox box : chromosomes) {
				if (reference.getChromosomes().contains(box.getText())) box.setSelected(true);
				box.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(ItemEvent e) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								try {
									List<String> newChromosomes = new ArrayList<>();
									for (JCheckBox box : chromosomes) {
										if (box.isSelected()) newChromosomes.add(box.getText());
									}
									reference.updateChromosomes(newChromosomes);
								}catch(Exception ex) {
									ProjectManager.toConsole(ex);
								}
							}
						});
					}
				});
				panel_chr_in.add(box, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));				row++;
				row++;
			}
			panel_chr_in.add(new JPanel(), new GridBagConstraints(0, row, 3, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
			scrollChromosomes.setViewportView(panel_chr_in);
		}
	}
	

	public void createReference(){
		Object resu = JOptionPane.showInputDialog(this, "Reference name", "Creating reference", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.i3dPlus, 64), null, null);
		if (resu != null){
			String name = resu.toString();
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(true);
					waitingPanel.start();
				}
			});
			try{
				int count = 0;
				try (Results res = DB.select(Schema.HIGHLANDER, "SELECT COUNT(*) FROM `references` WHERE `reference` = '"+name+"'")) {
					if (res.next()){
						count = res.getInt(1);
					}
				}
				if (count > 0){
					JOptionPane.showMessageDialog(this, "Reference name already exists'", "Error", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
				}else if (name.length() > 255){
					JOptionPane.showMessageDialog(this, "Reference is limited to 255 characters'", "Error", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
				}else{
					Object description = JOptionPane.showInputDialog(this, "Enter a full description of this reference genome", "Creating reference", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.i3dPlus, 64), null, null);
					if (description != null){
						Object ensembl = JOptionPane.showInputDialog(this, "Select the Ensembl database to used with this reference", "Creating reference", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.i3dPlus, 64), availableSchema, null);
						if (ensembl != null){
							ProjectManager.toConsole("-----------------------------------------------------");
							ProjectManager.toConsole("Creating reference " + name);
							final Reference reference = new Reference(name, new ArrayList<>(), description.toString(), Schema.ENSEMBL, ensembl.toString());
							reference.insert();
							ProjectManager.toConsole("-----------------------------------------------------");
							ProjectManager.toConsole("Fetching chromosomes in " + ensembl.toString());
							List<String> chrs = new ArrayList<>();
							try {
								for (String chr : DBUtils.getAllChromosomes(reference)) {
									Pattern pat = Pattern.compile("[^0-9]");
									if(!pat.matcher(chr).find() || chr.equalsIgnoreCase("X") || chr.equalsIgnoreCase("Y")) {
										chrs.add(chr);
									}
								}
								reference.updateChromosomes(chrs);
							}catch(Exception ex) {
								ProjectManager.toConsole(ex);
							}
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									listReferencesModel.addElement(reference);
									listReferences.setSelectedValue(reference, true);
									manager.refreshPanels();
								}
							});
						}
					}
				}
			}catch(Exception ex){
				ProjectManager.toConsole(ex);
			}
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(false);
					waitingPanel.stop();
				}
			});
		}		
	}

	public void renameReference(Reference reference){
		Object resu = JOptionPane.showInputDialog(this, "Reference name", "Renaming reference", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iUpdater, 64), null, reference);
		if (resu != null){
			String name = resu.toString();
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(true);
					waitingPanel.start();
				}
			});
			try{
				int count = 0;
				try (Results res = DB.select(Schema.HIGHLANDER, "SELECT COUNT(*) FROM `references` WHERE `reference` = '"+name+"'")) {
					if (res.next()){
						count = res.getInt(1);
					}
				}
				if (count > 0){
					JOptionPane.showMessageDialog(this, "Reference name already exists", "Error", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
				}else if (name.length() > 255){
					JOptionPane.showMessageDialog(this, "Reference is limited to 255 characters'", "Error", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
				}else{
					ProjectManager.toConsole("-----------------------------------------------------");
					ProjectManager.toConsole("Renaming reference " + reference + " to "  + name);
					reference.updateName(name);
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							manager.refreshPanels();
						}
					});
				}
			}catch(Exception ex){
				ProjectManager.toConsole(ex);
			}
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(false);
					waitingPanel.stop();
				}
			});
		}
	}

	public void deleteReference(Reference reference){
		try{
			int count = 0;
			try (Results res = DB.select(Schema.HIGHLANDER, "SELECT COUNT(*) FROM `analyses` WHERE `reference` = '"+reference.getName()+"'")) {
				if (res.next()){
					count = res.getInt(1);
				}
			}
			if (count == 0){
				int res = JOptionPane.showConfirmDialog(new JFrame(), "Are you SURE you want to delete reference '"+reference+"' ?", "Delete reference", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				if (res == JOptionPane.YES_OPTION){
					ProjectManager.toConsole("-----------------------------------------------------");
					ProjectManager.toConsole("Deleting reference " + reference);
					reference.delete();
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							listReferences.clearSelection();
							listReferences.setSelectedIndex(0);
							listReferencesModel.removeElement(reference);
							manager.refreshPanels();
						}
					});
				}
			}else{
				JOptionPane.showMessageDialog(new JFrame(), count + " analyses are still linked to this reference.\nPlease first delete those analyses or link them to another reference.", "Delete reference", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}
		}catch(Exception ex){
			ProjectManager.toConsole(ex);
		}
	}

}
