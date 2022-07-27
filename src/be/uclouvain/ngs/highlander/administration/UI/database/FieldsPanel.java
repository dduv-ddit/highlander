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
import java.awt.Component;
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
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
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
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Resources.Palette;
import be.uclouvain.ngs.highlander.UI.misc.WrapLayout;
import be.uclouvain.ngs.highlander.administration.UI.ManagerPanel;
import be.uclouvain.ngs.highlander.administration.UI.ProjectManager;
import be.uclouvain.ngs.highlander.database.Category;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.SqlGenerator;
import be.uclouvain.ngs.highlander.database.Field.Annotation;
import be.uclouvain.ngs.highlander.database.Field.JSon;
import be.uclouvain.ngs.highlander.database.Field.Tag;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;

/**
* @author Raphael Helaers
*/

public class FieldsPanel extends ManagerPanel {
	
	private DefaultListModel<Category> listCategoriesModel = new DefaultListModel<>();
	private JList<Category> listCategories = new JList<>(listCategoriesModel);
	private DefaultListModel<Field> listFieldsModel = new DefaultListModel<>();
	private JList<Field> listFields = new JList<>(listFieldsModel);
	private JScrollPane scrollFields = new JScrollPane();
	private JScrollPane scrollData = new JScrollPane();
	private JButton deleteFieldButton;
	
	public FieldsPanel(ProjectManager manager){
		super(manager);
		
		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.4);
		add(splitPane, BorderLayout.CENTER);

		splitPane.setLeftComponent(getLeftPanel());
		splitPane.setRightComponent(getRightPanel());
		
		JPanel southPanel = new JPanel(new WrapLayout(WrapLayout.CENTER));
		add(southPanel, BorderLayout.SOUTH);

