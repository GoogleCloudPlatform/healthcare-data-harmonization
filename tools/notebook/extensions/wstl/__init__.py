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
"""Whistle Mapping Language magics package."""
__version__ = '0.0.1'

from google3.third_party.cloud_healthcare_data_harmonization.tools.notebook.extensions.wstl.magics.wstl import LoadHL7Magics
from google3.third_party.cloud_healthcare_data_harmonization.tools.notebook.extensions.wstl.magics.wstl import WSTLMagics


def load_ipython_extension(ipython):
  """Module function necessary to load magic in iPython kernel.

    This function is required so that the magic comman can be loaded loaded
    via `%load_ext module.path` or be configured to be autoloaded by IPython
    at startup time.

  Args:
    ipython: running ipython instance
  """
  ipython.register_magics(WSTLMagics)
  ipython.register_magics(LoadHL7Magics)
