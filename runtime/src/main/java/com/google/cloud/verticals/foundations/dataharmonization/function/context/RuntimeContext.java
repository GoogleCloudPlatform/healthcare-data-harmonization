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

package com.google.cloud.verticals.foundations.dataharmonization.function.context;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.DataTypeImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.WhistleRuntimeException;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.WhistleStackOverflowError;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.OverloadSelector;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.CancellationToken.CancelledException;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.impl.NoopCancellationToken;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.storage.ContextStorage;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.storage.impl.DefaultContextStorage;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportProcessor;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Option;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * A common context containing information like the current stack. Importantly, used for calling
 * functions. A RuntimeContext should be immutable.
 */
public interface RuntimeContext extends Serializable {
  /** Static container for ContextStorage. */
  final class Storage {
    private static final ContextStorage storage = new DefaultContextStorage();

    private Storage() {}
  }

  static RuntimeContext current() {
    return Storage.storage.get();
  }

  static void updateCurrent(RuntimeContext newCurrent) {
    Storage.storage.put(newCurrent);
  }

  /**
   * wrap performs preprocessing steps, calls the given delegate with the (possibly preprocessed)
   * given arguments, performs postprocessing steps, and returns the (possibly postprocessed) return
   * value of the delegate.
   *
   * <p>These pre/post-processing steps may include error wrapping/handling, pushing and popping
   * stack frames, performing debugger related tasks, etc; Anything that has to happen before and
   * after a function gets called.
   *
   * <p>For example, a simple implementation of this method might push a stack frame, execute the
   * delegate, then pop the stack frame.
   *
   * @param function The function about to get called.
   * @param args The arguments the function is being called with.
   * @param delegate The actual body/procedure of the function being called.
   * @return the result of the delegate's execution.
   */
  default Data wrap(
      CallableFunction function, Data[] args, BiFunction<RuntimeContext, Data[], Data> delegate) {
    if (getCancellation().isCancelled()) {
      throw new CancelledException(getCancellation());
    }
    StackFrame.Builder builder =
        top()
            .newBuilder()
            .setDebugInfo(function.getDebugInfo())
            .setName(function.getName())
            .setInheritParentVars(function.getSignature().getInheritsParentVars());
    RuntimeContext newContext =
        newContextFromFrame(builder, function.getLocalPackageContext(getCurrentPackageContext()));
    try {
      updateCurrent(newContext);
      Data result = delegate.apply(newContext, args);
      if (getCancellation().isCancelled()) {
        throw new CancelledException(getCancellation());
      }
      return result;
    } catch (CancelledException | WhistleRuntimeException | WhistleStackOverflowError ex) {
      throw ex;
    } catch (RuntimeException e) {
      throw WhistleRuntimeException.fromCurrentContext(newContext, e);
    } finally {
      updateCurrent(this);
    }
  }

  /** Evaluates the given {@link ValueSource} into a {@link Data} value. */
  Data evaluate(ValueSource valueSource);

  /** Returns the {@link Registries} associated to this {@code RuntimeContext}. */
  Registries getRegistries();

  /** Returns the {@link MetaData} */
  MetaData getMetaData();

  /** Returns the current {@link PackageContext}. */
  PackageContext getCurrentPackageContext();

  /** Returns the {@link OverloadSelector} engine for the current context. */
  OverloadSelector getOverloadSelector();

  /**
   * Returns a new derivative {@link RuntimeContext}. The given {@link StackFrame.Builder} should be
   * used to build a new {@link #top()} for this derivative context. This method should set the
   * current {@link #top()} as the parent (with {@link StackFrame.Builder#setParent(StackFrame)})
   * and call {@link StackFrame.Builder#build()}. The given {@link PackageContext} should be the
   * {@link #getCurrentPackageContext()} for the derived RuntimeContext.
   */
  RuntimeContext newContextFromFrame(StackFrame.Builder frame, PackageContext localPackageContext);

  /** Returns the current top-most (newest) {@link StackFrame} in the stack. */
  StackFrame top();

  /** Returns the current bottom-most (oldest) {@link StackFrame} in the stack. */
  StackFrame bottom();

  /** Returns the initialized ImportProcessor for this stack. */
  ImportProcessor getImportProcessor();

  /** Returns the current data type implementation for this context. */
  DataTypeImplementation getDataTypeImplementation();

  /** Registers a monitor to notify when the context starts and finishes. */
  void addMonitor(RuntimeContextMonitor monitor);

  /**
   * Notifies all registered monitors that the context is finishing. ret is the result of the
   * transformation. return value is new result after any postprocessing in this function.
   */
  Data finish(Data ret);

  default CancellationToken getCancellation() {
    return new NoopCancellationToken();
  }

  default Set<Option> enabledOptions() {
    return ImmutableSet.of();
  }
}
