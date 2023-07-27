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

package be.uclouvain.ngs.highlander.administration.iontorrent;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Parameters;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.Parameters.Platform;
import be.uclouvain.ngs.highlander.UI.misc.WaitingPanel;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;


public class IonCleaner extends JFrame {

	static final public String version = "1.3";

	public Parameters parameters;

	private Map<Platform, JSch> sequencerJsch = new HashMap<Platform, JSch>();
	private Map<Platform, Session> sequencerSession = new HashMap<Platform, Session>();
	private Map<Platform, ChannelSftp> sequencerChannels = new HashMap<Platform, ChannelSftp>();

	private Map<Platform, String> sequencerResults = new HashMap<Platform, String>();
	private Map<Platform, String> sequencerRawData = new HashMap<Platform, String>();
	private Map<Platform, String> sequencerScript = new HashMap<Platform, String>();

	private JComboBox<Platform> platformBox;
	private JScrollPane scrollRaw;
	private ProjectTableModel modelRaw;
	private JTable tableRaw;
	private JScrollPane scrollAnal;
	private ProjectTableModel modelAnal;
	private JTable tableAnal;
	JButton backupButton;
	JButton removeButton;
	private JTabbedPane tabbedPane;
	private JLabel totalRawLabel = new JLabel();
	private JLabel totalAnalLabel = new JLabel();

	static private WaitingPanel waitingPanel;

