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
	"testing"
	"time"
)

func TestExpiringCache(t *testing.T) {
	tests := []struct {
		name            string
		ttl             int
		cleanupInterval int
		key             CodeLookupKey
		value           []HarmonizedCode
		wait            int
		expectedCache   int
	}{
		{
			"single entry",
			3,
			1,
			CodeLookupKey{
				Code:   "source-code",
				System: "source-system",
			},
			[]HarmonizedCode{},
			4,
			0,
		},
		{
			"single entry",
			3,
			3,
			CodeLookupKey{
				Code:   "source-code",
				System: "source-system",
			},
			[]HarmonizedCode{},
			2,
			1,
		},
	}

	for _, test := range tests {
		cache := NewCache(test.ttl, test.cleanupInterval)
		cache.Put(test.key, test.value)
		if cache.Len() != 1 {
			t.Errorf("ExpiringCache.Put failed to add an entry to the map")
		}

		time.Sleep(time.Duration(test.wait) * time.Second)
		got := cache.Len()
		if got != test.expectedCache {
			t.Errorf("ExpiringCache contains %v entries but expected %v", got, test.expectedCache)
		}
	}
}

func TestConcurrentGoroutines(t *testing.T) {
	m := NewCache(3, 1)
	var wg sync.WaitGroup
	addCache := func(delay int, k CodeLookupKey, expectedCount int) {
		wg.Add(1)
		defer wg.Done()
		time.Sleep(time.Duration(delay) * time.Second)
		m.Put(k, []HarmonizedCode{})

		l := m.Len()
		if l != expectedCount {
			t.Errorf("unexpected number of entries in expiring cache, got %v, want %v", l, expectedCount)
		}
	}

	go addCache(0, CodeLookupKey{"a", "b", "c"}, 1)
	go addCache(2, CodeLookupKey{"aa", "bb", "cc"}, 2)
	go addCache(4, CodeLookupKey{"aaa", "bbb", "ccc"}, 2)
	go addCache(6, CodeLookupKey{"aaaa", "bbbb", "cccc"}, 2)

	wg.Wait()
}
