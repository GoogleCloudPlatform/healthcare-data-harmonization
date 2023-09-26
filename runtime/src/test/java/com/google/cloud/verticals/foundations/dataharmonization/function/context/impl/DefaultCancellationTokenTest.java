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
package com.google.cloud.verticals.foundations.dataharmonization.function.context.impl;

import static com.google.common.truth.Truth.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for DefaultCancellationToken. */
@RunWith(JUnit4.class)
public class DefaultCancellationTokenTest {

  @Test
  public void test_cancel_cancels() {
    DefaultCancellationToken t = new DefaultCancellationToken();

    assertThat(t.isCancelled()).isFalse();
    assertThat(t.getReason()).isNull();

    boolean got = t.cancel("test");

    assertThat(got).isTrue();
    assertThat(t.isCancelled()).isTrue();
    assertThat(t.getReason()).isEqualTo("test");
  }

  @Test
  public void test_cancel_callsPreviouslyRegisteredCallbacks() {
    DefaultCancellationToken t = new DefaultCancellationToken();

    AtomicBoolean called = new AtomicBoolean();
    t.registerCancelCallback(
        c -> {
          called.set(true);
          assertThat(c).isSameInstanceAs(t);
        });

    assertThat(called.get()).isFalse();

    t.cancel("test");

    assertThat(called.get()).isTrue();
  }

  @Test
  public void test_cancel_callsLateRegisteredCallbacksImmediately() {
    DefaultCancellationToken t = new DefaultCancellationToken();

    boolean got = t.cancel("test");

    AtomicBoolean called = new AtomicBoolean();
    t.registerCancelCallback(
        c -> {
          called.set(true);
          assertThat(c).isSameInstanceAs(t);
        });

    assertThat(got).isTrue();
    assertThat(called.get()).isTrue();
  }

  @Test
  public void test_multipleCancels_secondNoops() {
    DefaultCancellationToken t = new DefaultCancellationToken();

    boolean got1 = t.cancel("test");
    boolean got2 = t.cancel("aaaaaaaaa");

    assertThat(got1).isTrue();
    assertThat(got2).isFalse();
    assertThat(t.getReason()).isEqualTo("test");
  }
}
