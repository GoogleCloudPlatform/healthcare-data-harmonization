/*
 * Copyright 2022 Google LLC.
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

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.toCollection;

import com.google.cloud.verticals.foundations.dataharmonization.Transpiler;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FileInfo;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.Source;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.SourcePosition;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.SymbolReference;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.Symbols;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.Registries;
import com.google.cloud.verticals.foundations.dataharmonization.function.whistle.WhistleFunction;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FieldMapping;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.Meta;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.GoogleLogger;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

/**
 * Class which holds static helper methods that create Language Server {@link Location} items from
 * Whistle Domain Objects.
 */
public final class SymbolLookup {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();
  // Maps which hold Symbols for definition and reference objects per documentUri key.
  private final Map<String, Set<SymbolWithPosition>> documentUriToDefinitionSymbols;
  private final Map<String, Set<SymbolWithPosition>> documentUriToWriteSymbols;
  private final Map<String, Set<SymbolWithPosition>> documentUriToReferenceSymbols;
  private static final String FILE_INFO_KEY = FileInfo.getDescriptor().getFullName();
  private static final String SOURCE_KEY = Source.getDescriptor().getFullName();
  private static final String SYMBOL_KEY = Symbols.getDescriptor().getFullName();
  private static final String ROOT_FUNCTION_SUFFIX = "_root_function";

  public SymbolLookup() {
    documentUriToDefinitionSymbols = new HashMap<>();
    documentUriToReferenceSymbols = new HashMap<>();
    documentUriToWriteSymbols = new HashMap<>();
  }

  /**
   * Called when a document is opened, and initializes a map which holds {@link Location} items for
   * this document.
   *
   * @param documentURI Associated documentURI.
   */
  public void openDocument(String documentURI) {
    documentUriToDefinitionSymbols.put(documentURI, new HashSet<>());
    documentUriToReferenceSymbols.put(documentURI, new HashSet<>());
    documentUriToWriteSymbols.put(documentURI, new HashSet<>());
  }


  /**
   * Called when a document is changed, and resets the map which holds {@link Location} items for
   * this document.
   *
   * @param documentURI Associated documentURI.
   */
  public void changeDocument(String documentURI) {
    documentUriToDefinitionSymbols.put(documentURI, new HashSet<>());
    documentUriToReferenceSymbols.put(documentURI, new HashSet<>());
    documentUriToWriteSymbols.put(documentURI, new HashSet<>());
  }

  /**
   * Called when a document is closed, and removes the map which holds {@link Location} items for
   * the document.
   *
   * @param documentURI Associated documentURI.
   */
  public void evictDocument(String documentURI) {
    documentUriToDefinitionSymbols.remove(documentURI);
    documentUriToReferenceSymbols.remove(documentURI);
    documentUriToWriteSymbols.remove(documentURI);
  }

