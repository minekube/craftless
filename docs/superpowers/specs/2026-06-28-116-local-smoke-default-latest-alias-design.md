# Local Smoke Default Latest Alias Design

## Problem

Local Minecraft server smoke provisioning now accepts `latest-release`, but the
smoke configuration still defaults to the historical exact version `1.21.6`.
That keeps active verification biased toward one old concrete server version
unless every caller overrides `CRAFTLESS_SMOKE_MINECRAFT_VERSION`.

This is an active verification default, not a Fabric gameplay API or supported
client-lane claim.

## Goals

- Default local server smoke configuration to `latest-release`.
- Keep explicit `CRAFTLESS_SMOKE_MINECRAFT_VERSION` overrides unchanged.
- Keep exact-version fixture tests available for deterministic cache/model
  behavior.
- Make the default visible in tests so active verification cannot drift back to
  a concrete historical version silently.

## Non-Goals

- Do not add new compiled Fabric lanes.
- Do not claim latest/current or older aliases are runnable end to end.
- Do not change final gameplay action discovery or invocation.
- Do not add public gameplay actions, generated route families, CLI gameplay
  catalogs, Fabric gameplay bindings, scenario shortcuts, or public
  version-specific APIs.

## Acceptance Criteria

- A focused test fails before implementation because the local smoke default is
  still `1.21.6`.
- After implementation, `LocalMinecraftServerSmokeConfig.fromEnvironment()`
  and the data-class default use `latest-release`.
- Explicit environment overrides such as `1.21.6` remain preserved.
- AGENTS/checklist/evidence record Phase 116 and keep runnable latest/older
  support open.
