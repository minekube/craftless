# Strict Fabric Runtime Lane Identity Design

## Problem

The Fabric compatibility matrix currently resolves a supported lane by
Minecraft game version only. That is unsafe for the multi-version goal:
driver runtime support also depends on the Fabric Loader version, Fabric API
version, and mappings fingerprint. A client with the same Minecraft version
but a different loader/API/mappings identity must not be treated as supported
by the current compiled driver lane.

This is a false-positive support problem, not a gameplay API problem.

## Goals

- Require supported Fabric lanes to match game version, loader version, Fabric
  API version, and mappings fingerprint.
- Return a machine-readable unsupported lane when the game version is known but
  the runtime identity does not match the compiled lane.
- Keep unknown game versions reporting `unsupported-version`.
- Prevent provider selection for mismatched runtime identities.
- Keep latest/current and representative older support open until actual
  runnable lane evidence exists.

## Non-Goals

- Do not add new compiled Fabric lanes.
- Do not mark current/latest/older lanes as newly supported.
- Do not add public gameplay actions, generated route families, CLI gameplay
  catalogs, Fabric gameplay bindings, or scenario shortcuts.
- Do not change the public OpenAPI surface.

## Acceptance Criteria

- A focused matrix test fails before implementation when a same-game-version
  identity with different loader/API/mappings resolves as supported.
- After implementation, that identity resolves as unsupported with reason
  `unsupported-runtime-identity`.
- Provider selection returns null for mismatched runtime identity.
- Unknown game versions still resolve with `unsupported-version`.
- AGENTS/checklist/evidence record Phase 110 and keep broad runnable
  latest/older support open.
