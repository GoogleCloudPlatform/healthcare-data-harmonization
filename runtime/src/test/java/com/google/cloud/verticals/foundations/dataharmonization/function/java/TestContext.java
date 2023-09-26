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
package com.google.cloud.verticals.foundations.dataharmonization.function.java;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.DataTypeImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.OverloadSelector;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.MetaData;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.PackageContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.Registries;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContextMonitor;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.StackFrame;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportProcessor;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import java.util.function.BiFunction;

/**
 * Simple {@link RuntimeContext} implementation that just forwards invocations to the given
 * delegate.
 */
public class TestContext implements RuntimeContext {

  @Override
  public Data wrap(
      CallableFunction function, Data[] args, BiFunction<RuntimeContext, Data[], Data> delegate) {
    return delegate.apply(this, args);
  }

  @Override
  public Data evaluate(ValueSource valueSource) {
    throw new UnsupportedOperationException("TestContext does not support such sorcery.");
  }

  @Override
  public Registries getRegistries() {
    throw new UnsupportedOperationException("TestContext does not support such sorcery.");
  }

  @Override
  public PackageContext getCurrentPackageContext() {
    throw new UnsupportedOperationException("TestContext does not support such sorcery.");
  }

  @Override
  public OverloadSelector getOverloadSelector() {
    throw new UnsupportedOperationException("TestContext does not support such sorcery.");
  }

  @Override
  public RuntimeContext newContextFromFrame(
      StackFrame.Builder frame, PackageContext localPackageContext) {
    throw new UnsupportedOperationException("TestContext does not support such sorcery.");
  }

  @Override
  public StackFrame top() {
    throw new UnsupportedOperationException("TestContext does not support such sorcery.");
  }

  @Override
  public StackFrame bottom() {
    throw new UnsupportedOperationException("TestContext does not support such sorcery.");
  }

  @Override
  public MetaData getMetaData() {
    throw new UnsupportedOperationException("TestContext does not support such sorcery.");
  }

  @Override
  public ImportProcessor getImportProcessor() {
    throw new UnsupportedOperationException("TestContext does not support such sorcery.");
  }

  @Override
  public DataTypeImplementation getDataTypeImplementation() {
    return testDTI();
  }

  @Override
  public void addMonitor(RuntimeContextMonitor monitor) {
    throw new UnsupportedOperationException("TestContext does not support such sorcery.");
  }

  @Override
  public Data finish(Data returnData) {
    // Noop
    return returnData;
  }
}
