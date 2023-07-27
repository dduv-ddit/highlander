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

package be.uclouvain.ngs.highlander.administration.UI;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import java.awt.BorderLayout;

import javax.swing.InputMap;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.WindowConstants;
import javax.swing.text.DefaultEditorKit;

import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JScrollPane;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;

import com.install4j.api.launcher.ApplicationLauncher;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Parameters;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.Parameters.Platform;
import be.uclouvain.ngs.highlander.UI.misc.WaitingPanel;
import be.uclouvain.ngs.highlander.administration.DbBuilder;
import be.uclouvain.ngs.highlander.administration.DbUpdater;
import be.uclouvain.ngs.highlander.administration.UI.client.DefaultColumnsPanel;
import be.uclouvain.ngs.highlander.administration.UI.client.GlobalSettingsPanel;
import be.uclouvain.ngs.highlander.administration.UI.client.LinksPanel;
import be.uclouvain.ngs.highlander.administration.UI.database.AnalysesPanel;
import be.uclouvain.ngs.highlander.administration.UI.database.FieldsPanel;
import be.uclouvain.ngs.highlander.administration.UI.database.PathologiesPanel;
import be.uclouvain.ngs.highlander.administration.UI.database.PopulationsPanel;
import be.uclouvain.ngs.highlander.administration.UI.database.ReferencesPanel;
import be.uclouvain.ngs.highlander.administration.UI.database.ReportsPanel;
import be.uclouvain.ngs.highlander.administration.UI.database.UpdatePanel;
import be.uclouvain.ngs.highlander.administration.UI.database.UpgradePanel;
import be.uclouvain.ngs.highlander.administration.UI.projects.PostImportationPanel;
import be.uclouvain.ngs.highlander.administration.UI.projects.ProjectsPanel;
import be.uclouvain.ngs.highlander.administration.UI.projects.SamplesPanel;
import be.uclouvain.ngs.highlander.administration.UI.tools.RelauncherPanel;
import be.uclouvain.ngs.highlander.administration.UI.tools.VcfToolsPanel;
import be.uclouvain.ngs.highlander.administration.UI.users.MailUsers;
import be.uclouvain.ngs.highlander.administration.UI.users.UserManagementPanel;
import be.uclouvain.ngs.highlander.administration.UI.users.UserPermissionsPanel;
import be.uclouvain.ngs.highlander.administration.dbpatcher.DbPatcher;
import be.uclouvain.ngs.highlander.administration.users.LoginBox;
import be.uclouvain.ngs.highlander.administration.users.User;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class ProjectManager extends JFrame {

	static final public String version = "17.17";

	enum FastqcResult {pass, warn, fail}

	private boolean betaActive = false;

	private static Parameters parameters;
	private static User user;
	private static HighlanderDatabase DB;

	private static DbPatcher dbPatcher;
	private static DbUpdater dbUpdater;
	private static DbBuilder dbBuilder;

	private JSch hlJsch;
	private Session hlSession;
	private ChannelSftp hlChannel;

	private User[] users;	
	private EventList<User> usersList;
	private List<AnalysisFull> availableAnalysis;

	private final static JFrame consoleFrame = new JFrame();
	private final static JTextArea console = new JTextArea();

	private volatile boolean redirectSystemOut = false;
	private final PrintStream normalSystemOut = System.out;
	private final PrintStream normalSystemErr = System.err;
	private static WaitingPanel waitingPanel;

	private ProjectsPanel projectsPanel;
	private SamplesPanel samplesPanel;
	private AnalysesPanel analysesPanel;
	private ReportsPanel reportsPanel;
	private RelauncherPanel relauncherPanel;
	
	public ProjectManager() {
		setIconImage(Resources.getScaledIcon(Resources.iAdminstrationTools, 32).getImage());
		setTitle("Highlander Administration Tools " + version);
		try {
			users = User.fetchList().toArray((new User[0]));
			usersList = GlazedLists.eventListOf(users);
			availableAnalysis = AnalysisFull.getAvailableAnalyses();
			betaActive = DB.isBetaFunctionalitiesActivated();
			waitingPanel = new WaitingPanel();
			setGlassPane(waitingPanel);
			dbPatcher = new DbPatcher(user, console, waitingPanel);
			dbUpdater = new DbUpdater();
			dbBuilder = new DbBuilder();
			initUI();		
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					consoleFrame.setLayout(new BorderLayout());
					console.setEditable(false);
					console.setLineWrap(true);
					console.setWrapStyleWord(true);
					consoleFrame.add(new JScrollPane(console), BorderLayout.CENTER);
					consoleFrame.setIconImage(Resources.getScaledIcon(Resources.iAdminstrationTools,64).getImage());
					consoleFrame.setTitle("Console");
					consoleFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
					Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
					int width = screenSize.width - (screenSize.width/4*3);
					int height = (int)(screenSize.height*0.95);
					consoleFrame.setSize(new Dimension(width,height));
					consoleFrame.setLocation(5+screenSize.width/4*3,5);
					consoleFrame.setVisible(true);
				}
			});
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void initUI(){
		JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);
		getContentPane().add(tabbedPane, BorderLayout.CENTER);		
		tabbedPane.addTab("Project management", null, getTabProjectsManagement(), null);
		tabbedPane.addTab("Database management", null, getTabDatabaseManagement(), null);
		tabbedPane.addTab("Client management", null, getTabClientManagement(), null);
		tabbedPane.addTab("User management", null, getTabUserManagement(), null);
		tabbedPane.addTab("Tools", null, getTabTools(), null);
		tabbedPane.addTab("Settings", null, new SettingsPanel(this), null);
	}

	private JPanel getTabProjectsManagement(){
		projectsPanel = new ProjectsPanel(this);
		samplesPanel = new SamplesPanel(this);
		JPanel panel = new JPanel(new BorderLayout());
		JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);		
		tabbedPane.addTab("Projects", null, projectsPanel, null);
		tabbedPane.addTab("Post-importation steps", null, new PostImportationPanel(this), null);
		tabbedPane.addTab("Samples", null, samplesPanel, null);
		panel.add(tabbedPane, BorderLayout.CENTER);
		return panel;
	}

	private JPanel getTabDatabaseManagement(){
		analysesPanel = new AnalysesPanel(this);
		reportsPanel = new ReportsPanel(this);
		JPanel panel = new JPanel(new BorderLayout());
		JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);		
		tabbedPane.addTab("Analyses", null, analysesPanel, null);
		tabbedPane.addTab("References", null, new ReferencesPanel(this), null);
		tabbedPane.addTab("Fields", null, new FieldsPanel(this), null);
		tabbedPane.addTab("Pathologies", null, new PathologiesPanel(this), null);
		tabbedPane.addTab("Populations", null, new PopulationsPanel(this), null);
		tabbedPane.addTab("Reports", null, reportsPanel, null);
		tabbedPane.addTab("Update database content", null, new UpdatePanel(this), null);
		tabbedPane.addTab("Upgrade database to last version", null, new UpgradePanel(this), null);
		panel.add(tabbedPane, BorderLayout.CENTER);
		return panel;
	}

	private JPanel getTabClientManagement(){
		JPanel panel = new JPanel(new BorderLayout());
		JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);		
		tabbedPane.addTab("Global settings", null, new GlobalSettingsPanel(this), null);
		tabbedPane.addTab("External links", null, new LinksPanel(this), null);
		tabbedPane.addTab("Default columns", null, new DefaultColumnsPanel(this), null);
		panel.add(tabbedPane, BorderLayout.CENTER);
		return panel;		
	}
	
	private JPanel getTabUserManagement(){
		JPanel panel = new JPanel(new BorderLayout());
		JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);		
		tabbedPane.addTab("Management", null, new UserManagementPanel(this), null);
		tabbedPane.addTab("Sample permissions", null, new UserPermissionsPanel(this), null);
		tabbedPane.addTab("Mail users", null, new MailUsers(this), null);
		panel.add(tabbedPane, BorderLayout.CENTER);
		return panel;		
	}

	private JPanel getTabTools(){
		JPanel panel = new JPanel(new BorderLayout());
		JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);		
		tabbedPane.addTab("VCF tools", null, new VcfToolsPanel(this), null);
		if (betaActive) {
			relauncherPanel = new RelauncherPanel(this);
			tabbedPane.addTab("Relauncher", null, relauncherPanel, null);
		}
		panel.add(tabbedPane, BorderLayout.CENTER);
		return panel;
	}

	public static Parameters getParameters() {
		return parameters;
	}
	
	public static HighlanderDatabase getDB() {
		return DB;
	}
	
	public static User getLoggedUser() {
		return user;
	}
	
	public static DbBuilder getDbBuilder() {
		return dbBuilder;
	}
	
	public static DbUpdater getDbUpdater() {
		return dbUpdater;
	}
	
	public static DbPatcher getDbPatcher() {
		return dbPatcher;
	}
	
	public static WaitingPanel getWaitingPanel() {
		return waitingPanel;
	}
	
	public static void toConsole(String line) {
		console.append(line+"\n");
		console.setCaretPosition(console.getText().length());
	}

	public static void toConsole(Exception ex){
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		toConsole("+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+");
		toConsole("Error : "+ ex.getMessage());
		Tools.exception(ex);
		toConsole("+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+");
		JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Error", ex), "Error",
				JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
	}

	public void startRedirectSystemOut(){
		redirectSystemOut = true;
		final PipedOutputStream pOut = new PipedOutputStream();
		System.setOut(new PrintStream(pOut));
		System.setErr(new PrintStream(pOut));
		new Thread(new Runnable(){
			@Override
			public void run(){
				try (PipedInputStream pIn = new PipedInputStream(pOut)){					
					try (BufferedReader reader = new BufferedReader(new InputStreamReader(pIn))){
						String line;
						while(redirectSystemOut){
							try {
								if((line = reader.readLine()) != null) {
									toConsole(line);
								}
							}catch(Exception ex) {
								//Do nothing
							}
						}
					}
				}catch (IOException ioex) {
					ioex.printStackTrace();
				}
			}
		}, "ProjectManager.startRedirectSystemOut").start();
	}

	public void stopRedirectSystemOut(){
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		redirectSystemOut = false;
		System.out.flush();
		System.setOut(normalSystemOut);
		System.setErr(normalSystemErr);
	}

	public static User login(){
		LoginBox loginBox = new LoginBox(new JFrame(), "Highlander Administration tools "+version+" login at "+parameters.getDbMainHost(), true) ;
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

	public static void setHardUpdate(boolean enable) {
		try{
			DB.update(Schema.HIGHLANDER, "UPDATE main SET update_hard = "+((enable)?1:0));
		}catch(Exception ex){
			toConsole(ex);
		}
	}

	public void connectToHighlander() throws JSchException {
		hlJsch = new JSch();
		hlJsch.addIdentity("config/"+parameters.getServerPipelinePrivateKey());
		hlSession = hlJsch.getSession(parameters.getServerPipelineUsername(), parameters.getServerPipelineHost(), 22);
		hlSession.setConfig("StrictHostKeyChecking", "no");
		hlSession.connect();
		hlChannel = (ChannelSftp) hlSession.openChannel("sftp");
		hlChannel.connect();
	}

	public void disconnectFromHighlander() throws JSchException {
		hlChannel.quit();
		hlSession.disconnect();
		hlJsch.removeAllIdentity();
	}

	public Session getHighlanderSftpSession() {
		return hlSession;
	}
	
	public AnalysisFull[] getAvailableAnalysesAsArray() {
		return availableAnalysis.toArray(new AnalysisFull[0]);
	}
	
	public List<AnalysisFull> getAvailableAnalysesAsList() {
		return new ArrayList<AnalysisFull>(availableAnalysis);
	}
	
	public void addAvailableAnalysis(AnalysisFull analysis) {
		availableAnalysis.add(analysis);
	}
	
	public void removeAvailableAnalysis(Analysis analysis) {
		availableAnalysis.remove(analysis);
	}
	
	public User[] getUsers() {
		return users;
	}
	
	public EventList<User> getUserList() {
		return usersList;
	}
	
	public String[] listSchema(boolean withUnavailableOption) throws Exception {
		Set<String> schemas = new TreeSet<String>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SHOW DATABASES")) {
			while(res.next()){
				schemas.add(res.getString(1));
			}
		}
		if (withUnavailableOption) schemas.add("SCHEMA UNAVAILABLE");
		schemas.remove("");
		return schemas.toArray(new String[0]);
	}
	
	public String[] listProjects() throws Exception {
		Set<String> projects = new TreeSet<String>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `run_label` FROM projects")) {
			while(res.next()){
				projects.add(res.getString(1));
			}
		}
		projects.remove("");
		return projects.toArray(new String[0]);
	}

	public String[] listPlatforms() {
		Set<String> availablePlatforms = new TreeSet<>();
		for (Platform p : Platform.values()){
			availablePlatforms.add(p.toString());
		}
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT DISTINCT(`platform`) as avp FROM `projects` WHERE platform IS NOT NULL ORDER BY avp")) {			
			while (res.next()){
				availablePlatforms.add(res.getString(1));
			}
		}catch(Exception ex){
			Tools.exception(ex);
		}
		availablePlatforms.add("Add new platform");
		availablePlatforms.remove("");
		return availablePlatforms.toArray(new String[0]);
	}

	public String[] listOursourcing() {
		Set<String> outsourcing = new TreeSet<String>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT DISTINCT(outsourcing) FROM projects WHERE outsourcing IS NOT NULL")) {
			while(res.next()){
				outsourcing.add(res.getString(1));
			}
		}catch(Exception ex){
			Tools.exception(ex);
		}
		outsourcing.add("Add new outsourcing");
		outsourcing.remove("");
		return outsourcing.toArray(new String[0]);
	}

	public String[] listKits() {
		Set<String> kits = new TreeSet<String>();
		kits.add("SureSelect5");
		kits.add("SureSelect6");
		kits.add("Nextera");
		kits.add("TruSeq");
		kits.add("SeqCap3");
		kits.add("TargetSeq");
		kits.add("AmpliSeq");
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT DISTINCT(kit) FROM projects WHERE kit IS NOT NULL")) {
			while(res.next()){
				kits.add(res.getString(1));
			}
		}catch(Exception ex){
			Tools.exception(ex);
		}
		kits.add("Add new kit");
		kits.remove("");
		return kits.toArray(new String[0]);
	}

	public String[] listPathologies() {
		Set<String> pathologies = new TreeSet<String>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT * FROM pathologies")) {
			while(res.next()){
				pathologies.add(res.getString("pathology"));
			}
		}catch(Exception ex){
			Tools.exception(ex);
		}
		pathologies.remove("");
		return pathologies.toArray(new String[0]);
	}

	public String[] listPopulations() {
		Set<String> populations = new TreeSet<String>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT * FROM populations")) {
			while(res.next()){
				populations.add(res.getString("population"));
			}
		}catch(Exception ex){
			Tools.exception(ex);
		}
		populations.remove("");
		return populations.toArray(new String[0]);
	}
	
	public String[] listSequencingTargets() {
		Set<String> targets = new TreeSet<String>();
		targets.add("Add new sequencing_target");
		targets.add("WES");
		targets.add("WGS");
		targets.add("sWGS");
		targets.add("Panel");
		targets.add("Mendeliome");
		targets.add("RNAseq");
		targets.add("CHIPseq");
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT sequencing_target FROM analyses WHERE sequencing_target IS NOT NULL")) {
			while (res.next()){
				targets.add(res.getString("sequencing_target"));
			}
		}catch(Exception ex){
			Tools.exception(ex);
		}
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT sequencing_target FROM projects WHERE sequencing_target IS NOT NULL")) {
			while (res.next()){
				targets.add(res.getString("sequencing_target"));
			}
		}catch(Exception ex){
			Tools.exception(ex);
		}
		targets.remove("");
		return targets.toArray(new String[0]);
	}

	public String[] listAnnotationSources() {
		Set<String> sources = new TreeSet<String>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT DISTINCT(`source`) FROM `fields` WHERE `source` IS NOT NULL")) {
			while(res.next()){
				sources.add(res.getString(1));
			}
		}catch(Exception ex){
			Tools.exception(ex);
		}
		sources.add("Add new source");
		sources.remove("");
		return sources.toArray(new String[0]);
	}

	/**
	 * Call it when analysis, references, etc change (creation/modification/deletion).
	 * It will refresh all panels that could be affected.
	 */
	public void refreshPanels() {
		analysesPanel.fill();
		reportsPanel.fill();
		projectsPanel.fill();
		samplesPanel.fill();
	}
	
	public RelauncherPanel getRelauncherPanel() {
		return relauncherPanel;
	}
	
	//Overridden so we can exit when window is closed
	@Override
	protected void processWindowEvent(WindowEvent e) {
		super.processWindowEvent(e);
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			DB.disconnectAll();
			System.exit(0);
		}
	}

	/**
	 * Program arguments
	 * -u [username] : give username as parameter 
	 * -p [password] : give password as parameter
	 * -c [config file] : give config file to use as parameter
	 * -upoff				 : turn auto update off
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String argUser = null;
		String argPass = null;
		String argConfig = null;
		boolean updateCheck = true;
		for(int i=0 ; i < args.length ; i++){
			if (args[i].equalsIgnoreCase("-u")){
				argUser = args[++i];
			}else if (args[i].equalsIgnoreCase("-p")){
				argPass = args[++i];
			}else if (args[i].equalsIgnoreCase("-c")){
				argConfig = args[++i];
			}else if (args[i].equalsIgnoreCase("-upoff")){
				updateCheck = false;
			}
		}
		try {
			if (updateCheck) ApplicationLauncher.launchApplication("170", null, false, null);
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
					//Set Cut/Copy/Paste/SelectAll shortcuts with Command on Mac instead of Control
					if (Tools.isMac()){
						for (String map : new String[]{"EditorPane.focusInputMap","FormattedTextField.focusInputMap","PasswordField.focusInputMap","TextArea.focusInputMap","TextField.focusInputMap","TextPane.focusInputMap"}){
							im = (InputMap) UIManager.get(map);
							im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.META_DOWN_MASK), DefaultEditorKit.copyAction);
							im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.META_DOWN_MASK), DefaultEditorKit.pasteAction);
							im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.META_DOWN_MASK), DefaultEditorKit.cutAction);
							im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.META_DOWN_MASK), DefaultEditorKit.selectAllAction);
						}
						//IMPORTANT: JTable.processKeyBinding has a bug (it doesn't check if the meta key is pressed before triggering the cell editor)
						//Don't forget to override it in each JTable to allow the meta+V to work correctly on MacOSX 
						for (String map : new String[]{"List.focusInputMap","Table.ancestorInputMap","Tree.focusInputMap"}){
							im = (InputMap) UIManager.get(map);
							im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.META_DOWN_MASK), "copy");
							im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.META_DOWN_MASK), "paste");
							im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.META_DOWN_MASK), "cut");
							im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.META_DOWN_MASK), "selectAll");
						}			
					}	
					break;
				}
			}
		} catch (Exception ex) {
			try{
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}catch (Exception ex1) {
				ex1.printStackTrace();
			}
		}
		parameters = (argConfig == null) ? new Parameters(true) : new Parameters(true, new File(argConfig));
		try{
			int maxThreads = 2*Runtime.getRuntime().availableProcessors();
			Highlander.initialize(parameters, 15+maxThreads);
			DB = Highlander.getDB();
		}catch (Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Problem when connecting the database", ex), "Connecting to Highlander database",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
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
			Highlander.setLoggedUser(user);
			final ProjectManager pm = new ProjectManager();
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					pm.validate();
					//Center the window
					Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
					pm.setSize(new Dimension(screenSize.width/4*3, (int)(screenSize.height*0.95)));
					Dimension frameSize = pm.getSize();
					if (frameSize.height > screenSize.height) {
						frameSize.height = screenSize.height;
					}
					if (frameSize.width > screenSize.width) {
						frameSize.width = screenSize.width;
					}
					//pm.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
					pm.setLocation(5,5);
					//pm.setExtendedState(ProjectManager.MAXIMIZED_BOTH);
					pm.setVisible(true);
					
					ToolTipManager.sharedInstance().setDismissDelay(3600000);
				}
			});
		}
	}

}
