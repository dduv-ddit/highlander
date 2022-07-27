#!/bin/bash

# Exit the script if any statement returns a non-true return value.
set -e

### Variables

if [ "$DB" == "" ]
then
	DB=${1}
fi
if [ "$REFNAME" == "" ]
then
	REFNAME=${2}
fi
if [ "$SAMPLE" == "" ]
then
	SAMPLE=${3}
fi
if [ "$CHR" == "" ]
then
	CHR=${4}
fi
if [ "$POS" == "" ]
then
	POS=${5}
fi
if [ "$NORMAL" == "" ]
then
	NORMAL=${6}
fi

### Functions 

### Returns 0 if the specified string contains the specified substring, otherwise returns 1.
function contains() {
    string="$1"
    substring="$2"
    if test "${string#*$substring}" != "$string"
    then
        return 0    # $substring is in $string
    else
        return 1    # $substring is not in $string
    fi
}

HIGHLANDER=/data/highlander

## Reference
if [ "$REFNAME" == "GRCh38" ]
then
  REFPATH=$HIGHLANDER/reference/GRCh38/
  REFFASTA=$REFPATH/GCA_000001405.15_GRCh38_no_alt_plus_hs38d1_analysis_set.fna
  ANNOTSVGENOME=GRCh38
elif [ "$REFNAME" == "b37" ]
then
  REFPATH=$HIGHLANDER/reference/bundle_gatk_2_8/b37/
  REFFASTA=$REFPATH/human_g1k_v37.fasta
  ANNOTSVGENOME=GRCh37
elif [ "$REFNAME" == "b37_decoy" ]
then
  REFPATH=$HIGHLANDER/reference/b37/
  REFFASTA=$REFPATH/human_g1k_v37_decoy.fasta
  ANNOTSVGENOME=GRCh37
elif [ "$REFNAME" == "hg19" ]
then
  REFPATH=$HIGHLANDER/reference/bundle_gatk_2_8/hg19/
  REFFASTA=$REFPATH/ucsc.hg19.fasta
  ANNOTSVGENOME=GRCh37
elif [ "$REFNAME" == "hg19_lifescope" ]
then
  REFPATH=$HIGHLANDER/reference/hg19_lifescope/
  REFFASTA=$REFPATH/human_hg19.fa
  ANNOTSVGENOME=GRCh37
elif [ "$REFNAME" == "GRCm38" ]
then
  REFPATH=$HIGHLANDER/reference/GRCm38/
  REFFASTA=$REFPATH/Mus_musculus_GRCm38_97.fa
  ANNOTSVGENOME=mm10
elif [ "$REFNAME" == "xeno" ]
then
  REFPATH=$HIGHLANDER/reference/xenograft/
  REFFASTA=$REFPATH/xenograft_b37_GRCm38.fasta
  ANNOTSVGENOME=GRCh37
fi

BAMPATH=/data/highlander/bam

BAMOUTTEMP=$HIGHLANDER/work/${SAMPLE}_${CHR}_${POS}.bam
BAIOUTTEMP=$HIGHLANDER/work/${SAMPLE}_${CHR}_${POS}.bai
BAMOUTFINAL=${BAMPATH}/bamout/${SAMPLE}_${CHR}_${POS}.bam
BAIOUTFINAL=${BAMPATH}/bamout/${SAMPLE}_${CHR}_${POS}.bai

VCFTEMP=$HIGHLANDER/work/${SAMPLE}_${CHR}_${POS}.vcf
VCFIDXTEMP=$HIGHLANDER/work/${SAMPLE}_${CHR}_${POS}.vcf.idx

umask 022

if contains ${DB} "somatic"
then
    HCDB=${DB/_somatic_/_}
    ## GATK known variants files
    KNOWNSITESPATH=$HIGHLANDER/reference/bundle_gatk_4/Mutect2
    if [ "$ANNOTSVGENOME" == "GRCh38" ]
    then
        GNOMAD=$KNOWNSITESPATH/af-only-gnomad.hg38.vcf.gz
    elif [ "$ANNOTSVGENOME" == "GRCh37" ]
    then
        GNOMAD=$KNOWNSITESPATH/af-only-gnomad.raw.sites.b37.vcf.gz
    else
        echo "Gnomad raw sites file missing for genome $ANNOTSVGENOME"
        exit 1
    fi
    /opt/GATK/gatk --java-options "-Xmx500m -XX:+AggressiveOpts" Mutect2 -R $REFFASTA -I ${BAMPATH}/${HCDB}/${SAMPLE}.bam -I ${BAMPATH}/${HCDB}/${NORMAL}.bam -normal ${NORMAL} -L ${CHR}:${POS} --germline-resource $GNOMAD -OVI true -bamout ${BAMOUTTEMP} -ip 200 -O $VCFTEMP
else
    /opt/GATK/gatk --java-options "-Xmx500m -XX:+AggressiveOpts" HaplotypeCaller -R $REFFASTA -I ${BAMPATH}/${DB}/${SAMPLE}.bam -L ${CHR}:${POS} -OVI true -bamout ${BAMOUTTEMP} -ip 200 -O $VCFTEMP
fi
rm $VCFTEMP;
rm $VCFIDXTEMP;
rm -f $VCFTEMP.stats;

echo "GATK done, moving files"
mv -f ${BAMOUTTEMP} ${BAMOUTFINAL}
mv -f ${BAIOUTTEMP} ${BAIOUTFINAL}
chmod 664 ${BAMOUTFINAL} || true
chmod 664 ${BAIOUTFINAL} || true
echo "Job done"

