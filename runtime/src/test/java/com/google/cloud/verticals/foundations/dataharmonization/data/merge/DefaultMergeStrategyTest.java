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
package com.google.cloud.verticals.foundations.dataharmonization.data.merge;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.arbitrary;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.arrayOf;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.mutableArrayOf;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.path.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for DefaultMergeStrategy TODO(rpolyano): Move other indirect tests of this back into here.
 */
@RunWith(JUnit4.class)
public class DefaultMergeStrategyTest {

  @Test
  public void merge_arrayWithSelf() {
    Data[] elements = new Data[] {arbitrary(), arbitrary()};
    Array base = mutableArrayOf(elements);
    Array copy = arrayOf(elements);
    when(base.deepCopy()).thenReturn(copy);

    Data got = DefaultMergeStrategy.INSTANCE.merge(testDTI(), base, base, Path.empty());
    assertThat(got.isArray()).isTrue();
    assertThat(got.asArray().size()).isEqualTo(4);
    assertThat(got.asArray().getElement(0)).isSameInstanceAs(elements[0]);
    assertThat(got.asArray().getElement(1)).isSameInstanceAs(elements[1]);
    assertThat(got.asArray().getElement(2)).isSameInstanceAs(elements[0]);
    assertThat(got.asArray().getElement(3)).isSameInstanceAs(elements[1]);
    verify(base).deepCopy();
  }
}
