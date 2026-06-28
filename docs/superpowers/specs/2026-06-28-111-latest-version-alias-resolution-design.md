# Latest Version Alias Resolution Design

## Problem

Craftless cache preparation currently treats `minecraftVersion` as a concrete
Mojang version id. Users and agents need a generic way to ask for the current
Mojang release or snapshot without hard-coding the concrete id before calling
the supervisor API. Mojang already publishes those aliases in
`version_manifest_v2.json`, but Craftless must resolve them before deriving
cache handles, Fabric metadata URLs, launch metadata, Java requirements, and
prepared manifest paths.

This is a supervisor runtime-version problem, not a gameplay API problem.

## Goals

- Accept `latest-release` and `latest-snapshot` as cache preparation aliases.
- Resolve aliases through Mojang `latest.release` and `latest.snapshot` in the
  version index before reading the concrete version manifest.
- Derive prepared cache handles, launch metadata, Fabric loader URLs, Java
  selection context, and returned `CachePrepareResult.minecraftVersion` from
  the concrete resolved id.
- Preserve exact concrete version behavior.
- Keep latest/current runnable support open until a compatible compiled driver
  lane and real launch evidence exist.

## Non-Goals

- Do not add new compiled Fabric lanes.
- Do not claim that latest release or latest snapshot is runnable.
- Do not add public gameplay actions, generated route families, CLI gameplay
  catalogs, Fabric gameplay bindings, scenario shortcuts, or public
  version-specific APIs.
- Do not add a separate public alias field to the protocol result in this
  phase.

## Acceptance Criteria

- A focused daemon test fails before implementation when `latest-release` is
  treated as an exact version id.
- After implementation, `latest-release` resolves to the concrete Mojang
  release id before cache handles and artifact sources are built.
- After implementation, `latest-snapshot` resolves to the concrete Mojang
  snapshot id before cache handles and artifact sources are built.
- Fabric metadata resolution, when used with an alias, requests Fabric metadata
  for the concrete resolved id.
- Exact version requests still prepare the same cache handles as before.
- AGENTS/checklist/evidence record Phase 111 and keep runnable latest/older
  support open.
