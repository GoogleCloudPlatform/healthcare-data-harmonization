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

import {makeStyles} from '@material-ui/core/styles';
import ClickAwayListener from '@material-ui/core/ClickAwayListener';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableRow from '@material-ui/core/TableRow';

import {InspectTooltip} from './inspect_tool_tip';
import {TopEntityTooltip} from './top_entity_tool_tip';
import {TopEntityMeta} from './types';

const localStyles = makeStyles({
  span: {
    cursor: 'pointer',
    '&:hover': {
      textDecoration: 'underline',
    },
  },
});

/**
 * Properties to customize the TopEntitiesViewer component.
 */
interface Props {
  /**
   * A list of all of the top level entites renderable by the component.
   */
  topEntities: TopEntityMeta[];

  /**
   * The name of the selected/active top level entity.
   */
  selected: string;

  /**
   * Callback invoked when a specific top level entity is inspected.
   */
  onInspect: (name: string) => void;

  /**
   * Callback invoked when a specific top level entity is selected.
   */
  onSelect: (name: string) => void;
}

/**
 * Component to visualize all the top level entites within a data model.
 */
// tslint:disable-next-line:enforce-name-casing React component.
export function TopEntitiesViewer(props: Props) {
  const [open, setOpen] = React.useState(false);
  const classes = localStyles();
  let keyIndex = 0;
  const handleTooltipClose = () => {
    setOpen(false);
  };
  return (
    <ClickAwayListener onClickAway={handleTooltipClose}>
      <Table>
        <TableBody>
          {props.topEntities.map(meta => {
            const isSelected = meta.name === props.selected;
            return (
              <InspectTooltip
                key={keyIndex++}
                title={TopEntityTooltip(meta)}
                open={isSelected && open}
                disableFocusListener
                disableHoverListener
                disableTouchListener
              >
                <TableRow
                  hover
                  key={meta.name}
                  onClick={e => {
                    setOpen(true);
                    props.onInspect(meta.name);
                  }}
                  selected={isSelected}
                >
                  <TableCell component="th" scope="row">
                    <span
                      className={classes.span}
                      onClick={e => {
                        props.onSelect(meta.name);
                      }}
                    >
                      {meta.name}
                    </span>
                  </TableCell>
                </TableRow>
              </InspectTooltip>
            );
          })}
        </TableBody>
      </Table>
    </ClickAwayListener>
  );
}
