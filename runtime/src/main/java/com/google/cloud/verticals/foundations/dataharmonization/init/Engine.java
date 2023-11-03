// Copyright 2021 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.cloud.verticals.foundations.dataharmonization.init;

import static java.util.Objects.requireNonNull;

import com.google.cloud.verticals.foundations.dataharmonization.builtins.Builtins;
import com.google.cloud.verticals.foundations.dataharmonization.builtins.BuiltinsConfig;
import com.google.cloud.verticals.foundations.dataharmonization.builtins.error.Errors;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.CancellationToken;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.MetaData;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.PackageContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.Registries;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContextImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.WrapperContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.impl.DefaultMetaData;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.impl.DefaultRegistries;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.impl.DefaultRuntimeContext.DefaultImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.function.whistle.WhistleFunction;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportProcessor;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Loader;
import com.google.cloud.verticals.foundations.dataharmonization.imports.URIParser;
import com.google.cloud.verticals.foundations.dataharmonization.imports.impl.DefaultImportProcessor;
import com.google.cloud.verticals.foundations.dataharmonization.init.DataAdapters.InputAdapter;
import com.google.cloud.verticals.foundations.dataharmonization.init.DataAdapters.OutputAdapter;
import com.google.cloud.verticals.foundations.dataharmonization.init.initializer.ConfigExtractorBase;
import com.google.cloud.verticals.foundations.dataharmonization.mocking.plugin.MockingPlugin;
import com.google.cloud.verticals.foundations.dataharmonization.mocking.registry.MockFunctionRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.mocking.registry.MockTargetRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Plugin;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;
import com.google.cloud.verticals.foundations.dataharmonization.registry.PackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.impl.DefaultPackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.impl.DefaultRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * The main entry point of Whistle ELP (go/dh-whistle-elp). Each instance of {@code Engine} class
 * saves all necessary information needed to run a data pipeline using a given mapping config.
 */
public class Engine implements AutoCloseable {
  private static final String NO_DATA_IN_EX_METAKEY = "NO_DATA_IN_EX";

  private final RuntimeContext context;
  private final CallableFunction entryPoint;
  private boolean closed = false;

  private Engine(RuntimeContext context, CallableFunction entryPoint) {
    this.context = context;
    this.entryPoint = entryPoint;
  }

  public RuntimeContext getRuntimeContext() {
    return context;
  }

  /**
   * Returns true if the given context has the no data in exceptions flag set. This means that
   * exceptions generated will have no message and errors in {@link Errors#withError} will have no
   * vars.
   */
  public static boolean isNoDataInExceptionsSet(RuntimeContext context) {
    return context.getMetaData().getSerializableMeta(NO_DATA_IN_EX_METAKEY) != null
        && context.getMetaData().<Boolean>getSerializableMeta(NO_DATA_IN_EX_METAKEY);
  }

  /**
   * Transforms the input data with the type of {@code InT} using the initialized config and returns
   * the result in type {@code OutT}. This methods serves as a base method for implementing
   * transform methode with different input/output data formats.
   *
   * @param inputAdapter adapts input from {@code InT} to {@link Data}.
   * @param outputAdapter adapts output from {@link Data} to {@code OutT}.
   * @param arg the input data to transform.
   * @param <InT> the type of input data.
   * @param <OutT> the type of output data.
   * @return the transformed result in {@code OutT}.
   */
  <InT, OutT> OutT transform(
      InputAdapter<InT> inputAdapter, OutputAdapter<OutT> outputAdapter, InT arg) {
    return outputAdapter.adapt(transform(inputAdapter.adapt(arg)));
  }

  public Data transform(Data args) {
    if (closed) {
      throw new IllegalStateException("This engine has already been closed.");
    }
    Data ret = entryPoint.call(context, args);
    return context.finish(ret);
  }

  public String transform(URI inputUri) throws IOException {
    String scheme = URIParser.getSchema(inputUri);
    Path path = URIParser.getPath(inputUri);
    ImportPath ipath = ImportPath.of(scheme, path, path.getParent());
    Loader loader = context.getRegistries().getLoaderRegistry().get(ipath.getLoader());
    byte[] input = loader.load(ipath);
    return transform(DataAdapters::fromByteArr, DataAdapters::toJSONString, input);
  }

