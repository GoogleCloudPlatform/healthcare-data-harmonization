# Google HCLS Data Harmonization Mapping Configs

## Summary

This directory contains sample configurations for mapping between various
healthcare standards, along with some sample messages that exercise the
mappings.

## Structure

Every sample directory is composed of different files and directories, namely:

*   projector_library: sets of helper functions that are used in the mapping
*   code_harmonization:
    [FHIR ConceptMaps](https://www.hl7.org/fhir/conceptmap.html) in JSON format
    that are used for
    [Code Harmonization](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_language/docs/reference/index.md#code-harmonization)
*   configurations
    *   main.dhml: the mapping configuration
    *   main.textproto: the
        [Data Harmonization configuration](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_engine/proto/data_harmonization.proto).
        This configuration is composed of the mapping config,
        library configs and the different types of harmonization configs.
    *   units.textproto:
        [Unit Harmonization](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_engine/proto/unit_config.proto)
        configuration
*   test cases in the form:
    *   TEST_NAME.input.json: the input file used in the test
    *   TEST_NAME.config.dhml: the mapping config file used in the test
    *   TEST_NAME.output.json: the output file produced by the test

## Running

Set the following environment variables:

*   `$MAPPING_ENGINE_HOME` to the root of the github repo.
*   `$CONVERSION` to the mapping folder you want to use. E.g. "hl7v2_fhir_stu3"
*   `$INPUT_FILE` to a sample input. E.g. "adt_a01.hl7.fhir.input.json"

There are two ways of running mappings, namely by using the Data Harmonization
config, or by specifying the individual configuration separately. In both cases,
the output from the mapping engine is written to the terminal.

See the
[language reference](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_language/docs/reference/index.md#running-your-mappings)
for more details and other flags that are supported.

### Using individual configs

```
go run $MAPPING_ENGINE_HOME/mapping_engine/main:main --
--input_file_spec=$MAPPING_ENGINE_HOME/mapping_configs/$CONVERSION/$INPUT_FILE
--mapping_file_spec=$MAPPING_ENGINE_HOME/mapping_configs/$CONVERSION/configurations/main.dhml
--lib_dir_spec=$MAPPING_ENGINE_HOME/mapping_configs/$CONVERSION/projector_library/
--harmonize_code_dir_spec=$MAPPING_ENGINE_HOME/mapping_configs/$CONVERSION/code_harmonization/
```

### Using Data Harmonization config

```
go run $MAPPING_ENGINE_HOME/mapping_engine/main:main --
--input_file_spec=$MAPPING_ENGINE_HOME/mapping_configs/$CONVERSION/$INPUT_FILE
--data_harmonization_config_file_spec=$MAPPING_ENGINE_HOME/mapping_configs/$CONVERSION/configurations/main.textproto
```
