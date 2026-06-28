# CLI Create Client Loader Version Design

## Problem

Phase 123 added optional `loaderVersion` to the supervisor create-client API,
but `craftless clients create` still cannot send it. Users who need a client
runtime lane matching a packaged driver-mod manifest would have to drop to raw
HTTP even though the CLI already exposes client creation.

## Goals

- Add `--loader-version <version>` to `craftless clients create`.
- Include `loaderVersion` in the JSON body only when provided.
- Update usage/help text for the stable CLI command.
- Keep cache preparation's existing `--loader-version` behavior unchanged.

## Non-Goals

- Do not add new compiled Fabric lanes.
- Do not mark latest/current or older versions as newly supported.
- Do not add gameplay actions, route families, CLI gameplay catalogs, Fabric
  gameplay bindings, scenario shortcuts, or public version-specific APIs.
- Do not add static generated gameplay commands.

## Acceptance Criteria

- A focused CLI test fails before implementation because `clients create
  --loader-version 0.16.14` does not send `loaderVersion`.
- After implementation, the CLI posts `loaderVersion` in the create-client JSON
  request.
- CLI usage for `clients create` mentions `--loader-version <version>`.
- `mise exec -- gradle :cli:test --tests '*CraftlessCliTest.*loader version*'`
  passes.
- `git diff --check` and `mise run ci` pass locally.
