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
import {Divider} from '@material-ui/core';
import Grid from '@material-ui/core/Grid';
import Typography from '@material-ui/core/Typography';

/**
 * Properites for a SubEntityProps component.
 */
interface SubEntityProps {
  /**
   * The name of the sub entity/field.
   */
  name: string;

  /**
   * The detailed description of the sub entity/field.
   */
  description: string;

  /**
   * Whether the sub entity/field is defined as being required within the
   * data model.
   */
  required: boolean;

  /**
   * The type of the sub entity/field as define within the data model.
   */
  type: string;
}

/**
 * Tooltip that describes a top level entity/resources in a specific data model.
 * For example, in FHIR, this is the for the resources (e.g. Patient).
 */
// tslint:disable-next-line:enforce-name-casing
export function SubEntityTooltip(props: SubEntityProps) {
  return (
    <React.Fragment>
      <Typography variant='overline' color='textSecondary'>Field:</Typography>
      <Typography variant='subtitle1' color='textPrimary' gutterBottom>{props.name}</Typography>
      <Divider />
      <Grid container direction='row' justify='flex-start' alignItems='flex-start'>
        <Grid item xs>
            <Typography variant='overline' color='textSecondary'>Required:</Typography>
            <Typography variant='subtitle1' color='textPrimary' gutterBottom>{props.required ? 'yes' : 'no'}</Typography>
        </Grid>
        <Grid item xs>
            <Typography variant='overline' color='textSecondary'>Type:</Typography>
            <Typography variant='subtitle1' color='textPrimary' gutterBottom>{props.type}</Typography>
        </Grid>
      </Grid>
      <Typography variant='overline' color='textSecondary'>Scope and Usage:</Typography>
      <Typography variant='caption' display='block' color='textPrimary' gutterBottom>{props.description}</Typography>
    </React.Fragment>
  );
}
