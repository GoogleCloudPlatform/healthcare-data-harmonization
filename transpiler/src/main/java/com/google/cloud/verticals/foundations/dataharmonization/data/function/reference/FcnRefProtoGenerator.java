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
package com.google.cloud.verticals.foundations.dataharmonization.data.function.reference;

import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall.FunctionReference;

/** Creates a FunctionReference proto from the registered package and function names. */
public interface FcnRefProtoGenerator {

  FunctionReference getFunctionReferenceProto();
}
