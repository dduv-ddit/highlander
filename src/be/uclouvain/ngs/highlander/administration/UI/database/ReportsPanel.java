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
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.administration.UI.ManagerPanel;
import be.uclouvain.ngs.highlander.administration.UI.ProjectManager;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.Report;

/**
* @author Raphael Helaers
*/

public class ReportsPanel extends ManagerPanel {
	
	private DefaultListModel<Report> listReportsModel = new DefaultListModel<>();
	private JList<Report> listReports = new JList<>(listReportsModel);
	private JTextArea txtAreaDescription = new JTextArea();
	private JTextField txtFieldPath = new JTextField();
	private JScrollPane scrollFileExtensions = new JScrollPane();
	private Map<JTextField, String> fileExtensions = new HashMap<>();
	private JScrollPane scrollAnalyses = new JScrollPane();
	
	public ReportsPanel(ProjectManager manager){
		super(manager);

		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.2);
		add(splitPane, BorderLayout.CENTER);

		JPanel panel_left = new JPanel(new BorderLayout(5,5));
		panel_left.setBorder(BorderFactory.createTitledBorder("Available software"));
		splitPane.setLeftComponent(panel_left);
		
		JScrollPane scrollPane_left = new JScrollPane();
		panel_left.add(scrollPane_left, BorderLayout.CENTER);

