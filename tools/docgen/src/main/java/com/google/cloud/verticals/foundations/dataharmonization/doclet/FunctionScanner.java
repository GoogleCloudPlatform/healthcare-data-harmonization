// Copyright 2022 Google LLC.
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
package com.google.cloud.verticals.foundations.dataharmonization.doclet;

import com.google.cloud.verticals.foundations.dataharmonization.doclet.formatting.MarkupFormat;
import com.google.cloud.verticals.foundations.dataharmonization.doclet.model.ArgumentDoc;
import com.google.cloud.verticals.foundations.dataharmonization.doclet.model.FunctionDoc;
import com.google.cloud.verticals.foundations.dataharmonization.doclet.model.PackageDoc;
import com.google.cloud.verticals.foundations.dataharmonization.doclet.model.ReturnDoc;
import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.ThrowsTree;
import com.sun.source.util.DocTrees;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementScanner8;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Function Scanner finds plugin functions and targets and registers them into a map, organized by
 * Whistle package.
 */
public class FunctionScanner extends ElementScanner8<Void, Map<String, PackageDoc.Builder>> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final String PLUGIN_FUNCTION_CLASSNAME =
      "com.google.cloud.verticals.foundations.dataharmonization.function.java.PluginFunction";

  private final MarkupFormat format;
  private final DocTrees docs;
  private final Types types;
  private final Elements elements;
  private final ReversePackageSearch reversePackageSearch;

  public FunctionScanner(
      MarkupFormat format,
      DocTrees docs,
      Types types,
      Elements elements,
      ReversePackageSearch reversePackageSearch) {
    this.format = format;
    this.docs = docs;
    this.types = types;
    this.elements = elements;
    this.reversePackageSearch = reversePackageSearch;
  }

  @Override
  public Void visitExecutable(ExecutableElement e, Map<String, PackageDoc.Builder> pkgToDocs) {
    // Process plugin functions and targets
    boolean isFunc = hasPluginFunctionAnnotation(e);
    boolean isTarget = !isFunc && isTargetConstructMethod(e);
    if (!isFunc && !isTarget) {
      return super.visitExecutable(e, pkgToDocs);
    }
    DocCommentTree commentTree = docs.getDocCommentTree(e);
    if (commentTree == null && !isTarget) {
      logger.atWarning().log("Null tree for %s", e);
      return super.visitExecutable(e, pkgToDocs);
    }

    if (isFunc) {
      return processFunction(commentTree, e, pkgToDocs);
    }

    return processTarget(commentTree, e, pkgToDocs);
  }

  private static boolean hasPluginFunctionAnnotation(ExecutableElement e) {
    return e.getAnnotationMirrors().stream()
        .anyMatch(am -> am.getAnnotationType().toString().equals(PLUGIN_FUNCTION_CLASSNAME));
  }

  private boolean isTargetConstructMethod(ExecutableElement e) {
    return e.getEnclosingElement() != null
        && e.getEnclosingElement().asType().getKind() != TypeKind.NONE
        && reversePackageSearch.isKnownTargetConstructorClass(e.getEnclosingElement())
        && ClassSearch.isTargetConstructMethod(types, elements, e);
  }

  private Void processFunction(
      DocCommentTree docTree, ExecutableElement e, Map<String, PackageDoc.Builder> pkgToDocs) {
    FunctionDoc.Builder functionBuilder = getFunctionDoc(docTree, e, e.getSimpleName().toString());
    String pkg = reversePackageSearch.getPackageOfFunctionElement(types, e);
    String className = reversePackageSearch.getPackageClass(pkg);
    pkgToDocs.put(
        pkg,
        pkgToDocs
            .getOrDefault(pkg, PackageDoc.builder().setPackageName(pkg).setClassName(className))
            .addFunction(functionBuilder.build()));

    return super.visitExecutable(e, pkgToDocs);
  }

  private Void processTarget(
      DocCommentTree docTree,
      ExecutableElement constructMethod,
      Map<String, PackageDoc.Builder> pkgToDocs) {

    if (docTree == null) {
      docTree = new ToDoDocCommentTree();
    }
    FunctionDoc.Builder functionBuilder =
        getFunctionDoc(
            docTree,
            constructMethod,
            reversePackageSearch.getTargetNameOfTargetConstructorClass(
                constructMethod.getEnclosingElement().asType()));

    String pkg =
        reversePackageSearch.getPackageNameOfTargetConstructorClass(
            constructMethod.getEnclosingElement().asType());
    String className = reversePackageSearch.getPackageClass(pkg);
    pkgToDocs.put(
        pkg,
        pkgToDocs
            .getOrDefault(pkg, PackageDoc.builder().setPackageName(pkg).setClassName(className))
            .addTarget(functionBuilder.build()));

    return super.visitExecutable(constructMethod, pkgToDocs);
  }

  public FunctionDoc.Builder getFunctionDoc(
      DocCommentTree docTree, ExecutableElement e, String name) {
    ReturnDoc.Builder ret =
        ReturnDoc.builder().setType(types.asElement(e.getReturnType()).getSimpleName().toString());
    Optional<ReturnTree> returnDocTree =
        docTree.getBlockTags().stream()
            .filter(ReturnTree.class::isInstance)
            .map(ReturnTree.class::cast)
            .findFirst();

    ret.setBody(returnDocTree.map(ReturnTree::getDescription).map(this::docToString).orElse(""));

    List<ArgumentDoc> extraArgs = new ArrayList<>();
    FunctionDoc.Builder functionBuilder =
        FunctionDoc.builder()
            .setBody(docToString(docTree.getFullBody(), extraArgs))
            .setName(name)
            .setReturns(ret.build());
    extraArgs.forEach(functionBuilder::addArgument);

    docTree.getBlockTags().stream()
        .filter(ThrowsTree.class::isInstance)
        .map(ThrowsTree.class::cast)
        .forEach(
            tt -> {
              ReturnDoc throwsDoc =
                  ReturnDoc.builder()
                      .setType(docToString(ImmutableList.of(tt.getExceptionName())))
                      .setBody(docToString(tt.getDescription()))
                      .build();
              functionBuilder.addThrows(throwsDoc);
            });

    boolean isFunc = hasPluginFunctionAnnotation(e);
    boolean isTarget = !isFunc && isTargetConstructMethod(e);

    if (isFunc) {
      processFunctionDocArguments(docTree, functionBuilder, e);
    } else if (isTarget) {
      processTargetDocArguments(docTree, functionBuilder);
    }
    return functionBuilder;
  }

  /**
   * Process the arguments of an PluginFunction ExecutableElement and add documentation for these
   * arguments to the FunctionDoc.Builder object.
   *
   * @param docTree Element docTree
   * @param functionBuilder FunctionDoc.Builder object which stores all information about the
   *     function docString and which holds argument documentation
   * @param e The element being processed
   */
  private void processFunctionDocArguments(
      DocCommentTree docTree, FunctionDoc.Builder functionBuilder, ExecutableElement e) {
    Map<String, String> argNameToDoc =
        docTree.getBlockTags().stream()
            .filter(ParamTree.class::isInstance)
            .map(ParamTree.class::cast)
            .collect(
                Collectors.toMap(
                    pt -> docToString(ImmutableList.of(pt.getName())),
                    pt -> docToString(pt.getDescription())));

    List<? extends VariableElement> parameters = e.getParameters();
    for (int i = 0; i < parameters.size(); i++) {
      VariableElement parameter = parameters.get(i);
      String name = parameter.getSimpleName().toString();
      Element elem = types.asElement(parameter.asType());
      if (parameter.asType().getKind() == TypeKind.ARRAY) {
        elem = types.asElement(((ArrayType) parameter.asType()).getComponentType());
      }

      String type;
      if (elem == null) {
        logger.atSevere().log("No type for %s of %s", parameter, e);
        type = "???";
      } else {
        type = elem.getSimpleName().toString();
      }

      if (type.equals("RuntimeContext")) {
        continue;
      }

      if (i == parameters.size() - 1 && e.isVarArgs()) {
        type += "...";
      }

      ArgumentDoc arg =
          ArgumentDoc.builder()
              .setName(name)
              .setType(type)
              .setBody(argNameToDoc.getOrDefault(name, ""))
              .build();
      functionBuilder.addArgument(arg);
    }
  }

  /**
   * Process the arguments for a Plugin Target ExecutableElement and add documentation for these
   * arguments to the FunctionDoc.Builder object.
   *
   * @param docTree Element docTree which contains argument documentation as part of the javadoc
   *     string.
   * @param functionBuilder FunctionDoc.Builder object which stores all information about the
   *     function docString and which holds argument documentation.
   */
  private void processTargetDocArguments(
      DocCommentTree docTree, FunctionDoc.Builder functionBuilder) {
    List<Entry<String, String>> argNameToDoc =
        docTree instanceof ToDoDocCommentTree
            ? ImmutableList.of(new AbstractMap.SimpleEntry<>("...TODO", "..."))
            : docTree.getBlockTags().stream()
                .filter(ParamTree.class::isInstance)
                .map(ParamTree.class::cast)
                .map(
                    pt ->
                        new AbstractMap.SimpleEntry<>(
                            docToString(ImmutableList.of(pt.getName())),
                            docToString(pt.getDescription())))
                .collect(Collectors.toList());

    for (Entry<String, String> entry : argNameToDoc) {
      ArgumentDoc arg =
          ArgumentDoc.builder()
              .setName(entry.getKey())
              .setType(
                  docTree instanceof ToDoDocCommentTree
                      ? "Args need to be added to javadoc..."
                      : "Data")
              .setBody(entry.getValue())
              .build();
      functionBuilder.addArgument(arg);
    }
  }

  private String docToString(List<? extends DocTree> items) {
    DocTreeToString dts = new DocTreeToString(format);
    String doc = dts.visitAll(items, new StringBuilder()).toString();
    if (dts.getSpecialArgs().size() > 0) {
      throw new IllegalStateException("Did not expect special argument documentation here.");
    }
    return doc;
  }

  private String docToString(List<? extends DocTree> items, List<ArgumentDoc> specialArgs) {
    DocTreeToString dts = new DocTreeToString(format);
    String doc = dts.visitAll(items, new StringBuilder()).toString();
    specialArgs.addAll(dts.getSpecialArgs());
    return doc;
  }
}
