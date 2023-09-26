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

package com.google.cloud.verticals.foundations.dataharmonization.data;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static org.junit.Assert.assertEquals;

import com.google.cloud.verticals.foundations.dataharmonization.data.impl.DataImplementationSemanticsTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests to exercise the equals() method on {@link NullData}. */
@RunWith(JUnit4.class)
public class NullDataTest {

  @Test
  public void equals_returnsTrue_twoNullData() {
    NullData nullDataOne = NullData.instance;
    assertEquals(nullDataOne, nullDataOne);
  }

  @Test
  public void equals_returnsTrue_nullDataEmptyContainer() {
    NullData nullData = NullData.instance;
    Container emptyContainer = testDTI().emptyContainer();

    assertEquals(nullData, emptyContainer);
    assertEquals(emptyContainer, nullData);
  }

  @Test
  public void equals_returnsTrue_nullDataEmptyArray() {
    NullData nullData = NullData.instance;
    Array emptyArray = testDTI().emptyArray();

    assertEquals(nullData, emptyArray);
    assertEquals(emptyArray, nullData);
  }

  @Test
  public void equals_returnsTrue_nullDataNullPrimitive() {
    NullData nullData = NullData.instance;
    Primitive nullPrimitive = testDTI().primitiveOf((Double) null);

    assertEquals(nullData, nullPrimitive);
    assertEquals(nullPrimitive, nullData);
  }

  @Test
  public void checkDataInvariants() {
    new DataImplementationSemanticsTest(() -> NullData.instance).testAll();
  }
}
