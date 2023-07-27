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

package be.uclouvain.ngs.highlander.administration.UI.tools;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;

import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.administration.UI.ManagerPanel;
import be.uclouvain.ngs.highlander.administration.UI.Project;
import be.uclouvain.ngs.highlander.administration.UI.ProjectManager;
import be.uclouvain.ngs.highlander.administration.script.IlluminaExome;
import be.uclouvain.ngs.highlander.administration.script.IlluminaGenome;
import be.uclouvain.ngs.highlander.administration.script.IonTorrent;
import be.uclouvain.ngs.highlander.administration.script.MiSeq;
import be.uclouvain.ngs.highlander.administration.script.Script;
import be.uclouvain.ngs.highlander.administration.script.Solid;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;

/**
* @author Raphael Helaers
*/

//TODO SCRIPTS 17 - sortir les scripts en DB, comme ça ils sont modifiables hors code et utilisable hors GEHU

public class RelauncherPanel extends ManagerPanel {
	
	private DefaultListModel<Project> projectListModel = new DefaultListModel<>();
	private JList<Project> projectList = new JList<>(projectListModel);
	private DefaultListModel<Project> relaunchListModel = new DefaultListModel<>();
	private JList<Project> relaunchList = new JList<>(relaunchListModel);
	private JComboBox<String> scriptBox;
	//private JComboBox<String> pipelineRelauncherPlatformBox;
	private JComboBox<AnalysisFull> analysisBox;
	private JPanel panelParameters = new JPanel();
	private CardLayout relauncherCardLayout = new CardLayout();
	
	private Map<String, Script> scripts = new TreeMap<String, Script>(); 

