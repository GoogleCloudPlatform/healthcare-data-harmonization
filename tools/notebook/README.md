# Cloud Healthcare Data Harmonization Tooling

## Summary

This repo will contain extensions to JupyterLab that enable rapid authoring,
testing, and debugging of data harmonization whistle scripts.

# Jupyter Notebook Cloud Healthcare Data Harmonization Stack

`gcr.io/cloud-healthcare-containers/cloud-healthcare-data-harmonization-notebook:latest`
includes extensions and packages to facilitate healthcare data harmonization and
`gcr.io/cloud-healthcare-containers/cloud-healthcare-data-harmonization-services:latest`
is the mapping engine service that performs the transformations. The features
include:

*   **FHIR stu3 data model browser** - provides a mechanism to browse resource
    structural documentation for [FHIR stu3](http://hl7.org/fhir/STU3/)
    resources.
    ![Demo](https://storage.googleapis.com/data-harmonization-sample-data/jupyterlab-data-model-browser-demo-sm.gif)

*   **load_hl7v2_datastore** - the line magic command allows you to load data
    directly from a
    [HL7v2Store](https://cloud.google.com/healthcare/docs/reference/rest/v1/projects.locations.datasets.hl7V2Stores/list)
    and store the results either in a python variable or persist them to disk.
    optional arguments:

    *   --project_id PROJECT_ID - ID of the GCP project that the HL7v2 Store
        belongs to.
    *   --region REGION - Region of the HL7v2 Store.
    *   --dataset_id DATASET_ID - ID of the dataset that the HL7v2 store belongs
        to.
    *   --hl7v2_store_id HL7V2_STORE_ID - ID of the HL7v2 store to load data
        from.
    *   --api_version <{v1,v1beta1}> - The version of the healthcare api to
        call. Default to v1.
    *   --filter FILTER - filter restricts messages returned to those matching a
        filter. See the
        [Healthcare API documentation](https://cloud.google.com/healthcare/docs/reference/rest/v1beta1/projects.locations.datasets.hl7V2Stores.messages/list#query-parameters)
        for additional details.
    *   --dest_file_name DEST_FILE_NAME - The destination file path to store the
        loaded data. If not provided, the result will be directly returned to
        the IPython kernel.

*   **load_hl7v2_gcs** - the line magic command loads JSON data from a
    [Google Cloud Storage](https://cloud.google.com/storage) bucket and allows
    the user to either save the results to a python variable, persist them to
    disk, or print the results.

    *   --bucket_name BUCKET_NAME - The name of the GCS bucket to load data
        from.
    *   --source_blob_name SOURCE_BLOB_NAME - The name of the blob to load.
    *   --dest_file_name DEST_FILE_NAME - The destination file path to store the
        loaded data. If not provided, the result will be directly returned to
        the IPython kernel.

*   **wstl** - the cell magic to evaluate whistle mapping language from iPython
    kernel. optional arguments:

    *   --input INPUT - The input. Supports the following prefix notations:
        py://<name_of_python_variable> json://<inline_json_object_or_array> :
        python inline dict and list expressions are supported. e.g.
        json://{"field":"value"} or
        json://[{"first":"value"},{"second":"value"}]
        file://<path_to_local_file_system> , supports glob wildcard expressions
        and will only load .json or .ndjson file extensions. Each json
        object/list defined within an ndjson will be a separate input to the
        mapping.
    *   --library_config LIBRARY_CONFIG - Path to the directory where the
        library mapping files are located.
    *   --code_config CODE_CONFIG - Path to the directory of
        [FHIR ConceptMaps](https://www.hl7.org/fhir/conceptmap.html) used for
        code harmonization.
    *   --unit_config UNIT_CONFIG - Path to a unit harmonization file
        (textproto).
    *   --output OUTPUT - Name of python variable to store result.

    ![Demo](https://storage.googleapis.com/data-harmonization-sample-data/jupyterlab-wstl-demo-sm.gif)

*   [Google Cloud Storage browser extension](https://github.com/gclouduniverse/jupyterlab_gcsfilebrowser)

*   Includes everything in the
    [jupyter/minimal-notebook](https://jupyter-docker-stacks.readthedocs.io/en/latest/using/selecting.html#jupyter-minimal-notebook)
    and its ancestor images

*   Includes support for all of the
    [Jupyter notebook built-in magic commands](https://ipython.readthedocs.io/en/stable/interactive/magics.html).

*   Includes the following python packages pre-installed:

    *   [Google API Python Client](https://github.com/googleapis/google-api-python-client)
    *   [Google Cloud Storage Python Client](https://github.com/googleapis/python-storage)
    *   [Pandas](https://pandas.pydata.org/)

### Prerequisites

*   Install [docker](https://docs.docker.com/get-docker/) on your local file
    system. Note that you want to run docker without sudo, you need to follow
    the
    [post-installation steps](https://docs.docker.com/engine/install/linux-postinstall/)
    on Linux.

*   Install [docker compose](https://docs.docker.com/compose/install/) on your
    local file system.

## Running the image

*   Create a
    [service account, and corresponding key,](https://cloud.google.com/iam/docs/creating-managing-service-account-keys#creating_service_account_keys)
    that will be used within the notebook to read/write data from Google Cloud
    Storage and Cloud Healthcare API HL7v2Store. Add the following roles to the
    service account.

    *   Storage Admin - ability to list, read, and write GCS resources.
    *   Healthcare HL7v2 Message Editor - read/write to the HL7v2 Store.

*   Download and store the service account key in a directory on your file
    system, and set the $NOTEBOOK_SERVICE_ACCOUNT_DIR environment to the
    directory that contains the key and see the
    $NOTEBOOK_SERVICE_ACCOUNT_FILENAME environment variable to the name of the
    service account key.

*   Create a working directory on your file system to store all notebooks. This
    directory will be mounted and used by the Jupyter.

*   Set the following environment variables so that the docker images are able
    to access the mapping configurations, all notebooks are persisted to your
    local file system, and the appropriate service account key is used to access
    GCP. If you would like the environment variables to persist across terminal
    session,
    [docker-compose supports reading from an environment file](https://docs.docker.com/compose/env-file/).

```bash
cd $DH_ROOT # where $DH_ROOT is the root of the git repository.
export NOTEBOOK_UID=$UID
export NOTEBOOK_WORKING_DIR="$PWD" # where $PWD is the root of the git repository.
export NOTEBOOK_FUNCTION_LIBRARY_DIR="$PWD/mapping_configs/hl7v2_fhir_stu3"
export NOTEBOOK_SERVICE_ACCOUNT_FILENAME="<replace_with_service_account_json>"
export NOTEBOOK_SERVICE_ACCOUNT_DIR="<replace_with_folder_containing_service_account_json>"
export GOOGLE_APPLICATION_CREDENTIALS="$NOTEBOOK_SERVICE_ACCOUNT_DIR/$NOTEBOOK_SERVICE_ACCOUNT_FILENAME"
tee .env << EOF
NOTEBOOK_UID=$NOTEBOOK_UID
NOTEBOOK_WORKING_DIR=$NOTEBOOK_WORKING_DIR
NOTEBOOK_FUNCTION_LIBRARY_DIR=$NOTEBOOK_FUNCTION_LIBRARY_DIR
NOTEBOOK_SERVICE_ACCOUNT_FILENAME=$NOTEBOOK_SERVICE_ACCOUNT_FILENAME
NOTEBOOK_SERVICE_ACCOUNT_DIR=$NOTEBOOK_SERVICE_ACCOUNT_DIR
GOOGLE_APPLICATION_CREDENTIALS=$GOOGLE_APPLICATION_CREDENTIALS
EOF

docker-compose pull
docker-compose up --no-build
```

*   In browser go to http://localhost:8888/lab
*   If you prefer the container runs on a different port set the NOTEBOOK_PORT
    environment variable to your desired port number.

```bash
tee -a .env << EOF
NOTEBOOK_PORT=10000
EOF
docker-compose up --no-build
```

*   Open the [example notebook](examples/demo-sample.ipynb) from the file
    browser within JupyterLab.

## Building the image

*   Execute the command below to build the image.

```bash
docker-compose up --build
```

## Additional Information

*   [Jupyter Docker Stacks on ReadTheDocs](http://jupyter-docker-stacks.readthedocs.io/en/latest/index.html)
