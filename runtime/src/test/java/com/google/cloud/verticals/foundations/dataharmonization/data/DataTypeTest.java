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

package com.google.cloud.verticals.foundations.dataharmonization.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.WrapperDataUtils.ExtendedTestWrapperData;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.WrapperDataUtils.IrrelevantWrapperData;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.WrapperDataUtils.TestWrapperData;
import com.google.cloud.verticals.foundations.dataharmonization.data.wrappers.WrapperData;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link Data} type assertion and casting. */
@RunWith(JUnit4.class)
public class DataTypeTest {

  private <T extends Data> T mockWithClass(Class<T> clazz) {
    T mock = mock(clazz);
    when(mock.isClass(any())).thenAnswer(CALLS_REAL_METHODS);
    when(mock.asClass(any())).thenAnswer(CALLS_REAL_METHODS);
    when(mock.isContainer()).thenAnswer(CALLS_REAL_METHODS);
    when(mock.isArray()).thenAnswer(CALLS_REAL_METHODS);
    when(mock.isPrimitive()).thenAnswer(CALLS_REAL_METHODS);
    when(mock.isDataset()).thenAnswer(CALLS_REAL_METHODS);
    return mock;
  }

  @Test
  public void isClassData_instanceOfData_returnsTrue() {
    Data data = mockWithClass(Data.class);
    assertTrue(data.isClass(Data.class));
  }

  @Test
  public void isClass_instanceOfContainer() {
    Container container = mockWithClass(Container.class);
    assertTrue(container.isClass(Container.class));
    assertTrue(container.isClass(Data.class));
    assertFalse(container.isClass(Array.class));
    assertFalse(container.isClass(Primitive.class));
    assertFalse(container.isClass(Dataset.class));
  }

  @Test
  public void isClass_nullData_returnsTrueForDACP() {
    Data nullData = NullData.instance;
    assertTrue(nullData.isClass(Data.class));
    assertTrue(nullData.isClass(Container.class));
    assertTrue(nullData.isClass(Array.class));
    assertTrue(nullData.isClass(Primitive.class));
    assertTrue(nullData.isClass(Dataset.class));
  }

  @Test
  public void isClass_deepExtensionOfData_returnTrueForParentClassAndInterfaces() {
    Data dataImpl = mockWithClass(Data.class);
    Data extendedDataImpl = mockWithClass(dataImpl.getClass());
    assertTrue(extendedDataImpl.isClass(Data.class));
    assertTrue(extendedDataImpl.isClass(extendedDataImpl.getClass()));
    assertTrue(extendedDataImpl.isClass(dataImpl.getClass()));
  }

  @Test
  public void isClass_emptyContainerArrayIsNullData_returnsTrue() {
    assertTrue(TestDataTypeImplementation.testDTI().emptyContainer().isClass(NullData.class));
    assertTrue(TestDataTypeImplementation.testDTI().emptyArray().isClass(NullData.class));
  }

  @Test
  public void isClass_workAcrossWrapperDataBoundary() {
    Data extendedFakeData = new ExtendedFakeData();
    Data wrappedData = new IrrelevantWrapperData(new ExtendedTestWrapperData(extendedFakeData));
    assertTrue(wrappedData.isClass(IrrelevantWrapperData.class));
    assertTrue(wrappedData.isClass(ExtendedTestWrapperData.class));
    assertTrue(wrappedData.isClass(TestWrapperData.class));
    assertTrue(wrappedData.isClass(WrapperData.class));
    assertTrue(wrappedData.isClass(ExtendedFakeData.class));
    assertTrue(wrappedData.isClass(FakeData.class));
    assertTrue(wrappedData.isClass(Data.class));
  }

  @Test
  public void isClass_wrappedEmptyContainerArrayIsNullData_returnTrue() {
    Data wrappedEmptyContainer =
        new TestWrapperData(TestDataTypeImplementation.testDTI().emptyContainer());
    Data wrappedEmptyArray =
        new IrrelevantWrapperData(
            new TestWrapperData(TestDataTypeImplementation.testDTI().emptyArray()));
    assertTrue(wrappedEmptyContainer.isClass(NullData.class));
    assertTrue(wrappedEmptyArray.isClass(NullData.class));
  }

  @Test
  public void asClass_returnInstanceOfParentClass() {
    Data extendedDataImpl = new ExtendedFakeData();
    FakeData castData = extendedDataImpl.asClass(FakeData.class);
    assertEquals(castData, extendedDataImpl);
  }

  @Test
  public void asClass_isClassFalse_errors() {
    Data container = mockWithClass(Container.class);
    assertFalse(container.isClass(Array.class));
    assertThrows(ClassCastException.class, () -> container.asClass(Array.class));
  }

  @Test
  public void asClass_castEmptyContainerArrayToNullData() {
    Data emptyContainer = TestDataTypeImplementation.testDTI().emptyContainer();
    assertEquals(NullData.instance, emptyContainer.asClass(NullData.class));
    Data emptyArray = TestDataTypeImplementation.testDTI().emptyArray();
    assertEquals(NullData.instance, emptyArray.asClass(NullData.class));
  }

  @Test
  public void asClass_castWrapperDataToCompatibleType_returnBacking() {
    Data extendedFakeData = new ExtendedFakeData();
    Data wrappedData = new IrrelevantWrapperData(new ExtendedTestWrapperData(extendedFakeData));
    assertEquals(extendedFakeData, wrappedData.asClass(ExtendedFakeData.class));
    assertEquals(extendedFakeData, wrappedData.asClass(FakeData.class));
  }

  @Test
  public void asClass_castWrapperDataToIncompatibleType_error() {
    Data extendedFakeData = new ExtendedFakeData();
    Data wrappedData = new IrrelevantWrapperData(new ExtendedTestWrapperData(extendedFakeData));
    assertFalse(wrappedData.isClass(Array.class));
    assertThrows(IllegalArgumentException.class, () -> wrappedData.asClass(Array.class));
  }

  @Test
  public void asClass_castWrapperDataWithEmptyBackingToNullData_returnNullData() {
    Data wrappedEmptyContainer =
        new IrrelevantWrapperData(
            new ExtendedTestWrapperData(TestDataTypeImplementation.testDTI().emptyContainer()));
    Data wrappedEmptyArray =
        new IrrelevantWrapperData(
            new TestWrapperData(TestDataTypeImplementation.testDTI().emptyArray()));
    assertEquals(NullData.instance, wrappedEmptyContainer.asClass(NullData.class));
    assertEquals(NullData.instance, wrappedEmptyArray.asClass(NullData.class));
  }

  private static class FakeData implements Data {

    @Override
    public boolean isNullOrEmpty() {
      return false;
    }

    @Override
    public Data deepCopy() {
      return null;
    }

    @Override
    public boolean isWritable() {
      return false;
    }
  }

  private static class ExtendedFakeData extends FakeData {}
}
