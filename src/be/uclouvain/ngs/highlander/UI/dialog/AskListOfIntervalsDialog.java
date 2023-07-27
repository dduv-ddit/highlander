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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;

import java.awt.BorderLayout;

import javax.swing.JButton;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
import java.io.FileWriter;

import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.JTextComponent;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.dialog.ProfileTree.Action;
import be.uclouvain.ngs.highlander.administration.users.User.UserData;
import be.uclouvain.ngs.highlander.datatype.Interval;
import be.uclouvain.ngs.highlander.datatype.Reference;

public class AskListOfIntervalsDialog extends JDialog {
	private Reference reference;
	private String listName = null;
	private List<Interval> selection = new ArrayList<Interval>();
	private IncrementableTableModel tmodel;
	private JTable table;
	private JButton btnOk;

	public AskListOfIntervalsDialog(Reference reference) {
		this(reference, new HashSet<Interval>());
	}

	public AskListOfIntervalsDialog(Reference reference, Set<Interval> intervals) {
		this.reference = reference;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int width = screenSize.width - (screenSize.width/3*2);
		int height = screenSize.height - (screenSize.height/3);
		setSize(new Dimension(width,height));
		initUI();
		loadList(intervals.toArray(new Interval[0]));
	}

	private void initUI(){
		setModal(true);
		setTitle("Create list of genomic intervals");
		setIconImage(Resources.getScaledIcon(Resources.iInterval, 64).getImage());

		JPanel panel_2 = new JPanel();
		getContentPane().add(panel_2, BorderLayout.NORTH);

		JButton btnSort = new JButton(Resources.getScaledIcon(Resources.iSortAZ, 40));
		btnSort.setToolTipText("Sort current list by chromosome-positions and remove duplicates");
		btnSort.setPreferredSize(new Dimension(54,54));
		btnSort.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				sort();
			}
		});
		panel_2.add(btnSort);

		JButton btnFile = new JButton(Resources.getScaledIcon(Resources.iImportFile, 40));
		btnFile.setToolTipText("Import values from a bed file");
		btnFile.setPreferredSize(new Dimension(54,54));
		btnFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				FileDialog chooser = new FileDialog(AskListOfIntervalsDialog.this, "Choose a bed file", FileDialog.LOAD);
				chooser.setVisible(true);
				if (chooser.getFile() != null) {
					try {
						File bed = new File(chooser.getDirectory() + chooser.getFile());
						try (FileReader fr = new FileReader(bed)){
							try (BufferedReader br = new BufferedReader(fr)){
								String line;
								int row=0;
								while (table.getValueAt(row, 0) != null) row++;
								while ((line = br.readLine()) != null){
									if (!line.startsWith("track")){
										String[] array = line.split("\t");
										String chr = array[0];
										if (chr.startsWith("chr")) chr = chr.substring(3);
										table.setValueAt(chr, row, 0);
										if (array.length > 1) table.setValueAt(array[1], row, 1);
										if (array.length > 2) table.setValueAt(array[2], row, 2);
										row++;
									}
								}
							}
						}
					} catch (Exception ex) {
						Tools.exception(ex);
						JOptionPane.showMessageDialog(AskListOfIntervalsDialog.this, Tools.getMessage("Error", ex), 
								"Import values from a text file", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
					btnOk.setText(getValuesCount()+" interval(s)");
				}
			}
		});
		panel_2.add(btnFile);

		JButton btnLoadList = new JButton(Resources.getScaledIcon(Resources.iDbLoad, 40));
		btnLoadList.setToolTipText("Load a list of intervals from your profile");
		btnLoadList.setPreferredSize(new Dimension(54,54));
		btnLoadList.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				String listname = ProfileTree.showProfileDialog(AskListOfIntervalsDialog.this, Action.LOAD, UserData.INTERVALS, reference.getName());
				if (listname != null){
					loadList(listname);
				}
			}
		});
		panel_2.add(btnLoadList);

		JButton btnSaveFile = new JButton(Resources.getScaledIcon(Resources.iExportFile, 40));
		btnSaveFile.setToolTipText("Export current list of intervals in a bed file");
		btnSaveFile.setPreferredSize(new Dimension(54,54));
		btnSaveFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				saveBed();
			}
		});
		panel_2.add(btnSaveFile);

		JButton btnSaveList = new JButton(Resources.getScaledIcon(Resources.iDbSave, 40));
		btnSaveList.setToolTipText("Save current list of intervals in your profile");
		btnSaveList.setPreferredSize(new Dimension(54,54));
		btnSaveList.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				saveList();
			}
		});
		panel_2.add(btnSaveList);


		JPanel panel = new JPanel();	
		getContentPane().add(panel, BorderLayout.SOUTH);

		btnOk = new JButton(Resources.getScaledIcon(Resources.iButtonApply, 24));
		btnOk.setText("0 interval(s)");
		btnOk.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (createSelection()){
					dispose();
				}
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

		JPanel panel_1 = new JPanel();
		getContentPane().add(panel_1, BorderLayout.CENTER);

		GridBagLayout gbl_panel_1 = new GridBagLayout();
		panel_1.setLayout(gbl_panel_1);

		JScrollPane scrollPane = new JScrollPane();
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.weighty = 1.0;
		gbc_scrollPane.weightx = 1.0;
		gbc_scrollPane.insets = new Insets(5, 5, 0, 5);
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.gridx = 0;
		gbc_scrollPane.gridy = 0;
		panel_1.add(scrollPane, gbc_scrollPane);

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
		table.getDefaultEditor(Object.class).addCellEditorListener(new CellEditorListener() {
			@Override
			public void editingStopped(ChangeEvent arg0) {				
				btnOk.setText(getValuesCount()+" interval(s)");
			}
			@Override
			public void editingCanceled(ChangeEvent arg0) {
				btnOk.setText(getValuesCount()+" interval(s)");
			}
		});
		new ExcelAdapter(table);
		table.setCellSelectionEnabled(true);
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		scrollPane.setViewportView(table);

	}

	public static class IncrementableTableModel	extends AbstractTableModel {
		private List<Object[]> data;
		private String[] headers = new String[]{"Chromosome","Start position","End position"};
		private final int ncol = headers.length;

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
		public String getColumnName(int columnIndex) {
			return headers[columnIndex];
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return (columnIndex > 0) ? Integer.class : String.class;
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
				btnOk.setText(getValuesCount()+" interval(s)");
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
				btnOk.setText(getValuesCount()+" interval(s)");
			}
		}
	}

	private boolean createSelection(){
		selection.clear();
		for (int i=0 ; i < tmodel.getRowCount() ; i++){
			if (tmodel.getValueAt(i, 0) != null && tmodel.getValueAt(i, 0).toString().length() > 0 &&
					tmodel.getValueAt(i, 1) != null && tmodel.getValueAt(i, 1).toString().length() > 0){
				String chr = tmodel.getValueAt(i, 0).toString().trim();
				try{
					int start = Integer.parseInt(tmodel.getValueAt(i, 1).toString().trim());
					int end = start;
					if (tmodel.getValueAt(i, 2) != null && tmodel.getValueAt(i, 2).toString().length() > 0){
						try{
							end = Integer.parseInt(tmodel.getValueAt(i, 2).toString().trim());
						}catch(NumberFormatException ex){
							JOptionPane.showMessageDialog(AskListOfIntervalsDialog.this, tmodel.getValueAt(i, 2).toString() + " is not a valid position", 
									"Validate interval list", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
							return false;
						}
					}
					selection.add(new Interval(reference, chr, start, end));
				}catch(NumberFormatException ex){
					JOptionPane.showMessageDialog(AskListOfIntervalsDialog.this, tmodel.getValueAt(i, 1).toString() + " is not a valid position", 
							"Validate interval list", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					return false;
				}
			}
		}
		return true;
	}

	public List<Interval> getSelection(){
		return selection;
	}

	public String getListName(){
		return listName;
	}

	public void saveBed(){
		if (createSelection()){
			FileDialog chooser = new FileDialog(AskListOfIntervalsDialog.this, "Save as a bed file", FileDialog.LOAD);
			chooser.setVisible(true);
			if (chooser.getFile() != null) {
				try {
					String filename = chooser.getDirectory() + chooser.getFile();
					if (!filename.endsWith(".bed")) filename += ".bed";
					File bed = new File(filename);
					try (FileWriter fw = new FileWriter(bed)){
						for (Interval interval : selection){
							fw.write(interval.getChromosome() + "\t" + interval.getStart() + "\t" + interval.getEnd() + "\n");
						}
					}
				} catch (Exception ex) {
					Tools.exception(ex);
					JOptionPane.showMessageDialog(AskListOfIntervalsDialog.this, Tools.getMessage("Error", ex), 
							"Export intervals to a bed file", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}
			}
		}
	}

	public void saveList(){
		String name = ProfileTree.showProfileDialog(this, Action.SAVE, UserData.INTERVALS, reference.getName(), "Save list of intervals to your profile", listName);
		saveList(name);
	}

	public void saveList(String name){
		try {
			if (name == null) return;
			if (Highlander.getLoggedUser().doesPersonalDataExists(UserData.INTERVALS, reference.getName(), name)){
				int yesno = JOptionPane.showConfirmDialog(new JFrame(), 
						"You already have a "+UserData.INTERVALS.getName()+" named '"+name.replace("~", " -> ")+"', do you want to overwrite it ?", 
						"Overwriting element in your profile", JOptionPane.YES_NO_OPTION , JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iDbSave,64));
				if (yesno == JOptionPane.NO_OPTION)	return;
			}

			listName = name;
			if (createSelection()){
				Highlander.getLoggedUser().saveIntervals(listName, reference, selection);
			}
		} catch (Exception ex) {
			Tools.exception(ex);
			JOptionPane.showMessageDialog(AskListOfIntervalsDialog.this, Tools.getMessage("Error", ex), "Save current list of intervals in your profile", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
	}

	public void loadList(String key){
		listName = key;
		int row=0;
		while (table.getValueAt(row, 0) != null) row++;
		try {
			List<Interval> list = Highlander.getLoggedUser().loadIntervals(reference, listName);
			if (list.isEmpty()) {
				list = Highlander.getLoggedUser().loadIntervals(listName);
			}
			for (Interval item : list){
				table.setValueAt(item.getChromosome(), row, 0);
				table.setValueAt(item.getStart(), row, 1);
				table.setValueAt(item.getEnd(), row, 2);
				row++;
			}
		} catch (Exception ex) {
			Tools.exception(ex);
			JOptionPane.showMessageDialog(AskListOfIntervalsDialog.this, Tools.getMessage("Error", ex), 
					"Load list of intervals from your profile", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
		btnOk.setText(getValuesCount()+" interval(s)");
		table.revalidate();
	}

	public void loadList(Interval[] values){
		int row=0;
		while (table.getValueAt(row, 0) != null) row++;
		for (Interval item : values){
			table.setValueAt(item.getChromosome(), row, 0);
			table.setValueAt(item.getStart(), row, 1);
			table.setValueAt(item.getEnd(), row, 2);
			row++;
		}
		btnOk.setText(getValuesCount()+" interval(s)");
		table.revalidate();
	}

	public void sort(){
		if (createSelection()){
			Set<Interval> set = new TreeSet<Interval>(selection);
			for (int row = 0 ; row < table.getRowCount() ; row++){
				table.setValueAt(null, row, 0);
				table.setValueAt(null, row, 1);
				table.setValueAt(null, row, 2);
			}
			int row = 0;
			for (Interval item : set){
				table.setValueAt(item.getChromosome(), row, 0);
				table.setValueAt(item.getStart(), row, 1);
				table.setValueAt(item.getEnd(), row, 2);
				row++;
			}
			btnOk.setText(getValuesCount()+" interval(s)");
		}
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
