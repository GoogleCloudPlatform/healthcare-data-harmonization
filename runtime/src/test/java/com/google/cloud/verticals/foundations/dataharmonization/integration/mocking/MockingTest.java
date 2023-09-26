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

package com.google.cloud.verticals.foundations.dataharmonization.integration.mocking;

import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import com.google.cloud.verticals.foundations.dataharmonization.integration.IntegrationTest;
import java.io.IOException;

/** Base class to provide functionality for mocking integration tests. */
public class MockingTest extends IntegrationTest {
  public static final String TEST_DIR = "mocking/";
  private static final String MAIN_SUFFIX = ".wstl";
  private static final String MOCK_SUFFIX = ".mockconfig.wstl";

  public MockingTest(String subdir) {
    super(TEST_DIR + subdir);
  }

  public MockingTest() {
    super(TEST_DIR);
  }

  /**
   * Initialize an {@link Engine} with resources/test/mocking/{@code name}.wstl as the main config
   * file and resources/test/mocking/{@code name}.mockconfig.wstl as the mock config file.
   */
  protected Engine initTestFileWithMock(String name) throws IOException {
    return initBuilderWithMock(name).initialize().build();
  }

  /**
   * Initialize an {@link Engine.Builder} with resources/test/mocking/{@code name}.wstl as the main
   * config file and resources/test/mocking/{@code name}.mockconfig.wstl as the mock config file.
   * Useful when there are additional mock config files to load.
   */
  protected Engine.Builder initBuilderWithMock(String name) throws IOException {
    String mainPath = getTestDir() + name + MAIN_SUFFIX;
    String mockPath = getTestDir() + name + MOCK_SUFFIX;
    return testBase.initializeBuilder(mainPath, mockPath);
  }
}
