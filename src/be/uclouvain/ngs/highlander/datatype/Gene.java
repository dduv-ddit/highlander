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

package be.uclouvain.ngs.highlander.datatype;

import java.awt.Color;
//import java.sql.Blob;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.database.DBUtils;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.SNPEffect.VariantType;
import be.uclouvain.ngs.highlander.datatype.SNPEffect.Zygosity;


public class Gene {
	public static final int SPLICE_SITE_EXTENDS = 10;

	public static final String[] biotypePriorities = new String[] {
			"protein_coding",
			"miRNA" ,										//A small RNA (~22bp) that silences the expression of target mRNA.
			"lincRNA" ,									//Transcripts that are long intergenic non-coding RNA locus with a length >200bp. Requires lack of coding potential and may not be conserved between species.
			"misc_RNA" ,									//Miscellaneous RNA. A non-coding RNA that cannot be classified.
			"piRNA",												//piRNA: An RNA that interacts with piwi proteins involved in genetic silencing.
			"rRNA" ,											//The RNA component of a ribosome.
			"siRNA" ,												//siRNA: A small RNA (20-25bp) that silences the expression of target mRNA through the RNAi pathway.
			"snRNA" ,										//Small RNA molecules that are found in the cell nucleus and are involved in the processing of pre messenger RNAs
			"snoRNA" ,										//Small RNA molecules that are found in the cell nucleolus and are involved in the post-transcriptional modification of other RNAs.
			"tRNA" ,												//tRNA: A transfer RNA, which acts as an adaptor molecule for translation of mRNA.
			"vaultRNA" ,												//vaultRNA: Short non coding RNA genes that form part of the vault ribonucleoprotein complex.
			"processed_transcript" ,
			"3prime_overlapping_ncrna" , //Transcripts where ditag and/or published experimental data strongly supports the existence of long (>200bp) non-coding transcripts that overlap the 3'UTR of a protein-coding locus on the same strand.
			"antisense" , 								//Transcripts that overlap the genomic span (i.e. exon or introns) of a protein-coding locus on the opposite strand.			
			"retained_intron" , 					//An alternatively spliced transcript believed to contain intronic sequence relative to other, coding, transcripts of the same gene.
			"sense_intronic" ,						//A long non-coding transcript in introns of a coding gene that does not overlap any exons.
			"sense_overlapping" ,				//A long non-coding transcript that contains a coding gene in its intron on the same strand.
			"pseudogene" ,								
			"IG_C_pseudogene" ,					//Inactivated immunoglobulin gene.
			"IG_J_pseudogene" ,					//Inactivated immunoglobulin gene.
			"IG_V_pseudogene" ,					//Inactivated immunoglobulin gene.
			"TR_J_pseudogene" ,					//Inactivated T cell receptor gene.
			"TR_V_pseudogene" ,					//Inactivated T cell receptor gene.
			"polymorphic_pseudogene",		//Pseudogene owing to a SNP/indel but in other individuals/haplotypes/strains the gene is translated.
			"processed_pseudogene" ,			//Pseudogene that lack introns and is thought to arise from reverse transcription of mRNA followed by reinsertion of DNA into the genome.
			"transcribed_processed_pseudogene" ,		//Pseudogene where protein homology or genomic structure indicates a pseudogene, but the presence of locus-specific transcripts indicates expression. These can be classified into 'Processed', 'Unprocessed' and 'Unitary'.
			"translated_processed_pseudogene" ,		//Pseudogenes that have mass spec data suggesting that they are also translated. These can be classified into 'Processed', 'Unprocessed'
			"unitary_pseudogene" ,				//A species specific unprocessed pseudogene without a parent gene, as it has an active orthologue in another species.
			"unprocessed_pseudogene" ,		//Pseudogene that can contain introns since produced by gene duplication.
			"transcribed_unprocessed_pseudogene" ,	//?
			"nonsense_mediated_decay",		//?
			"IG_C_gene", 								//Constant chain immunoglobulin gene that undergoes somatic recombination before transcription
			"IG_D_gene", 								//Diversity chain immunoglobulin gene that undergoes somatic recombination before transcription
			"IG_J_gene", 								//Joining chain immunoglobulin gene that undergoes somatic recombination before transcription
			"IG_V_gene", 								//Variable chain immunoglobulin gene that undergoes somatic recombination before transcription
			"TR_C_gene",									//Constant chain T cell receptor gene that undergoes somatic recombination before transcription
			"TR_D_gene",									//Diversity chain T cell receptor gene that undergoes somatic recombination before transcription
			"TR_J_gene",									//Joining chain T cell receptor gene that undergoes somatic recombination before transcription
			"TR_V_gene",									//Variable chain T cell receptor gene that undergoes somatic recombination before transcription
	};
	
