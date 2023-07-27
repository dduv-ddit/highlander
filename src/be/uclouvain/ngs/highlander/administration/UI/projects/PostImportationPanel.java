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
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import be.uclouvain.ngs.highlander.UI.misc.WrapLayout;
import be.uclouvain.ngs.highlander.administration.UI.ManagerPanel;
import be.uclouvain.ngs.highlander.administration.UI.ProjectManager;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;

/**
* @author Raphael Helaers
*/

public class PostImportationPanel extends ManagerPanel {

	List<JCheckBox> possibleValuesAnalyses = new ArrayList<JCheckBox>();

	List<JCheckBox> statisticsAnalyses = new ArrayList<JCheckBox>();

	public PostImportationPanel(ProjectManager manager){
		super(manager);
		
		JPanel scrollPanel = new JPanel(new GridBagLayout());
		int row = 0;
		scrollPanel.add(getTabAlleleFrequencies(), new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
		scrollPanel.add(getTabPossibleValues(), new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
		scrollPanel.add(getTabWarnUsers(), new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
		scrollPanel.add(new JPanel(), new GridBagConstraints(0, row++, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
		JScrollPane scroll = new  JScrollPane(scrollPanel);
		add(scroll);
	}

	private JPanel getTabWarnUsers(){
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createTitledBorder("Send an email to all users having a new sample recently imported"));

		JPanel panelArguments = new JPanel(new BorderLayout());
		panel.add(panelArguments, BorderLayout.NORTH);

		JPanel panelAnalyses = new JPanel(new WrapLayout(FlowLayout.LEADING, 10, 5));		
		panelAnalyses.add(new JLabel("Analyses: "));
		for (Analysis a : manager.getAvailableAnalysesAsArray()){
			JCheckBox check = new JCheckBox(a.toString());
			possibleValuesAnalyses.add(check);
			panelAnalyses.add(check);
		}
		panelArguments.add(panelAnalyses, BorderLayout.NORTH);


		JPanel panelLaunch = new JPanel(new WrapLayout(FlowLayout.LEADING, 10, 5));
		JButton launch = new JButton(" Send emails ");
		panelLaunch.add(launch);
		panel.add(panelLaunch, BorderLayout.SOUTH);

		launch.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						List<AnalysisFull> analyses = new ArrayList<>();
						for (JCheckBox box : possibleValuesAnalyses){
							if (box.isSelected()){
								for (AnalysisFull a : manager.getAvailableAnalysesAsArray()){
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
							ProjectManager.toConsole("-----------------------------------------------------");
							ProjectManager.toConsole("Checking for updated samples ...");
							ProjectManager.getDbBuilder().warnusers(analyses);
							ProjectManager.toConsole("Done");
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
				}, "PostImportationPanel.warnusers").start();
			}
		});

		return panel;				
	}

	private JPanel getTabPossibleValues(){
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createTitledBorder("Update possible values"));

		JPanel panelArguments = new JPanel(new BorderLayout());
		panel.add(panelArguments, BorderLayout.NORTH);

		JPanel panelAnalyses = new JPanel(new WrapLayout(FlowLayout.LEADING, 10, 5));		
		panelAnalyses.add(new JLabel("Analyses: "));
		for (Analysis a : manager.getAvailableAnalysesAsArray()){
			JCheckBox check = new JCheckBox(a.toString());
			possibleValuesAnalyses.add(check);
			panelAnalyses.add(check);
		}
		panelArguments.add(panelAnalyses, BorderLayout.NORTH);


		JPanel panelLaunch = new JPanel(new WrapLayout(FlowLayout.LEADING, 10, 5));
		JButton launch = new JButton(" Update possible values ");
		panelLaunch.add(launch);
		panel.add(panelLaunch, BorderLayout.SOUTH);

		launch.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						List<AnalysisFull> analyses = new ArrayList<>();
						for (JCheckBox box : possibleValuesAnalyses){
							if (box.isSelected()){
								for (AnalysisFull a : manager.getAvailableAnalysesAsArray()){
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
							ProjectManager.toConsole("-----------------------------------------------------");
							ProjectManager.getDbBuilder().computePossibleValues(analyses);
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
				}, "PostImportationPanel.computePossibleValues").start();
			}
		});

		return panel;				
	}

	private JPanel getTabAlleleFrequencies(){
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createTitledBorder("Allele frequencies update"));

		JPanel panelArguments = new JPanel(new BorderLayout());
		panel.add(panelArguments, BorderLayout.NORTH);

		JPanel panelAnalyses = new JPanel(new WrapLayout(FlowLayout.LEADING, 10, 5));		
		panelAnalyses.add(new JLabel("Analyses: "));
		for (Analysis a : manager.getAvailableAnalysesAsArray()){
			JCheckBox check = new JCheckBox(a.toString());
			statisticsAnalyses.add(check);
			panelAnalyses.add(check);
		}
		panelArguments.add(panelAnalyses, BorderLayout.NORTH);


		JPanel panelLaunch = new JPanel(new WrapLayout(FlowLayout.LEADING, 10, 5));
		JButton changeStatsButton = new JButton(" Update allele frequencies ");
		panelLaunch.add(changeStatsButton);
		panel.add(panelLaunch, BorderLayout.SOUTH);

		changeStatsButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						List<Analysis> analyses = new ArrayList<>();
						for (JCheckBox box : statisticsAnalyses){
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
							ProjectManager.toConsole("-----------------------------------------------------");
							ProjectManager.getDbBuilder().computeAlleleFrequencies(analyses, false);
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
				}, "PostImportationPanel.updateAlleleFrequencies").start();
			}
		});

		JButton geneStatsButton = new JButton(" Update allele frequencies per pathology ");
		panelLaunch.add(geneStatsButton);
		panel.add(panelLaunch, BorderLayout.SOUTH);

		geneStatsButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					@Override
					public void run(){
						List<Analysis> analyses = new ArrayList<>();
						for (JCheckBox box : statisticsAnalyses){
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
							ProjectManager.toConsole("-----------------------------------------------------");
							ProjectManager.getDbBuilder().computeAlleleFrequencies(analyses, true);
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
				}, "PostImportationPanel.updateAlleleFrequenciesPerPathology").start();
			}
		});

		panel.add(panelLaunch, BorderLayout.SOUTH);

		return panel;				
	}


}
