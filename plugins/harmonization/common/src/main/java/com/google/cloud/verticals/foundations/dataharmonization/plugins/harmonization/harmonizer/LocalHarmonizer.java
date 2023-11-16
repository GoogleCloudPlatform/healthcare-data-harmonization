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


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Builds a lookup map based on the input json ConceptMaps. */
public final class LocalHarmonizer implements Harmonizer, java.io.Serializable {
  /**
   * Loads a given json ConceptMaps into memory to performs Harmonization. Here is an example:
   * github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_configs/hl7v2_fhir_stu3/code_harmonization/Gender.harmonization.json
   */
  public static final class Conceptmap implements java.io.Serializable {

    /** Each entry of a ConceptMap has an output of this type. */
    static final class Target implements java.io.Serializable {
      private final String code;
      private final String display;

      public Target(String code, String display) {
        this.code = code;
        this.display = display;
      }

      public String getCode() {
        return code;
      }

      public String getDisplay() {
        return display;
      }
    }

    enum UnmappedMode {
      FIXED {
        @Override
        public String toString() {
          return "fixed";
        }
      },
      PROVIDED {
        @Override
        public String toString() {
          return "provided";
        }
      }
    }

    /** Each Group set has an Unmapped entry which will be used as default returned value. */
    static final class Unmapped implements java.io.Serializable {
      private final Target target;
      private final UnmappedMode mode;

      public Unmapped(JsonObject unmapped) throws IllegalArgumentException {
        String code = "";
        String display = "";
        String mode = unmapped.get("mode").getAsString();

        if (UnmappedMode.FIXED.toString().equals(mode)) {
          this.mode = UnmappedMode.FIXED;
          code = unmapped.get("code").getAsString();
          display = unmapped.get("display").getAsString();
        } else if (UnmappedMode.PROVIDED.toString().equals(mode)) {
          this.mode = UnmappedMode.PROVIDED;
        } else {
          throw new IllegalArgumentException(
              String.format("Unmaped mode does not match one of the expected values: '%s'", mode));
        }

        this.target = new Target(code, display);
      }

      public Target getTarget() {
        return target;
      }

      public UnmappedMode getMode() {
        return mode;
      }
    }

    /**
     * Each ConceptMap is made of one or more Groups that are identified by their Source and Target
     * systems.
     */
    static final class Group implements java.io.Serializable {
      /**
       * Each Group is a list of Elements where each one maps an input SourceCode to a list of
       * Targets.
       */
      final class Elements implements java.io.Serializable {
        private final ImmutableMap<String, ImmutableList<Target>> elementLookup;

        public Elements(List<SimpleImmutableEntry<String, ImmutableList<Target>>> elements) {
          this.elementLookup = ImmutableMap.copyOf(elements);
        }

        public List<Target> lookup(String code) {
          if (this.elementLookup.containsKey(code)) {
            return this.elementLookup.get(code).asList();
          }
          return ImmutableList.of();
        }
      }

      private final String source;
      private final String target;
      private final Elements elements;
      private final Unmapped unmapped;

      public Group(
          String source,
          String target,
          List<SimpleImmutableEntry<String, ImmutableList<Target>>> elements,
          Unmapped unmapped) {
        this.source = source;
        this.target = target;
        this.elements = new Elements(elements);
        this.unmapped = unmapped;
      }

      public boolean canHarmonize(String sourceSystem, String targetSystem) {
        return (sourceSystem.isEmpty() || this.source.equals(sourceSystem))
            && (targetSystem.isEmpty() || this.target.equals(targetSystem));
      }

      public Unmapped getUnmapped() {
        return unmapped;
      }

      public String getTarget() {
        return target;
      }

      public List<Target> lookup(String code) {
        return this.elements.lookup(code);
      }
    }

    private String version;
    private String id;
    private ImmutableList<Group> groups;

    static final String CONCEPT_MAP = "ConceptMap";
    static final String RESOURCE_TYPE = "resourceType";
    static final String ID = "id";
    static final String VERSION = "version";
    static final String GROUP = "group";
    static final String SOURCE = "source";
    static final String TARGET = "target";
    static final String ELEMENT = "element";
    static final String UNMAPPED = "unmapped";
    static final String CODE = "code";
    static final String DISPLAY = "display";

    public Conceptmap(JsonObject conceptMap) {
      // ToDo(b/178478464): include the name of the file in all following error messages.
      if (!CONCEPT_MAP.equals(conceptMap.get(RESOURCE_TYPE).getAsString())) {
        throw new IllegalArgumentException(
            String.format(
                "Given Json object for the ConceptMap does not contain \"resourceType\":"
                    + " \"ConceptMap\", instead: %s",
                conceptMap.get(RESOURCE_TYPE).getAsString()));
      }
      if (conceptMap.get(ID).getAsString().isEmpty()) {
        throw new IllegalArgumentException(
            "Given Json object for the ConceptMap does not have an id.");
      }

      this.id = conceptMap.get(ID).getAsString();
      this.version = conceptMap.get(VERSION).getAsString();
      this.groups = ImmutableList.copyOf(createGroupList(conceptMap.get(GROUP).getAsJsonArray()));
    }

