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

import java.util.ArrayList;
import java.util.List;

public class SNPEffect {

	public enum Input {TXT,VCF_EFF,VCF_ANN}

	public enum Impact {HIGH,MODERATE,LOW,MODIFIER}
	public enum VariantType {SNV,MNV,INS,DEL,SV}
	public enum Zygosity {Homozygous,Heterozygous,Reference}

	public enum Effect {
		//HIGH
		CHROMOSOME_LARGE_DELETION(500, "chromosome_number_variation", new String[]{"chromosome"},"A large parte (over 1%) of the chromosome was deleted","",Impact.HIGH,1),
		CHROMOSOME_LARGE_INVERSION(500, "inversion", new String[]{},"Inversion of a large chromosome segment (over 1% or 1,000,000 bases).","",Impact.HIGH,2),
		CHROMOSOME_LARGE_DUPLICATION(500, "duplication", new String[]{},"Duplication of a large chromosome segment (over 1% or 1,000,000 bases)","",Impact.HIGH,3),
		GENE_REARRANGEMENT(500, "rearranged_at_DNA_level", new String[]{},"Rearrangement affecting one or more genes.","",Impact.HIGH,4),
		GENE_DELETED(500, "feature_ablation", new String[]{},"Deletion of a gene.","",Impact.HIGH,5),
		TRANSCRIPT_DELETED(500, "transcript_ablation", new String[]{},"Deletion of a transcript.","",Impact.HIGH,6),
		EXON_DELETED(500, "exon_loss_variant", new String[]{},"A deletion removes the whole exon.","",Impact.HIGH,7),
		EXON_DELETED_PARTIAL(500, "exon_loss_variant", new String[]{},"Deletion affecting part of an exon.","",Impact.HIGH,8),
		GENE_FUSION(500, "gene_fusion", new String[]{},"Fusion of two genes.","",Impact.HIGH,9),
		GENE_FUSION_REVERESE(500, "bidirectional_gene_fusion", new String[]{},"Fusion of two genes in opposite directions.","",Impact.HIGH,10),
		GENE_FUSION_HALF(500, "transcript_ablation", new String[]{},"Fusion of one gene and an intergenic region.","",Impact.HIGH,11),
		FRAME_SHIFT(400, "frameshift_variant", new String[]{},"Insertion or deletion causes a frame shift","An indel size is not multple of 3",Impact.HIGH,12),
		STOP_GAINED(400, "stop_gained", new String[]{},"Variant causes a STOP codon","Cag/Tag, Q/*",Impact.HIGH,13),
		STOP_LOST(400, "stop_lost", new String[]{},"Variant causes stop codon to be mutated into a non-stop codon","Tga/Cga, */R",Impact.HIGH,14),
		START_LOST(400, "start_lost", new String[]{},"Variant causes start codon to be mutated into a non-start codon.","aTg/aGg, M/R",Impact.HIGH,15),
		SPLICE_SITE_ACCEPTOR(300, "splice_acceptor_variant", new String[]{},"The variant hits a splice acceptor site (defined as two bases before exon start, except for the first exon).","",Impact.HIGH,16),
		SPLICE_SITE_DONOR(300, "splice_donor_variant", new String[]{},"The variant hits a Splice donor site (defined as two bases after coding exon end, except for the last exon).","",Impact.HIGH,17),
		RARE_AMINO_ACID(400, "rare_amino_acid_variant", new String[]{},"The variant hits a rare amino acid thus is likely to produce protein loss of function.","",Impact.HIGH,18),
		EXON_DUPLICATION(500, "duplication", new String[]{},"Duplication of an exon.","",Impact.HIGH,19),
		EXON_DUPLICATION_PARTIAL(500, "duplication", new String[]{},"Duplication affecting part of an exon.","",Impact.HIGH,20),
		EXON_INVERSION(500, "inversion", new String[]{},"Inversion of an exon.","",Impact.HIGH,21),
		EXON_INVERSION_PARTIAL(500, "inversion", new String[]{},"Inversion affecting part of an exon.","",Impact.HIGH,22),
		PROTEIN_PROTEIN_INTERACTION_LOCUS(500, "protein_protein_contact", new String[]{},"Protein-Protein interaction loci.","",Impact.HIGH,23),
		PROTEIN_STRUCTURAL_INTERACTION_LOCUS(500, "structural_interaction_variant", new String[]{},"Within protein interacion loci.","two AA that are in contact within the same protein, possibly helping structural conformation",Impact.HIGH,24),
		//MODERATE
		NON_SYNONYMOUS_CODING(0, "missense_variant", new String[]{},"Variant causes a codon that produces a different amino acid","Tgg/Cgg, W/R",Impact.MODERATE,25),
		GENE_DUPLICATION(0, "duplication", new String[]{},"Duplication of a gene.","",Impact.MODERATE,26),
		TRANSCRIPT_DUPLICATION(0, "duplication", new String[]{},"","",Impact.MODERATE,27),
		UTR_5_DELETED(0, "5_prime_UTR_truncation&exon_loss_variant", new String[]{"5_prime_UTR_truncation+exon_loss_variant"},"The variant deletes and exon which is in the 5'UTR of the transcript","",Impact.MODERATE,28),
		UTR_3_DELETED(0, "3_prime_UTR_truncation&exon_loss_variant", new String[]{"3_prime_UTR_truncation+exon_loss"},"The variant deletes and exon which is in the 3'UTR of the transcript","",Impact.MODERATE,29),
		SPLICE_SITE_BRANCH_U12(0, "splice_branch_variant", new String[]{},"A variant affective putative (Lariat) branch point from U12 splicing machinery, located in the intron","",Impact.MODERATE,30),
		GENE_INVERSION(0, "inversion", new String[]{},"","",Impact.MODERATE,31),
		TRANSCRIPT_INVERSION(0, "inversion", new String[]{},"","",Impact.MODERATE,32),
		CODON_INSERTION(0, "conservative_inframe_insertion", new String[]{"inframe_insertion"},"One or many codons are inserted","An insert multiple of three in a codon boundary",Impact.MODERATE,33),
		CODON_CHANGE_PLUS_CODON_INSERTION(0, "disruptive_inframe_insertion", new String[]{},"One codon is changed and one or many codons are inserted","An insert of size multiple of three, not at codon boundary",Impact.MODERATE,34),
		CODON_DELETION(0, "conservative_inframe_deletion", new String[]{"inframe_deletion"},"One or many codons are deleted","A deletion multiple of three at codon boundary",Impact.MODERATE,35),
		CODON_CHANGE_PLUS_CODON_DELETION(0, "disruptive_inframe_deletion", new String[]{},"One codon is changed and one or more codons are deleted","A deletion of size multiple of three, not at codon boundary",Impact.MODERATE,36),
		//LOW
		NON_SYNONYMOUS_STOP(0, "stop_retained_variant", new String[]{},"Variant causes stop codon to be mutated into another codon that is not a stop codon.","",Impact.LOW,37),
		NON_SYNONYMOUS_START(0, "initiator_codon_variant", new String[]{},"Variant causes start codon to be mutated into another codon that is not a start codon.","",Impact.LOW,38),
		SPLICE_SITE_REGION(0, "splice_region_variant", new String[]{},"Variant causes a change within the region of a splice site (1-3bps into an exon or 3-8bps into an intron).","",Impact.LOW,39),
		SPLICE_SITE_BRANCH(0, "splice_branch_variant", new String[]{},"A varaint affective putative (Lariat) branch point, located in the intron","",Impact.LOW,40),
		SYNONYMOUS_CODING(0, "synonymous_variant", new String[]{},"Variant causes a codon that produces the same amino acid","Ttg/Ctg, L/L",Impact.LOW,41),
		SYNONYMOUS_START(0, "start_retained_variant", new String[]{"start_retained"},"Variant causes start codon to be mutated into another start codon.","Ttg/Ctg, L/L (TTG and CTG can be START codons)",Impact.LOW,42),
		SYNONYMOUS_STOP(0, "stop_retained_variant", new String[]{},"Variant causes stop codon to be mutated into another stop codon.","taA/taG, */*",Impact.LOW,43),
		CODON_CHANGE(0, "coding_sequence_variant", new String[]{},"One or many codons are changed","An MNV of size multiple of 3",Impact.LOW,44),
		START_GAINED(0, "5_prime_UTR_premature_start_codon_gain_variant", new String[]{},"A variant in 5'UTR region produces a three base sequence that can be a START codon.","",Impact.LOW,45),
		MOTIF(0, "TF_binding_site_variant", new String[]{},"","",Impact.LOW,46),
		MOTIF_DELETED(0, "TFBS_ablation", new String[]{},"","",Impact.LOW,47),
		FEATURE_FUSION(0, "feature_fusion", new String[]{},"","",Impact.LOW,48),
		//MODIFIER
		FRAME_SHIFT_BEFORE_CDS_START(0, "start_retained_variant", new String[]{},"","",Impact.MODIFIER,49),
		FRAME_SHIFT_AFTER_CDS_END(0, "stop_retained_variant", new String[]{},"","",Impact.MODIFIER,50),
		UTR_5_PRIME(0, "5_prime_UTR_variant", new String[]{},"Variant hits 5'UTR region","",Impact.MODIFIER,51),
		UTR_3_PRIME(0, "3_prime_UTR_variant", new String[]{},"Variant hits 3'UTR region","",Impact.MODIFIER,52),
		REGULATION(0, "regulatory_region_variant", new String[]{},"","",Impact.MODIFIER,53),
		MICRO_RNA(0, "miRNA", new String[]{},"Variant affects an miRNA","",Impact.MODIFIER,54),
		UPSTREAM(0, "upstream_gene_variant", new String[]{},"Upstream of a gene (default length: 5K bases)","",Impact.MODIFIER,55),
		DOWNSTREAM(0, "downstream_gene_variant", new String[]{},"Downstream of a gene (default length: 5K bases)","",Impact.MODIFIER,56),
		NEXT_PROT(0, "sequence_feature", new String[]{},"A 'NextProt' based annotation. Details are provided in the 'feature type' sub-field (ANN), or in the effect details (EFF).","",Impact.MODIFIER,57),
		INTRON_CONSERVED(0, "conserved_intron_variant", new String[]{},"The variant is in a highly conserved intronic region","",Impact.MODIFIER,58),
		INTRON(0, "intron_variant", new String[]{},"Variant hist an intron. Technically, hits no exon in the transcript.","",Impact.MODIFIER,59),
		INTRAGENIC(0, "intragenic_variant", new String[]{},"The variant hits a gene, but no transcripts within the gene","",Impact.MODIFIER,60),
		INTERGENIC_CONSERVED(0, "conserved_intergenic_variant", new String[]{},"The variant is in a highly conserved intergenic region","",Impact.MODIFIER,61),
		INTERGENIC(0, "intergenic_region", new String[]{},"The variant is in an intergenic region","",Impact.MODIFIER,62),
		CDS(0, "coding_sequence_variant", new String[]{},"The variant hits a CDS.","",Impact.MODIFIER,63),
		EXON(0, "non_coding_transcript_exon_variant", new String[]{"exon_variant","exon_region"},"The variant hits an exon.","",Impact.MODIFIER,64),
		TRANSCRIPT(0, "non_coding_transcript_variant", new String[]{"transcript"},"The variant hits a transcript.","",Impact.MODIFIER,65),
		GENE(0, "gene_variant", new String[]{},"The variant hits a gene.","",Impact.MODIFIER,66),
		SEQUENCE(0, "", new String[]{},"","",Impact.MODIFIER,67),
		CHROMOSOME_ELONGATION(0, "feature_elongation", new String[]{},"","",Impact.MODIFIER,68),
		CUSTOM(0, "custom", new String[]{},"","",Impact.MODIFIER,69),
		CHROMOSOME(0, "chromosome", new String[]{},"","",Impact.MODIFIER,70),
		GENOME(0, "", new String[]{},"","",Impact.MODIFIER,71),
		//Not anymore in snpEff 4.1
		TF_BINDING_SITE(0, "TF_binding_site_variant", new String[]{},"Variant affects a transcription factor binding site","",Impact.LOW,72),
		SEQUENCE_FEATURE(0, "sequence_feature", new String[]{},"","",Impact.LOW,73),
		NON_CODING_EXON(0, "non_coding_exon_variant", new String[]{},"Variant hits a non coding exon.","",Impact.MODIFIER,74),
		WITHIN_NON_CODING_GENE(0, "", new String[]{},"","",Impact.MODIFIER,75),
		//Used when no conversion is found (so probably a good time to update this class ...)
		NONE(0, "", new String[]{},"","",Impact.MODIFIER,76),
		;
		private final String sequenceOntology;
		private final String[] alternativeOntology; //Used to keep old conversions, so an older version of snpEff may still work.
		private final String note;
		private final String example;
		private final Impact impact;
		private final int consensusPredictionStartingValue;
		private final int priority; //Highest impact first, following SnpEff source code
		Effect(int consensusPredictionStartingValue, String sequenceOntology, String[] alternativeOntology, String note, String example, Impact impact, int priority){
			this.sequenceOntology = sequenceOntology;
			this.alternativeOntology = alternativeOntology;
			this.note = note;
			this.example = example;
			this.impact = impact;
			this.consensusPredictionStartingValue = consensusPredictionStartingValue;
			this.priority = priority;
		}
		public String getSequenceOntology(){return sequenceOntology;}
		public String[] getAnternativeOntologyTerms(){return alternativeOntology;}
		public String getNote(){return note;}
		public String getExample(){return example;}
		public Impact getImpact(){return impact;}
		public int getConsensusPredictionStartingValue(){return consensusPredictionStartingValue;}  
		public int getPriority(){return priority;}  
		public static Effect getEffect(String sequenceOntologyTerm){
			for (Effect effect : values()){
				if (effect.getSequenceOntology().equalsIgnoreCase(sequenceOntologyTerm)) {
					return effect;
				}
				for (String altOnto : effect.getAnternativeOntologyTerms()) {
					if (altOnto.equalsIgnoreCase(sequenceOntologyTerm)) {
						return effect;
					}
				}
					
			}
			//System.err.println(sequenceOntologyTerm + " was not found in snpEff effects");
			return NONE;
		}
		public Effect getGeneRegion() {
			switch (this) {
			case NONE:
			case CHROMOSOME:
			case CHROMOSOME_LARGE_DELETION:
			case CHROMOSOME_LARGE_DUPLICATION:
			case CHROMOSOME_LARGE_INVERSION:
			case CHROMOSOME_ELONGATION:
			case CUSTOM:
			case SEQUENCE:
			case SEQUENCE_FEATURE:
				return Effect.CHROMOSOME;

			case INTERGENIC:
			case INTERGENIC_CONSERVED:
			case FEATURE_FUSION:
				return Effect.INTERGENIC;

			case UPSTREAM:
				return Effect.UPSTREAM;

			case UTR_5_PRIME:
			case UTR_5_DELETED:
			case START_GAINED:
				return Effect.UTR_5_PRIME;

			case SPLICE_SITE_ACCEPTOR:
				return Effect.SPLICE_SITE_ACCEPTOR;

			case SPLICE_SITE_BRANCH_U12:
			case SPLICE_SITE_BRANCH:
				return Effect.SPLICE_SITE_BRANCH;

			case SPLICE_SITE_DONOR:
				return Effect.SPLICE_SITE_DONOR;

			case SPLICE_SITE_REGION:
				return Effect.SPLICE_SITE_REGION;

			case TRANSCRIPT_DELETED:
			case TRANSCRIPT_DUPLICATION:
			case TRANSCRIPT_INVERSION:
			case INTRAGENIC:
			case NEXT_PROT:
			case TRANSCRIPT:
			case CDS:
			case NON_CODING_EXON:
			case WITHIN_NON_CODING_GENE:
				return Effect.TRANSCRIPT;

			case GENE:
			case GENE_DELETED:
			case GENE_DUPLICATION:
			case GENE_FUSION:
			case GENE_FUSION_HALF:
			case GENE_FUSION_REVERESE:
			case GENE_INVERSION:
			case GENE_REARRANGEMENT:
				return Effect.GENE;

			case EXON:
			case EXON_DELETED:
			case EXON_DELETED_PARTIAL:
			case EXON_DUPLICATION:
			case EXON_DUPLICATION_PARTIAL:
			case EXON_INVERSION:
			case EXON_INVERSION_PARTIAL:
			case NON_SYNONYMOUS_START:
			case NON_SYNONYMOUS_CODING:
			case SYNONYMOUS_CODING:
			case SYNONYMOUS_START:
			case FRAME_SHIFT:
			case FRAME_SHIFT_AFTER_CDS_END:
			case FRAME_SHIFT_BEFORE_CDS_START:
			case CODON_CHANGE:
			case CODON_INSERTION:
			case CODON_CHANGE_PLUS_CODON_INSERTION:
			case CODON_DELETION:
			case CODON_CHANGE_PLUS_CODON_DELETION:
			case START_LOST:
			case STOP_GAINED:
			case SYNONYMOUS_STOP:
			case NON_SYNONYMOUS_STOP:
			case STOP_LOST:
			case RARE_AMINO_ACID:
			case PROTEIN_PROTEIN_INTERACTION_LOCUS:
			case PROTEIN_STRUCTURAL_INTERACTION_LOCUS:
				return Effect.EXON;

			case INTRON:
			case INTRON_CONSERVED:
				return Effect.INTRON;

			case UTR_3_PRIME:
			case UTR_3_DELETED:
				return Effect.UTR_3_PRIME;

			case DOWNSTREAM:
				return Effect.DOWNSTREAM;

			case REGULATION:
				return Effect.REGULATION;

			case MOTIF:
			case MOTIF_DELETED:
			case TF_BINDING_SITE:
				return Effect.MOTIF;

			case MICRO_RNA:
				return Effect.MICRO_RNA;

			case GENOME:
				return Effect.GENOME;

			default:
				throw new RuntimeException("Unknown gene region for effect type: '" + this + "'");
			}
		}
	}

