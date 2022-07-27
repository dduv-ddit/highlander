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

package be.uclouvain.ngs.highlander.administration.UI;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.misc.WrapLayout;
import be.uclouvain.ngs.highlander.administration.UI.ProjectManager;

/**
* @author Raphael Helaers
*/

public class SettingsPanel extends ManagerPanel {
	
	private JComboBox<String> memoryBox;

	public SettingsPanel(ProjectManager manager){
		super(manager);

		JPanel memory = new JPanel(new WrapLayout(WrapLayout.LEADING));

		try {
			String appData = Tools.getApplicationDataFolder();
			String currentSetting = "";
			final List<File> vmoptions = new ArrayList<File>();
			if (Tools.isMac()){
				//Old installer java 6 (Apple bundler, install4j 4)
				//vmoptions.add(new File("Highlander.app/Contents/Info.plist"));
				//vmoptions.add(new File("highlander.vmoptions"));
				vmoptions.add(new File("AdminTools.app/Contents/vmoptions.txt"));
			}else if (Tools.isUnix()){
				vmoptions.add(new File("AdminTools.vmoptions"));
			}else{
				vmoptions.add(new File(appData + "admintools.vmoptions"));			
			}
			final int[] startXmx = new int[vmoptions.size()];
			final int[] endXmx = new int[vmoptions.size()];
			final int[] startXms = new int[vmoptions.size()];
			final int[] endXms = new int[vmoptions.size()];
			for (int i=0 ; i < vmoptions.size() ; i++){
				ProjectManager.toConsole("Using memory file " + vmoptions.get(i).getPath());
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
									if (i==0) currentSetting += line.charAt(endXmx[i]);
									endXmx[i]++;
								}
							}
							if (line.indexOf("Xms") >= 0){
								startXms[i] = line.indexOf("Xms")+3;
								endXms[i] = startXms[i];
								while (line.charAt(endXms[i]) != 'm'){
									//if (i==0) currentSetting += line.charAt(endXms[i]);
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
			memoryBox = new JComboBox<>(memoryOptions);
			memoryBox.setSelectedItem(currentSetting);
			memory.add(new JLabel("Set maximum memory allowed to Highlander Administration Tools : "));
			memory.add(memoryBox);
			memory.add(new JLabel("Mb"));
			JButton validate = new JButton("Apply");
			memory.add(validate);
			validate.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								Object newXmxValue = memoryBox.getSelectedItem();
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
									String text = "Memory settings changed, you must restart Highlander Adminstration Tools before it can take effect.";
									JOptionPane.showMessageDialog(new JFrame(), text, "Memory settings", JOptionPane.INFORMATION_MESSAGE, Resources.getScaledIcon(Resources.iMemory, 128));
								}
							}catch (Exception ex){
								Tools.exception(ex);
							}
						}
					}, "SettingsPanel.validate").start();
				}
			});
		}catch (Exception ex){
			Tools.exception(ex);
		}

		add(memory, BorderLayout.NORTH);
	}
	
}
