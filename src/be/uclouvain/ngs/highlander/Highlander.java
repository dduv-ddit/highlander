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

package be.uclouvain.ngs.highlander;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.InputMap;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import apple.dts.samplecode.osxadapter.OSXAdapter;
import be.uclouvain.ngs.highlander.UI.details.DetailsPanel;
import be.uclouvain.ngs.highlander.UI.dialog.ProfileTree;
import be.uclouvain.ngs.highlander.UI.dialog.ProfileTree.Action;
import be.uclouvain.ngs.highlander.UI.misc.HighlanderObserver;
import be.uclouvain.ngs.highlander.UI.misc.WaitingPanel;
import be.uclouvain.ngs.highlander.UI.misc.WaitingPanel.CancelException;
import be.uclouvain.ngs.highlander.UI.table.CellRenderer;
import be.uclouvain.ngs.highlander.UI.table.VariantsTable;
import be.uclouvain.ngs.highlander.UI.toolbar.DatabasePanel;
import be.uclouvain.ngs.highlander.UI.toolbar.FilteringPanel;
import be.uclouvain.ngs.highlander.UI.toolbar.HelpPanel;
import be.uclouvain.ngs.highlander.UI.toolbar.HighlightingPanel;
import be.uclouvain.ngs.highlander.UI.toolbar.LogoPanel;
import be.uclouvain.ngs.highlander.UI.toolbar.NavigationPanel;
import be.uclouvain.ngs.highlander.UI.toolbar.ProfilePanel;
import be.uclouvain.ngs.highlander.UI.toolbar.SearchPanel;
import be.uclouvain.ngs.highlander.UI.toolbar.SortingPanel;
import be.uclouvain.ngs.highlander.UI.toolbar.ToolsPanel;
import be.uclouvain.ngs.highlander.administration.users.LoginBox;
import be.uclouvain.ngs.highlander.administration.users.User;
import be.uclouvain.ngs.highlander.administration.users.User.UserData;
import be.uclouvain.ngs.highlander.database.Category;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.VariantResults;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
import be.uclouvain.ngs.highlander.datatype.ExternalLink;
import be.uclouvain.ngs.highlander.datatype.HighlightingRule;
import be.uclouvain.ngs.highlander.datatype.VariantsList;
import be.uclouvain.ngs.highlander.datatype.filter.ComboFilter;
import be.uclouvain.ngs.highlander.datatype.filter.Filter;

import com.install4j.api.launcher.ApplicationLauncher;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.text.DefaultEditorKit;

public class Highlander extends JFrame {

	static final public String version = "18";
	static final public String databaseRequiredVersion = "18";

	private static Parameters parameters;
	private static User user = null;
	private static HighlanderDatabase DB;

	private static List<AnalysisFull> analyses = new ArrayList<AnalysisFull>();
	private static AnalysisFull currentAnalysis;
	private static List<ExternalLink> externalLinks = new ArrayList<>();
	private static CellRenderer cellRenderer = new CellRenderer();
	private static HighlanderObserver obs = new HighlanderObserver();
	public JTabbedPane tabbedPane;

	private final VariantsTable variantsTable = new VariantsTable(this);
	private DatabasePanel databasePanel;
	private FilteringPanel filteringPanel;
	private SortingPanel sortingPanel;
	private SearchPanel searchPanel;
	private NavigationPanel navigationPanel;
	private HighlightingPanel highlightPanel;
	private ProfilePanel profilePanel;
	private ToolsPanel toolsPanel;
	private HelpPanel helpPanel;
	private LogoPanel logoPanel;
	private DetailsPanel detailsPanel;


	static public WaitingPanel waitingPanel;

	public static void initialize(Parameters p, int maxPoolSize) throws Exception {
		parameters = p;
		DB = new HighlanderDatabase(parameters,maxPoolSize);
		try {
			analyses = AnalysisFull.getAvailableAnalyses();
			currentAnalysis = analyses.get(0);
		}catch (Exception ex) {
			System.err.println("Analyses cannot be fetch from the database");
			ex.printStackTrace();
		}
		try {
			Category.fetchAvailableCategories(DB);
			Field.fetchAvailableFields(DB);
		}catch (Exception ex) {
			System.err.println("Database fields cannot be fetch from the database");
			ex.printStackTrace();
		}
	}
	
