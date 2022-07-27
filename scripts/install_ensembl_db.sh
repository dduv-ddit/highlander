#!/bin/bash

#######################################################################
### Download and install Ensembl databases necessary for Highlander ###
#######################################################################
#
# Parameters : 
# $1 = highlander database username
# $2 = highlander database password
# $3 = species (ex: homo_sapiens, mus_musculus)
# $4 = Ensembl release (ex: 100)
# $5 = GRC version of the genome (ex : 37, 38)
#

### Parameters

DBUSER=$1
DBPASS=$2
SPECIES=$3
ENSEMBL=$4
GRC=$5

### Variables

HIGHLANDERPATH=/data/highlander

VERSION=${ENSEMBL}_${GRC}
FTP=ftp://ftp.ensembl.org/pub/release-$ENSEMBL/mysql/${SPECIES}_core_${VERSION}


### Script

cd ${HIGHLANDERPATH}
mkdir -p temp
mkdir -p temp/release_$ENSEMBL
mkdir -p temp/release_$ENSEMBL/${SPECIES}_${VERSION}

echo "downloading 'release_$ENSEMBL' from Ensembl"
cd temp
cd release_$ENSEMBL
cd ${SPECIES}_${VERSION}
wget ${FTP}/${SPECIES}_core_${VERSION}.sql.gz
wget ${FTP}/assembly.txt.gz
wget ${FTP}/assembly_exception.txt.gz
wget ${FTP}/coord_system.txt.gz
wget ${FTP}/dna.txt.gz
wget ${FTP}/exon.txt.gz
wget ${FTP}/exon_transcript.txt.gz
wget ${FTP}/external_db.txt.gz
wget ${FTP}/external_synonym.txt.gz
wget ${FTP}/gene.txt.gz
wget ${FTP}/object_xref.txt.gz
wget ${FTP}/seq_region.txt.gz
wget ${FTP}/transcript.txt.gz
wget ${FTP}/translation.txt.gz
wget ${FTP}/xref.txt.gz
cd ..

echo "extracting 'release_$ENSEMBL' from archives"
cd ${SPECIES}_${VERSION}
gzip -d ${SPECIES}_core_${VERSION}.sql.gz
gzip -d *.txt.gz
cd ..

echo "creating databases"

mysql -u${DBUSER} -p${DBPASS} -e"create database ${SPECIES}_core_${VERSION};"

echo "Populating databases"

cd ${SPECIES}_${VERSION}
mysql -u${DBUSER} -p${DBPASS} ${SPECIES}_core_${VERSION} < ${SPECIES}_core_${VERSION}.sql
mysqlimport -u${DBUSER} -p${DBPASS} ${SPECIES}_core_${VERSION} -L *.txt
cd ..
cd ..

function delempty()
{
    db=$1
    for f in $(mysql -u${DBUSER} -p${DBPASS} -e "use $db;SHOW TABLES;" | sed 's/^| \([^ ]*\).*$/\1/g'|sed 1d)
    do 
        c=`mysql -u${DBUSER} -p${DBPASS} -e "use $db;SELECT COUNT(*) FROM ${f}\G"|sed '/:/!d; s/^[^:]*: //g'`
        [[ $c == 0 ]] && { echo "DROP db.$f" && mysql -u${DBUSER} -p${DBPASS} -e "use $db;DROP TABLE $f"; }
    done
}

echo "Removing empty database"
delempty ${SPECIES}_core_${VERSION}

echo "installation of '${SPECIES}_${VERSION}' is done!"
