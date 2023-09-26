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

import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ContainerJsonSerializer provides a {@link com.google.gson.JsonSerializer} TypeAdapter allowing
 * Gson to convert a {@link Container} to a JsonObject and ultimately to a byte array.
 */
public class ContainerJsonSerializer implements JsonSerializer<Container> {

  public static JsonObject processContainer(Container src) {
    JsonObject container = new JsonObject();
    List<String> fieldList = src.fields().stream().sorted().collect(Collectors.toList());
    for (String k : fieldList) {
      container.add(k, JsonSerializerDeserializer.gson.toJsonTree(src.getField(k), Data.class));
    }
    return container;
  }

  @Override
  public JsonElement serialize(Container src, Type typeOfSrc, JsonSerializationContext context) {
    return processContainer(src);
  }
}
