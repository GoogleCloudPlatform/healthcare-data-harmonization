/*
 * Copyright 2021 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.registry.util;

import static com.google.common.truth.Truth.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/** Tests for LevenshteinDistance. */
@RunWith(Parameterized.class)
public class LevenshteinDistanceTest {

  @Parameter public String testName;

  @Parameter(1)
  public String[] candidates;

  @Parameter(2)
  public String target;

  @Parameter(3)
  public int threshold;

  @Parameter(4)
  public String[] expectedBestMatch;

  @Parameter(5)
  public String[] expectedAcceptedMatch;

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {
            "exact match",
            new String[] {"abc", "google", "cloud", "hello"},
            "google",
            1,
            new String[] {"google"},
            new String[] {"google"},
          },
          {
            "completely different",
            new String[] {"alpha", "beta", "delta", "lambda"},
            "test",
            1,
            new String[] {"beta", "delta"},
            new String[] {},
          },
          {
            "transpose of two adjacent characters and accept",
            new String[] {"alpha", "beta", "delta", "lambda"},
            "lamdba",
            2,
            new String[] {"lambda"},
            new String[] {"lambda"},
          },
          {
            "transpose of two adjacent characters but not accept",
            new String[] {"whistle", "whisper", "waterloo", "turtle"},
            "whislte",
            1,
            new String[] {"whistle"},
            new String[] {},
          },
          {
            "insert",
            new String[] {"a", "ab", "abc", "abcd"},
            "",
            2,
            new String[] {"a"},
            new String[] {"a"},
          },
          {
            "delete",
            new String[] {"a", "ab", "abc", "abcd"},
            "abcde",
            2,
            new String[] {"abcd"},
            new String[] {"abcd"},
          },
          {
            "insert and delete",
            new String[] {"a", "ab", "abcd", "abcde"},
            "abc",
            1,
            new String[] {"ab", "abcd"},
            new String[] {"ab", "abcd"},
          },
          {
            "empty target",
            new String[] {"a", "ab", "abc", "abcd"},
            "",
            1,
            new String[] {"a"},
            new String[] {"a"},
          },
          {
            "empty in candidates",
            new String[] {"abcde", "bcdef", "", "cdefg"},
            "ab",
            1,
            new String[] {""},
            new String[] {},
          },
        });
  }

  @Test
  public void testPickAndAccept() {
    StringSimilarity similarity = new LevenshteinDistance(threshold);
    List<String> bestMatch = similarity.pick(new HashSet<>(Arrays.asList(candidates)), target);
    assertThat(Arrays.asList(expectedBestMatch)).containsExactlyElementsIn(bestMatch);
    List<String> accepted =
        bestMatch.stream().filter(m -> similarity.accept(m, target)).collect(Collectors.toList());
    assertThat(Arrays.asList(expectedAcceptedMatch)).containsExactlyElementsIn(accepted);
  }
}
