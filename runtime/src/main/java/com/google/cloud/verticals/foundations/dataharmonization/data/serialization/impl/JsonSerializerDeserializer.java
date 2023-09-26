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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.data.serialization.Deserializer;
import com.google.cloud.verticals.foundations.dataharmonization.data.serialization.Serializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

/**
 * JsonSerializerDeserializer implements custom serialization and deserialization between {@link
 * Data} object and a Json formatted byte array.
 */
public class JsonSerializerDeserializer implements Serializer<Data>, Deserializer {

  static Gson gson;
  static Gson prettyGson;

  private static final JsonSerializerDeserializer jsonSerializerDeserializer;

  static {
    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.registerTypeAdapter(Primitive.class, new PrimitiveJsonSerializer());
    gsonBuilder.registerTypeAdapter(Container.class, new ContainerJsonSerializer());
    gsonBuilder.registerTypeAdapter(Array.class, new ArrayJsonSerializer());
    gsonBuilder.registerTypeAdapter(Data.class, new DataJsonSerializer());
    gsonBuilder.registerTypeAdapter(Primitive.class, new PrimitiveJsonDeserializer());
    gsonBuilder.registerTypeAdapter(Container.class, new ContainerJsonDeserializer());
    gsonBuilder.registerTypeAdapter(Array.class, new ArrayJsonDeserializer());
    gsonBuilder.registerTypeAdapter(Data.class, new DataJsonDeserializer());
    gsonBuilder.disableHtmlEscaping();
    gson = gsonBuilder.create();
    prettyGson = gsonBuilder.setPrettyPrinting().create();

    jsonSerializerDeserializer = new JsonSerializerDeserializer();
  }

  /**
   * Recursively converts a {@link Data} object to a byte array using Gson and custom {@link
   * com.google.gson.JsonSerializer} TypeAdapter for {@link Primitive}, {@link Container}, and
   * {@link Array}.
   *
   * @param data {@link Data} object to convert to a byte array.
   * @return a byte array representing the incoming {@link Data}
   */
  @Override
  public byte[] serialize(Data data) {
    JsonElement jsonElement = gson.toJsonTree(data, Data.class);
    return gson.toJson(jsonElement).getBytes(UTF_8);
  }

  /** Returns JSON string representation of given data. */
  @Override
  public String serializeString(Data data) {
    JsonElement jsonElement = gson.toJsonTree(data, Data.class);
    return gson.toJson(jsonElement);
  }

  /**
   * Deserializes a byte array into a {@link Data} object using Gson and custom {@link
   * com.google.gson.JsonDeserializer} TypeAdapter classes supporting {@link Primitive}, {@link
   * Container}, and {@link Array}
   *
   * @param serializedJson the array of bytes to convert to a {@link Data} object
   * @return {@link Data} object represented by the incoming array of bytes.
   */
  @Override
  public Data deserialize(byte[] serializedJson) {
    return NullData.wrapNull(gson.fromJson(new String(serializedJson, UTF_8), Data.class));
  }

  /** Deserializes a json string into Data. */
  @Override
  public Data deserialize(String serializedJson) {
    return NullData.wrapNull(gson.fromJson(serializedJson, Data.class));
  }

  /**
   * Utility method to deserialize json into structured Data
   *
   * @param serializedJson json to be deserialized
   * @return deserialized data
   */
  public static Data jsonToData(byte[] serializedJson) {
    return jsonSerializerDeserializer.deserialize(serializedJson);
  }

  /**
   * Utility method to deserialize json into structured Data.
   *
   * @param serializedJson json to be deserialized
   * @return deserialized data
   */
  public static Data jsonToData(String serializedJson) {
    return jsonSerializerDeserializer.deserialize(serializedJson);
  }

  /**
   * Utility method to convert deserialized JsonElement object into deserialized Data object.
   *
   * @param json a json object which has already been deserialized into a Java object.
   * @return a json object which is deserialized to Data.
   */
  public static Data jsonObjToData(JsonElement json) {
    return NullData.wrapNull(gson.fromJson(json, Data.class));
  }

  /**
   * Utility method to serialize Data into byte array.
   *
   * @param data Data to be serialized
   * @return serialized byte array
   */
  public static byte[] dataToJson(Data data) {
    return jsonSerializerDeserializer.serialize(data);
  }

  /**
   * Utility method to serialize Data into string.
   *
   * @param data Data to be serialized
   * @return serialized byte array
   */
  public static String dataToJsonString(Data data) {
    return jsonSerializerDeserializer.serializeString(data);
  }

  public static String dataToPrettyJson(Data data) {
    return prettyGson.toJson(data, Data.class);
  }

  /**
   * Converts {@link Data} into equivalent {@link JsonElement}, using the custom type adapters used
   * to serialize Data registered in this class
   *
   * @param data Data to be converted
   * @return converted JsonElement object
   */
  public static JsonElement dataToJsonElement(Data data) {
    return gson.toJsonTree(data, Data.class);
  }
}
