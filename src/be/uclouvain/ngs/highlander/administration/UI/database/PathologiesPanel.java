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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;

import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.misc.WrapLayout;
import be.uclouvain.ngs.highlander.administration.UI.AdministrationTableModel;
import be.uclouvain.ngs.highlander.administration.UI.ManagerPanel;
import be.uclouvain.ngs.highlander.administration.UI.ProjectManager;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;

/**
* @author Raphael Helaers
*/

public class PathologiesPanel extends ManagerPanel {
	
	private AdministrationTableModel pathologiesTableModel;
	private JTable pathologiesTable;

	public PathologiesPanel(ProjectManager manager){
		super(manager);
		
		pathologiesTable = new JTable(pathologiesTableModel){
			protected JTableHeader createDefaultTableHeader() {
				return new JTableHeader(columnModel) {
					public String getToolTipText(MouseEvent e) {
						java.awt.Point p = e.getPoint();
						int index = columnModel.getColumnIndexAtX(p.x);
						if (index >= 0){
							int realIndex = columnModel.getColumn(index).getModelIndex();
							return (table.getModel()).getColumnName(realIndex);
						}else{
							return null;
						}
					}
				};
			}
		};
		pathologiesTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		pathologiesTable.setCellSelectionEnabled(false);
		pathologiesTable.setRowSelectionAllowed(true);
		pathologiesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane scroll = new JScrollPane(pathologiesTable);
		add(scroll, BorderLayout.CENTER);

		fill();

		JPanel southPanel = new JPanel(new WrapLayout(WrapLayout.CENTER));
		add(southPanel, BorderLayout.SOUTH);

		JButton createNewButton = new JButton("Create new pathology", Resources.getScaledIcon(Resources.i3dPlus, 16));
		createNewButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						createPathology();
					}
				}, "PathologiesPanel.createPathology").start();

			}
		});
		southPanel.add(createNewButton);

		JButton renameButton = new JButton("Rename pathology", Resources.getScaledIcon(Resources.iUpdater, 16));
		renameButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						renamePathology(Integer.parseInt(pathologiesTable.getModel().getValueAt(pathologiesTable.getSelectedRow(), 0).toString()), 
								pathologiesTable.getModel().getValueAt(pathologiesTable.getSelectedRow(), 1).toString());
					}
				}, "PathologiesPanel.rename").start();

			}
		});
		southPanel.add(renameButton);

		JButton descriptionButton = new JButton("Set description", Resources.getScaledIcon(Resources.iUpdater, 16));
		descriptionButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						setDescription(Integer.parseInt(pathologiesTable.getModel().getValueAt(pathologiesTable.getSelectedRow(), 0).toString()), 
								pathologiesTable.getModel().getValueAt(pathologiesTable.getSelectedRow(), 1).toString(),
								pathologiesTable.getModel().getValueAt(pathologiesTable.getSelectedRow(), 2).toString());
					}
				}, "PathologiesPanel.setDescription").start();
				
			}
		});
		southPanel.add(descriptionButton);
		
		JButton deleteButton = new JButton("Delete pathology", Resources.getScaledIcon(Resources.iCross, 16));
		deleteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						deletePathology(Integer.parseInt(pathologiesTable.getModel().getValueAt(pathologiesTable.getSelectedRow(), 0).toString()), pathologiesTable.getModel().getValueAt(pathologiesTable.getSelectedRow(), 1).toString());
					}
				}, "PathologiesPanel.delete").start();

			}
		});
		southPanel.add(deleteButton);
	}

	private void fill(){
		try{
			String[] headers = new String[3+manager.getAvailableAnalysesAsArray().length];
			headers[0] = "id";
			headers[1] = "pathology";
			headers[2] = "description";
			for (int i=0 ; i < manager.getAvailableAnalysesAsArray().length ; i++){
				headers[i+3] = manager.getAvailableAnalysesAsArray()[i].toString();
			}
			Object[][] data;
			List<Object[]> arrayList = new ArrayList<Object[]>();
			try (Results res = DB.select(Schema.HIGHLANDER, "SELECT * FROM pathologies ORDER BY pathology")) {
				while(res.next()){
					Object[] array = new Object[headers.length];
					array[0] = res.getObject("pathology_id");
					array[1] = res.getObject("pathology");
					array[2] = res.getObject("pathology_description");
					arrayList.add(array);
				}
			}			
			data = new Object[arrayList.size()][headers.length];
			int row = 0;
			for (Object[] array : arrayList){
				data[row] = array;
				row++;
			}
			for (int i=3 ; i < headers.length ; i++){
				try (Results res = DB.select(Schema.HIGHLANDER, "SELECT pathology_id, COUNT(*) FROM projects JOIN projects_analyses USING (project_id) JOIN pathologies USING (pathology_id) WHERE analysis = '"+headers[i]+"' GROUP BY pathology_id")) {
					while(res.next()){
						int pathology_id = res.getInt(1);
						for (int j=0 ; j < data.length ; j++){
							if (Integer.parseInt(data[j][0].toString()) == pathology_id){
								data[j][i] = res.getInt(2);
								break;
							}
						}
					}
				}
			}
			pathologiesTableModel = new AdministrationTableModel(data, headers);
			pathologiesTable.setModel(pathologiesTableModel);
			refresh();
		}catch(Exception ex){
			Tools.exception(ex);
		}
	}

	private void refresh(){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				try{
					pathologiesTableModel.fireTableRowsUpdated(0,pathologiesTableModel.getRowCount()-1);
				}catch(Exception ex){
					Tools.exception(ex);
				}
			}
		});	
	}

	public void createPathology(){
		Object resu = JOptionPane.showInputDialog(this, "Pathology new name (alphanumeric caracters only and '_')", "Creating pathology", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.i3dPlus, 64), null, null);
		if (resu != null){
			String name = resu.toString();
			name = name.trim().replace(' ', '_').toUpperCase();
			Pattern pat = Pattern.compile("[^a-zA-Z0-9_]");
			if (pat.matcher(name).find()){
				JOptionPane.showMessageDialog(this, "Pathology name can only contain alphanumeric caracters and '_'", "Error", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
			}else{
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						waitingPanel.setVisible(true);
						waitingPanel.start();
					}
				});
				try{
					int count = 0;
					try (Results res = DB.select(Schema.HIGHLANDER, "SELECT COUNT(*) FROM pathologies WHERE pathology = '"+name+"'")) {
						if (res.next()){
							count = res.getInt(1);
						}
					}
					if (count > 0){
						JOptionPane.showMessageDialog(this, "Pathology already exists'", "Error", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
					}else if (name.length() > 1000){
						JOptionPane.showMessageDialog(this, "Pathology is limited to 1000 characters'", "Error", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
					}else{
						Object desc = JOptionPane.showInputDialog(this, "Pathology full description", "Creating pathology", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.i3dPlus, 64), null, null);
						if (desc != null){
							ProjectManager.toConsole("-----------------------------------------------------");
							ProjectManager.toConsole("Creating pathology " + name);
							DB.update(Schema.HIGHLANDER, "INSERT INTO pathologies (`pathology`,`pathology_description`) VALUES ('"+name+"','"+desc+"')");
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									fill();
								}
							});
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
	}
	
	public void renamePathology(int pathologyId, String pathology){
		Object nameAsk = JOptionPane.showInputDialog(this, "Pathology new name (alphanumeric caracters only and '_')", "Renaming pathology", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iUpdater, 64), null, pathology);
		if (nameAsk != null){
			String name = nameAsk.toString();
			name = name.trim().replace(' ', '_').toUpperCase();
			Pattern p = Pattern.compile("[^a-zA-Z0-9_]");
			if (p.matcher(name).find()){
				JOptionPane.showMessageDialog(this, "Pathology name can only contain alphanumeric caracters and '_'", "Error", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
			}else if (!pathology.equals(name)){
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						waitingPanel.setVisible(true);
						waitingPanel.start();
					}
				});
				try{
					int count = 0;
					try (Results res = DB.select(Schema.HIGHLANDER, "SELECT COUNT(*) FROM pathologies WHERE pathology = '"+name+"'")) {
						if (res.next()){
							count = res.getInt(1);
						}
					}
					if (count > 0){
						JOptionPane.showMessageDialog(this, "Pathology already exists'", "Error", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
					}else if (name.length() > 1000){
						JOptionPane.showMessageDialog(this, "Pathology is limited to 1000 characters'", "Error", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
					}else{
						ProjectManager.toConsole("-----------------------------------------------------");
						ProjectManager.toConsole("Renaming pathology " + pathology + " to "  + name);
						DB.update(Schema.HIGHLANDER, "UPDATE pathologies SET pathology = '"+name+"' WHERE pathology_id = "+pathologyId);
					}
				}catch(Exception ex){
					ProjectManager.toConsole(ex);
				}
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						fill();
					}
				});
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						waitingPanel.setVisible(false);
						waitingPanel.stop();
					}
				});
			}
		}
	}
	
	public void setDescription(int pathologyId, String pathology, String currentDescription){
		Object descriptionAsk = JOptionPane.showInputDialog(this, "Set description of " + pathology, "Set pathology description", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iUpdater, 64), null, currentDescription);
		if (descriptionAsk != null){
			String description = descriptionAsk.toString().trim();
			if (!currentDescription.equals(description)){
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						waitingPanel.setVisible(true);
						waitingPanel.start();
					}
				});
				try{
					ProjectManager.toConsole("-----------------------------------------------------");
					ProjectManager.toConsole("Set description of pathology " + pathology + " to '"  + description + "'");
					DB.update(Schema.HIGHLANDER, "UPDATE pathologies SET pathology_description = '"+description+"' WHERE pathology_id = "+pathologyId);
				}catch(Exception ex){
					ProjectManager.toConsole(ex);
				}
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						fill();
					}
				});
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						waitingPanel.setVisible(false);
						waitingPanel.stop();
					}
				});
			}
		}
	}
	
	public void deletePathology(int pathologyId, String pathology){
		try{
			int count = 0;
			try (Results res = DB.select(Schema.HIGHLANDER, "SELECT COUNT(*) FROM projects WHERE pathology_id = "+pathologyId)) {			
				if(res.next()){
					count += res.getInt(1);
				}
			}
			if (count == 0){
				int res = JOptionPane.showConfirmDialog(new JFrame(), "Are you SURE you want to delete pathology '"+pathology+"' ?", "Delete pathology", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				if (res == JOptionPane.YES_OPTION){
					ProjectManager.toConsole("-----------------------------------------------------");
					ProjectManager.toConsole("Deleting pathology " + pathology);
					DB.update(Schema.HIGHLANDER, "DELETE FROM pathologies WHERE pathology_id = "+pathologyId);
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							fill();
						}
					});
				}
			}else{
				JOptionPane.showMessageDialog(new JFrame(), count + " samples are still linked to this pathology.\nPlease first delete those samples or link them to another pathology.", "Delete pathology", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}
		}catch(Exception ex){
			ProjectManager.toConsole(ex);
		}
	}

}
