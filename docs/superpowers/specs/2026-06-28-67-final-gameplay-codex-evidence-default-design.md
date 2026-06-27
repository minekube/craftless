# Phase 67: Final Gameplay Codex Evidence Default Design

## Goal

Make the final gameplay code default match the Phase 65 completion gate:
Codex-verifiable public API/CLI evidence is required, while human chat
confirmation is optional diagnostic evidence.

## Context

Phase 65 aligned active docs so completion no longer requires Robin's
Minecraft chat confirmation. Production final-gameplay code still had older
defaults:

- the default plan described waiting for Robin's chat confirmation;
- `fabricFinalGameplay` injected `goal may be completed` as the default
  confirmation phrase;
- macOS runs injected a default `say` prompt asking Robin to confirm in
  Minecraft chat;
- the ready reminder default kept the confirmation loop active by default.

That made the code path preserve the previous completion model even though the
docs no longer did. The corrected behavior is to keep ready artifacts and an
optional hold window by default, but require no human phrase. Operators can
still opt into confirmation diagnostics by setting
`CRAFTLESS_FABRIC_SMOKE_CONFIRM_CHAT_CONTAINS`.

## Requirements

- The default final gameplay plan must gate completion on generated OpenAPI,
  SSE, public API/CLI gameplay artifacts, no server-side item provisioning, and
  Codex evidence.
- The default final gameplay plan must not require Robin, Minecraft chat, or a
  human confirmation phrase.
- The Gradle `fabricFinalGameplay` task must not inject a default
  `CRAFTLESS_FABRIC_SMOKE_CONFIRM_CHAT_CONTAINS` value.
- The Gradle task must not inject a default ready reminder loop that keeps
  confirmation polling alive without explicit operator opt-in.
- The Gradle task must not inject a default macOS `say` command asking for
  Minecraft chat confirmation.
- Explicit confirmation diagnostics must remain available when the operator
  sets `CRAFTLESS_FABRIC_SMOKE_CONFIRM_CHAT_CONTAINS`.

## Non-Goals

- Do not remove historical confirmation artifacts or tests that prove the
  explicit opt-in confirmation path works.
- Do not add gameplay actions, route families, CLI gameplay catalogs, Fabric
  descriptor/binding pairs, scenario shortcuts, or version-specific public APIs.
- Do not mark the project complete.

## Verification

- Focused Fabric driver tests prove the default plan and Gradle task no longer
  inject human confirmation requirements.
- Existing confirmation tests continue proving explicit confirmation evidence
  works when configured.
- `git diff --check`, `mise run architecture-check`, and `mise run ci` pass
  before the phase is considered landed.
