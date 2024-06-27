FHIR Resources Conversion

# Brief Description

We plan to implement a round-trip conversion for FHIR resources across all FHIR
versions using the Whistle mapping language. It can be potentially integrated
into the Cloud Healthcare FHIR API and other ETL pipelines where the Whistle
mapping framework can be used. The conversion specifies field-to-field
transformation that covers, in addition to basic mappings, incompatible field
mappings and code-system/value-sets mappings.

# Entry Point

fhir_harmonization_r3r4.wstl is an entry point of a FHIR version conversion
mapping, however it is currently designed to work with HDEv2 only.
For use with legacy pipelines, the entry point will need to be adapted.

# Related links

FHIR roadmaps r3r4: https://www.hl7.org/fhir/r3maps.html

FHIR roadmaps r2r3: http://hl7.org/fhir/STU3/r2maps.html

Running Whistle mapping files: https://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/quickrun/README.md

# Implemented Features

- Implemented baseline conversions followed the FHIR roadmaps R3R4 and R2R3.

- Support feature for converting newly added / deleted / modified incompatible
  fields with use of extension field.

- Support feature for converting fields with structure change.

- Support feature for fields cardinality change.

- Unit tests for util functions and resources with high priority fields.

# Missing Features

- Some resource types don't have an official mapping at all. In that case, we
  left the wstl mapping empty for now.

- Newly added resource types or removed resource types are not supported.

- Code fields with ValueSet binding changes are put in extension for now.
  Technically, at least the overlapping code values can be set on the fields.

- Reference fields with changes in the allowed resource types are directly
  copied. Strictly speaking, this is invalid.

- Missing required fields are ignored. Strictly speaking, this causes "invalid"
  resources.

- Nested resources are directly copied. So they are not converted at all.

- Top level extensions and modifier extensions are directly copied. There are
  changes in the primitive fields inside the extensions across versions.

- Extensions in the sub-levels are all ignored.
