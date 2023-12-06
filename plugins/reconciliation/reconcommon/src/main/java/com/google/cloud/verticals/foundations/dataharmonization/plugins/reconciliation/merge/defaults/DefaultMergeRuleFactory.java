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

import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeResultAnnotator;
import java.io.Serializable;
import javax.naming.ConfigurationException;

/** DefaultMergeRuleFactory is a factory for creating DefaultMergeRule instances. */
public class DefaultMergeRuleFactory implements Serializable {
  private final MergeResultAnnotator annotator;

  public DefaultMergeRuleFactory(MergeResultAnnotator annotator) {
    this.annotator = annotator;
  }

  /**
   * Creates a default merge rule
   *
   * @param existing A {@link Container} representing the existing resource in the merge
   * @param inbound A {@link Container} representing the inbound resource in the merge
   * @param field The name of the field on which to perform the merge
   * @param fieldType The type of the field on which the merge is to be performed
   * @param defaultFieldMergeRules A {@link Container} which contains the defualt field merge rules.
   * @return A {@link DefaultMergeRule} instance which can perform default merges.
   */
  public DefaultMergeRule create(
      Container existing,
      Container inbound,
      String field,
      String fieldType,
      Container defaultFieldMergeRules)
      throws ConfigurationException {
    return new DefaultMergeRule(
        annotator, existing, inbound, field, fieldType, defaultFieldMergeRules);
  }
}
