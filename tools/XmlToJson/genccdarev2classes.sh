#!/bin/bash
#
# Generates JAXB bindings for CCDA Revision 2.

# Set the schema variable to the relative location of the HL7v3 CDA.xsd file
schema="../hl7v3xsd/cda_r2_normativewebedition2010/infrastructure/cda/CDA_SDTC.xsd"

basedir="./src/main"
outdir="${basedir}/java/com/google/cloud/healthcare/etl/xmltojson/xjcgen/ccdarev2"

mkdir -p "${outdir}"
xjc "${schema}" \
  -xmlschema -b \
  "./ccdarev2_binding.xml" \
  -d "${basedir}/java" \
  -p "com.google.cloud.healthcare.etl.xmltojson.xjcgen.ccdarev2.org.hl7.v3"

# The generated files incorrectly depend on com.sun.xml.internal
find "${outdir}" -type f -exec sed -i -e \
  's/com.sun.xml.internal/com.sun.xml/g' {} +
