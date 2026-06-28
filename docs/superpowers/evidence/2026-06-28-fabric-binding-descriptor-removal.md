# Fabric Binding Descriptor Removal Evidence

## Scope

Phase 83 removes public descriptor/schema ownership from private Fabric
execution bindings. `FabricActionBinding` is now an operation-id keyed private
executor; graph discovery/projection remains the source of public actions,
schemas, availability, resources, and OpenAPI.

## Red Evidence

- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric action bindings do not own public descriptors or schemas*' --tests '*FabricDriverModuleTest.fabric operation adapter registration does not use binding descriptors*' --tests '*FabricDriverModuleTest.transitional fabric binding operation ids are represented as runtime graph operations*'`
  failed before implementation because `FabricActionBindings.kt` still used
  `DriverActionDescriptor`/schema descriptor metadata and
  `FabricDriverBackend` still registered adapters from `binding.descriptor.id`.

## Green Evidence

- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric action bindings do not own public descriptors or schemas*' --tests '*FabricDriverModuleTest.fabric operation adapter registration does not use binding descriptors*' --tests '*FabricDriverModuleTest.transitional fabric binding operation ids are represented as runtime graph operations*' --tests '*FabricDriverModuleTest.fabric backend exposes bootstrap bindings as graph operation adapters*'`
- `mise exec -- gradle :driver-fabric:test`

Both focused guards and the full Fabric regression passed after moving private
binding registration to `operationId` and deleting descriptor helper metadata
from `FabricActionBindings.kt`.

## Local Final Gates

- `git diff --check`
- `mise run architecture-check`
- `mise run ci`

All local final gates passed before commit.

## Remote CI

Pending until this phase is pushed to `main` and GitHub Actions passes.

## Notes

- No new public gameplay action, generated route family, CLI gameplay catalog,
  Fabric descriptor/binding pair, scenario shortcut, compiled lane, public
  version-specific API, or Minecraft support claim was added.
- The broader binding-exit blocker remains active because future gameplay
  breadth still needs generic runtime discovery instead of hand-maintained
  bootstrap operation definitions.
