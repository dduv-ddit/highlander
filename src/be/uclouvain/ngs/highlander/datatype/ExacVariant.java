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

import java.util.Set;

import be.uclouvain.ngs.highlander.database.Field;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase;
import be.uclouvain.ngs.highlander.database.Field.Annotation;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.DBMS;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;

public class ExacVariant extends AnnotatedVariant {

	//Changes details
	//int filters` VARCHAR(255) //InbreedingCoeff_Filter: InbreedingCoeff <= -0.2 ; AC_Adj0_Filter: AC_Adj == 0 ; LowQual: Low quality ; NewCut_Filter: VQSLOD > -2.632 && InbreedingCoeff >-0.8 ; VQSRTranche[SNP/INDEL]AtoB: Truth sensitivity tranche level for SNP/INDEL model at VQS Lod: A <= x < B
	//int confidence = -1.0;    //The Phred scaled probability of Probability that reference/alternative polymorphism exists at this site given sequencing data. Because the Phred scale is -10 * log(1-p), a value of 10 indicates a 1 in 10 chance of error, while a 100 indicates a 1 in 10^10 chance. The GATK values can grow very large when lots of NGS data is used to call.
	int AC = -1;    //Allele count in genotypes, for each ALT allele, in the same order as listed
	int AC_AFR = -1;    //African/African American Allele Counts
	int AC_AMR = -1;    //American Allele Counts
	int AC_Adj = -1;    //Adjusted Allele Counts
	int AC_EAS = -1;    //East Asian Allele Counts
	int AC_FIN = -1;    //Finnish Allele Counts
	int AC_Hemi = -1;    //Adjusted Hemizygous Counts
	int AC_Het = -1;    //Adjusted Heterozygous Counts
	int AC_Hom = -1;    //Adjusted Homozygous Counts
	int AC_NFE = -1;    //Non-Finnish European Allele Counts
	int AC_OTH = -1;    //Other Allele Counts
	int AC_SAS = -1;    //South Asian Allele Counts
	double AF = -1.0;    //Allele Frequency, for each ALT allele, in the same order as listed
	double AF_Adj = -1.0;    //Adjusted Allele Frequency, for each ALT allele, in the same order as listed (AC_Adj / AN_Adj)
	int AN = -1;    //Total number of alleles in called genotypes
	int AN_AFR = -1;    //African/African American Chromosome Count
	int AN_AMR = -1;    //American Chromosome Count
	int AN_Adj = -1;    //Adjusted Chromosome Count
	int AN_EAS = -1;    //East Asian Chromosome Count
	int AN_FIN = -1;    //Finnish Chromosome Count
	int AN_NFE = -1;    //Non-Finnish European Chromosome Count
	int AN_OTH = -1;    //Other Chromosome Count
	int AN_SAS = -1;    //South Asian Chromosome Count
	//rank_sum_test_base_qual == int BaseQRankSum = -1.0;    //Z-score from Wilcoxon rank sum test of Alt Vs. Ref base qualities
	int CCC = -1;    //Number of called chromosomes
	double ClippingRankSum = -1.0;    //Z-score From Wilcoxon rank sum test of Alt vs. Ref number of hard clipped bases
	//read_depth == int DP = -1;    //Approximate read depth; some reads may have been filtered
	//downsampled == int DS;    //Were any of the samples downsampled?
	int END = -1;    //Stop position of the interval
	//fisher_strand_bias == int FS = -1.0;    //Phred-scaled p-value using Fisher s exact test to detect strand bias
	double GQ_MEAN = -1.0;    //Mean of all GQ values
	double GQ_STDDEV = -1.0;    //Standard deviation of all GQ values
	double HWP = -1.0;    //P value from test of Hardy Weinberg Equilibrium
	//haplotype_score == double HaplotypeScore = -1.0;    //Consistency of the site with at most two segregating haplotypes
	int Hemi_AFR = -1;    //African/African American Hemizygous Counts
	int Hemi_AMR = -1;    //American Hemizygous Counts
	int Hemi_EAS = -1;    //East Asian Hemizygous Counts
	int Hemi_FIN = -1;    //Finnish Hemizygous Counts
	int Hemi_NFE = -1;    //Non-Finnish European Hemizygous Counts
	int Hemi_OTH = -1;    //Other Hemizygous Counts
	int Hemi_SAS = -1;    //South Asian Hemizygous Counts
	int Het_AFR = -1;    //African/African American Heterozygous Counts
	int Het_AMR = -1;    //American Heterozygous Counts
	int Het_EAS = -1;    //East Asian Heterozygous Counts
	int Het_FIN = -1;    //Finnish Heterozygous Counts
	int Het_NFE = -1;    //Non-Finnish European Heterozygous Counts
	int Het_OTH = -1;    //Other Heterozygous Counts
	int Het_SAS = -1;    //South Asian Heterozygous Counts
	int Hom_AFR = -1;    //African/African American Homozygous Counts
	int Hom_AMR = -1;    //American Homozygous Counts
	int Hom_EAS = -1;    //East Asian Homozygous Counts
	int Hom_FIN = -1;    //Finnish Homozygous Counts
	int Hom_NFE = -1;    //Non-Finnish European Homozygous Counts
	int Hom_OTH = -1;    //Other Homozygous Counts
	int Hom_SAS = -1;    //South Asian Homozygous Counts
	//inbreedingCoeff == double InbreedingCoeff = -1.0;    //Inbreeding coefficient as estimated from the genotype likelihoods per-sample when compared against the Hardy-Weinberg expectation
	//mle_allele_count == int MLEAC = -1;    //Maximum likelihood expectation (MLE) for the allele counts (not necessarily the same as the AC), for each ALT allele, in the same order as listed
	//mle_allele_frequency == double MLEAF = -1.0;    //Maximum likelihood expectation (MLE) for the allele frequency (not necessarily the same as the AF), for each ALT allele, in the same order as listed
	//mapping_quality == double MQ = -1.0;    //RMS Mapping Quality
	//mapping_quality_zero_reads == int MQ0 = -1;    //Total Mapping Quality Zero Reads
	//rank_sum_test_read_mapping_qual == double MQRankSum = -1.0;    //Z-score From Wilcoxon rank sum test of Alt vs. Ref read mapping qualities
	int NCC = -1;    //Number of no-called samples
	boolean NEGATIVE_TRAIN_SITE;    //This variant was used to build the negative training set of bad variants
	boolean POSITIVE_TRAIN_SITE;    //This variant was used to build the positive training set of good variants
	//variant_confidence_by_depth == double QD = -1.0;    //Variant Confidence/Quality by Depth
	//rank_sum_test_read_pos_bias == double ReadPosRankSum = -1.0;    //Z-score from Wilcoxon rank sum test of Alt vs. Ref read position bias
	double VQSLOD = -1.0;    //Log odds ratio of being a true variant versus being false under the trained gaussian mixture model
	String culprit = null;    //The annotation which was the worst performing in the Gaussian mixture model, likely the reason why the variant was filtered out
	String DP_HIST = null;    //Histogram for DP; Mids: 2.5|7.5|12.5|17.5|22.5|27.5|32.5|37.5|42.5|47.5|52.5|57.5|62.5|67.5|72.5|77.5|82.5|87.5|92.5|97.5
	String GQ_HIST = null;    //Histogram for GQ; Mids: 2.5|7.5|12.5|17.5|22.5|27.5|32.5|37.5|42.5|47.5|52.5|57.5|62.5|67.5|72.5|77.5|82.5|87.5|92.5|97.5

