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

package com.google.cloud.verticals.foundations.dataharmonization.data.impl;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.wrappers.WrapperData;

/** A utility class that provides fake wrapper data implementations. */
public final class WrapperDataUtils {

  private WrapperDataUtils() {}

  /** A {@link WrapperData} that inherit {@link TestWrapperData}. */
  public static final class ExtendedTestWrapperData extends TestWrapperData {
    public ExtendedTestWrapperData(Data backing) {
      super(backing);
    }
  }

  /** A shallow {@link WrapperData}. */
  public static class TestWrapperData extends WrapperData<TestWrapperData> {

    public TestWrapperData(Data backing) {
      super(backing);
    }

    @Override
    protected TestWrapperData rewrap(Data backing) {
      return new TestWrapperData(backing);
    }
  }

  /** A {@link WrapperData} that does not inherit {@link TestWrapperData}. */
  public static final class IrrelevantWrapperData extends WrapperData<IrrelevantWrapperData> {

    public IrrelevantWrapperData(Data backing) {
      super(backing);
    }

    @Override
    protected IrrelevantWrapperData rewrap(Data backing) {
      return new IrrelevantWrapperData(backing);
    }
  }
}
