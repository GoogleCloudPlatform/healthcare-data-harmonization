# Whistle Language Server Protocol README

This is the language server implementing the Language Server Protocol for the
Whistle mapping language. It provides the backend for the Whistle mapping
language on IDEs following the
[Language Server Protocol](https://microsoft.github.io/language-server-protocol/).
The current client in this package compiles with VSCode. To integrate it with
other IDEs, please refer to the requirements for the specific IDE.

## Features

*   Auto-Suggestion: it suggests the
    [Whistle built-in functions](https://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_language/doc/builtins.md)
    preceded by `$`.

## Requirements

*   Please ensure you have npm installed. To check the npm version on your
    machine, run `node -v` and `npm -v`.
*   To install all the dependencies, run `npm install` in the root directory of
    the server.

## Testing

We use [Jasmine](https://jasmine.github.io/) for unit testing of this package.
Unit test specs are configured in ./spec/support/jasmine.s. To start the unit
test, run `npm test`.
