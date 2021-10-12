# instance-binding-go

Sample project to consume `DicomAvailableEvent`. This sample image will read dicom image from WADO-RS endpoint and print dicom tags.

## Build

```
make docker-build
make docker-push
```

## Test Locally

```
# In one terminal window

go run main.go

# In another terminal window

curl -XPOST -v "http://localhost:8080" \
-H "Ce-Id: 1234" \
-H "Ce-Specversion: 1.0" \
-H "Ce-Type: DicomAvailableEvent" \
-H "Ce-Source: curl" \
-H "Content-Type: application/json" \
-d '"https://ingestion-wado-imaging-ingestion.<domain>/wado-rs/studies/<study_id>/series/<series_id>/instances/<instance_id>"'
```
