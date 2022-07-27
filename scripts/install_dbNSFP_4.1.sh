#!/bin/bash

set -e

################################################
### Download and install dbNSFP 4.1 database ###
################################################
#
# Parameters : 
# $1 = highlander database username
# $2 = highlander database password

### Parameters

DBUSER=$1
DBPASS=$2

### Variables

HIGHLANDERPATH=/data/highlander

VERSION=4.1
SQL=$(echo "$VERSION" | tr '.' '_')

DBNSFP=dbNSFPv${VERSION}
ZIP=ftp://dbnsfp:dbnsfp@dbnsfp.softgenetics.com/dbNSFP4.1a.zip
DBSCSNV=ftp://dbnsfp:dbnsfp@dbnsfp.softgenetics.com/dbscSNV1.1.zip
DBMTS=ftp://dbnsfp:dbnsfp@dbnsfp.softgenetics.com/dbMTS1.0

### Function to create SQL table

function createSQL(){
TABLENAME=$1;
echo "DROP TABLE IF EXISTS ${TABLENAME};" > $TABLENAME.sql
echo "CREATE TABLE ${TABLENAME} (" >> $TABLENAME.sql
echo '`chr` VARCHAR(50) DEFAULT NULL,' >> $TABLENAME.sql
echo '`pos` INT NOT NULL,' >> $TABLENAME.sql
echo '`ref` VARCHAR(1) DEFAULT NULL,' >> $TABLENAME.sql
echo '`alt` VARCHAR(1) DEFAULT NULL,' >> $TABLENAME.sql
echo '`aaref` VARCHAR(1) DEFAULT NULL,' >> $TABLENAME.sql
echo '`aaalt` VARCHAR(1) DEFAULT NULL,' >> $TABLENAME.sql
echo '`rs_dbSNP151` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`hg19_chr` VARCHAR(50) DEFAULT NULL,' >> $TABLENAME.sql
echo '`hg19_pos` VARCHAR(20) DEFAULT NULL,' >> $TABLENAME.sql
echo '`hg18_chr` VARCHAR(50) DEFAULT NULL,' >> $TABLENAME.sql
echo '`hg18_pos` VARCHAR(20) DEFAULT NULL,' >> $TABLENAME.sql
echo '`aapos` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`genename` VARCHAR(1000) DEFAULT NULL,' >> $TABLENAME.sql
echo '`Ensembl_geneid` VARCHAR(1000) DEFAULT NULL,' >> $TABLENAME.sql
echo '`Ensembl_transcriptid` VARCHAR(1000) DEFAULT NULL,' >> $TABLENAME.sql
echo '`Ensembl_proteinid` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Uniprot_acc` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Uniprot_entry` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`HGVSc_ANNOVAR` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`HGVSp_ANNOVAR` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`HGVSc_snpEff` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`HGVSp_snpEff` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`HGVSc_VEP` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`HGVSp_VEP` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`APPRIS` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GENCODE_basic` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TSL` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`VEP_canonical` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`cds_strand` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`refcodon` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`codonpos` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`codon_degeneracy` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Ancestral_allele` VARCHAR(1) DEFAULT NULL,' >> $TABLENAME.sql
echo '`AltaiNeandertal` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Denisova` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`VindijiaNeandertal` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`SIFT_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`SIFT_converted_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`SIFT_pred` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`SIFT4G_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`SIFT4G_converted_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`SIFT4G_pred` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Polyphen2_HDIV_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Polyphen2_HDIV_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Polyphen2_HDIV_pred` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Polyphen2_HVAR_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Polyphen2_HVAR_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Polyphen2_HVAR_pred` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`LRT_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`LRT_converted_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`LRT_pred` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`LRT_Omega` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`MutationTaster_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`MutationTaster_converted_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`MutationTaster_pred` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`MutationTaster_model` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`MutationTaster_AAE` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`MutationAssessor_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`MutationAssessor_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`MutationAssessor_pred` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`FATHMM_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`FATHMM_converted_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`FATHMM_pred` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`PROVEAN_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`PROVEAN_converted_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`PROVEAN_pred` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`VEST4_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`VEST4_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`MetaSVM_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`MetaSVM_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`MetaSVM_pred` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`MetaLR_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`MetaLR_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`MetaLR_pred` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Reliability_index` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M-CAP_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M-CAP_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M-CAP_pred` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`REVEL_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`REVEL_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`MutPred_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`MutPred_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`MutPred_protID` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`MutPred_AAchange` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`MutPred_Top5features` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`MVP_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`MVP_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`MPC_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`MPC_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`PrimateAI_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`PrimateAI_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`PrimateAI_pred` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`DEOGEN2_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`DEOGEN2_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`DEOGEN2_pred` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`BayesDel_addAF_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`BayesDel_addAF_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`BayesDel_addAF_pred` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`BayesDel_noAF_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`BayesDel_noAF_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`BayesDel_noAF_pred` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`ClinPred_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`ClinPred_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`ClinPred_pred` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`LIST-S2_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`LIST-S2_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`LIST-S2_pred` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Aloft_Fraction_transcripts_affected` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Aloft_prob_Tolerant` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Aloft_prob_Recessive` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Aloft_prob_Dominant` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Aloft_pred` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Aloft_Confidence` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`CADD_raw` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`CADD_raw_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`CADD_phred` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`CADD_raw_hg19` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`CADD_raw_rankscore_hg19` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`CADD_phred_hg19` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`DANN_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`DANN_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`fathmm-MKL_coding_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`fathmm-MKL_coding_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`fathmm-MKL_coding_pred` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`fathmm-MKL_coding_group` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`fathmm-XF_coding_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`fathmm-XF_coding_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`fathmm-XF_coding_pred` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Eigen-raw_coding` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Eigen-raw_coding_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Eigen-phred_coding` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Eigen-PC-raw_coding` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Eigen-PC-raw_coding_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Eigen-PC-phred_coding` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GenoCanyon_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GenoCanyon_score_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`integrated_fitCons_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`integrated_fitCons_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`integrated_confidence_value` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GM12878_fitCons_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GM12878_fitCons_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GM12878_confidence_value` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`H1-hESC_fitCons_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`H1-hESC_fitCons_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`H1-hESC_confidence_value` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`HUVEC_fitCons_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`HUVEC_fitCons_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`HUVEC_confidence_value` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`LINSIGHT` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`LINSIGHT_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GERP++_NR` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`GERP++_RS` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`GERP++_RS_rankscore` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`phyloP100way_vertebrate` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`phyloP100way_vertebrate_rankscore` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`phyloP30way_mammalian` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`phyloP30way_mammalian_rankscore` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`phyloP17way_primate` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`phyloP17way_primate_rankscore` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`phastCons100way_vertebrate` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`phastCons100way_vertebrate_rankscore` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`phastCons30way_mammalian` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`phastCons30way_mammalian_rankscore` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`phastCons17way_primate` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`phastCons17way_primate_rankscore` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`SiPhy_29way_pi` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`SiPhy_29way_logOdds` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`SiPhy_29way_logOdds_rankscore` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`bStatistic` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`bStatistic_converted_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`1000Gp3_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`1000Gp3_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`1000Gp3_AFR_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`1000Gp3_AFR_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`1000Gp3_EUR_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`1000Gp3_EUR_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`1000Gp3_AMR_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`1000Gp3_AMR_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`1000Gp3_EAS_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`1000Gp3_EAS_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`1000Gp3_SAS_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`1000Gp3_SAS_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`TWINSUK_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`TWINSUK_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ALSPAC_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ALSPAC_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`UK10K_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`UK10K_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ESP6500_AA_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ESP6500_AA_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ESP6500_EA_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ESP6500_EA_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_Adj_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_Adj_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_AFR_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_AFR_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_AMR_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_AMR_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_EAS_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_EAS_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_FIN_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_FIN_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_NFE_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_NFE_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_SAS_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_SAS_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonTCGA_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonTCGA_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonTCGA_Adj_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonTCGA_Adj_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonTCGA_AFR_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonTCGA_AFR_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonTCGA_AMR_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonTCGA_AMR_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonTCGA_EAS_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonTCGA_EAS_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonTCGA_FIN_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonTCGA_FIN_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonTCGA_NFE_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonTCGA_NFE_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonTCGA_SAS_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonTCGA_SAS_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonpsych_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonpsych_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonpsych_Adj_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonpsych_Adj_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonpsych_AFR_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonpsych_AFR_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonpsych_AMR_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonpsych_AMR_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonpsych_EAS_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonpsych_EAS_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonpsych_FIN_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonpsych_FIN_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonpsych_NFE_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonpsych_NFE_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonpsych_SAS_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonpsych_SAS_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_flag` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_AN` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_nhomalt` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_AFR_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_AFR_AN` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_AFR_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_AFR_nhomalt` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_AMR_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_AMR_AN` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_AMR_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_AMR_nhomalt` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_ASJ_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_ASJ_AN` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_ASJ_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_ASJ_nhomalt` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_EAS_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_EAS_AN` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_EAS_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_EAS_nhomalt` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_FIN_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_FIN_AN` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_FIN_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_FIN_nhomalt` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_NFE_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_NFE_AN` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_NFE_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_NFE_nhomalt` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_SAS_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_SAS_AN` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_SAS_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_SAS_nhomalt` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_POPMAX_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_POPMAX_AN` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_POPMAX_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_POPMAX_nhomalt` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_AN` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_nhomalt` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_AFR_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_AFR_AN` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_AFR_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_AFR_nhomalt` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_AMR_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_AMR_AN` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_AMR_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_AMR_nhomalt` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_ASJ_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_ASJ_AN` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_ASJ_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_ASJ_nhomalt` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_EAS_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_EAS_AN` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_EAS_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_EAS_nhomalt` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_FIN_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_FIN_AN` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_FIN_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_FIN_nhomalt` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_NFE_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_NFE_AN` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_NFE_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_NFE_nhomalt` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_SAS_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_SAS_AN` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_SAS_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_SAS_nhomalt` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_POPMAX_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_POPMAX_AN` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_POPMAX_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_exomes_controls_POPMAX_nhomalt` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_flag` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_AN` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_nhomalt` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_AFR_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_AFR_AN` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_AFR_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_AFR_nhomalt` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_AMR_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_AMR_AN` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_AMR_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_AMR_nhomalt` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_ASJ_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_ASJ_AN` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_ASJ_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_ASJ_nhomalt` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_EAS_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_EAS_AN` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_EAS_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_EAS_nhomalt` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_FIN_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_FIN_AN` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_FIN_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_FIN_nhomalt` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_NFE_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_NFE_AN` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_NFE_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_NFE_nhomalt` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_AMI_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_AMI_AN` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_AMI_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_AMI_nhomalt` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_SAS_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_SAS_AN` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_SAS_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_SAS_nhomalt` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_POPMAX_AC` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_POPMAX_AN` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_POPMAX_AF` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_genomes_POPMAX_nhomalt` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`clinvar_id` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`clinvar_clnsig` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`clinvar_trait` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`clinvar_review` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`clinvar_hgvs` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`clinvar_var_source` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`clinvar_MedGen_id` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`clinvar_OMIM_id` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`clinvar_Orphanet_id` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Interpro_domain` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GTEx_V8_gene` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GTEx_V8_tissue` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Geuvadis_eQTL_target_gene` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`id` int(10) unsigned NOT NULL AUTO_INCREMENT,' >> $TABLENAME.sql
echo 'PRIMARY KEY  (`id`),' >> $TABLENAME.sql
echo 'INDEX `Ensembl` (`Ensembl_geneid`),' >> $TABLENAME.sql
echo 'INDEX `RefSeq` (`genename`),' >> $TABLENAME.sql
echo 'INDEX `Transcript` (`Ensembl_transcriptid`),' >> $TABLENAME.sql
echo 'INDEX `hg19` (`hg19_pos`,`hg19_chr`),' >> $TABLENAME.sql
echo 'INDEX `hg38` (`pos`)' >> $TABLENAME.sql
echo ') ENGINE=MyISAM DEFAULT CHARSET=latin1;' >> $TABLENAME.sql
}