  /**
   * Returns a list containing a single {@link Location} item which refers to either a variable or
   * function definition, given a line and column position from where the goto command was issued.
   * Line and column input are zero-based as per the LSP {@link Position} spec.
   *
   * @param documentUri DocumentURI for the document from where the command is being called
   * @param lineNumber 0-based line number from where the command was issued
   * @param columnPosition 0-based column number from where the command was issued
   * @return a list containing a single {@link Location} item
   */
  ImmutableList<Location> fetchLocationFromPosition(
      String documentUri, int lineNumber, int columnPosition, Set<String> documentImportURIs) {

    SymbolWithPosition referenceSymbol =
        documentUriToReferenceSymbols.get(documentUri).stream()
            .filter(
                symbolSourceReference ->
                    lineAndColumnInSource(
                        symbolSourceReference.getZeroBasedSource(), lineNumber, columnPosition))
            .min(this::compareSymbolRefSize)
            .orElse(null);

    if (referenceSymbol == null) {
      // Try searching through write symbols
      SymbolWithPosition writeSymbol =
          documentUriToWriteSymbols.get(documentUri).stream()
              .filter(
                  symbolSourceReference ->
                      lineAndColumnInSource(
                          symbolSourceReference.getZeroBasedSource(), lineNumber, columnPosition))
              .min(this::compareSymbolRefSize)
              .orElse(null);
      if (writeSymbol != null) {
        // Search through both other write and definition symbols for this write symbol
        ImmutableList<Location> foundDefSymbol =
            searchSymbolCollection(
                documentUriToDefinitionSymbols.get(documentUri), writeSymbol, lineNumber);
        return !foundDefSymbol.isEmpty()
            ? foundDefSymbol
            : searchSymbolCollection(
                documentUriToWriteSymbols.get(documentUri), writeSymbol, lineNumber);
      }
      // Nothing found, no symbols at this location.
      logger.atInfo().log("No symbol found at position %s:%s", lineNumber, columnPosition);
      return ImmutableList.of();
    }
    // Search through both write and definition symbols for the reference symbol
    ImmutableList<Location> foundWriteSymbol =
        searchSymbolCollection(
            documentUriToWriteSymbols.get(documentUri), referenceSymbol, lineNumber);

    // If no write symbols are found in the current file, we search through all definition symbols
    // in the current file, and any imports it includes.
    return !foundWriteSymbol.isEmpty()
        ? foundWriteSymbol
        : searchDefinitionSymbols(documentUri, referenceSymbol, documentImportURIs, lineNumber);
  }

  /**
   * Search the current documentURI and its imports for the definition Symbol of the given reference
   * Symbol.
   */
  private ImmutableList<Location> searchDefinitionSymbols(
      String documentUri,
      SymbolWithPosition referenceSymbol,
      Set<String> documentImportURIs,
      int lineNumber) {
    ArrayList<Location> foundDefinitionSymbols = new ArrayList<>();
    // Search through the imported documents, as well as the current document.
    documentImportURIs.add(documentUri);
    for (String documentImport : documentImportURIs) {
      ImmutableList<Location> foundDefinitions =
          searchSymbolCollection(
              documentUriToDefinitionSymbols.getOrDefault(documentImport, new HashSet<>()),
              referenceSymbol,
              lineNumber);
      foundDefinitionSymbols.addAll(foundDefinitions);
    }
    return ImmutableList.copyOf(foundDefinitionSymbols);
  }

  /**
   * Given a line and column position within a given document, looks through the referenceSymbol map
   * and returns the corresponding {@link SymbolWithPosition} object at that position.
   *
   * @param documentUri The document being processed
   * @param lineNumber The line number
   * @param columnPosition The column number
   * @return The SymbolWithPosition object at that position, null if none found.
   */
  @Nullable
  public SymbolWithPosition getReferenceSymbolFromPosition(
      String documentUri, int lineNumber, int columnPosition) {
    // Only search through reference symbols
    return documentUriToReferenceSymbols.get(documentUri).stream()
        .filter(
            symbolSourceReference ->
                lineAndColumnInSource(
                    symbolSourceReference.getZeroBasedSource(), lineNumber, columnPosition))
        .min(this::compareSymbolRefSize)
        .orElse(null);
  }

  /**
   * Given a line and column position in the document, the function should return the name of the
   * function (scope) being looked up. If the lookup is not within a local function, it should
   * return "root_function".
   *
   * @param documentUri The document being processed
   * @param lineNumber The line number
   * @param columnPosition The column number
   * @return return the function name.
   */
  private ImmutableSet<String> getScopeFromPosition(
      String documentUri, int lineNumber, int columnPosition) {
    ImmutableSet<String> scopes =
        documentUriToDefinitionSymbols.get(documentUri).stream()
            .filter(
                definitionSymbol ->
                    (definitionSymbol.getSymbolReference().getType()
                            == SymbolReference.Type.FUNCTION
                        && lineAndColumnInSource(
                            definitionSymbol.getZeroBasedSource(), lineNumber, columnPosition)))
            .map(SymbolWithPosition::getSymbolReference)
            .map(SymbolReference::getName)
            .collect(toImmutableSet());

    var defaultDocumentUri = documentUriToDefinitionSymbols.get(documentUri);
    ImmutableSet<String> defaultScope =
        defaultDocumentUri.isEmpty()
            ? ImmutableSet.of()
            : ImmutableSet.of(
                defaultDocumentUri.iterator().next().getPackageName() + ROOT_FUNCTION_SUFFIX);

    return scopes.isEmpty() ? defaultScope : scopes;
  }

