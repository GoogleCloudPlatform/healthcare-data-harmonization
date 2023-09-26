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
	"strings"
	"testing"

	"github.com/google/go-cmp/cmp" /* copybara-comment: cmp */
	"github.com/google/go-cmp/cmp/cmpopts" /* copybara-comment: cmpopts */
	"google.golang.org/grpc/codes" /* copybara-comment: codes */
	"google.golang.org/grpc/status" /* copybara-comment: status */
	"google.golang.org/protobuf/testing/protocmp" /* copybara-comment: protocmp */

	spb "google.golang.org/genproto/googleapis/rpc/status"
	wspb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/tools/notebook/extensions/wstl/proto" /* copybara-comment: wstlservice_go_proto */
)

func TestValidate_SingleInput(t *testing.T) {
	tests := []struct {
		name string
		req  *wspb.ValidationRequest
		want *wspb.ValidationResponse
	}{
		{
			name: "Validate STU3 acronym field",
			req:  requestWrapper([]string{`{"id":"example","resourceType":"3","udi":{"carrierHRF":"test"}}`}, wspb.ValidationRequest_FHIR_VERSION_UNSPECIFIED, false),
			want: &wspb.ValidationResponse{
				Status: []*spb.Status{newStatusWithDetails(codes.InvalidArgument, "invalid FHIR resource", "error at \"3\": unknown resource type")},
			},
		},
		{
			name: "Validate empty json",
			req:  requestWrapper([]string{``}, wspb.ValidationRequest_FHIR_VERSION_UNSPECIFIED, false),
			want: &wspb.ValidationResponse{
				Status: []*spb.Status{newStatusWithDetails(codes.InvalidArgument, "invalid FHIR resource", "invalid JSON")},
			},
		},
		{
			name: "Validate STU3 acronym field",
			req:  requestWrapper([]string{`{"id":"example","resourceType":"Device","udi":{"carrierHRF":"test"}}`}, wspb.ValidationRequest_STU3, false),
			want: &wspb.ValidationResponse{
				Status: []*spb.Status{newStatusWithDetails(codes.OK, "Validation Success", "")},
			},
		},
		{
			name: "Validate STU3 device resource with R4-only field",
			req:  requestWrapper([]string{`{"id":"example","resourceType":"Device","deviceName":[{"name":"deviceName","code":"other"}]}`}, wspb.ValidationRequest_STU3, false),
			want: &wspb.ValidationResponse{
				Status: []*spb.Status{newStatusWithDetails(codes.InvalidArgument, "invalid FHIR resource", "error at \"Device\": unknown field")},
			},
		},
		{
			name: "Validate R4 device resource with R4-only field",
			req:  requestWrapper([]string{`{"id":"example","resourceType":"Device","deviceName":[{"name":"deviceName","type":"other"}]}`}, wspb.ValidationRequest_R4, false),
			want: &wspb.ValidationResponse{
				Status: []*spb.Status{newStatusWithDetails(codes.OK, "Validation Success", "")},
			},
		},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := NewValidator().Validate(context.Background(), test.req)
			if err != nil {
				t.Fatalf("unexpected error")
			}
			if diff := cmp.Diff(test.want, got, protocmp.Transform()); diff != "" {
				t.Fatalf("Diff occurred on test case %v:\n returned diff (-want +got):\n%v ",
					test.name, diff)
			}
		})
	}
}

func TestValidate_SingleInputMultipleErrors(t *testing.T) {
	tests := []struct {
		name string
		req  *wspb.ValidationRequest
		want *wspb.ValidationResponse
	}{
		{
			name: "Validate multiple validation errors",
			req:  requestWrapper([]string{`{"id":"example","resourceType":"Device","status":"badStatus","udi":{"unknownSubField":"foo"}}`}, wspb.ValidationRequest_STU3, false),
			want: &wspb.ValidationResponse{
				Status: []*spb.Status{newStatusWithDetails(
					codes.InvalidArgument,
					"invalid FHIR resource",
					"error at \"Device.status\": code type mismatch\nerror at \"Device.udi\": unknown field")},
			},
		},
		{
			name: "Validate multiple validation errors with duplicate type",
			req:  requestWrapper([]string{`{"id":"example","resourceType":"Device","status":"badStatus","udi":{"unknownSubField1":"foo","unknownSubField2":"foo"}}`}, wspb.ValidationRequest_STU3, false),
			want: &wspb.ValidationResponse{
				Status: []*spb.Status{newStatusWithDetails(
					codes.InvalidArgument,
					"invalid FHIR resource",
					"error at \"Device.status\": code type mismatch\nerror at \"Device.udi\": unknown field\nerror at \"Device.udi\": unknown field")},
			},
		},
	}

	// The Status.message in the Details does not have deterministic ordering of
	// the individual error messages, so we use a sorting comparison option.
	statusMsgComparer := cmp.Comparer(func(a, b string) bool {
		as := strings.Split(a, "\n")
		bs := strings.Split(b, "\n")
		return cmp.Equal(as, bs, cmpopts.SortSlices(func(a, b string) bool { return a < b }))
	})

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := NewValidator().Validate(context.Background(), test.req)
			if err != nil {
				t.Fatalf("unexpected error")
			}
			if diff := cmp.Diff(test.want, got, protocmp.Transform(),
				protocmp.FilterField(&spb.Status{}, "message", statusMsgComparer)); diff != "" {
				t.Fatalf("Diff occurred on test case %v:\n returned diff (-want +got):\n%v ", test.name, diff)
			}
		})
	}
}

