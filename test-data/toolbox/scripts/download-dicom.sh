#!/bin/bash -ex

DIR=$(dirname $0)
DCM4CHEE_VERSION=5.24.2
DCM4CHEE_DOWNLOAD_URL=https://sourceforge.net/projects/dcm4che/files/dcm4che3/$DCM4CHEE_VERSION/dcm4che-$DCM4CHEE_VERSION-bin.zip/download
WADO_ENDPOINT=https://server.dcmjs.org/dcm4chee-arc/aets/DCM4CHEE/rs/studies
TEST_DATA_DIR=test-data/dcmjs

echo Downloading dcm4che
curl -vL $DCM4CHEE_DOWNLOAD_URL -o dcm4che-$DCM4CHEE_VERSION-bin.zip
unzip dcm4che-$DCM4CHEE_VERSION-bin.zip

for s in $(cat $DIR/studies.txt)
do
    mkdir -p $TEST_DATA_DIR/$s
    echo Retrieving study $s
    dcm4che-$DCM4CHEE_VERSION/bin/wadors --out-dir $TEST_DATA_DIR/$s $WADO_ENDPOINT/$s
done
