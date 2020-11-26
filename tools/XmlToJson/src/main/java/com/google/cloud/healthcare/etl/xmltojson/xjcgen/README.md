# XJC Generated Classes for XmlToJson tool

This directory will contain the XJC generated code after building this project.
There should be one subdirectory per supported schema.

## Why to generate JAVA classes using XJC?

XJC generated classes are required in order to unmarshall XML in to JAVA
objects, by using the JAXB library. XJC generates the JAVA classes based on the
schema definition of the XML files to be converted.

## How to generate the JAVA classes using XJC?

The first step to generate the JAVA classes it is to define any required
customizations, as explained
[here](https://www.oracle.com/technical-resources/articles/javase/jaxb.html#custbind).

The customizations used in this project, can be find on each subdirectory
containing the schemas. There should be a file called binding.xml per each
supported schema.

As an example of the usage of binding.xml and XJC, the following command should
allow to regenerate the JAVA objects mentioned in this document:

```shell
$ xjc CDA.xsd -xmlschema -b binding.xml
```

In the previous command, `CDA.xsd` refers to the schema of the specific
[CCDA](http://www.hl7.org/implement/standards/product_brief.cfm?product_id=492)
revision of interest.