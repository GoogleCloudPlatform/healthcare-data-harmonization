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
package com.google.cloud.verticals.foundations.dataharmonization.modifier.arg;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test for {@link RequiredArgMod}. */
@RunWith(JUnit4.class)
public class RequiredArgModTest {

  @Test
  public void testCanShortCircuit() {
    RequiredArgMod argMod = new RequiredArgMod();
    assertTrue(argMod.canShortCircuit(testDTI().primitiveOf("")));
    assertTrue(argMod.canShortCircuit(testDTI().emptyContainer()));
    assertTrue(argMod.canShortCircuit(testDTI().emptyArray()));
    assertFalse(argMod.canShortCircuit(testDTI().primitiveOf(1.)));
  }

  @Test
  public void getDefaultVal() {
    RequiredArgMod argMod = new RequiredArgMod();
    Assert.assertEquals(NullData.instance, argMod.getShortCircuitValue(testDTI().primitiveOf(1.)));
  }
}
