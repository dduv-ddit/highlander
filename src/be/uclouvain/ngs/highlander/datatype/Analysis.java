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

import java.io.Serializable;

import be.uclouvain.ngs.highlander.Highlander;

/**
 * 	Analysis only contains analysis name
 * 	AnalysisFull also contains the linked variant caller
 * 	AnalysisFull contains all information available in the database (reference, sequencing target, URLs, icon, file extensions, ...)
 * 	The last 2 extends Analysis.
 * 
 * 	In some case, like when creating a new empty database, you don't have all information yet.
 * 	In other cases, like with some dbBuilder functions, you don't have a database at all, but you manipulate Analysis names.
 * 
 *  "Dividing" the class in 3 makes sure that if a methods needs e.g. the variant caller, it asks for an AnalysisFull (but not an AnalysisFull because it doesn't care about other information). 
 *  
 * @author Raphaël Helaers
 *
 */
public class Analysis implements Serializable, Comparable<Analysis> {

	private static final long serialVersionUID = 1L;
	
	protected String name;

	public Analysis(String name){
		this.name = name;
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
		return name.equals(obj.toString());
	}

	@Override
	public int compareTo (Analysis a) {
		return name.compareTo(a.name);
	} 
	
	public String getTableSampleAnnotations() {
		return name+"_sample_annotations";
	}
	
	public String getTableStaticAnnotations() {
		return name+"_static_annotations";
	}
	
	public String getTableGeneAnnotations() {
		return name+"_gene_annotations";
	}
	
	public String getTableCustomAnnotations() {
		return name+"_custom_annotations";
	}
	
	public String getTableProjects() {
		return "projects";
	}
	
	public String getTablePathologies() {
		return "pathologies";
	}
	
	public String getTablePopulations() {
		return "populations";
	}
	
	public String getTableAlleleFrequencies() {
		return name+"_allele_frequencies";
	}
	
	public String getTableAlleleFrequenciesPerPathology() {
		return name+"_allele_frequencies_per_pathology";
	}
	
	public String getTableUserAnnotationsEvaluations() {
		return name+"_user_annotations_evaluations";
	}
	
	public String getTableUserAnnotationsNumEvaluations() {
		return name+"_user_annotations_num_evaluations";
	}
	
	public String getTableUserAnnotationsVariants() {
		return name+"_user_annotations_variants";
	}
	
	public String getTableUserAnnotationsGenes() {
		return name+"_user_annotations_genes";
	}
	
	public String getTableUserAnnotationsSamples() {
		return name+"_user_annotations_samples";
	}
	
	public String getTablePossibleValues() {
		return name+"_possible_values";
	}
	
	public String getTableCoverage() {
		return name+"_coverage";
	}
	
	public String getTableCoverageRegions() {
		return name+"_coverage_regions";
	}
	
	public String getFromSampleAnnotations() {
		return "`"+getTableSampleAnnotations()+"` ";
	}
	
	public String getFromStaticAnnotations() {
		return "`"+getTableStaticAnnotations()+"` ";
	}
	
	public String getFromGeneAnnotations() {
		return "`"+getTableGeneAnnotations()+"` ";
	}
	
	public String getFromCustomAnnotations() {
		return "`"+getTableCustomAnnotations()+"` ";
	}
	
	public String getFromProjects() {
		return "`"+getTableProjects()+"` ";
	}
	
	public String getFromPathologies() {
		return "`"+getTablePathologies()+"` ";
	}
	
	public String getFromPopulations() {
		return "`"+getTablePopulations()+"` ";
	}
	
	public String getFromAlleleFrequencies() {
		return "`"+getTableAlleleFrequencies()+"` ";
	}
	
	public String getFromAlleleFrequenciesPerPathology() {
		return "`"+getTableAlleleFrequenciesPerPathology()+"` ";
	}
	
	public String getFromUserAnnotationsEvaluations() {
		return "`"+getTableUserAnnotationsEvaluations()+"` ";
	}
	
	public String getFromUserAnnotationsNumEvaluations() {
		return "`"+getTableUserAnnotationsNumEvaluations()+"` ";
	}
	
