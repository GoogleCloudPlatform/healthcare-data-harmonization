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

package com.google.cloud.verticals.foundations.dataharmonization.lsp;

import static com.google.cloud.verticals.foundations.dataharmonization.imports.impl.DefaultImportProcessor.IMPORT_EXCEPTION_LIST_KEY;
import static com.google.cloud.verticals.foundations.dataharmonization.lsp.CompletionUtil.createCompletionItemFromCallableFunction;
import static com.google.cloud.verticals.foundations.dataharmonization.lsp.CompletionUtil.createCompletionItemFromTargetConstructor;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FunctionInfo.FunctionType;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.Source;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.SourcePosition;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.Symbols;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.ImportException;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.Registries;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.impl.DefaultMetaData;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Loader;
import com.google.cloud.verticals.foundations.dataharmonization.imports.URIParser;
import com.google.cloud.verticals.foundations.dataharmonization.imports.impl.DefaultImportProcessor;
import com.google.cloud.verticals.foundations.dataharmonization.imports.impl.PluginClassLoader;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine.InitializedBuilder;
import com.google.cloud.verticals.foundations.dataharmonization.init.initializer.InlineConfigExtractor;
import com.google.cloud.verticals.foundations.dataharmonization.lsp.SymbolLookup.SymbolWithPosition;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Plugin;
import com.google.cloud.verticals.foundations.dataharmonization.registry.PackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target.Constructor;
import com.google.common.base.Strings;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.flogger.GoogleLogger;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

/**
 * Class which holds static methods that are called to initialization a Whistle engine object from a
 * Whistle document. The list of requested plugins to load is retrieved and this information is used
 * to update the state of the plugins loaded for a particular document.
 */
public final class EngineUtil {

  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();
  public static final String ENGINE_INIT_DIAGNOSTICS = "Engine Initialization";

  private final SymbolLookup symbolLookup;
  private final Set<Loader> loaders;
  // Map which stores a map of completion items per documentURI key, per plugin package key
  HashBasedTable<String, String, Set<CompletionItem>> pluginCompletionItems =
      HashBasedTable.create();
  // Map to store user-defined completion items per documentURI key. User-defined completion items
  // include completion objects for functions that a user defines in their whistle configs, as
  // opposed to pluginCompletionItems which store completion objects generated from any imported
  // Whistle plugins (for plugin targets and functions).
  private final Map<String, Set<CompletionItem>> userDefinedCompletionItems = new HashMap<>();
  private final Map<String, Set<String>> loadedFunctionRegistries = new HashMap<>();
  private final Map<String, Set<String>> loadedTargetRegistries = new HashMap<>();
  private final Map<String, Set<String>> documentImportURIs = new HashMap<>();
  private final DiagnosticMessageCollector diagnosticMessageCollector;

  public EngineUtil(Set<Loader> loaders, DiagnosticMessageCollector diagnosticMessageCollector) {
    this.symbolLookup = new SymbolLookup();
    this.loaders = loaders;
    this.diagnosticMessageCollector = diagnosticMessageCollector;
  }

  public void didOpen(String documentURI) {
    loadedFunctionRegistries.put(documentURI, new HashSet<>());
    loadedTargetRegistries.put(documentURI, new HashSet<>());
    userDefinedCompletionItems.put(documentURI, new HashSet<>());
    documentImportURIs.put(documentURI, new HashSet<>());
    symbolLookup.openDocument(documentURI);
  }

  public void didChange(String documentURI) {
    // Reset the userDefined completion items, definition locations and imports for this document
    userDefinedCompletionItems.put(documentURI, new HashSet<>());
    symbolLookup.changeDocument(documentURI);
    documentImportURIs.put(documentURI, new HashSet<>());
  }

  public void didClose(String documentURI) {
    pluginCompletionItems.column(documentURI).clear();
    userDefinedCompletionItems.remove(documentURI);
    loadedFunctionRegistries.remove(documentURI);
    loadedTargetRegistries.remove(documentURI);
    documentImportURIs.remove(documentURI);
    symbolLookup.evictDocument(documentURI);
  }

  public ImmutableTable<String, String, Set<CompletionItem>> getPluginCompletionItems() {
    return ImmutableTable.copyOf(pluginCompletionItems);
  }

  public ImmutableMap<String, Set<CompletionItem>> getUserDefinedCompletionItems() {
    return ImmutableMap.copyOf(userDefinedCompletionItems);
  }

