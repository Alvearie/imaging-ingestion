module github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator

go 1.15

require (
	github.com/coderanger/controller-utils v0.0.0-20201221100905-e26c5734ecc9
	github.com/go-logr/logr v0.4.0
	github.com/onsi/ginkgo v1.16.4
	github.com/onsi/gomega v1.14.0
	github.com/pkg/errors v0.9.1
	github.com/stretchr/testify v1.7.0
	k8s.io/api v0.21.4
	k8s.io/apimachinery v0.21.4
	k8s.io/client-go v0.21.4
	knative.dev/eventing v0.23.0
	knative.dev/pkg v0.0.0-20211018141937-a34efd6b409d
	knative.dev/serving v0.23.0
	sigs.k8s.io/controller-runtime v0.9.6
)
