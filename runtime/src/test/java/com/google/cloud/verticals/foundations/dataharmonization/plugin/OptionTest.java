/*
 * Copyright 2022 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.verticals.foundations.dataharmonization.plugin;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.WrapperContext;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Option.WithOptionWrapper;
import com.google.cloud.verticals.foundations.dataharmonization.utils.RuntimeContextUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

/** Tests for Options helper APIs. */
@RunWith(JUnit4.class)
public class OptionTest {

  @Test
  public void withOption_alreadyEnabled_noop() {
    Option option = Mockito.mock(Option.class);

    RuntimeContext leaf = RuntimeContextUtil.mockRuntimeContextWithDefaultMetaData();
    WithOptionWrapper wrapper = new WithOptionWrapper(leaf);
    wrapper.enable(option);
    ExtraWrapper extraWrapper = new ExtraWrapper(wrapper);

    RuntimeContext actual = Option.withOption(extraWrapper, option);
    assertThat(actual.enabledOptions()).contains(option);
    assertThat(inner(actual)).isInstanceOf(ExtraWrapper.class);
    assertThat(inner(inner(actual))).isSameInstanceAs(leaf);
  }

  @Test
  public void withOption_alreadyDisabled_enabled() {
    Option option = Mockito.mock(Option.class);

    RuntimeContext leaf = RuntimeContextUtil.mockRuntimeContextWithDefaultMetaData();
    WithOptionWrapper wrapper = new WithOptionWrapper(leaf);
    wrapper.disable(option);
    ExtraWrapper extraWrapper = new ExtraWrapper(wrapper);

    RuntimeContext actual = Option.withOption(extraWrapper, option);
    assertThat(actual.enabledOptions()).contains(option);
    assertThat(actual).isInstanceOf(WithOptionWrapper.class);
    assertThat(inner(actual)).isInstanceOf(ExtraWrapper.class);
    assertThat(inner(inner(actual))).isSameInstanceAs(leaf);
  }

  @Test
  public void withOption_noWrappers_wrapsAndEnabled() {
    Option option = Mockito.mock(Option.class);

    RuntimeContext leaf = RuntimeContextUtil.mockRuntimeContextWithDefaultMetaData();

    RuntimeContext actual = Option.withOption(leaf, option);
    assertThat(actual.enabledOptions()).contains(option);
    assertThat(actual).isInstanceOf(WrapperContext.class);
    assertThat(((WrapperContext<?>) actual).getInnerContext()).isSameInstanceAs(leaf);
  }

  @Test
  public void withOption_extraWrapper_wrapsAndEnabled() {
    Option option = Mockito.mock(Option.class);

    RuntimeContext leaf = RuntimeContextUtil.mockRuntimeContextWithDefaultMetaData();
    ExtraWrapper extraWrapper = new ExtraWrapper(leaf);

    RuntimeContext actual = Option.withOption(extraWrapper, option);
    assertThat(actual.enabledOptions()).contains(option);
    assertThat(actual).isInstanceOf(WrapperContext.class);
    assertThat(((WrapperContext<?>) actual).getInnerContext()).isSameInstanceAs(extraWrapper);
  }

  @Test
  public void withOption_multipleOptions_singleWrapper() {
    Option option1 = Mockito.mock(Option.class);
    Option option2 = Mockito.mock(Option.class);

    RuntimeContext leaf = RuntimeContextUtil.mockRuntimeContextWithDefaultMetaData();
    WithOptionWrapper wrapper = new WithOptionWrapper(leaf);
    ExtraWrapper extraWrapper = new ExtraWrapper(wrapper);

    RuntimeContext actual = Option.withOption(Option.withOption(extraWrapper, option1), option2);
    assertThat(actual.enabledOptions()).containsExactly(option1, option2);
    assertThat(actual).isInstanceOf(WithOptionWrapper.class);
    assertThat(inner(actual)).isInstanceOf(ExtraWrapper.class);
    assertThat(inner(inner(actual))).isSameInstanceAs(leaf);
  }

  @Test
  public void withoutOption_noOption_noop() {
    Option option = Mockito.mock(Option.class);

    RuntimeContext leaf = RuntimeContextUtil.mockRuntimeContextWithDefaultMetaData();
    ExtraWrapper extraWrapper = new ExtraWrapper(leaf);

    RuntimeContext actual = Option.withoutOption(extraWrapper, option);
    assertThat(actual.enabledOptions()).isEmpty();
    assertThat(actual).isInstanceOf(WrapperContext.class);
    assertThat(((WrapperContext<?>) actual).getInnerContext()).isSameInstanceAs(leaf);
  }

  @Test
  public void withoutOption_enabledOption_disables() {
    Option option = Mockito.mock(Option.class);

    RuntimeContext leaf = RuntimeContextUtil.mockRuntimeContextWithDefaultMetaData();
    WithOptionWrapper wrapper = new WithOptionWrapper(leaf);
    wrapper.enable(option);
    ExtraWrapper extraWrapper = new ExtraWrapper(wrapper);

    RuntimeContext actual = Option.withoutOption(extraWrapper, option);
    assertThat(actual.enabledOptions()).isEmpty();
    assertThat(actual).isInstanceOf(WithOptionWrapper.class);
    assertThat(inner(actual)).isInstanceOf(ExtraWrapper.class);
    assertThat(inner(inner(actual))).isSameInstanceAs(leaf);
  }

  private static RuntimeContext inner(RuntimeContext context) {
    assertThat(context).isInstanceOf(WrapperContext.class);
    return ((WrapperContext<?>) context).getInnerContext();
  }

  private static class ExtraWrapper extends WrapperContext<ExtraWrapper> {

    public ExtraWrapper(RuntimeContext innerContext) {
      super(innerContext, ExtraWrapper.class);
    }

    @Override
    protected ExtraWrapper rewrap(RuntimeContext innerContext) {
      return new ExtraWrapper(innerContext);
    }
  }
}
