# Copyright 2020 Google LLC.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

ARG BASE_CONTAINER=jupyter/base-notebook:4011ed54844a
FROM $BASE_CONTAINER

USER root

# Install all OS dependencies for fully functional notebook server
RUN apt-get update && apt-get install -yq --no-install-recommends \
    build-essential \
    emacs-nox \
    vim-tiny \
    git \
    inkscape \
    jed \
    libsm6 \
    libxext-dev \
    libxrender1 \
    lmodern \
    netcat \
    python-dev \
    # ---- nbconvert dependencies ----
    texlive-xetex \
    texlive-fonts-recommended \
    texlive-plain-generic \
    # Optional dependency
    texlive-fonts-extra \
    tzdata \
    unzip \
    nano \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

USER $NB_UID

RUN mkdir -p /home/$NB_USER/.config/gcloud

# wstl syntax highlighting extension
COPY --chown=$NB_UID ./wstl-syntax-highlighting /home/$NB_USER/wstl-syntax-highlighting/

WORKDIR /home/$NB_USER/wstl-syntax-highlighting/
RUN jlpm && \
    jlpm build && \
    jupyter labextension link . && \
    jupyter lab build -y

# wstl magic commands
WORKDIR $HOME
COPY --chown=$NB_UID ./extensions /home/$NB_USER/magics/wstl-extensions
WORKDIR /home/$NB_USER/magics/wstl-extensions
RUN ./build.sh && \
    pip install dist/wstl-extensions-0.0.1.tar.gz

# copybara:strip_begin
# Jupytext customizations with wstl support
# TODO : uncomment this block when the jupytext extension
# modifications have been submitted.
# WORKDIR $HOME
# COPY --chown=$NB_UID ../third_party/jupytext /home/$NB_USER/magics/jupytext
# WORKDIR /home/$NB_USER/magics/jupytext
# RUN python setup.py sdist bdist_wheel && \
#     pip install dist/jupytext-1.5.0.tar.gz
# copybara:strip_end

# data model browser extension
COPY --chown=$NB_UID ./data-model-browser /home/$NB_USER/data-model-browser/

WORKDIR /home/$NB_USER/data-model-browser/
RUN python setup.py sdist && \
    pip install dist/data_model_browser-0.0.1.tar.gz && \
    npm install && \
    jupyter labextension install .

# Various third_party extensions
WORKDIR $HOME
RUN pip install jupyterlab-gcloud-auth==0.2.4 \
    google-api-python-client==1.9.2 \
    google-cloud-storage==1.28.1 \
    https://storage.googleapis.com/deeplearning-platform-ui-public/jupyterlab_gcsfilebrowser-0.1.8.tar.gz

WORKDIR $HOME/work
RUN jupyter lab build -y

# Add samples files into image
COPY --chown=$NB_UID ./examples /home/$NB_USER/examples/
