#!/bin/bash
# Copyright 2020 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# This script will run code generation for protos.

protoc --go_out=./proto -I ../../../third_party/api-common-protos/ ../../../third_party/api-common-protos/google/rpc/status.proto
protoc --go_out=paths=source_relative:. --go-grpc_out=paths=source_relative:. -I ../../../third_party/api-common-protos/ -I ./ ./proto/wstlservice.proto
