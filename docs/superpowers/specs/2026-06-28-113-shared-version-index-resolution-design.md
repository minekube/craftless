# Shared Version Index Resolution Design

## Problem

Cache preparation now resolves `latest-release` and `latest-snapshot`, but
`JavaRuntimeService` still parses Mojang `version_manifest_v2.json` with its
own exact-version helper. That means `/runtimes/java:resolve` and CLI
`runtimes java resolve --mc latest-release` reject aliases even though the
cache/runtime path accepts them.

This is supervisor metadata plumbing, not a gameplay API problem.

## Goals

- Share Mojang version-index parsing between cache preparation and Java
  runtime resolution.
- Resolve `latest-release` and `latest-snapshot` before Java runtime metadata
  derivation.
- Preserve exact-version cache and Java-runtime behavior.
- Keep file-safe validation for resolved version ids.

## Non-Goals

- Do not add new compiled Fabric lanes.
- Do not claim latest/current or older aliases are runnable.
- Do not add public gameplay actions, generated route families, CLI gameplay
  catalogs, Fabric gameplay bindings, scenario shortcuts, or public
  version-specific APIs.
- Do not change Java runtime provider priority in this phase.

## Acceptance Criteria

- A focused supervisor/CLI test fails before implementation when Java runtime
  resolution treats `latest-release` as an exact version id.
- After implementation, Java runtime resolution derives requirements from the
  concrete version resolved through Mojang `latest.release`.
- Cache preparation still passes the Phase 111 alias tests.
- AGENTS/checklist/evidence record Phase 113 and keep runnable latest/older
  support open.
