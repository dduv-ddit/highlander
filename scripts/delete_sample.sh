#!/bin/bash


###########################################
### Delete a sample in Highlander files ###
###########################################
#
# Parameters : 
# $1 = run path (like /data/highlander/results/182_2015_05_18_LELM/hg19_lifescope)
# $2 = sample id
# $3,... = analyses (like panels_haplotype_caller panels_torrent_caller)

path=$1
shift
sample=$1
shift

# delete project dir
rm -rf ${path}/${sample}

while [ ! -z "$1" ];
do
	analysis=$1
	shift
	# delete bam symlink
	rm /data/highlander/bam/$analysis/$sample.*bam
	rm /data/highlander/bam/$analysis/$sample.*bai
	# delete vcf symlink
	rm /data/highlander/vcf/$analysis/$sample.*vcf
	rm /data/highlander/vcf/$analysis/$sample.*vcf.idx
done
