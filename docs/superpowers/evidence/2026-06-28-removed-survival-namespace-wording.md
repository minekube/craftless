# Removed Survival Namespace Wording Evidence

## Scope

Phase 122 renames active protocol wording for rejected `task.survival.*`
scenario task ids/events. The namespace remains rejected and this phase does
not add gameplay descriptors, route families, CLI gameplay catalogs, Fabric
bindings, scenario shortcuts, version lanes, or support claims.

## Red

Command:

```sh
mise exec -- gradle :protocol:test --tests '*NavigationModelsTest.navigation protocol calls survival namespace removed not legacy*'
```

Result: failed as expected before the wording change because active protocol
source/tests still contained stale old-path survival wording.

## Green

Command:

```sh
mise exec -- gradle :protocol:test --tests '*NavigationModelsTest.*'
```

Result: passed after renaming the active validation messages and rejected
event fixture to removed-survival-scenario wording.

## Local Gates

Commands:

```sh
git diff --check
mise run ci
```

Result: passed after formatting the new protocol test imports with the
configured ktlint layout. `mise run ci` completed Gradle lint, unused-check,
Gradle tests, and Bun Playwright tests successfully.
