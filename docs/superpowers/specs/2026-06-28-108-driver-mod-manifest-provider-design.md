# Driver Mod Manifest Provider Design

## Problem

Phase 107 made driver-mod selection version-aware, but the configured provider
still only has one single-jar environment variable:
`CRAFTLESS_FABRIC_DRIVER_MOD`. That is enough for the current release, but it
does not give packaged distributions a generic way to carry multiple driver
artifacts for different Minecraft/Fabric lanes.

The next system-level step is a manifest-backed provider. It should let a
distribution or test environment point Craftless at a local manifest of driver
mod artifacts without coupling the daemon or CLI to `driver-fabric` classes.

## Goals

- Add `CRAFTLESS_DRIVER_MOD_MANIFEST` to `ConfiguredClientRuntimeDriverModProvider`.
- Select manifest entries by loader, Minecraft version, and loader version.
- Resolve relative manifest paths relative to the manifest file directory.
- Keep `CRAFTLESS_FABRIC_DRIVER_MOD` as a fallback for current single-driver
  releases.
- Avoid hard-coded Minecraft version catalogs or public support claims.

## Non-Goals

- Do not package multiple driver jars in this phase.
- Do not add a new compiled Fabric lane.
- Do not infer compatibility across Minecraft versions.
- Do not add public gameplay actions, route families, CLI gameplay catalogs,
  Fabric gameplay bindings, scenario shortcuts, or generated OpenAPI changes.

## Manifest Format

```json
{
  "entries": [
    {
      "loader": "FABRIC",
      "minecraftVersion": "1.21.6",
      "loaderVersion": "0.17.2",
      "path": "mods/craftless-driver-fabric-1.21.6.jar"
    }
  ]
}
```

`loaderVersion` is optional. Exact loader-version entries win when the request
has a loader version. A version entry without `loaderVersion` may match that
Minecraft version as a fallback. If no manifest entry matches, the provider
falls back to the current `CRAFTLESS_FABRIC_DRIVER_MOD` behavior.

## Acceptance Criteria

- A focused daemon test proves a manifest exact match wins over
  `CRAFTLESS_FABRIC_DRIVER_MOD`.
- A focused daemon test proves the provider falls back to
  `CRAFTLESS_FABRIC_DRIVER_MOD` when the manifest has no matching lane.
- Relative manifest paths resolve relative to the manifest directory.
- Existing Phase 107 daemon/CLI compatibility tests still pass.
- AGENTS and the project checklist record Phase 108 and keep runnable
  latest/older support open.