  public String transform(String inputString) {
    return transform(DataAdapters::fromJSONString, DataAdapters::toJSONString, inputString);
  }

  @Override
  public synchronized void close() {
    if (closed) {
      return;
    }

    closed = true;
    for (Plugin plugin : context.getRegistries().getLoadedPlugins()) {
      plugin.close();
    }
  }

  /** Builder for {@link Engine}. */
  public static class Builder {
    // plugins only used for loading mock config, whose functions are not carried into the runtime
    // context.
    private static final ImmutableList<Plugin> mockOnlyPlugins =
        ImmutableList.of(new MockingPlugin());

    private final ConfigExtractorBase mainConfig;
    private final List<Plugin> defaultMainPlugins;
    private final Set<Loader> defaultLoaders = new HashSet<>();
    private final List<Plugin> defaultMockPlugins = new ArrayList<>();
    private final List<ConfigExtractorBase> mockConfigs = new ArrayList<>();

    // Flag to remove all data from exceptions/errors. False by default.
    private boolean noDataInExceptions = false;

    private Function<RuntimeContext, WrapperContext<?>> wrappers;

    /**
     * @param mainConfig {@link ConfigExtractorBase} constructed with information from main config.
     */
    public Builder(ConfigExtractorBase mainConfig) {
      this(mainConfig, new ArrayList<>(ImmutableList.of(new Builtins())));
    }

    public Builder(ConfigExtractorBase mainConfig, boolean throwWhistleTranspilationException) {
      this(
          mainConfig,
          new ArrayList<>(
              ImmutableList.of(
                  new Builtins(
                      BuiltinsConfig.builder()
                          .setThrowWhistleParserTranspilationException(
                              throwWhistleTranspilationException)
                          .build()))));
    }

    public Builder(ConfigExtractorBase mainConfig, List<Plugin> defaultMainPlugins) {
      this.mainConfig = mainConfig;
      this.defaultMainPlugins = new ArrayList<>(defaultMainPlugins);
    }

    /**
     * Added the given {@link Plugin}s to the plugins to be loaded before initializing main config
     * and mock config.
     */
    @CanIgnoreReturnValue
    public Builder withDefaultPlugins(Plugin... defaultPlugins) {
      Collections.addAll(defaultMainPlugins, defaultPlugins);
      return this;
    }

    /**
     * Added the given {@link Plugin}s to the plugins to be loaded before initializing mock config.
     */
    @CanIgnoreReturnValue
    public Builder withDefaultMockPlugins(Plugin... mockPlugins) {
      Collections.addAll(defaultMockPlugins, mockPlugins);
      return this;
    }

    @CanIgnoreReturnValue
    public Builder withDefaultLoaders(Set<Loader> loaders) {
      defaultLoaders.addAll(loaders);
      return this;
    }

    /**
     * Added a mock config for the engine initialized.
     *
     * @param mockConfigExtractor where mock config will be extracted from.
     */
    @CanIgnoreReturnValue
    public Builder addMock(ConfigExtractorBase mockConfigExtractor) {
      mockConfigs.add(mockConfigExtractor);
      return this;
    }

    /** Adds a runtime context wrapper */
    @CanIgnoreReturnValue
    public Builder withWrapper(Function<RuntimeContext, WrapperContext<?>> wrapper) {
      requireNonNull(wrapper, "RuntimeContext wrapper may not be null");
      if (this.wrappers == null) {
        this.wrappers = wrapper;
      } else {
        Function<RuntimeContext, WrapperContext<?>> prev = this.wrappers;
        this.wrappers = r -> wrapper.apply(prev.apply(r));
      }
      return this;
    }

