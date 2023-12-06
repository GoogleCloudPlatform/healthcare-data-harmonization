# Package recon

[TOC]

## Import

The recon package can be imported by adding this code to the top of your Whistle
file:

```
import "class://com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.ReconciliationPlugin"
```

## Functions

### addStableIds

`recon::addStableIds(databaseName: String, resources: Data, resourceFragments:
Data, allMatchingConfig: Container)` returns `Data` - A representation of the
given fhir resources, with the references replaced, ids and stable ids assigned.

#### Arguments

**databaseName**: `String` -
projects/PROJECT_ID/instances/INSTANCE_ID/databases/DATABASE_ID

**resources**: `Data` - the FHIR resources

**resourceFragments**: `Data` - the resources that are referred by the incoming
resources, we put this type of resources in the resourceFragments list. The
resources are only built with external id.

**allMatchingConfig**: `Container` - the defined matching criteria for all
resources which is used in extracting property-values. Each field in the
container is named for the resource type. The corresponding values are the
matching criteria.

#### Description

addStableIds takes a list of related FHIR resources, and assigns stable IDs and
resource IDs to each resource. The resource IDs will be a hash of the resource
after the references have been replaced with stable ID references. All stable
IDs are computed using the provided matching criteria.

#### Throws

*   **`StableIdBundleProcessingException`** -

### allOf

`recon::allOf(matchingCriteria: Data, otherMatchingCriteria: Data...)` returns
`Data` - Data matching criteria that will match if all of the given matching
criteria

match.

#### Arguments

**matchingCriteria**: `Data` - Data initial matching criteria to use in the
result

**otherMatchingCriteria**: `Data...` - Zero or more Data matching criteria to
use in the result

#### Description

Creates Data matching criteria that will match if all of the given matching
criteria match. This operation is assumed to be in the context of a container.

### anyCoding

`recon::anyCoding(filters: Data...)` returns `Data` - Data matching criteria
that can match against any coding with filters applied.

#### Arguments

