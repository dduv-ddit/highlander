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

import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.apache.commons.lang.WordUtils;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.administration.users.User;
import be.uclouvain.ngs.highlander.administration.users.User.Settings;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.SqlGenerator;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.DBMS;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;

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
public class AnalysisFull extends Analysis implements Comparable<Analysis> {

	public static enum VariantCaller {GATK,MUTECT,TORRENT,LIFESCOPE,SV,OTHER}

	protected String sequencingTarget;
	protected Reference reference;
	protected VariantCaller variantCaller;
	
	private int ordering;
	private ImageIcon icon;
	private ImageIcon smallIcon;
	private String bamURL;
	private String vcfURL;
	private String bamDIR;
	private String vcfDIR;
	private String vcfExtension;

	public AnalysisFull(String name, VariantCaller caller, Reference reference, String sequencingTarget, String bamURL, String vcfURL, String bamDIR, String vcfDIR, String vcfExtension, String vcfIndelExtension){
		super(name);
		this.variantCaller = caller;
		this.reference = reference;
		this.sequencingTarget = sequencingTarget;
		this.bamURL = bamURL;
		this.vcfURL = vcfURL;
		this.bamDIR = bamDIR;
		this.vcfDIR = vcfDIR;
		this.vcfExtension = vcfExtension;
	}

	public AnalysisFull(Analysis analysis) throws Exception {
		super(analysis.name);
		fetchInfo();
	}
	
	public AnalysisFull(Results res) throws Exception {
		super(res.getString("analysis"));
		setFields(res);
	}

