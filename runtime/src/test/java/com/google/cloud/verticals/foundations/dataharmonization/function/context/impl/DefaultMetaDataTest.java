/*
 * Copyright 2022 Google LLC.
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
package com.google.cloud.verticals.foundations.dataharmonization.function.context.impl;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for Metadata. */
@RunWith(JUnit4.class)
public class DefaultMetaDataTest {

  @Test
  public void getFlag_readsOnlySerializableMeta() {
    DefaultMetaData metaData = new DefaultMetaData();
    metaData.setMeta("hello", true);

    assertThat(metaData.getFlag("hello")).isFalse();
  }

  @Test
  public void getFlag_readsSerializableMeta() {
    DefaultMetaData metaData = new DefaultMetaData();
    metaData.setSerializableMeta("hello", true);
    metaData.setSerializableMeta("hello2", false);

    assertThat(metaData.getFlag("hello")).isTrue();
    assertThat(metaData.getFlag("hello2")).isFalse();
  }

  @Test
  public void getFlag_nullMeta_returnsFalse() {
    DefaultMetaData metaData = new DefaultMetaData();
    assertThat(metaData.getFlag("hello")).isFalse();
  }

  @Test
  public void getFlag_nonBoolMeta_throws() {
    DefaultMetaData metaData = new DefaultMetaData();
    metaData.setSerializableMeta("hello", "pong");

    assertThrows(IllegalArgumentException.class, () -> metaData.getFlag("hello"));
  }
}
