# Driver Mod Manifest Runtime Identity Design

## Problem

The generated private Fabric driver lane catalog already records runtime
identity fields: Minecraft version, Fabric Loader version, Fabric API version,
Java major version, and mappings fingerprint. The packaged public
`driver-mods.json` manifest currently projects only Minecraft version, loader
version, and path.

Daemon selection also matches configured driver-mod manifest entries only by
loader, Minecraft version, and Fabric Loader version. That is too loose for
multi-version support. A prepared runtime with the same Minecraft and loader
versions but a different Fabric API or Java major could select a driver artifact
that was compiled and packaged for another runtime identity.

## Goals

- Project Fabric API version, Java major version, and mappings fingerprint into
  the packaged driver-mod manifest from the generated private lane catalog.
- Keep generated manifests free of build-only fields such as `artifactKey` and
  `distributionPath`.
- Make daemon driver-mod selection reject manifest entries whose Fabric API
  version or Java major version conflicts with the prepared runtime identity.
- Preserve compatibility with already-published manifests that do not yet carry
  the new optional identity fields.

## Non-Goals

- Do not add a new compiled Fabric lane.
- Do not change Minecraft, Fabric Loader, Fabric API, Loom, Yarn, or Java
  dependency versions.
- Do not claim latest/current or older-version support is complete.
- Do not add public gameplay APIs, generated route families, CLI gameplay
  catalogs, Fabric descriptor/binding pairs, or scenario shortcuts.
- Do not require mappings-fingerprint matching until the prepared runtime has a
  comparable runtime-derived value.

## Design

`cli` packaging projects these runtime identity fields from
`fabric-driver-lanes.json` into `driver-mods.json`:

- `fabricApiVersion`
- `javaMajorVersion`
- `mappingsFingerprint`

The daemon manifest model treats those fields as optional for backward
compatibility. When a `ClientRuntimeDriverModRequest` includes a resolved
`fabricApiVersion` or `javaMajorVersion`, manifest entries declaring those
fields must match. Missing optional fields continue to match so existing
published manifests remain usable.

`WorkspaceClientRuntimeDriverFactory` builds the stricter driver-mod request
after cache preparation, using:

- Fabric API version derived from the resolved Fabric API mod artifact;
- Java major version from the selected Java runtime requirement.

## Acceptance Criteria

- A red CLI packaging test fails before implementation because generated
  `driver-mods.json` lacks the runtime identity fields.
- A red daemon provider test fails before implementation because a manifest
  entry with mismatched `fabricApiVersion` is selected.
- After implementation, generated manifests include runtime identity fields but
  not build-only fields.
- After implementation, daemon selection rejects known Fabric API or Java major
  mismatches and includes the requested runtime identity in the error context.
- Existing old-style manifest tests keep passing.
- Focused tests, `git diff --check`, and `mise run ci` pass locally.
