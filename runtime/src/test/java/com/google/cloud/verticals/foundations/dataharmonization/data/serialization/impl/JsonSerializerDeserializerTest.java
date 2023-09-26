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

package com.google.cloud.verticals.foundations.dataharmonization.data.serialization.impl;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.arrayOf;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.mutableContainerOf;
import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.Dataset;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.data.serialization.WithCustomSerialization;
import com.google.cloud.verticals.foundations.dataharmonization.data.wrappers.WrapperData;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Answers;

/** Tests for Json Serialization/Deserialization. */
@RunWith(JUnit4.class)
public class JsonSerializerDeserializerTest {
  @Test
  public void serialize_primitiveStr() {
    JsonSerializerDeserializer jsonSerializerDeserializer = new JsonSerializerDeserializer();
    Primitive stringPrimitive = testDTI().primitiveOf("Test \\String\\\n Value 1");
    String expected = "\"Test \\\\String\\\\\\n Value 1\"";
    byte[] actual = jsonSerializerDeserializer.serialize(stringPrimitive);
    byte[] actualStatic = JsonSerializerDeserializer.dataToJson(stringPrimitive);
    String actualStr = jsonSerializerDeserializer.serializeString(stringPrimitive);

    assertEquals(expected, new String(actual, UTF_8));
    assertEquals(expected, new String(actualStatic, UTF_8));
    assertEquals(expected, actualStr);
  }

  @Test
  public void serialize_primitiveStr_noHtmlEscapes() {
    JsonSerializerDeserializer jsonSerializerDeserializer = new JsonSerializerDeserializer();
    Primitive stringPrimitive = testDTI().primitiveOf("<body></body>");
    String expected = "\"<body></body>\"";
    byte[] actual = jsonSerializerDeserializer.serialize(stringPrimitive);
    byte[] actualStatic = JsonSerializerDeserializer.dataToJson(stringPrimitive);
    String actualStr = jsonSerializerDeserializer.serializeString(stringPrimitive);

    assertEquals(expected, new String(actual, UTF_8));
    assertEquals(expected, new String(actualStatic, UTF_8));
    assertEquals(expected, actualStr);
  }

  @Test
  public void serialize_primitiveNum() {
    JsonSerializerDeserializer jsonSerializerDeserializer = new JsonSerializerDeserializer();
    Primitive numValue = testDTI().primitiveOf(5.0);
    byte[] actual = jsonSerializerDeserializer.serialize(numValue);
    String actualStr = jsonSerializerDeserializer.serializeString(numValue);

    assertEquals("5", new String(actual, UTF_8));
    assertEquals("5", actualStr);
  }

  @Test
  public void serialize_primitiveBool() {
    JsonSerializerDeserializer jsonSerializerDeserializer = new JsonSerializerDeserializer();
    Primitive boolValue = testDTI().primitiveOf(false);
    byte[] actual = jsonSerializerDeserializer.serialize(boolValue);
    String actualStr = jsonSerializerDeserializer.serializeString(boolValue);

    assertEquals("false", new String(actual, UTF_8));
    assertEquals("false", actualStr);
  }

  @Test
  public void serialize_primitiveEmptyStr() {
    JsonSerializerDeserializer jsonSerializerDeserializer = new JsonSerializerDeserializer();
    Primitive strValue = testDTI().primitiveOf("");
    byte[] actual = jsonSerializerDeserializer.serialize(strValue);
    String actualStr = jsonSerializerDeserializer.serializeString(strValue);

    assertEquals("\"\"", new String(actual, UTF_8));
    assertEquals("\"\"", actualStr);
  }

  @Test
  public void serialize_dataset() {
    JsonSerializerDeserializer jsonSerializerDeserializer = new JsonSerializerDeserializer();
    Dataset ds = mock(Dataset.class, Answers.CALLS_REAL_METHODS);
    byte[] actual = jsonSerializerDeserializer.serialize(ds);
    String actualStr = jsonSerializerDeserializer.serializeString(ds);

    assertThat(new String(actual, UTF_8)).contains("this value was a Dataset");
    assertThat(actualStr).contains("this value was a Dataset");
  }

