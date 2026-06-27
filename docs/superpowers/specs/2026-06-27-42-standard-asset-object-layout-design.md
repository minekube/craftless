# Phase 42: Standard Asset Object Layout Design

## Goal

Craftless cache preparation must store Minecraft asset objects in the layout
that the Minecraft client expects at launch:
`assets/objects/<first-two-hash-chars>/<hash>`.

## Context

The supervisor already downloads the Mojang asset index and asset object bytes.
The launch argument generation passes the prepared assets root to the client as
`${assets_root}`. For real versioned clients to work across Minecraft versions,
the files below that root must use Mojang's standard object layout. A
Craftless-specific object filename is not enough because the Minecraft client
does not consult the Craftless cache manifest when resolving asset object
paths.

## Requirements

- Derive each asset object handle from the Mojang asset hash, not from a
  Craftless hash of the hash string.
- Store objects under `cache/assets/objects/<prefix>/<hash>` where `prefix` is
  the first two characters of the Mojang hash.
- Keep the object download source as
  `https://resources.download.minecraft.net/<prefix>/<hash>`.
- Validate asset hashes before using them in cache handles.
- Keep this change inside supervisor cache preparation and launch evidence.
- Do not add gameplay actions, generated route families, CLI gameplay
  catalogs, scenario shortcuts, or Fabric descriptor/binding pairs.

## Non-Goals

- Do not change public gameplay OpenAPI.
- Do not add a custom asset serving API.
- Do not hard-code assets for a specific Minecraft version.
- Do not make the Fabric driver aware of cache layout details.

## Verification

- Focused daemon test proves the prepared asset handle matches Mojang's
  expected object path.
- Existing cache preparation tests still pass.
- `mise run lint`, `mise run architecture-check`, and `mise run ci` pass
  before claiming this phase complete.