	private Reference referenceGenome;
	private String geneSymbol;
	private String ensemblGene;
	private String biotype;
	private String chromosome;
	private boolean strandPositive;
	private String ensemblTranscript;
	private String refSeqTranscript;
	private Map<Zygosity,List<Integer>> variantsIds = new EnumMap<Zygosity, List<Integer>>(Zygosity.class);
	//Transcription start/end, CDS + UTR, from smallest to highest pos (independent from strand orientation)
	private int txStart; 
	private int txEnd; 
	//Translation (CDS) start/end, CDS without UTR, from smallest to highest pos (independent from strand orientation)
	private boolean isTranslated;
	private int cdsStart; 
	private int cdsEnd; 
	private int count; 
	private int[] ranks; //Ranks on the protein, from 5' to 3' on strand + and from 3' to 5' on strand - 
	private int[] starts; 
	private int[] ends;
	private int[] frames;
	private String[] sequences;

	/**
	 * Empty gene data, should call a setExonData method
	 * @param geneSymbol
	 * @param chromosome
	 * @param ensemblTranscript
	 * @param refSeqTranscript
	 */
	public Gene(Reference referenceGenome, String chromosome, String geneSymbol, String ensemblGene, String ensemblTranscript, String refSeqTranscript, String biotype){
		this.referenceGenome = referenceGenome;
		this.chromosome = chromosome;
		this.geneSymbol = (geneSymbol != null) ? geneSymbol : ensemblGene;
		this.ensemblGene = ensemblGene;
		this.ensemblTranscript = ensemblTranscript;
		this.refSeqTranscript = refSeqTranscript;
		this.biotype = biotype;
		for (Zygosity zygosity : Zygosity.values()){
			variantsIds.put(zygosity, new ArrayList<Integer>());
		}
	}

	public void addVariantId(int id, Zygosity zygosity){
		variantsIds.get(zygosity).add(id);
	}

	/**
	 * Retreive all information from Ensembl
	 * @param ensemblTranscript
	 */
	public Gene(String ensemblTranscript, Reference referenceGenome, String chromosome, boolean fetchExonData) throws Exception {
		this.ensemblTranscript = ensemblTranscript;
		this.referenceGenome = referenceGenome;
		this.chromosome = chromosome;
		try (Results res = Highlander.getDB().select(referenceGenome, Schema.ENSEMBL, 
				"SELECT g.stable_id, tx.biotype, x.display_label "
						+ "FROM gene g "
						+ "JOIN transcript tx USING (gene_id) "
						+ "LEFT JOIN xref as x ON (g.display_xref_id = x.xref_id) "
						+ "WHERE tx.stable_id = '"+ensemblTranscript+"'")){
			if (res.next()) {
				ensemblGene = res.getString("g.stable_id");
				biotype = res.getString("tx.biotype");
				if (res.getString("x.display_label") != null) {
					geneSymbol = res.getString("x.display_label");
				}else {
					geneSymbol = ensemblGene;
				}
			}
		}
		refSeqTranscript = DBUtils.getAccessionRefSeqMRna(referenceGenome, ensemblTranscript);
		if (fetchExonData) setExonDataFromEnsembl();
	}

	public void setExonDataFromEnsembl() throws Exception {
		if (getEnsemblTranscript() != null){
			String query;
			switch(biotype) {
			case "protein_coding":
			case "nonsense_mediated_decay":
			case "polymorphic_pseudogene":
			case "IG_C_gene":
			case "IG_D_gene":
			case "IG_J_gene":
			case "IG_V_gene":
			case "TR_C_gene":
			case "TR_D_gene":
			case "TR_J_gene":
			case "TR_V_gene":
				isTranslated = true;
				query = "SELECT tl.seq_start, tl.seq_end, tl.start_exon_id, tl.end_exon_id, extx.rank, ex.exon_id, ex.seq_region_start, ex.seq_region_end, ex.seq_region_strand, ex.phase, ex.end_phase "+
						"FROM transcript as tx "+
						"JOIN translation as tl USING (transcript_id) "+
						"JOIN exon_transcript as extx USING (transcript_id) "+
						"JOIN exon as ex USING (exon_id) "+
						"WHERE tx.stable_id = '"+getEnsemblTranscript()+"' "+
						"ORDER BY rank"
						;
				break;
			default:
				isTranslated = false;
				query = "SELECT extx.rank, ex.exon_id, ex.seq_region_start, ex.seq_region_end, ex.seq_region_strand, ex.phase, ex.end_phase "+
						"FROM transcript as tx "+
						"JOIN exon_transcript as extx USING (transcript_id) "+
						"JOIN exon as ex USING (exon_id) "+
						"WHERE tx.stable_id = '"+getEnsemblTranscript()+"' "+
						"ORDER BY rank"
						;
				break;
			}		
			try (Results res = Highlander.getDB().select(referenceGenome, Schema.ENSEMBL, query)){
				count = res.getResultSetSize();
				starts = new int[count];
				ends = new int[count];
				frames = new int[count];
				ranks = new int[count];
				Map<Integer, Integer> mapStarts = new TreeMap<>();
				Map<Integer, Integer> mapEnds = new TreeMap<>();
				Map<Integer, Integer> mapFrames = new TreeMap<>();
				while (res.next()) {
					int rank = res.getInt("extx.rank");
					mapStarts.put(rank, res.getInt("ex.seq_region_start"));
					mapEnds.put(rank, res.getInt("ex.seq_region_end"));
					mapFrames.put(rank, res.getInt("ex.phase"));
					if (mapFrames.get(rank) == -1) mapFrames.put(rank, 0);
					strandPositive = (res.getInt("ex.seq_region_strand") == 1);
					if (isTranslated) {
						if (res.getString("ex.exon_id").equals(res.getString("tl.start_exon_id"))) {
							if (strandPositive)
								cdsStart = res.getInt("ex.seq_region_start") + res.getInt("tl.seq_start") - 2;
							else
								cdsEnd = res.getInt("ex.seq_region_end") - res.getInt("tl.seq_start") + 1;
						}
						if (res.getString("ex.exon_id").equals(res.getString("tl.end_exon_id"))) {
							if (strandPositive)
								cdsEnd = res.getInt("ex.seq_region_start") + res.getInt("tl.seq_end") - 1;
							else
								cdsStart = res.getInt("ex.seq_region_end") - res.getInt("tl.seq_end");
						}
					}
				}
				for (int i=0 ; i < count ; i++) {
					starts[i] = (strandPositive) ? mapStarts.get(i+1) : mapStarts.get(count-i);
					ends[i] = (strandPositive) ? mapEnds.get(i+1) : mapEnds.get(count-i);
					frames[i] = (strandPositive) ? mapFrames.get(i+1) : mapFrames.get(count-i);
					ranks[i] = (strandPositive) ? (i+1) : (count-i);
				}
				txStart = starts[0];
				txEnd = ends[count-1];
			}
		}
	}

