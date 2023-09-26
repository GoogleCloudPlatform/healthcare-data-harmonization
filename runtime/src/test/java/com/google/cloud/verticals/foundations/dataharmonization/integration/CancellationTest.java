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

package com.google.cloud.verticals.foundations.dataharmonization.integration;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.CancellationToken;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.CancellationToken.CancelledException;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.impl.DefaultCancellationToken;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.JavaFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.PluginFunction;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Plugin;
import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Integration tests for blocks. This aims to test (end-to-end) mechanisms and semantics of block
 * expressions, and how they interact with other structures and language mechanics.
 */
@RunWith(JUnit4.class)
public class CancellationTest {
  private static final String SUBDIR = "cancellation/";
  private static final IntegrationTest TEST = new IntegrationTest(SUBDIR);

  @Test
  public void cancel_block() throws IOException {
    TestPlugin plugin = new TestPlugin();
    Engine engine =
        TEST.initializeBuilderWithTestFile("block.wstl")
            .withDefaultPlugins(plugin)
            .initialize()
            .build();
    CancelledException ex =
        assertThrows(CancelledException.class, () -> engine.transform(NullData.instance));
    assertThat(ex).hasMessageThat().contains("testing");
    assertThat(plugin.badCall).isFalse();
  }

  @Test
  public void cancel_iterated() throws IOException {
    TestPlugin plugin = new TestPlugin();
    Engine engine =
        TEST.initializeBuilderWithTestFile("iterated.wstl")
            .withDefaultPlugins(plugin)
            .initialize()
            .build();
    CancelledException ex =
        assertThrows(CancelledException.class, () -> engine.transform(NullData.instance));
    assertThat(ex).hasMessageThat().contains("testing");
    assertThat(plugin.badCall).isFalse();
    assertThat(plugin.records).containsExactly(1., 2., 3.);
  }

  @Test
  public void cancel_iterated_with_cancel_token_callback() throws IOException {
    TestPlugin plugin = new TestPlugin();
    CancellationToken token = new DefaultCancellationToken();
    AtomicInteger cancelCount = new AtomicInteger();
    cancelCount.set(1);
    token.registerCancelCallback(
        c -> {
          cancelCount.getAndIncrement();
        });

    Engine engine =
        TEST.initializeBuilderWithTestFile("iterated.wstl")
            .withDefaultPlugins(plugin)
            .initialize()
            .build(token);
    CancelledException ex =
        assertThrows(CancelledException.class, () -> engine.transform(NullData.instance));
    assertThat(ex).hasMessageThat().contains("testing");
    assertThat(plugin.badCall).isFalse();
    assertThat(plugin.records).containsExactly(1., 2., 3.);
    assertThat(cancelCount.get()).isEqualTo(2);
  }

  @Test
  public void cancel_nested() throws IOException {
    TestPlugin plugin = new TestPlugin();
    Engine engine =
        TEST.initializeBuilderWithTestFile("nested.wstl")
            .withDefaultPlugins(plugin)
            .initialize()
            .build();
    CancelledException ex =
        assertThrows(CancelledException.class, () -> engine.transform(NullData.instance));
    assertThat(ex).hasMessageThat().contains("testing");
    assertThat(plugin.badCall).isFalse();
    assertThat(plugin.records)
        .containsExactlyElementsIn(
            IntStream.range(0, 10).mapToDouble(Double::valueOf).boxed().collect(toImmutableList()));
  }

  @Test
  public void cancel_nested_with_cancel_token_callback() throws IOException {
    TestPlugin plugin = new TestPlugin();
    CancellationToken token = new DefaultCancellationToken();
    AtomicInteger cancelCount = new AtomicInteger();
    cancelCount.set(1);
    token.registerCancelCallback(
        c -> {
          cancelCount.getAndIncrement();
        });
    Engine engine =
        TEST.initializeBuilderWithTestFile("nested.wstl")
            .withDefaultPlugins(plugin)
            .initialize()
            .build(token);
    CancelledException ex =
        assertThrows(CancelledException.class, () -> engine.transform(NullData.instance));
    assertThat(ex).hasMessageThat().contains("testing");
    assertThat(plugin.badCall).isFalse();
    assertThat(plugin.records)
        .containsExactlyElementsIn(
            IntStream.range(0, 10).mapToDouble(Double::valueOf).boxed().collect(toImmutableList()));
    assertThat(cancelCount.get()).isEqualTo(2);
  }

  public static class TestPlugin implements Plugin {
    private final List<Double> records = new ArrayList<>();
    private boolean badCall = false;

    @Override
    public String getPackageName() {
      return "test";
    }

    @Override
    public List<CallableFunction> getFunctions() {
      return ImmutableList.copyOf(JavaFunction.ofPluginFunctionsInInstance(this, getPackageName()));
    }

    @PluginFunction
    public NullData cancel(RuntimeContext context) {
      context.getCancellation().cancel("testing");
      return NullData.instance;
    }

    @PluginFunction
    public Primitive record(RuntimeContext context, Double i) {
      records.add(i);
      return context.getDataTypeImplementation().primitiveOf(i);
    }

    @PluginFunction
    public NullData shouldNeverBeCalled() {
      badCall = true;
      throw new VerifyException("shouldNeverBeCalled was called");
    }
  }
}
