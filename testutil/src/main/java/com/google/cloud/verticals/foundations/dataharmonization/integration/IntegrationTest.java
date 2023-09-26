/*
 * Copyright 2021 Google LLC.
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

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import java.io.IOException;

/**
 * Helper class for integration tests. Stores location of integrationt test cases and provides basic
 * utility methods to set up a test.
 */
public class IntegrationTest {

  public static final String TESTS_DIR = "/tests/";
  protected static IntegrationTestBase testBase = new IntegrationTestBase();
  private final String testDir;

  public IntegrationTest() {
    this("");
  }

  public IntegrationTest(String subDir) {
    this.testDir = TESTS_DIR + subDir;
  }

  public Engine initializeTestFile(String name) throws IOException {
    return testBase.initializeTestFile(testDir + name);
  }

  public Engine.Builder initializeBuilderWithTestFile(String name) throws IOException {
    return testBase.initializeBuilder(testDir + name);
  }

  /**
   * Reads the given file name as json and deserializes to Data.
   *
   * @param name file name relative to <code>resources/tests</code>.
   */
  public Data loadJson(String name) throws IOException {
    return testBase.loadJson(testDir + name);
  }

  public String getTestDir() {
    return testDir;
  }

  public String loadText(String name) throws IOException {
    return testBase.loadText(testDir + name);
  }
}
