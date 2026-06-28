# Final Local Release Gates Design

## Problem

CL-05 closed the external-user and agent usability surface, but the repository
still needs one final local release-quality pass from the current worktree
before any honest gameplay completion attempt. Old phase evidence is not enough
because CL-05 changed CLI help, docs, and architecture tests.

## Goal

Close CL-06 by rerunning the repository's local release gates and product lane
probes from fresh artifacts:

- Kotlin lint and static analysis;
- architecture checks that enforce generated runtime graph/OpenAPI authority;
- local CI;
- packaged CLI build;
- Docker runtime smoke;
- install script smoke;
- latest/current packaged lane probe;
- representative older packaged lane probe;
- `git diff --check`.

## Non-Goals

- Do not close final gameplay.
- Do not run remote CI as the blocking signal.
- Do not weaken tests to preserve stale architecture expectations.
- Do not treat compile-only output as product-lane proof.

## Acceptance

- `mise run lint` exits `0`.
- `mise run architecture-check` exits `0`.
- `mise run ci` exits `0`.
- `mise run package-cli` exits `0`.
- Docker runtime image starts the packaged CLI with `ok=true`.
- Install script smoke starts the installed CLI with `ok=true`.
- `mise run packaged-latest-current-probe` exits `0` and captures generated
  invocation evidence for latest/current `26.2`.
- `mise run packaged-representative-older-probe` exits `0` and captures
  generated invocation evidence for `1.20.6`.
- `git diff --check` exits `0`.
- Evidence is written to
  `docs/superpowers/evidence/2026-06-28-final-local-release-gates.md`.
