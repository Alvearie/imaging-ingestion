#!/bin/bash -e

export DIMSE_CONFIG_PATH=dimse-config
export DIMSE_PROXY_PORT=<PROXY_PORT>
export DIMSE_CALLED_AET=<TARGET_AET>
export DIMSE_NATS_URL=<NATS_HOST>:<NATS_PORT>
export DIMSE_NATS_TLS_ENABLED=<true|false>
export DIMSE_CALLED_HOST=<TARGET_ARCHIVE_HOST>
export DIMSE_CALLED_PORT=<TARGET_ARCHIVE_PORT>
export DIMSE_NATS_SUBJECT_ROOT=<NATS_SUBJECT_ROOT>
export DIMSE_PROXY_ACTOR=PROXY

echo "Starting dicom-dimse-proxy in background ..."
nohup ./dicom-dimse-proxy-*-linux-x86_64 2>&1 &