  @Test
  public void serialize_defaultContainer() {
    JsonSerializerDeserializer jsonSerializerDeserializer = new JsonSerializerDeserializer();

    Map<String, Data> embeddedContainer = new HashMap<>();
    embeddedContainer.put("embeddedTest", testDTI().primitiveOf(5.0));
    embeddedContainer.put("embeddedTest2", testDTI().primitiveOf("embeddedString"));
    embeddedContainer.put("embeddedTest3", testDTI().primitiveOf(true));
    Map<String, Data> container = new HashMap<>();
    container.put("test", testDTI().primitiveOf(5.0));
    container.put("embeddedContainer", testDTI().containerOf(embeddedContainer));
    List<Data> containerList = new ArrayList<>();
    containerList.add(testDTI().primitiveOf("testValue1"));
    containerList.add(testDTI().primitiveOf(false));
    containerList.add(testDTI().primitiveOf(5.0));
    container.put("embeddedList", testDTI().arrayOf(containerList));

    Container containerValue = testDTI().containerOf(container);
    byte[] actual = jsonSerializerDeserializer.serialize(containerValue);
    byte[] actualStatic = JsonSerializerDeserializer.dataToJson(containerValue);
    String actualStr = jsonSerializerDeserializer.serializeString(containerValue);

    System.out.println("Serialized Container: " + new String(actual, UTF_8));
    String expected =
        "{\"embeddedContainer\":{\"embeddedTest\":5,\"embeddedTest2\":\"embeddedString\",\"embeddedTest3\":true},\"embeddedList\":[\"testValue1\",false,5],\"test\":5}";
    assertEquals(expected, new String(actual, UTF_8));
    assertEquals(expected, new String(actualStatic, UTF_8));
    assertEquals(expected, actualStr);
  }

  @Test
  public void serialize_defaultContainerEmpty() {
    JsonSerializerDeserializer jsonSerializerDeserializer = new JsonSerializerDeserializer();
    Map<String, Data> container = new HashMap<>();

    Container containerValue = testDTI().containerOf(container);
    String expected = "{}";
    byte[] actual = jsonSerializerDeserializer.serialize(containerValue);
    byte[] actualStatic = JsonSerializerDeserializer.dataToJson(containerValue);
    String actualStr = jsonSerializerDeserializer.serializeString(containerValue);

    assertEquals(expected, new String(actual, UTF_8));
    assertEquals(expected, new String(actualStatic, UTF_8));
    assertEquals(expected, actualStr);
  }

  @Test
  public void serialize_defaultArray() {
    JsonSerializerDeserializer jsonSerializerDeserializer = new JsonSerializerDeserializer();
    Array arrayValue = testDTI().emptyArray();
    Map<String, Data> embeddedContainer = new HashMap<>();
    embeddedContainer.put("embeddedTest", testDTI().primitiveOf(5.0));
    embeddedContainer.put("embeddedTest2", testDTI().primitiveOf("embeddedString"));
    embeddedContainer.put("embeddedTest3", testDTI().primitiveOf(true));
    arrayValue =
        arrayValue
            .setElement(0, testDTI().primitiveOf(5.0))
            .setElement(1, testDTI().containerOf(embeddedContainer));
    List<Data> embeddedArray = new ArrayList<>();
    embeddedArray.add(testDTI().primitiveOf("testValue1"));
    embeddedArray.add(testDTI().primitiveOf(false));
    embeddedArray.add(testDTI().primitiveOf(5.0));
    arrayValue = arrayValue.setElement(2, testDTI().arrayOf(embeddedArray));
    byte[] actual = jsonSerializerDeserializer.serialize(arrayValue);
    byte[] actualStatic = JsonSerializerDeserializer.dataToJson(arrayValue);
    String actualStr = jsonSerializerDeserializer.serializeString(arrayValue);

    String expected =
        "[5,{\"embeddedTest\":5,\"embeddedTest2\":\"embeddedString\",\"embeddedTest3\":true},[\"testValue1\",false,5]]";
    assertEquals(expected, new String(actual, UTF_8));
    assertEquals(expected, new String(actualStatic, UTF_8));
    assertEquals(expected, actualStr);
  }

