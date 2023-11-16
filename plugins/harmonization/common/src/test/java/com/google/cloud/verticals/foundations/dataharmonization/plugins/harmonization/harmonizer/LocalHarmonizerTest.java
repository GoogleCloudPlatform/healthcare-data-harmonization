/*
 * Copyright 2020 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.harmonization.harmonizer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.cloud.verticals.foundations.dataharmonization.plugins.harmonization.harmonizer.LocalHarmonizer.Conceptmap;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link LocalHarmonizer}. */
@RunWith(JUnit4.class)
public class LocalHarmonizerTest {
  Conceptmap conceptmap;
  LocalHarmonizer localHarmonizer;

  @Before
  public void setUp() {
    JsonObject jsonCM1 = getJsonFromFile(HarmonizationTest.CONCEPT_MAP1);
    conceptmap = new Conceptmap(jsonCM1);

    localHarmonizer = new LocalHarmonizer(jsonCM1);
    JsonObject jsonCM2 = getJsonFromFile(HarmonizationTest.CONCEPT_MAP2);
    localHarmonizer.addAnotherConceptmap(jsonCM2);
  }

  private JsonObject getJsonFromFile(String path) {
    try {
      InputStream stream = LocalHarmonizerTest.class.getResourceAsStream(path);
      JsonElement element =
          JsonParser.parseString(
              CharStreams.toString(new InputStreamReader(stream, Charsets.UTF_8)));
      return element.getAsJsonObject();
    } catch (IOException e) {
      System.out.println("Json file is missing.");
    }
    return null;
  }

  @Test
  public void testSingleTarget() {
    HarmonizedCode[] hc1ab = {
      HarmonizedCode.create("target_code11_ab", "target_display11_ab", "target_system_b", "v1")
    };
    assertArrayEquals(
        hc1ab,
        conceptmap.harmonize("source_code1", "source_system_a", "target_system_b").toArray());
  }

  @Test
  public void testMultipleTargets() {
    HarmonizedCode[] hc2ab = {
      HarmonizedCode.create("target_code21_ab", "target_display21_ab", "target_system_b", "v1"),
      HarmonizedCode.create("target_code22_ab", "target_display22_ab", "target_system_b", "v1")
    };
    assertArrayEquals(
        hc2ab,
        conceptmap.harmonize("source_code2", "source_system_a", "target_system_b").toArray());
  }

  @Test
  public void testUnmappedFixed() {
    HarmonizedCode[] hcab = {
      HarmonizedCode.create("unmapped_code_ab", "unmapped_display_ab", "target_system_b", "v1")
    };
    assertArrayEquals(
        hcab,
        conceptmap
            .harmonize("source_code_missing1", "source_system_a", "target_system_b")
            .toArray());
    assertArrayEquals(
        hcab,
        conceptmap
            .harmonize("source_code_missing2", "source_system_a", "target_system_b")
            .toArray());
  }

  @Test
  public void testUnmappedProvided() {
    HarmonizedCode[] hcMissing1 = {
      HarmonizedCode.create("source_code_missing1", "source_code_missing1", "target_system_c", "v1")
    };
    assertArrayEquals(
        hcMissing1,
        conceptmap
            .harmonize("source_code_missing1", "source_system_a", "target_system_c")
            .toArray());

    HarmonizedCode[] hcMissing2 = {
      HarmonizedCode.create("source_code_missing2", "source_code_missing2", "target_system_c", "v1")
    };
    assertArrayEquals(
        hcMissing2,
        conceptmap
            .harmonize("source_code_missing2", "source_system_a", "target_system_c")
            .toArray());
  }

  @Test
  public void testMissingUnmapped() {
    HarmonizedCode[] hcMissing1 = {
      HarmonizedCode.create("source_code_missing", "", conceptmap.getId() + "-unharmonized", "v1")
    };
    assertArrayEquals(
        hcMissing1,
        conceptmap
            .harmonize("source_code_missing", "source_system_e", "target_system_f")
            .toArray());
  }

