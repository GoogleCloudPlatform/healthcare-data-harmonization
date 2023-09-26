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
package com.google.cloud.verticals.foundations.dataharmonization.function;

import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.DefaultDataTypeImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.debug.DebugInfo;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.NoMatchingOverloadsException;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource.SourceCase;
import com.google.cloud.verticals.foundations.dataharmonization.registry.util.LevenshteinDistance;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

/**
 * DefaultClosure implements Closure and (a readonly) {@link Container} to allow it to be passed to
 * a function as a parameter (which must extend {@link Data}). This is the way to pass
 * lambdas/expressions to functions that need them. For example, a filter on an array might be
 * implemented like
 *
 * <pre>{@code
 * public Array filter(RuntimeContext ctx, Array array, Closure predicate) {
 *   return streamToArray( // Assuming this function exists.
 *           // Note the (here unchecked) assumption that predicate has exactly one free parameter.
 *           array.stream().filter(element -> predicate.bindNextFreeParameter(element).execute(ctx))
 *   );
 * }
 *
 * }</pre>
 *
 * <p>A Closure is built directly from {@link FunctionCall}.
 */
public class DefaultClosure implements Container, Closure {
  /** The Container key to get the list of arguments. */
  public static final String ARGS = "args";
  /** The Container key to get the function reference. */
  public static final String FUNCTION_REF = "functionRef";
  /** The Container key to get the list of argument indices that are free/unbound. */
  public static final String FREE_ARG_INDICES = "freeParamIndices";
  /** The threshold on Levenshtein edit distance allowed for matching similar function names. */
  private static final int FUNCTION_MATCH_THRESHOLD = 2;

  private static final Set<String> fields = ImmutableSet.of(ARGS, FUNCTION_REF, FREE_ARG_INDICES);
  private final Data[] args;
  private final FunctionReference functionRef;
  private final SortedSet<Integer> freeArgIndices;
  private final FunctionCall originalCall;

  private DefaultClosure(
      FunctionReference functionRef,
      Data[] args,
      SortedSet<Integer> freeArgIndices,
      FunctionCall originalCall) {
    this.args = args;
    this.functionRef = functionRef;
    this.freeArgIndices = freeArgIndices;
    this.originalCall = originalCall;
  }
  /**
   * Create a new closure with the given name and arguments.
   *
   * @param functionRef Reference to the function to call.
   * @param args Arguments to pass to the function.
   */
  public static DefaultClosure create(FunctionReference functionRef, Data... args) {
    return new DefaultClosure(
        functionRef, args, Collections.emptySortedSet(), FunctionCall.getDefaultInstance());
  }

  public static DefaultClosure create(
      FunctionReference functionRef, SortedSet<Integer> freeArgIndices, Data... args) {
    return new DefaultClosure(functionRef, args, freeArgIndices, FunctionCall.getDefaultInstance());
  }
  /**
   * Converts the given {@link FunctionCall} to a {@link Data} based {@link Closure}. Arguments that
   * are themselves {@link FunctionCall}s will be converted recursively.
   *
   * @param context {@link RuntimeContext} used to evaluate {@link ValueSource} arguments into their
   *     {@link Data} based values.
   * @param proto The {@link FunctionCall} proto to convert.
   */
  public static DefaultClosure create(RuntimeContext context, FunctionCall proto) {
    int numArgs = proto.getArgsCount();
    Data[] args = new Data[numArgs];
    SortedSet<Integer> freeArgIndices = new TreeSet<>();
    for (int i = 0; i < numArgs; i++) {
      ValueSource arg = proto.getArgs(i);
      // TODO(rpolyano): Remove this, since transpiler handles it.
      if (arg.getIterate()) {
        throw new IllegalArgumentException(
            "Iterated arguments should have been simplified to a call to builtins::iterate");
      }
      if (arg.getSourceCase() == SourceCase.FREE_PARAMETER) {
        freeArgIndices.add(i);
        args[i] = new FreeParameter(arg.getFreeParameter());
        continue;
      }
      // TODO(rpolyano): Remove this, since evaluator handles it.
      if (arg.hasFunctionCall() && arg.getFunctionCall().getBuildClosure()) {
        args[i] = create(context, arg.getFunctionCall());
        continue;
      }
      args[i] = context.evaluate(arg);
    }
    return new DefaultClosure(
        new FunctionReference(proto.getReference().getPackage(), proto.getReference().getName()),
        args,
        Collections.unmodifiableSortedSet(freeArgIndices),
        proto);
  }

