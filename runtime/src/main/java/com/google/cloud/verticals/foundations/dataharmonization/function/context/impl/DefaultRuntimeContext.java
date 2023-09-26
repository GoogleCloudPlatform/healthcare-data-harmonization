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

package com.google.cloud.verticals.foundations.dataharmonization.function.context.impl;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.DataTypeImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.DefaultDataTypeImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.function.DefaultClosure;
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
import com.google.cloud.verticals.foundations.dataharmonization.function.context.impl.serialization.RuntimeContextComponentSerializer;
import com.google.cloud.verticals.foundations.dataharmonization.function.whistle.WhistleFunction;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportProcessor;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

/** Default implementation of runtime context. */
public class DefaultRuntimeContext implements RuntimeContext {
  public static final String REGISTRIES_SERIALIZER_KEY =
      String.format("%s/REGISTRIES_SERIALIZER_KEY", DefaultRuntimeContext.class);

  private final OverloadSelector selector;
  private transient Registries registries;
  private final MetaData metaData;
  private final ImportProcessor importProcessor;
  private final PackageContext packageContext;
  private final StackFrame stackTop;
  private final StackFrame stackBottom;
  private final CancellationToken cancellationToken;
  private final transient Set<RuntimeContextMonitor> monitors;

  public DefaultRuntimeContext(
      PackageContext packageContext,
      StackFrame stackTop,
      StackFrame stackBottom,
      Registries registries,
      ImportProcessor importProcessor,
      MetaData metaData,
      CancellationToken cancellationToken,
      Set<RuntimeContextMonitor> monitors) {
    this.selector = new DefaultOverloadSelector();
    this.packageContext = packageContext;
    this.stackTop = stackTop;
    this.stackBottom = stackBottom;
    this.registries = registries;
    this.importProcessor = importProcessor;
    this.metaData = metaData;
    this.monitors = monitors;
    this.cancellationToken = cancellationToken;
  }

  public DefaultRuntimeContext(
      PackageContext packageContext,
      StackFrame rootStackFrame,
      Registries registries,
      ImportProcessor importProcessor) {
    this(
        packageContext,
        rootStackFrame,
        rootStackFrame,
        registries,
        importProcessor,
        new DefaultMetaData(),
        new DefaultCancellationToken(),
        new HashSet<>());
  }

