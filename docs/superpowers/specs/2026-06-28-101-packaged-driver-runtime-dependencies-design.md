# Packaged Driver Runtime Dependencies Design

## Problem

Phase 100 added Fabric driver self-attach transport, but `mise run package-cli`
initially staged a Fabric mod jar that contained only `driver-fabric` classes.
The jar did not include nested Craftless driver/runtime/protocol jars, Kotlin
runtime jars, serialization jars, coroutine jars, or Ktor runtime jars.

That meant the packaged Docker/CLI path could build successfully while still
failing in a real Fabric client classloader.

## Goals

- Nest the Craftless runtime modules needed by the Fabric driver mod:
  `protocol`, `driver-api`, `driver-runtime`, `daemon`, and `bridge-hmc`.
- Nest the Kotlin, kotlinx.serialization, kotlinx.coroutines, kotlinx-io, Ktor,
  and Typesafe config runtime jars required by the in-client self-attach
  endpoint.
- Keep the package check from accepting a staged mod that has no nested jars or
  misses representative transitive runtime jars.
- Keep this as packaging/runtime closure only.

## Non-Goals

- Do not add public gameplay actions, static descriptors, route families, CLI
  gameplay catalogs, Fabric bindings, or scenario shortcuts.
- Do not make daemon or CLI compile against `driver-fabric`.
- Do not claim live in-client self-attach or final gameplay completion from
  packaging alone.

## Acceptance Criteria

- A repository policy test proves `driver-fabric/build.gradle.kts` declares
  Fabric Loom `include(...)` entries for Craftless runtime modules and
  representative self-attach transitive runtime jars.
- `mise run package-cli` verifies the staged
  `craftless-driver-fabric.jar` contains `fabric.mod.json`, nested jars,
  Kotlin stdlib, kotlinx.coroutines, and Ktor HTTP runtime jars.
- Package smoke proves the staged Fabric mod's `fabric.mod.json` includes a
  `jars` section with those runtime jars.
- Focused protocol policy tests, package smoke, lint/static analysis, and diff
  checks pass locally.
