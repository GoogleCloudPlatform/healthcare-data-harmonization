/*
 * Copyright 2020 Google LLC.
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

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.Dataset;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.debug.DebugInfo;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.WhistleStackOverflowError;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.converters.BooleanConverter;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.converters.Converter;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.converters.DoubleConverter;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.converters.IntegerConverter;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.converters.LongConverter;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.converters.NoopConverter;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.converters.NullConverter;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.converters.StringConverter;
import com.google.cloud.verticals.foundations.dataharmonization.function.signature.Signature;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.VerifyException;
import com.google.common.collect.Iterables;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;

/** A JavaFunction wraps a {@link Method} into a {@link CallableFunction}. */
public class JavaFunction extends CallableFunction {
  protected transient Method javaFunction;
  protected final Serializable instance;
  protected final Signature signature;
  protected final List<Converter<?>> converters = new ArrayList<>();
  protected final boolean firstArgIsRuntimeContext;

  // A placeholder package name for testing purposes;
  public static final String DEFAULT_PACKAGE_NAME = "defaultJavaPkg";

  /**
   * Create a JavaFunction wrapping a {@link Method}. If the Method is not static, an object
   * instance on which to call it can be given.
   *
   * @throws IllegalArgumentException If
   *     <ul>
   *       <li>The given method is not static, but an object instance is not supplied.
   *       <li>The method is not public.
   *     </ul>
   */
  @VisibleForTesting
  public JavaFunction(@Nonnull Method javaFunction, Serializable instance) {
    this(DEFAULT_PACKAGE_NAME, javaFunction, instance, /*inheritParentVars*/ false);
  }

