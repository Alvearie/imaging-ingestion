/*
(C) Copyright IBM Corp. 2021

SPDX-License-Identifier: Apache-2.0
*/

package model

import (
	v1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
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
				a[i] = *bb.DeepCopy()
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

func GetResourceQuantity(str string) resource.Quantity {
	v, _ := resource.ParseQuantity(str)
	return v
}

func GetResourceRequirements(memRequest, memLimit string) v1.ResourceRequirements {
	return v1.ResourceRequirements{
		Requests: v1.ResourceList{
			v1.ResourceMemory: GetResourceQuantity(memRequest),
		},
		Limits: v1.ResourceList{
			v1.ResourceMemory: GetResourceQuantity(memLimit),
		},
	}
}

func GetDefaultCuids() string {
	return `
ComputedRadiographyImageStorage:1.2.840.10008.5.1.4.1.1.1
DigitalXRayImageStorageForPresentation:1.2.840.10008.5.1.4.1.1.1.1
DigitalXRayImageStorageForProcessing:1.2.840.10008.5.1.4.1.1.1.1.1
DigitalMammographyXRayImageStorageForPresentation:1.2.840.10008.5.1.4.1.1.1.2
DigitalMammographyXRayImageStorageForProcessing:1.2.840.10008.5.1.4.1.1.1.2.1
DigitalIntraOralXRayImageStorageForPresentation:1.2.840.10008.5.1.4.1.1.1.3
DigitalIntraOralXRayImageStorageForProcessing:1.2.840.10008.5.1.4.1.1.1.3.1
CTImageStorage:1.2.840.10008.5.1.4.1.1.2
EnhancedCTImageStorage:1.2.840.10008.5.1.4.1.1.2.1
LegacyConvertedEnhancedCTImageStorage:1.2.840.10008.5.1.4.1.1.2.2
UltrasoundMultiFrameImageStorageRetired:1.2.840.10008.5.1.4.1.1.3
UltrasoundMultiFrameImageStorage:1.2.840.10008.5.1.4.1.1.3.1
MRImageStorage:1.2.840.10008.5.1.4.1.1.4
EnhancedMRImageStorage:1.2.840.10008.5.1.4.1.1.4.1
MRSpectroscopyStorage:1.2.840.10008.5.1.4.1.1.4.2
EnhancedMRColorImageStorage:1.2.840.10008.5.1.4.1.1.4.3
LegacyConvertedEnhancedMRImageStorage:1.2.840.10008.5.1.4.1.1.4.4
UltrasoundImageStorage:1.2.840.10008.5.1.4.1.1.6.1
EnhancedUSVolumeStorage:1.2.840.10008.5.1.4.1.1.6.2
SecondaryCaptureImageStorage:1.2.840.10008.5.1.4.1.1.7
MultiFrameSingleBitSecondaryCaptureImageStorage:1.2.840.10008.5.1.4.1.1.7.1
MultiFrameGrayscaleByteSecondaryCaptureImageStorage:1.2.840.10008.5.1.4.1.1.7.2
MultiFrameGrayscaleWordSecondaryCaptureImageStorage:1.2.840.10008.5.1.4.1.1.7.3
MultiFrameTrueColorSecondaryCaptureImageStorage:1.2.840.10008.5.1.4.1.1.7.4
TwelveLeadECGWaveformStorage:1.2.840.10008.5.1.4.1.1.9.1.1
GeneralECGWaveformStorage:1.2.840.10008.5.1.4.1.1.9.1.2
AmbulatoryECGWaveformStorage:1.2.840.10008.5.1.4.1.1.9.1.3
HemodynamicWaveformStorage:1.2.840.10008.5.1.4.1.1.9.2.1
CardiacElectrophysiologyWaveformStorage:1.2.840.10008.5.1.4.1.1.9.3.1
BasicVoiceAudioWaveformStorage:1.2.840.10008.5.1.4.1.1.9.4.1
GeneralAudioWaveformStorage:1.2.840.10008.5.1.4.1.1.9.4.2
ArterialPulseWaveformStorage:1.2.840.10008.5.1.4.1.1.9.5.1
RespiratoryWaveformStorage:1.2.840.10008.5.1.4.1.1.9.6.1
MultichannelRespiratoryWaveformStorage:1.2.840.10008.5.1.4.1.1.9.6.2
RoutineScalpElectroencephalogramWaveformStorage:1.2.840.10008.5.1.4.1.1.9.7.1
ElectromyogramWaveformStorage:1.2.840.10008.5.1.4.1.1.9.7.2
ElectrooculogramWaveformStorage:1.2.840.10008.5.1.4.1.1.9.7.3
SleepElectroencephalogramWaveformStorage:1.2.840.10008.5.1.4.1.1.9.7.4
BodyPositionWaveformStorage:1.2.840.10008.5.1.4.1.1.9.8.1
GrayscaleSoftcopyPresentationStateStorage:1.2.840.10008.5.1.4.1.1.11.1
ColorSoftcopyPresentationStateStorage:1.2.840.10008.5.1.4.1.1.11.2
PseudoColorSoftcopyPresentationStateStorage:1.2.840.10008.5.1.4.1.1.11.3
BlendingSoftcopyPresentationStateStorage:1.2.840.10008.5.1.4.1.1.11.4
XAXRFGrayscaleSoftcopyPresentationStateStorage:1.2.840.10008.5.1.4.1.1.11.5
GrayscalePlanarMPRVolumetricPresentationStateStorage:1.2.840.10008.5.1.4.1.1.11.6
CompositingPlanarMPRVolumetricPresentationStateStorage:1.2.840.10008.5.1.4.1.1.11.7
AdvancedBlendingPresentationStateStorage:1.2.840.10008.5.1.4.1.1.11.8
VolumeRenderingVolumetricPresentationStateStorage:1.2.840.10008.5.1.4.1.1.11.9
SegmentedVolumeRenderingVolumetricPresentationStateStorage:1.2.840.10008.5.1.4.1.1.11.10
MultipleVolumeRenderingVolumetricPresentationStateStorage:1.2.840.10008.5.1.4.1.1.11.11
XRayAngiographicImageStorage:1.2.840.10008.5.1.4.1.1.12.1
EnhancedXAImageStorage:1.2.840.10008.5.1.4.1.1.12.1.1
XRayRadiofluoroscopicImageStorage:1.2.840.10008.5.1.4.1.1.12.2
EnhancedXRFImageStorage:1.2.840.10008.5.1.4.1.1.12.2.1
XRay3DAngiographicImageStorage:1.2.840.10008.5.1.4.1.1.13.1.1
XRay3DCraniofacialImageStorage:1.2.840.10008.5.1.4.1.1.13.1.2
BreastTomosynthesisImageStorage:1.2.840.10008.5.1.4.1.1.13.1.3
BreastProjectionXRayImageStorageForPresentation:1.2.840.10008.5.1.4.1.1.13.1.4
BreastProjectionXRayImageStorageForProcessing:1.2.840.10008.5.1.4.1.1.13.1.5
IntravascularOpticalCoherenceTomographyImageStorageForPresentation:1.2.840.10008.5.1.4.1.1.14.1
IntravascularOpticalCoherenceTomographyImageStorageForProcessing:1.2.840.10008.5.1.4.1.1.14.2
NuclearMedicineImageStorage:1.2.840.10008.5.1.4.1.1.20
ParametricMapStorage:1.2.840.10008.5.1.4.1.1.30
RawDataStorage:1.2.840.10008.5.1.4.1.1.66
SpatialRegistrationStorage:1.2.840.10008.5.1.4.1.1.66.1
SpatialFiducialsStorage:1.2.840.10008.5.1.4.1.1.66.2
DeformableSpatialRegistrationStorage:1.2.840.10008.5.1.4.1.1.66.3
SegmentationStorage:1.2.840.10008.5.1.4.1.1.66.4
SurfaceSegmentationStorage:1.2.840.10008.5.1.4.1.1.66.5
TractographyResultsStorage:1.2.840.10008.5.1.4.1.1.66.6
RealWorldValueMappingStorage:1.2.840.10008.5.1.4.1.1.67
SurfaceScanMeshStorage:1.2.840.10008.5.1.4.1.1.68.1
SurfaceScanPointCloudStorage:1.2.840.10008.5.1.4.1.1.68.2
VLEndoscopicImageStorage:1.2.840.10008.5.1.4.1.1.77.1.1
VideoEndoscopicImageStorage:1.2.840.10008.5.1.4.1.1.77.1.1.1
VLMicroscopicImageStorage:1.2.840.10008.5.1.4.1.1.77.1.2
VideoMicroscopicImageStorage:1.2.840.10008.5.1.4.1.1.77.1.2.1
VLSlideCoordinatesMicroscopicImageStorage:1.2.840.10008.5.1.4.1.1.77.1.3
VLPhotographicImageStorage:1.2.840.10008.5.1.4.1.1.77.1.4
VideoPhotographicImageStorage:1.2.840.10008.5.1.4.1.1.77.1.4.1
OphthalmicPhotography8BitImageStorage:1.2.840.10008.5.1.4.1.1.77.1.5.1
OphthalmicPhotography16BitImageStorage:1.2.840.10008.5.1.4.1.1.77.1.5.2
StereometricRelationshipStorage:1.2.840.10008.5.1.4.1.1.77.1.5.3
OphthalmicTomographyImageStorage:1.2.840.10008.5.1.4.1.1.77.1.5.4
WideFieldOphthalmicPhotographyStereographicProjectionImageStorage:1.2.840.10008.5.1.4.1.1.77.1.5.5
WideFieldOphthalmicPhotography3DCoordinatesImageStorage:1.2.840.10008.5.1.4.1.1.77.1.5.6
OphthalmicOpticalCoherenceTomographyEnFaceImageStorage:1.2.840.10008.5.1.4.1.1.77.1.5.7
OphthalmicOpticalCoherenceTomographyBscanVolumeAnalysisStorage:1.2.840.10008.5.1.4.1.1.77.1.5.8
VLWholeSlideMicroscopyImageStorage:1.2.840.10008.5.1.4.1.1.77.1.6
DermoscopicPhotographyImageStorage:1.2.840.10008.5.1.4.1.1.77.1.7
LensometryMeasurementsStorage:1.2.840.10008.5.1.4.1.1.78.1
AutorefractionMeasurementsStorage:1.2.840.10008.5.1.4.1.1.78.2
KeratometryMeasurementsStorage:1.2.840.10008.5.1.4.1.1.78.3
SubjectiveRefractionMeasurementsStorage:1.2.840.10008.5.1.4.1.1.78.4
VisualAcuityMeasurementsStorage:1.2.840.10008.5.1.4.1.1.78.5
SpectaclePrescriptionReportStorage:1.2.840.10008.5.1.4.1.1.78.6
OphthalmicAxialMeasurementsStorage:1.2.840.10008.5.1.4.1.1.78.7
IntraocularLensCalculationsStorage:1.2.840.10008.5.1.4.1.1.78.8
MacularGridThicknessAndVolumeReportStorage:1.2.840.10008.5.1.4.1.1.79.1
OphthalmicVisualFieldStaticPerimetryMeasurementsStorage:1.2.840.10008.5.1.4.1.1.80.1
OphthalmicThicknessMapStorage:1.2.840.10008.5.1.4.1.1.81.1
CornealTopographyMapStorage:1.2.840.10008.5.1.4.1.1.82.1
BasicTextSRStorage:1.2.840.10008.5.1.4.1.1.88.11
EnhancedSRStorage:1.2.840.10008.5.1.4.1.1.88.22
ComprehensiveSRStorage:1.2.840.10008.5.1.4.1.1.88.33
Comprehensive3DSRStorage:1.2.840.10008.5.1.4.1.1.88.34
ExtensibleSRStorage:1.2.840.10008.5.1.4.1.1.88.35
ProcedureLogStorage:1.2.840.10008.5.1.4.1.1.88.40
MammographyCADSRStorage:1.2.840.10008.5.1.4.1.1.88.50
KeyObjectSelectionDocumentStorage:1.2.840.10008.5.1.4.1.1.88.59
ChestCADSRStorage:1.2.840.10008.5.1.4.1.1.88.65
XRayRadiationDoseSRStorage:1.2.840.10008.5.1.4.1.1.88.67
RadiopharmaceuticalRadiationDoseSRStorage:1.2.840.10008.5.1.4.1.1.88.68
ColonCADSRStorage:1.2.840.10008.5.1.4.1.1.88.69
ImplantationPlanSRStorage:1.2.840.10008.5.1.4.1.1.88.70
AcquisitionContextSRStorage:1.2.840.10008.5.1.4.1.1.88.71
SimplifiedAdultEchoSRStorage:1.2.840.10008.5.1.4.1.1.88.72
PatientRadiationDoseSRStorage:1.2.840.10008.5.1.4.1.1.88.73
PlannedImagingAgentAdministrationSRStorage:1.2.840.10008.5.1.4.1.1.88.74
PerformedImagingAgentAdministrationSRStorage:1.2.840.10008.5.1.4.1.1.88.75
EnhancedXRayRadiationDoseSRStorage:1.2.840.10008.5.1.4.1.1.88.76
ContentAssessmentResultsStorage:1.2.840.10008.5.1.4.1.1.90.1
MicroscopyBulkSimpleAnnotationsStorage:1.2.840.10008.5.1.4.1.1.91.1
EncapsulatedPDFStorage:1.2.840.10008.5.1.4.1.1.104.1
EncapsulatedCDAStorage:1.2.840.10008.5.1.4.1.1.104.2
EncapsulatedSTLStorage:1.2.840.10008.5.1.4.1.1.104.3
EncapsulatedOBJStorage:1.2.840.10008.5.1.4.1.1.104.4
EncapsulatedMTLStorage:1.2.840.10008.5.1.4.1.1.104.5
PositronEmissionTomographyImageStorage:1.2.840.10008.5.1.4.1.1.128
LegacyConvertedEnhancedPETImageStorage:1.2.840.10008.5.1.4.1.1.128.1
EnhancedPETImageStorage:1.2.840.10008.5.1.4.1.1.130
BasicStructuredDisplayStorage:1.2.840.10008.5.1.4.1.1.131
CTPerformedProcedureProtocolStorage:1.2.840.10008.5.1.4.1.1.200.2
XAPerformedProcedureProtocolStorage:1.2.840.10008.5.1.4.1.1.200.8
RTImageStorage:1.2.840.10008.5.1.4.1.1.481.1
RTDoseStorage:1.2.840.10008.5.1.4.1.1.481.2
RTStructureSetStorage:1.2.840.10008.5.1.4.1.1.481.3
RTBeamsTreatmentRecordStorage:1.2.840.10008.5.1.4.1.1.481.4
RTPlanStorage:1.2.840.10008.5.1.4.1.1.481.5
RTBrachyTreatmentRecordStorage:1.2.840.10008.5.1.4.1.1.481.6
RTTreatmentSummaryRecordStorage:1.2.840.10008.5.1.4.1.1.481.7
RTIonPlanStorage:1.2.840.10008.5.1.4.1.1.481.8
RTIonBeamsTreatmentRecordStorage:1.2.840.10008.5.1.4.1.1.481.9
RTPhysicianIntentStorage:1.2.840.10008.5.1.4.1.1.481.10
RTSegmentAnnotationStorage:1.2.840.10008.5.1.4.1.1.481.11
RTRadiationSetStorage:1.2.840.10008.5.1.4.1.1.481.12
CArmPhotonElectronRadiationStorage:1.2.840.10008.5.1.4.1.1.481.13
TomotherapeuticRadiationStorage:1.2.840.10008.5.1.4.1.1.481.14
RoboticArmRadiationStorage:1.2.840.10008.5.1.4.1.1.481.15
RTRadiationRecordSetStorage:1.2.840.10008.5.1.4.1.1.481.16
RTRadiationSalvageRecordStorage:1.2.840.10008.5.1.4.1.1.481.17
TomotherapeuticRadiationRecordStorage:1.2.840.10008.5.1.4.1.1.481.18
CArmPhotonElectronRadiationRecordStorage:1.2.840.10008.5.1.4.1.1.481.19
RoboticRadiationRecordStorage:1.2.840.10008.5.1.4.1.1.481.20
RTRadiationSetDeliveryInstructionStorage:1.2.840.10008.5.1.4.1.1.481.21
RTTreatmentPreparationStorage:1.2.840.10008.5.1.4.1.1.481.22
RTBeamsDeliveryInstructionStorage:1.2.840.10008.5.1.4.34.7
RTBrachyApplicationSetupDeliveryInstructionStorage:1.2.840.10008.5.1.4.34.10
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
