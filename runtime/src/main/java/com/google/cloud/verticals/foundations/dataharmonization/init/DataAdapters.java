// Copyright 2021 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.cloud.verticals.foundations.dataharmonization.init;


import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.serialization.impl.JsonSerializerDeserializer;

/** Utility class containing {@link Data} adapter methods for {@link Engine#transform(Data)}. */
public final class DataAdapters {

  private DataAdapters() {}

  public static Data fromJSONString(String input) {
    return JsonSerializerDeserializer.jsonToData(input);
  }

  public static Data fromByteArr(byte[] bytes) {
    return JsonSerializerDeserializer.jsonToData(bytes);
  }

  public static byte[] toByteArr(Data data) {
    return JsonSerializerDeserializer.dataToJson(data);
  }

  public static String toJSONString(Data data) {
    return JsonSerializerDeserializer.dataToJsonString(data);
  }

  /**
   * Input adapter for {@link Engine#transform(Data)}.
   *
   * @param <InT> type of input data.
   */
  @FunctionalInterface
  interface InputAdapter<InT> {
    Data adapt(InT input);
  }

  /**
   * Output adapter for {@link Engine#transform(Data)}.
   *
   * @param <OutT> type of output data.
   */
  @FunctionalInterface
  public interface OutputAdapter<OutT> {
    OutT adapt(Data result);
  }
}
