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

import unittest

from data_model_browser import handlers


class TestList(unittest.TestCase):

  def testList(self):
    want = {
        'metadata': {
            'datamodel': {
                'name': 'fhir',
                'version': 'stu3',
            }
        }
    }
    got = handlers.load_data()
    self.assertEqual(len(got), 1)
    self.assertIn('metadata', got[0])
    self.assertDictEqual(want['metadata'], got[0]['metadata'])


if __name__ == '__main__':
  unittest.main()