	public IonCleaner(){
		setIconImage(Resources.getScaledIcon(Resources.iIonImporter, 32).getImage());
		setTitle("Ion Torrent and Proton projects cleaner " + version);
		sequencerResults.put(Platform.ION_TORRENT, "/results/analysis/output/Home");
		sequencerResults.put(Platform.PROTON, "/results/analysis/output/Home");
		sequencerRawData.put(Platform.ION_TORRENT, "/results/PGM1");
		sequencerRawData.put(Platform.PROTON, "/rawdata/sn247770060/");
		sequencerScript.put(Platform.ION_TORRENT, "/results/clean_iontorrent.sh");
		sequencerScript.put(Platform.PROTON, "/results/clean_proton.sh");
		try {
			parameters = new Parameters(true);
			Highlander.initialize(parameters, 5);
			platformBox = new JComboBox<Platform>(parameters.getAvailablePlatforms());
			for (Platform platform : parameters.getAvailablePlatforms()){
				connectToSequencer(platform);
			}
			initUI();		
			this.addComponentListener(new ComponentListener() {
				@Override
				public void componentShown(ComponentEvent arg0) {
					new Thread(new Runnable(){
						@Override
						public void run(){
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									waitingPanel.setVisible(true);
									waitingPanel.start();
								}
							});
							fillRawData(platformBox.getItemAt(platformBox.getSelectedIndex()));
							fillAnalyses(platformBox.getItemAt(platformBox.getSelectedIndex()));
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									waitingPanel.setVisible(false);
									waitingPanel.stop();
								}
							});
						}
					}, "IonCleaner.componentShown").start();
				}
				@Override
				public void componentResized(ComponentEvent arg0) {
				}
				@Override
				public void componentMoved(ComponentEvent arg0) {
				}

				@Override
				public void componentHidden(ComponentEvent arg0) {
				}
			});
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void initUI(){
		getContentPane().setLayout(new BorderLayout());

		platformBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							new Thread(new Runnable(){
								@Override
								public void run(){
									SwingUtilities.invokeLater(new Runnable() {
										@Override
										public void run() {
											waitingPanel.setVisible(true);
											waitingPanel.start();
										}
									});
									fillRawData(platformBox.getItemAt(platformBox.getSelectedIndex()));
									fillAnalyses(platformBox.getItemAt(platformBox.getSelectedIndex()));
									SwingUtilities.invokeLater(new Runnable() {
										@Override
										public void run() {
											waitingPanel.setVisible(false);
											waitingPanel.stop();
										}
									});
								}
							}, "IonCleaner.platformBox.itemStateChanged").start();
						}
					});
				}
			}
		});
		getContentPane().add(platformBox, BorderLayout.NORTH);

		tabbedPane = new JTabbedPane();
		getContentPane().add(tabbedPane, BorderLayout.CENTER);

		tableRaw = new JTable();
		tableRaw.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		scrollRaw = new JScrollPane(tableRaw);
		tabbedPane.addTab("Raw data", scrollRaw);

		tableAnal = new JTable();
		tableAnal.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		scrollAnal = new JScrollPane(tableAnal);
		tabbedPane.addTab("Analyses", scrollAnal);

		JPanel southPanel = new JPanel();
		getContentPane().add(southPanel, BorderLayout.SOUTH);

		backupButton = new JButton("Backup", Resources.getScaledIcon(Resources.iDbAdd, 16));
		backupButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						backup(tabbedPane.getSelectedIndex() == 0, platformBox.getItemAt(platformBox.getSelectedIndex()));
					}
				}, "IonCleaner.backup").start();

			}
		});
		southPanel.add(backupButton);

		removeButton = new JButton("Remove", Resources.getScaledIcon(Resources.iCross, 16));
		removeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						remove(tabbedPane.getSelectedIndex() == 0, platformBox.getItemAt(platformBox.getSelectedIndex()));
					}
				}, "IonCleaner.remove").start();

			}
		});
		southPanel.add(removeButton);
		southPanel.add(totalRawLabel);
		southPanel.add(totalAnalLabel);

		waitingPanel = new WaitingPanel();
		setGlassPane(waitingPanel);
	}

	private void fillRawData(Platform platform){
		try{
			Object[][] data = listDir(sequencerRawData.get(platform), platform);
			String[] headers = new String[]{
					"Run","Size (Gb)"
			};
			Class<?>[] colClass = new Class[]{
					String.class,Long.class,
			};
			modelRaw = new ProjectTableModel(data, headers, colClass);
			tableRaw.setModel(modelRaw);
			tableRaw.setRowSorter(new TableRowSorter<ProjectTableModel>(modelRaw));
			totalRawLabel.setText("Raw : " + Tools.doubleToString((double)modelRaw.getTotalSize()/1024,1,false) +" Tb");
		}catch(Exception ex){
			Tools.exception(ex);
		}
	}

	private void fillAnalyses(Platform platform){
		try{
			Object[][] data = listDir(sequencerResults.get(platform), platform);
			String[] headers = new String[]{
					"Analysis","Size (Gb)"
			};
			Class<?>[] colClass = new Class[]{
					String.class,Long.class,
			};
			modelAnal = new ProjectTableModel(data, headers, colClass);
			tableAnal.setModel(modelAnal);
			tableAnal.setRowSorter(new TableRowSorter<ProjectTableModel>(modelAnal));
			totalAnalLabel.setText("Analyses : " + Tools.doubleToString((double)modelAnal.getTotalSize()/1024,1,false) +" Tb");
		}catch(Exception ex){
			Tools.exception(ex);
		}
	}

	public class ProjectTableModel	extends AbstractTableModel {
		private Object[][] data;
		private String[] headers;
		private Class<?>[] colClass;

		public ProjectTableModel(Object[][] data, String[] headers, Class<?>[] colClass) {    	
			this.data = data;
			this.headers = headers;
			this.colClass = colClass;
		}

		@Override
		public int getColumnCount() {
			return headers.length;
		}

		@Override
		public String getColumnName(int col) {
			return headers[col];
		}

		@Override
		public int getRowCount() {
			return data.length;
		}

		@Override
		public Class<?> getColumnClass(int col) {
			return colClass[col];
		}

		@Override
		public Object getValueAt(int row, int col) {
			return data[row][col];
		}

		@Override
		public void setValueAt(Object value, int row, int col) {
			data[row][col] = value;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}

		public long getTotalSize(){
			long tot = 0;
			for (Object[] cell : data){
				tot += (Long)cell[1];
			}
			return tot;
		}
	}

	private void backup(boolean raw, Platform platform){
		JTable selectedTable = (raw) ? tableRaw : tableAnal;
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				waitingPanel.setVisible(true);
				waitingPanel.start();
			}
		});
		try{
			for (int row : selectedTable.getSelectedRows()){
				ChannelExec channelExec = (ChannelExec)sequencerSession.get(platform).openChannel("exec");
				String command = "/results/clean_iontorrent.sh ";
				if (raw) command += "-b ";
				else command += "-B ";
				command += selectedTable.getValueAt(row, 0);
				channelExec.setCommand(command);
				channelExec.connect();
				channelExec.setInputStream(null);
				channelExec.setOutputStream(System.out);
				channelExec.setErrStream(System.err);
				try (InputStream in=channelExec.getInputStream()){
					channelExec.connect();
					try (InputStreamReader isr = new InputStreamReader(in)){
						try (BufferedReader br = new BufferedReader(isr)){
							String line;
							while ((line = br.readLine()) != null || !channelExec.isClosed()) {
								if (line != null) {
									System.out.println(line);
								}
							}
							System.out.println("exit-status: "+channelExec.getExitStatus());
						}
					}
				}
				channelExec.disconnect();
			}
			JOptionPane.showMessageDialog(new JFrame(), "Backup is done. Please check the console to see if everything went well.", "Ion Importer",
					JOptionPane.PLAIN_MESSAGE, Resources.getScaledIcon(Resources.iDbAdd,64));		      
			disconnectFromSequencer(platform);
			connectToSequencer(platform);
		}catch(Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Backup", ex), "Ion Cleaner",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));			
		}
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				waitingPanel.setVisible(false);
				waitingPanel.stop();
			}
		});
	}

	private void remove(boolean raw, Platform platform){
		JTable selectedTable = (raw) ? tableRaw : tableAnal;
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				waitingPanel.setVisible(true);
				waitingPanel.start();
			}
		});
		try{
			for (int row : selectedTable.getSelectedRows()){
				ChannelExec channelExec = (ChannelExec)sequencerSession.get(platform).openChannel("exec");
				String command = sequencerScript.get(platform) + " ";
				if (raw) command += "-r ";
				else command += "-R ";
				command += selectedTable.getValueAt(row, 0);
				channelExec.setCommand(command);
				channelExec.connect();
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
						System.out.print(new String(tmp, 0, i));
					}
					if(channelExec.isClosed()){
						if(in.available()>0) continue;
						System.out.println("exit-status: "+channelExec.getExitStatus());
						break;
					}
					try{Thread.sleep(1000);}catch(Exception ee){}
				}
				channelExec.disconnect();
			}
			JOptionPane.showMessageDialog(new JFrame(), "Deletion is done. Please check the console to see if everything went well.", "Ion Cleaner",
					JOptionPane.PLAIN_MESSAGE, Resources.getScaledIcon(Resources.iDbAdd,64));		      
			disconnectFromSequencer(platform);
			connectToSequencer(platform);
		}catch(Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Remove", ex), "Ion Cleaner",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));			
		}
		if (raw) fillRawData(platform);
		else fillAnalyses(platform);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				waitingPanel.setVisible(false);
				waitingPanel.stop();
			}
		});
	}

	private void connectToSequencer(Platform platform) throws JSchException {
		sequencerJsch.put(platform, new JSch());
		sequencerJsch.get(platform).addIdentity("config/"+parameters.getServerSequencerPrivateKey().get(platform));
		sequencerSession.put(platform, sequencerJsch.get(platform).getSession(parameters.getServerSequencerUsername().get(platform), parameters.getServerSequencerHost().get(platform), 22));
		sequencerSession.get(platform).setConfig("StrictHostKeyChecking", "no");
		sequencerSession.get(platform).connect();
		sequencerChannels.put(platform, (ChannelSftp) sequencerSession.get(platform).openChannel("sftp"));
		sequencerChannels.get(platform).connect();
	}

	private void disconnectFromSequencer(Platform platform) throws JSchException {
		sequencerChannels.get(platform).quit();
		sequencerSession.get(platform).disconnect();
		sequencerJsch.get(platform).removeAllIdentity();
	}

	public Object[][] listDir(String directory, Platform platform) throws Exception {
		Map<String,Long> projects = new TreeMap<String,Long>();
		for (Object dir : sequencerChannels.get(platform).ls(directory)){
			LsEntry entry = (LsEntry)dir;
			SftpATTRS attrs = entry.getAttrs();
			if (!entry.getFilename().equals(".") && !entry.getFilename().equals("..") && attrs.isDir())
				projects.put(entry.getFilename(), (computeDirSize(directory, entry, platform) / 1024 / 1024 / 1024));
		}
		Object[][] res = new Object[projects.size()][2];
		int i=0;
		for (Entry<String, Long> e : projects.entrySet()){
			res[i][0] = e.getKey();
			res[i][1] = e.getValue();
			i++;
		}
		return res;
	}

	public long computeDirSize(String directory, LsEntry entry, Platform platform) throws Exception {
		long size = 0;
		SftpATTRS attrs = entry.getAttrs();
		if (!entry.getFilename().equals(".") && !entry.getFilename().equals("..")){
			if (attrs.isDir()){
				String subDir = directory + "/" + entry.getFilename();
				for (Object dir : sequencerChannels.get(platform).ls(subDir)){
					size += computeDirSize(subDir, (LsEntry)dir, platform);				
				}
			}else{
				size += attrs.getSize();
			}
		}
		return size;
	}

	//Overridden so we can exit when window is closed
	@Override
	protected void processWindowEvent(WindowEvent e) {
		super.processWindowEvent(e);
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			exit();
		}
	}

	public void exit(){
		try{
			for (Platform platform : parameters.getAvailablePlatforms()){
				disconnectFromSequencer(platform);
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
		System.exit(0);
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					InputMap im = (InputMap)UIManager.get("Button.focusInputMap");
					im.put( KeyStroke.getKeyStroke( "ENTER" ), "pressed" );
					im.put( KeyStroke.getKeyStroke( "released ENTER" ), "released" );
					break;
				}
			}
		} catch (Exception e) {
			try{
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		final IonCleaner ion = new IonCleaner();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				ion.validate();
				//Center the window
				Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
				ion.setSize(new Dimension(screenSize.width/2, screenSize.height/3*2));
				Dimension frameSize = ion.getSize();
				if (frameSize.height > screenSize.height) {
					frameSize.height = screenSize.height;
				}
				if (frameSize.width > screenSize.width) {
					frameSize.width = screenSize.width;
				}
				ion.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
				//pm.setExtendedState(ProjectManager.MAXIMIZED_BOTH);
				ion.setVisible(true);
			}
		});


	}

}
