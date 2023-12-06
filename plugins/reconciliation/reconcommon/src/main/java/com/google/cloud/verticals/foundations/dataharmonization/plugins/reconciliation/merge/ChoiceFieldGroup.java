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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;

/** Immutable class to hold choiceX fields for a FHIR resource type. */
@AutoValue
public abstract class ChoiceFieldGroup implements Serializable {
  /** Static factory method that creates a new ChoiceFieldGroup.Builder. */
  public static Builder builder() {
    return new AutoValue_ChoiceFieldGroup.Builder();
  }

  /** Returns {@link ImmutableSet<String>} representing the set of choiceX field groups. */
  public abstract ImmutableSet<ImmutableSet<String>> getGroups();

  /** Returns {@link ImmutableSet<String>} representing all choiceX fields for the resource type. */
  public abstract ImmutableSet<String> getAllFields();

  /**
   * Checks if the provided field is a choice field within the given {@link ChoiceFieldGroup}.
   *
   * @param field String field name to check for.
   * @return true if the provided field is a choice field, false otherwise.
   */
  public boolean isChoiceField(String field) {
    return getAllFields().contains(field);
  }

  /** Static Builder class for {@link ChoiceFieldGroup} */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setGroups(ImmutableSet<ImmutableSet<String>> value);

    public abstract Builder setAllFields(ImmutableSet<String> value);

    abstract ImmutableSet.Builder<String> allFieldsBuilder();

    abstract ImmutableSet.Builder<ImmutableSet<String>> groupsBuilder();

    public final Builder withGroup(String... groupFields) {
      ImmutableSet<String> groupFieldsSet = ImmutableSet.copyOf(ImmutableSet.copyOf(groupFields));
      allFieldsBuilder().addAll(groupFieldsSet);
      groupsBuilder().add(groupFieldsSet);
      return this;
    }

    public abstract ChoiceFieldGroup build();
  }
}
