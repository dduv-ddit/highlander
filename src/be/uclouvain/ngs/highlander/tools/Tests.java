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

package be.uclouvain.ngs.highlander.tools;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Parameters;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.administration.DbBuilder;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;

/**
* This class is only used for internal tests of new functionalities. 
* @author Raphael Helaers
*/

public class Tests {

	//Compare analysis split results to see which are the missing variants
	//It was due tu UNIQUE INDEX limits, with ref/alt fields to size 300/500 everything is present
	public static void checkSplitDB(Analysis analysis) throws Exception {
		Map<String,Integer> map = new HashMap<>();
		int count = 0;
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
				"SELECT `chr`,`pos`,if((variant_type='SNP' OR variant_type='MNP'),length(reference), abs(length(reference)-length(alternative))) as length,`reference`,`alternative`,`gene_symbol`,`patient` "
				+ "FROM " + analysis, true)){
			while(res.next()) {
				String key = (res.getString("chr").length() <= 2) ? res.getString("chr") : res.getString("chr").substring(0,2);
				key += "-" + res.getString("pos");
				key += "-" + res.getString("length");
				key += "-" + ((res.getString("reference").length() <= 5) ? res.getString("reference") : res.getString("reference").substring(0,5));
				key += "-" + ((res.getString("alternative").length() <= 5) ? res.getString("alternative") : res.getString("alternative").substring(0,5));
				key += "-" + ((res.getString("gene_symbol") == null) ? "" : ((res.getString("gene_symbol").length() <= 25) ? res.getString("gene_symbol") : res.getString("gene_symbol").substring(0,25)));
				key += "-" + ((res.getString("patient").length() <= 25) ? res.getString("patient") : res.getString("patient").substring(0,25));
				if (!map.containsKey(key)) {
					map.put(key, 1);
				}else {
					map.put(key, map.get(key)+1);
					count++;
				}
			}
		}
		System.out.println("Total keys = " + map.size());
		System.out.println("Missed inserts = " + count);
		for (String key : map.keySet()) {
			if (map.get(key) > 1) {
				System.out.println(key + "\t" + map.get(key));
			}
		}
	}
	
	public static void fillIndividualFieldForGEHUSamples() throws Exception {
		Map<String, String> changes = new HashMap<>();
		try(Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT sample FROM projects ORDER BY sample")){
			while (res.next()) {
				String sample = res.getString("sample");
				String change = sample;
				if (change.contains(".")) {
					change = change.split("\\.")[0];
				}
				if (!change.startsWith("WIAME")) {
					change = change.split("-[A-Z]-.+")[0];
					change = change.split("-Tissue-.+")[0];
					change = change.split("-blood-.+")[0];
					change = change.split("-Blood-.+")[0];
					change = change.split("-Frozen-.+")[0];
					if (change.toUpperCase().endsWith("-T") 
							|| change.toUpperCase().endsWith("-A") 
							|| change.toUpperCase().endsWith("-B") 
							|| change.toUpperCase().endsWith("-C") 
							|| change.toUpperCase().endsWith("-D") 
							|| change.toUpperCase().endsWith("-E") 
							|| change.toUpperCase().endsWith("-O") 
							|| change.toUpperCase().endsWith("-P")) {
						change = change.substring(0, change.length()-2);
					}
					if (change.toUpperCase().endsWith("-TB") 
							|| change.toUpperCase().endsWith("-TA") 
							|| change.toUpperCase().endsWith("-BB") 
							|| change.toUpperCase().endsWith("-EC") 
							|| change.toUpperCase().endsWith("-CF") 
							|| change.toUpperCase().endsWith("-RL")) {
						change = change.substring(0, change.length()-3);
					}
					if (change.toUpperCase().endsWith("-RLA")
							|| change.toUpperCase().endsWith("-WGS")
							|| change.toUpperCase().endsWith("-HIQ")) {
						change = change.substring(0, change.length()-4);
					}
					if (change.toUpperCase().endsWith("-TBIS")) {
						change = change.substring(0, change.length()-5);
					}
					if (change.toUpperCase().endsWith("-CELLS") 
							|| change.toUpperCase().endsWith("-BLOOD")
							|| change.toUpperCase().endsWith("-FIBRO")
							|| change.toUpperCase().endsWith("-LATER")) {
						change = change.substring(0, change.length()-6);
					}
					if (change.toUpperCase().endsWith("-FROZEN")
							|| change.toUpperCase().endsWith("-NEEDLE")) {
						change = change.substring(0, change.length()-7);
					}
				}
				if (!sample.equals(change)) {
					changes.put(sample, change);
					System.out.println(sample + " -> " + change);
					Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE projects SET individual = '"+change+"' WHERE sample = '"+sample+"'");
				}else {
					//System.out.println(sample);
				}
			}
		}
	}
	
	public static void testPHPWithApacheHttpClient() throws Exception {
		HttpClient httpClient = new HttpClient();
		PostMethod post = new PostMethod(Highlander.getParameters().getUrlForPhpScripts()+"/test.php");
		
		System.out.println("Executing post method");
		httpClient.executeMethod(post);
		
		System.out.println("Opening input stream");
		//try (BufferedReader br = new BufferedReader(new InputStreamReader(post.getResponseBodyAsStream()))){
		InputStreamReader isr = new InputStreamReader(post.getResponseBodyAsStream());

    System.out.println("Stream ready");
    
		int value = 0;
		while(((value = isr.read()) != -1)) {
			char c = (char)value;
			System.out.print(c);
		}
		System.out.println("++++ DONE ++++");
	}
	
	public static void testPHPWithHttpURLConnection() throws Exception {
		URL url = new URL(Highlander.getParameters().getUrlForPhpScripts()+"/bamcheck.php");
		Map<String,Object> params = new LinkedHashMap<>();
    params.put("filename", "99999");
    params.put("patients", "\"panels_torrent_caller|VA-1159-T.pVMGENES;panels_torrent_caller|VA-1167-T.pVMGENES;panels_torrent_caller|VA-1179-T.pVMGENES;panels_torrent_caller|VA-1192-T.pVMGENES;panels_torrent_caller|VA-1206-T.pVMGENES;panels_torrent_caller|VA-1211-T.pVMGENES;panels_torrent_caller|VA-1217-T.pVMGENES;panels_torrent_caller|VA-1218-T.pVMGENES;panels_torrent_caller|VA-1230-T.pVMGENES;panels_torrent_caller|VA-1246-T.pVMGENES;panels_torrent_caller|VA-1249-T.pVMGENES;panels_torrent_caller|VA-1250-T.pVMGENES\"");
    params.put("positions", "\"9:27206638;9:27212707;9:27212770\"");

    StringBuilder postData = new StringBuilder();
    for (Map.Entry<String,Object> param : params.entrySet()) {
        if (postData.length() != 0) postData.append('&');
        postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
        postData.append('=');
        postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
    }
    byte[] postDataBytes = postData.toString().getBytes("UTF-8");

    HttpURLConnection conn = (HttpURLConnection)url.openConnection();
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
    conn.setDoOutput(true);
    conn.getOutputStream().write(postDataBytes);

    System.out.println("Opening input stream");
    
    InputStreamReader isr = new InputStreamReader(conn.getInputStream(), "UTF-8");

    System.out.println("Stream ready");
    
		int value = 0;
		while(((value = isr.read()) != -1)) {
			char c = (char)value;
			if (c != '#')
				System.out.print(c);
		}
		System.out.println("++++ DONE ++++");
	}
	
	/**
	 * Génère des fichiers avec toutes les positions manquantes dans dbNSFP (ou n'ayant pas les 3 alt)
	 * Finalement j'ai trouvé le soucis, dbNSFP n'a pas d'entrée pour les SNV synonymous coding, d'où le grand nombre de positions manquantes
	 * Par contre certains exons entiers semblent encore manquants, mais ça n'a pas grand intérêt comparé à ce qu'il manque pour les NSC ...
	 * 
	 * @param analysis
	 * @throws Exception
	 */
	public static void verifyDBNSFP(AnalysisFull analysis) throws Exception {
		boolean addSplicingRegion = false; //DBNSFP doesn't seems to make a difference between forward/reverse genes, and always took start-1 & end+2. But it skip start-1 of first "left" non-UTR exon and end+2 of last "right" non-UTR exon, which is complicated to get from my region table alone. 
		File details_file = new File("missing_details.txt");
		File genes_file = new File("missing_genes.txt");
		File total_file = new File("missing_total.txt");
		try(FileWriter total_writer = new FileWriter(total_file)){
			int total_missing = 0;
			int total_alts = 0;
			try(FileWriter genes_writer = new FileWriter(genes_file)){
				Map<String,Integer> genes = new TreeMap<>();
				try(FileWriter details_writer = new FileWriter(details_file)){
					try(Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
							"SELECT chr, start, end, gene_symbol, exon FROM "+analysis.getFromCoverageRegions()+" WHERE utr IS FALSE AND coding IS TRUE", true)){
						while (res.next()) {
							String chr = res.getString("chr");
							int start = res.getInt("start");
							int end = res.getInt("end");
							String gene_symbol = res.getString("gene_symbol");
							int exon = res.getInt("exon");
							System.out.println(chr+":"+start+"-"+end+"|"+gene_symbol);
							total_alts += (end+((addSplicingRegion)?2:0))-(start-((addSplicingRegion)?1:0))+1;
							Map<Integer,Integer> problematicPos = new TreeMap<>();
							String positions = "(";
							for (int p = start-((addSplicingRegion)?1:0) ; p <= end+((addSplicingRegion)?2:0) ; p++) {
								problematicPos.put(p,0);
								positions += p;
								if (p < end+((addSplicingRegion)?2:0)) {
									positions += ",";
								}else {
									positions += ")";
								}
							}
							try(Results intRes = Highlander.getDB().select(analysis.getReference(), Schema.DBNSFP, 
									"SELECT pos, COUNT(*) FROM chromosome_"+chr+" WHERE pos IN " + positions + " GROUP BY pos")){
								while (intRes.next()) {
									int pos = intRes.getInt(1);
									int count = intRes.getInt(2);
									if (count >= 3) {
										problematicPos.remove(pos);							
									}else {
										problematicPos.put(pos,count);
									}
								}
							}
							if (!genes.containsKey(gene_symbol)) {
								genes.put(gene_symbol, 0);
							}
							for (int pos : problematicPos.keySet()) {
								details_writer.write(chr+"\t"+pos+"\t"+gene_symbol+"\t"+problematicPos.get(pos)+" alt\t"+((pos < start || pos > end) ? "splice site" : "exon")+" "+exon+"\n");
								genes.put(gene_symbol, genes.get(gene_symbol)+(3-problematicPos.get(pos)));
							}
						}
					}		
				}
				for (String gene_symbol : genes.keySet()) {
					genes_writer.write(gene_symbol+"\t"+genes.get(gene_symbol)+"\t");
					total_missing += genes.get(gene_symbol);
				}
			}
			total_writer.write("total missing alts:\t"+total_missing+"\n");
			total_writer.write("total alts (3 alts per pos):\t"+total_alts+"\n");
			total_writer.write("percentage missing alts:\t"+Tools.doubleToPercent(total_missing/total_alts,0)+"\n");
		}
		System.out.println("DONE");
	}
	
	public static void reimportAllFastQC(boolean RNAseq) throws Exception {
		DbBuilder dbb = new DbBuilder("/data/highlander/config/settings.xml");
		String root = (RNAseq) ? "rnaseq" : "highlander";
		File results = new File("/data/"+root+"/results");
		for (File run : results.listFiles()) {
			if (run.isDirectory() && run.getName().startsWith("0_")) {
				System.out.println(run.getName());
				for (File ref : run.listFiles()) {
					if (ref.isDirectory() && ref.getName().equals("GRCh38")) {
						for (File sample : ref.listFiles()) {
							File fastqc = new File(sample.getAbsolutePath() + "/" + sample.getName() + "_fastqc.zip");
							if (fastqc.exists()) {
								dbb.importProjectFastQC(run.getName(), sample.getName(), fastqc.getAbsolutePath());
							}else {
								System.err.println(sample.getAbsolutePath());
							}
						}
					}
				}						
			}
		}
	}
	
	
	public static void main(String[] args) {
		try {
			Highlander.initialize(new Parameters(false, new File("..\\config\\GEHU admin\\settings.xml")), 20);			
			/*
			reimportAllFastQC(true);
			*/
			/*
			for (AnalysisFull analysis : AnalysisFull.getAvailableAnalyses()) {
				if (!analysis.toString().equals("exomes_haplotype_caller") &&
						!analysis.toString().equals("genomes_haplotype_caller") &&
						!analysis.toString().equals("exomes_solid")
						) {
				System.out.println("-------------------------------------------------------------------");
				System.out.println(analysis);
				System.out.println("-------------------------------------------------------------------");
				checkSplitDB(analysis);
				System.out.println("-------------------------------------------------------------------");
				}
			}
			*/
			/*
			fillIndividualFieldForGEHUSamples();
			*/
			/*
			testPHPWithApacheHttpClient();
			testPHPWithHttpURLConnection();
			*/
			/*
			 * Setting coverage XY ratio for all WES
			 * 
			for (AnalysisFull analysis : AnalysisFull.getAvailableAnalyses()) {
				if (analysis.toString().equals("exomes_hc_hg38")) {
					int count = 0;
					try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT project_id FROM projects join projects_analyses using (project_id) where analysis = '"+analysis+"' AND gene_coverage_ratio_chr_xy IS NULL")){
						while (res.next()) {
							System.out.println(count++);
							int project_id = res.getInt("project_id");
							Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE projects_analyses SET `gene_coverage_ratio_chr_xy` = '"+DBUtils.getGeneCoverageRatioChrXY(analysis.getReference(), project_id)+"' WHERE project_id = " + project_id + " AND analysis = '"+analysis+"'");					
						}
					}
				}
			}
			*/
			/*
			String user = args[0];
			if (user.contains("\\")) {
				System.out.println("NT proxy credentials");
				System.out.println("user: " + user.split("\\\\")[1]);
				System.out.println("domain: " + user.split("\\\\")[0]);
			}else {
				System.out.println("non-NT proxy credentials");
				System.out.println("user: " + user);			
				}
			*/
			/*
			verifyDBNSFP(new AnalysisFull(new Analysis("exomes_hg38")));
			*/
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}

}