**filters**: `Data...` - Zero or more Data filters to apply. Filters are of the
form: { "field": "field", // One of "system" or "code" "value": "value" }

#### Description

Creates Data matching criteria for matching any coding. Filters can be applied
to codings.

### anyIdentifier

`recon::anyIdentifier(filters: Data...)` returns `Data` - Data matching criteria
that can match against any identifier with filters applied.

#### Arguments

**filters**: `Data...` - Zero or more Data filters to apply. Filters are of the
form: { "field": "field", // One of "system" or "value" "value": "value" }

#### Description

Creates Data matching criteria for matching any identifier. Filters can be
applied to identifiers based on "system" or "value".

### anyMetaExtension

`recon::anyMetaExtension(fieldFilters: Data...)` returns `Data` - Data matching
criteria that can match against any identifier with filters applied.

#### Arguments

**fieldFilters**: `Data...` - Zero or more Data filters to apply. FieldFilters
are of the form: recon::filterField{ "system", ["system1", "system2"] }

#### Description

Creates Data matching criteria for matching any meta.extension. Filters can be
applied to identifiers based on "url" or "valueString".

### anyMetaTag

`recon::anyMetaTag(fieldFilters: Data...)` returns `Data` - Data matching
criteria that can match against any identifier with filters applied.

#### Arguments

**fieldFilters**: `Data...` - Zero or more Data filters to apply. FieldFilters
are of the form: recon::filterField{ "system", ["system1", "system2"] }

#### Description

Creates Data matching criteria for matching any meta.tag. Filters can be applied
to identifiers based on "system" or "code".

### anyOf

`recon::anyOf(matchingCriteria: Data, otherMatchingCriteria: Data...)` returns
`Data` - Data matching criteria that will match if any of the given matching
criteria match.

#### Arguments

**matchingCriteria**: `Data` - Data initial matching criteria to use in the
result

**otherMatchingCriteria**: `Data...` - Zero or more Data matching criteria to
usein the result

#### Description

Creates Data matching criteria that will match if any of the given matching
criteria match. This operation is assumed to be in the context of a container.

### arrayAllOf

`recon::arrayAllOf(fieldName: String, matchingCriteria: Data,
otherMatchingCriteria: Data...)` returns `Data` - Data matching criteria that
will match if any of the given matching criteria match.

#### Arguments

**fieldName**: `String` - field name for an array field

**matchingCriteria**: `Data` - Data initial matching criteria to use in the
result

**otherMatchingCriteria**: `Data...` - Zero or more Data matching criteria to
usein the result

#### Description

Creates Data matching criteria that will match if all of the elements in the
array field match all of the given matching criteria.

### arrayAnyOf

`recon::arrayAnyOf(fieldName: String, matchingCriteria: Data,
otherMatchingCriteria: Data...)` returns `Data` - Data matching criteria that
will match if any of the given matching criteria match.

#### Arguments

**fieldName**: `String` - field name for an array field

**matchingCriteria**: `Data` - Data initial matching criteria to use in the
resulta

**otherMatchingCriteria**: `Data...` - Zero or more Data matching criteria to
use in the result

#### Description

Creates Data matching criteria that will match if any of the elements in the
array field match any of the given matching criteria.

### assignStableId

`recon::assignStableId(databaseName: String, matchingConfig: Container,
resource: Container)` returns `Data` - identifier container with the stable-id.

#### Arguments

**databaseName**: `String` -
projects/PROJECT_ID/instances/INSTANCE_ID/databases/DATABASE_ID

**matchingConfig**: `Container` - the defined matching criteria for extracting
property-values

**resource**: `Container` - the FHIR resource

#### Description

assignStableId takes a FHIR resource, extracts property-values to be used as
matching criteria, queries these in the Stable ID index and either returns a
match or a new UUID. The property values from the resource will be added the
index. If there are no property values, the Resource ID will be assigned as the
Stable ID.

### backfillReferences

`recon::backfillReferences(resource: Container, databaseName: String)` returns
`NullData`

#### Arguments

**resource**: `Container` - FHIR resource fetched from backfill GCS bucket.

**databaseName**: `String` -
projects/PROJECT_ID/instances/INSTANCE_ID/databases/DATABASE_ID.

#### Description

backfillReferences takes a FHIR resource, extracts all the valid references
(resourceType/stableId) in this resource and update spanner tables with
references.

### choiceField

`recon::choiceField(existing: Data, inbound: Data, mergeRule: String,
choiceFieldPaths: String...)` returns `Data` - singleton DefaultContainer of the
merged {choiceX field name : field value} or NullData if there is an empty merge
result.

#### Arguments

**existing**: `Data` - Data field value from the older resource to merge.

**inbound**: `Data` - Data field value from the more recent resource to merge.

**mergeRule**: `String` - String either "forceInbound" or "preferInbound".

**choiceFieldPaths**: `String...` - java.util.List<String> any number of choiceX
field paths from which to source the field values to merge.

#### Description

Merges Data choiceX fields from an existing and an inbound resource by finding
the fields to merge from among the possible 'choiceFieldPaths' provided and
applying the 'mergeRule' logic, such that the merge produces exactly one of the
provided choice fields in the final resource.

There can be, at most, one choice field present in each resource. For example,
one choiceX field for the Patient FHIR resource is the 'deceased[x]' field with
two possible field types: 'deceasedBoolean' and 'deceasedDateTime'. Therefore,
any given Patient resource can either have a 'deceasedBoolean' field, have a
'deceasedDateTime' field, or have neither. A Patient resource can never have
both of the 'deceased[x]' fields present.

Applying this example, if we have inbound and existing Patient resources like
the following:

existing: {

*   ...some other fields...
*   deceasedBoolean: "false";
*   ...some other fields...

}

inbound: {

*   ...some other fields...
*   deceasedDateTime: "02/03/2010";
*   ...some other fields...

}

and we apply the 'preferInbound' merge rule, then we first search the existing
and inbound resources for any of the 'deceased[x]' fields. Finding one of the
fields in each, we then merge the two fields using our merge rule - since there
is a 'deceased[x]' field present in the inbound resource, we prefer that value
over the existing value. Therefore, our output field added to the resource
written to the final store will be {deceasedDateTime: "02/03/2010"}.

If in the previous example, the 'deceasedDateTime' hadn't been present in the
inbound resource, then the 'preferInbound' rule would've meant we would push
{deceasedBoolean: "false"} from the existing resource to the output resource
written to the final store.

The 'preferInbound' rule means we:

*   - output the add field from the inbound resource if it has one
*   - otherwise, add the choiceX field from the existing resource if it has one
*   - otherwise, add no field to the output resource

The pattern for the 'forceInbound' rule is similar, but with the second line
removed - for this rule, the choiceX field from the existing resource is never
considered, even if it is present.

### clearHDEMetadata

`recon::clearHDEMetadata(resource: Data)` returns `Data` - The passed FHIR
resource with no reconciliation timestamp.

#### Arguments

**resource**: `Data` - Data Container object holding a FHIR resource.

#### Description

Clears the reconciliation timestamp from the provided FHIR resource's
'extension' field, Clears "urn:oid:data-type/data-source",
"reconciliation-external-id" from meta.tag, Clears "urn:oid:google/create-time"
from meta.extension.

### clearReconciliationTimestamp

`recon::clearReconciliationTimestamp(resource: Data)` returns `Data` - The
passed FHIR resource with no reconciliation timestamp.

#### Arguments

**resource**: `Data` - Data Container object holding a FHIR resource.

#### Description

Clears the reconciliation timestamp from the provided FHIR resource's
'extension' field.

### deleteRecordsWithStableId

`recon::deleteRecordsWithStableId(databaseName: String, deletionInfo:
Container)` returns `Data` - Successed deletionInfo containers

#### Arguments

**databaseName**: `String` - -
projects/PROJECT_ID/instances/INSTANCE_ID/databases/DATABASE_ID.

**deletionInfo**: `Container` - - Container with fields (resourceType, stableId)

#### Description

deleteRecordsWithStableId takes a container with {"resourceType", "stableId"}
and deletes Spanner records related in ALL stable id tables.

#### Throws

*   **`StableIdRollbackManagerException`** - when spanner transaction fails
*   **`IllegalArgumentException`** - when groupedRecords are not in the required
    format

### deleteStableIdRecords

`recon::deleteStableIdRecords(databaseName: String, stableIdPropertyValueId:
String, groupedRecords: Array)` returns `Data` - Container of resourceType and
stableId for the deleted spanner records.

#### Arguments

**databaseName**: `String` -
projects/PROJECT_ID/instances/INSTANCE_ID/databases/DATABASE_ID.

**stableIdPropertyValueId**: `String` - rowId of StableIdPropertyValue table.

**groupedRecords**: `Array` - array of containers (resourceType, resourceId,
stableId).

#### Description

deleteStableIdRecords takes stableIdPropertyValueId as well as an array of
containers which represents the spanner records associated with this
stableIdPropertyValueId, and deletes Spanner records in StableIdResources table.
If the size of deleted StableIdResources records matches the number of records
which refer to this stableIdPropertyValueId, then the stableIdPropertyValue
record with rowId stableIdPropertyValueId will get deleted as well since all of
associated records are going for deletion. For example:

recon::deleteStableIdRecords("databaseName", "stableIdPropertyValueId", Array of
(resourceType, resourceId, stableId));

The above plugin function will return a container of (resourceType, stableId)
for the deleted spanner records.

#### Throws

*   **`IllegalArgumentException`** - when input arguments are invalid.
*   **`StableIdRollbackManagerException`** - when spanner transaction fails.

### diff
`recon::diff(existing: Array, inbound: Array)` returns `Array` - Data merge
result.

#### Arguments
**existing**: `Array` - Data Array field value from the older resource to merge.


**inbound**: `Array` - Data Array field value from the more recent resource to
merge.


#### Description
Merges two Array field values from an existing and an inbound resource,
returning the elements in the first array but not in the second.

```
diff([{"target": {"reference": "Patient/patient_stableId1"}},
        {"target": {"reference": "Patient/patient_stableId2"}}],
       [{"target": {"reference": "Patient/patient_stableId1"}}]) =
       [{"target": {"reference": "Patient/patient_stableId2"}}]
```

### extractPropertyValues

`recon::extractPropertyValues(config: Data, resource: Data)` returns `Data` -
Array of parsed property-values.

#### Arguments

**config**: `Data` - the matching criteria config.

**resource**: `Data` - the FHIR resource.

#### Description

extractPropertyValues takes in a FHIR resource and matching configuration for
its resourceType and extracts property values for this resource.

#### Throws

*   **`MatchingCriteriaConfigException`** - when input matching config is
    invalid.
*   **`PropertyValueFetcherException`** - when property-values fetching fails.

### extractReconciliationTimestamp

`recon::extractReconciliationTimestamp(resource: Data)` returns `Data` - the
reconciliation timestamp, as a Primitive valueInstant.

#### Arguments

**resource**: `Data` - an intermediate FHIR snapshot with a reconciliation
timestamp.

#### Description

Extracts the reconciliation timestamp from the intermediate FHIR resource from
either the extension or meta extension, depending on where it can be found.

For example, passing the input resource below to this function returns
"2023-01-01T00:00:00.000+00:00"

```
{
    "resourceType": "Patient",
    "id": "123456",
    "meta": {
    "extension": [
         {
           "url": "urn:oid:google/reconciliation-timestamp",
           "valueInstant": "2023-01-01T00:00:00.000+00:00"
         }
       ],
       "lastUpdated": "2023-03-15T17:16:20.878972+00:00",
       "versionId": "MTY3ODkwMDU4MDg3ODk3MjAwMA"
    },
  }
```

### filter

`recon::filter(fieldName: String, fieldValue: Data)` returns `Data` - A Data
filter.

#### Arguments

**fieldName**: `String` - String naming the container field to filter

**fieldValue**: `Data` - Data Primitive specifying the value of the named field
is filtered.

#### Description

Creates a Data filter for filtering containers in an array.

### filterField

`recon::filterField(fieldName: String, fieldValues: Data)` returns `Data` - Data
representing the matching criteria.

#### Arguments

**fieldName**: `String` - The field name to match

**fieldValues**: `Data` - The field values to filter on. This can be an array of
primitives or a primitive.

#### Description

Creates Data matching criteria for a primitive field that applies only when the
field value matches the given filter. This is best used in conjunction with
other rules. For example:

recon::arrayAnyOf("myArray", recon::filterField("field1", ["value1"]))

The above config will match entries in myArray where field1 matches value1.

### filterValue

`recon::filterValue(fieldValues: Data)` returns `Data` - Data representing the
matching criteria.

#### Arguments

**fieldValues**: `Data` - The field values to filter on. This can be an array of
primitives or a primitive.

#### Description

Creates Data matching criteria for a primitive field that applies only when the
value matches the given filter. This is best used in conjunction with other
rules. For example:

recon::arrayAnyOf("myArray", recon::filterValue(["value1", "value2"]))

The above config will match entries in an array of primitives where values match
value1 or value2.

### forceInbound

`recon::forceInbound(existing: Data, inbound: Data)` returns `Data` - Data merge
result.

#### Arguments

**existing**: `Data` - Data field value from the older resource to merge.

**inbound**: `Data` - Data field value from the more recent resource to merge.

#### Description

Merges two Data field values from an existing and an inbound resource, returning
the inbound value if it exists. Implements the "From IR" rule from
go/fhir-reconciliation-rules.

### getBypassReconciliationSystem

`recon::getBypassReconciliationSystem()` returns `Primitive` - bypass
reconciliation system for the meta tag, must end in "/bypass-reconciliation"

#### Arguments

#### Description

getBypassReconciliationSystem creates the system for the meta tag where the
resource should bypass reconciliation

### getCaveatCaseInterval

`recon::getCaveatCaseInterval()` returns `Data` - Data Desired caveat case
pipeline start date and end date.

#### Arguments

#### Description

Calculate the interval for caveat case pipeline from the cloud schedule.

### getExternalIdTagSystemValue

`recon::getExternalIdTagSystemValue()` returns `Primitive` - a string that
should be used when building meta.tag for stable id purposes.

#### Arguments

#### Description

getExternalIdTagSystemValue provides a string value that should be used as
default when moving external id into meta.tag for stable id matching.

### getFinalStableId

`recon::getFinalStableId(databaseName: String, stableIdPropertyValueId: String)`
returns `Primitive` - final stable-id in StableIdPropertyValue table.

#### Arguments

**databaseName**: `String` -
projects/PROJECT_ID/instances/INSTANCE_ID/databases/DATABASE_ID.

**stableIdPropertyValueId**: `String` - the StableIdPropertyValueId in
StableIdPropertyValue table.

#### Description

getFinalStableId takes the StableIdPropertyValueId and returns corresponding
final stable id.

#### Throws

*   **`StableIdIndexException`** - when spanner query fails.

### getOutgoingReferences

`recon::getOutgoingReferences(input: Container, filterResourceTypes: Array)`
returns `Data` - An Array representing outgoing references, in the format
{"reference": "outgoingResourceType"/"outgoingResourceId", from:
"inputResourceType"/"inputResourceId"}

#### Arguments

**input**: `Container` - FHIR R4 resource Container, or a container type with R4
resource in the field "resource".

**filterResourceTypes**: `Array` - An Array specifying the resourceTypes of
interest.

#### Description

getOutgoingReferences outputs an array of references within a given FHIR R4
resource. This function only outputs outgoing references of certain
resourceTypes when filterResourceTypes is available. When the filter is empty,
outputs all resourceTypes.

### getReferenceId

`recon::getReferenceId(databaseName: String, matchingConfig: Container,
resource: Container)` returns `Primitive` - the resource's stable-id.

#### Arguments

**databaseName**: `String` -
projects/PROJECT_ID/instances/INSTANCE_ID/databases/DATABASE_ID

**matchingConfig**: `Container` - the defined matching criteria for extracting
property-values

**resource**: `Container` - the FHIR resource

#### Description

getReferenceId takes a FHIR resource, extracts property-values to be used as
matching criteria, queries these in the Stable ID index and returns a match. If
there are no property-values a StableIdIndexException is thrown as we cannot
create an entry in the PropertyBooleanIndex. The caveat case is not handled. If
there are no property values, the Resource ID will be assigned as the Stable ID.

### getReferenceIdFromExternalId

`recon::getReferenceIdFromExternalId(databaseName: String, resourceType: String,
externalId: String)` returns `Primitive` - the resource's stable-id.

#### Arguments

**databaseName**: `String` -
projects/PROJECT_ID/instances/INSTANCE_ID/databases/DATABASE_ID

**resourceType**: `String` - the resource type of the reference.

**externalId**: `String` - the external id of the reference.

#### Description

getReferenceIdFromExternalId takes a database name, resource type and external
Id, extracts property-values to be used as matching criteria, queries these in
the Stable ID index and returns a match. If there are no property-values a
StableIdIndexException is thrown as we cannot create an entry in the
PropertyBooleanIndex. If there are no property values, the Resource ID will be
assigned as the Stable ID.

#### Throws

*   **`StableIdIndexException`** -

### getStableIdRecords

`recon::getStableIdRecords(databaseName: String, resourceType: String,
resourceId: String)` returns `Data` - Array of StableIdPropertyValue Spanner
records.

#### Arguments

**databaseName**: `String` -
projects/PROJECT_ID/instances/INSTANCE_ID/databases/DATABASE_ID.

**resourceType**: `String` - resourceType of fhir store resource which gets
rolled back.

**resourceId**: `String` - resourceId of fhir store resource which gets rolled
back.

#### Description

getStableIdRecords takes resourceType and resourceId from LRO operation and
fetches the stable id records in the StableIdPropertyValue table.

#### Throws

*   **`IllegalArgumentException`** - when input arguments are invalid.
*   **`StableIdRollbackManagerException`** - when spanner query fails.

### getValidPatientStableIdWithMetadata

`recon::getValidPatientStableIdWithMetadata(databaseName: String,
pidWithMetadata: Container)` returns `Container` - the input pidWithMetadata if
the given `patient_id` is valid, null otherwise.

#### Arguments

**databaseName**: `String` - -
projects/PROJECT_ID/instances/INSTANCE_ID/databases/DATABASE_ID

**pidWithMetadata**: `Container` - - a Container with fields in the
format {"patient_id": "id", (optional) "metadata": {container of metadata}}

#### Description

getValidPatientStableIdWithMetadata checks if the given `patient_id` exists as a
known `stableId` for a patient resource. Returns the input pidWithMetadata if
true, otherwise returns null.

### mergeResources

`recon::mergeResources(sortedSnapshots: Data, resourceInfo: Data)` returns
`Data` - Data Container containing the cleaned merge result resource.

#### Arguments

**sortedSnapshots**: `Data` - - Data Array of resource snapshots.

**resourceInfo**: `Data` - - DataContainer holding 'resourceType', 'stableId'
and 'linkageIds' fields.

#### Description

Builtin for reconciliation to merge an Array of temporally-sorted resource
snapshots according to the resource-level merge rule and the configured
field-level merge function. Returns a container with resource and synthetic
fields. The resource field contains properly formatted, reconciled FHIR
resource, ready to be written to the final FHIR store. If merging the resource
produces synthetic resources, they will be added to synthetic field as an Array.

#### Throws

*   **`ConfigurationException`** - when there are improper user configurations.

### pathTo

`recon::pathTo(fieldName: String, matchingCriteria: Data)` returns `Data` - Data
matching criteria which is applied relative to the given field name.

#### Arguments

**fieldName**: `String` - String specifying the field for which the matching
criteria is relative to.

**matchingCriteria**: `Data` - Data arbitrary matching criteria.

#### Description

Creates Data matching criteria that will matching using matching criteria rooted
at the given field name.

### preferInbound

`recon::preferInbound(existing: Data, inbound: Data)` returns `Data` - Data
merge result.

#### Arguments

**existing**: `Data` - Data field value from the older resource to merge.

**inbound**: `Data` - Data field value from the more recent resource to merge.

#### Description

Merges two Data field values from an existing and an inbound resource, returning
the inbound value if it exists and the existing value otherwise. Implements the
"From IR if exists in IR, else from ER if exists in ER, else leave empty" rule
from go/fhir-reconciliation-rules.

### primitive

`recon::primitive(fieldName: String)` returns `Data` - Data matching criteria
that will match if values for the given field match.

#### Arguments

**fieldName**: `String` - String representing the field name.

#### Description

Creates Data matching criteria that will match using a simple primitive
comparison using the given field.

### reconcileResources

`recon::reconcileResources(groupedResourceInfos: PCollectionDataset,
intermediateStorePath: String, finalStorePath: String)` returns
`PCollectionDataset` - a PCollectionDataset with reconciled resources.

#### Arguments

**groupedResourceInfos**: `PCollectionDataset` - a PCollectionDataset of
Containers of resource metadata, consisting of resourceType and stableId, used
to retrieve appropriate snapshots.

**intermediateStorePath**: `String` - the path to the intermediate FHIR store
from which snapshots will be retrieved.

**finalStorePath**: `String` - the path to the final FHIR store, to fetch
reconciliation checkpoint metadata.

#### Description

Reconciles resources according to configured Merge rules. Expects resource info
groups as input, where each group is a collection of resourceType,stableId
pairs. The groups are expected to have the same stableId, but different
resourceType (input to this function is the output of a grouping on stable id
used for deduplication upstream). Each unique resource info input is mapped to
FhirSearchParameters, and these parameters are mapped to a PCollection of
intermediate snapshots with a unique key. The data is grouped on this key and
sorted by reconciliation timestamp within each key, then the sorted snapshots
are merged to produce a final resource, while writing Provenance resources to
the final FHIR store, and synthetic resources to the intermediate FHIR store.
The merged resources are returned.

### referenceFor

`recon::referenceFor(fieldName: String)` returns `Data` - Data reference
matching criteria which is applied relative to the given field name.

#### Arguments

**fieldName**: `String` - String specifying the field for which the reference
matching criteria is relative to.

#### Description

Creates Data matching criteria to match fhir references rooted at the given
field.

### replaceAllReferences

`recon::replaceAllReferences(databaseName: String, resource: Data)` returns
`Data`

#### Arguments

**databaseName**: `String` -
projects/PROJECT_ID/instances/INSTANCE_ID/databases/DATABASE_ID.

**resource**: `Data` - the FHIR R4 resource whose resource references need to be
replaced to stable id.

#### Description

replaceAllReferences takes a FHIR resource, and replaces any references within
the resource (in the form ResourceType/ID) with ResourceType/StableId. In
addition, the resourceType and resourceId of resource which contain all those
references as well as the corresponding rowIds of property value records for
external Ids will be tracked in the StableIdResources table.

#### Throws

*   **`IllegalArgumentException`** -
*   **`StableIdIndexException`** - when we fail to retrieve a stable id.
*   **`IOException`** -

### replaceAndAssignStableId

`recon::replaceAndAssignStableId(databaseName: String, matchingConfig:
Container, resource: Container)` returns `Data` - identifier container with the
stable-id.

#### Arguments

**databaseName**: `String` -
projects/PROJECT_ID/instances/INSTANCE_ID/databases/DATABASE_ID

**matchingConfig**: `Container` - the defined matching criteria for extracting
property-values

**resource**: `Container` - the FHIR resource

#### Description

replaceAndAssignStableId takes a FHIR resource, removes extra
property-values(ie: stableId) and extracts property-values to be used as
matching criteria, queries these in the Stable ID index and either returns a
match or a new UUID. The property values from the resource will be added to the
spanner table. If there are no property values, Resource ID will be assigned as
the Stable ID.

### replaceReferencesWithStableId

`recon::replaceReferencesWithStableId(resource: Container, referenceType:
String, historicalStableId: String, finalStableId: String)` returns `Data` -
resource with the final stable id in its references.

#### Arguments

**resource**: `Container` - the FHIR resource

**referenceType**: `String` - the resourceType of the reference

**historicalStableId**: `String` - the historical stable id

**finalStableId**: `String` - the final stable id

#### Description

replaceReferencesWithStableId takes a FHIR resource and replaces references with
the historical stable id to {resourceType}/{finalStableId}.

### replaceStableId

`recon::replaceStableId(resource: Container, resourceType: String,
historicalStableId: String, finalStableId: String)` returns `Data` - resource
with the final stable id in its identifiers.

#### Arguments

**resource**: `Container` - the FHIR resource.

**resourceType**: `String` - the resourceType.

**historicalStableId**: `String` - the historical stable id.

**finalStableId**: `String` - the final stable id.

#### Description

replaceStableId takes a FHIR resource and replaces its stable id with the given
stable id.

### searchAndResolveReverseCaveatCase

`recon::searchAndResolveReverseCaveatCase(databaseName: String,
stableIdContainer: Data)` returns `Data` - Array of reverse caveat case info
containers with fields (resourceType, resourceId, stableIdResourceType,
historicalStableId, finalStableId).

#### Arguments

**databaseName**: `String` -
projects/PROJECT_ID/instances/INSTANCE_ID/databases/DATABASE_ID.

**stableIdContainer**: `Data` - container of stable id and resourceType.

#### Description

searchAndResolveReverseCaveatCase takes in a stable id container with fields
(stableId, resourceType), checks whether there is a reverse caveat case needed
to be handled, and updates stableId to resolve reverse caveat case.

#### Throws

*   **`IllegalArgumentException`** - when input arguments are invalid.
*   **`StableIdRollbackManagerException`** - when spanner transaction fails.

### shouldAssignStableIdToIdentifier

`recon::shouldAssignStableIdToIdentifier(resourceType: String)` returns `Data` -
A boolean, indicating whether or not the resource type supports the identifier
field for stable ID assignment.

#### Arguments

**resourceType**: `String`

#### Description

shouldAssignStableIdToIdentifier returns true if the given resource type
supports identifier field for stable ID assignment.

### union

`recon::union(existing: Data, inbound: Data)` returns `Array` - Data merge
result.

#### Arguments

**existing**: `Data` - Data Array field value from the older resource to merge.

**inbound**: `Data` - Data Array field value from the more recent resource to
merge.

#### Description

Merges two Array field values from an existing and an inbound resource,
returning the union of the two values. Implements a version of the "Union of ER
and IR" rule from go/fhir-reconciliation-rules.

### unionByField

`recon::unionByField(existing: Data, inbound: Data, firstFieldPath: String,
restFieldPaths: String...)` returns `Array` - Data merge result.

#### Arguments

**existing**: `Data` - Data Array field value from the older resource to merge.

**inbound**: `Data` - Data Array field value from the more recent resource to
merge.

**firstFieldPath**: `String` - Data Primitive string representing the relative
JSON path within each entry of 'existing' and 'inbound' to extract keys from for
determining distinct entries.

**restFieldPaths**: `String...` - list of Data Primitive strings representing
relative JSON path within each entry of 'existing' and 'inbound' to extract keys
for determining distinct entries.

#### Description

Merges two Array field values from an existing and an inbound resource,
returning the union of the two values, where the provided JSON-type paths points
to fields within each Array element to use for determining distinctness within
the union operation. Implements a version of the "Union of ER and IR" rule from
go/fhir-reconciliation-rules.

### validateMatchingConfig

`recon::validateMatchingConfig(config: Container, resource: Data)` returns
`Container` - container of parsed property-values

#### Arguments

**config**: `Container` - the matching criteria config

**resource**: `Data` - the FHIR resource

#### Description

validateMatchingConfig is a helper function ONLY FOR testing a matching criteria
and an input resource which returns a string description of the results.
