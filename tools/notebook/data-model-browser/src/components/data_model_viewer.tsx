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

import * as csstips from 'csstips';
// tslint:disable-next-line:enforce-name-casing
import * as React from 'react';
import {stylesheet} from 'typestyle';

import BreadCrumb from '@material-ui/core/Breadcrumbs';
import InputLabel from '@material-ui/core/InputLabel';
import MenuItem from '@material-ui/core/MenuItem';
import Select from '@material-ui/core/Select';
import Typography from '@material-ui/core/Typography';

import {BreadCrumbItem} from './bread_crumb_item';
import {DataModelService, DataModel} from '../service/data_model_service';
import {FieldMetadataViewer} from './field_meta_data_viewer';
import {SearchBar} from './search_bar';
import {SubEntitiesViewer} from './sub_entities_viewer';
import {TopEntitiesViewer} from './top_entities_viewer';
import {SubEntityMeta, TopEntityMeta, SearchResult} from './types';

/**
 * Properites for the DataModelViewer component.
 */
interface Props {
  /**
   * The service that wrappers API requests to the extension's backend handler.
   */
  dataModelService: DataModelService;

  /**
   * A flag indicating whether the components is visible or not.
   */
  isVisible: boolean;
}

/**
 * The state maintained by the DataModelViewer component.
 */
interface State {
  /**
   * The JSON path representing the field selected for inspection which will
   * result in a tool tip being displayed with additional information.
   */
  inspectPath: string;

  /**
   * The search result selected from the drop down list of candidate search
   * results.
   */
  selectedSearchResult: SearchResult;

  /**
   * The path of the field that has been selected to drill down into.
   */
  selectedPath: string[];

  /**
   * The list of available data models.
   */
  dataModels: DataModel[];

  /**
   * The currently active data model.
   */
  activeDataModel: DataModel;

  /**
   * The ID of the active data model.
   */
  activeDataModelId: string;

  /**
   * Flag indicating that the data models have finished loading.
   */
  hasLoaded: boolean;

  /**
   * A cache of the top level entities in the active data model.
   */
  topEntities: TopEntityMeta[];
}

/**
 * The JSON schema definition of an entity.
 */
interface ResourceDefinition {
  /**
   * The name of the resource.
   */
  name: string;

  /**
   * The JSON schema definition of the resource.
   */
  // tslint:disable-next-line:no-any
  definition: any;
}

/**
 * Convenience class for a subset of the properties of a field.
 */
interface FieldProperties {
  /**
   * The field type.
   */
  type: string;
  /**
   * Flag indicating if the field is clickable.
   */
  clickable: boolean;
}

const localStyles = stylesheet({
  header: {
    borderBottom: 'var(--jp-border-width) solid var(--jp-border-color2)',
    fontWeight: 600,
    fontSize: 'var(--jp-ui-font-size0, 11px)',
    letterSpacing: '1px',
    margin: 0,
    padding: '8px 12px',
    textTransform: 'uppercase',
  },
  panel: {
    backgroundColor: 'white',
    height: '100%',
    width: '100%',
    ...csstips.vertical,
  },
  select: {
    margin: 'auto 16px',
  },
  viewer: {
    width: '100%',
    backgroundColor: 'white',
    height: 'calc(100% - 50px)',
    overflowY: 'auto' as 'auto',
  },
  tree: {
    height: 110,
    flexGrow: 1,
    maxWidth: 400,
    overflowY: 'scroll',
    ...csstips.flex,
  },
  table: {
    minWidth: 150,
  },
  button: {
    height: '100%',
  },
  selectLabelRoot: {
    color: 'black',
    fontSize: '1.3rem',
    fontWeight: 500,
  },
  tableName: {
    color: 'black',
    fontSize: '0.9rem',
    fontWeight: 600,
    padding: '15px 2px 5px 2px',
    margin: 'auto 16px',
  },
  selectRoot: {
    minWidth: '10rem',
  },
});

const HEADER_TITLE = 'Data Model Browser Extension';

/**
 * Component for visualizing a data model schema definition.
 */
export class DataModelViewer extends React.Component<Props, State> {
  state: State = {
    inspectPath: '',
    selectedSearchResult: null,
    selectedPath: ['FHIR'],
    dataModels: [],
    activeDataModel: null,
    activeDataModelId: 'FHIR',
    hasLoaded: false,
    topEntities: [],
  };

  constructor(props: Props) {
    super(props);
  }

  async componentDidMount() {
    try {
      this.getDataModels();
    } catch (err) {
      console.warn('Unexpected error', err);
    }
  }

  /**
   * Callback invoked when a BreadCrumbItem is clicked.
   *
   * @param path - the JSON path, spread across a list, from the root of the
   * resource to the selected item.
   */
  onListItemClicked = (path: string[]) => {
    this.setState({
      selectedPath: path,
    });
  };

