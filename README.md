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

## Overview

This repository is organized into several packages that together enable you to
author Whistle configs, extend existing mapping configurations, and test configs
within a Jupyter notebook environment.

*   [Mapping Engine](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_engine)
*   [Mapping Language](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_language)
*   [Mapping Configs](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_configs)
*   [Jupyter Notebook](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/tools/notebook)

## Getting Started

We highly recommend that you start by setting up your
[Jupyter Notebook](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/tools/notebook)
environment using the published docker images and executing the
[example notebook](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/tools/notebook/examples/demo-sample.ipynb).
Once setup, work through the
[Whistle Data Transformation Language Codelab](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_language/doc/codelab.md)
to get yourself familiar with Whistle. As you author more Whistle configs, use
the
[Whistle Data Transformation Language Reference](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_language/doc/reference.md)
to deepen your understanding of the language.

### Details

This project consists of three components, the mapping engine, the mapping
language, and Jupyter notebook UI extensions and magic commands. If you want to
build the mapping engine and mapping language packages, run the `./build_all.sh`
script. This command will build and run the tests of the above packages. In
addition, there are a set of JupyterLab UI extensions and magic commands that
simplify the authoring workflow. The extensions are packaged into a set of
pre-built and published docker images that contain and Jupyter notebook
extensions/magic commands and does not require you to build the mapping engine
and mapping library packages. For more details about each package, please refer
to their individual READMEs for more information.

### Language Reference

A language reference is available:
[Whistle Data Transformation Language Reference](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_language/doc/reference.md)

### Codelab

Please refer to the
[Whistle Data Transformation Language Codelab](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_language/doc/codelab.md)
for instructions on how to run the mapping engine and for getting familiar with
the mapping language.

### Sample pipelines

Whistle configs can be executed in [Apache Beam](https://beam.apache.org/).
Please refer to the
[Whistle Dataflow Pipelines Repo](https://github.com/GoogleCloudPlatform/healthcare-data-harmonization-dataflow)
for sample pipelines.

### Feedback

Want to help the Google Cloud Healthcare and Life Sciences team improve Whistle?
Please email: whistle-feedback@google.com to connect with the Whistle team for a
further discussion on your experience with Whistle.

## License

Apache License, Version 2.0
