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

package be.uclouvain.ngs.highlander.administration.users;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SortOrder;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.dialog.ProfileTree;
import be.uclouvain.ngs.highlander.UI.dialog.ProfileTree.Action;
import be.uclouvain.ngs.highlander.UI.table.VariantsTable;
import be.uclouvain.ngs.highlander.UI.toolbar.DatabasePanel;
import be.uclouvain.ngs.highlander.UI.toolbar.FilteringPanel;
import be.uclouvain.ngs.highlander.UI.toolbar.HighlightingPanel;
import be.uclouvain.ngs.highlander.UI.toolbar.NavigationPanel;
import be.uclouvain.ngs.highlander.UI.toolbar.SortingPanel;
import be.uclouvain.ngs.highlander.administration.users.UserDataDialog.UserDataDialogType;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.FiltersTemplate;
import be.uclouvain.ngs.highlander.datatype.HPOTerm;
import be.uclouvain.ngs.highlander.datatype.HeatMapCriterion;
import be.uclouvain.ngs.highlander.datatype.HighlightCriterion;
import be.uclouvain.ngs.highlander.datatype.HighlightingRule;
import be.uclouvain.ngs.highlander.datatype.Interval;
import be.uclouvain.ngs.highlander.datatype.Reference;
import be.uclouvain.ngs.highlander.datatype.SortingCriterion;
import be.uclouvain.ngs.highlander.datatype.VariantsList;
import be.uclouvain.ngs.highlander.datatype.HighlightingRule.RuleType;
import be.uclouvain.ngs.highlander.datatype.filter.ComboFilter;

public class User implements Comparable<User> {

	public enum UserDataLink {NONE, ANALYSIS, REFERENCE, FIELD}
	public enum Settings {POSITION, VISIBLE, WIDTH, COLOR, LAST_SELECTION, DEFAULT_COLUMNS}
	public enum TargetColor {VARIANT_TABLE, SAME_VARIANT}
	public enum TargetLastSelection {COLUMN_SELECTION}
	
	public enum UserData {
		FOLDER("folder",UserDataLink.NONE,Resources.iFolder,Resources.iUserFolderShare), 
		SETTINGS("settings",UserDataLink.NONE,Resources.iEditWrench,Resources.iUsers), 
		HISTORY("history",UserDataLink.NONE,Resources.iEditWrench,Resources.iUsers),
		VALUES("list of values",UserDataLink.FIELD,Resources.iList,Resources.iUserListShare), 
		INTERVALS("list of intervals",UserDataLink.REFERENCE,Resources.iInterval,Resources.iUserIntervalsShare), 
		PHENOTYPES("list of HPO terms",UserDataLink.REFERENCE,Resources.iHPO,Resources.iUserHPOShare), 
		COLUMN_SELECTION("columns selection",UserDataLink.ANALYSIS,Resources.iColumnSelection,Resources.iUserColumnSelectionShare), 
		FILTER("filter",UserDataLink.ANALYSIS,Resources.iFilter,Resources.iUserFilterShare), 
		COLUMN_MASK("columns mask",UserDataLink.ANALYSIS,Resources.iColumnMask,Resources.iUserColumnMaskShare), 
		SORTING("sorting criteria",UserDataLink.ANALYSIS,Resources.iSort,Resources.iUserSortingShare), 
		HIGHLIGHTING("highlighting rules",UserDataLink.ANALYSIS,Resources.iHighlighting,Resources.iUserHighlightingShare), 
		VARIANT_LIST("variants list",UserDataLink.ANALYSIS,Resources.iVariantList,Resources.iUserVariantListShare),
		FILTERS_TEMPLATE("filters template",UserDataLink.ANALYSIS,Resources.iUserTemplate,Resources.iUserTemplateShare),
		;
		private final String name;
		private final UserDataLink link;
		private final ImageIcon icon;
		private final ImageIcon sharingIcon;
		UserData(String name, UserDataLink link, ImageIcon icon, ImageIcon sharingIcon){
			this.name = name;
			this.link = link;
			this.icon = Resources.getScaledIcon(icon, 24);
			this.sharingIcon = Resources.getScaledIcon(sharingIcon, 64);
		}
		public String getName(){return name;}
		public UserDataLink getLink(){return link;}
		public boolean isLinked(){return link != UserDataLink.NONE;}
		public boolean isAnalysisLinked(){return link == UserDataLink.ANALYSIS;}
		public boolean isReferenceLinked(){return link == UserDataLink.REFERENCE;}
		public boolean isFieldLinked(){return link == UserDataLink.FIELD;}
		public ImageIcon getIcon(){return icon;}
		public ImageIcon getSharingIcon(){return sharingIcon;}
	}
	public enum Rights {user, administrator, inactive}

	private static final HighlanderDatabase DB = Highlander.getDB();
	private final String username;
	private String password;
	private String first_name;
	private String last_name;
	private String email;
	private String group;
	private Rights rights;

	public User(String username) {
		this.username = username;
	}