  @Test
  public void serialize_defaultArrayEmpty() {
    JsonSerializerDeserializer jsonSerializerDeserializer = new JsonSerializerDeserializer();
    Array arrayValue = testDTI().emptyArray();
    String expected = "[]";
    byte[] actual = jsonSerializerDeserializer.serialize(arrayValue);
    byte[] actualStatic = JsonSerializerDeserializer.dataToJson(arrayValue);
    String actualStr = jsonSerializerDeserializer.serializeString(arrayValue);

    assertEquals(expected, new String(actual, UTF_8));
    assertEquals(expected, new String(actualStatic, UTF_8));
    assertEquals(expected, actualStr);
  }

  @Test
  public void dataToPrettyJson() {
    Data data =
        arrayOf(
            mutableContainerOf(
                s -> {
                  s.set("c", testDTI().primitiveOf("c"));
                  s.set("b", testDTI().primitiveOf("b"));
                  s.set("a", testDTI().primitiveOf("a"));
                }),
            mutableContainerOf(
                s -> {
                  s.set("c", testDTI().primitiveOf(3.));
                  s.set("b", testDTI().primitiveOf(2.));
                  s.set("a", testDTI().primitiveOf(true));
                }));
    String actual = JsonSerializerDeserializer.dataToPrettyJson(data);
    String expected =
        "[\n"
            + "  {\n"
            + "    \"a\": \"a\",\n"
            + "    \"b\": \"b\",\n"
            + "    \"c\": \"c\"\n"
            + "  },\n"
            + "  {\n"
            + "    \"a\": true,\n"
            + "    \"b\": 2,\n"
            + "    \"c\": 3\n"
            + "  }\n"
            + "]";
    assertEquals(expected, actual);
  }

  @Test
  public void deserialize_primitiveStr() {
    JsonSerializerDeserializer jsonDeserializer = new JsonSerializerDeserializer();
    String strValue = "\"Test \\\\String\\\\\\n Value 1\"";
    Primitive expected = testDTI().primitiveOf("Test \\String\\\n Value 1");
    Data actual = jsonDeserializer.deserialize(strValue.getBytes(UTF_8));
    Data actualStatic = JsonSerializerDeserializer.jsonToData(strValue.getBytes(UTF_8));
    Data actualStr = jsonDeserializer.deserialize(strValue);
    assertEquals(expected, actual);
    assertEquals(expected, actualStatic);
    assertEquals(expected, actualStr);
  }

  @Test
  public void deserialize_primitiveNum() {
    JsonSerializerDeserializer jsonDeserializer = new JsonSerializerDeserializer();
    String numValue = "5.0";
    Data expected = testDTI().primitiveOf(5.0);
    Data actual = jsonDeserializer.deserialize(numValue.getBytes(UTF_8));
    Data actualStatic = JsonSerializerDeserializer.jsonToData(numValue.getBytes(UTF_8));
    Data actualStr = jsonDeserializer.deserialize(numValue);

    assertEquals(expected, actual);
    assertEquals(expected, actualStatic);
    assertEquals(expected, actualStr);
  }

  @Test
  public void deserialize_primitiveBool() {
    JsonSerializerDeserializer jsonDeserializer = new JsonSerializerDeserializer();
    String boolValue = "false";
    Data expected = testDTI().primitiveOf(false);
    Data actual = jsonDeserializer.deserialize(boolValue.getBytes(UTF_8));
    Data actualStatic = JsonSerializerDeserializer.jsonToData(boolValue.getBytes(UTF_8));
    Data actualStr = jsonDeserializer.deserialize(boolValue);

    assertEquals(expected, actual);
    assertEquals(expected, actualStatic);
    assertEquals(expected, actualStr);
  }

  @Test
  public void deserialize_primitiveEmptyStr() {
    JsonSerializerDeserializer jsonDeserializer = new JsonSerializerDeserializer();
    String emptyString = "\"\"";
    Data expected = testDTI().primitiveOf("");
    Data actual = jsonDeserializer.deserialize(emptyString.getBytes(UTF_8));
    Data actualStatic = JsonSerializerDeserializer.jsonToData(emptyString.getBytes(UTF_8));
    Data actualStr = jsonDeserializer.deserialize(emptyString);

    assertEquals(expected, actual);
    assertEquals(expected, actualStatic);
    assertEquals(expected, actualStr);
  }

