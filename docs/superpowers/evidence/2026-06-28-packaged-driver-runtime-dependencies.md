# Phase 101 Packaged Driver Runtime Dependencies Evidence

## Scope

Phase 101 closes the packaged Fabric driver runtime dependency gap found after
Phase 100:

- the staged mod now carries Craftless runtime module jars;
- the staged mod now carries representative Kotlin, kotlinx, Ktor, and config
  runtime jars required by self-attach;
- `package-cli` now fails if the staged mod lacks nested jars, Kotlin stdlib,
  coroutines, or Ktor HTTP runtime jars.

This is packaging/runtime closure only. It adds no public gameplay action,
static descriptor, generated route family, CLI gameplay catalog, Fabric
gameplay binding, scenario shortcut, compiled lane, version-specific public
API, live gameplay claim, or completion claim.

## Root Cause

`mise run package-cli` only verified that the staged Fabric mod contained
`fabric.mod.json`. The remapped jar initially contained the new Fabric
self-attach classes but no nested dependency jars. A real Fabric client
classloader would therefore miss Craftless driver/runtime/protocol classes and
Kotlin/Ktor runtime classes.

## Red Guards

- `mise exec -- gradle :protocol:test --tests '*NamespacePolicyTest.fabric driver mod declares nested runtime dependencies*'`
  - Result: failed as expected before implementation.
  - Evidence: missing `include(...)` declarations and package nested-jar smoke
    checks.

## Green Checks

- `mise exec -- gradle :protocol:test --tests '*NamespacePolicyTest.fabric driver mod declares nested runtime dependencies*'`
  - Result: `BUILD SUCCESSFUL`.
- `mise run package-cli`
  - Result: `BUILD SUCCESSFUL`.
- Staged jar inspection after package smoke found nested runtime jars including:
  `driver-api`, `driver-runtime`, `protocol`, `daemon`, `bridge-hmc`,
  `kotlin-stdlib`, `kotlinx-coroutines-core-jvm`, `kotlinx-serialization-core-jvm`,
  `kotlinx-serialization-json-jvm`, `ktor-server-core-jvm`,
  `ktor-server-cio-jvm`, `ktor-client-core-jvm`, `ktor-client-cio-jvm`,
  `ktor-http-jvm`, `ktor-network-jvm`, and `ktor-utils-jvm`.

## Final Local Gates

- `git diff --check`
  - Result: exit `0`.
- `mise exec -- gradle :protocol:test :driver-fabric:test`
  - Result: `BUILD SUCCESSFUL`.
- `mise exec -- gradle :protocol:ktlintCheck :protocol:detekt :driver-fabric:ktlintCheck :driver-fabric:detekt`
  - Result: `BUILD SUCCESSFUL`.
- `mise run package-cli`
  - Result: `BUILD SUCCESSFUL`.
