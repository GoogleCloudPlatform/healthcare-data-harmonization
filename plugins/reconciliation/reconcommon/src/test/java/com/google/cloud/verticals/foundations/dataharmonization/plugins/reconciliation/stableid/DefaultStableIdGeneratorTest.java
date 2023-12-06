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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid;

import static org.junit.Assert.assertNotEquals;

import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import com.google.cloud.verticals.foundations.dataharmonization.init.initializer.TestConfigExtractor;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline;
import com.google.protobuf.TextFormat;
import java.io.IOException;
import java.io.InputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for DefaultStableIdGenerator. */
@RunWith(JUnit4.class)
public class DefaultStableIdGeneratorTest {

  private Engine engine;

  @Before
  public void setup() throws IOException {
    InputStream textprotoStream =
        DefaultStableIdGeneratorTest.class.getResourceAsStream("/target_sample.textproto");
    Pipeline.PipelineConfig pConfig =
        TextFormat.parse(new String(textprotoStream.readAllBytes()), Pipeline.PipelineConfig.class);
    engine = new Engine.Builder(TestConfigExtractor.of(pConfig)).initialize().build();
  }

  @After
  public void teardown() {
    engine.close();
  }

  @Test
  public void getStableId_usesRandomIds() {
    DefaultStableIdGenerator g1 = new DefaultStableIdGenerator();
    DefaultStableIdGenerator g2 = new DefaultStableIdGenerator();

    assertNotEquals(g1.newStableId(), g2.newStableId());
  }
}