	public enum Codon {
		TTT("F"),
		TTC("F"),
		TTA("L"),
		TTG("L"),
		TCT("S"),
		TCC("S"),
		TCA("S"),
		TCG("S"),
		TAT("Y"),
		TAC("Y"),
		TAA("*"),
		TAG("*"),
		TGT("C"),
		TGC("C"),
		TGA("*"),
		TGG("W"),
		CTT("L"),
		CTC("L"),
		CTA("L"),
		CTG("L"),
		CCT("P"),
		CCC("P"),
		CCA("P"),
		CCG("P"),
		CAT("H"),
		CAC("H"),
		CAA("Q"),
		CAG("Q"),
		CGT("R"),
		CGC("R"),
		CGA("R"),
		CGG("R"),
		ATT("I"),
		ATC("I"),
		ATA("I"),
		ATG("M"),
		ACT("T"),
		ACC("T"),
		ACA("T"),
		ACG("T"),
		AAT("N"),
		AAC("N"),
		AAA("K"),
		AAG("K"),
		AGT("S"),
		AGC("S"),
		AGA("R"),
		AGG("R"),
		GTT("V"),
		GTC("V"),
		GTA("V"),
		GTG("V"),
		GCT("A"),
		GCC("A"),
		GCA("A"),
		GCG("A"),
		GAT("D"),
		GAC("D"),
		GAA("E"),
		GAG("E"),
		GGT("G"),
		GGC("G"),
		GGA("G"),
		GGG("G"),
		;
		private final String AA;
		Codon(String AA){
			this.AA = AA;
		}
		public String toAA(){return AA;}
	}