	public ExacVariant(AnalysisFull analysis) {
		super(analysis);
	}

	/**
	 * 
	 * @param analysis
	 * @param line
	 * @param altIdx index of the allele. First alternative allele is 0, second alternative allele is 1, etc. Do not take into account reference allele (when ref is also in a list, we use altidx+1).
	 */
	@Override
	public void setVCFLine(String[] header, String[] line, int altIdx, String sample){
		//TODO BURDEN - utiliser le parser comme dans Annotated variant
		int nAlt = 1;
		for (int col=0 ; col < header.length ; col++){
			if (header[col].equalsIgnoreCase("#CHROM") || header[col].equalsIgnoreCase("CHROM")){
				entries.put(Field.chr, line[col].replace("chr", "")); 
			}else if (header[col].equalsIgnoreCase("POS")){
				entries.put(Field.pos, Integer.parseInt(line[col]));
				setRefAlt();	
			}else if (header[col].equalsIgnoreCase("ID")){
				String dbsnp_id = line[col];
				entries.put(Field.dbsnp_id, dbsnp_id); 
				if (dbsnp_id.length() == 0) entries.put(Field.dbsnp_id, null); 
				else if (dbsnp_id.equals(".")) entries.put(Field.dbsnp_id, null); 
				//else dbsnp_flagged = true;
			}else if (header[col].equalsIgnoreCase("REF")){
				entries.put(Field.reference, line[col]); 
				setRefAlt();	
			}else if (header[col].equalsIgnoreCase("ALT")){
				entries.put(Field.alternative, line[col].split(",")[altIdx]); 
				nAlt = line[col].split(",").length;
				entries.put(Field.allele_num, nAlt+1); 
				setRefAlt();	
			}else if (header[col].equalsIgnoreCase("QUAL")){
				try{
					entries.put(Field.confidence, Double.parseDouble(line[col])); 
				}catch(NumberFormatException nfe){
					entries.put(Field.confidence, null); 
				}
			}else if (header[col].equalsIgnoreCase("FILTER")){
				entries.put(Field.filters, line[col]); 
			}else if (header[col].equalsIgnoreCase("INFO")){
				String[] infos = line[col].split(";");
				for (int i=0 ; i < infos.length ; i++){
					if (infos[i].startsWith("AC=")){
						String[] ACs = infos[i].substring(3).split(",");
						if (altIdx < ACs.length){
							AC = Integer.parseInt(ACs[altIdx]);
						}else{
							AC = Integer.parseInt(ACs[0]);
						}
					}else if (infos[i].startsWith("AC_AFR=")){
						String[] ACs = infos[i].substring("AC_AFR=".length()).split(",");
						if (altIdx < ACs.length){
							AC_AFR = Integer.parseInt(ACs[altIdx]);
						}else{
							AC_AFR = Integer.parseInt(ACs[0]);
						}
					}else if (infos[i].startsWith("AC_AMR=")){
						String[] ACs = infos[i].substring("AC_AMR=".length()).split(",");
						if (altIdx < ACs.length){
							AC_AMR = Integer.parseInt(ACs[altIdx]);
						}else{
							AC_AMR = Integer.parseInt(ACs[0]);
						}
					}else if (infos[i].startsWith("AC_Adj=")){
						String[] ACs = infos[i].substring("AC_Adj=".length()).split(",");
						if (altIdx < ACs.length){
							AC_Adj = Integer.parseInt(ACs[altIdx]);
						}else{
							AC_Adj = Integer.parseInt(ACs[0]);
						}
						entries.put(Field.exac_ac, AC_Adj); 
					}else if (infos[i].startsWith("AC_EAS=")){
						String[] ACs = infos[i].substring("AC_EAS=".length()).split(",");
						if (altIdx < ACs.length){
							AC_EAS = Integer.parseInt(ACs[altIdx]);
						}else{
							AC_EAS = Integer.parseInt(ACs[0]);
						}
					}else if (infos[i].startsWith("AC_FIN=")){
						String[] ACs = infos[i].substring("AC_FIN=".length()).split(",");
						if (altIdx < ACs.length){
							AC_FIN = Integer.parseInt(ACs[altIdx]);
						}else{
							AC_FIN = Integer.parseInt(ACs[0]);
						}
					}else if (infos[i].startsWith("AC_Hemi=")){
						String[] ACs = infos[i].substring("AC_Hemi=".length()).split(",");
						if (altIdx < ACs.length){
							AC_Hemi = Integer.parseInt(ACs[altIdx]);
						}else{
							AC_Hemi = Integer.parseInt(ACs[0]);
						}
					}else if (infos[i].startsWith("AC_Het=")){
						String[] ACs = infos[i].substring("AC_Het=".length()).split(",");
						if (altIdx < ACs.length){
							AC_Het = Integer.parseInt(ACs[altIdx]);
						}else{
							AC_Het = Integer.parseInt(ACs[0]);
						}
					}else if (infos[i].startsWith("AC_Hom=")){
						String[] ACs = infos[i].substring("AC_Hom=".length()).split(",");
						if (altIdx < ACs.length){
							AC_Hom = Integer.parseInt(ACs[altIdx]);
						}else{
							AC_Hom = Integer.parseInt(ACs[0]);
						}
					}else if (infos[i].startsWith("AC_NFE=")){
						String[] ACs = infos[i].substring("AC_NFE=".length()).split(",");
						if (altIdx < ACs.length){
							AC_NFE = Integer.parseInt(ACs[altIdx]);
						}else{
							AC_NFE = Integer.parseInt(ACs[0]);
						}
					}else if (infos[i].startsWith("AC_OTH=")){
						String[] ACs = infos[i].substring("AC_OTH=".length()).split(",");
						if (altIdx < ACs.length){
							AC_OTH = Integer.parseInt(ACs[altIdx]);
						}else{
							AC_OTH = Integer.parseInt(ACs[0]);
						}
					}else if (infos[i].startsWith("AC_SAS=")){
						String[] ACs = infos[i].substring("AC_SAS=".length()).split(",");
						if (altIdx < ACs.length){
							AC_SAS = Integer.parseInt(ACs[altIdx]);
						}else{
							AC_SAS = Integer.parseInt(ACs[0]);
						}
					}else if (infos[i].startsWith("AF=")){
						String[] AFs = infos[i].substring("AF=".length()).split(",");
						if (altIdx < AFs.length){
							AF = Double.parseDouble(AFs[altIdx]);
						}else{
							AF = Double.parseDouble(AFs[0]);
						}
					}else if (infos[i].startsWith("AN=")){
						AN = Integer.parseInt(infos[i].substring("AN=".length()));
						//AN seems to be used only when there is multiple samples in the same VCF. So the better way to really have the number of allele for a sample is to count the number of ALT (and add 1 for the REF allele)
						entries.put(Field.allele_num, nAlt+1); 
					}else if (infos[i].startsWith("AN_AFR=")){
						AN_AFR = Integer.parseInt(infos[i].substring("AN_AFR=".length()));
					}else if (infos[i].startsWith("AN_AMR=")){
						AN_AMR = Integer.parseInt(infos[i].substring("AN_AMR=".length()));
					}else if (infos[i].startsWith("AN_Adj=")){
						AN_Adj = Integer.parseInt(infos[i].substring("AN_Adj=".length()));
						entries.put(Field.exac_an, AN_Adj); 
					}else if (infos[i].startsWith("AN_EAS=")){
						AN_EAS = Integer.parseInt(infos[i].substring("AN_EAS=".length()));
					}else if (infos[i].startsWith("AN_FIN=")){
						AN_FIN = Integer.parseInt(infos[i].substring("AN_FIN=".length()));
					}else if (infos[i].startsWith("AN_NFE=")){
						AN_NFE = Integer.parseInt(infos[i].substring("AN_NFE=".length()));
					}else if (infos[i].startsWith("AN_OTH=")){
						AN_OTH = Integer.parseInt(infos[i].substring("AN_OTH=".length()));
					}else if (infos[i].startsWith("AN_SAS=")){
						AN_SAS = Integer.parseInt(infos[i].substring("AN_SAS=".length()));
					}else if (infos[i].startsWith("Hemi_AFR=")){
						String[] Hemis = infos[i].substring("Hemi_AFR=".length()).split(",");
						if (altIdx < Hemis.length){
							Hemi_AFR = Integer.parseInt(Hemis[altIdx]);
						}else{
							Hemi_AFR = Integer.parseInt(Hemis[0]);
						}
					}else if (infos[i].startsWith("Hemi_AMR=")){
						String[] Hemis = infos[i].substring("Hemi_AMR=".length()).split(",");
						if (altIdx < Hemis.length){
							Hemi_AMR = Integer.parseInt(Hemis[altIdx]);
						}else{
							Hemi_AMR = Integer.parseInt(Hemis[0]);
						}
					}else if (infos[i].startsWith("Hemi_EAS=")){
						String[] Hemis = infos[i].substring("Hemi_EAS=".length()).split(",");
						if (altIdx < Hemis.length){
							Hemi_EAS = Integer.parseInt(Hemis[altIdx]);
						}else{
							Hemi_EAS = Integer.parseInt(Hemis[0]);
						}
					}else if (infos[i].startsWith("Hemi_FIN=")){
						String[] Hemis = infos[i].substring("Hemi_FIN=".length()).split(",");
						if (altIdx < Hemis.length){
							Hemi_FIN = Integer.parseInt(Hemis[altIdx]);
						}else{
							Hemi_FIN = Integer.parseInt(Hemis[0]);
						}
					}else if (infos[i].startsWith("Hemi_NFE=")){
						String[] Hemis = infos[i].substring("Hemi_NFE=".length()).split(",");
						if (altIdx < Hemis.length){
							Hemi_NFE = Integer.parseInt(Hemis[altIdx]);
						}else{
							Hemi_NFE = Integer.parseInt(Hemis[0]);
						}
					}else if (infos[i].startsWith("Hemi_OTH=")){
						String[] Hemis = infos[i].substring("Hemi_OTH=".length()).split(",");
						if (altIdx < Hemis.length){
							Hemi_OTH = Integer.parseInt(Hemis[altIdx]);
						}else{
							Hemi_OTH = Integer.parseInt(Hemis[0]);
						}
					}else if (infos[i].startsWith("Hemi_SAS=")){
						String[] Hemis = infos[i].substring("Hemi_SAS=".length()).split(",");
						if (altIdx < Hemis.length){
							Hemi_SAS = Integer.parseInt(Hemis[altIdx]);
						}else{
							Hemi_SAS = Integer.parseInt(Hemis[0]);
						}
					}else if (infos[i].startsWith("Het_AFR=")){
						String[] Hets = infos[i].substring("Het_AFR=".length()).split(",");
						if (altIdx < Hets.length){
							Het_AFR = Integer.parseInt(Hets[altIdx]);
						}else{
							Het_AFR = Integer.parseInt(Hets[0]);
						}
					}else if (infos[i].startsWith("Het_AMR=")){
						String[] Hets = infos[i].substring("Het_AMR=".length()).split(",");
						if (altIdx < Hets.length){
							Het_AMR = Integer.parseInt(Hets[altIdx]);
						}else{
							Het_AMR = Integer.parseInt(Hets[0]);
						}
					}else if (infos[i].startsWith("Het_EAS=")){
						String[] Hets = infos[i].substring("Het_EAS=".length()).split(",");
						if (altIdx < Hets.length){
							Het_EAS = Integer.parseInt(Hets[altIdx]);
						}else{
							Het_EAS = Integer.parseInt(Hets[0]);
						}
					}else if (infos[i].startsWith("Het_FIN=")){
						String[] Hets = infos[i].substring("Het_FIN=".length()).split(",");
						if (altIdx < Hets.length){
							Het_FIN = Integer.parseInt(Hets[altIdx]);
						}else{
							Het_FIN = Integer.parseInt(Hets[0]);
						}
					}else if (infos[i].startsWith("Het_NFE=")){
						String[] Hets = infos[i].substring("Het_NFE=".length()).split(",");
						if (altIdx < Hets.length){
							Het_NFE = Integer.parseInt(Hets[altIdx]);
						}else{
							Het_NFE = Integer.parseInt(Hets[0]);
						}
					}else if (infos[i].startsWith("Het_OTH=")){
						String[] Hets = infos[i].substring("Het_OTH=".length()).split(",");
						if (altIdx < Hets.length){
							Het_OTH = Integer.parseInt(Hets[altIdx]);
						}else{
							Het_OTH = Integer.parseInt(Hets[0]);
						}
					}else if (infos[i].startsWith("Het_SAS=")){
						String[] Hets = infos[i].substring("Het_SAS=".length()).split(",");
						if (altIdx < Hets.length){
							Het_SAS = Integer.parseInt(Hets[altIdx]);
						}else{
							Het_SAS = Integer.parseInt(Hets[0]);
						}
					}else if (infos[i].startsWith("Hom_AFR=")){
						String[] Homs = infos[i].substring("Hom_AFR=".length()).split(",");
						if (altIdx < Homs.length){
							Hom_AFR = Integer.parseInt(Homs[altIdx]);
						}else{
							Hom_AFR = Integer.parseInt(Homs[0]);
						}
					}else if (infos[i].startsWith("Hom_AMR=")){
						String[] Homs = infos[i].substring("Hom_AMR=".length()).split(",");
						if (altIdx < Homs.length){
							Hom_AMR = Integer.parseInt(Homs[altIdx]);
						}else{
							Hom_AMR = Integer.parseInt(Homs[0]);
						}
					}else if (infos[i].startsWith("Hom_EAS=")){
						String[] Homs = infos[i].substring("Hom_EAS=".length()).split(",");
						if (altIdx < Homs.length){
							Hom_EAS = Integer.parseInt(Homs[altIdx]);
						}else{
							Hom_EAS = Integer.parseInt(Homs[0]);
						}
					}else if (infos[i].startsWith("Hom_FIN=")){
						String[] Homs = infos[i].substring("Hom_FIN=".length()).split(",");
						if (altIdx < Homs.length){
							Hom_FIN = Integer.parseInt(Homs[altIdx]);
						}else{
							Hom_FIN = Integer.parseInt(Homs[0]);
						}
					}else if (infos[i].startsWith("Hom_NFE=")){
						String[] Homs = infos[i].substring("Hom_NFE=".length()).split(",");
						if (altIdx < Homs.length){
							Hom_NFE = Integer.parseInt(Homs[altIdx]);
						}else{
							Hom_NFE = Integer.parseInt(Homs[0]);
						}
					}else if (infos[i].startsWith("Hom_OTH=")){
						String[] Homs = infos[i].substring("Hom_OTH=".length()).split(",");
						if (altIdx < Homs.length){
							Hom_OTH = Integer.parseInt(Homs[altIdx]);
						}else{
							Hom_OTH = Integer.parseInt(Homs[0]);
						}
					}else if (infos[i].startsWith("Hom_SAS=")){
						String[] Homs = infos[i].substring("Hom_SAS=".length()).split(",");
						if (altIdx < Homs.length){
							Hom_SAS = Integer.parseInt(Homs[altIdx]);
						}else{
							Hom_SAS = Integer.parseInt(Homs[0]);
						}
					}else if (infos[i].startsWith("CCC=")){
						CCC = Integer.parseInt(infos[i].substring("CCC=".length()));
					}else if (infos[i].startsWith("ClippingRankSum=")){
						ClippingRankSum = Double.parseDouble(infos[i].substring("ClippingRankSum=".length()));
					}else if (infos[i].startsWith("END=")){
						END = Integer.parseInt(infos[i].substring("END=".length()));
					}else if (infos[i].startsWith("GQ_MEAN=")){
						GQ_MEAN = Double.parseDouble(infos[i].substring("GQ_MEAN=".length()));
					}else if (infos[i].startsWith("GQ_STDDEV=")){
						GQ_STDDEV = Double.parseDouble(infos[i].substring("GQ_STDDEV=".length()));
					}else if (infos[i].startsWith("HWP=")){
						HWP = Double.parseDouble(infos[i].substring("HWP=".length()));
					}else if (infos[i].startsWith("NCC=")){
						NCC = Integer.parseInt(infos[i].substring("NCC=".length()));
					}else if (infos[i].startsWith("NEGATIVE_TRAIN_SITE")){
						NEGATIVE_TRAIN_SITE = true;
					}else if (infos[i].startsWith("POSITIVE_TRAIN_SITE")){
						POSITIVE_TRAIN_SITE = true;
					}else if (infos[i].startsWith("VQSLOD=")){
						VQSLOD = Double.parseDouble(infos[i].substring("VQSLOD=".length()));
					}else if (infos[i].startsWith("culprit=")){
						culprit = infos[i].substring("culprit=".length());
					}else if (infos[i].startsWith("GQ_HIST=")){
						GQ_HIST = infos[i].substring("GQ_HIST=".length());
					}else if (infos[i].startsWith("DP_HIST=")){
						DP_HIST = infos[i].substring("DP_HIST=".length());
						//TODO BURDEN - ExAC à mettre complètement à jour
						/*
					}else if (infos[i].startsWith("BaseQRankSum=")){
						entries.put(Field.rank_sum_test_base_qual, Double.parseDouble(infos[i].substring("BaseQRankSum=".length())));
					}else if (infos[i].startsWith("DB")){
						entries.put(Field.dbSNP, true);
					}else if (infos[i].startsWith("DP=")){
						entries.put(Field.read_depth, Integer.parseInt(infos[i].substring("DP=".length())));
					}else if (infos[i].startsWith("DS")){
						entries.put(Field.downsampled, true);
					}else if (infos[i].startsWith("FS=")){
						entries.put(Field.fisher_strand_bias, Double.parseDouble(infos[i].substring("FS=".length())));
					}else if (infos[i].startsWith("HaplotypeScore=")){
						entries.put(Field.haplotype_score, Double.parseDouble(infos[i].substring("HaplotypeScore=".length())));
					}else if (infos[i].startsWith("InbreedingCoeff=")){
						entries.put(Field.inbreedingCoeff, Double.parseDouble(infos[i].substring("InbreedingCoeff=".length())));
					}else if (infos[i].startsWith("MQ=")){
						entries.put(Field.mapping_quality, Double.parseDouble(infos[i].substring("MQ=".length())));
					}else if (infos[i].startsWith("MQ0=")){
						entries.put(Field.mapping_quality_zero_reads, Integer.parseInt(infos[i].substring("MQ0=".length())));
					}else if (infos[i].startsWith("MQRankSum=")){
						entries.put(Field.rank_sum_test_read_mapping_qual, Double.parseDouble(infos[i].substring("MQRankSum=".length())));
					}else if (infos[i].startsWith("QD=")){
						entries.put(Field.variant_confidence_by_depth, Double.parseDouble(infos[i].substring("QD=".length())));
					}else if (infos[i].startsWith("ReadPosRankSum=")){
						entries.put(Field.rank_sum_test_read_pos_bias, Double.parseDouble(infos[i].substring("ReadPosRankSum=".length())));
					}else if (infos[i].startsWith("MLEAC=")){
						String[] MLEAC = infos[i].substring("MLEAC=".length()).split(",");
						if (altIdx < MLEAC.length){
							entries.put(Field.mle_allele_count, Integer.parseInt(MLEAC[altIdx]));
						}else{
							entries.put(Field.mle_allele_count, Integer.parseInt(MLEAC[0]));
						}
					}else if (infos[i].startsWith("MLEAF=")){
						String[] MLEAF = infos[i].substring("MLEAF=".length()).split(",");
						if (altIdx < MLEAF.length){
							entries.put(Field.mle_allele_frequency, Double.parseDouble(MLEAF[altIdx]));
						}else{
							entries.put(Field.mle_allele_frequency, Double.parseDouble(MLEAF[0]));
						}
						*/
					}else if (infos[i].startsWith("CSQ=")){
						//Not used : Consequence type as predicted by VEP. Format: Allele|Gene|Feature|Feature_type|Consequence|cDNA_position|CDS_position|Protein_position|Amino_acids|Codons|Existing_variation|ALLELE_NUM|DISTANCE|STRAND|SYMBOL|SYMBOL_SOURCE|HGNC_ID|BIOTYPE|CANONICAL|CCDS|ENSP|SWISSPROT|TREMBL|UNIPARC|SIFT|PolyPhen|EXON|INTRON|DOMAINS|HGVSc|HGVSp|GMAF|AFR_MAF|AMR_MAF|ASN_MAF|EUR_MAF|AA_MAF|EA_MAF|CLIN_SIG|SOMATIC|PUBMED|MOTIF_NAME|MOTIF_POS|HIGH_INF_POS|MOTIF_SCORE_CHANGE|LoF_info|LoF_flags|LoF_filter|LoF
					}else if (infos[i].startsWith("ANN=")){
						parseSnpEffANN(infos[i]);
					}else if (infos[i].startsWith("EFF=")){
						System.err.println("snpEff classic mode not supported, EFF field not parsed.'");
						/*
						for (String eff : infos[i].substring("EFF=".length()).split(",")){
							SNPEffect effect = new SNPEffect(eff,Input.VCF_EFF, chr, pos, reference, alternative);
							if (effect.getAlternative().equals(alternative)){
								addSNPEffect(effect);
							}
						}
						 */
					}else if (infos[i].startsWith("LOF=")){
						//From snpEff - not used
					}else if (infos[i].startsWith("NMD=")){
						//From snpEff - not used
					}else{
						//Other unused INFO field
						//System.err.println("Unrecognized INFO field : " + infos[i] + " at line '" + Arrays.toString(line) + "'");
					}
				}
			}else{
				//unused
			}
		}
		if (AC_Adj > -1 && AN_Adj > 0){
			AF_Adj = (double)AC_Adj / (double)AN_Adj;
			entries.put(Field.exac_af, AF_Adj); 
		}
	}

