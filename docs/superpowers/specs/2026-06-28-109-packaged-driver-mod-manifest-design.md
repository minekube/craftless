# Packaged Driver Mod Manifest Design

## Problem

Phase 108 added `CRAFTLESS_DRIVER_MOD_MANIFEST`, but normal CLI
distributions still only ship `mods/craftless-driver-fabric.jar` and
`craftless server start` only auto-discovers that single jar. That leaves the
new manifest provider unused by installed users and Docker images.

For multi-version support, the distribution needs a durable manifest file that
can grow from one Fabric driver entry to many entries without changing CLI
launch code again.

## Goals

- Package `driver-mods.json` in the CLI tar/zip distribution.
- Include the current remapped Fabric driver mod in that manifest.
- Make `craftless server start` auto-discover packaged `driver-mods.json`
  before falling back to `mods/craftless-driver-fabric.jar`.
- Keep the existing single-jar fallback for current release compatibility.
- Keep the manifest local to distribution/runtime plumbing and avoid public
  gameplay API changes.

## Non-Goals

- Do not add a second driver jar or new compiled Fabric lane in this phase.
- Do not claim latest/current or representative older version support.
- Do not add public gameplay actions, generated route families, CLI gameplay
  catalogs, Fabric gameplay bindings, or scenario shortcuts.
- Do not remove the single `CRAFTLESS_FABRIC_DRIVER_MOD` fallback yet.

## Design

The CLI distribution will contain:

```text
mods/craftless-driver-fabric.jar
driver-mods.json
```

The manifest will use the Phase 108 schema:

```json
{
  "entries": [
    {
      "loader": "FABRIC",
      "minecraftVersion": "1.21.6",
      "loaderVersion": "0.19.3",
      "path": "mods/craftless-driver-fabric.jar"
    }
  ]
}
```

`craftless server start` will set `CRAFTLESS_DRIVER_MOD_MANIFEST` when the
installed distribution root contains `driver-mods.json`. If the manifest is
missing, it will keep setting `CRAFTLESS_FABRIC_DRIVER_MOD` from
`mods/craftless-driver-fabric.jar` as it does today.

## Acceptance Criteria

- A CLI test proves packaged `driver-mods.json` is preferred over the
  single-jar fallback.
- Distribution tests guard that `cli/build.gradle.kts` packages
  `driver-mods.json`.
- `mise run package-cli` verifies both tar and zip contain `driver-mods.json`.
- Existing packaged single-jar fallback tests still pass.
- AGENTS/checklist/evidence record Phase 109 and keep runnable latest/older
  support open.
