#!/bin/bash
#
# Generates JAXB bindings for CCDA Revision 2.

basedir="./src/main"
schemas="${basedir}/javaschemas/ccdarev2/infrastructure/cda"
outdir="${basedir}/java/com/google/cloud/healthcare/etl/xmltojson/xjcgen/ccdarev2"

mkdir -p "${outdir}"
xjc "${schemas}/CDA.xsd" \
  -xmlschema -b \
  "./ccdarev2_binding.xml" \
  -d "${basedir}/java" \
  -p "com.google.cloud.healthcare.etl.xmltojson.xjcgen.ccdarev2.org.hl7.v3"

# The generated files incorrectly depend on com.sun.xml.internal
find "${outdir}" -type f -exec sed -i -e \
  's/com.sun.xml.internal/com.sun.xml/g' {} +
