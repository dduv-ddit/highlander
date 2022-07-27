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

package be.uclouvain.ngs.highlander.datatype.filter;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.toolbar.FilteringPanel;
import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.VariantResults;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.Analysis;

public abstract class Filter extends JPanel {

	/**
	 * Symbols used to encode filters in the database 
	 * 
	 * Combo: 				€ ^  $  §  { }
	 * Custom: 				! ?  & |  #  [ ]
	 * Magic:
	 * - SPECIFIC: 		& |  #  !  µ  
	 * - COM_GENE: 		|  #  
	 * - COMMON: 			|  #  µ  
	 * - INTERVALS:		|  #  µ  
	 * - COMB_HET: 		#  
	 * - SAME_CODON: 	#  µ  
	 * - MNP: 				#  µ  
	 * VariantList:		°
	 * Template: 			°	@
	 * 
	 * Folders are separated by	~
	 *  
	 *  µ is used to separate magic filter and it's prefiltering. For backward compatibility, prefiltering is not always equal to NO_PREFILTERING when empty, but can be inexistant. So DON'T use µ for any other purpose.
	 *  
	 * Available 'reserved' symbols:		£ *		(they already are forbidden in filters and highlightings) 
	 */

	public static boolean containsForbiddenCharacters(String string) {
		if (string.contains("&") || 
				string.contains("|") ||
				string.contains("[") ||
				string.contains("]") ||
				string.contains("#") ||
				string.contains("!") ||
				string.contains("$") ||
				string.contains("°") ||
				string.contains("?") ||
				string.contains("£") ||
				string.contains("€") ||
				string.contains("^") ||
				string.contains("§") ||
				string.contains("@") ||
				string.contains("*") ||
				string.contains("µ") ||
				string.contains("{") ||
				string.contains("}") ){
			return true;				
		}else {
			return false;
		}
	}
	
	public static String getForbiddenCharacters() {
		return "& | # € ^ $ £ ° § ! ? @ * µ [ ] { }";
	}
	
	public enum LogicalOperator {AND,OR}

	public enum FilterType {
		VARIANTS_COMMON_TO_SAMPLES("Variants common to samples",
				"Variants intersection:\n" +
						"Keeps only the variants common to a set of samples.\n" +
						"In addition, for each sample, you can set a (possibly different) value for some fields:\n" +
						" zygosity, allelic depth and allelic depth proportion.\n" +
				"For example, you could obtain the common variants in a family, heterozygous in the parents and homozygous in the child.\n"),
		SAMPLE_SPECIFIC_VARIANTS("Sample-specific variants",
				"Variants relative complement:\n" +
						"Exclude variants found in a set of samples from another set of samples.\n" +
						"For example, you can exclude variants found in the blood of a patient and keep only the tissue-specific variants.\n" +
				"You could also exclude the variants found in a control sample, from all other samples."),
		COMMON_GENE_VARIANTS("Common-gene variants",
				"Keep only variants found in `common` genes (i.e. genes that have at least one variant in all (or less) given samples).\n" +
				"In addition, you can set unisnp_ids, consensus_prediction and filters criteria."),
		COMBINED_HETEROZYGOUS_VARIANTS("Combined-heterozygous variants",
				"Given a trio (2 parents and their child), find all potential combined-heterozygous variants.\n" +
						"Each of the variants found in the child, should be present in only one of the parents,\n" +
				"and at least two such variants should be present in the same gene."),
		PATHOLOGY_FREQUENCY("Allele frequency in a pathology",
				"Filter on the allele frequency within a specific pathology."),
		INTERVALS("Genomic intervals",
				"All variants contained in the given set of genomic intervals.\nAn interval is defined by chromosome plus starting and ending positions, and can be imported from a bed file."),
		SAME_CODON("Multiple variants in same codon",
				"Find all the codons affected by 2 or 3 variants for a given list of samples."),
		MULTIPLE_NUCLEOTIDES_VARIANTS("MNV",
				"Find all the MNV (Multiple Nucleotides Variants) for a given list of samples.\nMNV's are sets of two or more variants with consecutive positions."),
		CUSTOM("Custom filter",
				"Standard custom filter composed of one or more criteria.\n" +
						"A criterion can be either:\n" +
						"- A simple criterion composed of a field, a comparion operator (like =, >, etc) and a list of values\n" +
				"- A combination of simple criteria joined by a logical operator (AND or OR)"),  	
		COMBO("Combo filter",
				"Combination of any kind of filter that can be either:\n" +
						"- A Custom Filter\n" +
						"- A Magic Filter\n" +
				"- A combination of Combo Filters joined by a logical operator, AND to get the intersection or OR to get the union of resulting variants"),
		LIST_OF_VARIANTS("List of variants",
				"Internal filter used to retreive a list of variants given a list of id's.");  	
		private final String name;
		private final String description;
		FilterType(String name, String description){this.name = name;this.description = description;}
		public String getName(){return name;}
		public String getDescription() {return description;}
	}

