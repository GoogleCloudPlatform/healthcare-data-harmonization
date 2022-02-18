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

# This script will run code generation for the ANTLR grammar.

ANTLR_DIR=${ANTLR_DIR:-"./.antlr"}
ANTLR_JAR=${ANTLR_JAR:-"antlr.jar"}
ANTLR_DEFAULT_PATH="$ANTLR_DIR/$ANTLR_JAR"
ANTLR_PATH=${ANTLR_PATH:-$ANTLR_DEFAULT_PATH}

function generate_grammar() {
  mkdir -p $ANTLR_DIR

  if [ ! -f "$ANTLR_PATH" ]; then
    echo "Downloading ANTLR 4.7.1 to $ANTLR_PATH"
    curl -o "$ANTLR_PATH" https://www.antlr.org/download/antlr-4.7.1-complete.jar
  fi

  java \
    -Xmx500M \
    -cp "$ANTLR_PATH" \
    org.antlr.v4.Tool \
   -Dlanguage=Go \
   -visitor \
   -listener \
   -package parser \
   ./parser/Whistle.g4 \
   -o ./

   rm ./parser/*.tokens
   rm ./parser/*.interp

}

generate_grammar
