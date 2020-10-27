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

FROM openjdk:11-jdk-slim AS java-build-env

USER root

# set up installation tools
RUN apt-get update && apt-get install -yq --no-install-recommends \
    wget \
    bzip2 \
    ca-certificates \
    sudo \
    locales \
    fonts-liberation \
    gcc

RUN echo "en_US.UTF-8 UTF-8" > /etc/locale.gen && \
    locale-gen

RUN apt-get update && apt-get install -y git \
    python-dev \
    unzip \
    nano \
    curl \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

RUN wget https://golang.org/dl/go1.14.6.linux-amd64.tar.gz && \
    tar -C /usr/local -xzf go1.14.6.linux-amd64.tar.gz && \
    rm -f go1.14.6.linux-amd64.tar.gz

ENV PATH $PATH:/usr/local/go/bin

ENV GO_USER="jovyan"
RUN useradd -ms /bin/bash $GO_USER
ENV HOME=/home/$GO_USER
ENV GOPATH=$HOME
ENV GOBIN=$GOPATH/bin
ENV PATH $GOBIN:$PATH

ARG WSTL_INSTALL_DIR="/etc/wstl"
RUN mkdir -p $WSTL_INSTALL_DIR && \
    chown $GO_USER $WSTL_INSTALL_DIR

USER $GO_USER
RUN mkdir -p $WSTL_INSTALL_DIR/bin && \
    mkdir -p $WSTL_INSTALL_DIR/tools/notebook

COPY --chown=$GO_USER ./mapping_engine $WSTL_INSTALL_DIR/mapping_engine
COPY --chown=$GO_USER ./mapping_language $WSTL_INSTALL_DIR/mapping_language
COPY --chown=$GO_USER ./tools/notebook/extensions/ $WSTL_INSTALL_DIR/tools/notebook/extensions/

WORKDIR $WSTL_INSTALL_DIR/tools/notebook/extensions/wstl/
RUN chmod +x ./build_go.sh && \
    ./build_go.sh && \
    cp $WSTL_INSTALL_DIR/tools/notebook/extensions/wstl/service/main/main $WSTL_INSTALL_DIR/bin && \
    rm -rf $WSTL_INSTALL_DIR/mapping_engine && \
    rm -rf $WSTL_INSTALL_DIR/mapping_language && \
    rm -rf $WSTL_INSTALL_DIR/tools

# Now copy it into our base image.
FROM gcr.io/distroless/base-debian10
COPY --from=java-build-env /etc/wstl/* /etc/wstl/bin/
WORKDIR "/home/jovyan"
CMD ["/etc/wstl/bin/main",  "-host=0.0.0.0"]
