# Launch Attach Environment Design

## Problem

Phase 98 added the supervisor attach/proxy route, but a launched Minecraft
client process still does not receive enough information for its in-client
driver to call back to the supervisor. The driver mod needs a Craftless client
id and daemon URL at launch time before it can start a loopback endpoint and
register itself with `POST /clients/{id}:attach`.

Without launch attach environment, packaged clients can include the driver mod
and the supervisor can accept attachments, but the two sides still have no
generic rendezvous configuration.

## Goals

- Provide launched client processes with Craftless-owned attach environment:
  `CRAFTLESS_CLIENT_ID` and `CRAFTLESS_DAEMON_URL`.
- Thread the attach environment from `LocalSessionApiServer` into
  `WorkspaceClientRuntimeDriverFactory.prepare` and `ClientRuntimeLauncher`.
- Make `ProcessClientRuntimeLauncher` set those variables on the client
  process environment.
- Keep this as lifecycle/transport configuration, not gameplay API breadth.

## Non-Goals

- Do not implement the Fabric in-client loopback endpoint in this phase.
- Do not add public gameplay actions, static descriptors, route families, CLI
  gameplay catalogs, Fabric bindings, or scenario shortcuts.
- Do not make version support or final gameplay completion claims.

## Acceptance Criteria

- A daemon server test proves a workspace launcher receives the client id and
  daemon URL attach environment when `/clients` creates a prepared runtime.
- A process launcher test proves the launched process receives
  `CRAFTLESS_CLIENT_ID` and `CRAFTLESS_DAEMON_URL`.
- Existing runtime launch behavior remains unchanged except for the added
  environment variables.
- Focused daemon tests, ktlint, detekt, and diff checks pass.
