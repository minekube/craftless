# Version-Aware Driver Mod Selection Design

## Problem

Craftless now installs and packages a Fabric driver mod, but the launch
boundary still asks `ClientRuntimeDriverModProvider` for a mod by loader only.
That is too weak for the multi-version goal: once Craftless ships more than
one compiled driver lane, the launch system must select the driver artifact
for the requested Minecraft/Fabric runtime instead of treating Fabric as one
global mod jar.

This is a system-boundary problem, not a gameplay API problem. The fix must
not add static gameplay actions, public route families, or version support
claims.

## Goals

- Pass a version-aware request into `ClientRuntimeDriverModProvider`.
- Include the loader, requested Minecraft version, and resolved/requested
  loader version in that request.
- Keep the current `CRAFTLESS_FABRIC_DRIVER_MOD` single-jar environment
  fallback working for today's packaged release.
- Preserve daemon/CLI independence from the `driver-fabric` compile classpath.
- Record that this is a prerequisite for multiple runtime lanes, not evidence
  that latest/current or representative older support is complete.

## Non-Goals

- Do not add a new Fabric compiled lane.
- Do not add a driver-mod manifest format in this phase.
- Do not claim broad Minecraft version support.
- Do not add public gameplay actions, generated route families, CLI gameplay
  catalogs, Fabric gameplay bindings, or scenario shortcuts.
- Do not change the generated per-client OpenAPI contract.

## Design

Add a daemon-owned `ClientRuntimeDriverModRequest` value:

```kotlin
data class ClientRuntimeDriverModRequest(
    val loader: Loader,
    val minecraftVersion: String,
    val loaderVersion: String?,
)
```

`WorkspaceClientRuntimeDriverFactory.prepare` already receives a
`CreateClientRequest` and then gets a `CachePrepareResult` with the resolved
Fabric loader version. After cache preparation, it should create a
`ClientRuntimeDriverModRequest` from:

- `request.loader`;
- `request.version`;
- `cache.loaderVersion`.

`ClientRuntimeDriverModProvider` should accept that request. The configured
provider still returns `CRAFTLESS_FABRIC_DRIVER_MOD` for Fabric requests and
`null` for other loaders. That keeps the current CLI and install release
working while allowing later providers to select by version/lane without
changing launcher code again.

## Acceptance Criteria

- A focused daemon test proves the provider receives loader `FABRIC`,
  Minecraft version `1.21.6`, and resolved loader version `0.17.2`.
- Existing configured and packaged driver-mod CLI smoke tests still pass.
- `ClientRuntimeDriverModProvider` no longer exposes a loader-only `modFor`
  method.
- AGENTS and the project checklist record Phase 107 and keep broader
  latest/current plus representative older runnable support open.
- Local gates pass and changes are pushed to `main`.
