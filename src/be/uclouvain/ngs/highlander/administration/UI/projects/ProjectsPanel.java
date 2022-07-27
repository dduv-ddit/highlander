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

package be.uclouvain.ngs.highlander.administration.UI.projects;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.dialog.AskUsersDialog;
import be.uclouvain.ngs.highlander.UI.misc.WrapLayout;
import be.uclouvain.ngs.highlander.administration.DbBuilder;
import be.uclouvain.ngs.highlander.administration.DbBuilder.CoverageTarget;
import be.uclouvain.ngs.highlander.administration.UI.AdministrationTableModel;
import be.uclouvain.ngs.highlander.administration.UI.ManagerPanel;
import be.uclouvain.ngs.highlander.administration.UI.ProjectManager;
import be.uclouvain.ngs.highlander.administration.UI.Sample;
import be.uclouvain.ngs.highlander.administration.users.User;
import be.uclouvain.ngs.highlander.database.DBUtils;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.Field.SampleType;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.swing.AutoCompleteSupport;

/**
* @author Raphael Helaers
*/

public class ProjectsPanel extends ManagerPanel {

	private String[] availableProjects;
	private EventList<String> projectsList;
	private JComboBox<String> projectBox;
	AutoCompleteSupport<String> projectBoxSupport;
	private AdministrationTableModel projectTableModel;
	private JTable projectsTable;

	private JComboBox<AnalysisFull> importAnalysisBox;
	private Map<String, JTextField> importVCFs = new LinkedHashMap<>();
	private Map<String, JTextField> importAlamuts = new LinkedHashMap<>();
	private JCheckBox importOverwrite;

	private JComboBox<AnalysisFull> importCoverageDetailsBox;
	private JComboBox<CoverageTarget> importCoverageDetailsTargetBox;
	private Map<String, JTextField> importCoverageDetailsThresholdsFiles = new LinkedHashMap<>();
	private Map<String, JTextField> importCoverageDetailsRegionsFiles = new LinkedHashMap<>();
	
	//Run path is not imported anymore through those files, since it moved to projects_analyses
	//private JCheckBox importFastqcUpdateRunpath;
	private Map<String, JTextField> importFastqcFiles = new LinkedHashMap<>();

	//private JCheckBox importCoverageWDUpdateRunpath;
	private Map<String, JTextField> importCoverageWDFiles = new LinkedHashMap<>();

	//private JCheckBox importCoverageWODUpdateRunpath;
	private Map<String, JTextField> importCoverageWODFiles = new LinkedHashMap<>();

	//private JCheckBox importCoverageExomeUpdateRunpath;
	private Map<String, JTextField> importCoverageExomeFiles = new LinkedHashMap<>();

