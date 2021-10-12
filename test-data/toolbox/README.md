# toobox

A project to build a docker image with binaries and test data for testing `imaging-ingestion`

# Build Docker Image

```
make docker-build
```

# Push Docker Image

```
make docker-push
```

# Deploy

```
kubectl create ns {namespace}
# Edit image reference in deploy/manifests.yaml
kubectl -n {namespace} apply -f deploy/manifests.yaml
```

# Ingest Data

```
kubectl -n {namespace} get po
kubectl -n {namespace} exec -it toolbox-xxx -- bash
toolbox stow --image-path test-data/dcmjs/{study_id} --endpoint {stow_endpoint} --concurrency 10
```

# `toolbox stow` Usage

```
Ingest DICOM Images using stow-rs

Usage:
  toolbox stow [flags]

Flags:
      --concurrency int     Ingest Concurrency (default 1)
      --endpoint string     Ingest Endpoint
  -h, --help                help for stow
      --image-path string   DICOM Image Path of File or Folder
      --token string        Authorization token (ex: Bearer xxxx)
```

# Pull DICOM test data from different source

This project pulls studies from a publically available DICOM archive. For adding a different source,

* Update `WADO_ENDPOINT` and `TEST_DATA_DIR` in `scripts/download-dicom.sh`
* Update Study IDs in `scripts/studies.txt`
