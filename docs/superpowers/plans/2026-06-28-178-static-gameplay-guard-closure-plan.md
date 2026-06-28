# Static Gameplay Guard Closure Plan

## Scope

Close CL-02f by making each static-gameplay guard surface explicit and tested.

## Steps

1. Capture the stale policy-test failure against the deleted
   `FabricActionBindings.kt` file.
2. Update the Fabric guard to inspect `FabricExecutionAdapters.kt` and compare
   adapter operation constants against runtime graph operation sources.
3. Add a production-source guard for CLI and daemon static gameplay catalogs
   and alias route families.
4. Refactor `docs/project-completion-checklist.md` so CL-02f is split into
   named guard sub-gates.
5. Run focused protocol tests.
6. Run `git diff --check` and `mise run architecture-check`.
7. Record evidence and update the phase index.

## Review Checkpoints

- Do not mark CL-02 complete unless every CL-02f sub-gate and the final
  architecture verification are recorded.
- Do not delete generated-spec fixture tests merely because they contain
  example gameplay ids.
- Do not turn the guard into a ban on private execution adapters; the boundary
  is public/static gameplay API ownership.