	/*
	public void setExonDataFromRefSeq() throws Exception {
		if (getRefSeqTranscript() != null){
			try (Results res = Highlander.getDB().select(Schema.UCSC, "SELECT txStart, txEnd, cdsStart, cdsEnd, exonCount, exonStarts, exonEnds, exonFrames, strand FROM refGene WHERE chrom = 'chr"+getChromosome()+"' AND `name` = '"+getRefSeqTranscript()+"'")) {
			if (res.next()){
				Blob blobs = res.getBlob("exonStarts");
				Blob blobe = res.getBlob("exonEnds");
				Blob blobf = res.getBlob("exonFrames");
				String[] strStarts = new String(blobs.getBytes(1, (int) blobs.length())).split(",");
				String[] strEnds = new String(blobe.getBytes(1, (int) blobe.length())).split(",");
				String[] strFrames = new String(blobf.getBytes(1, (int) blobf.length())).split(",");
				int[] starts = new int[strStarts.length];
				int[] ends = new int[strEnds.length];
				int[] frames = new int[strFrames.length];
				for(int i = 0;i < strStarts.length;i++)
				{
					starts[i] = Integer.parseInt(strStarts[i]);
					ends[i] = Integer.parseInt(strEnds[i]);
					frames[i] = Integer.parseInt(strFrames[i]);
				}
				isTranslated = true;
				setExonData(res.getInt("txStart"), res.getInt("txEnd"), res.getInt("cdsStart"), res.getInt("cdsEnd"), res.getInt("exonCount"), starts, ends, frames, res.getString("strand").equals("+"));
			}
			}
		}
	}
	 */

	public void setExonData(int txStart, int txEnd, int cdsStart, int cdsEnd, int count, int[] starts, int ends[], int frames[], boolean strandPositive){
		this.txStart = txStart;
		this.txEnd = txEnd;
		this.cdsStart = cdsStart;
		this.cdsEnd = cdsEnd;
		this.count = count;
		this.ranks = new int[count];
		for (int i=0 ; i < count ; i++) ranks[i] = (strandPositive) ? i+1 : count - i;
		this.starts = starts;
		this.ends = ends;
		this.frames = frames;
		this.strandPositive = strandPositive;
	}

	public void setEnsemblGene(String ensemblGene){
		this.ensemblGene = ensemblGene;
	}

	public void setBiotype(String biotype){
		this.biotype = biotype;
	}

	public void setEnsemblTranscript(String ensemblTranscript){
		this.ensemblTranscript = ensemblTranscript;
	}

	public void setRefSeqTranscript(String refSeqTranscript){
		this.refSeqTranscript = refSeqTranscript;
	}

	public String getGeneSymbol() {
		return geneSymbol;
	}

	public String getEnsemblGene() {
		return ensemblGene;
	}

	public String getChromosome() {
		return chromosome;
	}

	public boolean isStrandPositive(){
		return strandPositive;
	}

	public String getEnsemblTranscript() {
		return ensemblTranscript;
	}

	public String getRefSeqTranscript() {
		return refSeqTranscript;
	}

	public String getBiotype() {
		return biotype;
	}

