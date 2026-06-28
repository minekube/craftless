# Graph-Owned Fabric Invoke Evidence

## Scope

Phase 79 makes Fabric legacy `invoke(...)` dispatch graph-owned. It does not
add gameplay breadth, public route families, static CLI catalogs, Fabric
descriptor/binding pairs, scenario shortcuts, compiled lanes, version-specific
public APIs, or Minecraft support claims.

Standalone `FabricActionDiscovery` remains in the codebase as a later cleanup
target, but `FabricDriverBackend` no longer accepts or calls it for
public-compatible dispatch.

## Red

- Command:
  `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric legacy invoke dispatches from runtime graph instead of action discovery*'`
- Result: failed as expected because legacy `invoke(...)` still used injected
  `FabricActionDiscovery`.
- Command:
  `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric legacy invoke adapters ignore action discovery overrides*'`
- Result: failed as expected because operation adapters still came from action
  discovery.

## Green

- Command:
  `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric backend dispatch does not depend on fabric action discovery*' --tests '*FabricDriverModuleTest.fabric legacy invoke dispatches unavailable operations from runtime graph*' --tests '*FabricDriverModuleTest.fabric legacy invoke adapters come from private binding map*'`
- Result: `BUILD SUCCESSFUL`.
- Command:
  `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.fabric runtime discovery probes client state before advertising unavailable raycast*'`
- Result: `BUILD SUCCESSFUL`.
- Command:
  `mise exec -- gradle :driver-fabric:test`
- Result: `BUILD SUCCESSFUL`.

## Final Local Gates

- Command: `git diff --check`
- Result: exited `0`.
- Command: `mise run architecture-check`
- Result: exited `0`.
- Command: `mise run ci`
- Result: exited `0`; Gradle lint/test succeeded and Bun Playwright tests
  reported `15 pass, 0 fail`.

## Push Gates

- Push to `main`: commit `a42d5680efc959ad19040f88ee173382f5efbf4d`
  (`driver-fabric: invoke through runtime graph`) pushed to
  `origin/main`.
- GitHub Actions CI for pushed `main`: run `28310090098`
  (`https://github.com/minekube/craftless/actions/runs/28310090098`) passed;
  job `verify` completed successfully in `4m44s`.
