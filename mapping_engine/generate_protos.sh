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

function generate_protos() {
  export GOPATH=$(go env GOPATH)
  export PATH=$GOPATH/bin:$PATH

  go get -u google.golang.org/protobuf/cmd/protoc-gen-go@v1.25.0

  find ./proto -type f -name "*.proto" -exec protoc --go_out=paths=source_relative:. {} \;
}

generate_protos
