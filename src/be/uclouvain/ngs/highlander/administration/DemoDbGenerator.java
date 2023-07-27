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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Parameters;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.SqlGenerator;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.DBMS;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
import be.uclouvain.ngs.highlander.datatype.Reference;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull.VariantCaller;

public class DemoDbGenerator {

	Parameters parameters1000g;
	Parameters parametersDemo;
	HighlanderDatabase DB;
	HighlanderDatabase local;
	DBMS dbms;

	private final static String samples = "'NA06985','NA07000','NA07056','NA07357','NA10851','NA11829','NA11830','NA11831','NA11832','NA11881','NA11992','NA11994','NA11995','NA12003','NA12004','NA12005','NA12006','NA12043','NA12044','NA12046','NA18939','NA18941','NA18946','NA18954','NA18956','NA18957','NA18962','NA18963','NA18965','NA18977','NA18978','NA18979','NA18980','NA18992','NA18993','NA18994','NA18995','NA18997','NA18998','NA19001','NA19307','NA19309','NA19320','NA19323','NA19351','NA19355','NA19360','NA19374','NA19376','NA19378','NA19379','NA19390','NA19391','NA19401','NA19403','NA19430','NA19435','NA19436','NA19437','NA19438','NA20502','NA20503','NA20505','NA20508','NA20511','NA20512','NA20516','NA20517','NA20524','NA20526','NA20528','NA20529','NA20530','NA20531','NA20532','NA20533','NA20534','NA20535','NA20536','NA20538'";

