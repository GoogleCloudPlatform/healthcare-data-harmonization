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

package com.google.cloud.verticals.foundations.dataharmonization.function.context.impl;

import com.google.cloud.verticals.foundations.dataharmonization.TranspilerData;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.DataTypeImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.DefaultDataTypeImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.debug.DebugInfo;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.ImportInfo;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.DefaultOverloadSelector;
import com.google.cloud.verticals.foundations.dataharmonization.function.OverloadSelector;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.CancellationToken;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.MetaData;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.PackageContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.Registries;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContextImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContextMonitor;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.StackFrame;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportProcessor;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig.Import;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.LinkedHashSet;
import java.util.function.BiFunction;

/**
 * The InitializationContext is a RuntimeContext that is used exclusively for initialization. It
 * only implements the methods relating to registries and imports, and creates a "real"
 * RuntimeContext and first stack frame only when {@link RuntimeContext#wrap(CallableFunction,
 * Data[], BiFunction)} is called for the first time.
 *
 * <p>After this first wrap, all non-registry/import related calls will delegate to the newly
 * created "real" context. Before this, those methods will throw an {@link IllegalStateException}.
 */
public class InitializationContext implements RuntimeContext {
  private final OverloadSelector selector = new DefaultOverloadSelector();
  private final Registries registries;
  private final ImportProcessor importProcessor;
  private final PackageContext packageContext;

  private final MetaData metaData;

  private final RuntimeContextImplementation mainRtxImpl;
  private final StackFrame.Builder rootFrameBuilder;
  private RuntimeContext initializedContext;

  // Placeholder for monitors added before the initializationContext is wrapped.
  private final LinkedHashSet<RuntimeContextMonitor> monitors =
      new LinkedHashSet<RuntimeContextMonitor>();

  public InitializationContext(
      PackageContext packageContext,
      Registries registries,
      ImportProcessor importProcessor,
      RuntimeContextImplementation mainRtxImpl,
      StackFrame.Builder rootFrameBuilder,
      MetaData metaData) {
    this.importProcessor = importProcessor;
    this.registries = registries;
    this.packageContext = packageContext;
    this.mainRtxImpl = mainRtxImpl;
    this.rootFrameBuilder = rootFrameBuilder;
    this.metaData = metaData;
    RuntimeContext.updateCurrent(this);
  }

  @Override
  public Data wrap(
      CallableFunction function, Data[] args, BiFunction<RuntimeContext, Data[], Data> delegate) {
    if (initializedContext == null) {
      StackFrame rootFrame =
          rootFrameBuilder
              .setDebugInfo(function.getDebugInfo())
              .setName(function.getName())
              .setInheritParentVars(function.getSignature().getInheritsParentVars())
              .build();
      initializedContext =
          mainRtxImpl.constructMainContext(
              packageContext,
              rootFrame /* stackTop */,
              rootFrame /* stackBottom */,
              registries,
              importProcessor,
              metaData);

      this.monitors.forEach(initializedContext::addMonitor);

      return delegate.apply(initializedContext, args);
    }

    return getInitializedContext().wrap(function, args, delegate);
  }

  @Override
  public Data evaluate(ValueSource valueSource) {
    return getInitializedContext().evaluate(valueSource);
  }

  public Data evaluateImport(Import importMsg, PipelineConfig config) {
    ImportInfo importInfo;
    try {
      importInfo =
          importMsg
              .getMeta()
              .getEntriesOrDefault(
                  TranspilerData.IMPORT_META_KEY,
                  Any.pack(ImportInfo.newBuilder().setPathCode("unknown path").build()))
              .unpack(ImportInfo.class);
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException(
          String.format(
              "Cannot find the import path in transpiled metadata (import proto: %s)", importMsg),
          e);
    }
    String pathCode = importInfo.getPathCode();
    StackFrame importFrame =
        rootFrameBuilder
            .setDebugInfo(DebugInfo.fromImport(config, importMsg))
            .setName(String.format("import (%s)", pathCode))
            .setInheritParentVars(false)
            .build();
    RuntimeContext context =
        mainRtxImpl.constructMainContext(
            packageContext, importFrame, importFrame, registries, importProcessor, metaData);
    try {
      return context.evaluate(importMsg.getValue());
    } catch (RuntimeException e) {
      throw new IllegalArgumentException(
          String.format(
              "%s cannot be evaluated into a valid string import path: %s",
              pathCode, e.getMessage()));
    }
  }

  @Override
  public Registries getRegistries() {
    return registries;
  }

  @Override
  public MetaData getMetaData() {
    return metaData;
  }

  @Override
  public PackageContext getCurrentPackageContext() {
    return packageContext;
  }

  @Override
  public OverloadSelector getOverloadSelector() {
    return selector;
  }

  @Override
  public RuntimeContext newContextFromFrame(
      StackFrame.Builder frame, PackageContext localPackageContext) {
    return getInitializedContext().newContextFromFrame(frame, packageContext);
  }

  @Override
  public StackFrame top() {
    return getInitializedContext().top();
  }

  @Override
  public StackFrame bottom() {
    return getInitializedContext().bottom();
  }

  @Override
  public ImportProcessor getImportProcessor() {
    return importProcessor;
  }

  @Override
  public DataTypeImplementation getDataTypeImplementation() {
    // TODO(): Pass this in through ctor
    if (initializedContext == null) {
      return DefaultDataTypeImplementation.instance;
    }
    return getInitializedContext().getDataTypeImplementation();
  }

  /** Adds a monitor to the context. It must already be initialized. */
  @Override
  public void addMonitor(RuntimeContextMonitor monitor) {
    if (initializedContext == null) {
      this.monitors.add(monitor);
    } else {
      getInitializedContext().addMonitor(monitor);
    }
  }

  @Override
  public Data finish(Data returnData) {
    if (initializedContext != null) {
      return getInitializedContext().finish(returnData);
    }
    return returnData;
  }

  @Override
  public CancellationToken getCancellation() {
    return getInitializedContext().getCancellation();
  }

  public RuntimeContext getInitializedContext() {
    if (initializedContext == null) {
      throw new IllegalStateException(
          "Context has not been bootstrapped yet. A root function must be called before this"
              + " method.");
    }

    return initializedContext;
  }
}
