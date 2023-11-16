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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.harmonization.harmonizer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.time.Clock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link ExpiringCache}. */
@RunWith(JUnit4.class)
public class ExpiringCacheTest {

  @Test
  public void testTTLExpires() {
    final long ttl = 3;
    final long cleanupInterval = 5;

    String sourceCode = "code1";
    String sourceSystem = "system1";
    String conceptmapId = "conceptMap1";
    ImmutableList<HarmonizedCode> value =
        ImmutableList.of(
            HarmonizedCode.create(
                "converted_code1", "converted_display1", "converted_system1", "v1"));

    Clock mockClock = mock(Clock.class);
    when(mockClock.millis()).thenReturn(0L);
    ExpiringCache cache = new ExpiringCache(mockClock, ttl, cleanupInterval);
    cache.put(sourceCode, sourceSystem, conceptmapId, value);

    for (long seconds = 0L; seconds <= ttl; seconds++) {
      when(mockClock.millis()).thenReturn(seconds * 1000L);
      assertEquals(true, cache.containsKey(sourceCode, sourceSystem, conceptmapId));
      assertEquals(value, cache.get(sourceCode, sourceSystem, conceptmapId));
    }

    when(mockClock.millis()).thenReturn(ttl * 1000L + 1);
    assertEquals(false, cache.containsKey(sourceCode, sourceSystem, conceptmapId));
    assertEquals(null, cache.get(sourceCode, sourceSystem, conceptmapId));
  }

  @Test
  public void testCleanup() {
    final long ttl = 3;
    final long cleanupInterval = 5;

    String sourceCode1 = "code1";
    String sourceCode2 = "code2";
    String sourceCode3 = "code3";
    String sourceSystem = "system1";
    String conceptmapId = "conceptMap1";
    ImmutableList<HarmonizedCode> value =
        ImmutableList.of(
            HarmonizedCode.create(
                "converted_code1", "converted_display1", "converted_system1", "v1"));

    Clock mockClock = mock(Clock.class);
    when(mockClock.millis()).thenReturn(0L);
    ExpiringCache cache = new ExpiringCache(mockClock, ttl, cleanupInterval);

    cache.put(sourceCode1, sourceSystem, conceptmapId, value);
    cache.put(sourceCode2, sourceSystem, conceptmapId, value);
    cache.put(sourceCode3, sourceSystem, conceptmapId, value);
    assertEquals(3, cache.size());

    for (int seconds = 0; seconds <= cleanupInterval; seconds++) {
      when(mockClock.millis()).thenReturn(seconds * 1000L);
      cache.cleanup(); // Clean up does not run until cleanupInterval * 1000L + 1.
      assertEquals(3, cache.size());
    }

    when(mockClock.millis()).thenReturn(ttl * 1000L + 1);
    // At this time all 3 values are expired. Fetching them will cause them to be deleted.
    assertEquals(false, cache.containsKey(sourceCode1, sourceSystem, conceptmapId));
    assertEquals(2, cache.size());
    cache.cleanup(); // Clean up does not run until cleanupInterval * 1000L + 1.
    assertEquals(2, cache.size());

    // Add a new value, this one is not expired so during the cleanup it will not be deleted.
    cache.put(sourceCode1, sourceSystem, conceptmapId, value);
    assertEquals(3, cache.size());

    when(mockClock.millis()).thenReturn(cleanupInterval * 1000L + 1);
    assertEquals(3, cache.size());
    cache.cleanup();
    assertEquals(1, cache.size());
  }

  @Test
  public void testCleanupShorterThanTTL() {
    final long ttl = 5;
    final long cleanupInterval = 3;

    String sourceCode1 = "code1";
    String sourceCode2 = "code2";
    String sourceCode3 = "code3";
    String sourceSystem = "system1";
    String conceptmapId = "conceptMap1";
    ImmutableList<HarmonizedCode> value =
        ImmutableList.of(
            HarmonizedCode.create(
                "converted_code1", "converted_display1", "converted_system1", "v1"));

    Clock mockClock = mock(Clock.class);
    when(mockClock.millis()).thenReturn(0L);
    ExpiringCache cache = new ExpiringCache(mockClock, ttl, cleanupInterval);

    cache.put(sourceCode1, sourceSystem, conceptmapId, value);
    cache.put(sourceCode2, sourceSystem, conceptmapId, value);
    assertEquals(2, cache.size());
    for (int seconds = 0; seconds <= cleanupInterval; seconds++) {
      when(mockClock.millis()).thenReturn(seconds * 1000L);
      cache.cleanup();
      assertEquals(2, cache.size());
    }

    when(mockClock.millis()).thenReturn(cleanupInterval * 1000L + 1L);
    cache.cleanup(); // Cleanup runs but nothing has expired yet.
    assertEquals(2, cache.size());

    when(mockClock.millis()).thenReturn(ttl * 1000L + 1L);
    cache.put(sourceCode3, sourceSystem, conceptmapId, value);
    assertEquals(3, cache.size());
    cache.cleanup(); // Clean up does not run until 2 * cleanupInterval * 1000L + 2.
    assertEquals(3, cache.size());

    when(mockClock.millis()).thenReturn(2 * cleanupInterval * 1000L + 1L);
    cache.cleanup(); // Clean up does not run until 2 * cleanupInterval * 1000L + 2.
    assertEquals(3, cache.size());

    when(mockClock.millis()).thenReturn(2 * cleanupInterval * 1000L + 2L);
    cache.cleanup();
    assertEquals(1, cache.size());
  }
}
