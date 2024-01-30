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

import com.google.cloud.verticals.foundations.dataharmonization.reflection.ReflectedCallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.reflection.ReflectedJavaFunction;
import com.google.cloud.verticals.foundations.dataharmonization.reflection.ReflectedPlugin;
import com.google.cloud.verticals.foundations.dataharmonization.reflection.ReflectedTargetConstructor;
import com.google.common.flogger.FluentLogger;
import java.io.IOException;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * Utility class for keeping track of mappings of plugin functions and targets to their (Whistle)
 * package name.
 */
public final class ReversePackageSearch {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();
  private final Map<String, String> javaFullMethodNameToPackageName = new HashMap<>();
  private final Map<String, String> targetConstructorClassNameToPackageName = new HashMap<>();
  private final Map<String, String> targetConstructorClassNameToTargetName = new HashMap<>();
  private final Map<String, String> packageNameToPluginClassName = new HashMap<>();
  private final Set<ReflectedPlugin> plugins;

  public ReversePackageSearch(Set<ReflectedPlugin> plugins) {
    this.plugins = plugins;
  }

  private void mapFunctions(String pkg, List<ReflectedCallableFunction> callableFunctions) {
    callableFunctions.forEach(
        fn -> {
          if (!fn.isJavaFunction()) {
            return;
          }
          javaFullMethodNameToPackageName.put(createSigKey(fn.asJavaFunction()), pkg);
        });
  }

  private String createSigKey(ReflectedJavaFunction fn) {
    return String.format(
        "%s.%s(%s)",
        fn.getMethod().getDeclaringClass().getName(),
        fn.getMethod().getName(),
        createArgsSigKey(fn.getMethod().getParameters()));
  }

  private String createArgsSigKey(Parameter[] parameters) {
    return Arrays.stream(parameters)
        .map(
            p -> {
              if (p.isVarArgs()) {
                return p.getType().getComponentType().getSimpleName() + "...";
              }
              return p.getType().getSimpleName();
            })
        .collect(Collectors.joining(", "));
  }

  private String createSigKey(Types types, ExecutableElement e) {
    StringBuilder fullName = new StringBuilder();
    Element current = e;
    while (current != null) {
      String name =
          current.getKind() == ElementKind.PACKAGE
              ? ((PackageElement) current).getQualifiedName().toString()
              : current.getSimpleName().toString();
      if (!name.isBlank()) {
        fullName.insert(0, name + (fullName.length() == 0 ? "" : "."));
      }
      current = current.getEnclosingElement();
    }

    String argTypes =
        e.getParameters().stream()
            .map(p -> ReversePackageSearch.getTypeString(types, p))
            .collect(Collectors.joining(", "));

    if (e.isVarArgs()) {
      argTypes = argTypes + "...";
    }

    return String.format("%s(%s)", fullName, argTypes);
  }

  /** Returns a Human Readable String describing the type of the given VariableElement. */
  public static String getTypeString(Types types, VariableElement element) {
    TypeMirror typeMirror = element.asType();

    if (typeMirror == null) {
      logger.atWarning().log("Can't find type of param %s%n", element);
      return "???";
    }
    if (typeMirror instanceof ArrayType) {
      typeMirror = ((ArrayType) typeMirror).getComponentType();
    }

    return types.asElement(typeMirror).getSimpleName().toString();
  }

  /**
   * Looks up the Whistle package of an ExecutableElement corresponding to a PluginFunction. This is
   * only meaningful after {@link #generateReverseMappings()} was called.
   */
  public String getPackageOfFunctionElement(Types types, ExecutableElement e) {
    String key = createSigKey(types, e);

    return javaFullMethodNameToPackageName.getOrDefault(key, "???");
  }

  /** Creates mappings of Whistle packages (plugins) to their provided targets and functions. */
  public void generateReverseMappings() throws IOException {
    System.out.printf("Loaded %d plugins: %s%n", plugins.size(), plugins);

    for (ReflectedPlugin p : plugins) {
      mapFunctions(p.getPackageName(), p.getFunctions());
      mapTargets(p.getPackageName(), p.getTargets());
      packageNameToPluginClassName.put(p.getPackageName(), p.getReflectedClass().getName());
    }
  }

  private void mapTargets(String pkg, List<ReflectedTargetConstructor> targets) {
    targets.forEach(
        fn -> {
          String className = fn.getReflectedClass().getCanonicalName();
          targetConstructorClassNameToPackageName.put(className, pkg);
          targetConstructorClassNameToTargetName.put(className, fn.getName());
        });
  }

  /**
   * Looks up the Plugin Class corresponding to the given Whistle package. This is only meaningful
   * after {@link #generateReverseMappings()} was called.
   */
  public String getPackageClass(String pkg) {
    return packageNameToPluginClassName.getOrDefault(pkg, "???");
  }

  /**
   * Returns true iff the given element corresponds to a Target.Constructor implementation that has
   * been mapped from a Plugin. This is only meaningful after {@link #generateReverseMappings()} was
   * called.
   */
  public boolean isKnownTargetConstructorClass(Element element) {
    return element instanceof TypeElement
        && targetConstructorClassNameToPackageName.containsKey(
            ((TypeElement) element).getQualifiedName().toString());
  }

  /**
   * Looks up the Target name (i.e. as it would be referred to in Whistle code) corresponding to the
   * given Target Constructor class. This is only meaningful after {@link
   * #generateReverseMappings()} was called.
   */
  public String getTargetNameOfTargetConstructorClass(TypeMirror type) {
    return targetConstructorClassNameToTargetName.get(
        ((TypeElement) ((DeclaredType) type).asElement()).getQualifiedName().toString());
  }

  /**
   * Looks up the Package name corresponding to the given Target Constructor class. This is only
   * meaningful after {@link #generateReverseMappings()} was called.
   */
  public String getPackageNameOfTargetConstructorClass(TypeMirror type) {
    return targetConstructorClassNameToPackageName.get(
        ((TypeElement) ((DeclaredType) type).asElement()).getQualifiedName().toString());
  }
}
