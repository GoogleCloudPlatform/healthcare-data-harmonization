# CPCDS to FHIR R4 (Based on CARIN community [mappings](https://build.fhir.org/ig/HL7/carin-bb/Common_Payer_Consumer_Data_Set.html#mapping-from-cpcds-to-fhir-resources) to FHIR)

This folder contains a mapping between
[CPCDS](https://build.fhir.org/ig/HL7/carin-bb/Common_Payer_Consumer_Data_Set.html)
Data and FHIR R4 resources.

## Overview

The input to the mapping is a JSON representation of a BigQuery row. The source
table to FHIR resource mapping is as follows:

Source table -> FHIR resource type:

-   Claim ->
    [ExplanationOfBenefit](https://www.hl7.org/fhir/explanationofbenefit.html)
-   Coverage -> [Coverage](http://www.hl7.org/fhir/coverage.html)
-   Member -> [Patient](https://www.hl7.org/fhir/patient.html)
-   Organization -> [Organization](https://www.hl7.org/fhir/organization.html)
-   PractitionerRole ->
    [PractitionerRole](https://www.hl7.org/fhir/practitionerrole.html)

## Pre-processing for Claim table.

The raw claim table is missing the coverage ID, and each claim has multiple rows
for each line number, which corresponds to FHIR's
[ExplanationOfBenefit.item](https://www.hl7.org/fhir/explanationofbenefit-definitions.html#ExplanationOfBenefit.item).
Therefore some pre-processing is needed. The following describes the
pre-processing in more detail:

-   Group rows with the same `Claim_unique_identifier` into a single row, and
    the different `line_number` and related fields into a struct array.

-   Bring in one coverage ID matching `Claim.Patient_account_number` =
    `Coverage.Member_id`.

-   Convert some int types to string so mapping can be consistent.

The resulting query is as follows:

```sql
WITH DistinctCoverage AS (SELECT Claim_unique_identifier, ARRAY_AGG(Coverage.Coverage_id)[offset(0)] AS Coverage_id
FROM `rawClaim` AS Claim LEFT JOIN `rawCoverage` AS Coverage
ON Claim.Patient_account_number = Coverage.Member_id GROUP BY Claim.Claim_unique_identifier)
SELECT
ARRAY_AGG(Claim_paid_date)[OFFSET(0)] as Claim_paid_date,
ARRAY_AGG(Claim_received_date)[OFFSET(0)] as Claim_received_date,
ARRAY_AGG(Member_admission_date)[OFFSET(0)] as Member_admission_date,
ARRAY_AGG(Member_discharge_date)[OFFSET(0)] as Member_discharge_date,
ARRAY_AGG(Patient_account_number)[OFFSET(0)] as Patient_account_number,
ARRAY_AGG(Medical_record_number)[OFFSET(0)] as Medical_record_number,
ARRAY_AGG(Claim.Claim_unique_identifier)[OFFSET(0)] as Claim_unique_identifier,
ARRAY_AGG(Claim_adjusted_from_identifier)[OFFSET(0)] as Claim_adjusted_from_identifier,
ARRAY_AGG(Claim_adjusted_to_identifier)[OFFSET(0)] as Claim_adjusted_to_identifier,
ARRAY_AGG(Claim_diagnosis_related_group)[OFFSET(0)] as Claim_diagnosis_related_group,
ARRAY_AGG(Claim_source_inpatient_admission_code)[OFFSET(0)] as Claim_source_inpatient_admission_code,
ARRAY_AGG(Claim_inpatient_admission_type_code)[OFFSET(0)] as Claim_inpatient_admission_type_code,
ARRAY_AGG(Claim_bill_facility_type_code)[OFFSET(0)] as Claim_bill_facility_type_code,
ARRAY_AGG(Claim_service_classification_type_code)[OFFSET(0)] as Claim_service_classification_type_code,
ARRAY_AGG(Claim_frequency_code)[OFFSET(0)] as Claim_frequency_code,
ARRAY_AGG(Claim_processing_status_code)[OFFSET(0)] as Claim_processing_status_code,
ARRAY_AGG(Claim_type)[OFFSET(0)] as Claim_type,
ARRAY_AGG(Patient_discharge_status_code)[OFFSET(0)] as Patient_discharge_status_code,
ARRAY_AGG(Claim_payment_denial_code)[OFFSET(0)] as Claim_payment_denial_code,
ARRAY_AGG(Claim_primary_payer_identifier)[OFFSET(0)] as Claim_primary_payer_identifier,
ARRAY_AGG(Claim_payee_type_code)[OFFSET(0)] as Claim_payee_type_code,
ARRAY_AGG(Claim_payee)[OFFSET(0)] as Claim_payee,
ARRAY_AGG(Claim_payment_status_code)[OFFSET(0)] as Claim_payment_status_code,
ARRAY_AGG(Claim_payer_identifier)[OFFSET(0)] as Claim_payer_identifier,
ARRAY_AGG(Days_supply)[OFFSET(0)] as Days_supply,
ARRAY_AGG(RX_service_reference_number)[OFFSET(0)] as RX_service_reference_number,
ARRAY_AGG(DAW_product_selection_code)[OFFSET(0)] as DAW_product_selection_code,
ARRAY_AGG(Refill_number)[OFFSET(0)] as Refill_number,
ARRAY_AGG(Prescription_origin_code)[OFFSET(0)] as Prescription_origin_code,
ARRAY_AGG(Plan_reported_brand_generic_code)[OFFSET(0)] as Plan_reported_brand_generic_code,
ARRAY_AGG(Pharmacy_service_type_code)[OFFSET(0)] as Pharmacy_service_type_code,
ARRAY_AGG(Patient_residence_code)[OFFSET(0)] as Patient_residence_code,
ARRAY_AGG(Claim_billing_provider_NPI)[OFFSET(0)] as Claim_billing_provider_NPI,
ARRAY_AGG(Claim_billing_provider_network_status)[OFFSET(0)] as Claim_billing_provider_network_status,
ARRAY_AGG(Claim_attending_provider_NPI)[OFFSET(0)] as Claim_attending_provider_NPI,
ARRAY_AGG(Claim_attending_provider_network_status)[OFFSET(0)] as Claim_attending_provider_network_status,
ARRAY_AGG(Claim_site_of_service_NPI)[OFFSET(0)] as Claim_site_of_service_NPI,
ARRAY_AGG(Claim_site_of_service_network_status)[OFFSET(0)] as Claim_site_of_service_network_status,
ARRAY_AGG(Claim_referring_provider_NPI)[OFFSET(0)] as Claim_referring_provider_NPI,
ARRAY_AGG(Claim_referring_provider_network_status)[OFFSET(0)] as Claim_referring_provider_network_status,
ARRAY_AGG(Claim_performing_provider_NPI)[OFFSET(0)] as Claim_performing_provider_NPI,
ARRAY_AGG(Claim_performing_provider_network_status)[OFFSET(0)] as Claim_performing_provider_network_status,
ARRAY_AGG(Claim_prescribing_provider_NPI)[OFFSET(0)] as Claim_prescribing_provider_NPI,
ARRAY_AGG(Claim_prescribing_provider_network_status)[OFFSET(0)] as Claim_prescribing_provider_network_status,
ARRAY_AGG(Claim_PCP_NPI)[OFFSET(0)] as Claim_PCP_NPI,
ARRAY_AGG(Service__from__date)[OFFSET(0)] as Service__from__date,
ARRAY_AGG(Service_to_date)[OFFSET(0)] as Service_to_date,
ARRAY_AGG(Type_of_service)[OFFSET(0)] as Type_of_service,
ARRAY_AGG(STRUCT(
  Line_number, Claim_service_start_date, Claim_service_end_date, Place_of_service_code, Revenue_center_code, Number_of_units, Allowed_number_of_units, National_drug_code, Compound_code, Quantity_dispensed, Quantity_qualifier_code,
  Line_benefit_payment_status, Line_payment_denial_code, Diagnosis_code, Diagnosis_code_type, Diagnosis_description, Present_on_admission, Diagnosis_type, Procedure_code, Procedure_description, Procedure_date, Procedure_code_type, Procedure_type,
  Is_E_code, Modifier_Code_1, Modifier_Code_2, Modifier_Code_3, Modifier_Code_4,
  CAST(Line_disallowed_amount as String) as Line_disallowed_amount,
  CAST(Line_member_reimbursement as String) as Line_member_reimbursement,
  CAST(Line_amount_paid_by_patient as String) as Line_amount_paid_by_patient,
  CAST(Drug_cost as String) as Drug_cost,
  CAST(Line_payment_amount as String) as Line_payment_amount,
  CAST(Line_amount_paid_to_provider as String) as Line_amount_paid_to_provider,
  CAST(Line_patient_deductible as String) as Line_patient_deductible,
  CAST(Line_primary_payer_paid_amount as String) as Line_primary_payer_paid_amount,
  CAST(Line_coinsurance_amount as String) as Line_coinsurance_amount,
  CAST(Line_submitted_amount as String) as Line_submitted_amount,
  CAST(Line_allowed_amount as String) as Line_allowed_amount,
  CAST(Line_member_liability as String) as Line_member_liability,
  CAST(Line_copay_amount as String) as Line_copay_amount,
  CAST(Line_discount_amount as String) as Line_discount_amount
)) as Items,
ARRAY_AGG(DistinctCoverage.Coverage_id)[offset(0)] as Coverage_id,
CAST(MAX(Claim_total_submitted_amount) as String) as Claim_total_submitted_amount,
CAST(MAX(Claim_total_allowed_amount) as String) as Claim_total_allowed_amount,
CAST(MAX(Amount_paid_by_patient) as String) as Amount_paid_by_patient,
CAST(MAX(Claim_amount_paid_to_provider) as String) as Claim_amount_paid_to_provider,
CAST(MAX(Member_reimbursement) as String) as Member_reimbursement,
CAST(MAX(Claim_payment_amount) as String) as Claim_payment_amount,
CAST(MAX(Claim_disallowed_amount) as String) as Claim_disallowed_amount,
CAST(MAX(Member_paid_deductible) as String) as Member_paid_deductible,
CAST(MAX(Co_insurance_liability_amount) as String) as Co_insurance_liability_amount,
CAST(MAX(Copay_amount) as String) as Copay_amount,
CAST(MAX(Member_liability) as String) as Member_liability,
CAST(MAX(Claim_primary_payer_paid_amount) as String) as Claim_primary_payer_paid_amount,
CAST(MAX(Claim_discount_amount) as String) as Claim_discount_amount,
FROM `rawClaim` AS Claim
LEFT JOIN DistinctCoverage ON DistinctCoverage.Claim_unique_identifier = Claim.Claim_unique_identifier
GROUP BY Claim.Claim_unique_identifier
```

## Running the mapping

The mapping can be run using
[these instructions.](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_configs/README.md)
