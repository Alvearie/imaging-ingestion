module github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator

go 1.15

require (
	github.com/go-logr/logr v0.3.0
	github.com/onsi/ginkgo v1.14.1
	github.com/onsi/gomega v1.10.2
	github.com/pkg/errors v0.9.1
	k8s.io/api v0.19.2
	k8s.io/apimachinery v0.19.2
	k8s.io/client-go v11.0.1-0.20190805182717-6502b5e7b1b5+incompatible
	knative.dev/eventing v0.16.0
	knative.dev/pkg v0.0.0-20200702222342-ea4d6e985ba0
	knative.dev/serving v0.16.0
	sigs.k8s.io/controller-runtime v0.7.2
)

replace (
	github.com/Azure/go-autorest => github.com/Azure/go-autorest v12.2.0+incompatible
	k8s.io/api => k8s.io/api v0.19.2
	k8s.io/apiextensions-apiserver => k8s.io/apiextensions-apiserver v0.19.2
	k8s.io/apimachinery => k8s.io/apimachinery v0.19.2
	k8s.io/apiserver => k8s.io/apiserver v0.19.2
	k8s.io/client-go => k8s.io/client-go v0.19.2
	knative.dev/pkg => knative.dev/pkg v0.0.0-20210114223020-f0ea5e6b9c4e
)
