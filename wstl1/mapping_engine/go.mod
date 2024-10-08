module github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine

go 1.14

replace github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language => ../mapping_language

replace github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto => ./proto

replace github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util => ./util

require (
	bitbucket.org/creachadair/stringset v0.0.9
	github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util v0.0.0-00010101000000-000000000000
	github.com/google/go-cmp v0.5.9
	github.com/google/uuid v1.3.0
	golang.org/x/oauth2 v0.7.0
	google.golang.org/grpc v1.56.3
	google.golang.org/protobuf v1.30.0
)