  @Test
  public void deserialize_defaultContainer() {
    JsonSerializerDeserializer jsonDeserializer = new JsonSerializerDeserializer();
    String containerString =
        "{\"embeddedContainer\":{\"embeddedTest\":5.0,\"embeddedTest2\":\"embeddedString\",\"embeddedTest3\":true},\"embeddedList\":[\"testValue1\",false,5.0],\"test\":5.0}";

    Map<String, Data> embeddedContainer = new HashMap<>();
    embeddedContainer.put("embeddedTest", testDTI().primitiveOf(5.0));
    embeddedContainer.put("embeddedTest2", testDTI().primitiveOf("embeddedString"));
    embeddedContainer.put("embeddedTest3", testDTI().primitiveOf(true));
    Map<String, Data> container = new HashMap<>();
    container.put("test", testDTI().primitiveOf(5.0));
    container.put("embeddedContainer", testDTI().containerOf(embeddedContainer));
    List<Data> containerList = new ArrayList<>();
    containerList.add(testDTI().primitiveOf("testValue1"));
    containerList.add(testDTI().primitiveOf(false));
    containerList.add(testDTI().primitiveOf(5.0));
    container.put("embeddedList", testDTI().arrayOf(containerList));

    Data expected = testDTI().containerOf(container);
    Data actual = jsonDeserializer.deserialize(containerString.getBytes(UTF_8));
    Data actualStaic = JsonSerializerDeserializer.jsonToData(containerString.getBytes(UTF_8));
    Data actualStr = jsonDeserializer.deserialize(containerString);

    assertEquals(expected, actual);
    assertEquals(expected, actualStaic);
    assertEquals(expected, actualStr);
  }

  @Test
  public void deserialize_defaultContainerEmpty() {
    JsonSerializerDeserializer jsonDeserializer = new JsonSerializerDeserializer();
    String containerString = "{}";

    Map<String, Data> container = new HashMap<>();

    Data expected = testDTI().containerOf(container);
    Data actual = jsonDeserializer.deserialize(containerString.getBytes(UTF_8));
    Data actualStatic = JsonSerializerDeserializer.jsonToData(containerString.getBytes(UTF_8));
    Data actualStr = jsonDeserializer.deserialize(containerString);

    assertEquals(expected, actual);
    assertEquals(expected, actualStatic);
    assertEquals(expected, actualStr);
  }

  @Test
  public void deserialize_null() {
    JsonSerializerDeserializer jsonDeserializer = new JsonSerializerDeserializer();
    String json = "null";

    Data expected = NullData.instance;
    Data actual = jsonDeserializer.deserialize(json.getBytes(UTF_8));
    Data actualStatic = JsonSerializerDeserializer.jsonToData(json.getBytes(UTF_8));
    Data actualStr = jsonDeserializer.deserialize(json);

    assertEquals(expected, actual);
    assertEquals(expected, actualStatic);
    assertEquals(expected, actualStr);
  }

  @Test
  public void deserialize_containerWithNull() {
    JsonSerializerDeserializer jsonDeserializer = new JsonSerializerDeserializer();
    String containerString = "{\"a\": null}";

    Map<String, Data> container = new HashMap<>();
    container.put("a", NullData.instance);

    Data expected = testDTI().containerOf(container);
    Data actual = jsonDeserializer.deserialize(containerString.getBytes(UTF_8));
    Data actualStatic = JsonSerializerDeserializer.jsonToData(containerString.getBytes(UTF_8));
    Data actualStr = jsonDeserializer.deserialize(containerString);

    assertEquals(expected, actual);
    assertEquals(expected, actualStatic);
    assertEquals(expected, actualStr);
  }

  @Test
  public void deserialize_arrayWithNull() {
    JsonSerializerDeserializer jsonDeserializer = new JsonSerializerDeserializer();
    String containerString = "[null]";

    List<Data> list = ImmutableList.of(NullData.instance);

    Data expected = testDTI().arrayOf(list);
    Data actual = jsonDeserializer.deserialize(containerString.getBytes(UTF_8));
    Data actualStatic = JsonSerializerDeserializer.jsonToData(containerString.getBytes(UTF_8));
    Data actualStr = jsonDeserializer.deserialize(containerString);

    assertEquals(expected, actual);
    assertEquals(expected, actualStatic);
    assertEquals(expected, actualStr);
  }

