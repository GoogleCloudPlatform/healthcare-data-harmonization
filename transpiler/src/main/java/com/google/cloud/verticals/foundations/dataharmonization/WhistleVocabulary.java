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

package com.google.cloud.verticals.foundations.dataharmonization;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import org.antlr.v4.runtime.Vocabulary;

/**
 * Class which acts as a wrapper to an instance of a default {@link Vocabulary} implementation. This
 * class allows us to override token display names based on the entries defined in the tokenMap
 * class below. If no override is defined for any given token, the class will return values from the
 * default Vocabulary instance which it wraps.
 */
public class WhistleVocabulary {
  public static final String ARRAY_APPEND = "[]";

  private final Vocabulary defaultVocabularyImpl;

  final ImmutableMap<String, String> tokenMap =
      ImmutableMap.<String, String>builder()
          .put("STRING_FRAGMENT_INTERP_START", "STRING")
          .put("STRING_FRAGMENT_INTERP_END", "STRING")
          .put("TERM_STRING", "STRING")
          .put("CONST_STRING", "STRING")
          .put(
              "SIMPLE_IDENTIFIER",
              "IDENTIFIER (Examples of Identifiers include, Function, Variable and Package names)")
          .put(
              "COMPLEX_IDENTIFIER",
              "IDENTIFIER (Examples of Identifiers include, Function, Variable and Package names)")
          .put("'\n'", "NEWLINE")
          .put("\n", "NEWLINE")
          .put("'\\n'", "NEWLINE")
          .buildOrThrow();

  public WhistleVocabulary(Vocabulary defaultVocabularyImpl) {
    this.defaultVocabularyImpl = defaultVocabularyImpl;
  }

  /**
   * Method which will either return the default Token String from the default Vocabulary, or use
   * the tokenMap defined above.
   *
   * @param intervals A List of intervals for which we need to retrieve tokens
   * @return A set of token strings.
   */
  public ImmutableSet<String> getDisplayName(List<Integer> intervals) {
    ImmutableSet.Builder<String> tokens = new ImmutableSet.Builder<>();
    for (Integer i : intervals) {
      String defaultToken = defaultVocabularyImpl.getDisplayName(i);
      // We have a replacement token
      if (tokenMap.containsKey(defaultToken)) {
        String returnToken = tokenMap.get(defaultToken);
        tokens.add(returnToken);
      } else {
        tokens.add(defaultToken);
      }
    }
    return tokens.build();
  }
}
