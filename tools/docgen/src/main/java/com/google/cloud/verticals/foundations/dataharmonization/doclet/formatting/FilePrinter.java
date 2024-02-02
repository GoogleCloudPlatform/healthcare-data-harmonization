// Copyright 2022 Google LLC.
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
package com.google.cloud.verticals.foundations.dataharmonization.doclet.formatting;

import static java.util.stream.Collectors.joining;

import com.google.cloud.verticals.foundations.dataharmonization.doclet.model.ArgumentDoc;
import com.google.cloud.verticals.foundations.dataharmonization.doclet.model.FunctionDoc;
import com.google.cloud.verticals.foundations.dataharmonization.doclet.model.PackageDoc;
import com.google.cloud.verticals.foundations.dataharmonization.doclet.model.ReturnDoc;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/** Prints the given packages to text files (one file per package) using the given format. */
public class FilePrinter implements Printer<Map<Path, String>> {

  private final MarkupFormat format;
  private static final int PACKAGE_NAME_HEADING_LEVEL = 1;
  private static final int FUNCTIONS_TARGETS_HEADING_LEVEL = 2;
  private static final int IMPORT_HEADING_LEVEL = 2;
  private static final int INDIVIDUAL_FUNCTION_HEADING_LEVEL = 3;
  private static final int FUNCTION_PARTS_HEADING_LEVEL = 4;

  public FilePrinter(MarkupFormat format) {
    this.format = format;
  }

  @Override
  public Map<Path, String> format(Collection<PackageDoc> docs) {
    return docs.stream()
        .collect(
            Collectors.toMap(
                d -> Path.of(String.format("%s.%s", d.packageName(), format.fileExt())),
                this::formatPackageDoc));
  }

  private String formatPackageDoc(PackageDoc doc) {
    return format.startHeading(PACKAGE_NAME_HEADING_LEVEL)
        + String.format("Package %s", doc.packageName())
        + format.endHeading(PACKAGE_NAME_HEADING_LEVEL)
        + format.paragraphBreak()
        + format.autoTOC()
        + importStatement(doc)
        + functions(doc.packageName(), doc.functions(), "Functions", this::function)
        + format.paragraphBreak()
        + functions(doc.packageName(), doc.targets(), "Targets", this::target);
  }

  private String functions(
      String pkg,
      ImmutableList<FunctionDoc> functions,
      String title,
      BiFunction<String, FunctionDoc, String> function) {
    if (functions.isEmpty()) {
      return "";
    }

    return format.startHeading(FUNCTIONS_TARGETS_HEADING_LEVEL)
        + format.text(title)
        + format.endHeading(FUNCTIONS_TARGETS_HEADING_LEVEL)
        + functions.stream()
            .distinct()
            .sorted(Comparator.comparing(FunctionDoc::name))
            .map(t -> function.apply(pkg, t))
            .collect(joining());
  }

  private String importStatement(PackageDoc doc) {
    if (doc.packageName().equals("builtins")) {
      return format.startHeading(IMPORT_HEADING_LEVEL)
          + format.text("Import")
          + format.endHeading(IMPORT_HEADING_LEVEL)
          + format.paragraphBreak()
          + format.text(
              String.format(
                  "The %s package does not need to be imported, or prefixed in front of any"
                      + " functions. These functions are available everywhere.",
                  doc.packageName()))
          + format.paragraphBreak();
    }

    return format.startHeading(IMPORT_HEADING_LEVEL)
        + format.text("Import")
        + format.endHeading(IMPORT_HEADING_LEVEL)
        + format.paragraphBreak()
        + format.text(
            String.format(
                "The %s package can be imported by adding this code to the top of your Whistle"
                    + " file:",
                doc.packageName()))
        + format.paragraphBreak()
        + format.startCodeBlock()
        + format.text(String.format("import \"class://%s\"", doc.className()))
        + format.endCodeBlock()
        + format.paragraphBreak();
  }

