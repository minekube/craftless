# Phase 53: Matrix-Authoritative Fabric Provider Selection Design

## Goal

Make Fabric runtime provider selection obey the compatibility matrix before
trusting any provider-reported support.

## Context

Craftless now records current compiled-lane metadata, latest-release
unsupported lane evidence, a stable Fabric entrypoint, and selector boundary
guards. The remaining weak point is `selectFabricRuntimeProvider(...)`: it can
pick the first provider that reports `SUPPORTED` without consulting
`defaultFabricCompatibilityMatrix()`.

That behavior is not safe for future 26.x and multi-version work. A version
must become selectable only when the compatibility matrix has an explicit
supported lane with a matching provider id. Provider support is still required,
but it is not the source of truth for product compatibility.

## Requirements

- `selectFabricRuntimeProvider(...)` must resolve the runtime identity through
  the Fabric compatibility matrix before evaluating provider support.
- Unsupported matrix lanes, including the current latest-release `26.2`
  evidence lane, must fail provider selection even if a provider would report
  support for that identity.
- Provider id must match the supported matrix lane provider id. A provider that
  supports the runtime identity under another id must not be selected.
- Error messages must include machine-readable reasons from either the
  compatibility lane or provider support so failed selection remains useful
  evidence.
- Current compiled-lane selection must keep working with the current provider.
- This phase must not add a new compiled lane, claim Minecraft `26.2` support,
  add public version-specific APIs, or add gameplay actions.

## Non-Goals

- Do not add dynamic Loom compilation.
- Do not add a second Fabric runtime provider.
- Do not add Minecraft `26.2` Fabric client support.
- Do not change public OpenAPI, CLI gameplay behavior, or runtime action
  descriptors.
- Do not mark Craftless complete.

## Design

`selectFabricRuntimeProvider(...)` gains an optional compatibility matrix
parameter that defaults to `defaultFabricCompatibilityMatrix()`. Selection
flow becomes:

1. Resolve the identity to a compatibility lane.
2. Reject non-supported lanes with the lane's machine-readable
   `unsupportedReason`, or a status-derived reason if no unsupported reason is
   present.
3. Find a provider whose id equals the supported lane's provider id.
4. Evaluate that provider's `support(identity)`.
5. Return the selected provider and access only when provider support is also
   `SUPPORTED`.

This keeps compatibility matrix evidence authoritative while preserving
provider-level runtime checks for the current compiled lane.

## Verification

- Focused provider tests fail before the selection function consults the
  matrix and pass after implementation.
- `:driver-fabric:test`, `mise run lint`, `mise run architecture-check`, and
  `mise run ci` pass before this phase is claimed complete.