  public ImmutableMap<String, Set<String>> getDocumentImportURIs() {
    return ImmutableMap.copyOf(documentImportURIs);
  }

  public SymbolWithPosition getReferenceSymbolFromPosition(String documentUri, int line, int col) {
    return symbolLookup.getReferenceSymbolFromPosition(documentUri, line, col);
  }

  public ImmutableSet<String> getDefinitionSymbolsFromPosition(
      String documentUri, int line, int col) {
    return symbolLookup.getDefinitionSymbolsFromPosition(documentUri, line, col);
  }

  /**
   * Returns a list of {@link Location} items which refers to either a variable or function
   * definition, given a line and column position from where the goto command was issued. Line and
   * column input are zero-based as per the LSP {@link Position} spec.
   *
   * @param documentUri DocumentURI for the document from where the command is being called
   * @param lineNumber 0-based line number from where the command was issued
   * @param columnPosition 0-based column number from where the command was issued
   * @return a list containing a single {@link Location} item.
   */
  public ImmutableList<Location> fetchLocationFromPosition(
      String documentUri, int lineNumber, int columnPosition) {
    return symbolLookup.fetchLocationFromPosition(
        documentUri, lineNumber, columnPosition, getDocumentImportURIs().get(documentUri));
  }

  /**
   * Method which reads in a whistle file and initializes an engine object from it. Registries from
   * the {@link Engine.InitializedBuilder} object are then used to: - Load {@link CompletionItem}s
   * for user defined whistle-functions - Load {@link CompletionItem}s for any imported plugin
   * functions and targets - Storing the associated {@link Symbols} present in this file - Updating
   * the map which tracks the whistle files this file is importing.
   *
   * @param documentURI The location of the file being loaded.
   * @param documentText The text body of the file being loaded.
   */
  @Nullable
  @CanIgnoreReturnValue
  public InitializedBuilder loadEngine(String documentURI, String documentText, String importRoot) {
    try {
      // Build the ImportPath from the given jupyter documentURI.
      String schema = URIParser.getSchema(URI.create(documentURI));
      Path mappingPath = URIParser.getPath(URI.create(documentURI));

      Path rootDir;
      if (Strings.isNullOrEmpty(importRoot)) {
        rootDir = mappingPath.getParent();
      } else {
        rootDir = Paths.get(importRoot);
      }
      ImportPath mappingImportPath = ImportPath.of(schema, mappingPath, rootDir);

      // Add a list to the metadata which will store any ImportExceptions.
      DefaultMetaData metadata = new DefaultMetaData();
      metadata.setMeta(IMPORT_EXCEPTION_LIST_KEY, new ArrayList<ImportException>());
      // Init engine with the loaded file and get all loaded plugins
      InitializedBuilder engine =
          new Engine.Builder(
                  InlineConfigExtractor.of(documentText, mappingImportPath, false), false)
              .withDefaultLoaders(loaders)
              .initialize(metadata);

      Registries registries = engine.getRegistries();

      // Currently present plugins retrieved.
      Set<String> pluginsFromEngine =
          registries.getLoadedPlugins().stream()
              .map(Plugin::getPackageName)
              .collect(Collectors.toSet());

      // Remove any unused plugins for this document
      removePlugins(
          pluginsFromEngine,
          pluginCompletionItems.column(documentURI),
          loadedFunctionRegistries.get(documentURI),
          loadedTargetRegistries.get(documentURI));

      // Create completion items from registries
      loadCompletionItems(
          documentURI,
          engine,
          pluginCompletionItems,
          loadedFunctionRegistries,
          loadedTargetRegistries,
          userDefinedCompletionItems);

      // Keep track of the imported files for the document being processed, and init the
      // symbolLookup maps for them.
      // TODO(): Make importPaths part of the ImportProcessor interface
      DefaultImportProcessor importProcessor = (DefaultImportProcessor) engine.getImportProcessor();
      ImmutableSet<ImportPath> importPaths =
          importProcessor.getImportPaths().stream()
              .filter(i -> !i.getLoader().equals(PluginClassLoader.NAME))
              .collect(toImmutableSet());
      importPaths.forEach(
          i -> {
            documentImportURIs.get(documentURI).add(i.toFileInfo().getUrl());
            symbolLookup.openDocument(i.toFileInfo().getUrl());
          });

      // Load location definitions
      loadSymbolsFromRegistries(symbolLookup, engine);

      // Add any import exceptions to the diagnostics.
      List<ImportException> importExceptions = metadata.getMeta(IMPORT_EXCEPTION_LIST_KEY);
      List<ImportExceptionDiagnostic> importDiagnostics =
          createDiagnosticSourcesFromImportException(importExceptions);
      importDiagnostics.forEach(
          d ->
              diagnosticMessageCollector.addDiagnosticsToDocumentURI(
                  d.getDocumentURI(), d.getDiagnostic()));

      // Add a diagnostic to the root file to indicate which of its imports are creating diagnostic
      // errors. This is primarily being done due to Jupyters inablility to publish diagnostics for
      // files which have not explictily been opened.
      if (!importDiagnostics.isEmpty()) {
        ImmutableSet<String> importedURIsWithErrors =
            importDiagnostics.stream()
                .map(ImportExceptionDiagnostic::getDocumentURI)
                .filter(uri -> !uri.equals(documentURI))
                .collect(toImmutableSet());
        if (!importedURIsWithErrors.isEmpty()) {
          // Only publish the diagnostic if the set is non-empty.
          Diagnostic rootFileImportDiagnostics =
              new Diagnostic(
                  new Range(new Position(0, 0), new Position(0, 2)),
                  String.format("Errors in imported files: %s", importedURIsWithErrors),
                  DiagnosticSeverity.Error,
                  ENGINE_INIT_DIAGNOSTICS);
          diagnosticMessageCollector.addDiagnosticsToDocumentURI(
              documentURI, rootFileImportDiagnostics);
        }
      }
      return engine;

    } catch (IOException | RuntimeException e) {
      logger.atSevere().withCause(e).log(
          "Error while trying to init Engine instance for file %s.", documentURI);
      return null;
    }
  }

