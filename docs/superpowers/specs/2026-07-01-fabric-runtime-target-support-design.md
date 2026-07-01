# Fabric Runtime Target Support Design

## Problem

`GET /versions/support-targets` reported Fabric game-version rows and embedded
matching driver mods, but the active compatibility goal is about Fabric
Minecraft, loader, and runtime combinations. Agents had to infer supported
runtime identities from `driverMods`, while unsupported targets only had a
game-version-level reason.

## Goals

- Add a Craftless-owned `runtimeTargets` projection to each Fabric support
  target.
- Represent each supported runtime identity with loader version, Java major
  version, mappings fingerprint, support status, and the matching driver mod.
- Represent unsupported Fabric game targets with a machine-readable unsupported
  runtime row using `NO_DRIVER_MOD`.
- Make the packaged matrix runner consume `runtimeTargets` as the support proof
  contract.

## Non-Goals

- Do not claim every Fabric loader version works.
- Do not add new static gameplay APIs, scenario actions, or per-version public
  route families.
- Do not launch every Fabric game version in normal push CI.

## Acceptance Criteria

- `/versions/support-targets` responses include `runtimeTargets` for supported
  and unsupported rows.
- Supervisor OpenAPI describes `runtimeTargets`.
- The generated matrix runner validates `runtimeTargets` against the packaged
  driver manifest and generates probe jobs from supported runtime rows.
- Focused protocol, daemon, script, and distribution tests pass.
