# CLI Create Client Loader Version Evidence

## Scope

Phase 124 adds `--loader-version <version>` to `craftless clients create` so
CLI users can send the same optional create-client loader-version lane exposed
by the supervisor API. This does not add gameplay commands, compiled lanes, or
support claims.

## Red

Command:

```sh
mise exec -- gradle :cli:test --tests '*CraftlessCliTest.*loader version*'
```

Result: failed before implementation because `clients create --loader-version
0.16.14` did not include `loaderVersion` in the request body and the usage
string did not mention the flag.

## Green

Command:

```sh
mise exec -- gradle :cli:ktlintFormat :cli:test --tests '*CraftlessCliTest.*loader version*'
```

Result: passed after parsing `--loader-version`, passing it to
`CreateClientRequest`, and updating usage text.

## Local Gates

Commands:

```sh
git diff --check
mise run ci
```

Result: passed. `mise run ci` completed Gradle lint, unused-check, Gradle
tests, and Bun Playwright tests successfully.
