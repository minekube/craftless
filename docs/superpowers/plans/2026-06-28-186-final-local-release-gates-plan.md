# Final Local Release Gates Implementation Plan

> **For agentic workers:** Execute this plan with fresh local command output.
> Do not close CL-06 from earlier phase evidence or remote CI.

**Goal:** Close CL-06 by proving the current worktree passes local
release-quality gates and both packaged product-lane probes.

## Task 1: Quality And Architecture Gates

- [x] Run `mise run lint`.
- [x] Fix formatting-only failures without changing product behavior.
- [x] Run `mise run architecture-check`.
- [x] Update stale architecture guards so they enforce the current packaged
  `26.2` runtime identity instead of rejecting the official lane.
- [x] Run `mise run ci`.

## Task 2: Package And Runtime Smokes

- [x] Run `mise run package-cli`.
- [x] Run Docker runtime smoke from the copied packaged artifact image.
- [x] Run install script smoke into a clean temporary install directory.

## Task 3: Version Product Probes

- [x] Run `mise run packaged-latest-current-probe`.
- [x] Confirm latest/current summary resolves `latest-release` to concrete
  Minecraft `26.2`.
- [x] Confirm latest/current JSON-RPC and adaptive CLI invocation use generated
  `world.time.query`.
- [x] Run `mise run packaged-representative-older-probe`.
- [x] Confirm older-lane JSON-RPC and adaptive CLI invocation use generated
  `entity.query`.

## Task 4: Evidence And Closure

- [x] Run `git diff --check`.
- [x] Write
  `docs/superpowers/evidence/2026-06-28-final-local-release-gates.md`.
- [x] Move the checklist active gate from CL-06 to CL-07.
- [x] Update `docs/superpowers/phase-index.md`.
- [x] Commit and push directly to `main`.
