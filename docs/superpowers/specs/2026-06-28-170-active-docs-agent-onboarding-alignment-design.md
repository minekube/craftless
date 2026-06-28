# Active Docs Agent Onboarding Alignment Design

Date: 2026-06-28

## Problem

The active completion board requires README, roadmap/checklist, specs/plans,
and repo-local agent skills to describe one architecture: gameplay APIs,
resources, OpenAPI, CLI help, and agent workflows come from the generated
runtime capability graph and per-client OpenAPI, not from static Kotlin/CLI
gameplay catalogs or scenario shortcuts.

The active docs already mostly say this, but README lacked a direct
external-agent usage path. That left agent onboarding split between README and
`.agents/skills/craftless-public-gameplay-agent/SKILL.md`.

## Design

Add a concise README agent usage section that tells external agents to:

- fetch supervisor and per-client OpenAPI;
- treat per-client `x-craftless-actions`, resources, routes, schemas,
  availability, and fingerprints as authority;
- treat `/clients/{id}/actions` and `/clients/{id}/resources` as projection
  evidence only;
- subscribe to SSE before state-changing work;
- invoke only advertised actions through `POST /clients/{id}:run`, generated
  alias routes, or the adaptive CLI;
- produce public-state evidence for final gameplay;
- reject server-provisioned inventory, Fabric/driver internals, and hard-coded
  scenario actions as product proof.

No code behavior changes are required.

## Non-Goals

- Do not add gameplay operations.
- Do not add CLI commands.
- Do not alter Fabric bindings, driver APIs, or version lanes.
- Do not claim binding-exit, multi-version support, or final gameplay
  completion.

## Verification

- Active-doc scan for old name/domain/SDK/static/scenario wording.
- Confirm matches in active docs are guardrails or negative examples, not
  product instructions.
- `mise exec -- bun test playwright`.
- `git diff --check`.
