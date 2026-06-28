# Binding Adapter Key Derivation Removal Evidence

## Scope

Phase 88 removes backend derivation of private Fabric adapter keys from
operation ids. It does not remove private adapters or complete generic runtime
discovery.

## Red Evidence

- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric backend does not derive binding adapter keys from operation ids*'`
  failed before implementation because `FabricDriverBackend.kt` still contained
  `fabricOperationAdapterKey` and the `replace(".", "-")` adapter-key
  derivation.

## Green Evidence

- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric backend does not derive binding adapter keys from operation ids*' --tests '*FabricDriverModuleTest.fabric backend exposes bootstrap bindings as graph operation adapters*'`

The focused guard and existing graph-owned adapter dispatch regression passed
after registering private binding adapters from bootstrap operation definitions
and deleting the derivation helper.

## Local Final Gates

- source scan:
  `rg -n 'fabricOperationAdapterKey|replace\("\\.", "-"\)' driver-fabric/src/main/kotlin/com/minekube/craftless/driver/fabric/v1_21_6/FabricDriverBackend.kt`
- `git diff --check`
- `mise exec -- gradle lint test --rerun-tasks`
- `mise exec -- bun test playwright`

All final local gates passed before commit. The backend adapter-key derivation
scan returned no matches, Bun Playwright tests passed, and the forced Gradle
gate executed lint and tests instead of relying on cached task state.

An initial forced Gradle gate hit a transient loopback bind race in
`CraftlessCliTest.generated client action alias dispatches from runtime action
metadata`. The single failing test passed on immediate focused rerun, and the
full forced Gradle gate passed on the next run without code changes.

## Remote CI

Not waited on during active development. Local forced CI is the working gate;
remote CI may continue in the background after push.

## Push Evidence

- Implementation commit pushed to `main`:
  `936b032 driver-fabric: remove binding adapter key derivation`

## Notes

- No new public gameplay action, generated route family, CLI gameplay catalog,
  Fabric execution binding, scenario shortcut, compiled lane, public
  version-specific API, or Minecraft support claim should be added in this
  phase.
- The broader binding-exit blocker remains active because future gameplay
  breadth still needs generic runtime discovery instead of hand-maintained
  bootstrap operation definitions.
