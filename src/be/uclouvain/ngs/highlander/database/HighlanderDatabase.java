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

package be.uclouvain.ngs.highlander.database;

import java.io.File;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Parameters;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.datatype.Reference;

/**
 * @author Raphaël Helaers
 *
 */
public class HighlanderDatabase {

	public enum DBMS {mysql,hsqldb}
	public enum Schema {HIGHLANDER, ENSEMBL, DBNSFP, GNOMAD_WES, GNOMAD_WGS, COSMIC, GONL, EXAC}
	private final Map<String,List<String>> availableTables = new TreeMap<String, List<String>>();

	private final Parameters parameters;
	private final HighlanderDataSource dataSourceMain;
	private final Map<String, Reference> references = new HashMap<>();

	private final Map<Statement,String> runningSelects = new LinkedHashMap<>();

	public HighlanderDatabase(Parameters parameters) {
		this(parameters, 5);
	}

	public HighlanderDatabase(Parameters parameters, int maxPoolSize) {
		this.parameters = parameters;
		dataSourceMain = new HighlanderDataSource(parameters, maxPoolSize);
		Set<String> availableReferenceNames = new HashSet<>();
		try (Results res = select(Schema.HIGHLANDER, "SELECT DISTINCT(reference) FROM `references`")) {
			while (res.next()) {
				availableReferenceNames.add(res.getString("reference"));
			}
		}catch(Exception ex) {
			System.err.println("Cannot fetch annotation schemas names");
			ex.printStackTrace();
		}
		for (String name : availableReferenceNames) {
			try (Results res = select(Schema.HIGHLANDER, "SELECT * FROM `references` WHERE reference = '"+name+"'")) {
				references.put(name, new Reference(res));
			}catch(Exception ex) {
				System.err.println("Cannot fetch annotation schemas for reference " + name);
				ex.printStackTrace();
			}
		}
	}

	public Set<Reference> getAvailableReferences(){
		return new TreeSet<>(references.values());
	}
	
	public void addReference(Reference reference) {
		references.put(reference.getName(), reference);
	}
	
	public void removeReference(Reference reference) {
		references.remove(reference.getName());
	}

	public boolean hasSchema(Reference reference, Schema schema){
		if (schema == Schema.HIGHLANDER) return true;
		if (references.containsKey(reference.getName())) {
			return references.get(reference.getName()).hasSchema(schema);  		
		}
		return false;
	}

	public HighlanderDataSource getDataSource(Schema schema){
		return dataSourceMain;  		
	}  

	public DBMS getDBMS(Schema schema) {
		return getDataSource(schema).getDBMS();
	}

	public String getSchemaName(Reference reference, Schema schema) throws Exception {
		if (hasSchema(reference, schema)) {
			if (schema == Schema.HIGHLANDER) return parameters.getSchemaHighlander();
			else return references.get(reference.getName()).getSchemaName(schema);		
		}
		throw new Exception("Highlander database has no schema '" + schema + "' for reference '" + reference + "'");
	}

	/**
	 * Note for prepared statements : you need to open the connection separately from the preparedStatement.
	 * <p>
	 * So don't do
	 * <p> 
	 * <code>try (PreparedStatement pstmt = DB.getConnection().prepareStatement("...")) { ... }</code>
	 * <p>
	 * But
	 * <p>
	 * <code>try (Connection con = DB.getConnection() ; PreparedStatement pstmt = con.prepareStatement("...")) { ... }</code>
	 * 
	 * @param reference
	 * @param schema
	 * @return a connection from the pool
	 * @throws Exception
	 */
	public Connection getConnection(Reference reference, Schema schema) throws Exception {
		return getDataSource(schema).getConnection(getSchemaName(reference, schema));
	}

	public void disconnectAll(){
		if (dataSourceMain.getDBMS() == DBMS.hsqldb) {
			try {
				update(Schema.HIGHLANDER, "SHUTDOWN");
			} catch (Exception ignored) {}
		}
		dataSourceMain.close();
	}

	public Results select(Schema schema, String query) throws Exception {
		return select(null, schema, query, false);
	}

