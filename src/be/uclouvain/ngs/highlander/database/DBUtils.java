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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Parameters;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
import be.uclouvain.ngs.highlander.datatype.Gene;
import be.uclouvain.ngs.highlander.datatype.Reference;
import be.uclouvain.ngs.highlander.datatype.Variant;
import be.uclouvain.ngs.highlander.datatype.SNPEffect.VariantType;
import be.uclouvain.ngs.highlander.datatype.SNPEffect.Zygosity;

public class DBUtils {

	public enum VariantKind {all, snp, indel}
	public enum VariantNovelty {all, novel, known}

	public static String[] getSimilarPossibleValues(Analysis analysis, Field field, String value, int numberOfResults) throws Exception {
		List<String> results = new ArrayList<>();
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT `value`, levenshtein('"+value+"',`value`) l FROM "+analysis.getFromPossibleValues()+" WHERE `field` = '"+field.getName()+"' AND levenshtein('"+value+"',`value`) < 10 ORDER BY l ASC LIMIT " + numberOfResults)) {
			while (res.next()) {
				results.add(res.getString(1));
			}
		}
		return results.toArray(new String[0]);
	}

	public static int getProjectId(String runLabel, String sample) throws Exception {
		int id = -1;
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
				"SELECT project_id FROM `projects` WHERE `sample` = '"+sample+"' AND `run_label` = '"+runLabel+"'")) {
			if (res.next()){
				id = res.getInt(1);
			}
		}
		if (id != -1) return id;
		throw new Exception("Unknown project/sample : " + runLabel + " / " + sample);
	}

	public static List<String> getEnsemblTranscripts(Reference reference, String ensemblGene) throws Exception {
		List<String> list = new ArrayList<String>();
		try (Results res = Highlander.getDB().select(reference, Schema.ENSEMBL, "SELECT transcript.stable_id FROM gene JOIN transcript USING (gene_id) WHERE gene.stable_id = '"+ensemblGene+"'")) {
			while (res.next()){
				list.add(res.getString(1));
			}
		}
		return list;
	}

	/**
	 * 
	 * @param ensemblGene
	 * @return the Ensembl canonical transcript id or "?" if it does not exist
	 * @throws Exception
	 */
	public static String getEnsemblCanonicalTranscript(Reference reference, String ensemblGene) throws Exception {
		String t = "?";
		try (Results res = Highlander.getDB().select(reference, Schema.ENSEMBL, "SELECT transcript.stable_id FROM gene JOIN transcript ON canonical_transcript_id = transcript_id WHERE gene.stable_id = '"+ensemblGene+"'")) {
			if (res.next()){
				t = res.getString(1);
			}
		}
		return t;
	}

	public static String getEnsemblProtein(Reference reference, String ensemblTranscript) throws Exception {
		String accession = null;
		try (Results res = Highlander.getDB().select(reference, Schema.ENSEMBL, 
				"SELECT translation.stable_id "
						+ "FROM transcript "
						+ "JOIN translation USING (transcript_id) "
						+ "WHERE transcript.stable_id = '"+ensemblTranscript+"'")){
			if (res.next()){
				accession = res.getString(1);
			}
		}
		return accession;
	}

	public static boolean isEnsemblTranscriptCanonical(Reference reference, String ensemblTranscript) throws Exception {
		boolean canonical = false;
		try (Results res = Highlander.getDB().select(reference, Schema.ENSEMBL, "SELECT COUNT(*) FROM gene as g, transcript as t WHERE t.stable_id = '"+ensemblTranscript+"' AND t.transcript_id = g.canonical_transcript_id;")) {
			if (res.next()) {
				if (res.getInt(1) > 0) canonical = true;
			}
		}
		return canonical;
	}

	public static String getAccessionRefSeqMRna(Reference reference, String ensemblTranscript) throws Exception {
		String accession = null;
		try (Results res = Highlander.getDB().select(reference, Schema.ENSEMBL, 
				"SELECT xref.display_label "
						+ "FROM transcript "
						+ "JOIN object_xref ON (transcript_id = ensembl_id) "
						+ "JOIN xref USING (xref_id) "
						+ "LEFT JOIN external_db ed USING (external_db_id) "
						+ "WHERE db_name = 'RefSeq_mRNA' "
						+ "AND ensembl_object_type = 'Transcript' "
						+ "AND transcript.stable_id = '"+ensemblTranscript+"'")){
			if (res.next()){
				accession = res.getString(1);
			}
		}
		return accession;
	}

	public static String getAccessionRefSeqProt(Reference reference, String ensemblTranscript) throws Exception {
		String accession = null;
		try (Results res = Highlander.getDB().select(reference, Schema.ENSEMBL, 
				"SELECT xref.display_label "
						+ "FROM transcript "
						+ "JOIN translation USING (transcript_id) "
						+ "JOIN object_xref ON (translation_id = ensembl_id) "
						+ "JOIN xref USING (xref_id) "
						+ "LEFT JOIN external_db ed USING (external_db_id) "
						+ "WHERE db_name = 'RefSeq_peptide' "
						+ "AND ensembl_object_type = 'Translation' "
						+ "AND transcript.stable_id = '"+ensemblTranscript+"'")){
			while (res.next()){
				if (accession == null || res.getString(1).length() > accession.length()) {
					accession = res.getString(1);
				}
			}
		}
		return accession;
	}

	public static String getAccessionUniprot(Reference reference, String ensemblTranscript) throws Exception {
		String accession = null;
		try (Results res = Highlander.getDB().select(reference, Schema.ENSEMBL, 
				"SELECT xref.dbprimary_acc "
						+ "FROM transcript "
						+ "JOIN translation USING (transcript_id) "
						+ "JOIN object_xref ON (translation_id = ensembl_id) "
						+ "JOIN xref USING (xref_id) "
						+ "LEFT JOIN external_db ed USING (external_db_id) "
						+ "WHERE db_name = 'Uniprot/SWISSPROT' "
						+ "AND ensembl_object_type = 'Translation' "
						+ "AND transcript.stable_id = '"+ensemblTranscript+"'")){
			if (res.next()){
				accession = res.getString(1);
			}
		}
		return accession;
	}

	public static String getGeneSymbol(Reference reference, String ensemblGene) throws Exception {
		String geneSymbol = null;
		try (Results res = Highlander.getDB().select(reference, Schema.ENSEMBL, 
				"SELECT display_label "
						+ "FROM gene as g JOIN xref as x ON (g.display_xref_id = x.xref_id) "
						+ "WHERE stable_id = '"+ensemblGene+"'")){
			if (res.next()){
				geneSymbol = res.getString(1);
			}
		}
		return geneSymbol;
	}

	public static String getBiotype(Reference reference, String ensemblTranscript) throws Exception {
		String biotype = null;
		try (Results res = Highlander.getDB().select(reference, Schema.ENSEMBL, 
				"SELECT biotype "
						+ "FROM transcript "
						+ "WHERE stable_id = '"+ensemblTranscript+"'")){
			if (res.next()) {
				biotype = res.getString("biotype");
			}
		}
		return biotype;
	}
	
	public static String getEnsemblGene(Reference reference, String geneSymbol) throws Exception {
		String symbol = null;
		String ensemblGene = null;
		String chr = null;
		/* Following query works BUT took 0.5sec, because of the OR (have to JOIN more possibilities)
		 * It's a lot faster to separate the OR in 2 queries, and just make a UNION (that even only keep distinct records)
		ResultSet res = Highlander.getDB().select(reference, Schema.ENSEMBL, 
				"SELECT DISTINCT(g.stable_id)"+
				"FROM gene as g "+
				"JOIN xref as x ON (g.display_xref_id = x.xref_id)"+
				"LEFT JOIN external_synonym es USING (xref_id)"+
				"JOIN external_db ed USING (external_db_id)"+
				"WHERE (ed.db_name LIKE 'HGNC%' OR ed.db_name LIKE 'UniProt%') "+
				"AND (x.display_label = '"+geneSymbol+"' OR es.synonym = '"+geneSymbol+"')");
		 */
		try (Results res = Highlander.getDB().select(reference, Schema.ENSEMBL, 
				"SELECT g.stable_id, r.name, display_label "+
						"FROM gene as g "+
						"JOIN xref as x ON (g.display_xref_id = x.xref_id) "+
						"JOIN external_db ed USING (external_db_id) "+
						"LEFT JOIN external_synonym es USING (xref_id) "+
						"JOIN seq_region as r  USING (seq_region_id) "+
						"WHERE es.synonym = '"+geneSymbol+"' "+
						//"AND (ed.db_name LIKE 'HGNC%' OR ed.db_name LIKE 'UniProt%') "+ //Doesn't work for non-human organisms, should work without it
						"UNION "+
						"SELECT g.stable_id, r.name, display_label "+
						"FROM gene as g "+
						"JOIN xref as x ON (g.display_xref_id = x.xref_id) "+
						"JOIN external_db ed USING (external_db_id) "+
						"JOIN seq_region as r  USING (seq_region_id) "+
						"WHERE x.display_label = '"+geneSymbol+"' "
						//+"AND (ed.db_name LIKE 'HGNC%' OR ed.db_name LIKE 'UniProt%')"
						)){	
			while (res.next()){
				if (ensemblGene == null) {
					ensemblGene = res.getString(1);
					chr = res.getString(2);
					symbol = res.getString(3);
				}else {
					List<String> okChromosomes = Arrays.asList(new String[] {"1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20","21","22","X","Y"});
					if (!okChromosomes.contains(chr) && okChromosomes.contains(res.getString(2))) {
						//Sometimes genes are on chr1-22-X-Y and also on other contigs (like PRSS1 present on 7 and HG7_PATCH)
						ensemblGene = res.getString(1);
						chr = res.getString(2);
						symbol = res.getString(3);
					}else if (!symbol.equals(geneSymbol) && symbol.equals(symbol)){
						//Sometimes genes have synonyms that are ALSO canonical names for OTHER genes (si si c'est possible)
						//e.g. NBN has ARTN as synonym but ... ARTN has not NBN as synonym, and they are 2 different genes on different chromosomes
						//e.g. MET / SLTM même combat
						ensemblGene = res.getString(1);
						chr = res.getString(2);
						symbol = res.getString(3);					
					}
				}
			}
		}
		return ensemblGene;
	}

	/**
	 * Return each gene for which the canonical transcript intersects the given variant.
	 * If 2 genes have the same gene symbol (but obviously different ensembl ids), only the most useful is kept (e.g. protein_coding kept vs nonsense_mediated_decay).
	 * 
	 * @param reference
	 * @param variant
	 * @return genes spanning the given position
	 * @throws Exception
	 */
	public static Set<Gene> getGenesWithCanonicalTranscriptIntersect(Reference reference, Variant variant) throws Exception {
		Set<Gene> genes = new HashSet<>();
		try (Results res = Highlander.getDB().select(reference, Schema.ENSEMBL, 
				"SELECT X.display_label as gene_symbol, G.stable_id as ensembl_gene, T.stable_id as ensembl_transcript, T.biotype as biotype " 
						+ "FROM gene as G "
						+ "LEFT JOIN xref as X ON (G.display_xref_id = X.xref_id) "
						+ "JOIN seq_region as R USING (seq_region_id) "
						+ "JOIN coord_system as C USING (coord_system_id) " 
						+ "JOIN transcript as T ON G.canonical_transcript_id = T.transcript_id "
						+ "WHERE C.rank = 1 and R.`name` = '"+variant.getChromosome()+"' "
						+ "AND G.seq_region_start <= "+variant.getPosition()+" AND G.seq_region_end >= " + (variant.getPosition()+variant.getAffectedReferenceLength()) + " " //query optimization: it limits the regions to look at in transcript table, far bigger than gene table. Logically, the gene region is bigger, because it encompass all transcripts.
						+ "AND T.seq_region_start <= "+variant.getPosition()+" AND T.seq_region_end >= " + (variant.getPosition()+variant.getAffectedReferenceLength()))){
			while (res.next()) {
				Gene gene = new Gene(reference, variant.getChromosome(), res.getString("gene_symbol"), res.getString("ensembl_gene"), res.getString("ensembl_transcript"), null, res.getString("biotype"));
				Gene alreadyThere = null;
				for (Gene g : genes) {
					if (g.getGeneSymbol().equals(gene.getGeneSymbol())) {
						alreadyThere = g;
						break;
					}
				}
				if (alreadyThere != null) {
					if (gene.getBiotypePriority() < alreadyThere.getBiotypePriority()) {
						genes.remove(alreadyThere);
						gene.setRefSeqTranscript(DBUtils.getAccessionRefSeqMRna(reference, gene.getEnsemblTranscript()));
						genes.add(gene);
					}
					//else do nothing, alreadyThere has higher priority, we keep it
				}else{
					gene.setRefSeqTranscript(DBUtils.getAccessionRefSeqMRna(reference, gene.getEnsemblTranscript()));
					genes.add(gene);
				}
			}
		}
		return genes;
	}
	
	public static List<String> getAllChromosomes(Reference reference) throws Exception {
		List<String> chromosome = new ArrayList<>();
		try (Results res = Highlander.getDB().select(reference, Schema.ENSEMBL, 
				"SELECT `seq_region`.`name` "
						+ "FROM seq_region "
						+ "JOIN coord_system USING(coord_system_id) " 
						+ "WHERE `rank` = 1 "
				)){	
			while (res.next()){
				chromosome.add(res.getString(1));
			}
		}
		Collections.sort(chromosome, new Tools.NaturalOrderComparator(true));
		return chromosome;
	}

	public static String getChromosome(Reference reference, String ensemblGene) throws Exception {
		String chromosome = null;
		try (Results res = Highlander.getDB().select(reference, Schema.ENSEMBL, 
				"SELECT `name` FROM gene JOIN seq_region USING (seq_region_id) where stable_id = '"+ensemblGene+"'")){	
			if (res.next()){
				chromosome = res.getString(1);
			}
		}
		return chromosome;
	}
	
	public static Map<String,Set<String>> getAlternativeContigs(Reference reference, String ensemblGene) throws Exception {
		Map<String,Set<String>> alt = new TreeMap<String, Set<String>>();
		try (Results res = Highlander.getDB().select(reference, Schema.ENSEMBL, 
				"SELECT exc_type, `name` as chr "
						+ "FROM gene "
						+ "JOIN assembly_exception ON gene.seq_region_id = assembly_exception.exc_seq_region_id "
						+ "JOIN seq_region ON assembly_exception.seq_region_id = seq_region.seq_region_id "
						+ "WHERE stable_id = '"+ensemblGene+"'")){
			while (res.next()){
				String excType = res.getString("exc_type");
				if (!alt.containsKey(excType)) alt.put(excType, new TreeSet<String>());
				alt.get(excType).add(res.getString("chr"));
			}
		}
		return alt;
	}


	/**
	 * Retreive DNA sequence from Ensembl database
	 * 
	 * Ensembl stores "low-level" sequences, contigs. For "top-level" sequences, like chromosomes, we need to use the assembly table.
	 * coord_system_id must be at 2 for human in Ensembl 75 (GRCh37) (chromosomes, other coordinates are possible, see their MySQL description)
	 * coord_system_id must be at 3 for mouse in Ensembl 98
	 * coord_system_id must be at 4 for human in Ensembl 100 (GRCh38) (NB: GRCh37 is present as coord_system_id 12, could maybe be used for liftover -- Sept 2020 not found how ...)
	 * -> coord_system rank seems a better universal approach than coord_system_id (rank 1 seems to always be chromosome level of latest build)
	 * name of the region is the name of the chromosome in our case
	 * For a given interval, we fetch all the regions overlapping this interval, using assembly table.
	 * asm_start/end gives the position in the assembly (i.e. genomic position on the chromosome in this case)
	 * cmp_start_end gives the position in the component, i.e. the contig stored. As contigs can overlap, or only a chunck can be aligned, this allow to have each position only once.
	 * So we cut the sequence between cmp_start and cmp_end, keeping exactly the sequence matching the assembly positions
	 * ori = -1 means the sequence is in reverse, so we need to take the reverse complement
	 * After that we put the sequences together, and fill the gaps with N (most of the time a region ends just before the next begins, but sometimes there is a gap).
	 * We also put N's before and after the sequence if necessary (first region start after our asked start, or last region ends before our asked end)
	 * 
	 * Note that this method fetch the full sequence of overlapping regions.
	 * So if only 1 position/nucleotide is asked and the region overlapping it has a size of 5 000 000 nucleotides, the client will fetch it then cut the 4 999 999 useless nucleotides
	 * 
	 * The SQL query could do this "cut" using SUBSTRING, just send the needed parts. But if the 4 possibilities for forward are easy to compute, it was far more complicated for reverse regions.
	 * 
	 * In the end the performance of this method is quite OK, even 10 times faster than the web call to UCSC DAS, so let's keep it simple :-)
	 * 
	 * @param chr
	 * @param start
	 * @param end
	 * @return
	 * @throws Exception
	 */
	public static String getSequence(Reference reference, String chr, int start, int end) throws Exception {
		StringBuilder sequence = new StringBuilder();
		int contigStart = start;
		int lastEnd = start-1;
		try (Results res = Highlander.getDB().select(reference, Schema.ENSEMBL, 
				"SELECT ori, SUBSTRING(sequence,cmp_start,(cmp_end-cmp_start+1)) as seq, asm_start, asm_end "
						+ "FROM seq_region as R "
						+ "JOIN coord_system as C ON R.coord_system_id = C.coord_system_id " 
						+ "JOIN assembly as A ON R.seq_region_id = A.asm_seq_region_id " 
						+ "JOIN dna as D ON D.seq_region_id = A.cmp_seq_region_id "
						+ "WHERE C.rank = 1 and R.`name` = '"+chr+"' "
						+ "AND (asm_start <= "+start+" AND asm_end >= "+start+" OR asm_start <= "+end+" AND asm_end >= "+end+" OR asm_start >= "+start+" AND asm_end <= "+end+") "
						+ "ORDER BY asm_start"
				)){	
			while (res.next()){
				boolean fwd = (res.getInt("ori") == 1);
				String seq = res.getString("seq");
				int asm_start = res.getInt("asm_start");
				int asm_end = res.getInt("asm_end");
				if (!fwd) {
					seq = Tools.reverseComplement(seq);
				}
				if (contigStart == start	) {
					contigStart = Math.min(asm_start, start);
				}
				for (int i=lastEnd ; i < asm_start-1 ; i++) {
					sequence.append("N");
				}
				sequence.append(seq);
				lastEnd = asm_end;
			}
		}
		for (int i=lastEnd ; i < end ; i++) {
			sequence.append("N");
		}
		return sequence.subSequence(start-contigStart, end-contigStart+1).toString();
	}

	public static List<Variant> convertProteinToGenomic(Reference reference, String geneSymbol, int proteinPosition, String refAA, String altAA) throws Exception {
		String ensemblGene = getEnsemblGene(reference, geneSymbol);
		if (ensemblGene != null){
			return convertEnsemblProteinToGenomic(reference, ensemblGene, proteinPosition, refAA, altAA);
		}else{
			throw new Exception("Gene symbol '"+geneSymbol+"' not found in Enseml Mart");
		}
	}

	public static List<Variant> convertEnsemblProteinToGenomic(Reference reference, String ensemblGene, int proteinPosition, String refAA,  String altAA) throws Exception {
		List<Variant> variants = new ArrayList<>();
		String transcript = getEnsemblCanonicalTranscript(reference, ensemblGene);
		if (transcript.equals("?")){
			throw new Exception("Canonical transcript not found for gene '"+ensemblGene+"'");
		}else{
			String protein = getEnsemblProtein(reference, transcript);
			if (protein != null){
				String genome = "";
				if (reference.getName().toLowerCase().startsWith("grc")) {
					genome = reference.getName().toLowerCase();
				}else {
					genome = "grc" + reference.getSchemaName(Schema.ENSEMBL).charAt(0) + reference.getSchemaName(Schema.ENSEMBL).split("_")[reference.getSchemaName(Schema.ENSEMBL).split("_").length-1]; 
				}
				String server = "http://"+genome+".rest.ensembl.org";	
				String ext = "/map/translation/"+protein+"/"+proteinPosition+"-"+proteinPosition+"?";
				URL url = new URL(server + ext);

				Proxy proxy = Proxy.NO_PROXY;
				boolean bypass = false;
				if (System.getProperty("http.nonProxyHosts") != null) {
					for (String host : System.getProperty("http.nonProxyHosts").split("\\|")) {
						if (url.toString().toLowerCase().contains(host.toLowerCase())) bypass = true;
					}
				}
				if (!bypass && System.getProperty("http.proxyHost") != null && System.getProperty("http.proxyPort") != null) {
					proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(System.getProperty("http.proxyHost"), Integer.parseInt(System.getProperty("http.proxyPort"))));
				}
				URLConnection connection = url.openConnection(proxy);
				HttpURLConnection httpConnection = (HttpURLConnection)connection;

				httpConnection.setRequestProperty("Content-Type", "application/json");


				InputStream response = connection.getInputStream();
				int responseCode = httpConnection.getResponseCode();

				if(responseCode != 200) {
					throw new RuntimeException("Response code was not 200. Detected response was "+responseCode);
				}

				String output;
				try (Reader reader = new BufferedReader(new InputStreamReader(response, "UTF-8"))){
					StringBuilder builder = new StringBuilder();
					char[] buffer = new char[8192];
					int read;
					while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
						builder.append(buffer, 0, read);
					}
					output = builder.toString();
				} 					
				JsonObject json = Json.parse(output).asObject();
				JsonArray mappings = json.get("mappings").asArray();
				for (JsonValue mapping : mappings){
					JsonObject m = mapping.asObject();
					String chr = m.getString("seq_region_name","?");
					int start = m.getInt("start",0);
					int end = m.getInt("end",0);
					int strand = m.getInt("strand",-1);
					String seq = getSequence(reference, chr, start, end);
					if (strand < 0){
						seq = Tools.reverseComplement(seq);
					}
					char ref = refAA.toUpperCase().charAt(0);
					char alt = altAA.toUpperCase().charAt(0);
					if (Tools.nucleotidesToProtein(seq) == ref){
						for (int i=0 ; i < seq.length() ; i++){
							for (char base : new char[]{'A','C','G','T'}){
								if (base != seq.charAt(i)){
									char[] s = seq.toCharArray();
									s[i] = base;
									char newAlt = Tools.nucleotidesToProtein(new String(s));
									if (newAlt == alt || alt == 'X'){
										int pos = (strand > 0) ? start+i : end-i;
										String r = seq.charAt(i)+"";		    						
										String a = base+"";
										if (strand < 0){
											r = Tools.reverseComplement(r);
											a = Tools.reverseComplement(a);
										}
										variants.add(new Variant(chr, pos, r, a));
									}
								}
							}
						}
					}else{
						throw new Exception("Reference sequence '"+seq+"' correspond to amino acid '"+Tools.nucleotidesToProtein(seq)+"' and not '"+refAA+"'");
					}			    
				}
				return variants;
			}else{
				throw new Exception("Protein id not found for transcript '"+transcript+"'");
			}
		}
	}

	public static void transferUserAnnotationsEvaluations(AnalysisFull from, AnalysisFull to, int projectId) throws Exception {
		transferUserAnnotationsEvaluations(from, to, projectId, true);
	}
	
	public static void transferUserAnnotationsEvaluations(AnalysisFull from, AnalysisFull to, int projectId, boolean rebuildUserAnnotationsNumEvaluations) throws Exception {
		List<String> columns = new ArrayList<String>();
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "DESCRIBE "+ to.getFromUserAnnotationsEvaluations())) {
			while (res.next()){
				columns.add(Highlander.getDB().getDescribeColumnName(Schema.HIGHLANDER, res));
			}
		}
		columns.remove("id");
		if (from.getReference().equals(to.getReference())) {
			StringBuilder query = new StringBuilder();
			query.append("INSERT IGNORE INTO "+to.getFromUserAnnotationsEvaluations() + "(");
			for (int i = 0 ; i < columns.size() ; i++) query.append("`"+columns.get(i) + "`, ");
			query.deleteCharAt(query.length()-1);
			query.deleteCharAt(query.length()-1);
			query.append(") SELECT ");
			for (int i = 0 ; i < columns.size() ; i++) query.append("`"+columns.get(i) + "`, ");
			query.deleteCharAt(query.length()-1);
			query.deleteCharAt(query.length()-1);
			query.append(" FROM "+from.getFromUserAnnotationsEvaluations());
			query.append("WHERE `project_id` = "+projectId);
			System.out.println(query.toString());
			Highlander.getDB().update(Schema.HIGHLANDER, query.toString());
			if (rebuildUserAnnotationsNumEvaluations) rebuildUserAnnotationsNumEvaluations(to);
		}else {
			//References are different, we'll have to do a liftover
			System.out.println("Performing liftover for each variant from "+from.getReference()+" to "+to.getReference());
			if (from.getReference().getSpecies().equals(to.getReference().getSpecies())) {
				try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT * FROM "+from.getFromUserAnnotationsEvaluations() + "WHERE `project_id` = "+projectId)){
					while (res.next()) {
						Variant varToLift = new Variant(res.getString(Field.chr.getName()), res.getInt(Field.pos.getName()), res.getInt(Field.length.getName()), res.getString(Field.reference.getName()), res.getString(Field.alternative.getName())); 
						Variant variant = varToLift.liftOver(from.getReference(), to.getReference());
						if (variant != null) {
							List<String> values = new ArrayList<String>();
							for (int i = 0 ; i < columns.size() ; i++) {
								if(columns.get(i).equals(Field.chr.getName())){
									values.add(variant.getChromosome());
								}else if(columns.get(i).equals(Field.pos.getName())){
									values.add(""+variant.getPosition());
								}else if(columns.get(i).equals(Field.length.getName())){
									values.add(""+variant.getLength());
								}else if(columns.get(i).equals(Field.reference.getName())){
									values.add(variant.getReference());
								}else if(columns.get(i).equals(Field.alternative.getName())){
									values.add(variant.getAlternative());
								}else {
									values.add(res.getString(columns.get(i)));
								}
							}
							StringBuilder query = new StringBuilder();
							query.append("INSERT IGNORE INTO "+to.getFromUserAnnotationsEvaluations() + "(");
							for (int i = 0 ; i < columns.size() ; i++) query.append("`"+columns.get(i) + "`, ");
							query.deleteCharAt(query.length()-1);
							query.deleteCharAt(query.length()-1);
							query.append(") VALUES (");
							for (int i = 0 ; i < values.size() ; i++) query.append(((values.get(i) != null) ? "'" + Highlander.getDB().format(Schema.HIGHLANDER, values.get(i)) + "', " : "NULL, "));
							query.deleteCharAt(query.length()-1);
							query.deleteCharAt(query.length()-1);
							query.append(")");
							Highlander.getDB().update(Schema.HIGHLANDER, query.toString());
						}else {
							System.out.println("WARNING -- Variant at " + varToLift + " from " + from.getReference() + " does not exists in " + to.getReference());
						}
					}
				}
				if (rebuildUserAnnotationsNumEvaluations) rebuildUserAnnotationsNumEvaluations(to);
			}else {
				throw new Exception("Cannot transfer annotations between different species ("+from.getReference().getSpecies()+" to "+to.getReference().getSpecies()+")");
			}
		}
	}

	public static void transferUserAnnotationsVariants(AnalysisFull from, AnalysisFull to) throws Exception {
		List<String> columns = new ArrayList<String>();
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "DESCRIBE "+ to.getFromUserAnnotationsVariants())) {
			while (res.next()){
				columns.add(Highlander.getDB().getDescribeColumnName(Schema.HIGHLANDER, res));
			}
		}
		columns.remove("id");
		if (from.getReference().equals(to.getReference())) {
			StringBuilder query = new StringBuilder();
			query.append("INSERT IGNORE INTO "+to.getFromUserAnnotationsVariants() + "(");
			for (int i = 0 ; i < columns.size() ; i++) query.append("`"+columns.get(i) + "`, ");
			query.deleteCharAt(query.length()-1);
			query.deleteCharAt(query.length()-1);
			query.append(") SELECT ");
			for (int i = 0 ; i < columns.size() ; i++) query.append("`"+columns.get(i) + "`, ");
			query.deleteCharAt(query.length()-1);
			query.deleteCharAt(query.length()-1);
			query.append(" FROM "+from.getFromUserAnnotationsVariants());
			System.out.println(query.toString());
			Highlander.getDB().update(Schema.HIGHLANDER, query.toString());
		}else {
			//References are different, we'll have to do a liftover
			System.out.println("Performing liftover for each variant from "+from.getReference()+" to "+to.getReference());
			if (from.getReference().getSpecies().equals(to.getReference().getSpecies())) {
				try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT * FROM "+from.getFromUserAnnotationsVariants())){
					while (res.next()) {
						Variant varToLift = new Variant(res.getString(Field.chr.getName()), res.getInt(Field.pos.getName()), res.getInt(Field.length.getName()), res.getString(Field.reference.getName()), res.getString(Field.alternative.getName())); 
						Variant variant = varToLift.liftOver(from.getReference(), to.getReference());
						if (variant != null) {
							List<String> values = new ArrayList<String>();
							for (int i = 0 ; i < columns.size() ; i++) {
								if(columns.get(i).equals(Field.chr.getName())){
									values.add(variant.getChromosome());
								}else if(columns.get(i).equals(Field.pos.getName())){
									values.add(""+variant.getPosition());
								}else if(columns.get(i).equals(Field.length.getName())){
									values.add(""+variant.getLength());
								}else if(columns.get(i).equals(Field.reference.getName())){
									values.add(variant.getReference());
								}else if(columns.get(i).equals(Field.alternative.getName())){
									values.add(variant.getAlternative());
								}else {
									values.add(res.getString(columns.get(i)));
								}
							}
							StringBuilder query = new StringBuilder();
							query.append("INSERT IGNORE INTO "+to.getFromUserAnnotationsVariants() + "(");
							for (int i = 0 ; i < columns.size() ; i++) query.append("`"+columns.get(i) + "`, ");
							query.deleteCharAt(query.length()-1);
							query.deleteCharAt(query.length()-1);
							query.append(") VALUES (");
							for (int i = 0 ; i < values.size() ; i++) query.append(((values.get(i) != null) ? "'" + Highlander.getDB().format(Schema.HIGHLANDER, values.get(i)) + "', " : "NULL, "));
							query.deleteCharAt(query.length()-1);
							query.deleteCharAt(query.length()-1);
							query.append(")");
							Highlander.getDB().update(Schema.HIGHLANDER, query.toString());
						}else {
							System.out.println("WARNING -- Variant at " + varToLift + " from " + from.getReference() + " does not exists in " + to.getReference());
						}
					}
				}
			}else {
				throw new Exception("Cannot transfer annotations between different species ("+from.getReference().getSpecies()+" to "+to.getReference().getSpecies()+")");
			}
		}
	}
	
	public static void transferUserAnnotationsGenes(Analysis from, Analysis to) throws Exception {
		List<String> columns = new ArrayList<String>();
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "DESCRIBE "+ to.getFromUserAnnotationsGenes())) {
			while (res.next()){
				columns.add(Highlander.getDB().getDescribeColumnName(Schema.HIGHLANDER, res));
			}
		}
		columns.remove("id");
		StringBuilder query = new StringBuilder();
		query.append("INSERT IGNORE INTO "+to.getFromUserAnnotationsGenes() + "(");
		for (int i = 0 ; i < columns.size() ; i++) query.append("`"+columns.get(i) + "`, ");
		query.deleteCharAt(query.length()-1);
		query.deleteCharAt(query.length()-1);
		query.append(") SELECT ");
		for (int i = 0 ; i < columns.size() ; i++) query.append("`"+columns.get(i) + "`, ");
		query.deleteCharAt(query.length()-1);
		query.deleteCharAt(query.length()-1);
		query.append(" FROM "+from.getFromUserAnnotationsGenes());
		System.out.println(query.toString());
		Highlander.getDB().update(Schema.HIGHLANDER, query.toString());
	}
	
	public static void transferUserAnnotationsSamples(Analysis from, Analysis to) throws Exception {
		List<String> columns = new ArrayList<String>();
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "DESCRIBE "+ to.getFromUserAnnotationsSamples())) {
			while (res.next()){
				columns.add(Highlander.getDB().getDescribeColumnName(Schema.HIGHLANDER, res));
			}
		}
		columns.remove("id");
		StringBuilder query = new StringBuilder();
		query.append("INSERT IGNORE INTO "+to.getFromUserAnnotationsSamples() + "(");
		for (int i = 0 ; i < columns.size() ; i++) query.append("`"+columns.get(i) + "`, ");
		query.deleteCharAt(query.length()-1);
		query.deleteCharAt(query.length()-1);
		query.append(") SELECT ");
		for (int i = 0 ; i < columns.size() ; i++) query.append("`"+columns.get(i) + "`, ");
		query.deleteCharAt(query.length()-1);
		query.deleteCharAt(query.length()-1);
		query.append(" FROM "+from.getFromUserAnnotationsSamples());
		System.out.println(query.toString());
		Highlander.getDB().update(Schema.HIGHLANDER, query.toString());
	}
	
	/**
	 * Reset the table [analysis]_user_annotations_num_evaluations, and recompute all values from scratch
	 * Normaly the table is updated at each new evaluation entered by any user,	
	 * but if anything happens this method can reconstruct the table. 
	 *  
	 * @param analysis
	 * @throws Exception
	 */
	public static void rebuildUserAnnotationsNumEvaluations(Analysis analysis) throws Exception {
		Highlander.getDB().update(Schema.HIGHLANDER, "DELETE FROM "+analysis.getFromUserAnnotationsNumEvaluations());
		Highlander.getDB().update(Schema.HIGHLANDER, 
				"INSERT IGNORE INTO `"+analysis.getTableUserAnnotationsNumEvaluations()+"` "
						+ "(`chr`, `pos`, `length`, `reference`, `alternative`, `gene_symbol`, `num_evaluated_as_type_1`, `num_evaluated_as_type_2`, `num_evaluated_as_type_3`, `num_evaluated_as_type_4`, `num_evaluated_as_type_5`) "
						+ "SELECT `chr`, `pos`, `length`, `reference`, `alternative`, `gene_symbol`, "
						+ "SUM(IF(`evaluation` = 1, 1, 0)) as `num_evaluated_as_type_1`, "
						+ "SUM(IF(`evaluation` = 2, 1, 0)) as `num_evaluated_as_type_2`, "
						+ "SUM(IF(`evaluation` = 3, 1, 0)) as `num_evaluated_as_type_3`, "
						+ "SUM(IF(`evaluation` = 4, 1, 0)) as `num_evaluated_as_type_4`, "
						+ "SUM(IF(`evaluation` = 5, 1, 0)) as `num_evaluated_as_type_5`"
						+ "FROM "+analysis.getFromUserAnnotationsEvaluations()
						+ "WHERE evaluation > 0 "
						+ "GROUP BY `chr`, `pos`, `reference`, `length`, `alternative`, `gene_symbol`");
	}
	
	public static double getGeneCoverageRatioChrXY(Reference reference, int projectId) throws Exception {
		Set<String> xgenes = new HashSet<String>();
		Set<String> ygenes = new HashSet<String>();
		try (Results res = Highlander.getDB().select(reference, Schema.ENSEMBL, 
				"SELECT `name` as chrom, display_label as genesymbol "
				+ "FROM gene "
				+ "JOIN seq_region USING (seq_region_id) "
				+ "JOIN xref ON (display_xref_id = xref_id) "
				+ "WHERE `name` IN ('X','Y')")) {
			while (res.next()){
				String gene = res.getString("genesymbol");
				if (res.getString("chrom").equals("X")){
					if (ygenes.contains(gene)) {
						ygenes.remove(gene);
					}else {
						xgenes.add(gene);											
					}
				}else{
					if (xgenes.contains(gene)) {
						xgenes.remove(gene);
					}else {
						ygenes.add(gene);											
					}
				}
			}
		}
		double x = 0.0;
		double y = 0.0;
		if (!xgenes.isEmpty() && !ygenes.isEmpty()){
			for (Analysis analysis : AnalysisFull.getAvailableAnalyses()){
				try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
						"SELECT gene_symbol, sum(mean_depth) as cv "
						+ "FROM " + analysis.getFromCoverageRegions()
						+ analysis.getJoinCoverage()
						+ "WHERE gene_symbol IN ("+HighlanderDatabase.makeSqlList(xgenes, String.class)+","+HighlanderDatabase.makeSqlList(ygenes, String.class)+") "
						+ "AND project_id = " + projectId + " "
						+ "GROUP BY gene_symbol")
						){		
					while (res.next()){
						if (xgenes.contains(res.getString("gene_symbol"))){
							x += res.getDouble("cv");
						}else{
							y += res.getDouble("cv");
						}
					}
				}
			}
		}
		if (y != 0.0){
			return x/y;
		}else{
			return 0.0;
		}
	}

	public static double getTiTvRatio(int projectId, Analysis analysis, VariantNovelty variantNovelty) throws Exception {
		String noveltyWhere;
		if (variantNovelty == VariantNovelty.known) noveltyWhere = "AND dbsnp_id IS NOT NULL ";
		else if (variantNovelty == VariantNovelty.novel) noveltyWhere = "AND dbsnp_id IS NULL ";
		else noveltyWhere = "";
		double ti = 0.0;
		double tv = 0.0;
		//First GROUP BY is necessary to avoid counting multiple times variants spanning multiple genes
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
				"SELECT tr, count(*) as ct FROM ("
						+ "SELECT concat(min(reference),\"-\",min(alternative)) as tr "
						+ "FROM "+analysis.getFromSampleAnnotations()
						+ analysis.getJoinStaticAnnotations()
						+ "WHERE "
						+ "project_id = "+projectId + " "
						+ noveltyWhere
						+ "AND filters = 'PASS' "
						+ "GROUP BY `pos`,`chr`,`alternative`,`reference`,`length`"
						+ ") as tmp GROUP BY tr"
				)) {	
			while (res.next()){
				String tr = res.getString("tr");
				switch(tr){
				case "A-G":
				case "G-A":
				case "C-T":
				case "T-C":
					ti += res.getDouble("ct");
					break;
				case "A-C":
				case "A-T":
				case "C-A":
				case "C-G":
				case "G-C":
				case "G-T":
				case "T-A":
				case "T-G":
					tv += res.getDouble("ct");
					break;
				default:
					//Don't count INDELs, MNVs and SVs
					break;
				}
			}
		}
		return ti / Math.max(tv,0.0001);
	}

	public static double getHetHomRatio(int projectId, Analysis analysis, VariantNovelty variantNovelty) throws Exception {
		String noveltyWhere;
		if (variantNovelty == VariantNovelty.known) noveltyWhere = "AND dbsnp_id IS NOT NULL ";
		else if (variantNovelty == VariantNovelty.novel) noveltyWhere = "AND dbsnp_id IS NULL ";
		else noveltyWhere = "";
		double het = 1.0;
		double hom = 1.0;
		//First GROUP BY is necessary to avoid counting multiple times variants spanning multiple genes
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
				"SELECT zygosity, count(*) as ct FROM ("
						+ "SELECT min(zygosity) as zygosity "
						+ "FROM "+analysis.getFromSampleAnnotations()
						+ analysis.getJoinStaticAnnotations()
						+ "WHERE "
						+ "project_id = "+projectId + " "
						+ noveltyWhere
						+ "GROUP BY `pos`,`chr`,`alternative`,`reference`,`length`"
						+ ") as tmp GROUP BY zygosity"
				)) {	
			while (res.next()){
				String zyg = res.getString("zygosity");
				if (zyg != null){
					if (Zygosity.valueOf(zyg) == Zygosity.Heterozygous){
						het = res.getDouble("ct"); 
					}else{
						hom = res.getDouble("ct");
					}
				}
			}
		}
		return het / Math.max(hom,1.0);
	}

	/**
	 * Count the number of unique variants in a given sample.
	 * If a variant span multiple genes, it's only counted once.
	 * 
	 * @param projectId
	 * @param analysis
	 * @param variantKind
	 * @param passFilters
	 * @param variantNovelty
	 * @return
	 * @throws Exception
	 */
	public static int getVariantCount(int projectId, Analysis analysis, VariantKind variantKind, boolean passFilters, VariantNovelty variantNovelty) throws Exception {
		String typeWhere;
		if (variantKind == VariantKind.snp) typeWhere = "AND "+Field.variant_type.getQueryWhereName(analysis, false)+" IN ('"+VariantType.SNV+"','"+VariantType.MNV+"') ";
		else if (variantKind == VariantKind.indel) typeWhere = "AND "+Field.variant_type.getQueryWhereName(analysis, false)+" IN ('"+VariantType.INS+"','"+VariantType.DEL+"') ";
		else typeWhere = ""; //no need to separate SV, they already should be in a different analysis
		String noveltyWhere;
		if (variantNovelty == VariantNovelty.known) noveltyWhere = "AND "+Field.dbsnp_id.getQueryWhereName(analysis, false)+" IS NOT NULL ";
		else if (variantNovelty == VariantNovelty.novel) noveltyWhere = "AND "+Field.dbsnp_id.getQueryWhereName(analysis, false)+" IS NULL ";
		else noveltyWhere = "";
		String passWhere = "";
		if (passFilters) passWhere = "AND filters = 'PASS' ";
		int count = 0;
		//First GROUP BY is necessary to avoid counting multiple times variants spanning multiple genes
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
				"SELECT COUNT(*) FROM ("
						+ "SELECT pos "
						+ "FROM "+analysis.getFromSampleAnnotations()
						+ analysis.getJoinStaticAnnotations()
						+ "WHERE project_id = "+projectId + " "
						+ typeWhere
						+ noveltyWhere
						+ passWhere
						+ "GROUP BY `pos`,`chr`,`alternative`,`reference`,`length`"
						+ ") as tmp"
				)) {
			if (res.next()){
				count = res.getInt(1);					
			} 
		}
		return count;
	}

	public static void main(String[] args){
		try{
			Highlander.initialize(new Parameters(false),5);
			
			for (Gene g : getGenesWithCanonicalTranscriptIntersect(new Reference("GRCh38"), new Variant("3",179267053))) {
				System.out.println(g.getGeneSymbol());				
			}
			
			//Test if we got same sequence than UCSC for a sampling through the whole genome (start of Y seems to be N's in Ensembl and sequence in UCSC)
			/*
			Reference genome = new Reference("GRCh37"); 
			try (Results res = DB.select(genome, Schema.ENSEMBL,  "SELECT * FROM homo_sapiens_core_75_37.seq_region WHERE coord_system_id = 2")) {
				while(res.next()) {
					String chr = res.getString("name");
					int length = res.getInt("length");
					for (int chunk=1; chunk < length ; chunk+=length/5) {
						int end = Math.min(chunk+1_000_000, length);
						System.out.println("---------");
						System.out.println("chr"+chr+":"+chunk +"-"+end);
						long timeStart = System.currentTimeMillis(); 
						String ens = DBUtils.getSequence(genome, chr, chunk, end);
						long highlanderTime = System.currentTimeMillis()- timeStart;
						timeStart = System.currentTimeMillis();
						String ucsc = Tools.getReferenceSequenceUCSC("hg19", chr, chunk, end);
						long ucscTime = System.currentTimeMillis()- timeStart;
						timeStart = System.currentTimeMillis();
						if (ens.equals(ucsc)) System.out.println("EQUAL");
						else System.err.println("DIFFERENT");;
						System.out.println("Time = " + Tools.longToString(highlanderTime) + " for Highlander vs " + Tools.longToString(ucscTime) + " for UCSC");
						if (!ens.equals(ucsc)) {
							System.out.println("chr"+chr+":"+chunk +"-"+end+ " is "+ens.equals(ucsc)+" with length " + Tools.intToString(ens.length()) + "?" + Tools.intToString(ucsc.length()));
							System.out.println(ens.substring(0,Math.min(200, ens.length())));
							System.out.println(ucsc.substring(0,Math.min(200, ens.length())));
							for (int j=0 ; j < ens.length() ; j++) {
								if (ens.charAt(j) != ucsc.charAt(j)) {
									System.out.println(j + "\t" + ens.charAt(j) + "\t" + ucsc.charAt(j));
								}
							}
						}
						System.out.println("---------");

					}
				}
			}
			*/
			
			/* 
			//tests convertProteinToGenomic
			for (Variant v : DBu.convertProteinToGenomic("TEK", 897, "Y", "N")){
				System.out.println(v + " " + v.getReference() + ">" + v.getAlternative());
			}
			for (Variant v : DBu.convertProteinToGenomic("GNAQ", 183, "R", "X")){
				System.out.println(v + " " + v.getReference() + ">" + v.getAlternative());
			}
			 */
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
}
