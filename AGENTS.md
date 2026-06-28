# Craftless Agent Instructions

This file is intentionally short. It is the repository-wide entrypoint for
agents, not the active roadmap, phase log, or detailed rule inventory.

Do not append per-phase history, roadmap checkboxes, temporary tasks,
completion evidence, or long guardrail lists here. Update the source-of-truth
files below instead.

## Read First

Before changing code or docs, read:

- the nearest subdirectory `AGENTS.md` for module-local rules;
- `docs/agent-operating-contract.md` for durable API, driver, transport,
  versioning, module, and workflow guardrails;
- `docs/project-completion-checklist.md` for active completion gates, blockers,
  and evidence status;
- `docs/superpowers/phase-index.md` for the maintained phase index.

## Stable Identity

Craftless uses `com.minekube.craftless` for JVM packages, Gradle coordinates,
OpenAPI metadata, Fabric entrypoints, and implementation docs.

The public domain is `minekube.com`.

## Update Policy

- Durable product and engineering rules belong in
  `docs/agent-operating-contract.md`.
- Active status, blockers, completion gates, and evidence summaries belong in
  `docs/project-completion-checklist.md`.
- Phase history belongs in `docs/superpowers/phase-index.md`.
- Specs, implementation plans, and evidence artifacts belong under
  `docs/superpowers/`.
- Module-local `AGENTS.md` files should contain only stable module rules, not
  phase status or duplicated project-wide checklists.

## Workflow

- Use `mise` for pinned dependencies and commands.
- Use `mise exec -- gradle ...` for JVM work.
- Use Bun only through mise: `mise exec -- bun ...`.
- Do not use npm, npx, yarn, pnpm, or globally installed Node tooling in repo
  workflows.
- Preserve unrelated dirty files; do not revert user work.
- If asked to push, push directly to `main`; do not create a PR unless asked.
- For docs-only edits, run at least `git diff --check` before claiming the
  change is complete.
