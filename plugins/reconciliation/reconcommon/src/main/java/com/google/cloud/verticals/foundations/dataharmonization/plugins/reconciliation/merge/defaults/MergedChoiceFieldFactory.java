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

import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.PREFER_INBOUND_RULE;

import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.ChoiceField;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeResultAnnotator;
import java.io.Serializable;
import java.util.Set;

/** A factory for creating MergeChoiceField instances. */
public class MergedChoiceFieldFactory implements Serializable {

  private final MergeResultAnnotator annotator;

  public MergedChoiceFieldFactory(MergeResultAnnotator annotator) {
    this.annotator = annotator;
  }

  /**
   * Create a MergeChoiceField instance, given existing and inbound resources and the set of choice
   * fields associated with the resource type.
   * @param context A {@link RuntimeContext} object used to perform the merge.
   * @param existing A {@link Container} representing the existing resource.
   * @param inbound A {@link Container} representing the inbound resource.
   * @param choiceFieldGroup A set of choice fields associated with the resource type.
   * @return A {@link MergedChoiceField} instance with which to perform choice field merges.
   */
  MergedChoiceField create(
      RuntimeContext context, Container existing, Container inbound, Set<String> choiceFieldGroup) {
    return MergedChoiceField.of(
        context,
        annotator,
        ChoiceField.choiceField(
                context,
                existing,
                inbound,
                PREFER_INBOUND_RULE,
                choiceFieldGroup.toArray(new String[0]))
            .asContainer());
  }
}
