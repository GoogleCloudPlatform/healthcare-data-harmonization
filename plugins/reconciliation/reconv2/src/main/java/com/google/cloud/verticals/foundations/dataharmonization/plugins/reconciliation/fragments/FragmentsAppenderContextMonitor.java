// Copyright 2024 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.fragments;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContextMonitor;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

/**
 * FragmentsAppenderContextMonitor extracts the "fragments" array from the runtime context metadata
 * and adds it to the returnData container to be output from the whistle transformation. If the
 * return data is not a container, the fragments will not be added.
 */
public class FragmentsAppenderContextMonitor implements RuntimeContextMonitor {

  public FragmentsAppenderContextMonitor() {}

  private boolean enabled = false;
  private static final String FRAGMENTS_FIELD = "fragments";

  @Override
  @CanIgnoreReturnValue
  public Data onRuntimeContextFinish(RuntimeContext context, Data returnData) {
    if (!enabled) {
      return returnData;
    }
    // TODO(): Support returnData as a bundle resource.
    if (returnData.isNullOrEmpty() || !returnData.isContainer()) {
      return returnData;
    }

    Container returnContainer = returnData.deepCopy().asContainer();
    if (!returnContainer.getField(FRAGMENTS_FIELD).isNullOrEmpty()) {
      // Don't overwrite the fragments key if the customer's mapping already has it set to something
      // in their mapping output.
      return returnContainer;
    }

    Array fragments = context.getMetaData().getSerializableMeta("fragments");

    if (fragments != null) {
      returnContainer.setField("fragments", fragments);
    }
    return returnContainer;
  }

  /** enable enables the MappingContextMonitor's onRuntimeContextFinish function. */
  public void enable() {
    this.enabled = true;
  }

  /**
   * disable disables the MappingContextMonitor's onRuntimeContextFinish function, making it a
   * no-op.
   */
  public void disable() {
    this.enabled = false;
  }
}
