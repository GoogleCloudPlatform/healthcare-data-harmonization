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

## install python language plugin for protoc
readonly API_COMMON_PROTOS='../../third_party/api-common-protos'
rm -rf $API_COMMON_PROTOS
git clone https://github.com/googleapis/api-common-protos $API_COMMON_PROTOS
python -m pip install grpcio==1.29.0 --ignore-installed
python -m pip install grpcio-tools==1.29.0 setuptools==49.1.0
./gen_protos.sh
python setup.py sdist bdist_wheel