  @Test
  public void deserialize_defaultArray() {
    JsonSerializerDeserializer jsonDeserializer = new JsonSerializerDeserializer();
    String arrayString =
        "[5.0,{\"embeddedTest\":5.0,\"embeddedTest2\":\"embeddedString\",\"embeddedTest3\":true},[\"testValue1\",false,5.0]]";

    Array expected = testDTI().emptyArray();
    Map<String, Data> embeddedContainer = new HashMap<>();
    embeddedContainer.put("embeddedTest", testDTI().primitiveOf(5.0));
    embeddedContainer.put("embeddedTest2", testDTI().primitiveOf("embeddedString"));
    embeddedContainer.put("embeddedTest3", testDTI().primitiveOf(true));
    expected =
        expected
            .setElement(0, testDTI().primitiveOf(5.0))
            .setElement(1, testDTI().containerOf(embeddedContainer));
    List<Data> containerList = new ArrayList<>();
    containerList.add(testDTI().primitiveOf("testValue1"));
    containerList.add(testDTI().primitiveOf(false));
    containerList.add(testDTI().primitiveOf(5.0));
    expected = expected.setElement(2, testDTI().arrayOf(containerList));

    Data actual = jsonDeserializer.deserialize(arrayString.getBytes(UTF_8));
    Data actualStatic = JsonSerializerDeserializer.jsonToData(arrayString.getBytes(UTF_8));
    Data actualStr = jsonDeserializer.deserialize(arrayString);

    assertEquals(expected, actual);
    assertEquals(expected, actualStatic);
    assertEquals(expected, actualStr);
  }

  @Test
  public void deserialize_defaultArrayEmpty() {
    JsonSerializerDeserializer jsonDeserializer = new JsonSerializerDeserializer();
    String arrayString = "[]";
    Data expected = testDTI().emptyArray();
    Data actual = jsonDeserializer.deserialize(arrayString.getBytes(UTF_8));
    Data actualStatic = JsonSerializerDeserializer.jsonToData(arrayString.getBytes(UTF_8));
    Data actualStr = jsonDeserializer.deserialize(arrayString);

    assertEquals(expected, actual);
    assertEquals(expected, actualStatic);
    assertEquals(expected, actualStr);
  }

  @Test
  public void dataToJsonElement_primitiveStr() {
    Data data = testDTI().primitiveOf("string");
    JsonElement result = JsonSerializerDeserializer.dataToJsonElement(data);

    assertThat(result.isJsonPrimitive()).isTrue();
    assertThat(result.getAsJsonPrimitive().isString()).isTrue();
  }

  @Test
  public void dataToJsonElement_primitiveBool() {
    Data data = testDTI().primitiveOf(true);
    JsonElement result = JsonSerializerDeserializer.dataToJsonElement(data);

    assertThat(result.isJsonPrimitive()).isTrue();
    assertThat(result.getAsJsonPrimitive().isBoolean()).isTrue();
  }

  @Test
  public void dataToJsonElement_primitiveLong() {
    Data data = testDTI().primitiveOf(3.0);
    JsonElement result = JsonSerializerDeserializer.dataToJsonElement(data);

    assertThat(result.isJsonPrimitive()).isTrue();
    assertThat(result.getAsJsonPrimitive().isNumber()).isTrue();
    assertThat(result.getAsJsonPrimitive().getAsLong()).isEqualTo(3);
  }

  @Test
  public void dataToJsonElement_primitiveDouble() {
    Data data = testDTI().primitiveOf(3.635);
    JsonElement result = JsonSerializerDeserializer.dataToJsonElement(data);

    assertThat(result.isJsonPrimitive()).isTrue();
    assertThat(result.getAsJsonPrimitive().isNumber()).isTrue();
    assertThat(result.getAsJsonPrimitive().getAsDouble()).isEqualTo(3.635);
  }

