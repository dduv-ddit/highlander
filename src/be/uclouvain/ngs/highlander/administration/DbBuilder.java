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

package be.uclouvain.ngs.highlander.administration;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Parameters;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.database.DBUtils;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.SqlGenerator;
import be.uclouvain.ngs.highlander.database.DBUtils.VariantKind;
import be.uclouvain.ngs.highlander.database.DBUtils.VariantNovelty;
import be.uclouvain.ngs.highlander.database.Field.Annotation;
import be.uclouvain.ngs.highlander.database.Field.StructuralVariantType;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.DBMS;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
import be.uclouvain.ngs.highlander.datatype.AnnotatedVariant;
import be.uclouvain.ngs.highlander.datatype.ExternalLink;
import be.uclouvain.ngs.highlander.datatype.Gene;
import be.uclouvain.ngs.highlander.datatype.Interval;
import be.uclouvain.ngs.highlander.datatype.Reference;
import be.uclouvain.ngs.highlander.datatype.Variant;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull.VariantCaller;
import be.uclouvain.ngs.highlander.datatype.SNPEffect.Zygosity;

public class DbBuilder {

	static final public String version = "17.15";

	public enum ToolArgument {
		input("I", "filename", "input filename"), 
		project("P", "project", "related project name in the file system"), 
		sample("S", "sample", "sample unique id or name (depends on tool)"), 
		analysis("A", "analysis", "Highlander analysis (e.g. exomes_hg38 or exomes_somatic_hg38). "
				+ "You can give several analyses using + (e.g. exomes_hg38+exomes_somatic_hg38) or all existing analyses using ALL.  "), 
		genome("G", "reference", "reference genome (must exist in Highlander references)"),
		runpath("R", "file", "path of the run"), 
		covWithDup("W", "file", "path of the coverage file WITH duplicates"), 
		covWithoutDup("w", "file", "path of the GATK coverage file WITHOUT duplicates"),
		mosdepthThreshold("M", "file", "path of a mosdepth threshold bed gzipped file (must be gzipped)"),
		mosdepthRegion("m", "file", "path of a mosdepth regions bed gzipped file (must be gzipped)"),
		target("T", "target", "target of coverage between raw, wodup, exons (default is exons)"),
		vcf("h", "file", "path of a VCF file"), 
		annotsv("a", "file", "path of an AnnotSV file"),
		alamut("j", "file", "path of an Alamut file"), 
		table("t", "table_name", "name of database table"), 
		repository("r", "directory", "path to the bam repository. Each analysis should have it's own subdirectory named exactly as the analysis (default is /data/highlander/bam/)"), 
		bool("b", "boolean", "set the tool value to 0 or 1"), 
		verbose("v", null, "tool shows all warnings"),
		config("c", "filename", "give config file to use as parameter"), 
		;
		private String shortArg;
		private String userInput;
		private String description;
		
		ToolArgument(String shortArg, String input, String description){
			this.shortArg = shortArg;
			this.userInput = input;
			this.description = description;
		}
		
		public String getShortArg() {
			return "-"+shortArg;
		}
		public String getLongArg() {
			return "--"+toString();
		}
		public boolean hasInput() {
			return userInput != null;
		}
		public String getInput() {
			return userInput;
		}
		public String getDescription() {
			return description;
		}
	}
	
	public enum Tool {
		softupdate("Set the Highlander database in 'soft update' mode (a warning is displayed in the client GUI)",
				new ToolArgument[] {ToolArgument.bool, },
				new String[]{
						"a boolean value",
				},
				new boolean[] {true, },
				new String[] {null, },
				true),
		variants("Import all variant from a VCF file in an analysis table.",
				new ToolArgument[] {ToolArgument.project, ToolArgument.sample, ToolArgument.analysis, ToolArgument.vcf, ToolArgument.alamut, ToolArgument.bool, ToolArgument.verbose, },
				new String[]{
						"the project name",
						"the sample name",
						"the Highlander analysis (e.g. exomes_hg38)",
						"the path of the VCF file",
						"an Alamut annotation file",
						"1 (delete existing variants, default) or 0 (keep existing variants) to manage variants already existing for this combination of project/sample/analysis",
						"show all warning for annotations (when SnpEff, Ensembl or dbNSFP don't find a transcript or position)",
				},
				new boolean[] {true, true, true, true, false, false, false, },
				new String[] {null, null, null, null, null, "1", "0", },
				true),
		annotsv("Import all structural variant from an AnnotSV tab-separated file in an analysis table.",
				new ToolArgument[] {ToolArgument.project, ToolArgument.sample, ToolArgument.analysis, ToolArgument.annotsv, ToolArgument.bool, ToolArgument.verbose, },
				new String[]{
						"the project name",
						"the sample name",
						"the Highlander analysis (e.g. exomes_hg38)",
						"the path of the AnnotSV file",
						"1 (delete existing variants, default) or 0 (keep existing variants) to manage variants already existing for this combination of project/sample/analysis",
						"show all warning for annotations (when SnpEff, Ensembl or dbNSFP don't find a transcript or position)",
		},
				new boolean[] {true, true, true, true, false, false, },
				new String[] {null, null, null, null, "1", "0", },
				true),
		coverage("Populate the [analysis]_coverage table in the Highlander schema.",
				new ToolArgument[] {ToolArgument.target, ToolArgument.mosdepthThreshold, ToolArgument.mosdepthRegion, ToolArgument.analysis, ToolArgument.project, ToolArgument.sample, },
				new String[]{
						"the target of coverage between raw, wodup, exons (default is exons)",
						"a mosdepth threshold bed file (must be gzipped)",
						"a mosdepth regions bed file (must be gzipped)",
						"the Highlander analysis (e.g. exomes_hg38)",
						"the project name",
						"the sample name",
				},
				new boolean[] {false, true, true, true, true, true, },
				new String[] {"exons", null, null, null, null, null, },
				true),
		possiblevalues("Populate [analysis]_possible_values table in the Highlander database.",
				new ToolArgument[] {ToolArgument.analysis, },
				new String[]{
						"the Highlander analysis (e.g. exomes_hg38)",
				},
				new boolean[] {true, },
				new String[] {null, },
				true),
		allelefreq("Update the [analysis]_allele_frequencies table(s) in the Highlander database.",
				new ToolArgument[] {ToolArgument.analysis, ToolArgument.bool, },
				new String[]{
						"the Highlander analysis (e.g. exomes_hg38)",
						"0 ([analysis]_allele_frequencies updated, default) or 1 ([analysis]_allele_frequencies_pathologies updated)",
				},
				new boolean[] {true, false, },
				new String[] {null, "0", },
				true),
		fastqc("Populate FastQC columns depending on one sample of the projects table in the Highlander schema.",
				new ToolArgument[] {ToolArgument.project, ToolArgument.sample, ToolArgument.input, },
				new String[]{
						"the project name",
						"the sample name",
						"the path of the fastQC zip file",
				},
				new boolean[] {true, true, true, },
				new String[] {null, null, null, },
				true),
		gatkcoverage("Populate coverage (target with and without duplicate, and exome) columns depending on one sample of the projects table in the Highlander schema.",
				new ToolArgument[] {ToolArgument.project, ToolArgument.sample, ToolArgument.covWithDup, ToolArgument.covWithoutDup, ToolArgument.input, },
				new String[]{
						"the project name",
						"the sample name",
						"the path of the GATK coverage summary file (WITH duplicates)",
						"the path of the GATK coverage summary file (WITHTOUT duplicates)",
						"the path of the GATK coverage summary file (exome)",
				},
				new boolean[] {true, true, true, true, true, },
				new String[] {null, null, null, null, null, },
				true),
		gatkcovwithdup("Populate coverage (target with duplicates) columns depending on one sample of the projects table in the Highlander schema.",
				new ToolArgument[] {ToolArgument.project, ToolArgument.sample, ToolArgument.input, },
				new String[]{
						"the project name",
						"the sample name",
						"the path of the GATK coverage summary file (WITH duplicates)",
				},
				new boolean[] {true, true, true, },
				new String[] {null, null, null, },
				true),
		gatkcovwithoutdup("Populate coverage (target without duplicates) columns depending on one sample of the projects table in the Highlander schema.",
				new ToolArgument[] {ToolArgument.project, ToolArgument.sample, ToolArgument.input, },
				new String[]{
						"the project name",
						"the sample name",
						"the path of the GATK coverage summary file (WITHTOUT duplicates)",
				},
				new boolean[] {true, true, true, },
				new String[] {null, null, null, },
				true),
		gatkcovexome("Populate coverage (exome without duplicates) columns depending on one sample of the projects table in the Highlander schema.",
				new ToolArgument[] {ToolArgument.project, ToolArgument.sample, ToolArgument.input, },
				new String[]{
						"the project name",
						"the sample name",
						"the path of the GATK coverage summary file (exome)",
				},
				new boolean[] {true, true, true, },
				new String[] {null, null, null, },
				true),
		gatktitv("Populate GATK Ti/Tv ratio columns depending on one sample of the projects table in the Highlander schema.",
				new ToolArgument[] {ToolArgument.project, ToolArgument.sample, ToolArgument.analysis, ToolArgument.input, },
				new String[]{
						"the project name",
						"the sample name",
						"the Highlander analysis (e.g. exomes_hg38)",
						"the path of the GATK Ti/Tv ratio file",
				},
				new boolean[] {true, true, true, true, },
				new String[] {null, null, null, null, },
				true),
		lsbamstat("Populate BamStats columns depending on one sample of the projects table in the Highlander schema.",
				new ToolArgument[] {ToolArgument.project, ToolArgument.sample, ToolArgument.input, },
				new String[]{
						"the project name",
						"the sample name",
						"the path of the BAMstat file",
				},
				new boolean[] {true, true, true, },
				new String[] {null, null, null, },
				true),
		lscoverage("Populate coverage (with duplicates) columns depending on one sample of the projects table in the Highlander schema.",
				new ToolArgument[] {ToolArgument.project, ToolArgument.sample, ToolArgument.input, },
				new String[]{
						"the project name",
						"the sample name",
						"the path of the LifeScope coverage summary file",
				},
				new boolean[] {true, true, true, },
				new String[] {null, null, null, },
				true),
		lshethom("Populate LifeScope Heterozygous/Homozygous columns depending on one sample of the projects table in the Highlander schema.",
				new ToolArgument[] {ToolArgument.project, ToolArgument.sample, ToolArgument.input, },
				new String[]{
						"the project name",
						"the sample name",
						"the path of the LifeScope heterozygous/homozygous ratio file",
				},
				new boolean[] {true, true, true, },
				new String[] {null, null, null, },
				true),
		lstitv("Populate LifeScope Ti/Tv ratio columns depending on one sample of the projects table in the Highlander schema.",
				new ToolArgument[] {ToolArgument.project, ToolArgument.sample, ToolArgument.analysis, ToolArgument.input, },
				new String[]{
						"the project name",
						"the sample name",
						"the Highlander analysis (e.g. exomes_hg38)",
						"the path of the LifeScope Ti/Tv ratio file",
				},
				new boolean[] {true, true, true, true, },
				new String[] {null, null, null, null, },
				true),
		setprojectpath("Set the run_path field in the projects table",
				new ToolArgument[] {ToolArgument.project, ToolArgument.sample, ToolArgument.analysis, ToolArgument.runpath, },
				new String[]{
						"the project name",
						"the sample name",
						"the Highlander analysis (e.g. exomes_hg38)",
						"the path of the run",
				},
				new boolean[] {true, true, true, true, },
				new String[] {null, null, null, null, },
				true),
		gonl("Import a VCF file from GoNL project into a GoNL database for annotation in Highlander.",
				new ToolArgument[] {ToolArgument.vcf, ToolArgument.genome, ToolArgument.project, },
				new String[]{
						"the path of the VCF file",
						"the reference genome",
						"the chromosome covered by the VCF file (e.g. 1 - NOT chr1)",
				},
				new boolean[] {true, true, true, },
				new String[] {null, null, null, },
				true),
		exac("Import a VCF file from ExAC project into a ExAC database for annotation in Highlander.",
				new ToolArgument[] {ToolArgument.vcf, ToolArgument.genome, },
				new String[]{
						"the path of the VCF file",
						"the reference genome",
				},
				new boolean[] {true, true, },
				new String[] {null, null, },
				true),
		warnusers("Send an email to all users having samples recently (re)imported in the given analyses.",
				new ToolArgument[] {ToolArgument.analysis, },
				new String[]{
						"the Highlander analysis (e.g. exomes_hg38)",
				},
				new boolean[] {true, },
				new String[] {"ALL", },
				true),
		sqlgenerator("Generate SQL files for creating Highlander tables",
				new ToolArgument[] {},
				new String[]{
				},
				new boolean[] {},
				new String[] {},
				false),
		addextlinks("Create all default external links, if they do not exists already.",
				new ToolArgument[] {},
				new String[]{
				},
				new boolean[] {},
				new String[] {},
				true),
		hsqldbgenerator("Generate HSqlDB Highlander local database",
				new ToolArgument[] {ToolArgument.config, },
				new String[]{
						"a valid configuration file",
				},
				new boolean[] {true, },
				new String[] {null, },
				false),
		toTsv("Generate tab delimited file (tsv) from a given VCF, and add Highlander annotations",
				new ToolArgument[] {ToolArgument.vcf, ToolArgument.analysis, ToolArgument.project, ToolArgument.sample, ToolArgument.alamut, ToolArgument.verbose, },
				new String[]{
						"the path of the VCF file",
						"the analysis (e.g. exomes_hg38) to use for reference genome and annotation databases",
						"the project name (if not, the project will be 'unknown' and some fields will be empty)",
						"the sample name (if not, a tab delimited file will be created for each sample found in the VCF)",
						"an Alamut annotation file",
						"show all warning for annotations (when SnpEff, Ensembl or dbNSFP don't find a transcript or position)",
				},
				new boolean[] {true, true, false, false, false, false, },
				new String[] {null, null, "unknown", null, null, "0", },
				true),
		toXlsx("Generate Excel file from a given VCF, and add Highlander annotations",
				new ToolArgument[] {ToolArgument.vcf, ToolArgument.analysis, ToolArgument.project, ToolArgument.sample, ToolArgument.alamut, ToolArgument.verbose, },
				new String[]{
						"the path of the VCF file",
						"the analysis (e.g. exomes_hg38) to use for reference genome and annotation databases",
						"the project name (if not, the project will be 'unknown' and some fields will be empty)",
						"the sample name (if not, a tab delimited file will be created for each sample found in the VCF)",
						"an Alamut annotation file",
						"show all warning for annotations (when SnpEff, Ensembl or dbNSFP don't find a transcript or position)",
				},
				new boolean[] {true, true, false, false, false, false, },
				new String[] {null, null, "unknown", null, null, "0", },
				true),
		toJson("Generate JSON file from a given VCF, and add Highlander annotations",
				new ToolArgument[] {ToolArgument.vcf, ToolArgument.analysis, ToolArgument.project, ToolArgument.sample, ToolArgument.alamut, ToolArgument.verbose, },
				new String[]{
						"the path of the VCF file",
						"the analysis (e.g. exomes_hg38) to use for reference genome and annotation databases",
						"the project name (if not, the project will be 'unknown' and some fields will be empty)",
						"the sample name (if not, a tab delimited file will be created for each sample found in the VCF)",
						"an Alamut annotation file",
						"show all warning for annotations (when SnpEff, Ensembl or dbNSFP don't find a transcript or position)",
				},
				new boolean[] {true, true, false, false, false, false, },
				new String[] {null, null, "unknown", null, null, "0", },
				true),
		ioncov("Populate the given table in the Highlander schema (useful only for Ion Importer).",
				new ToolArgument[] {ToolArgument.input, ToolArgument.project, ToolArgument.sample, ToolArgument.table, ToolArgument.analysis, },
				new String[]{
						"text file (.sample_gene_summary) containing results of the GATK DepthOfCoverage tools",
						"the project name",
						"the sample name",
						"the name of database table",
						"the Highlander analysis (e.g. exomes_hg38)",
				},
				new boolean[] {true, true, true, true, true, },
				new String[] {null, null, null, null, null, },
				true),
		getsampleid("Retreive the sample unique id in the database of the given sample name in given project",
				new ToolArgument[] {ToolArgument.sample, ToolArgument.project,  },
				new String[]{
						"the sample name",
						"the project name",
				},
				new boolean[] {true, true, },
				new String[] {null, null, },
				true),
		getnormal("Retreive the normal sample linked to the given sample if any",
				new ToolArgument[] {ToolArgument.sample, ToolArgument.project, },
				new String[]{
						"the sample unique id or name",
						"the project name (not necessary with sample unique id, but preferable with sample name that can have duplicates)",
				},
				new boolean[] {true, false,  },
				new String[] {null, null, },
				true),
		getprojectfield("Retreive the given field value of the given sample from the projects table",
				new ToolArgument[] {ToolArgument.sample, ToolArgument.project, ToolArgument.input, },
				new String[]{
						"the sample unique id or name",
						"the project name (not necessary with sample unique id, but preferable with sample name that can have duplicates)",
						"the field to look for (e.g. run_label, pathology, sequencing_target, sample_type, platform, outsourcing, family, individual, sample, index_case, kit, ...",
				},
				new boolean[] {true, false, true, },
				new String[] {null, null, null, },
				true),
		cleanvcf("Eliminate variants from a VCF with ref or alt equals to '-', '.' or ' '",
				new ToolArgument[] {ToolArgument.input, },
				new String[]{
						"a vcf filename",
				},
				new boolean[] {true, },
				new String[] {null, },
				false),
		cleanchr("Eliminate variants from a VCF with a chromosome different of (chr)1-22,X,Y",
				new ToolArgument[] {ToolArgument.input, },
				new String[]{
						"a vcf filename",
				},
				new boolean[] {true, },
				new String[] {null, },
				false),
		import1000G("[beta] Import 1000g vcf in an Highlander analysis",
				new ToolArgument[] {ToolArgument.vcf, ToolArgument.sample, ToolArgument.analysis, ToolArgument.project, ToolArgument.input, ToolArgument.verbose, },
				new String[]{
						"the path of the VCF file",
						"the chromosome to import",
						"the Highlander analysis (e.g. genomes_1000g)",
						"the project name",
						"project id's range to import in the form X-Y (e.g. 500-1000)",
						"show all warning for annotations (when SnpEff, Ensembl or dbNSFP don't find a transcript or position)",
		},
				new boolean[] {true, true, true, true, true, false, },
				new String[] {null, null, null, null, null, null, "0", },
				true),
		;
		
		private String description;
		private ToolArgument[] arguments;
		private String[] argumentsDescription;
		private boolean[] argumentsMandatory;
		private String[] argumentsDefault;
		private boolean needsDatabase;
		Tool(String description, ToolArgument[] arguments, String[] argumentsDescription, boolean[] argumentsMandatory, String[] argumentsDefault, boolean needsDatabase){
			this.description = description;
			this.arguments = arguments;
			this.argumentsDescription = argumentsDescription;
			this.argumentsMandatory = argumentsMandatory;
			this.argumentsDefault = argumentsDefault;
			this.needsDatabase = needsDatabase;
		}
		
		public String getDescription() {
			return description;
		}
		
		private String getArgumentText(ToolArgument argument) {
			StringBuilder sb = new StringBuilder();
			for (int i=0 ; i < arguments.length ; i++) {
				if (arguments[i] == argument) {
					if (argumentsMandatory[i]) {
						sb.append("You must give ");
					}else {
						sb.append("You can give ");
					}
					sb.append(argumentsDescription[i]);
					sb.append(" using ");
					sb.append(arguments[i].getLongArg());
					sb.append(" or ");
					sb.append(arguments[i].getShortArg());
					sb.append(".");
				}
			}
			return sb.toString();
		}
		
		public String[] getArgumentsList(){
			String[] list = new String[arguments.length];
			for (int i=0 ; i < list.length ; i++) {
				list[i] = getArgumentText(arguments[i]);
			}
			return list;
		}
		
		public boolean isDatabaseNeeded() {
			return needsDatabase;
		}
		
		public boolean hasArgument(ToolArgument argument) {
			for (int i=0 ; i < arguments.length ; i++) {
				if (arguments[i] == argument) {
					return true;
				}
			}
			return false;
		}
		
		public boolean isMandatoryArgumentsGiven(ToolArgument argument, String input) {
			for (int i=0 ; i < arguments.length ; i++) {
				if (arguments[i] == argument) {
					if (argumentsMandatory[i]) {
						return input != null;
					}else {
						return true;
					}
				}
			}
			return true;
		}
		
		public Set<ToolArgument> getOptionalArguments() {
			Set<ToolArgument> set = new HashSet<>();
			for (int i=0 ; i < arguments.length ; i++) {
				if (!argumentsMandatory[i]) {
					set.add(arguments[i]);
				}
			}
			return set;
		}
		
		public String getDefaultValue(ToolArgument argument) {
			for (int i=0 ; i < arguments.length ; i++) {
				if (arguments[i] == argument) {
					return argumentsDefault[i];
				}
			}
			return null;
		}
	}
	
	public enum FastqcResult {pass, warn, fail}
	public enum FileType {json, tsv, xlsx}
	public enum CoverageTarget {raw, wodup, exons}

	public Parameters parameters;
	public HighlanderDatabase DB;

	public DbBuilder(String configFile) throws Exception {
		parameters = (configFile == null) ? new Parameters(false) : new Parameters(false, new File(configFile));
		Highlander.initialize(parameters,20);
		DB = Highlander.getDB();
	}

	public DbBuilder() throws Exception {
		if (Highlander.getParameters() != null) {
			parameters = Highlander.getParameters();
		}else {
			throw new Exception("Highlander parameters have not been initialize");
		}
		if (Highlander.getDB() != null) {
			DB = Highlander.getDB();
		}else {
			throw new Exception("Highlander database has not been initialize");
		}
	}

