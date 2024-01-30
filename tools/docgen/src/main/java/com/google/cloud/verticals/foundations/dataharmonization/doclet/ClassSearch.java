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
package com.google.cloud.verticals.foundations.dataharmonization.doclet;

import com.google.cloud.verticals.foundations.dataharmonization.reflection.ReflectedTargetConstructor;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Queues;
import java.util.Collection;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/** Utility class for searching class hierarchies. */
public final class ClassSearch {
  private ClassSearch() {}

  /**
   * Finds and returns the first (closest) superclass/superinterface of the given class that matches
   * the given predicate.
   *
   * @param start The class to start at.
   * @param condition The precondition to check. This will be executed until it returns true.
   * @return The matching class/interface if one is found in the class hierarchy or an empty
   *     optional otherwise.
   */
  public static Optional<Class<?>> superClassMatching(
      Class<?> start, Predicate<Class<?>> condition) {
    return bfs(
        start,
        condition,
        c ->
            ImmutableList.<Class<?>>builder()
                .add(c.getInterfaces())
                .add(
                    c.getSuperclass() != null
                        ? new Class<?>[] {c.getSuperclass()}
                        : new Class<?>[0])
                .build());
  }

  /**
   * Returns true iff the given ExecutableElement is a {@code construct} method for a Target
   * Constructor.
   */
  public static boolean isTargetConstructMethod(
      Types types, Elements elements, ExecutableElement method) {
    return checkAssignability(
            types, elements, ReflectedTargetConstructor.TARGET_BASECLASS, method.getReturnType())
        && checkAssignability(
            types,
            elements,
            ReflectedTargetConstructor.TARGET_CONSTRUCTOR_BASECLASS,
            method.getEnclosingElement().asType());
  }

  private static boolean checkAssignability(
      Types types, Elements elements, Class<?> superClass, TypeMirror subClass) {
    return types.isAssignable(
        subClass, elements.getTypeElement(superClass.getCanonicalName()).asType());
  }

  private static <T> Optional<T> bfs(
      T start, Predicate<T> condition, Function<T, Collection<T>> next) {
    Queue<T> work = Queues.newArrayDeque();
    work.add(start);
    while (!work.isEmpty()) {
      T item = work.remove();
      if (condition.test(item)) {
        return Optional.of(item);
      }

      work.addAll(next.apply(item));
    }
    return Optional.empty();
  }
}
