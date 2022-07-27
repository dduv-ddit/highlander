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

import java.sql.Connection;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import be.uclouvain.ngs.highlander.Parameters;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.DBMS;

public class HighlanderDataSource {
	Parameters P;
	private String dbHost;
	private String dbUser;
	private String dbPassword;
	private DBMS dbms;
	private boolean compression;

	private HikariConfig config = new HikariConfig();
	private HikariDataSource datasource;

	public HighlanderDataSource(Parameters parameters, int maxPoolSize) {
		this.P = parameters;  		
		if (dbms == DBMS.hsqldb) System.setProperty("textdb.allow_full_path","true");
		//Can be set to "trace", "debug", "info", "warn", "error" or "off"
		System.setProperty("org.slf4j.simpleLogger.log.com.zaxxer.hikari","warn");
		//Doesn't work ... and don't figure why ...
		//System.setProperty("org.slf4j.simpleLogger.logFile","System.out");
		//... because adding the property with java -D works
		//-Dorg.slf4j.simpleLogger.logFile=System.out
		String database;
		dbHost = P.getDbMainHost();
		dbUser = P.getDbMainUser();
		dbPassword = P.getDbMainPassword();  			
		dbms = P.getDbMainJdbc();  			
		compression = P.isDbMainCompression();  						
		database = parameters.getSchemaHighlander();
		switch(dbms){
		case hsqldb:
			config.addDataSourceProperty( "shutdown" , "true" );
			//Etait dans LocalHsqldbDatabase
			config.addDataSourceProperty( "hsqldb.write_delay" , "false" );
			config.addDataSourceProperty( "allow_empty_batch" , "true" );
			config.setJdbcUrl("jdbc:"+dbms+":file:"+database);
			break;
		default:
			config.setJdbcUrl("jdbc:"+dbms+"://" + dbHost + "/" + database);
			break;
		}
		config.setUsername(dbUser);
		config.setPassword(dbPassword);
		config.setMinimumIdle(0);
		config.setMaximumPoolSize(maxPoolSize);
		config.setIdleTimeout(180000);
		config.setValidationTimeout(30000); //Problems @St-Luc with default 5000
		config.addDataSourceProperty( "cachePrepStmts" , "true" );
		config.addDataSourceProperty( "prepStmtCacheSize" , "250" );
		config.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );
		config.addDataSourceProperty( "allowUrlInLocalInfile" , "true" );
		if (compression) config.addDataSourceProperty( "useCompression" , "true" );
		datasource = new HikariDataSource( config );
		//datasource.setLeakDetectionThreshold(10 * 1000);
	}

	public DBMS getDBMS(){
		return dbms;
	}

	public Connection getConnection(String schema) throws SQLException {
		Connection con = datasource.getConnection();
		if (!con.getCatalog().equals(schema)) con.setCatalog(schema);
		return con;
	}

	public void close() {
		datasource.close();
	}

	public boolean isClosed() {
		return datasource.isClosed();
	}
}