	/**
	 * dbSNP after removing those flagged SNPs :
	 * - SNPs flagged in dbSnp as "clinically associated" (CLN) --> not available since 137
	 * - SNPs flagged with CDA : Variation is interrogated in a clinical diagnostic assay
	 * - SNPs flagged with OM : Has OMIM/OMIA
	 * - SNPs flagged with LSD : Submitted from a locus-specific database
	 * - SNPs flagged with MUT : Is mutation (journal citation, explicit fact): a low frequency variation that is cited in journal and other reputable sources
	 *
	 * We could do also for (like Annovar) ...
	 * - SNPs < 1% minor allele frequency (MAF) (or unknown), --> sans les unknown on en a déjà +/- 5 000 000 (10%)
	 * - SNPs mapping only once to reference assembly, --> ????
	 * 
	 * 
	 * dbSNP 137 n’ayant plus le tag « CLN », j’utilise les tags CDA, LSD, OM et MUT en plus.
	 * Pour dbSNP 135 : 36 110 rs CLN, CDA+LSD+OM en rajoutent 39 917 et en retire 3 620. 
	 * MTP n’en retire quasi aucun de plus mais en rajoute 4 000 de moins, donc je ne l’ai pas pris. 
	 * Par contre, CDA LSD et OM sont tous les 3 nécessaires, et aucun ne retire un nombre significatif de ‘rajout’ s’il est omis.
	 * 
	 * Since dbNSFP 2.1, there is a 'UniSNP ids' field which is a cleaned version of dbSNP 129. 
	 * This field will be used instead of flagging last version of dbSNP.
	 * 
	 * @param filename
	 */
	/*
	public void buildDbSnp(String filename) throws Exception {
		File input = new File(filename);
		if (!input.exists() || !input.getPath().endsWith(".gz")) {
			throw new Exception("You must choose a valid gz dbsnp file");
		}
			try (FileInputStream fis = new FileInputStream(input)){
				try (InputStream in = new GZIPInputStream(fis)){
					try (InputStreamReader isr = new InputStreamReader(in)){
						try (BufferedReader br = new BufferedReader(isr)){
							String table = "flagged_snps";
							String line;
							int p=0;
							while ((line = br.readLine()) != null){
								if((++p)%5000000 == 0) System.out.println(p + " records treated ...");
								if (!line.startsWith("##")){
									String[] cols = line.split("\t");
									if (line.contains("CLN") || line.contains("CDA") || line.contains("OM") || line.contains("LSD") || line.contains("MUT")){
										for (String rs : cols[2].split(";")){
											try{
												int rsid = Integer.parseInt(rs.substring(2));
												DB.update(Schema.DBSNP,"INSERT INTO " + table + " SET rsid = " + rsid);
											}catch(NumberFormatException ex){
												System.err.println("WARNING -- " + cols[2] + " seems not to be a valid rs number ...");
											}
										}
									}
								}else if (line.startsWith("##dbSNP_BUILD_ID")){
									String version = line.split("=")[1];
									String schema = "dbSNP_" + version;
									DB.createNewDbsnpDriver(parameters, schema);
									DB.update(Schema.DBSNP,"DROP TABLE IF EXISTS " + table);
									DB.update(Schema.DBSNP, "CREATE TABLE `"+table+"` ("+
											"`rsid` int(11) NOT NULL COMMENT 'dbSNP rs ID', "+
											"PRIMARY KEY  (`rsid`), "+
											"UNIQUE KEY `rsid_UNIQUE` (`rsid`)"+
											") ENGINE=MyISAM DEFAULT CHARSET=latin1;");		
								}
							}
						}
					}
				}
			}
			System.out.println("All records treated.");
	}
	 */

	public void importProjectFastQC(String project, String sample, String fastqcZipFile) throws Exception {
		int idProject = DBUtils.getProjectId(project, sample);
		FastqcResult per_base_sequence_quality = null;
		FastqcResult per_tile_sequence_quality = null;
		FastqcResult per_sequence_quality_scores = null;
		FastqcResult per_base_sequence_content = null;
		FastqcResult per_sequence_GC_content = null;
		FastqcResult per_base_N_content = null;
		FastqcResult sequence_length_distribution = null;
		FastqcResult sequence_duplication_levels = null;
		double sequence_duplication_prop = 0;
		FastqcResult over_represented_sequences = null;
		FastqcResult adapter_content = null;
		File inputFastQC = new File(fastqcZipFile);
		if (inputFastQC.exists()){
			try (FileInputStream fis = new FileInputStream(inputFastQC.getCanonicalFile())){
				try (BufferedInputStream bis = new BufferedInputStream(fis)){
					try (ZipInputStream zis = new ZipInputStream(bis)){
						ZipEntry ze;
						while (null != (ze = zis.getNextEntry())) {
							if (ze.getName().endsWith("fastqc_data.txt")){
								try (InputStreamReader isr = new InputStreamReader(zis)){
									try (BufferedReader br = new BufferedReader(isr)){
										String line;
										while ((line = br.readLine()) != null){
											if (line.startsWith(">>Per base sequence quality")){
												per_base_sequence_quality = FastqcResult.valueOf(line.split("\t")[1]);
											}else if(line.startsWith(">>Per sequence quality scores")){
												per_sequence_quality_scores = FastqcResult.valueOf(line.split("\t")[1]);
											}else if(line.startsWith(">>Per base sequence content")){
												per_base_sequence_content = FastqcResult.valueOf(line.split("\t")[1]);
											}else if(line.startsWith(">>Per tile sequence quality")){
												per_tile_sequence_quality = FastqcResult.valueOf(line.split("\t")[1]);
											}else if(line.startsWith(">>Per sequence GC content")){
												per_sequence_GC_content = FastqcResult.valueOf(line.split("\t")[1]);
											}else if(line.startsWith(">>Per base N content")){
												per_base_N_content = FastqcResult.valueOf(line.split("\t")[1]);
											}else if(line.startsWith(">>Sequence Length Distribution")){
												sequence_length_distribution = FastqcResult.valueOf(line.split("\t")[1]);
											}else if(line.startsWith(">>Overrepresented sequences")){
												over_represented_sequences = FastqcResult.valueOf(line.split("\t")[1]);
											}else if(line.startsWith(">>Adapter Content")){
												adapter_content = FastqcResult.valueOf(line.split("\t")[1]);
											}else if (line.startsWith(">>Sequence Duplication Levels")){
												sequence_duplication_levels = FastqcResult.valueOf(line.split("\t")[1]);
											}else if (line.startsWith("#Total Deduplicated Percentage")){
												sequence_duplication_prop = Double.parseDouble(line.split("\t")[1]);
											}
										}
									}
								}
								break;
							}
						}
					}
				}				
			}
		}else{
			throw new Exception("Cannot import FastQC data : " + inputFastQC + " does not exist.");
		}
		try{
			setHardUpdate(true);
			DB.update(Schema.HIGHLANDER,"UPDATE `projects` SET" +
					((per_base_sequence_quality == null) ? "" : " `per_base_sequence_quality` = '"+per_base_sequence_quality+"',") +
					((per_sequence_quality_scores == null) ? "" : " `per_sequence_quality_scores` = '"+per_sequence_quality_scores+"',") +
					((per_base_sequence_content == null) ? "" : " `per_base_sequence_content` = '"+per_base_sequence_content+"',") +
					((per_tile_sequence_quality == null) ? "" : " `per_tile_sequence_quality` = '"+per_tile_sequence_quality+"',") +
					((per_sequence_GC_content == null) ? "" : " `per_sequence_GC_content` = '"+per_sequence_GC_content+"',") +
					((per_base_N_content == null) ? "" : " `per_base_N_content` = '"+per_base_N_content+"',") +
					((sequence_length_distribution == null) ? "" : " `sequence_length_distribution` = '"+sequence_length_distribution+"',") +
					((sequence_duplication_levels == null) ? "" : " `sequence_duplication_levels` = '"+sequence_duplication_levels+"',") +
					((over_represented_sequences == null) ? "" : " `over-represented_sequences` = '"+over_represented_sequences+"',") +
					((adapter_content == null) ? "" : " `adapter_content` = '"+adapter_content+"',") +
					" `sequence_duplication_prop` = '"+sequence_duplication_prop+"'" +
					" WHERE `project_id` = '"+idProject+"'");
		}finally{
			setHardUpdate(false);
		}
	}

	public void importProjectLifeScopeBamStat(String project, String sample, String bamstatFile) throws Exception {
		int idProject = DBUtils.getProjectId(project, sample);
		int reads_produced = 0;
		int reads_mapped = 0;
		double percent_total_mapped = 0;
		File inputBamStat = new File (bamstatFile);
		if (inputBamStat.exists()){
			try (FileReader fr = new FileReader(inputBamStat)){
				try (BufferedReader br = new BufferedReader(fr)){
					String line;
					while ((line = br.readLine()) != null){
						if (line.startsWith("NumFragmentsTotal")){
							String[] vals = line.split("\t");
							for (int i=2 ; i < vals.length ; i++){
								try{
									int val = Integer.parseInt(vals[i]);
									reads_produced += val;
								}catch(Exception ex){
									System.err.println("WARNING -- BAMstat NumFragmentsTotal : cannot convert " + vals[i] + " to integer.");
								}
							}
						}else if (line.startsWith("Tag1-NumMapped")){
							String[] vals = line.split("\t");
							for (int i=2 ; i < vals.length ; i++){
								try{
									int val = Integer.parseInt(vals[i]);
									reads_mapped += val;
								}catch(Exception ex){
									System.err.println("WARNING -- BAMstat Tag1-NumMapped : cannot convert " + vals[i] + " to integer.");
								}
							}					
						}
					}
				}
			}
			percent_total_mapped = (double)reads_mapped / (double)reads_produced * 100.0;

		}else{
			throw new Exception("Cannot import BAMStat data : " + inputBamStat + " does not exist.");
		}
		try {
			setHardUpdate(true);
			DB.update(Schema.HIGHLANDER,"UPDATE `projects` SET" +
					" `reads_produced` = '"+reads_produced+"'," +
					" `reads_mapped` = '"+reads_mapped+"'," +
					" `percent_total_mapped` = '"+percent_total_mapped+"'" +
					" WHERE `project_id` = '"+idProject+"'");
		}finally{
			setHardUpdate(false);
		}
	}

