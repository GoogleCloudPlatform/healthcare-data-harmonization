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
ARG WSTL_INSTALL_DIR="/etc/wstl"

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

RUN mkdir -p $WSTL_INSTALL_DIR && \
    chown $NB_USER:$NB_GID $WSTL_INSTALL_DIR

USER $NB_UID
RUN mkdir -p /home/$NB_USER/.config/gcloud

# wstl syntax highlighting extension
COPY --chown=$NB_UID ./wstl-syntax-highlighting $WSTL_INSTALL_DIR/wstl-syntax-highlighting/

WORKDIR $WSTL_INSTALL_DIR/wstl-syntax-highlighting/
RUN jlpm && \
    jlpm build && \
    jupyter labextension link .

# wstl magic commands
COPY --chown=$NB_UID ./extensions $WSTL_INSTALL_DIR/magics/wstl-extensions
WORKDIR $WSTL_INSTALL_DIR/magics/wstl-extensions
RUN ./build.sh && \
    pip install dist/wstl-extensions-0.0.1.tar.gz

# data model browser extension
COPY --chown=$NB_UID ./data-model-browser $WSTL_INSTALL_DIR/data-model-browser/

WORKDIR $WSTL_INSTALL_DIR/data-model-browser/
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

WORKDIR $HOME
RUN jupyter lab build -y
