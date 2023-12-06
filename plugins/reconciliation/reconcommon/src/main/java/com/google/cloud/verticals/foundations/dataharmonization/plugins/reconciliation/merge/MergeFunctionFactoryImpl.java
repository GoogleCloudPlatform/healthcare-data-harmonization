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

import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.MERGE_RULES_PACKAGE;

import com.google.cloud.verticals.foundations.dataharmonization.function.DefaultClosure;

/** Default implementation for MergeFunctionFactory */
public class MergeFunctionFactoryImpl implements MergeFunctionFactory {

  private final MergeResultAnnotator annotator;

  public MergeFunctionFactoryImpl(MergeResultAnnotator annotator) {
    this.annotator = annotator;
  }

  @Override
  public MergeResourcesFunc createMergeFunc(String resourceType) {
    String functionName = MergeResourcesFunc.getFunctionName(resourceType);
    return (ctx, existing, inbound) ->
        annotator.annotate(
            ctx,
            functionName,
            DefaultClosure.create(
                    new DefaultClosure.FunctionReference(
                        MERGE_RULES_PACKAGE, MergeResourcesFunc.getFunctionName(resourceType)),
                    existing,
                    inbound)
                .execute(ctx));
  }

  @Override
  public MergeRuleFunc createMergeRuleFunc(String resourceType) {
    return (ctx) ->
        DefaultClosure.create(
                new DefaultClosure.FunctionReference(
                    MERGE_RULES_PACKAGE, MergeRuleFunc.getFunctionName(resourceType)))
            .execute(ctx);
  }
}
