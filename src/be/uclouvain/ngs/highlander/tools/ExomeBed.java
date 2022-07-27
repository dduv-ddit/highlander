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

package be.uclouvain.ngs.highlander.tools;

import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Parameters;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.administration.DbBuilder;
import be.uclouvain.ngs.highlander.database.DBUtils;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.DBMS;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.Gene;
import be.uclouvain.ngs.highlander.datatype.Reference;

public class ExomeBed {

	/*
	 * Some genes
	 * 
	 * TEK (+)
	 * GLMN (- et exon 1 entièrement UTR)
	 * DST (- et 84 exons)
	 * DSCAM (-)
	 * SOX18 (only 1 exon)
	 * IL9R, VAMP7 (sur X et Y dans la région PAR)
	 * SRY (uniquement Y)
	 * 
	 * Synonyms problems from Rachel Galot
	 * 
	 * NBN - ARTN --> both are synonyms AND canonical symbol (NBN is a synonym for ARTN but ARTN is NOT a synonym for NBN ...)
	 * MET - SLTM --> same
	 * 
	 * Synonyms from Laurence Desmyter
	 *  
	 * KLHL40 - KBTBD5 (found in old UCSC file used for coverage only) --> Ensembl synonym solved it
	 * KLHL41 - KBTBD10 (idem)
	 * 
	 * POMGNT2 (Highlander & HGNC --> Ensemlbl) - GTDC2 (IGV & coverage --> UCSC) --> Ensembl synonym solved it 
	 * POMK - SGK196 (idem)
	 *  
	 * HACD1 (HGNC) - PTPLA (IGV/Highlander/Coverage) --> not found in Ens75 synonyms (to check with more recent version)
	 * SELENON - SEPN1 (idem)
	 * WASHC5 - KIAA0196 (idem)
	 * ELP1 - IKBKAP (idem but Ensembl synonym solved it )
	 * 
	 * MAP3K20 (HGNC) - ZAK (IGV/Coverage) - MLTK (Highlander) --> Ensembl synonym solved ZAK (UniProt synonym, but not MAP3K20 in Ens75. to check with more recent version)
	 * 
	 * CTD-2168K21.2 (Highlander) - NEFL (HGNC/IGV/Coverage) --> ENSG different between Ensembl 75 & 97 (NEFL probably a new symbol, to check with more recent version) 
	 * 
	 */

	public static class Region {
		String name;
		String chr;
		int start;
		int end;
		String gene_symbol;
		String ensembl_gene;
		String ensembl_transcript;
		int exon;
		int intron;
		boolean utr;
		boolean coding;
		
		public Region(String name, String chr, int start, int end, String gene_symbol, String ensembl_gene, String ensembl_transcript, int exon, int intron, boolean utr, boolean coding) {
			this.name = name;
			this.chr = chr;
			this.start = start;
			this.end = end;
			this.gene_symbol = gene_symbol;
			this.ensembl_gene = ensembl_gene;
			this.ensembl_transcript = ensembl_transcript;
			this.exon = exon;
			this.intron = intron;
			this.utr = utr;
			this.coding = coding;
		}
		
		public String getBedString(boolean addChr, boolean addEnsG, boolean addEnsT) {
			return ((addChr)?"chr":"") + chr + "\t" + start + "\t" + end + "\t" + gene_symbol + "\t" + "exon " + exon + (utr?" (UTR)":"") + ((addEnsG) ? "\t" + ensembl_gene : "") + ((addEnsT) ? "\t" + ensembl_transcript : "") + "\n";
		}
		
		public String getSQLString(DBMS dbms) {
			String nullStr = HighlanderDatabase.getNullString(dbms);
			StringBuilder sb = new StringBuilder();
			sb.append(((name != null)?HighlanderDatabase.format(dbms, Schema.HIGHLANDER, name):"")+"\t");
			sb.append(((chr != null)?HighlanderDatabase.format(dbms, Schema.HIGHLANDER, chr):"")+"\t");
			sb.append((start)+"\t");
			sb.append((end)+"\t");
			sb.append(((gene_symbol != null)?HighlanderDatabase.format(dbms, Schema.HIGHLANDER, gene_symbol):"")+"\t");
			sb.append(((exon > 0)?exon:nullStr)+"\t");
			sb.append(((intron > 0)?intron:nullStr)+"\t");
			if (dbms == DBMS.hsqldb) sb.append(((utr)?"true":"false")+"\t");
			else sb.append(((utr)?"1":"0")+"\t");
			if (dbms == DBMS.hsqldb) sb.append(((coding)?"true":"false")+"\t");
			else sb.append(((coding)?"1":"0")+"\t");
			sb.append("\n");
			return sb.toString();
		}
		
