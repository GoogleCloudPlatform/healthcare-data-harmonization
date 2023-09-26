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
package com.google.cloud.verticals.foundations.dataharmonization.function.context;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.DataTypeImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.Closure;
import com.google.cloud.verticals.foundations.dataharmonization.function.DefaultClosure;
import com.google.cloud.verticals.foundations.dataharmonization.function.OverloadSelector;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportProcessor;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Option;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import javax.annotation.Nullable;

/**
 * Wrapper context allows a class to wrap some other runtime context implementation. This means it
 * can override some runtime context functionality while deferring the rest to the internal
 * implementation.
 *
 * @param <T> The wrapping subclass. This class should be extended like {@code public class X
 *     extends WrapperContext<X>}
 */
public abstract class WrapperContext<T extends WrapperContext<T>> implements RuntimeContext {
  private final RuntimeContext innerContext;
  private final Class<T> clazz;

  public WrapperContext(RuntimeContext innerContext, Class<T> clazz) {
    this.innerContext = innerContext;
    this.clazz = clazz;
  }

  /**
   * Construct a new WrapperContext implementation.
   *
   * @param innerContext the context to wrap.
   */
  protected abstract T rewrap(RuntimeContext innerContext);

  @Override
  public Data wrap(
      CallableFunction function, Data[] args, BiFunction<RuntimeContext, Data[], Data> delegate) {
    BiFunction<RuntimeContext, Data[], Data> wrappedDelegate =
        (ctx, a) -> {
          RuntimeContext wrappedContext = ctx.getClass().equals(clazz) ? ctx : rewrap(ctx);
          RuntimeContext.updateCurrent(wrappedContext);
          try {
            return delegate.apply(wrappedContext, a);
          } finally {
            RuntimeContext.updateCurrent(ctx);
          }
        };
    return innerContext.wrap(function, args, wrappedDelegate);
  }

  @Override
  public Data evaluate(ValueSource valueSource) {
    if (valueSource.hasFunctionCall()) {
      // Closures are special because they need an RTX to be created.
      return evaluateFunctionCall(valueSource.getFunctionCall());
    }
    return innerContext.evaluate(valueSource);
  }

  protected Data evaluateFunctionCall(FunctionCall functionCall) {
    Closure closure = DefaultClosure.create(this, functionCall);
    if (functionCall.getBuildClosure()) {
      return closure;
    }
    return closure.execute(this);
  }

  @Override
  public Registries getRegistries() {
    return innerContext.getRegistries();
  }

  @Override
  public MetaData getMetaData() {
    return innerContext.getMetaData();
  }

  @Override
  public PackageContext getCurrentPackageContext() {
    return innerContext.getCurrentPackageContext();
  }

  @Override
  public OverloadSelector getOverloadSelector() {
    return innerContext.getOverloadSelector();
  }

  @Override
  public RuntimeContext newContextFromFrame(
      StackFrame.Builder frame, PackageContext localPackageContext) {
    return rewrap(innerContext.newContextFromFrame(frame, localPackageContext));
  }

  @Override
  public StackFrame top() {
    return innerContext.top();
  }

  @Override
  public StackFrame bottom() {
    return innerContext.bottom();
  }

  @Override
  public ImportProcessor getImportProcessor() {
    return innerContext.getImportProcessor();
  }

  @Override
  public DataTypeImplementation getDataTypeImplementation() {
    return innerContext.getDataTypeImplementation();
  }

  @Override
  public void addMonitor(RuntimeContextMonitor monitor) {
    innerContext.addMonitor(monitor);
  }

  @Override
  public Data finish(Data returnData) {
    return innerContext.finish(returnData);
  }

  @Override
  public CancellationToken getCancellation() {
    return innerContext.getCancellation();
  }

  @Override
  public Set<Option> enabledOptions() {
    return innerContext.enabledOptions();
  }

  public RuntimeContext getInnerContext() {
    return innerContext;
  }

