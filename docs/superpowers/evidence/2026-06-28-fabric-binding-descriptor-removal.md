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
- `mise exec -- gradle :protocol:test --tests '*NamespacePolicyTest.private fabric gameplay bindings are limited to transitional bootstrap operation allowlist*' --rerun-tasks`
- `mise exec -- gradle lint test --rerun-tasks`
- `mise exec -- bun test playwright`

The first local `mise run ci` passed with cached protocol tests before the
initial push. Remote CI then failed on a clean checkout because
`NamespacePolicyTest` still searched `FabricActionBindings.kt` for descriptor
`id = "..."` declarations. The guard was updated to enforce the Phase 83
invariant directly: private Fabric binding `operationId` values must match the
transitional bootstrap allowlist, and the binding file must not own
`DriverActionDescriptor` or `DriverActionArgument` metadata.

The corrected guard, forced Gradle lint/test suite, and Bun Playwright tests
all passed locally before the follow-up push.

## Remote CI

- Initial push `c288286f21f7d0558cc6bd75372906e17344a311` failed in GitHub
  Actions run `28311666609` because
  `NamespacePolicyTest.hand written fabric gameplay descriptors are limited to
  transitional bootstrap allowlist` still expected descriptor ids in
  `FabricActionBindings.kt`.
- Follow-up remote CI is pending.

## Notes

- No new public gameplay action, generated route family, CLI gameplay catalog,
  Fabric descriptor/binding pair, scenario shortcut, compiled lane, public
  version-specific API, or Minecraft support claim was added.
- The broader binding-exit blocker remains active because future gameplay
  breadth still needs generic runtime discovery instead of hand-maintained
  bootstrap operation definitions.
