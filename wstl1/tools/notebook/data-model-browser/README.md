# Data Model Browser Extension

JupyterLab extension project

## Prerequisites

*   Python 3.5+
*   [JupyterLab](https://jupyterlab.readthedocs.io/en/stable/getting_started/installation.html)
*   [Virtualenv](https://virtualenv.pypa.io/en/latest/) (Recommended for local
    development)
*   [NPM](https://nodejs.org/en/) (For local development)

## Development

For a development install (requires npm version 4 or later), do the following in
the repository directory:

You will need to have Python3, virtualenv, and npm installed.

```bash
# Create a Python 3 virtualenv and install jupyterlab and the project in edit mode
virtualenv -p python3 venv
source venv/bin/activate

# Install the server extension
pip install .

# Install the npm package and the extension
npm install
jupyter labextension install .

# Now, run npm start which starts the Typescript compiler in watch mode on the
# extension directory as well as the JupyterLab server
npm start
```
