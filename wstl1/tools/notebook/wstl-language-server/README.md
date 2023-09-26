# Whistle Language Server README

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

*   We recommend using a conda env for the installation.
    [Conda](https://docs.conda.io/projects/conda/en/latest/user-guide/install/) >=
    v4.9.1 is recommended.

*   In order for the Jupyter Notebook to recognize the Whistle files, install
    the
    [wstl-syntax-highlighting](https://github.com/GoogleCloudPlatform/healthcare-data-harmonization/tree/master/tools/notebook/wstl-syntax-highlighting)
    extension.

*   If you intend to run the Whistle Language Server on Jupyterlab, please
    install the following dependencies:

    *   If JupyterLab and JupyterLab-LSP are already installed, make sure the
        version of JupyterLab is >=1.2.0 <2.0.0 and JupyterLab LSP is 0.8.0 in
        order to compile with wstl-syntax-highlighting.

    *   Otherwise, install the [JupyterLab, Jupyter LSP server extension and
        JupyterLab LSP front end
        extension](https://github.com/krassowski/jupyterlab-lsp#installation):

        ```
        conda install -c conda-forge 'jupyterlab=1.2.0' jupyter-lsp
        jupyter labextension install @krassowski/jupyterlab-lsp@0.8.0
        ```

## Installation Instructions

*   To install this language server and the dependencies, run the following
    command.

```
npm install
```

*   To install this language server into the global node module directory using
    the `-g` flag after installing the dependencies.

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

## Integrating with Jupyter Notebook

*   To connect the server with the
    [Jupyter LSP](https://github.com/krassowski/jupyterlab-lsp), generate a
    jupyterlab-lsp
    [spec file](https://github.com/krassowski/jupyterlab-lsp/blob/master/docs/Configuring.ipynb)
    that associates the language server with wstl language server requests on
    the client.

```
npm run-script gen-lsp-client-spec
```

*   You should be able to launch the Jupyter Notebook without extra
    configurations now. Keep reading if you want to understand the spec file and
    the configuration options.

*   You will find the spec file at
    `/usr/local/etc/jupyter/jupyter_notebook_config.json`. It enables the
    Jupyter LSP to send requests to and receive responses from the Whistle
    Language Server.

*   The auto-detection is turned on in the generated spec file. If you wish to
    disable it, change the field `autodetect` to false directly in the spec
    file.

*   If you wish to configure other language servers manually, add more items in
    `language_servers` in parallel with `whistle-language-server`. For more
    details about the spec file, please refer to the documentation of
    [Jupyter LSP](https://github.com/krassowski/jupyterlab-lsp/blob/master/docs/Configuring.ipynb).

## Development Instructions

*   After installing Node.js, ensure you clone this git repository to your local
    filesystem and cd into this directory (e.g `cd
    GIT_REPO/tools/notebook/wstl-language-server`)

*   To build and transpile the language server run the following commands:

```
npm install
```

*   If you are planning on changing the code, use the watch command to transpile
    the TypeScript files on-the-fly as they are modified.

```
npm run-script watch
```

*   This package uses [Jasmine](https://jasmine.github.io/) for unit testing.
    Unit test specs are configured in `./spec/support/jasmine.s`. To start the
    unit test, run `npm test`.
