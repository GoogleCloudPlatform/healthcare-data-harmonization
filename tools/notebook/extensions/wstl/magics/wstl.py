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
"""Magic command wrapper for whistle mapping language."""

import json
import os

from googleapiclient import discovery
import grpc
from IPython.core import magic_arguments
from IPython.core.magic import cell_magic
from IPython.core.magic import line_magic
from IPython.core.magic import Magics
from IPython.core.magic import magics_class
from IPython.display import JSON

from google.cloud import storage
from google.protobuf import json_format
from wstl.magics import _constants
from wstl.magics import _location
from wstl.proto import wstlservice_pb2
from wstl.proto import wstlservice_pb2_grpc

_GRPC_TIMEOUT = os.environ.get("NOTEBOOK_GRPC_TIMEOUT", 10.0)
_DEFAULT_HOST = os.environ.get("NOTEBOOK_GRPC_HOST", "localhost")
_DEFAULT_PORT = os.environ.get("NOTEBOOK_GRPC_PORT", "50051")


@magics_class
class WSTLMagics(Magics):
  """Evaluates whistle mapping language (.wstl) within a cell."""

  def __init__(self, shell):
    super(WSTLMagics, self).__init__(shell)
    self.grpc_target = "{}:{}".format(_DEFAULT_HOST, _DEFAULT_PORT)

  @line_magic("wstl-reset")
  def wstl_reset(self, line):
    """Cell magic to clear all variables and functions from incremental transformation."""
    with grpc.insecure_channel(self.grpc_target) as channel:
      stub = wstlservice_pb2_grpc.WhistleServiceStub(channel)
      session_id = str(self.shell.history_manager.session_number)
      req = wstlservice_pb2.DeleteIncrementalSessionRequest(
          session_id=session_id)
      try:
        resp = stub.DeleteIncrementalSessionRequest(req)
      except grpc.RpcError as rpc_error:
        return rpc_error
      else:
        return JSON(json_format.MessageToDict(resp))

  @magic_arguments.magic_arguments()
  @magic_arguments.argument(
      "--input",
      type=str,
      required=False,
      help="""The input. Supports the following prefix notations:
      py://<name_of_python_variable>
      json://<inline_json_object_or_array> : python inline dict and list
      expressions are supported. e.g. json://{"field":"value"} or
      json://[{"first":"value"},{"second":"value"}]
      file://<path_to_local_file_system> , supports glob wildcard expressions
      and will only load .json or .ndjson file extensions. Each json object/list
      defined within an ndjson will be a separate input to the mapping.
      """)
  @magic_arguments.argument(
      "--library_config",
      type=str,
      required=False,
      help="""Path to the directory where the library mapping files are located."""
  )
  @magic_arguments.argument(
      "--code_config",
      type=str,
      required=False,
      help="""Path to the directory of FHIR ConceptMaps used for code harmonization."""
  )
  @magic_arguments.argument(
      "--unit_config",
      type=str,
      required=False,
      help="""Path to a unit harmonization file (textproto).""")
  @magic_arguments.argument(
      "--output",
      type=str,
      required=False,
      help="""Name of python variable to store result. e.g. --output temp_var"""
  )
  @cell_magic("wstl")
  def wstl(self, line, cell):
    """Cell magic to evaluate whistle mapping language from iPython kernel."""
    args = magic_arguments.parse_argstring(self.wstl, line)

    # TODO (b/157468786): migrate to secure channel.
    with grpc.insecure_channel(self.grpc_target) as channel:
      stub = wstlservice_pb2_grpc.WhistleServiceStub(channel)

      (incremental_session, err) = _get_or_create_session(stub, self.shell)
      if err:
        return err

      (transform,
       err) = _get_incremental_transform(stub, self.shell,
                                         incremental_session.session_id, args,
                                         cell)
      if err:
        return err

      result = _response_to_json(transform)
      if args.output:
        self.shell.push({args.output: result})
      return JSON(result)


