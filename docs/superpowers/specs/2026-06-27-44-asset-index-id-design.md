# Phase 44: Asset Index Id Design

## Goal

Craftless cache preparation and launch arguments must use the Mojang
`assetIndex.id` from the selected Minecraft version manifest, not the requested
Minecraft version string.

## Context

Current Mojang metadata does not guarantee that the game version and asset
index id are the same. For example, current `1.21.6` metadata uses asset index
id `26`. Older versions use ids such as `5`, `1.12`, or `1.8`.

The Minecraft client receives the asset index name through launch arguments
such as `${assets_index_name}`. If Craftless prepares
`cache/assets/indexes/<minecraft-version>.json` and passes the Minecraft
version as the asset index name, real clients can look for an index file that
does not match the selected metadata.

## Requirements

- Parse `assetIndex.id` and `assetIndex.url` from the selected Minecraft
  version manifest.
- Cache the asset index under `cache/assets/indexes/<assetIndex.id>.json`.
- Resolve `${assets_index_name}` in prepared launch arguments to
  `assetIndex.id`.
- Continue to use the selected Minecraft version for the version manifest,
  client jar, and version name where Mojang launch metadata expects the game
  version.
- Validate `assetIndex.id` before using it in cache handles.
- Keep this change inside supervisor cache preparation and launch metadata.
- Do not add public gameplay API, Fabric descriptors, generated route families,
  CLI gameplay catalogs, scenario shortcuts, or version-specific hard-coded
  asset ids.

## Non-Goals

- Do not add a custom asset serving API.
- Do not change asset object layout; Phase 42 already covers object paths.
- Do not change Fabric runtime discovery or generated per-client gameplay
  OpenAPI.

## Verification

- Focused daemon tests prove cache preparation stores the asset index by
  `assetIndex.id` and resolves launch arguments to that id.
- Focused daemon tests reject unsafe `assetIndex.id` values before deriving
  cache handles.
- `mise run lint`, `mise run architecture-check`, and `mise run ci` pass
  before claiming this phase complete.
