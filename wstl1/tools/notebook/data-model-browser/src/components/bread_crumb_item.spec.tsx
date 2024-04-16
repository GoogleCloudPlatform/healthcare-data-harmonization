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
import {shallow} from 'enzyme';

import Link from '@material-ui/core/Link';
import {BreadCrumbItem} from './bread_crumb_item';

describe('BreadCrumbItem', () => {
  const click = jest.fn();
  const fakeField = 'fake_field';
  const path = ['FHIR', 'FakeResource', fakeField];
  const wrapper = shallow(
    <BreadCrumbItem
      path={path}
      label={fakeField}
      onClick={click}
    />
  );

  it('should render correctly', () => expect(wrapper).toMatchSnapshot());

  it('should render children', () => {
    expect(wrapper.contains(fakeField)).toBeTruthy();
  });

  it('should call the correct function on click', () => {
    wrapper.find(Link).simulate('click', {
      preventDefault: () => {},
    });
    expect(click).toHaveBeenCalled();
  });
});
