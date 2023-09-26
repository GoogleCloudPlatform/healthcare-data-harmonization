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

package com.google.cloud.verticals.foundations.dataharmonization.function.whistle;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import com.google.cloud.verticals.foundations.dataharmonization.builtins.Builtins;
import com.google.cloud.verticals.foundations.dataharmonization.builtins.Core;
import com.google.cloud.verticals.foundations.dataharmonization.builtins.Iteration;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.debug.DebugInfo;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.NoMatchingOverloadsException;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.Closure;
import com.google.cloud.verticals.foundations.dataharmonization.function.DefaultClosure.FunctionReference;
import com.google.cloud.verticals.foundations.dataharmonization.function.NativeUnaryClosure;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.PackageContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.signature.Signature;
import com.google.cloud.verticals.foundations.dataharmonization.function.whistle.VarTarget.Constructor;
import com.google.cloud.verticals.foundations.dataharmonization.modifier.arg.ArgModifier;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Option;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FieldMapping;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition.Argument;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;
import com.google.cloud.verticals.foundations.dataharmonization.registry.util.LevenshteinDistance;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;

/** {@link CallableFunction} implementation for a Whistle {@link FunctionDefinition}. */
public class WhistleFunction extends CallableFunction {

  public static final String OUTPUT_VAR = "$this";
  private final FunctionDefinition proto;
  private final PipelineConfig declaringConfig;
  private final Signature signature;
  private final PackageContext packageContext;

  public WhistleFunction(
      FunctionDefinition proto, PipelineConfig declaringConfig, PackageContext packageContext) {
    this.proto = proto;
    this.signature = generateSignature(packageContext.getCurrentPackage(), proto);
    this.declaringConfig = declaringConfig;
    this.packageContext = packageContext;
  }

  private Signature generateSignature(String currentPackageName, FunctionDefinition proto) {
    return new Signature(
        currentPackageName,
        proto.getName(),
        /* args= */ Collections.nCopies(proto.getArgsCount(), Data.class),
        /* isVariadic= */ false,
        proto.getInheritParentVars());
  }

  @Override
  protected Data callInternal(RuntimeContext context, Data... args) {
    // make sure argument number matches the function signature.
    verifyArgs(args);
    // determine if any of the argument values under its modifier can short circuit the function.
    Optional<Data> shortCircuitVal = shortCircuit(context, args);
    if (shortCircuitVal.isPresent()) {
      return shortCircuitVal.get();
    }
    // bind argument value to variable
    bindArgs(context, args);

    // Toggle options and execute mappings.
    Option.withConfig(
        new HashSet<>(declaringConfig.getOptionsList()), context, this::executeMappings);

    return context.top().getVar(OUTPUT_VAR);
  }

  private void executeMappings(RuntimeContext context) {
    for (FieldMapping mapping : proto.getMappingList()) {
      Data source = context.evaluate(mapping.getValue());
      Target target = getTarget(context, mapping);
      if (mapping.getIterateSource()) {
        Closure targetClosure = new NativeUnaryClosure(new TargetWriteFunction(target, context));
        if (source.isNullOrEmpty()) {
          continue;
        }
        if (source.isArray()) {
          Iteration.iterate(context, targetClosure, source.asArray());
        } else if (source.isContainer()) {
          Iteration.iterate(context, targetClosure, source.asContainer());
        } else if (source.isDataset()) {
          Iteration.iterate(context, targetClosure, source.asDataset());
        } else {
          throw new IllegalArgumentException(
              String.format(
                  "Cannot iterate %s (a non-iterable %s) into target %s",
                  source, source.getClass().getSimpleName(), target));
        }
      } else {
        // add debugging info to callSiteToNextFrame here.
        DebugInfo callerInfo = context.top().getDebugInfo();
        if (callerInfo != null) {
          callerInfo.setCallsiteToNextStackFrame(mapping);
        }
        target.write(context, source);
      }
    }
  }

  private Target getTarget(RuntimeContext context, FieldMapping mapping) {
    switch (mapping.getTargetCase()) {
      case CUSTOM_SINK:
        return customSink(context, mapping);
      case VAR:
        return builtinSink(
            context,
            Constructor.TARGET_NAME,
            context.getDataTypeImplementation().primitiveOf(mapping.getVar().getName()),
            context.getDataTypeImplementation().primitiveOf(mapping.getVar().getPath()));
      case FIELD:
        String field = mapping.getField().getPath();
        if (field.equals(OUTPUT_VAR)) {
          field = "";
        }
        switch (mapping.getField().getType()) {
          case LOCAL:
            return builtinSink(
                context,
                Constructor.TARGET_NAME,
                context.getDataTypeImplementation().primitiveOf(OUTPUT_VAR),
                context.getDataTypeImplementation().primitiveOf(field));
          case SIDE:
            return builtinSink(
                context,
                SideTarget.Constructor.TARGET_NAME,
                context.getDataTypeImplementation().primitiveOf(field));
          case UNRECOGNIZED:
            throw new IllegalArgumentException(
                String.format(
                    "Proto error - invalid field target type: %s", mapping.getField().getType()));
        }
        // fall through, though this should never happen.
      default:
        // By default - write to $this.
        return builtinSink(
            context,
            Constructor.TARGET_NAME,
            context.getDataTypeImplementation().primitiveOf(OUTPUT_VAR),
            context.getDataTypeImplementation().primitiveOf(""));
    }
  }

