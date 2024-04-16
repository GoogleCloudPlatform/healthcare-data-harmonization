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
import Link from '@material-ui/core/Link';

/**
 * Properties for BreadCrumbItem component.
 */
interface BreadCrumbItemProps {
  /**
   * The JSON path components leading to a leaf bread crumb item.
   */
  path: string[];

  /**
   * The display label for the bread crumb item.
   */
  label: string;

  /**
   * The callback function invoked when the bread crumb item is clicked. The
   * callback argument are the path components of the bread crumb item.
   */
  onClick: (path: string[]) => void;
}

/**
 * Component that represents a single bread crumb item.
 */
export class BreadCrumbItem extends React.PureComponent<BreadCrumbItemProps> {
  /**
   * Constructs a new BreadCrumbItem.
   */
  constructor(props: BreadCrumbItemProps) {
    super(props);
  }

  /**
   * Render the BreadCrumbItem component.
   */
  render() {
    return (
      <Link
        color="textSecondary"
        href={
          this.props.path.length > 0
            ? this.props.path[this.props.path.length - 1]
            : '#'
        }
        onClick={(e: React.MouseEvent<HTMLAnchorElement>) => {
          e.preventDefault();
          this.props.onClick(this.props.path);
        }}
      >
        {this.props.label}
      </Link>
    );
  }
}