	public void fetchInfo() throws Exception {
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT * FROM analyses WHERE analysis = '"+name+"'")) {
			if (res.next()){
				setFields(res);
			}
		}
	}

	private void setFields(Results res) throws Exception {
		name = res.getString("analysis");
		String refName = res.getString("reference");
		if (refName != null) {
			reference = new Reference(refName);
		}else {
			throw new Exception("Analysis " + name + " has no reference genome !");
		}
		sequencingTarget = res.getString("sequencing_target");
		variantCaller = VariantCaller.valueOf(res.getString("variant_caller"));	
		try{
			//Won't work when updating to version 17 because of vcf_extension renamed
			vcfExtension = res.getString("vcf_extension");
			Blob imagedata = res.getBlob("icon") ;
			//Won't work on a Unix without X11, put the try/catch for the dbBuilder to work on those
			Image img = Toolkit.getDefaultToolkit().createImage(imagedata.getBytes(1, (int)imagedata.length()));
			icon = new ImageIcon(img);	
			smallIcon = Resources.getScaledIcon(icon, 24);
		}catch(Exception ex){
			//ex.printStackTrace();
		}
		bamURL = res.getString("bam_url");
		vcfURL = res.getString("vcf_url");
		bamDIR = res.getString("bam_dir");
		vcfDIR = res.getString("vcf_dir");
		ordering = res.getInt("ordering");
	}

	public void insert(File iconFile) throws Exception {
		int count = 0;
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT COUNT(*) FROM `analyses` WHERE `analysis` = '"+name+"'")) {
			if (res.next()){
				count = res.getInt(1);
			}
		}
		if (count > 0){
			throw new Exception("Analisys '"+name+"' already exists in the database");
		}else {
			List<AnalysisFull> availableAnalyses = getAvailableAnalyses();
			//Creating all Highlander tables for this analysis
			try (Connection con = Highlander.getDB().getConnection(null, Schema.HIGHLANDER)){
				List<String> statements = new ArrayList<>();
				if (Highlander.getDB().getDataSource(Schema.HIGHLANDER).getDBMS() == DBMS.hsqldb){
					for (String st : SqlGenerator.convert(SqlGenerator.createStaticAnnotations(this), DBMS.hsqldb)) statements.addAll(Arrays.asList(st.split(";\n")));								
					for (String st : SqlGenerator.convert(SqlGenerator.createSampleAnnotations(this), DBMS.hsqldb)) statements.addAll(Arrays.asList(st.split(";\n")));
					for (String st : SqlGenerator.convert(SqlGenerator.createGeneAnnotations(this), DBMS.hsqldb)) statements.addAll(Arrays.asList(st.split(";\n")));
					for (String st : SqlGenerator.convert(SqlGenerator.createCustomAnnotations(this), DBMS.hsqldb)) statements.addAll(Arrays.asList(st.split(";\n")));
					for (String st : SqlGenerator.convert(SqlGenerator.createAlleleFrequencies(this), DBMS.hsqldb)) statements.addAll(Arrays.asList(st.split(";\n")));
					for (String st : SqlGenerator.convert(SqlGenerator.createAlleleFrequenciesPerPathology(this), DBMS.hsqldb)) statements.addAll(Arrays.asList(st.split(";\n")));
					for (String st : SqlGenerator.convert(SqlGenerator.createUserAnnotationsVariants(this), DBMS.hsqldb)) statements.addAll(Arrays.asList(st.split(";\n")));
					for (String st : SqlGenerator.convert(SqlGenerator.createUserAnnotationsGenes(this), DBMS.hsqldb)) statements.addAll(Arrays.asList(st.split(";\n")));
					for (String st : SqlGenerator.convert(SqlGenerator.createUserAnnotationsSamples(this), DBMS.hsqldb)) statements.addAll(Arrays.asList(st.split(";\n")));
					for (String st : SqlGenerator.convert(SqlGenerator.createUserAnnotationsEvaluations(this), DBMS.hsqldb)) statements.addAll(Arrays.asList(st.split(";\n")));
					for (String st : SqlGenerator.convert(SqlGenerator.createUserAnnotationsNumEvaluations(this), DBMS.hsqldb)) statements.addAll(Arrays.asList(st.split(";\n")));
					for (String st : SqlGenerator.convert(SqlGenerator.createPossibleValues(this), DBMS.hsqldb)) statements.addAll(Arrays.asList(st.split(";\n")));
					for (String st : SqlGenerator.convert(SqlGenerator.createCoverageRegions(this), DBMS.hsqldb)) statements.addAll(Arrays.asList(st.split(";\n")));
					for (String st : SqlGenerator.convert(SqlGenerator.createCoverage(this), DBMS.hsqldb)) statements.addAll(Arrays.asList(st.split(";\n")));
					for (int i=0 ; i < statements.size() ; i++){
						try (PreparedStatement pstmt = con.prepareStatement(statements.get(i))){
							pstmt.executeUpdate();
						}
					}
				}else{
					statements.addAll(Arrays.asList(SqlGenerator.createStaticAnnotations(this).toString().split(";\n")));
					statements.addAll(Arrays.asList(SqlGenerator.createSampleAnnotations(this).toString().split(";\n")));
					statements.addAll(Arrays.asList(SqlGenerator.createGeneAnnotations(this).toString().split(";\n")));
					statements.addAll(Arrays.asList(SqlGenerator.createCustomAnnotations(this).toString().split(";\n")));
					statements.addAll(Arrays.asList(SqlGenerator.createAlleleFrequencies(this).toString().split(";\n")));
					statements.addAll(Arrays.asList(SqlGenerator.createAlleleFrequenciesPerPathology(this).toString().split(";\n")));
					statements.addAll(Arrays.asList(SqlGenerator.createUserAnnotationsVariants(this).toString().split(";\n")));
					statements.addAll(Arrays.asList(SqlGenerator.createUserAnnotationsGenes(this).toString().split(";\n")));
					statements.addAll(Arrays.asList(SqlGenerator.createUserAnnotationsSamples(this).toString().split(";\n")));
					statements.addAll(Arrays.asList(SqlGenerator.createUserAnnotationsEvaluations(this).toString().split(";\n")));
					statements.addAll(Arrays.asList(SqlGenerator.createUserAnnotationsNumEvaluations(this).toString().split(";\n")));
					statements.addAll(Arrays.asList(SqlGenerator.createPossibleValues(this).toString().split(";\n")));
					statements.addAll(Arrays.asList(SqlGenerator.createCoverageRegions(this).toString().split(";\n")));
					statements.addAll(Arrays.asList(SqlGenerator.createCoverage(this).toString().split(";\n")));
					try (Statement st = con.createStatement()){
						for (int i=0 ; i < statements.size() ; i++){
							st.addBatch(statements.get(i));
						}
						st.executeBatch();
					}
				}
			}
			//Insert this analysis in analyses table
			ordering = 0;
			for (AnalysisFull a : availableAnalyses) {
				if (a.getOrdering() > ordering) ordering = a.getOrdering();
			}
			ordering++;
			try (Connection con = Highlander.getDB().getConnection(null, Schema.HIGHLANDER)){
				try (PreparedStatement pstmt = con.prepareStatement(Highlander.getDB().formatQuery(Schema.HIGHLANDER, 
						"INSERT INTO analyses "
								+ "(`analysis`,`reference`,`icon`,`ordering`,`sequencing_target`,`variant_caller`,"
								+ "`bam_url`,`vcf_url`,`bam_dir`,`vcf_dir`,`vcf_extension`) "
								+ "VALUES ("
								+ "'"+name+"',"
								+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getReference().toString())+"',"
								+ "?,"
								+ ""+ordering+","
								+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getSequencingTarget())+"',"
								+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getVariantCaller().toString())+"',"
								+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getBamRepository())+"',"
								+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getVcfRepository())+"',"
								+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getBamDirectory())+"',"
								+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getVcfDirectory())+"',"
								+ "'"+Highlander.getDB().format(Schema.HIGHLANDER, getVcfExtension())+"'"
								+ ")"))){
					FileInputStream input = new FileInputStream(iconFile);
					pstmt.setBinaryStream(1, input);
					pstmt.executeUpdate();
				}
			}
			Image img = ImageIO.read(iconFile);
			icon = new ImageIcon(img);	
			smallIcon = Resources.getScaledIcon(icon, 24);
			//Link all non custom annotation fields to this new analysis 
			Highlander.getDB().insert(Schema.HIGHLANDER, "INSERT INTO `fields_analyses` (`field`,`analysis`) SELECT `field`, '"+name+"' FROM `fields` WHERE `table` != '_custom_annotations'");
			//Set default columns
			setDefaultColumns();
		}
	}
	
	public void update() throws Exception {
		Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE analyses SET " +
				"reference = '"+Highlander.getDB().format(Schema.HIGHLANDER, getReference().toString())+"', " +
				"ordering = "+getOrdering()+", " +
				"sequencing_target = '"+Highlander.getDB().format(Schema.HIGHLANDER, getSequencingTarget())+"', " +
				"bam_url = '"+Highlander.getDB().format(Schema.HIGHLANDER, getBamRepository())+"', " +
				"vcf_url = '"+Highlander.getDB().format(Schema.HIGHLANDER, getVcfRepository())+"', " +
				"bam_dir = '"+Highlander.getDB().format(Schema.HIGHLANDER, getBamDirectory())+"', " +
				"vcf_dir = '"+Highlander.getDB().format(Schema.HIGHLANDER, getVcfDirectory())+"', " +
				"vcf_extension = '"+Highlander.getDB().format(Schema.HIGHLANDER, getVcfExtension())+"', " +
				"variant_caller = '"+Highlander.getDB().format(Schema.HIGHLANDER, getVariantCaller().toString())+"' " +
				"WHERE analysis = '"+name+"'");		
	}
	
	public void delete() throws Exception {
		//Delete Highlander tables supporting this analysis
		Highlander.getDB().update(Schema.HIGHLANDER, "DROP table " + getTableSampleAnnotations());
		Highlander.getDB().update(Schema.HIGHLANDER, "DROP table " + getTableStaticAnnotations());
		Highlander.getDB().update(Schema.HIGHLANDER, "DROP table " + getTableCustomAnnotations());
		Highlander.getDB().update(Schema.HIGHLANDER, "DROP table " + getTableGeneAnnotations());
		Highlander.getDB().update(Schema.HIGHLANDER, "DROP table " + getTablePossibleValues());
		Highlander.getDB().update(Schema.HIGHLANDER, "DROP table " + getTableAlleleFrequencies());
		Highlander.getDB().update(Schema.HIGHLANDER, "DROP table " + getTableAlleleFrequenciesPerPathology());
		Highlander.getDB().update(Schema.HIGHLANDER, "DROP table " + getTableCoverage());
		Highlander.getDB().update(Schema.HIGHLANDER, "DROP table " + getTableCoverageRegions());
		Highlander.getDB().update(Schema.HIGHLANDER, "DROP table " + getTableUserAnnotationsVariants());
		Highlander.getDB().update(Schema.HIGHLANDER, "DROP table " + getTableUserAnnotationsGenes());
		Highlander.getDB().update(Schema.HIGHLANDER, "DROP table " + getTableUserAnnotationsSamples());
		Highlander.getDB().update(Schema.HIGHLANDER, "DROP table " + getTableUserAnnotationsEvaluations());
		Highlander.getDB().update(Schema.HIGHLANDER, "DROP table " + getTableUserAnnotationsNumEvaluations());
		//Delete project imported in this analysis
		Highlander.getDB().update(Schema.HIGHLANDER, "DELETE FROM projects_analyses WHERE `analysis` = '"+name+"'");
		//Delete user data (filters, etc) linked to this analysis
		Highlander.getDB().update(Schema.HIGHLANDER, "DELETE FROM users_data WHERE `analysis` = '"+name+"' OR `value` = 'found_in_" + name + "'");
		Highlander.getDB().update(Schema.HIGHLANDER, "UPDATE users_data SET `value` = REPLACE(`value`,'°found_in_" + name + "°','°') WHERE `analysis` = '"+name+"'");
		//Delete entry from analyses table
		Highlander.getDB().update(Schema.HIGHLANDER, "DELETE FROM analyses WHERE analysis = '"+name+"'");
	}
	
	public void updateIcon(File file) throws Exception {
		try (Connection con = Highlander.getDB().getConnection(null, Schema.HIGHLANDER)){
			try (PreparedStatement pstmt = con.prepareStatement(Highlander.getDB().formatQuery(Schema.HIGHLANDER,"UPDATE analyses SET icon = ? WHERE analysis = '"+name+"'"))){
				FileInputStream input = new FileInputStream(file);
				pstmt.setBinaryStream(1, input);
				pstmt.executeUpdate();
			}
		}
		Image img = ImageIO.read(file);
		icon = new ImageIcon(img);	
		smallIcon = Resources.getScaledIcon(icon, 24);
	}
	
	public void setDefaultColumns() throws Exception {
		List<String> fields = new ArrayList<>();
		fields.add("chr");
		fields.add("pos");
		fields.add("length");
		fields.add("reference");
		fields.add("alternative");
		fields.add("gene_symbol");
		fields.add("variant_type");
		fields.add("sample");
		fields.add("pathology");
		fields.add("population");
		fields.add("sample_type");
		fields.add("hgvs_dna");
		fields.add("hgvs_protein");
		fields.add("cds_strand");
		fields.add("exon_intron_rank");
		fields.add("num_genes");
		fields.add("allele_num");
		fields.add("filters");
		fields.add("confidence");
		fields.add("zygosity");
		fields.add("read_depth");
		fields.add("allelic_depth_ref");
		fields.add("allelic_depth_alt");
		fields.add("allelic_depth_proportion_ref");
		fields.add("allelic_depth_proportion_alt");
		fields.add("snpeff_effect");
		fields.add("snpeff_impact");
		fields.add("consensus_prediction");
		fields.add("mutation_taster_pred");
		fields.add("fathmm_pred");
		fields.add("fathmm_xf_pred");
		fields.add("fathmm_mkl_pred");
		fields.add("polyphen_hdiv_pred");
		fields.add("polyphen_hvar_pred");
		fields.add("provean_pred");
		fields.add("sift_pred");
		fields.add("sift_4g_pred");
		fields.add("mutation_assessor_pred");
		fields.add("mcap_pred");
		fields.add("lrt_pred");
		fields.add("lists2_pred");
		fields.add("deogen_pred");
		fields.add("clinpred_pred");
		fields.add("bayesdel_noaf_pred");
		fields.add("bayesdel_addaf_pred");
		fields.add("primate_ai_pred");
		fields.add("aloft_pred");
		fields.add("metasvm_pred");
		fields.add("metalr_pred");
		fields.add("lrt_omega");
		fields.add("cadd_raw_rankscore");
		fields.add("vest_rankscore");
		fields.add("dann_rankscore");
		fields.add("eigen_rankscore");
		fields.add("eigen_pc_raw_rankscore");
		fields.add("revel_rankscore");
		fields.add("mpc_rankscore");
		fields.add("mvp_rankscore");
		fields.add("genocanyon_rankscore");
		fields.add("linsight_rankscore");
		fields.add("mutpred_rankscore");
		fields.add("splicing_ada_pred");
		fields.add("splicing_rf_pred");
		fields.add("integrated_fitcons_pred");
		fields.add("gm12878_fitcons_pred");
		fields.add("h1_hesc_fitcons_pred");
		fields.add("huvec_fitcons_pred");
		fields.add("local_af");
		fields.add("local_ac");
		fields.add("local_het");
		fields.add("local_hom");
		fields.add("local_pathologies");
		fields.add("gnomad_wgs_ac");
		fields.add("gnomad_wgs_af");
		fields.add("gnomad_wgs_nhomalt");
		fields.add("gnomad_wes_ac");
		fields.add("gnomad_wes_af");
		fields.add("gnomad_wes_nhomalt");
		fields.add("exac_ac");
		fields.add("exac_af");
		fields.add("uk10k_ac");
		fields.add("uk10k_af");
		fields.add("gonl_af");
		fields.add("gonl_ac");
		fields.add("clinvar_clnsig");
		fields.add("cosmic_id");
		fields.add("Interpro_domain");
		fields.add("transcript_ensembl");
		fields.add("transcript_uniprot_id");
		fields.add("transcript_refseq_mrna");
		fields.add("biotype");
		fields.add("exac_pli");
		fields.add("exac_prec");
		fields.add("exac_pnull");
		fields.add("gnomad_pli");
		fields.add("gnomad_prec");
		fields.add("gnomad_pnull");
		fields.add("exac_del_score");
		fields.add("exac_dup_score");
		fields.add("exac_cnv_score");
		fields.add("exac_cnv_flag");
		fields.add("rvis_percentile_evs");
		fields.add("lof_fdr_exac");
		fields.add("rvis_percentile_exac");
		fields.add("gdi_phred");
		fields.add("gene_damage_prediction_all");
		fields.add("loftool_score");
		fields.add("essential_gene");
		fields.add("gene_indispensability_pred");
		fields.add("haploinsufficiency");
		fields.add("haploinsufficiency_ghis");
		fields.add("recessive_probability");
		fields.add("variant_of_interest");
		fields.add("gene_of_interest");
		fields.add("sample_of_interest");
		fields.add("evaluation");
		fields.add("num_evaluated_as_type_1");
		fields.add("num_evaluated_as_type_2");
		fields.add("num_evaluated_as_type_3");
		fields.add("num_evaluated_as_type_4");
		fields.add("num_evaluated_as_type_5");
		fields.add("check_insilico");
		fields.add("reporting");
		fields.add("check_validated_variant");
		fields.add("check_somatic_variant");
		fields.add("check_segregation");
		User.saveDefaultSettings(name, Settings.DEFAULT_COLUMNS, fields);
	}
	
	public void setSequencingTarget(String sequencingTarget) {
		this.sequencingTarget = sequencingTarget;
	}

	public void setReference(Reference reference) {
		this.reference = reference;
	}

	public void setOrdering(int ordering) {
		this.ordering = ordering;
	}

	public void setBamURL(String bamURL) {
		this.bamURL = bamURL;
	}

	public void setVcfURL(String vcfURL) {
		this.vcfURL = vcfURL;
	}

	public void setBamDIR(String bamDIR) {
		this.bamDIR = bamDIR;
	}

	public void setVcfDIR(String vcfDIR) {
		this.vcfDIR = vcfDIR;
	}

	public void setVcfExtension(String vcfExtension) {
		this.vcfExtension = vcfExtension;
	}

	public int getOrdering() {
		return ordering;
	}
	
	public String getSequencingTarget(){
		return sequencingTarget;
	}

	public Reference getReference(){
		return reference;
	}

	public VariantCaller getVariantCaller(){
		return variantCaller;
	}

	public void setVariantCaller(VariantCaller variantCaller) {
		this.variantCaller = variantCaller;
	}

	public ImageIcon getIcon(){
		return icon;
	}

	public ImageIcon getSmallIcon(){
		return smallIcon;
	}

	public String getBamRepository(){
		return bamURL;
	}

	public String getVcfRepository(){
		return vcfURL;
	}
	
	public String getBamDirectory(){
		return bamDIR;
	}

	public String getBamFilename(String sample){
		return sample+".bam";
	}

	public String getBamURL(String sample){
		String url = bamURL;
		if (url.contains("@")){
			url = url.replace("@", sample);
		}
		if (url.contains("$")){
			String pathology = "unknown";
			try{
				try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
						"SELECT pathology "
						+ "FROM projects "
						+ "JOIN pathologies USING (pathology_id) "
						+ "JOIN projects_analyses USING (project_id) "
						+ "WHERE analysis = '"+name+"' AND sample = '"+sample+"'")) {
					if (res.next()) pathology = res.getString(1);
				}
			}catch(Exception ex){
				ex.printStackTrace();
			}
			url = url.replace("$", pathology);				
		}
		if (url.contains("#")){
			String population = "unknown";
			try{
				try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
						"SELECT population "
						+ "FROM projects "
						+ "JOIN populations USING (population_id) "
						+ "JOIN projects_analyses USING (project_id) "
						+ "WHERE analysis = '"+name+"' AND sample = '"+sample+"'")) {
					if (res.next()) population = res.getString(1);
				}
			}catch(Exception ex){
				ex.printStackTrace();
			}
			url = url.replace("#", population);				
		}
		return url+"/"+getBamFilename(sample);		
	}

	public String getVcfExtension(){
		return vcfExtension;
	}

	public String getVcfDirectory(){
		return vcfDIR;
	}

	public String getVcfFilename(String sample){
		return sample+vcfExtension;
	}

	public String getVcfURL(String sample){
		String url = vcfURL;
		if (url.contains("@")){
			url = url.replace("@", sample);
		}
		if (url.contains("$")){
			String pathology = "unknown";
			try{
				try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
						"SELECT pathology "
						+ "FROM projects "
						+ "JOIN pathologies USING (pathology_id) "
						+ "JOIN projects_analyses USING (project_id) "
						+ "WHERE analysis = '"+name+"' AND sample = '"+sample+"'")) {
					if (res.next()) pathology = res.getString(1);
				}
			}catch(Exception ex){
				ex.printStackTrace();
			}
			url = url.replace("$", pathology);				
		}
		if (url.contains("#")){
			String population = "unknown";
			try{
				try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, 
						"SELECT population "
						+ "FROM projects "
						+ "JOIN populations USING (population_id) "
						+ "JOIN projects_analyses USING (project_id) "
						+ "WHERE analysis = '"+name+"' AND sample = '"+sample+"'")) {
					if (res.next()) population = res.getString(1);
				}
			}catch(Exception ex){
				ex.printStackTrace();
			}
			url = url.replace("#", population);				
		}
		return url+"/"+sample+vcfExtension;
	}

	public String getHtmlTooltip() {
		return "<html><b>"+name+"</b><br>"+
				"<table>"+
				"<tr>" + "<td>" + "Target: " + "</td><td>" + getSequencingTarget() + "</td></tr>" +
				"<tr>" + "<td>" + "Reference: " + "</td><td>" + getReference().getName() + "</td></tr>" +
				"<tr>" + "<td>" + "" + "</td><td><i>" + getReference().getDescription() + "</i></td></tr>" +
				"<tr>" + "<td>" + "Species: " + "</td><td>" + WordUtils.capitalize(getReference().getSpecies().replace('_', ' ')) + "</td></tr>" +
				"<tr>" + "<td>" + "Genome version: " + "</td><td>" + getReference().getGenomeVersion() + "</td></tr>" +
				"<tr>" + "<td>" + "Ensembl version: " + "</td><td>" + getReference().getEnsemblVersion() + "</td></tr>" +
				"</table>"+
				"</html>";
	}
	
	public static List<AnalysisFull> getAvailableAnalyses() throws Exception {
		List<AnalysisFull> analyses = new ArrayList<AnalysisFull>();
		try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT * FROM analyses ORDER BY ordering")) {
			while (res.next()){
				analyses.add(new AnalysisFull(res));
			}
		}
		return analyses;
	}

}