	public RelauncherPanel(ProjectManager manager){
		super(manager);

		scripts.put("Illumina exome script", new IlluminaExome(manager));
		scripts.put("Illumina genome script", new IlluminaGenome(manager));
		scripts.put("Ion Torrent / Proton script", new IonTorrent(manager));
		scripts.put("Illumina MiSeq script", new MiSeq(manager));
		//scripts.put("Mutect2 script", new Mutect(manager)); //TODO SCRIPTS 17 - Doesn't work, because each script normally launch analysis on a RUN, and Mutect script launch analysis on a pair of samples. See Mutect.java in scripts for more info
		scripts.put("Solid exome script", new Solid(manager));

		JPanel panel_center = new JPanel();
		add(panel_center, BorderLayout.CENTER);
		panel_center.setLayout(new BorderLayout(5, 5));

		JPanel panel_platform = new JPanel(new BorderLayout(5,5));
		panel_center.add(panel_platform, BorderLayout.NORTH);

		scriptBox = new JComboBox<>(scripts.keySet().toArray(new String[0]));
		scriptBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					relauncherCardLayout.show(panelParameters, scriptBox.getSelectedItem().toString());
				}
			}
		});
		panel_platform.add(scriptBox, BorderLayout.NORTH);

		List<String> platforms = new ArrayList<>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT DISTINCT(`platform`) as avp FROM `projects` ORDER BY avp")) {			
			while (res.next()){
				platforms.add(res.getString(1));
			}
		}catch(Exception ex){
			ProjectManager.toConsole(ex);
		}
		/*
		pipelineRelauncherPlatformBox = new JComboBox<>(platforms.toArray(new String[0]));
		pipelineRelauncherPlatformBox.addItem("All Illumina platforms used for exomes");
		pipelineRelauncherPlatformBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					fillPipelineRelauncherLists();
				}
			}
		});
		panel_platform.add(pipelineRelauncherPlatformBox, BorderLayout.SOUTH);
		 */
		analysisBox = new JComboBox<>(manager.getAvailableAnalysesAsArray());
		analysisBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					fill();
				}
			}
		});
		panel_platform.add(analysisBox, BorderLayout.SOUTH);


		JPanel panel_lists = new JPanel();
		panel_center.add(panel_lists, BorderLayout.CENTER);
		GridBagLayout gbl_panel_lists = new GridBagLayout();
		gbl_panel_lists.columnWidths = new int[]{0, 0, 0, 0};
		gbl_panel_lists.rowHeights = new int[]{0, 0};
		gbl_panel_lists.columnWeights = new double[]{1.0, 0, 1.0, 1.0, Double.MIN_VALUE};
		gbl_panel_lists.rowWeights = new double[]{1.0, Double.MIN_VALUE};
		panel_lists.setLayout(gbl_panel_lists);

		JScrollPane scrollPane = new JScrollPane();
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.insets = new Insets(5, 10, 5, 10);
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.gridx = 0;
		gbc_scrollPane.gridy = 0;
		panel_lists.add(scrollPane, gbc_scrollPane);

		JPanel panel_middle = new JPanel();
		GridBagConstraints gbc_panel_middle = new GridBagConstraints();
		gbc_panel_middle.gridx = 1;
		gbc_panel_middle.gridy = 0;
		panel_lists.add(panel_middle, gbc_panel_middle);
		GridBagLayout gbl_panel_2 = new GridBagLayout();
		gbl_panel_2.columnWidths = new int[]{0, 0};
		gbl_panel_2.rowHeights = new int[]{0, 0, 0};
		gbl_panel_2.columnWeights = new double[]{0.0, Double.MIN_VALUE};
		gbl_panel_2.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		panel_middle.setLayout(gbl_panel_2);

		JButton button = new JButton(Resources.getScaledIcon(Resources.iArrowDoubleRight, 24));
		button.setToolTipText("Add selected run(s) to your selection");
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				addValues();
			}
		});
		GridBagConstraints gbc_button = new GridBagConstraints();
		gbc_button.insets = new Insets(0, 0, 5, 0);
		gbc_button.gridx = 0;
		gbc_button.gridy = 0;
		panel_middle.add(button, gbc_button);

		JButton button_1 = new JButton(Resources.getScaledIcon(Resources.iArrowDoubleLeft, 24));
		button_1.setToolTipText("Remove selected run(s) from your selection");
		button_1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				removeValues();
			}
		});
		GridBagConstraints gbc_button_1 = new GridBagConstraints();
		gbc_button_1.gridx = 0;
		gbc_button_1.gridy = 1;
		panel_middle.add(button_1, gbc_button_1);

		projectList.setFixedCellHeight(20); //To avoid a 'random bug' to sometimes (super) oversize one cell
		projectList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		projectList.setBorder(new TitledBorder(null, "Available projects", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		projectList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount() == 2){
					addValues();
				}
			}
		});
		scrollPane.setViewportView(projectList);

		JScrollPane scrollPane_1 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_1 = new GridBagConstraints();
		gbc_scrollPane_1.insets = new Insets(5, 10, 5, 10);
		gbc_scrollPane_1.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_1.gridx = 2;
		gbc_scrollPane_1.gridy = 0;
		panel_lists.add(scrollPane_1, gbc_scrollPane_1);

		relaunchList.setFixedCellHeight(20); //To avoid a 'random bug' to sometimes (super) oversize one cell
		relaunchList.setBorder(new TitledBorder(null, "Projects to relaunch", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		relaunchList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		relaunchList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount() == 2){
					removeValues();
				}
			}
		});
		scrollPane_1.setViewportView(relaunchList);

		GridBagConstraints gbc_panel_param = new GridBagConstraints();
		gbc_panel_param.insets = new Insets(5, 10, 5, 10);
		gbc_panel_param.gridx = 3;
		gbc_panel_param.gridy = 0;
		gbc_panel_param.fill = GridBagConstraints.BOTH;
		gbc_panel_param.weightx = 1.0;
		gbc_panel_param.weighty = 1.0;
		panel_lists.add(panelParameters, gbc_panel_param);
		panelParameters.setLayout(relauncherCardLayout);
		panelParameters.setBorder(new TitledBorder(null, "Parameters", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		for (String script : scripts.keySet()){
			panelParameters.add(scripts.get(script).getArgumentsPanel(), script);
		}
		
		fill();
	}

	private void fill(){
		Map<String,Project> projects = new TreeMap<String,Project>();
		projectListModel.removeAllElements();
		relaunchListModel.removeAllElements();
		try{
			/*
			String platform = "'" + pipelineRelauncherPlatformBox.getSelectedItem() + "'";
			if (platform.equals("'All Illumina platforms used for exomes'")) {
				platform = "'HISEQ','HISEQ_2000','HISEQ_2500','HISEQ_3000','HISEQ_4000','NextSeq','NOVASEQ','X'";
			}
			ResultSet res = DB.select(Schema.HIGHLANDER, 
					"SELECT `run_label`, `kit`, `pair_end`, `reference`, `run_path`, `comments`, GROUP_CONCAT(DISTINCT a.analysis) as analyses " +
					"FROM `projects` as p JOIN projects_analyses as a ON p.project_id = a.project_id " +
					"WHERE platform IN ("+platform+") AND run_path IS NOT NULL GROUP BY p.project_id");
			 */
			AnalysisFull analysis = (AnalysisFull)analysisBox.getSelectedItem();
			try (Results res = DB.select(Schema.HIGHLANDER, 
					"SELECT `run_label`, `kit`, `pair_end`, `run_path`, `comments`, GROUP_CONCAT(DISTINCT a.analysis) as analyses " +
							"FROM `projects` as p JOIN projects_analyses as a USING (project_id) " +
							"WHERE a.analysis = '"+analysis+"' AND run_path IS NOT NULL GROUP BY p.project_id")){
				while (res.next()){
					Project p = new Project();
					p.run_label = res.getString("run_label");
					p.pair_end = res.getBoolean("pair_end");
					p.comments = res.getString("comments");
					p.analyses = res.getString("analyses").split(",");
					p.setPathAndKit(res.getString("run_path"), res.getString("kit"), ""); //TODO SCRIPTS 17 - reference removed and run_path moved to projects_analyses
					if (projects.containsKey(p.run_label)){
						if (!p.run_path.equals(projects.get(p.run_label).run_path)){
							projects.get(p.run_label).kits.putAll(p.kits);
							projects.get(p.run_label).references.putAll(p.references);
						}
					}else{
						projects.put(p.run_label, p);
					}
				}
			}
			for (Project project : projects.values()){
				projectListModel.addElement(project);
			}
		}catch(Exception ex){
			Tools.exception(ex);
		}
	}

	private void addValues(){		
		for (Project project : projectList.getSelectedValuesList()){
			relaunchListModel.addElement(project);
			projectListModel.removeElement(project);
		}
	}

	private void removeValues(){
		for (Project project : relaunchList.getSelectedValuesList()){
			projectListModel.addElement(project);
			relaunchListModel.removeElement(project);
		}
	}

	public int getRelaunchListSize() {
		return relaunchListModel.getSize();
	}
	
	public Project getRelaunchListElement(int e) {
		return relaunchListModel.getElementAt(e);
	}
	
}