function createSQLgenedb(){
TABLENAME=$1;
echo "DROP TABLE IF EXISTS ${TABLENAME};" > $TABLENAME.sql
echo "CREATE TABLE ${TABLENAME} (" >> $TABLENAME.sql
echo '`Gene_name` VARCHAR(1000) DEFAULT NULL,' >> $TABLENAME.sql
echo '`Ensembl_gene` VARCHAR(1000) DEFAULT NULL,' >> $TABLENAME.sql
echo '`chr` VARCHAR(2) DEFAULT NULL,' >> $TABLENAME.sql
echo '`Gene_old_names` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Gene_other_names` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Uniprot_acc(HGNC/Uniprot)` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Uniprot_id(HGNC/Uniprot)` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Entrez_gene_id` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`CCDS_id` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Refseq_id` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`ucsc_id` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`MIM_id` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`OMIM_id` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Gene_full_name` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Pathway(Uniprot)` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Pathway(BioCarta)_short` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Pathway(BioCarta)_full` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Pathway(ConsensusPathDB)` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Pathway(KEGG)_id` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Pathway(KEGG)_full` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Function_description` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Disease_description` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`MIM_phenotype_id` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`MIM_disease` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Orphanet_disorder_id` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Orphanet_disorder` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Orphanet_association_type` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Trait_association(GWAS)` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`HPO_id` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`HPO_name` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GO_biological_process` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GO_cellular_component` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GO_molecular_function` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Tissue_specificity(Uniprot)` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Expression(egenetics)` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Expression(GNF/Atlas)` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Interactions(IntAct)` LONGTEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Interactions(BioGRID)` LONGTEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Interactions(ConsensusPathDB)` LONGTEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`P(HI)` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`HIPred_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`HIPred` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GHIS` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`P(rec)` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Known_rec_info` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`RVIS_EVS` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`RVIS_percentile_EVS` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`LoF-FDR_ExAC` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`RVIS_ExAC` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`RVIS_percentile_ExAC` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_pLI` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_pRec` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_pNull` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonTCGA_pLI` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonTCGA_pRec` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonTCGA_pNull` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonpsych_pLI` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonpsych_pRec` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_nonpsych_pNull` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_pLI` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_pRec` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`gnomAD_pNull` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_del.score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_dup.score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_cnv.score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`ExAC_cnv_flag` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GDI` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GDI-Phred` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Gene_damage_prediction_all` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Gene_damage_prediction_all_Mendelian` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Gene_damage_prediction_Mendelian_AD` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Gene_damage_prediction_Mendelian_AR` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Gene_damage_prediction_all_PID` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Gene_damage_prediction_PID_AD` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Gene_damage_prediction_PID_AR` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Gene_damage_prediction_all_cancer` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Gene_damage_prediction_cancer_recessive` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Gene_damage_prediction_cancer_dominant` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`LoFtool_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`SORVA_LOF_MAF0.005_HetOrHom` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`SORVA_LOF_MAF0.005_HomOrCompoundHet` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`SORVA_LOF_MAF0.001_HetOrHom` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`SORVA_LOF_MAF0.001_HomOrCompoundHet` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`SORVA_LOForMissense_MAF0.005_HetOrHom` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`SORVA_LOForMissense_MAF0.005_HomOrCompoundHet` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`SORVA_LOForMissense_MAF0.001_HetOrHom` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`SORVA_LOForMissense_MAF0.001_HomOrCompoundHet` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Essential_gene` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Essential_gene_CRISPR` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Essential_gene_CRISPR2` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Essential_gene_gene-trap` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Gene_indispensability_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Gene_indispensability_pred` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`MGI_mouse_gene` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`MGI_mouse_phenotype` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`ZFIN_zebrafish_gene` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`ZFIN_zebrafish_structure` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`ZFIN_zebrafish_phenotype_quality` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`ZFIN_zebrafish_phenotype_tag` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`id` int(10) unsigned NOT NULL AUTO_INCREMENT,' >> $TABLENAME.sql
echo 'PRIMARY KEY  (`id`),' >> $TABLENAME.sql
echo 'KEY `Ensembl` (`Ensembl_gene`)', >> $TABLENAME.sql
echo 'KEY `RefSeq` (`Gene_name`)' >> $TABLENAME.sql
echo ') ENGINE=MyISAM DEFAULT CHARSET=latin1;' >> $TABLENAME.sql
}

