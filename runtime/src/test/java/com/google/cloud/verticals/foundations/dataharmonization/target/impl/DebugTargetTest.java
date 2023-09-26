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

package com.google.cloud.verticals.foundations.dataharmonization.target.impl;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for DebugTarget. */
@RunWith(JUnit4.class)
public class DebugTargetTest {
  private static final Logger log = Logger.getGlobal();
  private static OutputStream logCapturingStream;
  private static StreamHandler customLogHandler;
  private static DebugTarget debugTarget;

  @Before
  public void setup() {
    DebugTarget.Constructor constructor = new DebugTarget.Constructor();
    debugTarget = constructor.construct(null);

    /**
     * To ensure that the log is receiving the correct information a StreamHandler is being attached
     * to the same Global logger. When a logging event is written to the log, a copy is provided to
     * the stream handler and will be available to the test for comparison.
     */
    logCapturingStream = new ByteArrayOutputStream();
    Handler[] handlers = log.getParent().getHandlers();
    customLogHandler = new StreamHandler(logCapturingStream, handlers[0].getFormatter());
    log.addHandler(customLogHandler);
  }

  public String getTestCapturedLog() throws IOException {
    customLogHandler.flush();
    return logCapturingStream.toString();
  }

  @Test
  public void write_onDebugTarget_array() throws Exception {
    final String expectedLogPart = "{\"num\":5.0}";

    Array array = testDTI().emptyArray();
    array = array.setElement(123, testDTI().primitiveOf(5.0));

    debugTarget.write(null, array);

    String capturedLog = getTestCapturedLog();
    assertThat(capturedLog).contains(expectedLogPart);
  }

  // TODO ()- Remove the following serialization tests when implemented
  @Test
  public void write_onDebugTarget_primitiveNum() throws Exception {
    final String expectedLogPart = "{\"num\":5.0}";

    Primitive numericPrimitive = testDTI().primitiveOf(5.0);

    debugTarget.write(null, numericPrimitive);

    String capturedLog = getTestCapturedLog();
    assertThat(capturedLog).contains(expectedLogPart);
  }

  @Test
  public void write_onDebugTarget_primitiveStr() throws Exception {
    final String expectedLogPart = "{\"str\":\"testPrimitiveString\"";

    Primitive stringPrimitive = testDTI().primitiveOf("testPrimitiveString");

    debugTarget.write(null, stringPrimitive);

    String capturedLog = getTestCapturedLog();
    assertThat(capturedLog).contains(expectedLogPart);
  }

  @Test
  public void write_onDebugTarget_primitiveBool() throws Exception {
    final String expectedLogPart = "{\"bool\":true}";

    Primitive boolPrimitive = testDTI().primitiveOf(true);

    debugTarget.write(null, boolPrimitive);

    String capturedLog = getTestCapturedLog();
    assertThat(capturedLog).contains(expectedLogPart);
  }

  @Test
  public void write_onDebugTarget_container() throws Exception {
    final String expectedLogPart = "{\"container\":{\"test\":{\"num\":5.0}}}";

    Container container =
        testDTI().containerOf(ImmutableMap.of("test", testDTI().primitiveOf(5.0)));

    debugTarget.write(null, container);

    String capturedLog = getTestCapturedLog();
    assertThat(capturedLog).contains(expectedLogPart);
  }
}
