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

import java.io.File;
import java.io.FileWriter;

import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Parameters;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.database.DBUtils;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.Field.Annotation;
import be.uclouvain.ngs.highlander.database.Field.AnnotationType;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
import be.uclouvain.ngs.highlander.datatype.AnnotatedVariant;
import be.uclouvain.ngs.highlander.datatype.Gene;
import be.uclouvain.ngs.highlander.datatype.Reference;
import be.uclouvain.ngs.highlander.datatype.SNPEffect.VariantType;

public class DbUpdater {

	static final public String version = "17.13";

	public enum ToolArgument {
		analysis("A", "analysis", "analysis program(s), corresponding to a database table prefix (e.g. exomes_haplotype_caller or exomes_lifescope). "
				+ "You can give several analyses using + (e.g. gatk+lifescope) or all existing analyses using 'ALL'"), 
		source("S", "datasource", "datasource to choose from " + Annotation.getAnnotations(AnnotationType.STATIC)), 
		repository("r", "directory", "path to the bam repository. Each analysis should have it's own subdirectory named exactly as the analysis (default is /data/highlander/bam/)"), 
		report("R", null, "only display a report with the differences with current database (for specerrors). Without this option, make update in the database"), 
		threads("n", "number", "number of thread to use for paralellization"), 
		bool("b", "boolean", "set the tool value to 0 or 1"), 
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
		annotations("Update all variant annotations relative to given datasource in the give analysis table",
				new ToolArgument[] {ToolArgument.analysis, ToolArgument.source, ToolArgument.threads, },
				new String[]{
						"the analysis program (e.g. exomes_haplotype_caller)",
						"one or more (+) datasource for annotations from " + Annotation.getAnnotations(AnnotationType.STATIC),
						"a number of threads to paralellize the process (default 1)",
				},
				new boolean[] {true, true, false, },
				new String[] {null, null, "1", },
				true),
		runmetrics("Update run metrics for all samples available in the given analysis table (het/hom ratio and number of variants)",
				new ToolArgument[] {ToolArgument.analysis, },
				new String[]{
						"the analysis program (e.g. exomes_haplotype_caller)",
				},
				new boolean[] {true, },
				new String[] {null, },
				true),
		ngslogistics("Generate a sample sheet for the NGS Logistics software",
				new ToolArgument[] {ToolArgument.analysis, ToolArgument.repository, },
				new String[]{
						"the analysis program (e.g. exomes_haplotype_caller)",
						"the path to the sample sheet directory",
				},
				new boolean[] {true, true, },
				new String[] {null, null, },
				true),
		updateiontorrent("[beta] Copy parameter file from 'reference' to each directory matching an Ion Torrent sample. "
				+ "Check which samples and which set of parameters to copy using the Ion Importer database.",
				new ToolArgument[] { },
				new String[]{	},
				new boolean[] { },
				new String[] { },
				true),
		pop1000g("[beta] Update 1000g table to set population instead of pathology",
				new ToolArgument[] {ToolArgument.analysis, },
				new String[]{
						"the analysis program (e.g. genomes_1000g)",
				},
				new boolean[] {true, },
				new String[] {null, },
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

	
	public Parameters parameters;
	public HighlanderDatabase DB;

	public volatile int count = 0;
	public int total = 0;

	public DbUpdater(String configFile, int nthreads) throws Exception {
		parameters = (configFile == null) ? new Parameters(false) : new Parameters(false, new File(configFile));
		Highlander.initialize(parameters, 10+(2*nthreads));
		DB = Highlander.getDB();
	}

	public DbUpdater() throws Exception {
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

	public void setSoftUpdate(boolean enable) throws Exception {
		DB.update(Schema.HIGHLANDER, "UPDATE main SET update_soft = "+((enable)?1:0));
	}

	public void setHardUpdate(boolean enable) {
		try{
			DB.update(Schema.HIGHLANDER, "UPDATE main SET update_hard = "+((enable)?1:0));
		}catch(Exception ex){
			System.err.println("Cannot change update_hard flag");
			ex.printStackTrace();
		}
	}	

	//TODO 1000G - revoir avec population plutot que pathology et nouvelles tables de jointure
	public void update1000g(List<? extends Analysis> analyses) throws Exception {
		for (Analysis analysis : analyses){
			Map<String,Set<String>> pathoHasSample = new HashMap<String, Set<String>>();
			try (Results res = DB.select(Schema.HIGHLANDER, "SELECT sample, pathology FROM projects")) {
				while (res.next()){
					String sample = res.getString("sample");
					String pathology = res.getString("pathology");
					if (!pathoHasSample.containsKey(pathology)){
						pathoHasSample.put(pathology, new HashSet<String>());
					}
					pathoHasSample.get(pathology).add(sample);
				}
			}
			setHardUpdate(true);
			List<String> columns = new ArrayList<String>();
			try (Results res = DB.select(Schema.HIGHLANDER, "DESCRIBE " + analysis)) {
				while (res.next()){
					columns.add(DB.getDescribeColumnName(Schema.HIGHLANDER, res));
				}
			}
			StringBuilder query = new StringBuilder();
			query.append("INSERT OVERWRITE table "+analysis+" SELECT ");
			for (int i = 0 ; i < columns.size() ; i++){
				if (columns.get(i).equalsIgnoreCase("pathology")) {
					query.append("CASE ");
					for (String pathology : pathoHasSample.keySet()){
						query.append("WHEN sample IN ("+HighlanderDatabase.makeSqlList(pathoHasSample.get(pathology), String.class)+") THEN '"+pathology+"' ");
					}
					query.append("ELSE `"+columns.get(i)+"` END AS `"+columns.get(i)+"`, ");					
				}else if (columns.get(i).equalsIgnoreCase("outsourcing")){
					query.append("CASE ");
					query.append("WHEN platform = 'HISEQ_2000' THEN '1000G' ");
					query.append("ELSE `"+columns.get(i)+"` END AS `"+columns.get(i)+"`, ");					
				}else{
					query.append("`"+columns.get(i) + "`, ");
				}
			}
			query.deleteCharAt(query.length()-1);
			query.deleteCharAt(query.length()-1);
			query.append(" FROM "+analysis);
			DB.update(Schema.HIGHLANDER, query.toString());
			setHardUpdate(false);
		}
		System.out.println("done");
	}

	public void updateAnnotations(List<? extends AnalysisFull> analyses, Set<Annotation> annotations, int nthreads) throws Exception {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd - HH_mm_ss");
		Map<AnalysisFull, Set<Annotation>> map = new HashMap<>();
		for (AnalysisFull analysis : analyses){
			Set<Annotation> set = new HashSet<>();
			for(Annotation annotation : annotations){
				if (annotation.getAnnotationType() == AnnotationType.STATIC) {
					if (annotation.hasDatabaseSchema()) {
						Schema schema = annotation.getDatabaseSchema();
						Reference reference = analysis.getReference();				
						if (!reference.hasSchema(schema)){
							System.out.println("Cannot update annotations of " + annotation + " for " + analysis+ ", schema "+ schema +" doesn't exists for reference " + reference);
						}else {
							System.out.println("Updating annotations of " + annotation + " using "+reference.getSchemaName(schema)+" for " + analysis);
							set.add(annotation);
						}
					}else {
						set.add(annotation);
					}
				}else {
					System.out.println("Cannot update annotations of " + annotation + " for " + analysis+ ", this tool only update STATIC annotations");
				}
			}
			if (!set.isEmpty()) {
				map.put(analysis, set);
			}
		}
		ExecutorService executor = Executors.newFixedThreadPool(nthreads); 
		count = 0;
		setHardUpdate(true);
		for (AnalysisFull analysis : map.keySet()){
			List<Integer> ids = new ArrayList<Integer>(); 
			System.out.println("Analysis " + analysis + " - " + df.format(System.currentTimeMillis()) + " - Retreiving all variants ...");
			try (Results res = DB.select(Schema.HIGHLANDER, 
					"SELECT " + Field.variant_static_id.getQuerySelectName(analysis, false) + " FROM " + analysis.getFromStaticAnnotations(), true)) {
				while (res.next()){
					ids.add(res.getInt(1));
					if (ids.size() == 10_000){
						total += ids.size();
						executor.execute(new AnnotationUpdater(analysis, ids, map.get(analysis)));
						ids = new ArrayList<Integer>(); 
					}
				}			
			}
			if (ids.size() > 0){
				total += ids.size();
				executor.execute(new AnnotationUpdater(analysis, ids, map.get(analysis)));
				ids = new ArrayList<Integer>(); 
			}
			System.out.println(df.format(System.currentTimeMillis()) + " - " + Tools.intToString(count) + " / " + Tools.intToString(total) + " variants updated ..." + " - " + Tools.doubleToString(((double)Tools.getUsedMemoryInMb() / 1024.0), 1, false) + " Gb / "+ (Tools.doubleToString(((double)(Runtime.getRuntime().maxMemory() / 1024 /1024) / 1024.0), 1, false)) + " Gb");
		}
		setHardUpdate(false);
		executor.shutdown();
		executor.awaitTermination(100, TimeUnit.DAYS);
	}
	
	/**
	 * Update only the variants with a SPLICE_SITE_REGION effect
	 * Not accessible from updater directly, to remove when consensus_prediciton is stable
	 * 
	 * @param analyses
	 * @param annotations
	 * @param nthreads
	 * @throws Exception
	 */
	public void updateSpliceSiteConsensus(List<? extends AnalysisFull> analyses, Set<Annotation> annotations, int nthreads) throws Exception {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd - HH_mm_ss");
		Map<AnalysisFull, Set<Annotation>> map = new HashMap<>();
		for (AnalysisFull analysis : analyses){
			Set<Annotation> set = new HashSet<>();
			for(Annotation annotation : annotations){
				if (annotation.getAnnotationType() == AnnotationType.STATIC) {
					if (annotation.hasDatabaseSchema()) {
						Schema schema = annotation.getDatabaseSchema();
						Reference reference = analysis.getReference();				
						if (!reference.hasSchema(schema)){
							System.out.println("Cannot update annotations of " + annotation + " for " + analysis+ ", schema "+ schema +" doesn't exists for reference " + reference);
						}else {
							System.out.println("Updating annotations of " + annotation + " using "+reference.getSchemaName(schema)+" for " + analysis);
							set.add(annotation);
						}
					}else {
						set.add(annotation);
					}
				}else {
					System.out.println("Cannot update annotations of " + annotation + " for " + analysis+ ", this tool only update STATIC annotations");
				}
			}
			if (!set.isEmpty()) {
				map.put(analysis, set);
			}
		}
		ExecutorService executor = Executors.newFixedThreadPool(nthreads); 
		count = 0;
		setHardUpdate(true);
		for (AnalysisFull analysis : map.keySet()){
			List<Integer> ids = new ArrayList<Integer>(); 
			System.out.println("Analysis " + analysis + " - " + df.format(System.currentTimeMillis()) + " - Retreiving all variants ...");
			try (Results res = DB.select(Schema.HIGHLANDER, 
					"SELECT " + Field.variant_static_id.getQuerySelectName(analysis, false) + " FROM " + analysis.getFromStaticAnnotations() + " WHERE INSTR(snpeff_all_effects, 'SPLICE_SITE_REGION')", true)) {
					//"SELECT " + Field.variant_static_id.getQuerySelectName(analysis, false) + " FROM " + analysis.getFromStaticAnnotations() + " WHERE INSTR(snpeff_all_effects, 'SPLICE_SITE_REGION') AND pos IN (39988257, 205745889, 88735052, 141679756, 84060461) ", true)) {
				while (res.next()){
					ids.add(res.getInt(1));
					if (ids.size() == 10_000){
						total += ids.size();
						executor.execute(new AnnotationUpdater(analysis, ids, map.get(analysis)));
						ids = new ArrayList<Integer>(); 
					}
				}			
			}
			if (ids.size() > 0){
				total += ids.size();
				executor.execute(new AnnotationUpdater(analysis, ids, map.get(analysis)));
				ids = new ArrayList<Integer>(); 
			}
			System.out.println(df.format(System.currentTimeMillis()) + " - " + Tools.intToString(count) + " / " + Tools.intToString(total) + " variants updated ..." + " - " + Tools.doubleToString(((double)Tools.getUsedMemoryInMb() / 1024.0), 1, false) + " Gb / "+ (Tools.doubleToString(((double)(Runtime.getRuntime().maxMemory() / 1024 /1024) / 1024.0), 1, false)) + " Gb");
		}
		setHardUpdate(false);
		executor.shutdown();
		executor.awaitTermination(100, TimeUnit.DAYS);
	}

	private class AnnotationUpdater implements Runnable {
		private final AnalysisFull analysis;
		private List<Integer> ids;
		private final Set<Annotation> annotations;

		public AnnotationUpdater(final AnalysisFull analysis, List<Integer> ids, final Set<Annotation> annotations){
			this.analysis = analysis;
			this.ids = ids;
			this.annotations = annotations;
		}

		public void run(){
			try{
				DateFormat df = new SimpleDateFormat("yyyy-MM-dd - HH_mm_ss");
				try (Connection con = DB.getConnection(null, Schema.HIGHLANDER)){
					try (Statement statement = con.createStatement()){
						for (int i=0 ; i < ids.size() ; i++){
							AnnotatedVariant annotatedVariant = new AnnotatedVariant(analysis);
							annotatedVariant.fetchStaticAndGeneAnnotations(ids.get(i));
							for(Annotation annotation : annotations){
								switch(annotation){
								case DBNSFP:
									if ((VariantType)annotatedVariant.getValue(Field.variant_type) == VariantType.SV) {
										annotatedVariant.setDBNSFP(false, false, true, true);
									}else {
										annotatedVariant.setDBNSFP(true, true, true, true);										
									}
									break;
								case ENSEMBL:
									Gene gene = new Gene(analysis.getReference(), annotatedVariant.getValue(Field.chr).toString(), annotatedVariant.getValue(Field.gene_symbol).toString(), null, null, null, null);
									gene.setEnsemblGene(DBUtils.getEnsemblGene(analysis.getReference(), annotatedVariant.getValue(Field.gene_symbol).toString()));
									gene.setEnsemblTranscript(DBUtils.getEnsemblCanonicalTranscript(analysis.getReference(), gene.getEnsemblGene()));
									gene.setRefSeqTranscript(DBUtils.getAccessionRefSeqMRna(analysis.getReference(), gene.getEnsemblTranscript()));
									gene.setBiotype(DBUtils.getBiotype(analysis.getReference(), gene.getEnsemblTranscript()));
									annotatedVariant.setEnsembl(gene, true);
									break;
								case CONSENSUS:
									annotatedVariant.setConsensusPrediction(new Gene(analysis.getReference(), annotatedVariant.getValue(Field.chr).toString(), annotatedVariant.getValue(Field.gene_symbol).toString(), 
											((annotatedVariant.getValue(Field.gene_ensembl) != null) ? annotatedVariant.getValue(Field.gene_ensembl).toString() : null), 
											((annotatedVariant.getValue(Field.transcript_ensembl) != null) ? annotatedVariant.getValue(Field.transcript_ensembl).toString() : null), 
											((annotatedVariant.getValue(Field.transcript_refseq_mrna) != null) ? annotatedVariant.getValue(Field.transcript_refseq_mrna).toString() : null), 
											((annotatedVariant.getValue(Field.biotype) != null) ? annotatedVariant.getValue(Field.biotype).toString() : null)
											));
									break;
								default:
									annotatedVariant.setAnnotation(annotation);
									break;
								}
								if (annotation == Annotation.DBNSFP) {
									annotatedVariant.setConsensusPrediction(new Gene(analysis.getReference(), annotatedVariant.getValue(Field.chr).toString(), annotatedVariant.getValue(Field.gene_symbol).toString(), 
											((annotatedVariant.getValue(Field.gene_ensembl) != null) ? annotatedVariant.getValue(Field.gene_ensembl).toString() : null), 
											((annotatedVariant.getValue(Field.transcript_ensembl) != null) ? annotatedVariant.getValue(Field.transcript_ensembl).toString() : null), 
											((annotatedVariant.getValue(Field.transcript_refseq_mrna) != null) ? annotatedVariant.getValue(Field.transcript_refseq_mrna).toString() : null), 
											((annotatedVariant.getValue(Field.biotype) != null) ? annotatedVariant.getValue(Field.biotype).toString() : null)
											));
								}
							}
							statement.addBatch(annotatedVariant.getAnnotationsUpdateStatement(annotations));
						}
						statement.executeBatch();
						count += ids.size();
						System.out.println(df.format(System.currentTimeMillis()) + " - " + Tools.intToString(count) + " / " + Tools.intToString(total) + " variants updated ..." + " - " + Tools.doubleToString(((double)Tools.getUsedMemoryInMb() / 1024.0), 1, false) + " Gb / "+ (Tools.doubleToString(((double)(Runtime.getRuntime().maxMemory() / 1024 /1024) / 1024.0), 1, false)) + " Gb");
					}catch(Exception ex){
						ex.printStackTrace();
					}
				}
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
	}

	public void updateRunMetrics(List<? extends AnalysisFull> analyses){
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd - HH_mm_ss");
		try{
			for (AnalysisFull analysis : analyses){
				System.out.println("Updating analysis " + analysis);
				Map<String,Integer> availableSamples = new HashMap<String,Integer>();
				try (Results res = DB.select(Schema.HIGHLANDER, "SELECT `value` FROM "+analysis.getFromPossibleValues()+" WHERE field = 'sample'")) {
					while (res.next()){
						availableSamples.put(res.getString(1),0);
					}
				}
				if (!availableSamples.isEmpty()){
					try (Results res = DB.select(Schema.HIGHLANDER, "SELECT sample, project_id FROM projects WHERE sample IN ("+HighlanderDatabase.makeSqlList(availableSamples.keySet(), String.class)+")")) {
						while (res.next()){
							availableSamples.put(res.getString(1),res.getInt(2));
						}
					}
				}
				setHardUpdate(true);
				for (int project_id : availableSamples.values()){
					DbBuilder.updateAnalysisMetrics(project_id, analysis);
				}
				setHardUpdate(false);
			}
		}catch (Exception ex){
			Tools.exception(ex);
			System.out.println(df.format(System.currentTimeMillis()) + "Problem during run metrics update !");
			setHardUpdate(false);
		}
	}

	/**
	 * Copy parameter file from 'reference' to each directory matching an Ion Torrent sample.
	 * Check which samples and which set of parameters to copy using the Ion Importer database.
	 */
	public void updateIonTorrentFiles(){
		try {
			File results = new File("/data/highlander/results");
			for (File projectDir : results.listFiles()){
				if (projectDir.isDirectory()){
					for (File analysisDir : projectDir.listFiles()){
						if (analysisDir.getName().equals("IT")){
							for (File sampleDir : analysisDir.listFiles()){
								if (sampleDir.isDirectory()){
									String[] project = projectDir.getName().split("_");
									try (Results res = DB.select(Schema.HIGHLANDER, "SELECT PANEL_NAME, CALLER FROM ion_importer WHERE RUN_ID = '"+project[0]+"' AND RUN_DATE = '"+project[1]+"_"+project[2]+"_"+project[3]+"' AND SAMPLE = '"+sampleDir.getName().split("\\.")[0]+"'")) {
										if (res.next()){
											String caller = res.getString(2);
											String jsonfilename = "";
											if (caller.equals("PGM_GERMLINE_LOW_STRINGENCY")){
												jsonfilename = "pgm_germline_low_stringency.json";
											}else if (caller.equals("PGM_SOMATIC_LOW_STRINGENCY")){
												jsonfilename = "pgm_somatic_low_stringency.json";
											}else if (caller.equals("PROTON_GERMLINE_LOW_STRINGENCY")){
												jsonfilename = "proton_germline_low_stringency.json";
											}else if (caller.equals("PROTON_SOMATIC_LOW_STRINGENCY")){
												jsonfilename = "proton_somatic_low_stringency.json";
											}
											FileUtils.copyFile(new File("/data/highlander/reference/iontorrent/"+jsonfilename), new File(sampleDir.getAbsolutePath()+"/"+sampleDir.getName()+".json"));
											//System.out.println("Copy /data/highlander/reference/iontorrent/"+jsonfilename+" to "+sampleDir.getAbsolutePath()+"/"+sampleDir.getName()+".json");
											for (File file : sampleDir.listFiles()){
												if (file.getName().endsWith(".bed")){
													FileUtils.copyFile(new File("/data/highlander/reference/panel/"+res.getString(1)+".bed"),new File(sampleDir.getAbsolutePath()+"/"+projectDir.getName()+".bed"));
													//System.out.println("Copy /data/highlander/reference/panel/"+res.getString(1)+".bed to "+sampleDir.getAbsolutePath()+"/"+projectDir.getName()+".bed");
												}
											}										
										}else{
											System.err.println("No info found for " + sampleDir.getAbsolutePath());
										}
									}
								}
							}
						}
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void ngsLogistics(List<? extends Analysis> analyses, String bamRepository){
		File sheet = new File(bamRepository+"/SampleSheet.txt");
		try{
			Map<String, String> lines = new HashMap<String, String>();
			try (Results res = DB.select(Schema.HIGHLANDER, "SELECT sample, platform, run_name, a.reference, a.analysis, bam_dir "
					+ "FROM projects as p "
					+ "JOIN projects_analyses as pa USING (project_id)"
					+ "JOIN analyses as a USING (analysis) "
					+ "WHERE a.analysis IN ("+HighlanderDatabase.makeSqlList(analyses, String.class)+")")) {
				Map<String, Analysis> chosenAnalysis = new HashMap<String, Analysis>();
				while (res.next()){
					String PI = "Miikka_Vikkula";
					if (res.getString("run_name").equalsIgnoreCase("COULIE")){
						PI = "Pierre_Coulie";
					}else if (res.getString("run_name").equalsIgnoreCase("CONSTANTINESCU")){
						PI = "Stefan_Constantinescu";
					}
					Analysis analysis = new Analysis(res.getString("analysis"));
					String reference = res.getString("reference");
					String referencePath = "/data/highlander/reference/bundle_gatk_2_8/b37/human_g1k_v37.fasta";
					String dbsnp = "/data/highlander/reference/bundle_gatk_2_8/b37/dbsnp_138.b37.vcf";
					String chr = "0";					
					String platform = res.getString("platform").toUpperCase();
					if (platform.contains("HISEQ") || platform.contains("X") || platform.contains("NOVASEQ") || platform.contains("MINISEQ") || platform.contains("MISEQ") || platform.contains("NEXTSEQ")){
						referencePath = "/data/highlander/reference/bundle_gatk_2_8/b37/human_g1k_v37.fasta";
						dbsnp = "/data/highlander/reference/bundle_gatk_2_8/b37/dbsnp_138.b37.vcf";
						chr = "0";					
					}else if (platform.contains("ION_TORRENT") || platform.contains("PROTON")){
						referencePath = "/data/highlander/reference/iontorrent/hg19.fasta";
						dbsnp = "/data/highlander/reference/iontorrent/dbsnp_138.b37.chr.vcf";
						chr = "chr";
					}else if (platform.contains("SOLID")){
						referencePath = "/data/highlander/reference/lifescope/human_hg19.fa";
						dbsnp = "/data/highlander/reference/lifescope/dbsnp_138.lifescope.vcf";
						chr = "chr";
					}else if (platform.contains("?")){
						//Other option
						referencePath = "/data/highlander/reference/bundle_gatk_2_8/hg19/ucsc.hg19.fasta";
						dbsnp = "/data/highlander/reference/bundle_gatk_2_8/hg19/dbsnp_138.hg19.vcf";
						chr = "chr";
					}
					String sampleId = res.getString("sample");
					if (sampleId.contains(".")) {
						sampleId = sampleId.split("\\.")[0];
					}
					String line = res.getString("sample") + "\t" + PI + "\t" + "UCL" +"\t" + "Research" +"\t" + analysis + "\t" + reference + "\t" + res.getString("bam_dir")+res.getString("sample")+".bam" + "\t" + referencePath + "\t" + dbsnp + "\t" + chr + "\n";
					if (chosenAnalysis.containsKey(sampleId)){
						if (analyses.indexOf(analysis) < analyses.indexOf(chosenAnalysis.get(sampleId))){
							lines.put(sampleId, line);
							chosenAnalysis.put(sampleId, analysis);						
						}
					}else{
						lines.put(sampleId, line);
						chosenAnalysis.put(sampleId, analysis);
					}
				}
			}
			try (FileWriter fw = new FileWriter(sheet)){
				for (String line : lines.values()){
					fw.write(line);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public static void showHelp(Tool tool){
		System.out.println("Highlander DbUpdater version "+version);
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
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try{
Map<ToolArgument, String> arguments = new EnumMap<>(ToolArgument.class);
			
			Tool tool = null;
			boolean help = false;
			arguments.put(ToolArgument.report, "0");
			
			for (int i=0 ; i < args.length ; i++){
				if (args[i].equals("--tool") || args[i].equals("-T")){
					try {
						tool = Tool.valueOf(args[++i]);
					}catch (Exception e) {
						System.err.println(args[i]+" is not an existing tool.");
						showHelp(null);
						System.exit(0);
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
			
			if (tool == null){
				showHelp(null);
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
					int nthreads = 4;
					if (arguments.containsKey(ToolArgument.threads)) {
						nthreads = Integer.parseInt(arguments.get(ToolArgument.threads));
					}
					DbUpdater dbu = new DbUpdater(arguments.get(ToolArgument.config), nthreads);
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
					Set<Annotation> annotations = new HashSet<Annotation>();
					if (arguments.containsKey(ToolArgument.source)) {
						for (String s : arguments.get(ToolArgument.source).split("\\+")){
							annotations.add(Annotation.valueOf(s));
						}
					}
					switch(tool){
					case annotations:
						dbu.updateAnnotations(analyses, annotations, Integer.parseInt(arguments.get(ToolArgument.threads)));
						//dbu.updateSpliceSiteConsensus(analyses, annotations, Integer.parseInt(arguments.get(ToolArgument.threads)));
						break;
					case ngslogistics:
						dbu.ngsLogistics(analyses, arguments.get(ToolArgument.repository));
						break;
					case runmetrics:
						dbu.updateRunMetrics(analyses);
						break;
					case softupdate:
						dbu.setSoftUpdate(arguments.get(ToolArgument.bool).equals("1"));
						break;
					case updateiontorrent:
						dbu.updateIonTorrentFiles();
						break;
					case pop1000g:
						dbu.update1000g(analyses);
						break;
					}
					dbu.DB.disconnectAll();
				}else{
					//All current tools need database
					switch(tool){
					default:
						System.err.println("Database needed");
						break;
					}
				}
			}else{
				for (ToolArgument ta : missingMandatoryArguments) {
					System.err.println(tool.getArgumentText(ta));
					System.exit(-1);
				}
			}
		}catch (Exception ex) {
			Tools.exception(ex);
			System.err.println(ex.getMessage());
			System.exit(1);
		}
	}

}

