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

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.tools.BurdenTest.Source;
import be.uclouvain.ngs.highlander.administration.users.User;
import be.uclouvain.ngs.highlander.administration.users.User.Settings;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.SNPEffect;
import be.uclouvain.ngs.highlander.datatype.SNPEffect.Effect;
import be.uclouvain.ngs.highlander.datatype.SNPEffect.Impact;
import be.uclouvain.ngs.highlander.datatype.SNPEffect.VariantType;
import be.uclouvain.ngs.highlander.datatype.SNPEffect.Zygosity;
import be.uclouvain.ngs.highlander.datatype.filter.CustomFilter.ComparisonOperator;

public class Field implements Serializable, Comparable<Field> {

	private static final long serialVersionUID = 1L;

	public enum SampleType {Germline,Somatic}
	public enum StructuralVariantType {INS,DEL,DUP,INV,CNV,BND,LINE1,SVA,ALU}
	public enum ImpactPrediction {DAMAGING,TOLERATED}
	public enum SplicingPrediction {AFFECTING_SPLICING,SPLICING_UNAFFECTED}
	public enum TorrentEvaluation {SIGNIFICANT,NON_SIGNIFICANT}
	public enum FitCons {HIGHLY_SIGNIFICANT,SIGNIFICANT,INFORMATIVE,OTHER}
	public enum Aloft {DOMINANT,RECESSIVE,TOLERANT}
	
	public enum Insilico {NOT_CHECKED,OK,SUSPECT,NOT_OK}
	public enum Reporting {NOT_CHECKED,YES,NO}
	public enum Validation {NOT_CHECKED,VALIDATED,SUSPECT,INVALIDATED}
	public enum Mosaicism {NOT_CHECKED,SOMATIC,DUBIOUS,GERMLINE}
	public enum Segregation {NOT_CHECKED,SINGLE,COSEG,CARRIERS,NO_COSEG,NO_COSEG_OTHER}


	private static List<Field> availableFields = new ArrayList<Field>();

	private static Map<String,Integer> fieldWidths = new LinkedHashMap<String, Integer>();
	private static Map<String,Integer> fieldAlignments = new LinkedHashMap<String, Integer>();

	public static Field project_id;
	public static Field individual;
	public static Field sample;
	public static Field pathology;
	public static Field pathology_description;
	public static Field population;
	public static Field outsourcing;
	public static Field platform;
	public static Field run_label;
	public static Field sample_type;
	public static Field variant_sample_id;
	public static Field variant_static_id;
	public static Field variant_custom_id;
	public static Field gene_id;
	public static Field insert_date_sample;
	public static Field insert_date_static;
	public static Field insert_date_custom;
	public static Field insert_date_gene;
	public static Field chr;
	public static Field pos;
	public static Field length;
	public static Field reference;
	public static Field alternative;
	public static Field variant_type;
	public static Field num_genes;
	public static Field gene_symbol;
	public static Field protein_pos;
	public static Field hgvs_dna;
	public static Field hgvs_protein;
	public static Field snpeff_other_transcripts;
	public static Field dbsnp_id;
	public static Field allelic_depth_ref;
	public static Field allelic_depth_alt;
	public static Field allelic_depth_proportion_ref;
	public static Field allelic_depth_proportion_alt;
	public static Field confidence;
	public static Field filters;
	public static Field zygosity;
	public static Field genotype_likelihood_hom_ref;
	public static Field genotype_likelihood_het;
	public static Field genotype_likelihood_hom_alt;
	public static Field read_depth;
	public static Field allele_num;
	public static Field snpeff_effect;
	public static Field snpeff_all_effects;
	public static Field snpeff_impact;
	public static Field splicing_ada_pred;
	public static Field splicing_rf_pred;
	public static Field aloft_pred;
	public static Field vest_score;
	public static Field revel_score;
	public static Field mvp_score;
	public static Field mutpred_score;
	public static Field cadd_phred;
	public static Field consensus_prediction;
	public static Field gene_ensembl;
	public static Field transcript_ensembl;
	public static Field transcript_uniprot_id;
	public static Field transcript_refseq_prot;
	public static Field transcript_refseq_mrna;
	public static Field biotype;
	public static Field gonl_ac;
	public static Field gonl_af;
	public static Field exac_ac;
	public static Field exac_af;
	public static Field exac_an;
	public static Field gnomad_wes_af;
	public static Field gnomad_wgs_af;
	public static Field variant_of_interest;
	public static Field variant_comments_private;
	public static Field variant_comments_public;
	public static Field gene_of_interest;
	public static Field gene_comments_private;
	public static Field gene_comments_public;
	public static Field sample_of_interest;
	public static Field sample_comments_private;
	public static Field sample_comments_public;
	public static Field evaluation;
	public static Field evaluation_username;
	public static Field evaluation_date;
	public static Field num_evaluated_as_type_1;
	public static Field num_evaluated_as_type_2;
	public static Field num_evaluated_as_type_3;
	public static Field num_evaluated_as_type_4;
	public static Field num_evaluated_as_type_5;
	public static Field check_insilico;
	public static Field check_insilico_username;
	public static Field check_insilico_date;
	public static Field reporting;
	public static Field reporting_username;
	public static Field reporting_date;
	public static Field check_validated_variant;
	public static Field check_validated_variant_username;
	public static Field check_validated_variant_date;
	public static Field check_somatic_variant;
	public static Field check_somatic_variant_username;
	public static Field check_somatic_variant_date;
	public static Field check_segregation;
	public static Field check_segregation_username;
	public static Field check_segregation_date;
	public static Field evaluation_comments;
	public static Field evaluation_comments_username;
	public static Field evaluation_comments_date;
	public static Field history;

	
	public static List<Field> getAvailableFields(boolean alphabeticalOrder){
		if (alphabeticalOrder){
			return new ArrayList<Field>(new TreeSet<Field>(availableFields));
		}else {
			return new ArrayList<Field>(availableFields);
		}
	}

	public static List<Field> getAvailableFields(Analysis analysis, boolean alphabeticalOrder){
		List<Field> list = new ArrayList<>();
		for (Field field : availableFields) {
			if (field.hasAnalysis(analysis)) {
				list.add(field);
			}
		}
		if (alphabeticalOrder){
			return new ArrayList<Field>(new TreeSet<Field>(list));
		}else {
			return list;
		}
	}
	