  /**
   * Given a line and column position in the document, return the names of the variables within the
   * scope of that position.
   *
   * @param documentUri The document being processed
   * @param lineNumber The line number
   * @param columnPosition The column number
   * @return Set of variables within the current scope.
   */
  public ImmutableSet<String> getDefinitionSymbolsFromPosition(
      String documentUri, int lineNumber, int columnPosition) {
    ImmutableSet<String> scopes = getScopeFromPosition(documentUri, lineNumber, columnPosition);
    return documentUriToDefinitionSymbols.get(documentUri).stream()
        .filter(
            definitionSymbol ->
                (definitionSymbol.getSymbolReference().getType() == SymbolReference.Type.VARIABLE
                    && scopes.contains(definitionSymbol.getSymbolReference().getEnvironment())
                    && lineAndColumnAfterSource(
                        definitionSymbol.getZeroBasedSource(), lineNumber, columnPosition)))
        .map(SymbolWithPosition::getSymbolReference)
        .map(SymbolReference::getName)
        .collect(toImmutableSet());
  }

  /**
   * Given a {@link Source} object, which was generated from the {@link Transpiler}, and a user
   * provided line amd column to look up, determines if the position of this Source is before the
   * line and column that are being looked up.
   *
   * @param source Source object which uses 1-based lines and 0-based column references.
   * @param line 1-based line number
   * @param column 0-based provided column
   * @return Boolean indicating whether the line and column is after the Source's position.
   */
  private boolean lineAndColumnAfterSource(Source source, int line, int column) {
    int endLine = source.getEnd().getLine();
    int endColumn = source.getEnd().getColumn();
    return line == endLine ? column > endColumn : line > endLine;
  }

  private ImmutableList<Location> searchSymbolCollection(
      Set<SymbolWithPosition> symbolCollection, SymbolWithPosition symbol, int lineNumber) {
    // Return locations created from the found symbols reference.
    Location foundLocation =
        symbolCollection.stream()
            .filter(
                s ->
                    isVarSymbolDefinitionBeforeSymbolRef(symbol, s)
                        && s.getSymbolReference()
                            .getName()
                            .equals(symbol.getSymbolReference().getName())
                        && s.getSymbolReference()
                            .getType()
                            .equals(symbol.getSymbolReference().getType())
                        && s.getPackageName().equals(symbol.getPackageName())
                        && Integer.valueOf(s.getNumArgs()).equals(symbol.getNumArgs())
                        && s.getSymbolReference()
                            .getEnvironment()
                            .equals(symbol.getSymbolReference().getEnvironment()))
            .map(d -> createLocation(d.getFileUri(), d.getZeroBasedSource()))
            .min((loc1, loc2) -> sortByClosestLocations(loc1, loc2, lineNumber))
            .orElse(null);

    return foundLocation != null ? ImmutableList.of(foundLocation) : ImmutableList.of();
  }

  /**
   * For a {@link SymbolReference.Type} of type Variable returns whether the definition Symbol being
   * looked up appears before it in the Whistle File, as variables are not hoisted in Whistle and
   * references can only be called after declarations.
   *
   * <p>Return true otherwise.
   *
   * @param symbolRef The reference symbol
   * @param symbolDef The definition (or write) symbol
   * @return Boolean indicating whether the condition is met.
   */
  private boolean isVarSymbolDefinitionBeforeSymbolRef(
      SymbolWithPosition symbolRef, SymbolWithPosition symbolDef) {
    return !symbolRef.getSymbolReference().getType().equals(SymbolReference.Type.VARIABLE)
        || symbolDef.getZeroBasedSource().getStart().getLine()
            < symbolRef.getZeroBasedSource().getStart().getLine();
  }

