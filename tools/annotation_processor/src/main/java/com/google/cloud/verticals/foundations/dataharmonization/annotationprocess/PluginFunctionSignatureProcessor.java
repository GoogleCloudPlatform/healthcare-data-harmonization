/*
 * Copyright 2023 Google LLC.
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
package com.google.cloud.verticals.foundations.dataharmonization.annotationprocess;

import com.google.common.flogger.GoogleLogger;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * Consolidates function signature for methods annotated with {@link
 * com.google.cloud.verticals.foundations.dataharmonization.function.java.PluginFunction} at
 * compile-time and saves function signature to the class path.
 */
public class PluginFunctionSignatureProcessor {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();
  private static final String RUNTIME_CONTEXT_ARG = "RuntimeContext";
  private final ProcessingEnvironment processingEnv;
  private final HashMap<String, String> fileNameToSignature;

  public PluginFunctionSignatureProcessor(ProcessingEnvironment processingEnv) {
    this.processingEnv = processingEnv;
    fileNameToSignature = new HashMap<>();
  }

  /**
   * Entry point to begin processing signatures for methods annotated with PluginFunction.
   *
   * @param annotatedElements - the annotated methods to be processed.
   * @return True on successful processing of all annotated methods, false otherwise.
   */
  public boolean processAnnotation(Set<? extends Element> annotatedElements) {
    try {
      for (Element element : annotatedElements) {
        String functionName = element.getSimpleName().toString();
        ExecutableElement executableElement = (ExecutableElement) element;
        List<String> types = new ArrayList<>();
        List<String> parameters = new ArrayList<>();
        Elements elementsUtil = processingEnv.getElementUtils();

        for (VariableElement parameter : executableElement.getParameters()) {
          TypeMirror typeMirror = parameter.asType();
          String type;
          // Check if it's an array type, then store the simple name (e.g., "String" for String[])
          // This is because whistle see them as primitive types and not as array types.
          if (typeMirror.getKind() == TypeKind.ARRAY) {
            ArrayType arrayType = (ArrayType) typeMirror;
            TypeMirror componentType = arrayType.getComponentType();
            type = elementsUtil.getTypeElement(componentType.toString()).getSimpleName().toString();
          } else {
            type = elementsUtil.getTypeElement(typeMirror.toString()).getSimpleName().toString();
          }
          if (!type.equals(RUNTIME_CONTEXT_ARG)) {
            types.add(type);
            parameters.add(parameter.getSimpleName().toString());
          }
        }
        String signature = createSignature(functionName, parameters, types);
        String fileName = getFileName(types, functionName, executableElement.isVarArgs());
        fileNameToSignature.put(fileName, signature);
      }
      writeSignatureToFiles();
    } catch (RuntimeException | IOException e) {
      logger.atSevere().withCause(e).log(
          "An error occurred while processing element %s", annotatedElements);
      return false;
    }
    return true;
  }

  /**
   * Create custom file with format signature_{functionName}({argTypes}).
   *
   * @param types List of argument types.
   * @param functionName Name of the function.
   * @param isVarArgs Boolean indicating arguments are variable or not.
   * @return name of the filename to store the function signature.
   */
  private String getFileName(List<String> types, String functionName, boolean isVarArgs) {
    List<String> argsType = new ArrayList<>();
    for (var type : types) {
      if (type.equals("String")
          || type.equals("Primitive")
          || type.equals("Long")
          || type.equals("Boolean")
          || type.equals("Double")
          || type.equals("Integer")) {
        argsType.add("Primitive");
      } else {
        argsType.add(type);
      }
    }

    StringBuilder filename =
        new StringBuilder()
            .append("signature_")
            .append(functionName)
            .append("(")
            .append(String.join(", ", argsType));
    if (isVarArgs) {
      filename.append("...");
    }
    filename.append(")");
    return sanitizeForFileName(filename.toString());
  }

  /**
   * Creates a function signature given the name of the function, parameters, and their types.
   *
   * @param functionName The name of the function.
   * @param parameters The list of parameters.
   * @param parameterTypes The list of types of the parameters.
   * @return The function signature.
   */
  private String createSignature(
      String functionName, List<String> parameters, List<String> parameterTypes) {
    StringBuilder signature = new StringBuilder().append(functionName).append("(");
    for (int index = 0; index < parameters.size(); index++) {
      signature.append(parameterTypes.get(index)).append(" ").append(parameters.get(index));
      if (index < parameters.size() - 1) {
        signature.append(", ");
      }
    }
    signature.append(")");
    return signature.toString();
  }

  /**
   * Remove invalid character for the filename.
   *
   * @param filename name of the file.
   * @return valid filename.
   */
  private String sanitizeForFileName(String filename) {
    // Replace invalid characters with underscores
    return filename.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
  }

  /**
   * Writes out the strings stored in the fileNameToSignature map.
   *
   * @return True on success, false on any exceptions.
   */
  private void writeSignatureToFiles() throws IOException {
    for (Entry<String, String> e : fileNameToSignature.entrySet()) {

      String fileName = e.getKey();
      String functionSignature = e.getValue();
      logger.atInfo().log("Writing: %s", fileName);
      FileObject res =
          processingEnv
              .getFiler()
              .createResource(StandardLocation.CLASS_OUTPUT, "", fileName);
      OutputStream outputStream = res.openOutputStream();
      outputStream.write(functionSignature.getBytes(StandardCharsets.UTF_8));
      outputStream.close();
    }
    fileNameToSignature.clear();
  }
}
