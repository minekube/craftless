# Alias Driver Manifest Loader Default Design

## Problem

Phase 126 lets a packaged driver-mod manifest steer the default Fabric Loader
version when the create-client request uses an exact Minecraft version such as
`1.21.6`. Common user-facing flows use aliases such as `latest-release`. The
runtime factory currently asks the manifest provider for a preferred loader
before resolving that alias, so a manifest entry for the resolved concrete
version cannot influence the default lane.

That leaves the easiest documented path fragile: a packaged distribution can
contain a compatible driver for the resolved current version, but create-client
can still prepare Fabric metadata's first stable loader and then fail strict
manifest matching.

## Goals

- Resolve Minecraft version aliases before asking the driver-mod provider for a
  default Fabric Loader version.
- Keep `CachePreparationService` as the owner of Mojang version-manifest
  resolution and metadata fetching.
- Preserve exact-version behavior from Phase 126.
- Keep explicit create-client `loaderVersion` authoritative.
- Avoid changing public protocol DTOs or adding new public API.

## Non-Goals

- Do not add compiled Fabric lanes, runtime support claims, or compatibility
  matrix claims.
- Do not add public gameplay actions, route families, CLI gameplay catalogs,
  Fabric descriptor/binding pairs, scenario shortcuts, or survival tasks.
- Do not expose raw Mojang/Fabric metadata as public gameplay API.
- Do not make manifest preference replace strict post-cache manifest matching.

## Acceptance Criteria

- A daemon create-client test fails before implementation because
  `latest-release` does not select the manifest-backed loader version for the
  resolved concrete Minecraft version.
- A packaged CLI test fails before implementation for the same alias path.
- After implementation, `latest-release` resolves to the concrete version
  before provider preference and prepares the manifest-backed loader lane.
- Exact version and explicit `loaderVersion` behavior remain unchanged.
- Focused daemon/CLI tests, `git diff --check`, and `mise run ci` pass locally.