		public static String getInsertionColumnsString() {
			StringBuilder out  = new StringBuilder();
			out.append("region_name" + ", ");
			out.append("chr" + ", ");
			out.append("start" + ", ");
			out.append("end" + ", ");
			out.append("gene_symbol" + ", ");
			out.append("exon" + ", ");
			out.append("intron" + ", ");
			out.append("utr" + ", ");
			out.append("coding");
			return out.toString();
		}
	}
	
	/**
	 * Fetch all Ensembl genes of the given reference, separated by chromosome.
	 * 
	 * @param referenceGenome reference genome will determine which Ensembl database is used
	 * @param includeNonChrContigs set to false to only fetch contigs that are selected in the given reference (e.g. for human generally chromosome 1-22,X,Y). Set to true if you want to include all existing contigs in Ensembl. 
	 * @return
	 * @throws Exception
	 */
	public static Map<String,Set<String>> getEnsemblGenesPerChr(Reference referenceGenome, boolean includeNonChrContigs) throws Exception {
		HighlanderDatabase DB = Highlander.getDB();
		String query = (includeNonChrContigs) ? "SELECT `name`, stable_id FROM gene JOIN seq_region USING (seq_region_id)" 
				: "SELECT `name`, stable_id FROM gene JOIN seq_region USING (seq_region_id) WHERE `name` IN ("+HighlanderDatabase.makeSqlList(referenceGenome.getChromosomes(), String.class)+")";
		Map<String,Set<String>> genes = new TreeMap<>(new Tools.NaturalOrderComparator(true));
		try (Results res = DB.select(referenceGenome, Schema.ENSEMBL, query)) {
			while(res.next()){
				String chr = res.getString("name");
				if (!genes.containsKey(chr)) {
					genes.put(chr, new TreeSet<String>());
				}
				genes.get(chr).add(res.getString("stable_id"));
			}
		}
		return genes;
	}

	/**
	 * Generate a bed file or populate the Highlander [analysis]_coverage_regions table depending on given parameters.
	 * Regions are fetch from Ensembl, using the canonical transcript of given genes.
	 * output and analysis argument can both be given to produce a bed file and populate the database at the same time.
	 * This method will fetch all genes from the used Ensembl database.
	 * 
	 * @param referenceGenome reference genome will determine which Ensembl database is used
	 * @param includeNonChrContigs set to false to only fetch contigs that are selected in the given reference (e.g. for human generally chromosome 1-22,X,Y). Set to true if you want to include all existing contigs in Ensembl. 
	 * @param biotypeToIncludeUTR Ensembl biotypes to include, e.g. protein_coding. Genes with biotype that don't match, won't be included. Boolean value determines if UTR must be included for that biotype.
	 * @param includeCDS include CoDing Sequence (region of a cDNA which is translated) or not.
	 * @param includeUTR include UnTranslated Region or not (if set to false, can be set for each biotype separately)
	 * @param addChr add 'chr' in front of chromosome names (e.g. for UCSC reference).
	 * @param output bed file to output. Set to null if you want to populate Highlander database.
	 * @param analysis the Highlander [analysis]_coverage_regions table to populate. Set to null if you want to produce a bed file.
	 * @throws Exception
	 */
	public static void generateBed(Reference referenceGenome, boolean includeNonChrContigs, Map<String, Boolean> biotypeToIncludeUTR, boolean includeCDS, boolean includeUTR, boolean addChr, boolean addEnsG, boolean addEnsT, File output, Analysis analysis) throws Exception {
		generateBed(referenceGenome, getEnsemblGenesPerChr(referenceGenome, includeNonChrContigs), biotypeToIncludeUTR, includeCDS, includeUTR, addChr, addEnsG, addEnsT, output, analysis);
	}

