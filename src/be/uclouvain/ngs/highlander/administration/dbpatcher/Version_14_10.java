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

import java.util.HashSet;
import java.util.Set;

import be.uclouvain.ngs.highlander.database.DBUtils;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.DBUtils.VariantKind;
import be.uclouvain.ngs.highlander.database.DBUtils.VariantNovelty;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;

/**
* @author Raphael Helaers
*/

public class Version_14_10 extends Version {

	public Version_14_10() {
		super("14.10");
	}

	@Override
	protected void makeUpdate() throws Exception {
		updateAndPrint(Schema.HIGHLANDER, "set session group_concat_max_len = 16384");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `analyses` CHANGE `variant_caller` `variant_caller` ENUM('GATK', 'MUTECT', 'TORRENT', 'LIFESCOPE', 'OTHER') NOT NULL DEFAULT 'OTHER'");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `analyses` ADD COLUMN `ordering` INT NOT NULL DEFAULT 0 COMMENT 'Ordering of the analyses icons in Highlander' AFTER `icon`");
		for (Analysis analysis : AnalysisFull.getAvailableAnalyses()){
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` CHANGE `platform` `platform` VARCHAR(50) DEFAULT NULL COMMENT 'Platform on which the variant has been detected.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` CHANGE `1000G_AC` `the1000G_AC` INT DEFAULT NULL COMMENT 'Alternative allele counts in the whole 1000 genomes phase 1 data.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` CHANGE `1000G_AF` `the1000G_AF` DOUBLE DEFAULT NULL COMMENT 'Alternative allele frequency in the whole 1000Gp1 data.'");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `field`='the1000G_AC' WHERE `field`='1000G_AC'");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `field`='the1000G_AF' WHERE `field`='1000G_AF'");
			updateAndPrint(Schema.HIGHLANDER, "DROP TABLE IF EXISTS `"+analysis+"_other_transcripts_polyphen`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` CHANGE `other_effects` `snpeff_other_transcripts` TEXT DEFAULT NULL COMMENT 'SnpEff prediction for other transcripts.'");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `description`='SnpEff prediction for other transcripts.' WHERE `field`='other_effects'");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `source`='snpEff 4.1' WHERE `field`='other_effects'");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `size`='largest' WHERE `field`='other_effects'");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `alignment`='LEFT' WHERE `field`='other_effects'");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `field`='snpeff_other_transcripts' WHERE `field`='other_effects'");
			updateAndPrint(Schema.HIGHLANDER, "DELETE FROM `"+analysis+"_possible_values` where `field` = 'other_effects'");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `"+analysis+"` as a, (SELECT variant_id, group_concat(transcript_ensembl,':',snpeff_effect SEPARATOR ';') as val FROM `"+analysis+"_other_transcripts_snpeff` GROUP BY variant_id) as b SET a.`snpeff_other_transcripts` = b.val WHERE a.id = b.variant_id");
			updateAndPrint(Schema.HIGHLANDER, "DROP TABLE IF EXISTS `"+analysis+"_other_transcripts_snpeff`");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` CHANGE `sample_type` `sample_type` ENUM('Blood','Tissue','Cells','Germline','Somatic') NOT NULL DEFAULT 'Germline' COMMENT 'Sample type (Germline or Somatic).'");				
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `"+analysis+"` SET `sample_type` = 'Germline' WHERE `sample_type` = 'Blood'");				
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `"+analysis+"` SET `sample_type` = 'Somatic' WHERE `sample_type` IN ('Tissue','Cells')");				
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` CHANGE `sample_type` `sample_type` ENUM('Germline','Somatic') NOT NULL DEFAULT 'Germline' COMMENT 'Sample type (Germline or Somatic).'");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `fields` SET `description`='Sample type (Germline or Somatic).' WHERE `field`='sample_type'");
			updateAndPrint(Schema.HIGHLANDER, "DELETE FROM `"+analysis+"_possible_values` where `field` = 'sample_type'");
			updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `"+analysis+"_possible_values` SET `field` = 'sample_type', `value` = 'Germline'");
			updateAndPrint(Schema.HIGHLANDER, "INSERT INTO `"+analysis+"_possible_values` SET `field` = 'sample_type', `value` = 'Somatic'");

			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_general_stats` CHANGE `num_patient_blood` `num_patient_germline` SMALLINT NOT NULL COMMENT 'Number of available germline samples from this pathology.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_general_stats` CHANGE `num_patient_tissues` `num_patient_somatic` SMALLINT NOT NULL COMMENT 'Number of available somatic samples from this pathology.'");

			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_gene_stats` CHANGE `gene_num_blood_unfilt_all` `gene_num_germline_unfilt_all` SMALLINT NOT NULL COMMENT 'Number of germline samples in the NGS Highlander database having a variation in the same gene as this pathology. All variations are taken into account, whichever are their impact and even if they don\\'t pass filters.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_gene_stats` CHANGE `gene_num_blood_unfilt_dam` `gene_num_germline_unfilt_dam` SMALLINT NOT NULL COMMENT 'Number of germline samples in the NGS Highlander database having a variation in the same gene as this pathology. Only variations that have been predicted damaging by at least one software are taken into account, but even if they don\\'t pass filters.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_gene_stats` CHANGE `gene_num_blood_filt_all` `gene_num_germline_filt_all` SMALLINT NOT NULL COMMENT 'Number of germline samples in the NGS Highlander database having a variation in the same gene as this pathology. Only variations that pass LowQual and GATK filters are taken into account, but wichever are their impact.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_gene_stats` CHANGE `gene_num_blood_filt_dam` `gene_num_germline_filt_dam` SMALLINT NOT NULL COMMENT 'Number of germline samples in the NGS Highlander database having a variation in the same gene as this pathology. Only variations that have been predicted damaging by at least one software and pass LowQual and GATK filters are taken into account.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_gene_stats` CHANGE `gene_num_tissues_unfilt_all` `gene_num_somatic_unfilt_all` SMALLINT NOT NULL COMMENT 'Number of somatic samples in the NGS Highlander database having a variation in the same gene as this pathology. All variations are taken into account, whichever are their impact and even if they don\\'t pass filters.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_gene_stats` CHANGE `gene_num_tissues_unfilt_dam` `gene_num_somatic_unfilt_dam` SMALLINT NOT NULL COMMENT 'Number of somatic samples in the NGS Highlander database having a variation in the same gene as this pathology. Only variations that have been predicted damaging by at least one software are taken into account, but even if they don\\'t pass filters.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_gene_stats` CHANGE `gene_num_tissues_filt_all` `gene_num_somatic_filt_all` SMALLINT NOT NULL COMMENT 'Number of somatic samples in the NGS Highlander database having a variation in the same gene as this pathology. Only variations that pass LowQual and GATK filters are taken into account, but wichever are their impact.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_gene_stats` CHANGE `gene_num_tissues_filt_dam` `gene_num_somatic_filt_dam` SMALLINT NOT NULL COMMENT 'Number of somatic samples in the NGS Highlander database having a variation in the same gene as this pathology. Only variations that have been predicted damaging by at least one software and pass LowQual and GATK filters are taken into account.'");

			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_change_stats` CHANGE `change_num_blood_unfilt` `change_num_germline_unfilt` SMALLINT NOT NULL COMMENT 'Number of germline samples in the NGS Highlander database having the SAME variation at same position as this pathology. All variations are taken into account even if they don\\'t pass filters.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_change_stats` CHANGE `change_num_blood_filt` `change_num_germline_filt` SMALLINT NOT NULL COMMENT 'Number of germline samples in the NGS Highlander database having the SAME variation at same position as this pathology. Only variations that pass LowQual and GATK filters are taken into account.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_change_stats` CHANGE `change_num_tissues_unfilt` `change_num_somatic_unfilt` SMALLINT NOT NULL COMMENT 'Number of somatic samples in the NGS Highlander database having the SAME variation at same position as this pathology. All variations are taken into account even if they don\\'t pass filters.'");
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_change_stats` CHANGE `change_num_tissues_filt` `change_num_somatic_filt` SMALLINT NOT NULL COMMENT 'Number of somatic samples in the NGS Highlander database having the SAME variation at same position as this pathology. Only variations that pass LowQual and GATK filters are taken into account.'");
		}			
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` CHANGE `platform` `platform` VARCHAR(50) DEFAULT NULL COMMENT 'Platform on which the variant has been detected.'");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` CHANGE `read_length` `read_length` VARCHAR(50) DEFAULT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `non_barcoded`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `use_ecc`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` ADD COLUMN `coverage_exome_wo_dup` INT DEFAULT NULL AFTER `percent_of_target_covered_meq_30X_wo_dup`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` ADD COLUMN `percent_of_exome_covered_meq_1X_wo_dup` decimal(5,2) DEFAULT NULL AFTER `coverage_exome_wo_dup`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` ADD COLUMN `percent_of_exome_covered_meq_5X_wo_dup` decimal(5,2) DEFAULT NULL AFTER `percent_of_exome_covered_meq_1X_wo_dup`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` ADD COLUMN `percent_of_exome_covered_meq_10X_wo_dup` decimal(5,2) DEFAULT NULL AFTER `percent_of_exome_covered_meq_5X_wo_dup`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` ADD COLUMN `percent_of_exome_covered_meq_20X_wo_dup` decimal(5,2) DEFAULT NULL AFTER `percent_of_exome_covered_meq_10X_wo_dup`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` ADD COLUMN `percent_of_exome_covered_meq_30X_wo_dup` decimal(5,2) DEFAULT NULL AFTER `percent_of_exome_covered_meq_20X_wo_dup`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` ADD COLUMN `gene_coverage_ratio_chr_xy` DOUBLE DEFAULT NULL AFTER `percent_not_totally_on_target`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` ADD COLUMN `ti_tv_ratio_all` DOUBLE DEFAULT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` ADD COLUMN `ti_tv_ratio_known` DOUBLE DEFAULT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` ADD COLUMN `ti_tv_ratio_novel` DOUBLE DEFAULT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` ADD COLUMN `het_hom_ratio_all` DOUBLE DEFAULT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` ADD COLUMN `het_hom_ratio_known` DOUBLE DEFAULT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` ADD COLUMN `het_hom_ratio_novel` DOUBLE DEFAULT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` ADD COLUMN `variant_count_all` INT DEFAULT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` ADD COLUMN `variant_count_known` INT DEFAULT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` ADD COLUMN `variant_count_novel` INT DEFAULT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` ADD COLUMN `variant_count_pass_filters_all` INT DEFAULT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` ADD COLUMN `variant_count_pass_filters_known` INT DEFAULT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` ADD COLUMN `variant_count_pass_filters_novel` INT DEFAULT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` ADD COLUMN `SNP_count_all` INT DEFAULT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` ADD COLUMN `SNP_count_known` INT DEFAULT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` ADD COLUMN `SNP_count_novel` INT DEFAULT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` ADD COLUMN `SNP_count_pass_filters_all` INT DEFAULT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` ADD COLUMN `SNP_count_pass_filters_known` INT DEFAULT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` ADD COLUMN `SNP_count_pass_filters_novel` INT DEFAULT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` ADD COLUMN `INDEL_count_all` INT DEFAULT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` ADD COLUMN `INDEL_count_known` INT DEFAULT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` ADD COLUMN `INDEL_count_novel` INT DEFAULT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` ADD COLUMN `INDEL_count_pass_filters_all` INT DEFAULT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` ADD COLUMN `INDEL_count_pass_filters_known` INT DEFAULT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` ADD COLUMN `INDEL_count_pass_filters_novel` INT DEFAULT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `het_hom_ratio_ls`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `ti_tv_ratio_ls`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `ti_tv_ratio_all_gatk`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `ti_tv_ratio_known_gatk`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `ti_tv_ratio_novel_gatk`");
		//Computing new annotations in projects_analysis
		toConsole("Computing new annotations for all projects");
		Set<Integer> ids = new HashSet<>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT id FROM `projects`")) {
			while(res.next()){
				ids.add(res.getInt(1));
			}
		}
		for (int project_id : ids){
			//Removed at version 17 because reference is now needed for that method
			//updateAndPrint(Schema.HIGHLANDER, "UPDATE projects SET `gene_coverage_ratio_chr_xy` = '"+DBUtils.getGeneCoverageRatioChrXY(project_id)+"' WHERE id = " + project_id);
			Set<Analysis> analyses = new HashSet<>();
			try (Results res = DB.select(Schema.HIGHLANDER, "SELECT analysis FROM `projects_analyses` WHERE id_project = "+project_id)) {
				while(res.next()){
					analyses.add(new Analysis(res.getString(1)));
				}
			}
			for (Analysis analysis : analyses){
				updateAndPrint(Schema.HIGHLANDER, "UPDATE projects_analyses SET `het_hom_ratio_all` = '"+DBUtils.getHetHomRatio(project_id, analysis, VariantNovelty.all)+"' WHERE id_project = " + project_id + " AND analysis = '"+analysis+"'");
				updateAndPrint(Schema.HIGHLANDER, "UPDATE projects_analyses SET `het_hom_ratio_known` = '"+DBUtils.getHetHomRatio(project_id, analysis, VariantNovelty.known)+"' WHERE id_project = " + project_id + " AND analysis = '"+analysis+"'");
				updateAndPrint(Schema.HIGHLANDER, "UPDATE projects_analyses SET `het_hom_ratio_novel` = '"+DBUtils.getHetHomRatio(project_id, analysis, VariantNovelty.novel)+"' WHERE id_project = " + project_id + " AND analysis = '"+analysis+"'");
				updateAndPrint(Schema.HIGHLANDER, "UPDATE projects_analyses SET `ti_tv_ratio_all` = '"+DBUtils.getTiTvRatio(project_id, analysis, VariantNovelty.all)+"' WHERE id_project = " + project_id + " AND analysis = '"+analysis+"'");
				updateAndPrint(Schema.HIGHLANDER, "UPDATE projects_analyses SET `ti_tv_ratio_known` = '"+DBUtils.getTiTvRatio(project_id, analysis, VariantNovelty.known)+"' WHERE id_project = " + project_id + " AND analysis = '"+analysis+"'");
				updateAndPrint(Schema.HIGHLANDER, "UPDATE projects_analyses SET `ti_tv_ratio_novel` = '"+DBUtils.getTiTvRatio(project_id, analysis, VariantNovelty.novel)+"' WHERE id_project = " + project_id + " AND analysis = '"+analysis+"'");
				updateAndPrint(Schema.HIGHLANDER, "UPDATE projects_analyses SET `variant_count_all` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.all, false, VariantNovelty.all)+"' WHERE id_project = " + project_id + " AND analysis = '"+analysis+"'");
				updateAndPrint(Schema.HIGHLANDER, "UPDATE projects_analyses SET `variant_count_known` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.all, false, VariantNovelty.known)+"' WHERE id_project = " + project_id + " AND analysis = '"+analysis+"'");
				updateAndPrint(Schema.HIGHLANDER, "UPDATE projects_analyses SET `variant_count_novel` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.all, false, VariantNovelty.novel)+"' WHERE id_project = " + project_id + " AND analysis = '"+analysis+"'");
				updateAndPrint(Schema.HIGHLANDER, "UPDATE projects_analyses SET `variant_count_pass_filters_all` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.all, true, VariantNovelty.all)+"' WHERE id_project = " + project_id + " AND analysis = '"+analysis+"'");
				updateAndPrint(Schema.HIGHLANDER, "UPDATE projects_analyses SET `variant_count_pass_filters_known` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.all, true, VariantNovelty.known)+"' WHERE id_project = " + project_id + " AND analysis = '"+analysis+"'");
				updateAndPrint(Schema.HIGHLANDER, "UPDATE projects_analyses SET `variant_count_pass_filters_novel` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.all, true, VariantNovelty.novel)+"' WHERE id_project = " + project_id + " AND analysis = '"+analysis+"'");
				updateAndPrint(Schema.HIGHLANDER, "UPDATE projects_analyses SET `SNP_count_all` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.snp, false, VariantNovelty.all)+"' WHERE id_project = " + project_id + " AND analysis = '"+analysis+"'");
				updateAndPrint(Schema.HIGHLANDER, "UPDATE projects_analyses SET `SNP_count_known` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.snp, false, VariantNovelty.known)+"' WHERE id_project = " + project_id + " AND analysis = '"+analysis+"'");
				updateAndPrint(Schema.HIGHLANDER, "UPDATE projects_analyses SET `SNP_count_novel` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.snp, false, VariantNovelty.novel)+"' WHERE id_project = " + project_id + " AND analysis = '"+analysis+"'");
				updateAndPrint(Schema.HIGHLANDER, "UPDATE projects_analyses SET `SNP_count_pass_filters_all` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.snp, true, VariantNovelty.all)+"' WHERE id_project = " + project_id + " AND analysis = '"+analysis+"'");
				updateAndPrint(Schema.HIGHLANDER, "UPDATE projects_analyses SET `SNP_count_pass_filters_known` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.snp, true, VariantNovelty.known)+"' WHERE id_project = " + project_id + " AND analysis = '"+analysis+"'");
				updateAndPrint(Schema.HIGHLANDER, "UPDATE projects_analyses SET `SNP_count_pass_filters_novel` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.snp, true, VariantNovelty.novel)+"' WHERE id_project = " + project_id + " AND analysis = '"+analysis+"'");
				updateAndPrint(Schema.HIGHLANDER, "UPDATE projects_analyses SET `INDEL_count_all` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.indel, false, VariantNovelty.all)+"' WHERE id_project = " + project_id + " AND analysis = '"+analysis+"'");
				updateAndPrint(Schema.HIGHLANDER, "UPDATE projects_analyses SET `INDEL_count_known` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.indel, false, VariantNovelty.known)+"' WHERE id_project = " + project_id + " AND analysis = '"+analysis+"'");
				updateAndPrint(Schema.HIGHLANDER, "UPDATE projects_analyses SET `INDEL_count_novel` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.indel, false, VariantNovelty.novel)+"' WHERE id_project = " + project_id + " AND analysis = '"+analysis+"'");
				updateAndPrint(Schema.HIGHLANDER, "UPDATE projects_analyses SET `INDEL_count_pass_filters_all` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.indel, true, VariantNovelty.all)+"' WHERE id_project = " + project_id + " AND analysis = '"+analysis+"'");
				updateAndPrint(Schema.HIGHLANDER, "UPDATE projects_analyses SET `INDEL_count_pass_filters_known` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.indel, true, VariantNovelty.known)+"' WHERE id_project = " + project_id + " AND analysis = '"+analysis+"'");
				updateAndPrint(Schema.HIGHLANDER, "UPDATE projects_analyses SET `INDEL_count_pass_filters_novel` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.indel, true, VariantNovelty.novel)+"' WHERE id_project = " + project_id + " AND analysis = '"+analysis+"'");
			}
		}
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` ADD COLUMN `reference` VARCHAR(255) DEFAULT NULL AFTER `pair_end`");

		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` CHANGE `sample_type` `sample_type` ENUM('Blood','Tissue','Cells','Germline','Somatic') NOT NULL DEFAULT 'Germline' COMMENT 'Sample type (Germline or Somatic).'");				
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `projects` SET `sample_type` = 'Germline' WHERE `sample_type` = 'Blood'");				
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `projects` SET `sample_type` = 'Somatic' WHERE `sample_type` IN ('Tissue','Cells')");				
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` CHANGE `sample_type` `sample_type` ENUM('Germline','Somatic') NOT NULL DEFAULT 'Germline' COMMENT 'Sample type (Germline or Somatic).'");

		updateAndPrint(Schema.HIGHLANDER, "UPDATE `analyses` SET `stats` = '0'");

		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'£','$') WHERE INSTR(`value`,'£') > 0");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'other_effects','snpeff_other_transcripts')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'1000G_','the1000G_')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'blood','germline')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'tissues','somatic')");
	}

}
