# Fabric Driver Self-Attach Design

## Problem

The supervisor can now launch clients with attach environment and accept a
remote driver attachment, but the Fabric driver mod still does not start a
loopback driver endpoint or register that endpoint with the supervisor. A
launched client can include the Craftless Fabric mod and receive
`CRAFTLESS_CLIENT_ID` plus `CRAFTLESS_DAEMON_URL`, yet the daemon still sees the
prepared placeholder driver until something posts `POST /clients/{id}:attach`.

## Goals

- Start a loopback HTTP endpoint from the Fabric driver when attach environment
  is present.
- Expose the stable `DriverSession` HTTP surface expected by daemon
  `HttpDriverSession`:
  - `GET /snapshot`
  - `POST /connect`
  - `GET /actions`
  - `GET /runtime-metadata`
  - `GET /runtime-graph`
  - `POST /invoke`
  - `POST /stop`
  - `GET /events`
- Self-register that endpoint with the supervisor by posting
  `{"endpoint":"http://127.0.0.1:<port>"}` to
  `{CRAFTLESS_DAEMON_URL}/clients/{CRAFTLESS_CLIENT_ID}:attach`.
- Keep the driver endpoint loopback-only.
- Keep all gameplay breadth owned by the existing runtime graph and
  `DriverSession`; this phase adds transport plumbing only.

## Non-Goals

- Do not add public gameplay actions, static descriptors, route families, CLI
  gameplay catalogs, Fabric bindings, or scenario shortcuts.
- Do not change the stable daemon attach route added in Phase 98.
- Do not claim latest/current or older Minecraft version support because of
  this transport work.
- Do not require self-attach when the attach environment is absent; local
  developer Fabric runs must continue to work without a daemon.

## Acceptance Criteria

- Unit tests prove attach environment is parsed only when both required values
  are present.
- Unit tests prove the loopback endpoint serves a `DriverSession` through the
  same HTTP contract consumed by daemon `HttpDriverSession`.
- Unit tests prove self-attach starts a loopback endpoint and posts the endpoint
  URL to the supervisor attach route.
- Source tests prove the Fabric current-lane bootstrap starts self-attach only
  after creating the real backend session.
- Focused driver-fabric and daemon tests, ktlint, detekt, and diff checks pass.