  /**
   * Finds the wrapper of the given class matching the given predicate. This wrapper is then brought
   * to the surface of the wrapper "chain". That is, the returned WrapperContext will be (a copy of)
   * the one requested, but will now have descendants of all the wrappers that were previously its
   * ancestors. For example, where {@code A -> B} represents {@code A.innerContext = B} and {@code
   * A'} represents a copy, via {@link #rewrap} of {@code A}:
   *
   * <pre>{@code
   * given x = A -> B -> C -> D -> E -> z
   * getWrapper(x, C.class, c -> c == C) => C' -> A' -> B' -> D -> E -> z
   * getWrapper(x, B.class, b -> b == B) => B' -> A' -> C -> D -> E -> z
   * getWrapper(x, A.class, a -> a == A) => A -> B -> C -> D -> E -> z (noop)
   * }</pre>
   */
  @Nullable
  public static <U extends WrapperContext<U>> U getWrapper(
      RuntimeContext context, Class<U> clazz, Predicate<U> predicate) {
    List<WrapperContext<?>> wrapperStack = new ArrayList<>();
    U head = null;
    RuntimeContext tail = context;

    // Find the wrapper matching the criteria. After this loop,
    // head should be the matching wrapper, and tail should be its inner context.
    while (tail instanceof WrapperContext) {
      WrapperContext<?> wContext = (WrapperContext<?>) tail;
      tail = wContext.innerContext;

      if (clazz.isAssignableFrom(wContext.getClass()) && predicate.test(clazz.cast(wContext))) {
        head = clazz.cast(wContext);
        break;
      }

      wrapperStack.add(wContext);
    }

    // Currently, we have X -> ... -> Z -> head -> tail.
    // We want head -> X -> ... -> Z -> tail.
    // wrapperStack is X -> ... -> Z, so reattach Z (creating a new Z) then reattach everything
    // ahead of Z, repeating through the stack.
    for (int i = wrapperStack.size() - 1; i >= 0; i--) {
      if (i == wrapperStack.size() - 1) {
        wrapperStack.set(i, wrapperStack.get(i).rewrap(tail));
        continue;
      }
      wrapperStack.set(i, wrapperStack.get(i).rewrap(wrapperStack.get(i + 1)));
    }

    // Reattach head to the first item in the intermediate stack.
    if (head != null && !wrapperStack.isEmpty()) {
      head = head.rewrap(wrapperStack.get(0));
    }

    return head;
  }

  /**
   * Returns the first wrapper that's of class {@code clazz}. Returns null if there's no wrapper of
   * class {@code clazz} in that {@code context} refers.
   */
  @Nullable
  public static <U extends WrapperContext<U>> U getWrapper(RuntimeContext context, Class<U> clazz) {
    return getWrapper(context, clazz, x -> true);
  }

  /**
   * Pushes this WrapperContext to the end of the chain of wrapper contexts. That is, makes all
   * "descendants" of this wrapper into ancestors.
   *
   * <p>For example, where {@code A -> B} represents {@code A.innerContext = B} and {@code A'}
   * represents a copy, via {@link #rewrap} of {@code A}:
   *
   * <pre>{@code
   * given A -> B -> C -> D -> E -> z
   * A.pushToBottom() => B' -> C' -> D' -> E' -> A' -> z
   * D.pushToBottom() => E' -> D' -> z
   * E.pushToBottom() => E -> z (noop)
   * }</pre>
   */
  public RuntimeContext pushToBottom() {
    List<WrapperContext<?>> stack = new ArrayList<>();
    RuntimeContext current = this.innerContext;
    while (current instanceof WrapperContext) {
      WrapperContext<?> w = (WrapperContext<?>) current;
      stack.add(w);

      current = w.innerContext;
    }

    if (stack.isEmpty()) {
      return this;
    }

    current = this.rewrap(current);

    for (int i = stack.size() - 1; i >= 0; i--) {
      stack.set(i, stack.get(i).rewrap(current));
      current = stack.get(i);
    }

    return current;
  }

  /** Returns true iff the {@code context} has a wrapper of class {@code clazz}. */
  public static <U extends WrapperContext<U>> boolean hasWrapper(
      RuntimeContext context, Class<U> clazz) {
    return getWrapper(context, clazz) != null;
  }
}
