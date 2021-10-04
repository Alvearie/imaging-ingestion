IMAGE_NAME=alvearie/imaging-ingestion-toolbox
VERSION=0.0.1

fmt:
	go fmt ./...

vet:
	go vet ./...

tidy:
	go mod tidy

docker-build: fmt vet tidy
	docker build -t $(IMAGE_NAME):$(VERSION) .

docker-push:
	docker push $(IMAGE_NAME):$(VERSION)
