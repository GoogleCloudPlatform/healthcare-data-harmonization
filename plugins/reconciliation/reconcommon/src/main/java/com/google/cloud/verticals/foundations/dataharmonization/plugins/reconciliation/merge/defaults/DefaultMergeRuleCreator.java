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

import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.CHOICE_FIELDS;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.DEFAULT_FIELD_RULES_METHOD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.MERGE_RULES_PACKAGE;

import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.function.DefaultClosure;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.ChoiceFieldGroup;
import java.io.Serializable;
import java.util.Optional;
import javax.naming.ConfigurationException;

/** DefaultMergeRuleCreator creates default merge rules */
public class DefaultMergeRuleCreator implements Serializable {

  private final Container defaultFieldMergeRules;
  private final DefaultMergeRuleFactory mergeRuleFactory;

  public DefaultMergeRuleCreator(DefaultMergeRuleFactory mergeRuleFactory, RuntimeContext ctx)
      throws ConfigurationException {
    this(mergeRuleFactory, getDefaultFieldMergeRules(ctx));
  }

  public DefaultMergeRuleCreator(
      DefaultMergeRuleFactory mergeRuleFactory, Container defaultFieldMergeRules) {
    this.mergeRuleFactory = mergeRuleFactory;
    this.defaultFieldMergeRules = defaultFieldMergeRules;
  }

  /**
   * Builds {@link Optional <DefaultMergeRule>} with a new DefaultMergeRule instance for performing
   * a field-level merge over the given 'field' if it is not a choice field and an empty Optional
   * instance if it is.
   *
   * @param resourceType String FHIR resource type.
   * @param existing {@link Container} to select field to merge from.
   * @param inbound {@link Container} to select field to merge from.
   * @param field String field to merge.
   * @param fieldType String data type (Primitive, Array, or Container) of the field to merge.
   * @return {@link Optional<DefaultMergeRule>} with a new DefaultMergeRule instance if 'field' is
   *     not a choice field and an empty Optional instance otherwise.
   * @throws ConfigurationException when the default merge rule to be used is improperly configured.
   */
  public Optional<DefaultMergeRule> createDefaultMergeRule(
      String resourceType, Container existing, Container inbound, String field, String fieldType)
      throws ConfigurationException {
    if (isChoiceField(field, resourceType)) {
      return Optional.empty();
    } else {
      return Optional.of(
          mergeRuleFactory.create(existing, inbound, field, fieldType, defaultFieldMergeRules));
    }
  }

  private boolean isChoiceField(String field, String resourceType) {
    return CHOICE_FIELDS
        .getOrDefault(resourceType, ChoiceFieldGroup.builder().build())
        .isChoiceField(field);
  }

  private static Container getDefaultFieldMergeRules(RuntimeContext context)
      throws ConfigurationException {
    try {
      Container mergeRules =
          DefaultClosure.create(
                  new DefaultClosure.FunctionReference(
                      MERGE_RULES_PACKAGE, DEFAULT_FIELD_RULES_METHOD))
              .execute(context)
              .asContainer();
      if (mergeRules == null) {
        throw throwInvalidDefaultFieldMergeRules("Null merge rules.");
      }
      return mergeRules;
    } catch (RuntimeException e) {
      throw throwInvalidDefaultFieldMergeRules(e.getMessage());
    }
  }

  private static ConfigurationException throwInvalidDefaultFieldMergeRules(String message) {
    return new ConfigurationException(
        String.format(
            "Must configure '%s()' method in file Default/merge-rules.wstl of package '%s' to"
                + " provide default field-level merge rules for field data types (Primitive, Array,"
                + " and Container), as well as any desired field names. Error message: %s",
            DEFAULT_FIELD_RULES_METHOD, MERGE_RULES_PACKAGE, message));
  }
}