  /**
   * Helper method which is used to update the loadedFunction and loadedTarget registries for a
   * document. Based on the plugins obtained from the {@link Registries} object, the loadedFunction
   * and loadedTarget sets are updated so any plugins which are no longer present in the engine
   * registries are removed for a given document.
   *
   * @param pluginsFromEngine The latest plugins retrieved from an Engine instance which loaded the
   *     associated wstl file.
   * @param documentPluginCompletionItems A map which stores a set of generated completionItems per
   *     Whistle plugin package for a document.
   * @param documentLoadedFunctionRegistries A set which stores the loaded function registries for a
   *     documentURI.
   * @param documentLoadedTargetRegistries A set which stores the loaded targets registries for a
   *     documentURI.
   */
  private void removePlugins(
      Set<String> pluginsFromEngine,
      Map<String, Set<CompletionItem>> documentPluginCompletionItems,
      Set<String> documentLoadedFunctionRegistries,
      Set<String> documentLoadedTargetRegistries) {

    // Get the plugin packages that are no longer present.
    SetView<String> functionRegistryDiff =
        Sets.difference(documentLoadedFunctionRegistries, pluginsFromEngine);
    SetView<String> targetRegistryDiff =
        Sets.difference(documentLoadedTargetRegistries, pluginsFromEngine);

    // Remove the set of completion items for unused plugin packages from this document.
    documentPluginCompletionItems.keySet().removeAll(functionRegistryDiff);
    documentPluginCompletionItems.keySet().removeAll(targetRegistryDiff);

    // Remove the unused plugins from the target and function registries maps
    documentLoadedFunctionRegistries.removeAll(functionRegistryDiff);
    documentLoadedTargetRegistries.removeAll(targetRegistryDiff);
  }

