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

package be.uclouvain.ngs.highlander.administration;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Parameters;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.SqlGenerator;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.DBMS;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;

public class LocalDbGenerator {

	Parameters parametersMysql;
	Parameters parametersLocal;
	HighlanderDatabase DB;
	HighlanderDatabase local;
	DBMS dbms;

	Schema[] schemas = new Schema[]{Schema.DBNSFP,Schema.ENSEMBL,Schema.GNOMAD_WES,Schema.GNOMAD_WGS,Schema.COSMIC,Schema.EXAC,Schema.GONL};

	//TODO DEMO - creation of those analyses is probably not enough, need to update analyses table after creation and add references and sequencing targets
	public LocalDbGenerator(String configFileMysql, String configFileLocal){
		List<Analysis> analyses = new ArrayList<>();
		analyses.add(new Analysis("exomes"));
		analyses.add(new Analysis("genomes"));
		analyses.add(new Analysis("panels"));
		analyses.add(new Analysis("rnaseq"));
		parametersLocal = new Parameters(false, new File(configFileLocal));
		dbms = parametersLocal.getDbMainJdbc();
		try{
			Thread.sleep(1000);
			parametersMysql = new Parameters(false, new File(configFileMysql));
			Highlander.initialize(parametersMysql, 5);
			DB = Highlander.getDB();
			local = new HighlanderDatabase(parametersLocal);
			SqlGenerator.localDbGenerator(dbms, local);
			String createStatement = null;

		//TODO DEMO - créer les analyses désirées via l'outil d'admin, pas de raison d'en avoir des vides auto-créées
			
			try (Connection con = local.getConnection(null, Schema.HIGHLANDER)){ 
				for (Analysis analysis : analyses){
					try (PreparedStatement pstmt = con.prepareStatement("UPDATE analyses SET icon = ? WHERE analysis = '"+analysis+"'")){
						File image = new File("src/gehu/ngs/highlander/resources/"+analysis+".png");
						try (FileInputStream fis = new FileInputStream(image)){
							try (ByteArrayOutputStream bos = new ByteArrayOutputStream()){
								byte[] buf = new byte[1024];
								for (int readNum; (readNum = fis.read(buf)) != -1;)
								{
									bos.write(buf, 0, readNum);
								}
								pstmt.setBytes(1, bos.toByteArray());
								pstmt.executeUpdate();
							}
						}
					}
				}
			}

			for (Schema schema : schemas){
				List<String> tables = new ArrayList<>();
				try (Results res = DB.select(schema, "SHOW TABLES")) {
					while(res.next()){
						tables.add(res.getString(1));
					}
				}
				for (String table : tables){
					try (Results res = DB.select(schema, "SHOW CREATE TABLE "+table)) {
						if(res.next()){
							createStatement = res.getString(2).toUpperCase().replaceFirst("CREATE TABLE", "CREATE CACHED TABLE") + ";";
							/*
						if (schema == Schema.UCSC){
							//HsqlDB takes ages to insert BLOBs from UCSC, changing it to LONGVARBINARY solved the problem 
							//SELECT MAX(OCTET_LENGTH(exonStarts)), MAX(OCTET_LENGTH(exonEnds)), MAX(OCTET_LENGTH(exonframes)) from refGene; --> gives the same number for MySQL and HSQL
							createStatement = createStatement.replaceAll("LONGBLOB", "LONGVARBINARY");
						}
							 */
						}
					}
					System.out.println("Creating `"+schema+"`.`"+table+"`");
					local.update(Schema.HIGHLANDER,"DROP TABLE IF EXISTS " + table);
					for (String st : SqlGenerator.convert(new StringBuilder(createStatement), dbms)) local.update(Schema.HIGHLANDER,st);
					transferTo(dbms, schema, table, table);
				}
				System.out.println("Optimizing `"+schema+"`");
				local.update(Schema.HIGHLANDER,"CHECKPOINT DEFRAG");
				local.disconnectAll();
				Thread.sleep(1000);				
			}

			System.out.println("Highlander local db ready !");
		}catch(Exception ex){
			ex.printStackTrace();
			local.disconnectAll();
			try{Thread.sleep(1000);}catch(InterruptedException ie){};
		}
	}