	//other_transcripts_snpeff table data
	private int id;
	private long variant_id;						//Unique identifier of the variant
	private int project_id;
	private VariantType variant_type;		//Type of change {SNV, MNV, INS, DEL, SV}
	private String transcript_ensembl;	//Feature ID: Depending on the annotation, this may be: Transcript ID (preferably using version number), Motif ID, miRNA, ChipSeq peak, Histone mark, etc. Note: Some features may not have ID (e.g. histone marks from custom Chip-Seq experiments may not have a unique ID).
	private int transcript_version;			//Transcript may include the version number (e.g. ENST00000261799.9)
	private Effect snpeff_effect;				//Most damaging effect of this variant predicted by SnpEff.
	private List<Effect> snpeff_all_effect = new ArrayList<SNPEffect.Effect>(); 
	private Impact snpeff_impact;				//Impact of the effect predicted by SnpEff.

	//main table information
	private String geneName;						//Common gene name (HGNC). Optional: use closest gene when the variant is “intergenic”.
	private String geneId;							//Gene ID (usually ENSEMBL)
	private String bioType;							//Transcript biotype: The bare minimum is at least a description on whether the transcript is {“Coding”, “Noncoding”}. Whenever possible, use ENSEMBL biotypes.
	private int exonRank;								//Rank / total: Exon or Intron rank number of exons or introns.
	private int exonTotal;							//Rank / total: Exon or Intron total number of exons or introns.
	private String hgvsDna = "";				//Variant using HGVS notation (DNA level)
	private String hgvsProtein = "";		//If variant is coding, this field describes the variant using HGVS notation (Protein level). Since transcript ID is already mentioned in ‘feature ID’, it may be omitted here.
	private int cdnaPos = 0;						//cDNA_position : Position in cDNA (one based).
	private int cdnaLen = 0;						//cDNA_len: trancript’s cDNA length (one based).
	private int cdsPos = 0;							//CDS_position: Position of coding bases (one based includes START and STOP codons).
	private int cdsLen = 0;							//CDS_len: Number of coding bases (one based includes START and STOP codons).
	private int protPos = 0;						//Protein_position : Position (one based, including START, but not STOP).
	private int protLen = 0;						//Protein_len: Number of AA (one based, including START, but not STOP).