	public Color getBiotypeColor() {
		switch(biotype) {
		//IG gene: Immunoglobulin gene that undergoes somatic recombination, annotated in collaboration with IMGT http://www.imgt.org/.
		case "IG_C_gene": 								//Constant chain immunoglobulin gene that undergoes somatic recombination before transcription
		case "IG_D_gene": 								//Diversity chain immunoglobulin gene that undergoes somatic recombination before transcription
		case "IG_J_gene": 								//Joining chain immunoglobulin gene that undergoes somatic recombination before transcription
		case "IG_V_gene": 								//Variable chain immunoglobulin gene that undergoes somatic recombination before transcription
			return new Color(255,0,110);
			//Processed transcript: Gene/transcript that doesn't contain an open reading frame (ORF).
		case "processed_transcript" :
			//Long non-coding RNA (lncRNA): A non-coding gene/transcript >200bp in length
		case "3prime_overlapping_ncrna" : //Transcripts where ditag and/or published experimental data strongly supports the existence of long (>200bp) non-coding transcripts that overlap the 3'UTR of a protein-coding locus on the same strand.
		case "antisense" : 								//Transcripts that overlap the genomic span (i.e. exon or introns) of a protein-coding locus on the opposite strand.			
			//case "":												//Macro lncRNA: Unspliced lncRNAs that are several kb in size.
			//case "":												//Non coding: Transcripts which are known from the literature to not be protein coding.
		case "retained_intron" : 					//An alternatively spliced transcript believed to contain intronic sequence relative to other, coding, transcripts of the same gene.
		case "sense_intronic" :						//A long non-coding transcript in introns of a coding gene that does not overlap any exons.
		case "sense_overlapping" :				//A long non-coding transcript that contains a coding gene in its intron on the same strand.
		case "lincRNA" :									//Transcripts that are long intergenic non-coding RNA locus with a length >200bp. Requires lack of coding potential and may not be conserved between species.
			return new Color(0,255,33);
			//ncRNA: A non-coding gene.
		case "miRNA" :										//A small RNA (~22bp) that silences the expression of target mRNA.
		case "misc_RNA" :									//Miscellaneous RNA. A non-coding RNA that cannot be classified.
			//case "":												//piRNA: An RNA that interacts with piwi proteins involved in genetic silencing.
		case "rRNA" :											//The RNA component of a ribosome.
			//case "" :												//siRNA: A small RNA (20-25bp) that silences the expression of target mRNA through the RNAi pathway.
		case "snRNA" :										//Small RNA molecules that are found in the cell nucleus and are involved in the processing of pre messenger RNAs
		case "snoRNA" :										//Small RNA molecules that are found in the cell nucleolus and are involved in the post-transcriptional modification of other RNAs.
			//case "" :												//tRNA: A transfer RNA, which acts as an adaptor molecule for translation of mRNA.
			//case "" :												//vaultRNA: Short non coding RNA genes that form part of the vault ribonucleoprotein complex.
			return new Color(15,255,217);
			//Protein coding: Gene/transcipt that contains an open reading frame (ORF).
		case "protein_coding":
		case "nonsense_mediated_decay":		//?
			return new Color(0,148,255);
			//Pseudogene: A gene that has homology to known protein-coding genes but contain a frameshift and/or stop codon(s) which disrupts the ORF. Thought to have arisen through duplication followed by loss of function.
		case "pseudogene" :								
		case "IG_C_pseudogene" :					//Inactivated immunoglobulin gene.
		case "IG_J_pseudogene" :					//Inactivated immunoglobulin gene.
		case "IG_V_pseudogene" :					//Inactivated immunoglobulin gene.
		case "TR_J_pseudogene" :					//Inactivated T cell receptor gene.
		case "TR_V_pseudogene" :					//Inactivated T cell receptor gene.
		case "polymorphic_pseudogene":		//Pseudogene owing to a SNP/indel but in other individuals/haplotypes/strains the gene is translated.
		case "processed_pseudogene" :			//Pseudogene that lack introns and is thought to arise from reverse transcription of mRNA followed by reinsertion of DNA into the genome.
		case "transcribed_processed_pseudogene" :		//Pseudogene where protein homology or genomic structure indicates a pseudogene, but the presence of locus-specific transcripts indicates expression. These can be classified into 'Processed', 'Unprocessed' and 'Unitary'.
		case "translated_processed_pseudogene" :		//Pseudogenes that have mass spec data suggesting that they are also translated. These can be classified into 'Processed', 'Unprocessed'
		case "unitary_pseudogene" :				//A species specific unprocessed pseudogene without a parent gene, as it has an active orthologue in another species.
		case "unprocessed_pseudogene" :		//Pseudogene that can contain introns since produced by gene duplication.
		case "transcribed_unprocessed_pseudogene" :	//?
			return new Color(178,0,255);
			//TR gene: T cell receptor gene that undergoes somatic recombination, annotated in collaboration with IMGT http://www.imgt.org/.
		case "TR_C_gene":									//Constant chain T cell receptor gene that undergoes somatic recombination before transcription
		case "TR_D_gene":									//Diversity chain T cell receptor gene that undergoes somatic recombination before transcription
		case "TR_J_gene":									//Joining chain T cell receptor gene that undergoes somatic recombination before transcription
		case "TR_V_gene":									//Variable chain T cell receptor gene that undergoes somatic recombination before transcription
			return new Color(255,0,220);
			//Readthrough: A readthrough transcript has exons that overlap exons from transcripts belonging to two or more different loci (in addition to the locus to which the readthrough transcript itself belongs).
			//TEC (To be Experimentally Confirmed): Regions with EST clusters that have polyA features that could indicate the presence of protein coding genes. These require experimental validation, either by 5' RACE or RT-PCR to extend the transcripts, or by confirming expression of the putatively-encoded peptide with specific antibodies.			
		default:
			return new Color(255,216,0);
		}	
	}

