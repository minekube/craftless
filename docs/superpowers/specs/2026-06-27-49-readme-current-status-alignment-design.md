# Phase 49: README Current Status Alignment Design

## Goal

Keep README usage and status text aligned with the current Craftless product
shape: generated per-client APIs, current compiled Fabric lane metadata, and
honest final gameplay evidence without server-provisioned inventory.

## Context

The README is the first product surface for users and agents. After the
version-lane and final-gameplay work, it still showed a stale Fabric Loader
example and active-status wording from an older diagnostic smoke path that used
server-side item provisioning. That wording conflicts with the completion
contract: final evidence must be composed through generated public APIs without
provisioned inventory or static scenario shortcuts.

## Requirements

- Update the cache-prepare example to use the current compiled Fabric Loader
  lane.
- Remove active-product wording that presents server-provisioned item setup as
  current gameplay evidence.
- Describe final gameplay evidence as generated API/SSE composition without
  server-provisioned inventory or manual movement for Craftless.
- Keep README free of removed TypeScript SDK, Homebrew, bridge-as-product, and
  legacy diagnostic survival task language.
- Add a test guard so stale loader and provisioning wording cannot reappear.

## Non-Goals

- Do not change product behavior in this phase.
- Do not add gameplay action ids, route families, CLI gameplay catalogs,
  scenario shortcuts, or a new Minecraft version lane.
- Do not mark Craftless complete.

## Verification

- Bun distribution tests prove README quickstarts remain present and reject the
  stale loader/provisioning wording.
- `git diff --check`, `mise run architecture-check`, and `mise run ci` pass
  before claiming this phase complete.
