# User-Facing Usability Docs Design

## Problem

CL-05 requires Craftless to be easy for external users and agents to install,
run, inspect, stream, invoke, and debug without reading source code. The repo
already has an installer, Docker image, reusable GitHub Action, adaptive CLI,
README, and repo-local agent skills, but the gate is not closed because:

- the current README status still contains stale latest/current wording from
  before CL-03 and CL-04 closed;
- `craftless clients --help` exits as an unknown command, which makes the
  stable CLI core less discoverable;
- the install script, Docker runtime image, adaptive CLI help, and docs grep
  need fresh local evidence.

## Goal

Close CL-05 by making the user-facing surfaces coherent and verified:

- README and roadmap describe CL-03 and CL-04 as current evidence and CL-05 as
  the active usability gate;
- CLI group help explains stable lifecycle/discovery commands and points
  users to generated per-client OpenAPI for gameplay;
- install script smoke proves a fresh user can install and run the packaged
  CLI;
- Docker runtime smoke proves the image runs from the copied packaged artifact
  rather than building Craftless inside the container;
- agent skill docs continue to teach generated OpenAPI/SSE/JSON-RPC
  composition, missing-primitive reporting, and no scenario shortcuts.

## Non-Goals

- Do not close CL-06, CL-07, or CL-08.
- Do not add a TypeScript SDK or Homebrew path.
- Do not add static gameplay CLI command trees.
- Do not use server-provisioned inventory or scenario shortcuts.
- Do not turn Docker into a build image; it remains a runtime image that copies
  the packaged Craftless distribution.

## Acceptance

- A CLI regression test fails before group help exists and passes after
  `craftless clients --help` returns stable help.
- README and roadmap have no stale "latest gameplay actions empty" or
  "latest/current compatibility work still pending" wording.
- `mise exec -- bun test playwright/src/distribution.test.ts` passes.
- Focused CLI help tests pass.
- `mise run package-cli` passes.
- Install smoke runs the installed `craftless` binary from a temporary install
  directory and `server start --once --port 0` returns `ok=true`.
- Docker runtime smoke runs
  `/opt/craftless/bin/craftless server start --once --port 0` from the built
  image and returns `ok=true`; if the local Docker daemon is unavailable, do
  not close CL-05.
- `git diff --check` passes.
- Evidence is written to
  `docs/superpowers/evidence/2026-06-28-user-facing-usability-docs.md`.