	//TODO DEMO - plus checké depuis longtemps, des doute que ça tourne toujours (viré LocalDB et utilise une HighlanderDatabase aussi)
	public DemoDbGenerator(String configFile1000g, String configFileDemo){		
		parametersDemo = new Parameters(false, new File(configFileDemo));
		dbms = parametersDemo.getDbMainJdbc();
		try{			
			Thread.sleep(1000);
			parameters1000g = new Parameters(false, new File(configFile1000g));
			Highlander.initialize(parameters1000g, 20);
			DB = Highlander.getDB();
			local = new HighlanderDatabase(parametersDemo);

			Reference reference = new Reference("GRCh37");
			List<AnalysisFull> analyses = new ArrayList<>();
			analyses.add(new AnalysisFull("demo", VariantCaller.GATK, reference, "WES", 
					"ftp://ftp.1000genomes.ebi.ac.uk/vol1/ftp/phase3/data/@/exome_alignment/@.mapped.ILLUMINA.bwa.$.exome.20130415.bam", "http://127.0.0.1/vcf/demo", 
					"/data/highlander/bam/demo/", "/data/highlander/vcf/demo/", 
					".demo.vcf", ".demo.vcf"));
			
			SqlGenerator.localDbGenerator(dbms, local);
			String createStatement = null;
			//TODO DEMO - créer les analyses désirées ici après via AnalysisFull.insert(icon);

			local.insert(Schema.HIGHLANDER,"INSERT INTO users VALUES ('demo','"+Tools.md5Encryption(new String(""))+"','User','Demo','demo@highlander.com','user')");
			try (Connection con = local.getConnection(null, Schema.HIGHLANDER)){ 
				try (PreparedStatement pstmt = con.prepareStatement("UPDATE analyses SET bam_url = ?, icon = ? WHERE analysis = 'demo'")){
					pstmt.setString(1, "ftp://ftp.1000genomes.ebi.ac.uk/vol1/ftp/phase3/data/@/exome_alignment/@.mapped.ILLUMINA.bwa.$.exome.20130415.bam");
					File image = new File("src/gehu/ngs/highlander/resources/highlander.png");
					try (FileInputStream fis = new FileInputStream(image)){
						try (ByteArrayOutputStream bos = new ByteArrayOutputStream()){
							byte[] buf = new byte[1024];
							for (int readNum; (readNum = fis.read(buf)) != -1;)
							{
								bos.write(buf, 0, readNum);
							}
							pstmt.setBytes(2, bos.toByteArray());
							pstmt.executeUpdate();
						}
					}
				}
			}
			transferTo(dbms, Schema.HIGHLANDER, "projects", "projects", "sample IN ("+samples+")");

			try (Results res = DB.select(Schema.HIGHLANDER, "SELECT project_id, sample FROM projects WHERE sample IN ("+samples+")")) {
				while(res.next()){
					int id = res.getInt(1);
					String sample = res.getString(2);
					local.update(Schema.HIGHLANDER,"UPDATE projects SET project_id = " + id + " WHERE sample = '"+sample+"'");
					local.insert(Schema.HIGHLANDER,"INSERT INTO projects_users VALUES ("+id+",'demo')");
				}
			}			
			transferTo(dbms, Schema.HIGHLANDER, "exomes_1000g", "demo", "chr = '22' AND sample IN ("+samples+")");
			local.disconnectAll();
			Thread.sleep(1000);

			System.out.println("Fetching involved genes");
			Set<String> genes = new HashSet<String>(); 
			try (Results res = DB.select(Schema.HIGHLANDER, "SELECT DISTINCT(gene_symbol) FROM exomes_1000g WHERE chr = '22' AND sample IN ("+samples+") AND gene_symbol IS NOT NULL")) {
				while(res.next()){
					genes.add(res.getString(1));
				}
			}
			/*
			try (Results res = DB.select(Schema.UCSC, "SHOW CREATE TABLE refGene")) {
			if(res.next()){
				createStatement = res.getString(2).toUpperCase().replaceFirst("CREATE TABLE", "CREATE CACHED TABLE").replaceAll("LONGBLOB", "LONGVARBINARY") + ";";
			}
			}
			System.out.println("Creating `refGene`");
			local.update("DROP TABLE IF EXISTS refGene");
	    for (String st : SqlGenerator.convert(new StringBuilder(createStatement), dbms)) local.update(st);
	    transferTo(dbms, Schema.UCSC, "refGene", "refGene", "name2 IN ("+HighlanderDatabase.makeSqlList(genes, String.class)+")");
			local.disconnectAll();
			Thread.sleep(1000);
			 */
			//TODO DEMO - transfer Ensembl instead of UCSC !
			try (Results res = DB.select(reference, Schema.DBNSFP, "SHOW CREATE TABLE genes")) {
				if(res.next()){
					createStatement = res.getString(2).toUpperCase().replaceFirst("CREATE TABLE", "CREATE CACHED TABLE") + ";";
				}
			}
			System.out.println("Creating `genes`");
			local.update(Schema.HIGHLANDER,"DROP TABLE IF EXISTS genes");
			for (String st : SqlGenerator.convert(new StringBuilder(createStatement), dbms)) local.update(Schema.HIGHLANDER,st);
			transferTo(dbms, Schema.DBNSFP, "genes", "genes", "Gene_name IN ("+HighlanderDatabase.makeSqlList(genes, String.class)+")");
			local.disconnectAll();
			Thread.sleep(1000);

			DbBuilder dbb = new DbBuilder(configFileDemo);
			dbb.computeAlleleFrequencies(analyses, false);
			dbb.computeAlleleFrequencies(analyses, true);
			dbb.computePossibleValues(analyses);			
			dbb.setHardUpdate(false);
			dbb.DB.disconnectAll();
			Thread.sleep(1000);

			System.out.println("Highlander demo db ready !");
		}catch(Exception ex){
			ex.printStackTrace();
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
							System.out.println(df.format(System.currentTimeMillis()) + " - " + Tools.longToString(p) + " / " + Tools.longToString(total) + " records prepared to be inserted - " + Tools.doubleToString((Tools.getUsedMemoryInMb()), 0, false) + " Mb / "+ (Tools.doubleToString((Runtime.getRuntime().maxMemory() / 1024 /1024), 0, false)) + " Mb of RAM used");
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
			System.err.println("Please give the 1000g config file and the target demo config file");; 
		}else{
			new DemoDbGenerator(args[0], args[1]);
		}
	}

}
