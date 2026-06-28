# Official Fabric Runtime Dependency Packaging Design

## Problem

Phase 147 wired the latest/current official Fabric lane to the shared
`driver-fabric-attach` module. The official module now compiles, but its jar
only contains the official entrypoint and metadata-only backend classes. It
does not carry the shared Craftless runtime, Ktor, Kotlin, serialization, and
transport dependencies required for the entrypoint to execute self-attach in a
real Fabric client.

That means the next launch/attach probe would fail for packaging reasons
before testing the actual official lane runtime boundary.

## Goal

Nest the runtime dependencies required by the official lane's metadata-only
self-attach path into the `driver-fabric-official` jar, matching the existing
Fabric nested-jar packaging pattern where appropriate.

## Non-Goals

- Do not add `driver-fabric-official` to the public packaged driver manifest.
- Do not claim Minecraft 26.x/latest support.
- Do not add public gameplay actions, static catalogs, route families, CLI
  gameplay commands, or scenario shortcuts.
- Do not copy Yarn/remap gameplay bindings into the official module.
- Do not run the full real-client launch/attach smoke in this phase.

## Design

`driver-fabric-official/build.gradle.kts` should keep compile dependencies on
the stable Craftless modules and add Fabric Loom `include(...)` entries for
the nested jars needed by the metadata-only self-attach path:

- `protocol`;
- `driver-api`;
- `driver-runtime`;
- `driver-fabric-attach`;
- Ktor client/server CIO/core and transitive runtime jars used by the attach
  path;
- Kotlin stdlib, coroutines, serialization, and IO jars required by those
  dependencies.

The official module must not include `driver-fabric`, `daemon`, `bridge-hmc`,
or any gameplay-binding module. The official jar remains an internal probe
artifact only.

## Acceptance

- Architecture tests prove the official build includes shared attach/runtime
  dependencies and excludes `driver-fabric`, `daemon`, and `bridge-hmc`.
- `:driver-fabric-official:jar` produces nested jars under `META-INF/jars/`.
- The nested jars include `driver-fabric-attach`, `driver-runtime`,
  `driver-api`, `protocol`, Kotlin stdlib, Ktor client/server core/CIO, and
  serialization runtime jars.
- `mise run fabric-lane-check-latest-official` still writes `status=compiled`.
- No public driver manifest entry or gameplay API is added.
