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

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.JScrollPane;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.JTextComponent;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.Resources.Palette;
import be.uclouvain.ngs.highlander.UI.dialog.ProfileTree.Action;
import be.uclouvain.ngs.highlander.administration.users.User.UserData;
import be.uclouvain.ngs.highlander.database.Category;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.AutoCompleteSupport;

public class AskListOfFreeValuesDialog extends JDialog {
	private String listName = null;
	private List<String> selection = new ArrayList<String>();
	private IncrementableTableModel tmodel;
	private JTable table;
	private JPanel panelField;
	private JComboBox<AnalysisFull> boxAnalyses;
	private ComboboxToolTipRenderer renderer;
	private JComboBox<String> boxCategories;
	private JComboBox<Field> boxField;
	private EventList<Field> fields;
	AutoCompleteSupport<Field> support;
	private JButton btnSaveList;
	private JButton btnLoadList;
	private JButton btnPossibleValuesDatabase;
	private JButton btnValidateList;
	private JButton btnOk;
	private Set<String> possibleValues = new HashSet<>();

	public AskListOfFreeValuesDialog(){
		this(null, new ArrayList<String>());
	}

	public AskListOfFreeValuesDialog(Field field){
		this(field, new ArrayList<String>());
	}
	
	public AskListOfFreeValuesDialog(Field field, List<String> startSelection) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int width = screenSize.width - (screenSize.width/3*2);
		int height = screenSize.height - (screenSize.height/3);
		setSize(new Dimension(width,height));
		initUI();
		int row=0;
		for (String value : startSelection){
			table.setValueAt(value, row++, 0);
		}
		if (field != null) {
		this.addComponentListener(new ComponentListener() {
			@Override
			public void componentShown(ComponentEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						if (!field.hasAnalysis(Highlander.getCurrentAnalysis())) {
							for (AnalysisFull analysis : Highlander.getAvailableAnalyses()) {
								if (field.hasAnalysis(analysis)) {
									boxAnalyses.setSelectedItem(analysis);
									break;
								}
							}
						}
						boxField.setSelectedItem(field);
					}
				}, "AskListOfFreeValuesDialog.shown").start();
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
		}
	}

	private void initUI(){
		setModal(true);
		setTitle("Create value list");
		setIconImage(Resources.getScaledIcon(Resources.iList, 64).getImage());

		JPanel panel_north = new JPanel(new BorderLayout());
		getContentPane().add(panel_north, BorderLayout.NORTH);
		
		JPanel panel_1 = new JPanel(new BorderLayout());
		panel_north.add(panel_1, BorderLayout.SOUTH);
		
		panelField = new JPanel(new GridBagLayout());
		panel_1.add(panelField, BorderLayout.CENTER);
		
		JPanel panelAnalyses = new JPanel();
		boxAnalyses = new JComboBox<AnalysisFull>(Highlander.getAvailableAnalyses().toArray(new AnalysisFull[0]));
		boxAnalyses.setToolTipText("Analysis");
		boxAnalyses.setSelectedItem(Highlander.getCurrentAnalysis());
		boxAnalyses.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED){
					setPanelFields();
				}
			}
		});
		panelAnalyses.add(boxAnalyses);
		panel_1.add(panelAnalyses, BorderLayout.WEST);
		
		Field[] fieldsArr = Field.getAvailableFields(Highlander.getCurrentAnalysis(), true).toArray(new Field[0]);
		fields = GlazedLists.eventListOf(fieldsArr);
		boxField = new JComboBox<>(fieldsArr);
		boxField.setMaximumRowCount(20);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				support = AutoCompleteSupport.install(boxField, fields);
				support.setCorrectsCase(true);
				support.setFilterMode(TextMatcherEditor.CONTAINS);
				support.setTextMatchingStrategy(TextMatcherEditor.NORMALIZED_STRATEGY);
				support.setStrict(false);
			}
		});		
		boxField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (arg0.getActionCommand().equals("comboBoxEdited")){
					if (boxField.getSelectedIndex() < 0) boxField.setSelectedItem(null);
				}
				ComboboxToolTipRenderer renderer = new ComboboxToolTipRenderer();
				renderer.setTooltips(support.getItemList());
				boxField.setRenderer(renderer);
			}
		});
		boxField.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					if (boxField.getSelectedIndex() >= 0){
						fieldIsSelected();
					}
				}
			}
		});
		renderer = new ComboboxToolTipRenderer();
		renderer.setTooltips(Field.getAvailableFields(Highlander.getCurrentAnalysis(), true));
		boxField.setRenderer(renderer);
		GridBagConstraints gbc_comboBox_field = new GridBagConstraints();
		gbc_comboBox_field.weightx = 1.0;
		gbc_comboBox_field.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBox_field.insets = new Insets(5, 5, 5, 5);
		gbc_comboBox_field.anchor = GridBagConstraints.NORTHWEST;
		gbc_comboBox_field.gridx = 0;
		gbc_comboBox_field.gridy = 0;
		panelField.add(boxField, gbc_comboBox_field);

		boxCategories = new JComboBox<>(Category.getAvailableCategories(true,true));
		boxCategories.setToolTipText("Restrict 'Database field' for easier searching");
		boxCategories.setMaximumRowCount(20);
		boxCategories.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							filterByCategory((AnalysisFull)boxAnalyses.getSelectedItem());							
						}
					});
				}
			}
		});
		GridBagConstraints gbc_boxCategories = new GridBagConstraints();
		gbc_boxCategories.insets = new Insets(5, 5, 5, 5);
		gbc_boxCategories.fill = GridBagConstraints.HORIZONTAL;
		gbc_boxCategories.gridx = 1;
		gbc_boxCategories.gridy = 0;
		panelField.add(boxCategories, gbc_boxCategories);
		
		JPanel panel_2 = new JPanel();
		panel_north.add(panel_2, BorderLayout.NORTH);

		JButton btnFile = new JButton(Resources.getScaledIcon(Resources.iImportFile, 40));
		btnFile.setToolTipText("Import values from a text file");
		btnFile.setPreferredSize(new Dimension(54,54));
		btnFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				FileDialog chooser = new FileDialog(AskListOfFreeValuesDialog.this, "Choose a plain text file containing 1 value per line", FileDialog.LOAD);
				chooser.setVisible(true);
				if (chooser.getFile() != null) {
					try {
						File file = new File(chooser.getDirectory() + chooser.getFile());
						try (FileReader fr = new FileReader(file)){
							try (BufferedReader br = new BufferedReader(fr)){
								String line;
								int row=0;
								while (table.getValueAt(row, 0) != null) row++;
								while ((line = br.readLine()) != null){
									table.setValueAt(line, row++, 0);
								}
							}
						}
						tmodel.fireTableDataChanged();
					} catch (Exception ex) {
						Tools.exception(ex);
						JOptionPane.showMessageDialog(AskListOfFreeValuesDialog.this, Tools.getMessage("Error", ex), 
								"Import values from a text file", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
					btnOk.setText(getValuesCount()+" values");
				}
			}
		});
		panel_2.add(btnFile);

		JButton btnSort = new JButton(Resources.getScaledIcon(Resources.iSortAZ, 40));
		btnSort.setToolTipText("Sort current list alphabetically and remove duplicates");
		btnSort.setPreferredSize(new Dimension(54,54));
		btnSort.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				sort();
			}
		});
		panel_2.add(btnSort);

		btnSaveList = new JButton(Resources.getScaledIcon(Resources.iDbSave, 40));
		btnSaveList.setToolTipText("Save current list of values in your profile");
		btnSaveList.setPreferredSize(new Dimension(54,54));
		btnSaveList.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				saveList();
			}
		});
		panel_2.add(btnSaveList);

		btnPossibleValuesDatabase = new JButton(Resources.getScaledIcon(Resources.iDbSearch, 40));
		btnPossibleValuesDatabase.setToolTipText("Add values among possible values found in the database");
		btnPossibleValuesDatabase.setPreferredSize(new Dimension(54,54));
		btnPossibleValuesDatabase.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					if (boxAnalyses.getSelectedItem() != null && boxField.getSelectedItem() != null){
						List<String> existingValues = new ArrayList<String>();
						for (int row = 0 ; row < table.getRowCount() ; row++){
							if (table.getValueAt(row, 0) != null && table.getValueAt(row, 0).toString().length() > 0) 
								existingValues.add(table.getValueAt(row, 0).toString().trim());
						}
						AskListOfPossibleValuesDialog ask = new AskListOfPossibleValuesDialog((Field)boxField.getSelectedItem(), null, false, existingValues);
						Tools.centerWindow(ask, false);
						ask.setVisible(true);
						if (!ask.getSelection().isEmpty()){
							loadList(ask.getSelection());
						}
					}
				} catch (Exception ex) {
					Tools.exception(ex);
					JOptionPane.showMessageDialog(AskListOfFreeValuesDialog.this, Tools.getMessage("Error retrieving selected database field", ex), "Ask for possible values", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}
			}
		});
		panel_2.add(btnPossibleValuesDatabase);
		
		btnLoadList = new JButton(Resources.getScaledIcon(Resources.iDbLoad, 40));
		btnLoadList.setToolTipText("Load a list of values from your profile");
		btnLoadList.setPreferredSize(new Dimension(54,54));
		btnLoadList.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String listname = ProfileTree.showProfileDialog(AskListOfFreeValuesDialog.this, Action.LOAD, UserData.VALUES, boxField.getSelectedItem().toString());
				if (listname != null){
					loadList((Field)boxField.getSelectedItem(), listname);
				}
			}
		});
		panel_2.add(btnLoadList);

		btnValidateList = new JButton(Resources.getScaledIcon(Resources.iUserListValidate, 40));
		btnValidateList.setToolTipText("Validate the list of values from your profile");
		btnValidateList.setPreferredSize(new Dimension(54,54));
		btnValidateList.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				List<String> list = new ArrayList<String>();
				List<String> comments = new ArrayList<String>();
				for (int row = 0 ; row < table.getRowCount() ; row++){
					if (table.getValueAt(row, 0) != null && table.getValueAt(row, 0).toString().length() > 0) {
						list.add(table.getValueAt(row, 0).toString().trim());
						if (table.getValueAt(row, 1) != null && table.getValueAt(row, 1).toString().length() > 0) {
							comments.add(table.getValueAt(row, 1).toString().trim());
						}else {
							comments.add("");
						}
					}
				}
				if (boxAnalyses.getSelectedItem() != null && boxField.getSelectedItem() != null){
					ValidateListOfPossibleValuesDialog validation = new ValidateListOfPossibleValuesDialog(list, comments, (AnalysisFull)boxAnalyses.getSelectedItem(), (Field)boxField.getSelectedItem());
					Tools.centerWindow(validation, false);
					validation.setVisible(true);
					if (!validation.getSelection().isEmpty()) {
						tmodel.clear();
						for (int row=0 ; row < validation.getSelection().size() ; row++)	{
							table.setValueAt(validation.getSelection().get(row), row, 0);
							table.setValueAt(validation.getSelectionComments().get(row), row, 1);
						}
						tmodel.fireTableDataChanged();
						btnOk.setText(getValuesCount()+" values");
					}
				}

			}
		});
		panel_2.add(btnValidateList);

		JPanel panel = new JPanel();	
		getContentPane().add(panel, BorderLayout.SOUTH);

		btnOk = new JButton(Resources.getScaledIcon(Resources.iButtonApply, 24));
		btnOk.setText("0 values");
		btnOk.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				for (int i=0 ; i < tmodel.getRowCount() ; i++){
					if (tmodel.getValueAt(i, 0) != null && tmodel.getValueAt(i, 0).toString().length() > 0)
						selection.add(tmodel.getValueAt(i, 0).toString());
				}
				dispose();
			}
		});
		panel.add(btnOk);

		JButton btnCancel = new JButton(Resources.getScaledIcon(Resources.iCross, 24));
		btnCancel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				cancelClose();
			}
		});
		panel.add(btnCancel);

		JPanel panel_0 = new JPanel();
		getContentPane().add(panel_0, BorderLayout.CENTER);

		GridBagLayout gbl_panel_1 = new GridBagLayout();
		panel_0.setLayout(gbl_panel_1);

		JScrollPane scrollPane = new JScrollPane();
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.weighty = 1.0;
		gbc_scrollPane.weightx = 1.0;
		gbc_scrollPane.insets = new Insets(5, 5, 0, 5);
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.gridx = 0;
		gbc_scrollPane.gridy = 0;
		panel_0.add(scrollPane, gbc_scrollPane);

		tmodel = new IncrementableTableModel();

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
		//table.setTableHeader(null);
		table.getDefaultEditor(Object.class).addCellEditorListener(new CellEditorListener() {
			@Override
			public void editingStopped(ChangeEvent arg0) {
				btnOk.setText(getValuesCount()+" values");
			}
			@Override
			public void editingCanceled(ChangeEvent arg0) {
				btnOk.setText(getValuesCount()+" values");
			}
		});
		new ExcelAdapter(table);
		table.getColumnModel().getColumn(0).setCellRenderer(new ErrorCellRenderer());
		scrollPane.setViewportView(table);

		btnOk.setEnabled(false);
		btnSaveList.setEnabled(false);
		btnPossibleValuesDatabase.setEnabled(false);
		btnLoadList.setEnabled(false);
		btnValidateList.setEnabled(false);

	}

	private void setPanelFields() {
		boxCategories.setSelectedIndex(0);
		boxField.setSelectedItem(null);
		AnalysisFull analysis = (AnalysisFull)boxAnalyses.getSelectedItem();
		List<Field> fields = Field.getAvailableFields(analysis, true);
		fields.clear();
		for (Field f : fields){
			fields.add(f);
		}
		renderer.setTooltips(fields);
		fieldIsSelected();
	}
	
	public class ComboboxToolTipRenderer extends DefaultListCellRenderer {
		List<Field> tooltips;

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {

			JComponent comp = (JComponent) super.getListCellRendererComponent(list,
					value, index, isSelected, cellHasFocus);

			if (-1 < index && null != value && null != tooltips) {
				list.setToolTipText(tooltips.get(index).getHtmlTooltip());
			}
			return comp;
		}

		public void setTooltips(List<Field> tooltips) {
			this.tooltips = tooltips;
		}
	}

	private void filterByCategory(AnalysisFull analysis){
		fields.clear();
		for (Field field : Field.getAvailableFields(analysis, true)){
			if (boxCategories.getSelectedItem().toString().equals("all available fields") || 
					field.getCategory().getName().equals(boxCategories.getSelectedItem().toString())){
				fields.add(field);
			}
		}
		ComboboxToolTipRenderer renderer = new ComboboxToolTipRenderer();
		renderer.setTooltips(fields);
		boxField.setRenderer(renderer);
		boxField.validate();
		boxField.repaint();
	}

	private void fieldIsSelected() {
		possibleValues.clear();
		if (boxField.getSelectedItem() != null) {
			btnOk.setEnabled(true);
			btnSaveList.setEnabled(true);
			btnPossibleValuesDatabase.setEnabled(true);
			btnLoadList.setEnabled(true);
			btnValidateList.setEnabled(true);
			AnalysisFull analysis = (AnalysisFull)boxAnalyses.getSelectedItem();
			Field field = (Field)boxField.getSelectedItem();
			try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT `value` FROM " + analysis.getFromPossibleValues() + "WHERE `field` = '" + field.getName() + "'")) {
				while (res.next()) {
					possibleValues.add(res.getString(1).toUpperCase());
				}
			}catch(Exception ex) {
				ex.printStackTrace();
			}
		}else {
			btnOk.setEnabled(false);
			btnSaveList.setEnabled(false);
			btnPossibleValuesDatabase.setEnabled(false);
			btnLoadList.setEnabled(false);
			btnValidateList.setEnabled(false);
		}
		table.validate();
		table.repaint();
	}
	
	public static class IncrementableTableModel	extends AbstractTableModel {
		private String[] headers = new String[] {"Values","Comments"};
		private List<Object[]> data;
		private final int ncol = 2;

		public IncrementableTableModel() {   
			data = new ArrayList<Object[]>();
			data.add(new Object[ncol]);
		}

		@Override
		public int getColumnCount() {
			return ncol;
		}

		@Override
		public int getRowCount() {
			return data.size();
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return Object.class;
		}

		@Override
		public String getColumnName(int col) {
			return headers[col];
		}
		
		@Override
		public Object getValueAt(int row, int col) {
			return data.get(row)[col];
		}

		@Override
		public void setValueAt(Object value, int row, int col) {
			data.get(row)[col] = value;
			if (!isLastLineEmpty()){
				data.add(new Object[ncol]);
			}
			fireTableCellUpdated(row, col);			
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return true;
		}

		public boolean isLastLineEmpty(){
			Object[] obj = data.get(data.size()-1);
			for (int i = 0 ; i < obj.length ; i++){
				if (obj[i] != null) return false;
			}
			return true;
		}

		public void clear() {
			data.clear();
			data.add(new Object[ncol]);
		}

	}

	class ErrorCellRenderer extends DefaultTableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if(value != null && !validateItem(value.toString())) {
				if (isSelected) {
					component.setBackground(new Color(57,105,138));
					component.setForeground(Resources.getColor(Palette.Red, 500, true));
				}else {
					if (row%2 == 1) {
						component.setBackground(Resources.getColor(Palette.Red, 300, false));				
					}else {
						component.setBackground(Resources.getColor(Palette.Red, 400, false));
					}
					component.setForeground(Color.BLACK);
				}
			} else {
				if (isSelected) {
					component.setBackground(new Color(57,105,138));
					component.setForeground(Color.WHITE);
				}else {
					if (row%2 == 1) {
						component.setBackground(new Color(242,242,242));				
					}else {
						component.setBackground(Color.WHITE);
					}
					component.setForeground(Color.BLACK);
				}
			}
			return component;
		}
	}

	private boolean validateItem(String value) {
		if (!possibleValues.isEmpty()) {
			return possibleValues.contains(value.toUpperCase());
		}else {
			return true;
		}
	}
	
	public class ExcelAdapter implements ActionListener {
		private String rowstring,value;
		private Clipboard system;
		private JTable table ;
		/**
		 * The Excel Adapter is constructed with a
		 * JTable on which it enables Copy-Paste and acts
		 * as a Clipboard listener.
		 */
		public ExcelAdapter(JTable myJTable){
			table = myJTable;
			KeyStroke paste = KeyStroke.getKeyStroke(KeyEvent.VK_V,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(),false);
			table.registerKeyboardAction(this,"Paste",paste,JComponent.WHEN_FOCUSED);
			KeyStroke delete = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,0,false);
			table.registerKeyboardAction(this,"Delete",delete,JComponent.WHEN_FOCUSED);
			system = Toolkit.getDefaultToolkit().getSystemClipboard();
		}
		/**
		 * Public Accessor methods for the Table on which this adapter acts.
		 */
		public JTable getJTable() {return table;}
		public void setJTable(JTable jTable1) {this.table=jTable1;}
		/**
		 * This method is activated on the Keystrokes we are listening to
		 * in this implementation. Here it listens for Copy and Paste ActionCommands.
		 * Selections comprising non-adjacent cells result in invalid selection and
		 * then copy action cannot be performed.
		 * Paste is done by aligning the upper left corner of the selection with the
		 * 1st element in the current selection of the JTable.
		 */
		@Override
		public void actionPerformed(ActionEvent e){
			if (e.getActionCommand().compareTo("Paste")==0){
				int startRow=(table.getSelectedRows())[0];
				int startCol=(table.getSelectedColumns())[0];
				try	{
					String trstring= ((String)(system.getContents(this).getTransferData(DataFlavor.stringFlavor))).replace("\r", "\n");
					String[] st1= trstring.split("\n");
					for(int i=0; i < st1.length ;i++)	{
						rowstring=st1[i];
						String[] st2= rowstring.split("\t");
						for(int j=0; j < st2.length ;j++)	{
							value= st2[j];
							if (startRow+i< table.getRowCount()  &&
									startCol+j< table.getColumnCount())
								table.setValueAt(value,startRow+i,startCol+j);
						}
					}
				}	catch(Exception ex){
					Tools.exception(ex);
				}
				tmodel.fireTableRowsUpdated(0,tmodel.getRowCount()-1);
				tmodel.fireTableDataChanged();
				btnOk.setText(getValuesCount()+" values");
			}else if (e.getActionCommand().compareTo("Delete")==0){
				try	{
					for (int row : table.getSelectedRows()){
						for (int col : table.getSelectedColumns()){
							table.setValueAt(null,row,col);
						}
					}
				}	catch(Exception ex){
					Tools.exception(ex);
				}
				tmodel.fireTableRowsUpdated(0,tmodel.getRowCount()-1);
				btnOk.setText(getValuesCount()+" values");
			}
		}
	}


	public List<String> getSelection(){
		return selection;
	}

	public Field getSelectedField() {
		return (Field)boxField.getSelectedItem();
	}
	
	public String getListName(){
		return listName;
	}

	public boolean saveList(){
		if (boxField.getSelectedItem() != null) {
			String name = ProfileTree.showProfileDialog(this, Action.SAVE, UserData.VALUES, boxField.getSelectedItem().toString(), "Save list of values to your profile", listName);
			saveList(name);
			return true;
		}
		return false;
	}

	public boolean saveList(String name){
		try {
			if (name == null) return false;
			if (boxField.getSelectedItem() != null) {
				if (Highlander.getLoggedUser().doesPersonalDataExists(UserData.VALUES, boxField.getSelectedItem().toString(), name)){
					int yesno = JOptionPane.showConfirmDialog(new JFrame(), 
							"You already have a "+UserData.VALUES.getName()+" named '"+name.replace("~", " -> ")+"', do you want to overwrite it ?", 
							"Overwriting element in your profile", JOptionPane.YES_NO_OPTION , JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iDbSave,64));
					if (yesno == JOptionPane.NO_OPTION)	return false;
				}
				listName = name;
				Map<String,String> map = new LinkedHashMap<>();
				for (int row = 0 ; row < table.getRowCount() ; row++){
					if (table.getValueAt(row, 0) != null && table.getValueAt(row, 0).toString().length() > 0) {
						String comment = (table.getValueAt(row, 1) != null && table.getValueAt(row, 1).toString().length() > 0) ? table.getValueAt(row, 1).toString().trim() : "";
						map.put(table.getValueAt(row, 0).toString().trim(), comment);
					}
				}
				Highlander.getLoggedUser().saveValues(listName, (Field)boxField.getSelectedItem(), map);
				return true;
			}
		} catch (Exception ex) {
			Tools.exception(ex);
			JOptionPane.showMessageDialog(AskListOfFreeValuesDialog.this, Tools.getMessage("Error", ex), "Save current criteria list in your profile", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
		return false;
	}

	public void loadList(Field field, String key){
		listName = key;
		int row=0;
		while (table.getValueAt(row, 0) != null) row++;
		try {
			for (Entry<String, String> item : Highlander.getLoggedUser().loadValuesWithComments(field, listName).entrySet()){
				table.setValueAt(item.getKey(), row, 0);
				table.setValueAt(item.getValue(), row, 1);
				row++;
			}
		} catch (Exception ex) {
			Tools.exception(ex);
			JOptionPane.showMessageDialog(AskListOfFreeValuesDialog.this, Tools.getMessage("Error", ex), 
					"Load list of values from your profile", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
		tmodel.fireTableDataChanged();
		btnOk.setText(getValuesCount()+" values");
	}

	public void loadList(Set<String> values){
		tmodel.clear();
		int row=0;
		for (String item : values){
			table.setValueAt(item, row++, 0);
		}
		tmodel.fireTableDataChanged();
		btnOk.setText(getValuesCount()+" values");
	}

	public void sort(){
		Map<String,Object> map = new TreeMap<String,Object>(new Tools.NaturalOrderComparator(true));
		for (int row = 0 ; row < table.getRowCount() ; row++){
			if (table.getValueAt(row, 0) != null && table.getValueAt(row, 0).toString().length() > 0){ 
				map.put(table.getValueAt(row, 0).toString().trim(), table.getValueAt(row, 1));
				table.setValueAt(null, row, 0);
				table.setValueAt(null, row, 1);
			}
		}
		int row = 0;
		for (String item : map.keySet()){
			table.setValueAt(item, row, 0);
			table.setValueAt(map.get(item), row, 1);
			row++;
		}
		btnOk.setText(getValuesCount()+" values");
	}

	private int getValuesCount(){
		int count = 0;
		for (int row = 0 ; row < table.getRowCount() ; row++){
			if (table.getValueAt(row, 0) != null && table.getValueAt(row, 0).toString().length() > 0) 
				count++;
		}
		return count;
	}

	//Overridden so we can exit when window is closed
	@Override
	protected void processWindowEvent(WindowEvent e) {
		super.processWindowEvent(e);
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			cancelClose();
		}
	}

	private void cancelClose(){
		selection.clear();
		dispose();
	}

}
