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

package be.uclouvain.ngs.highlander.UI.toolbar;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.misc.ToolbarScrollablePanel;
import be.uclouvain.ngs.highlander.UI.table.WelcomePage;

public class HelpPanel extends JPanel {

	public HelpPanel(){
		initUI();
	}

	private void initUI(){
		setLayout(new BorderLayout(0,0));

		JButton setMemory = new JButton(Resources.getScaledIcon(Resources.iMemory, 40));
		setMemory.setPreferredSize(new Dimension(54,54));
		setMemory.setToolTipText("Memory settings");
		setMemory.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						memorySettings();
					}
				}, "HelpPanel.memorySettings").start();
			}
		});

		JButton helpLastChanges = new JButton(Resources.getScaledIcon(Resources.iUpdater, 40));
		helpLastChanges.setPreferredSize(new Dimension(54,54));
		helpLastChanges.setToolTipText("Last changes");
		helpLastChanges.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						last();
					}
				}, "HelpPanel.last").start();
			}
		});

		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING));

		panel.add(setMemory);	  
		panel.add(helpLastChanges);

		ToolbarScrollablePanel scrollablePanel = new ToolbarScrollablePanel(panel, Highlander.getHighlanderObserver(), 40);
		add(scrollablePanel, BorderLayout.CENTER);
	}

	public void memorySettings(){
		try {
			String appData = Tools.getApplicationDataFolder();
			String currentSettingXmx = "";
			//String currentSettingXms = "";
			List<File> vmoptions = new ArrayList<File>();
			if (Tools.isMac()){
				//Old installer java 6 (Apple bundler, install4j 4)
				//vmoptions.add(new File("Highlander.app/Contents/Info.plist"));
				//vmoptions.add(new File("highlander.vmoptions"));
				vmoptions.add(new File("Highlander.app/Contents/vmoptions.txt"));
			}else if (Tools.isUnix()){
				vmoptions.add(new File("Highlander.vmoptions"));
			}else{
				vmoptions.add(new File(appData + "highlander.vmoptions"));			
			}
			int[] startXmx = new int[vmoptions.size()];
			int[] endXmx = new int[vmoptions.size()];
			int[] startXms = new int[vmoptions.size()];
			int[] endXms = new int[vmoptions.size()];
			for (int i=0 ; i < vmoptions.size() ; i++){
				System.out.println("modifying memory file " + vmoptions.get(i).getPath());
				try (FileReader fr = new FileReader(vmoptions.get(i))){
					try (BufferedReader br = new BufferedReader(fr)){
						String line;
						startXmx[i]=0;
						endXmx[i]=0;
						startXms[i]=0;
						endXms[i]=0;
						while ((line = br.readLine()) != null){
							if (line.indexOf("Xmx") >= 0){
								startXmx[i] = line.indexOf("Xmx")+3;
								endXmx[i] = startXmx[i];
								while (line.charAt(endXmx[i]) != 'm'){
									if (i==0) currentSettingXmx += line.charAt(endXmx[i]);
									endXmx[i]++;
								}
							}
							if (line.indexOf("Xms") >= 0){
								startXms[i] = line.indexOf("Xms")+3;
								endXms[i] = startXms[i];
								while (line.charAt(endXms[i]) != 'm'){
									//if (i==0) currentSettingXms += line.charAt(endXms[i]);
									endXms[i]++;
								}
							}
						}
					}
				}
			}
			List<String> availableAmounts = new ArrayList<String>();
			for (int val = 256 ; val < (Tools.getMaxPhysicalMemory()-500) ; val+=256){
				availableAmounts.add(""+val);
			}
			String[] memoryOptions = availableAmounts.toArray(new String[0]);
			Object newXmxValue = JOptionPane.showInputDialog(this, "Set maximum memory allowed to Highlander : ", "Memory settings", 
					JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iMemory, 128), memoryOptions, currentSettingXmx);
			if (newXmxValue != null){
				int newXmsValue = Math.min((Integer.parseInt(newXmxValue.toString()))-256,2304);
				for (int i=0 ; i < vmoptions.size() ; i++){
					File tempOutput = File.createTempFile("settings", ".temp") ;
					tempOutput.deleteOnExit() ;
					try (FileWriter fw = new FileWriter(tempOutput)){
						try (FileReader fr = new FileReader(vmoptions.get(i))){
							try (BufferedReader br = new BufferedReader(fr)){
								String line;
								while ((line = br.readLine()) != null){
									if (line.indexOf("Xmx") >= 0 && line.indexOf("Xms") >= 0){
										int[] startFirst = (startXms[i] < startXmx[i]) ? startXms : startXmx;
										int[] startLast = (startXms[i] < startXmx[i]) ? startXmx : startXms;							
										int[] endFirst = (startXms[i] < startXmx[i]) ? endXms : endXmx;
										int[] endLast = (startXms[i] < startXmx[i]) ? endXmx : endXms;
										String first = (startXms[i] < startXmx[i]) ? ""+newXmsValue : newXmxValue.toString();
										String last = (startXms[i] < startXmx[i]) ? newXmxValue.toString() : ""+newXmsValue;
										fw.write(line.substring(0, startFirst[i]));
										fw.write(first);
										fw.write(line.substring(endFirst[i], startLast[i]));
										fw.write(last);
										fw.write(line.substring(endLast[i]) + "\n");
									}else if (line.indexOf("Xmx") >= 0){
										fw.write(line.substring(0, startXmx[i]));
										fw.write(newXmxValue.toString());
										fw.write(line.substring(endXmx[i]) + "\n");
									}else	if (line.indexOf("Xms") >= 0){
										fw.write(line.substring(0, startXms[i]));
										fw.write(""+newXmsValue);
										fw.write(line.substring(endXms[i]) + "\n");
									}else{
										fw.write(line + "\n");
									}
								}
							}
						}
					}
					vmoptions.get(i).delete();
					tempOutput.renameTo(vmoptions.get(i));
				}
				String text = "Memory settings changed, you must restart Highlander before it can take effect.";
				JOptionPane.showMessageDialog(this, text, "Memory settings", JOptionPane.INFORMATION_MESSAGE, Resources.getScaledIcon(Resources.iMemory, 128));
			}
		}catch (Exception ex){
			Tools.exception(ex);
		}
	}

	public void last(){
		JFrame dlg = new JFrame();
		dlg.setIconImage(Resources.getScaledIcon(Resources.iUpdater, 64).getImage());
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);	
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);	
		WelcomePage startTxt = new WelcomePage(false);
		scrollPane.setViewportView(startTxt);	
		dlg.getContentPane().add(scrollPane, BorderLayout.CENTER);
		dlg.pack();
		Tools.centerWindow(dlg, false);
		dlg.setExtendedState(Highlander.MAXIMIZED_BOTH);
		dlg.setVisible(true);
	}


}