  @Nonnull
  @Override
  public Data getField(String field) {
    switch (field) {
      case ARGS:
        return DefaultDataTypeImplementation.instance.arrayOf(Arrays.asList(args));
      case FUNCTION_REF:
        return functionRef;
      case FREE_ARG_INDICES:
        return DefaultDataTypeImplementation.instance.arrayOf(
            freeArgIndices.stream()
                .mapToDouble(Double::valueOf)
                .mapToObj(DefaultDataTypeImplementation.instance::primitiveOf)
                .collect(Collectors.toList()));
      default:
        return NullData.instance;
    }
  }

  @Override
  public Container setField(@Nonnull String field, Data value) {
    throw new UnsupportedOperationException("Function calls are read only.");
  }

  @Override
  public Container removeField(@Nonnull String field) {
    throw new UnsupportedOperationException("Function calls are read only.");
  }

  @Nonnull
  @Override
  public Set<String> fields() {
    return fields;
  }

  @Override
  public Data deepCopy() {
    return new DefaultClosure(functionRef.deepCopy(), args.clone(), freeArgIndices, originalCall);
  }

  @Override
  public boolean isWritable() {
    return false;
  }
  /** Returns the arguments for the function. */
  @Override
  public Data[] getArgs() {
    return args;
  }

  @Override
  public boolean isNullOrEmpty() {
    return false;
  }
  /** Returns the reference to the function to call. */
  public FunctionReference getFunctionRef() {
    return functionRef;
  }
  /**
   * Binds the next free (i.e. unbound) parameter to the given value. This does not modify this
   * function call, but instead returns a new one.
   *
   * @return a new (copied) Closure with the parameter bound.
   * @throws UnsupportedOperationException if the function call does not have any free parameters.
   */
  @Override
  public DefaultClosure bindNextFreeParameter(Data value) {
    if (freeArgIndices.isEmpty()) {
      throw new UnsupportedOperationException("Function call has no free parameters to bind.");
    }
    Data[] args = this.args.clone();
    int arg = freeArgIndices.first();
    args[arg] = value;
    return new DefaultClosure(functionRef, args, freeArgIndices.tailSet(arg + 1), originalCall);
  }
  /**
   * Executes this closure.
   *
   * @param context The runtime context to use for execution.
   * @return The result of the execution.
   * @throws UnsupportedOperationException if this function has any free (i.e. unbound) parameters.
   */
  @Override
  public Data execute(RuntimeContext context) {
    DebugInfo callerInfo = context.top().getDebugInfo();
    if (callerInfo != null) {
      callerInfo.setCallsiteToNextStackFrame(originalCall);
    }

    if (!freeArgIndices.isEmpty()) {
      throw new UnsupportedOperationException("Function call contains unbound free parameters.");
    }
    Set<String> packagesToCheck = new HashSet<>();
    if (functionRef.getPackageName() != null
        && functionRef.getPackageName().equals(FunctionReference.WILDCARD_PACKAGE_NAME)) {
      packagesToCheck.addAll(context.getRegistries().getAllRegisteredPackages());
    } else if (functionRef.getPackageName() != null
        && functionRef.getPackageName().trim().length() > 0) {
      packagesToCheck.add(functionRef.getPackageName());
    } else {
      packagesToCheck.addAll(context.getCurrentPackageContext().getGloballyAliasedPackages());
    }
    Set<CallableFunction> overloads =
        packagesToCheck.stream()
            .map(
                pkg ->
                    context
                        .getRegistries()
                        .getFunctionRegistry(pkg)
                        .getOverloads(ImmutableSet.of(pkg), functionRef.getFunctionName()))
            .reduce(
                new HashSet<>(),
                (a, b) -> Stream.concat(a.stream(), b.stream()).collect(Collectors.toSet()));
    if (overloads.isEmpty()) {
      suggestFunctionNames(context, packagesToCheck);
    }

    CallableFunction overload =
        context.getOverloadSelector().select(ImmutableList.copyOf(overloads), getArgs());
    return overload.call(context, getArgs());
  }

