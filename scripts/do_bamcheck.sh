#!/bin/bash

# Exit the script if any statement returns a non-true return value.
set -e

### Variables

if [ "$FILENAME" == "" ]
then
	FILENAME=${1}
fi
if [ "$PATIENTS" == "" ]
then
	PATIENTS=${2}
fi
if [ "$POS" == "" ]
then
	POS=${3}
fi

HIGHLANDER=/data/highlander
BAMPATH=${HIGHLANDER}/bam

VIEWBAMPATH=${HIGHLANDER}/viewBam.jar

BAMOUTTEMP=$HIGHLANDER/work/${FILENAME}
BAMOUTFINAL=${BAMPATH}/bamout/${FILENAME}

umask 022
java -Xmx4g -Dhttp.proxyHost=192.168.200.254 -Dhttp.proxyPort=8000 -jar ${VIEWBAMPATH} -c ${HIGHLANDER}/config/settings.xml -T bamcheck -I ${BAMPATH} -O ${BAMOUTTEMP} -S "${PATIENTS}" -i "${POS}" 2>&1 >/dev/null
echo "BAMcheck done, moving files"
mv -f ${BAMOUTTEMP} ${BAMOUTFINAL}
chmod 664 ${BAMOUTFINAL} || true
echo "Job done"