	/**
	 * Generate a bed file or populate the Highlander [analysis]_coverage_regions table depending on given parameters.
	 * Regions are fetch from Ensembl, using the canonical transcript of given genes.
	 * output and analysis argument can both be given to produce a bed file and populate the database at the same time.
	 * This method take HGNC gene symbols and will convert them to Ensembl gene ids.
	 * 
	 * @param referenceGenome reference genome will determine which Ensembl database is used
	 * @param geneSymbols a set of HGNC gene symbols, that will be converted to Ensembl gene ids.
	 * @param biotypeToIncludeUTR Ensembl biotypes to include, e.g. protein_coding. Genes with biotype that don't match, won't be included. Boolean value determines if UTR must be included for that biotype.
	 * @param includeCDS include CoDing Sequence (region of a cDNA which is translated) or not.
	 * @param includeUTR include all UnTranslated Region or not (if set to false, can be set for each biotype separately)
	 * @param addChr add 'chr' in front of chromosome names (e.g. for UCSC reference).
	 * @param output bed file to output. Set to null if you want to populate Highlander database.
	 * @param analysis the Highlander [analysis]_coverage_regions table to populate. Set to null if you want to produce a bed file.
	 * @throws Exception
	 */
	public static void generateBed(Reference referenceGenome, Set<String> geneSymbols, Map<String, Boolean> biotypeToIncludeUTR, boolean includeCDS, boolean includeUTR, boolean addChr, boolean addEnsG, boolean addEnsT, File output, Analysis analysis) throws Exception {
		Map<String,Set<String>> genes = new TreeMap<>(new Tools.NaturalOrderComparator(true));
		for (String geneSymbol : geneSymbols) {
			String ensg = DBUtils.getEnsemblGene(referenceGenome, geneSymbol);
			String chr = DBUtils.getChromosome(referenceGenome, ensg);
			if (chr != null && ensg != null) {
				if (!genes.containsKey(chr)) {
					genes.put(chr, new TreeSet<String>());
				}
				genes.get(chr).add(ensg);
			}else {
				System.out.println(geneSymbol + " was not found in Ensembl");
			}
		}
		int total = 0;
		for (Set<String> set : genes.values()) total += set.size();
		System.out.println("Converted " + geneSymbols.size() + " gene symbols to " + total + " Ensembl genes");
		generateBed(referenceGenome, genes, biotypeToIncludeUTR, includeCDS, includeUTR, addChr, addEnsG, addEnsT, output, analysis);
	}