  /**
   * Helper which builds a completion list for a given document.
   *
   * @param documentURI documentURI The location of the file being loaded
   * @param engine The {@link InitializedBuilder} engine object, for the whistle file being
   *     processed.
   * @param pluginCompletionItems A map which stores a map of generated completionItems per Whistle
   *     plugin package, per documentURI.
   * @param loadedFunctionRegistries A map which to store the loaded function registries per
   *     documentURI key
   * @param loadedTargetRegistries A map which to store the loaded targets registries per
   *     documentURI key
   * @param userDefinedCompletionItems A map which stores a set of {@link CompletionItem}s which are
   *     created from any user-defined functions in .wstl files, per documentURI key.
   */
  private void loadCompletionItems(
      String documentURI,
      InitializedBuilder engine,
      HashBasedTable<String, String, Set<CompletionItem>> pluginCompletionItems,
      Map<String, Set<String>> loadedFunctionRegistries,
      Map<String, Set<String>> loadedTargetRegistries,
      Map<String, Set<CompletionItem>> userDefinedCompletionItems) {

    Registries registries = engine.getRegistries();
    // CompletionItem map for this document, where keys are the packages the completionItem
    // belongs to.
    Map<String, Set<CompletionItem>> documentPluginCompletionItems =
        pluginCompletionItems.column(documentURI);

    // Use the registries loadedPlugins() method to get the whistle core plugins loaded.
    Set<String> whistleCorePlugins =
        registries.getLoadedPlugins().stream()
            .map(Plugin::getPackageName)
            .collect(Collectors.toSet());

    for (String functionPackageName : registries.getAllRegisteredPackages()) {
      PackageRegistry<CallableFunction> functionRegistry =
          registries.getFunctionRegistry(functionPackageName);
      Set<CallableFunction> functionSet = functionRegistry.getAllInPackage(functionPackageName);

      if (whistleCorePlugins.contains(functionPackageName)) {
        // Load a whistle plugin function registry.
        loadWhistleCoreFunctions(
            functionSet,
            documentPluginCompletionItems,
            loadedFunctionRegistries.get(documentURI),
            functionPackageName);
      } else {
        // Load user defined function registry completion items.
        loadFunctionPackageToCompletionItem(
            functionSet,
            functionPackageName,
            userDefinedCompletionItems.get(documentURI),
            engine.getMainConfigProto().getPackageName(),
            true);
      }
    }
    // Load target registries
    loadWhistleCoreTargets(
        registries, documentPluginCompletionItems, loadedTargetRegistries.get(documentURI));
  }

  /**
   * Helper method which loads in Whistle core functions for a documentURI.
   *
   * @param functions The set of functions to be loaded.
   * @param documentPluginCompletionItems A map of Whistle plugin package names to set of {@link
   *     CompletionItem}s for this documentURI.
   * @param documentFunctionPackageRegistries Set of function packages loaded for this documentURI.
   * @param functionPackageName The function package name which is being loaded from registries.
   */
  private void loadWhistleCoreFunctions(
      Set<CallableFunction> functions,
      Map<String, Set<CompletionItem>> documentPluginCompletionItems,
      Set<String> documentFunctionPackageRegistries,
      String functionPackageName) {

    if (!documentFunctionPackageRegistries.contains(functionPackageName)) {
      logger.atInfo().log("Loading core plugin function package %s.", functionPackageName);
      documentPluginCompletionItems.put(functionPackageName, new HashSet<>());
      loadFunctionPackageToCompletionItem(
          functions,
          functionPackageName,
          documentPluginCompletionItems.get(functionPackageName),
          "",
          false);
      // Save the package to the document function registry map it does not reload.
      documentFunctionPackageRegistries.add(functionPackageName);
    }
  }

  /**
   * Helper method which loads a function package from the engine Registry and converts it into
   * {@link CompletionItem}s which then gets save to the appropriate completionItem Set.
   *
   * @param functions The set of functions to be loaded.
   * @param functionPackageName The function package name to be loaded.
   * @param completionItemSet Set which will store the created {@link CompletionItem}s.
   */
  private void loadFunctionPackageToCompletionItem(
      Set<CallableFunction> functions,
      String functionPackageName,
      Set<CompletionItem> completionItemSet,
      String rootConfigPackageName,
      boolean isUserDefinedFunctions) {

    for (CallableFunction function : functions) {
      if (isValidWhistleFunction(function)) {
        CompletionItem completionItem =
            createCompletionItemFromCallableFunction(
                function, functionPackageName, rootConfigPackageName, isUserDefinedFunctions);
        completionItemSet.add(completionItem);
      }
    }
  }

  /**
   * Helper method which makes a call to {@link SymbolLookup} to process {@link Symbols} for a given
   * file using objects from the {@link Registries}.
   *
   * @param symbolLookup Instance of {@link SymbolLookup} which manages maps for symbols and
   *     location definitions.
   * @param engine The instantiated {@link Engine} instance created from the Whistle file being
   *     processed.
   */
  private void loadSymbolsFromRegistries(SymbolLookup symbolLookup, InitializedBuilder engine) {
    symbolLookup.loadSymbolsFromRegistries(engine.getMainConfigProto(), engine.getRegistries());
  }

  /**
   * Helper method which determines if a function fits the criteria to be included as an
   * auto-completable option.
   *
   * @param fxn a Callable function for which an auto-complete entry may be created.
   * @return boolean status of whether this function is to be loaded.
   */
  private boolean isValidWhistleFunction(CallableFunction fxn) {
    return (fxn.getDebugInfo() != null
            && (fxn.getDebugInfo().getFunctionInfo().getType().equals(FunctionType.NATIVE)
                || fxn.getDebugInfo().getFunctionInfo().getType().equals(FunctionType.DECLARED)))
        || !fxn.getSignature().getInheritsParentVars();
  }

