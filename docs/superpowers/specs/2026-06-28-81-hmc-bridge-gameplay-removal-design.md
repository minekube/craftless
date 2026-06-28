# HMC Bridge Gameplay Removal Design

## Problem

`HmcBridgeDriverBackend` still publishes and executes static gameplay actions:
`player.move` and `player.chat`. Those descriptors and invocation branches are
not generated from a live runtime capability graph. They preserve the old bridge
PoC shape after the Fabric/generated API path became the durable product path.

The lower `bridge-hmc` module also exposes chat, move, jump, and look helper
methods as first-milestone bridge actions. That code is evidence-only and now
creates an attractive stale extension point for adding more gameplay outside
the runtime graph.

## Goals

- Make the HMC bridge driver lifecycle-only: connect, stop, and runtime
  metadata.
- Remove HMC bridge static gameplay descriptors and invocation branches.
- Remove lower bridge gameplay helpers for chat, movement, jump, and look.
- Update bridge docs to state that HMC is launch/lifecycle evidence only and
  gameplay must go through Fabric runtime graph/OpenAPI.
- Add tests/guards so HMC bridge gameplay action catalogs do not return.

## Non-Goals

- Do not change Fabric generated actions, Fabric bootstrap bindings, public
  agent gameplay policy, or live OpenAPI generation.
- Do not remove the bridge module entirely; it remains useful as historical
  launch/lifecycle evidence and a prior-art comparison point.
- Do not add new gameplay actions, route families, CLI commands, bridge command
  wrappers, scenario shortcuts, version lanes, or support claims.

## Acceptance Criteria

- `HmcBridgeDriverBackend.actions(...)` returns no gameplay descriptors.
- Invoking `player.chat`, `player.move`, or any other gameplay action against
  `HmcBridgeDriverBackend` returns `UNSUPPORTED`.
- `HmcBridgeDriverBackend.kt` no longer contains `bridgePlayer*ActionDescriptor`
  helpers or `player.chat` / `player.move` branches.
- `HmcBridgeBackend` no longer exposes `chat`, `move`, `jump`, `look`, or
  `MoveIntent`.
- `ClientAction` is limited to bridge lifecycle actions.
- Bridge limitations docs no longer say the bridge accepts gameplay actions.
- Focused bridge/runtime tests, local gates, pushed `main` CI, and evidence are
  recorded.