  private Location createLocation(String uri, Source source) {
    SourcePosition start = source.getStart();
    SourcePosition end = source.getEnd();

    Position positionStart = new Position(start.getLine(), start.getColumn());
    Position positionEnd = new Position(end.getLine(), end.getColumn());
    Range range = new Range(positionStart, positionEnd);
    return new Location(uri, range);
  }

  /**
   * Given a {@link Source} object, which was generated from the {@link Transpiler}, and a user
   * provided line amd column to look up, determines if the position of this Source is contained in
   * the line and column which is being looked up.
   *
   * @param source Source object which uses 1-based lines and 0-based column references.
   * @param line 1-based line number
   * @param column 0-based provided column
   * @return Boolean indicating whether the line and column is contained within a Source's position.
   */
  private boolean lineAndColumnInSource(Source source, int line, int column) {
    int startLine = source.getStart().getLine();
    int startColumn = source.getStart().getColumn();
    int endLine = source.getEnd().getLine();
    int endColumn = source.getEnd().getColumn();

    // Adding a +1 to the end column, this is due to b/229113462.
    return startLine == endLine
        ? line >= startLine && line <= endLine && column >= startColumn && column <= endColumn + 1
        : line >= startLine && line <= endLine;
  }

  /**
   * Given two symbol references performs a comparison to determine which of the symbol references
   * is a nested reference.
   *
   * @param sr1 The first {@link SymbolReference}
   * @param sr2 The second {@link SymbolReference}
   * @return a value less than 0 if sr1 exists within sr2, greater than 0 if sr2 exists within sr1
   *     and 0 otherwise (which should not occur for our usage).
   */
  private int compareSymbolRefSize(SymbolWithPosition sr1, SymbolWithPosition sr2) {
    SymbolReference symbolRef1 = sr1.getSymbolReference();
    SymbolReference symbolRef2 = sr2.getSymbolReference();
    int sr1Line =
        symbolRef1.getPosition().getEnd().getLine() - symbolRef1.getPosition().getStart().getLine();
    int sr2Line =
        symbolRef2.getPosition().getEnd().getLine() - symbolRef2.getPosition().getStart().getLine();
    // If any of two symbol references span multiple lines
    if (sr1Line != sr2Line) {
      return Integer.compare(sr1Line, sr2Line);
    }
    // Otherwise, they exist across the same line, and we compare by column spans.
    return Integer.compare(
        symbolRef2.getPosition().getStart().getColumn(),
        symbolRef1.getPosition().getStart().getColumn());
  }

  /** Helper function which will sort locations by proximity to the input line and column. */
  private int sortByClosestLocations(Location loc1, Location loc2, int line) {

    int loc1Line = loc1.getRange().getStart().getLine();
    int loc1Delta = line - loc1Line;

    int loc2Line = loc2.getRange().getStart().getLine();
    int loc2Delta = line - loc2Line;

    if (loc1Delta == 0 && loc2Delta == 0) {
      // if both line deltas are zero (i.e on the same line). We  sort by column position.
      return Integer.compare(
          loc1.getRange().getStart().getCharacter(), loc2.getRange().getStart().getCharacter());
    }

    // Sort by closest to input line and column.
    return Integer.compare(loc1Delta, loc2Delta);
  }

  /**
   * Processes {@link PipelineConfig}s which are present in the {@link Registries}.
   *
   * @param mainConfigProto The root pipeline proto config associated with the wstl file.
   * @param registries Registries obtained from an instantiated {@link Engine} object.
   */
  public void loadSymbolsFromRegistries(PipelineConfig mainConfigProto, Registries registries) {

    List<PipelineConfig> pipelineConfigs =
        registries.getAllRegisteredPackages().stream()
            .map(registries::getFunctionRegistry)
            .flatMap(r -> r.getAll().stream())
            .filter(WhistleFunction.class::isInstance)
            .map(WhistleFunction.class::cast)
            // Find all distinct whistle files (in proto form)
            .map(WhistleFunction::getPipelineConfig)
            .distinct()
            .collect(toCollection(ArrayList::new));

    pipelineConfigs.add(mainConfigProto);
    pipelineConfigs.forEach(
        p -> {
          try {
            Any fileInfoAny = p.getMeta().getEntriesOrThrow(FILE_INFO_KEY);
            FileInfo fileInfo = fileInfoAny.unpack(FileInfo.class);
            String fileUri = fileInfo.getUrl();
            extractSymbolsFromRootPipelineConfig(p, fileUri);
          } catch (InvalidProtocolBufferException | IllegalArgumentException e) {
            logger.atSevere().withCause(e).log("Error while processing a Pipeline Config.");
          }
        });
  }