	//information obtained before version 4 (and now totally unused)
	private String chromosome;					//Chromosome name (usually without any leading 'chr' string)
	private int position = -1;					//One based position
	private String reference;						//Reference
	private String change;							//Sequence change
	@SuppressWarnings("unused")
	private Zygosity zygosity;					//Is this homozygous or heterozygous {Hom, Het}
	@SuppressWarnings("unused")
	private String exonId;							//Exon ID (usually ENSEMBL)
	private String oldAA = "";					//Old amino acid.
	private String newAA = "";					//New amino acid.
	private String oldCodon = "";				//Old codon.
	private String newCodon = "";				//New codon.
	@SuppressWarnings("unused")
	private double confidence;					//Quality score (from input file)
	@SuppressWarnings("unused")
	private int coverage;								//Coverage (from input file)
	@SuppressWarnings("unused")
	private String codonDegeneracy = "";//Codon degenaracy (see below).
	@SuppressWarnings("unused")
	private String customIntervalId = "";//If any custom interval was used, add the IDs here (may be more than one).
	@SuppressWarnings("unused")
	private String codonsAround = "";			
	@SuppressWarnings("unused")
	private String aasAround = "";					

	//unused information from version 4+
	private String alternative;					//Compound variants: two or more variants affecting the annotations (e.g. two consecutive SNPs conforming a MNV, two consecutive frame_shift variants that “recover” the frame). In this case, the Allele field should include a reference to the other variant/s included in the annotation
	@SuppressWarnings("unused")
	private String feature_type; 				//Feature type: Which type of feature is in the next field (e.g. transcript, motif, miRNA, etc.). It is preferred to use Sequence Ontology (SO) terms, but ‘custom’ (user defined) are allowed. ANN=A|stop_gained|HIGH|||transcript|... Tissue specific features may include cell type / tissue information separated by semicolon e.g.: ANN=A|histone_binding_site|LOW|||H3K4me3:HeLa-S3|...
	@SuppressWarnings("unused")
	private String effectDetails = "";	//All items in this field are options, so the field could be empty. Up/Downstream: Distance to first / last codon Intergenic: Distance to closest gene Distance to closest Intron boundary in exon (+/- up/downstream). If same, use positive number. Distance to closest exon boundary in Intron (+/- up/downstream) Distance to first base in MOTIF Distance to first base in miRNA Distance to exon-intron boundary in splice_site or splice _region ChipSeq peak: Distance to summit (or peak center) Histone mark / Histone state: Distance to summit (or peak center)
	private String warnings;						//Errors, Warnings or Information messages: Add errors, warnings or informative message that can affect annotation accuracy. It can be added using either ‘codes’ (as shown in column 1, e.g. W1) or ‘message types’ (as shown in column 2, e.g. WARNING_REF_DOES_NOT_MATCH_GENOME). All these errors, warnings or information messages messages are optional.

