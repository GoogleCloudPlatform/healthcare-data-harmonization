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

import static org.junit.Assert.fail;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.DefaultContainer;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.WrapperDataUtils.TestWrapperData;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Container class for examples of java methods supported (and not) by the {@link JavaFunction} API.
 */
// Methods are accessed only through reflection, so they incorrectly report as unused.
@SuppressWarnings("unused")
final class TestMethods implements Serializable {
  private static final Map<String, Method> methods;

  static {
    Map<String, Method> foundMethods = new HashMap<>();
    for (Method m : TestMethods.class.getDeclaredMethods()) {
      foundMethods.put(m.getName(), m);
    }

    methods = Collections.unmodifiableMap(foundMethods);
  }

  private final transient InvocationCapture delegate;

  TestMethods(InvocationCapture delegate) {
    this.delegate = delegate;
  }

  public static Method get(String name) {
    if (!methods.containsKey(name)) {
      fail(String.format("No test method named %s", name));
    }

    return methods.get(name);
  }

  public static Data staticMethod() {
    return null;
  }

  public Object invalidReturnType() {
    fail("This method should never be called successfully - it's not valid");
    return null;
  }

  public Data invalidArgType(Object foo) {
    fail("This method should never be called successfully - it's not valid");
    return null;
  }

  public Data invalidRuntimeContextPosition(Data arg, RuntimeContext context) {
    fail("This method should never be called successfully - it's not valid");
    return null;
  }

  public Data invalidMultipleRuntimeContexts(RuntimeContext context1, RuntimeContext context2) {
    fail("This method should never be called successfully - it's not valid");
    return null;
  }

  public Data invalidVariadicRuntimeContexts(RuntimeContext... context) {
    fail("This method should never be called successfully - it's not valid");
    return null;
  }

  private Data invalidPrivateProtection() {
    fail("This method should never be called successfully - it's not valid");
    return null;
  }

  protected Data invalidProtectedProtection() {
    fail("This method should never be called successfully - it's not valid");
    return null;
  }

  Data invalidPackageProtectedProtection() {
    fail("This method should never be called successfully - it's not valid");
    return null;
  }

  public Data noArgs() {
    return delegate.capture(null);
  }

  public Data oneArg(Container arg) {
    return delegate.capture(null, arg);
  }

  public Data multipleArgs(Container arg, Array arg2) {
    return delegate.capture(null, arg, arg2);
  }

  public Primitive specificReturnValue() {
    return (Primitive) delegate.capture(null);
  }

  public Data variadicArgs(Container... arg) {
    return delegate.capture(null, (Object[]) arg);
  }

  public Data variadicPrimitiveArgs(String... arg) {
    return delegate.capture(null, (Object[]) arg);
  }

  public Data variadicAndRegularArgs(Container arg, Container... args) {
    Data[] allArgs = new Data[1 + args.length];
    allArgs[0] = arg;
    System.arraycopy(args, 0, allArgs, 1, args.length);
    return delegate.capture(null, (Object[]) allArgs);
  }

  public Data runtimeContextParam(RuntimeContext context) {
    return delegate.capture(context);
  }

  public Data runtimeContextParamWithOtherArgs(RuntimeContext context, Container arg) {
    return delegate.capture(context, arg);
  }

  public Data specificImplArg(DefaultContainer arg) {
    return delegate.capture(null, arg);
  }

  public Data primitiveArgs(Boolean bool, String str, Double num) {
    return delegate.capture(null, bool, str, num);
  }

  public Data specificWrapperDataArg(TestWrapperData data) {
    return delegate.capture(null, data);
  }
}
