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

package be.uclouvain.ngs.highlander.administration.dbpatcher;

import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;

/**
* @author Raphael Helaers
*/

public class Version_1_10 extends Version {

	public Version_1_10() {
		super("1.10");
	}

	@Override
	protected void makeUpdate() throws Exception {
		for (Analysis analysis : AnalysisFull.getAvailableAnalyses()){
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` CHANGE `snpeff_effect` `snpeff_effect` ENUM('EXON_DELETED', 'FRAME_SHIFT', 'STOP_GAINED', 'STOP_LOST', 'START_LOST', 'SPLICE_SITE_ACCEPTOR', 'SPLICE_SITE_DONOR', 'RARE_AMINO_ACID', 'CHROMOSOME_LARGE_DELETION', 'NON_SYNONYMOUS_CODING', 'CODON_INSERTION', 'CODON_CHANGE_PLUS_CODON_INSERTION', 'CODON_DELETION', 'CODON_CHANGE_PLUS_CODON_DELETION', 'UTR_5_DELETED', 'UTR_3_DELETED', 'SPLICE_SITE_REGION', 'SPLICE_SITE_BRANCH_U12', 'CODON_CHANGE', 'NON_SYNONYMOUS_STOP', 'NON_SYNONYMOUS_START', 'SYNONYMOUS_CODING', 'SYNONYMOUS_STOP', 'SYNONYMOUS_START', 'START_GAINED', 'SPLICE_SITE_BRANCH', 'TF_BINDING_SITE', 'UTR_5_PRIME', 'UTR_3_PRIME', 'UPSTREAM', 'DOWNSTREAM', 'REGULATION', 'MICRO_RNA', 'CUSTOM', 'INTRON_CONSERVED', 'INTRON', 'NON_CODING_EXON', 'INTRAGENIC', 'INTERGENIC_CONSERVED', 'INTERGENIC', 'CDS', 'EXON', 'TRANSCRIPT', 'GENE', 'CHROMOSOME', 'WITHIN_NON_CODING_GENE', 'NONE') DEFAULT NULL COMMENT 'Effect of this variant predicted by SnpEff.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_other_transcripts_snpeff` CHANGE `snpeff_effect` `snpeff_effect` ENUM('EXON_DELETED', 'FRAME_SHIFT', 'STOP_GAINED', 'STOP_LOST', 'START_LOST', 'SPLICE_SITE_ACCEPTOR', 'SPLICE_SITE_DONOR', 'RARE_AMINO_ACID', 'CHROMOSOME_LARGE_DELETION', 'NON_SYNONYMOUS_CODING', 'CODON_INSERTION', 'CODON_CHANGE_PLUS_CODON_INSERTION', 'CODON_DELETION', 'CODON_CHANGE_PLUS_CODON_DELETION', 'UTR_5_DELETED', 'UTR_3_DELETED', 'SPLICE_SITE_REGION', 'SPLICE_SITE_BRANCH_U12', 'CODON_CHANGE', 'NON_SYNONYMOUS_STOP', 'NON_SYNONYMOUS_START', 'SYNONYMOUS_CODING', 'SYNONYMOUS_STOP', 'SYNONYMOUS_START', 'START_GAINED', 'SPLICE_SITE_BRANCH', 'TF_BINDING_SITE', 'UTR_5_PRIME', 'UTR_3_PRIME', 'UPSTREAM', 'DOWNSTREAM', 'REGULATION', 'MICRO_RNA', 'CUSTOM', 'INTRON_CONSERVED', 'INTRON', 'NON_CODING_EXON', 'INTRAGENIC', 'INTERGENIC_CONSERVED', 'INTERGENIC', 'CDS', 'EXON', 'TRANSCRIPT', 'GENE', 'CHROMOSOME', 'WITHIN_NON_CODING_GENE', 'NONE') NOT NULL COMMENT 'Effect of this variant predicted by SnpEff.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_other_transcripts_snpeff` CHANGE `transcript_ensembl` `transcript_ensembl` CHAR(40) DEFAULT NULL COMMENT 'Ensembl transcript ID of the isophorm.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` CHANGE `transcript_ensembl` `transcript_ensembl` CHAR(40) DEFAULT NULL COMMENT 'Ensembl ID of the canonical transcript.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` CHANGE `dbsnp_id` `dbsnp_id_137` VARCHAR(255) DEFAULT NULL COMMENT 'The dbSNP 137 rs identifier of the SNP, based on the contig : position of the call and whether a record exists at this site in dbSNP'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `dbsnp_id_141` VARCHAR(255) DEFAULT NULL COMMENT 'The dbSNP 141 rs identifier of the SNP, based on the position' AFTER `dbsnp_id_137`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `vest_score` DOUBLE DEFAULT NULL COMMENT 'VEST 3.0 score. Score ranges from 0 to 1. The larger the score the more likely the mutation may cause functional change. In case there are multiple scores for the same variant, the largest score (most damaging) is presented. Please refer to Carter et al., (2013) BMC Genomics. 14(3) 1-16 for details.Please note this score is free for non-commercial use. For more details please refer to http://wiki.chasmsoftware.org/index.php/SoftwareLicense. Commercial users should contact the Johns Hopkins Technology Transfer office.' AFTER `reliability_index`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `cadd_raw` DOUBLE DEFAULT NULL COMMENT 'CADD raw score for funtional prediction of a SNP. Please refer to Kircher et al.(2014) Nature Genetics 46(3):310-5 for details. The larger the score the more likelythe SNP has damaging effect. Please note the following copyright statement for CADD: CADD scores (http://cadd.gs.washington.edu/) are Copyright 2013 University of Washington and Hudson-Alpha Institute for Biotechnology (all rights reserved) but are freely available for all academic, non-commercial applications. For commercial licensing information contact Jennifer McCullar (mccullaj@uw.edu).' AFTER `vest_score`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `cadd_phred` DOUBLE DEFAULT NULL COMMENT 'CADD phred-like score. This is phred-like rank score based on whole genomeCADD raw scores. Please refer to Kircher et al. (2014) Nature Genetics 46(3):310-5 for details. The larger the score the more likely the SNP has damaging effect. Please note the following copyright statement for CADD: CADD scores (http://cadd.gs.washington.edu/) are Copyright 2013 University of Washington and Hudson-Alpha Institute for Biotechnology (all rights reserved) but are freely available for all academic, non-commercial applications. For commercial licensing information contact Jennifer McCullar (mccullaj@uw.edu).' AFTER `cadd_raw`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` DROP COLUMN `phylop`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `phyloP46way_primate` DOUBLE DEFAULT NULL COMMENT 'phyloP (phylogenetic p-values) conservation score based on the multiple alignments of 10 primate genomes (including human). The larger the score, the more conserved the site.' AFTER `gerp_rs`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `phyloP46way_placental` DOUBLE DEFAULT NULL COMMENT 'phyloP (phylogenetic p-values) conservation score based on the multiple alignments of 33 placental mammal genomes (including human). The larger the score, the more conserved the site.' AFTER `phyloP46way_primate`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `phyloP100way_vertebrate` DOUBLE DEFAULT NULL COMMENT 'phyloP (phylogenetic p-values) conservation score based on the multiple alignments of 100 vertebrate genomes (including human). The larger the score, the more conserved the site.' AFTER `phyloP46way_placental`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `phastCons46way_primate` DOUBLE DEFAULT NULL COMMENT 'phastCons conservation score based on the multiple alignments of 10 primate genomes (including human). The larger the score, the more conserved the site.' AFTER `phyloP100way_vertebrate`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `phastCons46way_placental` DOUBLE DEFAULT NULL COMMENT 'phastCons conservation score based on the multiple alignments of 33 placental mammal genomes (including human). The larger the score, the more conserved the site.' AFTER `phastCons46way_primate`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `phastCons100way_vertebrate` DOUBLE DEFAULT NULL COMMENT 'phastCons conservation score based on the multiple alignments of 100 vertebrate genomes (including human). The larger the score, the more conserved the site.' AFTER `phastCons46way_placental`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `ARIC5606_AA_AC` INT DEFAULT NULL COMMENT 'Alternative allele counts in 2403 exomes of African Americans from the Atherosclerosis Risk in Communities Study (ARIC) cohort study.' AFTER `gonl_af`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `ARIC5606_AA_AF` DOUBLE DEFAULT NULL COMMENT 'Alternative allele frequency of 2403 exomes of African Americans from the Atherosclerosis Risk in Communities Study (ARIC) cohort study.' AFTER `ARIC5606_AA_AC`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `ARIC5606_EA_AC` INT DEFAULT NULL COMMENT 'Alternative allele counts in 3203 exomes of European Americans from the Atherosclerosis Risk in Communities Study (ARIC) cohort study.' AFTER `ARIC5606_AA_AF`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `ARIC5606_EA_AF` DOUBLE DEFAULT NULL COMMENT 'Alternative allele frequency of 3203 exomes of European Americans from the Atherosclerosis Risk in Communities Study (ARIC) cohort study.' AFTER `ARIC5606_EA_AC`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `clinvar_rs` VARCHAR(30) DEFAULT NULL COMMENT 'rs number from the clinvar data set' AFTER `ARIC5606_EA_AF`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `clinvar_clnsig` VARCHAR(30) DEFAULT NULL COMMENT 'clinical significance as to the clinvar data set 2 - Benign, 3 - Likely benign, 4 - Likely pathogenic, 5 - Pathogenic, 6 - drug response, 7 - histocompatibility. A negative score means the the score is for the ref allele' AFTER `clinvar_rs`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `clinvar_trait` VARCHAR(500) DEFAULT NULL COMMENT 'the trait/disease the clinvar_clnsig referring to' AFTER `clinvar_clnsig`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `cosmic_id` VARCHAR(20) DEFAULT NULL COMMENT 'ID of the SNV at the COSMIC (Catalogue Of Somatic Mutations In Cancer) database' AFTER `clinvar_trait`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `cosmic_count` INT DEFAULT NULL COMMENT 'number of samples having this SNV in the COSMIC database' AFTER `cosmic_id`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `is_scSNV_RefSeq` BOOLEAN DEFAULT NULL COMMENT 'Whether the SNV is a scSNV according to RefSeq (i.e. in splicing consensus regions, -3 to +8 at the 5’ splice site and -12 to +2 at the 3’ splice site).' AFTER `cadd_phred`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `is_scSNV_Ensembl` BOOLEAN DEFAULT NULL COMMENT 'Whether the SNV is a scSNV according to Ensembl (i.e. in splicing consensus regions, -3 to +8 at the 5’ splice site and -12 to +2 at the 3’ splice site).' AFTER `is_scSNV_RefSeq`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `splicing_ada_score` DOUBLE DEFAULT NULL COMMENT 'Ensemble prediction score based on ada-boost. Ranges 0 to 1. The larger the score the higher probability the scSNV will affect splicing. The suggested cutoff for a binary prediction (affecting splicing vs. not affecting splicing) is 0.6.' AFTER `is_scSNV_Ensembl`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `splicing_ada_pred` ENUM('AFFECTING_SPLICING','SPLICING_UNAFFECTED') DEFAULT NULL COMMENT 'Prediction based on ada-boost (0.6 cutoff).' AFTER `splicing_ada_score`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `splicing_rf_score` DOUBLE DEFAULT NULL COMMENT 'Ensemble prediction score based on random forests. Ranges 0 to 1. The larger the score the higher probability the scSNV will affect splicing. The suggested cutoff for a binary prediction (affecting splicing vs. not affecting splicing) is 0.6.' AFTER `splicing_ada_pred`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `splicing_rf_pred` ENUM('AFFECTING_SPLICING','SPLICING_UNAFFECTED') DEFAULT NULL COMMENT 'Prediction based on random forests (0.6 cutoff)' AFTER `splicing_rf_score`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` DROP COLUMN `exon_id`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` DROP COLUMN `old_aa`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` DROP COLUMN `new_aa`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` DROP COLUMN `old_codon`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` DROP COLUMN `new_codon`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` CHANGE `exon_number` `exon_intron_rank` SMALLINT DEFAULT NULL COMMENT 'Exon/Intron rank number out of total.' AFTER `gene_symbol`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `exon_intron_total` SMALLINT DEFAULT NULL COMMENT 'Total number of exons or introns.' AFTER `exon_intron_rank`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `cdna_pos` MEDIUMINT DEFAULT NULL COMMENT 'Position in cDNA (one based)' AFTER `exon_intron_total`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `cdna_length` MEDIUMINT DEFAULT NULL COMMENT 'trancript’s cDNA length (one based)' AFTER `cdna_pos`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `cds_pos` MEDIUMINT DEFAULT NULL COMMENT 'Position of coding bases (one based includes START and STOP codons)' AFTER `cdna_length`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` CHANGE `cds_size` `cds_length` MEDIUMINT DEFAULT NULL COMMENT 'Number of coding bases (one based includes START and STOP codons)' AFTER `cds_pos`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` CHANGE `codon_number` `protein_pos` MEDIUMINT DEFAULT NULL COMMENT 'Position in the protein (one based, including START, but not STOP)' AFTER `cds_length`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `protein_length` MEDIUMINT DEFAULT NULL COMMENT 'Number of amino acids (one based, including START, but not STOP)' AFTER `protein_pos`");
		}
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `description`='Consensus between prediction of all available software. The base score reflects the number software that predict the variant to be damaging: DAMAGING in Sift, DELETERIOUS in LRT, HIGH or MEDIUM in Mutation Assessor, DAMAGING in FATHMM, DISEASE_CAUSING (AUTOMATIC or not) in Mutation Taster, a score > 0.5 in Polyphen2 (hdiv or hvar). So max 6 if Sift, LRT, Mutation Assessor, FATHMM, Mutation Taster and Polyphen2 all agree. If the variant could affect splicing, a +1 or +2 could be added to the base score, if ada and/or rf score are > 0.6. To this, is added +10 for STOP_LOST, START_LOST, CODON_CHANGE, CODON_CHANGE_PLUS_CODON_DELETION, CODON_CHANGE_PLUS_CODON_INSERTION,	CODON_DELETION, CODON_INSERTION or RARE_AMINO_ACID ; +20 for SPLICE_SITE_REGION ; +30 for SPLICE_SITE_ACCEPTOR, SPLICE_SITE_DONOR or EXON_DELETED ; +40 for FRAME_SHIFT or STOP_GAINED (annotations from SnpEff).' WHERE `field`='consensus_prediction'");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `description`='DBNSFP\\'s support vector machine (SVM) based ensemble prediction score, which incorporated 10 scores (SIFT, PolyPhen-2 HDIV, PolyPhen-2 HVAR, GERP++, MutationTaster, Mutation Assessor, FATHMM, LRT, SiPhy, PhyloP) and the maximum frequency observed in the 1000 genomes populations. Larger value means the SNV is more likely to be damaging. Scores range from -2 to 3 in dbNSFP.' WHERE `field`='aggregation_score_radial_svm'");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `description`='Prediction of DBNSFP\\'s SVM based ensemble prediction score (aggregation_score_radial_svm),\"T(olerated)\" or \"D(amaging)\". The score cutoff between \"D\" and \"T\" is 0.' WHERE `field`='aggregation_pred_radial_svm'");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `description`='DBNSFP\\'s logistic regression (LR) based ensemble prediction score, which incorporated 10 scores (SIFT, PolyPhen-2 HDIV, PolyPhen-2 HVAR, GERP++, MutationTaster, Mutation Assessor, FATHMM, LRT, SiPhy, PhyloP) and the maximum frequency observed in the 1000 genomes populations. Larger value means the SNV is more likely to be damaging. Scores range from 0 to 1.' WHERE `field`='aggregation_score_lr'");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `description`='Prediction of DBNSFP\\'s MetaLR based ensemble prediction score (aggregation_score_lr),\"T(olerated)\" or \"D(amaging)\". The score cutoff between \"D\" and \"T\" is 0.5.' WHERE `field`='aggregation_pred_lr'");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `description`='Number of observed component scores (except the maximum frequency in the 1000 genomes populations) for aggregation_score_radial_svm and aggregation_score_lr. Ranges from 1 to 10. As aggregation_score_radial_svm and aggregation_score_lr scores are calculated based on imputed data, the less missing component scores, the higher the reliability of the scores and predictions.' WHERE `field`='reliability_index'");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `field`='dbsnp_id_137' WHERE `field`='dbsnp_id'");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('dbsnp_id_141','The dbSNP 141 rs identifier of the SNP, based on the position','dbSNP 141','related identifiers','medium','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('vest_score','VEST 3.0 score. Score ranges from 0 to 1. The larger the score the more likely the mutation may cause functional change. In case there are multiple scores for the same variant, the largest score (most damaging) is presented. VEST predictions can be used to prioritize mutations by sorting VEST scores from largest to smallest. This will rank the mutations by similarity to the disease mutation class of the VEST training set versus the neutral class mutations. Please refer to Carter et al., (2013) BMC Genomics. 14(3) 1-16 for details. Please note this score is free for non-commercial use. For more details please refer to http://wiki.chasmsoftware.org/index.php/SoftwareLicense. Commercial users should contact the Johns Hopkins Technology Transfer office.','dbNSFP 2.8','effect prediction','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('cadd_raw','CADD raw score for funtional prediction of a SNP. Raw CADD scores come straight from the model, and are interpretable as the extent to which the annotation profile for a given variant suggests that that variant is likely to be observed (negative values) vs simulated (positive values). These values have no absolute unit of meaning and are incomparable across distinct annotation combinations, training sets, or model parameters. However, raw values do have relative meaning, with higher values indicating that a variant is more likely to be simulated (or not observed) and therefore more likely to have deleterious effects. Please refer to Kircher et al.(2014) Nature Genetics 46(3):310-5 for details. The larger the score the more likelythe SNP has damaging effect. Please note the following copyright statement for CADD: CADD scores (http://cadd.gs.washington.edu/) are Copyright 2013 University of Washington and Hudson-Alpha Institute for Biotechnology (all rights reserved) but are freely available for all academic, non-commercial applications. For commercial licensing information contact Jennifer McCullar (mccullaj@uw.edu).','dbNSFP 2.8','effect prediction','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('cadd_phred','CADD phred-like score (-10*log10(rank/total)) based on whole genomeCADD raw scores. A scaled C-score of greater of equal 10 indicates that these are predicted to be the 10% most deleterious substitutions that you can do to the human genome, a score of greater or equal 20 indicates the 1% most deleterious and so on. If you would like to apply a cutoff on deleteriousness, you should put it somewhere between 10 and 20 (15). Please refer to Kircher et al. (2014) Nature Genetics 46(3):310-5 for details. The larger the score the more likely the SNP has damaging effect. Please note the following copyright statement for CADD: CADD scores (http://cadd.gs.washington.edu/) are Copyright 2013 University of Washington and Hudson-Alpha Institute for Biotechnology (all rights reserved) but are freely available for all academic, non-commercial applications. For commercial licensing information contact Jennifer McCullar (mccullaj@uw.edu).','dbNSFP 2.8','effect prediction','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "DELETE FROM `fields` WHERE `field` = 'phylop'");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('phyloP46way_primate','phyloP (phylogenetic p-values) conservation score based on the multiple alignments of 10 primate genomes (including human). The larger the score, the more conserved the site.','dbNSFP 2.8','conservation scores','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('phyloP46way_placental','phyloP (phylogenetic p-values) conservation score based on the multiple alignments of 33 placental mammal genomes (including human). The larger the score, the more conserved the site.','dbNSFP 2.8','conservation scores','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('phyloP100way_vertebrate','phyloP (phylogenetic p-values) conservation score based on the multiple alignments of 100 vertebrate genomes (including human). The larger the score, the more conserved the site.','dbNSFP 2.8','conservation scores','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('phastCons46way_primate','phastCons conservation score based on the multiple alignments of 10 primate genomes (including human). The larger the score, the more conserved the site.','dbNSFP 2.8','conservation scores','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('phastCons46way_placental','phastCons conservation score based on the multiple alignments of 33 placental mammal genomes (including human). The larger the score, the more conserved the site.','dbNSFP 2.8','conservation scores','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('phastCons100way_vertebrate','phastCons conservation score based on the multiple alignments of 100 vertebrate genomes (including human). The larger the score, the more conserved the site.','dbNSFP 2.8','conservation scores','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('ARIC5606_AA_AC','Alternative allele counts in 2403 exomes of African Americans from the Atherosclerosis Risk in Communities Study (ARIC) cohort study.','dbNSFP 2.8','allele frequency in population','small','CENTER')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('ARIC5606_AA_AF','Alternative allele frequency of 2403 exomes of African Americans from the Atherosclerosis Risk in Communities Study (ARIC) cohort study.','dbNSFP 2.8','allele frequency in population','small','CENTER')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('ARIC5606_EA_AC','Alternative allele counts in 3203 exomes of European Americans from the Atherosclerosis Risk in Communities Study (ARIC) cohort study.','dbNSFP 2.8','allele frequency in population','small','CENTER')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('ARIC5606_EA_AF','Alternative allele frequency of 3203 exomes of European Americans from the Atherosclerosis Risk in Communities Study (ARIC) cohort study.','dbNSFP 2.8','allele frequency in population','small','CENTER')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('clinvar_rs','rs number from the clinvar data set','dbNSFP 2.8','related identifiers','medium','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('clinvar_clnsig','clinical significance as to the clinvar data set : 2 - Benign, 3 - Likely benign, 4 - Likely pathogenic, 5 - Pathogenic, 6 - drug response, 7 - histocompatibility. A negative score means the the score is for the ref allele','dbNSFP 2.8','related identifiers','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('clinvar_trait','the trait/disease the clinvar_clnsig referring to','dbNSFP 2.8','related identifiers','medium','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('cosmic_id','ID of the SNV at the COSMIC (Catalogue Of Somatic Mutations In Cancer) database','dbNSFP 2.8','related identifiers','medium','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('cosmic_count','number of samples having this SNV in the COSMIC database','dbNSFP 2.8','related identifiers','small','CENTER')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('is_scSNV_RefSeq','Whether the SNV is a scSNV according to RefSeq (i.e. in splicing consensus regions, -3 to +8 at the 5’ splice site and -12 to +2 at the 3’ splice site).','dbNSFP 2.8','effect prediction','small','CENTER')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('is_scSNV_Ensembl','Whether the SNV is a scSNV according to Ensembl (i.e. in splicing consensus regions, -3 to +8 at the 5’ splice site and -12 to +2 at the 3’ splice site).','dbNSFP 2.8','effect prediction','small','CENTER')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('splicing_ada_score','Ensemble prediction score based on ada-boost. Ranges 0 to 1. The larger the score the higher probability the scSNV will affect splicing. The suggested cutoff for a binary prediction (affecting splicing vs. not affecting splicing) is 0.6.','dbNSFP 2.8','effect prediction','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('splicing_ada_pred','Prediction based on ada-boost (0.6 cutoff).','dbNSFP 2.8','effect prediction','medium','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('splicing_rf_score','Ensemble prediction score based on random forests. Ranges 0 to 1. The larger the score the higher probability the scSNV will affect splicing. The suggested cutoff for a binary prediction (affecting splicing vs. not affecting splicing) is 0.6.','dbNSFP 2.8','effect prediction','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('splicing_rf_pred','Prediction based on random forests (0.6 cutoff)','dbNSFP 2.8','effect prediction','medium','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "DELETE FROM `fields` WHERE `field` = 'exon_id'");
		updateAndPrint(Schema.HIGHLANDER, "DELETE FROM `fields` WHERE `field` = 'old_aa'");
		updateAndPrint(Schema.HIGHLANDER, "DELETE FROM `fields` WHERE `field` = 'new_aa'");
		updateAndPrint(Schema.HIGHLANDER, "DELETE FROM `fields` WHERE `field` = 'old_codon'");
		updateAndPrint(Schema.HIGHLANDER, "DELETE FROM `fields` WHERE `field` = 'new_codon'");
		updateAndPrint(Schema.HIGHLANDER, "DELETE FROM `fields` WHERE `field` = 'cds_size'");
		updateAndPrint(Schema.HIGHLANDER, "DELETE FROM `fields` WHERE `field` = 'codon_number'");
		updateAndPrint(Schema.HIGHLANDER, "DELETE FROM `fields` WHERE `field` = 'exon_number'");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('exon_intron_rank','Exon/Intron rank number out of total.','snpEff 4.1','change details','small','CENTER')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('exon_intron_total','Total number of exons or introns.','snpEff 4.1','change details','small','CENTER')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('cdna_pos','Position in cDNA (one based)','snpEff 4.1','change details','small','CENTER')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('cdna_length','trancript’s cDNA length (one based)','snpEff 4.1','change details','small','CENTER')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('cds_pos','Position of coding bases (one based includes START and STOP codons)','snpEff 4.1','change details','small','CENTER')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('cds_length','Number of coding bases (one based includes START and STOP codons)','snpEff 4.1','change details','small','CENTER')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('protein_pos','Position in the protein (one based, including START, but not STOP)','snpEff 4.1','change details','small','CENTER')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('protein_length','Number of amino acids (one based, including START, but not STOP)','snpEff 4.1','change details','small','CENTER')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `source`='GATK 3.3' WHERE `field`='zygosity'");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `description`='Is this homozygous or heterozygous.' WHERE `field`='zygosity'");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `source`='dbNSFP 2.8' WHERE `source`='dbNSFP 2.3'");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `source`='GATK 3.3' WHERE `source`='GATK 2.8'");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `source`='snpEff 4.1' WHERE `source`='snpEff 3.6'");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'phylop','phyloP46way_primate')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'dbsnp_id','dbsnp_id_137')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'exon_number','exon_intron_rank')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'cds_size','cds_length')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'codon_number','protein_pos')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'°exon_id','')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'°old_aa','')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'°new_aa','')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'°old_codon','')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'°new_codon','')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'exon_id','public_comments') WHERE `type` = 'FILTER'");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'old_aa','public_comments') WHERE `type` = 'FILTER'");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'new_aa','public_comments') WHERE `type` = 'FILTER'");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'old_codon','public_comments') WHERE `type` = 'FILTER'");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'new_codon','public_comments') WHERE `type` = 'FILTER'");
		updateAndPrint(Schema.HIGHLANDER, "DELETE FROM `users_data` WHERE `value` = 'exon_id'");
		updateAndPrint(Schema.HIGHLANDER, "DELETE FROM `users_data` WHERE `value` = 'old_aa'");
		updateAndPrint(Schema.HIGHLANDER, "DELETE FROM `users_data` WHERE `value` = 'new_aa'");
		updateAndPrint(Schema.HIGHLANDER, "DELETE FROM `users_data` WHERE `value` = 'old_codon'");
		updateAndPrint(Schema.HIGHLANDER, "DELETE FROM `users_data` WHERE `value` = 'new_codon'");
	}

}