  @Test
  public void testMissingGroup() {
    HarmonizedCode[] hcMissing1 = {
      HarmonizedCode.create("source_code1", "", conceptmap.getId() + "-unharmonized", "v1")
    };
    assertArrayEquals(
        hcMissing1, conceptmap.harmonize("source_code1", "source_system_missing", "").toArray());
    assertArrayEquals(
        hcMissing1, conceptmap.harmonize("source_code1", "", "target_system_missing").toArray());
  }

  @Test
  public void testMultipleGroupsMissingSourceSystem() {
    HarmonizedCode[] hc1b = {
      HarmonizedCode.create("target_code11_ab", "target_display11_ab", "target_system_b", "v1"),
      HarmonizedCode.create("target_code11_db", "target_display11_db", "target_system_b", "v1")
    };
    assertArrayEquals(hc1b, conceptmap.harmonize("source_code1", "", "target_system_b").toArray());
  }

  @Test
  public void testMultipleGroupsMissingTargetSystem() {
    HarmonizedCode[] hc1a = {
      HarmonizedCode.create("target_code11_ab", "target_display11_ab", "target_system_b", "v1"),
      HarmonizedCode.create("target_code11_ac", "target_display11_ac", "target_system_c", "v1")
    };
    assertArrayEquals(hc1a, conceptmap.harmonize("source_code1", "source_system_a", "").toArray());
  }

  @Test
  public void testMultipleGroupsMissingSourceAndTargetSystems() {
    HarmonizedCode[] hc1 = {
      HarmonizedCode.create("target_code11_ab", "target_display11_ab", "target_system_b", "v1"),
      HarmonizedCode.create("target_code11_ac", "target_display11_ac", "target_system_c", "v1"),
      HarmonizedCode.create("target_code11_db", "target_display11_db", "target_system_b", "v1"),
      HarmonizedCode.create("target_code11_ef", "target_display11_ef", "target_system_f", "v1")
    };
    assertArrayEquals(hc1, conceptmap.harmonize("source_code1", "", "").toArray());
  }

  @Test
  public void testMultipleGroupsMultipleTargetsMissingSourceSystem() {
    HarmonizedCode[] hc2b = {
      HarmonizedCode.create("target_code21_ab", "target_display21_ab", "target_system_b", "v1"),
      HarmonizedCode.create("target_code22_ab", "target_display22_ab", "target_system_b", "v1"),
      HarmonizedCode.create("target_code21_db", "target_display21_db", "target_system_b", "v1"),
      HarmonizedCode.create("target_code22_db", "target_display22_db", "target_system_b", "v1")
    };
    assertArrayEquals(hc2b, conceptmap.harmonize("source_code2", "", "target_system_b").toArray());
  }

  @Test
  public void testMultipleGroupsMultipleTargetsMissingTargetSystem() {
    HarmonizedCode[] hc2a = {
      HarmonizedCode.create("target_code21_ab", "target_display21_ab", "target_system_b", "v1"),
      HarmonizedCode.create("target_code22_ab", "target_display22_ab", "target_system_b", "v1"),
      HarmonizedCode.create("target_code21_ac", "target_display21_ac", "target_system_c", "v1"),
      HarmonizedCode.create("target_code22_ac", "target_display22_ac", "target_system_c", "v1")
    };
    assertArrayEquals(hc2a, conceptmap.harmonize("source_code2", "source_system_a", "").toArray());
  }

  @Test
  public void testMultipleGroupsMultipleTargetsMissingSourceAndTargetSystems() {
    HarmonizedCode[] hc2 = {
      HarmonizedCode.create("target_code21_ab", "target_display21_ab", "target_system_b", "v1"),
      HarmonizedCode.create("target_code22_ab", "target_display22_ab", "target_system_b", "v1"),
      HarmonizedCode.create("target_code21_ac", "target_display21_ac", "target_system_c", "v1"),
      HarmonizedCode.create("target_code22_ac", "target_display22_ac", "target_system_c", "v1"),
      HarmonizedCode.create("target_code21_db", "target_display21_db", "target_system_b", "v1"),
      HarmonizedCode.create("target_code22_db", "target_display22_db", "target_system_b", "v1")
    };
    assertArrayEquals(hc2, conceptmap.harmonize("source_code2", "", "").toArray());
  }

