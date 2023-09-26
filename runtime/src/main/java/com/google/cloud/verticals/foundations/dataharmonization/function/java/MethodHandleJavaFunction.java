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

package com.google.cloud.verticals.foundations.dataharmonization.function.java;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.WhistleStackOverflowError;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.VerifyException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/**
 * An alternative implementation of {@link JavaFunction} that invokes the wrapped {@link Method}
 * using {@link MethodHandle#invokeWithArguments(Object...)} instead of @{@link
 * Method#invoke(Object, Object...)}.
 *
 * <p>TODO(): Replace Whistle core's JavaFunction
 */
public class MethodHandleJavaFunction extends JavaFunction {
  private transient MethodHandle javaFunctionHandle;

  /**
   * Create a MethodHandleJavaFunction wrapping a {@link Method}. If the Method is not static, an
   * object instance on which to call it can be given.
   *
   * @throws IllegalArgumentException If
   *     <ul>
   *       <li>The given method is not static, but an object instance is not supplied.
   *       <li>The method is not public.
   *     </ul>
   */
  @VisibleForTesting
  public MethodHandleJavaFunction(@Nonnull Method javaFunction, Serializable instance) {
    this(DEFAULT_PACKAGE_NAME, javaFunction, instance, /*inheritParentVars*/ false);
  }

  /**
   * Create a MethodHandleJavaFunction wrapping a {@link Method}. If the Method is not static, an
   * object instance on which to call it can be given.
   *
   * @throws IllegalArgumentException If
   *     <ul>
   *       <li>The given method is not static, but an object instance is not supplied.
   *       <li>The method is not public.
   *     </ul>
   */
  public MethodHandleJavaFunction(
      @Nonnull String packageName,
      @Nonnull Method javaFunction,
      Serializable instance,
      boolean inheritParentVars) {
    super(packageName, javaFunction, instance, inheritParentVars);
    try {
      this.javaFunctionHandle = MethodHandles.lookup().unreflect(javaFunction).asFixedArity();
    } catch (IllegalAccessException e) {
      throw new IllegalArgumentException(e);
    }

    if (instance != null) {
      this.javaFunctionHandle = this.javaFunctionHandle.bindTo(instance);
    }
  }

  @Override
  public Data callInternal(RuntimeContext context, Data... args) {
    Object[] finalArgs = prepareFinalArgs(context, args);

    try {
      return (Data) javaFunctionHandle.invokeWithArguments(finalArgs);
    } catch (RuntimeException | WhistleStackOverflowError e) {
      throw e;
    } catch (Throwable other) {
      throw new VerifyException(other);
    }
  }

  private void readObject(ObjectInputStream ois) throws Exception {
    javaFunctionHandle = MethodHandles.lookup().unreflect(javaFunction).asFixedArity();
    if (instance != null) {
      javaFunctionHandle = javaFunctionHandle.bindTo(instance);
    }
  }

  /**
   * Wraps all static functions declared in the given class that are annotated with {@link
   * PluginFunction}.
   */
  public static List<JavaFunction> ofPluginFunctionsInClass(Class<?> clazz, String packageName) {
    return Arrays.stream(clazz.getDeclaredMethods())
        .filter(m -> Modifier.isStatic(m.getModifiers())) // static
        .filter(m -> m.isAnnotationPresent(PluginFunction.class)) // annotated
        .map(
            m ->
                new MethodHandleJavaFunction(
                    packageName,
                    m,
                    null,
                    m.getDeclaredAnnotation(PluginFunction.class).inheritParentVars()))
        .collect(Collectors.toList());
  }

  /**
   * Wraps all non-static functions declared in the given instance that are annotated with {@link
   * PluginFunction}. The given instance is used as the instance on which those functions will be
   * called. NOTE: This instance's state may be reset to the state passed to this method (assuming
   * this method is called during the plugin loading stage) throughout the execution of the engine.
   */
  public static List<JavaFunction> ofPluginFunctionsInInstance(
      Serializable instance, String packageName) {
    return Arrays.stream(instance.getClass().getDeclaredMethods())
        .filter(m -> !Modifier.isStatic(m.getModifiers())) // not static (we have an instance)
        .filter(m -> m.isAnnotationPresent(PluginFunction.class)) // annotated
        .map(
            m ->
                new MethodHandleJavaFunction(
                    packageName,
                    m,
                    instance,
                    m.getDeclaredAnnotation(PluginFunction.class).inheritParentVars()))
        .collect(Collectors.toList());
  }
}
