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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.misc.WrapLayout;
import be.uclouvain.ngs.highlander.administration.UI.ManagerPanel;
import be.uclouvain.ngs.highlander.administration.UI.ProjectManager;
import be.uclouvain.ngs.highlander.database.DBUtils;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.Field.Annotation;
import be.uclouvain.ngs.highlander.database.Field.AnnotationType;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;

/**
* @author Raphael Helaers
*/

public class UpdatePanel extends ManagerPanel {
	
	List<JCheckBox> annotationsAnalyses = new ArrayList<JCheckBox>();
	List<JCheckBox> annotationsSchema = new ArrayList<JCheckBox>();
	JSpinner annotationsThread = new JSpinner(new SpinnerNumberModel(1, 1, Runtime.getRuntime().availableProcessors(), 1));

	List<JCheckBox> numEvalAnalyses = new ArrayList<JCheckBox>();

	List<JCheckBox> transferAnnotationsFromAnalyses = new ArrayList<JCheckBox>();
	List<JCheckBox> transferAnnotationsToAnalyses = new ArrayList<JCheckBox>();
	
	List<JCheckBox> updateErrorsAnalyses = new ArrayList<JCheckBox>();
	JTextField updateErrorsRepository = new JTextField();

	public UpdatePanel(ProjectManager manager){
		super(manager);
		JPanel scrollPanel = new JPanel(new GridBagLayout());
		int row = 0;
		scrollPanel.add(getTabSoftupdate(), new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
		scrollPanel.add(getTabUpdateAnnotations(), new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
		scrollPanel.add(getTabRebuildUserAnnotationsNumEvaluations(), new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
		scrollPanel.add(getTabTransferUserAnnotations(), new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
		scrollPanel.add(new JPanel(), new GridBagConstraints(0, row++, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
		JScrollPane scroll = new  JScrollPane(scrollPanel);
		add(scroll);
	}

	private JPanel getTabSoftupdate(){
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createTitledBorder("Set the Highlander database in 'update' mode (a warning is displayed in the client GUI)"));
		JPanel panel_north = new JPanel(new WrapLayout(FlowLayout.LEADING));
		panel_north.add(new JLabel(" Current status of Highlander: "));
		final JLabel databaseLoadLabel = new JLabel();
		panel_north.add(databaseLoadLabel);
		new Thread(new Runnable() {
			@Override
			public void run() {
				try{
					while (!DB.getDataSource(Schema.HIGHLANDER).isClosed()){
						boolean soft = false; 
						boolean hard = false; 
						try (Results res = DB.select(Schema.HIGHLANDER, "SELECT update_soft, update_hard FROM main", false)) {
							if (res.next()){
								soft = res.getBoolean("update_soft");
								hard = res.getBoolean("update_hard");
							}
						}
						if (hard){
							databaseLoadLabel.setIcon(Resources.getScaledIcon(Resources.iShinyBallRed, 24));
							databaseLoadLabel.setText("Database is being updated, launching queries is strongly discouraged !");
						}else if (soft){
							databaseLoadLabel.setIcon(Resources.getScaledIcon(Resources.iShinyBallOrange, 24));
							databaseLoadLabel.setText("New samples are being processed by the pipeline");
						}else{
							databaseLoadLabel.setIcon(Resources.getScaledIcon(Resources.iShinyBallGreen, 24));
							databaseLoadLabel.setText("Database is ready");
						}
						Thread.sleep(60_000);
					}
				}catch(Exception ex){
					Tools.exception(ex);
				}				
			}
		}, "UpdatePanel.databaseStatus").start();
		panel.add(panel_north, BorderLayout.NORTH);
		JPanel panel_south = new JPanel(new WrapLayout(FlowLayout.LEADING));
		JButton normalButton = new JButton("Deactivate soft and hard update", Resources.getScaledIcon(Resources.iShinyBallGreen, 24));
		normalButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						try{
							ProjectManager.getDbUpdater().setSoftUpdate(false);
							ProjectManager.getDbUpdater().setHardUpdate(false);
						}catch(Exception ex){
							ProjectManager.toConsole(ex);
						}
					}
				}, "UpdatePanel.setNormal").start();
			}
		});
		panel_south.add(normalButton);
		JButton softButton = new JButton("Activate soft update", Resources.getScaledIcon(Resources.iShinyBallOrange, 24));
		softButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						try{
							ProjectManager.getDbUpdater().setSoftUpdate(true);
						}catch(Exception ex){
							ProjectManager.toConsole(ex);
						}
					}
				}, "UpdatePanel.setSoftUpdate").start();
			}
		});
		panel_south.add(softButton);
		JButton hardButton = new JButton("Activate hard update", Resources.getScaledIcon(Resources.iShinyBallRed, 24));
		hardButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						try{
							ProjectManager.getDbUpdater().setHardUpdate(true);
						}catch(Exception ex){
							ProjectManager.toConsole(ex);
						}
					}
				}, "UpdatePanel.setHardUpdate").start();
			}
		});
		panel_south.add(hardButton);
		panel.add(panel_south, BorderLayout.SOUTH);
		return panel;
	}

	private JPanel getTabUpdateAnnotations(){
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createTitledBorder("Update all variant static annotations relative to given datasources in the given analyses"));

		JPanel panelArguments = new JPanel(new BorderLayout());
		panel.add(panelArguments, BorderLayout.NORTH);

		JPanel panelAnalyses = new JPanel(new WrapLayout(FlowLayout.LEADING, 10, 5));
		panelAnalyses.add(new JLabel("Analyses: "));
		for (Analysis a : manager.getAvailableAnalysesAsArray()){
			JCheckBox check = new JCheckBox(a.toString());
			annotationsAnalyses.add(check);
			panelAnalyses.add(check);
		}
		panelAnalyses.setSize(new Dimension(300,1));
		panelArguments.add(panelAnalyses, BorderLayout.NORTH);

		JPanel panelSchema = new JPanel(new WrapLayout(FlowLayout.LEADING, 10, 5));		
		panelSchema.add(new JLabel("Annotations: "));
		for (Annotation a : Annotation.getAnnotations(AnnotationType.STATIC)){
			JCheckBox check = new JCheckBox(a.toString());
			annotationsSchema.add(check);
			panelSchema.add(check);
		}
		panelSchema.setSize(new Dimension(300,1));
		panelArguments.add(panelSchema, BorderLayout.SOUTH);

		JPanel panelLaunch = new JPanel(new WrapLayout(FlowLayout.LEADING, 10, 5));
		panelLaunch.add(new JLabel("Number of threads: "));
		panelLaunch.add(annotationsThread);
		JButton launch = new JButton("  Update annotations ");
		panelLaunch.add(launch);
		panelLaunch.setSize(new Dimension(300,1));
		panel.add(panelLaunch, BorderLayout.SOUTH);

		launch.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						List<AnalysisFull> analyses = new ArrayList<>();
						for (JCheckBox box : annotationsAnalyses){
							if (box.isSelected()){
								for (AnalysisFull a : manager.getAvailableAnalysesAsArray()){
									if (a.toString().equalsIgnoreCase(box.getText())){
										analyses.add(a);
									}
								}
							}
						}
						Set<Annotation> annotations = new HashSet<>();
						for (JCheckBox box : annotationsSchema){
							if (box.isSelected()){
								annotations.add(Annotation.valueOf(box.getText()));
							}
						}
						int nthreads = (Integer)annotationsThread.getValue();
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								waitingPanel.setVisible(true);
								waitingPanel.start();
							}
						});
						manager.startRedirectSystemOut();
						try{
							ProjectManager.getDbUpdater().updateAnnotations(analyses, annotations, nthreads);
						}catch(Exception ex){
							ProjectManager.toConsole(ex);
						}
						manager.stopRedirectSystemOut();
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								waitingPanel.setVisible(false);
								waitingPanel.stop();
							}
						});
					}
				}, "UpdatePanel.updateAnnotations").start();
			}
		});

		return panel;		
	}

	private JPanel getTabRebuildUserAnnotationsNumEvaluations(){
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createTitledBorder("Rebuild user annotations num evaluations"));

		JPanel panelArguments = new JPanel(new BorderLayout());
		panel.add(panelArguments, BorderLayout.NORTH);

		JPanel panelAnalyses = new JPanel(new WrapLayout(FlowLayout.LEADING, 10, 5));		
		panelAnalyses.add(new JLabel("Analyses: "));
		for (Analysis a : manager.getAvailableAnalysesAsArray()){
			JCheckBox check = new JCheckBox(a.toString());
			numEvalAnalyses.add(check);
			panelAnalyses.add(check);
		}
		panelArguments.add(panelAnalyses, BorderLayout.NORTH);


		JPanel panelLaunch = new JPanel(new WrapLayout(FlowLayout.LEADING, 10, 5));
		JButton launch = new JButton("  Update [analysis]_user_annotations_num_evaluations ");
		panelLaunch.add(launch);
		panel.add(panelLaunch, BorderLayout.SOUTH);

		launch.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						List<Analysis> analyses = new ArrayList<>();
						for (JCheckBox box : numEvalAnalyses){
							if (box.isSelected()){
								for (Analysis a : manager.getAvailableAnalysesAsArray()){
									if (a.toString().equalsIgnoreCase(box.getText())){
										analyses.add(a);
									}
								}
							}
						}
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								waitingPanel.setVisible(true);
								waitingPanel.start();
							}
						});						
						manager.startRedirectSystemOut();
						try{
							for (Analysis analysis : analyses) {
								ProjectManager.toConsole("-----------------------------------------------------");
								ProjectManager.toConsole("Rebuilding " + analysis.getTableUserAnnotationsNumEvaluations());
								DBUtils.rebuildUserAnnotationsNumEvaluations(analysis);
							}
						}catch(Exception ex){
							ProjectManager.toConsole(ex);
						}
						manager.stopRedirectSystemOut();
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								waitingPanel.setVisible(false);
								waitingPanel.stop();
							}
						});
					}
				}, "UpdatePanel.rebuildUserAnnotationsNumEvaluations").start();
			}
		});

		return panel;				
	}

	private JPanel getTabTransferUserAnnotations(){
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createTitledBorder("Transfer user annotations between 2 analyses"));

		JPanel panelArguments = new JPanel(new BorderLayout());
		panel.add(panelArguments, BorderLayout.NORTH);

		JPanel panelAnalyses = new JPanel(new BorderLayout());
		panelArguments.add(panelAnalyses, BorderLayout.NORTH);
		
		JPanel panelFrom = new JPanel(new WrapLayout(FlowLayout.LEADING, 10, 5));		
		panelFrom.add(new JLabel("Transfer FROM: "));
		ButtonGroup groupFrom = new ButtonGroup();
		for (Analysis a : manager.getAvailableAnalysesAsArray()){
			JCheckBox check = new JCheckBox(a.toString());
			transferAnnotationsFromAnalyses.add(check);
			panelFrom.add(check);
			groupFrom.add(check);
		}
		panelAnalyses.add(panelFrom, BorderLayout.NORTH);

		JPanel panelTo = new JPanel(new WrapLayout(FlowLayout.LEADING, 10, 5));		
		panelTo.add(new JLabel("Transfer TO: "));
		ButtonGroup groupTo = new ButtonGroup();
		for (Analysis a : manager.getAvailableAnalysesAsArray()){
			JCheckBox check = new JCheckBox(a.toString());
			transferAnnotationsToAnalyses.add(check);
			panelTo.add(check);
			groupTo.add(check);
		}
		panelAnalyses.add(panelTo, BorderLayout.SOUTH);
		
		JPanel panelLaunch = new JPanel(new WrapLayout(FlowLayout.LEADING, 10, 5));
		panel.add(panelLaunch, BorderLayout.SOUTH);

		JButton launchVariants = new JButton("  Transfer user annotations on variants ");
		panelLaunch.add(launchVariants);
		launchVariants.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						AnalysisFull from = null;
						for (JCheckBox box : transferAnnotationsFromAnalyses){
							if (box.isSelected()){
								for (AnalysisFull a : manager.getAvailableAnalysesAsArray()){
									if (a.toString().equalsIgnoreCase(box.getText())){
										from = a;
										break;
									}
								}
							}
						}
						AnalysisFull to = null;
						for (JCheckBox box : transferAnnotationsToAnalyses){
							if (box.isSelected()){
								for (AnalysisFull a : manager.getAvailableAnalysesAsArray()){
									if (a.toString().equalsIgnoreCase(box.getText())){
										to = a;
										break;
									}
								}
							}
						}
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								waitingPanel.setVisible(true);
								waitingPanel.start();
							}
						});						
						manager.startRedirectSystemOut();
						try{
							ProjectManager.toConsole("-----------------------------------------------------");
							ProjectManager.toConsole("Transfering variant user annotations from " + from.getTableUserAnnotationsVariants() + " to " + to.getTableUserAnnotationsVariants());
							DBUtils.transferUserAnnotationsVariants(from, to);
						}catch(Exception ex){
							ProjectManager.toConsole(ex);
						}
						manager.stopRedirectSystemOut();
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								waitingPanel.setVisible(false);
								waitingPanel.stop();
							}
						});
					}
				}, "UpdatePanel.transferUserAnnotationsVariants").start();
			}
		});

		JButton launchGenes = new JButton("  Transfer user annotations on genes ");
		panelLaunch.add(launchGenes);
		launchGenes.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						Analysis from = null;
						for (JCheckBox box : transferAnnotationsFromAnalyses){
							if (box.isSelected()){
								for (Analysis a : manager.getAvailableAnalysesAsArray()){
									if (a.toString().equalsIgnoreCase(box.getText())){
										from = a;
										break;
									}
								}
							}
						}
						Analysis to = null;
						for (JCheckBox box : transferAnnotationsToAnalyses){
							if (box.isSelected()){
								for (Analysis a : manager.getAvailableAnalysesAsArray()){
									if (a.toString().equalsIgnoreCase(box.getText())){
										to = a;
										break;
									}
								}
							}
						}
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								waitingPanel.setVisible(true);
								waitingPanel.start();
							}
						});						
						manager.startRedirectSystemOut();
						try{
							ProjectManager.toConsole("-----------------------------------------------------");
							ProjectManager.toConsole("Transfering gene user annotations from " + from.getTableUserAnnotationsGenes() + " to " + to.getTableUserAnnotationsGenes());
							DBUtils.transferUserAnnotationsGenes(from, to);
						}catch(Exception ex){
							ProjectManager.toConsole(ex);
						}
						manager.stopRedirectSystemOut();
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								waitingPanel.setVisible(false);
								waitingPanel.stop();
							}
						});
					}
				}, "UpdatePanel.transferUserAnnotationsGenes").start();
			}
		});
		
		JButton launchSamples = new JButton("  Transfer user annotations on samples ");
		panelLaunch.add(launchSamples);
		launchSamples.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						Analysis from = null;
						for (JCheckBox box : transferAnnotationsFromAnalyses){
							if (box.isSelected()){
								for (Analysis a : manager.getAvailableAnalysesAsArray()){
									if (a.toString().equalsIgnoreCase(box.getText())){
										from = a;
										break;
									}
								}
							}
						}
						Analysis to = null;
						for (JCheckBox box : transferAnnotationsToAnalyses){
							if (box.isSelected()){
								for (Analysis a : manager.getAvailableAnalysesAsArray()){
									if (a.toString().equalsIgnoreCase(box.getText())){
										to = a;
										break;
									}
								}
							}
						}
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								waitingPanel.setVisible(true);
								waitingPanel.start();
							}
						});						
						manager.startRedirectSystemOut();
						try{
							ProjectManager.toConsole("-----------------------------------------------------");
							ProjectManager.toConsole("Transfering sample user annotations from " + from.getTableUserAnnotationsSamples() + " to " + to.getTableUserAnnotationsSamples());
							DBUtils.transferUserAnnotationsSamples(from, to);
						}catch(Exception ex){
							ProjectManager.toConsole(ex);
						}
						manager.stopRedirectSystemOut();
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								waitingPanel.setVisible(false);
								waitingPanel.stop();
							}
						});
					}
				}, "UpdatePanel.transferUserAnnotationsSamples").start();
			}
		});
		
		JButton launchEvaluations = new JButton("  Transfer user evaluations on variants (for samples present in both analyses) ");
		panelLaunch.add(launchEvaluations);
		launchEvaluations.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						AnalysisFull from = null;
						for (JCheckBox box : transferAnnotationsFromAnalyses){
							if (box.isSelected()){
								for (AnalysisFull a : manager.getAvailableAnalysesAsArray()){
									if (a.toString().equalsIgnoreCase(box.getText())){
										from = a;
										break;
									}
								}
							}
						}
						AnalysisFull to = null;
						for (JCheckBox box : transferAnnotationsToAnalyses){
							if (box.isSelected()){
								for (AnalysisFull a : manager.getAvailableAnalysesAsArray()){
									if (a.toString().equalsIgnoreCase(box.getText())){
										to = a;
										break;
									}
								}
							}
						}
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								waitingPanel.setVisible(true);
								waitingPanel.start();
							}
						});						
						manager.startRedirectSystemOut();
						try{
							ProjectManager.toConsole("-----------------------------------------------------");
							ProjectManager.toConsole("Transfering variant user evaluations from " + from.getTableUserAnnotationsVariants() + " to " + to.getTableUserAnnotationsVariants());
							try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT sample, project_id FROM projects JOIN projects_analyses as fromA USING (project_id) JOIN projects_analyses as toA USING (project_id) WHERE fromA.analysis = '"+from+"' AND toA.analysis = '"+to+"'")){
								while (res.next()) {
									ProjectManager.toConsole(res.getString("sample"));
									DBUtils.transferUserAnnotationsEvaluations(from, to, res.getInt("project_id"), false);									
								}
							}
							DBUtils.rebuildUserAnnotationsNumEvaluations(to); 
						}catch(Exception ex){
							ProjectManager.toConsole(ex);
						}
						manager.stopRedirectSystemOut();
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								waitingPanel.setVisible(false);
								waitingPanel.stop();
							}
						});
					}
				}, "UpdatePanel.transferUserAnnotationsEvaluations").start();
			}
		});

		return panel;		
	}

}
