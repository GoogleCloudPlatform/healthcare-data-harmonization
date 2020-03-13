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

function replace_modules() {
  go mod edit -replace github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_language=$(realpath ../)/mapping_language
  go mod edit -replace github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/proto=$(realpath ./)/proto
  go mod edit -replace github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/util=$(realpath ./)/util
  go mod edit -replace github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/transform=$(realpath ./)/transform

  cd main
  go mod edit -replace github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_language=$(realpath ../../)/mapping_language
  go mod edit -replace github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/proto=$(realpath ../)/proto
  go mod edit -replace github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/util=$(realpath ../)/util
  go mod edit -replace github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/transform=$(realpath ../)/transform
  go mod edit -replace github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine=$(realpath ../)
  cd ../

  cd transform
  go mod edit -replace github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_language=$(realpath ../../)/mapping_language
  go mod edit -replace github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/proto=$(realpath ../)/proto
  go mod edit -replace github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/util=$(realpath ../)/util
  go mod edit -replace github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine=$(realpath ../)
  cd ../
}

replace_modules