  private void suggestFunctionNames(RuntimeContext context, Set<String> packagesToCheck) {
    Set<String> bestMatchNames =
        packagesToCheck.stream()
            .map(
                pkg ->
                    context
                        .getRegistries()
                        .getFunctionRegistry(pkg)
                        .getBestMatchOverloads(
                            ImmutableSet.of(pkg),
                            functionRef.getFunctionName(),
                            new LevenshteinDistance(FUNCTION_MATCH_THRESHOLD)))
            .flatMap(m -> m.entrySet().stream())
            .flatMap(
                e ->
                    e.getValue().stream()
                        .map(
                            callableFunction ->
                                String.format(
                                    "%s%s()",
                                    e.getKey().isEmpty() ? "" : String.format("%s::", e.getKey()),
                                    callableFunction.getName())))
            .collect(Collectors.toSet());
    throw new NoMatchingOverloadsException(functionRef, bestMatchNames);
  }
  /** Returns the number of free (unbound) parameters in this closure. */
  @Override
  public int getNumFreeParams() {
    return freeArgIndices.size();
  }

  @Override
  public SortedSet<Integer> getFreeArgIndices() {
    return ImmutableSortedSet.copyOfSorted(freeArgIndices);
  }

  @Override
  public String getName() {
    return String.format("%s::%s", functionRef.getPackageName(), functionRef.getFunctionName());
  }

  /** Storage class for a function name and package. */
  public static class FunctionReference implements Container {
    /** The Container key to get the package name. */
    public static final String PACKAGE_NAME = "packageName";
    /** The Container key to get the function name. */
    public static final String FUNCTION_NAME = "functionName";
    /** The special package name to match all available packages. */
    public static final String WILDCARD_PACKAGE_NAME = "*";

    private static final Set<String> fields = ImmutableSet.of(PACKAGE_NAME, FUNCTION_NAME);
    private final String packageName;
    private final String functionName;

    public FunctionReference(@Nonnull String functionName) {
      this(null, functionName);
    }

    public FunctionReference(String packageName, @Nonnull String functionName) {
      this.packageName = packageName;
      this.functionName = functionName;
    }

    public String getPackageName() {
      return packageName;
    }

    public String getFunctionName() {
      return functionName;
    }

    @Nonnull
    @Override
    public Data getField(String field) {
      switch (field) {
        case PACKAGE_NAME:
          return packageName != null && packageName.trim().length() > 0
              ? DefaultDataTypeImplementation.instance.primitiveOf(packageName)
              : NullData.instance;
        case FUNCTION_NAME:
          return DefaultDataTypeImplementation.instance.primitiveOf(functionName);
        default:
          return NullData.instance;
      }
    }

    @Override
    public Container setField(@Nonnull String field, Data value) {
      throw new UnsupportedOperationException("FunctionReference cannot be written to.");
    }

    @Override
    public Container removeField(@Nonnull String field) {
      throw new UnsupportedOperationException("FunctionReference is not mutable.");
    }

    @Nonnull
    @Override
    public Set<String> fields() {
      return fields;
    }

    @Override
    public FunctionReference deepCopy() {
      return new FunctionReference(packageName, functionName);
    }

    @Override
    public boolean isWritable() {
      return false;
    }
    // Auto-generated equality members:
    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof FunctionReference)) {
        return false;
      }
      FunctionReference that = (FunctionReference) o;
      return Objects.equals(getPackageName(), that.getPackageName())
          && getFunctionName().equals(that.getFunctionName());
    }

    @Override
    public int hashCode() {
      return Objects.hash(getPackageName(), getFunctionName());
    }
  }
}
