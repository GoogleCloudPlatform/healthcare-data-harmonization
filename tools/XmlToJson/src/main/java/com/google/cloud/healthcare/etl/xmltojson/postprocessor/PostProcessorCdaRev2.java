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

package com.google.cloud.healthcare.etl.xmltojson.postprocessor;

import com.google.cloud.healthcare.etl.xmltojson.postprocessor.transformers.DeletionTransformer;
import com.google.cloud.healthcare.etl.xmltojson.postprocessor.transformers.SingleTransformer;
import com.google.cloud.healthcare.etl.xmltojson.postprocessor.transformers.Transformer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// TODO(b/174143024): In case more releases need to be supported, we should consider extracting
// common code from this class.
/**
 * Class in charge of post processing the output of JAXB for a CDA Rev2 XML document. The
 * transformations applied on this class are expected to be at the JSON key level, for instance one
 * key might be converted from single element to array.
 */
public class PostProcessorCdaRev2 implements PostProcessor {
  /** List of transformations to be applied. */
  private List<Transformer> transformers;

  private static int INDENTATION = 4;

  private static String REFERENCE_KEY = "reference";
  private static String THUMBNAIL_KEY = "thumbnail";
  private static String HIGH_KEY = "high";
  private static String LOW_KEY = "low";
  private static String WIDTH_KEY = "width";
  private static String CENTER_KEY = "center";
  private static String NULLFLAVOR_KEY = "nullFlavor";

  /** Constructor for CCDA Release 2 post processor */
  public PostProcessorCdaRev2() {
    transformers = new ArrayList<>();

    /**
     * For CCDA Release 2 the following keys are incorrectly parsed as Arrays by JAXB, even though
     * the XSD schema indicates them as single elements.
     */
    Set<String> keysToSingle = new HashSet<>();
    keysToSingle.add(REFERENCE_KEY);
    keysToSingle.add(THUMBNAIL_KEY);
    keysToSingle.add(HIGH_KEY);
    keysToSingle.add(LOW_KEY);
    keysToSingle.add(WIDTH_KEY);
    keysToSingle.add(CENTER_KEY);
    transformers.add(new SingleTransformer(keysToSingle));

    /**
     * For CCDA Release 2 the nullFlavor key, which indicates missing data, is not eliminated by
     * JAXB. However for the next stage whistle mapping it is important this key to be not present.
     */
    Set<String> keysToDelete = new HashSet<>();
    keysToDelete.add(NULLFLAVOR_KEY);
    transformers.add(new DeletionTransformer(keysToDelete));
  }

  /**
   * Method in charge of post processing a CCDA Release 2 json string.
   *
   * @param jsonInput json string to be post processed
   * @return json string transformed to be compiant with CCDA release 2
   * @throws PostProcessorException
   */
  @Override
  public String postProcess(String jsonInput) throws PostProcessorException {
    JSONObject jsonObj = doPostProcess(jsonInput);
    return jsonObj.toString(INDENTATION);
  }

  /**
   * Method in charge of post processing a CCDA Release 2 json string with additional fields added
   * to the top level JSON object (i.e. on the same level as the FHIR resources).
   *
   * @param jsonInput json string to be post processed
   * @param fields the data source to be included in the JSON
   * @return json string transformed to be compiant with CCDA release 2
   * @throws PostProcessorException
   */
  @Override
  public String postProcessWithAdditionalFields(String jsonInput, Map<String, String> fields)
      throws PostProcessorException {
    JSONObject jsonObj = doPostProcess(jsonInput);
    if (!jsonObj.isEmpty()) {
      for (Map.Entry<String, String> field : fields.entrySet()) {
        jsonObj.put(field.getKey(), field.getValue());
      }
    }
    return jsonObj.toString(INDENTATION);
  }

  private JSONObject doPostProcess(String jsonInput) throws PostProcessorException {
    JSONObject jsonObj = unmarshallJSON(jsonInput);
    applyTransforms(jsonObj);
    return jsonObj;
  }

  private JSONObject unmarshallJSON(String jsonStr) throws PostProcessorException {
    JSONObject unmarshalledJson = null;
    try {
      unmarshalledJson = new JSONObject(jsonStr);
    } catch (JSONException e) {
      throw new PostProcessorException("error unmarshalling JSON", e);
    }
    return unmarshalledJson;
  }

  /**
   * This method applies transformations on each key of a JSON object. The transformations are done
   * in place, meaning the original JSON object is mutated.
   */
  private void applyTransforms(JSONObject curJson) throws PostProcessorException {
    // The transformations might mutate the original set of keys, so we copy them first.
    Set<String> keysSet = new HashSet<>();
    keysSet.addAll(curJson.keySet());
    for (String key : keysSet) {
      for (Transformer transformer : transformers) {
        transformer.transform(curJson, key);
      }
      // If a key was not deleted, it might contain JSON Objects or JSON Arrays which need to
      // be transformed recursively.
      if (curJson.has(key)) {
        transformChildren(curJson, key);
      }
    }
  }

  /**
   * This method applies transformations on children JSONObject or JSONArray of a parent JSONObject.
   * If the result is empty, then it is deleted from the parent.
   */
  private void transformChildren(JSONObject curJson, String key) throws PostProcessorException {
    Object childObject = curJson.opt(key);
    if (childObject instanceof JSONObject) {
      applyTransforms((JSONObject) childObject);
      deleteIfEmpty(curJson, key);
    } else if (childObject instanceof JSONArray) {
      JSONArray jsonArrTransformed = transformChildrenArray((JSONArray) childObject);
      curJson.put(key, jsonArrTransformed);
      deleteIfEmpty(curJson, key);
    }
  }

  /**
   * This method applies transformations on each element of a JSON array. For performance reasons,
   * the transformations are done in a functional manner, i.e., a new array is generated; then the
   * caller needs to mutate the parent.
   */
  private JSONArray transformChildrenArray(JSONArray jsonArr) throws PostProcessorException {
    JSONArray newJSONArr = new JSONArray();
    for (int i = 0; i < jsonArr.length(); i++) {
      Object arrayEl = jsonArr.opt(i);
      if (arrayEl instanceof JSONObject) {
        applyTransforms((JSONObject) arrayEl);
        addIfNotEmpty(newJSONArr, (JSONObject) arrayEl);
      } else if (arrayEl instanceof JSONArray) {
        JSONArray subArr = transformChildrenArray((JSONArray) arrayEl);
        addIfNotEmpty(newJSONArr, subArr);
      } else {
        // The object is a primitive, i.e. Number, Boolean, String
        newJSONArr.put(arrayEl);
      }
    }
    return newJSONArr;
  }

  private void addIfNotEmpty(JSONArray dst, JSONObject src) {
    if (dst != null && src != null && src.keys().hasNext()) {
      dst.put(src);
    }
  }

  private void addIfNotEmpty(JSONArray dst, JSONArray src) {
    if (dst != null && src != null && src.length() > 0) {
      dst.put(src);
    }
  }

  private void deleteIfEmpty(JSONObject curJson, String key) {
    Object child = curJson.opt(key);
    if (child != null) {
      if (child instanceof JSONObject && !((JSONObject) child).keys().hasNext()) {
        curJson.remove(key);
      } else if (child instanceof JSONArray && ((JSONArray) child).length() == 0) {
        curJson.remove(key);
      }
    }
  }
}
