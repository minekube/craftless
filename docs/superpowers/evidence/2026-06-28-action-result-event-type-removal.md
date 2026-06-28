# Action Result Event Type Removal Evidence

Phase: 118

## Red

Command:

```sh
mise exec -- gradle :driver-api:test --tests '*DriverSessionContractTest.driver action results do not carry static event type metadata*'
```

Result:

- Exit code: 1
- Failure reason: `DriverActionResult` still exposed an `eventType` field.

## Green

Commands:

```sh
mise exec -- gradle :driver-api:test --tests '*DriverSessionContractTest.driver action results do not carry static event type metadata*'
mise exec -- gradle :driver-runtime:test --tests '*BackendDriverSessionTest.*'
mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.server streams generic graph invocation results without legacy event metadata*' --tests '*LocalSessionApiServerTest.server dispatches graph operations through registered operation adapters*' --tests '*LocalSessionApiServerTest.server streams filtered live client events as sse*'
mise exec -- gradle :driver-fabric:test --tests '*FabricDriverSelfAttachTest.*'
```

Results:

- Driver API contract guard: exit code 0.
- Driver runtime focused regression: exit code 0.
- Daemon SSE/operation-id focused regressions: exit code 0.
- Fabric self-attach focused regression: exit code 0.

## Local Gates

Commands:

```sh
git diff --check
mise run ci
```

Results:

- `git diff --check`: exit code 0.
- First `mise run ci`: exit code 1.
  Root cause: stale daemon test import/assertions still referenced the removed
  result event metadata behavior.
- Final `mise run ci`: exit code 0.
- `mise run ci` completed:
  - `mise exec -- gradle lint`
  - `mise run unused-check`
  - `mise exec -- gradle test`
  - `mise exec -- bun test playwright`
- Bun reported 18 passing tests across the Playwright helper suite.

## Scope Guard

This phase only removes static action-result event type metadata. It does not
add a public gameplay action, generated route family, CLI gameplay catalog,
Fabric gameplay binding, scenario shortcut, public version-specific API,
runnable latest/older lane, replacement action-event enum, or new Minecraft
support claim.