	public User(String username, String password) throws Exception {
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT * FROM `users` WHERE `username` = '"+username+"' AND `password` = '"+password+"'")) {
			this.username = username;
			if (res.next()){
				setPassword(password);
				setFirstName(res.getString("first_name"));
				setLastName(res.getString("last_name"));
				setEmail(res.getString("email"));
				try { setGroup(res.getString("group")); }catch(Exception e) { } //before v17, no group, can cause crash at startup
				setRights(Rights.valueOf(res.getString("rights")));
				if (getRights() == Rights.inactive) {
					throw new Exception("Your account has been deactivated, please contact an administrator.");
				}
			}else{
				throw new Exception("Wrong identifier or password.");
			}
		}
	}

	@Override
	public String toString(){
		return first_name + " " + last_name;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getFirstName() {
		return first_name;
	}

	public void setFirstName(String firstName) {
		this.first_name = firstName;
	}

	public String getLastName() {
		return last_name;
	}

	public void setLastName(String lastName) {
		this.last_name = lastName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getGroup() {
		return group;
	}
	
	public void setGroup(String group) {
		this.group = group;
	}
	
	public static String[] getExistingGroups() {
		List<String> list = new ArrayList<>();
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT DISTINCT(`group`) FROM `users` ORDER BY `group`") ){
			while (res.next()) {
				list.add(res.getString("group"));
			}
		}catch (Exception ex) {
			ex.printStackTrace();
		}
		return list.toArray(new String[0]);
	}
	
	public Rights getRights() {
		return rights;
	}

	public void setRights(Rights rights) {
		this.rights = rights;
	}

	public boolean isAdmin(){
		return rights == Rights.administrator;
	}

	public boolean isInactive(){
		return rights == Rights.inactive;
	}
	
	public boolean isAnotherUserAdmin(){
		try {
			for (User otherUser : fetchList()){
				if (!otherUser.username.equals(username) && otherUser.isAdmin()){
					return true;
				}
			}
		} catch (Exception ex) {
			Tools.exception(ex);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return username.hashCode();
	}

	@Override
	public boolean equals(Object obj){
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		return username.equalsIgnoreCase(((User)obj).getUsername());
	}

	@Override
	public int compareTo (User a) {
		return username.compareTo(a.username);
	} 

	public boolean hasPermissionToModify(Analysis analysis, Set<Integer> variantIds) throws Exception {
		if (isAdmin()) return true;
		Set<Integer> idsToTest = new HashSet<>(variantIds);
		if (!idsToTest.isEmpty()){
			try (Results res = DB.select(Schema.HIGHLANDER, 
					"SELECT `variant_sample_id` "
							+ "FROM "+analysis.getFromSampleAnnotations()
							+ analysis.getJoinProjects()
							+ "JOIN `projects_users` USING (`project_id`) "
							+ "WHERE `variant_sample_id` IN ("+HighlanderDatabase.makeSqlList(variantIds, Integer.class)+") "
							+ "AND `username` = '"+username+"'"
					)) {
				while (res.next()){
					idsToTest.remove(res.getInt("variant_sample_id"));
				}
			}
		}
		return idsToTest.isEmpty();
	}

	public void saveFolder(String key, UserData userData, String category) throws Exception {
		deleteFolder(key, userData, category);
		if (category != null){
			DB.update(Schema.HIGHLANDER, "INSERT INTO `users_data` (`username`, `type`, `analysis`, `key`, `value`) VALUES (" +
					"'"+DB.format(Schema.HIGHLANDER, username)+"'," +
					"'"+DB.format(Schema.HIGHLANDER, UserData.FOLDER.toString())+"'," +
					"'"+DB.format(Schema.HIGHLANDER, category)+"'," +
					"'"+DB.format(Schema.HIGHLANDER, key)+"'," +
					"'"+userData.toString()+"')");
		}else{
			DB.update(Schema.HIGHLANDER, "INSERT INTO `users_data` (`username`, `type`, `key`, `value`) VALUES (" +
					"'"+DB.format(Schema.HIGHLANDER, username)+"'," +
					"'"+DB.format(Schema.HIGHLANDER, UserData.FOLDER.toString())+"'," +
					"'"+DB.format(Schema.HIGHLANDER, key)+"'," +
					"'"+userData.toString()+"')");

		}
	}

	public void deleteFolder(String key, UserData userData, String category) throws Exception {
		if (category != null){
			DB.update(Schema.HIGHLANDER, "DELETE FROM `users_data` WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' " +
					"AND `type` = '"+DB.format(Schema.HIGHLANDER, UserData.FOLDER.toString())+"' AND `key` = '"+DB.format(Schema.HIGHLANDER, key)+"' AND `value` = '"+DB.format(Schema.HIGHLANDER, userData.toString())+"' AND `analysis` = '"+DB.format(Schema.HIGHLANDER, category)+"' ");
		}else{
			DB.update(Schema.HIGHLANDER, "DELETE FROM `users_data` WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' " +
					"AND `type` = '"+DB.format(Schema.HIGHLANDER, UserData.FOLDER.toString())+"' AND `key` = '"+DB.format(Schema.HIGHLANDER, key)+"' AND `value` = '"+DB.format(Schema.HIGHLANDER, userData.toString())+"'");			
		}
	}

	/*
	 * Only rename given folder, must also be used on all descendants
	 */
	public void renameFolder(String oldKey, String newKey, UserData userData, String oldCategory, String newCategory, String value) throws Exception {
		String query = "UPDATE `users_data` SET `key` = '"+DB.format(Schema.HIGHLANDER, newKey)+"' ";
		if (newCategory != null) query += ", `analysis` = '"+DB.format(Schema.HIGHLANDER, newCategory)+"' ";
		query += "WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"'";
		query += " AND `key` = '"+DB.format(Schema.HIGHLANDER, oldKey)+"'";
		query += " AND `type` = '"+DB.format(Schema.HIGHLANDER, userData.toString())+"'";
		if (oldCategory != null) query += " AND `analysis` = '"+DB.format(Schema.HIGHLANDER, oldCategory)+"'";
		if (value != null) query += " AND `value` = '"+DB.format(Schema.HIGHLANDER, value)+"'";
		DB.update(Schema.HIGHLANDER, query);
	}

	public List<String> loadFolders(UserData userData) throws Exception {
		return loadFolders(userData, null);
	}
	
	public List<String> loadFolders(UserData userData, String category) throws Exception {
		String query  = (category != null) ? 
				"SELECT `key`, `value` FROM `users_data` " +
				"WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' AND `type` = '"+UserData.FOLDER+"' AND `analysis` = '"+DB.format(Schema.HIGHLANDER, category)+"' AND `value` = '"+userData+"' ORDER BY `key`" 
				: 
					"SELECT `key`, `value` FROM `users_data` " +
					"WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' AND `type` = '"+UserData.FOLDER+"' AND `value` = '"+userData+"' ORDER BY `key`";
		List<String> list = new ArrayList<String>();
		try (Results res = DB.select(Schema.HIGHLANDER, query)) {
			while (res.next()){
				list.add(res.getString("key"));
			}
		}
		Collections.sort(list, new Tools.NaturalOrderComparator(true));
		return list;	
	}

	public Map<UserData, Map<String, List<String>>> searchElements(String criterion) throws Exception {
		String query  = 
				"SELECT `type`, `users_data`.`analysis`, `key` "
						+ "FROM `users_data` LEFT JOIN analyses ON users_data.analysis = analyses.analysis "
						+ "WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' "
						+ "AND type != '"+DB.format(Schema.HIGHLANDER, UserData.SETTINGS.toString())+"' "
						+ "AND type != '"+DB.format(Schema.HIGHLANDER, UserData.HISTORY.toString())+"' "
						+ "AND type != '"+DB.format(Schema.HIGHLANDER, UserData.FOLDER.toString())+"' "
						+ "AND INSTR(`key`,'"+DB.format(Schema.HIGHLANDER, criterion)+"') "
						+ "GROUP BY `type`, `users_data`.`analysis`, `key` "
						+ "ORDER BY `type`, `analyses`.`ordering`, `key`";
		Map<UserData, Map<String, List<String>>> map = new TreeMap<>();
		try (Results res = DB.select(Schema.HIGHLANDER, query)) {
			while (res.next()){
				UserData type = UserData.valueOf(res.getString("type"));
				String category = (res.getObject("analysis") != null) ? res.getString("analysis") : null;
				String key = res.getString("key");
				String keyTip = (key.length() > 0) ? key.split("~")[key.split("~").length-1] : "";
				if (keyTip.toUpperCase().contains(criterion.toUpperCase())) {
					if (!map.containsKey(type)) {
						map.put(type, new LinkedHashMap<String,List<String>>());
					}
					if (!map.get(type).containsKey(category)) {
						map.get(type).put(category, new LinkedList<String>());
					}
					map.get(type).get(category).add(key);
				}
			}
		}
		query  = 
				"SELECT `type`, `users_data`.`analysis`, `key`, `value` "
						+ "FROM `users_data` LEFT JOIN analyses ON users_data.analysis = analyses.analysis "
						+ "WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' "
						+ "AND type = '"+DB.format(Schema.HIGHLANDER, UserData.FOLDER.toString())+"' "
						+ "AND INSTR(`key`,'"+DB.format(Schema.HIGHLANDER, criterion)+"') "
						+ "GROUP BY `type`, `users_data`.`analysis`, `key`, `value` "
						+ "ORDER BY `type`, `analyses`.`ordering`, `key`, `value`";
		try (Results res = DB.select(Schema.HIGHLANDER, query)) {
			while (res.next()){
				UserData type = UserData.valueOf(res.getString("type"));
				String category = (res.getObject("analysis") != null) ? res.getString("analysis") : null;
				String key = res.getString("key");
				String keyTip = (key.length() > 0) ? key.split("~")[key.split("~").length-1] : "";
				key = res.getString("value") + "|" + key;
				if (keyTip.toUpperCase().contains(criterion.toUpperCase())) {
					if (!map.containsKey(type)) {
						map.put(type, new LinkedHashMap<String,List<String>>());
					}
					if (!map.get(type).containsKey(category)) {
						map.get(type).put(category, new LinkedList<String>());
					}
					map.get(type).get(category).add(key);
				}
			}
		}
		return map;
	}

	public void saveSettings(Settings setting, String key, String value) throws Exception {
		deleteData(UserData.SETTINGS, setting+"|"+key);
		DB.update(Schema.HIGHLANDER, "INSERT INTO `users_data` (`username`, `type`, `key`, `value`) VALUES (" +
				"'"+DB.format(Schema.HIGHLANDER, username)+"'," +
				"'"+DB.format(Schema.HIGHLANDER, UserData.SETTINGS.toString())+"'," +
				"'"+DB.format(Schema.HIGHLANDER, setting+"|"+key)+"'," +
				"'"+DB.format(Schema.HIGHLANDER, value)+"')");
	}

	public void saveSettings(String category, Settings setting, String key, String value) throws Exception {
		deleteData(UserData.SETTINGS, category, setting+"|"+key);
		DB.update(Schema.HIGHLANDER, "INSERT INTO `users_data` (`username`, `type`, `analysis`, `key`, `value`) VALUES (" +
				"'"+DB.format(Schema.HIGHLANDER, username)+"'," +
				"'"+DB.format(Schema.HIGHLANDER, UserData.SETTINGS.toString())+"'," +
				"'"+DB.format(Schema.HIGHLANDER, category)+"'," +
				"'"+DB.format(Schema.HIGHLANDER, setting+"|"+key)+"'," +
				"'"+DB.format(Schema.HIGHLANDER, value)+"')");
	}

	public String loadSetting(Settings setting, String key) throws Exception {
		String value = null;
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `value` FROM `users_data` " +
				"WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' AND `type` = '"+DB.format(Schema.HIGHLANDER, UserData.SETTINGS.toString())
				+"' AND `key` = '"+DB.format(Schema.HIGHLANDER, setting+"|"+key)+"'")){
			if (res.next()){
				value = res.getString("value");
			}
		}
		return value;
	}

	public String loadSetting(String category, Settings setting, String key) throws Exception {
		String value = null;
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `value` FROM `users_data` " +
				"WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' AND `type` = '"+DB.format(Schema.HIGHLANDER, UserData.SETTINGS.toString())+
				"' AND `analysis` = '"+DB.format(Schema.HIGHLANDER, category)+"' AND `key` = '"+DB.format(Schema.HIGHLANDER, setting+"|"+key)+"'")){
			if (res.next()){
				value = res.getString("value");
			}
		}
		return value;
	}

	public Map<String,String> loadSettings(Settings setting) throws Exception {
		Map<String,String> values = new HashMap<String, String>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `key`,`value` FROM `users_data` " +
				"WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' AND `type` = '"+DB.format(Schema.HIGHLANDER, UserData.SETTINGS.toString())
				+"' AND INSTR(`key`,'"+DB.format(Schema.HIGHLANDER, setting.toString())+"') > 0")){
			while (res.next()){
				values.put(res.getString("key"), res.getString("value"));
			}
		}
		return values;
	}

	public Map<String,String> loadSettings(String category, Settings setting) throws Exception {
		Map<String,String> values = new HashMap<String, String>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `key`,`value` FROM `users_data` " +
				"WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' AND `type` = '"+DB.format(Schema.HIGHLANDER, UserData.SETTINGS.toString())
				+"' AND `analysis` = '"+DB.format(Schema.HIGHLANDER, category)+"' AND INSTR(`key`,'"+DB.format(Schema.HIGHLANDER, setting.toString())+"') > 0")){
			while (res.next()){
				values.put(res.getString("key"), res.getString("value"));
			}
		}
		return values;
	}

	public static void saveDefaultSettings(String category, Settings setting, List<String> values) throws Exception {
		deleteDefaultSetting(category, setting);
		for (String value : values) {
			DB.update(Schema.HIGHLANDER, "INSERT INTO `users_data` (`username`, `type`, `analysis`, `key`, `value`) VALUES (" +
				"'"+DB.format(Schema.HIGHLANDER, "default")+"'," +
				"'"+DB.format(Schema.HIGHLANDER, UserData.SETTINGS.toString())+"'," +
				"'"+DB.format(Schema.HIGHLANDER, category)+"'," +
				"'"+DB.format(Schema.HIGHLANDER, setting.toString())+"'," +
				"'"+DB.format(Schema.HIGHLANDER, value)+"')");
		}
	}
		
	public static List<String> loadDefaultSettings(String category, Settings setting) throws Exception {
		List<String> values = new ArrayList<>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `key`,`value` FROM `users_data` " +
				"WHERE `username` = 'default' AND `type` = '"+DB.format(Schema.HIGHLANDER, UserData.SETTINGS.toString())
				+"' AND `analysis` = '"+DB.format(Schema.HIGHLANDER, category)
				+"' AND INSTR(`key`,'"+DB.format(Schema.HIGHLANDER, setting.toString())+"') > 0 "
				+ "ORDER BY id")){
			while (res.next()){
				values.add(res.getString("value"));
			}
		}
		return values;
	}
	
	public void renameData(UserData userData, String oldListName, String newListName) throws Exception {
		DB.update(Schema.HIGHLANDER, "UPDATE `users_data` SET `key` = '"+DB.format(Schema.HIGHLANDER, newListName)+"' WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' " +
				"AND `type` = '"+DB.format(Schema.HIGHLANDER, userData.toString())+"' AND `key` = '"+DB.format(Schema.HIGHLANDER, oldListName)+"'");
		if (userData == UserData.VALUES || userData == UserData.INTERVALS || userData == UserData.PHENOTYPES){
			DB.update(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'"+DB.format(Schema.HIGHLANDER, oldListName)+"','"+DB.format(Schema.HIGHLANDER, newListName)+"') WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' " +
					"AND (`type` = 'FILTER' OR `type` = 'HIGHLIGHTING') AND INSTR(`value`, '"+DB.format(Schema.HIGHLANDER, oldListName)+"') > 0");
		}
	}

	public void renameData(UserData userData, String oldCategory, String newCategory, String oldListName, String newListName) throws Exception {
		DB.update(Schema.HIGHLANDER, "UPDATE `users_data` SET `key` = '"+DB.format(Schema.HIGHLANDER, newListName)+"', `analysis` = '"+DB.format(Schema.HIGHLANDER, newCategory)+"' WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' " +
				"AND `type` = '"+DB.format(Schema.HIGHLANDER, userData.toString())+"' AND `analysis` = '"+DB.format(Schema.HIGHLANDER, oldCategory)+"' AND `key` = '"+DB.format(Schema.HIGHLANDER, oldListName)+"'");
	}

	public void deleteData(UserData userData, String listName) throws Exception {
		DB.update(Schema.HIGHLANDER, "DELETE FROM `users_data` WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' " +
				"AND `type` = '"+DB.format(Schema.HIGHLANDER, userData.toString())+"' AND `key` = '"+DB.format(Schema.HIGHLANDER, listName)+"'");
	}

	public void deleteData(UserData userData, String category, String listName) throws Exception {
		DB.update(Schema.HIGHLANDER, "DELETE FROM `users_data` WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' " +
				"AND `type` = '"+DB.format(Schema.HIGHLANDER, userData.toString())+"' AND `analysis` = '"+DB.format(Schema.HIGHLANDER, category)+"' AND `key` = '"+DB.format(Schema.HIGHLANDER, listName)+"'");
	}

	public static void deleteDefaultSetting(String category, Settings setting) throws Exception {
		DB.update(Schema.HIGHLANDER, "DELETE FROM `users_data` WHERE `username` = 'default' " +
				"AND `type` = '"+DB.format(Schema.HIGHLANDER, UserData.SETTINGS.toString())
				+"' AND `analysis` = '"+DB.format(Schema.HIGHLANDER, category)
				+"' AND INSTR(`key`,'"+DB.format(Schema.HIGHLANDER, setting.toString())+"') > 0");
	}
	
	public void shareData(UserData userData, String fullpath, String key, User receiver) throws Exception {
		List<String> values = new ArrayList<String>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `value` FROM `users_data` WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' " +
				"AND `type` = '"+DB.format(Schema.HIGHLANDER, userData.toString())+"' AND `key` = '"+DB.format(Schema.HIGHLANDER, fullpath)+"' ORDER BY id")){
			while (res.next()){
				values.add(res.getString(1));			
			}
		}
		StringBuilder sb = new StringBuilder();
		for (String val : values){
			sb.append(" (" +
					"'"+DB.format(Schema.HIGHLANDER, receiver.getUsername())+"'," +
					"'"+DB.format(Schema.HIGHLANDER, userData.toString())+"'," +
					"'"+DB.format(Schema.HIGHLANDER, "SHARE|"+username+"|"+key)+"'," +
					"'"+DB.format(Schema.HIGHLANDER, val)+"'),");
		}
		if(!values.isEmpty()) {
			DB.update(Schema.HIGHLANDER, "INSERT INTO `users_data` (`username`, `type`, `key`, `value`) VALUES" + sb.deleteCharAt(sb.length()-1).toString());
		}
	}

	public void shareData(UserData userData, String category, String fullpath, String key, User receiver) throws Exception {
		List<String> values = new ArrayList<String>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `value` FROM `users_data` WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' " +
				"AND `type` = '"+DB.format(Schema.HIGHLANDER, userData.toString())+"' AND `analysis` = '"+DB.format(Schema.HIGHLANDER, category)+"' AND `key` = '"+DB.format(Schema.HIGHLANDER, fullpath)+"' ORDER BY id")){
			while (res.next()){
				values.add(res.getString(1));			
			}
		}
		StringBuilder sb = new StringBuilder();
		for (String val : values){
			sb.append(" (" +
					"'"+DB.format(Schema.HIGHLANDER, receiver.getUsername())+"'," +
					"'"+DB.format(Schema.HIGHLANDER, userData.toString())+"'," +
					"'"+DB.format(Schema.HIGHLANDER, category)+"'," +
					"'"+DB.format(Schema.HIGHLANDER, "SHARE|"+username+"|"+key)+"'," +
					"'"+DB.format(Schema.HIGHLANDER, val)+"'),");
		}
		if(!values.isEmpty()) {
			DB.update(Schema.HIGHLANDER, "INSERT INTO `users_data` (`username`, `type`, `analysis`, `key`, `value`) VALUES" + sb.deleteCharAt(sb.length()-1).toString());
		}
	}

	public Set<Field> getExistingListOfValuesFields() {
		Set<Field> set = new TreeSet<>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT DISTINCT(`analysis`) FROM `users_data` " +
				"WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' AND `type` = '"+DB.format(Schema.HIGHLANDER, UserData.VALUES.toString())+"'")){
			while (res.next()){
				String fieldName = res.getString(1);
				set.add(Field.getField(fieldName));
			}
		}catch(Exception ex2) {
			ex2.printStackTrace();
		}
		return set;
	}
	
	public Set<Reference> getExistingListOfIntervalsReferences() {
		Set<Reference> set = new TreeSet<>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT DISTINCT(`analysis`) FROM `users_data` " +
				"WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' AND `type` = '"+DB.format(Schema.HIGHLANDER, UserData.INTERVALS.toString())+"'")){
			while (res.next()){
				String referenceName = res.getString(1);
				try {
					set.add(ProfileTree.getReference(referenceName));
				}catch(Exception ex1) {
					ex1.printStackTrace();
				}
			}
		}catch(Exception ex2) {
			ex2.printStackTrace();
		}
		return set;
	}
	
	public Set<Reference> getExistingListOfPhenotypesReferences() {
		Set<Reference> set = new TreeSet<>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT DISTINCT(`analysis`) FROM `users_data` " +
				"WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' AND `type` = '"+DB.format(Schema.HIGHLANDER, UserData.PHENOTYPES.toString())+"'")){
			while (res.next()){
				String referenceName = res.getString(1);
				try {
					set.add(ProfileTree.getReference(referenceName));
				}catch(Exception ex1) {
					ex1.printStackTrace();
				}
			}
		}catch(Exception ex2) {
			ex2.printStackTrace();
		}
		return set;
	}
	
	public Map<String, List<String>> getPersonalData(UserData userData) throws Exception {
		Map<String, List<String>> map = new TreeMap<String, List<String>>(new Tools.NaturalOrderComparator(true));
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `key`, `value` FROM `users_data` " +
				"WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' AND `type` = '"+DB.format(Schema.HIGHLANDER, userData.toString())+"'")){
			while (res.next()){
				String listName = res.getString("key");
				String val = res.getString("value");
				if (!map.containsKey(listName)){
					map.put(listName, new ArrayList<String>());
				}
				List<String> list = map.get(listName);
				list.add(val);
			}
		}
		return map;		
	}

	public boolean doesPersonalDataExists(UserData userData, String category, String name) throws Exception {
		boolean exist = false;
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT COUNT(`key`) FROM `users_data` " +
				"WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' AND `type` = '"+DB.format(Schema.HIGHLANDER, userData.toString())+"' AND `key` = '"+DB.format(Schema.HIGHLANDER, name)+"'" +
				((category != null)?" AND `analysis` = '"+DB.format(Schema.HIGHLANDER, category)+"'":""))){
			if (res.next()){
				exist = res.getInt(1) > 0;
			}
		}
		return exist;
	}

	public Map<String, List<String>> getPersonalData(UserData userData, String category) throws Exception {
		Map<String, List<String>> map = new TreeMap<String, List<String>>(new Tools.NaturalOrderComparator(true));
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `key`, `value` FROM `users_data` " +
				"WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' AND `type` = '"+DB.format(Schema.HIGHLANDER, userData.toString())+"' AND `analysis` = '"+DB.format(Schema.HIGHLANDER, category)+"'")){
			while (res.next()){
				String listName = res.getString("key");
				String val = res.getString("value");
				if (!map.containsKey(listName)){
					map.put(listName, new ArrayList<String>());
				}
				List<String> list = map.get(listName);
				list.add(val);
			}
		}
		return map;		
	}

	public void saveValues(String listName, Field field, List<String> values) throws Exception {
		deleteData(UserData.VALUES, field.getName(), listName);
		StringBuilder sb = new StringBuilder();
		for (String val : values){
			sb.append(" (" +
					"'"+DB.format(Schema.HIGHLANDER, username)+"'," +
					"'"+DB.format(Schema.HIGHLANDER, UserData.VALUES.toString())+"'," +
					"'"+DB.format(Schema.HIGHLANDER, field.getName())+"'," +
					"'"+DB.format(Schema.HIGHLANDER, listName)+"'," +
					"'"+DB.format(Schema.HIGHLANDER, val)+"'),");
		}
		if(!values.isEmpty()) {
			DB.update(Schema.HIGHLANDER, "INSERT INTO `users_data` (`username`, `type`, `analysis`, `key`, `value`) VALUES" + sb.deleteCharAt(sb.length()-1).toString());
			updateHistory(UserData.VALUES, field.getName(), listName);
		}
	}

	public void saveValues(String listName, Field field, Map<String,String> valuesWithComments) throws Exception {
		List<String> values = new ArrayList<>();
		for (String value : valuesWithComments.keySet()) {
			if (valuesWithComments.get(value) != null && valuesWithComments.get(value).length() > 0) {
				values.add(value.replace('|', '/')+"|"+valuesWithComments.get(value).replace('|', '/'));
			}else {
				values.add(value);
			}
		}
		saveValues(listName, field, values);
	}
	
	public List<String> loadValues(Field field, String listName) throws Exception {
		List<String> list = new ArrayList<String>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `value` FROM `users_data` " +
				"WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' "
				+ "AND `type` = '"+DB.format(Schema.HIGHLANDER, UserData.VALUES.toString())+"' "
				+ "AND `analysis` = '"+DB.format(Schema.HIGHLANDER, field.getName())+"' "
				+ "AND `key` = '"+DB.format(Schema.HIGHLANDER, listName)+"'")){
			while (res.next()){
				list.add(res.getString("value").split("\\|")[0]);
			}
			Collections.sort(list, new Tools.NaturalOrderComparator(true));
		}
		return list;
	}

	public Map<String, String> loadValuesWithComments(Field field, String listName) throws Exception {
		Map<String, String> map = new TreeMap<>(new Tools.NaturalOrderComparator(true));
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `value` FROM `users_data` " +
				"WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' "
				+ "AND `type` = '"+DB.format(Schema.HIGHLANDER, UserData.VALUES.toString())+"' "
				+ "AND `analysis` = '"+DB.format(Schema.HIGHLANDER, field.getName())+"' "
				+ "AND `key` = '"+DB.format(Schema.HIGHLANDER, listName)+"'")){
			while (res.next()){
				String[] split = res.getString("value").split("\\|");
				map.put(split[0], (split.length == 2 ? split[1] : ""));
			}
		}
		return map;
	}
	
	public void saveIntervals(String listName, Reference reference, List<Interval> values) throws Exception {
		deleteData(UserData.INTERVALS, reference.getName(), listName);
		StringBuilder sb = new StringBuilder();
		for (Interval val : values){
			sb.append(" (" +
					"'"+DB.format(Schema.HIGHLANDER, username)+"'," +
					"'"+DB.format(Schema.HIGHLANDER, UserData.INTERVALS.toString())+"'," +
					"'"+DB.format(Schema.HIGHLANDER, reference.getName())+"'," +
					"'"+DB.format(Schema.HIGHLANDER, listName)+"'," +
					"'"+DB.format(Schema.HIGHLANDER, val.toString())+"'),");
		}
		if(!values.isEmpty()) {
			DB.update(Schema.HIGHLANDER, "INSERT INTO `users_data` (`username`, `type`, `analysis`, `key`, `value`) VALUES" + sb.deleteCharAt(sb.length()-1).toString());
			updateHistory(UserData.INTERVALS, reference.getName(), listName);
		}
	}

	public void savePhenotypes(String listName, Reference reference, List<HPOTerm> values) throws Exception {
		deleteData(UserData.PHENOTYPES, reference.getName(), listName);
		StringBuilder sb = new StringBuilder();
		for (HPOTerm val : values){
			sb.append(" (" +
					"'"+DB.format(Schema.HIGHLANDER, username)+"'," +
					"'"+DB.format(Schema.HIGHLANDER, UserData.PHENOTYPES.toString())+"'," +
					"'"+DB.format(Schema.HIGHLANDER, reference.getName())+"'," +
					"'"+DB.format(Schema.HIGHLANDER, listName)+"'," +
					"'"+DB.format(Schema.HIGHLANDER, val.getOntologyId())+"'),");
		}
		if(!values.isEmpty()) {
			DB.update(Schema.HIGHLANDER, "INSERT INTO `users_data` (`username`, `type`, `analysis`, `key`, `value`) VALUES" + sb.deleteCharAt(sb.length()-1).toString());
			updateHistory(UserData.PHENOTYPES, reference.getName(), listName);
		}
	}
	
	public List<Interval> loadIntervals(String listName) throws Exception {
		List<Interval> list = new ArrayList<Interval>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `value`, `analysis` FROM `users_data` " +
				"WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' " +
				"AND `type` = '"+DB.format(Schema.HIGHLANDER, UserData.INTERVALS.toString())+"' " +
				"AND `key` = '"+DB.format(Schema.HIGHLANDER, listName)+"'")){
			while (res.next()){
				list.add(new Interval(Reference.getReference(res.getString("analysis")), res.getString("value")));
			}
		}
		Collections.sort(list);
		return list;
	}
	
	public List<HPOTerm> loadPhenotypes(String listName) throws Exception {
		List<HPOTerm> list = new ArrayList<HPOTerm>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `value`, `analysis` FROM `users_data` " +
				"WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' " +
				"AND `type` = '"+DB.format(Schema.HIGHLANDER, UserData.PHENOTYPES.toString())+"' " +
				"AND `key` = '"+DB.format(Schema.HIGHLANDER, listName)+"'")){
			while (res.next()){
				list.add(new HPOTerm(res.getString("value"), Reference.getReference(res.getString("analysis"))));
			}
		}
		Collections.sort(list);
		return list;
	}
	
	public List<Interval> loadIntervals(Reference reference, String listName) throws Exception {
		List<Interval> list = new ArrayList<Interval>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `value`, `analysis` FROM `users_data` " +
				"WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' " +
				"AND `type` = '"+DB.format(Schema.HIGHLANDER, UserData.INTERVALS.toString())+"' " +
				"AND `analysis` = '"+DB.format(Schema.HIGHLANDER, reference.getName())+"' " +
				"AND `key` = '"+DB.format(Schema.HIGHLANDER, listName)+"'")){
			while (res.next()){
				list.add(new Interval(reference, res.getString("value")));
			}
		}
		Collections.sort(list);
		return list;
	}

	public List<HPOTerm> loadPhenotypes(Reference reference, String listName) throws Exception {
		List<HPOTerm> list = new ArrayList<HPOTerm>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `value`, `analysis` FROM `users_data` " +
				"WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' " +
				"AND `type` = '"+DB.format(Schema.HIGHLANDER, UserData.PHENOTYPES.toString())+"' " +
				"AND `analysis` = '"+DB.format(Schema.HIGHLANDER, reference.getName())+"' " +
				"AND `key` = '"+DB.format(Schema.HIGHLANDER, listName)+"'")){
			while (res.next()){
				list.add(new HPOTerm(res.getString("value"), reference));
			}
		}
		Collections.sort(list);
		return list;
	}
	
	public void saveColumnSelection(String selectionName, Analysis analysis, List<Field> headers) throws Exception {
		deleteData(UserData.COLUMN_SELECTION, analysis.toString(), selectionName);
		StringBuilder sb = new StringBuilder();
		for (Field field : headers){
			sb.append(" (" +
					"'"+DB.format(Schema.HIGHLANDER, username)+"'," +
					"'"+DB.format(Schema.HIGHLANDER, UserData.COLUMN_SELECTION.toString())+"'," +
					"'"+DB.format(Schema.HIGHLANDER, analysis.toString())+"'," +
					"'"+DB.format(Schema.HIGHLANDER, selectionName)+"'," +
					"'"+DB.format(Schema.HIGHLANDER, field.getName())+"'),");
		}
		if(!headers.isEmpty()) {
			DB.update(Schema.HIGHLANDER, "INSERT INTO `users_data` (`username`, `type`, `analysis`, `key`, `value`) VALUES" + sb.deleteCharAt(sb.length()-1).toString());
			updateHistory(UserData.COLUMN_SELECTION, analysis.toString(), selectionName);
		}
	}

	public List<Field> loadColumnSelection(Analysis analysis, String selectionName) throws Exception {
		List<Field> list = new ArrayList<Field>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `value` FROM `users_data` " +
				"WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' AND `type` = '"+DB.format(Schema.HIGHLANDER, UserData.COLUMN_SELECTION.toString())+"'" +
				" AND `analysis` = '"+DB.format(Schema.HIGHLANDER, analysis.toString())+"' AND `key` = '"+DB.format(Schema.HIGHLANDER, selectionName)+"' ORDER BY id")){
			while (res.next()){
				String columnName = res.getString("value");
				if (Field.exists(columnName)) {
					Field field = Field.getField(columnName);
					if (field.hasAnalysis(analysis)) {
						list.add(field);
					}
				}
			}
		}
		return list;
	}

	public void saveColumnMask(String maskName, Analysis analysis, List<Field> headers) throws Exception {
		deleteData(UserData.COLUMN_MASK, analysis.toString(), maskName);
		StringBuilder sb = new StringBuilder();
		for (Field field : headers){
			sb.append(" (" +
					"'"+DB.format(Schema.HIGHLANDER, username)+"'," +
					"'"+DB.format(Schema.HIGHLANDER, UserData.COLUMN_MASK.toString())+"'," +
					"'"+DB.format(Schema.HIGHLANDER, analysis.toString())+"'," +
					"'"+DB.format(Schema.HIGHLANDER, maskName)+"'," +
					"'"+DB.format(Schema.HIGHLANDER, field.getName())+"'),");
		}
		if(!headers.isEmpty()) {
			DB.update(Schema.HIGHLANDER, "INSERT INTO `users_data` (`username`, `type`, `analysis`, `key`, `value`) VALUES" + sb.deleteCharAt(sb.length()-1).toString());
			updateHistory(UserData.COLUMN_MASK, analysis.toString(), maskName);
		}
	}

	public List<Field> loadColumnMask(Analysis analysis, String maskName) throws Exception {
		List<Field> list = new ArrayList<Field>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `value` FROM `users_data` " +
				"WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' AND `type` = '"+DB.format(Schema.HIGHLANDER, UserData.COLUMN_MASK.toString())+"'" +
				" AND `analysis` = '"+DB.format(Schema.HIGHLANDER, analysis.toString())+"' AND `key` = '"+DB.format(Schema.HIGHLANDER, maskName)+"' ORDER BY id")){
			while (res.next()){
				String columnName = res.getString("value");
				if (Field.exists(columnName)) {
					Field field = Field.getField(columnName);
					if (field.hasAnalysis(analysis)) {
						list.add(field);
					}
				}
			}
		}
		return list;
	}

	public boolean saveFilter(String filterName, Analysis analysis, ComboFilter filter, boolean checkAnalysisCompatibility) throws Exception {
		String saveString = filter.getSaveString();
		if (checkAnalysisCompatibility){
			if (!filter.checkFieldCompatibility(analysis)) return false;
			saveString.replaceAll("found_in_"+analysis, "found_in_"+Highlander.getCurrentAnalysis());
		}
		return saveFilter(filterName, analysis, saveString);
	}

	public boolean saveFilter(String filterName, Analysis analysis, String filterSaveString) throws Exception {
		deleteData(UserData.FILTER, analysis.toString(), filterName);		
		DB.update(Schema.HIGHLANDER, "INSERT INTO `users_data` (`username`, `type`, `analysis`, `key`, `value`) VALUES (" +
				"'"+DB.format(Schema.HIGHLANDER, username)+"'," +
				"'"+DB.format(Schema.HIGHLANDER, UserData.FILTER.toString())+"'," +
				"'"+DB.format(Schema.HIGHLANDER, analysis.toString())+"'," +
				"'"+DB.format(Schema.HIGHLANDER, filterName)+"'," +
				"'"+DB.format(Schema.HIGHLANDER, filterSaveString)+"')");
		updateHistory(UserData.FILTER, analysis.toString(), filterName);
		return true;
	}

	public boolean compareFilter(String filterName, Analysis analysis, ComboFilter filter) throws Exception {
		String newFilterSaveString = filter.getSaveString(); 
		String oldFilterSaveString = null;
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `value` FROM `users_data` " +
				"WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' AND `type` = '"+DB.format(Schema.HIGHLANDER, UserData.FILTER.toString())+"' AND " +
				"`analysis` = '"+DB.format(Schema.HIGHLANDER, analysis.toString())+"' AND `key` = '"+DB.format(Schema.HIGHLANDER, filterName)+"'")){
			if (res.next()){
				oldFilterSaveString = res.getString("value");
			}
		}
		if (oldFilterSaveString == null) return false;
		return oldFilterSaveString.equals(newFilterSaveString);
	}

	public ComboFilter loadFilter(FilteringPanel filteringPanel, Analysis analysis, String filterName) throws Exception {
		String value = null;
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `value` FROM `users_data` " +
				"WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' AND `type` = '"+DB.format(Schema.HIGHLANDER, UserData.FILTER.toString())+"' AND " +
				"`analysis` = '"+DB.format(Schema.HIGHLANDER, analysis.toString())+"' AND `key` = '"+DB.format(Schema.HIGHLANDER, filterName)+"'")){
			if (res.next()){
				value = res.getString("value");
			}
		}
		if (value != null){
			return (ComboFilter)new ComboFilter().loadCriterion(filteringPanel, value);
		}else{
			return new ComboFilter();
		}
	}

	public void saveSorting(String sortingName, Analysis analysis, List<SortingCriterion> criteria) throws Exception {
		deleteData(UserData.SORTING, analysis.toString(), sortingName);
		StringBuilder sb = new StringBuilder();
		for (SortingCriterion crit : criteria){
			String critString = crit.getFieldName() + "|" + crit.getSortOrder();
			sb.append(" (" +
					"'"+DB.format(Schema.HIGHLANDER, username)+"'," +
					"'"+DB.format(Schema.HIGHLANDER, UserData.SORTING.toString())+"'," +
					"'"+DB.format(Schema.HIGHLANDER, analysis.toString())+"'," +
					"'"+DB.format(Schema.HIGHLANDER, sortingName)+"'," +
					"'"+DB.format(Schema.HIGHLANDER, critString)+"'),");
		}
		if(!criteria.isEmpty()) {
			DB.update(Schema.HIGHLANDER, "INSERT INTO `users_data` (`username`, `type`, `analysis`, `key`, `value`) VALUES" + sb.deleteCharAt(sb.length()-1).toString());
			updateHistory(UserData.SORTING, analysis.toString(), sortingName);
		}
	}

	public List<SortingCriterion> loadSorting(SortingPanel sortingPanel, Analysis analysis, String sortingName) throws Exception {
		List<SortingCriterion> list = new ArrayList<SortingCriterion>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `value` FROM `users_data` " +
				"WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' AND `type` = '"+DB.format(Schema.HIGHLANDER, UserData.SORTING.toString())+"' AND " +
				"`analysis` = '"+DB.format(Schema.HIGHLANDER, analysis.toString())+"' AND `key` = '"+DB.format(Schema.HIGHLANDER, sortingName)+"' ORDER BY id")){
			while (res.next()){
				String fieldName = res.getString("value").split("\\|")[0];
				SortOrder sortOrder = SortOrder.valueOf(res.getString("value").split("\\|")[1]);
				if (Field.exists(fieldName)) {
					Field field = Field.getField(fieldName);
					if (field.hasAnalysis(analysis)) {
						list.add(new SortingCriterion(sortingPanel, field, sortOrder));
					}
				}
			}
		}
		return list;
	}

	public void saveHighlighting(String highlightingName, Analysis analysis, List<HighlightingRule> criteria) throws Exception {
		deleteData(UserData.HIGHLIGHTING, analysis.toString(), highlightingName);
		StringBuilder sb = new StringBuilder();
		for (HighlightingRule crit : criteria){
			sb.append(" (" +
					"'"+DB.format(Schema.HIGHLANDER, username)+"'," +
					"'"+DB.format(Schema.HIGHLANDER, UserData.HIGHLIGHTING.toString())+"'," +
					"'"+DB.format(Schema.HIGHLANDER, analysis.toString())+"'," +
					"'"+DB.format(Schema.HIGHLANDER, highlightingName)+"'," +
					"'"+DB.format(Schema.HIGHLANDER, crit.getSaveString())+"'),");
		}
		if(!criteria.isEmpty()) {
			DB.update(Schema.HIGHLANDER, "INSERT INTO `users_data` (`username`, `type`, `analysis`, `key`, `value`) VALUES" + sb.deleteCharAt(sb.length()-1).toString());
			updateHistory(UserData.HIGHLIGHTING, analysis.toString(), highlightingName);
		}
	}

	public List<HighlightingRule> loadHighlighting(HighlightingPanel highlightingPanel, Analysis analysis, String highlightingName) throws Exception {
		List<HighlightingRule> list = new ArrayList<HighlightingRule>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `value` FROM `users_data` " +
				"WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' AND `type` = '"+DB.format(Schema.HIGHLANDER, UserData.HIGHLIGHTING.toString())+"' AND " +
				"`analysis` = '"+DB.format(Schema.HIGHLANDER, analysis.toString())+"' AND `key` = '"+DB.format(Schema.HIGHLANDER, highlightingName)+"' ORDER BY id")){
			while (res.next()){
				String saveString = res.getString("value");
				RuleType ruleType = RuleType.valueOf(saveString.split("\\|")[0]);
				switch (ruleType) {
				case HIGHLIGHTING:
					list.add(new HighlightCriterion().loadCriterion(highlightingPanel, saveString));				
					break;
				case HEATMAP:
					list.add(new HeatMapCriterion().loadCriterion(highlightingPanel, saveString));
					break;
				default:
					System.err.println("Unknown Highlighting rule type : " + ruleType);
					break;
				}
			}
		}
		return list;
	}

	public void saveVariantList(VariantsList list, String listName) throws Exception {
		deleteData(UserData.VARIANT_LIST, list.getAnalysis().toString(), listName);
		StringBuilder sb = new StringBuilder();
		for (String saveString : list.getSaveStrings()){
			sb.append(" (" +
					"'"+DB.format(Schema.HIGHLANDER, username)+"'," +
					"'"+DB.format(Schema.HIGHLANDER, UserData.VARIANT_LIST.toString())+"'," +
					"'"+DB.format(Schema.HIGHLANDER, list.getAnalysis().toString())+"'," +
					"'"+DB.format(Schema.HIGHLANDER, listName)+"'," +
					"'"+DB.format(Schema.HIGHLANDER, saveString)+"'),");
		}
		if(!list.getSaveStrings().isEmpty()) {
			DB.update(Schema.HIGHLANDER, "INSERT INTO `users_data` (`username`, `type`, `analysis`, `key`, `value`) VALUES" + sb.deleteCharAt(sb.length()-1).toString());
			updateHistory(UserData.VARIANT_LIST, list.getAnalysis().toString(), listName);
		}
	}

	public VariantsList loadVariantList(Analysis analysis, String listName, FilteringPanel filteringPanel, DatabasePanel databasePanel, NavigationPanel navigationPanel, 
			SortingPanel sortingPanel, HighlightingPanel highlightingPanel, VariantsTable variantsTable) throws Exception {
		VariantsList variantsList = new VariantsList(analysis, filteringPanel, databasePanel, navigationPanel, sortingPanel,	highlightingPanel, variantsTable, listName);
		List<String> saveStrings = new ArrayList<String>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `value` FROM `users_data` " +
				"WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' AND `type` = '"+DB.format(Schema.HIGHLANDER, UserData.VARIANT_LIST.toString())+"' AND " +
				"`analysis` = '"+DB.format(Schema.HIGHLANDER, analysis.toString())+"' AND `key` = '"+DB.format(Schema.HIGHLANDER, listName)+"'")){
			while (res.next()){
				saveStrings.add(res.getString("value"));
			}
		}
		for (String saveString : saveStrings){
			variantsList.parseSaveString(saveString);
		}
		return variantsList;
	}

	public void saveFiltersTemplate(FiltersTemplate template, String templateName) throws Exception {
		deleteData(UserData.FILTERS_TEMPLATE, template.getAnalysis().toString(), templateName);
		StringBuilder sb = new StringBuilder();
		for (String saveString : template.getSaveStrings()){
			sb.append(" (" +
					"'"+DB.format(Schema.HIGHLANDER, username)+"'," +
					"'"+DB.format(Schema.HIGHLANDER, UserData.FILTERS_TEMPLATE.toString())+"'," +
					"'"+DB.format(Schema.HIGHLANDER, template.getAnalysis().toString())+"'," +
					"'"+DB.format(Schema.HIGHLANDER, templateName)+"'," +
					"'"+DB.format(Schema.HIGHLANDER, saveString)+"'),");
		}
		if(!template.getSaveStrings().isEmpty()) {
			DB.update(Schema.HIGHLANDER, "INSERT INTO `users_data` (`username`, `type`, `analysis`, `key`, `value`) VALUES" + sb.deleteCharAt(sb.length()-1).toString());
			updateHistory(UserData.FILTERS_TEMPLATE, template.getAnalysis().toString(), templateName);
		}
	}

	public FiltersTemplate loadFiltersTemplate(Analysis analysis, String templateName) throws Exception {
		FiltersTemplate template = new FiltersTemplate(analysis, templateName);
		List<String> saveStrings = new ArrayList<String>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `value` FROM `users_data` " +
				"WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' AND `type` = '"+DB.format(Schema.HIGHLANDER, UserData.FILTERS_TEMPLATE.toString())+"' AND " +
				"`analysis` = '"+DB.format(Schema.HIGHLANDER, analysis.toString())+"' AND `key` = '"+DB.format(Schema.HIGHLANDER, templateName)+"'")){
			while (res.next()){
				saveStrings.add(res.getString("value"));
			}
		}
		for (String saveString : saveStrings){
			template.parseSaveString(saveString);
		}
		return template;
	}

	public void updateHistory(UserData userData, String category, String elementName) throws Exception {
		updateHistory(userData, category, elementName, first_name + " " + last_name);
	}
	
	public void updateHistory(UserData userData, String category, String elementName, String userOfLastModification) throws Exception {
		DateFormat df = new SimpleDateFormat("dd-MM-yyyy-MM HH:mm:ss");
		String fullElementName = userData + "|" + elementName;
		if (category == null) category = "";
		deleteData(UserData.HISTORY, category, fullElementName);
		DB.update(Schema.HIGHLANDER, "INSERT INTO `users_data` (`username`, `type`, `analysis`, `key`, `value`) VALUES (" +
				"'"+DB.format(Schema.HIGHLANDER, username)+"'," +
				"'"+DB.format(Schema.HIGHLANDER, UserData.HISTORY.toString())+"'," +
				"'"+DB.format(Schema.HIGHLANDER, category)+"'," +
				"'"+DB.format(Schema.HIGHLANDER, fullElementName)+"'," +
				"'"+DB.format(Schema.HIGHLANDER, "Last modification by " + userOfLastModification + " on " + df.format(System.currentTimeMillis()))+"')");
	}
	
	public String getHistory(UserData userData, String category, String elementName) throws Exception {
		String fullElementName = userData + "|" + elementName;
		String value = "";
		if (category == null) category = "";
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `value` FROM `users_data` " +
				"WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' AND `type` = '"+DB.format(Schema.HIGHLANDER, UserData.HISTORY.toString())+
				"' AND `analysis` = '"+DB.format(Schema.HIGHLANDER, category)+"' AND `key` = '"+DB.format(Schema.HIGHLANDER, fullElementName)+"'")){
			if (res.next()){
				value = res.getString("value");
			}
		}
		return value;
	}
	
	public void checkForNewSharedElement(Window parentComponent) {
		try{
			Map<String, UserData> types = new HashMap<String, User.UserData>();
			Map<String, Set<String>> categories = new HashMap<String, Set<String>>();
			Map<String, String> keys = new TreeMap<String, String>();
			try (Results res = DB.select(Schema.HIGHLANDER, "SELECT * FROM `users_data` WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' AND INSTR(`key`,'SHARE|') = 1")) {
				while (res.next()){
					UserData type = UserData.valueOf(res.getString("type"));
					String key = res.getString("key");
					String mapKey = type + "-" + key;
					if (!categories.containsKey(mapKey)){
						categories.put(mapKey, new TreeSet<String>());
					}
					types.put(mapKey, type);
					keys.put(mapKey, key);
					if (res.getObject("analysis") == null){
						categories.get(mapKey).add("NULL");						
					}else{
						categories.get(mapKey).add(res.getString("analysis"));						
					}
				}
			}
			for (String mapKey : keys.keySet()){
				for (String category : categories.get(mapKey)){
					try{
						if (category.equals("NULL")) category = null;
						String fullListName = keys.get(mapKey);
						String sender = fullListName.substring(fullListName.indexOf("|")+1, fullListName.indexOf("|", fullListName.indexOf("|")+1));
						String listName = fullListName.substring(fullListName.indexOf("|", fullListName.indexOf("|")+1)+1);
						UserData type = types.get(mapKey);
						try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `first_name`, `last_name` FROM `users` WHERE `username` = '"+DB.format(Schema.HIGHLANDER, sender)+"'")) {
							if (res.next()) sender = res.getString(1) + " " + res.getString(2);
						}
						if (type == UserData.FOLDER){
							UserData subtype = UserData.FOLDER;
							try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `value` FROM `users_data` WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' AND `type` = '" + DB.format(Schema.HIGHLANDER, type.toString()) + "' AND `key` = '" + DB.format(Schema.HIGHLANDER, fullListName) + "'")) {
								if (res.next()) subtype = UserData.valueOf(res.getString(1));
							}
							JOptionPane.showMessageDialog(parentComponent, "You have received a new shared folder called '"+listName+"' from " + sender + ".\n "
									+ "It has been saved in your profile in '"+subtype.getName() + ((category == null)?"":" > " + category) + "'.", "Shared folder received", JOptionPane.INFORMATION_MESSAGE, type.getSharingIcon());
							String query = "UPDATE `users_data` SET `key` = '"+listName+"' WHERE `username` = '"+DB.format(Schema.HIGHLANDER, username)+"' AND `type` = '" + DB.format(Schema.HIGHLANDER, type.toString()) + "' AND `key` = '" + DB.format(Schema.HIGHLANDER, fullListName) + "'";
							if (category != null) query += " AND `analysis` = '" + DB.format(Schema.HIGHLANDER, category) + "'";
							DB.update(Schema.HIGHLANDER, query);
						}else{
							JPanel panel = new JPanel(new GridBagLayout());
							JLabel first = new JLabel("You have received new " + type.getName() + ((category == null)?"":" in " + category) +" called '"+listName+"' from " + sender);
							JLabel second = new JLabel("Do you want to save it in your profile ?");				
							Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
							panel.setPreferredSize(new Dimension(screenSize.width*2/3, screenSize.height/2));
							panel.add(first, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
							JScrollPane scroll = new JScrollPane(ProfileTree.getDescriptionElement(type, category, fullListName, listName));
							panel.add(scroll,	new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
							panel.add(second, new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
							int answ = JOptionPane.showConfirmDialog(parentComponent, panel, "Shared element received", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, type.getSharingIcon());
							boolean toDelete = true;
							if (answ == JOptionPane.YES_OPTION){
								String savePath = ProfileTree.showProfileDialog(parentComponent, null, Action.SAVE, type, category, "Where do you want to save it ?", listName);
								if (savePath != null){
									toDelete = false;
									if (type != UserData.FOLDER && Highlander.getLoggedUser().doesPersonalDataExists(type, category, savePath)){
										int yesno = JOptionPane.showConfirmDialog(new JFrame(), 
												"You already have a "+type.getName()+" named '"+savePath.replace("~", " -> ")+"', do you want to overwrite it ?\nIf no, shared element will be deleted.", 
												"Overwriting "+type.getName()+" in your profile", JOptionPane.YES_NO_OPTION , JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iDbSave,64));
										if (yesno == JOptionPane.YES_OPTION){
											if (category == null){
												deleteData(type, savePath);
											}else{
												deleteData(type, category, savePath);
											}
										}else{
											toDelete = true;										
										}
									}
									if (!toDelete){
										String query = "UPDATE `users_data` SET `key` = '"+savePath+"' WHERE username = '"+DB.format(Schema.HIGHLANDER, username)+"' AND `type` = '" + DB.format(Schema.HIGHLANDER, type.toString()) + "' AND `key` = '" + DB.format(Schema.HIGHLANDER, fullListName) + "'";
										if (category != null) query += " AND `analysis` = '" + DB.format(Schema.HIGHLANDER, category) + "'";
										DB.update(Schema.HIGHLANDER, query);
										updateHistory(UserData.valueOf(type.toString()), category, savePath, sender);
									}
								}
							}
							if (toDelete){
								if (category == null){
									deleteData(type, fullListName);
								}else{
									deleteData(type, category, fullListName);
								}
							}							
						}						
					}catch (Exception ex){
						Tools.exception(ex);
						JOptionPane.showMessageDialog(new JFrame(), Tools.getMessage("Error : ", ex), "Shared element received", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}			
				}
			}
		}catch (Exception ex){
			Tools.exception(ex);
		}
	}

	public void validate() throws Exception {
		if (username.length() < 4)
			throw new Exception("Username must be at least 4 character long.");
		if (username.length() > 30)
			throw new Exception("Username must be at most 30 character long.");
		if (first_name.length() == 0)
			throw new Exception("First name is mandatory.");
		if (first_name.length() > 45)
			throw new Exception("First name must be at most 45 character long.");
		if (last_name.length() == 0)
			throw new Exception("Last name is mandatory.");
		if (group.length() > 255)
			throw new Exception("Group must be at most 255 character long.");
		if (email.length() == 0 || email.indexOf("@") == -1)
			throw new Exception("Email address is mandatory and must contains '@'.");
		if (email.length() > 45)
			throw new Exception("Email must be at most 45 character long.");
		if (password.length() == 0)
			throw new Exception("Password is mandatory and must be at least 4 character long.");
		if (rights != Rights.administrator && !isAnotherUserAdmin())
			throw new Exception("At least one user must have the administrative rights.");
	}

	public void update() throws Exception {
		validate();
		DB.update(Schema.HIGHLANDER, "UPDATE `users` SET " +
				"`password` = '"+getPassword()+"', " +
				"`first_name` = '"+DB.format(Schema.HIGHLANDER, getFirstName())+"', " +
				"`last_name` = '"+DB.format(Schema.HIGHLANDER, getLastName())+"', " +
				"`email` = '"+DB.format(Schema.HIGHLANDER, getEmail())+"', " +
				"`group` = '"+DB.format(Schema.HIGHLANDER, getGroup())+"', " +
				"`rights` = '"+DB.format(Schema.HIGHLANDER, getRights().toString())+"' " +
				"WHERE `username` = '"+DB.format(Schema.HIGHLANDER, getUsername())+"'");
	}

	public void insert() throws Exception {
		validate();
		if (doesUserExist(username)) throw new Exception("Username '"+username+"' already exists.");  	
		DB.update(Schema.HIGHLANDER, "INSERT INTO `users` (`username`, `password`, `first_name`, `last_name`, `email`, `group`, `rights`) " +
				"VALUES (" +
				"'"+DB.format(Schema.HIGHLANDER, getUsername())+"', " +
				"'"+DB.format(Schema.HIGHLANDER, getPassword())+"', " +
				"'"+DB.format(Schema.HIGHLANDER, getFirstName())+"', " +
				"'"+DB.format(Schema.HIGHLANDER, getLastName())+"', " +
				"'"+DB.format(Schema.HIGHLANDER, getEmail())+"', " +
				"'"+DB.format(Schema.HIGHLANDER, getGroup())+"', " +
				"'"+DB.format(Schema.HIGHLANDER, getRights().toString())+"')");
	}

	public void delete() throws Exception {
		DB.update(Schema.HIGHLANDER, "DELETE FROM `users` WHERE `username` = '"+DB.format(Schema.HIGHLANDER, getUsername())+"'");
		DB.update(Schema.HIGHLANDER, "DELETE FROM `users_data` WHERE `username` = '"+DB.format(Schema.HIGHLANDER, getUsername())+"'");
		DB.update(Schema.HIGHLANDER, "DELETE FROM `projects_users` WHERE `username` = '"+DB.format(Schema.HIGHLANDER, getUsername())+"'");
	}

	public String resetPassword() throws Exception {
		password = User.generatePassword(8);
		String encryptedPassword = Tools.md5Encryption(password);
		DB.update(Schema.HIGHLANDER, "UPDATE `users` SET `password` = '"+DB.format(Schema.HIGHLANDER, encryptedPassword)+"' WHERE `username` = '"+DB.format(Schema.HIGHLANDER, getUsername())+"'");
		return password;
	}

	public void promote() throws Exception {
		rights = Rights.administrator;
		DB.update(Schema.HIGHLANDER, "UPDATE `users` SET `rights` = '"+DB.format(Schema.HIGHLANDER, getRights().toString())+"' WHERE `username` = '"+DB.format(Schema.HIGHLANDER, getUsername())+"'");
	}

	public static List<User> fetchList() throws Exception {
		List<User> users = new ArrayList<User>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT * FROM `users` ORDER BY `first_name`")) {
			while (res.next()){
				User user = new User(res.getString("username"));
				user.setPassword(res.getString("password"));
				user.setFirstName(res.getString("first_name"));
				user.setLastName(res.getString("last_name"));
				user.setEmail(res.getString("email"));
				user.setGroup(res.getString("group"));
				user.setRights(Rights.valueOf(res.getString("rights")));
				users.add(user);
			}
		}
		return users;  	
	}

	public static boolean doesUserExist(String username) throws Exception {
		boolean exist = false;
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT COUNT(*) FROM `users` WHERE `username` = '"+username+"'")) {
			if (res.next()){
				exist = (res.getInt(1) > 0);
			}
		}
		return exist;
	}

	public static void editUserProfile(){
		UserDataDialog dialog = new UserDataDialog(new JFrame(), UserDataDialogType.EDIT_USER);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension windowSize = dialog.getSize();
		dialog.setLocation(Math.max(0, (screenSize.width - windowSize.width) / 2),
				Math.max(0, (screenSize.height - windowSize.height) / 2));
		dialog.setVisible(true);
	}

	public static User createUser(){
		UserDataDialog dialog = new UserDataDialog(new JFrame(), UserDataDialogType.CREATE_USER);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension windowSize = dialog.getSize();
		dialog.setLocation(Math.max(0, (screenSize.width - windowSize.width) / 2),
				Math.max(0, (screenSize.height - windowSize.height) / 2));
		dialog.setVisible(true);
		return dialog.getUser();
	}

	public static Optional<User> deleteUser(Component parentComponent){
		try{
			User[] users = User.fetchList().toArray((new User[0]));
			User user = (User)JOptionPane.showInputDialog(parentComponent, "Select the user you want to permanently delete: ", "Delete user", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iUserDelete, 64), users, null);
			if (user != null){
				if (user.isAnotherUserAdmin()){
					int answer = JOptionPane.showOptionDialog(parentComponent, "Are you SURE you want to delete this user:\n"+user+" ("+user.getUsername()+")", "Delete user", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iUserDelete, 64), null, null);
					if (answer == JOptionPane.YES_OPTION){
						try {
							user.delete();
							return Optional.of(user);
						} catch (Exception ex) {
							Tools.exception(ex);
							JOptionPane.showMessageDialog(parentComponent, "Cannot delete '"+user+"': " + ex.getMessage(), "Delete user", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
							return Optional.empty();
						}
					}
				}else{
					JOptionPane.showMessageDialog(parentComponent, "Error: at least one user must have the administrative rights.", "Delete user",	JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					return Optional.empty();
				}
			}
			return Optional.empty();
		}catch (Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(parentComponent, "Cannot fetch the user list: " + ex.getMessage(), "Delete user", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			return Optional.empty();
		}
	}

	public static void promoteUser(Component parentComponent){
		try{
			User[] users = User.fetchList().toArray((new User[0]));
			User user = (User)JOptionPane.showInputDialog(parentComponent, "Select the user you want to promote to administrator: ", "Promote user", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iUserPromote, 64), users, null);
			if (user != null){
				int answer = JOptionPane.showOptionDialog(parentComponent, "Are you SURE you want to promote this user:\n"+user+" ("+user.getUsername()+")", "Promote user", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iUserPromote, 64), null, null);
				if (answer == JOptionPane.YES_OPTION){
					try {
						user.promote();
					} catch (Exception ex) {
						Tools.exception(ex);
						JOptionPane.showMessageDialog(parentComponent, "Cannot promote '"+user+"': " + ex.getMessage(), "Promote user", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			}
		}catch (Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(parentComponent, "Cannot fetch the user list: " + ex.getMessage(), "Promote user", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
	}

	public static String generatePassword(int length) {
		if (length < 5) length = 5;
		char[] SYMBOLS = (new String("$*.[]{}()?-!@#%&/\\,><:;_~")).toCharArray();
		char[] LOWERCASE = (new String("abcdefghijklmnopqrstuvwxyz")).toCharArray();
		char[] UPPERCASE = (new String("ABCDEFGHIJKLMNOPQRSTUVWXYZ")).toCharArray();
		char[] NUMBERS = (new String("0123456789")).toCharArray();
		char[] ALL_CHARS = (new String("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789$*.[]{}()?-!@#%&/\\,><:;_~")).toCharArray();
		Random rand = new SecureRandom();
		char[] password = new char[length];
		//get the requirements out of the way
		password[0] = LOWERCASE[rand.nextInt(LOWERCASE.length)];
		password[1] = UPPERCASE[rand.nextInt(UPPERCASE.length)];
		password[2] = NUMBERS[rand.nextInt(NUMBERS.length)];
		password[3] = SYMBOLS[rand.nextInt(SYMBOLS.length)];
		//populate rest of the password with random chars
		for (int i = 4; i < length; i++) {
			password[i] = ALL_CHARS[rand.nextInt(ALL_CHARS.length)];
		}
		//shuffle it up
		for (int i = 0; i < password.length; i++) {
			int randomPosition = rand.nextInt(password.length);
			char temp = password[i];
			password[i] = password[randomPosition];
			password[randomPosition] = temp;
		}
		return new String(password);
	}

	public static void resetPassword(Component parentComponent){
		try{
			User[] users = User.fetchList().toArray((new User[0]));
			User user = (User)JOptionPane.showInputDialog(parentComponent, "Select the user for whom you want to reset password: ", "Reset password", JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iUserLock, 64), users, null);
			resetPassword(parentComponent, user);
		}catch (Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(), "Cannot fetch the user list: " + ex.getMessage(), "Reset password", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
	}
	
	public static void resetPassword(Component parentComponent, User user){
		try{
			if (user != null){
				int answer = JOptionPane.showOptionDialog(parentComponent, "Are you SURE you want to reset password for this user:\n"+user+" ("+user.getUsername()+")", "Reset password", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, Resources.getScaledIcon(Resources.iUserLock, 64), null, null);
				if (answer == JOptionPane.YES_OPTION){
					try {
						String password = user.resetPassword();
						StringBuilder sb = new StringBuilder();
						sb.append("Dear "+user.getFirstName() + " " + user.getLastName() + ",\n\n");
						sb.append("Your password for 'Highlander' has been reseted.\n");
						sb.append("Your login: "+user.getUsername() + "\n");
						sb.append("Your password: "+password + "\n");
						sb.append("You can change it in the profile tab within Highlander.\n");
						sb.append("\n");
						Tools.sendMail(user.getEmail(), "Password reset for Highlander", sb.toString());
					} catch (Exception ex) {
						Tools.exception(ex);
						JOptionPane.showMessageDialog(parentComponent, "Cannot reset password for '"+user+"': " + ex.getMessage(), "Reset password", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					}
				}
			}
		}catch (Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(parentComponent, "Cannot fetch the user list: " + ex.getMessage(), "Promote user", JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
		}
	}

}