	/**
	 * Sort biotype by priority (of interest). The lowest number is the highest priority.
	 * Can be useful, e.g. if 2 genes have different ensembl ids but same gene symbol.
	 * As an example : Both ENSG00000105647 and ENSG00000268173 are PIK3R2, but the first is protein_coding and the second nonsense_mediated_decay.
	 * 
	 * @return a priority integer, the lowest number is the highest priority
	 */
	public int getBiotypePriority() {
		for (int i=0 ; i < biotypePriorities.length ; i++) {
			if (biotypePriorities[i].equalsIgnoreCase(biotype)) {
				return i;
			}
		}
		return 100;
	}
	
	public List<Integer> getVariantsIds(Zygosity zygosity) {
		return variantsIds.get(zygosity);
	}

	public List<Integer> getAllVariantsIds() {
		List<Integer> results = new ArrayList<Integer>();
		for (List<Integer> list : variantsIds.values()){
			results.addAll(list);
		}
		return results;
	}

	/**
	 * Transcription start, CDS + UTR, smallest pos (independent from strand orientation)
	 * @return
	 */
	public int getTranscriptionStart() {
		return txStart;
	}

	/**
	 * Transcription end, CDS + UTR, highest pos (independent from strand orientation)
	 * @return
	 */
	public int getTranscriptionEnd() {
		return txEnd;
	}

	public boolean isTranslated() {
		return isTranslated;
	}

	/**
	 * Translation (CDS) start, CDS without UTR, smallest pos (independent from strand orientation)
	 * @return
	 */
	public int getTranslationStart() {
		return cdsStart;
	}

	/**
	 * Translation (CDS) end, CDS without UTR, highest pos (independent from strand orientation)
	 * @return
	 */
	public int getTranslationEnd() {
		return cdsEnd;
	}

	public int getExonCount() {
		return count;
	}

	public int getExonStart(int exonNum) {
		return starts[exonNum];
	}

	public int getExonEnd(int exonNum) {
		return ends[exonNum];
	}

	public int getExonFrame(int exonNum) {
		return frames[exonNum];
	}

	public String getSequence(int exonNum) {
		if (sequences == null) {
			sequences = new String[count];
			try {
				for (int i=0 ; i < count ; i++) {
					sequences[i] = DBUtils.getSequence(referenceGenome, chromosome, starts[i], ends[i]);
				}
			}catch (Exception ex){
				sequences = null;
				Tools.exception(ex);
				return "";
			}
		}
		return sequences[exonNum];
	}

	public char getNucleotide(int position) {
		for (int i=0 ; i < count ; i++){
			if (position >= starts[i] && position <= ends[i]){
				return getSequence(i).charAt(position - starts[i]);
			}
		}
		return '?';
	}

	public int getExonRank(int exonNum) {
		return ranks[exonNum];
	}

	public int getIntronRank(int intronNum) {
		return isStrandPositive() ? ranks[intronNum] : ranks[intronNum+1];
	}

	/**
	 * Return true if this exon is in 5'UTR/3'UTR for forward/reverse genes.
	 * Note that in 1 exon genes, the only exon is both in left and right UTR.
	 * @param exonNum
	 * @return
	 */
	public boolean isExonInLeftUTR(int exonNum){
		return isPosInLeftUTR(starts[exonNum]);
	}

	/**
	 * Return true if this exon is in 3'UTR/5'UTR for forward/reverse genes
	 * Note that in 1 exon genes, the only exon is both in left and right UTR.
	 * @param exonNum
	 * @return
	 */
	public boolean isExonInRightUTR(int exonNum){
		return isPosInRightUTR(ends[exonNum]);
	}

	/**
	 * Return true if this position is in 5'UTR/3'UTR for forward/reverse genes.
	 * Return always false for non translated genes
	 * @param exonNum
	 * @return
	 */
	public boolean isPosInLeftUTR(int pos){
		if (isTranslated) return (pos < cdsStart);
		else return false;
	}

	/**
	 * Return true if this position is in 3'UTR/5'UTR for forward/reverse genes
	 * Return always false for non translated genes
	 * @param exonNum
	 * @return
	 */
	public boolean isPosInRightUTR(int pos){
		if (isTranslated) return (pos > cdsEnd);
		else return false;
	}

	public int getCodingLength(int exonNum){
		if (!isTranslated) return 0;
		int start = isPosInLeftUTR(starts[exonNum]) ? cdsStart : starts[exonNum];
		int end = isPosInRightUTR(ends[exonNum]) ? cdsEnd : ends[exonNum];
		if (end < start) return 0; 
		return end-start+1;
	}

	public int getLeftUTRNonCodingLength(int exonNum){
		if (!isTranslated) return ends[exonNum]-starts[exonNum]+1;
		if (!isExonInLeftUTR(exonNum)) {
			return 0;
		}
		int start = starts[exonNum];
		int end = (ends[exonNum] < cdsStart) ? ends[exonNum] : cdsStart;
		return end-start+1;
	}