	protected Filter parentFilter = null;
	protected FilteringPanel filteringPanel;

	public Filter(){
		super();
	}

	public Filter getParentFilter(){
		return parentFilter;
	}

	public abstract void setFilteringPanel(FilteringPanel filteringPanel);

	public abstract FilterType getFilterType();

	public abstract boolean isSimple();
	public abstract boolean isComplex();
	public abstract Filter getSubFilter(int index);
	public abstract int getSubFilterCount();
	public abstract LogicalOperator getLogicalOperator();

	public abstract boolean hasSamples();

	/**
	 * Return all samples really involved in the filter, 
	 * even if they are retreived by another field than 'sample' (e.g. via 'pathology').
	 * i.e. included samples or all samples of the analysis if none were included. 
	 * @return
	 */
	public Set<String> getSamples(){
		Set<String> samples = getIncludedSamples();
		if (samples.isEmpty()){
			try{
				try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
						"SELECT `value` as sample "
						+ "FROM " + Highlander.getCurrentAnalysis().getFromPossibleValues() 
						+ "WHERE field = 'sample'"
						)) {
					while (res.next()){
						samples.add(res.getString("sample"));
					}
				}
			}catch(Exception ex){
				Tools.exception(ex);
			}
		}
		return samples;
	}

	/**
	 * Return all samples included and excluded by the user in this filter.
	 * So only the samples defined manually using 'sample' field are returned 
	 * (not for example, sample that coudl appear in the results if the 'pathology' field is used).
	 * @param boolean includeProfileList	if true, samples from profile lists are retreived and included, if false the're not
	 * @return
	 */
	public abstract Set<String> getUserDefinedSamples(boolean includeProfileList);	

	/**
	 * Return all samples included and excluded in this filter, 
	 * even if they are retreived by another field than 'sample' (e.g. via 'pathology').
	 * This method is mainly used to auto-generate a list of samples for a filter which has none (and so could be really slow on large databases).
	 * @return
	 */
	public Set<String> getAllSamples(){
		Set<String> set = getIncludedSamples();
		set.addAll(getExcludedSamples());
		return set;	
	}

	/**
	 * Return all samples included in the filter, 
	 * even if they are retreived by another field than 'sample' (e.g. via 'pathology').
	 * i.e. samples entered by the user and keeped by the filter
	 * @return
	 */
	public abstract Set<String> getIncludedSamples();

	/**
	 * Return all samples excluded by the filter,
	 * i.e. samples entered by the user and excluded by filters like "Sample specific".
	 * @return
	 */
	public abstract Set<String> getExcludedSamples();

	public abstract String toHtmlString();

	public abstract String getSaveString();

	public abstract String parseSaveString(String saveString);

	public abstract Filter loadCriterion(FilteringPanel filteringPanel, String saveString) throws Exception ;

	public abstract boolean isFilterValid();

	public abstract List<String> getValidationProblems();

	public abstract boolean checkFieldCompatibility(Analysis analysis);

	public abstract boolean changeAnalysis(Analysis analysis);

	public abstract void editFilter();

	public abstract void delete();

	public abstract Map<Integer,String> getResultIds(Set<String> autoSamples) throws Exception;

	protected abstract List<Field> getQueryWhereFields() throws Exception;

	protected abstract String getQueryWhereClause(boolean includeTableWithJoinON) throws Exception;

	protected abstract VariantResults extractResults(Results res, List<Field> headers, String progressTxt, boolean indeterminateProgress) throws Exception ;

	protected abstract int getNumberOfVariants();

	public VariantResults retreiveData(List<Field> headers, Set<String> autoSamples, String progressTxt) throws Exception{
		Highlander.waitingPanel.start(true);

		String query = getQuery(headers, autoSamples); 

		Tools.print("Submitting query to the database (filter): ");
		System.out.println(query.toString());
		Highlander.waitingPanel.setProgressString(progressTxt + " (submission)", true);
		long timeQueryStart = System.currentTimeMillis();
		VariantResults variantResults = null;
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, query.toString(), true)) {				
			long timeRetreiveStart = System.currentTimeMillis();
			int nrow = getNumberOfVariants();
			if (nrow == -1) nrow = res.getResultSetSize();
			if (nrow != -1){
				Highlander.waitingPanel.setProgressMaximum(nrow);
				Highlander.waitingPanel.setProgressString(progressTxt + " (retreiving "+nrow+" variants)", false);
			}else{
				Highlander.waitingPanel.setProgressString(progressTxt + " (retreiving variants)", true);
			}
			variantResults = extractResults(res, headers, progressTxt, (nrow == -1));

			Highlander.waitingPanel.setProgressDone();
			try{
				showProfilingData(timeQueryStart, timeRetreiveStart);
			}catch(Exception ex){
				Tools.exception(ex);
			}
			Highlander.waitingPanel.stop();
		}
		return variantResults;
	}

	public int retreiveCount(Set<String> autoSamples) throws Exception{
		Highlander.waitingPanel.start(true);

		Analysis analysis = Highlander.getCurrentAnalysis();

		boolean includeStatic = false; 
		boolean includeCustom = false; 
		boolean includeGene = false; 
		boolean includeProjects = false; 
		boolean includePathologies = false; 
		boolean includePopulations = false; 
		boolean includeAlleleFrequencies = false;
		boolean includeUserEvaluations = false;
		boolean includeUserNumEvaluations = false;
		boolean includeUserVariantsPublic = false;
		boolean includeUserVariantsPrivate = false;
		boolean includeUserGenesPublic = false;
		boolean includeUserGenesPrivate = false;
		boolean includeUserSamplesPublic = false;
		boolean includeUserSamplesPrivate = false;

		boolean includeTableWithJoinON = false;
		
		for (Field field : getQueryWhereFields()){
			if (field.getTable(analysis).equalsIgnoreCase(analysis.getTableStaticAnnotations())){
				includeStatic = true;
			}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTableCustomAnnotations())){
				includeCustom = true;
			}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTableGeneAnnotations())){
				includeGene = true;
			}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTableProjects())){
				includeProjects = true;
			}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTablePathologies())){
				includeProjects = true;
				includePathologies = true;
			}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTablePopulations())){
				includeProjects = true;
				includePopulations = true;
			}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTableAlleleFrequencies())){
				includeAlleleFrequencies = true;
			}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTableUserAnnotationsEvaluations())){
				includeUserEvaluations = true;
			}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTableUserAnnotationsNumEvaluations())){
				includeUserNumEvaluations = true;
			}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTableUserAnnotationsVariants())){
				if (field.toString().equalsIgnoreCase("variant_comments_public")) {
					includeUserVariantsPublic = true;
				}else {
					includeUserVariantsPrivate = true;
				}
				includeTableWithJoinON = true;
			}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTableUserAnnotationsGenes())){
				if (field.toString().equalsIgnoreCase("gene_comments_public")) {
					includeUserGenesPublic = true;
				}else {
					includeUserGenesPrivate = true;
				}
				includeTableWithJoinON = true;
			}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTableUserAnnotationsSamples())){
				if (field.toString().equalsIgnoreCase("sample_comments_public")) {
					includeUserSamplesPublic = true;
				}else {
					includeUserSamplesPrivate = true;
				}
				includeTableWithJoinON = true;
			}
		}
		
		String whereClause = getQueryWhereClause(includeTableWithJoinON);
		
		if (whereClause.length() > 0){
			if (!hasSamples() && !autoSamples.isEmpty() && !whereClause.contains(Field.variant_sample_id.getQueryWhereName(analysis, includeTableWithJoinON) + " IN (") && !whereClause.contains(Field.variant_sample_id.getQueryWhereName(analysis, includeTableWithJoinON) + " IS NULL")){
				System.out.println();
				System.out.println("----------------------");
				System.out.println(toString());
				System.out.println("This filter has no sample list defined by the user");
				System.out.println("Adding samples: " + autoSamples);
				System.out.println("----------------------");
				whereClause = "sample IN ("+HighlanderDatabase.makeSqlList(autoSamples, String.class)+") AND ("+whereClause+")"; 
				includeProjects = true;
			}
		}

		StringBuilder query = new StringBuilder();
		query.append("SELECT COUNT(*) ");
		query.append("FROM "+analysis.getFromSampleAnnotations());		
		if (includeStatic) query.append(analysis.getJoinStaticAnnotations());		
		if (includeCustom) query.append(analysis.getJoinCustomAnnotations());		
		if (includeGene) query.append(analysis.getJoinGeneAnnotations());		
		if (includeProjects) query.append(analysis.getJoinProjects());		
		if (includePathologies) query.append(analysis.getJoinPathologies());		
		if (includePopulations) query.append(analysis.getJoinPopulations());		
		if (includeAlleleFrequencies) query.append(analysis.getJoinAlleleFrequencies());		
		if (includeUserEvaluations) query.append(analysis.getJoinUserAnnotationsEvaluations());		
		if (includeUserNumEvaluations) query.append(analysis.getJoinUserAnnotationsNumEvaluations());		
		if (includeUserVariantsPrivate) query.append(analysis.getJoinUserAnnotationsVariantsPrivate());		
		if (includeUserVariantsPublic) query.append(analysis.getJoinUserAnnotationsVariantsPublic());		
		if (includeUserGenesPrivate) query.append(analysis.getJoinUserAnnotationsGenesPrivate());		
		if (includeUserGenesPublic) query.append(analysis.getJoinUserAnnotationsGenesPublic());		
		if (includeUserSamplesPrivate) query.append(analysis.getJoinUserAnnotationsSamplesPrivate());		
		if (includeUserSamplesPublic) query.append(analysis.getJoinUserAnnotationsSamplesPublic());		
		if (whereClause.length() > 0){
				query.append("WHERE ");
				query.append(whereClause);
		}

		Tools.print("Submitting query to the database (filter count): ");
		System.out.println(query.toString());
		Highlander.waitingPanel.setProgressString("Executing query", true);
		int count = 0;
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, query.toString())) {
			if (res.next()){
				count = res.getInt(1);
			}
		}		
		Highlander.waitingPanel.stop();

		return count;
	}

	public String getQuery(List<Field> headers, Set<String> autoSamples) throws Exception {
		Analysis analysis = Highlander.getCurrentAnalysis();

		boolean includeStatic = false; 
		boolean includeCustom = false; 
		boolean includeGene = false; 
		boolean includeProjects = false; 
		boolean includePathologies = false; 
		boolean includePopulations = false; 
		boolean includeAlleleFrequencies = false;
		boolean includeUserEvaluations = false;
		boolean includeUserNumEvaluations = false;
		boolean includeUserVariantsPublic = false;
		boolean includeUserVariantsPrivate = false;
		boolean includeUserGenesPublic = false;
		boolean includeUserGenesPrivate = false;
		boolean includeUserSamplesPublic = false;
		boolean includeUserSamplesPrivate = false;

		boolean includeTableWithJoinON = false;
		
		Set<Field> headersAndWhereClause = new LinkedHashSet<Field>();
		headersAndWhereClause.addAll(headers);
		headersAndWhereClause.addAll(getQueryWhereFields());
		for (Field field : headersAndWhereClause){
			if (field.getTable(analysis).equalsIgnoreCase(analysis.getTableStaticAnnotations())){
				includeStatic = true;
			}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTableCustomAnnotations())){
				includeCustom = true;
			}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTableGeneAnnotations())){
				includeGene = true;
			}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTableProjects())){
				includeProjects = true;
			}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTablePathologies())){
				includeProjects = true;
				includePathologies = true;
			}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTablePopulations())){
				includeProjects = true;
				includePopulations = true;
			}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTableAlleleFrequencies())){
				includeAlleleFrequencies = true;
			}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTableUserAnnotationsEvaluations())){
				includeUserEvaluations = true;
			}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTableUserAnnotationsNumEvaluations())){
				includeUserNumEvaluations = true;
			}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTableUserAnnotationsVariants())){
				if (field.toString().equalsIgnoreCase("variant_comments_public")) {
					includeUserVariantsPublic = true;
				}else {
					includeUserVariantsPrivate = true;
				}
				includeTableWithJoinON = true;
			}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTableUserAnnotationsGenes())){
				if (field.toString().equalsIgnoreCase("gene_comments_public")) {
					includeUserGenesPublic = true;
				}else {
					includeUserGenesPrivate = true;
				}
				includeTableWithJoinON = true;
			}else if (field.getTable(analysis).equalsIgnoreCase(analysis.getTableUserAnnotationsSamples())){
				if (field.toString().equalsIgnoreCase("sample_comments_public")) {
					includeUserSamplesPublic = true;
				}else {
					includeUserSamplesPrivate = true;
				}
				includeTableWithJoinON = true;
			}
		}

		Set<Field> neededFields = new LinkedHashSet<Field>();
		neededFields.addAll(headers);
		neededFields.add(Field.variant_sample_id);
		neededFields.add(Field.chr);
		neededFields.add(Field.pos);
		neededFields.add(Field.length);
		neededFields.add(Field.reference);
		neededFields.add(Field.alternative);
		neededFields.add(Field.gene_symbol);

		String whereClause = getQueryWhereClause(includeTableWithJoinON);
		
		if (whereClause.length() > 0){
			if (!hasSamples() && !autoSamples.isEmpty() && !whereClause.contains(Field.variant_sample_id.getQueryWhereName(analysis, includeTableWithJoinON) + " IN (") && !whereClause.contains(Field.variant_sample_id.getQueryWhereName(analysis, includeTableWithJoinON) + " IS NULL")){
				System.out.println();
				System.out.println("----------------------");
				System.out.println(toString());
				System.out.println("This filter has no sample list defined by the user");
				System.out.println("Adding samples: " + autoSamples);
				System.out.println("----------------------");
				whereClause = "sample IN ("+HighlanderDatabase.makeSqlList(autoSamples, String.class)+") AND ("+whereClause+")"; 
				includeProjects = true;
			}
		}

		StringBuilder query = new StringBuilder();
		query.append("SELECT ");
		for (Field field : neededFields){
			query.append(field.getQuerySelectName(analysis, includeTableWithJoinON)+", ");
		}
		query.delete(query.length()-2, query.length());

		query.append(" FROM "+analysis.getFromSampleAnnotations());		
		if (includeStatic) query.append(analysis.getJoinStaticAnnotations());		
		if (includeCustom) query.append(analysis.getJoinCustomAnnotations());		
		if (includeGene) query.append(analysis.getJoinGeneAnnotations());		
		if (includeProjects) query.append(analysis.getJoinProjects());		
		if (includePathologies) query.append(analysis.getJoinPathologies());		
		if (includePopulations) query.append(analysis.getJoinPopulations());		
		if (includeAlleleFrequencies) query.append(analysis.getJoinAlleleFrequencies());		
		if (includeUserEvaluations) query.append(analysis.getJoinUserAnnotationsEvaluations());		
		if (includeUserNumEvaluations) query.append(analysis.getJoinUserAnnotationsNumEvaluations());		
		if (includeUserVariantsPrivate) query.append(analysis.getJoinUserAnnotationsVariantsPrivate());		
		if (includeUserVariantsPublic) query.append(analysis.getJoinUserAnnotationsVariantsPublic());		
		if (includeUserGenesPrivate) query.append(analysis.getJoinUserAnnotationsGenesPrivate());		
		if (includeUserGenesPublic) query.append(analysis.getJoinUserAnnotationsGenesPublic());		
		if (includeUserSamplesPrivate) query.append(analysis.getJoinUserAnnotationsSamplesPrivate());		
		if (includeUserSamplesPublic) query.append(analysis.getJoinUserAnnotationsSamplesPublic());		

		if (whereClause.length() > 0){
			query.append("WHERE ");
			query.append(whereClause);
		}

		return query.toString();
	}

	protected void showProfilingData(long timeQuery, long timeRetreive) throws Exception {
		System.out.println("Query execution: " + ((timeRetreive - timeQuery)/1000) + " sec");
		System.out.println("Populating table: " + ((System.currentTimeMillis() - timeRetreive)/1000) + " sec");
		/* 
		 * Profiling using MySQL
		 * Works until MySQL 5.5, should look in performance_schema since 5.6
		 * Some part don't work accurately with Impala (will look profile on mysql, not Impala)
		 * So let's just count time between request and results
		 * 
		ResultSet profiling = Highlander.getDB().select(Schema.HIGHLANDER, "SHOW PROFILES");
		if(profiling.last()){
			int last = profiling.getInt(1);
			Highlander.getDB().closeResultSet(profiling);				
			double dur = 0.0, fetch = 0.0;
			profiling = Highlander.getDB().select(Schema.HIGHLANDER, "SHOW PROFILE FOR QUERY " + last);
			while(profiling.next()){
				if (profiling.getString(1).equals("Sending data")){
					fetch = profiling.getDouble(2);
				}else{
					dur += profiling.getDouble(2);
				}
			}
			System.out.println("Query execution: " + Tools.doubleToString(dur, 2, false) + " sec");
			System.out.println("Fetching results: " + Tools.doubleToString(fetch, 2, false) + " sec");
			System.out.println("Populating table: " + ((System.currentTimeMillis() - time)/1000) + " sec");
		}else{
			System.out.println("No profiling data available");
		}
		Highlander.getDB().closeResultSet(profiling);
		 */
	}
}
