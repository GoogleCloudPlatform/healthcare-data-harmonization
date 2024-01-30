### bigQueryIO
`bigQueryIO(projectId: String, datasetId: String, tableId: String, selectedCols: Array)` returns `Dataset` - DACP object representing the rows of given BigQuery table.

#### Arguments
**projectId**: `String` - - The Google Cloud Project ID. If empty will be replaced with the default value from the pipeline options object.


**datasetId**: `String` - - The BigQuery dataset ID.


**tableId**: `String` - - The BigQuery table ID.


**selectedCols**: `Array` - - The list of selected columns to retrieve from the BigQuery table.


#### Description
Reads records from a BigQuery Table.

