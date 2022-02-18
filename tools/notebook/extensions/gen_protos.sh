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

# TODO (): generate client files with the appropriate import path.
python -m grpc_tools.protoc -I./wstl/proto -I../../third_party/api-common-protos --python_out=./wstl/proto --grpc_python_out=./wstl/proto wstl/proto/wstlservice.proto
sed -i 's/^import wstlservice_pb2 as wstlservice__pb2/import wstl.proto.wstlservice_pb2 as wstlservice__pb2/g' wstl/proto/wstlservice_pb2_grpc.py;