	public Highlander(){
		setIconImage(Resources.getScaledIcon(Resources.iHighlander, 32).getImage());
		setTitle("Highlander - " + user.toString() + " is currently connected.");
		Tools.centerWindow(this, true);
		initUI();
		registerForMacOSXEvents();
		try{
			addComponentListener(new ComponentListener() {
				@Override
				public void componentShown(ComponentEvent e) {}
				@Override
				public void componentResized(ComponentEvent e) {
					obs.setControlName("RESIZE_TOOLBAR");
				}
				@Override
				public void componentMoved(ComponentEvent e) {}

				@Override
				public void componentHidden(ComponentEvent e) {}
			});
		}catch(Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(), ex.getMessage(), "Fetching variant statistics", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
		refreshTable();
	}

	private void initUI(){
		getContentPane().setLayout(new BorderLayout());

		JPanel panel_commands = new JPanel();
		getContentPane().add(panel_commands, BorderLayout.NORTH);
		panel_commands.setLayout(new BorderLayout(0, 0));

		tabbedPane = new JTabbedPane(SwingConstants.TOP);
		panel_commands.add(tabbedPane, BorderLayout.NORTH);

		databasePanel = new DatabasePanel(this);
		tabbedPane.addTab(null, Resources.getScaledIcon(Resources.iDbStatus, 32), databasePanel, "Database");

		filteringPanel = new FilteringPanel(this, obs);
		tabbedPane.addTab(null, Resources.getScaledIcon(Resources.iFilter, 32), filteringPanel, "Filtering");

		navigationPanel = new NavigationPanel(this);
		tabbedPane.addTab(null, Resources.getScaledIcon(Resources.iNavigation, 32), navigationPanel, "Navigation");

		sortingPanel = new SortingPanel(this);
		tabbedPane.addTab(null, Resources.getScaledIcon(Resources.iSort, 32), sortingPanel, "Sorting");

		highlightPanel = new HighlightingPanel(this);
		tabbedPane.addTab(null, Resources.getScaledIcon(Resources.iHighlighting, 32), highlightPanel, "Highlighting");

		searchPanel = new SearchPanel(this);
		tabbedPane.addTab(null, Resources.getScaledIcon(Resources.iSearch, 32), searchPanel, "Search");

		profilePanel = new ProfilePanel(this);
		tabbedPane.addTab(null, Resources.getScaledIcon(Resources.iUser, 32), profilePanel, "Profile");

		toolsPanel = new ToolsPanel(this);
		tabbedPane.addTab(null, Resources.getScaledIcon(Resources.iTools, 32), toolsPanel, "Tools");

		helpPanel = new HelpPanel();
		tabbedPane.addTab(null, Resources.getScaledIcon(Resources.iHelp, 32), helpPanel, "Help");

		logoPanel = new LogoPanel();
		tabbedPane.addTab(null, Resources.getHeightScaledIcon(Resources.iLogoDeDuveUCLouvain, 32), logoPanel, "About Highlander");
		
		final JSplitPane splitpanel_main = new JSplitPane();
		splitpanel_main.setResizeWeight(0.7);
		splitpanel_main.setOneTouchExpandable(true);
		getContentPane().add(splitpanel_main, BorderLayout.CENTER);

		splitpanel_main.setLeftComponent(variantsTable);

		detailsPanel = new DetailsPanel(variantsTable);
		splitpanel_main.setRightComponent(detailsPanel);

		((BasicSplitPaneUI)splitpanel_main.getUI()).getDivider().addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {
				if (!variantsTable.getSelectedVariantsId().isEmpty()){
					detailsPanel.setSelection(variantsTable.getSelectedVariantsId().get(0),variantsTable);
				}
			}
			@Override
			public void mousePressed(MouseEvent e) {
			}
			@Override
			public void mouseExited(MouseEvent e) {
			}
			@Override
			public void mouseEntered(MouseEvent e) {
			}
			@Override
			public void mouseClicked(MouseEvent e) {
			}
		});

