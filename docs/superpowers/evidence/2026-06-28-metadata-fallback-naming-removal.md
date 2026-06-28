# Metadata Fallback Naming Removal Evidence

## Scope

Phase 121 removes broad old-path naming from daemon metadata fallback
internals. Java runtime fallback still supports versions before Mojang
`javaVersion` metadata, native library resolution still supports classifier
metadata, and the required Mojang launch literal `user_type=legacy` remains
unchanged.

## Red

Command:

```sh
mise exec -- gradle :daemon:test --tests '*JavaRuntimeRequirementResolverTest.metadata fallback internals use pre metadata naming*'
```

Result before implementation: failed as expected because daemon source/test
files still contained the stale helper, local, and test names.

## Green

Command:

```sh
mise exec -- gradle :daemon:test --tests '*JavaRuntimeRequirementResolverTest.*'
```

Result after implementation: passed locally.

## Local Gates

Commands:

```sh
git diff --check
mise run ci
```

Result: both commands passed locally. `mise run ci` completed Gradle lint,
unused-check/detekt, Gradle tests, and Bun Playwright tests successfully.
