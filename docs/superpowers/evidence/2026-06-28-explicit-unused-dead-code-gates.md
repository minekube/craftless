# Explicit Unused And Dead-Code Gates Evidence

## Scope

Phase 106 makes practical unused/dead-code checks explicit through the existing
pinned Detekt stack and mise tasks.

## Red Check

- `mise exec -- gradle :protocol:test --tests '*NamespacePolicyTest.kotlin quality gates include explicit unused and dead code checks*'`
  failed because `config/detekt/detekt.yml` and `.mise.toml` did not explicitly
  name the unused/dead-code gate.

## Green Focused Checks

- `mise exec -- gradle :protocol:test --tests '*NamespacePolicyTest.kotlin quality gates include explicit unused and dead code checks*'`
  passed.
- `mise run unused-check` passed.

## Final Local Verification

- `git diff --check`
- `mise exec -- gradle :protocol:test --tests '*NamespacePolicyTest.kotlin quality gates include explicit unused and dead code checks*'`
- `mise run unused-check`
- `mise run ci`

All commands above passed locally on 2026-06-28.
