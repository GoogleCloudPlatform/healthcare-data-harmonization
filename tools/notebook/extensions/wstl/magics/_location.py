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
"""Utility functions for Location protobuf messages."""

from google3.third_party.cloud_healthcare_data_harmonization.tools.notebook.extensions.wstl.magics import _constants
from google3.third_party.cloud_healthcare_data_harmonization.tools.notebook.extensions.wstl.magics import _parser
from google3.third_party.cloud_healthcare_data_harmonization.tools.notebook.extensions.wstl.proto import wstlservice_pb2


def parse_location(shell, input_wstl_arg, file_ext=None):
  r"""Parses the argument and returns a Location protobuf message.

  Input arguments with the following prefixes are supported:
  * json://{\"hello\": \"world\"} - using python dict and list notation to
  define a json object or list.
  * file://<file_path> - path to file on local file system.
  * gs://<gcs_path> - path to file on Google Cloud Storage.
  * py://<name_of_python_variable> - name of python variable instantiated within
  session.

  Args:
    shell: ipython interactive shell.
    input_wstl_arg: wstl magic command input argument.
    file_ext: list of valid file extensions. Either `_constants.JSON_FILE_EXT`
    or `WSTL_FILE_EXT`.

  Returns:
    A Location protobuf message.

  Raises:
    ValueError: An unknown location prefix or the python variable can not be
    encoded into json.
  """
  (inputs, gcs_paths) = _parser.parse_object(shell, input_wstl_arg, file_ext)
  if input_wstl_arg.startswith(_constants.JSON_ARG_PREFIX) or \
      input_wstl_arg.startswith(_constants.PYTHON_ARG_PREFIX) or \
      input_wstl_arg.startswith(_constants.FILE_ARG_PREFIX):
    if file_ext == _constants.WSTL_FILE_EXT:
      return [
          wstlservice_pb2.Location(local_path=inline_json)
          for inline_json in inputs
      ]
    else:
      return [
          wstlservice_pb2.Location(inline_json=inline_json)
          for inline_json in inputs
      ]
  elif input_wstl_arg.startswith(_constants.GS_ARG_PREFIX):
    return [
        wstlservice_pb2.Location(gcs_location=gcs_path)
        for gcs_path in gcs_paths
    ]
  else:
    raise ValueError("Missing {} supported prefix".format(",".join([
        _constants.JSON_ARG_PREFIX, _constants.GS_ARG_PREFIX,
        _constants.PYTHON_ARG_PREFIX
    ])))