	/**
	 * Generate a bed file or populate the Highlander [analysis]_coverage_regions table depending on given parameters.
	 * Regions are fetch from Ensembl, using the canonical transcript of given genes.
	 * output and analysis argument can both be given to produce a bed file and populate the database at the same time.
	 * 
	 * @param referenceGenome reference genome will determine which Ensembl database is used
	 * @param ensemblGenesPerChr chromosomes as keys and set of Ensembl gene ids as values (e.g. ENSGxxx)
	 * @param biotypeToIncludeUTR Ensembl biotypes to include, e.g. protein_coding. Genes with biotype that don't match, won't be included. Boolean value determines if UTR must be included for that biotype.
	 * @param includeCDS include CoDing Sequence (region of a cDNA which is translated) or not.
	 * @param includeUTR include all UnTranslated Region or not (if set to false, can be set for each biotype separately)
	 * @param addChr add 'chr' in front of chromosome names (e.g. for UCSC reference).
	 * @param output bed file to output. Set to null if you want to populate Highlander database.
	 * @param analysis the Highlander [analysis]_coverage_regions table to populate. Set to null if you want to produce a bed file.
	 * @throws Exception
	 */
	public static void generateBed(Reference referenceGenome, Map<String,Set<String>> ensemblGenesPerChr, Map<String, Boolean> biotypeToIncludeUTR, boolean includeCDS, boolean includeUTR, boolean addChr, boolean addEnsG, boolean addEnsT, File output, Analysis analysis) throws Exception {
		DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
		File insertFile = (analysis != null) ? DbBuilder.createTempInsertionFile(analysis+"_coverage_region_"+df2.format(System.currentTimeMillis())+".sql") : null;
		try (FileWriter fw = (output != null) ? new FileWriter(output) : null; FileWriter sql = (analysis != null) ? new FileWriter(insertFile) : null){
			Map<String, Map<Integer, Map<Integer, Map<String,List<Region>>>>> intervals = new TreeMap<>(new Tools.NaturalOrderComparator(true));
			//int k=0;
			for (String chr : ensemblGenesPerChr.keySet()) {
				for (String ensg : ensemblGenesPerChr.get(chr)) {
					//System.out.println(k++);
					String enst = DBUtils.getEnsemblCanonicalTranscript(referenceGenome, ensg);
					String geneSymbol = DBUtils.getGeneSymbol(referenceGenome, ensg);
					Gene gene = new Gene(enst, referenceGenome, chr, true);
					System.out.println(chr + "\t" + geneSymbol + "\t" + ensg + "\t" + enst + "\t" + gene.getBiotype());
					if (geneSymbol != null && (biotypeToIncludeUTR.isEmpty() || biotypeToIncludeUTR.containsKey(gene.getBiotype()))) {
						for (int i=0 ; i < gene.getExonCount() ; i++) {
							int cdsStart = gene.getTranslationStart();
							int cdsEnd = gene.getTranslationEnd();
							int exonStart = gene.getExonStart(i);
							int exonEnd = gene.getExonEnd(i);
							if (includeCDS && gene.isTranslated() && exonEnd >= cdsStart && exonStart <= cdsEnd){
								if (exonStart < cdsStart) exonStart = cdsStart;
								if (exonEnd > cdsEnd) exonEnd = cdsEnd;
								//As Ensembl uses 1-based coordinates and BED uses 0-based coordinates ...
								exonStart -= 1;
								int exonNum = gene.getExonRank(i);
								if (!intervals.containsKey(chr)) intervals.put(chr, new TreeMap<Integer, Map<Integer,Map<String,List<Region>>>>());
								if (!intervals.get(chr).containsKey(exonStart)) intervals.get(chr).put(exonStart, new TreeMap<Integer,Map<String,List<Region>>>());
								if (!intervals.get(chr).get(exonStart).containsKey(exonEnd)) intervals.get(chr).get(exonStart).put(exonEnd, new TreeMap<String,List<Region>>(new Tools.NaturalOrderComparator(true)));
								if (!intervals.get(chr).get(exonStart).get(exonEnd).containsKey(geneSymbol)) intervals.get(chr).get(exonStart).get(exonEnd).put(geneSymbol, new ArrayList<Region>());
								intervals.get(chr).get(exonStart).get(exonEnd).get(geneSymbol).add(new Region(geneSymbol+" exon "+exonNum, chr, exonStart, exonEnd, geneSymbol, ensg, enst, exonNum, 0, false, true));
							}
							exonStart = gene.getExonStart(i);
							exonEnd = gene.getExonEnd(i);
							if ((includeUTR || (biotypeToIncludeUTR.containsKey(gene.getBiotype()) && biotypeToIncludeUTR.get(gene.getBiotype()))) && gene.isTranslated() && (exonEnd > cdsEnd || exonStart < cdsStart)){
								if (exonStart > cdsStart && exonStart < cdsEnd) exonStart = cdsEnd+1;
								if (exonEnd < cdsEnd && exonEnd > cdsStart) exonEnd = cdsStart-1;
								//As Ensembl uses 1-based coordinates and BED uses 0-based coordinates ...
								exonStart -= 1;
								int exonNum = gene.getExonRank(i);
								if (!intervals.containsKey(chr)) intervals.put(chr, new TreeMap<Integer, Map<Integer,Map<String,List<Region>>>>());
								if (!intervals.get(chr).containsKey(exonStart)) intervals.get(chr).put(exonStart, new TreeMap<Integer,Map<String,List<Region>>>());
								if (!intervals.get(chr).get(exonStart).containsKey(exonEnd)) intervals.get(chr).get(exonStart).put(exonEnd, new TreeMap<String,List<Region>>(new Tools.NaturalOrderComparator(true)));
								if (!intervals.get(chr).get(exonStart).get(exonEnd).containsKey(geneSymbol)) intervals.get(chr).get(exonStart).get(exonEnd).put(geneSymbol, new ArrayList<Region>());
								intervals.get(chr).get(exonStart).get(exonEnd).get(geneSymbol).add(new Region(geneSymbol+" exon "+exonNum, chr, exonStart, exonEnd, geneSymbol, ensg, enst, exonNum, 0, true, false));
							}else if ((includeUTR || (biotypeToIncludeUTR.containsKey(gene.getBiotype()) && biotypeToIncludeUTR.get(gene.getBiotype()))) && !gene.isTranslated()) {
								//As Ensembl uses 1-based coordinates and BED uses 0-based coordinates ...
								exonStart -= 1;
								int exonNum = gene.getExonRank(i);
								if (!intervals.containsKey(chr)) intervals.put(chr, new TreeMap<Integer, Map<Integer,Map<String,List<Region>>>>());
								if (!intervals.get(chr).containsKey(exonStart)) intervals.get(chr).put(exonStart, new TreeMap<Integer,Map<String,List<Region>>>());
								if (!intervals.get(chr).get(exonStart).containsKey(exonEnd)) intervals.get(chr).get(exonStart).put(exonEnd, new TreeMap<String,List<Region>>(new Tools.NaturalOrderComparator(true)));
								if (!intervals.get(chr).get(exonStart).get(exonEnd).containsKey(geneSymbol)) intervals.get(chr).get(exonStart).get(exonEnd).put(geneSymbol, new ArrayList<Region>());
								intervals.get(chr).get(exonStart).get(exonEnd).get(geneSymbol).add(new Region(geneSymbol+" exon "+exonNum, chr, exonStart, exonEnd, geneSymbol, ensg, enst, exonNum, 0, true, false));								
							}
						}
					}
				}
			}
			for (Map<Integer, Map<Integer, Map<String,List<Region>>>> a : intervals.values()) {
				for (Map<Integer, Map<String,List<Region>>> b : a.values()) {
					for (Map<String,List<Region>> c : b.values()) {
						for (List<Region> d : c.values()) {
							for (Region region : d) {
								if (output != null) fw.write(region.getBedString(addChr, addEnsG, addEnsT));
								if (analysis != null) sql.write(region.getSQLString(Highlander.getDB().getDataSource(Schema.HIGHLANDER).getDBMS()));
							}
						}
					}
				}
			}
		}
		if (analysis != null) {
			System.out.println("Importing regions in the database");
			Highlander.getDB().insertFile(Schema.HIGHLANDER, analysis.getTableCoverageRegions(), Region.getInsertionColumnsString(), insertFile, true, Highlander.getParameters());
			insertFile.delete();
		}
	}

