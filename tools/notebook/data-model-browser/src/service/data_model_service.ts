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

import {URLExt} from '@jupyterlab/coreutils';
import {ServerConnection} from '@jupyterlab/services';

/** The container for a data model schema. */
export interface DataModel {
  // tslint:disable-next-line:no-any - the raw JSON schema.
  schema: any;
}

/** The container for all of the registered data model schema. */
export interface DataModels {
  dataModels: DataModel[];
}

const SERVICE_ROUTE = 'datamodels/v1/list';
const SERVICE_REQUEST_METHOD = 'GET';

/** Service that wraps API calls to Jupyterlab server extension. */
export class DataModelService {
  async listModels(): Promise<DataModels> {
    return new Promise((resolve, reject) => {
      const serverSettings = ServerConnection.makeSettings();
      const requestUrl = URLExt.join(serverSettings.baseUrl, SERVICE_ROUTE);
      const requestInit: RequestInit = {method: SERVICE_REQUEST_METHOD};
      ServerConnection.makeRequest(requestUrl, requestInit, serverSettings)
          .then((response) => {
            response.json().then((content) => {
              if (content.error) {
                console.error(content.error);
                reject(content.error);
                return [];
              }
              resolve({
                // tslint:disable-next-line:no-any - the raw JSON schema.
                dataModels: content.map((dm: any) => {
                  return {schema: dm};
                })
              });
            });
          });
    });
  }
}