	public int getRightUTRNonCodingLength(int exonNum){
		if (!isTranslated) return ends[exonNum]-starts[exonNum]+1;
		if (!isExonInRightUTR(exonNum)) {
			return 0;
		}
		int start = (starts[exonNum] > cdsEnd) ? starts[exonNum] : cdsEnd;
		int end = ends[exonNum];
		return end-start+1;
	}

	public int getExonLength(int exonNum){
		return ends[exonNum]-starts[exonNum]+1;
	}

	public String getExonicWhereClause(String table){
		if (getRefSeqTranscript() == null) return "("+table+".pos > 0)";
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for (int i=0 ; i < count ; i++){
			sb.append("(");
			sb.append(table+".pos >= " + (starts[i]-SPLICE_SITE_EXTENDS));
			sb.append(" AND ");
			sb.append(table+".pos <= " + (ends[i]+SPLICE_SITE_EXTENDS));
			sb.append(")");
			if (i+1 < count){
				sb.append(" OR ");
			}
		}
		if (sb.length() == 1){
			sb.append(table+".pos > 0");
		}
		sb.append(")");
		return sb.toString();
	}

	public boolean isExonic(int position, boolean extendsToSpliceSites){
		for (int i=0 ; i < count ; i++){
			if (position >= starts[i]-((extendsToSpliceSites)?SPLICE_SITE_EXTENDS:0) && position <= ends[i]+((extendsToSpliceSites)?SPLICE_SITE_EXTENDS:0)){
				return true;
			}
		}
		return false;
	}

	/**
	 * Return the id of the exon, starting from 0, from smallest to highest pos. 
	 * Call getExonRank() on this id to get the rank, dependent on gene orientation (exon 0 is rank 1 for forward genes, or last rank for reverse genes)
	 * @param position
	 * @param extendsToSpliceSites
	 * @return
	 */
	public int getExonNum(int position, boolean extendsToSpliceSites){
		for (int i=0 ; i < count ; i++){
			if (position >= starts[i]-((extendsToSpliceSites)?SPLICE_SITE_EXTENDS:0) && position <= ends[i]+((extendsToSpliceSites)?SPLICE_SITE_EXTENDS:0)){
				return i;
			}
		}
		return -1;
	}

	/**
	 * Return the id of the intron, starting from 0, from smallest to highest pos. 
	 * Call getIntronRank() on this id to get the rank, dependent on gene orientation (exon 0 is rank 1 for forward genes, or last rank for reverse genes).
	 * @param position
	 * @param extendsToSpliceSites
	 * @return
	 */
	public int getIntronNum(int position, boolean extendsToSpliceSites){
		for (int i=0 ; i < count-1 ; i++){
			if (position >= ends[i]-((extendsToSpliceSites)?SPLICE_SITE_EXTENDS:0) && position <= starts[i+1]+((extendsToSpliceSites)?SPLICE_SITE_EXTENDS:0)){
				return i;
			}
		}
		return -1;
	}

	/**
	 * Return the position in the codon (0, 1 or 2 - start, middle or end) for the given genomic position.
	 * Position is strand dependant, from left to right it's 0-1-2 for forward and 2-1-0 for reverse. 
	 * @param pos the genomic position to consider
	 * @return position in the codon (starting by 0) or -1 if pos is not translated
	 */
	public int getCodonPos(int pos) {
		if (isTranslated && pos > cdsStart && pos <= cdsEnd && isExonic(pos, false)) {
			int ex = getExonNum(pos, false);			
			if (strandPositive) {
				int start = (isExonInLeftUTR(ex)) ? cdsStart+1 : starts[ex];
				return ((pos-start+frames[ex]) % 3);
			}else {
				int end = (isExonInRightUTR(ex)) ? cdsEnd : ends[ex];
				return ((end-pos+frames[ex]) % 3);
			}
		}
		return -1;
	}

	/**
	 * Return the position in the codon (0, 1 or 2 - start, middle or end) for the given genomic position.
	 * Position is strand dependant, from left to right it's 0-1-2 for forward and 2-1-0 for reverse.
	 * This method takes ONE mutation into account before computing position.
	 * @param pos the genomic position to consider
	 * @param mutation Variant modifications to apply before computation
	 * @return position in the codon (starting by 0) or -1 if pos is not translated
	 */
	public int getCodonPos(int pos, Variant mutation) {
		//TODO LONGTERM - SV not shown in the alignment (not necessary, a dedicated visualization tool must be developped for SV)
		//shift is the number of bases deleted (negative) or inserted (positive) if any
		int shift = 0;
		//frameShift is equal to 0, 1 or 2 depending on the number of base the reading frame should be shifted
		int frameShift = 0;
		if (mutation.getVariantType() == VariantType.DEL) {
			shift -= mutation.getAlternativeChangedNucleotides().length();
		}else if (mutation.getVariantType() == VariantType.INS) {
			shift = mutation.getAlternativeChangedNucleotides().length();
		}else {
			shift = 0;
		}
		frameShift = Math.abs(shift)%3;
		int codonPos = getCodonPos(pos);
		if (codonPos != -1 && ((isStrandPositive() && pos >= mutation.getAlternativePosition() || (!isStrandPositive() && pos <= mutation.getAlternativePosition())))) {
			codonPos += 3;
			if (mutation.getVariantType() == VariantType.DEL) codonPos -= frameShift;
			else codonPos += frameShift;
			codonPos = codonPos%3;
		}
		return codonPos;
	}