	/**
	 * Generate a txt file with all genes and their exons, to be used with GATK DepthOfCoverage tool using -geneList argument.
	 * GATK needs an export from UCSC, see https://software.broadinstitute.org/gatk/documentation/article.php?id=1329
	 * The non standard contig must be removed (keep only 1-22, X, Y, MT) and the file must be sorted in karyotypic order (after tests, it seems it has to be sorted by transcription start, not CDS start, which is strange considering that only the CDS is taken into account by GATK).
	 * This method generate a similar file but with Ensembl as the source.
	 * For Highlander, it means that the gene symbols and exons used in "gene coverage" tool are identical to the ones used in variant annotation (snpEff or VEP, both use Ensembl).
	 * Only translated protein coding genes are included.
	 * The big difference is that in this file, only the canonical transcripts are included.
	 * With UCSC file, all RefSeq transcripts are included, and GATK merge the exons from all of them.
	 * It means you will only have around ~20K entries instead of ~40K, and that some exons specific to non-canonical transcript won't be taken into account for the coverage computation.
	 * We decided it was a minor drawback, and more consistent with Highlander philosophy of showing only annotation on canonical transcripts.
	 * Anyways, if needed, it's possible to include all transcripts with a few adaptations of this code.
	 * 
	 * @param biotypeToInclude
	 * @param addChr
	 * @param output
	 * @throws Exception
	 */
	public static void generatedUCSCLikeFileForGATKDepthOfCoverage(Reference referenceGenome, Set<String> biotypeToInclude, boolean addChr, File output) throws Exception {
		Map<String,Set<String>> ensemblGenesPerChr = getEnsemblGenesPerChr(referenceGenome, false);
		try (FileWriter fw = new FileWriter(output)){
			int k=0;
			for (String chr : ensemblGenesPerChr.keySet()) {
				Map<Integer,String> entries = new TreeMap<>();	//To sort entries by karyotypic order -- chr:transcription_start
				for (String ensg : ensemblGenesPerChr.get(chr)) {
					System.out.println(k++);
					String enst = DBUtils.getEnsemblCanonicalTranscript(referenceGenome, ensg);
					//System.out.println(chr + "\t" + gene + "\t" + ensg + "\t" + enst);
					Gene gene = new Gene(enst, referenceGenome, chr, true);
					if (gene.isTranslated()) {	//as GATK gives coverage for the CDS I don't think it's useful to include UTR-only genes 
						if (biotypeToInclude.isEmpty() || biotypeToInclude.contains(gene.getBiotype())) {
							StringBuilder sb = new StringBuilder();
							sb.append("0" + "\t");	//bin -- Most of our tables have a special first column called "bin" that helps with quickly displaying data on the Genome Browser. This (chrom,bin) index causes query results to be ordered first by bin, then by chromStart. This allows us to query and return results more quickly than if they were sorted by chromStart.
							sb.append(gene.getEnsemblTranscript() + "\t");	//name
							sb.append(((addChr)?"chr":"") + gene.getChromosome() + "\t");	//chrom
							sb.append((gene.isStrandPositive()?"+":"-") + "\t");	//strand
							sb.append(gene.getTranscriptionStart() + "\t");	//txStart
							sb.append(gene.getTranscriptionEnd() + "\t");	//txEnd
							sb.append(gene.getTranslationStart() + "\t");	//cdsStart
							sb.append(gene.getTranslationEnd() + "\t");	//cdsEnd
							sb.append(gene.getExonCount() + "\t");	//exonCount
							for (int e=0 ; e < gene.getExonCount() ; e++) {
								//Warning: The GATK automatically adjusts the start and stop position of the records from zero-based half-open intervals (UCSC standard) to one-based closed intervals.
								//--> adjust one-based from Ensembl to zero-based UCSC (so GATK could reconvert them after ....)
								sb.append((gene.getExonStart(e)-1) + ",");	//exonStarts	
							}
							sb.append("\t");
							for (int e=0 ; e < gene.getExonCount() ; e++) {
								sb.append(gene.getExonEnd(e) + ",");	//exonEnds	
							}
							sb.append("\t");
							sb.append("0" + "\t");	//score
							sb.append(gene.getGeneSymbol() + "\t");	//name2
							sb.append("unk" + "\t");	//cdsStartStat -- Status of CDS start annotation (none, unknown, incomplete, or complete)
							sb.append("unk" + "\t");	//cdsEndStat -- Status of CDS end annotation (none, unknown, incomplete, or complete)
							for (int e=0 ; e < gene.getExonCount() ; e++) {
								sb.append(gene.getExonFrame(e) + ",");	//exonFrames	-- Exon frame offsets {0,1,2}	
							}
							sb.append("\n");
							int offset=0;
							while (entries.containsKey(gene.getTranscriptionStart()+offset)) {
								offset++; 
							}
							entries.put(gene.getTranscriptionStart()+offset, sb.toString());
						}
					}
				}
				for (String entry : entries.values()) {
					fw.write(entry);
				}				
			}
		}
	}

