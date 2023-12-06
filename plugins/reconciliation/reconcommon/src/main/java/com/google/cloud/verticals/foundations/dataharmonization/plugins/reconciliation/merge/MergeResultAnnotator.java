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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge;

import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.REMOVE_FIELD_PLACEHOLDER;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import java.io.Serializable;

/** MergeResultAnnotator applies an annotation suffix to the result of merges. */
public class MergeResultAnnotator implements Serializable {

  // private static final String ENABLE_DEBUG_MERGE_ANNOTATION_KEY =
  // "ENABLE_DEBUG_MERGE_ANNOTATION";

  /**
   * Adds a debug annotation to the values in the given data.
   *
   * <p>All primitives will be converted to a string with the annotation suffix appended.
   *
   * <p>eg. primitive(2) -> primitive("2 [MergeFunction]")
   *
   * <p>Annotations are applied to every value in an array and recursively applied to field values
   * in a container.
   *
   * <p>No annotations are applied if ENABLE_DEBUG_MERGE_ANNOTATION is not set.
   *
   * @param ctx The runtime context.
   * @param functionName The function name to use in the annotation suffix
   * @param data The data to annotate.
   */
  public Data annotate(RuntimeContext ctx, String functionName, Data data) {
    if (!isEnabled()) {
      return data;
    }
    return addAnnotation(ctx, functionName, data);
  }

  private Data addAnnotation(RuntimeContext ctx, String functionName, Data data) {
    if (data.isPrimitive()) {
      return ctx.getDataTypeImplementation()
          .primitiveOf(annotateString(functionName, data.toString()));
    } else if (data.isArray()) {
      return ctx.getDataTypeImplementation()
          .arrayOf(
              data.asArray().stream()
                  .map(v -> addAnnotation(ctx, functionName, v))
                  .collect(toImmutableList()));
    }

    Container container = data.deepCopy().asContainer();
    if (container.equals(REMOVE_FIELD_PLACEHOLDER)) {
      return container;
    }

    for (String field : container.fields()) {
      Data value = container.getField(field);
      container.setField(field, addAnnotation(ctx, functionName, value));
    }
    return container;
  }

  private String annotateString(String functionName, String value) {
    String annotation = String.format("[%s]", functionName);
    if (value.endsWith(annotation)) {
      return value;
    }
    return value + " " + annotation;
  }

  private boolean isEnabled() {
    // TODO() Refactor unused reconciliation functions under Merging plugin.
    // Data enableDebugMergeAnnotation =
    //     EnvironmentValues.getEnvironmentValue(ctx, ENABLE_DEBUG_MERGE_ANNOTATION_KEY);
    // if (enableDebugMergeAnnotation.isNullOrEmpty() || !enableDebugMergeAnnotation.isPrimitive())
    // {
    //   return false;
    // }
    // return enableDebugMergeAnnotation.asPrimitive().bool();
    return false;
  }
}
