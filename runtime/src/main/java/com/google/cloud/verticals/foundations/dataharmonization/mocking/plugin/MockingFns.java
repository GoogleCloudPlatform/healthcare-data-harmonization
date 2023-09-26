/*
 * Copyright 2021 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.verticals.foundations.dataharmonization.mocking.plugin;

import com.google.cloud.verticals.foundations.dataharmonization.WhistleHelper;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.function.DefaultClosure.FunctionReference;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.PluginFunction;
import com.google.cloud.verticals.foundations.dataharmonization.mocking.wrappers.Mock;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleLexer;
import com.google.common.collect.ImmutableList;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Plugin functions used to register mock into the map in serializable meta. */
public final class MockingFns {

  @PluginFunction
  public static NullData mock(
      RuntimeContext context, String originalRefStr, String mockRefStr, String selectorRefStr) {
    Mock newMock = new Mock(parseFuncRefStr(mockRefStr), parseFuncRefStr(selectorRefStr));
    registerMock(context, originalRefStr, newMock, MockingPlugin.MOCK_META_KEY);
    return NullData.instance;
  }

  @PluginFunction
  public static NullData mock(RuntimeContext context, String originalRefStr, String mockRefStr) {
    registerMock(
        context,
        originalRefStr,
        new Mock(parseFuncRefStr(mockRefStr), null),
        MockingPlugin.MOCK_META_KEY);
    return NullData.instance;
  }

  @PluginFunction
  public static NullData mockTarget(
      RuntimeContext context, String originalRefStr, String mockRefStr, String selectorRefStr) {
    Mock newMock = new Mock(parseFuncRefStr(mockRefStr), parseFuncRefStr(selectorRefStr));
    registerMock(context, originalRefStr, newMock, MockingPlugin.MOCK_TARGET_META_KEY);
    return NullData.instance;
  }

  @PluginFunction
  public static NullData mockTarget(
      RuntimeContext context, String originRefStr, String mockRefStr) {
    registerMock(
        context,
        originRefStr,
        new Mock(parseFuncRefStr(mockRefStr), null),
        MockingPlugin.MOCK_TARGET_META_KEY);
    return NullData.instance;
  }

  private static FunctionReference parseFuncRefStr(String funcRef) {
    if (funcRef == null) {
      return null;
    }
    String pkgSeparator = WhistleHelper.getTokenLiteral(WhistleLexer.PKG_REF);
    String[] refs = funcRef.split(pkgSeparator);
    if (refs.length <= 1) {
      throw new IllegalArgumentException(
          String.format(
              "Failed to parse function reference %s. Please specify function reference in the form"
                  + " of packageName::functionName.",
              funcRef));
    }
    return new FunctionReference(
        String.join(pkgSeparator, Arrays.copyOf(refs, refs.length - 1)), refs[refs.length - 1]);
  }

  private static void registerMock(
      RuntimeContext context, String originalRefStr, Mock newMock, String metaKey) {
    Map<FunctionReference, List<Mock>> originalToMocks =
        context.getMetaData().getSerializableMeta(metaKey);
    if (originalToMocks == null) {
      originalToMocks = new HashMap<>();
    }
    FunctionReference originalRef = parseFuncRefStr(originalRefStr);
    if (!originalToMocks.containsKey(originalRef)) {
      originalToMocks.put(originalRef, new ArrayList<>(ImmutableList.of(newMock)));
    } else {
      originalToMocks.get(originalRef).add(newMock);
    }
    context.getMetaData().setSerializableMeta(metaKey, (Serializable) originalToMocks);
  }

  private MockingFns() {}
}
