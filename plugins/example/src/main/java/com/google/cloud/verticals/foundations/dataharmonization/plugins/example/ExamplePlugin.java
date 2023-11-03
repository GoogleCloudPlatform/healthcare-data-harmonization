/*
 * Copyright 2021 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.verticals.foundations.dataharmonization.plugins.example;

import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Loader;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Parser;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.FunctionCollectionBuilder;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Plugin;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.example.functions.InstanceFns;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.example.functions.OverloadFns;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.example.functions.StaticFns;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.example.functions.VariadicFns;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.example.imports.ExampleLoader;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.example.imports.ExampleParser;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.example.io.ExampleServiceImpl;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.example.registries.ExamplePackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.example.targets.ExampleTarget;
import com.google.cloud.verticals.foundations.dataharmonization.registry.PackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target.Constructor;
import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 * This class returns all the things this Plugin provides. Things refer to parsers, loaders,
 * functions, etc.
 */
public class ExamplePlugin implements Plugin {
  public static final String NAME = "example";

  @Override
  public String getPackageName() {
    // When calling functions from this plugin, the user will prefix them with this.
    // E.g.: example::exampleFn(...)
    return NAME;
  }

  @Override
  public List<CallableFunction> getFunctions() {
    return new FunctionCollectionBuilder(NAME)
        // addAllJavaPluginFunctionsInClass is a convenience method for converting all the Java
        // native functions in the given class (annotated with @PluginFunction) into
        // CallableFunction.
        .addAllJavaPluginFunctionsInClass(StaticFns.class)
        .addAllJavaPluginFunctionsInClass(OverloadFns.class)
        .addAllJavaPluginFunctionsInClass(VariadicFns.class)
        // addAllJavaPluginFunctionInInstance only adds **instance** methods with @PluginFunction
        // annotation
        .addAllJavaPluginFunctionsInInstance(new InstanceFns(new ExampleServiceImpl()))
        // addAllJavaPluginFunctionInClass only adds **static** method with @PluginFunction
        // annotation
        .addAllJavaPluginFunctionsInClass(InstanceFns.class)
        // you need to do both if you have instance and static plugin method in the same class
        .build();
  }

  @Override
  public List<Constructor> getTargets() {
    // For targets, we return the Constructor instances directly.
    return ImmutableList.of(new ExampleTarget.Constructor());
  }

  @Override
  public List<Loader> getLoaders() {
    return ImmutableList.of(new ExampleLoader());
  }

  @Override
  public List<Parser> getParsers() {
    return ImmutableList.of(new ExampleParser());
  }

  @Override
  public PackageRegistry<CallableFunction> getFunctionRegistry() {

    // When this plugin loads, this package registry will be used for this "example" package instead
    // of the default package registry, DefaultPackageRegistry.
    return new ExamplePackageRegistry();
  }
}