  /**
   * Create a JavaFunction wrapping a {@link Method}. If the Method is not static, an object
   * instance on which to call it can be given.
   *
   * @throws IllegalArgumentException If
   *     <ul>
   *       <li>The given method is not static, but an object instance is not supplied.
   *       <li>The method is not public.
   *     </ul>
   */
  public JavaFunction(
      @Nonnull String packageName,
      @Nonnull Method javaFunction,
      Serializable instance,
      boolean inheritParentVars) {
    this.javaFunction = javaFunction;
    this.instance = instance;

    if (!Modifier.isStatic(javaFunction.getModifiers()) && instance == null) {
      throw new IllegalArgumentException(
          String.format(
              "Method %s is not static, so an object instance must be supplied.", javaFunction));
    }

    if (!Modifier.isPublic(javaFunction.getModifiers())) {
      throw new IllegalArgumentException(String.format("Method %s must be public.", javaFunction));
    }

    firstArgIsRuntimeContext =
        javaFunction.getParameterCount() > 0
            && RuntimeContext.class.isAssignableFrom(javaFunction.getParameterTypes()[0]);
    this.signature =
        generateSignature(
            packageName, javaFunction, converters, firstArgIsRuntimeContext, inheritParentVars);
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
                new JavaFunction(
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
      @Nonnull Serializable instance, String packageName) {
    return Arrays.stream(instance.getClass().getDeclaredMethods())
        .filter(m -> !Modifier.isStatic(m.getModifiers())) // not static (we have an instance)
        .filter(m -> m.isAnnotationPresent(PluginFunction.class)) // annotated
        .map(
            m ->
                new JavaFunction(
                    packageName,
                    m,
                    instance,
                    m.getDeclaredAnnotation(PluginFunction.class).inheritParentVars()))
        .collect(Collectors.toList());
  }

  public static Signature generateSignature(
      String packageName,
      Method javaFunction,
      List<Converter<?>> converters,
      boolean firstArgIsRuntimeContext,
      boolean inheritParentVars) {
    // TODO(): Support void outputs.
    if (!Data.class.isAssignableFrom(javaFunction.getReturnType())) {
      throw new IllegalArgumentException(
          String.format(
              "Java Method %s's return type is of an unsupported type %s.",
              javaFunction.getName(), javaFunction.getReturnType().getSimpleName()));
    }

    List<Class<? extends Data>> argTypes = new ArrayList<>(javaFunction.getParameterCount());
    Parameter[] parameters = javaFunction.getParameters();
    for (int i = 0; i < parameters.length; i++) {
      if (firstArgIsRuntimeContext && i == 0) {
        continue;
      }

      Parameter parameter = parameters[i];
      Class<?> parameterType = parameter.getType();
      if (parameter.isVarArgs()) {
        parameterType = parameterType.getComponentType();
      }

      Class<? extends Data> dataType;
      Converter<?> converter;

      // TODO (): define asClass in Primitive to handle casting from primitives to java
      // native data types. After that we only need noop converter.
      if (String.class.equals(parameterType)) {
        dataType = Primitive.class;
        converter = new StringConverter();
      } else if (Boolean.class.equals(parameterType)) {
        dataType = Primitive.class;
        converter = new BooleanConverter();
      } else if (Double.class.equals(parameterType)) {
        dataType = Primitive.class;
        converter = new DoubleConverter();
      } else if (Long.class.equals(parameterType)) {
        dataType = Primitive.class;
        converter = new LongConverter();
      } else if (Integer.class.equals(parameterType)) {
        dataType = Primitive.class;
        converter = new IntegerConverter();
      } else if (NullData.class.equals(parameterType)) {
        dataType = NullData.class;
        converter = new NullConverter();
      } else if (Primitive.class.isAssignableFrom(parameterType)) {
        dataType = parameterType.asSubclass(Primitive.class);
        converter = new NoopConverter(dataType);
      } else if (Container.class.isAssignableFrom(parameterType)) {
        dataType = parameterType.asSubclass(Container.class);
        converter = new NoopConverter(dataType);
      } else if (Array.class.isAssignableFrom(parameterType)) {
        dataType = parameterType.asSubclass(Array.class);
        converter = new NoopConverter(dataType);
      } else if (Dataset.class.isAssignableFrom(parameterType)) {
        dataType = parameterType.asSubclass(Dataset.class);
        converter = new NoopConverter(dataType);
      } else if (Data.class.isAssignableFrom(parameterType)) {
        dataType = parameterType.asSubclass(Data.class);
        converter = new NoopConverter(dataType);
      } else if (RuntimeContext.class.isAssignableFrom(parameterType)) {
        throw new IllegalArgumentException(
            String.format(
                "Java Method %s's parameter is a %s parameter, but only the first parameter can be"
                    + " a %s (and it cannot be variadic).",
                javaFunction.getName(),
                RuntimeContext.class.getSimpleName(),
                RuntimeContext.class.getSimpleName()));
      } else {
        throw new IllegalArgumentException(
            String.format(
                "Java Method %s's parameter %s is of an unsupported type %s.",
                javaFunction.getName(), parameter.getName(), parameterType.getSimpleName()));
      }

      argTypes.add(dataType);
      converters.add(converter);
    }

    return new Signature(
        packageName, javaFunction.getName(), argTypes, javaFunction.isVarArgs(), inheritParentVars);
  }

  /**
   * Collects arguments from an invocation into an array suitable for assignment to a variadic
   * parameter of a function invoked through reflection.
   *
   * <p>In a variadic function, the last parameter of type T (where T extends {@link Data}) is
   * variadic, and that means the function will refer to this parameter as a T[]. This array may
   * have 0 or more values, and the invoker can assign this parameter in one of 3 ways:
   *
   * <ol>
   *   <li>Omitted entirely - yielding an empty T[]
   *   <li>As an {@link Array} where each element is an instance of T - yielding that as a T[]
   *   <li>As one or more separate T arguments - yielding them as a collected T[]
   * </ol>
   *
   * When invoking a variadic function through reflection (using {@link Method#invoke(Object,
   * Object...)}), the variadic parameter must be an actual T[] (i.e. not an Object[] or a Data[]).
   * Since T[] does not extend from Object[] but extends from Object directly, this method will
   * return an Object that is actually a T[] where T is the type of the variadic parameter.
   *
   * @param args The args from which to extract the variadic array.
   * @return a T[] where T is the exact type specified by {@link Signature#getLastArgType()}.
   */
  protected Object getVariadic(Data[] args) {
    // Beware, dragons ahead! This method uses reflection to create dynamically typed arrays, which
    // basically drop kicks type safety out the window entirely, as T[] (for any type T) does not
    // extend Object[], but instead extends Object directly.
    int startVariadicIndex = signature.getArgs().size() - 1;

    // This is the component type of the array we are building (i.e. we are building a T[]).
    Class<?> t =
        javaFunction.getParameterTypes()[javaFunction.getParameterCount() - 1].getComponentType();

    // Case 1 - Omitted entirely.
    if (args.length <= startVariadicIndex) {
      return java.lang.reflect.Array.newInstance(t, 0);
    }

    Converter<?> con = Iterables.getLast(converters);
    // Case 2 - an Array instance.
    if (args.length == startVariadicIndex + 1
        && args[startVariadicIndex] != null
        && !args[startVariadicIndex].isNullOrEmpty()
        && args[startVariadicIndex].isArray()
        && !Array.class.isAssignableFrom(t)) {
      Array array = args[startVariadicIndex].asArray();
      Object result = java.lang.reflect.Array.newInstance(t, array.size());
      IntStream.range(0, array.size())
          .forEach(i -> java.lang.reflect.Array.set(result, i, con.convert(array.getElement(i))));
      return result;
    }

    // Case 3 - one or more separate arguments.
    int size = args.length - startVariadicIndex;
    Object result = java.lang.reflect.Array.newInstance(t, size);
    IntStream.range(0, size)
        .forEach(
            i -> java.lang.reflect.Array.set(result, i, con.convert(args[i + startVariadicIndex])));
    return result;
  }

  protected Object[] prepareFinalArgs(RuntimeContext context, Data[] args) {
    if (args.length > converters.size() && !signature.isVariadic()) {
      throw new IllegalArgumentException("Too many arguments");
    }

    // Try to allocate once and minimize copying, since this is happening every time a function
    // is called.
    Object[] finalArgs = new Object[converters.size() + (firstArgIsRuntimeContext ? 1 : 0)];
    int offset = 0;
    if (firstArgIsRuntimeContext) {
      finalArgs[0] = context;
      offset = 1;
    }

    for (int i = 0; i < converters.size(); i++) {
      if (i == converters.size() - 1 && signature.isVariadic()) {
        finalArgs[i + offset] = getVariadic(args);
        break;
      }

      if (args.length <= i) {
        throw new IllegalArgumentException("Not enough arguments");
      }

      finalArgs[i + offset] = converters.get(i).convert(args[i]);
    }
    return finalArgs;
  }

  @Override
  public Data callInternal(RuntimeContext context, Data... args) {
    try {
      Object[] finalArgs = prepareFinalArgs(context, args);

      return (Data) javaFunction.invoke(instance, finalArgs);
    } catch (IllegalAccessException e) {
      throw new VerifyException(
          String.format(
              "Access error calling %s - make sure it is public and accessible.",
              javaFunction.getName()),
          e);
    } catch (InvocationTargetException e) {
      if (e.getCause() instanceof WhistleStackOverflowError) {
        throw (WhistleStackOverflowError) e.getCause();
      }
      if (e.getCause() instanceof RuntimeException) {
        throw (RuntimeException) e.getCause();
      }
      if (e.getTargetException() instanceof StackOverflowError) {
        throw new WhistleStackOverflowError(context.top());
      }
      throw new VerifyException(e.getTargetException());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          // We don't actually know what kind of argument error...
          String.format(
              "Argument error: want: %s, got: (%s). See cause below for more information.",
              signature,
              Arrays.stream(args)
                  .map(d -> d == null ? "null" : d.getClass().getSimpleName())
                  .collect(Collectors.joining(", "))),
          e);
    }
  }

  @Override
  public Signature getSignature() {
    return signature;
  }

  @Override
  public DebugInfo getDebugInfo() {
    return DebugInfo.fromJavaFunction(signature.getPackageName(), javaFunction);
  }

  private void writeObject(ObjectOutputStream oos) throws Exception {
    oos.defaultWriteObject();
    oos.writeObject(javaFunction.getDeclaringClass());
    oos.writeUTF(javaFunction.getName());
    oos.writeObject(javaFunction.getParameterTypes());
  }

  private void readObject(ObjectInputStream ois) throws Exception {
    ois.defaultReadObject();
    Class<?> declaringClass = (Class<?>) ois.readObject();
    String name = ois.readUTF();
    Class<?>[] paramTypes = (Class<?>[]) ois.readObject();
    javaFunction = declaringClass.getMethod(name, paramTypes);
  }

  public Method getMethod() {
    return javaFunction;
  }
}
