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
# Lint as: python3
"""Tests for google3.third_party.cloud_healthcare_data_harmonization.tools.notebook.extensions.wstl.magics._location."""

import json
from os import path
from absl.testing import absltest

from IPython.testing.globalipapp import get_ipython

import mock
from google.cloud import storage
from google3.third_party.cloud_healthcare_data_harmonization.tools.notebook.extensions.wstl.magics import _constants
from google3.third_party.cloud_healthcare_data_harmonization.tools.notebook.extensions.wstl.magics import _location

_ip = get_ipython()


class LocationTest(absltest.TestCase):

  def test_parse_location_json_prefix_object_success(self):
    shell = mock.MagicMock()
    input_wstl_arg = """json://{"hello":"world"}"""
    locations = _location.parse_location(shell, input_wstl_arg, file_ext=None)
    self.assertLen(locations, 1)
    self.assertTrue(locations[0].HasField("inline_json"))
    self.assertEqual(locations[0].inline_json, "{\"hello\":\"world\"}")

  def test_parse_location_json_prefix_list_success(self):
    shell = mock.MagicMock()
    input_wstl_arg = """json://[{"first": "world"},{"second": "world"}]"""
    locations = _location.parse_location(shell, input_wstl_arg, file_ext=None)
    self.assertLen(locations, 1)
    self.assertTrue(locations[0].HasField("inline_json"))
    self.assertEqual(locations[0].inline_json,
                     """[{"first": "world"},{"second": "world"}]""")

  @mock.patch.object(storage, "Client", autospec=True)
  @mock.patch.object(storage, "Bucket", autospec=True)
  def test_parse_location_gs_prefix_success(self, mock_bucket, mock_client):

    class Item(object):

      def __init__(self, bucket_name, name):
        self.bucket = bucket_name
        self.name = name

    class FakeBucket(object):

      def __init__(self, bucket_name):
        self.name = bucket_name

    bucket = FakeBucket("dummy_bucket")
    items = [
        Item(bucket, "file1.wstl"),
        Item(bucket, "lib_folder/file2.wstl"),
        Item(bucket, "lib_folder/file3.txt"),
        Item(bucket, "input.json")
    ]

    mock_bucket.list_blobs.return_value = iter(items)
    mock_client.return_value.get_bucket.return_value = mock_bucket

    shell = mock.MagicMock()
    input_wstl_arg = "gs://dummy_bucket/input.json"
    locations = _location.parse_location(
        shell, input_wstl_arg, file_ext=_constants.JSON_FILE_EXT)
    self.assertLen(locations, 1)
    self.assertTrue(locations[0].HasField("gcs_location"))
    self.assertEqual(locations[0].gcs_location, input_wstl_arg)

  @mock.patch.object(storage, "Client", autospec=True)
  @mock.patch.object(storage, "Bucket", autospec=True)
  def test_parse_location_gs_prefix_wildcard_success(self, mock_bucket,
                                                     mock_client):

    class Item(object):

      def __init__(self, bucket, name):
        self.bucket = bucket
        self.name = name

    class FakeBucket(object):

      def __init__(self, bucket_name):
        self.name = bucket_name

    bucket = FakeBucket("dummy_bucket")
    items = [
        Item(bucket, "file1.txt"),
        Item(bucket, "lib_folder/file2.wstl"),
        Item(bucket, "lib_folder/file3.wstl"),
        Item(bucket, "lib_folder/file4.json"),
        Item(bucket, "input.json")
    ]
    mock_bucket.list_blobs.return_value = iter(items)
    mock_client.return_value.get_bucket.return_value = mock_bucket

    shell = mock.MagicMock()
    input_wstl_arg = "gs://dummy_bucket/lib_folder/*"
    locations = _location.parse_location(
        shell,
        input_wstl_arg,
        file_ext=_constants.WSTL_FILE_EXT,
        load_contents=False)
    self.assertLen(locations, 2)
    self.assertTrue(locations[0].HasField("gcs_location"))
    self.assertEqual(locations[0].gcs_location,
                     "gs://dummy_bucket/lib_folder/file2.wstl")
    self.assertTrue(locations[1].HasField("gcs_location"))
    self.assertEqual(locations[1].gcs_location,
                     "gs://dummy_bucket/lib_folder/file3.wstl")

  @mock.patch.object(storage, "Client", autospec=True)
  @mock.patch.object(storage, "Bucket", autospec=True)
  def test_parse_location_gs_prefix_wildcard_unsupported_ext(
      self, mock_bucket, mock_client):

    class Item(object):

      def __init__(self, bucket, name):
        self.bucket = bucket
        self.name = name

    class FakeBucket(object):

      def __init__(self, bucket_name):
        self.name = bucket_name

    bucket = FakeBucket("dummy_bucket")
    items = [
        Item(bucket, "file1.txt"),
        Item(bucket, "lib_folder/file2.wstl"),
        Item(bucket, "lib_folder/file3.wstl"),
        Item(bucket, "lib_folder/file4.json"),
        Item(bucket, "input.json")
    ]
    mock_bucket.list_blobs.return_value = iter(items)
    mock_client.return_value.get_bucket.return_value = mock_bucket

    shell = mock.MagicMock()
    input_wstl_arg = "gs://dummy_bucket/*.txt"
    locations = _location.parse_location(
        shell,
        input_wstl_arg,
        file_ext=_constants.WSTL_FILE_EXT,
        load_contents=False)
    self.assertEmpty(locations)

  def test_parse_location_file_prefix_file_exists_success(self):
    shell = mock.MagicMock()
    content = """{"hello": "world"}"""
    tmp_file = self.create_tempfile(
        file_path="dummy.json", content=content, mode="w")
    input_wstl_arg = "file://{}".format(tmp_file.full_path)
    locations = _location.parse_location(
        shell, input_wstl_arg, file_ext=_constants.JSON_FILE_EXT)
    self.assertTrue(locations[0].HasField("inline_json"))
    self.assertEqual(locations[0].inline_json, content)

  def test_parse_location_file_prefix_wstl_suffix_success(self):
    shell = mock.MagicMock()
    content = """Result: $ToUpper("a")"""
    tmp_file = self.create_tempfile(
        file_path="dummy.wstl", content=content, mode="w")
    input_wstl_arg = "file://{}".format(tmp_file.full_path)
    locations = _location.parse_location(
        shell,
        input_wstl_arg,
        file_ext=_constants.WSTL_FILE_EXT,
        load_contents=False)
    self.assertTrue(locations[0].HasField("local_path"))
    self.assertEqual(locations[0].local_path, tmp_file.full_path)

  def test_parse_location_file_prefix_wstl_wildcard_success(self):
    shell = mock.MagicMock()
    content = """Result: $ToUpper("a")"""
    tmp_file = self.create_tempfile(
        file_path="dummy.wstl", content=content, mode="w")
    input_wstl_arg = "file://{}/*".format(path.dirname(tmp_file.full_path))
    locations = _location.parse_location(
        shell,
        input_wstl_arg,
        file_ext=_constants.WSTL_FILE_EXT,
        load_contents=False)
    self.assertLen(locations, 1)
    self.assertTrue(locations[0].HasField("local_path"))
    self.assertEqual(locations[0].local_path, tmp_file.full_path)

  def test_parse_location_file_prefix_wildcard_success(self):
    shell = mock.MagicMock()
    content = """{"hello": "world"}"""
    tmp_file = self.create_tempfile(
        file_path="dummy.json", content=content, mode="w")
    input_wstl_arg = "file://{}/*".format(path.dirname(tmp_file.full_path))
    locations = _location.parse_location(
        shell, input_wstl_arg, file_ext=_constants.JSON_FILE_EXT)
    self.assertLen(locations, 1)
    self.assertTrue(locations[0].HasField("inline_json"))
    self.assertEqual(locations[0].inline_json, content)

  def test_parse_location_file_suffix_ndjson_success(self):
    shell = mock.MagicMock()
    content = """{"first": "item"}\n{"second": "item"}"""
    tmp_file = self.create_tempfile(
        file_path="dummy.ndjson", content=content, mode="w")
    input_wstl_arg = "file://{}".format(tmp_file.full_path)
    locations = _location.parse_location(
        shell, input_wstl_arg, file_ext=_constants.JSON_FILE_EXT)
    self.assertLen(locations, 2)
    self.assertTrue(locations[0].HasField("inline_json"))
    self.assertEqual(locations[0].inline_json, "{\"first\": \"item\"}")
    self.assertTrue(locations[1].HasField("inline_json"))
    self.assertEqual(locations[1].inline_json, "{\"second\": \"item\"}")

  def test_parse_location_file_prefix_textproto_suffix_success(self):
    shell = mock.MagicMock()
    content = """dummy_field: true"""
    tmp_file = self.create_tempfile(
        file_path="dummy.textproto", content=content, mode="w")
    input_wstl_arg = "file://{}".format(tmp_file.full_path)
    locations = _location.parse_location(
        shell,
        input_wstl_arg,
        file_ext=_constants.TEXTPROTO_FILE_EXT,
        load_contents=False)
    self.assertTrue(locations[0].HasField("local_path"))
    self.assertEqual(locations[0].local_path, tmp_file.full_path)

  def test_parse_location_file_prefix_textproto_suffix_load_content_success(
      self):
    shell = mock.MagicMock()
    content = """dummy_field: true"""
    tmp_file = self.create_tempfile(
        file_path="dummy.textproto", content=content, mode="w")
    input_wstl_arg = "file://{}".format(tmp_file.full_path)
    locations = _location.parse_location(
        shell,
        input_wstl_arg,
        file_ext=_constants.TEXTPROTO_FILE_EXT,
        load_contents=True)
    self.assertTrue(locations[0].HasField("inline_json"))
    self.assertEqual(locations[0].inline_json, "dummy_field: true")

  def test_parse_location_file_prefix_no_load_content_success(self):
    shell = mock.MagicMock()
    content = """{"hello": "world"}"""
    tmp_file = self.create_tempfile(
        file_path="dummy.json", content=content, mode="w")
    input_wstl_arg = "file://{}/*".format(path.dirname(tmp_file.full_path))
    locations = _location.parse_location(
        shell,
        input_wstl_arg,
        file_ext=_constants.JSON_FILE_EXT,
        load_contents=False)
    self.assertLen(locations, 1)
    self.assertTrue(locations[0].HasField("local_path"))
    self.assertEqual(locations[0].local_path, tmp_file.full_path)

  def test_parse_location_file_prefix_invalid_path(self):
    shell = mock.MagicMock()
    content = """{"hello": "world"}"""
    tmp_file = self.create_tempfile(content=content, mode="w")
    input_wstl_arg = "file://invalid-{}".format(tmp_file.full_path)
    locations = _location.parse_location(
        shell, input_wstl_arg, file_ext=_constants.JSON_FILE_EXT)
    self.assertEmpty(locations)

  def test_parse_location_file_prefix_missing_extension(self):
    shell = mock.MagicMock()
    input_wstl_arg = "file://placeholder.json"
    with self.assertRaises(ValueError):
      _location.parse_location(shell, input_wstl_arg, file_ext=None)

  def test_parse_location_python_prefix_string_success(self):
    str_content = """{"hello": "world"}"""
    _ip.push("str_content")
    input_wstl_arg = "py://str_content"
    locations = _location.parse_location(_ip, input_wstl_arg, file_ext=None)
    self.assertLen(locations, 1)
    self.assertTrue(locations[0].HasField("inline_json"))
    self.assertEqual(locations[0].inline_json, str_content)

  def test_parse_location_python_prefix_dict_success(self):
    dict_content = {"hello": "world"}
    _ip.push("dict_content")
    input_wstl_arg = "py://dict_content"
    locations = _location.parse_location(_ip, input_wstl_arg, file_ext=None)
    self.assertLen(locations, 1)
    self.assertTrue(locations[0].HasField("inline_json"))
    self.assertEqual(locations[0].inline_json, json.dumps(dict_content))

  def test_parse_location_python_prefix_list_success(self):
    list_content = [{"first": "item"}, {"second": "item"}]
    _ip.push("list_content")
    input_wstl_arg = "py://list_content"
    locations = _location.parse_location(_ip, input_wstl_arg, file_ext=None)
    self.assertLen(locations, 1)
    self.assertTrue(locations[0].HasField("inline_json"))
    self.assertEqual(locations[0].inline_json,
                     json.dumps(list_content, sort_keys=True))

  def test_parse_location_unknown_prefix_failure(self):
    shell = mock.MagicMock()
    input_wstl_arg = "invalid://blah"
    with self.assertRaises(ValueError):
      _location.parse_location(shell, input_wstl_arg, file_ext=None)


if __name__ == "__main__":
  absltest.main()
