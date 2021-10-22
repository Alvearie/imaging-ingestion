/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package model

import (
	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/api/v1alpha1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	ksourcesv1 "knative.dev/eventing/pkg/apis/sources/v1"
	duckv1 "knative.dev/pkg/apis/duck/v1"
	v1 "knative.dev/pkg/apis/duck/v1"
	"knative.dev/pkg/tracker"
	"sigs.k8s.io/controller-runtime/pkg/client"
)

func StudyBindingSinkBinding(cr *v1alpha1.DicomStudyBinding, source, sink string) *ksourcesv1.SinkBinding {
	return &ksourcesv1.SinkBinding{
		ObjectMeta: metav1.ObjectMeta{
			Name:      GetStudyBindingSinkBindingName(cr),
			Namespace: cr.Namespace,
		},

		Spec: ksourcesv1.SinkBindingSpec{
			SourceSpec: v1.SourceSpec{
				Sink: v1.Destination{
					Ref: &v1.KReference{
						APIVersion: "eventing.knative.dev/v1",
						Kind:       "Broker",
						Name:       sink,
					},
				},
			},
			BindingSpec: duckv1.BindingSpec{
				Subject: tracker.Reference{
					APIVersion: "serving.knative.dev/v1",
					Kind:       "Service",
					Name:       source,
				},
			},
		},
	}
}

func StudyBindingSinkBindingSelector(cr *v1alpha1.DicomStudyBinding) client.ObjectKey {
	return client.ObjectKey{
		Name:      GetStudyBindingSinkBindingName(cr),
		Namespace: cr.Namespace,
	}
}

func StudyBindingSinkBindingReconciled(cr *v1alpha1.DicomStudyBinding, currentState *ksourcesv1.SinkBinding) *ksourcesv1.SinkBinding {
	reconciled := currentState.DeepCopy()

	return reconciled
}

func GetStudyBindingSinkBindingName(cr *v1alpha1.DicomStudyBinding) string {
	return cr.Name + "-study-sink-binding"
}