function createSQLdbscSNVdb(){
TABLENAME=$1;
echo "DROP TABLE IF EXISTS ${TABLENAME};" > $TABLENAME.sql
echo "CREATE TABLE ${TABLENAME} (" >> $TABLENAME.sql
echo '`chr` VARCHAR(2) COMMENT '"'"'chr: chromosome number'"'"' DEFAULT NULL,' >> $TABLENAME.sql
echo '`pos` INT(11) COMMENT '"'"'pos(1-based): physical position on the chromosome as to hg19 (1-based coordinate)'"'"' DEFAULT NULL,' >> $TABLENAME.sql
echo '`ref` VARCHAR(1) COMMENT '"'"'ref: reference nucleotide allele (as on the + strand)'"'"' DEFAULT NULL,' >> $TABLENAME.sql
echo '`alt` VARCHAR(1) COMMENT '"'"'alt: alternative nucleotide allele (as on the + strand)'"'"' DEFAULT NULL,' >> $TABLENAME.sql
echo '`chr_hg38` VARCHAR(50) DEFAULT NULL,' >> $TABLENAME.sql
echo '`pos_hg38` VARCHAR(20) DEFAULT NULL,' >> $TABLENAME.sql
echo '`is_scSNV_RefSeq` VARCHAR(1) COMMENT '"'"'RefSeq?: whether the SNV is a scSNV according to RefSeq'"'"' DEFAULT NULL,' >> $TABLENAME.sql
echo '`is_scSNV_Ensembl` VARCHAR(1) COMMENT '"'"'Ensembl?: whether the SNV is a scSNV according to Ensembl'"'"' DEFAULT NULL,' >> $TABLENAME.sql
echo '`RefSeq_region` TEXT COMMENT '"'"'RefSeq_region: functional region the SNV located according to RefSeq'"'"' DEFAULT NULL,' >> $TABLENAME.sql
echo '`RefSeq_gene` TEXT COMMENT '"'"'RefSeq_gene: gene name according to RefSeq'"'"' DEFAULT NULL,' >> $TABLENAME.sql
echo '`RefSeq_functional_consequence` TEXT COMMENT '"'"'RefSeq_functional_consequence: functional consequence of the SNV according to RefSeq'"'"' DEFAULT NULL,' >> $TABLENAME.sql
echo '`RefSeq_id_cpchange` TEXT COMMENT '"'"'RefSeq_id_c.change_p.change: SNV in format of c.change and p.change according to RefSeq'"'"' DEFAULT NULL,' >> $TABLENAME.sql
echo '`Ensembl_region` TEXT COMMENT '"'"'Ensembl_region: functional region the SNV located according to Ensembl'"'"' DEFAULT NULL,' >> $TABLENAME.sql
echo '`Ensembl_gene` TEXT COMMENT '"'"'Ensembl_gene: gene id according to Ensembl'"'"' DEFAULT NULL,' >> $TABLENAME.sql
echo '`Ensembl_functional_consequence` TEXT COMMENT '"'"'Ensembl_functional_consequence: functional consequence of the SNV according to Ensembl'"'"' DEFAULT NULL,' >> $TABLENAME.sql
echo '`Ensembl_id_cpchange` TEXT COMMENT '"'"'Ensembl_id_c.change_p.change: SNV in format of c.change and p.change according to Ensembl'"'"' DEFAULT NULL,' >> $TABLENAME.sql
echo '`ada_score` TEXT COMMENT '"'"'ada_score: ensemble prediction score based on ada-boost. Ranges 0 to 1. The larger the score the higher probability the scSNV will affect splicing. The suggested cutoff fora binary prediction (affecting splicing vs. not affecting splicing) is 0.6.'"'"' DEFAULT NULL,' >> $TABLENAME.sql
echo '`rf_score` TEXT COMMENT '"'"'rf_score: ensemble prediction score based on random forests. Ranges 0 to 1. The larger the score the higher probability the scSNV will affect splicing. The suggested cutoff fora binary prediction (affecting splicing vs. not affecting splicing) is 0.6.'"'"' DEFAULT NULL,' >> $TABLENAME.sql
echo '`id` int(10) unsigned NOT NULL AUTO_INCREMENT,' >> $TABLENAME.sql
echo 'PRIMARY KEY  (`id`),' >> $TABLENAME.sql
echo 'INDEX `hg38` (`pos_hg38`,`chr_hg38`)', >> $TABLENAME.sql
echo 'INDEX `hg19` (`pos`)' >> $TABLENAME.sql
echo ') ENGINE=MyISAM DEFAULT CHARSET=latin1;' >> $TABLENAME.sql
}

