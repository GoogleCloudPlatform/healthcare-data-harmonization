# Google HCLS Data Harmonization

## Summary

This is an engine that converts data of one structure to another, based on a
configuration file which describes how.

The configuration file can be written in either the native
[protobuf](https://developers.google.com/protocol-buffers/docs/overview) format
or a condensed
[Whistle Data Transformation Language](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_language)
which is transpiled to protobuf configs for you.

The engine accepts data in JSON format and outputs it in JSON format. For
information on the mapping configuration, look at the protobuf files in the
proto directory.

## Building and Testing

### TL;DR

Run `build_all.sh`

### Details

This project consists of three components, the mapping engine, the mapping
language, both of which can be built and tested independently with `go build`
and `go test`, and Jupyter notebook extensions/magic commands. Please refer to
their individual READMEs for more information.

### Language Reference

A language reference is available:
[Whistle Data Transformation Language Reference](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_language/doc/reference.md)

### Codelab

Please refer to the
[Whistle Data Transformation Language Codelab](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_language/doc/codelab.md)
for instructions on how to run the mapping engine and for getting familiar with
the mapping language.

## License

Apache License, Version 2.0
