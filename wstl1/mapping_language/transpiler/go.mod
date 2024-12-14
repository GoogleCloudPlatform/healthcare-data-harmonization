module github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language/transpiler

go 1.14

replace github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language => ../

replace github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto => ../../mapping_engine/proto

replace github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util => ../../mapping_engine/util

require (
	bitbucket.org/creachadair/stringset v0.0.9
	github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto v0.0.0-00010101000000-000000000000
	github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util v0.0.0-00010101000000-000000000000
	github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language v0.0.0-00010101000000-000000000000
	github.com/antlr/antlr4 v0.0.0-20210203043838-a60c32d36933
	github.com/google/go-cmp v0.5.4
	google.golang.org/protobuf v1.25.0
)