	/**
	 * Generate a txt file with all genes and their exons, to be used with AnnotSV (instead of RefSeq).
	 * See generatedUCSCLikeFileForGATKDepthOfCoverage for details.
	 * Differences:
	 * - UTR-only genes are included
	 * - translation start/end are set to txEnd for non translated genes
	 * - chromosome are sorted alphabetically
	 * 
	 * @param biotypeToInclude
	 * @param output
	 * @throws Exception
	 */
	public static void generatedUCSCLikeFileForAnnotSV(Reference referenceGenome, Set<String> biotypeToInclude, File output) throws Exception {
		Map<String,Set<String>> ensemblGenesPerChr = new TreeMap<>(getEnsemblGenesPerChr(referenceGenome, false));
		try (FileWriter fw = new FileWriter(output)){
			int k=0;
			for (String chr : ensemblGenesPerChr.keySet()) {
				Map<Integer,String> entries = new TreeMap<>();	//To sort entries by karyotypic order -- chr:transcription_start
				for (String ensg : ensemblGenesPerChr.get(chr)) {
					System.out.println(k++);
					String enst = DBUtils.getEnsemblCanonicalTranscript(referenceGenome, ensg);
					//System.out.println(chr + "\t" + gene + "\t" + ensg + "\t" + enst);
					Gene gene = new Gene(enst, referenceGenome, chr, true);
					if (biotypeToInclude.isEmpty() || biotypeToInclude.contains(gene.getBiotype())) {
						StringBuilder sb = new StringBuilder();
						sb.append(gene.getChromosome() + "\t");	//chrom
						sb.append(gene.getTranscriptionStart() + "\t");	//txStart
						sb.append(gene.getTranscriptionEnd() + "\t");	//txEnd
						sb.append((gene.isStrandPositive()?"+":"-") + "\t");	//strand
						sb.append(gene.getGeneSymbol() + "\t");	//name2
						sb.append(gene.getEnsemblTranscript() + "\t");	//name
						if (gene.isTranslated()) {
							sb.append(gene.getTranslationStart() + "\t"); //cdsStart
							sb.append(gene.getTranslationEnd() + "\t");	//cdsEnd
						}else {
							sb.append(gene.getTranscriptionEnd() + "\t");	
							sb.append(gene.getTranscriptionEnd() + "\t");	
						}
						for (int e=0 ; e < gene.getExonCount() ; e++) {
							//Warning: The GATK automatically adjusts the start and stop position of the records from zero-based half-open intervals (UCSC standard) to one-based closed intervals.
							//--> adjust one-based from Ensembl to zero-based UCSC (so GATK could reconvert them after ....)
							sb.append((gene.getExonStart(e)-1) + ",");	//exonStarts	
						}
						sb.append("\t");
						for (int e=0 ; e < gene.getExonCount() ; e++) {
							sb.append(gene.getExonEnd(e) + ",");	//exonEnds	
						}
						sb.append("\n");
						int offset=0;
						while (entries.containsKey(gene.getTranscriptionStart()+offset)) {
							offset++; 
						}
						entries.put(gene.getTranscriptionStart()+offset, sb.toString());
					}
				}
				for (String entry : entries.values()) {
					fw.write(entry);
				}				
			}
		}
	}

