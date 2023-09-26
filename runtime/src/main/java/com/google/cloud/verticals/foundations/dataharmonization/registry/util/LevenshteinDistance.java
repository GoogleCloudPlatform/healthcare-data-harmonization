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

import static com.google.common.primitives.Ints.min;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/**
 * A memory efficient version of Levenshtein Distance algorithm to define a string similarity.
 * (https://en.wikipedia.org/wiki/Levenshtein_distance) All costs of each character insertion,
 * deletion, or substitution are equally 1.
 */
public class LevenshteinDistance implements StringSimilarity {

  private final int threshold;

  public LevenshteinDistance(int threshold) {
    this.threshold = threshold;
  }

  @Override
  public List<String> pick(Set<String> candidates, String target) {
    Map<String, Integer> distances =
        candidates.stream().collect(Collectors.toMap(c -> c, c -> distance(c, target)));
    Integer best = distances.values().stream().min(Integer::compare).get();
    return distances.entrySet().stream()
        .filter(e -> e.getValue().equals(best))
        .map(Entry::getKey)
        .collect(Collectors.toList());
  }

  @Override
  public boolean accept(String text, String target) {
    return distance(text, target) <= threshold;
  }

  /** Calculate the Levenshtein distance between two strings. */
  public static int distance(@Nonnull String x, @Nonnull String y) {
    if (x.equals(y)) {
      return 0;
    }
    if (x.length() == 0) {
      return y.length();
    }
    if (y.length() == 0) {
      return x.length();
    }

    // Uses this rolling array to avoid copying and allocating a new array in each iteration of
    // dynamic programming.
    final int[][] dist = new int[2][y.length() + 1];

    for (int i = 0; i <= y.length(); i++) {
      dist[0][i] = i;
    }

    for (int i = 1; i <= x.length(); i++) {
      int cur = i & 1;
      dist[cur][0] = i;
      for (int j = 1; j <= y.length(); j++) {
        dist[cur][j] =
            min(
                dist[1 - cur][j] + 1,
                dist[cur][j - 1] + 1,
                dist[1 - cur][j - 1] + (x.charAt(i - 1) == y.charAt(j - 1) ? 0 : 1));
      }
    }
    return dist[x.length() & 1][y.length()];
  }
}
