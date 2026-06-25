# Driver API Contract

Date: 2026-06-25

## Purpose

`driver-api/` is the JVM contract between the supervisor/daemon and any
in-client driver implementation. The fake daemon session and the future Fabric
driver should use the same session shape so CLI and Playwright routes do not
change when fake state is replaced by real Minecraft control.

## Current Contract

The module currently exposes:

- `DriverSession`
- `DriverClientSnapshot`
- `ConnectionTarget`
- `DriverActionDescriptor`
- `DriverActionInvocation`
- `DriverActionResult`
- `DriverActionStatus`
- `DriverEvent`
- `DriverEventType`
- `FakeDriverSession`

`driver-runtime/` now adds:

- `BackendDriverSession`
- `DriverBackend`
- `DriverBackendResult`
- `DriverBackendAction`
- `HmcBridgeDriverBackend`

`driver-fabric/` is the current Fabric/Loom module:

- `CraftlessFabricClientEntrypoint`
- `FabricDriverBackend`
- `FabricClientGateway`
- `MinecraftFabricClientGateway`
- `fabric.mod.json`
- `craftless-driver-fabric.mixins.json`

It keeps version-specific Minecraft bindings internal, currently under the
`v1_21_6` package, with room for reflection/mapping probes and small Java
Mixins/accessors where bytecode shape matters. Do not treat the Minecraft
version as part of the public module or artifact architecture.

Minimum supported actions:

- snapshot current client state;
- connect to a host/port;
- invoke discovered actions such as `player.chat` and `player.move`;
- stop the session;
- return structured driver events.

`FakeDriverSession` is a test and daemon-development implementation. It is not
the final Minecraft driver. It exists so daemon, CLI, and fixture code can use
the same public contract before the Fabric module lands.

`BackendDriverSession` is the first runtime adapter. It keeps `DriverSession`
state and events in Craftless-owned types while delegating automation actions
to a `DriverBackend`. The current HMC bridge adapter is temporary. The Fabric
module now routes connect, stop, and generic action invocation such as
`player.chat` and `player.move` through a client-thread gateway. It must still
add real-client movement smoke proof, perception, and structured event
observation through generated actions/resources rather than static player
methods.

## Fabric Handoff

The Fabric driver should continue implementing the runtime backend/session
boundary for real client state. Version differences should be handled by
internal binding support checks and by publishing only working actions in the
per-client OpenAPI document:

- map `connect(ConnectionTarget)` to in-client server connection behavior;
- keep Minecraft calls scheduled on the client thread;
- expose player state and position only through generated actions/resources
  described by the live per-client OpenAPI document;
- route generated/discovered actions such as `player.chat` and `player.move`
  through generic action invocation instead of adding one method per player
  action;
- preserve action invocation arguments as JSON values so schemas can use
  booleans, numbers, strings, arrays, and objects without string-only coercion;
- emit `DriverEvent` values for ready, connect, chat, movement, stop, and
  error lifecycle events;
- keep low-level Mixins/accessors in Java when bytecode shape matters.

The public daemon routes remain:

- `GET /clients/{id}/openapi.json`
- `GET /clients/{id}/actions`
- `POST /clients/{id}:run`
- generated aliases such as `POST /clients/{id}/player:move` and
  `POST /clients/{id}/player:chat` when described by that client's OpenAPI
  document
- `POST /clients/{id}:stop`
- `GET /clients/{id}/events`

Generic action invocation request bodies use typed JSON argument values:

```json
{"action":"player.move","args":{"forward":true,"ticks":20}}
```

Generated alias request bodies use the direct typed args object:

```json
{"forward":true,"ticks":20}
```

The Fabric module should change the driver implementation behind those routes,
not the route contract.
