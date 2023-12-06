/*
 * Copyright 2021 Google LLC.
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
package com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.defaults;

import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.DEFAULT_FIELD_RULES_METHOD;

import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeResultAnnotator;
import java.io.Serializable;

/** MergedChoiceField is a utility class to capture the result of merged choice fields */
public class MergedChoiceField implements Serializable {
  private final String field;
  private final Data value;

  private MergedChoiceField(String field, Data value) {
    this.field = field;
    this.value = value;
  }

  static MergedChoiceField of(
      RuntimeContext ctx, MergeResultAnnotator annotator, Container mergeResult) {
    // Result pair from choiceField builtin returns {fieldName: fieldValue} singleton Container.
    String resultantChoiceField = mergeResult.fields().stream().findFirst().orElse("");
    Data choiceFieldValue =
        annotator.annotate(
            ctx, DEFAULT_FIELD_RULES_METHOD, mergeResult.getField(resultantChoiceField));
    return new MergedChoiceField(resultantChoiceField, choiceFieldValue);
  }

  String getField() {
    return field;
  }

  Data getValue() {
    return value;
  }
}
