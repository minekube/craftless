# Transitional Fabric Action Allowlist Deletion Evidence

## Scope

Phase 131 deletes the stale static
`docs/architecture/transitional-fabric-action-allowlist.txt` artifact and makes
the private Fabric binding guard compare against
`fabricBootstrapOperationDefinitions()`. The remaining bootstrap definitions
are still transitional and still need future replacement by generic runtime
discovery. This phase does not add or remove runtime operations, add gameplay
APIs, change Fabric versions, claim latest/older support, or add scenario
shortcuts.

## Red Evidence

- `git rm docs/architecture/transitional-fabric-action-allowlist.txt && mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.transitional fabric binding operation ids are represented as runtime graph operations*'`
  - Failed before implementation with `NoSuchFileException` because the test
    still read the deleted allowlist file.

## Green Evidence

- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.transitional fabric binding operation ids are represented as runtime graph operations*'`
  - Passed after the test compared binding operation ids with
    `fabricBootstrapOperationDefinitions()` instead of the deleted file.
- `rg "transitional-fabric-action-allowlist|transitionalFabricActionAllowlist" driver-fabric/src AGENTS.md docs/project-completion-checklist.md docs/architecture || true`
  - Confirmed no source/test/checklist code reads the deleted file. The only
    remaining active mention is the Phase 131 explanatory text in `AGENTS.md`.

## Local CI Evidence

- `git diff --check`
  - Passed.
- `mise run ci`
  - Passed. Ran lint, unused/dead-code checks through detekt, Gradle tests, and
    Bun Playwright helper/distribution tests.
