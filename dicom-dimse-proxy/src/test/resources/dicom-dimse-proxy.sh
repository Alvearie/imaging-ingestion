#!/bin/bash -e

set -o allexport
source ./dicom-dimse-proxy.env
set +o allexport

echo "Starting dicom-dimse-proxy in background ..."
nohup ./dicom-dimse-proxy-*-linux-x86_64 2>&1 &