	public ProjectsPanel(ProjectManager manager){
		super(manager);
		try {
			availableProjects = manager.listProjects();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		projectsList = GlazedLists.eventListOf(availableProjects);

		JPanel northPanel = new JPanel(new BorderLayout());
		add(northPanel, BorderLayout.NORTH);

		projectBox = new JComboBox<>(availableProjects);
		projectBox.setMaximumRowCount(20);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				projectBoxSupport = AutoCompleteSupport.install(projectBox, projectsList);
				projectBoxSupport.setCorrectsCase(true);
				projectBoxSupport.setFilterMode(TextMatcherEditor.CONTAINS);
				projectBoxSupport.setTextMatchingStrategy(TextMatcherEditor.NORMALIZED_STRATEGY);
				projectBoxSupport.setStrict(false);
				//Only trick I've found to avoid oversized rows in the combobox list 
				projectBox.setSelectedItem(0);
				projectBox.setSelectedItem(null);
			}
		});
		projectBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (arg0.getActionCommand().equals("comboBoxEdited")){
					if (projectBox.getSelectedIndex() < 0) projectBox.setSelectedItem(null);
				}
			}
		});
		projectBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					if (projectBox.getSelectedIndex() >= 0) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								fill(projectBox.getSelectedItem().toString());
							}
						});
					}
				}
			}
		});
		northPanel.add(projectBox, BorderLayout.NORTH);

		JPanel fillToolsPanel = new JPanel(new WrapLayout(WrapLayout.LEFT));
		northPanel.add(fillToolsPanel, BorderLayout.SOUTH);

		JButton setRunIdButton = new JButton("Set run id");
		setRunIdButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object res = JOptionPane.showInputDialog(manager,  "Set the run id (mandatory): it must be a positive number", "Run id",
						JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), null, null);
				if (res != null){
					try{
						int item = Integer.parseInt(res.toString());
						if (item < 0) throw new NumberFormatException();
						for (int row=0 ; row < projectsTable.getRowCount() ; row++){
							projectsTable.setValueAt(item, row, projectsTable.convertColumnIndexToView(projectTableModel.getColumn("run_id")));
						}
						refresh();
					}catch(NumberFormatException ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(new JFrame(), "You must enter a valid number", "Run id", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			}
		});
		fillToolsPanel.add(setRunIdButton);

		JButton setRunDateButton = new JButton("Set run date");
		setRunDateButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object res = JOptionPane.showInputDialog(manager,  "Set the run date in this format (mandatory): YYYY-MM-DD", "Run date",
						JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), null, null);
				if (res != null){
					String item = res.toString();
					if (item.length() != 10 || 
							item.split("-").length != 3 || 
							Integer.parseInt(item.split("-")[0]) < 2000 || 
							Integer.parseInt(item.split("-")[0]) > 2500 || 
							Integer.parseInt(item.split("-")[1]) < 1 || 
							Integer.parseInt(item.split("-")[1]) > 12 || 
							Integer.parseInt(item.split("-")[2]) < 1 || 
							Integer.parseInt(item.split("-")[2]) > 31 
							){
						JOptionPane.showMessageDialog(new JFrame(), "You must enter a valid date in the format YYYY-MM-DD", "Run date", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));						
					}else{
						for (int row=0 ; row < projectsTable.getRowCount() ; row++){
							projectsTable.setValueAt(item, row, projectsTable.convertColumnIndexToView(projectTableModel.getColumn("run_date")));
						}
						refresh();
					}
				}
			}
		});
		fillToolsPanel.add(setRunDateButton);

		JButton setRunNameButton = new JButton("Set run name");
		setRunNameButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object res = JOptionPane.showInputDialog(manager,  "Set the run name (mandatory): it can be anything without space (they will be replaced by _ )", "Run name",
						JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), null, null);
				if (res != null){
					for (int row=0 ; row < projectsTable.getRowCount() ; row++){
						projectsTable.setValueAt(res.toString().trim().replace(' ', '_'), row, projectsTable.convertColumnIndexToView(projectTableModel.getColumn("run_name")));
					}
					refresh();
				}
			}
		});
		fillToolsPanel.add(setRunNameButton);

		JButton setPlatformButton = new JButton("Set platform");
		setPlatformButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object res = JOptionPane.showInputDialog(manager,  "Set the platform (mandatory)", "Platform",
						JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), manager.listPlatforms(), null);
				if (res != null){
					String item = res.toString();
					if (item.equals("Add new platform")){
						res = JOptionPane.showInputDialog(manager,  "Set a name for the new platform", "Platform",
								JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), null, null);
						if (res == null) return;
						item = res.toString().trim();
					}
					for (int row=0 ; row < projectsTable.getRowCount() ; row++){
						projectsTable.setValueAt(item, row, projectsTable.convertColumnIndexToView(projectTableModel.getColumn("platform")));
					}
					refresh();
				}
			}
		});
		fillToolsPanel.add(setPlatformButton);

		JButton setSequencingTargetButton = new JButton("Set sequencing target");
		setSequencingTargetButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object res = JOptionPane.showInputDialog(manager,  "Set the sequecing target of the run (optional)", "Sequencing target",
						JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), manager.listSequencingTargets(), null);
				if (res != null){
					String item = res.toString();
					if (item.equals("Add new sequencing_target")){
						res = JOptionPane.showInputDialog(manager,  "Set the name of the new target (no spaces)", "Sequencing target",
								JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), null, null);
						if (res == null) return;
						item = res.toString().trim().replace(' ', '_');
					}
					for (int row=0 ; row < projectsTable.getRowCount() ; row++){
						projectsTable.setValueAt(item, row, projectsTable.convertColumnIndexToView(projectTableModel.getColumn("sequencing_target")));
					}
					refresh();
				}
			}
		});
		fillToolsPanel.add(setSequencingTargetButton);

		JButton setOutsourcingButton = new JButton("Set outsourcing");
		setOutsourcingButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object res = JOptionPane.showInputDialog(manager,  "Set outsourcing (optional):\nif the run has been done outside the lab,\nplease give the name of the company which has done it", "Outsourcing",
						JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), manager.listOursourcing(), "?");
				if (res != null){
					String item = res.toString();
					if (item.equals("Add new outsourcing")){
						res = JOptionPane.showInputDialog(manager,  "Set a name for the new outsourcing", "Outsourcing",
								JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), null, null);
						if (res == null) return;
						item = res.toString().trim();
					}
					for (int row=0 ; row < projectsTable.getRowCount() ; row++){
						projectsTable.setValueAt(item, row, projectsTable.convertColumnIndexToView(projectTableModel.getColumn("outsourcing")));
					}
					refresh();
				}
			}
		});
		fillToolsPanel.add(setOutsourcingButton);

		JButton setIndexCaseButton = new JButton("Set index case");
		setIndexCaseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object res = JOptionPane.showInputDialog(manager,  "Set if selected samples are from an index case ('false' by default)", "Index case",
						JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), new String[]{"true","false"}, null);
				if (res != null){
					String item = res.toString();
					for (int row : projectsTable.getSelectedRows()){
						projectsTable.setValueAt(item, row, projectsTable.convertColumnIndexToView(projectTableModel.getColumn("index_case")));
					}
					refresh();
				}
			}
		});
		fillToolsPanel.add(setIndexCaseButton);

		JButton setPathologyButton = new JButton("Set pathology");
		setPathologyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object res = JOptionPane.showInputDialog(manager,  "Set the pathology of selected samples (mandatory)", "Pathology",
						JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), manager.listPathologies(), null);
				if (res != null){
					String item = res.toString();
					for (int row : projectsTable.getSelectedRows()){
						projectsTable.setValueAt(item, row, projectsTable.convertColumnIndexToView(projectTableModel.getColumn("pathology")));
					}
					refresh();
				}
			}
		});
		fillToolsPanel.add(setPathologyButton);

		JButton setPopulationButton = new JButton("Set population");
		setPopulationButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object res = JOptionPane.showInputDialog(manager,  "Set the population to which individuals of selected samples belong to", "Population",
						JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), manager.listPopulations(), null);
				if (res != null){
					String item = res.toString();
					for (int row : projectsTable.getSelectedRows()){
						projectsTable.setValueAt(item, row, projectsTable.convertColumnIndexToView(projectTableModel.getColumn("population")));
					}
					refresh();
				}
			}
		});
		fillToolsPanel.add(setPopulationButton);
		
		JButton setTypeButton = new JButton("Set sample type");
		setTypeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object res = JOptionPane.showInputDialog(manager,  "Set the type of selected samples (mandatory)", "Sample type",
						JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), SampleType.values(), null);
				if (res != null){
					String item = res.toString();
					for (int row : projectsTable.getSelectedRows()){
						projectsTable.setValueAt(item, row, projectsTable.convertColumnIndexToView(projectTableModel.getColumn("sample_type")));
					}
					refresh();
				}
			}
		});
		fillToolsPanel.add(setTypeButton);

		JButton setKitButton = new JButton("Set capture kit");
		setKitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object res = JOptionPane.showInputDialog(manager,  "Set the capture kit of selected samples (optional)", "Capture kit",
						JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), manager.listKits(), null);
				if (res != null){
					String item = res.toString();
					if (item.equals("Add new kit")){
						res = JOptionPane.showInputDialog(manager,  "Set the name of the new capture kit (no spaces)", "Capture kit",
								JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), null, null);
						if (res == null) return;
						item = res.toString().trim().replace(' ', '_');
					}
					for (int row : projectsTable.getSelectedRows()){
						projectsTable.setValueAt(item, row, projectsTable.convertColumnIndexToView(projectTableModel.getColumn("kit")));
					}
					refresh();
				}
			}
		});
		fillToolsPanel.add(setKitButton);

		JButton setReadLengthButton = new JButton("Set read length");
		setReadLengthButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object res = JOptionPane.showInputDialog(manager,  "Set the read length of selected samples, e.g. 2x150bp (optional)", "Read length",
						JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), null, null);
				if (res != null){
					String item = res.toString();
					for (int row=0 ; row < projectsTable.getRowCount() ; row++){
						projectsTable.setValueAt(item, row, projectsTable.convertColumnIndexToView(projectTableModel.getColumn("read_length")));
					}
					refresh();
				}
			}
		});
		fillToolsPanel.add(setReadLengthButton);

		JButton setPairEndButton = new JButton("Set pair-end");
		setPairEndButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object res = JOptionPane.showInputDialog(manager,  "Set if selected samples are pair-end ('true' by default)", "Pair-end",
						JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), new String[]{"true","false"}, null);
				if (res != null){
					String item = res.toString();
					for (int row : projectsTable.getSelectedRows()){
						projectsTable.setValueAt(item, row, projectsTable.convertColumnIndexToView(projectTableModel.getColumn("pair_end")));
					}
					refresh();
				}
			}
		});
		fillToolsPanel.add(setPairEndButton);

		JButton setTrimButton = new JButton("Set trimming needed");
		setTrimButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object res = JOptionPane.showInputDialog(manager,  "Set if a trimming of the reads is necessary before alignment ('false' by default).\nIt's generally not necessary, except for low quality DNA (e.g. FFPE samples).", "Trimming",
						JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), new String[]{"true","false"}, null);
				if (res != null){
					String item = res.toString();
					for (int row : projectsTable.getSelectedRows()){
						projectsTable.setValueAt(item, row, projectsTable.convertColumnIndexToView(projectTableModel.getColumn("trim")));
					}
					refresh();
				}
			}
		});
		fillToolsPanel.add(setTrimButton);
		
		JButton setRemoveDupButton = new JButton("Set remove duplicates");
		setRemoveDupButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object res = JOptionPane.showInputDialog(manager,  "Set if duplicated reads must be removed after alignment ('true' by default).\nIt's generally necessary to get rid of PCR duplicates that can skew the variant calling.\nSet it to 'false' for PCR-free sequencing or really high coverage (coverage > 2x read length).", "Remove duplicates",
						JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), new String[]{"true","false"}, null);
				if (res != null){
					String item = res.toString();
					for (int row : projectsTable.getSelectedRows()){
						projectsTable.setValueAt(item, row, projectsTable.convertColumnIndexToView(projectTableModel.getColumn("remove_duplicates")));
					}
					refresh();
				}
			}
		});
		fillToolsPanel.add(setRemoveDupButton);
		
		JButton setNormalButton = new JButton("Set normal sample");
		setNormalButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				AskNormalSampleDialog ask = new AskNormalSampleDialog();
				Tools.centerWindow(ask, false);
				ask.setVisible(true);
				if (ask.isSampleSelected()){
					int id = ask.getSelectedId();
					String sample = ask.getSelectedSample();
					for (int row : projectsTable.getSelectedRows()){
						projectsTable.setValueAt(id, row, projectsTable.convertColumnIndexToView(projectTableModel.getColumn("normal_id")));
						projectsTable.setValueAt(sample, row, projectsTable.convertColumnIndexToView(projectTableModel.getColumn("normal_sample")));
					}
					refresh();
				}
			}
		});
		fillToolsPanel.add(setNormalButton);

		JButton findNormalButton = new JButton("Auto-select normal sample");
		findNormalButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (int row : projectsTable.getSelectedRows()){
					if (projectsTable.getValueAt(row, projectsTable.convertColumnIndexToView(projectTableModel.getColumn("individual"))) != null) {
						String individual = projectsTable.getValueAt(row, projectsTable.convertColumnIndexToView(projectTableModel.getColumn("individual"))).toString();
						if (projectsTable.getValueAt(row, projectsTable.convertColumnIndexToView(projectTableModel.getColumn("sample_type"))) != null) {
							String sample_type = projectsTable.getValueAt(row, projectsTable.convertColumnIndexToView(projectTableModel.getColumn("sample_type"))).toString();
							if (sample_type.equalsIgnoreCase("Somatic")) {
								try (Results res = DB.select(Schema.HIGHLANDER, "SELECT project_id, sample FROM projects WHERE individual = '"+individual+"' AND sample_type = 'Germline' ORDER BY project_id DESC")){
									if (res.next()) {
										projectsTable.setValueAt(res.getInt("project_id"), row, projectsTable.convertColumnIndexToView(projectTableModel.getColumn("normal_id")));
										projectsTable.setValueAt(res.getString("sample"), row, projectsTable.convertColumnIndexToView(projectTableModel.getColumn("normal_sample")));										
									}
								}catch(Exception ex) {
									ProjectManager.toConsole(ex);
								}
							}
						}
					}
				}
				refresh();
			}
		});
		fillToolsPanel.add(findNormalButton);
		
		JButton setPersonInChargeButton = new JButton("Add person in charge");
		setPersonInChargeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				AskUsersDialog ask = new AskUsersDialog(false);
				Tools.centerWindow(ask, false);
				ask.setVisible(true);
				if (!ask.getSelection().isEmpty()){
					String item = "";
					for (User user : ask.getSelection()){
						item += user.getUsername()+",";
					}
					item = item.substring(0,item.length()-1);
					for (int row : projectsTable.getSelectedRows()){
						Object val = projectsTable.getValueAt(row, projectsTable.convertColumnIndexToView(projectTableModel.getColumn("users")));
						if (val != null && val.toString().length() > 0){
							if (!val.toString().contains(item)){
								projectsTable.setValueAt(val + "," + item, row, projectsTable.convertColumnIndexToView(projectTableModel.getColumn("users")));
							}
						}else{
							projectsTable.setValueAt(item, row, projectsTable.convertColumnIndexToView(projectTableModel.getColumn("users")));
						}
					}
					refresh();
				}
			}
		});
		fillToolsPanel.add(setPersonInChargeButton);

		JButton clearCellButton = new JButton("Clear selection");
		clearCellButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (int row : projectsTable.getSelectedRows()){
					for (int col : projectsTable.getSelectedColumns()){
						if (col != projectsTable.convertColumnIndexToView(projectTableModel.getColumn("project_id"))) projectsTable.setValueAt(null, row, col);
					}
				}
				refresh();
			}
		});
		fillToolsPanel.add(clearCellButton);

		projectsTable = new JTable(){
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
		projectsTable.setCellSelectionEnabled(true);
		projectsTable.setRowSelectionAllowed(true);
		projectsTable.setColumnSelectionAllowed(false);
		projectsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		projectsTable.createDefaultColumnsFromModel();
		projectsTable.getTableHeader().setReorderingAllowed(false);
		projectsTable.getTableHeader().setResizingAllowed(true);
		projectsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		new ExcelAdapter(projectsTable);		
		projectsTable.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent arg0) {
			}

			@Override
			public void keyReleased(KeyEvent arg0) {
			}

			@Override
			public void keyPressed(KeyEvent arg0) {
				if (arg0.getKeyCode() == KeyEvent.VK_ENTER ||
						arg0.getKeyCode() == KeyEvent.VK_LEFT ||
						arg0.getKeyCode() == KeyEvent.VK_RIGHT ||
						arg0.getKeyCode() == KeyEvent.VK_UP ||
						arg0.getKeyCode() == KeyEvent.VK_DOWN){
					refresh();
				}				
			}
		});
		JScrollPane scroll = new JScrollPane(projectsTable);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);	
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

		add(scroll, BorderLayout.CENTER);

		JPanel southPanel = new JPanel(new GridBagLayout());
		add(southPanel, BorderLayout.SOUTH);

		JPanel projectPanel = new JPanel(new WrapLayout(WrapLayout.LEADING));
		projectPanel.setBorder(BorderFactory.createTitledBorder("Project"));
		southPanel.add(projectPanel, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));

		JButton createNewButton = new JButton("Create", Resources.getScaledIcon(Resources.iDbAdd, 16));
		createNewButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						createProject();
					}
				}, "ProjectsPanel.createProject").start();

			}
		});
		projectPanel.add(createNewButton);

		JButton updateButton = new JButton("Validate (modifications)", Resources.getScaledIcon(Resources.iDbPatcher, 16));
		updateButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						updateProject();
					}
				}, "ProjectsPanel.updateProject").start();

			}
		});
		projectPanel.add(updateButton);

		JButton excelButton = new JButton("Export table to Excel", Resources.getScaledIcon(Resources.iExcel, 16));
		excelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						toXlsx();
					}
				}, "ProjectsPanel.toXlsx").start();

			}
		});
		projectPanel.add(excelButton);

		JPanel samplePanel = new JPanel(new WrapLayout(WrapLayout.LEADING));
		samplePanel.setBorder(BorderFactory.createTitledBorder("Sample"));
		southPanel.add(samplePanel, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));

		JButton importSampleButton = new JButton("Import SELECTED sample(s) from VCF(s) to an analysis", Resources.getScaledIcon(Resources.iDbAdd, 16));
		importSampleButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						importSample();
					}
				}, "ProjectsPanel.importSample").start();

			}
		});
		samplePanel.add(importSampleButton);

		JButton importCoverageDetailsButton = new JButton("Import coverage details for SELECTED sample(s)", Resources.getScaledIcon(Resources.iDbAdd, 16));
		importCoverageDetailsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						importCoverageDetails();
					}
				}, "ProjectsPanel.importCoverageDetails").start();

			}
		});
		samplePanel.add(importCoverageDetailsButton);

		/*
		 * 
		 * Old GATK coverage files, now using mosdepth and a single button with choice of coverage target
		 * 
		JButton importCoverageWithDupButton = new JButton("Import coverages with duplicates for SELECTED sample(s)", Resources.getScaledIcon(Resources.iDbAdd, 16));
		importCoverageWithDupButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						importCoverageWithDups();
					}
				}, "ProjectsPanel.importCoverageWithDups").start();

			}
		});
		samplePanel.add(importCoverageWithDupButton);

		JButton importCoverageWithoutDupButton = new JButton("Import coverages without duplicates for SELECTED sample(s)", Resources.getScaledIcon(Resources.iDbAdd, 16));
		importCoverageWithoutDupButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						importCoverageWithoutDups();
					}
				}, "ProjectsPanel.importCoverageWithoutDups").start();

			}
		});
		samplePanel.add(importCoverageWithoutDupButton);

		JButton importCoverageExomeButton = new JButton("Import exome coverages for SELECTED sample(s)", Resources.getScaledIcon(Resources.iDbAdd, 16));
		importCoverageExomeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						importCoverageExome();
					}
				}, "ProjectsPanel.importCoverageExome").start();

			}
		});
		samplePanel.add(importCoverageExomeButton);
		 */
		
		JButton importFastQCButton = new JButton("Import FastQC report for SELECTED sample(s)", Resources.getScaledIcon(Resources.iDbAdd, 16));
		importFastQCButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						importFastQC();
					}
				}, "ProjectsPanel.importFastQC").start();

			}
		});
		samplePanel.add(importFastQCButton);

		JButton copyAnalysisButton = new JButton("Duplicate SELECTION to another analysis", Resources.getScaledIcon(Resources.iDbPatcher, 16));
		copyAnalysisButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						Object resFrom = JOptionPane.showInputDialog(manager,  "Select the analysis FROM which the samples will be duplicated", "Sample duplication",
								JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), manager.getAvailableAnalysesAsArray(), null);
						if (resFrom != null){
							Object resTo = JOptionPane.showInputDialog(manager,  "Select the analysis TO which the samples will be duplicated", "Sample duplication",
									JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), manager.getAvailableAnalysesAsArray(), null);
							if (resTo != null){
								duplicateSample((AnalysisFull)resFrom,(AnalysisFull)resTo);								
							}
						}
					}
				}, "ProjectsPanel.copyAnalysisButton").start();

			}
		});
		samplePanel.add(copyAnalysisButton);

		JButton deleteAnalysisButton = new JButton("Delete SELECTION from an analysis", Resources.getScaledIcon(Resources.iDbRemove, 16));
		deleteAnalysisButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						Object res = JOptionPane.showInputDialog(manager,  "Select the analysis from which the samples will be deleted", "Analysis sample deletion",
								JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), manager.getAvailableAnalysesAsArray(), null);
						if (res != null){
							deleteSample(res.toString());
						}
					}
				}, "ProjectsPanel.deleteAnalysisButton").start();

			}
		});
		samplePanel.add(deleteAnalysisButton);

		JButton deleteButton = new JButton("Delete SELECTION from the WHOLE database", Resources.getScaledIcon(Resources.iDbRemove, 16));
		deleteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						deleteSample();
					}
				}, "ProjectsPanel.deleteButton").start();

			}
		});
		samplePanel.add(deleteButton);

		JPanel annotationsPanel = new JPanel(new WrapLayout(WrapLayout.LEADING));
		annotationsPanel.setBorder(BorderFactory.createTitledBorder("User annotations (variant evaluations)"));
		southPanel.add(annotationsPanel, new GridBagConstraints(0, 3, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));

		JButton transferAnnotationsButton = new JButton("Duplicate user evaluations of SELECTION to another analysis", Resources.getScaledIcon(Resources.iDbAdd, 16));
		transferAnnotationsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						Object from = JOptionPane.showInputDialog(manager,  "Select the analysis FROM which the evaluations will be duplicated", "User evaluations duplication",
								JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), manager.getAvailableAnalysesAsArray(), null);
						if (from != null){
							Object to = JOptionPane.showInputDialog(manager,  "Select the analysis TO which the evaluations will be duplicated", "User evaluations duplication",
									JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iPressKey,64), manager.getAvailableAnalysesAsArray(), null);
							if (to != null){
								duplicateUserAnnotations((AnalysisFull)from, (AnalysisFull)to);
							}
						}
					}
				}, "ProjectsPanel.transferAnnotationsButton").start();

			}
		});
		annotationsPanel.add(transferAnnotationsButton);

	}

	private void refresh(){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				try{
					projectTableModel.fireTableRowsUpdated(0,projectTableModel.getRowCount()-1);
				}catch(Exception ex){
					Tools.exception(ex);
				}
			}
		});	
	}

	public void fill(){
		if (projectBox.getSelectedItem() != null) {
			fill(projectBox.getSelectedItem().toString());
		}
	}
	
	private void fill(String run){
		fill(run, -1);
	}

	private void fill(int numOfRows){
		fill(null, numOfRows);
	}

	private void fill(String run, int numOfRows){
		try{
			String[] headers = new String[]{
					"project_id",
					"run_id",
					"run_date",
					"run_name",
					"platform",
					"sequencing_target",
					"outsourcing",
					"family",
					"individual",
					"sample",
					"index_case",
					"pathology",
					"population",
					"sample_type",
					"barcode",
					"kit",
					"read_length",
					"pair_end",
					"trim",
					"remove_duplicates",
					"normal_id",
					"normal_sample",
					"comments",
					"users",
					"cov_with_dups_available",
					"cov_without_dups_available",
					"cov_exome_available",
					"fastqc_available",
					"analyses",								 //Must stay last-1, or change code below
					"gene_coverage_available", //Must stay last, or change code below
			};
			Object[][] data;
			if (run == null){
				projectBox.setSelectedItem(null);
				data = new Object[numOfRows][headers.length];
				for (int row=0 ; row < numOfRows ; row++){
					for (int col=0 ; col < headers.length ; col++){
						if (headers[col].equals("index_case")) data[row][col] = "false";
						if (headers[col].equals("pair_end")) data[row][col] = "true";
						if (headers[col].equals("trim")) data[row][col] = "true";
						if (headers[col].equals("remove_duplicates")) data[row][col] = "true";
					}
				}
			}else{
				String[] parts = run.split("_");
				String run_id = parts[0];
				String run_date = parts[1] + "-" + parts[2] + "-" + parts[3];
				String run_name = parts[4];
				for (int i=5 ; i < parts.length ; i++){
					run_name += "_" + parts[i];
				}
				List<Object[]> arrayList = new ArrayList<Object[]>();
				try (Results res = DB.select(Schema.HIGHLANDER, "SELECT GROUP_CONCAT(DISTINCT u.username) as users, GROUP_CONCAT(DISTINCT a.analysis) as analyses, p.*"+
						", pathology, population" + 
						", p.average_depth_of_target_coverage IS NOT NULL as cov_with_dups_available, p.coverage_wo_dup IS NOT NULL as cov_without_dups_available, p.coverage_exome_wo_dup IS NOT NULL as cov_exome_available"+
						", (p.per_base_sequence_quality IS NOT NULL) as fastqc_available" +
						", \"\" as gene_coverage_available "+
						", p2.sample as normal_sample "+
						"FROM projects as p " +
						"JOIN pathologies USING (pathology_id) " +
						"LEFT JOIN populations USING (population_id) " +
						"LEFT JOIN projects_users as u USING (project_id) " +
						"LEFT JOIN projects_analyses as a USING (project_id) " +
						"LEFT JOIN projects as p2 ON p.normal_id = p2.project_id " +
						"WHERE p.run_id = '"+run_id+"' AND p.run_date = '"+run_date+"' AND p.run_name = '"+run_name+"' GROUP BY p.project_id")){
					while(res.next()){
						Object[] array = new Object[headers.length];
						for (int col=0 ; col < headers.length ; col++){
							if (headers[col].contains("_available") && !headers[col].equals("gene_coverage_available")) array[col] = res.getBoolean(headers[col]);
							else array[col] = res.getObject(headers[col]);
						}
						arrayList.add(array);
					}
				}
				int colAnalyses = headers.length-2;
				int colGeneCov = headers.length-1;
				for (Object[] array : arrayList){
					if (array[colAnalyses] != null && array[colAnalyses].toString().length() > 0){
						String[] anals = array[colAnalyses].toString().split(",");
						for (String analysis : anals){
							try (Results res = DB.select(Schema.HIGHLANDER, "SELECT project_id FROM "+new Analysis(analysis).getFromCoverage()+"WHERE project_id = "+array[0]+" limit 1")) {
								if (res.next()){
									array[colGeneCov] = (array[colGeneCov].toString().length() > 0) ? array[colGeneCov] + ", true" : "true"; 
								}else{
									array[colGeneCov] = (array[colGeneCov].toString().length() > 0) ? array[colGeneCov] + ", false" : "false";
								}
							}
						}
					}
				}
				data = new Object[arrayList.size()][headers.length];
				int row = 0;
				for (Object[] array : arrayList){
					data[row] = array;
					row++;
				}
			}
			projectTableModel = new AdministrationTableModel(data, headers);
			projectsTable.setModel(projectTableModel);
			if (run != null){
				for (int i=0 ; i < projectsTable.getColumnCount() ; i++){
					int width = 0;
					for (int row = 0; row < projectsTable.getRowCount(); row++) {
						TableCellRenderer renderer = projectsTable.getCellRenderer(row, i);
						Component comp = projectsTable.prepareRenderer(renderer, row, i);
						width = Math.max (comp.getPreferredSize().width, width);
					}
					projectsTable.getColumnModel().getColumn(i).setPreferredWidth(width+20);
				}
			}
			refresh();
		}catch(Exception ex){
			Tools.exception(ex);
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
								if (table.isCellEditable(startRow+i,startCol+j)) table.setValueAt(value,startRow+i,startCol+j);
						}
					}
				}	catch(Exception ex){
					Tools.exception(ex);
				}
				refresh();
			}else if (e.getActionCommand().compareTo("Delete")==0){
				try	{
					for (int row : table.getSelectedRows()){
						for (int col : table.getSelectedColumns()){
							if (col != ((AdministrationTableModel)(table.getModel())).getColumn("project_id") &&
									col != ((AdministrationTableModel)(table.getModel())).getColumn("cov_with_dups_available") &&
									col != ((AdministrationTableModel)(table.getModel())).getColumn("cov_without_dups_available") &&
									col != ((AdministrationTableModel)(table.getModel())).getColumn("fastqc_available") &&
									col != ((AdministrationTableModel)(table.getModel())).getColumn("analyses") &&
									col != ((AdministrationTableModel)(table.getModel())).getColumn("gene_coverage_available") &&
									col != ((AdministrationTableModel)(table.getModel())).getColumn("analyses"))
								table.setValueAt(null,row,col);
						}
					}
				}	catch(Exception ex){
					Tools.exception(ex);
				}
				refresh();
			}
		}
	}

	public void createProject(){
		Object res = JOptionPane.showInputDialog(manager, "How many samples are there in the project ?", "Create new project",
				JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iDbAdd,64), null, null);
		if (res != null){
			try{
				int num = Integer.parseInt(res.toString());
				if (num < 1) throw new NumberFormatException();
				fill(num);
			}catch(NumberFormatException ex){
				Tools.exception(ex);
				JOptionPane.showMessageDialog(new JFrame(), "You must enter a valid number", "Create new project", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}
		}
	}

	public void updateProject(){
		int finalCheck = 1;
		List<String> existingSamples = new ArrayList<>();
		for (int row=0 ; row < projectsTable.getRowCount() ; row++){
			Object o = projectsTable.getValueAt(row, projectTableModel.getColumn("project_id"));
			String check = checkSample(row);
			if (!check.equals("INVALID") && !check.equals("VALID") && o == null){
				finalCheck = 2;
				existingSamples.add(projectsTable.getValueAt(row, projectTableModel.getColumn("sample")).toString() + " in " + check);
			}else if (check.equals("INVALID")){
				finalCheck = 0;
				break;
			}
		}
		int res = JOptionPane.YES_OPTION;
		if (finalCheck == 0){
			JOptionPane.showMessageDialog(new JFrame(), "Some information is missing, please fill all information mandatory to the project", "Update project", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}else{ 
			if (finalCheck == 2){
				StringBuilder sb = new StringBuilder();
				sb.append("Some new samples are already present in the database, meaning that older sample data will be overwritten if importation is done in the same analysis.\n");
				for (String sample : existingSamples) {
					sb.append("  - " + sample + "\n");
				}
				sb.append("Are you sure you want duplicate sample names ?");
				res = JOptionPane.showConfirmDialog(new JFrame(), sb.toString(), "Update project", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iQuestion,64));
			}
			if (res == JOptionPane.YES_OPTION){
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						waitingPanel.setVisible(true);
						waitingPanel.start();
					}
				});
				try{
					String project = "";
					for (int row=0 ; row < projectsTable.getRowCount() ; row++){
						Object o = projectsTable.getValueAt(row, projectTableModel.getColumn("project_id"));
						Sample sample = new Sample(projectTableModel,row);
						project = sample.getProject();
						if (o == null){
							//New sample to insert							
							int id = sample.insertInDb();
							projectsTable.setValueAt(id, row, projectTableModel.getColumn("project_id"));
						}else{
							//Existing sample to update
							sample.updateInDb(manager, Integer.parseInt(o.toString()));
						}
					}
					projectsList.add(project);
					projectBox.setSelectedItem(project);
					JOptionPane.showMessageDialog(new JFrame(), "Project successfuly updated", "Update project",
							JOptionPane.PLAIN_MESSAGE, Resources.getScaledIcon(Resources.iDbPatcher,64));					
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

	public void toXlsx(){
		FileDialog chooser = new FileDialog(new JFrame(), "Output Excel file", FileDialog.SAVE) ;
		chooser.setFile(Tools.formatFilename(projectBox.getSelectedItem()+".xlsx"));
		Tools.centerWindow(chooser, false);
		chooser.setVisible(true) ;
		if (chooser.getFile() != null) {
			String filename = chooser.getDirectory() + chooser.getFile();
			if (!filename.endsWith(".xlsx")) filename += ".xlsx";
			File xls = new File(filename);
			try{
				waitingPanel.start();
				try{
					Workbook wb = new SXSSFWorkbook(100); 
					int totalRows = 0;					
					JTable table = projectsTable;
					Sheet sheet = wb.createSheet(projectBox.getSelectedItem().toString().replace(':', '-'));
					sheet.createFreezePane(0, 1);		
					int r = 0;
					Row row = sheet.createRow(r++);
					row.setHeightInPoints(50);
					for (int c = 0 ; c < table.getColumnCount() ; c++){
						row.createCell(c).setCellValue(table.getColumnName(c));
					}
					sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, table.getColumnCount()-1));
					int nrow = table.getRowCount();
					waitingPanel.setProgressString("Exporting "+Tools.doubleToString(nrow, 0, false)+" lines", false);
					waitingPanel.setProgressMaximum(nrow);
					for (int i=0 ; i < nrow ; i++ ){
						waitingPanel.setProgressValue(r);
						row = sheet.createRow(r++);
						for (int c = 0 ; c < table.getColumnCount() ; c++){
							if (table.getValueAt(i, c) == null)
								row.createCell(c);
							else if (table.getColumnClass(c) == Timestamp.class)
								row.createCell(c).setCellValue((Timestamp)table.getValueAt(i, c));
							else if (table.getColumnClass(c) == Integer.class)
								row.createCell(c).setCellValue(Integer.parseInt(table.getValueAt(i, c).toString()));
							else if (table.getColumnClass(c) == Long.class)
								row.createCell(c).setCellValue(Long.parseLong(table.getValueAt(i, c).toString()));
							else if (table.getColumnClass(c) == Double.class)
								row.createCell(c).setCellValue(Double.parseDouble(table.getValueAt(i, c).toString()));
							else if (table.getColumnClass(c) == Boolean.class)
								row.createCell(c).setCellValue(Boolean.parseBoolean(table.getValueAt(i, c).toString()));
							else 
								row.createCell(c).setCellValue(table.getValueAt(i, c).toString());
						}
					}		
					totalRows += nrow;
					waitingPanel.setProgressValue(totalRows);
					waitingPanel.setProgressString("Writing file ...",true);		
					try (FileOutputStream fileOut = new FileOutputStream(xls)){
						wb.write(fileOut);
					}
					waitingPanel.setProgressDone();
				}catch(Exception ex){
					waitingPanel.forceStop();
					throw ex;
				}
				waitingPanel.stop();
			}catch (IOException ex){
				Tools.exception(ex);
				JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("I/O error when creating file", ex), "Exporting to Excel",
						JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}catch (Exception ex){
				Tools.exception(ex);
				JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Error during export", ex), "Exporting to Excel",
						JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}
		}
	}

	public void deleteSample(){
		ProjectManager.setHardUpdate(true);
		StringBuilder sb = new StringBuilder();
		for (int row : projectsTable.getSelectedRows()){
			String sample = projectsTable.getValueAt(row, projectTableModel.getColumn("sample")).toString();
			String id = projectsTable.getValueAt(row, projectTableModel.getColumn("project_id")).toString();
			sb.append("sample "+sample+" (internal id "+id+")\n");
		}
		int res = JOptionPane.showConfirmDialog(new JFrame(), "Are you SURE you want to COMPLETELY delete:\n"+sb.toString()+"from the WHOLE Highlander database ?", "Delete sample from Highlander", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iDbRemove,64));
		if (res == JOptionPane.CANCEL_OPTION){
			return;
		}else if (res == JOptionPane.YES_OPTION){
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(true);
					waitingPanel.start();
				}
			});
			for (int row : projectsTable.getSelectedRows()){
				String sample = projectsTable.getValueAt(row, projectTableModel.getColumn("sample")).toString();
				String id = projectsTable.getValueAt(row, projectTableModel.getColumn("project_id")).toString();
				try{
					if (projectsTable.getValueAt(row, projectTableModel.getColumn("analyses")) != null) {
						for (String analysis : projectsTable.getValueAt(row, projectTableModel.getColumn("analyses")).toString().split(",")){
							deleteSample(new Analysis(analysis), id, sample);
						}
					}
					ProjectManager.toConsole("-----------------------------------------------------");
					ProjectManager.toConsole("Deleting project id " + id + " from projects");
					DB.update(Schema.HIGHLANDER, "DELETE FROM projects WHERE `project_id` = "+id);
					ProjectManager.toConsole("*** Remember to remove sample from server if necessary ***");
				}catch(Exception ex){
					ProjectManager.toConsole(ex);
				}
			}
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(false);
					waitingPanel.stop();
				}
			});
			refresh();
		}
		ProjectManager.setHardUpdate(false);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				fill(projectBox.getSelectedItem().toString());
			}
		});
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				fill(projectBox.getSelectedItem().toString());
			}
		});
	}

	public void importSample(){
		ImportSampleDialog dialog = new ImportSampleDialog();
		Tools.centerWindow(dialog, false);
		dialog.setVisible(true);
		if (dialog.validate){
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(true);
					waitingPanel.start();
				}
			});						
			manager.startRedirectSystemOut();
			for (int row : projectsTable.getSelectedRows()){
				Sample s = new Sample(projectTableModel,row);
				String sample = s.getSample();
				String project = s.getProject();
				try{
					AnalysisFull analysis = (AnalysisFull)importAnalysisBox.getSelectedItem();
					File vcf = new File(importVCFs.get(sample).getText());
					File alamut = new File(importAlamuts.get(sample).getText());
					boolean overwrite = importOverwrite.isSelected();
					ProjectManager.toConsole("-----------------------------------------------------");
					ProjectManager.toConsole("Importation of sample " + sample + " in project " + project);
					if (!vcf.exists()) {
						ProjectManager.toConsole("The selected VCF file ("+vcf+") does not exists, sample importation SKIPPED !");
					}else{
						ProjectManager.toConsole("Analysis: " + analysis);
						if (vcf.exists()) ProjectManager.toConsole("VCF file: " + vcf);
						if (alamut.exists()) ProjectManager.toConsole("Alamut file: " + alamut);
						else ProjectManager.toConsole("Not Alamut annotation file has been selected");
						if (overwrite) ProjectManager.toConsole("Existing data will be overwritten if necessary");
						else ProjectManager.toConsole("Sample won't be imported if existing data is found");
						List<AnalysisFull> analyses = new ArrayList<>();
						analyses.add(analysis);
						ProjectManager.getDbBuilder().importSample(project, sample, vcf.getPath(), analyses, (alamut.exists()?alamut.getPath():null), overwrite, false);
						int project_id = DBUtils.getProjectId(project, sample);
						DB.update(Schema.HIGHLANDER, "UPDATE projects_analyses SET run_path = '"+vcf.getPath().replace("\\", "/")+"' WHERE `project_id` = "+project_id+" AND `analysis` = '"+analysis+"' AND `run_path` IS NULL");
					}
				}catch(Exception ex){
					ProjectManager.toConsole(ex);
				}
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				ProjectManager.toConsole("*****************************************************");
				ProjectManager.toConsole("If all of your samples are imported, don't forget to run the post-importation steps ('statistics' and 'possible values' computation are mandatory if you want your sample to appear in Highlander clients)");
				ProjectManager.toConsole("*****************************************************");
			}
			manager.stopRedirectSystemOut();
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(false);
					waitingPanel.stop();
				}
			});
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					fill(projectBox.getSelectedItem().toString());
				}
			});
		}
	}

	public class ImportSampleDialog extends JDialog {
		public boolean validate = false;

		public ImportSampleDialog(){
			getContentPane().setLayout(new BorderLayout());
			getContentPane().add(getParametersPanel(), BorderLayout.CENTER);
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			int width = screenSize.width - (screenSize.width/3*2);
			int height = screenSize.height - (screenSize.height/3);
			setSize(new Dimension(width,height));
			setModal(true);
			setTitle("Sample importation parameters");
			setIconImage(Resources.getScaledIcon(Resources.iDbAdd, 64).getImage());
			pack();
		}

		public JPanel getParametersPanel(){
			JPanel panel = new JPanel(new BorderLayout());

			JPanel analysisPanel = new JPanel(new WrapLayout(WrapLayout.LEADING, 10, 5));		
			analysisPanel.add(new JLabel("Analysis: "));
			importAnalysisBox = new JComboBox<AnalysisFull>(manager.getAvailableAnalysesAsArray());
			analysisPanel.add(importAnalysisBox);

			JPanel topPanel = new JPanel(new BorderLayout());
			topPanel.add(analysisPanel, BorderLayout.NORTH);
			panel.add(topPanel, BorderLayout.NORTH);

			JPanel vcfPanel = new JPanel(new GridBagLayout());
			int r=0;
			for (int row : projectsTable.getSelectedRows()){
				final String sample = projectsTable.getValueAt(row, projectTableModel.getColumn("sample")).toString();
				JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 5));
				JLabel sampleLabel = new JLabel(sample);
				sampleLabel.setPreferredSize(new Dimension(150, 10));
				rowPanel.add(sampleLabel);
				final JTextField vcfFile = new JTextField();
				vcfFile.setColumns(30);
				JButton vcfFileButton = new JButton("VCF");
				vcfFileButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						FileDialog chooser = new FileDialog(new JFrame(), "Select the VCF file for sample "+sample, FileDialog.LOAD) ;
						Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize() ;
						Dimension windowSize = chooser.getSize() ;
						chooser.setLocation(Math.max(0, (screenSize.width - windowSize.width) / 2),
								Math.max(0, (screenSize.height - windowSize.height) / 2)) ;
						chooser.setVisible(true) ;
						if (chooser.getFile() != null) {
							vcfFile.setText(chooser.getDirectory() + chooser.getFile());
						}
					}			
				});
				importVCFs.put(sample, vcfFile);
				rowPanel.add(vcfFileButton);
				rowPanel.add(vcfFile);
				final JTextField alamutFile = new JTextField();
				alamutFile.setColumns(30);
				JButton alamutFileButton = new JButton("Alamut");
				alamutFileButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						FileDialog chooser = new FileDialog(new JFrame(), "Select the Alamut file for sample "+sample, FileDialog.LOAD) ;
						Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize() ;
						Dimension windowSize = chooser.getSize() ;
						chooser.setLocation(Math.max(0, (screenSize.width - windowSize.width) / 2),
								Math.max(0, (screenSize.height - windowSize.height) / 2)) ;
						chooser.setVisible(true) ;
						if (chooser.getFile() != null) {
							alamutFile.setText(chooser.getDirectory() + chooser.getFile());
						}
					}			
				});
				importAlamuts.put(sample, alamutFile);
				rowPanel.add(alamutFileButton);
				rowPanel.add(alamutFile);
				vcfPanel.add(rowPanel, new GridBagConstraints(0, r++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(1, 5, 1, 5), 0, 0));
			}
			JScrollPane vcfScrollPane = new JScrollPane(vcfPanel);
			vcfScrollPane.setBorder(BorderFactory.createTitledBorder("Select a VCF file for each sample, already annoted with snpEff. You can also optionally select an Alamut annotation file."));
			panel.add(vcfScrollPane, BorderLayout.CENTER);

			JPanel validationPanel = new JPanel(new WrapLayout(WrapLayout.LEADING, 10, 5));
			importOverwrite = new JCheckBox("Overwrite sample data if already present in selected analysis");
			importOverwrite.setSelected(true);
			validationPanel.add(importOverwrite);
			JButton importButton = new JButton(" Import ");
			importButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					validate = true;
					dispose();
				}
			});
			validationPanel.add(importButton);
			panel.add(validationPanel, BorderLayout.SOUTH);

			return panel;
		}

	}

	public void importCoverageDetails(){
		ImportCoverageDetailsDialog dialog = new ImportCoverageDetailsDialog();
		Tools.centerWindow(dialog, false);
		dialog.setVisible(true);
		if (dialog.validate){
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(true);
					waitingPanel.start();
				}
			});						
			manager.startRedirectSystemOut();
			for (int row : projectsTable.getSelectedRows()){
				Sample s = new Sample(projectTableModel,row);
				String sample = s.getSample();
				String project = s.getProject();
				try{
					AnalysisFull analysis = (AnalysisFull)importCoverageDetailsBox.getSelectedItem();
					CoverageTarget target = (CoverageTarget)importCoverageDetailsTargetBox.getSelectedItem();
					File inputFileThresholds = new File(importCoverageDetailsThresholdsFiles.get(sample).getText());
					File inputFileRegions = new File(importCoverageDetailsRegionsFiles.get(sample).getText());
					ProjectManager.toConsole("-----------------------------------------------------");
					ProjectManager.toConsole("Importation of coverage details for sample " + sample + " in project " + project);
					if (!inputFileThresholds.exists()) {
						ProjectManager.toConsole("The selected thresholds files ("+inputFileThresholds+") does not exists, importation SKIPPED for this sample !");
					}else if (!inputFileRegions.exists()) {
						ProjectManager.toConsole("The selected regions files ("+inputFileRegions+") does not exists, importation SKIPPED for this sample !");
					}else{
						ProjectManager.toConsole("Analysis: " + analysis);
						if (inputFileThresholds.exists()) ProjectManager.toConsole("MosDepth thresholds file: " + inputFileThresholds);
						if (inputFileRegions.exists()) ProjectManager.toConsole("MosDepth regions file: " + inputFileRegions);
						List<AnalysisFull> analyses = new ArrayList<>();
						analyses.add(analysis);
						//TODO les cov raw et wodup
						ProjectManager.getDbBuilder().buildCoverage(target, inputFileThresholds.getPath(), inputFileRegions.getPath(), analyses, project, sample);
					}
				}catch(Exception ex){
					ProjectManager.toConsole(ex);
				}
			}
			manager.stopRedirectSystemOut();
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(false);
					waitingPanel.stop();
				}
			});
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					fill(projectBox.getSelectedItem().toString());
				}
			});
		}
	}

	public class ImportCoverageDetailsDialog extends JDialog {
		public boolean validate = false;

		public ImportCoverageDetailsDialog(){
			getContentPane().setLayout(new BorderLayout());
			getContentPane().add(getMainPanel(), BorderLayout.CENTER);
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			int width = screenSize.width - (screenSize.width/3*2);
			int height = screenSize.height - (screenSize.height/3);
			setSize(new Dimension(width,height));
			setModal(true);
			setTitle("Coverage details importation");
			setIconImage(Resources.getScaledIcon(Resources.iDbAdd, 64).getImage());
			pack();
		}

		public JPanel getMainPanel(){
			JPanel panel = new JPanel(new BorderLayout());

			JPanel analysisPanel = new JPanel(new WrapLayout(WrapLayout.LEADING, 10, 5));		
			analysisPanel.add(new JLabel("Analysis: "));
			importCoverageDetailsBox = new JComboBox<AnalysisFull>(manager.getAvailableAnalysesAsArray());
			analysisPanel.add(importCoverageDetailsBox);
			analysisPanel.add(new JLabel("Target: "));
			importCoverageDetailsTargetBox = new JComboBox<CoverageTarget>(CoverageTarget.values());
			analysisPanel.add(importCoverageDetailsTargetBox);
			panel.add(analysisPanel, BorderLayout.NORTH);

			JPanel filePanel = new JPanel(new GridBagLayout());
			int r=0;
			for (int row : projectsTable.getSelectedRows()){
				final String sample = projectsTable.getValueAt(row, projectTableModel.getColumn("sample")).toString();
				JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 5));
				JLabel sampleLabel = new JLabel(sample);
				sampleLabel.setPreferredSize(new Dimension(150, 10));
				rowPanel.add(sampleLabel);
				final JTextField inputMosdepthThresholds = new JTextField();
				inputMosdepthThresholds.setColumns(30);
				JButton inputMosdepthThresholdsButton = new JButton("BROWSE");
				inputMosdepthThresholdsButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						FileDialog chooser = new FileDialog(new JFrame(), "Select input mosdepth thresholds gz file for sample "+sample, FileDialog.LOAD) ;
						Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize() ;
						Dimension windowSize = chooser.getSize() ;
						chooser.setLocation(Math.max(0, (screenSize.width - windowSize.width) / 2),
								Math.max(0, (screenSize.height - windowSize.height) / 2)) ;
						chooser.setVisible(true) ;
						if (chooser.getFile() != null) {
							inputMosdepthThresholds.setText(chooser.getDirectory() + chooser.getFile());
						}
					}			
				});
				importCoverageDetailsThresholdsFiles.put(sample, inputMosdepthThresholds);
				rowPanel.add(inputMosdepthThresholdsButton);
				rowPanel.add(inputMosdepthThresholds);
				final JTextField inputMosdepthRegions = new JTextField();
				inputMosdepthRegions.setColumns(30);
				JButton inputMosdepthRegionsButton = new JButton("BROWSE");
				inputMosdepthRegionsButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						FileDialog chooser = new FileDialog(new JFrame(), "Select input mosdepth regions gz file for sample "+sample, FileDialog.LOAD) ;
						Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize() ;
						Dimension windowSize = chooser.getSize() ;
						chooser.setLocation(Math.max(0, (screenSize.width - windowSize.width) / 2),
								Math.max(0, (screenSize.height - windowSize.height) / 2)) ;
						chooser.setVisible(true) ;
						if (chooser.getFile() != null) {
							inputMosdepthRegions.setText(chooser.getDirectory() + chooser.getFile());
						}
					}			
				});
				importCoverageDetailsRegionsFiles.put(sample, inputMosdepthRegions);
				rowPanel.add(inputMosdepthRegionsButton);
				rowPanel.add(inputMosdepthRegions);
				filePanel.add(rowPanel, new GridBagConstraints(0, r++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(1, 5, 1, 5), 0, 0));
			}
			JScrollPane inputFilesScrollPane = new JScrollPane(filePanel);
			inputFilesScrollPane.setBorder(BorderFactory.createTitledBorder(
					"Select a MosDepth thresholds and regions gz files (use mosdepth argument --thresholds 1,20,30,100,200,500,1000,5000) for each sample."));
			panel.add(inputFilesScrollPane, BorderLayout.CENTER);

			JPanel validationPanel = new JPanel(new WrapLayout(WrapLayout.LEADING, 10, 5));
			JButton importButton = new JButton(" Import ");
			importButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					validate = true;
					dispose();
				}
			});
			validationPanel.add(importButton);
			panel.add(validationPanel, BorderLayout.SOUTH);

			return panel;
		}

	}

	//Not used anymore (GATK coverage files replaced by Mosdepth)
	public void importCoverageWithDups(){
		ImportCoverageWithDupsDialog dialog = new ImportCoverageWithDupsDialog();
		Tools.centerWindow(dialog, false);
		dialog.setVisible(true);
		if (dialog.validate){
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(true);
					waitingPanel.start();
				}
			});						
			manager.startRedirectSystemOut();
			for (int row : projectsTable.getSelectedRows()){
				Sample s = new Sample(projectTableModel,row);
				String sample = s.getSample();
				String project = s.getProject();
				/*
				String projectId = projectsTable.getValueAt(row, projectTableModel.getColumn("project_id")).toString();
				String runPath = "";
				*/
				try{
					/*
					try (Results res = DB.select(Schema.HIGHLANDER, "SELECT run_path FROM projects_analyses WHERE `analysis` = '"+analysis+"' AND project_id = " + projectId)) {
						if (res.next()){
							runPath = res.getString(1);
						}
					}
					*/
					File inputFile = new File(importCoverageWDFiles.get(sample).getText());
					ProjectManager.toConsole("-----------------------------------------------------");
					ProjectManager.toConsole("Importation of coverage with duplicates for sample " + sample + " in project " + project);
					if (!inputFile.exists()) {
						ProjectManager.toConsole("The selected file ("+inputFile+") does not exists, importation SKIPPED for this sample !");
					}else{
						/*
						if (importCoverageWDUpdateRunpath.isSelected()){
							runPath = inputFile.getParent().replace("\\", "/");
						}
						*/
						if (inputFile.exists()) ProjectManager.toConsole("GATK coverage file: " + inputFile);
						//ProjectManager.toConsole("Run path set to: " + runPath);
						ProjectManager.getDbBuilder().importProjectGatkCoverageWithDuplicates(project, sample, inputFile.getPath());
					}
				}catch(Exception ex){
					ProjectManager.toConsole(ex);
				}
			}
			manager.stopRedirectSystemOut();
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(false);
					waitingPanel.stop();
				}
			});
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					fill(projectBox.getSelectedItem().toString());
				}
			});
		}
	}

	//Not used anymore (GATK coverage files replaced by Mosdepth)
	public class ImportCoverageWithDupsDialog extends JDialog {
		public boolean validate = false;

		public ImportCoverageWithDupsDialog(){
			getContentPane().setLayout(new BorderLayout());
			getContentPane().add(getMainPanel(), BorderLayout.CENTER);
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			int width = screenSize.width - (screenSize.width/3*2);
			int height = screenSize.height - (screenSize.height/3);
			setSize(new Dimension(width,height));
			setModal(true);
			setTitle("Importation of global coverage (alignment with duplicates)");
			setIconImage(Resources.getScaledIcon(Resources.iDbAdd, 64).getImage());
			pack();
		}

		public JPanel getMainPanel(){
			JPanel panel = new JPanel(new BorderLayout());

			JPanel analysisPanel = new JPanel(new WrapLayout(WrapLayout.LEADING, 10, 5));		
			/*
			importCoverageWDUpdateRunpath = new JCheckBox("Update 'run path' to the path of selected file");
			analysisPanel.add(importCoverageWDUpdateRunpath);
			*/
			panel.add(analysisPanel, BorderLayout.NORTH);

			JPanel filePanel = new JPanel(new GridBagLayout());
			int r=0;
			for (int row : projectsTable.getSelectedRows()){
				final String sample = projectsTable.getValueAt(row, projectTableModel.getColumn("sample")).toString();
				JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 5));
				JLabel sampleLabel = new JLabel(sample);
				sampleLabel.setPreferredSize(new Dimension(150, 10));
				rowPanel.add(sampleLabel);
				final JTextField inputFile = new JTextField();
				inputFile.setColumns(30);
				JButton inputFileButton = new JButton("BROWSE");
				inputFileButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						FileDialog chooser = new FileDialog(new JFrame(), "Select input file for sample "+sample, FileDialog.LOAD) ;
						Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize() ;
						Dimension windowSize = chooser.getSize() ;
						chooser.setLocation(Math.max(0, (screenSize.width - windowSize.width) / 2),
								Math.max(0, (screenSize.height - windowSize.height) / 2)) ;
						chooser.setVisible(true) ;
						if (chooser.getFile() != null) {
							inputFile.setText(chooser.getDirectory() + chooser.getFile());
						}
					}			
				});
				importCoverageWDFiles.put(sample, inputFile);
				rowPanel.add(inputFileButton);
				rowPanel.add(inputFile);
				filePanel.add(rowPanel, new GridBagConstraints(0, r++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(1, 5, 1, 5), 0, 0));
			}
			JScrollPane inputFilesScrollPane = new JScrollPane(filePanel);
			inputFilesScrollPane.setBorder(BorderFactory.createTitledBorder("Select a GATK coverage file .DOCbasemap10.sample_summary (using tool 'DepthOfCoverage' with arguments ' -ct 1 -ct 5 -ct 10 -ct 20 -ct 30') for each sample."));
			panel.add(inputFilesScrollPane, BorderLayout.CENTER);

			JPanel validationPanel = new JPanel(new WrapLayout(WrapLayout.LEADING, 10, 5));
			JButton importButton = new JButton(" Import ");
			importButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					validate = true;
					dispose();
				}
			});
			validationPanel.add(importButton);
			panel.add(validationPanel, BorderLayout.SOUTH);

			return panel;
		}

	}

	//Not used anymore (GATK coverage files replaced by Mosdepth)
	public void importCoverageWithoutDups(){
		ImportCoverageWithoutDupsDialog dialog = new ImportCoverageWithoutDupsDialog();
		Tools.centerWindow(dialog, false);
		dialog.setVisible(true);
		if (dialog.validate){
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(true);
					waitingPanel.start();
				}
			});						
			manager.startRedirectSystemOut();
			for (int row : projectsTable.getSelectedRows()){
				Sample s = new Sample(projectTableModel,row);
				String sample = s.getSample();
				String project = s.getProject();
				/*
				String projectId = projectsTable.getValueAt(row, projectTableModel.getColumn("project_id")).toString();
				String runPath = "";
				*/
				try{
					/*
					try (Results res = DB.select(Schema.HIGHLANDER, "SELECT run_path FROM projects_analyses WHERE `analysis` = '"+analysis+"' AND project_id = " + projectId)) {
						if (res.next()){
							runPath = res.getString(1);
						}
					}
					*/
					File inputFile = new File(importCoverageWODFiles.get(sample).getText());
					ProjectManager.toConsole("-----------------------------------------------------");
					ProjectManager.toConsole("Importation of coverage without duplicates for sample " + sample + " in project " + project);
					if (!inputFile.exists()) {
						ProjectManager.toConsole("The selected file ("+inputFile+") does not exists, importation SKIPPED for this sample !");
					}else{
						/*
						if (importCoverageWODUpdateRunpath.isSelected()){
							runPath = inputFile.getParent().replace("\\", "/");
						}
						*/
						if (inputFile.exists()) ProjectManager.toConsole("GATK coverage file: " + inputFile);
						//ProjectManager.toConsole("Run path set to: " + runPath);
						ProjectManager.getDbBuilder().importProjectGatkCoverageWithoutDuplicates(project, sample, inputFile.getPath());
					}
				}catch(Exception ex){
					ProjectManager.toConsole(ex);
				}
			}
			manager.stopRedirectSystemOut();
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(false);
					waitingPanel.stop();
				}
			});
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					fill(projectBox.getSelectedItem().toString());
				}
			});
		}
	}

	//Not used anymore (GATK coverage files replaced by Mosdepth)
	public class ImportCoverageWithoutDupsDialog extends JDialog {
		public boolean validate = false;

		public ImportCoverageWithoutDupsDialog(){
			getContentPane().setLayout(new BorderLayout());
			getContentPane().add(getMainPanel(), BorderLayout.CENTER);
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			int width = screenSize.width - (screenSize.width/3*2);
			int height = screenSize.height - (screenSize.height/3);
			setSize(new Dimension(width,height));
			setModal(true);
			setTitle("Importation of global coverage (alignment without duplicates)");
			setIconImage(Resources.getScaledIcon(Resources.iDbAdd, 64).getImage());
			pack();
		}

		public JPanel getMainPanel(){
			JPanel panel = new JPanel(new BorderLayout());

			JPanel analysisPanel = new JPanel(new WrapLayout(WrapLayout.LEADING, 10, 5));	
			/*
			importCoverageWODUpdateRunpath = new JCheckBox("Update 'run path' to the path of selected file");
			analysisPanel.add(importCoverageWODUpdateRunpath);
			*/
			panel.add(analysisPanel, BorderLayout.NORTH);

			JPanel filePanel = new JPanel(new GridBagLayout());
			int r=0;
			for (int row : projectsTable.getSelectedRows()){
				final String sample = projectsTable.getValueAt(row, projectTableModel.getColumn("sample")).toString();
				JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 5));
				JLabel sampleLabel = new JLabel(sample);
				sampleLabel.setPreferredSize(new Dimension(150, 10));
				rowPanel.add(sampleLabel);
				final JTextField inputFile = new JTextField();
				inputFile.setColumns(30);
				JButton inputFileButton = new JButton("BROWSE");
				inputFileButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						FileDialog chooser = new FileDialog(new JFrame(), "Select input file for sample "+sample, FileDialog.LOAD) ;
						Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize() ;
						Dimension windowSize = chooser.getSize() ;
						chooser.setLocation(Math.max(0, (screenSize.width - windowSize.width) / 2),
								Math.max(0, (screenSize.height - windowSize.height) / 2)) ;
						chooser.setVisible(true) ;
						if (chooser.getFile() != null) {
							inputFile.setText(chooser.getDirectory() + chooser.getFile());
						}
					}			
				});
				importCoverageWODFiles.put(sample, inputFile);
				rowPanel.add(inputFileButton);
				rowPanel.add(inputFile);
				filePanel.add(rowPanel, new GridBagConstraints(0, r++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(1, 5, 1, 5), 0, 0));
			}
			JScrollPane inputFilesScrollPane = new JScrollPane(filePanel);
			inputFilesScrollPane.setBorder(BorderFactory.createTitledBorder("Select a GATK coverage file .DOCbasemap10.sample_summary (using tool 'DepthOfCoverage' with arguments ' -ct 1 -ct 5 -ct 10 -ct 20 -ct 30') for each sample."));
			panel.add(inputFilesScrollPane, BorderLayout.CENTER);

			JPanel validationPanel = new JPanel(new WrapLayout(WrapLayout.LEADING, 10, 5));
			JButton importButton = new JButton(" Import ");
			importButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					validate = true;
					dispose();
				}
			});
			validationPanel.add(importButton);
			panel.add(validationPanel, BorderLayout.SOUTH);

			return panel;
		}

	}

	//Not used anymore (GATK coverage files replaced by Mosdepth)
	public void importCoverageExome(){
		ImportCoverageExomeDialog dialog = new ImportCoverageExomeDialog();
		Tools.centerWindow(dialog, false);
		dialog.setVisible(true);
		if (dialog.validate){
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(true);
					waitingPanel.start();
				}
			});						
			manager.startRedirectSystemOut();
			for (int row : projectsTable.getSelectedRows()){
				Sample s = new Sample(projectTableModel,row);
				String sample = s.getSample();
				String project = s.getProject();
				/*
				String projectId = projectsTable.getValueAt(row, projectTableModel.getColumn("project_id")).toString();
				String runPath = "";
				*/
				try{
					/*
					try (Results res = DB.select(Schema.HIGHLANDER, "SELECT run_path FROM projects_analyses WHERE `analysis` = '"+analysis+"' AND project_id = " + projectId)) {
						if (res.next()){
							runPath = res.getString(1);
						}
					}
					*/
					File inputFile = new File(importCoverageExomeFiles.get(sample).getText());
					ProjectManager.toConsole("-----------------------------------------------------");
					ProjectManager.toConsole("Importation of exome coverage for sample " + sample + " in project " + project);
					if (!inputFile.exists()) {
						ProjectManager.toConsole("The selected file ("+inputFile+") does not exists, importation SKIPPED for this sample !");
					}else{
						/*
						if (importCoverageExomeUpdateRunpath.isSelected()){
							runPath = inputFile.getParent().replace("\\", "/");
						}
						*/
						if (inputFile.exists()) ProjectManager.toConsole("GATK coverage file: " + inputFile);
						//ProjectManager.toConsole("Run path set to: " + runPath);
						ProjectManager.getDbBuilder().importProjectGatkCoverageExome(project, sample, inputFile.getPath());
					}
				}catch(Exception ex){
					ProjectManager.toConsole(ex);
				}
			}
			manager.stopRedirectSystemOut();
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(false);
					waitingPanel.stop();
				}
			});
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					fill(projectBox.getSelectedItem().toString());
				}
			});
		}
	}

	//Not used anymore (GATK coverage files replaced by Mosdepth)
	public class ImportCoverageExomeDialog extends JDialog {
		public boolean validate = false;

		public ImportCoverageExomeDialog(){
			getContentPane().setLayout(new BorderLayout());
			getContentPane().add(getMainPanel(), BorderLayout.CENTER);
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			int width = screenSize.width - (screenSize.width/3*2);
			int height = screenSize.height - (screenSize.height/3);
			setSize(new Dimension(width,height));
			setModal(true);
			setTitle("Importation of exome coverage (alignment without duplicates)");
			setIconImage(Resources.getScaledIcon(Resources.iDbAdd, 64).getImage());
			pack();
		}

		public JPanel getMainPanel(){
			JPanel panel = new JPanel(new BorderLayout());

			JPanel analysisPanel = new JPanel(new WrapLayout(WrapLayout.LEADING, 10, 5));		
			/*
			importCoverageExomeUpdateRunpath = new JCheckBox("Update 'run path' to the path of selected file");
			analysisPanel.add(importCoverageExomeUpdateRunpath);
			*/
			panel.add(analysisPanel, BorderLayout.NORTH);

			JPanel filePanel = new JPanel(new GridBagLayout());
			int r=0;
			for (int row : projectsTable.getSelectedRows()){
				final String sample = projectsTable.getValueAt(row, projectTableModel.getColumn("sample")).toString();
				JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 5));
				JLabel sampleLabel = new JLabel(sample);
				sampleLabel.setPreferredSize(new Dimension(150, 10));
				rowPanel.add(sampleLabel);
				final JTextField inputFile = new JTextField();
				inputFile.setColumns(30);
				JButton inputFileButton = new JButton("BROWSE");
				inputFileButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						FileDialog chooser = new FileDialog(new JFrame(), "Select input file for sample "+sample, FileDialog.LOAD) ;
						Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize() ;
						Dimension windowSize = chooser.getSize() ;
						chooser.setLocation(Math.max(0, (screenSize.width - windowSize.width) / 2),
								Math.max(0, (screenSize.height - windowSize.height) / 2)) ;
						chooser.setVisible(true) ;
						if (chooser.getFile() != null) {
							inputFile.setText(chooser.getDirectory() + chooser.getFile());
						}
					}			
				});
				importCoverageExomeFiles.put(sample, inputFile);
				rowPanel.add(inputFileButton);
				rowPanel.add(inputFile);
				filePanel.add(rowPanel, new GridBagConstraints(0, r++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(1, 5, 1, 5), 0, 0));
			}
			JScrollPane inputFilesScrollPane = new JScrollPane(filePanel);
			inputFilesScrollPane.setBorder(BorderFactory.createTitledBorder("Select a GATK coverage file .DOCbasemap10.sample_summary (using tool 'DepthOfCoverage' limited to exome (using -L) with arguments ' -ct 1 -ct 5 -ct 10 -ct 20 -ct 30') for each sample."));
			panel.add(inputFilesScrollPane, BorderLayout.CENTER);

			JPanel validationPanel = new JPanel(new WrapLayout(WrapLayout.LEADING, 10, 5));
			JButton importButton = new JButton(" Import ");
			importButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					validate = true;
					dispose();
				}
			});
			validationPanel.add(importButton);
			panel.add(validationPanel, BorderLayout.SOUTH);

			return panel;
		}

	}

	public void importFastQC(){
		ImportFastQCDialog dialog = new ImportFastQCDialog();
		Tools.centerWindow(dialog, false);
		dialog.setVisible(true);
		if (dialog.validate){
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(true);
					waitingPanel.start();
				}
			});						
			manager.startRedirectSystemOut();
			for (int row : projectsTable.getSelectedRows()){
				Sample s = new Sample(projectTableModel,row);
				String sample = s.getSample();
				String project = s.getProject();
				/*
				String projectId = projectsTable.getValueAt(row, projectTableModel.getColumn("project_id")).toString();
				String runPath = "";
				*/
				try{
					/*
					try (Results res = DB.select(Schema.HIGHLANDER, "SELECT run_path FROM projects_analyses WHERE `analysis` = '"+analysis+"' AND project_id = " + projectId)) {
						if (res.next()){
							runPath = res.getString(1);
						}
					}
					*/
					File inputFile = new File(importFastqcFiles.get(sample).getText());
					ProjectManager.toConsole("-----------------------------------------------------");
					ProjectManager.toConsole("Importation of FastQC report for sample " + sample + " in project " + project);
					if (!inputFile.exists()) {
						ProjectManager.toConsole("The selected file ("+inputFile+") does not exists, importation SKIPPED for this sample !");
					}else{
						/*
						if (importFastqcUpdateRunpath.isSelected()){
							runPath = inputFile.getParent().replace("\\", "/");
						}
						*/
						if (inputFile.exists()) ProjectManager.toConsole("FastQC ZIP file: " + inputFile);
						//ProjectManager.toConsole("Run path set to: " + runPath);
						ProjectManager.getDbBuilder().importProjectFastQC(project, sample, inputFile.getPath());
					}
				}catch(Exception ex){
					ProjectManager.toConsole(ex);
				}
			}
			manager.stopRedirectSystemOut();
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(false);
					waitingPanel.stop();
				}
			});
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					fill(projectBox.getSelectedItem().toString());
				}
			});
		}
	}

	public class ImportFastQCDialog extends JDialog {
		public boolean validate = false;

		public ImportFastQCDialog(){
			getContentPane().setLayout(new BorderLayout());
			getContentPane().add(getMainPanel(), BorderLayout.CENTER);
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			int width = screenSize.width - (screenSize.width/3*2);
			int height = screenSize.height - (screenSize.height/3);
			setSize(new Dimension(width,height));
			setModal(true);
			setTitle("Importation of FastQC report");
			setIconImage(Resources.getScaledIcon(Resources.iDbAdd, 64).getImage());
			pack();
		}

		public JPanel getMainPanel(){
			JPanel panel = new JPanel(new BorderLayout());

			JPanel analysisPanel = new JPanel(new WrapLayout(WrapLayout.LEADING, 10, 5));
			/*
			importFastqcUpdateRunpath = new JCheckBox("Update 'run path' to the path of selected file");
			analysisPanel.add(importFastqcUpdateRunpath);
			*/
			panel.add(analysisPanel, BorderLayout.NORTH);

			JPanel filePanel = new JPanel(new GridBagLayout());
			int r=0;
			for (int row : projectsTable.getSelectedRows()){
				final String sample = projectsTable.getValueAt(row, projectTableModel.getColumn("sample")).toString();
				JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 5));
				JLabel sampleLabel = new JLabel(sample);
				sampleLabel.setPreferredSize(new Dimension(150, 10));
				rowPanel.add(sampleLabel);
				final JTextField inputFile = new JTextField();
				inputFile.setColumns(30);
				JButton inputFileButton = new JButton("BROWSE");
				inputFileButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						FileDialog chooser = new FileDialog(new JFrame(), "Select input file for sample "+sample, FileDialog.LOAD) ;
						Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize() ;
						Dimension windowSize = chooser.getSize() ;
						chooser.setLocation(Math.max(0, (screenSize.width - windowSize.width) / 2),
								Math.max(0, (screenSize.height - windowSize.height) / 2)) ;
						chooser.setVisible(true) ;
						if (chooser.getFile() != null) {
							inputFile.setText(chooser.getDirectory() + chooser.getFile());
						}
					}			
				});
				importFastqcFiles.put(sample, inputFile);
				rowPanel.add(inputFileButton);
				rowPanel.add(inputFile);
				filePanel.add(rowPanel, new GridBagConstraints(0, r++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(1, 5, 1, 5), 0, 0));
			}
			JScrollPane inputFilesScrollPane = new JScrollPane(filePanel);
			inputFilesScrollPane.setBorder(BorderFactory.createTitledBorder("Select a FastQC 'zipped' report for each sample."));
			panel.add(inputFilesScrollPane, BorderLayout.CENTER);

			JPanel validationPanel = new JPanel(new WrapLayout(WrapLayout.LEADING, 10, 5));
			JButton importButton = new JButton(" Import ");
			importButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					validate = true;
					dispose();
				}
			});
			validationPanel.add(importButton);
			panel.add(validationPanel, BorderLayout.SOUTH);

			return panel;
		}

	}

	public void duplicateSample(AnalysisFull from, AnalysisFull to){
		ProjectManager.setHardUpdate(true);
		StringBuilder sb = new StringBuilder();
		for (int row : projectsTable.getSelectedRows()){
			String sample = projectsTable.getValueAt(row, projectTableModel.getColumn("sample")).toString();
			String id = projectsTable.getValueAt(row, projectTableModel.getColumn("project_id")).toString();
			sb.append("sample "+sample+" (internal id "+id+")\n");
		}
		int res = JOptionPane.showConfirmDialog(new JFrame(), "Are you SURE you want to duplicate:\n"+sb.toString()+"from '"+from+"' to '"+to+"' ?", "Duplicate samples", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iDbRemove,64));
		if (res == JOptionPane.CANCEL_OPTION){
			return;
		}else if (res == JOptionPane.YES_OPTION){
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(true);
					waitingPanel.start();
				}
			});
			for (int row : projectsTable.getSelectedRows()){
				String sample = projectsTable.getValueAt(row, projectTableModel.getColumn("sample")).toString();
				int id = Integer.parseInt(projectsTable.getValueAt(row, projectTableModel.getColumn("project_id")).toString());
				try{
					duplicateSample(from, to, id, sample);
				}catch(Exception ex){
					ProjectManager.toConsole(ex);
				}
			}
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(false);
					waitingPanel.stop();
				}
			});
		}
		ProjectManager.setHardUpdate(false);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				fill(projectBox.getSelectedItem().toString());
			}
		});
	}

	private void duplicateSample(AnalysisFull from, AnalysisFull to, int projectId, String sample) throws Exception {
		boolean idIsInFrom = false;
		boolean idIsInTo = false;
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT analysis FROM projects_analyses WHERE project_id = " + projectId)) {
			while (res.next()){
				if (res.getString("analysis").equals(from.toString())) idIsInFrom = true;
				if (res.getString("analysis").equals(to.toString())) idIsInTo = true;
			}
		}
		if (!idIsInFrom){
			ProjectManager.toConsole("+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+");
			ProjectManager.toConsole("ERROR: Sample " + sample + " (internal id " + projectId + ") is not present in source analysis '" + from + "'");
			return;
		}
		if (idIsInTo){
			ProjectManager.toConsole("+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+");
			ProjectManager.toConsole("ERROR: Sample " + sample + " (internal id " + projectId + ") is already present in destination analysis '" + to + "'");
			return;
		}
		ProjectManager.toConsole("-----------------------------------------------------");
		ProjectManager.toConsole("Dulpicating sample " + sample + " (internal id " + projectId + ") from analysis '" + from + "' to analysis '" + to + "'");
		//Delete any trace of sample in target analysis (should not have any, except if a precedent duplication was aborded before the end).
		DB.update(Schema.HIGHLANDER, "DELETE FROM "+to.getFromCoverage()+"WHERE `"+Field.project_id+"` = "+projectId);
		DB.update(Schema.HIGHLANDER, "DELETE FROM "+to.getFromCustomAnnotations()+"WHERE `"+Field.project_id+"` = "+projectId);
		DB.update(Schema.HIGHLANDER, "DELETE FROM "+to.getFromSampleAnnotations()+"WHERE `"+Field.project_id+"` = "+projectId);
		DB.update(Schema.HIGHLANDER, "DELETE FROM projects_analyses WHERE project_id = " + projectId + " AND analysis = '"+to+"'");
		//Duplicate
		ProjectManager.toConsole("Duplicating " + from + "_coverage");
		List<String> columns = duplicateSampleGetCommonColumns(from, to, "_coverage");
		DB.update(Schema.HIGHLANDER, "INSERT INTO "+to+"_coverage ("+HighlanderDatabase.makeSqlFieldList(columns)+") SELECT "+HighlanderDatabase.makeSqlFieldList(columns)+" FROM "+from+"_coverage WHERE `project_id` = "+projectId);
		ProjectManager.toConsole("Duplicating " + from + "_custom_annotations");
		columns = duplicateSampleGetCommonColumns(from, to, "_custom_annotations");
		DB.update(Schema.HIGHLANDER, "INSERT INTO "+to+"_custom_annotations ("+HighlanderDatabase.makeSqlFieldList(columns)+") SELECT "+HighlanderDatabase.makeSqlFieldList(columns)+" FROM "+from+"_custom_annotations WHERE `project_id` = "+projectId);
		ProjectManager.toConsole("Duplicating " + from + "_sample_annotations");		
		columns = duplicateSampleGetCommonColumns(from, to, "_sample_annotations");
		DB.update(Schema.HIGHLANDER, "INSERT INTO "+to+"_sample_annotations ("+HighlanderDatabase.makeSqlFieldList(columns)+") SELECT "+HighlanderDatabase.makeSqlFieldList(columns)+" FROM "+from+"_sample_annotations WHERE `project_id` = "+projectId);
		ProjectManager.toConsole("Duplicating " + from + "_static_annotations");		
		columns = duplicateSampleGetCommonColumns(from, to, "_static_annotations");
		DB.update(Schema.HIGHLANDER, "INSERT IGNORE INTO "+to+"_static_annotations ("+HighlanderDatabase.makeSqlFieldList(columns)+") SELECT "+HighlanderDatabase.makeSqlFieldList(columns)+" FROM "+from+"_static_annotations WHERE `project_id` = "+projectId);
		DBUtils.transferUserAnnotationsEvaluations(from, to, projectId);
		DB.insert(Schema.HIGHLANDER, "INSERT IGNORE INTO `projects_analyses` SET `project_id` = "+projectId+", `analysis` = '"+to+"', users_warned = FALSE");
		DbBuilder.updateAnalysisMetrics(projectId, to);
		ProjectManager.toConsole("Duplication done");
		ProjectManager.toConsole("After all duplicaton(s) / deletions, don't forget to do the post-importation steps (statistics and possible values update).");
		ProjectManager.toConsole("Note that BAM/VCF symbolic links should be manually copied in the directory of " + to + " analysis on the server.");
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				fill(projectBox.getSelectedItem().toString());
			}
		});
	}

	private List<String> duplicateSampleGetCommonColumns(Analysis from, Analysis to, String tableSuffix) throws Exception {
		List<String> columnsFrom = new ArrayList<String>();
		try (Results res = DB.select(Schema.HIGHLANDER, "DESCRIBE " + from + tableSuffix)) {
			while (res.next()){
				columnsFrom.add(DB.getDescribeColumnName(Schema.HIGHLANDER, res));
			}
		}
		List<String> columns = new ArrayList<String>();
		try (Results res = DB.select(Schema.HIGHLANDER, "DESCRIBE " + to + tableSuffix)) {
			while (res.next()){
				columns.add(DB.getDescribeColumnName(Schema.HIGHLANDER, res));
			}
		}
		columns.retainAll(columnsFrom);
		columns.remove("id");
		return columns;
	}

	public void deleteSample(String analysis){
		ProjectManager.setHardUpdate(true);
		StringBuilder sb = new StringBuilder();
		for (int row : projectsTable.getSelectedRows()){
			String sample = projectsTable.getValueAt(row, projectTableModel.getColumn("sample")).toString();
			String id = projectsTable.getValueAt(row, projectTableModel.getColumn("project_id")).toString();
			sb.append("sample "+sample+" (internal id "+id+")\n");
		}
		int res = JOptionPane.showConfirmDialog(new JFrame(), "Are you SURE you want to COMPLETELY delete:\n"+sb.toString()+"from the '"+analysis+"' Highlander database ?", "Delete sample from Highlander", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iDbRemove,64));
		if (res == JOptionPane.CANCEL_OPTION){
			return;
		}else if (res == JOptionPane.YES_OPTION){
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(true);
					waitingPanel.start();
				}
			});
			for (int row : projectsTable.getSelectedRows()){
				String sample = projectsTable.getValueAt(row, projectTableModel.getColumn("sample")).toString();
				String id = projectsTable.getValueAt(row, projectTableModel.getColumn("project_id")).toString();
				try{
					deleteSample(new Analysis(analysis), id, sample);
				}catch(Exception ex){
					ProjectManager.toConsole(ex);
				}
			}
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(false);
					waitingPanel.stop();
				}
			});
		}
		ProjectManager.setHardUpdate(false);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				fill(projectBox.getSelectedItem().toString());
			}
		});		
	}

	private void deleteSample(Analysis analysis, String projectId, String sample) throws Exception {
		ProjectManager.toConsole("-----------------------------------------------------");
		ProjectManager.toConsole("Deleting sample " + sample + " (internal id " + projectId + ") from analysis " + analysis);
		DB.update(Schema.HIGHLANDER, "DELETE FROM "+analysis.getFromCoverage()+"WHERE `"+Field.project_id.getName()+"` = "+projectId);
		DB.update(Schema.HIGHLANDER, "DELETE FROM "+analysis.getFromCustomAnnotations()+"WHERE `"+Field.project_id+"` = "+projectId);
		DB.update(Schema.HIGHLANDER, "DELETE FROM "+analysis.getFromSampleAnnotations()+"WHERE `"+Field.project_id+"` = "+projectId);
		DB.update(Schema.HIGHLANDER, "DELETE FROM projects_analyses WHERE project_id = " + projectId + " AND analysis = '"+analysis+"'");
	}

	public void duplicateUserAnnotations(AnalysisFull from, AnalysisFull to) {
		ProjectManager.setHardUpdate(true);
		StringBuilder sb = new StringBuilder();
		for (int row : projectsTable.getSelectedRows()){
			String sample = projectsTable.getValueAt(row, projectTableModel.getColumn("sample")).toString();
			String id = projectsTable.getValueAt(row, projectTableModel.getColumn("project_id")).toString();
			sb.append("sample "+sample+" (internal id "+id+")\n");
		}
		int res = JOptionPane.showConfirmDialog(new JFrame(), "Are you SURE you want to duplicate following user annotations:\n"+sb.toString()+"from '"+from+"' to '"+to+"' ?", "Duplicate user annotations", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iDbRemove,64));
		if (res == JOptionPane.CANCEL_OPTION){
			return;
		}else if (res == JOptionPane.YES_OPTION){
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(true);
					waitingPanel.start();
				}
			});
			for (int row : projectsTable.getSelectedRows()){
				String sample = projectsTable.getValueAt(row, projectTableModel.getColumn("sample")).toString();
				int id = Integer.parseInt(projectsTable.getValueAt(row, projectTableModel.getColumn("project_id")).toString());
				try{
					duplicateUserAnnotations(from, to, id, sample);
				}catch(Exception ex){
					ProjectManager.toConsole(ex);
				}
			}
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(false);
					waitingPanel.stop();
				}
			});
		}
		ProjectManager.setHardUpdate(false);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				fill(projectBox.getSelectedItem().toString());
			}
		});
	}

	private void duplicateUserAnnotations(AnalysisFull from, AnalysisFull to, int id, String sample) throws Exception {
		boolean idIsInFrom = false;
		boolean idIsInTo = false;
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT analysis FROM projects_analyses WHERE project_id = " + id)) {
			while (res.next()){
				if (res.getString("analysis").equals(from.toString())) idIsInFrom = true;
				if (res.getString("analysis").equals(to.toString())) idIsInTo = true;
			}
		}
		if (!idIsInFrom){
			ProjectManager.toConsole("+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+");
			ProjectManager.toConsole("ERROR: Sample " + sample + " (internal id " + id + ") is not present in source analysis '" + from + "'");
			return;
		}
		if (!idIsInTo){
			ProjectManager.toConsole("+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+");
			ProjectManager.toConsole("ERROR: Sample " + sample + " (internal id " + id + ") is not present in destination analysis '" + to + "'");
			return;
		}
		ProjectManager.toConsole("-----------------------------------------------------");
		ProjectManager.toConsole("Duplicating user annotations of sample " + sample + " (internal id " + id + ") from analysis '" + from + "' to analysis '" + to + "'");
		DBUtils.transferUserAnnotationsEvaluations(from, to, id);			
	}

	/**
	 * 
	 * @param row
	 * @return INVALID if the sample is not valid, VALID if it is and, if it's valid but already exists, the run name(s) and eventual analyses. 
	 */
	private String checkSample(int row) {
		boolean exists = false;
		String foundIn = "";
		Object o;
		o = projectTableModel.getValueAt(row, projectTableModel.getColumn("run_id"));
		if (o == null ) return "INVALID";
		o = projectTableModel.getValueAt(row, projectTableModel.getColumn("run_date"));
		if (o == null ) return "INVALID";
		o = projectTableModel.getValueAt(row, projectTableModel.getColumn("run_name"));
		if (o == null ) return "INVALID";
		o = projectTableModel.getValueAt(row, projectTableModel.getColumn("platform"));
		if (o == null ) return "INVALID";
		o = projectTableModel.getValueAt(row, projectTableModel.getColumn("pathology"));
		if (o == null ) return "INVALID";
		o = projectTableModel.getValueAt(row, projectTableModel.getColumn("sample_type"));
		if (o == null ) return "INVALID";
		o = projectTableModel.getValueAt(row, projectTableModel.getColumn("family"));
		if (o == null || o.toString().length() == 0) return "INVALID";
		o = projectTableModel.getValueAt(row, projectTableModel.getColumn("individual"));
		if (o == null || o.toString().length() == 0) return "INVALID";
		Object o2 = projectTableModel.getValueAt(row, projectTableModel.getColumn("sample"));
		if (o2 == null || o2.toString().length() == 0) {
			return "INVALID";
		}else{	
			o2 = o2.toString();
			try (Results res = DB.select(Schema.HIGHLANDER, 
					"SELECT run_label, GROUP_CONCAT(analysis) as analyses "
					+ "FROM projects "
					+ "LEFT JOIN projects_analyses USING (project_id) "
					+ "WHERE sample = '"+DB.format(Schema.HIGHLANDER, o2.toString().trim())+"' "
					+ "GROUP BY run_label"
					)){
				while (res.next()){
					exists = true;
					foundIn += ", " + res.getString("run_label");
					if (res.getString("analyses") != null) {
						foundIn += " ("+res.getString("analyses")+")";
					}
				}
			}catch(Exception ex){
				Tools.exception(ex);
			}
		}
		if (exists){
			return foundIn.substring(2);
		}else{
			return "VALID";				
		}
	}

}
