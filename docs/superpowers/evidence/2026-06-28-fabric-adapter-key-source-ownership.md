# Fabric Adapter Key Source Ownership Evidence

## Scope

Phase 86 removes duplicated private Fabric adapter-key literals from backend
adapter registration. It does not remove private adapters or complete generic
runtime discovery.

## Red Evidence

- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric backend does not own bootstrap adapter key literals*'`
  failed before implementation because `FabricDriverBackend.kt` still repeated
  bootstrap adapter-key literals such as `fabric.entity-query`,
  `fabric.world-block-query`, and `fabric.recipe-craft`.

## Green Evidence

- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric backend does not own bootstrap adapter key literals*' --tests '*FabricDriverModuleTest.fabric backend exposes bootstrap bindings as graph operation adapters*'`

The focused guard and existing graph-owned adapter registration regression
passed after adding `FabricBootstrapOperationAdapters` constants and using them
from backend registration.

## Local Final Gates

- source scan:
  `rg -n '"fabric\.(entity-query|entity-attack|world-block-query|recipe-query|recipe-craft)"' driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverBackend.kt`
- `git diff --check`
- `mise exec -- gradle lint test --rerun-tasks`
- `mise exec -- bun test playwright`

All final local gates passed before commit. The backend adapter-key literal
scan returned no matches, and the forced Gradle gate executed lint and tests
instead of relying on cached task state.

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
