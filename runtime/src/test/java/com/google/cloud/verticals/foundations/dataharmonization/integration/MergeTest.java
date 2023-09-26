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
package com.google.cloud.verticals.foundations.dataharmonization.integration;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.cloud.verticals.foundations.dataharmonization.data.merge.ExtendMergeStrategy.concat;
import static com.google.cloud.verticals.foundations.dataharmonization.utils.AssertUtil.assertDCAPEquals;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.path.Path;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Class to perform integration tests on merge modes. */
@RunWith(JUnit4.class)
public class MergeTest {
  private final IntegrationTest tester = new IntegrationTest("merge/");

  @Test
  public void merge_container_vars_noPath() throws Exception {
    Engine engine = tester.initializeTestFile("container_vars_nopath.wstl");
    Data actual = engine.transform(NullData.instance);
    Data expected = noPathContainerExpected();

    assertDCAPEquals(expected, actual);
  }

  @Test
  public void merge_container_vars_path() throws Exception {
    Engine engine = tester.initializeTestFile("container_vars_path.wstl");
    Data actual = engine.transform(NullData.instance);
    Data expected = testDTI().containerOf(ImmutableMap.of("thicccVar", noPathContainerExpected()));

    assertDCAPEquals(expected, actual);
  }

  @Test
  public void merge_container_fields_path() throws Exception {
    Engine engine = tester.initializeTestFile("container_fields_path.wstl");
    Data actual = engine.transform(NullData.instance);
    Data expected =
        testDTI().containerOf(ImmutableMap.of("thicccField", noPathContainerExpected()));

    assertDCAPEquals(expected, actual);
  }

  @Test
  public void merge_container_fields_noPath() throws Exception {
    Engine engine = tester.initializeTestFile("container_fields_nopath.wstl");
    Data actual = engine.transform(NullData.instance);
    Data expected = noPathContainerExpected();

    assertDCAPEquals(expected, actual);
  }

  @Test
  public void merge_array_vars_noPath() throws Exception {
    Engine engine = tester.initializeTestFile("array_vars_nopath.wstl");
    Data actual = engine.transform(NullData.instance);
    Data expected = noPathArrayExpected();

    assertDCAPEquals(expected, actual);
  }

  @Test
  public void merge_array_fields_noPath() throws Exception {
    Engine engine = tester.initializeTestFile("array_fields_nopath.wstl");
    Data actual = engine.transform(NullData.instance);
    Data expected = noPathArrayExpected();

    assertDCAPEquals(expected, actual);
  }

  @Test
  public void merge_array_vars_path() throws Exception {
    Engine engine = tester.initializeTestFile("array_vars_path.wstl");
    Data actual = engine.transform(NullData.instance);
    Data expected = testDTI().containerOf(ImmutableMap.of("thicccVar", noPathArrayExpected()));

    assertDCAPEquals(expected, actual);
  }

  @Test
  public void merge_array_fields_path() throws Exception {
    Engine engine = tester.initializeTestFile("array_fields_path.wstl");
    Data actual = engine.transform(NullData.instance);
    Data expected = testDTI().containerOf(ImmutableMap.of("thicccField", noPathArrayExpected()));

    assertDCAPEquals(expected, actual);
  }

  @Test
  public void merge_primitives() throws Exception {
    Engine engine = tester.initializeTestFile("primitives.wstl");
    Data actual = engine.transform(NullData.instance);
    Data expected = tester.loadJson("primitives.json");

    assertDCAPEquals(expected, actual);
  }

  private Data noPathArrayExpected() throws IOException {
    return testDTI()
        .containerOf(
            ImmutableMap.of(
                "replace",
                // Shocked pikachu
                inboundArray(),
                "merge",
                // Legacy merge for now (i.e. concat)
                concat(baseArray(), inboundArray()),
                "extend",
                // Explicit concat
                concat(baseArray(), inboundArray()),
                "append",
                // Explicit append (creating a nested array)
                baseArray().setElement(5, inboundArray())));
  }

  private Data noPathContainerExpected() throws IOException {
    return testDTI()
        .containerOf(
            ImmutableMap.of(
                "replace",
                // <Shocked pikachu face>
                inboundContainer(),
                "merge",
                // Semi-manual construction of the merge expected output
                testDTI()
                    .containerOf(
                        ImmutableMap.of(
                            // a kept from base
                            "a",
                            baseContainer().getField("a"),
                            // b got from inbound
                            "b",
                            inboundContainer().getField("b"),
                            // array got concatenated
                            "array",
                            concat(
                                baseContainer().getField("array").asArray(),
                                inboundContainer().getField("array").asArray()),
                            // container got merged
                            "container",
                            testDTI()
                                .containerOf(
                                    ImmutableMap.of(
                                        // a and b from respective containers
                                        "a",
                                        Path.parse("container.a").get(baseContainer()),
                                        "b",
                                        Path.parse("container.b").get(inboundContainer()),
                                        // prims got replaced by inbound
                                        "num",
                                        Path.parse("container.num").get(inboundContainer()),
                                        "str",
                                        Path.parse("container.str").get(inboundContainer()),
                                        "bool",
                                        Path.parse("container.bool").get(inboundContainer()))),
                            // prims got replaced by inbound
                            "num",
                            inboundContainer().getField("num"),
                            "str",
                            inboundContainer().getField("str"),
                            "bool",
                            inboundContainer().getField("bool"))),
                "extend",
                // Extension just added the b field, left everything else as is.
                baseContainer().setField("b", inboundContainer().getField("b"))));
  }

  private Container baseContainer() throws IOException {
    return tester.loadJson("data.json").asContainer().getField("shallowcontainer1").asContainer();
  }

  private Container inboundContainer() throws IOException {
    return tester.loadJson("data.json").asContainer().getField("shallowcontainer2").asContainer();
  }

  private Array baseArray() throws IOException {
    return tester.loadJson("data.json").asContainer().getField("shallowarray1").asArray();
  }

  private Array inboundArray() throws IOException {
    return tester.loadJson("data.json").asContainer().getField("shallowarray2").asArray();
  }
}
