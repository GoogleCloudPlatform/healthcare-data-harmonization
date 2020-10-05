<!--*
go/g3doc-canonical-go-links
*-->

<!--*
# Document freshness: For more information, see go/fresh-source.
freshness: { owner: 'sshussain' reviewed: '2020-04-29' }
*-->

<This is a g3doc Markdown page. For more information, see the Markdown Reference
(go/g3doc-reference) and the g3doc Style Guide (go/g3doc-style).>

<!--*
[TOC]
*-->

<!--*
## Tips

*   Make sure your page has a page title (see above)
*   Keep this file formatted using go/mdformat.
*-->

# Healthcare Data Harmonization: Mapping Guide

This document describes a mapping guide to transform various healthcare data
standards and terminologies using the
[Whistle Data Transformation Language](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_language/doc/index.md?cl=head).

## Healthcare Data Standards

Achieving healthcare data harmonization and interoperability among different
healthcare applications is deeply dependent on the use of standard healthcare
information models and controlled terminologies. Harmonized healthcare data
integration enables sharing and re-use of clinical data for predictive data
analytics and clinical decision making. Aiming towards this goal, various
healthcare data standards and terminologies are defined by organizations such as
HL7, OHDSI, PhysioNet, CDISC, IHTSDO, etc.

Widely used data standards
include:[HL7 v2](https://www.hl7.org/implement/standards/product_brief.cfm?product_id=185),
[HL7 CDA](https://www.hl7.org/implement/standards/product_brief.cfm?product_id=7),
[HL7 FHIR](https://www.hl7.org/fhir/STU3/),
[OMOP CDM](https://www.ohdsi.org/data-standardization/the-common-data-model/),
[MIMIC](https://mimic.physionet.org/)

## Mapping Process

A mapping process aims towards identifying alignments between source and target
schemas for achieving data transformation and mediation between two or more data
sources. A mapping process is outlined via the following steps:

### Step 1: Mapping Gap Analysis

In order to find an alignment between the source and target schemas, a crucial
step is determining the mapping gap. This step is heavily dependent on a
targeted use-case, when sometimes a complete alignment is neither achievable nor
necessarily desirable. For a targeted use-case, it is possible that a partial
alignment, with an outstanding mapping gap, would be sufficient to transform the
source data into the target schema, which could enable data integration and
sharing among data consumers in desirable data formats.

The process of Mapping Gap Analysis between source and target schemas is
comprised of:

*   Finding candidate mappings between source and target concepts
*   Finding candidate mappings between source and target attributes
*   Ensuring mandatory target concepts and attributes are mapped
*   Documenting unmapped concepts and attributes for further considerations

### Step 2: Mapping Configuration

TODO(b/158833342): Write Mapping Configuration section

1.  Datatype Transformation
2.  Vocabulary Transformation
3.  Data Element Transformation

## Whistle Data Transformation Language

The
[Whistle Data Transformation Language](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_language/doc/index.md?cl=head)
expresses data mappings from one schema to another. It lets users transform
complex, nested data formats into other complex and nested data formats.

## Mapping Healthcare Data Standards

TODO(b/158832819): Write Mapping Healthcare Data Standards section

### [HL7 v2 to FHIR](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_configs/hl7v2_fhir_stu3/doc/v2tofhir.md)

### OMOP to FHIR

### FHIR to OMOP

### MIMIC to FHIR

### MIMIC to OMOP