  /**
   * Helper method which creates {@link CompletionItem}s from the target registries and loads them
   * for a document.
   *
   * @param registries The The {@link Registries} object which was obtained from an {@link
   *     InitializedBuilder} engine object, for the whistle file.
   * @param documentPluginCompletionItems A map of Whistle plugin package names to set of {@link
   *     CompletionItem}s for this documentURI.
   * @param documentLoadedTargetRegistries Set of target packages loaded for this documentURI.
   */
  private void loadWhistleCoreTargets(
      Registries registries,
      Map<String, Set<CompletionItem>> documentPluginCompletionItems,
      Set<String> documentLoadedTargetRegistries) {
    // Load targets if not already loaded for this document.
    PackageRegistry<Constructor> targetRegistry = registries.getTargetRegistry();
    for (String targetPackageName : targetRegistry.getAllRegisteredPackages()) {
      // Add to an existing package, which may have been created when functions were added.
      documentPluginCompletionItems.computeIfAbsent(targetPackageName, val -> new HashSet<>());
      if (!documentLoadedTargetRegistries.contains(targetPackageName)) {
        logger.atInfo().log("Loading target registry for package %s", targetPackageName);
        Set<Constructor> targets = targetRegistry.getAllInPackage(targetPackageName);
        for (Constructor targetConstructor : targets) {
          CompletionItem completionItem =
              createCompletionItemFromTargetConstructor(targetConstructor, targetPackageName);
          documentPluginCompletionItems.get(targetPackageName).add(completionItem);
        }
        // Save the targetPackage to this document, so it does not have to be reloaded.
        documentLoadedTargetRegistries.add(targetPackageName);
      }
    }
  }

  /**
   * Given a list of {@link ImportException}s, returns a corresponding list of {@link
   * ImportExceptionDiagnostic} items.
   *
   * <p>Since a file can import other files, which may in addition have its own imports. We return a
   * list of {@link ImportExceptionDiagnostic}s which allows us to keep track of which file an
   * ImportException originates from, and at the time of publishing Diagnostics, publish the
   * diagnostic to the relevant file instead of the root file we may be processing.
   *
   * @param importExceptions List of importExceptions.
   * @return List of DiagnosticSources.
   */
  private ImmutableList<ImportExceptionDiagnostic> createDiagnosticSourcesFromImportException(
      List<ImportException> importExceptions) {

    return importExceptions.stream()
        .map(
            i -> {
              Source source = i.getSource();
              // These return SourcePosition.getDefaultInstance() if start and end do not exist in
              // source.
              SourcePosition startPos = source.getStart();
              SourcePosition endPos = source.getEnd();
              // Subtract 1 from the line position to make it 0-based as required by the LSP spec.
              // Columns are already provided as 0-based.
              Diagnostic diagnostic =
                  DiagnosticMessageCollector.createDiagnostic(
                      startPos.getLine() - 1,
                      startPos.getColumn(),
                      endPos.getLine() - 1,
                      endPos.getColumn(),
                      i.getMessage(),
                      ENGINE_INIT_DIAGNOSTICS,
                      DiagnosticSeverity.Error);
              return new ImportExceptionDiagnostic(diagnostic, i.getCurrentPath());
            })
        .collect(toImmutableList());
  }

  /**
   * Wrapper class which holds an importExceptions Diagnostic representation, and the file it
   * originated from.
   */
  static class ImportExceptionDiagnostic {
    private final Diagnostic diagnostic;
    private final String documentURI;

    ImportExceptionDiagnostic(Diagnostic diagnostic, String documentURI) {
      this.diagnostic = diagnostic;
      this.documentURI = documentURI;
    }

    public Diagnostic getDiagnostic() {
      return diagnostic;
    }

    public String getDocumentURI() {
      return documentURI;
    }

    @Override
    public int hashCode() {
      return Objects.hash(diagnostic, documentURI);
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (!(o instanceof ImportExceptionDiagnostic)) {
        return false;
      }
      ImportExceptionDiagnostic other = (ImportExceptionDiagnostic) o;
      return other.getDiagnostic().equals(diagnostic) && other.getDocumentURI().equals(documentURI);
    }
  }
}
