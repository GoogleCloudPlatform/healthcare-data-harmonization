/*
 * Copyright 2020 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.builtins;

import com.google.cloud.verticals.foundations.dataharmonization.builtins.error.Errors;
import com.google.cloud.verticals.foundations.dataharmonization.builtins.options.MergeModeExperiment;
import com.google.cloud.verticals.foundations.dataharmonization.builtins.options.SingleNullArrayExperiment;
import com.google.cloud.verticals.foundations.dataharmonization.builtins.options.UniqueVarAndFieldsExperiment;
import com.google.cloud.verticals.foundations.dataharmonization.builtins.random.IdGenerator;
import com.google.cloud.verticals.foundations.dataharmonization.builtins.random.Random;
import com.google.cloud.verticals.foundations.dataharmonization.builtins.random.RandomUUIDGenerator;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.whistle.SideTarget;
import com.google.cloud.verticals.foundations.dataharmonization.function.whistle.VarTarget;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Loader;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Parser;
import com.google.cloud.verticals.foundations.dataharmonization.imports.impl.FileLoader;
import com.google.cloud.verticals.foundations.dataharmonization.imports.impl.PluginClassLoader;
import com.google.cloud.verticals.foundations.dataharmonization.imports.impl.PluginClassParser;
import com.google.cloud.verticals.foundations.dataharmonization.imports.impl.ProtoParser;
import com.google.cloud.verticals.foundations.dataharmonization.imports.impl.TextprotoParser;
import com.google.cloud.verticals.foundations.dataharmonization.imports.impl.WhistleParser;
import com.google.cloud.verticals.foundations.dataharmonization.modifier.arg.ArgModifier;
import com.google.cloud.verticals.foundations.dataharmonization.modifier.arg.RequiredArgMod;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.FunctionCollectionBuilder;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Option;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Plugin;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target.Constructor;
import com.google.cloud.verticals.foundations.dataharmonization.target.impl.DebugTarget;
import com.google.common.collect.ImmutableList;
import java.time.Clock;
import java.util.List;

/** Plugin with Core functionality. */
public class Builtins implements Plugin {

  public static final String PACKAGE_NAME = "builtins";

  /**
   * @deprecated In use by retail team. b/279757115. Use config instead.
   */
  @Deprecated() public static final IdGenerator<String> UUID_GENERATOR = new RandomUUIDGenerator();

  /**
   * @deprecated In use by retail team. b/279757115. Use config instead.
   */
  @Deprecated() public static final Clock CLOCK = Clock.systemUTC();

  private final BuiltinsConfig config;

  public Builtins() {
    this(BuiltinsConfig.builder().build());
  }

  public Builtins(BuiltinsConfig config) {
    this.config = config;
  }

  @Override
  public List<Loader> getLoaders() {
    return ImmutableList.of(new PluginClassLoader(), new FileLoader());
  }

  @Override
  public List<Parser> getParsers() {
    return ImmutableList.of(
        new ProtoParser(),
        new TextprotoParser(),
        new PluginClassParser(config.importablePluginAllowlist()),
        new WhistleParser(config.throwWhistleParserTranspilationException()));
  }

  @Override
  public String getPackageName() {
    return PACKAGE_NAME;
  }

  @Override
  public List<CallableFunction> getFunctions() {
    FunctionCollectionBuilder functions =
        new FunctionCollectionBuilder(PACKAGE_NAME)
            .addAllJavaPluginFunctionsInInstance(new Random(config.idGenerator()))
            .addAllJavaPluginFunctionsInInstance(new TimeFns(config.clock()))
            .addAllJavaPluginFunctionsInClass(TimeFns.class)
            .addAllJavaPluginFunctionsInClass(ArrayFns.class)
            .addAllJavaPluginFunctionsInClass(Core.class)
            .addAllJavaPluginFunctionsInClass(DataFns.class)
            .addAllJavaPluginFunctionsInClass(Iteration.class)
            .addAllJavaPluginFunctionsInClass(Operators.class)
            .addAllJavaPluginFunctionsInClass(StringFns.class)
            .addAllJavaPluginFunctionsInClass(Ternary.class)
            .addAllJavaPluginFunctionsInClass(Errors.class);

    if (config.allowFsFuncs()) {
      functions = functions.addAllJavaPluginFunctionsInClass(FileFns.class);
    }

    return functions.build();
  }

  @Override
  public List<Option> getOptions() {
    return ImmutableList.of(
        new MergeModeExperiment(),
        new UniqueVarAndFieldsExperiment(),
        new SingleNullArrayExperiment());
  }

  @Override
  public List<Constructor> getTargets() {
    return ImmutableList.of(
        new DebugTarget.Constructor(), new VarTarget.Constructor(), new SideTarget.Constructor());
  }

  @Override
  public List<ArgModifier> getArgModifiers() {
    return ImmutableList.of(new RequiredArgMod());
  }
}
