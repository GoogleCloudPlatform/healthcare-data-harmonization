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

*   Install [Node.js](https://nodejs.org/en/download/) onto your system.
    Alternatively you can use the [NVM](https://github.com/nvm-sh/nvm) to
    install a sandbox version of node and npm on your filesystem.

## Development Instructions

*   After installing Node.js, ensure you clone this git repository to your local
    filesystem and cd into this directory (e.g `cd
    GIT_REPO/tools/notebook/wstl-language-server`)

*   To build and transpile the language server run the following commands:

```
npm install
npm run-script compile
```

*   If you are planning on changing the code, use `npm run-script watch` to
    transpile the TypeScript files on-the-fly as they are modified.

*   This package uses [Jasmine](https://jasmine.github.io/) for unit testing.
    Unit test specs are configured in `./spec/support/jasmine.s`. To start the
    unit test, run `npm test`.

## Installation Instructions

*   To install this language server into the global node module directory using
    the `-g` flag.

```
npm install -g
```

*   After the installation has completed, you can start the language server by
    invoking the `wstl-language-server` command. You will need to specify the
    desired communication method through which requests will be sent to the
    language server. All requests must follow the
    [JSON-RPC](https://www.jsonrpc.org/) based protocol defined by the
    [Language Server Protocol Specification](https://microsoft.github.io/language-server-protocol/specifications/specification-current/#baseProtocol)
    for sending and receiving messages between the client and server. The
    available communication mechanisms are:

    *   Send requests by writing to the server's standard input and reading from
        the standard output. Specified using the `--stdio` command line flag.
        `wstl-language-server --stdio`

    *   Use Node.js'
        [child_process interprocess communication](https://nodejs.org/api/child_process.html#child_process_child_process)
        to spawn a server process and to send/receive messages. Specified using
        the `--node-ipc` command line flag. `wstl-language-server --node-ipc`

    *   Use a socket and port to create a TCP network connection to the server
        with which to send and receive messages. Specified using the
        `--socket=<port>` command line flag. `wstl-language-server
        --socket=<port>`
