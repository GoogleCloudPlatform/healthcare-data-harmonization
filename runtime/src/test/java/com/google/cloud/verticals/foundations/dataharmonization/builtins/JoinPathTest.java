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
package com.google.cloud.verticals.foundations.dataharmonization.builtins;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.utils.RuntimeContextUtil;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Tests for join path method. */
@RunWith(Parameterized.class)
public class JoinPathTest {
  private final String firstPathElem;
  private final String[] otherPathElems;
  private final String want;
  private final RuntimeContext context = RuntimeContextUtil.testContext();

  public JoinPathTest(String name, String firstPathElem, String[] otherPathElems, String want) {
    this.firstPathElem = firstPathElem;
    this.otherPathElems = otherPathElems;
    this.want = want;
  }

  @Parameters(name = "{0}")
  public static ImmutableList<Object[]> data() {
    return ImmutableList.of(
        new Object[] {" absolute, no scheme", "/one/two", new String[] {"three"}, "/one/two/three"},
        new Object[] {
          "no scheme relative", "./one/two", new String[] {"./three"}, "./one/two/three"
        },
        new Object[] {
          "parent relative, no scheme", "../one/two", new String[] {"../three"}, "../one/three"
        },
        new Object[] {
          "import root relative, no scheme ", "one/two", new String[] {"../three"}, "one/three"
        },
        new Object[] {
          "absolute no leading slash, scheme",
          "foo://one/two",
          new String[] {"../three"},
          "foo://one/three"
        },
        new Object[] {
          "absolute, scheme ", "foo:///one/two", new String[] {"three"}, "foo:///one/two/three"
        },
        new Object[] {
          "relative, scheme", "foo://./one/two", new String[] {"./three"}, "foo://./one/two/three"
        },
        new Object[] {
          "parent relative, no scheme", "../one/two", new String[] {"../three"}, "../one/three"
        });
  }

  @Test
  public void test() {
    String got = FileFns.joinPath(context, firstPathElem, otherPathElems).string();
    assertThat(got).isEqualTo(want);
  }
}
