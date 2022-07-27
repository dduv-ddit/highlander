#!/bin/bash

#########################################################################
### Download and install Cosmic databases used for variant annotation ###
#########################################################################
#
# Parameters : 
# $1 = highlander database username
# $2 = highlander database password
# $3 = GRC version of the genome (ex : 37, 38)
#

### Parameters

DBUSER=$1
DBPASS=$2
GRC=$3

### Variables

HIGHLANDERPATH=/data/highlander

VERSION=92
CREDENTIALS=$(echo "raphael.helaers@uclouvain.be:aZeq6GYBL@y2%ag" | base64)

function createCosmicMutantExport(){
TABLENAME=$1;
echo "DROP TABLE IF EXISTS ${TABLENAME};" > $TABLENAME.sql
echo "CREATE TABLE ${TABLENAME} (" >> $TABLENAME.sql
echo '`gene_name` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`accession_number` VARCHAR(25) DEFAULT NULL,' >> $TABLENAME.sql
echo '`gene_cds_length` INT DEFAULT NULL,' >> $TABLENAME.sql
echo '`hgnc_id` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`sample_name` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`sample_id` INT DEFAULT NULL,' >> $TABLENAME.sql
echo '`id_tumour` INT DEFAULT NULL,' >> $TABLENAME.sql
echo '`primary_site` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`site_subtype_1` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`site_subtype_2` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`site_subtype_3` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`primary_histology` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`histology_subtype_1` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`histology_subtype_2` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`histology_subtype_3` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`genome` VARCHAR(1) DEFAULT NULL,' >> $TABLENAME.sql
echo '`genomic_mutation_id` VARCHAR(25) DEFAULT NULL,' >> $TABLENAME.sql
echo '`legacy_mutation_id` VARCHAR(25) DEFAULT NULL,' >> $TABLENAME.sql
echo '`mutation_id` INT DEFAULT NULL,' >> $TABLENAME.sql
echo '`mutation_cds` VARCHAR(500) DEFAULT NULL,' >> $TABLENAME.sql
echo '`mutation_aa` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`mutation_description` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`mutation_zygosity` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`loh` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`grch` VARCHAR(2) DEFAULT NULL,' >> $TABLENAME.sql
echo '`mutation_genome_position` VARCHAR(30) DEFAULT NULL,' >> $TABLENAME.sql
echo '`mutation_strand` VARCHAR(1) DEFAULT NULL,' >> $TABLENAME.sql
echo '`snp` VARCHAR(1) DEFAULT NULL,' >> $TABLENAME.sql
echo '`resistance_mutation` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`fathmm_prediction` VARCHAR(12) DEFAULT NULL,' >> $TABLENAME.sql
echo '`fathmm_score` VARCHAR(15) DEFAULT NULL,' >> $TABLENAME.sql
echo '`mutation_somatic_status` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`pubmed_pmid` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`id_study` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`sample_type` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`tumour_origin` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`age` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`hgvsp` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`hgvsc` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`hgvsg` TEXT DEFAULT NULL,' >> $TABLENAME.sql
echo '`id` int(10) unsigned NOT NULL AUTO_INCREMENT,' >> $TABLENAME.sql
echo 'PRIMARY KEY  (`id`),' >> $TABLENAME.sql
echo 'INDEX `variant` (`mutation_genome_position`)', >> $TABLENAME.sql
echo 'INDEX `mutation_cds` (`mutation_cds`)', >> $TABLENAME.sql
echo 'INDEX `cosv` (`genomic_mutation_id`)' >> $TABLENAME.sql
echo ') ENGINE=MyISAM DEFAULT CHARSET=latin1;' >> $TABLENAME.sql
}

### Script

cd ${HIGHLANDERPATH}
mkdir -p temp
mkdir -p temp/cosmic_$VERSION
mkdir -p temp/cosmic_$VERSION/${GRC}

echo "downloading 'cosmic_$VERSION' from Ensembl"

# https://cancer.sanger.ac.uk/cosmic/download

# CosmicMutantExport.tsv.gz
#   COSMIC Mutation Data
#   A tab separated table of all COSMIC coding point mutations from targeted and genome wide screens from the current release.

# CosmicNCV.tsv.gz
#   Non coding variants
#   A tab separated table of all non-coding mutations from the current release.

# CosmicBreakpointsExport.tsv.gz
#   Structural Genomic Rearrangements
#   All breakpoint data from the current release in a tab separated table.

# CosmicFusionExport.tsv.gz
#   Complete Fusion Export
#   All gene fusion mutation data from the current release in a tab separated table.

# CosmicCompleteCNA.tsv.gz
#   Copy Number Variants
#   All copy number abberations from the current release in a tab separated table. For more information on copy number data, please see http://cancer.sanger.ac.uk/cosmic/help/cnv/overview.

cd temp/cosmic_${VERSION}/${GRC}
for f in CosmicMutantExport.tsv.gz CosmicNCV.tsv.gz CosmicBreakpointsExport.tsv.gz CosmicFusionExport.tsv.gz CosmicCompleteCNA.tsv.gz
do
    curl -H "Authorization: Basic ${CREDENTIALS}" https://cancer.sanger.ac.uk/cosmic/file_download${GRC}/cosmic/v${VERSION}/${f} | python3 -c "import sys, json; print(json.load(sys.stdin['url'])" > url        
    url=$(cat url)
    wget -c -O ${f} ${url}
done

echo "extracting 'cosmic_$VERSION' from archives"
for f in CosmicMutantExport.tsv.gz CosmicNCV.tsv.gz CosmicBreakpointsExport.tsv.gz CosmicFusionExport.tsv.gz CosmicCompleteCNA.tsv.gz
do
    gzip -d ${f}
done

echo "Creating database"
mysql -u${DBUSER} -p${DBPASS} -e"CREATE DATABASE IF NOT EXISTS cosmic_${VERSION}_${GRC};"
createCosmicMutantExport mutant_export
mysql -u${DBUSER} -p${DBPASS} cosmic_${VERSION}_${GRC} < mutant_export.sql

echo "Populating databases"
mysql -u${DBUSER} -p${DBPASS} --execute="LOAD DATA LOCAL INFILE '${HIGHLANDERPATH}/temp/cosmic_${VERSION}/${GRC}/CosmicMutantExport.tsv' INTO TABLE cosmic_${VERSION}_${GRC}.mutant_export IGNORE 1 LINES (\`gene_name\`,\`accession_number\`,\`gene_cds_length\`,\`hgnc_id\`,\`sample_name\`,\`sample_id\`,\`id_tumour\`,\`primary_site\`,\`site_subtype_1\`,\`site_subtype_2\`,\`site_subtype_3\`,\`primary_histology\`,\`histology_subtype_1\`,\`histology_subtype_2\`,\`histology_subtype_3\`,\`genome\`,\`genomic_mutation_id\`,\`legacy_mutation_id\`,\`mutation_id\`,\`mutation_cds\`,\`mutation_aa\`,\`mutation_description\`,\`mutation_zygosity\`,\`loh\`,\`grch\`,\`mutation_genome_position\`,\`mutation_strand\`,\`snp\`,\`resistance_mutation\`,\`fathmm_prediction\`,\`fathmm_score\`,\`mutation_somatic_status\`,\`pubmed_pmid\`,\`id_study\`,\`sample_type\`,\`tumour_origin\`,\`age\`,\`hgvsp\`,\`hgvsc\`,\`hgvsg\`); SHOW WARNINGS\G; SELECT @@warning_count" > mutant_export.sql.output
cat mutant_export.sql.output

echo "installation of 'cosmic_${VERSION}_${GRC}' is done!"