# XSD schemas

This directory is a placeholder for the XSD files defining the supported
schemas.

This directory will contain one subdirectory per supported schema. Before
compiling the code targeting a specific schema, ensure that inside the
respective directory, there is a top-level file named `CDA.xsd`. This file can
include any other schema files as needed.

In case there is a need for a custom binding, as mentioned
[here](https://www.oracle.com/technical-resources/articles/javase/jaxb.html#custbind),
 please make sure the `binding.xml` file is also present in the respective
schema subdirectory.