@magics_class
class LoadHL7Magics(Magics):
  """Loads parsed HL7v2 message from GCS or HL7v2 Store."""

  @magic_arguments.magic_arguments()
  @magic_arguments.argument(
      "--project_id",
      type=str,
      help="""ID of the GCP project that the HL7v2 Store belongs to.""",
      required=True)
  @magic_arguments.argument(
      "--region",
      type=str,
      help="""Region of the HL7v2 Store.""",
      required=True)
  @magic_arguments.argument(
      "--dataset_id",
      type=str,
      required=True,
      help="""ID of the dataset that the HL7v2 store belongs to.""")
  @magic_arguments.argument(
      "--hl7v2_store_id",
      type=str,
      required=True,
      help="""ID of the HL7v2 store to load data from.""")
  @magic_arguments.argument(
      "--api_version",
      type=str,
      required=False,
      default="v1",
      choices=["v1", "v1beta1"],
      help="""The version of healthcare api to call. Default to v1.""")
  @magic_arguments.argument(
      "--filter",
      type=str,
      required=False,
      help="""
      filter: string, Restricts messages returned to those matching a filter. Syntax:
    https://cloud.google.com/appengine/docs/standard/python/search/query_strings

    If the filter string contains white space, it must be surrounded by single quotes.

    Fields/functions available for filtering are:

    *  `message_type`, from the MSH-9.1 field. For example,
    `NOT message_type = "ADT"`.
    *  `send_date` or `sendDate`, the YYYY-MM-DD date the message was sent in
    the dataset's time_zone, from the MSH-7 segment. For example,
    `send_date < "2017-01-02"`.
    *  `send_time`, the timestamp when the message was sent, using the
    RFC3339 time format for comparisons, from the MSH-7 segment. For example,
    `send_time < "2017-01-02T00:00:00-05:00"`.
    *  `send_facility`, the care center that the message came from, from the
    MSH-4 segment. For example, `send_facility = "ABC"`.
    *  `PatientId(value, type)`, which matches if the message lists a patient
    having an ID of the given value and type in the PID-2, PID-3, or PID-4
    segments. For example, `PatientId("123456", "MRN")`.
    *  `labels.x`, a string value of the label with key `x` as set using the
    Message.labels
    map. For example, `labels."priority"="high"`. The operator `:*` can be used
    to assert the existence of a label. For example, `labels."priority":*`.""")
  @magic_arguments.argument(
      "--dest_file_name",
      type=str,
      required=False,
      help="""
      The destination file path to store the loaded data.
      If not provided, the result will be directly returned to the IPython kernel.
      """,
      default="")
  @line_magic("load_hl7v2_datastore")
  def load_hl7v2_datastore(self, line):
    """Load parsed HL7v2 massage from the HL7v2 Store specified."""
    args = magic_arguments.parse_argstring(self.load_hl7v2_datastore, line)

    hl7v2_messages = _get_message_from_hl7v2_store(args.api_version,
                                                   args.project_id, args.region,
                                                   args.dataset_id,
                                                   args.hl7v2_store_id,
                                                   args.filter)
    if args.dest_file_name:
      with open(args.dest_file_name, "w") as dest_file:
        dest_file.write(hl7v2_messages)
        return "The message was written to {} successfully.".format(
            args.dest_file_name)

    return JSON(hl7v2_messages)

  @magic_arguments.magic_arguments()
  @magic_arguments.argument(
      "--bucket_name",
      type=str,
      help="""The name of the GCS bucket to load data from.""",
      required=True)
  @magic_arguments.argument(
      "--source_blob_name",
      type=str,
      help="""The name of the blob to load.""",
      required=True)
  @magic_arguments.argument(
      "--dest_file_name",
      type=str,
      required=False,
      help="""
      The destination file path to store the loaded data.
      If not provided, the result will be directly returned to the IPython kernel.
      """,
      default="")
  @line_magic("load_hl7v2_gcs")
  def load_hl7v2_gcs(self, line):
    """Load and return parsed HL7v2 massage from the blob in a GCS bucket specified."""
    args = magic_arguments.parse_argstring(self.load_hl7v2_gcs, line)
    dest_file_name = args.dest_file_name

    storage_client = storage.Client()

    bucket = storage_client.bucket(args.bucket_name)
    if not bucket.exists():
      raise ValueError(
          "The bucket does not exist. Please check the provided bucket name.")
    blob = bucket.get_blob(args.source_blob_name)
    if not blob:
      raise ValueError(
          "The blob does not exist. Please check the provided blob name.")
    content = blob.download_as_string()

    if blob and blob.content_encoding:
      content = content.decode(blob.content_encoding)

    # check if the returned content is a json
    try:
      try:
        result = json.loads(content)
      except TypeError:
        result = json.loads(content.decode("UTF-8"))
    except json.JSONDecodeError:
      print(
          "The loaded content is not a valid JSON. Please check the source bucket and blob."
      )
      raise
    if dest_file_name:
      with open(dest_file_name, "w") as dest:
        dest.write(content)
        return "The message was written to {} successfully.".format(
            dest_file_name)

    return JSON(result)


def _get_message_from_hl7v2_store(api_version, project, region, dataset,
                                  data_store, filter_str):
  """Returns an authorized API client by discovering the Healthcare API and creating a service object using the service account credentials in the GOOGLE_APPLICATION_CREDENTIALS environment variable."""
  # TODO(b/158861537): add paging support for HL7v2 messages.
  service_name = "healthcare"

  client = discovery.build(service_name, api_version)
  hl7v2_messages_parent = "projects/{}/locations/{}/datasets/{}".format(
      project, region, dataset)
  hl7v2_message_path = "{}/hl7V2Stores/{}".format(hl7v2_messages_parent,
                                                  data_store)
  if filter_str:
    filter_str = filter_str.strip("'")
  return (
      client.projects().locations().datasets().hl7V2Stores().messages().list(
          parent=hl7v2_message_path, view="FULL",
          filter=filter_str).execute().get("hl7V2Messages", []))