		waitingPanel = new WaitingPanel();
		setGlassPane(waitingPanel);
	}

	//Overridden so we can exit when window is closed
	@Override
	protected void processWindowEvent(WindowEvent e) {
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			if (filteringPanel.checkForChangesToBeSaved()){
				variantsTable.saveUserColumnWidths();
				DB.disconnectAll();
				super.processWindowEvent(e);
				System.exit(0);
			}
		}
	}

	// General info dialog; fed to the OSXAdapter as the method to call when 
	// "About OSXAdapter" is selected from the application menu
	public void about() {
		logoPanel.about();
	}

	// General preferences dialog; fed to the OSXAdapter as the method to call when
	// "Preferences..." is selected from the application menu
	public void preferences() {
		User.editUserProfile();
	}

	// General quit handler; fed to the OSXAdapter as the method to call when a system quit event occurs
	// A quit event is triggered by Cmd-Q, selecting Quit from the application or Dock menu, or logging out
	public boolean quit() {
		boolean quit = filteringPanel.checkForChangesToBeSaved();
		if (quit){
			variantsTable.saveUserColumnWidths();
			DB.disconnectAll();
		}
		return quit; 
	}

	public void registerForMacOSXEvents() {
		if (Tools.isMac()) {
			try {
				// Generate and register the OSXAdapter, passing it a hash of all the methods we wish to
				// use as delegates for various com.apple.eawt.ApplicationListener methods
				OSXAdapter.setQuitHandler(this, getClass().getDeclaredMethod("quit", (Class[])null));
				OSXAdapter.setAboutHandler(this, getClass().getDeclaredMethod("about", (Class[])null));
				OSXAdapter.setPreferencesHandler(this, getClass().getDeclaredMethod("preferences", (Class[])null));
				OSXAdapter.setDockIconImage(Resources.getScaledIcon(Resources.iHighlander, 64).getImage());
			} catch (Exception ex) {
				System.err.println("Error while loading the OSXAdapter:");
				Tools.exception(ex);
			}
		}
	}

	public void changeAnalysis(AnalysisFull analysis){
		Filter f = filteringPanel.getFilter();
		if (f != null && !f.checkFieldCompatibility(analysis)){
			int res = JOptionPane.showConfirmDialog(new JFrame(), 
					"Some fields in your filter '" + f.getFilterType().getName() + "' are incompatible with the selected analysis. " +
							"\nThis filter will be removed before switching to analysis '"+analysis+"', so make sure it has been saved to your profile if required. " +
							"\nDo you still want to switch to analysis '"+analysis+"' now ?", 
							"Switching analysis", JOptionPane.YES_NO_OPTION , JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			if (res == JOptionPane.NO_OPTION){
				databasePanel.switchBack(currentAnalysis);
				return;
			}
		}
		for (HighlightingRule h : highlightPanel.getHighlightingRules()){
			if (!h.checkFieldCompatibility(analysis)){
				int res = JOptionPane.showConfirmDialog(new JFrame(), 
						"Some fields in your highlighting rules '" + h.getFieldName() + "' are incompatible with the selected analysis. " +
								"\nThis highlighting criterion will be removed before switching to analysis '"+analysis+"', so make sure it has been saved to your profile if required. " +
								"\nDo you still want to switch to analysis '"+analysis+"' now ?", 
								"Switching analysis", JOptionPane.YES_NO_OPTION , JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				if (res == JOptionPane.NO_OPTION){
					databasePanel.switchBack(currentAnalysis);
					return;
				}
			}
		}
		navigationPanel.resetSelections();
		databasePanel.switchAnalysis(analysis);
		Filter i = filteringPanel.getFilter();
		if (i != null && !i.changeAnalysis(analysis)){
			i.delete();				
		}
		for (HighlightingRule j : highlightPanel.getHighlightingRules()){
			if (!j.changeAnalysis(analysis)){
				j.delete();				
			}
		}
		currentAnalysis = analysis;
		refreshTable();
		variantsTable.setMessageToCurrentAnalysis();
	}

	public static User login(){
		LoginBox loginBox = new LoginBox(new JFrame(), "Highlander "+version+" login at "+(parameters.getDbMainHost() != null ? parameters.getDbMainHost() : parameters.getDbMainJdbc()), true) ;
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize() ;
		Dimension windowSize = loginBox.getSize() ;
		loginBox.setLocation(Math.max(0, (screenSize.width - windowSize.width) / 2),
				Math.max(0, (screenSize.height - windowSize.height) / 2)) ;
		loginBox.setVisible(true) ;
		if (loginBox.OKCancel) {
			try {
				loginBox.setProxyPasswordIfNecessary();
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

	public static void setLoggedUser(User u){
		user = u;  	
	}

	public static HighlanderDatabase getDB(){
		return DB;  	
	}

	public static User getLoggedUser(){
		return user;  	
	}

	public static Parameters getParameters(){
		return parameters;  	
	}

	public static List<AnalysisFull> getAvailableAnalyses(){
		return analyses;
	}

	public static AnalysisFull getAnalysis(String name){
		for (AnalysisFull a : analyses) {
			if (a.toString().equals(name)) return a;
		}
		return null;
	}

	public static void setCurrentAnalysis(AnalysisFull analysis){
		currentAnalysis = analysis;  	
	}

	public static AnalysisFull getCurrentAnalysis(){
		return currentAnalysis;  	
	}
	
	public static List<ExternalLink> getAvailableExternalLinks(){
		return externalLinks;
	}
	
	public static CellRenderer getCellRenderer(){
		return cellRenderer;
	}

	public static HighlanderObserver getHighlanderObserver(){
		return obs;
	}

	public VariantsTable getVariantTable(){
		return variantsTable;
	}

	public DetailsPanel getDetailsPanel(){
		return detailsPanel;
	}

	public ComboFilter getCurrentFilter(){
		return filteringPanel.getFilter();
	}

	public String getCurrentFilterName(){
		return filteringPanel.getFilterName();
	}

	public List<Field> getColumnSelection(){
		return databasePanel.getColumnSelection();
	}

	public String getSelectedColumnSelectionName(){
		return databasePanel.getColumnSelectionName();
	}

	public String getSelectedMaskName(){
		return navigationPanel.getColumnMaskName();
	}

	public void refreshTable(){
		if (filteringPanel != null && filteringPanel.getFilter() != null){
			if (filteringPanel.getFilter().isFilterValid()){
				try {
					waitingPanel.start(true);
					variantsTable.saveUserColumnWidths();
					List<Field> headers = getColumnSelection();
					VariantResults variantResults = filteringPanel.getFilter().retreiveData(headers, filteringPanel.getFilter().getAllSamples(), "Query results");
					Highlander.waitingPanel.setProgressString("Populating table", true);
					if (variantResults != null) variantsTable.fillTable(variantResults);
					Highlander.waitingPanel.setProgressDone();
				} catch (CancelException ex){
					waitingPanel.setProgressString("Cancelling query", true);
				} catch (com.mysql.cj.jdbc.exceptions.MySQLStatementCancelledException ex){
					waitingPanel.setProgressString("Cancelling query", true);
				}catch (Exception ex){
					Tools.exception(ex);
					JOptionPane.showMessageDialog(this, Tools.getMessage("Problem when executing query", ex), "Executing query",
							JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				}finally{
					waitingPanel.forceStop();
				}
				new Thread(new Runnable() {
					@Override
					public void run() {
						System.gc();
					}
				}, "Highlander.garbageCollector").start();    
			}else{
				StringBuilder sb = new StringBuilder();
				sb.append("Your query can't be sent to the database.\n" +
						"Causes are:\n");
				for (String cause : filteringPanel.getFilter().getValidationProblems()){
					sb.append("-" + cause + "\n");
				}
				JOptionPane.showMessageDialog(new JFrame(), sb.toString(), "Invalid filter",
						JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iFilter,64));
			}
		}else{
			variantsTable.showWelcome();
		}
	}

	public void refreshTableView(){
		variantsTable.setHiddenColumns(navigationPanel.getColumnMask());
		sortingPanel.removeMaskedColumns();
		variantsTable.applyFilters();
	}

	public void saveVariantList(){
		try {
			String listName = ProfileTree.showProfileDialog(this, Action.SAVE, UserData.VARIANT_LIST, Highlander.getCurrentAnalysis().toString(), "Save "+UserData.VARIANT_LIST.getName()+" to your profile");
			if (listName == null) return;
			if (Highlander.getLoggedUser().doesPersonalDataExists(UserData.VARIANT_LIST, getCurrentAnalysis().toString(), listName)){
				int yesno = JOptionPane.showConfirmDialog(new JFrame(), 
						"You already have a variants list named '"+listName.replace("~", " -> ")+"', do you want to overwrite it ?", 
						"Overwriting variants list in your profile", JOptionPane.YES_NO_OPTION , JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iDbSave,64));
				if (yesno == JOptionPane.NO_OPTION)	return;
			}
			VariantsList list = new VariantsList(getCurrentAnalysis(), getColumnSelection(), getCurrentFilter(), 
					navigationPanel.getColumnMask(), sortingPanel.getSortingCriteria(), highlightPanel.getHighlightingRules(), variantsTable.getAllVariantsIds(), listName);
			Highlander.getLoggedUser().saveVariantList(list, listName);
		} catch (Exception ex) {
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Error", ex), "Save current variants list in your profile", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
	}

	public void loadVariantList(){
		String listname = ProfileTree.showProfileDialog(this, Action.LOAD, UserData.VARIANT_LIST, Highlander.getCurrentAnalysis().toString());
		if (listname != null){					
			try {
				VariantsList list = Highlander.getLoggedUser().loadVariantList(getCurrentAnalysis(), listname, filteringPanel, databasePanel, navigationPanel, sortingPanel, highlightPanel, variantsTable);
				list.restore();
			} catch (Exception ex) {
				Tools.exception(ex);
				JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Error", ex), "Load variants list from your profile", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			}
		}
	}

	/**
	 * 
	 * Program arguments
	 * -u [username]	 	: give username as parameter 
	 * -p [password] 		: give password as parameter
	 * -c [config file] : give config file to use as parameter
	 * -upoff				 		: turn auto update off
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
		Parameters p = (argConfig == null) ? new Parameters(true) : new Parameters(true, new File(argConfig));
		try{
			Highlander.initialize(p, 5);
		}catch (ClassNotFoundException ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Cannot connect to the database", ex), "Connecting to Highlander database",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}catch (SQLException ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Cannot connect to the database", ex), "Connecting to Highlander database",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			System.exit(-1);
		}catch (Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Problem when connecting the database", ex), "Connecting to Highlander database",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
		try{
			if (!DB.checkDatabaseVersionCompatibility()){
				String dbVersion = "?";
				try (Results res = DB.select(Schema.HIGHLANDER,"SELECT version FROM main")) {
					if(res.next()){
						dbVersion = res.getString("version");
					}
				}  		
				JOptionPane.showMessageDialog(new JFrame(), 
						"Highlander version "+version+" requires a database version "+databaseRequiredVersion+".\n"
								+ "Database on "+parameters.getDbMainHost()+" is version "+dbVersion+".\n"
								+ "Please update this application or the database to a compatible version.", 
								"Checking database version compatibility",
								JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				System.exit(-2);	
			}
		}catch (Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Cannot retrieve database version", ex), "Checking database version compatibility",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));			
			System.exit(-2);
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
		try {
			Field.setUserCustomWidths(DB, user);
			externalLinks = ExternalLink.getAvailableExternalLinks();
		} catch (Exception ex) {
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Cannot retreive Highlander field user widths", ex), "Retreiving Highlander field user widths",
					JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));

		}
		final Highlander highlander = new Highlander();

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				highlander.validate();
				//Center the window
				Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
				Dimension frameSize = highlander.getSize();
				if (frameSize.height > screenSize.height) {
					frameSize.height = screenSize.height;
				}
				if (frameSize.width > screenSize.width) {
					frameSize.width = screenSize.width;
				}
				highlander.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
				highlander.setExtendedState(Frame.MAXIMIZED_BOTH);
				highlander.setVisible(true);

				ToolTipManager.sharedInstance().setDismissDelay(3600000);
				new Thread(new Runnable() {
					@Override
					public void run() {
						while (true){
							getLoggedUser().checkForNewSharedElement(highlander);
							try{
								Thread.sleep(300_000);
							}catch (InterruptedException ex){
								Tools.exception(ex);
							}
						}
					}
				}, "User.checkForNewSharedElement").start();
			}
		});
	}

}
