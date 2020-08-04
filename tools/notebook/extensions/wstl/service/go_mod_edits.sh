#!/bin/bash
# Copyright 2020 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http:#www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# This script replaces go module dependencies with local paths. It makes the
# assumption that the mapping_engine and the mapping_language are stored in the
# same directory and their directory names are mapping_engine and
# mapping_language.

function abspath() {
  pushd $1 > /dev/null
  base=$(pwd)
  popd > /dev/null
  echo $base
}


function replace_modules() {
  # TODO  clean up relative paths in go_mod_edits.sh
  DH_ROOT=$(abspath ../../../../../)
  go mod edit -replace github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto=$DH_ROOT/mapping_engine/proto
  go mod edit -replace github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util=$DH_ROOT/mapping_engine/util
  go mod edit -replace github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/transform=$DH_ROOT/mapping_engine/transform
  go mod edit -replace github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language=$DH_ROOT/mapping_language
  go mod edit -replace github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language/transpiler=$DH_ROOT/mapping_language/transpiler
  go mod edit -replace github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine=$DH_ROOT/mapping_engine
  go mod edit -replace github.com/GoogleCloudPlatform/healthcare-data-harmonization/tools/notebook/extensions/wstl/proto=$DH_ROOT/tools/notebook/extensions/wstl/proto
}

replace_modules
