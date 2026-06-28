# README Public Entrypoint Overhaul Evidence

## Scope

Phase 82 rewrites README as the public entrypoint for current Craftless:
install, Docker, GitHub Actions, generated API usage, cache/runtime
preparation, current verification, comparison, roadmap, and development.

## Red Evidence

- `mise exec -- bun test playwright` failed before the README rewrite because
  the new guard could not find `generated per-client OpenAPI`,
  `lifecycle/launch evidence only`, and `not a gameplay adapter`.

## Green Evidence

- `mise exec -- bun test playwright`

The Bun suite passed with 16 tests and 68 assertions after the README rewrite.

## Local Final Gates

- `git diff --check`
- `mise run ci`

Both local final gates passed before commit.

## Remote CI

Pending until this phase is pushed to `main` and GitHub Actions passes.

## Notes

- This phase changes README/docs/tests only.
- No product behavior, generated route family, public gameplay action, Fabric
  binding, scenario shortcut, version lane, or support claim was added.