  @Test
  public void testMultipleGroupsOneUnmappedProvided() {
    HarmonizedCode[] hc3a = {
      HarmonizedCode.create("target_code31_ab", "target_display31_ab", "target_system_b", "v1"),
      HarmonizedCode.create("source_code3", "source_code3", "target_system_c", "v1")
    };
    assertArrayEquals(hc3a, conceptmap.harmonize("source_code3", "source_system_a", "").toArray());
  }

  @Test
  public void testMultipleGroupsOneUnmappedFixed() {
    HarmonizedCode[] hc3b = {
      HarmonizedCode.create("target_code31_ab", "target_display31_ab", "target_system_b", "v1"),
      HarmonizedCode.create("unmapped_code_db", "unmapped_display_db", "target_system_b", "v1")
    };
    assertArrayEquals(hc3b, conceptmap.harmonize("source_code3", "", "target_system_b").toArray());
  }

  @Test
  public void testMultipleGroupsAllUnmapped() {
    HarmonizedCode[] hcMissing = {
      HarmonizedCode.create("unmapped_code_ab", "unmapped_display_ab", "target_system_b", "v1"),
      HarmonizedCode.create("source_code_missing", "source_code_missing", "target_system_c", "v1"),
      HarmonizedCode.create("unmapped_code_db", "unmapped_display_db", "target_system_b", "v1")
    };
    assertArrayEquals(hcMissing, conceptmap.harmonize("source_code_missing", "", "").toArray());
  }

  @Test
  public void testLookupById() {
    HarmonizedCode[] hc1ab = {
      HarmonizedCode.create("target_code11_ab", "target_display11_ab", "target_system_b", "v1")
    };
    assertArrayEquals(
        hc1ab,
        localHarmonizer
            .harmonizeWithTarget(
                "source_code1", "source_system_a", "target_system_b", "conceptmap_id1")
            .toArray());

    HarmonizedCode[] hc10xy = {
      HarmonizedCode.create("target_code10_xy", "target_display10_xy", "target_system_y", "v1.1")
    };
    assertArrayEquals(
        hc10xy,
        localHarmonizer
            .harmonizeWithTarget(
                "source_code10", "source_system_x", "target_system_y", "conceptmap_id2")
            .toArray());
  }

  @Test
  public void testLookupUnMapped() {
    HarmonizedCode[] hcMissing1 = {
      HarmonizedCode.create("unmapped_code_ab", "unmapped_display_ab", "target_system_b", "v1")
    };
    assertArrayEquals(
        hcMissing1,
        localHarmonizer
            .harmonizeWithTarget(
                "source_code_missing", "source_system_a", "target_system_b", "conceptmap_id1")
            .toArray());

    HarmonizedCode[] hcMissing2 = {
      HarmonizedCode.create("unmapped_code_xy", "unmapped_display_xy", "target_system_y", "v1.1")
    };
    assertArrayEquals(
        hcMissing2,
        localHarmonizer
            .harmonizeWithTarget(
                "source_code_missing", "source_system_x", "target_system_y", "conceptmap_id2")
            .toArray());
  }

  @Test
  public void testLookupMissingGroup() {
    HarmonizedCode[] hcMissing1 = {
      HarmonizedCode.create("source_code1", "", "conceptmap_id1" + "-unharmonized", "v1")
    };
    assertArrayEquals(
        hcMissing1,
        localHarmonizer
            .harmonizeWithTarget("source_code1", "source_system_missing", "", "conceptmap_id1")
            .toArray());

    HarmonizedCode[] hcMissing2 = {
      HarmonizedCode.create("source_code10", "", "conceptmap_id2" + "-unharmonized", "v1.1")
    };
    assertArrayEquals(
        hcMissing2,
        localHarmonizer
            .harmonizeWithTarget(
                "source_code10", "source_system_missing", "target_system_y", "conceptmap_id2")
            .toArray());
  }

  @Test
  public void testLookupMissingConceptmap() {
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> localHarmonizer.harmonize("", "", "missing_conceptmap"));
    assertEquals(
        "There is no conceptMap with the give id: 'missing_conceptmap'", thrown.getMessage());
  }
}
