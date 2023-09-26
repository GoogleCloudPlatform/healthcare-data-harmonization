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
import TableRow from '@material-ui/core/TableRow';
import {TopEntitiesViewer} from './top_entities_viewer';
import {TopEntityMeta} from './types';

describe('TopEntitiesViewer', () => {

  it('should render correctly', () => {
    const entities: TopEntityMeta[] = [
      {name: 'Test', description: 'Verbose details'},
    ];
    const selected = 'Test';
    const onInspect = jest.fn();
    const onSelect = jest.fn();

    const wrapper = shallow(
      <TopEntitiesViewer
        topEntities={entities}
        onInspect={onInspect}
        onSelect={onSelect}
        selected={selected}
      />
    );
    expect(wrapper).toMatchSnapshot();
  });

  it('should call the correct function on entity row click', () => {
    const entities: TopEntityMeta[] = [
      {name: 'Test', description: 'Verbose details'},
    ];
    const selected = 'Test';
    const onInspect = jest.fn();
    const onSelect = jest.fn();

    const wrapper = shallow(
      <TopEntitiesViewer
        topEntities={entities}
        onInspect={onInspect}
        onSelect={onSelect}
        selected={selected}
      />
    );
    wrapper.find(TableRow).simulate('click');
    expect(onInspect).toHaveBeenCalled();
    expect(onInspect).toBeCalledWith('Test');
    expect(onSelect).not.toHaveBeenCalled();
  });

  it('should call the correct function on entity cell click', () => {
    const entities: TopEntityMeta[] = [
      {name: 'Test', description: 'Verbose details'},
    ];
    const selected = 'Test';
    const onInspect = jest.fn();
    const onSelect = jest.fn();

    const wrapper = shallow(
      <TopEntitiesViewer
        topEntities={entities}
        onInspect={onInspect}
        onSelect={onSelect}
        selected={selected}
      />
    );
    wrapper.find('span').simulate('click');
    expect(onInspect).not.toHaveBeenCalled();
    expect(onSelect).toHaveBeenCalled();
    expect(onSelect).toBeCalledWith('Test');
  });
});
