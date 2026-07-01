# Game-scoped Fabric loader matrix design

`GET /versions/support-targets` must not advertise a Fabric Loader runtime as
supported merely because the loader appears in Fabric's global loader list.
The launch path resolves `CachePrepareRequest` through
`/versions/loader/{minecraftVersion}`, so the matrix endpoint must use the same
game-scoped metadata as its compatibility authority.

## Contract

- Keep the global loader list useful for discovery so agents can see loader
  identities that exist.
- For each Fabric game target, fetch Fabric's game-scoped loader metadata from
  `/versions/loader/{minecraftVersion}`.
- Mark a runtime target supported only when the loader version appears in that
  game-scoped metadata and a Craftless driver lane matches it directly or via a
  wildcard manifest row.
- If a loader is globally listed but absent from the game-scoped metadata, keep
  the runtime target visible and mark it unsupported with
  `NO_COMPATIBLE_FABRIC_LOADER`.
- Explicit driver manifest loader versions that are absent from the
  game-scoped Fabric metadata must not create supported manifest-only rows.

This keeps the supervisor matrix aligned with `POST /clients`: an API caller can
read the matrix and know whether a Minecraft+Fabric Loader pair can be launched
or why it cannot.