  /**
   * Entry point to process a root {@link PipelineConfig} for a given file.
   *
   * @param pipelineConfig The root pipeline config
   * @param fileUri FileUri for the file being processed
   */
  public void extractSymbolsFromRootPipelineConfig(PipelineConfig pipelineConfig, String fileUri) {

    FunctionDefinition rootBlock = pipelineConfig.getRootBlock();
    String packageName = pipelineConfig.getPackageName();

    // Process field mappings
    rootBlock
        .getMappingList()
        .forEach(fm -> extractSymbolsFromFieldMapping(fm, fileUri, packageName));

    // Process function definitions
    pipelineConfig
        .getFunctionsList()
        .forEach(f -> extractSymbolsFromFunctionDefn(f, fileUri, packageName));
  }

  private void extractSymbolsFromFieldMapping(FieldMapping f, String fileUri, String packageName) {

    Meta meta = f.getMeta();
    ValueSource value = f.getValue();
    extractSymbolsFromValueSource(value, fileUri, packageName);
    // Process the field mapping definition.
    extractSymbolsFromMeta(meta, fileUri, packageName, 0);

    // Function calls inside field mappings.
    if (f.hasCustomSink()) {
      FunctionCall functionCall = f.getCustomSink();
      Meta functionCallMeta = functionCall.getMeta();
      extractSymbolsFromMeta(functionCallMeta, fileUri, packageName, functionCall.getArgsCount());
      // Process function arguments
      for (ValueSource args : functionCall.getArgsList()) {
        extractSymbolsFromValueSource(args, fileUri, packageName);
      }
    }
  }

  private void extractSymbolsFromFunctionDefn(
      FunctionDefinition f, String fileUri, String packageName) {
    Meta meta = f.getMeta();

    f.getArgsList()
        .forEach(
            arg ->
                extractSymbolsFromMeta(
                    arg.getMeta(),
                    fileUri,
                    packageName,
                    0));
    extractSymbolsFromMeta(
        meta,
        fileUri,
        packageName,
        f.getArgsCount());

    createSymbolForBlocks(f, fileUri, packageName);

    // Process any field mappings for this function definition
    f.getMappingList().forEach(fm -> extractSymbolsFromFieldMapping(fm, fileUri, packageName));
  }

  private void createSymbolForBlocks(FunctionDefinition f, String fileUri, String packageName) {
    Meta meta = f.getMeta();
    Any symbolAny = meta.getEntriesMap().getOrDefault(SYMBOL_KEY, null);
    Any sourceAny = meta.getEntriesMap().getOrDefault(SOURCE_KEY, null);
    // blocks are the function without symbol
    if (symbolAny == null && sourceAny != null) {
      try {
        SymbolReference blockReference =
            SymbolReference.newBuilder()
                .setName(f.getName())
                .setType(SymbolReference.Type.FUNCTION)
                .build();
        Source source = sourceAny.unpack(Source.class);
        SymbolWithPosition blockSymbolWithPosition =
            new SymbolWithPosition(source, blockReference, packageName, fileUri, 0);
        documentUriToDefinitionSymbols.get(fileUri).add(blockSymbolWithPosition);
      } catch (InvalidProtocolBufferException e) {
        logger.atSevere().withCause(e).log(
            "An error occurred while processing the pipeline config");
      }
    }
  }

