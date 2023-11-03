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

package com.google.cloud.healthcare.etl.plugins.logging;

import static org.junit.Assert.assertEquals;

import com.google.cloud.verticals.foundations.dataharmonization.plugins.logging.LoggingFns;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.logging.LoggingPlugin;
import com.google.common.testing.TestLogHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link LoggingFns}. */
@RunWith(JUnit4.class)
public class LoggingFnsTest {
  private static final String MESSAGE = "TestMessage";

  TestLogHandler handler;

  @Before
  public void setUp() {
    handler = new TestLogHandler();
    Logger.getLogger(LoggingPlugin.PACKAGE_NAME).addHandler(handler);
  }

  private void assertLog(String message, Level severity) {
    LogRecord firstRecord = handler.getStoredLogRecords().get(0);
    assertEquals(firstRecord.getLevel(), severity);
    assertEquals(firstRecord.getMessage(), message);
  }

  @After
  public void tearDown() {
    Logger.getLogger(LoggingPlugin.PACKAGE_NAME).removeHandler(handler);
  }

  @Test
  public void logInfo() {
    LoggingFns.logInfo(MESSAGE);
    assertLog(MESSAGE, Level.INFO);
  }

  @Test
  public void logWarning() {
    LoggingFns.logWarning(MESSAGE);
    assertLog(MESSAGE, Level.WARNING);
  }

  @Test
  public void logSevere() {
    LoggingFns.logSevere(MESSAGE);
    assertLog(MESSAGE, Level.SEVERE);
  }
}