	public void transferTo(DBMS dbms, Schema schemaFrom, String tableFrom, String tableTo) throws Exception {
		transferTo(dbms, schemaFrom, tableFrom, tableTo, null);
	}

	/*
	 * 
	 	It looks like you are using in-memory tables and the memory runs out when you insert a lot of rows.
		Try CREATE CACHED TABLE with a file-based database. You can then try different batch sizes.
		You must also commit after each batch. All the inserted rows are kept in memory until you commit.
	 * 
	 */
	public void transferTo(DBMS dbms, Schema schemaFrom, String tableFrom, String tableTo, String whereclause) throws Exception {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd - HH_mm_ss");
		System.out.println("Fetching "+tableFrom);
		String query = "SELECT SQL_NO_CACHE * FROM "+tableFrom;
		if (whereclause != null && whereclause.length() > 0) query += " WHERE " + whereclause;
		long total = 0;
		try (Results res = DB.select(schemaFrom, query.replace("SELECT SQL_NO_CACHE *", "SELECT COUNT(*)"))) {
			total = (res.next()) ? res.getLong(1) : 0;
		}
		List<String> columns = new ArrayList<String>();
		try (Results res = DB.select(schemaFrom, query + " LIMIT 1")) {
			for (int i=1 ; i <= res.getMetaData().getColumnCount() ; i++){
				String column = res.getMetaData().getColumnName(i);
				if (!column.equals("id") && !column.startsWith("found_in_")){
					columns.add(column);
				}
			}
		}
		long p=0;
		StringBuilder prepStat = new StringBuilder();
		prepStat.append("INSERT INTO " + tableTo + " (`"+HighlanderDatabase.makeSqlFieldList(columns).replace(", ", "`, `")+"`) VALUES (");
		for (int i= 0 ; i < columns.size() ; i++){
			prepStat.append("?,");	
		}
		prepStat.deleteCharAt(prepStat.length()-1);
		prepStat.append(")");
		//if (dbms == DBMS.sqlite) local.select("PRAGMA journal_mode = MEMORY");
		try (Connection con = local.getConnection(null, Schema.HIGHLANDER)){ 
			con.setAutoCommit(false);
			try (PreparedStatement pstmt = con.prepareStatement((dbms == DBMS.hsqldb) ? HighlanderDatabase.formatHsqldbEscape(prepStat.toString()) : prepStat.toString())){
				int countBatch = 0;
				try (Results res = DB.select(schemaFrom, query, true)) {
					while (res.next()){
						if((++p)%10_000 == 0){
							System.out.println(df.format(System.currentTimeMillis()) + " - " + Tools.longToString(p) + " / " + Tools.longToString(total) + " records prepared to be inserted - " + Tools.doubleToString(((double)Tools.getUsedMemoryInMb()), 0, false) + " Mb / "+ (Tools.doubleToString(((double)(Runtime.getRuntime().maxMemory() / 1024 /1024)), 0, false)) + " Mb of RAM used");
						}
						for (int i = 0 ; i < columns.size() ; i++){
							String column = columns.get(i);
							pstmt.setObject(i+1, res.getObject(column));
						}
						pstmt.addBatch();
						if ((++countBatch)%100_000 == 0){
							System.out.println("Inserting  records "+Tools.intToString(countBatch-100_000)+" to "+Tools.intToString(countBatch)+" records in "+tableTo + " ... ");
							pstmt.executeBatch();
							con.commit();
							pstmt.clearBatch();
							System.out.println("Batch insertion successful");
						}
					}
				}
				pstmt.executeBatch();
				con.commit();
			}
		}
	}

	public static void main(String[] args) {
		if (args.length != 2) {
			System.err.println("Please give the Mysql db config file and the target local db config file");; 
		}else{
			new LocalDbGenerator(args[0], args[1]);
		}
	}

}