	public void importProjectLifeScopeCoverageWithDuplicates(String project, String sample, String lsCovSummaryFile) throws Exception {
		int idProject = DBUtils.getProjectId(project, sample);
		boolean pairend = false;
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT pair_end FROM projects WHERE project_id = " + idProject)) {
			if (res.next()){
				pairend = res.getBoolean("pair_end");
			}
		}
		int reads_on = 0;
		double percent_on = 0;
		int reads_off = 0;
		double percent_off = 0;
		int enrichment_fold = 0;
		int num_targets_not_covered = 0;
		int target_bases_not_covered = 0;
		double percent_of_target_bases_not_covered = 0;
		double percent_of_target_covered_meq_1X = 0;
		double percent_of_target_covered_meq_5X = 0;
		double percent_of_target_covered_meq_10X = 0;
		double percent_of_target_covered_meq_20X = 0;
		int average_depth_of_target_coverage = 0;
		File inputLsCov = new File (lsCovSummaryFile);
		if (inputLsCov.exists()){
			try (FileReader fr = new FileReader(inputLsCov)){
				try (BufferedReader br = new BufferedReader(fr)){
					String line;
					while ((line = br.readLine()) != null){
						if (line.startsWith("Run")){
							line = br.readLine();
							String[] data = line.split("\t");
							reads_on = Integer.parseInt(data[1]);
							percent_on = Double.parseDouble(data[2].replace('%', ' ').trim());
							reads_off = Integer.parseInt(data[3]);
							percent_off = Double.parseDouble(data[4].replace('%', ' ').trim());
							enrichment_fold = (int)Math.round(Double.parseDouble(data[5]));
						}else if (line.startsWith("#")){
							line = br.readLine();
							String[] data = line.split("\t");
							num_targets_not_covered = Integer.parseInt(data[0]);
							target_bases_not_covered = Integer.parseInt(data[1]);
							percent_of_target_bases_not_covered = Double.parseDouble(data[2].replace('%', ' ').trim());
							percent_of_target_covered_meq_1X = Double.parseDouble(data[3].replace('%', ' ').trim());
							percent_of_target_covered_meq_5X = Double.parseDouble(data[4].replace('%', ' ').trim());
							percent_of_target_covered_meq_10X = Double.parseDouble(data[5].replace('%', ' ').trim());
							percent_of_target_covered_meq_20X = Double.parseDouble(data[6].replace('%', ' ').trim());
							average_depth_of_target_coverage = (int)Math.round(Double.parseDouble(data[7]));
						}
					}
				}
			}
			if (pairend){
				reads_on /= 2;
				reads_off /= 2;
			}
		}else{
			throw new Exception("Cannot import LifeScope Coverage data : " + inputLsCov + " does not exist.");
		}
		try {
			setHardUpdate(true);
			DB.update(Schema.HIGHLANDER,"UPDATE `projects` SET" +
					" `reads_on` = '"+reads_on+"'," +
					" `percent_on` = '"+percent_on+"'," +
					" `reads_off` = '"+reads_off+"'," +
					" `percent_off` = '"+percent_off+"'," +
					" `enrichment_fold` = '"+enrichment_fold+"'," +
					" `num_targets_not_covered` = '"+num_targets_not_covered+"'," +
					" `target_bases_not_covered` = '"+target_bases_not_covered+"'," +
					" `percent_of_target_bases_not_covered` = '"+percent_of_target_bases_not_covered+"'," +
					" `percent_of_target_covered_meq_1X` = '"+percent_of_target_covered_meq_1X+"'," +
					" `percent_of_target_covered_meq_5X` = '"+percent_of_target_covered_meq_5X+"'," +
					" `percent_of_target_covered_meq_10X` = '"+percent_of_target_covered_meq_10X+"'," +
					" `percent_of_target_covered_meq_20X` = '"+percent_of_target_covered_meq_20X+"'," +
					" `average_depth_of_target_coverage` = '"+average_depth_of_target_coverage+"'" +
					" WHERE `project_id` = '"+idProject+"'");
		}finally{
			setHardUpdate(false);
		}
	}

	public void importProjectGatkCoverageWithDuplicates(String project, String sample, String gatkCovSummaryFileWithDup) throws Exception {
		int idProject = DBUtils.getProjectId(project, sample);
		int average_depth_of_target_coverage = 0;
		double percent_of_target_covered_meq_1X = 0.0;
		double percent_of_target_covered_meq_5X = 0.0;
		double percent_of_target_covered_meq_10X = 0.0;
		double percent_of_target_covered_meq_20X = 0.0;
		int indexdepth= -1;
		int index1x= -1;
		int index5x= -1;
		int index10x= -1;
		int index20x= -1;
		File inputCovSumWD = new File(gatkCovSummaryFileWithDup);
		if (inputCovSumWD.exists()){
			try (FileReader fr = new FileReader(inputCovSumWD)){
				try (BufferedReader br = new BufferedReader(fr)){
					String line = br.readLine();
					String[] headers = line.split("\t");
					for (int i=0 ; i<headers.length ; i++ ){
						if (headers[i].equals("mean")){
							indexdepth=i;
						}else if (headers[i].equals("%_bases_above_1")){
							index1x=i;
						}else if (headers[i].equals("%_bases_above_5")){
							index5x=i;
						}else if (headers[i].equals("%_bases_above_10")){
							index10x=i;
						}else if (headers[i].equals("%_bases_above_20")){
							index20x=i;
						}
					}
					line = br.readLine();
					String[] data = line.split("\t");
					if (indexdepth > -1) average_depth_of_target_coverage = (int)Math.round(Double.parseDouble(data[indexdepth]));
					if (index1x > -1) percent_of_target_covered_meq_1X = Double.parseDouble(data[index1x]);
					if (index5x > -1) percent_of_target_covered_meq_5X = Double.parseDouble(data[index5x]);
					if (index10x > -1) percent_of_target_covered_meq_10X = Double.parseDouble(data[index10x]);
					if (index20x > -1) percent_of_target_covered_meq_20X = Double.parseDouble(data[index20x]);
				}
			}
		}else{
			throw new Exception("Cannot import coverage summary data (with duplicates) : " + inputCovSumWD + " does not exist.");
		}
		try{
			setHardUpdate(true);
			DB.update(Schema.HIGHLANDER,"UPDATE `projects` SET" +
					" `percent_of_target_covered_meq_1X` = '"+percent_of_target_covered_meq_1X+"'," +
					" `percent_of_target_covered_meq_5X` = '"+percent_of_target_covered_meq_5X+"'," +
					" `percent_of_target_covered_meq_10X` = '"+percent_of_target_covered_meq_10X+"'," +
					" `percent_of_target_covered_meq_20X` = '"+percent_of_target_covered_meq_20X+"'," +
					" `average_depth_of_target_coverage` = '"+average_depth_of_target_coverage+"'" +
					" WHERE `project_id` = '"+idProject+"'");
		}finally{
			setHardUpdate(false);
		}
	}

	public void importProjectGatkCoverageWithoutDuplicates(String project, String sample, String gatkCovSummaryFileWithoutDup) throws Exception {
		int idProject = DBUtils.getProjectId(project, sample);
		int coverage_wo_dup = 0;
		double percent_of_target_covered_meq_1X_wo_dup = 0.0;
		double percent_of_target_covered_meq_5X_wo_dup = 0.0;
		double percent_of_target_covered_meq_10X_wo_dup = 0.0;
		double percent_of_target_covered_meq_20X_wo_dup = 0.0;
		double percent_of_target_covered_meq_30X_wo_dup = 0.0;
		int indexdepthwd= -1;
		int index1xwd= -1;
		int index5xwd= -1;
		int index10xwd= -1;
		int index20xwd= -1;
		int index30xwd= -1;
		File inputCovSum = new File(gatkCovSummaryFileWithoutDup);
		if (inputCovSum.exists()){
			try (FileReader fr = new FileReader(inputCovSum)){
				try (BufferedReader br = new BufferedReader(fr)){
					String line = br.readLine();
					String[] headers = line.split("\t");
					for (int i=0 ; i<headers.length ; i++ ){
						if (headers[i].equals("mean")){
							indexdepthwd=i;
						}else if (headers[i].equals("%_bases_above_1")){
							index1xwd=i;
						}else if (headers[i].equals("%_bases_above_5")){
							index5xwd=i;
						}else if (headers[i].equals("%_bases_above_10")){
							index10xwd=i;
						}else if (headers[i].equals("%_bases_above_20")){
							index20xwd=i;
						}else if (headers[i].equals("%_bases_above_30")){
							index30xwd=i;					}
					}
					line = br.readLine();
					String[] data = line.split("\t");
					if (indexdepthwd > -1) coverage_wo_dup = (int)Math.round(Double.parseDouble(data[indexdepthwd]));
					if (index1xwd > -1) percent_of_target_covered_meq_1X_wo_dup = Double.parseDouble(data[index1xwd]);
					if (index5xwd > -1) percent_of_target_covered_meq_5X_wo_dup = Double.parseDouble(data[index5xwd]);
					if (index10xwd > -1) percent_of_target_covered_meq_10X_wo_dup = Double.parseDouble(data[index10xwd]);
					if (index20xwd > -1) percent_of_target_covered_meq_20X_wo_dup = Double.parseDouble(data[index20xwd]);
					if (index30xwd > -1) percent_of_target_covered_meq_30X_wo_dup = Double.parseDouble(data[index30xwd]);
				}
			}
		}else{
			throw new Exception("Cannot import coverage summary data : " + inputCovSum + " does not exist.");
		}
		try{
			setHardUpdate(true);
			DB.update(Schema.HIGHLANDER,"UPDATE `projects` SET" +
					" `coverage_wo_dup` = '"+coverage_wo_dup+"'," +
					" `percent_of_target_covered_meq_1X_wo_dup` = '"+percent_of_target_covered_meq_1X_wo_dup+"'," +
					" `percent_of_target_covered_meq_5X_wo_dup` = '"+percent_of_target_covered_meq_5X_wo_dup+"'," +
					" `percent_of_target_covered_meq_10X_wo_dup` = '"+percent_of_target_covered_meq_10X_wo_dup+"'," +
					" `percent_of_target_covered_meq_20X_wo_dup` = '"+percent_of_target_covered_meq_20X_wo_dup+"'," +
					" `percent_of_target_covered_meq_30X_wo_dup` = '"+percent_of_target_covered_meq_30X_wo_dup+"'" +
					" WHERE `project_id` = '"+idProject+"'");
		}finally{
			setHardUpdate(false);
		}
	}

	public void importProjectGatkCoverageExome(String project, String sample, String gatkCovSummaryFileExome) throws Exception {
		int idProject = DBUtils.getProjectId(project, sample);
		int coverage_exome = 0;
		double percent_of_target_covered_meq_1X_exome = 0.0;
		double percent_of_target_covered_meq_5X_exome = 0.0;
		double percent_of_target_covered_meq_10X_exome = 0.0;
		double percent_of_target_covered_meq_20X_exome = 0.0;
		double percent_of_target_covered_meq_30X_exome = 0.0;
		int indexdepthex= -1;
		int index1xex= -1;
		int index5xex= -1;
		int index10xex= -1;
		int index20xex= -1;
		int index30xex= -1;
		File inputCovSumExome = new File(gatkCovSummaryFileExome);
		if (inputCovSumExome.exists()){
			try (FileReader fr = new FileReader(inputCovSumExome)){
				try (BufferedReader br = new BufferedReader(fr)){
					String line = br.readLine();
					String[] headers = line.split("\t");
					for (int i=0 ; i<headers.length ; i++ ){
						if (headers[i].equals("mean")){
							indexdepthex=i;
						}else if (headers[i].equals("%_bases_above_1")){
							index1xex=i;
						}else if (headers[i].equals("%_bases_above_5")){
							index5xex=i;
						}else if (headers[i].equals("%_bases_above_10")){
							index10xex=i;
						}else if (headers[i].equals("%_bases_above_20")){
							index20xex=i;
						}else if (headers[i].equals("%_bases_above_30")){
							index30xex=i;					}
					}
					line = br.readLine();
					String[] data = line.split("\t");
					if (indexdepthex > -1) coverage_exome = (int)Math.round(Double.parseDouble(data[indexdepthex]));
					if (index1xex > -1) percent_of_target_covered_meq_1X_exome = Double.parseDouble(data[index1xex]);
					if (index5xex > -1) percent_of_target_covered_meq_5X_exome = Double.parseDouble(data[index5xex]);
					if (index10xex > -1) percent_of_target_covered_meq_10X_exome = Double.parseDouble(data[index10xex]);
					if (index20xex > -1) percent_of_target_covered_meq_20X_exome = Double.parseDouble(data[index20xex]);
					if (index30xex > -1) percent_of_target_covered_meq_30X_exome = Double.parseDouble(data[index30xex]);
				}
			}
		}else{
			throw new Exception("Cannot import coverage summary data for exome : " + inputCovSumExome + " does not exist.");
		}
		try {
			setHardUpdate(true);
			DB.update(Schema.HIGHLANDER,"UPDATE `projects` SET" +
					" `coverage_exome_wo_dup` = '"+coverage_exome+"'," +
					" `percent_of_exome_covered_meq_1X_wo_dup` = '"+percent_of_target_covered_meq_1X_exome+"'," +
					" `percent_of_exome_covered_meq_5X_wo_dup` = '"+percent_of_target_covered_meq_5X_exome+"'," +
					" `percent_of_exome_covered_meq_10X_wo_dup` = '"+percent_of_target_covered_meq_10X_exome+"'," +
					" `percent_of_exome_covered_meq_20X_wo_dup` = '"+percent_of_target_covered_meq_20X_exome+"'," +
					" `percent_of_exome_covered_meq_30X_wo_dup` = '"+percent_of_target_covered_meq_30X_exome+"'" +
					" WHERE `project_id` = '"+idProject+"'");
		}finally{
			setHardUpdate(false);
		}
	}

	public void importProjectGatkCoverage(String project, String sample, String gatkCovSummaryFileWithDup, String gatkCovSummaryFileWithoutDup, String gatkCovSummaryFileExome) throws Exception {
		int idProject = DBUtils.getProjectId(project, sample);
		int average_depth_of_target_coverage = 0;
		double percent_of_target_covered_meq_1X = 0.0;
		double percent_of_target_covered_meq_5X = 0.0;
		double percent_of_target_covered_meq_10X = 0.0;
		double percent_of_target_covered_meq_20X = 0.0;
		int indexdepth= -1;
		int index1x= -1;
		int index5x= -1;
		int index10x= -1;
		int index20x= -1;
		File inputCovSumWD = new File(gatkCovSummaryFileWithDup);
		if (inputCovSumWD.exists()){
			try (FileReader fr = new FileReader(inputCovSumWD)){
				try (BufferedReader br = new BufferedReader(fr)){
					String line = br.readLine();
					String[] headers = line.split("\t");
					for (int i=0 ; i<headers.length ; i++ ){
						if (headers[i].equals("mean")){
							indexdepth=i;
						}else if (headers[i].equals("%_bases_above_1")){
							index1x=i;
						}else if (headers[i].equals("%_bases_above_5")){
							index5x=i;
						}else if (headers[i].equals("%_bases_above_10")){
							index10x=i;
						}else if (headers[i].equals("%_bases_above_20")){
							index20x=i;
						}
					}
					line = br.readLine();
					String[] data = line.split("\t");
					if (indexdepth > -1) average_depth_of_target_coverage = (int)Math.round(Double.parseDouble(data[indexdepth]));
					if (index1x > -1) percent_of_target_covered_meq_1X = Double.parseDouble(data[index1x]);
					if (index5x > -1) percent_of_target_covered_meq_5X = Double.parseDouble(data[index5x]);
					if (index10x > -1) percent_of_target_covered_meq_10X = Double.parseDouble(data[index10x]);
					if (index20x > -1) percent_of_target_covered_meq_20X = Double.parseDouble(data[index20x]);
				}
			}
		}else{
			throw new Exception("Cannot import coverage summary data (with duplicates) : " + inputCovSumWD + " does not exist.");
		}

		int coverage_wo_dup = 0;
		double percent_of_target_covered_meq_1X_wo_dup = 0.0;
		double percent_of_target_covered_meq_5X_wo_dup = 0.0;
		double percent_of_target_covered_meq_10X_wo_dup = 0.0;
		double percent_of_target_covered_meq_20X_wo_dup = 0.0;
		double percent_of_target_covered_meq_30X_wo_dup = 0.0;
		int indexdepthwd= -1;
		int index1xwd= -1;
		int index5xwd= -1;
		int index10xwd= -1;
		int index20xwd= -1;
		int index30xwd= -1;
		File inputCovSum = new File(gatkCovSummaryFileWithoutDup);
		if (inputCovSum.exists()){
			try (FileReader fr = new FileReader(inputCovSum)){
				try (BufferedReader br = new BufferedReader(fr)){
					String line = br.readLine();
					String[] headers = line.split("\t");
					for (int i=0 ; i<headers.length ; i++ ){
						if (headers[i].equals("mean")){
							indexdepthwd=i;
						}else if (headers[i].equals("%_bases_above_1")){
							index1xwd=i;
						}else if (headers[i].equals("%_bases_above_5")){
							index5xwd=i;
						}else if (headers[i].equals("%_bases_above_10")){
							index10xwd=i;
						}else if (headers[i].equals("%_bases_above_20")){
							index20xwd=i;
						}else if (headers[i].equals("%_bases_above_30")){
							index30xwd=i;					}
					}
					line = br.readLine();
					String[] data = line.split("\t");
					if (indexdepthwd > -1) coverage_wo_dup = (int)Math.round(Double.parseDouble(data[indexdepthwd]));
					if (index1xwd > -1) percent_of_target_covered_meq_1X_wo_dup = Double.parseDouble(data[index1xwd]);
					if (index5xwd > -1) percent_of_target_covered_meq_5X_wo_dup = Double.parseDouble(data[index5xwd]);
					if (index10xwd > -1) percent_of_target_covered_meq_10X_wo_dup = Double.parseDouble(data[index10xwd]);
					if (index20xwd > -1) percent_of_target_covered_meq_20X_wo_dup = Double.parseDouble(data[index20xwd]);
					if (index30xwd > -1) percent_of_target_covered_meq_30X_wo_dup = Double.parseDouble(data[index30xwd]);
				}
			}
		}else{
			throw new Exception("Cannot import coverage summary data : " + inputCovSum + " does not exist.");
		}

		int coverage_exome = 0;
		double percent_of_target_covered_meq_1X_exome = 0.0;
		double percent_of_target_covered_meq_5X_exome = 0.0;
		double percent_of_target_covered_meq_10X_exome = 0.0;
		double percent_of_target_covered_meq_20X_exome = 0.0;
		double percent_of_target_covered_meq_30X_exome = 0.0;
		int indexdepthex= -1;
		int index1xex= -1;
		int index5xex= -1;
		int index10xex= -1;
		int index20xex= -1;
		int index30xex= -1;
		File inputCovSumExome = new File(gatkCovSummaryFileExome);
		if (inputCovSumExome.exists()){
			try (FileReader fr = new FileReader(inputCovSumExome)){
				try (BufferedReader br = new BufferedReader(fr)){
					String line = br.readLine();
					String[] headers = line.split("\t");
					for (int i=0 ; i<headers.length ; i++ ){
						if (headers[i].equals("mean")){
							indexdepthex=i;
						}else if (headers[i].equals("%_bases_above_1")){
							index1xex=i;
						}else if (headers[i].equals("%_bases_above_5")){
							index5xex=i;
						}else if (headers[i].equals("%_bases_above_10")){
							index10xex=i;
						}else if (headers[i].equals("%_bases_above_20")){
							index20xex=i;
						}else if (headers[i].equals("%_bases_above_30")){
							index30xex=i;					}
					}
					line = br.readLine();
					String[] data = line.split("\t");
					if (indexdepthex > -1) coverage_exome = (int)Math.round(Double.parseDouble(data[indexdepthex]));
					if (index1xex > -1) percent_of_target_covered_meq_1X_exome = Double.parseDouble(data[index1xex]);
					if (index5xex > -1) percent_of_target_covered_meq_5X_exome = Double.parseDouble(data[index5xex]);
					if (index10xex > -1) percent_of_target_covered_meq_10X_exome = Double.parseDouble(data[index10xex]);
					if (index20xex > -1) percent_of_target_covered_meq_20X_exome = Double.parseDouble(data[index20xex]);
					if (index30xex > -1) percent_of_target_covered_meq_30X_exome = Double.parseDouble(data[index30xex]);
				}
			}
		}else{
			throw new Exception("Cannot import coverage summary data for exome : " + inputCovSumExome + " does not exist.");
		}

		double percent_duplicates_picard = -1;
		if (average_depth_of_target_coverage > 0 && coverage_wo_dup > 0){
			percent_duplicates_picard = (double)(average_depth_of_target_coverage - coverage_wo_dup) / (double)average_depth_of_target_coverage * 100.0;
		}
		try {
			setHardUpdate(true);
			DB.update(Schema.HIGHLANDER,"UPDATE `projects` SET" +
					" `percent_of_target_covered_meq_1X` = '"+percent_of_target_covered_meq_1X+"'," +
					" `percent_of_target_covered_meq_5X` = '"+percent_of_target_covered_meq_5X+"'," +
					" `percent_of_target_covered_meq_10X` = '"+percent_of_target_covered_meq_10X+"'," +
					" `percent_of_target_covered_meq_20X` = '"+percent_of_target_covered_meq_20X+"'," +
					" `average_depth_of_target_coverage` = '"+average_depth_of_target_coverage+"'," +
					" `coverage_wo_dup` = '"+coverage_wo_dup+"'," +
					" `percent_of_target_covered_meq_1X_wo_dup` = '"+percent_of_target_covered_meq_1X_wo_dup+"'," +
					" `percent_of_target_covered_meq_5X_wo_dup` = '"+percent_of_target_covered_meq_5X_wo_dup+"'," +
					" `percent_of_target_covered_meq_10X_wo_dup` = '"+percent_of_target_covered_meq_10X_wo_dup+"'," +
					" `percent_of_target_covered_meq_20X_wo_dup` = '"+percent_of_target_covered_meq_20X_wo_dup+"'," +
					" `percent_of_target_covered_meq_30X_wo_dup` = '"+percent_of_target_covered_meq_30X_wo_dup+"'," +
					" `coverage_exome_wo_dup` = '"+coverage_exome+"'," +
					" `percent_of_exome_covered_meq_1X_wo_dup` = '"+percent_of_target_covered_meq_1X_exome+"'," +
					" `percent_of_exome_covered_meq_5X_wo_dup` = '"+percent_of_target_covered_meq_5X_exome+"'," +
					" `percent_of_exome_covered_meq_10X_wo_dup` = '"+percent_of_target_covered_meq_10X_exome+"'," +
					" `percent_of_exome_covered_meq_20X_wo_dup` = '"+percent_of_target_covered_meq_20X_exome+"'," +
					" `percent_of_exome_covered_meq_30X_wo_dup` = '"+percent_of_target_covered_meq_30X_exome+"'" +
					((percent_duplicates_picard > -1)?", `percent_duplicates_picard` = '"+percent_duplicates_picard+"'":"") +
					" WHERE `project_id` = '"+idProject+"'");
		}finally{
			setHardUpdate(false);
		}
	}

	public void importProjectLifeScopeHetHom(String project, String sample, String lsHetHomRatioFile) throws Exception {
		int idProject = DBUtils.getProjectId(project, sample);
		double het_hom_ratio_ls = 0;
		File hethomFile = new File(lsHetHomRatioFile);
		if (hethomFile.exists()){
			int het = 0;
			int hom = 0;
			try (FileReader fr = new FileReader(hethomFile)){
				try (BufferedReader br = new BufferedReader(fr)){
					String line;
					while ((line = br.readLine()) != null){
						if (line.startsWith("Heterozygous")){
							het = Integer.parseInt(line.split(",")[1]);
						}else if (line.startsWith("Homozygous")){
							hom = Integer.parseInt(line.split(",")[1]);
						}
					}
				}
			}
			het_hom_ratio_ls = (double)het / (double)hom;
		}else{
			throw new Exception("Cannot import heterozygous/homzygous ratio : " + hethomFile + " does not exist.");
		}
		try {
			setHardUpdate(true);
			DB.update(Schema.HIGHLANDER,"UPDATE `projects` SET" +
					" `het_hom_ratio_ls` = '"+het_hom_ratio_ls+"'" +
					" WHERE `project_id` = '"+idProject+"'");
		}finally{
			setHardUpdate(false);
		}
	}

	public void importProjectLifeScopeTiTv(String project, String sample, List<? extends Analysis> analyses, String lsTiTvRatioFile) throws Exception {
		int project_id = DBUtils.getProjectId(project, sample);
		for (Analysis analysis : analyses){
			double ti_tv_ratio_ls = 0;
			File titvLsFile = new File(lsTiTvRatioFile);
			if (titvLsFile.exists()){
				double ti = 1;
				double tv = 1;
				try (FileReader fr = new FileReader(titvLsFile)){
					try (BufferedReader br = new BufferedReader(fr)){
						String line;
						while ((line = br.readLine()) != null){
							if (line.startsWith("Transition,")){
								ti = Double.parseDouble(line.split(",")[1]);
							}else if (line.startsWith("Transversion,")){
								tv = Double.parseDouble(line.split(",")[1]);
							}
						}
					}
				}
				ti_tv_ratio_ls = (double)ti / (double)tv;
			}else{
				throw new Exception("Cannot import LifeScope Ti/Tv ratio : " + titvLsFile + " does not exist.");
			}
			try {
				setHardUpdate(true);
				DB.insert(Schema.HIGHLANDER, "INSERT IGNORE INTO `projects_analyses` SET `project_id` = "+project_id+", `analysis` = '"+analysis+"'");
				DB.update(Schema.HIGHLANDER, "UPDATE projects SET users_warned = FALSE WHERE `project_id` = "+project_id);				
				DB.update(Schema.HIGHLANDER,"UPDATE `projects_analyses` SET" +
						" `ti_tv_ratio_all` = '"+ti_tv_ratio_ls+"'" +
						" WHERE `project_id` = '"+project_id+"' AND analysis = '"+analysis+"'");
			}finally{
				setHardUpdate(false);
			}
		}
	}

	public void importProjectGatkTiTv(String project, String sample, List<? extends Analysis> analyses, String gatkTiTvRatioFile) throws Exception {
		int project_id = DBUtils.getProjectId(project, sample);
		for (Analysis analysis : analyses){
			double ti_tv_ratio_all_gatk = 0;
			double ti_tv_ratio_known_gatk = 0;
			double ti_tv_ratio_novel_gatk = 0;
			File titvGatkFile = new File(gatkTiTvRatioFile);
			if (titvGatkFile.exists()){
				try (FileReader fr = new FileReader(titvGatkFile)){
					try (BufferedReader br = new BufferedReader(fr)){
						int novelty=-1;
						int tiTvRatio=-1;
						String line;
						while ((line = br.readLine()) != null){
							if (line.startsWith("TiTvVariantEvaluator")){
								String[] cols = (line.contains("\t")) ? line.split("\t") : line.split(" +");
								if (tiTvRatio < 0){
									for (int i=0 ; i < cols.length ; i++){
										if (cols[i].equalsIgnoreCase("Novelty")) novelty = i;
										else if (cols[i].equalsIgnoreCase("tiTvRatio")) tiTvRatio = i;
									}
								}else{
									if (cols[novelty].equalsIgnoreCase("all")){
										ti_tv_ratio_all_gatk = Double.parseDouble(cols[tiTvRatio]);
									}else if (cols[novelty].equalsIgnoreCase("known")){
										ti_tv_ratio_known_gatk = Double.parseDouble(cols[tiTvRatio]);
									}else if (cols[novelty].equalsIgnoreCase("novel")){
										ti_tv_ratio_novel_gatk = Double.parseDouble(cols[tiTvRatio]);
									}
								}
							}
						}
					}
				}
			}else{
				throw new Exception("Cannot import GATK Ti/Tv ratio : " + titvGatkFile + " does not exist.");
			}
			try {
				setHardUpdate(true);
				DB.insert(Schema.HIGHLANDER, "INSERT IGNORE INTO `projects_analyses` SET `project_id` = "+project_id+", `analysis` = '"+analysis+"'");
				DB.update(Schema.HIGHLANDER, "UPDATE projects SET users_warned = FALSE WHERE `project_id` = "+project_id);
				DB.update(Schema.HIGHLANDER,"UPDATE `projects_analyses` SET" +
						" `ti_tv_ratio_all` = '"+ti_tv_ratio_all_gatk+"'," +
						" `ti_tv_ratio_known` = '"+ti_tv_ratio_known_gatk+"'," +
						" `ti_tv_ratio_novel` = '"+ti_tv_ratio_novel_gatk+"'" +
						" WHERE `project_id` = '"+project_id+"' AND analysis = '"+analysis+"'");
			}finally{
				setHardUpdate(false);
			}
		}
	}

	public void buildCoverage(CoverageTarget target, String thresholdFile, String regionsFile, List<AnalysisFull> analyses, String project, String sample) throws Exception {
		File thresholds = new File(thresholdFile);
		if (!thresholds.exists() || !thresholds.getPath().endsWith(".gz")) {
			throw new Exception("You must choose a valid gz mosdepth threshold file");
		}
		File regions = new File(regionsFile);
		if (!regions.exists() || !regions.getPath().endsWith(".gz")) {
			throw new Exception("You must choose a valid gz mosdepth regions file");
		}		
		int project_id = DBUtils.getProjectId(project, sample);
		for (AnalysisFull analysis : analyses){
			System.out.println("Fetching regions");
			Map<Interval,Set<Integer>> regionIds = new HashMap<>();
			Map<Integer,Object[]> coverage = new HashMap<>();
			List<String> listExonsCoverage = new ArrayList<String>();
			if (target == CoverageTarget.exons) {
				try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `region_id`, `chr`, `start`, `end` FROM " + analysis.getFromCoverageRegions())){
					while (res.next()) {
						Interval interval = new Interval(analysis.getReference(), res.getString("chr"), res.getInt("start"), res.getInt("end"));
						if (!regionIds.containsKey(interval)) {
							regionIds.put(interval, new HashSet<>());
						}
						regionIds.get(interval).add(res.getInt("region_id"));
					}
				}
				try (Results res = DB.select(Schema.HIGHLANDER, "SHOW COLUMNS FROM " + analysis.getFromCoverage())) {
					while(res.next()){
						String col = DB.getDescribeColumnName(Schema.HIGHLANDER, res);
						if (col.startsWith("num_pos_")){
							listExonsCoverage.add(col);
						}
					}
				}
			}
			Map<String, String> listSummaryCoverage = new LinkedHashMap<>();
			try (Results res = DB.select(Schema.HIGHLANDER, "SHOW COLUMNS FROM projects")) {
				while(res.next()){
					String col = DB.getDescribeColumnName(Schema.HIGHLANDER, res);
					if (target == CoverageTarget.raw && col.startsWith("percent_of_target_covered_meq_")){
						String x = col.split("_")[col.split("_").length-1];
						listSummaryCoverage.put(x, col);
					}else if (target == CoverageTarget.wodup && col.startsWith("percent_of_target_covered_meq_") && col.endsWith("_wo_dup")){
						String x = col.split("_")[col.split("_").length-3];
						listSummaryCoverage.put(x, col);
					}else if (target == CoverageTarget.exons && col.startsWith("percent_of_exome_covered_meq_") && col.endsWith("_wo_dup")){
						String x = col.split("_")[col.split("_").length-3];
						listSummaryCoverage.put(x, col);
					}
				}
			}
			String[] neededCov = listExonsCoverage.toArray(new String[0]);
			String[] neededSummary = listSummaryCoverage.keySet().toArray(new String[0]);
			long[] summaryCov = new long[listSummaryCoverage.size()];
			try (FileInputStream fis = new FileInputStream(thresholds)){
				try (InputStream in = new GZIPInputStream(fis)){
					try (InputStreamReader isr = new InputStreamReader(in)){
						try (BufferedReader br = new BufferedReader(isr)){
							String line = br.readLine();
							String[] headers = line.split("\t");
							int iChr = -1;
							int iStart = -1;
							int iEnd = -1;
							int[] iCoverage = new int[neededCov.length];
							int[] iSummary = new int[neededSummary.length];
							for (int i=0 ; i < iCoverage.length ; i++) iCoverage[i] = -1;
							for (int i=0 ; i < iSummary.length ; i++) iSummary[i] = -1;
							for (int i=0 ; i < headers.length ; i++){
								if (headers[i].equalsIgnoreCase("#chrom")){
									iChr = i;
								}else if (headers[i].equalsIgnoreCase("start")){
									iStart = i;										
								}else if (headers[i].equalsIgnoreCase("end")){
									iEnd = i;										
								}else{
									for (int j=0 ; j<neededCov.length ; j++){
										if (headers[i].equalsIgnoreCase(neededCov[j].split("_")[neededCov[j].split("_").length-1])){
											iCoverage[j] = i;
										}
									}
									for (int j=0 ; j<neededSummary.length ; j++){
										if (headers[i].equalsIgnoreCase(neededSummary[j])){
											iSummary[j] = i;
										}
									}
								}
							}
							int p=0;
							while ((line = br.readLine()) != null){
								if((++p)%2000 == 0) System.out.println(p + " records treated from thresholds ...");
								if (line.length() > 0){
									String[] cols = line.split("\t");
									String chr = cols[iChr];
									try{
										if (target == CoverageTarget.exons) {
											int start = Integer.parseInt(cols[iStart]);
											int end = Integer.parseInt(cols[iEnd]);
											int[] cov = new int[iCoverage.length];
											for (int i=0 ; i<cov.length ; i++){
												cov[i] = (iCoverage[i] > -1) ? Integer.parseInt(cols[iCoverage[i]]) : -1;
											}
											Interval interval = new Interval(analysis.getReference(), chr, start, end);
											if (regionIds.containsKey(interval)) {
												for (int regionId : regionIds.get(interval)) {
													Object[] values = new Object[3+neededCov.length];
													values[0] = regionId;
													values[1] = project_id;
													values[2] = null;
													for (int i=0 ; i < neededCov.length ; i++) {
														values[i+3] = (cov[i] > -1) ? cov[i] : null;
													}
													coverage.put(regionId, values);
												}
											}else {
												System.err.println("WARNING -- Interval " + interval + " not found in " + analysis.getFromCoverageRegions() + ", you have to add it there first.");
											}
										}
										for (int i=0 ; i < neededSummary.length ; i++) {
											if (iSummary[i] > -1) {
												summaryCov[i] += Integer.parseInt(cols[iSummary[i]]);
											}
										}
									}catch(NumberFormatException ex){
										System.err.println("WARNING -- Line skipped because an integer cannot be parsed: " + line);
									}
								}
							}
						}
					}
				}
			}
			double summaryDepth = 0.0;
			long summarySize = 0;
			try (FileInputStream fis = new FileInputStream(regions)){
				try (InputStream in = new GZIPInputStream(fis)){
					try (InputStreamReader isr = new InputStreamReader(in)){
						try (BufferedReader br = new BufferedReader(isr)){
							String line;
							int iChr = 0;
							int iStart = 1;
							int iEnd = 2;
							int iDepth = 4; //depends if the bed file has named regions
							int p=0;
							while ((line = br.readLine()) != null){
								if((++p)%2000 == 0) System.out.println(p + " records treated from regions ...");
								if (line.length() > 0){
									String[] cols = line.split("\t");
									iDepth = cols.length-1; //depends if the bed file has named regions
									String chr = cols[iChr];
									try{
										int start = Integer.parseInt(cols[iStart]);
										int end = Integer.parseInt(cols[iEnd]);
										double depth = Double.parseDouble(cols[iDepth]);
										if (target == CoverageTarget.exons) {
											Interval interval = new Interval(analysis.getReference(), chr, start, end);
											if (regionIds.containsKey(interval)) {
												for (int regionId : regionIds.get(interval)) {
													if (coverage.containsKey(regionId)) {
														coverage.get(regionId)[2] = (int)Math.round(depth);
													}else {
														System.err.println("WARNING -- Interval " + interval + " in regions file was not found in thresholds file, skipping it.");
													}
												}
											}else {
												System.err.println("WARNING -- Interval " + interval + " not found in " + analysis.getFromCoverageRegions() + ", you have to add it there first.");
											}
										}
										summaryDepth += depth * (double)(end-start);
										summarySize += end-start;
									}catch(NumberFormatException ex){
										System.err.println("WARNING -- Line skipped because an integer cannot be parsed: " + line);
									}
								}
							}
						}
					}
				}
			}
			summaryDepth = summaryDepth / (double)summarySize;
			String query = "UPDATE projects SET ";
			switch(target) {
			case raw:
				query += "average_depth_of_target_coverage = ";
				break;
			case wodup:
				query += "coverage_wo_dup = ";
				break;
			case exons:
				query += "coverage_exome_wo_dup = ";
				break;
			}
			query += "'" + summaryDepth + "', ";
			for (int i=0 ; i < neededSummary.length ; i++) {
				query += "`" + listSummaryCoverage.get(neededSummary[i]) + "`";
				query += " = ";
				query += "'" + (double)((double)summaryCov[i] / (double)summarySize * 100.0) + "'";
				if (i < neededSummary.length-1) query += ", ";
			}
			query += " WHERE `project_id` = "+project_id;
			DB.update(Schema.HIGHLANDER, query);
			if (target == CoverageTarget.wodup) {
				double raw = -1;
				try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `average_depth_of_target_coverage` FROM `projects` WHERE `project_id` = "+project_id)) {
					if(res.next()){
						raw = res.getDouble(1);
					}
				}
				if (raw != -1) {
					if (raw > 0 && summaryDepth > 0){
						double percent_duplicates_picard = (double)(raw - summaryDepth) / (double)raw * 100.0;
						if (percent_duplicates_picard < 0) percent_duplicates_picard = 0;
						try {
							setHardUpdate(true);
							DB.update(Schema.HIGHLANDER,"UPDATE `projects` SET `percent_duplicates_picard` = '"+percent_duplicates_picard+"' WHERE `project_id` = "+project_id);
						}finally{
							setHardUpdate(false);
						}
					}						
				}
			}
			if (target == CoverageTarget.exons) {
				String nullStr = HighlanderDatabase.getNullString(DB.getDataSource(Schema.HIGHLANDER).getDBMS());
				DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
				File insertFile = createTempInsertionFile(sample+"_"+analysis+"_coverage_"+df2.format(System.currentTimeMillis())+".sql");
				try (FileWriter fw = new FileWriter(insertFile)){
					for (Object[] values : coverage.values()) {
						for (int i=0 ; i < values.length ; i++) {
							if (values[i] != null) fw.write(values[i].toString());
							else fw.write(nullStr);
							if (i < values.length-1) fw.write("\t");
							else fw.write("\n");
						}
					}
				}
				String columns = "`region_id`, `project_id`, `mean_depth`, ";
				for (int i=0 ; i < neededCov.length ; i++) {
					columns += "`"+neededCov[i]+"`";
					if (i < neededCov.length-1) columns += ", ";
				}
				try{
					setHardUpdate(true);
					DB.update(Schema.HIGHLANDER, "DELETE FROM " + analysis.getFromCoverage() + "WHERE project_id = " + project_id);
					Highlander.getDB().insertFile(Schema.HIGHLANDER, analysis.getTableCoverage(), columns, insertFile, true, Highlander.getParameters());
					insertFile.delete();
				}finally{
					setHardUpdate(false);
				}
			}
		}
		System.out.println("All records treated.");
	}

	public void buildIonCoverage(String filename, String project, String sample, String table, AnalysisFull analysis) throws Exception {
		File input = new File(filename);
		if (!input.exists()) {
			throw new Exception("You must choose a valid text file");
		}
		try (FileReader fr = new FileReader(input)){
			try (BufferedReader br = new BufferedReader(fr)){
				List<String> list = new ArrayList<String>();
				try (Results res = DB.select(Schema.HIGHLANDER, "SHOW COLUMNS FROM "+table)) {
					while(res.next()){
						String col = DB.getDescribeColumnName(Schema.HIGHLANDER, res);
						if (col.startsWith("coverage_above_")){
							list.add(col);
						}
					}
				}
				String[] neededCov = list.toArray(new String[0]);
				int idProject = DBUtils.getProjectId(project, sample);
				DB.update(Schema.HIGHLANDER, "DELETE FROM "+table+" WHERE project_id = " + idProject);
				String line = br.readLine();
				String[] headers = line.split("\t");
				int iAmplicon = -1;
				int iTotal = -1;
				int iDepth = -1;
				int[] iCoverage = new int[neededCov.length];
				for (int i=0 ; i < iCoverage.length ; i++) iCoverage[i] = -1;
				for (int i=0 ; i < headers.length ; i++){
					if (headers[i].equalsIgnoreCase("Target")){
						iAmplicon = i;
					}else if (headers[i].equalsIgnoreCase(sample+"_total_cvg") || headers[i].equalsIgnoreCase(sample.replace("-", "_")+"_total_cvg")){
						iTotal = i;
					}else if (headers[i].equalsIgnoreCase(sample+"_mean_cvg") || headers[i].equalsIgnoreCase(sample.replace("-", "_")+"_mean_cvg")){
						iDepth = i;
					}else if (headers[i].startsWith(sample+"_%_above_") || headers[i].startsWith(sample.replace("-", "_")+"_%_above_")){
						String cov = headers[i].split("_")[headers[i].split("_").length-1];
						for (int j=0 ; j<neededCov.length ; j++){
							if ((cov+"x").equals(neededCov[j].split("_")[neededCov[j].split("_").length-1])){
								iCoverage[j] = i;
							}
						}
					}
				}
				//if sample name was not used in the bam, take the first one
				if (iDepth == -1){
					String usedSample = "";
					for (int i=0 ; i < headers.length ; i++){
						if (headers[i].endsWith("_total_cvg")){
							iTotal = i;
							usedSample = headers[i].substring(0,headers[i].indexOf("_total_cvg"));
							break;
						}
					}
					for (int i=0 ; i < headers.length ; i++){
						if (headers[i].endsWith("_mean_cvg")){
							iDepth = i;
							usedSample = headers[i].substring(0,headers[i].indexOf("_mean_cvg"));
							break;
						}
					}
					System.out.println("WARNING : sample " + sample + " was not found in coverage file header, " + usedSample + " will be used instead.");
					for (int i=0 ; i < headers.length ; i++){
						if (headers[i].startsWith(usedSample+"_%_above_") || headers[i].startsWith(usedSample.replace("-", "_")+"_%_above_")){
							String cov = headers[i].split("_")[headers[i].split("_").length-1];
							for (int j=0 ; j<neededCov.length ; j++){
								if ((cov+"x").equals(neededCov[j].split("_")[neededCov[j].split("_").length-1])){
									iCoverage[j] = i;
								}
							}
						}
					}
				}

				if (iAmplicon == -1) throw new Exception("Cannot find a 'Target' column in input file");
				int p=0;
				while ((line = br.readLine()) != null){
					if((++p)%2000 == 0) System.out.println(p + " records treated ...");
					if (line.length() > 0){
						String[] cols = line.split("\t");
						String amplicon = cols[iAmplicon];
						try{
							double total = (iTotal > -1) ? Double.parseDouble(cols[iTotal]) : -1;
							double depth = (iDepth > -1) ? Double.parseDouble(cols[iDepth]) : -1;
							double[] coverage = new double[iCoverage.length];
							for (int i=0 ; i<coverage.length ; i++){
								coverage[i] = (iCoverage[i] > -1) ? Double.parseDouble(cols[iCoverage[i]])/100.0 : -1;
							}
							try{
								Interval interval = new Interval(analysis.getReference(), amplicon);
								Variant variant = new Variant(interval.getChromosome(), interval.getStart(), interval.getSize(), "", "", StructuralVariantType.CNV);
								Set<Gene> genes = DBUtils.getGenesWithCanonicalTranscriptIntersect(analysis.getReference(), variant);
								for (Gene gene : genes) {
									String query = "INSERT INTO "+table+" SET" +
											" `project_id` = '"+idProject+"'" +
											", `reference` = '"+analysis.getReference()+"'" +
											", `panel_code` = '"+sample.split("\\.")[1]+"'" +
											", `amplicon` = '"+amplicon+"'" +
											", `gene_symbol` = '"+gene.getGeneSymbol()+"'" +
											((iTotal != -1 && !Double.isNaN(total)) ? (", `total_depth` = '"+total+"'") : ", `total_depth` = '0'") +
											((iDepth != -1 && !Double.isNaN(depth)) ? (", `mean_depth` = '"+depth+"'") : ", `mean_depth` = '0'");
									for (int i=0 ; i<neededCov.length ; i++){
										query += ((iCoverage[i] != -1 && !Double.isNaN(coverage[i])) ? (", `"+neededCov[i]+"` = '"+coverage[i]+"'") : ", `"+neededCov[i]+"` = '0'");
									}
									DB.update(Schema.HIGHLANDER,query);
								}
							}catch(Exception ex){
								System.err.println("WARNING -- Cannot insert a line in the database : " + line);
							}
						}catch(NumberFormatException nex){
							System.err.print("WARNING -- Cannot read number in line : " + line);
						}
					}
				}
			}
		}
		System.out.println("All records treated.");
	}

	public void importSample(String project, String sample, String vcf, List<AnalysisFull> analyses, String alamutFile, boolean overwrite, boolean verbose) throws Exception {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd - HH_mm_ss");
		DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
		int project_id = DBUtils.getProjectId(project, sample);
		for (AnalysisFull analysis : analyses){
			Map<Integer,String> otherProjects = new HashMap<Integer,String>();
			try (Results res = DB.select(Schema.HIGHLANDER,	
					"SELECT project_id, run_label FROM `projects` JOIN `projects_analyses` USING (project_id) "
							+ "WHERE `sample` = '" + sample + "' AND `analysis` = '"+analysis+"'")){
				while (res.next()){
					otherProjects.put(res.getInt("project_id"), res.getString("run_label"));
				}
			}
			if (otherProjects.isEmpty() || overwrite){
				try{
					setHardUpdate(true);
					for (int other_project_id : otherProjects.keySet()){
						//Delete the variants from this sample of this project
						System.out.println("-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+");
						System.out.println("WARNING -- The sample " + sample + " already exists in the "+analysis+" variants database, under the project " + otherProjects.get(other_project_id));
						System.out.println("Those variants will be DELETED before importing those of project " + project);
						System.out.println("User annotations are not deleted, but will only be visible again if you reimported an existing sample with same run label");
						System.out.println("If it's a resequencing of an existing sample (i.e. another run), user annotations are lost");
						System.out.println("-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+");
						DB.update(Schema.HIGHLANDER, "DELETE FROM "+analysis.getFromSampleAnnotations()+"WHERE `project_id` = "+other_project_id);
						DB.update(Schema.HIGHLANDER, "DELETE FROM "+analysis.getFromCustomAnnotations()+"WHERE `project_id` = "+other_project_id);
						DB.update(Schema.HIGHLANDER, "DELETE FROM projects_analyses WHERE project_id = " + other_project_id + " AND analysis = '"+analysis+"'");
					}
				}finally {
					setHardUpdate(false);
				}
				
				AlamutParser alamut = null;
				if (alamutFile != null){
					alamut = new AlamutParser(new File(alamutFile));
				}

				System.out.println(sample + " - " + df.format(System.currentTimeMillis()) + " - Parsing and annotating VCF file");
				int counter = 0;
				File input = new File(vcf);
				String run_path = input.getParentFile().getCanonicalPath();

				Set<String> genes = new HashSet<>();
				File insertFileSample = createTempInsertionFile(project+"_"+analysis+"_"+sample+"_sample_"+df2.format(System.currentTimeMillis())+".sql");
				File insertFileStatic = createTempInsertionFile(project+"_"+analysis+"_"+sample+"_static_"+df2.format(System.currentTimeMillis())+".sql");
				File insertFileGene = createTempInsertionFile(project+"_"+analysis+"_"+sample+"_gene_"+df2.format(System.currentTimeMillis())+".sql");
				File insertFileCustom = createTempInsertionFile(project+"_"+analysis+"_"+sample+"_custom_"+df2.format(System.currentTimeMillis())+".sql");
				File notImportedFile = new File(run_path + "/" + sample + ".not_imported_in_highlander.vcf");
				try (FileWriter writerSample = new FileWriter(insertFileSample);
						FileWriter writerStatic = new FileWriter(insertFileStatic);
						FileWriter writerGene = new FileWriter(insertFileGene);
						FileWriter writerCustom = new FileWriter(insertFileCustom);
						FileWriter writerNotImported = new FileWriter(notImportedFile)){
					try (FileReader fr = new FileReader(input)){
						try (BufferedReader br = new BufferedReader(fr)){
							String line;
							int lineCount = 0;
							String[] header = null;
							while ((line = br.readLine()) != null){
								lineCount++;
								if (line.startsWith("#") && !line.startsWith("##")){
									header = line.split("\t");
								}
								if (!line.startsWith("#")){
									try{
										if (header == null) throw new Exception("VCF header columns were not found, need a line starting with ONE # followed by all headers");
										for (AnnotatedVariant annotatedVariant : new AnnotatedVariant(analysis).setAllAnnotations(header, line.split("\t"), project_id, alamut, !verbose)){
											if (annotatedVariant.exist()){
												counter++;
												writerSample.write(annotatedVariant.getInsertionString(DB.getDataSource(Schema.HIGHLANDER).getDBMS(), analysis.getTableSampleAnnotations()));											
												writerStatic.write(annotatedVariant.getInsertionString(DB.getDataSource(Schema.HIGHLANDER).getDBMS(), analysis.getTableStaticAnnotations()));											
												if (annotatedVariant.affectsGene() && !genes.contains(annotatedVariant.getValue(Field.gene_symbol).toString())) {
													genes.add(annotatedVariant.getValue(Field.gene_symbol).toString());
													writerGene.write(annotatedVariant.getInsertionString(DB.getDataSource(Schema.HIGHLANDER).getDBMS(), analysis.getTableGeneAnnotations()));											
												}
												writerCustom.write(annotatedVariant.getInsertionString(DB.getDataSource(Schema.HIGHLANDER).getDBMS(), analysis.getTableCustomAnnotations()));											
												if (counter % 5000 == 0) System.out.println(sample + " - " + df.format(System.currentTimeMillis()) + " - " + counter + " variants annotated ..."); 												
											}else {
												writerNotImported.write(line + "\n");
											}
										}
									}catch (Exception ex){
										writerNotImported.write(line + "\n");
										System.err.println("WARNING -- Problem with line " + lineCount + " of " + vcf);
										System.err.println(line);
										Tools.exception(ex);
									}
								}
							}
						}
					}
				}

				System.out.println("------------------ seconds taken per annotation category -----------------------------");
				for (Annotation a : Annotation.values()) {
					System.out.println(a + "\t" + Tools.longToString(AnnotatedVariant.time.get(a) / 1000));
				}
				System.out.println("--------------------------------------------------------------------------------------");

				System.out.println(sample + " - " + df.format(System.currentTimeMillis()) + " - Importing variants in the database");
				try {
					setHardUpdate(true);
					DB.insertFile(Schema.HIGHLANDER, analysis.getTableSampleAnnotations(), AnnotatedVariant.getInsertionColumnsString(analysis, analysis.getTableSampleAnnotations()), insertFileSample, true, parameters);
					DB.insertFile(Schema.HIGHLANDER, analysis.getTableStaticAnnotations(), AnnotatedVariant.getInsertionColumnsString(analysis, analysis.getTableStaticAnnotations()), insertFileStatic, true, parameters);
					DB.insertFile(Schema.HIGHLANDER, analysis.getTableGeneAnnotations(), AnnotatedVariant.getInsertionColumnsString(analysis, analysis.getTableGeneAnnotations()), insertFileGene, true, parameters);
					DB.insertFile(Schema.HIGHLANDER, analysis.getTableCustomAnnotations(), AnnotatedVariant.getInsertionColumnsString(analysis, analysis.getTableCustomAnnotations()), insertFileCustom, true, parameters);
				}finally {
					setHardUpdate(false);
				}
				insertFileSample.delete();
				insertFileStatic.delete();
				insertFileGene.delete();
				insertFileCustom.delete();

				DB.insert(Schema.HIGHLANDER, "INSERT IGNORE INTO `projects_analyses` SET `project_id` = "+project_id+", `analysis` = '"+analysis+"', `run_path` = '"+run_path+"',  users_warned = FALSE");

				System.out.println(sample + " - " + df.format(System.currentTimeMillis()) + " - Computing Het/Hom ratio and variant count");
				updateAnalysisMetrics(project_id, analysis);

				//Allele frequencies must be updated at once using computeAlleleFrequencies method

				System.out.println(sample + " - Importation done");
			}else{
				for (int other_project_id : otherProjects.keySet()){
					System.out.println("-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+");
					System.out.println("The sample " + sample + " already exists in the "+analysis+" variants database, under the project " + otherProjects.get(other_project_id));
					System.out.println("Importation of this sample will be skipped. To import, delete this sample or use the overwrite parameter.");
					System.out.println("-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+");
				}
			}
		}
	}
	
	public void importAnnotSV(String project, String sample, String annotsvFile, List<AnalysisFull> analyses, boolean overwrite, boolean verbose) throws Exception {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd - HH_mm_ss");
		DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
		int project_id = DBUtils.getProjectId(project, sample);
		for (AnalysisFull analysis : analyses){
			Map<Integer,String> otherProjects = new HashMap<Integer,String>();
			try (Results res = DB.select(Schema.HIGHLANDER,	
					"SELECT project_id, run_label FROM `projects` JOIN `projects_analyses` USING (project_id) "
							+ "WHERE `sample` = '" + sample + "' AND `analysis` = '"+analysis+"'")){
				while (res.next()){
					otherProjects.put(res.getInt("project_id"), res.getString("run_label"));
				}
			}
			if (otherProjects.isEmpty() || overwrite){
				try {
					setHardUpdate(true);
					for (int other_project_id : otherProjects.keySet()){
						//Delete the variants from this sample of this project
						System.out.println("-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+");
						System.out.println("WARNING -- The sample " + sample + " already exists in the "+analysis+" variants database, under the project " + otherProjects.get(other_project_id));
						System.out.println("Those variants will be DELETED before importing those of project " + project);
						System.out.println("User annotations are not deleted, but will only be visible again if you reimported an existing sample with same run label");
						System.out.println("If it's a resequencing of an existing sample (i.e. another run), user annotations are lost");
						System.out.println("-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+");
						DB.update(Schema.HIGHLANDER, "DELETE FROM "+analysis.getFromSampleAnnotations()+"WHERE `project_id` = "+other_project_id);
						DB.update(Schema.HIGHLANDER, "DELETE FROM "+analysis.getFromCustomAnnotations()+"WHERE `project_id` = "+other_project_id);
						DB.update(Schema.HIGHLANDER, "DELETE FROM projects_analyses WHERE project_id = " + other_project_id + " AND analysis = '"+analysis+"'");
					}
				}finally {
					setHardUpdate(false);
				}

				System.out.println(sample + " - " + df.format(System.currentTimeMillis()) + " - Parsing AnnotSV file");
				File input = new File(annotsvFile);
				String run_path = input.getParentFile().getCanonicalPath();
				int counter = 0;

				Set<String> genes = new HashSet<>();
				File insertFileSample = createTempInsertionFile(project+"_"+analysis+"_"+sample+"_sample_"+df2.format(System.currentTimeMillis())+".sql");
				File insertFileStatic = createTempInsertionFile(project+"_"+analysis+"_"+sample+"_static_"+df2.format(System.currentTimeMillis())+".sql");
				File insertFileGene = createTempInsertionFile(project+"_"+analysis+"_"+sample+"_gene_"+df2.format(System.currentTimeMillis())+".sql");
				File insertFileCustom = createTempInsertionFile(project+"_"+analysis+"_"+sample+"_custom_"+df2.format(System.currentTimeMillis())+".sql");

				try (FileWriter writerSample = new FileWriter(insertFileSample);
						FileWriter writerStatic = new FileWriter(insertFileStatic);
						FileWriter writerGene = new FileWriter(insertFileGene);
						FileWriter writerCustom = new FileWriter(insertFileCustom)){
					try (FileReader fr = new FileReader(input)){
						try (BufferedReader br = new BufferedReader(fr)){
							String line;
							int lineCount = 0;
							String[] header = null;
							AnnotatedVariant fullVariant = null; //We consider that we ALWAYS have a "full" line BEFORE potential "split" lines of the SV
							while ((line = br.readLine()) != null){
								boolean full = false;
								boolean noGene = false;
								lineCount++;
								if (header == null){
									header = line.split("\t");
								}else{
									String[] columns = line.split("\t");
									for (int col=0 ; col < header.length ; col++){
										//TODO mettre champ en settings
										if (header[col].equalsIgnoreCase("AnnotSV type") || header[col].equalsIgnoreCase("Annotation_mode")) {
											if (columns[col].equalsIgnoreCase("full")) {
												full = true;
												for (int c=0 ; c < header.length ; c++){
													if (header[c].equalsIgnoreCase("Gene name") || header[c].equalsIgnoreCase("Gene_name")) {
														if (columns[c] == null || columns[c].length() == 0) {
															noGene = true;
														}
														break;
													}
												}
											}else if (columns[col].equalsIgnoreCase("split")) {
												full = false;
											}
											break;
										}
									}
									try{
										AnnotatedVariant annotatedVariant = null;
										if (full && !noGene) {
											//Save the AnnotSV fields that are only present in the "full" line 
											fullVariant = new AnnotatedVariant(analysis);
											fullVariant.setAllSVAnnotations(header, columns, true, project_id, !verbose);
										}else if (full && noGene) {
											//In case the SV doesn't span a gene, we must import the complete "full" line
											annotatedVariant = new AnnotatedVariant(analysis);
											annotatedVariant.setAllSVAnnotations(header, columns, false, project_id, !verbose);
										}else {
											//SV span 1 or more genes, we only import "split" annotations (per gene), with a copy of the "full only" annotations
											annotatedVariant = new AnnotatedVariant(fullVariant);
											annotatedVariant.setAllSVAnnotations(header, columns, false, project_id, !verbose);
										}
										if (annotatedVariant != null && annotatedVariant.exist()){
											counter++;
											writerSample.write(annotatedVariant.getInsertionString(DB.getDataSource(Schema.HIGHLANDER).getDBMS(), analysis.getTableSampleAnnotations()));											
											writerStatic.write(annotatedVariant.getInsertionString(DB.getDataSource(Schema.HIGHLANDER).getDBMS(), analysis.getTableStaticAnnotations()));											
											if (annotatedVariant.affectsGene() && !genes.contains(annotatedVariant.getValue(Field.gene_symbol).toString())) {
												genes.add(annotatedVariant.getValue(Field.gene_symbol).toString());
												writerGene.write(annotatedVariant.getInsertionString(DB.getDataSource(Schema.HIGHLANDER).getDBMS(), analysis.getTableGeneAnnotations()));											
											}
											writerCustom.write(annotatedVariant.getInsertionString(DB.getDataSource(Schema.HIGHLANDER).getDBMS(), analysis.getTableCustomAnnotations()));											
											if (counter % 5000 == 0) System.out.println(sample + " - " + df.format(System.currentTimeMillis()) + " - " + counter + " variants annotated ..."); 												
										}
									}catch (Exception ex){
										System.err.println("WARNING -- Problem with line " + lineCount + " of " + annotsvFile);
										System.err.println(line);
										Tools.exception(ex);
									}
								}
							}
						}
					}
				}

				System.out.println("------------------ seconds taken per annotation category -----------------------------");
				for (Annotation a : Annotation.values()) {
					System.out.println(a + "\t" + Tools.longToString(AnnotatedVariant.time.get(a) / 1000));
				}
				System.out.println("--------------------------------------------------------------------------------------");

				System.out.println(sample + " - " + df.format(System.currentTimeMillis()) + " - Importing variants in the database");
				try {
					setHardUpdate(true);
					DB.insertFile(Schema.HIGHLANDER, analysis.getTableSampleAnnotations(), AnnotatedVariant.getInsertionColumnsString(analysis, analysis.getTableSampleAnnotations()), insertFileSample, true, parameters);
					DB.insertFile(Schema.HIGHLANDER, analysis.getTableStaticAnnotations(), AnnotatedVariant.getInsertionColumnsString(analysis, analysis.getTableStaticAnnotations()), insertFileStatic, true, parameters);
					DB.insertFile(Schema.HIGHLANDER, analysis.getTableGeneAnnotations(), AnnotatedVariant.getInsertionColumnsString(analysis, analysis.getTableGeneAnnotations()), insertFileGene, true, parameters);
					DB.insertFile(Schema.HIGHLANDER, analysis.getTableCustomAnnotations(), AnnotatedVariant.getInsertionColumnsString(analysis, analysis.getTableCustomAnnotations()), insertFileCustom, true, parameters);
					insertFileSample.delete();
					insertFileStatic.delete();
					insertFileGene.delete();
					insertFileCustom.delete();
				}finally {
					setHardUpdate(false);
				}

				DB.insert(Schema.HIGHLANDER, "INSERT IGNORE INTO `projects_analyses` SET `project_id` = "+project_id+", `analysis` = '"+analysis+"', `run_path` = '"+run_path+"',  users_warned = FALSE");

				System.out.println(sample + " - " + df.format(System.currentTimeMillis()) + " - Computing Het/Hom ratio and variant count");
				updateAnalysisMetrics(project_id, analysis);

				//Allele frequencies must be updated at once using computeAlleleFrequencies method

				System.out.println(sample + " - Importation done");
			}else{
				for (int other_project_id : otherProjects.keySet()){
					System.out.println("-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+");
					System.out.println("The sample " + sample + " already exists in the "+analysis+" variants database, under the project " + otherProjects.get(other_project_id));
					System.out.println("Importation of this sample will be skipped. To import, delete this sample or use the overwrite parameter.");
					System.out.println("-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+");
				}
			}
		}
	}

	//TODO 1000G - check analyses avec ce qu'on recoit maintenant
	//TODO 1000G - remplacer pathologies par populations
	//TODO 1000G - optimisations faites dans importSample pour gene/static
	public void import1000G(String vcf, String chromosome, AnalysisFull analysis, String project, String ids, boolean verbose) throws Exception {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd - HH_mm_ss");
		DateFormat df2 = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
		Map<String, Integer> samples = new HashMap<>();
		Map<String, String> pathologies = new HashMap<String, String>();
		File insertFileSample = createTempInsertionFile(analysis+"_"+chromosome+"_"+ids+"_sample_"+df2.format(System.currentTimeMillis())+".sql");
		File insertFileStatic = createTempInsertionFile(analysis+"_"+chromosome+"_"+ids+"_static_"+df2.format(System.currentTimeMillis())+".sql");
		File insertFileGene = createTempInsertionFile(analysis+"_"+chromosome+"_"+ids+"_gene_"+df2.format(System.currentTimeMillis())+".sql");
		File insertFileCustom = createTempInsertionFile(analysis+"_"+chromosome+"_"+ids+"_custom_"+df2.format(System.currentTimeMillis())+".sql");
		FileWriter writerSample = new FileWriter(insertFileSample);
		FileWriter writerStatic = new FileWriter(insertFileStatic);
		FileWriter writerGene = new FileWriter(insertFileGene);
		FileWriter writerCustom = new FileWriter(insertFileCustom);
		try{
			try (Results res = DB.select(Schema.HIGHLANDER, "SELECT project_id, sample, pathology FROM `projects` WHERE project_id >= " + ids.split("-")[0] + " AND project_id <= " + ids.split("-")[1])) {
				while(res.next()){
					String sample = res.getString("sample");
					samples.put(sample, res.getInt("project_id"));
					pathologies.put(sample, res.getString("pathology"));
				}
			}

			System.out.println(vcf + " - " + df.format(System.currentTimeMillis()) + " - Parsing and annotating VCF file");
			int counter = 0;

			File input = new File(vcf);
			try (FileReader fr = new FileReader(input)){
				try (BufferedReader br = new BufferedReader(fr)){
					String line;
					int lineCount = 0;
					String[] header = null;
					while ((line = br.readLine()) != null){
						lineCount++;
						if (line.startsWith("#") && !line.startsWith("##")){
							header = line.split("\t");
						}
						if (!line.startsWith("#")){
							try{
								if (header == null) throw new Exception("VCF header columns were not found, need a line starting with ONE # followed by all headers");
								String[] data = line.split("\t");
								String indexSample = samples.keySet().iterator().next();
								for (AnnotatedVariant annotatedVariant : new AnnotatedVariant(analysis).setAllAnnotations(header, data, samples.get(indexSample), !verbose)){
									counter++;
									annotatedVariant.setFieldValue(Field.zygosity, Zygosity.Homozygous.toString());
									Map<String, Zygosity> zigosities = new HashMap<String, Zygosity>();									
									Map<String, Integer> nAlleles = new HashMap<String, Integer>();									
									for (int col=0 ; col < header.length ; col++){
										Zygosity zygosity = null;
										String sample = null;
										switch(header[col]){
										case "#CHROM":
										case "CHROM":
										case "POS":
										case "ID":
										case "REF":
										case "ALT":
										case "QUAL":
										case "FILTER":
										case "INFO":
										case "FORMAT":
											break;
										default:
											sample = header[col];
											String genotype = data[col];
											zygosity = AnnotatedVariant.parseZygosity(genotype);
											if (AnnotatedVariant.isAltInGenotype(genotype, annotatedVariant.getAlternativeIndexInVCF())) {
												zigosities.put(sample, zygosity);
												if(zygosity == Zygosity.Heterozygous && !genotype.contains("0")){
													nAlleles.put(sample, 3);
												}else{
													nAlleles.put(sample, 2);
												}
											}
											else zigosities.put(sample, null);
										}
									}
									for (String sample : samples.keySet()){
										if (zigosities.get(sample) != null){
											annotatedVariant.setProject(samples.get(sample));
											annotatedVariant.setFieldValue(Field.zygosity, zigosities.get(sample).toString());
											annotatedVariant.setFieldValue(Field.allele_num, nAlleles.get(sample).toString());
											writerSample.write(annotatedVariant.getInsertionString(DB.getDataSource(Schema.HIGHLANDER).getDBMS(), analysis.getTableSampleAnnotations()));											
											writerStatic.write(annotatedVariant.getInsertionString(DB.getDataSource(Schema.HIGHLANDER).getDBMS(), analysis.getTableStaticAnnotations()));
											if (annotatedVariant.affectsGene()) {
												writerGene.write(annotatedVariant.getInsertionString(DB.getDataSource(Schema.HIGHLANDER).getDBMS(), analysis.getTableGeneAnnotations()));
											}
											writerCustom.write(annotatedVariant.getInsertionString(DB.getDataSource(Schema.HIGHLANDER).getDBMS(), analysis.getTableCustomAnnotations()));											
										}
									}

									if (counter % 50_000 == 0) System.out.println(chromosome + " - " + df.format(System.currentTimeMillis()) + " - " + counter + " variants annotated ...");

									if (counter % 1_000_000 == 0){
										writerSample.close();
										writerStatic.close();
										writerGene.close();
										writerCustom.close();
										System.out.println(chromosome + " - " + df.format(System.currentTimeMillis()) + " - Importing next 1M variants in the database");
										try {
											setHardUpdate(true);
											List<AnalysisFull> allAnalyses = new ArrayList<AnalysisFull>();
											allAnalyses.add(analysis);
											System.out.println(chromosome + " - " + df.format(System.currentTimeMillis()) + " - Importing " + insertFileSample.getName());
											DB.insertFile(Schema.HIGHLANDER, analysis.getTableSampleAnnotations(), AnnotatedVariant.getInsertionColumnsString(analysis, analysis.getTableSampleAnnotations()), insertFileSample, true, parameters);
											System.out.println(chromosome + " - " + df.format(System.currentTimeMillis()) + " - Importing " + insertFileStatic.getName());
											DB.insertFile(Schema.HIGHLANDER, analysis.getTableStaticAnnotations(), AnnotatedVariant.getInsertionColumnsString(analysis, analysis.getTableStaticAnnotations()), insertFileStatic, true, parameters);
											System.out.println(chromosome + " - " + df.format(System.currentTimeMillis()) + " - Importing " + insertFileGene.getName());
											DB.insertFile(Schema.HIGHLANDER, analysis.getTableGeneAnnotations(), AnnotatedVariant.getInsertionColumnsString(analysis, analysis.getTableGeneAnnotations()), insertFileGene, true, parameters);
											System.out.println(chromosome + " - " + df.format(System.currentTimeMillis()) + " - Importing " + insertFileCustom.getName());
											DB.insertFile(Schema.HIGHLANDER, analysis.getTableCustomAnnotations(), AnnotatedVariant.getInsertionColumnsString(analysis, analysis.getTableCustomAnnotations()), insertFileCustom, true, parameters);
											insertFileSample.delete();
											insertFileStatic.delete();
											insertFileGene.delete();
											insertFileCustom.delete();
											insertFileSample = createTempInsertionFile(analysis+"_"+chromosome+"_"+ids+"_sample_"+df2.format(System.currentTimeMillis())+".sql");
											insertFileStatic = createTempInsertionFile(analysis+"_"+chromosome+"_"+ids+"_static_"+df2.format(System.currentTimeMillis())+".sql");
											insertFileGene = createTempInsertionFile(analysis+"_"+chromosome+"_"+ids+"_gene_"+df2.format(System.currentTimeMillis())+".sql");
											insertFileCustom = createTempInsertionFile(analysis+"_"+chromosome+"_"+ids+"_custom_"+df2.format(System.currentTimeMillis())+".sql");
											writerSample = new FileWriter(insertFileSample);
											writerStatic = new FileWriter(insertFileStatic);
											writerGene = new FileWriter(insertFileGene);
											writerCustom = new FileWriter(insertFileCustom);
										}finally {
											setHardUpdate(false);
										}
									}
								}
							}catch (Exception ex){
								System.err.println("WARNING -- Problem with line " + lineCount + " of " + vcf);
								System.err.println(line);
								Tools.exception(ex);
							}
						}
					}
				}
			}
		}finally {
			writerSample.close();
			writerStatic.close();
			writerCustom.close();
		}

		System.out.println(chromosome + " - " + df.format(System.currentTimeMillis()) + " - Importing variants in the database");
		try {
			setHardUpdate(true);
			List<AnalysisFull> allAnalyses = new ArrayList<AnalysisFull>();
			allAnalyses.add(analysis);
			DB.insertFile(Schema.HIGHLANDER, analysis.getTableSampleAnnotations(), AnnotatedVariant.getInsertionColumnsString(analysis, analysis.getTableSampleAnnotations()), insertFileSample, false, parameters);
			DB.insertFile(Schema.HIGHLANDER, analysis.getTableStaticAnnotations(), AnnotatedVariant.getInsertionColumnsString(analysis, analysis.getTableStaticAnnotations()), insertFileStatic, false, parameters);
			DB.insertFile(Schema.HIGHLANDER, analysis.getTableGeneAnnotations(), AnnotatedVariant.getInsertionColumnsString(analysis, analysis.getTableGeneAnnotations()), insertFileGene, false, parameters);
			DB.insertFile(Schema.HIGHLANDER, analysis.getTableCustomAnnotations(), AnnotatedVariant.getInsertionColumnsString(analysis, analysis.getTableCustomAnnotations()), insertFileCustom, false, parameters);
			insertFileSample.delete();
			insertFileStatic.delete();
			insertFileGene.delete();
			insertFileCustom.delete();

			for (String sample : samples.keySet()){
				int project_id = samples.get(sample);
				DB.insert(Schema.HIGHLANDER, "INSERT IGNORE INTO `projects_analyses` SET `project_id` = "+project_id+", `analysis` = '"+analysis+"'");
				//Repeated for each chromosome, so more optimal to do it once at the end of importation
				//System.out.println(df.format(System.currentTimeMillis()) + " - Computing Het/Hom ratio and variant count");
				//updateAnalysisMetrics(project_id, analysis);
				System.out.println("Don't forget to update analysis metrics (het/hom ratio and variant count) when the whole 1000g has been imported");
			}
		}finally {
			setHardUpdate(false);
		}
		System.out.println(chromosome + " - Importation done");
	}

	public static void convertTo(FileType fileType, String project, String vcf, String alamutFile, AnalysisFull analysis, boolean verbose) throws Exception {
		File file = new File(vcf);
		try (FileReader fr = new FileReader(file)){
			try	(BufferedReader br = new BufferedReader(fr)){
				String line;
				while ((line = br.readLine()) != null){
					if (line.startsWith("#CHROM")){
						String[] array = line.split("\t");
						if (array.length < 9){
							throw new Exception("VCF malformed, some columns are missing. Expected headers: #CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT + 1 header per sample");
						}
						for (int i=9 ; i < array.length ; i++){
							System.out.println("Creating "+fileType+" file for sample "+array[i]);
							convertTo(fileType, project, array[i], vcf, alamutFile, analysis, verbose);
						}
						break;
					}
				}
				System.out.println("Done!");
			}
		}
	}
	
	/**
	 * Convert one sample from a VCF to another file type
	 * 
	 * @param fileType tsv, xlsx, json
	 * @param project if not based on an highlander project, you MUST set it to 'unknown'
	 * @param sample exact name in the highlander database (if needed) or name as in the VCF column 
	 * @param vcf VCF path
	 * @param alamutFile optional, to add annotations from an alamut output
	 * @param analysis you can give an analysis to add Highlander annotation (will use this analysis variant caller, reference genome and annotation databases).
	 */
	public static void convertTo(FileType fileType, String project, String sample, String vcf, String alamutFile, AnalysisFull analysis, boolean verbose) throws Exception {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd - HH_mm_ss");
		System.out.println(sample + " - " + df.format(System.currentTimeMillis()) + " - Converting VCF file");
		int counter = 0;
		File output = new File(vcf.replace(".vcf", "_"+sample+"."+fileType));
		try (FileWriter fw = new FileWriter(output)){
			Sheet sheet = null;
			Workbook wb = null;
			switch(fileType){
			case json:
				fw.write("{");
				break;
			case tsv:
				fw.write(AnnotatedVariant.getInsertionColumnsString(analysis, analysis.getTableSampleAnnotations()).replace(", ", "\t"));
				fw.write(AnnotatedVariant.getInsertionColumnsString(analysis, analysis.getTableStaticAnnotations()).replace(", ", "\t"));
				fw.write(AnnotatedVariant.getInsertionColumnsString(analysis, analysis.getTableGeneAnnotations()).replace(", ", "\t"));
				fw.write(AnnotatedVariant.getInsertionColumnsString(analysis, analysis.getTableCustomAnnotations()).replace(", ", "\t"));
				fw.write("\n");
				break;
			case xlsx:
				wb = new SXSSFWorkbook(100);  		
				sheet = wb.createSheet(df.format(System.currentTimeMillis()));
				sheet.createFreezePane(0, 1);		
				int r = 0;
				Row row = sheet.createRow(r++);
				row.setHeightInPoints(50);
				List<Field> headers = Field.getAvailableFields(analysis, false);
				for (int c = 0 ; c < headers.size() ; c++){
					Cell cell = row.createCell(c);
					cell.setCellValue(headers.get(c).getName());
				}
				sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.size()-1));
				break;
			}

			File input = new File(vcf);
			try (FileReader fr = new FileReader(input)){
				try (BufferedReader br = new BufferedReader(fr)){

					int project_id = -1;
					if (!project.equals("unknown")){
						project_id = DBUtils.getProjectId(project, sample);
					}

					AlamutParser alamut = null;
					if (alamutFile != null){
						alamut = new AlamutParser(new File(alamutFile));
					}

					String line;
					int lineCount = 0;
					int r = 1;
					String[] header = null;
					while ((line = br.readLine()) != null){
						lineCount++;
						if (line.startsWith("#") && !line.startsWith("##")){
							header = line.split("\t");
						}
						if (!line.startsWith("#")){
							try{
								if (header == null) throw new Exception("VCF header columns were not found, need a line starting with ONE # followed by all headers");
								String[] data = line.split("\t");
								AnnotatedVariant av = new AnnotatedVariant(analysis);
								for (AnnotatedVariant annotatedVariant : ((project_id != -1) ? av.setAllAnnotations(header, data, project_id, alamut, !verbose) : av.setAllAnnotations(header, data, sample, alamut, !verbose))){
									if (annotatedVariant.exist()){
										counter++;
										switch(fileType){
										case json :
											if (counter > 1) fw.write(",");
											fw.write("\""+counter+"\":"+annotatedVariant.exportToJson());
											break;
										case tsv :
											StringBuilder sb = new StringBuilder();
											sb.append(annotatedVariant.getInsertionString(DBMS.mysql, analysis.getTableSampleAnnotations()));
											sb.deleteCharAt(sb.length()-1);
											sb.append(annotatedVariant.getInsertionString(DBMS.mysql, analysis.getTableStaticAnnotations()));
											sb.deleteCharAt(sb.length()-1);
											if (annotatedVariant.affectsGene()) {
												sb.append(annotatedVariant.getInsertionString(DBMS.mysql, analysis.getTableGeneAnnotations()));
												sb.deleteCharAt(sb.length()-1);
											}
											sb.append(annotatedVariant.getInsertionString(DBMS.mysql, analysis.getTableCustomAnnotations()));
											fw.write(sb.toString());
											break;
										case xlsx:
											Row row = sheet.createRow(r++);
											int c = 0;
											for (Field f : Field.getAvailableFields(analysis, false)){
												Cell cell = row.createCell(c++);
												if (annotatedVariant.getValue(f) != null && annotatedVariant.getValue(f).toString().length() > 0){
													if (f.getFieldClass() == Timestamp.class){
														cell.setCellValue((Timestamp)annotatedVariant.getValue(f));
													}else if (f.getFieldClass() == Integer.class){
														cell.setCellValue(Integer.parseInt(annotatedVariant.getValue(f).toString()));
													}else if (f.getFieldClass() == Long.class){
														cell.setCellValue(Long.parseLong(annotatedVariant.getValue(f).toString()));
													}else if (f.getFieldClass() == Double.class){
														cell.setCellValue(Double.parseDouble(annotatedVariant.getValue(f).toString()));
													}else if (f.getFieldClass() == Boolean.class){
														cell.setCellValue(Boolean.parseBoolean(annotatedVariant.getValue(f).toString()));
													}else {
														cell.setCellValue(annotatedVariant.getValue(f).toString());
													}
												}
											}  	
											break;
										}
										if (counter % 5000 == 0) System.out.println(sample + " - " + df.format(System.currentTimeMillis()) + " - " + counter + " variants annotated ..."); 
									}
								}
							}catch (Exception ex){
								System.err.println("WARNING -- Problem with line " + lineCount + " of " + vcf);
								System.err.println(line);
								Tools.exception(ex);
							}
						}
					}
					switch(fileType){
					case json:
						fw.write("}");
						break;
					case tsv:
						break;
					case xlsx:
						try (FileOutputStream fileOut = new FileOutputStream(output)){
							wb.write(fileOut);
						}
						break;
					}
				}
			}
		}
		System.out.println(sample + " - Conversion done");
	}

	public static void updateAnalysisMetrics(int project_id, AnalysisFull analysis) throws Exception {
		Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE projects_analyses SET `het_hom_ratio_all` = '"+DBUtils.getHetHomRatio(project_id, analysis, VariantNovelty.all)+"' WHERE project_id = " + project_id + " AND analysis = '"+analysis+"'");
		Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE projects_analyses SET `het_hom_ratio_known` = '"+DBUtils.getHetHomRatio(project_id, analysis, VariantNovelty.known)+"' WHERE project_id = " + project_id + " AND analysis = '"+analysis+"'");
		Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE projects_analyses SET `het_hom_ratio_novel` = '"+DBUtils.getHetHomRatio(project_id, analysis, VariantNovelty.novel)+"' WHERE project_id = " + project_id + " AND analysis = '"+analysis+"'");
		Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE projects_analyses SET `ti_tv_ratio_all` = '"+DBUtils.getTiTvRatio(project_id, analysis, VariantNovelty.all)+"' WHERE project_id = " + project_id + " AND analysis = '"+analysis+"'");
		Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE projects_analyses SET `ti_tv_ratio_known` = '"+DBUtils.getTiTvRatio(project_id, analysis, VariantNovelty.known)+"' WHERE project_id = " + project_id + " AND analysis = '"+analysis+"'");
		Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE projects_analyses SET `ti_tv_ratio_novel` = '"+DBUtils.getTiTvRatio(project_id, analysis, VariantNovelty.novel)+"' WHERE project_id = " + project_id + " AND analysis = '"+analysis+"'");
		Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE projects_analyses SET `variant_count_all` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.all, false, VariantNovelty.all)+"' WHERE project_id = " + project_id + " AND analysis = '"+analysis+"'");
		Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE projects_analyses SET `variant_count_known` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.all, false, VariantNovelty.known)+"' WHERE project_id = " + project_id + " AND analysis = '"+analysis+"'");
		Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE projects_analyses SET `variant_count_novel` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.all, false, VariantNovelty.novel)+"' WHERE project_id = " + project_id + " AND analysis = '"+analysis+"'");
		Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE projects_analyses SET `variant_count_pass_filters_all` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.all, true, VariantNovelty.all)+"' WHERE project_id = " + project_id + " AND analysis = '"+analysis+"'");
		Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE projects_analyses SET `variant_count_pass_filters_known` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.all, true, VariantNovelty.known)+"' WHERE project_id = " + project_id + " AND analysis = '"+analysis+"'");
		Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE projects_analyses SET `variant_count_pass_filters_novel` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.all, true, VariantNovelty.novel)+"' WHERE project_id = " + project_id + " AND analysis = '"+analysis+"'");
		Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE projects_analyses SET `SNV_count_all` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.snp, false, VariantNovelty.all)+"' WHERE project_id = " + project_id + " AND analysis = '"+analysis+"'");
		Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE projects_analyses SET `SNV_count_known` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.snp, false, VariantNovelty.known)+"' WHERE project_id = " + project_id + " AND analysis = '"+analysis+"'");
		Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE projects_analyses SET `SNV_count_novel` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.snp, false, VariantNovelty.novel)+"' WHERE project_id = " + project_id + " AND analysis = '"+analysis+"'");
		Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE projects_analyses SET `SNV_count_pass_filters_all` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.snp, true, VariantNovelty.all)+"' WHERE project_id = " + project_id + " AND analysis = '"+analysis+"'");
		Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE projects_analyses SET `SNV_count_pass_filters_known` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.snp, true, VariantNovelty.known)+"' WHERE project_id = " + project_id + " AND analysis = '"+analysis+"'");
		Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE projects_analyses SET `SNV_count_pass_filters_novel` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.snp, true, VariantNovelty.novel)+"' WHERE project_id = " + project_id + " AND analysis = '"+analysis+"'");
		Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE projects_analyses SET `INDEL_count_all` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.indel, false, VariantNovelty.all)+"' WHERE project_id = " + project_id + " AND analysis = '"+analysis+"'");
		Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE projects_analyses SET `INDEL_count_known` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.indel, false, VariantNovelty.known)+"' WHERE project_id = " + project_id + " AND analysis = '"+analysis+"'");
		Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE projects_analyses SET `INDEL_count_novel` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.indel, false, VariantNovelty.novel)+"' WHERE project_id = " + project_id + " AND analysis = '"+analysis+"'");
		Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE projects_analyses SET `INDEL_count_pass_filters_all` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.indel, true, VariantNovelty.all)+"' WHERE project_id = " + project_id + " AND analysis = '"+analysis+"'");
		Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE projects_analyses SET `INDEL_count_pass_filters_known` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.indel, true, VariantNovelty.known)+"' WHERE project_id = " + project_id + " AND analysis = '"+analysis+"'");
		Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE projects_analyses SET `INDEL_count_pass_filters_novel` = '"+DBUtils.getVariantCount(project_id, analysis, VariantKind.indel, true, VariantNovelty.novel)+"' WHERE project_id = " + project_id + " AND analysis = '"+analysis+"'");
		Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE projects_analyses SET `gene_coverage_ratio_chr_xy` = '"+DBUtils.getGeneCoverageRatioChrXY(analysis.getReference(), project_id)+"' WHERE project_id = " + project_id + " AND analysis = '"+analysis+"'");
	}

	/**
	 * Counts the number of time each alternative allele is seen in an analysis.
	 * Clear the table [analysis]_allele_frequencies and populate it from scratch.
	 * 
	 * July 2017: elimination of SQL 'WHERE' and 'GROUP BY' clauses. Faster to fetch all data and sort/group in Java. Method is now 20x faster (2000% gain).
	 * Second optimization: replacement of HashMap containing ChangeStat objects by List of int arrays. More index manipulation, but gain of 20% speed.
	 * 
	 * Performances should be reevaluated with a new engine like Elastic Search or MapD, which should be more efficient than MySQL for those kind of operation. 
	 * 
	 * July 2020: complete rework of the method to compute allele frequencies
	 * Variant present in different samples from the same individual should be counted only once.
	 * Variant present in different genes from the same chr/pos/length/ref/alt should be counted only once.
	 * 
	 * September 2020: query for fetching all variants is sort by pos. It allows to clear the map of individual after processing each pos.
	 * Withtout that, the map grows too much (12Gb RAM needed for 100K variants and 2K individuals)
	 * Query itself is longer (+/- 10 minutes for 100K variants) but processing is faster, so in the end memory imprint is good (~4Gb for 100K variants) and time OK (+/- 15 min) 
	 * 
	 * @param analyses A list of Highlander analyses for which the allele frequencies must be computed
	 * @param perPathology true to separate counts per pathology (table [analysis]_allele_frequencies_per_pathology is rebuild)
	 */
	public void computeAlleleFrequencies(List<? extends Analysis> analyses, boolean perPathology) throws Exception {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd - HH_mm_ss");
		DateFormat df2 = new SimpleDateFormat("yyyy_MM_dd.HH_mm_ss");
		for (Analysis analysis : analyses){
			System.out.println(df.format(System.currentTimeMillis()) + " - Computing allele frequencies for " + analysis);
			final int AS = 1_000_000; 
			Map<String, Integer> indexes = new HashMap<>();
			int listSizes = 0;
			int local_an = 0;
			int[] local_an_per_pathology;
			Map<String, Set<String>> local_non_uniques = new HashMap<>();
			List<int[]> local_ac_het = new ArrayList<>();
			List<int[]> local_ac_hom = new ArrayList<>();
			List<boolean[][]> local_pathologies = new ArrayList<>();
			int germline_an = 0;
			int[] germline_an_per_pathology;
			Map<String, Set<String>> germline_non_uniques = new HashMap<>();
			List<int[]> germline_ac_het = new ArrayList<>();
			List<int[]> germline_ac_hom = new ArrayList<>();
			List<boolean[][]> germline_pathologies = new ArrayList<>();
			int somatic_an = 0;
			int[] somatic_an_per_pathology;
			Map<String, Set<String>> somatic_non_uniques = new HashMap<>();
			List<int[]> somatic_ac_het = new ArrayList<>();
			List<int[]> somatic_ac_hom = new ArrayList<>();
			List<boolean[][]> somatic_pathologies = new ArrayList<>();
			int i=0;
			System.out.println(df.format(System.currentTimeMillis()) + " - Fetch pathologies");
			int maxid = 0;
			try (Results res = DB.select(Schema.HIGHLANDER, "SELECT MAX(pathology_id) FROM pathologies")) {
				if (res.next()){
					maxid = res.getInt(1);
				}
			}
			maxid++;
			local_an_per_pathology = new int[maxid];
			germline_an_per_pathology = new int[maxid];
			somatic_an_per_pathology = new int[maxid];
			System.out.println(df.format(System.currentTimeMillis()) + " - Fetch individuals");
			try (Results res = DB.select(Schema.HIGHLANDER, 
					"SELECT individual, count(*) as ct, MIN(pathology_id) as pathology_id "
							+ "FROM projects JOIN projects_analyses USING (project_id) JOIN pathologies USING (pathology_id) "
							+ "WHERE analysis = '"+analysis+"' GROUP BY individual")) {
				while (res.next()){
					local_an++;
					local_an_per_pathology[res.getInt("pathology_id")]++;
				}
			}
			local_an *= 2;
			for (int k=0 ; k < local_an_per_pathology.length ; k++) {
				local_an_per_pathology[k] *= 2;
			}
			try (Results res = DB.select(Schema.HIGHLANDER, 
					"SELECT individual, count(*) as ct, MIN(pathology_id) as pathology_id "
							+ "FROM projects JOIN projects_analyses USING (project_id) JOIN pathologies USING (pathology_id) "
							+ "WHERE analysis = '"+analysis+"' AND sample_type = 'Germline' GROUP BY individual")) {
				while (res.next()){
					germline_an++;
					germline_an_per_pathology[res.getInt("pathology_id")]++;
				}
			}
			germline_an *= 2;
			for (int k=0 ; k < local_an_per_pathology.length ; k++) {
				germline_an_per_pathology[k] *= 2;
			}
			try (Results res = DB.select(Schema.HIGHLANDER, 
					"SELECT individual, count(*) as ct, MIN(pathology_id) as pathology_id "
							+ "FROM projects JOIN projects_analyses USING (project_id) JOIN pathologies USING (pathology_id) "
							+ "WHERE analysis = '"+analysis+"' AND sample_type = 'Somatic' GROUP BY individual")) {
				while (res.next()){
					somatic_an++;
					somatic_an_per_pathology[res.getInt("pathology_id")]++;
				}
			}
			somatic_an *= 2;
			for (int k=0 ; k < local_an_per_pathology.length ; k++) {
				somatic_an_per_pathology[k] *= 2;
			}
			System.out.println(df.format(System.currentTimeMillis()) + " - Get total number of variants");
			long max=1;
			try (Results res = DB.select(Schema.HIGHLANDER, "SELECT COUNT(*) FROM "+analysis.getFromSampleAnnotations())) {
				if (res.next()){
					max = res.getLong(1);
				}
			}
			int index = -1;
			int hash = -1;
			int hashIndex = -1;
			int currentPos = -1;
			int count=0;
			System.out.println(df.format(System.currentTimeMillis()) + " - Querying the database");
			try (Results res = DB.select(Schema.HIGHLANDER, 
					"SELECT SQL_NO_CACHE chr, pos, length, reference, alternative, "
							+ "pathology_id, (projects.sample_type = 'Germline') as st, (zygosity = 'Homozygous') as zy, individual "
							+ "FROM "+analysis.getFromSampleAnnotations()
							+ analysis.getJoinProjects()
							+ analysis.getJoinPathologies()
							//ORDER is slow but mandatory, so xxxx_non_uniques maps can be cleared after each pos, reducing needed memory
							+ "ORDER BY pos"
							, true)) {
				System.out.println(df.format(System.currentTimeMillis()) + " - Building the map");
				while (res.next()){
					count++;
					if (count%AS == 0){
						System.out.println(df.format(System.currentTimeMillis()) + " - " + Tools.longToString(count) + " variants processed ("+Tools.doubleToPercent(((double)count/(double)max), 2)+") - " + Tools.doubleToString(((double)Tools.getUsedMemoryInMb()), 0, false) + " Mb / "+ (Tools.doubleToString(((double)(Runtime.getRuntime().maxMemory() / 1024 /1024)), 0, false)) + " Mb of RAM used");
					}
					int pos = res.getInt("pos");
					if (pos != currentPos) {
						local_non_uniques.clear();
						germline_non_uniques.clear();
						somatic_non_uniques.clear();
						currentPos = pos;
					}
					int pathology = res.getInt("pathology_id");
					String uid = res.getString("chr") + "|" + pos + "|" + res.getString("length") + "|" + res.getString("reference") + "|" + res.getString("alternative");
					if (perPathology) uid += "|" + pathology;
					if (indexes.containsKey(uid)){
						index = indexes.get(uid);
					}else{
						index = i++;
						indexes.put(uid, index);
						if (i-(listSizes*AS) > 0){
							listSizes++;
							local_ac_het.add(new int[AS]);
							local_ac_hom.add(new int[AS]);
							local_pathologies.add(new boolean[AS][maxid]);
							germline_ac_het.add(new int[AS]);
							germline_ac_hom.add(new int[AS]);
							germline_pathologies.add(new boolean[AS][maxid]);
							somatic_ac_het.add(new int[AS]);
							somatic_ac_hom.add(new int[AS]);
							somatic_pathologies.add(new boolean[AS][maxid]);
						}
					}			
					String individual = res.getString("individual");
					hash = index/AS;
					hashIndex = index%AS;
					/* local_non_uniques tracks if a variant has been seen in an individual
					 * so if an individual has multiple samples imported, common variants will be counted once
					 * but it also permit to avoid having a variant present in multiple genes counted multiple times,
					 * without having to track anything else 
					 */
					if (!local_non_uniques.containsKey(individual)){
						local_non_uniques.put(individual, new HashSet<>());
					}
					if (!local_non_uniques.get(individual).contains(uid)) {
						local_non_uniques.get(individual).add(uid);
						local_pathologies.get(hash)[hashIndex][pathology] = true;
						if (res.getBoolean("zy")){
							local_ac_hom.get(hash)[hashIndex]++;
						}else{
							local_ac_het.get(hash)[hashIndex]++;
						}
					}
					if (res.getBoolean("st")){
						if (!germline_non_uniques.containsKey(individual)){
							germline_non_uniques.put(individual, new HashSet<>());
						}
						if (!germline_non_uniques.get(individual).contains(uid)) {
							germline_non_uniques.get(individual).add(uid);
							germline_pathologies.get(hash)[hashIndex][pathology] = true;
							if (res.getBoolean("zy")){
								germline_ac_hom.get(hash)[hashIndex]++;
							}else{
								germline_ac_het.get(hash)[hashIndex]++;
							}
						}
					}else{
						if (!somatic_non_uniques.containsKey(individual)){
							somatic_non_uniques.put(individual, new HashSet<>());
						}
						if (!somatic_non_uniques.get(individual).contains(uid)) {
							somatic_non_uniques.get(individual).add(uid);
							somatic_pathologies.get(hash)[hashIndex][pathology] = true;
							if (res.getBoolean("zy")){
								somatic_ac_hom.get(hash)[hashIndex]++;
							}else{
								somatic_ac_het.get(hash)[hashIndex]++;
							}
						}
					}
				}
			}

			System.out.println(df.format(System.currentTimeMillis()) + " - Creating insertion file");
			File insertFile = createTempInsertionFile("allele_frequencies."+df2.format(System.currentTimeMillis())+".sql");
			try (FileWriter fw = new FileWriter(insertFile)){				
				for (String uid : indexes.keySet()){
					index = indexes.get(uid);
					hash = index/AS;
					hashIndex = index%AS;
					int local_het = local_ac_het.get(hash)[hashIndex];
					int local_hom = local_ac_hom.get(hash)[hashIndex];
					int local_pathos = 0;
					int germline_het = germline_ac_het.get(hash)[hashIndex];
					int germline_hom = germline_ac_hom.get(hash)[hashIndex];
					int germline_pathos = 0;
					int somatic_het = somatic_ac_het.get(hash)[hashIndex];
					int somatic_hom = somatic_ac_hom.get(hash)[hashIndex];
					int somatic_pathos = 0;
					if (!perPathology) {
						for (boolean b : local_pathologies.get(hash)[hashIndex]) {
							local_pathos += b ? 1 : 0;
						}
						for (boolean b : local_pathologies.get(hash)[hashIndex]) {
							germline_pathos += b ? 1 : 0;
						}
						for (boolean b : local_pathologies.get(hash)[hashIndex]) {
							somatic_pathos += b ? 1 : 0;
						}
					}
					int local_an_to_use = local_an;
					int germline_an_to_use = germline_an;
					int somatic_an_to_use = somatic_an;
					if (perPathology) {
						int pathology = Integer.parseInt(uid.substring(uid.lastIndexOf("|")+1));
						local_an_to_use = local_an_per_pathology[pathology];
						germline_an_to_use = germline_an_per_pathology[pathology];
						somatic_an_to_use = somatic_an_per_pathology[pathology];
					}
					StringBuilder sb = new StringBuilder();
					sb.append(uid.replace("|", "\t")+"\t");
					if (local_an_to_use > 0)
						sb.append(((double)(local_het+local_hom+local_hom)/(double)local_an_to_use)+"\t");	//local_af
					else
						sb.append(((double)0)+"\t");	//local_af
					sb.append((local_het+local_hom+local_hom)+"\t");	//local_ac
					sb.append(local_an_to_use+"\t");	//local_an
					sb.append(local_het+"\t");	//local_het
					sb.append(local_hom+"\t");	//local_hom
					if (!perPathology) sb.append(local_pathos+"\t");	//local_pathologies
					if (germline_an_to_use > 0)
						sb.append(((double)(germline_het+germline_hom+germline_hom)/(double)germline_an_to_use)+"\t");	//germline_af
					else
						sb.append(((double)0)+"\t");	//germline_af
					sb.append((germline_het+germline_hom+germline_hom)+"\t");	//germline_ac
					sb.append(germline_an_to_use+"\t");	//germline_an
					sb.append(germline_het+"\t");	//germline_het
					sb.append(germline_hom+"\t");	//germline_hom
					if (!perPathology) sb.append(germline_pathos+"\t");	//germline_pathologies
					if (somatic_an_to_use > 0)
						sb.append(((double)(somatic_het+somatic_hom+somatic_hom)/(double)somatic_an_to_use)+"\t");	//somatic_af
					else
						sb.append(((double)0)+"\t");	//somatic_af
					sb.append((somatic_het+somatic_hom+somatic_hom)+"\t");	//somatic_ac
					sb.append(somatic_an_to_use+"\t");	//somatic_an
					sb.append(somatic_het+"\t");	//somatic_het
					sb.append(somatic_hom+"\t");	//somatic_hom
					if (!perPathology) sb.append(somatic_pathos+"\t");	//somatic_pathologies
					sb.append("\n");
					fw.write(sb.toString());
				}
			}
			System.out.println(df.format(System.currentTimeMillis()) + " - Importing allele frequencies in the database");
			try {
				setHardUpdate(true);
				String table = analysis+"_allele_frequencies";
				if (perPathology) table += "_per_pathology";
				DB.update(Schema.HIGHLANDER, "DELETE FROM "+table);
				String columns = "chr, pos, length, reference, alternative";
				if (perPathology) columns += ", pathology_id";
				columns += ", local_af, local_ac, local_an, local_het, local_hom";
				if (!perPathology) columns += ", local_pathologies";
				columns += ", germline_af, germline_ac, germline_an, germline_het, germline_hom";
				if (!perPathology) columns += ", germline_pathologies";
				columns += ", somatic_af, somatic_ac, somatic_an, somatic_het, somatic_hom";
				if (!perPathology) columns += ", somatic_pathologies";
				DB.insertFile(Schema.HIGHLANDER, table, columns, insertFile, true, parameters);
			}finally {
				setHardUpdate(false);
			}
			insertFile.delete();
			System.out.println(df.format(System.currentTimeMillis()) + " - Database successfuly updated");
		}
	}

	public static File createTempInsertionFile(String filename) throws Exception {
		File dir = new File("sql");
		if(Tools.isWindows()){
			dir = new File(Tools.getApplicationDataFolder() + "sql\\");
			//dir = new File("D:\\sql\\"); //NB à cause du tréma chez moi
		}else if (Tools.isMac()){
			dir = new File(Tools.getApplicationDataFolder() + "sql/");
		}
		dir.mkdirs();
		File insertFile = new File(dir.getAbsolutePath()+"/"+filename);
		if (!insertFile.exists()) System.out.println("Create temporary file "+insertFile.getName()+" : " + (insertFile.createNewFile()?"OK":"PROBLEM !!!!"));
		System.out.println("Set "+insertFile.getName()+" readable : " + (insertFile.setReadable(true, false)?"OK":"PROBLEM !!!!"));
		//Runtime.getRuntime().exec("chmod 777 "+insertFile.getAbsolutePath());
		return insertFile;
	}

	@SuppressWarnings("unused")
	private boolean getLock(int timeOutHours) throws Exception {
		boolean gotIt = false;
		int timewaited = 0;
		int timeout = timeOutHours * 3600000;
		while (!gotIt && timewaited < timeout){
			File lock = new File("sql/lock");
			if (!lock.exists()){				
				releaseLock();
			}
			String status = "";
			try (FileReader fr = new FileReader(lock)){
				try (BufferedReader br = new BufferedReader(fr)){
					status = br.readLine();
				}
			}
			if (status.equals("free")){
				try (FileWriter fw = new FileWriter(lock)){
					fw.write("locked");
				}
				return true;
			}else{
				Thread.sleep(20000);
				timewaited += 20000;
			}
		}		
		System.out.println("WARNING -- I waited "+timeOutHours+" hours for the lock, so I'm starting without it.");
		return false;
	}

	private void releaseLock() {
		File lock = new File("sql/lock");
		try {
			if (!lock.exists()){
				File sql = new File("sql");
				if (!sql.exists() && !sql.isDirectory()){
					sql.mkdirs();
				}
				lock.createNewFile();
			}
			try (FileWriter fw = new FileWriter(lock)){
				fw.write("free");
			}
		} catch (Exception ex) {
			Tools.exception(ex);
			System.err.println("WARNING -- Error when trying to release lock !!");
			lock.delete();
		}
	}

	public int getSampleId(String project, String sample) throws Exception {
		int id = -1;
		try {
			id = DBUtils.getProjectId(project, sample);
		}catch(Exception ex) {
			id = -1;
		}
		return id;
	}

	public String getNormal(String project, String sample) throws Exception {
		String normal = null;
		int sampleId = -1;
		Pattern pat = Pattern.compile("[^0-9]");
		boolean isSampleId = !pat.matcher(sample).find();
		if (isSampleId) {
			sampleId = Integer.parseInt(sample);
		}
		if (!isSampleId && project != null && project.length() > 0) {
			sampleId = DBUtils.getProjectId(project, sample);
		}
		if (sampleId != -1) {
			try (Results res = DB.select(Schema.HIGHLANDER, 
					"SELECT p2.sample FROM projects as p LEFT JOIN projects as p2 ON p.normal_id = p2.project_id WHERE p.project_id = "+sampleId)) {
				if (res.next()){
					normal = res.getString("p2.sample");
				}
			}						
		}else{
			try (Results res = DB.select(Schema.HIGHLANDER, 
					"SELECT p2.sample FROM projects as p LEFT JOIN projects as p2 ON p.normal_id = p2.project_id WHERE p.sample = '"+sample+"'")) {
				if (res.next()){
					normal = res.getString("p2.sample");
				}
			}			
		}
		if (normal == null) return "UNKNOWN";
		return normal;
	}

	public String getProjectField(String project, String sample, String field) throws Exception {
		String result = "UNKNOWN";
		int sampleId = -1;
		Pattern pat = Pattern.compile("[^0-9]");
		boolean isSampleId = !pat.matcher(sample).find();
		if (isSampleId) {
			sampleId = Integer.parseInt(sample);
		}
		if (!isSampleId && project != null && project.length() > 0) {
			sampleId = DBUtils.getProjectId(project, sample);
		}
		if (sampleId != -1) {
			try (Results res = DB.select(Schema.HIGHLANDER, 
					"SELECT "+field+" FROM projects JOIN pathologies USING (pathology_id) LEFT JOIN populations USING (population_id) WHERE project_id = "+sampleId)) {
				if (res.next()){
					result = res.getString(field);
				}
			}						
		}else{
			try (Results res = DB.select(Schema.HIGHLANDER, 
					"SELECT "+field+" from projects JOIN pathologies USING (pathology_id) LEFT JOIN populations USING (population_id) WHERE sample = '"+sample+"'")) {
				if (res.next()){
					result = res.getString(field);
				}
			}			
		}
		if (result == null) result = "UNKNOWN";
		return result;
	}

	public void setProjectPath(String project, String sample, List<? extends Analysis> analyses, String runPath) throws Exception {
		for (Analysis analysis : analyses) {
			DB.update(Schema.HIGHLANDER, "UPDATE `projects_analyses` SET `run_path` = '"+runPath+"' WHERE `analysis` = '"+analysis+"' AND project_id = " + DBUtils.getProjectId(project, sample));
		}
	}

	public void setSoftUpdate(boolean enable) {
		try{
			DB.update(Schema.HIGHLANDER, "UPDATE main SET update_soft = "+((enable)?1:0));
		}catch(Exception ex){
			System.err.println("WARNING -- Cannot change update_soft flag");
			ex.printStackTrace();
		}
	}

	public void setHardUpdate(boolean enable) {
		try{
			DB.update(Schema.HIGHLANDER, "UPDATE main SET update_hard = "+((enable)?1:0));
		}catch(Exception ex){
			System.err.println("WARNING -- Cannot change update_hard flag");
			ex.printStackTrace();
		}
	}

	public static void cleanVCF(String filename) throws Exception {
		File input = new File(filename);
		if (!input.exists()) {
			throw new Exception("You must choose a valid vcf file");
		}
		File output = new File(input.getAbsolutePath()+".tmp");
		try (FileWriter fw = new FileWriter(output)){
			try (FileReader fr = new FileReader(input)){
				try (BufferedReader br = new BufferedReader(fr)){
					String line;
					while ((line = br.readLine()) != null){
						if (line.startsWith("#")){
							fw.write(line+"\n");
						}else{  				
							String[] cols = line.split("\t"); 
							String ref = cols[3];
							String alt = cols[4];
							if (ref.equals(".") || ref.equals("-") || ref.equals(" ") || alt.equals(".") || alt.equals("-") || alt.equals(" ")){
								System.out.println("Removed:");
								System.out.println(line);
							}else{
								fw.write(line+"\n");
							}
						}
					}
				}
			}
		}
		input.delete();
		output.renameTo(input);
	}

	public static void cleanChr(String filename) throws Exception {
		List<String> chromosomes = new ArrayList<String>();
		for (int i=1 ; i <= 22 ; i++){
			chromosomes.add(""+i);			
			chromosomes.add("chr"+i);			
		}
		chromosomes.add("X");
		chromosomes.add("chrX");
		chromosomes.add("Y");
		chromosomes.add("chrY");
		//chromosomes.add("MT");
		//chromosomes.add("chrMT");		
		File input = new File(filename);
		if (!input.exists()) {
			throw new Exception("You must choose a valid vcf file");
		}
		File output = new File(input.getAbsolutePath()+".tmp");
		try (FileWriter fw = new FileWriter(output)){
			try (FileReader fr = new FileReader(input)){
				try (BufferedReader br = new BufferedReader(fr)){
					String line;
					while ((line = br.readLine()) != null){
						if (line.startsWith("#")){
							fw.write(line+"\n");
						}else{
							String chr = line.split("\t")[0];
							if (chromosomes.contains(chr)){
								fw.write(line+"\n");
							}
						}
					}
				}
			}
		}
		input.delete();
		output.renameTo(input);
	}

	public void computePossibleValues(List<? extends AnalysisFull> analyses) throws Exception {
		for (AnalysisFull analysis : analyses){
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd - HH_mm_ss");
			DateFormat df2 = new SimpleDateFormat("yyyy_MM_dd.HH_mm_ss");

			System.out.println(df.format(System.currentTimeMillis()) + " - Counting number of variants for " + analysis);
			Map<String, Long> numVar = new HashMap<>();
			for (Field field : Field.getAvailableFields(analysis, false)){
				numVar.put(field.getTable(analysis), (long)0);
			}
			for (String table : numVar.keySet()) {
				try (Results res = DB.select(Schema.HIGHLANDER, "SELECT COUNT(*) FROM `" + table + "`")) {		
					if (res.next()){
						numVar.put(table, res.getLong(1));
					}
				}
			}

			System.out.println(df.format(System.currentTimeMillis()) + " - Gathering possible values for " + analysis + " tables");
			try {
				setHardUpdate(true);
				DB.update(Schema.HIGHLANDER, "DELETE FROM "+analysis.getFromPossibleValues());

				for (Field field : Field.getAvailableFields(analysis, false)){
					Set<String> values = new TreeSet<String>();
					if (field.getName().equalsIgnoreCase(Field.gene_symbol.getName())){
						try (Results res = DB.select(analysis.getReference(), Schema.ENSEMBL, 
								"SELECT display_label "
										+ "FROM gene "
										+ "JOIN seq_region as R USING (seq_region_id) "
										+ "JOIN coord_system as C USING (coord_system_id) "
										+ "JOIN xref ON (display_xref_id = xref_id) "
										+ "WHERE C.rank = 1 "
										+ "AND R.`name` IN ("+HighlanderDatabase.makeSqlList(analysis.getReference().getChromosomes(), String.class)+")")) {				
							while (res.next()){
								if (res.getObject(1) != null){
									values.add(res.getObject(1).toString());
								}
							}
						}
					}else if (field.getName().equalsIgnoreCase(Field.chr.getName())){
						values.addAll(analysis.getReference().getChromosomes());
					}else if (field.getName().toLowerCase().contains("username")){
						//Get all username, to avoid having to update the list when a user creates an annotation
						try (Results res = DB.select(Schema.HIGHLANDER, "SELECT SQL_NO_CACHE DISTINCT(username) FROM users")) {				
							while (res.next()){
								if (res.getObject(1) != null){
									values.add(res.getObject(1).toString());
								}
							}
						}
					}else if (field.getName().contains("private")){
						//DO NOTHING - or everyone will see comments from everyone - History also useless
					}else if (field.getName().toLowerCase().contains("comments") || field.getName().toLowerCase().contains("history")) {
						/*	DO NOTHING - comments can contain CR (\r) or LF (\n) creating new lines in the imported file. Note that it can be handled with enclosing e.g. with "" and:
						 * 							 LOAD DATA INFILE '{fileName}' INTO TABLE {importTable} FIELDS TERMINATED BY '\t' OPTIONALLY ENCLOSED BY '"' LINES TERMINATED BY '\n' ( {fieldList} );
						 * 						   But it doesn't solve the problem for queries that don't support LF in Highlander (and doesn't seem obvious with MySQL)
						 */
						//}else if (field.getName().equalsIgnoreCase("public_comments_gene")){
						// same problem than before
						/*
					try (Results res = DB.select(field.getSchema(), "SELECT SQL_NO_CACHE DISTINCT(comments_gene) FROM " + field.getTable(analysis) + " WHERE username IS NULL")) {				
					while (res.next()){
						if (res.getObject(1) != null){
							values.add(res.getObject(1).toString());
						}
					}
					}
						 */
					}else if (field.getName().equalsIgnoreCase(Field.evaluation.getName())){
						values.add("0");
						values.add("1");
						values.add("2");
						values.add("3");
						values.add("4");
						values.add("5");
					}else if (field.getFieldClass() == Double.class || field.getFieldClass() == Integer.class || field.getFieldClass() == Long.class || field.getFieldClass() == Timestamp.class){
						//DO NOTHING - Not really useful to list all possible numbers ; and take a lot of time and database space
					}else if (field.getName().equalsIgnoreCase(Field.snpeff_other_transcripts.getName()) ){
						//DO NOTHING - Not really useful to list all possible combinations of those fields ; and take a lot of time and database space
					}else if (field.getFieldClass() == Boolean.class){
						values.add("true");
						values.add("false");
					}else if (field.getFieldClass().isEnum()){
						for (Object val : field.getFieldClass().getEnumConstants()){
							values.add(val.toString());
						}
					}else if (field.getTable(analysis).equals("projects") || 
							field.getTable(analysis).equals("pathologies") ||
							field.getTable(analysis).equals("populations") ){
						try (Results res = DB.select(Schema.HIGHLANDER, 
								"SELECT SQL_NO_CACHE DISTINCT("+field.getName()+") "
										+ "FROM " + analysis.getFromProjects()
										+ analysis.getJoinPathologies()
										+ analysis.getJoinPopulations()
										+ "JOIN `projects_analyses` USING (`project_id`) "
										+ "WHERE analysis = '" + analysis + "'")) {				
							while (res.next()){
								if (res.getObject(1) != null){
									values.add(res.getObject(1).toString());
								}
							}
						}
					}else{
						//For optimization reasons, select distinct is not used but all variants are fetched (one by one, using hugeResultSetExpected trick) and put into a Set
						//As fields with too much values and no usefulness has been removed, Set size should limited to a few millions, and memory consumption should be reasonable --> if not, the solution below can be resurected 
						try (Results res = DB.select(field.getSchema(), "SELECT SQL_NO_CACHE `"+field.getName()+"` FROM " + field.getTable(analysis), true)) {
							int count = 0;
							while (res.next()){
								if (res.getObject(1) != null){
									values.add(res.getObject(1).toString());
								}
								count++;
								if (count%(numVar.get(field.getTable(analysis))/10) == 0){
									System.out.println("Distinct field `" + field.getName() + "` (" + Tools.doubleToPercent(((double)count)/((double)(numVar.get(field.getTable(analysis)))), 0) + ") - " + Tools.doubleToString(((double)Tools.getUsedMemoryInMb()), 0, false) + " Mb / "+ (Tools.doubleToString(((double)(Runtime.getRuntime().maxMemory() / 1024 /1024)), 0, false)) + " Mb of RAM used");
								}
							}
						}

						//Alternative : to limit memory consumption, we limit value fetching and writing to batch of 1_000_000 (but necessitate a DISTINCT and ORDER BY, and LIMIT OFFSET seems to re-prefetch everything each time)
						/*
					int limit = 1_000_000;
					try (Results res = DB.select(field.getSchema(), "SELECT SQL_NO_CACHE DISTINCT(`"+field.getName()+"`) FROM " + field.getTable(analysis) + " ORDER BY `"+field.getName()+"` LIMIT " + limit, true)) {
					int count = 0;
					while (res.next()){
						if (res.getObject(1) != null){
							values.add(res.getObject(1).toString());
						}
						count++;
					}
					}
					int total = count;
					int lastTotal = 0;
					int retreived = limit;
					while (total > lastTotal && (total%limit) == 0){
						//Writing available values in the database before fetching next batch
						if (!values.isEmpty()){
							File insertFile = createTempInsertionFile(analysis + "_possible_values_"+field.getName()+"."+df2.format(System.currentTimeMillis())+".sql");
							try (FileWriter fw = new FileWriter(insertFile)){
							for (String val : values){
								fw.write(field.getName()+"\t"+val+"\n");
							}
							}
							DB.insertFile(Schema.HIGHLANDER, analysis+"_possible_values", "field, value", insertFile, parameters);
							insertFile.delete();
						}
						values.clear();						
						lastTotal = total;
						try (Results res = DB.select(field.getSchema(), "SELECT SQL_NO_CACHE DISTINCT(`"+field.getName()+"`) FROM " + field.getTable(analysis) + " ORDER BY `"+field.getName()+"` LIMIT " + limit + " OFFSET " + retreived, true)) {	
						retreived += limit;
						count = 0;
						while (res.next()){
							if (res.getObject(1) != null){
								values.add(res.getObject(1).toString());
							}
							count++;
						}
						}
						total += count;
					}	
						 */
					}

					if (!values.isEmpty()){
						File insertFile = createTempInsertionFile(analysis + "_possible_values_"+field.getName()+"."+df2.format(System.currentTimeMillis())+".sql");
						try (FileWriter fw = new FileWriter(insertFile)){
							for (String val : values){
								fw.write(field.getName()+"\t"+val+"\n");
							}
						}
						DB.insertFile(Schema.HIGHLANDER, analysis+"_possible_values", "field, value", insertFile, true, parameters);
						insertFile.delete();
					}
				}		
			}finally {
				setHardUpdate(false);
			}
			System.out.println(df.format(System.currentTimeMillis()) + " - Database '"+analysis + "_possible_values' successfuly updated");
		}
	}

	public void warnusers(List<? extends AnalysisFull> analyses) throws Exception {
		Map<AnalysisFull, Set<Integer>> warned = new HashMap<>();
		Map<String, Map<AnalysisFull, Set<String>>> samples = new HashMap<>();
		Map<String, String> recipientNames = new HashMap<String, String>();
		Map<String, String> recipientAddresses = new HashMap<String, String>();
		recipientNames.put("Administrator", "Administrator");
		recipientAddresses.put("Administrator", parameters.getAdminMail());
		for (AnalysisFull analysis : analyses) {
			try (Results res = DB.select(Schema.HIGHLANDER, 
					"SELECT p.project_id, analysis, p.sample, pathology, p.sample_type, p.normal_id, p2.sample, u.username, u.first_name, u.last_name, u.email " +
							"FROM projects as p " +
							"JOIN pathologies USING (pathology_id) " +
							"JOIN projects_users as pu USING (project_id) " +
							"JOIN projects_analyses as a USING (project_id) " +
							"JOIN users as u ON pu.username = u.username " +
							"JOIN "+analysis.getFromPossibleValues()+" as pp ON p.sample = pp.`value` " +
							"LEFT JOIN projects as p2 ON p.normal_id = p2.project_id " +
							"WHERE users_warned = FALSE AND analysis = '"+analysis+"' AND pp.field = 'sample'")){
				while (res.next()){
					String username = res.getString("u.username");
					if (!samples.containsKey(username)){
						samples.put(username, new TreeMap<>());
					}
					if (!samples.get(username).containsKey(analysis)) {
						samples.get(username).put(analysis, new HashSet<String>());
					}
					if (!samples.containsKey("Administrator")){
						samples.put("Administrator", new TreeMap<>());
					}
					if (!samples.get("Administrator").containsKey(analysis)) {
						samples.get("Administrator").put(analysis, new HashSet<String>());
					}
					String samp = res.getString("sample_type") + " sample with id " + res.getString("sample") + " ("+res.getString("pathology")+").";
					if (analysis.getVariantCaller() == VariantCaller.MUTECT && res.getString("p.normal_id") != null) {
						samp += " " + res.getString("p2.sample") + " has been used as 'normal' sample for calling.";
					}
					samples.get(username).get(analysis).add(samp);
					samples.get("Administrator").get(analysis).add(samp);
					recipientNames.put(username, res.getString("u.first_name") + " " + res.getString("u.last_name"));
					recipientAddresses.put(username, res.getString("u.email"));
					if (!warned.containsKey(analysis)) {
						warned.put(analysis, new HashSet<>());
					}
					warned.get(analysis).add(res.getInt("p.project_id"));
				}
			}
		}
		for (String username : samples.keySet()){
			System.out.println("Sending mail to " + username + " " + samples.get(username));
			StringBuilder sb = new StringBuilder();
			sb.append("Dear "+ recipientNames.get(username) + ",\n\n");
			sb.append("Your samples have been analysed or updated in Highlander.\n");
			for (AnalysisFull analysis : samples.get(username).keySet()) {
				sb.append("\nSamples in "+analysis+" ("+analysis.getSequencingTarget()+" aligned on "+analysis.getReference()+", variants called with "+analysis.getVariantCaller()+"):\n");
				for (String samp : samples.get(username).get(analysis)){
					sb.append("- "+samp + "\n");				
				}
			}
			sb.append("\nThey are now available through Highlander.\n");
			try{
				Tools.sendMail(recipientAddresses.get(username), "New samples available in Highlander", sb.toString());
			}catch(Exception ex){
				ex.printStackTrace();
				System.out.println("Mail was not send to user " + username);
			}
		}
		for (AnalysisFull analysis : warned.keySet()) {
			DB.update(Schema.HIGHLANDER, "UPDATE projects_analyses SET users_warned = TRUE WHERE analysis = '"+analysis+"' AND project_id IN ("+HighlanderDatabase.makeSqlList(warned.get(analysis), Integer.class)+")");
		}
	}

	public static void addextlinks() throws Exception {
		Set<String> existingLinks = new HashSet<>();
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT `name` FROM `external_links`")){
			while (res.next()) {
				existingLinks.add(res.getString("name"));
			}
		}
		if (!existingLinks.contains("CliniPhenome")) new ExternalLink("CliniPhenome", "Patient in CliniPhenome", "http://bridgeiris.ulb.ac.be/cliniphenome/login/login_sample_search?sample_id=","[sample]").insert(Highlander.class.getResourceAsStream("resources/ext_cliniphenome.png"));
		if (!existingLinks.contains("Varsome")) {
			new ExternalLink("Varsome", "Variant in Varsome", "https://varsome.com/variant/[genome]/[chr]-[pos]-[reference]-[alternative]","").insert(Highlander.class.getResourceAsStream("resources/ext_varsome.png"));
			Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE `external_links` SET `url_genome` = 'GRCh37=hg19;hg19=hg19;b37=hg19;b37_decoy=hg19;GRCh38=hg38;hg19_lifescope=hg19' WHERE `name` = 'Varsome'");
		}
		if (!existingLinks.contains("MutaFrame")) new ExternalLink("MutaFrame", "Variant in MutaFrame", "http://deogen2.mutaframe.com/api?k=7221a82151b7f348394a13ec389cdc0d&m=[protein_change]&p=[transcript_uniprot_id]","").insert(Highlander.class.getResourceAsStream("resources/ext_mutaframe.png"));
		if (!existingLinks.contains("Mutation Taster")) new ExternalLink("Mutation Taster", "Predictions in Mutation Taster (all transcripts)", "http://www.mutationtaster.org/cgi-bin/MutationTaster/MT_ChrPos.cgi?chromosome=[chr_grch37]&position=[pos_grch37]&ref=[reference]&alt=[alternative]","").insert(Highlander.class.getResourceAsStream("resources/ext_mutation_taster.png"));
		if (!existingLinks.contains("Marrvel 1")) new ExternalLink("Marrvel 1", "Variant and gene in Marrvel 1", "http://v1.marrvel.org/search/variant/","[chr_grch37]:[pos_grch37] [reference]>[alternative]").insert(Highlander.class.getResourceAsStream("resources/ext_marrvel1.png"));
		if (!existingLinks.contains("Marrvel 2")) new ExternalLink("Marrvel 2", "Gene in Marrvel 2", "http://marrvel.org/human/variant/","[chr_grch37]:[pos_grch37][reference]>[alternative]").insert(Highlander.class.getResourceAsStream("resources/ext_marrvel2.png"));
		if (!existingLinks.contains("ClinVarMiner")) new ExternalLink("ClinVarMiner", "Variant in ClinVar", "https://www.ncbi.nlm.nih.gov/clinvar?term=%28[chr]%5BChromosome%5D%29%20AND%20[pos]%5BBase%20Position%20for%20Assembly%20[genome]%5D","").insert(Highlander.class.getResourceAsStream("resources/ext_clinvarminer.png"));		
		if (!existingLinks.contains("ClinVar")) {
			new ExternalLink("ClinVar", "Variant in ClinVar using ClinVarMiner", "https://clinvarminer.genetics.utah.edu/submissions-by-variant/","[transcript_refseq_mrna]([gene_symbol]):[hgvs_dna] ([hgvs_protein])").insert(Highlander.class.getResourceAsStream("resources/ext_clinvar.png"));
			Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE `external_links` SET `url_genome` = 'GRCh37=GRCh37;hg19=GRCh37;b37=GRCh37;b37_decoy=GRCh37;GRCh38=GRCh38;hg19_lifescope=GRCh37' WHERE `name` = 'ClinVar'");
		}
		if (!existingLinks.contains("Franklin")) new ExternalLink("Franklin", "Variant in Franklin", "https://franklin.genoox.com/clinical-db/variant/snp/chr[chr]-[pos]-[reference]-[alternative]-hg38","").insert(Highlander.class.getResourceAsStream("resources/ext_franklin.png"));
		if (!existingLinks.contains("gnomAD")) {
			new ExternalLink("gnomAD", "Variant in gnomAD", "http://gnomad.broadinstitute.org/variant/[chr]-[pos]-[reference]-[alternative]?dataset=gnomad_[genome]","").insert(Highlander.class.getResourceAsStream("resources/ext_gnomad.png"));
			Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE `external_links` SET `url_genome` = 'GRCh37=r2_1;hg19=r2_1;b37=r2_1;b37_decoy=r2_1;GRCh38=r3;hg19_lifescope=r2_1' WHERE `name` = 'gnomAD'");		}
		if (!existingLinks.contains("ExAC")) new ExternalLink("ExAC", "Variant in ExAC", "http://gnomad.broadinstitute.org/variant/[chr_grch37]-[pos_grch37]-[reference]-[alternative]?dataset=exac","").insert(Highlander.class.getResourceAsStream("resources/ext_exac.png"));
		if (!existingLinks.contains("RGC Million Exome")) new ExternalLink("RGC Million Exome", "RGC Million Exome variants", "https://rgc-research.regeneron.com/me/variant/[chr_grch38]:[pos_grch38]:[reference]:[alternative]","").insert(Highlander.class.getResourceAsStream("resources/ext_regeneron.png"));
		if (!existingLinks.contains("dbSNP")) new ExternalLink("dbSNP", "Variant in dbSNP", "http://www.ncbi.nlm.nih.gov/projects/SNP/snp_ref.cgi?rs=","[dbsnp_id:2]").insert(Highlander.class.getResourceAsStream("resources/ext_dbsnp.png"));
		if (!existingLinks.contains("COSMIC")) new ExternalLink("COSMIC", "Variant in COSMIC", "http://cancer.sanger.ac.uk/cosmic/search?q=","[cosmic_id]").insert(Highlander.class.getResourceAsStream("resources/ext_cosmic.png"));
		if (!existingLinks.contains("Beacon of Beacons")) {
			new ExternalLink("Beacon of Beacons", "Variant in Beacon of Beacons", "https://beacon-network.org/#/search?chrom=[chr]&pos=[pos]&ref=[reference]&allele=[alternative]&rs=[genome]","").insert(Highlander.class.getResourceAsStream("resources/ext_beacon.png"));
			Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE `external_links` SET `url_genome` = 'GRCh37=GRCh37;hg19=GRCh37;b37=GRCh37;b37_decoy=GRCh37;GRCh38=GRCh38;hg19_lifescope=GRCh37' WHERE `name` = 'Beacon of Beacons'");
		}
		if (!existingLinks.contains("Mutalyzer")) new ExternalLink("Mutalyzer", "Mutalyzer name checker", "http://mutalyzer.nl/check?name=","[transcript_refseq_mrna]:[hgvs_dna]").insert(Highlander.class.getResourceAsStream("resources/ext_mutalyzer.png"));
		if (!existingLinks.contains("UCSC CNV")) {
			new ExternalLink("UCSC CNV", "UCSC interval", "http://genome.ucsc.edu/cgi-bin/hgTracks?db=[genome]&position=chr[chr]:[sv_start]-[sv_end]","").insert(Highlander.class.getResourceAsStream("resources/ext_ucsc.png"));
			Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE `external_links` SET `url_genome` = 'b37_decoy=hg19;hg19_lifescope=hg19;b37=hg19;hg19=hg19;GRCh38=hg38;GRCh37=hg19' WHERE `name` = 'UCSC CNV'");
		}
		if (!existingLinks.contains("UCSC")) {
			new ExternalLink("UCSC", "UCSC position", "http://genome.ucsc.edu/cgi-bin/hgTracks?db=[genome]&position=chr[chr]:[pos]","").insert(Highlander.class.getResourceAsStream("resources/ext_ucsc.png"));
			Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE `external_links` SET `url_genome` = 'GRCh37=hg19;hg19=hg19;b37=hg19;b37_decoy=hg19;GRCh38=hg38;hg19_lifescope=hg19' WHERE `name` = 'UCSC'");
		}
		if (!existingLinks.contains("Ensembl")) {
			new ExternalLink("Ensembl", "Ensembl gene", "http://www.ensembl.org/[genome]/Gene/Summary?g=","[gene_ensembl]").insert(Highlander.class.getResourceAsStream("resources/ext_ensembl.png"));
			Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE `external_links` SET `url_genome` = 'GRCh37=Homo_sapiens;hg19=Homo_sapiens;b37=Homo_sapiens;b37_decoy=Homo_sapiens;GRCh38=Homo_sapiens;hg19_lifescope=Homo_sapiens' WHERE `name` = 'Ensembl'");
		}
		if (!existingLinks.contains("Uniprot")) new ExternalLink("Uniprot", "Protein in UniProt", "https://www.uniprot.org/uniprot/[transcript_uniprot_id]","").insert(Highlander.class.getResourceAsStream("resources/ext_uniprot.png"));
		if (!existingLinks.contains("OMIM")) new ExternalLink("OMIM", "Gene in OMIM", "http://www.ncbi.nlm.nih.gov/omim/?term=","[gene_symbol]").insert(Highlander.class.getResourceAsStream("resources/ext_omim.png"));
		if (!existingLinks.contains("GTEx")) new ExternalLink("GTEx", "Gene in GTEx", "https://www.gtexportal.org/home/gene/[gene_symbol]","").insert(Highlander.class.getResourceAsStream("resources/ext_gtex.png"));
		if (!existingLinks.contains("PubMed")) new ExternalLink("PubMed", "Gene in PubMed", "http://www.ncbi.nlm.nih.gov/pubmed/?term=","[gene_symbol]").insert(Highlander.class.getResourceAsStream("resources/ext_pubmed.png"));
		if (!existingLinks.contains("NCBI")) new ExternalLink("NCBI", "Gene in NCBI", "http://www.ncbi.nlm.nih.gov/gene/?term=","[gene_symbol]").insert(Highlander.class.getResourceAsStream("resources/ext_ncbi.png"));
		if (!existingLinks.contains("Entrez")) new ExternalLink("Entrez", "Gene in Entrez", "http://www.ncbi.nlm.nih.gov/gquery/?term=","[gene_symbol]").insert(Highlander.class.getResourceAsStream("resources/ext_entrez.png"));
		if (!existingLinks.contains("LOVD")) new ExternalLink("LOVD", "Gene in LOVD", "http://[gene_symbol].lovd.nl","").insert(Highlander.class.getResourceAsStream("resources/ext_lovd.png"));
		if (!existingLinks.contains("DIDA")) new ExternalLink("DIDA", "Gene in DIDA", "http://dida.ibsquare.be/detail/?gene-p=","[gene_symbol]").insert(Highlander.class.getResourceAsStream("resources/ext_dida.png"));
		if (!existingLinks.contains("Decipher")) new ExternalLink("Decipher gene", "Gene in Decipher", "https://decipher.sanger.ac.uk/search?q=gene:[gene_symbol]","").insert(Highlander.class.getResourceAsStream("resources/ext_decipher_gene.png"));
		if (!existingLinks.contains("Decipher variant")) {
			new ExternalLink("Decipher variant", "Variant in Decipher", "https://www.deciphergenomics.org/search/patients/results?q=[genome]:[chr]:[sv_start]-[sv_end]","").insert(Highlander.class.getResourceAsStream("resources/ext_decipher_variant.png"));
			Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE `external_links` SET `url_genome` = 'b37_decoy=grch37;hg19_lifescope=grch37;b37=grch37;hg19=grch37;GRCh38=grch38;GRCh37=grch37' WHERE `name` = 'Decipher variant'");
		}
		if (!existingLinks.contains("HGNC")) new ExternalLink("HGNC", "Gene in HGNC", "https://www.genenames.org/tools/search/#!/genes?query=","[gene_symbol]").insert(Highlander.class.getResourceAsStream("resources/ext_hgnc.png"));
	}
	
	@Deprecated
	public void reads() throws Exception {
			File lifescope = new File("/data/results/projects/lifescope");
			for (File projectpath : lifescope.listFiles()){
				String project = projectpath.getName();
				File outputpath = new File(projectpath.getAbsolutePath()+"/TR/outputs/FragmentMapping.BAMStats");
				if (outputpath.exists()){
					for (File samplepath : outputpath.listFiles()){
						String sample = samplepath.getName().replace("_", "-");
						try{
							int idProject = DBUtils.getProjectId(project, sample);						
							System.out.println("Importing " + project + " / " + sample);
							int reads_produced = 0;
							int reads_mapped = 0;
							double percent_total_mapped = 0;
							File inputBamStat = new File (samplepath.getAbsolutePath()+"/"+samplepath.getName()+"-summary.tbl");
							if (inputBamStat.exists()){
								try (FileReader fr = new FileReader(inputBamStat)){
									try (BufferedReader br = new BufferedReader(fr)){
										String line;
										while ((line = br.readLine()) != null){
											if (line.startsWith("NumFragmentsTotal")){
												String[] vals = line.split("\t");
												for (int i=2 ; i < vals.length ; i++){
													try{
														int val = Integer.parseInt(vals[i]);
														reads_produced += val;
													}catch(Exception ex){
														System.err.println("WARNING -- BAMstat NumFragmentsTotal : cannot convert " + vals[i] + " to integer.");
													}
												}
											}else if (line.startsWith("Tag1-NumMapped")){
												String[] vals = line.split("\t");
												for (int i=2 ; i < vals.length ; i++){
													try{
														int val = Integer.parseInt(vals[i]);
														reads_mapped += val;
													}catch(Exception ex){
														System.err.println("WARNING -- BAMstat Tag1-NumMapped : cannot convert " + vals[i] + " to integer.");
													}
												}					
											}
										}
									}
								}
								percent_total_mapped = (double)reads_mapped / (double)reads_produced * 100.0;
							}else{
								throw new Exception("Cannot import BAMStat data : " + inputBamStat + " does not exist.");
							}

							DB.update(Schema.HIGHLANDER,"UPDATE `projects` SET" +
									" `reads_produced` = '"+reads_produced+"'," +
									" `reads_mapped` = '"+reads_mapped+"'," +
									" `percent_total_mapped` = '"+percent_total_mapped+"'" +
									" WHERE `project_id` = '"+idProject+"'");
						}catch(Exception ex){
							System.out.println("Problem with " + project + " / " + sample);

						}
					}
				}
			}			
		System.out.println("Done !");
	}

	//TODO BURDEN - GONL à mettre complètement à jour
	public void buildGoNL(String vcf, String chr, Reference referenceGenome) throws Exception {
		/*
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd - HH_mm_ss");
		File input = new File(vcf);
		if (!input.exists()) {
			throw new Exception("You must choose a valid VCF file");
		}
			if (!DB.hasSchema(referenceGenome, Schema.GONL)){				
				DB.update(referenceGenome, Schema.GONL, "CREATE DATABASE IF NOT EXISTS `"+DB.getSchemaName(referenceGenome, Schema.GONL)+"`");
			}
			if (!DB.hasSchema(referenceGenome, Schema.GONL)){
				throw new Exception("GONL schema is not accessible");
			}

			System.out.println("GoNL - " + df.format(System.currentTimeMillis()) + " - Creating tables");
			AnalysisFull pseudoAnalysis = new AnalysisFull("chromosome_"+chr, VariantCaller.GATK);
			List<String> statements = new ArrayList<>();
			statements.addAll(Arrays.asList(SqlGenerator.createAnalysis(pseudoAnalysis, new ArrayList<Analysis>(), false, true).toString().split(";\n")));
			for (int i=0 ; i < statements.size() ; i++){
				DB.update(referenceGenome, Schema.GONL, statements.get(i));
			}
			//TODO BURDEN - on a besoin d'une AnalysisFull, ce qui veut dire qu'on doit créer les tables analyses et references pour GoNL, sinon ce constructeur ne marchera pas.
			AnalysisFull analysis = new AnalysisFull(pseudoAnalysis);
			
			
			System.out.println("GoNL - " + df.format(System.currentTimeMillis()) + " - Annotating variants data");
			int counter = 0;
			File insertFile = createTempInsertionFile(input.getName().replace(".vcf", ".sql"));
			try (FileWriter fw = new FileWriter(insertFile)){
				try (FileReader fr = new FileReader(input)){
					try (BufferedReader br = new BufferedReader(fr)){
						String line;
						int lineCount = 0;
						String[] header = null;
						while ((line = br.readLine()) != null){
							lineCount++;
							if (line.startsWith("#") && !line.startsWith("##")){
								header = line.split("\t");
							}
							if (!line.startsWith("#")){
								try{
									if (header == null) throw new Exception("VCF header columns were not found, need a line starting with ONE # followed by all headers");
									String[] data = line.split("\t");
									int numAlleles = data[4].split(",").length;
									for (int altIdx=0 ; altIdx < numAlleles ; altIdx++){
										counter++;
										GonlVariant variant = new GonlVariant(analysis);
										variant.setVCFLine(header, data, altIdx, null);
										if (variant.exist()){
											variant.setEnsembl(new HashSet<String>(), false);
											variant.setSNPEffect(false);
											variant.setDBNSFP();
											variant.setAnnotation(Annotation.EXAC);
											variant.setAnnotation(Annotation.LOF_TOLERANT_GENES);
											variant.setAnnotation(Annotation.DANN);
											variant.setAnnotation(Annotation.EIGEN);
											variant.setAnnotation(Annotation.DEOGEN);
											variant.setConsensusPrediction();
											variant.setComputation();
										}
										fw.write(variant.getInsertionString(DB.getDataSource(Schema.GONL).getDBMS(), analysis));
										if (counter % 5000 == 0) System.out.println("Chromosome "+chr+" - " + df.format(System.currentTimeMillis()) + " - " + counter + " variants prepared ..."); 
									}
								}catch (Exception ex){
									System.err.println("WARNING -- Problem with line " + lineCount + " of " + vcf);
									System.err.println(line);
									Tools.exception(ex);
								}
							}
						}
					}
				}
			}
			System.out.println("Chromosome "+chr+" - " + df.format(System.currentTimeMillis()) + " - Importing variants in the database");
			DB.insertFile(referenceGenome, Schema.GONL, "chromosome_" + chr, GonlVariant.getInsertionColumnsString(), insertFile, parameters);
			insertFile.delete();

			System.out.println("All records treated.");
		*/
	}

	//TODO BURDEN - ExAC à mettre complètement à jour
	public void buildExac(String vcf, Reference referenceGenome) throws Exception {
		/*
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd - HH_mm_ss");
		File input = new File(vcf);
		if (!input.exists()) {
			throw new Exception("You must choose a valid VCF file");
		}
			if (!DB.hasSchema(referenceGenome, Schema.EXAC)){				
				DB.update(referenceGenome, Schema.EXAC, "CREATE DATABASE IF NOT EXISTS `"+DB.getSchemaName(referenceGenome, Schema.EXAC)+"`");
			}
			if (!DB.hasSchema(referenceGenome, Schema.EXAC)){				
				throw new Exception("EXAC schema is not accessible");
			}
			 
			String[] chromosomes = new String[24];
			for (int i=1 ; i <= 22 ; i++){
				chromosomes[i-1] = ""+i;
			}
			chromosomes[22] = "X";
			chromosomes[23] = "Y";

			System.out.println("ExAC - " + df.format(System.currentTimeMillis()) + " - Creating tables");
			List<String> statements = new ArrayList<>();
			AnalysisFull pseudoAnalysis = null;
			for (String chromosome : chromosomes){
				pseudoAnalysis = new AnalysisFull("chromosome_"+chromosome, VariantCaller.GATK);
				statements.addAll(Arrays.asList(SqlGenerator.createAnalysis(pseudoAnalysis, new ArrayList<Analysis>(), true, false).toString().split(";\n")));
			}
			for (int i=0 ; i < statements.size() ; i++){
				DB.update(Schema.EXAC, statements.get(i));
			}

			//TODO BURDEN - on a besoin d'une AnalysisFull, ce qui veut dire qu'on doit créer les tables analyses et references pour ExAC, sinon ce constructeur ne marchera pas.
			AnalysisFull analysis = new AnalysisFull(pseudoAnalysis);
			
			System.out.println("ExAC - " + df.format(System.currentTimeMillis()) + " - Annotating variants data");
			int counter = 0;
			Map<String,File> insertFiles = new HashMap<String, File>();
			for (String chromosome : chromosomes){
				insertFiles.put(chromosome, createTempInsertionFile("exac_chr_"+chromosome+".sql"));
			}
			Map<String,FileWriter> fileWriters = new HashMap<String, FileWriter>();
			try {
				for (String key : insertFiles.keySet()){
					fileWriters.put(key, new FileWriter(insertFiles.get(key)));
				}

				try (FileReader fr = new FileReader(input)){
					try (BufferedReader br = new BufferedReader(fr)){
						String line;
						int lineCount = 0;
						String[] header = null;
						while ((line = br.readLine()) != null){
							lineCount++;
							if (line.startsWith("#") && !line.startsWith("##")){
								header = line.split("\t");
							}
							if (!line.startsWith("#")){
								try{
									if (header == null) throw new Exception("VCF header columns were not found, need a line starting with ONE # followed by all headers");
									String[] data = line.split("\t");
									int numAlleles = data[4].split(",").length;
									for (int altIdx=0 ; altIdx < numAlleles ; altIdx++){
										counter++;
										ExacVariant variant = new ExacVariant(analysis);
										variant.setVCFLine(header, data, altIdx, null);
										if (variant.exist()){
											variant.setEnsembl(new HashSet<String>(), false);
											variant.setSNPEffect(false);
											variant.setDBNSFP();
											variant.setAnnotation(Annotation.GONL);
											variant.setAnnotation(Annotation.LOF_TOLERANT_GENES);
											variant.setAnnotation(Annotation.DANN);
											variant.setAnnotation(Annotation.EIGEN);
											variant.setAnnotation(Annotation.DEOGEN);
											variant.setConsensusPrediction();
											variant.setComputation();
										}
										fileWriters.get((String)variant.getValue(Field.chr)).write(variant.getInsertionString(DB.getDataSource(Schema.EXAC).getDBMS()));
										if (counter % 5000 == 0) System.out.println("All chromosomes - " + df.format(System.currentTimeMillis()) + " - " + counter + " variants prepared ..."); 
									}
								}catch (Exception ex){
									System.err.println("WARNING -- Problem with line " + lineCount + " of " + vcf);
									System.err.println(line);
									Tools.exception(ex);
								}
							}
						}
					}
				}
			}finally {
				for (FileWriter fw : fileWriters.values()){
					fw.close();
				}
			}
			for (String chr : insertFiles.keySet()){
				File insertFile = insertFiles.get(chr);
				System.out.println("Chromosome "+chr+" - " + df.format(System.currentTimeMillis()) + " - Importing variants in the database");
				DB.insertFile(referenceGenome, Schema.EXAC, "chromosome_" + chr, ExacVariant.getInsertionColumnsString(), insertFile, parameters);
				insertFile.delete();
			}

			System.out.println("All records treated.");
		*/
	}

	public static void showHelp(Tool tool){
		System.out.println("Highlander DbBuilder version "+version);
		System.out.println("NB: if you use a Unix system without graphical support (no X11) and get related errors, use -Djava.awt.headless=true");
		System.out.println("--tool/-T [tool] : name of the tool to use.");
		System.out.println("--help/-H [tool] : list of tools or parameters of a tool if given with -T.");
		System.out.println("");
		if (tool == null) {
			System.out.println("Available tools are: ");
			for (Tool t : Tool.values()) {
				System.out.printf("%-20s  %-30s%n" , t.toString(), t.getDescription());
				System.out.println("");
			}
		}else {
			System.out.printf("%-20s  %-30s%n" , tool.toString(), tool.getDescription());
			System.out.println("");
			for (String arg : tool.getArgumentsList()){
				System.out.printf("%-20s  %-30s%n" , "", arg);
			}
		}
		if (tool == null) {
			System.out.println("Available arguments are: ");
			for (ToolArgument a : ToolArgument.values()) {
				System.out.println(a.getLongArg()+"/"+a.getShortArg()+((a.hasInput())?" ["+a.getInput()+"]":"")+" : " + a.getDescription());
			}
		}
	}

	public static void main(String[] args) {
		try{
			Map<ToolArgument, String> arguments = new EnumMap<>(ToolArgument.class);
			
			Tool tool = null;
			boolean help = false;
			
			for (int i=0 ; i < args.length ; i++){
				if (args[i].equals("--tool") || args[i].equals("-T")){
					try {
						tool = Tool.valueOf(args[++i]);
					}catch (Exception e) {
						System.err.println(args[i]+" is not an existing tool.");
						showHelp(null);
						System.exit(1);
					}
				}else if (args[i].equals("--help") || args[i].equals("-H")){
					help = true;
				}else{
					for (ToolArgument ta : ToolArgument.values()) {
						if (args[i].equals(ta.getLongArg()) || args[i].equals(ta.getShortArg())){
							if (ta.hasInput()) {
								arguments.put(ta, args[++i]);
							}else {
								arguments.put(ta, "1");
							}
						}
					}
				}
			}
			
			if (tool == null || help){
				showHelp(tool);
				System.exit(0);
			}
			
			for (ToolArgument ta : tool.getOptionalArguments()) {
				if (!arguments.containsKey(ta)) {
					arguments.put(ta, tool.getDefaultValue(ta));
				}
			}
			
			Set<ToolArgument> missingMandatoryArguments = new TreeSet<>();
			for (ToolArgument ta : ToolArgument.values()) {
				if (!tool.isMandatoryArgumentsGiven(ta, arguments.get(ta))) missingMandatoryArguments.add(ta);
			}
			if (missingMandatoryArguments.isEmpty()) {
				if (tool.isDatabaseNeeded()) {
					DbBuilder dbb = new DbBuilder(arguments.get(ToolArgument.config));
					List<AnalysisFull> analyses = new ArrayList<AnalysisFull>();
					if (arguments.containsKey(ToolArgument.analysis)) {
						if (!arguments.get(ToolArgument.analysis).equalsIgnoreCase("ALL")){
							for (String s : arguments.get(ToolArgument.analysis).split("\\+")){
								analyses.add(new AnalysisFull(new Analysis(s)));
							}
						}else{
							analyses = AnalysisFull.getAvailableAnalyses();
						}
					}
					switch(tool){
					case coverage:
						dbb.buildCoverage(CoverageTarget.valueOf(arguments.get(ToolArgument.target)), arguments.get(ToolArgument.mosdepthThreshold), arguments.get(ToolArgument.mosdepthRegion), analyses, arguments.get(ToolArgument.project), arguments.get(ToolArgument.sample));
						break;
					case ioncov:
						dbb.buildIonCoverage(arguments.get(ToolArgument.input), arguments.get(ToolArgument.project), arguments.get(ToolArgument.sample), arguments.get(ToolArgument.table), analyses.get(0));
						break;
					case possiblevalues:
						dbb.computePossibleValues(analyses);
						break;
					case gonl:
						dbb.buildGoNL(arguments.get(ToolArgument.vcf), arguments.get(ToolArgument.project), new Reference(arguments.get(ToolArgument.genome)));
						break;
					case exac:
						dbb.buildExac(arguments.get(ToolArgument.vcf), new Reference(arguments.get(ToolArgument.genome)));
						break;
					case warnusers:
						dbb.warnusers(analyses);
						break;
					case addextlinks:
						DbBuilder.addextlinks();
						break;
					case variants:
						dbb.importSample(arguments.get(ToolArgument.project), arguments.get(ToolArgument.sample), arguments.get(ToolArgument.vcf), analyses, arguments.get(ToolArgument.alamut), arguments.get(ToolArgument.bool).equals("1"), arguments.get(ToolArgument.verbose).equals("1"));
						break;
					case annotsv:
						dbb.importAnnotSV(arguments.get(ToolArgument.project), arguments.get(ToolArgument.sample), arguments.get(ToolArgument.annotsv), analyses, arguments.get(ToolArgument.bool).equals("1"), arguments.get(ToolArgument.verbose).equals("1"));
						break;
					case import1000G:
						dbb.import1000G(arguments.get(ToolArgument.vcf), arguments.get(ToolArgument.sample), analyses.get(0), arguments.get(ToolArgument.project), arguments.get(ToolArgument.input), arguments.get(ToolArgument.verbose).equals("1"));
						break;
					case toTsv:
						if (arguments.get(ToolArgument.sample) == null){
							DbBuilder.convertTo(FileType.tsv, arguments.get(ToolArgument.project), arguments.get(ToolArgument.vcf), arguments.get(ToolArgument.alamut), analyses.get(0), arguments.get(ToolArgument.verbose).equals("1"));
						}else{
							DbBuilder.convertTo(FileType.tsv, arguments.get(ToolArgument.project), arguments.get(ToolArgument.sample), arguments.get(ToolArgument.vcf),arguments.get(ToolArgument.alamut), analyses.get(0), arguments.get(ToolArgument.verbose).equals("1"));
						}
						break;
					case toXlsx:
						if (arguments.get(ToolArgument.sample) == null){
							DbBuilder.convertTo(FileType.xlsx, arguments.get(ToolArgument.project), arguments.get(ToolArgument.vcf), arguments.get(ToolArgument.alamut), analyses.get(0), arguments.get(ToolArgument.verbose).equals("1"));
						}else{
							DbBuilder.convertTo(FileType.xlsx, arguments.get(ToolArgument.project), arguments.get(ToolArgument.sample), arguments.get(ToolArgument.vcf), arguments.get(ToolArgument.alamut), analyses.get(0), arguments.get(ToolArgument.verbose).equals("1"));
						}
						break;
					case toJson:
						if (arguments.get(ToolArgument.sample) == null){
							DbBuilder.convertTo(FileType.json, arguments.get(ToolArgument.project), arguments.get(ToolArgument.vcf), arguments.get(ToolArgument.alamut), analyses.get(0), arguments.get(ToolArgument.verbose).equals("1"));
						}else{
							DbBuilder.convertTo(FileType.json, arguments.get(ToolArgument.project), arguments.get(ToolArgument.sample), arguments.get(ToolArgument.vcf), arguments.get(ToolArgument.alamut), analyses.get(0), arguments.get(ToolArgument.verbose).equals("1"));
						}
						break;
					case allelefreq:
						dbb.computeAlleleFrequencies(analyses, arguments.get(ToolArgument.bool).equals("1"));
						break;
					case fastqc:
						dbb.importProjectFastQC(arguments.get(ToolArgument.project), arguments.get(ToolArgument.sample), arguments.get(ToolArgument.input));
						break;
					case gatkcoverage:
						dbb.importProjectGatkCoverage(arguments.get(ToolArgument.project), arguments.get(ToolArgument.sample), arguments.get(ToolArgument.covWithDup), arguments.get(ToolArgument.covWithoutDup), arguments.get(ToolArgument.input));
						break;
					case gatkcovwithdup:
						dbb.importProjectGatkCoverageWithDuplicates(arguments.get(ToolArgument.project), arguments.get(ToolArgument.sample), arguments.get(ToolArgument.input));
						break;
					case gatkcovwithoutdup:
						dbb.importProjectGatkCoverageWithoutDuplicates(arguments.get(ToolArgument.project), arguments.get(ToolArgument.sample), arguments.get(ToolArgument.input));
						break;
					case gatkcovexome:
						dbb.importProjectGatkCoverageExome(arguments.get(ToolArgument.project), arguments.get(ToolArgument.sample), arguments.get(ToolArgument.input));
						break;
					case gatktitv:
						dbb.importProjectGatkTiTv(arguments.get(ToolArgument.project), arguments.get(ToolArgument.sample), analyses, arguments.get(ToolArgument.input));
						break;
					case lsbamstat:
						dbb.importProjectLifeScopeBamStat(arguments.get(ToolArgument.project), arguments.get(ToolArgument.sample), arguments.get(ToolArgument.input));
						break;
					case lscoverage:
						dbb.importProjectLifeScopeCoverageWithDuplicates(arguments.get(ToolArgument.project), arguments.get(ToolArgument.sample), arguments.get(ToolArgument.input));
						break;
					case lshethom:
						dbb.importProjectLifeScopeHetHom(arguments.get(ToolArgument.project), arguments.get(ToolArgument.sample), arguments.get(ToolArgument.input));
						break;
					case lstitv:
						dbb.importProjectLifeScopeTiTv(arguments.get(ToolArgument.project), arguments.get(ToolArgument.sample), analyses, arguments.get(ToolArgument.input));
						break;
					case getsampleid:
						System.out.print(dbb.getSampleId(arguments.get(ToolArgument.project), arguments.get(ToolArgument.sample))+"");
						break;
					case getnormal:
						System.out.print(dbb.getNormal(arguments.get(ToolArgument.project), arguments.get(ToolArgument.sample)));
						break;
					case getprojectfield:
						System.out.print(dbb.getProjectField(arguments.get(ToolArgument.project), arguments.get(ToolArgument.sample), arguments.get(ToolArgument.input)));
						break;
					case setprojectpath:
						dbb.setProjectPath(arguments.get(ToolArgument.project), arguments.get(ToolArgument.sample), analyses, arguments.get(ToolArgument.runpath));
						break;
					case softupdate:
						dbb.setSoftUpdate(arguments.get(ToolArgument.bool).equals("1"));
						break;
					case cleanvcf:
					case cleanchr:
					case sqlgenerator:
					case hsqldbgenerator:
						System.err.println("Database not needed !");
						break;
					}
					dbb.DB.disconnectAll();
				}else{
					List<Analysis> analyses = new ArrayList<Analysis>();
					if (arguments.containsKey(ToolArgument.analysis)) {
						if (!arguments.get(ToolArgument.analysis).equalsIgnoreCase("ALL")){
							for (String s : arguments.get(ToolArgument.analysis).split("\\+")){
								analyses.add(new Analysis(s));
							}
						}
					}
					switch(tool){
					case cleanvcf:
						DbBuilder.cleanVCF(arguments.get(ToolArgument.input));
						break;
					case cleanchr:
						DbBuilder.cleanChr(arguments.get(ToolArgument.input));
						break;
					case sqlgenerator:
						SqlGenerator.sqlGenerator();
						break;
					case hsqldbgenerator:
						//TODO DEMO - probably doesn't work anymore
						Parameters parametersLocal = (!arguments.containsKey(ToolArgument.config)) ? new Parameters(false) : new Parameters(false, new File(arguments.get(ToolArgument.config)));
						HighlanderDatabase local = new HighlanderDatabase(parametersLocal);
						SqlGenerator.localDbGenerator(DBMS.hsqldb, local);
						break;
					case allelefreq:
					case exac:
					case fastqc:
					case gatkcoverage:
					case gatkcovexome:
					case gatkcovwithdup:
					case gatkcovwithoutdup:
					case gatktitv:
					case coverage:
					case getsampleid:
					case getnormal:
					case getprojectfield:
					case gonl:
					case ioncov:
					case lsbamstat:
					case lscoverage:
					case lshethom:
					case lstitv:
					case possiblevalues:
					case setprojectpath:
					case softupdate:
					case toJson:
					case toTsv:
					case toXlsx:
					case variants:
					case annotsv:
					case warnusers:
					case addextlinks:
					case import1000G:
						System.err.println("Database needed");
						break;
					}
				}
			}else{
				for (ToolArgument ta : missingMandatoryArguments) {
					System.err.println(tool.getArgumentText(ta));
					System.exit(1);
				}
			}
		}catch (Exception ex) {
			Tools.exception(ex);
			System.err.println(ex.getMessage());
			System.exit(1);
		}
	}
}