	public String getFromUserAnnotationsVariants() {
		return "`"+getTableUserAnnotationsVariants()+"` ";
	}
	
	public String getFromUserAnnotationsVariantsPrivate() {
		return "`"+getTableUserAnnotationsVariants()+"` ";
	}
	
	public String getFromUserAnnotationsVariantsPublic() {
		return "`"+getTableUserAnnotationsVariants()+"` AS `"+getTableUserAnnotationsVariants()+"_public` ";
	}
	
	public String getFromUserAnnotationsGenes() {
		return "`"+getTableUserAnnotationsGenes()+"` ";
	}
	
	public String getFromUserAnnotationsGenesPrivate() {
		return "`"+getTableUserAnnotationsGenes()+"` ";
	}
	
	public String getFromUserAnnotationsGenesPublic() {
		return "`"+getTableUserAnnotationsGenes()+"` AS `"+getTableUserAnnotationsGenes()+"_public` ";
	}
	
	public String getFromUserAnnotationsSamples() {
		return "`"+getTableUserAnnotationsSamples()+"` ";
	}
	
	public String getFromUserAnnotationsSamplesPrivate() {
		return "`"+getTableUserAnnotationsSamples()+"` ";
	}
	
	public String getFromUserAnnotationsSamplesPublic() {
		return "`"+getTableUserAnnotationsSamples()+"` AS `"+getTableUserAnnotationsSamples()+"_public` ";
	}
	
	public String getFromPossibleValues() {
		return "`"+getTablePossibleValues()+"` ";
	}
	
	public String getFromCoverage() {
		return "`"+getTableCoverage()+"` ";
	}
	
	public String getFromCoverageRegions() {
		return "`"+getTableCoverageRegions()+"` ";
	}
	
	public String getJoinStaticAnnotations() {
		return "INNER JOIN `"+getTableStaticAnnotations()+"` USING (`pos`,`chr`,`alternative`,`reference`,`length`,`gene_symbol`) ";
	}
	
	public String getJoinCustomAnnotations() {
		return "LEFT JOIN `"+getTableCustomAnnotations()+"` USING (`pos`,`gene_symbol`,`project_id`,`alternative`,`reference`,`chr`,`length`) ";
	}
	
	public String getJoinGeneAnnotations() {
		return "LEFT JOIN `"+getTableGeneAnnotations()+"` USING (`gene_symbol`) ";
	}
	
	public String getJoinProjects() {
		return "INNER JOIN `"+getTableProjects()+"` USING (`project_id`) ";
	}
	
	public String getJoinPathologies() {
		return "INNER JOIN `"+getTablePathologies()+"` USING (`pathology_id`) ";
	}
	
	public String getJoinPopulations() {
		return "LEFT JOIN `"+getTablePopulations()+"` USING (`population_id`) ";
	}
	
	public String getJoinCoverage() {
		return "LEFT JOIN `"+getTableCoverage()+"` USING (`region_id`) ";
	}
	
	public String getJoinAlleleFrequencies() {
		return "INNER JOIN `"+getTableAlleleFrequencies()+"` USING (`pos`,`alternative`,`reference`,`chr`,`length`) ";
	}
	
	public String getJoinAlleleFrequenciesPerPathology(boolean joinOnPathologyId) {
		return "INNER JOIN `"+getTableAlleleFrequenciesPerPathology()+"` USING (`pos`,`alternative`,`reference`,`chr`,`length`"+(joinOnPathologyId?",`pathology_id`":"")+") ";
	}
	
	public String getJoinUserAnnotationsEvaluations() {
		return "LEFT JOIN `"+getTableUserAnnotationsEvaluations()+"` USING (`pos`,`alternative`,`reference`,`chr`,`length`,`gene_symbol`,`project_id`) ";
	}
	
	public String getJoinUserAnnotationsNumEvaluations() {
		return "LEFT JOIN `"+getTableUserAnnotationsNumEvaluations()+"` USING (`pos`,`alternative`,`reference`,`chr`,`length`,`gene_symbol`) ";
	}

