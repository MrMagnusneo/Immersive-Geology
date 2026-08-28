# CI review fix round 1 — report

## Changes

- Expanded the runtime error matcher to reject both bare `[ERROR]` / `[FATAL]`
  records and thread-qualified records.
- Added `scripts/validate_gameplay_jar.py`, a CLI gameplay-JAR validator that:
  - inventories every file in `src/main/resources` and
    `src/generated/resources`, excluding `.cache` paths;
  - parses packaged JSON and `.mcmeta` files;
  - requires expanded, NeoForge-only Immersive Geology metadata and the Java
    21 gameplay entrypoint; and
  - rejects Forge metadata plus sources and datagen JARs.
- Wired that validator before artifact upload. The three independent build
  hash comparison remains unchanged.

## Tests and red-green record

- Added real CLI tests using temporary source roots and ZIP fixtures for clean
  inventory, a missing generated resource, invalid packaged JSON, wrong or
  unexpanded metadata, invalid/non-Java-21 bytecode, Forge metadata, and
  sources/datagen names.
- Added bare `[ERROR]` and `[FATAL]` runtime-log cases to the existing CLI
  test.
- Before implementation, the focused suite failed because bare level records
  passed and the gameplay-JAR CLI did not exist. After implementation,
  `python3 -m unittest discover -s scripts/tests -v` passed: 72 tests.
- `python3 -m py_compile scripts/validate_runtime_log.py
  scripts/validate_gameplay_jar.py` and `git diff --check` passed.

## Scope note

The inventory check compares resource paths, not bytes: `processResources`
minifies JSON, so byte equality would reject valid artifacts. Metadata
validation is intentionally limited to package-level NeoForge identity and
placeholder expansion; JVM linkage is left to the build and runtime GameTest
jobs.

## Commit

`Harden NeoForge CI artifact validation`
