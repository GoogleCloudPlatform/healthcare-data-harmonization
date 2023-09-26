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

// tslint:disable-next-line:enforce-name-casing
import * as React from 'react';
import {ReactWidget, UseSignal} from '@jupyterlab/apputils';
import {Signal} from '@phosphor/signaling';

import {DataModelViewer} from './data_model_viewer';
import {DataModelService} from '../service/data_model_service';

/**
 * Widget to be registered in the left-side panel.
 */
export class DataModelViewerWidget extends ReactWidget {
  id = 'datamodelviewer';
  private visibleSignal = new Signal<DataModelViewerWidget, boolean>(this);

  constructor(private readonly dataModelService: DataModelService) {
    super();
    this.title.iconClass = 'jp-Icon jp-Icon-20 jp-DataModelBrowserIcon';
    this.title.caption = 'Data Model Browser';
  }

  onAfterHide() {
    this.visibleSignal.emit(false);
  }

  onAfterShow() {
    this.visibleSignal.emit(true);
  }

  // tslint:disable:enforce-name-casing
  render() {
    return (
      <UseSignal signal={this.visibleSignal}>
        {(_, isVisible) => (
          <DataModelViewer
            isVisible={isVisible}
            dataModelService={this.dataModelService}
          />
        )}
      </UseSignal>
    );
  }
}
