# Public Agent Material Exploration Design

## Intent

Make the public-agent gameplay runner robust when the first local material
query returns no log blocks. The runner should explore through generic,
runtime-discovered navigation and perception actions, then continue the
material collection policy only when public evidence identifies a block target.

This phase must not turn the survival acceptance scenario into product API.
The acceptance goal can be "collect wood for survival", but every operation
must be discovered from the live client and invoked through the generated
Craftless API.

## Product Rules

- Do not add `find.tree`, `mine.log`, `collect.wood`, `craft.sword`,
  `kill.cow`, `task.survival.*`, or any other scenario-specific public action.
- Do not call Fabric internals, driver internals, server commands, or manual
  operator movement.
- Do not hard-code a survival macro as final proof. The public agent policy may
  compose generic primitives; missing primitives must become runtime graph,
  projection, invocation, stream, CLI, docs, or skill work.
- Do not claim success from accepted actions alone. Verify state from public
  inventory, block, entity, player, and event evidence.

## Behavior

The runner continues the Phase 14 policy and changes only the material search
step:

1. Query `world.block.query` with a Craftless-owned category such as `log`.
2. If a block position is returned, continue with material navigation and
   collection.
3. If no block is returned, query `player.query` for the public player
   position.
4. Build a bounded list of generic exploration waypoints around that position.
5. For each waypoint:
   - invoke `navigation.plan` with a generic block-position goal;
   - invoke `navigation.follow` with the returned plan id;
   - invoke `world.block.query` again with the same generic material query.
6. If a later query returns a block position, continue with the existing
   navigation, look, raycast, break, and inventory-verification sequence.
7. If all bounded exploration attempts fail, stop with
   `insufficient-public-evidence:world.block.query.log` and preserve all action
   evidence.

## Evidence

Artifacts must show:

- generated discovery happens before action invocation;
- an empty local material query does not end the run immediately;
- exploration uses only generated `player.query`, `navigation.plan`,
  `navigation.follow`, and `world.block.query`;
- no request body contains `task.survival`, `find.tree`, `mine.log`,
  `collect.wood`, `craft.sword`, or `kill.cow`;
- blocked runs preserve exploration action evidence before the blocker;
- successful runs still require final inventory evidence after breaking.

