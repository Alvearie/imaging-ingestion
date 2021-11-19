/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package model_test

import (
	"testing"

	"github.com/Alvearie/imaging-ingestion/imaging-ingestion-operator/model"
	"github.com/stretchr/testify/require"
	corev1 "k8s.io/api/core/v1"
)

// TestMergeEnvsEmptyEmpty calls model.MergeEnvs with a empty slices, checking
// for a returned empty slice
func TestMergeEnvsEmptyEmpty(t *testing.T) {
	assert := require.New(t)

	a := []corev1.EnvVar{}
	b := []corev1.EnvVar{}

	merged := model.MergeEnvs(a, b)

	assert.Empty(merged)
}

// TestMergeEnvsEmptyNonEmpty calls model.MergeEnvs with an empty slice for A, and non-empty slice for B,
// checking for a returned slice with the same values of B
func TestMergeEnvsEmptyNonEmpty(t *testing.T) {
	assert := require.New(t)

	a := []corev1.EnvVar{}
	b := []corev1.EnvVar{{Name: "key1", Value: "v1"}} // 1 entry only

	merged := model.MergeEnvs(a, b)

	assert.Equal(b, merged)

	// We append another value to b and merge again
	b = append(b, corev1.EnvVar{Name: "key2", Value: "v2"})

	merged = model.MergeEnvs(a, b)

	assert.Equal(b, merged)
	assert.Len(merged, 2)
}

// TestMergeEnvsNonEmptyEmpty calls model.MergeEnvs with a non-empty slice for A, and empty slice for B,
// checking for a returned slice with the same values of A
func TestMergeEnvsNonEmptyEmpty(t *testing.T) {
	assert := require.New(t)

	a := []corev1.EnvVar{{Name: "key1", Value: "v1"}} // 1 entry only
	b := []corev1.EnvVar{}

	merged := model.MergeEnvs(a, b)

	assert.Equal(a, merged)

	// We append another value to a and merge again
	a = append(a, corev1.EnvVar{Name: "key2", Value: "v2"})

	merged = model.MergeEnvs(a, b)

	assert.Equal(a, merged)
	assert.Len(merged, 2)
}

// TestMergeEnvsNoOverlap calls model.MergeEnvs with a non-empty slice for A, and non-empty slice for B,
// There are not duplicate keys between A and B entries
// Checking for a returned slice with the values of both A and B
func TestMergeEnvsNoOverlap(t *testing.T) {
	assert := require.New(t)

	a := []corev1.EnvVar{{Name: "A_KEY_1", Value: "value A 1"}} // 1 entry only
	b := []corev1.EnvVar{{Name: "B_KEY_1", Value: "value B 1"}} // 1 entry only

	merged := model.MergeEnvs(a, b)

	assert.Len(merged, 2)
	assert.Contains(merged, a[0], b[0])

	// We append another value to A and merge again
	a = append(a, corev1.EnvVar{Name: "A_KEY_2", Value: "value A 2"})

	merged = model.MergeEnvs(a, b)

	assert.Len(merged, 3)
	assert.Contains(merged, a[0], a[1], b[0])

	// We append another value to B and merge again
	b = append(b, corev1.EnvVar{Name: "B_KEY_2", Value: "value B 2"})

	merged = model.MergeEnvs(a, b)

	assert.Len(merged, 4)
	assert.Contains(merged, a[0], a[1], b[0], b[1])
}

// TestMergeEnvsOverlap calls model.MergeEnvs with a non-empty slice for A, and non-empty slice for B,
// There are duplicate keys between A and B entries
// Checking for a returned slice with the values of both A and B, where B values take precedence when same key
func TestMergeEnvsOverlap(t *testing.T) {
	assert := require.New(t)

	commonKey := "SAME_KEY_1"
	aValue := "value A"
	bValue := "value B"

	a := []corev1.EnvVar{{Name: commonKey, Value: aValue}} // 1 entry only
	b := []corev1.EnvVar{{Name: commonKey, Value: bValue}} // 1 entry only

	merged := model.MergeEnvs(a, b)

	assert.Len(merged, 1)
	assert.Contains(merged, b[0]) // should contain only the value from B

	// We append another value to A and merge again
	newA := corev1.EnvVar{Name: "A_KEY_2", Value: "value A 2"}
	a = append(a, newA)

	merged = model.MergeEnvs(a, b)

	assert.Len(merged, 2)
	assert.Contains(merged, newA, b[0])

	// We append another value to B and merge again
	b = append(b, corev1.EnvVar{Name: "B_KEY_2", Value: "value B 2"})

	merged = model.MergeEnvs(a, b)

	assert.Len(merged, 3)
	assert.Contains(merged, newA, b[0], b[1])
}

// TestMergeEnvsWithValueFrom calls model.MergeEnvs with a non-empty slice for A, and non-empty slice for B,
// There are duplicate keys between A and B entries
// Checking for a returned slice with the values of both A and B, where B values take precedence when same key
// The EnvVar used can also contain entries with ValueFrom instead of Value
func TestMergeEnvsWithValueFromInBSlice(t *testing.T) {
	assert := require.New(t)

	commonKey := "SAME_KEY_1"
	aEnvVar1 := corev1.EnvVar{Name: commonKey, Value: "Value A1"}
	bEnvVar1 := corev1.EnvVar{
		Name:      commonKey,
		ValueFrom: getTestEnvVarSource("localObjectRef1", "keySelectorName1"),
	}

	a := []corev1.EnvVar{aEnvVar1} // 1 entry only
	b := []corev1.EnvVar{bEnvVar1} // 1 entry only, with a ValueFrom env var

	merged := model.MergeEnvs(a, b)

	assert.Len(merged, 1)
	assert.Contains(merged, bEnvVar1) // should contain only the value from B

	// We append another value to A and merge again
	aEnvVar2 := corev1.EnvVar{Name: "A_KEY_2", Value: "value A 2"}
	a = append(a, aEnvVar2)

	merged = model.MergeEnvs(a, b)

	assert.Len(merged, 2)
	assert.Contains(merged, aEnvVar2, bEnvVar1)

	// We append another value to B and merge again

	bEnvVar2 := corev1.EnvVar{Name: "B_KEY_2", Value: "value B 2"}
	b = append(b, bEnvVar2)

	merged = model.MergeEnvs(a, b)

	// We should have the three values:
	assert.Len(merged, 3)
	assert.Contains(merged, bEnvVar1, aEnvVar2, bEnvVar2)

	// We append another "ValueFrom" value to A, with the same key used for bEnvVar2 ("B_KEY_2")
	// This new value should not exist after merging
	aEnvVar3 := corev1.EnvVar{
		Name:      "B_KEY_2",
		ValueFrom: getTestEnvVarSource("localObjectRef2", "keySelectorName2"),
	}
	a = append(a, aEnvVar3)

	merged = model.MergeEnvs(a, b)

	assert.Len(merged, 3)
	assert.Contains(merged, bEnvVar1, aEnvVar2, bEnvVar2)
}

// getTestEnvVarSource creates a test EnvVarSource to apply to
func getTestEnvVarSource(lorName string, key string) *corev1.EnvVarSource {
	return &corev1.EnvVarSource{
		SecretKeyRef: &corev1.SecretKeySelector{
			LocalObjectReference: corev1.LocalObjectReference{Name: lorName},
			Key:                  key,
		},
	}
}