  @Test
  public void dataToJsonElement_container() {
    Data data =
        testDTI()
            .containerOf(
                ImmutableMap.of(
                    "foo", testDTI().primitiveOf("bar"),
                    "foo1", testDTI().primitiveOf(12.05)));
    JsonElement result = JsonSerializerDeserializer.dataToJsonElement(data);

    assertThat(result.isJsonObject()).isTrue();
    assertThat(result.getAsJsonObject().get("foo").getAsJsonPrimitive().isString()).isTrue();
    assertThat(result.getAsJsonObject().get("foo1").getAsJsonPrimitive().isNumber()).isTrue();
  }

  @Test
  public void dataToJsonElement_array() {
    Data data =
        testDTI().arrayOf(testDTI().primitiveOf(12.0), testDTI().primitiveOf("hello world"));
    JsonElement result = JsonSerializerDeserializer.dataToJsonElement(data);

    assertThat(result.isJsonArray()).isTrue();
    assertThat(result.getAsJsonArray().get(0).getAsJsonPrimitive().isNumber()).isTrue();
    assertThat(result.getAsJsonArray().get(1).getAsJsonPrimitive().isString()).isTrue();
  }

  @Test
  public void dataToJsonElement_containerWithNullField() {
    Data data =
        testDTI()
            .containerOf(
                ImmutableMap.of(
                    "foo", testDTI().primitiveOf("bar"), "nullField", NullData.instance));
    JsonElement result = JsonSerializerDeserializer.dataToJsonElement(data);

    assertThat(result.getAsJsonObject().get("foo").getAsJsonPrimitive().isString()).isTrue();
    assertThat(result.getAsJsonObject().get("nullField")).isNull();
  }

  @Test
  public void dataToJson_withCustomSerializationResult_returnIt() {
    Data customSerializationResult = testDTI().primitiveOf("testDataValue");
    Data input = DataWithCustomSerialization.getInstance(customSerializationResult);
    assertEquals(
        "\"testDataValue\"", new String(JsonSerializerDeserializer.dataToJson(input), UTF_8));
  }

  @Test
  public void dataToJson_regularDataIncludesDataWithCustomSerialization_returnJsonContainingIt() {
    Data customSerializationResult = testDTI().primitiveOf("testDataValue");
    Data data = DataWithCustomSerialization.getInstance(customSerializationResult);
    Data input = testDTI().containerOf(ImmutableMap.of("field1", data));
    assertEquals(
        "{\"field1\":\"testDataValue\"}",
        new String(JsonSerializerDeserializer.dataToJson(input), UTF_8));
  }

  @Test
  public void dataToJson_wrappedNullData_returnNull() {
    Data input = new TestWrapperData(NullData.instance);
    assertEquals("null", new String(JsonSerializerDeserializer.dataToJson(input), UTF_8));
  }

  @Test
  public void dataToJson_wrappedDataWithCustomSerializationResult_returnIt() {
    Data customSerializationResult = testDTI().primitiveOf(1.0);
    Data input =
        new TestWrapperData(DataWithCustomSerialization.getInstance(customSerializationResult));
    assertEquals(customSerializationResult, ((WithCustomSerialization) input).getDataToSerialize());
    assertEquals("1", new String(JsonSerializerDeserializer.dataToJson(input), UTF_8));
  }

  @Test
  public void dataToJson_wrappedNormalData_returnSerializationResultOfNormalData() {
    Data data =
        testDTI()
            .containerOf(
                ImmutableMap.of(
                    "foo", testDTI().primitiveOf("bar"), "nullField", NullData.instance));
    Data input = new TestWrapperData(data);
    assertEquals(
        new String(JsonSerializerDeserializer.dataToJson(data), UTF_8),
        new String(JsonSerializerDeserializer.dataToJson(input), UTF_8));
  }

  private static class TestWrapperData extends WrapperData<TestWrapperData> {

    protected TestWrapperData(Data backing) {
      super(backing);
    }

    @Override
    protected TestWrapperData rewrap(Data backing) {
      return new TestWrapperData(backing);
    }
  }

  private interface DataWithCustomSerialization extends Data, WithCustomSerialization {

    static DataWithCustomSerialization getInstance(Data customSerializationResult) {
      DataWithCustomSerialization mockData = mock(DataWithCustomSerialization.class);
      when(mockData.getDataToSerialize()).thenReturn(customSerializationResult);
      return mockData;
    }
  }
}
