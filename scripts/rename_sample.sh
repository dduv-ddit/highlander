#!/bin/bash

###########################################
### Rename a sample in Highlander files ###
###########################################
#
# Parameters : 
# $1 = run path (like /data/highlander/results/182_2015_05_18_LELM/hg19_lifescope)
# $2 = old sample id
# $3 = new sample id
# $4,... = analyses (like panels_haplotype_caller panels_torrent_caller)

path=$1
shift
old=$1
shift
new=$1
shift

project=$(basename "$(dirname "$path")")

# rename fastq directory
echo "renaming fastq directory"
mv /data/fastq/$project/$old /data/fastq/$project/$new

# rename fastq directory on backup
if [ -d /data/pgen/isilon/fastq/$project/$old ];
then
	echo "renaming backuped fastq directory"
	mv /data/pgen/isilon/fastq/$project/$old /data/pgen/isilon/fastq/$project/$new
else
	echo "sample not yet backuped"
fi

# rename project dir
find ${path}/${old} -execdir rename -v "s/${old}/${new}/" '{}' \+

# rename reports
for report in /data/highlander/reports/$project/*
do 
	mv $report/$old $report/$new
done

# relink symlinks
for i in /data/highlander/reports/$project/*/$new/*
do
    if [ -L "$i" ]
    then
        echo "relink $i"
		a=$(readlink "$i") && ln -sf "$(echo $a | sed "s@$old@$new@g")" "$i"
		rename -v "s/${old}/${new}/" $i
	else
		rename -v "s/${old}/${new}/" $i
    fi
done

# rename BAM/VCF links
while [ ! -z "$1" ];
do
    analysis=$1
    shift

	# relink bam symlink
	for i in /data/highlander/bam/$analysis/$old.*bam /data/highlander/bam/$analysis/$old.*bai
	do	
		echo "relink $i"
		a=$(readlink "$i") && ln -sf "$(echo $a | sed "s@$old@$new@g")" "$i"
		rename "s/${old}/${new}/" $i 
	done
	# relink vcf symlink
	for i in /data/highlander/vcf/$analysis/$old.*vcf /data/highlander/vcf/$analysis/$old.*vcf.idx
	do
		echo "relink $i"
		a=$(readlink "$i") && ln -sf "$(echo $a | sed "s@$old@$new@g")" "$i"
		rename "s/${old}/${new}/" $i
	done

	# update ExomeDepth dataframe
	# too much time for just a rename, maybe just delete the sample ?
	# foireux, il efface pas le lock et le suivant reste en deadlock ad vitam ... en fait quand LOCK.old existe déjà le mv ne renomme pas mais bouge LOCK dans LOCK.old, et au 3e rename il bloque car impossible de bouger LOCK dans LOCK.old qui contient déjà un LOCK ... j'ai ajouté un rm voir si ça corrige le soucis
	# RETIRE DEFINITIVEMENT: à priori l'échantillon restera dans la dataframe avec son ancien nom, les bamcounts avec son ancien nom aussi, donc normalement pas de problème
	# /data/highlander/exomeDepth/scripts/admin/correct_dataframe.sh ${old} ${new} ${analysis} ${path}/${new}
done
