module github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language

go 1.14

replace github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto => ../mapping_engine/proto

replace github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util => ../mapping_engine/util

replace github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language/transpiler => ./transpiler

require github.com/antlr/antlr4 v0.0.0-20210203043838-a60c32d36933
