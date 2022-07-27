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

package be.uclouvain.ngs.highlander.administration.UI.projects;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;

import java.awt.BorderLayout;

import javax.swing.JButton;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JScrollPane;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Toolkit;

import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.UI.misc.SearchField;
import be.uclouvain.ngs.highlander.UI.misc.WaitingPanel;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;

public class AskNormalSampleDialog extends JDialog {

	private DefaultTableModel tSourceModel;
	private JTable tableSource;
	private TableRowSorter<DefaultTableModel> sorter;
	private SearchField	searchField = new SearchField(10);

	static private WaitingPanel waitingPanel;

	public AskNormalSampleDialog() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int width = screenSize.width - (screenSize.width/5);
		int height = screenSize.height - (screenSize.height/3);
		setSize(new Dimension(width,height));
		initUI();
		this.addComponentListener(new ComponentListener() {
			@Override
			public void componentShown(ComponentEvent arg0) {
				updateSourceTable();				
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

	private void initUI(){
		setModal(true);
		setTitle("Select a normal sample");
		setIconImage(Resources.getScaledIcon(Resources.iPatients, 64).getImage());

		StringBuilder sb = new StringBuilder();
		sb.append("Only fill this column for somatic samples for which you have also sequenced a germline sample from the same individual.\n");
		sb.append("If you are creating a new run and normal sample is there, you must first validate modification (to have an id).\n");
		sb.append("Variant callers like MuTect need the pair information.\n");
		sb.append("Note that you need only to set the normal sample in this column for each somatic sample,\n");
		sb.append("not the other way around, and it can stay empty if a pair has not been sequenced.");
		JTextArea label = new JTextArea(sb.toString());
		label.setWrapStyleWord(true);
		label.setEditable(false);
		getContentPane().add(label, BorderLayout.NORTH);
		
		JPanel panel = new JPanel();	
		getContentPane().add(panel, BorderLayout.SOUTH);

		JButton btnOk = new JButton(Resources.getScaledIcon(Resources.iButtonApply, 24));
		btnOk.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {				
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

		JPanel panel_0 = new JPanel(new BorderLayout(0,0));
		panel_0.setBorder(BorderFactory.createTitledBorder("Available samples"));		
		GridBagConstraints gbc_scrollPaneSource = new GridBagConstraints();
		gbc_scrollPaneSource.weighty = 1.0;
		gbc_scrollPaneSource.weightx = 1.0;
		gbc_scrollPaneSource.insets = new Insets(5, 5, 5, 5);
		gbc_scrollPaneSource.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneSource.gridx = 0;
		gbc_scrollPaneSource.gridy = 0;
		getContentPane().add(panel_0, BorderLayout.CENTER);

		JScrollPane scrollPaneSource = new JScrollPane();
		panel_0.add(scrollPaneSource, BorderLayout.CENTER);

		tableSource = new JTable(){
			public boolean isCellEditable(int row, int column){
				return false;
			}
		};
		tableSource.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tableSource.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount() == 2){
					dispose();
				}
			}
		});
		scrollPaneSource.setViewportView(tableSource);

		JPanel panel_filters = new JPanel(new BorderLayout());
		panel_0.add(panel_filters, BorderLayout.NORTH);

		panel_filters.add(searchField, BorderLayout.CENTER);

		waitingPanel = new WaitingPanel();
		setGlassPane(waitingPanel);
	}	

	private void updateSourceTable(){
		String[] headers = new String[]{
				"project_id",
				"sample",
				"run_id",
				"run_date",
				"run_name",
				"platform",
				"outsourcing",
				"sequencing_target",
				"pathology",
				"sample_type",
				"comments",
		};
		List<Object[]> arrayList = new ArrayList<Object[]>();
		try(Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT * FROM projects JOIN pathologies USING (pathology_id)")){
			while(res.next()){
				Object[] array = new Object[headers.length];
				for (int col=0 ; col < headers.length ; col++){
					array[col] = res.getObject(headers[col]);
				}
				arrayList.add(array);
			}
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		Object[][] data = new Object[arrayList.size()][headers.length];
		int row = 0;
		for (Object[] array : arrayList){
			data[row] = array;
			row++;
		}
		tSourceModel = new DefaultTableModel(data, headers);
		sorter = new TableRowSorter<DefaultTableModel>(tSourceModel);
		tableSource.setModel(tSourceModel);		
		tableSource.setRowSorter(sorter);
		searchField.setSorter(sorter);
		searchField.applyFilter();		
	}

	public int getSelectedId(){
		return Integer.parseInt(tableSource.getValueAt(tableSource.getSelectedRow(), 0).toString());
	}

	public String getSelectedSample(){
		return tableSource.getValueAt(tableSource.getSelectedRow(), 1).toString();
	}
	
	public boolean isSampleSelected() {
		return tableSource.getSelectedColumn() >= 0;
	}
	
	//Overridden so we can exit when window is closed
	protected void processWindowEvent(WindowEvent e) {
		super.processWindowEvent(e);
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			cancelClose();
		}
	}

	private void cancelClose(){
		tableSource.clearSelection();
		dispose();
	}

}
