/*
 * Copyright 2022 Google LLC.
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
package com.google.cloud.verticals.foundations.dataharmonization.builtins.options;

import static com.google.cloud.verticals.foundations.dataharmonization.debug.DebugInfo.UNKNOWN_SOURCE;
import static com.google.cloud.verticals.foundations.dataharmonization.imports.impl.DefaultImportProcessor.IMPORT_EXCEPTION_LIST_KEY;

import com.google.cloud.verticals.foundations.dataharmonization.builtins.options.exception.VarAndFieldOptionException;
import com.google.cloud.verticals.foundations.dataharmonization.data.path.Path;
import com.google.cloud.verticals.foundations.dataharmonization.debug.DebugInfo;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FileInfo;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.Source;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.ImportException;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.WhistleRuntimeException;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.MetaData;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Option;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FieldMapping;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Option which prevents users from using the same name for a var and field within a block of
 * Whistle code, and throws a RuntimeException when such a scenario occurs. Enabled by adding
 * "option "experiment/unique_vars_and_fields" to the top of a .wstl file.
 */
public class UniqueVarAndFieldsExperiment implements Option {
  private static final String VAR_TYPE = "variable";
  private static final String FIELD_TYPE = "field";

  @Override
  public RuntimeContext enable(RuntimeContext context, PipelineConfig.Option config) {
    return Option.withOption(context, this);
  }

  @Override
  public RuntimeContext disable(RuntimeContext context) {
    return Option.withoutOption(context, this);
  }

  @Override
  public String getName() {
    return Option.experiment("unique_vars_and_fields");
  }

  @Override
  public void runInitOption(PipelineConfig pipelineConfig, MetaData metaData) {
    verifyFieldsAndVars(pipelineConfig, metaData);
  }

  /**
   * Checks whether a config has a block that contains a field and variable which share the same
   * name. If true, throws a {@link WhistleRuntimeException}.
   *
   * @param config The whistle config being checked
   */
  public static void verifyFieldsAndVars(PipelineConfig config, MetaData metaData) {

    FileInfo fileInfo =
        DebugInfo.getMetaEntry(
            config.getMeta(),
            FileInfo.getDescriptor().getFullName(),
            FileInfo.class,
            FileInfo.getDefaultInstance());

    // Check all function definition blocks
    List<FunctionDefinition> functionDefinitions = new ArrayList<>(config.getFunctionsList());
    // Add the root block to the list so it is also checked
    functionDefinitions.add(config.getRootBlock());

    // Map the functionDefinitions to blockIds for further processing. Order so outer blocks are
    // processed first, i.e. in reverse order.
    Map<String, FunctionDefinition> blockIdToFunctionDef = new LinkedHashMap<>();
    for (FunctionDefinition functionDefinition : Lists.reverse(functionDefinitions)) {
      blockIdToFunctionDef.put(functionDefinition.getName(), functionDefinition);
    }
    // Check the field mappings in each block, use a set to keep track of shared var and field
    // names.
    // Any field mappings which make function calls that reference other function definitions (that
    // inheritParentVars) will
    // recursively process those function definitions using the same set to keep track of unique
    // var and field names within a scope.
    Set<String> functionDefsProcessed = new HashSet<>();
    blockIdToFunctionDef
        .entrySet()
        .forEach(
            e -> {
              FunctionDefinition functionDefinition = e.getValue();
              // Some function defs may have already been processed from a chain of nested function
              // defs which share context
              if (!functionDefsProcessed.contains(functionDefinition.getName())) {
                Map<String, TypeAndSource> fieldVarSourceMap = new HashMap<>();
                processFieldMappingsInFunctionDef(
                    fileInfo,
                    functionDefinition,
                    blockIdToFunctionDef,
                    fieldVarSourceMap,
                    functionDefsProcessed,
                    metaData);
              }
            });
  }

