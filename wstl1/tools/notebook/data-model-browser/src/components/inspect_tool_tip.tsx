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
import { makeStyles, Theme } from '@material-ui/core/styles';
import Tooltip, { TooltipProps } from '@material-ui/core/Tooltip';

const styles = makeStyles((theme: Theme) => {
  return {
    arrow: {
      position: 'absolute' as 'absolute',
      fontSize: 6,
      '&::before': {
        content: '""',
        margin: 'auto',
        display: 'block',
        width: 0,
        height: 0,
        borderStyle: 'solid',
      },
    },
    popper: {
      filter: `drop-shadow(2px 4px 6px #333333)`,
      '&[x-placement*="left"] $arrow': {
        right: 0,
        marginRight: '-0.95em',
        height: '2em',
        width: '1em',
        '&::before': {
          borderWidth: '1em 0 1em 1em',
          borderColor: `transparent transparent transparent ${theme.palette.common.white}`,
        },
      },
    },
    tooltip: {
      backgroundColor: theme.palette.common.white,
      color: theme.palette.common.black,
      position: 'relative' as 'relative',
    },
    tooltipPlacementLeft: {
      margin: '0 8px',
    },
  };
});

/**
 * Container ToolTip component that renders a component as the Tooltip
 * title.
 */
// tslint:disable-next-line:enforce-name-casing
export function InspectTooltip(props: TooltipProps) {
  const { arrow, ...classes } = styles(props);
  const [arrowRef, setArrowRef] = React.useState<HTMLSpanElement | null>(null);
  return (
    <Tooltip
      classes={classes}
      placement="left"
      PopperProps={{
        popperOptions: {
          modifiers: {
            arrow: {
              enabled: Boolean(arrowRef),
              element: arrowRef,
            },
          },
        },
      }}
      {...props}
      title={
        <React.Fragment>
          {props.title}
          <span ref={setArrowRef} />
        </React.Fragment>
      }
    />
  );
}
