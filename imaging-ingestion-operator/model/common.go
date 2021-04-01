/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package model

import (
	v1 "k8s.io/api/core/v1"
	logf "sigs.k8s.io/controller-runtime/pkg/log"
)

var log = logf.Log.WithName("model")

func GetImage(specImage, defaultImage string) string {
	if specImage != "" {
		return specImage
	}

	return defaultImage
}

// MergeEnvs favors values in b
func MergeEnvs(a []v1.EnvVar, b []v1.EnvVar) []v1.EnvVar {
	for _, bb := range b {
		found := false
		for i, aa := range a {
			if aa.Name == bb.Name {
				aa.Value = bb.Value
				a[i] = aa
				found = true
				break
			}
		}
		if !found {
			a = append(a, bb)
		}
	}
	return a
}

func GetDefaultCuids() string {
	return `
ComputedRadiographyImageStorage:1.2.840.10008.5.1.4.1.1.1
CTImageStorage:1.2.840.10008.5.1.4.1.1.2
`
}

func GetDefaultTsuids() string {
	return `
ImplicitVRLittleEndian:1.2.840.10008.1.2
ExplicitVRLittleEndian:1.2.840.10008.1.2.1
JPEGBaseline8Bit:1.2.840.10008.1.2.4.50
JPEGExtended12Bit:1.2.840.10008.1.2.4.51
JPEGLosslessSV1:1.2.840.10008.1.2.4.70
JPEGLossless:1.2.840.10008.1.2.4.57
JPEGLSLossless:1.2.840.10008.1.2.4.80
JPEGLSNearLossless:1.2.840.10008.1.2.4.81
JPEG2000Lossless:1.2.840.10008.1.2.4.90
JPEG2000:1.2.840.10008.1.2.4.91
RLELossless:1.2.840.10008.1.2.5
`
}
