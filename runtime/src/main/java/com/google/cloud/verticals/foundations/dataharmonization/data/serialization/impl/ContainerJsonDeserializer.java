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
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.DefaultDataTypeImplementation;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.lang.reflect.Type;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ArrayJsonDeserializer provides a {@link com.google.gson.JsonSerializer} TypeAdapter allowing Gson
 * to convert a byte array to an {@link Container}.
 */
public class ContainerJsonDeserializer implements JsonDeserializer<Container> {
  @Override
  public Container deserialize(
      JsonElement element, Type typeOfT, JsonDeserializationContext context) {
    JsonObject container = element.getAsJsonObject();
    List<String> keys = container.keySet().stream().sorted().collect(Collectors.toList());
    // TODO(): Use DTI from RuntimeContext here.
    Container returnValue = DefaultDataTypeImplementation.instance.emptyContainer();
    for (String key : keys) {
      JsonElement fieldElement = container.get(key);
      returnValue =
          returnValue.setField(
              key,
              NullData.wrapNull(
                  JsonSerializerDeserializer.gson.fromJson(fieldElement, Data.class)));
    }
    return returnValue;
  }
}
