# Quick Run

This is a tiny gradle config intended to provide a simple entry point to running
Whistle mappings until proper tooling is added to this repository.

It can be used like:

```shell
> cd quickrun
> gradle run --args="-m /<path to a whistle config>/.../<something>.wstl"
```

To test your Whistle matching and merging configs with an input file, run the unit tests.

For example:

```shell
> cd quickrun
> gradle run --args="-m /<path to repository>/data_harmonization/mappings/reconciliation/unit_tests/unit_test.wstl"
```

The input and output files, located in `mappings/reconciliation/unit_tests/merge` and `mappings/reconciliation/unit_tests/matching` can be modified as needed.