  private static void processFieldMappingsInFunctionDef(
      FileInfo fileInfo,
      FunctionDefinition functionDefinition,
      Map<String, FunctionDefinition> blockIdToFunctionDefinitions,
      Map<String, TypeAndSource> fieldVarSourceMap,
      Set<String> functionDefinitionsProcessed,
      MetaData metaData) {

    for (FieldMapping mapping : functionDefinition.getMappingList()) {
      // Handle nested blocks, which are represented as functionCalls
      if (mapping.getValue().hasFunctionCall()) {
        FunctionCall fc = mapping.getValue().getFunctionCall();
        String functionReferenceId = fc.getReference().getName();
        FunctionDefinition referenceFunctionDefinition =
            blockIdToFunctionDefinitions.getOrDefault(functionReferenceId, null);

        if (referenceFunctionDefinition != null
            && referenceFunctionDefinition.getInheritParentVars()) {
          // This reference function def allows variable inheritance, so we recursively process it
          processFieldMappingsInFunctionDef(
              fileInfo,
              referenceFunctionDefinition,
              blockIdToFunctionDefinitions,
              fieldVarSourceMap,
              functionDefinitionsProcessed,
              metaData);
        }

        for (ValueSource vs : fc.getArgsList()) {
          // Handle args that are function calls, ex ternary blocks
          if (vs.hasFunctionCall()) {
            String argFunctionReferenceId = vs.getFunctionCall().getReference().getName();
            referenceFunctionDefinition =
                blockIdToFunctionDefinitions.getOrDefault(argFunctionReferenceId, null);

            if (referenceFunctionDefinition != null
                && referenceFunctionDefinition.getInheritParentVars()) {
              // Recursively process this function def
              processFieldMappingsInFunctionDef(
                  fileInfo,
                  referenceFunctionDefinition,
                  blockIdToFunctionDefinitions,
                  fieldVarSourceMap,
                  functionDefinitionsProcessed,
                  metaData);
            }
          }
        }
      } else {
        // Handle fields and vars in this mapping
        checkFieldsAndVarsInMappings(fileInfo, mapping, fieldVarSourceMap, metaData);
      }
    }
    functionDefinitionsProcessed.add(functionDefinition.getName());
  }

  private static void checkFieldsAndVarsInMappings(
      FileInfo fileInfo,
      FieldMapping m,
      Map<String, TypeAndSource> fieldVarSourceMap,
      MetaData metaData) {

    String varOrFieldName = null;
    String type = null;
    if (m.hasVar() || m.hasField()) {
      if (m.hasField()) {
        varOrFieldName = m.getField().getPath();
        type = FIELD_TYPE;
      } else if (m.hasVar()) {
        varOrFieldName = m.getVar().getName() + m.getVar().getPath();
        type = VAR_TYPE;
      }
      Path path = Path.parse(varOrFieldName);
      varOrFieldName = path.getSegments().get(0).toString();
      Source source =
          DebugInfo.getMetaEntry(
              m.getMeta(), Source.getDescriptor().getFullName(), Source.class, UNKNOWN_SOURCE);

      if (!fieldVarSourceMap.containsKey(varOrFieldName)) {
        fieldVarSourceMap.put(varOrFieldName, new TypeAndSource(type, source));
      } else {
        // Found a var or field with the same name, check its type and see whether this is a
        // conflict
        // or a field or var update
        TypeAndSource previousTypeAndSource = fieldVarSourceMap.get(varOrFieldName);
        if (!previousTypeAndSource.getType().equals(type)) {
          // Check whether a list to capture exceptions has been provided, as is done with the
          // language server. If none is provided throw the exception, otherwise save the exception.
          List<ImportException> exceptionsList = metaData.getMeta(IMPORT_EXCEPTION_LIST_KEY);
          VarAndFieldOptionException varFieldOptionException =
              new VarAndFieldOptionException(
                  fileInfo, varOrFieldName, source, type, previousTypeAndSource);
          if (exceptionsList == null) {
            throw varFieldOptionException;
          }
          exceptionsList.add(varFieldOptionException);
        } else {
          // Found the same type, either a field or var update. Update the map value to reflect
          // this latest occurrence
          fieldVarSourceMap.put(varOrFieldName, new TypeAndSource(type, source));
        }
      }
    }
  }

  /**
   * Wrapper class to hold the {@link Source} and type (var or field) when handling unique vars and
   * field mappings in a pipeline config.
   */
  public static class TypeAndSource {
    private final Source source;
    private final String type;

    public TypeAndSource(String type, Source source) {
      this.source = source;
      this.type = type;
    }

    public String getType() {
      return type;
    }

    public Source getSource() {
      return source;
    }
  }
}
