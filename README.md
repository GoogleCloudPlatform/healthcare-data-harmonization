# Google HCLS Data Harmonization

## Summary

This is an engine that converts data of one structure to another, based on a
configuration file which describes how.

The configuration file can be written in either the native
[protobuf](https://developers.google.com/protocol-buffers/docs/overview) format
or a condensed
[Data Harmonization Mapping Language](https://github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_language)
which is transpiled to protobuf configs for you.

The engine accepts data in JSON format and outputs it in JSON format. For
information on the mapping configuration, look at the protobuf files in the
proto directory.

## Building and Testing

### TL;DR

Run `build_all.sh`

### Details

This project consists of two components, the mapping engine and the mapping
language, both of which can be built and tested independently with `go build`
and `go test`. Please refer to their individual READMEs for more information.

### Language Reference

TODO: Add language reference.

### Codelab

Please refer to the
[Data Harmonization Mapping Language Codelab](https://github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_language/docs/codelab.md)
for instructions on how to run the mapping engine and for getting familiar with
the mapping language.

## License

Apache License, Version 2.0