function createSQLdbMTS(){
TABLENAME=$1;
echo "DROP TABLE IF EXISTS ${TABLENAME};" > $TABLENAME.sql
echo "CREATE TABLE ${TABLENAME} (" >> $TABLENAME.sql
echo '`chr` VARCHAR(50) DEFAULT NULL,' >> $TABLENAME.sql
echo '`pos` INT NOT NULL,' >> $TABLENAME.sql
echo '`ref` VARCHAR(1) DEFAULT NULL,' >> $TABLENAME.sql
echo '`alt` VARCHAR(1) DEFAULT NULL,' >> $TABLENAME.sql
echo '`chr_hg19` VARCHAR(50) DEFAULT NULL,' >> $TABLENAME.sql
echo '`pos_hg19` VARCHAR(20) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ref_hg19` VARCHAR(1) DEFAULT NULL,' >> $TABLENAME.sql
echo '`alt_hg19` VARCHAR(1) DEFAULT NULL,' >> $TABLENAME.sql
echo '`ref_hg19=ref_hg38` VARCHAR(5) DEFAULT NULL,' >> $TABLENAME.sql
echo '`VEP_ensembl_Consequence` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`VEP_ensembl_Transcript_ID` VARCHAR(1000) DEFAULT NULL,' >> $TABLENAME.sql
echo '`VEP_ensembl_Gene_Name` VARCHAR(1000) DEFAULT NULL,' >> $TABLENAME.sql
echo '`VEP_ensembl_Gene_ID` VARCHAR(1000) DEFAULT NULL,' >> $TABLENAME.sql
echo '`VEP_ensembl_Protein_ID` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`VEP_ensembl_CCDS` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`VEP_ensembl_SWISSPROT` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`VEP_ensembl_Codon_Change_or_Distance` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`VEP_ensembl_Amino_Acid_Change` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`VEP_ensembl_HGVSc` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`VEP_ensembl_HGVSp` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`VEP_ensembl_cDNA_position` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`VEP_ensembl_CDS_position` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`VEP_ensembl_Protein_position` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`VEP_ensembl_Exon_or_Intron_Rank` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`VEP_ensembl_STRAND` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`VEP_ensembl_CANONICAL` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`VEP_ensembl_LoF` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`VEP_ensembl_LoF_filter` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`VEP_ensembl_LoF_flags` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`VEP_ensembl_LoF_info` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`VEP_ensembl_summary` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`rs_dbSNP150` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GWAS_catalog_rs` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GWAS_catalog_trait` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GWAS_catalog_pubmedid` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GRASP_rs` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GRASP_PMID` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GRASP_p-value` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GRASP_phenotype` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GRASP_ancestry` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GRASP_platform` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`clinvar_rs` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`clinvar_clnsig` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`clinvar_trait` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`clinvar_golden_stars` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GTEx_V6_gene` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GTEx_V6_tissue` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`1000G_strict_masked` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`RepeatMasker_masked` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Ancestral_allele` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`phyloP46way_primate` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`phyloP46way_primate_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`phyloP20way_mammalian` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`phyloP20way_mammalian_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`phyloP100way_vertebrate` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`phyloP100way_vertebrate_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`phastCons46way_primate` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`phastCons46way_primate_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`phastCons20way_mammalian` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`phastCons20way_mammalian_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`phastCons100way_vertebrate` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`phastCons100way_vertebrate_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GERP_NR` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GERP_RS` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GERP_RS_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`SiPhy_29way_logOdds` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`SiPhy_29way_logOdds_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`integrated_fitCons_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`integrated_fitCons_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`integrated_confidence_value` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GM12878_fitCons_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GM12878_fitCons_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GM12878_confidence_value` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`H1-hESC_fitCons_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`H1-hESC_fitCons_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`H1-hESC_confidence_value` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`HUVEC_fitCons_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`HUVEC_fitCons_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`HUVEC_confidence_value` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GenoCanyon_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`GenoCanyon_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`1000Gp3_AC` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`1000Gp3_AF` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`1000Gp3_AFR_AC` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`1000Gp3_AFR_AF` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`1000Gp3_EUR_AC` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`1000Gp3_EUR_AF` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`1000Gp3_AMR_AC` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`1000Gp3_AMR_AF` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`1000Gp3_EAS_AC` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`1000Gp3_EAS_AF` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`1000Gp3_SAS_AC` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`1000Gp3_SAS_AF` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`UK10K_AC` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`UK10K_AN` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`UK10K_AF` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TWINSUK_AC` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TWINSUK_AN` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TWINSUK_AF` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`ALSPAC_AC` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`ALSPAC_AN` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`ALSPAC_AF` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`RegulomeDB_motif` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`RegulomeDB_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Motif_breaking` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`network_hub` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`ENCODE_annotated` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`sensitive` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`ultra_sensitive` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`target_gene` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`funseq_noncoding_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`funseq2_noncoding_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`funseq2_noncoding_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`CADD_raw` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`CADD_phred` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`CADD_raw_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`DANN_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`DANN_rank_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`fathmm-MKL_non-coding_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`fathmm-MKL_non-coding_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`fathmm-MKL_non-coding_pred` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`fathmm-MKL_non-coding_group` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`fathmm-MKL_coding_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`fathmm-MKL_coding_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`fathmm-MKL_coding_pred` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`fathmm-MKL_coding_group` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Eigen_coding_or_noncoding` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Eigen-raw` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Eigen-phred` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Eigen-PC-raw` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`Eigen-PC-phred` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M_ref_raw_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M_ref_raw_miR` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M_ref_raw_gene` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M_ref_exp_cor_tumo` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M_ref_cor_tis_tumo` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M_ref_exp_cor_norm` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M_ref_cor_tis_norm` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M_ref_best_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M_ref_best_miR` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M_ref_best_gene` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M_ref_worst_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M_ref_worst_miR` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M_ref_worst_gene` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M_alt_raw_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M_alt_raw_miR` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M_alt_raw_gene` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M_alt_exp_cor_tumo` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M_alt_cor_tis_tumo` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M_alt_exp_cor_norm` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M_alt_cor_tis_norm` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M_alt_best_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M_alt_best_miR` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M_alt_best_gene` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M_alt_worst_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M_alt_worst_miR` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M_alt_worst_gene` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M_max_dif` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M_max_gene` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`M_cat` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TS_ref_raw_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TS_ref_raw_miR` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TS_ref_raw_gene` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TS_ref_exp_cor_tumo` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TS_ref_cor_tis_tumo` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TS_ref_exp_cor_norm` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TS_ref_cor_tis_norm` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TS_ref_best_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TS_ref_best_miR` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TS_ref_best_gene` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TS_ref_worst_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TS_ref_worst_miR` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TS_ref_worst_gene` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TS_alt_raw_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TS_alt_raw_miR` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TS_alt_raw_gene` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TS_alt_exp_cor_tumo` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TS_alt_cor_tis_tumo` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TS_alt_exp_cor_norm` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TS_alt_cor_tis_norm` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TS_alt_best_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TS_alt_best_miR` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TS_alt_best_gene` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TS_alt_worst_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TS_alt_worst_miR` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TS_alt_worst_gene` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TS_max_dif` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TS_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TS_max_gene` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`TS_cat` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`R_ref_raw_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`R_ref_raw_miR` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`R_ref_raw_gene` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`R_ref_exp_cor_tumo` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`R_ref_cor_tis_tumo` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`R_ref_exp_cor_norm` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`R_ref_cor_tis_norm` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`R_ref_best_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`R_ref_best_miR` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`R_ref_best_gene` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`R_ref_worst_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`R_ref_worst_miR` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`R_ref_worst_gene` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`R_alt_raw_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`R_alt_raw_miR` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`R_alt_raw_gene` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`R_alt_exp_cor_tumo` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`R_alt_cor_tis_tumo` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`R_alt_exp_cor_norm` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`R_alt_cor_tis_norm` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`R_alt_best_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`R_alt_best_miR` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`R_alt_best_gene` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`R_alt_worst_score` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`R_alt_worst_miR` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`R_alt_worst_gene` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`R_max_dif` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`R_rankscore` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`R_max_gene` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`R_cat` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`id` int(10) unsigned NOT NULL AUTO_INCREMENT,' >> $TABLENAME.sql
echo 'PRIMARY KEY  (`id`),' >> $TABLENAME.sql
echo 'INDEX `Transcript` (`VEP_ensembl_Transcript_ID`)', >> $TABLENAME.sql
echo 'INDEX `RefSeq` (`VEP_ensembl_Gene_Name`)', >> $TABLENAME.sql
echo 'INDEX `Ensembl` (`VEP_ensembl_Gene_ID`)', >> $TABLENAME.sql
echo 'INDEX `hg19` (`pos_hg19`,`chr_hg19`)', >> $TABLENAME.sql
echo 'INDEX `hg38` (`pos`)' >> $TABLENAME.sql
echo ') ENGINE=MyISAM DEFAULT CHARSET=latin1;' >> $TABLENAME.sql
}


### Script

## dbMTS is only for microRNA in WES and takes hundreds of Gb as database, so skip it for now

echo "downloading '${DBNSFP}'"
cd ${HIGHLANDERPATH}
mkdir -p temp/${DBNSFP}
cd temp/${DBNSFP}
wget --no-check-certificate ${ZIP} -O ${DBNSFP}.zip
wget --no-check-certificate ${DBSCSNV} -O dbscSNV.zip
#wget --no-check-certificate ${DBMTS}/dbMTS1.0_gene.gz
#for CHR in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 X Y M
#do
#wget --no-check-certificate ${DBMTS}/dbMTS1.0.chr${CHR}.gz
#echo "extracting dbMTS1.0.chr${CHR}.gz"
#gzip -d dbMTS1.0.chr${CHR}.gz
#mv dbMTS1.0.chr${CHR} dbMTS_1_0.chr${CHR}
#done

echo "extracting '${DBNSFP}' from archives"
echo "extracting ${DBNSFP}.zip"
jar xf ${DBNSFP}.zip
echo "extracting dbscSNV.zip"
jar xf dbscSNV.zip
for CHR in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 X Y M
do
echo "extracting dbNSFP${VERSION}a_variant.chr${CHR}.gz"
gzip -d dbNSFP${VERSION}a_variant.chr${CHR}.gz
mv dbNSFP${VERSION}a_variant.chr${CHR} dbNSFP_${SQL}.variant.chr${CHR}
done
#echo "extracting dbNSFP${VERSION}_gene.gz"
#gzip -d dbNSFP${VERSION}_gene.gz
#mv dbNSFP${VERSION}_gene dbNSFP_${SQL}_gene
echo "extracting dbNSFP${VERSION}_gene.complete.gz"
gzip -d dbNSFP${VERSION}_gene.complete.gz
mv dbNSFP${VERSION}_gene.complete dbNSFP_${SQL}_gene.complete
for CHR in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 X Y
do
mv dbscSNV1.1.chr${CHR} dbscSNV_1_1.chr${CHR}
done
#echo "extracting dbMTS1.0_gene.gz"
#gzip -d dbMTS1.0_gene.gz
#mv dbMTS1.0_gene dbMTS_1_0_gene
chmod 666 *

