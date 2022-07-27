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

package be.uclouvain.ngs.highlander.administration.iontorrent;


import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import java.awt.BorderLayout;

import javax.swing.JButton;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
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

import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.JTextComponent;

import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;


public class AskGeneList extends JDialog  {
	private String listName = null;
	private List<String> selection = new ArrayList<String>();
	private IncrementableTableModel tmodel;
	private JTable table;
	private JButton btnOk;
	
	public AskGeneList(Set<String> startSelection) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int width = screenSize.width - (screenSize.width/3*2);
		int height = screenSize.height - (screenSize.height/3);
		setSize(new Dimension(width,height));
		initUI();
		int row=0;
		for (String value : startSelection){
			table.setValueAt(value, row++, 0);
		}
	}
	
	private void initUI(){
		setModal(true);
		setTitle("Create gene list");
		setIconImage(Resources.getScaledIcon(Resources.iList, 64).getImage());
		
		JPanel panel_2 = new JPanel();
		getContentPane().add(panel_2, BorderLayout.NORTH);
		
		JButton btnFile = new JButton(Resources.getScaledIcon(Resources.iImportFile, 40));
		btnFile.setToolTipText("Import genes from a text file");
		btnFile.setPreferredSize(new Dimension(54,54));
		btnFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				FileDialog chooser = new FileDialog(AskGeneList.this, "Choose a plain text file containing 1 gene per line", FileDialog.LOAD);
				chooser.setVisible(true);
				if (chooser.getFile() != null) {
		      try {
						File file = new File(chooser.getDirectory() + chooser.getFile());
						FileReader fr = new FileReader(file);
						BufferedReader br = new BufferedReader(fr);
						String line;
						int row=0;
						while (table.getValueAt(row, 0) != null) row++;
						while ((line = br.readLine()) != null){
							table.setValueAt(line, row++, 0);
						}
						br.close();
						fr.close();
						tmodel.fireTableDataChanged();
					} catch (Exception ex) {
						Tools.exception(ex);
						JOptionPane.showMessageDialog(AskGeneList.this, Tools.getMessage("Error", ex), 
								"Import genes from a text file", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
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
			public void actionPerformed(ActionEvent arg0) {
				sort();
			}
		});
		panel_2.add(btnSort);
		
		JPanel panel = new JPanel();	
		getContentPane().add(panel, BorderLayout.SOUTH);
		
		btnOk = new JButton(Resources.getScaledIcon(Resources.iButtonApply, 24));
		btnOk.setText("0 values");
		btnOk.addActionListener(new ActionListener() {
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
		table.setTableHeader(null);
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
		scrollPane.setViewportView(table);
		
	}
	
	public static class IncrementableTableModel	extends AbstractTableModel {
		private List<Object[]> data;
		private final int ncol = 1;
		
		public IncrementableTableModel() {   
			data = new ArrayList<Object[]>();
			data.add(new Object[ncol]);
		}
		
		public int getColumnCount() {
			return ncol;
		}
		
		public int getRowCount() {
			return data.size();
		}
		
		public Class<?> getColumnClass(int columnIndex) {
			return Object.class;
		}
		
		public Object getValueAt(int row, int col) {
			return data.get(row)[col];
		}
		
		public void setValueAt(Object value, int row, int col) {
			data.get(row)[col] = value;
			if (!isLastLineEmpty()){
				data.add(new Object[ncol]);
			}
			fireTableCellUpdated(row, col);			
		}
		
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
	
	public String getListName(){
		return listName;
	}
	
	public void loadList(String[] values){
		int row=0;
		while (table.getValueAt(row, 0) != null) row++;
		for (String item : values){
			table.setValueAt(item, row++, 0);
		}
    btnOk.setText(getValuesCount()+" values");
	}
	
	public void sort(){
		Set<String> set = new TreeSet<String>(new Tools.NaturalOrderComparator(true));
		for (int row = 0 ; row < table.getRowCount() ; row++){
			if (table.getValueAt(row, 0) != null && table.getValueAt(row, 0).toString().length() > 0){ 
				set.add(table.getValueAt(row, 0).toString().trim());
				table.setValueAt(null, row, 0);
			}
		}
		int row = 0;
		for (String item : set){
			table.setValueAt(item, row++, 0);
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
