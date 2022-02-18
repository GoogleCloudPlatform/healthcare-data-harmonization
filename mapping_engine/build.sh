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

# This script will run code generation and compile and test all go modules.

set -o errexit

PREREQUISITES=( protoc go )
PREREQUISITE_SOURCES=( "https://github.com/protocolbuffers/protobuf/releases" "https://golang.org/dl/" )
PREREQUISITE_VERSIONS=( "v3.11.4" "v1.14" )

function check_tools {
  LENGTH=${#PREREQUISITES[@]}
  for (( i=0; i<${LENGTH}; i++ ));
  do
    COMMAND=${PREREQUISITES[$i]}
    SOURCE=${PREREQUISITE_SOURCES[$i]}
    VERSION=${PREREQUISITE_VERSIONS[$i]}

    if ! [ -x "$(command -v $COMMAND)" ]; then
      echo "Error: $COMMAND is not available in the PATH. You can download it from $SOURCE (we recommend version $VERSION) if you do not have it installed." >&2
      exit 1
    fi
  done
}

function absfilepath() {
  pushd $(dirname $1) > /dev/null
  base=$(pwd)
  popd > /dev/null
  echo $base/$(basename $1)
}

function go_mod_command {
  START=$(pwd)
  find . -name "go.mod" -print0 | while read -d $'\0' f
  do
    echo Entering $(dirname $(absfilepath "$f"))
    cd $(dirname $(absfilepath "$f"))
    go $1 ./...
    cd $START
  done
}

check_tools

source ./generate_protos.sh

# BEGIN LOCAL-MOD-REPLACE
cd ../mapping_language
source ./generate_grammar.sh
cd ../mapping_engine
# END LOCAL-MOD-REPLACE

go_mod_command "build"
go_mod_command "test"