	/**
	 * Return the DNA sequence of the full codon spanning the given position.
	 * @param pos the genomic position to consider
	 * @return codon sequence spanning the position given as argument or null if pos is not translated
	 */
	public Optional<String> getCodonSequence(int pos) {
		int codonPos = getCodonPos(pos);
		if (codonPos != -1) {
			int exon = getExonNum(pos, false);
			int pos1, pos2, pos3;
			if ((isStrandPositive() && codonPos == 0) || (!isStrandPositive() && codonPos == 2)) {
				if (!isExonic(pos+2, false) && exon == starts.length-1) return Optional.empty(); //Can happen with TR and IG genes (next codon is in another TR/IG "gene", and multiple combinations are possible)
				pos1 = pos;					
				pos2 = (isExonic(pos+1, false)) ? pos+1 :	starts[exon+1];
				pos3 = (isExonic(pos+2, false)) ? pos+2 :	((pos2 == pos+1) ? starts[exon+1] : starts[exon+1]+1);				
			}else if ((isStrandPositive() && codonPos == 2) || (!isStrandPositive() && codonPos == 0)) {
				if (!isExonic(pos-2, false) && exon == 0) return Optional.empty(); //Can happen with TR and IG genes (next codon is in another TR/IG "gene", and multiple combinations are possible)
				pos3 = pos;
				pos2 = isExonic(pos-1, false) ? pos-1 :	ends[exon-1];					
				pos1 = isExonic(pos-2, false) ? pos-2 :	((pos2 == pos-1) ? ends[exon-1] : ends[exon-1]-1);		
			}else {
				if (!isExonic(pos-1, false) && exon == 0) return Optional.empty(); //Can happen with TR and IG genes (next codon is in another TR/IG "gene", and multiple combinations are possible)
				if (!isExonic(pos+1, false) && exon == starts.length-1) return Optional.empty(); 
				pos1 = isExonic(pos-1, false) ? pos-1 :	ends[exon-1];					
				pos2 = pos;
				pos3 = isExonic(pos+1, false) ? pos+1 :	starts[exon+1];				
			}
			return Optional.of("" + getNucleotide(pos1) + getNucleotide(pos2) + getNucleotide(pos3));
		}else{
			return Optional.empty();
		}
	}

