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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.defaults;

import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.ARRAY_TYPE;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.CHOICE_FIELDS;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.CONTAINER_TYPE;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.PRIMITIVE_TYPE;

import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.ChoiceFieldGroup;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import javax.naming.ConfigurationException;

/** Helper class for setting up field-level merging with a given resource type and wstl package. */
public class DefaultMerger implements Serializable {

  private final DefaultMergeRuleCreator defaultMergeRuleCreator;
  private final MergedChoiceFieldFactory mergedChoiceFieldFactory;

  public DefaultMerger(
      DefaultMergeRuleCreator defaultMergeRuleCreator,
      MergedChoiceFieldFactory mergedChoiceFieldFactory) {
    this.defaultMergeRuleCreator = defaultMergeRuleCreator;
    this.mergedChoiceFieldFactory = mergedChoiceFieldFactory;
  }

  /**
   * Processes the un-configured fields present in the existing or inbound resources, but not in the
   * merged resource according the appropriate Default field merge rules.
   *
   * @param context {@link RuntimeContext} within which to process.
   * @param merged {@link Container} with merged resource.
   * @param existing {@link Container} with existing resource.
   * @param inbound {@link Container} with inbound resource.
   * @param resourceType String FHIR resource type.
   * @return {@link Container} merged resource with un-configured fields merged.
   * @throws ConfigurationException when the Default field merge rules are mis-configured.
   */
  public Container processUnmergedFields(
      RuntimeContext context,
      Container merged,
      Container existing,
      Container inbound,
      String resourceType,
      boolean isEmpiPersonResource)
      throws ConfigurationException {
    NavigableSet<String> unmergedFields = getUnmergedFields(merged, existing, inbound);
    if (unmergedFields.isEmpty()) {
      return merged;
    }

    for (String field : unmergedFields) {
      // We allow link field for EMPI person resources to be empty.
      if (isEmpiPersonResource && field.equals("link")) {
        continue;
      }
      // TODO (): Add capability for dynamic storage of choice field names in a
      //  resourceType:listOf(choiceFields) map in RuntimeContext
      Optional<DefaultMergeRule> mr = getMergeRule(existing, inbound, field, resourceType);
      if (mr.isPresent()) {
        merged = merged.setField(field, mr.get().merge(context));
      }
    }
    return merged;
  }

  /**
   * Performs a pairwise merge over an existing and an inbound resource using the configured Default
   * field merge rules in place of a resource-type-specific merge function.
   *
   * @param context {@link RuntimeContext} within which to process.
   * @param existing {@link Container} with existing resource.
   * @param inbound {@link Container} with inbound resource.
   * @param resourceType String FHIR resource type.
   * @return {@link Container} merged resource with all fields merged.
   * @throws ConfigurationException when the Default field merge rules are mis-configured.
   */
  public Container mergeWithDefaultFieldRules(
      RuntimeContext context, Container existing, Container inbound, String resourceType)
      throws ConfigurationException {
    // Collect all fields from the existing and inbound resources.
    NavigableSet<String> allFields = new TreeSet<>(existing.fields());
    allFields.addAll(inbound.fields());

    // Merge over all non-choice fields with configured Default field merge rules.
    Container merged = context.getDataTypeImplementation().emptyContainer();
    for (String field : allFields) {
      Optional<DefaultMergeRule> mr = getMergeRule(existing, inbound, field, resourceType);
      if (mr.isPresent()) { // Optional.empty signals that the field is a choice field.
        merged = merged.setField(field, mr.get().merge(context));
      }
    }

    // Merge the choice fields for the given FHIR resource type.
    ImmutableSet<ImmutableSet<String>> choiceFieldGroups =
        CHOICE_FIELDS.getOrDefault(resourceType, ChoiceFieldGroup.builder().build()).getGroups();
    for (Set<String> choiceFieldGroup : choiceFieldGroups) {
      MergedChoiceField mergedChoiceField =
          mergedChoiceFieldFactory.create(context, existing, inbound, choiceFieldGroup);
      merged = merged.setField(mergedChoiceField.getField(), mergedChoiceField.getValue());
    }
    return merged;
  }

  private NavigableSet<String> getUnmergedFields(
      Container merged, Container existing, Container inbound) {
    NavigableSet<String> unmergedFields =
        new TreeSet<>(existing.fields()); // Take all the fields in existing,
    unmergedFields.addAll(inbound.fields()); // union with all the fields in inbound,
    unmergedFields.removeAll(merged.fields()); // then remove all fields already merged.
    return unmergedFields;
  }

  private Optional<DefaultMergeRule> getMergeRule(
      Container existing, Container inbound, String field, String resourceType)
      throws ConfigurationException {
    String fieldDataType = getFieldDataType(existing, inbound, field);
    return defaultMergeRuleCreator.createDefaultMergeRule(
        resourceType, existing, inbound, field, fieldDataType);
  }

  private String getFieldDataType(Container existing, Container inbound, String field) {
    Data fieldValue =
        inbound.getField(field).isNullOrEmpty()
            ? existing.getField(field)
            : inbound.getField(field);
    if (fieldValue.isPrimitive()) {
      return PRIMITIVE_TYPE;
    } else if (fieldValue.isArray()) {
      return ARRAY_TYPE;
    } else {
      return CONTAINER_TYPE;
    }
  }
}
