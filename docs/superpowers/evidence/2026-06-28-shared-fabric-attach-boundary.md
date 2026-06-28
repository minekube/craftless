# Shared Fabric Attach Boundary Evidence

Date: 2026-06-28

## Scope

Phase 147 extracts Fabric self-attach and Ktor loopback transport into
`driver-fabric-attach` so the verified Yarn/remap lane and the latest/current
official lane share attach/session handoff infrastructure.

This evidence does not claim latest/current gameplay support. The 26.x official
lane is still not packaged as a supported driver artifact and is still missing
real launch, attached generated OpenAPI/actions/resources, SSE, packaged
distribution, and public API/CLI gameplay evidence.

## Changes

- Added `driver-fabric-attach` as a neutral shared module.
- Moved `FabricDriverAttachEnvironment`, `FabricDriverLoopbackEndpoint`, and
  `FabricDriverSelfAttach` out of `driver-fabric`.
- Moved existing self-attach tests into `driver-fabric-attach`.
- Rewired `driver-fabric` to consume `project(":driver-fabric-attach")`.
- Rewired `driver-fabric-official` to consume `project(":driver-fabric-attach")`
  and not depend on `project(":driver-fabric")`.
- Added `OfficialFabricDriverBackend` as metadata-only runtime handoff for the
  official lane.
- Updated the official entrypoint to call
  `FabricDriverSelfAttach.startFromEnvironment`.

## Red Evidence

Before implementation, the architecture guard failed:

```sh
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.official lane uses shared fabric attach boundary without depending on yarn remap lane'
```

Observed:

```text
FabricDriverModuleTest > official lane uses shared fabric attach boundary without depending on yarn remap lane() FAILED
```

## Green Evidence

Shared attach tests:

```sh
mise exec -- gradle :driver-fabric-attach:test
```

Observed:

```text
BUILD SUCCESSFUL
```

Official lane compile boundary:

```sh
mise exec -- gradle :driver-fabric-official:compileKotlin :driver-fabric-official:processResources :driver-fabric-official:jar
```

Observed:

```text
BUILD SUCCESSFUL
```

Current Yarn/remap lane regression:

```sh
mise exec -- gradle :driver-fabric:test
```

Observed:

```text
BUILD SUCCESSFUL
```

Latest official lane probe:

```sh
mise run fabric-lane-check-latest-official
cat build/reports/fabric-lane-check-latest-official.status
```

Observed:

```text
status=compiled
```

Lint and whitespace:

```sh
mise exec -- gradle lint
git diff --check
```

Observed:

```text
BUILD SUCCESSFUL
```

## Guardrails

- No packaged 26.x driver manifest entry was added.
- No public gameplay action descriptor/catalog was added.
- No version-specific public route family was added.
- No survival shortcut was added.
- No final latest/current support claim was added.
