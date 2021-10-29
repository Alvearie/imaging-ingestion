#!/bin/bash -e

set -o allexport
source ./dicom-dimse-proxy.env
set +o allexport

echo "Starting dicom-dimse-proxy in background ..."
nohup java -jar ./dicom-dimse-proxy-*.jar 2>&1 &
