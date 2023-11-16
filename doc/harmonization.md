# Package harmonization

[TOC]

## Import

The harmonization package can be imported by adding this code to the top of your Whistle file:


```
import "class://com.google.cloud.verticals.foundations.dataharmonization.plugins.harmonization.HarmonizationPlugin"
```

## Functions
### harmonize
`harmonization::harmonize(harmonizationSource: String, sourceCode: String, sourceSystem: String, conceptmapId: String)` returns `Data` - `Array` representing FHIR [Coding](https://build.fhir.org/datatypes.html#Coding) datatypes.

#### Arguments
**harmonizationSource**: `String` - - "$Local" for all conceptmaps loaded from Json, or the name of remote FHIR store.


**sourceCode**: `String` - - the sourceCode to be harmonized.


**sourceSystem**: `String` - - the sourceSystem to find which Group in the ConceptMap should be used.


**conceptmapId**: `String` - - the id of ConceptMap used for harmonization.


#### Description
Maps the given (sourceCode, sourceSystem) according to the ConceptMap identified by conceptmapID.

### harmonizeWithTarget
`harmonization::harmonizeWithTarget(harmonizationSource: String, sourceCode: String, sourceSystem: String, targetSystem: String, conceptmapId: String)` returns `Data` - `Array` representing FHIR [Coding](https://build.fhir.org/datatypes.html#Coding) datatypes.

#### Arguments
**harmonizationSource**: `String` - - "$Local" for all conceptmaps loaded from Json, or the name of remote FHIR store.


**sourceCode**: `String` - - the sourceCode to be harmonized.


**sourceSystem**: `String` - - the sourceSystem to find which Group in the ConceptMap should be used.


**targetSystem**: `String` - - the targetSystem to find which Group in the ConceptMap should be used.


**conceptmapId**: `String` - - the id of ConceptMap used for harmonization.


#### Description
Maps the given (sourceCode, sourceSystem, targetSystem) according to the ConceptMap identified by conceptmapID.


