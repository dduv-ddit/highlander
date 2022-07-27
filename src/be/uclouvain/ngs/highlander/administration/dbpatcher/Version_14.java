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

import be.uclouvain.ngs.highlander.database.Field.Insilico;
import be.uclouvain.ngs.highlander.database.Field.Mosaicism;
import be.uclouvain.ngs.highlander.database.Field.Segregation;
import be.uclouvain.ngs.highlander.database.Field.Validation;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;

/**
* @author Raphael Helaers
*/

public class Version_14 extends Version {

	public Version_14() {
		super("14");
	}

	@Override
	protected void makeUpdate() throws Exception {
		for (Analysis analysis : AnalysisFull.getAvailableAnalyses()){
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` CHANGE `snpeff_effect` `snpeff_effect` ENUM('EXON_DELETED', 'FRAME_SHIFT', 'STOP_GAINED', 'STOP_LOST', 'START_LOST', 'SPLICE_SITE_ACCEPTOR', 'SPLICE_SITE_DONOR', 'RARE_AMINO_ACID', 'CHROMOSOME_LARGE_DELETION', 'NON_SYNONYMOUS_CODING', 'CODON_INSERTION', 'CODON_CHANGE_PLUS_CODON_INSERTION', 'CODON_DELETION', 'CODON_CHANGE_PLUS_CODON_DELETION', 'UTR_5_DELETED', 'UTR_3_DELETED', 'SPLICE_SITE_REGION', 'SPLICE_SITE_BRANCH_U12', 'CODON_CHANGE', 'NON_SYNONYMOUS_STOP', 'NON_SYNONYMOUS_START', 'SYNONYMOUS_CODING', 'SYNONYMOUS_STOP', 'SYNONYMOUS_START', 'START_GAINED', 'SPLICE_SITE_BRANCH', 'TF_BINDING_SITE', 'SEQUENCE_FEATURE', 'UTR_5_PRIME', 'UTR_3_PRIME', 'UPSTREAM', 'DOWNSTREAM', 'REGULATION', 'MICRO_RNA', 'CUSTOM', 'INTRON_CONSERVED', 'INTRON', 'NON_CODING_EXON', 'INTRAGENIC', 'INTERGENIC_CONSERVED', 'INTERGENIC', 'CDS', 'EXON', 'TRANSCRIPT', 'GENE', 'CHROMOSOME', 'WITHIN_NON_CODING_GENE', 'NONE') DEFAULT NULL COMMENT 'Effect of this variant predicted by SnpEff.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_other_transcripts_snpeff` CHANGE `snpeff_effect` `snpeff_effect` ENUM('EXON_DELETED', 'FRAME_SHIFT', 'STOP_GAINED', 'STOP_LOST', 'START_LOST', 'SPLICE_SITE_ACCEPTOR', 'SPLICE_SITE_DONOR', 'RARE_AMINO_ACID', 'CHROMOSOME_LARGE_DELETION', 'NON_SYNONYMOUS_CODING', 'CODON_INSERTION', 'CODON_CHANGE_PLUS_CODON_INSERTION', 'CODON_DELETION', 'CODON_CHANGE_PLUS_CODON_DELETION', 'UTR_5_DELETED', 'UTR_3_DELETED', 'SPLICE_SITE_REGION', 'SPLICE_SITE_BRANCH_U12', 'CODON_CHANGE', 'NON_SYNONYMOUS_STOP', 'NON_SYNONYMOUS_START', 'SYNONYMOUS_CODING', 'SYNONYMOUS_STOP', 'SYNONYMOUS_START', 'START_GAINED', 'SPLICE_SITE_BRANCH', 'TF_BINDING_SITE', 'SEQUENCE_FEATURE', 'UTR_5_PRIME', 'UTR_3_PRIME', 'UPSTREAM', 'DOWNSTREAM', 'REGULATION', 'MICRO_RNA', 'CUSTOM', 'INTRON_CONSERVED', 'INTRON', 'NON_CODING_EXON', 'INTRAGENIC', 'INTERGENIC_CONSERVED', 'INTERGENIC', 'CDS', 'EXON', 'TRANSCRIPT', 'GENE', 'CHROMOSOME', 'WITHIN_NON_CODING_GENE', 'NONE') NOT NULL COMMENT 'Effect of this variant predicted by SnpEff.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` CHANGE `dbsnp_id_137` `dbsnp_id` VARCHAR(255) DEFAULT NULL COMMENT 'The dbSNP rs identifier of the SNP, based on the contig : position of the call and whether a record exists at this site in dbSNP'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` DROP COLUMN `dbsnp_id_141`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` CHANGE `check_insilico` `check_insilico` VARCHAR(20) DEFAULT 'NOT_CHECKED'");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `"+analysis+"` SET `check_insilico` = 'NOT_CHECKED' WHERE `check_insilico` IS NULL");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `"+analysis+"` SET `check_insilico` = 'OK' WHERE `check_insilico` = '1'");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `"+analysis+"` SET `check_insilico` = 'NOT_OK' WHERE `check_insilico` = '0'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` CHANGE `check_insilico` `check_insilico` ENUM('NOT_CHECKED','OK','SUSPECT','NOT_OK') NOT NULL DEFAULT 'NOT_CHECKED' COMMENT 'This field can be used if this variant has been evaluated insilico (e.g. by looking at the alignment): OK (it is likely a real change), NOT_OK (it is likely a sequencing error, e.g. alternative allele not specific to pathology), SUSPECT (checked insilico but not sure if it is real or not) or NOT_CHECKED (Not checked insilico, default value). Only users associated with the given sample can change this field, but any user can see the assigned value.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` CHANGE `check_validated_change` `check_validated_change` VARCHAR(20) DEFAULT 'NOT_CHECKED'");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `"+analysis+"` SET `check_validated_change` = 'NOT_CHECKED' WHERE `check_validated_change` IS NULL");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `"+analysis+"` SET `check_validated_change` = 'VALIDATED' WHERE `check_validated_change` = '1'");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `"+analysis+"` SET `check_validated_change` = 'INVALIDATED' WHERE `check_validated_change` = '0'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` CHANGE `check_validated_change` `check_validated_change` ENUM('NOT_CHECKED','VALIDATED','SUSPECT','INVALIDATED') NOT NULL DEFAULT 'NOT_CHECKED' COMMENT 'This field can be used if this variant has been tested in the lab: VALIDATED (the change has been confirmed with another lab technique, e.g. by Sanger sequencing), INVALIDATED (the change has been invalidated and is not real), SUSPECT (the change has been checked with another technique, but cannot be confirmed or invalidate) or NOT_CHECKED (Not checked with another lab technique, default value). Only users associated with the given sample can change this field, but any user can see the assigned value.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` CHANGE `check_somatic_change` `check_somatic_change` VARCHAR(20) DEFAULT 'NOT_CHECKED'");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `"+analysis+"` SET `check_somatic_change` = 'NOT_CHECKED' WHERE `check_somatic_change` IS NULL");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `"+analysis+"` SET `check_somatic_change` = 'SOMATIC' WHERE `check_somatic_change` = '1'");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `"+analysis+"` SET `check_somatic_change` = 'GERMLINE' WHERE `check_somatic_change` = '0'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` CHANGE `check_somatic_change` `check_somatic_change` ENUM('NOT_CHECKED','SOMATIC','DUBIOUS','GERMLINE') NOT NULL DEFAULT 'NOT_CHECKED' COMMENT 'This field can be used if this variant has been evaluated for mosaicism: SOMATIC (it is likely a somatic change), GERMLINE (it is likely a germline change), DUBIOUS (checked for mosaicism, but impossible to differenciate between somatic or germline) or NOT_CHECKED (Not checked for mosaicism, default value). Only users associated with the given sample can change this field, but any user can see the assigned value.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_backup_public_annotations` CHANGE `check_insilico` `check_insilico` VARCHAR(20) DEFAULT 'NOT_CHECKED'");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `"+analysis+"_backup_public_annotations` SET `check_insilico` = 'NOT_CHECKED' WHERE `check_insilico` IS NULL");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `"+analysis+"_backup_public_annotations` SET `check_insilico` = 'OK' WHERE `check_insilico` = '1'");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `"+analysis+"_backup_public_annotations` SET `check_insilico` = 'NOT_OK' WHERE `check_insilico` = '0'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_backup_public_annotations` CHANGE `check_insilico` `check_insilico` ENUM('NOT_CHECKED','OK','SUSPECT','NOT_OK') NOT NULL DEFAULT 'NOT_CHECKED' COMMENT 'This field can be used if this variant has been evaluated insilico (e.g. by looking at the alignment): OK (it is likely a real change), NOT_OK (it is likely a sequencing error, e.g. alternative allele not specific to pathology), SUSPECT (checked insilico but not sure if it is real or not) or NOT_CHECKED (Not checked insilico, default value). Only users associated with the given sample can change this field, but any user can see the assigned value.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_backup_public_annotations` CHANGE `check_validated_change` `check_validated_change` VARCHAR(20) DEFAULT 'NOT_CHECKED'");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `"+analysis+"_backup_public_annotations` SET `check_validated_change` = 'NOT_CHECKED' WHERE `check_validated_change` IS NULL");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `"+analysis+"_backup_public_annotations` SET `check_validated_change` = 'VALIDATED' WHERE `check_validated_change` = '1'");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `"+analysis+"_backup_public_annotations` SET `check_validated_change` = 'INVALIDATED' WHERE `check_validated_change` = '0'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_backup_public_annotations` CHANGE `check_validated_change` `check_validated_change` ENUM('NOT_CHECKED','VALIDATED','SUSPECT','INVALIDATED') NOT NULL DEFAULT 'NOT_CHECKED' COMMENT 'This field can be used if this variant has been tested in the lab: VALIDATED (the change has been confirmed with another lab technique, e.g. by Sanger sequencing), INVALIDATED (the change has been invalidated and is not real), SUSPECT (the change has been checked with another technique, but cannot be confirmed or invalidate) or NOT_CHECKED (Not checked with another lab technique, default value). Only users associated with the given sample can change this field, but any user can see the assigned value.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_backup_public_annotations` CHANGE `check_somatic_change` `check_somatic_change` VARCHAR(20) DEFAULT 'NOT_CHECKED'");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `"+analysis+"_backup_public_annotations` SET `check_somatic_change` = 'NOT_CHECKED' WHERE `check_somatic_change` IS NULL");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `"+analysis+"_backup_public_annotations` SET `check_somatic_change` = 'SOMATIC' WHERE `check_somatic_change` = '1'");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `"+analysis+"_backup_public_annotations` SET `check_somatic_change` = 'GERMLINE' WHERE `check_somatic_change` = '0'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_backup_public_annotations` CHANGE `check_somatic_change` `check_somatic_change` ENUM('NOT_CHECKED','SOMATIC','DUBIOUS','GERMLINE') NOT NULL DEFAULT 'NOT_CHECKED' COMMENT 'This field can be used if this variant has been evaluated for mosaicism: SOMATIC (it is likely a somatic change), GERMLINE (it is likely a germline change), DUBIOUS (checked for mosaicism, but impossible to differenciate between somatic or germline) or NOT_CHECKED (Not checked for mosaicism, default value). Only users associated with the given sample can change this field, but any user can see the assigned value.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `clinvar_ids` VARCHAR(500) DEFAULT NULL COMMENT 'ClinVar ids' AFTER `clinvar_trait`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `clinvar_origins` VARCHAR(500) DEFAULT NULL COMMENT 'ClinVar origins. Possible values: germline, somatic, de novo, maternal, etc.' AFTER `clinvar_ids`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `clinvar_methods` VARCHAR(500) DEFAULT NULL COMMENT 'ClinVar methods. Possible values: clinical testing, research, literature only, etc' AFTER `clinvar_origins`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `clinvar_clin_signifs` VARCHAR(500) DEFAULT NULL COMMENT 'ClinVar clinical significances' AFTER `clinvar_methods`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `clinvar_review_status` VARCHAR(500) DEFAULT NULL COMMENT 'ClinVar review status. Number of stars (0-4).' AFTER `clinvar_clin_signifs`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `clinvar_phenotypes` VARCHAR(500) DEFAULT NULL COMMENT 'ClinVar phenotypes' AFTER `clinvar_review_status`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `hgmd_id` VARCHAR(255) DEFAULT NULL COMMENT 'HGMD mutation id' AFTER `cosmic_count`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `hgmd_phenotype` VARCHAR(500) DEFAULT NULL COMMENT 'HGMD phenotype' AFTER `hgmd_id`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `hgmd_pubmed_id` VARCHAR(255) DEFAULT NULL COMMENT 'HGMD PubMed id' AFTER `hgmd_phenotype`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `hgmd_sub_category` VARCHAR(500) DEFAULT NULL COMMENT 'HGMD sub-category (DP, DFP, FP, FTV, DM?, DM )' AFTER `hgmd_pubmed_id`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `deogen_score` DOUBLE DEFAULT NULL COMMENT 'DEOGEN predicted score.' AFTER `fathmm_pred`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `deogen_pred` ENUM('DELETERIOUS','NEUTRAL') DEFAULT NULL COMMENT 'DEOGEN discrete prediction.' AFTER `deogen_score`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `gevact_first_score` DOUBLE DEFAULT NULL COMMENT 'Pre-classification score from the GeVaCT classification tool' AFTER `cadd_phred`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `gevact_first_class` VARCHAR(20) DEFAULT NULL COMMENT 'Initial class allocated to a variant based on gevact_first_score (classes are as defined by Sharon et al, 2008 for variant pathogenic classification)' AFTER `gevact_first_score`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `gevact_final_score` DOUBLE DEFAULT NULL COMMENT 'Final classification score made by the GeVaCT classification tool, based on gevact_first_score and manual inputs' AFTER `gevact_first_class`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `gevact_final_class` VARCHAR(20) DEFAULT NULL COMMENT 'Final class allocated to a variant based on gevact_final_score (classes are as defined by Sharon et al, 2008 for variant pathogenic classification)' AFTER `gevact_final_score`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `exac_an` INT DEFAULT NULL COMMENT 'Total allele number in ExAC for position' AFTER `exac_af`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `dbsnp_maf` DOUBLE DEFAULT NULL COMMENT 'dbSNP variation global Minor Allele Frequency' AFTER `dbsnp_id`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `grantham_dist` DOUBLE DEFAULT NULL COMMENT 'Grantham distance' AFTER `repeat_number_alt`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `agvgd_class` VARCHAR(255) DEFAULT NULL COMMENT 'AlignGVGD class' AFTER `grantham_dist`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `blosum62` DOUBLE DEFAULT NULL COMMENT 'BLOSUM62' AFTER `agvgd_class`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `wt_ssf_score` DOUBLE DEFAULT NULL COMMENT 'WT seq. SpliceSiteFinder score' AFTER `blosum62`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `wt_maxent_score` DOUBLE DEFAULT NULL COMMENT 'WT seq. MaxEntScan score' AFTER `wt_ssf_score`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `wt_nns_score` DOUBLE DEFAULT NULL COMMENT 'WT seq. NNSPLICE score' AFTER `wt_maxent_score`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `wt_gs_score` DOUBLE DEFAULT NULL COMMENT 'WT seq. GeneSplicer score' AFTER `wt_nns_score`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `wt_hsf_score` DOUBLE DEFAULT NULL COMMENT 'WT seq. HSF score' AFTER `wt_gs_score`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `var_ssf_score` DOUBLE DEFAULT NULL COMMENT 'Variant seq. SpliceSiteFinder score' AFTER `wt_hsf_score`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `var_maxent_score` DOUBLE DEFAULT NULL COMMENT 'Variant seq. MaxEntScan score' AFTER `var_ssf_score`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `var_nns_score` DOUBLE DEFAULT NULL COMMENT 'Variant seq. NNSPLICE score' AFTER `var_maxent_score`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `var_gs_score` DOUBLE DEFAULT NULL COMMENT 'Variant seq. GeneSplicer score' AFTER `var_nns_score`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `var_hsf_score` DOUBLE DEFAULT NULL COMMENT 'Variant seq. HSF score ' AFTER `var_gs_score`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` ADD COLUMN `local_splice_effect` VARCHAR(255) DEFAULT NULL COMMENT 'Splicing effect in variation vicinity (New Donor Site, New Acceptor Site, Cryptic Donor Strongly Activated, Cryptic Donor Weakly Activated, Cryptic Acceptor Strongly Activated, Cryptic Acceptor Weakly Activated)' AFTER `var_hsf_score`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` CHANGE `public_comments` `public_comments_variant` TEXT NOT NULL COMMENT 'Public comments about the variant, visible and modifiable by all users.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_backup_public_annotations` CHANGE `public_comments` `public_comments_variant` TEXT NOT NULL COMMENT 'Public comments about the variant, visible and modifiable by all users.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_private_annotations` CHANGE `private_comments` `private_comments_variant` TEXT NOT NULL COMMENT 'Private comments about the variant, visible only by you.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_private_annotations` CHANGE `of_interest` `of_interest_variant` BOOLEAN DEFAULT NULL COMMENT 'You can set this flag to true if the variant is of interest for you, or to false otherwise.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_backup_private_annotations` CHANGE `private_comments` `private_comments_variant` TEXT NOT NULL COMMENT 'Private comments about the variant, visible only by you.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_backup_private_annotations` CHANGE `of_interest` `of_interest_variant` BOOLEAN DEFAULT NULL COMMENT 'You can set this flag to true if the variant is of interest for you, or to false otherwise.'");
			updateAndPrint(Schema.HIGHLANDER, "DROP TABLE IF EXISTS `"+analysis+"_gene_annotations`");
			updateAndPrint(Schema.HIGHLANDER, "CREATE TABLE `"+analysis+"_gene_annotations` ("+
					" `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,"+
					" `gene_symbol` VARCHAR(255) NOT NULL COMMENT 'Gene symbol.',"+
					"	`username` VARCHAR(16) DEFAULT NULL COMMENT 'NULL for public comments',"+
					"	`of_interest_gene` BOOLEAN DEFAULT NULL COMMENT 'You can set this flag to true if the gene is of interest for you, or to false otherwise.',"+
					"	`comments_gene` TEXT NOT NULL COMMENT 'Comments about the gene, if any.',"+
					"	PRIMARY KEY  (`id`),"+
					"	INDEX `id` (`id`)"+
					") ENGINE=MyISAM DEFAULT CHARSET=latin1;");
			updateAndPrint(Schema.HIGHLANDER, "DROP TABLE IF EXISTS `"+analysis+"_public_annotations`");
			updateAndPrint(Schema.HIGHLANDER, "CREATE TABLE `"+analysis+"_public_annotations` ("+
					"	`id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,"+
					"	`variant_id` INT(10) UNSIGNED NOT NULL COMMENT 'Unique identifier of the variant',"+
					"	`project_id` INT(10) UNSIGNED NOT NULL COMMENT 'Id of the sample in the project table',"+
					"	`patient` VARCHAR(50) NOT NULL ,"+
					"	`chr` CHAR(2) NOT NULL ,"+
					"	`pos` INT(11) NOT NULL ,"+
					"	`reference` VARCHAR(1000) NOT NULL ,"+
					"	`alternative` VARCHAR(1000) NOT NULL ,"+
					"	`evaluation` TINYINT DEFAULT 0 COMMENT 'This flag can be used to assign an evaluation class to a change: I - Benign - Polymorphism. II - Variant Likely Benign. III - Variant of Unknown Significance. IV - Variant Likely Pathogenic. V - Pathogenic Mutation. A value of zero means that this variant has not been evaluated. Only users associated with the given sample can change this flag, but any user can see the assigned value.',"+
					"	`evaluation_username` VARCHAR(16) DEFAULT NULL COMMENT 'User who has set the evaluation flag.',"+
					"	`check_insilico` ENUM('NOT_CHECKED','OK','SUSPECT','NOT_OK') NOT NULL DEFAULT 'NOT_CHECKED' COMMENT 'This field can be used if this variant has been evaluated insilico (e.g. by looking at the alignment): OK (it\\'s likely a real change), NOT_OK (it\\'s likely a sequencing error, e.g. alternative allele not specific to pathology), SUSPECT (checked insilico but not sure if it\\'s real or not) or NOT_CHECKED (Not checked insilico, default value). Only users associated with the given sample can change this field, but any user can see the assigned value.',"+
					"	`check_insilico_username` VARCHAR(16) DEFAULT NULL COMMENT 'User who has set the check_insilico field.',"+
					"	`check_validated_change` ENUM('NOT_CHECKED','VALIDATED','SUSPECT','INVALIDATED') NOT NULL DEFAULT 'NOT_CHECKED' COMMENT 'This field can be used if this variant has been tested in the lab: VALIDATED (the change has been confirmed with another lab technique, e.g. by Sanger sequencing), INVALIDATED (the change has been invalidated and is not real), SUSPECT (the change has been checked with another technique, but cannot be confirmed or invalidate) or NOT_CHECKED (Not checked with another lab technique, default value). Only users associated with the given sample can change this field, but any user can see the assigned value.',"+
					"	`check_validated_change_username` VARCHAR(16) DEFAULT NULL COMMENT 'User who has set the check_validated_change field.',"+
					"	`check_somatic_change` ENUM('NOT_CHECKED','SOMATIC','DUBIOUS','GERMLINE') NOT NULL DEFAULT 'NOT_CHECKED' COMMENT 'This field can be used if this variant has been evaluated for mosaicism: SOMATIC (it\\'s likely a somatic change), GERMLINE (it\\'s likely a germline change), DUBIOUS (checked for mosaicism, but impossible to differenciate between somatic or germline) or NOT_CHECKED (Not checked for mosaicism, default value). Only users associated with the given sample can change this field, but any user can see the assigned value.',"+
					"	`check_somatic_change_username` VARCHAR(16) DEFAULT NULL COMMENT 'User who has set the check_somatic_change field.',"+
					"	`check_segregation` ENUM('NOT_CHECKED','SINGLE','COSEG','CARRIERS','NO_COSEG','NO_COSEG_OTHER') NOT NULL DEFAULT 'NOT_CHECKED' COMMENT 'This field can be used if this variant has been evaluated for segregation: SINGLE (No other sample in family), COSEG (Variant cosegregates), CARRIERS (Some unaffected carrier(s)), NO_COSEG (Not in other affected(s)), NO_COSEG_OTHER (Does not cosegregate in other families) or NOT_CHECKED (Not checked for segregation, default value). Only users associated with the given sample can change this field, but any user can see the assigned value.',"+
					"	`check_segregation_username` VARCHAR(16) DEFAULT NULL COMMENT 'User who has set the check_segregation field.',"+
					"	`evaluation_comments` TEXT NOT NULL COMMENT 'Comments visible to all users but modifiable only by users associated with the sample.',"+
					"	`history` TEXT NOT NULL COMMENT 'History of evaluation modifications, including username and date.',"+
					"	`public_comments_variant` TEXT NOT NULL COMMENT 'Public comments about the variant, visible and modifiable by all users.',"+
					"	PRIMARY KEY (`id`) ,"+
					"	INDEX `patient` (`variant_id`)"+
					") ENGINE=MyISAM DEFAULT CHARSET=latin1;");
			updateAndPrint(Schema.HIGHLANDER, "INSERT INTO "+analysis+"_public_annotations " +
					"(`variant_id`,`project_id`,`patient`,`chr`,`pos`,`reference`,`alternative`,`evaluation`,`evaluation_username`,`check_insilico`,`check_insilico_username`," +
					"`check_validated_change`,`check_validated_change_username`,`check_somatic_change`,`check_somatic_change_username`,`check_segregation`,`check_segregation_username`," +
					"`evaluation_comments`,`history`,`public_comments_variant`) " +
					"SELECT `id`,`project_id`,`patient`,`chr`,`pos`,`reference`,`alternative`,`evaluation`,`evaluation_username`,`check_insilico`,`check_insilico_username`," +
					"`check_validated_change`,`check_validated_change_username`,`check_somatic_change`,`check_somatic_change_username`,`check_segregation`,`check_segregation_username`," +
					"`evaluation_comments`,`history`,`public_comments_variant` " +
					"FROM "+analysis+" WHERE " +
					"((`evaluation` IS NOT NULL AND `evaluation` != 0) OR `check_insilico` != 'NOT_CHECKED' OR `check_validated_change` != 'NOT_CHECKED' OR `check_somatic_change` != 'NOT_CHECKED' OR " +
					"`check_segregation` != 'NOT_CHECKED' OR `evaluation_comments` != '' OR `history` != '' OR `public_comments_variant` != '')");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` DROP COLUMN `evaluation`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` DROP COLUMN `evaluation_username`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` DROP COLUMN `check_insilico`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` DROP COLUMN `check_insilico_username`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` DROP COLUMN `check_validated_change`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` DROP COLUMN `check_validated_change_username`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` DROP COLUMN `check_somatic_change`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` DROP COLUMN `check_somatic_change_username`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` DROP COLUMN `check_segregation`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` DROP COLUMN `check_segregation_username`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` DROP COLUMN `evaluation_comments`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` DROP COLUMN `history`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` DROP COLUMN `public_comments_variant`");
			for (Insilico val : Insilico.values()){
				updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `"+analysis+"_possible_values` SET `field` = 'check_insilico', `value` = '"+val+"'");
			}
			for (Validation val : Validation.values()){
				updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `"+analysis+"_possible_values` SET `field` = 'check_validated_change', `value` = '"+val+"'");
			}
			for (Mosaicism val : Mosaicism.values()){
				updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `"+analysis+"_possible_values` SET `field` = 'check_somatic_change', `value` = '"+val+"'");
			}
			for (Segregation val : Segregation.values()){
				updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `"+analysis+"_possible_values` SET `field` = 'check_segregation', `value` = '"+val+"'");
			}
			updateAndPrint(Schema.HIGHLANDER, "DELETE FROM `"+analysis+"_possible_values` WHERE `field` IN ('check_insilico','check_validated_change','check_somatic_change') AND `value` IN ('TRUE','FALSE')");
		}
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `description`='Consensus between prediction of all available software. The base score reflects the number software that predict the variant to be damaging: DAMAGING in Sift, DELETERIOUS in LRT, HIGH or MEDIUM in Mutation Assessor, DAMAGING in FATHMM, DISEASE_CAUSING (AUTOMATIC or not) in Mutation Taster, a score > 0.5 in Polyphen2 (hdiv or hvar). So max 6 if Sift, LRT, Mutation Assessor, FATHMM, Mutation Taster and Polyphen2 all agree. If the variant could affect splicing, a +1 or +2 could be added to the base score, if ada and/or rf score are > 0.6. To this, is added +10 for STOP_LOST, START_LOST, CODON_CHANGE, CODON_CHANGE_PLUS_CODON_DELETION, CODON_CHANGE_PLUS_CODON_INSERTION,	CODON_DELETION, CODON_INSERTION or RARE_AMINO_ACID ; +20 for SPLICE_SITE_REGION ; +30 for SPLICE_SITE_ACCEPTOR, SPLICE_SITE_DONOR or EXON_DELETED ; +40 for FRAME_SHIFT or STOP_GAINED (annotations from SnpEff).' WHERE `field`='consensus_prediction'");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `field`='dbsnp_id' WHERE `field`='dbsnp_id_137'");
		updateAndPrint(Schema.HIGHLANDER, "DELETE FROM `fields` WHERE `field` = 'dbsnp_id_141'");
		updateAndPrint(Schema.HIGHLANDER, "DELETE FROM `users_data` WHERE type = 'COLUMN_SELECTION' AND `value` = 'dbsnp_id_141'") ;
		updateAndPrint(Schema.HIGHLANDER, "DELETE FROM `users_data` WHERE type = 'COLUMN_MASK' AND `value` = 'dbsnp_id_141'") ;
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'dbsnp_id_137','dbsnp_id') WHERE INSTR(`value`,'dbsnp_id_137') > 0");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `key` = REPLACE(`value`,'dbsnp_id_137','dbsnp_id') WHERE INSTR(`key`,'dbsnp_id_137') > 0");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'dbsnp_id_141','dbsnp_id') WHERE INSTR(`value`,'dbsnp_id_141') > 0");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `key` = REPLACE(`value`,'dbsnp_id_141','dbsnp_id') WHERE INSTR(`key`,'dbsnp_id_141') > 0");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'public_comments','public_comments_variant') WHERE INSTR(`value`,'public_comments') > 0");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `key` = REPLACE(`key`,'public_comments','public_comments_variant') WHERE INSTR(`key`,'public_comments') > 0");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'of_interest','of_interest_variant') WHERE INSTR(`value`,'of_interest') > 0");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `key` = REPLACE(`key`,'of_interest','of_interest_variant') WHERE INSTR(`key`,'of_interest') > 0");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'private_comments','private_comments_variant') WHERE INSTR(`value`,'private_comments') > 0");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `key` = REPLACE(`key`,'private_comments','private_comments_variant') WHERE INSTR(`key`,'private_comments') > 0");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `alignment`='LEFT', `size`='large', `description`='This field can be used if this variant has been evaluated insilico (e.g. by looking at the alignment): OK (it\\'s likely a real change), NOT_OK (it\\'s likely a sequencing error, e.g. alternative allele not specific to pathology), SUSPECT (checked insilico but not sure if it\\'s real or not) or NOT_CHECKED (Not checked insilico, default value). Only users associated with the given sample can change this field, but any user can see the assigned value.' WHERE `field`='check_insilico'");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `description`='User who has set the check_insilico field.' WHERE `field`='check_insilico_username'");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `alignment`='LEFT', `size`='large', `description`='This field can be used if this variant has been tested in the lab: VALIDATED (the change has been confirmed with another lab technique, e.g. by Sanger sequencing), INVALIDATED (the change has been invalidated and is not real), SUSPECT (the change has been checked with another technique, but cannot be confirmed or invalidate) or NOT_CHECKED (Not checked with another lab technique, default value). Only users associated with the given sample can change this field, but any user can see the assigned value.' WHERE `field`='check_validated_change'");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `description`='User who has set the check_validated_change field.' WHERE `field`='check_validated_change_username'");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `alignment`='LEFT', `size`='large', `description`='This field can be used if this variant has been evaluated for mosaicism: SOMATIC (it\\'s likely a somatic change), GERMLINE (it\\'s likely a germline change), DUBIOUS (checked for mosaicism, but impossible to differenciate between somatic or germline) or NOT_CHECKED (Not checked for mosaicism, default value). Only users associated with the given sample can change this field, but any user can see the assigned value.' WHERE `field`='check_somatic_change'");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `description`='User who has set the check_somatic_change field.' WHERE `field`='check_somatic_change_username'");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('clinvar_ids','ClinVar ids','Alamut batch 1.4.2','related identifiers','medium','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('clinvar_origins','ClinVar origins. Possible values: germline, somatic, de novo, maternal, etc.','Alamut batch 1.4.2','related identifiers','medium','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('clinvar_methods','ClinVar methods. Possible values: clinical testing, research, literature only, etc','Alamut batch 1.4.2','related identifiers','medium','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('clinvar_clin_signifs','ClinVar clinical significances','Alamut batch 1.4.2','related identifiers','medium','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('clinvar_review_status','ClinVar review status. Number of stars (0-4).','Alamut batch 1.4.2','related identifiers','medium','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('clinvar_phenotypes','ClinVar phenotypes','Alamut batch 1.4.2','related identifiers','medium','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('hgmd_id','HGMD mutation id','Alamut batch 1.4.2','related identifiers','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('hgmd_phenotype','HGMD phenotype','Alamut batch 1.4.2','related identifiers','medium','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('hgmd_pubmed_id','HGMD PubMed id','Alamut batch 1.4.2','related identifiers','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('hgmd_sub_category','HGMD sub-category (DP, DFP, FP, FTV, DM?, DM )','Alamut batch 1.4.2','related identifiers','medium','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('deogen_score','DEOGEN predicted score.','DEOGEN','effect prediction','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('deogen_pred','DEOGEN discrete prediction.','DEOGEN','effect prediction','medium','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('gevact_first_score','Pre-classification score from the GeVaCT classification tool','GeVaCT','effect prediction','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('gevact_first_class','Initial class allocated to a variant based on gevact_first_score (classes are as defined by Sharon et al, 2008 for variant pathogenic classification)','GeVaCT','effect prediction','medium','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('gevact_final_score','Final classification score made by the GeVaCT classification tool, based on gevact_first_score and manual inputs','GeVaCT','effect prediction','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('gevact_final_class','Final class allocated to a variant based on gevact_final_score (classes are as defined by Sharon et al, 2008 for variant pathogenic classification)','GeVaCT','effect prediction','medium','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('exac_an','Total allele number in ExAC for position','ExAC 0.3','allele frequency in population','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('dbsnp_maf','dbSNP variation global Minor Allele Frequency','Alamut batch 1.4.2','allele frequency in population','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('grantham_dist','Grantham distance','Alamut batch 1.4.2','other scores','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('agvgd_class','AlignGVGD class','Alamut batch 1.4.2','other scores','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('blosum62','BLOSUM62','Alamut batch 1.4.2','other scores','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('wt_ssf_score','WT seq. SpliceSiteFinder score','Alamut batch 1.4.2','other scores','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('wt_maxent_score','WT seq. MaxEntScan score','Alamut batch 1.4.2','other scores','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('wt_nns_score','WT seq. NNSPLICE score','Alamut batch 1.4.2','other scores','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('wt_gs_score','WT seq. GeneSplicer score','Alamut batch 1.4.2','other scores','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('wt_hsf_score','WT seq. HSF score','Alamut batch 1.4.2','other scores','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('var_ssf_score','Variant seq. SpliceSiteFinder score','Alamut batch 1.4.2','other scores','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('var_maxent_score','Variant seq. MaxEntScan score','Alamut batch 1.4.2','other scores','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('var_nns_score','Variant seq. NNSPLICE score','Alamut batch 1.4.2','other scores','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('var_gs_score','Variant seq. GeneSplicer score','Alamut batch 1.4.2','other scores','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('var_hsf_score','Variant seq. HSF score ','Alamut batch 1.4.2','other scores','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('local_splice_effect','Splicing effect in variation vicinity (New Donor Site, New Acceptor Site, Cryptic Donor Strongly Activated, Cryptic Donor Weakly Activated, Cryptic Acceptor Strongly Activated, Cryptic Acceptor Weakly Activated)','Alamut batch 1.4.2','other scores','small','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('of_interest_variant','You can set this flag to true if the variant is of interest for you, or to false otherwise.','Highlander users','user annotations','small','CENTER')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('private_comments_variant','Private comments about the variant, visible only by you.','Highlander users','user annotations','largest','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('of_interest_gene','You can set this flag to true if the gene is of interest for you, or to false otherwise.','Highlander users','user annotations','small','CENTER')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('public_comments_gene','Public comments about the gene, visible and modifiable by all users.','Highlander users','user annotations','largest','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `fields` VALUES ('private_comments_gene','Private comments about the gene, visible only by you.','Highlander users','user annotations','largest','LEFT')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `description`='Public comments about the variant, visible and modifiable by all users.' WHERE `field`='public_comments'");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `field`='public_comments_variant' WHERE `field`='public_comments'");
	}

}
