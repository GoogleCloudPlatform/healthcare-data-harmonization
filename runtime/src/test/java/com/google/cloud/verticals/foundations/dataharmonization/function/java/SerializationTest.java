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
package com.google.cloud.verticals.foundations.dataharmonization.function.java;

import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.mockWithClass;
import static org.junit.Assert.assertEquals;

import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SerializationTest {

  static class Fns implements Serializable {
    public Data id(Data x) {
      return x;
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T serializeAndDeserialize(T beforeSerialized)
      throws IOException, ClassNotFoundException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(out);
    oos.writeObject(beforeSerialized);
    oos.close();

    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(out.toByteArray()));
    T object = (T) ois.readObject();
    ois.close();

    return object;
  }

  @Test
  public void serialize_methodHandleJavaFunction_identity() throws Exception {
    MethodHandleJavaFunction beforeSerialized =
        new MethodHandleJavaFunction(Fns.class.getMethod("id", Data.class), new Fns());

    MethodHandleJavaFunction fn = serializeAndDeserialize(beforeSerialized);

    Container container = mockWithClass(Container.class);
    Data got = fn.call(new TestContext(), container);
    assertEquals(got, container);
  }

  @Test
  public void serialize_javaFunction_identity() throws Exception {
    JavaFunction beforeSerialized =
        new JavaFunction(Fns.class.getMethod("id", Data.class), new Fns());
    JavaFunction fn = serializeAndDeserialize(beforeSerialized);

    Container container = mockWithClass(Container.class);
    Data got = fn.call(new TestContext(), container);
    assertEquals(got, container);
  }
}
