# Removed Survival Namespace Wording Design

## Problem

The protocol correctly rejects `task.survival.*`, but active validation
messages and tests still call that namespace a legacy survival scenario
namespace. That wording makes the removed scenario path look like an old API
generation rather than a disallowed shortcut namespace.

## Goals

- Rename active protocol rejection messages from old-path wording to
  removed-survival-scenario wording.
- Rename protocol test fixtures/messages to avoid stale old-path wording.
- Add a focused guard so active protocol source/tests do not reintroduce the
  stale phrase.
- Keep rejecting `task.survival.*`.

## Non-Goals

- Do not allow `task.survival.*`.
- Do not change validation behavior, public protocol DTO shape, or serialized
  data.
- Do not rewrite historical phase docs for earlier work.
- Do not add gameplay actions, route families, CLI catalogs, Fabric bindings,
  scenario shortcuts, version lanes, or support claims.

## Acceptance Criteria

- A focused protocol test fails before implementation because active protocol
  source/tests still contain stale old-path survival wording.
- After implementation, active protocol source/tests use removed-survival
  wording and still reject `task.survival.*`.
- `mise exec -- gradle :protocol:test --tests '*NavigationModelsTest.*'`
  passes.
- `git diff --check` and `mise run ci` pass locally.
