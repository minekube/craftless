# Binding Operation Id Source Ownership Evidence

## Scope

Phase 85 removes duplicated operation-id string literals from private Fabric
execution bindings. It does not remove private bindings or complete generic
runtime discovery.

## Red Evidence

- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric action bindings do not own operation id literals*'`
  failed before implementation because `FabricActionBindings.kt` still
  declared `operationId = "..."` literals.
- `mise exec -- gradle :protocol:test --tests '*NamespacePolicyTest.private fabric gameplay bindings are limited to bootstrap operation id references*'`
  failed before implementation because `FabricActionBindings.kt` had operation
  id literals and did not yet reference `FabricBootstrapOperationIds`.

## Green Evidence

- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric action bindings do not own operation id literals*' --tests '*FabricDriverModuleTest.transitional fabric binding operation ids are represented as runtime graph operations*' --tests '*FabricDriverModuleTest.fabric backend exposes bootstrap bindings as graph operation adapters*'`
- `mise exec -- gradle :protocol:test --tests '*NamespacePolicyTest.private fabric gameplay bindings are limited to bootstrap operation id references*'`

Focused Fabric and protocol guards passed after moving operation id ownership
to `FabricBootstrapOperationIds` and updating private bindings to reference
those constants.

## Local Final Gates

- `git diff --check`
- source scan:
  `rg -n 'operationId(?:\s*:\s*String)?\s*=\s*"[a-z][a-z0-9]*(?:\.[a-z][a-z0-9]*)*"' driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricActionBindings.kt protocol/src/test/kotlin/com/minekube/craftless/protocol/NamespacePolicyTest.kt`
- `mise exec -- gradle lint test --rerun-tasks`
- `mise exec -- bun test playwright`

All final local gates passed before commit. The operation-id literal scan
returned no matches, and the forced Gradle gate executed lint and tests instead
of relying on cached task state.

## Remote CI

Not waited on during active development. Local forced CI is the working gate;
remote CI may continue in the background after push.

## Notes

- No new public gameplay action, generated route family, CLI gameplay catalog,
  Fabric execution binding, scenario shortcut, compiled lane, public
  version-specific API, or Minecraft support claim should be added in this
  phase.
- The broader binding-exit blocker remains active because future gameplay
  breadth still needs generic runtime discovery instead of hand-maintained
  bootstrap operation definitions.
