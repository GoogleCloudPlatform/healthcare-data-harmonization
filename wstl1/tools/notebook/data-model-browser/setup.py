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
"""Wheel definition for data model browser extension."""
from distutils import log
import glob
import json
import os
import setuptools

here = os.path.dirname(os.path.abspath(__file__))
is_repo = os.path.exists(os.path.join(here, '.git'))
tar_path = os.path.join(here, 'data_model_browser*.tgz')
package_json = os.path.join(here, 'package.json')
requirements = os.path.join(here, 'requirements.txt')
schema_json = os.path.join(here, 'data_model_browser', 'data',
                           'fhir_schema_stu3.json')
log.info('Starting setup.py')
log.info('$PATH=%s' % os.environ['PATH'])


def get_data_files():
  """Get the data files for the package."""
  return [('', [
      os.path.relpath(package_json, '.'),
      os.path.relpath(requirements, '.'),
      os.path.relpath(schema_json, '.'),
  ]),
          ('share/jupyter/lab/extensions',
           [os.path.relpath(f, '.') for f in glob.glob(tar_path)]),
          ('etc/jupyter/jupyter_notebook_config.d',
           ['jupyter-config/jupyter_notebook_config.d/data_model_browser.json'])
         ]


version = ''
with open(package_json) as f:
  version = json.load(f)['version']

requires = []
with open(requirements) as f:
  for l in f:
    if not l.startswith('#'):
      requires.append(l.strip())

setup_args = {
    'name':
        'data_model_browser',
    'version':
        version,
    'description':
        'Notebooks Data Model Browser Extension Settings Schema',
    'long_description':
        'Data Model Browser Extension',
    'license':
        'Apache2',
    'include_package_data':
        True,
    'data_files':
        get_data_files(),
    'install_requires':
        requires,
    'packages':
        setuptools.find_packages(),
    'zip_safe':
        False,
    'author':
        'Googler',
    'author_email':
        'noreply@google.com',
    'url':
        'https://github.com/GoogleCloudPlatform/healthcare-data-harmonization.git',
    'keywords': [
        'ipython',
        'jupyter',
        'gcp',
        'google cloud',
    ],
    'classifiers': [
        'Intended Audience :: Developers',
        'Intended Audience :: Science/Research',
        'Intended Audience :: System Administrators',
        'Programming Language :: Python :: 3',
    ],
}

setuptools.setup(**setup_args)