	public SNPEffect(String line, Input input){
		this(line, input, null, -1, null, null, null);
	}

	public SNPEffect(String line, Input input, String chr, int pos, String ref, String alt, VariantType chType){
		chromosome = chr;
		position = pos;
		reference = ref;
		alternative = alt;
		variant_type = chType;
		if (input == Input.VCF_ANN){
			String[] effects = (line).split("\\|");
			int c = 0;
			if (c < effects.length) alternative = effects[c++];
			if (c < effects.length) {
				String[] annotations = effects[c++].split("\\&");				
				snpeff_effect = Effect.getEffect(annotations[0]);
				snpeff_all_effect.add(snpeff_effect);
				for (int j=1 ; j < annotations.length ; j++){
					Effect eff = Effect.getEffect(annotations[j]);
					snpeff_all_effect.add(eff);
					if (eff.getPriority() < snpeff_effect.getPriority()) snpeff_effect = eff;
				}
			}
			if (c < effects.length) snpeff_impact = Impact.valueOf(effects[c++]);
			if (c < effects.length) geneName = effects[c++];
			if (c < effects.length) geneId = effects[c++];
			if (c < effects.length) feature_type = effects[c++];
			if (c < effects.length) setTranscriptIdAndVersion(effects[c++]);;
			if (c < effects.length) bioType = effects[c++];
			if (c < effects.length) {
				String rank = effects[c++];
				if (rank.length() > 0){
					exonRank = Integer.parseInt(rank.split("/")[0]);
					exonTotal = Integer.parseInt(rank.split("/")[1]);
				}
			}
			if (c < effects.length) hgvsDna = effects[c++];
			if (c < effects.length) hgvsProtein = effects[c++];
			if (c < effects.length) {
				String cdna = effects[c++];
				if (cdna.length() > 0){
					cdnaPos = Integer.parseInt(cdna.split("/")[0]);
					cdnaLen = Integer.parseInt(cdna.split("/")[1]);
				}
			}
			if (c < effects.length) {
				String cds = effects[c++];
				if (cds.length() > 0){
					cdsPos = Integer.parseInt(cds.split("/")[0]);
					cdsLen = Integer.parseInt(cds.split("/")[1]);
				}
			}
			if (c < effects.length) {
				String prot = effects[c++];
				if (prot.length() > 0){
					protPos = Integer.parseInt(prot.split("/")[0]);
					protLen = Integer.parseInt(prot.split("/")[1]);
				}
			}
			if (c < effects.length) effectDetails = effects[c++];
			if (c < effects.length) warnings = effects[c++];
		}else if (input == Input.TXT){
			String[] effects = (line+"*").split("\t");
			int c = 0;
			chromosome = effects[c++];
			position = Integer.parseInt(effects[c++]);
			reference = effects[c++];
			change = effects[c++];
			//variant_type = VariantType.valueOf(effects[c++]); //SNP and MNP are now SNV and MNV
			String homozygous = effects[c++];
			if (homozygous.equals("Hom")) zygosity = Zygosity.Homozygous;
			else zygosity = Zygosity.Heterozygous;
			confidence = Double.parseDouble(effects[c++]);
			coverage = Integer.parseInt(effects[c++]);
			warnings = effects[c++];
			geneId = effects[c++];
			geneName = effects[c++];
			bioType = effects[c++];
			setTranscriptIdAndVersion(effects[c++]);
			exonId = effects[c++];
			String exrank = effects[c++];
			if (exrank.length() > 0) exonRank = Integer.parseInt(exrank);
			String[] eff = effects[c++].split(":"); 
			snpeff_effect = Effect.valueOf(eff[0]);
			switch (snpeff_effect){
			case NON_SYNONYMOUS_CODING:
				effectDetails = "Missense mutation";
				break;
			case STOP_GAINED:
				effectDetails = "Nonsense mutation";
				break;
			case SYNONYMOUS_CODING:
			case SYNONYMOUS_START:
			case NON_SYNONYMOUS_START:
			case SYNONYMOUS_STOP:
			case NON_SYNONYMOUS_STOP:
				effectDetails = "Silent mutation";
				break;
			default:
				break;
			}
			snpeff_impact = snpeff_effect.getImpact();
			if (eff.length > 1) effectDetails = eff[1].trim();
			String[] aa = effects[c++].split("/"); 
			if (aa.length > 0) oldAA = aa[0];
			if (aa.length > 1) newAA = aa[1];
			String[] codon = effects[c++].split("/"); 
			if (codon.length > 0) oldCodon = codon[0];
			if (codon.length > 1) newCodon = codon[1];
			String cnum = effects[c++];
			if (cnum.length() > 0) protPos = Integer.parseInt(cnum);
			codonDegeneracy = effects[c++];
			String cdssz = effects[c++];
			if (cdssz.length() > 0) cdsLen = Integer.parseInt(cdssz);
			codonsAround = effects[c++];
			aasAround = effects[c++];
			customIntervalId = effects[c++];
			if (oldAA.startsWith("p.")){
				hgvsProtein = oldAA;
				hgvsDna = newAA;
				oldAA = codonsToAAs(oldCodon);
				newAA = codonsToAAs(newCodon);
			}else if (oldAA.startsWith("c.")){
				hgvsDna = oldAA;
				oldAA = codonsToAAs(oldCodon);
				newAA = codonsToAAs(newCodon);			
			}
		}
	}

