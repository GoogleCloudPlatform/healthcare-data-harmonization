// Copyright 2019 Google LLC
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

package harmonizecode

import (
	"sync"
	"time"
)

// CodeLookupKey is the key to look for in the cache.
type CodeLookupKey struct {
	Code    string
	System  string
	Version string
}

// CodeLookupValue stores the cached values.
type CodeLookupValue struct {
	value       []HarmonizedCode
	lastUpdated int64
}

// ExpiringCache contains a sync.Map and implements read through cache functionalities.
type ExpiringCache struct {
	cache *sync.Map // map[CodeLookupKey]CodeLookupValue
	ttl   int       // duration in seconds for which a code is valid in the map.
}

// NewCache creates a new expiring cache with specified TTL and cleanup interval in seconds.
func NewCache(ttl int, cleanup int) *ExpiringCache {
	cache := &sync.Map{}
	if cleanup <= 0 {
		return &ExpiringCache{cache: cache, ttl: ttl}
	}

	go func() {
		for now := range time.Tick(time.Duration(cleanup) * time.Second) {
			cache.Range(func(k interface{}, v interface{}) bool {
				if val, ok := v.(CodeLookupValue); ok {
					if now.Unix()-val.lastUpdated >= int64(ttl) {
						cache.Delete(k)
					}
				}
				return true
			})
		}
	}()
	return &ExpiringCache{cache: cache, ttl: ttl}
}

// Get retrieves a value from the ExpiringCache by key, only if the value has not yet expired.
func (m *ExpiringCache) Get(key CodeLookupKey) (CodeLookupValue, bool) {
	if m.ttl <= 0 {
		return CodeLookupValue{}, false
	}

	if v, ok := m.cache.Load(key); ok {
		if clv, ok := v.(CodeLookupValue); ok {
			if time.Now().Unix()-clv.lastUpdated < int64(m.ttl) {
				return clv, true
			}
		}
		m.cache.Delete(key)
	}
	return CodeLookupValue{}, false
}

// Len returns the length of a sync.Map.
func (m *ExpiringCache) Len() int {
	count := 0
	m.cache.Range(func(_, _ interface{}) bool {
		count++
		return true
	})
	return count
}

// Put adds an array of harmonized codes into the cache with a timestamp.
func (m *ExpiringCache) Put(key CodeLookupKey, val []HarmonizedCode) {
	if m.ttl <= 0 {
		return
	}

	m.cache.Store(key, CodeLookupValue{
		value:       val,
		lastUpdated: time.Now().Unix(),
	})
}
