/*
 * Copyright 2022 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.data.impl;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.Dataset;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import java.util.function.Supplier;
import org.junit.Assert;

/**
 * Provides utility functions to check data API invariants, especially the implication relationship
 * between isX()/asX()/isClass()/asClass().
 */
public class DataImplementationSemanticsTest {
  private final Supplier<Data> dataSupplier;


  public DataImplementationSemanticsTest(Supplier<Data> dataSupplier) {
    this.dataSupplier = dataSupplier;
  }

  public void testAll() {
    checkArrayAPIInvariants(dataSupplier.get());
    checkContainerAPIInvariants(dataSupplier.get());
    checkPrimitiveAPIInvariants(dataSupplier.get());
    checkDatasetAPIInvariants(dataSupplier.get());
  }

  public static void checkArrayAPIInvariants(Data d) {
    if (d.isArray()) {
      Assert.assertNotNull(d.asArray());
      Assert.assertTrue(d.isClass(Array.class));
      Assert.assertNotNull(d.asClass(Array.class));
      Assert.assertSame(d.asClass(Array.class), d.asArray());
      Assert.assertSame(d.asArray(), d.asArray().asArray());
    } else {
      Assert.assertNull(d.asArray());
    }
  }

  public static void checkPrimitiveAPIInvariants(Data d) {
    if (d.isPrimitive()) {
      Assert.assertNotNull(d.asPrimitive());
      Assert.assertTrue(d.isClass(Primitive.class));
      Assert.assertNotNull(d.asClass(Primitive.class));
      Assert.assertSame(d.asClass(Primitive.class), d.asPrimitive());
      Assert.assertSame(d.asPrimitive(), d.asPrimitive().asPrimitive());
    } else {
      Assert.assertNull(d.asPrimitive());
      Assert.assertFalse(d.isClass(Primitive.class));
    }
  }

  public static void checkContainerAPIInvariants(Data d) {
    if (d.isContainer()) {
      Assert.assertNotNull(d.asContainer());
      Assert.assertTrue(d.isClass(Container.class));
      Assert.assertNotNull(d.asClass(Container.class));
      Assert.assertSame(d.asClass(Container.class), d.asContainer());
      Assert.assertSame(d.asContainer(), d.asContainer().asContainer());
    } else {
      Assert.assertNull(d.asContainer());
      Assert.assertFalse(d.isClass(Container.class));
    }
  }

  public static void checkDatasetAPIInvariants(Data d) {
    if (d.isDataset()) {
      Assert.assertNotNull(d.asDataset());
      Assert.assertTrue(d.isClass(Dataset.class));
      Assert.assertNotNull(d.asClass(Dataset.class));
      Assert.assertSame(d.asClass(Dataset.class), d.asDataset());
      Assert.assertSame(d.asDataset(), d.asDataset().asDataset());
    } else {
      Assert.assertNull(d.asDataset());
    }
  }

}
