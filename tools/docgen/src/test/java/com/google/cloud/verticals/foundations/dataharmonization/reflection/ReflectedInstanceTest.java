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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.Correspondence;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;

/** Tests for reflection utility. */
public class ReflectedInstanceTest {
  /** Placeholder test class number 2. */
  public static class TestClass1 {
    private final int id;

    public TestClass1(int id) {
      this.id = id;
    }

    public String getSomeString(String prefix) {
      return String.format("%s from %d", prefix, id);
    }

    public TestClass1 getSelf(Integer id) {
      return new TestClass1(id);
    }

    public TestClass2 getT2(Integer id) {
      return new TestClass2(id);
    }

    public List<String> getPrimList() {
      return ImmutableList.of("one", "two");
    }

    public List<TestClass2> getTestClassList(Integer ids) {
      return ImmutableList.of(new TestClass2(ids), new TestClass2(ids));
    }
  }

  /** Placeholder test class number 2. */
  public static class TestClass2 {
    private final int id;

    public TestClass2(int id) {
      this.id = id;
    }
  }

  private static class ReflectedT1 extends ReflectedInstance {

    protected ReflectedT1(Object instance) {
      super(instance);
    }

    @Override
    protected String getReflectedClassName() {
      return TestClass1.class.getName();
    }

    String getSomeString(String prefix) {
      return invoke(String.class, "getSomeString", prefix);
    }

    public ReflectedT1 getSelf(int newId) {
      return invoke(ReflectedT1.class, "getSelf", newId);
    }

    public ReflectedT2 getT2(int newId) {
      return invoke(ReflectedT2.class, "getT2", newId);
    }

    public List<String> getPrimList() {
      return invokeList(String.class, "getPrimList");
    }

    public List<ReflectedT2> getTestClassList(Integer ids) {
      return invokeList(ReflectedT2.class, "getTestClassList", ids);
    }
  }

  private static class ReflectedT2 extends ReflectedInstance {

    private final TestClass2 typedInstance;

    protected ReflectedT2(Object instance) {
      super(instance);
      // We can do this because in the test we have precise control over what goes here.
      this.typedInstance = (TestClass2) instance;
    }

    @Override
    protected String getReflectedClassName() {
      return TestClass2.class.getName();
    }

    public TestClass2 getInstance() {
      return typedInstance;
    }
  }

  @BeforeClass
  public static void setUp() {
    ReflectedInstance.registerFactory(ReflectedT1::new);
    ReflectedInstance.registerFactory(ReflectedT2::new);
  }

  @Test
  public void invoke_primitive_returns() {
    TestClass1 t1 = new TestClass1(123);
    ReflectedT1 rt1 = new ReflectedT1(t1);

    assertThat(rt1.getSomeString("hello")).isEqualTo("hello from 123");
  }

  @Test
  public void invoke_wrappable_returnsWrapped() {
    TestClass1 t1 = new TestClass1(123);
    ReflectedT1 rt1 = new ReflectedT1(t1);

    ReflectedT1 selfActual = rt1.getSelf(321);
    assertThat(selfActual.getSomeString("hello")).isEqualTo("hello from 321");

    ReflectedT2 t2Actual = rt1.getT2(456);
    assertThat(t2Actual.getInstance().id).isEqualTo(456);
  }

  @Test
  public void invokeList_primitive_returns() {
    TestClass1 t1 = new TestClass1(123);
    ReflectedT1 rt1 = new ReflectedT1(t1);

    List<String> gotPrim = rt1.getPrimList();
    assertThat(gotPrim).containsExactly("one", "two");
  }

  @Test
  public void invokeList_wrappable_returnsWrapped() {
    TestClass1 t1 = new TestClass1(123);
    ReflectedT1 rt1 = new ReflectedT1(t1);

    List<ReflectedT2> gotPrim = rt1.getTestClassList(999);
    assertThat(gotPrim)
        .comparingElementsUsing(
            Correspondence.<ReflectedT2, Integer>transforming(
                rt2 -> rt2.getInstance().id, "has a test class 2 instance with id of"))
        .containsExactly(999, 999);
  }

  private abstract static class CtorTestBase {
    protected boolean wrongConstructorCalled;
  }

