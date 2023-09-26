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
package com.google.cloud.verticals.foundations.dataharmonization.utils;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.DataTypeImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.function.DefaultOverloadSelector;
import com.google.cloud.verticals.foundations.dataharmonization.function.OverloadSelector;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.MetaData;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.PackageContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.Registries;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContextMonitor;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.StackFrame;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.impl.DefaultMetaData;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.impl.DefaultRegistries;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportProcessor;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import java.util.ArrayList;
import java.util.List;

/** A runtime context capable of storage. Cannot evaluate ValueSources or process imports. */
public class TestRuntimeContext implements RuntimeContext {
  private final Registries registries = new DefaultRegistries();
  private final MetaData metaData = new DefaultMetaData();
  private final OverloadSelector overloadSelector = new DefaultOverloadSelector();
  private final DataTypeImplementation dti = new TestDataTypeImplementation();

  private final StackFrame top;
  private final StackFrame bottom;
  private final PackageContext packageContext;
  private final List<RuntimeContextMonitor> monitors;

  public TestRuntimeContext(StackFrame top, StackFrame bottom, PackageContext packageContext) {
    this(top, bottom, packageContext, new ArrayList<>());
  }

  private TestRuntimeContext(
      StackFrame top,
      StackFrame bottom,
      PackageContext packageContext,
      List<RuntimeContextMonitor> monitors) {
    this.top = top;
    this.bottom = bottom;
    this.packageContext = packageContext;
    this.monitors = monitors;
  }

  @Override
  public Data evaluate(ValueSource valueSource) {
    throw new UnsupportedOperationException();
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
    return overloadSelector;
  }

  @Override
  public RuntimeContext newContextFromFrame(
      StackFrame.Builder frame, PackageContext localPackageContext) {
    return new TestRuntimeContext(frame.setParent(top).build(), bottom, localPackageContext);
  }

  @Override
  public StackFrame top() {
    return top;
  }

  @Override
  public StackFrame bottom() {
    return bottom;
  }

  @Override
  public ImportProcessor getImportProcessor() {
    throw new UnsupportedOperationException();
  }

  @Override
  public DataTypeImplementation getDataTypeImplementation() {
    return dti;
  }

  @Override
  public void addMonitor(RuntimeContextMonitor monitor) {
    monitors.add(monitor);
  }

  @Override
  public Data finish(Data returnData) {
    Data updatedData = returnData;
    for (RuntimeContextMonitor monitor : monitors) {
      updatedData = monitor.onRuntimeContextFinish(this, updatedData);
    }
    return updatedData;
  }
}
