# Driver Mod Manifest Miss Design

## Problem

Installed CLI distributions ship `driver-mods.json` keyed by Loader,
Minecraft version, and Loader version. When that manifest is present but the
requested Fabric runtime lane is not listed, `ConfiguredClientRuntimeDriverModProvider`
currently falls back to `CRAFTLESS_FABRIC_DRIVER_MOD` if it is set. In a
packaged distribution that fallback is the current compiled driver jar, so a
latest or older Fabric lane can be launched with an incompatible driver mod or
without a useful attach path.

That is unsafe for multi-version support. A manifest-backed distribution must
fail explicitly when no compatible Fabric driver mod exists for the prepared
runtime lane.

## Goals

- Treat `CRAFTLESS_DRIVER_MOD_MANIFEST` as authoritative for Fabric driver-mod
  lane selection.
- Return a clear create-client error when a manifest is configured but contains
  no matching Fabric entry for the prepared Minecraft version and Loader
  version.
- Preserve the single-driver fallback only when no manifest is configured.
- Keep exact manifest entries and loader-version wildcard entries working.

## Non-Goals

- Do not add new compiled Fabric lanes.
- Do not mark latest/current or older versions as newly supported.
- Do not add gameplay actions, route families, CLI gameplay catalogs, Fabric
  gameplay bindings, scenario shortcuts, or public version-specific APIs.
- Do not change cache resolution or Java/runtime selection.

## Acceptance Criteria

- A focused provider test fails before implementation because a manifest miss
  falls back to `CRAFTLESS_FABRIC_DRIVER_MOD`.
- After implementation, a configured manifest miss for Fabric throws a
  machine-readable error that names the requested loader, Minecraft version,
  and loader version.
- Exact manifest entries still win over a single-driver fallback.
- Single-driver fallback still works when no manifest is configured.
- Packaged CLI server-start evidence shows a manifest miss returns HTTP 400
  for client creation instead of copying the fallback driver jar.
- `mise exec -- gradle :daemon:test --tests '*ConfiguredClientRuntimeDriverModProviderTest.*' --tests '*LocalSessionApiServerTest.*driver mod manifest*'`
  passes.
- `mise exec -- gradle :cli:test --tests '*CraftlessCliTest.*driver mod manifest*'`
  passes.
- `git diff --check` and `mise run ci` pass locally.