	public String getJoinUserAnnotationsVariantsPrivate() {
		return "LEFT JOIN `"+getTableUserAnnotationsVariants()
				+"` ON `"+getTableStaticAnnotations()+"`.`pos` = `"+getTableUserAnnotationsVariants()+"`.`pos` "
				+ "AND `"+getTableStaticAnnotations()+"`.`chr` = `"+getTableUserAnnotationsVariants()+"`.`chr` "
				+ "AND `"+getTableStaticAnnotations()+"`.`alternative` = `"+getTableUserAnnotationsVariants()+"`.`alternative` "
				+ "AND `"+getTableStaticAnnotations()+"`.`reference` = `"+getTableUserAnnotationsVariants()+"`.`reference` "
				+ "AND `"+getTableStaticAnnotations()+"`.`length` = `"+getTableUserAnnotationsVariants()+"`.`length` "
				+ "AND `"+getTableStaticAnnotations()+"`.`gene_symbol` = `"+getTableUserAnnotationsVariants()+"`.`gene_symbol` "
				+ "AND `"+getTableUserAnnotationsVariants()+"`.`username` = '"+Highlander.getLoggedUser().getUsername()+"' ";
	}

	public String getJoinUserAnnotationsVariantsPublic() {
		return "LEFT JOIN `"+getTableUserAnnotationsVariants()+"` AS `"+getTableUserAnnotationsVariants()+"_public` "
				+ "ON `"+getTableStaticAnnotations()+"`.`pos` = `"+getTableUserAnnotationsVariants()+"_public`.`pos` "
				+ "AND `"+getTableStaticAnnotations()+"`.`chr` = `"+getTableUserAnnotationsVariants()+"_public`.`chr` "
				+ "AND `"+getTableStaticAnnotations()+"`.`alternative` = `"+getTableUserAnnotationsVariants()+"_public`.`alternative` "
				+ "AND `"+getTableStaticAnnotations()+"`.`reference` = `"+getTableUserAnnotationsVariants()+"_public`.`reference` "
				+ "AND `"+getTableStaticAnnotations()+"`.`length` = `"+getTableUserAnnotationsVariants()+"_public`.`length` "
				+ "AND `"+getTableStaticAnnotations()+"`.`gene_symbol` = `"+getTableUserAnnotationsVariants()+"_public`.`gene_symbol` "
				+ "AND `"+getTableUserAnnotationsVariants()+"_public`.`username` = 'PUBLIC' ";
	}

	public String getJoinUserAnnotationsGenesPrivate() {
		return "LEFT JOIN `"+getTableUserAnnotationsGenes()+"` "
				+ "ON `"+getTableStaticAnnotations()+"`.`gene_symbol` = `"+getTableUserAnnotationsGenes()+"`.`gene_symbol` "
				+ "AND `"+getTableUserAnnotationsGenes()+"`.`username` = '"+Highlander.getLoggedUser().getUsername()+"' ";
	}

	public String getJoinUserAnnotationsGenesPublic() {
		return "LEFT JOIN `"+getTableUserAnnotationsGenes()+"` AS `"+name+"_user_annotations_genes_public` "
				+ "ON `"+getTableStaticAnnotations()+"`.`gene_symbol` = `"+getTableUserAnnotationsGenes()+"_public`.`gene_symbol` "
				+ "AND `"+getTableUserAnnotationsGenes()+"_public`.`username` = 'PUBLIC' ";
	}

	public String getJoinUserAnnotationsSamplesPrivate() {
		return "LEFT JOIN `"+getTableUserAnnotationsSamples()+"` "
				+ "ON `"+getTableSampleAnnotations()+"`.`project_id` = `"+getTableUserAnnotationsSamples()+"`.`project_id` "
				+ "AND `"+getTableUserAnnotationsSamples()+"`.`username` = '"+Highlander.getLoggedUser().getUsername()+"' ";
	}

	public String getJoinUserAnnotationsSamplesPublic() {
		return "LEFT JOIN `"+getTableUserAnnotationsSamples()+"` AS `"+getTableUserAnnotationsSamples()+"_public` "
				+ "ON `"+getTableSampleAnnotations()+"`.`project_id` = `"+getTableUserAnnotationsSamples()+"_public`.`project_id` "
				+ "AND `"+getTableUserAnnotationsSamples()+"_public`.`username` = 'PUBLIC' ";
	}

}
