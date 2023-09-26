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

package com.google.cloud.verticals.foundations.dataharmonization.builtins;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static org.junit.Assert.assertEquals;

import com.google.cloud.verticals.foundations.dataharmonization.builtins.random.Random;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.TestContext;
import com.google.cloud.verticals.foundations.dataharmonization.mock.random.DeterministicUUIDGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test of {@link Random}. */
@RunWith(JUnit4.class)
public class RandomTest {

  @Test
  public void uuid_debug_mode_deterministic() {
    Random random = new Random(new DeterministicUUIDGenerator(100));
    assertEquals(
        testDTI().primitiveOf("ffffffff-b8d5-9fd6-ffff-ffffbc12dbb4"),
        random.uuid(new TestContext()));
    assertEquals(
        testDTI().primitiveOf("00000000-31e9-f345-ffff-ffffb73ee369"),
        random.uuid(new TestContext()));
    assertEquals(
        testDTI().primitiveOf("ffffffff-aaca-f867-0000-0000070c5774"),
        random.uuid(new TestContext()));
  }
}
