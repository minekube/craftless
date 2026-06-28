# Reflective Movement Input Shim Evidence

## Scope

Phase 136 removes the direct `PlayerInput` source dependency from Fabric
movement bindings. It preserves the transitional bootstrap movement binding by
using a Java input shim that can compile across the current and representative
older input shapes. This phase does not add public gameplay API breadth,
static action catalogs, route families, or support claims.

## Red Evidence

- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric movement bindings do not compile against player input record*'`
  - Failed before implementation because `FabricActionBindings.kt` imported
    and constructed `PlayerInput`.

## Green Evidence

- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric movement bindings do not compile against player input record*'`
  - Passed after movement input construction moved behind
    `CraftlessMovementInput`.
- `mise run fabric-lane-check-older`
  - Passed as a compatibility probe and wrote
    `build/reports/fabric-lane-check-older.status` with
    `status=source-compatibility-blocked`.
  - The blocker list is now `RecipeDisplayApi`; the previous `PlayerInput`
    blocker is gone.

## Local CI Evidence

- `mise run package-cli`
  - Passed before final commit.
- `git diff --check`
  - Passed before final commit.
- `mise run ci`
  - Passed before final commit: Gradle lint, detekt unused-check, Gradle tests,
    and Bun Playwright fixture/distribution tests.
