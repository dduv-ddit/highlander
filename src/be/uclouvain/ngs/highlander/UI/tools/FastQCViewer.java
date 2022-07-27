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

package be.uclouvain.ngs.highlander.UI.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.dialog.CreateRunSelection;
import be.uclouvain.ngs.highlander.UI.misc.WaitingPanel;
import be.uclouvain.ngs.highlander.UI.misc.WrapLayout;
import be.uclouvain.ngs.highlander.administration.DbBuilder.FastqcResult;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Report;
import be.uclouvain.ngs.highlander.datatype.RunNGS;

public class FastQCViewer extends JFrame {

	private JPanel panel_center = new JPanel(new BorderLayout(0,0));
	private JLabel label_run_selection = new JLabel("No run selected !");
	private JComboBox<String> boxField = new JComboBox<>(new String[]{
			"Per base sequence quality",
			"Per tile sequence quality",
			"Per sequence quality scores",
			"Per base sequence content",
			"Per sequence GC content",
			"Per base N content",
			"Sequence Length Distribution",
			"Sequence Duplication Levels",
			//"Overrepresented sequences", --> table, cannot fetch any image
			"Adapter Content",
	});
	private JComboBox<String> boxScale = new JComboBox<>(new String[]{
			"100%",
			"80%",
			"60%",
			"40%",
			"20%",
	});

	private Set<RunNGS> selectedRuns = new TreeSet<RunNGS>();

	static private WaitingPanel waitingPanel;
	private final Report fastqcReport;
	
	public FastQCViewer(Set<RunNGS> selection, Report fastqcReport) {
		this.fastqcReport = fastqcReport;
		initUI();
		showSelection(selection);
	}

	private void showSelection(Set<RunNGS> selection){
		if (!selection.isEmpty()){
			selectedRuns = selection;
			if (selectedRuns.size() > 0){
				label_run_selection.setText(selectedRuns.size() + " runs selected");
			}else{
				label_run_selection.setText("No run selected !");						
			}
			label_run_selection.repaint();
		}
		new Thread(new Runnable(){
			public void run(){
				showView(boxField.getSelectedItem().toString());
			}
		}, "FastQCViewer.showView").start();
	}

