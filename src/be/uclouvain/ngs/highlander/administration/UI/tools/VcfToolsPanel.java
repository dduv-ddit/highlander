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
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.misc.WrapLayout;
import be.uclouvain.ngs.highlander.administration.DbBuilder;
import be.uclouvain.ngs.highlander.administration.DbBuilder.FileType;
import be.uclouvain.ngs.highlander.administration.UI.ManagerPanel;
import be.uclouvain.ngs.highlander.administration.UI.ProjectManager;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;

/**
* @author Raphael Helaers
*/

public class VcfToolsPanel extends ManagerPanel {
	
	private JTextField conversionVCF;
	private JTextField conversionAlamuts;
	private JLabel conversionAnnotate;
	private JComboBox<AnalysisFull> conversionAnalysis;
	private List<JRadioButton> conversionFileTypes = new ArrayList<>();

	public VcfToolsPanel(ProjectManager manager){
		super(manager);
		JPanel scrollPanel = new JPanel(new GridBagLayout());
		int row = 0;
		scrollPanel.add(getTabConversion(), new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
		scrollPanel.add(getTabClean(), new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
		scrollPanel.add(new JPanel(), new GridBagConstraints(0, row++, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
		JScrollPane scroll = new  JScrollPane(scrollPanel);
		add(scroll);
	}
	
	private JPanel getTabConversion(){
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createTitledBorder("VCF conversion to"));

		JPanel vcfPanel = new JPanel(new WrapLayout(WrapLayout.LEADING, 10, 5));
		vcfPanel.add(new JLabel("VCF file to convert: "));
		conversionVCF = new JTextField();
		conversionVCF.setColumns(30);
		JButton browseVcfButton = new JButton("BROWSE");
		browseVcfButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				FileDialog chooser = new FileDialog(new JFrame(), "Choose a VCF file to convert", FileDialog.LOAD) ;
				Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize() ;
				Dimension windowSize = chooser.getSize() ;
				chooser.setLocation(Math.max(0, (screenSize.width - windowSize.width) / 2),
						Math.max(0, (screenSize.height - windowSize.height) / 2)) ;
				chooser.setVisible(true) ;
				if (chooser.getFile() != null) {
					conversionVCF.setText(chooser.getDirectory() + chooser.getFile());
				}
			}			
		});
		vcfPanel.add(conversionVCF);
		vcfPanel.add(browseVcfButton);

		JPanel analysisPanel = new JPanel(new BorderLayout());
		JPanel referencePanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		conversionAnnotate = new JLabel("Annotate variants with Highlander external sources (DBNSFP, ExAC, etc) and reference from analysis ");
		referencePanel.add(conversionAnnotate);
		conversionAnalysis = new JComboBox<>(manager.getAvailableAnalysesAsArray());
		referencePanel.add(conversionAnalysis);
		analysisPanel.add(referencePanel, BorderLayout.NORTH);
		
		JPanel alamutPanel = new JPanel(new WrapLayout(WrapLayout.LEADING, 10, 5));
		alamutPanel.add(new JLabel("Alamut annotations (optional, left blank to skip): "));
		conversionAlamuts = new JTextField();
		conversionAlamuts.setColumns(30);
		conversionAlamuts.setToolTipText("Alamut annotations text file generated using the same VCF");
		JButton browseAlamutButton = new JButton("BROWSE");
		browseAlamutButton.setToolTipText("Alamut annotations text file generated using the same VCF");
		browseAlamutButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				FileDialog chooser = new FileDialog(new JFrame(), "Choose an Alamut annotations file", FileDialog.LOAD) ;
				Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize() ;
				Dimension windowSize = chooser.getSize() ;
				chooser.setLocation(Math.max(0, (screenSize.width - windowSize.width) / 2),
						Math.max(0, (screenSize.height - windowSize.height) / 2)) ;
				chooser.setVisible(true) ;
				if (chooser.getFile() != null) {
					conversionAlamuts.setText(chooser.getDirectory() + chooser.getFile());
				}
			}			
		});
		alamutPanel.add(conversionAlamuts);
		alamutPanel.add(browseAlamutButton);

		JPanel targetPanel = new JPanel(new WrapLayout(WrapLayout.LEADING, 10, 5));
		targetPanel.add(new JLabel("Convert to: "));
		ButtonGroup group = new ButtonGroup();
		conversionFileTypes.clear();
		for (FileType type : FileType.values()){
			JRadioButton radio = new JRadioButton(type.toString());
			group.add(radio);
			conversionFileTypes.add(radio);
			targetPanel.add(radio);
		}

		JPanel launchPanel = new JPanel(new WrapLayout(WrapLayout.LEADING, 10, 5));
		JButton launch = new JButton(" Convert ");
		launchPanel.add(launch);
		launch.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								waitingPanel.setVisible(true);
								waitingPanel.start();
							}
						});						
						manager.startRedirectSystemOut();
						try{
							FileType type = null;
							for (JRadioButton radio : conversionFileTypes){
								if (radio.isSelected()) {
									type = FileType.valueOf(radio.getText());
									break;
								}
							}
							File vcf = new File(conversionVCF.getText());
							File alamut = new File(conversionAlamuts.getText());
							AnalysisFull analysis = (AnalysisFull)conversionAnalysis.getSelectedItem();
							ProjectManager.toConsole("-----------------------------------------------------");
							ProjectManager.toConsole("Converting VCF to " + type);
							if (!vcf.exists()) {
								ProjectManager.toConsole("The selected VCF file ("+vcf+") does not exists !");
							}else{
								if (vcf.exists()) ProjectManager.toConsole("VCF file: " + vcf);
								if (alamut.exists()) ProjectManager.toConsole("Alamut file: " + alamut);
								else ProjectManager.toConsole("Not Alamut annotation file has been selected");
								ProjectManager.toConsole("Adding Highlander annotations using " + analysis + " reference and datasources");
								DbBuilder.convertTo(type, "unknown", vcf.getPath(), (alamut.exists()?alamut.getPath():null), analysis, false);
							}
						}catch(Exception ex){
							ProjectManager.toConsole(ex);
						}
						manager.stopRedirectSystemOut();
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								waitingPanel.setVisible(false);
								waitingPanel.stop();
							}
						});
					}
				}, "VcfToolsPanel.launch").start();
			}
		});

		int row = 0;
		panel.add(vcfPanel, new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		panel.add(analysisPanel, new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		panel.add(alamutPanel, new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		panel.add(targetPanel, new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		panel.add(launchPanel, new GridBagConstraints(0, row++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

		return panel;				
	}

	private JPanel getTabClean(){
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createTitledBorder("Clean VCF"));

		JPanel panelArguments = new JPanel(new BorderLayout());
		panel.add(panelArguments, BorderLayout.NORTH);


		JPanel panelLaunch = new JPanel(new WrapLayout(WrapLayout.LEADING, 10, 5));
		JButton cleanVCF = new JButton("Eliminate variants from a VCF with ref or alt equals to '-', '.' or ' '");
		panelLaunch.add(cleanVCF);
		panel.add(panelLaunch, BorderLayout.SOUTH);

		cleanVCF.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						FileDialog d = new FileDialog(manager, "Select a VCF file", FileDialog.LOAD);
						Tools.centerWindow(d, false);
						d.setVisible(true);
						if (d.getFile() != null){
							String filename = d.getDirectory() + d.getFile();
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									waitingPanel.setVisible(true);
									waitingPanel.start();
								}
							});						
							manager.startRedirectSystemOut();
							try{
								ProjectManager.toConsole("-----------------------------------------------------");
								ProjectManager.toConsole("Cleaning VCF (unwanted variants) " + filename);
								DbBuilder.cleanVCF(filename);
								ProjectManager.toConsole("Done");
							}catch(Exception ex){
								ProjectManager.toConsole(ex);
							}
							manager.stopRedirectSystemOut();
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									waitingPanel.setVisible(false);
									waitingPanel.stop();
								}
							});
						}
					}
				}, "VcfToolsPanel.cleanVCF").start();
			}
		});

		JButton cleanCHR = new JButton("Eliminate variants from a VCF with a chromosome different of (chr)1-22,X,Y");
		panelLaunch.add(cleanCHR);
		panel.add(panelLaunch, BorderLayout.SOUTH);

		cleanCHR.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						FileDialog d = new FileDialog(manager, "Select a VCF file", FileDialog.LOAD);
						Tools.centerWindow(d, false);
						d.setVisible(true);
						if (d.getFile() != null){
							String filename = d.getDirectory() + d.getFile();
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									waitingPanel.setVisible(true);
									waitingPanel.start();
								}
							});						
							manager.startRedirectSystemOut();
							try{
								ProjectManager.toConsole("-----------------------------------------------------");
								ProjectManager.toConsole("Cleaning VCF (unwanted chromosomes) " + filename);
								ProjectManager.toConsole("Done");
								DbBuilder.cleanChr(filename);
							}catch(Exception ex){
								ProjectManager.toConsole(ex);
							}
							manager.stopRedirectSystemOut();
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									waitingPanel.setVisible(false);
									waitingPanel.stop();
								}
							});
						}
					}
				}, "VcfToolsPanel.cleanCHR").start();
			}
		});

		return panel;				
	}

}
