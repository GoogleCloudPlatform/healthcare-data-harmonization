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

FROM gcr.io/cloud-healthcare-containers/cloud-healthcare-data-harmonization-services:latest as wstl-service-env

FROM gcr.io/deeplearning-platform-release/base-cpu:latest AS aip-build-env

# Install base dependencies and upgrade vulnerable libraries
RUN pip install \
    jupyterlab-gcloud-auth==0.2.4 \
    python-language-server==0.36.1 \
    "urllib3>=2.6.3" \
    "h11>=0.16" \
    "aiohttp>=3.13.3" \
    "cryptography>=46.0.5" \
    "gitpython>=3.1.50" \
    "pillow>=12.2" \
    "pyjwt>=2.12" \
    "pyopenssl>=26" \
    "tornado>=6.5.5" \
    "ray>=2.52"


ARG WSTL_INSTALL_DIR="/opt/google/wstl"
RUN mkdir -p $WSTL_INSTALL_DIR

# wstl syntax highlighting extension
COPY ./wstl-syntax-highlighting $WSTL_INSTALL_DIR/wstl-syntax-highlighting/
WORKDIR $WSTL_INSTALL_DIR/wstl-syntax-highlighting/
RUN jlpm install && \
    jlpm build && \
    jupyter labextension install . --no-build && \
    rm -rf node_modules

# wstl language server extension
COPY ./wstl-language-server $WSTL_INSTALL_DIR/wstl-language-server/
WORKDIR $WSTL_INSTALL_DIR/wstl-language-server/
RUN pip install jupyterlab-lsp==3.10.1 && \
    npm install && \
    npm run-script compile && \
    npm run-script gen-lsp-client-spec

# wstl magic commands
COPY ./extensions $WSTL_INSTALL_DIR/magics/wstl-extensions
WORKDIR $WSTL_INSTALL_DIR/magics/wstl-extensions
RUN ./build.sh

# data model browser extension and building JupyterLab
COPY ./data-model-browser $WSTL_INSTALL_DIR/data-model-browser/
WORKDIR $WSTL_INSTALL_DIR/data-model-browser/
RUN python setup.py sdist && \
    npm install --legacy-peer-deps && \
    npm run-script build && \
    jupyter labextension install . --no-build && \
    pip install \
    $WSTL_INSTALL_DIR/data-model-browser/dist/data_model_browser-0.0.1.tar.gz \
    $WSTL_INSTALL_DIR/magics/wstl-extensions/dist/wstl_extensions-0.0.1.tar.gz && \
    jupyter lab build --dev-build=False --minimize=True && \
    rm -rf /opt/google/wstl/wstl-syntax-highlighting \
           /opt/google/wstl/data-model-browser \
           /opt/google/wstl/magics && \
    rm -rf /opt/python/3.10/share/jupyter/lab/staging/node_modules && \
    rm -rf /usr/local/share/.cache/yarn && \
    rm -rf /root/.cache && \
    rm -rf /root/.npm

COPY --from=wstl-service-env /etc/wstl/bin/main /usr/local/bin/wstl-service
COPY run_services.sh /run_services.sh
RUN chmod +x /run_services.sh
CMD ["/run_services.sh"]

WORKDIR /home/jupyter

# Trigger rebuild for CVE-2025-69204 (imagemagick)