	private void initUI(){
		setTitle("FastQC reports");
		setIconImage(Resources.getScaledIcon(Resources.iFastQC, 64).getImage());

		setLayout(new BorderLayout());

		getContentPane().add(panel_center, BorderLayout.CENTER);

		JPanel panel_south = new JPanel();	
		getContentPane().add(panel_south, BorderLayout.SOUTH);

		JButton export = new JButton(Resources.getScaledIcon(Resources.iExportJpeg, 24));
		export.setToolTipText("Export current view to image file");
		export.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					public void run(){
						export();
					}
				}, "FastQCViewer.export").start();
			}
		});
		panel_south.add(export);

		JButton btnClose = new JButton(Resources.getScaledIcon(Resources.iCross, 24));
		btnClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				dispose();
			}
		});
		panel_south.add(btnClose);

		JPanel panel_north = new JPanel();
		getContentPane().add(panel_north, BorderLayout.NORTH);

		JButton btnSelect = new JButton("Select NGS runs to include in the chart",Resources.getScaledIcon(Resources.i3dPlus, 24));
		btnSelect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				CreateRunSelection select = new CreateRunSelection(selectedRuns);
				Tools.centerWindow(select, false);
				select.setVisible(true);
				showSelection(select.getSelection());
			}
		});
		panel_north.add(btnSelect);

		panel_north.add(label_run_selection);

		boxField.setMaximumRowCount(10);
		boxField.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					if (!selectedRuns.isEmpty()){
						new Thread(new Runnable(){
							public void run(){
								showView(boxField.getSelectedItem().toString());
							}
						}, "FastQCViewer.showView").start();
					}
				}
			}
		});
		panel_north.add(boxField);

		boxScale.setMaximumRowCount(10);
		boxScale.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					if (!selectedRuns.isEmpty()){
						new Thread(new Runnable(){
							public void run(){
								showView(boxField.getSelectedItem().toString());
							}
						}, "FastQCViewer.showView").start();
					}
				}
			}
		});
		panel_north.add(boxScale);

		waitingPanel = new WaitingPanel();
		setGlassPane(waitingPanel);
	}

	public void showView(String field){
		try{
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(true);
					waitingPanel.start();
				}
			});

			String imageName = "";
			String fastqcField = "per_base_sequence_quality";
			switch(field){
			case "Per base sequence quality":
				imageName = "per_base_quality.png";
				fastqcField = "per_base_sequence_quality";
				break;
			case "Per sequence quality scores":
				imageName = "per_sequence_quality.png";
				fastqcField = "per_sequence_quality_scores";
				break;
			case "Per base sequence content":
				imageName = "per_base_sequence_content.png";
				fastqcField = "per_base_sequence_content";
				break;
			case "Per sequence GC content":
				imageName = "per_sequence_gc_content.png";
				fastqcField = "per_sequence_GC_content";
				break;
			case "Per base N content":
				imageName = "per_base_n_content.png";
				fastqcField = "per_base_N_content";
				break;
			case "Sequence Length Distribution":
				imageName = "sequence_length_distribution.png";
				fastqcField = "sequence_length_distribution";
				break;
			case "Sequence Duplication Levels":
				imageName = "duplication_levels.png";
				fastqcField = "sequence_duplication_levels";
				break;
			case "Per tile sequence quality":
				imageName = "per_tile_quality.png";
				fastqcField = "per_tile_sequence_quality";
				break;
			case "Adapter Content":
				imageName = "adapter_content.png";
				fastqcField = "adapter_content";
				break;
			case "Per base GC content":
				imageName = "per_base_gc_content.png";
				fastqcField = "per_base_GC_content";
				break;
			case "Kmer Content":
				imageName = "kmer_profiles.png";
				fastqcField = "kmer_content";
				break;
			}

			JPanel view = new JPanel(new WrapLayout());
			Map<String, Map<String, FastqcResult>> projects = new TreeMap<>();
			for (RunNGS run : selectedRuns){
				Map<String, FastqcResult> samples = new TreeMap<>();
				try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT sample, "+fastqcField+" FROM projects WHERE run_id = '"+run.getRun_id()+"' AND run_date = '"+run.getRun_date()+"' AND run_name = '"+run.getRun_name()+"'")) {
					while (res.next()){
						samples.put(res.getString("sample"), FastqcResult.valueOf(res.getString(fastqcField)));
					}
				}
				projects.put(run.getRunLabel(), samples);
			}
			for (String project : projects.keySet()){
				for (String sample : projects.get(project).keySet()){
					JPanel panel = new JPanel(new BorderLayout());
					JPanel top = new JPanel(new BorderLayout());
					FastqcResult value = projects.get(project).get(sample);
					if (value.toString().equalsIgnoreCase("pass")) top.setBackground(Color.green);
					else if (value.toString().equalsIgnoreCase("warn")) top.setBackground(Color.orange);
					else if (value.toString().equalsIgnoreCase("fail")) top.setBackground(Color.red);
					top.add(new JLabel(project, SwingConstants.CENTER), BorderLayout.NORTH);
					top.add(new JLabel(sample, SwingConstants.CENTER), BorderLayout.SOUTH);
					panel.add(top, BorderLayout.NORTH);
					String url = Highlander.getParameters().getUrlForReports()+"/"+project+"/"+fastqcReport.getPath()+"/"+sample+"/Images/"+imageName;
					if (Tools.exists(url)){
						ImageIcon image = new ImageIcon(new URL(url));
						int width = image.getIconWidth();
						int height = image.getIconHeight();
						switch(boxScale.getSelectedItem().toString()){
						case "20%":
							image = new ImageIcon(image.getImage().getScaledInstance((int)(width*0.2),(int)(height*0.2),  java.awt.Image.SCALE_SMOOTH));
							break;
						case "40%":
							image = new ImageIcon(image.getImage().getScaledInstance((int)(width*0.4),(int)(height*0.4),  java.awt.Image.SCALE_SMOOTH));
							break;
						case "60%":
							image = new ImageIcon(image.getImage().getScaledInstance((int)(width*0.6),(int)(height*0.6),  java.awt.Image.SCALE_SMOOTH));
							break;
						case "80%":
							image = new ImageIcon(image.getImage().getScaledInstance((int)(width*0.8),(int)(height*0.8),  java.awt.Image.SCALE_SMOOTH));
							break;
						}						
						panel.add(new JLabel(image), BorderLayout.CENTER);
					}else{
						panel.add(new JLabel(Resources.iButtonApply), BorderLayout.CENTER);
					}
					view.add(panel);
				}
			}


			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setVisible(false);
					waitingPanel.stop();
				}
			});
			panel_center.removeAll();
			panel_center.add(new JScrollPane(view), BorderLayout.CENTER);
			validate();
			repaint();
		}catch(Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(this, Tools.getMessage("Error when retreiving FastQC reports", ex), "Retreiving " + field, JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
	}

	public void export(){
		if (panel_center.getComponents().length > 0){
			JPanel view = (JPanel)(((JScrollPane)panel_center.getComponents()[0]).getViewport().getComponents()[0]);
			Object format = JOptionPane.showInputDialog(this, "Choose an image format: ", "Export view to image file", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iExportJpeg, 64), ImageIO.getWriterFileSuffixes(), "png");
			if (format != null){
				FileDialog chooser = new FileDialog(this, "Export view to image", FileDialog.SAVE) ;
				Tools.centerWindow(chooser, false);
				chooser.setVisible(true) ;
				if (chooser.getFile() != null) {
					try {
						String filename = chooser.getDirectory() + chooser.getFile();
						if (!filename.toLowerCase().endsWith("."+format.toString())) filename += "."+format.toString();      
						BufferedImage image = new BufferedImage(view.getWidth(), view.getHeight(), BufferedImage.TYPE_INT_RGB);
						Graphics2D g = image.createGraphics();
						view.paintComponents(g);
						ImageIO.write(image, format.toString(), new File(filename));
					} catch (Exception ex) {
						Tools.exception(ex);
						JOptionPane.showMessageDialog(this, Tools.getMessage("Error when exporting chart", ex), "Export chart to image file", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));  			}
				}   		
			}
		}
	}
}
