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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Parameters;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.administration.AlamutParser;
import be.uclouvain.ngs.highlander.database.DBUtils;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.Field.Aloft;
import be.uclouvain.ngs.highlander.database.Field.Annotation;
import be.uclouvain.ngs.highlander.database.Field.FitCons;
import be.uclouvain.ngs.highlander.database.Field.ImpactPrediction;
import be.uclouvain.ngs.highlander.database.Field.JSon;
import be.uclouvain.ngs.highlander.database.Field.SplicingPrediction;
import be.uclouvain.ngs.highlander.database.Field.Tag;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.DBMS;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull.VariantCaller;
import be.uclouvain.ngs.highlander.datatype.SNPEffect.Effect;
import be.uclouvain.ngs.highlander.datatype.SNPEffect.Impact;
import be.uclouvain.ngs.highlander.datatype.SNPEffect.Input;
import be.uclouvain.ngs.highlander.datatype.SNPEffect.VariantType;
import be.uclouvain.ngs.highlander.datatype.SNPEffect.Zygosity;

public class AnnotatedVariant {

	/**
	 * Use for performance optimization
	 */
	public static Map<Annotation, Long> time = new EnumMap<>(Annotation.class);
	static {
		for (Annotation a : Annotation.values()) {
			time.put(a, (long)0);
		}
	}
	
	protected final HighlanderDatabase DB;
	protected final AnalysisFull analysis;

	protected final Map<Field,Object> entries = new LinkedHashMap<Field, Object>();
	protected boolean exist = true;

	private final List<SNPEffect> other_transcripts_snpeff = new ArrayList<SNPEffect>();
	private int altIdx = -1;

	public AnnotatedVariant(AnalysisFull analysis){
		DB = Highlander.getDB();
		this.analysis = analysis;
		for (Field field : Field.getAvailableFields(analysis, false)){
			if (field.getFieldClass() == Boolean.class){
				entries.put(field, false);
			}else{
				entries.put(field, null);
			}
		}
	}

	/**
	 * copy constructor
	 * 
	 * @param annotatedVariant
	 */
	public AnnotatedVariant(AnnotatedVariant annotatedVariant) {
		this.DB = annotatedVariant.DB; 
		this.analysis = annotatedVariant.analysis;
		for (Field f : annotatedVariant.entries.keySet()) {
			this.entries.put(f, annotatedVariant.entries.get(f));
		}
		for (SNPEffect eff : annotatedVariant.other_transcripts_snpeff) {
			this.other_transcripts_snpeff.add(eff);
		}
		this.exist = annotatedVariant.exist;
		this.altIdx = annotatedVariant.altIdx;
	}
	
	public void fetchAllAnnotations(long variant_sample_id) throws Exception {
		Set<Field> fields = new HashSet<>();
		StringBuilder query = new StringBuilder();
		query.append("SELECT ");
		for (Field field : Field.getAvailableFields(analysis, false)){
			fields.add(field);
			query.append(field.getQuerySelectName(analysis, true)+", ");
		}
		query.delete(query.length()-2, query.length());
		query.append(" FROM "+analysis.getFromSampleAnnotations());		
		query.append(analysis.getJoinStaticAnnotations());		
		query.append(analysis.getJoinCustomAnnotations());		
		query.append(analysis.getJoinGeneAnnotations());		
		query.append(analysis.getJoinProjects());		
		query.append(analysis.getJoinPathologies());		
		query.append(analysis.getJoinPopulations());		
		query.append(analysis.getJoinAlleleFrequencies());		
		query.append(analysis.getJoinUserAnnotationsEvaluations());		
		query.append(analysis.getJoinUserAnnotationsNumEvaluations());		
		query.append(analysis.getJoinUserAnnotationsVariantsPrivate());		
		query.append(analysis.getJoinUserAnnotationsVariantsPublic());		
		query.append(analysis.getJoinUserAnnotationsGenesPrivate());		
		query.append(analysis.getJoinUserAnnotationsGenesPublic());		
		query.append(analysis.getJoinUserAnnotationsSamplesPrivate());		
		query.append(analysis.getJoinUserAnnotationsSamplesPublic());		
		query.append("WHERE "+Field.variant_sample_id.getQueryWhereName(analysis, true)+" = " + variant_sample_id);
		try (Results res = DB.select(Schema.HIGHLANDER, query.toString(), false)) {
			if (res.next()){
				extractFromSqlResultSet(this, res, fields);
			}else{
				throw new Exception("No "+analysis+" variant found with id " + variant_sample_id);
			}
		}
	}