  private void extractSymbolsFromValueSource(
      ValueSource valueSource, String fileUri, String packageName) {

    // Process a value source with a function call.
    if (valueSource.hasFunctionCall()) {
      FunctionCall functionCall = valueSource.getFunctionCall();
      Meta meta = functionCall.getMeta();
      extractSymbolsFromMeta(
          meta,
          fileUri,
          packageName,
          functionCall.getArgsCount());

      // Process function arguments
      for (ValueSource args : functionCall.getArgsList()) {
        extractSymbolsFromValueSource(args, fileUri, packageName);
      }
    } else {

      // Processes all other types of value sources.
      Meta meta = valueSource.getMeta();
      extractSymbolsFromMeta(meta, fileUri, packageName, 0);
    }
  }

  private void extractSymbolsFromMeta(
      Meta meta,
      String fileUri,
      String packageName,
      int numArgs) {

    Any symbolAny = meta.getEntriesMap().getOrDefault(SYMBOL_KEY, null);
    Any sourceAny = meta.getEntriesMap().getOrDefault(SOURCE_KEY, null);
    if (symbolAny != null) {
      try {
        Symbols symbols = symbolAny.unpack(Symbols.class);
        // A symbol which includes a package reference, for example packageRef::functionCall()
        if (symbols.getSymbolsCount() == 2
            && symbols.getSymbols(0).getType().equals(SymbolReference.Type.PACKAGE)
            && !symbols.getSymbols(0).getDefinition()) {
          packageName = symbols.getSymbols(0).getName();
        }
        for (SymbolReference sr : symbols.getSymbolsList()) {
          // We may be processing a terminal symbol node
          Source source = sourceAny != null ? sourceAny.unpack(Source.class) : sr.getPosition();
          createSymbolWithPosFromSymbolRef(
              sr,
              packageName,
              source,
              fileUri,
              numArgs);
        }
      } catch (InvalidProtocolBufferException e) {
        logger.atSevere().withCause(e).log(
            "An error occurred while processing the pipeline config");
      }
    }
  }

  private void createSymbolWithPosFromSymbolRef(
      SymbolReference symbolReference,
      String packageName,
      Source source,
      String fileUri,
      int numArgs) {
    boolean symbolIsDefn = symbolReference.getDefinition();
    boolean symbolIsWrite = symbolReference.getIsWrite();
    SymbolWithPosition symbolWithPosition =
        new SymbolWithPosition(source, symbolReference, packageName, fileUri, numArgs);
    if (symbolIsDefn) {
      documentUriToDefinitionSymbols.get(fileUri).add(symbolWithPosition);
    } else if (symbolIsWrite) {
      documentUriToWriteSymbols.get(fileUri).add(symbolWithPosition);
    } else {
      documentUriToReferenceSymbols.get(fileUri).add(symbolWithPosition);
    }
  }

  /**
   * Class which is used to hold {@link SymbolReference} and {@link Source} information for tokens
   * which exist in a file represented by a fileUri.
   */
  public static class SymbolWithPosition {
    private final Source source;
    private final SymbolReference symbolReference;
    private final String packageName;
    private final String fileUri;
    private final int numArgs;

    public SymbolWithPosition(
        Source source,
        SymbolReference symbolReference,
        String packageName,
        String fileUri,
        int numArgs) {
      this.source = source;
      this.symbolReference = symbolReference;
      this.packageName = packageName;
      this.fileUri = fileUri;
      this.numArgs = numArgs;
    }

    /**
     * Returns the default 1-based line and 0-based column {@link Source} which is generated by the
     * Transpiler.
     */
    public Source getSource() {
      return source;
    }

    public SymbolReference getSymbolReference() {
      return symbolReference;
    }

    public String getFileUri() {
      return fileUri;
    }

    public String getPackageName() {
      return packageName;
    }

    public int getNumArgs() {
      return numArgs;
    }

    /** Returns a 0-based line and column source used by the Whistle language server. */
    public Source getZeroBasedSource() {
      return source.toBuilder()
          .setStart(source.getStart().toBuilder().setLine(source.getStart().getLine() - 1).build())
          .setEnd(source.getEnd().toBuilder().setLine(source.getEnd().getLine() - 1).build())
          .build();
    }
  }
}
