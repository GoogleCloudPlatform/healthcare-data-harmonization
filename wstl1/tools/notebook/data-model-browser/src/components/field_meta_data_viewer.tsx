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
import withStyles, {WithStyles} from '@material-ui/core/styles/withStyles';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableRow from '@material-ui/core/TableRow';
import {SubEntityMeta} from './types';

import {Theme} from '@material-ui/core/styles';

const styles = (theme: Theme) => {
  return {
    root: {
      flex: 1,
    },
  };
};

/**
 * Property for FieldMetadataViewer.
 */
interface Props extends WithStyles<typeof styles> {
  /**
   * The sub entity/field meta data to render.
   */
  fieldMetadata: SubEntityMeta;
}

/**
 * A row in the FieldMetadataViewer component's table.
 */
interface TableRow {
  /**
   * The label of the row.
   */
  label: string;

  /**
   * The content of the row.
   */
  value: string;
}

/**
 * Component for rendering the meta data/details for a field in the data model.
 */
class FieldMetadataTable extends React.PureComponent<Props> {
  constructor(props: Props) {
    super(props);
  }

  private createTableRows(fieldMetadata: SubEntityMeta): TableRow[] {
    const requiredString = fieldMetadata.required ? 'Yes' : 'No';
    return [
      {label: 'Field', value: fieldMetadata.name},
      {label: 'Required', value: requiredString},
      {label: 'Type', value: fieldMetadata.type},
      {label: 'Definition', value: fieldMetadata.description},
    ];
  }

  render() {
    const fieldMetadata = this.props.fieldMetadata;
    if (!fieldMetadata) {
      return null;
    }
    return (
      <Table>
        <TableBody>
          {this.createTableRows(fieldMetadata).map(row => {
            return (
              <TableRow key={row.label}>
                <TableCell component="th" scope="row">
                  <span>{row.label}</span>
                </TableCell>
                <TableCell component="th" scope="row">
                  <span>{row.value}</span>
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    );
  }
}

/**
 * Component for rendering the meta data/details for a field in the data model.
 */
// tslint:disable-next-line:enforce-name-casing React component.
export const FieldMetadataViewer = withStyles(styles)(FieldMetadataTable);