		try {
			for (Report rep : Report.getAvailableReports()) {
				listReportsModel.addElement(rep);
			}
		}catch(Exception ex) {
			ProjectManager.toConsole(ex);
		}
		listReports.setFixedCellHeight(20); //To avoid a 'random bug' to sometimes (super) oversize one cell
		listReports.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listReports.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					fill(listReports.getSelectedValue());
				}
			}
		});
		scrollPane_left.setViewportView(listReports);
		
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
				Report report = listReports.getSelectedValue();
				report.setDescription(txtAreaDescription.getText());
				try {
					report.update();
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
		
		JPanel panel_path = new JPanel(new GridBagLayout());
		panel_path.setBorder(new EmptyBorder(0, 0, 5, 0));
		JLabel lbl_path = new JLabel("Path");
		lbl_path.setToolTipText("<html>Highlander will look for files of this software in [protocol]://[host]/[reports]/[run_id]_[run_date]_[run_name]/<b>[path]</b>/[sample]</html>");
		panel_path.add(lbl_path, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
		lbl_path.setToolTipText("<html>Highlander will look for files of this software in [protocol]://[host]/[reports]/[run_id]_[run_date]_[run_name]/<b>[path]</b>/[sample]</html>");
		txtFieldPath.setInputVerifier(new InputVerifier() {
			@Override
			public boolean verify(JComponent input) {
				Report report = listReports.getSelectedValue();
				report.setPath(txtFieldPath.getText());
				try {
					report.update();
				}catch(Exception ex) {
					ProjectManager.toConsole(ex);
					txtFieldPath.setForeground(Color.RED);
					return false;
				}
				txtFieldPath.setForeground(Color.BLACK);
				return true;
			}
		});
		panel_path.add(txtFieldPath, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 5, 0, 5), 0, 0));
		panel_right_north.add(panel_path, BorderLayout.SOUTH);
		
		JPanel panel_anal_files = new JPanel(new BorderLayout());
		panel_anal_files.setBorder(new EmptyBorder(0, 0, 0, 5));
		panel_right.add(panel_anal_files, BorderLayout.CENTER);
		
		JPanel panel_analyses = new JPanel(new BorderLayout());
		panel_analyses.setBorder(BorderFactory.createTitledBorder("Analyses"));
		scrollAnalyses.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		panel_analyses.add(scrollAnalyses, BorderLayout.CENTER);
		panel_anal_files.add(panel_analyses, BorderLayout.WEST);
		
		JPanel panel_files = new JPanel(new BorderLayout());
		panel_files.setBorder(BorderFactory.createTitledBorder("Files"));
		scrollFileExtensions.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		panel_files.add(scrollFileExtensions, BorderLayout.CENTER);
		panel_anal_files.add(panel_files, BorderLayout.CENTER);
		
		JPanel panel_south = new JPanel();
		add(panel_south, BorderLayout.SOUTH);
		
		JButton createNewButton = new JButton("Add software", Resources.getScaledIcon(Resources.i3dPlus, 16));
		createNewButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						createReport();
					}
				}, "ReportsPanel.createReport").start();

			}
		});
		panel_south.add(createNewButton);

		JButton renameButton = new JButton("Rename software", Resources.getScaledIcon(Resources.iUpdater, 16));
		renameButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						renameReport(listReports.getSelectedValue());
					}
				}, "ReportsPanel.listReports").start();

			}
		});
		panel_south.add(renameButton);

		JButton deleteButton = new JButton("Delete software", Resources.getScaledIcon(Resources.iCross, 16));
		deleteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						deleteReport(listReports.getSelectedValue());
					}
				}, "ReportsPanel.deleteReport").start();

			}
		});
		panel_south.add(deleteButton);
		
		listReports.setSelectedIndex(0);
	}
		
	public void fill() {
		fill(listReports.getSelectedValue());
	}
	
	private void fill(Report report) {
		scrollAnalyses.setViewportView(null);
		scrollFileExtensions.setViewportView(null);
		txtAreaDescription.setText("");
		txtFieldPath.setText("");
		if (report != null) {
			txtAreaDescription.setText(report.getDescription());
			txtFieldPath.setText(report.getPath());
			JPanel panel_analyses = new JPanel(new GridBagLayout());
			int row = 0;
			for (final Analysis analysis : manager.getAvailableAnalysesAsArray()) {
				JCheckBox box = new JCheckBox(analysis.toString());
				box.setSelected(report.getAnalyses().contains(analysis));
				box.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(ItemEvent e) {
						if (e.getStateChange() == ItemEvent.SELECTED) {
							report.addAnalysis(analysis);
						}else {
							report.removeAnalysis(analysis);
						}
						try {
							report.update();
						}catch(Exception ex) {
							ProjectManager.toConsole(ex);
						}
					}
				});
				panel_analyses.add(box, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
				row++;
			}
			panel_analyses.add(new JPanel(), new GridBagConstraints(0, row, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
			scrollAnalyses.setViewportView(panel_analyses);
			JPanel panel_schemas_in = new JPanel(new GridBagLayout());
			row = 0;
			JButton buttonAdd = new JButton("Add file", Resources.getScaledIcon(Resources.i3dPlus, 18));
			buttonAdd.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					report.addFile("", "");
					try {
						report.update();
					}catch(Exception ex) {
						ProjectManager.toConsole(ex);
					}
					fill(report);
				}
			});
			panel_schemas_in.add(buttonAdd, new GridBagConstraints(0, row++, 3, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
			for (final String file : report.getFiles()) {
				JButton button = new JButton(Resources.getScaledIcon(Resources.iCross, 18));
				JLabel lbl_ext = new JLabel("Extension");
				JTextField txt_ext = new JTextField(file);
				fileExtensions.put(txt_ext, file);
				txt_ext.setColumns(15);
				JLabel lbl_desc = new JLabel("Description");
				JTextArea txt_desc = new JTextArea(report.getFileDescription(file));
				txt_desc.setColumns(30);
				txt_desc.setRows(3);
				//Listen to modifications
				button.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						report.removeFile(fileExtensions.get(txt_ext));
						try {
							report.update();
						}catch(Exception ex) {
							ProjectManager.toConsole(ex);
						}
						fill(report);
					}
				});
				txt_ext.setInputVerifier(new InputVerifier() {
					@Override
					public boolean verify(JComponent input) {
						report.removeFile(fileExtensions.get(txt_ext));
						report.addFile(txt_ext.getText(), txt_desc.getText());
						try {
							report.update();
						}catch(Exception ex) {
							ProjectManager.toConsole(ex);
							txt_ext.setForeground(Color.RED);
							return false;
						}
						fileExtensions.put(txt_ext, txt_ext.getText());
						txt_ext.setForeground(Color.BLACK);
						return true;
					}
				});
				txt_desc.setInputVerifier(new InputVerifier() {
					@Override
					public boolean verify(JComponent input) {
						report.setFileDescription(txt_ext.getText(), txt_desc.getText());
						try {
							report.update();
						}catch(Exception ex) {
							ProjectManager.toConsole(ex);
							txt_desc.setForeground(Color.RED);
							return false;
						}
						txt_desc.setForeground(Color.BLACK);
						return true;
					}
				});
				panel_schemas_in.add(button, new GridBagConstraints(0, row, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(7, 5, 5, 5), 0, 0));
				panel_schemas_in.add(lbl_ext, new GridBagConstraints(1, row, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(12, 5, 5, 5), 0, 0));
				panel_schemas_in.add(txt_ext, new GridBagConstraints(2, row, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(7, 5, 5, 5), 0, 0));
				panel_schemas_in.add(lbl_desc, new GridBagConstraints(3, row, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(12, 5, 5, 5), 0, 0));
				panel_schemas_in.add(txt_desc, new GridBagConstraints(4, row, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
				row++;
			}
			panel_schemas_in.add(new JPanel(), new GridBagConstraints(0, row, 3, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
			scrollFileExtensions.setViewportView(panel_schemas_in);
		}
	}
	
	public void createReport(){
		Object resu = JOptionPane.showInputDialog(this, "Software name", "Add software", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.i3dPlus, 64), null, null);
		if (resu != null){
			String software = resu.toString();
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(true);
					waitingPanel.start();
				}
			});
			try{
				int count = 0;
				try (Results res = DB.select(Schema.HIGHLANDER, "SELECT COUNT(*) FROM `reports` WHERE `software` = '"+software+"'")) {
					if (res.next()){
						count = res.getInt(1);
					}
				}
				if (count > 0){
					JOptionPane.showMessageDialog(this, "Software already exists'", "Error", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
				}else if (software.length() > 255){
					JOptionPane.showMessageDialog(this, "Software name is limited to 255 characters'", "Error", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
				}else{
							ProjectManager.toConsole("-----------------------------------------------------");
							ProjectManager.toConsole("Creating software reports for " + software);
							Report report = new Report();
							report.setSoftware(software);
							report.insert();
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									listReportsModel.addElement(report);
									listReports.setSelectedValue(report, true);
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
	
	public void renameReport(Report report){
		Object resu = JOptionPane.showInputDialog(this, "Software name", "Renaming software", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iUpdater, 64), null, report);
		if (resu != null){
			String software = resu.toString();
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(true);
					waitingPanel.start();
				}
			});
			try{
				int count = 0;
				try (Results res = DB.select(Schema.HIGHLANDER, "SELECT COUNT(*) FROM `reports` WHERE `software` = '"+software+"'")) {
					if (res.next()){
						count = res.getInt(1);
					}
				}
				if (count > 0){
					JOptionPane.showMessageDialog(this, "Software name already exists'", "Error", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
				}else if (software.length() > 255){
					JOptionPane.showMessageDialog(this, "Software name is limited to 255 characters'", "Error", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross, 64));
				}else{
					ProjectManager.toConsole("-----------------------------------------------------");
					ProjectManager.toConsole("Renaming software " + report.getSoftware() + " to "  + software);
					report.setSoftware(software);
					report.update();
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

	public void deleteReport(Report report){
		try{
				int res = JOptionPane.showConfirmDialog(new JFrame(), "Are you SURE you want to delete software reports of '"+report+"' ?", "Delete software reports", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				if (res == JOptionPane.YES_OPTION){
					ProjectManager.toConsole("-----------------------------------------------------");
					ProjectManager.toConsole("Deleting software reports of " + report);
					report.delete();
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							listReports.clearSelection();
							listReportsModel.removeElement(report);
							listReports.setSelectedIndex(0);
						}
					});
				}
		}catch(Exception ex){
			ProjectManager.toConsole(ex);
		}
	}

}