  /**
   * Callback invoked when either a TopEntity or SubEntity is selected for
   * tool tip inspection.
   *
   * @param path - the JSON path from the root of the resource to the selected
   * item.
   */
  onEntityInspected = (path: string) => {
    this.setState({
      inspectPath: path,
    });
  };

  /**
   * Callback invoked when either a TopEntity or SubEntity is selectd for
   * further drill down.
   *
   * @param name - the name of the entity that was selected.
   */
  onEntitySelected = (name: string) => {
    this.setState({
      selectedPath: this.state.selectedPath.concat([name]),
      inspectPath: '',
    });
  };

  render() {
    if (!this.state.hasLoaded) {
      return (
        <div className={localStyles.panel}>
          <header className={localStyles.header}>
            {HEADER_TITLE}
          </header>
          <Typography color="textPrimary">Loading...</Typography>
        </div>
      );
    }

    if (this.state.selectedPath.length === 0) {
      return (
        <div className={localStyles.panel}>
          <header className={localStyles.header}>
            {HEADER_TITLE}
          </header>
          <Typography color="textPrimary">Missing data model schema</Typography>
        </div>
      );
    } else if (this.state.selectedPath.length === 1) {
      // No resources have been selected so render the resource list.
      return (
        <div className={localStyles.panel}>
          <header className={localStyles.header}>
            {HEADER_TITLE}
          </header>
          <div className={localStyles.tableName}>
            Select version
          </div>
          <InputLabel></InputLabel>
          <Select
            value={this.state.activeDataModelId}
            className={localStyles.select}
            disabled
          >
            <MenuItem value="FHIR">{'FHIR-stu3'}</MenuItem>
          </Select>
          <div className={localStyles.tableName}>
            Search tables and fields
          </div>
          <SearchBar
            dataModel={this.state.activeDataModel}
            onSearchResultSelected={this.onSearchResultSelected}
          />
          <div className={localStyles.tableName}>
            Resource name
          </div>
          <div className={localStyles.viewer}>
            <TopEntitiesViewer
              topEntities={this.state.topEntities}
              onInspect={this.onEntityInspected}
              onSelect={this.onEntitySelected}
              selected={this.state.inspectPath}
            />
          </div>
        </div>
      );
    }

    const previousPaths = this.state.selectedPath.slice(
      0,
      this.state.selectedPath.length - 1
    );
    const activePath = this.state.selectedPath[
      this.state.selectedPath.length - 1
    ];
    let pathOffset = 0;
    return (
      <div className={localStyles.panel}>
        <div className="bread-crumb-wrapper">
          <BreadCrumb maxItems={3} aria-label="breadcrumb">
            {previousPaths.map(previousPath => {
              return (
                <BreadCrumbItem
                  key={previousPath}
                  path={this.state.selectedPath.slice(0, ++pathOffset)}
                  label={previousPath}
                  onClick={this.onListItemClicked}
                />
              );
            })}
            <Typography color="textPrimary">{activePath}</Typography>
          </BreadCrumb>
        </div>
        <div className={localStyles.tableName}>
            Select or hover over a resource for more details
        </div>
        <SearchBar
          dataModel={this.state.activeDataModel}
          onSearchResultSelected={this.onSearchResultSelected}
        />
        <div className={localStyles.viewer}>
          {this.getSelectionDetails(
            this.state.selectedPath.slice(1),
            this.state.activeDataModel.schema
          )}
        </div>
      </div>
    );
  }

  private async getDataModels() {
    try {
      const dm = await this.props.dataModelService.listModels();
      if (dm.dataModels.length === 0) {
        console.warn('Error retrived empty data models list');
        throw new Error('Error retrived empty data models list');
      }
      this.setState({activeDataModel: dm.dataModels[0]});
      const topEntities: TopEntityMeta[] = Object.keys(
        this.state.activeDataModel.schema.discriminator.mapping
      )
        .sort()
        .map(key => {
          return this.extractTopEntityMeta(key);
        });
      this.setState({topEntities});
      this.setState({hasLoaded: true, dataModels: dm.dataModels});
    } catch (err) {
      console.warn('Error retrieving data models', err);
    }
  }

  private onSearchResultSelected = (path: string) => {
    if (!path || path.length === 0) {
      this.setState({selectedSearchResult: null});
      this.setState({selectedPath: [this.state.activeDataModelId]});
    } else {
      const splits = [this.state.activeDataModelId].concat(path.split(' > '));
      this.setState({selectedPath: splits});
      const searchResult: SearchResult = {
        path,
        resource: splits[0],
        field: '',
      };
      if (splits.length > 1) {
        searchResult.field = splits[1];
      }
      this.setState({selectedSearchResult: searchResult});
    }
  };

