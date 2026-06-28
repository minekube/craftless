# Phase 99 Launch Attach Environment Evidence

## Scope

Phase 99 passes Craftless lifecycle rendezvous environment into prepared client
launches:

- `CRAFTLESS_CLIENT_ID`
- `CRAFTLESS_DAEMON_URL`

This is not a gameplay API surface and does not add public gameplay actions,
static descriptors, generated route families, Fabric bindings, CLI gameplay
catalogs, scenario shortcuts, version-specific public APIs, or completion
claims.

## Red Guards

- `mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.server prepares and launches workspace client runtime without injected driver factory*'`
  - Result: failed as expected before implementation.
  - Evidence: unresolved `ClientDriverAttachEnvironment` and
    `attachEnvironment` symbols.
- `mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.process client runtime launcher starts prepared command*'`
  - Result: failed as expected before process environment propagation.
  - Evidence: assertion failed on recorded child-process attach environment.

## Green Checks

- `mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.server prepares and launches workspace client runtime without injected driver factory*'`
  - Result: `BUILD SUCCESSFUL`.
- `mise exec -- gradle :daemon:test --tests '*LocalSessionApiServerTest.process client runtime launcher starts prepared command*'`
  - Result: `BUILD SUCCESSFUL`.

## Final Local Gates

- `git diff --check`
  - Result: exit `0`.
- `mise exec -- gradle :daemon:test`
  - Result: `BUILD SUCCESSFUL`.
- `mise exec -- gradle :daemon:ktlintCheck :daemon:detekt`
  - Result: `BUILD SUCCESSFUL`.