  private Target builtinSink(RuntimeContext context, String name, Data... args) {
    // TODO(rpolyano): Give targets signatures and use regular overload selection.
    Set<Target.Constructor> overloads =
        context
            .getRegistries()
            .getTargetRegistry()
            .getOverloads(ImmutableSet.of(Builtins.PACKAGE_NAME), name);

    boolean multiple = false;
    Target matching = null;
    for (Target.Constructor overload : overloads) {
      try {
        Target target = overload.construct(context, args);
        if (matching != null) {
          multiple = true;
          break;
        }
        matching = target;
      } catch (IllegalArgumentException ex) {
        // Noop.
      }
    }

    if (matching == null) {
      throw new NoMatchingOverloadsException(
          new FunctionReference(Builtins.PACKAGE_NAME, name),
          context
              .getRegistries()
              .getTargetRegistry()
              .getBestMatchOverloads(
                  ImmutableSet.of(Builtins.PACKAGE_NAME), name, new LevenshteinDistance(2))
              .keySet());
    }

    if (multiple) {
      // TODO(rpolyano): Throw MultipleMatchingOverloadsException
      throw new IllegalArgumentException(
          String.format(
              "Too many targets callable with given args.\nArgs: \n\t%s\nTargets: \n\t%s",
              stream(args).map(a -> String.join("/", Core.types(a))).collect(joining("\n\t")),
              overloads.stream()
                  .map(
                      c -> {
                        try {
                          return c.construct(context, args);
                        } catch (IllegalArgumentException ex) {
                          return null;
                        }
                      })
                  .filter(Objects::nonNull)
                  .map(Target::getClass)
                  .map(Class::getSimpleName)
                  .collect(joining("\n\t"))));
    }

    return matching;
  }

  private Target customSink(RuntimeContext context, FieldMapping mapping) {
    String pkg = mapping.getCustomSink().getReference().getPackage();
    String name = mapping.getCustomSink().getReference().getName();
    Set<String> packages =
        pkg.trim().length() == 0
            ? context.getCurrentPackageContext().getGloballyAliasedPackages()
            : ImmutableSet.of(pkg);
    Set<Target.Constructor> targets =
        context.getRegistries().getTargetRegistry().getOverloads(packages, name);
    if (targets.size() > 1) {
      throw new UnsupportedOperationException(
          String.format(
              "Target Overloads are not supported; Target %s::%s has multiple implementations.",
              pkg, name));
    }

    if (targets.isEmpty()) {
      return FunctionTarget.construct(context, mapping.getCustomSink());
    }

    Data[] args =
        mapping.getCustomSink().getArgsList().stream().map(context::evaluate).toArray(Data[]::new);

    return targets.iterator().next().construct(context, args);
  }

  /**
   * Returns the short circuit value if any of the arg among all arguments to the {@link
   * WhistleFunction} can make the function skip its execution. Returns {@link Optional#empty()}
   * when non of the argument can do so.
   */
  private Optional<Data> shortCircuit(RuntimeContext context, Data[] args) {
    for (int i = 0; i < args.length; i++) {
      ArgModifier argModifier =
          context.getRegistries().getArgModifierRegistry().get(proto.getArgs(i).getModifier());
      if (argModifier != null && argModifier.canShortCircuit(args[i])) {
        return Optional.of(argModifier.getShortCircuitValue(args[i]));
      }
    }
    return Optional.empty();
  }

  private void bindArgs(RuntimeContext context, Data[] args) {
    IntStream.range(0, proto.getArgsCount())
        .forEach(
            i ->
                context
                    .top()
                    .setVar(
                        proto.getArgs(i).getName(),
                        modifyArg(
                            context
                                .getRegistries()
                                .getArgModifierRegistry()
                                .get(proto.getArgs(i).getModifier()),
                            args[i])));
  }

  private Data modifyArg(ArgModifier argMod, Data arg) {
    return argMod == null ? arg : argMod.modifyArgValue(arg);
  }

  private void verifyArgs(Data[] args) {
    if (args.length < proto.getArgsCount()) {
      throw new IllegalArgumentException(
          String.format(
              "Not enough arguments for function %s (want %d, got %d): missing values for %s",
              signature.getName(),
              proto.getArgsCount(),
              args.length,
              proto.getArgsList().stream()
                  .skip(args.length)
                  .map(Argument::getName)
                  .collect(joining(", "))));
    }
    if (args.length > proto.getArgsCount()) {
      throw new IllegalArgumentException(
          String.format(
              "Too many arguments for function %s: want %d, got %d",
              signature.getName(), proto.getArgsCount(), args.length));
    }
  }

  @Override
  public Signature getSignature() {
    return signature;
  }

  @Override
  public PackageContext getLocalPackageContext(PackageContext current) {
    return packageContext;
  }

  @Override
  public DebugInfo getDebugInfo() {
    return DebugInfo.fromFunction(declaringConfig, proto);
  }

  public PipelineConfig getPipelineConfig() {
    return declaringConfig;
  }

  public FunctionDefinition getProto() {
    return this.proto;
  }

  private static class TargetWriteFunction implements Serializable, Function<Data, Data> {

    private final Target target;
    private final RuntimeContext context;

    private TargetWriteFunction(Target target, RuntimeContext context) {
      this.target = target;
      this.context = context;
    }

    @Override
    public Data apply(Data data) {
      target.write(context, data);
      return NullData.instance;
    }
  }
}
