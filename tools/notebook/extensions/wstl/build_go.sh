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

# This script installs the necessary dependency for, builds and tests the backend
# server for jupyter toolings.

export GO111MODULE=on
echo 'Installing protoc and go-grpc.'
# pinning protoc version
go get -u google.golang.org/protobuf/cmd/protoc-gen-go@v1.25.0
# pinning protoc golang grpc plugin version
go get -u google.golang.org/grpc/cmd/protoc-gen-go-grpc@v0.0.0-20200822010404-0e72e09474d6
# in service/go.mod pin the version of grpc dependency

# install protoc
echo 'Installing protoc.'
PB_REL="https://github.com/protocolbuffers/protobuf/releases"
wget -P /tmp https://github.com/protocolbuffers/protobuf/releases/download/v3.12.1/protoc-3.12.1-linux-x86_64.zip
unzip /tmp/protoc-3.12.1-linux-x86_64.zip -d $HOME/.local
export PATH="$PATH:$HOME/.local/bin"


# install go language plugin for protoc
TOOL_DIR=$PWD
echo 'Installing golang plugin for protoc.'
rm -rf ../../../third_party/api-common-protos
rm -rf ../../../third_party/grpc-go
git clone https://github.com/googleapis/api-common-protos ../../../third_party/api-common-protos
cd $TOOL_DIR

# build mapping engine
echo 'Building mapping engine dependency for wstlserver.'
cd ../../../../mapping_engine
source ./generate_protos.sh
cd ../mapping_language
source ./generate_grammar.sh
cd $TOOL_DIR

echo 'Compiling protobuf.'
source ./gen_go_protos.sh

# build wstl server
echo Building wstlserver
cd service/wstlserver
go build ./...
go test ./...

# echo Starting wstlserver
cd ../main
go build ./...
go test ./...
