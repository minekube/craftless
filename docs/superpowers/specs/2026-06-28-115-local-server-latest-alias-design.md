# Local Server Latest Alias Design

## Problem

The supervisor runtime paths resolve `latest-release` and `latest-snapshot`,
but the local Minecraft server smoke testkit still provisions server jars by
looking up the requested string as an exact Mojang version id. A smoke request
with `CRAFTLESS_SMOKE_MINECRAFT_VERSION=latest-release` therefore fails before
it can exercise the current server version.

This is version/runtime plumbing for verification, not gameplay API breadth.

## Goals

- Let local server smoke provisioning accept `latest-release` and
  `latest-snapshot`.
- Resolve aliases through Mojang `version_manifest_v2.json` before fetching
  concrete server metadata.
- Store the downloaded server jar under the resolved concrete Minecraft
  version so artifacts are reproducible.
- Share version-index parsing with the other runtime metadata paths instead of
  creating another ad hoc parser.

## Non-Goals

- Do not add new compiled Fabric lanes.
- Do not claim latest/current or older aliases are runnable end to end.
- Do not add public gameplay actions, generated route families, CLI gameplay
  catalogs, Fabric gameplay bindings, scenario shortcuts, or public
  version-specific APIs.
- Do not change how final gameplay actions are discovered or invoked.

## Acceptance Criteria

- A focused testkit test fails before implementation when server provisioning
  treats `latest-release` as an exact version id.
- After implementation, provisioning resolves `latest-release` to the concrete
  Mojang release from the same version index response, fetches that version
  metadata, and writes `minecraft-server-<resolved-version>.jar`.
- Existing exact-version server provisioning remains unchanged.
- AGENTS/checklist/evidence record Phase 115 and keep runnable latest/older
  support open.
