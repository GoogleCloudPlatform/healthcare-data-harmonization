#!/bin/bash
#
# Generates JAXB bindings for CCDA Revision 2.

basedir="./src/main/java"
schemas="${basedir}/com/google/cloud/healthcare/etl/xmltojson/schemas/ccdarev2"
outdir="${basedir}/com/google/cloud/healthcare/etl/xmltojson/xjcgen/ccdarev2"

mkdir -p "${outdir}"
xjc "${schemas}/CDA.xsd" \
  -xmlschema -b \
  "${schemas}/binding.xml" \
  -d "${basedir}" \
  -p "com.google.cloud.healthcare.etl.xmltojson.xjcgen.ccdarev2.org.hl7.v3"

# The generated files incorrectly depend on com.sun.xml.internal
find "${outdir}" -type f -exec sed -i -e \
  's/com.sun.xml.internal/com.sun.xml/g' {} +