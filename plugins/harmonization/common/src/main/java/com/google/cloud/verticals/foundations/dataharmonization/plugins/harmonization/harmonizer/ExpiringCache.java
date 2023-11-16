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

import java.io.Serializable;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Caches the result of RemoteHarmonizer along with time tags to enforce expiring policies. */
public class ExpiringCache implements Serializable {
  /** To cache the result of remote ConceptMap translate along with the time tag. */
  static final class LookupValue implements Serializable {
    private final List<HarmonizedCode> harmonizedCodes;
    private final long lastUpdate;

    public LookupValue(List<HarmonizedCode> harmonizedCodes, long currentTimeMillis) {
      this.harmonizedCodes = harmonizedCodes;
      this.lastUpdate = currentTimeMillis;
    }

    public List<HarmonizedCode> getHarmonizedCodes() {
      return harmonizedCodes;
    }

    public long getLastUpdate() {
      return lastUpdate;
    }
  }

  /** To lookup the result of remote ConceptMap translate in the local cache. */
  static final class LookupKey implements Serializable {
    private final String sourceCode;
    private final String sourceSystem;
    private final String conceptmapId;

    public LookupKey(String sourceCode, String sourceSystem, String conceptmapId) {
      this.sourceCode = sourceCode;
      this.sourceSystem = sourceSystem;
      this.conceptmapId = conceptmapId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      LookupKey lookupKey = (LookupKey) o;
      return Objects.equals(sourceCode, lookupKey.sourceCode)
          && Objects.equals(sourceSystem, lookupKey.sourceSystem)
          && Objects.equals(conceptmapId, lookupKey.conceptmapId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(sourceCode, sourceSystem, conceptmapId);
    }
  }

  private final Clock clock;
  private final long ttl;
  private final long cleanupInterval;
  private long lastCleanup;
  private Map<LookupKey, LookupValue> cache;

  public ExpiringCache(Clock clock, long ttl, long cleanupInterval) {
    this.clock = clock;
    this.ttl = ttl;
    this.cleanupInterval = cleanupInterval;
    this.lastCleanup = clock.millis();
    this.cache = new HashMap<LookupKey, LookupValue>();
  }

  public void put(
      String sourceCode, String sourceSystem, String conceptmapId, List<HarmonizedCode> value) {
    LookupKey key = new LookupKey(sourceCode, sourceSystem, conceptmapId);
    if (containsKey(sourceCode, sourceSystem, conceptmapId)) {
      this.cache.remove(key);
    }
    this.cache.put(key, new LookupValue(value, clock.millis()));
  }

  public boolean containsKey(String sourceCode, String sourceSystem, String conceptmapId) {
    LookupKey key = new LookupKey(sourceCode, sourceSystem, conceptmapId);
    if (this.cache.containsKey(key)) {
      LookupValue lookupResult = this.cache.get(key);
      if (isExpired(lookupResult.getLastUpdate())) {
        this.cache.remove(key);
        return false;
      }
      return true;
    }
    return false;
  }

  public List<HarmonizedCode> get(String sourceCode, String sourceSystem, String conceptmapId) {
    if (containsKey(sourceCode, sourceSystem, conceptmapId)) {
      LookupKey key = new LookupKey(sourceCode, sourceSystem, conceptmapId);
      return this.cache.get(key).getHarmonizedCodes();
    }
    return null;
  }

  private boolean isExpired(long lastUpdate) {
    return clock.millis() - lastUpdate > this.ttl * 1000;
  }

  private boolean timeToCleanup() {
    return clock.millis() - this.lastCleanup > this.cleanupInterval * 1000;
  }

  public int size() {
    return this.cache.size();
  }

  public void cleanup() {
    if (timeToCleanup()) {
      List<LookupKey> toRemoveList = new ArrayList<>();
      for (Map.Entry<LookupKey, LookupValue> entry : this.cache.entrySet()) {
        if (isExpired(entry.getValue().getLastUpdate())) {
          toRemoveList.add(entry.getKey());
        }
      }
      toRemoveList.forEach(key -> this.cache.remove(key));
      this.lastCleanup = clock.millis();
    }
  }

  public long getTtl() {
    return ttl;
  }

  public long getCleanupInterval() {
    return cleanupInterval;
  }
}
