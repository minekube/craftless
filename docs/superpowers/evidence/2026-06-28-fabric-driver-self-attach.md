# Phase 100 Fabric Driver Self-Attach Evidence

## Scope

Phase 100 lets a launched Fabric driver register its existing live
`DriverSession` with the supervisor:

- parse `CRAFTLESS_CLIENT_ID` and `CRAFTLESS_DAEMON_URL`;
- start a loopback-only Ktor endpoint for the `DriverSession` HTTP contract;
- post the endpoint to `POST /clients/{id}:attach`;
- wire the current-lane Fabric bootstrap after backend installation.

This phase adds transport plumbing only. It adds no public gameplay action,
static descriptor, generated route family, CLI gameplay catalog, Fabric
gameplay binding, scenario shortcut, compiled lane, version-specific public
API, or completion claim.

## Red Guards

- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverSelfAttachTest.attach environment*'`
  - Result: failed as expected before implementation.
  - Evidence: unresolved `FabricDriverAttachEnvironment`.
- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverSelfAttachTest.loopback endpoint exposes driver session contract*'`
  - Result: failed as expected before implementation.
  - Evidence: unresolved `FabricDriverLoopbackEndpoint`.
- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverSelfAttachTest.self attach posts loopback endpoint to daemon*'`
  - Result: failed as expected before implementation.
  - Evidence: unresolved `FabricDriverSelfAttach`.
- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.current lane bootstrap starts self attach from backend session*'`
  - Result: failed as expected before bootstrap wiring.
  - Evidence: assertion failed because the bootstrap did not create
    `BackendDriverSession` or call `FabricDriverSelfAttach.startFromEnvironment`.

## Green Checks

- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverSelfAttachTest.attach environment*'`
  - Result: `BUILD SUCCESSFUL`.
- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverSelfAttachTest.loopback endpoint exposes driver session contract*'`
  - Result: `BUILD SUCCESSFUL`.
- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverSelfAttachTest.self attach posts loopback endpoint to daemon*'`
  - Result: `BUILD SUCCESSFUL`.
- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverModuleTest.current lane bootstrap starts self attach from backend session*'`
  - Result: `BUILD SUCCESSFUL`.
- `mise exec -- gradle :driver-fabric:test --tests '*FabricDriverSelfAttachTest*'`
  - Result: `BUILD SUCCESSFUL`.

## Final Local Gates

- `git diff --check`
  - Result: exit `0`.
- `mise exec -- gradle :driver-fabric:test :daemon:test`
  - Result: `BUILD SUCCESSFUL`.
- `mise exec -- gradle :driver-fabric:ktlintCheck :driver-fabric:detekt :daemon:ktlintCheck :daemon:detekt`
  - Result: `BUILD SUCCESSFUL`.
