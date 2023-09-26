/*
 * Copyright 2021 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.init;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.cloud.verticals.foundations.dataharmonization.exceptions.WhistleRuntimeException;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContextMonitor;
import com.google.cloud.verticals.foundations.dataharmonization.init.initializer.ExternalConfigExtractor;
import com.google.cloud.verticals.foundations.dataharmonization.integration.plugin.TestLoaderPlugin;
import com.google.cloud.verticals.foundations.dataharmonization.integration.plugin.TestLoaderPlugin.ResourceLoader;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Tests for Engine.transform. */
@RunWith(Parameterized.class)
public class EngineTransformTest {

  private Engine engine;
  private final String config;
  private final String inputPath;
  private final String expectedOutput;
  private final Boolean throwHiddenException;
  private final String expectedExceptionMsg;
  public final Class<? extends Exception> expectedExceptionClass;

  public EngineTransformTest(
      String config,
      String inputPath,
      String expectedOutput,
      Boolean throwHiddenException,
      String expectedExceptionMsg,
      Class<? extends Exception> expectedExceptionClass) {
    this.config = config;
    this.inputPath = inputPath;
    this.expectedOutput = expectedOutput;
    this.throwHiddenException = throwHiddenException;
    this.expectedExceptionMsg = expectedExceptionMsg;
    this.expectedExceptionClass = expectedExceptionClass;
  }

  @Parameters
  public static List<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {"/transform.wstl", "/transform.json", "\"transform_test\"", false, "", null},
          {
            "/exceptionTransform.wstl",
            "/transform.json",
            "\"transform_test\"",
            true,
            "HiddenException: Hidden UnsupportedOperationException",
            WhistleRuntimeException.class
          },
          {
            "/exceptionTransform.wstl",
            "/transform.json",
            "\"transform_test\"",
            false,
            "UnsupportedOperationException: Attempted to key into non-container"
                + " Primitive/DefaultPrimitive with field a",
            WhistleRuntimeException.class
          }
        });
  }

  @Before
  public void setUp() throws URISyntaxException, IOException {
    URI whistleConfig = new URI(ResourceLoader.TEST_LOADER, null, config, null);
    engine =
        new Engine.Builder(ExternalConfigExtractor.of(whistleConfig, new URI("")))
            .withDefaultPlugins(new TestLoaderPlugin())
            .setNoDataInExceptions(throwHiddenException)
            .initialize()
            .build();
    // Init the context
    CallableFunction.identity().call(engine.getRuntimeContext());
  }

  @After
  public void teardown() {
    engine.close();
  }

  @Test
  public void testTransform_inlineString() throws IOException {
    String input =
        new String(
            ByteStreams.toByteArray(EngineInitializeTest.class.getResourceAsStream(inputPath)),
            UTF_8);

    if (expectedExceptionMsg.isEmpty()) {
      String actual = engine.transform(input);
      assertEquals(expectedOutput, actual);
    } else {
      Exception e = assertThrows(expectedExceptionClass, () -> engine.transform(input));
      assertTrue(
          String.format("Didn't find \"%s\" in\n\"%s\"", expectedExceptionMsg, e.getMessage()),
          e.getMessage().contains(expectedExceptionMsg));
    }
  }

  @Test
  public void testTransform_uri() throws IOException, URISyntaxException {
    URI inputUri = new URI(ResourceLoader.TEST_LOADER, null, inputPath, null);
    if (expectedExceptionMsg.isEmpty()) {
      String actual = engine.transform(inputUri);
      assertEquals(expectedOutput, actual);
    } else {
      Exception e = assertThrows(expectedExceptionClass, () -> engine.transform(inputUri));
      assertTrue(
          String.format("Didn't find \"%s\" in\n\"%s\"", expectedExceptionMsg, e.getMessage()),
          e.getMessage().contains(expectedExceptionMsg));
    }
  }

  @Test
  public void testFinish_called() throws IOException {
    String input =
        new String(
            ByteStreams.toByteArray(EngineInitializeTest.class.getResourceAsStream(inputPath)),
            UTF_8);
    RuntimeContextMonitor monitor = mock(RuntimeContextMonitor.class);

    if (expectedExceptionMsg.isEmpty()) {
      engine.getRuntimeContext().addMonitor(monitor);
      engine.transform(input);
      verify(monitor).onRuntimeContextFinish(any(), any());
    } else {
      Exception e = assertThrows(expectedExceptionClass, () -> engine.transform(input));
      assertTrue(
          String.format("Didn't find \"%s\" in\n\"%s\"", expectedExceptionMsg, e.getMessage()),
          e.getMessage().contains(expectedExceptionMsg));
    }
  }
}
