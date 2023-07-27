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

package be.uclouvain.ngs.highlander.administration.UI.client;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.dialog.CreateColumnSelection;
import be.uclouvain.ngs.highlander.UI.misc.WrapLayout;
import be.uclouvain.ngs.highlander.administration.UI.ManagerPanel;
import be.uclouvain.ngs.highlander.administration.UI.ProjectManager;
import be.uclouvain.ngs.highlander.administration.users.User;
import be.uclouvain.ngs.highlander.administration.users.User.Settings;
import be.uclouvain.ngs.highlander.administration.users.User.UserData;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;

/**
* @author Raphael Helaers
*/

public class DefaultColumnsPanel extends ManagerPanel {
	
	private DefaultListModel<AnalysisFull> listAnalysesModel = new DefaultListModel<>();
	private JList<AnalysisFull> listAnalyses = new JList<>(listAnalysesModel);
	private JScrollPane scrollData = new JScrollPane();

	public DefaultColumnsPanel(ProjectManager manager){
		super(manager);
		
		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.2);
		add(splitPane, BorderLayout.CENTER);

		JPanel panel_left = new JPanel(new BorderLayout(5,5));
		panel_left.setBorder(BorderFactory.createTitledBorder("Analyses"));
		splitPane.setLeftComponent(panel_left);
		
		JScrollPane scrollPane_left = new JScrollPane();
		panel_left.add(scrollPane_left, BorderLayout.CENTER);

		for (AnalysisFull analysis : manager.getAvailableAnalysesAsArray()) {
			listAnalysesModel.addElement(analysis);
		}
		listAnalyses.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listAnalyses.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					fill(listAnalyses.getSelectedValue());
				}
			}
		});
		scrollPane_left.setViewportView(listAnalyses);
			
		JPanel panel_right = new JPanel(new BorderLayout());
		panel_right.setBorder(new EmptyBorder(10, 5, 0, 5));
		splitPane.setRightComponent(panel_right);
		
		panel_right.add(scrollData, BorderLayout.CENTER);
		
		JPanel southPanel = new JPanel(new WrapLayout(FlowLayout.CENTER));
		add(southPanel, BorderLayout.SOUTH);

		JButton createNewButton = new JButton("Set default columns", Resources.getScaledIcon(Resources.iColumnSelectionNew, 16));
		createNewButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						setDefaultColumns(listAnalyses.getSelectedValue());
					}
				}, "AnalysesPanel.setDefaultColumns").start();

			}
		});
		southPanel.add(createNewButton);

		JButton duplicateButton = new JButton("Duplicate default column set", Resources.getScaledIcon(Resources.iCopy, 16));
		duplicateButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						Object from = JOptionPane.showInputDialog(manager,  "Select the analysis FROM which the column set will be duplicated", "Duplicate default column set",
								JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), manager.getAvailableAnalysesAsArray(), null);
						if (from != null){
							Object to = JOptionPane.showInputDialog(manager,  "Select the analysis TO which the column set will be applyied", "Duplicate default column set",
									JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), manager.getAvailableAnalysesAsArray(), null);
							if (to != null){
								duplicateColumns((AnalysisFull)from, (AnalysisFull)to);
							}
						}
					}
				}, "AnalysesPanel.duplicateColumns").start();
				
			}
		});
		southPanel.add(duplicateButton);
		
		listAnalyses.setSelectedIndex(0);
	}
	
	public void fill(){
		fill(listAnalyses.getSelectedValue());
	}

	private void fill(AnalysisFull analysis){
		scrollData.setViewportView(null);
		if (analysis != null) {
			List<String> strings = new ArrayList<>();
			try {
				strings = User.loadDefaultSettings(analysis.toString(), Settings.DEFAULT_COLUMNS);
			}catch(Exception ex){
				ProjectManager.toConsole(ex);			
			}
			Field[] fields = new Field[strings.size()];
			for (int i=0 ; i < strings.size() ; i++) {
				Field field = Field.getField(strings.get(i));
				fields[i] = field;
			}
			JList<Field> fieldsList = new JList<>(fields);
			scrollData.setViewportView(fieldsList);
		}
	}

	public void setDefaultColumns(AnalysisFull analysis){
		CreateColumnSelection ccs = new CreateColumnSelection(analysis, UserData.SETTINGS, Field.getAvailableFields(analysis, true));
		Tools.centerWindow(ccs, false);
		ccs.setVisible(true);
		if(!ccs.getSelection().isEmpty()){		
			try{
				List<String> strings = new ArrayList<>();
				for (Field field : ccs.getSelection()) {
					strings.add(field.getName());
				}
				User.saveDefaultSettings(analysis.toString(), Settings.DEFAULT_COLUMNS, strings);
				fill();
			}catch(Exception ex){
				Tools.exception(ex);
				JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Can't save column selection list to your profile", ex), "Saving column selection list to your profile",
						JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}
		}
	}

	public void duplicateColumns(Analysis from, Analysis to) {
		int res = JOptionPane.showConfirmDialog(new JFrame(), "Are you SURE you want to duplicate default columns from '"+from+"' to '"+to+"' ?", "Duplicate default column set", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iCopy,64));
		if (res == JOptionPane.CANCEL_OPTION){
			return;
		}else if (res == JOptionPane.YES_OPTION){
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					waitingPanel.setVisible(true);
					waitingPanel.start();
				}
			});
			try {
				Highlander.getDB().update(Schema.HIGHLANDER, "DELETE FROM users_data WHERE `key` = 'DEFAULT_COLUMNS' AND `analysis` = '"+to+"'");
				Highlander.getDB().update(Schema.HIGHLANDER, "INSERT INTO users_data (`username`, `type`, `analysis`, `key`, `value`) SELECT `username`, `type`, '"+to+"', `key`, `value` FROM Highlander.users_data WHERE `key` = 'DEFAULT_COLUMNS' AND `analysis` = '"+from+"' ORDER by id");
			}catch (Exception ex) {
				ProjectManager.toConsole(ex);
			}
			listAnalyses.setSelectedValue(to, true);
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					waitingPanel.setVisible(false);
					waitingPanel.stop();
				}
			});
		}

	}
}
