/*
 * Copyright 2022 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.verticals.foundations.dataharmonization.builtins.options.exception;

import com.google.cloud.verticals.foundations.dataharmonization.builtins.options.UniqueVarAndFieldsExperiment.TypeAndSource;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FileInfo;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.Source;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.ImportException;

/** Class to hold exceptions for non-unique var and field names in a config. */
public class VarAndFieldOptionException extends ImportException {
  public VarAndFieldOptionException(
      FileInfo fileInfo,
      String varFieldName,
      Source source,
      String type,
      TypeAndSource previouslyFoundTypeAndSource) {
    super(
        String.format(
            "Fields and variables cannot share the same name within the same context,"
                + " cannot name %s \"%s\" at %s:%s-%s:%s, previously found %s at"
                + " %s:%s-%s:%s",
            type,
            varFieldName,
            source.getStart().getLine(),
            source.getStart().getColumn(),
            source.getEnd().getLine(),
            source.getEnd().getColumn(),
            previouslyFoundTypeAndSource.getType(),
            previouslyFoundTypeAndSource.getSource().getStart().getLine(),
            previouslyFoundTypeAndSource.getSource().getStart().getColumn(),
            previouslyFoundTypeAndSource.getSource().getEnd().getLine(),
            previouslyFoundTypeAndSource.getSource().getEnd().getColumn()),
        source,
        fileInfo.getUrl());
  }
}
