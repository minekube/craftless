# Bootstrap Operation Definition Isolation Evidence

## Scope

Phase 84 isolates the current transitional bootstrap operation definitions
from live client-state/resource probing. It does not complete the broader
generated-discovery exit; it makes the remaining bootstrap definition layer
explicit and guarded.

## Red Evidence

- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric client state probe does not own bootstrap operation definitions*' --tests '*FabricDriverModuleTest.bootstrap operation definitions still project into runtime graph*'`
  failed before implementation.
- The source guard failed because `FabricCapabilityProbe.kt` still contained
  direct `RuntimeOperationNode(` construction plus bootstrap operation ids and
  Fabric adapter ids such as `player.chat` and `fabric.player-chat`.
- The projection guard failed because
  `FabricBootstrapOperationDefinitions.kt` did not exist yet.

## Green Evidence

- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric client state probe does not own bootstrap operation definitions*' --tests '*FabricDriverModuleTest.bootstrap operation definitions still project into runtime graph*'`
- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric client state probe does not own bootstrap operation definitions*' --tests '*FabricDriverModuleTest.bootstrap operation definitions still project into runtime graph*' --tests '*FabricDriverModuleTest.fabric backend exposes bootstrap bindings as graph operation adapters*'`

Focused Phase 84 guards and the existing bootstrap adapter regression passed
after extracting transitional operation definitions into
`FabricBootstrapOperationDefinitions.kt`.

## Local Final Gates

- `git diff --check`
- `mise exec -- gradle lint test --rerun-tasks`
- `mise exec -- bun test playwright`

All final local gates passed before commit. The forced Gradle gate executed
lint and tests instead of relying on cached task state.

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
