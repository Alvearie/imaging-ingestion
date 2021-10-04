#-------------------------------------------------------------------------------
# (C) Copyright IBM Corp. 2021
#
# SPDX-License-Identifier: Apache-2.0
#-------------------------------------------------------------------------------
# Build the app binary
FROM golang:1.15 as go_builder

WORKDIR /workspace
# Copy the Go Modules manifests
COPY go.mod go.mod
COPY go.sum go.sum
# cache deps before building and copying source so that we don't need to re-download as much
# and so that source changes don't invalidate our downloaded layer
RUN go mod download

# Copy the go source
COPY main.go main.go
COPY cmd cmd

# Build
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 GO111MODULE=on go build -a -o toolbox main.go

# Build test data
FROM registry.access.redhat.com/ubi8/openjdk-11 as java_builder

USER 0
WORKDIR /workspace
COPY scripts scripts

# Download
RUN microdnf -y install unzip
RUN scripts/download-dicom.sh

FROM registry.access.redhat.com/ubi8/ubi-minimal

ARG VERSION=0.0.1

RUN curl https://storage.googleapis.com/hey-release/hey_linux_amd64 -o /bin/hey && chmod +x /bin/hey && \
    curl -L https://github.com/stedolan/jq/releases/download/jq-1.6/jq-linux64 -o /bin/jq && chmod +x /bin/jq && jq --version

COPY --from=go_builder /workspace/toolbox /bin/
COPY --from=java_builder /workspace/test-data test-data

ENTRYPOINT [ "/bin/toolbox", "sleep" ]
