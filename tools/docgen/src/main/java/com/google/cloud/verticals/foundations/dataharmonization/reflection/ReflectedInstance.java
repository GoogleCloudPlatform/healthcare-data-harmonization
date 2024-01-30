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
package com.google.cloud.verticals.foundations.dataharmonization.reflection;

import com.google.cloud.verticals.foundations.dataharmonization.doclet.ClassSearch;
import com.google.common.base.Defaults;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Helper class for working with classes that we can't have compile time dependencies on (due to
 * circular references).
 */
public abstract class ReflectedInstance {
  private static final Map<String, ReflectedInstanceFactory> reflectedInstanceFactoryRegistry =
      new HashMap<>();

  public static void registerFactory(ReflectedInstanceFactory reflectedInstanceFactory) {
    reflectedInstanceFactoryRegistry.put(
        reflectedInstanceFactory.createDefault().getReflectedClassName(), reflectedInstanceFactory);
  }

  public static void registerFactory(
      String className, ReflectedInstanceFactory reflectedInstanceFactory) {
    reflectedInstanceFactoryRegistry.put(className, reflectedInstanceFactory);
  }

  private Class<?> reflectedClass;
  protected final Object reflectedInstance;

  protected ReflectedInstance(Object instance) {
    reflectedInstance = instance;
  }

  protected abstract String getReflectedClassName();

  public Class<?> getReflectedClass() {

    if (reflectedClass == null) {
      try {
        reflectedClass = Class.forName(getReflectedClassName());
      } catch (ClassNotFoundException e) {
        throw new IllegalStateException(e);
      }
    }
    return reflectedClass;
  }

  protected <TRet> TRet invoke(Class<TRet> clazz, String methodName, Object... params) {
    Object result = invokeRaw(methodName, params);
    if (result == null) {
      return null;
    }

    return castOrWrap(clazz, result);
  }

  protected <TRet> List<TRet> invokeList(Class<TRet> clazz, String methodName, Object... params) {
    Object result = invokeRaw(methodName, params);
    if (result == null) {
      return null;
    }

    Class<?> realClass = result.getClass();
    if (!List.class.isAssignableFrom(realClass)) {
      throw new UnsupportedOperationException(
          String.format("Expected a list result but got %s", realClass));
    }

    List<?> resultsList = (List<?>) result;
    return resultsList.stream()
        .filter(Objects::nonNull)
        .map(o -> castOrWrap(clazz, o))
        .collect(Collectors.toList());
  }

  private <TRet> TRet castOrWrap(Class<TRet> clazz, Object result) {
    Class<?> realClass = result.getClass();
    ReflectedInstanceFactory factory = null;

    Optional<Class<?>> matchingSuperClass =
        ClassSearch.superClassMatching(
            realClass, c -> reflectedInstanceFactoryRegistry.containsKey(c.getName()));

    if (matchingSuperClass.isEmpty()) {
      if (!clazz.isAssignableFrom(realClass)) {
        throw new IllegalStateException(
            String.format(
                "Result was of class %s but expected class %s. No assignment could be made and no"
                    + " wrapper was registered.",
                realClass.getName(), clazz.getName()));
      }
      return clazz.cast(result);
    }

    factory = reflectedInstanceFactoryRegistry.get(matchingSuperClass.get().getName());

    ReflectedInstance refResult = factory.create(result);
    if (!clazz.isAssignableFrom(refResult.getClass())) {
      throw new IllegalStateException(
          String.format(
              "Expected factory for %s to produce a reflected instance wrapper %s but got %s",
              realClass, clazz.getName(), refResult.getClass().getName()));
    }
    return clazz.cast(refResult);
  }

  private Object invokeRaw(String methodName, Object... params) {
    try {
      return getReflectedClass()
          .getMethod(
              methodName, Arrays.stream(params).map(Object::getClass).toArray(Class<?>[]::new))
          .invoke(reflectedInstance, params);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Instantiates the given class, trying to find the simplest constructor (passing any parameters
   * as null/default value of that param).
   *
   * <p>The simplest constructor is located by finding the first: (searched in this order)
   *
   * <ol>
   *   <li>Public constructor with no parameters
   *   <li>Any constructor with no parameters
   *   <li>Public constructor with fewest parameters
   *   <li>Any constructor with fewest parameters
   * </ol>
   *
   * If no viable constructor is found, an IllegalArgumentException is thrown.
   */
  public static Object instantiate(String className) {
    try {
      Class<?> clazz = Class.forName(className);
      Constructor<?>[] ctors = clazz.getDeclaredConstructors();

      // Find a suitable ctor - look in this order:
      // 1 - public with no params
      // 2 - no params
      // 3 - public with fewest params
      // 4 - fewest params
      List<Constructor<?>> candidates =
          Arrays.stream(ctors)
              .sorted(
                  Comparator.comparing(
                      c -> c.getParameterCount() + (Modifier.isPublic(c.getModifiers()) ? 0 : 0.1)))
              .collect(Collectors.toList());

      IllegalStateException allExes =
          new IllegalStateException("No constructors could be executed.");
      for (Constructor<?> constructor : candidates) {
        try {
          constructor.setAccessible(true);
          return constructor.newInstance(
              IntStream.range(0, constructor.getParameterCount())
                  .mapToObj(i -> Defaults.defaultValue(constructor.getParameterTypes()[i]))
                  .toArray(Object[]::new));
        } catch (InvocationTargetException ex) {
          allExes.addSuppressed(ex);
        }
      }
      throw allExes;
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(String.format("Cannot instantiate class %s", className), e);
    }
  }

  /** Creates a reflected instance given an instance of the appropriate type. */
  @FunctionalInterface
  public interface ReflectedInstanceFactory {
    ReflectedInstance create(Object instance);

    default ReflectedInstance createDefault() {
      return create(null);
    }
  }

  @Override
  public String toString() {
    return "ReflectedInstance{"
        + "reflectedClass="
        + reflectedClass
        + ", reflectedInstance="
        + reflectedInstance
        + '}';
  }
}