  public String function(String pkg, FunctionDoc doc) {
    return format.startHeading(INDIVIDUAL_FUNCTION_HEADING_LEVEL)
        + format.text(doc.name())
        + format.endHeading(INDIVIDUAL_FUNCTION_HEADING_LEVEL)
        + formatFunctionSig(pkg, doc)
        + format.paragraphBreak()
        + format.startHeading(FUNCTION_PARTS_HEADING_LEVEL)
        + format.text("Arguments")
        + format.endHeading(FUNCTION_PARTS_HEADING_LEVEL)
        + formatArgs(doc.arguments())
        + format.paragraphBreak()
        + format.startHeading(FUNCTION_PARTS_HEADING_LEVEL)
        + format.text("Description")
        + format.endHeading(FUNCTION_PARTS_HEADING_LEVEL)
        + format.markup(doc.body())
        + format.paragraphBreak()
        + (doc.thrownExceptions().isEmpty()
            ? ""
            : (format.startHeading(FUNCTION_PARTS_HEADING_LEVEL)
                + format.text("Throws")
                + format.endHeading(FUNCTION_PARTS_HEADING_LEVEL)
                + formatThrows(doc.thrownExceptions())
                + format.paragraphBreak()));
  }

  private String formatThrows(ImmutableList<ReturnDoc> thrownExceptions) {
    return format.startList()
        + thrownExceptions.stream()
            .map(
                te ->
                    format.startListItem()
                        + format.startBold()
                        + te.type()
                        + format.endBold()
                        + format.text(" - ")
                        + format.markup(te.body())
                        + format.endListItem())
            .collect(joining())
        + format.endList();
  }

  private String target(String pkg, FunctionDoc doc) {
    return format.startHeading(INDIVIDUAL_FUNCTION_HEADING_LEVEL)
        + format.text(doc.name())
        + format.endHeading(INDIVIDUAL_FUNCTION_HEADING_LEVEL)
        + formatTargetSig(pkg, doc)
        + format.paragraphBreak()
        + format.startHeading(FUNCTION_PARTS_HEADING_LEVEL)
        + format.text("Arguments")
        + format.endHeading(FUNCTION_PARTS_HEADING_LEVEL)
        + formatArgs(doc.arguments())
        + format.paragraphBreak()
        + format.startHeading(FUNCTION_PARTS_HEADING_LEVEL)
        + format.text("Description")
        + format.endHeading(FUNCTION_PARTS_HEADING_LEVEL)
        + format.markup(doc.body())
        + format.paragraphBreak();
  }

  private String formatArgs(List<ArgumentDoc> args) {
    return args.stream().map(this::formatArg).collect(joining(format.paragraphBreak()));
  }

  private String formatFunctionSig(String pkg, FunctionDoc doc) {
    return format.startInlineCode()
        + format.text(
            (pkg.equals("builtins") ? "" : (pkg + "::"))
                + doc.name()
                + String.format(
                    "(%s)",
                    doc.arguments().stream().map(this::formatSigArg).collect(joining(", "))))
        + format.endInlineCode()
        + format.text(" returns ")
        + format.startInlineCode()
        + format.text(doc.returns().type())
        + format.endInlineCode()
        + format.markup(
            (Strings.isNullOrEmpty(doc.returns().body()) ? "" : (" - " + doc.returns().body())));
  }

  private String formatTargetSig(String pkg, FunctionDoc doc) {
    return format.startInlineCode()
        + format.text(
            (pkg.equals("builtins") ? "" : (pkg + "::"))
                + doc.name()
                + String.format(
                    "(%s)", doc.arguments().stream().map(this::formatSigArg).collect(joining(", ")))
                + ": ...")
        + format.endInlineCode();
  }

  private String formatSigArg(ArgumentDoc arg) {
    return String.format("%s: %s", arg.name(), arg.type());
  }

  private String formatArg(ArgumentDoc arg) {
    return format.startBold()
        + format.text(arg.name())
        + format.endBold()
        + format.text(": ")
        + format.startInlineCode()
        + format.text(arg.type())
        + format.endInlineCode()
        + format.markup(Strings.isNullOrEmpty(arg.body()) ? "" : (" - " + arg.body()))
        + format.paragraphBreak();
  }
}
