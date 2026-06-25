# Driver API Contract

Date: 2026-06-25

## Purpose

`driver-api/` is the JVM contract between the supervisor/daemon and any
in-client driver implementation. The fake daemon session and the future Fabric
driver should use the same session shape so SDK, CLI, and Playwright routes do
not change when fake state is replaced by real Minecraft control.

## Current Contract

The module currently exposes:

- `DriverSession`
- `DriverClientSnapshot`
- `ConnectionTarget`
- `ChatCommand`
- `PlayerSnapshot`
- `DriverEvent`
- `DriverEventType`
- `FakeDriverSession`

Minimum supported actions:

- snapshot current client state;
- connect to a host/port;
- send chat;
- return player identity/state;
- stop the session;
- return structured driver events.

`FakeDriverSession` is a test and daemon-development implementation. It is not
the final Minecraft driver. It exists so daemon, CLI, SDK, and fixture code can
use the same public contract before the Fabric module lands.

## Fabric Handoff

The first Fabric driver should implement `DriverSession` for real client state:

- map `connect(ConnectionTarget)` to in-client server connection behavior;
- map `sendChat(ChatCommand)` to real client chat send behavior;
- make `player()` return real player state;
- emit `DriverEvent` values for ready, connect, chat, movement, stop, and
  error lifecycle events;
- keep low-level Mixins/accessors in Java when bytecode shape matters.

The public daemon routes remain:

- `POST /clients/{id}/connection/connect`
- `POST /clients/{id}/player/sendChat`
- `GET /clients/{id}/player`
- `POST /clients/{id}/stop`
- `GET /clients/{id}/events`

The Fabric module should change the driver implementation behind those routes,
not the route contract.
