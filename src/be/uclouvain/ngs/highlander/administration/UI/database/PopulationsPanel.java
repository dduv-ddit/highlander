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

public class PopulationsPanel extends ManagerPanel {
	
	private AdministrationTableModel populationsTableModel;
	private JTable populationsTable;

	public PopulationsPanel(ProjectManager manager){
		super(manager);
		
		populationsTable = new JTable(populationsTableModel){
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
		populationsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		populationsTable.setCellSelectionEnabled(false);
		populationsTable.setRowSelectionAllowed(true);
		populationsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane scroll = new JScrollPane(populationsTable);
		add(scroll, BorderLayout.CENTER);

		fill();

		JPanel southPanel = new JPanel(new WrapLayout(WrapLayout.CENTER));
		add(southPanel, BorderLayout.SOUTH);

		JButton createNewButton = new JButton("Create new population", Resources.getScaledIcon(Resources.i3dPlus, 16));
		createNewButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						createPopulation();
					}
				}, "PopulationsPanel.createPopulation").start();

			}
		});
		southPanel.add(createNewButton);

		JButton renameButton = new JButton("Rename population", Resources.getScaledIcon(Resources.iUpdater, 16));
		renameButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						renamePopulation(Integer.parseInt(populationsTable.getModel().getValueAt(populationsTable.getSelectedRow(), 0).toString()), 
								populationsTable.getModel().getValueAt(populationsTable.getSelectedRow(), 1).toString());
					}
				}, "PopulationsPanel.rename").start();

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
						setDescription(Integer.parseInt(populationsTable.getModel().getValueAt(populationsTable.getSelectedRow(), 0).toString()), 
								populationsTable.getModel().getValueAt(populationsTable.getSelectedRow(), 1).toString(),
								populationsTable.getModel().getValueAt(populationsTable.getSelectedRow(), 2).toString());
					}
				}, "PopulationsPanel.setDescription").start();
				
			}
		});
		southPanel.add(descriptionButton);
		
		JButton deleteButton = new JButton("Delete population", Resources.getScaledIcon(Resources.iCross, 16));
		deleteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						deletePopulation(Integer.parseInt(populationsTable.getModel().getValueAt(populationsTable.getSelectedRow(), 0).toString()), populationsTable.getModel().getValueAt(populationsTable.getSelectedRow(), 1).toString());
					}
				}, "PopulationsPanel.deletePopulation").start();

			}
		});
		southPanel.add(deleteButton);
	}

	private void fill(){
		try{
			String[] headers = new String[3+manager.getAvailableAnalysesAsArray().length];
			headers[0] = "id";
			headers[1] = "population";
			headers[2] = "description";
			for (int i=0 ; i < manager.getAvailableAnalysesAsArray().length ; i++){
				headers[i+3] = manager.getAvailableAnalysesAsArray()[i].toString();
			}
			Object[][] data;
			List<Object[]> arrayList = new ArrayList<Object[]>();
			try (Results res = DB.select(Schema.HIGHLANDER, "SELECT * FROM populations ORDER BY population")) {
				while(res.next()){
					Object[] array = new Object[headers.length];
					array[0] = res.getObject("population_id");
					array[1] = res.getObject("population");
					array[2] = res.getObject("population_description");
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
				try (Results res = DB.select(Schema.HIGHLANDER, 
						"SELECT population_id, COUNT(*) FROM projects JOIN projects_analyses USING (project_id) JOIN populations USING (population_id) WHERE analysis = '"+headers[i]+"' GROUP BY population_id")) {
					while(res.next()){
						int population_id = res.getInt(1);
						for (int j=0 ; j < data.length ; j++){
							if (Integer.parseInt(data[j][0].toString()) == population_id){
								data[j][i] = res.getInt(2);
								break;
							}
						}
					}
				}
			}
			populationsTableModel = new AdministrationTableModel(data, headers);
			populationsTable.setModel(populationsTableModel);
			refresh();
		}catch(Exception ex){
			Tools.exception(ex);
		}
	}

	private void refresh(){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				try{
					populationsTableModel.fireTableRowsUpdated(0,populationsTableModel.getRowCount()-1);
				}catch(Exception ex){
					Tools.exception(ex);
				}
			}
		});	
	}

	public void createPopulation(){
		Object resu = JOptionPane.showInputDialog(this, "Population new name (alphanumeric caracters only and '_')", "Creating population", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.i3dPlus, 64), null, null);
		if (resu != null){
			String name = resu.toString();
			name = name.trim().replace(' ', '_').toUpperCase();
			Pattern pat = Pattern.compile("[^a-zA-Z0-9_]");
			if (pat.matcher(name).find()){
				JOptionPane.showMessageDialog(this, "Population name can only contain alphanumeric caracters and '_'", "Error", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
			}else{
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						waitingPanel.setVisible(true);
						waitingPanel.start();
					}
				});
				try{
					int count = 0;
					try (Results res = DB.select(Schema.HIGHLANDER, "SELECT COUNT(*) FROM populations WHERE population = '"+name+"'")) {
						if (res.next()){
							count = res.getInt(1);
						}
					}
					if (count > 0){
						JOptionPane.showMessageDialog(this, "Population already exists'", "Error", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
					}else if (name.length() > 1000){
						JOptionPane.showMessageDialog(this, "Population is limited to 1000 characters'", "Error", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
					}else{
						Object desc = JOptionPane.showInputDialog(this, "Population full description", "Creating population", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.i3dPlus, 64), null, null);
						if (desc != null){
							ProjectManager.toConsole("-----------------------------------------------------");
							ProjectManager.toConsole("Creating population " + name);
							DB.update(Schema.HIGHLANDER, "INSERT INTO populations (`population`,`population_description`) VALUES ('"+name+"','"+desc+"')");
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
	
	public void renamePopulation(int populationId, String population){
		Object nameAsk = JOptionPane.showInputDialog(this, "Population new name (alphanumeric caracters only and '_')", "Renaming population", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iUpdater, 64), null, population);
		if (nameAsk != null){
			String name = nameAsk.toString();
			name = name.trim().replace(' ', '_').toUpperCase();
			Pattern p = Pattern.compile("[^a-zA-Z0-9_]");
			if (p.matcher(name).find()){
				JOptionPane.showMessageDialog(this, "Population name can only contain alphanumeric caracters and '_'", "Error", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
			}else if (!population.equals(name)){
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						waitingPanel.setVisible(true);
						waitingPanel.start();
					}
				});
				try{
					int count = 0;
					try (Results res = DB.select(Schema.HIGHLANDER, "SELECT COUNT(*) FROM populations WHERE population = '"+name+"'")) {
						if (res.next()){
							count = res.getInt(1);
						}
					}
					if (count > 0){
						JOptionPane.showMessageDialog(this, "Population already exists'", "Error", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
					}else if (name.length() > 1000){
						JOptionPane.showMessageDialog(this, "Population is limited to 1000 characters'", "Error", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
					}else{
						ProjectManager.toConsole("-----------------------------------------------------");
						ProjectManager.toConsole("Renaming population " + population + " to "  + name);
						DB.update(Schema.HIGHLANDER, "UPDATE populations SET population = '"+name+"' WHERE population_id = "+populationId);
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
	
	public void setDescription(int populationId, String population, String currentDescription){
		Object descriptionAsk = JOptionPane.showInputDialog(this, "Set description of " + population, "Set population description", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iUpdater, 64), null, currentDescription);
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
					ProjectManager.toConsole("Set description of population " + population + " to '"  + description + "'");
					DB.update(Schema.HIGHLANDER, "UPDATE populations SET population_description = '"+description+"' WHERE population_id = "+populationId);
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
	
	public void deletePopulation(int populationId, String population){
		try{
			int count = 0;
			try (Results res = DB.select(Schema.HIGHLANDER, "SELECT COUNT(*) FROM projects WHERE population_id = "+populationId)) {			
				if(res.next()){
					count += res.getInt(1);
				}
			}
			if (count == 0){
				int res = JOptionPane.showConfirmDialog(new JFrame(), "Are you SURE you want to delete population '"+population+"' ?", "Delete population", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				if (res == JOptionPane.YES_OPTION){
					ProjectManager.toConsole("-----------------------------------------------------");
					ProjectManager.toConsole("Deleting population " + population);
					DB.update(Schema.HIGHLANDER, "DELETE FROM populations WHERE population_id = "+populationId);
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							fill();
						}
					});
				}
			}else{
				JOptionPane.showMessageDialog(new JFrame(), count + " samples are still linked to this population.\nPlease first delete those samples or link them to another population.", "Delete population", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}
		}catch(Exception ex){
			ProjectManager.toConsole(ex);
		}
	}

}
