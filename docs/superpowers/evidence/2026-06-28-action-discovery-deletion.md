# Action Discovery Deletion Evidence

## Scope

Phase 80 deletes the stale standalone Fabric action-discovery layer. It does
not add gameplay breadth, public route families, static CLI catalogs, Fabric
descriptor/binding pairs, scenario shortcuts, compiled lanes, version-specific
public APIs, or Minecraft support claims.

`FabricClientCapabilitySnapshot` moved into `FabricCapabilityProbe.kt` because
the runtime capability graph still uses it to compute live availability.

## Red

- Command:
  `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric standalone action discovery layer is removed*'`
- Result: failed as expected because `FabricActionDiscovery.kt` still existed.

## Green

- Command:
  `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric standalone action discovery layer is removed*'`
- Result: `BUILD SUCCESSFUL`.
- Command:
  `mise exec -- gradle :driver-fabric:test`
- Result: `BUILD SUCCESSFUL`.

## Final Local Gates

- Command: `git diff --check`
- Result: exited `0`.
- Command: `mise run architecture-check`
- Result: exited `0`; Gradle protocol/daemon/CLI/Fabric tests succeeded and
  Bun Playwright tests reported `15 pass, 0 fail`.
- Command: `mise run ci`
- Result: exited `0`; Gradle lint/test succeeded and Bun Playwright tests
  reported `15 pass, 0 fail`.

## Push Gates

- Push to `main`: commit `5c6f2d3a0fb166c32cccc6e261a19ea243c6c90d`
  (`driver-fabric: delete stale action discovery`) pushed to `origin/main`.
- GitHub Actions CI for pushed `main`: run `28310453815`
  (`https://github.com/minekube/craftless/actions/runs/28310453815`) passed;
  job `verify` completed successfully in `4m44s`.
