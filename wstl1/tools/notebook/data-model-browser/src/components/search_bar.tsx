/**
 * @jsx React.createElement
 * @jsxFrag React.Fragment
 */
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
import {stylesheet} from 'typestyle';
import classnames from 'classnames';

// tslint:disable-next-line:ban-malformed-import-paths
import Fuse from 'fuse.js';

import Autocomplete from '@material-ui/lab/Autocomplete';
import {AutocompleteChangeReason} from '@material-ui/lab/Autocomplete';
import Grid from '@material-ui/core/Grid';
import TextField from '@material-ui/core/TextField';
import Typography from '@material-ui/core/Typography';

import {DataModel} from '../service/data_model_service';

const localStyles = stylesheet({
  searchContainer: {
    margin: 'auto 16px',
  },
  input: {
    width: '100%',
  },
});

/**
 * The indexed field.
 */
interface SearchableField {
  /**
   * The path, from resource root, to the field.
   */
  path: string;

  /**
   * The name of the field.
   */
  name: string;

  /**
   * The description of the field.
   */
  description: string;
}

/**
 * Properties for SchemaSearch component.
 */
interface Props {
  /**
   * Callback function invoked when a search result is selected. The callback
   * is invoked with the JSON path of the selected field.
   */
  onSearchResultSelected: (path: string) => void;

  /**
   * The data model that will be indexed and searched on the client.
   */
  dataModel: DataModel;
}

/**
 * The state maintained by the search component.
 */
interface State {
  /**
   * The search text query.
   */
  searchText: string;

  /**
   * Results matching the search text.
   */
  activeResults: SearchableField[];

  /**
   * The search index for the data model.
   */
  // tslint:disable-next-line:no-any
  searchIndex: any;
}

/**
 * Data model schema search component.
 */
// tslint:disable-next-line:enforce-name-casing React component.
export class SearchBar extends React.PureComponent<Props, State> {
  state: State = {
    activeResults: [],
    searchText: '',
    searchIndex: null,
  };

  constructor(props: Props) {
    super(props);
    const indexOptions = {keys: ['name', 'defintion']};
    this.state.searchIndex = Fuse.createIndex(
      indexOptions.keys,
      this.getSearchableDefinitions()
    );
  }

  render() {
    return (
      <div className={localStyles.searchContainer}>
        <Autocomplete
          freeSolo
          id="search-search"
          getOptionLabel={(result: SearchableField) => result.path}
          getOptionSelected={(
            result: SearchableField,
            value: SearchableField
          ) => result.path === value.path}
          filterOptions={x => x}
          options={this.state.activeResults}
          onChange={(
            event: object,
            result: unknown,
            reason: AutocompleteChangeReason
          ) => {
            if (reason === 'select-option') {
              const searchableField = result as SearchableField;
              this.handleRowClick(searchableField.path);
            } else if (reason === 'clear') {
              this.clearSearch();
            }
          }}
          onInputChange={(event: object, newValue: string, reason: string) => {
            if (reason === 'input') {
              this.setState({searchText: newValue});
              this.doSearch(newValue);
            }
          }}
          classes={{root: classnames(localStyles.input)}}
          renderInput={params => (
            <TextField
              {...params}
              label="Search data model"
              variant="outlined"
              fullWidth
            />
          )}
          renderOption={(option: SearchableField) => {
            return (
              <Grid container alignItems="center">
                <Grid item xs>
                  <Typography
                    key={option.path}
                    variant="body2"
                    color="textPrimary"
                  >
                    {option.path}
                  </Typography>
                  <Typography
                    key={option.path + '1'}
                    variant="body2"
                    color="textSecondary"
                  >
                    {option.description}
                  </Typography>
                </Grid>
              </Grid>
            );
          }}
        />
      </div>
    );
  }

  private handleRowClick(path: string) {
    this.props.onSearchResultSelected(path);
  }

  private doSearch(query: string) {
    const fuseOptions = {
      keys: ['name', 'description'],
      caseSensitive: false,
      threshold: 0.7,
      shouldSort: true,
      includeScore: true,
      location: 0,
      distance: 100,
      minMatchCharLength: 3,
      maxPatternLength: 32,
    };
    const fuse = new Fuse(
      this.getSearchableDefinitions(),
      fuseOptions,
      this.state.searchIndex
    );
    const results = fuse.search(query);

    // tslint:disable-next-line:no-any
    const activeResults = results.map((result: any) => {
      return result.item as SearchableField;
    });
    this.setState({activeResults});
  }

  private clearSearch() {
    this.setState({searchText: ''});
    this.props.onSearchResultSelected('');
  }

  private getSearchableDefinitions(): SearchableField[] {
    const results: SearchableField[] = [];
    Object.keys(this.props.dataModel.schema.discriminator.mapping).forEach(
      entity => {
        if (!entity.startsWith('_')) {
          results.push(this.extractTopEntityMeta(entity));
          const definition = this.props.dataModel.schema.definitions[entity];
          if (!!definition && 'properties' in definition) {
            Object.keys(definition.properties).forEach(property => {
              results.push(this.getSubEntityMeta(entity, property));
            });
          }
        }
      }
    );
    return results;
  }

  private extractTopEntityMeta(name: string): SearchableField {
    return {
      path: name,
      name,
      description: this.props.dataModel.schema.definitions[name].description,
    };
  }

  private getSubEntityMeta(
    topEntityName: string,
    subEntityName: string
  ): SearchableField | null {
    const definition = this.props.dataModel.schema.definitions[topEntityName];
    if (
      !('properties' in definition) ||
      !definition.properties ||
      !definition.properties[subEntityName]
    ) {
      return null;
    }
    const subEntity = definition.properties[subEntityName];
    let type = subEntity.type;
    // A property might be non primitive type. In such case, a reference to the
    // type definition is used.
    // For example: fhir > Account > coverage has the following ref:
    // $ref: '#/definitions/Account_Coverage',
    if (!type && subEntity.$ref) {
      type = subEntity.$ref.slice(subEntity.$ref.lastIndexOf('/') + 1);
    }
    return {
      path: `${topEntityName} > ${subEntityName}`,
      name: subEntityName,
      description: subEntity.description,
    };
  }
}
