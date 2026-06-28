# Metadata Fallback Naming Removal Design

## Problem

Daemon metadata-resolution code still uses broad old-path naming for two
version manifest fallbacks:

- Java runtime fallback for versions that predate Mojang `javaVersion`
  metadata.
- Native library classifier fallback for manifests that still describe
  platform classifiers separately.

That wording makes normal compatibility handling look like dead product
surface. The code should name the concrete metadata condition it is handling.

## Goals

- Rename Java runtime fallback helpers/tests from old-path wording to
  pre-Java-runtime-metadata wording.
- Rename native library classifier fallback locals from old-path wording to
  classifier/manifest wording.
- Add a source guard so daemon metadata fallback code does not reintroduce the
  stale names.
- Preserve the required Mojang launch variable value `user_type=legacy`.

## Non-Goals

- Do not change Java runtime selection behavior.
- Do not change cache layout, artifact resolution, launch arguments, or
  manifest parsing behavior.
- Do not rename protocol rejection text for removed `task.survival.*`
  scenario ids.
- Do not add gameplay actions, route families, CLI catalogs, Fabric bindings,
  scenario shortcuts, version lanes, or support claims.

## Acceptance Criteria

- A focused daemon test fails before implementation because metadata fallback
  source/test files still contain the stale names.
- After implementation, daemon metadata fallback source/test files use
  pre-metadata/classifier wording.
- Existing Java runtime resolver behavior still passes.
- Existing cache native-library behavior still compiles under full local CI.
- `git diff --check` and `mise run ci` pass locally.
