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
package com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge;

import com.google.cloud.verticals.foundations.dataharmonization.exceptions.NoMatchingOverloadsException;
import java.util.HashMap;
import java.util.Map;

class TestMergeFunctionFactory implements MergeFunctionFactory {

  private final Map<String, MergeResourcesFunc> mergeFuncs = new HashMap<>();
  private final Map<String, MergeRuleFunc> mergeRuleFuncs = new HashMap<>();

  @Override
  public MergeResourcesFunc createMergeFunc(String resourceType) {
    MergeResourcesFunc f = mergeFuncs.getOrDefault(resourceType, null);
    if (f == null) {
      return (ctx, existing, inbound) -> {
        throw new NoMatchingOverloadsException();
      };
    }
    return f;
  }

  @Override
  public MergeRuleFunc createMergeRuleFunc(String resourceType) {
    MergeRuleFunc f = mergeRuleFuncs.getOrDefault(resourceType, null);
    if (f == null) {
      return ctx -> {
        throw new NoMatchingOverloadsException();
      };
    }
    return f;
  }

  public void addMergeFunc(String resourceType, MergeResourcesFunc f) {
    mergeFuncs.put(resourceType, f);
  }

  public void addMergeRuleFunc(String resourceType, MergeRuleFunc f) {
    mergeRuleFuncs.put(resourceType, f);
  }
}
