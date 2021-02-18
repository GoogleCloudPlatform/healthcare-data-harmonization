module github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/transform

go 1.14

replace github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language => ../../mapping_language

replace github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language/transpiler => ../../mapping_language/transpiler

replace github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto => ../proto

replace github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util => ../util

replace github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine => ../

require (
    github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine v0.0.0-00010101000000-000000000000
    github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto v0.0.0-00010101000000-000000000000
    github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util v0.0.0-00010101000000-000000000000
    github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language/transpiler v0.0.0-00010101000000-000000000000
    github.com/google/go-cmp v0.5.4
    google.golang.org/protobuf v1.25.0
)
