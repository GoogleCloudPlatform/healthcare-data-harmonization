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

import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.FieldMergeMethods.forceInbound;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.FieldMergeMethods.preferInbound;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.FieldMergeMethods.union;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.FieldMergeMethods.unionByField;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.DEFAULT_FIELD_RULES_METHOD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.FORCE_INBOUND_RULE;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.PATH_FIELD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.PREFER_INBOUND_RULE;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.RULE_FIELD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.UNION_BY_FIELD_RULE;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.UNION_RULE;

import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeResultAnnotator;
import java.io.Serializable;
import javax.naming.ConfigurationException;

/** Helper class defining a field-level merge rule for reconciling with Default rules. */
public class DefaultMergeRule implements Serializable {
  private final MergeResultAnnotator annotator;
  private final Data existing;
  private final Data inbound;
  private final String field;
  private final String fieldType;
  private final Container fieldMergeRule;

  public DefaultMergeRule(
      MergeResultAnnotator annotator,
      Container existing,
      Container inbound,
      String field,
      String fieldType,
      Container defaultFieldMergeRules)
      throws ConfigurationException {
    this.annotator = annotator;
    this.existing = existing.getField(field);
    this.inbound = inbound.getField(field);
    this.field = field;
    this.fieldType = fieldType;
    this.fieldMergeRule = getMergeRule(field, fieldType, defaultFieldMergeRules);
  }

  /**
   * Performs a merge applying the DefaultMergeRule instance's rule over its existing and inbound
   * values for its associated field.
   *
   * @param context {@link RuntimeContext} within which to execute merge operation.
   * @return {@link Data} result of the performed merge operation.
   * @throws ConfigurationException when the default merge rule to be used is improperly configured.
   */
  public Data merge(RuntimeContext context) throws ConfigurationException {
    return annotator.annotate(context, DEFAULT_FIELD_RULES_METHOD, mergeWithoutAnnotation(context));
  }

  private Data mergeWithoutAnnotation(RuntimeContext context) throws ConfigurationException {
    Data rule = this.fieldMergeRule.getField(RULE_FIELD);
    String ruleType = rule.asPrimitive().string();
    switch (ruleType) {
      case UNION_BY_FIELD_RULE:
        Data fieldPath = this.fieldMergeRule.getField(PATH_FIELD);
        return unionByField(context, this.existing, this.inbound, fieldPath.asPrimitive().string());
      case UNION_RULE:
        return union(context, this.existing, this.inbound);
      case PREFER_INBOUND_RULE:
        return preferInbound(context, this.existing, this.inbound);
      case FORCE_INBOUND_RULE:
        return forceInbound(context, this.existing, this.inbound);
      default:
        throw new ConfigurationException(
            String.format(
                "Default field merge rule definitions must be one of {'%s', '%s', '%s', '%s'}, but"
                    + " got '%s' as a default merge rule configuration for field '%s' with type"
                    + " '%s' in '%s()' of Default/merge-rules.wstl.",
                "{rule=" + FORCE_INBOUND_RULE + "}",
                "{rule=" + PREFER_INBOUND_RULE + "}",
                "{rule=" + UNION_RULE + "}",
                "{rule=" + UNION_BY_FIELD_RULE + ",fieldPath=<pathToField>}",
                this.fieldMergeRule,
                this.field,
                this.fieldType,
                DEFAULT_FIELD_RULES_METHOD));
    }
  }

  private Container getMergeRule(String field, String fieldType, Container defaultFieldMergeMethods)
      throws ConfigurationException {
    Data fieldMergeRule = getFieldMergeRule(field, fieldType, defaultFieldMergeMethods);
    return validateFieldMergeRule(field, fieldType, fieldMergeRule);
  }

  private Data getFieldMergeRule(
      String field, String fieldType, Container defaultFieldMergeMethods) {
    // Check if there's a default merge rule for a specific field (mapped to that field name).
    Data fieldMergeRule = defaultFieldMergeMethods.getField(field);
    if (!fieldMergeRule.isNullOrEmpty()) {
      return fieldMergeRule;
    }
    // Otherwise, use the default merge rule for the given field's data type.
    return defaultFieldMergeMethods.getField(fieldType);
  }

  private Container validateFieldMergeRule(String field, String fieldType, Data rule)
      throws ConfigurationException {
    // Throw exception if the merge rule for the field's name or type is invalidly formatted.
    if (!rule.isContainer() || rule.isNullOrEmpty()) {
      throwImproperlyDefinedMergeRule(field, fieldType, rule);
    }
    // Throw exception if 'rule' or 'path' fields are improperly configured for the merge rule.
    if (!validRuleField(rule) || !validPathField(rule)) {
      throwImproperlyDefinedMergeRule(field, fieldType, rule);
    }
    return rule.asContainer();
  }

  private boolean validRuleField(Data rule) {
    return isPrimitiveString(rule.asContainer().getField(RULE_FIELD));
  }

  private boolean validPathField(Data rule) {
    if (rule.asContainer()
        .getField(RULE_FIELD)
        .asPrimitive()
        .string()
        .equals(UNION_BY_FIELD_RULE)) {
      return isPrimitiveString(rule.asContainer().getField(PATH_FIELD));
    }
    return true;
  }

  private void throwImproperlyDefinedMergeRule(String field, String fieldType, Data rule)
      throws ConfigurationException {
    throw new ConfigurationException(
        String.format(
            "Improperly defined default field merge rule for '%s' type field '%s' with merge rule"
                + " defined as '%s'. Default field-level merge rules must be configured in the form"
                + " '<fieldName/fieldDataType>: {rule: <mergeRuleName>, (optional) path:"
                + " <unionByFieldPath>}'in '%s()' of Default/merge-rules.wstl.",
            fieldType, field, rule, DEFAULT_FIELD_RULES_METHOD));
  }

  private static boolean isPrimitiveString(Data data) {
    return !data.isNullOrEmpty() && data.isPrimitive() && (data.asPrimitive().string() != null);
  }
}
