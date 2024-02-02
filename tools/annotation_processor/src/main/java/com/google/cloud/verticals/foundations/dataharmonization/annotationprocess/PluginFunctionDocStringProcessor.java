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

package com.google.cloud.verticals.foundations.dataharmonization.annotationprocess;

import com.google.cloud.verticals.foundations.dataharmonization.doclet.FunctionScanner;
import com.google.cloud.verticals.foundations.dataharmonization.doclet.formatting.FilePrinter;
import com.google.cloud.verticals.foundations.dataharmonization.doclet.formatting.Markdown;
import com.google.cloud.verticals.foundations.dataharmonization.doclet.formatting.MarkupFormat;
import com.google.cloud.verticals.foundations.dataharmonization.doclet.model.FunctionDoc;
import com.google.common.flogger.GoogleLogger;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.util.DocTrees;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * Consolidates javadocs for methods annotated with {@link
 * com.google.cloud.verticals.foundations.dataharmonization.function.java.PluginFunction} at
 * compile-time and saves formatted documentation to the class path.
 */
public class PluginFunctionDocStringProcessor {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();
  private static final String RUNTIME_CONTEXT_ARG = "RuntimeContext";
  private static final String VAR_ARGS_FUNCTIONS = "VAR_ARGS";
  private static final MarkupFormat MD_DOC_FORMAT = new Markdown();

  private final DocTrees docs;
  private final ProcessingEnvironment processingEnv;
  private final Types types;
  private final Elements elements;
  private final HashMap<String, String> fileNameToMdStringMap;

  public PluginFunctionDocStringProcessor(ProcessingEnvironment processingEnv) {
    this.processingEnv = processingEnv;
    this.docs = DocTrees.instance(processingEnv);
    this.types = processingEnv.getTypeUtils();
    this.elements = processingEnv.getElementUtils();
    fileNameToMdStringMap = new HashMap<>();
  }

  /**
   * Entry point to begin processing javadocs for methods annotated with PluginFunction.
   *
   * @param annotatedElements - the annotated methods to be processed.
   * @return True on successful processing of all annotated methods, false otherwise.
   */
  public boolean processAnnotation(Set<? extends Element> annotatedElements) {
    try {
      for (Element element : annotatedElements) {
        String functionName = element.getSimpleName().toString();
        ExecutableElement executableElement = (ExecutableElement) element;
        List<String> args =
            executableElement.getParameters().stream()
                .map(s -> s.asType().toString())
                .filter(s -> !s.endsWith(RUNTIME_CONTEXT_ARG))
                .collect(Collectors.toList());

        DocCommentTree docCommentTree = docs.getDocCommentTree(executableElement);
        if (docCommentTree != null) {
          generateAndStoreMdString(
              processingEnv.getElementUtils().getPackageOf(element),
              executableElement,
              functionName,
              docCommentTree,
              args.size());
        }
      }
      writeMdStringsToFiles();
    } catch (RuntimeException | IOException e) {
      logger.atSevere().withCause(e).log(
          "An error occurred while processing element %s", annotatedElements);
      return false;
    }
    return true;
  }

  /**
   * Method which processes a DocumentTree for a given {@link ExecutableElement} and stores the
   * corresponding mdString into a fileName to documentation map which will eventually be written
   * once annotation processing is complete.
   *
   * @param pkg Package for the element being processed.
   * @param executableElement The element being processed.
   * @param functionName The name of the function/target being processed.
   * @param docCommentTree The corresponding document comment tree.
   * @param numArgs The number of args associated with this function/target.
   */
  private void generateAndStoreMdString(
      PackageElement pkg,
      ExecutableElement executableElement,
      String functionName,
      DocCommentTree docCommentTree,
      int numArgs) {
    FunctionScanner scanner = new FunctionScanner(MD_DOC_FORMAT, docs, types, elements, null);
    FunctionDoc.Builder functionDoc =
        scanner.getFunctionDoc(
            docCommentTree, executableElement, executableElement.getSimpleName().toString());

    FilePrinter mdPrinter = new FilePrinter(MD_DOC_FORMAT);
    String mdString = mdPrinter.function(pkg.getSimpleName().toString(), functionDoc.build());

    String fileName;
    if (executableElement.isVarArgs()) {
      fileName = functionName + "_" + VAR_ARGS_FUNCTIONS;
    } else {
      fileName = functionName + "_" + numArgs;
    }

    if (fileNameToMdStringMap.containsKey(fileName)) {
      // For the case of overloads with a matching number of args, we append to the doc already
      // present.
      String originalMd = fileNameToMdStringMap.get(fileName);
      fileNameToMdStringMap.put(fileName, originalMd + "\n\n" + mdString);
    } else {
      fileNameToMdStringMap.put(fileName, mdString);
    }
  }

  /**
   * Writes out the strings stored in the fileNameToMdString map.
   *
   * @return True on success, false on any exceptions.
   */
  private void writeMdStringsToFiles() throws IOException {
    for (Entry<String, String> e : fileNameToMdStringMap.entrySet()) {

      String fileName = e.getKey();
      String mdValue = e.getValue();
      logger.atInfo().log("Writing: %s", fileName);
      FileObject res =
          processingEnv
              .getFiler()
              .createResource(StandardLocation.CLASS_OUTPUT, "", fileName);
      OutputStream outputStream = res.openOutputStream();
      outputStream.write(mdValue.getBytes(StandardCharsets.UTF_8));
      outputStream.close();
    }
    fileNameToMdStringMap.clear();
  }
}
