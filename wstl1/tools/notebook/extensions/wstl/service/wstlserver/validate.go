// Copyright 2020 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package wstlserver

import (
	"context"

	"google3/third_party/fhir/go/fhirversion" /* copybara-comment: fhirversion */
	"github.com/google/fhir/go/jsonformat" /* copybara-comment: jsonformat */
	"google.golang.org/grpc/codes" /* copybara-comment: codes */
	"google.golang.org/grpc/status" /* copybara-comment: status */

	spb "google.golang.org/genproto/googleapis/rpc/status"
	wspb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/tools/notebook/extensions/wstl/proto" /* copybara-comment: wstlservice_go_proto */
)

// Validator initiates validation agains a FHIR version.
type Validator struct {
}

// NewValidator instantiates and initializes the validator.
func NewValidator() *Validator {
	return &Validator{}
}

// Validate returns the validation result of the json object. Returns an error if
// any error is thrown by FhirProto.
func (v *Validator) Validate(ctx context.Context, req *wspb.ValidationRequest) (*wspb.ValidationResponse, error) {
	if req == nil || len(req.GetInput()) == 0 {
		return nil, status.Error(codes.InvalidArgument, "no resource provided")
	}
	var version fhirversion.Version
	switch req.GetFhirVersion() {
	case wspb.ValidationRequest_FHIR_VERSION_UNSPECIFIED:
		version = fhirversion.R4
	case wspb.ValidationRequest_R4:
		version = fhirversion.R4
	case wspb.ValidationRequest_STU3:
		version = fhirversion.STU3
	}
	unMar, err := jsonformat.NewUnmarshaller("UTC", version)

	inSrc := req.GetInput()
	var res []*spb.Status
	if err != nil {
		return nil, status.Error(codes.Internal, err.Error())
	}
	for _, src := range inSrc {
		switch s := src.GetLocation().(type) {
		case *wspb.Location_GcsLocation:
			return nil, status.Error(codes.Unimplemented, "GCS source not implemented yet")
		case *wspb.Location_InlineJson:
			_, err := unMar.Unmarshal([]byte(s.InlineJson))
			if err != nil {
				rStatus := status.New(codes.InvalidArgument, "invalid FHIR resource")
				details := status.New(codes.InvalidArgument, err.Error())
				if dStatus, dErr := rStatus.WithDetails(details.Proto()); dErr == nil {
					res = append(res, dStatus.Proto())
				} else {
					res = append(res, rStatus.Proto())
				}
			} else {
				res = append(res, status.New(codes.OK, "Validation Success").Proto())
			}
		default:
			return nil, status.Error(codes.InvalidArgument, "Invalid resource type")
		}
	}
	return &wspb.ValidationResponse{Status: res}, nil
}
