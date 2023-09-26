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
package com.google.cloud.verticals.foundations.dataharmonization.plugin;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.cloud.verticals.foundations.dataharmonization.function.context.MetaData;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.WrapperContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.impl.InitializationContext;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;
import com.google.cloud.verticals.foundations.dataharmonization.registry.Registrable;
import com.google.cloud.verticals.foundations.dataharmonization.registry.Registry;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/** Represents an option that can be enabled at the top of a Whistle file. */
public interface Option extends Registrable {


  /**
   * Wraps/configures the given RuntimeContext according to the implementation of this option.
   *
   * @param context The RuntimeContext to work on. It may have been previously modified/wrapped by
   *     other options, so no assumptions about its state should be made, other than it will not be
   *     an {@link InitializationContext}.
   * @param config The Option proto that triggered this enable.
   * @return The wrapped/configured runtime context to continue with.
   */
  RuntimeContext enable(RuntimeContext context, PipelineConfig.Option config);

  /**
   * Unwraps/unconfigures the given RuntimeContext. This should undo anything done by {@link
   * #enable}.
   */
  RuntimeContext disable(RuntimeContext context);

  /**
   * Allows options that have implemented this method to run at Engine Init time.
   *
   * @param config The incoming pipeline config.
   * @param metaData RuntimeContext metadata
   */
  default void runInitOption(PipelineConfig config, MetaData metaData) {
    return;
  }
  ;

  static void withConfig(
      Set<PipelineConfig.Option> newOptionSet,
      RuntimeContext context,
      Consumer<RuntimeContext> body) {
    Set<Option> currentOptionSet = context.enabledOptions();

    ImmutableMap<Option, PipelineConfig.Option> newOptionToConfig =
        newOptionSet.stream()
            .collect(
                toImmutableMap(
                    c -> context.getRegistries().getOptionRegistry().get(c.getName()),
                    Function.identity()));

    Set<Option> disable = Sets.difference(currentOptionSet, newOptionToConfig.keySet());
    Set<Option> enable = Sets.difference(newOptionToConfig.keySet(), currentOptionSet);

    RuntimeContext finalContext = context;
    for (Option option : disable) {
      finalContext = option.disable(finalContext);
    }

    for (Option option : enable) {
      finalContext = option.enable(finalContext, newOptionToConfig.get(option));
    }

    body.accept(finalContext);

    // TODO(rpolyano): Figure out if below is necessary
    // for (Option option : disable) {
    //   // TODO(rpolyano): newOptionToConfig may not have entry for these.
    //   finalContext = option.enable(finalContext, newOptionToConfig.get(option));
    // }
    //
    // for (Option option : enable) {
    //   finalContext = option.disable(finalContext);
    // }
  }

  static void runEngineInitTimeOptions(
      RuntimeContext runtimeContext, PipelineConfig pipelineConfig) {

    // Run init logic for options that are enabled.
    ImmutableSet<String> enabledOptions =
        pipelineConfig.getOptionsList().stream().map(o -> o.getName()).collect(toImmutableSet());
    Registry<Option> optionRegistry = runtimeContext.getRegistries().getOptionRegistry();
    if (optionRegistry != null) {
      optionRegistry.getAll().stream()
          .filter(o -> enabledOptions.contains(o.getName()))
          .forEach(
              enabledOption ->
                  enabledOption.runInitOption(pipelineConfig, runtimeContext.getMetaData()));
    }
  }

  /**
   * Returns a runtime context that is wrapped on the given one, but with the given option appended
   * to the enabledOptions set (without modifying the inner context's actual set).
   */
  static RuntimeContext withOption(RuntimeContext context, Option option) {
    WithOptionWrapper wrapper =
        WrapperContext.getWrapper(context, WithOptionWrapper.class, x -> true);
    if (wrapper == null) {
      wrapper = new WithOptionWrapper(context);
    }

    wrapper.enable(option);
    return wrapper;
  }

  static RuntimeContext withoutOption(RuntimeContext context, Option option) {
    WithOptionWrapper wrapper =
        WrapperContext.getWrapper(context, WithOptionWrapper.class, x -> true);
    if (wrapper == null) {
      return context;
    }

    wrapper.disable(option);
    return wrapper;
  }

  /** Options in the experimental category should use this function for their name. */
  static String experiment(String name) {
    return String.format("experiment/%s", name);
  }

  /** Wrapper class that can add an option to any immutable runtime context. */
  class WithOptionWrapper extends WrapperContext<WithOptionWrapper> {
    private final Set<Option> enabledOptions = new HashSet<>();

    public WithOptionWrapper(RuntimeContext innerContext) {
      super(innerContext, WithOptionWrapper.class);
    }

    @Override
    protected WithOptionWrapper rewrap(RuntimeContext innerContext) {
      WithOptionWrapper rewrapped = new WithOptionWrapper(innerContext);
      rewrapped.enabledOptions.addAll(this.enabledOptions);
      return rewrapped;
    }

    @Override
    public Set<Option> enabledOptions() {
      return Sets.union(enabledOptions, super.enabledOptions());
    }

    public void enable(Option option) {
      this.enabledOptions.add(option);
    }

    public void disable(Option option) {
      this.enabledOptions.remove(option);
    }
  }
}