echo "Creating database"
mysql -u${DBUSER} -p${DBPASS} -e"CREATE DATABASE IF NOT EXISTS dbNSFP_${SQL};"
for CHR in M Y 21 X 22 20 19 18 17 16 15 14 13 12 11 10 9 8 7 6 5 4 3 2 1
do
echo "- Chromosome ${CHR} for dbNSFP"
createSQL chromosome_${CHR}
mysql -u${DBUSER} -p${DBPASS} dbNSFP_${SQL} < chromosome_${CHR}.sql
#echo "- Chromosome ${CHR} for dbMTS"
#createSQLdbMTS dbMTS_chr${CHR}
#mysql -u${DBUSER} -p${DBPASS} dbNSFP_${SQL} < dbMTS_chr${CHR}.sql
done
for CHR in Y 21 X 22 20 19 18 17 16 15 14 13 12 11 10 9 8 7 6 5 4 3 2 1
do
echo "- Chromosome ${CHR} for dbscSNV"
createSQLdbscSNVdb dbscSNV_chr${CHR}
mysql -u${DBUSER} -p${DBPASS} dbNSFP_${SQL} < dbscSNV_chr${CHR}.sql
done
echo "- Genes for dbNSFP"
createSQLgenedb genes
mysql -u${DBUSER} -p${DBPASS} dbNSFP_${SQL} < genes.sql

