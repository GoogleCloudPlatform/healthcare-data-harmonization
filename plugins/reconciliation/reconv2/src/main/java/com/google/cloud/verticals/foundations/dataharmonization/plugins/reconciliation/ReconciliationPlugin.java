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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation;

import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.FunctionCollectionBuilder;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Plugin;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConfigExecutorImpl;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeFunctionFactoryImpl;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeResources;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeResultAnnotator;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.defaults.DefaultMergeRuleFactory;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.defaults.DefaultMergerFactory;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.defaults.MergedChoiceFieldFactory;
import java.util.List;

/** A plugin that enables users to reconcile FHIR resources by matching and merging. */
public class ReconciliationPlugin implements Plugin {
  public static final String PACKAGE_NAME = "recon";

  public ReconciliationPlugin() {}

  @Override
  public String getPackageName() {
    return PACKAGE_NAME;
  }

  @Override
  public List<CallableFunction> getFunctions() {
    FunctionCollectionBuilder reconFuncListBuilder = new FunctionCollectionBuilder(PACKAGE_NAME);
    reconFuncListBuilder.addAllJavaPluginFunctionsInClass(MergingPlugin.class);
    reconFuncListBuilder.addAllJavaPluginFunctionsInClass(MatchingPlugin.class);

    MergeResultAnnotator mergeResultAnnotator = new MergeResultAnnotator();
    MergeResources merger =
        new MergeResources(
            new MergeConfigExecutorImpl(new MergeFunctionFactoryImpl(mergeResultAnnotator)),
            new DefaultMergerFactory(
                new DefaultMergeRuleFactory(mergeResultAnnotator),
                new MergedChoiceFieldFactory(mergeResultAnnotator)));
    reconFuncListBuilder.addAllJavaPluginFunctionsInInstance(merger);

    return reconFuncListBuilder.build();
  }
}
