// Copyright 2020 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// Ensure styles are loaded by webpack
import '../style/index.css';

import {JupyterFrontEnd, JupyterFrontEndPlugin} from '@jupyterlab/application';

import {DataModelViewerWidget} from './components/data_model_viewer_widget';
import {DataModelService} from './service/data_model_service';

async function activateExtension(app: JupyterFrontEnd) {
  const dataModelViewerWidget =
      new DataModelViewerWidget(new DataModelService());
  dataModelViewerWidget.addClass('jp-DataModelBrowser');
  app.shell.add(dataModelViewerWidget, 'left');
}


/**
 * The JupyterLab plugin.
 */
const dataModelBrowserPlugin: JupyterFrontEndPlugin<void> = {
  id: 'data-model-browser',
  requires: [],
  activate: activateExtension,
  autoStart: true
};


// Jupyterlab requires plugins to be exported as default.
// tslint:disable-next-line:no-default-export
export default [
  dataModelBrowserPlugin,
];
