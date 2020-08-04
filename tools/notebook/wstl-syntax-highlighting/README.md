# wstl-syntax-highlighting

Extension to support syntax highlighting for whistle data transformation
language.

## Prerequisites

*   Python 3.5+
*   [JupyterLab](https://jupyterlab.readthedocs.io/en/stable/getting_started/installation.html)
*   [Virtualenv](https://virtualenv.pypa.io/en/latest/) (Recommended for local
    development)
*   [NPM](https://nodejs.org/en/) (For local development)

## Install

```bash
jupyter labextension install wstl-syntax-highlighting
```

## Development

The `jlpm` command is JupyterLab's pinned version of
[yarn](https://yarnpkg.com/) that is installed with JupyterLab. You may use
`yarn` or `npm` in lieu of `jlpm` below.

### Dependencies

```bash
# Install dependencies (only needs to be done once or when dependencies change).
jlpm
# Build Typescript source
jlpm build
# Link your development version of the extension with JupyterLab
jupyter labextension link .
```

### On going development

```bash
# Rebuild Typescript source after making changes and rebuild JupyterLab
jlpm build && jupyter lab build
```

You can watch the source directory and run JupyterLab in watch mode to watch for
changes in the extension's source and automatically rebuild the extension and
application.

```bash
# Watch the source directory in another terminal tab
jlpm watch
# Run jupyterlab in watch mode in one terminal tab
jupyter lab --watch
```

### Uninstall

```bash

jupyter labextension uninstall wstl-syntax-highlighting
```
