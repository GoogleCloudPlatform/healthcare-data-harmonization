// Copyright 2020 Google LLC.
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

package errors

import (
	"fmt"
)

const (
	th = "th"
	st = "st"
	nd = "nd"
	rd = "rd"
)

// SuffixNumber adds an English ordinal indicator to the end of the given number.
// For example, 1 -> "1st", 54 -> "54th"
func SuffixNumber(num int) string {
	keyNum := num % 20
	suffix := th
	switch keyNum {
	case 1:
		suffix = st
	case 2:
		suffix = nd
	case 3:
		suffix = rd
	}

	return fmt.Sprintf("%d%s", num, suffix)
}
