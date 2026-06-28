# Packaged Older Lane Selection Smoke Design

## Problem

Phase 138 packages the representative older Fabric lane, but packaging alone
does not prove the supervisor create-client path selects that lane for an older
Minecraft runtime. Craftless needs a local, repeatable smoke that exercises the
same configured driver-mod manifest shape used by the CLI distribution and
verifies the older driver jar is staged into the prepared launch plan.

## Decision

Add a daemon-level create-client smoke using a packaged-style
`driver-mods.json` with both the current `1.21.6` lane and representative older
`1.20.6` lane. The test will create a `1.20.6` Fabric client through
`POST /clients`, use cache metadata for the older runtime and Fabric API
version, and assert the prepared launch plan contains the older lane jar, not
the current lane jar.

This phase is runtime selection evidence only. It does not prove that the older
client launches, attaches, exposes generated OpenAPI, or completes gameplay.

## Non-Goals

- Do not add public version-specific APIs, route families, or CLI gameplay
  catalogs.
- Do not add static gameplay descriptors, scenario shortcuts, or survival
  macros.
- Do not claim older Minecraft runtime support is complete.
- Do not start a real Minecraft process in this phase.

## Verification

- A daemon test must fail before implementation because the current test
  metadata fixture only covers the current packaged lane.
- The green test must prove a `1.20.6` create-client request selects
  `mods/fabric-1.20.6/craftless-driver-fabric.jar` from a multi-entry
  manifest.
- The launch plan must include the older jar content and exclude the current
  lane jar content.
- `git diff --check` and focused daemon tests must pass before commit.
