# Whistle 2 Example Plugin

This example plugin showcases various APIs and patterns used by plugins in
Whistle. It demonstrates concepts and implementations of
[Loaders](../../runtime/src/main/java/com/google/cloud/verticals/foundations/dataharmonization/imports/Loader.java),
[Parsers](../../runtime/src/main/java/com/google/cloud/verticals/foundations/dataharmonization/imports/Parser.java),
[Functions](../../runtime/src/main/java/com/google/cloud/verticals/foundations/dataharmonization/builtins/Core.java),
[Targets](../../runtime/src/main/java/com/google/cloud/verticals/foundations/dataharmonization/target/Target.java),
[Registries](../../runtime/src/main/java/com/google/cloud/verticals/foundations/dataharmonization/registry/PackageRegistry.java),
and
[test utilities](../../testutil/src/main/java/com/google/cloud/verticals/foundations/dataharmonization).

The plugin itself does not do anything particularly useful. It's intended for
demonstration purposes.

## Getting started
Check out the
[ExamplePlugin](./src/main/java/com/google/cloud/verticals/foundations/dataharmonization/plugins/example/ExamplePlugin.java)
class, and the various
[tests](./src/test/java/com/google/cloud/verticals/foundations/dataharmonization/plugins/example)
.