		JButton createNewCategoryButton = new JButton("Create category", Resources.getScaledIcon(Resources.i3dPlus, 16));
		createNewCategoryButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						createCategory();
					}
				}, "FieldsPanel.createCategory").start();

			}
		});
		southPanel.add(createNewCategoryButton);

		JButton deleteCategoryButton = new JButton("Delete category", Resources.getScaledIcon(Resources.i3dMinus, 16));
		deleteCategoryButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						deleteCategory(listCategories.getSelectedValue());
					}
				}, "FieldsPanel.deleteCategory").start();

			}
		});
		southPanel.add(deleteCategoryButton);
		
		JButton renameCategoryButton = new JButton("Rename category", Resources.getScaledIcon(Resources.iUpdater, 16));
		renameCategoryButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						renameCategory(listCategories.getSelectedValue());
					}
				}, "FieldsPanel.renameCategory").start();
				
			}
		});
		southPanel.add(renameCategoryButton);
		
		JButton enableCategoryButton = new JButton("Set category details box", Resources.getScaledIcon(Resources.iUpdater, 16));
		enableCategoryButton.setToolTipText("<html>Set wether or not this category has a generic detail box in Highlander.<br>"
					+ "The state of the category is shown in the list with a check/cross icon if the category has/has not a generic detail box set.<br>"
					+ "A generic detail box will simply show all fields of the category with their value, in a 2 column table.<br>"
					+ "Category 'User annotations' has specific detail boxes (where user can modify those fields), so an additional generic detail box is not necessary.<br>"
					+ "Category 'Miscellaneous' doesn't have a generic detail box by default, because fields it contains are considered of no interest for users.<br>"
					+ "</html>");
		enableCategoryButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						enableCategoryDetailBox(listCategories.getSelectedValue());
					}
				}, "FieldsPanel.enableCategoryDetailBox").start();
				
			}
		});
		southPanel.add(enableCategoryButton);
		
		JButton colorCategoryButton = new JButton("Set category color", Resources.getScaledIcon(Resources.iHighlighting, 16));
		colorCategoryButton.setToolTipText("Color is used e.g. to visually regroup detail boxes with the same theme");
		colorCategoryButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						colorCategory(listCategories.getSelectedValue());
					}
				}, "FieldsPanel.colorCategoryButton").start();
				
			}
		});
		southPanel.add(colorCategoryButton);
		
		JButton createNewFieldButton = new JButton("Create field", Resources.getScaledIcon(Resources.i3dPlus, 16));
		createNewFieldButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						createCustomField();
					}
				}, "FieldsPanel.createCustomField").start();
				
			}
		});
		southPanel.add(createNewFieldButton);
		
		deleteFieldButton = new JButton("Delete field", Resources.getScaledIcon(Resources.i3dMinus, 16));
		deleteFieldButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						deleteCustomField(listFields.getSelectedValue());
					}
				}, "FieldsPanel.deleteCustomField").start();
				
			}
		});
		southPanel.add(deleteFieldButton);

		listCategories.setSelectedIndex(0);
	}
	
	private JPanel getLeftPanel() {
		JPanel panel = new JPanel(new BorderLayout(5,5));
		
		JPanel panel_left = new JPanel(new BorderLayout(5,5));
		panel_left.setBorder(BorderFactory.createTitledBorder("Categories"));
		JScrollPane scrollPane_left = new JScrollPane();
		panel_left.add(scrollPane_left, BorderLayout.CENTER);

		for (Category category : Category.getAvailableCategories()) {
			listCategoriesModel.addElement(category);
		}
		listCategories.setFixedCellHeight(20); //To avoid a 'random bug' to sometimes (super) oversize one cell
		listCategories.setCellRenderer(new CategoryCellRenderer());
		listCategories.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listCategories.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					fill(listCategories.getSelectedValue());
				}
			}
		});
		scrollPane_left.setViewportView(listCategories);
		
		JPanel panel_category_order = new JPanel(new GridBagLayout());
		panel_left.add(panel_category_order, BorderLayout.EAST);

		JButton button_up = new JButton(Resources.getScaledIcon(Resources.iArrowDoubleUp, 24));
		button_up.setToolTipText("Put selected category before for fields ordering (and details boxes) in Highlander");
		button_up.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				reorderCategories(listCategories.getSelectedIndex(), true);
			}
		});
		GridBagConstraints gbc_button = new GridBagConstraints();
		gbc_button.insets = new Insets(0, 0, 5, 0);
		gbc_button.gridx = 0;
		gbc_button.gridy = 0;
		panel_category_order.add(button_up, gbc_button);

		JButton button_down = new JButton(Resources.getScaledIcon(Resources.iArrowDoubleDown, 24));
		button_down.setToolTipText("Put selected category after for fields ordering (and details boxes) in Highlander");
		button_down.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				reorderCategories(listCategories.getSelectedIndex(), false);
			}
		});
		GridBagConstraints gbc_button_1 = new GridBagConstraints();
		gbc_button_1.gridx = 0;
		gbc_button_1.gridy = 1;
		panel_category_order.add(button_down, gbc_button_1);

		panel_category_order.add(new JPanel(), new GridBagConstraints(0, 2, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(0,0,0,0), 0, 0));

		panel.add(panel_left, BorderLayout.WEST);
		
		JPanel panel_right = new JPanel(new BorderLayout(5,5));
		panel_right.setBorder(BorderFactory.createTitledBorder("Fields"));
		panel_right.add(scrollFields, BorderLayout.CENTER);
		
		listFields.setFixedCellHeight(20); //To avoid a 'random bug' to sometimes (super) oversize one cell
		listFields.setCellRenderer(new FieldCellRenderer());
		listFields.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listFields.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					fill(listFields.getSelectedValue());
				}
			}
		});

		JPanel panel_field_order = new JPanel(new GridBagLayout());
		panel_right.add(panel_field_order, BorderLayout.EAST);
		
		JButton button_up_field = new JButton(Resources.getScaledIcon(Resources.iArrowDoubleUp, 24));
		button_up_field.setToolTipText("Put selected field before for fields ordering in Highlander");
		button_up_field.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				reorderFields(listFields.getSelectedIndex(), true);
			}
		});
		GridBagConstraints gbc_button_2 = new GridBagConstraints();
		gbc_button_2.insets = new Insets(0, 0, 5, 0);
		gbc_button_2.gridx = 0;
		gbc_button_2.gridy = 0;
		panel_field_order.add(button_up_field, gbc_button_2);
		
		JButton button_down_field = new JButton(Resources.getScaledIcon(Resources.iArrowDoubleDown, 24));
		button_down_field.setToolTipText("Put selected field after for fields ordering in Highlander");
		button_down_field.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				reorderFields(listFields.getSelectedIndex(), false);
			}
		});
		GridBagConstraints gbc_button_4 = new GridBagConstraints();
		gbc_button_4.gridx = 0;
		gbc_button_4.gridy = 1;
		panel_field_order.add(button_down_field, gbc_button_4);
		
		panel_field_order.add(new JPanel(), new GridBagConstraints(0, 2, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(0,0,0,0), 0, 0));
		
		panel.add(panel_right, BorderLayout.CENTER);
		
		return panel;
	}
	
	private JPanel getRightPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(new EmptyBorder(10, 5, 0, 5));
		panel.add(scrollData, BorderLayout.CENTER);
		return panel;
	}
			
	public void fill(){
		fill(listCategories.getSelectedValue());
	}

	private void fill(Category category){
		scrollData.setViewportView(null);
		if (category != null) {
			listFieldsModel.removeAllElements();
			try (Results res = DB.select(Schema.HIGHLANDER, 
					"SELECT `fields`.*, group_concat(`tag`) as tags, group_concat(`analysis`) as analyses "
							+ "FROM `fields` "
							+ "JOIN `field_categories` USING(`category`)  "
							+ "LEFT JOIN `fields_tags` USING(`field`) "
							+ "LEFT JOIN `fields_analyses` USING(`field`) "
							+ "WHERE `category` = '"+category+"' "
							+ "GROUP BY `field`, `table`, `sql_datatype`, `json`, `description`, `annotation_code`, `annotation_header`, `source`, `ordering`, `category`, `size`, `alignment` "
							+ "ORDER BY `field_categories`.`ordering`, `fields`.`ordering` ASC"
					)) {
				while (res.next()){
					listFieldsModel.addElement(Field.getField(res));
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
			scrollFields.setViewportView(listFields);
			listFields.setSelectedIndex(0);
		}
	}
	
	private void fill(Field field){
		scrollData.setViewportView(null);
		if (field != null) {
			if (field.getTableSuffix().equals("_custom_annotations") && !field.equals(Field.variant_custom_id)) {
				deleteFieldButton.setEnabled(true);
			}else {
				deleteFieldButton.setEnabled(false);
			}
			
			JPanel panel = new JPanel(new GridBagLayout());

			int row = 0;

			String tooltipFieldName = "<html>The column name in Highlander.<br>"
					+ "<ul>"
					+ "<li>Field name can only contain alphanumeric caracters and '_'.</li>"
					+ "<li>Field cannot start by a number.</li>"
					+ "<li>Field name must be unique in the whole database.</li>"
					+ "</ul>"
					+ "</html>";
			JLabel labelFieldName = new JLabel("Column name");
			labelFieldName.setToolTipText(tooltipFieldName);
			panel.add(labelFieldName, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			JTextField txtFieldName = new JTextField(field.getName());
			txtFieldName.setToolTipText(tooltipFieldName);
			panel.add(txtFieldName, new GridBagConstraints(1, row, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			if (field.getTableSuffix().equals("_custom_annotations") && !field.equals(Field.variant_custom_id)) {
				txtFieldName.setInputVerifier(new InputVerifier() {
					@Override
					public boolean verify(JComponent input) {
						JTextField inputTxtField = (JTextField)input;
						Field field = listFields.getSelectedValue();
						try {
							String fieldStr = inputTxtField.getText();
							if (!field.getName().equals(fieldStr)) {
								fieldStr = fieldStr.trim().replace(' ', '_').toLowerCase();
								Pattern pat = Pattern.compile("(^[0-9])|([^a-zA-Z0-9_])");
								if (pat.matcher(fieldStr).find()){
									ProjectManager.toConsole("Field name can only contain alphanumeric caracters and '_', and cannot start by a number");
									inputTxtField.setForeground(Color.RED);
									return false;
								}else {
									field.updateName(fieldStr);
								}
							}
						}catch(Exception ex) {
							ProjectManager.toConsole(ex);
							inputTxtField.setForeground(Color.RED);
							return false;
						}
						inputTxtField.setForeground(Color.BLACK);
						return true;
					}
				});
			}else {
				txtFieldName.setEnabled(false);
			}
			row++;
			
			String tooltipDescription = "<html>Field full description, used <i>e.g.</i> in Highlander tooltips.<br>"
					+ "</html>";
			JLabel labelDescription = new JLabel("Description");
			labelDescription.setToolTipText(tooltipDescription);
			panel.add(labelDescription, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			JTextArea txtDescription = new JTextArea(field.getDescription());
			txtDescription.setWrapStyleWord(true);
			txtDescription.setRows(3);
			txtDescription.setToolTipText(tooltipDescription);
			panel.add(txtDescription, new GridBagConstraints(1, row, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			txtDescription.setInputVerifier(new InputVerifier() {
				@Override
				public boolean verify(JComponent input) {
					JTextArea inputTxtField = (JTextArea)input;
					Field field = listFields.getSelectedValue();
					if (!field.getDescription().equals(inputTxtField.getText())) {
						field.setDescription(inputTxtField.getText());
						try {
							field.update();
						}catch(Exception ex) {
							ProjectManager.toConsole(ex);
							inputTxtField.setForeground(Color.RED);
							return false;
						}
					}
					inputTxtField.setForeground(Color.BLACK);
					return true;
				}
			});
			row++;
			
			String tooltipSource = "<html>Source of annotation, displayed in Highlander.<br>"
					+ "Use software name and version, e.g. 'GATK 4.1' or 'SnpEff 4.3'.<br>"
					+ "To change all fields with the same source at once, use the update button next to this list.<br>"
					+ "<i>e.g.</i> If you update snpEff from 4.2 to 4.3, you don't have to change source for each field manually !<br>"
					+ "</html>";
			JLabel labelSource = new JLabel("Source");
			labelSource.setToolTipText(tooltipSource);
			panel.add(labelSource, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			JComboBox<String> boxSource = new JComboBox<>(manager.listAnnotationSources());
			boxSource.setToolTipText(tooltipSource);
			panel.add(boxSource, new GridBagConstraints(1, row, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			boxSource.setSelectedItem(field.getSource());				
			boxSource.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					JComboBox<String> source = boxSource;
					if (e.getStateChange() == ItemEvent.SELECTED){
						if (source.getSelectedIndex() >= 0) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									String target = source.getSelectedItem().toString();
									if (target.equals("Add new source")){
										Object res = JOptionPane.showInputDialog(manager,  "Set a name for the new source", "Annotation source",
												JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), null, null);
										if (res == null) return;
										target = res.toString().trim();
									}
									if (!field.getSource().equals(target)) {
										field.setSource(target);
										try {
											field.update();
										}catch(Exception ex) {
											ProjectManager.toConsole(ex);
										}
										fill(field);
									}
								}
							});
						}
					}
				}
			});
			JButton buttonSource = new JButton(Resources.getScaledIcon(Resources.iUpdater,18));
			buttonSource.setToolTipText("Change source for ALL fields with same source than this field");
			panel.add(buttonSource, new GridBagConstraints(2, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			buttonSource.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Object res = JOptionPane.showInputDialog(manager,  "Change annotation source for all fields with '"+field.getSource()+"'", "Annotation source",
							JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), manager.listAnnotationSources(), field.getSource());
					if (res != null){
						String target = res.toString();
						if (target.equals("Add new source")){
							res = JOptionPane.showInputDialog(manager,  "Set a name for the new source", "Annotation source",
									JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), null, null);
							if (res == null) return;
							target = res.toString().trim();
						}
						try {
							DB.update(Schema.HIGHLANDER, "UPDATE `fields` SET `source` = '"+target+"' WHERE `source` = '"+field.getSource()+"'");
						}catch(Exception ex) {
							ProjectManager.toConsole(ex);
						}
						fill();
					}
				}
			});
			row++;

			String tooltipCategory = "<html>Highlander category, used in filtering field lists and displaying details boxes.<br>"
					+ "</html>";
			JLabel labelCategory = new JLabel("Category");
			labelCategory.setToolTipText(tooltipCategory);
			panel.add(labelCategory, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			JComboBox<Category> boxCategory = new JComboBox<Category>(Category.getAvailableCategories().toArray(new Category[0]));
			boxCategory.setToolTipText(tooltipCategory);
			panel.add(boxCategory, new GridBagConstraints(1, row, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			boxCategory.setSelectedItem(field.getCategory());				
			boxCategory.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					JComboBox<Category> source = boxCategory;
					if (e.getStateChange() == ItemEvent.SELECTED){
						if (source.getSelectedIndex() >= 0) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									Category target = (Category)source.getSelectedItem();
									if (!target.equals(field.getCategory())) {
										field.setCategory(target);
										try {
											field.update();
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
			row++;

			String tooltipSQLType = "<html>SQL datatype.<br>"
					+ "You can use variation of the following datatypes for custom fields:"
					+ "<ul>"
					+ "<li>VARCHAR or VARCHAR(x)</li>"
					+ "<li>TEXT of any size like LONGTEXT</li>"
					+ "<li>INT or INT(x), of any size like SMALLINT or BIGINT, with or without UNSIGNED</li>"
					+ "<li>DOUBLE</li>"
					+ "<li>BOOLEAN</li>"
					+ "<li>DATETIME</li>"
					+ "</ul>"
					+ "Don't use ENUM for custom fields.<br>"					
					+ "Other datatypes are NOT supported for custom fields."					
					+ "</html>";
			JLabel labelSQLType = new JLabel("SQL datatype");
			labelSQLType.setToolTipText(tooltipSQLType);
			panel.add(labelSQLType, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			JTextField txtSQLType = new JTextField(field.getSqlDatatype());
			txtSQLType.setToolTipText(tooltipSQLType);
			panel.add(txtSQLType, new GridBagConstraints(1, row, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			if (field.getTableSuffix().equals("_custom_annotations") && !field.equals(Field.variant_custom_id)) {
				txtSQLType.setInputVerifier(new InputVerifier() {
					@Override
					public boolean verify(JComponent input) {
						JTextField inputTxtField = (JTextField)input;
						Field field = listFields.getSelectedValue();
						if (!field.getSqlDatatype().equals(inputTxtField.getText())) {
							int ans = JOptionPane.showConfirmDialog(new JFrame(), "As it is a custom field, the database tables of analyses where it exists will be altered accordingly.\nAre you sure you want to proceed with '"+inputTxtField.getText()+"' ?", "Modify SQL datatype for " + field.getName(), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iDbStatus,64));
							if (ans == JOptionPane.YES_OPTION){
								try {
									field.update();
								}catch(Exception ex) {
									ProjectManager.toConsole(ex);
									inputTxtField.setForeground(Color.RED);
									return false;
								}
								new Thread(new Runnable() {
									@Override
									public void run() {
										SwingUtilities.invokeLater(new Runnable() {
											public void run() {
												waitingPanel.setVisible(true);
												waitingPanel.start();
											}
										});
										field.setSqlDatatype(inputTxtField.getText());
										try {
											for (final Analysis analysis : manager.getAvailableAnalysesAsArray()) {
												try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "DESCRIBE "+ field.getTable(analysis))) {
													while (res.next()){
														String column = Highlander.getDB().getDescribeColumnName(Schema.HIGHLANDER, res);
														if (column.equals(field.getName())) {
															List<Field> list = new ArrayList<>();
															list.add(field);
															for (String query : SqlGenerator.split(SqlGenerator.modifyColumnsFromCustomAnnotations(analysis, list))) {
																try {
																	ProjectManager.toConsole(query);
																	Highlander.getDB().update(Schema.HIGHLANDER, query);
																}catch(Exception ex) {
																	ProjectManager.toConsole(ex);
																}
															}
														}
													}
												}
											}
										}catch(Exception ex) {
											ProjectManager.toConsole(ex);
										}
										SwingUtilities.invokeLater(new Runnable() {
											public void run() {
												waitingPanel.setVisible(false);
												waitingPanel.stop();
											}
										});
									}
								}).start();
							}
						}
						inputTxtField.setForeground(Color.BLACK);
						return true;
					}
				});
			}else {
				txtSQLType.setEnabled(false);
			}
			row++;
			
			String tooltipJson = "<html>The JSON path used when exporting to that format.<br>"
					+ "</html>";
			JLabel labelJson = new JLabel("JSON path");
			labelJson.setToolTipText(tooltipJson);
			panel.add(labelJson, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			JComboBox<JSon> boxJson = new JComboBox<>(JSon.values());
			boxJson.setToolTipText(tooltipJson);
			panel.add(boxJson, new GridBagConstraints(1, row, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			boxJson.setSelectedItem(field.getJSonPath());				
			boxJson.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					JComboBox<JSon> source = boxJson;
					if (e.getStateChange() == ItemEvent.SELECTED){
						if (source.getSelectedIndex() >= 0) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									JSon target = (JSon)source.getSelectedItem();
									if (!target.equals(field.getJSonPath())) {
										field.setJsonPath(target);
										try {
											field.update();
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
			row++;

			String tooltipAnnotationCode = "<html>The annotation pattern Highlander has to follow for this field.<br>"
					+ "For custom fields, the easiest way is to choose VCF and add the annotation in the VCF before importation in Highlander.<br>"
					+ "Here is a description of other possibilities:<br>"
					+ "<ul>";
			for (Annotation a : Annotation.values()) {
				tooltipAnnotationCode += "<li>" + a + ": " + a.getDescription() + "</li>";
			}
			tooltipAnnotationCode += "</ul>"
					+ "</html>";
			JLabel labelAnnotationCode = new JLabel("Annotation pattern");
			labelAnnotationCode.setToolTipText(tooltipAnnotationCode);
			panel.add(labelAnnotationCode, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			JComboBox<Annotation> boxAnnotationCode = new JComboBox<>(Annotation.values());
			boxAnnotationCode.setToolTipText(tooltipAnnotationCode);
			panel.add(boxAnnotationCode, new GridBagConstraints(1, row, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			boxAnnotationCode.setSelectedItem(field.getAnnotationCode());				
			boxAnnotationCode.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					JComboBox<Annotation> source = boxAnnotationCode;
					if (e.getStateChange() == ItemEvent.SELECTED){
						if (source.getSelectedIndex() >= 0) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									Annotation target = (Annotation)source.getSelectedItem();
									if (!target.equals(field.getAnnotationCode())) {
										field.setAnnotation(target);
										try {
											field.update();
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
			row++;
			
			String tooltipAnnotationHeader = "<html>In which header (or database field) the annotation can be found.<br>"
					+ "For annotations coming from the VCF:"
					+ "<ul>"
					+ "<li>Set the VCF section and the VCF field separated by a '|'.</li>"
					+ "<li>Add <code>&A</code> if there is one annotation per alternative allele.</li>"
					+ "<li>Add <code>&R</code> if there is one annotation for the reference plus one annotation per alternative allele, and that this field needs the reference allele (so first annotation in the serie).</li>"
					+ "<li>Add <code>&RA</code> if there is one annotation for the reference plus one annotation per alternative allele, and that this field needs the alternative allele (so Nth annotation in the serie, depending on the number of alternative alleles).</li>"
					+ "<li>Example 1: <code>INFO|QD</code> will search this field value in the QD field of the INFO section of the VCF.</li>"
					+ "<li>Example 2: <code>FORMAT|PL&A</code> will search this field value in the PL field of the FORMAT section of the VCF. The &A indicates that this field has as much values (separated by coma) as alternative alleles.</li>"
					+ "</ul>"
					+ "For annotations coming from a database:"			
					+ "<ul>"
					+ "<li>Set as <code>table|column</code>.</li>"
					+ "<li>Add <code>&[separator][column]</code> when the column can contains multiple annotations depending on another column.</li>"
					+ "<li>Example 1: <code>chromosome_[chr]|COSMIC_ID</code> will search this field value in the COSMIC_ID column of the chromosome_1 table for a variant on chromosome 1.</li>"
					+ "<li>Example 2: <code>chromosome_[chr]|FATHMM_score&;Ensembl_proteinid</code> will search this field value in the FATHMM_score, and if this variant can affect multiple transcript, select the score corresponding to the canonical transcript using column Ensembl_proteinid.</li>"
					+ "</ul>"
					+ "</html>";
			JLabel labelAnnotationHeader = new JLabel("Annotation header");
			labelAnnotationHeader.setToolTipText(tooltipAnnotationHeader);
			panel.add(labelAnnotationHeader, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			JTextField txtAnnotationHeader = new JTextField(field.getAnnotationHeader());
			txtAnnotationHeader.setToolTipText(tooltipAnnotationHeader);
			panel.add(txtAnnotationHeader, new GridBagConstraints(1, row, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			txtAnnotationHeader.setInputVerifier(new InputVerifier() {
				@Override
				public boolean verify(JComponent input) {
					JTextField inputTxtField = (JTextField)input;
					Field field = listFields.getSelectedValue();
					if (!field.getAnnotationHeader().equals(inputTxtField.getText())) {
						field.setHeader(inputTxtField.getText());
						try {
							field.update();
						}catch(Exception ex) {
							ProjectManager.toConsole(ex);
							inputTxtField.setForeground(Color.RED);
							return false;
						}
					}
					inputTxtField.setForeground(Color.BLACK);
					return true;
				}
			});
			row++;
			
			String tooltipAlignment = "<html>The alignment of the field in Highlander tables.<br>"
					+ "</html>";
			JLabel labelAlignment = new JLabel("Alignment");
			labelAlignment.setToolTipText(tooltipAlignment);
			panel.add(labelAlignment, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			JComboBox<String> boxAlignment = new JComboBox<>(Field.getDefaultAlignments().keySet().toArray(new String[0]));
			boxAlignment.setToolTipText(tooltipAlignment);
			panel.add(boxAlignment, new GridBagConstraints(1, row, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			String alignmentString = "LEFT";
			for (String s : Field.getDefaultAlignments().keySet()) {
				if (Field.getDefaultAlignments().get(s) == field.getAlignment()) {
					alignmentString = s;
				}
			}
			boxAlignment.setSelectedItem(alignmentString);				
			boxAlignment.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					JComboBox<String> source = boxAlignment;
					if (e.getStateChange() == ItemEvent.SELECTED){
						if (source.getSelectedIndex() >= 0) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									String target = source.getSelectedItem().toString();
									int align = Field.getDefaultAlignments().get(target);
									if (align != field.getAlignment()) {
										field.setAlignment(align);
										try {
											field.update();
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
			row++;

			String tooltipSize = "<html>The default width of the field in Highlander tables.<br>"
					+ "</html>";
			JLabel labelSize = new JLabel("Width");
			labelSize.setToolTipText(tooltipSize);
			panel.add(labelSize, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			JComboBox<String> boxSize = new JComboBox<>(Field.getDefaultWidths().keySet().toArray(new String[0]));
			boxSize.setToolTipText(tooltipSize);
			panel.add(boxSize, new GridBagConstraints(1, row, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			String widthString = "medium";
			for (String s : Field.getDefaultWidths().keySet()) {
				if (Field.getDefaultWidths().get(s) == field.getSize()) {
					widthString = s;
				}
			}
			boxSize.setSelectedItem(widthString);				
			boxSize.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					JComboBox<String> source = boxSize;
					if (e.getStateChange() == ItemEvent.SELECTED){
						if (source.getSelectedIndex() >= 0) {
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									String target = source.getSelectedItem().toString();
									int width = Field.getDefaultWidths().get(target);
									if (width != field.getSize()) {
										field.setSize(width);
										try {
											field.update();
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
			row++;
			
			JPanel lowerPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 5));
			
			String tooltipAnalysesExist = "<html>Analyses in which this field exists.<br>"
					+ "If you uncheck an analysis, the field will be <b>deleted</b> in the analysis, with <b>all variant data</b> (this data <b>cannot be recovered</b>)."
					+ "</html>";
			JPanel panel_analyses_exist = new JPanel(new GridBagLayout());
			panel_analyses_exist.setBorder(BorderFactory.createTitledBorder("Field exists in"));
			int x = 0;
			for (final Analysis analysis : manager.getAvailableAnalysesAsArray()) {
				JCheckBox box = new JCheckBox(analysis.toString());
				List<String> columns = new ArrayList<String>();
				try {
					try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "DESCRIBE "+ field.getTable(analysis))) {
						while (res.next()){
							columns.add(Highlander.getDB().getDescribeColumnName(Schema.HIGHLANDER, res));
						}
					}
					box.setSelected(columns.contains(field.getName()));
				}catch(Exception ex) {
					ProjectManager.toConsole(ex);
					box.setEnabled(false);
				}
				box.setToolTipText(tooltipAnalysesExist);
				if (!field.getTableSuffix().equals("_custom_annotations")) {
					box.setEnabled(false);
				}
				box.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(ItemEvent e) {
						List<Field> fields = new ArrayList<>();
						fields.add(field);
						if (e.getStateChange() == ItemEvent.SELECTED) {
							for (String query : SqlGenerator.split(SqlGenerator.addColumnsToCustomAnnotations(analysis, fields))) {
								try {
									ProjectManager.toConsole(query);
									Highlander.getDB().update(Schema.HIGHLANDER, query);
								}catch(Exception ex) {
									ProjectManager.toConsole(ex);
								}
							}
							field.addAnalysis(analysis);
						}else {
							for (String query : SqlGenerator.split(SqlGenerator.removeColumnsFromCustomAnnotations(analysis, fields))) {
								try {
									ProjectManager.toConsole(query);
									Highlander.getDB().update(Schema.HIGHLANDER, query);
								}catch(Exception ex) {
									ProjectManager.toConsole(ex);
								}
							}
							field.removeAnalysis(analysis);
							fill(field);
						}
						try {
							field.update();
						}catch(Exception ex) {
							ProjectManager.toConsole(ex);
						}
					}
				});
				panel_analyses_exist.add(box, new GridBagConstraints(0, x, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
				x++;
			}
			panel_analyses_exist.setToolTipText(tooltipAnalysesExist);
			lowerPanel.add(panel_analyses_exist);

			String tooltipAnalysesShow = "<html>Analyses in which this field is visible in Highlander client.<br>"
					+ "If you uncheck an analysis, the field and variant data linked to it will still exist in the database,<br>"
					+ "but it will not be shown in Highlander client.<br>"
					+ "It can be useful for non-custom fields you don't want to show in specific analyses (e.g. because annotation doesn't exist).<br>"
					+ "Example: an analysis with mouse data doesn't have data for prediction software, so those fields can be masked.<br>"
					+ "</html>";
			JPanel panel_analyses_show = new JPanel(new GridBagLayout());
			panel_analyses_show.setBorder(BorderFactory.createTitledBorder("Field visible in"));
			int y = 0;
			for (final Analysis analysis : manager.getAvailableAnalysesAsArray()) {
				JCheckBox box = new JCheckBox(analysis.toString());
				box.setSelected(field.getAnalyses().contains(analysis));
				box.setToolTipText(tooltipAnalysesShow);
				box.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(ItemEvent e) {
						if (e.getStateChange() == ItemEvent.SELECTED) {
							field.addAnalysis(analysis);
						}else {
							field.removeAnalysis(analysis);
						}
						try {
							field.update();
						}catch(Exception ex) {
							ProjectManager.toConsole(ex);
						}
					}
				});
				panel_analyses_show.add(box, new GridBagConstraints(0, y, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
				y++;
			}
			panel_analyses_show.setToolTipText(tooltipAnalysesShow);
			lowerPanel.add(panel_analyses_show);
			
			String tooltipTags = "<html>Tags are used in Highlander for different purposes.<br>"
					+ "<ul>"
					+ "<li><b>FORMAT_PERCENT_0</b>: field is a value between 0 and 1 that should be displayed as a percentage without decimals.<br> <i>e.g.</i> 0.7234 will be displayed 72%.</li>"
					+ "<li><b>FORMAT_PERCENT_2</b>: field is a value between 0 and 1 that should be displayed as a percentage with 2 decimals.<br> <i>e.g.</i> 0.7234 will be displayed 72.34%.</li>"
					+ "</ul>"
					+ "</html>";
			JPanel panel_tags = new JPanel(new GridBagLayout());
			panel_tags.setBorder(BorderFactory.createTitledBorder("Tags"));
			int s = 0;
			for (final Tag tag : Tag.values()) {
				JCheckBox box = new JCheckBox(tag.toString());
				box.setSelected(field.getTags().contains(tag));
				box.setToolTipText(tooltipTags);
				box.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(ItemEvent e) {
						if (e.getStateChange() == ItemEvent.SELECTED) {
							field.addTag(tag);
						}else {
							field.removeTag(tag);
						}
						try {
							field.update();
						}catch(Exception ex) {
							ProjectManager.toConsole(ex);
						}
					}
				});
				panel_tags.add(box, new GridBagConstraints(0, s, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
				s++;
			}
			panel_tags.setToolTipText(tooltipTags);
			lowerPanel.add(panel_tags);
			
			panel.add(lowerPanel, new GridBagConstraints(0, row, 3, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			row++;

			panel.add(new JPanel(), new GridBagConstraints(0, row, 3, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
			
			scrollData.setViewportView(panel);
		}
	}

	public void createCategory() {
		Object resu = JOptionPane.showInputDialog(this, "Category name", "Creating category", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.i3dPlus, 64), null, null);
		if (resu != null){
			String name = resu.toString().toLowerCase();
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(true);
					waitingPanel.start();
				}
			});
			try{
				int count = 0;
				try (Results res = DB.select(Schema.HIGHLANDER, "SELECT COUNT(*) FROM `field_categories` WHERE `category` = '"+name+"'")) {
					if (res.next()){
						count = res.getInt(1);
					}
				}
				if (count > 0){
					JOptionPane.showMessageDialog(this, "Category name already exists'", "Error", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
				}else if (name.length() > 255){
					JOptionPane.showMessageDialog(this, "Category is limited to 255 characters'", "Error", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
				}else{
							ProjectManager.toConsole("-----------------------------------------------------");
							ProjectManager.toConsole("Creating category " + name);
							final Category category = new Category(name);
							category.insert();
							Category.fetchAvailableCategories(Highlander.getDB());
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									listCategoriesModel.addElement(category);
									listCategories.setSelectedValue(category, true);
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
	
	public void deleteCategory(Category category) {
		try{
			int count = 0;
			try (Results res = DB.select(Schema.HIGHLANDER, "SELECT COUNT(*) FROM `fields` WHERE `category` = '"+category+"'")) {
				if (res.next()){
					count = res.getInt(1);
				}
			}
			if (count == 0){
				ProjectManager.setHardUpdate(true);
				int res = JOptionPane.showConfirmDialog(new JFrame(), "Are you SURE you want to DEFINITIVELY delete category:\n"+category+" ?", "Delete category", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.i3dMinus,64));
				if (res == JOptionPane.CANCEL_OPTION){
					return;
				}else if (res == JOptionPane.YES_OPTION){
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							waitingPanel.setVisible(true);
							waitingPanel.start();
						}
					});
					try {
						ProjectManager.toConsole("-----------------------------------------------------");
						ProjectManager.toConsole("Deleting category '"+category+"'");
						category.delete();
						Category.fetchAvailableCategories(Highlander.getDB());
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								listCategories.clearSelection();
								listCategories.setSelectedIndex(0);
								listCategoriesModel.removeElement(category);
							}
						});
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
				ProjectManager.setHardUpdate(false);
			}else{
				JOptionPane.showMessageDialog(new JFrame(), count + " fields are still linked to this category.\nPlease first delete those fields or link them to another category.", "Delete category", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}
		}catch(Exception ex){
			ProjectManager.toConsole(ex);
		}
	}
	
	public void renameCategory(Category category) {
		Object resu = JOptionPane.showInputDialog(this, "Category name", "Renaming category", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iUpdater, 64), null, category);
		if (resu != null){
			String name = resu.toString().toLowerCase();
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(true);
					waitingPanel.start();
				}
			});
			try{
				int count = 0;
				try (Results res = DB.select(Schema.HIGHLANDER, "SELECT COUNT(*) FROM `field_categories` WHERE `category` = '"+name+"'")) {
					if (res.next()){
						count = res.getInt(1);
					}
				}
				if (count > 0){
					JOptionPane.showMessageDialog(this, "Category name already exists", "Error", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
				}else if (name.length() > 255){
					JOptionPane.showMessageDialog(this, "Category is limited to 255 characters'", "Error", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
				}else{
					ProjectManager.toConsole("-----------------------------------------------------");
					ProjectManager.toConsole("Renaming category " + category + " to "  + name);
					category.updateName(name);
					Category.fetchAvailableCategories(Highlander.getDB());
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
	
	public void enableCategoryDetailBox(Category category) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				waitingPanel.setVisible(true);
				waitingPanel.start();
			}
		});
		try {
			ProjectManager.toConsole("-----------------------------------------------------");
			ProjectManager.toConsole("Updating category '"+category+"'");
			category.setGenericDetailBox(!category.hasGenericDetailBox());
			category.update();
			Category.fetchAvailableCategories(Highlander.getDB());
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
	
	public void colorCategory(Category category) {
		Object resu = JOptionPane.showInputDialog(this, "Category color", "Color category", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iHighlighting, 64), Palette.values(), category.getColor());
		if (resu != null){
			Palette color = (Palette)resu;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(true);
					waitingPanel.start();
				}
			});
			try {
				ProjectManager.toConsole("-----------------------------------------------------");
				ProjectManager.toConsole("Updating category '"+category+"'");
				category.setColor(color);
				category.update();
				Category.fetchAvailableCategories(Highlander.getDB());
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
	
	public void createCustomField(){
		Object resu = JOptionPane.showInputDialog(this, "Field name (alphanumeric caracters only and '_').", "Creating custom field", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.i3dPlus, 64), null, null);
		if (resu != null){
			String fieldStr = resu.toString();
			fieldStr = fieldStr.trim().replace(' ', '_').toLowerCase();
			Pattern pat = Pattern.compile("(^[0-9])|([^a-zA-Z0-9_])");
			if (pat.matcher(fieldStr).find()){
				JOptionPane.showMessageDialog(this, "Field name can only contain alphanumeric caracters and '_', and cannot start by a number", "Error", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
			}else if (Field.getAvailableFields(false).contains(new Field(fieldStr))){
				JOptionPane.showMessageDialog(this, "Field '"+fieldStr+"' already exists", "Error", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
			}else{
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						waitingPanel.setVisible(true);
						waitingPanel.start();
					}
				});
				try{
					ProjectManager.setHardUpdate(true);
					Field newField = new Field(fieldStr);
					newField.setCategory(listCategories.getSelectedValue());
					newField.setTableSuffix("_custom_annotations");
					newField.setDescription("");
					newField.setSource("");
					newField.setAnnotation(Annotation.VCF);
					ProjectManager.toConsole("-----------------------------------------------------");
					ProjectManager.toConsole("Creating custom field " + newField);
					newField.insert();
					Field.fetchAvailableFields(Highlander.getDB());
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							listFieldsModel.addElement(newField);
							listFields.setSelectedValue(newField, true);
						}
					});
				}catch(Exception ex){
					ProjectManager.toConsole(ex);
				}
				ProjectManager.setHardUpdate(false);
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						waitingPanel.setVisible(false);
						waitingPanel.stop();
					}
				});
			}
		}		
	}

	public void deleteCustomField(Field field){
		if (!field.getTableSuffix().equals("_custom_annotations") && !field.equals(Field.variant_custom_id)) {
			//Only custom fields can be deleted
			return;
		}
		ProjectManager.setHardUpdate(true);
		int res = JOptionPane.showConfirmDialog(new JFrame(), "Are you SURE you want to DEFINITIVELY delete custom field:\n"+field+" and ALL variant data associated with it ?", "Delete custom field", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.i3dMinus,64));
		if (res == JOptionPane.CANCEL_OPTION){
			return;
		}else if (res == JOptionPane.YES_OPTION){
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(true);
					waitingPanel.start();
				}
			});
			try {
				ProjectManager.toConsole("-----------------------------------------------------");
				ProjectManager.toConsole("Deleting Field '"+field+"'");
				field.delete();
				Field.fetchAvailableFields(Highlander.getDB());
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						listFields.clearSelection();
						listFields.setSelectedIndex(0);
						listFieldsModel.removeElement(field);
					}
				});
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
		ProjectManager.setHardUpdate(false);
	}
	
	private void reorderCategories(int index, boolean up) {
		if (up && index == 0) return;
		if (!up && index == listCategoriesModel.getSize()-1)  return;
		Category category = listCategoriesModel.get(index);
		Category other = listCategoriesModel.get((up)?index-1:index+1);
		if (up) {
			int newOrder = other.getOrdering();	
			other.setOrdering(category.getOrdering());
			category.setOrdering(newOrder);
		}else {
			int newOrder = category.getOrdering();
			category.setOrdering(other.getOrdering());
			other.setOrdering(newOrder);
		}
		try {
			category.update();
			other.update();
			listCategoriesModel.set(index, other);
			listCategoriesModel.set((up)?index-1:index+1, category);
			listCategories.setSelectedValue(category, true);
		}catch(Exception ex) {
			ProjectManager.toConsole(ex);
		}
	}
	
	private void reorderFields(int index, boolean up) {
		if (up && index == 0) return;
		if (!up && index == listFieldsModel.getSize()-1)  return;
		Field field = listFieldsModel.get(index);
		Field other = listFieldsModel.get((up)?index-1:index+1);
		if (up) {
			int newOrder = other.getOrdering();	
			other.setOrdering(field.getOrdering());
			field.setOrdering(newOrder);
		}else {
			int newOrder = field.getOrdering();
			field.setOrdering(other.getOrdering());
			other.setOrdering(newOrder);
		}
		try {
			field.update();
			other.update();
			listFieldsModel.set(index, other);
			listFieldsModel.set((up)?index-1:index+1, field);
			listFields.setSelectedValue(field, true);
		}catch(Exception ex) {
			ProjectManager.toConsole(ex);
		}
	}
	
	private class CategoryCellRenderer extends DefaultListCellRenderer {
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean hasFocus) {
      Component comp = super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
      JLabel label = (JLabel) comp;
      Category category = (Category)value;
      label.setText(value.toString());
      label.setForeground(Resources.getColor(category.getColor(), 500, false));
      if (category.hasGenericDetailBox()){
      	label.setIcon(Resources.getScaledIcon(Resources.iButtonApply, 14));
      }else {
      	label.setIcon(Resources.getScaledIcon(Resources.iCross, 14));
      }
      if (isSelected) {
        label.setBackground(new Color(57,105,138));
      }
      return label;
    }
  }

	private class FieldCellRenderer extends DefaultListCellRenderer {
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean hasFocus) {
			Component comp = super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
			JLabel label = (JLabel) comp;
			Field field = (Field)value;
			label.setText(value.toString());
			if (field.getTableSuffix().equals("_custom_annotations") && !field.equals(Field.variant_custom_id)){
				label.setForeground(new Color(72,0,255));
			}
			if (isSelected) {
				label.setBackground(new Color(57,105,138));
			}
			return label;
		}
	}
	
}
