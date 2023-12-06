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

import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import java.io.Serializable;
import javax.naming.ConfigurationException;

/** DefaultMerger factory creates DefaultMerger instances. */
public class DefaultMergerFactory implements Serializable {

  private final DefaultMergeRuleFactory mergeRuleFactory;
  private final MergedChoiceFieldFactory mergedChoiceFieldFactory;

  public DefaultMergerFactory(
      DefaultMergeRuleFactory mergeRuleFactory, MergedChoiceFieldFactory mergedChoiceFieldFactory) {
    this.mergeRuleFactory = mergeRuleFactory;
    this.mergedChoiceFieldFactory = mergedChoiceFieldFactory;
  }
  /**
   * Builds a new DefaultMerger instance with the given {@link RuntimeContext}.
   *
   * @param context {@link RuntimeContext} for getting the configured Default field merge rules.
   * @return DefaultMerger instance.
   * @throws ConfigurationException when the DefaultFieldRules method is mis- or un-configured in
   *     the given {@link RuntimeContext}.
   */
  public DefaultMerger create(RuntimeContext context) throws ConfigurationException {
    return new DefaultMerger(
        new DefaultMergeRuleCreator(mergeRuleFactory, context), mergedChoiceFieldFactory);
  }
}
