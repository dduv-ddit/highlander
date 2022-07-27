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

package be.uclouvain.ngs.highlander.administration.script;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jcraft.jsch.ChannelExec;

import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.administration.UI.Project;
import be.uclouvain.ngs.highlander.administration.UI.ProjectManager;

public abstract class Script {
	
	protected ProjectManager manager;
	protected String scriptFile = "";
	protected Map<String,String> arguments = new LinkedHashMap<>();
	protected List<JCheckBox> checkBoxes = new ArrayList<>();
	protected Map<String,JTextField> argumentsValues = new LinkedHashMap<>();
	protected Set<String> compatibleAnalysis = new TreeSet<>(); 
	
	public Script(ProjectManager manager){
		this.manager = manager;
	}
	
	public JPanel getArgumentsPanel(){
		JPanel panel = new JPanel(new GridBagLayout());
		int y=0;
		JPanel panel_S = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel label_S = new JLabel("Script");
		final JTextField input_S = new JTextField(scriptFile);
		input_S.setToolTipText("Script to launch on the server");
		panel_S.add(label_S);
		panel_S.add(input_S);
		panel.add(panel_S, new GridBagConstraints(0, y++, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(1, 5, 1, 5), 0, 0));
		for (String text : arguments.keySet()){
			JCheckBox box = new JCheckBox(text);
			panel.add(box, new GridBagConstraints(0, y++, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			if (argumentsValues.containsKey(text)) {
				panel.add(argumentsValues.get(text), new GridBagConstraints(0, y++, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			}
			checkBoxes.add(box);
		}
		JButton launch = new JButton("LAUNCH");
		launch.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				StringBuilder sb = new StringBuilder();
				sb.append(input_S.getText());
				for(JCheckBox box : checkBoxes){
					if (box.isSelected()) {
						sb.append(arguments.get(box.getText()));
						if (argumentsValues.containsKey(box.getText())) {
							sb.append(" " + argumentsValues.get(box.getText()).getText());
						}
					}
				}
				final String commandLine = sb.toString();
				new Thread(new Runnable() {
					@Override
					public void run() {
						launch(commandLine);
					}
				}, "Script.launch").start();
			}
		});
		panel.add(launch, new GridBagConstraints(0, y++, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10, 10, 10, 10), 0, 0));

		panel.add(new JPanel(), new GridBagConstraints(0, y++, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0), 0, 0));

		return panel;
	}
	
	protected abstract String getSpecificArguments(Project project, String subProject);
	
	protected boolean canGetSpecificArguments(Project project, String subProject) {
		return true;
	}
	
	public void launch(String baseCommandLine){
		ProjectManager.toConsole("-----------------------------------------------------");
		ProjectManager.toConsole("Relaunching selected samples");
		try{
			manager.connectToHighlander();
			for (int e=0 ; e < manager.getRelauncherPanel().getRelaunchListSize() ; e++){
				if (e%10 == 0){
					manager.disconnectFromHighlander();
					manager.connectToHighlander();
				}
				Project project = manager.getRelauncherPanel().getRelaunchListElement(e);
				for (String subProject : project.kits.keySet()){
					String commandLine = baseCommandLine;
					if (canGetSpecificArguments(project, subProject)) {
						commandLine += getSpecificArguments(project, subProject);
						commandLine += " ";
						commandLine += "/"+project.run_path.substring(0,project.run_path.lastIndexOf("/"));
						ProjectManager.toConsole(commandLine);

						ChannelExec channelExec = (ChannelExec)manager.getHighlanderSftpSession().openChannel("exec");
						commandLine = ProjectManager.getParameters().getServerPipelineScriptsPath()+"/"+commandLine;
						channelExec.setCommand(commandLine);
						channelExec.setInputStream(null);
						channelExec.setOutputStream(System.out);
						channelExec.setErrStream(System.err);
						InputStream in=channelExec.getInputStream();
						channelExec.connect();
						byte[] tmp=new byte[1024];
						while(true){
							while(in.available()>0){
								int i=in.read(tmp, 0, 1024);
								if(i<0)break;
								ProjectManager.toConsole(new String(tmp, 0, i));
							}
							if(channelExec.isClosed()){
								if(in.available()>0) continue;
								ProjectManager.toConsole("exit-status: "+channelExec.getExitStatus());
								break;
							}
							try{Thread.sleep(1000);}catch(Exception ee){}
						}
						channelExec.disconnect();
					}
				}
			}
			manager.disconnectFromHighlander();
		}catch(Exception ex){
			Tools.exception(ex);
		}
	}
	
}