echo "Populating databases"
for CHR in M Y 21 X 22 20 19 18 17 16 15 14 13 12 11 10 9 8 7 6 5 4 3 2 1
do
echo "- Chromosome ${CHR}"
mysql -u${DBUSER} -p${DBPASS} --execute="LOAD DATA LOCAL INFILE '${HIGHLANDERPATH}/temp/${DBNSFP}/dbNSFP_${SQL}.variant.chr${CHR}' INTO TABLE dbNSFP_${SQL}.chromosome_${CHR} IGNORE 1 LINES (\`chr\`,\`pos\`,\`ref\`,\`alt\`,\`aaref\`,\`aaalt\`,\`rs_dbSNP151\`,\`hg19_chr\`,\`hg19_pos\`,\`hg18_chr\`,\`hg18_pos\`,\`aapos\`,\`genename\`,\`Ensembl_geneid\`,\`Ensembl_transcriptid\`,\`Ensembl_proteinid\`,\`Uniprot_acc\`,\`Uniprot_entry\`,\`HGVSc_ANNOVAR\`,\`HGVSp_ANNOVAR\`,\`HGVSc_snpEff\`,\`HGVSp_snpEff\`,\`HGVSc_VEP\`,\`HGVSp_VEP\`,\`APPRIS\`,\`GENCODE_basic\`,\`TSL\`,\`VEP_canonical\`,\`cds_strand\`,\`refcodon\`,\`codonpos\`,\`codon_degeneracy\`,\`Ancestral_allele\`,\`AltaiNeandertal\`,\`Denisova\`,\`VindijiaNeandertal\`,\`SIFT_score\`,\`SIFT_converted_rankscore\`,\`SIFT_pred\`,\`SIFT4G_score\`,\`SIFT4G_converted_rankscore\`,\`SIFT4G_pred\`,\`Polyphen2_HDIV_score\`,\`Polyphen2_HDIV_rankscore\`,\`Polyphen2_HDIV_pred\`,\`Polyphen2_HVAR_score\`,\`Polyphen2_HVAR_rankscore\`,\`Polyphen2_HVAR_pred\`,\`LRT_score\`,\`LRT_converted_rankscore\`,\`LRT_pred\`,\`LRT_Omega\`,\`MutationTaster_score\`,\`MutationTaster_converted_rankscore\`,\`MutationTaster_pred\`,\`MutationTaster_model\`,\`MutationTaster_AAE\`,\`MutationAssessor_score\`,\`MutationAssessor_rankscore\`,\`MutationAssessor_pred\`,\`FATHMM_score\`,\`FATHMM_converted_rankscore\`,\`FATHMM_pred\`,\`PROVEAN_score\`,\`PROVEAN_converted_rankscore\`,\`PROVEAN_pred\`,\`VEST4_score\`,\`VEST4_rankscore\`,\`MetaSVM_score\`,\`MetaSVM_rankscore\`,\`MetaSVM_pred\`,\`MetaLR_score\`,\`MetaLR_rankscore\`,\`MetaLR_pred\`,\`Reliability_index\`,\`M-CAP_score\`,\`M-CAP_rankscore\`,\`M-CAP_pred\`,\`REVEL_score\`,\`REVEL_rankscore\`,\`MutPred_score\`,\`MutPred_rankscore\`,\`MutPred_protID\`,\`MutPred_AAchange\`,\`MutPred_Top5features\`,\`MVP_score\`,\`MVP_rankscore\`,\`MPC_score\`,\`MPC_rankscore\`,\`PrimateAI_score\`,\`PrimateAI_rankscore\`,\`PrimateAI_pred\`,\`DEOGEN2_score\`,\`DEOGEN2_rankscore\`,\`DEOGEN2_pred\`,\`BayesDel_addAF_score\`,\`BayesDel_addAF_rankscore\`,\`BayesDel_addAF_pred\`,\`BayesDel_noAF_score\`,\`BayesDel_noAF_rankscore\`,\`BayesDel_noAF_pred\`,\`ClinPred_score\`,\`ClinPred_rankscore\`,\`ClinPred_pred\`,\`LIST-S2_score\`,\`LIST-S2_rankscore\`,\`LIST-S2_pred\`,\`Aloft_Fraction_transcripts_affected\`,\`Aloft_prob_Tolerant\`,\`Aloft_prob_Recessive\`,\`Aloft_prob_Dominant\`,\`Aloft_pred\`,\`Aloft_Confidence\`,\`CADD_raw\`,\`CADD_raw_rankscore\`,\`CADD_phred\`,\`CADD_raw_hg19\`,\`CADD_raw_rankscore_hg19\`,\`CADD_phred_hg19\`,\`DANN_score\`,\`DANN_rankscore\`,\`fathmm-MKL_coding_score\`,\`fathmm-MKL_coding_rankscore\`,\`fathmm-MKL_coding_pred\`,\`fathmm-MKL_coding_group\`,\`fathmm-XF_coding_score\`,\`fathmm-XF_coding_rankscore\`,\`fathmm-XF_coding_pred\`,\`Eigen-raw_coding\`,\`Eigen-raw_coding_rankscore\`,\`Eigen-phred_coding\`,\`Eigen-PC-raw_coding\`,\`Eigen-PC-raw_coding_rankscore\`,\`Eigen-PC-phred_coding\`,\`GenoCanyon_score\`,\`GenoCanyon_score_rankscore\`,\`integrated_fitCons_score\`,\`integrated_fitCons_rankscore\`,\`integrated_confidence_value\`,\`GM12878_fitCons_score\`,\`GM12878_fitCons_rankscore\`,\`GM12878_confidence_value\`,\`H1-hESC_fitCons_score\`,\`H1-hESC_fitCons_rankscore\`,\`H1-hESC_confidence_value\`,\`HUVEC_fitCons_score\`,\`HUVEC_fitCons_rankscore\`,\`HUVEC_confidence_value\`,\`LINSIGHT\`,\`LINSIGHT_rankscore\`,\`GERP++_NR\`,\`GERP++_RS\`,\`GERP++_RS_rankscore\`,\`phyloP100way_vertebrate\`,\`phyloP100way_vertebrate_rankscore\`,\`phyloP30way_mammalian\`,\`phyloP30way_mammalian_rankscore\`,\`phyloP17way_primate\`,\`phyloP17way_primate_rankscore\`,\`phastCons100way_vertebrate\`,\`phastCons100way_vertebrate_rankscore\`,\`phastCons30way_mammalian\`,\`phastCons30way_mammalian_rankscore\`,\`phastCons17way_primate\`,\`phastCons17way_primate_rankscore\`,\`SiPhy_29way_pi\`,\`SiPhy_29way_logOdds\`,\`SiPhy_29way_logOdds_rankscore\`,\`bStatistic\`,\`bStatistic_converted_rankscore\`,\`1000Gp3_AC\`,\`1000Gp3_AF\`,\`1000Gp3_AFR_AC\`,\`1000Gp3_AFR_AF\`,\`1000Gp3_EUR_AC\`,\`1000Gp3_EUR_AF\`,\`1000Gp3_AMR_AC\`,\`1000Gp3_AMR_AF\`,\`1000Gp3_EAS_AC\`,\`1000Gp3_EAS_AF\`,\`1000Gp3_SAS_AC\`,\`1000Gp3_SAS_AF\`,\`TWINSUK_AC\`,\`TWINSUK_AF\`,\`ALSPAC_AC\`,\`ALSPAC_AF\`,\`UK10K_AC\`,\`UK10K_AF\`,\`ESP6500_AA_AC\`,\`ESP6500_AA_AF\`,\`ESP6500_EA_AC\`,\`ESP6500_EA_AF\`,\`ExAC_AC\`,\`ExAC_AF\`,\`ExAC_Adj_AC\`,\`ExAC_Adj_AF\`,\`ExAC_AFR_AC\`,\`ExAC_AFR_AF\`,\`ExAC_AMR_AC\`,\`ExAC_AMR_AF\`,\`ExAC_EAS_AC\`,\`ExAC_EAS_AF\`,\`ExAC_FIN_AC\`,\`ExAC_FIN_AF\`,\`ExAC_NFE_AC\`,\`ExAC_NFE_AF\`,\`ExAC_SAS_AC\`,\`ExAC_SAS_AF\`,\`ExAC_nonTCGA_AC\`,\`ExAC_nonTCGA_AF\`,\`ExAC_nonTCGA_Adj_AC\`,\`ExAC_nonTCGA_Adj_AF\`,\`ExAC_nonTCGA_AFR_AC\`,\`ExAC_nonTCGA_AFR_AF\`,\`ExAC_nonTCGA_AMR_AC\`,\`ExAC_nonTCGA_AMR_AF\`,\`ExAC_nonTCGA_EAS_AC\`,\`ExAC_nonTCGA_EAS_AF\`,\`ExAC_nonTCGA_FIN_AC\`,\`ExAC_nonTCGA_FIN_AF\`,\`ExAC_nonTCGA_NFE_AC\`,\`ExAC_nonTCGA_NFE_AF\`,\`ExAC_nonTCGA_SAS_AC\`,\`ExAC_nonTCGA_SAS_AF\`,\`ExAC_nonpsych_AC\`,\`ExAC_nonpsych_AF\`,\`ExAC_nonpsych_Adj_AC\`,\`ExAC_nonpsych_Adj_AF\`,\`ExAC_nonpsych_AFR_AC\`,\`ExAC_nonpsych_AFR_AF\`,\`ExAC_nonpsych_AMR_AC\`,\`ExAC_nonpsych_AMR_AF\`,\`ExAC_nonpsych_EAS_AC\`,\`ExAC_nonpsych_EAS_AF\`,\`ExAC_nonpsych_FIN_AC\`,\`ExAC_nonpsych_FIN_AF\`,\`ExAC_nonpsych_NFE_AC\`,\`ExAC_nonpsych_NFE_AF\`,\`ExAC_nonpsych_SAS_AC\`,\`ExAC_nonpsych_SAS_AF\`,\`gnomAD_exomes_flag\`,\`gnomAD_exomes_AC\`,\`gnomAD_exomes_AN\`,\`gnomAD_exomes_AF\`,\`gnomAD_exomes_nhomalt\`,\`gnomAD_exomes_AFR_AC\`,\`gnomAD_exomes_AFR_AN\`,\`gnomAD_exomes_AFR_AF\`,\`gnomAD_exomes_AFR_nhomalt\`,\`gnomAD_exomes_AMR_AC\`,\`gnomAD_exomes_AMR_AN\`,\`gnomAD_exomes_AMR_AF\`,\`gnomAD_exomes_AMR_nhomalt\`,\`gnomAD_exomes_ASJ_AC\`,\`gnomAD_exomes_ASJ_AN\`,\`gnomAD_exomes_ASJ_AF\`,\`gnomAD_exomes_ASJ_nhomalt\`,\`gnomAD_exomes_EAS_AC\`,\`gnomAD_exomes_EAS_AN\`,\`gnomAD_exomes_EAS_AF\`,\`gnomAD_exomes_EAS_nhomalt\`,\`gnomAD_exomes_FIN_AC\`,\`gnomAD_exomes_FIN_AN\`,\`gnomAD_exomes_FIN_AF\`,\`gnomAD_exomes_FIN_nhomalt\`,\`gnomAD_exomes_NFE_AC\`,\`gnomAD_exomes_NFE_AN\`,\`gnomAD_exomes_NFE_AF\`,\`gnomAD_exomes_NFE_nhomalt\`,\`gnomAD_exomes_SAS_AC\`,\`gnomAD_exomes_SAS_AN\`,\`gnomAD_exomes_SAS_AF\`,\`gnomAD_exomes_SAS_nhomalt\`,\`gnomAD_exomes_POPMAX_AC\`,\`gnomAD_exomes_POPMAX_AN\`,\`gnomAD_exomes_POPMAX_AF\`,\`gnomAD_exomes_POPMAX_nhomalt\`,\`gnomAD_exomes_controls_AC\`,\`gnomAD_exomes_controls_AN\`,\`gnomAD_exomes_controls_AF\`,\`gnomAD_exomes_controls_nhomalt\`,\`gnomAD_exomes_controls_AFR_AC\`,\`gnomAD_exomes_controls_AFR_AN\`,\`gnomAD_exomes_controls_AFR_AF\`,\`gnomAD_exomes_controls_AFR_nhomalt\`,\`gnomAD_exomes_controls_AMR_AC\`,\`gnomAD_exomes_controls_AMR_AN\`,\`gnomAD_exomes_controls_AMR_AF\`,\`gnomAD_exomes_controls_AMR_nhomalt\`,\`gnomAD_exomes_controls_ASJ_AC\`,\`gnomAD_exomes_controls_ASJ_AN\`,\`gnomAD_exomes_controls_ASJ_AF\`,\`gnomAD_exomes_controls_ASJ_nhomalt\`,\`gnomAD_exomes_controls_EAS_AC\`,\`gnomAD_exomes_controls_EAS_AN\`,\`gnomAD_exomes_controls_EAS_AF\`,\`gnomAD_exomes_controls_EAS_nhomalt\`,\`gnomAD_exomes_controls_FIN_AC\`,\`gnomAD_exomes_controls_FIN_AN\`,\`gnomAD_exomes_controls_FIN_AF\`,\`gnomAD_exomes_controls_FIN_nhomalt\`,\`gnomAD_exomes_controls_NFE_AC\`,\`gnomAD_exomes_controls_NFE_AN\`,\`gnomAD_exomes_controls_NFE_AF\`,\`gnomAD_exomes_controls_NFE_nhomalt\`,\`gnomAD_exomes_controls_SAS_AC\`,\`gnomAD_exomes_controls_SAS_AN\`,\`gnomAD_exomes_controls_SAS_AF\`,\`gnomAD_exomes_controls_SAS_nhomalt\`,\`gnomAD_exomes_controls_POPMAX_AC\`,\`gnomAD_exomes_controls_POPMAX_AN\`,\`gnomAD_exomes_controls_POPMAX_AF\`,\`gnomAD_exomes_controls_POPMAX_nhomalt\`,\`gnomAD_genomes_flag\`,\`gnomAD_genomes_AC\`,\`gnomAD_genomes_AN\`,\`gnomAD_genomes_AF\`,\`gnomAD_genomes_nhomalt\`,\`gnomAD_genomes_AFR_AC\`,\`gnomAD_genomes_AFR_AN\`,\`gnomAD_genomes_AFR_AF\`,\`gnomAD_genomes_AFR_nhomalt\`,\`gnomAD_genomes_AMR_AC\`,\`gnomAD_genomes_AMR_AN\`,\`gnomAD_genomes_AMR_AF\`,\`gnomAD_genomes_AMR_nhomalt\`,\`gnomAD_genomes_ASJ_AC\`,\`gnomAD_genomes_ASJ_AN\`,\`gnomAD_genomes_ASJ_AF\`,\`gnomAD_genomes_ASJ_nhomalt\`,\`gnomAD_genomes_EAS_AC\`,\`gnomAD_genomes_EAS_AN\`,\`gnomAD_genomes_EAS_AF\`,\`gnomAD_genomes_EAS_nhomalt\`,\`gnomAD_genomes_FIN_AC\`,\`gnomAD_genomes_FIN_AN\`,\`gnomAD_genomes_FIN_AF\`,\`gnomAD_genomes_FIN_nhomalt\`,\`gnomAD_genomes_NFE_AC\`,\`gnomAD_genomes_NFE_AN\`,\`gnomAD_genomes_NFE_AF\`,\`gnomAD_genomes_NFE_nhomalt\`,\`gnomAD_genomes_AMI_AC\`,\`gnomAD_genomes_AMI_AN\`,\`gnomAD_genomes_AMI_AF\`,\`gnomAD_genomes_AMI_nhomalt\`,\`gnomAD_genomes_SAS_AC\`,\`gnomAD_genomes_SAS_AN\`,\`gnomAD_genomes_SAS_AF\`,\`gnomAD_genomes_SAS_nhomalt\`,\`gnomAD_genomes_POPMAX_AC\`,\`gnomAD_genomes_POPMAX_AN\`,\`gnomAD_genomes_POPMAX_AF\`,\`gnomAD_genomes_POPMAX_nhomalt\`,\`clinvar_id\`,\`clinvar_clnsig\`,\`clinvar_trait\`,\`clinvar_review\`,\`clinvar_hgvs\`,\`clinvar_var_source\`,\`clinvar_MedGen_id\`,\`clinvar_OMIM_id\`,\`clinvar_Orphanet_id\`,\`Interpro_domain\`,\`GTEx_V8_gene\`,\`GTEx_V8_tissue\`,\`Geuvadis_eQTL_target_gene\`); SHOW WARNINGS\G; SELECT @@warning_count" > chromosome_${CHR}.sql.output
cat chromosome_${CHR}.sql.output
#echo "- dbMTS ${CHR}"
#mysql -u${DBUSER} -p${DBPASS} --execute="LOAD DATA LOCAL INFILE '${HIGHLANDERPATH}/temp/${DBNSFP}/dbMTS_1_0.chr${CHR}' INTO TABLE dbNSFP_${SQL}.dbMTS_chr${CHR} IGNORE 1 LINES (\`chr\`,\`pos\`,\`ref\`,\`alt\`,\`chr_hg19\`,\`pos_hg19\`,\`ref_hg19\`,\`alt_hg19\`,\`ref_hg19=ref_hg38\`,\`VEP_ensembl_Consequence\`,\`VEP_ensembl_Transcript_ID\`,\`VEP_ensembl_Gene_Name\`,\`VEP_ensembl_Gene_ID\`,\`VEP_ensembl_Protein_ID\`,\`VEP_ensembl_CCDS\`,\`VEP_ensembl_SWISSPROT\`,\`VEP_ensembl_Codon_Change_or_Distance\`,\`VEP_ensembl_Amino_Acid_Change\`,\`VEP_ensembl_HGVSc\`,\`VEP_ensembl_HGVSp\`,\`VEP_ensembl_cDNA_position\`,\`VEP_ensembl_CDS_position\`,\`VEP_ensembl_Protein_position\`,\`VEP_ensembl_Exon_or_Intron_Rank\`,\`VEP_ensembl_STRAND\`,\`VEP_ensembl_CANONICAL\`,\`VEP_ensembl_LoF\`,\`VEP_ensembl_LoF_filter\`,\`VEP_ensembl_LoF_flags\`,\`VEP_ensembl_LoF_info\`,\`VEP_ensembl_summary\`,\`rs_dbSNP150\`,\`GWAS_catalog_rs\`,\`GWAS_catalog_trait\`,\`GWAS_catalog_pubmedid\`,\`GRASP_rs\`,\`GRASP_PMID\`,\`GRASP_p-value\`,\`GRASP_phenotype\`,\`GRASP_ancestry\`,\`GRASP_platform\`,\`clinvar_rs\`,\`clinvar_clnsig\`,\`clinvar_trait\`,\`clinvar_golden_stars\`,\`GTEx_V6_gene\`,\`GTEx_V6_tissue\`,\`1000G_strict_masked\`,\`RepeatMasker_masked\`,\`Ancestral_allele\`,\`phyloP46way_primate\`,\`phyloP46way_primate_rankscore\`,\`phyloP20way_mammalian\`,\`phyloP20way_mammalian_rankscore\`,\`phyloP100way_vertebrate\`,\`phyloP100way_vertebrate_rankscore\`,\`phastCons46way_primate\`,\`phastCons46way_primate_rankscore\`,\`phastCons20way_mammalian\`,\`phastCons20way_mammalian_rankscore\`,\`phastCons100way_vertebrate\`,\`phastCons100way_vertebrate_rankscore\`,\`GERP_NR\`,\`GERP_RS\`,\`GERP_RS_rankscore\`,\`SiPhy_29way_logOdds\`,\`SiPhy_29way_logOdds_rankscore\`,\`integrated_fitCons_score\`,\`integrated_fitCons_rankscore\`,\`integrated_confidence_value\`,\`GM12878_fitCons_score\`,\`GM12878_fitCons_rankscore\`,\`GM12878_confidence_value\`,\`H1-hESC_fitCons_score\`,\`H1-hESC_fitCons_rankscore\`,\`H1-hESC_confidence_value\`,\`HUVEC_fitCons_score\`,\`HUVEC_fitCons_rankscore\`,\`HUVEC_confidence_value\`,\`GenoCanyon_score\`,\`GenoCanyon_rankscore\`,\`1000Gp3_AC\`,\`1000Gp3_AF\`,\`1000Gp3_AFR_AC\`,\`1000Gp3_AFR_AF\`,\`1000Gp3_EUR_AC\`,\`1000Gp3_EUR_AF\`,\`1000Gp3_AMR_AC\`,\`1000Gp3_AMR_AF\`,\`1000Gp3_EAS_AC\`,\`1000Gp3_EAS_AF\`,\`1000Gp3_SAS_AC\`,\`1000Gp3_SAS_AF\`,\`UK10K_AC\`,\`UK10K_AN\`,\`UK10K_AF\`,\`TWINSUK_AC\`,\`TWINSUK_AN\`,\`TWINSUK_AF\`,\`ALSPAC_AC\`,\`ALSPAC_AN\`,\`ALSPAC_AF\`,\`RegulomeDB_motif\`,\`RegulomeDB_score\`,\`Motif_breaking\`,\`network_hub\`,\`ENCODE_annotated\`,\`sensitive\`,\`ultra_sensitive\`,\`target_gene\`,\`funseq_noncoding_score\`,\`funseq2_noncoding_score\`,\`funseq2_noncoding_rankscore\`,\`CADD_raw\`,\`CADD_phred\`,\`CADD_raw_rankscore\`,\`DANN_score\`,\`DANN_rank_score\`,\`fathmm-MKL_non-coding_score\`,\`fathmm-MKL_non-coding_rankscore\`,\`fathmm-MKL_non-coding_pred\`,\`fathmm-MKL_non-coding_group\`,\`fathmm-MKL_coding_score\`,\`fathmm-MKL_coding_rankscore\`,\`fathmm-MKL_coding_pred\`,\`fathmm-MKL_coding_group\`,\`Eigen_coding_or_noncoding\`,\`Eigen-raw\`,\`Eigen-phred\`,\`Eigen-PC-raw\`,\`Eigen-PC-phred\`,\`M_ref_raw_score\`,\`M_ref_raw_miR\`,\`M_ref_raw_gene\`,\`M_ref_exp_cor_tumo\`,\`M_ref_cor_tis_tumo\`,\`M_ref_exp_cor_norm\`,\`M_ref_cor_tis_norm\`,\`M_ref_best_score\`,\`M_ref_best_miR\`,\`M_ref_best_gene\`,\`M_ref_worst_score\`,\`M_ref_worst_miR\`,\`M_ref_worst_gene\`,\`M_alt_raw_score\`,\`M_alt_raw_miR\`,\`M_alt_raw_gene\`,\`M_alt_exp_cor_tumo\`,\`M_alt_cor_tis_tumo\`,\`M_alt_exp_cor_norm\`,\`M_alt_cor_tis_norm\`,\`M_alt_best_score\`,\`M_alt_best_miR\`,\`M_alt_best_gene\`,\`M_alt_worst_score\`,\`M_alt_worst_miR\`,\`M_alt_worst_gene\`,\`M_max_dif\`,\`M_rankscore\`,\`M_max_gene\`,\`M_cat\`,\`TS_ref_raw_score\`,\`TS_ref_raw_miR\`,\`TS_ref_raw_gene\`,\`TS_ref_exp_cor_tumo\`,\`TS_ref_cor_tis_tumo\`,\`TS_ref_exp_cor_norm\`,\`TS_ref_cor_tis_norm\`,\`TS_ref_best_score\`,\`TS_ref_best_miR\`,\`TS_ref_best_gene\`,\`TS_ref_worst_score\`,\`TS_ref_worst_miR\`,\`TS_ref_worst_gene\`,\`TS_alt_raw_score\`,\`TS_alt_raw_miR\`,\`TS_alt_raw_gene\`,\`TS_alt_exp_cor_tumo\`,\`TS_alt_cor_tis_tumo\`,\`TS_alt_exp_cor_norm\`,\`TS_alt_cor_tis_norm\`,\`TS_alt_best_score\`,\`TS_alt_best_miR\`,\`TS_alt_best_gene\`,\`TS_alt_worst_score\`,\`TS_alt_worst_miR\`,\`TS_alt_worst_gene\`,\`TS_max_dif\`,\`TS_rankscore\`,\`TS_max_gene\`,\`TS_cat\`,\`R_ref_raw_score\`,\`R_ref_raw_miR\`,\`R_ref_raw_gene\`,\`R_ref_exp_cor_tumo\`,\`R_ref_cor_tis_tumo\`,\`R_ref_exp_cor_norm\`,\`R_ref_cor_tis_norm\`,\`R_ref_best_score\`,\`R_ref_best_miR\`,\`R_ref_best_gene\`,\`R_ref_worst_score\`,\`R_ref_worst_miR\`,\`R_ref_worst_gene\`,\`R_alt_raw_score\`,\`R_alt_raw_miR\`,\`R_alt_raw_gene\`,\`R_alt_exp_cor_tumo\`,\`R_alt_cor_tis_tumo\`,\`R_alt_exp_cor_norm\`,\`R_alt_cor_tis_norm\`,\`R_alt_best_score\`,\`R_alt_best_miR\`,\`R_alt_best_gene\`,\`R_alt_worst_score\`,\`R_alt_worst_miR\`,\`R_alt_worst_gene\`,\`R_max_dif\`,\`R_rankscore\`,\`R_max_gene\`,\`R_cat\`); SHOW WARNINGS\G; SELECT @@warning_count" > dbMTS_${CHR}.sql.output
#cat dbMTS_${CHR}.sql.output
done
for CHR in Y 21 X 22 20 19 18 17 16 15 14 13 12 11 10 9 8 7 6 5 4 3 2 1
do
echo "- dbscSNV ${CHR}"
mysql -u${DBUSER} -p${DBPASS} --execute="LOAD DATA LOCAL INFILE '${HIGHLANDERPATH}/temp/${DBNSFP}/dbscSNV_1_1.chr${CHR}' INTO TABLE dbNSFP_${SQL}.dbscSNV_chr${CHR} IGNORE 1 LINES (\`chr\`,\`pos\`,\`ref\`,\`alt\`,\`chr_hg38\`,\`pos_hg38\`,\`is_scSNV_RefSeq\`,\`is_scSNV_Ensembl\`,\`RefSeq_region\`,\`RefSeq_gene\`,\`RefSeq_functional_consequence\`,\`RefSeq_id_cpchange\`,\`Ensembl_region\`,\`Ensembl_gene\`,\`Ensembl_functional_consequence\`,\`Ensembl_id_cpchange\`,\`ada_score\`,\`rf_score\`); SHOW WARNINGS\G; SELECT @@warning_count" > dbscSNV_${CHR}.sql.output
cat dbscSNV_${CHR}.sql.output
done
echo "- Genes"
mysql -u${DBUSER} -p${DBPASS} --execute="LOAD DATA LOCAL INFILE '${HIGHLANDERPATH}/temp/${DBNSFP}/dbNSFP_${SQL}_gene.complete' INTO TABLE dbNSFP_${SQL}.genes IGNORE 1 LINES (\`Gene_name\`,\`Ensembl_gene\`,\`chr\`,\`Gene_old_names\`,\`Gene_other_names\`,\`Uniprot_acc(HGNC/Uniprot)\`,\`Uniprot_id(HGNC/Uniprot)\`,\`Entrez_gene_id\`,\`CCDS_id\`,\`Refseq_id\`,\`ucsc_id\`,\`MIM_id\`,\`OMIM_id\`,\`Gene_full_name\`,\`Pathway(Uniprot)\`,\`Pathway(BioCarta)_short\`,\`Pathway(BioCarta)_full\`,\`Pathway(ConsensusPathDB)\`,\`Pathway(KEGG)_id\`,\`Pathway(KEGG)_full\`,\`Function_description\`,\`Disease_description\`,\`MIM_phenotype_id\`,\`MIM_disease\`,\`Orphanet_disorder_id\`,\`Orphanet_disorder\`,\`Orphanet_association_type\`,\`Trait_association(GWAS)\`,\`HPO_id\`,\`HPO_name\`,\`GO_biological_process\`,\`GO_cellular_component\`,\`GO_molecular_function\`,\`Tissue_specificity(Uniprot)\`,\`Expression(egenetics)\`,\`Expression(GNF/Atlas)\`,\`Interactions(IntAct)\`,\`Interactions(BioGRID)\`,\`Interactions(ConsensusPathDB)\`,\`P(HI)\`,\`HIPred_score\`,\`HIPred\`,\`GHIS\`,\`P(rec)\`,\`Known_rec_info\`,\`RVIS_EVS\`,\`RVIS_percentile_EVS\`,\`LoF-FDR_ExAC\`,\`RVIS_ExAC\`,\`RVIS_percentile_ExAC\`,\`ExAC_pLI\`,\`ExAC_pRec\`,\`ExAC_pNull\`,\`ExAC_nonTCGA_pLI\`,\`ExAC_nonTCGA_pRec\`,\`ExAC_nonTCGA_pNull\`,\`ExAC_nonpsych_pLI\`,\`ExAC_nonpsych_pRec\`,\`ExAC_nonpsych_pNull\`,\`gnomAD_pLI\`,\`gnomAD_pRec\`,\`gnomAD_pNull\`,\`ExAC_del.score\`,\`ExAC_dup.score\`,\`ExAC_cnv.score\`,\`ExAC_cnv_flag\`,\`GDI\`,\`GDI-Phred\`,\`Gene_damage_prediction_all\`,\`Gene_damage_prediction_all_Mendelian\`,\`Gene_damage_prediction_Mendelian_AD\`,\`Gene_damage_prediction_Mendelian_AR\`,\`Gene_damage_prediction_all_PID\`,\`Gene_damage_prediction_PID_AD\`,\`Gene_damage_prediction_PID_AR\`,\`Gene_damage_prediction_all_cancer\`,\`Gene_damage_prediction_cancer_recessive\`,\`Gene_damage_prediction_cancer_dominant\`,\`LoFtool_score\`,\`SORVA_LOF_MAF0.005_HetOrHom\`,\`SORVA_LOF_MAF0.005_HomOrCompoundHet\`,\`SORVA_LOF_MAF0.001_HetOrHom\`,\`SORVA_LOF_MAF0.001_HomOrCompoundHet\`,\`SORVA_LOForMissense_MAF0.005_HetOrHom\`,\`SORVA_LOForMissense_MAF0.005_HomOrCompoundHet\`,\`SORVA_LOForMissense_MAF0.001_HetOrHom\`,\`SORVA_LOForMissense_MAF0.001_HomOrCompoundHet\`,\`Essential_gene\`,\`Essential_gene_CRISPR\`,\`Essential_gene_CRISPR2\`,\`Essential_gene_gene-trap\`,\`Gene_indispensability_score\`,\`Gene_indispensability_pred\`,\`MGI_mouse_gene\`,\`MGI_mouse_phenotype\`,\`ZFIN_zebrafish_gene\`,\`ZFIN_zebrafish_structure\`,\`ZFIN_zebrafish_phenotype_quality\`,\`ZFIN_zebrafish_phenotype_tag\`); SHOW WARNINGS\G; SELECT @@warning_count" > genes.sql.output
cat genes.sql.output
cd ${HIGHLANDERPATH}

echo "installation of '${DBNSFP}' is done!"