	//TODO BURDEN - on peut probablement s'en sortir avec des champs dans la table custom
	@Override
	public String getInsertionString(DBMS dbms, String table){
		String nullStr = HighlanderDatabase.getNullString(dbms);
		StringBuilder sb = new StringBuilder();
		for (Field f : Field.getAvailableFields(analysis, false)){
			//if (f.isAvailableInExac()){
			if ((f.getTable(analysis).equalsIgnoreCase(table) || f.isForeignKey(table))
					&& !(f.equals(Field.variant_sample_id) || f.equals(Field.variant_static_id) || f.equals(Field.gene_id) || f.equals(Field.variant_custom_id))
					){				
				if (f.getFieldClass() == String.class){
					if (entries.get(f) != null && entries.get(f).toString().length() > 0)	sb.append(HighlanderDatabase.format(dbms, Schema.HIGHLANDER, entries.get(f).toString())+"\t");
					else sb.append(nullStr+"\t");
				}else if (f.getFieldClass() == Boolean.class){
					if (dbms == DBMS.hsqldb) sb.append(((boolean)entries.get(f)?"true":"false")+"\t");
					else sb.append(((boolean)entries.get(f)?"1":"0")+"\t");
				}else{
					if (entries.get(f) != null)	sb.append(entries.get(f)+"\t");
					else sb.append(nullStr+"\t");
				}
			}
		}
		if (AC != -1)	sb.append(AC+"\t");
		else sb.append(nullStr+"\t");
		if (AC_Adj != -1)	sb.append(AC_Adj+"\t");
		else sb.append(nullStr+"\t");
		if (AC_AFR != -1)	sb.append(AC_AFR+"\t");
		else sb.append(nullStr+"\t");
		if (AC_AMR != -1)	sb.append(AC_AMR+"\t");
		else sb.append(nullStr+"\t");
		if (AC_EAS != -1)	sb.append(AC_EAS+"\t");
		else sb.append(nullStr+"\t");
		if (AC_FIN != -1)	sb.append(AC_FIN+"\t");
		else sb.append(nullStr+"\t");
		if (AC_NFE != -1)	sb.append(AC_NFE+"\t");
		else sb.append(nullStr+"\t");
		if (AC_SAS != -1)	sb.append(AC_SAS+"\t");
		else sb.append(nullStr+"\t");
		if (AC_OTH != -1)	sb.append(AC_OTH+"\t");
		else sb.append(nullStr+"\t");
		if (AC_Het != -1)	sb.append(AC_Het+"\t");
		else sb.append(nullStr+"\t");
		if (AC_Hom != -1)	sb.append(AC_Hom+"\t");
		else sb.append(nullStr+"\t");
		if (AC_Hemi != -1)	sb.append(AC_Hemi+"\t");
		else sb.append(nullStr+"\t");
		if (AF != -1)	sb.append(AF+"\t");
		else sb.append(nullStr+"\t");
		if (AF_Adj != -1)	sb.append(AF_Adj+"\t");
		else sb.append(nullStr+"\t");
		if (AN != -1)	sb.append(AN+"\t");
		else sb.append(nullStr+"\t");
		if (AN_Adj != -1)	sb.append(AN_Adj+"\t");
		else sb.append(nullStr+"\t");
		if (AN_AFR != -1)	sb.append(AN_AFR+"\t");
		else sb.append(nullStr+"\t");
		if (AN_AMR != -1)	sb.append(AN_AMR+"\t");
		else sb.append(nullStr+"\t");
		if (AN_EAS != -1)	sb.append(AN_EAS+"\t");
		else sb.append(nullStr+"\t");
		if (AN_FIN != -1)	sb.append(AN_FIN+"\t");
		else sb.append(nullStr+"\t");
		if (AN_NFE != -1)	sb.append(AN_NFE+"\t");
		else sb.append(nullStr+"\t");
		if (AN_SAS != -1)	sb.append(AN_SAS+"\t");
		else sb.append(nullStr+"\t");
		if (AN_OTH != -1)	sb.append(AN_OTH+"\t");
		else sb.append(nullStr+"\t");
		if (Het_AFR != -1)	sb.append(Het_AFR+"\t");
		else sb.append(nullStr+"\t");
		if (Het_AMR != -1)	sb.append(Het_AMR+"\t");
		else sb.append(nullStr+"\t");
		if (Het_EAS != -1)	sb.append(Het_EAS+"\t");
		else sb.append(nullStr+"\t");
		if (Het_FIN != -1)	sb.append(Het_FIN+"\t");
		else sb.append(nullStr+"\t");
		if (Het_NFE != -1)	sb.append(Het_NFE+"\t");
		else sb.append(nullStr+"\t");
		if (Het_SAS != -1)	sb.append(Het_SAS+"\t");
		else sb.append(nullStr+"\t");
		if (Het_OTH != -1)	sb.append(Het_OTH+"\t");
		else sb.append(nullStr+"\t");
		if (Hom_AFR != -1)	sb.append(Hom_AFR+"\t");
		else sb.append(nullStr+"\t");
		if (Hom_AMR != -1)	sb.append(Hom_AMR+"\t");
		else sb.append(nullStr+"\t");
		if (Hom_EAS != -1)	sb.append(Hom_EAS+"\t");
		else sb.append(nullStr+"\t");
		if (Hom_FIN != -1)	sb.append(Hom_FIN+"\t");
		else sb.append(nullStr+"\t");
		if (Hom_NFE != -1)	sb.append(Hom_NFE+"\t");
		else sb.append(nullStr+"\t");
		if (Hom_SAS != -1)	sb.append(Hom_SAS+"\t");
		else sb.append(nullStr+"\t");
		if (Hom_OTH != -1)	sb.append(Hom_OTH+"\t");
		else sb.append(nullStr+"\t");
		if (Hemi_AFR != -1)	sb.append(Hemi_AFR+"\t");
		else sb.append(nullStr+"\t");
		if (Hemi_AMR != -1)	sb.append(Hemi_AMR+"\t");
		else sb.append(nullStr+"\t");
		if (Hemi_EAS != -1)	sb.append(Hemi_EAS+"\t");
		else sb.append(nullStr+"\t");
		if (Hemi_FIN != -1)	sb.append(Hemi_FIN+"\t");
		else sb.append(nullStr+"\t");
		if (Hemi_NFE != -1)	sb.append(Hemi_NFE+"\t");
		else sb.append(nullStr+"\t");
		if (Hemi_SAS != -1)	sb.append(Hemi_SAS+"\t");
		else sb.append(nullStr+"\t");
		if (Hemi_OTH != -1)	sb.append(Hemi_OTH+"\t");
		else sb.append(nullStr+"\t");
		if (CCC != -1)	sb.append(CCC+"\t");
		else sb.append(nullStr+"\t");
		if (NCC != -1)	sb.append(NCC+"\t");
		else sb.append(nullStr+"\t");
		if (ClippingRankSum != -1)	sb.append(ClippingRankSum+"\t");
		else sb.append(nullStr+"\t");
		if (END != -1)	sb.append(END+"\t");
		else sb.append(nullStr+"\t");
		if (GQ_MEAN != -1)	sb.append(GQ_MEAN+"\t");
		else sb.append(nullStr+"\t");
		if (GQ_STDDEV != -1)	sb.append(GQ_STDDEV+"\t");
		else sb.append(nullStr+"\t");
		if (HWP != -1)	sb.append(HWP+"\t");
		else sb.append(nullStr+"\t");
		if (dbms == DBMS.hsqldb) sb.append((NEGATIVE_TRAIN_SITE?"true":"false")+"\t");
		else sb.append((NEGATIVE_TRAIN_SITE?"1":"0")+"\t");
		if (dbms == DBMS.hsqldb) sb.append((POSITIVE_TRAIN_SITE?"true":"false")+"\t");
		else sb.append((POSITIVE_TRAIN_SITE?"1":"0")+"\t");
		if (VQSLOD != -1)	sb.append(VQSLOD+"\t");
		else sb.append(nullStr+"\t");
		if (culprit != null && culprit.length() > 0)	sb.append(DB.format(Schema.HIGHLANDER, culprit)+"\t");
		else sb.append(nullStr+"\t");
		if (DP_HIST != null && DP_HIST.length() > 0)	sb.append(DB.format(Schema.HIGHLANDER, DP_HIST)+"\t");
		else sb.append(nullStr+"\t");
		if (GQ_HIST != null && GQ_HIST.length() > 0)	sb.append(DB.format(Schema.HIGHLANDER, GQ_HIST)+"\t");
		else sb.append(nullStr+"\t");
		sb.append("\n");
		return sb.toString();
	}

