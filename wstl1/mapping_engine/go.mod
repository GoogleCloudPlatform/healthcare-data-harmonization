module github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine

go 1.14

replace github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language => ../mapping_language

replace github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto => ./proto

replace github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util => ./util

require (
	bitbucket.org/creachadair/stringset v0.0.9
	github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto v0.0.0-00010101000000-000000000000
	github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util v0.0.0-00010101000000-000000000000
	github.com/google/go-cmp v0.5.0
	github.com/google/uuid v1.2.0
	golang.org/x/oauth2 v0.0.0-20200107190931-bf48bf16ab8d
	google.golang.org/grpc v1.27.1
	google.golang.org/protobuf v1.25.0
)