	public void fetchStaticAndGeneAnnotations(long variant_static_id) throws Exception {
		Set<Field> fields = new HashSet<>();
		StringBuilder query = new StringBuilder();
		query.append("SELECT ");
		for (Field field : Field.getAvailableFields(analysis, false)){
			if (field.getTable(analysis).equals(analysis.getTableStaticAnnotations()) || field.getTable(analysis).equals(analysis.getTableGeneAnnotations())) {
				fields.add(field);
				query.append(field.getQuerySelectName(analysis, false)+", ");
			}
		}
		query.delete(query.length()-2, query.length());
		query.append(" FROM "+analysis.getFromStaticAnnotations());		
		query.append(analysis.getJoinGeneAnnotations());		
		query.append("WHERE "+Field.variant_static_id.getQueryWhereName(analysis, false)+" = " + variant_static_id);
		try (Results res = DB.select(Schema.HIGHLANDER, query.toString(), false)) {
			if (res.next()){
				extractFromSqlResultSet(this, res, fields);
			}else{
				throw new Exception("No "+analysis+" variant found with id " + variant_static_id);
			}
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void extractFromSqlResultSet(AnnotatedVariant variant, Results res, Set<Field> fields) throws Exception {
		for (Field field : fields){
			if (res.getObject(field.getName()) != null){
				if (field.getFieldClass() == String.class){
					variant.entries.put(field, res.getString(field.getName()));
				}else if (field.getFieldClass() == Integer.class){
					variant.entries.put(field, res.getInt(field.getName()));
				}else if (field.getFieldClass() == Double.class){
					variant.entries.put(field, res.getDouble(field.getName()));
				}else if (field.getFieldClass() == Long.class){
					variant.entries.put(field, res.getLong(field.getName()));
				}else if (field.getFieldClass() == Boolean.class){					
					variant.entries.put(field, res.getBoolean(field.getName()));
				}else if (field.getFieldClass() == Timestamp.class){					
					variant.entries.put(field, res.getTimestamp(field.getName()));
				}else if (field.getFieldClass().isEnum()){
					Class cls = field.getFieldClass();
					variant.entries.put(field, Enum.valueOf(cls, res.getString(field.getName())));
				}else{
					variant.entries.put(field, res.getObject(field.getName()));
				}
			}
		}
	}

	/**
	 * Set most annotations for this variant and link it to a project (sample) 
	 * 
	 * @param vcfHeader list of VCF headers
	 * @param vcfLine the full VCF line describing the variant
	 * @param projectId the id of the sample in Highlander database
	 * @param alamut a parser of the alamut file. Can be set to null if not present
	 * @return all variant parsed from the VCF line (one variant per alternative allele per gene spanning the variant)
	 * @throws Exception
	 */
	public List<AnnotatedVariant> setAllAnnotations(String[] vcfHeader, String[] vcfLine, int projectId, AlamutParser alamut, boolean silent) throws Exception {
		setProject(projectId);
		return setAllAnnotations(vcfHeader, vcfLine, (String)entries.get(Field.sample), alamut, silent);
	}

	//method without alamut parameter
	public List<AnnotatedVariant> setAllAnnotations(String[] vcfHeader, String[] vcfLine, int projectId, boolean silent) throws Exception {
		return setAllAnnotations(vcfHeader, vcfLine, projectId, null, silent);
	}

	//method without alamut parameter
	public List<AnnotatedVariant> setAllAnnotations(String[] vcfHeader, String[] vcfLine, String sample, boolean silent) throws Exception {
		return setAllAnnotations(vcfHeader, vcfLine, sample, null, silent);
	}
	
	/**
	 * Set most annotations for this variant:
	 * - VCF
	 * - Ensembl
	 * - SNPEff
	 * - DBNSFP
	 * - Annotation databases (GONL, COSMIC)
	 * - Consensus prediction
	 * - Alamut (if given)
	 * 
	 * @param vcfHeader	list of VCF headers
	 * @param vcfLine the full VCF line describing the variant
	 * @param sample name of the sample, needed if multiple sample headers are present.
	 * @param alamut a parser of the alamut file. Can be set to null if not present
	 * @return all variant parsed from the VCF line (one variant per alternative allele per gene spanning the variant)
	 * @throws Exception
	 */
	public List<AnnotatedVariant> setAllAnnotations(String[] vcfHeader, String[] vcfLine, String sample, AlamutParser alamut, boolean silent) throws Exception {
		List<AnnotatedVariant> list = new ArrayList<>();
		boolean alamutNext = false;
		int numAlleles = vcfLine[4].split(",").length;					
		for (int altIdx=0 ; altIdx < numAlleles ; altIdx++){
			AnnotatedVariant va = new AnnotatedVariant(this);
			long t = System.currentTimeMillis();
			va.setVCFLine(vcfHeader, vcfLine, altIdx, sample);
			time.put(Annotation.VCF, time.get(Annotation.VCF)+(System.currentTimeMillis()-t));
			t = System.currentTimeMillis();
			//Annotation that are independant of gene
			va.setAnnotation(Annotation.GONL);
			time.put(Annotation.GONL, time.get(Annotation.GONL)+(System.currentTimeMillis()-t));
			t = System.currentTimeMillis();
			va.setAnnotation(Annotation.GNOMAD_WES);
			time.put(Annotation.GNOMAD_WES, time.get(Annotation.GNOMAD_WES)+(System.currentTimeMillis()-t));
			t = System.currentTimeMillis();
			va.setAnnotation(Annotation.GNOMAD_WGS);
			time.put(Annotation.GNOMAD_WGS, time.get(Annotation.GNOMAD_WGS)+(System.currentTimeMillis()-t));
			t = System.currentTimeMillis();
			Variant variant = new Variant(
					(String)va.getValue(Field.chr), 
					(int)va.getValue(Field.pos), 
					(int)va.getValue(Field.length), 
					(String)va.getValue(Field.reference), 
					(String)va.getValue(Field.alternative), 
					(VariantType)va.getValue(Field.variant_type));
			Set<Gene> genesAtThisPosition = DBUtils.getGenesWithCanonicalTranscriptIntersect(analysis.getReference(), variant);
			va.entries.put(Field.num_genes, genesAtThisPosition.size());
			if (genesAtThisPosition.isEmpty()) genesAtThisPosition.add(new Gene(analysis.getReference(), variant.getChromosome(), "", "", "", "", ""));
			time.put(Annotation.HIGHLANDER, time.get(Annotation.HIGHLANDER)+(System.currentTimeMillis()-t));
			t = System.currentTimeMillis();
			for (Gene gene : genesAtThisPosition) {
				AnnotatedVariant vg = new AnnotatedVariant(va);
				if (vg.exist){
					if (gene.getGeneSymbol().length() > 0) {
						vg.setEnsembl(gene, silent);
						time.put(Annotation.ENSEMBL, time.get(Annotation.ENSEMBL)+(System.currentTimeMillis()-t));
						t = System.currentTimeMillis();
						vg.setSNPEffect(gene, silent);
						time.put(Annotation.COMPUTED, time.get(Annotation.COMPUTED)+(System.currentTimeMillis()-t));
						t = System.currentTimeMillis();
						vg.setAnnotation(Annotation.COSMIC); //Cosmic need hgvs_dna column 
						time.put(Annotation.COSMIC, time.get(Annotation.COSMIC)+(System.currentTimeMillis()-t));
						t = System.currentTimeMillis();
					}
					vg.setDBNSFP(true, true, true, silent);
					time.put(Annotation.DBNSFP, time.get(Annotation.DBNSFP)+(System.currentTimeMillis()-t));
					t = System.currentTimeMillis();
					vg.setConsensusPrediction(gene);
					time.put(Annotation.CONSENSUS, time.get(Annotation.CONSENSUS)+(System.currentTimeMillis()-t));
					t = System.currentTimeMillis();
					if (alamut != null) {
						if (alamut.checkVariantPos(vg.getValue(Field.pos).toString())){
							if (vg.getValue(Field.gene_symbol) != null){
								alamut.setGene(vg.getValue(Field.gene_symbol).toString());
								vg.setAlamut(alamut);													
							}
						}
					}
					time.put(Annotation.ALAMUT, time.get(Annotation.ALAMUT)+(System.currentTimeMillis()-t));
					t = System.currentTimeMillis();
				}
				list.add(vg);
			}
			if (alamut != null) {
				if (alamut.checkVariantPos(va.getValue(Field.pos).toString())){
					alamutNext = true;
				}				
			}
		}
		if (alamutNext && alamut.hasNext()) alamut.nextVariant();
		return list;
	}
		
	public void setAllSVAnnotations(String[] annotSVHeader, String[] annotSVLine, boolean fullOnlyFields, int projectId, boolean silent) throws Exception {
		setProject(projectId);
		setAllSVAnnotations(annotSVHeader, annotSVLine, fullOnlyFields, (String)entries.get(Field.sample), silent);
	}
	
	public void setAllSVAnnotations(String[] annotSVHeader, String[] annotSVLine, boolean fullOnlyFields, String sample, boolean silent) throws Exception {
		long t = System.currentTimeMillis();
		setAnnotSVLine(annotSVHeader, annotSVLine, fullOnlyFields, sample);
		time.put(Annotation.ANNOTSV, time.get(Annotation.ANNOTSV)+(System.currentTimeMillis()-t));
		t = System.currentTimeMillis();
		if (!fullOnlyFields && getValue(Field.gene_symbol) != null && getValue(Field.gene_symbol).toString().length() > 0) {
			Gene gene = new Gene(getValue(Field.transcript_ensembl).toString(), analysis.getReference(), getValue(Field.chr).toString(), false);
			setEnsembl(gene, silent);
			time.put(Annotation.ENSEMBL, time.get(Annotation.ENSEMBL)+(System.currentTimeMillis()-t));
			t = System.currentTimeMillis();
			setSNPEffect(gene, silent); 
			time.put(Annotation.COMPUTED, time.get(Annotation.COMPUTED)+(System.currentTimeMillis()-t));
			t = System.currentTimeMillis();
			setDBNSFP(false, false, true, silent);
			time.put(Annotation.DBNSFP, time.get(Annotation.DBNSFP)+(System.currentTimeMillis()-t));
			t = System.currentTimeMillis();
		}
		entries.put(Field.num_genes, 0);
	}
	
	/**
	 * Add an entry from a VCF line to each field linked to it
	 * 
	 * @param fields
	 * @param value
	 * @param altIdx
	 */
	public void setFieldValue(List<Field> fields, String value, int altIdx) {
		for (Field field : fields) {
			String v = value;
			if (field.needsAnnotationForReference()) {
				String[] values = value.split(",");
				v = values[0]; 
			}else if (field.needsAnnotationForEachAlternativeAndReference()) {
				String[] values = value.split(",");
				if (altIdx+1 < values.length){
					v = values[altIdx+1]; 
				}else{
					v = values[values.length-1]; 
				}
			}else if (field.needsAnnotationForEachAlternative()) {
				String[] values = value.split(",");
				if (altIdx < values.length){
					v = values[altIdx]; 
				}else{
					v = values[0]; 
				}
			}
			setFieldValue(field, v);
		}
	}
	
	/**
	 * Add an entry from a VCF line to the given field
	 *  
	 * @param field
	 * @param value
	 */
	public void setFieldValue(Field field, String value) {
		if (value == null || value.length() == 0) {
			entries.put(field, null);
		}else {
			if (field.getFieldClass() == String.class) {
				entries.put(field, value);
			}else if (field.getFieldClass() == Double.class){
				try {
					entries.put(field, Double.parseDouble(value));
				} catch(NumberFormatException e) {
					entries.put(field, null);
				}
			}else if (field.getFieldClass() == Integer.class){
				try {
					entries.put(field, Integer.parseInt(value));
				} catch(NumberFormatException e) {
					entries.put(field, null);
				}
			}else if (field.getFieldClass() == Long.class){
				try {
					entries.put(field, Long.parseLong(value));
				} catch(NumberFormatException e) {
					entries.put(field, null);
				}
			}else if (field.getFieldClass() == Timestamp.class){
				try {
					entries.put(field, Timestamp.valueOf(value));
				} catch(IllegalArgumentException e) {
					entries.put(field, null);
				}
			}else if (field.getFieldClass() == Boolean.class){
				entries.put(field, Boolean.parseBoolean(value));
			}else if (field.getFieldClass().isEnum()){
				for (Object val : field.getFieldClass().getEnumConstants()){
					if(value.equals(val.toString())) {
						entries.put(field, val);
						break;
					}
				}
			}
		}
	}
	
	public void setProject(int projectId) throws Exception {
		entries.put(Field.project_id, projectId);
		Set<Field> fields = new HashSet<>();
		StringBuilder query = new StringBuilder();
		query.append("SELECT ");
		for (Field field : Field.getAvailableFields(analysis, false)){
			if (field.getTable(analysis).equals("projects") || field.getTable(analysis).equals("pathologies") || field.getTable(analysis).equals("populations")) {
				fields.add(field);
				query.append(field.getQuerySelectName(analysis, false)+", ");
			}
		}
		query.delete(query.length()-2, query.length());
		query.append(" FROM "+analysis.getFromProjects());		
		query.append(analysis.getJoinPathologies());		
		query.append(analysis.getJoinPopulations());		
		query.append("WHERE "+Field.project_id.getQueryWhereName(analysis, false)+" = " + projectId);
		try (Results res = DB.select(Schema.HIGHLANDER, query.toString(), false)) {
			if (res.next()){
				extractFromSqlResultSet(this, res, fields);
			}else{
				throw new Exception("Project id " + projectId + " was not found in the database.");
			}
		}
	}
	
	/**
	 * Parse a VCF line to extract variant data. 
	 * 
	 * @param header list of VCF headers
	 * @param line the full VCF line describing the variant
	 * @param altIdx index of the allele. First alternative allele is 0, second alternative allele is 1, etc. Do not take into account reference allele (when ref is also in a list, we use altidx+1).
	 * @param sample name of the sample, needed if multiple sample headers are present. Can be set to null, and use sample field if {@link #setProject(int) setProject} method has been called.
	 */
	public void setVCFLine(String[] header, String[] line, int altIdx, String sample){
		this.altIdx = altIdx;
		//Build a parsing map with information from Highlander database
		Map<String,Map<String,List<Field>>> parser = new HashMap<>(); // [VCF header] -> [VCF ID] -> [all fields linked to header->ID]
		for (Field field : Field.getAvailableFields(analysis, false)) {
			if (field.getAnnotationCode() == Annotation.VCF) {
				String head = (field.getAnnotationHeaders() != null) ? field.getAnnotationHeaders()[0] : "NULL";
				if (!parser.containsKey(head)) {
					parser.put(head, new HashMap<>());
				}
				String id = (field.getAnnotationHeaders().length > 1) ? field.getAnnotationHeaders()[1] : "NULL" ;
				if (!parser.get(head).containsKey(id)) {
					parser.get(head).put(id, new ArrayList<>());
				}
				parser.get(head).get(id).add(field);
			}
		}
		//Set the sample if necessary
		if (sample == null) {
			if (entries.get(Field.sample) != null) {
				sample = entries.get(Field.sample).toString();
			}
		}
		//Count the number of columns present in the header, used if only one column last and the sample id was not used
		int headerLeft = header.length;
		int nAlt = 1;
		String[] formatFields = null;
		for (int col=0 ; col < header.length ; col++){
			if (header[col].equalsIgnoreCase("#CHROM") || header[col].equalsIgnoreCase("CHROM")){
				//Need to remove the eventual "chr"
				headerLeft--;
				setFieldValue(parser.get("CHROM").get("NULL"), line[col].replace("chr", ""), altIdx);
			}else if (header[col].equalsIgnoreCase("POS")){
				headerLeft--;
				setFieldValue(parser.get("POS").get("NULL"), line[col], altIdx);
				setRefAlt();
			}else if (header[col].equalsIgnoreCase("ID")){
				headerLeft--;
				String id = line[col];
				if (id.length() == 0 || id.equals(".")) id = null;
				setFieldValue(parser.get("ID").get("NULL"), id, altIdx);
			}else if (header[col].equalsIgnoreCase("REF")){
				headerLeft--;
				setFieldValue(parser.get("REF").get("NULL"), line[col], altIdx);
				//Compute variant_type field
				setRefAlt();
			}else if (header[col].equalsIgnoreCase("ALT")){
				headerLeft--;
				setFieldValue(parser.get("ALT").get("NULL"), line[col], altIdx);
				//Compute allele_num field
				nAlt = line[col].split(",").length;
				entries.put(Field.allele_num, nAlt+1);
				//Compute variant_type field
				setRefAlt();
				//Handle * alternative allele
				if (entries.get(Field.alternative).toString().equals("*")) exist = false;
			}else if (header[col].equalsIgnoreCase("QUAL")){
				headerLeft--;
				setFieldValue(parser.get("QUAL").get("NULL"), line[col], altIdx);
			}else if (header[col].equalsIgnoreCase("FILTER")){
				headerLeft--;
				setFieldValue(parser.get("FILTER").get("NULL"), line[col], altIdx);
			}else if (header[col].equalsIgnoreCase("INFO")){
				headerLeft--;
				String[] infos = line[col].split(";");
				for (int i=0 ; i < infos.length ; i++){
					if (infos[i].contains("=")) {
						String vcfId = infos[i].split("=")[0];
						if (vcfId.equals("ANN")) {
							//SnpEff parsing
							parseSnpEffANN(infos[i]);							
						}else if (infos[i].startsWith("REF-DP=")){
							//Lifescope
							entries.put(Field.allelic_depth_ref, Integer.parseInt(infos[i].substring("REF-DP=".length())));
							//Compute allelic proportions
							if (entries.get(Field.allelic_depth_ref) != null && entries.get(Field.allelic_depth_alt) != null){
								int allelic_depth_ref = (int)entries.get(Field.allelic_depth_ref);
								int allelic_depth_alt = (int)entries.get(Field.allelic_depth_alt);
								double allelic_depth_proportion_ref = ((allelic_depth_ref+allelic_depth_alt) > 0) ? (double)allelic_depth_ref / (allelic_depth_ref+allelic_depth_alt) : 0;
								double allelic_depth_proportion_alt = ((allelic_depth_ref+allelic_depth_alt) > 0) ? (double)allelic_depth_alt / (allelic_depth_ref+allelic_depth_alt) : 0;
								entries.put(Field.allelic_depth_proportion_ref, allelic_depth_proportion_ref); 
								entries.put(Field.allelic_depth_proportion_alt, allelic_depth_proportion_alt);
							}
						}else if (infos[i].startsWith("GAP-DP=")){
							//Lifescope
							entries.put(Field.allelic_depth_alt, Integer.parseInt(infos[i].substring("GAP-DP=".length())));
							//Compute allelic proportions
							if (entries.get(Field.allelic_depth_ref) != null && entries.get(Field.allelic_depth_alt) != null){
								int allelic_depth_ref = (int)entries.get(Field.allelic_depth_ref);
								int allelic_depth_alt = (int)entries.get(Field.allelic_depth_alt);
								double allelic_depth_proportion_ref = ((allelic_depth_ref+allelic_depth_alt) > 0) ? (double)allelic_depth_ref / (allelic_depth_ref+allelic_depth_alt) : 0;
								double allelic_depth_proportion_alt = ((allelic_depth_ref+allelic_depth_alt) > 0) ? (double)allelic_depth_alt / (allelic_depth_ref+allelic_depth_alt) : 0;
								entries.put(Field.allelic_depth_proportion_ref, allelic_depth_proportion_ref); 
								entries.put(Field.allelic_depth_proportion_alt, allelic_depth_proportion_alt);
							}
						}else if (parser.get("INFO").containsKey(vcfId)) {
							//Standard parsing described in database
							setFieldValue(parser.get("INFO").get(vcfId), infos[i].substring(vcfId.length()+1), altIdx);
						}
					}else {
						//Boolean field
						if (parser.get("INFO").containsKey(infos[i])) {
							//Standard parsing described in database
							setFieldValue(parser.get("INFO").get(infos[i]), "true", altIdx);
						}	
					}
				}
			}else if (header[col].equalsIgnoreCase("FORMAT")){
				headerLeft--;
				formatFields = line[col].split(":");
			}else if (header[col].equalsIgnoreCase(sample) || 
					header[col].replace('_', '-').equalsIgnoreCase(sample) || 
					header[col].equalsIgnoreCase("Unknown") || 
					(analysis.getVariantCaller() == VariantCaller.MUTECT && header[col].equalsIgnoreCase("TUMOR")) || 
					headerLeft == 1){
				String[] format = line[col].split(":");
				//First, get DP because it's needed to compute allelic_proprotions for LifeScope and Torrent Caller
				for (int i=0 ; i < formatFields.length ; i++){
					if (formatFields[i].equals("DP")){
						//Read depth
						String value = (format[i].equals(".")) ? "0" : format[i];
						setFieldValue(Field.read_depth, value);
						break;
					}
				}
				for (int i=0 ; i < format.length ; i++){
					if (formatFields[i].equals("AD")){
						String[] AD = format[i].split(",");
						if (!AD[0].equals(".")){
							//Allelic depth
							entries.put(Field.allelic_depth_ref, Integer.parseInt(AD[0])); 
							if (altIdx+1 < AD.length){
								entries.put(Field.allelic_depth_alt, Integer.parseInt(AD[altIdx+1])); 
							}else{								
								//LifeScope ne donne la fréquence que du premier alternatif, du coup faut déduire les autres.
								int read_depth = (entries.get(Field.read_depth) == null) ? 0 : (int)entries.get(Field.read_depth);
								int allelic_depth_ref = (entries.get(Field.allelic_depth_ref) == null) ? 0 : (int)entries.get(Field.allelic_depth_ref);
								entries.put(Field.allelic_depth_alt, read_depth - allelic_depth_ref - Integer.parseInt(AD[1])); 
							}
							double sum = 0.0;
							for (int j=0 ; j < AD.length ; j++){
								sum += Integer.parseInt(AD[j]);
							}
							//LifeScope ne donne la fréquence que du premier alternatif ...
							if (nAlt > AD.length-1) sum = (entries.get(Field.read_depth) == null) ? 1 : (int)entries.get(Field.read_depth);
							int allelic_depth_ref = (entries.get(Field.allelic_depth_ref) == null) ? 0 : (int)entries.get(Field.allelic_depth_ref);
							int allelic_depth_alt = (entries.get(Field.allelic_depth_alt) == null) ? 0 : (int)entries.get(Field.allelic_depth_alt);
							double allelic_depth_proportion_ref = (sum > 0) ? (double)allelic_depth_ref / sum : 0;
							double allelic_depth_proportion_alt = (sum > 0) ? (double)allelic_depth_alt / sum : 0;
							entries.put(Field.allelic_depth_proportion_ref, allelic_depth_proportion_ref); 
							entries.put(Field.allelic_depth_proportion_alt, allelic_depth_proportion_alt);
						}else{
							exist = false;
						}
					}else if (formatFields[i].equals("RO")){ 
						//Torrent Caller Reference allele observation count
						if (!format[i].equals(".")){
							entries.put(Field.allelic_depth_ref, Integer.parseInt(format[i])); 
							int read_depth = (entries.get(Field.read_depth) == null) ? 1 : (int)entries.get(Field.read_depth);
							entries.put(Field.allelic_depth_proportion_ref, (double)((int)entries.get(Field.allelic_depth_ref)) / Math.max(read_depth, 1)); 
						}else{
							exist = false;
						}
					}else if (formatFields[i].equals("AO")){ 
						//Torrent Caller Alternate allele observation count
						if (!format[i].equals(".")){
							entries.put(Field.allelic_depth_alt, Integer.parseInt(format[i].split(",")[altIdx])); 
							int read_depth = (entries.get(Field.read_depth) == null) ? 1 : (int)entries.get(Field.read_depth);
							entries.put(Field.allelic_depth_proportion_alt, (double)((int)entries.get(Field.allelic_depth_alt)) / Math.max(read_depth, 1)); 
						}else{
							exist = false;
						}
					}else if (formatFields[i].equals("GT")){
						//Genotype
						String genotype = format[i]; 
						entries.put(Field.zygosity, parseZygosity(genotype));
						if (!isAltInGenotype(genotype, altIdx)) exist = false;
					}else if (formatFields[i].equals("PL")){
						//Genotype likelihoods
						String[] PL = format[i].split(",");
						int offset = altIdx*3;
						if (offset >= PL.length) offset = 0;
						entries.put(Field.genotype_likelihood_hom_ref, Integer.parseInt(PL[offset])); 
						entries.put(Field.genotype_likelihood_het, Integer.parseInt(PL[offset+1])); 
						entries.put(Field.genotype_likelihood_hom_alt, Integer.parseInt(PL[offset+2])); 
					}else if (parser.get("FORMAT").containsKey(formatFields[i])) {
						//Standard parsing described in database
						setFieldValue(parser.get("FORMAT").get(formatFields[i]), format[i], altIdx);
					}
				}
			}
		}
		//For VCF with multiple samples, if alt allele is not present for current sample, 
		//if (entries.get(Field.allelic_depth_alt) != null && (int)entries.get(Field.allelic_depth_alt) == 0) exist = false;
		//--> With Haplotype caller, real variants could have 0 coverage (coverage only visible in the "bamout")
	}

	/**
	 * SnpEff VCF ANN field parser (version 4.1)
	 * 
	 * @param infos
	 */
	protected void parseSnpEffANN(String infos){
		for (String eff : infos.substring("ANN=".length()).split(",")){
			String chr = (String)entries.get(Field.chr);
			int pos = (entries.get(Field.pos) == null) ? -1 : (int)entries.get(Field.pos);
			String reference = (String)entries.get(Field.reference);
			String alternative = (String)entries.get(Field.alternative);
			VariantType variant_type = (VariantType)entries.get(Field.variant_type);
			SNPEffect effect = new SNPEffect(eff,Input.VCF_ANN, chr, pos, reference, alternative, variant_type);
			if (entries.get(Field.allele_num) == null || (int)entries.get(Field.allele_num) <= 2){
				//With only one alternative allele, all annotations are for it, easy
				addSNPEffect(effect);
			}else{
				//With multiple alternative allele, more difficult to associate each snpeff annotation to the good allele ...
				if (effect.getVariantType() == VariantType.SNV && effect.getHgvsDna().contains(">")){
					//Alternative should always correspond
					if (effect.getAlternative().equals(alternative)){
						addSNPEffect(effect);
					}
				}else if (effect.getVariantType() == VariantType.MNV && (effect.getHgvsDna().contains("ins") && effect.getHgvsDna().contains("del"))){
					//TODO LONGTERM - case like ANN=C-chr1:123456_A>T|... are not supported
					if (effect.getAlternative().equals(alternative)) addSNPEffect(effect);
				}else if (effect.getVariantType() == VariantType.DEL && effect.getHgvsDna().contains("del")){
					if (effect.getAlternative().equals("")){
						//If we have more than 1 deletion, compare the size of the HVGS notation (because nucleotides can differ for reverse genes).
						//Could not work for complex multiple deletions
						int delVcfSize = reference.substring(alternative.length()).length();
						int indexDel = effect.getHgvsDna().indexOf("del")+3;
						int delSnpEffSize = (indexDel < effect.getHgvsDna().length() && indexDel > 0) ? effect.getHgvsDna().substring(indexDel).length() : -1;
						if (delVcfSize == delSnpEffSize) addSNPEffect(effect);
					}
				}else if (effect.getVariantType() == VariantType.INS && (effect.getHgvsDna().contains("ins") || effect.getHgvsDna().contains("dup"))){
					if ((effect.getAlternative().equals(alternative.substring(reference.length())))){
						addSNPEffect(effect);
					}else{
						//If we have more than 1 insertion, compare the size of the HVGS notation (because nucleotides can differ for reverse genes).
						//Could not work for complex multiple deletions
						int insVcfSize = alternative.substring(reference.length()).length();
						int insSnpEffSize = effect.getAlternative().length();
						if (insVcfSize == insSnpEffSize) addSNPEffect(effect);
					}
				}else if (effect.getVariantType() == VariantType.SV) {
					//TODO LONGTERM - see how snpeff annotates SV (probably not with 'SV' as type ...). Not necessary as long as we use AnnotSV input, without snpeff.
				}
			}
		}
	}

	protected void addSNPEffect(SNPEffect effect){
		if (entries.get(Field.project_id) != null) effect.setProjectId((int)entries.get(Field.project_id));
		other_transcripts_snpeff.add(effect);
	}

	/**
	 * Set snpEff canonical transcript annotation, if it has been parsed in the VCF.
	 * 
	 * @param silent if true, error messages will by printed through System.err
	 */
	public void setSNPEffect(Gene gene, boolean silent){
		List<SNPEffect> canonicalSNPEffects = new ArrayList<SNPEffect>();
		for (SNPEffect effect : other_transcripts_snpeff){
			if (effect.getTrancriptId() != null && effect.getTrancriptId().equals(gene.getEnsemblTranscript())){
				canonicalSNPEffects.add(effect);
			}
		}
		if (canonicalSNPEffects.isEmpty()){
			for (SNPEffect effect : other_transcripts_snpeff){
				if (effect.getGeneId() != null && effect.getGeneId().equals(gene.getEnsemblGene())){
					canonicalSNPEffects.add(effect);
				}
			}
		}
		if (canonicalSNPEffects.isEmpty()){
			Set<String> all = new HashSet<String>();
			for (SNPEffect effect : other_transcripts_snpeff) all.add(effect.getTrancriptId());
			if (!silent) System.err.println("setSNPEffect() - Nor the canonical transcript '" + (String)entries.get(Field.transcript_ensembl) + "', nor the gene '"+(String)entries.get(Field.gene_ensembl)+"' was found in " + other_transcripts_snpeff.size() + " SNPEff effects ("+HighlanderDatabase.makeSqlList(all, String.class)+") for " + toString());
		}else{
			SNPEffect canonicalSNPEffect = null;
			if (canonicalSNPEffects.size() > 1){
				int highest = 100;
				for (SNPEffect e : canonicalSNPEffects){
					if (e.getEffect().getPriority() < highest){
						canonicalSNPEffect = e;
						highest = e.getEffect().getPriority();
					}
				}
			}else{
				canonicalSNPEffect = canonicalSNPEffects.get(0);
			}
			other_transcripts_snpeff.remove(canonicalSNPEffect);
			for (Field field : Field.getAvailableFields(analysis, false)) {
				if (field.getAnnotationCode() == Annotation.VCF && field.getAnnotationHeaders()[0].equals("INFO") && field.getAnnotationHeaders()[1].equals("ANN")) {
					switch(field.getName()) {
					case "snpeff_effect":
						entries.put(field, canonicalSNPEffect.getEffect());
						break;
					case "snpeff_all_effects":
						entries.put(field, canonicalSNPEffect.getAllEffectsAsString());
						break;
					case "snpeff_impact":
						entries.put(field, canonicalSNPEffect.getImpact());
						break;
					case "exon_intron_rank":
						entries.put(field, canonicalSNPEffect.getExonRank());
						break;
					case "exon_intron_total":
						entries.put(field, canonicalSNPEffect.getExonTotal());
						break;
					case "cdna_pos":
						entries.put(field, canonicalSNPEffect.getcDnaPosition());
						break;
					case "cdna_length":
						entries.put(field, canonicalSNPEffect.getcDnaLength());
						break;
					case "cds_pos":
						entries.put(field, canonicalSNPEffect.getCDSPosition());
						break;
					case "cds_length":
						entries.put(field, canonicalSNPEffect.getCDSLength());
						break;
					case "protein_pos":
						entries.put(field, canonicalSNPEffect.getProteinPosition());
						break;
					case "protein_length":
						entries.put(field, canonicalSNPEffect.getProteinLength());
						break;
					case "hgvs_protein":
						entries.put(field, canonicalSNPEffect.getHgvsProtein());
						break;
					case "hgvs_dna":
						entries.put(field, canonicalSNPEffect.getHgvsDna());
						break;
					case "gene_symbol":
						entries.put(field, canonicalSNPEffect.getGeneName());
						break;
					case "gene_ensembl":
						entries.put(field, canonicalSNPEffect.getGeneId());
						break;
					case "biotype":
						entries.put(field, canonicalSNPEffect.getBioType());
						break;
					case "":
						break;
					default:
						break;
					}
				}
			}
			if (entries.get(Field.hgvs_dna) == null || entries.get(Field.hgvs_dna).toString().length() == 0){
				for (SNPEffect e : canonicalSNPEffects){
					if (e.getHgvsDna() != null && e.getHgvsDna().length() > 0){
						entries.put(Field.hgvs_protein, canonicalSNPEffect.getHgvsProtein());
						entries.put(Field.hgvs_dna, canonicalSNPEffect.getHgvsDna());
					}
				}
			}
			StringBuilder sb = new StringBuilder();
			for (SNPEffect eff : other_transcripts_snpeff){
				if (eff.getGeneId() != null && eff.getGeneId().equals(gene.getEnsemblGene())){
					if (eff.getEffect() != null){
						if (eff.getTrancriptId() != null){
							sb.append(eff.getTrancriptId());
						}else{
							sb.append("?");
						}
						sb.append(":");
						sb.append(eff.getEffect());
						sb.append(";");
					}
				}
			}
			if (sb.length() > 0) sb.deleteCharAt(sb.length()-1);
			entries.put(Field.snpeff_other_transcripts, sb.toString());
		}
	}

	/**
	 * 
	 * @param genotype in the VCF in the format found in GT
	 * @param altIdx index of the allele. First alternative allele is 0, second alternative allele is 1, etc. Do not take into account reference allele (when ref is also in a list, we use altidx+1).
	 * @return true if the given alternative allele is found in the genotype 
	 */
	public static boolean isAltInGenotype(String genotype, int altidx){
		String[] refalt;
		if (genotype.contains("/")){
			//genotype unphased
			refalt = genotype.split("/");
		}else if (genotype.contains("|")){
			//genotype phased
			refalt = genotype.split("\\|");
		}else{
			//Haploid call
			refalt = new String[]{genotype};
		}
		if (refalt.length == 1){
			//Haploid call
			if (refalt[0].equals(".")){
				//call cannot be made at a given locus
				return false;
			}else if (refalt[0].equals("0")){
				return false;
			}else{
				return Integer.parseInt(refalt[0]) == altidx+1;
			}
		}else{
			for (int j=0 ; j < refalt.length ; j++){
				//call cannot be made at a given locus
				if (refalt[j].equals(".")) return false;								
			}
			int i = refalt.length-1;
			while (i >= 0){
				if (Integer.parseInt(refalt[i]) > 0 && Integer.parseInt(refalt[i]) == altidx+1){
					return true;
				}else{
					i--;
				}
			}
			return false;
		}
	}

	public static Zygosity parseZygosity(String genotype){
		String[] refalt;
		if (genotype.contains("/")){
			//genotype unphased
			refalt = genotype.split("/");
		}else if (genotype.contains("|")){
			//genotype phased
			refalt = genotype.split("\\|");
		}else{
			//Haploid call
			refalt = new String[]{genotype};
		}
		if (refalt.length == 1){
			//Haploid call
			if (refalt[0].equals(".")){
				//call cannot be made at a given locus
				return null;
			}else if (refalt[0].equals("0")){
				return Zygosity.Reference;
			}else{
				return Zygosity.Homozygous;
			}
		}else{
			for (int j=0 ; j < refalt.length ; j++){
				//call cannot be made at a given locus
				if (refalt[j].equals(".")) return null;								
			}
			boolean homo = true;
			for (int j=1 ; j < refalt.length ; j++){
				if (!refalt[j].equals(refalt[0])) homo = false;
			}
			if (homo && refalt[0].equals("0")){
				return Zygosity.Reference;
			}else if (homo) {
				//if (Integer.parseInt(refalt[0]) != 1)
				return Zygosity.Homozygous;
			}else{
				return Zygosity.Heterozygous;
			}
		}
	}

	/**
	 * Set annotations from Ensembl database
	 * 
	 * @param priorityGenes
	 * @param silent
	 */
	public void setEnsembl(Gene gene, boolean silent){
		setValue(Field.gene_ensembl, gene.getEnsemblGene());
		setValue(Field.gene_symbol, gene.getGeneSymbol());
		setValue(Field.transcript_ensembl, gene.getEnsemblTranscript());
		setValue(Field.transcript_refseq_mrna, gene.getRefSeqTranscript());
		if (DB.hasSchema(analysis.getReference(), Schema.ENSEMBL)){
			try{
				entries.put(Field.transcript_uniprot_id, DBUtils.getAccessionUniprot(analysis.getReference(), gene.getEnsemblTranscript()));
			}catch (Exception ex){
				Tools.exception(ex);
				if (!silent) System.err.println("Cannot get transcript_uniprot_id for gene " + gene.getEnsemblGene() + " in variant " + toString());
			}
			try{
				entries.put(Field.transcript_refseq_prot, DBUtils.getAccessionRefSeqProt(analysis.getReference(), gene.getEnsemblTranscript()));
			}catch (Exception ex){
				Tools.exception(ex);
				if (!silent) System.err.println("Cannot get transcript_refseq_prot for gene " + gene.getEnsemblGene() + " in variant " + toString());
			}
		}
	}

	/**
	 * Set annotations from DBNSFP database version 4.1
	 * 
	 * @param snv true to get annotations for SNV (tables chromosome_?) 
	 * @param splicing true to get annotations from dbscSNV (tables dbscSNV_chr?)
	 * @param gene true to get annotations from genes (table genes)
	 * @param silent
	 */
	public void setDBNSFP(boolean snv, boolean splicing, boolean gene, boolean silent){
		if (DB.hasSchema(analysis.getReference(), Schema.DBNSFP)){
			try{
				String gene_symbol = (String)entries.get(Field.gene_symbol);
				String gene_ensembl = (String)entries.get(Field.gene_ensembl);
				String transcript_ensembl = (String)entries.get(Field.transcript_ensembl);
				String chr = (String)entries.get(Field.chr);
				int pos = (entries.get(Field.pos) == null) ? -1 : (int)entries.get(Field.pos);
				String alternative = (String)entries.get(Field.alternative);
				VariantType variant_type = (VariantType)entries.get(Field.variant_type);
				Map<String, Map<String,Field>> parser = new HashMap<>();
				for (Field field : Field.getAvailableFields(analysis, false)) {
					if (field.getAnnotationCode() == Annotation.DBNSFP) {						
						String table = (field.getAnnotationHeaders() != null) ? field.getAnnotationHeaders()[0] : "unknown_table";
						table = table.replace("[chr]", chr);
						boolean todo = true;
						if (table.startsWith("chromosome") && !snv) todo = false;
						if (table.startsWith("dbscSNV") && !splicing) todo = false;
						if (table.startsWith("genes") && !gene) todo = false;
						if (todo){
							if (!parser.containsKey(table)) {
								parser.put(table, new HashMap<>());
							}
							String column = (field.getAnnotationHeaders() != null && field.getAnnotationHeaders().length > 1) ? field.getAnnotationHeaders()[1] : "unknown_column" ;
							parser.get(table).put(column, field);							
						}
					}
				}
				for (String table : parser.keySet()) {
					String query = "SELECT * FROM `"+table+"` WHERE ";
					if (table.startsWith("genes")) {
						query += "`Gene_name` = '"+gene_symbol+"' OR `Ensembl_gene` = '"+gene_ensembl+"'";
					}else {
						if (analysis.getReference().getGenomeVersion() == 37) {
							query += "`hg19_pos` = '" + pos + "'";
						}else if (analysis.getReference().getGenomeVersion() == 36){
							query += "`hg18_pos` = '" + pos + "'";
						}else {
							query += "`pos` = " + pos;
						}
						query += " AND `alt` = '"+alternative+"'";
					}
					if (table.startsWith("chromosome")) {
						query += " AND (INSTR(`genename`, '"+gene_symbol+"') OR INSTR(`Ensembl_geneid`, '"+gene_ensembl+"'))";
					}else if (table.startsWith("dbscSNV")) {
						query += " AND (INSTR(`RefSeq_gene`, '"+gene_symbol+"') OR INSTR(`Ensembl_gene`, '"+gene_ensembl+"'))";
					}
					if ((variant_type == VariantType.SNV || table.startsWith("genes")) && DB.getAvailableTables(analysis.getReference(), Schema.DBNSFP).contains(table)){
						int transcriptIndex = -1;
						int MutationTasterIndex = 0;
						try (Results res = DB.select(analysis.getReference(), Schema.DBNSFP, query)) {
							if (res.next()){
								if (table.startsWith("chromosome")) {
									//Find index of canonical transcript (since dbNSFP 4.1, it seems easier, # of Ensembl protein ids and Uniprot ids match # of Ensembl transcripts, and all scores also match that same number 
									String[] transcriptIds = res.getString("Ensembl_transcriptid").split(";");
									boolean found = false;
									//First check if our transcript_ensembl is found in the list of dbNSFP
									for (int i=0 ; i < transcriptIds.length ; i++) {
										String transcript = transcriptIds[i];
										if (transcript.equals(transcript_ensembl)) {
											transcriptIndex = i;
											found = true;
											break;
										}
									}
									if (!found) {
										if (!silent) System.err.println("Ensembl transcript '"+transcript_ensembl+"' present in VCF was not found in dbNSFP for gene '"+gene_symbol+"' / '"+gene_ensembl+"' on variant " + chr + ":" + pos);
										//dbNSFP probably a version Ensembl different of the one linked to this analysis. Use the canonical given by VEP, it should match
										for (int i=0 ; i < res.getString("VEP_canonical").split(";").length ; i++) {
											String canonical = res.getString("VEP_canonical").split(";")[i];
											if (canonical.equals("YES")) {
												transcriptIndex = i;
												found = true;
												break;
											}
										}
									}
									if (!found) {
										if (!silent) System.err.println("No canonical transcript was found in dbNSFP for gene '"+gene_symbol+"' / '"+gene_ensembl+"' on variant " + chr + ":" + pos);
										if (!silent) System.err.println("Using transcript "+transcriptIds[0]+" instead of "+transcript_ensembl+" for gene '"+gene_symbol+"' / '"+gene_ensembl+"' on variant " + chr + ":" + pos);
										transcriptIndex = 0;
									}
								}
								//Mutation taster is only one that have different transcript number and/or order
								if (parser.get(table).containsKey("MutationTaster_score") || parser.get(table).containsKey("MutationTaster_AAE")) {
									if (res.getString("MutationTaster_AAE").contains(";")) {
										//try to find the one that match amino acid substitution (now always OK, but generally similar score, and anyway the best we can do with what we have)
										String canonicAAE = res.getString("aaref") + res.getString("aapos").split(";")[transcriptIndex] + res.getString("aaalt");
										String[] aae = res.getString("MutationTaster_AAE").split(";");
										for (int i=0 ; i < aae.length ; i++) {
											if (aae[i].equals(canonicAAE)) {
												MutationTasterIndex = i;
												break;
											}
										}
									}
								}
								for (String column : parser.get(table).keySet()) {
									Field field = parser.get(table).get(column);
									if (entries.get(field) == null &&
											//Predictions computed when fetching score
											!field.equals(Field.splicing_ada_pred) &&
											!field.equals(Field.splicing_rf_pred)
											) {
										//Basic field
										String value = res.getString(column);
										if (column.equals("CADD_raw") || column.equals("CADD_raw_rankscore") ||	column.equals("CADD_phred")) {
											//CADD scores for functional prediction of a SNP using the hg19 model.
											if (analysis.getReference().getGenomeVersion() == 37) {
												value = res.getString(column+"_hg19");
											}											
										}
										if (value != null) {
											value = value.trim();
											if (field.hasMultipleAnnotations()) {
												if (field.getMultipleAnnotationsField().equalsIgnoreCase("MutationTaster")) {
													value = value.split(field.getMultipleAnnotationsSeparator())[MutationTasterIndex];
												}else {
													value = value.split(field.getMultipleAnnotationsSeparator())[transcriptIndex];
												}
											}
											if (!value.equals(".")) {
												switch(column) {
												case "MutationTaster_pred":
													if (value.equals("A")) entries.put(field, ImpactPrediction.DAMAGING); 
													else if (value.equals("D")) entries.put(field, ImpactPrediction.DAMAGING); 
													else if (value.equals("N")) entries.put(field, ImpactPrediction.TOLERATED); 
													else if (value.equals("P")) entries.put(field, ImpactPrediction.TOLERATED); 
													break;
												case "FATHMM_pred":
												case "SIFT_pred":
												case "SIFT4G_pred":
												case "M-CAP_pred":
												case "LIST-S2_pred":
												case "DEOGEN2_pred":
												case "ClinPred_pred":
												case "BayesDel_addAF_pred":
												case "BayesDel_noAF_pred":
												case "PrimateAI_pred":
												case "MetaSVM_pred":
												case "MetaLR_pred":
													if (value.equals("D")) entries.put(field, ImpactPrediction.DAMAGING); 
													else if (value.equals("T")) entries.put(field, ImpactPrediction.TOLERATED); 
													break;
												case "fathmm-XF_coding_pred":
												case "fathmm-MKL_coding_pred":
												case "PROVEAN_pred":
													if (value.equals("D")) entries.put(field, ImpactPrediction.DAMAGING); 
													else if (value.equals("N")) entries.put(field, ImpactPrediction.TOLERATED); 
													break;
												case "Polyphen2_HDIV_pred":
												case "Polyphen2_HVAR_pred":
													if (value.equals("D")) entries.put(field, ImpactPrediction.DAMAGING);
													else if (value.equals("P")) entries.put(field, ImpactPrediction.DAMAGING); 
													else if (value.equals("B")) entries.put(field, ImpactPrediction.TOLERATED); 													
													break;
												case "MutationAssessor_pred":
													if (value.equals("H")) entries.put(field, ImpactPrediction.DAMAGING); 
													else if (value.equals("M")) entries.put(field, ImpactPrediction.DAMAGING); 
													else if (value.equals("L")) entries.put(field, ImpactPrediction.TOLERATED); 
													else if (value.equals("N")) entries.put(field, ImpactPrediction.TOLERATED); 
													break;
												case "LRT_pred":
													if (value.equals("D")) entries.put(field, ImpactPrediction.DAMAGING); 
													else if (value.equals("N")) entries.put(field, ImpactPrediction.TOLERATED); 
													else if (value.equals("U")) entries.put(field, null); 
													break;
												case "integrated_confidence_value":
												case "GM12878_confidence_value":
												case "H1-hESC_confidence_value":
												case "HUVEC_confidence_value":
													if (value.equalsIgnoreCase("0")) entries.put(field, FitCons.HIGHLY_SIGNIFICANT); 
													else if (value.equalsIgnoreCase("1")) entries.put(field, FitCons.SIGNIFICANT); 
													else if (value.equalsIgnoreCase("2")) entries.put(field, FitCons.INFORMATIVE); 
													else if (value.equalsIgnoreCase("3")) entries.put(field, FitCons.OTHER); 
													break;
												case "Aloft_pred":
													if (value.equalsIgnoreCase("DOMINANT")) entries.put(field, Aloft.DOMINANT); 
													else if (value.equalsIgnoreCase("RECESSIVE")) entries.put(field, Aloft.RECESSIVE); 
													else if (value.equalsIgnoreCase("TOLERANT")) entries.put(field, Aloft.TOLERANT); 
													break;
												case "ada_score":
													double ada_score = Double.parseDouble(value);
													entries.put(field, ada_score);
													entries.put(Field.splicing_ada_pred, (ada_score > 0.6) ? SplicingPrediction.AFFECTING_SPLICING : SplicingPrediction.SPLICING_UNAFFECTED);
													break;													
												case "rf_score":
													double rf_score = Double.parseDouble(value);
													entries.put(field, rf_score);
													entries.put(Field.splicing_rf_pred, (rf_score > 0.6) ? SplicingPrediction.AFFECTING_SPLICING : SplicingPrediction.SPLICING_UNAFFECTED);
													break;
												case "HIPred":
												case "is_scSNV_RefSeq":
												case "is_scSNV_Ensembl":
												case "ExAC_cnv_flag":
													//Convert Y/N to boolean
													entries.put(field, (value.toUpperCase().equals("Y")) ? true : false);
													break;
												default:
													setFieldValue(parser.get(table).get(column), value);
													break;
												}
											}
										}
									}
								}
							}else {
								if (!silent) {
									if (table.startsWith("genes")) {
										System.err.println("Gene '"+gene_symbol+"' / '"+gene_ensembl+"' does not exists in DBNSFP genes");
									}else {
										if (analysis.getReference().getGenomeVersion() == 37) {
											System.err.println("GRCh37 position " + chr + ":" + pos + " ("+getValue(Field.snpeff_effect)+") does not exists in DBNSFP (or has been deleted in GRCh38), annotation skipped for this position.");
										}else {
											System.err.println("Position " + chr + ":" + pos + " (" + getValue(Field.snpeff_effect) + ") does not exists in DBNSFP for gene '"+gene_symbol+"' / '"+gene_ensembl+"'");
										}										
									}
								}
							}
						}
					}
				}
			}catch (Exception ex){
				Tools.exception(ex);
			}
		}
	}

	/**
	 * Set annotations from a 'small' annotation database (few columns to fetch, few treatment to process the annotation).
	 * This method only works for databases that just need the table and column names to fetch, and few or no treatment of the results.
	 * 
	 * @param annotation GONL, COSMIC (other annotations have their own specialized method)
	 */
	public void setAnnotation(Annotation annotation){
		if (annotation.hasDatabaseSchema()) {
			Schema schema = annotation.getDatabaseSchema();
			if (DB.hasSchema(analysis.getReference(), schema)){
				try{
					String chr = (String)entries.get(Field.chr);
					Map<String, Map<String,Field>> parser = new HashMap<>();
					for (Field field : Field.getAvailableFields(analysis, false)) {
						if (field.getAnnotationCode() == annotation) {
							String table = (field.getAnnotationHeaders() != null) ? field.getAnnotationHeaders()[0] : "unknown_table";
							table = table.replace("[chr]", chr);
							if (!parser.containsKey(table)) {
								parser.put(table, new HashMap<>());
							}
							String column = (field.getAnnotationHeaders().length > 1) ? field.getAnnotationHeaders()[1] : "unknown_column" ;
							parser.get(table).put(column, field);
						}
					}
					for (String table : parser.keySet()) {
						if (DB.getAvailableTables(analysis.getReference(), schema).contains(table)){
							StringBuilder select = new StringBuilder();
							for (String column : parser.get(table).keySet()) {
								if (!column.equalsIgnoreCase("unknown_column")) {
									select.append("`"+column+"`, ");
								}
							}
							select.deleteCharAt(select.length()-1);
							select.deleteCharAt(select.length()-1);
							String where = "";
							switch(annotation) {
							case GONL:
							case GNOMAD_WES:
							case GNOMAD_WGS:
								int pos = (entries.get(Field.pos) == null) ? -1 : (int)entries.get(Field.pos);
								String reference = (String)entries.get(Field.reference);
								String alternative = (String)entries.get(Field.alternative);
								where = "`pos` = " + pos	+ " AND `reference` = '"+reference+"' AND `alternative` = '"+alternative+"'";
								break;
							case COSMIC:
								int start = (entries.get(Field.pos) == null) ? -1 : (int)entries.get(Field.pos);
								int length = (entries.get(Field.length) == null) ? -1 : (int)entries.get(Field.length);
								int stop = start + length - 1;
								String mutation_cds = (String)entries.get(Field.hgvs_dna);
								String mutation_genome_position = chr+":"+start+"-"+stop;
								where = "`mutation_cds` = '" + mutation_cds	+ "' AND `mutation_genome_position` = '"+mutation_genome_position+"'";
								break;
							default:
								System.err.println("Annotation source " + annotation + " is NOT supported by this method, annotation NOT done.");
								return;
							}
							try (Results res = DB.select(analysis.getReference(), schema, "SELECT "+select.toString()+" FROM `"+table+"` "+ "WHERE " + where)) {
								if (res.next()){
									for (String column : parser.get(table).keySet()) {
										Field field = parser.get(table).get(column);
										if (entries.get(field) == null) {
											String value = res.getString(column);
											setFieldValue(field, value);
										}
									}
								}
							}
						}
					}
				}catch (Exception ex){
					Tools.exception(ex);
				}
			}
		}
	}

	//TODO mettre le nom des champs dans les settings globaux, au cas où ils changeraient.
	//TODO tester si field est null avant setFieldValue, au cas ces champs seraient manquant en db, et envoyer un message d'erreur correct (champ doit être associé à l'analyse)
	public void setAnnotSVLine(String[] header, String[] line, boolean fullOnlyFields, String sample){
		Map<String,Field> parser = new HashMap<>();
		for (Field field : Field.getAvailableFields(analysis, false)) {
			if (field.getAnnotationCode() == Annotation.ANNOTSV) {						
				String annotSVType = (field.getAnnotationHeaders() != null) ? field.getAnnotationHeaders()[0] : "split";
				if ((fullOnlyFields && annotSVType.equalsIgnoreCase("full")) || (!fullOnlyFields)) {
					String column =  (field.getAnnotationHeaders() != null && field.getAnnotationHeaders().length > 1) ? field.getAnnotationHeaders()[1] : "unknown_column" ;
					column = column.replace("[sample]", sample);
					parser.put(column, field);
				}
			}
		}
		List<String> vcfHeaders = new ArrayList<>();
		List<String> vcfLine = new ArrayList<>();
		entries.put(Field.variant_type, VariantType.SV);
		for (int col=0 ; col < header.length ; col++){
			if (!fullOnlyFields && header[col].equalsIgnoreCase("SV chrom")) {
				vcfHeaders.add("CHROM");
				vcfLine.add(line[col]);
			}else if (!fullOnlyFields && header[col].equalsIgnoreCase("SV start")) {
				vcfHeaders.add("POS");
				vcfLine.add(line[col]);				
				setFieldValue(parser.get(header[col]), line[col]);
			}else if (!fullOnlyFields && ( 
					header[col].equalsIgnoreCase("REF") 
					|| header[col].equalsIgnoreCase("ALT")
					|| header[col].equalsIgnoreCase("QUAL")
					|| header[col].equalsIgnoreCase("FILTER")
					|| header[col].equalsIgnoreCase("INFO")
					|| header[col].equalsIgnoreCase("FORMAT")
					|| header[col].equalsIgnoreCase(sample)
					)) {				
				vcfHeaders.add(header[col]);
				vcfLine.add(line[col]);
			}else if (!fullOnlyFields && header[col].equalsIgnoreCase("SV length")) {		
				setFieldValue(Field.length, line[col]);
			}else if (!fullOnlyFields && header[col].equalsIgnoreCase("Gene name")) {
				String value = line[col];
				if (value == null || value.length() == 0) setFieldValue(Field.gene_symbol, "");
				else setFieldValue(Field.gene_symbol, line[col]);
			}else if (!fullOnlyFields && header[col].equalsIgnoreCase("tx")) {
				setFieldValue(Field.transcript_ensembl, line[col]);
			}else if (parser.containsKey(header[col])) {
				String value = line[col];
				if (value.equalsIgnoreCase("yes")) value = "true";
				if (value.equalsIgnoreCase("-1")) value = "";
				if (value.equalsIgnoreCase("NA")) value = "";
				if (value != null && value.length() > 0) setFieldValue(parser.get(header[col]), value);
			}
		}
		if (!fullOnlyFields) setVCFLine(vcfHeaders.toArray(new String[0]), vcfLine.toArray(new String[0]), 0, sample);
	}
	
	/**
	 * Set Alamut annotations from the TSV file it produces
	 *  
	 * @param alamut parser of the Alamut file.
	 * @throws Exception
	 */
	public void setAlamut(AlamutParser alamut) throws Exception {
		for (Field field : Field.getAvailableFields(analysis, false)) {
			if (field.getAnnotationCode() == Annotation.ALAMUT) {
				String header = field.toString().replaceAll("_", "");
				if (field.getName().equals("dbsnp_maf")) header = "rsMAF";
				String alamutString = alamut.extract(header, altIdx);
				if (alamutString == null || alamutString.length() == 0){
					entries.put(field, null);
				}else{
					setFieldValue(field, alamutString);
				}
			}
		} 
	}

	/**
	 * Compute length and variant_type depending on reference and alternative values.
	 * Also correct reference, alternative and position if necessary.
	 * Correction include VCF line with multiple alternative and normally multiple references.
	 * As a VCF can only have one reference, it must be a reference that matches all alternatives.
	 * But as Highlander split different alternatives in different variants, it creates "wrong" (non canonical) ref/alt.
	 * Example: ref / alt = CTTT / C,CTT. Deletion of TTT and of T at the same pos. CTTT>C is OK but CTTT>CTT must become CT>C.
	 * Same kind of behaviour happens a lot with Ion Torrent VCF, sometimes with no apparent reason.
	 * 
	 */
	public void setRefAlt(){
		if (entries.get(Field.reference) != null && 
				entries.get(Field.alternative) != null &&
				entries.get(Field.pos) != null) {
			VariantType variant_type;
			String reference = (String)entries.get(Field.reference);
			String alternative = (String)entries.get(Field.alternative);
			int pos = (entries.get(Field.pos) == null) ? -1 : (int)entries.get(Field.pos);
			if (reference.equalsIgnoreCase("N") || alternative.contains("<")) {
				variant_type = VariantType.SV;
			}else if (reference.length() == 1 && alternative.length() == 1){
				variant_type = VariantType.SNV;
			}else if (reference.length() > alternative.length()){
				if (reference.length() > 300) {
					//variant won't fit in VARCHAR(300) in database, transform it into a SV
					variant_type = VariantType.SV;
					entries.put(Field.length, Math.abs(reference.length()-alternative.length()));
					entries.put(Field.reference, "N");
					entries.put(Field.alternative, "<DEL>");
				}else {
					variant_type = VariantType.DEL;
					if (alternative.length() > 1) {
						//Set homopolymer deletion to the leftmost nucleotide, so start by fixing "end"
						int endRef = reference.length();
						int endAlt = alternative.length();
						while (endAlt > 1 && reference.charAt(endRef-1) == alternative.charAt(endAlt-1)) {
							endRef--;
							endAlt--;
						}
						int start = 0;
						while (start < endAlt && reference.charAt(start) == alternative.charAt(start)) {
							start++;
						}
						start--; //must keep 1 nucleotide not deleted
						reference = reference.substring(start, endRef);
						alternative = alternative.substring(start, endAlt);
						pos = pos + start;
						entries.put(Field.reference, reference); 
						entries.put(Field.alternative, alternative); 
						entries.put(Field.pos, pos);
					}					
				}
			}else if (reference.length() < alternative.length()){
				if (alternative.length() > 500) {
					//variant won't fit in VARCHAR(500) in database, transform it into a SV
					variant_type = VariantType.SV;
					entries.put(Field.length, Math.abs(reference.length()-alternative.length()));
					entries.put(Field.reference, "N");
					entries.put(Field.alternative, "<INS>");
				}else {
					variant_type = VariantType.INS;
					if (reference.length() > 1) {
						//Set homopolymer insertion to the leftmost nucleotide, so start by fixing "end"
						int endRef = reference.length();
						int endAlt = alternative.length();
						while (endRef > 1 && reference.charAt(endRef-1) == alternative.charAt(endAlt-1)) {
							endRef--;
							endAlt--;
						}
						int start = 0;
						while (start < endRef && reference.charAt(start) == alternative.charAt(start)) {
							start++;
						}
						start--; //must keep 1 nucleotide before the insertion
						reference = reference.substring(start, endRef);
						alternative = alternative.substring(start, endAlt);
						pos = pos + start;
						entries.put(Field.reference, reference); 
						entries.put(Field.alternative, alternative); 
						entries.put(Field.pos, pos);
					}
				}
			}else{
				//ref == alt but it's not always MNV, with multiple alt in one VCF line, it can be more complex
				int start = 0;
				while (start < reference.length() && reference.charAt(start) == alternative.charAt(start)) {
					start++;
				}
				int end = reference.length();
				while (end > start && reference.charAt(end-1) == alternative.charAt(end-1)) {
					end--;
				}
				if (start == 0 && end == reference.length()) {
					//Normal MNV
					variant_type = VariantType.MNV;		
				}else if (start < end) {
					//Must trim ref and/or alt
					reference = reference.substring(start, end);
					alternative = alternative.substring(start, end);
					pos = pos + start;
					entries.put(Field.reference, reference); 
					entries.put(Field.alternative, alternative); 
					entries.put(Field.pos, pos); 
					if (reference.length() == 1) {
						variant_type = VariantType.SNV;
					}else {
						variant_type = VariantType.MNV;
					}
				}else{
					//This is NOT a variant, both reference and alternative are identical.
					variant_type = VariantType.MNV;
					exist = false;
				}
			}
			entries.put(Field.variant_type, variant_type);
			switch(variant_type) {
			case SV:
				//length has been set before (parsed in the VCF or AnnotSV, set here if ref/alt > 300/500 bp)
				break;
			case SNV:
			case MNV:
				entries.put(Field.length, reference.length());
				break;
			case DEL:
			case INS:
			default:
				entries.put(Field.length, Math.abs(reference.length()-alternative.length()));
				break;
			}
		}
	}

	/**
		 * Consensus between prediction of all available software. 
		 * The base score reflects the number software that predict the variant to be damaging: 
		 * Each of the following software having a DAMAGING prediction add +1:
		 * Mutation Taster, FATHMM, FATHMM-XF, Polyphen2 (HDIV), Provean, SIFT4G, Mutation Assessor, MCAP, LRT, Lists2, Deogen, ClinPred, BayesDel (with MaxMAF), PrimateAI and MetaSVM.
		 * Each of the following scores add +1 when above a certain threshold:
		 * CADD phred > 20, VEST > 0.5, REVEL > 0.5, MVP > 0.75 and MutPred > 0.75 
		 * So max 20 if all software agree. 
		 * If the variant could affect splicing, a +1 or +2 could be added to the base score, if ada and/or rf predictions are AFFECTING_SPLICING. 
		 * To this, value is added depending on certain variant impact (annotations from SnpEff):
		 * +200 if any prediction indicates that splicing is affected.
		 * +300 for SPLICE_SITE_ACCEPTOR, SPLICE_SITE_DONOR, the 2 first or 2 last positions of an exon, the 3rd/4th/5th positions in 3' intron of an exon (before the STOP).
		 * +400 for FRAME_SHIFT, STOP_GAINED, STOP_LOST, START_LOST or RARE_AMINO_ACID.
		 * +500 for high impact structural variants.
		 * So, filtering on consensus_prediction > 0 should yield variants potentially damaging.
		 * Choosing a higher value like consensus_prediction > 5 should yield variants probably damaging.
	 *    
	 */
	public void setConsensusPrediction(Gene gene){
		SplicingPrediction splicing_ada_pred = (SplicingPrediction)entries.get(Field.splicing_ada_pred);
		SplicingPrediction splicing_rf_pred = (SplicingPrediction)entries.get(Field.splicing_rf_pred);
		Aloft aloft_pred = (Aloft)entries.get(Field.aloft_pred);
		double cadd_phred_score = (entries.get(Field.cadd_phred) == null) ? -1 : (double)entries.get(Field.cadd_phred);
		double vest_score = (entries.get(Field.vest_score) == null) ? -1 : (double)entries.get(Field.vest_score);
		double revel_score = (entries.get(Field.revel_score) == null) ? -1 : (double)entries.get(Field.revel_score);
		double mvp_score = (entries.get(Field.mvp_score) == null) ? -1 : (double)entries.get(Field.mvp_score);
		double mutpred_score = (entries.get(Field.mutpred_score) == null) ? -1 : (double)entries.get(Field.mutpred_score);
		int consensus_prediction = 0;
		if ((Impact)entries.get(Field.snpeff_impact) != null) {
			consensus_prediction = ((Effect)entries.get(Field.snpeff_effect)).getConsensusPredictionStartingValue();
		}
		if (Field.snpeff_all_effects != null && entries.get(Field.snpeff_all_effects) != null && consensus_prediction < 300) {
			boolean isSpliceSiteRegion = false;
			for (String ef : entries.get(Field.snpeff_all_effects).toString().split("&")) {
				switch (Effect.valueOf(ef).getGeneRegion()) {
				case SPLICE_SITE_REGION:
					isSpliceSiteRegion = true;
					break;
				default:
					break;
				}
			}
			if (isSpliceSiteRegion) {
				int startPos = (entries.get(Field.pos) == null) ? -1 : (int)entries.get(Field.pos);
				int length = (entries.get(Field.length) == null) ? 0 : (int)entries.get(Field.length);
				try {
					gene.setExonDataFromEnsembl();
					for (int i=0 ; i < length && consensus_prediction != 300 ; i++ ) {
						int pos = startPos+i;
						int exonNum = gene.getExonNum(pos, false);
						if (exonNum != -1) {
							if (pos == gene.getExonStart(exonNum) || pos == gene.getExonStart(exonNum)+1 
									|| pos == gene.getExonEnd(exonNum) || pos == gene.getExonEnd(exonNum)-1){
								consensus_prediction = 300;
							}
						}else {
							//with extendsToSpliceSite = true, we will be in an exon +/- 10, which are the only positions of interest here
							int nearestExon = gene.getExonNum(pos, true);
							if (nearestExon != -1) {
								if (gene.isStrandPositive()) {
									if (pos < gene.getTranslationEnd()) {
										if (pos == gene.getExonEnd(nearestExon)+3 || pos == gene.getExonEnd(nearestExon)+4 || pos == gene.getExonEnd(nearestExon)+5) {
											consensus_prediction = 300;
										}
									}
								}else {
									if (pos > gene.getTranslationStart()) {
										if (pos == gene.getExonStart(nearestExon)-3 || pos == gene.getExonStart(nearestExon)-4 || pos == gene.getExonStart(nearestExon)-5) {
											consensus_prediction = 300;
										}
									}
								}
							}
						}
					}
				}catch(Exception ex) {
					Tools.exception(ex);
				}
			}
		}
		for (Field field : Field.getAvailableFields(false)) {
			if (field.hasTag(Tag.IMPACT_PREDICTION)) {
				ImpactPrediction impact = (ImpactPrediction)entries.get(field);
				if (impact != null && impact == ImpactPrediction.DAMAGING) consensus_prediction++;
			}
		}		
		if (aloft_pred != null && aloft_pred != Aloft.TOLERANT) consensus_prediction++;
		if (cadd_phred_score > 20) consensus_prediction++;
		if (vest_score > 0.5) consensus_prediction++;
		if (revel_score > 0.5) consensus_prediction++;
		if (mvp_score > 0.75) consensus_prediction++;
		if (mutpred_score > 0.75) consensus_prediction++;
		if (splicing_ada_pred != null && splicing_ada_pred == SplicingPrediction.AFFECTING_SPLICING) {
			if (consensus_prediction < 200) consensus_prediction += 200;
			consensus_prediction++;
		}
		if (splicing_rf_pred != null && splicing_rf_pred == SplicingPrediction.AFFECTING_SPLICING)  {
			if (consensus_prediction < 200) consensus_prediction += 200;
			consensus_prediction++;
		}
		entries.put(Field.consensus_prediction, consensus_prediction);
	}

	public void setValue(Field field, Object value) {
		entries.put(field, value);
	}
	
	public Object getValue(Field e){
		return entries.get(e);
	}

	public AnalysisFull getAnalysis(){
		return analysis;
	}

	public String toString(){
		return entries.get(Field.chr) + "-" + String.format("%010d", entries.get(Field.pos)) + "-" + entries.get(Field.reference) + "-" + entries.get(Field.alternative);
	}

	/**
	 * If no data have been found during the parsing of the sample's format fields, this method will return false.
	 * It happens when a VCF contains more than one sample, and when one of the sample doesn't have the variant. 
	 * 
	 * @return false if the format column does not contain data, true otherwise
	 */
	public boolean exist(){
		return exist;
	}

	public boolean affectsGene() {
		return (getValue(Field.gene_symbol) != null && getValue(Field.gene_symbol).toString().length() > 0);
	}
	
	public int getAlternativeIndexInVCF() throws Exception {
		if (altIdx == -1) throw new Exception("AnnotatedVariant " + toString() + " has not beed parsed from a VCF, getAlternativeIndexInVCF() cannot be used.");
		return altIdx;
	}
	
	public String getInsertionString(DBMS dbms, String table){
		String nullStr = HighlanderDatabase.getNullString(dbms);
		StringBuilder sb = new StringBuilder();
		for (Field f : Field.getAvailableFields(analysis, false)){
			if ((f.getTable(analysis).equalsIgnoreCase(table) || f.isForeignKey(table))
					&& !(f.equals(Field.variant_sample_id) || f.equals(Field.variant_static_id) || f.equals(Field.gene_id) || f.equals(Field.variant_custom_id))
					){				
				if(f == Field.gene_symbol) {
					if (entries.get(f) != null)	sb.append(entries.get(f)+"\t");
					else sb.append("\t");					
				}else if (f.getFieldClass() == String.class){
					if (entries.get(f) != null && entries.get(f).toString().length() > 0)	sb.append(HighlanderDatabase.format(dbms, Schema.HIGHLANDER, entries.get(f).toString())+"\t");
					else sb.append(nullStr+"\t");
				}else if (f.getFieldClass() == Boolean.class){
					if (entries.get(f) != null) {
						if (dbms == DBMS.hsqldb) sb.append(((boolean)entries.get(f)?"true":"false")+"\t");
						else sb.append(((boolean)entries.get(f)?"1":"0")+"\t");
					}else {
						sb.append(nullStr+"\t");
					}
				}else{
					if (entries.get(f) != null)	sb.append(entries.get(f)+"\t");
					else sb.append(nullStr+"\t");
				}
			}
		}
		sb.append("\n");
		return sb.toString();
	}

	public static String getInsertionColumnsString(Analysis analysis, String table){
		StringBuilder out  = new StringBuilder();
		for (Field f : Field.getAvailableFields(analysis, false)){
			if ((f.getTable(analysis).equalsIgnoreCase(table) || f.isForeignKey(table))
					&& !(f.equals(Field.variant_sample_id) || f.equals(Field.variant_static_id) || f.equals(Field.gene_id) || f.equals(Field.variant_custom_id))
					){
				out.append(f.getName() + ", ");
			}
		}
		out.deleteCharAt(out.length()-1);
		out.deleteCharAt(out.length()-1);
		return out.toString();		
	}

	protected String buildSqlInsert(boolean includeNulls, String table){
		StringBuilder sb = new StringBuilder();
		for (Field f : Field.getAvailableFields(analysis, false)){
			if (f.getTable(analysis).equalsIgnoreCase(table) || f.isForeignKey(table)){
				if (f.getFieldClass() == String.class){
					if (entries.get(f) != null && entries.get(f).toString().length() > 0)	sb.append(f.getName()+" = '"+DB.format(Schema.HIGHLANDER, entries.get(f).toString())+"', ");
					else if (includeNulls) sb.append(f.getName()+" = NULL, ");
				}else if (f.getFieldClass() == Integer.class || f.getFieldClass() == Double.class || f.getFieldClass() == Long.class){
					if (entries.get(f) != null)	sb.append(f.getName()+" = "+entries.get(f)+", ");
					else if (includeNulls) sb.append(f.getName()+" = NULL, ");
				}else if (f.getFieldClass() == Boolean.class){
					if (entries.get(f) != null)	sb.append(f.getName()+" = "+(boolean)entries.get(f)+", ");
					else if (includeNulls) sb.append(f.getName()+" = NULL, ");
				}else{
					if (entries.get(f) != null)	sb.append(f.getName()+" = '"+entries.get(f)+"', ");
					else if (includeNulls) sb.append(f.getName()+" = NULL, ");
				}
			}
		}  	
		sb.deleteCharAt(sb.length()-1);
		sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}

	public String exportToJson(){
		return exportToJson("","");
	}

	public String exportToIndentedJson(){
		return exportToJson("\n","  ");
	}

	public String exportToJson(String endl, String tab){
		StringBuilder sb = new StringBuilder();
		sb.append("{"+endl);
		sb.append(tab+"\"readGroupSets\":{"+endl);
		sb.append(tab+tab+"\"readGroups\":{"+endl);
		sb.append(tab+tab+tab+"\"sampleID\":\""+entries.get(Field.project_id)+"\","+endl);
		sb.append(tab+tab+tab+"\"experiment\":{"+endl);
		sb.append(tab+tab+tab+tab+"\"sequencingCenter\":\""+entries.get(Field.outsourcing)+"\","+endl);
		if (entries.get(Field.platform) != null)	sb.append(tab+tab+tab+tab+"\"InstrumentModel\":\""+entries.get(Field.platform)+"\","+endl);
		if (entries.get(Field.run_label) != null)	sb.append(tab+tab+tab+tab+"\"libraryId\":\""+HighlanderDatabase.format(DBMS.mysql, Schema.HIGHLANDER, entries.get(Field.run_label).toString())+"\","+endl);
		if (sb.charAt(sb.length()-1) == ',') sb.deleteCharAt(sb.length()-1);
		sb.append(tab+tab+tab+"},"+endl);
		sb.append(tab+tab+tab+"\"info\":{"+endl);
		if (entries.get(Field.sample) != null)	sb.append(tab+tab+tab+tab+"\"sample\":\""+HighlanderDatabase.format(DBMS.mysql, Schema.HIGHLANDER, entries.get(Field.sample).toString())+"\","+endl);
		if (entries.get(Field.pathology) != null)	sb.append(tab+tab+tab+tab+"\"pathology\":\""+HighlanderDatabase.format(DBMS.mysql, Schema.HIGHLANDER, entries.get(Field.pathology).toString())+"\","+endl);
		if (entries.get(Field.sample_type) != null)	sb.append(tab+tab+tab+tab+"\"sample_type\":\""+entries.get(Field.sample_type)+"\","+endl);
		if (sb.charAt(sb.length()-1) == ',') sb.deleteCharAt(sb.length()-1);
		sb.append(tab+tab+tab+"}"+endl);
		sb.append(tab+tab+"}"+endl);
		sb.append(tab+"},"+endl);
		sb.append(tab+"\"variants\":{"+endl);
		for (Field e : Field.getAvailableFields(analysis, false)){
			if (e.getJSonPath() == JSon.VARIANTS){
				if (e.getFieldClass() == String.class){
					if (entries.get(e) != null && entries.get(e).toString().length() > 0)	sb.append(tab+tab+"\""+e.getName()+"\":\""+entries.get(e)+"\","+endl);
				}else{
					if (entries.get(e) != null)	sb.append(tab+tab+"\""+e.getName()+"\":\""+entries.get(e)+"\","+endl);
				}
			}
		}
		sb.append(tab+tab+"\"calls\":{"+endl);
		sb.append(tab+tab+tab+"\"info\":{"+endl);
		for (Field e : Field.getAvailableFields(analysis, false)){
			if (e.getJSonPath() == JSon.CALLS){
				if (e.getFieldClass() == String.class){
					if (entries.get(e) != null && entries.get(e).toString().length() > 0)	sb.append(tab+tab+tab+tab+"\""+e.getName()+"\":\""+entries.get(e)+"\","+endl);
				}else{
					if (entries.get(e) != null)	sb.append(tab+tab+tab+tab+"\""+e.getName()+"\":\""+entries.get(e)+"\","+endl);
				}
			}
		}
		if (sb.charAt(sb.length()-1) == ',') sb.deleteCharAt(sb.length()-1);
		sb.append(tab+tab+tab+"}"+endl);
		sb.append(tab+tab+"},"+endl);
		sb.append(tab+tab+"\"info\":{"+endl);
		for (Field e : Field.getAvailableFields(analysis, false)){
			if (e.getJSonPath() == JSon.INFO){
				if (e.getFieldClass() == String.class){
					if (entries.get(e) != null && entries.get(e).toString().length() > 0)	sb.append(tab+tab+tab+"\""+e.getName()+"\":\""+entries.get(e)+"\","+endl);
				}else{
					if (entries.get(e) != null)	sb.append(tab+tab+tab+"\""+e.getName()+"\":\""+entries.get(e)+"\","+endl);
				}
			}
		}		
		if (sb.charAt(sb.length()-1) == ',') sb.deleteCharAt(sb.length()-1);
		sb.append(tab+tab+"}"+endl);
		sb.append(tab+"}"+endl);
		sb.append("");
		sb.append("}");
		return sb.toString();
	}

	public void updateAnnotations(Set<Annotation> annotations) throws Exception {
		DB.update(Schema.HIGHLANDER, getAnnotationsUpdateStatement(annotations));
	}

	public String getAnnotationsUpdateStatement(Set<Annotation> annotations) throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append("UPDATE " + analysis.getFromStaticAnnotations() + analysis.getJoinGeneAnnotations() + " SET ");
		for (Field f : Field.getAvailableFields(analysis, false)){
			if (annotations.contains(f.getAnnotationCode())){
				if (f.getFieldClass() == String.class){
					if (entries.get(f) != null && entries.get(f).toString().length() > 0)	sb.append(f.getName()+" = '"+DB.format(Schema.HIGHLANDER, entries.get(f).toString())+"', ");
					else sb.append(f.getName()+" = NULL, ");
				}else if (f.getFieldClass() == Boolean.class){
					if (entries.get(f) != null)	sb.append(f.getName()+" = "+(boolean)entries.get(f)+", ");
					else sb.append(f.getName()+" = NULL, ");
				}else if (f.getFieldClass() == Integer.class || f.getFieldClass() == Double.class){
					if (entries.get(f) != null)	sb.append(f.getName()+" = "+entries.get(f)+", ");
					else sb.append(f.getName()+" = NULL, ");
				}else{
					if (entries.get(f) != null)	sb.append(f.getName()+" = '"+entries.get(f)+"', ");
					else sb.append(f.getName()+" = NULL, ");
				}
			}
		}
		sb.deleteCharAt(sb.length()-1);
		sb.deleteCharAt(sb.length()-1);
		sb.append(" WHERE "+Field.variant_static_id.getQueryWhereName(analysis, false)+" = " + (long)entries.get(Field.variant_static_id));
		return sb.toString();
	}

	/**
	 * Remove this variant from Highlander sample and custom tables (variant stay in static table, as well as user annotations).
	 * @throws Exception
	 */
	public void removeFromDatabase() throws Exception {
		int variant_sample_id = (entries.get(Field.variant_sample_id) == null) ? -1 : (int)entries.get(Field.variant_sample_id);
		if (variant_sample_id > 0)
			DB.update(Schema.HIGHLANDER, "DELETE FROM "+analysis.getFromSampleAnnotations()+" WHERE "+Field.variant_sample_id.getQueryWhereName(analysis, false)+" = " + variant_sample_id);
		int variant_custom_id = (entries.get(Field.variant_custom_id) == null) ? -1 : (int)entries.get(Field.variant_custom_id);
		if (variant_custom_id > 0)
			DB.update(Schema.HIGHLANDER, "DELETE FROM "+analysis.getFromCustomAnnotations()+" WHERE "+Field.variant_custom_id.getQueryWhereName(analysis, false)+" = " + variant_custom_id);
	}

	public static void main(String[] args){
		try{
			Highlander.initialize(new Parameters(false),10);
			AnalysisFull analysis = new AnalysisFull(new Analysis("panels_somatic_hg38"));			
			for (String sample : new String[]{"Bob","Kyra","Lian","Omar"}){
				System.out.println("Sample " + sample);
				String header = "#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	Bob	Kyra	Lian	Omar";
				//String line = "chr3	30650379	.	GAA	G	.	multiallelic;normal_artifact	AS_FilterStatus=weak_evidence|SITE|SITE;AS_SB_TABLE=1552,1661|8,11|89,106|74,73;DP=4246;ECNT=1;GERMQ=93;MBQ=36,37;MFRL=176,164;MMQ=60,60;MPOS=21;NALOD=-7.472;NLOD=508.38;POPAF=2.83;ROQ=93;RPA=10,8;RU=A;STR;STRQ=93;TLOD=6.12;ANN=G|frameshift_variant|HIGH|TGFBR2|ENSG00000163513|transcript|ENST00000359013.4|protein_coding|4/8|c.457_458delAA|p.Lys153fs|740/4605|457/1779|153/592||INFO_REALIGN_3_PRIME,G|frameshift_variant|HIGH|TGFBR2|ENSG00000163513|transcript|ENST00000295754.10|protein_coding|3/7|c.382_383delAA|p.Lys128fs|665/4530|382/1704|128/567||INFO_REALIGN_3_PRIME,G|non_coding_transcript_exon_variant|MODIFIER|TGFBR2|ENSG00000163513|transcript|ENST00000673250.1|processed_transcript|4/4|n.506_507delAA||||||INFO_REALIGN_3_PRIME,G|non_coding_transcript_exon_variant|MODIFIER|TGFBR2|ENSG00000163513|transcript|ENST00000672866.1|processed_transcript|3/7|n.1978_1979delAA||||||INFO_REALIGN_3_PRIME;LOF=(TGFBR2|ENSG00000163513|6|0.33)	GT:AD:AF:DP:F1R2:F2R1:SB	0/0:1821,12:0.005195:2033:869,9:866,3:871,950,105,107	0/1/0/0:1392,7:0.005527:1541:671,3:633,4:681,711,66,83";
				//String line = "chr5	67591097	.	A	G	395.89	PASS	AF=0.0375;AO=94;DP=2690;FAO=75;FDP=2000;FDVR=5;FR=.,REALIGNEDx0.041;FRO=1925;FSAF=43;FSAR=32;FSRF=888;FSRR=1037;FWDB=-0.0135147;FXX=0;HRUN=2;HS_ONLY=0;LEN=1;MLLD=69.0801;OALT=G;OID=.;OMAPALT=G;OPOS=67591097;OREF=A;PB=.;PBP=.;QD=0.791771;RBI=0.0469941;REFB=0.00123402;REVB=0.0450089;RO=2596;SAF=53;SAR=41;SRF=1205;SRR=1391;SSEN=0;SSEP=0;SSSB=0.0831814;STB=0.606725;STBP=0.055;TYPE=snp;VARB=-0.0314877;ANN=G|missense_variant|MODERATE|PIK3R1|ENSG00000145675|transcript|ENST00000396611|protein_coding|12/15|c.1690A>G|p.Asn564Asp|1732/6459|1690/2199|564/732||,G|missense_variant|MODERATE|PIK3R1|ENSG00000145675|transcript|ENST00000521381|protein_coding|13/16|c.1690A>G|p.Asn564Asp|2306/7011|1690/2175|564/724||,G|missense_variant|MODERATE|PIK3R1|ENSG00000145675|transcript|ENST00000521657|protein_coding|13/16|c.1690A>G|p.Asn564Asp|2254/3891|1690/2175|564/724||,G|missense_variant|MODERATE|PIK3R1|ENSG00000145675|transcript|ENST00000274335|protein_coding|12/15|c.1690A>G|p.Asn564Asp|1732/6435|1690/2175|564/724||,G|missense_variant|MODERATE|PIK3R1|ENSG00000145675|transcript|ENST00000320694|protein_coding|7/10|c.790A>G|p.Asn264Asp|1158/2625|790/1275|264/424||,G|missense_variant|MODERATE|PIK3R1|ENSG00000145675|transcript|ENST00000336483|protein_coding|7/10|c.880A>G|p.Asn294Asp|972/2439|880/1365|294/454||,G|missense_variant|MODERATE|PIK3R1|ENSG00000145675|transcript|ENST00000523872|protein_coding|6/9|c.601A>G|p.Asn201Asp|833/2473|601/1086|201/361||,G|3_prime_UTR_variant|MODIFIER|PIK3R1|ENSG00000145675|transcript|ENST00000517698|nonsense_mediated_decay|6/7|n.*660A>G|||||2158|,G|downstream_gene_variant|MODIFIER|PIK3R1|ENSG00000145675|transcript|ENST00000523807|protein_coding||c.*552A>G|||||1923|WARNING_TRANSCRIPT_INCOMPLETE,G|downstream_gene_variant|MODIFIER|PIK3R1|ENSG00000145675|transcript|ENST00000522084|protein_coding||c.*705A>G|||||2070|WARNING_TRANSCRIPT_INCOMPLETE,G|downstream_gene_variant|MODIFIER|PIK3R1|ENSG00000145675|transcript|ENST00000521409|protein_coding||c.*540A>G|||||657|WARNING_TRANSCRIPT_INCOMPLETE,G|downstream_gene_variant|MODIFIER|PIK3R1|ENSG00000145675|transcript|ENST00000518292|retained_intron||n.*558A>G|||||1877|,G|downstream_gene_variant|MODIFIER|PIK3R1|ENSG00000145675|transcript|ENST00000519025|protein_coding||c.*567A>G|||||611|WARNING_TRANSCRIPT_NO_START_CODON,G|non_coding_exon_variant|MODIFIER|PIK3R1|ENSG00000145675|transcript|ENST00000517698|nonsense_mediated_decay|6/7|n.*660A>G||||||,G|non_coding_exon_variant|MODIFIER|PIK3R1|ENSG00000145675|transcript|ENST00000518813|retained_intron|6/9|n.2233A>G||||||,G|non_coding_exon_variant|MODIFIER|PIK3R1|ENSG00000145675|transcript|ENST00000520550|retained_intron|5/6|n.1089A>G||||||	GT:AF:AO:DP:FAO:FDP:FRO:FSAF:FSAR:FSRF:FSRR:GQ:RO:SAF:SAR:SRF:SRR	0/1:0.0375:94:2690:75:2000:1925:43:32:888:1037:99:2596:53:41:1205:1391";
				//String line = "9	27183463	rs682632	A	C	7221.77	PASS	AC=2;AF=1.00;AN=2;DB;DP=254;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=60.00;MQ0=0;QD=28.43;SOR=0.987;ANN=C|missense_variant|MODERATE|TEK|ENSG00000120156|transcript|ENST00000380036|protein_coding|8/23|c.1037A>C|p.Gln346Pro|1479/4760|1037/3375|346/1124||,C|missense_variant|MODERATE|TEK|ENSG00000120156|transcript|ENST00000519097|protein_coding|6/21|c.596A>C|p.Gln199Pro|1046/3836|596/2931|199/976||,C|missense_variant|MODERATE|TEK|ENSG00000120156|transcript|ENST00000406359|protein_coding|7/22|c.908A>C|p.Gln303Pro|1083/3935|908/3246|303/1081||,C|missense_variant|MODERATE|TEK|ENSG00000120156|transcript|ENST00000519080|protein_coding|5/10|c.467A>C|p.Gln156Pro|605/1543|467/1405|156/467||WARNING_TRANSCRIPT_INCOMPLETE	GT:AD:DP:GQ:PL	1/1:0,253:253:99:7250,757,0";
				//String line	=	"chr9	27212708	.	A	T	21994	PASS	AF=0.0810914;AO=7431;DP=89214;FAO=4051;FDP=49956;FR=.;FRO=45905;FSAF=2599;FSAR=1452;FSRF=29961;FSRR=15944;FWDB=-0.0106544;FXX=0.00088;HRUN=1;LEN=1;MLLD=84.4881;QD=1.76107;RBI=0.0151118;REFB=-0.00274841;REVB=-0.0107168;RO=81712;SAF=4762;SAR=2669;SRF=53280;SRR=28432;SSEN=0;SSEP=0;SSSB=-0.0145801;STB=0.511163;STBP=0.151;TYPE=snp;VARB=0.0331574;OID=.;OPOS=27212708;OREF=A;OALT=T;OMAPALT=T	GT:GQ:DP:FDP:RO:FRO:AO:FAO:AF:SAR:SAF:SRF:SRR:FSAR:FSAF:FSRF:FSRR	0/1:21993:89214:49956:81712:45905:7431:4051:0.0810914:2669:4762:53280:28432:1452:2599:29961:15944";
				//String line	=	"chr1	10318652	.	CT	GT,TC	633.09	PASS	AF=0.463636,0;AO=50,2;DP=110;FAO=51,0;FDP=110;FR=.;FRO=59;FSAF=14,0;FSAR=37,0;FSRF=35;FSRR=24;FWDB=-0.0550657,-0.201462;FXX=0;HRUN=2,2;LEN=1,2;MLLD=88.348,134.992;OALT=G,TC;OID=.,.;OMAPALT=GT,TC;OPOS=10318652,10318652;OREF=C,CT;QD=23.0214;RBI=0.0550682,0.20168;REFB=-0.021508,-0.00201293;REVB=0.000524239,0.00936907;RO=52;SAF=13,2;SAR=37,0;SRF=28;SRR=24;SSEN=0,0;SSEP=0,0;SSSB=-0.280927,0.356146;STB=0.679717,0.5;STBP=0.001,1;TYPE=snp,mnp;VARB=0.0278924,0;ANN=TC|missense_variant|MODERATE|KIF1B|ENSG00000054523|transcript|ENST00000377081|protein_coding||c.285_286delCTinsTC|p.AlaTyr95AlaHis|364/8746|285/5472|95/1823||,TC|missense_variant|MODERATE|KIF1B|ENSG00000054523|transcript|ENST00000377093|protein_coding||c.285_286delCTinsTC|p.AlaTyr95AlaHis|438/7565|285/3462|95/1153||,TC|missense_variant|MODERATE|KIF1B|ENSG00000054523|transcript|ENST00000263934|protein_coding||c.285_286delCTinsTC|p.AlaTyr95AlaHis|438/6816|285/5313|95/1770||,TC|missense_variant|MODERATE|KIF1B|ENSG00000054523|transcript|ENST00000377086|protein_coding||c.285_286delCTinsTC|p.AlaTyr95AlaHis|487/10669|285/5451|95/1816||,TC|missense_variant|MODERATE|KIF1B|ENSG00000054523|transcript|ENST00000377083|protein_coding||c.285_286delCTinsTC|p.AlaTyr95AlaHis|598/5885|285/3462|95/1153||,G|synonymous_variant|LOW|KIF1B|ENSG00000054523|transcript|ENST00000377081|protein_coding|3/48|c.285C>G|p.Ala95Ala|364/8746|285/5472|95/1823||,G|synonymous_variant|LOW|KIF1B|ENSG00000054523|transcript|ENST00000377093|protein_coding|4/21|c.285C>G|p.Ala95Ala|438/7565|285/3462|95/1153||,G|synonymous_variant|LOW|KIF1B|ENSG00000054523|transcript|ENST00000263934|protein_coding|4/47|c.285C>G|p.Ala95Ala|438/6816|285/5313|95/1770||,G|synonymous_variant|LOW|KIF1B|ENSG00000054523|transcript|ENST00000377086|protein_coding|4/49|c.285C>G|p.Ala95Ala|487/10669|285/5451|95/1816||,G|synonymous_variant|LOW|KIF1B|ENSG00000054523|transcript|ENST00000377083|protein_coding|4/21|c.285C>G|p.Ala95Ala|598/5885|285/3462|95/1153||	GT:AF:AO:DP:FAO:FDP:FRO:FSAF:FSAR:FSRF:FSRR:GQ:RO:SAF:SAR:SRF:SRR	0/1:0.463636,0:50,2:110:51,0:110:59:14,0:37,0:35:24:99:52:13,2:37,0:28:24";
				//String line = "chr15	86983715	rs137960742	G	GAAT,GAATAATAAT	1681.73	.	AC=1,1;AF=0.500,0.500;AN=2;DB;DP=50;FS=0.000;MLEAC=1,1;MLEAF=0.500,0.500;MQ=60.00;QD=33.63;SOR=0.746;ANN=TAA|intron_variant|MODIFIER|AGBL1|ENSG00000166748|transcript|ENST00000441037|protein_coding|17/23|c.2417+42965_2417+42967dupTAA||||||INFO_REALIGN_3_PRIME,TAATAATAA|intron_variant|MODIFIER|AGBL1|ENSG00000166748|transcript|ENST00000441037|protein_coding|17/23|c.2417+42959_2417+42967dupTAATAATAA||||||INFO_REALIGN_3_PRIME,TAA|intron_variant|MODIFIER|AGBL1|ENSG00000166748|transcript|ENST00000421325|protein_coding|16/21|c.2417+42965_2417+42967dupTAA||||||INFO_REALIGN_3_PRIME,TAATAATAA|intron_variant|MODIFIER|AGBL1|ENSG00000166748|transcript|ENST00000421325|protein_coding|16/21|c.2417+42959_2417+42967dupTAATAATAA||||||INFO_REALIGN_3_PRIME,TAA|intron_variant|MODIFIER|AGBL1|ENSG00000166748|transcript|ENST00000389298|protein_coding|10/15|c.1610+42965_1610+42967dupTAA||||||INFO_REALIGN_3_PRIME,TAATAATAA|intron_variant|MODIFIER|AGBL1|ENSG00000166748|transcript|ENST00000389298|protein_coding|10/15|c.1610+42959_1610+42967dupTAATAATAA||||||INFO_REALIGN_3_PRIME	GT:AD:DP:GQ:PL	1/2:0,20,17:37:99:1719,761,686,843,0,786";
				//String line = "chrUn_gl000219	99615	.	C	T	439.77	VQSRTrancheSNP99.90to100.00	AC=1;AF=0.500;AN=2;BaseQRankSum=0.596;ClippingRankSum=0.995;DP=174;FS=0.000;MLEAC=1;MLEAF=0.500;MQ=46.01;MQ0=0;MQRankSum=-3.110;QD=2.53;ReadPosRankSum=-0.779;VQSLOD=-1.965e+00;culprit=QD;ANN=T||MODIFIER|||||||||||||ERROR_CHROMOSOME_NOT_FOUND	GT:AD:DP:GQ:PL	0/1:158,15:173:99:468,0,38620";
				//String line = "6	157528834	.	CT	C	1592.0	PASS	AAp=2187;CANONICAL=YES;CCDS=CCDS55072.1;CDSp=6560;CI95=0.5,0.5;CQ=frameshift_variant;DENOVO-INDEL;DINDEL;DP4=28,22,20,19;ENSG=ENSG00000049618;ENSP=ENSP00000344546;ENST=ENST00000346085;EXON=20/20;FQ=217;HGNC=ARID1B;HGNC_ALL=ARID1B;HI=0.132;HP=1;INDEL;NF=22;NFS=43;NR=22;NRS=44;PV4=0.67,2.4e-05,0.23,1;SAMTOOLS;cDNA=6561;ANN=|frameshift_variant|HIGH|ARID1B|ENSG00000049618|transcript|ENST00000367148|protein_coding|20/20|c.6680delT|p.Leu2227fs|6680/8246|6680/6870|2227/2289||,|frameshift_variant|HIGH|ARID1B|ENSG00000049618|transcript|ENST00000350026|protein_coding|19/19|c.6521delT|p.Leu2174fs|6522/7971|6521/6711|2174/2236||,|frameshift_variant|HIGH|ARID1B|ENSG00000049618|transcript|ENST00000346085|protein_coding|20/20|c.6560delT|p.Leu2187fs|6561/9639|6560/6750|2187/2249||,|frameshift_variant|HIGH|ARID1B|ENSG00000049618|transcript|ENST00000275248|protein_coding|20/20|c.6506delT|p.Leu2169fs|6658/8224|6506/6696|2169/2231||,|frameshift_variant|HIGH|ARID1B|ENSG00000049618|transcript|ENST00000414678|protein_coding|19/19|c.5087delT|p.Leu1696fs|5087/6653|5087/5277|1696/1758||WARNING_TRANSCRIPT_NO_START_CODON	GT:MOTHER_ALT_PRP:DP_MOTHER:IN_CHILD_VCF:FATHER_ALT_PRP:GQ:PP_DNM:CHILD_ALT_PRP:IN_MOTHER_VCF:MAX_ALT_IN_PARENT:TEAM29_FILTER:samtools_PL:DP_FATHER:DP_CHILD:IN_FATHER_VCF	0/1:0.0:108:Y:0.0:99:1:0.4333:N:0.0:PASS:255,0,255:155:90:N";
				//String line = "18	77108248	.	GAAA	G,GGAA	69	dindel_hp10	AFR_AF=.,.;AMR_AF=.,.;ASN_AF=.,.;CQ=intron_variant;DDD_AF=0.086786,0.007597;DINDEL;ENSG=ENSG00000166377;ENSP=ENSP00000436646;ENST=ENST00000490210;ESP_AF=.,.;EUR_AF=.,.;HGNC=ATP9B;HGNC_ALL=ATP9B;HP=21;INDEL;INTRON=16/18;MAX_AF=0.16173,0.007597;NF=0;NFS=3;NR=9;NRS=54;UK10K_cohort_AF=0.16173,.	GT:GQ	1/2:56";
				//String line = "1	17090902	rs11411246	A	AT	55	PASS	Allele=T;CI95=0.5,0.5;CQ=splice_region_variant;DDD_AF=0.145718;DINDEL;DP4=12,10,4,3;ENSG=ENSG00000186715;ENSP=ENSP00000439273;ENST=ENST00000334998;FQ=8.19;HGNC=MST1P9;HGNC_ALL=MST1P9;HP=2;INDEL;INTRON=1/14;MAX_AF=0.145718;NF=2;NFS=9;NR=3;NRS=12;PV4=1,1,0.1,0.4;SAMTOOLS;segmentaldup	GT:GQ:samtools_PL	0/1:55:42,0,179";
				//String line = "chr16	732076	.	GGTGA	G	17814.73	.	AC=2;AF=1.00;AN=2;DP=426;FS=0.000;MLEAC=2;MLEAF=1.00;MQ=60.00;MQ0=0;QD=27.04;SOR=0.967;ANN=|splice_region_variant&intron_variant|LOW|STUB1|ENSG00000103266|transcript|ENST00000219548|protein_coding|5/6|c.669+4_669+7delAGTG||||||INFO_REALIGN_3_PRIME,|splice_region_variant&intron_variant|LOW|STUB1|ENSG00000103266|transcript|ENST00000565677|protein_coding|5/6|c.453+4_453+7delAGTG||||||INFO_REALIGN_3_PRIME,|splice_region_variant&intron_variant|LOW|STUB1|ENSG00000103266|transcript|ENST00000569248|retained_intron|3/4|n.1382+4_1382+7delAGTG||||||INFO_REALIGN_3_PRIME,|splice_region_variant&intron_variant|LOW|STUB1|ENSG00000103266|transcript|ENST00000567173|protein_coding|5/5|c.612+4_612+7delAGTG||||||WARNING_TRANSCRIPT_INCOMPLETE&INFO_REALIGN_3_PRIME,|splice_region_variant&intron_variant|LOW|STUB1|ENSG00000103266|transcript|ENST00000564370|protein_coding|4/5|c.453+4_453+7delAGTG||||||INFO_REALIGN_3_PRIME,|splice_region_variant&intron_variant|LOW|STUB1|ENSG00000103266|transcript|ENST00000566181|processed_transcript|4/4|n.461+4_461+7delAGTG||||||INFO_REALIGN_3_PRIME,|splice_region_variant&intron_variant|LOW|STUB1|ENSG00000103266|transcript|ENST00000566408|protein_coding|4/4|c.384+4_384+7delAGTG||||||WARNING_TRANSCRIPT_NO_START_CODON&INFO_REALIGN_3_PRIME,|splice_region_variant&intron_variant|LOW|STUB1|ENSG00000103266|transcript|ENST00000564316|protein_coding|3/4|c.228+4_228+7delAGTG||||||WARNING_TRANSCRIPT_NO_START_CODON&INFO_REALIGN_3_PRIME,|3_prime_UTR_variant|MODIFIER|JMJD8|ENSG00000161999|transcript|ENST00000293882|protein_coding|9/9|c.*714_*717delTCAC|||||714|,|3_prime_UTR_variant|MODIFIER|JMJD8|ENSG00000161999|transcript|ENST00000412368|protein_coding|9/9|c.*714_*717delTCAC|||||714|,|3_prime_UTR_variant|MODIFIER|JMJD8|ENSG00000161999|transcript|ENST00000454700|protein_coding|8/8|c.*714_*717delTCAC|||||714|,|3_prime_UTR_variant|MODIFIER|JMJD8|ENSG00000161999|transcript|ENST00000609261|protein_coding|9/9|c.*714_*717delTCAC|||||714|,|upstream_gene_variant|MODIFIER|LA16c-313D11.9|ENSG00000260394|transcript|ENST00000571933|antisense||n.-1_-1delTCAC|||||2340|,|upstream_gene_variant|MODIFIER|LA16c-313D11.9|ENSG00000260394|transcript|ENST00000567091|antisense||n.-1_-1delTCAC|||||2300|,|upstream_gene_variant|MODIFIER|STUB1|ENSG00000103266|transcript|ENST00000565813|nonsense_mediated_decay||n.-3_-3delGTGA|||||339|,|downstream_gene_variant|MODIFIER|RHBDL1|ENSG00000103269|transcript|ENST00000219551|protein_coding||c.*1344_*1344delGTGA|||||3809|,|downstream_gene_variant|MODIFIER|WDR24|ENSG00000127580|transcript|ENST00000248142|protein_coding||c.*2763_*2763delTCAC|||||2545|,|downstream_gene_variant|MODIFIER|RHBDL1|ENSG00000103269|transcript|ENST00000352681|protein_coding||c.*1247_*1247delGTGA|||||3809|,|downstream_gene_variant|MODIFIER|RHBDL1|ENSG00000103269|transcript|ENST00000561556|protein_coding||c.*796_*796delGTGA|||||4234|WARNING_TRANSCRIPT_INCOMPLETE,|downstream_gene_variant|MODIFIER|RHBDL1|ENSG00000103269|transcript|ENST00000450775|retained_intron||n.*1566_*1566delGTGA|||||3810|,|downstream_gene_variant|MODIFIER|STUB1|ENSG00000103266|transcript|ENST00000563505|retained_intron||n.*894_*894delGTGA|||||11|,|downstream_gene_variant|MODIFIER|STUB1|ENSG00000103266|transcript|ENST00000567790|retained_intron||n.*428_*428delGTGA|||||687|,|downstream_gene_variant|MODIFIER|JMJD8|ENSG00000161999|transcript|ENST00000569441|retained_intron||n.*1428_*1428delTCAC|||||329|,|downstream_gene_variant|MODIFIER|JMJD8|ENSG00000161999|transcript|ENST00000562824|protein_coding||c.*749_*749delTCAC|||||451|,|downstream_gene_variant|MODIFIER|JMJD8|ENSG00000161999|transcript|ENST00000563088|retained_intron||n.*1067_*1067delTCAC|||||466|,|downstream_gene_variant|MODIFIER|JMJD8|ENSG00000161999|transcript|ENST00000564436|retained_intron||n.*633_*633delTCAC|||||498|,|downstream_gene_variant|MODIFIER|JMJD8|ENSG00000161999|transcript|ENST00000570037|retained_intron||n.*870_*870delTCAC|||||581|,|downstream_gene_variant|MODIFIER|JMJD8|ENSG00000161999|transcript|ENST00000562111|protein_coding||c.*687_*687delTCAC|||||581|,|downstream_gene_variant|MODIFIER|JMJD8|ENSG00000161999|transcript|ENST00000565258|nonsense_mediated_decay||n.*245_*245delTCAC|||||695|,|downstream_gene_variant|MODIFIER|JMJD8|ENSG00000161999|transcript|ENST00000569396|retained_intron||n.*729_*729delTCAC|||||1108|,|downstream_gene_variant|MODIFIER|JMJD8|ENSG00000161999|transcript|ENST00000566199|retained_intron||n.*719_*719delTCAC|||||1266|,|downstream_gene_variant|MODIFIER|JMJD8|ENSG00000161999|transcript|ENST00000567901|retained_intron||n.*785_*785delTCAC|||||1278|,|downstream_gene_variant|MODIFIER|WDR24|ENSG00000127580|transcript|ENST00000293883|protein_coding||c.*3133_*3133delTCAC|||||2545|,|downstream_gene_variant|MODIFIER|WDR24|ENSG00000127580|transcript|ENST00000567014|retained_intron||n.*757_*757delTCAC|||||3373|,|non_coding_exon_variant|MODIFIER|JMJD8|ENSG00000161999|transcript|ENST00000567120|retained_intron|8/8|n.1796_1799delTCAC||||||,|non_coding_exon_variant|MODIFIER|JMJD8|ENSG00000161999|transcript|ENST00000565302|retained_intron|6/6|n.1593_1596delTCAC||||||,|non_coding_exon_variant|MODIFIER|JMJD8|ENSG00000161999|transcript|ENST00000568689|retained_intron|8/8|n.1617_1620delTCAC||||||,|non_coding_exon_variant|MODIFIER|JMJD8|ENSG00000161999|transcript|ENST00000568313|retained_intron|2/2|n.949_952delTCAC||||||	GT:AD:DP:GQ:PL	1/1:0,410:410:99:17852,1235,0";
				//String line	=	"chr1	14	.	GCCCCACCC	G	633.09	PASS	DP=50	GT:AD:DP	0/0:100,0:100	1/1:0,100:100	0/1:50,50:100	0/1:50,50:100";
				//String line	=	"chr1	20	.	A	T,*	633.09	PASS	DP=50	GT:AD:DP	0/1:50,50,0:100	2/2:0,0,100:100	1/2:0,50,50:100	0/2:50,0,50:100";
				//String header = "#CHROM	POS	ID	REF	ALT	QUAL	FILTER	INFO	FORMAT	Bob";
				//String line = "chr6	43445127	rs2125739	T	C	1158.64	PASS	AC=1;AF=0.5;AN=2;ANN=C|missense_variant&splice_region_variant|MODERATE|ABCC10|ENSG00000124574|transcript|ENST00000244533.7|protein_coding|12/20|c.2759T>C|p.Ile920Thr|3118/5088|2759/4395|920/1464||,C|missense_variant&splice_region_variant|MODERATE|ABCC10|ENSG00000124574|transcript|ENST00000372530.9|protein_coding|14/22|c.2843T>C|p.Ile948Thr|3071/5043|2843/4479|948/1492||,C|splice_region_variant&non_coding_transcript_exon_variant|LOW|ABCC10|ENSG00000124574|transcript|ENST00000463024.1|retained_intron|8/16|n.2571T>C||||||,C|upstream_gene_variant|MODIFIER|ABCC10|ENSG00000124574|transcript|ENST00000437104.3|retained_intron||n.-1728T>C|||||1728|,C|upstream_gene_variant|MODIFIER|ABCC10|ENSG00000124574|transcript|ENST00000505344.1|protein_coding||c.-2866T>C|||||2866|WARNING_TRANSCRIPT_NO_START_CODON,C|downstream_gene_variant|MODIFIER|ABCC10|ENSG00000124574|transcript|ENST00000469856.1|retained_intron||n.*3066T>C|||||3066|,C|downstream_gene_variant|MODIFIER|ABCC10|ENSG00000124574|transcript|ENST00000372515.8|protein_coding||c.*3230T>C|||||3230|WARNING_TRANSCRIPT_NO_STOP_CODON,C|non_coding_transcript_exon_variant|MODIFIER|ABCC10|ENSG00000124574|transcript|ENST00000372512.2|retained_intron|1/2|n.28T>C||||||;BaseQRankSum=1.719;DB;DP=73;ExcessHet=3.0103;FS=3.118;MLEAC=1;MLEAF=0.5;MQ=60;MQ0=0;MQRankSum=0;QD=15.87;ReadPosRankSum=-1.042;SOR=1.075	GT:AD:DP:GQ:PL	0/1:33,40:73:99:1166,0,875";
				//TODO pour pas d'annotation snpeff alors que plus haut le CT	GT,TC en a ??
				String line = "chr13	107145465	.	CG	GC,TG	15.19	PASS	AF=0,0.0102041;AO=23,6;DP=589;FAO=0,6;FDP=588;FDVR=5,10;FR=.,.;FRO=582;FSAF=0,2;FSAR=0,4;FSRF=317;FSRR=265;FWDB=-0.137844,-0.0474046;FXX=0.00169776;HRUN=2,2;HS_ONLY=0;LEN=2,1;MLLD=44.5726,215.432;OALT=GC,T;OID=.,.;OMAPALT=GC,TG;OPOS=107145465,107145465;OREF=CG,C;PB=.,.;PBP=.,.;QD=0.103359;RBI=0.138569,0.0683631;REFB=-0.0005775,-0.000397812;REVB=0.0141535,0.0492576;RO=538;SAF=23,2;SAR=0,4;SRF=273;SRR=265;SSEN=0,0;SSEP=0,0;SSSB=0.452949,-0.0654476;STB=0.5,0.702767;STBP=1,0.373;TYPE=mnp,snp;VARB=0,0.040045;ANN=TG|missense_variant|MODERATE|EFNB2|ENSG00000125266|transcript|ENST00000245323|protein_coding|5/5|c.925G>A|p.Gly309Ser|1075/4461|925/1002|309/333||,GC|missense_variant|MODERATE|EFNB2|ENSG00000125266|transcript|ENST00000245323|protein_coding|5/5|c.924_925delCGinsGC|p.SerGly308ArgArg|1075/4461|924/1002|308/333||	GT:AF:AO:DP:FAO:FDP:FRO:FSAF:FSAR:FSRF:FSRR:GQ:RO:SAF:SAR:SRF:SRR	0/2:0,0.0102041:23,6:589:0,6:588:582:0,2:0,4:317:265:14:538:23,2:0,4:273:265";
				for (AnnotatedVariant annotatedVariant : new AnnotatedVariant(analysis).setAllAnnotations(header.split("\t"), line.split("\t"), sample, false)) {
					System.out.println("Does variant exists ? --> " + annotatedVariant.exist());
					if (annotatedVariant.exist){
						System.out.println(annotatedVariant);
						for (String table : new String[] {
								analysis.getTableSampleAnnotations(), 
								analysis.getTableStaticAnnotations(), 
								analysis.getTableGeneAnnotations(), 
								analysis.getTableCustomAnnotations()}) {
							System.out.println("Table " + table);
							System.out.println(AnnotatedVariant.getInsertionColumnsString(analysis, table).replace(", ", "\t"));
							System.out.println(annotatedVariant.getInsertionString(DBMS.mysql, table));
						}
						System.out.println();
						System.out.println(annotatedVariant.exportToIndentedJson());
					}
				}
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}

}