	public Results select(Reference reference, Schema schema, String query) throws Exception {
		return select(reference, schema, query, false);
	}

	public Results select(Schema schema, String query, boolean hugeResultSetExpected) throws Exception {
		return select(null, schema, query, hugeResultSetExpected);
	}

	public Results select(Reference reference, Schema schema, String query, boolean hugeResultSetExpected) throws Exception {
		//System.out.println(query); 
		String sqlStatement = formatQuery(schema, query);
		try {
			Results res = new Results(this, reference, schema, sqlStatement, hugeResultSetExpected);
			runningSelects.put(res.getStatement(), sqlStatement);
			res.executeStatement();
			runningSelects.remove(res.getStatement());
			return res;
		} catch (com.mysql.jdbc.exceptions.MySQLStatementCancelledException ex){
			throw ex;
		} catch (Exception ex) {
			Tools.print("SQL statement throwing exception : " + sqlStatement);
			throw ex;
		}
	}

	/**
	 * Cancel all 'select' queries still executing.
	 * As describe in Statement.cancel(), must be used from another thread, 
	 * and both the DBMS and driver must support aborting an SQL statement. 
	 * 
	 * In addition, kill the query, as if it's in state "Sending to client",
	 * the process will continue
	 * 
	 * @throws Exception
	 */
	public void cancelActiveSelects() throws Exception {
		for (Iterator<Statement> it = runningSelects.keySet().iterator() ; it.hasNext() ;){
			Statement stmt = it.next();
			if(stmt != null && !stmt.isClosed()) {
				stmt.cancel();
				it.remove();
			}
		}
		//Kill the query if it's still present in the process list
		try (Results res = select(Schema.HIGHLANDER, "SHOW PROCESSLIST")){
			while (res.next()) {
				String host = InetAddress.getLocalHost().getHostName();
				String ip = InetAddress.getLocalHost().getHostAddress();
				String sqlHost = res.getString("Host");
				if (sqlHost.contains(host) || sqlHost.contains(ip)) {
					String command = res.getString("COMMAND");
					String info = res.getString("INFO");
					if (command.equalsIgnoreCase("Query") && !info.equalsIgnoreCase("SHOW PROCESSLIST")) {
						String id = res.getString("ID");
						System.out.println("Killing [" + res.getString("STATE") + "] - [" + info + "]");
						update(Schema.HIGHLANDER, "KILL " + id);
					}
				}
			}
		}
	}

	public Set<String> getRunningSelects() {
		return new HashSet<>(runningSelects.values());
	}
	
	public void update(Schema schema, String query) throws Exception {
		update(null, schema, query);
	}

	public void update(Reference reference, Schema schema, String query) throws Exception {
		//System.out.println(query);
		String sqlStatement = formatQuery(schema, query);
		try (Connection con = getConnection(reference, schema)){
			try (Statement stm = con.createStatement()){
				try {
					stm.executeUpdate(sqlStatement, Statement.NO_GENERATED_KEYS);
				} catch (SQLException ex) {
					Tools.print("SQL statement throwing exception : " + sqlStatement);
					throw ex;
				}
			}
		}
	}

	public void insert(Schema schema, String query) throws Exception {
		insert(null, schema, query);
	}

	public void insert(Reference reference, Schema schema, String query) throws Exception {
		update(reference, schema, query);
	}

	public int insertAndGetAutoId(Schema schema, String query) throws Exception {
		return insertAndGetAutoId(null, schema, query);
	}

	public int insertAndGetAutoId(Reference reference, Schema schema, String query) throws Exception {
		//System.out.println(query);
		String sqlStatement = formatQuery(schema, query);
		try (Connection con = getConnection(reference, schema)){
			try (Statement stm = con.createStatement()){
				try {
					int r =  stm.executeUpdate(sqlStatement, Statement.RETURN_GENERATED_KEYS);
					if (r != 0) {
						int id = -1;
						try (ResultSet res = stm.getGeneratedKeys()){
							if (res.next()){
								id = res.getInt(1);
							}
						}
						if (id != -1) return id;
					}
					throw new Exception("Cannot retreive id");
				} catch (SQLException ex) {
					Tools.print("SQL statement throwing exception : " + sqlStatement);
					throw ex;
				}
			}
		}
	}

