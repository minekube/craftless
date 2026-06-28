# Explicit Unused And Dead-Code Gates Design

## Problem

Craftless already used Detekt through `mise run lint`, but unused/dead-code
coverage was implicit. The completion goal requires practical unused/dead-code
checks, mise tasks, and CI gates. That needs to be visible and guarded in the
repository so the quality gate cannot silently drift.

## Goals

- Explicitly configure pinned Detekt rules for unused imports, parameters,
  private classes, private functions, private properties, variables,
  unreachable code/catch blocks, and unused unary operators.
- Add a dedicated `mise run unused-check` task.
- Include `unused-check` in `mise run ci`.
- Add a repository policy test proving the config and mise task stay present.

## Non-Goals

- Do not add new third-party tools or unpinned dependencies.
- Do not relax existing ktlint, Detekt, Gradle warning, or Bun gates.
- Do not change gameplay APIs, version support, packaging, or release behavior.

## Acceptance Criteria

- Policy test fails before the explicit config/task is added and passes after.
- `mise run unused-check` passes.
- `mise run ci` passes with the explicit unused-check step included.
