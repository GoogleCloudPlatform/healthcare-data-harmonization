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

import {makeStyles} from '@material-ui/core/styles';
import Table from '@material-ui/core/Table';
import TableHead from '@material-ui/core/TableHead';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableRow from '@material-ui/core/TableRow';

import {InspectTooltip} from './inspect_tool_tip';
import {SubEntityTooltip} from './sub_entity_tool_tip';
import {SubEntityMeta} from './types';

const localStyles = makeStyles({
  span: {
    cursor: 'pointer',
    '&:hover': {
      textDecoration: 'underline',
    },
  },
});

/**
 * Properties to customize the SubEntitiesViewer component.
 */
interface Props {
  /**
   * A list of all of the sub entites/fields renderable by the component.
   */
  subEntities: SubEntityMeta[];

  /**
   * The name of the selected/active sub level entity.
   */
  selected: string;

  /**
   * Callback invoked when a specific top level entity is inspected.
   */
  onInspect: (name: string) => void;

  /**
   * Callback invoked when a specific sub level entity is selected.
   */
  onSelect: (name: string) => void;
}

/**
 * Component to visualize all the sub entites/fields within a data model.
 */
// tslint:disable-next-line:enforce-name-casing React component.
export function SubEntitiesViewer(props: Props) {
  const classes = localStyles();
  let keyIndex = 0;
  return (
    <Table>
      <TableHead>
        <TableRow>
          <TableCell>Field name</TableCell>
          <TableCell>Type</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {props.subEntities.map(meta => {
          const isSelected = meta.name === props.selected;
          return (
            <InspectTooltip
              key={keyIndex++}
              title={SubEntityTooltip(meta)}
              open={isSelected}
              disableFocusListener
              disableHoverListener
              disableTouchListener
            >
              <TableRow
                hover
                key={meta.name}
                onClick={e => {
                  props.onInspect(meta.name);
                }}
                selected={isSelected}
              >
                <TableCell component="th" scope="row">
                  <span
                    className={classes.span}
                    onClick={e => {
                      if (meta.clickable) {
                        props.onSelect(meta.name);
                      }
                    }}
                  >
                    {meta.name}
                  </span>
                </TableCell>
                <TableCell>
                  <span>{meta.type}</span>
                </TableCell>
              </TableRow>
            </InspectTooltip>
          );
        })}
      </TableBody>
    </Table>
  );
}