    /**
     * Initialize an {@link Engine} from provided metadata, configs and plugins. This included
     * registering all functions from plugins and configs, extracting main config content and load
     * mock configs if provided. However, runtime context and engine are not created.
     *
     * <p>Note there is a global static int variable increment within ANTLR.
     * (https://google3/third_party/antlr/v4/java/org/antlr/v4/runtime/atn/PredictionContext.java;l=40;bpv=1;bpt=0;rcl=425960845)
     * For thread safety, engines should be initialized one at a time.
     *
     * @param metaData {@link MetaData} to carry forward.
     */
    public InitializedBuilder initialize(MetaData metaData) throws IOException {
      InitializedBuilder initializedBuilder = new InitializedBuilder(wrappers);
      // Set noDataInException flag.
      metaData.setSerializableMeta(NO_DATA_IN_EX_METAKEY, noDataInExceptions);
      initializedBuilder.metaData = metaData;
      initializedBuilder.importProcessor = new DefaultImportProcessor();
      // Run mock config to construct map from original function to mocks
      // information stored into registries
      if (!mockConfigs.isEmpty()) {
        initializedBuilder.registries = constructRegistriesFromMock();
      } else {
        initializedBuilder.registries = new DefaultRegistries();
      }

      defaultLoaders.forEach(initializedBuilder.registries.getLoaderRegistry()::register);

      // load default main plugins, which allows mock config to be initialized
      loadPlugins(initializedBuilder.registries, defaultMainPlugins, initializedBuilder.metaData);

      // initialize mock config
      if (!mockConfigs.isEmpty()) {
        loadPlugins(initializedBuilder.registries, defaultMockPlugins, initializedBuilder.metaData);
        for (ConfigExtractorBase mockConfig : mockConfigs) {
          mockConfig.initialize(
              initializedBuilder.registries,
              initializedBuilder.metaData,
              initializedBuilder.importProcessor);
        }
      }

      initializedBuilder.mainConfigProto =
          mainConfig.initialize(
              initializedBuilder.registries,
              initializedBuilder.metaData,
              initializedBuilder.importProcessor);
      initializedBuilder.packageContext =
          new PackageContext(
              ImmutableSet.of(initializedBuilder.mainConfigProto.getPackageName()),
              initializedBuilder.mainConfigProto.getPackageName(),
              mainConfig.getImportPath());
      return initializedBuilder;
    }

    /**
     * Initialize an {@link Engine} from provided configs and plugins. This included registering all
     * functions from plugins and configs, extracting main config content and load mock configs if
     * provided. However, runtime context and engine are not created. By default, it uses a new
     * created {@link DefaultMetaData}.
     *
     * <p>Note there is a global static int variable increment within ANTLR.
     * (https://google3/third_party/antlr/v4/java/org/antlr/v4/runtime/atn/PredictionContext.java;l=40;bpv=1;bpt=0;rcl=425960845)
     * For thread safety, engines should be initialized one at a time.
     */
    public InitializedBuilder initialize() throws IOException {
      return initialize(new DefaultMetaData());
    }

    private Registries constructRegistriesFromMock() throws IOException {
      List<Plugin> pluginsToLoad = new ArrayList<>(defaultMockPlugins);
      pluginsToLoad.addAll(mockOnlyPlugins);

      MetaData mockRecords = new DefaultMetaData();
      for (ConfigExtractorBase mockConfig : mockConfigs) {
        try (Engine mockLoader =
            new Builder(mockConfig)
                .withDefaultPlugins(pluginsToLoad.toArray(new Plugin[0]))
                .withDefaultPlugins(defaultMainPlugins.toArray(new Plugin[0]))
                .withDefaultLoaders(defaultLoaders)
                .initialize(mockRecords)
                .build(mockRecords, new DefaultImplementation())) {
          mockLoader.transform(NullData.instance);
        }
      }
      PackageRegistry<CallableFunction> functionRegistry =
          mockRecords.getSerializableMeta(MockingPlugin.MOCK_META_KEY) == null
              ? new DefaultPackageRegistry<>()
              : new MockFunctionRegistry(
                  mockRecords.getSerializableMeta(MockingPlugin.MOCK_META_KEY));
      PackageRegistry<Target.Constructor> targetRegistry =
          mockRecords.getSerializableMeta(MockingPlugin.MOCK_TARGET_META_KEY) == null
              ? new DefaultPackageRegistry<>()
              : new MockTargetRegistry(
                  mockRecords.getSerializableMeta(MockingPlugin.MOCK_TARGET_META_KEY));
      return new DefaultRegistries(functionRegistry, targetRegistry, new DefaultRegistry<>());
    }