    private List<Group> createGroupList(JsonArray groups) {
      int numGroups = groups.size();
      if (numGroups <= 0) {
        throw new IllegalArgumentException("There must be at least one group per ConceptMap.");
      }

      List<Group> groupList = new ArrayList<Group>();
      groups.forEach(group -> groupList.add(createGroup(group.getAsJsonObject())));
      return groupList;
    }

    private Group createGroup(JsonObject group) {
      String source = group.get(SOURCE).getAsString();
      String target = group.get(TARGET).getAsString();

      Unmapped unmapped = null;
      if (group.has(UNMAPPED)) {
        unmapped = new Unmapped(group.get(UNMAPPED).getAsJsonObject());
      }
      JsonArray elements = group.get(ELEMENT).getAsJsonArray();
      return new Group(source, target, createElementList(elements), unmapped);
    }

    private List<SimpleImmutableEntry<String, ImmutableList<Target>>> createElementList(
        JsonArray elements) {
      int numElements = elements.size();
      if (numElements <= 0) {
        throw new IllegalArgumentException(
            "There must be at least one element per group in ConceptMaps.");
      }

      List<SimpleImmutableEntry<String, ImmutableList<Target>>> elementList = new ArrayList<>();
      for (int e = 0; e < numElements; e++) {
        JsonObject element = elements.get(e).getAsJsonObject();
        String elementCode = element.get(CODE).getAsString();

        JsonArray targets = element.get(TARGET).getAsJsonArray();
        elementList.add(
            new SimpleImmutableEntry<String, ImmutableList<Target>>(
                elementCode, createTargetList(targets)));
      }
      return elementList;
    }

    private ImmutableList<Target> createTargetList(JsonArray targets) {
      int numTargets = targets.size();
      if (numTargets <= 0) {
        throw new IllegalArgumentException(
            "Each element of ConceptMap mush have at least one target.");
      }

      List<Target> targetList = new ArrayList<>();
      for (int t = 0; t < numTargets; t++) {
        JsonObject target = targets.get(t).getAsJsonObject();
        String targetCode = target.get(CODE).getAsString();
        String targetDisplay = target.get(DISPLAY).getAsString();
        targetList.add(new Target(targetCode, targetDisplay));
      }
      return ImmutableList.copyOf(targetList);
    }

    public String getId() {
      return id;
    }

    public List<HarmonizedCode> harmonize(
        String sourceCode, String sourceSystem, String targetSystem)
        throws IllegalArgumentException {
      List<HarmonizedCode> output = new ArrayList<>();
      this.groups.forEach(
          group -> {
            if (group.canHarmonize(sourceSystem, targetSystem)) {
              List<Target> targetList = group.lookup(sourceCode);
              if (targetList.size() == 0) {
                Unmapped unmapped = group.getUnmapped();
                if (unmapped != null) {
                  output.add(createUnmapped(unmapped, sourceCode, group.getTarget()));
                }
              } else {
                for (Target target : targetList) {
                  output.add(
                      HarmonizedCode.create(
                          target.getCode(), target.getDisplay(), group.getTarget(), this.version));
                }
              }
            }
          });
      if (output.size() == 0) {
        output.add(HarmonizedCode.create(sourceCode, "", this.id + "-unharmonized", this.version));
      }
      return output;
    }

    private HarmonizedCode createUnmapped(Unmapped unmapped, String sourceCode, String targetSystem)
        throws IllegalArgumentException {
      HarmonizedCode result;
      switch (unmapped.getMode()) {
        case FIXED:
          result =
              HarmonizedCode.create(
                  unmapped.getTarget().getCode(),
                  unmapped.getTarget().getDisplay(),
                  targetSystem,
                  this.version);
          break;
        case PROVIDED:
          result = HarmonizedCode.create(sourceCode, sourceCode, targetSystem, this.version);
          break;
        default:
          throw new IllegalArgumentException("Unexpected unmapped mode: " + unmapped.getMode());
      }
      return result;
    }
  }

  private Map<String, Conceptmap> localLookup;

  public LocalHarmonizer(JsonObject conceptmap) {
    this.localLookup = new HashMap<String, Conceptmap>();
    Conceptmap localConceptmap = new Conceptmap(conceptmap);
    this.localLookup.put(localConceptmap.getId(), localConceptmap);
  }

  public void addAnotherConceptmap(JsonObject conceptmap) {
    Conceptmap localConceptmap = new Conceptmap(conceptmap);
    if (!this.localLookup.containsKey(localConceptmap.getId())) {
      this.localLookup.put(localConceptmap.getId(), localConceptmap);
    }
  }

  @Override
  public List<HarmonizedCode> harmonize(String sourceCode, String sourceSystem, String conceptmapId)
      throws IllegalArgumentException {
    return harmonizeWithTarget(sourceCode, sourceSystem, "", conceptmapId);
  }

  @Override
  public List<HarmonizedCode> harmonizeWithTarget(
      String sourceCode, String sourceSystem, String targetSystem, String conceptmapId)
      throws IllegalArgumentException {
    if (this.localLookup.containsKey(conceptmapId)) {
      return this.localLookup.get(conceptmapId).harmonize(sourceCode, sourceSystem, targetSystem);
    } else {
      throw new IllegalArgumentException(
          String.format("There is no conceptMap with the give id: '%s'", conceptmapId));
    }
  }
}