	/**
	 * check some constraints and ensure validity of strings
	 * @param schema
	 * @param input
	 * @return
	 */
	public static String format(DBMS dbms, Schema schema, String input) {
		// replaceAll("[\\p{Cf}]", "") --> remove invisible formatting indicator (pose mysql illegal mix of collations error with VUB database)
		switch(dbms){
		case hsqldb:
			return input.replaceAll("'", "''").replaceAll("\"", "'\"").replaceAll("[\\p{Cf}]", "");
		default:
			return input.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"").replaceAll("'", "\\\\'").replaceAll("[\\p{Cf}]", "");
		}
	}

	public String format(Schema schema, String input) {
		return format(getDBMS(schema), schema, input);
	}
	
	/**
	 * adapt queries for non-MySQL DBMS
	 * @param schema
	 * @param query
	 * @return
	 */
	public String formatQuery(Schema schema, String query){
		switch(getDBMS(schema)){
		case hsqldb:
			if (query.toUpperCase().startsWith("SHOW TABLES")) return "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC' AND TABLE_TYPE='BASE TABLE'";
			if (query.toUpperCase().startsWith("DESCRIBE")) return "SELECT * FROM INFORMATION_SCHEMA.SYSTEM_COLUMNS WHERE TABLE_NAME = '"+query.split(" ")[1].toUpperCase()+"'";
			if (query.toUpperCase().startsWith("SHOW COLUMNS FROM") || query.toUpperCase().startsWith("SHOW FULL COLUMNS FROM")) return "SELECT * FROM INFORMATION_SCHEMA.SYSTEM_COLUMNS WHERE TABLE_NAME = '"+query.split(" ")[query.split(" ").length-1].toUpperCase()+"'";
			if (query.toUpperCase().startsWith("ALTER TABLE")){
				query = query.replaceAll(" COMMENT '.+'", "");
				query = query.replaceAll("ENUM\\(.+\\)", "VARCHAR(255)");
				query = query.replaceAll("INT\\(.+\\)", "INT");
				query = query.replaceAll("LONGINT", "INT");
				query = query.replaceAll("MEDIUMINT", "INT");
				query = query.replaceAll("LONGTEXT", "LONGVARCHAR");
				query = query.replaceAll("MEDIUMTEXT", "LONGVARCHAR");
				query = query.replaceAll("TEXT", "LONGVARCHAR");
				query = query.replaceAll("LONGBLOB", "BLOB");
				query = query.replaceAll(" UNSIGNED", "");
				query = query.replaceAll("NOT NULL (DEFAULT .*) ", "$1 NOT NULL ");
				query = query.replaceAll("NOT NULL (DEFAULT .*),", "$1 NOT NULL,");
				query = query.replaceAll("\\\\'", "''");	
			}
			if (query.toUpperCase().matches("INSERT .*INTO [^ ]+ SET.+")){
				StringBuilder sb = new StringBuilder();
				int split = query.toUpperCase().indexOf(" SET ");
				sb.append(query.substring(0, split));
				sb.append(" (");
				String[] vals = query.substring(split+5).split(",");
				for (String v : vals){
					sb.append(v.split("=")[0].trim()+",");	
				}
				sb.deleteCharAt(sb.length()-1);
				sb.append(") VALUES(");
				for (String v : vals){
					sb.append(v.split("=")[1].trim()+",");	
				}
				sb.deleteCharAt(sb.length()-1);
				sb.append(")");
				query = sb.toString();
			}
			return formatHsqldbEscape(query.
					replaceAll("as SIGNED", "as INTEGER").
					replaceAll("DATE\\(([^\\)]+)\\)", "$1").
					replaceAll(" SQL_NO_CACHE ", " ").
					replaceAll("REPLACE\\(run_date,\"-\",\"_\"\\)", "REPLACE(to_char(run_date,\"YYYY MM DD\"),\" \",\"_\")")
					);
		default:
			return query;
		}  	
	}

	private static String formatHsqldbDoubleQuotes(String input){
		if (!input.contains("\"")) return input;
		String[] parts = input.split("\"");
		StringBuilder sb = new StringBuilder();
		if (input.startsWith("\"")){
			sb.append("'");
		}
		for (int i=0 ; i < parts.length ; i++){
			if (i%2 == 0){
				sb.append(parts[i]);
			}else{
				sb.append(parts[i].toUpperCase());
			}
			if (i < ((parts.length % 2 == 0) ? parts.length : parts.length-1)){
				sb.append("'");
			}
		}
		return sb.toString();
	}

	public static String formatHsqldbEscape(String input){
		input = formatHsqldbDoubleQuotes(input);
		if (!input.contains("`")) return input;
		String[] parts = input.split("`");
		StringBuilder sb = new StringBuilder();
		if (input.startsWith("`")){
			sb.append("\"");
		}
		for (int i=0 ; i < parts.length ; i++){
			if (i%2 == 0){
				sb.append(parts[i]);
			}else{
				sb.append(parts[i].toUpperCase());
			}
			if (i < ((parts.length % 2 == 0) ? parts.length : parts.length-1)){
				sb.append("\"");
			}
		}
		return sb.toString();
	}

	public String getDescribeColumnName(Schema schema, Results res) throws Exception {
		switch(getDBMS(schema)){
		case hsqldb:
			return res.getString("COLUMN_NAME").toLowerCase();
		case mysql:
		default:
			return res.getString("field");
		}
	}

	public String getDescribeColumnType(Schema schema, Results res) throws Exception {
		switch(getDBMS(schema)){
		case hsqldb:
			return res.getString("TYPE_NAME");
		case mysql:
		default:
			return res.getString("type");
		}
	}

	public String getDescribeColumnComment(Schema schema, Results res) throws Exception {
		switch(getDBMS(schema)){
		case hsqldb:
			return "";
		case mysql:
		default:
			return res.getString("Comment");
		}
	}

	public void insertFile(Schema schema, String table, String columns, File insertFile, boolean replaceExisting, Parameters parameters) throws Exception {
		insertFile(null, schema, table, columns, insertFile, replaceExisting, parameters);
	}

	public void insertFile(Reference reference, Schema schema, String table, String columns, File insertFile, boolean replaceExisting, Parameters parameters) throws Exception {
		switch(getDBMS(schema)){
		case mysql:
			String replace = (replaceExisting) ? "REPLACE" : "IGNORE";
			String url = parameters.getUrlForSqlImportFiles()+"/"+insertFile.getName();
			if(Tools.exists(url)){
				update(reference, schema, "LOAD DATA LOCAL INFILE '"+url+"' "+replace+" INTO TABLE " + table+" ("+columns+");");				
			}else{
				update(reference, schema, "LOAD DATA LOCAL INFILE '"+insertFile.getCanonicalPath().replace('\\', '/')+"' "+replace+" INTO TABLE " + table+" ("+columns+");");
			}
			break;
		case hsqldb:
			//Do not *format* statement for hsqldb, already done here, so don't use update() but directly create statements here
			if (columns == null || columns.length() == 0) throw new Exception("You MUST give the list of columns represented in the insertFile");
			//Create temporary table
			try (Connection con = getConnection(reference, schema)){
				try (Statement stm = con.createStatement()){
					stm.executeUpdate("DROP TABLE IF EXISTS temp_"+table);
				}
			}
			StringBuilder statement = new StringBuilder();
			statement.append("CREATE TEXT TABLE temp_"+table+"(");
			for (String column : columns.split(",")){
				statement.append(column.trim()+" LONGVARCHAR,");
			}
			statement.deleteCharAt(statement.length()-1);
			statement.append(")");
			try (Connection con = getConnection(reference, schema)){
				try (Statement stm = con.createStatement()){
					stm.executeUpdate(statement.toString());
				}
			}
			//Using the csv as a temporary text table
			try (Connection con = getConnection(reference, schema)){
				try (Statement stm = con.createStatement()){
					stm.executeUpdate("SET TABLE temp_"+table+" SOURCE \""+insertFile.getAbsolutePath()+";ignore_first=false;fs=\\t\"");
				}
			}
			//Importing the data from the temporary table to the main table
			try (Connection con = getConnection(reference, schema)){
				try (Statement stm = con.createStatement()){
					String insert = (replaceExisting) ? "REPLACE " : "INSERT IGNORE INTO ";
					stm.executeUpdate(insert+table+" ("+columns+") SELECT "+columns+" FROM temp_"+table+" ON DUPLICATE KEY UPDATE");
				}
			}
			//Deleting the temporary table
			try (Connection con = getConnection(reference, schema)){
				try (Statement stm = con.createStatement()){
					stm.executeUpdate("DROP TABLE temp_"+table);
				}
			}
			break;
		default:
			throw new Exception("Importation is not supported with DBMS '"+getDBMS(schema)+"'");
		}
	}

	/**
	 * Return all elements of given Set in sql format : 'elem1','elem2','elem3'...
	 * @param h a Set of elements
	 * @return elements in sql format
	 */
	public static String makeSqlList(Set<?> h, Class<?> type) {
		if(type == Integer.class || type == Double.class || type == Long.class){
			return h.toString().replace('[', ' ').replace(']', ' ').trim();			
		}else{
			return h.toString().replaceAll(", ", "','").replace('[', '\'').replace(']', '\'');			
		}
	}

	/**
	 * Return all elements of given Set in sql format : 'elem1','elem2','elem3'...
	 * @param h a Set of elements
	 * @return elements in sql format
	 */
	public static String makeSqlList(List<?> h, Class<?> type) {
		if(type == Integer.class || type == Double.class || type == Long.class){
			return h.toString().replace('[', ' ').replace(']', ' ').trim();			
		}else{
			return h.toString().replaceAll(", ", "','").replace('[', '\'').replace(']', '\'');
		}
	} 

	/**
	 * Return all elements of given Set in sql format : 'elem1','elem2','elem3'...
	 * @param h a Set of elements
	 * @return elements in sql format
	 */
	public static String makeSqlFieldList(List<?> h) {
		return h.toString().substring(1,h.toString().length()-1);
	}

	public boolean checkDatabaseVersionCompatibility() throws Exception {
		try (Results res = select(Schema.HIGHLANDER,"SELECT version FROM main")) {
			if(res.next()){
				String dbVersion = res.getString("version");
				return dbVersion.equals(Highlander.databaseRequiredVersion);
			}
			return false;
		}
	}

	public List<String> getAvailableTables(Reference reference, Schema schema){
		if (hasSchema(reference, schema)){
			try {
				String schemaName = getSchemaName(reference, schema);
				if (!availableTables.containsKey(schemaName)) {
					List<String> tables = new ArrayList<String>();
					try (Results res = select(reference, schema, formatQuery(schema, "SHOW TABLES"), false)) {
						while (res.next()){
							tables.add(res.getString(1));
						}
					}
					availableTables.put(schemaName, tables);
				}
				return new ArrayList<String>(availableTables.get(schemaName));
			}catch(Exception ex) {
				Tools.exception(ex);
			}
		}
		return new ArrayList<String>();
	}

	public List<String> getAvailableColumns(Reference reference, Schema schema, String table) throws Exception {
		List<String> columns = new ArrayList<String>();
		try (Results res = select(reference, schema, "DESCRIBE "+table)) {
			while (res.next()){
				columns.add(getDescribeColumnName(schema, res));
			}
		}
		return columns;
	}

	public static String getNullString(DBMS dbms){
		switch(dbms){
		case hsqldb:
			return "";
		case mysql:
		default:
			return "\\N";
		}
	}

	public boolean isBetaFunctionalitiesActivated(){
		try{
			boolean show = false;
			try (Results res = select(Schema.HIGHLANDER,"SELECT beta_functionalities FROM main")) {  		
				if(res.next()){
					show = res.getBoolean(1);
				}
			}
			return show;
		}catch(Exception ex){
			Tools.exception(ex);
		}
		return false;
	}
}