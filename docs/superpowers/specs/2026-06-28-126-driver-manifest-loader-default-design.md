# Driver Manifest Loader Default Design

## Problem

Packaged Craftless CLI distributions contain `driver-mods.json` entries keyed
by Loader, Minecraft version, and Fabric Loader version. Client creation also
allows `loaderVersion` to be omitted so users can ask Craftless for a normal
Fabric runtime. Today cache preparation chooses the first stable Fabric Loader
advertised by Fabric metadata before the driver-mod manifest is consulted.

That ordering can prepare a runtime lane that the packaged Craftless driver
does not support, even when the manifest already contains a compatible driver
for the requested Minecraft version on a different Fabric Loader version. Phase
125 made that fail safely, but the default user path is still too easy to
break.

## Goals

- Let a configured driver-mod manifest provide a preferred Fabric Loader
  version for a requested Minecraft version when the create-client request does
  not already pin `loaderVersion`.
- Keep an explicit request `loaderVersion` authoritative.
- Keep unconfigured environments using the existing Fabric metadata default.
- Preserve strict manifest miss behavior after cache preparation.
- Improve packaged CLI/server usability without adding a new compiled lane or
  claiming broader Minecraft support.

## Non-Goals

- Do not add public gameplay actions, generated route families, CLI gameplay
  catalogs, Fabric descriptor/binding pairs, scenario shortcuts, or survival
  task behavior.
- Do not add new Minecraft/Fabric compiled lanes.
- Do not make raw Fabric Loader versions part of public gameplay API.
- Do not claim `latest-release`, 26.x, or older Minecraft runtime support is
  complete by this phase alone.
- Do not remove the ability for advanced users to request a specific Loader
  version.

## Acceptance Criteria

- A focused provider test fails before implementation because configured
  manifests do not expose a preferred loader version.
- A create-client/server test fails before implementation because a manifest
  entry for `1.21.6` / `0.16.14` is ignored and the cache prepares `0.17.2`.
- After implementation, a Fabric create-client request for exact Minecraft
  version `1.21.6` with no `loaderVersion` prepares the manifest-backed
  `0.16.14` lane when the configured manifest prefers it.
- A create-client request with explicit `loaderVersion` keeps using the
  requested version and does not let the manifest override it.
- Existing single-driver fallback behavior remains unchanged when no manifest
  is configured.
- Local focused daemon and CLI tests, `git diff --check`, and `mise run ci`
  pass.