	/**
	 * Return the DNA sequence of the full codon spanning the given position.
	 * This method takes ONE mutation into account before computing positions.
	 * @param pos the genomic position to consider
	 * @param mutation Variant modifications to apply before computation
	 * @return codon sequence spanning the position given as argument or null if pos is not translated
	 */
	public Optional<String> getCodonSequence(int pos, Variant mutation) {
		//TODO LONGTERM - SV not shown in the alignment (not necessary, a dedicated visualization tool must be developped for SV)
		//shift is the number of bases deleted (negative) or inserted (positive) if any (zero of SNV)
		int shift = 0;
		//frameShift is equal to 0, 1 or 2 depending on the number of base the reading frame should be shifted
		int frameShift = 0;
		if (mutation.getVariantType() == VariantType.DEL) {
			shift -= mutation.getAlternativeChangedNucleotides().length();
		}else if (mutation.getVariantType() == VariantType.INS) {
			shift = mutation.getAlternativeChangedNucleotides().length();
		}else {
			shift = 0;
		}
		frameShift = Math.abs(shift)%3;
		//return Optional.empty() if we are inside a deletion
		if (mutation.getVariantType() == VariantType.DEL && pos >= mutation.getAlternativePosition() && pos < (mutation.getAlternativePosition() + Math.abs(shift))) {
			return Optional.empty();
		}
		//prepare an array with the positions directly affected by an insertion (will be displayed as +s with the AA in the tooltip)
		int[] insertionPos = new int[0];
		boolean isInsideInsertion = false;
		if (mutation.getVariantType() == VariantType.INS) {
			int neededBases = Math.abs(frameShift-3) % 3;
			insertionPos = new int[3+neededBases];
			int mutCodonPos = getCodonPos(mutation.getAlternativePosition());
			for (int i=0; i < 3+neededBases ; i++) {
				int p = (isStrandPositive()) ? mutation.getAlternativePosition()-mutCodonPos+i : mutation.getAlternativePosition()-neededBases-Math.abs(mutCodonPos-2)+i;
				insertionPos[i] = p;
				if (p == pos) {
					isInsideInsertion = true; 
				}
			}
		}
		int codonPos = getCodonPos(pos, mutation);
		if (codonPos != -1) {
			int[] positions;
			if (isInsideInsertion) {
				positions = insertionPos;
			}else {
				positions = new int[3];
				int exon = getExonNum(pos, false);
				if ((isStrandPositive() && codonPos == 0) || (!isStrandPositive() && codonPos == 2)) {
					if (!isExonic(pos+positions.length-1, false) && exon == starts.length-1) return Optional.empty(); //Can happen with TR and IG genes (next codon is in another TR/IG "gene", and multiple combinations are possible)
					int nextExonOffset = -1;
					for (int i=0 ; i < positions.length ; i++) {
						if (isExonic(pos+i, false)) {
							positions[i] = pos+i;
						}else {
							nextExonOffset++;
							positions[i] = starts[exon+1] + nextExonOffset;
						}
					}
				}else if ((isStrandPositive() && codonPos == 2) || (!isStrandPositive() && codonPos == 0)) {
					if (!isExonic(pos-positions.length-1, false) && exon == 0) return Optional.empty(); //Can happen with TR and IG genes (next codon is in another TR/IG "gene", and multiple combinations are possible)
					int nextExonOffset = -1;
					for (int i=positions.length-1 ; i >= 0 ; i--) {
						if (isExonic(pos-(positions.length-i-1), false)) {
							positions[i] = pos-(positions.length-i-1);
						}else {
							nextExonOffset++;
							positions[i] = ends[exon-1] - nextExonOffset;
						}
					}
				}else {
					if (!isExonic(pos-1, false) && exon == 0) return Optional.empty(); //Can happen with TR and IG genes (next codon is in another TR/IG "gene", and multiple combinations are possible)
					if (!isExonic(pos+1, false) && exon == starts.length-1) return Optional.empty(); 
					positions[0] = isExonic(pos-1, false) ? pos-1 :	ends[exon-1];					
					positions[1] = pos;
					positions[2] = isExonic(pos+1, false) ? pos+1 :	starts[exon+1];				
				}				
			}
			char[] bases = new char[positions.length];
			switch(mutation.getVariantType()) {
			case SNV:
			case MNV:
				int changeSize = mutation.getAlternativeChangedNucleotides().length();
				for (int i=0 ; i < positions.length ; i++) {
					bases[i] = (positions[i] >= mutation.getAlternativePosition() && positions[i] < mutation.getAlternativePosition()+changeSize) 
							? mutation.getAlternativeChangedNucleotides().charAt(positions[i]-mutation.getAlternativePosition()) 
									: getNucleotide(positions[i]);
				}
				break;
			case DEL:
				if (pos < mutation.getAlternativePosition()) {
					for (int i=0 ; i < positions.length ; i++) {
						bases[i] = (positions[i] < mutation.getAlternativePosition()) ? getNucleotide(positions[i]) : getNucleotide(positions[i]+Math.abs(shift));
					}
				}else {
					for (int i=0 ; i < positions.length ; i++) {
						bases[i] = (positions[i] > (mutation.getAlternativePosition()+Math.abs(shift)-1)) ? getNucleotide(positions[i]) : getNucleotide(positions[i]+shift);
					}
				}
				break;
			case INS:
				if (isInsideInsertion) {
					StringBuilder sb = new StringBuilder();
					for (int p : insertionPos) {
						sb.append(getNucleotide(p));
						if (p == mutation.getAlternativePosition()) {
							sb.append(mutation.getAlternativeChangedNucleotides());
						}
					}
					return Optional.of(sb.toString());
				}else {
					for (int i=0 ; i < positions.length ; i++) {
						bases[i] = getNucleotide(positions[i]);
					}
				}
				break;
			default:
				return Optional.empty();

			}
			return Optional.of(new String(bases));
		}else{
			return Optional.empty();
		}

	}

	/**
	 * Return the amino acid spanning the given position (single letter code, e.g. K if a Lysine span the given position).
	 * @param pos the genomic position to consider
	 * @return amino acid spanning the position given as argument or "#" if pos is not translated
	 */
	public String getAminoAcid(int pos) {
		Optional<String> seq = getCodonSequence(pos);
		if (seq.isPresent())
			return "" + ((isStrandPositive()) ? Tools.nucleotidesToProtein(seq.get()) : Tools.nucleotidesToProtein(Tools.reverseComplement(seq.get())));
		else
			return "#";
	}

	/**
	 * Return the amino acid spanning the given position (single letter code, e.g. K if a Lysine span the given position).
	 * This method takes ONE mutation into account before computing positions.
	 * @param pos the genomic position to consider
	 * @param mutation Variant modifications to apply before computation
	 * @return amino acid spanning the position given as argument or "#" if pos is not translated
	 */
	public String getAminoAcid(int pos, Variant mutation) {
		Optional<String> seq = getCodonSequence(pos, mutation);
		if (seq.isPresent() && (seq.get().length() % 3) == 0) {
			StringBuilder sb = new StringBuilder();
			if (isStrandPositive()) {
				for (int i=0 ; i < seq.get().length() ; i+=3) {
					sb.append(Tools.nucleotidesToProtein(seq.get().substring(i, i+3)));
				}				
			}else {
				for (int i=seq.get().length() ; i > 0 ; i-=3) {
					sb.append(Tools.nucleotidesToProtein(Tools.reverseComplement(seq.get().substring(i-3, i))));			
				}
			}
			return sb.toString();
		}else{
			return "#";
		}
	}
}
