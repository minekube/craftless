# Shared Fabric Event Graph Projection Design

## Problem

The latest/current official Fabric lane now shares runtime metadata,
registry projection, and graph composition with the Yarn/remap lane. Event
source graph projection is still embedded in the Yarn/remap capability probe
even though the public graph shape is generic: resource `event` plus
Craftless-owned event ids.

Event projection is not gameplay action breadth. It is generated runtime
evidence that tells clients whether lifecycle/action/capability event streams
are available, unavailable, or not yet connected.

## Goal

Move Fabric event graph projection into `driver-fabric-discovery`:

- a shared `fabricEventGraphFragment(sourceEvidence, available)` helper;
- shared Craftless-owned event ids `event.lifecycle`, `event.action`, and
  `event.capability`;
- Yarn/remap lane still owns Fabric API callback/mixin evidence, but delegates
  resource/event node projection to the shared helper;
- official lane composes runtime, registry, and event fragments through the
  shared graph composer, with event nodes marked unavailable until the official
  lane has real event hook/callback evidence.

The official lane may expose unavailable `event` resource and event nodes.
That is evidence progress and must not be treated as latest/current gameplay
support.

## Non-Goals

- Do not add public gameplay actions.
- Do not add static action descriptors or gameplay catalogs.
- Do not add a packaged `26.2` driver manifest entry.
- Do not move Fabric API callback registration or mixin hooks into
  `driver-fabric-discovery`.
- Do not claim SSE/event streaming is complete for the official lane.
- Do not claim latest/current gameplay support.

## Design

Create `FabricEventGraph.kt` in `driver-fabric-discovery`.

The helper should return a `FabricRuntimeGraphFragment` containing:

- resource `event`;
- events `event.lifecycle`, `event.action`, and `event.capability`;
- caller-provided source evidence, or a fallback
  `RuntimeSourceEvidence("event-source", "events:not-discovered")` when no
  lane evidence is available;
- `RuntimeAvailability.available()` when `available = true`;
- `RuntimeAvailability.unavailable("event-source-not-discovered")` when
  `available = false`.

`driver-fabric` should keep Fabric callback/mixin evidence in the lane and
call the shared helper with `available = true`.

`driver-fabric-official` should call the shared helper with `available = false`
and no source evidence. Its generated OpenAPI should then report `resources=3`
and `events=3` while actions remain `0`.

## Acceptance

- Shared tests prove the event fragment emits resource `event` and events
  `event.lifecycle`, `event.action`, and `event.capability` with available
  status and caller evidence.
- Shared tests prove unavailable event projection uses reason
  `event-source-not-discovered` and fallback evidence
  `events:not-discovered`.
- Architecture guard proves the Yarn/remap lane no longer hand-builds
  `RuntimeResourceNode(id = "event")` or `RuntimeEventNode(id = "event.$id")`.
- Architecture guard proves the official backend uses
  `fabricEventGraphFragment` and still does not import `RuntimeCapabilityGraph`.
- Focused Fabric/discovery/official tests pass.
- The real enabled official attach probe still observes `client.attached`.
- The official OpenAPI artifact reports `actions=0`, `resources=3`,
  `events=3`, and event availability unavailable.
- No public gameplay action, packaged 26.x manifest entry, static gameplay
  catalog, scenario shortcut, or latest/current support claim is added.
