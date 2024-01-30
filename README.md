# Whistle Data Transformation Language

# Introduction

Whistle is a mapping language used for converting complex, nested data from one
schema to another.

Whistle is a terse, efficient syntax to describe transformations of healthcare
data, but is applicable to any domain.

In addition to the built-in functionality, the engine can be extended with
plugins which can provide native transformations, extra features, integration
with external services, and otherwise extend the engine functionality.

# Prerequisites

1.  [Git](https://git-scm.com/)
1.  [JDK 11.x](https://www.azul.com/downloads/?version=java-11-lts&package=jdk#zulu)
1.  [Gradle 7.x](https://gradle.org/next-steps/?version=7.6&format=bin)

> NOTE Consider using [sdkman](https://sdkman.io/) - a standard platform for
> downloading/installing sdks: `sdk install java 11.0.20-zulu; sdk install
> gradle 7.6`

# Getting Started

1.  Clone and build the repository.

    ```console
    $ git clone https://github.com/GoogleCloudPlatform/healthcare-data-harmonization.git
    $ cd healthcare-data-harmonization
    $ gradle test
    ```

1.  Take a look at the [Getting Started Tutorial](./doc/getting_started.md)

1.  Refer to the [Language Specification](./doc/spec.md) for details

# Currently in this repository

*   Whistle 2 Runtime
    *   `runtime` - Execution engine
    *   `plugins` - First-Party plugins
        *   `example` - Example Plugin showcasing the plugin APIs
        *   `logging` - Simple logging functions implementation
        *   `test` - Unit Testing Plugin enabling writing unit tests in Whistle
        *   `harmonization` - FHIR code translation support
        *   `reconciliation` - Find and merge FHIR resources that refer to the same entity
    *   `proto` - Intermediate representation
    *   `testutil` - Unit testing helper functions
    *   `transpiler` - Syntax to `proto` transpilation

* Tooling
    *   `tools` - Various libraries/modules/IDE plugins
        *   `annotation_processor` - A Java compiler plugin to preserve
            documentation and function signatures.
        *   `docgen` - A Javadoc doclet and gradle plugin to generate plugin
            function documentation, like in `doc/`.
        *   `languageserver` - An [LSP](https://microsoft.github.io/language-server-protocol/)
            implementation for Whistle.
        *   `linter` - A code formatting tool.

* Documentation
    *   `doc` - Guides, codelabs, language spec, and plugin function
        documentation.

## Coming Soon to this repository

In no particular order:

*   Visual Studio Code Extension
*   HL7v2 to FHIR mappings

## Where is Whistle 1?

This repository contains what is technically Whistle 2, which is a from-scratch
rewrite of Whistle. This is now the current and actively maintained version of
Whistle.

The original version of Whistle is still available in the `wstl1` directory of
this repository for legacy purposes, however is not actively maintained, and we
encourage all users to migrate to Whistle 2.
