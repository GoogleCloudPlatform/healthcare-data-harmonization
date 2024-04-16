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
import Typography from '@material-ui/core/Typography';

/**
 * Properites for a TopEntityTooltip component.
 */
interface Props {
  /**
   * The name of the entity/resource.
   */
  name: string;

  /**
   * The detailed description of the entity/resource.
   */
  description: string;
}

/**
 * Tooltip that describes a top level entity/resources in a specific data model.
 * For example, in FHIR, this is the for the resources (e.g. Patient).
 *
 * @param props - properties to customize the component.
 */
// tslint:disable-next-line:enforce-name-casing React component.
export function TopEntityTooltip(props: Props) {
  return (
    <div>
      <Typography variant='overline' color='textSecondary'>Resource:</Typography>
      <Typography variant='subtitle1' color='textPrimary' gutterBottom>{props.name}</Typography>
      <Divider />
      <Typography variant='overline' color='textSecondary'>Scope and Usage:</Typography>
      <Typography variant='caption' display='block' color='textPrimary'>{props.description}</Typography>
    </div>
  );
}
