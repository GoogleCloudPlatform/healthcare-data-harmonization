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
"""Wheel definition for Whistle magic commands."""
import setuptools

setuptools.setup(
    name="wstl-extensions",
    version="0.0.1",
    author="Googler",
    author_email="noreply@google.com",
    description="Whistle Magic Commands",
    long_description="Various magic commands to simplify Whistle based data transformations.",
    long_description_content_type="text/markdown",
    url="https://github.com/GoogleCloudPlatform/healthcare-data-harmonization.git",
    packages=setuptools.find_packages(),
    classifiers=[
        "Programming Language :: Python :: 3",
        "License :: OSI Approved :: Apache License",
        "Operating System :: OS Independent",
    ],
    python_requires=">=3.5",
    install_requires=[
        "absl-py>=0.9.0", "ipython>=7.0.1", "google-api-python-client>=1.9.2",
        "google-auth>=1.16.1", "google-cloud-storage>=1.28.1", "grpcio>=1.29.0",
        "grpcio-tools>=1.29.0", "grpcio-status>=1.29.0"
    ])