	public static List<Field> getAvailableFieldsWithPossibleValues(HighlanderDatabase DB, Analysis analysis, boolean alphabeticalOrder){
		List<Field> list = getAvailableFields(analysis, alphabeticalOrder);
		try {
			Set<String> possible = new TreeSet<>();
			try (Results res = DB.select(Schema.HIGHLANDER, 
					"SELECT `field` FROM " + analysis.getFromPossibleValues() 
					+ "WHERE `field` IN ("+HighlanderDatabase.makeSqlList(list, Field.class)+") "
					+ "GROUP BY `field`")
					) {
				while(res.next()) {
					possible.add(res.getString(1));
				}
			}
			for (Iterator<Field> it = list.iterator() ; it.hasNext() ; ) {
				Field field = it.next();
				if (!possible.contains(field.getName())) {
					it.remove();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (alphabeticalOrder){
			return new ArrayList<Field>(new TreeSet<Field>(list));
		}else {
			return list;
		}
	}

	public static final Class<?>[] AVAILABLE_CLASSES = new Class<?>[]{
		String.class,
		Double.class,
		Integer.class,
		Long.class,
		Boolean.class,
		OffsetDateTime.class,
		SampleType.class,
		StructuralVariantType.class,
		Effect.class,
		ImpactPrediction.class,
		SplicingPrediction.class,
		FitCons.class,
		Aloft.class,
		Impact.class,
		Zygosity.class,
		VariantType.class,
		Source.class,
		Insilico.class,
		Reporting.class,
		Mosaicism.class,
		Segregation.class,
		Validation.class,
		TorrentEvaluation.class,
	};

	public enum Tag {FORMAT_PERCENT_0,FORMAT_PERCENT_2,IMPACT_RANKSCORE,IMPACT_PREDICTION,CONSERVATION_RANKSCORE}
	
	public enum AnnotationType {STATIC,SAMPLE,OTHER}
	
	public enum Annotation {
		HIGHLANDER (AnnotationType.OTHER, null, "Information is available in Highlander tables (like user annotations, projects, allele frequencies)"),	
		VCF (AnnotationType.SAMPLE, null, "Information will be parsed from the VCF"),											
		COMPUTED (AnnotationType.SAMPLE, null, "Information necessitate a minor computation during importation, generally extracted from some fields present in the VCF"),									
		CONSENSUS (AnnotationType.STATIC, null, "Information necessitate a major computation during importation, generally from a combination of multiple fields. In this case it's computation of consensus predictions."), 								
		ANNOTSV (AnnotationType.SAMPLE, null, "Information need to be parsed from an additional file (not in the VCF). In this case it's the TSV generated by AnnotSV software."),										
		ALAMUT (AnnotationType.SAMPLE, null, "Information need to be parsed from an additional file (not in the VCF). In this case it's the TSV generated by Alamut software."),										
		FALSEPOSITIVEEXAMINER (AnnotationType.SAMPLE, null, "Software that directly update Highlander after importation (FalsePositiveExaminer in this case)."), 		
		ENSEMBL (AnnotationType.STATIC, Schema.ENSEMBL, "Information is fetched from annotation tables, name must match an HighlanderDatabase.Schema"), 
		DBNSFP (AnnotationType.STATIC, Schema.DBNSFP, "Information is fetched from annotation tables, name must match an HighlanderDatabase.Schema"), 
		GNOMAD_WES (AnnotationType.STATIC, Schema.GNOMAD_WES, "Information is fetched from annotation tables, name must match an HighlanderDatabase.Schema"), 
		GNOMAD_WGS (AnnotationType.STATIC, Schema.GNOMAD_WGS, "Information is fetched from annotation tables, name must match an HighlanderDatabase.Schema"), 
		COSMIC (AnnotationType.STATIC, Schema.COSMIC, "Information is fetched from annotation tables, name must match an HighlanderDatabase.Schema"), 
		GONL (AnnotationType.STATIC, Schema.GONL, "Information is fetched from annotation tables, name must match an HighlanderDatabase.Schema"), 
		EXAC (AnnotationType.STATIC, Schema.EXAC, "Information is fetched from annotation tables, name must match an HighlanderDatabase.Schema"), 
		;
		private AnnotationType type;
		private Schema schema;
		private String description;
		Annotation(AnnotationType type, Schema schema, String description) {
			this.type = type;
			this.schema = schema;
			this.description = description;
		}
		public AnnotationType getAnnotationType() {
			return type;
		}
		public boolean hasDatabaseSchema() {
			return schema != null;
		}
		public Schema getDatabaseSchema() {
			return schema;
		}
		public String getDescription() {
			return description;
		}
		public static Set<Annotation> getAnnotations(AnnotationType type){
			Set<Annotation> set = new TreeSet<>();
			for (Annotation a : values()) {
				if (a.getAnnotationType() == type) {
					set.add(a);
				}
			}
			return set;
		}
	}
	
	public enum JSon {READGROUPSETS,VARIANTS,CALLS,INFO,EXCLUDED}

	private String name;
	private Schema schema = Schema.HIGHLANDER;
	private String tableSuffix = "unknown_table";
	private String sqlDatatype = "VARCHAR(255)";
	private Class<?> fieldClass = String.class;
	private String description = "unknown field";
	private Annotation annotation = Annotation.HIGHLANDER;
	private String header = "";
	private String source = "unknown";
	private Category category = null;
	private int ordering = 1000;
	private int size = 100;
	private int alignment = JLabel.LEFT;
	private JSon jsonPath = JSon.INFO;	

	private List<ComparisonOperator> comparisonOps = new ArrayList<ComparisonOperator>();
	private Set<Tag> tags = new HashSet<Tag>();
	private Set<Analysis> analyses = new TreeSet<>();

	/**
	 * Dummy field
	 * @param name
	 */
	public Field(String name) {
		this.name = name;
	}
	
	public Field(String name, Schema schema, String table, String type, JSon jsonPath, String description, Annotation annotation, String header, String source, int ordering, Category category, int size, int alignment){
		this.name = name;
		this.schema = schema;
		this.tableSuffix = table;
		this.sqlDatatype = type;
		this.jsonPath = jsonPath;
		this.description = description;
		this.annotation = annotation;
		this.header = header;
		this.source = source;
		this.ordering = ordering;
		this.category = category;
		this.size = size;
		this.alignment = alignment;
		comparisonOps.add(ComparisonOperator.EQUAL);
		comparisonOps.add(ComparisonOperator.DIFFERENT);
		if (type.toLowerCase().startsWith("char")){
			fieldClass = String.class;
			comparisonOps.add(ComparisonOperator.CONTAINS);
			comparisonOps.add(ComparisonOperator.DOESNOTCONTAINS);
		}else if (type.toLowerCase().equals("double")){
			fieldClass = Double.class;
			comparisonOps.add(ComparisonOperator.GREATER);
			comparisonOps.add(ComparisonOperator.GREATEROREQUAL);
			comparisonOps.add(ComparisonOperator.SMALLER);
			comparisonOps.add(ComparisonOperator.SMALLEROREQUAL);
			comparisonOps.add(ComparisonOperator.RANGE_II);
			comparisonOps.add(ComparisonOperator.RANGE_IE);
			comparisonOps.add(ComparisonOperator.RANGE_EI);
			comparisonOps.add(ComparisonOperator.RANGE_EE);
		}else if (type.toLowerCase().equals("text")){
			fieldClass = String.class;
			comparisonOps.add(ComparisonOperator.CONTAINS);
			comparisonOps.add(ComparisonOperator.DOESNOTCONTAINS);
		}else if (type.toLowerCase().equals("timestamp") || type.toLowerCase().equals("datetime")){
			fieldClass = OffsetDateTime.class;
			comparisonOps.add(ComparisonOperator.GREATER);
			comparisonOps.add(ComparisonOperator.GREATEROREQUAL);
			comparisonOps.add(ComparisonOperator.SMALLER);
			comparisonOps.add(ComparisonOperator.SMALLEROREQUAL);
			comparisonOps.add(ComparisonOperator.CONTAINS);
			comparisonOps.add(ComparisonOperator.DOESNOTCONTAINS);
			comparisonOps.add(ComparisonOperator.RANGE_II);
			comparisonOps.add(ComparisonOperator.RANGE_IE);
			comparisonOps.add(ComparisonOperator.RANGE_EI);
			comparisonOps.add(ComparisonOperator.RANGE_EE);
		}else if (type.toLowerCase().equals("tinyint(1)") || type.toLowerCase().equals("boolean")){
			fieldClass = Boolean.class;
			comparisonOps.remove(ComparisonOperator.DIFFERENT);
		}else if (type.toLowerCase().startsWith("varchar")){
			fieldClass = String.class;
			comparisonOps.add(ComparisonOperator.CONTAINS);
			comparisonOps.add(ComparisonOperator.DOESNOTCONTAINS);
		}else if (type.toLowerCase().startsWith("enum(")){
			if (type.contains("Germline")){
				fieldClass = SampleType.class;
			}else if (type.contains(StructuralVariantType.LINE1.toString())){
				fieldClass = StructuralVariantType.class;
			}else if (type.contains("CDS")){
				fieldClass = Effect.class;
			}else if (type.contains(ImpactPrediction.DAMAGING.toString()) && type.contains(ImpactPrediction.TOLERATED.toString())){
				fieldClass = ImpactPrediction.class;
			}else if (type.contains(SplicingPrediction.AFFECTING_SPLICING.toString())){
				fieldClass = SplicingPrediction.class;
			}else if (type.contains(Aloft.DOMINANT.toString()) && type.contains(Aloft.RECESSIVE.toString())){
				fieldClass = Aloft.class;
			}else if (type.contains(FitCons.HIGHLY_SIGNIFICANT.toString())){
				fieldClass = FitCons.class;
			}else if (type.contains(TorrentEvaluation.NON_SIGNIFICANT.toString())){
				fieldClass = TorrentEvaluation.class;
			}else if (type.contains(Impact.MODIFIER.toString())){
				fieldClass = Impact.class;
			}else if (type.contains(Zygosity.Homozygous.toString())){
				fieldClass = Zygosity.class;
			}else if (type.contains(VariantType.INS.toString()) && type.contains(VariantType.DEL.toString())){
				fieldClass = VariantType.class;
			}else if (type.contains(Source.HIGHLANDER.toString())){
				fieldClass = Source.class;
			}else if (type.contains(Insilico.NOT_OK.toString())){
				fieldClass = Insilico.class;
			}else if (type.contains(Validation.VALIDATED.toString())){
				fieldClass = Validation.class;
			}else if (type.contains(Mosaicism.SOMATIC.toString())){
				fieldClass = Mosaicism.class;
			}else if (type.contains(Segregation.COSEG.toString())){
				fieldClass = Segregation.class;
			}else if (type.contains(Reporting.NOT_CHECKED.toString()) && type.contains(Reporting.YES.toString())){
				fieldClass = Reporting.class;
			}else{
				fieldClass = String.class;
			}
			if (getDefaultValue() == null || !getDefaultValue().equals("NOT_CHECKED")) { 
				//contains operator pose problem for fields that should not be NULL (because of another default value) but could be because of LEFT JOIN
				//e.g. "empty" user evaluations are not created for all variants, but only when a user creates one. So a LEFT JOIN will generate NULL values instead of e.g. default 'NOT_CHECKED' 
				comparisonOps.add(ComparisonOperator.CONTAINS);
				comparisonOps.add(ComparisonOperator.DOESNOTCONTAINS);
			}
		}else if (type.toLowerCase().contains("int")){
			if (type.toLowerCase().contains("bigint")) fieldClass = Long.class;
			else fieldClass = Integer.class;
			comparisonOps.add(ComparisonOperator.GREATER);
			comparisonOps.add(ComparisonOperator.GREATEROREQUAL);
			comparisonOps.add(ComparisonOperator.SMALLER);
			comparisonOps.add(ComparisonOperator.SMALLEROREQUAL);
			comparisonOps.add(ComparisonOperator.RANGE_II);
			comparisonOps.add(ComparisonOperator.RANGE_IE);
			comparisonOps.add(ComparisonOperator.RANGE_EI);
			comparisonOps.add(ComparisonOperator.RANGE_EE);
		}else{
			fieldClass = String.class;
			comparisonOps.add(ComparisonOperator.CONTAINS);
			comparisonOps.add(ComparisonOperator.DOESNOTCONTAINS);
		}
	}

	public Set<Analysis> getAnalyses() {
		return analyses;
	}

	public void addAnalysis(Analysis analysis) {
		analyses.add(analysis);
	}
	
	public void removeAnalysis(Analysis analysis) {
		analyses.remove(analysis);
	}

	public boolean hasAnalysis(Analysis analysis) {
		return analyses.contains(analysis);
	}
	
	public Set<Tag> getTags() {
		return tags;
	}

	public void addTag(Tag tag) {
		tags.add(tag);
	}
	
	public void removeTag(Tag tag) {
		tags.remove(tag);
	}
	
	public boolean hasTag(Tag tag) {
		return tags.contains(tag);
	}
	
	public void saveUserSize(int userSize){
		if (userSize != getSize()){
			try{
				size = userSize;
				Highlander.getLoggedUser().saveSettings(Settings.WIDTH, getName(), userSize+"");
			}catch(Exception ex){
				Tools.exception(ex);
			}
		}
	}

	public static Field getField(Results res) throws Exception {
		String field = res.getString("field").toLowerCase();
		String table = res.getString("table");
		Category category = Category.valueOf(res.getString("category"));
		int size = fieldWidths.get(res.getString("size"));
		int alignment = fieldAlignments.get(res.getString("alignment"));	
		JSon json = JSon.valueOf(res.getString("json").toUpperCase());
		Annotation annotation = Annotation.valueOf(res.getString("annotation_code").toUpperCase());
		Field f = new Field(field, Schema.HIGHLANDER, table, res.getString("sql_datatype"), json, res.getString("description"), annotation, res.getString("annotation_header"), res.getString("source"), res.getInt("ordering"), category, size, alignment);
		if (res.getString("tags") != null) {
			for (String tag : res.getString("tags").split(",")) {
				f.tags.add(Tag.valueOf(tag));
			}
		}
		if (res.getString("analyses") != null) {
			for (String analysis : res.getString("analyses").split(",")) {
				f.analyses.add(new Analysis(analysis));
			}
		}
		return f;
	}
	
	public static void fetchAvailableFields(HighlanderDatabase DB) throws Exception {
		fieldWidths.put("smallest", 70);
		fieldWidths.put("small", 85);
		fieldWidths.put("medium", 100);
		fieldWidths.put("moderate", 130);
		fieldWidths.put("large", 160);
		fieldWidths.put("largest", 200);
		fieldWidths.put("huge", 300);
		try (Results res = DB.select(Schema.HIGHLANDER, "SELECT * FROM field_sizes")) {
			while(res.next()){
				fieldWidths.put(res.getString("size"), res.getInt("width"));			
			}
		}

		fieldAlignments.put("LEFT", SwingConstants.LEFT);
		fieldAlignments.put("CENTER", SwingConstants.CENTER);
		fieldAlignments.put("RIGHT", SwingConstants.RIGHT);

		availableFields.clear();
		try (Results res = DB.select(Schema.HIGHLANDER, 
				"SELECT `fields`.*, group_concat(`tag`) as tags, group_concat(`analysis`) as analyses "
						+ "FROM `fields` "
						+ "JOIN `field_categories` USING(`category`)  "
						+ "LEFT JOIN `fields_tags` USING(`field`) "
						+ "LEFT JOIN `fields_analyses` USING(`field`) "
						+ "GROUP BY `field`, `table`, `sql_datatype`, `json`, `description`, `annotation_code`, `annotation_header`, `source`, `ordering`, `category`, `size`, `alignment` "
						+ "ORDER BY `field_categories`.`ordering`, `fields`.`ordering` ASC"
				)) {
			while (res.next()){
				String fieldname = res.getString("field").toLowerCase();
				Field f = getField(res);
				availableFields.add(f);
				if (fieldname.equalsIgnoreCase("project_id") && project_id == null) project_id = f;
				else if (fieldname.equalsIgnoreCase("individual") && individual == null) individual = f;
				else if (fieldname.equalsIgnoreCase("sample") && sample == null) sample = f;
				else if (fieldname.equalsIgnoreCase("pathology") && pathology == null) pathology = f;
				else if (fieldname.equalsIgnoreCase("pathology_description") && pathology_description == null) pathology_description = f;
				else if (fieldname.equalsIgnoreCase("population") && population == null) population = f;
				else if (fieldname.equalsIgnoreCase("outsourcing") && outsourcing == null) outsourcing = f;
				else if (fieldname.equalsIgnoreCase("platform") && platform == null) platform = f;
				else if (fieldname.equalsIgnoreCase("run_label") && run_label == null) run_label = f;
				else if (fieldname.equalsIgnoreCase("sample_type") && sample_type == null) sample_type = f;
				else if (fieldname.equalsIgnoreCase("variant_sample_id") && variant_sample_id == null) variant_sample_id = f;
				else if (fieldname.equalsIgnoreCase("variant_static_id") && variant_static_id == null) variant_static_id = f;
				else if (fieldname.equalsIgnoreCase("variant_custom_id") && variant_custom_id == null) variant_custom_id = f;
				else if (fieldname.equalsIgnoreCase("gene_id") && gene_id == null) gene_id = f;
				else if (fieldname.equalsIgnoreCase("insert_date_sample") && insert_date_sample == null) insert_date_sample = f;
				else if (fieldname.equalsIgnoreCase("insert_date_static") && insert_date_static == null) insert_date_static = f;
				else if (fieldname.equalsIgnoreCase("insert_date_custom") && insert_date_custom == null) insert_date_custom = f;
				else if (fieldname.equalsIgnoreCase("insert_date_gene") && insert_date_gene == null) insert_date_gene = f;
				else if (fieldname.equalsIgnoreCase("chr") && chr == null) chr = f;
				else if (fieldname.equalsIgnoreCase("pos") && pos == null) pos = f;
				else if (fieldname.equalsIgnoreCase("length") && length == null) length = f;
				else if (fieldname.equalsIgnoreCase("reference") && reference == null) reference = f;
				else if (fieldname.equalsIgnoreCase("alternative") && alternative == null) alternative = f;
				else if (fieldname.equalsIgnoreCase("variant_type") && variant_type == null) variant_type = f;
				else if (fieldname.equalsIgnoreCase("num_genes") && num_genes == null) num_genes = f;
				else if (fieldname.equalsIgnoreCase("gene_symbol") && gene_symbol == null) gene_symbol = f;
				else if (fieldname.equalsIgnoreCase("protein_pos") && protein_pos == null) protein_pos = f;
				else if (fieldname.equalsIgnoreCase("hgvs_dna") && hgvs_dna == null) hgvs_dna = f;
				else if (fieldname.equalsIgnoreCase("hgvs_protein") && hgvs_protein == null) hgvs_protein = f;
				else if (fieldname.equalsIgnoreCase("snpeff_other_transcripts") && snpeff_other_transcripts == null) snpeff_other_transcripts = f;
				else if (fieldname.equalsIgnoreCase("dbsnp_id") && dbsnp_id == null) dbsnp_id = f;
				else if (fieldname.equalsIgnoreCase("allelic_depth_ref") && allelic_depth_ref == null) allelic_depth_ref = f;
				else if (fieldname.equalsIgnoreCase("allelic_depth_alt") && allelic_depth_alt == null) allelic_depth_alt = f;
				else if (fieldname.equalsIgnoreCase("allelic_depth_proportion_ref") && allelic_depth_proportion_ref == null) allelic_depth_proportion_ref = f;
				else if (fieldname.equalsIgnoreCase("allelic_depth_proportion_alt") && allelic_depth_proportion_alt == null) allelic_depth_proportion_alt = f;
				else if (fieldname.equalsIgnoreCase("confidence") && confidence == null) confidence = f;
				else if (fieldname.equalsIgnoreCase("filters") && filters == null) filters = f;
				else if (fieldname.equalsIgnoreCase("zygosity") && zygosity == null) zygosity = f;
				else if (fieldname.equalsIgnoreCase("genotype_likelihood_hom_ref") && genotype_likelihood_hom_ref == null) genotype_likelihood_hom_ref = f;
				else if (fieldname.equalsIgnoreCase("genotype_likelihood_het") && genotype_likelihood_het == null) genotype_likelihood_het = f;
				else if (fieldname.equalsIgnoreCase("genotype_likelihood_hom_alt") && genotype_likelihood_hom_alt == null) genotype_likelihood_hom_alt = f;
				else if (fieldname.equalsIgnoreCase("read_depth") && read_depth == null) read_depth = f;
				else if (fieldname.equalsIgnoreCase("allele_num") && allele_num == null) allele_num = f;
				else if (fieldname.equalsIgnoreCase("snpeff_effect") && snpeff_effect == null) snpeff_effect = f;
				else if (fieldname.equalsIgnoreCase("snpeff_all_effects") && snpeff_all_effects == null) snpeff_all_effects = f;
				else if (fieldname.equalsIgnoreCase("snpeff_impact") && snpeff_impact == null) snpeff_impact = f;
				else if (fieldname.equalsIgnoreCase("splicing_ada_pred") && splicing_ada_pred == null) splicing_ada_pred = f;
				else if (fieldname.equalsIgnoreCase("splicing_rf_pred") && splicing_rf_pred == null) splicing_rf_pred = f;
				else if (fieldname.equalsIgnoreCase("aloft_pred") && aloft_pred == null) aloft_pred = f;
				else if (fieldname.equalsIgnoreCase("vest_score") && vest_score == null) vest_score = f;
				else if (fieldname.equalsIgnoreCase("revel_score") && revel_score == null) revel_score = f;
				else if (fieldname.equalsIgnoreCase("mvp_score") && mvp_score == null) mvp_score = f;
				else if (fieldname.equalsIgnoreCase("mutpred_score") && mutpred_score == null) mutpred_score = f;
				else if (fieldname.equalsIgnoreCase("cadd_phred") && cadd_phred == null) cadd_phred = f;
				else if (fieldname.equalsIgnoreCase("consensus_prediction") && consensus_prediction == null) consensus_prediction = f;
				else if (fieldname.equalsIgnoreCase("gene_ensembl") && gene_ensembl == null) gene_ensembl = f;
				else if (fieldname.equalsIgnoreCase("transcript_ensembl") && transcript_ensembl == null) transcript_ensembl = f;
				else if (fieldname.equalsIgnoreCase("transcript_uniprot_id") && transcript_uniprot_id == null) transcript_uniprot_id = f;
				else if (fieldname.equalsIgnoreCase("transcript_refseq_prot") && transcript_refseq_prot == null) transcript_refseq_prot = f;
				else if (fieldname.equalsIgnoreCase("transcript_refseq_mrna") && transcript_refseq_mrna == null) transcript_refseq_mrna = f;
				else if (fieldname.equalsIgnoreCase("biotype") && biotype == null) biotype = f;
				else if (fieldname.equalsIgnoreCase("gonl_ac") && gonl_ac == null) gonl_ac = f;
				else if (fieldname.equalsIgnoreCase("gonl_af") && gonl_af == null) gonl_af = f;
				else if (fieldname.equalsIgnoreCase("exac_ac") && exac_ac == null) exac_ac = f;
				else if (fieldname.equalsIgnoreCase("exac_af") && exac_af == null) exac_af = f;
				else if (fieldname.equalsIgnoreCase("exac_an") && exac_an == null) exac_an = f;
				else if (fieldname.equalsIgnoreCase("gnomad_wes_af") && gnomad_wes_af == null) gnomad_wes_af = f;
				else if (fieldname.equalsIgnoreCase("gnomad_wgs_af") && gnomad_wgs_af == null) gnomad_wgs_af = f;
				else if (fieldname.equalsIgnoreCase("variant_of_interest") && variant_of_interest == null) variant_of_interest = f;
				else if (fieldname.equalsIgnoreCase("variant_comments_private") && variant_comments_private == null) variant_comments_private = f;
				else if (fieldname.equalsIgnoreCase("variant_comments_public") && variant_comments_public == null) variant_comments_public = f;
				else if (fieldname.equalsIgnoreCase("gene_of_interest") && gene_of_interest == null) gene_of_interest = f;
				else if (fieldname.equalsIgnoreCase("gene_comments_private") && gene_comments_private == null) gene_comments_private = f;
				else if (fieldname.equalsIgnoreCase("gene_comments_public") && gene_comments_public == null) gene_comments_public = f;
				else if (fieldname.equalsIgnoreCase("sample_of_interest") && sample_of_interest == null) sample_of_interest = f;
				else if (fieldname.equalsIgnoreCase("sample_comments_private") && sample_comments_private == null) sample_comments_private = f;
				else if (fieldname.equalsIgnoreCase("sample_comments_public") && sample_comments_public == null) sample_comments_public = f;
				else if (fieldname.equalsIgnoreCase("evaluation") && evaluation == null) evaluation = f;
				else if (fieldname.equalsIgnoreCase("evaluation_username") && evaluation_username == null) evaluation_username = f;
				else if (fieldname.equalsIgnoreCase("evaluation_date") && evaluation_date == null) evaluation_date = f;
				else if (fieldname.equalsIgnoreCase("num_evaluated_as_type_1") && num_evaluated_as_type_1 == null) num_evaluated_as_type_1 = f;
				else if (fieldname.equalsIgnoreCase("num_evaluated_as_type_2") && num_evaluated_as_type_2 == null) num_evaluated_as_type_2 = f;
				else if (fieldname.equalsIgnoreCase("num_evaluated_as_type_3") && num_evaluated_as_type_3 == null) num_evaluated_as_type_3 = f;
				else if (fieldname.equalsIgnoreCase("num_evaluated_as_type_4") && num_evaluated_as_type_4 == null) num_evaluated_as_type_4 = f;
				else if (fieldname.equalsIgnoreCase("num_evaluated_as_type_5") && num_evaluated_as_type_5 == null) num_evaluated_as_type_5 = f;
				else if (fieldname.equalsIgnoreCase("check_insilico") && check_insilico == null) check_insilico = f;
				else if (fieldname.equalsIgnoreCase("check_insilico_username") && check_insilico_username == null) check_insilico_username = f;
				else if (fieldname.equalsIgnoreCase("check_insilico_date") && check_insilico_date == null) check_insilico_date = f;
				else if (fieldname.equalsIgnoreCase("reporting") && reporting == null) reporting = f;
				else if (fieldname.equalsIgnoreCase("reporting_username") && reporting_username == null) reporting_username = f;
				else if (fieldname.equalsIgnoreCase("reporting_date") && reporting_date == null) reporting_date = f;
				else if (fieldname.equalsIgnoreCase("check_validated_variant") && check_validated_variant == null) check_validated_variant = f;
				else if (fieldname.equalsIgnoreCase("check_validated_variant_username") && check_validated_variant_username == null) check_validated_variant_username = f;
				else if (fieldname.equalsIgnoreCase("check_validated_variant_date") && check_validated_variant_date == null) check_validated_variant_date = f;
				else if (fieldname.equalsIgnoreCase("check_somatic_variant") && check_somatic_variant == null) check_somatic_variant = f;
				else if (fieldname.equalsIgnoreCase("check_somatic_variant_username") && check_somatic_variant_username == null) check_somatic_variant_username = f;
				else if (fieldname.equalsIgnoreCase("check_somatic_variant_date") && check_somatic_variant_date == null) check_somatic_variant_date = f;
				else if (fieldname.equalsIgnoreCase("check_segregation") && check_segregation == null) check_segregation = f;
				else if (fieldname.equalsIgnoreCase("check_segregation_username") && check_segregation_username == null) check_segregation_username = f;
				else if (fieldname.equalsIgnoreCase("check_segregation_date") && check_segregation_date == null) check_segregation_date = f;
				else if (fieldname.equalsIgnoreCase("evaluation_comments") && evaluation_comments == null) evaluation_comments = f;
				else if (fieldname.equalsIgnoreCase("evaluation_comments_username") && evaluation_comments_username == null) evaluation_comments_username = f;
				else if (fieldname.equalsIgnoreCase("evaluation_comments_date") && evaluation_comments_date == null) evaluation_comments_date = f;
				else if (fieldname.equalsIgnoreCase("history") && history == null) history = f;
			}
		}
	}

	public static void setUserCustomWidths(HighlanderDatabase DB, User user) throws Exception {
		//Fetch user custom field sizes
		Map<String, Integer> customWidth = new HashMap<>();
		if (Highlander.getLoggedUser() != null){
			try (Results res = DB.select(Schema.HIGHLANDER, "SELECT SUBSTRING(`key`,7) as field, `value` as width FROM `users_data` WHERE `username` = '"+user.getUsername()+"' AND `type` = 'SETTINGS' AND INSTR(`key`,'"+Settings.WIDTH+"|') > 0")) {
				while(res.next()){
					String key = res.getString("field").toLowerCase();
					if (res.getObject("width") != null) {
						customWidth.put(key, res.getInt("width"));
					}
				}
			}
		}
		for (Field field : availableFields) {
			if (customWidth.containsKey(field.getName().toLowerCase())) {
				field.size = customWidth.get(field.getName().toLowerCase());
			}
		}
	}
	
	/**
	 * Check if a field exists in the database, based on its column name
	 * 
	 * @param columnName
	 * @return
	 */
	public static boolean exists(String columnName) {
		for (Field f : availableFields){
			if (f.getName().equalsIgnoreCase(columnName)) return true;
		}
		return false;
	}

	/**
	 * Return a field based on its column name.
	 * 
	 * @param columnName
	 * @return the field, or an empty field with given name if it doesn't exist
	 */
	public static Field getField(String columnName) {
		for (Field f : availableFields){
			if (f.getName().equalsIgnoreCase(columnName)) return f;
		}
		return new Field(columnName);
	}
	
	public void updateName(String newName) throws Exception {
		int count = 0;
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT COUNT(*) FROM `fields` WHERE `field` = '"+newName+"'")) {
			if (res.next()){
				count = res.getInt(1);
			}
		}
		if (count > 0){
			throw new Exception("Field '"+newName+"' already exist in the database");
		}else {
			Highlander.getDB().update(Schema.HIGHLANDER, 
					"UPDATE `fields` SET " +
							"`field` = '"+Highlander.getDB().format(Schema.HIGHLANDER, newName)+"' " +
							"WHERE `field` = '"+getName()+"'");	
			Highlander.getDB().update(Schema.HIGHLANDER, 
					"UPDATE `fields_analyses` SET " +
							"`field` = '"+Highlander.getDB().format(Schema.HIGHLANDER, newName)+"' " +
							"WHERE `field` = '"+getName()+"'");
			Highlander.getDB().update(Schema.HIGHLANDER, 
					"UPDATE `fields_tags` SET " +
							"`field` = '"+Highlander.getDB().format(Schema.HIGHLANDER, newName)+"' " +
							"WHERE `field` = '"+getName()+"'");
			for (Analysis analysis : analyses) {
			Highlander.getDB().update(Schema.HIGHLANDER, 
					"ALTER TABLE `"+analysis.getTableCustomAnnotations()+"` "
							+ "CHANGE `"+getName()+"` `"+Highlander.getDB().format(Schema.HIGHLANDER, newName)+"` "+getSqlDatatype()+" DEFAULT NULL");
			}
			this.name = newName;
		}
	}

	public void setSchema(Schema schema) {
		this.schema = schema;
	}

	public void setSqlDatatype(String sqlDatatype) {
		this.sqlDatatype = sqlDatatype;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setAnnotation(Annotation annotation) {
		this.annotation = annotation;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public void setOrdering(int ordering) {
		this.ordering = ordering;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public void setSize(String size) {
		this.size = fieldWidths.get(size);
	}
	
	public void setAlignment(int alignment) {
		this.alignment = alignment;
	}

	public void setAlignment(String alignment) {
		this.alignment = fieldAlignments.get(alignment);
	}
	
	public void setJsonPath(JSon jsonPath) {
		this.jsonPath = jsonPath;
	}

	public void setTableSuffix(String tableSuffix) {
		this.tableSuffix = tableSuffix;
	}
	
	public void insert() throws Exception {
		int count = 0;
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT COUNT(*) FROM `fields` WHERE `field` = '"+getName()+"'")) {
			if (res.next()){
				count = res.getInt(1);
			}
		}
		if (count > 0){
			throw new Exception("Field '"+getName()+"' already exist in the database");
		}else {
			ordering = 0;
			for (Field f : getAvailableFields(false)) {
				if (f.getOrdering() > ordering) ordering = f.getOrdering();
			}
			ordering++;
			String sizeString = "medium";
			for (String s : fieldWidths.keySet()) {
				if (fieldWidths.get(s) == size) {
					sizeString = s;
				}
			}
			String alignmentString = "LEFT";
			for (String s : fieldAlignments.keySet()) {
				if (fieldAlignments.get(s) == alignment) {
					alignmentString = s;
				}
			}
			Highlander.getDB().update(Schema.HIGHLANDER, 
					"INSERT INTO `fields` "
					+ "(`field`,`table`,`sql_datatype`,`json`,`description`,`annotation_code`,`annotation_header`,`source`,`ordering`,`category`,`size`,`alignment`) "
					+ "VALUES ("
					+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getName())+"', "
					+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getTableSuffix())+"', "
					+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getSqlDatatype())+"', "
					+ "'"+getJSonPath()+"', "
					+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getDescription())+"', "
					+ "'"+getAnnotationCode()+"', "
					+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, header)+"', "
					+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getSource())+"', "
					+ ""+getOrdering()+", "
					+ "'"+getCategory().getName()+"', "
					+ "'"+sizeString+"', "
					+ "'"+alignmentString+"' "
					+ ")");		
			for (Tag tag : tags) {
				Highlander.getDB().insert(Schema.HIGHLANDER, "INSERT INTO `fields_tags` (`field`,`tag`) VALUES ('"+getName()+"','"+tag+"')");
			}
			for (Analysis analysis : analyses) {
				Highlander.getDB().insert(Schema.HIGHLANDER, "INSERT INTO `fields_analyses` (`field`,`analysis`) VALUES ('"+getName()+"','"+analysis+"')");
			}
		}
	}

	public void update() throws Exception {
		String sizeString = "medium";
		for (String s : fieldWidths.keySet()) {
			if (fieldWidths.get(s) == size) {
				sizeString = s;
			}
		}
		String alignmentString = "LEFT";
		for (String s : fieldAlignments.keySet()) {
			if (fieldAlignments.get(s) == alignment) {
				alignmentString = s;
			}
		}
		Highlander.getDB().update(Schema.HIGHLANDER, 
				"UPDATE `fields` SET " +
				"`table` = '"+Highlander.getDB().format(Schema.HIGHLANDER, getTableSuffix())+"', " +
				"`sql_datatype` = '"+Highlander.getDB().format(Schema.HIGHLANDER, getSqlDatatype())+"', " +
				"`json` = '"+getJSonPath()+"', " +
				"`description` = '"+Highlander.getDB().format(Schema.HIGHLANDER, getDescription())+"', " +
				"`annotation_code` = '"+getAnnotationCode()+"', " +
				"`annotation_header` = '"+Highlander.getDB().format(Schema.HIGHLANDER, header)+"', " +
				"`source` = '"+Highlander.getDB().format(Schema.HIGHLANDER, getSource())+"', " +
				"`ordering` = "+getOrdering()+", " +
				"`category` = '"+getCategory().getName()+"', " +
				"`size` = '"+sizeString+"', " +
				"`alignment` = '"+alignmentString+"' " +
				"WHERE `field` = '"+getName()+"'");	
		Highlander.getDB().update(Schema.HIGHLANDER, 
				"DELETE FROM `fields_tags` " +
				"WHERE `field` = '"+getName()+"'");
		for (Tag tag : tags)
			Highlander.getDB().insert(Schema.HIGHLANDER, "INSERT INTO `fields_tags` (`field`,`tag`) VALUES ('"+getName()+"','"+tag+"')");
		Highlander.getDB().update(Schema.HIGHLANDER, 
				"DELETE FROM `fields_analyses` " +
						"WHERE `field` = '"+getName()+"'");
		for (Analysis analysis : analyses)
			Highlander.getDB().insert(Schema.HIGHLANDER, "INSERT INTO `fields_analyses` (`field`,`analysis`) VALUES ('"+getName()+"','"+analysis+"')");
	}
	
	public void delete() throws Exception {
		for (Analysis analysis : analyses) {
			Highlander.getDB().update(Schema.HIGHLANDER, "ALTER TABLE `"+analysis.getTableCustomAnnotations()+"` DROP COLUMN `"+getName()+"`");
		}
		Highlander.getDB().update(Schema.HIGHLANDER, "DELETE FROM `fields` WHERE `field` = '"+getName()+"'");
	}
	
	public String getName() {
		return name;
	}

	/**
	 * Return the full name of the field to use in any WHERE field list of an SQL query.
	 * Generally the form `name` is sufficient, as most joins are of the form JOIN ... USING (field_list).
	 * But some fields necessitate more formatting, like constructed fields (e.g. variant_comments_public and variant_comments_private).
	 * For the few joins using JOIN ... ON, the form must be `table`.`name` to avoid ambiguities.
	 * It's only the case for tables xxx_user_annotations_variants, xxx_user_annotations_genes and xxx_user_annotations_samples.
	 * If any of those 3 tables are included in your query, you MUST set queryHasJoinON to true (then table xxx_sample_annotations MUST also be present).
	 * 
	 * @param analysis analysis in which the query will be used
	 * @param queryHasJoinON set to true if the query has JOIN ... ON in its FROM clause
	 * @return the name of the field usable in an SQL query
	 */
	public String getQueryWhereName(Analysis analysis, boolean queryHasJoinON) {
		switch(name.toLowerCase()) {
		case "variant_comments_public":
			return "`"+getTable(analysis)+"_public`.`variant_comments`";
		case "variant_comments_private":
			return "`"+getTable(analysis)+"`.`variant_comments`";
		case "gene_comments_public":
			return "`"+getTable(analysis)+"_public`.`gene_comments`";
		case "gene_comments_private":
			return "`"+getTable(analysis)+"`.`gene_comments`";
		case "sample_comments_public":
			return "`"+getTable(analysis)+"_public`.`sample_comments`";
		case "sample_comments_private":
			return "`"+getTable(analysis)+"`.`sample_comments`";
		default:
			if (queryHasJoinON && 
					(   isForeignKey(analysis.getTableUserAnnotationsVariants())	|| 
							isForeignKey(analysis.getTableUserAnnotationsSamples()) || 
							isForeignKey(analysis.getTableUserAnnotationsGenes())		)
					) {				
				return "`"+analysis.getTableSampleAnnotations()+"`.`"+name+"`";
			}else if (queryHasJoinON && 
					(		getTableSuffix().equals("_user_annotations_variants") ||
							getTableSuffix().equals("_user_annotations_genes") ||
							getTableSuffix().equals("_user_annotations_samples") )
					) {				
				return "`"+getTable(analysis)+"`.`"+name+"`";
			}else {
				return "`"+name+"`";
			}
		}
	}

	/**
	 * Return the full name of the field to use in any SELECT field list of an SQL query.
	 * Generally the form `name` is sufficient, as most joins are of the form JOIN ... USING (field_list).
	 * But some fields necessitate more formatting, like constructed fields (e.g. variant_comments_public and variant_comments_private).
	 * For the few joins using JOIN ... ON, the form must be `table`.`name` to avoid ambiguities.
	 * It's only the case for tables xxx_user_annotations_variants, xxx_user_annotations_genes and xxx_user_annotations_samples.
	 * If any of those 3 tables are included in your query, you MUST set queryHasJoinON to true (then table xxx_sample_annotations MUST also be present).
	 * 
	 * @param analysis analysis in which the query will be used
	 * @param queryHasJoinON set to true if the query has JOIN ... ON in its FROM clause
	 * @return the name of the field usable in an SQL query
	 */
	public String getQuerySelectName(Analysis analysis, boolean queryHasJoinON) {
		switch(name.toLowerCase()) {
		case "variant_comments_public":
			return "IF(`"+getTable(analysis)+"_public`.`variant_comments` IS NULL, '', `"+getTable(analysis)+"_public`.`variant_comments`) as `"+name+"`";
		case "variant_comments_private":
			return "IF(`"+getTable(analysis)+"`.`variant_comments` IS NULL, '', `"+getTable(analysis)+"`.`variant_comments`) as `"+name+"`";
		case "gene_comments_public":
			return "IF(`"+getTable(analysis)+"_public`.`gene_comments` IS NULL, '', `"+getTable(analysis)+"_public`.`gene_comments`) as `"+name+"`";
		case "gene_comments_private":
			return "IF(`"+getTable(analysis)+"`.`gene_comments` IS NULL, '', `"+getTable(analysis)+"`.`gene_comments`) as `"+name+"`";
		case "sample_comments_public":
			return "IF(`"+getTable(analysis)+"_public`.`sample_comments` IS NULL, '', `"+getTable(analysis)+"_public`.`sample_comments`) as `"+name+"`";
		case "sample_comments_private":
			return "IF(`"+getTable(analysis)+"`.`sample_comments` IS NULL, '', `"+getTable(analysis)+"`.`sample_comments`) as `"+name+"`";
		default:
			if (getDefaultValue() != null && !name.toLowerCase().equals("gene_symbol")) {
				return "IF(`"+getTable(analysis)+"`.`"+name+"` IS NULL, '"+getDefaultValue()+"', `"+getTable(analysis)+"`.`"+name+"`) as `"+name+"`";
			}else if (queryHasJoinON && 
					(   isForeignKey(analysis.getTableUserAnnotationsVariants())	|| 
							isForeignKey(analysis.getTableUserAnnotationsSamples()) || 
							isForeignKey(analysis.getTableUserAnnotationsGenes())		)
					) {				
				return "`"+analysis.getTableSampleAnnotations()+"`.`"+name+"`";
			}else if (queryHasJoinON && 
					(		getTableSuffix().equals("_user_annotations_variants") ||
							getTableSuffix().equals("_user_annotations_genes") ||
							getTableSuffix().equals("_user_annotations_samples") )
					) {				
				return "`"+getTable(analysis)+"`.`"+name+"`";
			}else {
				return "`"+name+"`";
			}
		}
	}
	
	public String getDefaultValue() {
		switch(name.toLowerCase()) {
		case "variant_comments_public":
		case "variant_comments_private":
		case "gene_comments_public":
		case "gene_comments_private":
		case "sample_comments_public":
		case "sample_comments_private":
		case "evaluation_comments":
		case "history":
		case "gene_symbol":
			return "";
		case "evaluation":
		case "num_evaluated_as_type_1":
		case "num_evaluated_as_type_2":
		case "num_evaluated_as_type_3":
		case "num_evaluated_as_type_4":
		case "num_evaluated_as_type_5":
			return "0";
		case "check_insilico":
		case "reporting":
		case "check_validated_variant":
		case "check_somatic_variant":
		case "check_segregation":
			return "NOT_CHECKED";
		default:
			return null;
		}
	}
	
	public Schema getSchema() {
		return schema;
	}

	public String getTableSuffix() {
		return tableSuffix;
	}

	public String getTable(Analysis analysis) {
		return (tableSuffix.startsWith("_")) ? analysis + tableSuffix : tableSuffix;
	}

	/**
	 * Return true if this field is a foreign key for the given table (or table suffix).
	 * Some fields are present in multiple tables, but {@link #getTable() getTable} method only gives the main table.
	 * 
	 * @param table
	 * @return
	 */
	public boolean isForeignKey(String table) {
		switch(name) {
		//Fields from _static_annotations
		case "chr":
		case "pos":
		case "length":
		case "reference":
		case "alternative":
			if (table.endsWith("_sample_annotations") ||
					table.endsWith("_custom_annotations") ||
					table.endsWith("_allele_frequencies") ||
					table.endsWith("_allele_frequencies_per_pathology") ||
					table.endsWith("_user_annotations_evaluations") ||
					table.endsWith("_user_annotations_num_evaluations") ||
					table.endsWith("_user_annotations_variants")
					) {
				return true;
			}
			return false;
		//Fields from _gene_annotations
		case "gene_symbol":
			if (table.endsWith("_sample_annotations") ||
					table.endsWith("_static_annotations") ||
					table.endsWith("_custom_annotations") ||
					table.endsWith("_user_annotations_evaluations") ||
					table.endsWith("_user_annotations_num_evaluations") ||
					table.endsWith("_user_annotations_variants") ||
					table.endsWith("_user_annotations_genes")
					) {
				return true;
			}
			return false;
		//Fields from projects
		case "project_id":
			if (table.endsWith("_sample_annotations") ||
					table.endsWith("_custom_annotations") ||
					table.endsWith("_user_annotations_num_evaluations") ||
					table.endsWith("_user_annotations_samples")
					) {
				return true;
			}
			return false;
		default:
			return false;
		}
	}
	
	/**
	 * If a table is joined using project_id, it can affect the list of samples involved in a query.
	 * _sample_annotations, _custom_annotations and _user_annotations_evaluations are not taken into account because query will generally be far too long to worth it.
	 * A sample is a minimum requirement for a query (to avoid querying the whole database),
	 * so methods that try to narrow the list of sample can use this method.
	 * 
	 * @return true if this field can affect the list of samples involved
	 */
	public boolean isSampleRelated() {
		boolean isRelated = false;
		if (tableSuffix.equals("projects")
				|| tableSuffix.equals("pathologies")
				|| tableSuffix.equals("populations")
				|| tableSuffix.equals("_user_annotations_samples")
				) {
			isRelated = true;
		}
		return isRelated;
	}
	
	public String getSqlDatatype() {
		return sqlDatatype;
	}
	
	public JSon getJSonPath() {
		return jsonPath;
	}

	public String getDescription() {
		return description;
	}

	public Annotation getAnnotationCode() {
		return annotation;
	}

	public String getAnnotationHeader() {
		return header;
	}
	
	public String[] getAnnotationHeaders() {
		if (header == null || header.length() == 0) return null;
		return header.split("&")[0].split("\\|");
	}
	
	/**
	 * Some annotations can contain multiple values, depending on another specific field.
	 * For example, in dbNSFP, the column FATHMM_score contains multiple scores separated by ";", corresponding to the column Ensembl_proteinid.
	 * 
	 * @return true if this field can contain multiple entries in the annotation source 
	 */
	public boolean hasMultipleAnnotations() {
		if (header == null || header.length() == 0) return false;
		if (header.split("&").length == 1) return false;
		return true;
	}

	/**
	 * The annotation contains multiple values, depending on another specific field.
	 * For example, in dbNSFP, the column FATHMM_score contains multiple scores separated by ";", corresponding to the column Ensembl_proteinid.
	 * In that case this method would return "Ensembl_proteinid". 
	 * 
	 * @return the field used to distinguish multiple entries, or NULL if it's a single entry field.
	 */
	public String getMultipleAnnotationsField() {
		if (!hasMultipleAnnotations()) return null;
		return header.split("&")[1].substring(1);				
	}
	
	/**
	 * The annotation contains multiple values, depending on another specific field.
	 * For example, in dbNSFP, the column FATHMM_score contains multiple scores separated by ";", corresponding to the column Ensembl_proteinid.
	 * In that case this method would return ";". 
	 * 
	 * @return the separator used to distinguish multiple entries, or NULL if it's a single entry field..
	 */
	public String getMultipleAnnotationsSeparator() {
		if (!hasMultipleAnnotations()) return null;
		return header.split("&")[1].substring(0,1);		
	}
	
	/**
	 * The annotation contains one value per alternative allele (and no value for the reference allele)
	 * @return
	 */
	public boolean needsAnnotationForEachAlternative() {
		if (header == null || header.length() == 0) return false;
		if (header.split("&").length == 1) return false;
		return header.split("&")[1].equalsIgnoreCase("A");
	}
	
	/**
	 * The annotation contains one value for the reference allele and one value per alternative allele.
	 * This field must hold the reference value.
	 * @return
	 */
	public boolean needsAnnotationForReference() {
		if (header == null || header.length() == 0) return false;
		if (header.split("&").length == 1) return false;
		return header.split("&")[1].equalsIgnoreCase("R");
	}
	
	/**
	 * The annotation contains one value for the reference allele and one value per alternative allele.
	 * This field must hold the alternative value.
	 * @return
	 */
	public boolean needsAnnotationForEachAlternativeAndReference() {
		if (header == null || header.length() == 0) return false;
		if (header.split("&").length == 1) return false;
		return header.split("&")[1].equalsIgnoreCase("RA");
	}
	
	public String getSource() {
		return source;
	}
	
	public String getDescriptionAndSource() {
		String res = description;
		if (source.length() > 0){
			if (!res.endsWith(".")) res += ".";
			res += " Source: " + source;
		}
		return res;
	}

	public String getHtmlTooltip(){
		String improvedSource = source;
		if (improvedSource.equalsIgnoreCase("ensembl") && Highlander.getCurrentAnalysis() != null) {
			improvedSource = improvedSource + " " + Highlander.getCurrentAnalysis().getReference().getEnsemblVersion();
		}
		if (this == snpeff_effect || this == snpeff_all_effects) {
			StringBuilder table = new StringBuilder();
			table.append("<tr><td><b>Effect</b></td><td><b>Sequence Ontology</b></td><td><b>Impact</b></td><td><b>Consensus prediction</b></td><td><b>Description</b></td></tr>");
			for (Effect eff : SNPEffect.Effect.values()) {
				table.append("<tr><td>"+eff.toString()+"</td><td>"+eff.getSequenceOntology()+"</td><td>"+eff.getImpact()+"</td><td style=\"text-align: center;\">"+eff.getConsensusPredictionStartingValue()+"</td><td>"+eff.getNote()+"</td></tr>");
			}
			return "<html><BODY style=\"font-family: Verdana; font-size: 7px; \"><b>["+getName()+"]</b><br>"+
					"<table border=0 cellspacing=0>"+table.toString()+"</table>"+
					((improvedSource.length() > 0)?"<br><i>Source: "+improvedSource+"</i>":"")+"</BODY></html>";
		}else {
			return "<html><b>["+getName()+"]</b><br>"+
				getDescription().replace("<", "&lt;").replace(">", "&gt;").replace("i.e.", "<i>i.e.</i>").replace("e.g.", "<i>e.g.</i>").replace(". ", ".<br>").replace(": ", ":<br>").replace(" ; ", ";<br>")+
				((improvedSource.length() > 0)?"<br><i>Source: "+improvedSource+"</i>":"")+"</html>";
		}
	}

	public Class<?> getFieldClass() {
		return fieldClass;
	}

	public String getVcfClass() {
		switch (fieldClass.getSimpleName()) {
		case "Integer":
		case "Long":
			return "Integer";
		case "Double":
		case "Float":
			return "Float";
			//Not implemented yet, now just a string field, like field=true/field=false
			//case "Boolean":
			//	return "Flag";
		case "Character":
			return "Character";
		default:
			return "String";
		}
	}

	public List<ComparisonOperator> getPossibleComparisonOperators(){
		return new ArrayList<ComparisonOperator>(comparisonOps);
	}

	public Category getCategory(){
		return category;
	}

	public int getSize(){
		return size;
	}

	public static Map<String, Integer> getDefaultWidths(){
		return new LinkedHashMap<>(fieldWidths);
	}
	
	public int getAlignment(){
		return alignment;
	}

	public static Map<String, Integer> getDefaultAlignments(){
		return new LinkedHashMap<>(fieldAlignments);
	}
	
	public int getOrdering(){
		return ordering;
	}

	public boolean hasPossibleValues(HighlanderDatabase DB, Analysis analysis){
		boolean has = false;
		try{
			try (Results res = DB.select(Schema.HIGHLANDER, 
					"SELECT * FROM " + analysis.getFromPossibleValues() 
					+ "WHERE `field` = '"+getName()+"' LIMIT 1")) {
				if (res.next()){
					has = true;
				}
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return has;
	}

	@Override
	public String toString(){
		return name;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj){
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Field))
			return false;
		return compareTo((Field)obj) == 0;
	}

	@Override
	public int compareTo(Field field){
		//If two fields have the same name in different tables, they are considered equal (it should be a e.g. foreign key)
		//return (table+"."+toString()).compareTo(field.table+"."+field.toString());
		return name.compareTo(field.name);
	}
}
