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

#
"""Request handler classes for the extensions."""

import json
import os

from notebook.base.handlers import APIHandler
from notebook.base.handlers import app_log
from tornado import gen

_DIR_PATH = os.path.dirname(os.path.realpath(__file__))
_DATA_MODEL_FILENAME = os.path.join(_DIR_PATH, 'data', 'fhir_schema_stu3.json')


def load_data():
  with open(_DATA_MODEL_FILENAME, 'r') as f:
    return [json.load(f)]


class DataModelHandler(APIHandler):
  """Handles requests for Data Models."""

  @gen.coroutine
  def get(self, *args, **kwargs):
    try:
      self.finish(json.dumps(load_data()))
    except Exception as e:  # pylint: disable=broad-except
      app_log.exception(str(e))
      self.set_status(500, str(e))
      self.finish({'error': {'message': str(e)}})
