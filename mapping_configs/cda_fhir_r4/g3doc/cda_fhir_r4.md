# CDA to FHIR R4

### Generating document bundles

By default, CDA to FHIR mappings will output a 'transaction' type bundle. To
generate a 'document' type bundle, change `configurations/main.wstl` to call
`Output_Document` instead of `Output_Bundle`.
