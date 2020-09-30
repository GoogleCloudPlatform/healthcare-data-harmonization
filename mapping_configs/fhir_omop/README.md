# FHIR to OMOP CDM: Mapping Guide

This document describes a mapping guide to transform FHIR STU3 resources to OMOP
CDM v6.0 entities using the
[Whistle Data Transformation Language](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_language/doc/index.md?cl=head).

A mapping process aims towards identifying alignments between source and target
schemas for achieving data transformation and mediation between two or more data
sources. A mapping process is outlined via the following steps:

## Mapping Gap Analysis

First, conduct a mapping gap analysis between
[source](https://www.hl7.org/fhir/STU3/) (FHIR STU3) and
[target](https://github.com/OHDSI/CommonDataModel/blob/v6.0.0/Documentation/CommonDataModel_Wiki_Files/Home.md)
(OMOP CDM v6.0) schemas. Mapping gap analysis is outlined via the following
steps:

*   Find candidate mappings between source and target concepts
*   Find candidate mappings between source and target attributes
*   Ensure mandatory target concepts and attributes (as required by the target
    model) are mapped
*   Document unmapped concepts and attributes for further considerations

In the FHIR to OMOP project, let us consider a mapping gap analysis between HL7
FHIR [`Patient`](https://www.hl7.org/fhir/stu3/patient.html) resource and OMOP
CDM
[`Person`](https://github.com/OHDSI/CommonDataModel/blob/v6.0.0/Documentation/CommonDataModel_Wiki_Files/StandardizedClinicalDataTables/PERSON.md)
and
[`Location`](https://github.com/OHDSI/CommonDataModel/blob/v6.0.0/Documentation/CommonDataModel_Wiki_Files/StandardizedHealthSystemDataTables/LOCATION.md)
entities, shown below.

![Gap Analysis](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_configs/fhir_omop/doc/img/FHIR-OMOP-Gap-Analysis.png "Gap Analysis")

## Mapping Configurations

Based on the example mapping gap analysis discussed above, integration analysts
and developers can define data tranformation by declaring mapping configurations
using
[Whistle Data Transformation Language](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_language/doc/reference.md?cl=head).

### Datatype Transformation {.leaf-numbered}

Datatype transformation may be needed to transform a source attribute into a
target attribute when an attribute contains complex datatypes. For example,
based on the mapping gap analysis (see above), the source attribute `address`
contains a complex datatype `Address` and needs to be mapped to the target
concept `Location`. Additional mappings are needed to be defined to transform
`Address` datatype into an `Location` datatype, shown below:

![Mapping: DataType](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_configs/fhir_omop/doc/img/FHIR-OMOP-Add-Loc.png "Mapping: DataType")

The above highlighted datatype transformations are defined using
[Whistle syntax](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_language/doc/index.md?cl=head)
listed below:

*   Datatype Transformation: `Address` to `Location`

```
// Description: Constructs an OMOP Location Table (v6.0) based on FHIR STU3 Address Datatype
//
// Argument(s):
//   Address: http://hl7.org/fhir/STU3/datatypes.html#Address
//
// Output(s):
//   Location: https://github.com/OHDSI/CommonDataModel/blob/v6.0.0/Documentation/CommonDataModel_Wiki_Files/StandardizedHealthSystemDataTables/LOCATION.md
//
def Address_Location(Address) {
  location_id : $IntHash($UUID());
  address_1 : Address.line[0];
  address_2 : Address.line[1];
  city : Address.city;
  state : Address.state;
  zip : Address.postalCode;
  county : Address.district;
  country : Address.country;
  location_source_value : Address.text;
  var geoLocation : Address.extension[where $.url = "http://hl7.org/fhir/StructureDefinition/geolocation"];
  var latitudeInfo : geoLocation[0].extension[where $.url = "latitude"];
  var longitudeInfo : geoLocation[0].extension[where $.url = "longitude"];
  latitude : latitudeInfo[0].valueDecimal;
  longitude : longitudeInfo[0].valueDecimal;
}
```

### Vocabulary Transformation {.leaf-numbered}

Some attributes contain values drawn from a pre-defined vocabulary (i.e. value
set), known as *controlled vocabulary attributes*. In order to transform such
attributes, we require mappings between their respective value-sets. For
example, based on the above-mentioned gap analysis exercise, both `gender` and
`gender_concept_id` are found as controlled vocabulary attributes, and therefore
requires vocabulary transformation between their value-sets, shown in Figure
below:

![Gap Analysis: Vocabulary](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_configs/fhir_omop/doc/img/FHIR-OMOP-ConceptMap.png "Gap Analysis: Vocabulary"){style="display:block;width:610px;margin:auto"}

Based on the above scenario, mappings between `gender` and `gender_concept_id`
value-sets are defined as
[FHIR Concept Maps](https://www.hl7.org/fhir/conceptmap.html), listed below:

```
{
  "group": [
    {
      "element": [
        {
          "code": "male",
          "display": "Male",
          "target": [
            {
              "code": "8507",
              "display": "MALE",
              "equivalence": "equivalent"
            }
          ]
        },
        {
          "code": "female",
          "display": "Female",
          "target": [
            {
              "code": "8532",
              "display": "FEMALE",
              "equivalence": "equivalent"
            }
          ]
        }
      ],
      "source": "http://hl7.org/fhir/administrative-gender",
      "target": "Gender"
    }
  ],
  "id": "FHIR-OMOP-ConceptMap",
  "resourceType": "ConceptMap",
  "version": "v1"
}
```

Based on the above-mentioned concept map, vocabulary transformation between
value-sets are defined using
[Whistle syntax](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_language/doc/reference.md?cl=head#code-harmonization)
below:

```
// Description: Converts FHIR terminology into OMOP CDM terminology
//
// Argument(s):
//   FHIR Code datatype: https://www.hl7.org/fhir/stu3/datatypes.html#code
//
// Output(s):
//   OMOP Code datatype
//
def Convert_Terminology(Code){
  var mapping : $HarmonizeCode("$Local", Code, "", "FHIR-OMOP-ConceptMap");
  $this : $ParseInt(mapping[0].code);
}
```

### Data Element Transformation {.leaf-numbered}

Based on the mapping gap analysis perfomed between the FHIR `Patient` resource
and OMOP CDM `Person` table, and their Datatype and Vocabulary transformations,
`Patient` resource is transfomed into `Person` table by defining mappings
between their attributes using
[Whistle syntax](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_language/doc/index.md?cl=head)
below:

```
// Description: Constructs an OMOP Person Table (v6.0) based on FHIR STU3 Patient Resource
//
// Argument(s):
//   Patient: https://www.hl7.org/fhir/stu3/patient.html
//   LocationID: OMOP Location Table (v6.0), already transformed using Address_Location projector - https://github.com/OHDSI/CommonDataModel/blob/v6.0.0/Documentation/CommonDataModel_Wiki_Files/StandardizedHealthSystemDataTables/LOCATION.md
// Output(s):
//   Person: https://github.com/OHDSI/CommonDataModel/blob/v6.0.0/Documentation/CommonDataModel_Wiki_Files/StandardizedClinicalDataTables/PERSON.md
//
def Patient_Person(Patient, locationID) {
  person_id : $IntHash(Patient.id);
  person_source_value : Patient.identifier[0].value;
  gender_source_value : Patient.gender;
  gender_concept_id : Convert_Terminology(Patient.gender);
  gender_source_concept_id : Convert_Terminology(Patient.gender);
  birth_datetime : Patient.birthDate;
  death_datetime : Patient.deceasedDateTime;
  year_of_birth : Extract_Year(Patient.birthDate);
  month_of_birth : Extract_Month(Patient.birthDate);
  day_of_birth : Extract_Day(Patient.birthDate);
  var race : Patient.extension[where $.url = "http://hl7.org/fhir/us/core/StructureDefinition/us-core-race"];
  race_concept_id : Convert_Terminology(race[0].extension[0].valueCoding.code);
  race_source_value : race[0].extension[0].valueCoding.display;
  race_source_concept_id : Convert_Terminology(race[0].extension[0].valueCoding.code);
  var ethnicity : Patient.extension[where $.url = "http://hl7.org/fhir/us/core/StructureDefinition/us-core-ethnicity"];
  ethnicity_concept_id : Convert_Terminology(ethnicity[0].extension[0].valueCoding.code);
  ethnicity_source_value : ethnicity[0].extension[0].valueCoding.display;
  ethnicity_source_concept_id : Convert_Terminology(ethnicity[0].extension[0].valueCoding.code);
  var provider : Patient.generalPractitioner[where Extract_ReferenceName($.reference) = "Practitioner"];
  provider_id : Extract_ReferenceID(provider[0].reference);
  care_site_id : Extract_ReferenceID(Patient.managingOrganization.reference);
  location_id : locationID;
}
```

### Transformation Post-processing and Utilities {.leaf-numbered}

[Post-processing](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_language/doc/reference.md?cl=head#post-processing-post)
and outputing utilities are defined to collect and output transformation
results, are listed below:

*   Extract Date and Time in OMOP format

```
// Description: Extracts a date in OMOP format from a FHIR DateTime datatype
//
// Argument(s):
//   FHIR DateTime datatype: https://www.hl7.org/fhir/stu3/datatypes.html#dateTime
//
// Output(s):
//   OMOP Date datatype: SQL datetime
//
def Extract_Date(str) {
  $this: $ReformatTime("2006-01-02T15:04:05Z07:00", str, "2006-01-02");
}

// Description: Extracts a time in OMOP format from a FHIR DateTime datatype
//
// Argument(s):
//   FHIR DateTime datatype: https://www.hl7.org/fhir/stu3/datatypes.html#dateTime
//
// Output(s):
//   OMOP Time datatype: SQL datetime
//
def Extract_Time(str) {
  $this: $ReformatTime("2006-01-02T15:04:05Z07:00", str, "15:04:05Z07:00");
}

```

*   Extract Year, Month and Day

```
// Description: Extracts a Year in OMOP format from a FHIR Date datatype
//
// Argument(s):
//   FHIR Date datatype: https://www.hl7.org/fhir/stu3/datatypes.html#date
//
// Output(s):
//   OMOP Year datatype: integer
//
def Extract_Year(str) {
  $this: $ReformatTime("2006-01-02", str, "2006");
}

// Description: Extracts a Month in OMOP format from a FHIR Date datatype
//
// Argument(s):
//   FHIR Date datatype: https://www.hl7.org/fhir/stu3/datatypes.html#date
//
// Output(s):
//   OMOP Month datatype: integer
//
def Extract_Month(str) {
  $this: $ReformatTime("2006-01-02", str, "01");
}

// Description: Extracts a Day in OMOP format from a FHIR Date datatype
//
// Argument(s):
//   FHIR Date datatype: https://www.hl7.org/fhir/stu3/datatypes.html#date
//
// Output(s):
//   OMOP Day datatype: integer
//
def Extract_Day(str) {
  $this: $ReformatTime("2006-01-02", str, "02");
}

```

*   Extract Reference from FHIR resource

```
// Description: Extracts a ReferenceID from a FHIR Reference datatype
//
// Argument(s):
//   FHIR Reference datatype: https://www.hl7.org/fhir/stu3/references-definitions.html#Reference.reference
//
// Output(s):
//   FHIR Reference ID: https://www.hl7.org/fhir/stu3/references-definitions.html#Reference.reference
//
def Extract_ReferenceID(str) {
  var temp: $StrSplit(str, "/");
  $this: $IntHash(temp[1]);
}

// Description: Extracts a Reference Resource Name from a FHIR Reference datatype
//
// Argument(s):
//   FHIR Reference datatype: https://www.hl7.org/fhir/stu3/references-definitions.html#Reference.reference
//
// Output(s):
//   FHIR Reference Resource Name: https://www.hl7.org/fhir/stu3/references-definitions.html#Reference.reference
//
def Extract_ReferenceName(str) {
  var temp: $StrSplit(str, "/");
  $this: temp[0];
}

```

*   Output transformed OMOP CDM `Person` and `Location` Table based FHIR
    `Patient` resource

```
// Description: Define FHIR STU3 to OMOP (v6.0) transformation
//
// Argument(s):
//   Patient: https://www.hl7.org/fhir/stu3/patient.html
// Output(s):
//   Location: https://github.com/OHDSI/CommonDataModel/blob/v6.0.0/Documentation/CommonDataModel_Wiki_Files/StandardizedHealthSystemDataTables/LOCATION.md
//   Person: https://github.com/OHDSI/CommonDataModel/blob/v6.0.0/Documentation/CommonDataModel_Wiki_Files/StandardizedClinicalDataTables/PERSON.md
//
def Process_Patient(Patient) {
  var location : Address_Location(Patient.address[0]);
  Location : location;
  Person : Patient_Person(Patient, location.location_id);
}
```
