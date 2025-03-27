# Quick Run

This is a tiny gradle config intended to provide a simple entry point to running
Whistle mappings until proper tooling is added to this repository.

It can be used like:

```shell
> cd quickrun
> gradle run --args="-m /<path to a whistle config>/.../<something>.wstl"
```

To test your Whistle matching and merging configs with an input file, run the
unit tests.

For example:

```shell
> cd quickrun
> gradle run --args="-m /<path to repository>/data_harmonization/mappings/reconciliation/unit_tests/unit_test.wstl"
```
To test mappings, the unit tests' input and output files, located in
`mappings/fhirVersionConversion/unit_tests/r3r4` and
`mappings/hl7v2_fhir_r4/unit_tests/datatypes` can be modified as needed. The
unit tests you run will depend on the source type you are testing.

To test matching and merging rules, the unit tests' input and output files,
located in `mappings/reconciliation/unit_tests/merge` and
`mappings/reconciliation/unit_tests/matching` can be modified as needed.

### Quickrun Example
For example, to test mapping rules for Patient resources in FHIR r3 to r4, do
the following:

```shell
# Substitute REPO with the path to your repository.
REPO="path/to/repo"

# Edit the mapping rules for the relevant resource types.
vi ${REPO}/mappings/fhirVersionConversion/mappings/r3r4/Person.wstl

# If your mappings vary from the sample mappings, edit the output for the unit tests to your expected values.
# You can also edit the input files to test specific input resources.
vi ${REPO}/mappings/fhirVersionConversion/unit_tests/r3r4/Patient_r3r4.fhir.input.json
vi ${REPO}/mappings/fhirVersionConversion/unit_tests/r3r4/Patient_r3r4.fhir.output.json

# Navigate to the quickrun directory, and run the unit tests
cd quickrun
gradle run --args="-m ${REPO}/mappings/fhirVersionConversion/unit_tests/unit_test.wstl"
```

Passing tests will have output like the following:

``` {.no-copy}
TEST test_R3PatientToR4
```
Failing tests will provide a diff of the received and expected output:

```{.no-copy}
TEST test_R3PatientToR4 FAIL
  com.google.common.base.VerifyException: -want, +got
    -X +Y
```