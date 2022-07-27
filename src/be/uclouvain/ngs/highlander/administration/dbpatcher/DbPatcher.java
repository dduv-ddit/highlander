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

package be.uclouvain.ngs.highlander.administration.dbpatcher;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import com.install4j.api.launcher.ApplicationLauncher;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Parameters;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.misc.WaitingPanel;
import be.uclouvain.ngs.highlander.administration.users.LoginBox;
import be.uclouvain.ngs.highlander.administration.users.User;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;

public class DbPatcher extends JFrame  {

	static final public String version = "17.13";

	private static Parameters parameters;
	private static User user;
	private static HighlanderDatabase DB;
	private static JTextArea console = null;

	static private WaitingPanel waitingPanel;

	public JLabel labelCurrentVersion;
	public JComboBox<Version> box;
	public static String currentVersion;
	public static final Version[] availableVersions = new Version[]{
			new Version_1_9(),
			new Version_1_10(),
			new Version_1_12(),
			new Version_14(),
			new Version_14_8(),
			new Version_14_10(),
			new Version_17(),
			new Version_17_12(),
			new Version_17_13(),
			//new Version_Development(),
	};

	public DbPatcher(User u, JTextArea c, WaitingPanel w) throws Exception {
		if (Highlander.getParameters() != null) {
			parameters = Highlander.getParameters();
		}else {
			throw new Exception("Highlander parameters have not been initialize");
		}
		if (Highlander.getDB() != null) {
			DB = Highlander.getDB();
		}else {
			throw new Exception("Highlander database has not been initialize");
		}
		user = u;
		console = c;
		waitingPanel = w;
		try {
			setCurrentVersion();
		} catch (Exception ex) {
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Error", ex), "Launching db patcher", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
	}

	public DbPatcher() throws Exception {
		if (Highlander.getParameters() != null) {
			parameters = Highlander.getParameters();
		}else {
			throw new Exception("Highlander parameters have not been initialize");
		}
		if (Highlander.getDB() == null) {
			DB = Highlander.getDB();
		}else {
			throw new Exception("Highlander database has not been initialize");
		}
		setIconImage(Resources.getScaledIcon(Resources.iDbPatcher, 32).getImage());
		setTitle("Highlander database patcher version " + version);
		try {
			setCurrentVersion();
			initUI();		
		} catch (Exception ex) {
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Error", ex), "Launching db patcher", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
	}

	private static void setCurrentVersion() throws Exception {
		try (Results res = DB.select(Schema.HIGHLANDER,"SELECT version FROM main")) {
			if(res.next()){
				currentVersion = res.getString("version");
			}
		}  				
	}

	private void initUI(){
		setLayout(new BorderLayout(10,10));
		JPanel centerPanel = new JPanel(new GridBagLayout());
		JLabel labelCurrent = new JLabel("Current Highlander database version");
		labelCurrentVersion = new JLabel(currentVersion);		
		JLabel labelUpdate = new JLabel("Update database to version");
		box = new JComboBox<>(availableVersions);
		box.setSelectedIndex(availableVersions.length-1);
		centerPanel.add(labelCurrent, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		centerPanel.add(labelCurrentVersion, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		centerPanel.add(labelUpdate, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		centerPanel.add(box, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
		add(centerPanel, BorderLayout.CENTER);

		JPanel southPanel = new JPanel();
		JButton updateButton = new JButton("Update", Resources.getScaledIcon(Resources.iUpdater, 16));
		updateButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				new Thread(new Runnable(){
					public void run(){
						update();
					}
				}, "DbPatcher Update").start();
			}
		});
		southPanel.add(updateButton);		
		add(southPanel, BorderLayout.SOUTH);

		waitingPanel = new WaitingPanel();
		setGlassPane(waitingPanel);
	}

	private static boolean isCurrentVersionUpToDate(String version){
		String[] currentShort = currentVersion.split("\\.");  
		String[] dbShort = version.split("\\.");
		int[] current = new int[Math.max(currentShort.length, dbShort.length)];
		for (int i=0 ; i < current.length ; i++){
			if (i < currentShort.length) current[i] = Integer.parseInt(currentShort[i]);
			else current[i] = 0;
		}
		int[] db = new int[Math.max(currentShort.length, dbShort.length)];
		for (int i=0 ; i < db.length ; i++){
			if (i < dbShort.length) db[i] = Integer.parseInt(dbShort[i]);
			else db[i] = 0;
		}
		for (int i=0  ; i < current.length && i < db.length ; i++){
			if (i < current.length-1 && i < db.length-1){
				if (current[i] > db[i]) return true;
				if (current[i] < db[i]) return false;
			}else{
				if (current[i] >= db[i]) return true;
				if (current[i] < db[i]) return false;
			}
		}
		return false;
	}

	public void update(){
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				waitingPanel.setVisible(true);
				waitingPanel.start();
			}
		});		
		Version targetVersion = (Version)box.getSelectedItem();
		String currentTry = "?";
		try{
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					waitingPanel.setProgressString("Updating database - DO NOT CLOSE !", false);
					waitingPanel.setProgressMaximum(availableVersions.length);
				}
			});
			int count = 0;
			//Make all updates before selected version, if needed
			for (int i=0 ; i < availableVersions.length && !availableVersions[i].equals(targetVersion) ; i++){
				waitingPanel.setProgressValue(++count);
				currentTry = availableVersions[i].getVersion();
				if (!isCurrentVersionUpToDate(currentTry)){
					availableVersions[i].update();
					labelCurrentVersion.setText(currentVersion);
					validate();
					repaint();
				}
			}
			//Make selected update, if needed
			waitingPanel.setProgressValue(++count);
			currentTry = targetVersion.getVersion();
			if (!isCurrentVersionUpToDate(currentTry)){
				targetVersion.update();
				labelCurrentVersion.setText(currentVersion);
				validate();
				repaint();
			}
			waitingPanel.setProgressDone();
		}catch(Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Problem when updating from version " + currentVersion + " to version " + currentTry, ex), "Database update", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				waitingPanel.setVisible(false);
				waitingPanel.stop();
			}
		});
	}

	public static void toConsole(String line) {
		if (console != null){
			console.append(line+"\n");
			console.setCaretPosition(console.getText().length());
		}else{
			System.out.println(line);
		}
	}

	public static void updateAndPrint(Schema schema, String query) throws Exception {
		toConsole(query);
		DB.update(schema, query);
	}

	public static User login(){
		LoginBox loginBox = new LoginBox(new JFrame(), "DBPatcher "+version+" login at "+parameters.getDbMainHost(), true) ;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize() ;
		Dimension windowSize = loginBox.getSize() ;
		loginBox.setLocation(Math.max(0, (screenSize.width - windowSize.width) / 2),
				Math.max(0, (screenSize.height - windowSize.height) / 2)) ;
		loginBox.setVisible(true) ;
		if (loginBox.OKCancel) {
			try {
				return new User(loginBox.getUsername(), loginBox.getEncryptedPassword()) ;
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(new JFrame(), ex.getMessage(), "Can't login", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				return null;
			}
		} else {
			System.exit(0);
			return null;
		}
	}


	//Overridden so we can exit when window is closed
	protected void processWindowEvent(WindowEvent e) {
		super.processWindowEvent(e);
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			DB.disconnectAll();
			System.exit(0);
		}
	}

	/**
	 * Program arguments
	 * -h : get all available versions
	 * -u [username] : give username as parameter 
	 * -p [password] : give password as parameter
	 * -c [config file] : give config file to use as parameter
	 * -v [version] : update database to given version without GUI (you must also use -u and -p)
	 * -upoff				 : turn auto update off
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String argUser = null;
		String argPass = null;
		String argConfig = null;
		boolean showVersion = false;
		boolean updateCheck = true;
		String selectedVersion = null;
		for(int i=0 ; i < args.length ; i++){
			if (args[i].equalsIgnoreCase("-u")){
				argUser = args[++i];
			}else if (args[i].equalsIgnoreCase("-p")){
				argPass = args[++i];
			}else if (args[i].equalsIgnoreCase("-c")){
				argConfig = args[++i];
			}else if (args[i].equalsIgnoreCase("-h")){
				showVersion = true;
			}else if (args[i].equalsIgnoreCase("-v")){
				selectedVersion = args[++i];
			}else if (args[i].equalsIgnoreCase("-upoff")){
				updateCheck = false;
			}
		}
		if (showVersion){
			System.out.println("Highlander DbPatcher version "+version+"\n");
			System.out.println("Available patches:");
			for (Version v : availableVersions){
				System.out.println(v);				
			}
			System.out.println();
			System.out.println("NB: if you use a Unix system without graphical support (no X11) and get related errors, use -Djava.awt.headless=true");
			System.out.println("Available arguments are: ");
			System.out.println("-h : get all available versions");
			System.out.println("-u [username] : give username as parameter");
			System.out.println("-p [password] : give password as parameter");
			System.out.println("-c [config file] : give config file to use as parameter");
			System.out.println("-v [version] : update database to given version without GUI (you must also use -u and -p)");
			System.out.println("-upoff : turn auto update off");
		}else if (selectedVersion != null){
			Version targetVersion = null;
			for (Version version : availableVersions) {
				if (version.getVersion().equals(selectedVersion)) {
					targetVersion = version;
					break;
				}
			}
			if (targetVersion != null) {
				if (argUser == null || argPass == null){
					System.err.println("If you want to directly update your database, please give also use a username and password using -u and -p arguments");
				}else{
					try{
						parameters = (argConfig == null) ? new Parameters(false) : new Parameters(false, new File(argConfig));
						Highlander.initialize(parameters,5);
						DB = Highlander.getDB();
						Version.setDatabase(DB);
						user = new User(argUser, Tools.md5Encryption(argPass));
						if (!user.isAdmin()){
							System.err.println("Sorry, you must be administrator of the Highlander database");
						}else{
							Highlander.setLoggedUser(user);
							setCurrentVersion();
							String currentTry = "?";
							toConsole("Updating database - DO NOT INTERRUPT !");
							//Make all updates before selected version, if needed
							for (int i=0 ; i < availableVersions.length && !availableVersions[i].equals(targetVersion) ; i++){
								currentTry = availableVersions[i].getVersion();
								if (!isCurrentVersionUpToDate(currentTry)){
									availableVersions[i].update();
								}
							}
							//Make selected update, if needed
							currentTry = targetVersion.getVersion();
							if (!isCurrentVersionUpToDate(currentTry)){
								targetVersion.update();
							}

						}
						DB.disconnectAll();
					}catch (Exception ex){
						Tools.exception(ex);
					}
				}
			}
		}else{
			try {
				if (updateCheck) ApplicationLauncher.launchApplication("170", null, true, null);
			} catch (Exception ex) {
				Tools.exception(ex);
			}
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
			try{
				parameters = (argConfig == null) ? new Parameters(true) : new Parameters(true, new File(argConfig));
				Highlander.initialize(parameters,5);
				DB = Highlander.getDB();
				Version.setDatabase(DB);
			}catch (Exception ex){
				Tools.exception(ex);
				JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Problem when reading configuration file", ex), "Reading Highlander parameters",
						JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				System.exit(-1);
			}
			while(user == null){
				if (argUser == null || argPass == null){
					user = login();
				}else{
					try {
						user = new User(argUser, Tools.md5Encryption(argPass));
					} catch (Exception ex) {
						JOptionPane.showMessageDialog(new JFrame(), ex.getMessage(), "Can't login", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
						user = login();
					}
				}
			}
			if (!user.isAdmin()){
				JOptionPane.showMessageDialog(new JFrame(), "Sorry, you must be administrator of the Highlander database", "Can't login", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				System.exit(0);
			}else{
				try {
					Highlander.setLoggedUser(user);
					final DbPatcher patcher = new DbPatcher();
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							patcher.validate();
							//Center the window
							patcher.pack();
							Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
							Dimension frameSize = patcher.getSize();
							patcher.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
							patcher.setVisible(true);
						}
					});
				}catch (Exception ex) {
					Tools.exception(ex);
					System.err.println(ex.getMessage());
					System.exit(1);
				}
			}
		}
	}

}