func TestValidate_MultiInput(t *testing.T) {
	tests := []struct {
		name string
		req  *wspb.ValidationRequest
		want *wspb.ValidationResponse
	}{
		{
			name: "Validate multiple in-line JSON with STU3",
			req: requestWrapper([]string{
				`{"id":"example","resourceType":"3","udi":{"carrierHRF":"test"}}`,
				``,
				`{"id":"example","resourceType":"Device","udi":{"carrierHRF":"test"}}`,
			}, wspb.ValidationRequest_STU3, false),
			want: &wspb.ValidationResponse{
				Status: []*spb.Status{
					newStatusWithDetails(codes.InvalidArgument, "invalid FHIR resource", "error at \"3\": unknown resource type"),
					newStatusWithDetails(codes.InvalidArgument, "invalid FHIR resource", "invalid JSON"),
					newStatusWithDetails(codes.OK, "Validation Success", ""),
				},
			},
		},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := NewValidator().Validate(context.Background(), test.req)
			if err != nil {
				t.Fatalf("unexpected error: %v", err)
			}
			if diff := cmp.Diff(test.want, got, protocmp.Transform()); diff != "" {
				t.Fatalf("Diff occurred on test case %v:\n returned diff (-want +got):\n%v ",
					test.name, diff)
			}
		})
	}
}

// We do want to see error in this test.
func TestValidate_UnimplementedAndInvalid(t *testing.T) {
	tests := []struct {
		name       string
		req        *wspb.ValidationRequest
		wantErrKey string
	}{
		{
			name:       "Validate empty gcs",
			req:        requestWrapper([]string{``}, wspb.ValidationRequest_FHIR_VERSION_UNSPECIFIED, true),
			wantErrKey: "not implemented yet",
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := NewValidator().Validate(context.Background(), test.req)
			if err == nil {
				t.Fatalf("expected error but not seen")
			}
			if !strings.Contains(err.Error(), test.wantErrKey) {
				t.Errorf("got error %v, expected error %v", err, test.wantErrKey)
			}
			if got != nil {
				t.Errorf("validate with gcs input source should return nil response, but got response %v", got)
			}
		})
	}
}

func TestValidate_Wstlservice(t *testing.T) {
	tests := []struct {
		name string
		req  *wspb.ValidationRequest
		want *wspb.ValidationResponse
	}{
		{
			name: "Validate STU3 acronym field",
			req:  requestWrapper([]string{`{"id":"example","resourceType":"3","udi":{"carrierHRF":"test"}}`}, wspb.ValidationRequest_STU3, false),
			want: &wspb.ValidationResponse{
				Status: []*spb.Status{newStatusWithDetails(codes.InvalidArgument, "invalid FHIR resource", "error at \"3\": unknown resource type")},
			},
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := NewWstlServiceServer().FhirValidate(context.Background(), test.req)
			if err != nil {
				t.Fatalf("unexpected error")
			}
			if diff := cmp.Diff(test.want, got, protocmp.Transform()); diff != "" {
				t.Fatalf("Diff occurred on test case %v:\n returned diff (-want +got):\n%v ",
					test.name, diff)
			}
		})
	}
}

func newStatusWithDetails(code codes.Code, message string, details string) *spb.Status {
	st := status.New(code, message)
	if details != "" {
		dStatus := status.New(code, details)
		var err error
		st, err = st.WithDetails(dStatus.Proto())
		if err != nil {
			return nil
		}
	}
	return st.Proto()
}

func requestWrapper(jsons []string, ver wspb.ValidationRequest_FhirVersion, gcs bool) *wspb.ValidationRequest {
	var hldr []*wspb.Location
	if !gcs {
		for _, json := range jsons {
			hldr = append(hldr, &wspb.Location{
				Location: &wspb.Location_InlineJson{
					InlineJson: json,
				},
			})
		}
	} else {
		for _, json := range jsons {
			hldr = append(hldr, &wspb.Location{
				Location: &wspb.Location_GcsLocation{
					GcsLocation: json,
				},
			})
		}
	}
	return &wspb.ValidationRequest{
		FhirVersion: ver,
		Input:       hldr,
	}
}