	private String codonsToAAs(String codons){
		if (codons.equals("-")) return "-";
		String result = "";
		String codon = "";
		for (int i=0 ; i < codons.length()+1 ; i++){
			if (i%3 == 0){
				if (codon.length() == 3){
					Codon c = Codon.valueOf(codon.toUpperCase());
					result += c.toAA();
				}
				if (i < codons.length()){
					codon = ""+codons.charAt(i);
				}
			}else{
				if (i < codons.length()){
					codon += codons.charAt(i);
				}else{
					result += "?";
				}
			}
		}
		return result;
	}

	public String getChromosome() {
		return chromosome;
	}

	public String getAlternative(){
		return alternative;
	}


	public VariantType getVariantType() {
		return variant_type;
	}



	public String getWarnings() {
		return warnings;
	}



	public String getGeneId() {
		return geneId;
	}



	public String getGeneName() {
		return geneName;
	}



	public String getBioType() {
		return bioType;
	}



	public String getTrancriptId() {
		return transcript_ensembl;
	}

	public int getTrancriptVersion() {
		return transcript_version;
	}
	
	public void setTranscriptIdAndVersion(String input) {
		String[] split = input.split("\\.");
		transcript_ensembl = split[0];
		transcript_version = 1;
		if (split.length > 1) {
			try {
				transcript_version = Integer.parseInt(split[1]);
			}catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public int getExonRank() {
		return exonRank;
	}

	public int getExonTotal() {
		return exonTotal;
	}

	public int getcDnaPosition() {
		return cdnaPos;
	}

	public int getcDnaLength() {
		return cdnaLen;
	}

	public int getCDSPosition() {
		return cdsPos;
	}

	public int getCDSLength() {
		return cdsLen;
	}

	public int getProteinPosition() {
		return protPos;
	}

	public int getProteinLength() {
		return protLen;
	}


	public Effect getEffect() {
		return snpeff_effect;
	}

	public List<Effect> getAllEffects() {
		return new ArrayList<>(snpeff_all_effect);
	}
	
	public String getAllEffectsAsString() {
		String effects = "";
		for (int i=0 ; i < snpeff_all_effect.size() ; i++) {
			effects += snpeff_all_effect.get(i);
			if (i < snpeff_all_effect.size() - 1) {
				effects += "&";
			}
		}
		return effects;
	}

	public String getHgvsProtein() {
		return hgvsProtein;
	}


	public String getHgvsDna() {
		return hgvsDna;
	}


	public int getId() {
		return id;
	}

	public void setVariantId(long variant_id) {
		this.variant_id = variant_id;
	}

	public long getVariantId() {
		return variant_id;
	}

	public int setProjectId(int project_id) {
		return this.project_id = project_id;
	}

	public int getProjectId() {
		return project_id;
	}

	public Impact getImpact() {
		return snpeff_impact;
	}

	public String getUniqueId(){
		return chromosome + "-" + String.format("%010d", position) + "-" + reference + "-" + change;
	}

	public String getChangeId(){
		return chromosome + "-" + String.format("%010d", position) + "-" + change;
	}

	public boolean isMoreDamagingThan(SNPEffect e){
		return (snpeff_effect.getPriority() < e.snpeff_effect.getPriority());
	}

}