def _get_or_create_session(stub, shell):
  """Retrieves or creates the incremental transform session.

  Args:
    stub: gRPC client stub library.
    shell: an instance of the iPython shell that invoked the magic command.

  Returns:
    IncrementalSessionResponse or a grpc.RcpError
  """
  session_id = shell.history_manager.session_number
  req = wstlservice_pb2.CreateIncrementalSessionRequest(
      session_id=str(session_id))
  try:
    resp = stub.GetOrCreateIncrementalSession(req)
  except grpc.RpcError as rpc_error:
    return None, rpc_error
  else:
    return resp, None


def _get_incremental_transform(stub, shell, session_id, wstl_args, cell):
  """Invokes, throughs a gRPC request, an incremental Whistle transform.

  Args:
    stub: gRPC client stub library.
    shell: an instance of the iPython shell that invoked the magic command.
    session_id: the incremental transformation session id.
    wstl_args: the arguments to the wstl magic command.
    cell: the contents of the cell, containing whistle.

  Returns:
    TransformResponse or a grpc.RpcError
  """
  req = wstlservice_pb2.IncrementalTransformRequest()
  req.session_id = session_id
  req.wstl = cell
  if wstl_args.library_config:
    library_configs = _location.parse_location(
        shell,
        wstl_args.library_config,
        file_ext=_constants.WSTL_FILE_EXT,
        load_contents=False)
    if library_configs:
      req.library_config.extend(library_configs)

  if wstl_args.code_config:
    code_configs = _location.parse_location(
        shell,
        wstl_args.code_config,
        file_ext=_constants.JSON_FILE_EXT,
        load_contents=False)
    if code_configs:
      req.code_config.extend(code_configs)

  if wstl_args.unit_config:
    unit_config = _location.parse_location(
        shell,
        wstl_args.unit_config,
        file_ext=_constants.TEXTPROTO_FILE_EXT,
        load_contents=False)
    if unit_config:
      req.unit_config = unit_config[0]

  if wstl_args.input:
    inputs = _location.parse_location(
        shell,
        wstl_args.input,
        file_ext=_constants.JSON_FILE_EXT,
        load_contents=True)
    if inputs:
      req.input.extend(inputs)
    else:
      return None, "no inputs matching arguement {}".format(wstl_args.input)

  try:
    resp = stub.GetIncrementalTransform(req, timeout=_GRPC_TIMEOUT)
  except grpc.RpcError as rpc_error:
    return None, rpc_error
  else:
    return resp, None


def _convert_message_to_json(transform_record):
  """Converts the output or error of TransformedRecords proto to JSON.

  Args:
    transform_record: a TransformRecords to convert to JSON.

  Returns:
    The JSON representation of the output or error field.
  """
  if transform_record.HasField("output"):
    return json.loads(transform_record.output)
  elif transform_record.HasField("error"):
    return json_format.MessageToDict(transform_record.error)
  else:
    return json_format.MessageToDict(transform_record)


def _response_to_json(response):
  """Converts each element within a TransformResponse result into JSON.

  Args:
    response: the TransformResponse from a GetIncrementalTransform request.

  Returns:
    One or more TransformedRecords contained in the TransformResponse as JSON.
  """
  if len(response.results) > 1:
    return [_convert_message_to_json(result) for result in response.results]
  else:
    return _convert_message_to_json(response.results[0])


def _get_validation(stub, shell, version, input_arg):
  """Validates the input JSON resource(s) against the FHIR version.

  Args:
    stub: gRPC client stub library.
    shell: an instance of the iPython shell that invoked the magic command.
    version: the FHIR version to be used for validation.
    input_arg: the FHIR resource to be validated against the specified version.

  Returns:
    The ValidationResponse containing the validation results of the resources
    or an error.

  """
  req = wstlservice_pb2.ValidationRequest(
      input=_location.parse_location(shell, input_arg))
  if version.lower() == "stu3":
    req.fhir_version = wstlservice_pb2.ValidationRequest.FhirVersion.STU3
  else:
    return None, ValueError("""FHIR version {} is incorrect or not supported,
{} are supported versions""".format(
    version, wstlservice_pb2.ValidationRequest.FhirVersion.keys()))
  try:
    resp = stub.FhirValidate(req)
  except grpc.RpcError as rpc_error:
    return None, rpc_error
  return resp, None
