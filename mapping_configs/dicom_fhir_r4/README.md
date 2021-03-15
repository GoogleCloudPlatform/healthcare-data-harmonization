# DICOM Study to FHIR R4 ImagingStudy

<!--*
# Document freshness: For more information, see go/fresh-source.
freshness: { owner: 'dbeaudreau' reviewed: '2021-03-15' }
*-->

This folder contains a mapping between a DICOM study as DICOM JSON and a FHIR R4
ImagingStudy resource.

## Using the mapping

The input to the mapping must be a DICOM study encoded in DICOM JSON. The DICOM
study can be retrieved from the Cloud Healthcare API by a call to
RetrieveMetadata:

https://cloud.google.com/healthcare/docs/how-tos/dicomweb#retrieving_metadata

The metadata must contain all instances from the study. This means that the
command to retrieve the study must follow the format below.

```
curl -X GET \
     -H "Authorization: Bearer $(gcloud auth application-default print-access-token)" \
     "https://healthcare.googleapis.com/v1/projects/PROJECT_ID/locations/LOCATION/datasets/DATASET_ID/dicomStores/DICOM_STORE_ID/dicomWeb/studies/STUDY_UID/metadata"
```

The input must be wrapped in a JSON object wrapper like so:

```
{
  "study": <DICOM JSON study array>
}
```

## How the mapping works

The DICOM to FHIR mapping extracts tags from the study and builds the FHIR
resource from these tag values. For every field in the ImagingStudy field that
is a reference, the mapping attempts to create a
[logical](https://www.hl7.org/fhir/references.html#logical) reference using
identifiers and codes present in the study.

The mapping was based on the DICOM tag mapping defined in the FHIR standard.
https://www.hl7.org/fhir/imagingstudy-mappings.html#dicom

## Running the mapping

The mapping can be run using
[these instructions.](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_configs/README.md)

## Unmapped fields

Certain fields could not be mapped.

[encounter](https://www.hl7.org/fhir/imagingstudy-definitions.html#ImagingStudy.encounter) -
There is no mapping defined from DICOM tags for an encounter.

[procedureReference](https://www.hl7.org/fhir/imagingstudy-definitions.html#ImagingStudy.procedureReference) -
While the DICOM instances do contain procedure codes, the FHIR standard requires
a single procedure reference. We cannot map multiple procedure codes to a single
logical FHIR reference so this field is left unmapped.

[endpoint](https://www.hl7.org/fhir/imagingstudy-definitions.html#ImagingStudy.endpoint) -
There is no information in DICOM JSON which indicates the endpoint it can be
accessed from. This applies to both the study and series endpoints.