	/**
	 * Generate a bed file for the whole exome, but with only the translated regions of protein_coding genes present on chr 1-22+X+Y (without exceptions, like PAR).
	 * 
	 * Notes:
	 * - UCSC uses 0-base coordinates and Ensembl uses 1-based coordinates. See https://www.biostars.org/p/84686/ for a nice wrap-up.
	 * - Add 'chr' to chromosomes 1-2+X+Y are enough for compatibility between hg19 & GRCh37. But other contigs are completely different (including MT, different sequence) and have other names. e.g. chr6_qbl_hap6 (UCSC) vs HSCHR6_MHC_QBL (Ensembl)
	 * - PAR / HAP regions are found in the Ensembl table 'assembly_exception'. Warning : some regions can be found multiple times in different chromosomes, use this table to find all of them.
	 * - Chromosome linked to a region in Ensembl is found in table 'seq_region'.
	 * - Each gene/transcript/translation is linked to a biotype. They can be different between the 3 tables (e.g. a gene has biotype A, but some of it's transcript have another one).
	 * 
	 *  So we decided to generate bed file of translated protein-coding genes only, with their canonical transcript only, and located on chr 1-22/X/Y (other contigs are ignored). 
	 *  This bed will be used for 'exome' coverage (vs target coverage that uses the bed file of capture kit), for CNV calling and for panels (whole gene bed).
	 *  Note that variant calling uses the target bed (with a 100 bases padding set in GATK command line).
	 *  
	 *  With this approach, the bed file generated by this method is smaller than RefSeq or UCSC export.
	 *  - It's mainly because of canonical transcript : UCSC exports all transcript (and GATK merge intervals of all them).
	 *  - Some untranslated genes are included in capture kits (e.g. miRNA and things like that). They are not included here.
	 *  - If we want to include untranslated things, it poses the problem of protein coding genes UTR: no kit capture those, and it be complicated and not really consistent to not add those too.
	 *  - Capture kits don't include non standard contigs (only 1-22+X+Y), so let's take the same approach to avoid headaches.
	 *  
	 *  See Excel file in E:\References\Exome for number of genes per chr per kit/source (UCSC/RefSeq/Ensembl)
	 *  
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Highlander.initialize(new Parameters(false), 5);
			/*
			//All variants from the DB for Amin / Elixir 
			try (Results res = DB.select(Schema.HIGHLANDER, "SELECT chr, pos, reference, alternative FROM exomes_haplotype_caller_change_stats", true)) {
			Map<String, Set<String>> map = new TreeMap<>(new Tools.NaturalOrderComparator());
			for (int i=1 ; i <= 22 ; i++) {
				map.put(""+i, new TreeSet<>(new Tools.NaturalOrderComparator()));
			}
			map.put("X", new TreeSet<>(new Tools.NaturalOrderComparator()));
			map.put("Y", new TreeSet<>(new Tools.NaturalOrderComparator()));
			map.put("MT", new TreeSet<>(new Tools.NaturalOrderComparator()));
			while(res.next()) {
				//System.out.println(res.getString("chr") + "\t" + res.getString("pos")+"\t"+res.getString("reference")+"\t"+res.getString("alternative"));
				map.get(res.getString("chr")).add(res.getString("pos")+"\t"+res.getString("reference")+"\t"+res.getString("alternative"));
			}
			}
			try (FileWriter fw = new FileWriter(new File("D:/all_pos.txt"))){
			for (Entry<String, Set<String>> e : map.entrySet()) {
				for (String val :  e.getValue()) {
					fw.write(e.getKey() + "\t" + val + "\n");
				}
			}
			}
			 */
			//Generate bed for exomes
			Map<String,Boolean> biotypes = new TreeMap<>();
			biotypes.put("protein_coding",false);
			//generateBed(new Reference("GRCh37"), false, biotypes, true, false, false, false, false, new File("GRCh37.CDS.protcod.nochr.bed"), new Analysis("exomes_cnv"));
			//generateBed(new Reference("GRCh38"), false, biotypes, true, false, false, false, false, new File("GRCh38.CDS.protcod.nochr.bed"), null);
			generateBed(new Reference("GRCh38"), false, biotypes, true, false, false, true, true, new File("GRCh38.coding.withoutUTR.withENS.chr.bed"), null);
			/*
			// Generate bed for rnaseq
			Map<String,Boolean> biotypes = new TreeMap<>();
			biotypes.put("protein_coding",false);
			biotypes.put("miRNA",true);										//A small RNA (~22bp) that silences the expression of target mRNA.
			biotypes.put("lincRNA",true);									//Transcripts that are long intergenic non-coding RNA locus with a length >200bp. Requires lack of coding potential and may not be conserved between species.
			biotypes.put("misc_RNA",true);									//Miscellaneous RNA. A non-coding RNA that cannot be classified.
			biotypes.put("piRNA",true);											//piRNA: An RNA that interacts with piwi proteins involved in genetic silencing.
			biotypes.put("rRNA",true);											//The RNA component of a ribosome.
			biotypes.put("siRNA",true);												//siRNA: A small RNA (20-25bp) that silences the expression of target mRNA through the RNAi pathway.
			biotypes.put("snRNA",true);										//Small RNA molecules that are found in the cell nucleus and are involved in the processing of pre messenger RNAs
			biotypes.put("snoRNA",true);										//Small RNA molecules that are found in the cell nucleolus and are involved in the post-transcriptional modification of other RNAs.
			biotypes.put("tRNA",true);												//tRNA: A transfer RNA, which acts as an adaptor molecule for translation of mRNA.
			biotypes.put("vaultRNA",true);												//vaultRNA: Short non coding RNA genes that form part of the vault ribonucleoprotein complex.			
			//generateBed(new Reference("GRCm38"), false, biotypes, true, false, false, false, false, new File("GRCm38.rnaseq.nochr.bed"), null);
			generateBed(new Reference("GRCh37"), false, biotypes, true, false, false, false, false, new File("GRCh37.rnaseq.nochr.bed"), null);
			*/
			//Create all .fullgenes for IonTorrent
			/*
			try (Results res = DB.select(Schema.HIGHLANDER, "SELECT code, genes from ion_panels WHERE code NOT IN ('pEXOME','pLEGAPS')")) {
			while (res.next()) {
				String panel = res.getString("code");
				System.out.println(panel);
				File tmpfull = new File(panel+".fullgenes.bed");
				Set<String> genes = new TreeSet<>(Arrays.asList(res.getString("genes").split(";")));
				ExomeBed.generateBed(new Reference("b37"), genes, new TreeSet<String>(), true, false, true, false, false, tmpfull);
				File tmpgenes = new File(panel+".genes");
				try (BufferedWriter bw = new BufferedWriter(new FileWriter(tmpgenes))){
				for (String geneSymbol : genes) {
					String ensg = DBUtils.getEnsemblGene(new Reference("b37"), geneSymbol);
					if (ensg != null) {
						bw.write(ensg + "\n");
					}
				}
				}
			}
			}
			 */
			/*
			//Create gene panel bed
			File tmpfull = new File("pAMIN.bed");
			Set<String> genes = new TreeSet<>(Arrays.asList(new String[] {"ALK","APC","ATM","AXIN2","BAP1","BARD1","BLM","BMPR1A","BRCA1","BRCA2","BRIP1","CDC73","CDH1","CDK12","CDK4","CDKN1B","CDKN2A","CHEK2","EPCAM","EXT1","EXT2","FANCG","FH","FLCN","GALNT12","KIT","MAX","MEN1","MET","MLH1","MLH3","MRE11A","MSH2","MSH3","MSH6","MUTYH","NBN","NF1","NF2","NTHL1","NTRK1","PALB2","PDGFRA","PHOX2B","PMS1","PMS2","POLD1","POLE","PRSS1","PTCH1","PTCH2","PTEN","RAD50","RAD51C","RAD51D","RB1","RET","SDHA","SDHAF2","SDHB","SDHC","SDHD","SMAD4","SMARCA4","SPINK1","STK11","SUFU","TMEM127","TP53","TSC1","TSC2","VHL","WT1","XPC"}));
			ExomeBed.generateBed(new Reference("b37"), genes, new TreeSet<String>(), true, false, false, false, false, tmpfull, null);						
			*/
			/*
			File tmpfull = new File("pTest.bed");
			Set<String> genes = new TreeSet<>(Arrays.asList(new String[] {"GLMN"}));
			ExomeBed.generateBed(new Reference("b37"), genes, new TreeSet<String>(), true, false, false, false, false, tmpfull);
			 */

			//generatedUCSCLikeFileForGATKDepthOfCoverage(new Reference("b37"), new HashSet<String>(), false, new File("refGene.b37.txt"));
			//generatedUCSCLikeFileForGATKDepthOfCoverage(new Reference("hg19"), new HashSet<String>(), true, new File("refGene.h19.txt"));
			//generatedUCSCLikeFileForGATKDepthOfCoverage(new Reference("GRCm38"), new HashSet<String>(), false, new File("refGene.GRCm38.txt"));
			//generatedUCSCLikeFileForAnnotSV(new Reference("b37"), new HashSet<String>(), new File("refGene.sorted.bed"));

		} catch (Exception ex) {
			Tools.exception(ex);
		}	
		System.exit(0);
	}
}