    private static void loadPlugins(
        Registries registries, List<Plugin> defaultMainPlugins, MetaData metaData) {

      Set<Class<? extends Plugin>> pluginClass = new HashSet<>();
      for (Plugin plugin : defaultMainPlugins) {
        if (pluginClass.add(plugin.getClass())) {
          Plugin.load(plugin, registries, metaData);
        }
      }
    }

    @CanIgnoreReturnValue
    public Builder setNoDataInExceptions(boolean noDataInExceptions) {
      this.noDataInExceptions = noDataInExceptions;
      return this;
    }
  }

  /** Initialized Builder for {@link Engine}. */
  public static class InitializedBuilder implements Serializable {
    private final Function<RuntimeContext, WrapperContext<?>> wrapper;

    private ImportProcessor importProcessor;
    private MetaData metaData;
    private PackageContext packageContext;
    private PipelineConfig mainConfigProto;
    private Registries registries;

    public InitializedBuilder() {
      wrapper = null;
    }

    public InitializedBuilder(Function<RuntimeContext, WrapperContext<?>> wrapper) {
      this.wrapper = wrapper;
    }

    /**
     * Build an {@link Engine} from provided runtime context factory and metadata.
     *
     * @param metaData {@link MetaData} used to initialize the {@link RuntimeContext} in the {@link
     *     Engine} created and it will not be copied.
     * @param mainRtxImplementation {@link RuntimeContextImplementation} that constructs the main
     *     {@link RuntimeContext} during the mapping execution.
     */
    private synchronized Engine build(
        MetaData metaData, RuntimeContextImplementation mainRtxImplementation) {
      // TODO(): Make registries immutable.
      RuntimeContext context =
          mainRtxImplementation.constructInitialContext(
              packageContext, registries, importProcessor, metaData);

      if (wrapper != null) {
        context = wrapper.apply(context);
      }

      return new Engine(
          context,
          new WhistleFunction(mainConfigProto.getRootBlock(), mainConfigProto, packageContext));
    }

    /**
     * Build an {@link Engine} with the supplied {@link CancellationToken} implementation, which
     * will be passed to the {@link RuntimeContext} used by this engine.
     *
     * @param cancellationToken An implementation of {@link CancellationToken}.
     */
    public synchronized Engine build(CancellationToken cancellationToken) {
      return build(metaData.deepCopy(), new DefaultImplementation(cancellationToken));
    }

    /**
     * Build an {@link Engine} from provided metadata and default runtime context factory.
     *
     * @param metaData {@link MetaData} used to initialize the {@link RuntimeContext} in the {@link
     *     Engine} created and it will not be copied.
     */
    public synchronized Engine build(MetaData metaData) {
      return build(metaData, new DefaultImplementation());
    }

    /**
     * Build an {@link Engine} by default context factory and metadata initialized in {@code
     * initialize()}. Please make sure call {@code initialize()} before this method. Note that this
     * method is synchronized and it is thread-safe to call it from multiple threads to build
     * multiple {@link Engine} instances and run {@code transform()} concurrently. Please note that
     * the initialized metadata will be deep copied to make the engine thread-safe.
     */
    public synchronized Engine build() {
      return build(metaData.deepCopy(), new DefaultImplementation());
    }

    /**
     * Builds an {@link Engine} with a specialized main {@link RuntimeContextImplementation} such
     * that the resulting engine uses the {@link RuntimeContext} implementation that the {@link
     * RuntimeContextImplementation} generates. This method can be used to build engine that runs
     * alternative RuntimeContext.
     *
     * @param mainRtxImplementation {@link RuntimeContextImplementation} of the main runtime context
     *     used during the mapping execution.
     */
    public synchronized Engine build(RuntimeContextImplementation mainRtxImplementation) {
      return build(metaData.deepCopy(), mainRtxImplementation);
    }

    public PipelineConfig getMainConfigProto() {
      return this.mainConfigProto;
    }

    public Registries getRegistries() {
      return registries;
    }

    public MetaData getMetaData() {
      return metaData;
    }

    public ImportProcessor getImportProcessor() {
      return importProcessor;
    }
  }
}