  @Override
  public Data evaluate(ValueSource valueSource) {
    switch (valueSource.getSourceCase()) {
      case FROM_LOCAL:
        return top().getVar(valueSource.getFromLocal());
      case FUNCTION_CALL:
        DefaultClosure closure = DefaultClosure.create(this, valueSource.getFunctionCall());
        if (valueSource.getFunctionCall().getBuildClosure()) {
          return closure;
        }
        return closure.execute(this);
      case CONST_STRING:
        return getDataTypeImplementation().primitiveOf(valueSource.getConstString());
      case CONST_INT:
        return getDataTypeImplementation().primitiveOf(Double.valueOf(valueSource.getConstInt()));
      case CONST_FLOAT:
        return getDataTypeImplementation().primitiveOf(valueSource.getConstFloat());
      case CONST_BOOL:
        return getDataTypeImplementation().primitiveOf(valueSource.getConstBool());
      default:
        throw new IllegalArgumentException(
            String.format("Unknown source type %s", valueSource.getSourceCase()));
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
      StackFrame.Builder frameBuilder, PackageContext localPackageContext) {
    return new DefaultRuntimeContext(
        localPackageContext,
        frameBuilder.setParent(stackTop).build(),
        stackBottom,
        registries,
        importProcessor,
        metaData,
        cancellationToken,
        monitors);
  }

  @Override
  public StackFrame top() {
    return stackTop;
  }

  @Override
  public StackFrame bottom() {
    return stackBottom;
  }

  @Override
  public ImportProcessor getImportProcessor() {
    return importProcessor;
  }

  @Override
  public DataTypeImplementation getDataTypeImplementation() {
    // TODO(): Pass from ctor
    return DefaultDataTypeImplementation.instance;
  }

  @Override
  public CancellationToken getCancellation() {
    return cancellationToken;
  }

  @Override
  public void addMonitor(RuntimeContextMonitor monitor) {
    monitors.add(monitor);
  }

  @Override
  public Data finish(Data ret) {
    if (monitors == null) {
      return ret;
    }

    Data updatedData = ret;
    for (RuntimeContextMonitor monitor : monitors) {
      updatedData = monitor.onRuntimeContextFinish(this, updatedData);
    }
    return updatedData;
  }

  /**
   * Non-generated override of equals method to provide a logical equivalence check for
   * DefaultRuntimeContext.
   *
   * @param object The object on which to execute the comparison
   * @return true if equal, otherwise false
   */
  @Override
  public boolean equals(@Nullable Object object) {
    if (!(object instanceof DefaultRuntimeContext)) {
      return false;
    }
    DefaultRuntimeContext that = (DefaultRuntimeContext) object;
    return Objects.equals(this.getCurrentPackageContext(), that.getCurrentPackageContext())
        && Objects.equals(this.top(), that.top())
        && Objects.equals(this.bottom(), that.bottom())
        && Objects.equals(this.metaData, that.getMetaData());
  }

  /**
   * Non-generated Override of hashCode on the class to provide the basis for testing for logical
   * equivalence.
   *
   * @return int hash value of the class.
   */
  @Override
  public int hashCode() {
    return Objects.hash(
        this.getCurrentPackageContext(), this.top(), this.bottom(), this.getMetaData());
  }

  /** Creates a new DefaultRuntimeContext. */
  public static class DefaultImplementation implements RuntimeContextImplementation {
    private final CancellationToken cancellationToken;

    public DefaultImplementation() {
      cancellationToken = new DefaultCancellationToken();
    }

    public DefaultImplementation(CancellationToken cancellationToken) {
      this.cancellationToken = cancellationToken;
    }

    @Override
    public RuntimeContext constructMainContext(
        PackageContext packageContext,
        StackFrame stackTop,
        StackFrame stackBottom,
        Registries registries,
        ImportProcessor importProcessor,
        MetaData metaData) {
      return new DefaultRuntimeContext(
          packageContext,
          stackTop,
          stackBottom,
          registries,
          importProcessor,
          metaData,
          cancellationToken,
          new HashSet<>());
    }
  }

  private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
    input.defaultReadObject();

    RuntimeContextComponentSerializer<Registries> regSerializer = null;
    if (metaData != null && metaData.getSerializableMeta(REGISTRIES_SERIALIZER_KEY) != null) {
      regSerializer = metaData.getSerializableMeta(REGISTRIES_SERIALIZER_KEY);
    }

    if (regSerializer == null) {
      registries = (Registries) input.readObject();
    } else {
      registries = regSerializer.deserialize(input);
    }
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();

    RuntimeContextComponentSerializer<Registries> regSerializer = null;
    if (metaData != null && metaData.getSerializableMeta(REGISTRIES_SERIALIZER_KEY) != null) {
      regSerializer = metaData.getSerializableMeta(REGISTRIES_SERIALIZER_KEY);
    }

    if (regSerializer == null) {
      stream.writeObject(registries);
    } else {
      regSerializer.serialize(registries, stream);
    }
  }

  /** Produces a deterministic hash of registries based on the Whistle code used to construct it. */
  public static int hashRegistries(Registries registries) {
    return Arrays.hashCode(
        registries.getAllRegisteredPackages().stream()
            .flatMap(p -> registries.getFunctionRegistry(p).getAll().stream())
            .filter(WhistleFunction.class::isInstance)
            .map(WhistleFunction.class::cast)
            .map(WhistleFunction::getPipelineConfig)
            .mapToInt(PipelineConfig::hashCode)
            .distinct()
            .sorted()
            .toArray());
  }
}
