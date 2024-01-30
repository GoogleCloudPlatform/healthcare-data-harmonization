# Whistle Linter Tool
The Whistle Linter tool will format your whistle files according to the specified formatting rules.

## Getting started
To run the Linter tool in the command line, first navigate to the linter folder. Then execute the gradle command, where the args are the full paths to the files to be formatted, separated by spaces. In the following example, there are no `--include` or `--exclude` flags specified, therefore the linter will apply all default rules. Don't forget to substitute your "workspace-name" in the command.

```bash
cd /google/src/cloud/{{USERNAME}}/{workspace-name}/github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/tools/linter

gradle run --args="/google/path/to/the/filename.wstl /google/path/to/the/second/file.wstl"
```
You can use a bash command to run the linter on all the Whistle files in a directory. For example:

```bash
gradle run --args="$(echo /google/path/to/all/the/files/*.wstl)"
```

If you want to include or exclude certain rules, use the `--include` and `--exclude` flags (or -i and -e) in the gradle command. You can also specify entire groups of rules by using the keywords with which they begin. For example, the following command will include all rules begining with "Spacing", "Semicolons" and "Brackets", *but* it will exclude the "SpacingStatement" rule. The rule names should be separated by commas. Additionally, the indent size can be configured using the `--indent` (or -in) flag. The default indent size is set to 4 spaces.

```bash
cd /google/src/cloud/{{USERNAME}}/{workspace-name}/github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/tools/linter

gradle run --args="/google/path/to/the/filename.wstl --include=Spacing,Semicolons,Brackets --exclude=SpacingStatement --indent=4"
```

If you do not specify the --include or --exclude flag the tool will include all the default linter rules.

<!-- LINT.IfChange -->
## Default Linter Rules
The list of default rules is shown below.

### Brackets Rules
#### Brackets Redundant:
    * applies to expressions (ExprSource nodes);
    * removes the brackets from an ExprSource node if the parent and child are not both operators


### Semicolon Rules
#### Semicolons Redundant:
    * applies to Statement nodes;
    * removes the semicolon from Statements that are within an ExprBlock and followed by a new line;
    * when multiple statements are on the same line, semicolons in between statements will not be removed

### Spacing Rules
#### Spacing Arrays:
    * applies to arrays (SourceListInit nodes);
    * must be run after the SpacingBlock rule;
    * adds an indent in front of every newline inside an array
#### Spacing Block:
    * applies to Block nodes (e.g. within {});
    * ensures there is an indent in front of every line inside an ExprBlock
#### Spacing Condition:
    * applies to Conditional nodes (e.g. if statements);
    * ensures there is a single space after 'if', and before and after 'then', and before and after 'else'.
#### Spacing Function Call:
    * applies to function calls (FunctionCall nodes);
    * must be run after the SpacingBlock rule;
    * adds an indent in front of every newline inside a function call
#### Spacing Function Def:
    * applies to function definitions (FunctionDef nodes);
    * makes sure there is a single space after 'def' or '()' in a FunctionDef node (e.g. def functionName() {})
#### Spacing Operator:
    * applies to Expression nodes;
    * adds a space on either side of the operator in an ExprInfixOp (e.g. a + b);
    * removes any spaces between a ExprPrefixOp/ExprPostfixOp and the expression (e.g. !x, x?)
#### Spacing Statement:
    * applies to Statement nodes;
    * makes sure there is a single space after a variable or field assignment (TARGET_ASSIGN::) in a statement
#### Spacing Trailing:
    * applies to all lines in a file (Program nodes);
    * removes trailing whitespaces preceding newlines or EOF

### Conditional Rules
#### Conditional Redundant:
    * applies to conditional expressions (ExprCondition nodes);
    * removes empty else {} blocks
<!-- LINT.ThenChange(//depot/github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/tools/linter/src/main/java/com/google/cloud/verticals/foundations/dataharmonization/tools/linter/Linter.java:all_rules) -->
