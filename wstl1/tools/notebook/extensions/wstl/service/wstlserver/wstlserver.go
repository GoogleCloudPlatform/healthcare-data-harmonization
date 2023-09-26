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

// Package wstlserver is the Whistle Mapping Language gRPC server.
package wstlserver

import (
	"context"

	"google.golang.org/grpc/codes" /* copybara-comment: codes */
	"google.golang.org/grpc/status" /* copybara-comment: status */

	wspb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/tools/notebook/extensions/wstl/proto" /* copybara-comment: wstlservice_go_proto */
	wsgrpc "github.com/GoogleCloudPlatform/healthcare-data-harmonization/tools/notebook/extensions/wstl/proto" /* copybara-comment: wstlservice_proto_grpc */
)

// NewWstlServiceServer instantiates a new WstlServiceServer
func NewWstlServiceServer() *WstlServiceServer {
	return &WstlServiceServer{
		env: NewEnvironment(),
		val: NewValidator(),
	}
}

// WstlServiceServer implements the wsgrpc.WhistleServiceServer interface
type WstlServiceServer struct {
	wsgrpc.WhistleServiceServer
	env *Environment
	val *Validator
}

// GetOrCreateIncrementalSession returns an incremental transformation session.
func (s *WstlServiceServer) GetOrCreateIncrementalSession(ctx context.Context, request *wspb.CreateIncrementalSessionRequest) (*wspb.IncrementalSessionResponse, error) {
	if s.env.SessionExists(request.GetSessionId()) {
		return &wspb.IncrementalSessionResponse{
			Status: &wspb.IncrementalSessionResponse_SessionId{
				SessionId: request.GetSessionId(),
			}}, nil
	}
	sess, err := s.env.CreateSession(request.GetSessionId())
	if err != nil {
		return nil, status.Error(codes.FailedPrecondition, err.Error())
	}

	return &wspb.IncrementalSessionResponse{Status: &wspb.IncrementalSessionResponse_SessionId{
		SessionId: sess.ID(),
	}}, nil
}

// GetIncrementalTransform returns an incremetal transformation within an existing session. All
// functions defined in the incremental transformation will be accessible
// by subsequent incremental transformation calls.
func (s *WstlServiceServer) GetIncrementalTransform(ctx context.Context, request *wspb.IncrementalTransformRequest) (*wspb.TransformResponse, error) {
	sess, err := s.env.Session(request.GetSessionId())
	if err != nil {
		return nil, status.Errorf(codes.InvalidArgument, "failed to retrieve session %q due to error: %v", request.GetSessionId(), err.Error())
	}

	results, err := sess.Context.EvaluateIncrementalTransformation(request)
	if err != nil {
		return nil, status.Errorf(codes.FailedPrecondition, "tranformation failed with err: %q", err.Error())
	}

	return &wspb.TransformResponse{Results: results}, nil
}

// DeleteIncrementalSession deletes an incremental transformation session.
func (s *WstlServiceServer) DeleteIncrementalSession(ctx context.Context, request *wspb.DeleteIncrementalSessionRequest) (*wspb.DeleteIncrementalSessionResponse, error) {
	err := s.env.DestroySession(request.GetSessionId())
	if err != nil {
		return nil, status.Error(codes.FailedPrecondition, err.Error())
	}

	return &wspb.DeleteIncrementalSessionResponse{
		Status: &wspb.DeleteIncrementalSessionResponse_SessionId{
			SessionId: request.GetSessionId(),
		}}, nil
}

// Validate the FHIR sources with the FHIR version in ValidationRequest. The default version is STU3.
func (s *WstlServiceServer) FhirValidate(ctx context.Context, req *wspb.ValidationRequest) (*wspb.ValidationResponse, error) {
	if s.val == nil {
		s.val = NewValidator()
	}
	return s.val.Validate(ctx, req)
}
