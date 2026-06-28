# Phase 74: Metadata Binary Checksums Design

## Goal

Extend cache artifact integrity beyond asset objects by using upstream SHA-1
metadata for binary artifacts in Mojang and Fabric metadata.

## Context

Phase 73 made Minecraft asset objects checksum-aware because the asset index
uses the object SHA-1 as the cache handle. The latest-version cache path still
has other large binary artifacts whose metadata may include SHA-1 values:

- Minecraft client jar downloads;
- Minecraft library artifact downloads;
- Minecraft native classifier downloads;
- managed Java runtime raw file downloads;
- Fabric profile `downloads.artifact` entries when present.

If these files are partially written or corrupted during a failed cache
preparation, Craftless should not blindly reuse them on the next run when
upstream metadata provides a checksum.

This is a supervisor/cache foundation change for multi-version compatibility.
It does not add Fabric runtime lanes, public gameplay actions, static CLI
gameplay catalogs, or version-specific public APIs.

## Requirements

- Parse optional SHA-1 metadata for Minecraft client jar downloads from the
  version manifest.
- Parse optional SHA-1 metadata for Minecraft libraries and native classifiers
  from the version manifest.
- Parse optional SHA-1 metadata for Java runtime raw file downloads from the
  Java runtime manifest.
- Parse optional SHA-1 metadata for Fabric profile `downloads.artifact`
  library entries. Fabric libraries derived only from Maven coordinates and a
  base URL may keep `sha1 = null`.
- Reuse an existing binary artifact with `sha1` only when local bytes match.
- Re-fetch and replace corrupt cached binary artifacts when `sha1` is known.
- Validate newly downloaded bytes against known SHA-1 before writing.
- Keep existing behavior for artifacts without SHA-1 metadata.

## Non-Goals

- Do not add a cache job queue or async progress stream in this phase.
- Do not add a new compiled Fabric lane.
- Do not claim Minecraft 26.2 or older-version Fabric client support.
- Do not add public gameplay actions, generated route families, CLI gameplay
  catalogs, Fabric descriptor/binding pairs, or scenario shortcuts.
- Do not require SHA-1 metadata for all Fabric Maven-coordinate libraries.

## Design

Introduce small internal metadata value types for URL plus optional SHA-1 where
the parser currently returns only URLs. Preserve public `CachePreparedArtifact`
as the single artifact metadata DTO by continuing to use its optional `sha1`
field from Phase 73.

`CachePreparationService.writeFetchedBytesArtifact(...)` already verifies
artifacts when `artifact.sha1` is present. Phase 74 should feed that field from
the client jar, library, native, Java runtime, and Fabric download parsers.

The implementation remains synchronous and scoped to cache preparation. More
advanced retry/backoff/progress semantics can build on this integrity metadata
later.

## Verification

- A focused daemon test fails before implementation because corrupt cached
  client/library/runtime binaries are reused despite SHA-1 metadata.
- The focused daemon test passes after implementation and proves corrupt
  cached binaries are re-fetched and valid binaries are reused.
- `mise exec -- gradle :daemon:test --tests '*CachePreparationServiceTest*'`
  passes.
- `git diff --check`, `mise run architecture-check`, and `mise run ci` pass.
