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
"""Tests for wstl.magics.wstl."""

import json
from unittest from unittest import mock

from absl.testing import absltest
from fakefs import fake_filesystem
from googleapiclient.http import HttpError
import grpc
import grpc_testing
from IPython.core import error
from IPython.display import JSON
from IPython.terminal import interactiveshell
from IPython.testing import tools

from google.cloud import storage
from google3.google.rpc import code_pb2
from google3.google.rpc import status_pb2
from wstl.magics import wstl
from wstl.proto import wstlservice_pb2
from wstl.proto import wstlservice_pb2_grpc


# pylint: disable=invalid-name
class WstlTest(absltest.TestCase):

  # TODO: look into x86_64-grtev4-linux-gnu-driver_is_not_gcc error
  # when merging with test cases from load_hl7
  def setUp(self):
    super(WstlTest, self).setUp()
    self.config = tools.default_config()
    self.config.TerminalInteractiveShell.simple_prompt = True
    self.shell = interactiveshell.TerminalInteractiveShell.instance(
        config=self.config)
    self._time = grpc_testing.strict_real_time()
    self._channel = grpc_testing.channel(
        wstlservice_pb2.DESCRIPTOR.services_by_name.values(), self._time)
    self.sample_hl7v2 = json.dumps("""
    {'ADT_A01': {'ACC': None,
      'AL1': [{'0': 'AL1',
        '1': '0',
        '2': {'1': 'AA'},
        '3': {'1': 'Z88.0',
           '2': 'Personal history of allergy to penicillin',
           '3': 'ZAL'},
        '4': {'1': 'SEVERE'},
        '5': ['Shortness of breath'],
        '6': None}],
      'ARV_1': None,
      'ARV_2': None,
      'DB1': None,
      'DRG': None}}""")

  def test_wstl_magic_is_correctly_defined(self):
    with self.shell.builtin_trap:
      ip = self.shell.get_ipython()
      failure = ip.magics_manager.register(wstl.WSTLMagics)
      self.assertIsNone(failure)
      magic = ip.find_cell_magic("wstl")
      self.assertIsNotNone(magic)
      failure = ip.magics_manager.register(wstl.LoadHL7Magics)
      self.assertIsNone(failure)
      magic = ip.find_line_magic("load_hl7v2_datastore")
      self.assertIsNotNone(magic)
      magic = ip.find_line_magic("load_hl7v2_gcs")
      self.assertIsNotNone(magic)

  # TODO : add additional unit tests using mock gRPC server.
  def test_wstl_magic_invoke_no_connection(self):
    with self.shell.builtin_trap:
      ip = self.shell.get_ipython()
      failure = ip.magics_manager.register(wstl.WSTLMagics)
      self.assertIsNone(failure)
      # No mock server connection has been established so the magic command
      # should raise an exception.
      with self.assertRaises(NotImplementedError):
        ip.run_cell_magic("wstl", "", "TopLevelField: $ToUpper(\"a\")")

  @mock.patch.object(wstl, "_get_message_from_hl7v2_store", autospec=True)
  def test_load_hl7v2_from_datastore_success(self, mocked_client):
    with self.shell.builtin_trap:
      mocked_client.return_value = "some hl7v2 message"
      # TODO  investigate get_ipython returns null issue
      ip = self.shell.get_ipython()
      failure = ip.magics_manager.register(wstl.LoadHL7Magics)
      self.assertIsNone(failure)
      result = ip.run_line_magic(
          "load_hl7v2_datastore", """--project_id=project --region=us
          --dataset_id=ds --hl7v2_store_id=store""")
      self.assertEqual(result.data, mocked_client.return_value)

  @mock.patch.object(wstl, "_get_message_from_hl7v2_store", autospec=True)
  def test_load_hl7v2_from_datastore_failure(self, mocked_client):
    with self.shell.builtin_trap:
      mocked_client.side_effect = HttpError(
          mock.Mock(status=403), bytes("permission denied", "utf-8"))
      ip = self.shell.get_ipython()
      failure = ip.magics_manager.register(wstl.LoadHL7Magics)
      self.assertIsNone(failure)
      with self.assertRaises(HttpError):
        ip.run_line_magic(
            "load_hl7v2_datastore", """--project_id=project --region=us
                          --dataset_id=dsi --hl7v2_store_id=store""")

  @mock.patch.object(storage, "Client", autospec=True)
  @mock.patch.object(storage, "Blob", autospec=True)
  def test_load_hl7v2_from_gcs_success_direct_return(self, mock_blob,
                                                     mock_client):
    with self.shell.builtin_trap:
      mock_blob.download_as_string.return_value = self.sample_hl7v2
      mock_blob.content_encoding = None
      mock_client.return_value.bucket.return_value.get_blob.return_value = mock_blob
      ip = self.shell.get_ipython()
      failure = ip.magics_manager.register(wstl.LoadHL7Magics)
      self.assertIsNone(failure)
      result = ip.run_line_magic(
          "load_hl7v2_gcs", """--bucket_name=foo
          --source_blob_name=bar""")
      self.assertEqual(result.data, json.loads(self.sample_hl7v2))

  @mock.patch.object(storage, "Client", autospec=True)
  @mock.patch.object(storage, "Blob", autospec=True)
  def test_load_hl7v2_from_gcs_success_output_file_create(
      self, mock_blob, mock_client):
    with self.shell.builtin_trap:
      mock_blob.download_as_string.return_value = self.sample_hl7v2
      mock_blob.content_encoding = None
      mock_client.return_value.bucket.return_value.get_blob.return_value = mock_blob
      ip = self.shell.get_ipython()
      failure = ip.magics_manager.register(wstl.LoadHL7Magics)
      self.assertIsNone(failure)
      with mock.patch("builtins.open", autospec=True) as mock_open:
        ip.run_line_magic(
            "load_hl7v2_gcs", """--bucket_name=foo
          --source_blob_name=bar --dest_file_name=some_file.txt""")
      mock_open.assert_called_once_with("some_file.txt", "w")

  @mock.patch.object(storage, "Client", autospec=True)
  @mock.patch.object(storage, "Blob", autospec=True)
  def test_load_hl7v2_from_gcs_success_output_file_content(
      self, mock_blob, mock_client):
    with self.shell.builtin_trap:
      fs = fake_filesystem.FakeFilesystem()
      fake_open = fake_filesystem.FakeFileOpen(fs)
      mock_blob.download_as_string.return_value = self.sample_hl7v2
      mock_blob.content_encoding = None
      mock_client.return_value.bucket.return_value.get_blob.return_value = mock_blob
      ip = self.shell.get_ipython()
      failure = ip.magics_manager.register(wstl.LoadHL7Magics)
      self.assertIsNone(failure)

      with mock.patch.multiple(wstl, open=fake_open):
        tmp_filename = "dummy.txt"
        ip.run_line_magic(
            "load_hl7v2_gcs", """--bucket_name=foo
          --source_blob_name=bar --dest_file_name={}""".format(tmp_filename))
        self.assertEqual(
            fs.GetObject(tmp_filename).contents.decode("UTF-8"),
            self.sample_hl7v2)

  @mock.patch.object(storage, "Bucket", autospec=True)
  @mock.patch.object(storage, "Client", autospec=True)
  def test_load_hl7v2_from_gcs_not_found(self, mock_client, mock_bucket):
    with self.shell.builtin_trap:
      mock_bucket.exists.return_value = False
      mock_client.return_value.bucket.return_value = mock_bucket
      ip = self.shell.get_ipython()
      failure = ip.magics_manager.register(wstl.LoadHL7Magics)
      self.assertIsNone(failure)
      with self.assertRaises(ValueError):
        ip.run_line_magic("load_hl7v2_gcs",
                          """--bucket_name=foo --source_blob_name=bar""")

  @mock.patch.object(storage, "Client", autospec=True)
  @mock.patch.object(storage, "Blob", autospec=True)
  def test_load_hl7v2_from_gcs_wrong_data(self, mock_blob, mock_client):
    with self.shell.builtin_trap:
      mock_blob.download_as_string.return_value = "some invalid json".encode(
          "UTF-8")
      mock_blob.content_encoding = None
      mock_client.return_value.bucket.return_value.get_blob.return_value = mock_blob
      ip = self.shell.get_ipython()
      failure = ip.magics_manager.register(wstl.LoadHL7Magics)
      self.assertIsNone(failure)
      with self.assertRaises(json.JSONDecodeError):
        ip.run_line_magic("load_hl7v2_gcs",
                          """--bucket_name=foo --source_blob_name=bar""")

  # BEGIN GOOGLE_INTERNAL
  def test_fhir_validate_magic_is_correctly_defined(self):
    ip = self.shell.get_ipython()
    failure = ip.magics_manager.register(wstl.WSTLMagics)
    self.assertIsNone(failure)
    magic = ip.find_line_magic("fhir_validate")
    self.assertIsNotNone(magic)
    # we cannot test object identity because decorators return wrapped versions.
    self.assertEqual(wstl.__name__, magic.__module__)

  @mock.patch.object(grpc, "insecure_channel", autospec=True)
  @mock.patch.object(wstlservice_pb2_grpc, "WhistleServiceStub", autospec=True)
  def test_fhir_validate_magic_inline_json(self, mock_stub, mock_channel):

    class DummyChannel:

      def __init__(self, channel):
        self.channel = channel

      def __enter__(self):
        return self.channel

      def __exit__(self, exc_type, exc_val, exc_tb):
        self.channel._close()
        return False

    class DummyService:

      def __init__(self, res):
        self.resp = res

      def FhirValidate(self, req):
        del req
        return self.resp

    mock_channel.return_value = DummyChannel(self._channel)
    ip = self.shell.get_ipython()
    failure = ip.magics_manager.register(wstl.WSTLMagics)
    self.assertIsNone(failure)
    lines = [
        "--version=stu3 --input=json://{'id':'example','resourceType':" +
        "'Device','udi':{'carrierHRF':'test'}}",
        "--version=stu3 --input=json://{'id':'example','resourceType':" +
        "'3','udi':{'carrierHRF':'test'}}"
    ]
    results = []
    resps = [
        wstlservice_pb2.ValidationResponse(status=[
            status_pb2.Status(code=code_pb2.OK, message="Validation Success")
        ]),
        wstlservice_pb2.ValidationResponse(status=[
            status_pb2.Status(
                code=code_pb2.INVALID_ARGUMENT, message="invalid FHIR resource")
        ])
    ]
    reqs = [
        wstlservice_pb2.ValidationRequest(
            fhir_version=wstlservice_pb2.ValidationRequest.FhirVersion.STU3,
            input=[
                wstlservice_pb2.Location(
                    inline_json="{'id':'example','resourceType':" +
                    "'Device','udi':{'carrierHRF':'test'}}")
            ]),
        wstlservice_pb2.ValidationRequest(
            fhir_version=wstlservice_pb2.ValidationRequest.FhirVersion.STU3,
            input=[
                wstlservice_pb2.Location(
                    inline_json="{'id':'example','resourceType':" +
                    "'3','udi':{'carrierHRF':'test'}}")
            ])
    ]
    for i in range(len(lines)):
      mock_service = mock.create_autospec(DummyService)
      mock_service.FhirValidate.return_value = resps[i]
      mock_stub.return_value = mock_service
      result = ip.run_line_magic("fhir_validate", lines[i])
      results.append(result)
      mock_service.FhirValidate.assert_called_once_with(reqs[i])
    wants = [
        "{'status': [{'message': 'Validation Success'}]}",
        "{'status': [{'code': 3, 'message': 'invalid FHIR resource'}]}"
    ]
    for j in range(len(wants)):
      result = results[j]
      want = JSON(wants[j])
      self.assertEqual(
          result.data,
          want.data,
          msg="JSON.data mismatch on input {}".format(lines[j]))
      self.assertEqual(
          result.url,
          want.url,
          msg="JSON.url mismatch on input {}".format(lines[j]))
      self.assertEqual(
          result.filename,
          want.filename,
          msg="JSON.filename mismatch on input {}".format(lines[j]))

  @mock.patch.object(grpc, "insecure_channel", autospec=True)
  @mock.patch.object(wstlservice_pb2_grpc, "WhistleServiceStub", autospec=True)
  def test_fhir_validate_magic_ipython(self, mock_stub, mock_channel):

    class DummyChannel:

      def __init__(self, channel):
        self.channel = channel

      def __enter__(self):
        return self.channel

      def __exit__(self, exc_type, exc_val, exc_tb):
        self.channel._close()
        return False

    class DummyService:

      def __init__(self, res):
        self.resp = res

      def FhirValidate(self, req):
        del req
        return self.resp

    mock_channel.return_value = DummyChannel(self._channel)
    ip = self.shell.get_ipython()
    failure = ip.magics_manager.register(wstl.WSTLMagics)
    self.assertIsNone(failure)
    st1 = "{'id':'example','resourceType':'Device','udi':{'carrierHRF':'test'}}"
    st2 = "{'id':'example','resourceType':'3','udi':{'carrierHRF':'test'}}"
    ip.push("st1")
    ip.push("st2")
    lines = [
        "--version=stu3 --input=py://st1", "--version=stu3 --input=py://st2"
    ]
    results = []
    resps = [
        wstlservice_pb2.ValidationResponse(status=[
            status_pb2.Status(code=code_pb2.OK, message="Validation Success")
        ]),
        wstlservice_pb2.ValidationResponse(status=[
            status_pb2.Status(
                code=code_pb2.INVALID_ARGUMENT, message="invalid FHIR resource")
        ])
    ]
    reqs = [
        wstlservice_pb2.ValidationRequest(
            fhir_version=wstlservice_pb2.ValidationRequest.FhirVersion.STU3,
            input=[
                wstlservice_pb2.Location(
                    inline_json="{'id':'example','resourceType':" +
                    "'Device','udi':{'carrierHRF':'test'}}")
            ]),
        wstlservice_pb2.ValidationRequest(
            fhir_version=wstlservice_pb2.ValidationRequest.FhirVersion.STU3,
            input=[
                wstlservice_pb2.Location(
                    inline_json="{'id':'example','resourceType':" +
                    "'3','udi':{'carrierHRF':'test'}}")
            ])
    ]
    for i in range(len(lines)):
      mock_service = mock.create_autospec(DummyService)
      mock_service.FhirValidate.return_value = resps[i]
      mock_stub.return_value = mock_service
      result = ip.run_line_magic("fhir_validate", lines[i])
      results.append(result)
      mock_service.FhirValidate.assert_called_once_with(reqs[i])

    wants = [
        "{'status': [{'message': 'Validation Success'}]}",
        "{'status': [{'code': 3, 'message': 'invalid FHIR resource'}]}"
    ]
    for j in range(len(wants)):
      result = results[j]
      want = JSON(wants[j])
      self.assertEqual(
          result.data,
          want.data,
          msg="JSON.data mismatch on input {}".format(lines[j]))
      self.assertEqual(
          result.url,
          want.url,
          msg="JSON.url mismatch on input {}".format(lines[j]))
      self.assertEqual(
          result.filename,
          want.filename,
          msg="JSON.filename mismatch on input {}".format(lines[j]))
    # Delete st1 and st2 to suppress the unused-variable linter warning.
    del st1
    del st2

  @mock.patch.object(grpc, "insecure_channel", autospec=True)
  @mock.patch.object(wstlservice_pb2_grpc, "WhistleServiceStub", autospec=True)
  @mock.patch.object(storage, "Client", autospec=True)
  @mock.patch.object(storage, "Bucket", autospec=True)
  def test_fhir_validate_magic_gcs(self, mock_bucket, mock_client, mock_stub,
                                   mock_channel):

    class DummyChannel:

      def __init__(self, channel):
        self.channel = channel

      def __enter__(self):
        return self.channel

      def __exit__(self, exc_type, exc_val, exc_tb):
        self.channel._close()
        return False

    class DummyService:

      def __init__(self, res):
        self.resp = res

      def FhirValidate(self, req):
        del req
        raise grpc.RpcError(code_pb2.UNIMPLEMENTED,
                            "GCS source not implemented yet")

    class Item(object):

      def __init__(self, bucket, name):
        self.bucket = bucket
        self.name = name

    class FakeBucket(object):

      def __init__(self, bucket_name):
        self.name = bucket_name

    mock_channel.return_value = DummyChannel(self._channel)
    bucket = FakeBucket("dummy_bucket")
    items = [Item(bucket, "file.wstl")]

    mock_bucket.list_blobs.return_value = items
    mock_client.return_value.get_bucket.return_value = mock_bucket
    ip = self.shell.get_ipython()
    failure = ip.magics_manager.register(wstl.WSTLMagics)
    self.assertIsNone(failure)
    with mock.patch.object(DummyService, "FhirValidate") as mock_method:
      mock_stub.return_value = DummyService(None)
      mock_method.side_effect = grpc.RpcError
      result = ip.run_line_magic(
          "fhir_validate", "--version=stu3 --input=gs://dummy_bucket/file.wstl")
      self.assertIsInstance(result, grpc.RpcError)
      req_gs = wstlservice_pb2.ValidationRequest(
          fhir_version=wstlservice_pb2.ValidationRequest.FhirVersion.STU3,
          input=[
              wstlservice_pb2.Location(
                  gcs_location="gs://dummy_bucket/file.wstl")
          ])
      mock_method.assert_called_once_with(req_gs)

  @mock.patch.object(grpc, "insecure_channel", autospec=True)
  @mock.patch.object(wstlservice_pb2_grpc, "WhistleServiceStub", autospec=True)
  @mock.patch.object(storage, "Client", autospec=True)
  @mock.patch.object(storage, "Bucket", autospec=True)
  def test_fhir_validate_magic_gcs_wildcard(self, mock_bucket, mock_client,
                                            mock_stub, mock_channel):

    class DummyChannel:

      def __init__(self, channel):
        self.channel = channel

      def __enter__(self):
        return self.channel

      def __exit__(self, exc_type, exc_val, exc_tb):
        self.channel._close()
        return False

    class DummyService:

      def __init__(self):
        pass

      def FhirValidate(self, req):
        del req
        raise grpc.RpcError(code_pb2.UNIMPLEMENTED,
                            "GCS source not implemented yet")

    class Item(object):

      def __init__(self, bucket, name):
        self.bucket = bucket
        self.name = name

    class FakeBucket(object):

      def __init__(self, bucket_name):
        self.name = bucket_name

    mock_channel.return_value = DummyChannel(self._channel)
    bucket = FakeBucket("dummy_bucket")
    items = [
        Item(bucket, "file1.txt"),
        Item(bucket, "lib_folder/file2.wstl"),
        Item(bucket, "lib_folder/file3.txt"),
        Item(bucket, "lib_folder/file4.json"),
        Item(bucket, "input.json")
    ]
    mock_bucket.list_blobs.return_value = iter(items)
    mock_client.return_value.get_bucket.return_value = mock_bucket

    ip = self.shell.get_ipython()
    failure = ip.magics_manager.register(wstl.WSTLMagics)
    self.assertIsNone(failure)
    with mock.patch.object(DummyService, "FhirValidate") as mock_method:
      mock_stub.return_value = DummyService()
      mock_method.side_effect = grpc.RpcError
      result = ip.run_line_magic(
          "fhir_validate", "--version=stu3 --input=gs://dummy_bucket/*.txt")
      self.assertIsInstance(result, grpc.RpcError)
      req_gs = wstlservice_pb2.ValidationRequest(
          fhir_version=wstlservice_pb2.ValidationRequest.FhirVersion.STU3,
          input=[
              wstlservice_pb2.Location(
                  gcs_location="gs://dummy_bucket/file1.txt"),
              wstlservice_pb2.Location(
                  gcs_location="gs://dummy_bucket/lib_folder/file3.txt")
          ])
      mock_method.assert_called_once_with(req_gs)

  @mock.patch.object(grpc, "insecure_channel", autospec=True)
  def test_fhir_validate_magic_invalid_input(self, mock_channel):

    class DummyChannel:

      def __init__(self, channel):
        self.channel = channel

      def __enter__(self):
        return self.channel

      def __exit__(self, exc_type, exc_val, exc_tb):
        self.channel._close()
        return False

    mock_channel.return_value = DummyChannel(self._channel)
    ip = self.shell.get_ipython()
    failure = ip.magics_manager.register(wstl.WSTLMagics)
    self.assertIsNone(failure)
    lines = [
        "--version=r4 --input=json://{'id':'example','resourceType':" +
        "'Device','udi':{'carrierHRF':'test'}}",
        "--version=stu3 --input={'id':'example','resourceType':" +
        "'Device','udi':{'carrierHRF':'test'}}",
        "--version=STU3 --input={'id':'example','resourceType':" +
        "'Device','udi':{'carrierHRF':'test'}}"
    ]
    errors = [error.UsageError, ValueError, error.UsageError]
    for i in range(len(lines)):
      self.assertRaises(errors[i], ip.run_line_magic, "fhir_validate", lines[i])
  # END GOOGLE_INTERNAL


if __name__ == "__main__":
  absltest.main()
