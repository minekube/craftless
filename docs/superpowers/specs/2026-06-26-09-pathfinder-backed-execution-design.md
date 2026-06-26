# Pathfinder-Backed Execution Design

## Goal

Craftless needs real movement execution for the no-cheat gameplay gate. Phase 8
created Craftless-owned navigation and task metadata; Phase 9 wires an internal
pathfinder backend to those graph operations so a visible Fabric client can
plan, follow, stop, and report progress without public backend names or static
gameplay shortcut APIs.

## Prior Art

Baritone provides a mature in-client pathing process model: a client instance
has pathing, look, custom-goal, mining, builder, and input override services.
The useful pattern for Craftless is not the chat command surface; it is the
internal process split where a goal is set, pathing starts, and progress can be
observed or cancelled.

SwarmBot separates navigation from task composition. Its navigation task
calculates paths in an expensive phase, follows them in ticks, and recalculates
when stale. Its entity-combat task first checks whether an entity is close
enough, otherwise emits a navigation task toward that entity. The useful pattern
for Craftless is this task stream model: observe, decide, invoke movement,
retry, and report progress.

## Product Boundary

The public API remains Craftless-owned:

- graph operations are `navigation.plan`, `navigation.follow`,
  `navigation.stop`, `task.run`, `task.status`;
- events are Craftless task/navigation progress events;
- no public OpenAPI, CLI, README, or SDK contract contains backend package
  names, backend class names, Minecraft commands, or one-off shortcut actions
  such as `kill.cow`, `find.tree`, or `craft.sword`.

Backend names are private evidence only. They may appear in internal source code
and tests when detecting optional runtime classes, but projected OpenAPI and SSE
payloads must not contain them.

## Runtime Shape

Add an internal `FabricPathfinderBackend` interface with these responsibilities:

- report whether a pathfinder implementation is present and executable;
- plan a Craftless `NavigationGoal` into an internal plan handle;
- follow a plan handle on the Minecraft client thread;
- stop active navigation;
- emit progress snapshots for SSE/task evidence.

The first concrete backend is reflection-based so Craftless can compile without
a hard public dependency on the optional pathfinder API. For local final
gameplay, Gradle may prepare a pinned runtime mod jar under the build directory
and include it only for opt-in client runs. That jar is an implementation input,
not a public Craftless dependency or API contract.

If the backend is absent, graph operations remain discovered but unavailable
with a machine-readable reason. If present but a method probe fails, operations
remain unavailable with a probe failure reason. Craftless must never advertise
available execution based only on class presence.

## Task Execution

Add an internal task registry owned by the Fabric backend. It stores task ids,
state, messages, timestamps, and latest payload data. `navigation.follow` and
`task.run` create or update registry entries, and every state transition emits a
Craftless-owned progress event that can flow through existing driver/daemon
events.

Phase 9 does not hard-code a complete cow-hunt executor. It creates the
execution substrate needed for Phase 10: real movement to observed targets,
observable task state, cancellation, and progress evidence.

## Testing

Use TDD with fake pathfinder backends first:

- protocol-level public leakage tests stay green;
- Fabric tests prove detected backend classes are private evidence only;
- backend tests prove `navigation.plan`, `navigation.follow`, and
  `navigation.stop` dispatch through `DriverOperationAdapters`;
- task registry tests prove progress state is recorded and can be serialized
  without backend names;
- a no-backend test proves operations are unavailable and machine-readable.

Final live gameplay remains incomplete until Phase 10 proves ordinary survival
material gathering, crafting/weapon acquisition, entity discovery, navigation,
combat, chat, and Robin's Minecraft chat confirmation.