  private extractTopEntityMeta = (name: string): TopEntityMeta => {
    const description = this.state.activeDataModel.schema.definitions[name]
      .description;
    return {name, description};
  };

  // tslint:disable-next-line:no-any (data from JSON schema)
  private extractReferenceType = (fieldDetails: any): string => {
    return fieldDetails.$ref.slice(fieldDetails.$ref.lastIndexOf('/') + 1);
  };

  // tslint:disable-next-line:no-any (data from JSON schema)
  private resolveFieldProperties = (field: string, fieldDetails: any): FieldProperties => {
    const fieldProp: FieldProperties = {type: '', clickable: false};
    if ('type' in fieldDetails) {
      if ('items' in fieldDetails && '$ref' in fieldDetails.items) {
        fieldProp.type = this.extractReferenceType(fieldDetails.items);
      } else {
        fieldProp.type = fieldDetails.type;
      }
      fieldProp.clickable = true;
    } else if ('const' in fieldDetails) {
      fieldProp.type = 'constant: ' + fieldDetails.const;
      fieldProp.clickable = false;
    } else if ('enum' in fieldDetails) {
      fieldProp.type = 'enum: ' + fieldDetails.enum.join(', ');
      fieldProp.clickable = false;
    } else if ('$ref' in fieldDetails) {
      fieldProp.type = this.extractReferenceType(fieldDetails);
      fieldProp.clickable = true;
    }
    return fieldProp;
  };

  // tslint:disable-next-line:no-any (data from JSON schema)
  private getSubEntityDetails = (name: string, schema: any): SubEntityMeta[] => {
    const subEntities: SubEntityMeta[] = [];
    const definition = schema.definitions[name];
    if (!!definition && 'properties' in definition) {
      for (const [field] of Object.entries(definition.properties).sort()) {
        if (!field.startsWith('_')) {
          let isRequired = false;
          if ('required' in definition) {
            isRequired = definition.required.includes(field);
          }
          const fieldDetails = definition.properties[field];
          const fieldProperty = this.resolveFieldProperties(field, fieldDetails);
          const subEntity: SubEntityMeta = {
            name: field,
            required: isRequired,
            type: fieldProperty.type,
            description: fieldDetails.description,
            schema: fieldDetails,
            clickable: fieldProperty.clickable,
          };
          subEntities.push(subEntity);
        }
      }
    }
    return subEntities;
  };

  private getSelectionDetails = (
    pathParts: string[],
    // tslint:disable-next-line:no-any (data from JSON schema)
    schema: any
  ): React.ReactNode => {
    if (pathParts.length <= 1) {
      const subEntities = this.getSubEntityDetails(pathParts[0], schema);
      return (
        <SubEntitiesViewer
          subEntities={subEntities}
          onInspect={this.onEntityInspected}
          onSelect={this.onEntitySelected}
          selected={this.state.inspectPath}
        />
      );
    }

    let parent: ResourceDefinition = {
      name: pathParts[0],
      definition: schema.definitions[pathParts[0]],
    };
    let i = 1;
    while (parent != null) {
      if (i >= pathParts.length) {
        break;
      }
      const field = pathParts[i++];
      if (
        !('properties' in parent.definition) ||
        !(field in parent.definition.properties)
      ) {
        break;
      }

      const fieldDetails = parent.definition.properties[field];
      parent = null;
      if (!!fieldDetails) {
        const fieldProperty = this.resolveFieldProperties(field, fieldDetails);
        if (fieldProperty.type in schema.definitions) {
          parent = {
            name: fieldProperty.type,
            definition: schema.definitions[fieldProperty.type],
          };
        }
      }
    }

    if (!parent) {
      parent = {
        name: pathParts[i - 1],
        definition: schema.definitions[pathParts[i - 1]],
      };
    }

    if ('properties' in parent.definition) {
      // either a complex type or a primitive base type.
      const subEntities: SubEntityMeta[] = this.getSubEntityDetails(
        parent.name,
        schema
      );
      return (
        <SubEntitiesViewer
          subEntities={subEntities}
          onInspect={this.onEntityInspected}
          onSelect={this.onEntitySelected}
          selected={this.state.inspectPath}
        />
      );
    } else {
      const fieldProperty = this.resolveFieldProperties(parent.name, parent.definition);
      const subEntity: SubEntityMeta = {
        name:
          'pattern' in parent.definition
            ? parent.definition.pattern
            : parent.name,
        required: false,
        type: fieldProperty.type,
        description: parent.definition.description,
        clickable: fieldProperty.clickable,
      };
      return <FieldMetadataViewer fieldMetadata={subEntity} />;
    }
  };
}
