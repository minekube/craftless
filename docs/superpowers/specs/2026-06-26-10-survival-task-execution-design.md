# Survival Task Execution Design

## Goal

Make `task.run` execute the honest no-cheat survival gate through the generated
runtime graph: observe the live world, obtain materials through gameplay,
craft or otherwise legitimately obtain a weapon, find a cow, navigate to it,
attack it, stream evidence, and keep completion blocked until Robin confirms in
Minecraft chat.

## Current Evidence

The no-cheat final gameplay run on June 26, 2026 proved launch, join,
graph-backed OpenAPI, SSE artifacts, chat, empty inventory, entity observation,
movement, block break, and block interaction. It also proved the missing piece:
`task.run` is advertised when the pathfinder backend is available, but the
adapter is still unsupported and no executor gathers materials, crafts,
navigates to a cow, or attacks through a task graph.

## Public Shape

Do not add public shortcut actions such as `find.cow`, `craft.sword`,
`kill.cow`, `mine.tree`, or static route families. The public action remains
the generated graph operation:

- `task.run` with `request.task = "task.survival.honest-cow-hunt"`
- `task.status` for the task id returned by `task.run`
- `task.progress` SSE events with Craftless-owned event types

The executor may call internal Fabric/Minecraft APIs, existing graph operation
adapters, and the internal pathfinder backend. Public OpenAPI and artifacts
must not expose raw Minecraft class names, Fabric API internals, backend
package names, server commands, or launcher internals.

## Internal Execution

The executor is an internal Fabric service owned by `driver-fabric`:

1. Verify the client is connected, the player and world are available, and the
   final run is not using item provisioning.
2. Observe nearby blocks/entities/inventory on the client thread.
3. Select a reachable material source such as a log block from observed world
   samples.
4. Use navigation and player/world interaction adapters to reach and break
   material blocks.
5. Craft a wooden sword or use an already legitimate weapon from inventory.
6. Observe passive entities, select a cow, navigate near it, face it, and attack
   until it is dead or the task fails with evidence.
7. Write progress to task status and SSE/event artifacts after every decision.

Direct server commands, server-side item provisioning, pre-seeded inventory,
manual movement for Craftless, and public static gameplay actions are invalid
completion evidence.

## Acceptance

- Focused tests prove `task.run` dispatches to a survival executor instead of
  the unsupported adapter.
- Tests prove failure states are honest and machine-readable when no cow,
  material, crafting path, pathfinder, or attack target is available.
- Tests prove the final gameplay controller invokes the task through
  `POST /clients/{id}:run` and records `task.run`, `task.status`, and
  `task.progress` evidence.
- A live no-cheat final run records material acquisition, weapon acquisition,
  cow observation, navigation, combat, chat, SSE, and no item provisioning.
- Robin writes in Minecraft chat that the goal may be completed.