  private static class NoParamsPublicCtor extends CtorTestBase {
    public NoParamsPublicCtor() {
      // This one should get picked!
    }

    public NoParamsPublicCtor(String something) {
      wrongConstructorCalled = true;
    }

    private NoParamsPublicCtor(int something) {
      wrongConstructorCalled = true;
    }
  }

  private static class NoParamsPrivateCtor extends CtorTestBase {
    private NoParamsPrivateCtor() {
      // This one should get picked!
    }

    public NoParamsPrivateCtor(String something) {
      wrongConstructorCalled = true;
    }

    private NoParamsPrivateCtor(int something) {
      wrongConstructorCalled = true;
    }
  }

  private static class SomeParamsPublicCtor extends CtorTestBase {
    public SomeParamsPublicCtor(String something) {
      // This one should get picked!
      assertThat(something).isNull();
    }

    public SomeParamsPublicCtor(String something, int somethingElse) {
      wrongConstructorCalled = true;
    }

    private SomeParamsPublicCtor(int something) {
      wrongConstructorCalled = true;
    }
  }

  private static class SomeParamsPrivateCtor extends CtorTestBase {
    private SomeParamsPrivateCtor(int something) {
      // This one should get picked!
      assertThat(something).isEqualTo(0);
    }

    private SomeParamsPrivateCtor(String something, boolean somethingElse) {
      wrongConstructorCalled = true;
    }
  }

  private static class NoParamsPublicCtorThrows extends CtorTestBase {
    public NoParamsPublicCtorThrows() {
      wrongConstructorCalled = true;
      throw new IllegalArgumentException("Woops");
    }

    public NoParamsPublicCtorThrows(String something) {
      // This one should get picked!
    }

    private NoParamsPublicCtorThrows(int something) {
      wrongConstructorCalled = true;
    }
  }

  private static class AllCtorsThrow extends CtorTestBase {
    public AllCtorsThrow() {
      throw new IllegalArgumentException("Woops 1");
    }

    public AllCtorsThrow(String something) {
      throw new IllegalArgumentException("Woops 2");
    }

    private AllCtorsThrow(int something) {
      throw new IllegalArgumentException("Woops 3");
    }
  }

  @Test
  public void instantiate_defaultPublicCtor() {
    CtorTestBase got =
        (CtorTestBase) ReflectedInstance.instantiate(NoParamsPublicCtor.class.getName());
    assertThat(got).isNotNull();
    assertThat(got.wrongConstructorCalled).isFalse();
  }

  @Test
  public void instantiate_defaultPrivateCtor() {
    CtorTestBase got =
        (CtorTestBase) ReflectedInstance.instantiate(NoParamsPrivateCtor.class.getName());
    assertThat(got).isNotNull();
    assertThat(got.wrongConstructorCalled).isFalse();
  }

  @Test
  public void instantiate_paramsPublicCtor() {
    CtorTestBase got =
        (CtorTestBase) ReflectedInstance.instantiate(SomeParamsPublicCtor.class.getName());
    assertThat(got).isNotNull();
    assertThat(got.wrongConstructorCalled).isFalse();
  }

  @Test
  public void instantiate_paramsPrivateCtor() {
    CtorTestBase got =
        (CtorTestBase) ReflectedInstance.instantiate(SomeParamsPrivateCtor.class.getName());
    assertThat(got).isNotNull();
    assertThat(got.wrongConstructorCalled).isFalse();
  }

  @Test
  public void instantiate_defaultPublicCtorThrows() {
    CtorTestBase got =
        (CtorTestBase) ReflectedInstance.instantiate(NoParamsPublicCtorThrows.class.getName());
    assertThat(got).isNotNull();
    assertThat(got.wrongConstructorCalled).isFalse();
  }

  @Test
  public void instantiate_allCtorsThrow() {
    IllegalStateException got =
        assertThrows(
            IllegalStateException.class,
            () -> ReflectedInstance.instantiate(AllCtorsThrow.class.getName()));
    assertThat(got.getSuppressed())
        .asList()
        .comparingElementsUsing(
            Correspondence.transforming(
                (InvocationTargetException e) -> e.getTargetException().getMessage(),
                "has a target exception with a message of"))
        .containsExactly("Woops 1", "Woops 2", "Woops 3")
        .inOrder();
  }
}
