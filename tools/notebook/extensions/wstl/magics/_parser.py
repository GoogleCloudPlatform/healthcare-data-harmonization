# Copyright 2020 Google LLC.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""Parser is the helper function for parsing the command on the shim level."""

import fnmatch
import glob
import json
import os

from google.cloud import storage
from google3.third_party.cloud_healthcare_data_harmonization.tools.notebook.extensions.wstl.magics import _constants


def _serialize_to_json(shell_var):
  """Serializes the python variable into a JSON string.

  Args:
    shell_var: ipython shell python object.

  Returns:
    A JSON string.

  Raises:
    ValueError: When serializing a type other than str, dict, or list.
    JSONDecodeError: When unable to encode the variable into JSON.
  """
  if isinstance(shell_var, str):
    return shell_var
  elif isinstance(shell_var, dict) or isinstance(shell_var, list):
    return json.dumps(shell_var)
  else:
    raise ValueError("variable {} is not json decodable".format(shell_var))


def _load_files(path_name, file_ext):
  """Loads files located at path_name.

  Supports glob wildcard expressions.

  Args:
    path_name: the file path, including glob patterns supported by the python
      glob module.
    file_ext: file extensions to be loaded. If .json or .ndjson that contents
      will be loaded and decoded.

  Returns:
    A list of file contents.

  Raises:
    ValueError: The file specified in the wildcard expression does not end with
    an expected extension.
    JSONDecodeError: The file does not contain JSON decodable data.
  """
  if not file_ext:
    raise ValueError("empty required extensions.")

  contents = list()
  for name in glob.glob(path_name):
    _, ext = os.path.splitext(name)
    if ext is None or ext not in file_ext:
      continue
    if os.path.isfile(name):
      with open(name, "r") as f:
        if ext == ".json":
          # decode and encode to verify contents of file are valid JSON.
          content = json.load(f)
          contents.append(json.dumps(content))
        elif ext == ".ndjson":
          json_content = f.readlines()
          if json_content:
            for line in json_content:
              # decode and encode to verify contents of line are valid JSON.
              content = json.loads(line.strip())
              contents.append(json.dumps(content))
        elif ext == ".wstl":
          contents.append(f.name)
        else:
          raise ValueError("invalid file prefix for file {}".format(name))
    elif os.path.isdir(name):
      raise ValueError(
          "use glob expression to specify files in directory {}".format(name))
  return contents


def _list_gcs_bucket_blobs(path_name, file_ext=None):
  """Lists all of the blobs located at the path within the bucket.

  Supports a limited set of leaf folder wildcard expressions using python's
  fnmatch.
  https://docs.python.org/3/library/fnmatch.html#fnmatch.fnmatch

  Args:
    path_name: the gcs path, including glob patterns supported by the python
      glob module.
    file_ext: file extensions supported.

  Returns:
    A list of file contents.

  Raises:
    ValueError: The file specified in the wildcard expression does not end with
    an expected extension.
    `google.cloud.exceptions.NotFound`: If the bucket is not found.
  """
  # normalizing the path by removing redunant separators and up-level
  # references.
  # See https://docs.python.org/3/library/os.path.html#os.path.normpath
  normalize_path = os.path.normpath(path_name)
  bucket_offset = normalize_path.find("/")
  if bucket_offset < 1:
    raise ValueError("Invalid bucket name in path '{}'".format(normalize_path))
  bucket_name = normalize_path[:bucket_offset]
  blob_name = normalize_path[bucket_offset + 1:]
  prefix = os.path.dirname(blob_name)
  # TODO : refactor to pass client down client from magic command.
  client = storage.Client()
  if not client:
    raise ValueError("Unable to create storage client")
  bucket = client.get_bucket(bucket_name)
  blobs = bucket.list_blobs(prefix=prefix)
  blob_names = list()
  for blob in blobs:
    if file_ext:
      _, ext = os.path.splitext(blob.name)
      if ext not in file_ext:
        continue
    if fnmatch.fnmatchcase(blob.name, blob_name):
      blob_names.append("gs://{}/{}".format(blob.bucket.name, blob.name))
  return blob_names


def parse_object(shell, input_object_arg, file_ext=None):
  r"""Parses the argument and returns a tuple that has interpreted information.

  Input arguments with the following prefixes are supported:
  * json://{\"hello\":\"world\"} - a JSON serializable python list, dict.
  * file://<file_path> - path or glob expression to files on local file system.
  * gs://<gcs_path> - path to file on Google Cloud Storage.
  * py://<name_of_python_variable> - name of python variable instantiated within
  session.

  Args:
    shell: ipython interactive shell.
    input_object_arg: magic command input argument.
    file_ext: list of valid file extensions. Either JSON_FILE_EXT or
      WSTL_FILE_EXT

  Returns:
    A tuple of length 2 that has the objects that is interpreted from
    input_object_arg. The tuple must have only one non-empty item. The first
    item is either None or an in-line json, and the second item is either None
    or a gcs path that directs to an in-line json object or whistle.

  Raises:
    ValueError: An unknown location prefix or the python variable can not be
    encoded into json.
  """
  if input_object_arg.startswith(_constants.JSON_ARG_PREFIX):
    offset = len(_constants.JSON_ARG_PREFIX)
    return [input_object_arg[offset:]], None
  elif input_object_arg.startswith(_constants.GS_ARG_PREFIX):
    offset = len(_constants.GS_ARG_PREFIX)
    return None, _list_gcs_bucket_blobs(input_object_arg[offset:], file_ext)
  elif input_object_arg.startswith(_constants.FILE_ARG_PREFIX):
    offset = len(_constants.FILE_ARG_PREFIX)
    return _load_files(input_object_arg[offset:], file_ext), None
  elif input_object_arg.startswith(_constants.PYTHON_ARG_PREFIX):
    offset = len(_constants.PYTHON_ARG_PREFIX)
    var_name = input_object_arg[offset:]
    if var_name not in shell.user_ns:
      raise ValueError("There is no python variable named {}".format(var_name))
    # Only supports json as UTF-8 string not byte array.
    return [_serialize_to_json(shell.user_ns[var_name])], None
  else:
    raise ValueError("Missing {} supported prefix".format(",".join([
        _constants.JSON_ARG_PREFIX, _constants.GS_ARG_PREFIX, \
        _constants.FILE_ARG_PREFIX, _constants.PYTHON_ARG_PREFIX
    ])))
