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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Parameters;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Reference;

/**
* @author Raphael Helaers
*/

public class CorrectDBNSFP {

	/**
	 * This tool compare a constraint file from gnomad and the scores available in DBNSFP.
	 * more than a thousand DBNSFP 4.1 pLI scores don't match gnomad 2.1.1 (generally picking last transcript in the txt instead of canonical)
	 * 
	 *  Download constraint file at https://gnomad.broadinstitute.org/downloads#v2-constraint
	 * 
	 * @param source a gnomad constraint file
	 * @param dryRun set to true if you don't want to update the database and just see how much scores differ
	 * @throws Exception
	 */
	public static void compareGnomadPLI(File source, boolean dryRun) throws Exception {
		System.out.println("Retreiving scores from DBNSFP");
		Map<String,Double> mapPli = new HashMap<>();
		Map<String,Double> mapPrec = new HashMap<>();
		Map<String,Double> mapPnull = new HashMap<>();
		Map<String,String> synonyms = new HashMap<>();
		try(Results res = Highlander.getDB().select(new Reference("GRCh38"), Schema.DBNSFP, 
				"SELECT Gene_name, Gene_old_names, Gene_other_names, gnomAD_pLI, gnomAD_pRec, gnomAD_pNull FROM genes", true)){
			while (res.next()) {
				double pli = (res.getString("gnomAD_pLI").equals(".") || res.getString("gnomAD_pLI").equals("NA")) ? -1 : res.getDouble("gnomAD_pLI");
				double prec = (res.getString("gnomAD_pRec").equals(".") || res.getString("gnomAD_pRec").equals("NA")) ? -1 : res.getDouble("gnomAD_pRec");
				double pnull = (res.getString("gnomAD_pNull").equals(".") || res.getString("gnomAD_pNull").equals("NA")) ? -1 : res.getDouble("gnomAD_pNull");
				String gene = res.getString("Gene_name");
				synonyms.put(gene, gene);
				for (String g : res.getString("Gene_old_names").split(";")) synonyms.put(g, gene);
				for (String g : res.getString("Gene_other_names").split(";")) synonyms.put(g, gene);
				mapPli.put(gene, pli);
				mapPrec.put(gene, prec);
				mapPnull.put(gene, pnull);
			}
		}
		System.out.println("Retreiving scores from file");
		int totalGenes = 0;
		int geneNotFound = 0;
		int badPli = 0;
		int badPrec = 0;
		int badPnull = 0;
		try (FileReader fr = new FileReader(source)){
			try (BufferedReader br = new BufferedReader(fr)){
				String line = br.readLine();
				String[] headers = line.split("\t");
				int iGene = -1;
				int iCanonical = -1;
				int ipLI = -1;
				int ipRec = -1;
				int ipNull = -1;
				for (int i=0 ; i < headers.length ; i++) {
					if (headers[i].equals("gene")) {
						iGene = i;
					}else if (headers[i].equals("canonical")) {
						iCanonical = i;
					}else if (headers[i].equals("pLI")) {
						ipLI = i;
					}else if (headers[i].equals("pRec")) {
						ipRec = i;
					}else if (headers[i].equals("pNull")) {
						ipNull = i;
					}
				}
				while ((line = br.readLine()) != null) {
					String[] columns = line.split("\t");
					String gene = columns[iGene];
					boolean canonical = (iCanonical != -1) ? Boolean.parseBoolean(columns[iCanonical]) : true;
					double pLI = (columns[ipLI].equals("NA")) ? -1 : Double.parseDouble(columns[ipLI]);
					double pRec = (columns[ipRec].equals("NA")) ? -1 : Double.parseDouble(columns[ipRec]);
					double pNull = (columns[ipNull].equals("NA")) ? -1 : Double.parseDouble(columns[ipNull]);
					if (canonical) {
						totalGenes++;
						if (synonyms.containsKey(gene)) {
							if (mapPli.get(synonyms.get(gene)) != pLI) {
								badPli++;
								System.out.println(gene + "\t("+synonyms.get(gene)+")" + "\tpLI"  + "\t" + pLI + " != " + mapPli.get(synonyms.get(gene)));
								if (!dryRun) {
									Highlander.getDB().update(new Reference("GRCh38"), Schema.DBNSFP, "UPDATE genes set gnomAD_pLI = '"+pLI+"' WHERE Gene_name = '"+synonyms.get(gene)+"'");
								}
							}
							if (mapPrec.get(synonyms.get(gene)) != pRec) {
								badPrec++;
								System.out.println(gene + "\t("+synonyms.get(gene)+")" + "\tpRec"  + "\t" + pRec + " != " + mapPrec.get(synonyms.get(gene)));
								if (!dryRun) {
									Highlander.getDB().update(new Reference("GRCh38"), Schema.DBNSFP, "UPDATE genes set gnomAD_pRec = '"+pRec+"' WHERE Gene_name = '"+synonyms.get(gene)+"'");
								}
							}
							if (mapPnull.get(synonyms.get(gene)) != pNull) {
								badPnull++;
								System.out.println(gene + "\t("+synonyms.get(gene)+")" + "\tpNull"  + "\t" + pNull + " != " + mapPnull.get(synonyms.get(gene)));
								if (!dryRun) {
									Highlander.getDB().update(new Reference("GRCh38"), Schema.DBNSFP, "UPDATE genes set gnomAD_pNull = '"+pNull+"' WHERE Gene_name = '"+synonyms.get(gene)+"'");
								}
							}
						}else {
							geneNotFound++;
							//System.err.println("Gene '"+gene+"' not found in DBNSFP");
						}
					}
				}
			}
		}
		System.out.println("Summary");
		System.out.println("Genes not found = " + geneNotFound + "/" + totalGenes);
		System.out.println("Corrected pLI = " + badPli + "/" + totalGenes);
		System.out.println("Corrected pRec = " + badPrec + "/" + totalGenes);
		System.out.println("Corrected pNull = " + badPnull + "/" + totalGenes);
	}
	
	public static void main(String[] args) {
		try {
			Highlander.initialize(new Parameters(false, new File("D:\\Dropbox\\Projets\\Highlander\\config\\GEHU admin\\settings.xml")), 5);
			compareGnomadPLI(new File("D:\\Dropbox\\Projets\\Highlander\\tools\\dbNSFP\\PLI\\gnomad.v2.1.1.lof_metrics.by_transcript.txt"), true);
		} catch (Exception ex) {
			Tools.exception(ex);
		}	
		System.exit(0);
	}

}
