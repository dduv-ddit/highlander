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

import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.administration.DbBuilder;
import be.uclouvain.ngs.highlander.administration.DbUpdater;
import be.uclouvain.ngs.highlander.database.Category;
import be.uclouvain.ngs.highlander.database.DBUtils;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.SqlGenerator;
import be.uclouvain.ngs.highlander.database.Field.Annotation;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
import be.uclouvain.ngs.highlander.datatype.AnnotatedVariant;
import be.uclouvain.ngs.highlander.datatype.Reference;
import be.uclouvain.ngs.highlander.datatype.Report;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull.VariantCaller;
import be.uclouvain.ngs.highlander.tools.ExomeBed;
import be.uclouvain.ngs.highlander.tools.ExomeBed.Region;

/**
 * @author Raphael Helaers
 */

public class Version_17 extends Version {

	public Version_17() {
		super("17");
	}

	/**
	 * This method allow to fetch analyses from a version 16 database 
	 * @return
	 * @throws Exception
	 */
	private List<Analysis> getAvailableAnalyses() throws Exception {
		List<Analysis> analyses = new ArrayList<Analysis>();
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT analysis FROM analyses ORDER BY ordering")) {
			while (res.next()){
				analyses.add(new Analysis(res.getString("analysis")));
			}
		}
		return analyses;
	}
	
	private VariantCaller getVariantCaller(Analysis analysis) {
		VariantCaller vc = VariantCaller.GATK;
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT variant_caller FROM analyses WHERE analysis = '"+analysis+"'")) {
			if (res.next()){
				vc = VariantCaller.valueOf(res.getString("variant_caller"));
			}
		}catch(Exception ex) {
			ex.printStackTrace();
		}
		return vc;
	}
	
	@Override
	protected void makeUpdate() throws Exception {
		boolean found = false;
		try (Results res = DB.select(Schema.HIGHLANDER, "SHOW FUNCTION STATUS WHERE Name = 'levenshtein'")) {
			if (res.next()) {
				found = true;
			}
		}
		if (!found) {
			//Create Levenshtein function
			toConsole("---[ Create Levenshtein function ]---");
			updateAndPrint(Schema.HIGHLANDER, SqlGenerator.createFunctions().toString());
		}

		//Add SV caller possibility for analyses
		toConsole("---[ Add SV caller possibility for analyses ]---");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `analyses` MODIFY `variant_caller` ENUM('GATK', 'MUTECT', 'TORRENT', 'LIFESCOPE', 'SV', 'OTHER') NOT NULL DEFAULT 'OTHER'");
		
		//Add group and active status to users 
		toConsole("---[ Add group and active status to users ]---");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `users` ADD COLUMN `group` varchar(255) NOT NULL DEFAULT 'Main'");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `users` MODIFY `rights` ENUM('user','administrator','inactive') NOT NULL DEFAULT 'user'");

		//Change some field and table names, to harmonize in all Highlander
		//change -> variant 
		//variation -> variant
		//SNP -> SNV
		//MNP -> MNV
		//group -> pathology
		//patient -> sample
		//Patient -> Sample
		//PATIENT -> SAMPLE
		toConsole("---[ Change some field and table names, to harmonize in all Highlander ]---");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `analyses` CHANGE `vcf_snp_extension` `vcf_extension` VARCHAR(255) NOT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `analyses` DROP COLUMN `vcf_indel_extension`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `analyses` DROP COLUMN `stats`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `main` CHANGE `lucky` `beta_functionalities` BOOLEAN DEFAULT FALSE COMMENT 'Set to true to activate functionnalities that still need testing'");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` CHANGE `patient` `sample` VARCHAR(50) DEFAULT NULL COMMENT 'Sample identifier.'");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` CHANGE `SNP_count_all` `SNV_count_all` INT DEFAULT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` CHANGE `SNP_count_known` `SNV_count_known` INT DEFAULT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` CHANGE `SNP_count_novel` `SNV_count_novel` INT DEFAULT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` CHANGE `SNP_count_pass_filters_all` `SNV_count_pass_filters_all` INT DEFAULT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` CHANGE `SNP_count_pass_filters_known` `SNV_count_pass_filters_known` INT DEFAULT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` CHANGE `SNP_count_pass_filters_novel` `SNV_count_pass_filters_novel` INT DEFAULT NULL");
		for (Analysis analysis : getAvailableAnalyses()){
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"` CHANGE `change_type` `variant_type` ENUM('SNP','MNP','INS','DEL','SV') DEFAULT NULL");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `"+analysis+"_possible_values` SET `field` = 'sample' WHERE `field` = 'patient'");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `"+analysis+"_possible_values` SET `field` = 'variant_type' WHERE `field` = 'change_type'");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `"+analysis+"_possible_values` SET `value` = 'SNV' WHERE `field` = 'variant_type' AND `value` = 'SNP'");
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `"+analysis+"_possible_values` SET `value` = 'MNV' WHERE `field` = 'variant_type' AND `value` = 'MNP'");
		}

		//Harmonize project_id field name in all tables
		toConsole("---[ Harmonize project_id field name in all tables ]---");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` CHANGE `id` `project_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_users` CHANGE `id_project` `project_id` INT NOT NULL");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` CHANGE `id_project` `project_id` INT NOT NULL");
		for (Analysis analysis : getAvailableAnalyses()){
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis+"_gene_coverage` CHANGE `id_project` `project_id` INT(10) UNSIGNED NOT NULL COMMENT 'Id in the projects table'");
		}

		//Add indexes in users_data to improve performances
		toConsole("---[ Add indexes in users_data to improve performances ]---");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `users_data` ADD INDEX `key` (`key`(5))");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `users_data` ADD INDEX `type` (`type`)");

		//Remove SOLID columns from projects
		toConsole("---[ Remove SOLID columns from projects ]---");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `total_bead_deposition`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `whole_run_reads_produced`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `percent_bad_beads`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `whole_run_assigned_reads`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `percent_unassigned_reads`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `reads_produced`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `reads_mapped`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `percent_total_mapped`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `low_mapqv_reads`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `percent_low_mapqv`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `reads_on`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `percent_on`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `reads_off`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `percent_off`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `enrichment_fold`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `num_targets_not_covered`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `target_bases_not_covered`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `percent_of_target_bases_not_covered`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `percent_not_totally_on_target`");

		//Add flags for pipeline support 
		toConsole("---[ Add flags for pipeline support ]---");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` ADD COLUMN `sequencing_target` VARCHAR(255) DEFAULT NULL COMMENT 'WGS, WES, panel, RNAseq, ...' AFTER `project_id`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `analyses` ADD COLUMN `sequencing_target` VARCHAR(255) DEFAULT NULL COMMENT 'WGS, WES, panel, RNAseq, ...' AFTER `ordering`");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `analyses` SET `sequencing_target` = 'WES'");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` ADD COLUMN `trim` BOOLEAN DEFAULT FALSE AFTER `pair_end`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` ADD COLUMN `remove_duplicates` BOOLEAN DEFAULT TRUE AFTER `trim`");

		//Move user warning to analysis
		toConsole("---[ Move user warning to analysis ]---");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `users_warned`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` ADD COLUMN `users_warned` BOOLEAN DEFAULT FALSE");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `projects_analyses` SET `users_warned` = TRUE");

		//Keep normal sample id in a column instead of comments
		toConsole("---[ Keep normal sample id in a column instead of comments ]---");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` ADD COLUMN `normal_id` INT(10) UNSIGNED DEFAULT NULL COMMENT 'project id of the NORMAL sample for NORMAL/TUMOR pairs' AFTER `sample_type`");
		Map<Integer,String> normals = new HashMap<>();
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT project_id, comments FROM `projects` WHERE INSTR(comments,'normal_sample')")) {
			while(res.next()){
				normals.put(res.getInt(1),res.getString(2));
			}
		}

		for (int id : normals.keySet()){
			String normalPath = normals.get(id).substring(normals.get(id).indexOf("normal_sample[")+"normal_sample[".length(),normals.get(id).lastIndexOf("]"));
			int normalId = -1;
			try (Results res = DB.select(Schema.HIGHLANDER, "SELECT project_id FROM `projects` WHERE run_path = '"+normalPath+"'")) {
				while(res.next()){
					if (res.getInt(1) > normalId) normalId = res.getInt(1);
				}
			}
			if (normalId > -1) {
				updateAndPrint(Schema.HIGHLANDER, "UPDATE `projects` SET `normal_id` = "+normalId+" WHERE project_id = " + id);
			}else {
				toConsole("project id " + id + " with comment " + normals.get(id) + " : normal id not found !");
			}
		}

		//New field 'individual': each sample belonging to the same individual must have the same 'individual' id.
		toConsole("---[ New field 'individual': each sample belonging to the same individual must have the same 'individual' id ]---");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` ADD COLUMN `individual` VARCHAR(50) NOT NULL COMMENT 'Each sample belonging to the same individual must have the same \\'individual\\' id' AFTER `outsourcing`");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `projects` SET `individual` = `sample`");
		//New field 'family': each individual belonging to the same family must have the same 'family' id.
		toConsole("---[ New field 'family': each individual belonging to the same family must have the same 'family' id ]---");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` ADD COLUMN `family` VARCHAR(50) NOT NULL COMMENT 'Each individual belonging to the same family must have the same \\'family\\' id' AFTER `outsourcing`");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `projects` SET `family` = `sample`");
		//New field 'index_case': Set to true for an individual when he or she is the index case of his or her family.
		toConsole("---[ New field 'index_case': Set to true for an individual when he or she is the index case of his or her family ]---");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` ADD COLUMN `index_case` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Set to true for an individual when he or she is the index case of his or her family' AFTER `sample`");
		
		//New field 'run_label', and easier to use than always concatenate run id+date+name
		toConsole("---[ New field 'run_label', and easier to use than always concatenate run id+date+name ]---");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` ADD COLUMN `run_label` VARCHAR(255) DEFAULT NULL COMMENT 'Name of the run (experiment number + date of run processing + name)' AFTER `run_name`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` ADD INDEX `run_label` (`run_label`)");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `projects` SET `run_label` = CONCAT(run_id,\"_\",REPLACE(run_date,\"-\",\"_\"),\"_\",run_name)");
		updateAndPrint(Schema.HIGHLANDER, "DROP INDEX `run_id` ON `projects`");

		//Add a table to record population information
		toConsole("---[ Add a table to record population information ]---");
		for (String query : SqlGenerator.split(SqlGenerator.createPopulations())) {
			updateAndPrint(Schema.HIGHLANDER, query);
		}
		for (String query : SqlGenerator.split(SqlGenerator.fillPopulations())) {
			updateAndPrint(Schema.HIGHLANDER, query);
		}
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` ADD COLUMN `population_id` INT(10) UNSIGNED DEFAULT NULL AFTER `pathology`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` ADD INDEX `population_id` (`population_id`)");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `projects` JOIN `populations` ON `projects`.`pathology` = `populations`.`population` SET `projects`.`population_id` = `populations`.`population_id`");
		
		//Permit longer pathologies (was limited to 10 characters) and add a numerical id to be used in other tables instead of string key (that could change)
		toConsole("---[ Permit longer pathologies (was limited to 10 characters) and add a numerical id to be used in other tables instead of string key (that could change) ]---");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `pathologies` DROP PRIMARY KEY, ADD COLUMN `pathology_id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `pathologies` MODIFY `pathology` VARCHAR(1000) NOT NULL COMMENT 'Patient pathology.'");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `pathologies` ADD COLUMN `pathology_description` TEXT NOT NULL AFTER `pathology`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` ADD COLUMN `pathology_id` INT(10) UNSIGNED DEFAULT NULL AFTER `pathology`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` ADD INDEX `pathology_id` (`pathology_id`)");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `projects` JOIN `pathologies` USING (`pathology`) SET `projects`.`pathology_id` = `pathologies`.`pathology_id`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `pathology`");
		
		//Move non-client specific parameters from settings to database
		toConsole("---[ Move non-client specific parameters from settings to database ]---");
		for (String query : SqlGenerator.split(SqlGenerator.createSettings())) {
			updateAndPrint(Schema.HIGHLANDER, query);
		}
		for (String query : SqlGenerator.split(SqlGenerator.fillSettings())) {
			updateAndPrint(Schema.HIGHLANDER, query);
		}

		//Create a reports table with list of software, sequencing_target, file names
		toConsole("---[ Create a reports table with list of software, sequencing_target, file names ]---");
		for (String query : SqlGenerator.split(SqlGenerator.createReports())) {
			updateAndPrint(Schema.HIGHLANDER, query);
		}

		//Fill the table for DDUV analyses (or if another DB has the same denomination it shoudl fit).
		toConsole("---[ Adding base software and files (you need to correct it to fit your own pipeline in Database management > Reports ]---");
		List<String> availableAnalyses = new ArrayList<>();
		for (Analysis analysis : getAvailableAnalyses()){
			availableAnalyses.add(analysis.toString());
		}
		Report report = new Report(); 
		report.setSoftware("FastQC");
		report.setDescription("Sequecing quality control");
		report.setPath("fastqc");
		for (String analysis : availableAnalyses) {
			if (!analysis.contains("torrent_caller") && !analysis.contains("mutect"))
			report.addAnalysis(new Analysis(analysis));
		}
		for (String ext : new String[] {".fastqc.zip"}) {
			report.addFile(ext, "");
		}
		report.insert();
		report = new Report(); 
		report.setSoftware("cn.Mops");
		report.setDescription("CNV call");
		report.setPath("cnmops");
		for (String analysis : new String[] {"genomes_haplotype_caller"}) {			
			if (availableAnalyses.contains(analysis)) {
				report.addAnalysis(new Analysis(analysis));
			}
		}
		for (String ext : new String[] {".cnmops.csv",".cnmops.vcf",".cnmops.stats.csv",".cnmops.bins.csv",".cnmops.rda",".cnmops.chromosomes.pdf",".cnmops.regions.pdf"}) {
			report.addFile(ext, "");
		}
		report.insert();
		report = new Report(); 
		report.setSoftware("ExomeDepth");
		report.setDescription("CNV call");
		report.setPath("exomedepth");
		for (String analysis : new String[] {"exomes_haplotype_caller","exomes_1000g"}) {
			if (availableAnalyses.contains(analysis)) {
				report.addAnalysis(new Analysis(analysis));
			}
		}
		for (String ext : new String[] {".exomedepth.csv",".exomedepth.stats.csv",".exomedepth.pdf",".exomedepth.rda",".exomedepth.vcf"}) {
			report.addFile(ext, "");
		}
		report.insert();
		report = new Report(); 
		report.setSoftware("Convading");
		report.setDescription("CNV call");
		report.setPath("convading");
		for (String analysis : new String[] {"panels_torrent_caller","panels_haplotype_caller"}) {
			if (availableAnalyses.contains(analysis)) {
				report.addAnalysis(new Analysis(analysis));
			}
		}
		for (String ext : new String[] {".convading.normalized.coverage.txt",".convading.best.match.score.txt",".convading.normalized.autosomal.coverage.all.controls.txt",".convading.best.score.log",".convading.best.score.longlist.txt",".convading.best.score.shortlist.txt",".convading.best.score.totallist.txt"}) {
			report.addFile(ext, "");
		}
		report.insert();
		report = new Report(); 
		report.setSoftware("FACETS");
		report.setDescription("Somatic CNV call");
		report.setPath("facets");
		for (String analysis : new String[] {"exomes_mutect","genomes_mutect","panels_mutect"}) {
			if (availableAnalyses.contains(analysis)) {
				report.addAnalysis(new Analysis(analysis));
			}
		}
		for (String ext : new String[] {".facets.png",".facets.scores",".facets.tsv"}) {
			report.addFile(ext, "");
		}
		report.insert();
		report = new Report(); 
		report.setSoftware("Sequenza");
		report.setDescription("Somatic CNV call");
		report.setPath("sequenza");
		for (String analysis : new String[] {"exomes_mutect","genomes_mutect","panels_mutect"}) {
			if (availableAnalyses.contains(analysis)) {
				report.addAnalysis(new Analysis(analysis));
			}
		}
		for (String ext : new String[] {".mutations.sequenza.vcf",".sequenza.chromosome_depths.pdf",".sequenza.chromosome_view.pdf",".sequenza.CN_bars.pdf",".sequenza.confints_CP.txt",".sequenza.CP_contours.pdf",".sequenza.gc_plots.pdf",".sequenza.genome_view.pdf",".sequenza.model_fit.pdf",".sequenza.mutations.txt",".sequenza.segments.txt"}) {
			report.addFile(ext, "");
		}
		report.insert();
		report = new Report(); 
		report.setSoftware("MSISensor");
		report.setDescription("Somatic microsat instability");
		report.setPath("msisensor");
		for (String analysis : new String[] {"exomes_mutect","genomes_mutect","panels_mutect"}) {
			if (availableAnalyses.contains(analysis)) {
				report.addAnalysis(new Analysis(analysis));
			}
		}
		for (String ext : new String[] {".tsv","_dis_tab.tsv","_germline.tsv","_somatic.tsv"}) {
			report.addFile(ext, "");
		}
		report.insert();		

		//Create a references table to link reference fields in projects and analyses to Ensembl schemas
		toConsole("---[ Create a references table to link reference fields in projects and analyses to Ensembl schemas ]---");
		for (String query : SqlGenerator.split(SqlGenerator.createReferences())) {
			updateAndPrint(Schema.HIGHLANDER, query);
		}		
		for (String query : SqlGenerator.split(SqlGenerator.fillReferences())) {
			updateAndPrint(Schema.HIGHLANDER, query);
		}

		//Add created references to DB object 
		DB.addReference(new Reference("GRCh37"));
		DB.addReference(new Reference("b37"));
		DB.addReference(new Reference("b37_decoy"));
		DB.addReference(new Reference("hg19"));
		DB.addReference(new Reference("GRCm38"));
		DB.addReference(new Reference("GRCh38"));

		//Genome version (reference) should be in project_analysis because it's linked to analysis
		toConsole("---[ Genome version (reference) should be in project_analysis because it's linked to analysis ]---");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `analyses` ADD COLUMN `reference` VARCHAR(255) DEFAULT NULL AFTER `sequencing_target`");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `analyses` SET `reference` = 'GRCh37'");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `reference`");

		//Create a table to store external links displayed in the detail box of Highlander
		toConsole("---[ Create a table to store external links displayed in the detail box of Highlander ]---");
		for (String query : SqlGenerator.split(SqlGenerator.createExternalLinks())) {
			updateAndPrint(Schema.HIGHLANDER, query);
		}
		DbBuilder.addextlinks();

		//gene_coverage_ratio_chr_xy should be in project_analysis because it needs reference
		toConsole("---[ gene_coverage_ratio_chr_xy should be in project_analysis because it needs reference ]---");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` ADD COLUMN `gene_coverage_ratio_chr_xy` DOUBLE DEFAULT NULL after `INDEL_count_pass_filters_novel`");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE projects_analyses as A LEFT JOIN projects as P USING (project_id) SET A.gene_coverage_ratio_chr_xy = P.gene_coverage_ratio_chr_xy");		
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `gene_coverage_ratio_chr_xy`");
		
		//run_path should be in project_analysis because it needs reference
		toConsole("---[ run_path should be in project_analysis because it needs reference ]---");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` ADD COLUMN `run_path` varchar(255) DEFAULT NULL after `analysis`");
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects_analyses` ADD INDEX `run_path` (`run_path`)");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE projects_analyses as A LEFT JOIN projects as P USING (project_id) SET A.run_path = P.run_path");		
		updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `projects` DROP COLUMN `run_path`");
		
		//set possible_values value field to TEXT to avoid any truncation
		toConsole("---[ set possible_values value field to TEXT to avoid any truncation ]---");
		for (Analysis analysis : getAvailableAnalyses()){
			updateAndPrint(Schema.HIGHLANDER, "ALTER TABLE `"+analysis.getTablePossibleValues()+"` MODIFY `value` TEXT NOT NULL");
		}

		//List of values are now linked to a database field, this code will try to guess it or set it to a dummy value
		toConsole("---[ List of values are now linked to a database field, try to guess it or set it to a dummy value ]---");
		Map<String, String> fields = new HashMap<String, String>();
		Map<String, String> values = new HashMap<String, String>();
		Set<String> samples = new HashSet<>();
		Set<String> genes = new HashSet<>();
		try(Results res = DB.select(Schema.HIGHLANDER, "SELECT `key`, min(`value`) as `value` FROM users_data WHERE `type` = 'VALUES' GROUP BY `key`")){
			while (res.next()) {
				values.put(res.getString(1), res.getString(2));
			}
		}
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT DISTINCT(sample) FROM projects")) {				
			while (res.next()){
				if (res.getString(1) != null){
					samples.add(res.getString(1).toUpperCase());
				}
			}
		}
		try (Results res = DB.select(new Reference("GRCh37"), Schema.ENSEMBL, "SELECT display_label FROM gene JOIN seq_region USING (seq_region_id) JOIN xref ON (display_xref_id = xref_id) WHERE `name` IN ('1','2','3','4','5','6','7','8','9','10','11','12','13','14','15','16','17','18','19','20','21','22','X','Y','MT')")) {				
			while (res.next()){
				if (res.getString(1) != null){
					genes.add(res.getString(1).toUpperCase());
				}
			}
		}
		Pattern pos = Pattern.compile("^[0-9]+$");
		for (String key : values.keySet()) {
			String value = values.get(key);
			if (samples.contains(value.toUpperCase())) {
				fields.put(key, "sample");
			}else if (genes.contains(value.toUpperCase())) {
				fields.put(key, "gene_symbol");
			}else if (value.startsWith("p.")) {
				fields.put(key, "hgvs_protein");
			}else if (value.startsWith("c.")) {
				fields.put(key, "hgvs_dna");
			}else if (pos.matcher(value).find()) {
				fields.put(key, "pos");
			}else {
				try(Results res = DB.select(Schema.HIGHLANDER, "SELECT `value` FROM users_data WHERE `type` = 'VALUES' AND `key` = '"+key+"'")){
					while (!fields.containsKey(key) && res.next()) {
						value = res.getString("value");
						if (samples.contains(value.toUpperCase())) {
							fields.put(key, "sample");
						}else if (genes.contains(value.toUpperCase())) {
							fields.put(key, "gene_symbol");
						}else if (value.startsWith("p.")) {
							fields.put(key, "hgvs_protein");
						}else if (value.startsWith("c.")) {
							fields.put(key, "hgvs_dna");
						}else if (pos.matcher(value).find()) {
							fields.put(key, "pos");
						}
					}
				}
				if (!fields.containsKey(key)) {
					fields.put(key, "unknown");
				}
			}
		}
		for (String key : fields.keySet()) {
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `analysis` = '"+fields.get(key)+"' WHERE `key` = '"+key+"' AND `type` = 'VALUES'");
		}

		//List of intervals are now linked to a reference, set it to GRCh37 by default for all existing
		toConsole("---[ List of intervals are now linked to a reference, set it to GRCh37 by default for all existing ]---");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `analysis` = 'GRCh37' WHERE `type` = 'INTERVALS'");

		//Table users_queries and linked functionnalities are removed (was generating problems and performance issues for low added value)
		toConsole("---[ Table users_queries and linked functionnalities are removed ]---");
		updateAndPrint(Schema.HIGHLANDER, "DROP TABLE IF EXISTS `users_queries`");

		//Restructuration of user annotations
		toConsole("---[ Restructuration of user annotations ]---");
		for (Analysis analysis : getAvailableAnalyses()){
			for (String query : SqlGenerator.split(SqlGenerator.createUserAnnotationsVariants(analysis))) {
				updateAndPrint(Schema.HIGHLANDER, query);
			}
			updateAndPrint(Schema.HIGHLANDER, "INSERT IGNORE INTO `"+analysis+"_user_annotations_variants` "
					+ "(`chr`, `pos`, `length`, `reference`, `alternative`, `gene_symbol`, `username`, `variant_of_interest`, `variant_comments`) "
					+ "SELECT `chr`, `pos`, if((A.variant_type='SNP' OR A.variant_type='MNP'),length(reference), abs(length(reference)-length(alternative))) as length, `reference`, `alternative`, "
					+ "IF(`gene_symbol` IS NULL, '', `gene_symbol`) as `gene_symbol`, `username`, `of_interest_variant`, `private_comments_variant` "
					+ "FROM `"+analysis+"_private_annotations` as P JOIN `"+analysis+"` as A ON P.variant_id = A.id");
			updateAndPrint(Schema.HIGHLANDER, "INSERT IGNORE INTO `"+analysis+"_user_annotations_variants` "
					+ "(`chr`, `pos`, `length`, `reference`, `alternative`, `gene_symbol`, `username`, `variant_of_interest`, `variant_comments`) "
					+ "SELECT A.`chr`, A.`pos`, if(A.variant_type='SNP',1, abs(length(A.reference)-length(A.alternative))) as length, A.`reference`, A.`alternative`, "
					+ "IF(`gene_symbol` IS NULL, '', `gene_symbol`) as `gene_symbol`, 'PUBLIC' as `username`, NULL as `of_interest_variant`, `public_comments_variant` "
					+ "FROM `"+analysis+"_public_annotations` as P JOIN `"+analysis+"` as A ON P.variant_id = A.id "
					+ "WHERE LENGTH(public_comments_variant) > 0");
			for (String query : SqlGenerator.split(SqlGenerator.createUserAnnotationsGenes(analysis))) {
				updateAndPrint(Schema.HIGHLANDER, query);
			}
			updateAndPrint(Schema.HIGHLANDER, "INSERT IGNORE INTO `"+analysis+"_user_annotations_genes` "
					+ "(`gene_symbol`, `username`, `gene_of_interest`, `gene_comments`) "
					+ "SELECT `gene_symbol`, IF(`username` IS NULL, 'PUBLIC', `username`) as `username`, `of_interest_gene`, `comments_gene` "
					+ "FROM `"+analysis+"_gene_annotations`");
			for (String query : SqlGenerator.split(SqlGenerator.createUserAnnotationsSamples(analysis))) {
				updateAndPrint(Schema.HIGHLANDER, query);
			}
			for (String query : SqlGenerator.split(SqlGenerator.createUserAnnotationsEvaluations(analysis))) {
				updateAndPrint(Schema.HIGHLANDER, query);
			}
			updateAndPrint(Schema.HIGHLANDER, "INSERT IGNORE INTO `"+analysis+"_user_annotations_evaluations` "
					+ "(`chr`, `pos`, `length`, `reference`, `alternative`, `gene_symbol`, `project_id`, "
					+ "`evaluation`, `evaluation_username`, "
					+ "`check_insilico`, `check_insilico_username`, "
					+ "`check_validated_variant`, `check_validated_variant_username`, "
					+ "`check_somatic_variant`, `check_somatic_variant_username`, "
					+ "`check_segregation`, `check_segregation_username`, "
					+ "`evaluation_comments`, `evaluation_comments_username`, "
					+ "`history`) "
					+ "SELECT A.`chr`, A.`pos`, if(A.variant_type='SNP',1, abs(length(A.reference)-length(A.alternative))) as length, A.`reference`, A.`alternative`, "
					+ "IF(`gene_symbol` IS NULL, '', `gene_symbol`) as `gene_symbol`, P.`project_id`,"
					+ "`evaluation`,`evaluation_username`,"
					+ "`check_insilico`,`check_insilico_username`,"
					+ "`check_validated_change`,`check_validated_change_username`,"
					+ "`check_somatic_change`,`check_somatic_change_username`,"
					+ "`check_segregation`,`check_segregation_username`,"
					+ "`evaluation_comments`, 'Administrator' as `evaluation_comments_username`, "
					+ "`history` "
					+ "FROM "+analysis+"_public_annotations as P JOIN `"+analysis+"` as A ON P.variant_id = A.id");
			toConsole("Computing values for `"+analysis+"_user_annotations_num_evaluations`");
			for (String query : SqlGenerator.split(SqlGenerator.createUserAnnotationsNumEvaluations(analysis))) {
				updateAndPrint(Schema.HIGHLANDER, query);
			}
			DBUtils.rebuildUserAnnotationsNumEvaluations(analysis);
		}

		//Update field categories
		toConsole("---[ Update field categories ]---");
		for (String query : SqlGenerator.split(SqlGenerator.createFieldCategories())) {
			updateAndPrint(Schema.HIGHLANDER, query);
		}
		for (String query : SqlGenerator.split(SqlGenerator.fillFieldCategories())) {
			updateAndPrint(Schema.HIGHLANDER, query);
		}
		//Update fields
		toConsole("---[ Update fields ]---");
		for (String query : SqlGenerator.split(SqlGenerator.createFieldTags())) {
			updateAndPrint(Schema.HIGHLANDER, query);
		}
		for (String query : SqlGenerator.split(SqlGenerator.createFields())) {
			updateAndPrint(Schema.HIGHLANDER, query);
		}
		for (String query : SqlGenerator.split(SqlGenerator.fillFields())) {
			updateAndPrint(Schema.HIGHLANDER, query);
		}
		for (String query : SqlGenerator.split(SqlGenerator.createFieldAnalyses())) {
			updateAndPrint(Schema.HIGHLANDER, query);
		}

		//Fields and categories should now exists
		Category.fetchAvailableCategories(DB);
		Field.fetchAvailableFields(DB);

		for (Analysis analysis : getAvailableAnalyses()){
			try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT `field`, `table`, `source` FROM `fields`")){
				while (res.next()) {
					String field = res.getString("field");
					String table = res.getString("table");
					String source = res.getString("source");
					boolean insert = false;
					if (table.equals("_custom_annotations")) {
						if (field.equals("variant_custom_id")) {
							insert = true;
						}else if (field.equals("insert_date_custom")) {
							insert = true;
						}else{
							switch(source) {
							case "Mutect 2":
								if (getVariantCaller(analysis) == VariantCaller.MUTECT) {
									insert = true;
								}
								break;
							case "LifeScope 2.5":
								if (getVariantCaller(analysis) == VariantCaller.LIFESCOPE) {
									insert = true;
								}
								break;
							case "Highlander":
								if (getVariantCaller(analysis) == VariantCaller.TORRENT) {
									insert = true;
								}
								break;
							case "Alamut batch 1.4.2":
							case "GeVaCT":
								//Do nothing, only VUB uses Alamut and GeVaCT
							default:
								break;
							}
						}
					}else{
						insert = true;
					}
					if (insert) Highlander.getDB().insert(Schema.HIGHLANDER, "INSERT INTO `fields_analyses` VALUES ('"+field+"','"+analysis+"')");
				}
			}
		}

		//Update users_data to take into account most of the structural modifications
		toConsole("---[ Update users_data to take into account most of the structural modifications ]---");
		updateAndPrint(Schema.HIGHLANDER, "DELETE FROM `users_data` WHERE INSTR(`key`,'POSITION|') > 0 OR INSTR(`key`,'VISIBLE|') > 0 OR INSTR(`key`,'WIDTH|') > 0");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = 'variant_sample_id' WHERE `value` = 'id'");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'°id°','°variant_sample_id°') WHERE INSTR(`value`,'COLUMN_SELECTION') > 0 OR INSTR(`value`,'COLUMN_MASK') > 0");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'patient','sample')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `analysis` = REPLACE(`analysis`,'patient','sample')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'change_type','variant_type')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'!SNP!','!SNV!')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'!MNP!','!MNV!')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'MULTIPLE_NUCLEOTIDES_POLYMORPHISMS','MULTIPLE_NUCLEOTIDES_VARIANTS')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'change_num_germline_unfilt','germline_ac')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'change_num_germline_filt','germline_ac')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'change_num_somatic_unfilt','somatic_ac')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'change_num_somatic_filt','somatic_ac')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'change_num_germline_filt_num_groups','germline_pathologies')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'change_num_germline_unfilt_num_groups','germline_pathologies')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'change_num_somatic_filt_num_groups','somatic_pathologies')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'change_num_somatic_unfilt_num_groups','somatic_pathologies')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'insert_date','insert_date_sample')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'of_interest_gene','gene_of_interest')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'of_interest_variant','variant_of_interest')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'private_comments_variant','variant_comments_private')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'public_comments_variant','variant_comments_public')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'private_comments_gene','gene_comments_private')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'public_comments_gene','gene_comments_public')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'check_somatic_change','check_somatic_variant')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'check_somatic_change_username','check_somatic_variant_username')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'check_validated_change','check_validated_variant')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'check_validated_change_username','check_validated_variant_username')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'aggregation_score_lr','metalr_score')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'aggregation_pred_lr','metalr_pred')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'aggregation_score_radial_svm','metasvm_score')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'aggregation_pred_radial_svm','metasvm_pred')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'phastCons100way_vertebrate','phastcons_100way_vertebrate')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'phastCons46way_placental','phastcons_30way_mammalian')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'phastCons46way_primate','phastcons_17way_primate')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'phyloP100way_vertebrate','phylop_100way_vertebrate')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'phyloP46way_placental','phylop_30way_mammalian')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'phyloP46way_primate','phylop_17way_primate')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'pph2_hdiv_pred','polyphen_hdiv_pred')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'pph2_hdiv_score','polyphen_hdiv_score')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'pph2_hvar_pred','polyphen_hvar_pred')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'pph2_hvar_score','polyphen_hvar_score')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'reliability_index','meta_reliability_index')");

		String[] deletedFields = new String[] {
				"ARIC5606_AA_AF",
				"ARIC5606_EA_AF",
				"ESP6500_AA_AF",
				"ESP6500_EA_AF",
				"the1000G_AF",
				"consensus_MAF",
				"ARIC5606_AA_AC",
				"ARIC5606_EA_AC",
				"consensus_MAC",
				"the1000G_AC",
				"transcript_uniprot_acc",
				//Filters for which nothing is done, and won't work because of missing field -- users will have to change them by hand (and it's probably better, so they see those fields have disappeared)
				"clinvar_rs",
				"cosmic_count",
				"cosmic_id",
				"unisnp_ids",
				"lof_tolerant_or_recessive_gene",
				"exac_an",
				"detected_as_error",
				"change_num_germline_filt_details_groups",
				"change_num_germline_filt_groups",
				"change_num_germline_unfilt_details_groups",
				"change_num_germline_unfilt_groups",
				"change_num_somatic_filt_details_groups",
				"change_num_somatic_filt_groups",
				"change_num_somatic_unfilt_details_groups",
				"change_num_somatic_unfilt_groups",
				//found_in_xxxx
				//xxx_change_num_xxx
				//xxx_change_percent_xxx
				//gene_num_xxx
				//xxx_gene_num_xxx
				//xxx_gene_percent_xxx
				//id
		};
		updateAndPrint(Schema.HIGHLANDER, "DELETE FROM `users_data` WHERE (`key` = 'COLUMN_MASK' OR `key` = 'COLUMN_SELECTION') AND `value` IN ("+HighlanderDatabase.makeSqlList(Arrays.asList(deletedFields), String.class)+")");
		updateAndPrint(Schema.HIGHLANDER, "DELETE FROM `users_data` WHERE (`key` = 'COLUMN_MASK' OR `key` = 'COLUMN_SELECTION') AND INSTR(`value`,'found_in') > 0");
		updateAndPrint(Schema.HIGHLANDER, "DELETE FROM `users_data` WHERE (`key` = 'COLUMN_MASK' OR `key` = 'COLUMN_SELECTION') AND INSTR(`value`,'_change_num_') > 0");
		updateAndPrint(Schema.HIGHLANDER, "DELETE FROM `users_data` WHERE (`key` = 'COLUMN_MASK' OR `key` = 'COLUMN_SELECTION') AND INSTR(`value`,'_change_percent_') > 0");
		updateAndPrint(Schema.HIGHLANDER, "DELETE FROM `users_data` WHERE (`key` = 'COLUMN_MASK' OR `key` = 'COLUMN_SELECTION') AND INSTR(`value`,'gene_num_') > 0");
		updateAndPrint(Schema.HIGHLANDER, "DELETE FROM `users_data` WHERE (`key` = 'COLUMN_MASK' OR `key` = 'COLUMN_SELECTION') AND INSTR(`value`,'_gene_num_') > 0");
		updateAndPrint(Schema.HIGHLANDER, "DELETE FROM `users_data` WHERE (`key` = 'COLUMN_MASK' OR `key` = 'COLUMN_SELECTION') AND INSTR(`value`,'_gene_percent_') > 0");
		for (String field : deletedFields) {
			updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'°"+field+"°','°') WHERE INSTR(`value`,'COLUMN_SELECTION') > 0 OR INSTR(`value`,'COLUMN_MASK') > 0");
		}
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'ARIC5606_AA_AF','exac_af')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'ARIC5606_EA_AF','exac_af')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'ESP6500_AA_AF','exac_af')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'ESP6500_EA_AF','exac_af')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'the1000G_AF','exac_af')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'consensus_MAF','exac_af')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'ARIC5606_AA_AC','exac_ac')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'ARIC5606_EA_AC','exac_ac')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'consensus_MAC','exac_ac')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'the1000G_AC','exac_ac')");
		updateAndPrint(Schema.HIGHLANDER, "UPDATE `users_data` SET `value` = REPLACE(`value`,'transcript_uniprot_acc','transcript_uniprot_id')");
		
		//Set default column selection for each analysis
		toConsole("---[ Set default column selection for each analysis ]---");
		for (AnalysisFull analysis : AnalysisFull.getAvailableAnalyses()) {
			toConsole("Set default column selection for " + analysis);
			analysis.setDefaultColumns();
		}

		//New coverage structure creation
		toConsole("---[ New coverage structure creation ]---");
		for (Analysis analysis : getAvailableAnalyses()) {
			for (String query : SqlGenerator.split(SqlGenerator.createCoverageRegions(analysis))) {
				updateAndPrint(Schema.HIGHLANDER, query);
			}
			for (String query : SqlGenerator.split(SqlGenerator.createCoverage(analysis))) {
				updateAndPrint(Schema.HIGHLANDER, query);
			}
		}
		//add coding regions for all analyses
		toConsole("---[ add coding regions for all analyses ]---");
		Map<String,Boolean> biotypes = new TreeMap<>();
		biotypes.put("protein_coding",false);
		AnalysisFull firstAnalysis = AnalysisFull.getAvailableAnalyses().get(0);
		ExomeBed.generateBed(firstAnalysis.getReference(), false, biotypes, true, false, false, false, false, null, firstAnalysis);
		for (Analysis analysis : getAvailableAnalyses()) {
			if (!analysis.equals(firstAnalysis)) {
				updateAndPrint(Schema.HIGHLANDER, 
						"INSERT INTO " + analysis.getFromCoverageRegions()+" ("+Region.getInsertionColumnsString()+") SELECT "+Region.getInsertionColumnsString()+" FROM "+firstAnalysis.getFromCoverageRegions());
			}
		}

		//Allele frequency table creation
		toConsole("---[ Allele frequency table creation ]---");
		for (Analysis analysis : getAvailableAnalyses()){
			for (String query : SqlGenerator.split(SqlGenerator.createAlleleFrequencies(analysis)))
				updateAndPrint(Schema.HIGHLANDER, query);
			for (String query : SqlGenerator.split(SqlGenerator.createAlleleFrequenciesPerPathology(analysis)))
				updateAndPrint(Schema.HIGHLANDER, query);
		}

		//Fields and categories should now exists
		Category.fetchAvailableCategories(DB);
		Field.fetchAvailableFields(DB);
		
		//Create new main annotation tables
		toConsole("---[ Create new main annotation tables ]---");
		for (Analysis analysis : getAvailableAnalyses()){
			//_gene_annotations must be removed first: new table has exact same name (and no data must be kept)
			updateAndPrint(Schema.HIGHLANDER, "DROP TABLE IF EXISTS `"+analysis+"_gene_annotations`");
		}
		for (AnalysisFull analysis : AnalysisFull.getAvailableAnalyses()){
			for (String query : SqlGenerator.split(SqlGenerator.createStaticAnnotations(analysis))) {
				updateAndPrint(Schema.HIGHLANDER, query);
			}
			for (String query : SqlGenerator.split(SqlGenerator.createSampleAnnotations(analysis))) {
				updateAndPrint(Schema.HIGHLANDER, query);
			}
			for (String query : SqlGenerator.split(SqlGenerator.createCustomAnnotations(analysis))) {
				updateAndPrint(Schema.HIGHLANDER, query);
			}
			for (String query : SqlGenerator.split(SqlGenerator.createGeneAnnotations(analysis))) {
				updateAndPrint(Schema.HIGHLANDER, query);
			}		
			List<String> existingFields = Highlander.getDB().getAvailableColumns(analysis.getReference(), Schema.HIGHLANDER, analysis.getTableCustomAnnotations());
			List<Field> customFields = new ArrayList<>();
			for (Field field : Field.getAvailableFields(analysis, false)) {
				if (field.getTable(analysis).equals(analysis.getTableCustomAnnotations()) && !existingFields.contains(field.getName())) {
					customFields.add(field);
				}
			}
			for (String query : SqlGenerator.split(SqlGenerator.addColumnsToCustomAnnotations(analysis, customFields))) {
				if (query.length() > 0) {
					updateAndPrint(Schema.HIGHLANDER, query);
				}
			}
			
			//Transfer data from old analysis to split tables
			toConsole("---[ Transfer data from old analysis '"+analysis+"' to split tables ]---");
			DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
			final int AS = 100_000;
			long max=1;
			try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT COUNT(*) FROM "+analysis)) {
				if (res.next()){
					max = res.getLong(1);
				}
			}
			File insertFileSample = DbBuilder.createTempInsertionFile(analysis+"_sample_"+df2.format(System.currentTimeMillis())+".sql");
			File insertFileStatic = DbBuilder.createTempInsertionFile(analysis+"_static_"+df2.format(System.currentTimeMillis())+".sql");
			File insertFileGene = DbBuilder.createTempInsertionFile(analysis+"_gene_"+df2.format(System.currentTimeMillis())+".sql");
			File insertFileCustom = DbBuilder.createTempInsertionFile(analysis+"_custom_"+df2.format(System.currentTimeMillis())+".sql");
			try (FileWriter writerSample = new FileWriter(insertFileSample);
					FileWriter writerStatic = new FileWriter(insertFileStatic);
					FileWriter writerGene = new FileWriter(insertFileGene);
					FileWriter writerCustom = new FileWriter(insertFileCustom)){
				Set<String> uniqueVariants = new HashSet<>();			
				Set<String> uniqueGenes = new HashSet<>();			
				Set<Field> fieldsToInclude = new HashSet<>();
				for (Field field : Field.getAvailableFields(analysis, false)){
					if ((
							field.getTable(analysis).equals(analysis.getTableSampleAnnotations()) || 
							field.getTable(analysis).equals(analysis.getTableStaticAnnotations()) || 
							field.getTable(analysis).equals(analysis.getTableGeneAnnotations()) || 
							field.getTable(analysis).equals(analysis.getTableCustomAnnotations())  
							 ) && !(
									field.getName().equals("variant_sample_id") || 
									field.getName().equals("variant_static_id") || 
									field.getName().equals("variant_custom_id") || 
									field.getName().equals("gene_id") || 
									field.getName().equals("insert_date_sample") || 
									field.getName().equals("insert_date_static") || 
									field.getName().equals("insert_date_custom") ||
									field.getName().equals("insert_date_gene") ||
									field.getName().equals("variant_type") || 
									field.getName().equals("length") ||
									field.getAnnotationCode() == Annotation.COSMIC ||
									field.getAnnotationCode() == Annotation.DBNSFP
								)) {
						fieldsToInclude.add(field);
					}
				}
				fieldsToInclude.add(Field.chr);
				fieldsToInclude.add(Field.pos);
				fieldsToInclude.add(Field.reference);
				fieldsToInclude.add(Field.alternative);
				fieldsToInclude.add(Field.gene_symbol);
				fieldsToInclude.add(Field.project_id);
				toConsole("Fields that will be transfered:");
				toConsole(fieldsToInclude.toString());
				int count = 0;
				try(Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT * FROM " + analysis, true)){
					while (res.next()) {
						count++;
						if (count%AS == 0){
							toConsole(df2.format(System.currentTimeMillis()) + " - " + Tools.longToString(count) + " variants processed ("+Tools.doubleToPercent(((double)count/(double)max), 2)+") - " + Tools.doubleToString((Tools.getUsedMemoryInMb()), 0, false) + " Mb / "+ (Tools.doubleToString((Runtime.getRuntime().maxMemory() / 1024 /1024), 0, false)) + " Mb of RAM used");
						}
						AnnotatedVariant variant = new AnnotatedVariant(analysis);
						AnnotatedVariant.extractFromSqlResultSet(variant, res, fieldsToInclude);
						//toConsole("extractFromSqlResultSet variant : " + variant.toString());
						//toConsole("Field.getAvailableFields(analysis, false): " + Field.getAvailableFields(analysis, false));
						variant.setRefAlt();
						writerSample.write(variant.getInsertionString(Highlander.getDB().getDataSource(Schema.HIGHLANDER).getDBMS(), analysis.getTableSampleAnnotations()));											
						writerCustom.write(variant.getInsertionString(Highlander.getDB().getDataSource(Schema.HIGHLANDER).getDBMS(), analysis.getTableCustomAnnotations()));
						if (!uniqueVariants.contains(variant.toString())) {
							uniqueVariants.add(variant.toString());
							writerStatic.write(variant.getInsertionString(Highlander.getDB().getDataSource(Schema.HIGHLANDER).getDBMS(), analysis.getTableStaticAnnotations()));																	
						}
						if (variant.affectsGene() && !uniqueGenes.contains(variant.getValue(Field.gene_symbol))) {
							uniqueGenes.add(variant.getValue(Field.gene_symbol).toString());
							writerGene.write(variant.getInsertionString(Highlander.getDB().getDataSource(Schema.HIGHLANDER).getDBMS(), analysis.getTableGeneAnnotations()));																	
						}
					}
				}
				toConsole(df2.format(System.currentTimeMillis()) + " - All variants processed, importing sample_annotations  - " + Tools.doubleToString((Tools.getUsedMemoryInMb()), 0, false) + " Mb / "+ (Tools.doubleToString((Runtime.getRuntime().maxMemory() / 1024 /1024), 0, false)) + " Mb of RAM used");
				Highlander.getDB().insertFile(Schema.HIGHLANDER, analysis.getTableSampleAnnotations(), AnnotatedVariant.getInsertionColumnsString(analysis, analysis.getTableSampleAnnotations()), insertFileSample, false, Highlander.getParameters());
				toConsole(df2.format(System.currentTimeMillis()) + " - All variants processed, importing static_annotations  - " + Tools.doubleToString((Tools.getUsedMemoryInMb()), 0, false) + " Mb / "+ (Tools.doubleToString((Runtime.getRuntime().maxMemory() / 1024 /1024), 0, false)) + " Mb of RAM used");
				Highlander.getDB().insertFile(Schema.HIGHLANDER, analysis.getTableStaticAnnotations(), AnnotatedVariant.getInsertionColumnsString(analysis, analysis.getTableStaticAnnotations()), insertFileStatic, false, Highlander.getParameters());
				toConsole(df2.format(System.currentTimeMillis()) + " - All variants processed, importing gene_annotations  - " + Tools.doubleToString((Tools.getUsedMemoryInMb()), 0, false) + " Mb / "+ (Tools.doubleToString((Runtime.getRuntime().maxMemory() / 1024 /1024), 0, false)) + " Mb of RAM used");
				Highlander.getDB().insertFile(Schema.HIGHLANDER, analysis.getTableGeneAnnotations(), AnnotatedVariant.getInsertionColumnsString(analysis, analysis.getTableGeneAnnotations()), insertFileGene, false, Highlander.getParameters());
				toConsole(df2.format(System.currentTimeMillis()) + " - All variants processed, importing custom_annotations  - " + Tools.doubleToString((Tools.getUsedMemoryInMb()), 0, false) + " Mb / "+ (Tools.doubleToString((Runtime.getRuntime().maxMemory() / 1024 /1024), 0, false)) + " Mb of RAM used");
				Highlander.getDB().insertFile(Schema.HIGHLANDER, analysis.getTableCustomAnnotations(), AnnotatedVariant.getInsertionColumnsString(analysis, analysis.getTableCustomAnnotations()), insertFileCustom, true, Highlander.getParameters());
				insertFileSample.delete();
				insertFileStatic.delete();
				insertFileGene.delete();
				insertFileCustom.delete();
			}
		}
		
		//Set new DBNSFP and COSMIC annotations
		toConsole("---[ Set new DBNSFP and COSMIC annotations ]---");
		DbUpdater dbu = new DbUpdater();
		Set<Annotation> annotations = new HashSet<>();
		annotations.add(Annotation.CONSENSUS);
		annotations.add(Annotation.DBNSFP);
		annotations.add(Annotation.COSMIC);
		dbu.updateAnnotations(AnalysisFull.getAvailableAnalyses(), annotations, 4);
		
		//Allele frequency computation
		toConsole("---[ Allele frequency computation ]---");
		DbBuilder dbb = new DbBuilder();
		dbb.computeAlleleFrequencies(getAvailableAnalyses(), false);
		dbb.computeAlleleFrequencies(getAvailableAnalyses(), true);

		//Possible values
		toConsole("---[ Possible values computation ]---");
		dbb.computePossibleValues(AnalysisFull.getAvailableAnalyses());

		//Old tables are not used anymore 
		toConsole("---[ Delete old tables that are not used anymore ]---");
		for (Analysis analysis : getAvailableAnalyses()){
			updateAndPrint(Schema.HIGHLANDER, "DROP TABLE IF EXISTS `"+analysis+"`");
			updateAndPrint(Schema.HIGHLANDER, "DROP TABLE IF EXISTS `"+analysis+"_private_annotations`");
			updateAndPrint(Schema.HIGHLANDER, "DROP TABLE IF EXISTS `"+analysis+"_backup_private_annotations`");
			updateAndPrint(Schema.HIGHLANDER, "DROP TABLE IF EXISTS `"+analysis+"_public_annotations`");
			updateAndPrint(Schema.HIGHLANDER, "DROP TABLE IF EXISTS `"+analysis+"_backup_public_annotations`");
			updateAndPrint(Schema.HIGHLANDER, "DROP TABLE IF EXISTS `"+analysis+"_change_stats`");
			updateAndPrint(Schema.HIGHLANDER, "DROP TABLE IF EXISTS `"+analysis+"_gene_stats`");
			updateAndPrint(Schema.HIGHLANDER, "DROP TABLE IF EXISTS `"+analysis+"_general_stats`");
			updateAndPrint(Schema.HIGHLANDER, "DROP TABLE IF EXISTS `"+analysis+"_machine_specific_errors`");
			updateAndPrint(Schema.HIGHLANDER, "DROP TABLE IF EXISTS `"+analysis+"_gene_coverage`");
		}

	}

}