	public static String getInsertionColumnsString(Analysis analysis, String table){
		StringBuilder out  = new StringBuilder();
		for (Field f : Field.getAvailableFields(analysis, false)){
			//if (f.isAvailableInExac()){
			if ((f.getTable(analysis).equalsIgnoreCase(table) || f.isForeignKey(table))
					&& !(f.equals(Field.variant_sample_id) || f.equals(Field.variant_static_id) || f.equals(Field.gene_id) || f.equals(Field.variant_custom_id))
					){
				out.append(f.getName() + ", ");
			}
		}
		out.append("AC, AC_Adj, AC_AFR, AC_AMR, AC_EAS, AC_FIN, AC_NFE, AC_SAS, AC_OTH, AC_Het, AC_Hom, AC_Hemi, "
				+ "AF, AF_Adj, "
				+ "AN, AN_Adj, AN_AFR, AN_AMR, AN_EAS, AN_FIN, AN_NFE, AN_SAS, AN_OTH, "
				+ "Het_AFR, Het_AMR, Het_EAS, Het_FIN, Het_NFE, Het_SAS, Het_OTH, "
				+ "Hom_AFR, Hom_AMR, Hom_EAS, Hom_FIN, Hom_NFE, Hom_SAS, Hom_OTH, "
				+ "Hemi_AFR, Hemi_AMR, Hemi_EAS, Hemi_FIN, Hemi_NFE, Hemi_SAS, Hemi_OTH, "
				+ "CCC, NCC, ClippingRankSum, END, GQ_MEAN, GQ_STDDEV, HWP, NEGATIVE_TRAIN_SITE, POSITIVE_TRAIN_SITE, VQSLOD, culprit, DP_HIST, GQ_HIST");
		return out.toString();
	}

	@Override
	public void updateAnnotations(Set<Annotation> tags) throws Exception {	}

	@Override
	public void removeFromDatabase() throws Exception { }

}
