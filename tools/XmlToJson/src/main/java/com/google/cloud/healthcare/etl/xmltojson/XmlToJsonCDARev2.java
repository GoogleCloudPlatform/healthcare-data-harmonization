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

package com.google.cloud.healthcare.etl.xmltojson;

import com.google.cloud.healthcare.etl.xmltojson.postprocessor.PostProcessor;
import com.google.cloud.healthcare.etl.xmltojson.postprocessor.PostProcessorCdaRev2;
import com.google.cloud.healthcare.etl.xmltojson.postprocessor.PostProcessorException;
import com.google.cloud.healthcare.etl.xmltojson.xjcgen.ccdarev2.org.hl7.v3.POCDMT000040ClinicalDocument;
import java.io.ByteArrayOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.eclipse.persistence.jaxb.JAXBContextProperties;

// TODO(): Consider creating a factory if more than one version of CDA is required.
/** Allows converting XML documents compliant with supported schemas to JSON * */
public class XmlToJsonCDARev2 implements XmlToJson {
  private JAXBContext jc;
  private Marshaller marshaller;
  private Unmarshaller unmarshaller;
  private PostProcessor ppCDARev2;
  private Map<String, String> fieldsToAdd;

  private static final String MEDIA_TYPE = "application/json";

  /**
   * Constructor for XML to JSON parser for CCDA Release 2
   *
   * @throws XmlToJsonException
   */
  public XmlToJsonCDARev2() throws XmlToJsonException {
    createJAXBContext();
    createJAXBMarshaller();
    setJAXBMarshallerProperties();
    createJAXBUnmarshaller();
    createPostProcessor();
  }

  /**
   * Constructor for XML to JSON parser for CCDA Release 2 with additional fields. The fields
   * provided will be added at the top-most level of the JSON produced, i.e. once per JSON object.
   *
   * @param fieldsToAdd Key-value pairs of fields to be added to the top level JSON.
   * @throws XmlToJsonException
   */
  public XmlToJsonCDARev2(Map<String, String> fieldsToAdd) throws XmlToJsonException {
    this();
    this.fieldsToAdd = fieldsToAdd;
  }

  private void createJAXBContext() throws XmlToJsonException {
    try {
      jc =
          JAXBContextFactory.createContext(
              new Class[] {POCDMT000040ClinicalDocument.class}, Collections.emptyMap());
    } catch (JAXBException e) {
      throw new XmlToJsonException("error creating JAXB context", e);
    }
  }

  private void createJAXBMarshaller() throws XmlToJsonException {
    try {
      marshaller = jc.createMarshaller();
    } catch (JAXBException e) {
      throw new XmlToJsonException("error creating JAXB marshaller", e);
    }
  }

  private void setJAXBMarshallerProperties() throws XmlToJsonException {
    try {
      marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT, true);
      marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_ENCODING, "UTF-8");
      marshaller.setProperty(JAXBContextProperties.MEDIA_TYPE, MEDIA_TYPE);
      marshaller.setProperty(JAXBContextProperties.JSON_INCLUDE_ROOT, true);
    } catch (PropertyException e) {
      throw new XmlToJsonException("error setting JAXB marshaller properties", e);
    }
  }

  private void createJAXBUnmarshaller() throws XmlToJsonException {
    try {
      unmarshaller = jc.createUnmarshaller();
    } catch (JAXBException e) {
      throw new XmlToJsonException("error creating jaxb unmarshaller", e);
    }
  }

  private void createPostProcessor() {
    ppCDARev2 = new PostProcessorCdaRev2();
  }

  /**
   * Method in charge of converting an XML CCDA Release 2 into JSON.
   *
   * @param input xml string to be parsed
   * @return conversion of input xml as json string
   * @throws XmlToJsonException
   */
  @Override
  public String parse(String input) throws XmlToJsonException {
    JAXBElement<POCDMT000040ClinicalDocument> unmarshalledJSON = unmarshallXMLToJAXBElement(input);
    String xmlAsJsonStr = marshallJAXBElementToJSON(unmarshalledJSON);
    return postProcess(xmlAsJsonStr);
  }

  private String postProcess(String input) throws XmlToJsonException {
    try {
      if (fieldsToAdd == null || fieldsToAdd.isEmpty()) {
        return ppCDARev2.postProcess(input);
      }
      return ppCDARev2.postProcessWithAdditionalFields(input, fieldsToAdd);
    } catch (PostProcessorException e) {
      throw new XmlToJsonException("error post processing jaxb output", e);
    }
  }

  private String marshallJAXBElementToJSON(JAXBElement<POCDMT000040ClinicalDocument> jaxbXml)
      throws XmlToJsonException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      marshaller.marshal(jaxbXml, out);
      return out.toString("UTF-8");
    } catch (JAXBException | UnsupportedEncodingException e) {
      throw new XmlToJsonException("error marshalling JAXB into JSON string", e);
    }
  }

  private JAXBElement<POCDMT000040ClinicalDocument> unmarshallXMLToJAXBElement(String input)
      throws XmlToJsonException {
    XMLStreamReader xmlReader = createXMLReader(input);
    JAXBElement<POCDMT000040ClinicalDocument> xmlAsObj = unmarshallXML(xmlReader);
    return xmlAsObj;
  }

  private XMLStreamReader createXMLReader(String input) throws XmlToJsonException {
    Reader reader = new StringReader(input);
    XMLInputFactory factory = XMLInputFactory.newInstance();
    XMLStreamReader xmlReader;
    try {
      xmlReader = factory.createXMLStreamReader(reader);
    } catch (XMLStreamException e) {
      throw new XmlToJsonException("error creating XML Stream Reader", e);
    }

    return xmlReader;
  }

  private JAXBElement<POCDMT000040ClinicalDocument> unmarshallXML(XMLStreamReader xmlReader)
      throws XmlToJsonException {
    JAXBElement<POCDMT000040ClinicalDocument> xmlAsObj;
    try {
      xmlAsObj = unmarshaller.unmarshal(xmlReader, POCDMT000040ClinicalDocument.class);
    } catch (JAXBException e) {
      throw new XmlToJsonException("error during JAXB unmarshalling", e);
    }
    return xmlAsObj;
  }
}
